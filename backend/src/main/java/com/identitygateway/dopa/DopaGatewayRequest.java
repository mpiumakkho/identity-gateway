package com.identitygateway.dopa;

public record DopaGatewayRequest(
        DopaIdentitySnapshot identity,
        String consentReference
) {
}