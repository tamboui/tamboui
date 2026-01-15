//DEPS dev.tamboui:tamboui-tui:LATEST
//DEPS dev.tamboui:tamboui-jline:LATEST
/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.demo.flex;

import dev.tamboui.buffer.Buffer;
import dev.tamboui.buffer.Cell;
import dev.tamboui.layout.Alignment;
import dev.tamboui.layout.Constraint;
import dev.tamboui.layout.Flex;
import dev.tamboui.layout.Layout;
import dev.tamboui.layout.Rect;
import dev.tamboui.style.Color;
import dev.tamboui.style.Style;
import dev.tamboui.terminal.Frame;
import dev.tamboui.text.Line;
import dev.tamboui.text.Span;
import dev.tamboui.text.Text;
import dev.tamboui.tui.TuiConfig;
import dev.tamboui.tui.TuiRunner;
import dev.tamboui.tui.event.Event;
import dev.tamboui.tui.event.KeyCode;
import dev.tamboui.tui.event.KeyEvent;
import dev.tamboui.widgets.block.Block;
import dev.tamboui.widgets.block.BorderType;
import dev.tamboui.widgets.block.Borders;
import dev.tamboui.widgets.block.Title;
import dev.tamboui.widgets.paragraph.Paragraph;
import dev.tamboui.widgets.scrollbar.Scrollbar;
import dev.tamboui.widgets.scrollbar.ScrollbarOrientation;
import dev.tamboui.widgets.scrollbar.ScrollbarState;
import dev.tamboui.widgets.tabs.Tabs;
import dev.tamboui.widgets.tabs.TabsState;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Ratatui-like flex demo (ported as closely as possible).
 *
 * <p>This is a TamboUI port of Ratatui's `examples/apps/flex` demo, aiming to match:
 * tabs, colors (Tailwind palette), borders, spacer rendering and interactions.
 *
 * <p>Controls:
 * <ul>
 *   <li>h/l or ←/→: Change tab</li>
 *   <li>j/k or ↓/↑: Scroll</li>
 *   <li>g/G or Home/End: Jump</li>
 *   <li>+/-: Change spacing</li>
 *   <li>q or Esc: Quit</li>
 * </ul>
 */
public final class RFlexDemo {

    // Tailwind palette values (match ratatui::style::palette::tailwind).
    private static final Color TW_BLACK = Color.rgb(0x00, 0x00, 0x00);
    private static final Color TW_WHITE = Color.rgb(0xff, 0xff, 0xff);

    private static final Color SLATE_400 = Color.rgb(0x94, 0xa3, 0xb8);
    private static final Color SLATE_700 = Color.rgb(0x33, 0x41, 0x55);
    private static final Color SLATE_800 = Color.rgb(0x1e, 0x29, 0x3b);
    private static final Color SLATE_900 = Color.rgb(0x0f, 0x17, 0x2a);
    private static final Color SLATE_950 = Color.rgb(0x02, 0x06, 0x17);

    private static final Color BLUE_800 = Color.rgb(0x1e, 0x40, 0xaf);
    private static final Color BLUE_900 = Color.rgb(0x1e, 0x3a, 0x8a);

    private static final Color ORANGE_400 = Color.rgb(0xfb, 0x92, 0x3c);
    private static final Color SKY_200 = Color.rgb(0xba, 0xe6, 0xfd);
    private static final Color SKY_300 = Color.rgb(0x7d, 0xd3, 0xfc);
    private static final Color SKY_400 = Color.rgb(0x38, 0xbd, 0xf8);
    private static final Color INDIGO_300 = Color.rgb(0xa5, 0xb4, 0xfc);
    private static final Color INDIGO_400 = Color.rgb(0x81, 0x8c, 0xf8);
    private static final Color INDIGO_500 = Color.rgb(0x63, 0x66, 0xf1);

