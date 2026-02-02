/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.css.parser;

import java.util.List;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CssLexerTest {

    @Test
    void tokenizesIdentifier() {
        CssLexer lexer = new CssLexer("Panel");
        List<Token> tokens = lexer.tokenizeFiltered();

        assertThat(tokens).hasSize(2);
        assertThat(tokens.get(0)).isInstanceOf(Token.Ident.class);
        assertThat(((Token.Ident) tokens.get(0)).value()).isEqualTo("Panel");
        assertThat(tokens.get(1)).isInstanceOf(Token.EOF.class);
    }

    @Test
    void tokenizesHash() {
        CssLexer lexer = new CssLexer("#sidebar");
        List<Token> tokens = lexer.tokenizeFiltered();

        assertThat(tokens).hasSize(2);
        assertThat(tokens.get(0)).isInstanceOf(Token.Hash.class);
        assertThat(((Token.Hash) tokens.get(0)).value()).isEqualTo("sidebar");
    }

    @Test
    void tokenizesVariable() {
        CssLexer lexer = new CssLexer("$primary");
        List<Token> tokens = lexer.tokenizeFiltered();

        assertThat(tokens).hasSize(2);
        assertThat(tokens.get(0)).isInstanceOf(Token.Variable.class);
        assertThat(((Token.Variable) tokens.get(0)).name()).isEqualTo("primary");
    }

    @Test
    void tokenizesNumber() {
        CssLexer lexer = new CssLexer("42");
        List<Token> tokens = lexer.tokenizeFiltered();

        assertThat(tokens).hasSize(2);
        assertThat(tokens.get(0)).isInstanceOf(Token.Number.class);
        Token.Number num = (Token.Number) tokens.get(0);
        assertThat(num.value()).isEqualTo("42");
        assertThat(num.isPercentage()).isFalse();
    }

    @Test
    void tokenizesPercentage() {
        CssLexer lexer = new CssLexer("50%");
        List<Token> tokens = lexer.tokenizeFiltered();

        assertThat(tokens).hasSize(2);
        Token.Number num = (Token.Number) tokens.get(0);
        assertThat(num.value()).isEqualTo("50");
        assertThat(num.isPercentage()).isTrue();
    }

    @Test
    void tokenizesString() {
        CssLexer lexer = new CssLexer("\"hello world\"");
        List<Token> tokens = lexer.tokenizeFiltered();

        assertThat(tokens).hasSize(2);
        assertThat(tokens.get(0)).isInstanceOf(Token.StringToken.class);
        assertThat(((Token.StringToken) tokens.get(0)).value()).isEqualTo("hello world");
    }

    @Test
    void tokenizesDelimiters() {
        CssLexer lexer = new CssLexer(".class > *");
        List<Token> tokens = lexer.tokenizeFiltered();

        assertThat(tokens).hasSize(5);
        assertThat(tokens.get(0)).isInstanceOf(Token.Delim.class);
        assertThat(((Token.Delim) tokens.get(0)).value()).isEqualTo('.');
        assertThat(tokens.get(1)).isInstanceOf(Token.Ident.class);
        assertThat(tokens.get(2)).isInstanceOf(Token.Delim.class);
        assertThat(((Token.Delim) tokens.get(2)).value()).isEqualTo('>');
        assertThat(tokens.get(3)).isInstanceOf(Token.Delim.class);
        assertThat(((Token.Delim) tokens.get(3)).value()).isEqualTo('*');
        assertThat(tokens.get(4)).isInstanceOf(Token.EOF.class);
    }

    @Test
    void tokenizesBraces() {
        CssLexer lexer = new CssLexer("{ }");
        List<Token> tokens = lexer.tokenizeFiltered();

        assertThat(tokens).hasSize(3);
        assertThat(tokens.get(0)).isInstanceOf(Token.OpenBrace.class);
        assertThat(tokens.get(1)).isInstanceOf(Token.CloseBrace.class);
    }

    @Test
    void tokenizesColonAndSemicolon() {
        CssLexer lexer = new CssLexer("color: red;");
        List<Token> tokens = lexer.tokenizeFiltered();

        assertThat(tokens).hasSize(5);
        assertThat(tokens.get(0)).isInstanceOf(Token.Ident.class);
        assertThat(tokens.get(1)).isInstanceOf(Token.Colon.class);
        assertThat(tokens.get(2)).isInstanceOf(Token.Ident.class);
        assertThat(tokens.get(3)).isInstanceOf(Token.Semicolon.class);
    }

    @Test
    void skipsComments() {
        CssLexer lexer = new CssLexer("Panel /* comment */ Button");
        List<Token> tokens = lexer.tokenizeFiltered();

        assertThat(tokens).hasSize(3);
        assertThat(((Token.Ident) tokens.get(0)).value()).isEqualTo("Panel");
        assertThat(((Token.Ident) tokens.get(1)).value()).isEqualTo("Button");
    }

    @Test
    void tokenizesCompleteRule() {
        String css = ".error { color: red; text-style: bold; }";
        CssLexer lexer = new CssLexer(css);
        List<Token> tokens = lexer.tokenizeFiltered();

        assertThat(tokens).hasSize(13);
        // .error
        assertThat(tokens.get(0)).isInstanceOf(Token.Delim.class);
        assertThat(tokens.get(1)).isInstanceOf(Token.Ident.class);
        // {
        assertThat(tokens.get(2)).isInstanceOf(Token.OpenBrace.class);
        // color: red;
        assertThat(tokens.get(3)).isInstanceOf(Token.Ident.class);
        assertThat(tokens.get(4)).isInstanceOf(Token.Colon.class);
        assertThat(tokens.get(5)).isInstanceOf(Token.Ident.class);
        assertThat(tokens.get(6)).isInstanceOf(Token.Semicolon.class);
    }

    @Test
    void throwsOnUnexpectedCharacter() {
        CssLexer lexer = new CssLexer("@invalid");

        assertThatThrownBy(lexer::tokenizeFiltered)
                .isInstanceOf(CssParseException.class)
                .hasMessageContaining("Unexpected character");
    }

    @Test
    void tokenizesHexColor() {
        CssLexer lexer = new CssLexer("#ff0000");
        List<Token> tokens = lexer.tokenizeFiltered();

        assertThat(tokens).hasSize(2);
        assertThat(tokens.get(0)).isInstanceOf(Token.Hash.class);
        assertThat(((Token.Hash) tokens.get(0)).value()).isEqualTo("ff0000");
    }

    @Test
    void tracksLineAndColumn() {
        String css = "Panel {\n  color: red;\n}";
        CssLexer lexer = new CssLexer(css);
        List<Token> tokens = lexer.tokenizeFiltered();

        // Panel is on line 1
        assertThat(tokens.get(0).position().line()).isEqualTo(1);
        // color is on line 2
        assertThat(tokens.get(2).position().line()).isEqualTo(2);
    }
}
