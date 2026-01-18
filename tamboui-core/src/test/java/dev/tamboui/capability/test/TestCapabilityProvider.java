/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.capability.test;

import dev.tamboui.capability.CapabilityProvider;
import dev.tamboui.capability.CapabilityReportBuilder;

public final class TestCapabilityProvider implements CapabilityProvider {

    @Override
    public String source() {
        return "tamboui-test";
    }

    @Override
    public void contribute(CapabilityReportBuilder report) {
        report.feature(source(), "foo", "bar");
        report.feature(source(), "answer", 42);
        report.feature(source(), "feature.a", true);
        report.feature(source(), "feature.b", false);
    }
}


