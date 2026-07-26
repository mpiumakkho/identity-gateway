package com.identitygateway.auth;

import com.identitygateway.common.api.ApiResponse;
import com.identitygateway.common.error.AuthenticationFailedException;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final BearerTokenResolver bearerTokenResolver;

    public AuthController(AuthService authService, BearerTokenResolver bearerTokenResolver) {
        this.authService = authService;
        this.bearerTokenResolver = bearerTokenResolver;
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(authService.login(request)));
    }

    @GetMapping("/me")
    public ApiResponse<CurrentOperatorResponse> me(@AuthenticationPrincipal AuthenticatedOperator operator) {
        return ApiResponse.ok(authService.currentOperator(operator));
    }

    @GetMapping("/sessions")
    public ApiResponse<List<OperatorSessionResponse>> sessions(
            @AuthenticationPrincipal AuthenticatedOperator operator,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorizationHeader
    ) {
        return ApiResponse.ok(authService.activeSessions(operator, resolveAccessToken(authorizationHeader)));
    }

    @DeleteMapping("/sessions/{sessionId}")
    public ApiResponse<SessionRevocationResponse> revokeSession(
            @AuthenticationPrincipal AuthenticatedOperator operator,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorizationHeader,
            @PathVariable UUID sessionId
    ) {
        return ApiResponse.ok(authService.revokeSession(operator, sessionId, resolveAccessToken(authorizationHeader)));
    }

    @PutMapping("/password")
    public ApiResponse<PasswordChangeResponse> changePassword(
            @AuthenticationPrincipal AuthenticatedOperator operator,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorizationHeader,
            @Valid @RequestBody ChangeOwnPasswordRequest request
    ) {
        return ApiResponse.ok(authService.changeOwnPassword(operator, resolveAccessToken(authorizationHeader), request));
    }

    @PostMapping("/logout")
    public ApiResponse<LogoutResponse> logout(
            @AuthenticationPrincipal AuthenticatedOperator operator,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorizationHeader
    ) {
        bearerTokenResolver.resolve(authorizationHeader).ifPresent(accessToken -> authService.logout(operator, accessToken));
        return ApiResponse.ok(new LogoutResponse(true));
    }

    private String resolveAccessToken(String authorizationHeader) {
        return bearerTokenResolver.resolve(authorizationHeader).orElseThrow(AuthenticationFailedException::new);
    }
}
