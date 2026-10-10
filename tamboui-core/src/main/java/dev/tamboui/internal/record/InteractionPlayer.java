/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.internal.record;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import dev.tamboui.buffer.Buffer;
import dev.tamboui.layout.Rect;

import static dev.tamboui.export.ExportRequest.export;
/**
 * Plays back scripted interactions for demo recording.
 * Supports VHS tape format (charmbracelet/vhs).
 * This is an internal API and not part of the public contract.
 */
final class InteractionPlayer {

    private static final int ESC = 27;
    /** How long a Wait for text on the screen waits when the tape gives no timeout, as in vhs. */
    private static final int DEFAULT_WAIT_TIMEOUT_MS = 15000;
    /** How often the screen is checked while waiting for text on it. */
    private static final int WAIT_POLL_MS = 50;
    /** The number in the escape sequence of the function keys F5 to F12: ESC [ n ~. */
    private static final int[] FUNCTION_KEY_CODES = {15, 17, 18, 19, 20, 21, 23, 24};
    private static final Set<String> MODIFIERS = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
            "ctrl", "control", "shift", "alt")));
    private static final Set<String> NAMED_KEYS = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
            "enter", "return", "tab", "space", "backspace", "back", "delete", "insert", "escape", "esc",
            "up", "down", "left", "right", "arrow_up", "arrow_down", "arrow_left", "arrow_right",
            "home", "end", "pageup", "pagedown")));

    private final List<Interaction> interactions;
    private final Deque<Integer> pendingBytes = new ArrayDeque<>();
    private int currentIndex = 0;
    private long waitUntilNanos = 0;
    private Interaction.WaitFor waitingFor;
    private long waitForDeadlineNanos;
    private volatile boolean hidden;
    private final Buffer buffer;

    InteractionPlayer(List<Interaction> interactions, Buffer buffer) {
        this.interactions = interactions;
        this.buffer = buffer;
    }

    /**
     * Loads interactions from a VHS tape file.
     *
     * @param path the tape file path
     * @return list of interactions, empty if file doesn't exist or has no interactions
     */
    static List<Interaction> loadFromFile(Path path, Path outputPath) {
        List<Interaction> interactions = new ArrayList<>();
        if (path == null || !Files.exists(path)) {
            return interactions;
        }

        try {
            loadTapeFile(path, outputPath, interactions);
        } catch (IOException e) {
            System.err.println("Warning: Failed to load tape file: " + e.getMessage());
        }

        // Post-process to collapse escape sequences into single key presses
        return collapseEscapeSequences(interactions);
    }

    /**
     * Collapses escape sequences like ESC + [ + C into single arrow key presses.
     * This handles tape files that use "Escape" + "Type [C" instead of "Right".
     */
    private static List<Interaction> collapseEscapeSequences(List<Interaction> interactions) {
        List<Interaction> result = new ArrayList<>();
        int i = 0;
        while (i < interactions.size()) {
            Interaction current = interactions.get(i);

            // Look for pattern: KeyPress("escape") + KeyPress("[") + KeyPress("A/B/C/D")
            if (current instanceof Interaction.KeyPress &&
                    "escape".equals(((Interaction.KeyPress) current).key()) &&
                    i + 2 < interactions.size()) {

                Interaction next1 = interactions.get(i + 1);
                Interaction next2 = interactions.get(i + 2);

                if (next1 instanceof Interaction.KeyPress &&
                        "[".equals(((Interaction.KeyPress) next1).key()) &&
                        next2 instanceof Interaction.KeyPress) {

                    String finalChar = ((Interaction.KeyPress) next2).key();
                    String arrowKey;
                    if ("A".equals(finalChar)) {
                        arrowKey = "up";
                    } else if ("B".equals(finalChar)) {
                        arrowKey = "down";
                    } else if ("C".equals(finalChar)) {
                        arrowKey = "right";
                    } else if ("D".equals(finalChar)) {
                        arrowKey = "left";
                    } else if ("H".equals(finalChar)) {
                        arrowKey = "home";
                    } else if ("F".equals(finalChar)) {
                        arrowKey = "end";
                    } else {
                        arrowKey = null;
                    }

                    if (arrowKey != null) {
                        result.add(new Interaction.KeyPress(arrowKey));
                        // Add a small delay after arrow keys to allow demo to process and redraw
                        result.add(new Interaction.Wait(100));
                        i += 3; // Skip the 3 interactions we collapsed
                        continue;
                    }
                }
            }

            // No collapse, add as-is
            result.add(current);
            i++;
        }
        return result;
    }

    private static void loadTapeFile(Path path, Path outputPath, List<Interaction> interactions) throws IOException {
        List<String> lines = Files.readAllLines(path);

        for (String line : lines) {
            line = line.trim();
            // Skip empty lines and comments
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            String lower = line.toLowerCase(Locale.ROOT);

            // Handle Source directive (include another tape file)
            if (lower.startsWith("source ")) {
                String includePath = line.substring(7).trim();
                Path includeFile = path.getParent().resolve(includePath);
                if (Files.exists(includeFile)) {
                    loadTapeFile(includeFile, outputPath, interactions);
                } else {
                    warn("Source tape not found: " + includeFile);
                }
                continue;
            }

            // Skip the directives that only apply to the vhs tool (settings, output, required programs)
            if (lower.startsWith("set ") || lower.startsWith("output ") || lower.startsWith("require ")) {
                continue;
            }

            // Hide and Show keep playing the interactions in between, but leave them out of the recording
            if (lower.equals("hide")) {
                interactions.add(new Interaction.Visibility(true));
                continue;
            }
            if (lower.equals("show")) {
                interactions.add(new Interaction.Visibility(false));
                continue;
            }

            parseVhsCommand(outputPath, line, interactions);
        }
    }

    private static void parseVhsCommand(Path outputPath, String line, List<Interaction> interactions) {
        // The command is the first word, and it may carry a timing suffix: Command@duration.
        // "Right@2.5s 3" presses Right 3 times with 2.5s between each, "Type@50ms "text"" types a character
        // every 50ms, and "Wait@10s /Ready/" waits at most 10s. Only the first word is checked for '@', so
        // Type "user@example.com" types the text as it is.
        String[] parts = line.split("\\s+", 2);
        String head = parts[0];
        String args = parts.length > 1 ? parts[1].trim() : "";
        int delayMs = 0;
        int atIndex = head.indexOf('@');
        if (atIndex > 0) {
            delayMs = parseDuration(head.substring(atIndex + 1));
            head = head.substring(0, atIndex);
        }
        String command = head.toLowerCase(Locale.ROOT);

        switch (command) {
            case "sleep":
                interactions.add(new Interaction.Wait(parseDuration(args)));
                break;
            case "screenshot":
                // Resolve screenshot path relative to the .cast file's directory (same folder)
                interactions.add(new Interaction.Screenshot(outputPath.getParent().resolve(args)));
                break;
            case "type":
                addTypedText(parseQuotedString(args), delayMs, interactions);
                break;
            case "wait":
            case "wait+line":
            case "wait+screen":
                addWaitFor(line, "wait+screen".equals(command), args, delayMs, interactions);
                break;
            default:
                if (isKey(head)) {
                    addRepeatedKey(command, parseRepeatCount(line, args), delayMs, interactions);
                } else {
                    warn("Ignoring unknown tape command: " + line);
                }
                break;
        }
    }

    /**
     * Whether the spec is a key the player can press: a named key such as {@code Enter} or {@code PageUp}, a function
     * key {@code F1} to {@code F12}, or a single character with modifiers such as {@code Ctrl+c}. Named and function
     * keys can carry modifiers too, such as {@code Shift+F8} or {@code Ctrl+Up}.
     */
    static boolean isKey(String spec) {
        String key = spec;
        boolean modified = false;
        int plus;
        while ((plus = key.indexOf('+')) > 0 && plus < key.length() - 1) {
            if (!MODIFIERS.contains(key.substring(0, plus).toLowerCase(Locale.ROOT))) {
                return false;
            }
            key = key.substring(plus + 1);
            modified = true;
        }
        String lower = key.toLowerCase(Locale.ROOT);
        return NAMED_KEYS.contains(lower) || functionKeyNumber(lower) > 0 || (modified && key.length() == 1);
    }

    private static int parseRepeatCount(String line, String args) {
        if (args.isEmpty()) {
            return 1;
        }
        try {
            return Math.max(1, Integer.parseInt(args));
        } catch (NumberFormatException e) {
            warn("Ignoring the repeat count of: " + line);
            return 1;
        }
    }

    private static void addTypedText(String text, int delayMs, List<Interaction> interactions) {
        for (int i = 0; i < text.length(); i++) {
            if (i > 0 && delayMs > 0) {
                interactions.add(new Interaction.Wait(delayMs));
            }
            interactions.add(new Interaction.KeyPress(String.valueOf(text.charAt(i))));
        }
    }

    private static void addWaitFor(String line, boolean screen, String args, int timeoutMs,
                                   List<Interaction> interactions) {
        if (args.length() < 2 || !args.startsWith("/") || !args.endsWith("/")) {
            warn("Ignoring Wait without a /regex/ to wait for: " + line);
            return;
        }
        try {
            Pattern pattern = Pattern.compile(args.substring(1, args.length() - 1));
            interactions.add(new Interaction.WaitFor(pattern, screen, timeoutMs > 0 ? timeoutMs : DEFAULT_WAIT_TIMEOUT_MS));
        } catch (PatternSyntaxException e) {
            warn("Ignoring Wait with an invalid regex: " + line + " (" + e.getDescription() + ")");
        }
    }

    private static void warn(String message) {
        System.err.println("Warning: " + message);
    }

    private static void addRepeatedKey(String key, int count, int delayMs, List<Interaction> interactions) {
        for (int i = 0; i < count; i++) {
            if (i > 0 && delayMs > 0) {
                interactions.add(new Interaction.Wait(delayMs));
            }
            interactions.add(new Interaction.KeyPress(key));
        }
    }

    private static int parseDuration(String duration) {
        duration = duration.trim().toLowerCase(Locale.ROOT);
        try {
            if (duration.endsWith("ms")) {
                return Integer.parseInt(duration.substring(0, duration.length() - 2));
            } else if (duration.endsWith("s")) {
                return (int) (Double.parseDouble(duration.substring(0, duration.length() - 1)) * 1000);
            } else if (duration.contains(".")) {
                // Bare decimal like "0.5" means seconds
                return (int) (Double.parseDouble(duration) * 1000);
            } else {
                // Bare integer - assume seconds for VHS compatibility
                return Integer.parseInt(duration) * 1000;
            }
        } catch (NumberFormatException e) {
            return 1000; // Default 1 second
        }
    }

    private static String parseQuotedString(String s) {
        s = s.trim();
        if (s.startsWith("\"") && s.endsWith("\"") && s.length() >= 2) {
            s = s.substring(1, s.length() - 1);
        }
        // Handle escape sequences
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\\' && i + 1 < s.length()) {
                char next = s.charAt(i + 1);
                switch (next) {
                    case 'n':
                        result.append('\n');
                        i++;
                        break;
                    case 't':
                        result.append('\t');
                        i++;
                        break;
                    case 'r':
                        result.append('\r');
                        i++;
                        break;
                    case 'x':
                        // Hex escape \x01 etc.
                        if (i + 3 < s.length()) {
                            try {
                                int code = Integer.parseInt(s.substring(i + 2, i + 4), 16);
                                result.append((char) code);
                                i += 3;
                            } catch (NumberFormatException e) {
                                result.append(c);
                            }
                        } else {
                            result.append(c);
                        }
                        break;
                    case '"':
                        result.append('"');
                        i++;
                        break;
                    case '\\':
                        result.append('\\');
                        i++;
                        break;
                    default:
                        result.append(c);
                        break;
                }
            } else {
                result.append(c);
            }
        }
        return result.toString();
    }

    /**
     * Returns true if there are no interactions defined.
     */
    boolean hasNoInteractions() {
        return interactions.isEmpty();
    }

    /**
     * Returns true if all interactions have been played.
     */
    boolean isFinished() {
        // Not finished if there's an active wait
        if (waitUntilNanos > 0 && System.nanoTime() < waitUntilNanos) {
            return false;
        }
        if (waitingFor != null) {
            return false;
        }
        return currentIndex >= interactions.size() && pendingBytes.isEmpty();
    }

    /**
     * Returns true while the interactions between a Hide and a Show are played; their frames are left out of the
     * recording.
     */
    boolean isHidden() {
        return hidden;
    }

    /**
     * Peeks at the next code point without consuming it.
     *
     * @return the next code point, or -1 if none available
     */
    int peekCodePoint() {
        if (!pendingBytes.isEmpty()) {
            return pendingBytes.peek();
        }
        return -1;
    }

    /**
     * Gets the next code point to return from read(), or -2 for timeout.
     * This method handles wait commands by sleeping.
     *
     * @param maxWaitMs maximum time to wait
     * @return the next code point, or -2 for timeout, or 'q' if finished
     */
    int nextCodePoint(int maxWaitMs) {
        // Return pending bytes first
        if (!pendingBytes.isEmpty()) {
            return pendingBytes.poll();
        }

        // Check if we're waiting
        if (waitUntilNanos > 0) {
            long remainingNanos = waitUntilNanos - System.nanoTime();
            if (remainingNanos > 0) {
                long remainingMs = TimeUnit.NANOSECONDS.toMillis(remainingNanos);
                try {
                    Thread.sleep(Math.min(remainingMs, maxWaitMs));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                // Still waiting
                if (System.nanoTime() < waitUntilNanos) {
                    return -2;
                }
            }
            waitUntilNanos = 0;
        }

        // Check if we're waiting for text on the screen
        if (waitingFor != null) {
            if (!screenMatches(waitingFor)) {
                if (System.nanoTime() < waitForDeadlineNanos) {
                    try {
                        Thread.sleep(Math.min(maxWaitMs, WAIT_POLL_MS));
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    return -2;
                }
                warn("Timed out after " + waitingFor.timeoutMillis() + "ms waiting for /" + waitingFor.pattern()
                        + "/ on the screen");
            }
            waitingFor = null;
        }

        // Process next interaction
        while (currentIndex < interactions.size()) {
            Interaction interaction = interactions.get(currentIndex++);

            if (interaction instanceof Interaction.Wait) {
                Interaction.Wait wait = (Interaction.Wait) interaction;
                waitUntilNanos = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(wait.millis());
                return -2; // Timeout to trigger redraw
            } else if (interaction instanceof Interaction.WaitFor) {
                waitingFor = (Interaction.WaitFor) interaction;
                waitForDeadlineNanos = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(waitingFor.timeoutMillis());
                return -2; // Timeout to trigger redraw
            } else if (interaction instanceof Interaction.Visibility) {
                hidden = ((Interaction.Visibility) interaction).hidden();
            } else if (interaction instanceof Interaction.KeyPress) {
                Interaction.KeyPress keyPress = (Interaction.KeyPress) interaction;
                enqueueKey(keyPress.key());
                if (!pendingBytes.isEmpty()) {
                    return pendingBytes.poll();
                }
            } else if (interaction instanceof Interaction.Screenshot) {
                Interaction.Screenshot screenshot = (Interaction.Screenshot) interaction;
                try {
                    Files.createDirectories(screenshot.path().getParent());
                    export(buffer).toFile(screenshot.path());
                } catch (IOException e) {
                    throw new UncheckedIOException("Warning: Failed to write screenshot: " + e.getMessage(), e);
                }
                return -2; // Timeout to trigger redraw
            }
        }

        // All interactions done
        return -2;
    }

    /**
     * Whether the screen shows text matching the pattern: on one line, or anywhere on the screen with its lines joined
     * by newlines for Wait+Screen.
     */
    private boolean screenMatches(Interaction.WaitFor waitFor) {
        if (buffer == null) {
            return false;
        }
        Rect area = buffer.area();
        StringBuilder screen = new StringBuilder();
        for (int y = area.top(); y < area.bottom(); y++) {
            StringBuilder row = new StringBuilder();
            for (int x = area.left(); x < area.right(); x++) {
                row.append(buffer.get(x, y).symbol());
            }
            if (!waitFor.screen() && waitFor.pattern().matcher(row).find()) {
                return true;
            }
            if (y > area.top()) {
                screen.append('\n');
            }
            screen.append(row);
        }
        return waitFor.screen() && waitFor.pattern().matcher(screen).find();
    }

    /**
     * Queues the bytes a terminal sends for the key, all at once so that an escape sequence is read as one key.
     * Modifiers are encoded the xterm way: a parameter in the sequence for the cursor, editing and function keys,
     * and an ESC prefix for Alt with the other keys.
     */
    private void enqueueKey(String keySpec) {
        boolean ctrl = false;
        boolean shift = false;
        boolean alt = false;
        String keyName = keySpec;
        int plus;
        // A lone "+" (typed text) is a key of its own, not a modifier separator
        while ((plus = keyName.indexOf('+')) > 0 && plus < keyName.length() - 1) {
            String prefix = keyName.substring(0, plus).toLowerCase(Locale.ROOT);
            if ("ctrl".equals(prefix) || "control".equals(prefix)) {
                ctrl = true;
            } else if ("shift".equals(prefix)) {
                shift = true;
            } else if ("alt".equals(prefix)) {
                alt = true;
            } else {
                break;
            }
            keyName = keyName.substring(plus + 1);
        }
        String lower = keyName.toLowerCase(Locale.ROOT);
        // the xterm modifier parameter: 1 + shift + 2 * alt + 4 * ctrl
        int modifiers = 1 + (shift ? 1 : 0) + (alt ? 2 : 0) + (ctrl ? 4 : 0);

        switch (lower) {
            case "up":
            case "arrow_up":
                addCursorKey('A', modifiers);
                break;
            case "down":
            case "arrow_down":
                addCursorKey('B', modifiers);
                break;
            case "right":
            case "arrow_right":
                addCursorKey('C', modifiers);
                break;
            case "left":
            case "arrow_left":
                addCursorKey('D', modifiers);
                break;
            case "home":
                addCursorKey('H', modifiers);
                break;
            case "end":
                addCursorKey('F', modifiers);
                break;
            case "insert":
                addTildeKey(2, modifiers);
                break;
            case "delete":
                addTildeKey(3, modifiers);
                break;
            case "pageup":
                addTildeKey(5, modifiers);
                break;
            case "pagedown":
                addTildeKey(6, modifiers);
                break;
            case "enter":
            case "return":
                addWithAlt('\r', alt);
                break;
            case "esc":
            case "escape":
                pendingBytes.add(ESC);
                break;
            case "tab":
                if (shift) {
                    // back tab
                    pendingBytes.add(ESC);
                    pendingBytes.add((int) '[');
                    pendingBytes.add((int) 'Z');
                } else {
                    addWithAlt('\t', alt);
                }
                break;
            case "space":
                addWithAlt(ctrl ? 0 : ' ', alt);
                break;
            case "backspace":
            case "back":
                addWithAlt(127, alt);
                break;
            default:
                int functionKey = functionKeyNumber(lower);
                if (functionKey > 0) {
                    addFunctionKey(functionKey, modifiers);
                } else if (keyName.length() == 1) {
                    // Single character - use keyName (with modifiers stripped) to preserve case
                    char c = keyName.charAt(0);
                    if (ctrl && Character.isLetter(c)) {
                        // Ctrl+letter = letter - 'a' + 1
                        addWithAlt(Character.toLowerCase(c) - 'a' + 1, alt);
                    } else if (shift && Character.isLetter(c)) {
                        addWithAlt(Character.toUpperCase(c), alt);
                    } else {
                        addWithAlt(c, alt);
                    }
                }
                break;
        }
    }

    /** The number of a function key name such as {@code f8}, or 0 when it is not F1 to F12. */
    static int functionKeyNumber(String lower) {
        if (lower.length() < 2 || lower.length() > 3 || lower.charAt(0) != 'f') {
            return 0;
        }
        try {
            int n = Integer.parseInt(lower.substring(1));
            return n >= 1 && n <= 12 ? n : 0;
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /** ESC [ X, or ESC [ 1 ; m X with modifiers: the cursor keys, Home, End, and F1 to F4 with modifiers. */
    private void addCursorKey(char finalByte, int modifiers) {
        pendingBytes.add(ESC);
        pendingBytes.add((int) '[');
        if (modifiers > 1) {
            addDigits(1);
            pendingBytes.add((int) ';');
            addDigits(modifiers);
        }
        pendingBytes.add((int) finalByte);
    }

    /** ESC [ n ~, or ESC [ n ; m ~ with modifiers: the editing keys and F5 to F12. */
    private void addTildeKey(int code, int modifiers) {
        pendingBytes.add(ESC);
        pendingBytes.add((int) '[');
        addDigits(code);
        if (modifiers > 1) {
            pendingBytes.add((int) ';');
            addDigits(modifiers);
        }
        pendingBytes.add((int) '~');
    }

    private void addFunctionKey(int n, int modifiers) {
        if (n > 4) {
            addTildeKey(FUNCTION_KEY_CODES[n - 5], modifiers);
        } else if (modifiers > 1) {
            addCursorKey((char) ('P' + n - 1), modifiers);
        } else {
            // ESC O P to ESC O S
            pendingBytes.add(ESC);
            pendingBytes.add((int) 'O');
            pendingBytes.add('P' + n - 1);
        }
    }

    private void addDigits(int n) {
        for (char digit : Integer.toString(n).toCharArray()) {
            pendingBytes.add((int) digit);
        }
    }

    private void addWithAlt(int code, boolean alt) {
        if (alt) {
            pendingBytes.add(ESC);
        }
        pendingBytes.add(code);
    }
}
