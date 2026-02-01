/// usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS dev.tamboui:tamboui-toolkit:LATEST
//DEPS dev.tamboui:tamboui-panama-backend:LATEST

import dev.tamboui.toolkit.app.ToolkitRunner;

import static dev.tamboui.layout.Flex.END;
import static dev.tamboui.layout.Flex.SPACE_BETWEEN;
import static dev.tamboui.toolkit.Toolkit.*;

/**
 * Demo entry point
 * @throws Exception in case something goes wrong
 */
void main() throws Exception {
    try (var runner = ToolkitRunner.create()) {
        runner.run(() ->
                panel("Toolkit Demo",
                        markupText("Hello, [red italic]TamboUI![/]").length(1),
                        row(markupText("Press [green]q[/] to exit!").fit()).length(1).flex(END)
                ).flex(SPACE_BETWEEN)
        );
    }
}