/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.toolkit.jfr;

import jdk.jfr.Category;
import jdk.jfr.Description;
import jdk.jfr.Event;
import jdk.jfr.EventType;
import jdk.jfr.Label;
import jdk.jfr.Name;

/**
 * JFR event emitted for drag state transitions (start/drag/end/cancel).
 */
@Name("dev.tamboui.toolkit.drag.state")
@Label("Toolkit Drag State")
@Description("Drag state transitions")
@Category({ "TamboUI", "Toolkit", "Events" })
public final class DragStateEvent extends Event {
    private static final EventType EVENT = EventType.getEventType(DragStateEvent.class);

    @Label("Route ID")
    long routeId;
    @Label("Action")
    String action;
    @Label("Element ID")
    String elementId;
    @Label("X")
    int x;
    @Label("Y")
    int y;

    private DragStateEvent() {
    }

    /**
     * Returns whether this event type is enabled.
     *
     * @return true if enabled
     */
    public static boolean enabled() {
        return EVENT.isEnabled();
    }

    /**
     * Commits a new drag state event.
     *
     * @param routeId route id
     * @param action drag action (start/drag/end/cancel)
     * @param elementId element id (may be null for end/cancel)
     * @param x x coordinate
     * @param y y coordinate
     */
    public static void commit(long routeId, String action, String elementId, int x, int y) {
        DragStateEvent ev = new DragStateEvent();
        ev.routeId = routeId;
        ev.action = action;
        ev.elementId = elementId;
        ev.x = x;
        ev.y = y;
        ev.commit();
    }
}
