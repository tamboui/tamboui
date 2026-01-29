//DEPS dev.tamboui:tamboui-toolkit:LATEST
//DEPS dev.tamboui:tamboui-pygments:LATEST
//DEPS dev.tamboui:tamboui-jline3-backend:LATEST
/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.demo;

import dev.tamboui.layout.Rect;
import static dev.tamboui.pygments.Pygments.pygments;
import dev.tamboui.pygments.Pygments;
import dev.tamboui.style.Color;
import dev.tamboui.style.Style;
import dev.tamboui.style.Tags;
import dev.tamboui.terminal.Frame;
import dev.tamboui.text.Line;
import dev.tamboui.text.Span;
import dev.tamboui.text.Text;
import dev.tamboui.toolkit.app.ToolkitRunner;
import dev.tamboui.toolkit.element.Element;
import dev.tamboui.toolkit.element.RenderContext;
import dev.tamboui.toolkit.event.EventResult;
import dev.tamboui.toolkit.elements.ListElement;
import dev.tamboui.toolkit.elements.RichTextAreaElement;
import dev.tamboui.tui.TuiConfig;
import dev.tamboui.tui.bindings.BindingSets;
import dev.tamboui.tui.event.KeyEvent;
import java.time.Duration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static dev.tamboui.toolkit.Toolkit.*;

/**
 * Demo showcasing {@link Pygments}.
 */
public final class PygmentsDemo implements Element {

    private static final List<Sample> SAMPLES = Arrays.asList(
        new Sample(
            "Java",
            "Hello.java",
            ""
                + "package dev.tamboui.demo;\n"
                + "\n"
                + "class Hello {\n"
                + "  // Highlighted via the pygmentize CLI\n"
                + "  static String greet(String name) {\n"
                + "    return name == null ? \"Hello, world\" : \"Hello, \" + name;\n"
                + "  }\n"
                + "\n"
                + "  public static void main(String[] args) {\n"
                + "    System.out.println(greet(args.length > 0 ? args[0] : null));\n"
                + "    int n = 42;\n"
                + "  }\n"
                + "}\n"
        ),
        new Sample(
            "Python",
            "hello.py",
            ""
                + "from dataclasses import dataclass\n"
                + "\n"
                + "@dataclass\n"
                + "class User:\n"
                + "    name: str\n"
                + "\n"
                + "def greet(user: User | None) -> str:\n"
                + "    # This is a comment\n"
                + "    return f\"Hello, {user.name if user else 'world'}\"\\\n"
                + "        .strip()\n"
                + "\n"
                + "print(greet(User('Ada')))\n"
        ),
        new Sample(
            "JavaScript",
            "app.js",
            ""
                + "export function greet(name) {\n"
                + "  // nullish coalescing\n"
                + "  return `Hello, ${name ?? 'world'}`;\n"
                + "}\n"
                + "\n"
                + "console.log(greet(null));\n"
        ),
        new Sample(
            "Rust",
            "main.rs",
            ""
                + "fn greet(name: Option<&str>) -> String {\n"
                + "    // This is a comment\n"
                + "    format!(\"Hello, {}\", name.unwrap_or(\"world\"))\n"
                + "}\n"
                + "\n"
                + "fn main() {\n"
                + "    println!(\"{}\", greet(None));\n"
                + "    let n: i32 = 42;\n"
                + "    println!(\"n={}\", n);\n"
                + "}\n"
        ),
        new Sample(
            "YAML",
            "config.yaml",
            ""
                + "name: tamboui\n"
                + "features:\n"
                + "  - tui\n"
                + "  - css\n"
                + "  - syntax-highlighting\n"
                + "meta:\n"
                + "  version: 1\n"
                + "  enabled: true\n"
        ),
        new Sample(
            "Markdown",
            "README.md",
            ""
                + "# TamboUI Demo\n"
                + "\n"
                + "```java\n"
                + "// Syntax highlighting for Java:\n"
                + "System.out.println(\"Hello, world!\");\n"
                + "```\n"
                + "\n"
                + "1. Easy bullet points\n"
                + "2. *Italic* and **bold** styles\n"
                + "\n"
                + "> Blockquote support!\n"
        )
    );

    private final RichTextAreaElement codeArea;
    private final ListElement<Sample> sampleList;

    private final Text[] highlightedCache;
    private final String[] subtitleCache;
    private int lastSelected = -1;
    private Text cachedText = Text.empty();
    private String cachedSubtitle = "";
    private String cachedTitle = "";

