package com.pulsewatch.monitor;

import java.time.Instant;

public record MonitorResponse(
        Long id,
        String name,
        String target,
        MonitorType type,
        int intervalSeconds,
        int timeoutSeconds,
        Integer expectedStatus,
        boolean enabled,
        Instant createdAt) {

    public static MonitorResponse from(Monitor monitor) {
        return new MonitorResponse(
                monitor.getId(),
                monitor.getName(),
                monitor.getTarget(),
                monitor.getType(),
                monitor.getIntervalSeconds(),
                monitor.getTimeoutSeconds(),
                monitor.getExpectedStatus(),
                monitor.isEnabled(),
                monitor.getCreatedAt());
    }
}