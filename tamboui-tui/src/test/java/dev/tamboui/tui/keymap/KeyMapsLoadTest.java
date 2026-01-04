/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.tui.keymap;

import dev.tamboui.tui.event.KeyCode;
import dev.tamboui.tui.event.KeyEvent;
import dev.tamboui.tui.event.KeyModifiers;
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

class KeyMapsLoadTest {

    @Test
    @DisplayName("Load keymap from classpath resource")
    void loadFromClasspathResource() throws IOException {
        KeyMap keyMap = KeyMaps.loadResource("/test-keymap.properties");

        // Verify navigation bindings
        KeyEvent up = KeyEvent.ofKey(KeyCode.UP, keyMap);
        KeyEvent k = KeyEvent.ofChar('k', keyMap);
        assertThat(up.isUp()).isTrue();
        assertThat(k.isUp()).isTrue();

        KeyEvent down = KeyEvent.ofKey(KeyCode.DOWN, keyMap);
        KeyEvent j = KeyEvent.ofChar('j', keyMap);
        assertThat(down.isDown()).isTrue();
        assertThat(j.isDown()).isTrue();
    }

    @Test
    @DisplayName("Load keymap from file path")
    void loadFromFilePath(@TempDir Path tempDir) throws IOException {
        Path keyMapFile = tempDir.resolve("custom.properties");
        Files.write(keyMapFile, Arrays.asList(
            "MOVE_UP = Up, w\n",
            "MOVE_DOWN = Down, s\n",
            "QUIT = Escape\n"
        ));

        KeyMap keyMap = KeyMaps.load(keyMapFile);

        KeyEvent w = KeyEvent.ofChar('w', keyMap);
        KeyEvent s = KeyEvent.ofChar('s', keyMap);
        KeyEvent esc = KeyEvent.ofKey(KeyCode.ESCAPE, keyMap);

        assertThat(w.isUp()).isTrue();
        assertThat(s.isDown()).isTrue();
        assertThat(esc.isQuit()).isTrue();
    }

    @Test
    @DisplayName("Load keymap from input stream")
    void loadFromInputStream() throws IOException {
        String props = "CONFIRM = Enter\nCANCEL = Escape\n";
        ByteArrayInputStream in = new ByteArrayInputStream(props.getBytes(StandardCharsets.UTF_8));

        KeyMap keyMap = KeyMaps.load(in);

        KeyEvent enter = KeyEvent.ofKey(KeyCode.ENTER, keyMap);
        KeyEvent escape = KeyEvent.ofKey(KeyCode.ESCAPE, keyMap);

        assertThat(enter.isConfirm()).isTrue();
        assertThat(escape.isCancel()).isTrue();
    }

    @Test
    @DisplayName("Parse Ctrl modifier")
    void parseCtrlModifier() throws IOException {
        String props = "PAGE_UP = Ctrl+u\nQUIT = Ctrl+c\n";
        KeyMap keyMap = KeyMaps.load(new ByteArrayInputStream(props.getBytes(StandardCharsets.UTF_8)));

        KeyEvent ctrlU = KeyEvent.ofChar('u', KeyModifiers.CTRL, keyMap);
        KeyEvent ctrlC = KeyEvent.ofChar('c', KeyModifiers.CTRL, keyMap);

        assertThat(ctrlU.isPageUp()).isTrue();
        assertThat(ctrlC.isQuit()).isTrue();
    }

    @Test
    @DisplayName("Parse Alt modifier")
    void parseAltModifier() throws IOException {
        String props = "PAGE_UP = Alt+v\n";
        KeyMap keyMap = KeyMaps.load(new ByteArrayInputStream(props.getBytes(StandardCharsets.UTF_8)));

        KeyEvent altV = KeyEvent.ofChar('v', KeyModifiers.ALT, keyMap);
        assertThat(altV.isPageUp()).isTrue();
    }

    @Test
    @DisplayName("Parse Shift modifier")
    void parseShiftModifier() throws IOException {
        String props = "FOCUS_PREVIOUS = Shift+Tab\n";
        KeyMap keyMap = KeyMaps.load(new ByteArrayInputStream(props.getBytes(StandardCharsets.UTF_8)));

        KeyEvent shiftTab = KeyEvent.ofKey(KeyCode.TAB, KeyModifiers.SHIFT, keyMap);
        assertThat(shiftTab.isFocusPrevious()).isTrue();
    }

    @Test
    @DisplayName("Parse Space key")
    void parseSpaceKey() throws IOException {
        String props = "SELECT = Space\n";
        KeyMap keyMap = KeyMaps.load(new ByteArrayInputStream(props.getBytes(StandardCharsets.UTF_8)));

        KeyEvent space = KeyEvent.ofChar(' ', keyMap);
        assertThat(space.isSelect()).isTrue();
    }

    @Test
    @DisplayName("Parse function keys")
    void parseFunctionKeys() throws IOException {
        String props = "CONFIRM = F1\nCANCEL = F12\n";
        KeyMap keyMap = KeyMaps.load(new ByteArrayInputStream(props.getBytes(StandardCharsets.UTF_8)));

        KeyEvent f1 = KeyEvent.ofKey(KeyCode.F1, keyMap);
        KeyEvent f12 = KeyEvent.ofKey(KeyCode.F12, keyMap);

        assertThat(f1.isConfirm()).isTrue();
        assertThat(f12.isCancel()).isTrue();
    }

