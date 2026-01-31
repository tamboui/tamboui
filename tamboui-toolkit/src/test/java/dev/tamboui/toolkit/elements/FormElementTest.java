/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.toolkit.elements;

import dev.tamboui.widgets.form.FieldType;
import dev.tamboui.widgets.form.FormState;
import dev.tamboui.widgets.form.ValidationResult;
import dev.tamboui.widgets.form.Validator;
import dev.tamboui.widgets.form.Validators;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static dev.tamboui.toolkit.Toolkit.*;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for FormElement.
 */
class FormElementTest {

    // ==================== Basic Form Building ====================

    @Test
    @DisplayName("form() creates form with FormState")
    void formCreatesWithState() {
        FormState state = FormState.builder()
                .textField("name", "John")
                .build();

        FormElement form = form(state);

        assertThat(form.formState()).isSameAs(state);
    }

    @Test
    @DisplayName("field() adds text field to form")
    void fieldAddsTextField() {
        FormState state = FormState.builder()
                .textField("email", "test@example.com")
                .build();

        FormElement form = form(state)
                .field("email", "Email");

        // Form should have field configured (validated via validateAll)
        assertThat(form.validateAll()).isTrue();
    }

    @Test
    @DisplayName("field() with validators configures validation")
    void fieldWithValidatorsConfiguresValidation() {
        FormState state = FormState.builder()
                .textField("email", "")
                .build();

        FormElement form = form(state)
                .field("email", "Email", Validators.required());

        assertThat(form.validateAll()).isFalse();
        assertThat(form.validationErrors()).containsKey("email");
    }

    @Test
    @DisplayName("field() with FieldType configures type")
    void fieldWithTypeConfiguresType() {
        FormState state = FormState.builder()
                .booleanField("subscribe", true)
                .build();

        FormElement form = form(state)
                .field("subscribe", "Subscribe", FieldType.CHECKBOX);

        assertThat(form.validateAll()).isTrue();
    }

    // ==================== Grouping ====================

    @Test
    @DisplayName("group() creates field groups")
    void groupCreatesFieldGroups() {
        FormState state = FormState.builder()
                .textField("name", "John")
                .textField("email", "john@example.com")
                .booleanField("newsletter", false)
                .build();

        FormElement form = form(state)
                .group("Profile")
                    .field("name", "Name")
                    .field("email", "Email")
                .group("Preferences")
                    .field("newsletter", "Newsletter", FieldType.CHECKBOX);

        // Groups should not affect validation
        assertThat(form.validateAll()).isTrue();
    }

    @Test
    @DisplayName("endGroup() ends current group")
    void endGroupEndsCurrentGroup() {
        FormState state = FormState.builder()
                .textField("name", "John")
                .textField("other", "value")
                .build();

        FormElement form = form(state)
                .group("Profile")
                    .field("name", "Name")
                .endGroup()
                .field("other", "Other");

        assertThat(form.validateAll()).isTrue();
    }

    // ==================== Validation ====================

    @Test
    @DisplayName("validateAll() returns true when all fields valid")
    void validateAllReturnsTrueWhenValid() {
        FormState state = FormState.builder()
                .textField("name", "John")
                .textField("email", "john@example.com")
                .build();

        FormElement form = form(state)
                .field("name", "Name", Validators.required())
                .field("email", "Email", Validators.required(), Validators.email());

        assertThat(form.validateAll()).isTrue();
    }

    @Test
    @DisplayName("validateAll() returns false when any field invalid")
    void validateAllReturnsFalseWhenInvalid() {
        FormState state = FormState.builder()
                .textField("name", "")
                .textField("email", "not-an-email")
                .build();

        FormElement form = form(state)
                .field("name", "Name", Validators.required())
                .field("email", "Email", Validators.email());

        assertThat(form.validateAll()).isFalse();
    }

