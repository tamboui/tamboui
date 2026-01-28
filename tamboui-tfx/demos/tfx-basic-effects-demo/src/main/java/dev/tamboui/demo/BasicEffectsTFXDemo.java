///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS dev.tamboui:tamboui-tfx:LATEST
//DEPS dev.tamboui:tamboui-tui:LATEST
//DEPS dev.tamboui:tamboui-jline3-backend:LATEST

/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.demo;

import dev.tamboui.tfx.CellFilter;
import dev.tamboui.tfx.TFxColorSpace;
import dev.tamboui.tfx.TFxDuration;
import dev.tamboui.tfx.Effect;
import dev.tamboui.tfx.EffectManager;
import dev.tamboui.tfx.Fx;
import dev.tamboui.tfx.Interpolation;
import dev.tamboui.tfx.Motion;
import dev.tamboui.tfx.pattern.DiagonalPattern;
import dev.tamboui.tfx.pattern.RadialPattern;
import dev.tamboui.tfx.pattern.SweepPattern;
import dev.tamboui.layout.Alignment;
import dev.tamboui.layout.Constraint;
import dev.tamboui.layout.Layout;
import dev.tamboui.layout.Rect;
import dev.tamboui.style.Color;
import dev.tamboui.style.Style;
import dev.tamboui.tui.TuiConfig;
import dev.tamboui.tui.TuiRunner;
import dev.tamboui.tui.event.KeyEvent;
import dev.tamboui.tui.event.TickEvent;
import dev.tamboui.widgets.block.Block;
import dev.tamboui.widgets.block.Borders;
import dev.tamboui.widgets.block.Title;
import dev.tamboui.widgets.list.ListItem;
import dev.tamboui.widgets.list.ListState;
import dev.tamboui.widgets.list.ListWidget;
import dev.tamboui.widgets.paragraph.Paragraph;
import dev.tamboui.text.Line;
import dev.tamboui.text.Span;
import dev.tamboui.text.Text;
import dev.tamboui.terminal.Frame;
import dev.tamboui.buffer.Cell;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Interactive demo showcasing TamboUI TFX effects.
 * <p>
 * Left panel: List of available effects (navigate with Up/Down arrows)
 * Center panel: Content area showing various text and widgets
 * <p>
 * Controls:
 * - Up/Down arrows: Navigate effect list
 * - Enter/Space: Trigger selected effect
 * - 't': Toggle between text and colored blocks view
 * - 'q': Quit
 */
public class BasicEffectsTFXDemo {
    
    private Instant lastFrame = Instant.now();
    private EffectManager effectManager;
    private ListState listState;
    private List<EffectDemo> demos;
    private Integer selectedEffectIndex;
    private boolean effectRunning = false;
    private boolean showColoredBlocks = false;
    private List<Integer> itemToDemoIndex; // Maps item index to demo index (-1 for headers/spacers)
    
    private static class EffectDemo {
        final String name;
        final String category;
        final Supplier<Effect> effectFactory;
        
        EffectDemo(String name, String category, Supplier<Effect> effectFactory) {
            this.name = name;
            this.category = category;
            this.effectFactory = effectFactory;
        }
    }

    private BasicEffectsTFXDemo() {

    }

    /**
     * Demo entry point.
     * @param args the CLI arguments
     * @throws Exception on unexpected error
     */
    public static void main(String[] args) throws Exception {
        new BasicEffectsTFXDemo().run();
    }
    
