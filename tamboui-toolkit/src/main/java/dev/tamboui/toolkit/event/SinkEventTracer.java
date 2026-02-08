/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.toolkit.event;


import dev.tamboui.tui.event.Event;
import dev.tamboui.tui.trace.TraceSink;

/**
 * EventTracer that writes to a {@link TraceSink} with record types:
 * {@code route_start}, {@code route_end}, {@code candidate}, {@code focus_change},
 * {@code focus_nav}, {@code drag}, {@code global_handler}.
 * <p>
 * Use the same sink instance as {@link dev.tamboui.tui.trace.SinkTuiEventTracer}
 * so that one file contains both TUI-level and toolkit-level traces.
 */
public final class SinkEventTracer implements EventTracer {

    private final TraceSink sink;

    /**
     * Creates a tracer that writes to the given sink.
     *
     * @param sink the trace sink (e.g. shared with TuiRunner's tracer)
     */
    public SinkEventTracer(TraceSink sink) {
        this.sink = sink;
    }

    @Override
    public void traceRouteStart(long routeId, Event event, String focusedId, int elementCount) {
        sink.write(routeId, "route_start", String.format("{ \"event\": %s, \"focused\": %s, \"elements\": %d }", jsonString(event.toString()), jsonString(focusedId), elementCount));
    }

    @Override
    public void traceRouteEnd(long routeId, Event event, EventResult result ) {
        sink.write(routeId, "route_end", String.format("{ \"event\": %s, \"result\": %s }", jsonString(event.toString()), jsonString(result.toString())));
    }

    @Override
    public void traceCandidate(long routeId, String elementId, String elementType, String phase, String decision, String reason) {
        if(reason != null) {
            sink.write(routeId, "candidate", String.format("{ \"elementId\": %s, \"elementType\": %s, \"phase\": %s, \"decision\": %s, \"reason\": %s }", jsonString(elementId), jsonString(elementType), jsonString(phase), jsonString(decision), jsonString(reason)));
        } else {
            sink.write(routeId, "candidate", String.format("{ \"elementId\": %s, \"elementType\": %s, \"phase\": %s, \"decision\": %s }", jsonString(elementId), jsonString(elementType), jsonString(phase), jsonString(decision)));
        }
      }

    @Override
    public void traceFocusChange(long routeId, String fromId, String toId, String reason) {
        sink.write(routeId, "focus_change",
                String.format("{ \"from\": %s, \"to\": %s, \"reason\": %s }", jsonString(fromId), jsonString(toId), jsonString(reason)));
    }

    @Override
    public void traceFocusNavigation(long routeId, String action, boolean success, String fromId, String toId) {
        sink.write(routeId, "focus_nav",
                String.format("{ \"action\": %s, \"success\": %b, \"from\": %s, \"to\": %s }", jsonString(action), success, jsonString(fromId), jsonString(toId)));
    }

    @Override
    public void traceDragState(long routeId, String action, String elementId, int x, int y) {
        sink.write(routeId, "drag",
                String.format("{ \"action\": %s, \"element\": %s, \"x\": %d, \"y\": %d }", jsonString(action), jsonString(elementId), x, y));
    }

    @Override
    public void traceGlobalHandler(long routeId, int index, EventResult result) {
        sink.write(routeId, "global_handler", String.format("{ \"index\": %d, \"result\": %s }", index, jsonString(result.toString())));
    }

    @Override
    public void close() {
        // Sink is shared (e.g. with TuiRunner's tracer); caller retains ownership and closes it
    }

    private static String jsonString(String value) {
        if (value == null) {
            return "null";
        }
        return "\"" + escape(value) + "\"";
    }

    private static String escape(String value) {
        if (value == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '"':
                    sb.append("\\\"");
                    break;
                case '\\':
                    sb.append("\\\\");
                    break;
                case '\n':
                    sb.append("\\n");
                    break;
                case '\r':
                    sb.append("\\r");
                    break;
                case '\t':
                    sb.append("\\t");
                    break;
                default:
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
            }
        }
        return sb.toString();
    }
}
