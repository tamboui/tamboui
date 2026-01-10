# AGENTS.md

This file provides guidance to AI coding agents when working with code in this repository.
See [agents.md](https://agents.md/) for the specification.

## Project Overview

TamboUI is a Java library for building modern terminal user interfaces, inspired by Rust's ratatui and Go's bubbletea. It uses immediate-mode rendering with an intermediate buffer system for diff-based terminal updates.

## Build and Test Commands

```bash
# Build the project
./gradlew -q assemble

# Run all tests
./gradlew -q test

# Run a single test class
./gradlew -q :tamboui-core:test --tests "dev.tamboui.buffer.BufferTest"

# Publish to local Maven repository
./gradlew publishToMavenLocal

# Publish to build directory (preferred for inspection)
./gradlew publishAllPublicationsToBuildRepository
# Artifacts appear in build/repo/

# Run a demo on JVM
./run-demo.sh sparkline-demo

# Run a demo as native executable (requires GraalVM)
./run-demo.sh sparkline-demo --native

# Alternative: install and run demo manually
./gradlew :demos:sparkline-demo:installDist
./demos/sparkline-demo/build/install/sparkline-demo/bin/sparkline-demo

# Compile demo to native image
./gradlew :demos:sparkline-demo:nativeCompile
```

## Module Structure

| Module | Purpose |
|--------|---------|
| `tamboui-core` | Core types: Buffer, Cell, Rect, Style, Layout, Text, Widget/StatefulWidget interfaces |
| `tamboui-widgets` | All widget implementations (Block, Paragraph, List, Table, Chart, Canvas, etc.) |
| `tamboui-jline` | JLine 3 terminal backend implementation |
| `tamboui-tui` | High-level TUI framework: TuiRunner, event handling, Keys utility |
| `tamboui-toolkit` | Fluent DSL for declarative UI with retained-mode elements, focus management, event routing |
| `tamboui-css` | CSS-based styling support |
| `tamboui-picocli` | PicoCLI integration for CLI argument parsing |
| `demos/*` | Demo applications showcasing features |

## Architecture

### Rendering Model

1. **Widget interface** (`tamboui-core`): Stateless widgets implement `Widget.render(Rect, Buffer)`. Stateful widgets implement `StatefulWidget<S>.render(Rect, Buffer, S)`.

2. **Buffer system** (`tamboui-core`): Widgets render to a `Buffer` (2D grid of `Cell`s). The `Terminal` diffs buffers between frames for efficient updates.

3. **Layout system** (`tamboui-core`): `Layout` with `Constraint`s (length, percentage, ratio, min, max, fill) splits areas. `Rect` represents rectangular regions.

### Application Layers

**Low-level (immediate mode):**
```java
Terminal<Backend> terminal = new Terminal<>(BackendFactory.create());
terminal.draw(frame -> widget.render(frame.area(), frame.buffer()));
```

**Mid-level (TuiRunner):**
```java
try (var tui = TuiRunner.create(TuiConfig.builder().mouseCapture(true).build())) {
    tui.run((event, runner) -> { /* handle events */ return shouldRedraw; },
            frame -> { /* render widgets */ });
}
```

**High-level (Toolkit DSL):**
```java
import static dev.tamboui.toolkit.Toolkit.*;

class MyApp extends ToolkitApp {
    protected Element render() {
        return panel("Title", text("Hello").bold().cyan());
    }
}
```

### Event System

- `TuiRunner` provides the main event loop with `EventHandler` callback
- Event types: `KeyEvent`, `MouseEvent`, `TickEvent`, `ResizeEvent`
- `Keys` utility provides pattern matching helpers (`isQuit`, `isUp`, `isDown`, etc.)
- Toolkit elements handle events via `handleKeyEvent()`/`handleMouseEvent()` or handler lambdas

### Key Packages

- `dev.tamboui.buffer` - Buffer, Cell for rendering
- `dev.tamboui.layout` - Rect, Constraint, Layout, Direction
- `dev.tamboui.style` - Style, Color, Modifier
- `dev.tamboui.text` - Text, Span, Line for styled text
- `dev.tamboui.widgets.*` - Widget implementations (block, paragraph, list, table, chart, canvas, etc.)
- `dev.tamboui.tui` - TuiRunner, TuiConfig, Keys, event types
- `dev.tamboui.toolkit` - Toolkit DSL factory methods, Element interface, element implementations

## Code Style Guidelines

- You MUST use Java 8 source compatibility for library modules
- You SHOULD use Java 21 for demo applications
- You MUST use JUnit 5 for testing
- You SHOULD Use immutable data structures as much as possible
- You MUST use the most recent Java idioms supported for a particular language level
- You MUST add braces on all control statements
- You MUST follow conventional field/method declarations (fields on top, methods below)
- You MUST avoid code duplication
- Add MUST add javadocs to all public APIs
- You MUST use imports instead of fully qualified names in code
- Do SHOULD NOT name a method `getXXX` if it's not a simple getter returning a private field: prefer `computeXXX`, `fetchXXX`, `toXXX`, etc.
- You MUST NOT add comments in source which cannot be understood without context

## Testing Instructions

- All new features MUST include unit tests
- Run `./gradlew test` to execute all tests
- Run `./gradlew -q test` for quiet output
- Do not consider the task complete until all tests pass without errors
- Run `./gradlew -q build` to ensure the project builds successfully

## PR Guidelines

- Use `git add` for new files to include them in the commit
- Do not commit automatically; wait for human review
- Do not include agent instruction files (CLAUDE.md, AGENTS.md) in commits
- Review your own code changes before finishing work