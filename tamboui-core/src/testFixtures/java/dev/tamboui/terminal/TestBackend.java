/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.terminal;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import dev.tamboui.buffer.CellUpdate;
import dev.tamboui.layout.Position;
import dev.tamboui.layout.Size;

/**
 * A test backend that captures all terminal operations for assertion.
 * <p>
 * Provides a higher-level DSL for verifying terminal behavior in tests:
 * 
 * <pre>{@code
 * TestBackend backend = new TestBackend(80, 24);
 * // ... perform operations ...
 * backend.assertOps().hasInsertLines(1).hasCursorUp(3).hasDeleteLines(2);
 * }</pre>
 */
public class TestBackend implements Backend {

    private final int width;
    private final int height;
    private final List<Op> ops = new ArrayList<>();
    private final StringBuilder rawOutput = new StringBuilder();
    private final List<Object> transcript = new ArrayList<>();

    public TestBackend(int width, int height) {
        this.width = width;
        this.height = height;
    }

    /**
     * Resets all captured operations and raw output.
     */
    public void reset() {
        ops.clear();
        rawOutput.setLength(0);
        transcript.clear();
    }

    /**
     * Returns the list of captured operations.
     */
    public List<Op> ops() {
        return Collections.unmodifiableList(ops);
    }

    /**
     * Returns the captured raw output.
     */
    public String rawOutput() {
        return rawOutput.toString();
    }

    /**
     * Returns the ordered transcript of all interactions (ops and raw writes).
     * Elements are either {@link Op} instances or {@link String} instances (raw
     * output).
     */
    public List<Object> transcript() {
        return Collections.unmodifiableList(transcript);
    }

    /**
     * Returns an assertion DSL for verifying operations.
     */
    public OpAssert assertOps() {
        return new OpAssert(ops);
    }

    /**
     * Returns an assertion DSL for verifying the transcript ordering.
     */
    public TranscriptAssert assertTranscript() {
        return new TranscriptAssert(transcript);
    }

    // -- Backend implementation --

    @Override
    public void insertLines(int n) throws IOException {
        Op op = new Op(OpType.INSERT_LINES, n);
        ops.add(op);
        transcript.add(op);
    }

    @Override
    public void deleteLines(int n) throws IOException {
        Op op = new Op(OpType.DELETE_LINES, n);
        ops.add(op);
        transcript.add(op);
    }

    @Override
    public void moveCursorUp(int n) throws IOException {
        Op op = new Op(OpType.CURSOR_UP, n);
        ops.add(op);
        transcript.add(op);
    }

    @Override
    public void moveCursorDown(int n) throws IOException {
        Op op = new Op(OpType.CURSOR_DOWN, n);
        ops.add(op);
        transcript.add(op);
    }

    @Override
    public void moveCursorRight(int n) throws IOException {
        Op op = new Op(OpType.CURSOR_RIGHT, n);
        ops.add(op);
        transcript.add(op);
    }

    @Override
    public void moveCursorLeft(int n) throws IOException {
        Op op = new Op(OpType.CURSOR_LEFT, n);
        ops.add(op);
        transcript.add(op);
    }

    @Override
    public void eraseToEndOfLine() throws IOException {
        Op op = new Op(OpType.ERASE_EOL, 0);
        ops.add(op);
        transcript.add(op);
    }

    @Override
    public void carriageReturn() throws IOException {
        Op op = new Op(OpType.CARRIAGE_RETURN, 0);
        ops.add(op);
        transcript.add(op);
    }

    @Override
    public void writeRaw(byte[] data) throws IOException {
        String s = new String(data);
        rawOutput.append(s);
        transcript.add(s);
    }

    @Override
    public void draw(Iterable<CellUpdate> updates) throws IOException {
    }

    @Override
    public void flush() throws IOException {
    }

    @Override
    public void clear() throws IOException {
    }

    @Override
    public Size size() throws IOException {
        return new Size(width, height);
    }

    @Override
    public void showCursor() throws IOException {
        ops.add(new Op(OpType.SHOW_CURSOR, 0));
    }

    @Override
    public void hideCursor() throws IOException {
        ops.add(new Op(OpType.HIDE_CURSOR, 0));
    }

    @Override
    public Position getCursorPosition() throws IOException {
        return new Position(0, 0);
    }

    @Override
    public void setCursorPosition(Position position) throws IOException {
    }

