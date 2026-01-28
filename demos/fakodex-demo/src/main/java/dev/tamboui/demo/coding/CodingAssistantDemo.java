//DEPS dev.tamboui:tamboui-toolkit:LATEST
//DEPS dev.tamboui:tamboui-jline3-backend:LATEST
//FILES styles/codex.tcss=../../../../../resources/styles/codex.tcss

/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.demo.coding;

import dev.tamboui.css.engine.StyleEngine;
import dev.tamboui.layout.Constraint;
import dev.tamboui.style.Color;
import dev.tamboui.style.Style;
import dev.tamboui.toolkit.app.ToolkitRunner;
import dev.tamboui.toolkit.element.Element;
import dev.tamboui.toolkit.element.StyledElement;
import dev.tamboui.toolkit.event.EventResult;
import dev.tamboui.tui.TuiConfig;
import dev.tamboui.tui.bindings.Actions;
import dev.tamboui.tui.event.KeyCode;
import dev.tamboui.tui.event.KeyEvent;
import dev.tamboui.widgets.input.TextInputState;
import dev.tamboui.widgets.wavetext.WaveTextState;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static dev.tamboui.toolkit.Toolkit.*;

/**
 * A Codex/Claude Code-like AI coding assistant demo with linear conversation flow.
 * <p>
 * This demo showcases a terminal UI similar to modern AI coding assistants with:
 * <ul>
 *   <li>Linear conversation flow with inline tool calls</li>
 *   <li>Animated spinners for active operations</li>
 *   <li>Code blocks and diffs displayed inline</li>
 *   <li>CSS styling with the toolkit module</li>
 *   <li>Key bindings for navigation</li>
 *   <li>Modern Java constructs (records, var, switch expressions)</li>
 * </ul>
 */
public class CodingAssistantDemo {

    // Theme colors - muted, professional palette
    private static final Color CYAN = Color.rgb(0, 180, 216);
    private static final Color GREEN = Color.rgb(46, 204, 113);
    private static final Color YELLOW = Color.rgb(241, 196, 15);
    private static final Color RED = Color.rgb(231, 76, 60);
    private static final Color MAGENTA = Color.rgb(155, 89, 182);
    private static final Color DIM = Color.rgb(127, 140, 141);
    private static final Color BRIGHT = Color.rgb(236, 240, 241);

    // Animation constants
    private static final String[] SPINNER_FRAMES = {"⠋", "⠙", "⠹", "⠸", "⠼", "⠴", "⠦", "⠧", "⠇", "⠏"};
    private static final int SPINNER_FRAME_DIVISOR = 2;
    private static final long TICK_RATE_MS = 50;
    private static final long RESPONSE_DELAY_MS = 1500;

    // Wave effect settings
    private static final double THINKING_WAVE_SPEED = 1.5;
    private static final int THINKING_WAVE_PEAK_WIDTH = 3;

    // Typing animation settings
    private static final int TYPING_MIN_CHARS = 2;
    private static final int TYPING_RANDOM_CHARS = 2;

    // Conversation lines for display
    private final List<StyledLine> lines = new ArrayList<>();
    private final TextInputState inputState = new TextInputState();
    private final Random random = new Random();

    private long tickCount = 0;
    private boolean isProcessing = false;
    private ToolkitRunner runner;

    // Active tool calls for animation
    private final List<ActiveToolCall> activeToolCalls = new ArrayList<>();

    // Active thinking indicator text (null when not thinking)
    private String activeThinkingText = null;
    private final WaveTextState thinkingWaveState = new WaveTextState();

    // Pending typing for AI responses
    private PendingTyping pendingTyping = null;

    // Pending action to run after typing completes
    private Runnable afterTypingAction = null;

    /**
     * A styled line with text and color.
     */
    private record StyledLine(String content, Color color, boolean bold) {
        StyledElement<?> toElement() {
            var style = Style.EMPTY.fg(color);
            if (bold) {
                style = style.bold();
            }
            return text(content).style(style);
        }

        static StyledLine of(String text) {
            return new StyledLine(text, BRIGHT, false);
        }

        static StyledLine of(String text, Color color) {
            return new StyledLine(text, color, false);
        }

        static StyledLine bold(String text, Color color) {
            return new StyledLine(text, color, true);
        }
    }

