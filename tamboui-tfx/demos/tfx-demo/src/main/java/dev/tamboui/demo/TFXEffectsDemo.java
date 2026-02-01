///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS dev.tamboui:tamboui-tfx:LATEST
//DEPS dev.tamboui:tamboui-tui:LATEST
//DEPS dev.tamboui:tamboui-jline3-backend:LATEST

/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.demo;

import dev.tamboui.tfx.TFxDuration;
import dev.tamboui.tfx.Effect;
import dev.tamboui.tfx.EffectManager;
import dev.tamboui.tfx.ExpandDirection;
import dev.tamboui.tfx.Fx;
import dev.tamboui.tfx.Interpolation;
import dev.tamboui.tfx.Motion;
import dev.tamboui.layout.Constraint;
import dev.tamboui.layout.Layout;
import dev.tamboui.layout.Margin;
import dev.tamboui.layout.Rect;
import dev.tamboui.style.Color;
import dev.tamboui.style.Style;
import dev.tamboui.tui.TuiConfig;
import dev.tamboui.tui.TuiRunner;
import dev.tamboui.tui.event.KeyCode;
import dev.tamboui.tui.event.KeyEvent;
import dev.tamboui.tui.event.TickEvent;
import dev.tamboui.widgets.block.Block;
import dev.tamboui.widgets.block.Borders;
import dev.tamboui.widgets.paragraph.Paragraph;
import dev.tamboui.text.Line;
import dev.tamboui.text.Span;
import dev.tamboui.text.Text;
import dev.tamboui.terminal.Frame;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.Supplier;

/**
 * Basic effects demo ported from Rust tachyonfx basic-effects example.
 * <p>
 * This demo showcases various effects that can be applied to terminal content.
 * <p>
 * Controls:
 * - Enter: Next transition
 * - Backspace: Previous transition
 * - Space: Restart transition
 * - r: Random transition
 * - ESC: Quit
 */
public class TFXEffectsDemo {

    private Instant lastFrame;
    private EffectManager effectManager;
    private EffectsRepository effects;
    private int activeEffectIdx = 0;
    private Random rng = new Random();

    // FPS tracking
    private int frameCount = 0;
    private Instant fpsLastUpdate;
    private double currentFps = 0.0;
    
    // Gruvbox colors (approximated with RGB)
    private static final Color DARK0_HARD = Color.rgb(0x1d, 0x20, 0x21);
    private static final Color DARK0_SOFT = Color.rgb(0x32, 0x30, 0x2f);
    private static final Color LIGHT3 = Color.rgb(0xbd, 0xae, 0x93);
    private static final Color LIGHT4 = Color.rgb(0xa8, 0x99, 0x84);
    private static final Color ORANGE = Color.rgb(0xd6, 0x5d, 0x0e);
    private static final Color ORANGE_BRIGHT = Color.rgb(0xfe, 0x80, 0x19);
    private static final Color YELLOW_BRIGHT = Color.rgb(0xfa, 0xbd, 0x2f);

    private TFXEffectsDemo() {

    }

    /**
     * Demo entry point.
     * @param args the CLI arguments
     * @throws Exception on unexpected error
     */
    public static void main(String[] args) throws Exception {
        new TFXEffectsDemo().run();
    }
    
