/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.tfx.effects;

import dev.tamboui.buffer.Buffer;
import dev.tamboui.buffer.Cell;
import dev.tamboui.tfx.CellFilter;
import dev.tamboui.tfx.CellIterator;
import dev.tamboui.tfx.TFxDuration;
import dev.tamboui.tfx.EffectTimer;
import dev.tamboui.tfx.Motion;
import dev.tamboui.tfx.Shader;
import dev.tamboui.tfx.SimpleRng;
import dev.tamboui.tfx.SlidingWindowAlpha;
import dev.tamboui.tfx.DirectionalVariance;
import dev.tamboui.layout.Position;
import dev.tamboui.layout.Rect;
import dev.tamboui.style.Color;
import dev.tamboui.style.Style;

/**
 * A slide effect that uses block characters to create a "shutter" animation.
 * Cells gradually fill with block characters (█, ▇, ▆, etc.) based on the direction.
 */
public final class SlideShader implements Shader {
    
    // Block characters for vertical sliding (from full to empty)
    private static final char[] SHRINK_V = {'█', '▇', '▆', '▅', '▄', '▃', '▂', '▁', ' '};
    // Block characters for horizontal sliding (from full to empty)
    private static final char[] SHRINK_H = {'█', '▉', '▊', '▋', '▌', '▍', '▎', '▏', ' '};
    private static final int LAST_IDX = SHRINK_H.length - 1;
    
    private final Motion direction;
    private final int gradientLength;
    private final int randomness;
    private final Color colorBehindCell;
    private final EffectTimer timer;
    private Rect area;
    private CellFilter cellFilter;
    private SimpleRng rng;
    
    /**
     * Creates a slide shader that slides out in the specified direction.
     *
     * @param direction the direction of the slide animation
     * @param gradientLength the length of the gradient transition zone
     * @param randomness the amount of randomness to apply to the slide
     * @param colorBehindCell the color revealed behind cells as they slide away
     * @param timer the effect timer controlling the animation
     * @return a new slide shader
     */
    public static SlideShader slideOut(Motion direction, int gradientLength, int randomness,
                                      Color colorBehindCell, EffectTimer timer) {
        return new SlideShader(direction, gradientLength, randomness, colorBehindCell, timer);
    }
    
    private SlideShader(Motion direction, int gradientLength, int randomness,
                       Color colorBehindCell, EffectTimer timer) {
        this.direction = direction;
        this.gradientLength = gradientLength;
        this.randomness = randomness;
        this.colorBehindCell = colorBehindCell;
        this.timer = timer;
        this.rng = SimpleRng.defaultRng();
    }
    
    @Override
    public String name() {
        boolean isReversed = timer.isReversed();
        boolean flipsTimer = direction.flipsTimer();
        if (isReversed ^ flipsTimer) {
            return "slide_in";
        } else {
            return "slide_out";
        }
    }
    
    @Override
    public TFxDuration process(TFxDuration duration, Buffer buffer, Rect area) {
        Rect effectArea = this.area != null ? this.area : area;
        EffectTimer currentTimer = timer;
        
        // Process timer
        TFxDuration overflow = currentTimer.process(duration);
        
        // Execute effect
        execute(duration, effectArea, buffer);
        
        return overflow;
    }
    