    /**
     * Tool call types.
     */
    private enum ToolType {
        READ_FILE, WRITE_FILE, EDIT_FILE, BASH, SEARCH
    }

    /**
     * An active tool call being animated.
     */
    private static class ActiveToolCall {
        final int lineIndex;
        final int startTick;
        final int durationTicks;
        final String completedText;
        final ToolType type;
        final String description;  // Store description to avoid parsing

        ActiveToolCall(int lineIndex, int startTick, int durationTicks, String completedText, ToolType type, String description) {
            this.lineIndex = lineIndex;
            this.startTick = startTick;
            this.durationTicks = durationTicks;
            this.completedText = completedText;
            this.type = type;
            this.description = description;
        }

        boolean isComplete(long currentTick) {
            return currentTick - startTick >= durationTicks;
        }
    }

    /**
     * Pending AI response typing animation.
     */
    private static class PendingTyping {
        final String fullText;
        int currentIndex;
        final int startLineIndex;

        PendingTyping(String fullText, int startLineIndex) {
            this.fullText = fullText;
            this.currentIndex = 0;
            this.startLineIndex = startLineIndex;
        }

        boolean isComplete() {
            return currentIndex >= fullText.length();
        }

        void advance(int chars) {
            currentIndex = Math.min(currentIndex + chars, fullText.length());
        }

        String currentText() {
            return fullText.substring(0, currentIndex);
        }
    }


    /**
     * Main entry point.
     *
     * @param args command line arguments
     * @throws Exception if an error occurs
     */
    public static void main(String[] args) throws Exception {
        new CodingAssistantDemo().run();
    }

    private CodingAssistantDemo() {

    }

    /**
     * Runs the demo application.
     *
     * @throws Exception if an error occurs
     */
    public void run() throws Exception {
        var styleEngine = createStyleEngine();
        var config = TuiConfig.builder()
                .mouseCapture(true)
                .tickRate(Duration.ofMillis(TICK_RATE_MS))
                .build();

        // Add welcome message
        addWelcomeMessage();

        try (var runner = ToolkitRunner.builder()
                .config(config)
                .styleEngine(styleEngine)
                .build()) {
            this.runner = runner;
            runner.run(this::render);
        }
    }

    /**
     * Adds the welcome message to the conversation.
     */
    private void addWelcomeMessage() {
        lines.add(StyledLine.of(""));
        lines.add(StyledLine.bold("  Welcome to Fakodex!", CYAN));
        lines.add(StyledLine.of("  I'm a fake AI coding assistant. I can help you write code,"));
        lines.add(StyledLine.of("  fix bugs, and explore your codebase."));
        lines.add(StyledLine.of(""));
        lines.add(StyledLine.of("  Try typing: \"create a hello world function\"", DIM));
        lines.add(StyledLine.of("  Or press Ctrl+D for demo, Ctrl+H for help", DIM));
        lines.add(StyledLine.of(""));
    }

    /**
     * Creates and configures the style engine.
     *
     * @return the configured style engine
     * @throws IOException if stylesheet loading fails
     */
    private StyleEngine createStyleEngine() throws IOException {
        var engine = StyleEngine.create();
        engine.loadStylesheet("/styles/codex.tcss");
        return engine;
    }

