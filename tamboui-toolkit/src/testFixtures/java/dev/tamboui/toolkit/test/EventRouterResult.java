/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.toolkit.test;

import dev.tamboui.toolkit.event.EventResult;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Wrapper around routing results with fluent assertions for testing.
 * <p>
 * Provides a readable API for verifying event routing behavior:
 *
 * <pre>{@code
 * app.send(KeyEvent.ofKey(KeyCode.TAB))
 *     .assertHandled()
 *     .assertFocusMovedTo("input2");
 *
 * app.send(MouseEvent.press(MouseButton.LEFT, 5, 5))
 *     .assertHandled()
 *     .assertFocusIs("input1");
 * }</pre>
 */
public final class EventRouterResult {

    private final EventResult result;
    private final String focusBefore;
    private final String focusAfter;
    private final TestToolkitApp app;

    EventRouterResult(EventResult result, String focusBefore, String focusAfter, TestToolkitApp app) {
        this.result = result;
        this.focusBefore = focusBefore;
        this.focusAfter = focusAfter;
        this.app = app;
    }

    /**
     * Returns the raw event result.
     *
     * @return the event result
     */
    public EventResult result() {
        return result;
    }

    /**
     * Returns the focused element ID before the event was routed.
     *
     * @return the focused ID before, or null if nothing was focused
     */
    public String focusBefore() {
        return focusBefore;
    }

    /**
     * Returns the focused element ID after the event was routed.
     *
     * @return the focused ID after, or null if nothing is focused
     */
    public String focusAfter() {
        return focusAfter;
    }

    /**
     * Returns the test app for chaining further operations.
     *
     * @return the test app
     */
    public TestToolkitApp app() {
        return app;
    }

    // ═══════════════════════════════════════════════════════════════
    // Fluent assertions
    // ═══════════════════════════════════════════════════════════════

    /**
     * Asserts that the event was handled.
     *
     * @return this for chaining
     */
    public EventRouterResult assertHandled() {
        assertThat(result)
            .as("Expected event to be HANDLED")
            .isEqualTo(EventResult.HANDLED);
        return this;
    }

    /**
     * Asserts that the event was not handled.
     *
     * @return this for chaining
     */
    public EventRouterResult assertUnhandled() {
        assertThat(result)
            .as("Expected event to be UNHANDLED")
            .isEqualTo(EventResult.UNHANDLED);
        return this;
    }

    /**
     * Asserts that focus moved to the specified element.
     *
     * @param expectedId the expected focused element ID
     * @return this for chaining
     */
    public EventRouterResult assertFocusMovedTo(String expectedId) {
        assertThat(focusAfter)
            .as("Expected focus to move to '%s' but focus is '%s' (was '%s' before)",
                expectedId, focusAfter, focusBefore)
            .isEqualTo(expectedId);
        assertThat(focusAfter)
            .as("Expected focus to change, but focus is still '%s'", focusBefore)
            .isNotEqualTo(focusBefore);
        return this;
    }

    /**
     * Asserts that focus is currently on the specified element.
     *
     * @param expectedId the expected focused element ID
     * @return this for chaining
     */
    public EventRouterResult assertFocusIs(String expectedId) {
        assertThat(focusAfter)
            .as("Expected focus to be '%s' but was '%s'", expectedId, focusAfter)
            .isEqualTo(expectedId);
        return this;
    }

    /**
     * Asserts that focus did not change.
     *
     * @return this for chaining
     */
    public EventRouterResult assertFocusUnchanged() {
        assertThat(focusAfter)
            .as("Expected focus to remain '%s' but changed to '%s'", focusBefore, focusAfter)
            .isEqualTo(focusBefore);
        return this;
    }

    /**
     * Asserts that nothing is focused.
     *
     * @return this for chaining
     */
    public EventRouterResult assertNoFocus() {
        assertThat(focusAfter)
            .as("Expected no focus but '%s' is focused", focusAfter)
            .isNull();
        return this;
    }

    /**
     * Asserts that focus was cleared (something was focused before, nothing now).
     *
     * @return this for chaining
     */
    public EventRouterResult assertFocusCleared() {
        assertThat(focusBefore)
            .as("Expected something to have been focused before clearing")
            .isNotNull();
        assertThat(focusAfter)
            .as("Expected focus to be cleared but '%s' is focused", focusAfter)
            .isNull();
        return this;
    }
}
