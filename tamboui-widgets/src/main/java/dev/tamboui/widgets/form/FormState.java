/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.widgets.form;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import dev.tamboui.widgets.input.TextInputState;

/**
 * Centralized state container for all fields in a form.
 * <p>
 * FormState holds the state for text fields, boolean fields (checkbox/toggle),
 * and select fields (dropdown). It provides type-safe access to field states
 * and convenient methods for getting/setting values.
 * <p>
 * Example usage:
 * <pre>{@code
 * // Create form state (typically as an instance field)
 * FormState form = FormState.builder()
 *     .textField("fullName", "Ada Lovelace")
 *     .textField("email", "ada@analytical.io")
 *     .booleanField("newsletter", true)
 *     .selectField("country", Arrays.asList("USA", "UK", "Germany"), 0)
 *     .build();
 *
 * // Access text field state for UI binding
 * formField("Full name", form.textField("fullName"))
 *
 * // Get/set values
 * String email = form.textValue("email");
 * form.setTextValue("email", "new@example.com");
 *
 * // Get all values
 * Map<String, String> values = form.textValues();
 * }</pre>
 *
 * @see TextInputState for text field state
 * @see BooleanFieldState for boolean field state
 * @see SelectFieldState for select field state
 */
public final class FormState {

    private final Map<String, TextInputState> textFields;
    private final Map<String, BooleanFieldState> booleanFields;
    private final Map<String, SelectFieldState> selectFields;
    private final Set<String> maskedFields;
    private final Map<String, ValidationResult> validationResults = new LinkedHashMap<>();

    private FormState(Builder builder) {
        this.textFields = new LinkedHashMap<>(builder.textFields);
        this.booleanFields = new LinkedHashMap<>(builder.booleanFields);
        this.selectFields = new LinkedHashMap<>(builder.selectFields);
        this.maskedFields = new HashSet<>(builder.maskedFields);
    }

    /**
     * Creates a new builder for constructing a FormState.
     *
     * @return a new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    // ==================== Text Field Access ====================

    /**
     * Returns the TextInputState for the given field name.
     *
     * @param name the field name
     * @return the text input state
     * @throws IllegalArgumentException if no text field with that name exists
     */
    public TextInputState textField(String name) {
        TextInputState state = textFields.get(name);
        if (state == null) {
            throw new IllegalArgumentException("No text field named: " + name);
        }
        return state;
    }

    /**
     * Returns the current text value for the given field name.
     *
     * @param name the field name
     * @return the text value
     * @throws IllegalArgumentException if no text field with that name exists
     */
    public String textValue(String name) {
        return textField(name).text();
    }

    /**
     * Sets the text value for the given field name.
     *
     * @param name the field name
     * @param value the new value
     * @throws IllegalArgumentException if no text field with that name exists
     */
    public void setTextValue(String name, String value) {
        textField(name).setText(value);
    }

    /**
     * Returns all text field values as a map.
     *
     * @return an unmodifiable map of field names to values
     */
    public Map<String, String> textValues() {
        Map<String, String> values = new LinkedHashMap<>();
        for (Map.Entry<String, TextInputState> entry : textFields.entrySet()) {
            values.put(entry.getKey(), entry.getValue().text());
        }
        return Collections.unmodifiableMap(values);
    }

    /**
     * Returns whether the given field should be masked (e.g., password field).
     *
     * @param name the field name
     * @return true if the field should display masked characters
     */
    public boolean isMaskedField(String name) {
        return maskedFields.contains(name);
    }

    // ==================== Boolean Field Access ====================

    /**
     * Returns the BooleanFieldState for the given field name.
     *
     * @param name the field name
     * @return the boolean field state
     * @throws IllegalArgumentException if no boolean field with that name exists
     */
    public BooleanFieldState booleanField(String name) {
        BooleanFieldState state = booleanFields.get(name);
        if (state == null) {
            throw new IllegalArgumentException("No boolean field named: " + name);
        }
        return state;
    }

    /**
     * Returns the current boolean value for the given field name.
     *
     * @param name the field name
     * @return the boolean value
     * @throws IllegalArgumentException if no boolean field with that name exists
     */
    public boolean booleanValue(String name) {
        return booleanField(name).value();
    }

    /**
     * Sets the boolean value for the given field name.
     *
     * @param name the field name
     * @param value the new value
     * @throws IllegalArgumentException if no boolean field with that name exists
     */
    public void setBooleanValue(String name, boolean value) {
        booleanField(name).setValue(value);
    }

    /**
     * Returns all boolean field values as a map.
     *
     * @return an unmodifiable map of field names to values
     */
    public Map<String, Boolean> booleanValues() {
        Map<String, Boolean> values = new LinkedHashMap<>();
        for (Map.Entry<String, BooleanFieldState> entry : booleanFields.entrySet()) {
            values.put(entry.getKey(), entry.getValue().value());
        }
        return Collections.unmodifiableMap(values);
    }

    // ==================== Select Field Access ====================

    /**
     * Returns the SelectFieldState for the given field name.
     *
     * @param name the field name
     * @return the select field state
     * @throws IllegalArgumentException if no select field with that name exists
     */
    public SelectFieldState selectField(String name) {
        SelectFieldState state = selectFields.get(name);
        if (state == null) {
            throw new IllegalArgumentException("No select field named: " + name);
        }
        return state;
    }

