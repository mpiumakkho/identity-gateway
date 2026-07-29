package com.identitygateway.auth;

import com.identitygateway.common.api.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/operators")
public class OperatorManagementController {

    private final OperatorManagementService operatorManagementService;
    private final BearerTokenResolver bearerTokenResolver;

    public OperatorManagementController(
            OperatorManagementService operatorManagementService,
            BearerTokenResolver bearerTokenResolver
    ) {
        this.operatorManagementService = operatorManagementService;
        this.bearerTokenResolver = bearerTokenResolver;
    }

    @GetMapping
    public ApiResponse<List<OperatorUserResponse>> operators() {
        return ApiResponse.ok(operatorManagementService.operators());
    }

    @PostMapping
    public ApiResponse<OperatorUserResponse> createOperator(
            @AuthenticationPrincipal AuthenticatedOperator admin,
            @Valid @RequestBody CreateOperatorRequest request
    ) {
        return ApiResponse.ok(operatorManagementService.createOperator(admin, request));
    }

    @PutMapping("/{operatorId}/password")
    public ApiResponse<OperatorUserResponse> changePassword(
            @AuthenticationPrincipal AuthenticatedOperator admin,
            @PathVariable UUID operatorId,
            @Valid @RequestBody ChangeOperatorPasswordRequest request
    ) {
        return ApiResponse.ok(operatorManagementService.changePassword(admin, operatorId, request));
    }

    @PutMapping("/{operatorId}/disabled")
    public ApiResponse<OperatorUserResponse> disableOperator(
            @AuthenticationPrincipal AuthenticatedOperator admin,
            @PathVariable UUID operatorId
    ) {
        return ApiResponse.ok(operatorManagementService.disableOperator(admin, operatorId));
    }

    @GetMapping("/{operatorId}/sessions")
    public ApiResponse<List<OperatorSessionResponse>> operatorSessions(
            @PathVariable UUID operatorId,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorizationHeader
    ) {
        return ApiResponse.ok(operatorManagementService.operatorSessions(operatorId, resolveAccessToken(authorizationHeader)));
    }

    @DeleteMapping("/{operatorId}/sessions/{sessionId}")
    public ApiResponse<SessionRevocationSummaryResponse> revokeOperatorSession(
            @AuthenticationPrincipal AuthenticatedOperator admin,
            @PathVariable UUID operatorId,
            @PathVariable UUID sessionId,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorizationHeader
    ) {
        return ApiResponse.ok(operatorManagementService.revokeOperatorSession(admin, operatorId, sessionId, resolveAccessToken(authorizationHeader)));
    }

    @DeleteMapping("/{operatorId}/sessions")
    public ApiResponse<SessionRevocationSummaryResponse> revokeOperatorSessions(
            @AuthenticationPrincipal AuthenticatedOperator admin,
            @PathVariable UUID operatorId,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorizationHeader
    ) {
        return ApiResponse.ok(operatorManagementService.revokeOperatorSessions(admin, operatorId, resolveAccessToken(authorizationHeader)));
    }

    private String resolveAccessToken(String authorizationHeader) {
        return bearerTokenResolver.resolve(authorizationHeader).orElseThrow(com.identitygateway.common.error.AuthenticationFailedException::new);
    }
}