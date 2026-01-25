/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.internal.record;

import java.nio.charset.StandardCharsets;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Locale;
import java.nio.file.Paths;
import dev.tamboui.export.BufferSvgExporter;
import dev.tamboui.buffer.Buffer;
/**
 * Plays back scripted interactions for demo recording.
 * Supports VHS tape format (charmbracelet/vhs).
 * This is an internal API and not part of the public contract.
 */
final class InteractionPlayer {

    private static final int ESC = 27;

    private final List<Interaction> interactions;
    private final Deque<Integer> pendingBytes = new ArrayDeque<>();
    private int currentIndex = 0;
    private long waitUntil = 0;
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
        boolean visible = true; // Track Show/Hide state

        for (String line : lines) {
            line = line.trim();
            // Skip empty lines and comments
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }

            // Handle Source directive (include another tape file)
            if (line.toLowerCase(Locale.ROOT).startsWith("source ")) {
                String includePath = line.substring(7).trim();
                Path includeFile = path.getParent().resolve(includePath);
                if (Files.exists(includeFile)) {
                    loadTapeFile(includeFile, outputPath, interactions);
                }
                continue;
            }

            // Skip Set commands (settings)
            if (line.toLowerCase(Locale.ROOT).startsWith("set ")) {
                continue;
            }

            // Skip Output command
            if (line.toLowerCase(Locale.ROOT).startsWith("output ")) {
                continue;
            }

            // Track visibility state
            if (line.equalsIgnoreCase("hide")) {
                visible = false;
                continue;
            }
            if (line.equalsIgnoreCase("show")) {
                visible = true;
                continue;
            }

            // Only process interactions when visible
            if (!visible) {
                continue;
            }

