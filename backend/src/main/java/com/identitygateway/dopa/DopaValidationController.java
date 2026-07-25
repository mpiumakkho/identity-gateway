package com.identitygateway.dopa;

import com.identitygateway.common.api.ApiResponse;
import com.identitygateway.auth.AuthenticatedOperator;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/verification/sessions/{transactionId}/dopa-validation")
public class DopaValidationController {

    private final DopaValidationService dopaValidationService;

    public DopaValidationController(DopaValidationService dopaValidationService) {
        this.dopaValidationService = dopaValidationService;
    }

    @PostMapping
    public ApiResponse<DopaValidationResponse> validate(
            @AuthenticationPrincipal AuthenticatedOperator operator,
            @PathVariable UUID transactionId,
            @Valid @RequestBody DopaValidationRequest request
    ) {
        return ApiResponse.ok(dopaValidationService.validate(operator, transactionId, request));
    }
}
