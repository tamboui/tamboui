///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS dev.tamboui:tamboui-tui:LATEST
//DEPS dev.tamboui:tamboui-jline3-backend:LATEST
/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.demo.doom;

import dev.tamboui.buffer.Buffer;
import dev.tamboui.buffer.Cell;
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
import dev.tamboui.tui.event.ResizeEvent;
import dev.tamboui.tui.event.TickEvent;
import dev.tamboui.widgets.block.Block;
import dev.tamboui.widgets.block.BorderType;
import dev.tamboui.widgets.block.Borders;
import dev.tamboui.widgets.block.Title;
import dev.tamboui.widgets.paragraph.Paragraph;

import java.time.Duration;
import java.util.List;

/**
 * Doom-style raycasting demo for TamboUI.
 *
 * <p>Controls:
 * <ul>
 *   <li>W/S or Up/Down - move forward/back</li>
 *   <li>A/D - strafe left/right</li>
 *   <li>Left/Right or H/L - rotate</li>
 *   <li>M - toggle minimap</li>
 *   <li>R - reset position</li>
 *   <li>Q or Ctrl+C - quit</li>
 * </ul>
 */
public class DoomDemo {

    private static final String[] DEFAULT_MAP = {
            "################",
            "#..............#",
            "#..####..####..#",
            "#..#..#..#..#..#",
            "#..#..#..#..#..#",
            "#..####..####..#",
            "#..............#",
            "#..##......##..#",
            "#..##..##..##..#",
            "#......##......#",
            "#..####..####..#",
            "#..#........#..#",
            "#..#..####..#..#",
            "#..#........#..#",
            "#..............#",
            "################"
    };

    private static final int HUD_HEIGHT = 4;
    private static final int MIN_VIEW_WIDTH = 24;
    private static final int MIN_VIEW_HEIGHT = 12;

    private static final double FOV = Math.toRadians(70);
    private static final double MOVE_STEP = 0.18;
    private static final double TURN_STEP = Math.toRadians(7.5);
    private static final double MAX_VIEW_DISTANCE = 18.0;

    private static final Cell[] WALL_SHADES = buildWallShades();
    private static final Cell[] FLOOR_SHADES = buildFloorShades();
    private static final Cell[] CEILING_SHADES = buildCeilingShades();

    private static final Cell MAP_WALL_CELL = cell('#', Style.EMPTY.fg(Color.LIGHT_RED));
    private static final Cell MAP_FLOOR_CELL = cell('.', Style.EMPTY.fg(Color.GRAY));
    private static final Cell MAP_PLAYER_CELL = cell('@', Style.EMPTY.fg(Color.LIGHT_YELLOW));
    private static final Cell MAP_FACING_CELL = cell('*', Style.EMPTY.fg(Color.LIGHT_CYAN));

    private static final Cell MAP_BORDER_H = cell('-', Style.EMPTY.fg(Color.DARK_GRAY));
    private static final Cell MAP_BORDER_V = cell('|', Style.EMPTY.fg(Color.DARK_GRAY));
    private static final Cell MAP_BORDER_CORNER = cell('+', Style.EMPTY.fg(Color.DARK_GRAY));

    private static final Cell CROSSHAIR_CELL = cell('+', Style.EMPTY.fg(Color.LIGHT_CYAN));

    private final DoomEngine engine;
    private long frameCount;
    private boolean showMap = true;

    /**
     * Entry point for the Doom raycaster demo.
     *
     * @param args command line arguments (not used)
     * @throws Exception if the TUI fails to start
     */
    public static void main(String[] args) throws Exception {
        new DoomDemo().run();
    }

    public DoomDemo() {
        engine = DoomEngine.defaultMap();
    }

    public void run() throws Exception {
        var config = TuiConfig.builder()
                .tickRate(Duration.ofMillis(50))
                .build();

        try (var tui = TuiRunner.create(config)) {
            tui.run(this::handleEvent, this::render);
        }
    }

    private boolean handleEvent(Event event, TuiRunner runner) {
        if (event instanceof KeyEvent keyEvent) {
            return handleKeyEvent(keyEvent, runner);
        }
        if (event instanceof TickEvent tickEvent) {
            return handleTickEvent(tickEvent);
        }
        if (event instanceof ResizeEvent) {
            return true;
        }
        return false;
    }

