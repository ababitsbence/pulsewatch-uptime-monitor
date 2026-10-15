package com.pulsewatch.check;

import com.pulsewatch.monitor.Monitor;
import com.pulsewatch.monitor.MonitorRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Semaphore;

@Component
@ConditionalOnProperty(name = "app.checks.enabled", havingValue = "true", matchIfMissing = true)
public class CheckScheduler {

    private static final Logger log = LoggerFactory.getLogger(CheckScheduler.class);

    private final MonitorRepository monitorRepository;
    private final CheckRunner checkRunner;
    private final Executor executor;
    private final Clock clock;
    private final Semaphore permits;
    private final Set<Long> inFlight = ConcurrentHashMap.newKeySet();

    public CheckScheduler(MonitorRepository monitorRepository,
                          CheckRunner checkRunner,
                          @Qualifier("checkExecutor") Executor executor,
                          Clock clock,
                          CheckProperties properties) {
        this.monitorRepository = monitorRepository;
        this.checkRunner = checkRunner;
        this.executor = executor;
        this.clock = clock;
        this.permits = new Semaphore(properties.maxConcurrent());
    }

    @Scheduled(fixedDelayString = "${app.checks.tick:PT5S}")
    public void runDueChecks() {
        List<Monitor> due;
        try {
            due = monitorRepository.findDueMonitors(clock.instant());
        } catch (RuntimeException e) {
            log.error("Could not load due monitors", e);
            return;
        }

        for (Monitor monitor : due) {
            Long id = monitor.getId();
            if (!inFlight.add(id)) {
                continue;
            }
            try {
                executor.execute(() -> execute(monitor));
            } catch (RuntimeException e) {
                inFlight.remove(id);
                log.warn("Could not schedule check for monitor {}: {}", id, e.toString());
            }
        }
    }

    private void execute(Monitor monitor) {
        try {
            permits.acquire();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            inFlight.remove(monitor.getId());
            return;
        }
        try {
            checkRunner.run(monitor);
        } catch (RuntimeException e) {
            log.error("Check for monitor {} failed unexpectedly", monitor.getId(), e);
        } finally {
            permits.release();
            inFlight.remove(monitor.getId());
        }
    }
}