    private static final ExampleData[] EXAMPLE_DATA = {
        new ExampleData(
            "Min(u16) takes any excess space always",
            new Constraint[] {
                Constraint.length(10), Constraint.min(10), Constraint.max(10), Constraint.percentage(10), Constraint.ratio(1, 10)
            },
            null
        ),
        new ExampleData(
            "Fill(u16) takes any excess space always",
            new Constraint[] { Constraint.length(20), Constraint.percentage(20), Constraint.ratio(1, 5), Constraint.fill(1) },
            null
        ),
        new ExampleData(
            "Here's all constraints in one line",
            new Constraint[] {
                Constraint.length(10),
                Constraint.min(10),
                Constraint.max(10),
                Constraint.percentage(10),
                Constraint.ratio(1, 10),
                Constraint.fill(1)
            },
            null
        ),
        new ExampleData("", new Constraint[] { Constraint.max(50), Constraint.min(50) }, null),
        new ExampleData("", new Constraint[] { Constraint.max(20), Constraint.length(10) }, null),
        new ExampleData("", new Constraint[] { Constraint.max(20), Constraint.length(10) }, null),
        new ExampleData(
            "Min grows always but also allows Fill to grow",
            new Constraint[] { Constraint.percentage(50), Constraint.fill(1), Constraint.fill(2), Constraint.min(50) },
            null
        ),
        new ExampleData(
            "In `Legacy`, the last constraint of lowest priority takes excess space",
            new Constraint[] { Constraint.length(20), Constraint.length(20), Constraint.percentage(20) },
            null
        ),
        new ExampleData("", new Constraint[] { Constraint.length(20), Constraint.percentage(20), Constraint.length(20) }, null),
        new ExampleData(
            "A lowest priority constraint will be broken before a high priority constraint",
            new Constraint[] { Constraint.ratio(1, 4), Constraint.percentage(20) },
            null
        ),
        new ExampleData(
            "`Length` is higher priority than `Percentage`",
            new Constraint[] { Constraint.percentage(20), Constraint.length(10) },
            null
        ),
        new ExampleData(
            "`Min/Max` is higher priority than `Length`",
            new Constraint[] { Constraint.length(10), Constraint.max(20) },
            null
        ),
        new ExampleData("", new Constraint[] { Constraint.length(100), Constraint.min(20) }, null),
        new ExampleData(
            "`Length` is higher priority than `Min/Max`",
            new Constraint[] { Constraint.max(20), Constraint.length(10) },
            null
        ),
        new ExampleData("", new Constraint[] { Constraint.min(20), Constraint.length(90) }, null),
        new ExampleData(
            "Fill is the lowest priority and will fill any excess space",
            new Constraint[] { Constraint.fill(1), Constraint.ratio(1, 4) },
            null
        ),
        new ExampleData(
            "Fill can be used to scale proportionally with other Fill blocks",
            new Constraint[] { Constraint.fill(1), Constraint.percentage(20), Constraint.fill(2) },
            null
        ),
        new ExampleData("", new Constraint[] { Constraint.ratio(1, 3), Constraint.percentage(20), Constraint.ratio(2, 3) }, null),
        new ExampleData(
            "Legacy will stretch the last lowest priority constraint\nStretch will only stretch equal weighted constraints",
            new Constraint[] { Constraint.length(20), Constraint.length(15) },
            null
        ),
        new ExampleData("", new Constraint[] { Constraint.percentage(20), Constraint.length(15) }, null),
        new ExampleData(
            "`Fill(u16)` fills up excess space, but is lower priority to spacers.\n"
                + "i.e. Fill will only have widths in Flex::Stretch and Flex::Legacy",
            new Constraint[] { Constraint.fill(1), Constraint.fill(1) },
            null
        ),
        new ExampleData("", new Constraint[] { Constraint.length(20), Constraint.length(20) }, null),
        new ExampleData(
            "When not using `Flex::Stretch` or `Flex::Legacy`,\n`Min(u16)` and `Max(u16)` collapse to their lowest values",
            new Constraint[] { Constraint.min(20), Constraint.max(20) },
            null
        ),
        new ExampleData("", new Constraint[] { Constraint.max(20) }, null),
        new ExampleData("", new Constraint[] { Constraint.min(20), Constraint.max(20), Constraint.length(20), Constraint.length(20) }, null),

        new ExampleData("", new Constraint[] { Constraint.fill(0), Constraint.fill(0) }, null),
        new ExampleData(
            "`Fill(1)` can be to scale with respect to other `Fill(2)`",
            new Constraint[] { Constraint.fill(1), Constraint.fill(2) },
            null
        ),
        new ExampleData("", new Constraint[] { Constraint.fill(1), Constraint.min(10), Constraint.max(10), Constraint.fill(2) }, null),
        new ExampleData(
            "`Fill(0)` collapses if there are other non-zero `Fill(_)`\nconstraints. e.g. `[Fill(0), Fill(0), Fill(1)]`:",
            new Constraint[] { Constraint.fill(0), Constraint.fill(0), Constraint.fill(1) },
            null
        )
    };

