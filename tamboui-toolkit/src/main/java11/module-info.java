/**
 * Fluent DSL for building TUI applications with TamboUI.
 * <p>
 * This module provides a declarative, retained-mode API for building
 * terminal user interfaces with focus management and event routing.
 */
module dev.tamboui.toolkit {
    requires transitive dev.tamboui.core;
    requires transitive dev.tamboui.widgets;
    requires transitive dev.tamboui.tui;
    requires transitive dev.tamboui.css;
    requires java.logging;

    exports dev.tamboui.toolkit;
    exports dev.tamboui.toolkit.app;
    exports dev.tamboui.toolkit.component;
    exports dev.tamboui.toolkit.element;
    exports dev.tamboui.toolkit.elements;
    exports dev.tamboui.toolkit.event;
    exports dev.tamboui.toolkit.focus;
}
