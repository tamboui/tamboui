/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.text;

/**
 * A wrapper around a string that is masked when displayed.
 * <p>
 * The masked string is displayed as a series of the same character. This might be used to display
 * a password field or similar secure data.
 * <p>
 * Example:
 * <pre>{@code
 * Masked password = new Masked("secret123", '*');
 * Span span = Span.styled(password.value(), Style.EMPTY.fg(Color.RED));
 * // span will display as "*********"
 * }</pre>
 */
public final class Masked {

    private final String inner;
    private final char maskChar;

    /**
     * Creates a new masked string.
     *
     * @param text the text to mask
     * @param maskChar the character to use for masking
     */
    public Masked(String text, char maskChar) {
        this.inner = text != null ? text : "";
        this.maskChar = maskChar;
    }

    /**
     * Returns the character used for masking.
     *
     * @return the mask character
     */
    public char maskChar() {
        return maskChar;
    }

    /**
     * Returns the underlying string, with all characters masked.
     * Uses code point count to handle Unicode properly.
     *
     * @return the masked string
     */
    public String value() {
        if (inner.isEmpty()) {
            return "";
        }
        int codePointCount = inner.codePointCount(0, inner.length());
        StringBuilder sb = new StringBuilder(codePointCount);
        for (int i = 0; i < codePointCount; i++) {
            sb.append(maskChar);
        }
        return sb.toString();
    }

    /**
     * Returns the original (unmasked) text.
     * Use with caution - this exposes the sensitive data.
     *
     * @return the original unmasked text
     */
    public String original() {
        return inner;
    }

    @Override
    public String toString() {
        return value();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Masked)) {
            return false;
        }
        Masked masked = (Masked) o;
        return inner.equals(masked.inner) && maskChar == masked.maskChar;
    }

    @Override
    public int hashCode() {
        int result = inner.hashCode();
        result = 31 * result + Character.hashCode(maskChar);
        return result;
    }
}

