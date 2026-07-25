package com.identitygateway.dopa;

public interface DopaGatewayClient {

    DopaGatewayResult validate(DopaGatewayRequest request);
}