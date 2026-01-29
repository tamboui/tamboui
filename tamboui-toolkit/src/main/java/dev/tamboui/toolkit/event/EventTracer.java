/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.toolkit.event;

import dev.tamboui.tui.event.Event;

/**
 * Interface for tracing event routing decisions.
 * <p>
 * Implementations log detailed information about how events are routed
 * through the element tree, which is useful for debugging focus and
 * event handling issues.
 * <p>
 * Enable tracing by setting the {@code TAMBOUI_EVENT_TRACE} environment
 * variable to a file path:
 * <pre>
 * TAMBOUI_EVENT_TRACE=/tmp/trace.log ./run-demo.sh my-demo
 * </pre>
 * <p>
 * The trace file uses JSON Lines format (each line is a complete JSON object):
 * <pre>
 * {"ts":"2026-01-29T10:00:00.123","type":"route_start","event":"KeyEvent[TAB]","focused":"input1","elements":3}
 * {"ts":"2026-01-29T10:00:00.124","type":"candidate","id":"input1","phase":"focused","decision":"tried"}
 * {"ts":"2026-01-29T10:00:00.125","type":"route_end","event":"KeyEvent[TAB]","result":"HANDLED"}
 * </pre>
 *
 * @see FileEventTracer
 * @see EventTracerFactory
 */
public interface EventTracer extends AutoCloseable {

    /**
     * A no-op tracer that does nothing.
     */
    EventTracer NOOP = new EventTracer() {
        @Override
        public void traceRouteStart(long routeId, Event event, String focusedId, int elementCount) {
        }

        @Override
        public void traceRouteEnd(long routeId, Event event, EventResult result) {
        }

        @Override
        public void traceCandidate(long routeId, String elementId, String elementType, String phase, String decision, String reason) {
        }

        @Override
        public void traceFocusChange(long routeId, String fromId, String toId, String reason) {
        }

        @Override
        public void traceFocusNavigation(long routeId, String action, boolean success, String fromId, String toId) {
        }

        @Override
        public void traceDragState(long routeId, String action, String elementId, int x, int y) {
        }

        @Override
        public void traceGlobalHandler(long routeId, int index, EventResult result) {
        }

        @Override
        public void close() {
        }
    };

    /**
     * Traces the start of event routing.
     *
     * @param routeId      unique ID for this routing operation
     * @param event        the event being routed
     * @param focusedId    the currently focused element ID (may be null)
     * @param elementCount the number of registered elements
     */
    void traceRouteStart(long routeId, Event event, String focusedId, int elementCount);

    /**
     * Traces the end of event routing.
     *
     * @param routeId the routing operation ID
     * @param event   the event that was routed
     * @param result  the routing result
     */
    void traceRouteEnd(long routeId, Event event, EventResult result);

    /**
     * Traces an element being considered for event handling.
     *
     * @param routeId     the routing operation ID
     * @param elementId   the element ID (may be null)
     * @param elementType the element type (class name or style type)
     * @param phase       the routing phase (e.g., "focused", "global", "unfocused")
     * @param decision    the decision (e.g., "tried", "skipped", "handled")
     * @param reason      optional reason for the decision (may be null)
     */
    void traceCandidate(long routeId, String elementId, String elementType, String phase, String decision, String reason);

    /**
     * Traces a focus change.
     *
     * @param routeId the routing operation ID
     * @param fromId  the previous focused element ID (may be null)
     * @param toId    the new focused element ID (may be null)
     * @param reason  the reason for the change (e.g., "Tab navigation", "click")
     */
    void traceFocusChange(long routeId, String fromId, String toId, String reason);

    /**
     * Traces a focus navigation action (Tab/Shift+Tab).
     *
     * @param routeId the routing operation ID
     * @param action  the action (e.g., "focusNext", "focusPrevious")
     * @param success whether the navigation succeeded
     * @param fromId  the element focused before (may be null)
     * @param toId    the element focused after (may be null)
     */
    void traceFocusNavigation(long routeId, String action, boolean success, String fromId, String toId);

    /**
     * Traces a drag state change.
     *
     * @param routeId   the routing operation ID
     * @param action    the action (e.g., "start", "drag", "end", "cancel")
     * @param elementId the element being dragged (may be null for end/cancel)
     * @param x         the x coordinate
     * @param y         the y coordinate
     */
    void traceDragState(long routeId, String action, String elementId, int x, int y);

    /**
     * Traces a global handler invocation.
     *
     * @param routeId the routing operation ID
     * @param index   the handler index
     * @param result  the result of the handler
     */
    void traceGlobalHandler(long routeId, int index, EventResult result);

    /**
     * Closes the tracer and releases any resources.
     */
    @Override
    void close();
}
