/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.widgets.figlet;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A FIGlet font loaded from a {@code .flf} file.
 *
 * <p>This is a minimal FIGlet implementation intended for deterministic rendering inside a TUI.
 * It loads fonts from resources and does not require any external {@code figlet} binary.</p>
 */
public final class FigletFont {

    private static final int ASCII_START = 32;
    private static final int ASCII_END = 126;
    private static final int ASCII_COUNT = ASCII_END - ASCII_START + 1;

    private static final Map<String, FigletFont> RESOURCE_CACHE = new ConcurrentHashMap<>();

    private final char hardBlank;
    private final int height;
    /**
     * Glyphs for ASCII 32..126. Each glyph is {@code height} strings.
     */
    private final String[][] asciiGlyphs;

    private FigletFont(char hardBlank, int height, String[][] asciiGlyphs) {
        this.hardBlank = hardBlank;
        this.height = height;
        this.asciiGlyphs = asciiGlyphs;
    }

    public static FigletFont bundled(BundledFigletFont font) {
        Objects.requireNonNull(font, "font");
        return fromResource(font.resourcePath());
    }

    public static FigletFont fromResource(String resourcePath) {
        Objects.requireNonNull(resourcePath, "resourcePath");
        return RESOURCE_CACHE.computeIfAbsent(resourcePath, path -> {
            try (InputStream in = FigletFont.class.getResourceAsStream(path)) {
                if (in == null) {
                    throw new IllegalArgumentException("FIGlet font resource not found: " + path);
                }
                return parse(in);
            } catch (IOException e) {
                throw new IllegalStateException("Failed to load FIGlet font from resource: " + path, e);
            }
        });
    }

    /**
     * Loads a FIGlet font from a filesystem path.
     *
     * <p>Unlike {@link #fromResource(String)}, this does not cache by default.</p>
     */
    public static FigletFont fromPath(Path path) {
        Objects.requireNonNull(path, "path");
        try (InputStream in = Files.newInputStream(path)) {
            return parse(in);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to load FIGlet font from path: " + path, e);
        }
    }

    /**
     * Loads a FIGlet font from an input stream.
     *
     * <p>The stream will be fully consumed but not closed.</p>
     */
    public static FigletFont fromInputStream(InputStream in) {
        Objects.requireNonNull(in, "in");
        try {
            return parse(in);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to load FIGlet font from input stream", e);
        }
    }

    public int height() {
        return height;
    }

    /**
     * Renders {@code text} into FIGlet lines.
     *
     * @param text              text to render (may contain newlines)
     * @param kerning           when true, overlaps adjacent letters where possible (spaces only)
     * @param letterSpacing     minimum number of spaces between glyphs (0+)
     * @param trimRight         when true, right-trims each output line
     */
    public List<String> render(String text, boolean kerning, int letterSpacing, boolean trimRight) {
        if (text == null || text.isEmpty()) {
            return Collections.emptyList();
        }
        int spacing = Math.max(0, letterSpacing);

        String[] inputLines = text.split("\\R", -1);
        List<String> out = new ArrayList<>(inputLines.length * height);

        for (String inputLine : inputLines) {
            List<StringBuilder> rows = new ArrayList<>(height);
            for (int i = 0; i < height; i++) {
                rows.add(new StringBuilder());
            }

            boolean firstGlyph = true;
            int i = 0;
            while (i < inputLine.length()) {
                int cp = inputLine.codePointAt(i);
                i += Character.charCount(cp);
                String[] glyph = glyphForCodePoint(cp);

                if (firstGlyph) {
                    for (int r = 0; r < height; r++) {
                        rows.get(r).append(glyph[r]);
                    }
                    firstGlyph = false;
                    continue;
                }

                // Add explicit spacing before attempting kerning overlap.
                if (spacing > 0) {
                    for (int r = 0; r < height; r++) {
                        for (int s = 0; s < spacing; s++) {
                            rows.get(r).append(' ');
                        }
                    }
                }

                if (!kerning) {
                    for (int r = 0; r < height; r++) {
                        rows.get(r).append(glyph[r]);
                    }
                    continue;
                }

                int shift = computeKerningShift(rows, glyph);
                if (shift <= 0) {
                    for (int r = 0; r < height; r++) {
                        rows.get(r).append(glyph[r]);
                    }
                    continue;
                }

                // Overlay the overlapped prefix onto the current rows.
                int currentWidth = maxWidth(rows);
                padRight(rows, currentWidth);
                for (int r = 0; r < height; r++) {
                    StringBuilder cur = rows.get(r);
                    String next = glyph[r];
                    for (int k = 0; k < shift; k++) {
                        int dest = currentWidth - shift + k;
                        char a = charAtOrSpace(cur, dest);
                        char b = charAtOrSpace(next, k);
                        if (a == ' ' && b != ' ') {
                            cur.setCharAt(dest, b);
                        }
                    }
                }

                // Append glyph excluding the overlapped prefix.
                for (int r = 0; r < height; r++) {
                    String line = glyph[r];
                    if (shift >= line.length()) {
                        // fully overlapped for this row (rare, but safe)
                        continue;
                    }
                    rows.get(r).append(line.substring(shift));
                }
            }

            for (int r = 0; r < height; r++) {
                String line = rows.get(r).toString();
                out.add(trimRight ? rtrim(line) : line);
            }
        }

        return out;
    }

