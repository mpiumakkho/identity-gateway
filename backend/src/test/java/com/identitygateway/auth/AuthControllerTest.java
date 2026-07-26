package com.identitygateway.auth;

import com.identitygateway.common.error.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private BearerTokenResolver bearerTokenResolver;

    @MockitoBean
    private OperatorSessionService operatorSessionService;

    @Test
    void loginReturnsAuthenticatedOperatorSession() throws Exception {
        when(authService.login(any(LoginRequest.class))).thenReturn(new LoginResponse(
                UUID.fromString("9e04e2eb-d74a-4d55-987c-f38660aa3060"),
                "operator",
                "Operations User",
                OperatorRole.OPERATIONS,
                Instant.parse("2026-07-25T00:00:00Z"),
                "issued-token",
                Instant.parse("2026-07-25T08:00:00Z")
        ));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"operator\",\"password\":\"secret\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.operatorId").value("9e04e2eb-d74a-4d55-987c-f38660aa3060"))
                .andExpect(jsonPath("$.data.username").value("operator"))
                .andExpect(jsonPath("$.data.displayName").value("Operations User"))
                .andExpect(jsonPath("$.data.role").value("OPERATIONS"))
                .andExpect(jsonPath("$.data.accessToken").value("issued-token"))
                .andExpect(jsonPath("$.data.expiresAt").value("2026-07-25T08:00:00Z"));
    }

    @Test
    void sessionsReturnsActiveOperatorSessions() throws Exception {
        AuthenticatedOperator operator = authenticatedOperator();
        UsernamePasswordAuthenticationToken authenticatedRequest = authenticatedRequest(operator);
        UUID sessionId = UUID.fromString("4ccf1d23-1be5-4356-af6f-cb7adf0b9426");

        when(bearerTokenResolver.resolve("Bearer current-token")).thenReturn(Optional.of("current-token"));
        when(authService.activeSessions(any(), eq("current-token"))).thenReturn(List.of(new OperatorSessionResponse(
                sessionId,
                true,
                Instant.parse("2026-07-25T00:00:00Z"),
                Instant.parse("2026-07-25T08:00:00Z")
        )));

        mockMvc.perform(get("/api/auth/sessions")
                        .with(authentication(authenticatedRequest))
                        .header(HttpHeaders.AUTHORIZATION, "Bearer current-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].sessionId").value("4ccf1d23-1be5-4356-af6f-cb7adf0b9426"))
                .andExpect(jsonPath("$.data[0].current").value(true));
    }

    @Test
    void revokeSessionReturnsRevocationResult() throws Exception {
        AuthenticatedOperator operator = authenticatedOperator();
        UsernamePasswordAuthenticationToken authenticatedRequest = authenticatedRequest(operator);
        UUID sessionId = UUID.fromString("4ccf1d23-1be5-4356-af6f-cb7adf0b9426");

        when(bearerTokenResolver.resolve("Bearer current-token")).thenReturn(Optional.of("current-token"));
        when(authService.revokeSession(any(), eq(sessionId), eq("current-token"))).thenReturn(new SessionRevocationResponse(true));

        mockMvc.perform(delete("/api/auth/sessions/{sessionId}", sessionId)
                        .with(authentication(authenticatedRequest))
                        .header(HttpHeaders.AUTHORIZATION, "Bearer current-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.revoked").value(true));
    }

    @Test
    void changePasswordReturnsPasswordChangeResult() throws Exception {
        AuthenticatedOperator operator = authenticatedOperator();
        UsernamePasswordAuthenticationToken authenticatedRequest = authenticatedRequest(operator);

        when(bearerTokenResolver.resolve("Bearer current-token")).thenReturn(Optional.of("current-token"));
        when(authService.changeOwnPassword(any(), eq("current-token"), any(ChangeOwnPasswordRequest.class)))
                .thenReturn(new PasswordChangeResponse(true));

        mockMvc.perform(put("/api/auth/password")
                        .with(authentication(authenticatedRequest))
                        .header(HttpHeaders.AUTHORIZATION, "Bearer current-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"current-password\",\"newPassword\":\"new-secret-123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.passwordChanged").value(true));
    }

    private static UsernamePasswordAuthenticationToken authenticatedRequest(AuthenticatedOperator operator) {
        return new UsernamePasswordAuthenticationToken(operator, null, operator.authorities());
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
}