    private void run() throws Exception {
        effectManager = new EffectManager();
        listState = new ListState();
        initializeDemos();
        
        // Select first item
        listState.selectFirst();
        selectedEffectIndex = 0;
        
        TuiConfig config = TuiConfig.builder()
            .tickRate(java.time.Duration.ofMillis(16)) // ~60fps
            .build();
        
        try (TuiRunner tui = TuiRunner.create(config)) {
            tui.run(
                (event, runner) -> {
                   
                    
                    if (event instanceof KeyEvent) {

                        KeyEvent keyEvent = (KeyEvent) event;
                        
                        if (keyEvent.isQuit()) {
                            runner.quit();
                            return false;
                        }

                        // Navigation
                        if (keyEvent.isUp()) {
                            navigateUp();
                            return true;
                        }
                        if (keyEvent.isDown()) {
                            navigateDown();
                            return true;
                        }
                        
                        // Trigger effect
                        if (keyEvent.isConfirm() || keyEvent.isSelect()) {
                            triggerSelectedEffect();
                            return true;
                        }
                        
                        // Toggle view mode
                        if (keyEvent.isChar('t') || keyEvent.isChar('T')) {
                            showColoredBlocks = !showColoredBlocks;
                            return true;
                        }
                    }
                    
                    if (event instanceof TickEvent) {
                        return true; // Redraw on tick
                    }
                    return false;
                },
                frame -> {
                    // Render UI
                    render(frame);
                    
                    // Process effects
                    Instant now = Instant.now();
                    long deltaMs = java.time.Duration.between(lastFrame, now).toMillis();
                    lastFrame = now;
                    
                    TFxDuration delta = TFxDuration.fromMillis(deltaMs);
                    effectManager.processEffects(delta, frame.buffer(), frame.area());
                    
                    // Auto-restart effect when done
                    if (effectRunning && !effectManager.isRunning()) {
                        effectRunning = false;
                    }
                }
            );
        }
    }
    
    
    /**
     * Finds the next selectable index in the given direction.
     * 
     * @param startIdx The starting index
     * @param itemToDemoIndex The mapping from item index to demo index
     * @param direction True for forward (down), false for backward (up)
     * @return The next selectable index, or -1 if none found
     */
    private void navigateUp() {
        if (itemToDemoIndex == null || itemToDemoIndex.isEmpty()) {
            listState.selectPrevious();
            return;
        }
        
        Integer current = listState.selected();
        if (current == null || current <= 0) {
            return;
        }
        
        // Find the previous selectable item
        int nextIdx = findNextSelectableIndex(current, itemToDemoIndex, false);
        if (nextIdx >= 0) {
            listState.select(nextIdx);
        } else {
            // No selectable item found going up, stay where we are
            listState.selectPrevious();
        }
    }
    
    private void navigateDown() {
        if (itemToDemoIndex == null || itemToDemoIndex.isEmpty()) {
            Integer current = listState.selected();
            if (current == null) {
                listState.selectFirst();
            } else {
                listState.selectNext(itemToDemoIndex.size());
            }
            return;
        }
        
        Integer current = listState.selected();
        if (current == null) {
            listState.selectFirst();
            return;
        }
        
        if (current >= itemToDemoIndex.size() - 1) {
            return; // Already at the end
        }
        
        // Find the next selectable item
        int nextIdx = findNextSelectableIndex(current, itemToDemoIndex, true);
        if (nextIdx >= 0) {
            listState.select(nextIdx);
        } else {
            // No selectable item found going down, try normal next
            listState.selectNext(itemToDemoIndex.size());
        }
    }
    
    private int findNextSelectableIndex(int startIdx, List<Integer> itemToDemoIndex, boolean direction) {
        if (direction) {
            // Search forward (down)
            for (int i = startIdx + 1; i < itemToDemoIndex.size(); i++) {
                if (itemToDemoIndex.get(i) >= 0) {
                    return i;
                }
            }
        } else {
            // Search backward (up)
            for (int i = startIdx - 1; i >= 0; i--) {
                if (itemToDemoIndex.get(i) >= 0) {
                    return i;
                }
            }
        }
        return -1;
    }
    
    /**
     * Finds the next selectable index (searches forward first, then backward).
     * This is used as a fallback when we don't know the direction.
     */
    private int findNextSelectableIndex(int startIdx, List<Integer> itemToDemoIndex) {
        // Search forward first
        int forward = findNextSelectableIndex(startIdx, itemToDemoIndex, true);
        if (forward >= 0) {
            return forward;
        }
        // If nothing forward, search backward
        return findNextSelectableIndex(startIdx, itemToDemoIndex, false);
    }
    
    private void triggerSelectedEffect() {
        if (selectedEffectIndex != null && selectedEffectIndex >= 0 && selectedEffectIndex < demos.size()) {
            effectManager = new EffectManager();
            // Create a fresh effect instance using the factory
            effectManager.addEffect(demos.get(selectedEffectIndex).effectFactory.get());
            effectRunning = true;
        }
    }
    
