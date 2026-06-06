///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS dev.tamboui:tamboui-image:LATEST
//DEPS dev.tamboui:tamboui-jline3-backend:LATEST
// Prevents macOS dock icon
//JAVA_OPTIONS -Dapple.awt.UIElement=true

/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.demo;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicLong;

import dev.tamboui.image.Image;
import dev.tamboui.image.ImageData;
import dev.tamboui.image.ImageScaling;
import dev.tamboui.image.capability.TerminalImageCapabilities;
import dev.tamboui.image.protocol.ImageProtocol;
import dev.tamboui.image.protocol.ITermProtocol;
import dev.tamboui.image.protocol.KittyProtocol;
import dev.tamboui.image.protocol.SixelProtocol;
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

/**
 * Manual stress test for verifying image memory leak fixes.
 * <p>
 * Run this in iTerm2, Kitty, Ghostty, or WezTerm and monitor the terminal process
 * memory (e.g. Activity Monitor on macOS, {@code ps -o rss} on Linux).
 *
 * <h2>What to watch for</h2>
 * <ul>
 *   <li><b>Before fix:</b> terminal RSS grows continuously (~MB/s), eventually freezing
 *       the terminal or exhausting system memory.</li>
 *   <li><b>After fix:</b> terminal RSS stabilises within a few seconds and stays flat
 *       regardless of how long the demo runs.</li>
 * </ul>
 *
 * <h2>Modes</h2>
 * <ul>
 *   <li><b>Same image (default)</b> — re-renders the same image every frame. With the fix,
 *       the skip logic avoids re-transmission entirely.</li>
 *   <li><b>Changing image (press 'c')</b> — generates a new random-colour image every frame,
 *       forcing re-transmission. Memory should still be bounded because the protocol reuses
 *       stable image ids (Kitty) or the terminal replaces the previous inline image.</li>
 * </ul>
 *
 * <h2>Usage</h2>
 * <pre>
 *   ./run-demo.sh image-stress-test
 * </pre>
 */
public class ImageStressTest {

    private enum Mode { SAME, CHANGING, HIDDEN }

    private boolean running = true;
    private Mode mode = Mode.SAME;
    private ImageScaling scaling = ImageScaling.FIT;
    private ImageProtocol protocol;
    private final ImageData staticImage;
    private final AtomicLong frameCount = new AtomicLong();
    private long startTimeMs;
    private String terminalPid = "?";

    /**
     * Entry point.
     *
     * @param args CLI arguments (unused)
     * @throws Exception on unexpected error
     */
    public static void main(String[] args) throws Exception {
        new ImageStressTest().run();
    }

    private ImageStressTest() {
        // Generate a 1024×768 test image — large enough to be meaningful for the leak
        this.staticImage = generateGradientImage(1024, 768);
        this.protocol = TerminalImageCapabilities.detect().bestProtocol();
    }

    private void run() throws Exception {
        startTimeMs = System.currentTimeMillis();
        terminalPid = detectTerminalPid();

        try (var backend = BackendFactory.create()) {
            backend.enableRawMode();
            backend.enterAlternateScreen();
            backend.hideCursor();

            var terminal = new Terminal<>(backend);

            while (running) {
                terminal.draw(this::ui);
                frameCount.incrementAndGet();
                var c = backend.read(16); // ~60 fps
                handleInput(c);
            }
        }
    }

    private void handleInput(int c) {
        switch (c) {
            case 'q': case 'Q': case 3:
                running = false;
                break;
            case 'c': case 'C':
                switch (mode) {
                    case SAME: mode = Mode.CHANGING; break;
                    case CHANGING: mode = Mode.HIDDEN; break;
                    case HIDDEN: mode = Mode.SAME; break;
                }
                break;
            case 'f':
                scaling = ImageScaling.FIT;
                break;
            case 'i':
                scaling = ImageScaling.FILL;
                break;
            case 's':
                scaling = ImageScaling.STRETCH;
                break;
            case '1':
                protocol = new KittyProtocol();
                break;
            case '2':
                protocol = new ITermProtocol();
                break;
            case '3':
                protocol = new SixelProtocol();
                break;
            case 'a':
                protocol = TerminalImageCapabilities.detect().bestProtocol();
                break;
            default:
                break;
        }
    }

