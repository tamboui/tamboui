/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.internal.record;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.*;

class InteractionPlayerTest {

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
