/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.tui.trace;

import java.util.concurrent.atomic.AtomicLong;

import dev.tamboui.tui.event.Event;
import dev.tamboui.tui.event.KeyEvent;
import dev.tamboui.tui.event.MouseEvent;
import dev.tamboui.tui.event.ResizeEvent;
import dev.tamboui.tui.event.TickEvent;
import dev.tamboui.tui.event.UiRunnable;

/**
 * TuiEventTracer that writes to a {@link TraceSink} with record types:
 * {@code event}, {@code handler_result}, {@code render_start}, {@code render_end}.
 */
public final class SinkTuiEventTracer implements TuiEventTracer {

    private final TraceSink sink;
    private final AtomicLong rid = new AtomicLong(0);

    /**
     * Creates a tracer that writes to the given sink.
     *
     * @param sink the trace sink
     */
    public SinkTuiEventTracer(TraceSink sink) {
        this.sink = sink;
    }

    @Override
    public void traceEvent(Event event) {
        long r = rid.incrementAndGet();
        String type = null;
        String payload = null;

        if (event instanceof KeyEvent) {
            KeyEvent e = (KeyEvent) event;
            type = "key_event";
            payload =   String.format("{ \"code\": \"%s\", \"char\": \"%s\", \"mods\": \"%s\" }", e.code(), e.character(), e.modifiers());
        }
        if (event instanceof MouseEvent) {
            MouseEvent e = (MouseEvent) event;  
            type = "mouse_event";
            payload =String.format("{ \"kind\": \"%s\", \"x\": %d, \"y\": %d, \"button\": \"%s\" }", e.kind(), e.x(), e.y(), e.button());
        }
        if (event instanceof ResizeEvent) {
            ResizeEvent e = (ResizeEvent) event;
            type = "resize_event";
            payload = String.format("{ \"width\": %d, \"height\": %d }", e.width(), e.height());
        }
        if (event instanceof TickEvent) {
            type = "tick_event";
            payload = null;
            return; // too noisy to trace this
        }
        if (event instanceof UiRunnable) {
            type = "ui_runnable";
            payload = null;
        }
        
        if(type==null) {
            type = event.getClass().getSimpleName();
        }
        sink.write(r, type, payload);
    }

    @Override
    public void traceHandlerResult(boolean redraw) {
        //long r = rid.incrementAndGet();
       // sink.write(r, "handler_result", String.format("{ \"redraw\": %b }", redraw));
    }

    @Override
    public void traceRenderStart() {
       // long r = rid.incrementAndGet();
        //sink.write(r, "render_start", null);
    }

    @Override
    public void traceRenderEnd() {
        //long r = rid.incrementAndGet();
       // sink.write(r, "render_end", null);
    }
}
