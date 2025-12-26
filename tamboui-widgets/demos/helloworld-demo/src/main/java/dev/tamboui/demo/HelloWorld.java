//DEPS dev.tamboui:tamboui-widgets:LATEST
//DEPS dev.tamboui:tamboui-jline:LATEST

package dev.tamboui.demo;

import dev.tamboui.terminal.Backend;
import dev.tamboui.terminal.BackendFactory;
import dev.tamboui.terminal.Frame;
import dev.tamboui.terminal.Terminal;
import dev.tamboui.widgets.paragraph.Paragraph;
import dev.tamboui.text.Text;

/**
 * Minimal Hello World demo using immediate mode rendering.
 */
public class HelloWorld {
    public static void main(String[] args) throws Exception {
        try (Backend backend = BackendFactory.create()) {
            backend.enableRawMode();
            backend.enterAlternateScreen();
            backend.hideCursor();

            Terminal<Backend> terminal = new Terminal<>(backend);

            // Redraw on terminal resize
            backend.onResize(() -> {
                try {
                    terminal.draw(HelloWorld::render);
                } catch (Exception ignored) {
                }
            });

            // Initial draw
            terminal.draw(HelloWorld::render);

            boolean running = true;
            while (running) {
                int c = backend.read(250);
                if (c == -1 || c == -2) {
                    continue; // no input yet
                }

                // Quit on q/Q or Ctrl+C
                if (c == 'q' || c == 'Q' || c == 3) {
                    running = false;
                }
            }
        }
    }

    private static void render(Frame frame) {
        var paragraph = Paragraph.builder()
            .text(Text.from("Hello, TamboUI! Press 'q' to quit."))
            .build();
        frame.renderWidget(paragraph, frame.area());
    }
}