    private void initializeDemos() {
        demos = new ArrayList<>();
        
        // Fade effects
        demos.add(new EffectDemo("Fade To (RGB)", "Fade",
            () -> Fx.fadeToFg(Color.CYAN, 2000, Interpolation.SineInOut)
                .withFilter(CellFilter.text())
                .withColorSpace(TFxColorSpace.RGB)));
        
        demos.add(new EffectDemo("Fade To (HSL)", "Fade",
            () -> Fx.fadeToFg(Color.CYAN, 2000, Interpolation.SineInOut)
                .withFilter(CellFilter.text())
                .withColorSpace(TFxColorSpace.HSL)));
        
        demos.add(new EffectDemo("Fade From", "Fade",
            () -> Fx.fadeFromFg(Color.CYAN, 2000, Interpolation.SineInOut)
                .withFilter(CellFilter.text())));
        
        // Dissolve effects
        demos.add(new EffectDemo("Dissolve", "Dissolve",
            () -> Fx.dissolve(2000, Interpolation.Linear)
                .withFilter(CellFilter.text())));
        
        demos.add(new EffectDemo("Dissolve To", "Dissolve",
            () -> Fx.dissolveTo(Style.EMPTY.bg(Color.RED), 2000, Interpolation.QuadOut)
                .withFilter(CellFilter.text())));
        
        demos.add(new EffectDemo("Coalesce", "Dissolve",
            () -> Fx.coalesce(2000, Interpolation.QuadIn)
                .withFilter(CellFilter.text())));
        
        // Sweep effects (no filter - affects all cells including backgrounds)
        demos.add(new EffectDemo("Sweep In (L→R)", "Sweep",
            () -> Fx.sweepIn(Motion.LEFT_TO_RIGHT, 10, 0, Color.BLUE, 2000, Interpolation.QuadOut)));
        
        demos.add(new EffectDemo("Sweep In (R→L)", "Sweep",
            () -> Fx.sweepIn(Motion.RIGHT_TO_LEFT, 10, 0, Color.GREEN, 2000, Interpolation.QuadOut)));
        
        demos.add(new EffectDemo("Sweep In (U→D)", "Sweep",
            () -> Fx.sweepIn(Motion.UP_TO_DOWN, 10, 0, Color.MAGENTA, 2000, Interpolation.QuadOut)));
        
        demos.add(new EffectDemo("Sweep Out (D→U)", "Sweep",
            () -> Fx.sweepOut(Motion.DOWN_TO_UP, 10, 0, Color.YELLOW, 2000, Interpolation.QuadOut)));
        
        // Paint effects
        demos.add(new EffectDemo("Paint FG", "Paint",
            () -> Fx.paintFg(Color.RED, 1500, Interpolation.SineInOut)
                .withFilter(CellFilter.text())));
        
        demos.add(new EffectDemo("Paint BG", "Paint",
            () -> Fx.paintBg(Color.BLUE, 1500, Interpolation.SineInOut)
                .withFilter(CellFilter.text())));
        
        demos.add(new EffectDemo("Paint Both", "Paint",
            () -> Fx.paint(Color.YELLOW, Color.CYAN, 1500, Interpolation.SineInOut)
                .withFilter(CellFilter.text())));
        
        // Patterns with fade (affects text+symbols, not backgrounds)
        demos.add(new EffectDemo("Fade + Sweep (L→R)", "Patterns",
            () -> Fx.fadeToFg(Color.CYAN, 2000, Interpolation.SineInOut)
                .withPattern(SweepPattern.leftToRight(15.0f))
                .withFilter(CellFilter.text())));
        
        demos.add(new EffectDemo("Fade + Radial", "Patterns",
            () -> Fx.fadeToFg(Color.GREEN, 2000, Interpolation.SineInOut)
                .withPattern(RadialPattern.center().withTransitionWidth(10.0f))
                .withFilter(CellFilter.text())));
        
        demos.add(new EffectDemo("Fade + Diagonal", "Patterns",
            () -> Fx.fadeToFg(Color.MAGENTA, 2000, Interpolation.SineInOut)
                .withPattern(DiagonalPattern.topLeftToBottomRight().withTransitionWidth(5.0f))
                .withFilter(CellFilter.text())));
        
        // Composition
        demos.add(new EffectDemo("Sequence", "Composition",
            () -> Fx.sequence(
                Fx.fadeFromFg(Color.CYAN, 1500, Interpolation.SineInOut)
                    .withFilter(CellFilter.text()),
                Fx.dissolve(2000, Interpolation.QuadOut)
                    .withFilter(CellFilter.text())
            )));
        
        demos.add(new EffectDemo("Parallel", "Composition",
            () -> Fx.parallel(
                Fx.fadeToFg(Color.CYAN, 2000, Interpolation.SineInOut)
                    .withFilter(CellFilter.text()),
                Fx.dissolve(2500, Interpolation.QuadOut)
                    .withFilter(CellFilter.text())
            )));
        
        // Interpolation types
        demos.add(new EffectDemo("Bounce Out", "Interpolation",
            () -> Fx.fadeToFg(Color.CYAN, 2000, Interpolation.BounceOut)
                .withFilter(CellFilter.text())));
        
        demos.add(new EffectDemo("Elastic Out", "Interpolation",
            () -> Fx.fadeToFg(Color.GREEN, 2000, Interpolation.ElasticOut)
                .withFilter(CellFilter.text())));
    }
    
