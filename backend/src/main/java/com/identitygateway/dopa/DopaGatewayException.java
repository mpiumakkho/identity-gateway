package com.identitygateway.dopa;

public class DopaGatewayException extends RuntimeException {

    public DopaGatewayException(String message) {
        super(message);
    }

    public DopaGatewayException(String message, Throwable cause) {
        super(message, cause);
    }
}
