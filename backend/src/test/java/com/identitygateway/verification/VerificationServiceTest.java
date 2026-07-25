package com.identitygateway.verification;

import com.identitygateway.auth.AuthenticatedOperator;
import com.identitygateway.auth.OperatorRole;
import com.identitygateway.auth.OperatorUser;
import com.identitygateway.auth.OperatorUserRepository;
import com.identitygateway.common.error.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VerificationServiceTest {

    @Mock
    private VerificationSessionRepository verificationSessionRepository;

    @Mock
    private ManualIdentityEntryRepository manualIdentityEntryRepository;

    @Mock
    private DipChipIdentityEntryRepository dipChipIdentityEntryRepository;

    @Mock
    private OperatorUserRepository operatorUserRepository;

    private VerificationService verificationService;

    @BeforeEach
    void setUp() {
        verificationService = new VerificationService(
                verificationSessionRepository,
                manualIdentityEntryRepository,
                dipChipIdentityEntryRepository,
                operatorUserRepository
        );
    }

    @Test
    void methodsReturnsConfiguredFlowEntryPoints() {
        List<VerificationMethodResponse> methods = verificationService.methods();

        assertThat(methods)
                .extracting(VerificationMethodResponse::id)
                .containsExactly("DIP_CHIP", "MANUAL_ENTRY");
    }

    @Test
    void recentSessionsReturnsLatestTransactions() {
        VerificationSessionEntity entity = sessionEntity(VerificationMethod.DIP_CHIP);
        when(verificationSessionRepository.findTop20ByOrderByCreatedAtDesc()).thenReturn(List.of(entity));

        List<VerificationSessionResponse> responses = verificationService.recentSessions();

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).transactionId()).isEqualTo(entity.getId());
        assertThat(responses.get(0).createdBy().username()).isEqualTo("operator");
    }

    @Test
    void sessionReturnsTransactionDetail() {
        VerificationSessionEntity entity = sessionEntity(VerificationMethod.DIP_CHIP);
        when(verificationSessionRepository.findDetailById(entity.getId())).thenReturn(Optional.of(entity));

        VerificationSessionResponse response = verificationService.session(entity.getId());

        assertThat(response.transactionId()).isEqualTo(entity.getId());
        assertThat(response.method()).isEqualTo("DIP_CHIP");
        assertThat(response.status()).isEqualTo("CREATED");
    }

    @Test
    void sessionRejectsUnknownTransactionId() {
        UUID transactionId = UUID.fromString("15e8023f-7d03-4287-ac9c-73d80ac9af67");
        when(verificationSessionRepository.findDetailById(transactionId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> verificationService.session(transactionId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Verification session not found.");
    }

    @Test
    void startSessionPersistsCreatedSessionForOperator() {
        AuthenticatedOperator authenticatedOperator = authenticatedOperator();
        OperatorUser operator = OperatorUser.create("operator", "hash", "Operations User", OperatorRole.OPERATIONS);
        when(operatorUserRepository.getReferenceById(authenticatedOperator.operatorId())).thenReturn(operator);
        when(verificationSessionRepository.save(any(VerificationSessionEntity.class)))
                .thenAnswer(invocation -> {
                    VerificationSessionEntity entity = invocation.getArgument(0);
                    entity.prePersist();
                    return entity;
                });

        VerificationSessionResponse response = verificationService.startSession(authenticatedOperator, new StartVerificationRequest("DIP_CHIP"));

        assertThat(response.transactionId()).isNotNull();
        assertThat(response.method()).isEqualTo("DIP_CHIP");
        assertThat(response.status()).isEqualTo("CREATED");
        assertThat(response.createdBy().username()).isEqualTo("operator");
        assertThat(response.createdAt()).isNotNull();
    }

    @Test
    void startSessionRejectsUnsupportedMethod() {
        assertThatThrownBy(() -> verificationService.startSession(authenticatedOperator(), new StartVerificationRequest("VIDEO_CALL")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Unsupported verification method: VIDEO_CALL");
    }

    @Test
    void saveManualIdentityCapturesManualEntryAndUpdatesSessionStatus() {
        VerificationSessionEntity session = sessionEntity(VerificationMethod.MANUAL_ENTRY);
        ManualIdentityRequest request = manualRequest();
        when(verificationSessionRepository.findDetailById(session.getId())).thenReturn(Optional.of(session));
        when(manualIdentityEntryRepository.findBySessionId(session.getId())).thenReturn(Optional.empty());
        when(manualIdentityEntryRepository.save(any(ManualIdentityEntry.class)))
                .thenAnswer(invocation -> {
                    ManualIdentityEntry entry = invocation.getArgument(0);
                    entry.prePersist();
                    return entry;
                });

        ManualIdentityResponse response = verificationService.saveManualIdentity(session.getId(), request);

        assertThat(response.transactionId()).isEqualTo(session.getId());
        assertThat(response.sessionStatus()).isEqualTo("IDENTITY_CAPTURED");
        assertThat(response.maskedNationalId()).isEqualTo("123******0123");
        assertThat(response.firstName()).isEqualTo("Somchai");
        assertThat(response.lastName()).isEqualTo("Jaidee");
        assertThat(response.updatedAt()).isNotNull();
    }

    @Test
    void saveManualIdentityRejectsDipChipSession() {
        VerificationSessionEntity session = sessionEntity(VerificationMethod.DIP_CHIP);
        when(verificationSessionRepository.findDetailById(session.getId())).thenReturn(Optional.of(session));

        assertThatThrownBy(() -> verificationService.saveManualIdentity(session.getId(), manualRequest()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Manual identity can only be captured for MANUAL_ENTRY sessions.");
    }

    @Test
    void saveDipChipPayloadCapturesDipChipSessionAndUpdatesSessionStatus() {
        VerificationSessionEntity session = sessionEntity(VerificationMethod.DIP_CHIP);
        DipChipPayloadRequest request = dipChipRequest();
        when(verificationSessionRepository.findDetailById(session.getId())).thenReturn(Optional.of(session));
        when(dipChipIdentityEntryRepository.findBySessionId(session.getId())).thenReturn(Optional.empty());
        when(dipChipIdentityEntryRepository.save(any(DipChipIdentityEntry.class)))
                .thenAnswer(invocation -> {
                    DipChipIdentityEntry entry = invocation.getArgument(0);
                    entry.prePersist();
                    return entry;
                });

        DipChipPayloadResponse response = verificationService.saveDipChipPayload(session.getId(), request);

        assertThat(response.transactionId()).isEqualTo(session.getId());
        assertThat(response.sessionStatus()).isEqualTo("IDENTITY_CAPTURED");
        assertThat(response.maskedNationalId()).isEqualTo("123******0123");
        assertThat(response.readerName()).isEqualTo("ACR39U");
        assertThat(response.readerSerialNumber()).isEqualTo("RD-001");
        assertThat(response.updatedAt()).isNotNull();
    }

    @Test
    void saveDipChipPayloadRejectsManualEntrySession() {
        VerificationSessionEntity session = sessionEntity(VerificationMethod.MANUAL_ENTRY);
        when(verificationSessionRepository.findDetailById(session.getId())).thenReturn(Optional.of(session));

        assertThatThrownBy(() -> verificationService.saveDipChipPayload(session.getId(), dipChipRequest()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Dip Chip payload can only be captured for DIP_CHIP sessions.");
    }
    @Test
    void saveDipChipPayloadRejectsInvalidCardDateOrder() {
        VerificationSessionEntity session = sessionEntity(VerificationMethod.DIP_CHIP);
        when(verificationSessionRepository.findDetailById(session.getId())).thenReturn(Optional.of(session));

        assertThatThrownBy(() -> verificationService.saveDipChipPayload(session.getId(), dipChipRequestWithInvalidDates()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Card expiry date must be on or after the issue date.");
    }

    private static VerificationSessionEntity sessionEntity(VerificationMethod method) {
        OperatorUser operator = OperatorUser.create("operator", "hash", "Operations User", OperatorRole.OPERATIONS);
        VerificationSessionEntity entity = VerificationSessionEntity.create(method, operator);
        entity.prePersist();
        return entity;
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

    private static ManualIdentityRequest manualRequest() {
        return new ManualIdentityRequest(
                "1234567890123",
                "Mr.",
                "Somchai",
                "Jaidee",
                LocalDate.parse("1990-01-31"),
                "JT1234567890"
        );
    }

    private static DipChipPayloadRequest dipChipRequest() {
        return new DipChipPayloadRequest(
                "1234567890123",
                "Mr.",
                "Somchai",
                "Jaidee",
                LocalDate.parse("1990-01-31"),
                "JT1234567890",
                LocalDate.parse("2021-02-01"),
                LocalDate.parse("2031-01-31"),
                "ACR39U",
                "RD-001",
                "{\"cid\":\"1234567890123\",\"reader\":\"ACR39U\"}"
        );
    }
    private static DipChipPayloadRequest dipChipRequestWithInvalidDates() {
        return new DipChipPayloadRequest(
                "1234567890123",
                "Mr.",
                "Somchai",
                "Jaidee",
                LocalDate.parse("1990-01-31"),
                "JT1234567890",
                LocalDate.parse("2031-01-31"),
                LocalDate.parse("2021-02-01"),
                "ACR39U",
                "RD-001",
                "{\"cid\":\"1234567890123\",\"reader\":\"ACR39U\"}"
        );
    }
}