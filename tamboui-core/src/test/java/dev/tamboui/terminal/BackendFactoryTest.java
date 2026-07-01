/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.terminal;

import java.nio.file.Path;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import dev.tamboui.util.IsolatedServiceClassLoader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;

class BackendFactoryTest {

    private static final String SERVICE = "dev.tamboui.terminal.BackendProvider";
    private static final String BROKEN_PROVIDER = "dev.tamboui.terminal.ThrowingBackendProvider";

    private String savedBackendProperty;

    @BeforeEach
    void clearBackendSelection() {
        // A stray tamboui.backend selection would change which branch create() takes.
        savedBackendProperty = System.getProperty("tamboui.backend");
        System.clearProperty("tamboui.backend");
    }

    @AfterEach
    void restoreBackendSelection() {
        // Reset to the original state even when a test set the property itself, so a stray
        // selection cannot leak into later tests.
        if (savedBackendProperty != null) {
            System.setProperty("tamboui.backend", savedBackendProperty);
        } else {
            System.clearProperty("tamboui.backend");
        }
    }

    @Test
    void noProviderDeclared_keepsAddDependencyMessage(@TempDir Path tempDir) {
        ClassLoader empty = new IsolatedServiceClassLoader(getClass().getClassLoader(), SERVICE, tempDir);

        assertThatThrownBy(() -> BackendFactory.create(empty))
                .isInstanceOf(BackendException.class)
                .hasMessageContaining("No BackendProvider found on classpath")
                .hasMessageContaining("Add a backend dependency")
                .hasMessageNotContaining("could be initialized");
    }

    @Test
    void providerDeclaredButFailsToInitialize_reportsRealCause(@TempDir Path tempDir) {
        ClassLoader broken = new IsolatedServiceClassLoader(
                getClass().getClassLoader(), SERVICE, tempDir, BROKEN_PROVIDER);

        Throwable thrown = catchThrowable(() -> BackendFactory.create(broken));

        assertThat(thrown)
                .isInstanceOf(BackendException.class)
                .hasMessageContaining("could be initialized")
                .hasMessageContaining("ThrowingBackendProvider")
                // The misleading "add a dependency" advice must NOT be the message here.
                .hasMessageNotContaining("Add a backend dependency");
        assertThat(thrown.getCause()).isNotNull();
        assertThat(rootCause(thrown)).isInstanceOf(NoClassDefFoundError.class);
    }

    @Test
    void userSelectedProviderPresentButBroken_reportsRealCauseNotMissingDependency(@TempDir Path tempDir) {
        // The user explicitly asked for a backend by name, but the only provider on the classpath
        // fails to initialize. The selection path must surface the real failure, not advise adding
        // a dependency that is already present (just broken).
        System.setProperty("tamboui.backend", "jline");
        ClassLoader broken = new IsolatedServiceClassLoader(
                getClass().getClassLoader(), SERVICE, tempDir, BROKEN_PROVIDER);

        Throwable thrown = catchThrowable(() -> BackendFactory.create(broken));

        assertThat(thrown)
                .isInstanceOf(BackendException.class)
                .hasMessageContaining("could be initialized")
                .hasMessageContaining("ThrowingBackendProvider")
                .hasMessageNotContaining("Add a backend dependency");
        assertThat(rootCause(thrown)).isInstanceOf(NoClassDefFoundError.class);
    }

    @Test
    void blindThreadContextLoader_stillDiscoversProviderViaFallback(@TempDir Path tempDir) throws Exception {
        ClassLoader blindTccl = new IsolatedServiceClassLoader(getClass().getClassLoader(), SERVICE, tempDir);
        ClassLoader original = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(blindTccl);
            try (Backend backend = BackendFactory.create()) {
                assertThat(backend).isNotNull();
            }
        } finally {
            Thread.currentThread().setContextClassLoader(original);
        }
    }

    private static Throwable rootCause(Throwable t) {
        Throwable current = t;
        while (current.getCause() != null && current.getCause() != current) {
            current = current.getCause();
        }
        return current;
    }
}