            parseVhsCommand(outputPath, line, interactions);
        }
    }

    private static void parseVhsCommand(Path outputPath, String line, List<Interaction> interactions) {
        // Check for timing suffix: Command@duration count
        // e.g., "Right@2.5s 3" means press Right 3 times with 2.5s between each
        String cmd = line;
        int atIndex = line.indexOf('@');
        int repeatCount = 1;
        int repeatDelayMs = 0;

        if (atIndex > 0) {
            // Parse timing: Command@duration count
            String afterAt = line.substring(atIndex + 1);
            cmd = line.substring(0, atIndex);
            String[] timingParts = afterAt.split("\\s+", 2);
            repeatDelayMs = parseDuration(timingParts[0]);
            if (timingParts.length > 1) {
                try {
                    repeatCount = Integer.parseInt(timingParts[1]);
                } catch (NumberFormatException e) {
                    repeatCount = 1;
                }
            }
        }

        // Parse the command
        String[] parts = cmd.split("\\s+", 2);
        String command = parts[0].toLowerCase(Locale.ROOT);
        String args = parts.length > 1 ? parts[1] : "";

        switch (command) {
            case "sleep":
                interactions.add(new Interaction.Wait(parseDuration(args)));
                break;
            case "screenshot":
                // Resolve screenshot path relative to the .cast file's directory (same folder)
                interactions.add(new Interaction.Screenshot(outputPath.getParent().resolve(args)));
                break;
            case "type":
                // Type "text" - parse quoted string and type each character
                String text = parseQuotedString(args);
                for (char c : text.toCharArray()) {
                    interactions.add(new Interaction.KeyPress(String.valueOf(c)));
                }
                break;

            case "enter":
                addRepeatedKey("enter", repeatCount, repeatDelayMs, interactions);
                break;

            case "tab":
                addRepeatedKey("tab", repeatCount, repeatDelayMs, interactions);
                break;

            case "space":
                addRepeatedKey("space", repeatCount, repeatDelayMs, interactions);
                break;

            case "backspace":
                addRepeatedKey("backspace", repeatCount, repeatDelayMs, interactions);
                break;

            case "delete":
                addRepeatedKey("delete", repeatCount, repeatDelayMs, interactions);
                break;

            case "escape":
                addRepeatedKey("escape", repeatCount, repeatDelayMs, interactions);
                break;

            case "up":
                addRepeatedKey("up", repeatCount, repeatDelayMs, interactions);
                break;

            case "down":
                addRepeatedKey("down", repeatCount, repeatDelayMs, interactions);
                break;

            case "left":
                addRepeatedKey("left", repeatCount, repeatDelayMs, interactions);
                break;

            case "right":
                addRepeatedKey("right", repeatCount, repeatDelayMs, interactions);
                break;

            case "home":
                addRepeatedKey("home", repeatCount, repeatDelayMs, interactions);
                break;

            case "end":
                addRepeatedKey("end", repeatCount, repeatDelayMs, interactions);
                break;

            case "pageup":
                addRepeatedKey("pageup", repeatCount, repeatDelayMs, interactions);
                break;

            case "pagedown":
                addRepeatedKey("pagedown", repeatCount, repeatDelayMs, interactions);
                break;

            case "ctrl+c":
                addRepeatedKey("ctrl+c", repeatCount, repeatDelayMs, interactions);
                break;

            default:
                // Check for Ctrl+key patterns
                if (command.startsWith("ctrl+")) {
                    addRepeatedKey(command, repeatCount, repeatDelayMs, interactions);
                }
                break;
        }
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
        if (waitUntil > 0 && System.currentTimeMillis() < waitUntil) {
            return false;
        }
        return currentIndex >= interactions.size() && pendingBytes.isEmpty();
    }

    /**
     * Peeks at the next byte without consuming it.
     *
     * @return the next byte, or -1 if no byte available
     */
    int peekByte() {
        if (!pendingBytes.isEmpty()) {
            return pendingBytes.peek();
        }
        return -1;
    }

    /**
     * Gets the next byte to return from read(), or -2 for timeout.
     * This method handles wait commands by sleeping.
     *
     * @param maxWaitMs maximum time to wait
     * @return the next byte, or -2 for timeout, or 'q' if finished
     */
    int nextByte(int maxWaitMs) {
        // Return pending bytes first
        if (!pendingBytes.isEmpty()) {
            return pendingBytes.poll();
        }

        // Check if we're waiting
        if (waitUntil > 0) {
            long remaining = waitUntil - System.currentTimeMillis();
            if (remaining > 0) {
                try {
                    Thread.sleep(Math.min(remaining, maxWaitMs));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                // Still waiting
                if (System.currentTimeMillis() < waitUntil) {
                    return -2;
                }
            }
            waitUntil = 0;
        }

        // Process next interaction
        while (currentIndex < interactions.size()) {
            Interaction interaction = interactions.get(currentIndex++);

            if (interaction instanceof Interaction.Wait) {
                Interaction.Wait wait = (Interaction.Wait) interaction;
                waitUntil = System.currentTimeMillis() + wait.millis();
                return -2; // Timeout to trigger redraw
            } else if (interaction instanceof Interaction.KeyPress) {
                Interaction.KeyPress keyPress = (Interaction.KeyPress) interaction;
                enqueueKey(keyPress.key());
                if (!pendingBytes.isEmpty()) {
                    return pendingBytes.poll();
                }
            } else if (interaction instanceof Interaction.Screenshot) {
                Interaction.Screenshot screenshot = (Interaction.Screenshot) interaction;
                String svg = BufferSvgExporter.exportSvg(buffer);
                try {
                    Files.createDirectories(screenshot.path().getParent());
                    Files.write(screenshot.path(), svg.getBytes(StandardCharsets.UTF_8));
                } catch (IOException e) {
                    throw new UncheckedIOException("Warning: Failed to write screenshot: " + e.getMessage(),e);
                }
                return -2; // Timeout to trigger redraw
            }
        }

        // All interactions done
        return -2;
    }

    private void enqueueKey(String keySpec) {
        String lower = keySpec.toLowerCase(Locale.ROOT);

        // Check for modifier prefixes
        boolean ctrl = false;
        boolean shift = false;

        while (lower.contains("+")) {
            int plusIdx = lower.indexOf('+');
            String prefix = lower.substring(0, plusIdx);
            lower = lower.substring(plusIdx + 1);

            switch (prefix) {
                case "ctrl":
                case "control":
                    ctrl = true;
                    break;
                case "shift":
                    shift = true;
                    break;
                default:
                    break;
            }
        }

        // Convert key name to bytes
        switch (lower) {
            case "up":
            case "arrow_up":
                pendingBytes.add(ESC);
                pendingBytes.add((int) '[');
                pendingBytes.add((int) 'A');
                break;
            case "down":
            case "arrow_down":
                pendingBytes.add(ESC);
                pendingBytes.add((int) '[');
                pendingBytes.add((int) 'B');
                break;
            case "right":
            case "arrow_right":
                pendingBytes.add(ESC);
                pendingBytes.add((int) '[');
                pendingBytes.add((int) 'C');
                break;
            case "left":
            case "arrow_left":
                pendingBytes.add(ESC);
                pendingBytes.add((int) '[');
                pendingBytes.add((int) 'D');
                break;
            case "enter":
            case "return":
                pendingBytes.add((int) '\r');
                break;
            case "esc":
            case "escape":
                pendingBytes.add(ESC);
                break;
            case "tab":
                pendingBytes.add((int) '\t');
                break;
            case "space":
                pendingBytes.add((int) ' ');
                break;
            case "backspace":
            case "back":
                pendingBytes.add(127);
                break;
            case "home":
                pendingBytes.add(ESC);
                pendingBytes.add((int) '[');
                pendingBytes.add((int) 'H');
                break;
            case "end":
                pendingBytes.add(ESC);
                pendingBytes.add((int) '[');
                pendingBytes.add((int) 'F');
                break;
            case "delete":
                pendingBytes.add(ESC);
                pendingBytes.add((int) '[');
                pendingBytes.add((int) '3');
                pendingBytes.add((int) '~');
                break;
            case "pageup":
                pendingBytes.add(ESC);
                pendingBytes.add((int) '[');
                pendingBytes.add((int) '5');
                pendingBytes.add((int) '~');
                break;
            case "pagedown":
                pendingBytes.add(ESC);
                pendingBytes.add((int) '[');
                pendingBytes.add((int) '6');
                pendingBytes.add((int) '~');
                break;
            default:
                // Single character - use original keySpec to preserve case
                if (keySpec.length() == 1) {
                    char c = keySpec.charAt(0);
                    if (ctrl && Character.isLetter(c)) {
                        // Ctrl+letter = letter - 'a' + 1
                        pendingBytes.add(Character.toLowerCase(c) - 'a' + 1);
                    } else if (shift && Character.isLetter(c)) {
                        pendingBytes.add((int) Character.toUpperCase(c));
                    } else {
                        pendingBytes.add((int) c);
                    }
                }
                break;
        }
    }
}
