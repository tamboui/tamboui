///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS dev.tamboui:tamboui-widgets:LATEST
//DEPS dev.tamboui:tamboui-jline:LATEST
//SOURCES JavaProcessMonitor.java JavaProcessInfo.java
//JAVA_OPTIONS --add-opens=jdk.attach/sun.tools.attach=ALL-UNNAMED --add-opens=jdk.internal.jvmstat/sun.jvmstat.perfdata.monitor.protocol.local=ALL-UNNAMED --add-opens=java.base/java.lang=ALL-UNNAMED
/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.demo;

import dev.tamboui.layout.Constraint;
import dev.tamboui.layout.Layout;
import dev.tamboui.layout.Rect;
import dev.tamboui.style.Color;
import dev.tamboui.style.Style;
import dev.tamboui.terminal.Backend;
import dev.tamboui.terminal.BackendFactory;
import dev.tamboui.terminal.Frame;
import dev.tamboui.terminal.Terminal;
import dev.tamboui.text.Line;
import dev.tamboui.text.Span;
import dev.tamboui.text.Text;
import dev.tamboui.widgets.block.Block;
import dev.tamboui.widgets.block.BorderType;
import dev.tamboui.widgets.block.Borders;
import dev.tamboui.widgets.block.Title;
import dev.tamboui.widgets.gauge.Gauge;
import dev.tamboui.widgets.list.ListItem;
import dev.tamboui.widgets.list.ListState;
import dev.tamboui.widgets.list.ListWidget;
import dev.tamboui.widgets.paragraph.Paragraph;
import dev.tamboui.widgets.sparkline.Sparkline;
import dev.tamboui.widgets.tabs.Tabs;
import dev.tamboui.widgets.tabs.TabsState;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * JTextVM - A TUI application for monitoring Java processes.
 * <p>
 * Displays a list of running Java processes on the left, and when a process
 * is selected, shows CPU, memory, and thread information on the right.
 * <p>
 * Controls:
 * <ul>
 *   <li>[↑/↓] or [j/k] - Navigate process list</li>
 *   <li>[r] - Refresh process list</li>
 *   <li>[q] - Quit</li>
 * </ul>
 */
public class JTextVM {

    private boolean running = true;
    private final ListState listState = new ListState();
    private final JavaProcessMonitor monitor = new JavaProcessMonitor();
    private List<JavaProcessInfo> processes = new ArrayList<>();
    private JavaProcessInfo selectedProcess = null;
    private Integer selectedPid = null; // Track selection by PID instead of index
    private boolean isLoadingProcessInfo = false;
    private String processInfoError = null; // Store error message if process info fails
    private volatile boolean needsRedraw = false; // Flag to trigger redraw when async data arrives
    private long lastRefresh = 0;
    private long lastProcessInfoRefresh = 0;
    private static final long REFRESH_INTERVAL_MS = 2000; // Refresh process list every 2 seconds
    private static final long PROCESS_INFO_REFRESH_INTERVAL_MS = 1000; // Refresh selected process info every 1 second
    
    // Error logging
    private final List<String> errorLog = new ArrayList<>();
    private boolean showErrorLog = false;
    private static final int MAX_ERROR_LOG_SIZE = 100;
    
    // History tracking for sparklines
    private static final int HISTORY_SIZE = 60; // Keep 60 data points (1 minute at 1 second intervals)
    private final Map<Integer, List<Long>> heapHistory = new HashMap<>();
    private final Map<Integer, List<Long>> nonHeapHistory = new HashMap<>();
    private final Map<Integer, List<Long>> threadHistory = new HashMap<>();
    
    // Tabs for dashboard views
    private final TabsState tabsState = new TabsState(0);
    private static final String[] TAB_NAMES = {"Overview", "Monitor", "Threads"};
    
    // Thread details cache
    private List<JavaProcessInfo.ThreadInfo> threadDetails = new ArrayList<>();
    private long lastThreadDetailsUpdate = 0;
    private static final long THREAD_DETAILS_UPDATE_INTERVAL = 2000; // Update every 2 seconds

    public static void main(String[] args) throws Exception {
        new JTextVM().run();
    }

