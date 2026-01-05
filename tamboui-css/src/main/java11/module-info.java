/**
 * CSS styling support for TamboUI TUI library.
 * <p>
 * This module provides CSS-based styling capabilities for TamboUI widgets,
 * including a CSS parser, selector matching, and style cascading.
 */
module dev.tamboui.css {
    requires transitive dev.tamboui.core;
    requires transitive dev.tamboui.widgets;

    exports dev.tamboui.css;
    exports dev.tamboui.css.cascade;
    exports dev.tamboui.css.engine;
    exports dev.tamboui.css.model;
    exports dev.tamboui.css.parser;
    exports dev.tamboui.css.property;
    exports dev.tamboui.css.selector;
}
