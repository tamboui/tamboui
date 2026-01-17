/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.css.selector;

import dev.tamboui.css.Styleable;
import dev.tamboui.css.cascade.PseudoClassState;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class SelectorTest {

    @Test
    void typeSelectorMatchesElement() {
        TypeSelector selector = new TypeSelector("Panel");
        Styleable element = createStyleable("Panel", null, Collections.<String>emptySet());

        assertThat(selector.matches(element, PseudoClassState.NONE, Collections.<Styleable>emptyList())).isTrue();
    }

    @Test
    void typeSelectorDoesNotMatchDifferentType() {
        TypeSelector selector = new TypeSelector("Panel");
        Styleable element = createStyleable("Button", null, Collections.<String>emptySet());

        assertThat(selector.matches(element, PseudoClassState.NONE, Collections.<Styleable>emptyList())).isFalse();
    }

    @Test
    void typeSelectorMatchesSubclass() {
        TypeSelector panelSelector = new TypeSelector("BasePanel");
        MyPanel myPanel = new MyPanel();

        // MyPanel extends BasePanel, so Panel selector should match MyPanel
        assertThat(panelSelector.matches(myPanel, PseudoClassState.NONE, Collections.<Styleable>emptyList())).isTrue();
    }

    @Test
    void typeSelectorMatchesExactTypeForSubclass() {
        TypeSelector myPanelSelector = new TypeSelector("MyPanel");
        MyPanel myPanel = new MyPanel();

        // MyPanel selector should also match MyPanel
        assertThat(myPanelSelector.matches(myPanel, PseudoClassState.NONE, Collections.<Styleable>emptyList())).isTrue();
    }

    @Test
    void styleTypesReturnsCorrectOrder() {
        MyPanel myPanel = new MyPanel();
        List<String> types = Styleable.styleTypesOf(myPanel);

        // Order should be parent first (lower precedence), child last (higher precedence)
        assertThat(types).containsExactly("BasePanel", "MyPanel");
    }

    @Test
    void styleTypesForSimpleElementReturnsOnlyOneType() {
        Styleable element = createStyleable("Panel", null, Collections.<String>emptySet());
        List<String> types = Styleable.styleTypesOf(element);

        // TestStyleable has no Styleable parent classes
        assertThat(types).containsExactly("Panel");
    }

    @Test
    void typeSelectorSpecificity() {
        TypeSelector selector = new TypeSelector("Panel");
        assertThat(selector.specificity()).isEqualTo(1);
    }

    @Test
    void idSelectorMatchesElement() {
        IdSelector selector = new IdSelector("sidebar");
        Styleable element = createStyleable("Panel", "sidebar", Collections.<String>emptySet());

        assertThat(selector.matches(element, PseudoClassState.NONE, Collections.<Styleable>emptyList())).isTrue();
    }

    @Test
    void idSelectorDoesNotMatchWithoutId() {
        IdSelector selector = new IdSelector("sidebar");
        Styleable element = createStyleable("Panel", null, Collections.<String>emptySet());

        assertThat(selector.matches(element, PseudoClassState.NONE, Collections.<Styleable>emptyList())).isFalse();
    }

    @Test
    void idSelectorSpecificity() {
        IdSelector selector = new IdSelector("sidebar");
        assertThat(selector.specificity()).isEqualTo(100);
    }

    @Test
    void classSelectorMatchesElement() {
        ClassSelector selector = new ClassSelector("primary");
        Styleable element = createStyleable("Panel", null, new HashSet<>(Arrays.asList("primary", "large")));

        assertThat(selector.matches(element, PseudoClassState.NONE, Collections.<Styleable>emptyList())).isTrue();
    }

    @Test
    void classSelectorDoesNotMatchWithoutClass() {
        ClassSelector selector = new ClassSelector("primary");
        Styleable element = createStyleable("Panel", null, Collections.<String>emptySet());

        assertThat(selector.matches(element, PseudoClassState.NONE, Collections.<Styleable>emptyList())).isFalse();
    }

    @Test
    void classSelectorSpecificity() {
        ClassSelector selector = new ClassSelector("primary");
        assertThat(selector.specificity()).isEqualTo(10);
    }

    @Test
    void universalSelectorMatchesAnyElement() {
        Styleable element1 = createStyleable("Panel", null, Collections.<String>emptySet());
        Styleable element2 = createStyleable("Button", "id", new HashSet<>(Arrays.asList("class")));

        assertThat(UniversalSelector.INSTANCE.matches(element1, PseudoClassState.NONE, Collections.<Styleable>emptyList())).isTrue();
        assertThat(UniversalSelector.INSTANCE.matches(element2, PseudoClassState.NONE, Collections.<Styleable>emptyList())).isTrue();
    }

    @Test
    void universalSelectorSpecificity() {
        assertThat(UniversalSelector.INSTANCE.specificity()).isEqualTo(0);
    }

    @Test
    void pseudoClassSelectorMatchesFocused() {
        PseudoClassSelector selector = new PseudoClassSelector("focus");
        Styleable element = createStyleable("Panel", null, Collections.<String>emptySet());

        assertThat(selector.matches(element, PseudoClassState.ofFocused(), Collections.<Styleable>emptyList())).isTrue();
        assertThat(selector.matches(element, PseudoClassState.NONE, Collections.<Styleable>emptyList())).isFalse();
    }

    @Test
    void pseudoClassSelectorMatchesHovered() {
        PseudoClassSelector selector = new PseudoClassSelector("hover");
        Styleable element = createStyleable("Panel", null, Collections.<String>emptySet());

        assertThat(selector.matches(element, PseudoClassState.ofHovered(), Collections.<Styleable>emptyList())).isTrue();
    }

    @Test
    void pseudoClassSelectorSpecificity() {
        PseudoClassSelector selector = new PseudoClassSelector("focus");
        assertThat(selector.specificity()).isEqualTo(10);
    }

    @Test
    void compoundSelectorMatchesAllParts() {
        List<Selector> parts = Arrays.<Selector>asList(
                new TypeSelector("Panel"),
                new ClassSelector("primary")
        );
        CompoundSelector selector = new CompoundSelector(parts);

        Styleable matching = createStyleable("Panel", null, new HashSet<>(Arrays.asList("primary")));
        Styleable wrongType = createStyleable("Button", null, new HashSet<>(Arrays.asList("primary")));
        Styleable wrongClass = createStyleable("Panel", null, new HashSet<>(Arrays.asList("secondary")));

        assertThat(selector.matches(matching, PseudoClassState.NONE, Collections.<Styleable>emptyList())).isTrue();
        assertThat(selector.matches(wrongType, PseudoClassState.NONE, Collections.<Styleable>emptyList())).isFalse();
        assertThat(selector.matches(wrongClass, PseudoClassState.NONE, Collections.<Styleable>emptyList())).isFalse();
    }

    @Test
    void compoundSelectorSpecificity() {
        List<Selector> parts = Arrays.<Selector>asList(
                new TypeSelector("Panel"),
                new ClassSelector("primary"),
                new IdSelector("main")
        );
        CompoundSelector selector = new CompoundSelector(parts);

        // 100 (id) + 10 (class) + 1 (type) = 111
        assertThat(selector.specificity()).isEqualTo(111);
    }

    @Test
    void descendantSelectorMatchesNestedElement() {
        DescendantSelector selector = new DescendantSelector(
                new TypeSelector("Panel"),
                new TypeSelector("Button")
        );

        Styleable panel = createStyleable("Panel", null, Collections.<String>emptySet());
        Styleable button = createStyleable("Button", null, Collections.<String>emptySet());
        List<Styleable> ancestors = Arrays.asList(panel);

        assertThat(selector.matches(button, PseudoClassState.NONE, ancestors)).isTrue();
    }

    @Test
    void descendantSelectorMatchesDeeplyNestedElement() {
        DescendantSelector selector = new DescendantSelector(
                new TypeSelector("Panel"),
                new TypeSelector("Button")
        );

        Styleable panel = createStyleable("Panel", null, Collections.<String>emptySet());
        Styleable container = createStyleable("Container", null, Collections.<String>emptySet());
        Styleable button = createStyleable("Button", null, Collections.<String>emptySet());
        List<Styleable> ancestors = Arrays.asList(panel, container);

        assertThat(selector.matches(button, PseudoClassState.NONE, ancestors)).isTrue();
    }

    @Test
    void descendantSelectorDoesNotMatchWithoutAncestor() {
        DescendantSelector selector = new DescendantSelector(
                new TypeSelector("Panel"),
                new TypeSelector("Button")
        );

        Styleable button = createStyleable("Button", null, Collections.<String>emptySet());

        assertThat(selector.matches(button, PseudoClassState.NONE, Collections.<Styleable>emptyList())).isFalse();
    }

    @Test
    void whitespaceDistinguishesCompoundFromDescendantMatching() {
        // Setup: Text element with class "muted"
        Styleable textWithMuted = createStyleable("Text", null, new HashSet<>(Arrays.asList("muted")));
        // Setup: Text element without class "muted"
        Styleable textWithoutMuted = createStyleable("Text", null, Collections.<String>emptySet());
        // Setup: Span element with class "muted"
        Styleable spanWithMuted = createStyleable("Span", null, new HashSet<>(Arrays.asList("muted")));
        // Setup: Container with class "muted"
        Styleable containerWithMuted = createStyleable("Container", null, new HashSet<>(Arrays.asList("muted")));

        // Text.muted - compound selector: Text element WITH class muted
        CompoundSelector compoundSelector = new CompoundSelector(Arrays.<Selector>asList(
                new TypeSelector("Text"),
                new ClassSelector("muted")
        ));
        assertThat(compoundSelector.matches(textWithMuted, PseudoClassState.NONE, Collections.<Styleable>emptyList()))
                .as("Text.muted should match Text element with class muted").isTrue();
        assertThat(compoundSelector.matches(textWithoutMuted, PseudoClassState.NONE, Collections.<Styleable>emptyList()))
                .as("Text.muted should NOT match Text element without class muted").isFalse();
        assertThat(compoundSelector.matches(spanWithMuted, PseudoClassState.NONE, Collections.<Styleable>emptyList()))
                .as("Text.muted should NOT match Span element even with class muted").isFalse();

        // Text .muted - descendant selector: element with class muted INSIDE Text
        DescendantSelector descendant1 = new DescendantSelector(
                new TypeSelector("Text"),
                new ClassSelector("muted")
        );
        assertThat(descendant1.matches(spanWithMuted, PseudoClassState.NONE, Arrays.asList(textWithoutMuted)))
                .as("Text .muted should match element with class muted inside Text").isTrue();
        assertThat(descendant1.matches(spanWithMuted, PseudoClassState.NONE, Collections.<Styleable>emptyList()))
                .as("Text .muted should NOT match element with class muted without Text ancestor").isFalse();

        // .muted Text - descendant selector: Text element INSIDE element with class muted
        DescendantSelector descendant2 = new DescendantSelector(
                new ClassSelector("muted"),
                new TypeSelector("Text")
        );
        assertThat(descendant2.matches(textWithoutMuted, PseudoClassState.NONE, Arrays.asList(containerWithMuted)))
                .as(".muted Text should match Text element inside element with class muted").isTrue();
        assertThat(descendant2.matches(textWithoutMuted, PseudoClassState.NONE, Collections.<Styleable>emptyList()))
                .as(".muted Text should NOT match Text element without muted ancestor").isFalse();
    }

    @Test
    void childSelectorMatchesDirectChild() {
        ChildSelector selector = new ChildSelector(
                new TypeSelector("Panel"),
                new TypeSelector("Button")
        );

        Styleable panel = createStyleable("Panel", null, Collections.<String>emptySet());
        Styleable button = createStyleable("Button", null, Collections.<String>emptySet());
        List<Styleable> ancestors = Arrays.asList(panel);

        assertThat(selector.matches(button, PseudoClassState.NONE, ancestors)).isTrue();
    }

    @Test
    void childSelectorDoesNotMatchGrandchild() {
        ChildSelector selector = new ChildSelector(
                new TypeSelector("Panel"),
                new TypeSelector("Button")
        );

        Styleable panel = createStyleable("Panel", null, Collections.<String>emptySet());
        Styleable container = createStyleable("Container", null, Collections.<String>emptySet());
        Styleable button = createStyleable("Button", null, Collections.<String>emptySet());
        List<Styleable> ancestors = Arrays.asList(panel, container);

        assertThat(selector.matches(button, PseudoClassState.NONE, ancestors)).isFalse();
    }

    @Test
    void selectorToCss() {
        assertThat(new TypeSelector("Panel").toCss()).isEqualTo("Panel");
        assertThat(new IdSelector("sidebar").toCss()).isEqualTo("#sidebar");
        assertThat(new ClassSelector("primary").toCss()).isEqualTo(".primary");
        assertThat(UniversalSelector.INSTANCE.toCss()).isEqualTo("*");
        assertThat(new PseudoClassSelector("focus").toCss()).isEqualTo(":focus");

        CompoundSelector compound = new CompoundSelector(Arrays.<Selector>asList(
                new TypeSelector("Panel"),
                new ClassSelector("primary")
        ));
        assertThat(compound.toCss()).isEqualTo("Panel.primary");

        DescendantSelector desc = new DescendantSelector(
                new TypeSelector("Panel"),
                new TypeSelector("Button")
        );
        assertThat(desc.toCss()).isEqualTo("Panel Button");

        ChildSelector child = new ChildSelector(
                new TypeSelector("Panel"),
                new TypeSelector("Button")
        );
        assertThat(child.toCss()).isEqualTo("Panel > Button");
    }

    // ═══════════════════════════════════════════════════════════════
    // Attribute Selectors
    // ═══════════════════════════════════════════════════════════════

    @Test
    void attributeSelectorExistsMatchesElementWithAttribute() {
        AttributeSelector selector = new AttributeSelector("title");
        Styleable elementWithTitle = createStyleableWithAttrs("Panel", null, Collections.<String>emptySet(),
                Collections.singletonMap("title", "Test Tree"));
        Styleable elementWithoutTitle = createStyleable("Panel", null, Collections.<String>emptySet());

        assertThat(selector.matches(elementWithTitle, PseudoClassState.NONE, Collections.<Styleable>emptyList())).isTrue();
        assertThat(selector.matches(elementWithoutTitle, PseudoClassState.NONE, Collections.<Styleable>emptyList())).isFalse();
    }

    @Test
    void attributeSelectorEqualsMatchesExactValue() {
        AttributeSelector selector = new AttributeSelector("title", AttributeSelector.Operator.EQUALS, "Test Tree");
        Styleable matchingElement = createStyleableWithAttrs("Panel", null, Collections.<String>emptySet(),
                Collections.singletonMap("title", "Test Tree"));
        Styleable nonMatchingElement = createStyleableWithAttrs("Panel", null, Collections.<String>emptySet(),
                Collections.singletonMap("title", "Other Title"));

        assertThat(selector.matches(matchingElement, PseudoClassState.NONE, Collections.<Styleable>emptyList())).isTrue();
        assertThat(selector.matches(nonMatchingElement, PseudoClassState.NONE, Collections.<Styleable>emptyList())).isFalse();
    }

    @Test
    void attributeSelectorStartsWithMatchesPrefix() {
        AttributeSelector selector = new AttributeSelector("title", AttributeSelector.Operator.STARTS_WITH, "Test");
        Styleable matchingElement = createStyleableWithAttrs("Panel", null, Collections.<String>emptySet(),
                Collections.singletonMap("title", "Test Tree"));
        Styleable nonMatchingElement = createStyleableWithAttrs("Panel", null, Collections.<String>emptySet(),
                Collections.singletonMap("title", "My Test"));

        assertThat(selector.matches(matchingElement, PseudoClassState.NONE, Collections.<Styleable>emptyList())).isTrue();
        assertThat(selector.matches(nonMatchingElement, PseudoClassState.NONE, Collections.<Styleable>emptyList())).isFalse();
    }

    @Test
    void attributeSelectorEndsWithMatchesSuffix() {
        AttributeSelector selector = new AttributeSelector("title", AttributeSelector.Operator.ENDS_WITH, "Output");
        Styleable matchingElement = createStyleableWithAttrs("Panel", null, Collections.<String>emptySet(),
                Collections.singletonMap("title", "Test Output"));
        Styleable nonMatchingElement = createStyleableWithAttrs("Panel", null, Collections.<String>emptySet(),
                Collections.singletonMap("title", "Output Test"));

        assertThat(selector.matches(matchingElement, PseudoClassState.NONE, Collections.<Styleable>emptyList())).isTrue();
        assertThat(selector.matches(nonMatchingElement, PseudoClassState.NONE, Collections.<Styleable>emptyList())).isFalse();
    }

    @Test
    void attributeSelectorContainsMatchesSubstring() {
        AttributeSelector selector = new AttributeSelector("title", AttributeSelector.Operator.CONTAINS, "Tree");
        Styleable matchingElement = createStyleableWithAttrs("Panel", null, Collections.<String>emptySet(),
                Collections.singletonMap("title", "Test Tree View"));
        Styleable nonMatchingElement = createStyleableWithAttrs("Panel", null, Collections.<String>emptySet(),
                Collections.singletonMap("title", "Test Output"));

        assertThat(selector.matches(matchingElement, PseudoClassState.NONE, Collections.<Styleable>emptyList())).isTrue();
        assertThat(selector.matches(nonMatchingElement, PseudoClassState.NONE, Collections.<Styleable>emptyList())).isFalse();
    }

    @Test
    void attributeSelectorSpecificity() {
        AttributeSelector selector = new AttributeSelector("title", AttributeSelector.Operator.EQUALS, "Test");
        assertThat(selector.specificity()).isEqualTo(10); // Same as class selector
    }

    @Test
    void attributeSelectorToCss() {
        assertThat(new AttributeSelector("title").toCss()).isEqualTo("[title]");
        assertThat(new AttributeSelector("title", AttributeSelector.Operator.EQUALS, "Test").toCss())
                .isEqualTo("[title=\"Test\"]");
        assertThat(new AttributeSelector("title", AttributeSelector.Operator.STARTS_WITH, "Test").toCss())
                .isEqualTo("[title^=\"Test\"]");
        assertThat(new AttributeSelector("title", AttributeSelector.Operator.ENDS_WITH, "Test").toCss())
                .isEqualTo("[title$=\"Test\"]");
        assertThat(new AttributeSelector("title", AttributeSelector.Operator.CONTAINS, "Test").toCss())
                .isEqualTo("[title*=\"Test\"]");
    }

    private Styleable createStyleable(String type, String id, Set<String> classes) {
        return new TestStyleable(type, id, classes, Collections.<String, String>emptyMap());
    }

    private Styleable createStyleableWithAttrs(String type, String id, Set<String> classes, Map<String, String> attrs) {
        return new TestStyleable(type, id, classes, attrs);
    }

    private static class TestStyleable implements Styleable {
        private final String type;
        private final String id;
        private final Set<String> classes;
        private final Map<String, String> attrs;

        TestStyleable(String type, String id, Set<String> classes, Map<String, String> attrs) {
            this.type = type;
            this.id = id;
            this.classes = classes;
            this.attrs = attrs;
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

        @Override
        public Map<String, String> styleAttributes() {
            return attrs;
        }
    }

    /**
     * Base panel class for testing type hierarchy matching.
     */
    private static class BasePanel implements Styleable {
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
    }

    /**
     * Subclass of BasePanel for testing type hierarchy matching.
     * CSS selectors for both "BasePanel" and "MyPanel" should match instances of this class.
     */
    private static class MyPanel extends BasePanel {
        // Inherits all methods from BasePanel
        // styleType() defaults to "MyPanel" (getClass().getSimpleName())
    }
}
