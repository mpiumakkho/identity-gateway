package com.identitygateway.dopa;

public class LocalDopaGatewayClient implements DopaGatewayClient {

    @Override
    public DopaGatewayResult validate(DopaGatewayRequest request) {
        DopaIdentitySnapshot identity = request.identity();
        boolean matched = !identity.nationalId().endsWith("0000")
                && !identity.laserCode().toUpperCase().contains("FAIL");

        if (matched) {
            return new DopaGatewayResult(
                    DopaValidationResultStatus.MATCHED,
                    "DOPA-0000",
                    "Citizen identity matched."
            );
        }

        return new DopaGatewayResult(
                DopaValidationResultStatus.NOT_MATCHED,
                "DOPA-4001",
                "Citizen identity did not match."
        );
    }
}
