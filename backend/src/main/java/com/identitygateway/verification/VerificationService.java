package com.identitygateway.verification;

import com.identitygateway.auth.AuthenticatedOperator;
import com.identitygateway.auth.OperatorUser;
import com.identitygateway.auth.OperatorUserRepository;
import com.identitygateway.common.error.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
public class VerificationService {

    private final VerificationSessionRepository verificationSessionRepository;
    private final OperatorUserRepository operatorUserRepository;

    public VerificationService(
            VerificationSessionRepository verificationSessionRepository,
            OperatorUserRepository operatorUserRepository
    ) {
        this.verificationSessionRepository = verificationSessionRepository;
        this.operatorUserRepository = operatorUserRepository;
    }

    @Transactional(readOnly = true)
    public List<VerificationMethodResponse> methods() {
        return Arrays.stream(VerificationMethod.values())
                .map(method -> new VerificationMethodResponse(method.name(), method.label(), method.description(), true))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<VerificationSessionResponse> recentSessions() {
        return verificationSessionRepository.findTop20ByOrderByCreatedAtDesc().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public VerificationSessionResponse session(UUID transactionId) {
        return verificationSessionRepository.findDetailById(transactionId)
                .map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Verification session not found."));
    }

    @Transactional
    public VerificationSessionResponse startSession(AuthenticatedOperator operator, StartVerificationRequest request) {
        VerificationMethod method = VerificationMethod.from(request.method());
        OperatorUser createdBy = operatorUserRepository.getReferenceById(operator.operatorId());
        VerificationSessionEntity session = verificationSessionRepository.save(VerificationSessionEntity.create(method, createdBy));
        return toResponse(session);
    }

    private VerificationSessionResponse toResponse(VerificationSessionEntity session) {
        return new VerificationSessionResponse(
                session.getId(),
                session.getMethod().name(),
                session.getStatus().name(),
                SessionOperatorResponse.from(session.getCreatedBy()),
                session.getCreatedAt()
        );
    }
}