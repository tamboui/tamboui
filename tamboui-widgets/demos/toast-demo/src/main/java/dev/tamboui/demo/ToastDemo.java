///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS dev.tamboui:tamboui-widgets:LATEST
//DEPS dev.tamboui:tamboui-jline3-backend:LATEST

/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.demo;

import java.time.Duration;

import dev.tamboui.layout.Constraint;
import dev.tamboui.layout.Layout;
import dev.tamboui.layout.Rect;
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
import dev.tamboui.widgets.toast.Toast;
import dev.tamboui.widgets.toast.ToastBuilder;
import dev.tamboui.widgets.toast.ToastEngine;

/**
 * Demo TUI application showcasing the Toast widget and {@link ToastEngine}.
 */
public class ToastDemo {

    private static final Duration TOAST_LIFETIME = Duration.ofSeconds(4);
    private static final Duration TICK = Duration.ofMillis(100);

    private static final String LONG_MESSAGE =
            "This is a deliberately long toast message that should wrap across multiple "
                    + "lines when the terminal width is limited, demonstrating text wrapping "
                    + "inside the toast overlay.";

    private final ToastEngine engine = ToastEngine.builder().build();
    private boolean running = true;

    private ToastDemo() {
    }

    /**
     * Demo entry point.
     *
     * @param args the CLI arguments
     * @throws Exception on unexpected error
     */
    public static void main(String[] args) throws Exception {
        new ToastDemo().run();
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

            backend.onResize(() -> terminal.draw(this::ui));

            while (running) {
                engine.tick(TICK);
                terminal.draw(this::ui);

                int c = backend.read(100);
                if (c == 'q' || c == 'Q' || c == 3) {
                    running = false;
                } else if (c != -1 && c != -2) {
                    handleKey(c);
                }
            }
        }
    }

    private void handleKey(int c) {
        switch (c) {
            case '1':
                engine.show(infoToast("Information toast", "Press keys to enqueue more toasts."));
                break;
            case '2':
                engine.show(successToast("Success toast", "Operation completed successfully."));
                break;
            case '3':
                engine.show(warningToast("Warning toast", "Something needs your attention."));
                break;
            case '4':
                engine.show(errorToast("Error toast", "Something went wrong."));
                break;
            case 's':
            case 'S':
                engine.show(ToastBuilder.info("Sticky toast")
                        .title("Pinned")
                        .message("This toast stays until dismissed.")
                        .sticky(true)
                        .build());
                break;
            case 'l':
            case 'L':
                engine.show(infoToast("Long message", LONG_MESSAGE));
                break;
            default:
                break;
        }
    }

    private static Toast infoToast(String title, String message) {
        return ToastBuilder.info(message).title(title).duration(TOAST_LIFETIME).build();
    }

    private static Toast successToast(String title, String message) {
        return ToastBuilder.success(message).title(title).duration(TOAST_LIFETIME).build();
    }

    private static Toast warningToast(String title, String message) {
        return ToastBuilder.warning(message).title(title).duration(TOAST_LIFETIME).build();
    }

    private static Toast errorToast(String title, String message) {
        return ToastBuilder.error(message).title(title).duration(TOAST_LIFETIME).build();
    }

    private void ui(Frame frame) {
        Rect area = frame.area();

        var layout = Layout.vertical()
                .constraints(
                        Constraint.length(3),
                        Constraint.fill(),
                        Constraint.length(3))
                .split(area);

        renderHeader(frame, layout.get(0));
        renderMainContent(frame, layout.get(1));
        renderFooter(frame, layout.get(2));
        engine.render(frame, area);
    }

    private void renderHeader(Frame frame, Rect area) {
        Block headerBlock = Block.builder()
                .borders(Borders.ALL)
                .borderType(BorderType.ROUNDED)
                .borderStyle(Style.EMPTY.fg(Color.CYAN))
                .title(Title.from(
                        Line.from(
                                Span.raw(" TamboUI ").bold().cyan(),
                                Span.raw("Toast Demo ").yellow()))
                        .centered())
                .build();

        frame.renderWidget(headerBlock, area);
    }

    private void renderMainContent(Frame frame, Rect area) {
        Text help = Text.from(
                Line.from(Span.raw("Keybindings").bold().cyan()),
                Line.empty(),
                Line.from(
                        Span.raw("  ").dim(),
                        Span.raw("1").bold().yellow(),
                        Span.raw("  Info toast").dim()),
                Line.from(
                        Span.raw("  ").dim(),
                        Span.raw("2").bold().yellow(),
                        Span.raw("  Success toast").dim()),
                Line.from(
                        Span.raw("  ").dim(),
                        Span.raw("3").bold().yellow(),
                        Span.raw("  Warning toast").dim()),
                Line.from(
                        Span.raw("  ").dim(),
                        Span.raw("4").bold().yellow(),
                        Span.raw("  Error toast").dim()),
                Line.from(
                        Span.raw("  ").dim(),
                        Span.raw("s").bold().yellow(),
                        Span.raw("  Sticky toast (manual dismiss)").dim()),
                Line.from(
                        Span.raw("  ").dim(),
                        Span.raw("l").bold().yellow(),
                        Span.raw("  Long message (wrap demo)").dim()),
                Line.empty(),
                Line.from(
                        Span.raw("Active toasts: ").dim(),
                        Span.raw(String.valueOf(engine.visibleCount())).bold().cyan()));

        Paragraph panel = Paragraph.builder()
                .text(help)
                .block(Block.builder()
                        .borders(Borders.ALL)
                        .borderType(BorderType.ROUNDED)
                        .borderStyle(Style.EMPTY.fg(Color.DARK_GRAY))
                        .title(Title.from(" Controls ").centered())
                        .build())
                .build();

        frame.renderWidget(panel, area);
    }

    private void renderFooter(Frame frame, Rect area) {
        Line helpLine = Line.from(
                Span.raw(" Toasts appear in the bottom-right corner. ").dim(),
                Span.raw("q").bold().yellow(),
                Span.raw(" Quit").dim());

        Paragraph footer = Paragraph.builder()
                .text(Text.from(helpLine))
                .block(Block.builder()
                        .borders(Borders.ALL)
                        .borderType(BorderType.ROUNDED)
                        .borderStyle(Style.EMPTY.fg(Color.DARK_GRAY))
                        .build())
                .build();

        frame.renderWidget(footer, area);
    }
}
