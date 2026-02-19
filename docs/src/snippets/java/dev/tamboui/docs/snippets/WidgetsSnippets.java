package dev.tamboui.docs.snippets;

import dev.tamboui.buffer.Buffer;
import dev.tamboui.layout.Alignment;
import dev.tamboui.layout.Constraint;
import dev.tamboui.layout.Padding;
import dev.tamboui.layout.Rect;
import dev.tamboui.style.Color;
import dev.tamboui.style.Overflow;
import dev.tamboui.style.Style;
import dev.tamboui.terminal.Frame;
import dev.tamboui.text.Text;
import dev.tamboui.toolkit.element.Element;
import dev.tamboui.tui.event.KeyCode;
import dev.tamboui.tui.event.KeyEvent;
import dev.tamboui.widgets.Clear;
import dev.tamboui.widgets.barchart.Bar;
import dev.tamboui.widgets.barchart.BarChart;
import dev.tamboui.widgets.barchart.BarGroup;
import dev.tamboui.widgets.block.Block;
import dev.tamboui.widgets.block.BorderType;
import dev.tamboui.widgets.block.Borders;
import dev.tamboui.widgets.calendar.CalendarEventStore;
import dev.tamboui.widgets.calendar.Monthly;
import dev.tamboui.widgets.canvas.Canvas;
import dev.tamboui.widgets.canvas.Marker;
import dev.tamboui.widgets.canvas.shapes.Circle;
import dev.tamboui.widgets.canvas.shapes.Line;
import dev.tamboui.widgets.canvas.shapes.Rectangle;
import dev.tamboui.widgets.chart.Axis;
import dev.tamboui.widgets.chart.Chart;
import dev.tamboui.widgets.chart.Dataset;
import dev.tamboui.widgets.checkbox.Checkbox;
import dev.tamboui.widgets.checkbox.CheckboxState;
import dev.tamboui.widgets.error.ErrorDisplay;
import dev.tamboui.widgets.gauge.Gauge;
import dev.tamboui.widgets.gauge.LineGauge;
import dev.tamboui.widgets.input.TextArea;
import dev.tamboui.widgets.input.TextAreaState;
import dev.tamboui.widgets.input.TextInput;
import dev.tamboui.widgets.input.TextInputState;
import dev.tamboui.widgets.list.ListItem;
import dev.tamboui.widgets.list.ListState;
import dev.tamboui.widgets.list.ListWidget;
import dev.tamboui.widgets.logo.Logo;
import dev.tamboui.widgets.paragraph.Paragraph;
import dev.tamboui.widgets.scrollbar.Scrollbar;
import dev.tamboui.widgets.scrollbar.ScrollbarOrientation;
import dev.tamboui.widgets.scrollbar.ScrollbarState;
import dev.tamboui.widgets.select.Select;
import dev.tamboui.widgets.select.SelectState;
import dev.tamboui.widgets.sparkline.Sparkline;
import dev.tamboui.widgets.spinner.Spinner;
import dev.tamboui.widgets.spinner.SpinnerFrameSet;
import dev.tamboui.widgets.spinner.SpinnerState;
import dev.tamboui.widgets.spinner.SpinnerStyle;
import dev.tamboui.widgets.table.Row;
import dev.tamboui.widgets.table.Table;
import dev.tamboui.widgets.table.TableState;
import dev.tamboui.widgets.tabs.Tabs;
import dev.tamboui.widgets.tabs.TabsState;
import dev.tamboui.widgets.toggle.Toggle;
import dev.tamboui.widgets.toggle.ToggleState;
import dev.tamboui.widgets.tree.TreeNode;
import dev.tamboui.widgets.wavetext.WaveText;
import dev.tamboui.widgets.wavetext.WaveTextState;

import java.time.LocalDate;
import java.util.List;

import static dev.tamboui.toolkit.Toolkit.*;

/**
 * Code snippets for widgets.adoc documentation.
 * Each method contains tagged regions that are included in the documentation.
 */
@SuppressWarnings({"unused", "UnnecessaryLocalVariable"})
public class WidgetsSnippets {

    // Stub types and methods for application-specific code
    enum FileType { DIRECTORY, JAVA, OTHER }

