/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.integration.backend;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import dev.tamboui.capability.Capabilities;
import dev.tamboui.terminal.Backend;
import dev.tamboui.terminal.BackendException;
import dev.tamboui.terminal.BackendFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Integration tests for backend discovery and selection on a JVM older than the one
 * {@code tamboui-panama-backend} is compiled for (Java 22).
 * <p>
 * The test task pins a Java 21 toolchain (see build.gradle.kts), with the real panama and
 * jline3 backend jars on the classpath. The panama provider therefore fails to load with a
 * genuine {@code UnsupportedClassVersionError}, which is the mixed-JDK deployment scenario
 * behind the preference-order fallback and absent-vs-broken error reporting.
 */
class BackendSelectionIntegrationTest {

    private static final String JLINE3_BACKEND_CLASS = "dev.tamboui.backend.jline3.JLineBackend";
    private static final String JLINE3_PROVIDER_FQCN = "dev.tamboui.backend.jline3.JLineBackendProvider";

    private String savedBackendProperty;

    @BeforeAll
    static void requiresJava21WithoutEnvOverride() {
        assertThat(System.getProperty("java.specification.version"))
                .as("these tests must run on a Java 21 toolchain (see build.gradle.kts)")
                .isEqualTo("21");
        assumeTrue(System.getenv("TAMBOUI_BACKEND") == null,
                "TAMBOUI_BACKEND is set in the environment; it would override the tested selection");
    }

    @BeforeEach
    void saveBackendSelection() {
        savedBackendProperty = System.getProperty("tamboui.backend");
        System.clearProperty("tamboui.backend");
    }

    @AfterEach
    void restoreBackendSelection() {
        if (savedBackendProperty != null) {
            System.setProperty("tamboui.backend", savedBackendProperty);
        } else {
            System.clearProperty("tamboui.backend");
        }
    }

    @Test
    @DisplayName("comma-separated spec falls back when the first choice cannot load on this JVM")
    void commaSpecFallsBackToNextProvider() throws Exception {
        System.setProperty("tamboui.backend", "panama,jline3");

        try (Backend backend = BackendFactory.create()) {
            assertThat(backend.getClass().getName()).isEqualTo(JLINE3_BACKEND_CLASS);
        }
    }

    @Test
    @DisplayName("requesting only an incompatible provider reports the real cause, not missing dependency")
    void incompatibleOnlyChoiceReportsRealCause() {
        System.setProperty("tamboui.backend", "panama");

        Throwable thrown = catchThrowable(BackendFactory::create);

        assertThat(thrown)
                .isInstanceOf(BackendException.class)
                .hasMessageContaining("could not be initialized")
                .hasMessageContaining("PanamaBackendProvider")
                .hasMessageContaining("class file version")
                // The jar IS on the classpath; advising to add it would be wrong.
                .hasMessageNotContaining("Add a backend dependency")
                // The error suggests the working fallback spec.
                .hasMessageContaining("panama,jline3");
    }

    @Test
    @DisplayName("unknown provider name lists available providers and discloses dropped ones")
    void unknownProviderDisclosesDroppedProviders() {
        System.setProperty("tamboui.backend", "bogus");

        Throwable thrown = catchThrowable(BackendFactory::create);

        assertThat(thrown)
                .isInstanceOf(BackendException.class)
                .hasMessageContaining("'bogus'")
                .hasMessageContaining("Available providers: jline3")
                .hasMessageContaining("could not be loaded on this JVM")
                .hasMessageContaining("PanamaBackendProvider")
                .hasMessageContaining("Add a backend dependency");
    }

    @Test
    @DisplayName("provider can be selected by fully qualified class name")
    void fullyQualifiedClassNameSelection() throws Exception {
        System.setProperty("tamboui.backend", JLINE3_PROVIDER_FQCN);

        try (Backend backend = BackendFactory.create()) {
            assertThat(backend.getClass().getName()).isEqualTo(JLINE3_BACKEND_CLASS);
        }
    }

    @Test
    @DisplayName("auto-discovery without a spec selects the loadable provider")
    void autoDiscoverySelectsLoadableProvider() throws Exception {
        try (Backend backend = BackendFactory.create()) {
            assertThat(backend.getClass().getName()).isEqualTo(JLINE3_BACKEND_CLASS);
        }
    }

    @Test
    @DisplayName("capabilities report names loadable providers and discloses load errors")
    void capabilitiesReportDisclosesLoadErrors() throws Exception {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        PrintStream out = new PrintStream(buffer, true, "UTF-8");
        Capabilities.print(out);
        String report = new String(buffer.toByteArray(), StandardCharsets.UTF_8);

        assertThat(report)
                .contains("backend.provider_names: jline3")
                .contains("backend.provider_error.0")
                .contains("PanamaBackendProvider");
    }
}
