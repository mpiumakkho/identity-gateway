package com.identitygateway.verification;

import com.identitygateway.auth.AuthenticatedOperator;
import com.identitygateway.auth.OperatorUser;
import com.identitygateway.auth.OperatorUserRepository;
import com.identitygateway.common.error.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class VerificationService {

    private final VerificationSessionRepository verificationSessionRepository;
    private final ManualIdentityEntryRepository manualIdentityEntryRepository;
    private final OperatorUserRepository operatorUserRepository;

    public VerificationService(
            VerificationSessionRepository verificationSessionRepository,
            ManualIdentityEntryRepository manualIdentityEntryRepository,
            OperatorUserRepository operatorUserRepository
    ) {
        this.verificationSessionRepository = verificationSessionRepository;
        this.manualIdentityEntryRepository = manualIdentityEntryRepository;
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
        return findSession(transactionId)
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

    @Transactional
    public ManualIdentityResponse saveManualIdentity(UUID transactionId, ManualIdentityRequest request) {
        VerificationSessionEntity session = findSession(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException("Verification session not found."));

        if (session.getMethod() != VerificationMethod.MANUAL_ENTRY) {
            throw new IllegalArgumentException("Manual identity can only be captured for MANUAL_ENTRY sessions.");
        }

        ManualIdentityEntry entry = manualIdentityEntryRepository.findBySessionId(session.getId())
                .map(existing -> existing.update(request))
                .orElseGet(() -> ManualIdentityEntry.create(session, request));

        session.markIdentityCaptured();
        ManualIdentityEntry savedEntry = manualIdentityEntryRepository.save(entry);

        return toManualIdentityResponse(session, savedEntry);
    }

    private Optional<VerificationSessionEntity> findSession(UUID transactionId) {
        return verificationSessionRepository.findDetailById(transactionId);
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

    private ManualIdentityResponse toManualIdentityResponse(VerificationSessionEntity session, ManualIdentityEntry entry) {
        return new ManualIdentityResponse(
                session.getId(),
                session.getStatus().name(),
                maskNationalId(entry.getNationalId()),
                entry.getTitle(),
                entry.getFirstName(),
                entry.getLastName(),
                entry.getDateOfBirth(),
                entry.getUpdatedAt()
        );
    }

    private static String maskNationalId(String nationalId) {
        if (nationalId == null || nationalId.length() != 13) {
            return "*************";
        }

        return nationalId.substring(0, 3) + "******" + nationalId.substring(9);
    }
}
