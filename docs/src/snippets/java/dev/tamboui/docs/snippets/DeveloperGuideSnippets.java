package dev.tamboui.docs.snippets;

import dev.tamboui.buffer.Buffer;
import dev.tamboui.buffer.Cell;
import dev.tamboui.export.Formats;
import dev.tamboui.layout.Rect;
import dev.tamboui.style.Color;
import dev.tamboui.style.ColorConverter;
import dev.tamboui.style.PropertyDefinition;
import dev.tamboui.style.PropertyRegistry;
import dev.tamboui.style.StandardProperties;
import dev.tamboui.style.Style;
import dev.tamboui.style.StylePropertyResolver;
import dev.tamboui.text.CharWidth;
import dev.tamboui.toolkit.app.ToolkitRunner;
import dev.tamboui.toolkit.component.Component;
import dev.tamboui.toolkit.element.Element;
import dev.tamboui.annotations.bindings.OnAction;
import dev.tamboui.tui.bindings.BindingSets;
import dev.tamboui.tui.bindings.KeyTrigger;
import dev.tamboui.tui.event.Event;
import dev.tamboui.widget.StatefulWidget;
import dev.tamboui.widget.Widget;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;

import static dev.tamboui.export.ExportRequest.export;
import static dev.tamboui.toolkit.Toolkit.*;

/**
 * Code snippets for developer-guide.adoc documentation.
 * Each method contains tagged regions that are included in the documentation.
 */
@SuppressWarnings("unused")
public class DeveloperGuideSnippets {

    // Stub types for examples
    interface MyPanel {
        Rect renderedArea();
    }

    MyPanel myPanel;
    Buffer buffer;
    OutputStream outputStream;
    Rect area;
    Style style;
    Style bgStyle;
    int x;
    int y;

    // tag::widget-interface[]
    public interface WidgetInterface {
        void render(Rect area, Buffer buffer);
    }
    // end::widget-interface[]

    // tag::separator-widget[]
    public class Separator implements Widget {
        private final Style style;

        public Separator(Style style) {
            this.style = style;
        }

        @Override
        public void render(Rect area, Buffer buffer) {
            // Fill the entire width with a horizontal line
            for (int x = area.x(); x < area.right(); x++) {
                buffer.set(x, area.y(), new Cell("─", style));
            }
        }
    }
    // end::separator-widget[]

    // tag::stateful-widget-interface[]
    public interface StatefulWidgetInterface<S> {
        void render(Rect area, Buffer buffer, S state);
    }
    // end::stateful-widget-interface[]

    // tag::counter-widget[]
    public class Counter implements StatefulWidget<Counter.State> {
        private final Style style;

        public static class State {
            private int value = 0;

            public int value() { return value; }
            public void increment() { value++; }
            public void decrement() { if (value > 0) value--; }
        }

        public Counter(Style style) {
            this.style = style;
        }

        @Override
        public void render(Rect area, Buffer buffer, State state) {
            String text = "Count: " + state.value();
            int x = area.x();
            for (char c : text.toCharArray()) {
                if (x < area.right()) {
                    buffer.set(x++, area.y(), new Cell(String.valueOf(c), style));
                }
            }
        }
    }
    // end::counter-widget[]

    void counterUsage() {
        // tag::counter-usage[]
        Counter.State counterState = new Counter.State();
        Counter counter = new Counter(Style.EMPTY.bold());
        counter.render(area, buffer, counterState);
        // end::counter-usage[]
    }

    void settingCells() {
        // tag::setting-cells[]
        // Set a single cell
        buffer.set(x, y, new Cell("X", style));

        // Set a string
        int col = x;
        for (char c : "Hello".toCharArray()) {
            buffer.set(col++, y, new Cell(String.valueOf(c), style));
        }
        // end::setting-cells[]
    }

    void readingCells() {
        // tag::reading-cells[]
        Cell cell = buffer.get(x, y);
        String symbol = cell.symbol();
        Style cellStyle = cell.style();
        // end::reading-cells[]
    }

    void fillingAreas() {
        // tag::filling-areas[]
        // Fill a rectangle with a character
        for (int row = area.y(); row < area.bottom(); row++) {
            for (int col = area.x(); col < area.right(); col++) {
                buffer.set(col, row, new Cell(" ", bgStyle));
            }
        }
        // end::filling-areas[]
    }

