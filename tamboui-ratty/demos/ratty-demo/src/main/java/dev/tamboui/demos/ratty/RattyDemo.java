///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS dev.tamboui:tamboui-widgets:LATEST
//DEPS dev.tamboui:tamboui-ratty:LATEST
//DEPS dev.tamboui:tamboui-jline3-backend:LATEST
//FILES tamboui-logo.obj=../../../../../resources/tamboui-logo.obj
//FILES tamboui-logo.mtl=../../../../../resources/tamboui-logo.mtl

/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.demos.ratty;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import dev.tamboui.layout.Constraint;
import dev.tamboui.layout.Layout;
import dev.tamboui.layout.Rect;
import dev.tamboui.ratty.ObjectFormat;
import dev.tamboui.ratty.PlaceOptions;
import dev.tamboui.ratty.protocol.RattyProtocol;
import dev.tamboui.style.Color;
import dev.tamboui.style.Style;
import dev.tamboui.terminal.Backend;
import dev.tamboui.terminal.BackendFactory;
import dev.tamboui.terminal.Frame;
import dev.tamboui.terminal.Terminal;
import dev.tamboui.text.Line;
import dev.tamboui.text.Span;
import dev.tamboui.text.Text;
import dev.tamboui.widgets.block.Block;
import dev.tamboui.widgets.block.BorderType;
import dev.tamboui.widgets.block.Borders;
import dev.tamboui.widgets.block.Title;
import dev.tamboui.widgets.paragraph.Paragraph;

/**
 * Demo of Ratty Graphics Protocol for 3D object rendering.
 * <p>
 * This demo shows the RGP protocol API and widget usage.
 * <p>
 * <b>Note:</b> This demo requires a terminal that supports Ratty Graphics Protocol
 * (currently only the Ratty terminal emulator). The demo shows the API and
 * instructions for using RGP.
 */
public class RattyDemo {

    private boolean running = true;
    private float rotation = 0f;
    private boolean commandsSent = false;
    private String modelPath = null;
    private boolean useDefaultLogo = false;

    /**
     * Creates a new RattyDemo instance.
     */
    public RattyDemo() {
    }

    /**
     * Demo entry point.
     * @param args the CLI arguments (optional: path to .obj or .glb file, or "--logo" for default)
     * @throws Exception on unexpected error
     */
    public static void main(String[] args) throws Exception {
        RattyDemo demo = new RattyDemo();
        if (args.length > 0) {
            if ("--logo".equals(args[0])) {
                demo.useDefaultLogo = true;
                demo.modelPath = extractDefaultLogo();
            } else {
                demo.modelPath = args[0];
            }
        } else {
            // Use default logo if no argument provided
            demo.useDefaultLogo = true;
            demo.modelPath = extractDefaultLogo();
        }
        
        if (demo.modelPath != null) {
            System.err.println("Using model: " + demo.modelPath);
        } else {
            System.err.println("No model available - showing docs only");
        }
        
        demo.run();
    }

