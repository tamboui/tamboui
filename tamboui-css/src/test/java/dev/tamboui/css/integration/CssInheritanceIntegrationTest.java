/*
 * Copyright (c) 2026 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.css.integration;

import dev.tamboui.buffer.Buffer;
import dev.tamboui.css.Styleable;
import dev.tamboui.css.cascade.CssStyleResolver;
import dev.tamboui.css.cascade.PseudoClassState;
import dev.tamboui.css.engine.StyleEngine;
import dev.tamboui.layout.Rect;
import dev.tamboui.style.Overflow;
import dev.tamboui.style.Style;
import dev.tamboui.widgets.paragraph.Paragraph;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static dev.tamboui.assertj.BufferAssertions.assertThat;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for CSS inheritance features (inherit keyword and structural selectors).
 * <p>
 * Tests actual rendered output to verify that inheritance features work correctly
 * in the rendering pipeline.
 */
class CssInheritanceIntegrationTest {

    private StyleEngine styleEngine;

    @BeforeEach
    void setUp() {
        styleEngine = StyleEngine.create();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // STRUCTURAL SELECTOR TESTS - Rendered Output
    // ═══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("structural selector - rendered output verification")
    class StructuralSelectorRenderedOutputTests {

        @Test
        @DisplayName(".container * { text-overflow: ellipsis } - child paragraph renders with ellipsis")
        void textOverflowStructuralSelectorRendersEllipsis() {
            // CSS: structural selector applies text-overflow to all descendants of .container
            styleEngine.addStylesheet(
                ".container * { text-overflow: ellipsis; }\n" +
                ".item { }"
            );

            // Resolve styles: child with container as ancestor
            TestStyleable container = new TestStyleable("Panel", null, setOf("container"));
            TestStyleable item = new TestStyleable("Paragraph", null, setOf("item"));

            // Resolve item with container in ancestor list — the .container * rule matches directly
            List<Styleable> ancestors = Collections.singletonList(container);
            CssStyleResolver itemResolved = styleEngine.resolve(item, PseudoClassState.NONE, ancestors);

            // Verify child got text-overflow from the structural selector
            assertThat(itemResolved.textOverflow()).contains(Overflow.ELLIPSIS);

            // Render a paragraph with the style - area is only 15 chars wide
            Rect area = new Rect(0, 0, 15, 1);
            Buffer buffer = Buffer.empty(area);

            Paragraph paragraph = Paragraph.builder()
                .text("Hello World, this is long text that should be truncated")
                .styleResolver(itemResolved)
                .build();

            paragraph.render(area, buffer);

            // Text should be truncated with ellipsis
            Buffer expected = Buffer.empty(area);
            expected.setString(0, 0, "Hello World,...", Style.EMPTY);

            assertThat(buffer).isEqualTo(expected);
        }

        @Test
        @DisplayName("without * selector - child does NOT get parent's text-overflow")
        void withoutStructuralSelectorChildDoesNotInherit() {
            // CSS: container has text-overflow but no descendant selector
            styleEngine.addStylesheet(
                ".container { text-overflow: ellipsis; }\n" +
                ".item { }"
            );

            TestStyleable container = new TestStyleable("Panel", null, setOf("container"));
            TestStyleable item = new TestStyleable("Paragraph", null, setOf("item"));

            // Resolve with container as ancestor — no .container * rule, so no match
            List<Styleable> ancestors = Collections.singletonList(container);
            CssStyleResolver itemResolved = styleEngine.resolve(item, PseudoClassState.NONE, ancestors);

            // Apply withFallback for natural inheritance check — text-overflow is NOT inheritable
            CssStyleResolver containerResolved = styleEngine.resolve(container);
            CssStyleResolver mergedResolver = itemResolved.withFallback(containerResolved);

            // Child should NOT have text-overflow (not inheritable by default)
            assertThat(mergedResolver.textOverflow()).isEmpty();

            // Render: text should be clipped (default), not ellipsis
            Rect area = new Rect(0, 0, 15, 1);
            Buffer buffer = Buffer.empty(area);

            Paragraph paragraph = Paragraph.builder()
                .text("Hello World, this is long text that should be clipped")
                .styleResolver(mergedResolver)
                .build();

            paragraph.render(area, buffer);

            // Text should be clipped (no ellipsis)
            Buffer expected = Buffer.empty(area);
            expected.setString(0, 0, "Hello World, th", Style.EMPTY);

            assertThat(buffer).isEqualTo(expected);
        }

        @Test
        @DisplayName("child can override structural selector text-overflow")
        void childCanOverrideStructuralSelectorTextOverflow() {
            styleEngine.addStylesheet(
                ".container * { text-overflow: ellipsis; }\n" +
                ".item { text-overflow: clip; }"
            );

            TestStyleable container = new TestStyleable("Panel", null, setOf("container"));
            TestStyleable item = new TestStyleable("Paragraph", null, setOf("item"));

            // Resolve item with container as ancestor — both rules match,
            // .item has higher specificity than .container * so it wins
            List<Styleable> ancestors = Collections.singletonList(container);
            CssStyleResolver itemResolved = styleEngine.resolve(item, PseudoClassState.NONE, ancestors);

            // Child's explicit value should override
            assertThat(itemResolved.textOverflow()).contains(Overflow.CLIP);

            // Render: text should be clipped
            Rect area = new Rect(0, 0, 15, 1);
            Buffer buffer = Buffer.empty(area);

            Paragraph paragraph = Paragraph.builder()
                .text("Hello World, this is long text")
                .styleResolver(itemResolved)
                .build();

            paragraph.render(area, buffer);

            // Text should be clipped (child's override wins)
            Buffer expected = Buffer.empty(area);
            expected.setString(0, 0, "Hello World, th", Style.EMPTY);

            assertThat(buffer).isEqualTo(expected);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // INHERIT KEYWORD TESTS - Rendered Output
    // ═══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("inherit keyword - rendered output verification")
    class InheritKeywordRenderedOutputTests {

        @Test
        @DisplayName("text-overflow: inherit - child explicitly inherits and renders with ellipsis")
        void textOverflowInheritRendersEllipsis() {
            // CSS: container has text-overflow (not inheritable), child uses inherit keyword
            styleEngine.addStylesheet(
                ".container { text-overflow: ellipsis; }\n" +  // NOT inheritable
                ".item { text-overflow: inherit; }"            // Child explicitly inherits
            );

            TestStyleable container = new TestStyleable("Panel", null, setOf("container"));
            TestStyleable item = new TestStyleable("Paragraph", null, setOf("item"));

            CssStyleResolver containerResolved = styleEngine.resolve(container);
            CssStyleResolver itemResolved = styleEngine.resolve(item);
            CssStyleResolver mergedResolver = itemResolved.withFallback(containerResolved);

            // Child should have text-overflow via explicit inherit
            assertThat(mergedResolver.textOverflow()).contains(Overflow.ELLIPSIS);

            // Render with inherited ellipsis
            Rect area = new Rect(0, 0, 15, 1);
            Buffer buffer = Buffer.empty(area);

            Paragraph paragraph = Paragraph.builder()
                .text("Hello World, this is long text that should be truncated")
                .styleResolver(mergedResolver)
                .build();

            paragraph.render(area, buffer);

            // Text should be truncated with ellipsis
            Buffer expected = Buffer.empty(area);
            expected.setString(0, 0, "Hello World,...", Style.EMPTY);

            assertThat(buffer).isEqualTo(expected);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // MULTI-LEVEL INHERITANCE TESTS
    // ═══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Multi-level inheritance")
    class MultiLevelInheritanceTests {

        @Test
        @DisplayName("structural selector propagates through grandparent -> parent -> child")
        void structuralSelectorPropagatesMultipleLevels() {
            styleEngine.addStylesheet(
                ".grandparent * { text-overflow: ellipsis; }\n" +
                ".parent { }\n" +
                ".child { }"
            );

            TestStyleable grandparent = new TestStyleable("Panel", null, setOf("grandparent"));
            TestStyleable parent = new TestStyleable("Row", null, setOf("parent"));
            TestStyleable child = new TestStyleable("Paragraph", null, setOf("child"));

            // Resolve child with [grandparent, parent] ancestor list
            // Descendant selector .grandparent * matches at any depth
            List<Styleable> ancestors = Arrays.asList(grandparent, parent);
            CssStyleResolver childResolved = styleEngine.resolve(child, PseudoClassState.NONE, ancestors);

            // Child should get text-overflow from the structural selector
            assertThat(childResolved.textOverflow()).contains(Overflow.ELLIPSIS);

            // Verify rendered output
            Rect area = new Rect(0, 0, 15, 1);
            Buffer buffer = Buffer.empty(area);

            Paragraph paragraph = Paragraph.builder()
                .text("This text is too long and will be truncated with ellipsis")
                .styleResolver(childResolved)
                .build();

            paragraph.render(area, buffer);

            Buffer expected = Buffer.empty(area);
            expected.setString(0, 0, "This text is...", Style.EMPTY);

            assertThat(buffer).isEqualTo(expected);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // HELPER METHODS
    // ═══════════════════════════════════════════════════════════════════════════

    private static Set<String> setOf(String... values) {
        Set<String> set = new HashSet<>();
        Collections.addAll(set, values);
        return set;
    }

    /**
     * Test Styleable implementation.
     */
    private static class TestStyleable implements Styleable {
        private final String type;
        private final String id;
        private final Set<String> classes;

        TestStyleable(String type, String id, Set<String> classes) {
            this.type = type;
            this.id = id;
            this.classes = classes;
        }

        @Override
        public String styleType() {
            return type;
        }

        @Override
        public Optional<String> cssId() {
            return Optional.ofNullable(id);
        }

        @Override
        public Set<String> cssClasses() {
            return classes;
        }

        @Override
        public Optional<Styleable> cssParent() {
            return Optional.empty();
        }
    }
}
