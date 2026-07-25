package com.identitygateway.dopa;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DopaValidationRequest(
        @NotBlank
        @Size(max = 80)
        String consentReference
) {
}