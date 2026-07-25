package com.identitygateway.verification;

import java.util.List;

public record VerificationDashboardResponse(
        long totalTransactions,
        List<VerificationMetricCount> byStatus,
        List<VerificationMetricCount> byMethod
) {
}