    private static String extractDefaultLogo() throws IOException {
        // JBang puts FILES in current directory, jars have them in classpath
        // Try current directory first (JBang with //FILES)
        Path localLogo = Path.of("tamboui-logo.obj");
        if (Files.exists(localLogo)) {
            System.err.println("Using logo from current directory (JBang mode): " + localLogo.toAbsolutePath());
            return localLogo.toAbsolutePath().toString();
        }
        
        // Try classpath (when running as jar)
        InputStream logoStream = RattyDemo.class.getResourceAsStream("/tamboui-logo.obj");
        if (logoStream == null) {
            logoStream = Thread.currentThread().getContextClassLoader().getResourceAsStream("tamboui-logo.obj");
        }
        if (logoStream == null) {
            logoStream = RattyDemo.class.getClassLoader().getResourceAsStream("tamboui-logo.obj");
        }
        
        if (logoStream != null) {
            // Found in classpath - extract to temp
            Path tempDir = Files.createTempDirectory("tamboui-ratty-");
            Path tempFile = tempDir.resolve("tamboui-logo.obj");
            Files.copy(logoStream, tempFile, StandardCopyOption.REPLACE_EXISTING);
            logoStream.close();
            tempFile.toFile().deleteOnExit();
            tempDir.toFile().deleteOnExit();
            System.err.println("Extracted logo from classpath to: " + tempFile.toAbsolutePath());
            
            // Also extract the material file
            InputStream mtlStream = RattyDemo.class.getResourceAsStream("/tamboui-logo.mtl");
            if (mtlStream == null) {
                mtlStream = Thread.currentThread().getContextClassLoader().getResourceAsStream("tamboui-logo.mtl");
            }
            if (mtlStream == null) {
                mtlStream = RattyDemo.class.getClassLoader().getResourceAsStream("tamboui-logo.mtl");
            }
            if (mtlStream != null) {
                Path mtlFile = tempDir.resolve("tamboui-logo.mtl");
                Files.copy(mtlStream, mtlFile, StandardCopyOption.REPLACE_EXISTING);
                mtlStream.close();
                mtlFile.toFile().deleteOnExit();
            }
            
            return tempFile.toAbsolutePath().toString();
        }
        
        System.err.println("WARNING: Could not find tamboui-logo.obj");
        return null;
    }

    /**
     * Runs the demo application.
     *
     * @throws Exception if an error occurs
     */
    public void run() throws Exception {
        try (Backend backend = BackendFactory.create()) {
            backend.enableRawMode();
            backend.enterAlternateScreen();
            backend.hideCursor();

            Terminal<Backend> terminal = new Terminal<>(backend);

            // Send RGP test commands if we have a model
            if (modelPath != null && !commandsSent) {
                sendTestCommands(backend);
                commandsSent = true;
            }

            // Handle resize
            backend.onResize(() -> {
                terminal.draw(this::ui);
            });

            // Event loop with animation
            while (running) {
                terminal.draw(this::ui);

                int c = backend.read(33); // ~30 FPS
                if (c == 'q' || c == 'Q' || c == 3) {
                    running = false;
                }

                // Update rotation counter for UI display
                rotation += 2f;
                if (rotation >= 360f) {
                    rotation -= 360f;
                }
                
                // Note: We use animate=true in the place command,
                // so the terminal handles rotation natively.
                // This counter is just for the UI display.
            }
            
            // Cleanup: delete the 3D object when exiting
            if (modelPath != null) {
                try {
                    String deleteCmd = RattyProtocol.delete(1);
                    backend.writeRaw(deleteCmd.getBytes("UTF-8"));
                    backend.flush();
                } catch (IOException e) {
                    // Best effort cleanup
                }
            }
        }
    }

    private void ui(Frame frame) {
        Rect area = frame.area();

        var layout = Layout.vertical()
            .constraints(
                Constraint.length(5),  // Header
                Constraint.fill()      // Main content
            )
            .split(area);

        renderHeader(frame, layout.get(0));
        renderMainContent(frame, layout.get(1));
    }

    private void renderHeader(Frame frame, Rect area) {
        Block headerBlock = Block.builder()
            .borders(Borders.ALL)
            .borderType(BorderType.ROUNDED)
            .borderStyle(Style.EMPTY.fg(Color.CYAN))
            .title(Title.from(
                Line.from(
                    Span.raw(" TamboUI ").bold().magenta(),
                    Span.raw("• ").dim(),
                    Span.raw("Ratty Graphics Protocol ").bold().cyan(),
                    Span.raw("Demo ").yellow()
                )
            ).centered())
            .build();

        Paragraph header = Paragraph.builder()
            .block(headerBlock)
            .text(Text.from(
                Line.from(
                    Span.raw("3D Object Rendering in Terminal ").dim(),
                    Span.raw("| ").dim(),
                    Span.raw("Press 'q' to quit").dim()
                )
            ))
            .centered()
            .build();

        frame.renderWidget(header, area);
    }

    private void renderMainContent(Frame frame, Rect area) {
        var cols = Layout.horizontal()
            .constraints(
                Constraint.percentage(50),
                Constraint.percentage(50)
            )
            .split(area);

        renderInstructions(frame, cols.get(0));
        renderCodeExample(frame, cols.get(1));
    }

