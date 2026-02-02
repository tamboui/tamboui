/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.demo;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import dev.tamboui.layout.Constraint;
import dev.tamboui.layout.Layout;
import dev.tamboui.layout.Rect;
import dev.tamboui.style.Color;
import dev.tamboui.style.Style;
import dev.tamboui.terminal.Frame;
import dev.tamboui.text.Line;
import dev.tamboui.text.Span;
import dev.tamboui.toolkit.element.Element;
import dev.tamboui.toolkit.element.RenderContext;
import dev.tamboui.toolkit.event.EventResult;
import dev.tamboui.tui.event.KeyEvent;
import dev.tamboui.widgets.block.Block;
import dev.tamboui.widgets.block.BorderType;
import dev.tamboui.widgets.block.Borders;
import dev.tamboui.widgets.block.Title;
import dev.tamboui.widgets.chart.Axis;
import dev.tamboui.widgets.chart.Chart;
import dev.tamboui.widgets.chart.Dataset;
import dev.tamboui.widgets.chart.GraphType;

import static dev.tamboui.toolkit.Toolkit.column;
import static dev.tamboui.toolkit.Toolkit.gauge;
import static dev.tamboui.toolkit.Toolkit.panel;
import static dev.tamboui.toolkit.Toolkit.row;
import static dev.tamboui.toolkit.Toolkit.sparkline;
import static dev.tamboui.toolkit.Toolkit.text;

/**
 * UI component that displays system metrics.
 * Delegates system monitoring to {@link SystemMetrics}.
 */
final class SystemMonitor implements Element {

    /**
     * CPU display mode.
     */
    enum CpuViewMode {
        BARS,      // Show bars for each CPU
        SPARKLINES, // Show sparklines for all CPUs
        CHART      // Show history chart with average load
    }

    /**
     * Snapshot of metrics data for rendering.
     */
    private record RenderData(
        int numCpus,
        double[] coreUsage,
        List<List<Long>> coreHistories,
        long memTotal,
        long memUsed,
        double memoryRatio,
        List<Long> memoryHistory,
        double uptime,
        double loadAvg1,
        double loadAvg5,
        double loadAvg15,
        long swapUsed,
        List<SystemMetrics.ProcessInfo> processes
    ) {}

    private final SystemMetrics metrics = new SystemMetrics();
    private final AtomicReference<SystemMetrics.SortMode> sortMode =
        new AtomicReference<>(SystemMetrics.SortMode.CPU);
    private final AtomicReference<CpuViewMode> cpuViewMode =
        new AtomicReference<>(CpuViewMode.BARS);

    /**
     * Updates the metrics. Should be called from a background thread.
     */
    void updateMetrics() {
        metrics.update(sortMode.get());
    }

    @Override
    public void render(Frame frame, Rect area, RenderContext context) {
        // Read all metrics under the lock once
        var data = metrics.read(() -> new RenderData(
            metrics.numCpus(),
            copyUsageArray(metrics.numCpus()),
            metrics.allCoreHistories(),
            metrics.memTotal(),
            metrics.memUsed(),
            metrics.memoryRatio(),
            metrics.memoryHistory(),
            metrics.uptime(),
            metrics.loadAvg1(),
            metrics.loadAvg5(),
            metrics.loadAvg15(),
            metrics.swapUsed(),
            metrics.processes()
        ));

        var currentCpuViewMode = cpuViewMode.get();

        // Calculate charts height based on view mode
        int chartsHeight;
        if (currentCpuViewMode == CpuViewMode.SPARKLINES) {
            int maxHeight = (int) (area.height() * 0.8);
            chartsHeight = Math.min(data.numCpus() + 4, maxHeight);
        } else if (currentCpuViewMode == CpuViewMode.CHART) {
            chartsHeight = Math.max(12, Math.min(area.height() / 3, 20));
        } else {
            var maxChartsHeight = area.height() / 4;
            chartsHeight = Math.min(maxChartsHeight, Math.max(8, data.numCpus() + 4));
        }

        var layout = Layout.vertical()
            .constraints(Constraint.length(chartsHeight), Constraint.fill())
            .split(area);

        renderChartsSection(frame, layout.get(0), context, data, currentCpuViewMode);
        renderProcessList(frame, layout.get(1), context, data);
    }

    private double[] copyUsageArray(int numCpus) {
        var result = new double[numCpus];
        for (int i = 0; i < numCpus; i++) {
            result[i] = metrics.coreUsage(i);
        }
        return result;
    }

    private void renderChartsSection(Frame frame, Rect area, RenderContext context,
                                     RenderData data, CpuViewMode viewMode) {
        var layout = Layout.horizontal()
            .constraints(Constraint.percentage(35), Constraint.percentage(35), Constraint.percentage(30))
            .split(area);

        renderCpuChart(frame, layout.get(0), context, data, viewMode);
        renderMemoryChart(frame, layout.get(1), context, data);
        renderSystemInfo(frame, layout.get(2), context, data);
    }

