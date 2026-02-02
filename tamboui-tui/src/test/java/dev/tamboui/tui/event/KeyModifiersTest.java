/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.tui.event;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class KeyModifiersTest {

    @Test
    @DisplayName("NONE has no modifiers")
    void noneHasNoModifiers() {
        assertThat(KeyModifiers.NONE.ctrl()).isFalse();
        assertThat(KeyModifiers.NONE.alt()).isFalse();
        assertThat(KeyModifiers.NONE.shift()).isFalse();
    }

    @Test
    @DisplayName("CTRL has only ctrl modifier")
    void ctrlHasOnlyCtrl() {
        assertThat(KeyModifiers.CTRL.ctrl()).isTrue();
        assertThat(KeyModifiers.CTRL.alt()).isFalse();
        assertThat(KeyModifiers.CTRL.shift()).isFalse();
    }

    @Test
    @DisplayName("ALT has only alt modifier")
    void altHasOnlyAlt() {
        assertThat(KeyModifiers.ALT.ctrl()).isFalse();
        assertThat(KeyModifiers.ALT.alt()).isTrue();
        assertThat(KeyModifiers.ALT.shift()).isFalse();
    }

    @Test
    @DisplayName("SHIFT has only shift modifier")
    void shiftHasOnlyShift() {
        assertThat(KeyModifiers.SHIFT.ctrl()).isFalse();
        assertThat(KeyModifiers.SHIFT.alt()).isFalse();
        assertThat(KeyModifiers.SHIFT.shift()).isTrue();
    }

    @Test
    @DisplayName("of creates modifier with specified flags")
    void ofCreatesModifier() {
        KeyModifiers mods = KeyModifiers.of(true, true, true);
        assertThat(mods.ctrl()).isTrue();
        assertThat(mods.alt()).isTrue();
        assertThat(mods.shift()).isTrue();
    }

    @Test
    @DisplayName("of with partial flags")
    void ofWithPartialFlags() {
        KeyModifiers ctrlAlt = KeyModifiers.of(true, true, false);
        assertThat(ctrlAlt.ctrl()).isTrue();
        assertThat(ctrlAlt.alt()).isTrue();
        assertThat(ctrlAlt.shift()).isFalse();

        KeyModifiers shiftOnly = KeyModifiers.of(false, false, true);
        assertThat(shiftOnly.ctrl()).isFalse();
        assertThat(shiftOnly.alt()).isFalse();
        assertThat(shiftOnly.shift()).isTrue();
    }

    @Test
    @DisplayName("constants are equivalent to of() with matching flags")
    void constantsEquivalentToOf() {
        assertThat(KeyModifiers.NONE).isEqualTo(KeyModifiers.of(false, false, false));
        assertThat(KeyModifiers.CTRL).isEqualTo(KeyModifiers.of(true, false, false));
        assertThat(KeyModifiers.ALT).isEqualTo(KeyModifiers.of(false, true, false));
        assertThat(KeyModifiers.SHIFT).isEqualTo(KeyModifiers.of(false, false, true));
    }
}