    @Test
    @DisplayName("validateAll() stores results in FormState")
    void validateAllStoresResultsInFormState() {
        FormState state = FormState.builder()
                .textField("name", "")
                .textField("email", "valid@example.com")
                .build();

        FormElement form = form(state)
                .field("name", "Name", Validators.required())
                .field("email", "Email", Validators.email());

        form.validateAll();

        assertThat(state.validationResult("name").isValid()).isFalse();
        assertThat(state.validationResult("email").isValid()).isTrue();
    }

    @Test
    @DisplayName("validationErrors() returns map of errors")
    void validationErrorsReturnsMap() {
        FormState state = FormState.builder()
                .textField("name", "")
                .textField("email", "invalid")
                .build();

        FormElement form = form(state)
                .field("name", "Name", Validators.required())
                .field("email", "Email", Validators.email());

        Map<String, String> errors = form.validationErrors();

        assertThat(errors).hasSize(2);
        assertThat(errors).containsKey("name");
        assertThat(errors).containsKey("email");
    }

    @Test
    @DisplayName("validationErrors() returns empty map when valid")
    void validationErrorsReturnsEmptyWhenValid() {
        FormState state = FormState.builder()
                .textField("name", "John")
                .build();

        FormElement form = form(state)
                .field("name", "Name", Validators.required());

        Map<String, String> errors = form.validationErrors();

        assertThat(errors).isEmpty();
    }

    // ==================== Submission ====================

    @Test
    @DisplayName("submit() calls onSubmit callback")
    void submitCallsOnSubmitCallback() {
        FormState state = FormState.builder()
                .textField("name", "John")
                .build();

        AtomicBoolean submitted = new AtomicBoolean(false);
        AtomicReference<FormState> receivedState = new AtomicReference<>();

        FormElement form = form(state)
                .field("name", "Name")
                .onSubmit(s -> {
                    submitted.set(true);
                    receivedState.set(s);
                });

        boolean result = form.submit();

        assertThat(result).isTrue();
        assertThat(submitted.get()).isTrue();
        assertThat(receivedState.get()).isSameAs(state);
    }

    @Test
    @DisplayName("submit() validates before calling callback by default")
    void submitValidatesBeforeCallback() {
        FormState state = FormState.builder()
                .textField("name", "")
                .build();

        AtomicBoolean submitted = new AtomicBoolean(false);

        FormElement form = form(state)
                .field("name", "Name", Validators.required())
                .onSubmit(s -> submitted.set(true));

        boolean result = form.submit();

        assertThat(result).isFalse();
        assertThat(submitted.get()).isFalse();
    }

    @Test
    @DisplayName("submit() skips validation when validateOnSubmit is false")
    void submitSkipsValidationWhenDisabled() {
        FormState state = FormState.builder()
                .textField("name", "")
                .build();

        AtomicBoolean submitted = new AtomicBoolean(false);

        FormElement form = form(state)
                .field("name", "Name", Validators.required())
                .validateOnSubmit(false)
                .onSubmit(s -> submitted.set(true));

        boolean result = form.submit();

        assertThat(result).isTrue();
        assertThat(submitted.get()).isTrue();
    }

