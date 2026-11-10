# AGENTS.md

This file provides guidance to AI coding agents when contributing to code in this repository.

If you are learning to use or write applications with TamboUI check out https://tamboui.dev/llms.txt instead.

## Project Overview

TamboUI is a Java library for building modern terminal user interfaces, inspired by Rust's ratatui, Go's bubbletea and Python's Textual. It uses immediate-mode rendering with an intermediate buffer system for diff-based terminal updates, while also providing a higher-level toolkit for easy component-based rendering.

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

| Module                | Purpose                                                                                             |
|-----------------------|-----------------------------------------------------------------------------------------------------|
| `tamboui-core`        | Core types: Buffer, Cell, Rect, Style, Layout, Text, Widget/StatefulWidget interfaces, InlineDisplay |
| `tamboui-widgets`     | All widget implementations (Block, Paragraph, List, Table, Chart, Canvas, Sparkline, DualSparkline, etc.) |
| `tamboui-jline`       | JLine 3 terminal backend implementation                                                             |
| `tamboui-tui`         | High-level TUI framework: TuiRunner, event handling, bindings, action handlers                      |
| `tamboui-toolkit`     | Fluent DSL for declarative UI with retained-mode elements, focus management, event routing          |
| `tamboui-css`         | CSS-based styling with TCSS format, selectors, cascade resolution, theme switching                  |
| `tamboui-annotations` | Annotation definitions (`@OnAction`) for action handling                                            |
| `tamboui-processor`   | Annotation processor for compile-time action handler generation (avoids reflection)                 |
| `tamboui-image`       | Image rendering with multiple protocols (Kitty, iTerm, Sixel, HalfBlock, Braille)                   |
| `tamboui-markdown`    | Markdown renderer (CommonMark + GFM; opt-in CommonMark dep; Java 11) |
| `tamboui-toolkit-markdown` | Toolkit DSL element wrapping `MarkdownView` (Java 11) |
| `tamboui-picocli`     | PicoCLI integration for CLI argument parsing                                                        |
| `**/demos/*`          | Demo applications showcasing features                                                               |

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
- Event types: `KeyEvent`, `MouseEvent`, `TickEvent`, `ResizeEvent`, `UiRunnable`
- `TickEvent` is a redraw pulse that carries no time; at most one waits in the queue (ticks do not pile up). To advance an animation by real time, measure it at the render with `RenderClock` (monotonic, sub-millisecond carry, clamps long gaps); to step once per tick, use `TickEvent.frameCount()`. tfx effects use `RenderClock`, mirroring ratatui's tachyonfx (the host measures elapsed at the draw, not from a tick).
- `KeyEvent` provides semantic methods (`isQuit()`, `isUp()`, `isDown()`, `isSelect()`, etc.) that respect configured bindings
- Bindings map physical inputs to semantic actions; use `BindingSets.standard()`, `BindingSets.vim()`, or custom bindings
- `@OnAction` annotation on Component methods handles actions; use annotation processor for compile-time generation
- Toolkit elements handle events via `handleKeyEvent()`/`handleMouseEvent()` or handler lambdas

### Threading Model

- TamboUI TUI framework uses a dedicated **render thread model** similar to JavaFX or Swing
- All rendering and UI state modifications must happen on the render thread (the thread running `TuiRunner.run()`)
- `RenderThread.isRenderThread()` checks if current thread is render thread; `RenderThread.checkRenderThread()` asserts it
- `TuiRunner.runOnRenderThread(Runnable)` executes on render thread (immediately if already on it, queued otherwise)
- `TuiRunner.runLater(Runnable)` always queues for later execution
- Scheduled actions via `ToolkitRunner.schedule()` run on scheduler thread; use `runOnRenderThread()` for UI state changes
- Thread checks only enforce when render thread is set (allows unit tests without special setup)

### Key Packages