    /**
     * Renders the main UI.
     *
     * @return the root element
     */
    private Element render() {
        tickCount++;
        var spinner = getSpinner();

        // Update animations
        updateToolCalls(spinner);
        updateTyping();

        // Status indicator
        var status = isProcessing ? "working..." : "ready";
        var statusColor = isProcessing ? YELLOW : GREEN;

        // Build header as a single line
        var workingDirectory = "~/projects/my-app";
        var headerText = " fakodex michel-tambouille-4-5 | " + workingDirectory;

        // Build input prompt
        var prompt = isProcessing ? " ... " : " > ";

        // Build the column content
        var columnChildren = new ArrayList<Element>();

        // Header line
        columnChildren.add(
                row(
                        text(headerText).fg(DIM),
                        spacer(),
                        text("[" + status + "] ").fg(statusColor)
                ).constraint(Constraint.length(1))
        );

        // Conversation area
        columnChildren.add(
                list()
                        .data(lines, StyledLine::toElement)
                        .displayOnly()
                        .scrollToEnd()
                        .constraint(Constraint.fill())
        );

        // Active thinking indicator with wave effect (shown below list, above input)
        if (activeThinkingText != null && pendingTyping == null) {
            columnChildren.add(
                    waveText(activeThinkingText, CYAN)
                            .speed(THINKING_WAVE_SPEED)
                            .peakWidth(THINKING_WAVE_PEAK_WIDTH)
                            .state(thinkingWaveState)
                            .constraint(Constraint.length(1))
            );
        }

        // Active tool calls
        for (var tc : activeToolCalls) {
            var label = switch (tc.type) {
                case READ_FILE -> "Read";
                case WRITE_FILE -> "Write";
                case EDIT_FILE -> "Edit";
                case BASH -> "Bash";
                case SEARCH -> "Search";
            };
            var toolText = "  [" + spinner + "] " + label + " " + tc.description;
            columnChildren.add(
                    text(toolText).fg(YELLOW).constraint(Constraint.length(1))
            );
        }

        // Input line
        columnChildren.add(
                row(
                        text(prompt).fg(isProcessing ? YELLOW : GREEN).bold(),
                        textInput(inputState)
                                .placeholder(isProcessing ? "" : "Ask anything...")
                                .showCursor(!isProcessing)
                                .cursorRequiresFocus(false)
                                .onSubmit(this::submitInput)
                                .constraint(Constraint.fill())
                ).constraint(Constraint.length(1))
        );

        return panel(
                column(columnChildren.toArray(new Element[0]))
        ).id("main").focusable().onKeyEvent(this::handleKey);
    }


    /**
     * Handles key events.
     *
     * @param event the key event
     * @return the event result
     */
    private EventResult handleKey(KeyEvent event) {
        // Don't process input while working
        if (isProcessing) {
            return EventResult.HANDLED;
        }

        // Submit on Enter
        if (event.matches(Actions.CONFIRM)) {
            submitInput();
            return EventResult.HANDLED;
        }

        // Ctrl+D for demo
        if (event.code() == KeyCode.CHAR && event.character() == 'd' && event.hasCtrl()) {
            runDemo();
            return EventResult.HANDLED;
        }

        // Ctrl+H for help
        if (event.code() == KeyCode.CHAR && event.character() == 'h' && event.hasCtrl()) {
            showHelp();
            return EventResult.HANDLED;
        }

        // Ctrl+L to clear
        if (event.code() == KeyCode.CHAR && event.character() == 'l' && event.hasCtrl()) {
            lines.clear();
            activeToolCalls.clear();
            activeThinkingText = null;
            thinkingWaveState.reset();
            pendingTyping = null;
            addWelcomeMessage();
            return EventResult.HANDLED;
        }

        // Clear input on Escape
        if (event.matches(Actions.CANCEL) && !inputState.text().isEmpty()) {
            inputState.clear();
            return EventResult.HANDLED;
        }

        // Handle text input - this processes characters
        if (handleTextInputKey(inputState, event)) {
            return EventResult.HANDLED;
        }

        return EventResult.UNHANDLED;
    }

    /**
     * Submits the current input.
     */
    private void submitInput() {
        var text = inputState.text().trim();
        if (text.isEmpty()) {
            return;
        }

        // Add user message
        lines.add(StyledLine.of(""));
        lines.add(StyledLine.bold("> " + text, GREEN));

        inputState.clear();
        processUserInput(text);
    }

    /**
     * Processes user input and generates a response.
     *
     * @param input the user's input
     */
    private void processUserInput(String input) {
        var lowerInput = input.toLowerCase();

        if (lowerInput.contains("create") || lowerInput.contains("write")) {
            handleCreateRequest();
        } else if (lowerInput.contains("fix") || lowerInput.contains("bug")) {
            handleFixRequest();
        } else if (lowerInput.contains("explain")) {
            handleExplainRequest();
        } else if (lowerInput.contains("search") || lowerInput.contains("find")) {
            handleSearchRequest();
        } else if (lowerInput.contains("run") || lowerInput.contains("test")) {
            handleRunRequest();
        } else {
            handleGenericRequest();
        }
    }

