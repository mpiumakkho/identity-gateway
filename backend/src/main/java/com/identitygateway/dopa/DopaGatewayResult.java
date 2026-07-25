package com.identitygateway.dopa;

public record DopaGatewayResult(
        DopaValidationResultStatus status,
        String responseCode,
        String responseMessage
) {

    public boolean matched() {
        return status == DopaValidationResultStatus.MATCHED;
    }
}