- `dev.tamboui.buffer` - Buffer, Cell for rendering
- `dev.tamboui.layout` - Rect, Constraint, Layout, Direction
- `dev.tamboui.style` - Style, Color, Modifier
- `dev.tamboui.text` - Text, Span, Line for styled text
- `dev.tamboui.widgets.*` - Widget implementations (block, paragraph, list, table, chart, canvas, sparkline, etc.)
- `dev.tamboui.tui` - TuiRunner, TuiConfig, RenderThread, RenderClock, event types
- `dev.tamboui.tui.bindings` - Bindings, BindingSets, KeyTrigger, MouseTrigger, ActionHandler, @OnAction
- `dev.tamboui.toolkit` - Toolkit DSL factory methods, Element interface, element implementations
- `dev.tamboui.css` - StyleEngine, CssParser, selectors, cascade resolution
- `dev.tamboui.inline` - InlineDisplay for progress/status output
- `dev.tamboui.image` - Image widget and protocol implementations

## Unicode and Display Width

When working with text in widgets, **always use `CharWidth` for display width calculations**, not `String.length()`.

### Critical Rules

- **NEVER** use `text.length()` for display width calculations
- **NEVER** use `text.substring(0, n)` for display truncation
- **ALWAYS** use `CharWidth.of(text)` for display width
- **ALWAYS** use `CharWidth.substringByWidth(text, maxWidth)` for truncation

### Why This Matters

Java's `String.length()` returns UTF-16 code units, not terminal display columns:

| Character | `length()` | Display Width |
|-----------|------------|---------------|
| "A" | 1 | 1 |
| "世" (CJK) | 1 | 2 |
| "🔥" | 2 | 2 |
| "👨‍🦲" (ZWJ) | 5 | 2 |

### Required Pattern

```java
// WRONG
int width = text.length();
String truncated = text.substring(0, maxWidth);

// CORRECT
int width = CharWidth.of(text);
String truncated = CharWidth.substringByWidth(text, maxWidth);
```

### CharWidth Utilities

```java
import dev.tamboui.text.CharWidth;

CharWidth.of(String s)                    // Display width of string
CharWidth.of(int codePoint)               // Display width of code point
CharWidth.substringByWidth(s, maxWidth)   // Truncate by display width
CharWidth.substringByWidthFromEnd(s, w)   // Truncate from end
CharWidth.truncateWithEllipsis(s, w, pos) // Truncate with "..."
```

### Grapheme Clusters

One visible character can be several code points: a ZWJ emoji (`👩‍💻`), a flag (`🇫🇷`), a letter with combining marks.

- **NEVER** move a cursor, wrap or cut text by `char` or by code point
- **ALWAYS** step through text with `GraphemeClusters.clusterEnd(text, pos)` and `GraphemeClusters.clusterStart(text, pos)` (`dev.tamboui.text`)
- `CharWidth.substringByWidth` already cuts on cluster boundaries: use it instead of a loop of your own
- Tests for text handling MUST include a ZWJ sequence (written with `\u200D`), a flag and a CJK character

### Reference Implementation

See `Paragraph.java` for correct CharWidth usage in text rendering.

## JFR (Java Flight Recorder) Support

TamboUI can emit JFR events for diagnostics/performance analysis.

### Packaging

- **Core JFR events**: `dev.tamboui.jfr` (e.g. `TerminalDrawEvent`)
- **Toolkit JFR events**: `dev.tamboui.toolkit.jfr`

### Naming

- Use `@Name("dev.tamboui.<area>.<thing>")`
- Keep names short and consistent; **do not include `Event`** in the `@Name`.

### Runtime overhead

- Always guard emission with `enabled()` checks to keep overhead minimal when not recording:
  - Callers should do `if (X.enabled()) { X.commit(...); }`
  - The `enabled()` method must be on the event class so callers can avoid any allocations.

### Helper method convention

- Prefer static helpers on the event class over separate tracer interfaces.
- If a helper just creates + populates + calls `Event.commit()`, name it **`commit(...)`** to match JDK convention.

