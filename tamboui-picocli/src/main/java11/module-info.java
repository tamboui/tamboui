/**
 * PicoCLI integration for TamboUI TUI applications.
 * <p>
 * This module provides integration between PicoCLI command-line parsing and
 * TamboUI terminal user interfaces.
 */
module dev.tamboui.picocli {
    requires transitive dev.tamboui.tui;
    requires transitive info.picocli;

    exports dev.tamboui.picocli;
}
