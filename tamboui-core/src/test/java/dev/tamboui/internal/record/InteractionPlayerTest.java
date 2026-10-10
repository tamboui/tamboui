/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.internal.record;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import dev.tamboui.buffer.Buffer;

import static org.assertj.core.api.Assertions.*;

class InteractionPlayerTest {

    private static final int ESC = 27;

    @TempDir
    Path tempDir;

    @Test
    void ctrlLetterKeysShouldProduceCorrectBytes() throws IOException {
        Path tape = tempDir.resolve("test.tape");
        Files.write(tape, "Ctrl+t\nCtrl+d\nCtrl+a\nCtrl+c\n".getBytes(StandardCharsets.UTF_8));

        List<Interaction> interactions = InteractionPlayer.loadFromFile(tape, tempDir.resolve("out.cast"));
        InteractionPlayer player = new InteractionPlayer(interactions, null);

        List<Integer> bytes = collectKeyBytes(player);

        assertThat(bytes).containsExactly(
                0x14, // Ctrl+T = 't' - 'a' + 1 = 20
                0x04, // Ctrl+D = 'd' - 'a' + 1 = 4
                0x01, // Ctrl+A = 'a' - 'a' + 1 = 1
                0x03  // Ctrl+C = 'c' - 'a' + 1 = 3
        );
    }

    @Test
    void shiftLetterShouldProduceUppercase() throws IOException {
        Path tape = tempDir.resolve("test.tape");
        Files.write(tape, "Shift+d\n".getBytes(StandardCharsets.UTF_8));

        List<Interaction> interactions = InteractionPlayer.loadFromFile(tape, tempDir.resolve("out.cast"));
        InteractionPlayer player = new InteractionPlayer(interactions, null);

        List<Integer> bytes = collectKeyBytes(player);

        assertThat(bytes).containsExactly((int) 'D');
    }

    @Test
    void typedTextShouldProduceCharacterBytes() throws IOException {
        Path tape = tempDir.resolve("test.tape");
        Files.write(tape, "Type \"abc\"\n".getBytes(StandardCharsets.UTF_8));

        List<Interaction> interactions = InteractionPlayer.loadFromFile(tape, tempDir.resolve("out.cast"));
        InteractionPlayer player = new InteractionPlayer(interactions, null);

        List<Integer> bytes = collectKeyBytes(player);

        assertThat(bytes).containsExactly((int) 'a', (int) 'b', (int) 'c');
    }

    @Test
    void ctrlKeysInMixedTapeShouldWork() throws IOException {
        Path tape = tempDir.resolve("test.tape");
        Files.write(tape, "Ctrl+t\nType \"hi\"\nEnter\n".getBytes(StandardCharsets.UTF_8));

        List<Interaction> interactions = InteractionPlayer.loadFromFile(tape, tempDir.resolve("out.cast"));
        InteractionPlayer player = new InteractionPlayer(interactions, null);

        List<Integer> bytes = collectKeyBytes(player);

        assertThat(bytes).containsExactly(
                0x14,        // Ctrl+T
                (int) 'h',   // Type "hi"
                (int) 'i',
                (int) '\r'   // Enter
        );
    }

    @Test
    void functionKeysShouldProduceEscapeSequences() throws IOException {
        List<Integer> bytes = keyBytes("F1\nF4\nF5\nF8\nF12\n", null);

        assertThat(bytes).containsExactlyElementsOf(concat(
                seq(ESC, 'O', 'P'),           // F1
                seq(ESC, 'O', 'S'),           // F4
                seq(ESC, '[', '1', '5', '~'), // F5
                seq(ESC, '[', '1', '9', '~'), // F8
                seq(ESC, '[', '2', '4', '~')  // F12
        ));
    }

    @Test
    void modifiersOnFunctionAndCursorKeysShouldUseTheXtermParameter() throws IOException {
        List<Integer> bytes = keyBytes("Shift+F8\nCtrl+F1\nAlt+F3\nCtrl+Up\nShift+End\nCtrl+Delete\n", null);

        assertThat(bytes).containsExactlyElementsOf(concat(
                seq(ESC, '[', '1', '9', ';', '2', '~'), // Shift+F8: 1 + shift
                seq(ESC, '[', '1', ';', '5', 'P'),      // Ctrl+F1: 1 + 4 * ctrl
                seq(ESC, '[', '1', ';', '3', 'R'),      // Alt+F3: 1 + 2 * alt
                seq(ESC, '[', '1', ';', '5', 'A'),      // Ctrl+Up
                seq(ESC, '[', '1', ';', '2', 'F'),      // Shift+End
                seq(ESC, '[', '3', ';', '5', '~')       // Ctrl+Delete
        ));
    }

    @Test
    void shiftTabAndAltKeysShouldProduceTheirSequences() throws IOException {
        List<Integer> bytes = keyBytes("Shift+Tab\nAlt+x\nAlt+Enter\nCtrl+Alt+a\n", null);

        assertThat(bytes).containsExactlyElementsOf(concat(
                seq(ESC, '[', 'Z'), // back tab
                seq(ESC, 'x'),      // Alt is an ESC prefix
                seq(ESC, '\r'),
                seq(ESC, 0x01)      // Ctrl+a with the Alt prefix
        ));
    }

