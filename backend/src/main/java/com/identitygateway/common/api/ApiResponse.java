package com.identitygateway.common.api;

import java.time.Instant;

public record ApiResponse<T>(
        String status,
        String code,
        String message,
        T data,
        Instant timestamp
) {
    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>("success", "OK", "", data, Instant.now());
    }

    public static <T> ApiResponse<T> error(String code, String message) {
        return new ApiResponse<>("error", code, message, null, Instant.now());
    }
}