package com.identitygateway.auth;

import com.identitygateway.audit.AuditEventRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private OperatorUserRepository operatorUserRepository;

    @Autowired
    private OperatorSessionRepository operatorSessionRepository;

    @Autowired
    private AuditEventRepository auditEventRepository;

    @Autowired
    private TokenHashingService tokenHashingService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void cleanDatabase() {
        auditEventRepository.deleteAll();
        operatorSessionRepository.deleteAll();
        operatorUserRepository.deleteAll();
    }

    @Test
    void verificationEndpointsRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/verification/methods"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"));
    }

    @Test
    void validBearerTokenCanAccessCurrentOperatorAndVerification() throws Exception {
        String accessToken = "valid-token";
        createSession(accessToken);

        mockMvc.perform(get("/api/auth/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value("operator"))
                .andExpect(jsonPath("$.data.displayName").value("Operations User"))
                .andExpect(jsonPath("$.data.role").value("OPERATIONS"));

        mockMvc.perform(get("/api/verification/methods")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"));
    }

    @Test
    void logoutRevokesBearerToken() throws Exception {
        String accessToken = "token-to-revoke";
        createSession(accessToken);

        mockMvc.perform(post("/api/auth/logout")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.signedOut").value(true));

        mockMvc.perform(get("/api/verification/methods")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"));
    }

    private void createSession(String accessToken) {
        OperatorUser operator = operatorUserRepository.save(OperatorUser.create(
                "operator",
                passwordEncoder.encode("StrongPassword123!"),
                "Operations User",
                OperatorRole.OPERATIONS
        ));

        operatorSessionRepository.save(OperatorSession.create(
                operator,
                tokenHashingService.hash(accessToken),
                Instant.parse("2026-07-25T23:59:59Z")
        ));
    }
}