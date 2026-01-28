/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.tfx.effects;

import dev.tamboui.buffer.Buffer;
import dev.tamboui.layout.Rect;
import dev.tamboui.style.Color;
import dev.tamboui.style.Style;
import dev.tamboui.tfx.CellFilter;
import dev.tamboui.tfx.CellIterator;
import dev.tamboui.tfx.EffectTimer;
import dev.tamboui.tfx.Shader;
import dev.tamboui.tfx.TFxDuration;

/**
 * A paint effect that immediately applies colors to cells.
 */
public final class PaintShader implements Shader {
    
    private final Color fg;
    private final Color bg;
    private final EffectTimer timer;
    private Rect area;
    private CellFilter cellFilter;
    
    /**
     * Creates a paint shader that paints foreground and/or background colors.
     *
     * @param fg the foreground color
     * @param bg the background color
     * @param timer the effect timer controlling the paint duration
     * @return a new paint shader
     */
    public static PaintShader paint(Color fg, Color bg, EffectTimer timer) {
        return new PaintShader(fg, bg, timer);
    }
    
    /**
     * Creates a paint shader that paints only foreground color.
     *
     * @param fg the foreground color
     * @param timer the effect timer controlling the paint duration
     * @return a new paint shader for foreground only
     */
    public static PaintShader paintFg(Color fg, EffectTimer timer) {
        return new PaintShader(fg, null, timer);
    }
    
    /**
     * Creates a paint shader that paints only background color.
     *
     * @param bg the background color
     * @param timer the effect timer controlling the paint duration
     * @return a new paint shader for background only
     */
    public static PaintShader paintBg(Color bg, EffectTimer timer) {
        return new PaintShader(null, bg, timer);
    }
    
    private PaintShader(Color fg, Color bg, EffectTimer timer) {
        if (fg == null && bg == null) {
            throw new IllegalArgumentException("At least one of fg or bg must be non-null");
        }
        this.fg = fg;
        this.bg = bg;
        this.timer = timer;
    }
    
    @Override
    public String name() {
        if (fg != null && bg != null) {
            return "paint";
        } else if (fg != null) {
            return "paint_fg";
        } else {
            return "paint_bg";
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
        // Paint effect applies colors immediately based on timer alpha
        // For now, we'll apply it when alpha > 0
        float alpha = timer.alpha();
        if (alpha <= 0.0f) {
            return;
        }

        CellFilter filter = cellFilter != null ? cellFilter : CellFilter.all();
        CellIterator iterator = new CellIterator(buffer, area, filter);

        // Use optimized methods when only one color is being set
        if (fg != null && bg == null) {
            iterator.forEachCellMutable((x, y, mutable) -> mutable.setFg(fg));
        } else if (fg == null && bg != null) {
            iterator.forEachCellMutable((x, y, mutable) -> mutable.setBg(bg));
        } else {
            // Both colors - need to set style
            iterator.forEachCellMutable((x, y, mutable) -> {
                Style newStyle = mutable.cell().style().fg(fg).bg(bg);
                mutable.setStyle(newStyle);
            });
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
    public Shader copy() {
        EffectTimer timerCopy = EffectTimer.fromMs(timer.duration().asMillis(), timer.interpolation());
        timerCopy.loopMode(timer.loopMode());  // Preserve loop mode
        PaintShader copy = new PaintShader(fg, bg, timerCopy);
        copy.area = area;
        copy.cellFilter = cellFilter;
        return copy;
    }
}


