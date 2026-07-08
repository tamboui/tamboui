/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.demo.gourman;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.IntStream;

import dev.tamboui.layout.Constraint;
import dev.tamboui.layout.Layout;
import dev.tamboui.layout.Rect;
import dev.tamboui.style.Color;
import dev.tamboui.style.Style;
import dev.tamboui.terminal.Frame;
import dev.tamboui.text.Line;
import dev.tamboui.text.Span;
import dev.tamboui.text.Text;
import dev.tamboui.tui.TuiConfig;
import dev.tamboui.tui.TuiRunner;
import dev.tamboui.tui.event.Event;
import dev.tamboui.tui.event.KeyEvent;
import dev.tamboui.tui.event.TickEvent;
import dev.tamboui.widgets.block.Block;
import dev.tamboui.widgets.block.BorderType;
import dev.tamboui.widgets.block.Borders;
import dev.tamboui.widgets.block.Title;
import dev.tamboui.widgets.canvas.Canvas;
import dev.tamboui.widgets.canvas.Context;
import dev.tamboui.widgets.canvas.Marker;
import dev.tamboui.widgets.canvas.shapes.Points;
import dev.tamboui.widgets.paragraph.Paragraph;

/**
 * Full-screen terminal Gourman, rendered with TamboUI's canvas.
 *
 * <p>
 * Movement is tile-based: Gourman and each ghost step one tile per their own interval, driven by
 * the tick event. Turns are buffered — press a direction early and Gourman turns at the next tile
 * where it fits. Ghosts alternate scatter and chase phases with their arcade personalities
 * (Blinky chases, Pinky ambushes four tiles ahead, Inky doubles Blinky's pincer vector, Clyde
 * chases only from afar), run away randomly while frightened, and race home as eyes when eaten.
 * Eating a power pellet is worth 200/400/800/1600 per ghost in a chain; a level ends when every
 * pellet is gone, and each level speeds everyone up.
 * </p>
 */
public final class GourmanGame {

    /** Creates a game; call {@link #run()} to start it. */
    public GourmanGame() {
    }

    private static final Duration TICK_RATE = Duration.ofMillis(33);
    private static final double NANOS_PER_SECOND = 1_000_000_000.0;

    // Base movement intervals; each level multiplies them by SPEEDUP_PER_LEVEL (floored).
    private static final long GOURMAN_INTERVAL_MILLIS = 130;
    private static final long GHOST_INTERVAL_MILLIS = 140;
    private static final long FRIGHTENED_INTERVAL_MILLIS = 210;
    private static final long EATEN_INTERVAL_MILLIS = 50;
    private static final double SPEEDUP_PER_LEVEL = 0.94;
    private static final double MIN_SPEED_FACTOR = 0.5;

    // Ghost mode schedule: scatter for 7s, then chase for 20s, repeating from each life/level start.
    private static final long SCATTER_SECONDS = 7;
    private static final long CHASE_SECONDS = 20;
    private static final long FRIGHTENED_SECONDS = 6;
    private static final long FRIGHTENED_FLASH_SECONDS = 2;

    private static final Duration READY_PAUSE = Duration.ofSeconds(2);
    private static final Duration DEATH_PAUSE = Duration.ofMillis(1500);
    private static final Duration LEVEL_CLEAR_PAUSE = Duration.ofMillis(1500);
    private static final Duration EATEN_GHOST_REST = Duration.ofSeconds(1);

    private static final int PELLET_POINTS = 10;
    private static final int POWER_PELLET_POINTS = 50;
    private static final int GHOST_BASE_POINTS = 200;
    private static final int EXTRA_LIFE_SCORE = 10_000;
    private static final int STARTING_LIVES = 3;
    // Clyde switches from chasing to his scatter corner within this many tiles of Gourman.
    private static final int CLYDE_SHY_DISTANCE = 8;
    // Pinky aims this many tiles ahead of Gourman; Inky's pincer pivots two tiles ahead.
    private static final int PINKY_LOOKAHEAD = 4;
    private static final int INKY_LOOKAHEAD = 2;

