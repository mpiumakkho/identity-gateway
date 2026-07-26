package com.identitygateway.dopa;

import com.identitygateway.auth.AuthenticatedOperator;
import com.identitygateway.audit.AuditEventType;
import com.identitygateway.audit.AuditService;
import com.identitygateway.common.error.ResourceNotFoundException;
import com.identitygateway.verification.DipChipIdentityEntry;
import com.identitygateway.verification.DipChipIdentityEntryRepository;
import com.identitygateway.verification.ManualIdentityEntry;
import com.identitygateway.verification.ManualIdentityEntryRepository;
import com.identitygateway.verification.VerificationMethod;
import com.identitygateway.verification.VerificationSessionEntity;
import com.identitygateway.verification.VerificationSessionRepository;
import com.identitygateway.verification.VerificationStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static com.identitygateway.identity.IdentityDataProtector.maskNationalId;

@Service
public class DopaValidationService {

    private static final String SESSION_NOT_FOUND_MESSAGE = "Verification session not found.";
    private static final String DOPA_PARTNER_ERROR_CODE = "DOPA-PARTNER-ERROR";
    private static final String DOPA_PARTNER_ERROR_MESSAGE = "DOPA partner validation unavailable.";

    private final VerificationSessionRepository verificationSessionRepository;
    private final ManualIdentityEntryRepository manualIdentityEntryRepository;
    private final DipChipIdentityEntryRepository dipChipIdentityEntryRepository;
    private final DopaValidationAttemptRepository dopaValidationAttemptRepository;
    private final DopaGatewayClient dopaGatewayClient;
    private final AuditService auditService;

    public DopaValidationService(
            VerificationSessionRepository verificationSessionRepository,
            ManualIdentityEntryRepository manualIdentityEntryRepository,
            DipChipIdentityEntryRepository dipChipIdentityEntryRepository,
            DopaValidationAttemptRepository dopaValidationAttemptRepository,
            DopaGatewayClient dopaGatewayClient,
            AuditService auditService
    ) {
        this.verificationSessionRepository = verificationSessionRepository;
        this.manualIdentityEntryRepository = manualIdentityEntryRepository;
        this.dipChipIdentityEntryRepository = dipChipIdentityEntryRepository;
        this.dopaValidationAttemptRepository = dopaValidationAttemptRepository;
        this.dopaGatewayClient = dopaGatewayClient;
        this.auditService = auditService;
    }

    @Transactional
    public DopaValidationResponse validate(AuthenticatedOperator operator, UUID transactionId, DopaValidationRequest request) {
        VerificationSessionEntity session = verificationSessionRepository.findDetailById(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException(SESSION_NOT_FOUND_MESSAGE));

        if (session.isClosed()) {
            throw new IllegalArgumentException("Verification session is already closed.");
        }

        if (session.getStatus() == VerificationStatus.CREATED) {
            throw new IllegalArgumentException("Identity must be captured before DOPA validation.");
        }

        DopaIdentitySnapshot identity = resolveIdentity(session);
        DopaGatewayResult result = validateWithGateway(identity, request);
        DopaValidationAttempt savedAttempt = dopaValidationAttemptRepository.save(
                DopaValidationAttempt.create(session, identity.source(), result, request.consentReference())
        );

        applySessionStatus(session, result);

        auditService.recordTransactionEvent(
                AuditEventType.DOPA_VALIDATION_COMPLETED,
                operator.operatorId(),
                session,
                "DOPA validation completed.",
                AuditService.metadata("result", result.status().name(), "status", session.getStatus().name(), "responseCode", result.responseCode())
        );

        return toResponse(session, identity, savedAttempt);
    }

    private DopaGatewayResult validateWithGateway(DopaIdentitySnapshot identity, DopaValidationRequest request) {
        try {
            return dopaGatewayClient.validate(new DopaGatewayRequest(identity, request.consentReference().trim()));
        } catch (DopaGatewayException ex) {
            return DopaGatewayResult.error(DOPA_PARTNER_ERROR_CODE, DOPA_PARTNER_ERROR_MESSAGE);
        }
    }

    private void applySessionStatus(VerificationSessionEntity session, DopaGatewayResult result) {
        if (result.matched()) {
            session.markDopaVerified();
            return;
        }

        if (result.notMatched()) {
            session.markDopaRejected();
        }
    }

    private DopaIdentitySnapshot resolveIdentity(VerificationSessionEntity session) {
        if (session.getMethod() == VerificationMethod.MANUAL_ENTRY) {
            return manualIdentityEntryRepository.findBySessionId(session.getId())
                    .map(this::fromManualIdentity)
                    .orElseThrow(() -> new IllegalArgumentException("Identity must be captured before DOPA validation."));
        }

        return dipChipIdentityEntryRepository.findBySessionId(session.getId())
                .map(this::fromDipChipIdentity)
                .orElseThrow(() -> new IllegalArgumentException("Identity must be captured before DOPA validation."));
    }

    private DopaIdentitySnapshot fromManualIdentity(ManualIdentityEntry identity) {
        return new DopaIdentitySnapshot(
                DopaIdentitySource.MANUAL_ENTRY,
                identity.getNationalId(),
                identity.getTitle(),
                identity.getFirstName(),
                identity.getLastName(),
                identity.getDateOfBirth(),
                identity.getLaserCode()
        );
    }

    private DopaIdentitySnapshot fromDipChipIdentity(DipChipIdentityEntry identity) {
        return new DopaIdentitySnapshot(
                DopaIdentitySource.DIP_CHIP,
                identity.getNationalId(),
                identity.getTitle(),
                identity.getFirstName(),
                identity.getLastName(),
                identity.getDateOfBirth(),
                identity.getLaserCode()
        );
    }

    private DopaValidationResponse toResponse(
            VerificationSessionEntity session,
            DopaIdentitySnapshot identity,
            DopaValidationAttempt attempt
    ) {
        return new DopaValidationResponse(
                session.getId(),
                session.getStatus().name(),
                attempt.getResultStatus().name(),
                attempt.getIdentitySource().name(),
                maskNationalId(identity.nationalId()),
                attempt.getResponseCode(),
                attempt.getResponseMessage(),
                attempt.getConsentReference(),
                attempt.getValidatedAt()
        );
    }

}
