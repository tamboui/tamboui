/*
 * Copyright (c) 2025 TamboUI Contributors
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
        
        // Create sliding window alpha calculator
        SlidingWindowAlpha windowAlpha = SlidingWindowAlpha.create(
            direction, area, alpha, gradientLength + randomness);
        
        CellFilter filter = cellFilter != null ? cellFilter : CellFilter.all();
        CellIterator iterator = new CellIterator(buffer, area, filter);
        iterator.forEachCellMutable((pos, mutable) -> {
            Cell cell = mutable.cell();
            float cellAlpha = windowAlpha.alpha(pos);
            
            // Apply circular out interpolation for smoother transition
            float modAlpha = Interpolation.CircOut.alpha(cellAlpha);
            
            if (cellAlpha <= 0.0f) {
                // Fully faded - use faded color
                mutable.setStyle(dev.tamboui.style.Style.EMPTY.fg(fadedColor).bg(fadedColor));
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
                
                mutable.patchStyle(dev.tamboui.style.Style.EMPTY.fg(interpolatedFg).bg(interpolatedBg));
            }
        });
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
        SweepShader copy = new SweepShader(
            direction, gradientLength, randomness, fadedColor,
            EffectTimer.fromMs(timer.duration().asMillis(), timer.interpolation()));
        copy.area = area;
        copy.cellFilter = cellFilter;
        copy.rng = new SimpleRng(rng.state());
        copy.colorSpace = colorSpace;
        return copy;
    }
}

