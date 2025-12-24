/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.tfx.effects;

import dev.tamboui.buffer.Buffer;
import dev.tamboui.buffer.Cell;
import dev.tamboui.tfx.CellFilter;
import dev.tamboui.tfx.CellIterator;
import dev.tamboui.tfx.TFxDuration;
import dev.tamboui.tfx.EffectTimer;
import dev.tamboui.tfx.Shader;
import dev.tamboui.tfx.SimpleRng;
import dev.tamboui.layout.Position;
import dev.tamboui.layout.Rect;
import dev.tamboui.style.Style;

/**
 * A dissolve effect that randomly dissolves text characters over time.
 */
public final class DissolveShader implements Shader {
    
    private final EffectTimer timer;
    private final Style dissolvedStyle;
    private Rect area;
    private CellFilter cellFilter;
    private SimpleRng rng;
    
    /**
     * Creates a dissolve shader that dissolves text over time.
     */
    public static DissolveShader dissolve(EffectTimer timer) {
        return new DissolveShader(timer, null);
    }
    
    /**
     * Creates a dissolve shader that dissolves to a specific style.
     */
    public static DissolveShader dissolveTo(Style style, EffectTimer timer) {
        return new DissolveShader(timer, style);
    }
    
    private DissolveShader(EffectTimer timer, Style dissolvedStyle) {
        this.timer = timer;
        this.dissolvedStyle = dissolvedStyle;
        this.rng = SimpleRng.defaultRng();
    }
    
    @Override
    public String name() {
        if (dissolvedStyle != null) {
            return timer.isReversed() ? "coalesce_from" : "dissolve_to";
        } else {
            return timer.isReversed() ? "coalesce" : "dissolve";
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
        
        CellFilter filter = cellFilter != null ? cellFilter : CellFilter.all();
        CellIterator iterator = new CellIterator(buffer, effectArea, filter);
        iterator.forEachCellMutable((pos, mutable) -> {
            Cell cell = mutable.cell();
            
            // Use random thresholding - if alpha exceeds random value, dissolve the cell
            // This matches Rust's logic: process all cells that match the filter
            float randomValue = rng.genF32();
            if (alpha > randomValue) {
                // Dissolve: set symbol to space and apply dissolved style
                // Matching Rust: cell.set_char(' ') and cell.set_style(style)
                mutable.setSymbol(" ");
                if (dissolvedStyle != null) {
                    // Apply the full dissolved style (matching Rust's set_style behavior)
                    mutable.setCell(new Cell(" ", dissolvedStyle));
                }
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
    public Shader copy() {
        DissolveShader copy = new DissolveShader(
            EffectTimer.fromMs(timer.duration().asMillis(), timer.interpolation()),
            dissolvedStyle
        );
        copy.area = area;
        copy.cellFilter = cellFilter;
        copy.rng = new SimpleRng(rng.state());
        return copy;
    }
}


