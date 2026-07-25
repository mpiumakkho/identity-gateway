package com.identitygateway.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangeOperatorPasswordRequest(
        @NotBlank
        @Size(min = 12, max = 128)
        String password
) {
}