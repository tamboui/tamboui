/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.tui.bindings;

import dev.tamboui.tui.event.KeyCode;
import dev.tamboui.tui.event.KeyEvent;
import dev.tamboui.tui.event.KeyModifiers;
import dev.tamboui.tui.event.MouseButton;
import dev.tamboui.tui.event.MouseEvent;
import dev.tamboui.tui.event.MouseEventKind;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.*;

class BindingsLoadTest {

    @Test
    @DisplayName("Load bindings from classpath resource")
    void loadFromClasspathResource() throws IOException {
        Bindings bindings = BindingSets.loadResource("/test-bindings.properties");

        // Verify navigation bindings
        KeyEvent up = KeyEvent.ofKey(KeyCode.UP, bindings);
        KeyEvent k = KeyEvent.ofChar('k', bindings);
        assertThat(up.isUp()).isTrue();
        assertThat(k.isUp()).isTrue();

        KeyEvent down = KeyEvent.ofKey(KeyCode.DOWN, bindings);
        KeyEvent j = KeyEvent.ofChar('j', bindings);
        assertThat(down.isDown()).isTrue();
        assertThat(j.isDown()).isTrue();
    }

    @Test
    @DisplayName("Load bindings from file path")
    void loadFromFilePath(@TempDir Path tempDir) throws IOException {
        Path bindingsFile = tempDir.resolve("custom.properties");
        Files.write(bindingsFile, Arrays.asList(
            "moveUp = Up, w",
            "moveDown = Down, s",
            "quit = Escape"
        ));

        Bindings bindings = BindingSets.load(bindingsFile);

        KeyEvent w = KeyEvent.ofChar('w', bindings);
        KeyEvent s = KeyEvent.ofChar('s', bindings);
        KeyEvent esc = KeyEvent.ofKey(KeyCode.ESCAPE, bindings);

        assertThat(w.isUp()).isTrue();
        assertThat(s.isDown()).isTrue();
        assertThat(esc.isQuit()).isTrue();
    }

    @Test
    @DisplayName("Load bindings from input stream")
    void loadFromInputStream() throws IOException {
        String props = "confirm = Enter\ncancel = Escape\n";
        ByteArrayInputStream in = new ByteArrayInputStream(props.getBytes(StandardCharsets.UTF_8));

        Bindings bindings = BindingSets.load(in);

        KeyEvent enter = KeyEvent.ofKey(KeyCode.ENTER, bindings);
        KeyEvent escape = KeyEvent.ofKey(KeyCode.ESCAPE, bindings);

        assertThat(enter.isConfirm()).isTrue();
        assertThat(escape.isCancel()).isTrue();
    }

    @Test
    @DisplayName("Parse Ctrl modifier")
    void parseCtrlModifier() throws IOException {
        String props = "pageUp = Ctrl+u\nquit = Ctrl+c\n";
        Bindings bindings = BindingSets.load(new ByteArrayInputStream(props.getBytes(StandardCharsets.UTF_8)));

        KeyEvent ctrlU = KeyEvent.ofChar('u', KeyModifiers.CTRL, bindings);
        KeyEvent ctrlC = KeyEvent.ofChar('c', KeyModifiers.CTRL, bindings);

        assertThat(ctrlU.isPageUp()).isTrue();
        assertThat(ctrlC.isQuit()).isTrue();
    }

    @Test
    @DisplayName("Parse Alt modifier")
    void parseAltModifier() throws IOException {
        String props = "pageUp = Alt+v\n";
        Bindings bindings = BindingSets.load(new ByteArrayInputStream(props.getBytes(StandardCharsets.UTF_8)));

        KeyEvent altV = KeyEvent.ofChar('v', KeyModifiers.ALT, bindings);
        assertThat(altV.isPageUp()).isTrue();
    }

    @Test
    @DisplayName("Parse Shift modifier")
    void parseShiftModifier() throws IOException {
        String props = "focusPrevious = Shift+Tab\n";
        Bindings bindings = BindingSets.load(new ByteArrayInputStream(props.getBytes(StandardCharsets.UTF_8)));

        KeyEvent shiftTab = KeyEvent.ofKey(KeyCode.TAB, KeyModifiers.SHIFT, bindings);
        assertThat(shiftTab.isFocusPrevious()).isTrue();
    }

