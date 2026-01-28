/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.backend.jline3;

import dev.tamboui.buffer.Cell;
import dev.tamboui.buffer.CellUpdate;
import dev.tamboui.layout.Position;
import dev.tamboui.layout.Size;
import dev.tamboui.style.Hyperlink;
import dev.tamboui.style.Modifier;
import dev.tamboui.style.Style;
import dev.tamboui.terminal.AnsiStringBuilder;
import dev.tamboui.terminal.Backend;
import dev.tamboui.terminal.Mode2027Status;
import dev.tamboui.terminal.Mode2027Support;
import org.jline.terminal.Attributes;
import org.jline.terminal.Terminal;
import org.jline.terminal.Terminal.Signal;
import org.jline.terminal.TerminalBuilder;
import org.jline.utils.InfoCmp;
import org.jline.utils.NonBlockingReader;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.EnumSet;
import java.util.Objects;

/**
 * JLine 3 based backend for terminal operations.
 */
public class JLineBackend implements Backend {

    private static final String ESC = "\033";
    private static final String CSI = ESC + "[";

    private final Terminal terminal;
    private final PrintWriter writer;
    private final NonBlockingReader reader;
    private Attributes savedAttributes;
    private boolean inAlternateScreen;
    private boolean mouseEnabled;
    private boolean mode2027Enabled;

    /**
     * Creates a new JLine 3 backend using the system terminal.
     *
     * @throws IOException if the terminal cannot be opened
     */
    public JLineBackend() throws IOException {
        this.terminal = TerminalBuilder.builder()
            .system(true)
            .jansi(true)
            .build();
        this.writer = terminal.writer();
        this.reader = terminal.reader();
        this.inAlternateScreen = false;
        this.mouseEnabled = false;
        this.mode2027Enabled = false;
    }

    @Override
    public void draw(Iterable<CellUpdate> updates) throws IOException {
        Style lastStyle = null;
        Hyperlink lastHyperlink = null;

        for (CellUpdate update : updates) {
            Cell cell = update.cell();

            // Skip continuation cells - the terminal fills them automatically
            // when printing a wide character
            if (cell.isContinuation()) {
                continue;
            }

            // Move cursor
            moveCursor(update.x(), update.y());

            // Apply style if changed
            if (!cell.style().equals(lastStyle)) {
                // Check if hyperlink changed
                Hyperlink currentHyperlink = cell.style().hyperlink().orElse(null);
                if (!Objects.equals(currentHyperlink, lastHyperlink)) {
                    // End previous hyperlink if any
                    if (lastHyperlink != null) {
                        writer.print(AnsiStringBuilder.hyperlinkEnd());
                    }
                    // Start new hyperlink if any
                    if (currentHyperlink != null) {
                        writer.print(AnsiStringBuilder.hyperlinkStart(currentHyperlink));
                    }
                    lastHyperlink = currentHyperlink;
                }

                applyStyle(cell.style());
                lastStyle = cell.style();
            }

            // Write symbol
            writer.print(cell.symbol());
        }

        // End any active hyperlink
        if (lastHyperlink != null) {
            writer.print(AnsiStringBuilder.hyperlinkEnd());
        }

        // Reset style after drawing
        writer.print(CSI + "0m");
    }

    @Override
    public void flush() throws IOException {
        writer.flush();
    }

    @Override
    public void clear() throws IOException {
        writer.print(CSI + "2J");  // Clear entire screen
        writer.print(CSI + "H");    // Move cursor to home
        writer.flush();
    }

    @Override
    public Size size() throws IOException {
        return new Size(terminal.getWidth(), terminal.getHeight());
    }

    @Override
    public void showCursor() throws IOException {
        writer.print(CSI + "?25h");
        writer.flush();
    }

    @Override
    public void hideCursor() throws IOException {
        writer.print(CSI + "?25l");
        writer.flush();
    }

    @Override
    public Position getCursorPosition() throws IOException {
        // JLine doesn't provide a direct way to query cursor position
        // Return origin as fallback
        return Position.ORIGIN;
    }

    @Override
    public void setCursorPosition(Position position) throws IOException {
        moveCursor(position.x(), position.y());
        writer.flush();
    }

    @Override
    public void enterAlternateScreen() throws IOException {
        terminal.puts(InfoCmp.Capability.enter_ca_mode);
        writer.flush();
        inAlternateScreen = true;
    }

    @Override
    public void leaveAlternateScreen() throws IOException {
        terminal.puts(InfoCmp.Capability.exit_ca_mode);
        writer.flush();
        inAlternateScreen = false;
    }

