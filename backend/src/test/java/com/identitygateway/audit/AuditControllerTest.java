package com.identitygateway.audit;

import com.identitygateway.common.error.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuditController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class AuditControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuditService auditService;

    @Test
    void auditEventsReturnsRecentEvents() throws Exception {
        UUID operatorId = UUID.fromString("9e04e2eb-d74a-4d55-987c-f38660aa3060");
        when(auditService.recentEvents("AUTH_LOGIN_SUCCEEDED", operatorId, 25)).thenReturn(List.of(new AuditEventResponse(
                UUID.fromString("ca21e048-17f8-4b2f-a6b0-47c5d34172e2"),
                "AUTH_LOGIN_SUCCEEDED",
                null,
                null,
                "Operator login succeeded.",
                null,
                Instant.parse("2026-07-25T00:00:00Z")
        )));

        mockMvc.perform(get("/api/audit-events")
                        .param("eventType", "AUTH_LOGIN_SUCCEEDED")
                        .param("operatorId", operatorId.toString())
                        .param("limit", "25"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].eventId").value("ca21e048-17f8-4b2f-a6b0-47c5d34172e2"))
                .andExpect(jsonPath("$.data[0].eventType").value("AUTH_LOGIN_SUCCEEDED"));
    }
}
