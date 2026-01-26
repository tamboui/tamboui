///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS dev.tamboui:tamboui-tui:LATEST
//DEPS dev.tamboui:tamboui-image:LATEST
//DEPS dev.tamboui:tamboui-jline3-backend:LATEST
/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.demo.doom;

import dev.tamboui.buffer.Buffer;
import dev.tamboui.buffer.Cell;
import dev.tamboui.image.Image;
import dev.tamboui.image.ImageData;
import dev.tamboui.image.ImageScaling;
import dev.tamboui.image.capability.TerminalImageCapabilities;
import dev.tamboui.image.protocol.BrailleProtocol;
import dev.tamboui.image.protocol.HalfBlockProtocol;
import dev.tamboui.image.protocol.ITermProtocol;
import dev.tamboui.image.protocol.ImageProtocol;
import dev.tamboui.image.protocol.KittyProtocol;
import dev.tamboui.image.protocol.SixelProtocol;
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

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Doom-style raycasting demo for TamboUI.
 *
 * <p>Controls:
 * <ul>
 *   <li>W/S or Up/Down - move forward/back</li>
 *   <li>A/D - strafe left/right</li>
 *   <li>Left/Right or H/L - rotate</li>
 *   <li>M - toggle minimap</li>
 *   <li>V - toggle render mode (ASCII/Block/Image)</li>
 *   <li>C - toggle color output</li>
 *   <li>P - cycle image protocol (Image mode only)</li>
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

    private static final int DEFAULT_MAP_SCALE = 64;
    private static final int MIN_VIEW_WIDTH = 24;
    private static final int MIN_VIEW_HEIGHT = 12;

    private static final double FOV = Math.toRadians(70);
    private static final double MOVE_STEP = 0.18;
    private static final double TURN_STEP = Math.toRadians(7.5);

    private static final Cell[] WALL_SHADES = buildWallShades(true);
    private static final Cell[] WALL_SHADES_MONO = buildWallShades(false);
    private static final Cell[] FLOOR_SHADES = buildFloorShades(true);
    private static final Cell[] FLOOR_SHADES_MONO = buildFloorShades(false);
    private static final Cell[] CEILING_SHADES = buildCeilingShades(true);
    private static final Cell[] CEILING_SHADES_MONO = buildCeilingShades(false);

    private static final Cell MAP_WALL_CELL = cell('#', Style.EMPTY.fg(Color.LIGHT_RED));
    private static final Cell MAP_FLOOR_CELL = cell('.', Style.EMPTY.fg(Color.GRAY));
    private static final Cell MAP_PLAYER_CELL = cell('@', Style.EMPTY.fg(Color.LIGHT_YELLOW));
    private static final Cell MAP_FACING_CELL = cell('*', Style.EMPTY.fg(Color.LIGHT_CYAN));

    private static final Cell MAP_BORDER_H = cell('-', Style.EMPTY.fg(Color.DARK_GRAY));
    private static final Cell MAP_BORDER_V = cell('|', Style.EMPTY.fg(Color.DARK_GRAY));
    private static final Cell MAP_BORDER_CORNER = cell('+', Style.EMPTY.fg(Color.DARK_GRAY));

    private static final Cell CROSSHAIR_CELL = cell('+', Style.EMPTY.fg(Color.LIGHT_CYAN));
    private static final String BLOCK_UPPER = "\u2580";

    private static final Color.Rgb WALL_BASE = new Color.Rgb(200, 80, 50);
    private static final Color.Rgb FLOOR_BASE = new Color.Rgb(38, 36, 42);
    private static final Color.Rgb CEILING_BASE = new Color.Rgb(30, 40, 70);

    private final DoomEngine engine;
    private final String mapInfo;
    private final int mapScale;
    private final WadTexture wallTexture;
    private final String wallTextureName;
    private ImageProtocol imageProtocol;
    private final List<String> warnings;
    private RenderMode renderMode;
    private boolean useColor;
    private boolean showMap;

    /**
     * Entry point for the Doom raycaster demo.
     *
     * @param args command line arguments (not used)
     * @throws Exception if the TUI fails to start
     */
    public static void main(String[] args) throws Exception {
        DemoConfig config = DemoConfig.parseArgs(args);
        new DoomDemo(config).run();
    }

    public DoomDemo(DemoConfig config) {
        MapLoadResult loadResult = loadMap(config);
        MapData map = loadResult.map();
        this.engine = new DoomEngine(map.map(), map.startX(), map.startY(), map.startAngle());
        this.mapInfo = buildMapInfo(map);
        this.mapScale = map.scale();
        this.wallTexture = loadResult.wallTexture();
        this.wallTextureName = loadResult.wallTextureName();
        this.warnings = loadResult.warnings();
        this.renderMode = config.renderMode();
        this.useColor = config.useColor();
        this.showMap = config.showMap();
        this.imageProtocol = resolveImageProtocol(config.imageProtocol());
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
        if (key.isCharIgnoreCase('v')) {
            renderMode = renderMode.toggle();
            redraw = true;
        }
        if (key.isCharIgnoreCase('c')) {
            useColor = !useColor;
            redraw = true;
        }
        if (key.isCharIgnoreCase('p') && renderMode == RenderMode.IMAGE) {
            imageProtocol = cycleProtocol(imageProtocol);
            redraw = true;
        }
        if (key.isCharIgnoreCase('r')) {
            engine.reset();
            redraw = true;
        }

        return redraw;
    }

    private boolean handleTickEvent(TickEvent tickEvent) {
        return true;
    }

    private void render(Frame frame) {
        Rect area = frame.area();
        int hudHeight = 6 + warnings.size();
        List<Rect> layout = Layout.vertical()
                .constraints(
                        Constraint.fill(),
                        Constraint.length(hudHeight)
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
            RenderMode mode = effectiveRenderMode();
            if (mode == RenderMode.IMAGE) {
                renderWorldImage(frame, inner);
            } else if (mode == RenderMode.BLOCK) {
                renderWorldBlocks(frame.buffer(), inner);
            } else {
                renderWorldAscii(frame.buffer(), inner);
            }
            if (showMap && mode != RenderMode.IMAGE) {
                renderMiniMap(frame.buffer(), inner);
            }
            if (mode != RenderMode.IMAGE) {
                renderCrosshair(frame.buffer(), inner);
            }
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
                Span.raw("V").yellow().bold(),
                Span.raw(" mode ").dim(),
                Span.raw("C").yellow().bold(),
                Span.raw(" color ").dim(),
                Span.raw("P").yellow().bold(),
                Span.raw(" protocol ").dim(),
                Span.raw("Q").yellow().bold(),
                Span.raw(" quit").dim()
        );

        String position = String.format("Pos: %.2f, %.2f", engine.playerX(), engine.playerY());
        String angle = String.format("Angle: %.0f deg", Math.toDegrees(engine.angle()));
        String mapState = showMap ? "Map: on" : "Map: off";
        String renderState = "Mode: " + effectiveRenderMode().label();
        String colorState = useColor ? "Color: on" : "Color: off";
        String textureState = wallTextureName == null ? "Texture: none" : "Texture: " + wallTextureName;
        String protocolState = effectiveRenderMode() == RenderMode.IMAGE
                ? "Protocol: " + imageProtocol.name()
                : null;
        String scaleState = mapScale > 1 ? "Scale: " + mapScale : null;
        String view = "View: " + viewArea.width() + "x" + viewArea.height();

        Line status = Line.from(
                Span.raw(position).cyan(),
                Span.raw("  ").dim(),
                Span.raw(angle).magenta(),
                Span.raw("  ").dim(),
                Span.raw(mapState).green(),
                Span.raw("  ").dim(),
                Span.raw(renderState).yellow(),
                Span.raw("  ").dim(),
                Span.raw(colorState).yellow(),
                Span.raw("  ").dim(),
                Span.raw(view).dim()
        );

        List<Line> lines = new ArrayList<>();
        lines.add(controls);
        lines.add(status);
        Line info = Line.from(
                Span.raw(mapInfo).cyan(),
                Span.raw("  " + textureState).yellow(),
                Span.raw(scaleState == null ? "" : "  " + scaleState).dim(),
                Span.raw(protocolState == null ? "" : "  " + protocolState).dim()
        );
        lines.add(info);
        for (String warning : warnings) {
            lines.add(Line.from(Span.raw(warning).fg(Color.LIGHT_RED).dim()));
        }

        Block hudBlock = Block.builder()
                .borders(Borders.ALL)
                .borderType(BorderType.PLAIN)
                .borderStyle(Style.EMPTY.fg(Color.DARK_GRAY))
                .title(Title.from("Status"))
                .build();

        Paragraph hud = Paragraph.builder()
                .text(Text.from(lines))
                .block(hudBlock)
                .build();

        frame.renderWidget(hud, area);
    }

    private void renderWorldImage(Frame frame, Rect area) {
        int width = area.width();
        int height = area.height();
        if (width <= 0 || height <= 0) {
            return;
        }

        int pixelHeight = height * 2;
        int[] pixels = new int[width * pixelHeight];
        int halfHeight = pixelHeight / 2;
        int maxPixel = pixelHeight - 1;
        double maxDistance = engine.maxViewDistance();

        for (int x = 0; x < width; x++) {
            double ratio = width == 1 ? 0.5 : (double) x / (double) (width - 1);
            double rayAngle = engine.angle() - (FOV / 2.0) + (FOV * ratio);
            double rayDirX = Math.cos(rayAngle);
            double rayDirY = Math.sin(rayAngle);

            RaycastHit hit = engine.castRay(rayAngle, maxDistance);
            double distance = Math.max(0.0001, hit.distance());

            int lineHeight = (int) Math.min(pixelHeight, pixelHeight / distance);
            int drawStart = Math.max(0, halfHeight - lineHeight / 2);
            int drawEnd = Math.min(maxPixel, halfHeight + lineHeight / 2);

            double wallX = hit.verticalSide() ? hit.hitY() : hit.hitX();
            wallX -= Math.floor(wallX);
            if (hit.verticalSide() && rayDirX > 0) {
                wallX = 1.0 - wallX;
            }
            if (!hit.verticalSide() && rayDirY < 0) {
                wallX = 1.0 - wallX;
            }

            int texX = wallTexture == null ? 0
                    : clamp((int) Math.floor(wallX * wallTexture.width()), 0, wallTexture.width() - 1);

            for (int y = 0; y < pixelHeight; y++) {
                int index = y * width + x;
                if (y < drawStart) {
                    pixels[index] = argb(ceilingColor(y, pixelHeight));
                } else if (y <= drawEnd) {
                    int argb = wallTexture == null
                            ? argb(wallColor(distance, hit.verticalSide(), maxDistance))
                            : wallTexture.sample(texX, textureY(y, drawStart, lineHeight, wallTexture.height()));
                    if (wallTexture == null || ImageData.isVisible(argb)) {
                        pixels[index] = argb;
                    } else {
                        pixels[index] = argb(wallColor(distance, hit.verticalSide(), maxDistance));
                    }
                } else {
                    pixels[index] = argb(floorColor(y, pixelHeight));
                }
            }
        }

        ImageData data = imageDataFromPixels(width, pixelHeight, pixels);
        Image image = Image.builder()
                .data(data)
                .scaling(ImageScaling.STRETCH)
                .protocol(imageProtocol)
                .build();
        frame.renderWidget(image, area);
    }

    private void renderWorldAscii(Buffer buffer, Rect area) {
        int width = area.width();
        int height = area.height();
        if (width <= 0 || height <= 0) {
            return;
        }

        Cell[] wallShades = useColor ? WALL_SHADES : WALL_SHADES_MONO;
        Cell[] floorShades = useColor ? FLOOR_SHADES : FLOOR_SHADES_MONO;
        Cell[] ceilingShades = useColor ? CEILING_SHADES : CEILING_SHADES_MONO;

        int originX = area.x();
        int originY = area.y();
        int halfHeight = height / 2;
        int maxY = height - 1;
        double maxDistance = engine.maxViewDistance();

        for (int x = 0; x < width; x++) {
            double ratio = width == 1 ? 0.5 : (double) x / (double) (width - 1);
            double rayAngle = engine.angle() - (FOV / 2.0) + (FOV * ratio);

            RaycastHit hit = engine.castRay(rayAngle, maxDistance);
            double distance = Math.max(0.0001, hit.distance());

            int lineHeight = (int) Math.min(height, height / distance);
            int drawStart = Math.max(0, halfHeight - lineHeight / 2);
            int drawEnd = Math.min(maxY, halfHeight + lineHeight / 2);

            int shadeIndex = wallShadeIndex(distance, hit.verticalSide(), maxDistance, wallShades.length);
            Cell wallCell = wallShades[shadeIndex];

            for (int y = 0; y < height; y++) {
                int screenX = originX + x;
                int screenY = originY + y;
                Cell cell;
                if (y < drawStart) {
                    cell = ceilingCellFor(y, halfHeight, ceilingShades);
                } else if (y <= drawEnd) {
                    cell = wallCell;
                } else {
                    cell = floorCellFor(y, height, floorShades);
                }
                buffer.set(screenX, screenY, cell);
            }
        }
    }

    private void renderWorldBlocks(Buffer buffer, Rect area) {
        int width = area.width();
        int height = area.height();
        if (width <= 0 || height <= 0) {
            return;
        }

        int originX = area.x();
        int originY = area.y();
        int pixelHeight = height * 2;
        int halfHeight = pixelHeight / 2;
        int maxPixel = pixelHeight - 1;
        double maxDistance = engine.maxViewDistance();

        for (int x = 0; x < width; x++) {
            double ratio = width == 1 ? 0.5 : (double) x / (double) (width - 1);
            double rayAngle = engine.angle() - (FOV / 2.0) + (FOV * ratio);

            RaycastHit hit = engine.castRay(rayAngle, maxDistance);
            double distance = Math.max(0.0001, hit.distance());

            int lineHeight = (int) Math.min(pixelHeight, pixelHeight / distance);
            int drawStart = Math.max(0, halfHeight - lineHeight / 2);
            int drawEnd = Math.min(maxPixel, halfHeight + lineHeight / 2);

            for (int y = 0; y < height; y++) {
                int topPixel = y * 2;
                int bottomPixel = topPixel + 1;
                Color topColor = pixelColor(topPixel, drawStart, drawEnd, distance, hit.verticalSide(), maxDistance, pixelHeight);
                Color bottomColor = pixelColor(bottomPixel, drawStart, drawEnd, distance, hit.verticalSide(), maxDistance, pixelHeight);
                buffer.set(originX + x, originY + y, blockCell(topColor, bottomColor));
            }
        }
    }

    private RenderMode effectiveRenderMode() {
        if (!useColor && (renderMode == RenderMode.BLOCK || renderMode == RenderMode.IMAGE)) {
            return RenderMode.ASCII;
        }
        if (renderMode == RenderMode.IMAGE && wallTexture == null) {
            return RenderMode.BLOCK;
        }
        return renderMode;
    }

    private Color pixelColor(int pixelY, int drawStart, int drawEnd, double distance,
                             boolean verticalSide, double maxDistance, int pixelHeight) {
        if (pixelY < drawStart) {
            return ceilingColor(pixelY, pixelHeight);
        }
        if (pixelY <= drawEnd) {
            return wallColor(distance, verticalSide, maxDistance);
        }
        return floorColor(pixelY, pixelHeight);
    }

    private Color wallColor(double distance, boolean verticalSide, double maxDistance) {
        double shade = 1.0 - clamp01(distance / maxDistance);
        double brightness = 0.25 + 0.75 * shade;
        if (verticalSide) {
            brightness *= 0.8;
        }
        return shadeColor(WALL_BASE, brightness);
    }

    private Color ceilingColor(int pixelY, int pixelHeight) {
        int half = Math.max(1, pixelHeight / 2);
        double t = 1.0 - (double) pixelY / half;
        double brightness = 0.2 + 0.8 * clamp01(t);
        return shadeColor(CEILING_BASE, brightness);
    }

    private Color floorColor(int pixelY, int pixelHeight) {
        int half = Math.max(1, pixelHeight / 2);
        double t = (double) (pixelY - half) / half;
        double brightness = 0.2 + 0.8 * clamp01(t);
        return shadeColor(FLOOR_BASE, brightness);
    }

    private Cell blockCell(Color top, Color bottom) {
        return new Cell(BLOCK_UPPER, Style.EMPTY.fg(top).bg(bottom));
    }

    private void renderMiniMap(Buffer buffer, Rect area) {
        int mapWidth = engine.mapWidth();
        int mapHeight = engine.mapHeight();
        int innerWidth = area.width() - 2;
        int innerHeight = area.height() - 2;
        if (innerWidth <= 0 || innerHeight <= 0) {
            return;
        }

        int windowWidth = Math.min(mapWidth, innerWidth);
        int windowHeight = Math.min(mapHeight, innerHeight);
        int playerX = clamp((int) engine.playerX(), 0, mapWidth - 1);
        int playerY = clamp((int) engine.playerY(), 0, mapHeight - 1);

        int mapStartX = clamp(playerX - windowWidth / 2, 0, mapWidth - windowWidth);
        int mapStartY = clamp(playerY - windowHeight / 2, 0, mapHeight - windowHeight);

        int startX = area.x() + 1;
        int startY = area.y() + 1;
        int totalWidth = windowWidth + 2;
        int totalHeight = windowHeight + 2;

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

        for (int y = 0; y < windowHeight; y++) {
            for (int x = 0; x < windowWidth; x++) {
                int mapX = mapStartX + x;
                int mapY = mapStartY + y;
                int screenX = startX + x;
                int screenY = startY + y;
                char cell = engine.mapCell(mapX, mapY);
                buffer.set(screenX, screenY, cell == '#' ? MAP_WALL_CELL : MAP_FLOOR_CELL);
            }
        }

        int playerScreenX = startX + (playerX - mapStartX);
        int playerScreenY = startY + (playerY - mapStartY);
        buffer.set(playerScreenX, playerScreenY, MAP_PLAYER_CELL);

        double dirX = Math.cos(engine.angle());
        double dirY = Math.sin(engine.angle());
        for (int step = 1; step <= 3; step++) {
            int rayX = clamp(playerX + (int) Math.round(dirX * step), 0, mapWidth - 1);
            int rayY = clamp(playerY + (int) Math.round(dirY * step), 0, mapHeight - 1);
            int screenX = startX + (rayX - mapStartX);
            int screenY = startY + (rayY - mapStartY);
            if (screenX >= startX && screenX < startX + windowWidth
                    && screenY >= startY && screenY < startY + windowHeight) {
                buffer.set(screenX, screenY, MAP_FACING_CELL);
            }
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

    private Cell floorCellFor(int y, int height, Cell[] floorShades) {
        double t = (double) (y - height / 2) / Math.max(1, height / 2);
        int index = clamp((int) Math.round(t * (floorShades.length - 1)), 0, floorShades.length - 1);
        return floorShades[index];
    }

    private Cell ceilingCellFor(int y, int halfHeight, Cell[] ceilingShades) {
        double t = 1.0 - (double) y / Math.max(1, halfHeight);
        int index = clamp((int) Math.round(t * (ceilingShades.length - 1)), 0, ceilingShades.length - 1);
        return ceilingShades[index];
    }

    private int wallShadeIndex(double distance, boolean verticalSide, double maxDistance, int shadeCount) {
        double clamped = Math.min(maxDistance, Math.max(0.0, distance));
        int index = (int) Math.round((clamped / maxDistance) * (shadeCount - 1));
        if (verticalSide && index < shadeCount - 1) {
            index += 1;
        }
        return clamp(index, 0, shadeCount - 1);
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

    private static double clamp01(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }

    private static Color.Rgb shadeColor(Color.Rgb base, double factor) {
        double clamped = clamp01(factor);
        int r = (int) Math.round(base.r() * clamped);
        int g = (int) Math.round(base.g() * clamped);
        int b = (int) Math.round(base.b() * clamped);
        return new Color.Rgb(r, g, b);
    }

    private static int textureY(int pixelY, int drawStart, int lineHeight, int textureHeight) {
        if (lineHeight <= 0 || textureHeight <= 0) {
            return 0;
        }
        double ratio = (pixelY - drawStart) / (double) lineHeight;
        int texY = (int) Math.floor(ratio * textureHeight);
        return clamp(texY, 0, textureHeight - 1);
    }

    private static int argb(Color color) {
        Color.Rgb rgb = color.toRgb();
        return 0xFF000000 | (rgb.r() << 16) | (rgb.g() << 8) | rgb.b();
    }

    private static ImageData imageDataFromPixels(int width, int height, int[] pixels) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        image.setRGB(0, 0, width, height, pixels, 0, width);
        return ImageData.fromBufferedImage(image);
    }

    private static Cell[] buildWallShades(boolean useColor) {
        if (!useColor) {
            return new Cell[] {
                    cell('#', Style.EMPTY),
                    cell('X', Style.EMPTY),
                    cell('x', Style.EMPTY),
                    cell('.', Style.EMPTY),
                    cell('-', Style.EMPTY)
            };
        }
        return new Cell[] {
                cell('#', Style.EMPTY.fg(Color.LIGHT_RED)),
                cell('X', Style.EMPTY.fg(Color.RED)),
                cell('x', Style.EMPTY.fg(Color.DARK_GRAY)),
                cell('.', Style.EMPTY.fg(Color.GRAY)),
                cell('-', Style.EMPTY.fg(Color.DARK_GRAY))
        };
    }

    private static Cell[] buildFloorShades(boolean useColor) {
        if (!useColor) {
            return new Cell[] {
                    cell('.', Style.EMPTY),
                    cell('-', Style.EMPTY),
                    cell(' ', Style.EMPTY)
            };
        }
        return new Cell[] {
                cell('.', Style.EMPTY.fg(Color.GRAY)),
                cell('-', Style.EMPTY.fg(Color.DARK_GRAY)),
                cell(' ', Style.EMPTY)
        };
    }

    private static Cell[] buildCeilingShades(boolean useColor) {
        if (!useColor) {
            return new Cell[] {
                    cell(' ', Style.EMPTY),
                    cell('.', Style.EMPTY),
                    cell('-', Style.EMPTY)
            };
        }
        return new Cell[] {
                cell(' ', Style.EMPTY),
                cell('-', Style.EMPTY.fg(Color.DARK_GRAY)),
                cell('.', Style.EMPTY.fg(Color.BLUE))
        };
    }

    private static Cell cell(char symbol, Style style) {
        return new Cell(String.valueOf(symbol), style);
    }

    private static char[][] toCharMap(String[] mapRows) {
        if (mapRows.length == 0) {
            throw new IllegalArgumentException("Map cannot be empty.");
        }
        int width = mapRows[0].length();
        char[][] map = new char[mapRows.length][width];
        for (int y = 0; y < mapRows.length; y++) {
            if (mapRows[y].length() != width) {
                throw new IllegalArgumentException("Map rows must have equal width.");
            }
            mapRows[y].getChars(0, width, map[y], 0);
        }
        return map;
    }

    private static String buildMapInfo(MapData map) {
        String source = map.source();
        String name = map.mapName();
        if (source == null || source.isEmpty()) {
            return "Map: " + name;
        }
        return source + " " + name;
    }

    private static MapLoadResult loadMap(DemoConfig config) {
        List<String> warnings = new ArrayList<>();
        if (config.wadPath() == null) {
            return new MapLoadResult(defaultMapData(), null, null, warnings);
        }

        Path wadPath = Paths.get(config.wadPath());
        try {
            WadFile wad = WadFile.open(wadPath);
            String mapName = config.mapName();
            if (mapName == null) {
                mapName = wad.defaultMapName();
            }
            WadMap map = wad.loadMap(mapName);
            MapData data = WadRasterizer.rasterize(map, config.mapScale(), wadPath.getFileName().toString());

            WadTexture wallTexture = null;
            String textureName = config.textureName();
            try {
                WadTextureSet textures = wad.textureSet();
                if (textureName == null) {
                    textureName = textures.defaultTextureName();
                }
                if (textureName != null) {
                    wallTexture = textures.texture(textureName);
                }
                if (wallTexture == null && textureName != null) {
                    warnings.add("Texture not found: " + textureName);
                }
            } catch (Exception e) {
                warnings.add("Texture load failed: " + e.getMessage());
            }

            return new MapLoadResult(data, wallTexture, textureName, warnings);
        } catch (Exception e) {
            warnings.add("WAD load failed: " + e.getMessage());
            return new MapLoadResult(defaultMapData(), null, null, warnings);
        }
    }

    private static MapData defaultMapData() {
        char[][] map = toCharMap(DEFAULT_MAP);
        return new MapData(map, 3.5, 3.5, Math.toRadians(45), "Default", "Built-in", 1);
    }

    private static ImageProtocol resolveImageProtocol(String value) {
        if (value == null) {
            return new HalfBlockProtocol();
        }
        switch (value.toLowerCase(Locale.ROOT)) {
            case "half":
                return new HalfBlockProtocol();
            case "braille":
                return new BrailleProtocol();
            case "kitty":
                return new KittyProtocol();
            case "sixel":
                return new SixelProtocol();
            case "iterm":
                return new ITermProtocol();
            case "auto":
                return TerminalImageCapabilities.detect().bestProtocol();
            default:
                return new HalfBlockProtocol();
        }
    }

    private static ImageProtocol cycleProtocol(ImageProtocol current) {
        if (current instanceof HalfBlockProtocol) {
            return new BrailleProtocol();
        }
        if (current instanceof BrailleProtocol) {
            return new KittyProtocol();
        }
        if (current instanceof KittyProtocol) {
            return new SixelProtocol();
        }
        if (current instanceof SixelProtocol) {
            return new ITermProtocol();
        }
        if (current instanceof ITermProtocol) {
            return TerminalImageCapabilities.detect().bestProtocol();
        }
        // For auto-detected protocol, cycle back to half
        return new HalfBlockProtocol();
    }

    private enum RenderMode {
        ASCII("ASCII"),
        BLOCK("Block"),
        IMAGE("Image");

        private final String label;

        RenderMode(String label) {
            this.label = label;
        }

        String label() {
            return label;
        }

        RenderMode toggle() {
            switch (this) {
                case ASCII:
                    return BLOCK;
                case BLOCK:
                    return IMAGE;
                default:
                    return ASCII;
            }
        }

        static RenderMode parse(String value) {
            if (value == null) {
                throw new IllegalArgumentException("Render mode is required.");
            }
            switch (value.toLowerCase(Locale.ROOT)) {
                case "ascii":
                    return ASCII;
                case "block":
                    return BLOCK;
                case "image":
                    return IMAGE;
                default:
                    throw new IllegalArgumentException("Unknown render mode: " + value);
            }
        }
    }

    private record DemoConfig(String wadPath, String mapName, int mapScale, RenderMode renderMode,
                              boolean useColor, boolean showMap, String textureName, String imageProtocol) {

        static DemoConfig parseArgs(String[] args) {
            String wadPath = null;
            String mapName = null;
            int mapScale = DEFAULT_MAP_SCALE;
            RenderMode renderMode = RenderMode.BLOCK;
            boolean useColor = true;
            boolean showMap = true;
            String textureName = null;
            String imageProtocol = "half";

            for (int i = 0; i < args.length; i++) {
                String arg = args[i];
                switch (arg) {
                    case "--wad":
                        wadPath = requireValue(args, ++i, "--wad");
                        break;
                    case "--map":
                        mapName = requireValue(args, ++i, "--map");
                        break;
                    case "--scale":
                        String scaleValue = requireValue(args, ++i, "--scale");
                        mapScale = parseInt(scaleValue, "--scale");
                        break;
                    case "--render":
                        String modeValue = requireValue(args, ++i, "--render");
                        try {
                            renderMode = RenderMode.parse(modeValue);
                        } catch (IllegalArgumentException e) {
                            printUsage(e.getMessage());
                            System.exit(1);
                        }
                        break;
                    case "--texture":
                        textureName = requireValue(args, ++i, "--texture").toUpperCase(Locale.ROOT);
                        break;
                    case "--protocol":
                        String protocolValue = requireValue(args, ++i, "--protocol");
                        if (!isValidProtocol(protocolValue)) {
                            printUsage("Unknown protocol: " + protocolValue);
                            System.exit(1);
                        }
                        imageProtocol = protocolValue;
                        break;
                    case "--no-color":
                        useColor = false;
                        break;
                    case "--no-map":
                        showMap = false;
                        break;
                    case "--help":
                    case "-h":
                        printUsage(null);
                        System.exit(0);
                        break;
                    default:
                        printUsage("Unknown option: " + arg);
                        System.exit(1);
                }
            }

            if (mapScale <= 0) {
                printUsage("Scale must be positive.");
                System.exit(1);
            }

            return new DemoConfig(wadPath, mapName, mapScale, renderMode, useColor, showMap, textureName,
                    imageProtocol);
        }

        private static String requireValue(String[] args, int index, String option) {
            if (index >= args.length) {
                printUsage("Missing value for " + option);
                System.exit(1);
            }
            return args[index];
        }

        private static int parseInt(String value, String option) {
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException e) {
                printUsage("Invalid integer for " + option + ": " + value);
                System.exit(1);
                return DEFAULT_MAP_SCALE;
            }
        }

        private static boolean isValidProtocol(String value) {
            if (value == null) {
                return false;
            }
            switch (value.toLowerCase(Locale.ROOT)) {
                case "half":
                case "braille":
                case "kitty":
                case "sixel":
                case "iterm":
                case "auto":
                    return true;
                default:
                    return false;
            }
        }

        private static void printUsage(String error) {
            if (error != null && !error.isEmpty()) {
                System.err.println(error);
            }
            System.out.println("Usage: doom-demo [options]");
            System.out.println("  --wad <path>     Load a DOOM WAD file");
            System.out.println("  --map <name>     Map name (e.g. E1M1, MAP01)");
            System.out.println("  --scale <int>    Map units per grid cell (default: " + DEFAULT_MAP_SCALE + ")");
            System.out.println("  --render <mode>  Render mode: ascii|block|image");
            System.out.println("  --texture <name> Wall texture name (TEXTURE1 entry)");
            System.out.println("  --protocol <p>   Image protocol: half|braille|kitty|sixel|iterm|auto");
            System.out.println("  --no-color       Disable color output");
            System.out.println("  --no-map         Hide minimap overlay");
            System.out.println("  --help           Show this help");
        }
    }

    private record MapLoadResult(MapData map, WadTexture wallTexture, String wallTextureName,
                                 List<String> warnings) {
    }

    record MapData(char[][] map, double startX, double startY, double startAngle,
                   String mapName, String source, int scale) {
    }

    static final class WadFile {
        private static final int HEADER_SIZE = 12;
        private static final int DIRECTORY_ENTRY_SIZE = 16;

        private final byte[] data;
        private final List<WadLump> lumps;
        private WadTextureSet textureSet;

        private WadFile(byte[] data, List<WadLump> lumps) {
            this.data = data;
            this.lumps = lumps;
        }

        static WadFile open(Path path) throws IOException {
            byte[] data = Files.readAllBytes(path);
            if (data.length < HEADER_SIZE) {
                throw new IOException("WAD file is too small.");
            }
            String identification = readName(data, 0, 4);
            if (!"IWAD".equalsIgnoreCase(identification) && !"PWAD".equalsIgnoreCase(identification)) {
                throw new IOException("Unsupported WAD type: " + identification);
            }
            int numLumps = readIntLE(data, 4);
            int directoryOffset = readIntLE(data, 8);
            if (numLumps <= 0) {
                throw new IOException("WAD has no lumps.");
            }
            int directorySize = numLumps * DIRECTORY_ENTRY_SIZE;
            if (directoryOffset < HEADER_SIZE || directoryOffset + directorySize > data.length) {
                throw new IOException("WAD directory is out of bounds.");
            }

            List<WadLump> lumps = new ArrayList<>(numLumps);
            for (int i = 0; i < numLumps; i++) {
                int entryOffset = directoryOffset + i * DIRECTORY_ENTRY_SIZE;
                int filePos = readIntLE(data, entryOffset);
                int size = readIntLE(data, entryOffset + 4);
                String name = readName(data, entryOffset + 8, 8);
                lumps.add(new WadLump(name.toUpperCase(Locale.ROOT), filePos, size));
            }
            return new WadFile(data, lumps);
        }

        String defaultMapName() {
            for (WadLump lump : lumps) {
                if (isMapLabel(lump.name())) {
                    return lump.name();
                }
            }
            throw new IllegalArgumentException("No map label found in WAD.");
        }

        WadMap loadMap(String mapName) {
            if (mapName == null || mapName.isEmpty()) {
                throw new IllegalArgumentException("Map name is required.");
            }
            String target = mapName.toUpperCase(Locale.ROOT);
            int mapIndex = -1;
            for (int i = 0; i < lumps.size(); i++) {
                if (lumps.get(i).name().equals(target)) {
                    mapIndex = i;
                    break;
                }
            }
            if (mapIndex < 0) {
                throw new IllegalArgumentException("Map not found: " + target);
            }

            int endIndex = lumps.size();
            for (int i = mapIndex + 1; i < lumps.size(); i++) {
                if (isMapLabel(lumps.get(i).name())) {
                    endIndex = i;
                    break;
                }
            }

            Map<String, WadLump> mapLumps = new HashMap<>();
            for (int i = mapIndex + 1; i < endIndex; i++) {
                WadLump lump = lumps.get(i);
                mapLumps.putIfAbsent(lump.name(), lump);
            }

            WadLump things = requireLump(mapLumps, "THINGS");
            WadLump linedefs = requireLump(mapLumps, "LINEDEFS");
            WadLump vertexes = requireLump(mapLumps, "VERTEXES");

            List<WadThing> thingList = readThings(things);
            List<WadLine> lineList = readLines(linedefs);
            List<WadVertex> vertexList = readVertices(vertexes);

            return new WadMap(target, vertexList, lineList, thingList);
        }

        WadTextureSet textureSet() {
            if (textureSet == null) {
                textureSet = WadTextureSet.fromWad(this);
            }
            return textureSet;
        }

        boolean hasLump(String name) {
            return findLump(name) != null;
        }

        WadLump findLump(String name) {
            String target = name.toUpperCase(Locale.ROOT);
            for (int i = lumps.size() - 1; i >= 0; i--) {
                WadLump lump = lumps.get(i);
                if (lump.name().equals(target)) {
                    return lump;
                }
            }
            return null;
        }

        private WadLump requireLump(Map<String, WadLump> mapLumps, String name) {
            WadLump lump = mapLumps.get(name);
            if (lump == null) {
                throw new IllegalArgumentException("Missing map lump: " + name);
            }
            return lump;
        }

        private List<WadVertex> readVertices(WadLump lump) {
            byte[] bytes = readLumpBytes(lump);
            if (bytes.length % 4 != 0) {
                throw new IllegalArgumentException("VERTEXES lump size invalid.");
            }
            int count = bytes.length / 4;
            List<WadVertex> vertices = new ArrayList<>(count);
            for (int i = 0; i < count; i++) {
                int offset = i * 4;
                short x = readShortLE(bytes, offset);
                short y = readShortLE(bytes, offset + 2);
                vertices.add(new WadVertex(x, y));
            }
            return vertices;
        }

        private List<WadLine> readLines(WadLump lump) {
            byte[] bytes = readLumpBytes(lump);
            if (bytes.length % 14 != 0) {
                throw new IllegalArgumentException("LINEDEFS lump size invalid.");
            }
            int count = bytes.length / 14;
            List<WadLine> lines = new ArrayList<>(count);
            for (int i = 0; i < count; i++) {
                int offset = i * 14;
                int v1 = readShortLE(bytes, offset);
                int v2 = readShortLE(bytes, offset + 2);
                int flags = readShortLE(bytes, offset + 4);
                lines.add(new WadLine(v1, v2, flags));
            }
            return lines;
        }

        private List<WadThing> readThings(WadLump lump) {
            byte[] bytes = readLumpBytes(lump);
            if (bytes.length % 10 != 0) {
                throw new IllegalArgumentException("THINGS lump size invalid.");
            }
            int count = bytes.length / 10;
            List<WadThing> things = new ArrayList<>(count);
            for (int i = 0; i < count; i++) {
                int offset = i * 10;
                short x = readShortLE(bytes, offset);
                short y = readShortLE(bytes, offset + 2);
                short angle = readShortLE(bytes, offset + 4);
                short type = readShortLE(bytes, offset + 6);
                things.add(new WadThing(x, y, angle, type));
            }
            return things;
        }

        private byte[] readLumpBytes(WadLump lump) {
            int start = lump.offset();
            int end = start + lump.size();
            if (start < 0 || end > data.length) {
                throw new IllegalArgumentException("Lump data out of bounds: " + lump.name());
            }
            return Arrays.copyOfRange(data, start, end);
        }

        private byte[] readLumpBytes(String name) {
            WadLump lump = findLump(name);
            if (lump == null) {
                throw new IllegalArgumentException("Missing lump: " + name);
            }
            return readLumpBytes(lump);
        }

        private static boolean isMapLabel(String name) {
            if (name == null) {
                return false;
            }
            if (name.length() == 4 && name.charAt(0) == 'E' && name.charAt(2) == 'M') {
                return Character.isDigit(name.charAt(1)) && Character.isDigit(name.charAt(3));
            }
            if (name.length() == 5 && name.startsWith("MAP")) {
                return Character.isDigit(name.charAt(3)) && Character.isDigit(name.charAt(4));
            }
            return false;
        }

        private static int readIntLE(byte[] data, int offset) {
            return (data[offset] & 0xFF)
                    | ((data[offset + 1] & 0xFF) << 8)
                    | ((data[offset + 2] & 0xFF) << 16)
                    | ((data[offset + 3] & 0xFF) << 24);
        }

        private static short readShortLE(byte[] data, int offset) {
            return (short) ((data[offset] & 0xFF) | ((data[offset + 1] & 0xFF) << 8));
        }

        private static String readName(byte[] data, int offset, int length) {
            int end = offset;
            int max = offset + length;
            while (end < max && data[end] != 0) {
                end++;
            }
            return new String(data, offset, end - offset, StandardCharsets.US_ASCII).trim();
        }
    }

    private record WadLump(String name, int offset, int size) {
    }

    private record WadVertex(short x, short y) {
    }

    private record WadLine(int v1, int v2, int flags) {
        boolean isBlocking() {
            return (flags & 0x1) != 0 || (flags & 0x4) == 0;
        }
    }

    private record WadThing(short x, short y, short angle, short type) {
    }

    record WadMap(String name, List<WadVertex> vertices, List<WadLine> lines, List<WadThing> things) {
        WadThing playerStart() {
            for (WadThing thing : things) {
                if (thing.type() == 1) {
                    return thing;
                }
            }
            return things.isEmpty() ? null : things.get(0);
        }
    }

    record WadTexture(String name, int width, int height, int[] pixels) {
        int sample(int x, int y) {
            if (x < 0 || x >= width || y < 0 || y >= height) {
                return 0;
            }
            return pixels[y * width + x];
        }
    }

    private record TextureDef(String name, int width, int height, List<TexturePatch> patches) {
    }

    private record TexturePatch(int originX, int originY, String patchName) {
    }

    private record WadPatch(int width, int height, int[] pixels) {
        int pixelAt(int x, int y) {
            if (x < 0 || x >= width || y < 0 || y >= height) {
                return 0;
            }
            return pixels[y * width + x];
        }
    }

    static final class WadTextureSet {
        private static final int TRANSPARENT = 0x00000000;
        private static final String[] PREFERRED_TEXTURES = {
                "STARTAN3", "STARTAN2", "STARTAN1", "STONE2", "STONE3", "BRICK1"
        };

        private final WadFile wad;
        private final int[] palette;
        private final Map<String, TextureDef> textures;
        private final Map<String, WadPatch> patchCache = new HashMap<>();
        private final Map<String, WadTexture> textureCache = new HashMap<>();

        private WadTextureSet(WadFile wad, int[] palette, Map<String, TextureDef> textures) {
            this.wad = wad;
            this.palette = palette;
            this.textures = textures;
        }

        static WadTextureSet fromWad(WadFile wad) {
            int[] palette = loadPalette(wad);
            List<String> patchNames = loadPatchNames(wad);
            Map<String, TextureDef> textures = new HashMap<>();

            if (wad.hasLump("TEXTURE1")) {
                parseTextures(wad.readLumpBytes("TEXTURE1"), patchNames, textures);
            }
            if (wad.hasLump("TEXTURE2")) {
                parseTextures(wad.readLumpBytes("TEXTURE2"), patchNames, textures);
            }

            if (textures.isEmpty()) {
                throw new IllegalArgumentException("No textures found in WAD.");
            }
            return new WadTextureSet(wad, palette, textures);
        }

        String defaultTextureName() {
            for (String preferred : PREFERRED_TEXTURES) {
                if (textures.containsKey(preferred)) {
                    return preferred;
                }
            }
            List<String> names = new ArrayList<>(textures.keySet());
            names.sort(String::compareTo);
            return names.isEmpty() ? null : names.get(0);
        }

        WadTexture texture(String name) {
            if (name == null) {
                return null;
            }
            String key = name.toUpperCase(Locale.ROOT);
            WadTexture cached = textureCache.get(key);
            if (cached != null) {
                return cached;
            }
            TextureDef def = textures.get(key);
            if (def == null) {
                return null;
            }

            int[] pixels = new int[def.width() * def.height()];
            Arrays.fill(pixels, TRANSPARENT);

            for (TexturePatch patch : def.patches()) {
                WadPatch patchImage = loadPatch(patch.patchName());
                if (patchImage == null) {
                    continue;
                }
                blitPatch(pixels, def.width(), def.height(), patchImage, patch.originX(), patch.originY());
            }

            WadTexture texture = new WadTexture(def.name(), def.width(), def.height(), pixels);
            textureCache.put(key, texture);
            return texture;
        }

        private static int[] loadPalette(WadFile wad) {
            if (!wad.hasLump("PLAYPAL")) {
                int[] fallback = new int[256];
                for (int i = 0; i < fallback.length; i++) {
                    int v = i & 0xFF;
                    fallback[i] = 0xFF000000 | (v << 16) | (v << 8) | v;
                }
                return fallback;
            }

            byte[] bytes = wad.readLumpBytes("PLAYPAL");
            if (bytes.length < 256 * 3) {
                throw new IllegalArgumentException("PLAYPAL lump is too small.");
            }
            int[] palette = new int[256];
            for (int i = 0; i < 256; i++) {
                int r = bytes[i * 3] & 0xFF;
                int g = bytes[i * 3 + 1] & 0xFF;
                int b = bytes[i * 3 + 2] & 0xFF;
                palette[i] = 0xFF000000 | (r << 16) | (g << 8) | b;
            }
            return palette;
        }

        private static List<String> loadPatchNames(WadFile wad) {
            byte[] bytes = wad.readLumpBytes("PNAMES");
            int count = WadFile.readIntLE(bytes, 0);
            if (count < 0 || bytes.length < 4 + count * 8) {
                throw new IllegalArgumentException("PNAMES lump is invalid.");
            }
            List<String> names = new ArrayList<>(count);
            for (int i = 0; i < count; i++) {
                int offset = 4 + i * 8;
                String name = WadFile.readName(bytes, offset, 8).toUpperCase(Locale.ROOT);
                names.add(name);
            }
            return names;
        }

        private static void parseTextures(byte[] bytes, List<String> patchNames, Map<String, TextureDef> target) {
            if (bytes.length < 4) {
                throw new IllegalArgumentException("Texture lump is too small.");
            }
            int numTextures = WadFile.readIntLE(bytes, 0);
            if (numTextures < 0 || bytes.length < 4 + numTextures * 4) {
                throw new IllegalArgumentException("Texture lump header invalid.");
            }
            for (int i = 0; i < numTextures; i++) {
                int offset = WadFile.readIntLE(bytes, 4 + i * 4);
                if (offset < 0 || offset >= bytes.length) {
                    continue;
                }
                String name = WadFile.readName(bytes, offset, 8).toUpperCase(Locale.ROOT);
                int width = WadFile.readShortLE(bytes, offset + 12) & 0xFFFF;
                int height = WadFile.readShortLE(bytes, offset + 14) & 0xFFFF;
                int patchCount = WadFile.readShortLE(bytes, offset + 20) & 0xFFFF;
                int patchOffset = offset + 22;
                List<TexturePatch> patches = new ArrayList<>(patchCount);

                for (int p = 0; p < patchCount; p++) {
                    int entryOffset = patchOffset + p * 10;
                    if (entryOffset + 10 > bytes.length) {
                        break;
                    }
                    int originX = WadFile.readShortLE(bytes, entryOffset);
                    int originY = WadFile.readShortLE(bytes, entryOffset + 2);
                    int patchIndex = WadFile.readShortLE(bytes, entryOffset + 4) & 0xFFFF;
                    String patchName = patchIndex < patchNames.size() ? patchNames.get(patchIndex) : null;
                    if (patchName != null && !patchName.isEmpty()) {
                        patches.add(new TexturePatch(originX, originY, patchName));
                    }
                }

                if (!target.containsKey(name)) {
                    target.put(name, new TextureDef(name, width, height, patches));
                }
            }
        }

        private WadPatch loadPatch(String name) {
            if (name == null) {
                return null;
            }
            String key = name.toUpperCase(Locale.ROOT);
            WadPatch cached = patchCache.get(key);
            if (cached != null) {
                return cached;
            }
            WadLump lump = wad.findLump(key);
            if (lump == null) {
                return null;
            }
            byte[] bytes = wad.readLumpBytes(lump);
            WadPatch patch = decodePatch(bytes);
            patchCache.put(key, patch);
            return patch;
        }

        private WadPatch decodePatch(byte[] bytes) {
            if (bytes.length < 8) {
                throw new IllegalArgumentException("Patch lump is too small.");
            }
            int width = WadFile.readShortLE(bytes, 0) & 0xFFFF;
            int height = WadFile.readShortLE(bytes, 2) & 0xFFFF;
            int[] pixels = new int[width * height];
            Arrays.fill(pixels, TRANSPARENT);

            int columnTableOffset = 8;
            if (bytes.length < columnTableOffset + width * 4) {
                throw new IllegalArgumentException("Patch column table is incomplete.");
            }

            for (int x = 0; x < width; x++) {
                int columnOffset = WadFile.readIntLE(bytes, columnTableOffset + x * 4);
                if (columnOffset < 0 || columnOffset >= bytes.length) {
                    continue;
                }
                int pointer = columnOffset;
                while (pointer < bytes.length) {
                    int topDelta = bytes[pointer] & 0xFF;
                    if (topDelta == 0xFF) {
                        break;
                    }
                    int length = bytes[pointer + 1] & 0xFF;
                    int dataStart = pointer + 3;
                    for (int i = 0; i < length; i++) {
                        int y = topDelta + i;
                        if (y >= 0 && y < height && dataStart + i < bytes.length) {
                            int index = bytes[dataStart + i] & 0xFF;
                            pixels[y * width + x] = palette[index];
                        }
                    }
                    pointer += length + 4;
                }
            }
            return new WadPatch(width, height, pixels);
        }

        private static void blitPatch(int[] target, int targetWidth, int targetHeight,
                                      WadPatch patch, int originX, int originY) {
            for (int y = 0; y < patch.height(); y++) {
                int destY = originY + y;
                if (destY < 0 || destY >= targetHeight) {
                    continue;
                }
                for (int x = 0; x < patch.width(); x++) {
                    int destX = originX + x;
                    if (destX < 0 || destX >= targetWidth) {
                        continue;
                    }
                    int color = patch.pixelAt(x, y);
                    if (ImageData.isVisible(color)) {
                        target[destY * targetWidth + destX] = color;
                    }
                }
            }
        }
    }

    static final class WadRasterizer {
        static MapData rasterize(WadMap map, int scale, String source) {
            if (map.vertices().isEmpty()) {
                throw new IllegalArgumentException("Map has no vertices.");
            }
            int minX = Integer.MAX_VALUE;
            int minY = Integer.MAX_VALUE;
            int maxX = Integer.MIN_VALUE;
            int maxY = Integer.MIN_VALUE;
            for (WadVertex vertex : map.vertices()) {
                minX = Math.min(minX, vertex.x());
                minY = Math.min(minY, vertex.y());
                maxX = Math.max(maxX, vertex.x());
                maxY = Math.max(maxY, vertex.y());
            }

            int width = (int) Math.ceil((maxX - minX) / (double) scale) + 3;
            int height = (int) Math.ceil((maxY - minY) / (double) scale) + 3;
            char[][] grid = new char[height][width];
            for (int y = 0; y < height; y++) {
                Arrays.fill(grid[y], '.');
            }

            for (WadLine line : map.lines()) {
                if (!line.isBlocking()) {
                    continue;
                }
                WadVertex v1 = map.vertices().get(line.v1());
                WadVertex v2 = map.vertices().get(line.v2());
                int x1 = toGridX(v1.x(), minX, scale) + 1;
                int y1 = toGridY(v1.y(), maxY, scale) + 1;
                int x2 = toGridX(v2.x(), minX, scale) + 1;
                int y2 = toGridY(v2.y(), maxY, scale) + 1;
                drawLine(grid, x1, y1, x2, y2);
            }

            sealBorders(grid);

            WadThing start = map.playerStart();
            double startX;
            double startY;
            double startAngle;
            if (start != null) {
                startX = toGridX(start.x(), minX, scale) + 1.5;
                startY = toGridY(start.y(), maxY, scale) + 1.5;
                startAngle = DoomEngine.normalizeAngle(Math.toRadians(360 - start.angle()));
            } else {
                startX = grid[0].length / 2.0;
                startY = grid.length / 2.0;
                startAngle = Math.toRadians(45);
            }

            int startCellX = clamp((int) startX, 0, grid[0].length - 1);
            int startCellY = clamp((int) startY, 0, grid.length - 1);
            if (grid[startCellY][startCellX] == '#') {
                grid[startCellY][startCellX] = '.';
            }

            String sourceLabel = source == null ? "WAD" : "WAD:" + source;
            return new MapData(grid, startX, startY, startAngle, map.name(), sourceLabel, scale);
        }

        private static int toGridX(int x, int minX, int scale) {
            return (int) Math.round((x - minX) / (double) scale);
        }

        private static int toGridY(int y, int maxY, int scale) {
            return (int) Math.round((maxY - y) / (double) scale);
        }

        private static void drawLine(char[][] grid, int x0, int y0, int x1, int y1) {
            int dx = Math.abs(x1 - x0);
            int dy = -Math.abs(y1 - y0);
            int sx = x0 < x1 ? 1 : -1;
            int sy = y0 < y1 ? 1 : -1;
            int err = dx + dy;
            int x = x0;
            int y = y0;
            while (true) {
                plot(grid, x, y);
                if (x == x1 && y == y1) {
                    break;
                }
                int e2 = 2 * err;
                if (e2 >= dy) {
                    err += dy;
                    x += sx;
                }
                if (e2 <= dx) {
                    err += dx;
                    y += sy;
                }
            }
        }

        private static void plot(char[][] grid, int x, int y) {
            if (y < 0 || y >= grid.length || x < 0 || x >= grid[0].length) {
                return;
            }
            grid[y][x] = '#';
        }

        private static void sealBorders(char[][] grid) {
            int height = grid.length;
            int width = grid[0].length;
            for (int x = 0; x < width; x++) {
                grid[0][x] = '#';
                grid[height - 1][x] = '#';
            }
            for (int y = 0; y < height; y++) {
                grid[y][0] = '#';
                grid[y][width - 1] = '#';
            }
        }
    }

    static final class DoomEngine {
        private final char[][] map;
        private final int width;
        private final int height;
        private final double startX;
        private final double startY;
        private final double startAngle;
        private final double maxViewDistance;
        private double playerX;
        private double playerY;
        private double angle;

        DoomEngine(String[] mapRows, double startX, double startY, double startAngle) {
            this(toCharMap(mapRows), startX, startY, startAngle);
        }

        DoomEngine(char[][] map, double startX, double startY, double startAngle) {
            if (map.length == 0) {
                throw new IllegalArgumentException("Map cannot be empty.");
            }
            this.height = map.length;
            this.width = map[0].length;
            this.map = map;
            this.startX = startX;
            this.startY = startY;
            this.startAngle = normalizeAngle(startAngle);
            this.maxViewDistance = Math.max(width, height) * 1.25;
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

        double maxViewDistance() {
            return maxViewDistance;
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
            double hitX = playerX + rayDirX * distance;
            double hitY = playerY + rayDirY * distance;
            return new RaycastHit(distance, side == 0, hitX, hitY);
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

        static double normalizeAngle(double value) {
            double twoPi = Math.PI * 2.0;
            double normalized = value % twoPi;
            if (normalized < 0) {
                normalized += twoPi;
            }
            return normalized;
        }
    }

    record RaycastHit(double distance, boolean verticalSide, double hitX, double hitY) {
    }
}
