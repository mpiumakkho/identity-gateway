package com.identitygateway.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateOperatorRequest(
        @NotBlank
        @Size(max = 80)
        @Pattern(regexp = "[A-Za-z0-9._-]{3,80}", message = "must be 3-80 characters and contain only letters, numbers, dots, underscores, or hyphens")
        String username,

        @NotBlank
        @Size(min = 12, max = 128)
        String password,

        @NotBlank
        @Size(max = 120)
        String displayName,

        @NotBlank
        String role
) {
}