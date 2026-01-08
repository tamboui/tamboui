///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS dev.tamboui:tamboui-image:LATEST
//DEPS dev.tamboui:tamboui-jline:LATEST

/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.demo;

import dev.tamboui.image.Image;
import dev.tamboui.image.ImageData;
import dev.tamboui.image.ImageScaling;
import dev.tamboui.image.capability.TerminalImageProtocol;
import dev.tamboui.image.capability.TerminalImageCapabilities;
import dev.tamboui.image.protocol.BrailleProtocol;
import dev.tamboui.image.protocol.HalfBlockProtocol;
import dev.tamboui.image.protocol.ImageProtocol;
import dev.tamboui.image.protocol.ITermProtocol;
import dev.tamboui.image.protocol.KittyProtocol;
import dev.tamboui.image.protocol.SixelProtocol;
import dev.tamboui.layout.Constraint;
import dev.tamboui.layout.Layout;
import dev.tamboui.layout.Rect;
import dev.tamboui.layout.Size;
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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Demo TUI application showcasing image rendering capabilities.
 * <p>
 * Demonstrates:
 * <ul>
 *   <li>Terminal capability detection</li>
 *   <li>Image rendering with half-block and Braille protocols</li>
 *   <li>Scaling modes (FIT, FILL, STRETCH, NONE)</li>
 *   <li>Protocol switching via keyboard</li>
 * </ul>
 */
public class ImageDemo {

    private static final String BUNDLED_IMAGE = "/c2023-tsuchinshan-atlas.jpg";
    private static final String BUNDLED_IMAGE_TITLE = "Comet C/2023 Tsuchinshan";

    // Protocol options
    private static final ImageProtocol HALF_BLOCK = new HalfBlockProtocol();
    private static final ImageProtocol BRAILLE = new BrailleProtocol();
    private static final ImageProtocol SIXEL = new SixelProtocol();
    private static final ImageProtocol KITTY = new KittyProtocol();
    private static final ImageProtocol ITERM2 = new ITermProtocol();

    private boolean running = true;
    private final TerminalImageCapabilities capabilities;
    private final ImageData imageData;
    private final String imageTitle;
    private ImageProtocol currentProtocol;
    private ImageScaling currentScaling = ImageScaling.FIT;
    private Backend currentBackend;
    private boolean needsFullRedraw;

    public static void main(String[] args) throws Exception {
        String customImagePath = args.length > 0 ? args[0] : null;
        new ImageDemo(customImagePath).run();
    }

    public ImageDemo(String customImagePath) throws IOException {
        this.capabilities = TerminalImageCapabilities.detect();
        this.currentProtocol = capabilities.bestProtocol();

        // Load image from path or bundled resource
        if (customImagePath != null && Files.exists(Path.of(customImagePath))) {
            this.imageData = ImageData.fromPath(Path.of(customImagePath));
            this.imageTitle = Path.of(customImagePath).getFileName().toString();
        } else {
            this.imageData = ImageData.fromResource(BUNDLED_IMAGE);
            this.imageTitle = BUNDLED_IMAGE_TITLE;
        }
    }

    public void run() throws Exception {
        try (Backend backend = BackendFactory.create()) {
            this.currentBackend = backend;
            backend.enableRawMode();
            backend.enterAlternateScreen();
            backend.hideCursor();

            Terminal<Backend> terminal = new Terminal<>(backend);

            // Handle resize
            backend.onResize(() -> {
                try {
                    terminal.draw(this::ui);
                } catch (IOException e) {
                    // Ignore
                }
            });

            // Event loop
            while (running) {
                // Force full redraw if needed (e.g., after protocol switch)
                if (needsFullRedraw) {
                    terminal.clear();
                    needsFullRedraw = false;
                }

                terminal.draw(this::ui);

                // For native protocols, render image after frame is drawn
                if (currentProtocol.requiresRawOutput()) {
                    renderNativeImage(backend);
                }

                int c = backend.read(100);
                handleInput(c);
            }
        }
    }

