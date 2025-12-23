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
        
        CellFilter filter = cellFilter != null ? cellFilter : CellFilter.all();
        CellIterator iterator = new CellIterator(buffer, area, filter);
        iterator.forEachCellMutable((pos, mutable) -> {
            Cell cell = mutable.cell();
            
            // Only dissolve cells that have content
            if (!cell.isEmpty() && cell.symbol() != null && !" ".equals(cell.symbol())) {
                // Use random thresholding - if alpha exceeds random value, dissolve the cell
                float randomValue = rng.genF32();
                if (alpha > randomValue) {
                    // Dissolve: replace with space
                    if (dissolvedStyle != null) {
                        mutable.setCell(new Cell(" ", dissolvedStyle));
                    } else {
                        mutable.setSymbol(" ");
                    }
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


