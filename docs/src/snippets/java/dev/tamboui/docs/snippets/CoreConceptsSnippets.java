package dev.tamboui.docs.snippets;

import dev.tamboui.buffer.Buffer;
import dev.tamboui.buffer.Cell;
import dev.tamboui.layout.Alignment;
import dev.tamboui.layout.Constraint;
import dev.tamboui.layout.Direction;
import dev.tamboui.layout.Flex;
import dev.tamboui.layout.Margin;
import dev.tamboui.layout.Position;
import dev.tamboui.layout.Rect;
import dev.tamboui.style.Color;
import dev.tamboui.style.Style;
import dev.tamboui.terminal.Backend;
import dev.tamboui.terminal.Frame;
import dev.tamboui.terminal.Terminal;
import dev.tamboui.layout.Layout;
import dev.tamboui.text.Line;
import dev.tamboui.text.Span;
import dev.tamboui.text.Text;
import dev.tamboui.tui.TuiConfig;
import dev.tamboui.tui.event.Event;
import dev.tamboui.tui.event.KeyCode;
import dev.tamboui.tui.event.KeyEvent;
import dev.tamboui.tui.event.KeyModifiers;
import dev.tamboui.tui.event.MouseButton;
import dev.tamboui.tui.event.MouseEvent;
import dev.tamboui.tui.event.MouseEventKind;
import dev.tamboui.tui.event.TickEvent;
import dev.tamboui.widget.StatefulWidget;
import dev.tamboui.widget.Widget;
import dev.tamboui.widgets.list.ListState;
import dev.tamboui.widgets.list.ListWidget;
import dev.tamboui.widgets.paragraph.Paragraph;

import java.time.Duration;
import java.util.List;

/**
 * Code snippets for core-concepts.adoc documentation.
 * Each method contains tagged regions that are included in the documentation.
 */
@SuppressWarnings({"unused", "UnnecessaryLocalVariable"})
public class CoreConceptsSnippets {

    // Stub types for documentation examples
    interface MyWidget extends Widget {}

    final Widget myWidget = null;
    final Widget headerWidget = null;

    // Stub methods for examples
    void handleClick(int x, int y) {}
    void updateAnimation() {}

    void bufferBasics() {
        int width = 80;
        int height = 24;
        int x = 0;
        int y = 0;
        // tag::buffer-basics[]
        // A Buffer is created for each frame
        Buffer buffer = Buffer.empty(new Rect(0, 0, width, height));

        // Set a cell at position (x, y)
        buffer.set(x, y, new Cell("A", Style.EMPTY.fg(Color.RED)));

        // Get a cell
        Cell cell = buffer.get(x, y);
        // end::buffer-basics[]
    }

    void cellBasics() {
        // tag::cell-basics[]
        // Create a cell with character and style
        Cell cell = new Cell("X", Style.EMPTY.bold().fg(Color.CYAN));

        // Get properties
        String symbol = cell.symbol();
        Style style = cell.style();
        // end::cell-basics[]
    }

    void frameBasics(Terminal<? extends Backend> terminal) {
        // tag::frame-basics[]
        terminal.draw(frame -> {
            // Get the renderable area
            Rect area = frame.area();

            // Render a widget to the frame
            frame.renderWidget(myWidget, area);

            // Or render to a sub-area
            Rect subArea = new Rect(0, 0, 40, 10);
            frame.renderWidget(headerWidget, subArea);
        });
        // end::frame-basics[]
    }

    void rectBasics() {
        int x = 0;
        int y = 0;
        int width = 80;
        int height = 24;
        // tag::rect-basics[]
        // Create a rectangle
        Rect rect = new Rect(x, y, width, height);

        // Properties
        int rx = rect.x();
        int ry = rect.y();
        int rw = rect.width();
        int rh = rect.height();

        // Derived values
        int right = rect.right();    // x + width
        int bottom = rect.bottom();  // y + height
        // end::rect-basics[]
    }

