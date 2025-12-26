/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.demo;

import dev.tamboui.toolkit.element.Element;
import dev.tamboui.toolkit.element.RenderContext;
import dev.tamboui.toolkit.event.EventResult;
import dev.tamboui.layout.Constraint;
import dev.tamboui.layout.Layout;
import dev.tamboui.layout.Rect;
import dev.tamboui.style.Color;
import dev.tamboui.terminal.Frame;
import dev.tamboui.tui.Keys;
import dev.tamboui.tui.event.KeyEvent;

import java.time.Duration;
import java.util.ArrayList;

import static dev.tamboui.toolkit.Toolkit.*;

/**
 * UI component that displays system metrics.
 * Delegates system monitoring to {@link SystemMetrics}.
 */
final class SystemMonitor implements Element {

    private final SystemMetrics metrics = new SystemMetrics();
    private SystemMetrics.SortMode sortMode = SystemMetrics.SortMode.CPU;

    @Override
    public void render(Frame frame, Rect area, RenderContext context) {
        metrics.update(sortMode);

        // Layout: top section for charts, capped at 25% of available height
        var maxChartsHeight = area.height() / 4;
        var chartsHeight = Math.min(maxChartsHeight, Math.max(8, metrics.numCpus() + 4));
        var layout = Layout.vertical()
            .constraints(Constraint.length(chartsHeight), Constraint.fill())
            .split(area);

        renderChartsSection(frame, layout.get(0), context);
        renderProcessList(frame, layout.get(1), context);
    }

    private void renderChartsSection(Frame frame, Rect area, RenderContext context) {
        var layout = Layout.horizontal()
            .constraints(Constraint.percentage(35), Constraint.percentage(35), Constraint.percentage(30))
            .split(area);

        renderCpuChart(frame, layout.get(0), context);
        renderMemoryChart(frame, layout.get(1), context);
        renderSystemInfo(frame, layout.get(2), context);
    }

    private void renderCpuChart(Frame frame, Rect area, RenderContext context) {
        var avgCpu = metrics.averageCpuUsage();
        var numCpus = metrics.numCpus();

        // Calculate how many cores can fit (area height - 2 for borders)
        var maxCores = Math.max(1, area.height() - 2);
        var coresToShow = Math.min(numCpus, maxCores);

        var rows = new ArrayList<Element>();

        for (var i = 0; i < coresToShow; i++) {
            var usage = metrics.coreUsage(i);
            var color = usage < 50 ? Color.GREEN : (usage < 80 ? Color.YELLOW : Color.RED);

            var barWidth = Math.max(10, area.width() - 16);
            var filled = (int) (usage * barWidth / 100);
            var bar = "█".repeat(filled) + "░".repeat(barWidth - filled);

            rows.add(row(
                text(String.format("CPU%d ", i)).dim().length(5),
                text(bar).fg(color),
                text(String.format(" %3.0f%%", usage)).length(5)
            ));
        }

        var content = column(rows.toArray(Element[]::new));
        var title = coresToShow < numCpus
            ? String.format("CPU %d/%d (avg %.1f%%)", coresToShow, numCpus, avgCpu)
            : String.format("CPU (avg %.1f%%)", avgCpu);
        panel(title, content).rounded().render(frame, area, context);
    }

    private void renderMemoryChart(Frame frame, Rect area, RenderContext context) {
        var ratio = metrics.memoryRatio();
        var gaugeColor = ratio < 0.5 ? Color.GREEN : (ratio < 0.8 ? Color.YELLOW : Color.RED);
        var label = String.format("%s / %s", formatKb(metrics.memUsed()), formatKb(metrics.memTotal()));
        var title = String.format("Memory %.0f%%", ratio * 100);

        gauge(ratio)
            .label(label)
            .gaugeColor(gaugeColor)
            .title(title)
            .rounded()
            .render(frame, area, context);
    }

