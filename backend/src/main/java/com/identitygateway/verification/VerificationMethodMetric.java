package com.identitygateway.verification;

interface VerificationMethodMetric {

    VerificationMethod getMethod();

    long getTotal();
}
