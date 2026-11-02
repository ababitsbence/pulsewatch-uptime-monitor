package com.pulsewatch.check;

import com.pulsewatch.monitor.MonitorType;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class CheckMetricsTest {

    private final SimpleMeterRegistry registry = new SimpleMeterRegistry();
    private final CheckMetrics metrics = new CheckMetrics(registry);

    private double count(String type, String status) {
        return registry.get("pulsewatch.checks").tag("type", type).tag("status", status).counter().count();
    }

    @Test
    void registersEverySeriesAtStartupWithZero() {
        assertThat(registry.find("pulsewatch.checks").counters()).hasSize(4);
        assertThat(registry.find("pulsewatch.checks").counters())
                .extracting(Counter::count)
                .containsOnly(0.0);
        assertThat(registry.find("pulsewatch.check.duration").timers()).hasSize(4);
    }

    @Test
    void countsPerTypeAndStatus() {
        metrics.record(MonitorType.HTTP, CheckOutcome.up(10));
        metrics.record(MonitorType.HTTP, CheckOutcome.up(20));
        metrics.record(MonitorType.TCP, CheckOutcome.down(30, "refused"));

        assertThat(count("http", "up")).isEqualTo(2);
        assertThat(count("http", "down")).isZero();
        assertThat(count("tcp", "up")).isZero();
        assertThat(count("tcp", "down")).isEqualTo(1);
    }

    @Test
    void recordsTheResponseTimeAsDuration() {
        metrics.record(MonitorType.TCP, CheckOutcome.up(120));
        metrics.record(MonitorType.TCP, CheckOutcome.up(80));

        var timer = registry.get("pulsewatch.check.duration").tag("type", "tcp").tag("status", "up").timer();
        assertThat(timer.count()).isEqualTo(2);
        assertThat(timer.totalTime(TimeUnit.MILLISECONDS)).isEqualTo(200.0);
    }
}