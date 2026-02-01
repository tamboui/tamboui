/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.css.selector;

import dev.tamboui.css.Styleable;
import dev.tamboui.css.cascade.PseudoClassState;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for SelectorParser.
 */
class SelectorParserTest {

    @Nested
    @DisplayName("Type selector")
    class TypeSelectorTests {

        @Test
        @DisplayName("parses simple type selector")
        void parsesSimpleType() {
            Selector selector = SelectorParser.parse("Panel");

            assertThat(selector).isInstanceOf(TypeSelector.class);
            assertThat(selector.toCss()).isEqualTo("Panel");
        }

        @Test
        @DisplayName("type selector matches element with matching type")
        void matchesElementWithType() {
            Selector selector = SelectorParser.parse("Button");
            TestElement element = new TestElement("Button", null, Collections.emptySet());

            assertThat(selector.matches(element, PseudoClassState.NONE, Collections.emptyList())).isTrue();
        }

        @Test
        @DisplayName("type selector does not match element with different type")
        void doesNotMatchDifferentType() {
            Selector selector = SelectorParser.parse("Button");
            TestElement element = new TestElement("Panel", null, Collections.emptySet());

            assertThat(selector.matches(element, PseudoClassState.NONE, Collections.emptyList())).isFalse();
        }
    }

    @Nested
    @DisplayName("ID selector (#id)")
    class IdSelectorTests {

        @Test
        @DisplayName("parses ID selector")
        void parsesIdSelector() {
            Selector selector = SelectorParser.parse("#header");

            assertThat(selector).isInstanceOf(IdSelector.class);
            assertThat(selector.toCss()).isEqualTo("#header");
        }

        @Test
        @DisplayName("ID selector matches element with matching ID")
        void matchesElementWithId() {
            Selector selector = SelectorParser.parse("#sidebar");
            TestElement element = new TestElement("Panel", "sidebar", Collections.emptySet());

            assertThat(selector.matches(element, PseudoClassState.NONE, Collections.emptyList())).isTrue();
        }

        @Test
        @DisplayName("ID selector does not match element with different ID")
        void doesNotMatchDifferentId() {
            Selector selector = SelectorParser.parse("#sidebar");
            TestElement element = new TestElement("Panel", "header", Collections.emptySet());

            assertThat(selector.matches(element, PseudoClassState.NONE, Collections.emptyList())).isFalse();
        }
    }

    @Nested
    @DisplayName("Class selector (.class)")
    class ClassSelectorTests {

        @Test
        @DisplayName("parses class selector")
        void parsesClassSelector() {
            Selector selector = SelectorParser.parse(".primary");

            assertThat(selector).isInstanceOf(ClassSelector.class);
            assertThat(selector.toCss()).isEqualTo(".primary");
        }

        @Test
        @DisplayName("class selector matches element with matching class")
        void matchesElementWithClass() {
            Selector selector = SelectorParser.parse(".highlight");
            TestElement element = new TestElement("Text", null, setOf("highlight", "bold"));

            assertThat(selector.matches(element, PseudoClassState.NONE, Collections.emptyList())).isTrue();
        }

        @Test
        @DisplayName("class selector does not match element without class")
        void doesNotMatchWithoutClass() {
            Selector selector = SelectorParser.parse(".highlight");
            TestElement element = new TestElement("Text", null, setOf("bold"));

            assertThat(selector.matches(element, PseudoClassState.NONE, Collections.emptyList())).isFalse();
        }
    }

    @Nested
    @DisplayName("Universal selector (*)")
    class UniversalSelectorTests {

        @Test
        @DisplayName("parses universal selector")
        void parsesUniversalSelector() {
            Selector selector = SelectorParser.parse("*");

            assertThat(selector).isInstanceOf(UniversalSelector.class);
            assertThat(selector.toCss()).isEqualTo("*");
        }

