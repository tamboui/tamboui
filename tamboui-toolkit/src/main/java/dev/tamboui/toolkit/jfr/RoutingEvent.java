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
 * JFR duration event for a single toolkit routing operation.
 * <p>
 * This event is started at the beginning of {@code EventRouter.route(...)} and committed at the end.
 */
@Name("dev.tamboui.toolkit.route")
@Label("Toolkit Event Route")
@Description("Duration of toolkit event routing")
@Category({ "TamboUI", "Toolkit", "Events" })
public final class RoutingEvent extends Event {
    private static final EventType EVENT = EventType.getEventType(RoutingEvent.class);

    @Label("Route ID")
    long routeId;
    @Label("Event")
    String event;
    @Label("Focused ID")
    String focusedId;
    @Label("Element Count")
    int elementCount;
    @Label("Result")
    String result;
    

    private RoutingEvent() {
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
     * Starts a new routing duration event.
     *
     * @param routeId route id
     * @param event the routed event
     * @param focusedId focused element id (may be null)
     * @param elementCount number of registered elements
     * @return the started routing event (must be ended and committed)
     */
    public static RoutingEvent begin(long routeId, dev.tamboui.tui.event.Event event, String focusedId, int elementCount) {
        RoutingEvent ev = new RoutingEvent();
        ev.routeId = routeId;
        ev.event = String.valueOf(event);
        ev.focusedId = focusedId;
        ev.elementCount = elementCount;
        ev.begin();
        return ev;
    }

    /**
     * Sets the routing result label.
     *
     * @param result routing result
     */
    public void setResult(EventResult result) {
        this.result = result != null ? result.name() : null;
    }
}