    @Test
    @DisplayName("Parse Space key")
    void parseSpaceKey() throws IOException {
        String props = "select = Space\n";
        Bindings bindings = BindingSets.load(new ByteArrayInputStream(props.getBytes(StandardCharsets.UTF_8)));

        KeyEvent space = KeyEvent.ofChar(' ', bindings);
        assertThat(space.isSelect()).isTrue();
    }

    @Test
    @DisplayName("Parse function keys")
    void parseFunctionKeys() throws IOException {
        String props = "confirm = F1\ncancel = F12\n";
        Bindings bindings = BindingSets.load(new ByteArrayInputStream(props.getBytes(StandardCharsets.UTF_8)));

        KeyEvent f1 = KeyEvent.ofKey(KeyCode.F1, bindings);
        KeyEvent f12 = KeyEvent.ofKey(KeyCode.F12, bindings);

        assertThat(f1.isConfirm()).isTrue();
        assertThat(f12.isCancel()).isTrue();
    }

    @Test
    @DisplayName("Parse multiple bindings per action")
    void parseMultipleBindings() throws IOException {
        String props = "moveUp = Up, k, K, w\n";
        Bindings bindings = BindingSets.load(new ByteArrayInputStream(props.getBytes(StandardCharsets.UTF_8)));

        KeyEvent up = KeyEvent.ofKey(KeyCode.UP, bindings);
        KeyEvent k = KeyEvent.ofChar('k', bindings);
        KeyEvent K = KeyEvent.ofChar('K', bindings);
        KeyEvent w = KeyEvent.ofChar('w', bindings);

        assertThat(up.isUp()).isTrue();
        assertThat(k.isUp()).isTrue();
        assertThat(K.isUp()).isTrue();
        assertThat(w.isUp()).isTrue();
    }

    @Test
    @DisplayName("Key names are case-insensitive")
    void keyNamesAreCaseInsensitive() throws IOException {
        String props = "moveUp = up\nmoveDown = DOWN\nmoveLeft = Left\n";
        Bindings bindings = BindingSets.load(new ByteArrayInputStream(props.getBytes(StandardCharsets.UTF_8)));

        KeyEvent up = KeyEvent.ofKey(KeyCode.UP, bindings);
        KeyEvent down = KeyEvent.ofKey(KeyCode.DOWN, bindings);
        KeyEvent left = KeyEvent.ofKey(KeyCode.LEFT, bindings);

        assertThat(up.isUp()).isTrue();
        assertThat(down.isDown()).isTrue();
        assertThat(left.isLeft()).isTrue();
    }

    @Test
    @DisplayName("Throws IOException for unknown key name")
    void throwsForUnknownKeyName() {
        String props = "moveUp = InvalidKeyName\n";
        ByteArrayInputStream in = new ByteArrayInputStream(props.getBytes(StandardCharsets.UTF_8));

        assertThatThrownBy(() -> BindingSets.load(in))
            .isInstanceOf(IOException.class)
            .hasMessageContaining("Unknown key");
    }

    @Test
    @DisplayName("Throws IOException for non-existent resource")
    void throwsForNonExistentResource() {
        assertThatThrownBy(() -> BindingSets.loadResource("/non-existent.properties"))
            .isInstanceOf(IOException.class)
            .hasMessageContaining("Resource not found");
    }

    @Test
    @DisplayName("Comments and empty lines are ignored")
    void commentsAndEmptyLinesIgnored() throws IOException {
        String props =
            "# This is a comment\n" +
            "\n" +
            "moveUp = Up\n" +
            "# Another comment\n" +
            "moveDown = Down\n" +
            "\n";
        Bindings bindings = BindingSets.load(new ByteArrayInputStream(props.getBytes(StandardCharsets.UTF_8)));

        KeyEvent up = KeyEvent.ofKey(KeyCode.UP, bindings);
        KeyEvent down = KeyEvent.ofKey(KeyCode.DOWN, bindings);

        assertThat(up.isUp()).isTrue();
        assertThat(down.isDown()).isTrue();
    }

