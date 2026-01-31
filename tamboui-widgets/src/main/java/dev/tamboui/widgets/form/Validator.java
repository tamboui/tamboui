/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.widgets.form;

/**
 * A validator that checks if a string value meets certain criteria.
 * <p>
 * Validators are used to validate form field input. They can be composed
 * using the {@link #and(Validator)} method.
 * <p>
 * Example usage:
 * <pre>{@code
 * // Using built-in validators
 * Validator emailValidator = Validators.required().and(Validators.email());
 * ValidationResult result = emailValidator.validate("user@example.com");
 *
 * // Custom validator
 * Validator usernameAvailable = value ->
 *     userService.isAvailable(value)
 *         ? ValidationResult.valid()
 *         : ValidationResult.invalid("Username is taken");
 * }</pre>
 *
 * @see Validators for built-in validators
 * @see ValidationResult for validation results
 */
@FunctionalInterface
public interface Validator {

    /**
     * Validates the given value.
     *
     * @param value the value to validate (may be null or empty)
     * @return the validation result
     */
    ValidationResult validate(String value);

    /**
     * Composes this validator with another, requiring both to pass.
     * <p>
     * If this validator fails, the other validator is not called and
     * this validator's error message is returned.
     *
     * @param other the other validator to compose with
     * @return a composed validator that requires both validators to pass
     */
    default Validator and(Validator other) {
        return value -> {
            ValidationResult result = validate(value);
            if (!result.isValid()) {
                return result;
            }
            return other.validate(value);
        };
    }
}