    @Test
    @DisplayName("Parse multiple bindings per action")
    void parseMultipleBindings() throws IOException {
        String props = "MOVE_UP = Up, k, K, w\n";
        KeyMap keyMap = KeyMaps.load(new ByteArrayInputStream(props.getBytes(StandardCharsets.UTF_8)));

        KeyEvent up = KeyEvent.ofKey(KeyCode.UP, keyMap);
        KeyEvent k = KeyEvent.ofChar('k', keyMap);
        KeyEvent K = KeyEvent.ofChar('K', keyMap);
        KeyEvent w = KeyEvent.ofChar('w', keyMap);

        assertThat(up.isUp()).isTrue();
        assertThat(k.isUp()).isTrue();
        assertThat(K.isUp()).isTrue();
        assertThat(w.isUp()).isTrue();
    }

    @Test
    @DisplayName("Key names are case-insensitive")
    void keyNamesAreCaseInsensitive() throws IOException {
        String props = "MOVE_UP = up\nMOVE_DOWN = DOWN\nMOVE_LEFT = Left\n";
        KeyMap keyMap = KeyMaps.load(new ByteArrayInputStream(props.getBytes(StandardCharsets.UTF_8)));

        KeyEvent up = KeyEvent.ofKey(KeyCode.UP, keyMap);
        KeyEvent down = KeyEvent.ofKey(KeyCode.DOWN, keyMap);
        KeyEvent left = KeyEvent.ofKey(KeyCode.LEFT, keyMap);

        assertThat(up.isUp()).isTrue();
        assertThat(down.isDown()).isTrue();
        assertThat(left.isLeft()).isTrue();
    }

    @Test
    @DisplayName("Action names are case-insensitive")
    void actionNamesAreCaseInsensitive() throws IOException {
        String props = "move_up = Up\nMOVE_DOWN = Down\nMove_Left = Left\n";
        KeyMap keyMap = KeyMaps.load(new ByteArrayInputStream(props.getBytes(StandardCharsets.UTF_8)));

        KeyEvent up = KeyEvent.ofKey(KeyCode.UP, keyMap);
        KeyEvent down = KeyEvent.ofKey(KeyCode.DOWN, keyMap);
        KeyEvent left = KeyEvent.ofKey(KeyCode.LEFT, keyMap);

        assertThat(up.isUp()).isTrue();
        assertThat(down.isDown()).isTrue();
        assertThat(left.isLeft()).isTrue();
    }

    @Test
    @DisplayName("Unknown actions are ignored")
    void unknownActionsAreIgnored() throws IOException {
        String props = "MOVE_UP = Up\nUNKNOWN_ACTION = x\nMOVE_DOWN = Down\n";
        KeyMap keyMap = KeyMaps.load(new ByteArrayInputStream(props.getBytes(StandardCharsets.UTF_8)));

        // Should load successfully, ignoring unknown action
        KeyEvent up = KeyEvent.ofKey(KeyCode.UP, keyMap);
        KeyEvent down = KeyEvent.ofKey(KeyCode.DOWN, keyMap);

        assertThat(up.isUp()).isTrue();
        assertThat(down.isDown()).isTrue();
    }

    @Test
    @DisplayName("Throws IOException for unknown key name")
    void throwsForUnknownKeyName() {
        String props = "MOVE_UP = InvalidKeyName\n";
        ByteArrayInputStream in = new ByteArrayInputStream(props.getBytes(StandardCharsets.UTF_8));

        assertThatThrownBy(() -> KeyMaps.load(in))
            .isInstanceOf(IOException.class)
            .hasMessageContaining("Unknown key");
    }

    @Test
    @DisplayName("Throws IOException for non-existent resource")
    void throwsForNonExistentResource() {
        assertThatThrownBy(() -> KeyMaps.loadResource("/non-existent.properties"))
            .isInstanceOf(IOException.class)
            .hasMessageContaining("Resource not found");
    }

    @Test
    @DisplayName("Comments and empty lines are ignored")
    void commentsAndEmptyLinesIgnored() throws IOException {
        String props =
            "# This is a comment\n" +
            "\n" +
            "MOVE_UP = Up\n" +
            "# Another comment\n" +
            "MOVE_DOWN = Down\n" +
            "\n";
        KeyMap keyMap = KeyMaps.load(new ByteArrayInputStream(props.getBytes(StandardCharsets.UTF_8)));

        KeyEvent up = KeyEvent.ofKey(KeyCode.UP, keyMap);
        KeyEvent down = KeyEvent.ofKey(KeyCode.DOWN, keyMap);

        assertThat(up.isUp()).isTrue();
        assertThat(down.isDown()).isTrue();
    }

    @Test
    @DisplayName("Alternate key name aliases work")
    void alternateKeyNameAliasesWork() throws IOException {
        String props =
            "CANCEL = Esc\n" +
            "DELETE_FORWARD = Del\n" +
            "PAGE_UP = PgUp\n" +
            "PAGE_DOWN = PgDn\n";
        KeyMap keyMap = KeyMaps.load(new ByteArrayInputStream(props.getBytes(StandardCharsets.UTF_8)));

        KeyEvent esc = KeyEvent.ofKey(KeyCode.ESCAPE, keyMap);
        KeyEvent del = KeyEvent.ofKey(KeyCode.DELETE, keyMap);
        KeyEvent pgUp = KeyEvent.ofKey(KeyCode.PAGE_UP, keyMap);
        KeyEvent pgDn = KeyEvent.ofKey(KeyCode.PAGE_DOWN, keyMap);

        assertThat(esc.isCancel()).isTrue();
        assertThat(del.isDeleteForward()).isTrue();
        assertThat(pgUp.isPageUp()).isTrue();
        assertThat(pgDn.isPageDown()).isTrue();
    }
}