    /**
     * Renders the image using a native protocol (Sixel, Kitty, iTerm2).
     */
    private void renderNativeImage(Backend backend) throws IOException {
        // Calculate the image area (same as in ui())
        Rect area = new Rect(0, 0, terminal().width(), terminal().height());
        var layout = Layout.vertical()
            .constraints(
                Constraint.length(3),   // Header
                Constraint.length(7),   // Capabilities info
                Constraint.fill(),      // Image area
                Constraint.length(4)    // Footer/help
            )
            .split(area);

        Rect imageArea = layout.get(2);
        // Account for block borders
        imageArea = new Rect(imageArea.x() + 1, imageArea.y() + 1,
            imageArea.width() - 2, imageArea.height() - 2);

        if (imageArea.isEmpty()) {
            return;
        }

        // Create output stream that writes to backend
        OutputStream rawOutput = new OutputStream() {
            @Override
            public void write(int b) throws IOException {
                backend.writeRaw(new byte[] {(byte) b});
            }

            @Override
            public void write(byte[] b, int off, int len) throws IOException {
                if (off == 0 && len == b.length) {
                    backend.writeRaw(b);
                } else {
                    byte[] slice = new byte[len];
                    System.arraycopy(b, off, slice, 0, len);
                    backend.writeRaw(slice);
                }
            }
        };

        // Render the image
        try {
            currentProtocol.render(imageData, imageArea, null, rawOutput);
            backend.flush();
        } catch (Exception e) {
            // Ignore rendering errors for native protocols
        }
    }

    private Size terminal() throws IOException {
        return currentBackend.size();
    }

    private void handleInput(int c) {
        ImageProtocol previousProtocol = currentProtocol;

        switch (c) {
            case 'q':
            case 'Q':
            case 3: // Ctrl+C
                running = false;
                break;
            case '1':
                currentProtocol = HALF_BLOCK;
                break;
            case '2':
                currentProtocol = BRAILLE;
                break;
            case '3':
                currentProtocol = SIXEL;
                break;
            case '4':
                currentProtocol = KITTY;
                break;
            case '5':
                currentProtocol = ITERM2;
                break;
            case 'f':
            case 'F':
                currentScaling = ImageScaling.FIT;
                break;
            case 'i':
            case 'I':
                currentScaling = ImageScaling.FILL;
                break;
            case 's':
            case 'S':
                currentScaling = ImageScaling.STRETCH;
                break;
            case 'n':
            case 'N':
                currentScaling = ImageScaling.NONE;
                break;
            case 'a':
            case 'A':
                // Auto-detect best protocol
                currentProtocol = capabilities.bestProtocol();
                break;
            default:
                break;
        }

        // Force full redraw when switching protocols
        if (previousProtocol != currentProtocol) {
            needsFullRedraw = true;
        }
    }

    private void ui(Frame frame) {
        Rect area = frame.area();

        var layout = Layout.vertical()
            .constraints(
                Constraint.length(3),   // Header
                Constraint.length(7),   // Capabilities info
                Constraint.fill(),      // Image area
                Constraint.length(4)    // Footer/help
            )
            .split(area);

        renderHeader(frame, layout.get(0));
        renderCapabilities(frame, layout.get(1));
        renderImage(frame, layout.get(2));
        renderFooter(frame, layout.get(3));
    }

    private void renderHeader(Frame frame, Rect area) {
        Block headerBlock = Block.builder()
            .borders(Borders.ALL)
            .borderType(BorderType.ROUNDED)
            .borderStyle(Style.EMPTY.fg(Color.CYAN))
            .title(Title.from(
                Line.from(
                    Span.raw(" TamboUI ").bold().cyan(),
                    Span.raw("Image Demo ").yellow()
                )
            ).centered())
            .build();

        frame.renderWidget(headerBlock, area);
    }

