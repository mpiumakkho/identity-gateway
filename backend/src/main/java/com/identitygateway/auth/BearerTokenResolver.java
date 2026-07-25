package com.identitygateway.auth;

import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class BearerTokenResolver {

    private static final String BEARER_PREFIX = "Bearer ";

    public Optional<String> resolve(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith(BEARER_PREFIX)) {
            return Optional.empty();
        }

        String token = authorizationHeader.substring(BEARER_PREFIX.length()).trim();
        return token.isEmpty() ? Optional.empty() : Optional.of(token);
    }
}