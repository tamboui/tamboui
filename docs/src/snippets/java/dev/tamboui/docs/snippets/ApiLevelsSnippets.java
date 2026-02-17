package dev.tamboui.docs.snippets;

import java.io.IOException;

import dev.tamboui.backend.jline3.JLineBackend;
import dev.tamboui.buffer.Buffer;
import dev.tamboui.buffer.Cell;
import dev.tamboui.inline.InlineDisplay;
import dev.tamboui.layout.Constraint;
import dev.tamboui.layout.Flex;
import dev.tamboui.layout.Layout;
import dev.tamboui.layout.Rect;
import dev.tamboui.style.Color;
import dev.tamboui.style.Style;
import dev.tamboui.terminal.Frame;
import dev.tamboui.terminal.Terminal;
import dev.tamboui.text.Line;
import dev.tamboui.text.Span;
import dev.tamboui.text.Text;
import dev.tamboui.toolkit.app.InlineApp;
import dev.tamboui.toolkit.app.InlineToolkitRunner;
import dev.tamboui.toolkit.app.ToolkitApp;
import dev.tamboui.toolkit.app.ToolkitRunner;
import dev.tamboui.toolkit.element.Element;
import dev.tamboui.toolkit.elements.ListElement;
import dev.tamboui.toolkit.event.EventResult;
import dev.tamboui.tui.EventHandler;
import dev.tamboui.tui.InlineTuiConfig;
import dev.tamboui.tui.InlineTuiRunner;
import dev.tamboui.tui.RenderThread;
import dev.tamboui.tui.Renderer;
import dev.tamboui.tui.TuiConfig;
import dev.tamboui.tui.TuiRunner;
import dev.tamboui.tui.error.ErrorAction;
import dev.tamboui.tui.error.RenderErrorHandlers;
import dev.tamboui.tui.event.Event;
import dev.tamboui.tui.event.KeyEvent;
import dev.tamboui.tui.event.MouseEvent;
import dev.tamboui.tui.event.ResizeEvent;
import dev.tamboui.tui.event.TickEvent;
import dev.tamboui.widget.Widget;
import dev.tamboui.widgets.block.Block;
import dev.tamboui.widgets.block.BorderType;
import dev.tamboui.widgets.block.Borders;
import dev.tamboui.widgets.gauge.Gauge;
import dev.tamboui.widgets.input.TextInputState;
import dev.tamboui.widgets.paragraph.Paragraph;
import dev.tamboui.widgets.table.TableState;

import java.io.PrintStream;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import static dev.tamboui.toolkit.InlineToolkit.scope;
import static dev.tamboui.toolkit.Toolkit.*;

/**
 * Code snippets for api-levels.adoc documentation.
 * Each method contains tagged regions that are included in the documentation.
 */
@SuppressWarnings({"unused", "UnnecessaryLocalVariable", "InfiniteLoopStatement"})
public class ApiLevelsSnippets {

    // Stub fields for rendering
    Widget header;
    Widget content;
    Widget statusBar;
    Widget customWidget1;
    Widget customWidget2;
    Widget myCustomWidget;

    // Stub methods for rendering
    void renderUI(Frame frame) {}
    boolean handleEvent(Event event) { return false; }
    boolean handleKey(KeyEvent k) { return false; }
    boolean handleMouse(MouseEvent m) { return false; }
    void renderProgress(Rect area, Buffer buffer, Task task, double progress) {}
    void processTask(Task task) {}

    // Stub types
    interface Task {
        String name();
    }
    List<Task> tasks = new ArrayList<>();
    double progress = 0;
    double progress1 = 0;
    double progress2 = 0;
    boolean downloading = true;
    int animationFrame = 0;

    // ========== IMMEDIATE MODE ==========

    void terminalAndBackend() throws Exception {
        // tag::terminal-and-backend[]
        try (var backend = new JLineBackend()) {
            var terminal = new Terminal<>(backend);
            // ...
        }
        // end::terminal-and-backend[]
    }