    private boolean handleKeyEvent(KeyEvent key, TuiRunner runner) {
        if (key.isQuit()) {
            runner.quit();
            return false;
        }

        boolean redraw = false;
        double moveStep = key.hasShift() ? MOVE_STEP * 1.7 : MOVE_STEP;

        if (key.isUp() || key.isCharIgnoreCase('w')) {
            redraw |= engine.moveForward(moveStep);
        }
        if (key.isDown() || key.isCharIgnoreCase('s')) {
            redraw |= engine.moveForward(-moveStep);
        }
        if (key.isCharIgnoreCase('a')) {
            redraw |= engine.strafe(-moveStep);
        }
        if (key.isCharIgnoreCase('d')) {
            redraw |= engine.strafe(moveStep);
        }
        if (key.isLeft() || key.isCharIgnoreCase('h')) {
            engine.rotate(-TURN_STEP);
            redraw = true;
        }
        if (key.isRight() || key.isCharIgnoreCase('l')) {
            engine.rotate(TURN_STEP);
            redraw = true;
        }
        if (key.isCharIgnoreCase('m')) {
            showMap = !showMap;
            redraw = true;
        }
        if (key.isCharIgnoreCase('r')) {
            engine.reset();
            redraw = true;
        }

        return redraw;
    }

    private boolean handleTickEvent(TickEvent tickEvent) {
        frameCount = tickEvent.frameCount();
        return true;
    }

    private void render(Frame frame) {
        Rect area = frame.area();
        List<Rect> layout = Layout.vertical()
                .constraints(
                        Constraint.fill(),
                        Constraint.length(HUD_HEIGHT)
                )
                .split(area);

        Rect viewArea = layout.get(0);
        Rect hudArea = layout.get(1);

        Block viewBlock = Block.builder()
                .borders(Borders.ALL)
                .borderType(BorderType.ROUNDED)
                .borderStyle(Style.EMPTY.fg(Color.DARK_GRAY))
                .title(Title.from(
                        Line.from(
                                Span.raw(" Doom Raycaster ").bold().fg(Color.LIGHT_RED),
                                Span.raw("  WASD + arrows ").dim()
                        )
                ))
                .build();

        frame.renderWidget(viewBlock, viewArea);
        Rect inner = viewBlock.inner(viewArea);

        if (inner.width() < MIN_VIEW_WIDTH || inner.height() < MIN_VIEW_HEIGHT) {
            renderTooSmall(frame, inner);
        } else {
            renderWorld(frame.buffer(), inner);
            if (showMap) {
                renderMiniMap(frame.buffer(), inner);
            }
            renderCrosshair(frame.buffer(), inner);
        }

        renderHud(frame, hudArea, inner);
    }

    private void renderTooSmall(Frame frame, Rect area) {
        Text text = Text.from(
                Line.from(Span.raw("Terminal too small for Doom demo.").yellow()),
                Line.from(Span.raw("Resize to at least " + MIN_VIEW_WIDTH + "x" + MIN_VIEW_HEIGHT + ".").dim())
        );

        Paragraph paragraph = Paragraph.builder()
                .text(text)
                .build();

        frame.renderWidget(paragraph, area);
    }

    private void renderHud(Frame frame, Rect area, Rect viewArea) {
        Line controls = Line.from(
                Span.raw("W/S").yellow().bold(),
                Span.raw(" move ").dim(),
                Span.raw("A/D").yellow().bold(),
                Span.raw(" strafe ").dim(),
                Span.raw("Left/Right").yellow().bold(),
                Span.raw(" turn ").dim(),
                Span.raw("M").yellow().bold(),
                Span.raw(" map ").dim(),
                Span.raw("R").yellow().bold(),
                Span.raw(" reset ").dim(),
                Span.raw("Q").yellow().bold(),
                Span.raw(" quit").dim()
        );

        String position = String.format("Pos: %.2f, %.2f", engine.playerX(), engine.playerY());
        String angle = String.format("Angle: %.0f deg", Math.toDegrees(engine.angle()));
        String mapState = showMap ? "Map: on" : "Map: off";
        String view = "View: " + viewArea.width() + "x" + viewArea.height();

        Line status = Line.from(
                Span.raw(position).cyan(),
                Span.raw("  ").dim(),
                Span.raw(angle).magenta(),
                Span.raw("  ").dim(),
                Span.raw(mapState).green(),
                Span.raw("  ").dim(),
                Span.raw(view).dim()
        );

        Block hudBlock = Block.builder()
                .borders(Borders.ALL)
                .borderType(BorderType.PLAIN)
                .borderStyle(Style.EMPTY.fg(Color.DARK_GRAY))
                .title(Title.from("Status"))
                .build();

        Paragraph hud = Paragraph.builder()
                .text(Text.from(controls, status))
                .block(hudBlock)
                .build();

        frame.renderWidget(hud, area);
    }

