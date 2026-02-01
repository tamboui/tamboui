/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.css.cascade;

import dev.tamboui.css.Styleable;
import dev.tamboui.css.engine.StyleEngine;
import dev.tamboui.widgets.block.BorderType;
import dev.tamboui.style.Style;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

class CascadeResolverTest {

    @Test
    void universalSelectorProvidesBackgroundToAllElements() {
        String css = "* { color: black; background: #eeeeee; }";
        StyleEngine engine = StyleEngine.create();
        engine.addStylesheet("test", css);
        engine.setActiveStylesheet("test");

        // Create a "TextElement" type element (no classes, no id)
        Styleable textElement = createStyleable("TextElement", null, Collections.emptySet());

        CssStyleResolver resolved = engine.resolve(textElement);

        assertThat(resolved.hasProperties()).isTrue();
        assertThat(resolved.foreground()).isPresent();
        assertThat(resolved.background()).isPresent();

        // Verify the resolved style converts to Style correctly
        Style style = resolved.toStyle();
        assertThat(style.fg()).isNotNull();
        assertThat(style.bg()).isNotNull();
    }

    @Test
    void universalSelectorWithVariables() {
        String css = "$bg-primary: #eeeeee;\n" +
                     "$fg-primary: black;\n" +
                     "* { color: $fg-primary; background: $bg-primary; }";
        StyleEngine engine = StyleEngine.create();
        engine.addStylesheet("test", css);
        engine.setActiveStylesheet("test");

        Styleable textElement = createStyleable("TextElement", null, Collections.emptySet());

        CssStyleResolver resolved = engine.resolve(textElement);

        assertThat(resolved.hasProperties()).isTrue();
        assertThat(resolved.foreground()).isPresent();
        assertThat(resolved.background()).isPresent();
    }

    @Test
    void classSelectorOverridesUniversalSelector() {
        String css = "* { color: black; background: white; }\n" +
                     ".error { color: red; }";
        StyleEngine engine = StyleEngine.create();
        engine.addStylesheet("test", css);
        engine.setActiveStylesheet("test");

        Styleable element = createStyleable("TextElement", null,
            new HashSet<>(Collections.singletonList("error")));

        CssStyleResolver resolved = engine.resolve(element);

        // Should have color from .error (red) and background from * (white)
        assertThat(resolved.foreground()).isPresent();
        assertThat(resolved.background()).isPresent();
    }

    @Test
    void classSelectorInheritsBackgroundFromUniversalSelector() {
        // This simulates the CSS Demo's light theme
        String css = "$bg-primary: #eeeeee;\n" +
                     "$fg-primary: black;\n" +
                     "* { color: $fg-primary; background: $bg-primary; }\n" +
                     ".primary { color: blue; text-style: bold; }";
        StyleEngine engine = StyleEngine.create();
        engine.addStylesheet("test", css);
        engine.setActiveStylesheet("test");

        // Element with class "primary" should get:
        // - color: blue (from .primary)
        // - background: #eeeeee (inherited from *)
        // - text-style: bold (from .primary)
        Styleable element = createStyleable("TextElement", null,
            new HashSet<>(Collections.singletonList("primary")));

        CssStyleResolver resolved = engine.resolve(element);

        assertThat(resolved.foreground()).isPresent();
        assertThat(resolved.background()).isPresent();

        // Verify toStyle() preserves both
        Style style = resolved.toStyle();
        assertThat(style.fg()).isPresent();
        assertThat(style.bg()).isPresent();

        System.out.println("Resolved style: " + resolved);
        System.out.println("Style fg: " + style.fg());
        System.out.println("Style bg: " + style.bg());
    }

    @Test
    void plainTextElementGetsBackgroundFromUniversalSelector() {
        // Element with no classes should still get background from *
        String css = "* { color: black; background: #eeeeee; }";
        StyleEngine engine = StyleEngine.create();
        engine.addStylesheet("test", css);
        engine.setActiveStylesheet("test");

        // Plain TextElement with no classes
        Styleable element = createStyleable("TextElement", null, Collections.emptySet());

        CssStyleResolver resolved = engine.resolve(element);

        assertThat(resolved.hasProperties()).isTrue();
        assertThat(resolved.foreground()).isPresent();
        assertThat(resolved.background()).isPresent();

        Style style = resolved.toStyle();
        assertThat(style.bg()).isPresent();
    }

