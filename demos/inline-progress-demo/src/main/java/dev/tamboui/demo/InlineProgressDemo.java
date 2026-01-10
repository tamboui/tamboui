/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.demo;

import dev.tamboui.buffer.Buffer;
import dev.tamboui.layout.Constraint;
import dev.tamboui.layout.Layout;
import dev.tamboui.layout.Rect;
import dev.tamboui.style.Color;
import dev.tamboui.style.Modifier;
import dev.tamboui.style.Style;
import dev.tamboui.text.Line;
import dev.tamboui.text.Span;
import dev.tamboui.text.Text;
import dev.tamboui.inline.InlineDisplay;
import dev.tamboui.terminal.Backend;
import dev.tamboui.terminal.BackendFactory;
import dev.tamboui.widgets.gauge.Gauge;
import dev.tamboui.widgets.paragraph.Paragraph;

import java.util.Arrays;
import java.util.Scanner;

/**
 * Demonstrates the InlineDisplay class for NPM/Gradle-style progress UX.
 * <p>
 * This demo shows:
 * <ul>
 *   <li>Basic inline display with widgets</li>
 *   <li>Release and continue pattern (multiple phases)</li>
 *   <li>println() scrolling above status area</li>
 *   <li>clearOnClose() for transient displays</li>
 * </ul>
 */
public class InlineProgressDemo {

    private static final String[] SPINNER = {"⠋", "⠙", "⠹", "⠸", "⠼", "⠴", "⠦", "⠧", "⠇", "⠏"};

    public static void main(String[] args) throws Exception {
        var scanner = new Scanner(System.in);
        boolean restart;
        do {
            runDemo(scanner);
            restart = promptRestart();
        } while (restart);
    }

    private static void runDemo(Scanner scanner) throws Exception {
        System.out.println("=== Inline Progress Demo ===\n");

        // Part 1: Simple toAnsiString() usage
        demoToAnsiString();

        System.out.println();

        // Part 2: NPM-style package installation
        demoNpmInstall();

        System.out.println();

        // Prompt user to continue
        if (!promptContinue(scanner)) {
            System.out.println("Demo cancelled.");
            return;
        }

        System.out.println();

        // Part 3: Multi-phase with release and continue
        demoMultiPhase();

        System.out.println("\n=== Demo Complete ===");
    }

    /**
     * Demonstrates Buffer.toAnsiString() for one-shot rendering.
     */
    private static void demoToAnsiString() throws InterruptedException {
        System.out.println("--- Part 1: Buffer.toAnsiString() ---\n");

        // Simple styled text
        var buf = Buffer.empty(Rect.of(50, 1));
        Paragraph.builder()
            .text(Text.from(Line.from(
                Span.styled("Success: ", Style.EMPTY.fg(Color.GREEN).addModifier(Modifier.BOLD)),
                Span.raw("Operation completed successfully"))))
            .build()
            .render(buf.area(), buf);
        System.out.println(buf.toAnsiStringTrimmed());

        // Progress bar with carriage return for updates
        var prefix = "Progress: ";
        System.out.print(prefix);
        buf = Buffer.empty(Rect.of(40, 1));
        for (var i = 0; i <= 100; i += 5) {
            buf.clear();
            Gauge.builder()
                .ratio(i / 100.0)
                .label(i + "%")
                .gaugeStyle(Style.EMPTY.fg(Color.CYAN))
                .build()
                .render(buf.area(), buf);
            System.out.print("\r" + prefix + buf.toAnsiString());
            System.out.flush();
            Thread.sleep(50);
        }

        // Replace progress bar with completion message
        buf.clear();
        Paragraph.builder()
            .text(Text.from(Line.from(
                Span.styled("Done!", Style.EMPTY.fg(Color.GREEN).addModifier(Modifier.BOLD)))))
            .build()
            .render(buf.area(), buf);
        System.out.print("\r" + prefix + buf.toAnsiString());
        System.out.println();
    }

