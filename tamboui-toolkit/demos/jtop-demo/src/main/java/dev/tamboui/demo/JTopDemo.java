///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS dev.tamboui:tamboui-toolkit:LATEST
//DEPS dev.tamboui:tamboui-jline:LATEST
//SOURCES SystemMetrics.java SystemMonitor.java

/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.demo;

import dev.tamboui.toolkit.app.ToolkitRunner;
import dev.tamboui.style.Color;
import dev.tamboui.tui.TuiConfig;

import java.time.Duration;

import static dev.tamboui.toolkit.Toolkit.*;

/**
 * JTop - A "top" alternative built with TamboUI DSL.
 * <p>
 * Displays system metrics including:
 * <ul>
 *   <li>CPU usage with history sparkline</li>
 *   <li>Memory usage with gauge</li>
 *   <li>Top processes by CPU/memory</li>
 *   <li>System information</li>
 * </ul>
 */
public class JTopDemo {

    public static void main(String[] args) throws Exception {
        var config = TuiConfig.builder()
            .tickRate(Duration.ofMillis(500))
            .build();

        // Create stateful component outside the render supplier
        var systemMonitor = new SystemMonitor();

        try (var runner = ToolkitRunner.create(config)) {
            runner.run(() -> column(
                panel(() -> row(
                    text(" JTop - System Monitor ").bold().cyan(),
                    spacer(),
                    text(" [s] Sort ").dim(),
                    text(" [q] Quit ").dim()
                )).rounded().borderColor(Color.DARK_GRAY).length(3),
                systemMonitor
            ));
        }
    }
}
