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
import dev.tamboui.tfx.Shader;
import dev.tamboui.tfx.pattern.IdentityPattern;
import dev.tamboui.tfx.pattern.Pattern;
import dev.tamboui.layout.Position;
import dev.tamboui.layout.Rect;
import dev.tamboui.style.Color;

/**
 * Shader implementation for color fade effects.
 * <p>
 * FadeShader smoothly transitions between two colors over time. It supports:
 * <ul>
 *   <li><b>Color Interpolation:</b> Uses configurable color spaces (RGB, HSL, HSV)
 *       for perceptually smooth transitions</li>
 *   <li><b>Spatial Patterns:</b> Can apply fade patterns using {@link Pattern}
 *       implementations for directional or radial fades</li>
 *   <li><b>Cell Filtering:</b> Can target specific cells using {@link CellFilter}</li>
 * </ul>
 * <p>
 * <b>Implementation Details:</b>
 * <p>
 * The shader iterates through cells in the target area, applies the cell filter,
 * and interpolates colors based on the timer's alpha value. If a pattern is set,
 * the pattern transforms the global alpha into position-specific alpha values.
 * <p>
 * <b>Color Space Selection:</b>
 * <ul>
 *   <li><b>RGB:</b> Fastest, but produces perceptually non-uniform transitions
 *       (e.g., gray midpoints when fading between saturated colors)</li>
 *   <li><b>HSL:</b> Default - good balance of performance and perceptual quality,
 *       produces smooth hue transitions</li>
 *   <li><b>HSV:</b> Similar to HSL but with different perceptual model</li>
 * </ul>
 * <p>
 * This shader is typically created through {@link dev.tamboui.tfx.Fx} factory methods
 * rather than directly.
 */
public final class FadeShader implements Shader {
    
    private final Color fromColor;
    private final Color toColor;
    private final EffectTimer timer;
    private Rect area;
    private CellFilter cellFilter;
    private TFxColorSpace colorSpace;
    private Pattern pattern;
    
    /**
     * Creates a fade shader that transitions from one color to another.
     *
     * @param fromColor the starting color
     * @param toColor the target color
     * @param timer the effect timer controlling the fade duration
     * @return a new fade shader
     */
    public static FadeShader fadeTo(Color fromColor, Color toColor, EffectTimer timer) {
        return new FadeShader(fromColor, toColor, timer);
    }
    
    private FadeShader(Color fromColor, Color toColor, EffectTimer timer) {
        this.fromColor = fromColor;
        this.toColor = toColor;
        this.timer = timer;
        this.colorSpace = TFxColorSpace.HSL; // Default to HSL for better perceptual quality
        this.pattern = IdentityPattern.INSTANCE; // Default to identity pattern
    }
    
    @Override
    public String name() {
        return "fade_to";
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
        float globalAlpha = timer.alpha();

        Pattern currentPattern = pattern != null ? pattern : IdentityPattern.INSTANCE;

        CellFilter filter = cellFilter != null ? cellFilter : CellFilter.all();
        CellIterator iterator = new CellIterator(buffer, area, filter);
        iterator.forEachCellMutable((x, y, mutable) -> {
            // Only modify cells that have content (not empty)
            Cell cell = mutable.cell();
            if (!cell.isEmpty()) {
                // Apply pattern to get position-specific alpha
                float positionAlpha = currentPattern.mapAlpha(globalAlpha, x, y, area);

                // Use ColorSpace for proper color interpolation
                Color currentColor = colorSpace.lerp(fromColor, toColor, positionAlpha);

                mutable.setFg(currentColor);
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
    public Pattern pattern() {
        return pattern;
    }
    
    @Override
    public void setPattern(Pattern pattern) {
        this.pattern = pattern;
    }
    
    @Override
    public Shader copy() {
        EffectTimer timerCopy = EffectTimer.fromMs(timer.duration().asMillis(), timer.interpolation());
        timerCopy.loopMode(timer.loopMode());  // Preserve loop mode
        FadeShader copy = new FadeShader(fromColor, toColor, timerCopy);
        copy.area = area;
        copy.cellFilter = cellFilter;
        copy.colorSpace = colorSpace;
        copy.pattern = pattern;
        return copy;
    }
}

