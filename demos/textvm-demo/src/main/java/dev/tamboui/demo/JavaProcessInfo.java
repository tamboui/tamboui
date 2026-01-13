/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.demo;

/**
 * Information about a Java process.
 */
public record JavaProcessInfo(
    int pid,
    String mainClass,
    String arguments,
    long heapUsed,
    long heapMax,
    long nonHeapUsed,
    long nonHeapMax,
    int threadCount,
    int liveThreadCount,
    int daemonThreadCount,
    long classesLoaded,
    double cpuUsage,
    long uptime
) {
    public String displayName() {
        if (mainClass != null && !mainClass.isEmpty()) {
            return mainClass;
        }
        return "PID " + pid;
    }
    
    public double heapUsagePercent() {
        return heapMax > 0 ? (heapUsed * 100.0) / heapMax : 0;
    }
    
    public double nonHeapUsagePercent() {
        return nonHeapMax > 0 ? (nonHeapUsed * 100.0) / nonHeapMax : 0;
    }
    
    public String formatBytes(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        }
        if (bytes < 1024 * 1024) {
            return String.format("%.1f KB", bytes / 1024.0);
        }
        if (bytes < 1024 * 1024 * 1024) {
            return String.format("%.1f MB", bytes / (1024.0 * 1024));
        }
        return String.format("%.2f GB", bytes / (1024.0 * 1024.0 * 1024));
    }
    
    public String formatUptime() {
        long seconds = uptime / 1000;
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs = seconds % 60;
        if (hours > 0) {
            return String.format("%dh %02dm %02ds", hours, minutes, secs);
        }
        return String.format("%dm %02ds", minutes, secs);
    }
    
    /**
     * Thread information for display in threads view.
     */
    public record ThreadInfo(
        String name,
        long id,
        String state,
        boolean daemon,
        long cpuTime,
        boolean running
    ) {}
}