    // Start tiles: Gourman below the ghost house, Blinky just above the door, the rest inside.
    private static final int GOURMAN_START_COLUMN = 13;
    private static final int GOURMAN_START_ROW = 23;
    private static final int DOOR_EXIT_COLUMN = 13;
    private static final int DOOR_EXIT_ROW = 11;
    private static final int HOUSE_ROW = 14;
    private static final int[] HOUSE_COLUMNS = { 13, 11, 15 };

    // Canvas resolution: 2x2 half-block pixels per maze tile, so tiles render square.
    private static final int CELL_PIXELS = 2;
    private static final int PIXELS_X = Maze.COLUMNS * CELL_PIXELS;
    private static final int PIXELS_Y = Maze.ROWS * CELL_PIXELS;
    private static final int BOARD_PANEL_WIDTH = PIXELS_X + 2;
    private static final int BOARD_PANEL_HEIGHT = Maze.ROWS + 2;
    private static final int SIDE_PANEL_WIDTH = 24;
    // Banner row: the empty stretch below the ghost house, where the arcade prints READY!.
    private static final int BANNER_ROW = 17;

    private static final Color WALL_COLOR = Color.rgb(66, 66, 255);
    private static final Color DOOR_COLOR = Color.rgb(255, 184, 255);
    private static final Color PELLET_COLOR = Color.rgb(255, 183, 174);
    private static final Color GOURMAN_COLOR = Color.YELLOW;
    private static final Color FRIGHTENED_COLOR = Color.rgb(60, 60, 255);
    private static final Color EYES_COLOR = Color.WHITE;
    private static final long BLINK_NANOS = Duration.ofMillis(400).toNanos();

    /** What the fixed pauses between play are showing. PLAYING is the only phase that moves. */
    private enum Phase {
        READY, PLAYING, DYING, LEVEL_CLEAR, GAME_OVER
    }

    private final Maze maze = new Maze();
    private final Random random = new Random();
    private final List<Ghost> ghosts = new ArrayList<>();
    private final List<GameListener> listeners = new CopyOnWriteArrayList<>();

    private int gourmanColumn;
    private int gourmanRow;
    private Direction gourmanDirection = Direction.LEFT;
    private Direction desiredDirection = Direction.LEFT;
    private long nextGourmanMoveNanos;

    private int score;
    private int lives;
    private int level;
    private boolean extraLifeAwarded;
    private boolean paused;
    private Phase phase = Phase.READY;
    private long phaseUntilNanos;
    private long modeStartNanos;
    private long frightenedUntilNanos;
    private int ghostChain;

    /**
     * Subscribes a listener to every {@link GameEvent} this game emits. Listeners run on the
     * game loop thread and must not block; see {@link GameListener}.
     *
     * @param listener the listener to subscribe
     */
    public void addListener(GameListener listener) {
        listeners.add(Objects.requireNonNull(listener, "listener"));
    }

    private void publish(GameEvent event) {
        for (GameListener listener : listeners) {
            try {
                listener.onEvent(event);
            } catch (RuntimeException e) {
                // A misbehaving listener must not break the game loop, and stdout belongs to the
                // TUI, so there is nowhere to report it; skip the listener for this event.
            }
        }
    }

    /** Runs the game full-screen until the player quits; blocks the calling thread. */
    public void run() {
        newGame();
        try (TuiRunner tui = TuiRunner.create(TuiConfig.builder().tickRate(TICK_RATE).build())) {
            tui.run(this::handleEvent, this::render);
        } catch (Exception e) {
            throw new GourmanException("Gourman failed: " + e.getMessage(), e);
        }
    }

    // --- Game state ---

    /** Starts a fresh game. Package-visible so tests can drive the game without a terminal. */
    void newGame() {
        maze.resetPellets();
        score = 0;
        lives = STARTING_LIVES;
        level = 1;
        extraLifeAwarded = false;
        paused = false;
        resetPositions();
        publish(new GameEvent.GameStarted(level, lives));
    }

