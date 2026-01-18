//DEPS dev.tamboui:tamboui-toolkit:LATEST
//DEPS dev.tamboui:tamboui-jline:LATEST
//DEPS dev.tamboui:tamboui-image:LATEST
//SOURCES FileManagerController.java FileManagerView.java FileManagerKeyHandler.java DirectoryBrowserController.java
// Prevents OSX from showing up in the terminal when running the demo
//JAVA_OPTIONS -Dapple.awt.UIElement=true

/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.demo.filemanager;

import dev.tamboui.toolkit.app.ToolkitRunner;
import dev.tamboui.tui.TuiConfig;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;

/// Two-panel file manager demo showcasing MVC architecture.
///
///
///
/// Architecture:
///
/// - {@link FileManagerController} - Application state and file operations
/// - {@link DirectoryBrowserController} - Single directory browsing state
/// - {@link FileManagerView} - Main UI composition and browser panel rendering
/// - {@link FileManagerKeyHandler} - Event routing to controllers
///
///
///
///
/// Key bindings:
///
/// - Tab/Left/Right - Switch between panels
/// - Up/Down/PgUp/PgDn - Navigate files
/// - Enter - Open directory
/// - Backspace - Go to parent
/// - Space/Insert - Mark file
/// - +/-/* - Mark all/Unmark all/Invert marks
/// - F5/C - Copy to other panel
/// - F6/M - Move to other panel
/// - F8/D - Delete
/// - V - View file (text files show in scrollable paragraph, PNG images show in Image widget)
/// - R - Refresh
/// - Q - Quit
///
///
///
/// Viewer key bindings:
///
/// - Esc - Close viewer
/// - Up/Down - Scroll text (text files only)
/// - PgUp/PgDn - Page scroll text (text files only)
public class FileManagerDemo {

    public static void main(String[] args) throws Exception {
        // Determine starting directories
        Path home = Paths.get(System.getProperty("user.home"));
        Path leftStart = args.length > 0 ? Paths.get(args[0]) : Paths.get(".");
        Path rightStart = args.length > 1 ? Paths.get(args[1]) : home;

        // Create the model
        FileManagerController manager = new FileManagerController(leftStart, rightStart);

        // Create the view (implements Element with handleKeyEvent)
        FileManagerView view = new FileManagerView(manager);

        // Run the application
        TuiConfig config = TuiConfig.builder()
            .tickRate(Duration.ofMillis(50))
            .build();

        try (ToolkitRunner runner = ToolkitRunner.create(config)) {
            runner.run(() -> view);
        }
    }
}

