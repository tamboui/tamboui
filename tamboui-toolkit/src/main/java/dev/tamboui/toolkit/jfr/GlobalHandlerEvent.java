/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.toolkit.jfr;

import dev.tamboui.toolkit.event.EventResult;

import jdk.jfr.Category;
import jdk.jfr.Description;
import jdk.jfr.Event;
import jdk.jfr.EventType;
import jdk.jfr.Label;
import jdk.jfr.Name;

/**
 * JFR event emitted for each global handler invocation during routing.
 */
@Name("dev.tamboui.toolkit.global.handler")
@Label("Toolkit Event Global Handler")
@Description("Global event handler invocation")
@Category({ "TamboUI", "Toolkit", "Events" })
public final class GlobalHandlerEvent extends Event {
    private static final EventType EVENT = EventType.getEventType(GlobalHandlerEvent.class);

    @Label("Route ID")
    long routeId;
    @Label("Handler Index")
    int index;
    @Label("Result")
    String result;

    private GlobalHandlerEvent() {
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
     * Commits a new global handler event.
     *
     * @param routeId route id
     * @param index handler index
     * @param result handler result
     */
    public static void commit(long routeId, int index, EventResult result) {
        GlobalHandlerEvent ev = new GlobalHandlerEvent();
        ev.routeId = routeId;
        ev.index = index;
        ev.result = result.name();
        ev.commit();
    }
}