        @Test
        @DisplayName("universal selector matches any element")
        void matchesAnyElement() {
            Selector selector = SelectorParser.parse("*");

            assertThat(selector.matches(
                    new TestElement("Panel", null, Collections.emptySet()),
                    PseudoClassState.NONE, Collections.emptyList())).isTrue();
            assertThat(selector.matches(
                    new TestElement("Button", "btn1", setOf("primary")),
                    PseudoClassState.NONE, Collections.emptyList())).isTrue();
        }
    }

    @Nested
    @DisplayName("Pseudo-class selector (:pseudo)")
    class PseudoClassSelectorTests {

        @Test
        @DisplayName("parses pseudo-class selector")
        void parsesPseudoClassSelector() {
            Selector selector = SelectorParser.parse(":focus");

            assertThat(selector).isInstanceOf(PseudoClassSelector.class);
            assertThat(selector.toCss()).isEqualTo(":focus");
        }

        @Test
        @DisplayName("parses functional pseudo-class selector")
        void parsesFunctionalPseudoClass() {
            Selector selector = SelectorParser.parse(":nth-child(even)");

            assertThat(selector).isInstanceOf(PseudoClassSelector.class);
            assertThat(selector.toCss()).isEqualTo(":nth-child(even)");
        }

        @Test
        @DisplayName(":focus matches focused element")
        void focusMatchesFocusedElement() {
            Selector selector = SelectorParser.parse(":focus");
            TestElement element = new TestElement("Input", null, Collections.emptySet());

            assertThat(selector.matches(element, PseudoClassState.ofFocused(), Collections.emptyList())).isTrue();
            assertThat(selector.matches(element, PseudoClassState.NONE, Collections.emptyList())).isFalse();
        }

        @Test
        @DisplayName(":hover matches hovered element")
        void hoverMatchesHoveredElement() {
            Selector selector = SelectorParser.parse(":hover");
            TestElement element = new TestElement("Button", null, Collections.emptySet());

            assertThat(selector.matches(element, PseudoClassState.ofHovered(), Collections.emptyList())).isTrue();
            assertThat(selector.matches(element, PseudoClassState.NONE, Collections.emptyList())).isFalse();
        }

        @Test
        @DisplayName(":disabled matches disabled element")
        void disabledMatchesDisabledElement() {
            Selector selector = SelectorParser.parse(":disabled");
            TestElement element = new TestElement("Button", null, Collections.emptySet());

            assertThat(selector.matches(element, PseudoClassState.ofDisabled(), Collections.emptyList())).isTrue();
            assertThat(selector.matches(element, PseudoClassState.NONE, Collections.emptyList())).isFalse();
        }
    }

    @Nested
    @DisplayName("Attribute selector ([attr])")
    class AttributeSelectorTests {

        @Test
        @DisplayName("parses attribute existence selector")
        void parsesAttributeExistence() {
            Selector selector = SelectorParser.parse("[title]");

            assertThat(selector).isInstanceOf(AttributeSelector.class);
            assertThat(selector.toCss()).isEqualTo("[title]");
        }

        @Test
        @DisplayName("parses attribute equals selector")
        void parsesAttributeEquals() {
            Selector selector = SelectorParser.parse("[title=\"Hello\"]");

            assertThat(selector).isInstanceOf(AttributeSelector.class);
            assertThat(selector.toCss()).isEqualTo("[title=\"Hello\"]");
        }

        @Test
        @DisplayName("parses attribute starts-with selector")
        void parsesAttributeStartsWith() {
            Selector selector = SelectorParser.parse("[href^=\"https\"]");

            assertThat(selector).isInstanceOf(AttributeSelector.class);
        }

        @Test
        @DisplayName("parses attribute ends-with selector")
        void parsesAttributeEndsWith() {
            Selector selector = SelectorParser.parse("[href$=\".pdf\"]");

            assertThat(selector).isInstanceOf(AttributeSelector.class);
        }

        @Test
        @DisplayName("parses attribute contains selector")
        void parsesAttributeContains() {
            Selector selector = SelectorParser.parse("[title*=\"test\"]");

            assertThat(selector).isInstanceOf(AttributeSelector.class);
        }

