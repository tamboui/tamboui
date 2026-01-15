/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.tui;

import dev.tamboui.tui.bindings.Bindings;
import dev.tamboui.tui.bindings.BindingSets;

import java.time.Duration;

/**
 * Configuration options for {@link TuiRunner}.
 */
public final class TuiConfig {

    public static final int DEFAULT_POLL_TIMEOUT = 40;
    public static final int DEFAULT_TICK_TIMEOUT = 40;
    /**
     * Default grace period for resize events (250ms).
     * This ensures resize events are processed within a reasonable time even when ticks are disabled.
     */
    public static final int DEFAULT_RESIZE_GRACE_PERIOD = 250;
    private final boolean rawMode;
    private final boolean alternateScreen;
    private final boolean hideCursor;
    private final boolean mouseCapture;
    private final Duration pollTimeout;
    private final Duration tickRate;
    private final Duration resizeGracePeriod;
    private final boolean shutdownHook;
    private final Bindings bindings;
    private final boolean fpsOverlayEnabled;

    public TuiConfig(
            boolean rawMode,
            boolean alternateScreen,
            boolean hideCursor,
            boolean mouseCapture,
            Duration pollTimeout,
            Duration tickRate,
            Duration resizeGracePeriod,
            boolean shutdownHook,
            Bindings bindings,
            boolean fpsOverlayEnabled
    ) {
        this.rawMode = rawMode;
        this.alternateScreen = alternateScreen;
        this.hideCursor = hideCursor;
        this.mouseCapture = mouseCapture;
        this.pollTimeout = pollTimeout;
        this.tickRate = tickRate;
        this.resizeGracePeriod = resizeGracePeriod;
        this.shutdownHook = shutdownHook;
        this.bindings = bindings;
        this.fpsOverlayEnabled = fpsOverlayEnabled;
    }

    /**
     * Returns the default configuration.
     * <p>
     * By default, tick events are generated every 100ms to ensure periodic UI refresh.
     * Use {@link Builder#noTick()} to disable automatic ticking.
     */
    public static TuiConfig defaults() {
        return new TuiConfig(
                true,                        // rawMode
                true,                        // alternateScreen
                true,                        // hideCursor
                false,                       // mouseCapture
                Duration.ofMillis(DEFAULT_POLL_TIMEOUT),      // pollTimeout
                Duration.ofMillis(DEFAULT_TICK_TIMEOUT),      // tickRate
                Duration.ofMillis(DEFAULT_RESIZE_GRACE_PERIOD),  // resizeGracePeriod
                true,                        // shutdownHook
                BindingSets.defaults(),      // bindings
                false                        // fpsOverlayEnabled
        );
    }

    /**
     * Returns a configuration suitable for animated applications.
     *
     * @param tickRate the interval between tick events (e.g., Duration.ofMillis(16) for ~60fps)
     * @return configuration with ticks enabled
     */
    public static TuiConfig withAnimation(Duration tickRate) {
        return builder().tickRate(tickRate).build();
    }

    /**
     * Returns a new builder with default values.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Returns true if tick events are enabled.
     */
    public boolean ticksEnabled() {
        return tickRate != null;
    }

    /**
     * Returns whether raw mode is enabled.
     */
    public boolean rawMode() {
        return rawMode;
    }

    /**
     * Returns whether the alternate screen buffer is used.
     */
    public boolean alternateScreen() {
        return alternateScreen;
    }

    /**
     * Returns whether the cursor should be hidden.
     */
    public boolean hideCursor() {
        return hideCursor;
    }

    /**
     * Returns whether mouse capture is enabled.
     */
    public boolean mouseCapture() {
        return mouseCapture;
    }

    /**
     * Returns the poll timeout for reading events.
     */
    public Duration pollTimeout() {
        return pollTimeout;
    }

    /**
     * Returns the tick interval, or null if ticks are disabled.
     */
    public Duration tickRate() {
        return tickRate;
    }

