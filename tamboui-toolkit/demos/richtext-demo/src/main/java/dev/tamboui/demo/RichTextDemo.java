//DEPS dev.tamboui:tamboui-toolkit:LATEST
//DEPS dev.tamboui:tamboui-jline:LATEST
/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.demo;

import dev.tamboui.css.engine.StyleEngine;
import dev.tamboui.css.parser.CssParseException;
import dev.tamboui.layout.Constraint;
import dev.tamboui.layout.Rect;
import dev.tamboui.style.Color;
import dev.tamboui.style.Style;
import dev.tamboui.terminal.Frame;
import dev.tamboui.toolkit.app.ToolkitRunner;
import dev.tamboui.toolkit.element.Element;
import dev.tamboui.toolkit.element.RenderContext;
import dev.tamboui.toolkit.elements.MarkupTextAreaElement;
import dev.tamboui.toolkit.elements.RichTextAreaElement;
import dev.tamboui.tui.TuiConfig;
import dev.tamboui.tui.bindings.ActionHandler;
import dev.tamboui.tui.bindings.BindingSets;
import dev.tamboui.tui.bindings.KeyTrigger;
import dev.tamboui.tui.event.KeyCode;
import dev.tamboui.widgets.input.TextAreaState;
import dev.tamboui.widgets.tabs.TabsState;
import dev.tamboui.text.MarkupParser;

import static dev.tamboui.toolkit.Toolkit.*;

/**
 * Demo showcasing the RichText and MarkupText components.
 * <p>
 * Features demonstrated:
 * <ul>
 *   <li>Live markup editor with rendered preview</li>
 *   <li>CSS editor for custom tag styling</li>
 *   <li>Tabbed interface</li>
 *   <li>Scrolling with keyboard and mouse</li>
 * </ul>
 * <p>
 * Controls:
 * <ul>
 *   <li>Tab - Switch focus between panels</li>
 *   <li>Arrow keys - Navigate/scroll content</li>
 *   <li>Page Up/Down - Scroll by page</li>
 *   <li>Mouse scroll - Scroll content</li>
 *   <li>Ctrl+C - Quit</li>
 * </ul>
 */
public class RichTextDemo implements Element {

    private static final String INITIAL_CSS = """
            /* Custom tag styles */
            /* These will be applied to markup tags via TCSS */

            .keyword {
                color: magenta;
                text-style: bold;
            }

            .string {
                color: green;
            }

            .comment {
                color: gray;
                text-style: italic;
            }

            .type {
                color: yellow;
            }

            .error {
                color: red;
                text-style: underlined;
            }""";
    private final StyleEngine styleEngine;
    private final TabsState tabsState;
    private final TextAreaState markupEditorState;
    private final TextAreaState cssEditorState;
    private final MarkupTextAreaElement previewElement;
    private String lastAppliedCss = "";
    private String cssError = null;

    public RichTextDemo() {
        // Initialize style engine
        styleEngine = StyleEngine.create();

        // Initialize tab state with first tab selected
        tabsState = new TabsState(0);

        // Initialize markup editor with sample content
        markupEditorState = new TextAreaState(getInitialMarkup());

        // Initialize CSS editor with sample styles
        cssEditorState = new TextAreaState(INITIAL_CSS);

        applyUserCss(INITIAL_CSS);

        // Create preview element once (state persists across renders)
        previewElement = new MarkupTextAreaElement()
                .wrapWord()
                .scrollbar(RichTextAreaElement.ScrollBarPolicy.AS_NEEDED)
                .rounded()
                .focusable()
                .focusedBorderColor(Color.CYAN)
                .id("preview")
                .fill();
    }

    public static void main(String[] args) throws Exception {
        var demo = new RichTextDemo();
        demo.run();
    }

    public void run() throws Exception {
        // Create bindings with F1/F2 for tab switching
        var bindings = BindingSets.standard()
                .toBuilder()
                .bind(KeyTrigger.key(KeyCode.F1), "selectMarkupTab")
                .bind(KeyTrigger.key(KeyCode.F2), "selectCssTab")
                .build();

        var config = TuiConfig.builder()
                .mouseCapture(true)
                .noTick()
                .build();

        try (var runner = ToolkitRunner.builder()
                .config(config)
                .bindings(bindings)
                .build()) {

            // Register global handler for tab switching
            var globalHandler = new ActionHandler(bindings)
                    .on("selectMarkupTab", e -> tabsState.select(0))
                    .on("selectCssTab", e -> tabsState.select(1));
            runner.eventRouter().addGlobalHandler(globalHandler);

            // Set style engine for CSS support
            runner.styleEngine(styleEngine);

            runner.run(() -> this);
        }
    }

