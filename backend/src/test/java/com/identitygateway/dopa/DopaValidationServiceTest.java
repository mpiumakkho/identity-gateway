package com.identitygateway.dopa;

import com.identitygateway.auth.AuthenticatedOperator;
import com.identitygateway.auth.OperatorRole;
import com.identitygateway.auth.OperatorUser;
import com.identitygateway.common.error.ResourceNotFoundException;
import com.identitygateway.audit.AuditService;
import com.identitygateway.dipchip.DipChipPayloadNormalizer;
import com.identitygateway.identity.ManualIdentityNormalizer;
import com.identitygateway.verification.DipChipIdentityEntry;
import com.identitygateway.verification.DipChipIdentityEntryRepository;
import com.identitygateway.verification.ManualIdentityEntry;
import com.identitygateway.verification.ManualIdentityEntryRepository;
import com.identitygateway.verification.VerificationMethod;
import com.identitygateway.verification.VerificationSessionEntity;
import com.identitygateway.verification.VerificationSessionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DopaValidationServiceTest {

    @Mock
    private VerificationSessionRepository verificationSessionRepository;

    @Mock
    private ManualIdentityEntryRepository manualIdentityEntryRepository;

    @Mock
    private DipChipIdentityEntryRepository dipChipIdentityEntryRepository;

    @Mock
    private DopaValidationAttemptRepository dopaValidationAttemptRepository;

    @Mock
    private DopaGatewayClient dopaGatewayClient;

    @Mock
    private AuditService auditService;

    private DopaValidationService dopaValidationService;

    @BeforeEach
    void setUp() {
        dopaValidationService = new DopaValidationService(
                verificationSessionRepository,
                manualIdentityEntryRepository,
                dipChipIdentityEntryRepository,
                dopaValidationAttemptRepository,
                dopaGatewayClient,
                auditService
        );
    }

    @Test
    void validateMatchesManualIdentityAndMarksSessionVerified() {
        VerificationSessionEntity session = sessionEntity(VerificationMethod.MANUAL_ENTRY);
        session.markIdentityCaptured();
        when(verificationSessionRepository.findDetailById(session.getId())).thenReturn(Optional.of(session));
        when(manualIdentityEntryRepository.findBySessionId(session.getId())).thenReturn(Optional.of(manualIdentity(session)));
        when(dopaGatewayClient.validate(any(DopaGatewayRequest.class)))
                .thenReturn(new DopaGatewayResult(DopaValidationResultStatus.MATCHED, "DOPA-0000", "Citizen identity matched."));
        when(dopaValidationAttemptRepository.save(any(DopaValidationAttempt.class))).thenAnswer(invocation -> {
            DopaValidationAttempt attempt = invocation.getArgument(0);
            attempt.prePersist();
            return attempt;
        });

        DopaValidationResponse response = dopaValidationService.validate(authenticatedOperator(), session.getId(), new DopaValidationRequest("CONSENT-001"));

        assertThat(response.transactionId()).isEqualTo(session.getId());
        assertThat(response.sessionStatus()).isEqualTo("DOPA_VERIFIED");
        assertThat(response.validationStatus()).isEqualTo("MATCHED");
        assertThat(response.identitySource()).isEqualTo("MANUAL_ENTRY");
        assertThat(response.maskedNationalId()).isEqualTo("123******0121");
        assertThat(response.consentReference()).isEqualTo("CONSENT-001");
        assertThat(response.validatedAt()).isNotNull();
    }

    @Test
    void validateRejectsDipChipIdentityAndMarksSessionRejected() {
        VerificationSessionEntity session = sessionEntity(VerificationMethod.DIP_CHIP);
        session.markIdentityCaptured();
        when(verificationSessionRepository.findDetailById(session.getId())).thenReturn(Optional.of(session));
        when(dipChipIdentityEntryRepository.findBySessionId(session.getId())).thenReturn(Optional.of(dipChipIdentity(session)));
        when(dopaGatewayClient.validate(any(DopaGatewayRequest.class)))
                .thenReturn(new DopaGatewayResult(DopaValidationResultStatus.NOT_MATCHED, "DOPA-4001", "Citizen identity did not match."));
        when(dopaValidationAttemptRepository.save(any(DopaValidationAttempt.class))).thenAnswer(invocation -> {
            DopaValidationAttempt attempt = invocation.getArgument(0);
            attempt.prePersist();
            return attempt;
        });

        DopaValidationResponse response = dopaValidationService.validate(authenticatedOperator(), session.getId(), new DopaValidationRequest("CONSENT-002"));

        assertThat(response.sessionStatus()).isEqualTo("DOPA_REJECTED");
        assertThat(response.validationStatus()).isEqualTo("NOT_MATCHED");
        assertThat(response.identitySource()).isEqualTo("DIP_CHIP");
        assertThat(response.responseCode()).isEqualTo("DOPA-4001");
    }

    @Test
    void validateRecordsGatewayErrorWithoutRejectingSession() {
        VerificationSessionEntity session = sessionEntity(VerificationMethod.MANUAL_ENTRY);
        session.markIdentityCaptured();
        when(verificationSessionRepository.findDetailById(session.getId())).thenReturn(Optional.of(session));
        when(manualIdentityEntryRepository.findBySessionId(session.getId())).thenReturn(Optional.of(manualIdentity(session)));
        when(dopaGatewayClient.validate(any(DopaGatewayRequest.class)))
                .thenThrow(new DopaGatewayException("Partner timeout."));
        when(dopaValidationAttemptRepository.save(any(DopaValidationAttempt.class))).thenAnswer(invocation -> {
            DopaValidationAttempt attempt = invocation.getArgument(0);
            attempt.prePersist();
            return attempt;
        });

        DopaValidationResponse response = dopaValidationService.validate(authenticatedOperator(), session.getId(), new DopaValidationRequest("CONSENT-003"));

        assertThat(response.sessionStatus()).isEqualTo("IDENTITY_CAPTURED");
        assertThat(response.validationStatus()).isEqualTo("ERROR");
        assertThat(response.responseCode()).isEqualTo("DOPA-PARTNER-ERROR");
        assertThat(response.responseMessage()).isEqualTo("DOPA partner validation unavailable.");
    }

    @Test
    void validateRejectsSessionWithoutCapturedIdentity() {
        VerificationSessionEntity session = sessionEntity(VerificationMethod.MANUAL_ENTRY);
        when(verificationSessionRepository.findDetailById(session.getId())).thenReturn(Optional.of(session));

        assertThatThrownBy(() -> dopaValidationService.validate(authenticatedOperator(), session.getId(), new DopaValidationRequest("CONSENT-003")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Identity must be captured before DOPA validation.");
        verify(dopaGatewayClient, never()).validate(any(DopaGatewayRequest.class));
    }

    @Test
    void validateRejectsUnknownSession() {
        UUID transactionId = UUID.fromString("15e8023f-7d03-4287-ac9c-73d80ac9af67");
        when(verificationSessionRepository.findDetailById(transactionId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> dopaValidationService.validate(authenticatedOperator(), transactionId, new DopaValidationRequest("CONSENT-004")))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Verification session not found.");
    }

    private static AuthenticatedOperator authenticatedOperator() {
        return new AuthenticatedOperator(
                UUID.fromString("9e04e2eb-d74a-4d55-987c-f38660aa3060"),
                "operator",
                "Operations User",
                OperatorRole.OPERATIONS,
                Instant.parse("2026-07-25T08:00:00Z")
        );
    }

    private static VerificationSessionEntity sessionEntity(VerificationMethod method) {
        OperatorUser operator = OperatorUser.create("operator", "hash", "Operations User", OperatorRole.OPERATIONS);
        VerificationSessionEntity entity = VerificationSessionEntity.create(method, operator);
        ReflectionTestUtils.setField(entity, "id", UUID.randomUUID());
        ReflectionTestUtils.setField(entity, "createdAt", Instant.parse("2026-07-25T00:00:00Z"));
        return entity;
    }

    private static ManualIdentityEntry manualIdentity(VerificationSessionEntity session) {
        return ManualIdentityEntry.create(session, new ManualIdentityNormalizer().normalize(
                "1234567890121",
                "Mr.",
                "Somchai",
                "Jaidee",
                LocalDate.parse("1990-01-31"),
                "JT1234567890"
        ));
    }

    private static DipChipIdentityEntry dipChipIdentity(VerificationSessionEntity session) {
        return DipChipIdentityEntry.create(session, new DipChipPayloadNormalizer().normalize(
                "1234567890121",
                "Mr.",
                "Somchai",
                "Jaidee",
                LocalDate.parse("1990-01-31"),
                "JT1234567890",
                LocalDate.parse("2021-02-01"),
                LocalDate.parse("2031-01-31"),
                "ACR39U",
                "RD-001",
                "CID=1234567890121;READER=ACR39U"
        ));
    }
}
