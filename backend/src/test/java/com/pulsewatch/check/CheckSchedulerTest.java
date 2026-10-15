package com.pulsewatch.check;

import com.pulsewatch.monitor.Monitor;
import com.pulsewatch.monitor.MonitorRepository;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class CheckSchedulerTest {

    private static final Instant NOW = Instant.parse("2026-10-15T10:00:00Z");

    private final MonitorRepository monitors = mock(MonitorRepository.class);
    private final CheckRunner runner = mock(CheckRunner.class);
    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
    private final List<Runnable> queued = new ArrayList<>();
    // tasks are only queued, so each test decides when they run
    private final CheckScheduler scheduler =
            new CheckScheduler(monitors, runner, queued::add, clock, new CheckProperties(10));

    private static Monitor monitor(long id) {
        Monitor monitor = mock(Monitor.class);
        when(monitor.getId()).thenReturn(id);
        return monitor;
    }

    private void runQueuedTasks() {
        List<Runnable> tasks = new ArrayList<>(queued);
        queued.clear();
        tasks.forEach(Runnable::run);
    }

    @Test
    void doesNothingWhenNothingIsDue() {
        when(monitors.findDueMonitors(any())).thenReturn(List.of());

        scheduler.runDueChecks();

        assertThat(queued).isEmpty();
    }

    @Test
    void asksForMonitorsDueAtTheCurrentTime() {
        when(monitors.findDueMonitors(any())).thenReturn(List.of());

        scheduler.runDueChecks();

        verify(monitors).findDueMonitors(NOW);
    }

    @Test
    void queuesOneTaskPerDueMonitorAndRunsThem() {
        Monitor first = monitor(1L);
        Monitor second = monitor(2L);
        when(monitors.findDueMonitors(any())).thenReturn(List.of(first, second));

        scheduler.runDueChecks();
        assertThat(queued).hasSize(2);
        verifyNoInteractions(runner);

        runQueuedTasks();
        verify(runner).run(first);
        verify(runner).run(second);
    }

    @Test
    void doesNotQueueAMonitorWhoseCheckIsStillRunning() {
        Monitor monitor = monitor(1L);
        when(monitors.findDueMonitors(any())).thenReturn(List.of(monitor));

        scheduler.runDueChecks();
        scheduler.runDueChecks();

        assertThat(queued).hasSize(1);
    }

    @Test
    void queuesAMonitorAgainOnceItsCheckHasFinished() {
        Monitor monitor = monitor(1L);
        when(monitors.findDueMonitors(any())).thenReturn(List.of(monitor));

        scheduler.runDueChecks();
        runQueuedTasks();
        scheduler.runDueChecks();

        assertThat(queued).hasSize(1);
    }

    @Test
    void aFailingCheckDoesNotBlockTheMonitorForever() {
        Monitor monitor = monitor(1L);
        when(monitors.findDueMonitors(any())).thenReturn(List.of(monitor));
        doThrow(new RuntimeException("boom")).when(runner).run(monitor);

        scheduler.runDueChecks();
        runQueuedTasks();
        scheduler.runDueChecks();

        assertThat(queued).hasSize(1);
    }

    @Test
    void survivesAFailureToLoadDueMonitors() {
        when(monitors.findDueMonitors(any())).thenThrow(new RuntimeException("db down"));

        scheduler.runDueChecks();

        assertThat(queued).isEmpty();
    }
}