/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.demo.filemanager;

import dev.tamboui.toolkit.app.ToolkitRunner;
import dev.tamboui.tui.TuiConfig;

import java.nio.file.Path;
import java.time.Duration;

/**
 * Two-panel file manager demo showcasing MVC architecture.
 *
 * <p>Architecture:
 * <ul>
 *   <li>{@link FileManagerController} - Application state and file operations</li>
 *   <li>{@link DirectoryBrowserController} - Single directory browsing state</li>
 *   <li>{@link FileManagerView} - Main UI composition and browser panel rendering</li>
 *   <li>{@link FileManagerKeyHandler} - Event routing to controllers</li>
 * </ul>
 *
 * <p>Key bindings:
 * <ul>
 *   <li>Tab/Left/Right - Switch between panels</li>
 *   <li>Up/Down/PgUp/PgDn - Navigate files</li>
 *   <li>Enter - Open directory</li>
 *   <li>Backspace - Go to parent</li>
 *   <li>Space/Insert - Mark file</li>
 *   <li>+/-/* - Mark all/Unmark all/Invert marks</li>
 *   <li>F5/C - Copy to other panel</li>
 *   <li>F6/M - Move to other panel</li>
 *   <li>F8/D - Delete</li>
 *   <li>R - Refresh</li>
 *   <li>Q - Quit</li>
 * </ul>
 */
public class FileManagerDemo {

    public static void main(String[] args) throws Exception {
        // Determine starting directories
        var home = Path.of(System.getProperty("user.home"));
        var leftStart = args.length > 0 ? Path.of(args[0]) : home;
        var rightStart = args.length > 1 ? Path.of(args[1]) : home;

        // Create the model
        var manager = new FileManagerController(leftStart, rightStart);

        // Create the view (implements Element with handleKeyEvent)
        var view = new FileManagerView(manager);

        // Run the application
        var config = TuiConfig.builder()
            .tickRate(Duration.ofMillis(50))
            .build();

        try (var runner = ToolkitRunner.create(config)) {
            runner.run(() -> view);
        }
    }
}
