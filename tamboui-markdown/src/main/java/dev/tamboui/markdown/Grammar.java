/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.markdown;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * A declarative description of how to tokenize a programming language, in the
 * same spirit as a highlight.js grammar. A grammar is an ordered list of
 * {@link Rule}s; {@link RegexSyntaxHighlighter} applies them to a snippet to
 * produce styled {@link dev.tamboui.text.Span}s.
 *
 * <p>Rules are matched at a given position in the order they are declared; the
 * first rule whose pattern matches consumes that position. This means rules
 * that should win over others (typically comments and strings) must be declared
 * first.
 */
public final class Grammar {

    private final String id;
    private final List<String> aliases;
    private final List<Rule> rules;

    private Grammar(Builder builder) {
        this.id = builder.id;
        this.aliases = Collections.unmodifiableList(new ArrayList<>(builder.aliases));
        this.rules = Collections.unmodifiableList(new ArrayList<>(builder.rules));
    }

    /**
     * Returns the canonical language identifier.
     *
     * @return the language id
     */
    public String id() {
        return id;
    }

    /**
     * Returns the alternate names this language is known by (e.g. {@code js}
     * and {@code node} for {@code javascript}).
     *
     * @return the aliases, never null
     */
    public List<String> aliases() {
        return aliases;
    }

    /**
     * Returns the ordered tokenizing rules.
     *
     * @return the rules, never null
     */
    public List<Rule> rules() {
        return rules;
    }

    /**
     * Creates a new builder for a language.
     *
     * @param id the canonical language identifier
     * @return a new builder
     */
    public static Builder builder(String id) {
        return new Builder(Objects.requireNonNull(id, "id"));
    }

    /**
     * A single tokenizing rule: a {@link TokenType} plus either a regex
     * {@link Pattern} or a literal multi-line {@code open}/{@code close} pair.
     */
    public static final class Rule {

        private final TokenType type;
        private final Pattern pattern;
        private final String open;
        private final String close;

        private Rule(TokenType type, Pattern pattern, String open, String close) {
            this.type = type;
            this.pattern = pattern;
            this.open = open;
            this.close = close;
        }

        /**
         * Creates a single-line rule matching {@code pattern}.
         *
         * @param type the token type
         * @param pattern the regex matched against the source from the current position
         * @return a new rule
         */
        public static Rule pattern(TokenType type, Pattern pattern) {
            return new Rule(Objects.requireNonNull(type, "type"),
                Objects.requireNonNull(pattern, "pattern"), null, null);
        }

        /**
         * Creates a single-line rule matching the literal {@code text}.
         *
         * @param type the token type
         * @param text the literal text to match
         * @return a new rule
         */
        public static Rule literal(TokenType type, String text) {
            return pattern(type, Pattern.compile(Pattern.quote(text)));
        }

        /**
         * Creates a multi-line rule that begins when {@code open} is found and
         * continues until {@code close} is found on a later (or the same) line.
         * Everything in between, across newlines, is emitted as a single token
         * of the given type.
         *
         * @param type the token type
         * @param open the literal opener (e.g. {@code /*})
         * @param close the literal closer (e.g. {@code *}{@code /})
         * @return a new rule
         */
        public static Rule multiline(TokenType type, String open, String close) {
            return new Rule(Objects.requireNonNull(type, "type"), null,
                Objects.requireNonNull(open, "open"), Objects.requireNonNull(close, "close"));
        }

        TokenType type() {
            return type;
        }

        Pattern pattern() {
            return pattern;
        }

        String open() {
            return open;
        }

        String close() {
            return close;
        }

        boolean multiline() {
            return open != null;
        }
    }

    /** Builder for {@link Grammar}. */
    public static final class Builder {

        private final String id;
        private final List<String> aliases = new ArrayList<>();
        private final List<Rule> rules = new ArrayList<>();

        private Builder(String id) {
            this.id = id;
        }

        /**
         * Adds alternate names for the language.
         *
         * @param aliases the aliases (e.g. {@code js}, {@code node})
         * @return this builder
         */
        public Builder alias(String... aliases) {
            for (String alias : aliases) {
                this.aliases.add(Objects.requireNonNull(alias, "alias"));
            }
            return this;
        }

        /**
         * Adds a tokenizing rule.
         *
         * @param rule the rule
         * @return this builder
         */
        public Builder rule(Rule rule) {
            rules.add(Objects.requireNonNull(rule, "rule"));
            return this;
        }

        /**
         * Builds the {@link Grammar}.
         *
         * @return a new grammar
         */
        public Grammar build() {
            return new Grammar(this);
        }
    }
}