    private void run() throws Exception {
        effectManager = new EffectManager();
        effects = new EffectsRepository();
        
        // Start with first effect
        restartEffect();
        
        TuiConfig config = TuiConfig.builder()
            .tickRate(java.time.Duration.ofMillis(16)) // ~60fps
            .build();
        
        try (TuiRunner tui = TuiRunner.create(config)) {
            tui.run(
                (event, runner) -> {
                   
                    
                    if (event instanceof KeyEvent) {
                        KeyEvent keyEvent = (KeyEvent) event;

                        if (keyEvent.isQuit() || keyEvent.isCancel()) {
                            runner.quit();
                            return false;
                        }
                        
                        if (keyEvent.isKey(KeyCode.ENTER)) {
                            activeEffectIdx = (activeEffectIdx + 1) % effects.size();
                            restartEffect();
                            return true;
                        }
                        
                        if (keyEvent.isKey(KeyCode.BACKSPACE)) {
                            activeEffectIdx = (activeEffectIdx == 0) 
                                ? effects.size() - 1 
                                : activeEffectIdx - 1;
                            restartEffect();
                            return true;
                        }
                        
                        if (keyEvent.isChar(' ')) {
                            restartEffect();
                            return true;
                        }
                        
                        if (keyEvent.isChar('r') || keyEvent.isChar('R')) {
                            activeEffectIdx = rng.nextInt(effects.size());
                            restartEffect();
                            return true;
                        }
                    }
                    
                    if (event instanceof TickEvent) {
                        return true; // Redraw on tick
                    }
                    return false;
                },
                frame -> {
                    // Update FPS counter
                    Instant now = Instant.now();
                    frameCount++;
                    if (fpsLastUpdate == null) {
                        fpsLastUpdate = now;
                    } else {
                        long elapsed = java.time.Duration.between(fpsLastUpdate, now).toMillis();
                        if (elapsed >= 500) { // Update FPS display every 500ms
                            currentFps = frameCount * 1000.0 / elapsed;
                            frameCount = 0;
                            fpsLastUpdate = now;
                        }
                    }

                    // Render UI first
                    render(frame);

                    // Calculate content area for effects
                    Rect area = frame.area();
                    int contentWidth = 80;
                    int contentHeight = 17;
                    int contentX = (area.width() - contentWidth) / 2;
                    int contentY = (area.height() - contentHeight) / 2;

                    if (contentX < 0) {
                        contentX = 0;
                    }
                    if (contentY < 0) {
                        contentY = 0;
                    }

                    Rect contentArea = new Rect(
                        area.left() + contentX,
                        area.top() + contentY,
                        Math.min(contentWidth, area.width() - contentX),
                        Math.min(contentHeight, area.height() - contentY)
                    );

                    // Process effects on content area
                    long deltaMs;
                    if (lastFrame == null) {
                        // First frame - initialize to avoid huge delta
                        lastFrame = now;
                        deltaMs = 16; // Assume ~60fps for first frame
                    } else {
                        deltaMs = java.time.Duration.between(lastFrame, now).toMillis();
                        // Clamp delta to reasonable maximum (e.g., 100ms) to avoid jumps
                        // This handles cases where the app was paused or had a long delay
                        deltaMs = Math.min(deltaMs, 100);
                        lastFrame = now;
                    }

                    TFxDuration delta = TFxDuration.fromMillis(deltaMs);
                    if (effectManager.isRunning()) {
                        effectManager.processEffects(delta, frame.buffer(), contentArea);
                    }
                }
            );
        }
    }
    
    private void restartEffect() {
        effectManager = new EffectManager();
        Effect effect = effects.getEffect(activeEffectIdx);
        effectManager.addEffect(effect);
    }
    