### Java 8 compatibility

- Library modules target **Java 8**, so referencing `jdk.jfr.*` requires the build to supply it.
- When adding JFR events to a module, add `compileOnly(libs.jfr.polyfill)` to that module’s Gradle dependencies.

## Exception Handling Strategy

TamboUI uses a consistent exception hierarchy for framework errors.

### Exception Hierarchy

- **`TamboUIException`** (`dev.tamboui.error.TamboUIException`) - Abstract base exception for all TamboUI framework errors
 - Extends `RuntimeException` (unchecked)
 - Abstract base class that all TamboUI exceptions extend
 - **`RuntimeIOException`** (`dev.tamboui.error.RuntimeIOException`) - Extends `TamboUIException`
 - Used specifically for terminal I/O errors (wraps `IOException` from backend operations)
 - **`BackendException`** (`dev.tamboui.terminal.BackendException`) - Extends `TamboUIException`
 - Used for backend-related errors that are not specifically terminal I/O (e.g. native failures, provider lookup)
 - **`TuiException`** (`dev.tamboui.tui.TuiException`) - Extends `TamboUIException`
 - Used for TUI framework–level errors (e.g. render thread misuse, invalid bindings)

### Exception Usage Guidelines

1. **Terminal Operations**: All `Terminal` methods throw `RuntimeIOException` for I/O errors
   - Terminal wraps all `IOException`s from the backend in `RuntimeIOException` with descriptive messages
   - This provides a cleaner API and consistent error handling
   - Use `RuntimeIOException` when you know the error is I/O related

2. **Backend Operations**: `Backend` interface methods still throw `IOException` (checked exception)
   - Backends are low-level implementations; `IOException` is appropriate here
   - Terminal layer wraps these in `RuntimeIOException` for user-facing APIs

3. **General Framework Errors**: Use the most specific `TamboUIException` subtype:
   - `RuntimeIOException` for terminal I/O errors
   - `BackendException` for non-I/O backend errors (e.g., native/Panama failures, provider resolution)
   - `TuiException` for TUI framework errors (e.g., invalid bindings, render thread misuse)

4. **Parameter Validation**: Use standard Java exceptions:
   - `IllegalArgumentException` for invalid method parameters
   - `IllegalStateException` for invalid object state
   - `NullPointerException` for null values (via `Objects.requireNonNull()`)

5. **Domain-Specific Exceptions**: Existing domain exceptions remain:
   - `SolverException` and subclasses for layout constraint solver errors
   - `CssParseException` for CSS parsing errors
   - `UnknownCssPropertyException` for unknown CSS properties

### Error Context

When wrapping exceptions, always include context:
```java
// Good: includes context and uses appropriate exception type
throw new RuntimeIOException(
    String.format("Failed to set cursor position to %s: %s", pos, e.getMessage()), e);

// Good: backend error
throw new BackendException("Failed to load backend: " + backendName, e);

// Avoid: generic message
throw new RuntimeException("Error", e);
```

### Error Handling in TuiRunner

- `TuiRunner` catches all exceptions during rendering and event handling
- Errors are passed to `RenderErrorHandler` for customizable handling
- Default handler displays errors in the UI with scrollable stack traces
- See `RenderErrorHandlers` for pre-built handlers

## Fixing Bugs

