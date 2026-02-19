/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */

/**
 * High-level TUI application framework for TamboUI.
 * <p>
 * This module provides the TuiRunner and event handling infrastructure
 * for building interactive terminal applications.
 */
module dev.tamboui.tui {
    requires transitive dev.tamboui.annotations;
    requires transitive dev.tamboui.core;
    requires transitive dev.tamboui.widgets;

    exports dev.tamboui.tui;
    exports dev.tamboui.tui.bindings;
    exports dev.tamboui.tui.event;

    opens dev.tamboui.tui.bindings;
    exports dev.tamboui.tui.error;
}