    private void render(Frame frame) {
        Rect area = frame.area();

        // Clear and set background
        frame.buffer().fill(area, new dev.tamboui.buffer.Cell(" ", Style.EMPTY.bg(DARK0_HARD)));

        // Display FPS in top-right corner
        String fpsText = String.format("FPS: %.1f", currentFps);
        int fpsX = area.right() - fpsText.length() - 1;
        if (fpsX >= area.left()) {
            frame.buffer().setString(fpsX, area.top(), fpsText, Style.EMPTY.fg(LIGHT4).bg(DARK0_HARD));
        }
        
        // Create centered content area (80x17)
        int contentWidth = 80;
        int contentHeight = 17;
        int contentX = (area.width() - contentWidth) / 2;
        int contentY = (area.height() - contentHeight) / 2;
        
        if (contentX < 0) contentX = 0;
        if (contentY < 0) contentY = 0;
        
        Rect contentArea = new Rect(
            area.left() + contentX,
            area.top() + contentY,
            Math.min(contentWidth, area.width() - contentX),
            Math.min(contentHeight, area.height() - contentY)
        );
        
        // Draw content block background
        Block block = Block.builder()
            .borders(Borders.NONE)
            .style(Style.EMPTY.bg(DARK0_SOFT))
            .build();
        frame.renderWidget(block, contentArea);
        
        // Inner area with margin
        Rect innerArea = contentArea.inner(Margin.uniform(1));
        
        // Create layout
        Layout layout = Layout.vertical()
            .constraints(
                Constraint.length(2),  // Active animation label
                Constraint.length(7),  // Main text
                Constraint.length(6)   // Shortcuts
            )
            .spacing(0);
        
        List<Rect> splits = layout.split(innerArea);
        
        // Active animation label
        String effectName = effects.getName(activeEffectIdx);
        Text activeAnimation = Text.from(Line.from(
            Span.raw("Active animation: ").fg(ORANGE),
            Span.raw(effectName).fg(ORANGE_BRIGHT)
        ));
        frame.renderWidget(Paragraph.builder()
            .text(activeAnimation)
            .style(Style.EMPTY.bg(DARK0_SOFT))
            .build(), splits.get(0));
        
        // Main text
        Text mainText = Text.from(
            Line.from("Many effects are composable, e.g. `parallel`, `sequence`, `repeating`."),
            Line.from("Most effects have a lifetime, after which they report done()."),
            Line.from("Effects such as `never_complete`, `temporary` influence or override this."),
            Line.from("Symbols: !@#$%^&*()"),
            Line.from("The text in this window will undergo a random transition"),
            Line.from("when any of the following keys are pressed:")
        ).fg(LIGHT3);
        frame.renderWidget(Paragraph.builder()
            .text(mainText)
            .style(Style.EMPTY.bg(DARK0_SOFT))
            .build(), splits.get(1));
        
        // Shortcuts
        Text shortcuts = Text.from(
            shortcut("↵   ", "next transition"),
            shortcut("⌫   ", "previous transition"),
            shortcut("␣   ", "restart transition"),
            shortcut("r   ", "random transition"),
            shortcut("ESC ", "quit")
        );
        frame.renderWidget(Paragraph.builder()
            .text(shortcuts)
            .style(Style.EMPTY.bg(DARK0_SOFT))
            .build(), splits.get(2));
        
        // Apply effect to content area
        // Effects are processed in the render callback below
    }
    
    private Line shortcut(String key, String desc) {
        return Line.from(
            Span.raw(key).fg(YELLOW_BRIGHT).bold(),
            Span.raw(desc).fg(LIGHT4)
        );
    }
    
    private static class EffectsRepository {
        private final List<EffectEntry> effects;
        