- Fix the root cause, where the design puts it. TamboUI is ported from ratatui (widgets, buffer) and tachyonfx (effects measure time at the render with `RenderClock`), and is inspired by Textual (CSS, bbcode/markup) and in some places bubbletea: check how they and other terminal frameworks solve the problem before adding a workaround
- Do not end a protocol sequence with a timeout or a heuristic: a bracketed paste is read until its end marker, as other terminal libraries do. Correctness comes before latency
- Search sibling code for the same class of bug. Fix the identical cases in the same PR and list the related ones in the description. Known families:
  - text wrapping: `Paragraph`, `MarkdownInlineRenderer.applyOverflow`, `TextAreaState.computeDisplayRows` (to be unified, see #462)
  - cutting with an ellipsis: `Paragraph`, `Block` titles, markdown
  - scrollbars that reserve a column: `ListWidget`, `MarkupTextAreaElement`, `RichTextAreaElement`
  - `TuiRunner` and `InlineTuiRunner` share scheduler and event-queue code (see #494): change both
- Before starting, look for open PRs and issues on the same code (`gh pr list --search`, `gh issue list --search`) and link them

## Code Style Guidelines

- You MUST use Java 8 source compatibility for core library modules; a module MAY require a higher level only when unavoidable (e.g. the FFM backend needs Java 22+) — use multi-release JARs where it helps.
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
- Tests SHOULD use the `BufferAssertions` test fixtures as much as possible
- You CAN improve test fixtures to make tests more readable
- You MUST reuse existing helpers (`CharWidth`, `GraphemeClusters`, `BufferAssertions`, a module's shared package-private helpers) instead of adding a private copy: search before writing one
- You MUST NOT add public API, builder options or toggles without a concrete use case: removing them later is a breaking change, so propose new API in an issue first
- You MUST NOT use the wall clock (`System.currentTimeMillis()`, `Instant.now()`) to schedule or animate: use `System.nanoTime()` (compared by difference) or `RenderClock`

## Testing Instructions

- All new features MUST include unit tests
- Run `./gradlew test` to execute all tests
- Run `./gradlew -q test` for quiet output
- Do not consider the task complete until all tests pass without errors
- Run `./gradlew -q build` to ensure the project builds successfully
- Run `./gradlew -q uJB` to update the JBang demo catalog when a demo is added
- Before calling something done, run `./gradlew -q javadoc` to ensure no javadoc warnings are emitted
- Use `BufferAssertions` for testing buffer contents in tests
- A bug fix MUST include a test that fails without the fix
- Test the inputs that must be rejected, not only the ones that work
- For locale- or script-dependent code, run the change over `Locale.getAvailableLocales()` (or every affected script) and say in the PR what still fails
- Drive time-dependent code with a fake clock (a `LongSupplier`, as `RenderClock` takes) rather than `Thread.sleep`
- Run `./gradlew spotlessApply` before committing; CI runs `spotlessCheck` (import order, unused imports)

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
- A fix or feature that can be seen should show up in the matching demo: add the case (a wide or ZWJ line, a blank spacer item, a readout) and update its `.tape`

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
| ToastHostElement | `-info`, `-success`, `-warning`, `-error`, `-title`, `-progress` |

## PR Guidelines

- Use `git add` for new files to include them in the commit
- Do not commit automatically; wait for human review
- Do not include agent instruction files (CLAUDE.md, AGENTS.md) in commits
- Review your own code changes before finishing work
- Keep one root cause per PR, keep it minimal, and scope the title to what changed ("Paragraph", not "wrapping")
- Show the bug: a before/after image or sketch, and a reproducer the reviewer can run
- Prefer a **self-contained JBang reproducer**: a single `.java` file (e.g. a gist) with the dependencies and runtime declared *in the file*, so a reviewer runs it with just `jbang <url>` and no flags to remember. Put the directives in the source rather than on the command line:

```java
///usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 21
//DEPS dev.tamboui:tamboui-toolkit:LATEST
//DEPS dev.tamboui:tamboui-aesh-backend:LATEST
//RUNTIME_OPTIONS --enable-native-access=ALL-UNNAMED  // only when using the Panama (FFM) backend
```

  So `jbang https://gist.github.com/.../Repro.java` just works, instead of `jbang --deps ... --java 21 <url>`. Use `LATEST` for released behaviour, or a `-SNAPSHOT` / local build when reproducing an unreleased fix.
- Say what the PR leaves out on purpose
- Send PRs one at a time, most important first, and wait for review before sending more
