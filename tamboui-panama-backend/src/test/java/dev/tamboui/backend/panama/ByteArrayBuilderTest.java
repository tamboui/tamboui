/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.backend.panama;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that {@link ByteArrayBuilder#appendUtf8(CharSequence)} encodes exactly what
 * {@link String#getBytes(java.nio.charset.Charset)} would produce, including for the
 * box-drawing and emoji symbols a TUI writes every frame. The encoder replaced a
 * {@code getBytes} call that allocated a byte array per non-ASCII write.
 */
class ByteArrayBuilderTest {

    @ParameterizedTest
    @ValueSource(strings = {
        "",
        "a",
        "plain ascii text",
        "\u001b[12;34H",
        "éèê",
        "─│┌┘",
        "▁▂▃█",
        "世界",
        "mixed █ ascii 世 and wide",
        "🔥",
        "👨‍🦲",
        "a🔥b█c"
    })
    @DisplayName("encodes the same bytes as String.getBytes(UTF_8)")
    void matchesGetBytes(String input) {
        ByteArrayBuilder builder = new ByteArrayBuilder(4);
        builder.appendUtf8(input);

        byte[] expected = input.getBytes(StandardCharsets.UTF_8);
        assertThat(builder.toByteArray()).isEqualTo(expected);
    }

    @Test
    @DisplayName("unpaired surrogates are substituted like String.getBytes(UTF_8)")
    void unpairedSurrogates() {
        String highOnly = "a\uD83Db";
        String lowOnly = "a\uDD25b";

        ByteArrayBuilder high = new ByteArrayBuilder(4);
        high.appendUtf8(highOnly);
        ByteArrayBuilder low = new ByteArrayBuilder(4);
        low.appendUtf8(lowOnly);

        assertThat(high.toByteArray()).isEqualTo(highOnly.getBytes(StandardCharsets.UTF_8));
        assertThat(low.toByteArray()).isEqualTo(lowOnly.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    @DisplayName("appends a StringBuilder without going through toString")
    void appendsCharSequence() {
        StringBuilder cursorMove = new StringBuilder().append("\u001b[").append(12).append(';').append(34).append('H');

        ByteArrayBuilder builder = new ByteArrayBuilder(4);
        builder.appendUtf8(cursorMove);

        assertThat(builder.toByteArray()).isEqualTo(cursorMove.toString().getBytes(StandardCharsets.UTF_8));
    }

    @Test
    @DisplayName("grows to fit a long non-ASCII sequence")
    void growsForNonAscii() {
        char[] glyphs = new char[1000];
        Arrays.fill(glyphs, '█');
        String input = new String(glyphs);

        ByteArrayBuilder builder = new ByteArrayBuilder(2);
        builder.appendUtf8(input);

        assertThat(builder.toByteArray()).isEqualTo(input.getBytes(StandardCharsets.UTF_8));
    }
}