    private void renderCpuChart(Frame frame, Rect area, RenderContext context,
                                RenderData data, CpuViewMode viewMode) {
        switch (viewMode) {
            case BARS -> renderCpuBars(frame, area, context, data);
            case SPARKLINES -> renderCpuSparklines(frame, area, context, data);
            case CHART -> renderCpuHistoryChart(frame, area, context, data);
        }
    }

    private void renderCpuBars(Frame frame, Rect area, RenderContext context, RenderData data) {
        var avgCpu = computeAverageCpu(data);
        var numCpus = data.numCpus();

        var maxCores = Math.max(1, area.height() - 2);
        var coresToShow = Math.min(numCpus, maxCores);

        var rows = new ArrayList<Element>();

        for (var i = 0; i < coresToShow; i++) {
            var usage = data.coreUsage()[i];
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
            ? String.format("CPU %d/%d (avg %.1f%%) [c]", coresToShow, numCpus, avgCpu)
            : String.format("CPU (avg %.1f%%) [c]", avgCpu);
        panel(title, content).rounded().render(frame, area, context);
    }

    private void renderCpuSparklines(Frame frame, Rect area, RenderContext context, RenderData data) {
        var avgCpu = computeAverageCpu(data);
        var numCpus = data.numCpus();

        var rows = new ArrayList<Element>();

        for (var i = 0; i < numCpus; i++) {
            var usage = data.coreUsage()[i];
            var history = data.coreHistories().get(i);
            var color = usage < 50 ? Color.GREEN : (usage < 80 ? Color.YELLOW : Color.RED);

            var historyArray = history.stream().mapToLong(Long::longValue).toArray();

            var sparklineElement = sparkline(historyArray)
                .max(100)
                .color(color)
                .barSet(dev.tamboui.widgets.sparkline.Sparkline.BarSet.NINE_LEVELS);

            rows.add(row(
                text(String.format("CPU%d ", i)).dim().length(6),
                sparklineElement,
                text(String.format(" %3.0f%%", usage)).length(6)
            ));
        }

        var content = column(rows.toArray(Element[]::new));
        var title = String.format("CPU (avg %.1f%%) [c]", avgCpu);
        panel(title, content).rounded().render(frame, area, context);
    }

    private void renderCpuHistoryChart(Frame frame, Rect area, RenderContext context, RenderData data) {
        var avgCpu = computeAverageCpu(data);
        var numCpus = data.numCpus();

        var avgHistory = computeAverageCpuHistory(data);
        if (avgHistory.isEmpty()) {
            renderCpuBars(frame, area, context, data);
            return;
        }

        var chartData = new double[avgHistory.size()][2];
        int index = 0;
        for (var value : avgHistory) {
            chartData[index][0] = index;
            chartData[index][1] = value;
            index++;
        }

        var dataset = Dataset.builder()
            .name("Avg CPU")
            .data(chartData)
            .graphType(GraphType.LINE)
            .marker(Dataset.Marker.DOT)
            .style(Style.EMPTY.fg(Color.CYAN))
            .build();

        int historySize = avgHistory.size();
        var xLabels = new ArrayList<String>();
        if (historySize > 0) {
            xLabels.add("0");
            if (historySize > 1) {
                xLabels.add(String.valueOf(historySize / 4));
                xLabels.add(String.valueOf(historySize / 2));
                xLabels.add(String.valueOf(3 * historySize / 4));
                xLabels.add(String.valueOf(historySize - 1));
            }
        }

        var chart = Chart.builder()
            .datasets(dataset)
            .xAxis(Axis.builder()
                .title("Time")
                .bounds(0, Math.max(1, historySize - 1))
                .labels(xLabels.toArray(new String[0]))
                .build())
            .yAxis(Axis.builder()
                .title("Usage %")
                .bounds(0, 100)
                .labels("0", "25", "50", "75", "100")
                .build())
            .hideLegend()
            .block(Block.builder()
                .borders(Borders.ALL)
                .borderType(BorderType.ROUNDED)
                .borderStyle(Style.EMPTY.fg(Color.CYAN))
                .title(Title.from(Line.from(
                    Span.raw(String.format(" CPU History (avg %.1f%%, %d cores) [c] ", avgCpu, numCpus)).cyan()
                )))
                .build())
            .build();

        frame.renderWidget(chart, area);
    }

    private double computeAverageCpu(RenderData data) {
        double avg = 0;
        for (int i = 0; i < data.numCpus(); i++) {
            avg += data.coreUsage()[i];
        }
        return data.numCpus() > 0 ? avg / data.numCpus() : 0;
    }

    private List<Double> computeAverageCpuHistory(RenderData data) {
        var numCpus = data.numCpus();
        if (numCpus == 0) {
            return List.of();
        }

        var histories = data.coreHistories();
        if (histories.isEmpty() || histories.get(0).isEmpty()) {
            return List.of();
        }

        int historySize = histories.get(0).size();
        var result = new ArrayList<Double>(historySize);

        for (int timeIndex = 0; timeIndex < historySize; timeIndex++) {
            double sum = 0;
            int count = 0;
            for (var history : histories) {
                if (timeIndex < history.size()) {
                    sum += history.get(timeIndex);
                    count++;
                }
            }
            result.add(count > 0 ? sum / count : 0.0);
        }

        return result;
    }

    private void renderMemoryChart(Frame frame, Rect area, RenderContext context, RenderData data) {
        var ratio = data.memoryRatio();
        var memPercent = ratio * 100;
        var gaugeColor = ratio < 0.5 ? Color.GREEN : (ratio < 0.8 ? Color.YELLOW : Color.RED);
        var label = String.format("%s / %s", formatKb(data.memUsed()), formatKb(data.memTotal()));

        var memHistory = data.memoryHistory();
        if (memHistory.isEmpty()) {
            gauge(ratio)
                .label(label)
                .gaugeColor(gaugeColor)
                .title(String.format("Memory %.0f%%", memPercent))
                .rounded()
                .render(frame, area, context);
            return;
        }

        var chartData = new double[memHistory.size()][2];
        int index = 0;
        for (var value : memHistory) {
            chartData[index][0] = index;
            chartData[index][1] = value;
            index++;
        }

        var dataset = Dataset.builder()
            .name("Memory")
            .data(chartData)
            .graphType(GraphType.LINE)
            .marker(Dataset.Marker.DOT)
            .style(Style.EMPTY.fg(gaugeColor))
            .build();

        int historySize = memHistory.size();
        var xLabels = new ArrayList<String>();
        if (historySize > 0) {
            xLabels.add("0");
            if (historySize > 1) {
                xLabels.add(String.valueOf(historySize / 4));
                xLabels.add(String.valueOf(historySize / 2));
                xLabels.add(String.valueOf(3 * historySize / 4));
                xLabels.add(String.valueOf(historySize - 1));
            }
        }

        var chart = Chart.builder()
            .datasets(dataset)
            .xAxis(Axis.builder()
                .title("Time")
                .bounds(0, Math.max(1, historySize - 1))
                .labels(xLabels.toArray(new String[0]))
                .build())
            .yAxis(Axis.builder()
                .title("Usage %")
                .bounds(0, 100)
                .labels("0", "25", "50", "75", "100")
                .build())
            .hideLegend()
            .block(Block.builder()
                .borders(Borders.ALL)
                .borderType(BorderType.ROUNDED)
                .borderStyle(Style.EMPTY.fg(gaugeColor))
                .title(Title.from(Line.from(
                    Span.raw(String.format(" Memory %.0f%% - %s ", memPercent, label)).fg(gaugeColor)
                )))
                .build())
            .build();

        frame.renderWidget(chart, area);
    }

    private void renderSystemInfo(Frame frame, Rect area, RenderContext context, RenderData data) {
        var availableLines = area.height() - 2;
        var rows = new ArrayList<Element>();

        if (availableLines >= 1) {
            rows.add(text(String.format(" Load: %.1f %.1f %.1f",
                data.loadAvg1(), data.loadAvg5(), data.loadAvg15())));
        }
        if (availableLines >= 2) {
            rows.add(text(" Up: " + formatUptime((long) data.uptime())).dim());
        }
        if (availableLines >= 3) {
            rows.add(text(" CPUs: " + data.numCpus() + " Swap: " + formatKb(data.swapUsed())).dim());
        }

        var content = column(rows.toArray(Element[]::new));
        panel("System", content).rounded().render(frame, area, context);
    }

    private void renderProcessList(Frame frame, Rect area, RenderContext context, RenderData data) {
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

        var processes = data.processes();
        var maxRows = Math.max(0, area.height() - 4);

        int fixedColumnsWidth = 8 + 10 + 3 + 10 + 2;
        int commandMaxWidth = Math.max(20, area.width() - fixedColumnsWidth - 4);

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
            if (name.length() > commandMaxWidth) {
                name = name.substring(0, commandMaxWidth - 3) + "...";
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
        var currentSortMode = sortMode.get();
        var title = String.format("Processes (%d) - Press [s] to sort by %s",
            processes.size(),
            switch (currentSortMode) {
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
        if (event.isCharIgnoreCase('s')) {
            sortMode.updateAndGet(mode -> switch (mode) {
                case CPU -> SystemMetrics.SortMode.MEMORY;
                case MEMORY -> SystemMetrics.SortMode.PID;
                case PID -> SystemMetrics.SortMode.CPU;
            });
            return EventResult.HANDLED;
        }
        if (event.isCharIgnoreCase('c')) {
            cpuViewMode.updateAndGet(mode -> switch (mode) {
                case BARS -> CpuViewMode.SPARKLINES;
                case SPARKLINES -> CpuViewMode.CHART;
                case CHART -> CpuViewMode.BARS;
            });
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
