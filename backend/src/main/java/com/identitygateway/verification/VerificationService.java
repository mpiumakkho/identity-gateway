package com.identitygateway.verification;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

@Service
public class VerificationService {

    private final VerificationSessionRepository verificationSessionRepository;

    public VerificationService(VerificationSessionRepository verificationSessionRepository) {
        this.verificationSessionRepository = verificationSessionRepository;
    }

    @Transactional(readOnly = true)
    public List<VerificationMethodResponse> methods() {
        return Arrays.stream(VerificationMethod.values())
                .map(method -> new VerificationMethodResponse(method.name(), method.label(), method.description(), true))
                .toList();
    }

    @Transactional
    public VerificationSessionResponse startSession(StartVerificationRequest request) {
        VerificationMethod method = VerificationMethod.from(request.method());
        VerificationSessionEntity session = verificationSessionRepository.save(VerificationSessionEntity.create(method));
        return toResponse(session);
    }

    private VerificationSessionResponse toResponse(VerificationSessionEntity session) {
        return new VerificationSessionResponse(
                session.getId(),
                session.getMethod().name(),
                session.getStatus().name(),
                session.getCreatedAt()
        );
    }
}