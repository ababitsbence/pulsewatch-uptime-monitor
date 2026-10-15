package com.pulsewatch.check;

import com.pulsewatch.monitor.Monitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class CheckRunner {

    private static final Logger log = LoggerFactory.getLogger(CheckRunner.class);

    private final HealthCheckFactory factory;
    private final CheckResultService checkResultService;

    public CheckRunner(HealthCheckFactory factory, CheckResultService checkResultService) {
        this.factory = factory;
        this.checkResultService = checkResultService;
    }

    public void run(Monitor monitor) {
        long start = System.nanoTime();
        CheckOutcome outcome;
        try {
            outcome = factory.forType(monitor.getType()).check(monitor);
        } catch (RuntimeException e) {
            log.warn("Check for monitor {} failed unexpectedly: {}", monitor.getId(), e.toString());
            outcome = CheckOutcome.down(TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start), e);
        }

        try {
            checkResultService.record(monitor.getId(), outcome);
        } catch (RuntimeException e) {
            // for example the monitor was deleted while its check was running
            log.warn("Could not record result for monitor {}: {}", monitor.getId(), e.toString());
        }
    }
}