    @Test
    void aKeyWithACountShouldBePressedThatManyTimes() throws IOException {
        List<Integer> bytes = keyBytes("Down 3\n", null);

        assertThat(bytes).containsExactlyElementsOf(concat(
                seq(ESC, '[', 'B'), seq(ESC, '[', 'B'), seq(ESC, '[', 'B')));
    }

    @Test
    void typeShouldKeepAtSignsAndPlusSignsInTheText() throws IOException {
        List<Integer> bytes = keyBytes("Type \"a@b+1\"\n", null);

        assertThat(bytes).containsExactly((int) 'a', (int) '@', (int) 'b', (int) '+', (int) '1');
    }

    @Test
    void typeWithASpeedShouldWaitBetweenTheCharacters() throws IOException {
        List<Interaction> interactions = load("Type@20ms \"abc\"\n");

        assertThat(interactions).extracting(i -> i.getClass().getSimpleName())
                .containsExactly("KeyPress", "Wait", "KeyPress", "Wait", "KeyPress");
        assertThat(((Interaction.Wait) interactions.get(1)).millis()).isEqualTo(20);
        assertThat(keyBytes("Type@20ms \"abc\"\n", null)).containsExactly((int) 'a', (int) 'b', (int) 'c');
    }

    @Test
    void unknownCommandsShouldBeReportedAndSkipped() throws IOException {
        PrintStream originalErr = System.err;
        ByteArrayOutputStream err = new ByteArrayOutputStream();
        System.setErr(new PrintStream(err, true, "UTF-8"));
        List<Integer> bytes;
        try {
            bytes = keyBytes("Frobnicate 3\nF13\nEnter\n", null);
        } finally {
            System.setErr(originalErr);
        }

        assertThat(bytes).containsExactly((int) '\r');
        assertThat(err.toString("UTF-8"))
                .contains("Ignoring unknown tape command: Frobnicate 3")
                .contains("Ignoring unknown tape command: F13");
    }

    @Test
    void waitShouldContinueOnceTheScreenShowsTheText() throws IOException {
        Buffer screen = Buffer.withLines("Loading", "Ready > ");

        long start = System.nanoTime();
        List<Integer> bytes = keyBytes("Wait /Ready/\nEnter\n", screen);

        assertThat(bytes).containsExactly((int) '\r');
        assertThat(TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start)).isLessThan(5000);
    }

    @Test
    void waitShouldGiveUpAfterItsTimeout() throws IOException {
        Buffer screen = Buffer.withLines("Loading");

        long start = System.nanoTime();
        List<Integer> bytes = keyBytes("Wait@200ms /Ready/\nEnter\n", screen);

        assertThat(bytes).containsExactly((int) '\r');
        assertThat(TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start)).isGreaterThanOrEqualTo(200);
    }

    @Test
    void waitPlusScreenShouldMatchAcrossLines() throws IOException {
        Buffer screen = Buffer.withLines("Loading", "Ready");

        long start = System.nanoTime();
        List<Integer> bytes = keyBytes("Wait+Screen@3s /Loading\\s*\\nReady/\nEnter\n", screen);

        assertThat(bytes).containsExactly((int) '\r');
        assertThat(TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start)).isLessThan(3000);
    }

    @Test
    void hideShouldPlayTheKeysWhileMarkingThemHidden() throws IOException {
        InteractionPlayer player = new InteractionPlayer(load("Hide\nType \"a\"\nShow\nType \"b\"\n"), null);

        List<String> played = new ArrayList<>();
        while (!player.isFinished()) {
            int b = player.nextCodePoint(0);
            if (b >= 0) {
                played.add((char) b + (player.isHidden() ? " hidden" : " shown"));
            }
        }

        assertThat(played).containsExactly("a hidden", "b shown");
    }

    private List<Interaction> load(String tape) throws IOException {
        Path file = tempDir.resolve("test.tape");
        Files.write(file, tape.getBytes(StandardCharsets.UTF_8));
        return InteractionPlayer.loadFromFile(file, tempDir.resolve("out.cast"));
    }

    private List<Integer> keyBytes(String tape, Buffer screen) throws IOException {
        return collectKeyBytes(new InteractionPlayer(load(tape), screen));
    }

    private static List<Integer> seq(int... bytes) {
        List<Integer> list = new ArrayList<>();
        for (int b : bytes) {
            list.add(b);
        }
        return list;
    }

    @SafeVarargs
    private static List<Integer> concat(List<Integer>... sequences) {
        List<Integer> all = new ArrayList<>();
        for (List<Integer> sequence : sequences) {
            all.addAll(sequence);
        }
        return all;
    }

    private static List<Integer> collectKeyBytes(InteractionPlayer player) {
        List<Integer> bytes = new ArrayList<>();
        while (!player.isFinished()) {
            int b = player.nextCodePoint(0);
            if (b >= 0) {
                bytes.add(b);
            } else if (b == -2) {
                // timeout/wait — skip
            } else {
                break;
            }
        }
        return bytes;
    }
}
