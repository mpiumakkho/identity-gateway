package com.identitygateway.verification;

import com.identitygateway.auth.AuthenticatedOperator;
import com.identitygateway.auth.BearerTokenResolver;
import com.identitygateway.auth.OperatorRole;
import com.identitygateway.auth.OperatorSessionService;
import com.identitygateway.common.error.GlobalExceptionHandler;
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
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.blankOrNullString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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