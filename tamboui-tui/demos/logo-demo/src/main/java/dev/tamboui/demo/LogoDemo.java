///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS dev.tamboui:tamboui-widgets:LATEST
//DEPS dev.tamboui:tamboui-jline3-backend:LATEST

package dev.tamboui.demo;

import dev.tamboui.layout.Constraint;
import dev.tamboui.layout.Layout;
import dev.tamboui.terminal.Backend;
import dev.tamboui.terminal.BackendFactory;
import dev.tamboui.terminal.Frame;
import dev.tamboui.terminal.Terminal;
import dev.tamboui.text.Text;
import dev.tamboui.widgets.logo.Logo;
import dev.tamboui.widgets.paragraph.Paragraph;

/**
 * Logo demo.
 */
public class LogoDemo {
    private LogoDemo() {

    }

    /**
     * Demo entry point.
     * @param args the CLI arguments
     * @throws Exception on unexpected error
     */
    public static void main(String[] args) throws Exception {
        Logo.Size size = args.length > 0 && "small".equals(args[0]) 
            ? Logo.Size.TINY  // Note: SMALL not yet implemented
            : Logo.Size.TINY;

        try (Backend backend = BackendFactory.create()) {
            backend.enableRawMode();
            backend.enterAlternateScreen();
            backend.hideCursor();

            Terminal<Backend> terminal = new Terminal<>(backend);

            while (true) {
                terminal.draw(frame -> render(frame, size));
                int c = backend.read(250);
                if (c != -1 && c != -2) {
                    break; // Quit on any key press
                }
            }
        }
    }

    private static void render(Frame frame, Logo.Size size) {
        var layout = Layout.vertical()
            .constraints(Constraint.length(1), Constraint.fill(1));
        var areas = layout.split(frame.area());

        frame.renderWidget(Paragraph.from(Text.from("Powered by")), areas.get(0));
        frame.renderWidget(Logo.of(size), areas.get(1));
    }
}