    /** Puts every actor back on its start tile and freezes play for the READY pause. */
    private void resetPositions() {
        long now = System.nanoTime();
        gourmanColumn = GOURMAN_START_COLUMN;
        gourmanRow = GOURMAN_START_ROW;
        gourmanDirection = Direction.LEFT;
        desiredDirection = Direction.LEFT;
        ghosts.clear();
        Ghost.Kind[] kinds = Ghost.Kind.values();
        IntStream.range(0, kinds.length).forEach(i -> {
            Ghost ghost = new Ghost(kinds[i]);
            if (kinds[i] == Ghost.Kind.BLINKY) {
                ghost.position(DOOR_EXIT_COLUMN, DOOR_EXIT_ROW);
                ghost.state(Ghost.State.ACTIVE);
            } else {
                ghost.position(HOUSE_COLUMNS[i - 1], HOUSE_ROW);
                ghost.releaseAtNanos(now + kinds[i].releaseSeconds() * (long) NANOS_PER_SECOND);
            }
            ghosts.add(ghost);
        });
        frightenedUntilNanos = 0;
        ghostChain = 0;
        phase = Phase.READY;
        phaseUntilNanos = now + READY_PAUSE.toNanos();
    }

    private double speedFactor() {
        return Math.max(MIN_SPEED_FACTOR, Math.pow(SPEEDUP_PER_LEVEL, level - 1));
    }

    private long interval(long baseMillis) {
        return (long) (baseMillis * speedFactor() * Duration.ofMillis(1).toNanos());
    }

    private void tick() {
        if (paused || phase == Phase.GAME_OVER) {
            return;
        }
        long now = System.nanoTime();
        if (phase != Phase.PLAYING) {
            if (now >= phaseUntilNanos) {
                advancePhase(now);
            }
            return;
        }
        if (frightenedUntilNanos != 0 && now >= frightenedUntilNanos) {
            frightenedUntilNanos = 0;
            ghosts.forEach(ghost -> ghost.frightened(false));
            publish(new GameEvent.FrightenedEnded());
        }
        while (now >= nextGourmanMoveNanos && phase == Phase.PLAYING) {
            nextGourmanMoveNanos += interval(GOURMAN_INTERVAL_MILLIS);
            moveGourman(now);
        }
        for (Ghost ghost : ghosts) {
            while (now >= ghost.nextMoveNanos() && phase == Phase.PLAYING) {
                ghost.nextMoveNanos(ghost.nextMoveNanos() + interval(ghostIntervalMillis(ghost)));
                moveGhost(ghost, now);
            }
        }
    }

    private long ghostIntervalMillis(Ghost ghost) {
        if (ghost.state() == Ghost.State.EATEN) {
            return EATEN_INTERVAL_MILLIS;
        }
        if (ghost.frightened()) {
            return FRIGHTENED_INTERVAL_MILLIS;
        }
        return GHOST_INTERVAL_MILLIS;
    }

    private void advancePhase(long now) {
        if (phase == Phase.READY) {
            phase = Phase.PLAYING;
            modeStartNanos = now;
            nextGourmanMoveNanos = now;
            ghosts.forEach(ghost -> ghost.nextMoveNanos(now));
            publish(new GameEvent.LevelStarted(level));
            return;
        }
        if (phase == Phase.DYING) {
            if (lives > 0) {
                resetPositions();
            } else {
                phase = Phase.GAME_OVER;
                publish(new GameEvent.GameOver(score, level));
            }
            return;
        }
        if (phase == Phase.LEVEL_CLEAR) {
            level++;
            maze.resetPellets();
            resetPositions();
        }
    }

    /** One Gourman step. Package-visible so tests can drive the game without a terminal. */
    void moveGourman(long now) {
        if (canMove(gourmanColumn, gourmanRow, desiredDirection)) {
            gourmanDirection = desiredDirection;
        }
        if (canMove(gourmanColumn, gourmanRow, gourmanDirection)) {
            gourmanColumn = Maze.wrapColumn(gourmanColumn + gourmanDirection.dx());
            gourmanRow += gourmanDirection.dy();
            publish(new GameEvent.GourmanMoved(gourmanColumn, gourmanRow, gourmanDirection));
            eatPellet(now);
        }
        checkCollisions(now);
    }

    private void eatPellet(long now) {
        int eaten = maze.eat(gourmanColumn, gourmanRow);
        if (eaten == 1) {
            publish(new GameEvent.PelletEaten(gourmanColumn, gourmanRow, maze.pelletCount()));
            addScore(PELLET_POINTS);
        } else if (eaten == 2) {
            publish(new GameEvent.PowerPelletEaten(gourmanColumn, gourmanRow));
            addScore(POWER_PELLET_POINTS);
            frightenGhosts(now);
        }
        if (eaten != 0 && maze.pelletCount() == 0) {
            phase = Phase.LEVEL_CLEAR;
            phaseUntilNanos = now + LEVEL_CLEAR_PAUSE.toNanos();
            publish(new GameEvent.LevelCleared(level, score));
        }
    }