    void backendSetup() throws Exception {
        try (var backend = new JLineBackend()) {
            // tag::backend-setup[]
            // Raw mode disables line buffering - keys are available immediately
            backend.enableRawMode();

            // Alternate screen preserves the user's terminal content
            backend.enterAlternateScreen();

            // Hide the cursor for a cleaner look
            backend.hideCursor();

            // Mouse support is optional
            backend.enableMouseCapture();
            // end::backend-setup[]
        }
    }

    void drawing() throws Exception {
        try (var backend = new JLineBackend()) {
            var terminal = new Terminal<>(backend);
            // tag::drawing[]
            terminal.draw(frame -> {
                Rect area = frame.area();  // Full terminal size

                var paragraph = Paragraph.builder()
                    .text(Text.from("Hello, TamboUI!"))
                    .style(Style.EMPTY.bold().fg(Color.CYAN))
                    .build();

                frame.renderWidget(paragraph, area);
            });
            // end::drawing[]
        }
    }

    void bufferManipulation() throws Exception {
        try (var backend = new JLineBackend()) {
            var terminal = new Terminal<>(backend);
            // tag::buffer-manipulation[]
            terminal.draw(frame -> {
                Buffer buffer = frame.buffer();
                buffer.set(0, 0, new Cell("X", Style.EMPTY.bold()));

                Cell cell = buffer.get(5, 5);
                // ...
            });
            // end::buffer-manipulation[]
        }
    }

    // tag::immediate-mode-example[]
    public static class ImmediateModeExample {
        public static void main(String[] args) throws Exception {
            try (var backend = new JLineBackend()) {
                backend.enableRawMode();
                backend.enterAlternateScreen();
                backend.hideCursor();

                var terminal = new Terminal<>(backend);

                // Note: For event handling, use TuiRunner which provides
                // a managed event loop. This example shows low-level terminal setup.
                terminal.draw(frame -> {
                    var widget = Paragraph.builder()
                        .text(Text.from("Press 'q' to quit"))
                        .style(Style.EMPTY.fg(Color.CYAN))
                        .build();
                    frame.renderWidget(widget, frame.area());
                });
            }
        }
    }
    // end::immediate-mode-example[]

    // ========== TUI RUNNER ==========

    void tuiRunnerBasic() throws Exception {
        // tag::tui-runner-basic[]
        try (var tui = TuiRunner.create()) {
            tui.run(
                (event, runner) -> {
                    if (event instanceof KeyEvent key && key.isQuit()) {
                        runner.quit();
                        return false;
                    }
                    return handleEvent(event);
                },
                frame -> renderUI(frame)
            );
        }
        // end::tui-runner-basic[]
    }

    void tuiConfig() throws Exception {
        EventHandler handler = (event, runner) -> false;
        Renderer renderer = frame -> {};
        // tag::tui-config[]
        var config = TuiConfig.builder()
            .mouseCapture(true)
            .tickRate(Duration.ofMillis(16))  // ~60fps for animations
            .pollTimeout(Duration.ofMillis(50))
            .build();

        try (var tui = TuiRunner.create(config)) {
            tui.run(handler, renderer);
        }
        // end::tui-config[]
    }

    void tuiNoTick() {
        // tag::tui-no-tick[]
        var config = TuiConfig.builder()
            .noTick()
            .build();
        // end::tui-no-tick[]
    }

    void tuiResizeGracePeriod() {
        // tag::tui-resize-grace-period[]
        var config = TuiConfig.builder()
            .noTick()
            .resizeGracePeriod(Duration.ofMillis(100))  // Faster resize response
            .build();
        // end::tui-resize-grace-period[]
    }

    void errorHandlingDefault() throws Exception {
        EventHandler handler = (event, runner) -> false;
        Renderer renderer = frame -> {};
        // tag::error-handling-default[]
        // Default: errors display in UI, press 'q' to quit
        try (var tui = TuiRunner.create()) {
            tui.run(handler, renderer);
        }
        // end::error-handling-default[]
    }

    void errorHandlerConfig() throws Exception {
        // tag::error-handler-config[]
        // Log errors to a file and quit immediately
        var config = TuiConfig.builder()
            .errorHandler(RenderErrorHandlers.logAndQuit(new PrintStream("/tmp/tui-errors.log")))
            .build();

        // Write error details to a file, then show in-app error display
        var config2 = TuiConfig.builder()
            .errorHandler(RenderErrorHandlers.writeToFile(Path.of("/tmp/crash.log")))
            .build();
        // end::error-handler-config[]
    }

