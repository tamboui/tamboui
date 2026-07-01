/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.tui;

import java.net.URL;
import java.net.URLClassLoader;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TuiRunnerBackendClassLoaderTest {

    private static ClassLoader newLoader() {
        return new URLClassLoader(new URL[0], TuiRunnerBackendClassLoaderTest.class.getClassLoader());
    }

    @Test
    void explicitArgumentTakesPrecedenceOverConfig() {
        ClassLoader fromConfig = newLoader();
        ClassLoader explicit = newLoader();
        TuiConfig config = TuiConfig.builder().backendClassLoader(fromConfig).build();

        assertThat(TuiRunner.resolveBackendClassLoader(config, explicit)).isSameAs(explicit);
    }

    @Test
    void fallsBackToConfigWhenNoExplicitArgument() {
        ClassLoader fromConfig = newLoader();
        TuiConfig config = TuiConfig.builder().backendClassLoader(fromConfig).build();

        assertThat(TuiRunner.resolveBackendClassLoader(config, null)).isSameAs(fromConfig);
    }

    @Test
    void nullWhenNeitherProvided() {
        TuiConfig config = TuiConfig.builder().build();

        assertThat(TuiRunner.resolveBackendClassLoader(config, null)).isNull();
    }
}
