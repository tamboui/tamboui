//DEPS dev.tamboui:tamboui-tfx:LATEST
//DEPS dev.tamboui:tamboui-tfx-toolkit:LATEST
//DEPS dev.tamboui:tamboui-toolkit:LATEST
//DEPS dev.tamboui:tamboui-jline3-backend:LATEST
/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.demo;

import dev.tamboui.style.Color;
import dev.tamboui.style.Style;
import dev.tamboui.tfx.Fx;
import dev.tamboui.tfx.Interpolation;
import dev.tamboui.tfx.toolkit.ToolkitEffects;
import dev.tamboui.toolkit.app.ToolkitRunner;
import dev.tamboui.toolkit.element.Element;
import dev.tamboui.toolkit.event.EventResult;
import dev.tamboui.tui.TuiConfig;

import java.time.Duration;

import static dev.tamboui.toolkit.Toolkit.*;

/**
 * Demo showcasing TFX effects integration with the Toolkit DSL.
 * <p>
 * This demo shows how to use ToolkitEffects to apply visual effects
 * to specific elements by ID or CSS selectors.
 * <p>
 * Controls:
 * - 1: Apply effect to header panel (by ID)
 * - 2: Apply effect to content panel (by ID)
 * - 3: Apply effect to footer panel (by ID)
 * - 4: Apply global effect
 * - 5: Apply effect to all panels (by CSS selector ".main-panel")
 * - 6: Apply effect to all Panel types (by type selector "Panel")
 * - 7: Apply effect to .highlight spans inside #looping-panel (descendant selector)
 * - Space: Clear all effects
 * - q/ESC: Quit
 * <p>
 * The looping-panel also demonstrates automatic :focus pseudo-class effects -
 * when the panel is focused (Tab to it), the .focus-demo text animates automatically.
 */
public class TfxToolkitDemo {

    private static final Color HEADER_COLOR = Color.rgb(0x00, 0xd9, 0xff);
    private static final Color CONTENT_COLOR = Color.rgb(0x00, 0xff, 0x88);
    private static final Color FOOTER_COLOR = Color.rgb(0xff, 0xd3, 0x00);
    private static final Color ACCENT_COLOR = Color.rgb(0xff, 0x00, 0x80);

    private final ToolkitEffects effects = new ToolkitEffects();
    private String statusMessage = "Press 1-7 to trigger effects, Space to clear";

    public static void main(String[] args) throws Exception {
        new TfxToolkitDemo().run();
    }

    private void run() throws Exception {
        TuiConfig config = TuiConfig.builder()
                .tickRate(Duration.ofMillis(16))
                .build();

        try (ToolkitRunner r = ToolkitRunner.builder()
                .config(config)
                .postRenderProcessor(effects.asPostRenderProcessor())
                .build()) {

            // Apply different looping effects to highlighted words using CSS class selectors
            effects.addEffectBySelector(".looping",
                    Fx.fadeTo(Color.WHITE, Color.rgb(0xff, 0x00, 0x80), 1000, Interpolation.SineInOut).pingPong());
            effects.addEffectBySelector(".fx",
                    Fx.fadeTo(Color.WHITE, Color.rgb(0x00, 0xd9, 0xff), 1200, Interpolation.SineInOut).pingPong());
            effects.addEffectBySelector(".different",
                    Fx.fadeTo(Color.WHITE, Color.rgb(0x00, 0xff, 0x88), 1400, Interpolation.SineInOut).pingPong());

            // Focus pseudo-class effect - only active when #looping-panel is focused
            effects.addEffectBySelector("#looping-panel:focus .focus-demo",
                    Fx.fadeTo(Color.GRAY, Color.rgb(0xff, 0x00, 0xff), 400, Interpolation.SineInOut).pingPong());

            r.run(this::buildUI);
        }
    }

