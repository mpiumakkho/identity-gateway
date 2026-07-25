package com.identitygateway.verification;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CloseVerificationRequest(
        @NotBlank
        @Size(max = 40)
        String decision,

        @Size(max = 1000)
        String notes
) {
}