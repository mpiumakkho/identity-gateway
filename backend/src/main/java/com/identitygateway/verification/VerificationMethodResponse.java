package com.identitygateway.verification;

public record VerificationMethodResponse(
        String id,
        String label,
        String description,
        boolean enabled
) {
}