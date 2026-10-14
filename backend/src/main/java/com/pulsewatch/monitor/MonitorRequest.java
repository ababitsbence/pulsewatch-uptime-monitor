package com.pulsewatch.monitor;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record MonitorRequest(
        @NotBlank @Size(max = 100) String name,
        @NotBlank @Size(max = 2048) String target,
        @NotNull MonitorType type,
        @NotNull @Min(30) @Max(86400) Integer intervalSeconds,
        @NotNull @Min(1) @Max(60) Integer timeoutSeconds,
        @Min(100) @Max(599) Integer expectedStatus,
        Boolean enabled) {
}