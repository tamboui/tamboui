//DEPS dev.tamboui:tamboui-toolkit:LATEST
//DEPS dev.tamboui:tamboui-aesh-backend:LATEST
//DEPS org.aesh:terminal-ssh:3.1
//DEPS org.aesh:terminal-http:3.1
//DEPS org.apache.sshd:sshd-core:2.14.0
//DEPS org.apache.sshd:sshd-netty:2.14.0
//DEPS io.netty:netty-all:4.1.81.Final
/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.demo.aesh;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.aesh.terminal.Connection;
import org.aesh.terminal.http.netty.NettyWebsocketTtyBootstrap;
import org.aesh.terminal.ssh.netty.NettySshTtyBootstrap;

import dev.tamboui.backend.aesh.AeshBackend;
import dev.tamboui.style.Color;
import dev.tamboui.toolkit.app.ToolkitApp;
import dev.tamboui.toolkit.app.ToolkitRunner;
import dev.tamboui.toolkit.element.Element;
import dev.tamboui.toolkit.event.EventResult;
import dev.tamboui.tui.TuiConfig;
import dev.tamboui.tui.event.KeyEvent;
import dev.tamboui.tui.event.MouseEvent;
import dev.tamboui.widgets.input.TextAreaState;

import static dev.tamboui.toolkit.Toolkit.*;

/**
 * Demo showcasing TamboUI app accessible via SSH and HTTP/WebSocket.
 * <p>
 * This demo starts:
 * <ul>
 *   <li>An SSH server on port 2222</li>
 *   <li>An HTTP/WebSocket server on port 8080</li>
 * </ul>
 * <p>
 * Connect via SSH:
 * <pre>{@code ssh -p 2222 user@localhost}</pre>
 * <p>
 * Connect via WebSocket:
 * Open http://localhost:8080 in a browser with WebSocket terminal support
 */
public class AeshSshHttpDemo implements java.util.function.Consumer<Connection> {

    /**
     * Creates a new AeshSshHttpDemo instance.
     */
    public AeshSshHttpDemo() {
       
    }
    // ==================== Server Configuration ====================

    private static final int SSH_PORT = 2222;
    private static final int HTTP_PORT = 8080;

    // ==================== Shared State ====================

    private final ExecutorService executor = Executors.newCachedThreadPool();

    // ==================== Main Entry Point ====================

    /**
     * Main entry point for the demo.
     * Starts the SSH and HTTP servers and keeps the program running until interrupted.
     * @param args the command line arguments
     * @throws Exception if an error occurs
     */
    public static void main(String[] args) throws Exception {
        var demo = new AeshSshHttpDemo();
        try {
            demo.start();
            System.out.println("TamboUI SSH/HTTP Demo started:");
            System.out.println("  SSH: ssh -p " + SSH_PORT + " user@localhost");
            System.out.println("  HTTP: http://localhost:" + HTTP_PORT);
            System.out.println("\nPress Ctrl+C to stop...");
            
            // Keep running until interrupted
            Thread.currentThread().join();
        } catch (InterruptedException e) {
            System.out.println("\nShutting down...");
        } finally {
            demo.stop();
        }
    }

    // ==================== Server Startup ====================

    private void start() throws Exception {
        startSshServer();
        startHttpServer();
    }

    // ==================== SSH-Specific Server Setup ====================

