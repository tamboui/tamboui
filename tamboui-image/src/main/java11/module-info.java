/**
 * Image rendering support for TamboUI TUI library.
 * <p>
 * This module provides image display capabilities with automatic terminal detection.
 * Native protocols (Kitty, iTerm2, Sixel) are used when available, with character-based
 * fallbacks (half-blocks, Braille) for universal compatibility.
 */
module dev.tamboui.image {
    requires transitive dev.tamboui.core;
    requires transitive dev.tamboui.widgets;
    requires java.desktop;

    exports dev.tamboui.image;
    exports dev.tamboui.image.capability;
    exports dev.tamboui.image.protocol;
}