    /**
     * Demonstrates NPM-style package installation with InlineDisplay.
     */
    private static void demoNpmInstall() throws Exception {
        System.out.println("--- Part 2: NPM-style Install ---\n");
        System.out.println("npm install\n");

        var packages = Arrays.asList(
            new Package("lodash", "4.17.21", 150),
            new Package("express", "4.18.2", 280),
            new Package("typescript", "5.3.3", 420),
            new Package("react", "18.2.0", 310),
            new Package("webpack", "5.89.0", 890)
        );

        var totalSize = 0;
        for (var pkg : packages) {
            totalSize += pkg.sizeKb;
        }

        try (var display = InlineDisplay.create(4)) {
            var installed = 0;
            var downloadedSize = 0;

            for (var pkg : packages) {
                // Simulate download with progress
                for (var progress = 0; progress <= 100; progress += 10) {
                    var currentDownloaded = downloadedSize + (pkg.sizeKb * progress / 100);
                    var spinnerIdx = (int) (System.currentTimeMillis() / 100) % SPINNER.length;

                    final var finalInstalled = installed;
                    final var finalTotalSize = totalSize;
                    final var finalProgress = progress;

                    display.render((area, buf) -> {
                        renderNpmStatus(area, buf, pkg, spinnerIdx, finalInstalled,
                                        packages.size(), currentDownloaded, finalTotalSize, finalProgress);
                    });

                    Thread.sleep(30);
                }

                downloadedSize += pkg.sizeKb;
                installed++;

                // Log package installation above status area
                display.println(Text.from(Line.from(
                    Span.styled("+ ", Style.EMPTY.fg(Color.GREEN)),
                    Span.raw(pkg.name),
                    Span.styled("@" + pkg.version, Style.EMPTY.fg(Color.GRAY))
                )));
            }

            // Final summary
            display.render((area, buf) -> {
                Paragraph.builder()
                    .text(Text.from(Line.from(
                        Span.styled("added ", Style.EMPTY),
                        Span.styled(String.valueOf(packages.size()), Style.EMPTY.fg(Color.GREEN)),
                        Span.raw(" packages in 2.3s"))))
                    .build()
                    .render(area, buf);
            });

            Thread.sleep(500);
        }
    }

    private static void renderNpmStatus(Rect area,
                                        Buffer buf,
                                        Package pkg,
                                        int spinnerIdx,
                                        int installed,
                                        int total,
                                        int downloadedKb,
                                        int totalKb,
                                        int pkgProgress) {
        var rows = Layout.vertical()
            .constraints(
                Constraint.length(1),
                Constraint.length(1),
                Constraint.length(1),
                Constraint.length(1))
            .split(area);

        // Row 0: Spinner and current package
        Paragraph.builder()
            .text(Text.from(Line.from(
                Span.styled(SPINNER[spinnerIdx] + " ", Style.EMPTY.fg(Color.CYAN)),
                Span.raw("Installing "),
                Span.styled(pkg.name, Style.EMPTY.addModifier(Modifier.BOLD)))))
            .build()
            .render(rows.get(0), buf);

        // Row 1: Progress bar
        Gauge.builder()
            .ratio((double) downloadedKb / totalKb)
            .gaugeStyle(Style.EMPTY.fg(Color.GREEN))
            .build()
            .render(rows.get(1), buf);

        // Row 2: Package stats
        Paragraph.builder()
            .text(Text.from(Line.from(
                Span.styled(String.format("%d/%d", installed, total), Style.EMPTY.fg(Color.YELLOW)),
                Span.raw(" packages  "),
                Span.styled(formatSize(downloadedKb), Style.EMPTY.fg(Color.CYAN)),
                Span.raw(" / "),
                Span.raw(formatSize(totalKb)))))
            .build()
            .render(rows.get(2), buf);

        // Row 3: Timing
        Paragraph.builder()
            .text(Text.styled("timing: reify:install " + pkgProgress + "%",
                              Style.EMPTY.fg(Color.GRAY)))
            .build()
            .render(rows.get(3), buf);
    }