    @Test
    @DisplayName("Alternate key name aliases work")
    void alternateKeyNameAliasesWork() throws IOException {
        String props =
            "cancel = Esc\n" +
            "deleteForward = Del\n" +
            "pageUp = PgUp\n" +
            "pageDown = PgDn\n";
        Bindings bindings = BindingSets.load(new ByteArrayInputStream(props.getBytes(StandardCharsets.UTF_8)));

        KeyEvent esc = KeyEvent.ofKey(KeyCode.ESCAPE, bindings);
        KeyEvent del = KeyEvent.ofKey(KeyCode.DELETE, bindings);
        KeyEvent pgUp = KeyEvent.ofKey(KeyCode.PAGE_UP, bindings);
        KeyEvent pgDn = KeyEvent.ofKey(KeyCode.PAGE_DOWN, bindings);

        assertThat(esc.isCancel()).isTrue();
        assertThat(del.isDeleteForward()).isTrue();
        assertThat(pgUp.isPageUp()).isTrue();
        assertThat(pgDn.isPageDown()).isTrue();
    }

    // ========== Mouse Trigger Loading Tests ==========

    @Test
    @DisplayName("Parse mouse click binding")
    void parseMouseClickBinding() throws IOException {
        String props = "click = Mouse.Left.Press\n";
        Bindings bindings = BindingSets.load(new ByteArrayInputStream(props.getBytes(StandardCharsets.UTF_8)));

        MouseEvent click = MouseEvent.press(MouseButton.LEFT, 10, 20, bindings);
        assertThat(click.isClick()).isTrue();
    }

    @Test
    @DisplayName("Parse mouse right click binding")
    void parseMouseRightClickBinding() throws IOException {
        String props = "rightClick = Mouse.Right.Press\n";
        Bindings bindings = BindingSets.load(new ByteArrayInputStream(props.getBytes(StandardCharsets.UTF_8)));

        MouseEvent rightClick = MouseEvent.press(MouseButton.RIGHT, 10, 20, bindings);
        assertThat(rightClick.isRightClick()).isTrue();
    }

    @Test
    @DisplayName("Parse mouse scroll bindings")
    void parseMouseScrollBindings() throws IOException {
        String props = "scrollUp = Mouse.ScrollUp\nscrollDown = Mouse.ScrollDown\n";
        Bindings bindings = BindingSets.load(new ByteArrayInputStream(props.getBytes(StandardCharsets.UTF_8)));

        MouseEvent scrollUp = new MouseEvent(MouseEventKind.SCROLL_UP, MouseButton.NONE, 0, 0, KeyModifiers.NONE, bindings);
        MouseEvent scrollDown = new MouseEvent(MouseEventKind.SCROLL_DOWN, MouseButton.NONE, 0, 0, KeyModifiers.NONE, bindings);

        assertThat(scrollUp.matches(Actions.SCROLL_UP)).isTrue();
        assertThat(scrollDown.matches(Actions.SCROLL_DOWN)).isTrue();
    }

    @Test
    @DisplayName("Parse Ctrl+mouse click binding")
    void parseCtrlMouseClickBinding() throws IOException {
        String props = "customClick = Ctrl+Mouse.Left.Press\n";
        Bindings bindings = BindingSets.load(new ByteArrayInputStream(props.getBytes(StandardCharsets.UTF_8)));

        MouseEvent ctrlClick = new MouseEvent(MouseEventKind.PRESS, MouseButton.LEFT, 10, 20, KeyModifiers.CTRL, bindings);
        assertThat(ctrlClick.matches("customClick")).isTrue();
    }

    @Test
    @DisplayName("Custom string actions work with bindings")
    void customStringActionsWork() throws IOException {
        String props = "myCustomAction = Up\nanotherAction = Mouse.Left.Press\n";
        Bindings bindings = BindingSets.load(new ByteArrayInputStream(props.getBytes(StandardCharsets.UTF_8)));

        KeyEvent up = KeyEvent.ofKey(KeyCode.UP, bindings);
        MouseEvent click = MouseEvent.press(MouseButton.LEFT, 0, 0, bindings);

        assertThat(up.matches("myCustomAction")).isTrue();
        assertThat(click.matches("anotherAction")).isTrue();
    }
}