    private void renderInstructions(Frame frame, Rect area) {
        Paragraph instructions = Paragraph.builder()
            .block(Block.builder()
                .borders(Borders.ALL)
                .borderType(BorderType.ROUNDED)
                .borderStyle(Style.EMPTY.fg(Color.GREEN))
                .title(Title.from(" About Ratty Graphics Protocol "))
                .build())
            .text(Text.from(
                Line.from(Span.raw("About RGP").bold().yellow()),
                Line.empty(),
                Line.from("Ratty Graphics Protocol allows terminals"),
                Line.from("to display 3D objects (OBJ/GLB files)"),
                Line.from("as inline terminal elements."),
                Line.empty(),
                Line.from(Span.raw("TamboUI Support:").bold().magenta()),
                Line.from("• Protocol encoding/decoding"),
                Line.from("• Object lifecycle management"),
                Line.from("• High-level widget API"),
                Line.from("• Transform & animation"),
                Line.empty(),
                Line.from(Span.raw("Current rotation:").bold()),
                Line.from(Span.raw(String.format("  %.1f°", rotation)).green())
            ))
            .build();

        frame.renderWidget(instructions, area);
    }

    private void renderCodeExample(Frame frame, Rect area) {
        Paragraph code = Paragraph.builder()
            .block(Block.builder()
                .borders(Borders.ALL)
                .borderType(BorderType.ROUNDED)
                .borderStyle(Style.EMPTY.fg(Color.MAGENTA))
                .title(Title.from(" Usage Example "))
                .build())
            .text(Text.from(
                Line.from(Span.raw("TamboUI RGP Usage:").bold().magenta()),
                Line.empty(),
                Line.from(Span.raw("1. Register:").bold().cyan()),
                Line.from("   graphics.registerByPath("),
                Line.from("     1, ObjectFormat.OBJ,"),
                Line.from("     \"model.obj\")"),
                Line.empty(),
                Line.from(Span.raw("2. Widget:").bold().cyan()),
                Line.from("   Ratty.builder()"),
                Line.from("     .objectId(1)"),
                Line.from("     .animate(true)"),
                Line.from("     .rotateY(" + (int) rotation + ")"),
                Line.from("     .build()"),
                Line.empty(),
                Line.from(Span.raw("3. Render:").bold().cyan()),
                Line.from("   frame.renderWidget(widget, area)"),
                Line.empty(),
                Line.from(Span.raw("Status:").bold().yellow()),
                Line.from(modelPath != null 
                    ? (useDefaultLogo 
                        ? "✓ TamboUI Logo (rotating!)" 
                        : "✓ Custom: " + new File(modelPath).getName())
                    : "⚠ No model"),
                Line.from(useDefaultLogo 
                    ? "Watch the 3D 'TUI' logo rotate!" 
                    : "Pass .obj/.glb or --logo")
            ))
            .build();

        frame.renderWidget(code, area);
    }

    private void sendTestCommands(Backend backend) throws IOException {
        // Send support query
        String supportQuery = RattyProtocol.supportQuery();
        backend.writeRaw(supportQuery.getBytes("UTF-8"));
        backend.flush();

        // Determine format from file extension
        ObjectFormat format = modelPath.toLowerCase().endsWith(".glb") 
            ? ObjectFormat.GLB 
            : ObjectFormat.OBJ;

        // Register object by path
        String registerCmd = RattyProtocol.registerByPath(1, format, modelPath);
        backend.writeRaw(registerCmd.getBytes("UTF-8"));
        backend.flush();

        // Place object in center of screen
        // Wider placement for full "TamboUI" text
        PlaceOptions options = PlaceOptions.builder(12, 40, 30, 8)
            .animate(true)  // Let terminal handle smooth animation
            .rotateY(0)
            .scale(1.2f)
            .depth(1.0f)
            .build();
        String placeCmd = RattyProtocol.place(1, options);
        backend.writeRaw(placeCmd.getBytes("UTF-8"));
        backend.flush();
    }
    
}
