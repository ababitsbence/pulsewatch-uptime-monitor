package com.pulsewatch.check;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.checks")
public record CheckProperties(@DefaultValue("50") @Min(1) @Max(1000) int maxConcurrent) { }