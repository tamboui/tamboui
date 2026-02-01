/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.toolkit.event;

import dev.tamboui.layout.Rect;
import dev.tamboui.toolkit.element.Element;
import dev.tamboui.toolkit.element.ElementRegistry;
import dev.tamboui.toolkit.elements.FormFieldElement;
import dev.tamboui.toolkit.focus.FocusManager;
import dev.tamboui.tui.event.KeyCode;
import dev.tamboui.tui.event.KeyEvent;
import dev.tamboui.widgets.form.SelectFieldState;
import dev.tamboui.widgets.input.TextInputState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static dev.tamboui.toolkit.Toolkit.formField;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for EventRouter focus navigation.
 */
class EventRouterTest {

    private FocusManager focusManager;
    private ElementRegistry elementRegistry;
    private EventRouter router;

    @BeforeEach
    void setUp() {
        focusManager = new FocusManager();
        elementRegistry = new ElementRegistry();
        router = new EventRouter(focusManager, elementRegistry);
    }

    private void registerElement(Element element, Rect area) {
        router.registerElement(element, area);
        if (element.isFocusable() && element.id() != null) {
            focusManager.registerFocusable(element.id(), area);
        }
    }

    private KeyEvent downKey() {
        return KeyEvent.ofKey(KeyCode.DOWN);
    }

    private KeyEvent upKey() {
        return KeyEvent.ofKey(KeyCode.UP);
    }

    @Test
    @DisplayName("down key navigates to next focusable element when text field is focused with arrowNavigation")
    void downKeyNavigatesToNextField() {
        // Create two text fields with arrow navigation enabled
        TextInputState state1 = new TextInputState("Field 1");
        TextInputState state2 = new TextInputState("Field 2");

        FormFieldElement field1 = formField("Name", state1).id("field1").arrowNavigation(true);
        FormFieldElement field2 = formField("Email", state2).id("field2").arrowNavigation(true);

        registerElement(field1, new Rect(0, 0, 30, 1));
        registerElement(field2, new Rect(0, 1, 30, 1));

        // Focus should be on field1 (first registered)
        assertThat(focusManager.focusedId()).isEqualTo("field1");

        // Press down - should navigate to field2
        EventResult result = router.route(downKey());

        assertThat(result).isEqualTo(EventResult.HANDLED);
        assertThat(focusManager.focusedId()).isEqualTo("field2");
    }

    @Test
    @DisplayName("up key navigates to previous focusable element when text field is focused with arrowNavigation")
    void upKeyNavigatesToPreviousField() {
        // Create two text fields with arrow navigation enabled
        TextInputState state1 = new TextInputState("Field 1");
        TextInputState state2 = new TextInputState("Field 2");

        FormFieldElement field1 = formField("Name", state1).id("field1").arrowNavigation(true);
        FormFieldElement field2 = formField("Email", state2).id("field2").arrowNavigation(true);

        registerElement(field1, new Rect(0, 0, 30, 1));
        registerElement(field2, new Rect(0, 1, 30, 1));

        // Focus field2
        focusManager.setFocus("field2");
        assertThat(focusManager.focusedId()).isEqualTo("field2");

        // Press up - should navigate to field1
        EventResult result = router.route(upKey());

        assertThat(result).isEqualTo(EventResult.HANDLED);
        assertThat(focusManager.focusedId()).isEqualTo("field1");
    }

    @Test
    @DisplayName("select field handles down key for selection, not navigation")
    void selectFieldHandlesDownKeyForSelection() {
        // Create a select field
        SelectFieldState selectState = new SelectFieldState(Arrays.asList("Option A", "Option B", "Option C"), 0);
        FormFieldElement selectField = formField("Choice", selectState).id("select");

        // Create another field after it
        TextInputState textState = new TextInputState("");
        FormFieldElement textField = formField("Name", textState).id("text");

        registerElement(selectField, new Rect(0, 0, 30, 1));
        registerElement(textField, new Rect(0, 1, 30, 1));

        // Focus should be on select (first registered)
        assertThat(focusManager.focusedId()).isEqualTo("select");
        assertThat(selectState.selectedIndex()).isEqualTo(0);

        // Press down - should change selection, NOT navigate
        EventResult result = router.route(downKey());

        assertThat(result).isEqualTo(EventResult.HANDLED);
        // Still focused on select
        assertThat(focusManager.focusedId()).isEqualTo("select");
        // Selection changed to next option
        assertThat(selectState.selectedIndex()).isEqualTo(1);
    }