    /**
     * Sets the thinking indicator with wave effect.
     *
     * @param text the thinking text (e.g., "Thinking...", "Analyzing...")
     */
    private void setThinkingIndicator(String text) {
        lines.add(StyledLine.of(""));
        activeThinkingText = "  " + text;
        thinkingWaveState.reset();
    }

    /**
     * Handles a create/write code request.
     */
    private void handleCreateRequest() {
        isProcessing = true;

        setThinkingIndicator("Thinking...");

        // Add tool calls
        addToolCall(ToolType.SEARCH, "existing patterns", 30, "Found 3 similar implementations");
        addToolCall(ToolType.READ_FILE, "src/main/java/App.java", 25, "Read 45 lines");

        // Schedule response, then show code block after typing completes
        scheduleResponse("I'll create that for you. Here's the implementation:");
        afterTypingAction = this::showCodeBlock;
    }

    /**
     * Handles a fix/debug request.
     */
    private void handleFixRequest() {
        isProcessing = true;

        setThinkingIndicator("Analyzing...");

        addToolCall(ToolType.READ_FILE, "src/main/java/Service.java", 20, "Read 120 lines");
        addToolCall(ToolType.SEARCH, "null pointer", 35, "Found issue at line 42");

        scheduleResponse("I found the issue. The null check is missing:");
        afterTypingAction = this::showDiffBlock;
    }

    /**
     * Handles an explain request.
     */
    private void handleExplainRequest() {
        isProcessing = true;

        setThinkingIndicator("Reading code...");

        addToolCall(ToolType.READ_FILE, "src/main/java/Utils.java", 25, "Read 80 lines");

        var explanation = """
                This code implements a caching layer:
                
                1. Cache lookup - checks if data exists in memory
                2. Fallback - fetches from database if not cached
                3. Storage - stores result for future requests
                
                The TTL ensures stale data is refreshed.""";

        scheduleResponse(explanation);
    }

    /**
     * Handles a search request.
     */
    private void handleSearchRequest() {
        isProcessing = true;

        setThinkingIndicator("Searching...");

        addToolCall(ToolType.SEARCH, "config files", 30, "Found 5 matches");

        var response = """
                Found these configuration files:
                
                - config/settings.yaml - Main settings
                - config/database.yaml - DB connection
                - .env - Environment variables
                - docker-compose.yml - Container config""";

        scheduleResponse(response);
    }

    /**
     * Handles a run/test request.
     */
    private void handleRunRequest() {
        isProcessing = true;

        setThinkingIndicator("Running tests...");

        addToolCall(ToolType.BASH, "npm test", 50, "12 passed, 0 failed");

        var response = """
                All tests passing!
                
                Test Suites: 3 passed
                Tests: 12 passed
                Time: 2.34s""";

        scheduleResponse(response);
    }

    /**
     * Handles a generic request.
     */
    private void handleGenericRequest() {
        isProcessing = true;

        setThinkingIndicator("Thinking...");

        var response = """
                I can help with:
                
                - Create code - write functions/classes
                - Fix bugs - analyze and repair issues
                - Explain - break down how code works
                - Search - find files and patterns
                - Run - execute tests or commands
                
                What would you like me to do?""";

        scheduleResponse(response);
    }

    /**
     * Adds a tool call with animation.
     *
     * @param type        the tool type
     * @param description the tool description
     * @param duration    the animation duration in ticks
     * @param result      the result to show when complete
     */
    private void addToolCall(ToolType type, String description, int duration, String result) {
        var label = switch (type) {
            case READ_FILE -> "Read";
            case WRITE_FILE -> "Write";
            case EDIT_FILE -> "Edit";
            case BASH -> "Bash";
            case SEARCH -> "Search";
        };

        activeToolCalls.add(new ActiveToolCall(lines.size(), (int) tickCount, duration,
                "  [+] " + label + " " + description + " - " + result, type, description));
    }

    /**
     * Schedules an AI response to start typing after a delay.
     *
     * @param response the response text
     */
    private void scheduleResponse(String response) {
        // Start typing after tool calls have had time to show
        runner.schedule(() -> {
            // Clear thinking indicator when typing starts
            activeThinkingText = null;
            pendingTyping = new PendingTyping(response, lines.size());
        }, Duration.ofMillis(RESPONSE_DELAY_MS));
    }