    void customErrorHandler() {
        // tag::custom-error-handler[]
        var config = TuiConfig.builder()
            .errorHandler((error, context) -> {
                // Log error details
                context.errorOutput().println("Error: " + error.message());
                error.cause().printStackTrace(context.errorOutput());
                // Return the action to take
                return ErrorAction.QUIT_IMMEDIATELY;
            })
            .build();
        // end::custom-error-handler[]
    }

    void renderThreadCheck() throws Exception {
        try (var tui = TuiRunner.create()) {
            TuiRunner runner = tui;
            // tag::render-thread-check[]
            // Check if on render thread
            if (runner.isRenderThread()) {
                // Safe to modify UI state
            }

            // Or use the static utility
            if (RenderThread.isRenderThread()) {
                // ...
            }
            // end::render-thread-check[]
        }
    }

    void runOnRenderThread() throws Exception {
        try (var tui = TuiRunner.create()) {
            TuiRunner runner = tui;
            // tag::run-on-render-thread[]
            // From a background thread:
            runner.runOnRenderThread(() -> {
                // This runs on the render thread
                // status = "Download complete";
                // UI will redraw on next event
            });
            // end::run-on-render-thread[]
        }
    }

    void runLater() throws Exception {
        try (var tui = TuiRunner.create()) {
            TuiRunner runner = tui;
            // tag::run-later[]
            // Always queues, never executes immediately
            runner.runLater(() -> {
                // Runs after current event handling completes
                processNextItem();
            });
            // end::run-later[]
        }
    }

    void processNextItem() {}

    void scheduledActions() throws Exception {
        try (var runner = ToolkitRunner.create()) {
            // tag::scheduled-actions[]
            // Action runs on scheduler thread - use runOnRenderThread for UI state
            runner.schedule(() -> {
                runner.runOnRenderThread(() -> {
                    // countdown--;
                });
            }, Duration.ofSeconds(1));

            // Same pattern for repeating actions
            runner.scheduleRepeating(() -> {
                runner.runOnRenderThread(() -> animationFrame++);
            }, Duration.ofMillis(16));
            // end::scheduled-actions[]
        }
    }

    void externalSchedulerInjection() throws Exception {
        EventHandler handler = (event, runner) -> false;
        Renderer renderer = frame -> {};
        // tag::external-scheduler-injection[]
        ScheduledExecutorService myScheduler = Executors.newSingleThreadScheduledExecutor();

        var config = TuiConfig.builder()
            .scheduler(myScheduler)  // Use externally-managed scheduler
            .build();

        try (var tui = TuiRunner.create(config)) {
            tui.run(handler, renderer);
        }

        // myScheduler is NOT shut down when TuiRunner closes - caller retains ownership
        myScheduler.shutdown();
        // end::external-scheduler-injection[]
    }

    void semanticKeyChecks() {
        KeyEvent key = null; // placeholder
        // tag::semantic-key-checks[]
        if (key.isQuit()) { }      // q, Q, Ctrl+C
        if (key.isUp()) { }        // Up arrow, k, Ctrl+P (with vim bindings)
        if (key.isDown()) { }      // Down arrow, j, Ctrl+N
        if (key.isSelect()) { }    // Enter, Space
        if (key.isCancel()) { }    // Escape
        if (key.isPageUp()) { }    // PageUp, Ctrl+B
        if (key.isPageDown()) { }  // PageDown, Ctrl+F
        // end::semantic-key-checks[]
    }

    void eventPatternMatching() {
        // tag::event-pattern-matching[]
        EventHandler handler = (event, runner) -> {
            if (event instanceof KeyEvent k) return handleKey(k);
            if (event instanceof MouseEvent m) return handleMouse(m);
            if (event instanceof TickEvent) { animationFrame++; return true; }
            if (event instanceof ResizeEvent) return true;  // Always redraw on resize
            return false;
        };
        // end::event-pattern-matching[]
    }

