package com.pulsewatch.user;

import com.pulsewatch.auth.UserResponse;
import com.pulsewatch.common.InvalidCredentialsException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserRepository userRepository;

    public UserController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping("/me")
    public UserResponse me(@AuthenticationPrincipal Jwt jwt) {
        String subject = jwt.getSubject();
        if (subject == null) {
            throw new InvalidCredentialsException("Invalid token");
        }
        User user = userRepository.findById(Long.valueOf(subject))
                .orElseThrow(() -> new InvalidCredentialsException("Account no longer exists"));
        return new UserResponse(user.getId(), user.getEmail());
    }
}