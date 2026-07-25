package com.identitygateway.audit;

import com.identitygateway.auth.OperatorRole;
import com.identitygateway.auth.OperatorUser;
import com.identitygateway.auth.OperatorUserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuditServiceTest {

    @Mock
    private AuditEventRepository auditEventRepository;

    @Mock
    private OperatorUserRepository operatorUserRepository;

    @Test
    void recentEventsFiltersByEventTypeAndCapsLimit() {
        AuditService auditService = new AuditService(auditEventRepository, operatorUserRepository);
        UUID operatorId = UUID.fromString("9e04e2eb-d74a-4d55-987c-f38660aa3060");
        OperatorUser operator = OperatorUser.create("operator", "hash", "Operations User", OperatorRole.OPERATIONS);
        AuditEventEntity event = AuditEventEntity.create(AuditEventType.AUTH_LOGIN_SUCCEEDED, operator, null, "Operator login succeeded.", null);
        event.prePersist();

        when(auditEventRepository.searchRecent(eq(AuditEventType.AUTH_LOGIN_SUCCEEDED), eq(operatorId), any(Pageable.class))).thenReturn(List.of(event));

        List<AuditEventResponse> responses = auditService.recentEvents("AUTH_LOGIN_SUCCEEDED", operatorId, 500);

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).eventType()).isEqualTo("AUTH_LOGIN_SUCCEEDED");
        verify(auditEventRepository).searchRecent(eq(AuditEventType.AUTH_LOGIN_SUCCEEDED), eq(operatorId), argThat(pageable -> pageable.getPageSize() == 100));
    }

    @Test
    void recentEventsRejectsUnsupportedEventType() {
        AuditService auditService = new AuditService(auditEventRepository, operatorUserRepository);

        assertThatThrownBy(() -> auditService.recentEvents("NOPE", null, 50))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Unsupported audit event type: NOPE");
    }
}
