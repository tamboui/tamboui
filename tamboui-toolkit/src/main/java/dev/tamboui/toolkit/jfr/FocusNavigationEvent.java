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
 * JFR event emitted when performing focus navigation (Tab/Shift+Tab).
 */
@Name("dev.tamboui.toolkit.focus.navigation")
@Label("Toolkit Event Focus Navigation")
@Description("Focus navigation action")
@Category({ "TamboUI", "Toolkit", "Events" })
public final class FocusNavigationEvent extends Event {
    private static final EventType EVENT = EventType.getEventType(FocusNavigationEvent.class);

    @Label("Route ID")
    long routeId;
    @Label("Action")
    String action;
    @Label("Success")
    boolean success;
    @Label("From")
    String fromId;
    @Label("To")
    String toId;

    private FocusNavigationEvent() {
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
     * Commits a new focus navigation event.
     *
     * @param routeId route id
     * @param action action (focusNext/focusPrevious)
     * @param success whether navigation succeeded
     * @param fromId focused id before (may be null)
     * @param toId focused id after (may be null)
     */
    public static void commit(long routeId, String action, boolean success, String fromId, String toId) {
        FocusNavigationEvent ev = new FocusNavigationEvent();
        ev.routeId = routeId;
        ev.action = action;
        ev.success = success;
        ev.fromId = fromId;
        ev.toId = toId;
        ev.commit();
    }
}