    private void renderWorld(Buffer buffer, Rect area) {
        int width = area.width();
        int height = area.height();
        if (width <= 0 || height <= 0) {
            return;
        }

        int originX = area.x();
        int originY = area.y();
        int halfHeight = height / 2;
        int maxY = height - 1;

        double pulse = 0.85 + 0.15 * Math.sin(frameCount * 0.12);

        for (int x = 0; x < width; x++) {
            double ratio = width == 1 ? 0.5 : (double) x / (double) (width - 1);
            double rayAngle = engine.angle() - (FOV / 2.0) + (FOV * ratio);

            RaycastHit hit = engine.castRay(rayAngle, MAX_VIEW_DISTANCE);
            double distance = Math.max(0.0001, hit.distance() / pulse);

            int lineHeight = (int) Math.min(height, height / distance);
            int drawStart = Math.max(0, halfHeight - lineHeight / 2);
            int drawEnd = Math.min(maxY, halfHeight + lineHeight / 2);

            int shadeIndex = wallShadeIndex(distance, hit.verticalSide());
            Cell wallCell = WALL_SHADES[shadeIndex];

            for (int y = 0; y < height; y++) {
                int screenX = originX + x;
                int screenY = originY + y;
                Cell cell;
                if (y < drawStart) {
                    cell = ceilingCellFor(y, halfHeight);
                } else if (y <= drawEnd) {
                    cell = wallCell;
                } else {
                    cell = floorCellFor(y, height);
                }
                buffer.set(screenX, screenY, cell);
            }
        }
    }

    private void renderMiniMap(Buffer buffer, Rect area) {
        int mapWidth = engine.mapWidth();
        int mapHeight = engine.mapHeight();
        int totalWidth = mapWidth + 2;
        int totalHeight = mapHeight + 2;

        if (totalWidth > area.width() || totalHeight > area.height()) {
            return;
        }

        int startX = area.x() + 1;
        int startY = area.y() + 1;

        for (int x = 0; x < totalWidth; x++) {
            int screenX = startX - 1 + x;
            buffer.set(screenX, startY - 1, borderCellFor(x, totalWidth, true));
            buffer.set(screenX, startY - 1 + totalHeight - 1, borderCellFor(x, totalWidth, true));
        }

        for (int y = 0; y < totalHeight; y++) {
            int screenY = startY - 1 + y;
            buffer.set(startX - 1, screenY, borderCellFor(y, totalHeight, false));
            buffer.set(startX - 1 + totalWidth - 1, screenY, borderCellFor(y, totalHeight, false));
        }

        for (int y = 0; y < mapHeight; y++) {
            for (int x = 0; x < mapWidth; x++) {
                int screenX = startX + x;
                int screenY = startY + y;
                char cell = engine.mapCell(x, y);
                buffer.set(screenX, screenY, cell == '#' ? MAP_WALL_CELL : MAP_FLOOR_CELL);
            }
        }

        int playerX = clamp((int) engine.playerX(), 0, mapWidth - 1);
        int playerY = clamp((int) engine.playerY(), 0, mapHeight - 1);
        buffer.set(startX + playerX, startY + playerY, MAP_PLAYER_CELL);

        double dirX = Math.cos(engine.angle());
        double dirY = Math.sin(engine.angle());
        for (int step = 1; step <= 3; step++) {
            int rayX = clamp(playerX + (int) Math.round(dirX * step), 0, mapWidth - 1);
            int rayY = clamp(playerY + (int) Math.round(dirY * step), 0, mapHeight - 1);
            buffer.set(startX + rayX, startY + rayY, MAP_FACING_CELL);
        }
    }