    public void run() throws Exception {
        try (Backend backend = BackendFactory.create()) {
            backend.enableRawMode();
            backend.enterAlternateScreen();
            backend.hideCursor();

            Terminal<Backend> terminal = new Terminal<>(backend);

            // Handle resize
            backend.onResize(() -> {
                try {
                    terminal.draw(this::ui);
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });

            // Set error logger callback
            monitor.setErrorLogger(this::logError);
            
            // Initial refresh (async)
            refreshProcessesAsync();

            // Clear terminal before first draw
            try {
                terminal.clear();
            } catch (Exception e) {
                // Ignore clear errors
            }

            // Initial draw
            try {
                terminal.draw(this::ui);
            } catch (Exception e) {
                System.err.println("Error drawing UI: " + e.getMessage());
                e.printStackTrace();
                // Try to show error message
                backend.leaveAlternateScreen();
                backend.disableRawMode();
                backend.showCursor();
                System.err.println("Failed to render UI. Exiting.");
                return;
            }

            // Event loop
            while (running) {
                int c = backend.read(100);
                if (c == -2) {
                    // Timeout - refresh processes and selected process info periodically
                    long now = System.currentTimeMillis();
                    if (now - lastRefresh > REFRESH_INTERVAL_MS) {
                        refreshProcessesAsync();
                        lastRefresh = now;
                    }
                    // Refresh selected process info more frequently for real-time updates
                    if (selectedPid != null && now - lastProcessInfoRefresh > PROCESS_INFO_REFRESH_INTERVAL_MS) {
                        refreshSelectedProcessInfo();
                        lastProcessInfoRefresh = now;
                    }
                    // Always redraw to show loading states and async updates
                    try {
                        terminal.draw(this::ui);
                        needsRedraw = false; // Reset flag after redraw
                    } catch (Exception e) {
                        System.err.println("Error drawing UI: " + e.getMessage());
                    }
                    continue;
                }
                if (c == -1) {
                    continue;
                }

                boolean inputNeedsRedraw = handleInput(c, backend);
                if (inputNeedsRedraw || needsRedraw) {
                    try {
                        terminal.draw(this::ui);
                        needsRedraw = false;
                    } catch (Exception e) {
                        System.err.println("Error drawing UI: " + e.getMessage());
                    }
                }
            }
        }
    }

    private void refreshProcessesAsync() {
        // Run jps in a virtual thread to avoid blocking
        Thread.ofVirtual().start(() -> {
            try {
                List<JavaProcessInfo> newProcesses = monitor.getJavaProcesses();
                
                // Find the currently selected PID in the new list
                int newIndex = -1;
                if (selectedPid != null) {
                    for (int i = 0; i < newProcesses.size(); i++) {
                        if (newProcesses.get(i).pid() == selectedPid) {
                            newIndex = i;
                            break;
                        }
                    }
                }
                
                // Update processes list
                processes = newProcesses;
                
                // Maintain selection by PID
                if (newIndex >= 0) {
                    // Same process still exists, keep it selected
                    listState.select(newIndex);
                    // Only update process info if we don't already have it loaded
                    // (to avoid unnecessary reloads)
                    if (selectedProcess == null || selectedProcess.pid() != selectedPid) {
                        updateSelectedProcessAsync();
                    }
                } else {
                    // Selected process no longer exists
                    if (selectedPid != null) {
                        selectedPid = null;
                        selectedProcess = null;
                        isLoadingProcessInfo = false;
                        if (!processes.isEmpty()) {
                            // Select first item if list not empty
                            listState.selectFirst();
                            selectedPid = processes.get(0).pid();
                            updateSelectedProcessAsync();
                        } else {
                            listState.select(null);
                        }
                    } else if (!processes.isEmpty() && listState.selected() == null) {
                        // No selection and list not empty - select first
                        listState.selectFirst();
                        selectedPid = processes.get(0).pid();
                        updateSelectedProcessAsync();
                    } else {
                        // Clear selection
                        listState.select(null);
                    }
                }
            } catch (Exception e) {
                System.err.println("Error refreshing processes: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    private void updateSelectedProcessAsync() {
        Integer selected = listState.selected();
        if (selected != null && selected >= 0 && selected < processes.size()) {
            JavaProcessInfo basicInfo = processes.get(selected);
            int pid = basicInfo.pid();
            
            // Update selected PID if it changed
            if (selectedPid == null || selectedPid != pid) {
                selectedPid = pid;
                // Set loading flag for initial load
                isLoadingProcessInfo = true;
                selectedProcess = basicInfo; // Show basic info immediately
                // Clear thread details when switching processes
                threadDetails = new ArrayList<>();
                lastThreadDetailsUpdate = 0;
            }
            
            // Always refresh the process info (will be called periodically)
            refreshSelectedProcessInfo();
        } else {
            selectedPid = null;
            selectedProcess = null;
            isLoadingProcessInfo = false;
            processInfoError = null;
        }
    }
    
    // Track if a refresh is in progress to avoid multiple concurrent refreshes
    private volatile boolean refreshInProgress = false;
    
    /**
     * Refreshes the currently selected process info in the background.
     * This is called periodically to keep the data up-to-date.
     */
    private void refreshSelectedProcessInfo() {
        if (selectedPid == null) {
            return;
        }
        
        // Don't start a new refresh if one is already in progress
        if (refreshInProgress) {
            return;
        }
        
        // Find the basic info for this PID
        JavaProcessInfo basicInfo = null;
        for (JavaProcessInfo p : processes) {
            if (p.pid() == selectedPid) {
                basicInfo = p;
                break;
            }
        }
        
        if (basicInfo == null) {
            // Process no longer in list
            selectedPid = null;
            selectedProcess = null;
            isLoadingProcessInfo = false;
            return;
        }
        
        // Extract values to make them effectively final for lambda
        final int pid = selectedPid;
        final String mainClass = basicInfo.mainClass();
        final String arguments = basicInfo.arguments();
        
        // Only set loading flag if we don't have any data yet
        // If we have data, keep showing it until new data arrives
        if (selectedProcess == null || selectedProcess.heapMax() == 0) {
            isLoadingProcessInfo = true;
            processInfoError = null; // Clear previous error
        }
        
        // Mark refresh as in progress
        refreshInProgress = true;
        
        // Fetch detailed info asynchronously in a virtual thread
        Thread.ofVirtual().start(() -> {
            try {
                JavaProcessInfo updatedInfo = monitor.getProcessInfo(
                    pid,
                    mainClass,
                    arguments
                );
                // Only update if this is still the selected PID and we got valid data
                if (selectedPid != null && selectedPid == pid) {
                    // Only update if we got new data (heapMax > 0 means we got real data)
                    if (updatedInfo.heapMax() > 0 || updatedInfo.threadCount() > 0) {
                        selectedProcess = updatedInfo;
                        isLoadingProcessInfo = false;
                        processInfoError = null; // Clear error on success
                        
                        // Update history for sparklines
                        updateHistory(pid, updatedInfo);
                        
                        needsRedraw = true; // Trigger redraw on next event loop iteration
                    } else {
                        // No data received - check if we should show an error
                        if (selectedProcess == null || selectedProcess.heapMax() == 0) {
                            // We don't have any data yet, check if attachment failed
                            processInfoError = "Unable to retrieve process information. " +
                                "The process may not allow attachment or may have security restrictions.";
                            isLoadingProcessInfo = false;
                            needsRedraw = true;
                        }
                        // If we have old data, keep showing it
                    }
                }
            } catch (Exception e) {
                logError("Error fetching process info for PID " + pid + ": " + e.getMessage());
                // Set error message if we don't have data
                if (selectedPid != null && selectedPid == pid) {
                    if (selectedProcess == null || selectedProcess.heapMax() == 0) {
                        processInfoError = "Failed to get process info: " + e.getMessage();
                        isLoadingProcessInfo = false;
                        needsRedraw = true;
                    }
                    // If we have old data, keep showing it
                }
            } finally {
                refreshInProgress = false;
            }
        });
    }

    private boolean handleInput(int c, Backend backend) throws IOException {
        // Handle escape sequences (arrow keys, etc.)
        if (c == 27) { // ESC
            int next = backend.peek(50);
            if (next == '[') {
                backend.read(50); // consume '['
                int code = backend.read(50);
                return handleEscapeSequence(code);
            }
            return false;
        }

        return handleKey(c);
    }

    private boolean handleEscapeSequence(int code) {
        return switch (code) {
            case 'A' -> { // Up arrow
                listState.selectPrevious();
                updateSelectionFromListState();
                yield true;
            }
            case 'B' -> { // Down arrow
                listState.selectNext(processes.size());
                updateSelectionFromListState();
                yield true;
            }
            case 'C' -> { // Right arrow - switch to next tab
                tabsState.selectNext(TAB_NAMES.length);
                yield true;
            }
            case 'D' -> { // Left arrow - switch to previous tab
                tabsState.selectPrevious(TAB_NAMES.length);
                yield true;
            }
            default -> false;
        };
    }
    
    private void updateSelectionFromListState() {
        Integer selected = listState.selected();
        if (selected != null && selected >= 0 && selected < processes.size()) {
            int newPid = processes.get(selected).pid();
            // Only update if PID changed
            if (selectedPid == null || selectedPid != newPid) {
                updateSelectedProcessAsync();
            }
        } else {
            selectedPid = null;
            selectedProcess = null;
            isLoadingProcessInfo = false;
        }
    }

    private boolean handleKey(int c) {
        return switch (c) {
            case 'q', 'Q' -> {
                running = false;
                yield true;
            }
            case 'r', 'R' -> {
                refreshProcessesAsync();
                yield true;
            }
            case 'e', 'E' -> {
                showErrorLog = !showErrorLog;
                yield true;
            }
            case 'c', 'C' -> {
                // Clear error log
                if (showErrorLog) {
                    errorLog.clear();
                }
                yield true;
            }
            case 'j', 'J' -> {
                listState.selectNext(processes.size());
                updateSelectionFromListState();
                yield true;
            }
            case 'k', 'K' -> {
                listState.selectPrevious();
                updateSelectionFromListState();
                yield true;
            }
            case '\t' -> {
                // Tab key - switch to next tab in dashboard
                tabsState.selectNext(TAB_NAMES.length);
                yield true;
            }
            case '1' -> {
                tabsState.select(0);
                yield true;
            }
            case '2' -> {
                tabsState.select(1);
                yield true;
            }
            case '3' -> {
                tabsState.select(2);
                yield true;
            }
            default -> false;
        };
    }

    private void ui(Frame frame) {
        Rect area = frame.area();

        // Split into header, main content, and footer
        List<Rect> mainLayout = Layout.vertical()
            .constraints(
                Constraint.length(3),    // Header
                Constraint.fill(),       // Main content
                Constraint.length(3)     // Footer
            )
            .split(area);

        renderHeader(frame, mainLayout.get(0));
        renderMain(frame, mainLayout.get(1));
        renderFooter(frame, mainLayout.get(2));
    }

    private void renderHeader(Frame frame, Rect area) {
        Block headerBlock = Block.builder()
            .borders(Borders.ALL)
            .borderType(BorderType.ROUNDED)
            .borderStyle(Style.EMPTY.fg(Color.CYAN))
            .title(Title.from(
                Line.from(
                    Span.raw(" JTextVM ").bold().cyan(),
                    Span.raw("Java Process Monitor").yellow()
                )
            ).centered())
            .build();

        frame.renderWidget(headerBlock, area);
    }

    private void renderMain(Frame frame, Rect area) {
        if (showErrorLog) {
            // Show error log panel
            renderErrorLog(frame, area);
        } else {
            // Split main area into left (process list) and right (dashboard)
            List<Rect> mainPanels = Layout.horizontal()
                .constraints(
                    Constraint.percentage(40),  // Process list
                    Constraint.percentage(60)  // Dashboard
                )
                .spacing(1)
                .split(area);

            renderProcessList(frame, mainPanels.get(0));
            renderDashboard(frame, mainPanels.get(1));
        }
    }

    private void renderProcessList(Frame frame, Rect area) {
        List<ListItem> listItems;
        if (processes.isEmpty()) {
            listItems = List.of(
                ListItem.from("No Java processes found"),
                ListItem.from("Press 'r' to refresh")
            );
        } else {
            listItems = processes.stream()
                .map(p -> {
                    String display = String.format("%d - %s", p.pid(), p.displayName());
                    // Truncate if too long
                    if (display.length() > area.width() - 6) {
                        display = display.substring(0, area.width() - 9) + "...";
                    }
                    return ListItem.from(display);
                })
                .toList();
        }

        ListWidget list = ListWidget.builder()
            .items(listItems)
            .block(Block.builder()
                .borders(Borders.ALL)
                .borderType(BorderType.ROUNDED)
                .borderStyle(Style.EMPTY.fg(Color.GREEN))
                .title(Title.from(
                    Line.from(
                        Span.raw("Java Processes "),
                        Span.raw(String.format("(%d)", processes.size())).dim()
                    )
                ))
                .titleBottom(Title.from("↑/↓ navigate, r refresh").right())
                .build())
            .highlightStyle(Style.EMPTY.bg(Color.BLUE).fg(Color.WHITE).bold())
            .highlightSymbol("▶ ")
            .build();

        frame.renderStatefulWidget(list, area, listState);
    }

    private void renderDashboard(Frame frame, Rect area) {
        if (selectedProcess == null) {
            Text noSelectionText = Text.from(
                Line.from(Span.raw("No process selected").dim().italic())
            );

            Paragraph info = Paragraph.builder()
                .text(noSelectionText)
                .block(Block.builder()
                    .borders(Borders.ALL)
                    .borderType(BorderType.ROUNDED)
                    .borderStyle(Style.EMPTY.fg(Color.DARK_GRAY))
                    .title("Process Dashboard")
                    .build())
                .build();

            frame.renderWidget(info, area);
            return;
        }
        
        // Show loading indicator or error only if we have no data at all
        // If we have data, show it even if a refresh is in progress (avoids flicker)
        if (selectedProcess == null || (isLoadingProcessInfo && selectedProcess.heapMax() == 0 && selectedProcess.threadCount() == 0)) {
            List<Line> lines = new ArrayList<>();
            
            if (processInfoError != null) {
                // Show error message
                lines.add(Line.from(Span.raw("Error loading process information:").bold().red()));
                lines.add(Line.empty());
                // Split long error messages into multiple lines
                String[] errorParts = processInfoError.split("\\s+");
                StringBuilder currentLine = new StringBuilder();
                for (String part : errorParts) {
                    if (currentLine.length() + part.length() + 1 > area.width() - 6) {
                        if (currentLine.length() > 0) {
                            lines.add(Line.from(Span.raw("  " + currentLine.toString()).red()));
                            currentLine = new StringBuilder();
                        }
                    }
                    if (currentLine.length() > 0) {
                        currentLine.append(" ");
                    }
                    currentLine.append(part);
                }
                if (currentLine.length() > 0) {
                    lines.add(Line.from(Span.raw("  " + currentLine.toString()).red()));
                }
                lines.add(Line.empty());
                lines.add(Line.from(Span.raw("Press 'r' to retry").dim()));
            } else {
                // Show loading message
                lines.add(Line.from(Span.raw("Loading process information...").yellow().italic()));
            }

            Paragraph message = Paragraph.builder()
                .text(Text.from(lines))
                .block(Block.builder()
                    .borders(Borders.ALL)
                    .borderType(BorderType.ROUNDED)
                    .borderStyle(Style.EMPTY.fg(processInfoError != null ? Color.RED : Color.YELLOW))
                    .title("Process Dashboard")
                    .build())
                .build();

            frame.renderWidget(message, area);
            return;
        }

        // Split dashboard into tabs area and content area
        List<Rect> dashboardSections = Layout.vertical()
            .constraints(
                Constraint.length(3),  // Tabs
                Constraint.fill()       // Tab content
            )
            .spacing(0)
            .split(area);
        
        renderTabs(frame, dashboardSections.get(0));
        
        // Render content based on selected tab
        Integer selectedTab = tabsState.selected();
        int tabIndex = selectedTab != null ? selectedTab : 0;
        
        switch (tabIndex) {
            case 0 -> renderOverviewTab(frame, dashboardSections.get(1));
            case 1 -> renderMonitorTab(frame, dashboardSections.get(1));
            case 2 -> renderThreadsTab(frame, dashboardSections.get(1));
        }
    }

    private void renderProcessInfo(Frame frame, Rect area) {
        List<Line> lines = new ArrayList<>();
        lines.add(Line.from(
            Span.raw("PID: ").bold(),
            Span.raw(String.valueOf(selectedProcess.pid())).cyan()
        ));
        lines.add(Line.from(
            Span.raw("Main Class: ").bold(),
            Span.raw(selectedProcess.mainClass()).yellow()
        ));
        
        if (selectedProcess.uptime() > 0) {
            lines.add(Line.from(
                Span.raw("Uptime: ").bold(),
                Span.raw(selectedProcess.formatUptime()).green()
            ));
        } else {
            lines.add(Line.from(
                Span.raw("Uptime: ").bold(),
                Span.raw("calculating...").dim()
            ));
        }
        
        if (selectedProcess.cpuUsage() > 0 || !isLoadingProcessInfo) {
            lines.add(Line.from(
                Span.raw("CPU Usage: ").bold(),
                Span.raw(String.format("%.1f%%", selectedProcess.cpuUsage())).magenta()
            ));
        } else {
            lines.add(Line.from(
                Span.raw("CPU Usage: ").bold(),
                Span.raw("calculating...").dim()
            ));
        }
        
        Text infoText = Text.from(lines);

        Paragraph info = Paragraph.builder()
            .text(infoText)
            .block(Block.builder()
                .borders(Borders.ALL)
                .borderType(BorderType.ROUNDED)
                .borderStyle(Style.EMPTY.fg(Color.CYAN))
                .title("Process Information")
                .build())
            .build();

        frame.renderWidget(info, area);
    }

    private void renderHeapSparkline(Frame frame, Rect area) {
        double heapPercent = selectedProcess.heapUsagePercent();
        Color heapColor = heapPercent < 50 ? Color.GREEN : (heapPercent < 80 ? Color.YELLOW : Color.RED);
        
        List<Long> history = heapHistory.getOrDefault(selectedPid, List.of());
        
        String title = String.format("Heap Memory: %s / %s (%.1f%%)",
            selectedProcess.formatBytes(selectedProcess.heapUsed()),
            selectedProcess.formatBytes(selectedProcess.heapMax()),
            heapPercent);
        
        if (!history.isEmpty()) {
            // Calculate max from history for better visualization
            long maxInHistory = history.stream().mapToLong(Long::longValue).max().orElse(1);
            long currentValue = selectedProcess.heapUsed();
            // Use the larger of: max in history, current value, or heapMax (with some padding)
            long maxValue = Math.max(Math.max(maxInHistory, currentValue), selectedProcess.heapMax());
            // Add 10% padding for better visualization
            maxValue = (long) (maxValue * 1.1);
            
            long[] historyArray = history.stream().mapToLong(Long::longValue).toArray();
            Sparkline sparkline = Sparkline.builder()
                .data(historyArray)
                .max(maxValue)
                .style(Style.EMPTY.fg(heapColor))
                .barSet(Sparkline.BarSet.NINE_LEVELS)
                .block(Block.builder()
                    .borders(Borders.ALL)
                    .borderType(BorderType.ROUNDED)
                    .borderStyle(Style.EMPTY.fg(heapColor))
                    .title(Title.from(Line.from(Span.raw(title).fg(heapColor))))
                    .build())
                .build();
            
            frame.renderWidget(sparkline, area);
        } else {
            // No history yet, show placeholder
            Paragraph placeholder = Paragraph.builder()
                .text(Text.from(Line.from(Span.raw("Collecting heap memory data...").dim())))
                .block(Block.builder()
                    .borders(Borders.ALL)
                    .borderType(BorderType.ROUNDED)
                    .borderStyle(Style.EMPTY.fg(heapColor))
                    .title(Title.from(Line.from(Span.raw(title).fg(heapColor))))
                    .build())
                .build();
            frame.renderWidget(placeholder, area);
        }
    }
    
    private void renderNonHeapSparkline(Frame frame, Rect area) {
        double nonHeapPercent = selectedProcess.nonHeapUsagePercent();
        Color nonHeapColor = nonHeapPercent < 50 ? Color.GREEN : (nonHeapPercent < 80 ? Color.YELLOW : Color.RED);
        
        List<Long> history = nonHeapHistory.getOrDefault(selectedPid, List.of());
        
        String title = String.format("Non-Heap Memory: %s / %s (%.1f%%)",
            selectedProcess.formatBytes(selectedProcess.nonHeapUsed()),
            selectedProcess.formatBytes(selectedProcess.nonHeapMax()),
            nonHeapPercent);
        
        if (!history.isEmpty()) {
            // Calculate max from history for better visualization
            long maxInHistory = history.stream().mapToLong(Long::longValue).max().orElse(1);
            long currentValue = selectedProcess.nonHeapUsed();
            // Use the larger of: max in history, current value, or nonHeapMax (with some padding)
            long maxValue = Math.max(Math.max(maxInHistory, currentValue), selectedProcess.nonHeapMax());
            // Add 10% padding for better visualization
            maxValue = (long) (maxValue * 1.1);
            
            long[] historyArray = history.stream().mapToLong(Long::longValue).toArray();
            Sparkline sparkline = Sparkline.builder()
                .data(historyArray)
                .max(maxValue)
                .style(Style.EMPTY.fg(nonHeapColor))
                .barSet(Sparkline.BarSet.NINE_LEVELS)
                .block(Block.builder()
                    .borders(Borders.ALL)
                    .borderType(BorderType.ROUNDED)
                    .borderStyle(Style.EMPTY.fg(nonHeapColor))
                    .title(Title.from(Line.from(Span.raw(title).fg(nonHeapColor))))
                    .build())
                .build();
            
            frame.renderWidget(sparkline, area);
        } else {
            // No history yet, show placeholder
            Paragraph placeholder = Paragraph.builder()
                .text(Text.from(Line.from(Span.raw("Collecting non-heap memory data...").dim())))
                .block(Block.builder()
                    .borders(Borders.ALL)
                    .borderType(BorderType.ROUNDED)
                    .borderStyle(Style.EMPTY.fg(nonHeapColor))
                    .title(Title.from(Line.from(Span.raw(title).fg(nonHeapColor))))
                    .build())
                .build();
            frame.renderWidget(placeholder, area);
        }
    }

    private void renderThreadSparkline(Frame frame, Rect area) {
        List<Long> threadHist = threadHistory.getOrDefault(selectedPid, List.of());
        int currentThreads = selectedProcess.threadCount();
        
        String title = String.format("Threads: %d", currentThreads);
        
        if (!threadHist.isEmpty()) {
            long maxThreads = threadHist.stream().mapToLong(Long::longValue).max().orElse(1);
            // Add some padding to max for better visualization
            long maxValue = Math.max(maxThreads + 5, Math.max(currentThreads + 5, 10));
            
            long[] historyArray = threadHist.stream().mapToLong(Long::longValue).toArray();
            Sparkline sparkline = Sparkline.builder()
                .data(historyArray)
                .max(maxValue)
                .style(Style.EMPTY.fg(Color.MAGENTA))
                .barSet(Sparkline.BarSet.NINE_LEVELS)
                .block(Block.builder()
                    .borders(Borders.ALL)
                    .borderType(BorderType.ROUNDED)
                    .borderStyle(Style.EMPTY.fg(Color.MAGENTA))
                    .title(Title.from(Line.from(Span.raw(title).fg(Color.MAGENTA))))
                    .build())
                .build();
            
            frame.renderWidget(sparkline, area);
        } else {
            // No history yet, show placeholder
            Paragraph placeholder = Paragraph.builder()
                .text(Text.from(Line.from(Span.raw("Collecting thread data...").dim())))
                .block(Block.builder()
                    .borders(Borders.ALL)
                    .borderType(BorderType.ROUNDED)
                    .borderStyle(Style.EMPTY.fg(Color.MAGENTA))
                    .title(Title.from(Line.from(Span.raw(title).fg(Color.MAGENTA))))
                    .build())
                .build();
            frame.renderWidget(placeholder, area);
        }
    }
    
    /**
     * Updates history for sparklines when new process info arrives.
     */
    private void updateHistory(int pid, JavaProcessInfo info) {
        // Update heap history (store actual bytes used)
        List<Long> heapHist = heapHistory.computeIfAbsent(pid, k -> new ArrayList<>());
        heapHist.add(info.heapUsed());
        if (heapHist.size() > HISTORY_SIZE) {
            heapHist.remove(0);
        }
        
        // Update non-heap history (store actual bytes used)
        List<Long> nonHeapHist = nonHeapHistory.computeIfAbsent(pid, k -> new ArrayList<>());
        nonHeapHist.add(info.nonHeapUsed());
        if (nonHeapHist.size() > HISTORY_SIZE) {
            nonHeapHist.remove(0);
        }
        
        // Update thread history (store actual count)
        List<Long> threadHist = threadHistory.computeIfAbsent(pid, k -> new ArrayList<>());
        threadHist.add((long) info.threadCount());
        if (threadHist.size() > HISTORY_SIZE) {
            threadHist.remove(0);
        }
    }

    private void renderTabs(Frame frame, Rect area) {
        Line[] tabLines = new Line[TAB_NAMES.length];
        for (int i = 0; i < TAB_NAMES.length; i++) {
            String number = String.valueOf(i + 1);
            tabLines[i] = Line.from(
                Span.raw(number).dim(),
                Span.raw(":"),
                Span.raw(TAB_NAMES[i])
            );
        }

        Tabs tabs = Tabs.builder()
            .titles(tabLines)
            .highlightStyle(Style.EMPTY.fg(Color.YELLOW).bold())
            .style(Style.EMPTY.fg(Color.WHITE))
            .divider(Span.raw(" │ ").fg(Color.DARK_GRAY))
            .padding(" ", " ")
            .block(Block.builder()
                .borders(Borders.ALL)
                .borderType(BorderType.ROUNDED)
                .borderStyle(Style.EMPTY.fg(Color.CYAN))
                .build())
            .build();

        frame.renderStatefulWidget(tabs, area, tabsState);
    }
    
    private void renderOverviewTab(Frame frame, Rect area) {
        List<Line> lines = new ArrayList<>();
        lines.add(Line.from(
            Span.raw("PID: ").bold(),
            Span.raw(String.valueOf(selectedProcess.pid())).cyan()
        ));
        lines.add(Line.from(
            Span.raw("Main Class: ").bold(),
            Span.raw(selectedProcess.mainClass()).yellow()
        ));
        lines.add(Line.empty());
        
        if (!selectedProcess.arguments().isEmpty()) {
            lines.add(Line.from(Span.raw("Arguments:").bold()));
            // Split long arguments into multiple lines
            String args = selectedProcess.arguments();
            int maxWidth = area.width() - 4;
            int pos = 0;
            while (pos < args.length()) {
                int end = Math.min(pos + maxWidth, args.length());
                lines.add(Line.from(Span.raw("  " + args.substring(pos, end)).dim()));
                pos = end;
            }
            lines.add(Line.empty());
        }
        
        if (selectedProcess.uptime() > 0) {
            lines.add(Line.from(
                Span.raw("Uptime: ").bold(),
                Span.raw(selectedProcess.formatUptime()).green()
            ));
        } else {
            lines.add(Line.from(
                Span.raw("Uptime: ").bold(),
                Span.raw("calculating...").dim()
            ));
        }
        
        if (selectedProcess.cpuUsage() > 0 || !isLoadingProcessInfo) {
            lines.add(Line.from(
                Span.raw("CPU Usage: ").bold(),
                Span.raw(String.format("%.1f%%", selectedProcess.cpuUsage())).magenta()
            ));
        } else {
            lines.add(Line.from(
                Span.raw("CPU Usage: ").bold(),
                Span.raw("calculating...").dim()
            ));
        }
        
        lines.add(Line.empty());
        lines.add(Line.from(Span.raw("Memory:").bold()));
        lines.add(Line.from(
            Span.raw("  Heap: ").dim(),
            Span.raw(selectedProcess.formatBytes(selectedProcess.heapUsed())).green(),
            Span.raw(" / ").dim(),
            Span.raw(selectedProcess.formatBytes(selectedProcess.heapMax())).green(),
            Span.raw(String.format(" (%.1f%%)", selectedProcess.heapUsagePercent())).dim()
        ));
        lines.add(Line.from(
            Span.raw("  Non-Heap: ").dim(),
            Span.raw(selectedProcess.formatBytes(selectedProcess.nonHeapUsed())).yellow(),
            Span.raw(" / ").dim(),
            Span.raw(selectedProcess.formatBytes(selectedProcess.nonHeapMax())).yellow(),
            Span.raw(String.format(" (%.1f%%)", selectedProcess.nonHeapUsagePercent())).dim()
        ));
        
        lines.add(Line.empty());
        lines.add(Line.from(Span.raw("Threads:").bold()));
        lines.add(Line.from(
            Span.raw("  Total: ").dim(),
            Span.raw(String.valueOf(selectedProcess.threadCount())).cyan()
        ));
        lines.add(Line.from(
            Span.raw("  Live: ").dim(),
            Span.raw(String.valueOf(selectedProcess.liveThreadCount())).green()
        ));
        lines.add(Line.from(
            Span.raw("  Daemon: ").dim(),
            Span.raw(String.valueOf(selectedProcess.daemonThreadCount())).yellow()
        ));
        
        lines.add(Line.empty());
        lines.add(Line.from(Span.raw("Classes:").bold()));
        lines.add(Line.from(
            Span.raw("  Loaded: ").dim(),
            Span.raw(String.valueOf(selectedProcess.classesLoaded())).magenta()
        ));
        
        Text infoText = Text.from(lines);

        Paragraph info = Paragraph.builder()
            .text(infoText)
            .block(Block.builder()
                .borders(Borders.ALL)
                .borderType(BorderType.ROUNDED)
                .borderStyle(Style.EMPTY.fg(Color.CYAN))
                .title("Overview")
                .build())
            .build();

        frame.renderWidget(info, area);
    }
    
    private void renderMonitorTab(Frame frame, Rect area) {
        List<Rect> sections = Layout.vertical()
            .constraints(
                Constraint.length(6),   // CPU
                Constraint.length(6),   // Heap memory sparkline
                Constraint.length(6),   // Non-heap memory sparkline
                Constraint.length(6),   // Threads sparkline
                Constraint.length(6),   // Classes loaded
                Constraint.fill()       // Additional info
            )
            .spacing(1)
            .split(area);
        
        renderCPUInfo(frame, sections.get(0));
        renderHeapSparkline(frame, sections.get(1));
        renderNonHeapSparkline(frame, sections.get(2));
        renderThreadSparkline(frame, sections.get(3));
        renderClassesInfo(frame, sections.get(4));
        renderThreadSummary(frame, sections.get(5));
    }
    
    private void renderCPUInfo(Frame frame, Rect area) {
        double cpuPercent = selectedProcess.cpuUsage();
        Color cpuColor = cpuPercent < 50 ? Color.GREEN : (cpuPercent < 80 ? Color.YELLOW : Color.RED);
        
        String title = String.format("CPU Usage: %.1f%%", cpuPercent);
        
        Gauge cpuGauge = Gauge.builder()
            .ratio(cpuPercent / 100.0)
            .label(String.format("%.1f%%", cpuPercent))
            .gaugeStyle(Style.EMPTY.fg(cpuColor))
            .block(Block.builder()
                .borders(Borders.ALL)
                .borderType(BorderType.ROUNDED)
                .borderStyle(Style.EMPTY.fg(cpuColor))
                .title(Title.from(Line.from(Span.raw(title).fg(cpuColor))))
                .build())
            .build();
        
        frame.renderWidget(cpuGauge, area);
    }
    
    private void renderClassesInfo(Frame frame, Rect area) {
        long classesLoaded = selectedProcess.classesLoaded();
        
        Text classesText = Text.from(
            Line.from(Span.raw("Classes Loaded: ").bold()),
            Line.from(Span.raw(String.valueOf(classesLoaded)).fg(Color.MAGENTA).bold())
        );
        
        Paragraph classes = Paragraph.builder()
            .text(classesText)
            .block(Block.builder()
                .borders(Borders.ALL)
                .borderType(BorderType.ROUNDED)
                .borderStyle(Style.EMPTY.fg(Color.MAGENTA))
                .title("Classes")
                .build())
            .build();
        
        frame.renderWidget(classes, area);
    }
    
    private void renderThreadSummary(Frame frame, Rect area) {
        int total = selectedProcess.threadCount();
        int live = selectedProcess.liveThreadCount();
        int daemon = selectedProcess.daemonThreadCount();
        int nonDaemon = live - daemon;
        
        List<Line> lines = new ArrayList<>();
        lines.add(Line.from(
            Span.raw("Total Threads: ").bold(),
            Span.raw(String.valueOf(total)).cyan()
        ));
        lines.add(Line.from(
            Span.raw("Live Threads: ").bold(),
            Span.raw(String.valueOf(live)).green()
        ));
        lines.add(Line.from(
            Span.raw("Daemon Threads: ").bold(),
            Span.raw(String.valueOf(daemon)).yellow()
        ));
        lines.add(Line.from(
            Span.raw("Non-Daemon Threads: ").bold(),
            Span.raw(String.valueOf(nonDaemon)).magenta()
        ));
        
        Text threadText = Text.from(lines);
        
        Paragraph threadSummary = Paragraph.builder()
            .text(threadText)
            .block(Block.builder()
                .borders(Borders.ALL)
                .borderType(BorderType.ROUNDED)
                .borderStyle(Style.EMPTY.fg(Color.DARK_GRAY))
                .title("Thread Summary")
                .build())
            .build();
        
        frame.renderWidget(threadSummary, area);
    }
    
    private void renderThreadsTab(Frame frame, Rect area) {
        // Update thread details periodically
        long now = System.currentTimeMillis();
        if (selectedPid != null && (now - lastThreadDetailsUpdate > THREAD_DETAILS_UPDATE_INTERVAL)) {
            // Update asynchronously
            Thread.ofVirtual().start(() -> {
                try {
                    List<JavaProcessInfo.ThreadInfo> newDetails = monitor.getThreadDetails(selectedPid);
                    threadDetails = newDetails;
                    lastThreadDetailsUpdate = System.currentTimeMillis();
                    needsRedraw = true;
                } catch (Exception e) {
                    // Ignore errors
                }
            });
        }
        
        if (threadDetails.isEmpty()) {
            Paragraph message = Paragraph.builder()
                .text(Text.from(Line.from(Span.raw("Loading thread information...").yellow().italic())))
                .block(Block.builder()
                    .borders(Borders.ALL)
                    .borderType(BorderType.ROUNDED)
                    .borderStyle(Style.EMPTY.fg(Color.YELLOW))
                    .title("Threads")
                    .build())
                .build();
            frame.renderWidget(message, area);
            return;
        }
        
        // Count running vs total
        long runningCount = threadDetails.stream().filter(JavaProcessInfo.ThreadInfo::running).count();
        long totalCount = threadDetails.size();
        
        // Split into header and list
        List<Rect> sections = Layout.vertical()
            .constraints(
                Constraint.length(3),  // Header with stats
                Constraint.fill()       // Thread list
            )
            .spacing(1)
            .split(area);
        
        // Render header
        Text headerText = Text.from(
            Line.from(
                Span.raw("Running: ").bold(),
                Span.raw(String.valueOf(runningCount)).green(),
                Span.raw(" / ").dim(),
                Span.raw("Total: ").bold(),
                Span.raw(String.valueOf(totalCount)).cyan()
            )
        );
        
        Paragraph header = Paragraph.builder()
            .text(headerText)
            .block(Block.builder()
                .borders(Borders.ALL)
                .borderType(BorderType.ROUNDED)
                .borderStyle(Style.EMPTY.fg(Color.CYAN))
                .title("Threads")
                .build())
            .build();
        
        frame.renderWidget(header, sections.get(0));
        
        // Render thread list
        List<Line> threadLines = new ArrayList<>();
        int maxVisible = sections.get(1).height() - 2;
        int start = 0;
        int end = Math.min(threadDetails.size(), start + maxVisible);
        
        for (int i = start; i < end; i++) {
            JavaProcessInfo.ThreadInfo thread = threadDetails.get(i);
            String state = thread.running() ? "RUNNING" : thread.state();
            Color stateColor = thread.running() ? Color.GREEN : Color.YELLOW;
            
            // Create activity bar (simple visual indicator)
            int barWidth = 10;
            String activityBar = thread.running() ? "█".repeat(barWidth) : "░".repeat(barWidth);
            
            Line threadLine = Line.from(
                Span.raw(String.format("%-40s", thread.name().length() > 40 ? 
                    thread.name().substring(0, 37) + "..." : thread.name())).fg(Color.WHITE),
                Span.raw(" ").fg(Color.DARK_GRAY),
                Span.raw(activityBar).fg(stateColor),
                Span.raw(" ").fg(Color.DARK_GRAY),
                Span.raw(String.format("%-10s", state)).fg(stateColor),
                Span.raw(thread.daemon() ? " [D]" : "").fg(Color.YELLOW).dim()
            );
            threadLines.add(threadLine);
        }
        
        if (threadDetails.size() > maxVisible) {
            threadLines.add(Line.from(
                Span.raw(String.format("... and %d more threads", threadDetails.size() - maxVisible)).dim()
            ));
        }
        
        Text threadListText = Text.from(threadLines);
        
        Paragraph threadList = Paragraph.builder()
            .text(threadListText)
            .block(Block.builder()
                .borders(Borders.ALL)
                .borderType(BorderType.ROUNDED)
                .borderStyle(Style.EMPTY.fg(Color.DARK_GRAY))
                .build())
            .build();
        
        frame.renderWidget(threadList, sections.get(1));
    }

    private void renderFooter(Frame frame, Rect area) {
        Line helpLine;
        if (showErrorLog) {
            helpLine = Line.from(
                Span.raw("e").bold().yellow(),
                Span.raw(" Toggle log  ").dim(),
                Span.raw("c").bold().yellow(),
                Span.raw(" Clear log  ").dim(),
                Span.raw("q").bold().yellow(),
                Span.raw(" Quit").dim()
            );
        } else {
            int errorCount = errorLog.size();
            if (errorCount > 0) {
                // Show error count indicator
                helpLine = Line.from(
                    Span.raw(" ↑↓").bold().yellow(),
                    Span.raw(" Navigate  ").dim(),
                    Span.raw("Tab/→").bold().yellow(),
                    Span.raw(" Next tab  ").dim(),
                    Span.raw("r").bold().yellow(),
                    Span.raw(" Refresh  ").dim(),
                    Span.raw("e").bold().yellow(),
                    Span.raw(" Errors ").dim(),
                    Span.raw("(" + errorCount + ")").bold().red(),
                    Span.raw("  ").dim(),
                    Span.raw("q").bold().yellow(),
                    Span.raw(" Quit").dim()
                );
            } else {
                helpLine = Line.from(
                    Span.raw(" ↑↓").bold().yellow(),
                    Span.raw(" Navigate  ").dim(),
                    Span.raw("Tab/→").bold().yellow(),
                    Span.raw(" Next tab  ").dim(),
                    Span.raw("r").bold().yellow(),
                    Span.raw(" Refresh  ").dim(),
                    Span.raw("e").bold().yellow(),
                    Span.raw(" Errors  ").dim(),
                    Span.raw("q").bold().yellow(),
                    Span.raw(" Quit").dim()
                );
            }
        }

        Paragraph footer = Paragraph.builder()
            .text(Text.from(helpLine))
            .block(Block.builder()
                .borders(Borders.ALL)
                .borderType(BorderType.ROUNDED)
                .borderStyle(Style.EMPTY.fg(Color.DARK_GRAY))
                .build())
            .build();

        frame.renderWidget(footer, area);
    }
    
    private void renderErrorLog(Frame frame, Rect area) {
        List<Line> lines = new ArrayList<>();
        lines.add(Line.from(Span.raw("Error Log (" + errorLog.size() + " entries)").bold().red()));
        lines.add(Line.empty());
        
        if (errorLog.isEmpty()) {
            lines.add(Line.from(Span.raw("No errors logged").dim().italic()));
        } else {
            // Show most recent errors first (reverse order)
            int start = Math.max(0, errorLog.size() - (area.height() - 4));
            for (int i = start; i < errorLog.size(); i++) {
                String error = errorLog.get(i);
                // Truncate if too long
                if (error.length() > area.width() - 4) {
                    error = error.substring(0, area.width() - 7) + "...";
                }
                lines.add(Line.from(Span.raw("  " + error).fg(Color.RED)));
            }
        }
        
        Text logText = Text.from(lines);
        
        Paragraph logPanel = Paragraph.builder()
            .text(logText)
            .block(Block.builder()
                .borders(Borders.ALL)
                .borderType(BorderType.ROUNDED)
                .borderStyle(Style.EMPTY.fg(Color.RED))
                .title(Title.from(
                    Line.from(
                        Span.raw("Error Log "),
                        Span.raw("(Press 'e' to close, 'c' to clear)").dim()
                    )
                ))
                .build())
            .build();
        
        frame.renderWidget(logPanel, area);
    }
    
    private void logError(String error) {
        synchronized (errorLog) {
            errorLog.add("[" + System.currentTimeMillis() % 100000 + "] " + error);
            if (errorLog.size() > MAX_ERROR_LOG_SIZE) {
                errorLog.remove(0);
            }
            needsRedraw = true;
        }
    }
}

