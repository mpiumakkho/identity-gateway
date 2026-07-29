package com.identitygateway.auth;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Validated
@ConfigurationProperties(prefix = "app.auth.hardening")
public class AuthHardeningProperties {

    @Valid
    private Lockout lockout = new Lockout();

    @Valid
    private PasswordPolicy password = new PasswordPolicy();

    @Valid
    private SessionCleanup sessionCleanup = new SessionCleanup();

    public Lockout getLockout() {
        return lockout;
    }

    public void setLockout(Lockout lockout) {
        this.lockout = lockout;
    }

    public PasswordPolicy getPassword() {
        return password;
    }

    public void setPassword(PasswordPolicy password) {
        this.password = password;
    }

    public SessionCleanup getSessionCleanup() {
        return sessionCleanup;
    }

    public void setSessionCleanup(SessionCleanup sessionCleanup) {
        this.sessionCleanup = sessionCleanup;
    }

    public static class Lockout {
        private boolean enabled = true;

        @Min(1)
        private int maxFailedAttempts = 5;

        private Duration duration = Duration.ofMinutes(15);

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public int getMaxFailedAttempts() {
            return maxFailedAttempts;
        }

        public void setMaxFailedAttempts(int maxFailedAttempts) {
            this.maxFailedAttempts = maxFailedAttempts;
        }

        public Duration getDuration() {
            return duration;
        }

        public void setDuration(Duration duration) {
            this.duration = duration;
        }
    }

    public static class PasswordPolicy {
        @Min(8)
        private int minLength = 12;

        @Min(1)
        private int maxLength = 128;

        private boolean requireUppercase = true;
        private boolean requireLowercase = true;
        private boolean requireDigit = true;
        private boolean requireSpecial = false;

        public int getMinLength() {
            return minLength;
        }

        public void setMinLength(int minLength) {
            this.minLength = minLength;
        }

        public int getMaxLength() {
            return maxLength;
        }

        public void setMaxLength(int maxLength) {
            this.maxLength = maxLength;
        }

        public boolean isRequireUppercase() {
            return requireUppercase;
        }

        public void setRequireUppercase(boolean requireUppercase) {
            this.requireUppercase = requireUppercase;
        }

        public boolean isRequireLowercase() {
            return requireLowercase;
        }

        public void setRequireLowercase(boolean requireLowercase) {
            this.requireLowercase = requireLowercase;
        }

        public boolean isRequireDigit() {
            return requireDigit;
        }

        public void setRequireDigit(boolean requireDigit) {
            this.requireDigit = requireDigit;
        }

        public boolean isRequireSpecial() {
            return requireSpecial;
        }

        public void setRequireSpecial(boolean requireSpecial) {
            this.requireSpecial = requireSpecial;
        }
    }

    public static class SessionCleanup {
        private boolean enabled = true;
        private Duration retention = Duration.ofDays(30);
        private Duration fixedDelay = Duration.ofHours(1);

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public Duration getRetention() {
            return retention;
        }

        public void setRetention(Duration retention) {
            this.retention = retention;
        }

        public Duration getFixedDelay() {
            return fixedDelay;
        }

        public void setFixedDelay(Duration fixedDelay) {
            this.fixedDelay = fixedDelay;
        }
    }
}
