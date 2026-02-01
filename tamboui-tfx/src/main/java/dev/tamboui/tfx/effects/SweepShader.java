/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.tfx.effects;

import dev.tamboui.buffer.Buffer;
import dev.tamboui.buffer.Cell;
import dev.tamboui.tfx.CellFilter;
import dev.tamboui.tfx.CellIterator;
import dev.tamboui.tfx.TFxColorSpace;
import dev.tamboui.tfx.TFxDuration;
import dev.tamboui.tfx.EffectTimer;
import dev.tamboui.tfx.Interpolation;
import dev.tamboui.tfx.Motion;
import dev.tamboui.tfx.Shader;
import dev.tamboui.tfx.SimpleRng;
import dev.tamboui.tfx.SlidingWindowAlpha;
import dev.tamboui.tfx.DirectionalVariance;
import dev.tamboui.layout.Position;
import dev.tamboui.layout.Rect;
import dev.tamboui.style.Color;

/**
 * A sweep effect that transitions from a faded color to the original content.
 */
public final class SweepShader implements Shader {
    
    private final Motion direction;
    private final int gradientLength;
    private final int randomness;
    private final Color fadedColor;
    private final EffectTimer timer;
    private Rect area;
    private CellFilter cellFilter;
    private SimpleRng rng;
    private TFxColorSpace colorSpace;
    
    /**
     * Creates a sweep shader that sweeps in from a specified color.
     *
     * @param direction the direction of the sweep animation
     * @param gradientLength the length of the gradient transition zone
     * @param randomness the amount of randomness to apply to the sweep
     * @param fadedColor the color to sweep in from
     * @param timer the effect timer controlling the animation
     * @return a new sweep shader
     */
    public static SweepShader sweepIn(Motion direction, int gradientLength, int randomness,
                                      Color fadedColor, EffectTimer timer) {
        return new SweepShader(direction, gradientLength, randomness, fadedColor, timer);
    }
    
    private SweepShader(Motion direction, int gradientLength, int randomness,
                        Color fadedColor, EffectTimer timer) {
        this.direction = direction;
        this.gradientLength = gradientLength;
        this.randomness = randomness;
        this.fadedColor = fadedColor;
        this.timer = timer;
        this.rng = SimpleRng.defaultRng();
        this.colorSpace = TFxColorSpace.HSL; // Default to HSL
    }
    
    @Override
    public String name() {
        boolean isReversed = timer.isReversed();
        boolean flipsTimer = direction.flipsTimer();
        if (isReversed ^ flipsTimer) {
            return "sweep_out";
        } else {
            return "sweep_in";
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
        
        // Create directional variance for randomness (clone RNG to avoid mutation)
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
                    // Apply variance offset to position for alpha calculation (clamped to >= 0)
                    Position offsetPos = offset(pos, rowVariance);
                    float cellAlpha = windowAlpha.alpha(offsetPos);
                    applyAlpha(buffer, pos, cell, cellAlpha);
                }
            }
        } else {
            // Vertical directions: one variance per column
            int[] colVariances = new int[effectArea.width()];
            for (int x = 0; x < effectArea.width(); x++) {
                Position colVariance = axisJitter.next();
                colVariances[x] = colVariance.y(); // For vertical, variance is on y-axis
            }
            
            for (int y = effectArea.top(); y < effectArea.bottom(); y++) {
                for (int x = effectArea.left(); x < effectArea.right(); x++) {
                    Position pos = new Position(x, y);
                    Cell cell = buffer.get(pos);
                    if (!filter.matches(pos, cell, effectArea)) {
                        continue;
                    }
                    // Apply variance offset to position for alpha calculation (clamped to >= 0)
                    int colVariance = colVariances[x - effectArea.left()];
                    Position offsetPos = new Position(x, Math.max(0, y + colVariance));
                    float cellAlpha = windowAlpha.alpha(offsetPos);
                    applyAlpha(buffer, pos, cell, cellAlpha);
                }
            }
        }
    }
    
    private static Position offset(Position p, Position translate) {
        // Clamp to >= 0 (matching Rust behavior)
        return new Position(Math.max(0, p.x() + translate.x()), Math.max(0, p.y() + translate.y()));
    }
    
    private void applyAlpha(Buffer buffer, Position pos, Cell cell, float cellAlpha) {
        // Apply circular out interpolation for smoother transition
        float modAlpha = Interpolation.CircOut.alpha(cellAlpha);
        
        if (cellAlpha <= 0.0f) {
            // Fully faded - use faded color
            buffer.set(pos, cell.style(dev.tamboui.style.Style.EMPTY.fg(fadedColor).bg(fadedColor)));
        } else if (cellAlpha >= 1.0f) {
            // Fully revealed - keep original
            // Nothing to do
        } else {
            // Transition - interpolate between faded and original using ColorSpace
            java.util.Optional<Color> cellFg = cell.style().fg();
            java.util.Optional<Color> cellBg = cell.style().bg();
            
            Color targetFg = cellFg.isPresent() ? cellFg.get() : Color.WHITE;
            Color targetBg = cellBg.isPresent() ? cellBg.get() : Color.BLACK;
            
            Color interpolatedFg = colorSpace.lerp(fadedColor, targetFg, modAlpha);
            Color interpolatedBg = colorSpace.lerp(fadedColor, targetBg, modAlpha);
            
            buffer.set(pos, cell.patchStyle(dev.tamboui.style.Style.EMPTY.fg(interpolatedFg).bg(interpolatedBg)));
        }
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
    public TFxColorSpace colorSpace() {
        return colorSpace;
    }
    
    @Override
    public void setColorSpace(TFxColorSpace colorSpace) {
        this.colorSpace = colorSpace;
    }
    
    @Override
    public Shader copy() {
        EffectTimer timerCopy = EffectTimer.fromMs(timer.duration().asMillis(), timer.interpolation());
        timerCopy.loopMode(timer.loopMode());  // Preserve loop mode
        SweepShader copy = new SweepShader(
            direction, gradientLength, randomness, fadedColor, timerCopy);
        copy.area = area;
        copy.cellFilter = cellFilter;
        copy.rng = new SimpleRng(rng.state());
        copy.colorSpace = colorSpace;
        return copy;
    }
}

