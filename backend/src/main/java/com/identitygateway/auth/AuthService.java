package com.identitygateway.auth;

import com.identitygateway.audit.AuditEventType;
import com.identitygateway.audit.AuditService;
import com.identitygateway.common.error.AuthenticationFailedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class AuthService {

    private final OperatorUserRepository operatorUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final OperatorSessionService operatorSessionService;
    private final AuditService auditService;
    private final AuthHardeningProperties hardeningProperties;
    private final PasswordPolicyService passwordPolicyService;
    private final Clock clock;

    public AuthService(
            OperatorUserRepository operatorUserRepository,
            PasswordEncoder passwordEncoder,
            OperatorSessionService operatorSessionService,
            AuditService auditService,
            AuthHardeningProperties hardeningProperties,
            PasswordPolicyService passwordPolicyService,
            Clock clock
    ) {
        this.operatorUserRepository = operatorUserRepository;
        this.passwordEncoder = passwordEncoder;
        this.operatorSessionService = operatorSessionService;
        this.auditService = auditService;
        this.hardeningProperties = hardeningProperties;
        this.passwordPolicyService = passwordPolicyService;
        this.clock = clock;
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
        Instant now = clock.instant();
        if (user.isLocked(now)) {
            auditService.recordOperatorEvent(
                    AuditEventType.AUTH_LOGIN_FAILED,
                    user,
                    "Operator login rejected because the account is locked.",
                    AuditService.metadata("username", user.getUsername())
            );
            throw new AuthenticationFailedException();
        }

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            recordFailedLogin(user);
            auditService.recordSystemEvent(
                    AuditEventType.AUTH_LOGIN_FAILED,
                    "Operator login failed.",
                    AuditService.metadata("username", user.getUsername())
            );
            throw new AuthenticationFailedException();
        }

        user.clearLoginLock();
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
                user.getRole().permissions(),
                clock.instant(),
                session.accessToken(),
                session.expiresAt()
        );
    }

    public CurrentOperatorResponse currentOperator(AuthenticatedOperator operator) {
        return CurrentOperatorResponse.from(operator);
    }

    @Transactional(readOnly = true)
    public List<OperatorSessionResponse> activeSessions(AuthenticatedOperator operator, String accessToken) {
        return operatorSessionService.activeSessions(requireEnabledOperator(operator), accessToken);
    }

    @Transactional
    public SessionRevocationResponse revokeSession(AuthenticatedOperator operator, UUID sessionId, String accessToken) {
        SessionRevocationResponse response = operatorSessionService.revokeSession(requireEnabledOperator(operator), sessionId, accessToken);
        auditService.recordOperatorEvent(
                AuditEventType.AUTH_SESSION_REVOKED,
                operator.operatorId(),
                "Operator session revoked.",
                AuditService.metadata("sessionId", sessionId.toString(), "username", operator.username())
        );
        return response;
    }

    @Transactional
    public PasswordChangeResponse changeOwnPassword(AuthenticatedOperator operator, String accessToken, ChangeOwnPasswordRequest request) {
        OperatorUser user = requireEnabledOperator(operator);

        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new AuthenticationFailedException();
        }

        passwordPolicyService.validate(request.newPassword());

        if (passwordEncoder.matches(request.newPassword(), user.getPasswordHash())) {
            throw new IllegalArgumentException("New password must be different from the current password.");
        }

        user.changePasswordHash(passwordEncoder.encode(request.newPassword()));
        operatorSessionService.revokeActiveSessionsExcept(user, accessToken);
        auditService.recordOperatorEvent(
                AuditEventType.AUTH_PASSWORD_CHANGED,
                user,
                "Operator changed own password.",
                AuditService.metadata("username", user.getUsername())
        );

        return new PasswordChangeResponse(true);
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

    private void recordFailedLogin(OperatorUser user) {
        AuthHardeningProperties.Lockout lockout = hardeningProperties.getLockout();
        if (!lockout.isEnabled()) {
            return;
        }

        int nextFailedAttempts = user.getFailedLoginAttempts() + 1;
        Instant lockedUntil = nextFailedAttempts >= lockout.getMaxFailedAttempts()
                ? clock.instant().plus(lockout.getDuration())
                : null;
        user.recordFailedLogin(lockedUntil);
    }

    private OperatorUser requireEnabledOperator(AuthenticatedOperator operator) {
        return operatorUserRepository.findById(operator.operatorId())
                .filter(OperatorUser::isEnabled)
                .orElseThrow(AuthenticationFailedException::new);
    }
}
