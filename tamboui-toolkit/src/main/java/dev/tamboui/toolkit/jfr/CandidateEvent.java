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
 * JFR event emitted for each element candidate considered during event routing.
 */
@Name("dev.tamboui.toolkit.candidate")
@Label("Toolkit Event Candidate Element")
@Description("Candidate element considered during event routing")
@Category({ "TamboUI", "Toolkit", "Events" })
public final class CandidateEvent extends Event {
    private static final EventType EVENT = EventType.getEventType(CandidateEvent.class);

    @Label("Route ID")
    long routeId;
    @Label("Element ID")
    String elementId;
    @Label("Element Type")
    String elementType;
    @Label("Phase")
    String phase;
    @Label("Decision")
    String decision;
    @Label("Reason")
    String reason;

    private CandidateEvent() {
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
     * Commits a new candidate event.
     *
     * @param routeId route id
     * @param elementId element id (may be null)
     * @param elementType element type
     * @param phase routing phase (focused/unfocused/mouse, etc.)
     * @param decision decision (tried/handled/unhandled, etc.)
     * @param reason optional reason (may be null)
     */
    public static void commit(long routeId, String elementId, String elementType, String phase, String decision, String reason) {
        CandidateEvent ev = new CandidateEvent();
        ev.routeId = routeId;
        ev.elementId = elementId;
        ev.elementType = elementType;
        ev.phase = phase;
        ev.decision = decision;
        ev.reason = reason;
        ev.commit();
    }
}
