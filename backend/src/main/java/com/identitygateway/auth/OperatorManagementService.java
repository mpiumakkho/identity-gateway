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