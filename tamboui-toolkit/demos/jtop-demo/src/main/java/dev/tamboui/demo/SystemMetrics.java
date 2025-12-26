/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.demo;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Reads system metrics from /proc on Linux.
 * Separates system monitoring logic from UI rendering.
 */
final class SystemMetrics {

    private static final int HISTORY_SIZE = 60;
    private static final Path PROC_STAT = Path.of("/proc/stat");
    private static final Path PROC_MEMINFO = Path.of("/proc/meminfo");
    private static final Path PROC_UPTIME = Path.of("/proc/uptime");
    private static final Path PROC_LOADAVG = Path.of("/proc/loadavg");
    private static final Path PROC = Path.of("/proc");
    private static final Pattern STAT_PATTERN = Pattern.compile("^(\\d+) \\((.*)\\) (\\S).*");

    // CPU tracking
    private final int numCpus;
    private final double[] coreUsage;
    private final long[] lastCoreTotal;
    private final long[] lastCoreIdle;
    private final List<Deque<Long>> coreHistory;

    // Memory tracking
    private final Deque<Long> memoryHistory = new ArrayDeque<>(HISTORY_SIZE);
    private long memTotal = 0;
    private long memAvailable = 0;
    private long memUsed = 0;
    private long swapTotal = 0;
    private long swapFree = 0;

    // System info
    private double uptime = 0;
    private double loadAvg1 = 0;
    private double loadAvg5 = 0;
    private double loadAvg15 = 0;

    // Process list
    private List<ProcessInfo> processes = new ArrayList<>();

    /**
     * Information about a running process.
     */
    record ProcessInfo(
        int pid,
        String name,
        char state,
        double cpuPercent,
        long memoryKb,
        String user
    ) {}

    /**
     * Sort modes for the process list.
     */
    enum SortMode {
        CPU, MEMORY, PID
    }

    SystemMetrics() {
        numCpus = Runtime.getRuntime().availableProcessors();
        coreUsage = new double[numCpus];
        lastCoreTotal = new long[numCpus];
        lastCoreIdle = new long[numCpus];
        coreHistory = new ArrayList<>(numCpus);

        for (var i = 0; i < numCpus; i++) {
            var history = new ArrayDeque<Long>(HISTORY_SIZE);
            for (var j = 0; j < HISTORY_SIZE; j++) {
                history.add(0L);
            }
            coreHistory.add(history);
        }

        for (var i = 0; i < HISTORY_SIZE; i++) {
            memoryHistory.add(0L);
        }
    }

    /**
     * Updates all metrics by reading from /proc.
     */
    void update(SortMode sortMode) {
        updateCpuUsage();
        updateMemoryInfo();
        updateSystemInfo();
        updateProcessList(sortMode);
    }

    // ==================== Accessors ====================

    int numCpus() {
        return numCpus;
    }

    double coreUsage(int core) {
        return core >= 0 && core < numCpus ? coreUsage[core] : 0;
    }

    double averageCpuUsage() {
        double avg = 0;
        for (var i = 0; i < numCpus; i++) {
            avg += coreUsage[i];
        }
        return avg / numCpus;
    }

    Deque<Long> coreHistory(int core) {
        return core >= 0 && core < numCpus ? coreHistory.get(core) : new ArrayDeque<>();
    }

    long memTotal() {
        return memTotal;
    }

    long memUsed() {
        return memUsed;
    }

    long memAvailable() {
        return memAvailable;
    }

    long swapTotal() {
        return swapTotal;
    }

    long swapFree() {
        return swapFree;
    }

    long swapUsed() {
        return swapTotal - swapFree;
    }

    double memoryRatio() {
        return memTotal > 0 ? (double) memUsed / memTotal : 0;
    }

    Deque<Long> memoryHistory() {
        return memoryHistory;
    }

    double uptime() {
        return uptime;
    }

    double loadAvg1() {
        return loadAvg1;
    }

    double loadAvg5() {
        return loadAvg5;
    }

    double loadAvg15() {
        return loadAvg15;
    }

    List<ProcessInfo> processes() {
        return processes;
    }

    // ==================== Update Methods ====================

    private void updateCpuUsage() {
        try {
            var lines = Files.readAllLines(PROC_STAT);
            for (var line : lines) {
                if (line.startsWith("cpu") && !line.startsWith("cpu ")) {
                    var parts = line.split("\\s+");
                    var coreId = Integer.parseInt(parts[0].substring(3));
                    if (coreId >= numCpus) {
                        continue;
                    }

                    var user = Long.parseLong(parts[1]);
                    var nice = Long.parseLong(parts[2]);
                    var system = Long.parseLong(parts[3]);
                    var idle = Long.parseLong(parts[4]);
                    var iowait = Long.parseLong(parts[5]);
                    var irq = Long.parseLong(parts[6]);
                    var softirq = Long.parseLong(parts[7]);

                    var total = user + nice + system + idle + iowait + irq + softirq;
                    var totalDelta = total - lastCoreTotal[coreId];
                    var idleDelta = idle - lastCoreIdle[coreId];

                    if (lastCoreTotal[coreId] > 0 && totalDelta > 0) {
                        coreUsage[coreId] = 100.0 * (totalDelta - idleDelta) / totalDelta;
                    }

                    lastCoreTotal[coreId] = total;
                    lastCoreIdle[coreId] = idle;

                    var history = coreHistory.get(coreId);
                    if (history.size() >= HISTORY_SIZE) {
                        history.removeFirst();
                    }
                    history.addLast((long) coreUsage[coreId]);
                }
            }
        } catch (IOException e) {
            // Ignore - will show 0
        }
    }

