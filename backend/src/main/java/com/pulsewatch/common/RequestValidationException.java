package com.pulsewatch.common;

import java.util.Map;

public class RequestValidationException extends RuntimeException {

    private final Map<String, String> errors;

    public RequestValidationException(Map<String, String> errors) {
        super("Validation failed");
        this.errors = Map.copyOf(errors);
    }

    public Map<String, String> getErrors() {
        return errors;
    }
}