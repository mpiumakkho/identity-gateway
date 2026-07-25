package com.identitygateway.verification;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VerificationServiceTest {

    @Mock
    private VerificationSessionRepository verificationSessionRepository;

    private VerificationService verificationService;

    @BeforeEach
    void setUp() {
        verificationService = new VerificationService(verificationSessionRepository);
    }

    @Test
    void methodsReturnsConfiguredFlowEntryPoints() {
        List<VerificationMethodResponse> methods = verificationService.methods();

        assertThat(methods)
                .extracting(VerificationMethodResponse::id)
                .containsExactly("DIP_CHIP", "MANUAL_ENTRY");
    }

    @Test
    void startSessionPersistsCreatedSession() {
        when(verificationSessionRepository.save(any(VerificationSessionEntity.class)))
                .thenAnswer(invocation -> {
                    VerificationSessionEntity entity = invocation.getArgument(0);
                    entity.prePersist();
                    return entity;
                });

        VerificationSessionResponse response = verificationService.startSession(new StartVerificationRequest("DIP_CHIP"));

        assertThat(response.transactionId()).isNotNull();
        assertThat(response.method()).isEqualTo("DIP_CHIP");
        assertThat(response.status()).isEqualTo("CREATED");
        assertThat(response.createdAt()).isNotNull();
    }

    @Test
    void startSessionRejectsUnsupportedMethod() {
        assertThatThrownBy(() -> verificationService.startSession(new StartVerificationRequest("VIDEO_CALL")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Unsupported verification method: VIDEO_CALL");
    }
}