    private enum SelectedTab {
        START("Start", 0x38bdf8, Flex.START),
        CENTER("Center", 0x7dd3fc, Flex.CENTER),
        END("End", 0xbae6fd, Flex.END),
        SPACE_AROUND("SpaceAround", 0x6366f1, Flex.SPACE_AROUND),
        SPACE_EVENLY("SpaceEvenly", 0x818cf8, Flex.SPACE_EVENLY),
        SPACE_BETWEEN("SpaceBetween", 0xa5b4fc, Flex.SPACE_BETWEEN);

        final String label;
        final int colorRgb;
        final Flex flex;

        SelectedTab(String label, int colorRgb, Flex flex) {
            this.label = label;
            this.colorRgb = colorRgb;
            this.flex = flex;
        }

        Color color() {
            return Color.rgb((colorRgb >> 16) & 0xff, (colorRgb >> 8) & 0xff, colorRgb & 0xff);
        }

        static SelectedTab fromIndex(int idx) {
            SelectedTab[] values = values();
            return values[Math.max(0, Math.min(values.length - 1, idx))];
        }
    }

    private SelectedTab selectedTab = SelectedTab.START;
    private int scrollOffset = 0;
    private int spacing = 0;

    public static void main(String[] args) throws Exception {
        new RFlexDemo().run();
    }

    public void run() throws Exception {
        TuiConfig config = TuiConfig.builder()
            .tickRate(Duration.ofMillis(100))
            .build();

        try (TuiRunner tui = TuiRunner.create(config)) {
            tui.run(this::handleEvent, this::render);
        }
    }

    private boolean handleEvent(Event event, TuiRunner runner) {
        if (!(event instanceof KeyEvent)) {
            return true;
        }

        KeyEvent k = (KeyEvent) event;
        if (k.isQuit() || k.code() == KeyCode.ESCAPE) {
            runner.quit();
            return false;
        }

        if (k.isRight() || (k.code() == KeyCode.CHAR && k.character() == 'l')) {
            selectedTab = SelectedTab.fromIndex(selectedTab.ordinal() + 1);
            return true;
        }
        if (k.isLeft() || (k.code() == KeyCode.CHAR && k.character() == 'h')) {
            selectedTab = SelectedTab.fromIndex(selectedTab.ordinal() - 1);
            return true;
        }

        if (k.isDown() || (k.code() == KeyCode.CHAR && k.character() == 'j')) {
            scrollOffset = Math.min(maxScrollOffset(), scrollOffset + 1);
            return true;
        }
        if (k.isUp() || (k.code() == KeyCode.CHAR && k.character() == 'k')) {
            scrollOffset = Math.max(0, scrollOffset - 1);
            return true;
        }

        if (k.code() == KeyCode.HOME || (k.code() == KeyCode.CHAR && k.character() == 'g')) {
            scrollOffset = 0;
            return true;
        }
        if (k.code() == KeyCode.END || (k.code() == KeyCode.CHAR && k.character() == 'G')) {
            scrollOffset = maxScrollOffset();
            return true;
        }

        if (k.code() == KeyCode.CHAR && k.character() == '+') {
            spacing = Math.min(10, spacing + 1);
            return true;
        }
        if (k.code() == KeyCode.CHAR && k.character() == '-') {
            spacing = Math.max(0, spacing - 1);
            return true;
        }

        return true;
    }

    private int maxScrollOffset() {
        int totalHeight = examplesTotalHeight();
        int last = EXAMPLE_DATA.length == 0 ? 0 : exampleHeight(EXAMPLE_DATA[EXAMPLE_DATA.length - 1]);
        return Math.max(0, totalHeight - last);
    }

