package com.pulsewatch.check;

import com.pulsewatch.monitor.MonitorType;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Component
public class HealthCheckFactory {

    private final Map<MonitorType, HealthCheck> checks = new EnumMap<>(MonitorType.class);

    public HealthCheckFactory(List<HealthCheck> healthChecks) {
        for (HealthCheck check : healthChecks) {
            if (checks.putIfAbsent(check.type(), check) != null) {
                throw new IllegalStateException("Duplicate health check for type " + check.type());
            }
        }
    }

    public HealthCheck forType(MonitorType type) {
        HealthCheck check = checks.get(type);
        if (check == null) {
            throw new IllegalStateException("No health check registered for type " + type);
        }
        return check;
    }
}