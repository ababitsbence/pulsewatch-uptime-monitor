package com.pulsewatch.check;

public record CheckOutcome(CheckStatus status, long responseTimeMs, String error) {

    static final int MAX_ERROR_LENGTH = 500;

    public static CheckOutcome up(long responseTimeMs) {
        return new CheckOutcome(CheckStatus.UP, responseTimeMs, null);
    }

    public static CheckOutcome down(long responseTimeMs, String error) {
        return new CheckOutcome(CheckStatus.DOWN, responseTimeMs, truncate(error));
    }

    public static CheckOutcome down(long responseTimeMs, Exception cause) {
        String message = cause.getMessage() != null ? cause.getMessage() : cause.getClass().getSimpleName();
        return down(responseTimeMs, message);
    }

    private static String truncate(String error) {
        if (error == null || error.length() <= MAX_ERROR_LENGTH) {
            return error;
        }
        return error.substring(0, MAX_ERROR_LENGTH);
    }
}