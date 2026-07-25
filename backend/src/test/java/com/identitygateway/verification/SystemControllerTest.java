package com.identitygateway.verification;

import com.identitygateway.auth.BearerTokenResolver;
import com.identitygateway.auth.OperatorSessionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SystemController.class)
@AutoConfigureMockMvc(addFilters = false)
class SystemControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BearerTokenResolver bearerTokenResolver;

    @MockitoBean
    private OperatorSessionService operatorSessionService;

    @MockitoBean
    private SystemHealthService systemHealthService;

    @Test
    void healthReturnsServiceStatus() throws Exception {
        when(systemHealthService.health()).thenReturn(new SystemHealthResponse(
                "identity-gateway",
                "UP",
                "UP",
                Instant.parse("2026-07-25T03:00:00Z")
        ));

        mockMvc.perform(get("/api/system/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.service").value("identity-gateway"))
                .andExpect(jsonPath("$.data.status").value("UP"))
                .andExpect(jsonPath("$.data.databaseStatus").value("UP"))
                .andExpect(jsonPath("$.data.checkedAt").value("2026-07-25T03:00:00Z"))
                .andExpect(jsonPath("$.timestamp").exists());
    }
}