    void boundsChecking() {
        Cell cell = new Cell(" ", style);
        // tag::bounds-checking[]
        if (x >= area.x() && x < area.right() &&
            y >= area.y() && y < area.bottom()) {
            buffer.set(x, y, cell);
        }
        // end::bounds-checking[]
    }

    void exportFluentApi() throws IOException {
        // tag::export-fluent-api[]
        // Start from a buffer (e.g. after rendering widgets)
        Buffer buffer = Buffer.empty(new Rect(0, 0, 80, 24));

        // Shorthand: export as SVG with default options
        export(buffer).svg().toFile(Path.of("output.svg"));

        // Configure options via options(Consumer)
        export(buffer).svg()
            .options(o -> o.title("My App"))
            .toFile(Path.of("output.svg"));

        // Export to string or bytes
        String svgString = export(buffer).svg().toString();
        byte[] htmlBytes = export(buffer).html().options(o -> o.inlineStyles(true)).toBytes();

        // Write to an OutputStream or Writer
        export(buffer).text().to(outputStream);
        // end::export-fluent-api[]
    }

    void exportCustomFormat() throws IOException {
        Path path = Path.of("output.svg");
        // tag::export-custom-format[]
        export(buffer).as(Formats.SVG).toFile(path);
        // end::export-custom-format[]
    }

    void exportByExtension() throws IOException {
        // tag::export-by-extension[]
        export(buffer).toFile(Path.of("screenshot.svg"));   // SVG
        export(buffer).toFile(Path.of("page.html"));       // HTML
        export(buffer).toFile(Path.of("dump.txt"));        // Plain text
        export(buffer).toFile(Path.of("styled.ansi"));     // ANSI-styled text
        // end::export-by-extension[]
    }

    void exportCropping() throws IOException {
        // tag::export-cropping[]
        // Export a single region (e.g. title, table, or footer)
        Rect titleRect = new Rect(0, 0, 80, 3);
        export(buffer).crop(titleRect).svg().toFile(Path.of("title.svg"));

        // Combined rectangle of multiple elements: union of two rects
        Rect tableRect = new Rect(22, 3, 58, 12);
        Rect footerRect = new Rect(0, 15, 80, 2);
        Rect tableAndFooter = tableRect.union(footerRect);
        export(buffer).crop(tableAndFooter).svg().toFile(Path.of("table_and_footer.svg"));
        // end::export-cropping[]
    }

    void exportElementArea() throws IOException {
        // tag::export-element-area[]
        // After rendering (e.g. in an action handler with access to buffer and element)
        Rect area = myPanel.renderedArea();
        if (area != null && !area.isEmpty()) {
            export(buffer).crop(area).svg().toFile(Path.of("panel.svg"));
        }
        // end::export-element-area[]
    }

    void exportDefaultColors() throws IOException {
        Path path = Path.of("output.svg");
        // tag::export-default-colors[]
        // Use property defaults (dark theme) — no resolver
        export(buffer).svg().toFile(path);

        // Custom defaults via a resolver (e.g. from toolkit StyleEngine)
        StylePropertyResolver myResolver = StylePropertyResolver.empty();  // e.g. styleEngine.resolver()
        export(buffer).svg().options(o -> o.styles(myResolver)).toFile(path);
        // end::export-default-colors[]
    }

    void exportSvgOptions() throws IOException {
        // tag::export-svg-options[]
        export(buffer).svg()
            .options(o -> o.title("Dashboard"))
            .toFile(Path.of("dashboard.svg"));
        // end::export-svg-options[]
    }

    void exportHtmlOptions() throws IOException {
        // tag::export-html-options[]
        // One file with external stylesheet (default)
        export(buffer).html().toFile(Path.of("output.html"));

        // Inline styles (self-contained, no separate CSS)
        export(buffer).html()
            .options(o -> o.inlineStyles(true))
            .toFile(Path.of("output_inline.html"));
        // end::export-html-options[]
    }

