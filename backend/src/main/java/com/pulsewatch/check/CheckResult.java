package com.pulsewatch.check;

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
@Table(name = "check_results")
public class CheckResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "monitor_id", nullable = false, updatable = false)
    private Long monitorId;

    @Column(name = "checked_at", nullable = false, updatable = false)
    private Instant checkedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false)
    private CheckStatus status;

    @Column(name = "response_time_ms", nullable = false, updatable = false)
    private long responseTimeMs;

    @Column(length = 500, updatable = false)
    private String error;

    protected CheckResult() {
        // required by JPA
    }

    public CheckResult(Long monitorId, Instant checkedAt, CheckStatus status,
                       long responseTimeMs, String error) {
        this.monitorId = monitorId;
        this.checkedAt = checkedAt;
        this.status = status;
        this.responseTimeMs = responseTimeMs;
        this.error = error;
    }

    public Long getId() { return id; }
    public Long getMonitorId() { return monitorId; }
    public Instant getCheckedAt() { return checkedAt; }
    public CheckStatus getStatus() { return status; }
    public long getResponseTimeMs() { return responseTimeMs; }
    public String getError() { return error; }
}