    private void frightenGhosts(long now) {
        frightenedUntilNanos = now + FRIGHTENED_SECONDS * (long) NANOS_PER_SECOND;
        ghostChain = 0;
        for (Ghost ghost : ghosts) {
            if (ghost.state() != Ghost.State.EATEN) {
                ghost.frightened(true);
                ghost.direction(ghost.direction().opposite());
            }
        }
        publish(new GameEvent.GhostsFrightened(FRIGHTENED_SECONDS));
    }

    private void addScore(int points) {
        score += points;
        publish(new GameEvent.ScoreChanged(points, score));
        if (!extraLifeAwarded && score >= EXTRA_LIFE_SCORE) {
            extraLifeAwarded = true;
            lives++;
            publish(new GameEvent.ExtraLifeAwarded(score, lives));
        }
    }

    private static boolean canMove(int column, int row, Direction direction) {
        int targetColumn = Maze.wrapColumn(column + direction.dx());
        int targetRow = row + direction.dy();
        if (targetRow < 0 || targetRow >= Maze.ROWS) {
            return false;
        }
        return Maze.passable(targetColumn, targetRow);
    }

    // --- Ghost movement ---

    private void moveGhost(Ghost ghost, long now) {
        if (ghost.state() == Ghost.State.WAITING) {
            if (now >= ghost.releaseAtNanos()) {
                ghost.position(DOOR_EXIT_COLUMN, DOOR_EXIT_ROW);
                ghost.direction(Direction.LEFT);
                ghost.state(Ghost.State.ACTIVE);
                publish(new GameEvent.GhostReleased(ghost.kind()));
            }
            return;
        }
        Direction choice = chooseDirection(ghost);
        ghost.direction(choice);
        ghost.position(Maze.wrapColumn(ghost.column() + choice.dx()), ghost.row() + choice.dy());
        publish(new GameEvent.GhostMoved(ghost.kind(), ghost.column(), ghost.row(), ghost.state(),
                ghost.frightened()));
        if (ghost.state() == Ghost.State.EATEN && ghost.column() == DOOR_EXIT_COLUMN && ghost.row() == DOOR_EXIT_ROW) {
            // The eyes reached the doorstep: drop back into the house for a short rest.
            ghost.position(HOUSE_COLUMNS[0], HOUSE_ROW);
            ghost.state(Ghost.State.WAITING);
            ghost.releaseAtNanos(now + EATEN_GHOST_REST.toNanos());
            publish(new GameEvent.GhostReturned(ghost.kind()));
        }
        checkCollisions(now);
    }

    private Direction chooseDirection(Ghost ghost) {
        List<Direction> options = new ArrayList<>(EnumSet.allOf(Direction.class));
        options.removeIf(direction -> direction == ghost.direction().opposite()
                || !canMove(ghost.column(), ghost.row(), direction));
        if (options.isEmpty()) {
            return ghost.direction().opposite();
        }
        if (ghost.frightened() && ghost.state() != Ghost.State.EATEN) {
            return options.get(random.nextInt(options.size()));
        }
        int[] target = target(ghost);
        Direction best = options.get(0);
        long bestDistance = Long.MAX_VALUE;
        for (Direction option : options) {
            long distance = squaredDistance(Maze.wrapColumn(ghost.column() + option.dx()),
                    ghost.row() + option.dy(), target[0], target[1]);
            if (distance < bestDistance) {
                bestDistance = distance;
                best = option;
            }
        }
        return best;
    }

