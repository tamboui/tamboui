/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.toolkit.elements;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import dev.tamboui.buffer.Buffer;
import dev.tamboui.css.engine.StyleEngine;
import dev.tamboui.layout.Rect;
import dev.tamboui.style.Color;
import dev.tamboui.terminal.Frame;
import dev.tamboui.toolkit.AbstractElementTest;
import dev.tamboui.toolkit.element.DefaultRenderContext;
import dev.tamboui.toolkit.element.Element;

import static dev.tamboui.toolkit.Toolkit.column;
import static dev.tamboui.toolkit.Toolkit.panel;
import static dev.tamboui.toolkit.Toolkit.text;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Child ({@code >}) and multi-level descendant selectors, matched against the elements being rendered.
 */
class ChildAndDescendantSelectorRenderingTest extends AbstractElementTest {

    private StyleEngine styleEngine;
    private DefaultRenderContext context;
    private Buffer buffer;
    private Frame frame;
    private Rect area;

    @BeforeEach
    void setUp() {
        styleEngine = StyleEngine.create();
        context = DefaultRenderContext.createEmpty();
        context.setStyleEngine(styleEngine);

        area = new Rect(0, 0, 20, 5);
        buffer = Buffer.empty(area);
        frame = Frame.forTesting(buffer);
    }

    @Test
    @DisplayName("Panel > TextElement styles text whose parent is the panel")
    void childSelectorMatchesTheParent() {
        render("Panel > TextElement { color: red; }", column(panel(text("x"))));

        assertThat(foregroundOf("x")).contains(Color.RED);
    }

    @Test
    @DisplayName("Column > TextElement does not style text whose parent is a panel inside the column")
    void childSelectorDoesNotMatchTheGrandparent() {
        render("Column > TextElement { color: red; }", column(panel(text("x"))));

        assertThat(foregroundOf("x")).isNotEqualTo(Optional.of(Color.RED));
    }

    @Test
    @DisplayName("#outer #inner TextElement matches ancestors in document order")
    void descendantSelectorMatchesAncestorsInOrder() {
        render("#outer #inner TextElement { color: red; }",
            column(panel(text("x")).id("inner")).id("outer"));

        assertThat(foregroundOf("x")).contains(Color.RED);
    }

    @Test
    @DisplayName("#inner #outer TextElement does not match when #outer is the outermost element")
    void descendantSelectorDoesNotMatchReversedAncestors() {
        render("#inner #outer TextElement { color: red; }",
            column(panel(text("x")).id("inner")).id("outer"));

        assertThat(foregroundOf("x")).isNotEqualTo(Optional.of(Color.RED));
    }

    @Test
    @DisplayName("Column > Panel border-color is still applied, an element is not its own parent")
    void childSelectorForPropertiesResolvedWhileRendering() {
        // Panel resolves border-color from inside its own renderContent, when it is on the element stack
        render("Column > Panel { border-color: red; }", column(panel(text("x"))));

        assertThat(buffer.get(0, 0).style().fg()).contains(Color.RED);
    }

    private void render(String css, Element root) {
        styleEngine.addStylesheet("test", css);
        styleEngine.setActiveStylesheet("test");
        root.render(frame, area, context);
    }

    private Optional<Color> foregroundOf(String symbol) {
        for (int y = area.top(); y < area.bottom(); y++) {
            for (int x = area.left(); x < area.right(); x++) {
                if (symbol.equals(buffer.get(x, y).symbol())) {
                    return buffer.get(x, y).style().fg();
                }
            }
        }
        throw new AssertionError("'" + symbol + "' was not rendered");
    }
}