    /**
     * Returns the resize grace period.
     * <p>
     * This defines the maximum time before resize events are processed,
     * ensuring the UI redraws promptly on terminal resize even when
     * ticks are disabled or have a long interval.
     *
     * @return the resize grace period, or null to use the poll timeout
     */
    public Duration resizeGracePeriod() {
        return resizeGracePeriod;
    }

    /**
     * Returns whether a shutdown hook is registered to restore the terminal.
     */
    public boolean shutdownHook() {
        return shutdownHook;
    }

    /**
     * Returns the bindings used for semantic action matching.
     *
     * @return the configured bindings
     * @see BindingSets
     */
    public Bindings bindings() {
        return bindings;
    }

    /**
     * Returns whether the FPS overlay is enabled.
     *
     * @return true if FPS overlay is enabled
     */
    public boolean fpsOverlayEnabled() {
        return fpsOverlayEnabled;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof TuiConfig)) {
            return false;
        }
        TuiConfig that = (TuiConfig) o;
        return rawMode == that.rawMode
                && alternateScreen == that.alternateScreen
                && hideCursor == that.hideCursor
                && mouseCapture == that.mouseCapture
                && pollTimeout.equals(that.pollTimeout)
                && (tickRate != null ? tickRate.equals(that.tickRate) : that.tickRate == null)
                && (resizeGracePeriod != null ? resizeGracePeriod.equals(that.resizeGracePeriod) : that.resizeGracePeriod == null)
                && bindings.equals(that.bindings)
                && fpsOverlayEnabled == that.fpsOverlayEnabled;
    }

    @Override
    public int hashCode() {
        int result = Boolean.hashCode(rawMode);
        result = 31 * result + Boolean.hashCode(alternateScreen);
        result = 31 * result + Boolean.hashCode(hideCursor);
        result = 31 * result + Boolean.hashCode(mouseCapture);
        result = 31 * result + pollTimeout.hashCode();
        result = 31 * result + (tickRate != null ? tickRate.hashCode() : 0);
        result = 31 * result + (resizeGracePeriod != null ? resizeGracePeriod.hashCode() : 0);
        result = 31 * result + bindings.hashCode();
        result = 31 * result + Boolean.hashCode(fpsOverlayEnabled);
        return result;
    }

    @Override
    public String toString() {
        return String.format(
                "TuiConfig[rawMode=%s, alternateScreen=%s, hideCursor=%s, mouseCapture=%s, pollTimeout=%s, tickRate=%s, resizeGracePeriod=%s, shutdownHook=%s, bindings=%s, fpsOverlayEnabled=%s]",
                rawMode,
                alternateScreen,
                hideCursor,
                mouseCapture,
                pollTimeout,
                tickRate,
                resizeGracePeriod,
                shutdownHook,
                bindings,
                fpsOverlayEnabled
        );
    }

    /**
     * Builder for {@link TuiConfig}.
     */
    public static final class Builder {
        private boolean rawMode = true;
        private boolean alternateScreen = true;
        private boolean hideCursor = true;
        private boolean mouseCapture = false;
        private Duration pollTimeout = Duration.ofMillis(DEFAULT_POLL_TIMEOUT);
        private Duration tickRate = Duration.ofMillis(DEFAULT_POLL_TIMEOUT);
        private Duration resizeGracePeriod = Duration.ofMillis(DEFAULT_RESIZE_GRACE_PERIOD);
        private boolean shutdownHook = true;
        private Bindings bindings = BindingSets.defaults();
        private boolean fpsOverlayEnabled = false;

        private Builder() {
        }

        /**
         * Sets whether to register a JVM shutdown hook for cleanup.
         *
         * @param shutdownHook true to register a shutdown hook
         * @return this builder
         */
        public Builder shutdownHook(boolean shutdownHook) {
            this.shutdownHook = shutdownHook;
            return this;
        }

        /**
         * Sets whether to enable raw mode.
         *
         * @param rawMode true to enable raw mode
         * @return this builder
         */
        public Builder rawMode(boolean rawMode) {
            this.rawMode = rawMode;
            return this;
        }

        /**
         * Sets whether to use alternate screen buffer.
         *
         * @param alternateScreen true to use alternate screen
         * @return this builder
         */
        public Builder alternateScreen(boolean alternateScreen) {
            this.alternateScreen = alternateScreen;
            return this;
        }

        /**
         * Sets whether to hide the cursor.
         *
         * @param hideCursor true to hide the cursor
         * @return this builder
         */
        public Builder hideCursor(boolean hideCursor) {
            this.hideCursor = hideCursor;
            return this;
        }

        /**
         * Sets whether to capture mouse events.
         *
         * @param mouseCapture true to enable mouse capture
         * @return this builder
         */
        public Builder mouseCapture(boolean mouseCapture) {
            this.mouseCapture = mouseCapture;
            return this;
        }

        /**
         * Sets the timeout for polling events.
         *
         * @param pollTimeout poll timeout duration (non-null)
         * @return this builder
         */
        public Builder pollTimeout(Duration pollTimeout) {
            this.pollTimeout = pollTimeout != null ? pollTimeout : Duration.ofMillis(DEFAULT_POLL_TIMEOUT);
            return this;
        }

        /**
         * Sets the interval between tick events.
         * Set to null to disable tick events.
         *
         * @param tickRate the tick interval (e.g., Duration.ofMillis(16) for ~60fps)
         * @return this builder
         */
        public Builder tickRate(Duration tickRate) {
            this.tickRate = tickRate;
            return this;
        }

        /**
         * Disables automatic tick events.
         * <p>
         * Use this for purely event-driven UIs that only need to refresh on user input.
         *
         * @return this builder
         */
        public Builder noTick() {
            this.tickRate = null;
            return this;
        }

        /**
         * Sets the resize grace period.
         * <p>
         * This defines the maximum time before resize events are processed,
         * ensuring the UI redraws promptly on terminal resize even when
         * ticks are disabled or have a long interval.
         * <p>
         * Default is {@value #DEFAULT_RESIZE_GRACE_PERIOD}ms.
         *
         * @param resizeGracePeriod the grace period, or null to disable automatic resize handling
         * @return this builder
         */
        public Builder resizeGracePeriod(Duration resizeGracePeriod) {
            this.resizeGracePeriod = resizeGracePeriod;
            return this;
        }

        /**
         * Sets the bindings for semantic action matching.
         * <p>
         * Use predefined binding sets from {@link BindingSets}:
         * <ul>
         *   <li>{@link BindingSets#standard()} - Arrow keys only (default)</li>
         *   <li>{@link BindingSets#vim()} - Vim-style navigation (hjkl)</li>
         *   <li>{@link BindingSets#emacs()} - Emacs-style navigation (Ctrl+n/p/f/b)</li>
         *   <li>{@link BindingSets#intellij()} - IntelliJ IDEA-style</li>
         *   <li>{@link BindingSets#vscode()} - VS Code-style</li>
         * </ul>
         *
         * @param bindings the bindings to use
         * @return this builder
         */
        public Builder bindings(Bindings bindings) {
            this.bindings = bindings != null ? bindings : BindingSets.defaults();
            return this;
        }

        /**
         * Enables or disables the FPS overlay.
         *
         * @param enabled true to enable the FPS overlay
         * @return this builder
         */
        public Builder fpsOverlay(boolean enabled) {
            this.fpsOverlayEnabled = enabled;
            return this;
        }

        /**
         * Builds the configuration.
         */
        public TuiConfig build() {
            return new TuiConfig(
                    rawMode,
                    alternateScreen,
                    hideCursor,
                    mouseCapture,
                    pollTimeout,
                    tickRate,
                    resizeGracePeriod,
                    shutdownHook,
                    bindings,
                    fpsOverlayEnabled
            );
        }
    }
}