    /** The tile a ghost steers toward, per the arcade targeting rules (simplified to full tiles). */
    private int[] target(Ghost ghost) {
        if (ghost.state() == Ghost.State.EATEN) {
            return new int[] { DOOR_EXIT_COLUMN, DOOR_EXIT_ROW };
        }
        if (!chaseMode()) {
            return new int[] { ghost.kind().scatterColumn(), ghost.kind().scatterRow() };
        }
        switch (ghost.kind()) {
        case PINKY:
            return new int[] { gourmanColumn + PINKY_LOOKAHEAD * gourmanDirection.dx(),
                    gourmanRow + PINKY_LOOKAHEAD * gourmanDirection.dy() };
        case INKY:
            return inkyTarget();
        case CLYDE:
            if (squaredDistance(ghost.column(), ghost.row(), gourmanColumn,
                    gourmanRow) > (long) CLYDE_SHY_DISTANCE * CLYDE_SHY_DISTANCE) {
                return new int[] { gourmanColumn, gourmanRow };
            }
            return new int[] { ghost.kind().scatterColumn(), ghost.kind().scatterRow() };
        default:
            return new int[] { gourmanColumn, gourmanRow };
        }
    }

    /** Inky's pincer: double the vector from Blinky to the tile two ahead of Gourman. */
    private int[] inkyTarget() {
        Ghost blinky = ghosts.get(0);
        int pivotColumn = gourmanColumn + INKY_LOOKAHEAD * gourmanDirection.dx();
        int pivotRow = gourmanRow + INKY_LOOKAHEAD * gourmanDirection.dy();
        return new int[] { 2 * pivotColumn - blinky.column(), 2 * pivotRow - blinky.row() };
    }

    private boolean chaseMode() {
        long elapsedSeconds = (long) ((System.nanoTime() - modeStartNanos) / NANOS_PER_SECOND);
        return elapsedSeconds % (SCATTER_SECONDS + CHASE_SECONDS) >= SCATTER_SECONDS;
    }

    private static long squaredDistance(int column, int row, int targetColumn, int targetRow) {
        long dc = column - (long) targetColumn;
        long dr = row - (long) targetRow;
        return dc * dc + dr * dr;
    }

    // --- Collisions ---

    private void checkCollisions(long now) {
        for (Ghost ghost : ghosts) {
            if (ghost.column() != gourmanColumn || ghost.row() != gourmanRow
                    || ghost.state() != Ghost.State.ACTIVE) {
                continue;
            }
            if (ghost.frightened()) {
                eatGhost(ghost);
            } else {
                lives--;
                phase = Phase.DYING;
                phaseUntilNanos = now + DEATH_PAUSE.toNanos();
                publish(new GameEvent.GourmanDied(lives));
                return;
            }
        }
    }

    private void eatGhost(Ghost ghost) {
        int points = GHOST_BASE_POINTS << ghostChain;
        ghostChain++;
        ghost.frightened(false);
        ghost.state(Ghost.State.EATEN);
        publish(new GameEvent.GhostEaten(ghost.kind(), points, ghostChain));
        addScore(points);
    }

    // --- Events ---

    private boolean handleEvent(Event event, TuiRunner runner) {
        if (event instanceof KeyEvent key) {
            handleKey(key, runner);
            return true;
        }
        if (event instanceof TickEvent) {
            tick();
            return true;
        }
        return false;
    }

    private void handleKey(KeyEvent key, TuiRunner runner) {
        if (key.isQuit() || key.isCharIgnoreCase('q')) {
            runner.quit();
            return;
        }
        if (key.isCharIgnoreCase('r')) {
            newGame();
            return;
        }
        if (key.isCharIgnoreCase('p')) {
            boolean wasPaused = paused;
            paused = !paused && phase != Phase.GAME_OVER;
            if (paused != wasPaused) {
                publish(paused ? new GameEvent.GamePaused() : new GameEvent.GameResumed());
            }
            return;
        }
        if (key.isLeft() || key.isCharIgnoreCase('h')) {
            desiredDirection = Direction.LEFT;
        } else if (key.isRight() || key.isCharIgnoreCase('l')) {
            desiredDirection = Direction.RIGHT;
        } else if (key.isUp() || key.isCharIgnoreCase('k')) {
            desiredDirection = Direction.UP;
        } else if (key.isDown() || key.isCharIgnoreCase('j')) {
            desiredDirection = Direction.DOWN;
        }
    }

    // --- Rendering ---