    @Test
    void borderCharsPropertyIsResolved() {
        String css = ".custom-border { border-chars: \"─\" \"─\" \"│\" \"│\" \"┌\" \"┐\" \"└\" \"┘\"; }";
        StyleEngine engine = StyleEngine.create();
        engine.addStylesheet("test", css);
        engine.setActiveStylesheet("test");

        Styleable element = createStyleable("Panel", null,
            new HashSet<>(Collections.singletonList("custom-border")));

        CssStyleResolver resolved = engine.resolve(element);

        assertThat(resolved.borderChars()).isPresent();
        assertThat(resolved.borderChars().get())
            .isEqualTo("\"─\" \"─\" \"│\" \"│\" \"┌\" \"┐\" \"└\" \"┘\"");
    }

    @Test
    void cornersOnlyBorderCharsWithEmptyStrings() {
        String css = ".corners-only { border-chars: \"\" \"\" \"\" \"\" \"┌\" \"┐\" \"└\" \"┘\"; }";
        StyleEngine engine = StyleEngine.create();
        engine.addStylesheet("test", css);
        engine.setActiveStylesheet("test");

        Styleable element = createStyleable("Panel", null,
            new HashSet<>(Collections.singletonList("corners-only")));

        CssStyleResolver resolved = engine.resolve(element);

        assertThat(resolved.borderChars()).isPresent();
        assertThat(resolved.borderChars().get())
            .isEqualTo("\"\" \"\" \"\" \"\" \"┌\" \"┐\" \"└\" \"┘\"");
    }

    @Test
    void individualBorderPropertiesAreResolved() {
        String css = ".custom {\n" +
                     "  border-top: 'x';\n" +
                     "  border-bottom: 'y';\n" +
                     "  border-left: '|';\n" +
                     "  border-right: '|';\n" +
                     "  border-top-left: '+';\n" +
                     "  border-top-right: '+';\n" +
                     "  border-bottom-left: '+';\n" +
                     "  border-bottom-right: '+';\n" +
                     "}";
        StyleEngine engine = StyleEngine.create();
        engine.addStylesheet("test", css);
        engine.setActiveStylesheet("test");

        Styleable element = createStyleable("Panel", null,
            new HashSet<>(Collections.singletonList("custom")));

        CssStyleResolver resolved = engine.resolve(element);

        // Individual border properties are stored with quotes stripped
        assertThat(resolved.borderTop()).hasValue("x");
        assertThat(resolved.borderBottom()).hasValue("y");
        assertThat(resolved.borderLeft()).hasValue("|");
        assertThat(resolved.borderRight()).hasValue("|");
        assertThat(resolved.borderTopLeft()).hasValue("+");
        assertThat(resolved.borderTopRight()).hasValue("+");
        assertThat(resolved.borderBottomLeft()).hasValue("+");
        assertThat(resolved.borderBottomRight()).hasValue("+");
    }

    @Test
    void borderTypeWithBorderCharsOverride() {
        // Verify both border-type and border-chars can be used together
        String css = ".panel {\n" +
                     "  border-type: plain;\n" +
                     "  border-chars: \"\" \"\" \"\" \"\" \"┌\" \"┐\" \"└\" \"┘\";\n" +
                     "}";
        StyleEngine engine = StyleEngine.create();
        engine.addStylesheet("test", css);
        engine.setActiveStylesheet("test");

        Styleable element = createStyleable("Panel", null,
            new HashSet<>(Collections.singletonList("panel")));

        CssStyleResolver resolved = engine.resolve(element);

        // border-type is resolved
        assertThat(resolved.borderType()).hasValue(BorderType.PLAIN);
        // border-chars is also resolved
        assertThat(resolved.borderChars()).isPresent();
    }

    @Test
    void borderLeftWithEmptyValueIsNotResolved() {
        // When CSS has "border-left:" with no value (e.g., while typing),
        // the property should NOT be stored as an empty string, which would
        // cause the left border to disappear.
        String css = ".panel {\n" +
                     "  border-left:\n" +  // Empty value - user is still typing
                     "}";
        StyleEngine engine = StyleEngine.create();
        engine.addStylesheet("test", css);
        engine.setActiveStylesheet("test");

        Styleable element = createStyleable("Panel", null,
            new HashSet<>(Collections.singletonList("panel")));

        CssStyleResolver resolved = engine.resolve(element);

        // Empty values should NOT be stored - they would cause borders to disappear
        assertThat(resolved.borderLeft()).isEmpty();
    }

