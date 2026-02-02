/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.tui;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import dev.tamboui.terminal.Backend;
import dev.tamboui.tui.bindings.Bindings;
import dev.tamboui.tui.event.Event;
import dev.tamboui.tui.event.EventParser;

/**
 * A dedicated thread for reading terminal input.
 * <p>
 * This class isolates terminal input reading from the main event loop,
 * ensuring that keyboard input remains responsive even when tick events
 * are being processed. It reads from the terminal using a blocking call
 * with the configured poll timeout, parses input into events, and queues
 * them for the main loop to consume.
 *
 * @see TuiRunner
 */
public final class TerminalInputReader implements Runnable {

    private static final int MAX_CONSECUTIVE_ERRORS = 10;

    private final Backend backend;
    private final BlockingQueue<Event> eventQueue;
    private final Bindings bindings;
    private final AtomicBoolean running;
    private final int pollTimeoutMs;
    private volatile Thread thread;

    /**
     * Creates a new terminal input reader.
     *
     * @param backend     the terminal backend to read from
     * @param eventQueue  the queue to place parsed events into
     * @param bindings    the bindings for event semantic action matching
     * @param running     the shared running flag for shutdown coordination
     * @param pollTimeout the timeout for reading terminal input
     */
    public TerminalInputReader(Backend backend, BlockingQueue<Event> eventQueue,
                               Bindings bindings, AtomicBoolean running,
                               Duration pollTimeout) {
        this.backend = backend;
        this.eventQueue = eventQueue;
        this.bindings = bindings;
        this.running = running;
        this.pollTimeoutMs = (int) pollTimeout.toMillis();
    }

    /**
     * Starts the input reader thread.
     * <p>
     * The thread is created as a daemon thread named "tui-input-reader".
     */
    public void start() {
        thread = new Thread(this, "tui-input-reader");
        thread.setDaemon(true);
        thread.start();
    }

    /**
     * Stops the input reader thread and waits for it to terminate.
     *
     * @param timeoutMs maximum time to wait for thread termination in milliseconds
     */
    public void stop(long timeoutMs) {
        Thread t = thread;
        if (t != null && t.isAlive()) {
            try {
                t.join(timeoutMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * Returns whether the input reader thread is alive.
     *
     * @return true if the thread is running
     */
    public boolean isAlive() {
        Thread t = thread;
        return t != null && t.isAlive();
    }

    @Override
    public void run() {
        int consecutiveErrors = 0;

        while (running.get()) {
            try {
                Event event = EventParser.readEvent(backend, pollTimeoutMs, bindings);
                if (event != null) {
                    eventQueue.offer(event);
                    consecutiveErrors = 0;
                }
            } catch (IOException e) {
                consecutiveErrors++;
                if (consecutiveErrors >= MAX_CONSECUTIVE_ERRORS) {
                    break;
                }
            }
        }
    }
}
