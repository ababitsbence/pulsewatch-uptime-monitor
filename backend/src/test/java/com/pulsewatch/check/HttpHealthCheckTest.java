package com.pulsewatch.check;

import com.pulsewatch.monitor.Monitor;
import com.pulsewatch.monitor.MonitorType;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;

import static org.assertj.core.api.Assertions.assertThat;

class HttpHealthCheckTest {

    private final HttpHealthCheck check = new HttpHealthCheck(HttpHealthCheck.defaultClient());
    private HttpServer server;
    private String baseUrl;

    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress(InetAddress.getByName("127.0.0.1"), 0), 0);
        server.createContext("/ok", exchange -> respond(exchange, 200));
        server.createContext("/missing", exchange -> respond(exchange, 404));
        server.createContext("/redirect", exchange -> {
            exchange.getResponseHeaders().add("Location", "/ok");
            respond(exchange, 302);
        });
        server.createContext("/slow", exchange -> {
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            respond(exchange, 200);
        });
        server.start();
        baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
    }

    private static void respond(HttpExchange exchange, int status) throws IOException {
        exchange.sendResponseHeaders(status, -1);
        exchange.close();
    }

    private static Monitor monitor(String target, int timeoutSeconds, Integer expectedStatus) {
        return new Monitor(1L, "test", target, MonitorType.HTTP, 30, timeoutSeconds, expectedStatus, true);
    }

    @Test
    void reportsUpWhenStatusMatchesDefault() {
        CheckOutcome outcome = check.check(monitor(baseUrl + "/ok", 5, null));

        assertThat(outcome.status()).isEqualTo(CheckStatus.UP);
        assertThat(outcome.error()).isNull();
        assertThat(outcome.responseTimeMs()).isGreaterThanOrEqualTo(0);
    }

    @Test
    void reportsDownWhenStatusDiffers() {
        CheckOutcome outcome = check.check(monitor(baseUrl + "/missing", 5, null));

        assertThat(outcome.status()).isEqualTo(CheckStatus.DOWN);
        assertThat(outcome.error()).contains("404");
    }

    @Test
    void reportsUpWhenCustomExpectedStatusMatches() {
        CheckOutcome outcome = check.check(monitor(baseUrl + "/missing", 5, 404));

        assertThat(outcome.status()).isEqualTo(CheckStatus.UP);
    }

    @Test
    void doesNotFollowRedirects() {
        CheckOutcome outcome = check.check(monitor(baseUrl + "/redirect", 5, null));

        assertThat(outcome.status()).isEqualTo(CheckStatus.DOWN);
        assertThat(outcome.error()).contains("302");
    }

    @Test
    void reportsDownOnTimeout() {
        CheckOutcome outcome = check.check(monitor(baseUrl + "/slow", 1, null));

        assertThat(outcome.status()).isEqualTo(CheckStatus.DOWN);
        assertThat(outcome.error()).contains("Timed out");
    }

    @Test
    void reportsDownWhenConnectionIsRefused() throws IOException {
        int closedPort;
        try (ServerSocket socket = new ServerSocket(0, 1, InetAddress.getByName("127.0.0.1"))) {
            closedPort = socket.getLocalPort();
        }

        CheckOutcome outcome = check.check(monitor("http://127.0.0.1:" + closedPort + "/", 5, null));

        assertThat(outcome.status()).isEqualTo(CheckStatus.DOWN);
        assertThat(outcome.error()).isNotBlank();
    }

    @Test
    void reportsDownForUnusableTarget() {
        CheckOutcome outcome = check.check(monitor("not a url", 5, null));

        assertThat(outcome.status()).isEqualTo(CheckStatus.DOWN);
    }
}