    record FileInfo(String name, FileType type, long size) {
        public FileInfo(String name, FileType type) {
            this(name, type, 0);
        }
        String icon() { return type == FileType.DIRECTORY ? "folder" : "file"; }
        String formattedSize() { return size > 0 ? size + " bytes" : ""; }
        Color statusColor() { return Color.WHITE; }
    }

    List<TreeNode<FileInfo>> loadChildrenFromDisk(String path) {
        return List.of(TreeNode.of("stub", new FileInfo("stub", FileType.OTHER)));
    }

    // Stub fields for rendering
    Rect area;
    Buffer buffer;
    Frame frame;
    KeyEvent event;
    int scrollPosition;
    int scrollOffset;
    int visibleHeight;

    void blockWidget() {
        // tag::block[]
        Block block = Block.builder()
            .title("My Title")
            .borders(Borders.ALL)
            .borderType(BorderType.ROUNDED)
            .borderStyle(Style.EMPTY.fg(Color.CYAN))
            .padding(new Padding(1, 2, 1, 2))  // top, right, bottom, left
            .build();

        // Render the block
        block.render(area, buffer);

        // Get the inner area (after borders and padding)
        Rect inner = block.inner(area);
        // end::block[]
    }

    void clearWidget() {
        Rect dialogArea = area;
        // tag::clear[]
        Clear.INSTANCE.render(dialogArea, buffer);
        // Then render your dialog on top
        // end::clear[]
    }

    void paragraphWidget() {
        // tag::paragraph[]
        Paragraph paragraph = Paragraph.builder()
            .text(Text.from("This is a paragraph of text that can wrap across multiple lines."))
            .overflow(Overflow.WRAP_WORD)
            .alignment(Alignment.LEFT)
            .block(Block.builder()
                .title("Description")
                .borders(Borders.ALL)
                .build())
            .build();

        paragraph.render(area, buffer);
        // end::paragraph[]
    }

    void listWidget() {
        // tag::list-widget[]
        // Create state
        ListState state = new ListState();
        state.select(0);  // Select first item

        // Build and render
        ListWidget list = ListWidget.builder()
            .items(
                ListItem.from("First item"),
                ListItem.from("Second item"),
                ListItem.from("Styled item").style(Style.EMPTY.bold())
            )
            .highlightStyle(Style.EMPTY.reversed())
            .highlightSymbol(">> ")
            .block(Block.builder()
                .title("Select an Item")
                .borders(Borders.ALL)
                .build())
            .build();

        list.render(area, buffer, state);
        // end::list-widget[]
    }

    void listElement() {
        // tag::list-element[]
        // Toolkit DSL - ListElement with rich content
        list()
            .add(row(text("*").yellow(), text(" Featured Item")))
            .add(text("Plain Item"))
            .add(panel("Nested Panel", text("Content")).rounded())
            .highlightColor(Color.CYAN)
            .autoScroll();
        // end::list-element[]
    }

    void tableWidget() {
        // tag::table[]
        // Create table
        Table table = Table.builder()
            .header(Row.from("Name", "Age", "City"))
            .rows(
                Row.from("Alice", "30", "NYC"),
                Row.from("Bob", "25", "LA"),
                Row.from("Charlie", "35", "Chicago")
            )
            .widths(
                Constraint.percentage(40),
                Constraint.length(10),
                Constraint.fill()
            )
            .highlightStyle(Style.EMPTY.bg(Color.DARK_GRAY))
            .block(Block.builder()
                .title("Users")
                .borders(Borders.ALL)
                .build())
            .build();

        // Create and use state
        TableState state = new TableState();
        state.select(0);
        table.render(area, buffer, state);
        // end::table[]
    }

    void tabsWidget() {
        // tag::tabs[]
        Tabs tabs = Tabs.builder()
            .titles("Home", "Settings", "About")
            .highlightStyle(Style.EMPTY.bold().fg(Color.CYAN))
            .divider(" | ")
            .block(Block.builder()
                .borders(Borders.BOTTOM_ONLY)
                .build())
            .build();

        TabsState state = new TabsState();
        state.select(0);
        tabs.render(area, buffer, state);
        // end::tabs[]
    }

