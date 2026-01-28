import dev.tamboui.backend.aesh.AeshBackendProvider;
import dev.tamboui.terminal.BackendProvider;

/**
 * Aesh Readline backend for TamboUI TUI library.
 * <p>
 * This module provides a terminal backend implementation using aesh-readline,
 * enabling TamboUI applications to run using the aesh-readline terminal abstraction.
 * <p>
 * Note: aesh-readline is not modularized, so it will be resolved as an automatic module
 * from the classpath.
 */
@SuppressWarnings({"requires-transitive-automatic", "requires-automatic"})
module dev.tamboui.aesh.backend {
    requires transitive dev.tamboui.core;
    // aesh-readline is not modularized, will be resolved from classpath

    exports dev.tamboui.backend.aesh;

    provides BackendProvider with AeshBackendProvider;
}
