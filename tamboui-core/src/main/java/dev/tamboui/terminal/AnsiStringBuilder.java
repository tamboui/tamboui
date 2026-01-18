/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.terminal;

import dev.tamboui.style.AnsiColor;
import dev.tamboui.style.Color;
import dev.tamboui.style.Hyperlink;
import dev.tamboui.style.Modifier;
import dev.tamboui.style.Style;

import java.util.EnumSet;

/// Utility class for building ANSI-escaped strings from styled content.
/// This class provides methods to convert TamboUI styles to ANSI escape sequences
/// that can be used for direct terminal output without requiring the full TUI system.
///
///
///
/// Example usage:
/// ```java
/// Style style = Style.create().fg(Color.GREEN).bold();
/// String ansi = AnsiStringBuilder.styleToAnsi(style);
/// System.out.print(ansi + "Hello, World!" + AnsiStringBuilder.RESET);
/// }
/// ```
public final class AnsiStringBuilder {

    /// The escape character.
    private static final String ESC = "\u001b";

    /// Control Sequence Introducer (CSI) prefix for ANSI escape codes.
    private static final String CSI = ESC + "[";

    /// ANSI reset sequence that clears all formatting.
    public static final String RESET = CSI + "0m";

    private AnsiStringBuilder() {
        // Utility class
    }

    /// Converts a {@link Style} to an ANSI SGR (Select Graphic Rendition) escape sequence.
    /// The returned string includes the complete escape sequence including the reset prefix
    /// and the 'm' terminator.
    ///
    /// @param style the style to convert
    /// @return an ANSI escape sequence representing the style
    public static String styleToAnsi(Style style) {
        StringBuilder sb = new StringBuilder();
        sb.append(CSI).append("0");  // Reset first

        // Foreground color
        if (style.fg().isPresent()) {
            sb.append(";");
            sb.append(colorToAnsiForeground(style.fg().get()));
        }

        // Background color
        if (style.bg().isPresent()) {
            sb.append(";");
            sb.append(colorToAnsiBackground(style.bg().get()));
        }

        // Modifiers
        EnumSet<Modifier> modifiers = style.effectiveModifiers();
        for (Modifier mod : modifiers) {
            sb.append(";").append(mod.code());
        }

        // Underline color (if supported by terminal)
        if (style.underlineColor().isPresent()) {
            String underlineAnsi = underlineColorToAnsi(style.underlineColor().get());
            if (!underlineAnsi.isEmpty()) {
                sb.append(";").append(underlineAnsi);
            }
        }

        sb.append("m");
        return sb.toString();
    }

    /// Converts a {@link Color} to an ANSI foreground color code.
    ///
    /// @param color the color to convert
    /// @return the ANSI code string (without CSI prefix or 'm' suffix)
    public static String colorToAnsiForeground(Color color) {
        if (color instanceof Color.Reset) {
            return "39";
        } else if (color instanceof Color.Ansi) {
            AnsiColor c = ((Color.Ansi) color).color();
            return String.valueOf(c.fgCode());
        } else if (color instanceof Color.Indexed) {
            int idx = ((Color.Indexed) color).index();
            return "38;5;" + idx;
        } else if (color instanceof Color.Rgb) {
            Color.Rgb rgb = (Color.Rgb) color;
            return "38;2;" + rgb.r() + ";" + rgb.g() + ";" + rgb.b();
        }
        return "";
    }

    /// Converts a {@link Color} to an ANSI background color code.
    ///
    /// @param color the color to convert
    /// @return the ANSI code string (without CSI prefix or 'm' suffix)
    public static String colorToAnsiBackground(Color color) {
        if (color instanceof Color.Reset) {
            return "49";
        } else if (color instanceof Color.Ansi) {
            AnsiColor c = ((Color.Ansi) color).color();
            return String.valueOf(c.bgCode());
        } else if (color instanceof Color.Indexed) {
            int idx = ((Color.Indexed) color).index();
            return "48;5;" + idx;
        } else if (color instanceof Color.Rgb) {
            Color.Rgb rgb = (Color.Rgb) color;
            return "48;2;" + rgb.r() + ";" + rgb.g() + ";" + rgb.b();
        }
        return "";
    }

    /// Converts a {@link Color} to an ANSI underline color code.
    /// Note that underline colors are only supported by some terminal emulators.
    ///
    /// @param color the color to convert
    /// @return the ANSI code string (without CSI prefix or 'm' suffix),
    /// or empty string if the color type doesn't support underline coloring
    public static String underlineColorToAnsi(Color color) {
        if (color instanceof Color.Indexed) {
            int idx = ((Color.Indexed) color).index();
            return "58;5;" + idx;
        } else if (color instanceof Color.Rgb) {
            Color.Rgb rgb = (Color.Rgb) color;
            return "58;2;" + rgb.r() + ";" + rgb.g() + ";" + rgb.b();
        }
        return "";
    }

    /// Generates an OSC8 hyperlink escape sequence to start a hyperlink.
    ///
    ///
    ///
    /// OSC8 format: {@code \033]8;id=<id>;<url>\033\\}
    /// If no ID is provided, the format is: {@code \033]8;;<url>\033\\}
    ///
    /// @param hyperlink the hyperlink to generate a sequence for
    /// @return the OSC8 escape sequence to start the hyperlink
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

    /// Generates an OSC8 escape sequence to end a hyperlink.
    ///
    ///
    ///
    /// Format: {@code \033]8;;\033\\}
    ///
    /// @return the OSC8 escape sequence to end the hyperlink
    public static String hyperlinkEnd() {
        return ESC + "]8;;" + ESC + "\\";
    }

    /// Escapes special characters in OSC parameter values.
    ///
    ///
    ///
    /// According to the OSC8 specification, semicolons and backslashes need to be escaped.
    ///
    /// @param param the parameter value to escape
    /// @return the escaped parameter value
    private static String escapeOscParam(String param) {
        // Escape backslashes and semicolons
        return param.replace("\\", "\\\\").replace(";", "\\;");
    }
}

