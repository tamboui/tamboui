/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.backend.aesh;

import dev.tamboui.terminal.Backend;
import dev.tamboui.terminal.BackendProvider;

import java.io.IOException;

/**
 * {@link BackendProvider} implementation for Aesh Readline.
 * <p>
 * This provider is registered via the Java {@link java.util.ServiceLoader} mechanism.
 */
public class AeshBackendProvider implements BackendProvider {

    /** Creates a new Aesh backend provider. */
    public AeshBackendProvider() {
    }

    @Override
    public String name() {
        return "aesh";
    }

    @Override
    public Backend create() throws IOException {
        return new AeshBackend();
    }
}