    /**
     * Shows the code block after typing completes.
     */
    private void showCodeBlock() {
        lines.add(StyledLine.of(""));
        lines.add(StyledLine.bold("  Greeter.java", MAGENTA));
        lines.add(StyledLine.of("    1  public String greet(String name) {"));
        lines.add(StyledLine.of("    2      if (name == null || name.isBlank()) {"));
        lines.add(StyledLine.of("    3          return \"Hello, World!\";"));
        lines.add(StyledLine.of("    4      }"));
        lines.add(StyledLine.of("    5      return \"Hello, \" + name + \"!\";"));
        lines.add(StyledLine.of("    6  }"));
        addToolCall(ToolType.WRITE_FILE, "src/main/java/Greeter.java", 15, "Created file");
    }

    /**
     * Shows a diff block after typing completes.
     */
    private void showDiffBlock() {
        lines.add(StyledLine.of(""));
        lines.add(StyledLine.bold("  Service.java (diff)", MAGENTA));
        lines.add(StyledLine.of("  - String result = data.process();", RED));
        lines.add(StyledLine.of("  + String result = data != null ? data.process() : \"\";", GREEN));
        lines.add(StyledLine.of("    return result;", DIM));
        addToolCall(ToolType.EDIT_FILE, "src/main/java/Service.java", 15, "Applied fix");
    }

    /**
     * Runs the demo sequence.
     */
    private void runDemo() {
        lines.clear();
        addWelcomeMessage();
        inputState.setText("create a greeting function");
        submitInput();
    }

    /**
     * Shows help information.
     */
    private void showHelp() {
        lines.add(StyledLine.of(""));
        lines.add(StyledLine.bold("  Keyboard Shortcuts", CYAN));
        lines.add(StyledLine.of(""));
        lines.add(StyledLine.of("  Enter     - Send message"));
        lines.add(StyledLine.of("  Escape    - Clear input"));
        lines.add(StyledLine.of("  Ctrl+D    - Run demo"));
        lines.add(StyledLine.of("  Ctrl+H    - Show this help"));
        lines.add(StyledLine.of("  Ctrl+L    - Clear conversation"));
        lines.add(StyledLine.of("  Ctrl+C/q  - Quit"));
        lines.add(StyledLine.of(""));
        lines.add(StyledLine.of("  Try: create, fix, explain, search, run", DIM));
        lines.add(StyledLine.of(""));
    }

    /**
     * Updates tool call animations.
     *
     * @param spinner the current spinner (unused, kept for compatibility)
     */
    private void updateToolCalls(String spinner) {
        var completed = new ArrayList<ActiveToolCall>();

        for (var tc : activeToolCalls) {
            if (tc.isComplete(tickCount)) {
                // Add completed tool call to the conversation
                lines.add(StyledLine.of(tc.completedText, GREEN));
                completed.add(tc);
            }
        }

        activeToolCalls.removeAll(completed);

        // Check if all processing is done
        if (activeToolCalls.isEmpty() && pendingTyping == null && activeThinkingText == null) {
            isProcessing = false;
        }
    }

    /**
     * Updates typing animation.
     */
    private void updateTyping() {
        if (pendingTyping == null) {
            return;
        }

        // Type characters
        pendingTyping.advance(TYPING_MIN_CHARS + random.nextInt(TYPING_RANDOM_CHARS));

        // Update the lines with current typed text
        var typedText = pendingTyping.currentText();
        var typedLines = typedText.split("\n", -1);

        // Remove old lines from thinking onwards
        while (lines.size() > pendingTyping.startLineIndex) {
            lines.remove(lines.size() - 1);
        }

        // Add typed lines
        for (var line : typedLines) {
            lines.add(StyledLine.of("  " + line));
        }

        if (pendingTyping.isComplete()) {
            pendingTyping = null;
            // Run any pending action after typing completes
            if (afterTypingAction != null) {
                var action = afterTypingAction;
                afterTypingAction = null;
                action.run();
            }
        }
    }

    /**
     * Gets the current spinner character.
     *
     * @return the spinner character
     */
    private String getSpinner() {
        return SPINNER_FRAMES[(int) ((tickCount / SPINNER_FRAME_DIVISOR) % SPINNER_FRAMES.length)];
    }

}
