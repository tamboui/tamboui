/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.widgets.form;

import java.util.Arrays;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for FormState.
 */
class FormStateTest {

    // ==================== Text Fields ====================

    @Test
    @DisplayName("textField() returns TextInputState")
    void textFieldReturnsState() {
        FormState form = FormState.builder()
                .textField("name", "John")
                .build();

        assertThat(form.textField("name")).isNotNull();
        assertThat(form.textField("name").text()).isEqualTo("John");
    }

    @Test
    @DisplayName("textValue() returns current value")
    void textValueReturnsValue() {
        FormState form = FormState.builder()
                .textField("name", "John")
                .build();

        assertThat(form.textValue("name")).isEqualTo("John");
    }

    @Test
    @DisplayName("setTextValue() updates value")
    void setTextValueUpdates() {
        FormState form = FormState.builder()
                .textField("name", "John")
                .build();

        form.setTextValue("name", "Jane");
        assertThat(form.textValue("name")).isEqualTo("Jane");
    }

    @Test
    @DisplayName("textValues() returns all text values")
    void textValuesReturnsAll() {
        FormState form = FormState.builder()
                .textField("firstName", "John")
                .textField("lastName", "Doe")
                .build();

        Map<String, String> values = form.textValues();
        assertThat(values).containsEntry("firstName", "John");
        assertThat(values).containsEntry("lastName", "Doe");
    }

    @Test
    @DisplayName("textField() with empty value creates empty state")
    void textFieldWithEmptyValue() {
        FormState form = FormState.builder()
                .textField("name")
                .build();

        assertThat(form.textValue("name")).isEmpty();
    }