    private void render(Frame frame) {
        Rect area = frame.area();
        // The board must get its exact size: the canvas maps a fixed 56x62 pixel grid onto the
        // panel, so a smaller area merges adjacent pixel rows/columns into the same character -
        // walls bleed into corridors and the maze silently distorts. Refuse to render distorted;
        // shed the block border and the side panel first, and past that show a resize hint.
        if (area.width() < PIXELS_X || area.height() < Maze.ROWS) {
            renderTooSmall(frame, area);
            return;
        }
        // The rounded border costs 2 rows/columns; drop it when the terminal is that tight
        // (32 rows is a common default height, one short of the bordered board's 33).
        boolean bordered = area.width() >= BOARD_PANEL_WIDTH && area.height() >= BOARD_PANEL_HEIGHT;
        int boardWidth = bordered ? BOARD_PANEL_WIDTH : PIXELS_X;
        int boardHeight = bordered ? BOARD_PANEL_HEIGHT : Maze.ROWS;
        List<Rect> rows = Layout.vertical()
                .constraints(Constraint.fill(), Constraint.length(boardHeight), Constraint.fill())
                .split(area);
        if (area.width() < boardWidth + 1 + SIDE_PANEL_WIDTH) {
            List<Rect> columns = Layout.horizontal()
                    .constraints(Constraint.fill(), Constraint.length(boardWidth), Constraint.fill())
                    .split(rows.get(1));
            frame.renderWidget(boardCanvas(bordered), columns.get(1));
            return;
        }
        List<Rect> columns = Layout.horizontal()
                .constraints(Constraint.fill(), Constraint.length(boardWidth),
                        Constraint.length(SIDE_PANEL_WIDTH), Constraint.fill())
                .spacing(1)
                .split(rows.get(1));
        frame.renderWidget(boardCanvas(bordered), columns.get(1));
        renderSidePanel(frame, columns.get(2));
    }

    private static void renderTooSmall(Frame frame, Rect area) {
        List<Line> message = new ArrayList<>();
        message.add(Line.from(Span.raw(" Terminal too small").bold().red()));
        message.add(Line.from(Span.raw(" Gourman needs at least " + PIXELS_X + "x" + Maze.ROWS
                + " characters; this window is " + area.width() + "x" + area.height() + ".")));
        message.add(Line.from(Span.raw(" Enlarge the window or reduce the font size (Cmd -).").dim()));
        frame.renderWidget(Paragraph.builder().text(Text.from(message)).build(), area);
    }

    /** The board widget; package-visible so tests can render it headlessly into a buffer. */
    Canvas boardCanvas(boolean bordered) {
        Color borderColor = phase == Phase.GAME_OVER ? Color.RED : Color.CYAN;
        Canvas.Builder builder = Canvas.builder()
                // Bounds span [0, N-1] for N pixels so integer coordinates map 1:1 onto grid
                // cells; a [0, N] span aliases and blocks shrink at some positions.
                .xBounds(0, PIXELS_X - 1)
                .yBounds(0, PIXELS_Y - 1)
                .marker(Marker.HALF_BLOCK)
                .paint(this::paintBoard);
        if (bordered) {
            builder.block(roundedBlock(borderColor, " GOURMAN "));
        }
        return builder.build();
    }

    private void paintBoard(Context context) {
        long now = System.nanoTime();
        paintMaze(context, now);
        for (Ghost ghost : ghosts) {
            paintGhost(context, ghost, now);
        }
        if (phase != Phase.DYING || blinkOn(now)) {
            paintGourman(context, now);
        }
        paintBanner(context);
    }

    private void paintMaze(Context context, long now) {
        IntStream.range(0, Maze.ROWS).forEach(row -> IntStream.range(0, Maze.COLUMNS).forEach(column -> {
            if (Maze.wall(column, row)) {
                paintWallEdges(context, column, row);
            } else if (Maze.door(column, row)) {
                paintPixel(context, column * CELL_PIXELS, row * CELL_PIXELS, DOOR_COLOR);
                paintPixel(context, column * CELL_PIXELS + 1, row * CELL_PIXELS, DOOR_COLOR);
            } else if (maze.pellet(column, row)) {
                // Text labels overlay the pixel grid, so pellets are crisp round dots that can
                // never be confused with the half-block quanta the wall outlines are made of.
                if (!occupied(column, row)) {
                    printTile(context, column, row, Span.styled("・", Style.EMPTY.fg(PELLET_COLOR).bold()));
                }
            } else if (maze.powerPellet(column, row) && blinkOn(now) && !occupied(column, row)) {
                printTile(context, column, row, Span.styled("〇", Style.EMPTY.fg(PELLET_COLOR).bold()));
            }
        }));
    }

