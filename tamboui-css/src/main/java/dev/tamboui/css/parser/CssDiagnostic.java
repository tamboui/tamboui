/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.css.parser;

import java.util.Objects;

import dev.tamboui.css.selector.Selector;

/**
 * Diagnostic information for an invalid, out-of-range, or unrecognized CSS declaration.
 * <p>
 * Diagnostics are produced during parse-time validation ({@link CssParser#validate(dev.tamboui.css.model.Stylesheet)})
 * to provide authoring-time feedback on stylesheet problems without impacting the rendering cascade.
 */
public final class CssDiagnostic {

    /**
     * The classification of diagnostic problem.
     */
    public enum Kind {
        /** The CSS property name is not recognized by the registry or standard properties. */
        UNKNOWN_PROPERTY("unknown-property"),
        /** The CSS property value is recognized syntactically but exceeds valid range limits or overflows. */
        OUT_OF_RANGE("out-of-range"),
        /** The CSS property value cannot be converted to the expected type. */
        UNCONVERTIBLE_VALUE("unconvertible"),
        /** A referenced CSS variable is not defined in the stylesheet. */
        UNRESOLVED_VARIABLE("unresolved-variable");

        private final String code;

        Kind(String code) {
            this.code = code;
        }

        /**
         * Returns the short string code for this diagnostic kind.
         *
         * @return the diagnostic code
         */
        public String code() {
            return code;
        }
    }

    private final Selector selector;
    private final Token.Position position;
    private final String property;
    private final String rawValue;
    private final String reason;
    private final Kind kind;

    /**
     * Creates a new CSS diagnostic.
     *
     * @param selector the selector of the rule containing the declaration (may be null)
     * @param position the source position of the declaration
     * @param property the CSS property name
     * @param rawValue the raw property value
     * @param reason   the human-readable reason describing the issue
     * @param kind     the classification of the problem
     */
    public CssDiagnostic(Selector selector,
                         Token.Position position,
                         String property,
                         String rawValue,
                         String reason,
                         Kind kind) {
        this.selector = selector;
        this.position = position != null ? position : Token.Position.UNKNOWN;
        this.property = Objects.requireNonNull(property, "property cannot be null");
        this.rawValue = Objects.requireNonNull(rawValue, "rawValue cannot be null");
        this.reason = Objects.requireNonNull(reason, "reason cannot be null");
        this.kind = Objects.requireNonNull(kind, "kind cannot be null");
    }

    /**
     * Returns the selector of the rule containing this declaration.
     *
     * @return the selector, or null if not associated with a specific selector
     */
    public Selector selector() {
        return selector;
    }

    /**
     * Returns the position in the CSS source where this declaration was parsed.
     *
     * @return the source position
     */
    public Token.Position position() {
        return position;
    }

    /**
     * Returns the 1-based line number of the declaration in the CSS source, or 0 if unknown.
     *
     * @return the line number
     */
    public int line() {
        return position.line();
    }

    /**
     * Returns the 1-based column number of the declaration in the CSS source, or 0 if unknown.
     *
     * @return the column number
     */
    public int column() {
        return position.column();
    }

    /**
     * Returns the CSS property name.
     *
     * @return the property name
     */
    public String property() {
        return property;
    }

    /**
     * Returns the raw property value string from the stylesheet.
     *
     * @return the raw value
     */
    public String rawValue() {
        return rawValue;
    }

    /**
     * Returns a human-readable explanation of why the declaration is invalid.
     *
     * @return the reason description
     */
    public String reason() {
        return reason;
    }

    /**
     * Returns the category/kind of problem detected.
     *
     * @return the diagnostic kind
     */
    public Kind kind() {
        return kind;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof CssDiagnostic)) {
            return false;
        }
        CssDiagnostic that = (CssDiagnostic) o;
        return Objects.equals(selector, that.selector)
                && position.equals(that.position)
                && property.equals(that.property)
                && rawValue.equals(that.rawValue)
                && reason.equals(that.reason)
                && kind == that.kind;
    }

    @Override
    public int hashCode() {
        return Objects.hash(selector, position, property, rawValue, reason, kind);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (position.line() > 0) {
            sb.append("line ").append(position.line()).append(", column ").append(position.column()).append(": ");
        }
        if (selector != null) {
            sb.append(selector.toCss()).append(" { ");
        }
        sb.append(property).append(": ").append(rawValue);
        if (selector != null) {
            sb.append(" }");
        }
        sb.append(" - ").append(kind.code()).append(" (").append(reason).append(")");
        return sb.toString();
    }
}
