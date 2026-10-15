package com.pulsewatch;

import com.pulsewatch.ssrf.HostResolver;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * Replaces DNS in integration tests: names resolve only if a test registered them,
 * IP literals resolve to themselves, anything else is unknown.
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestDnsConfig {

    private static final Pattern LITERAL = Pattern.compile("^(\\[?[0-9a-fA-F]*:[0-9a-fA-F:.]*]?|[0-9.]+)$");
    private static final Map<String, List<String>> RECORDS = new ConcurrentHashMap<>();

    public static void setRecords(String host, String... addresses) {
        RECORDS.put(host.toLowerCase(Locale.ROOT), List.of(addresses));
    }

    public static void clear() {
        RECORDS.clear();
    }

    @Bean
    @Primary
    HostResolver testHostResolver() {
        return host -> {
            List<String> records = RECORDS.get(host.toLowerCase(Locale.ROOT));
            if (records != null) {
                InetAddress[] addresses = new InetAddress[records.size()];
                for (int i = 0; i < addresses.length; i++) {
                    addresses[i] = InetAddress.getByName(records.get(i));
                }
                return addresses;
            }
            if (LITERAL.matcher(host).matches()) {
                return InetAddress.getAllByName(host);
            }
            throw new UnknownHostException(host);
        };
    }
}