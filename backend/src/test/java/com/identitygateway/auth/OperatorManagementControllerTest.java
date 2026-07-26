package com.identitygateway.auth;

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

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.blankOrNullString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OperatorManagementController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class OperatorManagementControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OperatorManagementService operatorManagementService;

    @MockitoBean
    private BearerTokenResolver bearerTokenResolver;

    @MockitoBean
    private OperatorSessionService operatorSessionService;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void operatorsReturnsOperatorListWithoutPasswordHash() throws Exception {
        when(operatorManagementService.operators()).thenReturn(List.of(operatorResponse(true)));

        mockMvc.perform(get("/api/operators"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].username").value("operator"))
                .andExpect(jsonPath("$.data[0].enabled").value(true))
                .andExpect(jsonPath("$.data[0].passwordHash").doesNotExist());
    }

    @Test
    void createOperatorReturnsCreatedOperator() throws Exception {
        setAuthenticatedAdmin();
        when(operatorManagementService.createOperator(any(AuthenticatedOperator.class), any(CreateOperatorRequest.class)))
                .thenReturn(operatorResponse(true));

        mockMvc.perform(post("/api/operators")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "operator",
                                  "password": "very-secret-123",
                                  "displayName": "Operations User",
                                  "role": "OPERATIONS"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.operatorId", not(blankOrNullString())))
                .andExpect(jsonPath("$.data.username").value("operator"))
                .andExpect(jsonPath("$.data.role").value("OPERATIONS"))
                .andExpect(jsonPath("$.data.password").doesNotExist())
                .andExpect(jsonPath("$.data.passwordHash").doesNotExist());
    }

    @Test
    void createOperatorRejectsWeakPassword() throws Exception {
        setAuthenticatedAdmin();

        mockMvc.perform(post("/api/operators")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "operator",
                                  "password": "short",
                                  "displayName": "Operations User",
                                  "role": "OPERATIONS"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void changePasswordReturnsOperatorSummary() throws Exception {
        setAuthenticatedAdmin();
        UUID operatorId = UUID.fromString("17f9946f-f754-4929-aa27-92f8db7c4a88");
        when(operatorManagementService.changePassword(any(AuthenticatedOperator.class), eq(operatorId), any(ChangeOperatorPasswordRequest.class)))
                .thenReturn(operatorResponse(true));

        mockMvc.perform(put("/api/operators/{operatorId}/password", operatorId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"password\":\"new-secret-123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value("operator"))
                .andExpect(jsonPath("$.data.passwordHash").doesNotExist());
    }

    @Test
    void disableOperatorReturnsDisabledOperator() throws Exception {
        setAuthenticatedAdmin();
        UUID operatorId = UUID.fromString("17f9946f-f754-4929-aa27-92f8db7c4a88");
        when(operatorManagementService.disableOperator(any(AuthenticatedOperator.class), eq(operatorId)))
                .thenReturn(operatorResponse(false));

        mockMvc.perform(put("/api/operators/{operatorId}/disabled", operatorId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.enabled").value(false))
                .andExpect(jsonPath("$.data.disabledAt").value("2026-07-25T00:30:00Z"));
    }

    private static OperatorUserResponse operatorResponse(boolean enabled) {
        return new OperatorUserResponse(
                UUID.fromString("17f9946f-f754-4929-aa27-92f8db7c4a88"),
                "operator",
                "Operations User",
                OperatorRole.OPERATIONS,
                enabled,
                Instant.parse("2026-07-25T00:00:00Z"),
                Instant.parse("2026-07-25T00:15:00Z"),
                enabled ? null : Instant.parse("2026-07-25T00:30:00Z")
        );
    }

    private static void setAuthenticatedAdmin() {
        AuthenticatedOperator admin = new AuthenticatedOperator(
                UUID.fromString("9e04e2eb-d74a-4d55-987c-f38660aa3060"),
                "admin",
                "Admin User",
                OperatorRole.ADMIN,
                OperatorRole.ADMIN.permissions(),
                Instant.parse("2026-07-25T08:00:00Z")
        );
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(new UsernamePasswordAuthenticationToken(admin, null, admin.authorities()));
        SecurityContextHolder.setContext(context);
    }
}