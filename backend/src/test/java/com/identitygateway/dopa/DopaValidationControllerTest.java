package com.identitygateway.dopa;

import com.identitygateway.auth.AuthenticatedOperator;
import com.identitygateway.auth.BearerTokenResolver;
import com.identitygateway.auth.OperatorSessionService;
import com.identitygateway.common.error.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.UUID;

import static org.hamcrest.Matchers.blankOrNullString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DopaValidationController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class DopaValidationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BearerTokenResolver bearerTokenResolver;

    @MockitoBean
    private OperatorSessionService operatorSessionService;

    @MockitoBean
    private DopaValidationService dopaValidationService;

    @Test
    void validateReturnsDopaResult() throws Exception {
        UUID transactionId = UUID.fromString("b9d38258-8ec4-4645-a6ca-e901e1c1766a");
        when(dopaValidationService.validate(any(), any(UUID.class), any(DopaValidationRequest.class))).thenReturn(new DopaValidationResponse(
                transactionId,
                "DOPA_VERIFIED",
                "MATCHED",
                "DIP_CHIP",
                "123******0123",
                "DOPA-0000",
                "Citizen identity matched.",
                "CONSENT-001",
                Instant.parse("2026-07-25T01:30:00Z")
        ));

        mockMvc.perform(post("/api/verification/sessions/{transactionId}/dopa-validation", transactionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"consentReference\":\"CONSENT-001\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.transactionId").value(transactionId.toString()))
                .andExpect(jsonPath("$.data.sessionStatus").value("DOPA_VERIFIED"))
                .andExpect(jsonPath("$.data.validationStatus").value("MATCHED"))
                .andExpect(jsonPath("$.data.maskedNationalId").value("123******0123"))
                .andExpect(jsonPath("$.data.responseCode").value("DOPA-0000"))
                .andExpect(jsonPath("$.data.nationalId").doesNotExist())
                .andExpect(jsonPath("$.data.laserCode").doesNotExist());
    }

    @Test
    void validateRejectsBlankConsentReference() throws Exception {
        UUID transactionId = UUID.fromString("b9d38258-8ec4-4645-a6ca-e901e1c1766a");

        mockMvc.perform(post("/api/verification/sessions/{transactionId}/dopa-validation", transactionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"consentReference\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message", not(blankOrNullString())));
    }
}