    private int examplesTotalHeight() {
        int sum = 0;
        for (ExampleData ex : EXAMPLE_DATA) {
            sum += exampleHeight(ex);
        }
        return sum;
    }

    private static int exampleHeight(ExampleData ex) {
        return descriptionHeight(ex.description) + 4;
    }

    private static int descriptionHeight(String s) {
        if (s == null || s.isEmpty()) {
            return 0;
        }
        return s.split("\n").length;
    }

    private void render(Frame frame) {
        Rect area = frame.area();
        List<Rect> parts = Layout.vertical()
            .constraints(Constraint.length(3), Constraint.length(1), Constraint.fill())
            .split(area);

        renderTabs(frame, parts.get(0));
        renderAxis(frame, parts.get(1));
        renderDemo(frame, parts.get(2));
    }

    private void renderTabs(Frame frame, Rect area) {
        List<Line> titles = new ArrayList<>();
        for (SelectedTab tab : SelectedTab.values()) {
            titles.add(Line.from(Span.raw(" " + tab.label + " ").fg(tab.color()).bg(TW_BLACK)));
        }

        Tabs tabs = Tabs.builder()
            .titles(titles)
            .divider(" ")
            .padding("", "")
            .highlightStyle(Style.EMPTY.reversed())
            .block(
                Block.builder()
                    .title(
                        Title.from(
                                Line.from(
                                    Span.raw("Flex Layouts ").bold(),
                                    Span.raw(" Use ◄ ► to change tab, ▲ ▼  to scroll, - + to change spacing ").dim()
                                )
                            )
                    )
                    .build()
            )
            .build();

        frame.renderStatefulWidget(tabs, area, new TabsState(selectedTab.ordinal()));
    }

    private void renderAxis(Frame frame, Rect area) {
        int width = area.width();
        String label = spacing != 0
            ? width + " px (gap: " + spacing + " px)"
            : width + " px";
        int barWidth = Math.max(0, width - 2);
        String widthBar = "<" + center(label, barWidth, '-') + ">";

        Paragraph axis = Paragraph.builder()
            .text(Text.from(Line.from(Span.raw(widthBar).fg(Color.DARK_GRAY))))
            .alignment(Alignment.CENTER)
            .build();
        frame.renderWidget(axis, area);
    }

    private static String center(String text, int width, char fill) {
        if (width <= 0) {
            return "";
        }
        if (text.length() >= width) {
            return text.substring(0, width);
        }
        int padding = width - text.length();
        int left = padding / 2;
        int right = padding - left;
        return repeat(fill, left) + text + repeat(fill, right);
    }

    private static String repeat(char c, int count) {
        if (count <= 0) {
            return "";
        }
        return String.valueOf(c).repeat(count);
    }

    private void renderDemo(Frame frame, Rect area) {
        int totalHeight = examplesTotalHeight();
        Rect demoArea = new Rect(0, 0, area.width(), totalHeight);
        Buffer demoBuf = Buffer.empty(demoArea);

        int y = 0;
        for (ExampleData ex : EXAMPLE_DATA) {
            int h = exampleHeight(ex);
            Rect exArea = new Rect(0, y, demoArea.width(), h);
            renderExample(exArea, demoBuf, ex, selectedTab.flex, spacing);
            y += h;
        }

        int visibleHeight = area.height();
        int startY = Math.min(scrollOffset, Math.max(0, totalHeight - visibleHeight));
        int maxX = area.width();
        for (int dy = 0; dy < visibleHeight && (startY + dy) < totalHeight; dy++) {
            for (int x = 0; x < maxX; x++) {
                Cell cell = demoBuf.get(x, startY + dy);
                frame.buffer().set(area.x() + x, area.y() + dy, cell);
            }
        }

        if (totalHeight > visibleHeight) {
            Rect scrollArea = area;
            // Reserve the last column for the scrollbar (like ratatui's VerticalRight).
            Rect content = new Rect(scrollArea.x(), scrollArea.y(), Math.max(0, scrollArea.width() - 1), scrollArea.height());
            // Re-render content without clobbering scrollbar column.
            for (int dy = 0; dy < visibleHeight && (startY + dy) < totalHeight; dy++) {
                for (int x = 0; x < content.width(); x++) {
                    Cell cell = demoBuf.get(x, startY + dy);
                    frame.buffer().set(content.x() + x, content.y() + dy, cell);
                }
            }

            Scrollbar scrollbar = Scrollbar.builder()
                .orientation(ScrollbarOrientation.VERTICAL_RIGHT)
                .thumbStyle(Style.EMPTY.fg(Color.DARK_GRAY))
                .trackStyle(Style.EMPTY.fg(Color.DARK_GRAY))
                .build();
            ScrollbarState state = new ScrollbarState(maxScrollOffset() + 1)
                .viewportContentLength(visibleHeight)
                .position(scrollOffset);
            frame.renderStatefulWidget(scrollbar, scrollArea, state);
        }
    }

