package com.identitygateway.verification;

import jakarta.validation.constraints.NotNull;

public record UpdateVerificationMethodStatusRequest(
        @NotNull Boolean enabled
) {
}