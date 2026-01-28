/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.tui;

import dev.tamboui.tui.bindings.Bindings;
import dev.tamboui.tui.bindings.BindingSets;
import dev.tamboui.tui.error.RenderErrorHandler;
import dev.tamboui.tui.error.RenderErrorHandlers;

import java.io.PrintStream;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Configuration options for {@link TuiRunner}.
 */
public final class TuiConfig {

    /** Default poll timeout in milliseconds. */
    public static final int DEFAULT_POLL_TIMEOUT = 40;
    /** Default tick rate in milliseconds. */
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
    private final RenderErrorHandler errorHandler;
    private final PrintStream errorOutput;
    private final boolean fpsOverlayEnabled;
    private final List<PostRenderProcessor> postRenderProcessors;

    /**
     * Creates a new TUI configuration with the specified options.
     * <p>
     * Prefer using {@link #builder()} or {@link #defaults()} instead of this constructor.
     *
     * @param rawMode whether to enable raw terminal mode
     * @param alternateScreen whether to use the alternate screen buffer
     * @param hideCursor whether to hide the cursor
     * @param mouseCapture whether to capture mouse events
     * @param pollTimeout timeout for polling events
     * @param tickRate interval between tick events, or null to disable
     * @param resizeGracePeriod grace period for resize events, or null to disable
     * @param shutdownHook whether to register a JVM shutdown hook
     * @param bindings the key/mouse bindings for semantic actions
     * @param errorHandler the handler for render errors
     * @param errorOutput the output stream for error logging
     * @param fpsOverlayEnabled whether to show the FPS overlay
     * @param postRenderProcessors list of post-render processors
     */
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
            RenderErrorHandler errorHandler,
            PrintStream errorOutput,
            boolean fpsOverlayEnabled,
            List<PostRenderProcessor> postRenderProcessors
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
        this.errorHandler = errorHandler;
        this.errorOutput = errorOutput;
        this.fpsOverlayEnabled = fpsOverlayEnabled;
        this.postRenderProcessors = postRenderProcessors != null
                ? Collections.unmodifiableList(new ArrayList<>(postRenderProcessors))
                : Collections.emptyList();
    }

    /**
     * Returns the default configuration.
     * <p>
     * By default, tick events are generated every 100ms to ensure periodic UI refresh.
     * Use {@link Builder#noTick()} to disable automatic ticking.
     *
     * @return the default TUI configuration
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
                RenderErrorHandlers.displayAndQuit(),  // errorHandler
                System.err,                  // errorOutput
                false,                       // fpsOverlayEnabled
                Collections.emptyList()      // postRenderProcessors
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
     *
     * @return a new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Returns true if tick events are enabled.
     *
     * @return true if tick rate is configured
     */
    public boolean ticksEnabled() {
        return tickRate != null;
    }

    /**
     * Returns whether raw mode is enabled.
     *
     * @return true if raw mode is enabled
     */
    public boolean rawMode() {
        return rawMode;
    }

    /**
     * Returns whether the alternate screen buffer is used.
     *
     * @return true if the alternate screen buffer is used
     */
    public boolean alternateScreen() {
        return alternateScreen;
    }

    /**
     * Returns whether the cursor should be hidden.
     *
     * @return true if the cursor is hidden
     */
    public boolean hideCursor() {
        return hideCursor;
    }

    /**
     * Returns whether mouse capture is enabled.
     *
     * @return true if mouse capture is enabled
     */
    public boolean mouseCapture() {
        return mouseCapture;
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
     * Returns the tick interval, or null if ticks are disabled.
     *
     * @return the tick rate duration, or null
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
     *
     * @return true if a shutdown hook is registered
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
     * Returns the error handler for render errors.
     *
     * @return the error handler
     * @see RenderErrorHandler
     */
    public RenderErrorHandler errorHandler() {
        return errorHandler;
    }

    /**
     * Returns the output stream for error logging.
     * <p>
     * This stream is used to log fatal errors when the TUI has captured
     * standard streams and stack traces would otherwise be invisible.
     *
     * @return the error output stream
     */
    public PrintStream errorOutput() {
        return errorOutput;
    }

    /**
     * Returns whether the FPS overlay is enabled.
     *
     * @return true if FPS overlay is enabled
     */
    public boolean fpsOverlayEnabled() {
        return fpsOverlayEnabled;
    }

    /**
     * Returns the list of post-render processors.
     * <p>
     * Processors are called in order after the main renderer completes.
     *
     * @return an unmodifiable list of post-render processors
     */
    public List<PostRenderProcessor> postRenderProcessors() {
        return postRenderProcessors;
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
        private RenderErrorHandler errorHandler = RenderErrorHandlers.displayAndQuit();
        private PrintStream errorOutput = System.err;
        private boolean fpsOverlayEnabled = false;
        private final List<PostRenderProcessor> postRenderProcessors = new ArrayList<>();

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
         * Sets the error handler for render errors.
         * <p>
         * The handler is invoked when an exception occurs during rendering.
         * Use factory methods from {@link RenderErrorHandlers} for common behaviors.
         *
         * @param errorHandler the error handler to use
         * @return this builder
         * @see RenderErrorHandlers
         */
        public Builder errorHandler(RenderErrorHandler errorHandler) {
            this.errorHandler = errorHandler != null ? errorHandler : RenderErrorHandlers.displayAndQuit();
            return this;
        }

        /**
         * Sets the output stream for error logging.
         * <p>
         * This stream is used to log fatal errors when the TUI has captured
         * standard streams. Defaults to {@code System.err}.
         *
         * @param errorOutput the error output stream
         * @return this builder
         */
        public Builder errorOutput(PrintStream errorOutput) {
            this.errorOutput = errorOutput != null ? errorOutput : System.err;
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
         * Adds a post-render processor.
         * <p>
         * Post-render processors are called after each frame is rendered,
         * allowing for effects, overlays, or other post-processing.
         * Processors are called in the order they are added.
         *
         * @param processor the processor to add
         * @return this builder
         */
        public Builder postRenderProcessor(PostRenderProcessor processor) {
            if (processor != null) {
                this.postRenderProcessors.add(processor);
            }
            return this;
        }

        /**
         * Builds the configuration.
         *
         * @return the constructed TuiConfig
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
                    errorHandler,
                    errorOutput,
                    fpsOverlayEnabled,
                    postRenderProcessors
            );
        }
    }
}
