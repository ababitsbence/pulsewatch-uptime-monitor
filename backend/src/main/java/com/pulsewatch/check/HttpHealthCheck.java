package com.pulsewatch.check;

import com.pulsewatch.monitor.Monitor;
import com.pulsewatch.monitor.MonitorType;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

public class HttpHealthCheck implements HealthCheck {

    private static final int DEFAULT_EXPECTED_STATUS = 200;

    private final HttpClient client;

    public HttpHealthCheck(HttpClient client) {
        this.client = client;
    }

    public static HttpClient defaultClient() {
        return HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .followRedirects(HttpClient.Redirect.NEVER)
                .connectTimeout(Duration.ofSeconds(60))
                .build();
    }

    @Override
    public MonitorType type() {
        return MonitorType.HTTP;
    }

    @Override
    public CheckOutcome check(Monitor monitor) {
        long start = System.nanoTime();
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(monitor.getTarget()))
                    .timeout(Duration.ofSeconds(monitor.getTimeoutSeconds()))
                    .header("User-Agent", "PulseWatch/0.1")
                    .GET()
                    .build();
            HttpResponse<InputStream> response =
                    client.send(request, HttpResponse.BodyHandlers.ofInputStream());
            response.body().close();
            long elapsed = elapsedMillis(start);

            int expected = monitor.getExpectedStatus() != null
                    ? monitor.getExpectedStatus() : DEFAULT_EXPECTED_STATUS;
            if (response.statusCode() == expected) {
                return CheckOutcome.up(elapsed);
            }
            return CheckOutcome.down(elapsed,
                    "Expected status " + expected + " but got " + response.statusCode());
        } catch (HttpTimeoutException e) {
            return CheckOutcome.down(elapsedMillis(start),
                    "Timed out after " + monitor.getTimeoutSeconds() + " seconds");
        } catch (IOException | IllegalArgumentException e) {
            return CheckOutcome.down(elapsedMillis(start), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return CheckOutcome.down(elapsedMillis(start), "Check was interrupted");
        }
    }

    private static long elapsedMillis(long startNanos) {
        return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos);
    }
}