import dev.tamboui.backend.aesh.AeshBackendProvider;
import dev.tamboui.terminal.BackendProvider;

/**
 * Aesh Readline backend for TamboUI TUI library.
 * <p>
 * This module provides a terminal backend implementation using aesh-readline,
 * enabling TamboUI applications to run using the aesh-readline terminal abstraction.
 * <p>
 * Note: aesh-readline is not modularized. It will be resolved from the classpath
 * as an automatic module when available.
 */
@SuppressWarnings({"requires-transitive-automatic", "requires-automatic"})
module dev.tamboui.aesh.backend {
    requires transitive dev.tamboui.core;
    // aesh-readline is not modularized; resolved from classpath as automatic module

    exports dev.tamboui.backend.aesh;

    provides BackendProvider with AeshBackendProvider;
}
