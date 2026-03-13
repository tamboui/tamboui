/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.terminal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.Test;

import dev.tamboui.buffer.Buffer;
import dev.tamboui.buffer.Cell;
import dev.tamboui.buffer.DiffResult;
import dev.tamboui.layout.Rect;
import dev.tamboui.style.Color;
import dev.tamboui.style.Style;

/**
 * Analysis test to measure cursor adjacency ratio in realistic DiffResult output.
 * This tells us how many cursor moves could be skipped if we tracked cursor position.
 */
class CursorAdjacencyAnalysisTest {

    /**
     * Simulates a bordered panel with text content (like most TUI apps).
     * Measures how many DiffResult entries are adjacent (same row, x+1).
     */
    @Test
    void borderedPanelWithTextContent() {
        int width = 160, height = 50;
        Rect area = Rect.of(width, height);

        // Previous frame: bordered panel with text
        Buffer prev = Buffer.empty(area);
        renderBorderedPanel(prev, width, height, "Hello world - this is line ");

        // Current frame: same border, some text changed (e.g. scrolled content)
        Buffer curr = Buffer.empty(area);
        renderBorderedPanel(curr, width, height, "Updated content for line ");

        DiffResult diff = new DiffResult(area.area());
        prev.diff(curr, diff);

        analyzeAdjacency("Bordered panel (text change)", diff, width, height);
    }

    /**
     * Simulates scattered random changes (like cursor blink + status bar update).
     */
    @Test
    void scatteredChanges() {
        int width = 160, height = 50;
        Rect area = Rect.of(width, height);

        Buffer prev = Buffer.empty(area);
        Buffer curr = Buffer.empty(area);

        // Fill both with same content
        String line = repeat("a", width);
        for (int y = 0; y < height; y++) {
            prev.setString(0, y, line, Style.EMPTY);
            curr.setString(0, y, line, Style.EMPTY);
        }

        // Scatter 5% random changes
        Random rand = new Random(42);
        int totalCells = width * height;
        int numChanges = totalCells * 5 / 100;
        List<Integer> indices = new ArrayList<>(totalCells);
        for (int i = 0; i < totalCells; i++) indices.add(i);
        Collections.shuffle(indices, rand);
        for (int i = 0; i < numChanges; i++) {
            int idx = indices.get(i);
            int x = idx % width;
            int y = idx / width;
            curr.setString(x, y, "X", Style.EMPTY);
        }

        DiffResult diff = new DiffResult(area.area());
        prev.diff(curr, diff);

        analyzeAdjacency("Scattered 5% random", diff, width, height);
    }

    /**
     * Simulates a list/table view where a few rows change (scroll by 1).
     */
    @Test
    void listScrollByOne() {
        int width = 160, height = 50;
        Rect area = Rect.of(width, height);

        Buffer prev = Buffer.empty(area);
        Buffer curr = Buffer.empty(area);

        // Previous: rows labeled 0-49
        for (int y = 0; y < height; y++) {
            String text = String.format("  Row %3d: %s", y, repeat("content for this row", 3));
            if (text.length() > width) text = text.substring(0, width);
            prev.setString(0, y, text, Style.EMPTY);
        }

        // Current: rows labeled 1-50 (scrolled by 1)
        for (int y = 0; y < height; y++) {
            String text = String.format("  Row %3d: %s", y + 1, repeat("content for this row", 3));
            if (text.length() > width) text = text.substring(0, width);
            curr.setString(0, y, text, Style.EMPTY);
        }

        DiffResult diff = new DiffResult(area.area());
        prev.diff(curr, diff);

        analyzeAdjacency("List scroll by 1", diff, width, height);
    }

    /**
     * Simulates a status bar update (only bottom row changes).
     */
    @Test
    void statusBarUpdate() {
        int width = 160, height = 50;
        Rect area = Rect.of(width, height);

        Buffer prev = Buffer.empty(area);
        Buffer curr = Buffer.empty(area);

        // Same content everywhere
        String line = repeat("Static content here...", 5);
        if (line.length() > width) line = line.substring(0, width);
        for (int y = 0; y < height; y++) {
            prev.setString(0, y, line, Style.EMPTY);
            curr.setString(0, y, line, Style.EMPTY);
        }

        // Only status bar differs
        String prevStatus = String.format("%-" + width + "s", " Status: OK | 12:34:56 | Lines: 100");
        String currStatus = String.format("%-" + width + "s", " Status: OK | 12:34:57 | Lines: 101");
        prev.setString(0, height - 1, prevStatus, Style.EMPTY.fg(Color.GREEN));
        curr.setString(0, height - 1, currStatus, Style.EMPTY.fg(Color.GREEN));

        DiffResult diff = new DiffResult(area.area());
        prev.diff(curr, diff);

        analyzeAdjacency("Status bar update only", diff, width, height);
    }

    /**
     * Full screen redraw (like RGB demo or first frame).
     */
    @Test
    void fullScreenRedraw() {
        int width = 160, height = 50;
        Rect area = Rect.of(width, height);

        Buffer prev = Buffer.empty(area); // all Cell.EMPTY
        Buffer curr = Buffer.empty(area);

        // Fill entire screen with content
        for (int y = 0; y < height; y++) {
            StringBuilder sb = new StringBuilder(width);
            for (int x = 0; x < width; x++) {
                sb.append((char) ('a' + ((x + y) % 26)));
            }
            curr.setString(0, y, sb.toString(), Style.EMPTY);
        }

        DiffResult diff = new DiffResult(area.area());
        prev.diff(curr, diff);

        analyzeAdjacency("Full screen redraw", diff, width, height);
    }