    void layoutInRenderer() {
        // tag::layout-in-renderer[]
        Renderer renderer = frame -> {
            var areas = Layout.vertical()
                .constraints(
                    Constraint.length(3),
                    Constraint.fill(),
                    Constraint.length(1)
                )
                .split(frame.area());

            frame.renderWidget(header, areas.get(0));
            frame.renderWidget(content, areas.get(1));
            frame.renderWidget(statusBar, areas.get(2));
        };
        // end::layout-in-renderer[]
    }

    // tag::counter-demo[]
    public class CounterDemo {
        private int counter = 0;
        private int ticks = 0;

        /**
         * Runs the demo application.
         *
         * @throws Exception if an error occurs
         */
         public void run() throws Exception {
            var config = TuiConfig.builder()
                .tickRate(Duration.ofMillis(100))
                .build();

            try (var tui = TuiRunner.create(config)) {
                tui.run(this::handleEvent, this::render);
            }
        }

        private boolean handleEvent(Event event, TuiRunner runner) {
            if (event instanceof KeyEvent key && key.isQuit()) {
                runner.quit();
                return false;
            }
            if (event instanceof TickEvent) {
                ticks++;
                return true;
            }
            if (event instanceof KeyEvent key) {
                if (key.isChar('+')) { counter++; return true; }
                if (key.isChar('-')) { counter--; return true; }
            }
            return false;
        }

        private void render(Frame frame) {
            var text = String.format("Counter: %d (ticks: %d)%n%nPress +/- to change, q to quit",
                counter, ticks);

            var widget = Paragraph.builder()
                .text(Text.from(text))
                .block(Block.builder()
                    .title("Demo")
                    .borders(Borders.ALL)
                    .borderType(BorderType.ROUNDED)
                    .build())
                .build();

            frame.renderWidget(widget, frame.area());
        }
    }
    // end::counter-demo[]

    // ========== TOOLKIT DSL ==========

    // tag::toolkit-app-example[]
    public static class MyApp extends ToolkitApp {

        @Override
        protected Element render() {
            return panel("My App",
                text("Hello!").bold().cyan(),
                spacer(),
                text("Press 'q' to quit").dim()
            ).rounded();
        }

        public static void main(String[] args) throws Exception {
            new MyApp().run();
        }
    }
    // end::toolkit-app-example[]

    void rowColumnLayout() {
        // tag::row-column-layout[]
        row(
            panel("Left").fill(),
            panel("Right").fill()
        );

        column(
            text("Header"),
            panel("Content").fill(),
            text("Footer")
        );
        // end::row-column-layout[]
    }

    void columnsLayout() {
        Element item1 = text("1"), item2 = text("2"), item3 = text("3"),
                item4 = text("4"), item5 = text("5"), item6 = text("6");
        // tag::columns-layout[]
        // Auto-detects column count based on available width and child widths
        columns(item1, item2, item3, item4, item5, item6)
            .spacing(1);

        // Explicit column count
        columns(item1, item2, item3, item4)
            .columnCount(2);

        // Column-first ordering (fills top-to-bottom, then left-to-right)
        columns(item1, item2, item3, item4)
            .columnCount(2)
            .columnFirst();
        // end::columns-layout[]
    }

    void spacerUsage() {
        // tag::spacer-usage[]
        row(
            text("Left"),
            spacer(),
            text("Right")
        );
        // end::spacer-usage[]
    }

    void flexPositioning() {
        // tag::flex-positioning[]
        row(
            text("Item 1"),
            text("Item 2"),
            text("Item 3")
        ).flex(Flex.CENTER);  // Center items horizontally

        column(
            panel("Top"),
            panel("Bottom")
        ).flex(Flex.SPACE_BETWEEN);  // Spread items vertically
        // end::flex-positioning[]
    }

    void stylingChain() {
        // tag::styling-chain[]
        text("Styled").bold().italic().cyan().onBlue();
        // end::styling-chain[]
    }

    void panelBorderStyling() {
        // tag::panel-border-styling[]
        panel("Title", text("content"))
            .rounded()
            .borderColor(Color.CYAN)
            .focusedBorderColor(Color.YELLOW);
        // end::panel-border-styling[]
    }

    void sizingOptions() {
        // tag::sizing-options[]
        panel("Fixed width").length(30);
        panel("Take what's left").fill();
        panel("Twice the weight").fill(2);
        panel("At least 10").min(10);
        panel("At most 50").max(50);
        // end::sizing-options[]
    }

