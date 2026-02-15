/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.tui.trace;

import dev.tamboui.tui.event.Event;

/**
 * Optional tracer for TuiRunner-level events: raw events, handler result, and render start/end.
 * When tracing is enabled, one implementation (e.g. {@link SinkTuiEventTracer}) writes
 * to a {@link TraceSink} so that TUI and toolkit traces share the same stream.
 */
public interface TuiEventTracer {

    /**
     * Called when an event is about to be delivered to the handler.
     *
     * @param event the event
     */
    void traceEvent(Event event);

    /**
     * Called after the handler returns.
     *
     * @param redraw whether the handler requested a redraw
     */
    void traceHandlerResult(boolean redraw);

    /**
     * Called at the start of a render pass.
     */
    void traceRenderStart();

    /**
     * Called at the end of a render pass.
     */
    void traceRenderEnd();

    /**
     * No-op tracer.
     */
    TuiEventTracer NOOP = new TuiEventTracer() {
        @Override
        public void traceEvent(Event event) {
        }

        @Override
        public void traceHandlerResult(boolean redraw) {
        }

        @Override
        public void traceRenderStart() {
        }

        @Override
        public void traceRenderEnd() {
        }
    };
}
