package com.pulsewatch.auth;

import com.pulsewatch.common.EmailAlreadyExistsException;
import com.pulsewatch.user.User;
import com.pulsewatch.user.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public UserResponse register(RegisterRequest request) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        if (userRepository.existsByEmail(email)) {
            throw new EmailAlreadyExistsException();
        }
        try {
            User user = userRepository.saveAndFlush(
                    new User(email, passwordEncoder.encode(request.password())));
            return new UserResponse(user.getId(), user.getEmail());
        } catch (DataIntegrityViolationException e) {
            // two requests registering the same email at the same moment
            throw new EmailAlreadyExistsException();
        }
    }
}