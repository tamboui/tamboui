/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.widgets.syntax;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import dev.tamboui.style.Color;
import dev.tamboui.style.Style;

import static org.assertj.core.api.Assertions.assertThat;

class SyntaxThemeTest {

    @Test
    @DisplayName("themes with the same token styles are equal")
    void themesWithTheSameStylesAreEqual() {
        SyntaxTheme red = SyntaxTheme.builder().token(TokenType.KEYWORD, Style.EMPTY.fg(Color.RED)).build();
        SyntaxTheme sameRed = SyntaxTheme.builder().token(TokenType.KEYWORD, Style.EMPTY.fg(Color.RED)).build();

        assertThat(SyntaxTheme.builder().build()).isEqualTo(SyntaxTheme.DEFAULTS)
            .hasSameHashCodeAs(SyntaxTheme.DEFAULTS);
        assertThat(red).isEqualTo(sameRed).hasSameHashCodeAs(sameRed);
        assertThat(red).isNotEqualTo(SyntaxTheme.DEFAULTS);
        assertThat(SyntaxTheme.builder().clear(TokenType.COMMENT).build()).isNotEqualTo(SyntaxTheme.DEFAULTS);
    }
}
