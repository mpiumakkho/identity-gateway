package com.identitygateway.dopa;

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

@Service
public class DopaValidationService {

    private static final String SESSION_NOT_FOUND_MESSAGE = "Verification session not found.";

    private final VerificationSessionRepository verificationSessionRepository;
    private final ManualIdentityEntryRepository manualIdentityEntryRepository;
    private final DipChipIdentityEntryRepository dipChipIdentityEntryRepository;
    private final DopaValidationAttemptRepository dopaValidationAttemptRepository;
    private final DopaGatewayClient dopaGatewayClient;

    public DopaValidationService(
            VerificationSessionRepository verificationSessionRepository,
            ManualIdentityEntryRepository manualIdentityEntryRepository,
            DipChipIdentityEntryRepository dipChipIdentityEntryRepository,
            DopaValidationAttemptRepository dopaValidationAttemptRepository,
            DopaGatewayClient dopaGatewayClient
    ) {
        this.verificationSessionRepository = verificationSessionRepository;
        this.manualIdentityEntryRepository = manualIdentityEntryRepository;
        this.dipChipIdentityEntryRepository = dipChipIdentityEntryRepository;
        this.dopaValidationAttemptRepository = dopaValidationAttemptRepository;
        this.dopaGatewayClient = dopaGatewayClient;
    }

    @Transactional
    public DopaValidationResponse validate(UUID transactionId, DopaValidationRequest request) {
        VerificationSessionEntity session = verificationSessionRepository.findDetailById(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException(SESSION_NOT_FOUND_MESSAGE));

        if (session.isClosed()) {
            throw new IllegalArgumentException("Verification session is already closed.");
        }

        if (session.getStatus() == VerificationStatus.CREATED) {
            throw new IllegalArgumentException("Identity must be captured before DOPA validation.");
        }

        DopaIdentitySnapshot identity = resolveIdentity(session);
        DopaGatewayResult result = dopaGatewayClient.validate(new DopaGatewayRequest(identity, request.consentReference().trim()));
        DopaValidationAttempt savedAttempt = dopaValidationAttemptRepository.save(
                DopaValidationAttempt.create(session, identity.source(), result, request.consentReference())
        );

        if (result.matched()) {
            session.markDopaVerified();
        } else {
            session.markDopaRejected();
        }

        return toResponse(session, identity, savedAttempt);
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

    private static String maskNationalId(String nationalId) {
        if (nationalId == null || nationalId.length() != 13) {
            return "*************";
        }

        return nationalId.substring(0, 3) + "******" + nationalId.substring(9);
    }
}