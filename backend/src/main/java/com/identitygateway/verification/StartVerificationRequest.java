package com.identitygateway.verification;

import jakarta.validation.constraints.NotBlank;

public record StartVerificationRequest(
        @NotBlank String method
) {
}