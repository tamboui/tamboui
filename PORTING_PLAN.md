# JRatatui: Pure Java Port of Ratatui

A comprehensive plan for creating a pure Java port of [ratatui](https://github.com/ratatui/ratatui), the Rust TUI library.

## 1. Ratatui Architecture Overview

Ratatui is a modular Rust TUI library with these core components:

| Module | Purpose |
|--------|---------|
| `ratatui-core` | Foundation: Buffer, Cell, Rect, Style, Layout, Text primitives |
| `ratatui-widgets` | 14 standard widgets (Block, Paragraph, List, Table, etc.) |
| Backend crates | Terminal abstraction (crossterm, termion, termwiz) |

### Key Design Principles

1. **Immediate-mode rendering** - Redraw entire UI each frame
2. **Intermediate buffers** - Widgets render to `Buffer`, not directly to terminal
3. **Constraint-based layout** - Uses Cassowary solver for flexible sizing
4. **Diff-based updates** - Only changed cells are sent to terminal
5. **Backend abstraction** - Pluggable terminal implementations

### Rendering Pipeline

```
1. Application calls Terminal.draw(frame -> { ... })
2. Terminal creates Frame with current Buffer
3. Closure renders widgets:
   - frame.renderWidget(widget, area)
   - frame.renderStatefulWidget(widget, area, state)
4. Widgets call buffer.setString() and other methods
5. Terminal compares current with previous buffer
6. Only differences sent to Backend via draw()
7. Backend flushes changes to terminal
8. Buffers swapped for next cycle
```

---

## 2. Core Abstractions to Port

### A. Geometry (`layout` package)

```java
public record Rect(int x, int y, int width, int height) {
    public int left() { return x; }
    public int right() { return x + width; }
    public int top() { return y; }
    public int bottom() { return y + height; }
    public int area() { return width * height; }
    public boolean isEmpty() { return width == 0 || height == 0; }
    public Rect inner(Margin margin) { ... }
    public Rect intersection(Rect other) { ... }
    public Rect union(Rect other) { ... }
    public boolean contains(Position pos) { ... }
}

public record Position(int x, int y) {}
public record Size(int width, int height) {}
public record Margin(int top, int right, int bottom, int left) {
    public static Margin uniform(int value) { ... }
    public static Margin symmetric(int vertical, int horizontal) { ... }
}
```

### B. Styling (`style` package)

```java
public sealed interface Color permits
    Color.Reset, Color.Ansi, Color.Rgb, Color.Indexed {

    record Reset() implements Color {}
    record Ansi(AnsiColor color) implements Color {}
    record Rgb(int r, int g, int b) implements Color {}
    record Indexed(int index) implements Color {}

    // Convenience constants
    Color BLACK = new Ansi(AnsiColor.BLACK);
    Color RED = new Ansi(AnsiColor.RED);
    Color GREEN = new Ansi(AnsiColor.GREEN);
    // ... etc
}

public enum AnsiColor {
    BLACK, RED, GREEN, YELLOW, BLUE, MAGENTA, CYAN, WHITE,
    BRIGHT_BLACK, BRIGHT_RED, BRIGHT_GREEN, BRIGHT_YELLOW,
    BRIGHT_BLUE, BRIGHT_MAGENTA, BRIGHT_CYAN, BRIGHT_WHITE
}

public enum Modifier {
    BOLD, DIM, ITALIC, UNDERLINED, SLOW_BLINK,
    RAPID_BLINK, REVERSED, HIDDEN, CROSSED_OUT
}

public record Style(
    Optional<Color> fg,
    Optional<Color> bg,
    Optional<Color> underlineColor,
    EnumSet<Modifier> addModifiers,
    EnumSet<Modifier> subModifiers
) {
    public static final Style EMPTY = new Style(
        Optional.empty(), Optional.empty(), Optional.empty(),
        EnumSet.noneOf(Modifier.class), EnumSet.noneOf(Modifier.class)
    );

    // Builder-style methods (return new instances)
    public Style fg(Color color) { ... }
    public Style bg(Color color) { ... }
    public Style bold() { ... }
    public Style italic() { ... }
    public Style underlined() { ... }
    public Style reversed() { ... }

    // Combine styles (other overwrites this where set)
    public Style patch(Style other) { ... }
}
```

### C. Text Hierarchy (`text` package)

```java
public record Span(String content, Style style) {
    public static Span raw(String content) {
        return new Span(content, Style.EMPTY);
    }

    public static Span styled(String content, Style style) {
        return new Span(content, style);
    }

    public int width() {
        // Handle Unicode width (grapheme clusters)
        return content.codePointCount(0, content.length());
    }

    // Builder-style styling
    public Span style(Style style) { return new Span(content, style); }
    public Span fg(Color color) { return new Span(content, style.fg(color)); }
    public Span bg(Color color) { return new Span(content, style.bg(color)); }
    public Span bold() { return new Span(content, style.bold()); }
}

public record Line(List<Span> spans, Optional<Alignment> alignment) {
    public static Line from(String text) {
        return new Line(List.of(Span.raw(text)), Optional.empty());
    }

    public static Line from(Span... spans) {
        return new Line(List.of(spans), Optional.empty());
    }

    public int width() {
        return spans.stream().mapToInt(Span::width).sum();
    }

    public Line alignment(Alignment alignment) {
        return new Line(spans, Optional.of(alignment));
    }
}

public record Text(List<Line> lines, Optional<Alignment> alignment) {
    public static Text from(String text) {
        var lines = text.lines()
            .map(Line::from)
            .toList();
        return new Text(lines, Optional.empty());
    }

    public int height() { return lines.size(); }

    public int width() {
        return lines.stream()
            .mapToInt(Line::width)
            .max()
            .orElse(0);
    }
}
```

### D. Buffer System (`buffer` package)

```java
public record Cell(String symbol, Style style) {
    public static final Cell EMPTY = new Cell(" ", Style.EMPTY);

    public Cell reset() {
        return EMPTY;
    }

    public Cell setSymbol(String symbol) {
        return new Cell(symbol, this.style);
    }

    public Cell setStyle(Style style) {
        return new Cell(this.symbol, style);
    }
}

public class Buffer {
    private final Rect area;
    private final Cell[] content;  // area.width() * area.height()

    private Buffer(Rect area, Cell[] content) {
        this.area = area;
        this.content = content;
    }

    public static Buffer empty(Rect area) {
        Cell[] content = new Cell[area.area()];
        Arrays.fill(content, Cell.EMPTY);
        return new Buffer(area, content);
    }

    public static Buffer filled(Rect area, Cell cell) {
        Cell[] content = new Cell[area.area()];
        Arrays.fill(content, cell);
        return new Buffer(area, content);
    }

    public Rect area() { return area; }

    public Cell get(int x, int y) {
        return content[index(x, y)];
    }

    public void set(int x, int y, Cell cell) {
        if (area.contains(new Position(x, y))) {
            content[index(x, y)] = cell;
        }
    }

    public void setString(int x, int y, String string, Style style) {
        int col = x;
        for (int i = 0; i < string.length(); ) {
            int codePoint = string.codePointAt(i);
            String symbol = new String(Character.toChars(codePoint));
            set(col, y, new Cell(symbol, style));
            col++;
            i += Character.charCount(codePoint);
        }
    }

    public void setStyle(Rect area, Style style) {
        Rect intersection = this.area.intersection(area);
        for (int y = intersection.top(); y < intersection.bottom(); y++) {
            for (int x = intersection.left(); x < intersection.right(); x++) {
                Cell cell = get(x, y);
                set(x, y, cell.setStyle(cell.style().patch(style)));
            }
        }
    }

    public List<CellUpdate> diff(Buffer other) {
        List<CellUpdate> updates = new ArrayList<>();
        for (int i = 0; i < content.length; i++) {
            if (!content[i].equals(other.content[i])) {
                int x = area.x() + (i % area.width());
                int y = area.y() + (i / area.width());
                updates.add(new CellUpdate(x, y, content[i]));
            }
        }
        return updates;
    }

    private int index(int x, int y) {
        return (y - area.y()) * area.width() + (x - area.x());
    }
}

public record CellUpdate(int x, int y, Cell cell) {}
```

### E. Layout System (`layout` package)

```java
public sealed interface Constraint permits
    Constraint.Length, Constraint.Percentage, Constraint.Ratio,
    Constraint.Min, Constraint.Max, Constraint.Fill {

    record Length(int value) implements Constraint {}
    record Percentage(int value) implements Constraint {}  // 0-100
    record Ratio(int numerator, int denominator) implements Constraint {}
    record Min(int value) implements Constraint {}
    record Max(int value) implements Constraint {}
    record Fill(int weight) implements Constraint {}  // weight for proportional distribution

    // Convenience factory methods
    static Constraint length(int value) { return new Length(value); }
    static Constraint percentage(int value) { return new Percentage(value); }
    static Constraint ratio(int num, int den) { return new Ratio(num, den); }
    static Constraint min(int value) { return new Min(value); }
    static Constraint max(int value) { return new Max(value); }
    static Constraint fill(int weight) { return new Fill(weight); }
    static Constraint fill() { return new Fill(1); }
}

public enum Direction { HORIZONTAL, VERTICAL }

public enum Alignment { START, CENTER, END }

public enum Flex {
    LEGACY,      // Original tui-rs behavior
    START,       // Pack to start
    CENTER,      // Center elements
    END,         // Pack to end
    SPACE_BETWEEN,
    SPACE_AROUND
}

public class Layout {
    private final Direction direction;
    private final List<Constraint> constraints;
    private final Margin margin;
    private final int spacing;
    private final Flex flex;

    private Layout(Direction direction, List<Constraint> constraints,
                   Margin margin, int spacing, Flex flex) {
        this.direction = direction;
        this.constraints = constraints;
        this.margin = margin;
        this.spacing = spacing;
        this.flex = flex;
    }

    public static Layout vertical() {
        return new Layout(Direction.VERTICAL, List.of(),
                         Margin.uniform(0), 0, Flex.START);
    }

    public static Layout horizontal() {
        return new Layout(Direction.HORIZONTAL, List.of(),
                         Margin.uniform(0), 0, Flex.START);
    }

    public Layout constraints(Constraint... constraints) {
        return new Layout(direction, List.of(constraints), margin, spacing, flex);
    }

    public Layout constraints(List<Constraint> constraints) {
        return new Layout(direction, constraints, margin, spacing, flex);
    }

    public Layout margin(Margin margin) {
        return new Layout(direction, constraints, margin, spacing, flex);
    }

    public Layout spacing(int spacing) {
        return new Layout(direction, constraints, margin, spacing, flex);
    }

    public Layout flex(Flex flex) {
        return new Layout(direction, constraints, margin, spacing, flex);
    }

    public List<Rect> split(Rect area) {
        // Apply margin first
        Rect inner = area.inner(margin);

        // Use constraint solver to allocate space
        return ConstraintSolver.solve(inner, direction, constraints, spacing, flex);
    }
}
```

### F. Widget System (`widgets` package)

```java
@FunctionalInterface
public interface Widget {
    void render(Rect area, Buffer buffer);
}

public interface StatefulWidget<S> {
    void render(Rect area, Buffer buffer, S state);
}

// Convenience interface for widgets that support styling
public interface Stylize<T> {
    T style(Style style);

    default T fg(Color color) {
        return style(Style.EMPTY.fg(color));
    }

    default T bg(Color color) {
        return style(Style.EMPTY.bg(color));
    }

    default T bold() {
        return style(Style.EMPTY.bold());
    }

    // ... more style methods
}
```

### G. Terminal & Frame (`terminal` package)

```java
public interface Backend extends AutoCloseable {
    void draw(Iterable<CellUpdate> updates);
    void flush() throws IOException;
    void clear() throws IOException;
    Size size() throws IOException;
    void showCursor() throws IOException;
    void hideCursor() throws IOException;
    Position getCursorPosition() throws IOException;
    void setCursorPosition(Position position) throws IOException;
    void scrollUp(int lines) throws IOException;
    void scrollDown(int lines) throws IOException;
}

public class Frame {
    private final Buffer buffer;
    private final Rect area;
    private Position cursorPosition;
    private boolean cursorVisible;

    Frame(Buffer buffer) {
        this.buffer = buffer;
        this.area = buffer.area();
        this.cursorVisible = false;
    }

    public Rect area() { return area; }

    public Buffer buffer() { return buffer; }

    public void renderWidget(Widget widget, Rect area) {
        widget.render(area, buffer);
    }

    public <S> void renderStatefulWidget(StatefulWidget<S> widget, Rect area, S state) {
        widget.render(area, buffer, state);
    }

    public void setCursorPosition(Position position) {
        this.cursorPosition = position;
        this.cursorVisible = true;
    }
}

public record CompletedFrame(Buffer buffer, Rect area) {}

public enum Viewport {
    FULLSCREEN,
    INLINE(int height),
    FIXED(Rect area)
}

public class Terminal<B extends Backend> implements AutoCloseable {
    private final B backend;
    private Buffer currentBuffer;
    private Buffer previousBuffer;
    private final Viewport viewport;
    private boolean hiddenCursor;

    public Terminal(B backend) throws IOException {
        this(backend, Viewport.FULLSCREEN);
    }

    public Terminal(B backend, Viewport viewport) throws IOException {
        this.backend = backend;
        this.viewport = viewport;
        this.hiddenCursor = false;

        Size size = backend.size();
        Rect area = new Rect(0, 0, size.width(), size.height());
        this.currentBuffer = Buffer.empty(area);
        this.previousBuffer = Buffer.empty(area);
    }

    public B backend() { return backend; }

    public CompletedFrame draw(Consumer<Frame> renderer) throws IOException {
        // 1. Handle resize if needed
        Size size = backend.size();
        Rect area = new Rect(0, 0, size.width(), size.height());

        if (!area.equals(currentBuffer.area())) {
            currentBuffer = Buffer.empty(area);
            previousBuffer = Buffer.empty(area);
            backend.clear();
        }

        // 2. Reset current buffer
        currentBuffer = Buffer.empty(area);

        // 3. Create frame and render
        Frame frame = new Frame(currentBuffer);
        renderer.accept(frame);

        // 4. Calculate diff and draw
        List<CellUpdate> updates = currentBuffer.diff(previousBuffer);
        backend.draw(updates);

        // 5. Handle cursor
        // ... cursor logic

        // 6. Flush
        backend.flush();

        // 7. Swap buffers
        Buffer temp = previousBuffer;
        previousBuffer = currentBuffer;
        currentBuffer = temp;

        return new CompletedFrame(previousBuffer, area);
    }

    public void clear() throws IOException {
        backend.clear();
        currentBuffer = Buffer.empty(currentBuffer.area());
        previousBuffer = Buffer.empty(previousBuffer.area());
    }

    public void showCursor() throws IOException {
        backend.showCursor();
        hiddenCursor = false;
    }

    public void hideCursor() throws IOException {
        backend.hideCursor();
        hiddenCursor = true;
    }

    @Override
    public void close() throws Exception {
        if (hiddenCursor) {
            backend.showCursor();
        }
        backend.close();
    }
}
```

---

## 3. Widgets to Implement

### Implementation Status

| Widget | Type | Status | Description |
|--------|------|--------|-------------|
| `Block` | Stateless | ✅ Done | Container with borders and titles |
| `Paragraph` | Stateless | ✅ Done | Multi-line styled/wrapped text |
| `List` | Stateful | ✅ Done | Scrollable items with selection |
| `TextInput` | Stateful | ✅ Done | Single-line text input (JRatatui addition) |
| `Clear` | Stateless | ✅ Done | Clears area (for layering widgets) |
| `Gauge` | Stateless | ✅ Done | Progress percentage display |
| `LineGauge` | Stateless | ✅ Done | Progress as line |
| `Sparkline` | Stateless | ✅ Done | Single dataset sparkline visualization |
| `BarChart` | Stateless | ❌ TODO | Multiple datasets as bars |
| `Table` | Stateful | ✅ Done | Grid with rows/columns and selection |
| `Tabs` | Stateful | ✅ Done | Tab bar with selection |
| `Scrollbar` | Stateful | ✅ Done | Visual scrollbar indicator |
| `Chart` | Stateless | ❌ TODO | Line/scatter graphs for datasets |
| `Canvas` | Stateless | ❌ TODO | Arbitrary shape drawing with braille/block characters |
| `Calendar` | Stateful | ❌ TODO | Monthly calendar view (optional feature) |

### Demo Requirement

**When implementing a new widget, a corresponding demo must be created** under the `demos/` directory to showcase its functionality. Each demo should:

1. Be a separate Gradle subproject under `demos/` (e.g., `demos/gauge-demo/`)
2. Demonstrate the widget's key features and configuration options
3. Include keyboard interaction where applicable
4. Support GraalVM native image compilation

The build automatically includes any subdirectory of `demos/` as a project.

### Stateless Widgets (implement `Widget`)

| Widget | Description | Priority |
|--------|-------------|----------|
| `Clear` | Clears area (for layering widgets) | High |
| `Gauge` | Progress percentage using block characters | High |
| `LineGauge` | Progress as a horizontal line | High |
| `Sparkline` | Single dataset sparkline visualization | Medium |
| `BarChart` | Multiple datasets as grouped bars | Medium |
| `Chart` | Line/scatter graphs for datasets | Low |
| `Canvas` | Arbitrary shape drawing with braille/block characters | Low |

### Stateful Widgets (implement `StatefulWidget<S>`)

| Widget | State Class | Description | Priority |
|--------|-------------|-------------|----------|
| `Table` | `TableState` | Grid with rows/columns and selection | High |
| `Tabs` | `TabsState` | Tab bar with selection | High |
| `Scrollbar` | `ScrollbarState` | Visual scrollbar indicator | Medium |
| `Calendar` | `CalendarState` | Monthly calendar view | Low |

### Example Widget Implementation

```java
public class Block implements Widget {
    private final Optional<Title> title;
    private final Optional<Title> titleBottom;
    private final Borders borders;
    private final BorderType borderType;
    private final Style borderStyle;
    private final Style style;
    private final Padding padding;

    private Block(Builder builder) {
        this.title = builder.title;
        this.titleBottom = builder.titleBottom;
        this.borders = builder.borders;
        this.borderType = builder.borderType;
        this.borderStyle = builder.borderStyle;
        this.style = builder.style;
        this.padding = builder.padding;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static Block bordered() {
        return builder().borders(Borders.ALL).build();
    }

    public Rect inner(Rect area) {
        // Calculate inner area after borders and padding
        int x = area.x() + (borders.contains(Border.LEFT) ? 1 : 0) + padding.left();
        int y = area.y() + (borders.contains(Border.TOP) ? 1 : 0) + padding.top();
        int width = area.width()
            - (borders.contains(Border.LEFT) ? 1 : 0)
            - (borders.contains(Border.RIGHT) ? 1 : 0)
            - padding.left() - padding.right();
        int height = area.height()
            - (borders.contains(Border.TOP) ? 1 : 0)
            - (borders.contains(Border.BOTTOM) ? 1 : 0)
            - padding.top() - padding.bottom();

        return new Rect(x, y, Math.max(0, width), Math.max(0, height));
    }

    @Override
    public void render(Rect area, Buffer buffer) {
        // 1. Fill background
        buffer.setStyle(area, style);

        // 2. Draw borders
        if (!borders.isEmpty()) {
            renderBorders(area, buffer);
        }

        // 3. Draw titles
        title.ifPresent(t -> renderTitle(t, area, buffer, true));
        titleBottom.ifPresent(t -> renderTitle(t, area, buffer, false));
    }

    private void renderBorders(Rect area, Buffer buffer) {
        BorderSet set = borderType.getSet();
        // ... draw border characters
    }

    public static class Builder {
        // Builder implementation
    }
}
```

---

## 4. Backend Options for Java

### Recommended: JLine 3

[JLine](https://github.com/jline/jline3) is a modern, actively maintained terminal library for Java.

```java
public class JLineBackend implements Backend {
    private final org.jline.terminal.Terminal terminal;
    private final PrintWriter writer;

    public JLineBackend() throws IOException {
        this.terminal = TerminalBuilder.builder()
            .system(true)
            .jansi(true)
            .build();
        terminal.enterRawMode();
        this.writer = terminal.writer();
    }

    @Override
    public void draw(Iterable<CellUpdate> updates) {
        for (CellUpdate update : updates) {
            moveCursor(update.x(), update.y());
            applyStyle(update.cell().style());
            writer.print(update.cell().symbol());
        }
    }

    @Override
    public void flush() {
        writer.flush();
    }

    @Override
    public Size size() {
        return new Size(terminal.getWidth(), terminal.getHeight());
    }

    // ... other Backend methods

    private void moveCursor(int x, int y) {
        writer.print("\033[" + (y + 1) + ";" + (x + 1) + "H");
    }

    private void applyStyle(Style style) {
        // Build ANSI escape sequence for style
        StringBuilder sb = new StringBuilder("\033[0");
        style.fg().ifPresent(c -> sb.append(";").append(fgCode(c)));
        style.bg().ifPresent(c -> sb.append(";").append(bgCode(c)));
        for (Modifier mod : style.addModifiers()) {
            sb.append(";").append(modifierCode(mod));
        }
        sb.append("m");
        writer.print(sb.toString());
    }

    @Override
    public void close() throws Exception {
        writer.print("\033[0m");  // Reset style
        writer.print("\033[?25h"); // Show cursor
        writer.flush();
        terminal.close();
    }
}
```

### Alternative Backends

| Backend | Pros | Cons |
|---------|------|------|
| **Lanterna** | Pure Java, Swing fallback emulator | Higher-level API mismatch |
| **Jexer** | Sixel images, embedded terminal | Complex, Turbo Vision style |
| **Raw ANSI** | Zero dependencies | Limited platform support |

---

## 5. ANSI Support Analysis: JLine/Jansi vs Ratatui Requirements

This section analyzes whether JLine 3 (with Jansi) provides sufficient ANSI terminal support for a full ratatui port.

### Ratatui Terminal Requirements

Ratatui (via its crossterm backend) uses these terminal capabilities:

| Category | Feature | ANSI Sequence |
|----------|---------|---------------|
| **Colors** | 16 ANSI colors | `ESC[30-37m`, `ESC[90-97m` |
| | 256-color indexed | `ESC[38;5;Nm`, `ESC[48;5;Nm` |
| | RGB true color (24-bit) | `ESC[38;2;R;G;Bm`, `ESC[48;2;R;G;Bm` |
| | Underline color | `ESC[58;2;R;G;Bm`, `ESC[58;5;Nm` |
| **Modifiers** | Bold | `ESC[1m` |
| | Dim | `ESC[2m` |
| | Italic | `ESC[3m` |
| | Underlined | `ESC[4m` |
| | Slow blink | `ESC[5m` |
| | Rapid blink | `ESC[6m` |
| | Reversed | `ESC[7m` |
| | Hidden | `ESC[8m` |
| | Crossed out | `ESC[9m` |
| **Cursor** | Show/hide | `ESC[?25h`, `ESC[?25l` |
| | Move absolute | `ESC[row;colH` |
| | Move relative | `ESC[nA/B/C/D` |
| | Save/restore position | `ESC[s`, `ESC[u` |
| **Screen** | Clear screen | `ESC[2J` |
| | Clear line | `ESC[2K` |
| | Scroll up/down | `ESC[nS`, `ESC[nT` |
| | Set scroll region | `ESC[top;bottomr` |
| | Alternate screen | `ESC[?1049h`, `ESC[?1049l` |
| **Input** | Raw mode | Platform-specific |
| | Mouse capture | `ESC[?1000h` and variants |
| | Bracketed paste | `ESC[?2004h` |

### JLine 3 Capabilities

| Feature | JLine Support | Notes |
|---------|---------------|-------|
| **16 ANSI colors** | ✅ Full | Via `AttributedStyle` |
| **256 colors** | ✅ Full | `Colors` class with palette |
| **RGB true color** | ✅ Full | Jansi 2.1.0+ added `fgRgb()`/`bgRgb()` |
| **Underline color** | ⚠️ Manual | Write raw `ESC[58;...]` sequences |
| **All 9 modifiers** | ✅ Full | `AttributedStyle.BOLD`, `.ITALIC`, etc. |
| **Cursor show/hide** | ✅ Full | `InfoCmp.Capability.cursor_visible` |
| **Cursor positioning** | ✅ Full | `InfoCmp.Capability.cursor_address` |
| **Screen clear** | ✅ Full | `InfoCmp.Capability.clear_screen` |
| **Line clear** | ✅ Full | `InfoCmp.Capability.clr_eol` |
| **Alternate screen** | ✅ Full | `enter_ca_mode` / `exit_ca_mode` |
| **Raw mode** | ✅ Full | `terminal.enterRawMode()` |
| **Mouse support** | ✅ Full | `Terminal.MouseEvent`, tracking modes |
| **Scroll regions** | ✅ Full | `change_scroll_region` capability |
| **Bracketed paste** | ⚠️ Manual | Write raw sequences |

### Jansi Capabilities (used by JLine on Windows)

| Feature | Jansi Support | Version |
|---------|---------------|---------|
| **16 ANSI colors** | ✅ Full | All versions |
| **256 colors** | ✅ Full | 2.1.0+ |
| **RGB true color** | ✅ Full | 2.1.0+ |
| **Text modifiers** | ✅ Full | All versions |
| **Windows console** | ✅ Full | Native JNI fallback |
| **Auto-detection** | ✅ Full | Checks `COLORTERM`, `TERM` |

### Gap Analysis

| Ratatui Feature | JLine/Jansi | Mitigation |
|-----------------|-------------|------------|
| Underline color | Not in API | Write raw ANSI: `ESC[58;2;R;G;Bm` |
| Bracketed paste | Not in API | Write raw ANSI: `ESC[?2004h` |
| Focus events | Not supported | Application-level handling |
| Kitty keyboard | Not supported | Not critical for basic TUI |

### Conclusion: JLine 3 is Sufficient ✅

JLine 3 with Jansi provides **all critical capabilities** needed for a ratatui port:

1. **Colors**: Full support for 16, 256, and 24-bit RGB colors
2. **Modifiers**: All 9 text modifiers (bold, italic, underline, blink, etc.)
3. **Cursor**: Complete cursor control (position, show/hide, save/restore)
4. **Screen**: Clear, scroll regions, alternate screen buffer
5. **Input**: Raw mode, mouse events with coordinates
6. **Cross-platform**: Windows support via Jansi native fallback

**Minor gaps** (underline color, bracketed paste) can be handled by writing raw ANSI escape sequences directly, which JLine allows.

### Recommended JLineBackend Implementation

```java
public class JLineBackend implements Backend {
    private final org.jline.terminal.Terminal terminal;
    private final PrintWriter writer;
    private boolean mouseEnabled = false;

    public JLineBackend() throws IOException {
        this.terminal = TerminalBuilder.builder()
            .system(true)
            .jansi(true)
            .build();
        this.writer = terminal.writer();
    }

    // --- Screen Management ---

    public void enterAlternateScreen() {
        terminal.puts(InfoCmp.Capability.enter_ca_mode);
    }

    public void leaveAlternateScreen() {
        terminal.puts(InfoCmp.Capability.exit_ca_mode);
    }

    public void enterRawMode() {
        terminal.enterRawMode();
    }

    // --- Colors (using raw ANSI for full control) ---

    private String colorToAnsi(Color color, boolean foreground) {
        int base = foreground ? 38 : 48;
        return switch (color) {
            case Color.Reset() -> foreground ? "39" : "49";
            case Color.Ansi(AnsiColor c) -> String.valueOf(ansiCode(c, foreground));
            case Color.Indexed(int idx) -> base + ";5;" + idx;
            case Color.Rgb(int r, int g, int b) -> base + ";2;" + r + ";" + g + ";" + b;
        };
    }

    private String underlineColorToAnsi(Color color) {
        return switch (color) {
            case Color.Indexed(int idx) -> "58;5;" + idx;
            case Color.Rgb(int r, int g, int b) -> "58;2;" + r + ";" + g + ";" + b;
            default -> "";
        };
    }

    // --- Mouse Support ---

    public void enableMouseCapture() {
        writer.print("\033[?1000h");  // Normal tracking
        writer.print("\033[?1002h");  // Button event tracking
        writer.print("\033[?1015h");  // urxvt style
        writer.print("\033[?1006h");  // SGR extended mode
        mouseEnabled = true;
    }

    public void disableMouseCapture() {
        writer.print("\033[?1006l");
        writer.print("\033[?1015l");
        writer.print("\033[?1002l");
        writer.print("\033[?1000l");
        mouseEnabled = false;
    }

    // --- Bracketed Paste ---

    public void enableBracketedPaste() {
        writer.print("\033[?2004h");
    }

    public void disableBracketedPaste() {
        writer.print("\033[?2004l");
    }

    // --- Scrolling ---

    @Override
    public void scrollUp(int lines) {
        writer.print("\033[" + lines + "S");
    }

    @Override
    public void scrollDown(int lines) {
        writer.print("\033[" + lines + "T");
    }

    public void setScrollRegion(int top, int bottom) {
        writer.print("\033[" + top + ";" + bottom + "r");
    }
}
```

### Dependencies

```kotlin
// build.gradle.kts
dependencies {
    implementation("org.jline:jline:3.25.1")
    // Jansi is included transitively by JLine
}
```

### References

- [JLine 3 GitHub](https://github.com/jline/jline3)
- [JLine Mouse Support](https://jline.org/docs/advanced/mouse-support/)
- [JLine Screen Clearing](https://jline.org/docs/advanced/screen-clearing/)
- [JLine Terminal Attributes](https://jline.org/docs/advanced/terminal-attributes/)
- [Jansi GitHub](https://github.com/fusesource/jansi)
- [Jansi 256 Color Support](https://github.com/fusesource/jansi/issues/131)

---

## 6. Cassowary Constraint Solver

### Option 1: Use Existing Library

[cassowary-java](https://github.com/pybee/cassowary-java) provides a pure Java implementation:

```groovy
dependencies {
    implementation 'org.pybee:cassowary:0.1.0'
}
```

### Option 2: Simplified Solver

Ratatui's constraints are simpler than full Cassowary (mostly 1D allocation). A custom solver may be more appropriate:

```java
public class ConstraintSolver {
    public static List<Rect> solve(
            Rect area,
            Direction direction,
            List<Constraint> constraints,
            int spacing,
            Flex flex) {

        int available = direction == Direction.HORIZONTAL
            ? area.width()
            : area.height();
        int totalSpacing = spacing * (constraints.size() - 1);
        int distributable = available - totalSpacing;

        // First pass: allocate fixed sizes
        int[] sizes = new int[constraints.size()];
        int[] mins = new int[constraints.size()];
        int[] maxs = new int[constraints.size()];
        Arrays.fill(maxs, Integer.MAX_VALUE);

        int remaining = distributable;
        int fillWeight = 0;

        for (int i = 0; i < constraints.size(); i++) {
            Constraint c = constraints.get(i);
            switch (c) {
                case Constraint.Length(int v) -> {
                    sizes[i] = v;
                    remaining -= v;
                }
                case Constraint.Percentage(int p) -> {
                    sizes[i] = distributable * p / 100;
                    remaining -= sizes[i];
                }
                case Constraint.Min(int v) -> mins[i] = v;
                case Constraint.Max(int v) -> maxs[i] = v;
                case Constraint.Fill(int w) -> fillWeight += w;
                case Constraint.Ratio(int num, int den) -> {
                    sizes[i] = distributable * num / den;
                    remaining -= sizes[i];
                }
            }
        }

        // Second pass: distribute remaining to Fill constraints
        if (fillWeight > 0 && remaining > 0) {
            for (int i = 0; i < constraints.size(); i++) {
                if (constraints.get(i) instanceof Constraint.Fill(int w)) {
                    sizes[i] = remaining * w / fillWeight;
                }
            }
        }

        // Third pass: apply min/max bounds
        for (int i = 0; i < constraints.size(); i++) {
            sizes[i] = Math.max(mins[i], Math.min(maxs[i], sizes[i]));
        }

        // Build rectangles
        List<Rect> result = new ArrayList<>();
        int pos = direction == Direction.HORIZONTAL ? area.x() : area.y();

        for (int i = 0; i < sizes.length; i++) {
            Rect rect = direction == Direction.HORIZONTAL
                ? new Rect(pos, area.y(), sizes[i], area.height())
                : new Rect(area.x(), pos, area.width(), sizes[i]);
            result.add(rect);
            pos += sizes[i] + spacing;
        }

        return result;
    }
}
```

---

## 7. Project Structure

```
jratatui/
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
├── jratatui-core/
│   ├── build.gradle.kts
│   └── src/
│       ├── main/java/io/github/jratatui/
│       │   ├── buffer/
│       │   │   ├── Buffer.java
│       │   │   ├── Cell.java
│       │   │   └── CellUpdate.java
│       │   ├── layout/
│       │   │   ├── Alignment.java
│       │   │   ├── Constraint.java
│       │   │   ├── ConstraintSolver.java
│       │   │   ├── Direction.java
│       │   │   ├── Flex.java
│       │   │   ├── Layout.java
│       │   │   ├── Margin.java
│       │   │   ├── Position.java
│       │   │   ├── Rect.java
│       │   │   └── Size.java
│       │   ├── style/
│       │   │   ├── AnsiColor.java
│       │   │   ├── Color.java
│       │   │   ├── Modifier.java
│       │   │   ├── Style.java
│       │   │   └── Stylize.java
│       │   ├── terminal/
│       │   │   ├── Backend.java
│       │   │   ├── CompletedFrame.java
│       │   │   ├── Frame.java
│       │   │   ├── Terminal.java
│       │   │   └── Viewport.java
│       │   ├── text/
│       │   │   ├── Line.java
│       │   │   ├── Span.java
│       │   │   └── Text.java
│       │   └── widgets/
│       │       ├── StatefulWidget.java
│       │       └── Widget.java
│       └── test/java/...
├── jratatui-widgets/
│   ├── build.gradle.kts
│   └── src/main/java/io/github/jratatui/widgets/
│       ├── block/
│       │   ├── Block.java
│       │   ├── Border.java
│       │   ├── BorderSet.java
│       │   ├── BorderType.java
│       │   ├── Borders.java
│       │   ├── Padding.java
│       │   └── Title.java
│       ├── paragraph/
│       │   ├── Paragraph.java
│       │   └── Wrap.java
│       ├── list/
│       │   ├── List.java
│       │   ├── ListItem.java
│       │   └── ListState.java
│       ├── table/
│       │   ├── Cell.java
│       │   ├── Row.java
│       │   ├── Table.java
│       │   └── TableState.java
│       ├── gauge/
│       │   ├── Gauge.java
│       │   └── LineGauge.java
│       ├── chart/
│       │   ├── Axis.java
│       │   ├── Chart.java
│       │   └── Dataset.java
│       ├── canvas/
│       │   ├── Canvas.java
│       │   ├── Context.java
│       │   └── shapes/
│       │       ├── Circle.java
│       │       ├── Line.java
│       │       ├── Rectangle.java
│       │       └── Shape.java
│       ├── barchart/
│       │   ├── Bar.java
│       │   ├── BarChart.java
│       │   ├── BarGroup.java
│       │   └── BarChartState.java
│       ├── calendar/
│       │   ├── Calendar.java
│       │   └── CalendarState.java
│       ├── scrollbar/
│       │   ├── Scrollbar.java
│       │   ├── ScrollbarOrientation.java
│       │   └── ScrollbarState.java
│       ├── sparkline/
│       │   └── Sparkline.java
│       ├── tabs/
│       │   ├── Tabs.java
│       │   └── TabsState.java
│       └── Clear.java
├── jratatui-jline/
│   ├── build.gradle.kts
│   └── src/main/java/io/github/jratatui/backend/jline/
│       └── JLineBackend.java
└── demos/
    ├── basic-demo/
    │   ├── build.gradle.kts
    │   └── src/main/java/io/github/jratatui/demo/
    │       └── Demo.java
    ├── gauge-demo/           # (example: add when Gauge is implemented)
    │   └── ...
    └── table-demo/           # (example: add when Table is implemented)
        └── ...
```

Demo projects are automatically discovered by the build system. To add a new demo, simply create a new directory under `demos/` with a `build.gradle.kts` file.

---

## 8. Modern Java Idioms

| Feature | Usage |
|---------|-------|
| **Records** | `Rect`, `Cell`, `Span`, `Line`, `Style`, `Color`, `Position`, `Size` |
| **Sealed interfaces** | `Constraint`, `Color` hierarchies |
| **Pattern matching** | Switch on sealed types in layout solver, widget rendering |
| **`Optional<T>`** | For nullable style components, titles |
| **`EnumSet`** | For `Modifier` flags (efficient bitset operations) |
| **Streams** | Buffer operations, text processing, list transformations |
| **`var`** | Local variable type inference throughout |
| **Text blocks** | Multi-line strings in examples and tests |
| **Virtual threads** | Async event handling with JLine (Java 21+) |
| **`Consumer<T>`** | Callback for `Terminal.draw()` |
| **Builder pattern** | Complex widgets like `Block`, `Table` |

---

## 9. Gradle Build Configuration

### settings.gradle.kts

```kotlin
rootProject.name = "jratatui-parent"

include(
    "jratatui-core",
    "jratatui-widgets",
    "jratatui-jline"
)

// Auto-discover demo projects
File("demos").listFiles()?.forEach {
    if (it.isDirectory) {
        include("demos:${it.name}")
    }
}

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")
```

### build.gradle.kts (root)

```kotlin
plugins {
    java
    `java-library`
    `maven-publish`
}

allprojects {
    group = "io.github.jratatui"
    version = "0.1.0-SNAPSHOT"

    repositories {
        mavenCentral()
    }
}

subprojects {
    apply(plugin = "java-library")
    apply(plugin = "maven-publish")

    java {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(21))
        }
        withSourcesJar()
        withJavadocJar()
    }

    tasks.withType<JavaCompile> {
        options.encoding = "UTF-8"
        options.compilerArgs.addAll(listOf(
            "-Xlint:all",
            "-Werror"
        ))
    }

    tasks.withType<Test> {
        useJUnitPlatform()
    }

    dependencies {
        testImplementation(platform("org.junit:junit-bom:6.0.1"))
        testImplementation("org.junit.jupiter:junit-jupiter")
        testImplementation("org.assertj:assertj-core:3.26.3")
    }

    publishing {
        publications {
            create<MavenPublication>("maven") {
                from(components["java"])
            }
        }
    }
}
```

### jratatui-core/build.gradle.kts

```kotlin
plugins {
    `java-library`
}

description = "Core types and abstractions for JRatatui TUI library"

dependencies {
    // Minimal dependencies for core module
    // ICU4J for proper Unicode grapheme handling (optional)
    // implementation("com.ibm.icu:icu4j:74.2")
}
```

### jratatui-widgets/build.gradle.kts

```kotlin
plugins {
    `java-library`
}

description = "Standard widgets for JRatatui TUI library"

dependencies {
    api(project(":jratatui-core"))
}
```

### jratatui-jline/build.gradle.kts

```kotlin
plugins {
    `java-library`
}

description = "JLine 3 backend for JRatatui TUI library"

dependencies {
    api(project(":jratatui-core"))
    implementation("org.jline:jline:3.25.1")
}
```

### demos/basic-demo/build.gradle.kts

```kotlin
plugins {
    id("io.github.jratatui.demo-project")
}

description = "Demo TUI application using JRatatui"

application {
    mainClass.set("io.github.jratatui.demo.Demo")
}
```

---

## 10. Unit Testing

### Testing Framework: JUnit 5 + AssertJ

JRatatui uses JUnit Jupiter for unit testing with AssertJ for fluent assertions.

#### Dependencies (gradle/libs.versions.toml)

```toml
[versions]
junit = "5.10.1"
assertj = "3.24.2"

[libraries]
junit-bom = { module = "org.junit:junit-bom", version.ref = "junit" }
junit-jupiter = { module = "org.junit.jupiter:junit-jupiter" }
junit-platform-launcher = { module = "org.junit.platform:junit-platform-launcher" }
assertj-core = { module = "org.assertj:assertj-core", version.ref = "assertj" }

[bundles]
testing = ["junit-jupiter", "junit-platform-launcher", "assertj-core"]
```

#### Test Configuration (build.gradle.kts)

```kotlin
subprojects {
    dependencies {
        testImplementation(platform(libs.junit.bom))
        testImplementation(libs.bundles.testing)
    }

    tasks.withType<Test> {
        useJUnitPlatform()
    }
}
```

### Test Categories

| Module | Test Focus |
|--------|------------|
| `jratatui-core` | Layout calculations, buffer operations, style merging, constraint solving |
| `jratatui-widgets` | Widget rendering to buffer, border calculations, text wrapping |
| `jratatui-jline` | Backend integration tests (may require mocking terminal) |

### Example Tests

#### Rect Tests (jratatui-core)

```java
package io.github.jratatui.layout;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import static org.assertj.core.api.Assertions.*;

class RectTest {

    @Test
    @DisplayName("Rect area calculation")
    void areaCalculation() {
        var rect = new Rect(0, 0, 10, 5);
        assertThat(rect.area()).isEqualTo(50);
    }

    @Test
    @DisplayName("Rect intersection")
    void intersection() {
        var a = new Rect(0, 0, 10, 10);
        var b = new Rect(5, 5, 10, 10);
        var intersection = a.intersection(b);

        assertThat(intersection).isEqualTo(new Rect(5, 5, 5, 5));
    }

    @ParameterizedTest
    @CsvSource({
        "0, 0, true",
        "9, 9, true",
        "10, 10, false",
        "-1, 0, false"
    })
    @DisplayName("Rect contains position")
    void containsPosition(int x, int y, boolean expected) {
        var rect = new Rect(0, 0, 10, 10);
        assertThat(rect.contains(new Position(x, y))).isEqualTo(expected);
    }
}
```

#### Buffer Tests (jratatui-core)

```java
package io.github.jratatui.buffer;

import io.github.jratatui.layout.Rect;
import io.github.jratatui.style.Style;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class BufferTest {

    @Test
    void setStringWritesToBuffer() {
        var buffer = Buffer.empty(new Rect(0, 0, 10, 1));
        buffer.setString(0, 0, "Hello", Style.EMPTY);

        assertThat(buffer.get(0, 0).symbol()).isEqualTo("H");
        assertThat(buffer.get(4, 0).symbol()).isEqualTo("o");
    }

    @Test
    void diffReturnsChangedCells() {
        var area = new Rect(0, 0, 5, 1);
        var prev = Buffer.empty(area);
        var curr = Buffer.empty(area);

        curr.setString(0, 0, "Hi", Style.EMPTY);
        var updates = curr.diff(prev);

        assertThat(updates).hasSize(2);
    }
}
```

#### Widget Tests (jratatui-widgets)

```java
package io.github.jratatui.widgets.block;

import io.github.jratatui.buffer.Buffer;
import io.github.jratatui.layout.Rect;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class BlockTest {

    @Test
    void borderedBlockRendersCorners() {
        var area = new Rect(0, 0, 5, 3);
        var buffer = Buffer.empty(area);

        Block.bordered().render(area, buffer);

        // Check corners for default border type (Rounded)
        assertThat(buffer.get(0, 0).symbol()).isEqualTo("╭");
        assertThat(buffer.get(4, 0).symbol()).isEqualTo("╮");
        assertThat(buffer.get(0, 2).symbol()).isEqualTo("╰");
        assertThat(buffer.get(4, 2).symbol()).isEqualTo("╯");
    }

    @Test
    void innerAreaCalculation() {
        var area = new Rect(0, 0, 10, 10);
        var block = Block.bordered();

        var inner = block.inner(area);

        assertThat(inner).isEqualTo(new Rect(1, 1, 8, 8));
    }
}
```

### Test Coverage Goals

| Module | Target Coverage |
|--------|-----------------|
| `jratatui-core` | 90%+ (core logic) |
| `jratatui-widgets` | 80%+ (rendering logic) |
| `jratatui-jline` | 60%+ (integration tests) |

---

## 11. Key Differences from Rust

| Rust Concept | Java Equivalent |
|--------------|-----------------|
| `trait Widget` | `interface Widget` |
| `impl Widget for X` | `class X implements Widget` |
| `&str` / `String` | `String` (immutable) |
| `Option<T>` | `Optional<T>` or nullable |
| `Vec<T>` | `List<T>` (prefer `ArrayList` or immutable `List.of()`) |
| Ownership/borrowing | Garbage collection (simpler memory model) |
| `#[derive(Clone, Copy)]` | Records are immutable by default |
| Procedural macros | No equivalent (use builders, factory methods) |
| `Result<T, E>` | Checked exceptions or `Optional<T>` |
| Pattern matching on enums | Pattern matching with sealed interfaces (Java 21+) |
| `impl Default for X` | Static factory methods, builder defaults |
| Lifetime annotations | Not needed (GC handles this) |
| `Cow<'_, str>` | Just use `String` |

---

## 12. Implementation Phases

### Phase 1: Core Primitives ✅ COMPLETE
- [x] `Rect`, `Position`, `Size`, `Margin`
- [x] `Color`, `Modifier`, `Style`
- [x] `Cell`, `CellUpdate`
- [x] `Buffer`

### Phase 2: Text System ✅ COMPLETE
- [x] `Span`
- [x] `Line`
- [x] `Text`
- [ ] Unicode width handling (basic implementation, needs ICU4J for full support)

### Phase 3: Layout Engine ✅ COMPLETE
- [x] `Constraint` sealed interface
- [x] `Direction`, `Alignment`, `Flex`
- [x] `Layout` builder
- [x] `ConstraintSolver` (simplified implementation)

### Phase 4: Terminal Abstraction ✅ COMPLETE
- [x] `Backend` interface
- [x] `Frame`
- [x] `Terminal`
- [ ] `Viewport` (fullscreen only, inline/fixed modes TODO)

### Phase 5: JLine Backend ✅ COMPLETE
- [x] `JLineBackend` implementation
- [x] ANSI escape code handling
- [x] Cursor management
- [x] Raw mode handling
- [x] Alternate screen
- [x] Mouse capture support

### Phase 6: Basic Widgets ✅ COMPLETE
- [x] `Widget` and `StatefulWidget` interfaces
- [x] `Block` (borders, titles, padding)
- [x] `Paragraph` (text wrapping, alignment)
- [x] `Clear`

### Phase 7: Interactive Widgets ✅ COMPLETE
- [x] `List` with `ListState`
- [x] `TextInput` with `TextInputState` (JRatatui addition)
- [x] `Table` with `TableState`
- [x] `Tabs` with `TabsState`
- [x] `Scrollbar` with `ScrollbarState`

### Phase 8: Visualization Widgets 🔄 IN PROGRESS
- [x] `Gauge` (progress bar with unicode block characters)
- [x] `LineGauge` (progress as horizontal line)
- [x] `Sparkline`
- [ ] `BarChart`
- [ ] `Chart` (line/scatter)
- [ ] `Canvas` with shapes

### Phase 9: Advanced Features ❌ TODO
- [ ] `Calendar` widget
- [ ] Layout caching (ThreadLocal LRU cache)
- [ ] Viewport modes (inline, fixed)
- [ ] Mouse event handling in widgets

### Phase 10: Unit Testing ✅ PARTIALLY COMPLETE
- [x] Core module tests (Rect, Buffer, Style, Layout, Cell, Span, Line, Text, Margin, Color)
- [x] Widget tests (Block, Paragraph, List, Padding)
- [ ] Backend integration tests (JLineBackend with mocked terminal)
- [ ] Achieve 80%+ code coverage
- [ ] TextInput widget tests (when implemented)

**Test Summary:**
- jratatui-core: 81 tests passing
- jratatui-widgets: 191 tests passing

### Phase 11: Polish & Documentation ❌ TODO
- [ ] Comprehensive Javadoc
- [ ] Example applications
- [ ] README and user guide
- [x] Native image support (GraalVM)
- [ ] Performance optimization

---

## 13. Example Application

```java
package io.github.jratatui.examples;

import io.github.jratatui.backend.jline.JLineBackend;
import io.github.jratatui.layout.*;
import io.github.jratatui.style.*;
import io.github.jratatui.terminal.*;
import io.github.jratatui.text.*;
import io.github.jratatui.widgets.*;
import io.github.jratatui.widgets.block.*;
import io.github.jratatui.widgets.paragraph.*;

import java.io.IOException;

public class HelloWorld {
    public static void main(String[] args) throws Exception {
        try (var backend = new JLineBackend();
             var terminal = new Terminal<>(backend)) {

            terminal.draw(frame -> {
                var greeting = Paragraph.builder()
                    .text(Text.from("Hello, JRatatui!"))
                    .block(Block.bordered()
                        .title(Title.from("Welcome"))
                        .borderStyle(Style.EMPTY.fg(Color.CYAN)))
                    .alignment(Alignment.CENTER)
                    .build();

                frame.renderWidget(greeting, frame.area());
            });

            // Wait for keypress
            System.in.read();
        }
    }
}
```

---

## References

- [Ratatui GitHub Repository](https://github.com/ratatui/ratatui)
- [Ratatui Documentation](https://docs.rs/ratatui/latest/ratatui/)
- [Ratatui Architecture](https://github.com/ratatui/ratatui/blob/main/ARCHITECTURE.md)
- [Ratatui Website](https://ratatui.rs/)
- [JLine 3 GitHub](https://github.com/jline/jline3)
- [Lanterna GitHub](https://github.com/mabe02/lanterna)
- [Jexer Homepage](https://jexer.sourceforge.io/)
- [Cassowary Java Implementation](https://github.com/pybee/cassowary-java)
- [UW Cassowary Toolkit](https://constraints.cs.washington.edu/cassowary/)
