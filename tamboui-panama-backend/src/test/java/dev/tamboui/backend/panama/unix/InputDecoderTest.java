/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.backend.panama.unix;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class InputDecoderTest {

    private static final int TIMEOUT = -2;
    private static final int EOF = -1;

    @Test
    @DisplayName("Latin-1: the key typed after an accented letter is not lost")
    void keyAfterAccentedLetterInLatin1() throws IOException {
        // é in Latin-1 is 0xE9, which starts a 3-byte sequence in UTF-8
        assertThat(decode(StandardCharsets.ISO_8859_1, 0xE9, 'a')).isEqualTo("éa");
    }

    @Test
    @DisplayName("Latin-1: an accented letter typed last comes through without waiting for another byte")
    void accentedLetterTypedLastInLatin1() throws IOException {
        int[] reads = {0};
        InputDecoder decoder = new InputDecoder(timeoutMs -> ++reads[0] == 1 ? 0xE9 : TIMEOUT,
                StandardCharsets.ISO_8859_1);

        assertThat(decoder.read(0)).isEqualTo('é');
        assertThat(reads[0]).isEqualTo(1);
    }

    @Test
    @DisplayName("Latin-1: text comes through one byte per character")
    void latin1Text() throws IOException {
        String text = "Größe »café«";
        byte[] bytes = text.getBytes(StandardCharsets.ISO_8859_1);

        assertThat(decode(StandardCharsets.ISO_8859_1, bytes)).isEqualTo(text);
    }

    @Test
    @DisplayName("A single-byte charset other than Latin-1 decodes each byte in that charset")
    void otherSingleByteCharset() throws IOException {
        // 0xA4 is the euro sign in ISO-8859-15, and the currency sign in Latin-1
        assertThat(decode(Charset.forName("ISO-8859-15"), 0xA4)).isEqualTo("€");
    }

    @Test
    @DisplayName("UTF-8: a character is completed however late its last byte arrives")
    void utf8SequenceIsNotEndedByTimeout() throws IOException {
        // é in UTF-8 is C3 A9, with two reads timing out in between
        InputDecoder decoder = new InputDecoder(reader(0xC3, TIMEOUT, TIMEOUT, 0xA9), StandardCharsets.UTF_8);

        assertThat(decoder.read(0)).isEqualTo(TIMEOUT);
        assertThat(decoder.read(0)).isEqualTo(TIMEOUT);
        assertThat(decoder.read(0)).isEqualTo('é');
    }

    @Test
    @DisplayName("UTF-8: a byte that does not continue a sequence is not lost")
    void keyAfterNonUtf8ByteIsKept() throws IOException {
        assertThat(decode(StandardCharsets.UTF_8, 0xE9, 'a')).isEqualTo("éa");
    }

    @Test
    @DisplayName("UTF-8: Latin-1 bytes are not lost")
    void latin1BytesInUtf8() throws IOException {
        String text = "Größe »café«";
        byte[] bytes = text.getBytes(StandardCharsets.ISO_8859_1);

        assertThat(decode(StandardCharsets.UTF_8, bytes)).isEqualTo(text);
    }

    @Test
    @DisplayName("UTF-8: text after a byte that is not UTF-8 is decoded, ZWJ sequences and a flag included")
    void utf8AfterNonUtf8Byte() throws IOException {
        String text = "👩\u200D💻 é 👨\u200D👩\u200D👧 🇫🇷 世界";
        byte[] utf8 = text.getBytes(StandardCharsets.UTF_8);
        byte[] input = new byte[utf8.length + 1];
        input[0] = (byte) 0xE9;
        System.arraycopy(utf8, 0, input, 1, utf8.length);

        assertThat(decode(StandardCharsets.UTF_8, input)).isEqualTo("é" + text);
    }

    // Decodes the bytes in the charset until the end of the input
    private static String decode(Charset charset, byte[] bytes) throws IOException {
        int[] script = new int[bytes.length];
        for (int i = 0; i < bytes.length; i++) {
            script[i] = bytes[i] & 0xFF;
        }
        return decode(charset, script);
    }

    private static String decode(Charset charset, int... script) throws IOException {
        InputDecoder decoder = new InputDecoder(reader(script), charset);
        StringBuilder decoded = new StringBuilder();
        for (int codePoint = decoder.read(0); codePoint >= 0; codePoint = decoder.read(0)) {
            decoded.appendCodePoint(codePoint);
        }
        return decoded.toString();
    }

    // Returns the bytes and timeouts of the script in turn, then EOF
    private static InputDecoder.ByteReader reader(int... script) {
        int[] next = {0};
        return timeoutMs -> next[0] < script.length ? script[next[0]++] : EOF;
    }
}