    private void ui(Frame frame) {
        var area = frame.area();
        var rows = Layout.vertical()
            .constraints(
                Constraint.length(6),  // Stats
                Constraint.fill(),     // Image
                Constraint.length(3)   // Help
            )
            .split(area);

        renderStats(frame, rows.get(0));
        renderImage(frame, rows.get(1));
        renderHelp(frame, rows.get(2));
    }

    private void renderStats(Frame frame, Rect area) {
        long frames = frameCount.get();
        long elapsedSec = Math.max(1, (System.currentTimeMillis() - startTimeMs) / 1000);
        long fps = frames / elapsedSec;
        long jvmMb = Runtime.getRuntime().totalMemory() / (1024 * 1024);
        long jvmUsedMb = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / (1024 * 1024);
        String termRss = readTerminalRss();

        var lines = Text.from(
            Line.from(
                Span.raw("  Mode: ").dim(),
                mode == Mode.SAME ? Span.raw("SAME IMAGE (skip expected)").green().bold()
                    : mode == Mode.CHANGING ? Span.raw("CHANGING IMAGE (re-transmit every frame)").red().bold()
                    : Span.raw("HIDDEN (no image rendered)").yellow().bold()
            ),
            Line.from(
                Span.raw("  Protocol: ").dim(),
                Span.raw(protocol.name()).yellow().bold(),
                Span.raw("  Scaling: ").dim(),
                Span.raw(scaling.name()).magenta().bold()
            ),
            Line.from(
                Span.raw("  Frames: ").dim(),
                Span.raw(String.valueOf(frames)).cyan(),
                Span.raw("  FPS: ").dim(),
                Span.raw(String.valueOf(fps)).cyan(),
                Span.raw("  Elapsed: ").dim(),
                Span.raw(elapsedSec + "s").cyan()
            ),
            Line.from(
                Span.raw("  JVM heap: ").dim(),
                Span.raw(jvmUsedMb + "/" + jvmMb + " MB").cyan(),
                Span.raw("  Terminal RSS: ").dim(),
                Span.raw(termRss).yellow().bold()
            )
        );

        var stats = Paragraph.builder()
            .text(lines)
            .block(Block.builder()
                .borders(Borders.ALL)
                .borderType(BorderType.ROUNDED)
                .borderStyle(Style.EMPTY.fg(Color.CYAN))
                .title(Title.from(Line.from(
                    Span.raw(" Image Memory Stress Test ").bold().cyan()
                )).centered())
                .build())
            .build();

        frame.renderWidget(stats, area);
    }

    private void renderImage(Frame frame, Rect area) {
        if (mode == Mode.HIDDEN) {
            // Render just the block with a label — no image widget, so the terminal's
            // raw-output cleanup should remove any previously shown image.
            var placeholder = Paragraph.builder()
                .text(Text.from(Line.empty(), Line.from(Span.raw("Image hidden — press c to cycle back").dim())))
                .centered()
                .block(Block.builder()
                    .borders(Borders.ALL)
                    .borderType(BorderType.ROUNDED)
                    .borderStyle(Style.EMPTY.fg(Color.DARK_GRAY))
                    .title(Title.from(Line.from(Span.raw(" hidden ").dim())))
                    .build())
                .build();
            frame.renderWidget(placeholder, area);
            return;
        }

        var imageData = (mode == Mode.CHANGING)
            ? generateRandomImage(256, 256)
            : staticImage;

        var image = Image.builder()
            .data(imageData)
            .scaling(scaling)
            .protocol(protocol)
            .block(Block.builder()
                .borders(Borders.ALL)
                .borderType(BorderType.ROUNDED)
                .borderStyle(Style.EMPTY.fg(Color.BLUE))
                .title(Title.from(Line.from(Span.raw(
                    " " + imageData.width() + "×" + imageData.height() + " "
                ).blue())))
                .build())
            .build();

        frame.renderWidget(image, area);
    }

