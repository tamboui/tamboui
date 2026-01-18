/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.image.capability;

import dev.tamboui.capability.CapabilityProvider;
import org.junit.jupiter.api.Test;

import java.util.ServiceLoader;

import static org.assertj.core.api.Assertions.assertThat;

class ImageCapabilityProviderTest {

    @Test
    void providerIsDiscoverableViaServiceLoader() {
        ServiceLoader<CapabilityProvider> loader = ServiceLoader.load(CapabilityProvider.class);
        boolean found = false;
        for (CapabilityProvider provider : loader) {
            if (provider instanceof ImageCapabilityProvider) {
                found = true;
                break;
            }
        }
        assertThat(found).isTrue();
    }
}