    public PygmentsDemo() {
        codeArea = new RichTextAreaElement()
            .wrapCharacter()
            .scrollbar(RichTextAreaElement.ScrollBarPolicy.AS_NEEDED)
            .rounded()
            .focusable()
            .focusedBorderColor(Color.CYAN)
            .fill();

        sampleList = new ListElement<Sample>()
            .data(SAMPLES, s -> row(
                text(s.title).bold(),
                spacer(),
                text(s.filename).dim()
            ))
            .title("Samples")
            .rounded()
            .scrollbar(ListElement.ScrollBarPolicy.AS_NEEDED)
            .highlightSymbol("› ")
            .highlightColor(Color.CYAN)
            .autoScroll()
            .focusable()
            .id("samples");

        highlightedCache = new Text[SAMPLES.size()];
        subtitleCache = new String[SAMPLES.size()];
    }

    public static void main(String[] args) throws Exception {
        var config = TuiConfig.builder()
            .mouseCapture(true)
            // Enable vim-style navigation (j/k, Ctrl+u/d) in addition to arrow keys.
            .bindings(BindingSets.vim())
            .build();

        try (var runner = ToolkitRunner.builder()
            .config(config)
            .bindings(BindingSets.vim())
            .build()) {
            var demo = new PygmentsDemo();
            runner.run(() -> demo);
        }
    }


    @Override
    public void render(Frame frame, Rect area, RenderContext context) {
        int selected = Math.min(Math.max(0, sampleList.selected()), SAMPLES.size() - 1);
        if (selected != lastSelected) {
            lastSelected = selected;
        }

        Sample sample = SAMPLES.get(selected);
        cachedTitle = sample.title + " — " + sample.filename;

        // Highlight each sample at most once (lazy cache).
        if (highlightedCache[selected] == null) {
            Pygments.Result result = pygments().highlightWithInfo(
                sample.filename,
                sample.source,
                Duration.ofSeconds(3) // Use default style resolver
            );

            subtitleCache[selected] = result.highlighted()
                ? "lexer=" + result.lexer().orElse("?")
                : ("no highlighting (" + result.message().orElse("unknown") + ")");
            highlightedCache[selected] = addLineNumbers(result.text());
        }

        cachedSubtitle = subtitleCache[selected] != null ? subtitleCache[selected] : "";
        cachedText = highlightedCache[selected] != null ? highlightedCache[selected] : Text.raw(sample.source);

        column(
            panel(() -> row(
                text(" Syntax highlighting (Pygmentize) ").bold(),
                spacer(1),
                text(" [Tab] Focus ").dim(),
                text(" [Ctrl+C] Quit ").dim()
            )).rounded().length(3),
            text(cachedSubtitle).dim().length(1),
            row(
                sampleList.length(34),
                spacer(1),
                panel(() -> codeArea.text(cachedText))
                    .title(cachedTitle)
                    .rounded()
                    .fill()
            ).fill()
        ).render(frame, area, context);
    }

    private static Text addLineNumbers(Text text) {
        List<Line> in = text.lines();
        if (in.isEmpty()) {
            return text;
        }

        int digits = String.valueOf(in.size()).length();
        Style lnStyle = Style.EMPTY
            .fg(Color.GRAY)
            .dim()
            .withExtension(Tags.class, Tags.of("syntax-line-number"));

        List<Line> out = new ArrayList<>(in.size());
        for (int i = 0; i < in.size(); i++) {
            Line line = in.get(i);
            String ln = padLeft(String.valueOf(i + 1), digits);
            List<Span> spans = new ArrayList<>(2 + line.spans().size());
            spans.add(Span.styled(ln, lnStyle));
            spans.add(Span.styled(" │ ", lnStyle));
            spans.addAll(line.spans());
            out.add(Line.from(spans));
        }
        return Text.from(out);
    }

    private static String padLeft(String s, int width) {
        if (s.length() >= width) {
            return s;
        }
        StringBuilder sb = new StringBuilder(width);
        for (int i = s.length(); i < width; i++) {
            sb.append(' ');
        }
        sb.append(s);
        return sb.toString();
    }

    private static final class Sample {
        final String title;
        final String filename;
        final String source;

        Sample(String title, String filename, String source) {
            this.title = title;
            this.filename = filename;
            this.source = source;
        }
    }
}