    private Element buildUI() {
        return column(
                // Header panel - has .main-panel class
                panel("Header",
                        column(
                                text("TFX Toolkit Integration Demo").bold().cyan(),
                                text("Effects target by ID or CSS selector").dim()
                        )
                )
                        .id("header")
                        .addClass("main-panel")
                        .rounded()
                        .borderColor(HEADER_COLOR)
                        .length(5)
                        .focusable()
                        .onKeyEvent(this::handleKeyEvent),

                // Main content row with content panel and looping panel
                row(
                        // Content panel - has .main-panel class
                        panel("Content",
                                column(
                                        text("Press 1-7 to apply effects:").fg(Color.WHITE),
                                        text("  1-3: By ID (header/content/footer)").fg(Color.GRAY),
                                        text("  4: Global effect").fg(Color.GRAY),
                                        text("  5: By class (.main-panel)").fg(Color.GRAY),
                                        text("  6: By type (Panel)").fg(Color.GRAY),
                                        text("  7: Descendant (#looping-panel .highlight)").fg(Color.GRAY),
                                        text(""),
                                        text("Status: " + statusMessage).yellow()
                                )
                        ).id("content").addClass("main-panel").rounded().borderColor(CONTENT_COLOR).fill(),

                        // Looping effects panel - uses markupTextArea with custom style resolver
                        markupTextArea("""

                                [gray]Words with [/gray][looping]looping[/looping][gray] colors[/gray]
                                [gray]and smooth [/gray][fx]effects[/fx]
                                [gray]on [/gray][different]different[/different][gray] words.[/gray]

                                [gray]Press 7 to highlight:[/gray]
                                [highlight]This text is targeted[/highlight]
                                [gray]by descendant selector.[/gray]

                                [gray]Focus this panel (Tab) to see:[/gray]
                                [focus-demo]Auto-animates when focused[/focus-demo]
                                """)
                                .customResolver(this::resolveEffectStyle)
                                .title("Looping (Tab to focus)")
                                .rounded()
                                .borderColor(ACCENT_COLOR)
                                .focusedBorderColor(Color.LIGHT_YELLOW)
                                .id("looping-panel")
                                .focusable()
                                .wrapWord()
                                .length(30)
                ).fill(),

                // Footer panel - has .main-panel class
                panel("Footer",
                        row(
                                text("Effects running: " + (effects.isRunning() ? "yes" : "no")).fg(Color.GRAY),
                                spacer(),
                                text("Space").bold().fg(Color.WHITE),
                                text(" Clear  ").fg(Color.GRAY),
                                text("q/ESC").bold().fg(Color.WHITE),
                                text(" Quit").fg(Color.GRAY)
                        )
                ).id("footer").addClass("main-panel").rounded().borderColor(FOOTER_COLOR).length(3)
        );
    }

    private Style resolveEffectStyle(String tagName) {
        return switch (tagName) {
            case "looping", "fx", "different" -> Style.EMPTY.fg(Color.WHITE);
            case "highlight", "focus-demo" -> Style.EMPTY.fg(Color.GRAY);
            default -> null;
        };
    }

    private EventResult handleKeyEvent(dev.tamboui.tui.event.KeyEvent event) {
        if (event.isChar('1')) {
            effects.addEffect("header", Fx.fadeToFg(HEADER_COLOR, 500, Interpolation.SineInOut));
            statusMessage = "Applied fade effect to header";
            return EventResult.HANDLED;
        }

        if (event.isChar('2')) {
            effects.addEffect("content", Fx.fadeToFg(CONTENT_COLOR, 600, Interpolation.SineInOut));
            statusMessage = "Applied fade effect to content";
            return EventResult.HANDLED;
        }

        if (event.isChar('3')) {
            effects.addEffect("footer", Fx.fadeToFg(FOOTER_COLOR, 400, Interpolation.SineInOut));
            statusMessage = "Applied fade effect to footer";
            return EventResult.HANDLED;
        }

        if (event.isChar('4')) {
            effects.addGlobalEffect(Fx.dissolve(1500, Interpolation.QuadOut));
            statusMessage = "Applied global dissolve effect";
            return EventResult.HANDLED;
        }

        if (event.isChar('5')) {
            // CSS class selector - targets all elements with .main-panel class
            effects.addEffectBySelector(".main-panel", Fx.fadeToFg(ACCENT_COLOR, 400, Interpolation.SineInOut));
            statusMessage = "Applied effect to .main-panel (3 panels)";
            return EventResult.HANDLED;
        }

        if (event.isChar('6')) {
            // Type selector - targets all Panel elements
            effects.addEffectBySelector("Panel", Fx.fadeToFg(Color.WHITE, 300, Interpolation.QuadOut));
            statusMessage = "Applied effect to all Panel types (4 panels)";
            return EventResult.HANDLED;
        }

        if (event.isChar('7')) {
            // Descendant selector - targets .highlight spans inside #looping-panel only
            effects.addEffectBySelector("#looping-panel .highlight",
                    Fx.fadeTo(Color.GRAY, Color.rgb(0x00, 0xff, 0x00), 600, Interpolation.SineInOut).pingPong());
            statusMessage = "Applied effect to #looping-panel .highlight (descendant)";
            return EventResult.HANDLED;
        }

        if (event.isChar(' ')) {
            effects.clear();
            // Re-add the looping word effects after clearing (using CSS class selectors)
            effects.addEffectBySelector(".looping",
                    Fx.fadeTo(Color.WHITE, Color.rgb(0xff, 0x00, 0x80), 1000, Interpolation.SineInOut).pingPong());
            effects.addEffectBySelector(".fx",
                    Fx.fadeTo(Color.WHITE, Color.rgb(0x00, 0xd9, 0xff), 1200, Interpolation.SineInOut).pingPong());
            effects.addEffectBySelector(".different",
                    Fx.fadeTo(Color.WHITE, Color.rgb(0x00, 0xff, 0x88), 1400, Interpolation.SineInOut).pingPong());
            // Re-add focus-based effect
            effects.addEffectBySelector("#looping-panel:focus .focus-demo",
                    Fx.fadeTo(Color.GRAY, Color.rgb(0xff, 0x00, 0xff), 400, Interpolation.SineInOut).pingPong());
            statusMessage = "Effects cleared";
            return EventResult.HANDLED;
        }

        return EventResult.UNHANDLED;
    }
}