    private void render(Frame frame) {
        Rect area = frame.area();
        
        // Split into left (list) and right (content) panels
        Layout layout = Layout.horizontal()
            .constraints(
                Constraint.percentage(35),  // Left panel: 35%
                Constraint.fill()            // Right panel: remaining
            )
            .spacing(1);
        
        List<Rect> splits = layout.split(area);
        Rect leftArea = splits.get(0);
        Rect rightArea = splits.get(1);
        
        // Render left panel: Effect list
        renderEffectList(frame, leftArea);
        
        // Render right panel: Content area
        renderContentArea(frame, rightArea);
    }
    
    private void renderEffectList(Frame frame, Rect area) {
        // Build list items - map item indices to demo indices
        List<ListItem> items = new ArrayList<>();
        itemToDemoIndex = new ArrayList<>(); // Maps item index to demo index
        String currentCategory = null;
        
        for (int i = 0; i < demos.size(); i++) {
            EffectDemo demo = demos.get(i);
            
            // Add category header if category changed
            if (currentCategory == null || !currentCategory.equals(demo.category)) {
                if (currentCategory != null) {
                    items.add(ListItem.from("")); // Spacer
                    itemToDemoIndex.add(-1); // Spacer doesn't map to a demo
                }
                currentCategory = demo.category;
                items.add(ListItem.from(
                    Text.from(Line.from(Span.raw("── " + demo.category + " ──").dim()))
                ));
                itemToDemoIndex.add(-1); // Header doesn't map to a demo
            }
            
            // Add effect item
            items.add(ListItem.from(demo.name));
            itemToDemoIndex.add(i); // This item maps to demo index i
        }
        
        // Update selectedEffectIndex based on listState selection
        // Also ensure we don't select headers/spacers
        Integer listSelected = listState.selected();
        if (listSelected != null && listSelected >= 0 && listSelected < itemToDemoIndex.size()) {
            int demoIdx = itemToDemoIndex.get(listSelected);
            if (demoIdx >= 0) {
                selectedEffectIndex = demoIdx;
            } else {
                // Selected a header/spacer, find next selectable item
                int nextIdx = findNextSelectableIndex(listSelected, itemToDemoIndex);
                if (nextIdx >= 0) {
                    listState.select(nextIdx);
                    selectedEffectIndex = itemToDemoIndex.get(nextIdx);
                }
            }
        }
        
        // Update scroll to keep selected visible
        listState.scrollToSelected(area.height(), items);
        
        ListWidget list = ListWidget.builder()
            .items(items)
            .highlightStyle(Style.EMPTY.fg(Color.YELLOW).bold())
            .highlightSymbol("▶ ")
            .block(Block.builder()
                .borders(Borders.ALL)
                .title(Title.from(Line.from(
                    Span.raw("Effects "),
                    Span.raw("(" + demos.size() + ")").dim()
                )))
                .build())
            .build();
        
        frame.renderStatefulWidget(list, area, listState);
    }
    
