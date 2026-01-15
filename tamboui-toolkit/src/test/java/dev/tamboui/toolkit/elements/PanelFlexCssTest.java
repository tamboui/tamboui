/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.toolkit.elements;

import dev.tamboui.buffer.Buffer;
import dev.tamboui.css.engine.StyleEngine;
import dev.tamboui.layout.Constraint;
import dev.tamboui.layout.Direction;
import dev.tamboui.layout.Flex;
import dev.tamboui.layout.Margin;
import dev.tamboui.layout.Rect;
import dev.tamboui.terminal.Frame;
import dev.tamboui.toolkit.element.DefaultRenderContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests that Panel consumes CSS flex layout properties.
 */
class PanelFlexCssTest {

    private StyleEngine styleEngine;
    private DefaultRenderContext context;
    private Buffer buffer;
    private Frame frame;

    @BeforeEach
    void setUp() throws IOException {
        styleEngine = StyleEngine.create();
        styleEngine.loadStylesheet("flex-test", "/themes/flex-test.tcss");
        styleEngine.setActiveStylesheet("flex-test");

        context = DefaultRenderContext.createEmpty();
        context.setStyleEngine(styleEngine);

        Rect area = new Rect(0, 0, 80, 24);
        buffer = Buffer.empty(area);
        frame = Frame.forTesting(buffer);
    }

    @Test
    void panel_programmaticDirection_overridesCss() {
        Panel panel = new Panel(
            new TextElement("A").length(5),
            new TextElement("B").length(5)
        );
        panel.addClass("horizontal-panel");
        panel.vertical(); // Programmatic override
        Rect area = new Rect(0, 0, 40, 10);

        panel.render(frame, area, context);

        // Children should be laid out vertically (programmatic overrides CSS)
        // Text A at row 1 (inside border)
        assertThat(buffer.get(1, 1).symbol()).isEqualTo("A");
        // In vertical layout, B should be on a different row than A
        // Find where B is rendered
        int bRow = -1;
        for (int y = 1; y < 9; y++) {
            if ("B".equals(buffer.get(1, y).symbol())) {
                bRow = y;
                break;
            }
        }
        assertThat(bRow).as("B should be found in vertical layout").isGreaterThan(1);
        assertThat(bRow).as("B should be below A (vertical layout)").isGreaterThan(1);
    }

    @Test
    void panel_cssDirection_appliesWhenNoProgrammatic() {
        Panel panel = new Panel(
            new TextElement("A").length(5),
            new TextElement("B").length(5)
        );
        panel.addClass("horizontal-panel");
        // No programmatic direction - CSS should apply
        Rect area = new Rect(0, 0, 40, 5);

        panel.render(frame, area, context);

        // Children should be laid out horizontally (from CSS)
        // Both texts should be on the same row (row 1, inside border)
        assertThat(buffer.get(1, 1).symbol()).isEqualTo("A");
        // In horizontal layout with spacing=2, B would be to the right
        // Check that B is on the same row
        boolean foundB = false;
        for (int x = 2; x < 38; x++) {
            if ("B".equals(buffer.get(x, 1).symbol())) {
                foundB = true;
                break;
            }
        }
        assertThat(foundB).as("Text B should be on same row as A (horizontal layout from CSS)").isTrue();
    }

    @Test
    void panel_cssMargin_appliesWhenNoProgrammatic() {
        Panel panel = new Panel(new TextElement("X"));
        panel.addClass("margined-panel");
        Rect area = new Rect(0, 0, 20, 10);

        panel.render(frame, area, context);

        // With margin: 2, the panel border should start at (2, 2) not (0, 0)
        // At (0,0) should be empty (no border)
        assertThat(buffer.get(0, 0).symbol()).isEqualTo(" ");
        assertThat(buffer.get(1, 1).symbol()).isEqualTo(" ");
        // Border should start at (2, 2)
        String borderChar = buffer.get(2, 2).symbol();
        assertThat(borderChar).isNotEqualTo(" ");
    }

    @Test
    void panel_programmaticMargin_overridesCss() {
        Panel panel = new Panel(new TextElement("X"));
        panel.addClass("margined-panel"); // CSS margin: 2
        panel.margin(0); // Programmatic override: no margin
        Rect area = new Rect(0, 0, 20, 10);

        panel.render(frame, area, context);

        // With margin override to 0, the panel border should start at (0, 0)
        String borderChar = buffer.get(0, 0).symbol();
        assertThat(borderChar).isNotEqualTo(" ");
    }

    @Test
    void panel_programmaticFlex_overridesCss() {
        Panel panel = new Panel(
            new TextElement("A").length(2),
            new TextElement("B").length(2)
        );
        panel.addClass("centered-panel"); // CSS flex: center
        panel.flex(Flex.START); // Programmatic override
        Rect area = new Rect(0, 0, 20, 10);

        panel.render(frame, area, context);

        // With Flex.START, content should be at top
        // Text A should be at row 1 (just below top border)
        assertThat(buffer.get(1, 1).symbol()).isEqualTo("A");
    }

    @Test
    void panel_programmaticSpacing_overridesCss() {
        Panel panel = new Panel(
            new TextElement("A").length(2),
            new TextElement("B").length(2)
        );
        panel.addClass("horizontal-panel"); // CSS spacing: 2
        panel.horizontal();
        panel.spacing(0); // Programmatic override
        Rect area = new Rect(0, 0, 20, 5);

        panel.render(frame, area, context);

        // With spacing=0, B should be close to A
        // Both A and B should be on the same row (row 1, inside border)
        assertThat(buffer.get(1, 1).symbol()).isEqualTo("A");
        // Find where B is rendered - should be on same row
        int bCol = -1;
        for (int x = 1; x < 18; x++) {
            if ("B".equals(buffer.get(x, 1).symbol())) {
                bCol = x;
                break;
            }
        }
        assertThat(bCol).as("B should be found on same row (horizontal layout)").isGreaterThan(1);
        // With spacing=0, B should be closer than with spacing=2
        // The first child gets 2 cells width, so B should start around col 3
        assertThat(bCol).as("B should be close to A with spacing=0").isLessThanOrEqualTo(5);
    }

    @Test
    void panel_allFlexProperties_fromCss() {
        Panel panel = new Panel(
            new TextElement("L").fill(),
            new TextElement("R").fill()
        );
        panel.addClass("full-flex-panel");
        // full-flex-panel has: direction: horizontal, flex: space-between, spacing: 1, margin: 1 2 1 2
        Rect area = new Rect(0, 0, 30, 6);

        panel.render(frame, area, context);

        // With margin 1 2 1 2 (top right bottom left), border starts at (2, 1)
        // At (0,0) and (1,0) should be empty (margin)
        assertThat(buffer.get(0, 0).symbol()).isEqualTo(" ");
        assertThat(buffer.get(1, 0).symbol()).isEqualTo(" ");
        // Border at (2, 1) should not be empty
        String borderChar = buffer.get(2, 1).symbol();
        assertThat(borderChar).isNotEqualTo(" ");
    }
}