    @Test
    @DisplayName("textField() throws on unknown field")
    void textFieldThrowsOnUnknown() {
        FormState form = FormState.builder()
                .textField("name", "John")
                .build();

        assertThatThrownBy(() -> form.textField("unknown"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("unknown");
    }

    // ==================== Boolean Fields ====================

    @Test
    @DisplayName("booleanField() returns BooleanFieldState")
    void booleanFieldReturnsState() {
        FormState form = FormState.builder()
                .booleanField("subscribe", true)
                .build();

        assertThat(form.booleanField("subscribe")).isNotNull();
        assertThat(form.booleanField("subscribe").value()).isTrue();
    }

    @Test
    @DisplayName("booleanValue() returns current value")
    void booleanValueReturnsValue() {
        FormState form = FormState.builder()
                .booleanField("subscribe", true)
                .build();

        assertThat(form.booleanValue("subscribe")).isTrue();
    }

    @Test
    @DisplayName("setBooleanValue() updates value")
    void setBooleanValueUpdates() {
        FormState form = FormState.builder()
                .booleanField("subscribe", true)
                .build();

        form.setBooleanValue("subscribe", false);
        assertThat(form.booleanValue("subscribe")).isFalse();
    }

    @Test
    @DisplayName("booleanField() with no value defaults to false")
    void booleanFieldDefaultsFalse() {
        FormState form = FormState.builder()
                .booleanField("subscribe")
                .build();

        assertThat(form.booleanValue("subscribe")).isFalse();
    }

    @Test
    @DisplayName("booleanField() throws on unknown field")
    void booleanFieldThrowsOnUnknown() {
        FormState form = FormState.builder()
                .booleanField("subscribe", true)
                .build();

        assertThatThrownBy(() -> form.booleanField("unknown"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("unknown");
    }

    @Test
    @DisplayName("booleanValues() returns all boolean values")
    void booleanValuesReturnsAll() {
        FormState form = FormState.builder()
                .booleanField("newsletter", true)
                .booleanField("darkMode", false)
                .build();

        Map<String, Boolean> values = form.booleanValues();
        assertThat(values).containsEntry("newsletter", true);
        assertThat(values).containsEntry("darkMode", false);
    }

    // ==================== Select Fields ====================

    @Test
    @DisplayName("selectField() returns SelectFieldState")
    void selectFieldReturnsState() {
        FormState form = FormState.builder()
                .selectField("country", Arrays.asList("USA", "UK", "Germany"), 1)
                .build();

        assertThat(form.selectField("country")).isNotNull();
        assertThat(form.selectField("country").selectedValue()).isEqualTo("UK");
    }

    @Test
    @DisplayName("selectValue() returns selected value")
    void selectValueReturnsValue() {
        FormState form = FormState.builder()
                .selectField("country", Arrays.asList("USA", "UK", "Germany"), 0)
                .build();

        assertThat(form.selectValue("country")).isEqualTo("USA");
    }

    @Test
    @DisplayName("selectIndex() returns and sets index")
    void selectIndexReturnsAndSets() {
        FormState form = FormState.builder()
                .selectField("country", Arrays.asList("USA", "UK", "Germany"), 0)
                .build();

        assertThat(form.selectIndex("country")).isEqualTo(0);

        form.selectIndex("country", 2);
        assertThat(form.selectIndex("country")).isEqualTo(2);
        assertThat(form.selectValue("country")).isEqualTo("Germany");
    }

    @Test
    @DisplayName("selectField() with no index defaults to first")
    void selectFieldDefaultsToFirst() {
        FormState form = FormState.builder()
                .selectField("country", Arrays.asList("USA", "UK", "Germany"))
                .build();

        assertThat(form.selectValue("country")).isEqualTo("USA");
    }

    @Test
    @DisplayName("selectField() throws on unknown field")
    void selectFieldThrowsOnUnknown() {
        FormState form = FormState.builder()
                .selectField("country", Arrays.asList("USA", "UK"))
                .build();

        assertThatThrownBy(() -> form.selectField("unknown"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("unknown");
    }

    @Test
    @DisplayName("selectValues() returns all selected values")
    void selectValuesReturnsAll() {
        FormState form = FormState.builder()
                .selectField("country", Arrays.asList("USA", "UK", "Germany"), 1)
                .selectField("role", Arrays.asList("Admin", "User"), 0)
                .build();

        Map<String, String> values = form.selectValues();
        assertThat(values).containsEntry("country", "UK");
        assertThat(values).containsEntry("role", "Admin");
    }

    // ==================== Builder ====================

    @Test
    @DisplayName("builder creates form with all field types")
    void builderCreatesAllTypes() {
        FormState form = FormState.builder()
                .textField("name", "John")
                .booleanField("subscribe", true)
                .selectField("country", Arrays.asList("USA", "UK"), 0)
                .build();

        assertThat(form.textValue("name")).isEqualTo("John");
        assertThat(form.booleanValue("subscribe")).isTrue();
        assertThat(form.selectValue("country")).isEqualTo("USA");
    }

    // ==================== Validation State ====================

    @Test
    @DisplayName("validationResult() returns valid by default")
    void validationResultReturnsValidByDefault() {
        FormState form = FormState.builder()
                .textField("email", "")
                .build();

        assertThat(form.validationResult("email").isValid()).isTrue();
        assertThat(form.validationResult("unknown").isValid()).isTrue();
    }

    @Test
    @DisplayName("setValidationResult() stores validation result")
    void setValidationResultStoresResult() {
        FormState form = FormState.builder()
                .textField("email", "")
                .build();

        ValidationResult error = ValidationResult.invalid("Invalid email");
        form.setValidationResult("email", error);

        assertThat(form.validationResult("email").isValid()).isFalse();
        assertThat(form.validationResult("email").errorMessage()).isEqualTo("Invalid email");
    }

    @Test
    @DisplayName("clearValidationResult() removes validation result")
    void clearValidationResultRemovesResult() {
        FormState form = FormState.builder()
                .textField("email", "")
                .build();

        form.setValidationResult("email", ValidationResult.invalid("Error"));
        assertThat(form.validationResult("email").isValid()).isFalse();

        form.clearValidationResult("email");
        assertThat(form.validationResult("email").isValid()).isTrue();
    }

    @Test
    @DisplayName("clearAllValidationResults() removes all results")
    void clearAllValidationResultsRemovesAll() {
        FormState form = FormState.builder()
                .textField("email", "")
                .textField("name", "")
                .build();

        form.setValidationResult("email", ValidationResult.invalid("Error 1"));
        form.setValidationResult("name", ValidationResult.invalid("Error 2"));

        form.clearAllValidationResults();

        assertThat(form.validationResult("email").isValid()).isTrue();
        assertThat(form.validationResult("name").isValid()).isTrue();
    }

    @Test
    @DisplayName("hasValidationErrors() returns true when errors exist")
    void hasValidationErrorsReturnsTrueWhenErrorsExist() {
        FormState form = FormState.builder()
                .textField("email", "")
                .textField("name", "John")
                .build();

        assertThat(form.hasValidationErrors()).isFalse();

        form.setValidationResult("email", ValidationResult.invalid("Required"));

        assertThat(form.hasValidationErrors()).isTrue();
    }

    @Test
    @DisplayName("setValidationResult() with null stores valid result")
    void setValidationResultWithNullStoresValid() {
        FormState form = FormState.builder()
                .textField("email", "")
                .build();

        form.setValidationResult("email", ValidationResult.invalid("Error"));
        form.setValidationResult("email", null);

        assertThat(form.validationResult("email").isValid()).isTrue();
    }

    // ==================== Masked Fields ====================

    @Test
    @DisplayName("maskedField() creates text field marked as masked")
    void maskedFieldCreatesMarkedTextField() {
        FormState form = FormState.builder()
                .maskedField("password", "secret")
                .build();

        // Should be accessible as text field
        assertThat(form.textValue("password")).isEqualTo("secret");
        // Should be marked as masked
        assertThat(form.isMaskedField("password")).isTrue();
    }

    @Test
    @DisplayName("maskedField() without initial value creates empty field")
    void maskedFieldWithoutValueCreatesEmpty() {
        FormState form = FormState.builder()
                .maskedField("password")
                .build();

        assertThat(form.textValue("password")).isEmpty();
        assertThat(form.isMaskedField("password")).isTrue();
    }

    @Test
    @DisplayName("textField() is not marked as masked")
    void textFieldIsNotMasked() {
        FormState form = FormState.builder()
                .textField("username", "john")
                .build();

        assertThat(form.isMaskedField("username")).isFalse();
    }

    @Test
    @DisplayName("isMaskedField() returns false for unknown fields")
    void isMaskedFieldReturnsFalseForUnknown() {
        FormState form = FormState.builder()
                .textField("name", "")
                .build();

        assertThat(form.isMaskedField("unknown")).isFalse();
    }
}