    void layoutBasics() {
        // tag::layout-basics[]
        Layout layout = Layout.vertical()
            .constraints(
                Constraint.length(3),   // First area: 3 cells tall
                Constraint.fill()       // Second area: takes remaining space
            );

        // Split a 80x24 area into two rectangles
        Rect area = new Rect(0, 0, 80, 24);
        List<Rect> areas = layout.split(area);

        // Result:
        // areas.get(0) = Rect(0, 0, 80, 3)   - header area
        // areas.get(1) = Rect(0, 3, 80, 21)  - content area
        // end::layout-basics[]
    }

    void layoutComplete(Frame frame) {
        // tag::layout-complete[]
        // Split vertically into header (3 lines) and content (rest)
        Layout layout = Layout.vertical()
            .constraints(
                Constraint.length(3),
                Constraint.fill()
            );

        List<Rect> areas = layout.split(frame.area());
        Rect headerArea = areas.get(0);
        Rect contentArea = areas.get(1);

        // Render widgets into the split areas
        Paragraph header = Paragraph.from("Header Text");
        header.render(headerArea, frame.buffer());

        Paragraph content = Paragraph.from("Main content goes here...");
        content.render(contentArea, frame.buffer());
        // end::layout-complete[]
    }

    void layoutNested(Frame frame, Rect contentArea) {
        // tag::layout-nested[]
        // Split horizontally with percentages
        Layout horizontalLayout = Layout.horizontal()
            .constraints(
                Constraint.percentage(30),
                Constraint.percentage(70)
            );

        List<Rect> columns = horizontalLayout.split(contentArea);

        // Render sidebar and main area
        ListWidget sidebar = ListWidget.builder()
            .items("Item 1", "Item 2", "Item 3")
            .build();
        sidebar.render(columns.get(0), frame.buffer(), new ListState());

        Paragraph mainArea = Paragraph.from("Main content");
        mainArea.render(columns.get(1), frame.buffer());
        // end::layout-nested[]
    }

    // Stub method for flex example
    void renderButton(String label, Rect area, Buffer buffer) {}
    void renderMenuItem(String label, Rect area, Buffer buffer) {}

    void layoutFlex(Frame frame, Rect toolbarArea, Rect navbarArea) {
        // tag::layout-flex[]
        // Center three fixed-width buttons in a toolbar
        Layout toolbar = Layout.horizontal()
            .constraints(
                Constraint.length(10),  // Button 1
                Constraint.length(10),  // Button 2
                Constraint.length(10)   // Button 3
            )
            .flex(Flex.CENTER);  // Center the buttons, space on both sides

        List<Rect> buttonAreas = toolbar.split(toolbarArea);
        renderButton("Save", buttonAreas.get(0), frame.buffer());
        renderButton("Cancel", buttonAreas.get(1), frame.buffer());
        renderButton("Help", buttonAreas.get(2), frame.buffer());

        // Spread menu items across a navigation bar
        Layout navbar = Layout.horizontal()
            .constraints(
                Constraint.length(8),   // "File"
                Constraint.length(8)    // "Edit"
            )
            .flex(Flex.SPACE_BETWEEN);  // Push to edges with gap between

        List<Rect> menuAreas = navbar.split(navbarArea);
        renderMenuItem("File", menuAreas.get(0), frame.buffer());
        renderMenuItem("Edit", menuAreas.get(1), frame.buffer());
        // end::layout-flex[]
    }

    void layoutMargin() {
        // tag::layout-margin[]
        Layout.vertical()
            .margin(new Margin(1, 2, 1, 2))  // top, right, bottom, left
            .constraints(Constraint.fill());
        // end::layout-margin[]
    }

    void styleBasics() {
        // tag::style-basics[]
        // Create a style
        Style style = Style.EMPTY
            .fg(Color.CYAN)
            .bg(Color.BLACK)
            .bold()
            .underlined();

        // Styles are immutable - methods return new instances
        Style dimStyle = style.dim();
        // end::style-basics[]
    }

