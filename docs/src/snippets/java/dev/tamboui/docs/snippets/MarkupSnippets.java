package dev.tamboui.docs.snippets;

import dev.tamboui.style.Color;
import dev.tamboui.style.Style;
import dev.tamboui.text.MarkupParser;
import dev.tamboui.text.Text;
import dev.tamboui.toolkit.element.Element;
import dev.tamboui.widgets.paragraph.Paragraph;

import static dev.tamboui.toolkit.Toolkit.*;

/**
 * Code snippets for markup.adoc documentation.
 * Each method contains tagged regions that are included in the documentation.
 */
@SuppressWarnings({"unused", "UnnecessaryLocalVariable"})
public class MarkupSnippets {

    void basicUsage() {
        // tag::basic-usage[]
        // Parse markup into styled Text
        Text text = MarkupParser.parse("This is [bold]bold[/bold] and [red]red[/red].");

        // Use with widgets
        Paragraph paragraph = Paragraph.from(
            MarkupParser.parse("[cyan]Status:[/cyan] [bold green]Ready[/]")
        );
        // end::basic-usage[]
    }

    void customStyleResolver() {
        // tag::custom-style-resolver[]
        MarkupParser.StyleResolver resolver = tagName -> {
            switch (tagName) {
                case "keyword": return Style.EMPTY.fg(Color.CYAN).bold();
                case "string": return Style.EMPTY.fg(Color.GREEN);
                case "comment": return Style.EMPTY.fg(Color.GRAY).italic();
                case "error": return Style.EMPTY.fg(Color.WHITE).bg(Color.RED).bold();
                default: return null;
            }
        };

        Text code = MarkupParser.parse(
            "[keyword]public[/] [keyword]void[/] main([string]\"Hello\"[/]) [comment]// entry point[/]",
            resolver
        );
        // end::custom-style-resolver[]
    }

    void resolverPriority() {
        // tag::resolver-priority[]
        // Redefine "red" to mean something else in your theme
        MarkupParser.StyleResolver themeResolver = tagName -> {
            if ("red".equals(tagName)) {
                return Style.EMPTY.fg(Color.hex("#ff6b6b"));  // Custom red
            }
            return null;
        };

        // Now [red] uses your custom color instead of Color.RED
        Text text = MarkupParser.parse("[red]Custom red[/]", themeResolver);
        // end::resolver-priority[]
    }

    void mergingResolverAndCompoundStyles() {
        // tag::merging-resolver-compound[]
        MarkupParser.StyleResolver resolver = tagName -> {
            if ("error".equals(tagName)) {
                return Style.EMPTY.fg(Color.CYAN).bold();
            }
            return null;
        };

        // [error] alone: cyan foreground + bold (from resolver)
        Text t1 = MarkupParser.parse("[error]Message[/]", resolver);

        // [error on red]: cyan foreground + bold + red background
        // The resolver provides the base, compound adds the background
        Text t2 = MarkupParser.parse("[error on red]Message[/]", resolver);

        // [error yellow]: yellow foreground + bold
        // Compound's yellow overrides resolver's cyan, but bold is kept
        Text t3 = MarkupParser.parse("[error yellow]Message[/]", resolver);

        // [error italic]: cyan foreground + bold + italic
        // Compound adds italic to resolver's style
        Text t4 = MarkupParser.parse("[error italic]Message[/]", resolver);
        // end::merging-resolver-compound[]
    }

    void cssClassTargeting() {
        // tag::css-class-targeting[]
        Text text = MarkupParser.parse("[error]message[/]");
        // The span has Tags extension containing "error"

        Text compound = MarkupParser.parse("[bold underlined yellow]Warning![/]");
        // The span has Tags extension containing "bold", "underlined", "yellow"

        Text background = MarkupParser.parse("[white on blue]text[/]");
        // The span has Tags extension containing "white", "blue" (not "on")
        // end::css-class-targeting[]
    }

    void unknownTags() {
        // tag::unknown-tags[]
        // "custom" is unknown, but tracked as a CSS class
        Text text = MarkupParser.parse("[custom]styled by CSS[/custom]");

        // Can be styled via CSS:
        // .custom { color: magenta; }
        // end::unknown-tags[]
    }

    void multiLineText() {
        // tag::multi-line-text[]
        Text multiline = MarkupParser.parse("""
            [bold]Header[/]

            [dim]This is a paragraph with [cyan]highlighted[/] text.[/]

            [italic]Footer note[/]
            """);
        // end::multi-line-text[]
    }

    void statusMessages() {
        // tag::status-messages[]
        MarkupParser.parse("[green]SUCCESS[/] Operation completed");
        MarkupParser.parse("[yellow]WARNING[/] Disk space low");
        MarkupParser.parse("[bold red]ERROR[/] Connection failed");
        // end::status-messages[]
    }

    void syntaxHighlighting() {
        // tag::syntax-highlighting[]
        MarkupParser.StyleResolver syntax = tag -> switch (tag) {
            case "kw" -> Style.EMPTY.fg(Color.MAGENTA).bold();
            case "str" -> Style.EMPTY.fg(Color.GREEN);
            case "num" -> Style.EMPTY.fg(Color.CYAN);
            case "cmt" -> Style.EMPTY.fg(Color.GRAY).italic();
            default -> null;
        };

        Text code = MarkupParser.parse(
            "[kw]int[/] x = [num]42[/]; [cmt]// answer[/]",
            syntax
        );
        // end::syntax-highlighting[]
    }

    void richNotifications() {
        // tag::rich-notifications[]
        MarkupParser.parse("""
            [bold white on blue] NOTICE [/]

            Your session will expire in [bold yellow]5 minutes[/].
            Please [link=https://example.com/save]save your work[/link].
            """);
        // end::rich-notifications[]
    }
}
