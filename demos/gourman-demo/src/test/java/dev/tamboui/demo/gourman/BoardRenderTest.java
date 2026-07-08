/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.demo.gourman;

import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;

import dev.tamboui.buffer.Buffer;
import dev.tamboui.layout.Rect;

import static org.assertj.core.api.Assertions.assertThat;

/** Renders the real board widget into a buffer and checks the pellet glyphs are uniform. */
class BoardRenderTest {

    private static final int PANEL_WIDTH = Maze.COLUMNS * 2 + 2;
    private static final int PANEL_HEIGHT = Maze.ROWS + 2;

    @Test
    void everyPelletRendersAsTheSameGlyph() {
        Buffer buffer = renderBoard();
        Maze maze = new Maze();
        IntStream.range(0, Maze.ROWS).forEach(row -> IntStream.range(0, Maze.COLUMNS).forEach(column -> {
            if (!maze.pellet(column, row)) {
                return;
            }
            // Tile (column, row) occupies buffer cells (1 + 2*column, 1 + row) and its right
            // neighbor, inside the block border. The pellet is a fullwidth dot spanning both
            // cells (glyph + continuation), so the mark sits dead center in the tile.
            assertThat(buffer.get(1 + column * 2, 1 + row).symbol())
                    .as("pellet glyph at %d,%d", column, row).isEqualTo("・");
            assertThat(buffer.get(1 + column * 2 + 1, 1 + row).isContinuation())
                    .as("continuation cell of pellet tile %d,%d", column, row).isTrue();
        }));
    }

    @Test
    void dumpBoard() {
        Buffer buffer = renderBoard();
        StringBuilder dump = new StringBuilder();
        IntStream.range(0, PANEL_HEIGHT).forEach(y -> {
            IntStream.range(0, PANEL_WIDTH).forEach(x -> {
                String symbol = buffer.get(x, y).symbol();
                dump.append(symbol.isEmpty() ? " " : symbol);
            });
            dump.append('\n');
        });
        System.out.println(dump);
    }

    private static Buffer renderBoard() {
        GourmanGame game = new GourmanGame();
        Buffer buffer = Buffer.empty(new Rect(0, 0, PANEL_WIDTH, PANEL_HEIGHT));
        game.boardCanvas(true).render(buffer.area(), buffer);
        return buffer;
    }

}
