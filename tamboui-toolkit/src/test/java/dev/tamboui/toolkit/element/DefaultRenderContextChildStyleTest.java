/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.toolkit.element;

import dev.tamboui.css.Styleable;
import dev.tamboui.css.cascade.PseudoClassState;
import dev.tamboui.css.engine.StyleEngine;
import dev.tamboui.style.Color;
import dev.tamboui.style.Style;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class DefaultRenderContextChildStyleTest {

    private DefaultRenderContext context;
    private StyleEngine styleEngine;

    @BeforeEach
    void setUp() {
        context = DefaultRenderContext.createEmpty();
        styleEngine = StyleEngine.create();
        context.setStyleEngine(styleEngine);
    }

    @Test
    @DisplayName("childStyle with ChildPosition resolves nth-child(odd)")
    void childStyleResolvesNthChildOdd() {
        // Given CSS with nth-child selectors
        String css = "ListElement-item:nth-child(odd) { background: red; }\n" +
                     "ListElement-item:nth-child(even) { background: blue; }";
        styleEngine.addStylesheet("test", css);
        styleEngine.setActiveStylesheet("test");

        // And a parent element on the stack
        Styleable parent = createStyleable("ListElement");
        context.withElement(parent, Style.EMPTY, () -> {
            // When resolving child style for first item (index 0, nthChild = 1 = odd)
            ChildPosition pos = ChildPosition.of(0, 5);
            Style style = context.childStyle("item", pos);

            // Then background should be red (odd)
            assertThat(style.bg()).isPresent();
            assertThat(style.bg().get()).isEqualTo(Color.RED);
        });
    }

    @Test
    @DisplayName("childStyle with ChildPosition resolves nth-child(even)")
    void childStyleResolvesNthChildEven() {
        // Given CSS with nth-child selectors
        String css = "ListElement-item:nth-child(odd) { background: red; }\n" +
                     "ListElement-item:nth-child(even) { background: blue; }";
        styleEngine.addStylesheet("test", css);
        styleEngine.setActiveStylesheet("test");

        // And a parent element on the stack
        Styleable parent = createStyleable("ListElement");
        context.withElement(parent, Style.EMPTY, () -> {
            // When resolving child style for second item (index 1, nthChild = 2 = even)
            ChildPosition pos = ChildPosition.of(1, 5);
            Style style = context.childStyle("item", pos);

            // Then background should be blue (even)
            assertThat(style.bg()).isPresent();
            assertThat(style.bg().get()).isEqualTo(Color.BLUE);
        });
    }

    @Test
    @DisplayName("childStyle returns current style when element stack is empty")
    void childStyleReturnsCurrentStyleWhenStackEmpty() {
        // Given CSS with nth-child selectors
        String css = "ListElement-item:nth-child(odd) { background: red; }";
        styleEngine.addStylesheet("test", css);
        styleEngine.setActiveStylesheet("test");

        // When resolving without parent on stack
        ChildPosition pos = ChildPosition.of(0, 5);
        Style style = context.childStyle("item", pos);

        // Then should return empty style (no CSS resolved)
        assertThat(style).isEqualTo(Style.EMPTY);
    }

    @Test
    @DisplayName("childStyle with hex colors parses correctly")
    void childStyleParsesHexColors() {
        // Given CSS with hex color values (like the real theme)
        String css = "ListElement-item:nth-child(odd) { background: #ff0000; }\n" +
                     "ListElement-item:nth-child(even) { background: #0000ff; }";
        styleEngine.addStylesheet("test", css);
        styleEngine.setActiveStylesheet("test");

        // And a parent element on the stack
        Styleable parent = createStyleable("ListElement");
        context.withElement(parent, Style.EMPTY, () -> {
            // When resolving for odd position
            ChildPosition oddPos = ChildPosition.of(0, 5);
            Style oddStyle = context.childStyle("item", oddPos);

            // Then background should be parsed as RGB red
            assertThat(oddStyle.bg()).isPresent();
            // #ff0000 = RGB(255, 0, 0)
            Color expectedRed = Color.rgb(255, 0, 0);
            assertThat(oddStyle.bg().get()).isEqualTo(expectedRed);

            // When resolving for even position
            ChildPosition evenPos = ChildPosition.of(1, 5);
            Style evenStyle = context.childStyle("item", evenPos);

            // Then background should be parsed as RGB blue
            assertThat(evenStyle.bg()).isPresent();
            Color expectedBlue = Color.rgb(0, 0, 255);
            assertThat(evenStyle.bg().get()).isEqualTo(expectedBlue);
        });
    }

    @Test
    @DisplayName("childStyle with universal selector override")
    void childStyleWithUniversalSelectorOverride() {
        // Given CSS with universal selector AND specific nth-child rules (like real theme)
        String css = "* { background: black; color: white; }\n" +
                     "ListElement-item { color: white; }\n" +
                     "ListElement-item:nth-child(odd) { background: #992299; }\n" +
                     "ListElement-item:nth-child(even) { background: #229922; }";
        styleEngine.addStylesheet("test", css);
        styleEngine.setActiveStylesheet("test");

        // And a parent element on the stack
        Styleable parent = createStyleable("ListElement");
        context.withElement(parent, Style.EMPTY, () -> {
            // When resolving for odd position (index 0 -> nthChild 1)
            ChildPosition oddPos = ChildPosition.of(0, 5);
            Style oddStyle = context.childStyle("item", oddPos);

            // Then specific background should override universal selector
            assertThat(oddStyle.bg()).isPresent();
            assertThat(oddStyle.bg().get()).isEqualTo(Color.rgb(0x99, 0x22, 0x99));

            // When resolving for even position (index 1 -> nthChild 2)
            ChildPosition evenPos = ChildPosition.of(1, 5);
            Style evenStyle = context.childStyle("item", evenPos);

            // Then specific background should override universal selector
            assertThat(evenStyle.bg()).isPresent();
            assertThat(evenStyle.bg().get()).isEqualTo(Color.rgb(0x22, 0x99, 0x22));
        });
    }

    private Styleable createStyleable(String type) {
        return new Styleable() {
            @Override
            public String styleType() {
                return type;
            }

            @Override
            public Optional<String> cssId() {
                return Optional.empty();
            }

            @Override
            public Set<String> cssClasses() {
                return Collections.emptySet();
            }

            @Override
            public Optional<Styleable> cssParent() {
                return Optional.empty();
            }
        };
    }
}
