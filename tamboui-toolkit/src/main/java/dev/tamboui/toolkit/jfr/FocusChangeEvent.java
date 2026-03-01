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
 * JFR event emitted when focus changes during toolkit event routing.
 */
@Name("dev.tamboui.toolkit.focus.change")
@Label("Toolkit Focus Change")
@Description("Focus change in toolkit event routing")
@Category({ "TamboUI", "Toolkit", "Events" })
public final class FocusChangeEvent extends Event {
    private static final EventType EVENT = EventType.getEventType(FocusChangeEvent.class);

    @Label("Route ID")
    long routeId;
    @Label("From")
    String fromId;
    @Label("To")
    String toId;
    @Label("Reason")
    String reason;

    private FocusChangeEvent() {
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
     * Commits a new focus change event.
     *
     * @param routeId route id
     * @param fromId previous focused id (may be null)
     * @param toId new focused id (may be null)
     * @param reason reason (may be null)
     */
    public static void commit(long routeId, String fromId, String toId, String reason) {
        FocusChangeEvent ev = new FocusChangeEvent();
        ev.routeId = routeId;
        ev.fromId = fromId;
        ev.toId = toId;
        ev.reason = reason;
        ev.commit();
    }
}