    private void renderContentArea(Frame frame, Rect area) {
        Block block = Block.builder()
            .borders(Borders.ALL)
            .title(Title.from(Line.from(Span.raw(
                showColoredBlocks ? "Content Area (Colored Blocks)" : "Content Area (Text)"
            ))))
            .build();
        
        Rect innerArea = block.inner(area);
        frame.renderWidget(block, area);
        
        if (showColoredBlocks) {
            renderColoredBlocks(frame, innerArea);
        } else {
            renderTextContent(frame, innerArea);
        }
    }
    
    private void renderTextContent(Frame frame, Rect area) {
        // Create content with various text elements to demonstrate effects
        StringBuilder content = new StringBuilder();
        content.append("TamboUI TFX Effects Demo\n");
        content.append("════════════════════\n\n");
        
        if (selectedEffectIndex != null && selectedEffectIndex < demos.size()) {
            EffectDemo selected = demos.get(selectedEffectIndex);
            content.append("Selected: ").append(selected.name).append("\n");
            content.append("Category: ").append(selected.category).append("\n\n");
        }
        
        content.append("This is a demonstration of various effects.\n");
        content.append("The effects will be applied to this text content.\n\n");
        
        content.append("Sample Text Elements:\n");
        content.append("─────────────────────\n\n");
        
        content.append("• Regular text line\n");
        content.append("• Another text line with more content\n");
        content.append("• Short line\n");
        content.append("• A longer line of text that demonstrates how effects work across different line lengths\n");
        content.append("• Numbers: 1234567890\n");
        content.append("• Symbols: !@#$%^&*()\n");
        content.append("• Mixed: Hello World 123!\n\n");
        
        content.append("Status: ");
        if (effectRunning && effectManager.isRunning()) {
            content.append("Running...");
        } else if (effectRunning) {
            content.append("Complete");
        } else {
            content.append("Ready - Press Enter/Space to trigger");
        }
        content.append("\n\n");
        
        content.append("Controls:\n");
        content.append("  ↑/↓  - Navigate effects\n");
        content.append("  Enter/Space - Trigger effect\n");
        content.append("  t - Toggle text/blocks view\n");
        content.append("  q - Quit\n");
        
        Paragraph paragraph = Paragraph.builder()
            .text(Text.from(content.toString()))
            .alignment(Alignment.LEFT)
            .build();
        
        frame.renderWidget(paragraph, area);
    }
    
    private void renderColoredBlocks(Frame frame, Rect area) {
        // Fill the area with colored blocks in a grid pattern
        // Use different colors to create a visual pattern
        Color[] colors = {
            Color.RED, Color.GREEN, Color.BLUE, Color.YELLOW,
            Color.CYAN, Color.MAGENTA, Color.WHITE, Color.BLACK
        };
        
        int blockWidth = 4;
        int blockHeight = 2;
        int colorIndex = 0;
        
        for (int y = area.top(); y < area.bottom(); y += blockHeight) {
            for (int x = area.left(); x < area.right(); x += blockWidth) {
                Color bgColor = colors[colorIndex % colors.length];
                colorIndex++;
                
                // Create a block area
                Rect blockArea = new Rect(
                    x,
                    y,
                    Math.min(blockWidth, area.right() - x),
                    Math.min(blockHeight, area.bottom() - y)
                );
                
                // Fill with colored cell (using full block character for solid color)
                Cell coloredCell = new Cell(
                    "X",
                    Style.EMPTY.bg(bgColor)
                );
                
                frame.buffer().fill(blockArea, coloredCell);
            }
        }
        
        // Add a small info text at the top
        if (area.height() > 2) {
            String info = "Colored Blocks View - Press 't' to toggle";
            int infoY = area.top();
            int infoX = area.left() + (area.width() - info.length()) / 2;
            if (infoX >= area.left() && infoX + info.length() <= area.right()) {
                frame.buffer().setString(
                    infoX,
                    infoY,
                    info,
                    Style.EMPTY.fg(Color.WHITE).bold()
                );
            }
        }
    }
}
