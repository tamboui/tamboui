///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS dev.tamboui:tamboui-image:LATEST
//DEPS dev.tamboui:tamboui-jline3-backend:LATEST
//FILES c2023-tsuchinshan-atlas.jpg=../../../../resources/c2023-tsuchinshan-atlas.jpg
// Prevents OSX from showing up in the terminal when running the demo
//JAVA_OPTIONS -Dapple.awt.UIElement=true

/*
 * Copyright TamboUI Contributors
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
import dev.tamboui.internal.record.RecordingBackend;
import dev.tamboui.layout.Constraint;
import dev.tamboui.layout.Layout;
import dev.tamboui.layout.Rect;
import dev.tamboui.style.Color;
import dev.tamboui.style.Style;
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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumSet;

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
    private boolean forceProtocol;

    /**
     * Demo entry point.
     * @param args the CLI arguments
     * @throws Exception on unexpected error
     */
    public static void main(String[] args) throws Exception {
        var customImagePath = args.length > 0 ? args[0] : null;
        new ImageDemo(customImagePath).run();
    }

    private ImageDemo(String customImagePath) throws IOException {
        try (var backend = BackendFactory.create()) {
            if (backend instanceof RecordingBackend) {
                this.capabilities = TerminalImageCapabilities.withSupport(EnumSet.of(
                        TerminalImageProtocol.HALF_BLOCK,
                        TerminalImageProtocol.BRAILLE
                ));
            } else {
                this.capabilities = TerminalImageCapabilities.detect();
            }
        }
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

    /**
     * Runs the demo application.
     *
     * @throws Exception if an error occurs
     */
     public void run() throws Exception {
        try (var backend = BackendFactory.create()) {
            backend.enableRawMode();
            backend.enterAlternateScreen();
            backend.hideCursor();

            var terminal = new Terminal<>(backend);

            // Event loop
            while (running) {
                terminal.draw(this::ui);

                var c = backend.read(100);
                handleInput(c);
            }
        }
    }

    /**
     * Returns true if the current protocol is supported by the terminal.
     */
    private boolean isCurrentProtocolSupported() {
        return capabilities.supports(currentProtocol.protocolType());
    }

    private void handleInput(int c) {
        var previousProtocol = currentProtocol;

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
                currentScaling = ImageScaling.FIT;
                break;
            case 'i':
                currentScaling = ImageScaling.FILL;
                break;
            case 's':
                currentScaling = ImageScaling.STRETCH;
                break;
            case 'n':
                currentScaling = ImageScaling.NONE;
                break;
            case 'a':
            case 'A':
                // Auto-detect best protocol
                currentProtocol = capabilities.bestProtocol();
                break;
            case 6: // Ctrl+F - force protocol
                forceProtocol = true;
                break;
            default:
                break;
        }

        // Reset force flag when switching protocols
        if (previousProtocol != currentProtocol) {
            forceProtocol = false;
        }
    }

    private void ui(Frame frame) {
        var area = frame.area();

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
        var headerBlock = Block.builder()
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
        var best = capabilities.bestSupport();

        var titleLine = Line.from(Span.raw(" Terminal Capabilities ").bold().green());

        var detectedLine = Line.from(
            Span.raw("  Best detected: ").dim(),
            supportSpan(best)
        );

        var supportLine = Line.from(
            Span.raw("  Supported: ").dim(),
            capabilities.supports(TerminalImageProtocol.KITTY) ? Span.raw("Kitty ").green() : Span.raw("Kitty ").dim(),
            capabilities.supports(TerminalImageProtocol.ITERM2) ? Span.raw("iTerm2 ").green() : Span.raw("iTerm2 ").dim(),
            capabilities.supports(TerminalImageProtocol.SIXEL) ? Span.raw("Sixel ").green() : Span.raw("Sixel ").dim(),
            capabilities.supports(TerminalImageProtocol.HALF_BLOCK) ? Span.raw("Half-Block ").green() : Span.raw("Half-Block ").dim(),
            capabilities.supports(TerminalImageProtocol.BRAILLE) ? Span.raw("Braille ").green() : Span.raw("Braille ").dim()
        );

        var currentLine = Line.from(
            Span.raw("  Current protocol: ").dim(),
            Span.raw(currentProtocol.name()).bold().yellow(),
            Span.raw(" (").dim(),
            Span.raw(currentProtocol.resolution().widthMultiplier() + "x" +
                currentProtocol.resolution().heightMultiplier()).cyan(),
            Span.raw(" per cell)").dim()
        );

        var scalingLine = Line.from(
            Span.raw("  Scaling mode: ").dim(),
            Span.raw(currentScaling.name()).bold().magenta()
        );

        var info = Paragraph.builder()
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
        // Check if the current protocol is not supported and not forced
        if (!isCurrentProtocolSupported() && !forceProtocol) {
            renderUnsupportedProtocolWarning(frame, area);
            return;
        }

        var image = Image.builder()
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

    private void renderUnsupportedProtocolWarning(Frame frame, Rect area) {
        var warningLine1 = Line.from(
            Span.raw("The ").dim(),
            Span.raw(currentProtocol.name()).bold().yellow(),
            Span.raw(" protocol is not detected as supported by your terminal.").dim()
        );
        var warningLine2 = Line.from(
            Span.raw("Press ").dim(),
            Span.raw("Ctrl+F").bold().cyan(),
            Span.raw(" to force using it anyway.").dim()
        );

        var warning = Paragraph.builder()
            .text(Text.from(Line.empty(), warningLine1, Line.empty(), warningLine2))
            .centered()
            .block(Block.builder()
                .borders(Borders.ALL)
                .borderType(BorderType.ROUNDED)
                .borderStyle(Style.EMPTY.fg(Color.YELLOW))
                .title(Title.from(Line.from(Span.raw(" " + imageTitle + " ").blue())))
                .build())
            .build();

        frame.renderWidget(warning, area);
    }

    private void renderFooter(Frame frame, Rect area) {
        var helpLine1 = Line.from(
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

        var helpLine2 = Line.from(
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

        var footer = Paragraph.builder()
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
