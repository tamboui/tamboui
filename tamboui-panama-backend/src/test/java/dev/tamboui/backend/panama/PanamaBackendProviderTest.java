/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.backend.panama;

import dev.tamboui.terminal.BackendProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import java.util.ServiceLoader;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for PanamaBackendProvider.
 */
@EnabledOnOs({OS.LINUX, OS.MAC})
class PanamaBackendProviderTest {

    @Test
    void providerCanBeInstantiated() {
        PanamaBackendProvider provider = new PanamaBackendProvider();
        assertNotNull(provider, "Provider should be instantiable");
    }

    @Test
    void providerIsDiscoverableViaServiceLoader() {
        ServiceLoader<BackendProvider> loader = ServiceLoader.load(BackendProvider.class);
        boolean found = false;
        for (BackendProvider provider : loader) {
            if (provider instanceof PanamaBackendProvider) {
                found = true;
                break;
            }
        }
        assertTrue(found, "PanamaBackendProvider should be discoverable via ServiceLoader");
    }
}
