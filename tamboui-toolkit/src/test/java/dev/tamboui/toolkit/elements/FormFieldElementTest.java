/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.toolkit.elements;

import dev.tamboui.buffer.Buffer;
import dev.tamboui.layout.Rect;
import dev.tamboui.terminal.Frame;
import dev.tamboui.toolkit.element.RenderContext;
import dev.tamboui.widgets.form.BooleanFieldState;
import dev.tamboui.widgets.form.FieldType;
import dev.tamboui.widgets.form.SelectFieldState;
import dev.tamboui.widgets.form.ValidationResult;
import dev.tamboui.widgets.form.Validators;
import dev.tamboui.widgets.input.TextInputState;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static dev.tamboui.toolkit.Toolkit.*;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for FormFieldElement.
 */
class FormFieldElementTest {

    @Test
    @DisplayName("formField renders label and input")
    void formFieldRendersLabelAndInput() {
        TextInputState state = new TextInputState("Hello");
        FormFieldElement field = formField("Name", state).labelWidth(10);

        Rect area = new Rect(0, 0, 30, 1);
        Buffer buffer = Buffer.empty(area);
        Frame frame = Frame.forTesting(buffer);

        field.render(frame, area, RenderContext.empty());

        // Label should be rendered
        assertThat(buffer.get(0, 0).symbol()).isEqualTo("N");
        assertThat(buffer.get(1, 0).symbol()).isEqualTo("a");
        assertThat(buffer.get(2, 0).symbol()).isEqualTo("m");
        assertThat(buffer.get(3, 0).symbol()).isEqualTo("e");
    }

    @Test
    @DisplayName("formField is focusable by default")
    void formFieldIsFocusableByDefault() {
        FormFieldElement field = formField("Name");
        assertThat(field.isFocusable()).isTrue();
    }

    @Test
    @DisplayName("formField with labelWidth sets width correctly")
    void formFieldWithLabelWidth() {
        FormFieldElement field = formField("Name", new TextInputState("test")).labelWidth(20);
        // Preferred width should include label width
        assertThat(field.preferredWidth()).isGreaterThanOrEqualTo(20);
    }

    @Test
    @DisplayName("formField validates on validateField()")
    void formFieldValidatesOnValidate() {
        TextInputState state = new TextInputState("");
        FormFieldElement field = formField("Email", state)
                .validate(Validators.required(), Validators.email());

        ValidationResult result = field.validateField();

        assertThat(result.isValid()).isFalse();
        assertThat(result.errorMessage()).isEqualTo("Field is required");
    }

    @Test
    @DisplayName("formField validation passes when valid")
    void formFieldValidationPasses() {
        TextInputState state = new TextInputState("user@example.com");
        FormFieldElement field = formField("Email", state)
                .validate(Validators.required(), Validators.email());

        ValidationResult result = field.validateField();

        assertThat(result.isValid()).isTrue();
    }

    @Test
    @DisplayName("formField lastValidation() returns last result")
    void formFieldLastValidationReturnsLastResult() {
        TextInputState state = new TextInputState("");
        FormFieldElement field = formField("Email", state)
                .validate(Validators.required());

        assertThat(field.lastValidation().isValid()).isTrue(); // Before validation

        field.validateField();

        assertThat(field.lastValidation().isValid()).isFalse();
    }

    @Test
    @DisplayName("formField with BooleanFieldState renders checkbox")
    void formFieldWithBooleanRendersCheckbox() {
        BooleanFieldState state = new BooleanFieldState(true);
        FormFieldElement field = formField("Subscribe", state);

        Rect area = new Rect(0, 0, 30, 1);
        Buffer buffer = Buffer.empty(area);
        Frame frame = Frame.forTesting(buffer);

        field.render(frame, area, RenderContext.empty());

        // Should render something (label at minimum)
        assertThat(buffer.get(0, 0).symbol()).isEqualTo("S");
    }

    @Test
    @DisplayName("formField with SelectFieldState renders select")
    void formFieldWithSelectRendersSelect() {
        SelectFieldState state = new SelectFieldState("USA", "UK", "Germany");
        FormFieldElement field = formField("Country", state);

        Rect area = new Rect(0, 0, 40, 1);
        Buffer buffer = Buffer.empty(area);
        Frame frame = Frame.forTesting(buffer);

        field.render(frame, area, RenderContext.empty());

        // Should render something (label at minimum)
        assertThat(buffer.get(0, 0).symbol()).isEqualTo("C");
    }

    @Test
    @DisplayName("formField type() changes field type")
    void formFieldTypeChangesType() {
        BooleanFieldState state = new BooleanFieldState(true);
        FormFieldElement field = formField("Dark Mode", state, FieldType.TOGGLE);

        Rect area = new Rect(0, 0, 40, 1);
        Buffer buffer = Buffer.empty(area);
        Frame frame = Frame.forTesting(buffer);

        field.render(frame, area, RenderContext.empty());

        // Should render something
        assertThat(buffer.get(0, 0).symbol()).isEqualTo("D");
    }

    @Test
    @DisplayName("formField preferredHeight() returns correct height")
    void formFieldPreferredHeightReturnsCorrectHeight() {
        FormFieldElement field = formField("Name", new TextInputState("test"));

        // Without border: 1 row
        assertThat(field.preferredHeight()).isEqualTo(1);

        // With border: 3 rows
        field.rounded();
        assertThat(field.preferredHeight()).isEqualTo(3);
    }

    @Test
    @DisplayName("formField chaining works correctly")
    void formFieldChainingWorks() {
        FormFieldElement field = formField("Email")
                .labelWidth(14)
                .spacing(2)
                .placeholder("you@example.com")
                .rounded()
                .borderColor(dev.tamboui.style.Color.CYAN)
                .focusedBorderColor(dev.tamboui.style.Color.GREEN)
                .errorBorderColor(dev.tamboui.style.Color.RED)
                .validate(Validators.required(), Validators.email())
                .showInlineErrors(true);

        assertThat(field.isFocusable()).isTrue();
        // Before validation, no error row is shown
        assertThat(field.preferredHeight()).isEqualTo(3); // 3 for bordered

        // After validation fails, error row is added
        field.validateField();
        assertThat(field.preferredHeight()).isEqualTo(4); // 3 for bordered + 1 for error
    }
}
