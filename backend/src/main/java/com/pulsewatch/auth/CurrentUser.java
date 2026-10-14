package com.pulsewatch.auth;

import com.pulsewatch.common.InvalidCredentialsException;
import org.springframework.security.oauth2.jwt.Jwt;

public final class CurrentUser {

    private CurrentUser() {}

    public static Long id(Jwt jwt) {
        String subject = jwt.getSubject();
        if (subject == null) {
            throw new InvalidCredentialsException("Invalid token");
        }
        try {
            return Long.valueOf(subject);
        } catch (NumberFormatException e) {
            throw new InvalidCredentialsException("Invalid token");
        }
    }
}