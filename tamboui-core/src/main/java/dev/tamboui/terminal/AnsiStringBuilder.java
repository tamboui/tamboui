/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.terminal;

import java.util.EnumSet;

import dev.tamboui.style.Hyperlink;
import dev.tamboui.style.Modifier;
import dev.tamboui.style.Style;

/**
 * Utility class for building ANSI-escaped strings from styled content. This
 * class provides methods to convert TamboUI styles to ANSI escape sequences
 * that can be used for direct terminal output without requiring the full TUI
 * system.
 *
 * <p>
 * Example usage:
 * 
 * <pre>{@code
 * Style style = Style.create().fg(Color.GREEN).bold();
 * String ansi = AnsiStringBuilder.styleToAnsi(style);
 * System.out.print(ansi + "Hello, World!" + AnsiStringBuilder.RESET);
 * }</pre>
 */
public final class AnsiStringBuilder {

    /**
     * The escape character.
     */
    private static final String ESC = "\u001b";

    /**
     * Control Sequence Introducer (CSI) prefix for ANSI escape codes.
     */
    private static final String CSI = ESC + "[";

    /**
     * ANSI reset sequence that clears all formatting.
     */
    public static final String RESET = CSI + "0m";

    private AnsiStringBuilder() {
        // Utility class
    }

    /**
     * Converts a {@link Style} to an ANSI SGR (Select Graphic Rendition) escape
     * sequence. The returned string includes the complete escape sequence including
     * the reset prefix and the 'm' terminator.
     *
     * @param style
     *            the style to convert
     * @return an ANSI escape sequence representing the style
     */
    public static String styleToAnsi(Style style) {
        StringBuilder sb = new StringBuilder();
        sb.append(CSI).append("0"); // Reset first

        // Foreground color
        if (style.fg().isPresent()) {
            sb.append(";");
            sb.append(style.fg().get().toAnsiForeground());
        }

        // Background color
        if (style.bg().isPresent()) {
            sb.append(";");
            sb.append(style.bg().get().toAnsiBackground());
        }

        // Modifiers
        EnumSet<Modifier> modifiers = style.effectiveModifiers();
        for (Modifier mod : modifiers) {
            sb.append(";").append(mod.code());
        }

        // Underline color (if supported by terminal)
        if (style.underlineColor().isPresent()) {
            String underlineAnsi = style.underlineColor().get().toAnsiUnderline();
            if (!underlineAnsi.isEmpty()) {
                sb.append(";").append(underlineAnsi);
            }
        }

        sb.append("m");
        return sb.toString();
    }

    /**
     * Generates an OSC8 hyperlink escape sequence to start a hyperlink.
     * <p>
     * OSC8 format: {@code \033]8;id=<id>;<url>\033\\} If no ID is provided, the
     * format is: {@code \033]8;;<url>\033\\}
     *
     * @param hyperlink
     *            the hyperlink to generate a sequence for
     * @return the OSC8 escape sequence to start the hyperlink
     */
    public static String hyperlinkStart(Hyperlink hyperlink) {
        StringBuilder sb = new StringBuilder();
        sb.append(ESC).append("]8;");
        if (hyperlink.id().isPresent()) {
            sb.append("id=").append(escapeOscParam(hyperlink.id().get()));
        }
        sb.append(";");
        sb.append(escapeOscParam(hyperlink.url()));
        sb.append(ESC).append("\\");
        return sb.toString();
    }

    /**
     * Generates an OSC8 escape sequence to end a hyperlink.
     * <p>
     * Format: {@code \033]8;;\033\\}
     *
     * @return the OSC8 escape sequence to end the hyperlink
     */
    public static String hyperlinkEnd() {
        return ESC + "]8;;" + ESC + "\\";
    }

    /**
     * Escapes special characters in OSC parameter values.
     * <p>
     * According to the OSC8 specification, semicolons and backslashes need to be
     * escaped.
     *
     * @param param
     *            the parameter value to escape
     * @return the escaped parameter value
     */
    private static String escapeOscParam(String param) {
        // Escape backslashes and semicolons
        return param.replace("\\", "\\\\").replace(";", "\\;");
    }
}
