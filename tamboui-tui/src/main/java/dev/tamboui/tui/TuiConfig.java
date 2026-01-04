/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.tui;

import dev.tamboui.tui.keymap.KeyMap;
import dev.tamboui.tui.keymap.KeyMaps;

import java.time.Duration;

/**
 * Configuration options for {@link TuiRunner}.
 */
public final class TuiConfig {

    private final boolean rawMode;
    private final boolean alternateScreen;
    private final boolean hideCursor;
    private final boolean mouseCapture;
    private final Duration pollTimeout;
    private final Duration tickRate;
    private final boolean shutdownHook;
    private final KeyMap keyMap;

    public TuiConfig(
            boolean rawMode,
            boolean alternateScreen,
            boolean hideCursor,
            boolean mouseCapture,
            Duration pollTimeout,
            Duration tickRate,
            boolean shutdownHook,
            KeyMap keyMap
    ) {
        this.rawMode = rawMode;
        this.alternateScreen = alternateScreen;
        this.hideCursor = hideCursor;
        this.mouseCapture = mouseCapture;
        this.pollTimeout = pollTimeout;
        this.tickRate = tickRate;
        this.shutdownHook = shutdownHook;
        this.keyMap = keyMap;
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
                Duration.ofMillis(100),      // pollTimeout
                Duration.ofMillis(100),      // tickRate
                true,                        // shutdownHook
                KeyMaps.defaults()           // keyMap
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
     * Returns whether a shutdown hook is registered to restore the terminal.
     */
    public boolean shutdownHook() {
        return shutdownHook;
    }

    /**
     * Returns the keymap used for semantic key action matching.
     *
     * @return the configured keymap
     * @see KeyMaps
     */
    public KeyMap keyMap() {
        return keyMap;
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
                && keyMap.equals(that.keyMap);
    }

    @Override
    public int hashCode() {
        int result = Boolean.hashCode(rawMode);
        result = 31 * result + Boolean.hashCode(alternateScreen);
        result = 31 * result + Boolean.hashCode(hideCursor);
        result = 31 * result + Boolean.hashCode(mouseCapture);
        result = 31 * result + pollTimeout.hashCode();
        result = 31 * result + (tickRate != null ? tickRate.hashCode() : 0);
        result = 31 * result + keyMap.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return String.format(
                "TuiConfig[rawMode=%s, alternateScreen=%s, hideCursor=%s, mouseCapture=%s, pollTimeout=%s, tickRate=%s, shutdownHook=%s, keyMap=%s]",
                rawMode,
                alternateScreen,
                hideCursor,
                mouseCapture,
                pollTimeout,
                tickRate,
                shutdownHook,
                keyMap
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
        private Duration pollTimeout = Duration.ofMillis(100);
        private Duration tickRate = Duration.ofMillis(100);
        private boolean shutdownHook = true;
        private KeyMap keyMap = KeyMaps.defaults();

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
            this.pollTimeout = pollTimeout != null ? pollTimeout : Duration.ofMillis(100);
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
         * Sets the keymap for semantic key action matching.
         * <p>
         * Use predefined keymaps from {@link KeyMaps}:
         * <ul>
         *   <li>{@link KeyMaps#standard()} - Arrow keys only (default)</li>
         *   <li>{@link KeyMaps#vim()} - Vim-style navigation (hjkl)</li>
         *   <li>{@link KeyMaps#emacs()} - Emacs-style navigation (Ctrl+n/p/f/b)</li>
         *   <li>{@link KeyMaps#intellij()} - IntelliJ IDEA-style</li>
         *   <li>{@link KeyMaps#vscode()} - VS Code-style</li>
         * </ul>
         *
         * @param keyMap the keymap to use
         * @return this builder
         */
        public Builder keyMap(KeyMap keyMap) {
            this.keyMap = keyMap != null ? keyMap : KeyMaps.defaults();
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
                    shutdownHook,
                    keyMap
            );
        }
    }
}