    private void renderCapabilities(Frame frame, Rect area) {
        TerminalImageProtocol best = capabilities.bestSupport();

        Line titleLine = Line.from(Span.raw(" Terminal Capabilities ").bold().green());

        Line detectedLine = Line.from(
            Span.raw("  Best detected: ").dim(),
            supportSpan(best)
        );

        Line supportLine = Line.from(
            Span.raw("  Supported: ").dim(),
            capabilities.supports(TerminalImageProtocol.KITTY) ? Span.raw("Kitty ").green() : Span.raw("Kitty ").dim(),
            capabilities.supports(TerminalImageProtocol.ITERM2) ? Span.raw("iTerm2 ").green() : Span.raw("iTerm2 ").dim(),
            capabilities.supports(TerminalImageProtocol.SIXEL) ? Span.raw("Sixel ").green() : Span.raw("Sixel ").dim(),
            capabilities.supports(TerminalImageProtocol.HALF_BLOCK) ? Span.raw("Half-Block ").green() : Span.raw("Half-Block ").dim(),
            capabilities.supports(TerminalImageProtocol.BRAILLE) ? Span.raw("Braille ").green() : Span.raw("Braille ").dim()
        );

        Line currentLine = Line.from(
            Span.raw("  Current protocol: ").dim(),
            Span.raw(currentProtocol.name()).bold().yellow(),
            Span.raw(" (").dim(),
            Span.raw(currentProtocol.resolution().widthMultiplier() + "x" +
                currentProtocol.resolution().heightMultiplier()).cyan(),
            Span.raw(" per cell)").dim()
        );

        Line scalingLine = Line.from(
            Span.raw("  Scaling mode: ").dim(),
            Span.raw(currentScaling.name()).bold().magenta()
        );

        Paragraph info = Paragraph.builder()
            .text(Text.from(titleLine, detectedLine, supportLine, currentLine, scalingLine))
            .block(Block.builder()
                .borders(Borders.ALL)
                .borderType(BorderType.ROUNDED)
                .borderStyle(Style.EMPTY.fg(Color.GREEN))
                .build())
            .build();

        frame.renderWidget(info, area);
    }

    private Span supportSpan(TerminalImageProtocol support) {
        switch (support) {
            case KITTY:
                return Span.raw("Kitty Graphics").bold().green();
            case ITERM2:
                return Span.raw("iTerm2 Inline Images").bold().green();
            case SIXEL:
                return Span.raw("Sixel Graphics").bold().green();
            case HALF_BLOCK:
                return Span.raw("Half-Block (Unicode)").yellow();
            case BRAILLE:
                return Span.raw("Braille (Unicode)").yellow();
            default:
                return Span.raw("None").red();
        }
    }

    private void renderImage(Frame frame, Rect area) {
        Image image = Image.builder()
            .data(imageData)
            .scaling(currentScaling)
            .protocol(currentProtocol)
            .block(Block.builder()
                .borders(Borders.ALL)
                .borderType(BorderType.ROUNDED)
                .borderStyle(Style.EMPTY.fg(Color.BLUE))
                .title(Title.from(Line.from(Span.raw(" " + imageTitle + " ").blue())))
                .build())
            .build();

        frame.renderWidget(image, area);
    }

    private void renderFooter(Frame frame, Rect area) {
        Line helpLine1 = Line.from(
            Span.raw(" Protocol: ").dim(),
            Span.raw("1").bold().yellow(),
            Span.raw(" Half-Block ").dim(),
            Span.raw("2").bold().yellow(),
            Span.raw(" Braille ").dim(),
            Span.raw("3").bold().yellow(),
            Span.raw(" Sixel ").dim(),
            Span.raw("4").bold().yellow(),
            Span.raw(" Kitty ").dim(),
            Span.raw("5").bold().yellow(),
            Span.raw(" iTerm2 ").dim(),
            Span.raw("a").bold().yellow(),
            Span.raw(" Auto").dim()
        );

        Line helpLine2 = Line.from(
            Span.raw(" Scaling: ").dim(),
            Span.raw("f").bold().yellow(),
            Span.raw(" Fit  ").dim(),
            Span.raw("i").bold().yellow(),
            Span.raw(" Fill  ").dim(),
            Span.raw("s").bold().yellow(),
            Span.raw(" Stretch  ").dim(),
            Span.raw("n").bold().yellow(),
            Span.raw(" None  ").dim(),
            Span.raw("q").bold().red(),
            Span.raw(" Quit").dim()
        );

        Paragraph footer = Paragraph.builder()
            .text(Text.from(helpLine1, helpLine2))
            .block(Block.builder()
                .borders(Borders.ALL)
                .borderType(BorderType.ROUNDED)
                .borderStyle(Style.EMPTY.fg(Color.DARK_GRAY))
                .build())
            .build();

        frame.renderWidget(footer, area);
    }
}
