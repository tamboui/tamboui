/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.css.property;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import dev.tamboui.widgets.block.BorderSet;

import static org.assertj.core.api.Assertions.assertThat;

class BorderSetConverterTest {

    private final BorderSetConverter converter = BorderSetConverter.INSTANCE;

    @Test
    void convertsFullBorderSet() {
        Optional<BorderSet> result = converter
                .convert("\"─\" \"─\" \"│\" \"│\" \"┌\" \"┐\" \"└\" \"┘\"", Collections.emptyMap());

        assertThat(result).hasValueSatisfying(borderSet -> {
            assertThat(borderSet.topHorizontal()).isEqualTo("─");
            assertThat(borderSet.bottomHorizontal()).isEqualTo("─");
            assertThat(borderSet.leftVertical()).isEqualTo("│");
            assertThat(borderSet.rightVertical()).isEqualTo("│");
            assertThat(borderSet.topLeft()).isEqualTo("┌");
            assertThat(borderSet.topRight()).isEqualTo("┐");
            assertThat(borderSet.bottomLeft()).isEqualTo("└");
            assertThat(borderSet.bottomRight()).isEqualTo("┘");
        });
    }

    @Test
    void convertsCornersOnlyWithEmptyStrings() {
        Optional<BorderSet> result = converter
                .convert("\"\" \"\" \"\" \"\" \"┌\" \"┐\" \"└\" \"┘\"", Collections.emptyMap());

        assertThat(result).hasValueSatisfying(borderSet -> {
            assertThat(borderSet.topHorizontal()).isEmpty();
            assertThat(borderSet.bottomHorizontal()).isEmpty();
            assertThat(borderSet.leftVertical()).isEmpty();
            assertThat(borderSet.rightVertical()).isEmpty();
            assertThat(borderSet.topLeft()).isEqualTo("┌");
            assertThat(borderSet.topRight()).isEqualTo("┐");
            assertThat(borderSet.bottomLeft()).isEqualTo("└");
            assertThat(borderSet.bottomRight()).isEqualTo("┘");
        });
    }

    @Test
    void convertsHorizontalOnlyBorders() {
        Optional<BorderSet> result = converter.convert("\"─\" \"─\" \"\" \"\" \"\" \"\" \"\" \"\"",
                Collections.emptyMap());

        assertThat(result).hasValueSatisfying(borderSet -> {
            assertThat(borderSet.topHorizontal()).isEqualTo("─");
            assertThat(borderSet.bottomHorizontal()).isEqualTo("─");
            assertThat(borderSet.leftVertical()).isEmpty();
            assertThat(borderSet.rightVertical()).isEmpty();
            assertThat(borderSet.topLeft()).isEmpty();
            assertThat(borderSet.topRight()).isEmpty();
            assertThat(borderSet.bottomLeft()).isEmpty();
            assertThat(borderSet.bottomRight()).isEmpty();
        });
    }

    @Test
    void convertsCustomCharacters() {
        Optional<BorderSet> result = converter
                .convert("\"~\" \"~\" \"|\" \"|\" \"+\" \"+\" \"+\" \"+\"", Collections.emptyMap());

        assertThat(result).hasValueSatisfying(borderSet -> {
            assertThat(borderSet.topHorizontal()).isEqualTo("~");
            assertThat(borderSet.bottomHorizontal()).isEqualTo("~");
            assertThat(borderSet.leftVertical()).isEqualTo("|");
            assertThat(borderSet.rightVertical()).isEqualTo("|");
            assertThat(borderSet.topLeft()).isEqualTo("+");
            assertThat(borderSet.topRight()).isEqualTo("+");
            assertThat(borderSet.bottomLeft()).isEqualTo("+");
            assertThat(borderSet.bottomRight()).isEqualTo("+");
        });
    }

    @Test
    void supportsSingleQuotes() {
        Optional<BorderSet> result = converter.convert("'-' '-' '|' '|' '+' '+' '+' '+'",
                Collections.emptyMap());

        assertThat(result).hasValueSatisfying(borderSet -> {
            assertThat(borderSet.topHorizontal()).isEqualTo("-");
            assertThat(borderSet.bottomHorizontal()).isEqualTo("-");
            assertThat(borderSet.leftVertical()).isEqualTo("|");
            assertThat(borderSet.rightVertical()).isEqualTo("|");
            assertThat(borderSet.topLeft()).isEqualTo("+");
            assertThat(borderSet.topRight()).isEqualTo("+");
            assertThat(borderSet.bottomLeft()).isEqualTo("+");
            assertThat(borderSet.bottomRight()).isEqualTo("+");
        });
    }

    @Test
    void resolvesVariableReference() {
        Map<String, String> variables = new HashMap<>();
        variables.put("corners", "\"\" \"\" \"\" \"\" \"┌\" \"┐\" \"└\" \"┘\"");

        Optional<BorderSet> result = converter.convert("$corners", variables);

        assertThat(result).hasValueSatisfying(borderSet -> {
            assertThat(borderSet.topHorizontal()).isEmpty();
            assertThat(borderSet.topLeft()).isEqualTo("┌");
        });
    }

    @Test
    void returnsEmptyForInvalidValue() {
        assertThat(converter.convert("", Collections.emptyMap())).isEmpty();
        assertThat(converter.convert(null, Collections.emptyMap())).isEmpty();
        assertThat(converter.convert("invalid", Collections.emptyMap())).isEmpty();
    }

    @Test
    void returnsEmptyForWrongNumberOfStrings() {
        // Only 4 strings instead of 8
        assertThat(converter.convert("\"─\" \"─\" \"│\" \"│\"", Collections.emptyMap())).isEmpty();

        // 9 strings
        assertThat(converter.convert("\"─\" \"─\" \"│\" \"│\" \"┌\" \"┐\" \"└\" \"┘\" \"extra\"",
                Collections.emptyMap())).isEmpty();
    }

    @Test
    void returnsEmptyForUnterminatedQuote() {
        assertThat(converter.convert("\"─\" \"─\" \"│\" \"│\" \"┌\" \"┐\" \"└\" \"┘",
                Collections.emptyMap())).isEmpty();
    }
}