    // tag::stateful-widgets[]
    private ListElement<?> myList = list("Apple", "Banana", "Cherry")
        .highlightColor(Color.CYAN)
        .autoScroll();

    private TableState tableState = new TableState();
    private TextInputState inputState = new TextInputState();

    Element statefulWidgetsExample() {
        // In render():
        return column(
            myList,  // ListElement manages selection and scroll internally

            table()
                .header("Name", "Age")
                .row("Alice", "30")
                .row("Bob", "25")
                .state(tableState),

            textInput(inputState)
                .placeholder("Type here...")
        );
    }
    // end::stateful-widgets[]

    void listElementNavigation() {
        int itemCount = 10;
        // tag::list-element-navigation[]
        myList.selectNext(itemCount);  // Move selection down
        myList.selectPrevious();       // Move selection up
        myList.selected();             // Get current selection index
        myList.selected(0);            // Set selection to specific index
        // end::list-element-navigation[]
    }

    void eventHandling() {
        // tag::event-handling[]
        panel("Interactive")
            .id("main")
            .focusable()
            .onKeyEvent(event -> {
                if (event.isChar('a')) {
                    addItem();
                    return EventResult.HANDLED;
                }
                return EventResult.UNHANDLED;
            });
        // end::event-handling[]
    }

    void addItem() {}

    void focusNavigation() {
        // tag::focus-navigation[]
        column(
            panel("First").id("first").focusable(),
            panel("Second").id("second").focusable()
        );
        // end::focus-navigation[]
    }

    void dataVisualization() {
        // tag::data-visualization[]
        gauge(0.75).label("Progress").gaugeColor(Color.GREEN);

        sparkline(1, 4, 2, 8, 5, 7).color(Color.CYAN);

        barChart(10, 20, 30)
            .barColor(Color.BLUE);
        // end::data-visualization[]
    }

    void widgetWrapper() {
        Widget customWidget1 = null;
        Widget customWidget2 = null;
        Widget myCustomWidget = null;
        // tag::widget-wrapper[]
        // Wrap any Widget for use in the Toolkit DSL
        widget(myCustomWidget)
            .addClass("custom")
            .fill();

        // Use in layouts like any other element
        row(
            widget(customWidget1).fill(),
            widget(customWidget2).fill()
        );
        // end::widget-wrapper[]
    }

    void toolkitRunnerDirect() throws Exception {
        Element content = text("content");
        // tag::toolkit-runner-direct[]
        var config = TuiConfig.builder()
            .mouseCapture(true)
            .tickRate(Duration.ofMillis(50))
            .build();

        try (var runner = ToolkitRunner.create(config)) {
            runner.run(() -> panel("App", content));
        }
        // end::toolkit-runner-direct[]
    }

    void faultTolerantRendering() throws Exception {
        // tag::fault-tolerant-rendering[]
        try (var runner = ToolkitRunner.builder()
                .faultTolerant(true)
                .build()) {
            runner.run(() -> render());
        }
        // end::fault-tolerant-rendering[]
    }

    Element render() { return text(""); }

    // tag::todo-app[]
    public static class TodoApp extends ToolkitApp {
        private final List<String> items = new ArrayList<>(List.of(
            "Learn TamboUI",
            "Build something cool"
        ));
        private final ListElement<?> todoList = list()
            .highlightColor(Color.CYAN)
            .autoScroll();

        @Override
        protected Element render() {
            return panel("Todo",
                items.isEmpty()
                    ? text("Empty - press 'a' to add").dim()
                    : todoList.items(items.toArray(new String[0])),
                spacer(),
                text("[a]dd [d]elete [q]uit").dim()
            )
            .rounded()
            .id("main")
            .focusable()
            .onKeyEvent(this::handleKey);
        }

        private EventResult handleKey(KeyEvent event) {
            if (event.isChar('a')) {
                items.add("New item");
                return EventResult.HANDLED;
            }
            if (event.isChar('d') && !items.isEmpty()) {
                items.remove(todoList.selected());
                return EventResult.HANDLED;
            }
            if (event.isDown()) {
                todoList.selectNext(items.size());
                return EventResult.HANDLED;
            }
            if (event.isUp()) {
                todoList.selectPrevious();
                return EventResult.HANDLED;
            }
            return EventResult.UNHANDLED;
        }

