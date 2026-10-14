package com.pulsewatch.monitor;

import com.pulsewatch.common.RequestValidationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class MonitorRequestValidatorTest {

    private static MonitorRequest http(String target) {
        return new MonitorRequest("m", target, MonitorType.HTTP, 60, 10, null, null);
    }

    private static MonitorRequest tcp(String target) {
        return new MonitorRequest("m", target, MonitorType.TCP, 60, 10, null, null);
    }

    private static Map<String, String> errors(MonitorRequest request) {
        try {
            MonitorRequestValidator.validate(request);
            return Map.of();
        } catch (RequestValidationException e) {
            return e.getErrors();
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"http://example.com", "https://example.com:8443/health?x=1", "HTTPS://EXAMPLE.COM"})
    void acceptsHttpAndHttpsUrls(String target) {
        assertThat(errors(http(target))).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"ftp://example.com", "not a url", "http:///path", "http://", "example.com",
            "http://user:secret@example.com"})
    void rejectsInvalidHttpTargets(String target) {
        assertThat(errors(http(target))).containsKey("target");
    }

    @ParameterizedTest
    @ValueSource(strings = {"db.example.com:5432", "10.0.0.5:22", "localhost:80"})
    void acceptsValidTcpTargets(String target) {
        assertThat(errors(tcp(target))).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"db.example.com", "db.example.com:", ":5432", "db.example.com:0",
            "db.example.com:70000", "db.example.com:abc", "http://db.example.com:80", "[::1]:80"})
    void rejectsInvalidTcpTargets(String target) {
        assertThat(errors(tcp(target))).containsKey("target");
    }

    @Test
    void rejectsTimeoutNotSmallerThanInterval() {
        MonitorRequest request = new MonitorRequest("m", "http://example.com", MonitorType.HTTP, 30, 30, null, null);

        assertThat(errors(request)).containsKey("timeoutSeconds");
    }

    @Test
    void rejectsExpectedStatusOnTcpMonitor() {
        MonitorRequest request = new MonitorRequest("m", "db.example.com:5432", MonitorType.TCP, 60, 10, 200, null);

        assertThat(errors(request)).containsKey("expectedStatus");
    }

    @Test
    void reportsEveryProblemAtOnce() {
        MonitorRequest request = new MonitorRequest("m", "ftp://example.com", MonitorType.HTTP, 30, 30, null, null);

        assertThat(errors(request)).containsKeys("target", "timeoutSeconds");
    }
}