    /**
     * Walls render as outlines, not filled regions: only wall tiles that border open space
     * (corridor or void) are painted, leaving wall interiors black. The outline is traced at
     * character granularity — each exposed half-tile column paints both of its half-block
     * pixels — because mixing half-height edges with full-height corners makes straight walls
     * look crenellated (the corner characters bulge out of the line every few tiles).
     */
    private static void paintWallEdges(Context context, int column, int row) {
        IntStream.range(0, CELL_PIXELS).forEach(sx -> {
            int hx = sx == 0 ? -1 : 1;
            boolean exposed = openForWallTracing(column + hx, row)
                    || openForWallTracing(column, row - 1) || openForWallTracing(column, row + 1)
                    || openForWallTracing(column + hx, row - 1) || openForWallTracing(column + hx, row + 1);
            if (exposed) {
                paintPixel(context, column * CELL_PIXELS + sx, row * CELL_PIXELS, WALL_COLOR);
                paintPixel(context, column * CELL_PIXELS + sx, row * CELL_PIXELS + 1, WALL_COLOR);
            }
        });
    }

    /** Out-of-bounds counts as wall so the maze border draws a single line on its inner side. */
    private static boolean openForWallTracing(int column, int row) {
        if (column < 0 || column >= Maze.COLUMNS || row < 0 || row >= Maze.ROWS) {
            return false;
        }
        return !Maze.wall(column, row);
    }

    /** Gourman chomps: one mouth pixel on the side he is facing blinks open and shut. */
    private void paintGourman(Context context, long now) {
        IntStream.range(0, CELL_PIXELS * CELL_PIXELS).forEach(i -> {
            int sx = i % CELL_PIXELS;
            int sy = i / CELL_PIXELS;
            if (phase == Phase.PLAYING && blinkOn(now) && isMouthPixel(sx, sy)) {
                return;
            }
            paintPixel(context, gourmanColumn * CELL_PIXELS + sx, gourmanRow * CELL_PIXELS + sy, GOURMAN_COLOR);
        });
    }

    private boolean isMouthPixel(int sx, int sy) {
        switch (gourmanDirection) {
        case LEFT:
            return sx == 0 && sy == 0;
        case RIGHT:
            return sx == 1 && sy == 0;
        case UP:
            return sx == 0 && sy == 0;
        default:
            return sx == 1 && sy == 1;
        }
    }

    private void paintGhost(Context context, Ghost ghost, long now) {
        if (ghost.state() == Ghost.State.EATEN) {
            // Just the eyes racing home.
            paintPixel(context, ghost.column() * CELL_PIXELS, ghost.row() * CELL_PIXELS, EYES_COLOR);
            paintPixel(context, ghost.column() * CELL_PIXELS + 1, ghost.row() * CELL_PIXELS, EYES_COLOR);
            return;
        }
        Color color = ghost.kind().color();
        if (ghost.frightened()) {
            boolean flashing = frightenedUntilNanos - now < FRIGHTENED_FLASH_SECONDS * (long) NANOS_PER_SECOND;
            color = flashing && blinkOn(now) ? Color.WHITE : FRIGHTENED_COLOR;
        }
        // No eyes overlay: the half-block renderer shows only the TOP pixel's color when both
        // halves of a character are painted, so white top pixels would repaint the whole ghost
        // white. Solid bodies it is; Gourman stands out through his chomping mouth instead.
        paintCell(context, ghost.column(), ghost.row(), color);
    }

    /** Whether an actor is standing on the tile (its pellet dot would print on top of it). */
    private boolean occupied(int column, int row) {
        for (Ghost ghost : ghosts) {
            if (ghost.column() == column && ghost.row() == row) {
                return true;
            }
        }
        return gourmanColumn == column && gourmanRow == row;
    }

