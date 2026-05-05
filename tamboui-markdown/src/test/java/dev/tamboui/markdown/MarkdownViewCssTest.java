/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.markdown;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import dev.tamboui.buffer.Buffer;
import dev.tamboui.layout.Rect;
import dev.tamboui.style.Color;
import dev.tamboui.style.Modifier;
import dev.tamboui.style.PropertyDefinition;
import dev.tamboui.style.Style;
import dev.tamboui.style.StylePropertyResolver;

import static org.assertj.core.api.Assertions.assertThat;

class MarkdownViewCssTest {

    private static Buffer renderInto(MarkdownView view, int width, int height) {
        Rect area = new Rect(0, 0, width, height);
        Buffer buffer = Buffer.empty(area);
        view.render(area, buffer);
        return buffer;
    }

    private static StylePropertyResolver resolverOf(Map<String, String> rules) {
        return new StylePropertyResolver() {
            @Override
            public <T> Optional<T> get(PropertyDefinition<T> property) {
                String value = rules.get(property.name());
                if (value == null) {
                    return Optional.empty();
                }
                return property.convert(value);
            }
        };
    }

    @Test
    @DisplayName("CSS color and text-style properties drive an unset slot")
    void cssFillsDefault() {
        Map<String, String> rules = new HashMap<>();
        rules.put("link-color", "magenta");
        rules.put("link-text-style", "crossed-out");

        MarkdownView view = MarkdownView.builder()
            .source("see [docs](https://example.com)")
            .styleResolver(resolverOf(rules))
            .build();
        Buffer buffer = renderInto(view, 30, 1);

        assertThat(buffer.get(4, 0).symbol()).isEqualTo("d");
        assertThat(buffer.get(4, 0).style().fg()).isPresent()
            .get()
            .isEqualTo(Color.MAGENTA);
        assertThat(buffer.get(4, 0).style().addModifiers()).contains(Modifier.CROSSED_OUT);
    }

    @Test
    @DisplayName("explicit MarkdownStyles override wins over CSS")
    void explicitWinsOverCss() {
        Map<String, String> rules = new HashMap<>();
        rules.put("link-color", "magenta");

        MarkdownStyles userStyles = MarkdownStyles.builder()
            .link(Style.EMPTY.fg(Color.GREEN))
            .build();

        MarkdownView view = MarkdownView.builder()
            .source("[docs](https://example.com)")
            .styles(userStyles)
            .styleResolver(resolverOf(rules))
            .build();
        Buffer buffer = renderInto(view, 30, 1);

        assertThat(buffer.get(0, 0).style().fg()).isPresent()
            .get()
            .isEqualTo(Color.GREEN);
    }

    @Test
    @DisplayName("CSS heading-1 properties replace the default H1 style")
    void cssHeading() {
        Map<String, String> rules = new HashMap<>();
        rules.put("heading-1-color", "yellow");
        rules.put("heading-1-text-style", "bold");

        MarkdownView view = MarkdownView.builder()
            .source("# Title")
            .styleResolver(resolverOf(rules))
            .build();
        Buffer buffer = renderInto(view, 20, 1);

        assertThat(buffer.get(0, 0).style().fg()).isPresent()
            .get()
            .isEqualTo(Color.YELLOW);
        assertThat(buffer.get(0, 0).style().addModifiers()).contains(Modifier.BOLD);
        assertThat(buffer.get(0, 0).style().addModifiers()).doesNotContain(Modifier.REVERSED);
    }

    @Test
    @DisplayName("default values are still used when neither programmatic nor CSS sets a slot")
    void defaultsRemain() {
        MarkdownView view = MarkdownView.builder()
            .source("**bold**")
            .styleResolver(resolverOf(new HashMap<>()))
            .build();
        Buffer buffer = renderInto(view, 10, 1);

        assertThat(buffer.get(0, 0).style().addModifiers()).contains(Modifier.BOLD);
    }

    @Test
    @DisplayName("CSS can set just one of color, background, or text-style")
    void cssPartialOverride() {
        Map<String, String> rules = new HashMap<>();
        rules.put("strong-color", "red");

        MarkdownView view = MarkdownView.builder()
            .source("**bold**")
            .styleResolver(resolverOf(rules))
            .build();
        Buffer buffer = renderInto(view, 10, 1);

        assertThat(buffer.get(0, 0).style().fg()).isPresent()
            .get()
            .isEqualTo(Color.RED);
    }