    private void renderSystemInfo(Frame frame, Rect area, RenderContext context) {
        var availableLines = area.height() - 2;
        var rows = new ArrayList<Element>();

        if (availableLines >= 1) {
            rows.add(text(String.format(" Load: %.1f %.1f %.1f",
                metrics.loadAvg1(), metrics.loadAvg5(), metrics.loadAvg15())));
        }
        if (availableLines >= 2) {
            rows.add(text(" Up: " + formatUptime((long) metrics.uptime())).dim());
        }
        if (availableLines >= 3) {
            rows.add(text(" CPUs: " + metrics.numCpus() + " Swap: " + formatKb(metrics.swapUsed())).dim());
        }

        var content = column(rows.toArray(Element[]::new));
        panel("System", content).rounded().render(frame, area, context);
    }

    private void renderProcessList(Frame frame, Rect area, RenderContext context) {
        var header = row(
            text(" PID").fg(Color.DARK_GRAY).length(8),
            text("USER").fg(Color.DARK_GRAY).length(10),
            text("S").fg(Color.DARK_GRAY).length(3),
            text("MEM").fg(Color.DARK_GRAY).length(10),
            text("COMMAND").fg(Color.DARK_GRAY)
        );

        var rows = new ArrayList<Element>();
        rows.add(header);
        rows.add(text("-".repeat(Math.max(1, area.width() - 4))).dim());

        var processes = metrics.processes();
        var maxRows = Math.max(0, area.height() - 4);

        for (var i = 0; i < Math.min(processes.size(), maxRows); i++) {
            var p = processes.get(i);
            var stateColor = switch (p.state()) {
                case 'R' -> Color.GREEN;
                case 'S' -> Color.GRAY;
                case 'D' -> Color.RED;
                case 'Z' -> Color.MAGENTA;
                case 'T' -> Color.YELLOW;
                default -> Color.GRAY;
            };

            var name = p.name();
            if (name.length() > 30) {
                name = name.substring(0, 27) + "...";
            }

            rows.add(row(
                text(String.format(" %6d", p.pid())).length(8),
                text(p.user().length() > 8 ? p.user().substring(0, 8) : p.user()).length(10),
                text(String.valueOf(p.state())).fg(stateColor).length(3),
                text(formatKb(p.memoryKb())).length(10),
                text(name)
            ));
        }

        var content = column(rows.toArray(Element[]::new));
        var title = String.format("Processes (%d) - Press [s] to sort by %s",
            processes.size(),
            switch (sortMode) {
                case CPU -> "Memory";
                case MEMORY -> "PID";
                case PID -> "CPU";
            });
        panel(title, content).rounded().render(frame, area, context);
    }

    @Override
    public Constraint constraint() {
        return Constraint.fill();
    }

    @Override
    public EventResult handleKeyEvent(KeyEvent event, boolean focused) {
        if (Keys.isChar(event, 's') || Keys.isChar(event, 'S')) {
            sortMode = switch (sortMode) {
                case CPU -> SystemMetrics.SortMode.MEMORY;
                case MEMORY -> SystemMetrics.SortMode.PID;
                case PID -> SystemMetrics.SortMode.CPU;
            };
            return EventResult.HANDLED;
        }
        return EventResult.UNHANDLED;
    }

    private static String formatKb(long kb) {
        if (kb < 1024) {
            return kb + "K";
        }
        if (kb < 1024 * 1024) {
            return String.format("%.1fM", kb / 1024.0);
        }
        return String.format("%.1fG", kb / (1024.0 * 1024));
    }

    private static String formatUptime(long seconds) {
        var d = Duration.ofSeconds(seconds);
        var days = d.toDays();
        long hours = d.toHoursPart();
        long minutes = d.toMinutesPart();
        if (days > 0) {
            return String.format("%dd %02d:%02d", days, hours, minutes);
        }
        return String.format("%02d:%02d:%02d", hours, minutes, d.toSecondsPart());
    }
}
