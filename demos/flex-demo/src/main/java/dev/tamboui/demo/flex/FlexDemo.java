//DEPS dev.tamboui:tamboui-toolkit:LATEST
//DEPS dev.tamboui:tamboui-jline:LATEST
/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.demo.flex;

import dev.tamboui.layout.Flex;
import dev.tamboui.style.Color;
import dev.tamboui.toolkit.app.ToolkitRunner;
import dev.tamboui.toolkit.element.Element;
import dev.tamboui.toolkit.event.EventResult;
import dev.tamboui.tui.TuiConfig;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import static dev.tamboui.toolkit.Toolkit.*;

/**
 * Demo application showcasing Flex layout modes with practical examples.
 *
 * <p>Controls:
 * <ul>
 *   <li>1-4: Switch between examples</li>
 *   <li>Left/Right arrows: Cycle through Flex modes</li>
 *   <li>q: Quit</li>
 * </ul>
 */
public class FlexDemo {

    private static final Flex[] FLEX_MODES = {
            Flex.START, Flex.CENTER, Flex.END,
            Flex.SPACE_BETWEEN, Flex.SPACE_AROUND, Flex.SPACE_EVENLY
    };

    private static final String[] EXAMPLE_NAMES = {
            "1:Fixed",
            "2:Fill",
            "3:Nested",
            "4:Toolbar"
    };

    private static final AtomicInteger modeIndex = new AtomicInteger(0);
    private static final AtomicInteger exampleIndex = new AtomicInteger(0);

    /**
     * Entry point for the flex demo application.
     *
     * @param args command line arguments (not used)
     * @throws Exception if an error occurs during execution
     */
    public static void main(String[] args) throws Exception {
        var config = TuiConfig.builder()
                .tickRate(Duration.ofMillis(100))
                .build();

        try (var runner = ToolkitRunner.create(config)) {
            runner.run(() -> renderUI());
        }
    }

    private static Element renderUI() {
        Flex currentFlex = FLEX_MODES[modeIndex.get()];
        int currentExample = exampleIndex.get();

        return column(
                // Header: tabs + mode info
                panel(() -> column(
                        row(
                                tab(0, currentExample),
                                tab(1, currentExample),
                                tab(2, currentExample),
                                tab(3, currentExample)
                        ).spacing(1).length(1),
                        row(
                                text(" Mode: ").dim(),
                                text(currentFlex.name()).bold().yellow(),
                                text("  [←/→] change mode  [q] quit").dim()
                        ).length(1)
                ))
                .rounded()
                .borderColor(Color.DARK_GRAY)
                .length(4)
                .focusable()
                .onKeyEvent(event -> {
                    if (event.isLeft()) {
                        modeIndex.updateAndGet(i ->
                                (i - 1 + FLEX_MODES.length) % FLEX_MODES.length);
                        return EventResult.HANDLED;
                    }
                    if (event.isRight()) {
                        modeIndex.updateAndGet(i ->
                                (i + 1) % FLEX_MODES.length);
                        return EventResult.HANDLED;
                    }
                    if (event.isChar('1')) {
                        exampleIndex.set(0);
                        return EventResult.HANDLED;
                    }
                    if (event.isChar('2')) {
                        exampleIndex.set(1);
                        return EventResult.HANDLED;
                    }
                    if (event.isChar('3')) {
                        exampleIndex.set(2);
                        return EventResult.HANDLED;
                    }
                    if (event.isChar('4')) {
                        exampleIndex.set(3);
                        return EventResult.HANDLED;
                    }
                    return EventResult.UNHANDLED;
                }),

                // Description
                text(" " + getModeDescription(currentFlex)).dim().length(1),

                // Content based on selected example
                renderExample(currentExample, currentFlex)
        );
    }

    private static Element tab(int index, int currentExample) {
        String name = EXAMPLE_NAMES[index];
        if (index == currentExample) {
            return text(" " + name + " ").bold().black().bg(Color.CYAN);
        } else {
            return text(" " + name + " ").dim();
        }
    }

    private static Element renderExample(int exampleIndex, Flex flex) {
        switch (exampleIndex) {
            case 0:
                return renderFixedSizeItems(flex);
            case 1:
                return renderFixedVsGrowing(flex);
            case 2:
                return renderNestedLayouts(flex);
            case 3:
                return renderToolbar(flex);
            default:
                return renderFixedSizeItems(flex);
        }
    }

    private static String getModeDescription(Flex flex) {
        switch (flex) {
            case START:
                return "START: Elements packed at the beginning";
            case CENTER:
                return "CENTER: Elements centered, equal space on both sides";
            case END:
                return "END: Elements packed at the end";
            case SPACE_BETWEEN:
                return "SPACE_BETWEEN: First/last at edges, equal gaps between";
            case SPACE_AROUND:
                return "SPACE_AROUND: Equal space around each element";
            default:
                return "";
        }
    }

    /**
     * Example 1: Fixed size items in horizontal and vertical layouts.
     */
    private static Element renderFixedSizeItems(Flex flex) {
        return column(
                // Horizontal
                panel(() -> row(
                        box("A", Color.RED),
                        box("B", Color.GREEN),
                        box("C", Color.BLUE),
                        box("D", Color.YELLOW),
                        box("E", Color.CYAN)
                ).flex(flex))
                .title("Horizontal: 5 boxes (width=8)")
                .rounded()
                .borderColor(Color.BLUE)
                .length(5),

                // Vertical
                panel(() -> column(
                        vbar("1", Color.RED),
                        vbar("2", Color.GREEN),
                        vbar("3", Color.BLUE)
                ).flex(flex))
                .title("Vertical: 3 bars (height=3)")
                .rounded()
                .borderColor(Color.GREEN)
                .fill()
        );
    }

