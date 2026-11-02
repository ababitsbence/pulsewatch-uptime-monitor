package com.pulsewatch.check;

import com.pulsewatch.monitor.MonitorType;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;

/**
 * Counts and times health checks. Every combination of type and status is registered up front,
 * so the series exist with value 0 from startup and the number of series stays fixed.
 */
@Component
public class CheckMetrics {

    private final Map<MonitorType, Map<CheckStatus, Counter>> counters = new EnumMap<>(MonitorType.class);
    private final Map<MonitorType, Map<CheckStatus, Timer>> timers = new EnumMap<>(MonitorType.class);

    public CheckMetrics(MeterRegistry registry) {
        for (MonitorType type : MonitorType.values()) {
            Map<CheckStatus, Counter> typeCounters = new EnumMap<>(CheckStatus.class);
            Map<CheckStatus, Timer> typeTimers = new EnumMap<>(CheckStatus.class);
            String typeTag = type.name().toLowerCase(Locale.ROOT);

            for (CheckStatus status : CheckStatus.values()) {
                String statusTag = status.name().toLowerCase(Locale.ROOT);

                typeCounters.put(status, Counter.builder("pulsewatch.checks")
                        .description("Completed health checks")
                        .tag("type", typeTag)
                        .tag("status", statusTag)
                        .register(registry));

                typeTimers.put(status, Timer.builder("pulsewatch.check.duration")
                        .description("Duration of health checks")
                        .tag("type", typeTag)
                        .tag("status", statusTag)
                        .serviceLevelObjectives(
                                Duration.ofMillis(50),
                                Duration.ofMillis(100),
                                Duration.ofMillis(250),
                                Duration.ofMillis(500),
                                Duration.ofSeconds(1),
                                Duration.ofMillis(2500),
                                Duration.ofSeconds(5),
                                Duration.ofSeconds(10))
                        .register(registry));
            }
            counters.put(type, typeCounters);
            timers.put(type, typeTimers);
        }
    }

    public void record(MonitorType type, CheckOutcome outcome) {
        counters.get(type).get(outcome.status()).increment();
        timers.get(type).get(outcome.status()).record(Duration.ofMillis(outcome.responseTimeMs()));
    }
}