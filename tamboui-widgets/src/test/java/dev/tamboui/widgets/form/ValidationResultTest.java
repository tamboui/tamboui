/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.widgets.form;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for ValidationResult.
 */
class ValidationResultTest {

    @Test
    @DisplayName("valid() returns a valid result")
    void validReturnsValidResult() {
        ValidationResult result = ValidationResult.valid();
        assertThat(result.isValid()).isTrue();
        assertThat(result.errorMessage()).isNull();
    }

    @Test
    @DisplayName("invalid() returns an invalid result with error message")
    void invalidReturnsInvalidResult() {
        ValidationResult result = ValidationResult.invalid("Field is required");
        assertThat(result.isValid()).isFalse();
        assertThat(result.errorMessage()).isEqualTo("Field is required");
    }

    @Test
    @DisplayName("invalid() throws on null error message")
    void invalidThrowsOnNull() {
        assertThatThrownBy(() -> ValidationResult.invalid(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("valid() returns singleton")
    void validReturnsSingleton() {
        ValidationResult result1 = ValidationResult.valid();
        ValidationResult result2 = ValidationResult.valid();
        assertThat(result1).isSameAs(result2);
    }

    @Test
    @DisplayName("equals and hashCode work correctly")
    void equalsAndHashCode() {
        ValidationResult valid1 = ValidationResult.valid();
        ValidationResult valid2 = ValidationResult.valid();
        ValidationResult invalid1 = ValidationResult.invalid("error");
        ValidationResult invalid2 = ValidationResult.invalid("error");
        ValidationResult invalid3 = ValidationResult.invalid("different");

        assertThat(valid1).isEqualTo(valid2);
        assertThat(invalid1).isEqualTo(invalid2);
        assertThat(valid1).isNotEqualTo(invalid1);
        assertThat(invalid1).isNotEqualTo(invalid3);

        assertThat(valid1.hashCode()).isEqualTo(valid2.hashCode());
        assertThat(invalid1.hashCode()).isEqualTo(invalid2.hashCode());
    }

    @Test
    @DisplayName("toString() returns meaningful representation")
    void toStringReturnsRepresentation() {
        assertThat(ValidationResult.valid().toString()).contains("valid");
        assertThat(ValidationResult.invalid("test error").toString())
                .contains("invalid")
                .contains("test error");
    }
}
