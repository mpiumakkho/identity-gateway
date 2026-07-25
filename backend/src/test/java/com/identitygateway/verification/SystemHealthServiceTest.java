package com.identitygateway.verification;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SystemHealthServiceTest {

    private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-07-25T03:00:00Z"), ZoneOffset.UTC);

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Test
    void healthReturnsUpWhenDatabaseResponds() {
        when(jdbcTemplate.queryForObject("SELECT 1", Integer.class)).thenReturn(1);

        SystemHealthResponse response = new SystemHealthService(jdbcTemplate, FIXED_CLOCK).health();

        assertThat(response.service()).isEqualTo("identity-gateway");
        assertThat(response.status()).isEqualTo("UP");
        assertThat(response.databaseStatus()).isEqualTo("UP");
        assertThat(response.checkedAt()).isEqualTo(Instant.parse("2026-07-25T03:00:00Z"));
    }

    @Test
    void healthReturnsDownWhenDatabaseCheckFails() {
        when(jdbcTemplate.queryForObject("SELECT 1", Integer.class)).thenThrow(new DataAccessResourceFailureException("unavailable"));

        SystemHealthResponse response = new SystemHealthService(jdbcTemplate, FIXED_CLOCK).health();

        assertThat(response.status()).isEqualTo("DOWN");
        assertThat(response.databaseStatus()).isEqualTo("DOWN");
    }
}
