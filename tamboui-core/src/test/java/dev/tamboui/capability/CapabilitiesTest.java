/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.capability;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CapabilitiesTest {

    @Test
    void loadsProvidersViaServiceLoader_and_buildsReport() {
        CapabilityReport report = Capabilities.detect();
        assertThat(report.feature("tamboui-core", "capabilityProvider.loadError.0", String.class))
                .isPresent();
        assertThat(report.feature("tamboui-core", "backend.provider_count", Integer.class))
                .contains(1);
        assertThat(report.feature("tamboui-core", "backend.provider_names", String.class))
                .hasValueSatisfying(v -> assertThat(v).contains("test"));
        assertThat(report.feature("tamboui-core", "backend.provider_error.0", String.class))
                .isPresent();
        assertThat(report.feature("tamboui-test", "foo", String.class)).contains("bar");
        assertThat(report.feature("tamboui-test", "answer", Integer.class)).contains(42);
        assertThat(report.feature("tamboui-test", "feature.a", Boolean.class)).contains(true);
        assertThat(report.feature("tamboui-test", "feature.b", Boolean.class)).contains(false);
    }
}


