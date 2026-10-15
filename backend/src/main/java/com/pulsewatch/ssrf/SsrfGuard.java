package com.pulsewatch.ssrf;

import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Optional;

@Component
public class SsrfGuard {

    static final String MESSAGE = "Target resolves to a private or reserved address";

    private final HostResolver resolver;
    private final SsrfProperties properties;

    public SsrfGuard(HostResolver resolver, SsrfProperties properties) {
        this.resolver = resolver;
        this.properties = properties;
    }

    /**
     * Returns a message if the host resolves to a blocked address. Hosts that do not resolve are
     * allowed here: they cannot reach anything, and the check itself will report them as DOWN.
     * The message never contains the resolved address.
     */
    public Optional<String> violation(String host) {
        if (properties.allowPrivateTargets()) {
            return Optional.empty();
        }
        InetAddress[] addresses;
        try {
            addresses = resolver.resolve(host);
        } catch (UnknownHostException e) {
            return Optional.empty();
        }
        for (InetAddress address : addresses) {
            if (AddressPolicy.isBlocked(address)) {
                return Optional.of(MESSAGE);
            }
        }
        return Optional.empty();
    }
}