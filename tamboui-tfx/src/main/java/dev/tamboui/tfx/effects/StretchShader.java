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
import dev.tamboui.tfx.Motion;
import dev.tamboui.tfx.Shader;
import dev.tamboui.tfx.TFxDuration;
import dev.tamboui.tfx.TFxMath;

/**
 * A shader that applies a stretching effect to terminal cells, expanding or shrinking
 * rectangular areas using block characters.
 * <p>
 * The stretch effect fills areas with a style (including background color) and uses
 * partial block characters (▏▎▍▌▋▊▉█ for horizontal, ▁▂▃▄▅▆▇█ for vertical) to create
 * smooth transitions as the area expands or contracts.
 */
public final class StretchShader implements Shader {
    
    private final Style style;
    private final Motion direction;
    private EffectTimer timer;  // Mutable to allow in-place reversal (matching Rust behavior)
    private Rect area;
    private CellFilter cellFilter;
    
    // Block characters for smooth transitions
    private static final String[] STRETCH_H = {"▏", "▎", "▍", "▌", "▋", "▊", "▉", "█"};
    private static final String[] STRETCH_V = {"▁", "▂", "▃", "▄", "▅", "▆", "▇", "█"};
    private static final int LAST_IDX = STRETCH_H.length - 1;
    
    /**
     * Creates a new stretch shader.
     *
     * @param style the style to apply to the stretched area
     * @param direction the direction of the stretch animation
     * @param timer the effect timer controlling the animation
     */
    public StretchShader(Style style, Motion direction, EffectTimer timer) {
        this.style = style;
        this.direction = direction;
        this.timer = timer;
    }
    
    @Override
    public String name() {
        return "stretch";
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
        
        // Determine the effective + safe area to apply the effect
        Rect effectArea = this.area != null ? this.area : area;
        effectArea = effectArea.intersection(buffer.area());
        
        if (alpha >= 1.0f) {
            fillArea(inverseStyle(style), effectArea, buffer);
            return;
        } else if (alpha <= 0.0f) {
            fillArea(style, effectArea, buffer);
            return;
        }
        
        StretchBounds bounds = new StretchBounds(effectArea, direction, alpha);
        float fractional = bounds.end % 1.0f;
        fractional = (direction == Motion.RIGHT_TO_LEFT || direction == Motion.DOWN_TO_UP) 
            ? 1.0f - fractional 
            : fractional;
        
        StretchChar stretchChar = stretchChar(fractional, direction, style);
        Regions regions = regions(direction, alpha, effectArea);
        
        fillArea(inverseStyle(style), regions.filled, buffer);
        fillArea(style, regions.empty, buffer);
        
        CellFilter filter = cellFilter != null ? cellFilter : CellFilter.all();
        CellIterator iterator = new CellIterator(buffer, regions.stretching, filter);
        iterator.forEachCellMutable((x, y, mutable) -> {
            mutable.setSymbol(stretchChar.symbol);
            mutable.setStyle(stretchChar.style);
        });
    }

    private void fillArea(Style fillStyle, Rect area, Buffer buffer) {
        CellFilter filter = cellFilter != null ? cellFilter : CellFilter.all();
        CellIterator iterator = new CellIterator(buffer, area, filter);
        iterator.forEachCellMutable((x, y, mutable) -> {
            mutable.setSymbol(" ");
            mutable.setStyle(fillStyle);
        });
    }
    
    private static Regions regions(Motion direction, float progress, Rect area) {
        switch (direction) {
            case LEFT_TO_RIGHT: {
                float len = area.width() * progress;
                int filledWidth = (int) TFxMath.floor(len);
                int stretchingX = area.left() + filledWidth;
                int emptyX = area.left() + (int) TFxMath.ceil(len);
                int emptyWidth = area.width() - (int) TFxMath.ceil(len);
                return new Regions(
                    new Rect(area.left(), area.top(), filledWidth, area.height()),
                    new Rect(stretchingX, area.top(), 1, area.height()),
                    new Rect(emptyX, area.top(), emptyWidth, area.height())
                );
            }
            case RIGHT_TO_LEFT: {
                float len = area.width() * progress;
                int filledWidth = (int) TFxMath.ceil(len);
                int filledX = area.left() + area.width() - filledWidth;
                int stretchingX = area.left() + area.width() - (int) TFxMath.floor(len) - 1;
                int emptyWidth = area.width() - (int) TFxMath.floor(len) - 1;
                return new Regions(
                    new Rect(filledX, area.top(), filledWidth, area.height()),
                    new Rect(stretchingX, area.top(), 1, area.height()),
                    new Rect(area.left(), area.top(), emptyWidth, area.height())
                );
            }
            case UP_TO_DOWN: {
                float len = area.height() * progress;
                int filledHeight = (int) TFxMath.floor(len);
                int stretchingY = area.top() + filledHeight;
                int emptyY = area.top() + (int) TFxMath.ceil(len);
                int emptyHeight = area.height() - (int) TFxMath.ceil(len);
                return new Regions(
                    new Rect(area.left(), area.top(), area.width(), filledHeight),
                    new Rect(area.left(), stretchingY, area.width(), 1),
                    new Rect(area.left(), emptyY, area.width(), emptyHeight)
                );
            }
            case DOWN_TO_UP: {
                float len = area.height() * progress;
                int filledHeight = (int) TFxMath.ceil(len);
                int filledY = area.top() + area.height() - filledHeight;
                int stretchingY = area.top() + area.height() - (int) TFxMath.floor(len) - 1;
                int emptyHeight = area.height() - (int) TFxMath.floor(len) - 1;
                return new Regions(
                    new Rect(area.left(), filledY, area.width(), filledHeight),
                    new Rect(area.left(), stretchingY, area.width(), 1),
                    new Rect(area.left(), area.top(), area.width(), emptyHeight)
                );
            }
            default:
                throw new IllegalArgumentException("Unknown direction: " + direction);
        }
    }
    