    private void renderExample(Rect area, Buffer buf, ExampleData ex, Flex flex, int spacing) {
        int titleHeight = descriptionHeight(ex.description);
        // Use Fill(0) to match ratatui exactly
        List<Rect> layout = Layout.vertical()
            .constraints(Constraint.length(titleHeight), Constraint.fill(0))
            .split(area);

        Rect title = layout.get(0);
        Rect illustrations = layout.get(1);

        if (titleHeight > 0) {
            String[] lines = ex.description.split("\n");
            for (int i = 0; i < lines.length; i++) {
                Paragraph p = Paragraph.builder()
                    .text(Text.from(Line.from(Span.raw("// " + lines[i]).italic().fg(SLATE_400))))
                    .build();
                p.render(new Rect(title.x(), title.y() + i, title.width(), 1), buf);
            }
        }

        // In ratatui: split_with_spacers() returns blocks and spacers separately.
        // TamboUI doesn't have that helper, so we compute gaps by comparing adjacent rects.
        List<Rect> blocks = Layout.horizontal()
            .constraints(ex.constraints)
            .flex(flex)
            .spacing(spacing)
            .split(illustrations);

        List<Rect> spacers = computeSpacersFromBlocks(illustrations, blocks);

        for (int i = 0; i < blocks.size(); i++) {
            String labelOverride = ex.displayOverrides != null && i < ex.displayOverrides.length ? ex.displayOverrides[i] : null;
            renderIllustration(blocks.get(i), buf, ex.constraints[i], blocks.get(i).width(), labelOverride);
        }
        for (Rect spacer : spacers) {
            renderSpacer(spacer, buf);
        }
    }

    private static List<Rect> computeSpacersFromBlocks(Rect container, List<Rect> blocks) {
        List<Rect> spacers = new ArrayList<>();
        if (blocks.isEmpty()) {
            return spacers;
        }

        Rect first = blocks.get(0);
        if (first.x() > container.x()) {
            spacers.add(new Rect(container.x(), container.y(), first.x() - container.x(), container.height()));
        }
        for (int i = 0; i < blocks.size() - 1; i++) {
            Rect a = blocks.get(i);
            Rect b = blocks.get(i + 1);
            int start = a.right();
            int end = b.x();
            if (end > start) {
                spacers.add(new Rect(start, container.y(), end - start, container.height()));
            }
        }
        Rect last = blocks.get(blocks.size() - 1);
        if (last.right() < container.right()) {
            spacers.add(new Rect(last.right(), container.y(), container.right() - last.right(), container.height()));
        }
        return spacers;
    }

    private static void renderIllustration(Rect area, Buffer buf, Constraint constraint, int width, String displayOverride) {
        Color main = colorForConstraint(constraint);
        String title = displayOverride != null ? displayOverride : constraintLabel(constraint);
        String content = width + " px";

        Style blockStyle = Style.EMPTY.fg(TW_WHITE).bg(main);
        Style borderStyle = Style.EMPTY.fg(main).reversed();

        Block block = Block.builder()
            .borders(Borders.ALL)
            .borderType(BorderType.QUADRANT_OUTSIDE)
            .borderStyle(borderStyle)
            .style(blockStyle)
            .build();
        block.render(area, buf);

        // Style the text with the same fg/bg as the block so it renders properly
        // (Buffer.setString replaces cells, so the text must carry its own style)
        Paragraph para = Paragraph.builder()
            .text(Text.from(
                Line.from(Span.styled(title, blockStyle)),
                Line.from(Span.styled(content, blockStyle))
            ))
            .alignment(Alignment.CENTER)
            .build();
        para.render(block.inner(area), buf);
    }

