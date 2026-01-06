import dev.tamboui.backend.panama.PanamaBackendProvider;
import dev.tamboui.terminal.BackendProvider;

/**
 * Panama FFI backend for TamboUI TUI library.
 * <p>
 * This module provides a terminal backend implementation using the
 * Java Foreign Function and Memory API (Panama FFI), enabling
 * TamboUI applications to run without external dependencies.
 * <p>
 * Requires Java 22 or later for the finalized FFI API.
 */
module dev.tamboui.panama.backend {
    requires transitive dev.tamboui.core;

    exports dev.tamboui.backend.panama;
    exports dev.tamboui.backend.panama.unix;
    exports dev.tamboui.backend.panama.windows;

    provides BackendProvider with PanamaBackendProvider;
}
