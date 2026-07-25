package com.identitygateway.verification;

import com.identitygateway.auth.AuthenticatedOperator;
import com.identitygateway.auth.BearerTokenResolver;
import com.identitygateway.auth.OperatorRole;
import com.identitygateway.auth.OperatorSessionService;
import com.identitygateway.common.error.GlobalExceptionHandler;
import com.identitygateway.audit.AuditEventResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.blankOrNullString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(VerificationController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class VerificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BearerTokenResolver bearerTokenResolver;

    @MockitoBean
    private OperatorSessionService operatorSessionService;

    @MockitoBean
    private VerificationService verificationService;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void methodsReturnsEnabledVerificationMethods() throws Exception {
        when(verificationService.methods()).thenReturn(List.of(
                new VerificationMethodResponse("DIP_CHIP", "Dip Chip", "Read citizen card data from a supported reader.", true),
                new VerificationMethodResponse("MANUAL_ENTRY", "Manual Entry", "Capture citizen data through a controlled form.", true)
        ));

        mockMvc.perform(get("/api/verification/methods"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.data[0].id").value("DIP_CHIP"))
                .andExpect(jsonPath("$.data[0].label").value("Dip Chip"))
                .andExpect(jsonPath("$.data[0].enabled").value(true))
                .andExpect(jsonPath("$.data[1].id").value("MANUAL_ENTRY"))
                .andExpect(jsonPath("$.data[1].label").value("Manual Entry"))
                .andExpect(jsonPath("$.data[1].enabled").value(true));
    }

    @Test
    void sessionsReturnsRecentTransactions() throws Exception {
        when(verificationService.recentSessions()).thenReturn(List.of(sessionResponse()));

        mockMvc.perform(get("/api/verification/sessions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].transactionId").value("b9d38258-8ec4-4645-a6ca-e901e1c1766a"))
                .andExpect(jsonPath("$.data[0].createdBy.username").value("operator"));
    }

    @Test
    void sessionReturnsTransactionDetail() throws Exception {
        UUID transactionId = UUID.fromString("b9d38258-8ec4-4645-a6ca-e901e1c1766a");
        when(verificationService.session(transactionId)).thenReturn(sessionResponse());

        mockMvc.perform(get("/api/verification/sessions/{transactionId}", transactionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.transactionId").value(transactionId.toString()))
                .andExpect(jsonPath("$.data.status").value("CREATED"));
    }

    @Test
    void auditEventsReturnsTransactionTimeline() throws Exception {
        UUID transactionId = UUID.fromString("b9d38258-8ec4-4645-a6ca-e901e1c1766a");
        when(verificationService.auditEvents(transactionId)).thenReturn(List.of(new AuditEventResponse(
                UUID.fromString("2c287647-1807-4a7b-9f37-b53a6c3a0228"),
                "VERIFICATION_SESSION_CREATED",
                transactionId,
                new SessionOperatorResponse(
                        UUID.fromString("9e04e2eb-d74a-4d55-987c-f38660aa3060"),
                        "operator",
                        "Operations User"
                ),
                "Verification session created.",
                "{\"method\":\"DIP_CHIP\"}",
                Instant.parse("2026-07-25T00:01:00Z")
        )));

        mockMvc.perform(get("/api/verification/sessions/{transactionId}/audit-events", transactionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].eventType").value("VERIFICATION_SESSION_CREATED"))
                .andExpect(jsonPath("$.data[0].operator.username").value("operator"))
                .andExpect(jsonPath("$.data[0].metadataJson").value("{\"method\":\"DIP_CHIP\"}"));
    }

    @Test
    void startSessionCreatesTransactionSession() throws Exception {
        setAuthenticatedOperator();
        when(verificationService.startSession(any(AuthenticatedOperator.class), any(StartVerificationRequest.class))).thenReturn(sessionResponse());

        mockMvc.perform(post("/api/verification/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"method\":\"DIP_CHIP\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.transactionId", not(blankOrNullString())))
                .andExpect(jsonPath("$.data.method").value("DIP_CHIP"))
                .andExpect(jsonPath("$.data.status").value("CREATED"))
                .andExpect(jsonPath("$.data.createdBy.username").value("operator"))
                .andExpect(jsonPath("$.data.createdAt").exists());
    }

    @Test
    void startSessionRejectsBlankMethod() throws Exception {
        setAuthenticatedOperator();

        mockMvc.perform(post("/api/verification/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"method\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message", not(blankOrNullString())));
    }

    @Test
    void saveManualIdentityReturnsCapturedIdentity() throws Exception {
        setAuthenticatedOperator();
        UUID transactionId = UUID.fromString("b9d38258-8ec4-4645-a6ca-e901e1c1766a");
        ManualIdentityResponse response = new ManualIdentityResponse(
                transactionId,
                "IDENTITY_CAPTURED",
                "123******0123",
                "Mr.",
                "Somchai",
                "Jaidee",
                LocalDate.parse("1990-01-31"),
                Instant.parse("2026-07-25T01:15:00Z")
        );
        when(verificationService.saveManualIdentity(any(), any(UUID.class), any(ManualIdentityRequest.class))).thenReturn(response);

        mockMvc.perform(put("/api/verification/sessions/{transactionId}/manual-identity", transactionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nationalId": "1234567890123",
                                  "title": "Mr.",
                                  "firstName": "Somchai",
                                  "lastName": "Jaidee",
                                  "dateOfBirth": "1990-01-31",
                                  "laserCode": "JT1234567890"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.transactionId").value(transactionId.toString()))
                .andExpect(jsonPath("$.data.sessionStatus").value("IDENTITY_CAPTURED"))
                .andExpect(jsonPath("$.data.maskedNationalId").value("123******0123"))
                .andExpect(jsonPath("$.data.firstName").value("Somchai"))
                .andExpect(jsonPath("$.data.laserCode").doesNotExist());
    }

    @Test
    void saveManualIdentityRejectsInvalidNationalId() throws Exception {
        UUID transactionId = UUID.fromString("b9d38258-8ec4-4645-a6ca-e901e1c1766a");

        mockMvc.perform(put("/api/verification/sessions/{transactionId}/manual-identity", transactionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nationalId": "123",
                                  "title": "Mr.",
                                  "firstName": "Somchai",
                                  "lastName": "Jaidee",
                                  "dateOfBirth": "1990-01-31",
                                  "laserCode": "JT1234567890"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message", not(blankOrNullString())));
    }

    @Test
    void saveDipChipPayloadReturnsCapturedIdentity() throws Exception {
        setAuthenticatedOperator();
        UUID transactionId = UUID.fromString("b9d38258-8ec4-4645-a6ca-e901e1c1766a");
        DipChipPayloadResponse response = new DipChipPayloadResponse(
                transactionId,
                "IDENTITY_CAPTURED",
                "123******0123",
                "Mr.",
                "Somchai",
                "Jaidee",
                LocalDate.parse("1990-01-31"),
                LocalDate.parse("2021-02-01"),
                LocalDate.parse("2031-01-31"),
                "ACR39U",
                "RD-001",
                Instant.parse("2026-07-25T01:20:00Z")
        );
        when(verificationService.saveDipChipPayload(any(), any(UUID.class), any(DipChipPayloadRequest.class))).thenReturn(response);

        mockMvc.perform(put("/api/verification/sessions/{transactionId}/dip-chip-payload", transactionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nationalId": "1234567890123",
                                  "title": "Mr.",
                                  "firstName": "Somchai",
                                  "lastName": "Jaidee",
                                  "dateOfBirth": "1990-01-31",
                                  "laserCode": "JT1234567890",
                                  "cardIssueDate": "2021-02-01",
                                  "cardExpiryDate": "2031-01-31",
                                  "readerName": "ACR39U",
                                  "readerSerialNumber": "RD-001",
                                  "rawPayload": "CID=1234567890123;READER=ACR39U"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.transactionId").value(transactionId.toString()))
                .andExpect(jsonPath("$.data.sessionStatus").value("IDENTITY_CAPTURED"))
                .andExpect(jsonPath("$.data.maskedNationalId").value("123******0123"))
                .andExpect(jsonPath("$.data.readerSerialNumber").value("RD-001"))
                .andExpect(jsonPath("$.data.laserCode").doesNotExist())
                .andExpect(jsonPath("$.data.rawPayload").doesNotExist());
    }

    @Test
    void closeSessionReturnsDecisionSummary() throws Exception {
        setAuthenticatedOperator();
        UUID transactionId = UUID.fromString("b9d38258-8ec4-4645-a6ca-e901e1c1766a");
        VerificationCloseoutResponse response = new VerificationCloseoutResponse(
                transactionId,
                "APPROVED",
                "APPROVED",
                "Matched and reviewed.",
                new SessionOperatorResponse(
                        UUID.fromString("9e04e2eb-d74a-4d55-987c-f38660aa3060"),
                        "operator",
                        "Operations User"
                ),
                Instant.parse("2026-07-25T02:00:00Z")
        );
        when(verificationService.closeSession(any(AuthenticatedOperator.class), any(UUID.class), any(CloseVerificationRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/verification/sessions/{transactionId}/closeout", transactionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "decision": "APPROVED",
                                  "notes": "Matched and reviewed."
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.transactionId").value(transactionId.toString()))
                .andExpect(jsonPath("$.data.sessionStatus").value("APPROVED"))
                .andExpect(jsonPath("$.data.decision").value("APPROVED"))
                .andExpect(jsonPath("$.data.notes").value("Matched and reviewed."))
                .andExpect(jsonPath("$.data.decidedBy.username").value("operator"))
                .andExpect(jsonPath("$.data.decidedAt").exists());
    }

    @Test
    void closeSessionRejectsBlankDecision() throws Exception {
        setAuthenticatedOperator();
        UUID transactionId = UUID.fromString("b9d38258-8ec4-4645-a6ca-e901e1c1766a");

        mockMvc.perform(post("/api/verification/sessions/{transactionId}/closeout", transactionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "decision": "",
                                  "notes": "Reviewed."
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message", not(blankOrNullString())));
    }

    @Test
    void saveDipChipPayloadRejectsInvalidPayload() throws Exception {
        UUID transactionId = UUID.fromString("b9d38258-8ec4-4645-a6ca-e901e1c1766a");

        mockMvc.perform(put("/api/verification/sessions/{transactionId}/dip-chip-payload", transactionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nationalId": "123",
                                  "title": "Mr.",
                                  "firstName": "Somchai",
                                  "lastName": "Jaidee",
                                  "dateOfBirth": "1990-01-31",
                                  "laserCode": "JT1234567890",
                                  "cardIssueDate": "2021-02-01",
                                  "cardExpiryDate": "2031-01-31",
                                  "readerName": "ACR39U",
                                  "readerSerialNumber": "RD-001",
                                  "rawPayload": "{}"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message", not(blankOrNullString())));
    }

    private static VerificationSessionResponse sessionResponse() {
        return new VerificationSessionResponse(
                UUID.fromString("b9d38258-8ec4-4645-a6ca-e901e1c1766a"),
                "DIP_CHIP",
                "CREATED",
                new SessionOperatorResponse(
                        UUID.fromString("9e04e2eb-d74a-4d55-987c-f38660aa3060"),
                        "operator",
                        "Operations User"
                ),
                Instant.parse("2026-07-25T00:00:00Z")
        );
    }

    private static void setAuthenticatedOperator() {
        AuthenticatedOperator operator = new AuthenticatedOperator(
                UUID.fromString("9e04e2eb-d74a-4d55-987c-f38660aa3060"),
                "operator",
                "Operations User",
                OperatorRole.OPERATIONS,
                Instant.parse("2026-07-25T08:00:00Z")
        );
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(new UsernamePasswordAuthenticationToken(operator, null, operator.authorities()));
        SecurityContextHolder.setContext(context);
    }
}