        EffectsRepository() {
            effects = new ArrayList<>();
            
            Color screenBg = DARK0_HARD;
            Color bg = DARK0_SOFT;
            
            long slow = 1250;
            long medium = 750;
            
            // Sweep in
            effects.add(new EffectEntry(
                "sweep in",
                () -> Fx.sweepIn(Motion.LEFT_TO_RIGHT, 30, 0, screenBg, slow, Interpolation.QuadOut)
            ));
            
            // Smooth expand and reversed
            effects.add(new EffectEntry(
                "smooth expand and reversed",
                () -> Fx.sequence(
                    Fx.expand(ExpandDirection.VERTICAL, Style.EMPTY.fg(bg).bg(screenBg), 1200, Interpolation.QuadOut),
                    Fx.expand(ExpandDirection.HORIZONTAL, Style.EMPTY.fg(bg).bg(screenBg), 1200, Interpolation.QuadOut).reversed()
                )
            ));
            
            // Irregular sweep out/sweep in
            effects.add(new EffectEntry(
                "irregular sweep out/sweep in",
                () -> Fx.sequence(
                    Fx.sweepOut(Motion.DOWN_TO_UP, 5, 20, bg, 2000, Interpolation.QuadOut),
                    Fx.sweepIn(Motion.UP_TO_DOWN, 5, 20, bg, 2000, Interpolation.QuadOut),
                    Fx.sweepOut(Motion.UP_TO_DOWN, 5, 20, bg, 2000, Interpolation.QuadOut),
                    Fx.sweepIn(Motion.DOWN_TO_UP, 5, 20, bg, 2000, Interpolation.QuadOut)
                )
            ));
            
            // Coalesce
            effects.add(new EffectEntry(
                "coalesce",
                () -> Fx.sequence(
                    Fx.coalesce(medium, Interpolation.CubicOut),
                    Fx.dissolveTo(Style.EMPTY.bg(screenBg), medium, Interpolation.QuadOut)
                )
            ));
            
            // Slide in/out
            effects.add(new EffectEntry(
                "slide in/out",
                () -> Fx.sequence(
                    Fx.parallel(
                        Fx.fadeFromFg(bg, 2000, Interpolation.ExpoInOut),
                        Fx.slideIn(Motion.UP_TO_DOWN, 20, 0, DARK0_HARD, medium, Interpolation.Linear)
                    ),
                    Fx.slideOut(Motion.LEFT_TO_RIGHT, 80, 0, DARK0_HARD, medium, Interpolation.Linear)
                )
            ));
            
            // Change hue, saturation and lightness (simulated with fade effects)
            // Note: HSL shift not available in Java version, using fade sequence instead
            effects.add(new EffectEntry(
                "color transitions",
                () -> Fx.sequence(
                    Fx.fadeToFg(Color.rgb(0xff, 0x00, 0x00), medium, Interpolation.Linear),
                    Fx.fadeToFg(Color.rgb(0x00, 0xff, 0x00), medium, Interpolation.Linear),
                    Fx.fadeToFg(Color.rgb(0x00, 0x00, 0xff), medium, Interpolation.Linear),
                    Fx.fadeToFg(LIGHT3, medium, Interpolation.Linear)
                )
            ));
            
            // Custom color cycle (simulated with repeating fade)
            // Note: Custom effect functions not available, using fade sequence
            // In Rust version, this has CellFilter::FgColor(Light3), but we'll apply to all for simplicity
            effects.add(new EffectEntry(
                "color cycle",
                () -> Fx.sequence(
                    Fx.fadeToFg(Color.rgb(0xff, 0x00, 0x00), slow, Interpolation.SineInOut),
                    Fx.fadeToFg(Color.rgb(0xff, 0xff, 0x00), slow, Interpolation.SineInOut),
                    Fx.fadeToFg(Color.rgb(0x00, 0xff, 0x00), slow, Interpolation.SineInOut),
                    Fx.fadeToFg(Color.rgb(0x00, 0xff, 0xff), slow, Interpolation.SineInOut),
                    Fx.fadeToFg(Color.rgb(0x00, 0x00, 0xff), slow, Interpolation.SineInOut),
                    Fx.fadeToFg(Color.rgb(0xff, 0x00, 0xff), slow, Interpolation.SineInOut),
                    Fx.fadeToFg(LIGHT3, slow, Interpolation.SineInOut)
                )
            ));
        }
        
        Effect getEffect(int idx) {
            return effects.get(idx).effectFactory.get();
        }
        
        String getName(int idx) {
            return effects.get(idx).name;
        }
        
        int size() {
            return effects.size();
        }
        
        private static class EffectEntry {
            final String name;
            final Supplier<Effect> effectFactory;
            
            EffectEntry(String name, Supplier<Effect> effectFactory) {
                this.name = name;
                this.effectFactory = effectFactory;
            }
        }
    }
}

