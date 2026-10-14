/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.toolkit.elements;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import dev.tamboui.buffer.Buffer;
import dev.tamboui.layout.Rect;
import dev.tamboui.terminal.Frame;
import dev.tamboui.toolkit.AbstractElementTest;
import dev.tamboui.toolkit.element.RenderContext;
import dev.tamboui.widgets.form.*;

import static dev.tamboui.toolkit.Toolkit.form;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for FormElement.
 */
class FormElementTest extends AbstractElementTest {

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

        assertThat(form.preferredSize(-1, -1, null).widthOr(0)).isGreaterThanOrEqualTo(15);
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
        assertThat(form.preferredSize(-1, -1, null).heightOr(0)).isGreaterThanOrEqualTo(2);
    }

    @Test
    @DisplayName("preferredHeight() accounts for borders")
    void preferredHeightAccountsForBorders() {
        FormState state = FormState.builder()
                .textField("name", "John")
                .build();

        FormElement plainForm = form(state).field("name", "Name");
        FormElement borderedForm = form(state).field("name", "Name").rounded();

        assertThat(borderedForm.preferredSize(-1, -1, null).heightOr(0)).isGreaterThan(plainForm.preferredSize(-1, -1, null).heightOr(0));
    }

    // ==================== TextArea Field Tests ====================

    @Test
    @DisplayName("field() with TEXT_AREA adds TextArea field to form")
    void fieldWithTextAreaAddsField() {
        FormState state = FormState.builder()
                .textAreaField("bio", "Software Engineer")
                .build();

        FormElement form = form(state)
                .field("bio", "Bio", FieldType.TEXT_AREA);

        assertThat(form.validateAll()).isTrue();
    }

    @Test
    @DisplayName("field() with TEXT_AREA and rows accounts for height in preferredSize")
    void fieldWithTextAreaRowsAccountsForHeight() {
        FormState state = FormState.builder()
                .textField("name", "John")
                .textAreaField("bio", "Line 1\nLine 2")
                .build();

        FormElement form = form(state)
                .field("name", "Name")
                .field("bio", "Bio", FieldType.TEXT_AREA, 5)
                .fieldSpacing(1);

        // name: 1 row, spacing: 1 row, bio: 5 rows => total 7 rows
        assertThat(form.preferredSize(-1, -1, null).heightOr(0)).isEqualTo(7);

        // Bordered: name = 3 rows, spacing = 1 row, bio = 5 + 2 = 7 rows => total 11 rows
        form.rounded();
        assertThat(form.preferredSize(-1, -1, null).heightOr(0)).isEqualTo(11);
    }

    @Test
    @DisplayName("field() with TEXT_AREA validates properly")
    void fieldWithTextAreaValidatesProperly() {
        FormState state = FormState.builder()
                .textAreaField("comments", "")
                .build();

        FormElement form = form(state)
                .field("comments", "Comments", FieldType.TEXT_AREA, Validators.required());

        assertThat(form.validateAll()).isFalse();
        assertThat(form.validationErrors()).containsKey("comments");

        state.setTextAreaValue("comments", "All good");
        assertThat(form.validateAll()).isTrue();
        assertThat(form.validationErrors()).isEmpty();
    }

    @Test
    @DisplayName("FormElement renders TEXT_AREA field to buffer")
    void formElementRendersTextArea() {
        FormState state = FormState.builder()
                .textField("name", "Alice")
                .textAreaField("bio", "Line1\nLine2")
                .build();

        FormElement form = form(state)
                .labelWidth(10)
                .field("name", "Name")
                .field("bio", "Bio", FieldType.TEXT_AREA, 2);

        Rect area = new Rect(0, 0, 40, 6);
        Buffer buffer = Buffer.empty(area);
        Frame frame = Frame.forTesting(buffer);

        form.render(frame, area, RenderContext.empty());

        // Name field at row 0
        assertThat(buffer.get(0, 0).symbol()).isEqualTo("N");
        // Bio label at row 2 (row 0: name, row 1: spacing, row 2: bio)
        assertThat(buffer.get(0, 2).symbol()).isEqualTo("B");
        // Bio content at row 2, column 11
        assertThat(buffer.get(11, 2).symbol()).isEqualTo("L");
        // Second line of bio at row 3, column 11
        assertThat(buffer.get(11, 3).symbol()).isEqualTo("L");
    }
}
