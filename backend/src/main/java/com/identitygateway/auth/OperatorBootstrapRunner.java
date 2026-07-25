package com.identitygateway.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class OperatorBootstrapRunner implements ApplicationRunner {

    private final OperatorUserRepository operatorUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final boolean enabled;
    private final String username;
    private final String password;
    private final String displayName;
    private final OperatorRole role;

    public OperatorBootstrapRunner(
            OperatorUserRepository operatorUserRepository,
            PasswordEncoder passwordEncoder,
            @Value("${app.bootstrap.operator.enabled:false}") boolean enabled,
            @Value("${app.bootstrap.operator.username:}") String username,
            @Value("${app.bootstrap.operator.password:}") String password,
            @Value("${app.bootstrap.operator.display-name:Operations User}") String displayName,
            @Value("${app.bootstrap.operator.role:OPERATIONS}") OperatorRole role
    ) {
        this.operatorUserRepository = operatorUserRepository;
        this.passwordEncoder = passwordEncoder;
        this.enabled = enabled;
        this.username = username;
        this.password = password;
        this.displayName = displayName;
        this.role = role;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!enabled) {
            return;
        }

        String normalizedUsername = requireText(username, "Bootstrap operator username is required.");
        String rawPassword = requireText(password, "Bootstrap operator password is required.");
        String normalizedDisplayName = requireText(displayName, "Bootstrap operator display name is required.");

        if (operatorUserRepository.existsByUsernameIgnoreCase(normalizedUsername)) {
            return;
        }

        OperatorUser user = OperatorUser.create(
                normalizedUsername,
                passwordEncoder.encode(rawPassword),
                normalizedDisplayName,
                role
        );
        operatorUserRepository.save(user);
    }

    private static String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(message);
        }
        return value.trim();
    }
}