    /**
     * Starts the SSH server using aesh-terminal-ssh bootstrap.
     * This is SSH-specific code.
     */
    private void startSshServer() {
        try {
            NettySshTtyBootstrap bootstrap = new NettySshTtyBootstrap();
            bootstrap.setPort(SSH_PORT);
            bootstrap.setHost("localhost");
            bootstrap.start(this).get(10, TimeUnit.SECONDS);
            System.out.println("SSH server started on port " + SSH_PORT);
        } catch (Exception e) {
            System.err.println("Could not start SSH server: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ==================== HTTP-Specific Server Setup ====================

    /**
     * Starts the HTTP/WebSocket server using aesh-terminal-http bootstrap.
     * This is HTTP-specific code.
     */
    private void startHttpServer() {
        try {
            NettyWebsocketTtyBootstrap bootstrap = new NettyWebsocketTtyBootstrap();
            bootstrap.setPort(HTTP_PORT);
            bootstrap.setHost("localhost");
            bootstrap.start(this).get(10, TimeUnit.SECONDS);
            System.out.println("HTTP server started on port " + HTTP_PORT);
        } catch (Exception e) {
            System.err.println("Could not start HTTP server: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ==================== Shared Connection Handling ====================

    /**
     * Called by both SSH and HTTP libraries when a new Connection is established.
     * This is shared code - works identically for both protocols.
     */
    @Override
    public void accept(Connection connection) {
        connection.setCloseHandler(close -> {
            // Connection closed
        });

        executor.submit(() -> {
            try {
                runTuiApp(connection);
            } catch (Exception e) {
                System.err.println("Error running TUI app: " + e.getMessage());
                e.printStackTrace();
            } finally {
                try {
                    connection.close();
                } catch (Exception e) {
                    // Ignore
                }
            }
        });
    }

    /**
     * Runs the TUI application for a given connection.
     * This is shared code - works identically for both SSH and HTTP connections.
     */
    private void runTuiApp(Connection connection) throws Exception {
        AeshBackend backend = new AeshBackend(connection);
        TuiConfig config = TuiConfig.builder()
            .backend(backend)
            .rawMode(true)
            .alternateScreen(true)
            .hideCursor(true)
            .mouseCapture(true)
            .build();
        try (ToolkitRunner runner = ToolkitRunner.create(config)) {
            var app = new DemoApp();
            runner.run(() -> app.render());
        }
    }

    // ==================== Shared Cleanup ====================

    private void stop() {
        // Shutdown executor - servers will be stopped when JVM exits
        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
        }
    }

    // ==================== Shared TUI Application ====================

    /**
     * The TUI application that runs on both SSH and HTTP connections.
     * This is shared code - the same app works for both protocols.
     */
    private static class DemoApp extends ToolkitApp {
        
        private int counter = 0;
        private final TextAreaState eventLog = new TextAreaState();
        
        @Override
        protected Element render() {
            return column(
                panel(
                    text(" TamboUI Demo ").bold().cyan()
                ).rounded().borderColor(Color.CYAN).length(3),
                
                column(
                    panel(
                        column(
                            text("Welcome to TamboUI!").bold().green(),
                            text(""),
                            text("Counter: " + counter).yellow(),
                            text(""),
                            text("Press 'q' to quit").dim(),
                            text("Press 'c' to increment counter").dim()
                        )
                    )
                    .rounded()
                    .borderColor(Color.WHITE)
                    .fill()
                    .focusable()
                    .onKeyEvent(this::handleKey)
                    .onMouseEvent(this::handleMouse),
                    
                    panel(
                        column(
                            text(" Event Log ").bold().cyan(),
                            textArea(eventLog)
                                .fill()
                                .focusable(false)
                        )
                    )
                    .rounded()
                    .borderColor(Color.CYAN)
                    .percent(50)
                )
                .fill(),
                
                panel(
                    text(" q: Quit | c: Increment Counter ").dim()
                ).rounded().borderColor(Color.DARK_GRAY).length(3)
            );
        }
        
        private EventResult handleKey(KeyEvent event) {
            // Log the event
            String eventStr = String.format("KeyEvent: code=%s, char='%s', modifiers=%s",
                event.code(), 
                event.character() != 0 ? event.character() : ' ',
                event.modifiers());
            appendToLog(eventStr);
            
            // Handle specific keys
            if (event.isChar('c')) {
                counter++;
                return EventResult.HANDLED;
            }
            return EventResult.UNHANDLED;
        }
        
        private EventResult handleMouse(MouseEvent event) {
            // Log the event
            String eventStr = String.format("MouseEvent: kind=%s, button=%s, x=%d, y=%d, modifiers=%s",
                event.kind(),
                event.button(),
                event.x(),
                event.y(),
                event.modifiers());
            appendToLog(eventStr);
            
            return EventResult.UNHANDLED;
        }
        
        private void appendToLog(String message) {
            String currentText = eventLog.text();
            String newText = currentText.isEmpty() ? message : currentText + "\n" + message;
            // Keep only last 100 lines to avoid memory issues
            String[] lines = newText.split("\n");
            if (lines.length > 100) {
                newText = String.join("\n", java.util.Arrays.copyOfRange(lines, lines.length - 100, lines.length));
            }
            eventLog.setText(newText);
            // Auto-scroll to bottom - setText already moves cursor to end,
            // ensureCursorVisible will scroll to show it (using large visibleRows to allow full scroll)
            eventLog.ensureCursorVisible(1000, 200);
        }
    }
}
