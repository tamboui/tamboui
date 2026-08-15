/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.demo.gourman;

import java.util.stream.IntStream;

/**
 * The classic 28x31 maze: static walls plus the per-level pellet state. {@code #} is a wall,
 * {@code .} a pellet, {@code o} a power pellet, {@code -} the ghost-house door and spaces are
 * open corridor. The tunnel row wraps horizontally at both edges.
 */
final class Maze {

    static final int COLUMNS = 28;
    static final int ROWS = 31;
    static final int TUNNEL_ROW = 14;

    private static final char WALL = '#';
    private static final char DOOR = '-';
    private static final char PELLET = '.';
    private static final char POWER = 'o';

    private static final String[] LAYOUT = {
            "############################",
            "#............##............#",
            "#.####.#####.##.#####.####.#",
            "#o####.#####.##.#####.####o#",
            "#.####.#####.##.#####.####.#",
            "#..........................#",
            "#.####.##.########.##.####.#",
            "#.####.##.########.##.####.#",
            "#......##....##....##......#",
            "######.##### ## #####.######",
            "     #.##### ## #####.#     ",
            "     #.##          ##.#     ",
            "     #.## ###--### ##.#     ",
            "######.## #      # ##.######",
            "      .   #      #   .      ",
            "######.## #      # ##.######",
            "     #.## ######## ##.#     ",
            "     #.##          ##.#     ",
            "     #.## ######## ##.#     ",
            "######.## ######## ##.######",
            "#............##............#",
            "#.####.#####.##.#####.####.#",
            "#.####.#####.##.#####.####.#",
            "#o..##.......  .......##..o#",
            "###.##.##.########.##.##.###",
            "###.##.##.########.##.##.###",
            "#......##....##....##......#",
            "#.##########.##.##########.#",
            "#.##########.##.##########.#",
            "#..........................#",
            "############################" };

    private final boolean[][] pellets = new boolean[ROWS][COLUMNS];
    private final boolean[][] powerPellets = new boolean[ROWS][COLUMNS];
    private int pelletCount;

    Maze() {
        resetPellets();
    }

    /** Restores every pellet and power pellet, for a new level or a new game. */
    void resetPellets() {
        pelletCount = 0;
        IntStream.range(0, ROWS).forEach(row -> IntStream.range(0, COLUMNS).forEach(column -> {
            char tile = LAYOUT[row].charAt(column);
            pellets[row][column] = tile == PELLET;
            powerPellets[row][column] = tile == POWER;
            if (tile == PELLET || tile == POWER) {
                pelletCount++;
            }
        }));
    }

    static boolean wall(int column, int row) {
        return LAYOUT[row].charAt(column) == WALL;
    }

    static boolean door(int column, int row) {
        return LAYOUT[row].charAt(column) == DOOR;
    }

    /** Whether an actor may occupy the tile. The ghost-house door blocks everyone. */
    static boolean passable(int column, int row) {
        return !wall(column, row) && !door(column, row);
    }

    /** Wraps a column index through the side tunnels. */
    static int wrapColumn(int column) {
        return Math.floorMod(column, COLUMNS);
    }

    boolean pellet(int column, int row) {
        return pellets[row][column];
    }

    boolean powerPellet(int column, int row) {
        return powerPellets[row][column];
    }

    /** Removes and reports the pellet on the tile: 0 = none, 1 = pellet, 2 = power pellet. */
    int eat(int column, int row) {
        if (pellets[row][column]) {
            pellets[row][column] = false;
            pelletCount--;
            return 1;
        }
        if (powerPellets[row][column]) {
            powerPellets[row][column] = false;
            pelletCount--;
            return 2;
        }
        return 0;
    }

    int pelletCount() {
        return pelletCount;
    }

}