    @Test
    @DisplayName("text field with arrowNavigation does not affect select field when pressing down")
    void textFieldDoesNotAffectSelectOnDown() {
        // Create text field first (with arrow navigation), then select
        TextInputState textState = new TextInputState("Hello");
        FormFieldElement textField = formField("Name", textState).id("text").arrowNavigation(true);

        SelectFieldState selectState = new SelectFieldState(Arrays.asList("Option A", "Option B", "Option C"), 0);
        FormFieldElement selectField = formField("Choice", selectState).id("select");

        registerElement(textField, new Rect(0, 0, 30, 1));
        registerElement(selectField, new Rect(0, 1, 30, 1));

        // Focus on text field
        assertThat(focusManager.focusedId()).isEqualTo("text");
        assertThat(selectState.selectedIndex()).isEqualTo(0);

        // Press down - should navigate to select, NOT change select's value
        EventResult result = router.route(downKey());

        assertThat(result).isEqualTo(EventResult.HANDLED);
        // Focus moved to select
        assertThat(focusManager.focusedId()).isEqualTo("select");
        // But select's value should NOT have changed
        assertThat(selectState.selectedIndex()).isEqualTo(0);
    }

    @Test
    @DisplayName("down key wraps around to first field with arrowNavigation")
    void downKeyWrapsAround() {
        TextInputState state1 = new TextInputState("Field 1");
        TextInputState state2 = new TextInputState("Field 2");

        FormFieldElement field1 = formField("Name", state1).id("field1").arrowNavigation(true);
        FormFieldElement field2 = formField("Email", state2).id("field2").arrowNavigation(true);

        registerElement(field1, new Rect(0, 0, 30, 1));
        registerElement(field2, new Rect(0, 1, 30, 1));

        // Focus last field
        focusManager.setFocus("field2");

        // Press down - should wrap to field1
        router.route(downKey());

        assertThat(focusManager.focusedId()).isEqualTo("field1");
    }

    @Test
    @DisplayName("up key wraps around to last field with arrowNavigation")
    void upKeyWrapsAround() {
        TextInputState state1 = new TextInputState("Field 1");
        TextInputState state2 = new TextInputState("Field 2");

        FormFieldElement field1 = formField("Name", state1).id("field1").arrowNavigation(true);
        FormFieldElement field2 = formField("Email", state2).id("field2").arrowNavigation(true);

        registerElement(field1, new Rect(0, 0, 30, 1));
        registerElement(field2, new Rect(0, 1, 30, 1));

        // Focus first field (already the default)
        assertThat(focusManager.focusedId()).isEqualTo("field1");

        // Press up - should wrap to field2
        router.route(upKey());

        assertThat(focusManager.focusedId()).isEqualTo("field2");
    }

    @Test
    @DisplayName("text field without arrowNavigation does not navigate on down key")
    void textFieldWithoutArrowNavigationDoesNotNavigate() {
        TextInputState state1 = new TextInputState("Field 1");
        TextInputState state2 = new TextInputState("Field 2");

        // arrowNavigation is false by default
        FormFieldElement field1 = formField("Name", state1).id("field1");
        FormFieldElement field2 = formField("Email", state2).id("field2");

        registerElement(field1, new Rect(0, 0, 30, 1));
        registerElement(field2, new Rect(0, 1, 30, 1));

        // Focus should be on field1
        assertThat(focusManager.focusedId()).isEqualTo("field1");

        // Press down - should NOT navigate (arrowNavigation is off)
        EventResult result = router.route(downKey());

        // Event is not handled by the text field
        assertThat(result).isEqualTo(EventResult.UNHANDLED);
        // Focus stays on field1
        assertThat(focusManager.focusedId()).isEqualTo("field1");
    }
}