    /**
     * Demonstrates multi-phase installation with release and continue.
     */
    private static void demoMultiPhase() throws Exception {
        System.out.println("--- Part 3: Multi-Phase (Release & Continue) ---\n");
        System.out.println("Starting installation...\n");

        // Phase 1: Download packages
        try (var display = InlineDisplay.create(2).clearOnClose()) {
            for (var i = 0; i <= 100; i += 5) {
                final var progress = i;
                display.render((area, buf) -> {
                    var rows = Layout.vertical()
                        .constraints(Constraint.length(1), Constraint.length(1))
                        .split(area);

                    Paragraph.builder()
                        .text(Text.raw("Downloading packages..."))
                        .build()
                        .render(rows.get(0), buf);

                    Gauge.builder()
                        .ratio(progress / 100.0)
                        .label(progress + "%")
                        .build()
                        .render(rows.get(1), buf);
                });
                Thread.sleep(20);
            }
        }
        // Display released, cursor below

        printSuccess("Downloaded 5 packages");

        // Phase 2: Build native modules
        var modules = new String[]{"node-sass", "bcrypt", "sharp"};

        try (var display = InlineDisplay.create(3).clearOnClose()) {
            for (var m = 0; m < modules.length; m++) {
                display.println("Building: " + modules[m]);

                for (var i = 0; i <= 100; i += 10) {
                    final var moduleIdx = m;
                    final var progress = i;

                    display.render((area, buf) -> {
                        var rows = Layout.vertical()
                            .constraints(
                                Constraint.length(1),
                                Constraint.length(1),
                                Constraint.length(1))
                            .split(area);

                        Paragraph.builder()
                            .text(Text.raw("Compiling " + modules[moduleIdx] + "..."))
                            .build()
                            .render(rows.get(0), buf);

                        Gauge.builder()
                            .ratio(progress / 100.0)
                            .build()
                            .render(rows.get(1), buf);

                        Paragraph.builder()
                            .text(Text.styled((moduleIdx + 1) + "/" + modules.length + " modules",
                                              Style.EMPTY.fg(Color.GRAY)))
                            .build()
                            .render(rows.get(2), buf);
                    });
                    Thread.sleep(15);
                }
            }
        }

        printSuccess("Built 3 native modules");

        // Phase 3: Run postinstall scripts (transient - clears when done)
        try (var display = InlineDisplay.create(1).clearOnClose()) {
            display.setLine(0, "Running postinstall scripts...");
            Thread.sleep(300);
        }
        // Transient display cleared

        System.out.println("Installation complete!");
        System.out.println("Run 'npm start' to begin.");
    }

    private static boolean promptContinue(Scanner scanner) {
        var buf = Buffer.empty(Rect.of(60, 1));
        Paragraph.builder()
            .text(Text.from(Line.from(
                Span.styled("? ", Style.EMPTY.fg(Color.CYAN).addModifier(Modifier.BOLD)),
                Span.raw("Continue with native module build? "),
                Span.styled("[Y/n] ", Style.EMPTY.fg(Color.GRAY)))))
            .build()
            .render(buf.area(), buf);
        System.out.print(buf.toAnsiStringTrimmed());
        System.out.flush();

        var input = scanner.nextLine().trim().toLowerCase();
        return input.isEmpty() || input.startsWith("y");
    }

    private static boolean promptRestart() throws Exception {
        System.out.println();
        var buf = Buffer.empty(Rect.of(60, 1));
        Paragraph.builder()
            .text(Text.from(Line.from(
                Span.styled("? ", Style.EMPTY.fg(Color.CYAN).addModifier(Modifier.BOLD)),
                Span.raw("Press "),
                Span.styled("r", Style.EMPTY.fg(Color.YELLOW).addModifier(Modifier.BOLD)),
                Span.raw(" to restart or any other key to exit "))))
            .build()
            .render(buf.area(), buf);
        System.out.print(buf.toAnsiStringTrimmed());
        System.out.flush();

        try (Backend backend = BackendFactory.create()) {
            backend.enableRawMode();
            int key = backend.read(0);
            backend.disableRawMode();
            System.out.println();
            if (key == 'r' || key == 'R') {
                System.out.println();
                return true;
            }
            System.out.println("Goodbye!");
            return false;
        }
    }

    private static void printSuccess(String message) {
        var buf = Buffer.empty(Rect.of(message.length() + 2, 1));
        Paragraph.builder()
            .text(Text.from(Line.from(
                Span.styled("✓ ", Style.EMPTY.fg(Color.GREEN)),
                Span.raw(message))))
            .build()
            .render(buf.area(), buf);
        System.out.println(buf.toAnsiStringTrimmed());
        System.out.println();
    }

    private static String formatSize(int kb) {
        if (kb >= 1024) {
            return String.format("%.1f MB", kb / 1024.0);
        }
        return kb + " kB";
    }

    private static class Package {
        final String name;
        final String version;
        final int sizeKb;

        Package(String name, String version, int sizeKb) {
            this.name = name;
            this.version = version;
            this.sizeKb = sizeKb;
        }
    }
}