    private Cell borderCellFor(int index, int total, boolean horizontal) {
        if (index == 0 || index == total - 1) {
            return MAP_BORDER_CORNER;
        }
        return horizontal ? MAP_BORDER_H : MAP_BORDER_V;
    }

    private void renderCrosshair(Buffer buffer, Rect area) {
        int centerX = area.x() + area.width() / 2;
        int centerY = area.y() + area.height() / 2;
        buffer.set(centerX, centerY, CROSSHAIR_CELL);
    }

    private Cell floorCellFor(int y, int height) {
        double t = (double) (y - height / 2) / Math.max(1, height / 2);
        int index = clamp((int) Math.round(t * (FLOOR_SHADES.length - 1)), 0, FLOOR_SHADES.length - 1);
        return FLOOR_SHADES[index];
    }

    private Cell ceilingCellFor(int y, int halfHeight) {
        double t = 1.0 - (double) y / Math.max(1, halfHeight);
        int index = clamp((int) Math.round(t * (CEILING_SHADES.length - 1)), 0, CEILING_SHADES.length - 1);
        return CEILING_SHADES[index];
    }

    private int wallShadeIndex(double distance, boolean verticalSide) {
        double clamped = Math.min(MAX_VIEW_DISTANCE, Math.max(0.0, distance));
        int index = (int) Math.round((clamped / MAX_VIEW_DISTANCE) * (WALL_SHADES.length - 1));
        if (verticalSide && index < WALL_SHADES.length - 1) {
            index += 1;
        }
        return clamp(index, 0, WALL_SHADES.length - 1);
    }

    private static int clamp(int value, int min, int max) {
        if (value < min) {
            return min;
        }
        if (value > max) {
            return max;
        }
        return value;
    }

    private static Cell[] buildWallShades() {
        return new Cell[] {
                cell('#', Style.EMPTY.fg(Color.LIGHT_RED)),
                cell('X', Style.EMPTY.fg(Color.RED)),
                cell('x', Style.EMPTY.fg(Color.DARK_GRAY)),
                cell('.', Style.EMPTY.fg(Color.GRAY)),
                cell('-', Style.EMPTY.fg(Color.DARK_GRAY))
        };
    }

    private static Cell[] buildFloorShades() {
        return new Cell[] {
                cell('.', Style.EMPTY.fg(Color.GRAY)),
                cell('-', Style.EMPTY.fg(Color.DARK_GRAY)),
                cell(' ', Style.EMPTY)
        };
    }

    private static Cell[] buildCeilingShades() {
        return new Cell[] {
                cell(' ', Style.EMPTY),
                cell('-', Style.EMPTY.fg(Color.DARK_GRAY)),
                cell('.', Style.EMPTY.fg(Color.BLUE))
        };
    }

    private static Cell cell(char symbol, Style style) {
        return new Cell(String.valueOf(symbol), style);
    }

    static final class DoomEngine {
        private final char[][] map;
        private final int width;
        private final int height;
        private final double startX;
        private final double startY;
        private final double startAngle;
        private double playerX;
        private double playerY;
        private double angle;

        DoomEngine(String[] mapRows, double startX, double startY, double startAngle) {
            if (mapRows.length == 0) {
                throw new IllegalArgumentException("Map cannot be empty.");
            }
            this.height = mapRows.length;
            this.width = mapRows[0].length();
            this.map = new char[height][width];
            for (int y = 0; y < height; y++) {
                if (mapRows[y].length() != width) {
                    throw new IllegalArgumentException("Map rows must have equal width.");
                }
                mapRows[y].getChars(0, width, map[y], 0);
            }
            this.startX = startX;
            this.startY = startY;
            this.startAngle = normalizeAngle(startAngle);
            reset();
        }

        static DoomEngine defaultMap() {
            return new DoomEngine(DEFAULT_MAP, 3.5, 3.5, Math.toRadians(45));
        }

        double playerX() {
            return playerX;
        }

        double playerY() {
            return playerY;
        }

        double angle() {
            return angle;
        }

        int mapWidth() {
            return width;
        }

