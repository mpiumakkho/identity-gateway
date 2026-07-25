package com.identitygateway.verification;

public enum VerificationMethod {
    DIP_CHIP("Dip Chip", "Read citizen card data from a supported reader."),
    MANUAL_ENTRY("Manual Entry", "Capture citizen data through a controlled form.");

    private final String label;
    private final String description;

    VerificationMethod(String label, String description) {
        this.label = label;
        this.description = description;
    }

    public String label() {
        return label;
    }

    public String description() {
        return description;
    }

    public static VerificationMethod from(String value) {
        try {
            return VerificationMethod.valueOf(value);
        } catch (RuntimeException ex) {
            throw new IllegalArgumentException("Unsupported verification method: " + value);
        }
    }
}