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
    exports dev.tamboui.layout.columns;
    exports dev.tamboui.layout.dock;
    exports dev.tamboui.layout.flow;
    exports dev.tamboui.layout.grid;
    exports dev.tamboui.layout.stack;
    exports dev.tamboui.widget;
    exports dev.tamboui.error;

    uses dev.tamboui.terminal.BackendProvider;
}
