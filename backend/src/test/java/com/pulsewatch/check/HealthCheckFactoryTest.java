package com.pulsewatch.check;

import com.pulsewatch.monitor.Monitor;
import com.pulsewatch.monitor.MonitorType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HealthCheckFactoryTest {

    private static HealthCheck stub(MonitorType type) {
        return new HealthCheck() {
            @Override
            public MonitorType type() {
                return type;
            }

            @Override
            public CheckOutcome check(Monitor monitor) {
                return CheckOutcome.up(0);
            }
        };
    }

    @Test
    void returnsTheCheckForEachType() {
        HealthCheck http = stub(MonitorType.HTTP);
        HealthCheck tcp = stub(MonitorType.TCP);
        HealthCheckFactory factory = new HealthCheckFactory(List.of(http, tcp));

        assertThat(factory.forType(MonitorType.HTTP)).isSameAs(http);
        assertThat(factory.forType(MonitorType.TCP)).isSameAs(tcp);
    }

    @Test
    void failsWhenNoCheckIsRegisteredForAType() {
        HealthCheckFactory factory = new HealthCheckFactory(List.of(stub(MonitorType.HTTP)));

        assertThatThrownBy(() -> factory.forType(MonitorType.TCP))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void rejectsTwoChecksForTheSameType() {
        List<HealthCheck> duplicates = List.of(stub(MonitorType.HTTP), stub(MonitorType.HTTP));

        assertThatThrownBy(() -> new HealthCheckFactory(duplicates))
                .isInstanceOf(IllegalStateException.class);
    }
}