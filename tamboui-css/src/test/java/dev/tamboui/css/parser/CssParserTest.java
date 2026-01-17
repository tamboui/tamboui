/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.css.parser;

import dev.tamboui.css.model.PropertyValue;
import dev.tamboui.css.model.Rule;
import dev.tamboui.css.model.Stylesheet;
import dev.tamboui.css.selector.AttributeSelector;
import dev.tamboui.css.selector.ChildSelector;
import dev.tamboui.css.selector.ClassSelector;
import dev.tamboui.css.selector.CompoundSelector;
import dev.tamboui.css.selector.DescendantSelector;
import dev.tamboui.css.selector.IdSelector;
import dev.tamboui.css.selector.PseudoClassSelector;
import dev.tamboui.css.selector.TypeSelector;
import dev.tamboui.css.selector.UniversalSelector;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CssParserTest {

    @Test
    void parsesEmptyStylesheet() {
        Stylesheet stylesheet = CssParser.parse("");

        assertThat(stylesheet.rules()).isEmpty();
        assertThat(stylesheet.variables()).isEmpty();
    }

    @Test
    void parsesVariable() {
        Stylesheet stylesheet = CssParser.parse("$primary: #3498db;");

        assertThat(stylesheet.variables()).containsEntry("primary", "#3498db");
        assertThat(stylesheet.rules()).isEmpty();
    }

    @Test
    void parsesMultipleVariables() {
        String css = "$primary: blue;\n$error: red;";
        Stylesheet stylesheet = CssParser.parse(css);

        assertThat(stylesheet.variables())
                .containsEntry("primary", "blue")
                .containsEntry("error", "red");
    }

    @Test
    void parsesTypeSelector() {
        Stylesheet stylesheet = CssParser.parse("Panel { color: red; }");

        assertThat(stylesheet.rules()).hasSize(1);
        Rule rule = stylesheet.rules().get(0);
        assertThat(rule.selector()).isInstanceOf(TypeSelector.class);
        assertThat(((TypeSelector) rule.selector()).typeName()).isEqualTo("Panel");
    }

    @Test
    void parsesIdSelector() {
        Stylesheet stylesheet = CssParser.parse("#sidebar { color: blue; }");

        assertThat(stylesheet.rules()).hasSize(1);
        Rule rule = stylesheet.rules().get(0);
        assertThat(rule.selector()).isInstanceOf(IdSelector.class);
        assertThat(((IdSelector) rule.selector()).id()).isEqualTo("sidebar");
    }

    @Test
    void parsesClassSelector() {
        Stylesheet stylesheet = CssParser.parse(".error { color: red; }");

        assertThat(stylesheet.rules()).hasSize(1);
        Rule rule = stylesheet.rules().get(0);
        assertThat(rule.selector()).isInstanceOf(ClassSelector.class);
        assertThat(((ClassSelector) rule.selector()).className()).isEqualTo("error");
    }

    @Test
    void parsesUniversalSelector() {
        Stylesheet stylesheet = CssParser.parse("* { margin: 0; }");

        assertThat(stylesheet.rules()).hasSize(1);
        Rule rule = stylesheet.rules().get(0);
        assertThat(rule.selector()).isEqualTo(UniversalSelector.INSTANCE);
    }

    @Test
    void parsesPseudoClassSelector() {
        Stylesheet stylesheet = CssParser.parse("Panel:focus { color: yellow; }");

        assertThat(stylesheet.rules()).hasSize(1);
        Rule rule = stylesheet.rules().get(0);
        assertThat(rule.selector()).isInstanceOf(CompoundSelector.class);
        CompoundSelector compound = (CompoundSelector) rule.selector();
        assertThat(compound.parts()).hasSize(2);
        assertThat(compound.parts().get(1)).isInstanceOf(PseudoClassSelector.class);
    }

    @Test
    void parsesCompoundSelector() {
        Stylesheet stylesheet = CssParser.parse("Panel.primary { color: blue; }");

        assertThat(stylesheet.rules()).hasSize(1);
        Rule rule = stylesheet.rules().get(0);
        assertThat(rule.selector()).isInstanceOf(CompoundSelector.class);
        CompoundSelector compound = (CompoundSelector) rule.selector();
        assertThat(compound.parts()).hasSize(2);
        assertThat(compound.parts().get(0)).isInstanceOf(TypeSelector.class);
        assertThat(compound.parts().get(1)).isInstanceOf(ClassSelector.class);
    }

    @Test
    void parsesDescendantSelector() {
        Stylesheet stylesheet = CssParser.parse("Panel Button { color: white; }");

        assertThat(stylesheet.rules()).hasSize(1);
        Rule rule = stylesheet.rules().get(0);
        assertThat(rule.selector()).isInstanceOf(DescendantSelector.class);
        DescendantSelector desc = (DescendantSelector) rule.selector();
        assertThat(desc.ancestor()).isInstanceOf(TypeSelector.class);
        assertThat(desc.descendant()).isInstanceOf(TypeSelector.class);
    }

    @Test
    void whitespaceDistinguishesCompoundFromDescendant() {
        // Text.muted - compound selector (Text element WITH class muted)
        Stylesheet compound = CssParser.parse("Text.muted { color: gray; }");
        assertThat(compound.rules()).hasSize(1);
        assertThat(compound.rules().get(0).selector()).isInstanceOf(CompoundSelector.class);
        CompoundSelector compoundSel = (CompoundSelector) compound.rules().get(0).selector();
        assertThat(compoundSel.parts()).hasSize(2);
        assertThat(compoundSel.parts().get(0)).isInstanceOf(TypeSelector.class);
        assertThat(compoundSel.parts().get(1)).isInstanceOf(ClassSelector.class);

        // Text .muted - descendant selector (element with class muted INSIDE Text)
        Stylesheet descendant1 = CssParser.parse("Text .muted { color: gray; }");
        assertThat(descendant1.rules()).hasSize(1);
        assertThat(descendant1.rules().get(0).selector()).isInstanceOf(DescendantSelector.class);
        DescendantSelector desc1 = (DescendantSelector) descendant1.rules().get(0).selector();
        assertThat(desc1.ancestor()).isInstanceOf(TypeSelector.class);
        assertThat(desc1.descendant()).isInstanceOf(ClassSelector.class);

        // .muted Text - descendant selector (Text element INSIDE element with class muted)
        Stylesheet descendant2 = CssParser.parse(".muted Text { color: gray; }");
        assertThat(descendant2.rules()).hasSize(1);
        assertThat(descendant2.rules().get(0).selector()).isInstanceOf(DescendantSelector.class);
        DescendantSelector desc2 = (DescendantSelector) descendant2.rules().get(0).selector();
        assertThat(desc2.ancestor()).isInstanceOf(ClassSelector.class);
        assertThat(desc2.descendant()).isInstanceOf(TypeSelector.class);
    }

    @Test
    void parsesChildSelector() {
        Stylesheet stylesheet = CssParser.parse("Panel > Button { color: gray; }");

        assertThat(stylesheet.rules()).hasSize(1);
        Rule rule = stylesheet.rules().get(0);
        assertThat(rule.selector()).isInstanceOf(ChildSelector.class);
        ChildSelector child = (ChildSelector) rule.selector();
        assertThat(child.parent()).isInstanceOf(TypeSelector.class);
        assertThat(child.child()).isInstanceOf(TypeSelector.class);
    }

    @Test
    void parsesDeclarations() {
        Stylesheet stylesheet = CssParser.parse("Panel { color: red; background: blue; }");

        Rule rule = stylesheet.rules().get(0);
        assertThat(rule.declarations()).hasSize(2);
        assertThat(rule.declarations().get("color").raw()).isEqualTo("red");
        assertThat(rule.declarations().get("background").raw()).isEqualTo("blue");
    }

    @Test
    void parsesImportantDeclaration() {
        Stylesheet stylesheet = CssParser.parse("Panel { color: red !important; }");

        Rule rule = stylesheet.rules().get(0);
        PropertyValue color = rule.declarations().get("color");
        assertThat(color.raw()).isEqualTo("red");
        assertThat(color.important()).isTrue();
    }

    @Test
    void parsesVariableReference() {
        String css = "$primary: blue;\nPanel { color: $primary; }";
        Stylesheet stylesheet = CssParser.parse(css);

        assertThat(stylesheet.variables()).containsEntry("primary", "blue");
        Rule rule = stylesheet.rules().get(0);
        assertThat(rule.declarations().get("color").raw()).isEqualTo("$primary");
    }

    @Test
    void parsesNestedRuleWithAmpersand() {
        String css = "Panel { color: red; &:focus { color: yellow; } }";
        Stylesheet stylesheet = CssParser.parse(css);

        assertThat(stylesheet.rules()).hasSize(2);
        // First rule: Panel { color: red }
        Rule rule1 = stylesheet.rules().get(0);
        assertThat(rule1.selector()).isInstanceOf(TypeSelector.class);
        assertThat(rule1.declarations()).containsKey("color");

        // Second rule: Panel:focus { color: yellow }
        Rule rule2 = stylesheet.rules().get(1);
        assertThat(rule2.selector()).isInstanceOf(CompoundSelector.class);
    }

    @Test
    void parsesComplexValue() {
        Stylesheet stylesheet = CssParser.parse("Panel { border: 1 2 3 4; }");

        Rule rule = stylesheet.rules().get(0);
        assertThat(rule.declarations().get("border").raw()).isEqualTo("1 2 3 4");
    }

    @Test
    void parsesRgbFunction() {
        Stylesheet stylesheet = CssParser.parse("Panel { color: rgb(255, 0, 0); }");

        Rule rule = stylesheet.rules().get(0);
        assertThat(rule.declarations().get("color").raw()).isEqualTo("rgb(255, 0, 0)");
    }

    @Test
    void parsesMultipleRules() {
        String css = "Panel { color: red; }\nButton { color: blue; }";
        Stylesheet stylesheet = CssParser.parse(css);

        assertThat(stylesheet.rules()).hasSize(2);
    }

    @Test
    void throwsOnMissingOpenBrace() {
        assertThatThrownBy(() -> CssParser.parse("Panel color: red; }"))
                .isInstanceOf(CssParseException.class)
                .hasMessageContaining("Expected '{'");
    }

    @Test
    void throwsOnMissingCloseBrace() {
        assertThatThrownBy(() -> CssParser.parse("Panel { color: red;"))
                .isInstanceOf(CssParseException.class)
                .hasMessageContaining("Expected '}'");
    }

    @Test
    void parsesDeclarationWithoutSemicolon() {
        // Last declaration before } doesn't require semicolon
        Stylesheet stylesheet = CssParser.parse("Panel { color: red }");

        Rule rule = stylesheet.rules().get(0);
        assertThat(rule.declarations().get("color").raw()).isEqualTo("red");
    }

    @Test
    void handlesComments() {
        String css = "/* Header styles */\nPanel { /* text color */ color: red; }";
        Stylesheet stylesheet = CssParser.parse(css);

        assertThat(stylesheet.rules()).hasSize(1);
        assertThat(stylesheet.rules().get(0).declarations()).containsKey("color");
    }

    @Test
    void preservesSourceOrder() {
        String css = "Panel { color: red; }\nButton { color: blue; }\nLabel { color: green; }";
        Stylesheet stylesheet = CssParser.parse(css);

        assertThat(stylesheet.rules()).hasSize(3);
        assertThat(stylesheet.rules().get(0).sourceOrder()).isEqualTo(0);
        assertThat(stylesheet.rules().get(1).sourceOrder()).isEqualTo(1);
        assertThat(stylesheet.rules().get(2).sourceOrder()).isEqualTo(2);
    }

    @Test
    void parsesFunctionalPseudoClassNthChildEven() {
        Stylesheet stylesheet = CssParser.parse("ListItem:nth-child(even) { background: gray; }");

        assertThat(stylesheet.rules()).hasSize(1);
        Rule rule = stylesheet.rules().get(0);
        assertThat(rule.selector()).isInstanceOf(CompoundSelector.class);
        CompoundSelector compound = (CompoundSelector) rule.selector();
        assertThat(compound.parts()).hasSize(2);
        assertThat(compound.parts().get(0)).isInstanceOf(TypeSelector.class);
        assertThat(compound.parts().get(1)).isInstanceOf(PseudoClassSelector.class);
        PseudoClassSelector pseudo = (PseudoClassSelector) compound.parts().get(1);
        assertThat(pseudo.pseudoClass()).isEqualTo("nth-child(even)");
    }

    @Test
    void parsesFunctionalPseudoClassNthChildOdd() {
        Stylesheet stylesheet = CssParser.parse("ListItem:nth-child(odd) { background: white; }");

        assertThat(stylesheet.rules()).hasSize(1);
        Rule rule = stylesheet.rules().get(0);
        CompoundSelector compound = (CompoundSelector) rule.selector();
        PseudoClassSelector pseudo = (PseudoClassSelector) compound.parts().get(1);
        assertThat(pseudo.pseudoClass()).isEqualTo("nth-child(odd)");
    }

    @Test
    void parsesFunctionalPseudoClassStandalone() {
        Stylesheet stylesheet = CssParser.parse(":nth-child(even) { color: red; }");

        assertThat(stylesheet.rules()).hasSize(1);
        Rule rule = stylesheet.rules().get(0);
        assertThat(rule.selector()).isInstanceOf(PseudoClassSelector.class);
        PseudoClassSelector pseudo = (PseudoClassSelector) rule.selector();
        assertThat(pseudo.pseudoClass()).isEqualTo("nth-child(even)");
    }

    @Test
    void parsesFunctionalPseudoClassWithNumericArgument() {
        Stylesheet stylesheet = CssParser.parse("ListItem:nth-child(3) { color: blue; }");

        assertThat(stylesheet.rules()).hasSize(1);
        Rule rule = stylesheet.rules().get(0);
        CompoundSelector compound = (CompoundSelector) rule.selector();
        PseudoClassSelector pseudo = (PseudoClassSelector) compound.parts().get(1);
        assertThat(pseudo.pseudoClass()).isEqualTo("nth-child(3)");
    }

    // ═══════════════════════════════════════════════════════════════
    // Selector Lists (comma-separated selectors)
    // ═══════════════════════════════════════════════════════════════

    @Test
    void parsesSelectorListWithTwoSelectors() {
        Stylesheet stylesheet = CssParser.parse(".foo, .bar { color: red; }");

        assertThat(stylesheet.rules()).hasSize(2);

        Rule rule1 = stylesheet.rules().get(0);
        assertThat(rule1.selector()).isInstanceOf(ClassSelector.class);
        assertThat(((ClassSelector) rule1.selector()).className()).isEqualTo("foo");
        assertThat(rule1.declarations().get("color").raw()).isEqualTo("red");

        Rule rule2 = stylesheet.rules().get(1);
        assertThat(rule2.selector()).isInstanceOf(ClassSelector.class);
        assertThat(((ClassSelector) rule2.selector()).className()).isEqualTo("bar");
        assertThat(rule2.declarations().get("color").raw()).isEqualTo("red");
    }

    @Test
    void parsesSelectorListWithThreeSelectors() {
        Stylesheet stylesheet = CssParser.parse(".error, .warning, .info { border-type: rounded; }");

        assertThat(stylesheet.rules()).hasSize(3);
        assertThat(((ClassSelector) stylesheet.rules().get(0).selector()).className()).isEqualTo("error");
        assertThat(((ClassSelector) stylesheet.rules().get(1).selector()).className()).isEqualTo("warning");
        assertThat(((ClassSelector) stylesheet.rules().get(2).selector()).className()).isEqualTo("info");
    }

    @Test
    void parsesSelectorListWithComplexSelectors() {
        Stylesheet stylesheet = CssParser.parse("Panel.primary, Button > Label, #sidebar { color: blue; }");

        assertThat(stylesheet.rules()).hasSize(3);

        // Panel.primary - compound selector
        assertThat(stylesheet.rules().get(0).selector()).isInstanceOf(CompoundSelector.class);

        // Button > Label - child selector
        assertThat(stylesheet.rules().get(1).selector()).isInstanceOf(ChildSelector.class);

        // #sidebar - id selector
        assertThat(stylesheet.rules().get(2).selector()).isInstanceOf(IdSelector.class);
    }

    @Test
    void selectorListSharesSameSourceOrder() {
        Stylesheet stylesheet = CssParser.parse(".foo, .bar { color: blue; }");

        assertThat(stylesheet.rules()).hasSize(2);
        // All rules from a selector list should share the same source order
        assertThat(stylesheet.rules().get(0).sourceOrder())
            .isEqualTo(stylesheet.rules().get(1).sourceOrder());
    }

    @Test
    void parsesNestedRulesWithSelectorList() {
        Stylesheet stylesheet = CssParser.parse("Panel { &.foo, &.bar { color: yellow; } }");

        assertThat(stylesheet.rules()).hasSize(2);

        // Panel.foo and Panel.bar
        Rule rule1 = stylesheet.rules().get(0);
        Rule rule2 = stylesheet.rules().get(1);

        assertThat(rule1.selector()).isInstanceOf(CompoundSelector.class);
        assertThat(rule2.selector()).isInstanceOf(CompoundSelector.class);

        // Both should have the same declarations
        assertThat(rule1.declarations().get("color").raw()).isEqualTo("yellow");
        assertThat(rule2.declarations().get("color").raw()).isEqualTo("yellow");
    }

    @Test
    void parsesMixedTypesInSelectorList() {
        Stylesheet stylesheet = CssParser.parse("Panel, .error, #main { padding: 1; }");

        assertThat(stylesheet.rules()).hasSize(3);
        assertThat(stylesheet.rules().get(0).selector()).isInstanceOf(TypeSelector.class);
        assertThat(stylesheet.rules().get(1).selector()).isInstanceOf(ClassSelector.class);
        assertThat(stylesheet.rules().get(2).selector()).isInstanceOf(IdSelector.class);
    }

    // ═══════════════════════════════════════════════════════════════
    // Attribute Selectors
    // ═══════════════════════════════════════════════════════════════

    @Test
    void parsesAttributeExistsSelector() {
        Stylesheet stylesheet = CssParser.parse("Panel[title] { color: red; }");

        assertThat(stylesheet.rules()).hasSize(1);
        Rule rule = stylesheet.rules().get(0);
        assertThat(rule.selector()).isInstanceOf(CompoundSelector.class);
        CompoundSelector compound = (CompoundSelector) rule.selector();
        assertThat(compound.parts()).hasSize(2);
        assertThat(compound.parts().get(0)).isInstanceOf(TypeSelector.class);
        assertThat(compound.parts().get(1)).isInstanceOf(AttributeSelector.class);

        AttributeSelector attrSelector = (AttributeSelector) compound.parts().get(1);
        assertThat(attrSelector.attribute()).isEqualTo("title");
        assertThat(attrSelector.operator()).isEqualTo(AttributeSelector.Operator.EXISTS);
    }

    @Test
    void parsesAttributeEqualsSelector() {
        Stylesheet stylesheet = CssParser.parse("Panel[title=\"Test Tree\"] { color: cyan; }");

        assertThat(stylesheet.rules()).hasSize(1);
        Rule rule = stylesheet.rules().get(0);
        CompoundSelector compound = (CompoundSelector) rule.selector();
        AttributeSelector attrSelector = (AttributeSelector) compound.parts().get(1);

        assertThat(attrSelector.attribute()).isEqualTo("title");
        assertThat(attrSelector.operator()).isEqualTo(AttributeSelector.Operator.EQUALS);
        assertThat(attrSelector.value()).isEqualTo("Test Tree");
    }

    @Test
    void parsesAttributeStartsWithSelector() {
        Stylesheet stylesheet = CssParser.parse("[title^=\"Test\"] { color: yellow; }");

        assertThat(stylesheet.rules()).hasSize(1);
        Rule rule = stylesheet.rules().get(0);
        assertThat(rule.selector()).isInstanceOf(AttributeSelector.class);
        AttributeSelector attrSelector = (AttributeSelector) rule.selector();

        assertThat(attrSelector.attribute()).isEqualTo("title");
        assertThat(attrSelector.operator()).isEqualTo(AttributeSelector.Operator.STARTS_WITH);
        assertThat(attrSelector.value()).isEqualTo("Test");
    }

    @Test
    void parsesAttributeEndsWithSelector() {
        Stylesheet stylesheet = CssParser.parse("[title$=\"Output\"] { color: green; }");

        assertThat(stylesheet.rules()).hasSize(1);
        AttributeSelector attrSelector = (AttributeSelector) stylesheet.rules().get(0).selector();

        assertThat(attrSelector.attribute()).isEqualTo("title");
        assertThat(attrSelector.operator()).isEqualTo(AttributeSelector.Operator.ENDS_WITH);
        assertThat(attrSelector.value()).isEqualTo("Output");
    }

    @Test
    void parsesAttributeContainsSelector() {
        Stylesheet stylesheet = CssParser.parse("[title*=\"Tree\"] { color: blue; }");

        assertThat(stylesheet.rules()).hasSize(1);
        AttributeSelector attrSelector = (AttributeSelector) stylesheet.rules().get(0).selector();

        assertThat(attrSelector.attribute()).isEqualTo("title");
        assertThat(attrSelector.operator()).isEqualTo(AttributeSelector.Operator.CONTAINS);
        assertThat(attrSelector.value()).isEqualTo("Tree");
    }

    @Test
    void parsesAttributeSelectorWithUnquotedValue() {
        Stylesheet stylesheet = CssParser.parse("[title=Test] { color: red; }");

        assertThat(stylesheet.rules()).hasSize(1);
        AttributeSelector attrSelector = (AttributeSelector) stylesheet.rules().get(0).selector();

        assertThat(attrSelector.attribute()).isEqualTo("title");
        assertThat(attrSelector.operator()).isEqualTo(AttributeSelector.Operator.EQUALS);
        assertThat(attrSelector.value()).isEqualTo("Test");
    }

    @Test
    void parsesAttributeSelectorInSelectorList() {
        Stylesheet stylesheet = CssParser.parse("Panel[title=\"A\"], Panel[title=\"B\"] { color: green; }");

        assertThat(stylesheet.rules()).hasSize(2);

        CompoundSelector compound1 = (CompoundSelector) stylesheet.rules().get(0).selector();
        AttributeSelector attr1 = (AttributeSelector) compound1.parts().get(1);
        assertThat(attr1.value()).isEqualTo("A");

        CompoundSelector compound2 = (CompoundSelector) stylesheet.rules().get(1).selector();
        AttributeSelector attr2 = (AttributeSelector) compound2.parts().get(1);
        assertThat(attr2.value()).isEqualTo("B");
    }

    @Test
    void attributeSelectorToCss() {
        Stylesheet stylesheet = CssParser.parse("[title=\"Test\"] { color: red; }");
        AttributeSelector attrSelector = (AttributeSelector) stylesheet.rules().get(0).selector();

        assertThat(attrSelector.toCss()).isEqualTo("[title=\"Test\"]");
    }

    @Test
    void attributeSelectorExistsToCss() {
        Stylesheet stylesheet = CssParser.parse("[title] { color: red; }");
        AttributeSelector attrSelector = (AttributeSelector) stylesheet.rules().get(0).selector();

        assertThat(attrSelector.toCss()).isEqualTo("[title]");
    }
}
