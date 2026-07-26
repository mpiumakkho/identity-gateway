package com.identitygateway.auth;

import com.identitygateway.audit.AuditService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OperatorManagementServiceTest {

    @Mock
    private OperatorUserRepository operatorUserRepository;

    @Mock
    private OperatorSessionService operatorSessionService;

    @Mock
    private AuditService auditService;

    private final Clock clock = Clock.fixed(Instant.parse("2026-07-25T00:00:00Z"), ZoneOffset.UTC);
    private PasswordEncoder passwordEncoder;
    private OperatorManagementService operatorManagementService;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder();
        operatorManagementService = new OperatorManagementService(
                operatorUserRepository,
                passwordEncoder,
                operatorSessionService,
                auditService,
                clock
        );
    }

    @Test
    void operatorsReturnsUsersWithoutPasswordHash() {
        OperatorUser user = operator("operator", OperatorRole.OPERATIONS);
        when(operatorUserRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(user));

        List<OperatorUserResponse> responses = operatorManagementService.operators();

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).username()).isEqualTo("operator");
        assertThat(responses.get(0).role()).isEqualTo(OperatorRole.OPERATIONS);
        assertThat(responses.get(0).enabled()).isTrue();
    }

    @Test
    void createOperatorHashesPasswordAndPersistsEnabledUser() {
        AuthenticatedOperator admin = admin();
        when(operatorUserRepository.existsByUsernameIgnoreCase("new.operator")).thenReturn(false);
        when(operatorUserRepository.save(any(OperatorUser.class))).thenAnswer(invocation -> {
            OperatorUser user = invocation.getArgument(0);
            user.prePersist();
            return user;
        });

        OperatorUserResponse response = operatorManagementService.createOperator(
                admin,
                new CreateOperatorRequest("new.operator", "very-secret-123", "New Operator", "OPERATIONS")
        );

        assertThat(response.operatorId()).isNotNull();
        assertThat(response.username()).isEqualTo("new.operator");
        assertThat(response.displayName()).isEqualTo("New Operator");
        assertThat(response.role()).isEqualTo(OperatorRole.OPERATIONS);
        assertThat(response.enabled()).isTrue();
        verify(operatorUserRepository).save(any(OperatorUser.class));
    }

    @Test
    void createOperatorRejectsDuplicateUsername() {
        when(operatorUserRepository.existsByUsernameIgnoreCase("operator")).thenReturn(true);

        assertThatThrownBy(() -> operatorManagementService.createOperator(
                admin(),
                new CreateOperatorRequest("operator", "very-secret-123", "Operator", "OPERATIONS")
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Operator username already exists.");
    }

    @Test
    void changePasswordUpdatesHashAndRevokesActiveSessions() {
        OperatorUser user = operator("operator", OperatorRole.OPERATIONS);
        String oldHash = user.getPasswordHash();
        when(operatorUserRepository.findById(user.getId())).thenReturn(Optional.of(user));

        OperatorUserResponse response = operatorManagementService.changePassword(
                admin(),
                user.getId(),
                new ChangeOperatorPasswordRequest("new-secret-123")
        );

        assertThat(response.operatorId()).isEqualTo(user.getId());
        assertThat(user.getPasswordHash()).isNotEqualTo(oldHash);
        assertThat(passwordEncoder.matches("new-secret-123", user.getPasswordHash())).isTrue();
        verify(operatorSessionService).revokeActiveSessions(user);
    }

    @Test
    void disableOperatorRejectsSelfDisable() {
        AuthenticatedOperator admin = admin();

        assertThatThrownBy(() -> operatorManagementService.disableOperator(admin, admin.operatorId()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Admin operators cannot disable their own account.");

        verify(operatorSessionService, never()).revokeActiveSessions(any());
    }

    @Test
    void disableOperatorDisablesUserAndRevokesSessions() {
        OperatorUser user = operator("operator", OperatorRole.OPERATIONS);
        when(operatorUserRepository.findById(user.getId())).thenReturn(Optional.of(user));

        OperatorUserResponse response = operatorManagementService.disableOperator(admin(), user.getId());

        assertThat(response.enabled()).isFalse();
        assertThat(response.disabledAt()).isEqualTo(clock.instant());
        verify(operatorSessionService).revokeActiveSessions(user);
    }

    private static OperatorUser operator(String username, OperatorRole role) {
        OperatorUser user = OperatorUser.create(username, "hash", "Operations User", role);
        user.prePersist();
        return user;
    }

    private static AuthenticatedOperator admin() {
        return new AuthenticatedOperator(
                UUID.fromString("9e04e2eb-d74a-4d55-987c-f38660aa3060"),
                "admin",
                "Admin User",
                OperatorRole.ADMIN,
                OperatorRole.ADMIN.permissions(),
                Instant.parse("2026-07-25T08:00:00Z")
        );
    }
}