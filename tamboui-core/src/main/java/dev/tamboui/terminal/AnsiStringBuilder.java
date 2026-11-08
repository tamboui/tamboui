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
 * Utility class for building ANSI-escaped strings from styled content.
 * This class provides methods to convert TamboUI styles to ANSI escape sequences
 * that can be used for direct terminal output without requiring the full TUI system.
 *
 * <p>Example usage:
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
     * Converts a {@link Style} to an ANSI SGR (Select Graphic Rendition) escape sequence.
     * The returned string includes the complete escape sequence including the reset prefix
     * and the 'm' terminator.
     *
     * @param style the style to convert
     * @return an ANSI escape sequence representing the style
     */
    public static String styleToAnsi(Style style) {
        StringBuilder sb = new StringBuilder();
        sb.append(CSI).append("0");  // Reset first

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

        // Modifiers. NORMAL is the reset written first; its code 0 here would also clear the colors.
        EnumSet<Modifier> modifiers = style.effectiveModifiers();
        for (Modifier mod : modifiers) {
            if (mod != Modifier.NORMAL) {
                sb.append(";").append(mod.code());
            }
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
     * OSC8 format: {@code \033]8;id=<id>;<url>\033\\}
     * If no ID is provided, the format is: {@code \033]8;;<url>\033\\}
     *
     * @param hyperlink the hyperlink to generate a sequence for
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
     * Generates an OSC 2 escape sequence to set the terminal window title.
     * <p>
     * Format: {@code \033]2;title\007}
     * <p>
     * Control characters (ESC and BEL) are stripped from the title to prevent
     * sequence injection or premature termination.
     *
     * @param title the window title to set
     * @return the OSC 2 escape sequence
     */
    public static String windowTitle(String title) {
        String safeTitle = title.replace("\u001b", "").replace("\u0007", "");
        return ESC + "]2;" + safeTitle + "\u0007";
    }

    /**
     * Generates an XTWINOPS escape sequence to push the current window title
     * onto the terminal's title stack ({@code CSI 22;2t}).
     * <p>
     * Terminals without title-stack support ignore this sequence.
     *
     * @return the save window title escape sequence
     */
    public static String saveWindowTitle() {
        return ESC + "[22;2t";
    }

    /**
     * Generates an XTWINOPS escape sequence to pop the window title from the
     * terminal's title stack ({@code CSI 23;2t}), restoring the previously saved title.
     * <p>
     * Terminals without title-stack support ignore this sequence.
     *
     * @return the restore window title escape sequence
     */
    public static String restoreWindowTitle() {
        return ESC + "[23;2t";
    }

    /**
     * Sanitizes an OSC 8 parameter value against terminal escape-sequence injection.
     * <p>
     * Control characters that could terminate or forge terminal sequences are
     * percent-encoded: C0 controls ({@code 0x00}-{@code 0x1F}, including ESC and
     * BEL), DEL ({@code 0x7F}) and C1 controls ({@code 0x80}-{@code 0x9F}). All
     * other characters, including semicolons, are passed through unchanged: the
     * OSC 8 specification defines no backslash escaping, and escaping semicolons
     * corrupts otherwise valid URLs.
     *
     * @param param the parameter value to sanitize
     * @return the sanitized parameter value, safe to embed in an OSC 8 sequence
     */
    private static String escapeOscParam(String param) {
        StringBuilder sb = new StringBuilder(param.length());
        for (int i = 0; i < param.length(); i++) {
            char c = param.charAt(i);
            if (c <= 0x1F || c == 0x7F || (c >= 0x80 && c <= 0x9F)) {
                sb.append('%').append(String.format("%02X", (int) c));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}
