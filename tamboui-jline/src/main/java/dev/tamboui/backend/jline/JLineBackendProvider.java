/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.backend.jline;

import dev.tamboui.terminal.Backend;
import dev.tamboui.terminal.BackendProvider;

import java.io.IOException;

/// {@link BackendProvider} implementation for JLine 3.
///
///
///
/// This provider is registered via the Java {@link java.util.ServiceLoader} mechanism.
public class JLineBackendProvider implements BackendProvider {

    @Override
    public String name() {
        return "jline";
    }

    @Override
    public Backend create() throws IOException {
        return new JLineBackend();
    }
}