    private void updateMemoryInfo() {
        try {
            var lines = Files.readAllLines(PROC_MEMINFO);
            for (var line : lines) {
                if (line.startsWith("MemTotal:")) {
                    memTotal = parseMemValue(line);
                } else if (line.startsWith("MemAvailable:")) {
                    memAvailable = parseMemValue(line);
                } else if (line.startsWith("SwapTotal:")) {
                    swapTotal = parseMemValue(line);
                } else if (line.startsWith("SwapFree:")) {
                    swapFree = parseMemValue(line);
                }
            }
            memUsed = memTotal - memAvailable;
        } catch (IOException e) {
            // Ignore
        }

        var memPercent = memTotal > 0 ? (memUsed * 100) / memTotal : 0;
        if (memoryHistory.size() >= HISTORY_SIZE) {
            memoryHistory.removeFirst();
        }
        memoryHistory.addLast(memPercent);
    }

    private long parseMemValue(String line) {
        var parts = line.split("\\s+");
        if (parts.length >= 2) {
            return Long.parseLong(parts[1]);
        }
        return 0;
    }

    private void updateSystemInfo() {
        try {
            var uptimeLine = Files.readString(PROC_UPTIME).trim();
            var parts = uptimeLine.split("\\s+");
            uptime = Double.parseDouble(parts[0]);
        } catch (IOException | NumberFormatException e) {
            // Ignore
        }

        try {
            var loadavg = Files.readString(PROC_LOADAVG).trim();
            var parts = loadavg.split("\\s+");
            loadAvg1 = Double.parseDouble(parts[0]);
            loadAvg5 = Double.parseDouble(parts[1]);
            loadAvg15 = Double.parseDouble(parts[2]);
        } catch (IOException | NumberFormatException e) {
            // Ignore
        }
    }

    private void updateProcessList(SortMode sortMode) {
        var newProcesses = new ArrayList<ProcessInfo>();

        try (var stream = Files.list(PROC)) {
            stream.filter(p -> {
                    var name = p.getFileName().toString();
                    return name.chars().allMatch(Character::isDigit);
                })
                .forEach(pidPath -> {
                    try {
                        var info = readProcessInfo(pidPath);
                        if (info != null) {
                            newProcesses.add(info);
                        }
                    } catch (Exception e) {
                        // Process may have exited
                    }
                });
        } catch (IOException e) {
            // Ignore
        }

        var comparator = switch (sortMode) {
            case CPU -> Comparator.comparingDouble(ProcessInfo::cpuPercent).reversed();
            case MEMORY -> Comparator.comparingLong(ProcessInfo::memoryKb).reversed();
            case PID -> Comparator.comparingInt(ProcessInfo::pid);
        };
        newProcesses.sort(comparator);

        this.processes = newProcesses;
    }

    private ProcessInfo readProcessInfo(Path pidPath) {
        try {
            var pid = Integer.parseInt(pidPath.getFileName().toString());

            var statLine = Files.readString(pidPath.resolve("stat")).trim();
            var matcher = STAT_PATTERN.matcher(statLine);
            if (!matcher.find()) {
                return null;
            }

            var name = matcher.group(2);
            var state = matcher.group(3).charAt(0);

            var statmLine = Files.readString(pidPath.resolve("statm")).trim();
            var statmParts = statmLine.split("\\s+");
            var memPages = Long.parseLong(statmParts[1]);
            var memoryKb = memPages * 4;

            var statParts = statLine.substring(statLine.lastIndexOf(')') + 2).split("\\s+");
            double cpuPercent = 0;
            if (statParts.length > 12) {
                var utime = Long.parseLong(statParts[11]);
                var stime = Long.parseLong(statParts[12]);
                cpuPercent = (utime + stime) / 100.0;
            }

            var user = "?";
            try {
                var statusLines = Files.readAllLines(pidPath.resolve("status"));
                for (var line : statusLines) {
                    if (line.startsWith("Uid:")) {
                        var uid = line.split("\\s+")[1];
                        user = uid;
                        break;
                    }
                }
            } catch (IOException e) {
                // Ignore
            }

            return new ProcessInfo(pid, name, state, cpuPercent, memoryKb, user);
        } catch (IOException | NumberFormatException e) {
            return null;
        }
    }
}
