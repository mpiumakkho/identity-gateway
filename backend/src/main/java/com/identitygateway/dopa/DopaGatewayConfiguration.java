package com.identitygateway.dopa;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DopaGatewayConfiguration {

    @Bean
    @ConditionalOnProperty(prefix = "app.dopa", name = "mode", havingValue = "local", matchIfMissing = true)
    DopaGatewayClient localDopaGatewayClient() {
        return new LocalDopaGatewayClient();
    }

    @Bean
    @ConditionalOnProperty(prefix = "app.dopa", name = "mode", havingValue = "partner")
    DopaGatewayClient partnerDopaGatewayClient(DopaIntegrationProperties properties) {
        return new PartnerDopaGatewayClient(properties);
    }
}
