package com.identitygateway.auth;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void loginReturnsNotImplementedUntilAuthStrategyIsAdded() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"operator\",\"password\":\"secret\"}"))
                .andExpect(status().isNotImplemented())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.code").value("AUTH_NOT_IMPLEMENTED"))
                .andExpect(jsonPath("$.data").doesNotExist());
    }
}