/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.util;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import dev.tamboui.terminal.BackendProvider;

import static org.assertj.core.api.Assertions.assertThat;

class SafeServiceLoaderTest {

    private static final String SERVICE = "dev.tamboui.terminal.BackendProvider";
    private static final String WORKING_PROVIDER = "dev.tamboui.capability.test.TestBackendProvider";
    private static final String BROKEN_PROVIDER = "dev.tamboui.terminal.ThrowingBackendProvider";

    @Test
    void collectsFailureFromBrokenProviderButStillReturnsWorkingOnes(@TempDir Path tempDir) {
        ClassLoader loader = new IsolatedServiceClassLoader(
                getClass().getClassLoader(), SERVICE, tempDir, BROKEN_PROVIDER, WORKING_PROVIDER);

        List<Throwable> failures = new ArrayList<Throwable>();
        List<BackendProvider> providers = SafeServiceLoader.load(BackendProvider.class, loader, failures::add);

        assertThat(providers).extracting(BackendProvider::name).containsExactly("test");
        assertThat(failures).hasSize(1);
        assertThat(failures.get(0).getMessage()).contains("ThrowingBackendProvider");
    }

    @Test
    void explicitLoaderIsUsedExclusively(@TempDir Path tempDir) {
        // The thread-context loader (the app loader) DOES have providers, but an explicit
        // loader with none must yield none: discovery is deterministic, not a union.
        ClassLoader empty = new IsolatedServiceClassLoader(
                getClass().getClassLoader(), SERVICE, tempDir);

        List<Throwable> failures = new ArrayList<Throwable>();
        List<BackendProvider> providers = SafeServiceLoader.load(BackendProvider.class, empty, failures::add);

        assertThat(providers).isEmpty();
        assertThat(failures).isEmpty();
    }

    @Test
    void fallsBackBeyondBlindThreadContextLoaderWhenNoExplicitLoaderGiven(@TempDir Path tempDir) {
        // Reproduces CAMEL-23835: the TCCL cannot see the provider, but it is reachable from
        // SafeServiceLoader's own loader. With no explicit loader, the fallback must find it.
        ClassLoader blindTccl = new IsolatedServiceClassLoader(
                getClass().getClassLoader(), SERVICE, tempDir);
        ClassLoader original = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(blindTccl);
            List<BackendProvider> providers = SafeServiceLoader.load(BackendProvider.class);
            assertThat(providers).extracting(BackendProvider::name).contains("test");
        } finally {
            Thread.currentThread().setContextClassLoader(original);
        }
    }

    @Test
    void deduplicatesProvidersAndFailuresAcrossCandidateLoaders() {
        // The fallback consults several candidate loaders (TCCL, own, system), which here all see
        // the same test service file. De-duplication (by provider class name and by failure
        // signature) must collapse the result to a single occurrence regardless of whether those
        // loaders resolve to the same object or to distinct loaders sharing the classpath.
        List<Throwable> failures = new ArrayList<Throwable>();
        List<BackendProvider> providers = SafeServiceLoader.load(BackendProvider.class, failures::add);

        assertThat(providers).filteredOn(p -> p.name().equals("test")).hasSize(1);
        // The deliberately-missing provider in the test service file fails exactly once.
        assertThat(failures).hasSize(1);
    }
}