    void exportTextOptions() throws IOException {
        // tag::export-text-options[]
        // Plain text
        String plain = export(buffer).text().toString();
        export(buffer).text().toFile(Path.of("dump.txt"));

        // ANSI-styled (e.g. for .ansi files or terminal paste)
        String ansi = export(buffer).text().options(o -> o.styles(true)).toString();
        export(buffer).text().options(o -> o.styles(true)).toFile(Path.of("styled.ansi"));
        // end::export-text-options[]
    }

    // tag::counter-card[]
    public class CounterCard extends Component<CounterCard> {
        private int count = 0;

        @OnAction("increment")
        void onIncrement(Event event) {
            count++;
        }

        @OnAction("decrement")
        void onDecrement(Event event) {
            count--;
        }

        @Override
        protected Element render() {
            var borderColor = isFocused() ? Color.CYAN : Color.GRAY;

            return panel(() -> column(
                    text("Count: " + count).bold(),
                    text("Press +/- to change").dim()
            ))
            .rounded()
            .borderColor(borderColor)
            .fill();
        }
    }
    // end::counter-card[]

    void usingComponents() throws Exception {
        // tag::using-components[]
        var counter1 = new CounterCard().id("counter-1");
        var counter2 = new CounterCard().id("counter-2");

        // Define bindings
        var bindings = BindingSets.standard()
            .toBuilder()
            .bind(KeyTrigger.ch('+'), "increment")
            .bind(KeyTrigger.ch('='), "increment")
            .bind(KeyTrigger.ch('-'), "decrement")
            .bind(KeyTrigger.ch('_'), "decrement")
            .build();

        try (var runner = ToolkitRunner.builder()
                .bindings(bindings)
                .build()) {
            runner.run(() -> row(counter1, counter2));
        }
        // end::using-components[]
    }

    // tag::custom-properties[]
    public class MyGauge implements Widget {
        // Define a custom property for the filled bar color
        public static final PropertyDefinition<Color> BAR_COLOR =
            PropertyDefinition.of("bar-color", ColorConverter.INSTANCE);

        // Register the property so style resolvers recognize it
        static {
            PropertyRegistry.register(BAR_COLOR);
        }

        @Override
        public void render(Rect area, Buffer buffer) {
            // Widget implementation
        }
    }
    // end::custom-properties[]

    // tag::style-property-resolver[]
    public static class MyGaugeWithResolver implements Widget {
        public static final PropertyDefinition<Color> BAR_COLOR =
            PropertyDefinition.of("bar-color", ColorConverter.INSTANCE);

        private final Color barColor;

        private MyGaugeWithResolver(Builder builder) {
            // Resolve: programmatic value → CSS value → property default
            this.barColor = builder.resolveBarColor();
        }

        @Override
        public void render(Rect area, Buffer buffer) {
            // Use barColor for rendering
        }

        public static final class Builder {
            private Color barColor;  // Programmatic value
            private StylePropertyResolver styleResolver = StylePropertyResolver.empty();

            public Builder barColor(Color color) {
                this.barColor = color;
                return this;
            }

            public Builder styleResolver(StylePropertyResolver resolver) {
                this.styleResolver = resolver != null ? resolver : StylePropertyResolver.empty();
                return this;
            }

            private Color resolveBarColor() {
                return styleResolver.resolve(BAR_COLOR, barColor);
            }

            public MyGaugeWithResolver build() {
                return new MyGaugeWithResolver(this);
            }
        }
    }
    // end::style-property-resolver[]

    void standardProperties() {
        StylePropertyResolver styleResolver = StylePropertyResolver.empty();
        Color background = null;
        Color foreground = null;
        // tag::standard-properties[]
        // In your resolution method:
        Color bg = styleResolver.resolve(StandardProperties.BACKGROUND, background);
        Color fg = styleResolver.resolve(StandardProperties.COLOR, foreground);
        // end::standard-properties[]
    }

    void charWidthBasics() {
        // tag::charwidth-basics[]
        // Get display width of a string
        int width = CharWidth.of("Hello 世界 🔥");  // Returns 14, not 11

        // Get display width of a code point
        int cpWidth = CharWidth.of(0x4E16);  // Returns 2 (CJK character)

        // Truncate to fit display width (preserves grapheme clusters)
        String truncated = CharWidth.substringByWidth("Hello 👨‍🦲 World", 8);  // "Hello 👨‍🦲"

        // Truncate from the end
        String suffix = CharWidth.substringByWidthFromEnd("Hello World", 5);  // "World"

        // Truncate with ellipsis
        String ellipsized = CharWidth.truncateWithEllipsis(
            "Very long text here",
            10,
            CharWidth.TruncatePosition.END
        );  // "Very lo..."
        // end::charwidth-basics[]
    }

