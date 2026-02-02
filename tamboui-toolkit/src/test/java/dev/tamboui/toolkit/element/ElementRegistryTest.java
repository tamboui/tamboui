/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.toolkit.element;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import dev.tamboui.css.cascade.PseudoClassState;
import dev.tamboui.layout.Rect;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for ElementRegistry CSS selector support.
 */
class ElementRegistryTest {

    private ElementRegistry registry;
    private Rect area1;
    private Rect area2;
    private Rect area3;

    @BeforeEach
    void setUp() {
        registry = new ElementRegistry();
        area1 = new Rect(0, 0, 10, 5);
        area2 = new Rect(10, 0, 10, 5);
        area3 = new Rect(20, 0, 10, 5);
    }

    @Nested
    @DisplayName("ID selector (#id)")
    class IdSelector {

        @Test
        @DisplayName("query #id returns matching element")
        void queryByIdReturnsMatchingElement() {
            registry.register("header", "Panel", setOf("main"), area1);

            Optional<ElementRegistry.ElementInfo> result = registry.query("#header");

            assertThat(result).isPresent();
            assertThat(result.get().id()).isEqualTo("header");
            assertThat(result.get().area()).isEqualTo(area1);
        }

        @Test
        @DisplayName("query #id returns empty for non-existent ID")
        void queryByIdReturnsEmptyForNonExistent() {
            registry.register("header", "Panel", setOf("main"), area1);

            Optional<ElementRegistry.ElementInfo> result = registry.query("#footer");

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("getArea returns area for ID")
        void getAreaReturnsAreaForId() {
            registry.register("header", "Panel", setOf("main"), area1);

            Rect result = registry.getArea("header");

            assertThat(result).isEqualTo(area1);
        }
    }

    @Nested
    @DisplayName("Class selector (.class)")
    class ClassSelector {

        @Test
        @DisplayName("query .class returns first matching element")
        void queryByClassReturnsFirstMatch() {
            registry.register("btn1", "Button", setOf("primary"), area1);
            registry.register("btn2", "Button", setOf("primary"), area2);

            Optional<ElementRegistry.ElementInfo> result = registry.query(".primary");

            assertThat(result).isPresent();
            assertThat(result.get().id()).isEqualTo("btn1");
        }

        @Test
        @DisplayName("queryAll .class returns all matching elements")
        void queryAllByClassReturnsAllMatches() {
            registry.register("btn1", "Button", setOf("primary"), area1);
            registry.register("btn2", "Button", setOf("secondary"), area2);
            registry.register("btn3", "Button", setOf("primary", "large"), area3);

            List<ElementRegistry.ElementInfo> results = registry.queryAll(".primary");

            assertThat(results).hasSize(2);
            assertThat(results.get(0).id()).isEqualTo("btn1");
            assertThat(results.get(1).id()).isEqualTo("btn3");
        }

        @Test
        @DisplayName("query .class1.class2 requires both classes")
        void queryMultipleClassesRequiresBoth() {
            registry.register("btn1", "Button", setOf("primary"), area1);
            registry.register("btn2", "Button", setOf("primary", "large"), area2);

            List<ElementRegistry.ElementInfo> results = registry.queryAll(".primary.large");

            assertThat(results).hasSize(1);
            assertThat(results.get(0).id()).isEqualTo("btn2");
        }
    }

    @Nested
    @DisplayName("Type selector (Type)")
    class TypeSelector {

        @Test
        @DisplayName("query Type returns first matching element")
        void queryByTypeReturnsFirstMatch() {
            registry.register("panel1", "Panel", setOf("main"), area1);
            registry.register("btn1", "Button", setOf("primary"), area2);

            Optional<ElementRegistry.ElementInfo> result = registry.query("Panel");

            assertThat(result).isPresent();
            assertThat(result.get().type()).isEqualTo("Panel");
        }

        @Test
        @DisplayName("queryAll Type returns all matching elements")
        void queryAllByTypeReturnsAllMatches() {
            registry.register("panel1", "Panel", setOf("main"), area1);
            registry.register("btn1", "Button", setOf("primary"), area2);
            registry.register("panel2", "Panel", setOf("sidebar"), area3);

            List<ElementRegistry.ElementInfo> results = registry.queryAll("Panel");

            assertThat(results).hasSize(2);
        }
    }

    @Nested
    @DisplayName("Universal selector (*)")
    class UniversalSelector {

        @Test
        @DisplayName("query * returns first element")
        void queryUniversalReturnsFirst() {
            registry.register("panel1", "Panel", setOf("main"), area1);
            registry.register("btn1", "Button", setOf("primary"), area2);

            Optional<ElementRegistry.ElementInfo> result = registry.query("*");

            assertThat(result).isPresent();
            assertThat(result.get().id()).isEqualTo("panel1");
        }