        @Test
        @DisplayName("attribute existence matches element with attribute")
        void existenceMatchesWithAttribute() {
            Selector selector = SelectorParser.parse("[title]");
            TestElement element = new TestElement("Panel", null, Collections.emptySet());
            element.setAttribute("title", "My Panel");

            assertThat(selector.matches(element, PseudoClassState.NONE, Collections.emptyList())).isTrue();
        }

        @Test
        @DisplayName("attribute existence does not match element without attribute")
        void existenceDoesNotMatchWithoutAttribute() {
            Selector selector = SelectorParser.parse("[title]");
            TestElement element = new TestElement("Panel", null, Collections.emptySet());

            assertThat(selector.matches(element, PseudoClassState.NONE, Collections.emptyList())).isFalse();
        }

        @Test
        @DisplayName("attribute equals matches exact value")
        void equalsMatchesExactValue() {
            Selector selector = SelectorParser.parse("[title=\"Hello\"]");
            TestElement element = new TestElement("Panel", null, Collections.emptySet());
            element.setAttribute("title", "Hello");

            assertThat(selector.matches(element, PseudoClassState.NONE, Collections.emptyList())).isTrue();
        }
    }

    @Nested
    @DisplayName("Compound selector (Type.class#id)")
    class CompoundSelectorTests {

        @Test
        @DisplayName("parses type + class compound selector")
        void parsesTypeAndClass() {
            Selector selector = SelectorParser.parse("Button.primary");

            assertThat(selector).isInstanceOf(CompoundSelector.class);
        }

        @Test
        @DisplayName("parses type + ID compound selector")
        void parsesTypeAndId() {
            Selector selector = SelectorParser.parse("Panel#header");

            assertThat(selector).isInstanceOf(CompoundSelector.class);
        }

        @Test
        @DisplayName("parses multiple classes")
        void parsesMultipleClasses() {
            Selector selector = SelectorParser.parse(".primary.large.rounded");

            assertThat(selector).isInstanceOf(CompoundSelector.class);
        }

        @Test
        @DisplayName("compound selector matches element with all parts")
        void matchesElementWithAllParts() {
            Selector selector = SelectorParser.parse("Button.primary#submit");
            TestElement element = new TestElement("Button", "submit", setOf("primary", "large"));

            assertThat(selector.matches(element, PseudoClassState.NONE, Collections.emptyList())).isTrue();
        }

        @Test
        @DisplayName("compound selector does not match if any part fails")
        void doesNotMatchIfPartFails() {
            Selector selector = SelectorParser.parse("Button.primary#submit");
            TestElement element = new TestElement("Button", "cancel", setOf("primary"));

            assertThat(selector.matches(element, PseudoClassState.NONE, Collections.emptyList())).isFalse();
        }
    }

    @Nested
    @DisplayName("Descendant combinator (A B)")
    class DescendantCombinatorTests {

        @Test
        @DisplayName("parses descendant combinator")
        void parsesDescendantCombinator() {
            Selector selector = SelectorParser.parse("Panel Button");

            assertThat(selector).isInstanceOf(DescendantSelector.class);
        }

        @Test
        @DisplayName("descendant selector matches direct child")
        void matchesDirectChild() {
            Selector selector = SelectorParser.parse("Panel Button");
            TestElement parent = new TestElement("Panel", null, Collections.emptySet());
            TestElement child = new TestElement("Button", null, Collections.emptySet());

            assertThat(selector.matches(child, PseudoClassState.NONE, Collections.singletonList(parent))).isTrue();
        }

        @Test
        @DisplayName("descendant selector matches nested descendant")
        void matchesNestedDescendant() {
            Selector selector = SelectorParser.parse("Panel Button");
            TestElement grandparent = new TestElement("Panel", null, Collections.emptySet());
            TestElement parent = new TestElement("Row", null, Collections.emptySet());
            TestElement child = new TestElement("Button", null, Collections.emptySet());

            List<Styleable> ancestors = Arrays.asList(grandparent, parent);
            assertThat(selector.matches(child, PseudoClassState.NONE, ancestors)).isTrue();
        }