        int mapHeight() {
            return height;
        }

        char mapCell(int x, int y) {
            if (x < 0 || x >= width || y < 0 || y >= height) {
                return '#';
            }
            return map[y][x];
        }

        void reset() {
            playerX = startX;
            playerY = startY;
            angle = startAngle;
        }

        boolean moveForward(double distance) {
            double dx = Math.cos(angle) * distance;
            double dy = Math.sin(angle) * distance;
            return move(dx, dy);
        }

        boolean strafe(double distance) {
            double dx = Math.cos(angle + Math.PI / 2.0) * distance;
            double dy = Math.sin(angle + Math.PI / 2.0) * distance;
            return move(dx, dy);
        }

        boolean move(double dx, double dy) {
            boolean moved = false;
            double nextX = playerX + dx;
            double nextY = playerY + dy;

            if (!isWall(nextX, playerY)) {
                playerX = nextX;
                moved = true;
            }
            if (!isWall(playerX, nextY)) {
                playerY = nextY;
                moved = true;
            }
            return moved;
        }

        void rotate(double delta) {
            angle = normalizeAngle(angle + delta);
        }

        RaycastHit castRay(double rayAngle, double maxDistance) {
            double normalizedAngle = normalizeAngle(rayAngle);
            double rayDirX = Math.cos(normalizedAngle);
            double rayDirY = Math.sin(normalizedAngle);

            int mapX = (int) playerX;
            int mapY = (int) playerY;

            double deltaDistX = rayDirX == 0.0 ? 1.0e30 : Math.abs(1.0 / rayDirX);
            double deltaDistY = rayDirY == 0.0 ? 1.0e30 : Math.abs(1.0 / rayDirY);

            int stepX;
            int stepY;
            double sideDistX;
            double sideDistY;

            if (rayDirX < 0) {
                stepX = -1;
                sideDistX = (playerX - mapX) * deltaDistX;
            } else {
                stepX = 1;
                sideDistX = (mapX + 1.0 - playerX) * deltaDistX;
            }

            if (rayDirY < 0) {
                stepY = -1;
                sideDistY = (playerY - mapY) * deltaDistY;
            } else {
                stepY = 1;
                sideDistY = (mapY + 1.0 - playerY) * deltaDistY;
            }

            int side = 0;
            int steps = 0;
            int maxSteps = width * height;
            double distance = maxDistance;

            while (steps < maxSteps) {
                if (sideDistX < sideDistY) {
                    sideDistX += deltaDistX;
                    mapX += stepX;
                    side = 0;
                } else {
                    sideDistY += deltaDistY;
                    mapY += stepY;
                    side = 1;
                }

                double candidate = side == 0 ? sideDistX - deltaDistX : sideDistY - deltaDistY;
                if (candidate > maxDistance) {
                    distance = maxDistance;
                    break;
                }

                if (isWallCell(mapX, mapY)) {
                    distance = computePerpendicularDistance(rayDirX, rayDirY, mapX, mapY, stepX, stepY, side);
                    break;
                }
                steps++;
            }

            distance = Math.max(0.0001, Math.min(maxDistance, distance));
            return new RaycastHit(distance, side == 0);
        }

        private double computePerpendicularDistance(double rayDirX, double rayDirY,
                                                   int mapX, int mapY, int stepX, int stepY, int side) {
            if (side == 0) {
                double denom = rayDirX == 0.0 ? 1.0e-6 : rayDirX;
                return (mapX - playerX + (1.0 - stepX) / 2.0) / denom;
            }
            double denom = rayDirY == 0.0 ? 1.0e-6 : rayDirY;
            return (mapY - playerY + (1.0 - stepY) / 2.0) / denom;
        }

        private boolean isWall(double x, double y) {
            return isWallCell((int) x, (int) y);
        }

        private boolean isWallCell(int x, int y) {
            return x < 0 || y < 0 || x >= width || y >= height || map[y][x] == '#';
        }

        private double normalizeAngle(double value) {
            double twoPi = Math.PI * 2.0;
            double normalized = value % twoPi;
            if (normalized < 0) {
                normalized += twoPi;
            }
            return normalized;
        }
    }

    record RaycastHit(double distance, boolean verticalSide) {
    }
}
