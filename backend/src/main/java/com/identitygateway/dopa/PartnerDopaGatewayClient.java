package com.identitygateway.dopa;

import org.springframework.util.StringUtils;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

public class PartnerDopaGatewayClient implements DopaGatewayClient {

    private static final String API_KEY_HEADER = "X-API-Key";
    private static final String ERROR_CODE = "DOPA-PARTNER-ERROR";
    private static final String ERROR_MESSAGE = "DOPA partner validation unavailable.";

    private final DopaIntegrationProperties.Partner properties;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public PartnerDopaGatewayClient(DopaIntegrationProperties integrationProperties) {
        this.properties = integrationProperties.getPartner();
        validateConfiguration(properties);
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(properties.getConnectTimeout())
                .build();
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public DopaGatewayResult validate(DopaGatewayRequest request) {
        int maxAttempts = properties.getRetryAttempts() + 1;
        DopaGatewayException lastFailure = null;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                return sendValidationRequest(request);
            } catch (DopaGatewayException ex) {
                lastFailure = ex;
                if (attempt == maxAttempts) {
                    break;
                }
            }
        }

        throw lastFailure == null ? new DopaGatewayException(ERROR_MESSAGE) : lastFailure;
    }

    private DopaGatewayResult sendValidationRequest(DopaGatewayRequest request) {
        try {
            PartnerDopaRequest partnerRequest = PartnerDopaRequest.from(request);
            String body = objectMapper.writeValueAsString(partnerRequest);
            HttpRequest httpRequest = HttpRequest.newBuilder(resolveValidationUri())
                    .timeout(readTimeout())
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .header(API_KEY_HEADER, properties.getApiKey().trim())
                    .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            return mapResponse(response);
        } catch (IOException ex) {
            throw new DopaGatewayException(ERROR_MESSAGE, ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new DopaGatewayException(ERROR_MESSAGE, ex);
        }
    }

    private DopaGatewayResult mapResponse(HttpResponse<String> response) {
        int statusCode = response.statusCode();
        if (statusCode < 200 || statusCode >= 300) {
            throw new DopaGatewayException(ERROR_MESSAGE);
        }

        try {
            PartnerDopaResponse partnerResponse = objectMapper.readValue(response.body(), PartnerDopaResponse.class);
            DopaValidationResultStatus status = mapStatus(partnerResponse.status(), partnerResponse.responseCode());
            return new DopaGatewayResult(
                    status,
                    requireText(partnerResponse.responseCode(), "DOPA-PARTNER-UNKNOWN"),
                    requireText(partnerResponse.responseMessage(), defaultMessage(status))
            );
        } catch (RuntimeException ex) {
            throw new DopaGatewayException(ERROR_MESSAGE, ex);
        }
    }

    private DopaValidationResultStatus mapStatus(String status, String responseCode) {
        if ("MATCHED".equalsIgnoreCase(status) || "DOPA-0000".equalsIgnoreCase(responseCode)) {
            return DopaValidationResultStatus.MATCHED;
        }
        if ("NOT_MATCHED".equalsIgnoreCase(status)) {
            return DopaValidationResultStatus.NOT_MATCHED;
        }
        if ("ERROR".equalsIgnoreCase(status)) {
            return DopaValidationResultStatus.ERROR;
        }
        throw new IllegalArgumentException("Unsupported DOPA partner status.");
    }

    private URI resolveValidationUri() {
        String baseUrl = properties.getBaseUrl().trim();
        String path = properties.getValidationPath().trim();
        String normalizedPath = path.startsWith("/") ? path : "/" + path;
        return URI.create(baseUrl + normalizedPath);
    }

    private Duration readTimeout() {
        return properties.getReadTimeout() == null ? Duration.ofSeconds(10) : properties.getReadTimeout();
    }

    private static void validateConfiguration(DopaIntegrationProperties.Partner properties) {
        if (!StringUtils.hasText(properties.getBaseUrl())) {
            throw new IllegalStateException("DOPA partner base URL must be configured when partner mode is enabled.");
        }
        if (!StringUtils.hasText(properties.getValidationPath())) {
            throw new IllegalStateException("DOPA partner validation path must be configured when partner mode is enabled.");
        }
        if (!StringUtils.hasText(properties.getApiKey())) {
            throw new IllegalStateException("DOPA partner API key must be configured when partner mode is enabled.");
        }
    }

    private static String requireText(String value, String fallback) {
        return StringUtils.hasText(value) ? value.trim() : fallback;
    }

    private static String defaultMessage(DopaValidationResultStatus status) {
        return switch (status) {
            case MATCHED -> "Citizen identity matched.";
            case NOT_MATCHED -> "Citizen identity did not match.";
            case ERROR -> ERROR_MESSAGE;
        };
    }

    private record PartnerDopaRequest(
            String nationalId,
            String title,
            String firstName,
            String lastName,
            String dateOfBirth,
            String laserCode,
            String identitySource,
            String consentReference
    ) {
        private static PartnerDopaRequest from(DopaGatewayRequest request) {
            DopaIdentitySnapshot identity = request.identity();
            return new PartnerDopaRequest(
                    identity.nationalId(),
                    identity.title(),
                    identity.firstName(),
                    identity.lastName(),
                    identity.dateOfBirth().toString(),
                    identity.laserCode(),
                    identity.source().name(),
                    request.consentReference()
            );
        }
    }

    private record PartnerDopaResponse(
            String status,
            String responseCode,
            String responseMessage
    ) {
    }
}

