package com.identitygateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class IdentityGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(IdentityGatewayApplication.class, args);
    }
}
