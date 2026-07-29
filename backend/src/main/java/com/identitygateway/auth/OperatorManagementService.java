package com.identitygateway.auth;

import com.identitygateway.audit.AuditEventType;
import com.identitygateway.audit.AuditService;
import com.identitygateway.common.error.ResourceNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;
import java.util.UUID;

@Service
public class OperatorManagementService {

    private static final String OPERATOR_NOT_FOUND_MESSAGE = "Operator not found.";

    private final OperatorUserRepository operatorUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final OperatorSessionService operatorSessionService;
    private final AuditService auditService;
    private final Clock clock;

    public OperatorManagementService(
            OperatorUserRepository operatorUserRepository,
            PasswordEncoder passwordEncoder,
            OperatorSessionService operatorSessionService,
            AuditService auditService,
            Clock clock
    ) {
        this.operatorUserRepository = operatorUserRepository;
        this.passwordEncoder = passwordEncoder;
        this.operatorSessionService = operatorSessionService;
        this.auditService = auditService;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<OperatorUserResponse> operators() {
        return operatorUserRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(OperatorUserResponse::from)
                .toList();
    }

    @Transactional
    public OperatorUserResponse createOperator(AuthenticatedOperator admin, CreateOperatorRequest request) {
        String username = request.username().trim();
        if (operatorUserRepository.existsByUsernameIgnoreCase(username)) {
            throw new IllegalArgumentException("Operator username already exists.");
        }

        OperatorRole role = parseRole(request.role());
        OperatorUser operator = operatorUserRepository.save(OperatorUser.create(
                username,
                passwordEncoder.encode(request.password()),
                request.displayName().trim(),
                role
        ));
        auditService.recordOperatorEvent(
                AuditEventType.OPERATOR_CREATED,
                admin.operatorId(),
                "Operator account created.",
                AuditService.metadata("operatorId", operator.getId().toString(), "username", operator.getUsername(), "role", operator.getRole().name())
        );

        return OperatorUserResponse.from(operator);
    }

    @Transactional
    public OperatorUserResponse changePassword(AuthenticatedOperator admin, UUID operatorId, ChangeOperatorPasswordRequest request) {
        OperatorUser operator = requireOperator(operatorId);
        operator.changePasswordHash(passwordEncoder.encode(request.password()));
        operatorSessionService.revokeActiveSessions(operator);
        auditService.recordOperatorEvent(
                AuditEventType.OPERATOR_PASSWORD_CHANGED,
                admin.operatorId(),
                "Operator password changed.",
                AuditService.metadata("operatorId", operator.getId().toString(), "username", operator.getUsername())
        );

        return OperatorUserResponse.from(operator);
    }

    @Transactional
    public OperatorUserResponse disableOperator(AuthenticatedOperator admin, UUID operatorId) {
        if (admin.operatorId().equals(operatorId)) {
            throw new IllegalArgumentException("Admin operators cannot disable their own account.");
        }

        OperatorUser operator = requireOperator(operatorId);
        if (operator.isEnabled()) {
            operator.disable(clock.instant());
            operatorSessionService.revokeActiveSessions(operator);
            auditService.recordOperatorEvent(
                    AuditEventType.OPERATOR_DISABLED,
                    admin.operatorId(),
                    "Operator account disabled.",
                    AuditService.metadata("operatorId", operator.getId().toString(), "username", operator.getUsername())
            );
        }

        return OperatorUserResponse.from(operator);
    }

    @Transactional(readOnly = true)
    public List<OperatorSessionResponse> operatorSessions(UUID operatorId, String activeAccessToken) {
        return operatorSessionService.activeSessionsForAdmin(requireOperator(operatorId), activeAccessToken);
    }

    @Transactional
    public SessionRevocationSummaryResponse revokeOperatorSession(
            AuthenticatedOperator admin,
            UUID operatorId,
            UUID sessionId,
            String activeAccessToken
    ) {
        OperatorUser operator = requireOperator(operatorId);
        SessionRevocationSummaryResponse response = operatorSessionService.revokeSessionForAdmin(operator, sessionId, activeAccessToken);
        recordSessionRevocation(admin, operator, response.revokedSessions(), sessionId);
        return response;
    }

    @Transactional
    public SessionRevocationSummaryResponse revokeOperatorSessions(
            AuthenticatedOperator admin,
            UUID operatorId,
            String activeAccessToken
    ) {
        OperatorUser operator = requireOperator(operatorId);
        boolean keepCurrentSession = admin.operatorId().equals(operator.getId());
        SessionRevocationSummaryResponse response = operatorSessionService.revokeActiveSessionsForAdmin(operator, activeAccessToken, keepCurrentSession);
        recordSessionRevocation(admin, operator, response.revokedSessions(), null);
        return response;
    }

    private void recordSessionRevocation(AuthenticatedOperator admin, OperatorUser operator, int revokedSessions, UUID sessionId) {
        if (revokedSessions == 0) {
            return;
        }

        auditService.recordOperatorEvent(
                AuditEventType.OPERATOR_SESSIONS_REVOKED,
                admin.operatorId(),
                "Operator sessions revoked.",
                sessionId == null
                        ? AuditService.metadata("operatorId", operator.getId().toString(), "username", operator.getUsername(), "revokedSessions", String.valueOf(revokedSessions))
                        : AuditService.metadata("operatorId", operator.getId().toString(), "sessionId", sessionId.toString(), "revokedSessions", String.valueOf(revokedSessions))
        );
    }

    private OperatorUser requireOperator(UUID operatorId) {
        return operatorUserRepository.findById(operatorId)
                .orElseThrow(() -> new ResourceNotFoundException(OPERATOR_NOT_FOUND_MESSAGE));
    }

    private static OperatorRole parseRole(String role) {
        try {
            return OperatorRole.valueOf(role.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Unsupported operator role: " + role, ex);
        }
    }
}