        @Test
        @DisplayName("descendant selector does not match without ancestor")
        void doesNotMatchWithoutAncestor() {
            Selector selector = SelectorParser.parse("Panel Button");
            TestElement parent = new TestElement("Row", null, Collections.emptySet());
            TestElement child = new TestElement("Button", null, Collections.emptySet());

            assertThat(selector.matches(child, PseudoClassState.NONE, Collections.singletonList(parent))).isFalse();
        }
    }

    @Nested
    @DisplayName("Child combinator (A > B)")
    class ChildCombinatorTests {

        @Test
        @DisplayName("parses child combinator")
        void parsesChildCombinator() {
            Selector selector = SelectorParser.parse("Panel > Button");

            assertThat(selector).isInstanceOf(ChildSelector.class);
        }

        @Test
        @DisplayName("child selector matches direct child")
        void matchesDirectChild() {
            Selector selector = SelectorParser.parse("Panel > Button");
            TestElement parent = new TestElement("Panel", null, Collections.emptySet());
            TestElement child = new TestElement("Button", null, Collections.emptySet());

            assertThat(selector.matches(child, PseudoClassState.NONE, Collections.singletonList(parent))).isTrue();
        }

        @Test
        @DisplayName("child selector does not match nested descendant")
        void doesNotMatchNestedDescendant() {
            Selector selector = SelectorParser.parse("Panel > Button");
            TestElement grandparent = new TestElement("Panel", null, Collections.emptySet());
            TestElement parent = new TestElement("Row", null, Collections.emptySet());
            TestElement child = new TestElement("Button", null, Collections.emptySet());

            List<Styleable> ancestors = Arrays.asList(grandparent, parent);
            assertThat(selector.matches(child, PseudoClassState.NONE, ancestors)).isFalse();
        }
    }

    @Nested
    @DisplayName("Combinator + Universal selector")
    class CombinatorUniversalTests {

        @Test
        @DisplayName("parses Panel > * as ChildSelector(TypeSelector, UniversalSelector)")
        void parsesChildUniversal() {
            Selector selector = SelectorParser.parse("Panel > *");

            assertThat(selector).isInstanceOf(ChildSelector.class);
            ChildSelector child = (ChildSelector) selector;
            assertThat(child.parent()).isInstanceOf(TypeSelector.class);
            assertThat(((TypeSelector) child.parent()).typeName()).isEqualTo("Panel");
            assertThat(child.child()).isInstanceOf(UniversalSelector.class);
        }

        @Test
        @DisplayName("parses Panel * as DescendantSelector(TypeSelector, UniversalSelector)")
        void parsesDescendantUniversal() {
            Selector selector = SelectorParser.parse("Panel *");

            assertThat(selector).isInstanceOf(DescendantSelector.class);
            DescendantSelector desc = (DescendantSelector) selector;
            assertThat(desc.ancestor()).isInstanceOf(TypeSelector.class);
            assertThat(((TypeSelector) desc.ancestor()).typeName()).isEqualTo("Panel");
            assertThat(desc.descendant()).isInstanceOf(UniversalSelector.class);
        }

        @Test
        @DisplayName("parses .class > * as ChildSelector(ClassSelector, UniversalSelector)")
        void parsesClassChildUniversal() {
            Selector selector = SelectorParser.parse(".container > *");

            assertThat(selector).isInstanceOf(ChildSelector.class);
            ChildSelector child = (ChildSelector) selector;
            assertThat(child.parent()).isInstanceOf(ClassSelector.class);
            assertThat(child.child()).isInstanceOf(UniversalSelector.class);
        }

