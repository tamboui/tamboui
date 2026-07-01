/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.terminal;

/**
 * Test {@link BackendProvider} whose construction fails with a {@link NoClassDefFoundError},
 * simulating a backend jar that is present on the classpath but binary-incompatible with a
 * transitive dependency (e.g. a JLine version mismatch).
 * <p>
 * This class is intentionally NOT registered in any {@code META-INF/services} file on the
 * default test classpath; tests declare it explicitly through an isolated classloader so it
 * does not interfere with other tests.
 */
public final class ThrowingBackendProvider implements BackendProvider {

    public ThrowingBackendProvider() {
        throw new NoClassDefFoundError("org/jline/terminal/Terminal");
    }

    @Override
    public Backend create() {
        throw new IllegalStateException("provider should never be instantiated");
    }
}
