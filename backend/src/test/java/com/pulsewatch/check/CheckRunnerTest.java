package com.pulsewatch.check;

import com.pulsewatch.monitor.Monitor;
import com.pulsewatch.monitor.MonitorType;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CheckRunnerTest {

    private final HealthCheckFactory factory = mock(HealthCheckFactory.class);
    private final HealthCheck healthCheck = mock(HealthCheck.class);
    private final CheckResultService results = mock(CheckResultService.class);
    private final CheckRunner runner = new CheckRunner(factory, results);

    private Monitor monitor() {
        Monitor monitor = mock(Monitor.class);
        when(monitor.getId()).thenReturn(5L);
        when(monitor.getType()).thenReturn(MonitorType.HTTP);
        when(factory.forType(MonitorType.HTTP)).thenReturn(healthCheck);
        return monitor;
    }

    @Test
    void recordsTheOutcomeOfASuccessfulCheck() {
        Monitor monitor = monitor();
        CheckOutcome outcome = CheckOutcome.up(10);
        when(healthCheck.check(monitor)).thenReturn(outcome);

        runner.run(monitor);

        verify(results).record(5L, outcome);
    }

    @Test
    void recordsDownWhenTheCheckThrows() {
        Monitor monitor = monitor();
        when(healthCheck.check(monitor)).thenThrow(new IllegalStateException("boom"));

        runner.run(monitor);

        ArgumentCaptor<CheckOutcome> captor = ArgumentCaptor.forClass(CheckOutcome.class);
        verify(results).record(org.mockito.ArgumentMatchers.eq(5L), captor.capture());
        assertThat(captor.getValue().status()).isEqualTo(CheckStatus.DOWN);
        assertThat(captor.getValue().error()).contains("boom");
    }

    @Test
    void doesNotPropagateAFailureToRecordTheResult() {
        Monitor monitor = monitor();
        when(healthCheck.check(monitor)).thenReturn(CheckOutcome.up(10));
        doThrow(new RuntimeException("db down")).when(results).record(any(), any());

        assertThatCode(() -> runner.run(monitor)).doesNotThrowAnyException();
    }
}