        @Test
        @DisplayName("parses #id * as DescendantSelector(IdSelector, UniversalSelector)")
        void parsesIdDescendantUniversal() {
            Selector selector = SelectorParser.parse("#sidebar *");

            assertThat(selector).isInstanceOf(DescendantSelector.class);
            DescendantSelector desc = (DescendantSelector) selector;
            assertThat(desc.ancestor()).isInstanceOf(IdSelector.class);
            assertThat(desc.descendant()).isInstanceOf(UniversalSelector.class);
        }

        @Test
        @DisplayName("Panel > * matches direct child of Panel")
        void childUniversalMatchesDirectChild() {
            Selector selector = SelectorParser.parse("Panel > *");
            TestElement parent = new TestElement("Panel", null, Collections.emptySet());
            TestElement child = new TestElement("Button", null, Collections.emptySet());

            assertThat(selector.matches(child, PseudoClassState.NONE, Collections.singletonList(parent))).isTrue();
        }

        @Test
        @DisplayName("Panel > * does not match grandchild")
        void childUniversalDoesNotMatchGrandchild() {
            Selector selector = SelectorParser.parse("Panel > *");
            TestElement grandparent = new TestElement("Panel", null, Collections.emptySet());
            TestElement parent = new TestElement("Row", null, Collections.emptySet());
            TestElement child = new TestElement("Button", null, Collections.emptySet());

            List<Styleable> ancestors = Arrays.asList(grandparent, parent);
            assertThat(selector.matches(child, PseudoClassState.NONE, ancestors)).isFalse();
        }

        @Test
        @DisplayName("Panel * matches any descendant")
        void descendantUniversalMatchesAnyDescendant() {
            Selector selector = SelectorParser.parse("Panel *");
            TestElement grandparent = new TestElement("Panel", null, Collections.emptySet());
            TestElement parent = new TestElement("Row", null, Collections.emptySet());
            TestElement child = new TestElement("Button", null, Collections.emptySet());

            // Direct child
            assertThat(selector.matches(parent, PseudoClassState.NONE,
                    Collections.singletonList(grandparent))).isTrue();

            // Grandchild
            List<Styleable> ancestors = Arrays.asList(grandparent, parent);
            assertThat(selector.matches(child, PseudoClassState.NONE, ancestors)).isTrue();
        }
    }

    @Nested
    @DisplayName("Complex selectors")
    class ComplexSelectorTests {

        @Test
        @DisplayName("parses complex selector with multiple combinators")
        void parsesComplexSelector() {
            Selector selector = SelectorParser.parse("Panel .content > Button.primary");

            assertThat(selector).isInstanceOf(ChildSelector.class);
        }

        @Test
        @DisplayName("parses selector with type, class, pseudo-class")
        void parsesTypClassPseudo() {
            Selector selector = SelectorParser.parse("Button.primary:focus");

            assertThat(selector).isInstanceOf(CompoundSelector.class);
        }
    }

    @Nested
    @DisplayName("Error handling")
    class ErrorHandlingTests {

        @Test
        @DisplayName("throws on null selector")
        void throwsOnNull() {
            assertThatThrownBy(() -> SelectorParser.parse(null))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("throws on empty selector")
        void throwsOnEmpty() {
            assertThatThrownBy(() -> SelectorParser.parse(""))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("throws on invalid selector")
        void throwsOnInvalid() {
            assertThatThrownBy(() -> SelectorParser.parse(">>>"))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    // Helper methods and classes

    private static Set<String> setOf(String... values) {
        return new HashSet<>(Arrays.asList(values));
    }

    /**
     * Test implementation of Styleable for testing selectors.
     */
    private static class TestElement implements Styleable {
        private final String type;
        private final String id;
        private final Set<String> classes;
        private final Map<String, String> attributes = new HashMap<>();
        private Styleable parent;

        TestElement(String type, String id, Set<String> classes) {
            this.type = type;
            this.id = id;
            this.classes = classes;
        }

        void setAttribute(String name, String value) {
            attributes.put(name, value);
        }

        void setParent(Styleable parent) {
            this.parent = parent;
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
            return Optional.ofNullable(parent);
        }

        @Override
        public Map<String, String> styleAttributes() {
            return attributes;
        }
    }
}
