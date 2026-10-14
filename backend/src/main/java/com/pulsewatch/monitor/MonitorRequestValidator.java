package com.pulsewatch.monitor;

import com.pulsewatch.common.RequestValidationException;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public final class MonitorRequestValidator {

    private MonitorRequestValidator() {
    }

    public static void validate(MonitorRequest request) {
        Map<String, String> errors = new LinkedHashMap<>();

        if (request.type() == MonitorType.HTTP) {
            validateHttpTarget(request.target()).ifPresent(message -> errors.put("target", message));
        } else if (request.type() == MonitorType.TCP) {
            validateTcpTarget(request.target()).ifPresent(message -> errors.put("target", message));
            if (request.expectedStatus() != null) {
                errors.put("expectedStatus", "is only valid for HTTP monitors");
            }
        }

        if (request.timeoutSeconds() >= request.intervalSeconds()) {
            errors.put("timeoutSeconds", "must be smaller than intervalSeconds");
        }

        if (!errors.isEmpty()) {
            throw new RequestValidationException(errors);
        }
    }

    private static Optional<String> validateHttpTarget(String target) {
        try {
            URI uri = new URI(target.trim());
            String scheme = uri.getScheme();
            if (scheme == null
                    || !(scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https"))) {
                return Optional.of("must be an http or https URL");
            }
            if (uri.getHost() == null || uri.getHost().isBlank()) {
                return Optional.of("must include a host");
            }
            if (uri.getUserInfo() != null) {
                return Optional.of("must not contain credentials");
            }
            return Optional.empty();
        } catch (URISyntaxException e) {
            return Optional.of("must be a valid URL");
        }
    }

    private static Optional<String> validateTcpTarget(String target) {
        String trimmed = target.trim();
        int separator = trimmed.lastIndexOf(':');
        if (separator <= 0 || separator == trimmed.length() - 1) {
            return Optional.of("must be in host:port format");
        }
        String host = trimmed.substring(0, separator);
        String portText = trimmed.substring(separator + 1);

        if (!host.matches("[A-Za-z0-9.-]+")) {
            return Optional.of("host may only contain letters, digits, dots and hyphens");
        }
        try {
            int port = Integer.parseInt(portText);
            if (port < 1 || port > 65535) {
                return Optional.of("port must be between 1 and 65535");
            }
        } catch (NumberFormatException e) {
            return Optional.of("port must be a number");
        }
        return Optional.empty();
    }
}