    @Test
    void borderLeftWithEmptyQuotedValueIsResolvedAsEmpty() {
        // When CSS has explicit empty quotes, it IS intentional
        String css = ".panel {\n" +
                     "  border-left: \"\";\n" +  // Explicit empty - intentional
                     "}";
        StyleEngine engine = StyleEngine.create();
        engine.addStylesheet("test", css);
        engine.setActiveStylesheet("test");

        Styleable element = createStyleable("Panel", null,
            new HashSet<>(Collections.singletonList("panel")));

        CssStyleResolver resolved = engine.resolve(element);

        // Explicit empty quotes ARE stored - user intentionally wants no border
        assertThat(resolved.borderLeft()).hasValue("");
    }

    @Test
    void borderLeftWithParserErrorValueIsNotResolved() {
        // When CSS has "border-left:" without semicolon, the parser may consume
        // the next property as the value (e.g., "height: 3"). This should NOT
        // be stored as a border character since it's clearly a parser error.
        String css = ".panel {\n" +
                     "  border-left:\n" +  // Missing value and semicolon
                     "  height: 3;\n" +    // Parser consumes this as border-left's value
                     "}";
        StyleEngine engine = StyleEngine.create();
        engine.addStylesheet("test", css);
        engine.setActiveStylesheet("test");

        Styleable element = createStyleable("Panel", null,
            new HashSet<>(Collections.singletonList("panel")));

        CssStyleResolver resolved = engine.resolve(element);

        // "height: 3" should NOT be stored as border-left - it contains ':'
        // and is too long to be a valid border character
        assertThat(resolved.borderLeft()).isEmpty();
    }

    @Test
    void borderLeftWithSingleUnquotedCharIsResolved() {
        // Single unquoted character should be valid
        String css = ".panel {\n" +
                     "  border-left: *;\n" +
                     "}";
        StyleEngine engine = StyleEngine.create();
        engine.addStylesheet("test", css);
        engine.setActiveStylesheet("test");

        Styleable element = createStyleable("Panel", null,
            new HashSet<>(Collections.singletonList("panel")));

        CssStyleResolver resolved = engine.resolve(element);

        assertThat(resolved.borderLeft()).hasValue("*");
    }

    @Test
    void descendantUniversalSelectorResolvesOnChild() {
        String css = "Panel * { text-overflow: ellipsis; }";
        StyleEngine engine = StyleEngine.create();
        engine.addStylesheet("test", css);
        engine.setActiveStylesheet("test");

        Styleable panel = createStyleable("Panel", null, Collections.emptySet());
        Styleable child = createStyleable("TextElement", null, Collections.emptySet());

        // Resolve child with panel as ancestor
        CssStyleResolver resolved = engine.resolve(child,
                dev.tamboui.css.cascade.PseudoClassState.NONE,
                Collections.singletonList(panel));

        assertThat(resolved.hasProperties()).isTrue();
        assertThat(resolved.textOverflow()).isPresent();
    }

    @Test
    void childUniversalSelectorResolvesOnDirectChildOnly() {
        String css = "Panel > * { text-overflow: ellipsis; }";
        StyleEngine engine = StyleEngine.create();
        engine.addStylesheet("test", css);
        engine.setActiveStylesheet("test");

        Styleable panel = createStyleable("Panel", null, Collections.emptySet());
        Styleable directChild = createStyleable("TextElement", null, Collections.emptySet());
        Styleable grandchild = createStyleable("TextElement", null, Collections.emptySet());

        // Direct child should match
        CssStyleResolver directResolved = engine.resolve(directChild,
                dev.tamboui.css.cascade.PseudoClassState.NONE,
                Collections.singletonList(panel));
        assertThat(directResolved.textOverflow()).isPresent();

        // Grandchild should NOT match
        Styleable middleRow = createStyleable("Row", null, Collections.emptySet());
        CssStyleResolver grandchildResolved = engine.resolve(grandchild,
                dev.tamboui.css.cascade.PseudoClassState.NONE,
                java.util.Arrays.asList(panel, middleRow));
        assertThat(grandchildResolved.textOverflow()).isEmpty();
    }

    private Styleable createStyleable(String type, String id, Set<String> classes) {
        return new TestStyleable(type, id, classes);
    }

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