    private void renderHelp(Frame frame, Rect area) {
        var help = Paragraph.builder()
            .text(Text.from(Line.from(
                Span.raw(" c").bold().yellow(), Span.raw(" cycle mode  ").dim(),
                Span.raw("1").bold().yellow(), Span.raw(" Kitty  ").dim(),
                Span.raw("2").bold().yellow(), Span.raw(" iTerm2  ").dim(),
                Span.raw("3").bold().yellow(), Span.raw(" Sixel  ").dim(),
                Span.raw("a").bold().yellow(), Span.raw(" auto  ").dim(),
                Span.raw("f").bold().yellow(), Span.raw("/").dim(),
                Span.raw("i").bold().yellow(), Span.raw("/").dim(),
                Span.raw("s").bold().yellow(), Span.raw(" fit/fill/stretch  ").dim(),
                Span.raw("q").bold().red(), Span.raw(" quit").dim()
            )))
            .block(Block.builder()
                .borders(Borders.ALL)
                .borderType(BorderType.ROUNDED)
                .borderStyle(Style.EMPTY.fg(Color.DARK_GRAY))
                .build())
            .build();

        frame.renderWidget(help, area);
    }

    // ---- helpers ----

    private static ImageData generateGradientImage(int w, int h) {
        var img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int r = (x * 255) / w;
                int g = (y * 255) / h;
                int b = 128;
                img.setRGB(x, y, 0xFF000000 | (r << 16) | (g << 8) | b);
            }
        }
        return ImageData.fromBufferedImage(img);
    }

    private static ImageData generateRandomImage(int w, int h) {
        var img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        long seed = System.nanoTime();
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                // Fast xorshift pseudo-random — not cryptographic, just visual noise
                seed ^= (seed << 13);
                seed ^= (seed >> 7);
                seed ^= (seed << 17);
                img.setRGB(x, y, 0xFF000000 | (int) (seed & 0xFFFFFF));
            }
        }
        return ImageData.fromBufferedImage(img);
    }

    private static String detectTerminalPid() {
        // Walk the process tree to find the terminal emulator
        try {
            var handle = ProcessHandle.current();
            while (handle.parent().isPresent()) {
                handle = handle.parent().get();
                var cmd = handle.info().command().orElse("");
                if (cmd.contains("iTerm") || cmd.contains("kitty") || cmd.contains("ghostty")
                        || cmd.contains("wezterm") || cmd.contains("alacritty")
                        || cmd.contains("Terminal")) {
                    return String.valueOf(handle.pid());
                }
            }
        } catch (Exception ignored) {
            // Process tree walking not supported
        }
        return "?";
    }

    private String readTerminalRss() {
        if ("?".equals(terminalPid)) {
            return "N/A (couldn't find terminal PID)";
        }
        try {
            var proc = new ProcessBuilder("ps", "-o", "rss=", "-p", terminalPid)
                .redirectErrorStream(true)
                .start();
            var output = new String(proc.getInputStream().readAllBytes()).trim();
            proc.waitFor();
            if (output.isEmpty()) {
                return "N/A";
            }
            long kb = Long.parseLong(output);
            if (kb > 1024 * 1024) {
                return String.format("%.1f GB", kb / (1024.0 * 1024.0));
            } else if (kb > 1024) {
                return String.format("%.1f MB", kb / 1024.0);
            }
            return kb + " KB";
        } catch (IOException | InterruptedException | NumberFormatException e) {
            return "N/A";
        }
    }
}
