package dev.tamboui.docs.snippets;

import dev.tamboui.inline.InlineDisplay;
import dev.tamboui.terminal.BackendFactory;
import dev.tamboui.terminal.Terminal;
import dev.tamboui.text.Text;
import dev.tamboui.toolkit.app.ToolkitApp;
import dev.tamboui.toolkit.element.Element;
import dev.tamboui.tui.TuiRunner;
import dev.tamboui.tui.event.KeyEvent;
import dev.tamboui.widgets.gauge.Gauge;
import dev.tamboui.widgets.paragraph.Paragraph;

import static dev.tamboui.toolkit.Toolkit.*;

/**
 * Code snippets for getting-started.adoc documentation.
 * Each class/method contains tagged regions that are included in the documentation.
 */
@SuppressWarnings({"unused", "UnnecessaryLocalVariable"})
public class GettingStartedSnippets {

    // tag::hello-dsl[]
    public class HelloDsl extends ToolkitApp {

        @Override
        protected Element render() {
            return panel("Hello",
                text("Welcome to TamboUI DSL!").bold().cyan(),
                spacer(),
                text("Press 'q' to quit").dim()
            ).rounded();
        }

        public void runApp() throws Exception {
            new HelloDsl().run();
        }
    }
    // end::hello-dsl[]

    void helloTuiRunner() throws Exception {
        // tag::hello-tui-runner[]
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
        // end::hello-tui-runner[]
    }

    void helloImmediate() throws Exception {
        // tag::hello-immediate[]
        try (var backend = BackendFactory.create()) {
            backend.enableRawMode();
            backend.enterAlternateScreen();

            try (var terminal = new Terminal<>(backend)) {
                terminal.draw(frame -> {
                    var paragraph = Paragraph.builder()
                        .text(Text.from("Hello, Immediate Mode!"))
                        .build();
                    frame.renderWidget(paragraph, frame.area());
                });
            }
            // Wait for user input
            Thread.sleep(2000);
        }
        // end::hello-immediate[]
    }

    void helloInline() throws Exception {
        // tag::hello-inline[]
        try (var display = InlineDisplay.create(2)) {
            for (int i = 0; i <= 100; i += 5) {
                int progress = i;
                display.render((area, buffer) -> {
                    var gauge = Gauge.builder()
                        .ratio(progress / 100.0)
                        .label("Progress: " + progress + "%")
                        .build();
                    gauge.render(area, buffer);
                });
                Thread.sleep(50);
            }
            display.println("Done!");
        }
        // end::hello-inline[]
    }
}