    /**
     * Returns the currently selected value for the given field name.
     *
     * @param name the field name
     * @return the selected value
     * @throws IllegalArgumentException if no select field with that name exists
     */
    public String selectValue(String name) {
        return selectField(name).selectedValue();
    }

    /**
     * Returns the currently selected index for the given field name.
     *
     * @param name the field name
     * @return the selected index (0-based)
     * @throws IllegalArgumentException if no select field with that name exists
     */
    public int selectIndex(String name) {
        return selectField(name).selectedIndex();
    }

    /**
     * Selects the option at the given index for the given field name.
     *
     * @param name the field name
     * @param index the index to select (0-based)
     * @throws IllegalArgumentException if no select field with that name exists or index is out of bounds
     */
    public void selectIndex(String name, int index) {
        selectField(name).selectIndex(index);
    }

    /**
     * Returns all select field values as a map.
     *
     * @return an unmodifiable map of field names to selected values
     */
    public Map<String, String> selectValues() {
        Map<String, String> values = new LinkedHashMap<>();
        for (Map.Entry<String, SelectFieldState> entry : selectFields.entrySet()) {
            values.put(entry.getKey(), entry.getValue().selectedValue());
        }
        return Collections.unmodifiableMap(values);
    }

    // ==================== Validation State ====================

    /**
     * Returns the validation result for the given field name.
     *
     * @param name the field name
     * @return the validation result, or {@link ValidationResult#valid()} if not set
     */
    public ValidationResult validationResult(String name) {
        return validationResults.getOrDefault(name, ValidationResult.valid());
    }

    /**
     * Sets the validation result for the given field name.
     *
     * @param name the field name
     * @param result the validation result
     */
    public void setValidationResult(String name, ValidationResult result) {
        validationResults.put(name, result != null ? result : ValidationResult.valid());
    }

    /**
     * Clears the validation result for the given field name.
     *
     * @param name the field name
     */
    public void clearValidationResult(String name) {
        validationResults.remove(name);
    }

    /**
     * Clears all validation results.
     */
    public void clearAllValidationResults() {
        validationResults.clear();
    }

    /**
     * Returns true if any field has a validation error.
     *
     * @return true if there are validation errors
     */
    public boolean hasValidationErrors() {
        return validationResults.values().stream().anyMatch(r -> !r.isValid());
    }

    /**
     * Builder for constructing FormState instances.
     */
    public static final class Builder {

        private final Map<String, TextInputState> textFields = new LinkedHashMap<>();
        private final Map<String, BooleanFieldState> booleanFields = new LinkedHashMap<>();
        private final Map<String, SelectFieldState> selectFields = new LinkedHashMap<>();
        private final Set<String> maskedFields = new HashSet<>();

        private Builder() {
        }

        /**
         * Adds a text field with the given initial value.
         *
         * @param name the field name
         * @param initialValue the initial value
         * @return this builder for chaining
         */
        public Builder textField(String name, String initialValue) {
            Objects.requireNonNull(name, "Field name must not be null");
            textFields.put(name, new TextInputState(initialValue != null ? initialValue : ""));
            return this;
        }

        /**
         * Adds a text field with an empty initial value.
         *
         * @param name the field name
         * @return this builder for chaining
         */
        public Builder textField(String name) {
            return textField(name, "");
        }

        /**
         * Adds a masked text field (e.g., for passwords) with the given initial value.
         * <p>
         * Masked fields display '*' characters instead of actual text.
         * Use {@link FormState#isMaskedField(String)} to check if a field is masked.
         *
         * @param name the field name
         * @param initialValue the initial value
         * @return this builder for chaining
         */
        public Builder maskedField(String name, String initialValue) {
            textField(name, initialValue);
            maskedFields.add(name);
            return this;
        }

        /**
         * Adds a masked text field (e.g., for passwords) with an empty initial value.
         *
         * @param name the field name
         * @return this builder for chaining
         */
        public Builder maskedField(String name) {
            return maskedField(name, "");
        }

        /**
         * Adds a boolean field with the given initial value.
         *
         * @param name the field name
         * @param initialValue the initial value
         * @return this builder for chaining
         */
        public Builder booleanField(String name, boolean initialValue) {
            Objects.requireNonNull(name, "Field name must not be null");
            booleanFields.put(name, new BooleanFieldState(initialValue));
            return this;
        }

        /**
         * Adds a boolean field with false as the initial value.
         *
         * @param name the field name
         * @return this builder for chaining
         */
        public Builder booleanField(String name) {
            return booleanField(name, false);
        }

        /**
         * Adds a select field with the given options and initial selection.
         *
         * @param name the field name
         * @param options the available options
         * @param selectedIndex the initially selected index (0-based)
         * @return this builder for chaining
         */
        public Builder selectField(String name, List<String> options, int selectedIndex) {
            Objects.requireNonNull(name, "Field name must not be null");
            selectFields.put(name, new SelectFieldState(options, selectedIndex));
            return this;
        }

        /**
         * Adds a select field with the given options, selecting the first option.
         *
         * @param name the field name
         * @param options the available options
         * @return this builder for chaining
         */
        public Builder selectField(String name, List<String> options) {
            return selectField(name, options, 0);
        }

        /**
         * Builds the FormState.
         *
         * @return the constructed FormState
         */
        public FormState build() {
            return new FormState(this);
        }
    }
}