    /**
     * Example 2: Mix of fixed-size and growing elements.
     */
    private static Element renderFixedVsGrowing(Flex flex) {
        return column(
                // Fixed width items with flex
                text(" Fixed width items with flex:").bold().length(1),
                row(
                        panel(() -> text("A").bold()).rounded().borderColor(Color.RED).length(10),
                        panel(() -> text("B").bold()).rounded().borderColor(Color.GREEN).length(10),
                        panel(() -> text("C").bold()).rounded().borderColor(Color.BLUE).length(10)
                ).flex(flex).length(4),

                text("").length(1),

                // Mixed: fixed + fill
                text(" Mixed: fixed + fill() - fill takes remaining space:").bold().length(1),
                row(
                        panel(() -> text("Fixed").bold()).rounded().borderColor(Color.YELLOW).length(12),
                        panel(() -> text("Grows").bold()).rounded().borderColor(Color.CYAN).fill(),
                        panel(() -> text("Fixed").bold()).rounded().borderColor(Color.YELLOW).length(12)
                ).length(4),

                text("").length(1),

                // Weighted fills
                text(" Weighted: fill(1) + fill(2) + fill(1):").bold().length(1),
                row(
                        panel(() -> text("1x").bold()).rounded().borderColor(Color.RED).fill(1),
                        panel(() -> text("2x").bold()).rounded().borderColor(Color.GREEN).fill(2),
                        panel(() -> text("1x").bold()).rounded().borderColor(Color.BLUE).fill(1)
                ).length(4),

                spacer()
        );
    }

    /**
     * Example 3: Nested layouts demonstrating flex in containers.
     */
    private static Element renderNestedLayouts(Flex flex) {
        return row(
                // Left: Vertical layout with fixed items
                panel(() -> column(
                        panel(() -> text("Top")).rounded().borderColor(Color.RED).length(3),
                        panel(() -> text("Mid")).rounded().borderColor(Color.GREEN).length(3),
                        panel(() -> text("Bot")).rounded().borderColor(Color.BLUE).length(3)
                ).flex(flex))
                .title("Column flex")
                .rounded()
                .borderColor(Color.CYAN)
                .fill(),

                // Center: Nested structures
                panel(() -> column(
                        text(" Row with flex:").bold().length(1),
                        row(
                                panel(() -> text("L")).rounded().borderColor(Color.RED).length(6),
                                panel(() -> text("M")).rounded().borderColor(Color.GREEN).length(6),
                                panel(() -> text("R")).rounded().borderColor(Color.BLUE).length(6)
                        ).flex(flex).length(4),
                        text("").length(1),
                        text(" Column with flex:").bold().length(1),
                        column(
                                panel(() -> text("1")).rounded().borderColor(Color.YELLOW).length(3),
                                panel(() -> text("2")).rounded().borderColor(Color.MAGENTA).length(3),
                                panel(() -> text("3")).rounded().borderColor(Color.CYAN).length(3)
                        ).flex(flex).fill()
                ))
                .title("Mixed nesting")
                .rounded()
                .borderColor(Color.YELLOW)
                .fill(),

                // Right: Deep nesting
                panel(() -> column(
                        panel(() -> row(
                                box("A", Color.RED),
                                box("B", Color.GREEN)
                        ).flex(flex))
                        .rounded()
                        .borderColor(Color.WHITE)
                        .length(5),

                        panel(() -> column(
                                vbar("X", Color.CYAN),
                                vbar("Y", Color.MAGENTA)
                        ).flex(flex))
                        .rounded()
                        .borderColor(Color.WHITE)
                        .fill()
                ))
                .title("Deep nesting")
                .rounded()
                .borderColor(Color.MAGENTA)
                .fill()
        );
    }

    /**
     * Example 4: Practical toolbar examples.
     */
    private static Element renderToolbar(Flex flex) {
        return column(
                // Simple toolbar
                panel(() -> row(
                        button("New", Color.GREEN),
                        button("Open", Color.BLUE),
                        button("Save", Color.CYAN),
                        button("Help", Color.YELLOW),
                        button("Quit", Color.RED)
                ).flex(flex).spacing(1))
                .title("Simple toolbar")
                .rounded()
                .borderColor(Color.BLUE)
                .length(5),

                // Status bar style
                panel(() -> row(
                        text(" Ready").bold().green(),
                        text(" | Line 42, Col 15").dim(),
                        text(" | UTF-8").dim()
                ).flex(flex))
                .title("Status bar")
                .rounded()
                .borderColor(Color.GREEN)
                .length(3),

                // Menu bar style
                panel(() -> row(
                        menuItem("File"),
                        menuItem("Edit"),
                        menuItem("View"),
                        menuItem("Help")
                ).flex(flex).spacing(2))
                .title("Menu bar")
                .rounded()
                .borderColor(Color.YELLOW)
                .length(3),

                spacer()
        );
    }

    private static Element box(String label, Color color) {
        return panel(() -> text(label).bold())
                .rounded()
                .borderColor(color)
                .length(8);
    }

    private static Element vbar(String label, Color color) {
        return panel(() -> text(" " + label).bold())
                .rounded()
                .borderColor(color)
                .length(3);
    }

    private static Element button(String label, Color color) {
        return panel(() -> text(" " + label + " ").bold())
                .rounded()
                .borderColor(color)
                .length(label.length() + 4);
    }

    private static Element menuItem(String label) {
        return text(" " + label + " ").bold();
    }
}
