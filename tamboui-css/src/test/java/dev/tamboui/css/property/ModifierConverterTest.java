/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.css.property;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;

import dev.tamboui.style.Modifier;

import static org.assertj.core.api.Assertions.assertThat;

class ModifierConverterTest {

    private final ModifierConverter converter = ModifierConverter.INSTANCE;

    @Test
    void convertsSingleModifier() {
        assertThat(converter.convert("bold", Collections.<String, String>emptyMap()))
                .hasValueSatisfying(mods -> assertThat(mods).containsExactly(Modifier.BOLD));

        assertThat(converter.convert("italic", Collections.<String, String>emptyMap()))
                .hasValueSatisfying(mods -> assertThat(mods).containsExactly(Modifier.ITALIC));

        assertThat(converter.convert("underlined", Collections.<String, String>emptyMap()))
                .hasValueSatisfying(mods -> assertThat(mods).containsExactly(Modifier.UNDERLINED));
    }

    @Test
    void convertsMultipleModifiers() {
        Optional<Set<Modifier>> result = converter.convert("bold italic underlined",
                Collections.<String, String>emptyMap());

        assertThat(result).hasValueSatisfying(mods -> assertThat(mods)
                .containsExactlyInAnyOrder(Modifier.BOLD, Modifier.ITALIC, Modifier.UNDERLINED));
    }

    @Test
    void convertsAlternativeNames() {
        assertThat(converter.convert("underline", Collections.<String, String>emptyMap()))
                .hasValueSatisfying(mods -> assertThat(mods).containsExactly(Modifier.UNDERLINED));

        assertThat(converter.convert("reverse", Collections.<String, String>emptyMap()))
                .hasValueSatisfying(mods -> assertThat(mods).containsExactly(Modifier.REVERSED));

        assertThat(converter.convert("strikethrough", Collections.<String, String>emptyMap()))
                .hasValueSatisfying(mods -> assertThat(mods).containsExactly(Modifier.CROSSED_OUT));
    }

    @Test
    void ignoresInvalidModifiers() {
        Optional<Set<Modifier>> result = converter.convert("bold invalid italic",
                Collections.<String, String>emptyMap());

        assertThat(result).hasValueSatisfying(
                mods -> assertThat(mods).containsExactlyInAnyOrder(Modifier.BOLD, Modifier.ITALIC));
    }

    @Test
    void returnsEmptyForInvalidValue() {
        assertThat(converter.convert("invalid", Collections.<String, String>emptyMap())).isEmpty();
        assertThat(converter.convert("", Collections.<String, String>emptyMap())).isEmpty();
        assertThat(converter.convert(null, Collections.<String, String>emptyMap())).isEmpty();
    }

    @Test
    void handlesCaseInsensitive() {
        assertThat(converter.convert("BOLD", Collections.<String, String>emptyMap()))
                .hasValueSatisfying(mods -> assertThat(mods).containsExactly(Modifier.BOLD));
    }
}
