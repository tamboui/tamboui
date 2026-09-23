/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.demo.gourman;

/**
 * An event emitted by {@link GourmanGame}. Every state change in a game publishes one: lifecycle
 * (start, level, pause, game over), every actor move, every pellet, every score change. Consumers
 * subscribe with {@link GourmanGame#addListener(GameListener)} and can forward the stream anywhere
 * — a metrics sink, a replay log, or a message broker such as Kafka.
 *
 * <p>
 * The hierarchy is sealed so consumers can switch over events exhaustively with pattern matching.
 * Events are immutable records carrying tile coordinates ({@code column}/{@code row} on the 28x31
 * maze grid) and are not timestamped: they are emitted synchronously as the change happens, so
 * listeners stamp them with whatever clock their transport needs.
 * </p>
 */
public sealed interface GameEvent {

    /**
     * A new game began (initial start or restart), with its starting level and lives.
     *
     * @param level the starting level
     * @param lives the starting number of lives
     */
    record GameStarted(int level, int lives) implements GameEvent {
    }

    /**
     * The READY pause ended and play began for the given level (also after each lost life).
     *
     * @param level the level now being played
     */
    record LevelStarted(int level) implements GameEvent {
    }

    /**
     * Every pellet was eaten; the level is complete.
     *
     * @param level the level that was cleared
     * @param score the score at clear time
     */
    record LevelCleared(int level, int score) implements GameEvent {
    }

    /**
     * Gourman moved one tile.
     *
     * @param column the tile column moved to
     * @param row the tile row moved to
     * @param direction the direction of the step
     */
    record GourmanMoved(int column, int row, Direction direction) implements GameEvent {
    }

    /**
     * Gourman ate a pellet; {@code remaining} counts pellets (of both kinds) left on the maze.
     *
     * @param column the tile column of the pellet
     * @param row the tile row of the pellet
     * @param remaining pellets (of both kinds) left on the maze
     */
    record PelletEaten(int column, int row, int remaining) implements GameEvent {
    }

    /**
     * Gourman ate a power pellet; a {@link GhostsFrightened} event follows.
     *
     * @param column the tile column of the power pellet
     * @param row the tile row of the power pellet
     */
    record PowerPelletEaten(int column, int row) implements GameEvent {
    }

    /**
     * All roaming ghosts turned frightened for the given duration.
     *
     * @param durationSeconds how long the frightened period lasts
     */
    record GhostsFrightened(long durationSeconds) implements GameEvent {
    }

    /** The frightened period expired with ghosts still uneaten. */
    record FrightenedEnded() implements GameEvent {
    }

    /**
     * A ghost moved one tile.
     *
     * @param ghost the ghost that moved
     * @param column the tile column moved to
     * @param row the tile row moved to
     * @param state the ghost state after the move
     * @param frightened whether the ghost is currently frightened
     */
    record GhostMoved(Ghost.Kind ghost, int column, int row, Ghost.State state, boolean frightened)
            implements GameEvent {
    }

    /**
     * A ghost left the ghost house and started roaming.
     *
     * @param ghost the ghost that left the house
     */
    record GhostReleased(Ghost.Kind ghost) implements GameEvent {
    }

    /**
     * Gourman ate a frightened ghost; {@code chain} counts ghosts eaten on this power pellet.
     *
     * @param ghost the ghost that was eaten
     * @param points the points awarded
     * @param chain how many ghosts this power pellet has claimed so far
     */
    record GhostEaten(Ghost.Kind ghost, int points, int chain) implements GameEvent {
    }

    /**
     * An eaten ghost's eyes reached the ghost house and it will respawn.
     *
     * @param ghost the ghost whose eyes reached the house
     */
    record GhostReturned(Ghost.Kind ghost) implements GameEvent {
    }

    /**
     * A ghost caught Gourman.
     *
     * @param livesRemaining lives left after this death
     */
    record GourmanDied(int livesRemaining) implements GameEvent {
    }

    /**
     * The score changed by {@code points}, to {@code score}. Accompanies every scoring event.
     *
     * @param points the points just added
     * @param score the new total score
     */
    record ScoreChanged(int points, int score) implements GameEvent {
    }

    /**
     * The bonus life threshold was crossed.
     *
     * @param score the score that crossed the threshold
     * @param lives the new number of lives
     */
    record ExtraLifeAwarded(int score, int lives) implements GameEvent {
    }

    /** The player paused the game. */
    record GamePaused() implements GameEvent {
    }

    /** The player resumed the game. */
    record GameResumed() implements GameEvent {
    }

    /**
     * The last life was lost.
     *
     * @param score the final score
     * @param level the level reached
     */
    record GameOver(int score, int level) implements GameEvent {
    }

}
