//DEPS dev.tamboui:tamboui-toolkit:LATEST
//DEPS dev.tamboui:tamboui-aesh-backend:LATEST
//DEPS org.aesh:terminal-ssh:3.0
//DEPS org.aesh:terminal-http:3.0
//DEPS org.apache.sshd:sshd-core:2.14.0
//DEPS org.apache.sshd:sshd-netty:2.14.0
//DEPS io.netty:netty-all:4.1.81.Final
/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.demo.aesh;

import dev.tamboui.backend.aesh.AeshBackend;
import dev.tamboui.style.Color;
import dev.tamboui.toolkit.app.ToolkitApp;
import dev.tamboui.toolkit.app.ToolkitRunner;
import dev.tamboui.toolkit.element.Element;
import dev.tamboui.toolkit.event.EventResult;
import dev.tamboui.tui.TuiConfig;
import dev.tamboui.tui.event.KeyEvent;
import org.aesh.terminal.Connection;
import org.aesh.terminal.ssh.netty.NettySshTtyBootstrap;
import org.aesh.terminal.http.netty.NettyWebsocketTtyBootstrap;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

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

    private static final int SSH_PORT = 2222;
    private static final int HTTP_PORT = 8080;

    private final ExecutorService executor = Executors.newCachedThreadPool();

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

    private void start() throws Exception {
        // Start SSH server using aesh-terminal-ssh bootstrap
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

        // Start HTTP/WebSocket server using aesh-terminal-http bootstrap
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

    /**
     * Called by the SSH/HTTP libraries when a new Connection is established.
     */
    @Override
    public void accept(Connection connection) {
        connection.setCloseHandler(close -> {
            // Connection closed
        });

        executor.submit(() -> {
            try {
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
     * Simple demo app to run on SSH and HTTP connections.
     */
    private static class DemoApp extends ToolkitApp {
        
        private int counter = 0;
        
        @Override
        protected Element render() {
            return column(
                panel(
                    text(" TamboUI Demo ").bold().cyan()
                ).rounded().borderColor(Color.CYAN).length(3),
                
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
                .onKeyEvent(this::handleKey),
                
                panel(
                    text(" q: Quit | c: Increment Counter ").dim()
                ).rounded().borderColor(Color.DARK_GRAY).length(3)
            );
        }
        
        private EventResult handleKey(KeyEvent event) {
            if (event.isChar('c')) {
                counter++;
                return EventResult.HANDLED;
            }
            return EventResult.UNHANDLED;
        }
    }
}
