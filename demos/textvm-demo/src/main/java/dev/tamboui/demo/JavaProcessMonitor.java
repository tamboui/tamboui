/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.demo;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.management.ManagementFactory;
import java.lang.management.ClassLoadingMXBean;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.lang.management.RuntimeMXBean;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Pattern;
import java.util.function.Consumer;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
/**
 * Monitors Java processes using jps and jstat.
 */
public class JavaProcessMonitor {
    
    private static final Pattern JPS_PATTERN = Pattern.compile("^(\\d+)\\s+(.+)$");
    private final Map<Integer, JavaProcessInfo> processCache = new HashMap<>();
    private final Map<Integer, Long> lastCpuTime = new HashMap<>();
    private final Map<Integer, Long> lastSampleTime = new HashMap<>();
    private final Map<Integer, Boolean> attachmentFailed = new HashMap<>(); // Track processes that can't be attached to
    private Consumer<String> errorLogger = null;
    
    /**
     * Sets a callback for error logging.
     */
    public void setErrorLogger(Consumer<String> logger) {
        this.errorLogger = logger;
    }
    
    private void logError(String message) {
        if (errorLogger != null) {
            errorLogger.accept(message);
        } else {
            System.err.println(message);
        }
    }
    
    /**
     * Gets a list of all running Java processes.
     * This only gets basic info (PID, main class) - detailed info is fetched on demand.
     */
    public List<JavaProcessInfo> getJavaProcesses() {
        List<JavaProcessInfo> processes = new ArrayList<>();
        
        try {
            Process jpsProcess = new ProcessBuilder("jps", "-l", "-v")
                .redirectErrorStream(true)
                .start();
            
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(jpsProcess.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (line.isEmpty()) continue;
                    
                    // Parse jps output: "pid mainClass [args...]"
                    String[] parts = line.split("\\s+", 2);
                    if (parts.length >= 1) {
                        try {
                            int pid = Integer.parseInt(parts[0]);
                            String mainClass = parts.length > 1 ? parts[1] : "";
                            
                            // Extract main class and arguments
                            String[] classAndArgs = mainClass.split("\\s+", 2);
                            String className = classAndArgs.length > 0 ? classAndArgs[0] : "";
                            String args = classAndArgs.length > 1 ? classAndArgs[1] : "";
                            
                            // Create basic info without expensive operations
                            // Detailed info will be fetched when process is selected
                            JavaProcessInfo info = new JavaProcessInfo(
                                pid, className, args,
                                0, 0, 0, 0, 0, 0, 0, 0, 0.0, 0
                            );
                            processes.add(info);
                        } catch (NumberFormatException e) {
                            // Skip invalid lines
                        }
                    }
                }
            }
            
            jpsProcess.waitFor();
        } catch (Exception e) {
            // If jps fails, return empty list
            // Log error for debugging (in production, might want to use a logger)
            System.err.println("Error getting Java processes: " + e.getMessage());
            e.printStackTrace();
        }
        