    @Test
    @DisplayName("submit() returns true even without callback")
    void submitReturnsTrueWithoutCallback() {
        FormState state = FormState.builder()
                .textField("name", "John")
                .build();

        FormElement form = form(state)
                .field("name", "Name");

        boolean result = form.submit();

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("submitOnEnter() configures enter key submission")
    void submitOnEnterConfiguresEnterKey() {
        FormState state = FormState.builder()
                .textField("name", "John")
                .build();

        FormElement form = form(state)
                .field("name", "Name")
                .submitOnEnter(true);

        assertThat(form.isSubmitOnEnter()).isTrue();
    }

    @Test
    @DisplayName("submitOnEnter() is false by default")
    void submitOnEnterIsFalseByDefault() {
        FormState state = FormState.builder()
                .textField("name", "John")
                .build();

        FormElement form = form(state);

        assertThat(form.isSubmitOnEnter()).isFalse();
    }

    // ==================== Styling ====================

    @Test
    @DisplayName("styling methods chain correctly")
    void stylingMethodsChain() {
        FormState state = FormState.builder()
                .textField("name", "John")
                .build();

        FormElement form = form(state)
                .field("name", "Name")
                .labelWidth(20)
                .spacing(2)
                .fieldSpacing(1)
                .rounded()
                .borderColor(dev.tamboui.style.Color.CYAN)
                .focusedBorderColor(dev.tamboui.style.Color.GREEN)
                .errorBorderColor(dev.tamboui.style.Color.RED)
                .showInlineErrors(true);

        // Should not throw
        assertThat(form.validateAll()).isTrue();
    }

    // ==================== Field Types ====================

    @Test
    @DisplayName("form handles all field types")
    void formHandlesAllFieldTypes() {
        FormState state = FormState.builder()
                .textField("name", "John")
                .booleanField("subscribe", true)
                .selectField("country", Arrays.asList("USA", "UK"), 0)
                .build();

        FormElement form = form(state)
                .field("name", "Name")
                .field("subscribe", "Subscribe", FieldType.CHECKBOX)
                .field("country", "Country", FieldType.SELECT);

        assertThat(form.validateAll()).isTrue();
    }

    @Test
    @DisplayName("form validates boolean fields")
    void formValidatesBooleanFields() {
        FormState state = FormState.builder()
                .booleanField("terms", false)
                .build();

        // Custom validator: must accept terms (value must be "true")
        Validator termsValidator = v -> "true".equals(v)
                ? ValidationResult.valid()
                : ValidationResult.invalid("You must accept the terms");

        FormElement form = form(state)
                .field("terms", "Accept Terms", FieldType.CHECKBOX, termsValidator);

        assertThat(form.validateAll()).isFalse();

        state.setBooleanValue("terms", true);
        assertThat(form.validateAll()).isTrue();
    }

    @Test
    @DisplayName("form validates select fields")
    void formValidatesSelectFields() {
        FormState state = FormState.builder()
                .selectField("plan", Arrays.asList("Free", "Pro", "Enterprise"), 0)
                .build();

        // Custom validator: Free plan not allowed
        Validator planValidator = v -> !"Free".equals(v)
                ? ValidationResult.valid()
                : ValidationResult.invalid("Free plan not allowed");

        FormElement form = form(state)
                .field("plan", "Plan", FieldType.SELECT, planValidator);

        assertThat(form.validateAll()).isFalse();

        state.selectIndex("plan", 1); // Select "Pro"
        assertThat(form.validateAll()).isTrue();
    }

    // ==================== Sizing ====================

    @Test
    @DisplayName("preferredWidth() returns reasonable value")
    void preferredWidthReturnsReasonableValue() {
        FormState state = FormState.builder()
                .textField("name", "John")
                .build();

        FormElement form = form(state)
                .field("name", "Name")
                .labelWidth(15);

        assertThat(form.preferredWidth()).isGreaterThanOrEqualTo(15);
    }

    @Test
    @DisplayName("preferredHeight() accounts for fields")
    void preferredHeightAccountsForFields() {
        FormState state = FormState.builder()
                .textField("name", "John")
                .textField("email", "john@example.com")
                .build();

        FormElement form = form(state)
                .field("name", "Name")
                .field("email", "Email");

        // At minimum, 1 row per field
        assertThat(form.preferredHeight()).isGreaterThanOrEqualTo(2);
    }

    @Test
    @DisplayName("preferredHeight() accounts for borders")
    void preferredHeightAccountsForBorders() {
        FormState state = FormState.builder()
                .textField("name", "John")
                .build();

        FormElement plainForm = form(state).field("name", "Name");
        FormElement borderedForm = form(state).field("name", "Name").rounded();

        assertThat(borderedForm.preferredHeight()).isGreaterThan(plainForm.preferredHeight());
    }
}
