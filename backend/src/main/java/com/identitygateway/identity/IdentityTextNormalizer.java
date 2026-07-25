package com.identitygateway.identity;

import java.util.Locale;

public final class IdentityTextNormalizer {

    private IdentityTextNormalizer() {
    }

    public static String clean(String value) {
        return value.trim().replaceAll("\\s+", " ");
    }

    public static String upperClean(String value) {
        return clean(value).toUpperCase(Locale.ROOT);
    }
}