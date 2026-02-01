/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.tfx.effects;

import dev.tamboui.buffer.Buffer;
import dev.tamboui.tfx.CellFilter;
import dev.tamboui.tfx.ExpandDirection;
import dev.tamboui.tfx.TFxDuration;
import dev.tamboui.tfx.EffectTimer;
import dev.tamboui.tfx.Motion;
import dev.tamboui.tfx.Shader;
import dev.tamboui.layout.Rect;
import dev.tamboui.style.Style;

/**
 * A shader that applies bidirectional expansion effects using two opposing stretch shaders.
 * <p>
 * Creates expansion animations that grow outward from the center in both directions
 * simultaneously, either horizontally or vertically.
 */
public final class ExpandShader implements Shader {
    
    private final ExpandDirection direction;
    private final Style style;
    private final StretchShader stretchA;
    private final StretchShader stretchB;
    private Rect area;
    
    /**
     * Creates a new expand effect with the specified direction, style, and timing.
     *
     * @param direction the expansion direction (horizontal or vertical)
     * @param style the style to apply during expansion
     * @param timer the effect timer controlling the expansion duration
     */
    public ExpandShader(ExpandDirection direction, Style style, EffectTimer timer) {
        this.direction = direction;
        this.style = style;
        
        // Create two stretch shaders in opposite directions
        Motion motionA, motionB;
        if (direction == ExpandDirection.HORIZONTAL) {
            motionA = Motion.RIGHT_TO_LEFT;
            motionB = Motion.LEFT_TO_RIGHT;
        } else { // VERTICAL
            motionA = Motion.DOWN_TO_UP;
            motionB = Motion.UP_TO_DOWN;
        }
        
        this.stretchA = new StretchShader(style, motionA, timer);
        this.stretchB = new StretchShader(style, motionB, timer);
    }
    
    @Override
    public String name() {
        return "Expand";
    }
    
    @Override
    public TFxDuration process(TFxDuration duration, Buffer buffer, Rect area) {
        Rect effectArea = this.area != null ? this.area : area;
        effectArea = effectArea.intersection(buffer.area());
        
        // Split area in half based on direction
        Rect areaA, areaB;
        if (direction == ExpandDirection.HORIZONTAL) {
            int halfWidth = effectArea.width() / 2;
            areaA = new Rect(effectArea.left(), effectArea.top(), halfWidth, effectArea.height());
            areaB = new Rect(areaA.right(), effectArea.top(), 
                effectArea.width() - halfWidth, effectArea.height());
        } else { // VERTICAL
            int halfHeight = effectArea.height() / 2;
            areaA = new Rect(effectArea.left(), effectArea.top(), effectArea.width(), halfHeight);
            areaB = new Rect(effectArea.left(), areaA.bottom(), 
                effectArea.width(), effectArea.height() - halfHeight);
        }
        
        // Process both stretch shaders in parallel
        TFxDuration overflowA = stretchA.process(duration, buffer, areaA);
        TFxDuration overflowB = stretchB.process(duration, buffer, areaB);
        
        // Return the minimum overflow (or null if both are still running)
        if (overflowA == null && overflowB == null) {
            return null;
        } else if (overflowA == null) {
            return overflowB;
        } else if (overflowB == null) {
            return overflowA;
        } else {
            // Return the smaller overflow duration
            return overflowA.asMillis() < overflowB.asMillis() ? overflowA : overflowB;
        }
    }
    
    @Override
    public void execute(TFxDuration duration, Rect area, Buffer buffer) {
        // Execute is handled by process() which calls the stretch shaders directly
    }
    
    @Override
    public boolean done() {
        return stretchA.done();
    }
    
    @Override
    public Rect area() {
        return area;
    }
    
    @Override
    public void setArea(Rect area) {
        this.area = area;
        stretchA.setArea(area);
        stretchB.setArea(area);
    }
    
    @Override
    public EffectTimer timer() {
        return stretchA.timer();
    }
    
    @Override
    public EffectTimer mutableTimer() {
        return stretchA.mutableTimer();
    }
    
    @Override
    public CellFilter cellFilter() {
        return stretchA.cellFilter();
    }
    
    @Override
    public void setCellFilter(CellFilter filter) {
        stretchA.setCellFilter(filter);
        stretchB.setCellFilter(filter);
    }
    
    @Override
    public void reverse() {
        // Reverse both stretch shaders
        stretchA.reverse();
        stretchB.reverse();
    }
    
    @Override
    public Shader copy() {
        EffectTimer timerCopy = EffectTimer.fromMs(stretchA.timer().duration().asMillis(),
            stretchA.timer().interpolation());
        timerCopy.loopMode(stretchA.timer().loopMode());  // Preserve loop mode
        ExpandShader copy = new ExpandShader(direction, style, timerCopy);
        copy.area = area;
        copy.setCellFilter(cellFilter());
        // Preserve reversed state
        if (stretchA.timer().isReversed()) {
            copy.reverse();
        }
        return copy;
    }
}

