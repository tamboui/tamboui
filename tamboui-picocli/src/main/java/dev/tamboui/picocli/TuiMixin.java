/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.picocli;

import dev.tamboui.tui.TuiConfig;
import picocli.CommandLine.Option;

import java.time.Duration;

/**
 * PicoCLI mixin that provides common TUI-related command line options.
 * <p>
 * Use this mixin to add TUI options to your existing commands without
 * extending {@link TuiCommand}.
 *
 * <pre>{@code
 * @Command(name = "myapp")
 * public class MyApp implements Callable<Integer> {
 *
 *     @Mixin
 *     TuiMixin tuiOptions;
 *
 *     @Override
 *     public Integer call() throws Exception {
 *         TuiConfig config = tuiOptions.toConfig();
 *         try (TuiRunner runner = TuiRunner.create(config)) {
 *             runner.run(...);
 *         }
 *         return 0;
 *     }
 * }
 * }</pre>
 *
 * @see TuiCommand
 */
public class TuiMixin {

    /**
     * Creates a new TuiMixin with default option values.
     */
    public TuiMixin() {
    }

    @Option(
        names = {"--no-alt-screen"},
        description = "Disable alternate screen mode",
        negatable = true,
        defaultValue = "false"
    )
    private boolean noAltScreen;

    @Option(
        names = {"--show-cursor"},
        description = "Show cursor in TUI mode",
        defaultValue = "false"
    )
    private boolean showCursor;

    @Option(
        names = {"--mouse"},
        description = "Enable mouse capture",
        defaultValue = "false"
    )
    private boolean mouseCapture;

    @Option(
        names = {"--tick-rate"},
        description = "Tick rate in milliseconds for animations (0 to disable)",
        defaultValue = "0"
    )
    private int tickRateMs;

    @Option(
        names = {"--poll-timeout"},
        description = "Event poll timeout in milliseconds",
        defaultValue = "100"
    )
    private int pollTimeoutMs;

    /**
     * Creates a TuiConfig based on the parsed CLI options.
     *
     * @return the TuiConfig
     */
    public TuiConfig toConfig() {
        TuiConfig.Builder builder = TuiConfig.builder()
                .alternateScreen(!noAltScreen)
                .hideCursor(!showCursor)
                .mouseCapture(mouseCapture)
                .pollTimeout(Duration.ofMillis(pollTimeoutMs));

        if (tickRateMs > 0) {
            builder.tickRate(Duration.ofMillis(tickRateMs));
        }

        return builder.build();
    }

    /**
     * Returns true if alternate screen mode is disabled.
     *
     * @return true if alternate screen is disabled
     */
    public boolean isNoAltScreen() {
        return noAltScreen;
    }

    /**
     * Returns true if the cursor should be shown.
     *
     * @return true if cursor should be visible
     */
    public boolean isShowCursor() {
        return showCursor;
    }

    /**
     * Returns true if mouse capture is enabled.
     *
     * @return true if mouse capture is enabled
     */
    public boolean isMouseCapture() {
        return mouseCapture;
    }

    /**
     * Returns the tick rate in milliseconds, or 0 if ticks are disabled.
     *
     * @return the tick rate in milliseconds
     */
    public int getTickRateMs() {
        return tickRateMs;
    }

    /**
     * Returns the poll timeout in milliseconds.
     *
     * @return the poll timeout in milliseconds
     */
    public int getPollTimeoutMs() {
        return pollTimeoutMs;
    }
}
