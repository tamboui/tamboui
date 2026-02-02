/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.backend.panama;

import java.io.IOException;

import dev.tamboui.terminal.Backend;
import dev.tamboui.terminal.BackendProvider;

/**
 * {@link BackendProvider} implementation for Panama FFI backend.
 * <p>
 * This provider is registered via the Java {@link java.util.ServiceLoader} mechanism.
 */
public class PanamaBackendProvider implements BackendProvider {

    /**
     * Creates a new Panama backend provider.
     */
    public PanamaBackendProvider() {
    }

    @Override
    public Backend create() throws IOException {
        return new PanamaBackend();
    }
}