    @Override
    public void enableRawMode() throws IOException {
        savedAttributes = terminal.getAttributes();
        terminal.enterRawMode();
        // Disable signal generation so Ctrl+C goes through the event system
        // instead of generating SIGINT. This allows bindings to control quit behavior.
        Attributes attrs = terminal.getAttributes();
        attrs.setLocalFlag(Attributes.LocalFlag.ISIG, false);
        terminal.setAttributes(attrs);

        // Query and enable Mode 2027 (grapheme cluster mode) after entering raw mode
        // to prevent the response from being echoed to the terminal
        Mode2027Status status = Mode2027Support.query(this, 500);
        if (status.isSupported()) {
            Mode2027Support.enable(this);
            mode2027Enabled = true;
        }
    }

    @Override
    public void disableRawMode() throws IOException {
        // Disable Mode 2027 if it was enabled
        if (mode2027Enabled) {
            Mode2027Support.disable(this);
            mode2027Enabled = false;
        }

        if (savedAttributes != null) {
            terminal.setAttributes(savedAttributes);
        }
    }

    @Override
    public void enableMouseCapture() throws IOException {
        // Enable mouse tracking modes
        writer.print(CSI + "?1000h");  // Normal tracking
        writer.print(CSI + "?1002h");  // Button event tracking
        writer.print(CSI + "?1015h");  // urxvt style
        writer.print(CSI + "?1006h");  // SGR extended mode
        writer.flush();
        mouseEnabled = true;
    }

    @Override
    public void disableMouseCapture() throws IOException {
        writer.print(CSI + "?1006l");
        writer.print(CSI + "?1015l");
        writer.print(CSI + "?1002l");
        writer.print(CSI + "?1000l");
        writer.flush();
        mouseEnabled = false;
    }

    @Override
    public void scrollUp(int lines) throws IOException {
        writer.print(CSI + lines + "S");
        writer.flush();
    }

    @Override
    public void scrollDown(int lines) throws IOException {
        writer.print(CSI + lines + "T");
        writer.flush();
    }

    @Override
    public void insertLines(int n) throws IOException {
        if (n <= 0) {
            return;
        }
        writer.print(CSI + n + "L");
    }

    @Override
    public void deleteLines(int n) throws IOException {
        if (n <= 0) {
            return;
        }
        writer.print(CSI + n + "M");
    }

    @Override
    public void moveCursorUp(int n) throws IOException {
        if (n <= 0) {
            return;
        }
        writer.print(CSI + n + "A");
    }

    @Override
    public void moveCursorDown(int n) throws IOException {
        if (n <= 0) {
            return;
        }
        writer.print(CSI + n + "B");
    }

    @Override
    public void moveCursorRight(int n) throws IOException {
        if (n <= 0) {
            return;
        }
        writer.print(CSI + n + "C");
    }

    @Override
    public void moveCursorLeft(int n) throws IOException {
        if (n <= 0) {
            return;
        }
        writer.print(CSI + n + "D");
    }

    @Override
    public void eraseToEndOfLine() throws IOException {
        writer.print(CSI + "K");
    }

    @Override
    public void carriageReturn() throws IOException {
        writer.print("\r");
    }

    @Override
    public void writeRaw(byte[] data) throws IOException {
        terminal.output().write(data);
    }

    @Override
    public void writeRaw(String data) {
        writer.print(data);
    }

    @Override
    public void onResize(Runnable handler) {
        terminal.handle(Signal.WINCH, signal -> handler.run());
    }

    @Override
    public int read(int timeoutMs) throws IOException {
        return reader.read(timeoutMs);
    }

    @Override
    public int peek(int timeoutMs) throws IOException {
        return reader.peek(timeoutMs);
    }

    @Override
    public void close() throws IOException {
        // Reset state
        writer.print(CSI + "0m");  // Reset style

        if (mouseEnabled) {
            disableMouseCapture();
        }

        if (inAlternateScreen) {
            leaveAlternateScreen();
        }

        showCursor();
        disableRawMode();

        writer.flush();
        terminal.close();
    }

    private void moveCursor(int x, int y) {
        // ANSI uses 1-based coordinates
        writer.print(CSI + (y + 1) + ";" + (x + 1) + "H");
    }

    private void applyStyle(Style style) {
        StringBuilder sb = new StringBuilder();
        sb.append(CSI).append("0");  // Reset first

        // Foreground color
        style.fg().ifPresent(color -> {
            sb.append(";");
            sb.append(color.toAnsiForeground());
        });

        // Background color
        style.bg().ifPresent(color -> {
            sb.append(";");
            sb.append(color.toAnsiBackground());
        });

        // Modifiers
        EnumSet<Modifier> modifiers = style.effectiveModifiers();
        for (Modifier mod : modifiers) {
            sb.append(";").append(mod.code());
        }

        // Underline color (if supported)
        style.underlineColor().ifPresent(color -> {
            sb.append(";");
            sb.append(color.toAnsiUnderline());
        });

        sb.append("m");
        writer.print(sb.toString());
    }

    /**
     * Returns the underlying JLine terminal for advanced operations.
     *
     * @return the JLine terminal instance
     */
    public Terminal jlineTerminal() {
        return terminal;
    }
}
