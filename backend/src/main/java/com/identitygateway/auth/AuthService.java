package com.identitygateway.auth;

import com.identitygateway.audit.AuditEventType;
import com.identitygateway.audit.AuditService;
import com.identitygateway.common.error.AuthenticationFailedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

@Service
public class AuthService {

    private final OperatorUserRepository operatorUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final OperatorSessionService operatorSessionService;
    private final AuditService auditService;

    public AuthService(
            OperatorUserRepository operatorUserRepository,
            PasswordEncoder passwordEncoder,
            OperatorSessionService operatorSessionService,
            AuditService auditService
    ) {
        this.operatorUserRepository = operatorUserRepository;
        this.passwordEncoder = passwordEncoder;
        this.operatorSessionService = operatorSessionService;
        this.auditService = auditService;
    }

    @Transactional(noRollbackFor = AuthenticationFailedException.class)
    public LoginResponse login(LoginRequest request) {
        Optional<OperatorUser> userResult = operatorUserRepository.findByUsernameIgnoreCase(request.username())
                .filter(OperatorUser::isEnabled);

        if (userResult.isEmpty()) {
            auditService.recordSystemEvent(
                    AuditEventType.AUTH_LOGIN_FAILED,
                    "Operator login failed.",
                    AuditService.metadata("username", request.username().trim())
            );
            throw new AuthenticationFailedException();
        }

        OperatorUser user = userResult.get();
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            auditService.recordSystemEvent(
                    AuditEventType.AUTH_LOGIN_FAILED,
                    "Operator login failed.",
                    AuditService.metadata("username", user.getUsername())
            );
            throw new AuthenticationFailedException();
        }

        IssuedOperatorSession session = operatorSessionService.createSession(user);
        auditService.recordOperatorEvent(
                AuditEventType.AUTH_LOGIN_SUCCEEDED,
                user,
                "Operator login succeeded.",
                AuditService.metadata("username", user.getUsername(), "role", user.getRole().name())
        );

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

    public CurrentOperatorResponse currentOperator(AuthenticatedOperator operator) {
        return CurrentOperatorResponse.from(operator);
    }

    @Transactional
    public void logout(AuthenticatedOperator operator, String accessToken) {
        operatorSessionService.revoke(accessToken);
        auditService.recordOperatorEvent(
                AuditEventType.AUTH_LOGOUT,
                operator.operatorId(),
                "Operator logged out.",
                AuditService.metadata("username", operator.username())
        );
    }
}