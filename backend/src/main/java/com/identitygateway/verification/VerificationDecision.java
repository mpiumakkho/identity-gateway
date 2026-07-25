package com.identitygateway.verification;

public enum VerificationDecision {
    APPROVED,
    REJECTED;

    static VerificationDecision from(String value) {
        try {
            return VerificationDecision.valueOf(value.trim().toUpperCase());
        } catch (RuntimeException ex) {
            throw new IllegalArgumentException("Unsupported verification decision: " + value);
        }
    }
}