        public static void main(String[] args) throws Exception {
            new TodoApp().run();
        }
    }
    // end::todo-app[]

    // ========== INLINE DISPLAY MODE ==========

    void inlineDisplayBasic() throws Exception {
        // tag::inline-display-basic[]
        try (var display = InlineDisplay.create(3)) {  // Reserve 3 lines
            for (int i = 0; i <= 100; i += 10) {
                final int progress = i;
                display.render((area, buffer) -> {
                    var gauge = Gauge.builder()
                        .ratio(progress / 100.0)
                        .label("Processing: " + progress + "%")
                        .build();
                    gauge.render(area, buffer);
                });
                Thread.sleep(100);
            }
        }
        // end::inline-display-basic[]
    }

    void inlinePrintln() throws Exception {
        // tag::inline-println[]
        try (var display = InlineDisplay.create(4)) {
            for (var task : tasks) {
                // Update status area
                display.render((area, buffer) -> {
                    renderProgress(area, buffer, task, progress);
                });

                processTask(task);

                // Log completion - scrolls above, status stays fixed
                display.println(Text.from(Line.from(
                    Span.styled("OK ", Style.EMPTY.fg(Color.GREEN)),
                    Span.raw(task.name())
                )));
            }
        }
        // end::inline-println[]
    }

    void inlineDisplayConfig() throws IOException {
        // tag::inline-display-config[]
        // Fixed height (width matches terminal)
        var display = InlineDisplay.create(4);

        // Fixed height and width
        var display2 = InlineDisplay.create(4, 80);

        // Clear the status area when done
        var display3 = InlineDisplay.create(4).clearOnClose();
        // end::inline-display-config[]
    }

    void inlineSetLine() throws Exception {
        try (var display = InlineDisplay.create(4)) {
            // tag::inline-set-line[]
            display.setLine(0, "Building module: core");
            display.setLine(1, "Progress: 45%");
            display.setLine(2, Text.from(Span.styled("No errors", Style.EMPTY.fg(Color.GREEN))));
            // end::inline-set-line[]
        }
    }

    // ========== INLINE TUI RUNNER ==========

    void inlineTuiRunnerBasic() throws Exception {
        // tag::inline-tui-runner-basic[]
        try (var runner = InlineTuiRunner.create(4)) {
            runner.run(
                // Event handler
                (event, r) -> {
                    if (event instanceof KeyEvent key && key.character() == 'q') {
                        r.quit();
                        return true;
                    }
                    return false;
                },
                // Renderer
                frame -> {
                    var gauge = Gauge.builder()
                        .ratio(progress / 100.0)
                        .build();
                    gauge.render(frame.area(), frame.buffer());
                }
            );
        }
        // end::inline-tui-runner-basic[]
    }

    void inlineTuiConfig() throws Exception {
        // tag::inline-tui-config[]
        var config = InlineTuiConfig.builder(4)  // 4 lines
            .tickRate(Duration.ofMillis(50))     // For animations
            .clearOnClose(true)                   // Clear viewport on exit
            .build();

        try (var runner = InlineTuiRunner.create(config)) {
            // ...
        }
        // end::inline-tui-config[]
    }

    void inlineRunnerPrintln() throws Exception {
        try (var runner = InlineTuiRunner.create(4)) {
            // tag::inline-runner-println[]
            // Plain text
            runner.println("Task completed!");

            // Styled text
            runner.println(Text.from(Line.from(
                Span.styled("OK ", Style.EMPTY.fg(Color.GREEN)),
                Span.raw("Build successful")
            )));
            // end::inline-runner-println[]
        }
    }

    void inlineThreadSafeUpdates() throws Exception {
        try (var runner = InlineTuiRunner.create(4)) {
            // tag::inline-thread-safe-updates[]
            // Run code on the render thread
            runner.runOnRenderThread(() -> {
                // progress = newValue;
            });

            // Schedule for later
            runner.runLater(() -> {
                cleanup();
            });
            // end::inline-thread-safe-updates[]
        }
    }

