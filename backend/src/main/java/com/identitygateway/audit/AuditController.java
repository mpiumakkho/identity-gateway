package com.identitygateway.audit;

import com.identitygateway.common.api.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/audit-events")
public class AuditController {

    private final AuditService auditService;

    public AuditController(AuditService auditService) {
        this.auditService = auditService;
    }

    @GetMapping
    public ApiResponse<List<AuditEventResponse>> auditEvents(
            @RequestParam(required = false) String eventType,
            @RequestParam(required = false) UUID operatorId,
            @RequestParam(defaultValue = "50") int limit
    ) {
        return ApiResponse.ok(auditService.recentEvents(eventType, operatorId, limit));
    }
}