    /**
     * Gauge widget: entire gauge area changes (progress update).
     */
    @Test
    void gaugeProgressUpdate() {
        int width = 160, height = 50;
        Rect area = Rect.of(width, height);

        Buffer prev = Buffer.empty(area);
        Buffer curr = Buffer.empty(area);

        Style borderStyle = Style.EMPTY.fg(Color.WHITE);
        Cell hBorder = new Cell("─", borderStyle);
        Cell vBorder = new Cell("│", borderStyle);

        // Both have same border
        for (int x = 0; x < width; x++) {
            prev.set(x, 0, hBorder);
            curr.set(x, 0, hBorder);
            prev.set(x, height - 1, hBorder);
            curr.set(x, height - 1, hBorder);
        }
        for (int y = 1; y < height - 1; y++) {
            prev.set(0, y, vBorder);
            curr.set(0, y, vBorder);
            prev.set(width - 1, y, vBorder);
            curr.set(width - 1, y, vBorder);
        }

        // Gauge area inside border: prev at 40%, curr at 42%
        Style gaugeStyle = Style.EMPTY.fg(Color.GREEN);
        int innerWidth = width - 2;
        int prevFilled = innerWidth * 40 / 100;
        int currFilled = innerWidth * 42 / 100;

        for (int y = 1; y < height - 1; y++) {
            for (int x = 1; x < width - 1; x++) {
                int col = x - 1;
                prev.set(x, y, new Cell(col < prevFilled ? "█" : " ", gaugeStyle));
                curr.set(x, y, new Cell(col < currFilled ? "█" : " ", gaugeStyle));
            }
        }

        DiffResult diff = new DiffResult(area.area());
        prev.diff(curr, diff);

        analyzeAdjacency("Gauge 40%→42%", diff, width, height);
    }

    private void renderBorderedPanel(Buffer buffer, int width, int height, String linePrefix) {
        Style borderStyle = Style.EMPTY.fg(Color.WHITE);

        // Borders via buffer.set (like Block widget)
        buffer.set(0, 0, new Cell("┌", borderStyle));
        buffer.set(width - 1, 0, new Cell("┐", borderStyle));
        buffer.set(0, height - 1, new Cell("└", borderStyle));
        buffer.set(width - 1, height - 1, new Cell("┘", borderStyle));
        for (int x = 1; x < width - 1; x++) {
            buffer.set(x, 0, new Cell("─", borderStyle));
            buffer.set(x, height - 1, new Cell("─", borderStyle));
        }
        for (int y = 1; y < height - 1; y++) {
            buffer.set(0, y, new Cell("│", borderStyle));
            buffer.set(width - 1, y, new Cell("│", borderStyle));
        }

        // Text content inside border via setString (like Paragraph widget)
        int innerWidth = width - 2;
        for (int y = 1; y < height - 1; y++) {
            String text = linePrefix + y;
            if (text.length() < innerWidth) {
                text = text + repeat(" ", innerWidth - text.length());
            } else {
                text = text.substring(0, innerWidth);
            }
            buffer.setString(1, y, text, Style.EMPTY);
        }
    }

    private static String repeat(String s, int count) {
        StringBuilder sb = new StringBuilder(s.length() * count);
        for (int i = 0; i < count; i++) {
            sb.append(s);
        }
        return sb.toString();
    }

    private void analyzeAdjacency(String scenario, DiffResult diff, int width, int height) {
        int size = diff.size();
        if (size == 0) {
            System.out.printf("%n=== %s ===%n  No diffs (identical frames)%n", scenario);
            return;
        }

        int adjacent = 0;       // cursor is already at the right position (x+1, same y)
        int sameRow = 0;        // same row but not adjacent (gap)
        int differentRow = 0;   // different row (always needs cursor move)
        int continuations = 0;  // skipped continuation cells

        int prevX = -1, prevY = -1;
        for (int i = 0; i < size; i++) {
            if (diff.getCell(i).isContinuation()) {
                continuations++;
                continue;
            }

            int x = diff.getX(i);
            int y = diff.getY(i);

            if (prevX >= 0) {
                if (y == prevY && x == prevX + 1) {
                    adjacent++;
                } else if (y == prevY) {
                    sameRow++;
                } else {
                    differentRow++;
                }
            }

            prevX = x;
            prevY = y;
        }

        int totalMoves = size - continuations;
        int skippable = adjacent;
        int required = totalMoves - skippable - 1; // -1 for first cell which always needs a move

        // Estimate bytes saved
        // Average cursor move: "\e[NN;NNNNH" ≈ 10-12 bytes
        int avgMoveBytes = 11;
        int bytesSaved = skippable * avgMoveBytes;
        int totalMoveBytes = (totalMoves - 1) * avgMoveBytes;

        System.out.printf("%n=== %s (160x50) ===%n", scenario);
        System.out.printf("  Total diffs: %d (continuations skipped: %d)%n", size, continuations);
        System.out.printf("  Cell writes: %d%n", totalMoves);
        System.out.printf("  Cursor moves:%n");
        System.out.printf("    Adjacent (skippable):  %5d (%5.1f%%)%n", adjacent, 100.0 * adjacent / Math.max(1, totalMoves));
        System.out.printf("    Same row (gap):        %5d (%5.1f%%)%n", sameRow, 100.0 * sameRow / Math.max(1, totalMoves));
        System.out.printf("    Different row:         %5d (%5.1f%%)%n", differentRow, 100.0 * differentRow / Math.max(1, totalMoves));
        System.out.printf("  Cursor moves saved: %d / %d = %.1f%% reduction%n",
                skippable, totalMoves - 1, 100.0 * skippable / Math.max(1, totalMoves - 1));
        System.out.printf("  Estimated bytes saved: %d / %d bytes (%.1f%% less I/O)%n",
                bytesSaved, totalMoveBytes, 100.0 * bytesSaved / Math.max(1, totalMoveBytes));
    }
}