    void cleanup() {}

    // ========== INLINE TOOLKIT ==========

    // tag::inline-app-example[]
    public static class MyProgressApp extends InlineApp {

        private double progress = 0.0;

        public static void main(String[] args) throws Exception {
            new MyProgressApp().run();
        }

        @Override
        protected int height() {
            return 3;  // Reserve 3 lines
        }

        @Override
        protected Element render() {
            return column(
                text("Installing packages...").bold(),
                gauge(progress).green(),
                text(String.format("%.0f%% complete", progress * 100)).dim()
            );
        }

        @Override
        protected void onStart() {
            // Schedule periodic updates
            runner().scheduleRepeating(() -> {
                runner().runOnRenderThread(() -> {
                    progress += 0.01;
                    if (progress >= 1.0) quit();
                });
            }, Duration.ofMillis(50));
        }
    }
    // end::inline-app-example[]

    // tag::inline-app-configure[]
    public static class MyConfiguredInlineApp extends InlineApp {
        @Override
        protected int height() { return 3; }

        @Override
        protected Element render() { return text(""); }

        @Override
        protected InlineTuiConfig configure(int height) {
            return InlineTuiConfig.builder(height)
                .tickRate(Duration.ofMillis(30))  // Faster for smooth animations
                .clearOnClose(false)               // Keep output visible
                .build();
        }
    }
    // end::inline-app-configure[]

    void inlinePrintElements() {
        // tag::inline-print-elements[]
        // Print a styled element
        println(row(
            text("OK ").green().fit(),
            text("lodash").fit(),
            text("@4.17.21").dim().fit()
        ).flex(Flex.START));

        // Print plain text
        println("Step completed");
        // end::inline-print-elements[]
    }

    void println(Element element) {}
    void println(String text) {}
    void startNextPhase() {}

    // tag::inline-key-handling[]
    public static class MyInteractiveInlineApp extends InlineApp {
        @Override
        protected int height() { return 3; }

        @Override
        protected Element render() {
            return column(
                text("Continue? [Y/n]").cyan(),
                spacer()
            )
            .focusable()
            .onKeyEvent(event -> {
                if (event.character() == 'y' || event.character() == 'Y') {
                    startNextPhase();
                    return EventResult.HANDLED;
                } else if (event.character() == 'n') {
                    quit();
                    return EventResult.HANDLED;
                }
                return EventResult.UNHANDLED;
            });
        }

        void startNextPhase() {}
    }
    // end::inline-key-handling[]

    // tag::inline-text-input[]
    public static class MyFormInlineApp extends InlineApp {
        private final TextInputState nameState = new TextInputState();

        @Override
        protected int height() { return 3; }

        @Override
        protected Element render() {
            return column(
                row(
                    text("Name: ").bold().fit(),
                    textInput(nameState)
                        .id("name-input")
                        .placeholder("Enter name...")
                        .constraint(Constraint.length(20))
                        .onSubmit(() -> handleSubmit(nameState.text()))
                ).flex(Flex.START),
                text("[Enter] Submit  [Tab] Next field").dim()
            );
        }

        private void handleSubmit(String value) {}
    }
    // end::inline-text-input[]

    void inlineToolkitRunnerDirect() throws Exception {
        // tag::inline-toolkit-runner-direct[]
        try (var runner = InlineToolkitRunner.create(3)) {
            runner.run(() -> column(
                waveText("Processing...").cyan(),
                gauge(progress),
                text("Please wait").dim()
            ));
        }
        // end::inline-toolkit-runner-direct[]
    }

    // tag::inline-scopes[]
    public static class MyScopedInlineApp extends InlineApp {
        private boolean downloading = true;
        private double progress1 = 0;
        private double progress2 = 0;

        @Override
        protected int height() { return 5; }

        @Override
        protected Element render() {
            return column(
                text("Package Installation").bold(),

                // This section collapses when downloading becomes false
                scope(downloading,
                    row(text("file1.zip: "), gauge(progress1)),
                    row(text("file2.zip: "), gauge(progress2))
                ),

                text(downloading ? "Downloading..." : "Complete!").dim()
            );
        }
    }
    // end::inline-scopes[]
}
