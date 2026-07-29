package com.identitygateway.config;

import com.identitygateway.auth.OperatorSessionRepository;
import com.identitygateway.auth.OperatorUserRepository;
import com.identitygateway.verification.VerificationMethod;
import com.identitygateway.verification.VerificationSessionRepository;
import com.identitygateway.verification.VerificationStatus;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.binder.MeterBinder;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.util.function.Supplier;

@Configuration
public class BusinessMetricsConfiguration {

    @Bean
    public MeterBinder identityGatewayMetrics(
            VerificationSessionRepository verificationSessionRepository,
            OperatorSessionRepository operatorSessionRepository,
            OperatorUserRepository operatorUserRepository,
            Clock clock
    ) {
        return registry -> {
            for (VerificationStatus status : VerificationStatus.values()) {
                registerGauge(
                        registry,
                        "identity_gateway_verification_sessions",
                        "Persisted verification sessions by status.",
                        () -> verificationSessionRepository.countByStatus(status),
                        "status",
                        status.name()
                );
            }

            for (VerificationMethod method : VerificationMethod.values()) {
                registerGauge(
                        registry,
                        "identity_gateway_verification_sessions_by_method",
                        "Persisted verification sessions by intake method.",
                        () -> verificationSessionRepository.countByMethod(method),
                        "method",
                        method.name()
                );
            }

            registerGauge(
                    registry,
                    "identity_gateway_operator_sessions_active",
                    "Active operator bearer-token sessions.",
                    () -> operatorSessionRepository.countActiveSessions(clock.instant())
            );
            registerGauge(
                    registry,
                    "identity_gateway_operators_enabled",
                    "Enabled operator accounts.",
                    () -> operatorUserRepository.countByEnabled(true)
            );
            registerGauge(
                    registry,
                    "identity_gateway_operators_disabled",
                    "Disabled operator accounts.",
                    () -> operatorUserRepository.countByEnabled(false)
            );
            registerGauge(
                    registry,
                    "identity_gateway_operators_locked",
                    "Currently locked operator accounts.",
                    () -> operatorUserRepository.countLockedOperators(clock.instant())
            );
        };
    }

    private static void registerGauge(MeterRegistry registry, String name, String description, Supplier<Number> supplier, String... tags) {
        Gauge.builder(name, () -> safeNumber(supplier))
                .description(description)
                .tags(tags)
                .register(registry);
    }

    private static Number safeNumber(Supplier<Number> supplier) {
        try {
            return supplier.get();
        } catch (RuntimeException ex) {
            return 0;
        }
    }
}