    @Override
    public void enterAlternateScreen() throws IOException {
    }

    @Override
    public void leaveAlternateScreen() throws IOException {
    }

    @Override
    public void enableRawMode() throws IOException {
    }

    @Override
    public void disableRawMode() throws IOException {
    }

    @Override
    public void onResize(Runnable handler) {
    }

    @Override
    public int read(int timeoutMs) throws IOException {
        return -2;
    }

    @Override
    public int peek(int timeoutMs) throws IOException {
        return -2;
    }

    @Override
    public void close() throws IOException {
    }

    // -- Structured operation types --

    public enum OpType {
        INSERT_LINES, DELETE_LINES, CURSOR_UP, CURSOR_DOWN, CURSOR_LEFT, CURSOR_RIGHT, ERASE_EOL, CARRIAGE_RETURN, SHOW_CURSOR, HIDE_CURSOR
    }

    public static final class Op {
        private final OpType type;
        private final int count;

        public Op(OpType type, int count) {
            this.type = type;
            this.count = count;
        }

        public OpType type() {
            return type;
        }

        public int count() {
            return count;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof Op)) {
                return false;
            }
            Op op = (Op) o;
            return count == op.count && type == op.type;
        }

        @Override
        public int hashCode() {
            return 31 * type.hashCode() + count;
        }

        @Override
        public String toString() {
            if (count == 0) {
                return type.name();
            }
            return type.name() + "(" + count + ")";
        }
    }

    /**
     * Assertion DSL for verifying captured operations.
     */
    public static class OpAssert {
        private final List<Op> ops;

        OpAssert(List<Op> ops) {
            this.ops = ops;
        }

        /**
         * Asserts that an INSERT_LINES operation with the given count was recorded.
         */
        public OpAssert hasInsertLines(int n) {
            assertContains(OpType.INSERT_LINES, n);
            return this;
        }

        /**
         * Asserts that a DELETE_LINES operation with the given count was recorded.
         */
        public OpAssert hasDeleteLines(int n) {
            assertContains(OpType.DELETE_LINES, n);
            return this;
        }

        /**
         * Asserts that a CURSOR_UP operation with the given count was recorded.
         */
        public OpAssert hasCursorUp(int n) {
            assertContains(OpType.CURSOR_UP, n);
            return this;
        }

        /**
         * Asserts that a CURSOR_DOWN operation with the given count was recorded.
         */
        public OpAssert hasCursorDown(int n) {
            assertContains(OpType.CURSOR_DOWN, n);
            return this;
        }

        /**
         * Asserts that a CURSOR_RIGHT operation with the given count was recorded.
         */
        public OpAssert hasCursorRight(int n) {
            assertContains(OpType.CURSOR_RIGHT, n);
            return this;
        }

        /**
         * Asserts that a CURSOR_LEFT operation with the given count was recorded.
         */
        public OpAssert hasCursorLeft(int n) {
            assertContains(OpType.CURSOR_LEFT, n);
            return this;
        }

        /**
         * Asserts that an ERASE_EOL operation was recorded.
         */
        public OpAssert hasEraseEol() {
            assertContains(OpType.ERASE_EOL, 0);
            return this;
        }

        /**
         * Asserts that a CARRIAGE_RETURN operation was recorded.
         */
        public OpAssert hasCarriageReturn() {
            assertContains(OpType.CARRIAGE_RETURN, 0);
            return this;
        }

        /**
         * Asserts that no DELETE_LINES operations were recorded.
         */
        public OpAssert hasNoDeleteLines() {
            assertNotContainsType(OpType.DELETE_LINES);
            return this;
        }

        /**
         * Asserts that a specific operation was NOT recorded.
         */
        public OpAssert hasNo(OpType type, int count) {
            Op unexpected = new Op(type, count);
            if (ops.contains(unexpected)) {
                throw new AssertionError(
                        "Unexpected operation " + unexpected + " found in: " + ops);
            }
            return this;
        }

        /**
         * Asserts that no INSERT_LINES operations were recorded.
         */
        public OpAssert hasNoInsertLines() {
            assertNotContainsType(OpType.INSERT_LINES);
            return this;
        }

        /**
         * Asserts that the total number of operations matches.
         */
        public OpAssert hasSize(int expected) {
            if (ops.size() != expected) {
                throw new AssertionError(
                        "Expected " + expected + " operations, but got " + ops.size() + ": " + ops);
            }
            return this;
        }

        private void assertContains(OpType type, int count) {
            Op expected = new Op(type, count);
            if (!ops.contains(expected)) {
                throw new AssertionError(
                        "Expected operation " + expected + " not found in: " + ops);
            }
        }

        private void assertNotContainsType(OpType type) {
            for (Op op : ops) {
                if (op.type() == type) {
                    throw new AssertionError("Expected no " + type + " operations, but found: " + op
                            + " in: " + ops);
                }
            }
        }
    }

    /**
     * Assertion DSL for verifying the transcript ordering.
     * <p>
     * The transcript contains both {@link Op} instances and raw output
     * {@link String}s in the order they were emitted. Assertions work with
     * cumulative raw text to handle character-by-character writes (e.g.,
     * cell-by-cell rendering produces individual chars).
     */
    public static class TranscriptAssert {
        private final List<Object> transcript;
        private int position;

        TranscriptAssert(List<Object> transcript) {
            this.transcript = transcript;
            this.position = 0;
        }

        /**
         * Advances past entries until finding an op matching the given type and count.
         * Fails if the end is reached without finding a match.
         */
        public TranscriptAssert expectEventually(OpType type, int count) {
            Op expected = new Op(type, count);
            for (int i = position; i < transcript.size(); i++) {
                if (expected.equals(transcript.get(i))) {
                    position = i + 1;
                    return this;
                }
            }
            throw new AssertionError("Expected " + expected + " not found after position "
                    + position + " in transcript: " + formatTranscript());
        }

        /**
         * Advances past entries until the cumulative raw text from current position
         * contains the given text. Handles character-by-character writes.
         */
        public TranscriptAssert expectRawContaining(String text) {
            StringBuilder accumulated = new StringBuilder();
            for (int i = position; i < transcript.size(); i++) {
                Object entry = transcript.get(i);
                if (entry instanceof String) {
                    accumulated.append((String) entry);
                    if (accumulated.toString().contains(text)) {
                        position = i + 1;
                        return this;
                    }
                }
            }
            throw new AssertionError("Expected raw output containing \"" + escape(text)
                    + "\" not found after position " + position + ". Accumulated raw: \""
                    + escape(accumulated.toString()) + "\"\n  Transcript: " + formatTranscript());
        }

        /**
         * Asserts that a specific Op appears BEFORE the cumulative raw output contains
         * the given text. This validates output ordering.
         */
        public TranscriptAssert hasOpBefore(OpType type, int count, String rawText) {
            Op expected = new Op(type, count);
            int opIndex = -1;
            int rawCompleteIndex = -1;
            StringBuilder accumulated = new StringBuilder();

            for (int i = 0; i < transcript.size(); i++) {
                Object entry = transcript.get(i);
                if (opIndex < 0 && expected.equals(entry)) {
                    opIndex = i;
                }
                if (rawCompleteIndex < 0 && entry instanceof String) {
                    accumulated.append((String) entry);
                    if (accumulated.toString().contains(rawText)) {
                        rawCompleteIndex = i;
                    }
                }
                if (opIndex >= 0 && rawCompleteIndex >= 0) {
                    break;
                }
            }

            if (opIndex < 0) {
                throw new AssertionError("Operation " + expected + " not found in transcript: "
                        + formatTranscript());
            }
            if (rawCompleteIndex < 0) {
                throw new AssertionError("Raw output containing \"" + escape(rawText)
                        + "\" not found in transcript: " + formatTranscript());
            }
            if (opIndex >= rawCompleteIndex) {
                throw new AssertionError("Expected " + expected + " (at " + opIndex
                        + ") to appear before raw \"" + escape(rawText) + "\" (completed at "
                        + rawCompleteIndex + ") in transcript: " + formatTranscript());
            }
            return this;
        }

        private String formatTranscript() {
            StringBuilder sb = new StringBuilder();
            sb.append("[\n");
            for (int i = 0; i < transcript.size(); i++) {
                sb.append("  ").append(i).append(": ");
                Object entry = transcript.get(i);
                if (entry instanceof String) {
                    sb.append("RAW(\"").append(escape((String) entry)).append("\")");
                } else {
                    sb.append(entry);
                }
                sb.append("\n");
            }
            sb.append("]");
            return sb.toString();
        }

        private static String escape(String s) {
            return s.replace("\n", "\\n").replace("\r", "\\r").replace("\u001b", "\\e");
        }
    }
}
