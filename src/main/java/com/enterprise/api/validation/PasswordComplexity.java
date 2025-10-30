package com.enterprise.api.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * Custom validation annotation for password complexity requirements.
 * Ensures passwords meet security standards with uppercase, lowercase, numbers, and special characters.
 */
@Documented
@Constraint(validatedBy = PasswordComplexityValidator.class)
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface PasswordComplexity {
    
    String message() default "Password must contain at least one uppercase letter, one lowercase letter, one number, and one special character";
    
    Class<?>[] groups() default {};
    
    Class<? extends Payload>[] payload() default {};
}