    @Test
    @DisplayName("CSS background color is applied to the resolved style")
    void cssBackground() {
        Map<String, String> rules = new HashMap<>();
        rules.put("blockquote-color", "yellow");
        rules.put("blockquote-background", "blue");

        MarkdownView view = MarkdownView.builder()
            .source("> quote")
            .styleResolver(resolverOf(rules))
            .build();
        Buffer buffer = renderInto(view, 20, 1);

        assertThat(buffer.get(2, 0).symbol()).isEqualTo("q");
        assertThat(buffer.get(2, 0).style().fg()).isPresent()
            .get()
            .isEqualTo(Color.YELLOW);
        assertThat(buffer.get(2, 0).style().bg()).isPresent()
            .get()
            .isEqualTo(Color.BLUE);
    }

    @Test
    @DisplayName("CSS blockquote-prefix replaces the default bar glyph")
    void cssBlockquotePrefix() {
        Map<String, String> rules = new HashMap<>();
        rules.put("blockquote-prefix", ">");

        MarkdownView view = MarkdownView.builder()
            .source("> quote")
            .styleResolver(resolverOf(rules))
            .build();
        Buffer buffer = renderInto(view, 20, 1);

        assertThat(buffer.get(0, 0).symbol()).isEqualTo(">");
        assertThat(buffer.get(2, 0).symbol()).isEqualTo("q");
    }

    @Test
    @DisplayName("programmatic blockquotePrefix wins over CSS")
    void programmaticBlockquotePrefix() {
        Map<String, String> rules = new HashMap<>();
        rules.put("blockquote-prefix", ">");

        MarkdownStyles styles = MarkdownStyles.builder()
            .blockquotePrefix("»")
            .build();

        MarkdownView view = MarkdownView.builder()
            .source("> quote")
            .styles(styles)
            .styleResolver(resolverOf(rules))
            .build();
        Buffer buffer = renderInto(view, 20, 1);

        assertThat(buffer.get(0, 0).symbol()).isEqualTo("»");
    }

    @Test
    @DisplayName("CSS task-checked-color colours the checked marker")
    void cssTaskCheckedColor() {
        Map<String, String> rules = new HashMap<>();
        rules.put("task-checked-color", "magenta");

        MarkdownView view = MarkdownView.builder()
            .source("- [x] done")
            .styleResolver(resolverOf(rules))
            .build();
        Buffer buffer = renderInto(view, 30, 1);

        // After "• " (offset 2), the task glyph "[x] " starts.
        assertThat(buffer.get(2, 0).symbol()).isEqualTo("[");
        assertThat(buffer.get(2, 0).style().fg()).isPresent()
            .get()
            .isEqualTo(Color.MAGENTA);
        assertThat(buffer.get(3, 0).style().fg()).isPresent()
            .get()
            .isEqualTo(Color.MAGENTA);
    }

    @Test
    @DisplayName("CSS task-unchecked-color is independent of checked")
    void cssTaskUncheckedColor() {
        Map<String, String> rules = new HashMap<>();
        rules.put("task-checked-color", "green");
        rules.put("task-unchecked-color", "red");

        MarkdownView view = MarkdownView.builder()
            .source("- [x] done\n- [ ] todo")
            .styleResolver(resolverOf(rules))
            .build();
        Buffer buffer = renderInto(view, 30, 2);

        assertThat(buffer.get(2, 0).style().fg()).isPresent().get().isEqualTo(Color.GREEN);
        assertThat(buffer.get(2, 1).style().fg()).isPresent().get().isEqualTo(Color.RED);
    }

    @Test
    @DisplayName("CSS task-checked-symbol replaces the default [x] glyph")
    void cssTaskCheckedSymbol() {
        Map<String, String> rules = new HashMap<>();
        rules.put("task-checked-symbol", "✓");
        rules.put("task-unchecked-symbol", "·");

        MarkdownView view = MarkdownView.builder()
            .source("- [x] done\n- [ ] todo")
            .styleResolver(resolverOf(rules))
            .build();
        Buffer buffer = renderInto(view, 30, 2);

        assertThat(buffer.get(2, 0).symbol()).isEqualTo("✓");
        assertThat(buffer.get(2, 1).symbol()).isEqualTo("·");
    }

    @Test
    @DisplayName("programmatic taskChecked wins over CSS")
    void programmaticTaskCheckedWins() {
        Map<String, String> rules = new HashMap<>();
        rules.put("task-checked-color", "magenta");

        MarkdownStyles styles = MarkdownStyles.builder()
            .taskChecked(Style.EMPTY.fg(Color.YELLOW))
            .build();

        MarkdownView view = MarkdownView.builder()
            .source("- [x] done")
            .styles(styles)
            .styleResolver(resolverOf(rules))
            .build();
        Buffer buffer = renderInto(view, 30, 1);

        assertThat(buffer.get(2, 0).style().fg()).isPresent()
            .get()
            .isEqualTo(Color.YELLOW);
    }
}
