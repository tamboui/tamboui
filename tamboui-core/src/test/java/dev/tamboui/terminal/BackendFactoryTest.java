/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.terminal;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import dev.tamboui.util.IsolatedServiceClassLoader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;

/**
 * Tests for {@link BackendFactory}'s provider resolution and discovery logic.
 * <p>
 * Provider resolution tests use reflection on the private {@code resolveProviders}
 * method, verifying that a comma-separated provider specification acts as a
 * preference order with fallback rather than a mandatory list. Discovery tests use
 * {@link IsolatedServiceClassLoader} to simulate absent, broken, and hidden providers.
 */
class BackendFactoryTest {

    private static final String SERVICE = "dev.tamboui.terminal.BackendProvider";
    private static final String BROKEN_PROVIDER = "dev.tamboui.terminal.ThrowingBackendProvider";
    private static final String WORKING_PROVIDER = "dev.tamboui.capability.test.TestBackendProvider";

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
    @DisplayName("resolveProviders skips unavailable provider and falls back to next")
    void resolveProviders_skipsUnavailableProvider() throws Exception {
        // Only "beta" is available; "alpha" is not (simulating a provider that
        // failed to load, e.g. because it targets a newer Java version)
        List<BackendProvider> available = new ArrayList<>();
        available.add(new StubBackendProvider("beta"));

        List<BackendProvider> resolved = resolveProviders("alpha,beta", available);

        assertThat(resolved).hasSize(1);
        assertThat(resolved.get(0).name()).isEqualTo("beta");
    }

    @Test
    @DisplayName("resolveProviders preserves order when all providers are available")
    void resolveProviders_preservesOrder() throws Exception {
        List<BackendProvider> available = new ArrayList<>();
        available.add(new StubBackendProvider("beta"));
        available.add(new StubBackendProvider("alpha"));

        List<BackendProvider> resolved = resolveProviders("alpha,beta", available);

        assertThat(resolved).hasSize(2);
        assertThat(resolved.get(0).name()).isEqualTo("alpha");
        assertThat(resolved.get(1).name()).isEqualTo("beta");
    }

    @Test
    @DisplayName("resolveProviders matches a fully qualified provider class name")
    void resolveProviders_matchesFullyQualifiedClassName() throws Exception {
        // The class JavaDoc documents selection by FQCN, e.g.
        // tamboui.backend=dev.tamboui.backend.jline.JLineBackendProvider
        List<BackendProvider> available = new ArrayList<>();
        available.add(new StubBackendProvider("alpha"));

        List<BackendProvider> resolved = resolveProviders(StubBackendProvider.class.getName(), available);

        assertThat(resolved).hasSize(1);
        assertThat(resolved.get(0).name()).isEqualTo("alpha");
    }