    private static Style inverseStyle(Style style) {
        Style result = Style.EMPTY;
        
        // Copy modifiers
        for (dev.tamboui.style.Modifier mod : style.addModifiers()) {
            result = result.addModifier(mod);
        }
        for (dev.tamboui.style.Modifier mod : style.subModifiers()) {
            result = result.removeModifier(mod);
        }
        
        // Swap colors
        java.util.Optional<Color> fg = style.fg();
        java.util.Optional<Color> bg = style.bg();
        
        if (fg.isPresent() && bg.isPresent()) {
            return result.fg(bg.get()).bg(fg.get());
        } else if (fg.isPresent()) {
            return result.bg(fg.get());
        } else if (bg.isPresent()) {
            return result.fg(bg.get());
        }
        return result;
    }
    
    private static int stretchSymbolIdx(float alpha) {
        alpha = Math.max(0.0f, Math.min(1.0f, alpha));
        return (int) TFxMath.round(LAST_IDX * alpha);
    }
    
    private static StretchChar stretchChar(float insideCellAlpha, Motion motion, Style style) {
        int charIdx = stretchSymbolIdx(insideCellAlpha);
        
        String symbol;
        switch (motion) {
            case LEFT_TO_RIGHT:
                symbol = STRETCH_H[charIdx];
                break;
            case RIGHT_TO_LEFT:
                symbol = STRETCH_H[LAST_IDX - charIdx];
                break;
            case UP_TO_DOWN:
                symbol = STRETCH_V[LAST_IDX - charIdx];
                break;
            case DOWN_TO_UP:
                symbol = STRETCH_V[charIdx];
                break;
            default:
                throw new IllegalArgumentException("Unknown motion: " + motion);
        }
        
        boolean isReverse = (motion == Motion.RIGHT_TO_LEFT || motion == Motion.UP_TO_DOWN);
        Style charStyle = isReverse ? inverseStyle(style) : style;
        
        return new StretchChar(symbol, charStyle);
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
    public void reverse() {
        // Reverse the timer in place (matching Rust's in-place mutation behavior)
        // EffectTimer.reversed() returns a new instance, so we replace the timer
        this.timer = timer.reversed();
    }
    
    @Override
    public Shader copy() {
        EffectTimer timerCopy = EffectTimer.fromMs(timer.duration().asMillis(), timer.interpolation());
        timerCopy.loopMode(timer.loopMode());  // Preserve loop mode
        // Preserve reversed state
        if (timer.isReversed()) {
            timerCopy = timerCopy.reversed();
        }
        StretchShader copy = new StretchShader(style, direction, timerCopy);
        copy.area = area;
        copy.cellFilter = cellFilter;
        return copy;
    }
    
    // Helper classes
    private static class StretchBounds {
        final float start;
        final float end;
        
        StretchBounds(Rect rect, Motion motion, float alpha) {
            float x = rect.left();
            float y = rect.top();
            float w = rect.width();
            float h = rect.height();
            
            switch (motion) {
                case LEFT_TO_RIGHT:
                    this.start = x;
                    this.end = x + w * alpha;
                    break;
                case RIGHT_TO_LEFT:
                    this.start = x + w - 1.0f;
                    this.end = x + w * (1.0f - alpha);
                    break;
                case UP_TO_DOWN:
                    this.start = y;
                    this.end = y + h * alpha;
                    break;
                case DOWN_TO_UP:
                    this.start = y + h - 1.0f;
                    this.end = y + h * (1.0f - alpha);
                    break;
                default:
                    throw new IllegalArgumentException("Unknown motion: " + motion);
            }
        }
    }
    
    private static class Regions {
        final Rect filled;
        final Rect stretching;
        final Rect empty;
        
        Regions(Rect filled, Rect stretching, Rect empty) {
            this.filled = filled;
            this.stretching = stretching;
            this.empty = empty;
        }
    }
    
    private static class StretchChar {
        final String symbol;
        final Style style;
        
        StretchChar(String symbol, Style style) {
            this.symbol = symbol;
            this.style = style;
        }
    }
}

