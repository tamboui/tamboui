/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.css.cascade;

import dev.tamboui.css.Styleable;
import dev.tamboui.css.engine.StyleEngine;
import dev.tamboui.css.model.Stylesheet;
import dev.tamboui.css.parser.CssParser;
import dev.tamboui.style.Color;
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
