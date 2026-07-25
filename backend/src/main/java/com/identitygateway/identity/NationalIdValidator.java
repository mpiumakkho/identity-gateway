package com.identitygateway.identity;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class NationalIdValidator implements ConstraintValidator<NationalId, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        return value == null || IdentityDataProtector.isValidNationalId(value);
    }
}