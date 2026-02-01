//DEPS dev.tamboui:tamboui-tfx-tui:LATEST
//DEPS dev.tamboui:tamboui-jline3-backend:LATEST
/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.demo;

import dev.tamboui.layout.Constraint;
import dev.tamboui.layout.Layout;
import dev.tamboui.layout.Margin;
import dev.tamboui.layout.Rect;
import dev.tamboui.style.Color;
import dev.tamboui.style.Style;
import dev.tamboui.terminal.Frame;
import dev.tamboui.text.Line;
import dev.tamboui.text.Span;
import dev.tamboui.text.Text;
import dev.tamboui.tfx.Fx;
import dev.tamboui.tfx.Interpolation;
import dev.tamboui.tfx.Motion;
import dev.tamboui.tfx.tui.TfxIntegration;
import dev.tamboui.tui.TuiConfig;
import dev.tamboui.tui.TuiRunner;
import dev.tamboui.tui.event.KeyEvent;
import dev.tamboui.widgets.block.Block;
import dev.tamboui.widgets.block.BorderType;
import dev.tamboui.widgets.block.Borders;
import dev.tamboui.widgets.paragraph.Paragraph;

import java.time.Duration;
import java.util.List;

/**
 * Demo showcasing TFX integration with TuiRunner.
 * <p>
 * This demo shows how to use TfxIntegration to easily add effects to TUI applications.
 * <p>
 * Controls:
 * - 1-4: Trigger different effects
 * - Space: Clear all effects
 * - ESC/q: Quit
 */
public class TfxTuiDemo {

    private static final Color BG = Color.rgb(0x1a, 0x1a, 0x2e);
    private static final Color PANEL_BG = Color.rgb(0x16, 0x21, 0x3e);
    private static final Color CYAN = Color.rgb(0x00, 0xd9, 0xff);
    private static final Color MAGENTA = Color.rgb(0xff, 0x00, 0x80);
    private static final Color YELLOW = Color.rgb(0xff, 0xd3, 0x00);
    private static final Color GREEN = Color.rgb(0x00, 0xff, 0x88);

    private TfxIntegration tfx;
    private String statusMessage = "Press 1-4 to trigger effects, Space to clear";

    private TfxTuiDemo() {

    }

    /**
     * Demo entry point.
     * @param args the CLI arguments
     * @throws Exception on unexpected error
     */
    public static void main(String[] args) throws Exception {
        new TfxTuiDemo().run();
    }

    private void run() throws Exception {
        tfx = new TfxIntegration();

        // Add initial sweep-in effect (one-shot, completes)
        tfx.addEffect(Fx.sweepIn(Motion.LEFT_TO_RIGHT, 20, 0, BG, 800, Interpolation.QuadOut));

        TuiConfig config = TuiConfig.builder()
            .tickRate(Duration.ofMillis(16))
            .build();

        try (TuiRunner tui = TuiRunner.create(config)) {
            tui.run(
                tfx.wrapHandler((event, runner) -> handleEvent(event, runner)),
                tfx.wrapRenderer(this::render)
            );
        }
    }

    private boolean handleEvent(dev.tamboui.tui.event.Event event, TuiRunner runner) {
        if (event instanceof KeyEvent) {
            KeyEvent keyEvent = (KeyEvent) event;

            if (keyEvent.isQuit() || keyEvent.isCancel()) {
                runner.quit();
                return false;
            }

            if (keyEvent.isChar('1')) {
                // Sweep in from left - one-shot effect
                tfx.clearEffects();
                tfx.addEffect(Fx.sweepIn(Motion.LEFT_TO_RIGHT, 30, 5, BG, 1000, Interpolation.QuadOut));
                statusMessage = "Sweep in from left";
                return true;
            }

            if (keyEvent.isChar('2')) {
                // Sweep in from top - reveals content
                tfx.clearEffects();
                tfx.addEffect(Fx.sweepIn(Motion.UP_TO_DOWN, 20, 0, BG, 800, Interpolation.QuadOut));
                statusMessage = "Sweep in from top";
                return true;
            }

            if (keyEvent.isChar('3')) {
                // Dissolve and coalesce back - sequence
                tfx.clearEffects();
                tfx.addEffect(Fx.sequence(
                    Fx.dissolve(800, Interpolation.QuadOut),
                    Fx.coalesce(800, Interpolation.QuadIn)
                ));
                statusMessage = "Dissolve and coalesce";
                return true;
            }

            if (keyEvent.isChar('4')) {
                // Color flash effect - fade to color and back
                tfx.clearEffects();
                tfx.addEffect(Fx.sequence(
                    Fx.fadeToFg(CYAN, 400, Interpolation.QuadOut),
                    Fx.fadeToFg(Color.WHITE, 400, Interpolation.QuadIn)
                ));
                statusMessage = "Color flash (cyan)";
                return true;
            }

            if (keyEvent.isChar(' ')) {
                tfx.clearEffects();
                statusMessage = "Effects cleared";
                return true;
            }
        }

        return false;
    }

