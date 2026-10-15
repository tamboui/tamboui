/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.demo;

import java.util.List;

import org.junit.jupiter.api.Test;

import dev.tamboui.tui.event.KeyCode;
import dev.tamboui.tui.event.KeyEvent;
import dev.tamboui.tui.event.KeyModifiers;
import dev.tamboui.tui.event.MouseButton;
import dev.tamboui.tui.event.MouseEvent;
import dev.tamboui.tui.event.MouseEventKind;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TuiDemoTest {

    @Test
    void modifierFlagsKeepExtendedModifierSlotsVisible() {
        List<TuiDemo.ModifierFlag> flags = TuiDemo.modifierFlags(KeyModifiers.of(true, false, true));

        assertEquals(List.of("Ctrl", "Alt", "Shift", "Super", "Hyper", "Meta"),
                flags.stream().map(TuiDemo.ModifierFlag::label).toList());
        assertTrue(flags.get(0).active());
        assertFalse(flags.get(1).active());
        assertTrue(flags.get(2).active());
        assertFalse(flags.get(3).available());
        assertFalse(flags.get(4).available());
        assertFalse(flags.get(5).available());
    }

    @Test
    void keySnapshotSeparatesLogicalKeyAndProducedText() {
        TuiDemo.KeySnapshot character = TuiDemo.KeySnapshot.from(KeyEvent.ofChar('Å', KeyModifiers.of(true, true, false)));
        TuiDemo.KeySnapshot function = TuiDemo.KeySnapshot.from(KeyEvent.ofKey(KeyCode.F5, KeyModifiers.NONE));

        assertTrue(character.present());
        assertEquals("CHAR", character.logicalKey());
        assertEquals("Å", character.producedText());
        assertEquals(KeyModifiers.of(true, true, false), character.modifiers());
        assertEquals("F5", function.logicalKey());
        assertEquals("∅", function.producedText());
    }

    @Test
    void mouseDeviceTracksButtonsHoverAndScrollDirections() {
        TuiDemo.MouseDeviceState state = new TuiDemo.MouseDeviceState();

        state.observe(new MouseEvent(MouseEventKind.PRESS, MouseButton.LEFT, 4, 3, KeyModifiers.NONE));
        assertTrue(state.isLeftPressed());

        state.observe(new MouseEvent(MouseEventKind.SCROLL_LEFT, MouseButton.NONE, 4, 3, KeyModifiers.NONE));
        assertTrue(state.horizontalScrollPulse() < 0);
        assertEquals(0, state.hoverPulse());

        state.observe(new MouseEvent(MouseEventKind.MOVE, MouseButton.NONE, 4, 3, KeyModifiers.NONE));
        assertTrue(state.hoverPulse() > 0);

        state.observe(new MouseEvent(MouseEventKind.RELEASE, MouseButton.NONE, 4, 3, KeyModifiers.NONE));
        assertFalse(state.isLeftPressed());

        for (int i = 0; i < 8; i++) {
            state.tick();
        }

        assertEquals(0, state.horizontalScrollPulse());
        assertEquals(0, state.hoverPulse());
    }
}
