package com.pulsewatch.auth;

import com.jayway.jsonpath.JsonPath;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.pulsewatch.AbstractIntegrationTest;
import com.pulsewatch.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.test.web.servlet.MockMvc;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SecurityIntegrationTest extends AbstractIntegrationTest {

    private static final String EMAIL = "user@example.com";
    private static final String PASSWORD = "supersecret";

    @Autowired
    MockMvc mockMvc;

    @Autowired
    UserRepository userRepository;

    @Autowired
    JwtEncoder jwtEncoder;

    @BeforeEach
    void cleanUsers() {
        userRepository.deleteAll();
    }

    private String body(String email, String password) {
        return "{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}";
    }

    private String registerAndLogin() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(EMAIL, PASSWORD)))
                .andExpect(status().isCreated());
        String response = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(EMAIL, PASSWORD)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(response, "$.accessToken");
    }

    private String tokenFor(JwtEncoder encoder, Instant issuedAt, Instant expiresAt) {
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("pulsewatch")
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .subject("1")
                .claim("email", "someone@example.com")
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    @Test
    void rejectsRequestWithoutToken() throws Exception {
        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Authentication required"));
    }

    @Test
    void rejectsGarbageToken() throws Exception {
        mockMvc.perform(get("/api/users/me").header("Authorization", "Bearer not.a.jwt"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsExpiredToken() throws Exception {
        Instant twoHoursAgo = Instant.now().minus(Duration.ofHours(2));
        String expired = tokenFor(jwtEncoder, twoHoursAgo, twoHoursAgo.plus(Duration.ofHours(1)));

        mockMvc.perform(get("/api/users/me").header("Authorization", "Bearer " + expired))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsTokenSignedWithDifferentKey() throws Exception {
        SecretKeySpec otherKey = new SecretKeySpec(
                "a-completely-different-secret-of-32-plus-chars".getBytes(StandardCharsets.UTF_8),
                "HmacSHA256");
        JwtEncoder otherEncoder = new NimbusJwtEncoder(new ImmutableSecret<>(otherKey));
        String forged = tokenFor(otherEncoder, Instant.now(), Instant.now().plus(Duration.ofHours(1)));

        mockMvc.perform(get("/api/users/me").header("Authorization", "Bearer " + forged))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void returnsCurrentUserForValidToken() throws Exception {
        String token = registerAndLogin();

        mockMvc.perform(get("/api/users/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(EMAIL))
                .andExpect(jsonPath("$.id").isNumber());
    }

    @Test
    void rejectsValidTokenOfDeletedUser() throws Exception {
        String token = registerAndLogin();
        userRepository.deleteAll();

        mockMvc.perform(get("/api/users/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }
}