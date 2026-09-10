/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.widgets.syntax;

/**
 * The semantic category of a highlighted source-code token. {@link SyntaxTheme}
 * maps each token type to a {@link dev.tamboui.style.Style} so a single code
 * base is rendered differently per kind of token, in the same spirit as
 * highlight.js themes.
 *
 * <p>Types are intentionally coarse and language-agnostic; grammars map
 * language-specific constructs (keywords, types, comments, ...) onto these
 * buckets.
 */
public enum TokenType {

    /**
     * Text matched by no grammar rule. A theme may style it, but by default
     * it falls back to the enclosing code-block base style.
     */
    PLAIN,
    /** A comment (single-line or multi-line). */
    COMMENT,
    /** A reserved word such as {@code if}, {@code class} or {@code return}. */
    KEYWORD,
    /** A primitive or built-in type such as {@code int} or {@code Number}. */
    TYPE,
    /** A string or character literal, including triple-quoted and raw strings. */
    STRING,
    /** A numeric literal. */
    NUMBER,
    /** A built-in constant such as {@code true}, {@code false} or {@code null}. */
    CONSTANT,
    /** A function or method name. */
    FUNCTION,
    /** An annotation or decorator (e.g. {@code @Override} in Java, {@code @decorator} in Python). */
    ANNOTATION,
    /** A language operator such as {@code +}, {@code ==} or {@code &&}. */
    OPERATOR,
    /** Structural punctuation such as braces, parentheses, commas and semicolons. */
    PUNCTUATION,
    /** An XML/HTML element name. */
    TAG,
    /** An XML/HTML attribute name. */
    ATTRIBUTE
}