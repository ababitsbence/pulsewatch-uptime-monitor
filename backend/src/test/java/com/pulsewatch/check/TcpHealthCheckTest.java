package com.pulsewatch.check;

import com.pulsewatch.monitor.Monitor;
import com.pulsewatch.monitor.MonitorType;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;

import static org.assertj.core.api.Assertions.assertThat;

class TcpHealthCheckTest {

    private final TcpHealthCheck check = new TcpHealthCheck();

    private static Monitor monitor(String target) {
        return new Monitor(1L, "test", target, MonitorType.TCP, 30, 2, null, true);
    }

    @Test
    void reportsUpWhenPortAcceptsConnections() throws IOException {
        try (ServerSocket server = new ServerSocket(0, 50, InetAddress.getByName("127.0.0.1"))) {
            CheckOutcome outcome = check.check(monitor("127.0.0.1:" + server.getLocalPort()));

            assertThat(outcome.status()).isEqualTo(CheckStatus.UP);
            assertThat(outcome.error()).isNull();
        }
    }

    @Test
    void reportsDownWhenConnectionIsRefused() throws IOException {
        int closedPort;
        try (ServerSocket socket = new ServerSocket(0, 1, InetAddress.getByName("127.0.0.1"))) {
            closedPort = socket.getLocalPort();
        }

        CheckOutcome outcome = check.check(monitor("127.0.0.1:" + closedPort));

        assertThat(outcome.status()).isEqualTo(CheckStatus.DOWN);
        assertThat(outcome.error()).isNotBlank();
    }

    @Test
    void reportsDownForTargetWithoutPort() {
        CheckOutcome outcome = check.check(monitor("nonsense"));

        assertThat(outcome.status()).isEqualTo(CheckStatus.DOWN);
        assertThat(outcome.error()).contains("Invalid target");
    }

    @Test
    void reportsDownForPortOutOfRange() {
        CheckOutcome outcome = check.check(monitor("127.0.0.1:99999"));

        assertThat(outcome.status()).isEqualTo(CheckStatus.DOWN);
        assertThat(outcome.error()).contains("Invalid target");
    }
}