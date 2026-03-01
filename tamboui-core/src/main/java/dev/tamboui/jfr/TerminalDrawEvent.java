/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.jfr;

import jdk.jfr.Category;
import jdk.jfr.Description;
import jdk.jfr.Event;
import jdk.jfr.EventType;
import jdk.jfr.Label;
import jdk.jfr.Name;
import jdk.jfr.Threshold;

/**
 * JFR event measuring time spent in {@code Terminal.draw()}.
 *
 * @see dev.tamboui.terminal.Terminal#draw(java.util.function.Consumer)
 */
@Name("dev.tamboui.terminal.draw")
@Label("Terminal Draw")
@Description("Time spent in Terminal.draw()")
@Category({ "TamboUI", "Terminal" })
@Threshold("1 ms")
public final class TerminalDrawEvent extends Event {
    /**
     * The {@link EventType} for {@link TerminalDrawEvent}.
     */
    public static final EventType EVENT = EventType.getEventType(TerminalDrawEvent.class);

    /**
     * Creates a new draw event instance.
     */
    public TerminalDrawEvent() {
    }
}