        return processes;
    }
    
    /**
     * Gets thread details for a process.
     */
    public List<JavaProcessInfo.ThreadInfo> getThreadDetails(int pid) {
        List<JavaProcessInfo.ThreadInfo> threads = new ArrayList<>();
        
        if (attachmentFailed.getOrDefault(pid, false)) {
            return threads;
        }
        
        try {
            Object vm = attachToVM(pid);
            if (vm != null) {
                try {
                    String connectorAddress = getJMXConnectorAddress(vm);
                    if (connectorAddress != null) {
                        JMXServiceURL url = new JMXServiceURL(connectorAddress);
                        try (JMXConnector connector = JMXConnectorFactory.connect(url, null)) {
                            MBeanServerConnection mbsc = connector.getMBeanServerConnection();
                            
                            ThreadMXBean threadBean = ManagementFactory.newPlatformMXBeanProxy(
                                mbsc, ManagementFactory.THREAD_MXBEAN_NAME, ThreadMXBean.class);
                            
                            long[] threadIds = threadBean.getAllThreadIds();
                            ThreadInfo[] threadInfos = threadBean.getThreadInfo(threadIds);
                            
                            for (ThreadInfo info : threadInfos) {
                                if (info != null) {
                                    boolean running = "RUNNABLE".equals(info.getThreadState().toString());
                                    threads.add(new JavaProcessInfo.ThreadInfo(
                                        info.getThreadName(),
                                        info.getThreadId(),
                                        info.getThreadState().toString(),
                                        info.isDaemon(),
                                        threadBean.getThreadCpuTime(info.getThreadId()),
                                        running
                                    ));
                                }
                            }
                        }
                    }
                } finally {
                    detachVM(vm);
                }
            }
        } catch (Exception e) {
            // Ignore errors when getting thread details
        }
        
        return threads;
    }
    
    /**
     * Gets detailed information about a specific Java process using JMX (like jvisualvm).
     */
    public JavaProcessInfo getProcessInfo(int pid, String mainClass, String arguments) {
        long heapUsed = 0;
        long heapMax = 0;
        long nonHeapUsed = 0;
        long nonHeapMax = 0;
        int threadCount = 0;
        int liveThreadCount = 0;
        int daemonThreadCount = 0;
        long classesLoaded = 0;
        double cpuUsage = 0.0;
        long uptime = 0;
        
        // Skip if we know this process can't be attached to
        if (attachmentFailed.getOrDefault(pid, false)) {
            return new JavaProcessInfo(pid, mainClass, arguments, 0, 0, 0, 0, 
                threadCount, liveThreadCount, daemonThreadCount, classesLoaded, cpuUsage, uptime);
        }
        
        // Try JMX first (like jvisualvm does)
        try {
            Object vm = attachToVM(pid);
            if (vm != null) {
                try {
                    // Get JMX connector URL
                    String connectorAddress = getJMXConnectorAddress(vm);
                    if (connectorAddress != null) {
                        // Connect via JMX
                        JMXServiceURL url = new JMXServiceURL(connectorAddress);
                        try (JMXConnector connector = JMXConnectorFactory.connect(url, null)) {
                            MBeanServerConnection mbsc = connector.getMBeanServerConnection();
                            
                            // Get MemoryMXBean
                            MemoryMXBean memoryBean = ManagementFactory.newPlatformMXBeanProxy(
                                mbsc, ManagementFactory.MEMORY_MXBEAN_NAME, MemoryMXBean.class);
                            MemoryUsage heapUsage = memoryBean.getHeapMemoryUsage();
                            MemoryUsage nonHeapUsage = memoryBean.getNonHeapMemoryUsage();
                            
                            heapUsed = heapUsage.getUsed();
                            heapMax = heapUsage.getMax();
                            nonHeapUsed = nonHeapUsage.getUsed();
                            nonHeapMax = nonHeapUsage.getMax();
                            
                            // Get ThreadMXBean
                            ThreadMXBean threadBean = ManagementFactory.newPlatformMXBeanProxy(
                                mbsc, ManagementFactory.THREAD_MXBEAN_NAME, ThreadMXBean.class);
                            threadCount = threadBean.getThreadCount();
                            liveThreadCount = threadBean.getThreadCount();
                            
                            // Count daemon threads
                            long[] threadIds = threadBean.getAllThreadIds();
                            for (long threadId : threadIds) {
                                try {
                                    ThreadInfo info = threadBean.getThreadInfo(threadId);
                                    if (info != null && info.isDaemon()) {
                                        daemonThreadCount++;
                                    }
                                } catch (Exception e) {
                                    // Ignore individual thread errors
                                }
                            }
                            
                            // Get ClassLoadingMXBean for classes loaded
                            ClassLoadingMXBean classBean = ManagementFactory.newPlatformMXBeanProxy(
                                mbsc, ManagementFactory.CLASS_LOADING_MXBEAN_NAME, ClassLoadingMXBean.class);
                            classesLoaded = classBean.getLoadedClassCount();
                            
                            // Get RuntimeMXBean for uptime
                            RuntimeMXBean runtimeBean = ManagementFactory.newPlatformMXBeanProxy(
                                mbsc, ManagementFactory.RUNTIME_MXBEAN_NAME, RuntimeMXBean.class);
                            uptime = runtimeBean.getUptime();
                            
                            // Get CPU usage (requires sampling)
                            // For now, we'll calculate it if we have previous sample
                            Long lastCpu = lastCpuTime.get(pid);
                            Long lastSample = lastSampleTime.get(pid);
                            long currentTime = System.currentTimeMillis();
                            
                            if (lastCpu != null && lastSample != null) {
                                try {
                                    Object cpuLoad = mbsc.getAttribute(
                                        new ObjectName("java.lang:type=OperatingSystem"), "ProcessCpuLoad");
                                    if (cpuLoad instanceof Double) {
                                        cpuUsage = ((Double) cpuLoad) * 100.0;
                                    }
                                } catch (Exception e) {
                                    // CPU load might not be available
                                }
                            }
                            
                            lastCpuTime.put(pid, currentTime);
                            lastSampleTime.put(pid, currentTime);
                        }
                    }
                } finally {
                    detachVM(vm);
                }
                
                // Success - return the info
                JavaProcessInfo info = new JavaProcessInfo(
                    pid, mainClass, arguments,
                    heapUsed, heapMax, nonHeapUsed, nonHeapMax,
                    threadCount, liveThreadCount, daemonThreadCount, classesLoaded,
                    cpuUsage, uptime
                );
                processCache.put(pid, info);
                return info;
            }
        } catch (Exception e) {
            // JMX failed - mark as failed and fall back to jstat
            attachmentFailed.put(pid, true);
            if (!processCache.containsKey(pid) || processCache.get(pid).heapMax() == 0) {
                logError("JMX attach failed for PID " + pid + ": " + e.getMessage());
            }
        }
        
        // Fallback to jstat if JMX failed
        try {
            // Get memory info using jstat -gc
            Process jstatProcess = new ProcessBuilder("jstat", "-gc", String.valueOf(pid))
                .redirectErrorStream(true)
                .start();
            
            StringBuilder output = new StringBuilder();
            String header = null;
            String data = null;
            
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(jstatProcess.getInputStream()))) {
                String line;
                boolean isFirstLine = true;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                    if (isFirstLine) {
                        header = line;
                        isFirstLine = false;
                    } else if (data == null && !line.trim().isEmpty()) {
                        data = line;
                    }
                }
            }
            
            int exitCode = jstatProcess.waitFor();
            
            if (exitCode != 0) {
                // jstat failed - check for attachment errors
                String errorOutput = output.toString().trim();
                boolean isAttachmentError = errorOutput.contains("Could not attach") || 
                                          errorOutput.contains("MonitorException") ||
                                          errorOutput.contains("permission denied") ||
                                          errorOutput.contains("Access denied");
                
                if (isAttachmentError) {
                    // Mark this process as unable to attach (don't keep retrying)
                    attachmentFailed.put(pid, true);
                    // Only log once per process
                    if (!processCache.containsKey(pid) || processCache.get(pid).heapMax() == 0) {
                        logError("Cannot attach to PID " + pid + " (permission denied or security restriction)");
                    }
                } else {
                    // Other error - log it
                    if (errorOutput.isEmpty()) {
                        logError("jstat -gc failed for PID " + pid + " (exit code: " + exitCode + ")");
                    } else {
                        // Truncate long error messages
                        String shortError = errorOutput.length() > 200 
                            ? errorOutput.substring(0, 197) + "..." 
                            : errorOutput;
                        logError("jstat -gc failed for PID " + pid + " (exit code: " + exitCode + "): " + shortError);
                    }
                }
            } else if (data != null && !data.trim().isEmpty()) {
                // Check if data looks like an error message (starts with common error patterns)
                String trimmedData = data.trim();
                if (trimmedData.startsWith(".") || trimmedData.startsWith("Exception") || 
                    trimmedData.startsWith("Error") || trimmedData.contains("at ")) {
                    // This looks like an error message, not data
                    logError("jstat returned error message for PID " + pid + ": " + 
                        (trimmedData.length() > 150 ? trimmedData.substring(0, 147) + "..." : trimmedData));
                } else {
                    String[] values = trimmedData.split("\\s+");
                    if (values.length >= 12) {
                        // Parse jstat -gc output
                        // S0C, S1C, S0U, S1U, EC, EU, OC, OU, MC, MU, CCSC, CCSU, YGC, YGCT, FGC, FGCT, GCT
                        try {
                            double s0c = parseDouble(values[0]);
                            double s1c = parseDouble(values[1]);
                            double s0u = parseDouble(values[2]);
                            double s1u = parseDouble(values[3]);
                            double ec = parseDouble(values[4]);
                            double eu = parseDouble(values[5]);
                            double oc = parseDouble(values[6]);
                            double ou = parseDouble(values[7]);
                            double mc = parseDouble(values[8]);
                            double mu = parseDouble(values[9]);
                            double ccsc = parseDouble(values[10]);
                            double ccsu = parseDouble(values[11]);
                            
                            // Heap = Eden + Survivor + Old (all in KB, convert to bytes)
                            heapUsed = (long) ((s0u + s1u + eu + ou) * 1024);
                            heapMax = (long) ((s0c + s1c + ec + oc) * 1024);
                            
                            // Non-heap = Metaspace + Compressed Class Space
                            nonHeapUsed = (long) ((mu + ccsu) * 1024);
                            nonHeapMax = (long) ((mc + ccsc) * 1024);
                        } catch (Exception e) {
                            logError("Parse error for PID " + pid + ": " + e.getMessage() + 
                                " (data: " + trimmedData + ", values: " + values.length + ")");
                        }
                    } else {
                        logError("jstat output has insufficient columns for PID " + pid + 
                            " (expected >=12, got " + values.length + ", data: " + trimmedData + ")");
                    }
                }
            } else {
                logError("jstat returned no data for PID " + pid + " (header: " + header + ")");
            }
        } catch (Exception e) {
            // jstat may fail if process doesn't exist or permissions issue
            logError("Exception getting memory info for PID " + pid + ": " + e.getMessage());
        }
        
        try {
            // Get thread count using jstack (just count lines with "Thread" in name)
            Process jstackProcess = new ProcessBuilder("jstack", String.valueOf(pid))
                .redirectErrorStream(true)
                .start();
            
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(jstackProcess.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.contains("\"") && line.contains("Thread")) {
                        threadCount++;
                    }
                }
            }
            
            jstackProcess.waitFor();
        } catch (Exception e) {
            logError("Exception getting thread count for PID " + pid + ": " + e.getMessage());
        }
        
        // Calculate CPU usage using jstat -gcutil (requires sampling over time)
        // For now, we'll use a simple approach: sample twice and calculate difference
        long currentTime = System.currentTimeMillis();
        Long lastTime = lastSampleTime.get(pid);
        
        if (lastTime == null || (currentTime - lastTime) > 1000) {
            // Sample CPU time using jstat -gcutil
            // The CPU column in jstat -gcutil shows cumulative CPU time, not percentage
            // We need to sample twice and calculate the difference
            try {
                // For a proper implementation, we'd need to:
                // 1. Sample jstat -gcutil twice with a delay
                // 2. Calculate the difference in CPU time
                // 3. Divide by the time interval
                // For now, use a placeholder
                cpuUsage = 0.0;
            } catch (Exception e) {
                // jstat may fail
            }
            lastSampleTime.put(pid, currentTime);
        } else {
            // Use cached value
            JavaProcessInfo cached = processCache.get(pid);
            if (cached != null) {
                cpuUsage = cached.cpuUsage();
            }
        }
        
        // Get uptime using jinfo
        try {
            Process jinfoProcess = new ProcessBuilder("jinfo", "-sysprops", String.valueOf(pid))
                .redirectErrorStream(true)
                .start();
            
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(jinfoProcess.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.contains("java.lang.Runtime.startTime")) {
                        // Extract start time: java.lang.Runtime.startTime = 1234567890
                        String[] parts = line.split("=");
                        if (parts.length == 2) {
                            try {
                                long startTime = Long.parseLong(parts[1].trim());
                                uptime = System.currentTimeMillis() - startTime;
                            } catch (NumberFormatException e) {
                                // Ignore parse errors
                            }
                        }
                    }
                }
            }
            
            jinfoProcess.waitFor();
        } catch (Exception e) {
            // jinfo may fail - use cached value if available
            JavaProcessInfo cached = processCache.get(pid);
            if (cached != null) {
                uptime = cached.uptime();
            }
        }
        
        JavaProcessInfo info = new JavaProcessInfo(
            pid, mainClass, arguments,
            heapUsed, heapMax, nonHeapUsed, nonHeapMax,
            threadCount, liveThreadCount, daemonThreadCount, classesLoaded,
            cpuUsage, uptime
        );
        
        processCache.put(pid, info);
        lastSampleTime.put(pid, currentTime);
        
        return info;
    }
    
    /**
     * Attaches to a Java Virtual Machine using the Attach API.
     * Uses reflection to work with both Java 8 (tools.jar) and Java 9+ (jdk.attach module).
     */
    private Object attachToVM(int pid) throws Exception {
        try {
            // Try Java 9+ module path first
            Class<?> vmClass = Class.forName("com.sun.tools.attach.VirtualMachine");
            return vmClass.getMethod("attach", String.class).invoke(null, String.valueOf(pid));
        } catch (ClassNotFoundException e) {
            // Try loading from tools.jar (Java 8)
            // This would require tools.jar on classpath, which is usually not available
            throw new Exception("Attach API not available - ensure jdk.attach module is accessible");
        }
    }
    
    /**
     * Gets the JMX connector address from an attached VM.
     */
    private String getJMXConnectorAddress(Object vm) throws Exception {
        // Get the local connector address
        // This is typically stored in a system property
        try {
            // First try to get agent properties
            Object agentProperties = vm.getClass().getMethod("getAgentProperties").invoke(vm);
            if (agentProperties instanceof Properties) {
                Properties props = (Properties) agentProperties;
                String address = props.getProperty("com.sun.management.jmxremote.localConnectorAddress");
                if (address != null && !address.isEmpty()) {
                    return address;
                }
            }
            
            // If not found, try system properties
            Object systemProperties = vm.getClass().getMethod("getSystemProperties").invoke(vm);
            if (systemProperties instanceof Properties) {
                Properties props = (Properties) systemProperties;
                String address = props.getProperty("com.sun.management.jmxremote.localConnectorAddress");
                if (address != null && !address.isEmpty()) {
                    return address;
                }
            }
            
            // Start the local connector agent if not already started
            try {
                vm.getClass().getMethod("startLocalManagementAgent").invoke(vm);
                // Try again after starting
                agentProperties = vm.getClass().getMethod("getAgentProperties").invoke(vm);
                if (agentProperties instanceof Properties) {
                    Properties props = (Properties) agentProperties;
                    String address = props.getProperty("com.sun.management.jmxremote.localConnectorAddress");
                    if (address != null && !address.isEmpty()) {
                        return address;
                    }
                }
            } catch (Exception e) {
                // startLocalManagementAgent might fail if already started or not supported
                // Try to get address from system properties again
                systemProperties = vm.getClass().getMethod("getSystemProperties").invoke(vm);
                if (systemProperties instanceof Properties) {
                    Properties props = (Properties) systemProperties;
                    String address = props.getProperty("com.sun.management.jmxremote.localConnectorAddress");
                    if (address != null && !address.isEmpty()) {
                        return address;
                    }
                }
            }
        } catch (Exception e) {
            logError("Failed to get JMX connector address: " + e.getMessage());
            if (e.getCause() != null) {
                logError("  Caused by: " + e.getCause().getMessage());
            }
        }
        return null;
    }
    
    /**
     * Detaches from a Virtual Machine.
     */
    private void detachVM(Object vm) {
        try {
            vm.getClass().getMethod("detach").invoke(vm);
        } catch (Exception e) {
            // Ignore detach errors
        }
    }
    
    private double parseDouble(String value) {
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }
    
    /**
     * Updates CPU usage for a process by comparing jstat samples over time.
     * This would need to be called periodically to track CPU usage.
     */
    public void updateProcessInfo(JavaProcessInfo process) {
        // For CPU usage tracking, we'd need to sample jstat -gcutil over time
        // and calculate the difference. For now, this is a placeholder.
    }
}