    @Override
    public void execute(TFxDuration duration, Rect area, Buffer buffer) {
        float alpha = timer.alpha();
        Rect effectArea = this.area != null ? this.area : area;
        effectArea = effectArea.intersection(buffer.area());
        
        // Create sliding window alpha calculator
        SlidingWindowAlpha windowAlpha = SlidingWindowAlpha.create(
            direction, effectArea, alpha, gradientLength + randomness);
        
        // Create directional variance for randomness
        DirectionalVariance axisJitter = DirectionalVariance.withRng(
            new SimpleRng(rng.state()), direction, randomness);
        
        CellFilter filter = cellFilter != null ? cellFilter : CellFilter.all();
        
        // Apply randomness based on direction
        if (randomness == 0 || direction == Motion.LEFT_TO_RIGHT || direction == Motion.RIGHT_TO_LEFT) {
            // Horizontal directions: one variance per row
            for (int y = effectArea.top(); y < effectArea.bottom(); y++) {
                Position rowVariance = axisJitter.next();
                for (int x = effectArea.left(); x < effectArea.right(); x++) {
                    Position pos = new Position(x, y);
                    Cell cell = buffer.get(pos);
                    if (!filter.matches(pos, cell, effectArea)) {
                        continue;
                    }
                    Position offsetPos = offset(pos, rowVariance);
                    float cellAlpha = windowAlpha.alpha(offsetPos);
                    updateCell(buffer, pos, cell, cellAlpha);
                }
            }
        } else {
            // Vertical directions: one variance per column
            int[] colVariances = new int[effectArea.width()];
            for (int x = 0; x < effectArea.width(); x++) {
                Position colVariance = axisJitter.next();
                colVariances[x] = colVariance.y();
            }
            
            for (int y = effectArea.top(); y < effectArea.bottom(); y++) {
                for (int x = effectArea.left(); x < effectArea.right(); x++) {
                    Position pos = new Position(x, y);
                    Cell cell = buffer.get(pos);
                    if (!filter.matches(pos, cell, effectArea)) {
                        continue;
                    }
                    int colVariance = colVariances[x - effectArea.left()];
                    Position offsetPos = new Position(
                        Math.max(0, pos.x()),
                        Math.max(0, pos.y() + colVariance)
                    );
                    float cellAlpha = windowAlpha.alpha(offsetPos);
                    updateCell(buffer, pos, cell, cellAlpha);
                }
            }
        }
    }
    
    private void updateCell(Buffer buffer, Position pos, Cell cell, float cellAlpha) {
        if (cellAlpha <= 0.0f) {
            // Not affected yet - leave cell unchanged
            return;
        }
        
        // Get the original background color (before any effects)
        // This is what we'll use as the foreground for block characters
        java.util.Optional<Color> originalBg = cell.style().bg();
        Color blockFg = originalBg.isPresent() ? originalBg.get() : Color.BLACK;
        
        // Set the new background to color_behind_cell
        // For block characters: fg = original bg, bg = color_behind_cell (matching Rust)
        dev.tamboui.style.Style blockStyle = dev.tamboui.style.Style.EMPTY
            .fg(blockFg)
            .bg(colorBehindCell);
        
        if (cellAlpha >= 1.0f) {
            // Fully slid - set to space with block style
            buffer.set(pos, new Cell(" ", blockStyle));
        } else {
            // Partially slid - use block character based on alpha
            // The block character uses the original background as foreground
            char blockChar = slidedCell(cellAlpha);
            buffer.set(pos, new Cell(String.valueOf(blockChar), blockStyle));
        }
    }
    
    private char slidedCell(float alpha) {
        float clamped = Math.max(0.0f, Math.min(1.0f, alpha));
        int charIdx = Math.round(LAST_IDX * clamped);
        charIdx = Math.min(charIdx, LAST_IDX);
        
        if (direction == Motion.LEFT_TO_RIGHT || direction == Motion.RIGHT_TO_LEFT) {
            return SHRINK_H[charIdx];
        } else {
            return SHRINK_V[charIdx];
        }
    }
    
    private static Position offset(Position p, Position translate) {
        return new Position(Math.max(0, p.x() + translate.x()), Math.max(0, p.y() + translate.y()));
    }
    
    @Override
    public boolean done() {
        return timer.done();
    }
    
    @Override
    public Rect area() {
        return area;
    }
    
    @Override
    public void setArea(Rect area) {
        this.area = area;
    }
    
    @Override
    public EffectTimer timer() {
        return timer;
    }
    
    @Override
    public EffectTimer mutableTimer() {
        return timer;
    }
    
    @Override
    public CellFilter cellFilter() {
        return cellFilter;
    }
    
    @Override
    public void setCellFilter(CellFilter filter) {
        this.cellFilter = filter;
    }
    
    @Override
    public Shader copy() {
        EffectTimer timerCopy = EffectTimer.fromMs(timer.duration().asMillis(), timer.interpolation());
        timerCopy.loopMode(timer.loopMode());  // Preserve loop mode
        SlideShader copy = new SlideShader(
            direction, gradientLength, randomness, colorBehindCell, timerCopy);
        copy.area = area;
        copy.cellFilter = cellFilter;
        copy.rng = new SimpleRng(rng.state());
        return copy;
    }
}

