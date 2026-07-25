package com.identitygateway.auth;

import com.identitygateway.common.api.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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

    @PostMapping("/logout")
    public ApiResponse<LogoutResponse> logout(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorizationHeader) {
        bearerTokenResolver.resolve(authorizationHeader).ifPresent(authService::logout);
        return ApiResponse.ok(new LogoutResponse(true));
    }
}