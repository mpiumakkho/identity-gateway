package com.identitygateway.auth;

import org.springframework.stereotype.Service;

@Service
public class PasswordPolicyService {

    private final AuthHardeningProperties properties;

    public PasswordPolicyService(AuthHardeningProperties properties) {
        this.properties = properties;
    }

    public void validate(String password) {
        AuthHardeningProperties.PasswordPolicy policy = properties.getPassword();
        if (password == null || password.length() < policy.getMinLength() || password.length() > policy.getMaxLength()) {
            throw new IllegalArgumentException("Password does not meet the configured password policy.");
        }
        if (policy.isRequireUppercase() && password.chars().noneMatch(Character::isUpperCase)) {
            throw new IllegalArgumentException("Password must include an uppercase letter.");
        }
        if (policy.isRequireLowercase() && password.chars().noneMatch(Character::isLowerCase)) {
            throw new IllegalArgumentException("Password must include a lowercase letter.");
        }
        if (policy.isRequireDigit() && password.chars().noneMatch(Character::isDigit)) {
            throw new IllegalArgumentException("Password must include a digit.");
        }
        if (policy.isRequireSpecial() && password.chars().noneMatch(ch -> !Character.isLetterOrDigit(ch))) {
            throw new IllegalArgumentException("Password must include a special character.");
        }
    }
}
