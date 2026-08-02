/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.widgets.toast;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import dev.tamboui.style.Color;
import dev.tamboui.tui.bindings.KeyTrigger;

import static org.assertj.core.api.Assertions.*;

class ToastTypesTest {

    @Test
    @DisplayName("ToastType provides default accent colors")
    void toastTypeAccentColors() {
        assertThat(ToastType.INFO.accentColor()).isEqualTo(Color.BLUE);
        assertThat(ToastType.SUCCESS.accentColor()).isEqualTo(Color.GREEN);
        assertThat(ToastType.WARNING.accentColor()).isEqualTo(Color.YELLOW);
        assertThat(ToastType.ERROR.accentColor()).isEqualTo(Color.RED);
    }

    @Test
    @DisplayName("BorderMode maps to Borders sets")
    void borderModeBorders() {
        assertThat(BorderMode.SIDE_RAILS.borders()).containsExactlyInAnyOrder(
                dev.tamboui.widgets.block.Borders.LEFT,
                dev.tamboui.widgets.block.Borders.RIGHT);
        assertThat(BorderMode.FULL.borders()).isEqualTo(dev.tamboui.widgets.block.Borders.ALL);
    }

    @Test
    @DisplayName("ToastShortcut stores trigger and action")
    void toastShortcut() {
        ToastShortcut shortcut = ToastShortcut.of(KeyTrigger.ch('d'), ToastShortcut.Action.DISMISS_TOP);
        assertThat(shortcut.trigger()).isEqualTo(KeyTrigger.ch('d'));
        assertThat(shortcut.action()).isEqualTo(ToastShortcut.Action.DISMISS_TOP);
    }
}
