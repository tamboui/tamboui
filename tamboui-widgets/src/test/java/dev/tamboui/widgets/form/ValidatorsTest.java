/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.widgets.form;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for Validators.
 */
class ValidatorsTest {

    // ==================== required() ====================

    @Test
    @DisplayName("required() fails on null")
    void requiredFailsOnNull() {
        ValidationResult result = Validators.required().validate(null);
        assertThat(result.isValid()).isFalse();
        assertThat(result.errorMessage()).isEqualTo("Field is required");
    }

    @Test
    @DisplayName("required() fails on empty string")
    void requiredFailsOnEmpty() {
        ValidationResult result = Validators.required().validate("");
        assertThat(result.isValid()).isFalse();
    }

    @Test
    @DisplayName("required() fails on whitespace-only string")
    void requiredFailsOnWhitespace() {
        ValidationResult result = Validators.required().validate("   ");
        assertThat(result.isValid()).isFalse();
    }

    @Test
    @DisplayName("required() passes on non-empty string")
    void requiredPassesOnNonEmpty() {
        ValidationResult result = Validators.required().validate("hello");
        assertThat(result.isValid()).isTrue();
    }

    @Test
    @DisplayName("required() with custom message uses that message")
    void requiredWithCustomMessage() {
        ValidationResult result = Validators.required("Name is required").validate("");
        assertThat(result.errorMessage()).isEqualTo("Name is required");
    }

    // ==================== email() ====================

    @Test
    @DisplayName("email() passes on valid email")
    void emailPassesOnValid() {
        assertThat(Validators.email().validate("user@example.com").isValid()).isTrue();
        assertThat(Validators.email().validate("user.name@example.co.uk").isValid()).isTrue();
        assertThat(Validators.email().validate("user+tag@example.com").isValid()).isTrue();
    }

    @Test
    @DisplayName("email() fails on invalid email")
    void emailFailsOnInvalid() {
        assertThat(Validators.email().validate("invalid").isValid()).isFalse();
        assertThat(Validators.email().validate("@example.com").isValid()).isFalse();
        assertThat(Validators.email().validate("user@").isValid()).isFalse();
        assertThat(Validators.email().validate("user@example").isValid()).isFalse();
    }

    @Test
    @DisplayName("email() passes on empty (use required() for non-empty)")
    void emailPassesOnEmpty() {
        assertThat(Validators.email().validate("").isValid()).isTrue();
        assertThat(Validators.email().validate(null).isValid()).isTrue();
    }

    @Test
    @DisplayName("email() with custom message uses that message")
    void emailWithCustomMessage() {
        ValidationResult result = Validators.email("Please enter a valid email").validate("invalid");
        assertThat(result.errorMessage()).isEqualTo("Please enter a valid email");
    }

    // ==================== minLength() ====================

    @Test
    @DisplayName("minLength() passes when length >= minimum")
    void minLengthPassesWhenAbove() {
        assertThat(Validators.minLength(3).validate("abc").isValid()).isTrue();
        assertThat(Validators.minLength(3).validate("abcd").isValid()).isTrue();
    }

    @Test
    @DisplayName("minLength() fails when length < minimum")
    void minLengthFailsWhenBelow() {
        assertThat(Validators.minLength(3).validate("ab").isValid()).isFalse();
        assertThat(Validators.minLength(3).validate("").isValid()).isFalse();
    }

    @Test
    @DisplayName("minLength() treats null as empty string")
    void minLengthTreatsNullAsEmpty() {
        assertThat(Validators.minLength(1).validate(null).isValid()).isFalse();
    }

    @Test
    @DisplayName("minLength() with custom message uses that message")
    void minLengthWithCustomMessage() {
        ValidationResult result = Validators.minLength(3, "Too short").validate("ab");
        assertThat(result.errorMessage()).isEqualTo("Too short");
    }

    // ==================== maxLength() ====================

    @Test
    @DisplayName("maxLength() passes when length <= maximum")
    void maxLengthPassesWhenBelow() {
        assertThat(Validators.maxLength(3).validate("abc").isValid()).isTrue();
        assertThat(Validators.maxLength(3).validate("ab").isValid()).isTrue();
        assertThat(Validators.maxLength(3).validate("").isValid()).isTrue();
    }

