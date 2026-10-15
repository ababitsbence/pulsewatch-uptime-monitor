package com.pulsewatch.check;

import com.pulsewatch.AbstractIntegrationTest;
import com.pulsewatch.monitor.Monitor;
import com.pulsewatch.monitor.MonitorRepository;
import com.pulsewatch.monitor.MonitorType;
import com.pulsewatch.user.User;
import com.pulsewatch.user.UserRepository;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.TestPropertySource;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@TestPropertySource(properties = "app.ssrf.allow-private-targets=true")
class CheckSchedulingIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    UserRepository userRepository;

    @Autowired
    MonitorRepository monitorRepository;

    @Autowired
    CheckResultRepository checkResultRepository;

    @Autowired
    CheckRunner checkRunner;

    @Autowired
    Clock clock;

    private HttpServer server;
    private int serverPort;
    private Long userId;

    @BeforeEach
    void setUp() throws IOException {
        checkResultRepository.deleteAll();
        monitorRepository.deleteAll();
        userRepository.deleteAll();
        userId = userRepository.save(new User("owner@example.com", "hash")).getId();

        server = HttpServer.create(new InetSocketAddress(InetAddress.getByName("127.0.0.1"), 0), 0);
        server.createContext("/ok", exchange -> {
            exchange.sendResponseHeaders(200, -1);
            exchange.close();
        });
        server.start();
        serverPort = server.getAddress().getPort();
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
    }

    // ---- helpers ----

    private Monitor saveMonitor(String target, MonitorType type, boolean enabled) {
        Integer expectedStatus = type == MonitorType.HTTP ? 200 : null;
        return monitorRepository.save(
                new Monitor(userId, "m", target, type, 60, 5, expectedStatus, enabled));
    }

    private Monitor saveHttpMonitor() {
        return saveMonitor("http://127.0.0.1:" + serverPort + "/ok", MonitorType.HTTP, true);
    }

    private void addResult(Monitor monitor, Instant at) {
        checkResultRepository.save(new CheckResult(monitor.getId(), at, CheckStatus.UP, 10, null));
    }

    private List<CheckResult> resultsOf(Monitor monitor) {
        return checkResultRepository.findByMonitorIdOrderByCheckedAtDesc(monitor.getId(), PageRequest.of(0, 10));
    }

    private List<Long> dueIds() {
        return monitorRepository.findDueMonitors(Instant.now()).stream().map(Monitor::getId).toList();
    }

    private CheckScheduler scheduler() {
        return new CheckScheduler(monitorRepository, checkRunner, Runnable::run, clock, new CheckProperties(5));
    }

    private static int closedPort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0, 1, InetAddress.getByName("127.0.0.1"))) {
            return socket.getLocalPort();
        }
    }

    // ---- due query ----

    @Test
    void aMonitorThatWasNeverCheckedIsDue() {
        Monitor monitor = saveHttpMonitor();

        assertThat(dueIds()).containsExactly(monitor.getId());
    }

    @Test
    void aMonitorIsNotDueRightAfterACheck() {
        Monitor monitor = saveHttpMonitor();
        addResult(monitor, Instant.now().minus(10, ChronoUnit.SECONDS));

        assertThat(dueIds()).isEmpty();
    }

    @Test
    void aMonitorIsDueOnceItsIntervalHasPassed() {
        Monitor monitor = saveHttpMonitor();
        addResult(monitor, Instant.now().minus(61, ChronoUnit.SECONDS));

        assertThat(dueIds()).containsExactly(monitor.getId());
    }

    @Test
    void onlyTheNewestResultCounts() {
        Monitor monitor = saveHttpMonitor();
        addResult(monitor, Instant.now().minus(10, ChronoUnit.MINUTES));
        addResult(monitor, Instant.now().minus(10, ChronoUnit.SECONDS));

        assertThat(dueIds()).isEmpty();
    }

    @Test
    void aDisabledMonitorIsNeverDue() {
        saveMonitor("http://127.0.0.1:" + serverPort + "/ok", MonitorType.HTTP, false);

        assertThat(dueIds()).isEmpty();
    }

    // ---- scheduler run against real sockets ----

    @Test
    void checksDueMonitorsAndStoresTheResults() {
        Monitor http = saveHttpMonitor();
        Monitor tcp = saveMonitor("127.0.0.1:" + serverPort, MonitorType.TCP, true);

        scheduler().runDueChecks();

        assertThat(resultsOf(http)).hasSize(1);
        assertThat(resultsOf(http).get(0).getStatus()).isEqualTo(CheckStatus.UP);
        assertThat(resultsOf(tcp)).hasSize(1);
        assertThat(resultsOf(tcp).get(0).getStatus()).isEqualTo(CheckStatus.UP);
    }

    @Test
    void recordsDownForAnUnreachableTarget() throws IOException {
        Monitor monitor = saveMonitor("http://127.0.0.1:" + closedPort() + "/", MonitorType.HTTP, true);

        scheduler().runDueChecks();

        assertThat(resultsOf(monitor)).hasSize(1);
        assertThat(resultsOf(monitor).get(0).getStatus()).isEqualTo(CheckStatus.DOWN);
        assertThat(resultsOf(monitor).get(0).getError()).isNotBlank();
    }

    @Test
    void doesNotCheckAgainBeforeTheIntervalPasses() {
        Monitor monitor = saveHttpMonitor();

        scheduler().runDueChecks();
        scheduler().runDueChecks();

        assertThat(resultsOf(monitor)).hasSize(1);
    }

    @Test
    void skipsDisabledMonitors() {
        Monitor monitor = saveMonitor("http://127.0.0.1:" + serverPort + "/ok", MonitorType.HTTP, false);

        scheduler().runDueChecks();

        assertThat(resultsOf(monitor)).isEmpty();
    }
}