    private static Color colorForConstraint(Constraint constraint) {
        if (constraint instanceof Constraint.Min) {
            return BLUE_900;
        }
        if (constraint instanceof Constraint.Max) {
            return BLUE_800;
        }
        if (constraint instanceof Constraint.Length) {
            return SLATE_700;
        }
        if (constraint instanceof Constraint.Percentage) {
            return SLATE_800;
        }
        if (constraint instanceof Constraint.Ratio) {
            return SLATE_900;
        }
        if (constraint instanceof Constraint.Fill) {
            return SLATE_950;
        }
        return Color.GRAY;
    }

    private static String constraintLabel(Constraint constraint) {
        if (constraint instanceof Constraint.Min) {
            return "Min(" + ((Constraint.Min) constraint).value() + ")";
        }
        if (constraint instanceof Constraint.Max) {
            return "Max(" + ((Constraint.Max) constraint).value() + ")";
        }
        if (constraint instanceof Constraint.Length) {
            return "Length(" + ((Constraint.Length) constraint).value() + ")";
        }
        if (constraint instanceof Constraint.Percentage) {
            return "Percentage(" + ((Constraint.Percentage) constraint).value() + ")";
        }
        if (constraint instanceof Constraint.Ratio) {
            Constraint.Ratio r = (Constraint.Ratio) constraint;
            return "Ratio(" + r.numerator() + ", " + r.denominator() + ")";
        }
        if (constraint instanceof Constraint.Fill) {
            return "Fill(" + ((Constraint.Fill) constraint).weight() + ")";
        }
        return constraint.toString();
    }

    private static void renderSpacer(Rect spacer, Buffer buf) {
        Style style = Style.EMPTY.fg(Color.DARK_GRAY);
        int width = spacer.width();

        if (width > 1) {
            // Corners-only border like ratatui's custom border set.
            drawCornersOnly(spacer, buf, style);

            String label;
            if (width > 4) {
                label = width + " px";
            } else if (width > 2) {
                label = String.valueOf(width);
            } else {
                label = "";
            }

            if (!label.isEmpty()) {
                Text text = Text.from(
                    Line.from(""),
                    Line.from(""),
                    Line.from(Span.raw(label).fg(Color.DARK_GRAY))
                );
                Paragraph para = Paragraph.builder()
                    .text(text)
                    .alignment(Alignment.CENTER)
                    .style(style)
                    .build();
                para.render(spacer, buf);
            }
        } else {
            // Single column spacer: "", "│", "│", ""
            if (spacer.height() >= 2) {
                if (spacer.height() >= 2) {
                    int y1 = spacer.y() + 1;
                    if (y1 < spacer.bottom()) {
                        buf.setString(spacer.x(), y1, "│", style);
                    }
                }
                if (spacer.height() >= 3) {
                    int y2 = spacer.y() + 2;
                    if (y2 < spacer.bottom()) {
                        buf.setString(spacer.x(), y2, "│", style);
                    }
                }
            }
        }
    }

    private static void drawCornersOnly(Rect r, Buffer buf, Style style) {
        if (r.width() <= 0 || r.height() <= 0) {
            return;
        }
        // Clear entire spacer.
        buf.fill(r, Cell.EMPTY);
        if (r.width() == 1 || r.height() == 1) {
            return;
        }
        buf.set(r.left(), r.top(), new Cell("┌", style));
        buf.set(r.right() - 1, r.top(), new Cell("┐", style));
        buf.set(r.left(), r.bottom() - 1, new Cell("└", style));
        buf.set(r.right() - 1, r.bottom() - 1, new Cell("┘", style));
    }

    private static final class ExampleData {
        final String description;
        final Constraint[] constraints;
        final String[] displayOverrides;

        ExampleData(String description, Constraint[] constraints, String[] displayOverrides) {
            this.description = description;
            this.constraints = constraints;
            this.displayOverrides = displayOverrides;
        }
    }
}

