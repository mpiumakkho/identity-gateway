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
    private final OperatorSessionService operatorSessionService;

    public AuthService(
            OperatorUserRepository operatorUserRepository,
            PasswordEncoder passwordEncoder,
            OperatorSessionService operatorSessionService
    ) {
        this.operatorUserRepository = operatorUserRepository;
        this.passwordEncoder = passwordEncoder;
        this.operatorSessionService = operatorSessionService;
    }

    @Transactional
    public LoginResponse login(LoginRequest request) {
        OperatorUser user = operatorUserRepository.findByUsernameIgnoreCase(request.username())
                .filter(OperatorUser::isEnabled)
                .orElseThrow(AuthenticationFailedException::new);

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new AuthenticationFailedException();
        }

        IssuedOperatorSession session = operatorSessionService.createSession(user);

        return new LoginResponse(
                user.getId(),
                user.getUsername(),
                user.getDisplayName(),
                user.getRole(),
                Instant.now(),
                session.accessToken(),
                session.expiresAt()
        );
    }
}