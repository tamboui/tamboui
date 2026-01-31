/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.widgets.form;

import java.util.Objects;

/**
 * Represents the result of a validation check.
 * <p>
 * A validation result is either valid (no errors) or invalid with an error message.
 * <p>
 * Example usage:
 * <pre>{@code
 * ValidationResult result = Validators.required().validate("");
 * if (!result.isValid()) {
 *     System.out.println(result.errorMessage());  // "Field is required"
 * }
 * }</pre>
 */
public final class ValidationResult {

    private static final ValidationResult VALID = new ValidationResult(true, null);

    private final boolean valid;
    private final String errorMessage;

    private ValidationResult(boolean valid, String errorMessage) {
        this.valid = valid;
        this.errorMessage = errorMessage;
    }

    /**
     * Creates a valid result indicating the validation passed.
     *
     * @return a valid result
     */
    public static ValidationResult valid() {
        return VALID;
    }

    /**
     * Creates an invalid result with the given error message.
     *
     * @param errorMessage the error message describing why validation failed
     * @return an invalid result
     * @throws NullPointerException if errorMessage is null
     */
    public static ValidationResult invalid(String errorMessage) {
        Objects.requireNonNull(errorMessage, "Error message must not be null");
        return new ValidationResult(false, errorMessage);
    }

    /**
     * Returns whether the validation passed.
     *
     * @return true if valid, false if invalid
     */
    public boolean isValid() {
        return valid;
    }

    /**
     * Returns the error message if validation failed.
     *
     * @return the error message, or null if validation passed
     */
    public String errorMessage() {
        return errorMessage;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ValidationResult that = (ValidationResult) o;
        return valid == that.valid && Objects.equals(errorMessage, that.errorMessage);
    }

    @Override
    public int hashCode() {
        return Objects.hash(valid, errorMessage);
    }

    @Override
    public String toString() {
        if (valid) {
            return "ValidationResult[valid]";
        }
        return "ValidationResult[invalid: " + errorMessage + "]";
    }
}