    private void render(Frame frame) {
        Rect area = frame.area();

        // Clear background
        frame.buffer().fill(area, new dev.tamboui.buffer.Cell(" ", Style.EMPTY.bg(BG)));

        // Create centered content area
        int contentWidth = Math.min(70, area.width() - 4);
        int contentHeight = Math.min(20, area.height() - 4);
        int contentX = (area.width() - contentWidth) / 2;
        int contentY = (area.height() - contentHeight) / 2;

        Rect contentArea = new Rect(
            area.left() + contentX,
            area.top() + contentY,
            contentWidth,
            contentHeight
        );

        // Main panel
        Block panel = Block.builder()
            .title(" TFX TUI Integration Demo ")
            .borders(Borders.ALL)
            .borderType(BorderType.ROUNDED)
            .style(Style.EMPTY.bg(PANEL_BG).fg(CYAN))
            .build();

        frame.renderWidget(panel, contentArea);
        Rect inner = panel.inner(contentArea);

        // Layout
        Layout layout = Layout.vertical()
            .constraints(
                Constraint.length(3),  // Title
                Constraint.fill(1),    // Content
                Constraint.length(4)   // Controls
            );

        List<Rect> splits = layout.split(inner.inner(Margin.uniform(1)));

        // Title
        Text title = Text.from(
            Line.from(Span.styled("TfxIntegration Demo", Style.EMPTY.fg(YELLOW).bold()))
        );
        frame.renderWidget(Paragraph.builder().text(title).build(), splits.get(0));

        // Content
        Text content = Text.from(
            Line.from(""),
            Line.from(Span.styled("This demo shows TfxIntegration usage:", Style.EMPTY.fg(Color.WHITE))),
            Line.from(Span.styled("- Wraps EventHandler and Renderer", Style.EMPTY.fg(Color.GRAY))),
            Line.from(Span.styled("- Automatic timing from TickEvents", Style.EMPTY.fg(Color.GRAY))),
            Line.from(Span.styled("- Forces redraws when effects are active", Style.EMPTY.fg(Color.GRAY))),
            Line.from(""),
            Line.from(Span.styled("Status: ", Style.EMPTY.fg(Color.WHITE)),
                      Span.styled(statusMessage, Style.EMPTY.fg(GREEN))),
            Line.from(Span.styled("Effects running: " + tfx.effectCount(), Style.EMPTY.fg(Color.GRAY)))
        );
        frame.renderWidget(Paragraph.builder().text(content).build(), splits.get(1));

        // Controls
        Text controls = Text.from(
            Line.from(Span.styled("1", Style.EMPTY.fg(CYAN).bold()), Span.styled(" Sweep  ", Style.EMPTY.fg(Color.GRAY)),
                      Span.styled("2", Style.EMPTY.fg(MAGENTA).bold()), Span.styled(" Slide  ", Style.EMPTY.fg(Color.GRAY)),
                      Span.styled("3", Style.EMPTY.fg(YELLOW).bold()), Span.styled(" Dissolve  ", Style.EMPTY.fg(Color.GRAY)),
                      Span.styled("4", Style.EMPTY.fg(GREEN).bold()), Span.styled(" Flash", Style.EMPTY.fg(Color.GRAY))),
            Line.from(Span.styled("Space", Style.EMPTY.fg(Color.WHITE).bold()), Span.styled(" Clear  ", Style.EMPTY.fg(Color.GRAY)),
                      Span.styled("q/ESC", Style.EMPTY.fg(Color.WHITE).bold()), Span.styled(" Quit", Style.EMPTY.fg(Color.GRAY)))
        );
        frame.renderWidget(Paragraph.builder().text(controls).build(), splits.get(2));
    }
}
