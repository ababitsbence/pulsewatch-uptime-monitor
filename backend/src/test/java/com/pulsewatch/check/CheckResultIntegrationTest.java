package com.pulsewatch.check;

import com.jayway.jsonpath.JsonPath;
import com.pulsewatch.AbstractIntegrationTest;
import com.pulsewatch.monitor.Monitor;
import com.pulsewatch.monitor.MonitorRepository;
import com.pulsewatch.monitor.MonitorType;
import com.pulsewatch.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CheckResultIntegrationTest extends AbstractIntegrationTest {

    private static final String PASSWORD = "supersecret";

    @Autowired
    MockMvc mockMvc;

    @Autowired
    UserRepository userRepository;

    @Autowired
    MonitorRepository monitorRepository;

    @Autowired
    CheckResultRepository checkResultRepository;

    @BeforeEach
    void cleanDatabase() {
        checkResultRepository.deleteAll();
        monitorRepository.deleteAll();
        userRepository.deleteAll();
    }

    // ---- helpers ----

    private String tokenFor(String email) throws Exception {
        String credentials = "{\"email\":\"" + email + "\",\"password\":\"" + PASSWORD + "\"}";
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

    private Long monitorFor(String email) {
        Long userId = userRepository.findByEmail(email).orElseThrow().getId();
        return monitorRepository.save(new Monitor(
                userId, "Example", "https://example.com", MonitorType.HTTP, 60, 10, 200, true)).getId();
    }

    private void addResult(Long monitorId, Instant at, CheckStatus status, long ms, String error) {
        checkResultRepository.save(new CheckResult(monitorId, at, status, ms, error));
    }

    // ---- tests ----

    @Test
    void rejectsRequestWithoutToken() throws Exception {
        mockMvc.perform(get("/api/monitors/1/results")).andExpect(status().isUnauthorized());
    }

    @Test
    void returnsHistoryNewestFirst() throws Exception {
        String token = tokenFor("a@example.com");
        Long monitorId = monitorFor("a@example.com");
        Instant now = Instant.now();
        addResult(monitorId, now.minus(2, ChronoUnit.MINUTES), CheckStatus.UP, 120, null);
        addResult(monitorId, now.minus(1, ChronoUnit.MINUTES), CheckStatus.DOWN, 5000, "Timed out after 5 seconds");

        mockMvc.perform(get("/api/monitors/" + monitorId + "/results")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].status").value("DOWN"))
                .andExpect(jsonPath("$[0].error").value("Timed out after 5 seconds"))
                .andExpect(jsonPath("$[0].responseTimeMs").value(5000))
                .andExpect(jsonPath("$[1].status").value("UP"))
                .andExpect(jsonPath("$[1].error").doesNotExist());
    }

    @Test
    void returnsEmptyListForMonitorWithoutResults() throws Exception {
        String token = tokenFor("a@example.com");
        Long monitorId = monitorFor("a@example.com");

        mockMvc.perform(get("/api/monitors/" + monitorId + "/results")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void limitCapsTheNumberOfResults() throws Exception {
        String token = tokenFor("a@example.com");
        Long monitorId = monitorFor("a@example.com");
        Instant now = Instant.now();
        for (int i = 0; i < 5; i++) {
            addResult(monitorId, now.minus(i, ChronoUnit.MINUTES), CheckStatus.UP, 100, null);
        }

        mockMvc.perform(get("/api/monitors/" + monitorId + "/results?limit=2")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void rejectsLimitOutsideAllowedRange() throws Exception {
        String token = tokenFor("a@example.com");
        Long monitorId = monitorFor("a@example.com");

        mockMvc.perform(get("/api/monitors/" + monitorId + "/results?limit=0")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.limit").exists());
        mockMvc.perform(get("/api/monitors/" + monitorId + "/results?limit=201")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/monitors/" + monitorId + "/results?limit=abc")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void otherUserCannotReadResults() throws Exception {
        tokenFor("a@example.com");
        String tokenB = tokenFor("b@example.com");
        Long monitorId = monitorFor("a@example.com");
        addResult(monitorId, Instant.now(), CheckStatus.UP, 100, null);

        mockMvc.perform(get("/api/monitors/" + monitorId + "/results")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenB)))
                .andExpect(status().isNotFound());
    }

    @Test
    void returnsNotFoundForUnknownMonitor() throws Exception {
        String token = tokenFor("a@example.com");

        mockMvc.perform(get("/api/monitors/999999/results")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isNotFound());
    }

    @Test
    void deletingAMonitorDeletesItsResults() throws Exception {
        String token = tokenFor("a@example.com");
        Long monitorId = monitorFor("a@example.com");
        addResult(monitorId, Instant.now(), CheckStatus.UP, 100, null);
        assertThat(checkResultRepository.count()).isEqualTo(1);

        mockMvc.perform(delete("/api/monitors/" + monitorId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isNoContent());

        assertThat(checkResultRepository.count()).isZero();
    }
}