    @Override
    public void render(Frame frame, Rect area, RenderContext context) {
        // Apply CSS if it changed
        String currentCss = cssEditorState.text();
        if (!currentCss.equals(lastAppliedCss)) {
            applyUserCss(currentCss);
            lastAppliedCss = currentCss;
        }

        boolean showMarkupEditor = tabsState.selected() == 0;

        column(
                // Header panel with styled title
                panel(() -> row(
                        tabs("Markup", "CSS")
                                .state(tabsState)
                                .highlightStyle(Style.EMPTY.fg(Color.CYAN).bold())
                                .focusable()
                                .id("tabs")
                                .length(12),
                        spacer(),
                        text(" [F1/F2] Tabs ").dim(),
                        text(" [Tab] Focus ").dim(),
                        text(" [Ctrl+C] Quit ").dim()
                )).title(MarkupParser.parse("[bold][green]Rich [/green][red]live[/red][green] editor[/green][/bold]").lines().getFirst())
                  .rounded()
                  .length(3),

                // Main content - editor left, preview right
                row(
                        // Left - Editor (Markup or CSS based on selected tab)
                        showMarkupEditor
                                ? textArea(markupEditorState)
                                .title("Markup Editor")
                                .showLineNumbers()
                                .lineNumberStyle(Style.EMPTY.fg(Color.GRAY))
                                .rounded()
                                .focusedBorderColor(Color.CYAN)
                                .id("markup-editor")
                                .fill()
                                : textArea(cssEditorState)
                                .title("CSS Editor")
                                .showLineNumbers()
                                .lineNumberStyle(Style.EMPTY.fg(Color.GRAY))
                                .rounded()
                                .focusedBorderColor(Color.CYAN)
                                .id("css-editor")
                                .fill(),

                        // Right - Rendered preview
                        previewElement
                                .markup(markupEditorState.text())
                                .title("Rendered Preview")
                ).fill(),

                // Help bar at bottom
                panel(() -> row(
                        cssError != null
                                ? text(" CSS Error: " + cssError).red()
                                : row(
                                text(" Tags: ").dim(),
                                text("[bold] [italic] [red] [green] [blue] [cyan] [yellow] [magenta]").cyan()
                        ),
                        spacer(),
                        text(" Line: ").dim(),
                        text(String.valueOf(currentEditorState().cursorRow() + 1)).cyan(),
                        text(" Col: ").dim(),
                        text(String.valueOf(currentEditorState().cursorCol() + 1)).cyan()
                )).rounded().length(3)
        ).render(frame, area, context);
    }

    private TextAreaState currentEditorState() {
        return tabsState.selected() == 0 ? markupEditorState : cssEditorState;
    }

    private String getInitialMarkup() {
        return """
                [bold]Welcome to MarkupText![/bold]
                
                This is a [cyan]live editor[/cyan] for BBCode-style markup.
                Edit this text and watch the preview update!
                
                [bold]Try these styles:[/bold]
                - [red]Red text[/red]
                - [green][bold]Green and bold[/bold][/green]
                - [blue][italic]Blue and italic[/italic][/blue]
                - [underlined]Underlined[/underlined]
                - [dim]Dimmed text[/dim]
                - [magenta]Magenta[/magenta] and [yellow]Yellow[/yellow]
                
                [bold]Nesting works:[/bold]
                [red][bold][italic]Red, bold, and italic![/italic][/bold][/red]
                
                [bold]Links:[/bold]
                [link=https://example.com]Click here[/link]
                
                [bold]Escape brackets:[/bold]
                Use [[double brackets]] to show literal [brackets]
                
                [bold]Custom CSS tags:[/bold]
                Switch to CSS tab and define styles for custom tags!
                Try: [keyword]function[/keyword] [string]"hello"[/string]""";
    }

    @Override
    public Constraint constraint() {
        return Constraint.fill();
    }

    private void applyUserCss(String css) {
        try {
            styleEngine.addStylesheet("user", css);
            styleEngine.setActiveStylesheet("user");
            cssError = null;
        } catch (CssParseException e) {
            cssError = "Line " + e.getPosition().line() + ": " + e.getMessage();
        } catch (Exception e) {
            cssError = "Error: " + e.getMessage();
        }
    }
}
