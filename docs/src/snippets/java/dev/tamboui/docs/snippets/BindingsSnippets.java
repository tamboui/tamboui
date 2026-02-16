package dev.tamboui.docs.snippets;

import dev.tamboui.annotations.bindings.OnAction;
import dev.tamboui.toolkit.component.Component;
import dev.tamboui.toolkit.element.Element;
import dev.tamboui.tui.TuiRunner;
import dev.tamboui.tui.bindings.ActionHandler;
import dev.tamboui.tui.bindings.Actions;
import dev.tamboui.tui.bindings.BindingSets;
import dev.tamboui.tui.bindings.Bindings;
import dev.tamboui.tui.bindings.KeyTrigger;
import dev.tamboui.tui.bindings.MouseTrigger;
import dev.tamboui.tui.event.Event;
import dev.tamboui.tui.event.KeyCode;
import dev.tamboui.tui.event.KeyEvent;
import dev.tamboui.tui.event.MouseButton;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static dev.tamboui.toolkit.Toolkit.text;

/**
 * Code snippets for bindings.adoc documentation.
 * Each method contains tagged regions that are included in the documentation.
 */
@SuppressWarnings({"unused", "UnnecessaryLocalVariable"})
public class BindingsSnippets {

    void predefinedBindingSets() {
        // tag::predefined-binding-sets[]
        Bindings standard = BindingSets.standard();  // Arrow keys, Enter, Escape
        Bindings vim = BindingSets.vim();            // hjkl navigation, Ctrl+u/d
        Bindings emacs = BindingSets.emacs();        // Ctrl+n/p/f/b navigation
        // end::predefined-binding-sets[]
    }

    void matchingActions(KeyEvent event) {
        // tag::matching-actions[]
        // Semantic - works with any binding set
        if (event.isUp()) { }
        if (event.isSelect()) { }
        if (event.matches("delete")) { }

        // Low-level - when you need specific keys
        if (event.isChar('x')) { }
        if (event.isKey(KeyCode.F1)) { }
        // end::matching-actions[]
    }

    void keyTriggers() {
        // tag::key-triggers[]
        KeyTrigger.key(KeyCode.UP);           // Arrow key
        KeyTrigger.ch('j');                   // Character
        KeyTrigger.chIgnoreCase('j');         // j or J
        KeyTrigger.ctrl('u');                 // Ctrl+U
        KeyTrigger.alt('x');                  // Alt+X
        // end::key-triggers[]
    }

    void mouseTriggers() {
        // tag::mouse-triggers[]
        MouseTrigger.click();                 // Left click
        MouseTrigger.rightClick();            // Right click
        MouseTrigger.ctrlClick();             // Ctrl+click
        MouseTrigger.scrollUp();              // Scroll wheel
        MouseTrigger.drag(MouseButton.LEFT);  // Dragging
        // end::mouse-triggers[]
    }

    void customBindings() {
        // tag::custom-bindings[]
        Bindings custom = BindingSets.standard()
            .toBuilder()
            .bind(KeyTrigger.ch('d'), "delete")
            .bind(KeyTrigger.key(KeyCode.DELETE), "delete")
            .bind(KeyTrigger.ctrl('s'), "save")
            .bind(MouseTrigger.rightClick(), "contextMenu")
            .build();
        // end::custom-bindings[]
    }

    // Stub methods for action handler examples
    TuiRunner runner;
    void save() {}
    void deleteSelected() {}

    void actionHandler() {
        // tag::action-handler[]
        ActionHandler actions = new ActionHandler(BindingSets.vim())
            .on(Actions.QUIT, e -> runner.quit())
            .on("save", e -> save())
            .on("delete", e -> deleteSelected());
        // end::action-handler[]
    }

    // tag::action-handler-event[]
    // In your event handler:
    boolean handleEvent(Event event, TuiRunner runner) {
        ActionHandler actions = new ActionHandler(BindingSets.vim());
        if (actions.dispatch(event)) {
            return true;  // Action was handled
        }
        // Handle other events...
        return false;
    }
    // end::action-handler-event[]

    void setColor(String color) {}

    void actionHandlerWithName() {
        Bindings bindings = BindingSets.standard();
        // tag::action-handler-with-name[]
        // Handler with action name (useful when same handler serves multiple actions)
        ActionHandler actions = new ActionHandler(bindings)
            .on("red", (event, action) -> setColor(action))
            .on("blue", (event, action) -> setColor(action))
            .on("green", (event, action) -> setColor(action));
        // end::action-handler-with-name[]
    }

    // tag::on-action-annotation[]
    public class EditorComponent extends Component<EditorComponent> {
        private List<String> lines = new ArrayList<>();
        private int cursor = 0;

        @OnAction(Actions.MOVE_UP)
        void moveCursorUp(Event event) {
            if (cursor > 0) cursor--;
        }

        @OnAction(Actions.MOVE_DOWN)
        void moveCursorDown(Event event) {
            if (cursor < lines.size() - 1) cursor++;
        }

        @OnAction("delete")
        void deleteLine(Event event) {
            if (!lines.isEmpty()) {
                lines.remove(cursor);
            }
        }

        @Override
        protected Element render() {
            return text("Editor");
        }
    }
    // end::on-action-annotation[]

    void loadingBindings() throws IOException {
        // tag::loading-bindings[]
        // From classpath
        Bindings bindings = BindingSets.loadResource("/my-bindings.properties");

        // From filesystem
        Bindings bindingsFromFile = BindingSets.load(Paths.get("~/.config/myapp/bindings.properties"));
        // end::loading-bindings[]
    }
}
