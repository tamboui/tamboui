/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.tui;

import java.time.Duration;
import java.util.concurrent.ScheduledExecutorService;

import dev.tamboui.tui.bindings.BindingSets;
import dev.tamboui.tui.bindings.Bindings;

/**
 * Configuration options for {@link InlineTuiRunner}.
 * <p>
 * Unlike {@link TuiConfig}, this configuration is tailored for inline displays
 * that stay within the normal terminal flow (no alternate screen, cursor stays visible).
 *
 * <pre>{@code
 * InlineTuiConfig config = InlineTuiConfig.builder(4)
 *     .tickRate(Duration.ofMillis(50))  // For animations
 *     .clearOnClose(true)
 *     .build();
 * }</pre>
 *
 * @see InlineTuiRunner
 */
public final class InlineTuiConfig {

    /**
     * Default poll timeout for reading events (40ms).
     */
    public static final int DEFAULT_POLL_TIMEOUT = 40;

    /**
     * Default tick rate for animations (40ms, ~25fps).
     */
    public static final int DEFAULT_TICK_RATE = 40;

    private final int height;
    private final Duration tickRate;
    private final Duration pollTimeout;
    private final boolean clearOnClose;
    private final Bindings bindings;
    private final ScheduledExecutorService scheduler;

    private InlineTuiConfig(int height, Duration tickRate, Duration pollTimeout,
                            boolean clearOnClose, Bindings bindings, ScheduledExecutorService scheduler) {
        this.height = height;
        this.tickRate = tickRate;
        this.pollTimeout = pollTimeout;
        this.clearOnClose = clearOnClose;
        this.bindings = bindings;
        this.scheduler = scheduler;
    }

    /**
     * Creates a default configuration for the given height.
     * <p>
     * Defaults:
     * <ul>
     *   <li>Tick rate: 40ms (~25fps, suitable for animations)</li>
     *   <li>Poll timeout: 40ms</li>
     *   <li>Clear on close: false</li>
     *   <li>Bindings: defaults</li>
     * </ul>
     *
     * @param height the number of lines for the inline display
     * @return a new configuration with defaults
     */
    public static InlineTuiConfig defaults(int height) {
        return new InlineTuiConfig(
                height,
                Duration.ofMillis(DEFAULT_TICK_RATE),
                Duration.ofMillis(DEFAULT_POLL_TIMEOUT),
                false,
                BindingSets.defaults(),
                null
        );
    }

    /**
     * Returns a builder for creating custom configurations.
     *
     * @param height the number of lines for the inline display
     * @return a new builder
     */
    public static Builder builder(int height) {
        return new Builder(height);
    }

    /**
     * Returns the height of the inline display in lines.
     *
     * @return the display height
     */
    public int height() {
        return height;
    }

    /**
     * Returns the tick interval for animations.
     *
     * @return the tick interval, or null if ticks are disabled
     */
    public Duration tickRate() {
        return tickRate;
    }

    /**
     * Returns whether tick events are enabled.
     *
     * @return true if ticks are enabled
     */
    public boolean ticksEnabled() {
        return tickRate != null;
    }

    /**
     * Returns the poll timeout for reading events.
     *
     * @return the poll timeout duration
     */
    public Duration pollTimeout() {
        return pollTimeout;
    }

    /**
     * Returns whether the display should be cleared when closed.
     *
     * @return true to clear on close
     */
    public boolean clearOnClose() {
        return clearOnClose;
    }

    /**
     * Returns the key bindings for semantic action matching.
     *
     * @return the bindings
     */
    public Bindings bindings() {
        return bindings;
    }

    /**
     * Returns the externally-managed scheduler, or null if the runner should create its own.
     * <p>
     * When an external scheduler is provided, the runner will NOT shut it down on close -
     * the caller retains ownership and is responsible for its lifecycle.
     *
     * @return the external scheduler, or null to use an internally-managed one
     */
    public ScheduledExecutorService scheduler() {
        return scheduler;
    }

    @Override
    public String toString() {
        return String.format(
                "InlineTuiConfig[height=%d, tickRate=%s, pollTimeout=%s, clearOnClose=%s]",
                height, tickRate, pollTimeout, clearOnClose);
    }

    /**
     * Builder for {@link InlineTuiConfig}.
     */
    public static final class Builder {
        private final int height;
        private Duration tickRate = Duration.ofMillis(DEFAULT_TICK_RATE);
        private Duration pollTimeout = Duration.ofMillis(DEFAULT_POLL_TIMEOUT);
        private boolean clearOnClose = false;
        private Bindings bindings = BindingSets.defaults();
        private ScheduledExecutorService scheduler;

        private Builder(int height) {
            if (height <= 0) {
                throw new IllegalArgumentException("Height must be positive");
            }
            this.height = height;
        }

        /**
         * Sets the tick interval for animations.
         * <p>
         * A shorter interval provides smoother animations but uses more CPU.
         * Common values:
         * <ul>
         *   <li>16ms (~60fps) - very smooth</li>
         *   <li>40ms (~25fps) - good default</li>
         *   <li>100ms (~10fps) - simple progress updates</li>
         * </ul>
         *
         * @param tickRate the tick interval, or null to disable ticks
         * @return this builder
         */
        public Builder tickRate(Duration tickRate) {
            this.tickRate = tickRate;
            return this;
        }

        /**
         * Disables tick events.
         * <p>
         * Use this for purely event-driven inline UIs that don't need animations.
         *
         * @return this builder
         */
        public Builder noTick() {
            this.tickRate = null;
            return this;
        }

        /**
         * Sets the timeout for polling events.
         *
         * @param pollTimeout the poll timeout
         * @return this builder
         */
        public Builder pollTimeout(Duration pollTimeout) {
            this.pollTimeout = pollTimeout != null ? pollTimeout : Duration.ofMillis(DEFAULT_POLL_TIMEOUT);
            return this;
        }

        /**
         * Configures whether to clear the display when closed.
         * <p>
         * When true, the inline display area is cleared on close, leaving no trace.
         * When false (default), the final content remains visible.
         *
         * @param clearOnClose true to clear on close
         * @return this builder
         */
        public Builder clearOnClose(boolean clearOnClose) {
            this.clearOnClose = clearOnClose;
            return this;
        }

        /**
         * Sets the key bindings for semantic action matching.
         *
         * @param bindings the bindings to use
         * @return this builder
         */
        public Builder bindings(Bindings bindings) {
            this.bindings = bindings != null ? bindings : BindingSets.defaults();
            return this;
        }

        /**
         * Sets an externally-managed scheduler.
         * <p>
         * When an external scheduler is provided, the runner will NOT shut it down
         * on close - the caller retains ownership and is responsible for its lifecycle.
         * <p>
         * This is useful when multiple runners should share a single scheduler,
         * or when integrating with frameworks that manage their own thread pools.
         *
         * @param scheduler the scheduler to use, or null to create an internal one
         * @return this builder
         */
        public Builder scheduler(ScheduledExecutorService scheduler) {
            this.scheduler = scheduler;
            return this;
        }

        /**
         * Builds the configuration.
         *
         * @return a new InlineTuiConfig
         */
        public InlineTuiConfig build() {
            return new InlineTuiConfig(height, tickRate, pollTimeout, clearOnClose, bindings, scheduler);
        }
    }
}
