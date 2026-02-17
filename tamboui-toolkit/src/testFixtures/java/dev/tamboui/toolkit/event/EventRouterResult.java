/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.toolkit.event;

import dev.tamboui.toolkit.app.EventRouterTestHarness;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Wrapper of the result of one {@link EventRouterTestHarness#send(dev.tamboui.tui.event.Event)} call
 * with fluent assertions for routing and focus.
 *
 * <p>
 * Provides a readable API for verifying event routing behavior:
 *
 * <pre>{@code
 * harness.send(KeyEvent.ofKey(KeyCode.TAB))
 *     .assertHandled()
 *     .assertFocusMovedTo("input2");
 *
 * harness.send(MouseEvent.press(MouseButton.LEFT, 5, 5))
 *     .assertHandled()
 *     .assertFocusIs("input1");
 * }</pre>
 */
public final class EventRouterResult {

    private final EventResult result;
    private final String focusBefore;
    private final String focusAfter;
    private final EventRouterTestHarness harness;

    /**
     * Creates a result wrapper.
     *
     * @param result      the routing result
     * @param focusBefore focused element id before route, or null
     * @param focusAfter  focused element id after route, or null
     * @param harness     the test harness (for further sends or assertions)
     */
    public EventRouterResult(EventResult result, String focusBefore, String focusAfter,
                             EventRouterTestHarness harness) {
        this.result = result;
        this.focusBefore = focusBefore;
        this.focusAfter = focusAfter;
        this.harness = harness;
    }

    /**
     * Returns the routing result.
     *
     * @return HANDLED or UNHANDLED
     */
    public EventResult result() {
        return result;
    }

    /**
     * Returns the focused element id before the route.
     *
     * @return id or null
     */
    public String focusBefore() {
        return focusBefore;
    }

    /**
     * Returns the focused element id after the route.
     *
     * @return id or null
     */
    public String focusAfter() {
        return focusAfter;
    }

    /**
     * Returns the test harness for further sends.
     *
     * @return the EventRouterTestHarness
     */
    public EventRouterTestHarness harness() {
        return harness;
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