    @Test
    @DisplayName("maxLength() fails when length > maximum")
    void maxLengthFailsWhenAbove() {
        assertThat(Validators.maxLength(3).validate("abcd").isValid()).isFalse();
    }

    @Test
    @DisplayName("maxLength() passes on null")
    void maxLengthPassesOnNull() {
        assertThat(Validators.maxLength(3).validate(null).isValid()).isTrue();
    }

    @Test
    @DisplayName("maxLength() with custom message uses that message")
    void maxLengthWithCustomMessage() {
        ValidationResult result = Validators.maxLength(3, "Too long").validate("abcd");
        assertThat(result.errorMessage()).isEqualTo("Too long");
    }

    // ==================== pattern() ====================

    @Test
    @DisplayName("pattern() passes when matches")
    void patternPassesWhenMatches() {
        assertThat(Validators.pattern("\\d{3}").validate("123").isValid()).isTrue();
        assertThat(Validators.pattern("[A-Z]+").validate("ABC").isValid()).isTrue();
    }

    @Test
    @DisplayName("pattern() fails when not matches")
    void patternFailsWhenNotMatches() {
        assertThat(Validators.pattern("\\d{3}").validate("12").isValid()).isFalse();
        assertThat(Validators.pattern("\\d{3}").validate("abc").isValid()).isFalse();
    }

    @Test
    @DisplayName("pattern() passes on empty (use required() for non-empty)")
    void patternPassesOnEmpty() {
        assertThat(Validators.pattern("\\d+").validate("").isValid()).isTrue();
        assertThat(Validators.pattern("\\d+").validate(null).isValid()).isTrue();
    }

    @Test
    @DisplayName("pattern() with custom message uses that message")
    void patternWithCustomMessage() {
        ValidationResult result = Validators.pattern("\\d{3}", "Must be 3 digits").validate("ab");
        assertThat(result.errorMessage()).isEqualTo("Must be 3 digits");
    }

    // ==================== range() ====================

    @Test
    @DisplayName("range() passes when within range")
    void rangePassesWhenWithin() {
        assertThat(Validators.range(1, 10).validate("5").isValid()).isTrue();
        assertThat(Validators.range(1, 10).validate("1").isValid()).isTrue();
        assertThat(Validators.range(1, 10).validate("10").isValid()).isTrue();
    }

    @Test
    @DisplayName("range() fails when outside range")
    void rangeFailsWhenOutside() {
        assertThat(Validators.range(1, 10).validate("0").isValid()).isFalse();
        assertThat(Validators.range(1, 10).validate("11").isValid()).isFalse();
    }

    @Test
    @DisplayName("range() fails on non-numeric")
    void rangeFailsOnNonNumeric() {
        ValidationResult result = Validators.range(1, 10).validate("abc");
        assertThat(result.isValid()).isFalse();
        assertThat(result.errorMessage()).isEqualTo("Must be a number");
    }

    @Test
    @DisplayName("range() passes on empty (use required() for non-empty)")
    void rangePassesOnEmpty() {
        assertThat(Validators.range(1, 10).validate("").isValid()).isTrue();
        assertThat(Validators.range(1, 10).validate(null).isValid()).isTrue();
    }

    @Test
    @DisplayName("range() with custom message uses that message")
    void rangeWithCustomMessage() {
        ValidationResult result = Validators.range(1, 10, "Out of range").validate("0");
        assertThat(result.errorMessage()).isEqualTo("Out of range");
    }

    // ==================== Composition ====================

    @Test
    @DisplayName("and() composes validators")
    void andComposesValidators() {
        Validator composed = Validators.required().and(Validators.email());

        assertThat(composed.validate("").isValid()).isFalse();
        assertThat(composed.validate("invalid").isValid()).isFalse();
        assertThat(composed.validate("user@example.com").isValid()).isTrue();
    }

    @Test
    @DisplayName("and() short-circuits on first failure")
    void andShortCircuits() {
        Validator composed = Validators.required().and(Validators.email());
        ValidationResult result = composed.validate("");
        assertThat(result.errorMessage()).isEqualTo("Field is required");
    }
}
