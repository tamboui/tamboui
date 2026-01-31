/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.widgets.form;

import java.util.regex.Pattern;

/**
 * Built-in validators for common validation scenarios.
 * <p>
 * Example usage:
 * <pre>{@code
 * formField("Email", emailState)
 *     .validate(Validators.required(), Validators.email())
 *
 * formField("Username", usernameState)
 *     .validate(Validators.required(), Validators.minLength(3), Validators.maxLength(20))
 *
 * formField("Phone", phoneState)
 *     .validate(Validators.pattern("\\d{3}-\\d{3}-\\d{4}", "Invalid phone format"))
 * }</pre>
 */
public final class Validators {

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$"
    );

    private Validators() {
        // Utility class
    }

    /**
     * Creates a validator that requires a non-empty value.
     *
     * @return a required validator
     */
    public static Validator required() {
        return value -> {
            if (value == null || value.trim().isEmpty()) {
                return ValidationResult.invalid("Field is required");
            }
            return ValidationResult.valid();
        };
    }

    /**
     * Creates a validator that requires a non-empty value with a custom message.
     *
     * @param message the error message when validation fails
     * @return a required validator with custom message
     */
    public static Validator required(String message) {
        return value -> {
            if (value == null || value.trim().isEmpty()) {
                return ValidationResult.invalid(message);
            }
            return ValidationResult.valid();
        };
    }

    /**
     * Creates a validator that requires a valid email format.
     *
     * @return an email validator
     */
    public static Validator email() {
        return value -> {
            if (value == null || value.isEmpty()) {
                return ValidationResult.valid(); // Use required() for non-empty check
            }
            if (!EMAIL_PATTERN.matcher(value).matches()) {
                return ValidationResult.invalid("Invalid email format");
            }
            return ValidationResult.valid();
        };
    }

    /**
     * Creates a validator that requires a valid email format with a custom message.
     *
     * @param message the error message when validation fails
     * @return an email validator with custom message
     */
    public static Validator email(String message) {
        return value -> {
            if (value == null || value.isEmpty()) {
                return ValidationResult.valid();
            }
            if (!EMAIL_PATTERN.matcher(value).matches()) {
                return ValidationResult.invalid(message);
            }
            return ValidationResult.valid();
        };
    }

    /**
     * Creates a validator that requires a minimum length.
     *
     * @param min the minimum length (inclusive)
     * @return a minimum length validator
     */
    public static Validator minLength(int min) {
        return value -> {
            if (value == null) {
                value = "";
            }
            if (value.length() < min) {
                return ValidationResult.invalid("Must be at least " + min + " characters");
            }
            return ValidationResult.valid();
        };
    }

    /**
     * Creates a validator that requires a minimum length with a custom message.
     *
     * @param min the minimum length (inclusive)
     * @param message the error message when validation fails
     * @return a minimum length validator with custom message
     */
    public static Validator minLength(int min, String message) {
        return value -> {
            if (value == null) {
                value = "";
            }
            if (value.length() < min) {
                return ValidationResult.invalid(message);
            }
            return ValidationResult.valid();
        };
    }

    /**
     * Creates a validator that requires a maximum length.
     *
     * @param max the maximum length (inclusive)
     * @return a maximum length validator
     */
    public static Validator maxLength(int max) {
        return value -> {
            if (value == null) {
                return ValidationResult.valid();
            }
            if (value.length() > max) {
                return ValidationResult.invalid("Must be at most " + max + " characters");
            }
            return ValidationResult.valid();
        };
    }

    /**
     * Creates a validator that requires a maximum length with a custom message.
     *
     * @param max the maximum length (inclusive)
     * @param message the error message when validation fails
     * @return a maximum length validator with custom message
     */
    public static Validator maxLength(int max, String message) {
        return value -> {
            if (value == null) {
                return ValidationResult.valid();
            }
            if (value.length() > max) {
                return ValidationResult.invalid(message);
            }
            return ValidationResult.valid();
        };
    }

    /**
     * Creates a validator that requires the value to match a regex pattern.
     *
     * @param regex the regex pattern
     * @return a pattern validator
     */
    public static Validator pattern(String regex) {
        Pattern pattern = Pattern.compile(regex);
        return value -> {
            if (value == null || value.isEmpty()) {
                return ValidationResult.valid(); // Use required() for non-empty check
            }
            if (!pattern.matcher(value).matches()) {
                return ValidationResult.invalid("Invalid format");
            }
            return ValidationResult.valid();
        };
    }

    /**
     * Creates a validator that requires the value to match a regex pattern.
     *
     * @param regex the regex pattern
     * @param message the error message when validation fails
     * @return a pattern validator with custom message
     */
    public static Validator pattern(String regex, String message) {
        Pattern pattern = Pattern.compile(regex);
        return value -> {
            if (value == null || value.isEmpty()) {
                return ValidationResult.valid();
            }
            if (!pattern.matcher(value).matches()) {
                return ValidationResult.invalid(message);
            }
            return ValidationResult.valid();
        };
    }

    /**
     * Creates a validator that requires a numeric value within a range.
     *
     * @param min the minimum value (inclusive)
     * @param max the maximum value (inclusive)
     * @return a range validator
     */
    public static Validator range(int min, int max) {
        return value -> {
            if (value == null || value.isEmpty()) {
                return ValidationResult.valid(); // Use required() for non-empty check
            }
            try {
                int number = Integer.parseInt(value);
                if (number < min || number > max) {
                    return ValidationResult.invalid("Must be between " + min + " and " + max);
                }
                return ValidationResult.valid();
            } catch (NumberFormatException e) {
                return ValidationResult.invalid("Must be a number");
            }
        };
    }

    /**
     * Creates a validator that requires a numeric value within a range with a custom message.
     *
     * @param min the minimum value (inclusive)
     * @param max the maximum value (inclusive)
     * @param message the error message when validation fails
     * @return a range validator with custom message
     */
    public static Validator range(int min, int max, String message) {
        return value -> {
            if (value == null || value.isEmpty()) {
                return ValidationResult.valid();
            }
            try {
                int number = Integer.parseInt(value);
                if (number < min || number > max) {
                    return ValidationResult.invalid(message);
                }
                return ValidationResult.valid();
            } catch (NumberFormatException e) {
                return ValidationResult.invalid("Must be a number");
            }
        };
    }
}