    void treeWidget() {
        TreeNode<FileInfo> docs = TreeNode.of("docs", new FileInfo("docs", FileType.DIRECTORY));
        TreeNode<FileInfo> build = TreeNode.of("build", new FileInfo("build", FileType.DIRECTORY));
        // tag::tree[]
        // Create tree nodes with data model
        TreeNode<FileInfo> src = TreeNode.of("src", new FileInfo("src", FileType.DIRECTORY))
            .add(TreeNode.of("main", new FileInfo("main", FileType.DIRECTORY))
                .add(TreeNode.of("App.java", new FileInfo("App.java", FileType.JAVA, 2048)).leaf())
                .expanded())
            .expanded();

        // Create tree with custom node renderer
        tree(src, docs, build)
            .title("Project Files")
            .rounded()
            .highlightColor(Color.CYAN)
            .scrollbar()
            .nodeRenderer(node -> row(
                text(node.data().icon() + " "),
                text(node.label()).bold(),
                spacer(),
                text(node.data().formattedSize()).dim()
            ));
        // end::tree[]
    }

    // Stub for tree model example
    enum Status { OK, ERROR }
    FileInfo fileInfo = new FileInfo("stub", FileType.OTHER);

    void treeModelView() {
        // tag::tree-model[]
        // Pure data model - no view concerns
        record FileData(String name, FileType type, long size, Status status) {
            String icon() { /* data-derived */ return ""; }
            String formattedSize() { /* data formatting */ return ""; }
        }

        FileData data = new FileData("App.java", FileType.JAVA, 2048, Status.OK);
        // TreeNode holds the data
        TreeNode<FileData> node = TreeNode.of("App.java", data).leaf();
        // end::tree-model[]
    }

    void treeViewRenderer() {
        TreeNode<FileInfo> node = TreeNode.of("App.java", fileInfo).leaf();
        // tag::tree-view[]
        // Rendering logic separated from model
        tree(node).nodeRenderer(n -> {
            var info = n.data();
            return row(
                text(info.icon() + " "),
                text(info.name()).fg(info.statusColor()),
                spacer(),
                text(info.formattedSize()).dim()
            );
        });
        // end::tree-view[]
    }

    void treeLazyLoading() {
        FileInfo dirInfo = new FileInfo("dir", FileType.DIRECTORY);
        // tag::tree-lazy[]
        TreeNode.of("Large Directory", dirInfo)
            .childrenLoader(() -> loadChildrenFromDisk(dirInfo.name()));
        // end::tree-lazy[]
    }

    void gaugeWidget() {
        // tag::gauge[]
        Gauge gauge = Gauge.builder()
            .ratio(0.75)  // 75% complete
            .label("Loading...")
            .gaugeStyle(Style.EMPTY.fg(Color.GREEN))
            .block(Block.builder()
                .title("Progress")
                .borders(Borders.ALL)
                .build())
            .build();

        gauge.render(area, buffer);
        // end::gauge[]
    }

    void lineGaugeWidget() {
        // tag::line-gauge[]
        LineGauge lineGauge = LineGauge.builder()
            .ratio(0.5)
            .label("50%")
            .lineSet(LineGauge.THICK)
            .filledStyle(Style.EMPTY.fg(Color.CYAN))
            .unfilledStyle(Style.EMPTY.fg(Color.DARK_GRAY))
            .build();

        lineGauge.render(area, buffer);
        // end::line-gauge[]
    }

    void sparklineWidget() {
        // tag::sparkline[]
        int[] data = {1, 4, 2, 8, 5, 7, 3, 6};

        Sparkline sparkline = Sparkline.builder()
            .data(data)
            .foreground(Color.CYAN)
            .block(Block.builder()
                .title("CPU Usage")
                .borders(Borders.ALL)
                .build())
            .build();

        sparkline.render(area, buffer);
        // end::sparkline[]
    }

