package com.identitygateway.verification;

import com.identitygateway.auth.AuthenticatedOperator;
import com.identitygateway.auth.OperatorUser;
import com.identitygateway.auth.OperatorUserRepository;
import com.identitygateway.audit.AuditEventResponse;
import com.identitygateway.audit.AuditEventType;
import com.identitygateway.audit.AuditService;
import com.identitygateway.common.error.ResourceNotFoundException;
import com.identitygateway.dopa.DopaValidationAttempt;
import com.identitygateway.dopa.DopaValidationAttemptRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
public class VerificationService {

    private static final String SESSION_NOT_FOUND_MESSAGE = "Verification session not found.";
    private static final String SESSION_CLOSED_MESSAGE = "Verification session is already closed.";

    private final VerificationSessionRepository verificationSessionRepository;
    private final ManualIdentityEntryRepository manualIdentityEntryRepository;
    private final DipChipIdentityEntryRepository dipChipIdentityEntryRepository;
    private final VerificationDecisionRepository verificationDecisionRepository;
    private final DopaValidationAttemptRepository dopaValidationAttemptRepository;
    private final OperatorUserRepository operatorUserRepository;
    private final AuditService auditService;

    public VerificationService(
            VerificationSessionRepository verificationSessionRepository,
            ManualIdentityEntryRepository manualIdentityEntryRepository,
            DipChipIdentityEntryRepository dipChipIdentityEntryRepository,
            VerificationDecisionRepository verificationDecisionRepository,
            DopaValidationAttemptRepository dopaValidationAttemptRepository,
            OperatorUserRepository operatorUserRepository,
            AuditService auditService
    ) {
        this.verificationSessionRepository = verificationSessionRepository;
        this.manualIdentityEntryRepository = manualIdentityEntryRepository;
        this.dipChipIdentityEntryRepository = dipChipIdentityEntryRepository;
        this.verificationDecisionRepository = verificationDecisionRepository;
        this.dopaValidationAttemptRepository = dopaValidationAttemptRepository;
        this.operatorUserRepository = operatorUserRepository;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<VerificationMethodResponse> methods() {
        return Arrays.stream(VerificationMethod.values())
                .map(method -> new VerificationMethodResponse(method.name(), method.label(), method.description(), true))
                .toList();
    }

    @Transactional(readOnly = true)
    public VerificationDashboardResponse dashboard() {
        return new VerificationDashboardResponse(
                verificationSessionRepository.count(),
                verificationSessionRepository.countByStatus().stream()
                        .map(metric -> new VerificationMetricCount(metric.getStatus().name(), metric.getTotal()))
                        .toList(),
                verificationSessionRepository.countByMethod().stream()
                        .map(metric -> new VerificationMetricCount(metric.getMethod().name(), metric.getTotal()))
                        .toList()
        );
    }

    @Transactional(readOnly = true)
    public List<VerificationSessionResponse> recentSessions() {
        return recentSessions(null, null);
    }

    @Transactional(readOnly = true)
    public List<VerificationSessionResponse> recentSessions(String method, String status) {
        VerificationMethod methodFilter = parseMethodFilter(method);
        VerificationStatus statusFilter = parseStatusFilter(status);

        return verificationSessionRepository.findRecent(methodFilter, statusFilter, PageRequest.of(0, 20)).stream()
                .map(this::toResponse)
                .toList();
    }
    @Transactional(readOnly = true)
    public VerificationSessionDetailResponse session(UUID transactionId) {
        return toDetailResponse(requireSession(transactionId));
    }

    @Transactional(readOnly = true)
    public List<AuditEventResponse> auditEvents(UUID transactionId) {
        requireSession(transactionId);
        return auditService.transactionEvents(transactionId);
    }

    @Transactional
    public VerificationSessionResponse startSession(AuthenticatedOperator operator, StartVerificationRequest request) {
        VerificationMethod method = VerificationMethod.from(request.method());
        OperatorUser createdBy = operatorUserRepository.getReferenceById(operator.operatorId());
        VerificationSessionEntity session = verificationSessionRepository.save(VerificationSessionEntity.create(method, createdBy));
        auditService.recordTransactionEvent(
                AuditEventType.VERIFICATION_SESSION_CREATED,
                operator.operatorId(),
                session,
                "Verification session created.",
                AuditService.metadata("method", method.name(), "status", session.getStatus().name())
        );
        return toResponse(session);
    }

    @Transactional
    public ManualIdentityResponse saveManualIdentity(AuthenticatedOperator operator, UUID transactionId, ManualIdentityRequest request) {
        VerificationSessionEntity session = requireSession(transactionId);
        requireOpen(session);
        requireMethod(session, VerificationMethod.MANUAL_ENTRY, "Manual identity can only be captured for MANUAL_ENTRY sessions.");

        ManualIdentityEntry entry = manualIdentityEntryRepository.findBySessionId(session.getId())
                .map(existing -> existing.update(request))
                .orElseGet(() -> ManualIdentityEntry.create(session, request));

        session.markIdentityCaptured();
        ManualIdentityEntry savedEntry = manualIdentityEntryRepository.save(entry);
        auditService.recordTransactionEvent(
                AuditEventType.MANUAL_IDENTITY_CAPTURED,
                operator.operatorId(),
                session,
                "Manual identity captured.",
                AuditService.metadata("method", session.getMethod().name(), "status", session.getStatus().name())
        );

        return toManualIdentityResponse(session, savedEntry);
    }

    @Transactional
    public DipChipPayloadResponse saveDipChipPayload(AuthenticatedOperator operator, UUID transactionId, DipChipPayloadRequest request) {
        VerificationSessionEntity session = requireSession(transactionId);
        requireOpen(session);
        requireMethod(session, VerificationMethod.DIP_CHIP, "Dip Chip payload can only be captured for DIP_CHIP sessions.");
        requireValidCardDates(request);

        DipChipIdentityEntry entry = dipChipIdentityEntryRepository.findBySessionId(session.getId())
                .map(existing -> existing.update(request))
                .orElseGet(() -> DipChipIdentityEntry.create(session, request));

        session.markIdentityCaptured();
        DipChipIdentityEntry savedEntry = dipChipIdentityEntryRepository.save(entry);
        auditService.recordTransactionEvent(
                AuditEventType.DIP_CHIP_PAYLOAD_CAPTURED,
                operator.operatorId(),
                session,
                "Dip Chip payload captured.",
                AuditService.metadata("method", session.getMethod().name(), "status", session.getStatus().name(), "readerName", savedEntry.getReaderName())
        );

        return toDipChipPayloadResponse(session, savedEntry);
    }

    @Transactional
    public VerificationCloseoutResponse closeSession(
            AuthenticatedOperator operator,
            UUID transactionId,
            CloseVerificationRequest request
    ) {
        VerificationSessionEntity session = requireSession(transactionId);
        requireOpen(session);
        requireDopaDecisionReady(session);

        VerificationDecision decision = VerificationDecision.from(request.decision());
        requireDecisionAllowed(session, decision);

        if (verificationDecisionRepository.existsBySessionId(session.getId())) {
            throw new IllegalArgumentException(SESSION_CLOSED_MESSAGE);
        }

        OperatorUser decidedBy = operatorUserRepository.getReferenceById(operator.operatorId());
        VerificationDecisionEntity closeout = verificationDecisionRepository.save(
                VerificationDecisionEntity.create(session, decision, request.notes(), decidedBy)
        );
        session.close(decision);
        auditService.recordTransactionEvent(
                AuditEventType.VERIFICATION_CLOSED,
                operator.operatorId(),
                session,
                "Verification transaction closed.",
                AuditService.metadata("decision", decision.name(), "status", session.getStatus().name())
        );

        return toCloseoutResponse(session, closeout);
    }

    private static void requireValidCardDates(DipChipPayloadRequest request) {
        if (request.cardExpiryDate().isBefore(request.cardIssueDate())) {
            throw new IllegalArgumentException("Card expiry date must be on or after the issue date.");
        }
    }

    private static VerificationMethod parseMethodFilter(String method) {
        if (method == null || method.isBlank()) {
            return null;
        }

        return VerificationMethod.from(method);
    }

    private static VerificationStatus parseStatusFilter(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }

        try {
            return VerificationStatus.valueOf(status.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Unsupported verification status: " + status, ex);
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

    private static void requireOpen(VerificationSessionEntity session) {
        if (session.isClosed()) {
            throw new IllegalArgumentException(SESSION_CLOSED_MESSAGE);
        }
    }

    private static void requireDopaDecisionReady(VerificationSessionEntity session) {
        if (session.getStatus() != VerificationStatus.DOPA_VERIFIED && session.getStatus() != VerificationStatus.DOPA_REJECTED) {
            throw new IllegalArgumentException("DOPA validation must be completed before closeout.");
        }
    }

    private static void requireDecisionAllowed(VerificationSessionEntity session, VerificationDecision decision) {
        if (session.getStatus() == VerificationStatus.DOPA_REJECTED && decision == VerificationDecision.APPROVED) {
            throw new IllegalArgumentException("DOPA rejected sessions cannot be approved.");
        }
    }

    private VerificationSessionDetailResponse toDetailResponse(VerificationSessionEntity session) {
        return new VerificationSessionDetailResponse(
                session.getId(),
                session.getMethod().name(),
                session.getStatus().name(),
                SessionOperatorResponse.from(session.getCreatedBy()),
                session.getCreatedAt(),
                identitySummary(session),
                dopaSummary(session),
                closeoutSummary(session)
        );
    }

    private VerificationIdentitySummaryResponse identitySummary(VerificationSessionEntity session) {
        if (session.getMethod() == VerificationMethod.DIP_CHIP) {
            return dipChipIdentityEntryRepository.findBySessionId(session.getId())
                    .map(this::toDipChipIdentitySummary)
                    .orElse(null);
        }

        return manualIdentityEntryRepository.findBySessionId(session.getId())
                .map(this::toManualIdentitySummary)
                .orElse(null);
    }

    private VerificationIdentitySummaryResponse toManualIdentitySummary(ManualIdentityEntry entry) {
        return new VerificationIdentitySummaryResponse(
                VerificationMethod.MANUAL_ENTRY.name(),
                maskNationalId(entry.getNationalId()),
                entry.getTitle(),
                entry.getFirstName(),
                entry.getLastName(),
                entry.getDateOfBirth(),
                null,
                null,
                null,
                null,
                entry.getUpdatedAt()
        );
    }

    private VerificationIdentitySummaryResponse toDipChipIdentitySummary(DipChipIdentityEntry entry) {
        return new VerificationIdentitySummaryResponse(
                VerificationMethod.DIP_CHIP.name(),
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

    private VerificationDopaSummaryResponse dopaSummary(VerificationSessionEntity session) {
        return dopaValidationAttemptRepository.findTop10BySessionIdOrderByValidatedAtDesc(session.getId()).stream()
                .findFirst()
                .map(this::toDopaSummary)
                .orElse(null);
    }

    private VerificationDopaSummaryResponse toDopaSummary(DopaValidationAttempt attempt) {
        return new VerificationDopaSummaryResponse(
                attempt.getResultStatus().name(),
                attempt.getIdentitySource().name(),
                attempt.getResponseCode(),
                attempt.getResponseMessage(),
                attempt.getConsentReference(),
                attempt.getValidatedAt()
        );
    }

    private VerificationDecisionSummaryResponse closeoutSummary(VerificationSessionEntity session) {
        return verificationDecisionRepository.findBySessionId(session.getId())
                .map(closeout -> new VerificationDecisionSummaryResponse(
                        closeout.getDecision().name(),
                        closeout.getNotes(),
                        SessionOperatorResponse.from(closeout.getDecidedBy()),
                        closeout.getDecidedAt()
                ))
                .orElse(null);
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

    private VerificationCloseoutResponse toCloseoutResponse(VerificationSessionEntity session, VerificationDecisionEntity closeout) {
        return new VerificationCloseoutResponse(
                session.getId(),
                session.getStatus().name(),
                closeout.getDecision().name(),
                closeout.getNotes(),
                SessionOperatorResponse.from(closeout.getDecidedBy()),
                closeout.getDecidedAt()
        );
    }

    private static String maskNationalId(String nationalId) {
        if (nationalId == null || nationalId.length() != 13) {
            return "*************";
        }

        return nationalId.substring(0, 3) + "******" + nationalId.substring(9);
    }
}
