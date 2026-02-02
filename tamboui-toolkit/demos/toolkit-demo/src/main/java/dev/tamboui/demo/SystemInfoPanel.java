/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.demo;

import java.util.function.Supplier;

import dev.tamboui.style.Color;
import dev.tamboui.toolkit.element.Element;

import static dev.tamboui.toolkit.Toolkit.*;

/**
 * A panel displaying system information.
 */
final class SystemInfoPanel extends PanelContent {
    private final Supplier<String> uptimeSupplier;

    SystemInfoPanel(Supplier<String> uptimeSupplier) {
        super("[System]", 28, 6, Color.MAGENTA);
        this.uptimeSupplier = uptimeSupplier;
    }

    @Override
    Element render(boolean focused) {
        var rt = Runtime.getRuntime();
        var usedMb = (rt.totalMemory() - rt.freeMemory()) / 1024 / 1024;
        var maxMb = rt.maxMemory() / 1024 / 1024;
        var cpus = rt.availableProcessors();
        var javaVersion = System.getProperty("java.version");

        return column(
            row(text("Java:   ").dim(), text(javaVersion).green()),
            row(text("CPUs:   ").dim(), text(String.valueOf(cpus)).cyan()),
            row(text("Memory: ").dim(), text(usedMb + "/" + maxMb + " MB").yellow()),
            row(text("Uptime: ").dim(), text(uptimeSupplier.get()).magenta())
        );
    }
}
