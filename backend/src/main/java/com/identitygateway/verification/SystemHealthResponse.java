package com.identitygateway.verification;

public record SystemHealthResponse(
        String service,
        String status
) {
}