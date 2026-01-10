/**
 * Core types and abstractions for TamboUI TUI library.
 * <p>
 * This module provides the fundamental building blocks for terminal user interfaces:
 * buffers, cells, layouts, styles, text, and widget interfaces.
 */
module dev.tamboui.core {
    exports dev.tamboui.buffer;
    exports dev.tamboui.inline;
    exports dev.tamboui.layout;
    exports dev.tamboui.style;
    exports dev.tamboui.symbols.merge;
    exports dev.tamboui.terminal;
    exports dev.tamboui.text;
    exports dev.tamboui.util;
    exports dev.tamboui.widget;

    uses dev.tamboui.terminal.BackendProvider;
}
