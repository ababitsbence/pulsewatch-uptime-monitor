package com.pulsewatch.ssrf;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "app.ssrf")
public record SsrfProperties(@DefaultValue("false") boolean allowPrivateTargets) {
}