    private String[] glyphForCodePoint(int codePoint) {
        int cp = codePoint;
        if (cp < ASCII_START || cp > ASCII_END) {
            cp = '?';
        }
        String[] glyph = asciiGlyphs[cp - ASCII_START];
        if (glyph != null) {
            return glyph;
        }

        // Fallbacks: '?' then space.
        String[] fallback = asciiGlyphs['?' - ASCII_START];
        if (fallback != null) {
            return fallback;
        }
        return asciiGlyphs[' ' - ASCII_START];
    }

    private int computeKerningShift(List<StringBuilder> currentRows, String[] nextGlyph) {
        int currentWidth = maxWidth(currentRows);
        int nextWidth = maxWidth(nextGlyph);
        if (currentWidth == 0 || nextWidth == 0) {
            return 0;
        }

        // Normalize current rows to the same width by padding spaces.
        padRight(currentRows, currentWidth);

        int maxShift = Math.min(currentWidth, nextWidth);
        for (int shift = maxShift; shift >= 1; shift--) {
            boolean collision = false;
            for (int r = 0; r < height; r++) {
                StringBuilder cur = currentRows.get(r);
                String next = nextGlyph[r];
                for (int k = 0; k < shift; k++) {
                    char a = charAtOrSpace(cur, currentWidth - shift + k);
                    char b = charAtOrSpace(next, k);
                    if (a != ' ' && b != ' ') {
                        collision = true;
                        break;
                    }
                }
                if (collision) {
                    break;
                }
            }
            if (!collision) {
                return shift;
            }
        }

        return 0;
    }

    private static int maxWidth(List<StringBuilder> rows) {
        int w = 0;
        for (StringBuilder sb : rows) {
            w = Math.max(w, sb.length());
        }
        return w;
    }

    private static int maxWidth(String[] rows) {
        int w = 0;
        for (String s : rows) {
            w = Math.max(w, s.length());
        }
        return w;
    }

    private static void padRight(List<StringBuilder> rows, int width) {
        for (StringBuilder sb : rows) {
            int missing = width - sb.length();
            if (missing > 0) {
                for (int i = 0; i < missing; i++) {
                    sb.append(' ');
                }
            }
        }
    }

    private static char charAtOrSpace(CharSequence s, int idx) {
        if (idx < 0 || idx >= s.length()) {
            return ' ';
        }
        return s.charAt(idx);
    }

    private static String rtrim(String s) {
        int end = s.length();
        while (end > 0 && s.charAt(end - 1) == ' ') {
            end--;
        }
        return end == s.length() ? s : s.substring(0, end);
    }

    private static FigletFont parse(InputStream in) throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));

        String header = br.readLine();
        if (header == null || header.length() < 6 || !header.startsWith("flf2a")) {
            throw new IllegalArgumentException("Invalid FIGlet font header");
        }

        char hardBlank = header.charAt(5);

        String[] parts = header.substring(6).trim().split("\\s+");
        if (parts.length < 5) {
            throw new IllegalArgumentException("Invalid FIGlet font header parameters");
        }

        int height = Integer.parseInt(parts[0]);
        // Header params: height, baseline, maxLen, oldLayout, commentLines, ...
        int commentLines = Integer.parseInt(parts[4]);

        for (int i = 0; i < commentLines; i++) {
            // discard
            br.readLine();
        }

        String[][] glyphs = new String[ASCII_COUNT][];

        // Read ASCII 32..126 glyphs.
        Character endMark = null;
        for (int code = ASCII_START; code <= ASCII_END; code++) {
            String[] lines = new String[height];
            for (int r = 0; r < height; r++) {
                String line = br.readLine();
                if (line == null) {
                    throw new IllegalArgumentException("Unexpected EOF while reading FIGlet glyphs");
                }

                if (endMark == null) {
                    endMark = line.charAt(line.length() - 1);
                }

                String stripped = stripEndMark(line, endMark);
                stripped = stripped.replace(hardBlank, ' ');
                lines[r] = stripped;
            }
            glyphs[code - ASCII_START] = lines;
        }

        return new FigletFont(hardBlank, height, glyphs);
    }

    private static String stripEndMark(String line, char endMark) {
        int end = line.length();
        if (end >= 2 && line.charAt(end - 1) == endMark && line.charAt(end - 2) == endMark) {
            end -= 2;
        } else if (end >= 1 && line.charAt(end - 1) == endMark) {
            end -= 1;
        }
        return line.substring(0, end);
    }
}

