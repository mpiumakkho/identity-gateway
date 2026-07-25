package com.identitygateway.verification;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;

@Service
public class SystemHealthService {

    private static final String SERVICE_NAME = "identity-gateway";
    private static final String STATUS_UP = "UP";
    private static final String STATUS_DOWN = "DOWN";

    private final JdbcTemplate jdbcTemplate;
    private final Clock clock;

    public SystemHealthService(JdbcTemplate jdbcTemplate, Clock clock) {
        this.jdbcTemplate = jdbcTemplate;
        this.clock = clock;
    }

    public SystemHealthResponse health() {
        String databaseStatus = databaseStatus();
        String serviceStatus = STATUS_UP.equals(databaseStatus) ? STATUS_UP : STATUS_DOWN;

        return new SystemHealthResponse(
                SERVICE_NAME,
                serviceStatus,
                databaseStatus,
                Instant.now(clock)
        );
    }

    private String databaseStatus() {
        try {
            Integer result = jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            return Integer.valueOf(1).equals(result) ? STATUS_UP : STATUS_DOWN;
        } catch (RuntimeException ex) {
            return STATUS_DOWN;
        }
    }
}