    void charWidthCalculation() {
        String text = "Hello";
        // tag::charwidth-calculation[]
        // WRONG - breaks emoji and CJK
        int wrongWidth = text.length();

        // CORRECT - respects display width
        int correctWidth = CharWidth.of(text);
        // end::charwidth-calculation[]
    }

    void charWidthTruncation() {
        String text = "Hello";
        int maxWidth = 10;
        // tag::charwidth-truncation[]
        // WRONG - may break mid-grapheme cluster
        String wrongTruncated = text.substring(0, Math.min(text.length(), maxWidth));

        // CORRECT - preserves grapheme boundaries
        String correctTruncated = CharWidth.substringByWidth(text, maxWidth);
        // end::charwidth-truncation[]
    }

    void charWidthPositionTracking() {
        String text = "Hello";
        // tag::charwidth-position-tracking[]
        // WRONG - position drift with wide characters
        int col = x;
        buffer.setString(col, y, text, style);
        col += text.length();

        // CORRECT - use display width
        int col2 = x;
        buffer.setString(col2, y, text, style);
        col2 += CharWidth.of(text);
        // end::charwidth-position-tracking[]
    }

    void charWidthCentering() {
        String label = "Hello";
        int width = 20;
        // tag::charwidth-centering[]
        // WRONG - misaligned with CJK/emoji
        int wrongLabelX = x + (width - label.length()) / 2;

        // CORRECT - proper centering
        int correctLabelX = x + (width - CharWidth.of(label)) / 2;
        // end::charwidth-centering[]
    }

    void charWidthEllipsis() {
        String text = "Hello";
        int maxWidth = 10;
        // tag::charwidth-ellipsis[]
        // WRONG - char count, not display width
        String wrongEllipsis = text;
        if (text.length() > maxWidth) {
            wrongEllipsis = text.substring(0, maxWidth - 3) + "...";
        }

        // CORRECT - using CharWidth utilities
        String correctEllipsis = CharWidth.truncateWithEllipsis(text, maxWidth, CharWidth.TruncatePosition.END);

        // Or manually:
        String manualEllipsis = text;
        if (CharWidth.of(text) > maxWidth) {
            int ellipsisWidth = CharWidth.of("...");
            manualEllipsis = CharWidth.substringByWidth(text, maxWidth - ellipsisWidth) + "...";
        }
        // end::charwidth-ellipsis[]
    }

    void charWidthTruncatePositions() {
        // tag::charwidth-truncate-positions[]
        String text = "Hello World Example";

        // END: "Hello Wor..."
        String endTruncate = CharWidth.truncateWithEllipsis(text, 12, CharWidth.TruncatePosition.END);

        // START: "...d Example"
        String startTruncate = CharWidth.truncateWithEllipsis(text, 12, CharWidth.TruncatePosition.START);

        // MIDDLE: "Hell...ample"
        String middleTruncate = CharWidth.truncateWithEllipsis(text, 12, CharWidth.TruncatePosition.MIDDLE);
        // end::charwidth-truncate-positions[]
    }

    void charWidthCustomEllipsis() {
        String text = "Hello World Example";
        // tag::charwidth-custom-ellipsis[]
        String customEllipsis = CharWidth.truncateWithEllipsis(text, 12, "…", CharWidth.TruncatePosition.END);
        // end::charwidth-custom-ellipsis[]
    }

    void charWidthZwjSafety() {
        // tag::charwidth-zwj-safety[]
        // "👨‍🦲" is: man (👨) + ZWJ + bald (🦲) = 5 code units, 2 display columns

        // Safe truncation - won't break mid-sequence
        String result = CharWidth.substringByWidth("A👨‍🦲B", 3);
        // Returns "A👨‍🦲" (width 3), not "A👨" (broken sequence showing "?")
        // end::charwidth-zwj-safety[]
    }
}
