/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.demo;

import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.GlobalMemory;
import oshi.software.os.OSProcess;
import oshi.software.os.OperatingSystem;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Supplier;

/**
 * Reads system metrics using OSHI (cross-platform).
 * Separates system monitoring logic from UI rendering.
 * <p>
 * Thread-safe: uses a ReentrantReadWriteLock for safe concurrent access
 * between the update thread and render thread.
 */
final class SystemMetrics {

    private static final int HISTORY_SIZE = 60;

    private final SystemInfo systemInfo = new SystemInfo();
    private final GlobalMemory memory = systemInfo.getHardware().getMemory();
    private final CentralProcessor processor = systemInfo.getHardware().getProcessor();
    private final OperatingSystem operatingSystem = systemInfo.getOperatingSystem();

    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    // CPU tracking
    private final int numCpus;
    private final double[] coreUsage;
    private final List<Deque<Long>> coreHistory;
    private long[][] lastCpuTicks;

    // Memory tracking
    private final Deque<Long> memoryHistory = new ArrayDeque<>(HISTORY_SIZE);
    private long memTotal;
    private long memUsed;
    private long memAvailable;
    private long swapTotal;
    private long swapFree;

    // System info
    private double uptime;
    private double loadAvg1;
    private double loadAvg5;
    private double loadAvg15;

    // Process list
    private List<ProcessInfo> processes = new ArrayList<>();
    private final Map<Integer, OSProcess> lastProcessesByPid = new HashMap<>();

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
        numCpus = processor.getLogicalProcessorCount();
        coreUsage = new double[numCpus];
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
     * Updates all metrics.
     * This method should be called from a background thread.
     *
     * @param sortMode the sort mode for the process list
     */
    void update(SortMode sortMode) {
        lock.writeLock().lock();
        try {
            updateCpuUsage();
            updateMemoryInfo();
            updateSystemInfo();
            updateProcessList(sortMode);
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Executes a read operation under the read lock.
     *
     * @param reader the read operation to execute
     * @param <T> the return type
     * @return the result of the read operation
     */
    <T> T read(Supplier<T> reader) {
        lock.readLock().lock();
        try {
            return reader.get();
        } finally {
            lock.readLock().unlock();
        }
    }

    // ==================== Accessors (must be called within read()) ====================

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

    List<Long> coreHistory(int core) {
        return core >= 0 && core < numCpus ? List.copyOf(coreHistory.get(core)) : List.of();
    }

    List<List<Long>> allCoreHistories() {
        var result = new ArrayList<List<Long>>(numCpus);
        for (var history : coreHistory) {
            result.add(List.copyOf(history));
        }
        return result;
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

    List<Long> memoryHistory() {
        return List.copyOf(memoryHistory);
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

    // ==================== Private Update Methods ====================

    private void updateCpuUsage() {
        if (lastCpuTicks == null) {
            lastCpuTicks = processor.getProcessorCpuLoadTicks();
            return;
        }

        var load = processor.getProcessorCpuLoadBetweenTicks(lastCpuTicks);
        lastCpuTicks = processor.getProcessorCpuLoadTicks();

        for (var coreId = 0; coreId < Math.min(numCpus, load.length); coreId++) {
            var usage = 100.0 * load[coreId];
            if (Double.isNaN(usage) || Double.isInfinite(usage)) {
                usage = 0;
            }
            coreUsage[coreId] = Math.max(0, Math.min(100, usage));

            var history = coreHistory.get(coreId);
            if (history.size() >= HISTORY_SIZE) {
                history.removeFirst();
            }
            history.addLast((long) coreUsage[coreId]);
        }
    }

    private void updateMemoryInfo() {
        memTotal = memory.getTotal() / 1024;
        memAvailable = memory.getAvailable() / 1024;
        memUsed = Math.max(0, memTotal - memAvailable);

        var vm = memory.getVirtualMemory();
        swapTotal = vm.getSwapTotal() / 1024;
        var swapUsedVal = vm.getSwapUsed() / 1024;
        swapFree = Math.max(0, swapTotal - swapUsedVal);

        var memPercent = memTotal > 0 ? (memUsed * 100) / memTotal : 0;
        if (memoryHistory.size() >= HISTORY_SIZE) {
            memoryHistory.removeFirst();
        }
        memoryHistory.addLast(memPercent);
    }

    private void updateSystemInfo() {
        uptime = operatingSystem.getSystemUptime();

        var la = processor.getSystemLoadAverage(3);
        if (la != null && la.length >= 3) {
            loadAvg1 = la[0] < 0 ? 0 : la[0];
            loadAvg5 = la[1] < 0 ? 0 : la[1];
            loadAvg15 = la[2] < 0 ? 0 : la[2];
        } else {
            loadAvg1 = 0;
            loadAvg5 = 0;
            loadAvg15 = 0;
        }
    }

    private void updateProcessList(SortMode sortMode) {
        var newProcesses = new ArrayList<ProcessInfo>();

        var osProcesses = operatingSystem.getProcesses();
        var newByPid = new HashMap<Integer, OSProcess>(osProcesses.size());

        for (var p : osProcesses) {
            var pid = p.getProcessID();
            var prev = lastProcessesByPid.get(pid);

            var cpuPercent = 0.0;
            try {
                if (prev != null) {
                    cpuPercent = 100.0 * p.getProcessCpuLoadBetweenTicks(prev);
                }
            } catch (Exception ignored) {
                // Some platforms/permissions may fail per-process CPU query.
            }
            if (Double.isNaN(cpuPercent) || Double.isInfinite(cpuPercent)) {
                cpuPercent = 0;
            }

            var memoryKb = p.getResidentSetSize() / 1024;
            var user = p.getUser();
            if (user == null || user.isBlank()) {
                user = "?";
            }

            var name = p.getCommandLine();
            if (name == null || name.isBlank()) {
                name = p.getName();
            }
            if (name == null || name.isBlank()) {
                name = "?";
            }

            newProcesses.add(new ProcessInfo(
                pid,
                name,
                mapState(p.getState()),
                Math.max(0, cpuPercent),
                Math.max(0, memoryKb),
                user
            ));

            newByPid.put(pid, p);
        }

        var comparator = switch (sortMode) {
            case CPU -> Comparator.comparingDouble(ProcessInfo::cpuPercent).reversed();
            case MEMORY -> Comparator.comparingLong(ProcessInfo::memoryKb).reversed();
            case PID -> Comparator.comparingInt(ProcessInfo::pid);
        };
        newProcesses.sort(comparator);

        this.processes = newProcesses;
        lastProcessesByPid.clear();
        lastProcessesByPid.putAll(newByPid);
    }

    private static char mapState(OSProcess.State state) {
        if (state == null) {
            return '?';
        }
        return switch (state) {
            case RUNNING -> 'R';
            case SLEEPING -> 'S';
            case WAITING -> 'D';
            case STOPPED -> 'T';
            case ZOMBIE -> 'Z';
            default -> '?';
        };
    }
}
