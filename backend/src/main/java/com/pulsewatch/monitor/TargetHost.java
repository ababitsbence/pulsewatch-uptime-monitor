package com.pulsewatch.monitor;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Optional;

public final class TargetHost {

    private TargetHost() {
    }

    public static Optional<String> of(MonitorType type, String target) {
        if (target == null) {
            return Optional.empty();
        }
        String trimmed = target.trim();
        if (type == MonitorType.HTTP) {
            try {
                return Optional.ofNullable(new URI(trimmed).getHost()).filter(host -> !host.isBlank());
            } catch (URISyntaxException e) {
                return Optional.empty();
            }
        }
        int separator = trimmed.lastIndexOf(':');
        if (separator <= 0) {
            return Optional.empty();
        }
        return Optional.of(trimmed.substring(0, separator));
    }
}