package com.identitygateway.config;

import com.identitygateway.auth.OperatorSessionRepository;
import com.identitygateway.auth.OperatorUserRepository;
import com.identitygateway.verification.VerificationMethod;
import com.identitygateway.verification.VerificationSessionRepository;
import com.identitygateway.verification.VerificationStatus;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;

class BusinessMetricsConfigurationTest {

    private final VerificationSessionRepository verificationSessionRepository = mock(VerificationSessionRepository.class);
    private final OperatorSessionRepository operatorSessionRepository = mock(OperatorSessionRepository.class);
    private final OperatorUserRepository operatorUserRepository = mock(OperatorUserRepository.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-07-25T00:00:00Z"), ZoneOffset.UTC);

    @Test
    void registersBusinessGauges() {
        when(verificationSessionRepository.countByStatus(VerificationStatus.CREATED)).thenReturn(7L);
        when(verificationSessionRepository.countByMethod(VerificationMethod.DIP_CHIP)).thenReturn(3L);
        when(operatorSessionRepository.countActiveSessions(clock.instant())).thenReturn(2L);
        when(operatorUserRepository.countByEnabled(true)).thenReturn(5L);
        when(operatorUserRepository.countByEnabled(false)).thenReturn(1L);
        when(operatorUserRepository.countLockedOperators(clock.instant())).thenReturn(4L);
        SimpleMeterRegistry registry = new SimpleMeterRegistry();

        new BusinessMetricsConfiguration()
                .identityGatewayMetrics(verificationSessionRepository, operatorSessionRepository, operatorUserRepository, clock)
                .bindTo(registry);

        assertThat(registry.get("identity_gateway_verification_sessions").tag("status", "CREATED").gauge().value()).isEqualTo(7);
        assertThat(registry.get("identity_gateway_verification_sessions_by_method").tag("method", "DIP_CHIP").gauge().value()).isEqualTo(3);
        assertThat(registry.get("identity_gateway_operator_sessions_active").gauge().value()).isEqualTo(2);
        assertThat(registry.get("identity_gateway_operators_enabled").gauge().value()).isEqualTo(5);
        assertThat(registry.get("identity_gateway_operators_disabled").gauge().value()).isEqualTo(1);
        assertThat(registry.get("identity_gateway_operators_locked").gauge().value()).isEqualTo(4);
    }
}