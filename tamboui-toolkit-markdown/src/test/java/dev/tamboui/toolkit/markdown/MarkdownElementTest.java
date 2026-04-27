/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.toolkit.markdown;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import dev.tamboui.buffer.Buffer;
import dev.tamboui.layout.Rect;
import dev.tamboui.markdown.MarkdownStyles;
import dev.tamboui.style.Color;
import dev.tamboui.style.Modifier;
import dev.tamboui.style.Overflow;
import dev.tamboui.style.Style;
import dev.tamboui.terminal.Frame;
import dev.tamboui.toolkit.AbstractElementTest;
import dev.tamboui.toolkit.element.RenderContext;
import dev.tamboui.toolkit.element.Size;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MarkdownElementTest extends AbstractElementTest {

    private static Buffer renderInto(MarkdownElement element, int width, int height) {
        Rect area = new Rect(0, 0, width, height);
        Buffer buffer = Buffer.empty(area);
        Frame frame = Frame.forTesting(buffer);
        element.render(frame, area, RenderContext.empty());
        return buffer;
    }

    @Test
    @DisplayName("of() and markdown() create equivalent elements")
    void factoryMethods() {
        assertThat(MarkdownElement.of("hi").source()).isEqualTo("hi");
        assertThat(MarkdownElement.markdown("hi").source()).isEqualTo("hi");
    }

    @Test
    @DisplayName("of() rejects null source")
    void rejectsNullSource() {
        assertThatThrownBy(() -> MarkdownElement.of(null))
            .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("renders bold text via the underlying widget")
    void rendersBold() {
        Buffer buffer = renderInto(MarkdownElement.of("**bold**"), 20, 1);
        assertThat(buffer.get(0, 0).symbol()).isEqualTo("b");
        assertThat(buffer.get(0, 0).style().addModifiers()).contains(Modifier.BOLD);
    }

    @Test
    @DisplayName("programmatic styles override defaults")
    void programmaticStyles() {
        MarkdownStyles styles = MarkdownStyles.builder()
            .strong(Style.EMPTY.fg(Color.MAGENTA))
            .build();

        Buffer buffer = renderInto(
            MarkdownElement.of("**bold**").styles(styles), 20, 1);

        assertThat(buffer.get(0, 0).style().fg()).isPresent()
            .get()
            .isEqualTo(Color.MAGENTA);
    }

    @Test
    @DisplayName("overflow propagates to the widget")
    void overflowPropagates() {
        String longText = "the quick brown fox jumps over the lazy dog and so on";
        Buffer buffer = renderInto(
            MarkdownElement.of(longText).overflow(Overflow.ELLIPSIS), 20, 1);

        StringBuilder rendered = new StringBuilder();
        for (int x = 0; x < 20; x++) {
            rendered.append(buffer.get(x, 0).symbol());
        }
        assertThat(rendered.toString()).endsWith("...");
    }

    @Test
    @DisplayName("scroll skips the leading rows")
    void scrolls() {
        Buffer buffer = renderInto(
            MarkdownElement.of("# Title\n\nbody line one\n\nbody line two").scroll(6), 20, 1);

        assertThat(buffer.get(0, 0).symbol()).isEqualTo("b");
    }

    @Test
    @DisplayName("preferredSize returns UNKNOWN when the available width is non-positive")
    void preferredSizeUnknownWithoutWidth() {
        Size size = MarkdownElement.of("# Title").preferredSize(-1, -1, RenderContext.empty());
        assertThat(size).isEqualTo(Size.UNKNOWN);
    }

    @Test
    @DisplayName("preferredSize reports a height that fits the rendered content")
    void preferredSizeReportsHeight() {
        // Heading + underline rule + spacer + paragraph + spacer + paragraph = 6 rows at 80 cols.
        Size size = MarkdownElement.of("# Title\n\nfirst paragraph\n\nsecond paragraph")
            .preferredSize(80, -1, RenderContext.empty());

        assertThat(size.width()).isEqualTo(80);
        assertThat(size.height()).isEqualTo(6);
    }

    @Test
    @DisplayName("preferredSize grows when content has to wrap at a narrower width")
    void preferredSizeGrowsWhenWrapping() {
        String prose = "alpha beta gamma delta epsilon zeta eta theta iota kappa lambda";
        MarkdownElement element = MarkdownElement.of(prose);

        int wide = element.preferredSize(80, -1, RenderContext.empty()).height();
        int narrow = element.preferredSize(20, -1, RenderContext.empty()).height();

        assertThat(wide).isEqualTo(1);
        assertThat(narrow).isGreaterThan(wide);
    }
}