    void barChartWidget() {
        // tag::bar-chart[]
        BarChart barChart = BarChart.builder()
            .data(BarGroup.of(
                Bar.of(80, "Mon"),
                Bar.of(95, "Tue"),
                Bar.of(60, "Wed"),
                Bar.of(75, "Thu"),
                Bar.of(90, "Fri")
            ))
            .barWidth(5)
            .barGap(1)
            .barColor(Color.GREEN)
            .labelColor(Color.DARK_GRAY)
            .block(Block.builder()
                .title("Weekly Sales")
                .borders(Borders.ALL)
                .build())
            .build();

        barChart.render(area, buffer);
        // end::bar-chart[]
    }

    void chartWidget() {
        // tag::chart[]
        // Create datasets
        Dataset dataset1 = Dataset.builder()
            .name("Series A")
            .data(new double[][] {
                {0, 0},
                {1, 2},
                {2, 1},
                {3, 4},
                {4, 3}
            })
            .marker(Dataset.Marker.BRAILLE)
            .style(Style.EMPTY.fg(Color.CYAN))
            .build();

        // Build chart
        Chart chart = Chart.builder()
            .datasets(dataset1)
            .xAxis(Axis.builder()
                .title("X Axis")
                .bounds(0, 5)
                .build())
            .yAxis(Axis.builder()
                .title("Y Axis")
                .bounds(0, 5)
                .build())
            .block(Block.builder()
                .title("Line Chart")
                .borders(Borders.ALL)
                .build())
            .build();

        chart.render(area, buffer);
        // end::chart[]
    }

    void canvasWidget() {
        // tag::canvas[]
        Canvas canvas = Canvas.builder()
            .xBounds(-10, 10)
            .yBounds(-10, 10)
            .marker(Marker.BRAILLE)
            .paint(ctx -> {
                // Draw shapes
                ctx.draw(new Circle(0, 0, 5, Color.RED));
                ctx.draw(new Line(-5, -5, 5, 5, Color.GREEN));
                ctx.draw(new Rectangle(-3, -3, 6, 6, Color.BLUE));
            })
            .block(Block.builder()
                .title("Drawing")
                .borders(Borders.ALL)
                .build())
            .build();

        canvas.render(area, buffer);
        // end::canvas[]
    }

    void calendarWidget() {
        // tag::calendar[]
        CalendarEventStore events = CalendarEventStore.today(Style.EMPTY.bold().fg(Color.CYAN));

        Monthly calendar = Monthly.of(LocalDate.now(), events)
            .showMonthHeader(Style.EMPTY.bold())
            .showWeekdaysHeader(Style.EMPTY.fg(Color.CYAN))
            .showSurrounding(Style.EMPTY.dim())
            .block(Block.bordered());

        calendar.render(area, buffer);
        // end::calendar[]
    }

    void spinnerWidget() {
        // tag::spinner[]
        // Create spinner with built-in style
        Spinner spinner = Spinner.builder()
            .spinnerStyle(SpinnerStyle.DOTS)
            .style(Style.EMPTY.fg(Color.CYAN))
            .build();

        // Or with custom frames
        Spinner spinnerCustom = Spinner.builder()
            .frames("*", "+", "x", "+")
            .build();

        // Or with a custom frame set
        Spinner spinnerFrameSet = Spinner.builder()
            .frameSet(SpinnerFrameSet.of("a", "b", "c", "d"))
            .build();

        // Create state and render
        SpinnerState state = new SpinnerState();
        spinner.render(area, buffer, state);

        // Advance state on each tick
        state.advance();
        // end::spinner[]
    }

    void waveTextWidget() {
        // tag::wave-text[]
        // Dark shadow moving through bright text (default)
        WaveText wave = WaveText.builder()
            .text("Processing...")
            .color(Color.CYAN)
            .build();

        // Bright peak moving through dim text (inverted)
        WaveText waveInverted = WaveText.builder()
            .text("Loading...")
            .color(Color.YELLOW)
            .inverted(true)
            .build();

        // Back-and-forth oscillation instead of looping
        WaveText waveOscillate = WaveText.builder()
            .text("Thinking...")
            .mode(WaveText.Mode.OSCILLATE)
            .build();

        // Multiple peaks for dynamic effect
        WaveText waveMulti = WaveText.builder()
            .text("Working hard...")
            .color(Color.GREEN)
            .peakCount(2)
            .peakWidth(5)
            .speed(1.5)
            .build();

        // Create state and render
        WaveTextState state = new WaveTextState();
        wave.render(area, buffer, state);

        // Advance animation each frame
        state.advance();
        // end::wave-text[]
    }

