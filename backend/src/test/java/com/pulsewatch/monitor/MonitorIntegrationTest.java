package com.pulsewatch.monitor;

import com.jayway.jsonpath.JsonPath;
import com.pulsewatch.AbstractIntegrationTest;
import com.pulsewatch.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class MonitorIntegrationTest extends AbstractIntegrationTest {

    private static final String PASSWORD = "supersecret";

    @Autowired
    MockMvc mockMvc;

    @Autowired
    UserRepository userRepository;

    @Autowired
    MonitorRepository monitorRepository;

    @BeforeEach
    void cleanDatabase() {
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

    private static String monitorJson(String name, String target, String type, int interval, int timeout) {
        return """
                {"name":"%s","target":"%s","type":"%s","intervalSeconds":%d,"timeoutSeconds":%d}"""
                .formatted(name, target, type, interval, timeout);
    }

    private static String validHttpMonitor() {
        return monitorJson("Example", "https://example.com", "HTTP", 60, 10);
    }

    private long createMonitor(String token, String json) throws Exception {
        String response = mockMvc.perform(post("/api/monitors")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Number id = JsonPath.read(response, "$.id");
        return id.longValue();
    }

    private int listSize(String token) throws Exception {
        String response = mockMvc.perform(get("/api/monitors")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        Integer size = JsonPath.read(response, "$.length()");
        return size;
    }

    // ---- authentication ----

    @Test
    void rejectsRequestsWithoutToken() throws Exception {
        mockMvc.perform(get("/api/monitors")).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/monitors")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validHttpMonitor()))
                .andExpect(status().isUnauthorized());
    }

    // ---- create ----

    @Test
    void createsHttpMonitorWithDefaults() throws Exception {
        String token = tokenFor("a@example.com");

        mockMvc.perform(post("/api/monitors")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validHttpMonitor()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.name").value("Example"))
                .andExpect(jsonPath("$.type").value("HTTP"))
                .andExpect(jsonPath("$.expectedStatus").value(200))
                .andExpect(jsonPath("$.enabled").value(true));
    }

    @Test
    void createsTcpMonitor() throws Exception {
        String token = tokenFor("a@example.com");

        mockMvc.perform(post("/api/monitors")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(monitorJson("Database", "db.example.com:5432", "TCP", 60, 10)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.type").value("TCP"))
                .andExpect(jsonPath("$.target").value("db.example.com:5432"));
    }

    // ---- read, update, delete ----

    @Test
    void getReturnsOwnMonitor() throws Exception {
        String token = tokenFor("a@example.com");
        long id = createMonitor(token, validHttpMonitor());

        mockMvc.perform(get("/api/monitors/" + id).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.target").value("https://example.com"));
    }

    @Test
    void updatesMonitor() throws Exception {
        String token = tokenFor("a@example.com");
        long id = createMonitor(token, validHttpMonitor());

        mockMvc.perform(put("/api/monitors/" + id)
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(monitorJson("Renamed", "https://example.org", "HTTP", 120, 20)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/monitors/" + id).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(jsonPath("$.name").value("Renamed"))
                .andExpect(jsonPath("$.target").value("https://example.org"))
                .andExpect(jsonPath("$.intervalSeconds").value(120))
                .andExpect(jsonPath("$.timeoutSeconds").value(20));
    }

    @Test
    void deletesMonitor() throws Exception {
        String token = tokenFor("a@example.com");
        long id = createMonitor(token, validHttpMonitor());

        mockMvc.perform(delete("/api/monitors/" + id).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/monitors/" + id).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isNotFound());
    }

    @Test
    void returnsNotFoundForUnknownId() throws Exception {
        String token = tokenFor("a@example.com");

        mockMvc.perform(get("/api/monitors/999999").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void rejectsNonNumericId() throws Exception {
        String token = tokenFor("a@example.com");

        mockMvc.perform(get("/api/monitors/abc").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isBadRequest());
    }

    // ---- per-user isolation ----

    @Test
    void listShowsOnlyOwnMonitors() throws Exception {
        String tokenA = tokenFor("a@example.com");
        String tokenB = tokenFor("b@example.com");
        createMonitor(tokenA, validHttpMonitor());
        createMonitor(tokenA, monitorJson("Second", "https://example.org", "HTTP", 60, 10));
        createMonitor(tokenB, validHttpMonitor());

        assertThat(listSize(tokenA)).isEqualTo(2);
        assertThat(listSize(tokenB)).isEqualTo(1);
    }

    @Test
    void otherUserCannotReadUpdateOrDeleteMonitor() throws Exception {
        String tokenA = tokenFor("a@example.com");
        String tokenB = tokenFor("b@example.com");
        long id = createMonitor(tokenA, validHttpMonitor());

        mockMvc.perform(get("/api/monitors/" + id).header(HttpHeaders.AUTHORIZATION, bearer(tokenB)))
                .andExpect(status().isNotFound());

        mockMvc.perform(put("/api/monitors/" + id)
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenB))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(monitorJson("Hijacked", "https://evil.example", "HTTP", 60, 10)))
                .andExpect(status().isNotFound());

        mockMvc.perform(delete("/api/monitors/" + id).header(HttpHeaders.AUTHORIZATION, bearer(tokenB)))
                .andExpect(status().isNotFound());

        // the owner's monitor is untouched
        mockMvc.perform(get("/api/monitors/" + id).header(HttpHeaders.AUTHORIZATION, bearer(tokenA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Example"));
    }

    // ---- validation ----

    @Test
    void rejectsMissingFields() throws Exception {
        String token = tokenFor("a@example.com");

        mockMvc.perform(post("/api/monitors")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.name").exists())
                .andExpect(jsonPath("$.details.target").exists())
                .andExpect(jsonPath("$.details.type").exists())
                .andExpect(jsonPath("$.details.intervalSeconds").exists())
                .andExpect(jsonPath("$.details.timeoutSeconds").exists());
    }

    @Test
    void rejectsInvalidHttpTarget() throws Exception {
        String token = tokenFor("a@example.com");

        mockMvc.perform(post("/api/monitors")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(monitorJson("Bad", "ftp://example.com", "HTTP", 60, 10)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.target").exists());
    }

    @Test
    void rejectsMalformedTcpTarget() throws Exception {
        String token = tokenFor("a@example.com");

        mockMvc.perform(post("/api/monitors")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(monitorJson("Bad", "db.example.com", "TCP", 60, 10)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.target").exists());
    }

    @Test
    void rejectsIntervalBelowMinimum() throws Exception {
        String token = tokenFor("a@example.com");

        mockMvc.perform(post("/api/monitors")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(monitorJson("Fast", "https://example.com", "HTTP", 10, 5)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.intervalSeconds").exists());
    }

    @Test
    void rejectsTimeoutNotSmallerThanInterval() throws Exception {
        String token = tokenFor("a@example.com");

        mockMvc.perform(post("/api/monitors")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(monitorJson("Slow", "https://example.com", "HTTP", 30, 30)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.timeoutSeconds").exists());
    }

    @Test
    void rejectsExpectedStatusOnTcpMonitor() throws Exception {
        String token = tokenFor("a@example.com");
        String json = """
                {"name":"Db","target":"db.example.com:5432","type":"TCP","intervalSeconds":60,"timeoutSeconds":10,"expectedStatus":200}""";

        mockMvc.perform(post("/api/monitors")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.expectedStatus").exists());
    }
}