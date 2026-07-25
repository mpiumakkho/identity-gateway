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

    private static final String SESSION_NOT_FOUND_MESSAGE = "Verification session not found.";

    private final VerificationSessionRepository verificationSessionRepository;
    private final ManualIdentityEntryRepository manualIdentityEntryRepository;
    private final DipChipIdentityEntryRepository dipChipIdentityEntryRepository;
    private final OperatorUserRepository operatorUserRepository;

    public VerificationService(
            VerificationSessionRepository verificationSessionRepository,
            ManualIdentityEntryRepository manualIdentityEntryRepository,
            DipChipIdentityEntryRepository dipChipIdentityEntryRepository,
            OperatorUserRepository operatorUserRepository
    ) {
        this.verificationSessionRepository = verificationSessionRepository;
        this.manualIdentityEntryRepository = manualIdentityEntryRepository;
        this.dipChipIdentityEntryRepository = dipChipIdentityEntryRepository;
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
        return toResponse(requireSession(transactionId));
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
        VerificationSessionEntity session = requireSession(transactionId);
        requireMethod(session, VerificationMethod.MANUAL_ENTRY, "Manual identity can only be captured for MANUAL_ENTRY sessions.");

        ManualIdentityEntry entry = manualIdentityEntryRepository.findBySessionId(session.getId())
                .map(existing -> existing.update(request))
                .orElseGet(() -> ManualIdentityEntry.create(session, request));

        session.markIdentityCaptured();
        ManualIdentityEntry savedEntry = manualIdentityEntryRepository.save(entry);

        return toManualIdentityResponse(session, savedEntry);
    }

    @Transactional
    public DipChipPayloadResponse saveDipChipPayload(UUID transactionId, DipChipPayloadRequest request) {
        VerificationSessionEntity session = requireSession(transactionId);
        requireMethod(session, VerificationMethod.DIP_CHIP, "Dip Chip payload can only be captured for DIP_CHIP sessions.");
        requireValidCardDates(request);

        DipChipIdentityEntry entry = dipChipIdentityEntryRepository.findBySessionId(session.getId())
                .map(existing -> existing.update(request))
                .orElseGet(() -> DipChipIdentityEntry.create(session, request));

        session.markIdentityCaptured();
        DipChipIdentityEntry savedEntry = dipChipIdentityEntryRepository.save(entry);

        return toDipChipPayloadResponse(session, savedEntry);
    }

    private static void requireValidCardDates(DipChipPayloadRequest request) {
        if (request.cardExpiryDate().isBefore(request.cardIssueDate())) {
            throw new IllegalArgumentException("Card expiry date must be on or after the issue date.");
        }
    }

    private VerificationSessionEntity requireSession(UUID transactionId) {
        return verificationSessionRepository.findDetailById(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException(SESSION_NOT_FOUND_MESSAGE));
    }

    private static void requireMethod(VerificationSessionEntity session, VerificationMethod expectedMethod, String message) {
        if (session.getMethod() != expectedMethod) {
            throw new IllegalArgumentException(message);
        }
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

    private DipChipPayloadResponse toDipChipPayloadResponse(VerificationSessionEntity session, DipChipIdentityEntry entry) {
        return new DipChipPayloadResponse(
                session.getId(),
                session.getStatus().name(),
                maskNationalId(entry.getNationalId()),
                entry.getTitle(),
                entry.getFirstName(),
                entry.getLastName(),
                entry.getDateOfBirth(),
                entry.getCardIssueDate(),
                entry.getCardExpiryDate(),
                entry.getReaderName(),
                entry.getReaderSerialNumber(),
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