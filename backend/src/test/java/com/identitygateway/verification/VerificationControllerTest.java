package com.identitygateway.verification;

import com.identitygateway.common.error.GlobalExceptionHandler;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.blankOrNullString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(VerificationController.class)
@Import(GlobalExceptionHandler.class)
class VerificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void methodsReturnsEnabledVerificationMethods() throws Exception {
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
    void startSessionCreatesTransactionSession() throws Exception {
        mockMvc.perform(post("/api/verification/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"method\":\"DIP_CHIP\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.transactionId", not(blankOrNullString())))
                .andExpect(jsonPath("$.data.method").value("DIP_CHIP"))
                .andExpect(jsonPath("$.data.status").value("CREATED"))
                .andExpect(jsonPath("$.data.createdAt").exists());
    }

    @Test
    void startSessionRejectsBlankMethod() throws Exception {
        mockMvc.perform(post("/api/verification/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"method\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message", not(blankOrNullString())));
    }
}