    void colorBasics() {
        // tag::color-basics[]
        // Named ANSI colors
        Color red = Color.RED;
        Color green = Color.GREEN;
        Color cyan = Color.CYAN;
        Color white = Color.WHITE;
        Color gray = Color.GRAY;

        // Indexed colors (0-255)
        Color indexed = Color.indexed(196);

        // RGB colors (true color)
        Color rgb = Color.rgb(255, 128, 0);
        // end::color-basics[]
    }

    void modifierBasics() {
        // tag::modifier-basics[]
        Style.EMPTY
            .bold()       // Bold text
            .dim()        // Dimmed/faint
            .italic()     // Italic (terminal support varies)
            .underlined() // Underlined
            .slowBlink()  // Slow blinking
            .rapidBlink() // Rapid blinking
            .reversed()   // Swap fg/bg colors
            .hidden()     // Hidden text
            .crossedOut(); // Strikethrough
        // end::modifier-basics[]
    }

    void textBasics() {
        // tag::text-basics[]
        // Simple text
        Text text = Text.from("Hello, World!");

        // Multi-line text
        Text multiLine = Text.from(
            Line.from("First line"),
            Line.from("Second line")
        );

        // With alignment
        Text centered = Text.from("Centered").alignment(Alignment.CENTER);
        // end::text-basics[]
    }

    void lineBasics() {
        // tag::line-basics[]
        Line line = Line.from(
            Span.styled("Bold", Style.EMPTY.bold()),
            Span.raw(" and "),
            Span.styled("Red", Style.EMPTY.fg(Color.RED))
        );
        // end::line-basics[]
    }

    void spanBasics() {
        // tag::span-basics[]
        // Unstyled span
        Span plain = Span.raw("plain text");

        // Styled span
        Span styled = Span.styled("styled", Style.EMPTY.fg(Color.CYAN).bold());
        // end::span-basics[]
    }

    // tag::widget-interface[]
    public interface WidgetInterface {
        void render(Rect area, Buffer buffer);
    }
    // end::widget-interface[]

    // tag::stateful-widget-interface[]
    public interface StatefulWidgetInterface<S> {
        void render(Rect area, Buffer buffer, S state);
    }
    // end::stateful-widget-interface[]

    void statefulWidgetUsage(StatefulWidget<ListState> listWidget, Rect area, Buffer buffer) {
        List<String> items = List.of("Item 1", "Item 2", "Item 3");
        // tag::stateful-widget-usage[]
        // Create state
        ListState listState = new ListState();

        // Render with state
        listWidget.render(area, buffer, listState);

        // Modify state based on user input
        listState.selectNext(items.size());
        // end::stateful-widget-usage[]
    }

    void keyEventBasics(KeyEvent event) {
        // tag::key-event-basics[]
        // Check specific keys
        if (event.code() == KeyCode.ENTER) { /* handle enter */ }
        if (event.code() == KeyCode.CHAR && event.character() == 'q') { /* handle q */ }

        // Check modifiers
        if (event.modifiers().ctrl()) { /* Ctrl is held */ }

        // Using semantic actions
        if (event.isQuit()) { /* quit action */ }
        if (event.isUp()) { /* move up action */ }
        // end::key-event-basics[]
    }

    void mouseEventBasics(MouseEvent event) {
        // tag::mouse-event-basics[]
        int x = event.x();
        int y = event.y();
        MouseEventKind kind = event.kind();

        if (kind == MouseEventKind.PRESS && event.button() == MouseButton.LEFT) {
            handleClick(x, y);
        }
        // end::mouse-event-basics[]
    }

    void tickEventBasics(Event event) {
        // tag::tick-event-basics[]
        // Configure tick rate
        TuiConfig config = TuiConfig.builder()
            .tickRate(Duration.ofMillis(16))  // ~60fps
            .build();

        // Handle tick
        if (event instanceof TickEvent) {
            updateAnimation();
            // return true to trigger redraw
        }
        // end::tick-event-basics[]
    }
}