    @Test
    @DisplayName("resolveProviders throws when no specified provider is available")
    void resolveProviders_throwsWhenNoneAvailable() {
        List<BackendProvider> available = new ArrayList<>();
        available.add(new StubBackendProvider("gamma"));

        assertThatThrownBy(() -> resolveProviders("alpha,beta", available))
                .hasCauseInstanceOf(BackendException.class)
                .cause()
                .hasMessageContaining("alpha,beta");
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
    void requestedProviderBrokenWhileAnotherWorks_reportsRealCause(@TempDir Path tempDir) {
        // Mixed case: the requested provider is present on the classpath but fails to
        // initialize, while an unrelated provider is healthy. The error must surface the
        // real failure instead of advising to add a dependency that is already present.
        System.setProperty("tamboui.backend", "throwing");
        ClassLoader mixed = new IsolatedServiceClassLoader(
                getClass().getClassLoader(), SERVICE, tempDir, BROKEN_PROVIDER, WORKING_PROVIDER);

        Throwable thrown = catchThrowable(() -> BackendFactory.create(mixed));

        assertThat(thrown)
                .isInstanceOf(BackendException.class)
                .hasMessageContaining("ThrowingBackendProvider")
                .hasMessageNotContaining("Add a backend dependency");
        assertThat(rootCause(thrown)).isInstanceOf(NoClassDefFoundError.class);
    }

    @Test
    void unknownProvider_notesUnrelatedLoadFailures(@TempDir Path tempDir) {
        // The user asked for a provider that simply does not exist, while another provider on
        // the classpath failed to load. The error should keep the "add a dependency" advice for
        // the unknown name, but also disclose the dropped provider so it is not silently hidden
        // (e.g. a typo of a provider that IS present but incompatible with this JVM).
        System.setProperty("tamboui.backend", "bogus");
        ClassLoader mixed = new IsolatedServiceClassLoader(
                getClass().getClassLoader(), SERVICE, tempDir, BROKEN_PROVIDER, WORKING_PROVIDER);

        Throwable thrown = catchThrowable(() -> BackendFactory.create(mixed));

        assertThat(thrown)
                .isInstanceOf(BackendException.class)
                .hasMessageContaining("'bogus'")
                .hasMessageContaining("Available providers: test")
                .hasMessageContaining("could not be loaded on this JVM")
                .hasMessageContaining("ThrowingBackendProvider")
                .hasMessageContaining("Add a backend dependency");
    }

    @Test
    void requestedProviderBrokenWithFallback_fallsBackSilently(@TempDir Path tempDir) throws Exception {
        // Preference order with fallback (#422): the broken first choice is skipped and the
        // healthy second choice is used, no exception.
        System.setProperty("tamboui.backend", "throwing,test");
        ClassLoader mixed = new IsolatedServiceClassLoader(
                getClass().getClassLoader(), SERVICE, tempDir, BROKEN_PROVIDER, WORKING_PROVIDER);

        try (Backend backend = BackendFactory.create(mixed)) {
            assertThat(backend).isNotNull();
        }
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

    @Test
    @DisplayName("candidateProvider returns first discovered provider when no spec is set")
    void candidateProvider_returnsFirstDiscoveredWhenNoSpecSet() {
        Optional<BackendProvider> candidate = BackendFactory.candidateProvider();
        assertThat(candidate).isPresent();
        assertThat(candidate.get().name()).isEqualTo("test");
    }

    @Test
    @DisplayName("candidateProvider respects explicit tamboui.backend selection")
    void candidateProvider_respectsExplicitSelection() {
        System.setProperty("tamboui.backend", "test");
        Optional<BackendProvider> candidate = BackendFactory.candidateProvider();
        assertThat(candidate).isPresent();
        assertThat(candidate.get().name()).isEqualTo("test");
    }

    @Test
    @DisplayName("candidateProvider skips unavailable provider and selects fallback")
    void candidateProvider_skipsUnavailableAndSelectsFallback() {
        System.setProperty("tamboui.backend", "nonexistent,test");
        Optional<BackendProvider> candidate = BackendFactory.candidateProvider();
        assertThat(candidate).isPresent();
        assertThat(candidate.get().name()).isEqualTo("test");
    }

    @Test
    @DisplayName("candidateProvider returns empty when configured spec does not match")
    void candidateProvider_returnsEmptyWhenSpecDoesNotMatch() {
        System.setProperty("tamboui.backend", "nonexistent");
        Optional<BackendProvider> candidate = BackendFactory.candidateProvider();
        assertThat(candidate).isEmpty();
    }

    @Test
    @DisplayName("candidateProvider matches fully qualified provider class name")
    void candidateProvider_matchesFullyQualifiedClassName() {
        System.setProperty("tamboui.backend", WORKING_PROVIDER);
        Optional<BackendProvider> candidate = BackendFactory.candidateProvider();
        assertThat(candidate).isPresent();
        assertThat(candidate.get().name()).isEqualTo("test");
    }

    @Test
    @DisplayName("candidateProvider with isolated class loader returns empty when no providers present")
    void candidateProvider_withIsolatedClassLoader_noProviders(@TempDir Path tempDir) {
        ClassLoader empty = new IsolatedServiceClassLoader(getClass().getClassLoader(), SERVICE, tempDir);
        Optional<BackendProvider> candidate = BackendFactory.candidateProvider(empty);
        assertThat(candidate).isEmpty();
    }

    @Test
    @DisplayName("candidateProvider with isolated class loader skips broken provider")
    void candidateProvider_withIsolatedClassLoader_brokenProvider(@TempDir Path tempDir) {
        ClassLoader broken = new IsolatedServiceClassLoader(
                getClass().getClassLoader(), SERVICE, tempDir, BROKEN_PROVIDER);
        Optional<BackendProvider> candidate = BackendFactory.candidateProvider(broken);
        assertThat(candidate).isEmpty();
    }

    @Test
    @DisplayName("candidateProvider with isolated class loader selects working provider in mixed environment")
    void candidateProvider_withIsolatedClassLoader_mixed(@TempDir Path tempDir) {
        ClassLoader mixed = new IsolatedServiceClassLoader(
                getClass().getClassLoader(), SERVICE, tempDir, BROKEN_PROVIDER, WORKING_PROVIDER);
        System.setProperty("tamboui.backend", "throwing,test");
        Optional<BackendProvider> candidate = BackendFactory.candidateProvider(mixed);
        assertThat(candidate).isPresent();
        assertThat(candidate.get().name()).isEqualTo("test");
    }

    @Test
    @DisplayName("resolveCandidate returns empty when provider list is null or empty")
    void resolveCandidate_emptyProviders() {
        assertThat(BackendFactory.resolveCandidate("alpha", null)).isEmpty();
        assertThat(BackendFactory.resolveCandidate("alpha", Collections.emptyList())).isEmpty();
        assertThat(BackendFactory.resolveCandidate(null, Collections.emptyList())).isEmpty();
    }

    @Test
    @DisplayName("resolveCandidate returns first provider when spec is null or blank")
    void resolveCandidate_nullOrBlankSpec() {
        BackendProvider alpha = new StubBackendProvider("alpha");
        BackendProvider beta = new StubBackendProvider("beta");
        List<BackendProvider> providers = Arrays.asList(alpha, beta);

        assertThat(BackendFactory.resolveCandidate(null, providers)).contains(alpha);
        assertThat(BackendFactory.resolveCandidate("", providers)).contains(alpha);
        assertThat(BackendFactory.resolveCandidate("   ", providers)).contains(alpha);
    }

    @Test
    @DisplayName("resolveCandidate selects first matching provider according to preference spec order")
    void resolveCandidate_preferenceOrder() {
        BackendProvider alpha = new StubBackendProvider("alpha");
        BackendProvider beta = new StubBackendProvider("beta");
        List<BackendProvider> providers = Arrays.asList(alpha, beta);

        assertThat(BackendFactory.resolveCandidate("beta,alpha", providers)).contains(beta);
        assertThat(BackendFactory.resolveCandidate("alpha,beta", providers)).contains(alpha);
    }

    @Test
    @DisplayName("resolveCandidate skips unavailable provider and selects first match")
    void resolveCandidate_skipsUnavailable() {
        BackendProvider alpha = new StubBackendProvider("alpha");
        BackendProvider beta = new StubBackendProvider("beta");
        List<BackendProvider> providers = Arrays.asList(alpha, beta);

        assertThat(BackendFactory.resolveCandidate("gamma,beta", providers)).contains(beta);
    }

    @Test
    @DisplayName("resolveCandidate returns empty when no specified provider matches")
    void resolveCandidate_noMatch() {
        BackendProvider alpha = new StubBackendProvider("alpha");
        BackendProvider beta = new StubBackendProvider("beta");
        List<BackendProvider> providers = Arrays.asList(alpha, beta);

        assertThat(BackendFactory.resolveCandidate("gamma,delta", providers)).isEmpty();
        assertThat(BackendFactory.resolveCandidate("  , ,  ", providers)).isEmpty();
    }

    @SuppressWarnings("unchecked")
    private static List<BackendProvider> resolveProviders(String spec, List<BackendProvider> available)
            throws Exception {
        Method method = BackendFactory.class.getDeclaredMethod(
                "resolveProviders", String.class, List.class, List.class);
        method.setAccessible(true);
        return (List<BackendProvider>) method.invoke(null, spec, available, new ArrayList<Throwable>());
    }

    private static Throwable rootCause(Throwable t) {
        Throwable current = t;
        while (current.getCause() != null && current.getCause() != current) {
            current = current.getCause();
        }
        return current;
    }

    /**
     * Minimal BackendProvider stub for testing provider resolution.
     */
    private static class StubBackendProvider implements BackendProvider {
        private final String providerName;

        StubBackendProvider(String name) {
            this.providerName = name;
        }

        @Override
        public String name() {
            return providerName;
        }

        @Override
        public Backend create() throws IOException {
            throw new IOException("stub — not a real backend");
        }
    }
}
