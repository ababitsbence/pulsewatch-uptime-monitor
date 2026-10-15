package com.pulsewatch.check;

import com.pulsewatch.monitor.Monitor;
import com.pulsewatch.monitor.MonitorType;
import com.pulsewatch.monitor.TargetHost;
import com.pulsewatch.ssrf.SsrfGuard;

import java.util.Optional;

public class GuardedHealthCheck implements HealthCheck {

    private final HealthCheck delegate;
    private final SsrfGuard guard;

    public GuardedHealthCheck(HealthCheck delegate, SsrfGuard guard) {
        this.delegate = delegate;
        this.guard = guard;
    }

    @Override
    public MonitorType type() {
        return delegate.type();
    }

    @Override
    public CheckOutcome check(Monitor monitor) {
        Optional<String> violation = TargetHost.of(monitor.getType(), monitor.getTarget())
                .flatMap(guard::violation);
        return violation.map(s -> CheckOutcome.down(0, s)).orElseGet(() -> delegate.check(monitor));
    }
}