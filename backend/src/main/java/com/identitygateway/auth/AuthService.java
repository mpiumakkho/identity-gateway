package com.identitygateway.auth;

import com.identitygateway.common.error.AuthenticationFailedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class AuthService {

    private final OperatorUserRepository operatorUserRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(OperatorUserRepository operatorUserRepository, PasswordEncoder passwordEncoder) {
        this.operatorUserRepository = operatorUserRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        OperatorUser user = operatorUserRepository.findByUsernameIgnoreCase(request.username())
                .filter(OperatorUser::isEnabled)
                .orElseThrow(AuthenticationFailedException::new);

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new AuthenticationFailedException();
        }

        return new LoginResponse(
                user.getId(),
                user.getUsername(),
                user.getDisplayName(),
                user.getRole(),
                Instant.now()
        );
    }
}