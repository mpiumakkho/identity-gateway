package com.identitygateway.auth;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class OperatorSessionCleanupJob {

    private final OperatorSessionService operatorSessionService;
    private final AuthHardeningProperties properties;

    public OperatorSessionCleanupJob(OperatorSessionService operatorSessionService, AuthHardeningProperties properties) {
        this.operatorSessionService = operatorSessionService;
        this.properties = properties;
    }

    @Scheduled(fixedDelayString = "${app.auth.hardening.session-cleanup.fixed-delay:PT1H}")
    public void cleanupSessions() {
        if (properties.getSessionCleanup().isEnabled()) {
            operatorSessionService.cleanupExpiredAndRevokedSessions();
        }
    }
}
