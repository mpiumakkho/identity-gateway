package com.identitygateway.dopa;

public record DopaGatewayResult(
        DopaValidationResultStatus status,
        String responseCode,
        String responseMessage
) {

    public boolean matched() {
        return status == DopaValidationResultStatus.MATCHED;
    }

    public boolean notMatched() {
        return status == DopaValidationResultStatus.NOT_MATCHED;
    }

    public static DopaGatewayResult error(String responseCode, String responseMessage) {
        return new DopaGatewayResult(DopaValidationResultStatus.ERROR, responseCode, responseMessage);
    }
}
