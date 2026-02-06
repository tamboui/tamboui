# TamboUI

[![Release](https://img.shields.io/github/release/tamboui/tamboui.svg)](https://github.com/tamboui/tamboui/releases)
[![Downloads](https://img.shields.io/github/downloads/tamboui/tamboui/total.svg)](https://hanadigital.github.io/grev/?user=jbangdev&repo=jbang)
[![Build Status](https://github.com/tamboui/tamboui/workflows/Tests/badge.svg)](https://github.com/tamboui/tamboui/actions)
[![Chat](https://img.shields.io/badge/zulip-join_chat-brightgreen.svg)](https://tamboui.zulipchat.com)
[![All Contributors](https://img.shields.io/github/all-contributors/tamboui/tamboui?color=ee8449&style=flat-square)](#contributors)

A Java library for building modern terminal user interfaces.

TamboUI brings the TUI paradigms seen in things like Rust's [ratatui](https://github.com/ratatui/ratatui) or Go's [bubbletea](https://github.com/charmbracelet/bubbletea/) to the Java ecosystem.

TamboUI is pronounced like "[tambouille](https://www.howtopronounce.com/french/tambouille)" (\tɑ̃.buj\) in French, or "tan-bouy".

It provides a comprehensive set of widgets and a layout system for building rich terminal applications with modern Java idioms.

> **Note:** TamboUI is still experimental and under active development. APIs may/will change. Feedback, issues, and contributions are welcome!

### Snapshots

Currently TamboUI is only available as snapshot builds. You can use the following Maven repository to access snapshot versions:

```xml
<repository>
    <id>ossrh-snapshots</id>
    <url>https://central.sonatype.com/repository/maven-snapshots/</url>
    <releases>
        <enabled>false</enabled>
    </releases>
    <snapshots>
        <enabled>true</enabled>
    </snapshots>
</repository>
```

Gradle:

```kotlin
repositories {
    maven {
        url = uri("https://central.sonatype.com/repository/maven-snapshots/")
        mavenContent {
            snapshotsOnly()
        }
    }
}
```

### Key Features

- **Immediate-mode rendering** - Redraw the entire UI each frame for simple state management
- **Intermediate buffer system** - Widgets render to a buffer, enabling diff-based terminal updates
- **Constraint-based layout** - Flexible layout system with percentage, fixed, ratio, and proportional sizing
- **JLine 3 backend** - Cross-platform terminal support including Windows via Jansi
- **High-level TUI framework** - TuiRunner eliminates boilerplate with built-in event handling
- **PicoCLI integration** - Optional module for CLI argument parsing
- **GraalVM native image support** - Compile to native executables for instant startup
- **Works everywhere** - Core is Java 8+ compatible, but following patterns that works really well with modern Java idioms

## Modules

| Module | Description |
|--------|-------------|
| `tamboui-core` | Core types: Buffer, Cell, Rect, Style, Layout, Text primitives |
| `tamboui-widgets` | All widget implementations (see below) |
| `tamboui-jline` | JLine 3 terminal backend |
| `tamboui-tui` | High-level TUI framework with TuiRunner, event handling, and key helpers |
| `tamboui-toolkit` | Fluent DSL for declarative UI construction with components and focus management |
| `tamboui-picocli` | Optional PicoCLI integration for CLI argument parsing |
| `tamboui-core-assertj` | AssertJ assertions for things like Buffer comparison |
| `demos/*` | Demo applications showcasing widgets and features |

## Requirements

- Java 8 or later, Java 17+ highly recommended
- Gradle 9.x

## Quick Start

### Using TuiRunner (Recommended)

The `tamboui-tui` module provides a high-level framework that eliminates boilerplate:

```java
//DEPS dev.tamboui:tamboui-tui:LATEST
//DEPS dev.tamboui:tamboui-jline3-backend:LATEST

package dev.tamboui.demo;

import dev.tamboui.tui.TuiRunner;
import dev.tamboui.tui.event.KeyEvent;
import dev.tamboui.widgets.paragraph.Paragraph;
import dev.tamboui.text.Text;

public class HelloWorldTuiDemo {
    public static void main(String[] args) throws Exception {
        try (var tui = TuiRunner.create()) {
            tui.run(
                (event, runner) ->
                    switch (event) {
                        case KeyEvent k when k.isQuit() -> {
                            runner.quit();
                            yield false;
                        }
                        default -> false;
                    },
                frame -> {
                    var paragraph = Paragraph.builder()
                        .text(Text.from("Hello, TamboUI! Press 'q' to quit."))
                        .build();
                    frame.renderWidget(paragraph, frame.area());
                }
            );
        }
    }
}
```

Save the above file to `HelloWorld.java` and you can run it as `jbang HelloWorld.java`

### With Mouse and Animation

```java
import dev.tamboui.tui.TuiConfig;
import dev.tamboui.tui.TuiRunner;
import java.time.Duration;

var config = TuiConfig.builder()
    .mouseCapture(true)
    .tickRate(Duration.ofMillis(16))  // ~60fps animation
    .build();

try (var tui = TuiRunner.create(config)) {
    tui.run(
        (event, runner) -> switch (event) {
            case KeyEvent k when Keys.isQuit(k) -> { runner.quit(); yield false; }
            case MouseEvent m -> { handleMouse(m); yield true; }
            case TickEvent t -> { updateAnimation(t); yield true; }
            default -> false;
        },
        frame -> render(frame)
    );
}
```

### With PicoCLI Integration

```java
//DEPS dev.tamboui:tamboui-picocli:LATEST
//DEPS dev.tamboui:tamboui-jline3-backend:LATEST

import dev.tamboui.picocli.TuiCommand;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "myapp", mixinStandardHelpOptions = true)
public class MyApp extends TuiCommand {

    @Option(names = {"-t", "--title"}, description = "Window title")
    private final String title = "My App";

    @Override
    protected void runTui(TuiRunner runner) throws Exception {
        runner.run(this::handleEvent, this::render);
    }

    public static void main(String[] args) {
        System.exit(new CommandLine(new MyApp()).execute(args));
    }
}
```

### Using the declarative Toolkit

The `tamboui-toolkit` module provides a fluent DSL giving you retained mode declarative UI construction:

```java
//DEPS dev.tamboui:tamboui-toolkit:LATEST
//DEPS dev.tamboui:tamboui-jline3-backend:LATEST

import static dev.tamboui.toolkit.Toolkit.*;
import dev.tamboui.toolkit.app.ToolkitApp;
import dev.tamboui.toolkit.element.Element;

public class HelloDsl extends ToolkitApp {

    @Override
    protected Element render() {
        return panel("Hello",
            text("Welcome to TamboUI DSL!").bold().cyan(),
            spacer(),
            text("Press 'q' to quit").dim()
        ).rounded();
    }

    public static void main(String[] args) throws Exception {
        new HelloDsl().run();
    }
}
```

#### Toolkit Elements

| Element | Factory Method | Description |
|---------|----------------|-------------|
| Text | `text("Hello")` | Styled text content |
| Panel | `panel("Title", ...)` | Bordered container with title |
| Row | `row(a, b, c)` | Horizontal layout |
| Column | `column(a, b, c)` | Vertical layout |
| Spacer | `spacer()` | Flexible empty space |
| Gauge | `gauge(0.75)` | Progress bar |
| LineGauge | `lineGauge(50)` | Single-line progress indicator |
| Sparkline | `sparkline(1,2,3,4,5)` | Mini data chart |
| List | `list("A", "B", "C")` | Selectable list |
| Table | `table()` | Data table with rows/columns |
| Tabs | `tabs("Home", "Settings")` | Tab bar |
| TextInput | `textInput(state)` | Text input field |
| BarChart | `barChart(10, 20, 30)` | Bar chart |
| Chart | `chart()` | Line/scatter plots |
| Canvas | `canvas()` | Drawing surface |
| Calendar | `calendar()` | Monthly calendar |
| Scrollbar | `scrollbar()` | Scroll position indicator |

#### Toolkit Examples

```java
// Progress with color coding
gauge(0.75)
    .label("Loading...")
    .gaugeColor(Color.GREEN)
    .title("Progress")
    .rounded()

// Sparkline chart
sparkline(cpuHistory)
    .color(Color.CYAN)
    .title("CPU Usage")
    .rounded()

// Selectable list
list("Option 1", "Option 2", "Option 3")
    .state(listState)
    .highlightColor(Color.YELLOW)
    .title("Menu")
    .rounded()

// Tab bar
tabs("Home", "Settings", "About")
    .selected(0)
    .highlightColor(Color.CYAN)

// Table with data
table()
    .header("Name", "Age", "City")
    .row("Alice", "30", "NYC")
    .row("Bob", "25", "LA")
    .widths(Constraint.percentage(40), Constraint.length(10), Constraint.fill())
    .title("Users")
    .rounded()

// Canvas drawing
canvas(-10, 10, -10, 10)
    .paint(ctx -> {
        ctx.draw(new Circle(0, 0, 5, Color.RED));
        ctx.draw(new Line(-5, -5, 5, 5, Color.GREEN));
    })
    .title("Drawing")
    .rounded()
```

#### Features

- **Static imports** - `text()`, `panel()`, `row()`, `column()`, `spacer()`, and all widget factories
- **Fluent styling** - `.bold()`, `.cyan()`, `.onBlue()`, `.rounded()`, `.borderColor()`
- **Layout constraints** - `.length(n)`, `.percent(n)`, `.fill()`, `.min(n)`, `.max(n)`
- **Automatic focus** - Tab navigation, click-to-focus
- **Component events** - Components handle their own key/mouse events via `.onKeyEvent()` and `.onMouseEvent()`
- **Drag support** - `.draggable()` for movable elements

### Immediate Mode API

For more control, use the terminal directly:

```java
//DEPS dev.tamboui:tamboui-widgets:LATEST
//DEPS dev.tamboui:tamboui-jline3-backend:LATEST

import dev.tamboui.terminal.Backend;
import dev.tamboui.terminal.BackendFactory;
import dev.tamboui.terminal.Terminal;

try (var backend = BackendFactory.create()) {
    backend.enableRawMode();
    backend.enterAlternateScreen();

    var terminal = new Terminal<>(backend);
    terminal.draw(frame -> {
        // render widgets...
    });
}
```

## Building and Running Demos

### Building the Project

```bash
./gradlew assemble
```

### Running Tests

```bash
./gradlew test
```

### Publishing to Local Maven Repository

```bash
./gradlew publishToMavenLocal
```

### Publishing to the build directory

This method is preferred to publishing to Maven local to see what will actually be published:

```bash
./gradlew publishAllPublicationsToBuildRepository
```

Then look into `build/repo` for the generated artifacts.

### Running Demos

To run the demos, you can either run them on the JVM or compile them to a native executable.

#### JBang

If you have JBang installed you can list the local catalog:

```
jbang alias list .
```

And then pick one you want to run, i.e.:

```
jbang sparkline-demo
```

If your IDE supports JBang you can open the demo .java files and run them directly from your IDE too.

#### Bash

You can use the `./run-demo.sh` script to run any demo directly without building/installing first. For example, to run the `sparkline-demo`:

```bash
./run-demo.sh sparkline-demo
```

To run it as a native executable:

```bash
./run-demo.sh sparkline-demo --native
```

##### JVM

To run a demo on the JVM, first install the distribution and then execute the generated script.
For example, to run the `sparkline-demo`:

```bash
./gradlew :demos:sparkline-demo:installDist
./demos/sparkline-demo/build/install/sparkline-demo/bin/sparkline-demo
```

##### Native Image (requires GraalVM)

To run as a native executable:

```bash
./gradlew :demos:sparkline-demo:nativeCompile
./demos/sparkline-demo/build/native/nativeCompile/sparkline-demo
```

Replace `sparkline-demo` with the name of any other demo (e.g., `basic-demo`, `tui-demo`, `toolkit-demo`).

## Widgets

All standard ratatui widgets are implemented:

| Widget | Type | Description |
|--------|------|-------------|
| **Block** | Stateless | Container with borders, titles, and padding |
| **Paragraph** | Stateless | Multi-line text with wrapping and alignment |
| **List** | Stateful | Scrollable list with selection |
| **Table** | Stateful | Grid with rows, columns, and selection |
| **Tabs** | Stateful | Tab bar with selection |
| **Gauge** | Stateless | Progress bar with percentage |
| **LineGauge** | Stateless | Horizontal line progress indicator |
| **Sparkline** | Stateless | Mini line chart for data series |
| **BarChart** | Stateless | Vertical bar chart |
| **Chart** | Stateless | Line and scatter plots with axes |
| **Canvas** | Stateless | Drawing surface with shapes (braille/block characters) |
| **Calendar** | Stateless | Monthly calendar with date styling |
| **Scrollbar** | Stateful | Visual scroll position indicator |
| **Clear** | Stateless | Clears an area (for widget layering) |
| **TextInput** | Stateful | Single-line text input (TamboUI addition) |

## Demo Applications

| Demo | Description |
|------|-------------|
| `hello-world-demo` | Minimal Hello World using TuiRunner |
| `basic-demo` | Interactive list with text input |
| `tui-demo` | Showcases TuiRunner with keyboard, mouse, and animation |
| `toolkit-demo` | Widget Playground showcasing DSL with draggable panels |
| `jtop-demo` | System monitor (like "top") using the DSL |
| `picocli-demo` | PicoCLI integration with CLI options |
| `gauge-demo` | Progress bars and line gauges |
| `table-demo` | Table widget with selection |
| `tabs-demo` | Tab navigation |
| `scrollbar-demo` | Scrollbar with content |
| `sparkline-demo` | Sparkline data visualization |
| `barchart-demo` | Bar chart widget |
| `chart-demo` | Line and scatter charts |
| `canvas-demo` | Canvas with shapes and braille drawing |
| `calendar-demo` | Monthly calendar with events |

## Key Bindings (via Keys utility)

The `Keys` class provides helpers for common key patterns:

```java
import static dev.tamboui.tui.Keys.*;

// Quit patterns
isQuit(event)      // q, Q, or Ctrl+C

// Vim-style navigation (also accepts arrow keys)
isUp(event)        // Up arrow or k/K
isDown(event)      // Down arrow or j/J
isLeft(event)      // Left arrow or h/H
isRight(event)     // Right arrow or l/L

// Page navigation
isPageUp(event)    // PageUp or Ctrl+U
isPageDown(event)  // PageDown or Ctrl+D
isHome(event)      // Home or g
isEnd(event)       // End or G

// Actions
isSelect(event)    // Enter or Space
isEscape(event)    // Escape
isTab(event)       // Tab
isBackTab(event)   // Shift+Tab
```

## Contributors

<!-- ALL-CONTRIBUTORS-LIST:START - Do not remove or modify this section -->
<!-- prettier-ignore-start -->
<!-- markdownlint-disable -->
<table>
  <tbody>
    <tr>
      <td align="center" valign="top" width="14.28%"><a href="http://melix.github.io/blog"><img src="https://avatars.githubusercontent.com/u/316357?v=4?s=100" width="100px;" alt="Cédric Champeau"/><br /><sub><b>Cédric Champeau</b></sub></a><br /><a href="#infra-melix" title="Infrastructure (Hosting, Build-Tools, etc)">🚇</a> <a href="#test-melix" title="Tests">⚠️</a> <a href="#code-melix" title="Code">💻</a></td>
      <td align="center" valign="top" width="14.28%"><a href="https://xam.dk"><img src="https://avatars.githubusercontent.com/u/54129?v=4?s=100" width="100px;" alt="Max Rydahl Andersen"/><br /><sub><b>Max Rydahl Andersen</b></sub></a><br /><a href="#infra-maxandersen" title="Infrastructure (Hosting, Build-Tools, etc)">🚇</a> <a href="#test-maxandersen" title="Tests">⚠️</a> <a href="#code-maxandersen" title="Code">💻</a></td>
      <td align="center" valign="top" width="14.28%"><a href="https://github.com/davsclaus"><img src="https://avatars.githubusercontent.com/u/477100?v=4?s=100" width="100px;" alt="Claus Ibsen"/><br /><sub><b>Claus Ibsen</b></sub></a><br /><a href="#code-davsclaus" title="Code">💻</a></td>
    </tr>
  </tbody>
</table>

<!-- markdownlint-restore -->
<!-- prettier-ignore-end -->

<!-- ALL-CONTRIBUTORS-LIST:END -->

## Acknowledgments

This project started as a port of [ratatui](https://github.com/ratatui/ratatui), an excellent Rust TUI library. We thank the ratatui maintainers and contributors for their work.

## License

MIT License - see [LICENSE](LICENSE) for details.
