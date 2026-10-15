package com.pulsewatch.check;

import com.pulsewatch.monitor.Monitor;
import com.pulsewatch.monitor.MonitorType;

public interface HealthCheck {

    MonitorType type();

    CheckOutcome check(Monitor monitor);
}