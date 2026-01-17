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
| `tamboui-core` | Core types: Buffer, Cell, Rect, Style, Layout, Text, Widget/StatefulWidget interfaces, InlineDisplay |
| `tamboui-widgets` | All widget implementations (Block, Paragraph, List, Table, Chart, Canvas, etc.) |
| `tamboui-jline` | JLine 3 terminal backend implementation |
| `tamboui-tui` | High-level TUI framework: TuiRunner, event handling, bindings, action handlers |
| `tamboui-toolkit` | Fluent DSL for declarative UI with retained-mode elements, focus management, event routing |
| `tamboui-css` | CSS-based styling with TCSS format, selectors, cascade resolution, theme switching |
| `tamboui-annotations` | Annotation definitions (`@OnAction`) for action handling |
| `tamboui-processor` | Annotation processor for compile-time action handler generation (avoids reflection) |
| `tamboui-image` | Image rendering with multiple protocols (Kitty, iTerm, Sixel, HalfBlock, Braille) |
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
- `KeyEvent` provides semantic methods (`isQuit()`, `isUp()`, `isDown()`, `isSelect()`, etc.) that respect configured bindings
- Bindings map physical inputs to semantic actions; use `BindingSets.standard()`, `BindingSets.vim()`, or custom bindings
- `@OnAction` annotation on Component methods handles actions; use annotation processor for compile-time generation
- Toolkit elements handle events via `handleKeyEvent()`/`handleMouseEvent()` or handler lambdas

### Key Packages

- `dev.tamboui.buffer` - Buffer, Cell for rendering
- `dev.tamboui.layout` - Rect, Constraint, Layout, Direction
- `dev.tamboui.style` - Style, Color, Modifier
- `dev.tamboui.text` - Text, Span, Line for styled text
- `dev.tamboui.widgets.*` - Widget implementations (block, paragraph, list, table, chart, canvas, etc.)
- `dev.tamboui.tui` - TuiRunner, TuiConfig, event types
- `dev.tamboui.tui.bindings` - Bindings, BindingSets, KeyTrigger, MouseTrigger, ActionHandler, @OnAction
- `dev.tamboui.toolkit` - Toolkit DSL factory methods, Element interface, element implementations
- `dev.tamboui.css` - StyleEngine, CssParser, selectors, cascade resolution
- `dev.tamboui.inline` - InlineDisplay for progress/status output
- `dev.tamboui.image` - Image widget and protocol implementations

## Code Style Guidelines

- You MUST use Java 8 source compatibility for library modules
- You SHOULD use Java 21 for demo applications
- You MUST use JUnit 5 for testing
- You SHOULD Use immutable data structures as much as possible
- You MUST use the most recent Java idioms supported for a particular language level
- You MUST add braces on all control statements
- You MUST follow conventional field/method declarations (fields on top, methods below)
- You MUST avoid code duplication
- You MUST add javadocs to all public APIs
- You MUST use imports instead of fully qualified names in code
- You SHOULD NOT name a method `getXXX` if it's not a simple getter returning a private field: prefer `computeXXX`, `fetchXXX`, `toXXX`, etc.
- You MUST NOT add comments in source which cannot be understood without context

## Testing Instructions

- All new features MUST include unit tests
- Run `./gradlew test` to execute all tests
- Run `./gradlew -q test` for quiet output
- Do not consider the task complete until all tests pass without errors
- Run `./gradlew -q build` to ensure the project builds successfully
- Use `BufferAssertions` for testing buffer contents in widget tests

## Documentation

- Any API changes MUST be reflected in the documentation under `docs/src/docs/asciidoc/`
- Build docs with `./gradlew :docs:asciidoctor` - output goes to `docs/build/docs/`
- Key documentation files: `api-levels.adoc`, `widgets.adoc`, `bindings.adoc`, `styling.adoc`, `core-concepts.adoc`
- Update `index.adoc` module overview when adding new modules
- Keep AGENTS.md in sync with module structure and key packages

## Widget/App Demos

- `docs/video` should have a .tape files which are used with `vhs` to generate .svg and .mp4 for use on website
- each .tape should `Source shared_.tape` to have consistent look
- each .tape should use a `# Setup` section to `Hide` then run the demo using `jbang`, wait some seconds and then `Show`
- after `# Setup` put `# Recording` and do the necessary `Type` and `Sleep` actions
- keep each .tape short and focus. 
- Try cover all demo features, but be aware they should not be too long as then the `.svg` renderings gets too big.

## CSS-Compatible Elements

When writing new Elements or modifying existing ones, follow these guidelines to ensure consistent CSS support:

### The `resolveEffectiveStyle` Helper

Use `StyledElement.resolveEffectiveStyle()` for sub-component styling with the priority order: **explicit > CSS > default**.

```java
// Simple case (no pseudo-class):
Style effectiveCursorStyle = resolveEffectiveStyle(context, "cursor", cursorStyle, DEFAULT_CURSOR_STYLE);

// With pseudo-class state (e.g., :selected):
Style effectiveHighlightStyle = resolveEffectiveStyle(
    context, "item", PseudoClassState.ofSelected(),
    highlightStyle, DEFAULT_HIGHLIGHT_STYLE);
```

### Pattern for CSS-Compatible Fields

1. **Make style fields nullable** (null = "use CSS or default"):
   ```java
   private Style cursorStyle;  // null, not Style.EMPTY
   ```

2. **Add default style constants**:
   ```java
   private static final Style DEFAULT_CURSOR_STYLE = Style.EMPTY.reversed();
   private static final Style DEFAULT_PLACEHOLDER_STYLE = Style.EMPTY.dim();
   ```

3. **Use resolveEffectiveStyle in renderContent()**:
   ```java
   Style effectiveStyle = resolveEffectiveStyle(context, "child-name", explicitStyle, DEFAULT_STYLE);
   ```

### Documenting CSS Selectors

Add a JavaDoc section to each Element class documenting its CSS child selectors:

```java
/**
 * <h2>CSS Child Selectors</h2>
 * <ul>
 *   <li>{@code ElementName-cursor} - The cursor style (default: reversed)</li>
 *   <li>{@code ElementName-placeholder} - The placeholder text style (default: dim)</li>
 * </ul>
 */
```

### Available CSS Child Selectors

| Element | Child Selectors |
|---------|-----------------|
| TextInputElement | `-cursor`, `-placeholder` |
| TextAreaElement | `-cursor`, `-placeholder`, `-line-number` |
| GaugeElement | `-filled` |
| LineGaugeElement | `-filled`, `-unfilled` |
| ScrollbarElement | `-thumb`, `-track`, `-begin`, `-end` |
| ListElement | `-item`, `-scrollbar-thumb`, `-scrollbar-track` |
| TableElement | `-row`, `-header` |
| TabsElement | `-tab`, `-divider` |

## PR Guidelines

- Use `git add` for new files to include them in the commit
- Do not commit automatically; wait for human review
- Do not include agent instruction files (CLAUDE.md, AGENTS.md) in commits
- Review your own code changes before finishing work