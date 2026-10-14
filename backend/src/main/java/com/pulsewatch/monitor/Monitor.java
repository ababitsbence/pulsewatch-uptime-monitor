package com.pulsewatch.monitor;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "monitors")
public class Monitor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, updatable = false)
    private Long userId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String target;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MonitorType type;

    @Column(name = "interval_seconds", nullable = false)
    private int intervalSeconds;

    @Column(name = "timeout_seconds", nullable = false)
    private int timeoutSeconds;

    @Column(name = "expected_status")
    private Integer expectedStatus;

    @Column(nullable = false)
    private boolean enabled = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected Monitor() {
        // required by JPA
    }

    public Monitor(Long userId, String name, String target, MonitorType type,
                   int intervalSeconds, int timeoutSeconds, Integer expectedStatus, boolean enabled) {
        this.userId = userId;
        update(name, target, type, intervalSeconds, timeoutSeconds, expectedStatus, enabled);
    }

    public void update(String name, String target, MonitorType type,
                       int intervalSeconds, int timeoutSeconds, Integer expectedStatus, boolean enabled) {
        this.name = name;
        this.target = target;
        this.type = type;
        this.intervalSeconds = intervalSeconds;
        this.timeoutSeconds = timeoutSeconds;
        this.expectedStatus = expectedStatus;
        this.enabled = enabled;
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public String getName() { return name; }
    public String getTarget() { return target; }
    public MonitorType getType() { return type; }
    public int getIntervalSeconds() { return intervalSeconds; }
    public int getTimeoutSeconds() { return timeoutSeconds; }
    public Integer getExpectedStatus() { return expectedStatus; }
    public boolean isEnabled() { return enabled; }
    public Instant getCreatedAt() { return createdAt; }
}