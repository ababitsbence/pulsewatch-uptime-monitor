package com.pulsewatch.auth;

import com.jayway.jsonpath.JsonPath;
import com.pulsewatch.AbstractIntegrationTest;
import com.pulsewatch.user.User;
import com.pulsewatch.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class LoginIntegrationTest extends AbstractIntegrationTest {

    private static final String EMAIL = "user@example.com";
    private static final String PASSWORD = "supersecret";

    @Autowired
    MockMvc mockMvc;

    @Autowired
    UserRepository userRepository;

    @Autowired
    JwtDecoder jwtDecoder;

    @BeforeEach
    void registerUser() throws Exception {
        userRepository.deleteAll();
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(EMAIL, PASSWORD)))
                .andExpect(status().isCreated());
    }

    private String body(String email, String password) {
        return "{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}";
    }

    private String loginAndGetBody(String email, String password, int expectedStatus) throws Exception {
        return mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(email, password)))
                .andExpect(status().is(expectedStatus))
                .andReturn().getResponse().getContentAsString();
    }

    @Test
    void returnsValidTokenForCorrectCredentials() throws Exception {
        String responseBody = loginAndGetBody(EMAIL, PASSWORD, 200);

        String token = JsonPath.read(responseBody, "$.accessToken");
        assertThat(JsonPath.<String>read(responseBody, "$.tokenType")).isEqualTo("Bearer");
        assertThat(JsonPath.<Integer>read(responseBody, "$.expiresIn")).isEqualTo(3600);

        Jwt jwt = jwtDecoder.decode(token);
        User saved = userRepository.findByEmail(EMAIL).orElseThrow();
        assertThat(jwt.getSubject()).isEqualTo(String.valueOf(saved.getId()));
        assertThat(jwt.getClaimAsString("email")).isEqualTo(EMAIL);
    }

    @Test
    void acceptsEmailInDifferentCase() throws Exception {
        loginAndGetBody("USER@Example.COM", PASSWORD, 200);
    }

    @Test
    void rejectsWrongPassword() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(EMAIL, "wrongpassword")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Invalid email or password"));
    }

    @Test
    void wrongPasswordAndUnknownEmailGiveIdenticalResponses() throws Exception {
        String wrongPassword = loginAndGetBody(EMAIL, "wrongpassword", 401);
        String unknownEmail = loginAndGetBody("nobody@example.com", PASSWORD, 401);

        assertThat(wrongPassword).isEqualTo(unknownEmail);
    }

    @Test
    void rejectsBlankFields() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("", "")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.email").exists())
                .andExpect(jsonPath("$.details.password").exists());
    }
}