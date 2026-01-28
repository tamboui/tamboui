///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS dev.tamboui:tamboui-toolkit:LATEST
//DEPS dev.tamboui:tamboui-jline3-backend:LATEST
//DEPS com.github.oshi:oshi-core:6.9.2
//SOURCES SystemMetrics.java SystemMonitor.java

/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.demo;

import dev.tamboui.toolkit.app.ToolkitRunner;
import dev.tamboui.style.Color;
import dev.tamboui.tui.TuiConfig;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

import static dev.tamboui.toolkit.Toolkit.*;

/**
 * JTop - A "top" alternative built with TamboUI DSL.
 * <p>
 * Displays system metrics including:
 * <ul>
 *   <li>CPU usage with toggleable views: bars, sparklines (all CPUs), or history chart</li>
 *   <li>Memory usage with sparkline chart showing history</li>
 *   <li>Top processes by CPU/memory/PID (sortable)</li>
 *   <li>System information</li>
 * </ul>
 * <p>
 * Controls:
 * <ul>
 *   <li>[c] - Toggle CPU view (bars → sparklines → chart → bars)</li>
 *   <li>[s] - Toggle sort mode (CPU → Memory → PID → CPU)</li>
 *   <li>[q] - Quit</li>
 * </ul>
 */
public class JTopDemo {

    private static final Duration UPDATE_INTERVAL = Duration.ofMillis(500);

    private JTopDemo() {

    }

    /**
     * Demo entry point.
     * @param args the CLI arguments
     * @throws Exception on unexpected error
     */
    public static void main(String[] args) throws Exception {
        var config = TuiConfig.builder()
            .build();

        // Create stateful component outside the render supplier
        var systemMonitor = new SystemMonitor();

        try (var runner = ToolkitRunner.create(config)) {
            // Schedule metrics updates on a background thread
            runner.scheduleWithFixedDelay(systemMonitor::updateMetrics, UPDATE_INTERVAL);

            runner.run(() -> column(
                panel(() -> row(
                    text(" JTop - System Monitor ").bold().cyan(),
                    spacer(),
                    text(" [s] Sort ").dim(),
                    text(" [c] CPU View ").dim(),
                    text(" [q] Quit ").dim()
                )).rounded().borderColor(Color.DARK_GRAY).length(3),
                systemMonitor
            ));
        }
    }
}
