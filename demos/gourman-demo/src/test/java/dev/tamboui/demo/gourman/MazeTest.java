/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.demo.gourman;

import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MazeTest {

    @Test
    void layoutIsExactlyTwentyEightByThirtyOne() {
        IntStream.range(0, Maze.ROWS).forEach(row -> IntStream.range(0, Maze.COLUMNS)
                .forEach(column -> assertThat(Maze.wall(column, row) || Maze.door(column, row)
                        || Maze.passable(column, row)).isTrue()));
    }

    @Test
    void borderIsWalledExceptForTheTunnel() {
        // Rows 10-18 are the inset middle section: their edge tiles are void space outside the
        // playfield, enclosed by walls, so only the tunnel row must be open at the border there.
        IntStream.range(0, Maze.ROWS).filter(row -> row < 10 || row > 18).forEach(row -> {
            assertThat(Maze.passable(0, row)).as("row %d left", row).isFalse();
            assertThat(Maze.passable(Maze.COLUMNS - 1, row)).as("row %d right", row).isFalse();
        });
        assertThat(Maze.passable(0, Maze.TUNNEL_ROW)).isTrue();
        assertThat(Maze.passable(Maze.COLUMNS - 1, Maze.TUNNEL_ROW)).isTrue();
    }

    @Test
    void tunnelPocketsAreSealedFromTheVoid() {
        // From the tunnel, the rows above and below the outer stretch must be walls so actors
        // can never step off the playfield into the void pockets.
        IntStream.range(0, 6).forEach(column -> {
            assertThat(Maze.passable(column, Maze.TUNNEL_ROW - 1)).as("above col %d", column).isFalse();
            assertThat(Maze.passable(column, Maze.TUNNEL_ROW + 1)).as("below col %d", column).isFalse();
            assertThat(Maze.passable(Maze.COLUMNS - 1 - column, Maze.TUNNEL_ROW - 1)).isFalse();
            assertThat(Maze.passable(Maze.COLUMNS - 1 - column, Maze.TUNNEL_ROW + 1)).isFalse();
        });
    }

    @Test
    void tunnelWrapsColumns() {
        assertThat(Maze.wrapColumn(-1)).isEqualTo(Maze.COLUMNS - 1);
        assertThat(Maze.wrapColumn(Maze.COLUMNS)).isZero();
    }

    @Test
    void mazeHasFourPowerPelletsAndPlentyOfPellets() {
        Maze maze = new Maze();
        long power = count(maze, true);
        long pellets = count(maze, false);
        assertThat(power).isEqualTo(4);
        assertThat(pellets).isGreaterThan(200);
        assertThat(maze.pelletCount()).isEqualTo(power + pellets);
    }

    @Test
    void eatingRemovesAndCountsDown() {
        Maze maze = new Maze();
        int before = maze.pelletCount();
        assertThat(maze.eat(1, 1)).isEqualTo(1);
        assertThat(maze.eat(1, 1)).isZero();
        assertThat(maze.eat(1, 3)).isEqualTo(2);
        assertThat(maze.pelletCount()).isEqualTo(before - 2);
        maze.resetPellets();
        assertThat(maze.pelletCount()).isEqualTo(before);
    }

    @Test
    void startTilesAreOpen() {
        assertThat(Maze.passable(13, 23)).isTrue();
        assertThat(Maze.passable(13, 11)).isTrue();
        assertThat(Maze.passable(13, 14)).isTrue();
        assertThat(Maze.door(13, 12)).isTrue();
        assertThat(Maze.door(14, 12)).isTrue();
    }

    private static long count(Maze maze, boolean power) {
        return IntStream.range(0, Maze.ROWS)
                .mapToLong(row -> IntStream.range(0, Maze.COLUMNS)
                        .filter(column -> power ? maze.powerPellet(column, row) : maze.pellet(column, row))
                        .count())
                .sum();
    }

}