    void textInputWidget() {
        // tag::text-input[]
        // Create state
        TextInputState state = new TextInputState();

        // Or with initial value
        TextInputState stateWithValue = new TextInputState("initial text");

        // Build widget
        TextInput textInput = TextInput.builder()
            .placeholder("Enter your name...")
            .cursorStyle(Style.EMPTY.reversed())
            .block(Block.builder()
                .title("Name")
                .borders(Borders.ALL)
                .build())
            .build();

        textInput.render(area, buffer, state);

        // Handle key events
        if (event.code() == KeyCode.CHAR) {
            state.insert(event.character());
        } else if (event.code() == KeyCode.BACKSPACE) {
            state.deleteBackward();
        } else if (event.code() == KeyCode.DELETE) {
            state.deleteForward();
        } else if (event.code() == KeyCode.LEFT) {
            state.moveCursorLeft();
        } else if (event.code() == KeyCode.RIGHT) {
            state.moveCursorRight();
        }
        // end::text-input[]
    }

    void textAreaWidget() {
        // tag::text-area[]
        // Create state
        TextAreaState state = new TextAreaState();

        // Or with initial content
        TextAreaState stateWithContent = new TextAreaState("Line 1\nLine 2\nLine 3");

        // Build widget
        TextArea textArea = TextArea.builder()
            .placeholder("Enter text...")
            .showLineNumbers(true)
            .cursorStyle(Style.EMPTY.reversed())
            .lineNumberStyle(Style.EMPTY.dim())
            .block(Block.builder()
                .title("Editor")
                .borders(Borders.ALL)
                .build())
            .build();

        // Render with cursor visible
        textArea.renderWithCursor(area, buffer, state, frame);

        // Handle key events
        if (event.code() == KeyCode.CHAR) {
            state.insert(event.character());
        } else if (event.code() == KeyCode.ENTER) {
            state.insert('\n');
        } else if (event.code() == KeyCode.BACKSPACE) {
            state.deleteBackward();
        } else if (event.code() == KeyCode.UP) {
            state.moveCursorUp();
        } else if (event.code() == KeyCode.DOWN) {
            state.moveCursorDown();
        }
        // end::text-area[]
    }

    void checkboxWidget() {
        // tag::checkbox[]
        // Create state
        CheckboxState state = new CheckboxState();       // unchecked
        CheckboxState stateChecked = new CheckboxState(true);   // checked

        // Build widget with default style [x] / [ ]
        Checkbox checkbox = Checkbox.builder().build();

        // Custom symbols
        Checkbox checkboxCustom = Checkbox.builder()
            .checkedSymbol("[v]")
            .uncheckedSymbol("[ ]")
            .checkedColor(Color.GREEN)
            .uncheckedColor(Color.DARK_GRAY)
            .build();

        checkbox.render(area, buffer, state);

        // Handle input
        if (event.code() == KeyCode.ENTER || (event.code() == KeyCode.CHAR && event.character() == ' ')) {
            state.toggle();
        }
        // end::checkbox[]
    }

    void toggleWidget() {
        // tag::toggle[]
        // Create state
        ToggleState state = new ToggleState();       // off
        ToggleState stateOn = new ToggleState(true);   // on

        // Single symbol mode (default): [ON ] or [OFF]
        Toggle toggle = Toggle.builder()
            .onSymbol("o ON ")
            .offSymbol("o OFF")
            .onColor(Color.GREEN)
            .build();

        // Inline choice mode: o Yes / o No
        Toggle toggleInline = Toggle.builder()
            .inlineChoice(true)
            .onLabel("Yes")
            .offLabel("No")
            .selectedIndicator("o")
            .unselectedIndicator("o")
            .selectedColor(Color.GREEN)
            .unselectedColor(Color.DARK_GRAY)
            .build();

        toggle.render(area, buffer, state);

        // Handle input
        if (event.code() == KeyCode.ENTER || (event.code() == KeyCode.CHAR && event.character() == ' ')) {
            state.toggle();
        }
        // end::toggle[]
    }

