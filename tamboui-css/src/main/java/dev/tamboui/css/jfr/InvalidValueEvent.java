/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.css.jfr;

import jdk.jfr.Category;
import jdk.jfr.Description;
import jdk.jfr.Event;
import jdk.jfr.EventType;
import jdk.jfr.FlightRecorder;
import jdk.jfr.Label;
import jdk.jfr.Name;

/**
 * JFR event emitted when an invalid or unconvertible CSS property value is ignored during style cascade.
 */
@Name("dev.tamboui.css.invalidValue")
@Label("CSS Invalid Property Value")
@Description("Ignored or invalid CSS property value during cascade resolution")
@Category({ "TamboUI", "CSS" })
public final class InvalidValueEvent extends Event {
    private static EventType EVENT;

    @Label("Property")
    String property;
    @Label("Value")
    String value;
    @Label("Reason")
    String reason;

    private InvalidValueEvent() {
    }

    /**
     * Returns whether this event type is enabled.
     *
     * @return true if enabled
     */
    public static boolean enabled() {
        if (!FlightRecorder.isAvailable()) {
            return false;
        }
        if (EVENT == null) {
            EVENT = EventType.getEventType(InvalidValueEvent.class);
        }
        return EVENT.isEnabled();
    }

    /**
     * Commits a new invalid value event.
     *
     * @param property property name
     * @param value    raw property value
     * @param reason   reason the value was ignored
     */
    public static void commit(String property, String value, String reason) {
        InvalidValueEvent ev = new InvalidValueEvent();
        ev.property = property;
        ev.value = value;
        ev.reason = reason;
        ev.commit();
    }
}