    /**
     * Prints a glyph over a tile (labels overlay the pixel grid). Tiles are two characters wide
     * with no middle character, so pellet glyphs are FULLWIDTH (East Asian wide) characters:
     * they occupy both cells of the tile with the mark rendered dead center, which is the only
     * way to truly center a dot in an even-width tile.
     */
    private static void printTile(Context context, int column, int row, Span glyph) {
        context.print(column * CELL_PIXELS, PIXELS_Y - 1.0 - row * CELL_PIXELS, Line.from(glyph));
    }

    private static boolean blinkOn(long now) {
        return now / BLINK_NANOS % 2 == 0;
    }

    /** Fills one maze tile as a square of canvas pixels (half-block marker: 2 per terminal row). */
    private static void paintCell(Context context, int column, int row, Color color) {
        double[][] points = new double[CELL_PIXELS * CELL_PIXELS][];
        IntStream.range(0, CELL_PIXELS * CELL_PIXELS).forEach(i -> {
            int px = column * CELL_PIXELS + i % CELL_PIXELS;
            int py = row * CELL_PIXELS + i / CELL_PIXELS;
            points[i] = new double[] { px, PIXELS_Y - 1 - py };
        });
        context.draw(Points.of(points, color));
    }

    private static void paintPixel(Context context, int px, int py, Color color) {
        // The canvas y-axis points up; board rows count down from the top.
        context.draw(Points.of(new double[][] { { px, PIXELS_Y - 1 - py } }, color));
    }

    private void paintBanner(Context context) {
        if (paused) {
            printCentered(context, "PAUSED", Color.YELLOW, true);
        } else if (phase == Phase.READY) {
            printCentered(context, "READY!", Color.YELLOW, false);
        } else if (phase == Phase.GAME_OVER) {
            printCentered(context, "GAME  OVER", Color.RED, true);
        } else if (phase == Phase.LEVEL_CLEAR) {
            printCentered(context, "LEVEL " + level + " CLEAR", Color.GREEN, false);
        }
    }

    private static void printCentered(Context context, String text, Color color, boolean reversed) {
        Style style = Style.EMPTY.fg(color).bold();
        if (reversed) {
            style = style.reversed();
        }
        double x = Math.max(0, (PIXELS_X - text.length()) / 2.0);
        context.print(x, PIXELS_Y - 1.0 - BANNER_ROW * CELL_PIXELS, Line.from(Span.styled(text, style)));
    }

    private void renderSidePanel(Frame frame, Rect area) {
        List<Line> panelLines = new ArrayList<>();
        panelLines.add(statLine("Score", Integer.toString(score)));
        panelLines.add(statLine("Level", Integer.toString(level)));
        panelLines.add(livesLine());
        panelLines.add(Line.from(Span.raw("")));
        panelLines.add(Line.from(Span.raw(" Pellets left  ").dim(),
                Span.raw(Integer.toString(maze.pelletCount())).bold().white()));
        panelLines.add(Line.from(Span.raw("")));
        panelLines.add(keyLine("← → ↑ ↓ / hjkl", "move"));
        panelLines.add(keyLine("p", "pause"));
        panelLines.add(keyLine("r", "restart"));
        panelLines.add(keyLine("q", "quit"));
        Paragraph panel = Paragraph.builder()
                .text(Text.from(panelLines))
                .block(roundedBlock(Color.DARK_GRAY, null))
                .build();
        frame.renderWidget(panel, area);
    }

    private Line livesLine() {
        String[] icons = new String[Math.max(0, lives)];
        Arrays.fill(icons, "●");
        return Line.from(Span.raw(" Lives  ").dim(),
                Span.raw(String.join(" ", icons)).fg(GOURMAN_COLOR).bold());
    }

    private static Line statLine(String label, String value) {
        return Line.from(Span.raw(" " + label + "  ").dim(), Span.raw(value).bold().white());
    }

    private static Line keyLine(String keys, String action) {
        return Line.from(Span.raw(" " + keys).yellow(), Span.raw("  " + action).dim());
    }

    private static Block roundedBlock(Color color, String title) {
        Block.Builder builder = Block.builder()
                .borders(Borders.ALL)
                .borderType(BorderType.ROUNDED)
                .borderStyle(Style.EMPTY.fg(color));
        if (title != null) {
            builder.title(Title.from(Line.from(Span.styled(title, Style.EMPTY.fg(color).bold()))));
        }
        return builder.build();
    }

}