    void selectWidget() {
        // tag::select[]
        // Create state with options
        SelectState state = new SelectState("Option A", "Option B", "Option C");

        // Build widget: < Option A >
        Select select = Select.builder()
            .leftIndicator("< ")
            .rightIndicator(" >")
            .selectedColor(Color.CYAN)
            .indicatorColor(Color.DARK_GRAY)
            .build();

        select.render(area, buffer, state);

        // Handle navigation
        if (event.code() == KeyCode.LEFT) {
            state.selectPrevious();  // wraps around
        } else if (event.code() == KeyCode.RIGHT) {
            state.selectNext();      // wraps around
        }
        // end::select[]
    }

    void scrollbarWidget() {
        // tag::scrollbar[]
        Scrollbar scrollbar = Scrollbar.builder()
            .orientation(ScrollbarOrientation.VERTICAL_RIGHT)
            .thumbStyle(Style.EMPTY.fg(Color.CYAN))
            .trackStyle(Style.EMPTY.fg(Color.DARK_GRAY))
            .build();

        ScrollbarState state = new ScrollbarState();
        state.contentLength(100);
        state.viewportContentLength(20);
        state.position(scrollPosition);

        scrollbar.render(area, buffer, state);
        // end::scrollbar[]
    }

    void logoWidget() {
        // tag::logo[]
        // Default tiny logo (2 lines)
        Logo logo = Logo.tiny();
        logo.render(area, buffer);

        // Or the normal size (4 lines)
        Logo logoNormal = Logo.of(Logo.Size.NORMAL);
        logoNormal.render(area, buffer);
        // end::logo[]
    }

    void errorDisplayWidget() {
        Exception exception = new RuntimeException("Example error");
        // tag::error-display[]
        // Quick creation from exception
        ErrorDisplay display = ErrorDisplay.from(exception);

        // Or with customization
        ErrorDisplay displayCustom = ErrorDisplay.builder()
            .error(exception)
            .title(" CRASH ")
            .footer(" Press 'q' to quit, arrows to scroll ")
            .borderColor(Color.RED)
            .scroll(scrollOffset)
            .build();

        display.render(area, buffer);

        // Handle scrolling
        int totalLines = display.lineCount();
        if (event.code() == KeyCode.DOWN) {
            scrollOffset = Math.min(scrollOffset + 1, totalLines - visibleHeight);
        } else if (event.code() == KeyCode.UP) {
            scrollOffset = Math.max(0, scrollOffset - 1);
        }
        // end::error-display[]
    }

    void clearWithStyle() {
        // tag::clear-style[]
        // Clear is a singleton widget with no style support
        Clear.INSTANCE.render(area, buffer);
        // end::clear-style[]
    }

    void toolkitDsl() {
        TableState tableState = new TableState();
        TextInputState inputState = new TextInputState();
        Element left = text("left");
        Element right = text("right");
        Element top = text("top");
        Element middle = text("middle");
        Element bottom = text("bottom");
        // tag::toolkit-dsl[]
        // Text
        text("Hello").bold().cyan();

        // Panel (Block)
        panel("Title", text("child")).rounded().borderColor(Color.CYAN);

        // List (manages its own state)
        list("Item 1", "Item 2", "Item 3").highlightColor(Color.YELLOW).autoScroll();

        // Table
        table()
            .header("Name", "Age")
            .row("Alice", "30")
            .state(tableState);

        // Gauge
        gauge(0.75).label("Loading...").gaugeColor(Color.GREEN);

        // Sparkline
        sparkline(1, 4, 2, 8, 5).color(Color.CYAN);

        // Spinner (manages its own state, advances each render)
        spinner().cyan();
        spinner(SpinnerStyle.LINE, "Loading...").green();

        // Tree (expand/collapse, keyboard navigation, lazy loading)
        tree(
            TreeNode.of("Root",
                TreeNode.of("Child 1"),
                TreeNode.of("Child 2"))
        ).highlightColor(Color.CYAN).rounded().scrollbar();

        // TextInput
        textInput(inputState).placeholder("Enter name...");

        // Layout
        row(left, right).spacing(1);
        column(top, middle, bottom).spacing(1);
        // end::toolkit-dsl[]
    }
}
