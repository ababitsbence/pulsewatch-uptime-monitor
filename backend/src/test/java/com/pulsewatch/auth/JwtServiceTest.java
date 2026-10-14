package com.pulsewatch.auth;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private static final String SECRET = "unit-test-secret-that-is-at-least-32-chars-long";
    private static final String OTHER_SECRET = "a-completely-different-secret-of-32-plus-chars";

    private static SecretKey key(String secret) {
        return new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    }

    private static JwtService serviceWith(String secret, Clock clock) {
        JwtProperties properties = new JwtProperties(secret, Duration.ofHours(1));
        return new JwtService(new NimbusJwtEncoder(new ImmutableSecret<>(key(secret))), properties, clock);
    }

    private static JwtDecoder decoder() {
        return NimbusJwtDecoder.withSecretKey(key(SECRET)).macAlgorithm(MacAlgorithm.HS256).build();
    }

    @Test
    void generatesTokenWithExpectedClaims() {
        String token = serviceWith(SECRET, Clock.systemUTC()).generateToken(42L, "user@example.com");

        Jwt jwt = decoder().decode(token);

        assertThat(jwt.getSubject()).isEqualTo("42");
        assertThat(jwt.getClaimAsString("email")).isEqualTo("user@example.com");
        assertThat(jwt.getClaimAsString("iss")).isEqualTo("pulsewatch");
        assertThat(jwt.getIssuedAt()).isNotNull();
        assertThat(jwt.getExpiresAt()).isNotNull();
        assertThat(Duration.between(jwt.getIssuedAt(), jwt.getExpiresAt())).isEqualTo(Duration.ofHours(1));
    }

    @Test
    void rejectsExpiredToken() {
        Clock twoHoursAgo = Clock.fixed(Instant.now().minus(Duration.ofHours(2)), ZoneOffset.UTC);
        String token = serviceWith(SECRET, twoHoursAgo).generateToken(42L, "user@example.com");

        assertThatThrownBy(() -> decoder().decode(token)).isInstanceOf(JwtException.class);
    }

    @Test
    void rejectsTokenSignedWithDifferentKey() {
        String token = serviceWith(OTHER_SECRET, Clock.systemUTC()).generateToken(42L, "user@example.com");

        assertThatThrownBy(() -> decoder().decode(token)).isInstanceOf(JwtException.class);
    }

    @Test
    void rejectsTamperedPayload() {
        String token = serviceWith(SECRET, Clock.systemUTC()).generateToken(42L, "user@example.com");
        String[] parts = token.split("\\.");
        String forgedPayload = Base64.getUrlEncoder().withoutPadding()
                .encodeToString("{\"sub\":\"1\",\"email\":\"evil@example.com\"}".getBytes(StandardCharsets.UTF_8));
        String forged = parts[0] + "." + forgedPayload + "." + parts[2];

        assertThatThrownBy(() -> decoder().decode(forged)).isInstanceOf(JwtException.class);
    }
}