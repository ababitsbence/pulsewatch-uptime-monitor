package com.pulsewatch.check;

import java.time.Instant;

public record CheckResultResponse(
        Long id,
        Instant checkedAt,
        CheckStatus status,
        long responseTimeMs,
        String error) {

    public static CheckResultResponse from(CheckResult result) {
        return new CheckResultResponse(
                result.getId(),
                result.getCheckedAt(),
                result.getStatus(),
                result.getResponseTimeMs(),
                result.getError());
    }
}