/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.backend.jline3;

import dev.tamboui.terminal.Backend;
import dev.tamboui.terminal.BackendProvider;

import java.io.IOException;

/**
 * {@link BackendProvider} implementation for JLine 3.
 * <p>
 * This provider is registered via the Java {@link java.util.ServiceLoader} mechanism.
 */
public class JLineBackendProvider implements BackendProvider {

    /**
     * Creates a new JLine 3 backend provider.
     */
    public JLineBackendProvider() {
    }

    @Override
    public String name() {
        return "jline3";
    }

    @Override
    public Backend create() throws IOException {
        return new JLineBackend();
    }
}
