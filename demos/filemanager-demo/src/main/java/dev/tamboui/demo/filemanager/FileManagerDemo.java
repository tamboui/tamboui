//DEPS dev.tamboui:tamboui-toolkit:LATEST
//DEPS dev.tamboui:tamboui-jline3-backend:LATEST
//DEPS dev.tamboui:tamboui-image:LATEST
//DEPS dev.tamboui:tamboui-panama-backend:LATEST


//SOURCES FileManagerController.java FileManagerView.java FileManagerKeyHandler.java DirectoryBrowserController.java
// Prevents OSX from showing up in the terminal when running the demo
//JAVA_OPTIONS -Dapple.awt.UIElement=true

/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.demo.filemanager;

import dev.tamboui.toolkit.app.ToolkitRunner;
import dev.tamboui.tui.TuiConfig;

import java.nio.file.Path;
import java.nio.file.Paths;
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
 *   <li>V - View file (text files show in scrollable paragraph, PNG images show in Image widget)</li>
 *   <li>R - Refresh</li>
 *   <li>Q - Quit</li>
 * </ul>
 * <p>Viewer key bindings:
 * <ul>
 *   <li>Esc - Close viewer</li>
 *   <li>Up/Down - Scroll text (text files only)</li>
 *   <li>PgUp/PgDn - Page scroll text (text files only)</li>
 * </ul>
 */
public class FileManagerDemo {

    private FileManagerDemo() {

    }

    /**
     * Demo entry point.
     * @param args the CLI arguments
     * @throws Exception on unexpected error
     */
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
