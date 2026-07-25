package com.identitygateway.verification;

import com.identitygateway.auth.AuthenticatedOperator;
import com.identitygateway.auth.OperatorRole;
import com.identitygateway.auth.OperatorUser;
import com.identitygateway.auth.OperatorUserRepository;
import com.identitygateway.common.error.ResourceNotFoundException;
import com.identitygateway.audit.AuditEventType;
import com.identitygateway.audit.AuditService;
import com.identitygateway.dipchip.DipChipPayloadNormalizer;
import com.identitygateway.dopa.DopaGatewayResult;
import com.identitygateway.dopa.DopaIdentitySource;
import com.identitygateway.dopa.DopaValidationAttempt;
import com.identitygateway.dopa.DopaValidationAttemptRepository;
import com.identitygateway.dopa.DopaValidationHistoryResponse;
import com.identitygateway.dopa.DopaValidationResultStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VerificationServiceTest {

    @Mock
    private VerificationMethodRepository verificationMethodRepository;

    @Mock
    private VerificationSessionRepository verificationSessionRepository;

    @Mock
    private ManualIdentityEntryRepository manualIdentityEntryRepository;

    @Mock
    private DipChipIdentityEntryRepository dipChipIdentityEntryRepository;

    @Mock
    private VerificationDecisionRepository verificationDecisionRepository;

    @Mock
    private DopaValidationAttemptRepository dopaValidationAttemptRepository;

    @Mock
    private OperatorUserRepository operatorUserRepository;

    @Mock
    private AuditService auditService;

    private VerificationService verificationService;

    @BeforeEach
    void setUp() {
        verificationService = new VerificationService(
                verificationMethodRepository,
                verificationSessionRepository,
                manualIdentityEntryRepository,
                dipChipIdentityEntryRepository,
                verificationDecisionRepository,
                dopaValidationAttemptRepository,
                new DipChipPayloadNormalizer(),
                operatorUserRepository,
                auditService
        );
    }

    @Test
    void methodsReturnsConfiguredFlowEntryPoints() {
        when(verificationMethodRepository.findByEnabledTrueOrderBySortOrderAscIdAsc()).thenReturn(List.of(
                VerificationMethodEntity.create(VerificationMethod.DIP_CHIP, true, 10),
                VerificationMethodEntity.create(VerificationMethod.MANUAL_ENTRY, true, 20)
        ));

        List<VerificationMethodResponse> methods = verificationService.methods();

        assertThat(methods)
                .extracting(VerificationMethodResponse::id)
                .containsExactly("DIP_CHIP", "MANUAL_ENTRY");
    }

    @Test
    void methodCatalogReturnsAllConfiguredMethods() {
        when(verificationMethodRepository.findAllByOrderBySortOrderAscIdAsc()).thenReturn(List.of(
                VerificationMethodEntity.create(VerificationMethod.DIP_CHIP, false, 10),
                VerificationMethodEntity.create(VerificationMethod.MANUAL_ENTRY, true, 20)
        ));

        List<VerificationMethodResponse> methods = verificationService.methodCatalog();

        assertThat(methods)
                .extracting(VerificationMethodResponse::enabled)
                .containsExactly(false, true);
    }

    @Test
    void updateMethodStatusChangesCatalogEntryAndAuditsEvent() {
        AuthenticatedOperator authenticatedOperator = authenticatedOperator();
        VerificationMethodEntity method = VerificationMethodEntity.create(VerificationMethod.DIP_CHIP, true, 10);
        when(verificationMethodRepository.findById("DIP_CHIP")).thenReturn(Optional.of(method));

        VerificationMethodResponse response = verificationService.updateMethodStatus(
                authenticatedOperator,
                "DIP_CHIP",
                new UpdateVerificationMethodStatusRequest(false)
        );

        assertThat(response.enabled()).isFalse();
        verify(auditService).recordOperatorEvent(
                eq(AuditEventType.VERIFICATION_METHOD_STATUS_CHANGED),
                eq(authenticatedOperator.operatorId()),
                eq("Verification method status changed."),
                any()
        );
    }

    @Test
    void updateMethodStatusRejectsUnknownMethod() {
        assertThatThrownBy(() -> verificationService.updateMethodStatus(
                authenticatedOperator(),
                "VIDEO_CALL",
                new UpdateVerificationMethodStatusRequest(true)
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Unsupported verification method: VIDEO_CALL");
    }


    @Test
    void dashboardReturnsGroupedTransactionMetrics() {
        when(verificationSessionRepository.count()).thenReturn(3L);
        when(verificationSessionRepository.countByStatus()).thenReturn(List.of(statusMetric(VerificationStatus.CREATED, 1), statusMetric(VerificationStatus.APPROVED, 2)));
        when(verificationSessionRepository.countByMethod()).thenReturn(List.of(methodMetric(VerificationMethod.DIP_CHIP, 2), methodMetric(VerificationMethod.MANUAL_ENTRY, 1)));

        VerificationDashboardResponse response = verificationService.dashboard();

        assertThat(response.totalTransactions()).isEqualTo(3);
        assertThat(response.byStatus()).containsExactly(new VerificationMetricCount("CREATED", 1), new VerificationMetricCount("APPROVED", 2));
        assertThat(response.byMethod()).containsExactly(new VerificationMetricCount("DIP_CHIP", 2), new VerificationMetricCount("MANUAL_ENTRY", 1));
    }
    @Test
    void recentSessionsReturnsLatestTransactions() {
        VerificationSessionEntity entity = sessionEntity(VerificationMethod.DIP_CHIP);
        when(verificationSessionRepository.findRecent(isNull(), isNull(), any(Pageable.class))).thenReturn(List.of(entity));

        List<VerificationSessionResponse> responses = verificationService.recentSessions();

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).transactionId()).isEqualTo(entity.getId());
        assertThat(responses.get(0).createdBy().username()).isEqualTo("operator");
    }

    @Test
    void recentSessionsAppliesMethodAndStatusFilters() {
        VerificationSessionEntity entity = sessionEntity(VerificationMethod.DIP_CHIP);
        entity.markIdentityCaptured();
        entity.markDopaVerified();
        when(verificationSessionRepository.findRecent(eq(VerificationMethod.DIP_CHIP), eq(VerificationStatus.DOPA_VERIFIED), any(Pageable.class)))
                .thenReturn(List.of(entity));

        List<VerificationSessionResponse> responses = verificationService.recentSessions("DIP_CHIP", "DOPA_VERIFIED");

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).status()).isEqualTo("DOPA_VERIFIED");
        verify(verificationSessionRepository).findRecent(eq(VerificationMethod.DIP_CHIP), eq(VerificationStatus.DOPA_VERIFIED), any(Pageable.class));
    }

    @Test
    void recentSessionsRejectsUnsupportedStatusFilter() {
        assertThatThrownBy(() -> verificationService.recentSessions(null, "DONE"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Unsupported verification status: DONE");
    }

    @Test
    void sessionReturnsTransactionDetail() {
        VerificationSessionEntity entity = sessionEntity(VerificationMethod.DIP_CHIP);
        when(verificationSessionRepository.findDetailById(entity.getId())).thenReturn(Optional.of(entity));

        VerificationSessionDetailResponse response = verificationService.session(entity.getId());

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
    void dopaValidationHistoryReturnsLatestAttempts() {
        VerificationSessionEntity session = sessionEntity(VerificationMethod.DIP_CHIP);
        DopaValidationAttempt attempt = DopaValidationAttempt.create(
                session,
                DopaIdentitySource.DIP_CHIP,
                new DopaGatewayResult(DopaValidationResultStatus.MATCHED, "DOPA-0000", "Citizen identity matched."),
                "CONSENT-001"
        );
        attempt.prePersist();
        when(verificationSessionRepository.findDetailById(session.getId())).thenReturn(Optional.of(session));
        when(dopaValidationAttemptRepository.findTop10BySessionIdOrderByValidatedAtDesc(session.getId())).thenReturn(List.of(attempt));

        List<DopaValidationHistoryResponse> responses = verificationService.dopaValidationHistory(session.getId());

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).validationStatus()).isEqualTo("MATCHED");
        assertThat(responses.get(0).responseCode()).isEqualTo("DOPA-0000");
        assertThat(responses.get(0).consentReference()).isEqualTo("CONSENT-001");
    }

    @Test
    void startSessionPersistsCreatedSessionForOperator() {
        AuthenticatedOperator authenticatedOperator = authenticatedOperator();
        OperatorUser operator = OperatorUser.create("operator", "hash", "Operations User", OperatorRole.OPERATIONS);
        when(verificationMethodRepository.existsByIdAndEnabledTrue("DIP_CHIP")).thenReturn(true);
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
    void startSessionRejectsDisabledMethod() {
        when(verificationMethodRepository.existsByIdAndEnabledTrue("DIP_CHIP")).thenReturn(false);

        assertThatThrownBy(() -> verificationService.startSession(authenticatedOperator(), new StartVerificationRequest("DIP_CHIP")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Verification method is disabled: DIP_CHIP");
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

        ManualIdentityResponse response = verificationService.saveManualIdentity(authenticatedOperator(), session.getId(), request);

        assertThat(response.transactionId()).isEqualTo(session.getId());
        assertThat(response.sessionStatus()).isEqualTo("IDENTITY_CAPTURED");
        assertThat(response.maskedNationalId()).isEqualTo("123******0121");
        assertThat(response.firstName()).isEqualTo("Somchai");
        assertThat(response.lastName()).isEqualTo("Jaidee");
        assertThat(response.updatedAt()).isNotNull();
    }

    @Test
    void saveManualIdentityRejectsDipChipSession() {
        VerificationSessionEntity session = sessionEntity(VerificationMethod.DIP_CHIP);
        when(verificationSessionRepository.findDetailById(session.getId())).thenReturn(Optional.of(session));

        assertThatThrownBy(() -> verificationService.saveManualIdentity(authenticatedOperator(), session.getId(), manualRequest()))
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

        DipChipPayloadResponse response = verificationService.saveDipChipPayload(authenticatedOperator(), session.getId(), request);

        assertThat(response.transactionId()).isEqualTo(session.getId());
        assertThat(response.sessionStatus()).isEqualTo("IDENTITY_CAPTURED");
        assertThat(response.maskedNationalId()).isEqualTo("123******0121");
        assertThat(response.readerName()).isEqualTo("ACR39U Reader");
        assertThat(response.readerSerialNumber()).isEqualTo("RD-001");
        assertThat(response.updatedAt()).isNotNull();
    }

    @Test
    void saveDipChipPayloadRejectsManualEntrySession() {
        VerificationSessionEntity session = sessionEntity(VerificationMethod.MANUAL_ENTRY);
        when(verificationSessionRepository.findDetailById(session.getId())).thenReturn(Optional.of(session));

        assertThatThrownBy(() -> verificationService.saveDipChipPayload(authenticatedOperator(), session.getId(), dipChipRequest()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Dip Chip payload can only be captured for DIP_CHIP sessions.");
    }
    @Test
    void closeSessionApprovesDopaVerifiedTransaction() {
        AuthenticatedOperator authenticatedOperator = authenticatedOperator();
        VerificationSessionEntity session = sessionEntity(VerificationMethod.DIP_CHIP);
        session.markDopaVerified();
        OperatorUser operator = OperatorUser.create("operator", "hash", "Operations User", OperatorRole.OPERATIONS);
        when(verificationSessionRepository.findDetailById(session.getId())).thenReturn(Optional.of(session));
        when(verificationDecisionRepository.existsBySessionId(session.getId())).thenReturn(false);
        when(operatorUserRepository.getReferenceById(authenticatedOperator.operatorId())).thenReturn(operator);
        when(verificationDecisionRepository.save(any(VerificationDecisionEntity.class)))
                .thenAnswer(invocation -> {
                    VerificationDecisionEntity entity = invocation.getArgument(0);
                    entity.prePersist();
                    return entity;
                });

        VerificationCloseoutResponse response = verificationService.closeSession(
                authenticatedOperator,
                session.getId(),
                new CloseVerificationRequest("APPROVED", "Matched and reviewed.")
        );

        assertThat(response.transactionId()).isEqualTo(session.getId());
        assertThat(response.sessionStatus()).isEqualTo("APPROVED");
        assertThat(response.decision()).isEqualTo("APPROVED");
        assertThat(response.notes()).isEqualTo("Matched and reviewed.");
        assertThat(response.decidedBy().username()).isEqualTo("operator");
        assertThat(response.decidedAt()).isNotNull();
        assertThat(session.getStatus()).isEqualTo(VerificationStatus.APPROVED);
    }

    @Test
    void closeSessionRejectsBeforeDopaValidation() {
        VerificationSessionEntity session = sessionEntity(VerificationMethod.DIP_CHIP);
        session.markIdentityCaptured();
        when(verificationSessionRepository.findDetailById(session.getId())).thenReturn(Optional.of(session));

        assertThatThrownBy(() -> verificationService.closeSession(
                authenticatedOperator(),
                session.getId(),
                new CloseVerificationRequest("APPROVED", "Reviewed.")
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("DOPA validation must be completed before closeout.");
    }

    @Test
    void closeSessionRejectsApprovalForDopaRejectedTransaction() {
        VerificationSessionEntity session = sessionEntity(VerificationMethod.MANUAL_ENTRY);
        session.markDopaRejected();
        when(verificationSessionRepository.findDetailById(session.getId())).thenReturn(Optional.of(session));

        assertThatThrownBy(() -> verificationService.closeSession(
                authenticatedOperator(),
                session.getId(),
                new CloseVerificationRequest("APPROVED", "Override.")
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("DOPA rejected sessions cannot be approved.");
    }

    @Test
    void saveManualIdentityRejectsClosedSession() {
        VerificationSessionEntity session = sessionEntity(VerificationMethod.MANUAL_ENTRY);
        session.close(VerificationDecision.REJECTED);
        when(verificationSessionRepository.findDetailById(session.getId())).thenReturn(Optional.of(session));

        assertThatThrownBy(() -> verificationService.saveManualIdentity(authenticatedOperator(), session.getId(), manualRequest()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Verification session is already closed.");
    }

    @Test
    void saveDipChipPayloadRejectsInvalidCardDateOrder() {
        VerificationSessionEntity session = sessionEntity(VerificationMethod.DIP_CHIP);
        when(verificationSessionRepository.findDetailById(session.getId())).thenReturn(Optional.of(session));

        assertThatThrownBy(() -> verificationService.saveDipChipPayload(authenticatedOperator(), session.getId(), dipChipRequestWithInvalidDates()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Card expiry date must be on or after the issue date.");
    }


    private static VerificationStatusMetric statusMetric(VerificationStatus status, long total) {
        return new VerificationStatusMetric() {
            @Override
            public VerificationStatus getStatus() {
                return status;
            }

            @Override
            public long getTotal() {
                return total;
            }
        };
    }

    private static VerificationMethodMetric methodMetric(VerificationMethod method, long total) {
        return new VerificationMethodMetric() {
            @Override
            public VerificationMethod getMethod() {
                return method;
            }

            @Override
            public long getTotal() {
                return total;
            }
        };
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
                "1234567890121",
                "Mr.",
                "Somchai",
                "Jaidee",
                LocalDate.parse("1990-01-31"),
                "JT1234567890"
        );
    }

    private static DipChipPayloadRequest dipChipRequest() {
        return new DipChipPayloadRequest(
                "1234567890121",
                "Mr.",
                "Somchai",
                "Jaidee",
                LocalDate.parse("1990-01-31"),
                "JT1234567890",
                LocalDate.parse("2021-02-01"),
                LocalDate.parse("2031-01-31"),
                " ACR39U  Reader ",
                " rd-001 ",
                "{\"cid\":\"1234567890121\",\"reader\":\"ACR39U\"}"
        );
    }

    private static DipChipPayloadRequest dipChipRequestWithInvalidDates() {
        return new DipChipPayloadRequest(
                "1234567890121",
                "Mr.",
                "Somchai",
                "Jaidee",
                LocalDate.parse("1990-01-31"),
                "JT1234567890",
                LocalDate.parse("2031-01-31"),
                LocalDate.parse("2021-02-01"),
                " ACR39U  Reader ",
                " rd-001 ",
                "{\"cid\":\"1234567890121\",\"reader\":\"ACR39U\"}"
        );
    }
}