        @Test
        @DisplayName("queryAll * returns all elements")
        void queryAllUniversalReturnsAll() {
            registry.register("panel1", "Panel", setOf("main"), area1);
            registry.register("btn1", "Button", setOf("primary"), area2);
            registry.register("panel2", "Panel", setOf("sidebar"), area3);

            List<ElementRegistry.ElementInfo> results = registry.queryAll("*");

            assertThat(results).hasSize(3);
        }
    }

    @Nested
    @DisplayName("Pseudo-class selector (:pseudo)")
    class PseudoClassSelector {

        @Test
        @DisplayName("query :focus returns element when focused")
        void queryFocusReturnsElementWhenFocused() {
            registry.register("input1", "Input", setOf(), area1);

            Optional<ElementRegistry.ElementInfo> result = registry.query(":focus", PseudoClassState.ofFocused());

            assertThat(result).isPresent();
        }

        @Test
        @DisplayName("query :focus returns empty when not focused")
        void queryFocusReturnsEmptyWhenNotFocused() {
            registry.register("input1", "Input", setOf(), area1);

            Optional<ElementRegistry.ElementInfo> result = registry.query(":focus", PseudoClassState.NONE);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("query Button:focus combines type and pseudo-class")
        void queryCombinesTypeAndPseudo() {
            registry.register("input1", "Input", setOf(), area1);
            registry.register("btn1", "Button", setOf(), area2);

            Optional<ElementRegistry.ElementInfo> result = registry.query("Button:focus", PseudoClassState.ofFocused());

            assertThat(result).isPresent();
            assertThat(result.get().id()).isEqualTo("btn1");
        }

        @Test
        @DisplayName("query :hover returns element when hovered")
        void queryHoverReturnsElementWhenHovered() {
            registry.register("btn1", "Button", setOf(), area1);

            Optional<ElementRegistry.ElementInfo> result = registry.query(":hover", PseudoClassState.ofHovered());

            assertThat(result).isPresent();
        }

        @Test
        @DisplayName("query :disabled returns element when disabled")
        void queryDisabledReturnsElementWhenDisabled() {
            registry.register("btn1", "Button", setOf(), area1);

            Optional<ElementRegistry.ElementInfo> result = registry.query(":disabled", PseudoClassState.ofDisabled());

            assertThat(result).isPresent();
        }
    }

    @Nested
    @DisplayName("Attribute selector ([attr])")
    class AttributeSelector {

        @Test
        @DisplayName("query [attr] returns element with attribute")
        void queryAttributeExistence() {
            Map<String, String> attrs = new HashMap<>();
            attrs.put("title", "Hello");
            registry.register("panel1", "Panel", setOf(), attrs, area1, null);
            registry.register("panel2", "Panel", setOf(), null, area2, null);

            List<ElementRegistry.ElementInfo> results = registry.queryAll("[title]");

            assertThat(results).hasSize(1);
            assertThat(results.get(0).id()).isEqualTo("panel1");
        }

        @Test
        @DisplayName("query [attr=value] returns element with matching value")
        void queryAttributeEquals() {
            Map<String, String> attrs1 = new HashMap<>();
            attrs1.put("title", "Hello");
            Map<String, String> attrs2 = new HashMap<>();
            attrs2.put("title", "World");
            registry.register("panel1", "Panel", setOf(), attrs1, area1, null);
            registry.register("panel2", "Panel", setOf(), attrs2, area2, null);

            List<ElementRegistry.ElementInfo> results = registry.queryAll("[title=\"Hello\"]");

            assertThat(results).hasSize(1);
            assertThat(results.get(0).id()).isEqualTo("panel1");
        }
    }

    @Nested
    @DisplayName("Combined selectors")
    class CombinedSelectors {

        @Test
        @DisplayName("Type.class matches elements with both type and class")
        void typeAndClassSelector() {
            registry.register("panel1", "Panel", setOf("main"), area1);
            registry.register("btn1", "Button", setOf("main"), area2);
            registry.register("panel2", "Panel", setOf("sidebar"), area3);

            List<ElementRegistry.ElementInfo> results = registry.queryAll("Panel.main");

            assertThat(results).hasSize(1);
            assertThat(results.get(0).id()).isEqualTo("panel1");
        }

        @Test
        @DisplayName("Type#id matches element with both type and ID")
        void typeAndIdSelector() {
            registry.register("header", "Panel", setOf("main"), area1);
            registry.register("header", "Row", setOf(), area2);  // Same ID, different type

            Optional<ElementRegistry.ElementInfo> result = registry.query("Panel#header");

            assertThat(result).isPresent();
            assertThat(result.get().type()).isEqualTo("Panel");
        }

        @Test
        @DisplayName(".class#id matches element with both class and ID")
        void classAndIdSelector() {
            registry.register("btn1", "Button", setOf("primary"), area1);
            registry.register("btn2", "Button", setOf("secondary"), area2);

            Optional<ElementRegistry.ElementInfo> result = registry.query(".primary#btn1");

            assertThat(result).isPresent();
            assertThat(result.get().id()).isEqualTo("btn1");
        }
    }

