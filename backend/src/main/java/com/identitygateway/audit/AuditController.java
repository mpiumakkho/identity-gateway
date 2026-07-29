package com.identitygateway.audit;

import com.identitygateway.common.api.ApiResponse;
import com.identitygateway.report.CsvReportService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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
    private final CsvReportService csvReportService;

    public AuditController(AuditService auditService, CsvReportService csvReportService) {
        this.auditService = auditService;
        this.csvReportService = csvReportService;
    }

    @GetMapping
    public ApiResponse<List<AuditEventResponse>> auditEvents(
            @RequestParam(required = false) String eventType,
            @RequestParam(required = false) UUID operatorId,
            @RequestParam(defaultValue = "50") int limit
    ) {
        return ApiResponse.ok(auditService.recentEvents(eventType, operatorId, limit));
    }
    @GetMapping("/report.csv")
    public ResponseEntity<String> auditEventsReport(
            @RequestParam(required = false) String eventType,
            @RequestParam(required = false) UUID operatorId,
            @RequestParam(defaultValue = "100") int limit
    ) {
        String csv = csvReportService.auditEvents(auditService.recentEvents(eventType, operatorId, limit));
        return csvResponse("audit-events.csv", csv);
    }

    private static ResponseEntity<String> csvResponse(String filename, String csv) {
        return ResponseEntity.ok()
                .contentType(new MediaType("text", "csv"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(csv);
    }
}
