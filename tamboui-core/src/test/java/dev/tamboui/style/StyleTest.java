/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.style;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.assertj.core.api.Assertions.*;

class StyleTest {

    @Test
    @DisplayName("Style.EMPTY has no colors or modifiers")
    void emptyStyle() {
        assertThat(Style.EMPTY.fg()).isEmpty();
        assertThat(Style.EMPTY.bg()).isEmpty();
        assertThat(Style.EMPTY.addModifiers()).isEmpty();
    }

    @Test
    @DisplayName("Style fg sets foreground color")
    void fgColor() {
        Style style = Style.EMPTY.fg(Color.RED);
        assertThat(style.fg()).contains(Color.RED);
        assertThat(style.bg()).isEmpty();
    }

    @Test
    @DisplayName("Style bg sets background color")
    void bgColor() {
        Style style = Style.EMPTY.bg(Color.BLUE);
        assertThat(style.bg()).contains(Color.BLUE);
        assertThat(style.fg()).isEmpty();
    }

    @Test
    @DisplayName("Style bold adds BOLD modifier")
    void boldModifier() {
        Style style = Style.EMPTY.bold();
        assertThat(style.addModifiers()).contains(Modifier.BOLD);
    }

    @Test
    @DisplayName("Style italic adds ITALIC modifier")
    void italicModifier() {
        Style style = Style.EMPTY.italic();
        assertThat(style.addModifiers()).contains(Modifier.ITALIC);
    }

    @Test
    @DisplayName("Style underlined adds UNDERLINED modifier")
    void underlinedModifier() {
        Style style = Style.EMPTY.underlined();
        assertThat(style.addModifiers()).contains(Modifier.UNDERLINED);
    }

    @Test
    @DisplayName("Style chaining")
    void chaining() {
        Style style = Style.EMPTY
            .fg(Color.RED)
            .bg(Color.BLACK)
            .bold()
            .italic();

        assertThat(style.fg()).contains(Color.RED);
        assertThat(style.bg()).contains(Color.BLACK);
        assertThat(style.addModifiers()).contains(Modifier.BOLD, Modifier.ITALIC);
    }

    @Test
    @DisplayName("Style patch merges styles")
    void patch() {
        Style base = Style.EMPTY.fg(Color.RED).bold();
        Style patch = Style.EMPTY.bg(Color.BLUE).italic();

        Style merged = base.patch(patch);

        assertThat(merged.fg()).contains(Color.RED);
        assertThat(merged.bg()).contains(Color.BLUE);
        assertThat(merged.addModifiers()).contains(Modifier.BOLD, Modifier.ITALIC);
    }

    @Test
    @DisplayName("Style patch overwrites colors")
    void patchOverwrites() {
        Style base = Style.EMPTY.fg(Color.RED);
        Style patch = Style.EMPTY.fg(Color.GREEN);

        Style merged = base.patch(patch);

        assertThat(merged.fg()).contains(Color.GREEN);
    }

    @Test
    @DisplayName("Style hyperlink stores url")
    void hyperlinkUrl() {
        Style style = Style.EMPTY.hyperlink("https://example.com");
        assertThat(style.hyperlink()).contains(Hyperlink.of("https://example.com"));
    }

    @Test
    @DisplayName("Style hyperlink stores url and id")
    void hyperlinkUrlAndId() {
        Style style = Style.EMPTY.hyperlink("https://example.com", "link-1");
        assertThat(style.hyperlink()).contains(Hyperlink.of("https://example.com", "link-1"));
    }

    @Test
    @DisplayName("Style hyperlink accepts Hyperlink instance")
    void hyperlinkInstance() {
        Hyperlink hyperlink = Hyperlink.of("https://example.com", "link-2");
        Style style = Style.EMPTY.hyperlink(hyperlink);
        assertThat(style.hyperlink()).contains(hyperlink);
    }

    @Test
    @DisplayName("Style preserves hyperlink across style modifiers")
    void hyperlinkPreservedAcrossModifiers() {
        Style style = Style.EMPTY.hyperlink("https://example.com")
            .fg(Color.RED)
            .underlined();

        assertThat(style.hyperlink()).contains(Hyperlink.of("https://example.com"));
    }

    @Test
    @DisplayName("Style patch with Tags extension merges tags via Patchable")
    void patchMergesTagsViaPatchable() {
        Style style1 = Style.EMPTY.withExtension(Tags.class, Tags.of("bold", "red"));
        Style style2 = Style.EMPTY.withExtension(Tags.class, Tags.of("italic", "green"));

        Style merged = style1.patch(style2);

        Tags tags = merged.extension(Tags.class, Tags.empty());
        assertThat(tags.values()).containsExactlyInAnyOrder("bold", "red", "italic", "green");
    }

    @Test
    @DisplayName("Style patch with non-Patchable extension replaces value")
    void patchReplacesNonPatchableExtension() {
        Style style1 = Style.EMPTY.hyperlink("https://first.com");
        Style style2 = Style.EMPTY.hyperlink("https://second.com");

        Style merged = style1.patch(style2);

        assertThat(merged.hyperlink()).contains(Hyperlink.of("https://second.com"));
    }

    @Test
    @DisplayName("Style patch with Tags on only one side preserves Tags")
    void patchWithTagsOnOneSide() {
        Style style1 = Style.EMPTY.withExtension(Tags.class, Tags.of("bold"));
        Style style2 = Style.EMPTY.fg(Color.RED);

        Style merged = style1.patch(style2);

        Tags tags = merged.extension(Tags.class, Tags.empty());
        assertThat(tags.values()).containsExactly("bold");
        assertThat(merged.fg()).contains(Color.RED);
    }

    @Test
    @DisplayName("Style patch accumulates Tags through multiple patches")
    void patchAccumulatesTagsThroughMultiplePatches() {
        Style style1 = Style.EMPTY.withExtension(Tags.class, Tags.of("outer"));
        Style style2 = Style.EMPTY.withExtension(Tags.class, Tags.of("middle"));
        Style style3 = Style.EMPTY.withExtension(Tags.class, Tags.of("inner"));

        Style merged = style1.patch(style2).patch(style3);

        Tags tags = merged.extension(Tags.class, Tags.empty());
        assertThat(tags.values()).containsExactlyInAnyOrder("outer", "middle", "inner");
    }

}