    @Nested
    @DisplayName("Descendant selector (A B)")
    class DescendantSelector {

        @Test
        @DisplayName("A B matches child of A")
        void matchesDirectChild() {
            ElementRegistry.ElementInfo parent = registerAndGet("panel1", "Panel", setOf("main"), area1, null);
            registry.register("btn1", "Button", setOf("primary"), null, area2, parent);

            List<ElementRegistry.ElementInfo> results = registry.queryAll("Panel Button");

            assertThat(results).hasSize(1);
            assertThat(results.get(0).id()).isEqualTo("btn1");
        }

        @Test
        @DisplayName("A B matches nested descendant of A")
        void matchesNestedDescendant() {
            ElementRegistry.ElementInfo grandparent = registerAndGet("panel1", "Panel", setOf("main"), area1, null);
            ElementRegistry.ElementInfo parent = registerAndGet("row1", "Row", setOf(), area2, grandparent);
            registry.register("btn1", "Button", setOf("primary"), null, area3, parent);

            List<ElementRegistry.ElementInfo> results = registry.queryAll("Panel Button");

            assertThat(results).hasSize(1);
            assertThat(results.get(0).id()).isEqualTo("btn1");
        }

        @Test
        @DisplayName("A B does not match without ancestor")
        void doesNotMatchWithoutAncestor() {
            ElementRegistry.ElementInfo parent = registerAndGet("row1", "Row", setOf(), area1, null);
            registry.register("btn1", "Button", setOf("primary"), null, area2, parent);

            List<ElementRegistry.ElementInfo> results = registry.queryAll("Panel Button");

            assertThat(results).isEmpty();
        }
    }

    @Nested
    @DisplayName("Child selector (A > B)")
    class ChildSelector {

        @Test
        @DisplayName("A > B matches direct child of A")
        void matchesDirectChild() {
            ElementRegistry.ElementInfo parent = registerAndGet("panel1", "Panel", setOf("main"), area1, null);
            registry.register("btn1", "Button", setOf("primary"), null, area2, parent);

            List<ElementRegistry.ElementInfo> results = registry.queryAll("Panel > Button");

            assertThat(results).hasSize(1);
            assertThat(results.get(0).id()).isEqualTo("btn1");
        }

        @Test
        @DisplayName("A > B does not match nested descendant")
        void doesNotMatchNestedDescendant() {
            ElementRegistry.ElementInfo grandparent = registerAndGet("panel1", "Panel", setOf("main"), area1, null);
            ElementRegistry.ElementInfo parent = registerAndGet("row1", "Row", setOf(), area2, grandparent);
            registry.register("btn1", "Button", setOf("primary"), null, area3, parent);

            List<ElementRegistry.ElementInfo> results = registry.queryAll("Panel > Button");

            assertThat(results).isEmpty();
        }
    }

    @Nested
    @DisplayName("Edge cases")
    class EdgeCases {

        @Test
        @DisplayName("null selector returns empty")
        void nullSelectorReturnsEmpty() {
            registry.register("header", "Panel", setOf("main"), area1);

            assertThat(registry.query(null)).isEmpty();
            assertThat(registry.queryAll(null)).isEmpty();
        }

        @Test
        @DisplayName("empty selector returns empty")
        void emptySelectorReturnsEmpty() {
            registry.register("header", "Panel", setOf("main"), area1);

            assertThat(registry.query("")).isEmpty();
            assertThat(registry.queryAll("")).isEmpty();
        }

        @Test
        @DisplayName("clear removes all elements")
        void clearRemovesAllElements() {
            registry.register("header", "Panel", setOf("main"), area1);
            registry.register("footer", "Panel", setOf("main"), area2);

            registry.clear();

            assertThat(registry.size()).isZero();
            assertThat(registry.query("#header")).isEmpty();
        }

        @Test
        @DisplayName("elements without ID can be queried by type or class")
        void elementsWithoutIdCanBeQueried() {
            registry.register(null, "Spacer", setOf(), area1);
            registry.register(null, "Spacer", setOf("large"), area2);

            List<ElementRegistry.ElementInfo> results = registry.queryAll("Spacer");

            assertThat(results).hasSize(2);
        }

        @Test
        @DisplayName("invalid selector returns empty")
        void invalidSelectorReturnsEmpty() {
            registry.register("header", "Panel", setOf("main"), area1);

            assertThat(registry.query(">>>")).isEmpty();
            assertThat(registry.queryAll(">>>")).isEmpty();
        }
    }

    // Helper methods

    private static Set<String> setOf(String... values) {
        return new HashSet<>(Arrays.asList(values));
    }

    private ElementRegistry.ElementInfo registerAndGet(String id, String type, Set<String> classes,
                                                        Rect area, ElementRegistry.ElementInfo parent) {
        registry.register(id, type, classes, null, area, parent);
        return registry.query("#" + id).orElseThrow(() -> new AssertionError("Element not found: " + id));
    }
}
