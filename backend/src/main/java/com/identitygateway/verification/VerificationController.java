package com.identitygateway.verification;

import com.identitygateway.auth.AuthenticatedOperator;
import com.identitygateway.common.api.ApiResponse;
import com.identitygateway.audit.AuditEventResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/verification")
public class VerificationController {

    private final VerificationService verificationService;

    public VerificationController(VerificationService verificationService) {
        this.verificationService = verificationService;
    }

    @GetMapping("/methods")
    public ApiResponse<List<VerificationMethodResponse>> methods() {
        return ApiResponse.ok(verificationService.methods());
    }

    @GetMapping("/sessions")
    public ApiResponse<List<VerificationSessionResponse>> sessions() {
        return ApiResponse.ok(verificationService.recentSessions());
    }

    @GetMapping("/sessions/{transactionId}")
    public ApiResponse<VerificationSessionResponse> session(@PathVariable UUID transactionId) {
        return ApiResponse.ok(verificationService.session(transactionId));
    }

    @GetMapping("/sessions/{transactionId}/audit-events")
    public ApiResponse<List<AuditEventResponse>> auditEvents(@PathVariable UUID transactionId) {
        return ApiResponse.ok(verificationService.auditEvents(transactionId));
    }

    @PostMapping("/sessions")
    public ResponseEntity<ApiResponse<VerificationSessionResponse>> startSession(
            @AuthenticationPrincipal AuthenticatedOperator operator,
            @Valid @RequestBody StartVerificationRequest request
    ) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.ok(verificationService.startSession(operator, request)));
    }

    @PutMapping("/sessions/{transactionId}/manual-identity")
    public ApiResponse<ManualIdentityResponse> saveManualIdentity(
            @AuthenticationPrincipal AuthenticatedOperator operator,
            @PathVariable UUID transactionId,
            @Valid @RequestBody ManualIdentityRequest request
    ) {
        return ApiResponse.ok(verificationService.saveManualIdentity(operator, transactionId, request));
    }

    @PutMapping("/sessions/{transactionId}/dip-chip-payload")
    public ApiResponse<DipChipPayloadResponse> saveDipChipPayload(
            @AuthenticationPrincipal AuthenticatedOperator operator,
            @PathVariable UUID transactionId,
            @Valid @RequestBody DipChipPayloadRequest request
    ) {
        return ApiResponse.ok(verificationService.saveDipChipPayload(operator, transactionId, request));
    }

    @PostMapping("/sessions/{transactionId}/closeout")
    public ApiResponse<VerificationCloseoutResponse> closeSession(
            @AuthenticationPrincipal AuthenticatedOperator operator,
            @PathVariable UUID transactionId,
            @Valid @RequestBody CloseVerificationRequest request
    ) {
        return ApiResponse.ok(verificationService.closeSession(operator, transactionId, request));
    }
}
