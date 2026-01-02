/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.picocli;

import dev.tamboui.tui.TuiConfig;
import dev.tamboui.tui.TuiRunner;
import picocli.CommandLine.Mixin;

import java.util.concurrent.Callable;

/**
 * Base class for PicoCLI commands that run TUI applications.
 * <p>
 * Extend this class to create a TUI application with CLI argument parsing.
 * The class handles terminal setup and teardown automatically.
 *
 * <pre>{@code
 * @Command(name = "myapp", description = "My TUI Application")
 * public class MyApp extends TuiCommand {
 *
 *     @Option(names = {"-c", "--config"}, description = "Config file path")
 *     Path configFile;
 *
 *     @Override
 *     protected void runTui(TuiRunner runner) throws Exception {
 *         AppState state = loadState(configFile);
 *
 *         runner.run(
 *             (event, r) -> handleEvent(event, r, state),
 *             frame -> render(frame, state)
 *         );
 *     }
 *
 *     public static void main(String[] args) {
 *         System.exit(new CommandLine(new MyApp()).execute(args));
 *     }
 * }
 * }</pre>
 *
 * @see TuiMixin
 * @see TuiRunner
 */
public abstract class TuiCommand implements Callable<Integer> {

    @Mixin
    private final TuiMixin tuiOptions = new TuiMixin();

    /**
     * Runs the TUI application.
     * <p>
     * Override this method to implement your TUI logic. The TuiRunner
     * is already created and configured based on CLI options.
     *
     * @param runner the configured TUI runner
     * @throws Exception if an error occurs during execution
     */
    protected abstract void runTui(TuiRunner runner) throws Exception;

    /**
     * Creates the TuiConfig for this command.
     * <p>
     * Override this method to customize the configuration beyond what
     * the CLI options provide.
     *
     * @return the TuiConfig to use
     */
    protected TuiConfig createConfig() {
        return tuiOptions.toConfig();
    }

    @Override
    public final Integer call() throws Exception {
        TuiConfig config = createConfig();

        try (TuiRunner runner = TuiRunner.create(config)) {
            runTui(runner);
            return 0;
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            return 1;
        }
    }

    /**
     * Returns the TUI options mixin.
     * <p>
     * This can be used to access the parsed TUI-related CLI options.
     *
     * @return the TUI options
     */
    protected TuiMixin tuiOptions() {
        return tuiOptions;
    }
}
