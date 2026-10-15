package com.pulsewatch.ssrf;

import com.jayway.jsonpath.JsonPath;
import com.pulsewatch.AbstractIntegrationTest;
import com.pulsewatch.TestDnsConfig;
import com.pulsewatch.check.CheckProperties;
import com.pulsewatch.check.CheckResult;
import com.pulsewatch.check.CheckResultRepository;
import com.pulsewatch.check.CheckRunner;
import com.pulsewatch.check.CheckScheduler;
import com.pulsewatch.check.CheckStatus;
import com.pulsewatch.monitor.Monitor;
import com.pulsewatch.monitor.MonitorRepository;
import com.pulsewatch.monitor.MonitorType;
import com.pulsewatch.user.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.Clock;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SsrfIntegrationTest extends AbstractIntegrationTest {

    private static final String PASSWORD = "supersecret";
    private static final String EMAIL = "a@example.com";

    @Autowired
    MockMvc mockMvc;

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

    @BeforeEach
    void setUp() {
        TestDnsConfig.clear();
        checkResultRepository.deleteAll();
        monitorRepository.deleteAll();
        userRepository.deleteAll();
    }

    @AfterEach
    void clearDns() {
        TestDnsConfig.clear();
    }

    // ---- helpers ----

    private String token() throws Exception {
        String credentials = "{\"email\":\"" + EMAIL + "\",\"password\":\"" + PASSWORD + "\"}";
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credentials))
                .andExpect(status().isCreated());
        String response = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credentials))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(response, "$.accessToken");
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }

    private static String monitorJson(String target, String type) {
        return """
                {"name":"m","target":"%s","type":"%s","intervalSeconds":60,"timeoutSeconds":5}"""
                .formatted(target, type);
    }

    private ResultActions createMonitor(String token, String target, String type) throws Exception {
        return mockMvc.perform(post("/api/monitors")
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(monitorJson(target, type)));
    }

    private long createdId(ResultActions created) throws Exception {
        String body = created.andReturn().getResponse().getContentAsString();
        Number id = JsonPath.read(body, "$.id");
        return id.longValue();
    }

    private void runScheduler() {
        new CheckScheduler(monitorRepository, checkRunner, Runnable::run, clock, new CheckProperties(5))
                .runDueChecks();
    }

    private List<CheckResult> resultsOf(long monitorId) {
        return checkResultRepository.findByMonitorIdOrderByCheckedAtDesc(monitorId, PageRequest.of(0, 5));
    }

    // ---- save time ----

    @ParameterizedTest
    @ValueSource(strings = {
            "http://127.0.0.1/",
            "http://127.0.0.1:8080/admin",
            "http://10.0.0.5/health",
            "http://192.168.1.1/",
            "http://172.16.0.1/",
            "http://169.254.169.254/latest/meta-data/",
            "http://0.0.0.0/",
            "http://[::1]/",
            "http://[::ffff:127.0.0.1]/"
    })
    void rejectsHttpMonitorsPointingAtPrivateAddresses(String target) throws Exception {
        createMonitor(token(), target, "HTTP")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.target").exists());
    }

    @ParameterizedTest
    @ValueSource(strings = {"10.0.0.5:22", "127.0.0.1:5432", "169.254.169.254:80", "2130706433:80"})
    void rejectsTcpMonitorsPointingAtPrivateAddresses(String target) throws Exception {
        createMonitor(token(), target, "TCP")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.target").exists());
    }

    @Test
    void rejectsHostnameThatResolvesToPrivateAddress() throws Exception {
        TestDnsConfig.setRecords("internal.test", "10.0.0.5");

        createMonitor(token(), "http://internal.test/", "HTTP")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.target", not(containsString("10.0.0.5"))));
    }

    @Test
    void rejectsHostnameWhenAnyRecordIsPrivate() throws Exception {
        TestDnsConfig.setRecords("mixed.test", "93.184.216.34", "10.0.0.5");

        createMonitor(token(), "http://mixed.test/", "HTTP").andExpect(status().isBadRequest());
    }

    @Test
    void acceptsHostnameThatResolvesToPublicAddress() throws Exception {
        TestDnsConfig.setRecords("public.test", "93.184.216.34");

        createMonitor(token(), "https://public.test/", "HTTP").andExpect(status().isCreated());
    }

    @Test
    void acceptsHostThatDoesNotResolve() throws Exception {
        createMonitor(token(), "https://unresolvable.test/", "HTTP").andExpect(status().isCreated());
    }

    @Test
    void rejectsUpdatingAMonitorToAPrivateTarget() throws Exception {
        TestDnsConfig.setRecords("public.test", "93.184.216.34");
        String token = token();
        long id = createdId(createMonitor(token, "https://public.test/", "HTTP").andExpect(status().isCreated()));

        mockMvc.perform(put("/api/monitors/" + id)
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(monitorJson("http://10.0.0.5/", "HTTP")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.target").exists());

        mockMvc.perform(get("/api/monitors/" + id).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.target").value("https://public.test/"));
    }

    // ---- check time ----

    @Test
    void blocksTheCheckWhenAHostnameStartsResolvingToAPrivateAddress() throws Exception {
        TestDnsConfig.setRecords("rebind.test", "93.184.216.34");
        long id = createdId(createMonitor(token(), "http://rebind.test/", "HTTP").andExpect(status().isCreated()));

        TestDnsConfig.setRecords("rebind.test", "10.0.0.5");
        runScheduler();

        List<CheckResult> results = resultsOf(id);
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getStatus()).isEqualTo(CheckStatus.DOWN);
        assertThat(results.get(0).getError()).contains("private or reserved");
    }

    @Test
    void blocksTheCheckForAPrivateTargetThatWasStoredDirectly() throws Exception {
        token();
        Long userId = userRepository.findByEmail(EMAIL).orElseThrow().getId();
        Monitor stored = monitorRepository.save(new Monitor(
                userId, "direct", "http://127.0.0.1:1/", MonitorType.HTTP, 60, 5, 200, true));

        runScheduler();

        List<CheckResult> results = resultsOf(stored.getId());
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getStatus()).isEqualTo(CheckStatus.DOWN);
        assertThat(results.get(0).getError()).contains("private or reserved");
    }
}