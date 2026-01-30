import dev.tamboui.backend.jline3.JLineBackendProvider;
import dev.tamboui.terminal.BackendProvider;

/**
 * JLine 3 backend for TamboUI TUI library.
 * <p>
 * This module provides a terminal backend implementation using JLine 3,
 * enabling TamboUI applications to run in standard terminals.
 */
@SuppressWarnings({"requires-transitive-automatic",
        "requires-automatic"}) module dev.tamboui.jline_three.backend {
    requires transitive dev.tamboui.core;
    requires transitive org.jline;

    exports dev.tamboui.backend.jline3;

    provides BackendProvider with JLineBackendProvider;
}
