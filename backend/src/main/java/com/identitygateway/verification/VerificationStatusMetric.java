package com.identitygateway.verification;

interface VerificationStatusMetric {

    VerificationStatus getStatus();

    long getTotal();
}
