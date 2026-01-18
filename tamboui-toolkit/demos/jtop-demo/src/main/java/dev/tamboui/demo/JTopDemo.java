///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS dev.tamboui:tamboui-toolkit:LATEST
//DEPS dev.tamboui:tamboui-jline:LATEST
//DEPS com.github.oshi:oshi-core:6.9.2
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

/// JTop - A "top" alternative built with TamboUI DSL.
///
///
///
/// Displays system metrics including:
///
/// - CPU usage with toggleable views: bars, sparklines (all CPUs), or history chart
/// - Memory usage with sparkline chart showing history
/// - Top processes by CPU/memory/PID (sortable)
/// - System information
///
///
///
///
/// Controls:
///
/// - `c` - Toggle CPU view (bars → sparklines → chart → bars)
/// - `s` - Toggle sort mode (CPU → Memory → PID → CPU)
/// - `q` - Quit
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
                    text(" [c] CPU View ").dim(),
                    text(" [q] Quit ").dim()
                )).rounded().borderColor(Color.DARK_GRAY).length(3),
                systemMonitor
            ));
        }
    }
}

