# TamboUI Toolkit Tutorial

This tutorial explains how to build terminal user interfaces (TUIs) using the TamboUI Toolkit module. The toolkit provides a declarative, fluent API for creating interactive terminal applications.

## Table of Contents

1. [Getting Started](#getting-started)
2. [Architecture: Separating State from UI](#architecture-separating-state-from-ui)
3. [The Element Interface](#the-element-interface)
4. [Building UI Elements](#building-ui-elements)
5. [Event Handling and Routing](#event-handling-and-routing)
   - [UI Refresh and Ticking](#ui-refresh-and-ticking)
6. [Focus Management](#focus-management)
7. [Styling](#styling)
8. [Advanced Patterns](#advanced-patterns)
9. [Complete Example](#complete-example)

---

## Getting Started

### Adding the Dependency

```kotlin
// Gradle (Kotlin DSL)
dependencies {
    implementation("dev.tamboui:tamboui-toolkit:VERSION")
}
```

### Essential Imports

```java
import dev.tamboui.toolkit.app.ToolkitRunner;
import dev.tamboui.toolkit.element.Element;
import dev.tamboui.toolkit.event.EventResult;
import dev.tamboui.tui.event.KeyCode;
import dev.tamboui.tui.event.KeyEvent;
import dev.tamboui.widgets.text.Overflow;

import static dev.tamboui.toolkit.Toolkit.*;  // Factory methods
```

---

## Architecture: Separating State from UI

The recommended architecture separates concerns into three layers:

```
┌─────────────┐     reads      ┌─────────────┐
│  Controller │ ◄──────────────│    View     │
│   (State)   │                │  (Element)  │
└─────────────┘                └─────────────┘
       ▲                              │
       │         dispatches           │
       └──────────────────────────────┘
                 events
```

- **Controller**: Holds all application state and provides methods to modify it
- **View**: A pure function that reads state from the controller and returns an `Element`
- **Events**: Dispatched to the controller, which updates state

### The Controller

The controller is a plain Java class that encapsulates your application state:

```java
import dev.tamboui.widgets.input.TextInputState;

public class TodoController {
    private final List<TodoItem> items = new ArrayList<>();
    private final TextInputState inputState = new TextInputState();
    private int selectedIndex = 0;
    private boolean inputMode = false;

    public record TodoItem(String text, boolean done) {}

    // Queries (read state)
    public List<TodoItem> items() { return List.copyOf(items); }
    public int selectedIndex() { return selectedIndex; }
    public TextInputState inputState() { return inputState; }
    public boolean isInputMode() { return inputMode; }

    // Commands (modify state)
    public void moveUp() {
        if (selectedIndex > 0) selectedIndex--;
    }

    public void moveDown() {
        if (selectedIndex < items.size() - 1) {
            selectedIndex++;
        }
    }

    public void toggleSelected() {
        if (!items.isEmpty()) {
            var item = items.get(selectedIndex);
            items.set(selectedIndex, new TodoItem(item.text(), !item.done()));
        }
    }

    public void deleteSelected() {
        if (!items.isEmpty()) {
            items.remove(selectedIndex);
            if (selectedIndex >= items.size() && selectedIndex > 0) {
                selectedIndex--;
            }
        }
    }

    public void startInput() { inputMode = true; }
    public void cancelInput() { inputMode = false; inputState.clear(); }

    public void submitInput() {
        if (inputState.length() > 0) {
            items.add(new TodoItem(inputState.text(), false));
            selectedIndex = items.size() - 1;
        }
        inputMode = false;
        inputState.clear();
    }
}
```

### The View

The view is a pure function that transforms controller state into UI elements:

```java
public class TodoView {
    private final TodoController controller;

    public TodoView(TodoController controller) {
        this.controller = controller;
    }

    public Element render() {
        return panel("Todo List",
            renderList(),
            spacer(),
            renderInput(),
            renderHelp()
        ).rounded().id("main").focusable();
    }

    private Element renderList() {
        var items = controller.items();
        if (items.isEmpty()) {
            return text("No items. Press 'a' to add one.").dim().italic();
        }

        var elements = new Element[items.size()];
        for (int i = 0; i < items.size(); i++) {
            elements[i] = renderItem(i, items.get(i));
        }
        return column(elements);
    }

    private Element renderItem(int index, TodoController.TodoItem item) {
        var checkbox = item.done() ? "[x]" : "[ ]";
        var element = text(checkbox + " " + item.text());

        if (index == controller.selectedIndex()) {
            element = element.reversed();
        }
        if (item.done()) {
            element = element.dim().crossedOut();
        }
        return element;
    }

    private Element renderInput() {
        if (!controller.isInputMode()) {
            return text("");
        }
        return row(
            text("New: ").cyan(),
            textInput(controller.inputState()).fill()
        );
    }

    private Element renderHelp() {
        if (controller.isInputMode()) {
            return text("[Enter] Save  [Esc] Cancel").dim();
        }
        return text("[a] Add  [Space] Toggle  [d] Delete  [j/k] Navigate  [q] Quit").dim();
    }
}
```

### Wiring It Together

The application wires controller, view, and events:

```java
import static dev.tamboui.toolkit.Toolkit.handleTextInputKey;

public class TodoApp {
    public static void main(String[] args) throws Exception {
        var controller = new TodoController();
        var view = new TodoView(controller);

        try (var runner = ToolkitRunner.create()) {
            runner.run(() ->
                view.render()
                    .onKeyEvent(event -> handleEvent(event, controller))
            );
        }
    }

    private static EventResult handleEvent(KeyEvent event, TodoController ctrl) {
        if (ctrl.isInputMode()) {
            return handleInputMode(event, ctrl);
        }
        return handleNormalMode(event, ctrl);
    }

    private static EventResult handleInputMode(KeyEvent event, TodoController ctrl) {
        if (event.isCancel()) {
            ctrl.cancelInput();
            return EventResult.HANDLED;
        }
        if (event.isSelect()) {
            ctrl.submitInput();
            return EventResult.HANDLED;
        }
        // Delegate text editing to the utility (handles chars, backspace, delete, arrows, home/end)
        if (handleTextInputKey(ctrl.inputState(), event)) {
            return EventResult.HANDLED;
        }
        return EventResult.UNHANDLED;
    }

    private static EventResult handleNormalMode(KeyEvent event, TodoController ctrl) {
        if (event.isChar('a')) { ctrl.startInput(); return EventResult.HANDLED; }
        if (event.isUp()) { ctrl.moveUp(); return EventResult.HANDLED; }
        if (event.isDown()) { ctrl.moveDown(); return EventResult.HANDLED; }
        if (event.isSelect()) { ctrl.toggleSelected(); return EventResult.HANDLED; }
        if (event.isChar('d')) { ctrl.deleteSelected(); return EventResult.HANDLED; }
        return EventResult.UNHANDLED;
    }
}
```

### Benefits of This Architecture

1. **Testable**: Controller can be unit tested without any UI
2. **Reusable**: Same controller can power different views
3. **Predictable**: State changes only through controller methods
4. **Debuggable**: Easy to log/inspect state transitions
5. **Maintainable**: Clear separation of concerns

---

## The Element Interface

All UI components implement the `Element` interface. Understanding it helps you build interactive applications.

### How Event Routing Works

The framework automatically tracks all rendered elements. When a key is pressed:

1. The router looks through all rendered elements
2. It calls `handleKeyEvent()` on matching elements
3. Elements return `HANDLED` (stop) or `UNHANDLED` (continue)

### Using Toolkit Elements (Most Applications)

When you use toolkit methods like `panel()`, `text()`, `row()`, etc., just attach event handlers:

```java
panel("My App")
    .id("main")
    .focusable()
    .onKeyEvent(event -> {
        if (event.isUp()) {
            controller.moveUp();
            return EventResult.HANDLED;
        }
        return EventResult.UNHANDLED;
    })
```

This is sufficient for most applications.

### Implementing Element Directly (Custom Root Elements)

Sometimes you need a custom root element that:
- Composes multiple toolkit elements
- Renders overlays (dialogs) on top of the main UI
- Has complex rendering logic

In this case, implement `Element` directly:

```java
public class MyView implements Element {
    private final MyController controller;

    @Override
    public void render(Frame frame, Rect area, RenderContext context) {
        // Compose and render toolkit elements
        var ui = column(
            header(),
            mainContent()
        );
        ui.render(frame, area, context);

        // Render dialog on top if needed
        if (controller.hasDialog()) {
            renderDialog(frame, area, context);
        }
    }

    @Override
    public Constraint constraint() {
        return Constraint.fill();
    }

    @Override
    public EventResult handleKeyEvent(KeyEvent event, boolean focused) {
        // Handle events for the entire view
        if (event.isCancel() && controller.hasDialog()) {
            controller.dismissDialog();
            return EventResult.HANDLED;
        }
        // ... more handling
        return EventResult.UNHANDLED;
    }
}
```

### Why Implement Element Directly?

The main reason is **dialogs and overlays**. When you have:

```java
runner.run(() ->
    panel("App").onKeyEvent(e -> handleKey(e))
);
```

The panel receives events. But if you show a dialog, how does the dialog intercept ESC? The panel's handler would need complex logic to know about dialogs.

By implementing `Element` directly as a root view:

```java
runner.run(() -> myView);  // myView implements Element
```

Your `handleKeyEvent()` receives ALL events for the entire view. You can check for dialogs, route to sub-handlers, etc. The toolkit elements you compose inside don't need `.focusable()` or `.onKeyEvent()` - you handle everything centrally.

### Quick Reference

| Approach | When to Use |
|----------|-------------|
| Toolkit + `.onKeyEvent()` | Single panel apps, simple event handling |
| Implement `Element` | Dialogs, overlays, complex multi-panel layouts |

---

## Building UI Elements

### Panels

The primary container for grouping content:

```java
// With title
panel("Title", child1, child2)

// With dynamic content (supplier evaluated each frame)
panel("Dynamic", () -> text("Value: " + controller.value()))

// Styling
panel("Styled")
    .rounded()          // Border style: rounded, doubleBorder(), thick(), plain()
    .borderColor(Color.GRAY)
    .focusedBorderColor(Color.CYAN)
```

### Layout Containers

```java
// Horizontal layout
row(
    text("Left"),
    spacer(),           // Flexible space
    text("Right")
).spacing(1)            // Gap between children

// Vertical layout
column(
    text("Top"),
    text("Bottom")
).spacing(1)
```

### Text

```java
text("Plain text")
text("Styled").bold().cyan()
text("Multiple styles").bold().italic().underlined()
```

### Text Overflow

When text is too long for the available space, you can control how it's handled:

```java
// Truncate with ellipsis at end: "Long text..."
text(filename).ellipsis()

// Truncate with ellipsis at start: "...ong path" (useful for paths)
text(path).ellipsisStart()

// Truncate with ellipsis in middle: "Long...text"
text(value).ellipsisMiddle()

// Or use the Overflow enum directly
text(content).overflow(Overflow.ELLIPSIS)
```

Available overflow modes:
- `CLIP` - Silent truncation (default)
- `WRAP_CHARACTER` - Wrap at character boundaries
- `WRAP_WORD` - Wrap at word boundaries
- `ELLIPSIS` - Truncate with "..." at end
- `ELLIPSIS_START` - Truncate with "..." at start
- `ELLIPSIS_MIDDLE` - Truncate with "..." in middle

Panel titles also support overflow:

```java
// Title will show "...ong/path/to/file" if too long
panel(longPath, content)
    .titleEllipsisStart()

// Title will show "Long title..." if too long
panel(longTitle, content)
    .titleEllipsis()
```

**Note:** You don't need to manually calculate widths or truncate strings. Just declare the overflow behavior and the framework handles it automatically during rendering.

### Sizing with Constraints

```java
panel("Fixed").length(20)           // Fixed 20 characters
panel("Percentage").percentage(50)  // 50% of available space
panel("Flexible").fill()            // Fill remaining space
panel("Weighted").fill(2)           // Fill with weight 2
panel("Bounded").min(10).max(50)    // Constrained size
```

### Complex Layouts

```java
column(
    // Header bar
    panel("App Title").length(3),

    // Main content area
    row(
        panel("Sidebar").percentage(30),
        panel("Main Content").fill()
    ).fill(),

    // Status bar
    text("Ready").length(1)
)
```

### Toolkit Method Reference

The `Toolkit` class provides these static factory methods (use `import static dev.tamboui.toolkit.Toolkit.*;`):

**Core Layout:**

| Method                      | Description                    |
|-----------------------------|--------------------------------|
| `text(content)`             | Text element with styling      |
| `panel(title, children...)` | Bordered container with title  |
| `row(children...)`          | Horizontal layout              |
| `column(children...)`       | Vertical layout                |
| `spacer()`                  | Flexible space filler          |

**Constraints:**

| Method               | Description                            |
|----------------------|----------------------------------------|
| `length(n)`          | Fixed size of n characters             |
| `percent(n)`         | n% of available space                  |
| `fill()`             | Fill remaining space                   |
| `fill(weight)`       | Fill with relative weight              |
| `min(n)`             | Minimum size constraint                |
| `max(n)`             | Maximum size constraint                |
| `ratio(num, denom)`  | Fractional size (e.g., `ratio(1, 3)`)  |

**Data Visualization:**

| Method               | Description                    |
|----------------------|--------------------------------|
| `gauge(ratio)`       | Progress bar (0.0 to 1.0)      |
| `lineGauge(ratio)`   | Compact line-style gauge       |
| `sparkline(data...)` | Mini chart from data points    |
| `barChart(values...)` | Bar chart                     |
| `chart()`            | XY line/scatter chart          |

**Input & Lists:**

| Method               | Description           |
|----------------------|-----------------------|
| `list(items...)`     | Scrollable list       |
| `table()`            | Data table            |
| `tabs(titles...)`    | Tab bar               |
| `textInput(state)`   | Text input field      |

### List with Data and Item Renderer

For lists backed by domain objects, use `data()` with an item renderer:

```java
// File list with custom rendering
list()
    .data(files, file -> ListItem.from(
        file.isDirectory() ? file.name() + "/" : file.name()
    ))
    .state(listState)
    .autoScroll()
    .highlightColor(Color.CYAN)
```

**Key methods:**

| Method                         | Description                              |
|--------------------------------|------------------------------------------|
| `data(list, renderer)`         | Set data and conversion function         |
| `itemRenderer(fn)`             | Set item renderer separately             |
| `state(listState)`             | Bind ListState for selection tracking    |
| `autoScroll()`                 | Auto-scroll to keep selection visible    |
| `highlightStyle(style)`        | Style for selected item                  |
| `highlightSymbol(s)`           | Prefix for selected item (default: ">> ")|

**ListState** tracks selection and scroll position:
```java
var listState = new ListState();
listState.selectFirst();
listState.selectNext(items.size());
listState.selectPrevious();
listState.select(index);
```

When `autoScroll()` is enabled, the list automatically calls `listState.scrollToSelected()` before rendering to keep the selected item visible.

### Text Input Handling

The toolkit provides utilities for building text input features:

**TextInputState** - Manages text and cursor position:
```java
var inputState = new TextInputState();          // Empty
var inputState = new TextInputState("initial"); // With initial text

inputState.text();           // Get current text
inputState.cursorPosition(); // Get cursor position
inputState.length();         // Get text length
inputState.clear();          // Clear text and reset cursor
inputState.setText("new");   // Set text (cursor stays in bounds)
```

**TextInputElement** - Renders the input with cursor and handles key events automatically:
```java
textInput(inputState)
    .placeholder("Enter name...")
    .title("Name")
    .rounded()
    .borderColor(Color.CYAN)
```

`TextInputElement` implements `handleKeyEvent()`, so when the element is focused, it automatically handles arrow keys, home/end, backspace, delete, and character input. This is the recommended approach when using the toolkit's focus system.

**handleTextInputKey utility** - For edge cases without element-based routing:
```java
import static dev.tamboui.toolkit.Toolkit.handleTextInputKey;

// In a custom key handler (rarely needed)
if (handleTextInputKey(inputState, event)) {
    return EventResult.HANDLED;
}
```

This utility is rarely needed since `TextInputElement` handles events automatically, and `DialogElement` routes events to its children. Use it only for edge cases where you can't use element-based event routing.

### Dialogs

The `dialog()` element simplifies creating modal dialogs by automatically centering, clearing the background, and handling key events:

```java
// Modal input dialog with callbacks
var inputDialog = dialog("New Directory",
    text("Enter name:"),
    textInput(inputState),
    text("[Enter] Confirm  [Esc] Cancel").dim()
).rounded()
 .borderColor(Color.CYAN)
 .width(50)
 .onConfirm(() -> createDirectory(inputState.text()))
 .onCancel(() -> dismissDialog());

// Render the dialog
inputDialog.render(frame, area, context);

// Route key events to the dialog (in handleKeyEvent)
if (inputDialog != null) {
    return inputDialog.handleKeyEvent(event, true);
}
```

The dialog handles Enter for confirm, Escape for cancel, and routes other events to its children (e.g., `TextInputElement` handles text input automatically).

**DialogElement methods:**

| Method             | Description                           |
|--------------------|---------------------------------------|
| `title(s)`         | Set dialog title                      |
| `rounded()`        | Use rounded border                    |
| `doubleBorder()`   | Use double-line border                |
| `borderColor(c)`   | Set border color                      |
| `width(n)`         | Set fixed width                       |
| `height(n)`        | Set fixed height                      |
| `minWidth(n)`      | Set minimum width (default: 20)       |
| `onConfirm(fn)`    | Callback for Enter key                |
| `onCancel(fn)`     | Callback for Escape key               |
| `add(element...)`  | Add child elements                    |

**Other:**

| Method               | Description              |
|----------------------|--------------------------|
| `calendar(date)`     | Calendar widget          |
| `canvas(bounds)`     | Drawing canvas           |
| `scrollbar(state)`   | Scrollbar indicator      |
| `lazy(supplier)`     | Lazily evaluated element |

---

## Event Handling and Routing

This section explains how key events flow through the framework and how your application handles them.

### Framework Behavior (Built-in)

The framework handles these keys automatically - you don't need to write any code for them:

| Key | Framework Action |
|-----|------------------|
| **Tab** | Move focus to next focusable element |
| **Shift+Tab** | Move focus to previous focusable element |
| **ESC** | Clear focus (only if no element handles it) |
| **q / Ctrl+C** | Quit the application |

### Application Behavior (Your Code)

Your elements receive key events via the `handleKeyEvent` method. The framework calls your code like this:

1. **Focused element first**: If an element has focus, it receives the event with `focused=true`
2. **All elements second**: If not handled, all elements receive it with `focused=false`

```
Key pressed
    │
    ▼
┌─────────────────────────────────────────────────────┐
│ FRAMEWORK: Tab/Shift+Tab? → Navigate focus, stop   │
└─────────────────────────────────────────────────────┘
    │ no
    ▼
┌─────────────────────────────────────────────────────┐
│ YOUR CODE: Focused element receives event          │
│            handleKeyEvent(event, focused=true)     │
│            Return HANDLED to stop, UNHANDLED to    │
│            continue                                │
└─────────────────────────────────────────────────────┘
    │ UNHANDLED
    ▼
┌─────────────────────────────────────────────────────┐
│ YOUR CODE: All other elements receive event        │
│            handleKeyEvent(event, focused=false)    │
│            Return HANDLED to stop                  │
└─────────────────────────────────────────────────────┘
    │ UNHANDLED
    ▼
┌─────────────────────────────────────────────────────┐
│ FRAMEWORK: ESC? → Clear focus                      │
└─────────────────────────────────────────────────────┘
```

### Writing Event Handlers

Return `HANDLED` to consume the event, `UNHANDLED` to let others process it:

```java
.onKeyEvent(event -> {
    if (event.isUp()) {
        controller.moveUp();
        return EventResult.HANDLED;   // Event consumed, stop here
    }
    return EventResult.UNHANDLED;     // Let framework/other elements handle
})
```

### The `focused` Parameter

When implementing `Element.handleKeyEvent()`, the `focused` parameter tells you if your element has focus:

```java
@Override
public EventResult handleKeyEvent(KeyEvent event, boolean focused) {
    // Always handle ESC for dialogs (regardless of focus)
    if (event.isCancel() && controller.hasDialog()) {
        controller.dismissDialog();
        return EventResult.HANDLED;
    }

    // Navigation only when we "own" the UI
    if (event.isUp()) {
        controller.moveUp();
        return EventResult.HANDLED;
    }

    return EventResult.UNHANDLED;
}
```

**Note:** When implementing `Element` directly as a root view, you typically ignore the `focused` parameter and handle all events. The parameter is more useful for individual focusable panels within a larger UI.

### Key Utilities

KeyEvent provides convenience methods that delegate to the configured `KeyMap`. The default keymap (`standard`) uses only arrow keys:

| Method | Keys Matched (Standard) |
|--------|-------------------------|
| `event.isQuit()` | q, Q, Ctrl+C |
| `event.isUp()` | Up arrow |
| `event.isDown()` | Down arrow |
| `event.isLeft()` | Left arrow |
| `event.isRight()` | Right arrow |
| `event.isHome()` | Home |
| `event.isEnd()` | End |
| `event.isPageUp()` | Page Up |
| `event.isPageDown()` | Page Down |
| `event.isSelect()` | Enter, Space |
| `event.isConfirm()` | Enter only |
| `event.isCancel()` | Escape |
| `event.isFocusNext()` | Tab |
| `event.isFocusPrevious()` | Shift+Tab |
| `event.isChar('x')` | Specific character (case-sensitive) |
| `event.isCharIgnoreCase('x')` | Specific character (case-insensitive) |
| `event.isKey(KeyCode.F5)` | Specific key code |

### Configurable Keymaps

You can configure different keymaps via `TuiConfig`:

```java
import dev.tamboui.tui.keymap.KeyMaps;

TuiConfig config = TuiConfig.builder()
    .keyMap(KeyMaps.vim())  // Enable vim-style navigation
    .build();
```

Available keymaps:
- `KeyMaps.standard()` - Arrow keys only (default, safe for text input)
- `KeyMaps.vim()` - hjkl navigation, g/G for home/end, Ctrl+u/d for page navigation
- `KeyMaps.emacs()` - Ctrl+n/p/f/b navigation
- `KeyMaps.intellij()` - IntelliJ IDEA-style bindings
- `KeyMaps.vscode()` - VS Code-style bindings

You can also create custom keymaps:

```java
import dev.tamboui.tui.keymap.*;

KeyMap custom = KeyMaps.standard()
    .toBuilder()
    .bind(Action.QUIT, KeyBinding.ch('x'))  // Quit with 'x' instead of 'q'
    .build();
```

### Loading Keymaps from Properties Files

You can load keymaps from standard Java properties files:

```properties
# my-keymap.properties
MOVE_UP = Up, k, K
MOVE_DOWN = Down, j, J
CONFIRM = Enter
CANCEL = Escape
QUIT = q, Ctrl+c
```

Load from a file or classpath resource:

```java
// From filesystem
KeyMap keymap = KeyMaps.load(Path.of("/path/to/keymap.properties"));

// From classpath resource
KeyMap keymap = KeyMaps.loadResource("/keymaps/custom.properties");
```

Binding syntax supports:
- Key names: `Up`, `Down`, `Enter`, `Tab`, `Escape`, `Backspace`, `Delete`, `Home`, `End`, `PageUp`, `PageDown`, `F1`-`F12`
- Characters: Single character like `k`, `q`
- Modifiers: `Ctrl+c`, `Alt+x`, `Shift+Tab`
- Space: Use the word `Space`

### Checking Key Codes Directly

For keys not covered by the keymap convenience methods, check the key code directly:

```java
import dev.tamboui.tui.event.KeyCode;

if (event.code() == KeyCode.F5) {
    controller.copy();
    return EventResult.HANDLED;
}
if (event.code() == KeyCode.BACKSPACE) {
    controller.deleteChar();
    return EventResult.HANDLED;
}
```

### Handling Character Input

For text input, check for printable characters:

```java
if (event.code() == KeyCode.CHAR && event.character() >= 32 && event.character() < 127) {
    controller.appendChar(event.character());
    return EventResult.HANDLED;
}
```

### Mouse Events

Enable mouse capture in configuration:

```java
var config = TuiConfig.builder()
    .mouseCapture(true)
    .build();

try (var runner = ToolkitRunner.create(config)) {
    runner.run(() ->
        panel("Clickable")
            .onMouseEvent(event -> {
                controller.handleClick(event.x(), event.y());
                return EventResult.HANDLED;
            })
    );
}
```

### Drag Events

```java
panel("Draggable")
    .draggable((deltaX, deltaY) -> {
        controller.move(deltaX, deltaY);
    })
```

### UI Refresh and Ticking

By default, TamboUI refreshes the UI every 100ms via **tick events**. This ensures animations, clocks, and live data update smoothly without requiring user input.

#### When Ticking is Useful

- **Animations**: Progress bars, spinners, visual transitions
- **Live data**: System monitors, log viewers, real-time dashboards
- **Clocks**: Any time-based display
- **Background operations**: Polling for file changes, network updates

#### When to Disable Ticking

Use `noTick()` for purely **event-driven** applications where the UI only needs to update in response to user input:

```java
var config = TuiConfig.builder()
    .noTick()  // Disable automatic ticking
    .build();

try (var runner = ToolkitRunner.create(config)) {
    runner.run(() -> view.render());
}
```

Good candidates for `noTick()`:
- **Text editors**: Only redraw when user types or navigates
- **Form-based UIs**: Only redraw when fields change
- **Static displays**: Menus, help screens, confirmation dialogs

Benefits of disabling ticks:
- Lower CPU usage (no periodic wake-ups)
- No unnecessary redraws when nothing changes

#### Custom Tick Rates

For applications that need updates but not as frequently as the default 100ms:

```java
var config = TuiConfig.builder()
    .tickRate(Duration.ofMillis(500))  // Update every 500ms
    .build();
```

Common tick rates:

| Rate | Use Case |
| --- | --- |
| 16ms (~60 FPS) | Smooth animations |
| 50ms (20 FPS) | Responsive UI with moderate updates |
| 100ms (default) | General-purpose applications |
| 250-500ms | Dashboards with infrequent updates |
| 1000ms+ | Slow-updating status displays |

#### How Refresh Works Without Ticking

When ticking is disabled, the UI still refreshes automatically after each event is handled. No manual intervention is required:

```java
var config = TuiConfig.builder().noTick().build();

try (var runner = ToolkitRunner.create(config)) {
    runner.run(() ->
        panel("Editor", text(controller.content()))
            .onKeyEvent(event -> {
                if (event.code() == KeyCode.CHAR) {
                    controller.insertChar(event.character());
                    return EventResult.HANDLED;  // UI refreshes automatically
                }
                return EventResult.UNHANDLED;
            })
    );
}
```

The flow is:
1. User presses a key
2. Your handler updates the controller state
3. You return `EventResult.HANDLED`
4. The framework calls your render function and redraws the UI

This is efficient because the UI only redraws when something actually changes, rather than polling every 100ms.

**Note:** For most applications, the default ticking behavior is sufficient. Only disable ticking if you have a specific reason to do so (e.g., minimizing CPU usage for simple, static UIs).

---

## Focus Management

Focus determines which element receives keyboard events first. The `FocusManager` tracks the currently focused element and handles focus navigation.

### How Focus Works

1. **Focused element receives events first** with `focused=true`
2. **Other elements receive events second** with `focused=false` (for global hotkeys)
3. **Tab/Shift+Tab** navigates between focusable elements
4. **ESC clears focus** (but only if no element handles it first)
5. **Clicking** on a focusable element focuses it

### Making Elements Focusable

Both `id()` AND `focusable()` are required:

```java
panel("Panel A")
    .id("panel-a")          // Unique identifier (REQUIRED)
    .focusable()            // Enable focus (REQUIRED)
    .borderColor(Color.GRAY)
    .focusedBorderColor(Color.CYAN)  // Visual feedback when focused
```

### Focus Navigation Keys

| Key | Action |
|-----|--------|
| Tab | Focus next element |
| Shift+Tab | Focus previous element |
| Mouse click | Focus clicked element |
| ESC | Clear focus (if no element handles it) |

### Focus Order

Elements are focused in the order they are registered during rendering. This typically matches the visual order (top-to-bottom, left-to-right).

### Multi-Panel Applications

```java
column(
    panel("Settings")
        .id("settings")
        .focusable()
        .onKeyEvent(event -> handleSettingsKey(event, controller)),

    panel("Actions")
        .id("actions")
        .focusable()
        .onKeyEvent(event -> handleActionsKey(event, controller))
)
```

Each panel handles its own keys when focused. Tab switches between them.

### Focus and Dialogs

When showing dialogs, you may want to handle events at the root level rather than relying on focus. Implement `Element` directly:

```java
public class MyView implements Element {
    @Override
    public EventResult handleKeyEvent(KeyEvent event, boolean focused) {
        // ESC should dismiss dialog, not clear focus
        if (event.isCancel() && controller.hasDialog()) {
            controller.dismissDialog();
            return EventResult.HANDLED;  // Prevents ESC from clearing focus
        }
        // ... other handling
    }
}
```

**Key insight:** ESC is routed to elements first. If you return `HANDLED`, the focus won't be cleared. If you return `UNHANDLED`, the EventRouter will clear focus as a fallback.

---

## Styling

### Colors

```java
// Foreground
text("Colored").red().green().blue().yellow().cyan().magenta().white().gray()
text("Custom").fg(Color.rgb(255, 128, 0))

// Background
text("Highlighted").onRed().onBlue()
text("Custom bg").bg(Color.rgb(50, 50, 50))
```

### Modifiers

```java
text("Bold").bold()
text("Dim").dim()
text("Italic").italic()
text("Underlined").underlined()
text("Reversed").reversed()
text("Strikethrough").crossedOut()

// Combine
text("Combined").bold().cyan().underlined()
```

### Reusable Styles

```java
public class AppStyles {
    public static final Style HEADER = Style.EMPTY.fg(Color.CYAN).bold();
    public static final Style MUTED = Style.EMPTY.fg(Color.GRAY).dim();
    public static final Style ERROR = Style.EMPTY.fg(Color.RED).bold();
}

// Usage
text("Title").style(AppStyles.HEADER)
text("Info").style(AppStyles.MUTED)
```

---

## Advanced Patterns

This section covers patterns for building complex applications like file managers, editors, and multi-panel tools.

### Pattern: Separate Key Handler Class

For complex applications, extract key handling into a dedicated class:

```java
public class MyKeyHandler {
    private final MyController controller;

    public MyKeyHandler(MyController controller) {
        this.controller = controller;
    }

    public EventResult handle(KeyEvent event) {
        // Check ESC first for dialogs
        if (event.isCancel() && controller.hasDialog()) {
            controller.dismissDialog();
            return EventResult.HANDLED;
        }

        // Route to appropriate handler based on state
        if (controller.isInputMode()) {
            return handleInputMode(event);
        }
        if (controller.hasDialog()) {
            return handleDialogMode(event);
        }
        return handleNormalMode(event);
    }

    private EventResult handleInputMode(KeyEvent event) {
        if (event.isCancel()) {
            controller.cancelInput();
            return EventResult.HANDLED;
        }
        if (event.isConfirm()) {
            controller.submitInput();
            return EventResult.HANDLED;
        }
        if (event.code() == KeyCode.BACKSPACE) {
            controller.deleteChar();
            return EventResult.HANDLED;
        }
        if (event.code() == KeyCode.CHAR && event.character() >= 32 && event.character() < 127) {
            controller.appendChar(event.character());
            return EventResult.HANDLED;
        }
        return EventResult.HANDLED; // Consume all keys in input mode
    }

    private EventResult handleDialogMode(KeyEvent event) {
        if (event.isCharIgnoreCase('y')) {
            controller.confirmDialog();
            return EventResult.HANDLED;
        }
        if (event.isCharIgnoreCase('n')) {
            controller.dismissDialog();
            return EventResult.HANDLED;
        }
        return EventResult.UNHANDLED;
    }

    private EventResult handleNormalMode(KeyEvent event) {
        // Navigation, actions, etc.
        if (event.isUp()) {
            controller.moveUp();
            return EventResult.HANDLED;
        }
        // ... more handlers
        return EventResult.UNHANDLED;
    }
}
```

### Pattern: Rendering Dialogs and Overlays

Dialogs should be rendered on top of the main UI. Use `Clear` to ensure proper background:

```java
import dev.tamboui.widgets.Clear;

@Override
public void render(Frame frame, Rect area, RenderContext context) {
    // 1. Render main UI
    var ui = column(header(), content(), footer());
    ui.render(frame, area, context);

    // 2. Render dialog on top (if present)
    if (controller.hasDialog()) {
        renderDialog(frame, area, context);
    }
}

private void renderDialog(Frame frame, Rect area, RenderContext context) {
    // Calculate centered dialog position
    int dialogWidth = 50;
    int dialogHeight = 6;
    int x = (area.width() - dialogWidth) / 2;
    int y = (area.height() - dialogHeight) / 2;
    var dialogArea = new Rect(area.x() + x, area.y() + y, dialogWidth, dialogHeight);

    // Clear the area first (prevents transparency issues)
    frame.renderWidget(Clear.INSTANCE, dialogArea);

    // Render the dialog panel
    var dialog = panel("Confirm",
        text(controller.dialogMessage()),
        text(""),
        text("[y] Yes  [n] No  [Esc] Cancel").dim()
    ).rounded().borderColor(Color.YELLOW);

    dialog.render(frame, dialogArea, context);
}
```

### Pattern: Input Dialogs

For dialogs that accept text input:

```java
public class Controller {
    public enum DialogType { NONE, CONFIRM, INPUT, ERROR }

    private final StringBuilder inputBuffer = new StringBuilder();
    private DialogType dialogType = DialogType.NONE;

    public boolean isInputDialog() {
        return dialogType == DialogType.INPUT;
    }

    public String inputBuffer() {
        return inputBuffer.toString();
    }

    public void promptInput(String prompt) {
        dialogType = DialogType.INPUT;
        inputBuffer.setLength(0);
    }

    public void appendChar(char c) {
        inputBuffer.append(c);
    }

    public void deleteChar() {
        if (inputBuffer.length() > 0) {
            inputBuffer.setLength(inputBuffer.length() - 1);
        }
    }

    public void submitInput() {
        // Process inputBuffer
        doSomethingWith(inputBuffer.toString());
        dialogType = DialogType.NONE;
        inputBuffer.setLength(0);
    }
}
```

Rendering the input dialog:

```java
private void renderInputDialog(Frame frame, Rect area, RenderContext context) {
    var input = controller.inputBuffer();
    var inputDisplay = input + "_";  // Show cursor

    int dialogWidth = Math.max(50, inputDisplay.length() + 10);
    // ... calculate position ...

    frame.renderWidget(Clear.INSTANCE, dialogArea);

    var dialog = panel("Enter Name",
        text(controller.dialogPrompt()),
        text(""),
        text(inputDisplay).cyan(),
        text("[Enter] Confirm  [Esc] Cancel").dim()
    ).rounded().borderColor(Color.CYAN);

    dialog.render(frame, dialogArea, context);
}
```

### Pattern: Two-Panel Layout (File Manager Style)

```java
public class TwoPanelView implements Element {
    private final TwoPanelController controller;

    @Override
    public void render(Frame frame, Rect area, RenderContext context) {
        var ui = column(
            headerBar(),
            row(
                leftPanel().fill(),
                rightPanel().fill()
            ).fill(),
            statusBar()
        );
        ui.render(frame, area, context);
    }

    private Element leftPanel() {
        var active = controller.activeSide() == Side.LEFT;
        return panel(controller.leftTitle(),
            renderContent(controller.leftItems(), controller.leftCursor())
        )
        .rounded()
        .borderColor(active ? Color.CYAN : Color.DARK_GRAY)  // Active panel highlight
        .id("left");  // No focusable() - we handle events at root level
    }

    private Element rightPanel() {
        var active = controller.activeSide() == Side.RIGHT;
        return panel(controller.rightTitle(),
            renderContent(controller.rightItems(), controller.rightCursor())
        )
        .rounded()
        .borderColor(active ? Color.CYAN : Color.DARK_GRAY)
        .id("right");
    }
}
```

**Key insight:** For applications where you manage your own "active panel" state, don't use `.focusable()` on the panels. Instead, handle all events at the root `Element` level and use visual cues (border color) to show which panel is active.

### Pattern: Running the Application

```java
public class MyApp {
    public static void main(String[] args) throws Exception {
        var controller = new MyController();
        var view = new MyView(controller);

        var config = TuiConfig.builder()
            .tickRate(Duration.ofMillis(50))  // 20 FPS
            .build();

        try (var runner = ToolkitRunner.create(config)) {
            runner.run(() -> view);  // View implements Element
        }
    }
}
```

### Common Pitfalls

1. **Using focusable panels with custom event handling**: ESC will clear focus unexpectedly
2. **Not handling ESC before other keys**: Dialogs won't dismiss properly
3. **Transparent dialog backgrounds**: Use `Clear.INSTANCE` before rendering overlays
4. **Returning UNHANDLED in input mode**: Unwanted characters may leak through
5. **Key binding conflicts**: With the `vim` keymap, `event.isUp()` etc. match vim keys (hjkl). Use the `standard` keymap (default) or check `event.code()` directly for precise control

---

## Complete Example

Here's a complete, well-structured application:

```java
import dev.tamboui.toolkit.app.ToolkitRunner;
import dev.tamboui.toolkit.element.Element;
import dev.tamboui.toolkit.event.EventResult;
import dev.tamboui.style.Color;
import dev.tamboui.tui.TuiConfig;
import dev.tamboui.tui.event.KeyCode;
import dev.tamboui.tui.event.KeyEvent;

import java.util.ArrayList;
import java.util.List;

import static dev.tamboui.toolkit.Toolkit.*;

public class TodoApp {

    // ═══════════════════════════════════════════════════════════════
    // CONTROLLER - Application state and business logic
    // ═══════════════════════════════════════════════════════════════

    static class Controller {
        public record Item(String text, boolean done) {}

        private final List<Item> items = new ArrayList<>();
        private final StringBuilder input = new StringBuilder();
        private int cursor = 0;
        private boolean editing = false;

        // Queries
        public List<Item> items() { return List.copyOf(items); }
        public int cursor() { return cursor; }
        public String input() { return input.toString(); }
        public boolean isEditing() { return editing; }
        public boolean isEmpty() { return items.isEmpty(); }

        // Commands
        public void cursorUp() { if (cursor > 0) cursor--; }
        public void cursorDown() { if (cursor < items.size() - 1) cursor++; }

        public void toggle() {
            if (!items.isEmpty()) {
                var item = items.get(cursor);
                items.set(cursor, new Item(item.text(), !item.done()));
            }
        }

        public void delete() {
            if (!items.isEmpty()) {
                items.remove(cursor);
                if (cursor >= items.size() && cursor > 0) cursor--;
            }
        }

        public void startEditing() { editing = true; }
        public void cancelEditing() { editing = false; input.setLength(0); }
        public void typeChar(char c) { input.append(c); }
        public void backspace() {
            if (input.length() > 0) input.setLength(input.length() - 1);
        }

        public void submit() {
            if (input.length() > 0) {
                items.add(new Item(input.toString(), false));
                cursor = items.size() - 1;
            }
            editing = false;
            input.setLength(0);
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // VIEW - Renders UI from controller state
    // ═══════════════════════════════════════════════════════════════

    static class View {
        private final Controller ctrl;

        View(Controller ctrl) { this.ctrl = ctrl; }

        Element render() {
            return panel("Todo List",
                ctrl.isEmpty() ? emptyState() : itemList(),
                spacer(),
                inputArea(),
                helpBar()
            )
            .rounded()
            .borderColor(Color.DARK_GRAY)
            .focusedBorderColor(Color.CYAN)
            .id("main")
            .focusable();
        }

        private Element emptyState() {
            return text("No items yet. Press 'a' to add one.").dim().italic();
        }

        private Element itemList() {
            var items = ctrl.items();
            var elements = new Element[items.size()];
            for (int i = 0; i < items.size(); i++) {
                elements[i] = itemRow(i, items.get(i));
            }
            return column(elements);
        }

        private Element itemRow(int index, Controller.Item item) {
            var prefix = item.done() ? "[x] " : "[ ] ";
            var elem = text(prefix + item.text());

            if (index == ctrl.cursor()) {
                elem = elem.reversed();
            }
            if (item.done()) {
                elem = elem.dim().crossedOut();
            }

            return elem;
        }

        private Element inputArea() {
            if (!ctrl.isEditing()) {
                return text("");
            }
            return row(
                text("New: ").cyan(),
                text(ctrl.input() + "_").bold()
            );
        }

        private Element helpBar() {
            var help = ctrl.isEditing()
                ? "[Enter] Save  [Esc] Cancel"
                : "[a] Add  [Space] Toggle  [d] Delete  [j/k] Move  [q] Quit";
            return text(help).dim();
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // EVENT HANDLER - Routes events to controller
    // ═══════════════════════════════════════════════════════════════

    static EventResult handleKey(KeyEvent event, Controller ctrl) {
        if (ctrl.isEditing()) {
            if (event.isCancel()) { ctrl.cancelEditing(); return EventResult.HANDLED; }
            if (event.isSelect()) { ctrl.submit(); return EventResult.HANDLED; }
            if (event.code() == KeyCode.CHAR && event.character() >= 32) { ctrl.typeChar(event.character()); return EventResult.HANDLED; }
            if (event.code() == KeyCode.BACKSPACE) { ctrl.backspace(); return EventResult.HANDLED; }
            return EventResult.UNHANDLED;
        }

        if (event.isChar('a')) { ctrl.startEditing(); return EventResult.HANDLED; }
        if (event.isUp()) { ctrl.cursorUp(); return EventResult.HANDLED; }
        if (event.isDown()) { ctrl.cursorDown(); return EventResult.HANDLED; }
        if (event.isSelect()) { ctrl.toggle(); return EventResult.HANDLED; }
        if (event.isChar('d')) { ctrl.delete(); return EventResult.HANDLED; }
        return EventResult.UNHANDLED;
    }

    // ═══════════════════════════════════════════════════════════════
    // MAIN - Wire everything together
    // ═══════════════════════════════════════════════════════════════

    public static void main(String[] args) throws Exception {
        var controller = new Controller();
        var view = new View(controller);

        // Add sample data
        controller.items.add(new Controller.Item("Learn TamboUI", false));
        controller.items.add(new Controller.Item("Build awesome apps", false));

        var config = TuiConfig.builder().build();

        try (var runner = ToolkitRunner.create(config)) {
            runner.run(() ->
                view.render()
                    .onKeyEvent(e -> handleKey(e, controller))
            );
        }
    }
}
```

### Running the Example

```bash
# If using JBang, add the following at the top of TodoApp.java:

///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS dev.tamboui:tamboui-toolkit:LATEST
//DEPS dev.tamboui:tamboui-jline:LATEST

Then run with:
jbang TodoApp.java

# If with Gradle, the `run` command will not support running a CLI application directly.
# The easiest is to create a distribution and install it:
./gradlew installDist

Then run the installed application:
build/install/todo-app/bin/todo-app

# TODO: Maven instructions (depends on the project setup)
```

---

## Summary

### Core Concepts

| Concept | Description |
|---------|-------------|
| **Controller** | Plain Java class holding state and mutation methods |
| **View** | Pure function: `Controller → Element` |
| **Element** | Interface with `render()`, `constraint()`, and optional event handlers |
| **EventRouter** | Routes events to focused element first, then to all elements |
| **FocusManager** | Tracks focused element, handles Tab navigation |

### Event Handling

| Concept | Description |
|---------|-------------|
| **EventResult.HANDLED** | Event was processed, stop propagation |
| **EventResult.UNHANDLED** | Event was not processed, continue routing |
| **handleKeyEvent(event, focused)** | `focused=true` if element has focus |

### UI Building

| Concept | Description |
|---------|-------------|
| **Panel** | Bordered container with optional title |
| **Row/Column** | Horizontal/vertical layout containers |
| **Spacer** | Flexible space that fills available room |
| **Constraints** | `length()`, `percentage()`, `fill()`, `min()`, `max()` |
| **Overflow** | `ellipsis()`, `ellipsisStart()`, `ellipsisMiddle()` for text truncation |
| **Focus** | `id()` + `focusable()` enables Tab navigation |
| **Styling** | Chain methods: `text("Hi").bold().cyan()` |

### Key Principles

1. **UI is a function of state**: The view reads from the controller each frame
2. **Unidirectional data flow**: Events → Controller → State → View
3. **Handle ESC early**: Check for ESC before other keys when you have dialogs
4. **Clear before overlays**: Use `Clear.INSTANCE` before rendering dialogs