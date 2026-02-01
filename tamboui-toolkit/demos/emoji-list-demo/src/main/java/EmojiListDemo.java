/// usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS dev.tamboui:tamboui-toolkit:LATEST
//DEPS dev.tamboui:tamboui-panama-backend:LATEST
//DEPS dev.tamboui:tamboui-jline3-backend:LATEST

import dev.tamboui.text.Emoji;
import dev.tamboui.toolkit.app.InlineToolkitRunner;

import java.util.Map;

import static dev.tamboui.toolkit.Toolkit.*;

/**
 * Main entry point
 * @throws Exception in case something goes wrong
 */
void main() throws Exception {
    try (var runner = InlineToolkitRunner.create()) {
        runner.println(columns(
                        Emoji.emojis().entrySet().stream()
                                .sorted(Map.Entry.comparingByKey())
                                .map(e -> row(text(e.getValue()).fit(), text(" " + e.getKey())))
                                .toList()
                )
        );
    }
}
