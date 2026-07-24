package com.identitygateway.common.api;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ApiResponseTest {

    @Test
    void okWrapsDataWithSuccessMetadata() {
        ApiResponse<Map<String, String>> response = ApiResponse.ok(Map.of("service", "identity-gateway"));

        assertThat(response.status()).isEqualTo("success");
        assertThat(response.code()).isEqualTo("OK");
        assertThat(response.message()).isEmpty();
        assertThat(response.data()).containsEntry("service", "identity-gateway");
        assertThat(response.timestamp()).isNotNull();
    }

    @Test
    void errorWrapsMessageWithoutData() {
        ApiResponse<Object> response = ApiResponse.error("VALIDATION_ERROR", "method is required");

        assertThat(response.status()).isEqualTo("error");
        assertThat(response.code()).isEqualTo("VALIDATION_ERROR");
        assertThat(response.message()).isEqualTo("method is required");
        assertThat(response.data()).isNull();
        assertThat(response.timestamp()).isNotNull();
    }
}