package com.pulsewatch.check;

import com.pulsewatch.common.RequestValidationException;
import com.pulsewatch.common.ResourceNotFoundException;
import com.pulsewatch.monitor.MonitorRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class CheckResultServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-15T10:00:00Z");

    private final CheckResultRepository results = mock(CheckResultRepository.class);
    private final MonitorRepository monitors = mock(MonitorRepository.class);
    private final CheckResultService service =
            new CheckResultService(results, monitors, Clock.fixed(NOW, ZoneOffset.UTC));

    @Test
    void recordStoresTheOutcomeWithTheCurrentTime() {
        service.record(7L, CheckOutcome.down(123, "Connection refused"));

        ArgumentCaptor<CheckResult> captor = ArgumentCaptor.forClass(CheckResult.class);
        verify(results).save(captor.capture());
        CheckResult saved = captor.getValue();
        assertThat(saved.getMonitorId()).isEqualTo(7L);
        assertThat(saved.getCheckedAt()).isEqualTo(NOW);
        assertThat(saved.getStatus()).isEqualTo(CheckStatus.DOWN);
        assertThat(saved.getResponseTimeMs()).isEqualTo(123);
        assertThat(saved.getError()).isEqualTo("Connection refused");
    }

    @Test
    void historyOfAnotherUsersMonitorIsNotFound() {
        org.mockito.Mockito.when(monitors.findByIdAndUserId(1L, 2L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.history(2L, 1L, 10))
                .isInstanceOf(ResourceNotFoundException.class);
        verifyNoInteractions(results);
    }

    @ParameterizedTest
    @ValueSource(ints = {-5, 0, 201, 1000})
    void historyRejectsLimitOutsideAllowedRange(int limit) {
        assertThatThrownBy(() -> service.history(2L, 1L, limit))
                .isInstanceOf(RequestValidationException.class);
        verifyNoInteractions(monitors);
        verifyNoInteractions(results);
    }
}