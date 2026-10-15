package com.pulsewatch.ssrf;

import org.junit.jupiter.api.Test;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class SsrfGuardTest {

    private final Map<String, String[]> dns = new HashMap<>();

    private final HostResolver resolver = host -> {
        String[] records = dns.get(host);
        if (records != null) {
            InetAddress[] addresses = new InetAddress[records.length];
            for (int i = 0; i < records.length; i++) {
                addresses[i] = InetAddress.getByName(records[i]);
            }
            return addresses;
        }
        if (host.matches("[0-9.]+") || host.contains(":")) {
            return InetAddress.getAllByName(host);
        }
        throw new UnknownHostException(host);
    };

    private SsrfGuard guard(boolean allowPrivateTargets) {
        return new SsrfGuard(resolver, new SsrfProperties(allowPrivateTargets));
    }

    @Test
    void blocksPrivateIpLiteral() {
        assertThat(guard(false).violation("10.0.0.5")).isPresent();
        assertThat(guard(false).violation("127.0.0.1")).isPresent();
    }

    @Test
    void blocksBracketedIpv6Loopback() {
        assertThat(guard(false).violation("[::1]")).isPresent();
    }

    @Test
    void allowsPublicIpLiteral() {
        assertThat(guard(false).violation("8.8.8.8")).isEmpty();
    }

    @Test
    void blocksHostnameThatResolvesToPrivateAddress() {
        dns.put("internal.test", new String[]{"10.0.0.5"});

        assertThat(guard(false).violation("internal.test")).isPresent();
    }

    @Test
    void blocksHostnameWhenAnyRecordIsPrivate() {
        dns.put("mixed.test", new String[]{"93.184.216.34", "10.0.0.5"});

        assertThat(guard(false).violation("mixed.test")).isPresent();
    }

    @Test
    void allowsHostnameThatResolvesToPublicAddress() {
        dns.put("public.test", new String[]{"93.184.216.34"});

        assertThat(guard(false).violation("public.test")).isEmpty();
    }

    @Test
    void allowsHostThatDoesNotResolve() {
        assertThat(guard(false).violation("unresolvable.test")).isEmpty();
    }

    @Test
    void messageDoesNotRevealTheResolvedAddress() {
        dns.put("internal.test", new String[]{"10.0.0.5"});

        String message = guard(false).violation("internal.test").orElseThrow();

        assertThat(message).doesNotContain("10.0.0.5");
    }

    @Test
    void settingAllowsPrivateTargets() {
        assertThat(guard(true).violation("127.0.0.1")).isEmpty();
    }
}