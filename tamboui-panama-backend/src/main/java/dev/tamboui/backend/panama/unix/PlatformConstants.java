/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.backend.panama.unix;

import java.lang.foreign.MemoryLayout;
import java.lang.foreign.ValueLayout;

/**
 * Platform-specific constants for Unix terminal operations.
 * <p>
 * This class provides the correct values for termios flags, control character
 * indices, and structure layouts that differ between Linux and macOS.
 */
public final class PlatformConstants {

    private static final boolean IS_WINDOWS;
    private static final boolean IS_MACOS;

    static {
        var os = System.getProperty("os.name", "").toLowerCase();
        IS_WINDOWS = os.contains("windows");
        IS_MACOS = os.contains("mac") || os.contains("darwin");
    }

    private PlatformConstants() {
    }

    /**
     * Returns true if running on Windows.
     *
     * @return true if Windows, false otherwise
     */
    public static boolean isWindows() {
        return IS_WINDOWS;
    }

    /**
     * Returns true if running on macOS.
     *
     * @return true if macOS, false otherwise
     */
    public static boolean isMacOS() {
        return IS_MACOS;
    }

    /**
     * Returns true if running on Linux.
     *
     * @return true if Linux, false otherwise
     */
    public static boolean isLinux() {
        return !IS_WINDOWS && !IS_MACOS;
    }

    /**
     * Returns true if running on a Unix-like system (Linux or macOS).
     *
     * @return true if Unix-like, false otherwise
     */
    public static boolean isUnix() {
        return !IS_WINDOWS;
    }

    /** ioctl request code to get terminal window size. */
    public static final long TIOCGWINSZ = IS_MACOS ? 0x40087468L : 0x5413L;

    /** Local flag: enable echo of input characters. */
    public static final int ECHO = 0x00000008;  // Same on both
    /** Local flag: enable canonical (line-by-line) input mode. */
    public static final int ICANON = IS_MACOS ? 0x00000100 : 0x00000002;
    /** Local flag: enable signal generation for INTR, QUIT, SUSP characters. */
    public static final int ISIG = IS_MACOS ? 0x00000080 : 0x00000001;
    /** Local flag: enable implementation-defined input processing. */
    public static final int IEXTEN = IS_MACOS ? 0x00000400 : 0x00008000;

    /** Input flag: enable XON/XOFF flow control on output. */
    public static final int IXON = IS_MACOS ? 0x00000200 : 0x00000400;
    /** Input flag: translate carriage return to newline on input. */
    public static final int ICRNL = 0x00000100;  // Same on both
    /** Input flag: signal interrupt on break. */
    public static final int BRKINT = 0x00000002;  // Same on both
    /** Input flag: enable input parity check. */
    public static final int INPCK = 0x00000010;  // Same on both
    /** Input flag: strip eighth bit off input characters. */
    public static final int ISTRIP = 0x00000020;  // Same on both

    /** Output flag: enable implementation-defined output processing. */
    public static final int OPOST = 0x00000001;  // Same on both

    /** Control flag: set character size to 8 bits. */
    public static final int CS8 = IS_MACOS ? 0x00000300 : 0x00000030;

    /** Control character index for minimum number of bytes for non-canonical read. */
    public static final int VMIN = IS_MACOS ? 16 : 6;
    /** Control character index for timeout in deciseconds for non-canonical read. */
    public static final int VTIME = IS_MACOS ? 17 : 5;

    /** Signal number for terminal window size change. */
    public static final int SIGWINCH = 28;

    /**
     * Termios structure layout.
     * <p>
     * Linux layout:
     * - c_iflag (4 bytes)
     * - c_oflag (4 bytes)
     * - c_cflag (4 bytes)
     * - c_lflag (4 bytes)
     * - c_line (1 byte)
     * - c_cc[32] (32 bytes)
     * - padding (3 bytes)
     * - c_ispeed (4 bytes)
     * - c_ospeed (4 bytes)
     * <p>
     * macOS layout:
     * - c_iflag (8 bytes - unsigned long)
     * - c_oflag (8 bytes)
     * - c_cflag (8 bytes)
     * - c_lflag (8 bytes)
     * - c_cc[20] (20 bytes)
     * - padding (4 bytes)
     * - c_ispeed (8 bytes)
     * - c_ospeed (8 bytes)
     */
    public static final MemoryLayout TERMIOS_LAYOUT = IS_MACOS
            ? MemoryLayout.structLayout(
                    ValueLayout.JAVA_LONG.withName("c_iflag"),
                    ValueLayout.JAVA_LONG.withName("c_oflag"),
                    ValueLayout.JAVA_LONG.withName("c_cflag"),
                    ValueLayout.JAVA_LONG.withName("c_lflag"),
                    MemoryLayout.sequenceLayout(20, ValueLayout.JAVA_BYTE).withName("c_cc"),
                    MemoryLayout.paddingLayout(4),
                    ValueLayout.JAVA_LONG.withName("c_ispeed"),
                    ValueLayout.JAVA_LONG.withName("c_ospeed")
            )
            : MemoryLayout.structLayout(
                    ValueLayout.JAVA_INT.withName("c_iflag"),
                    ValueLayout.JAVA_INT.withName("c_oflag"),
                    ValueLayout.JAVA_INT.withName("c_cflag"),
                    ValueLayout.JAVA_INT.withName("c_lflag"),
                    ValueLayout.JAVA_BYTE.withName("c_line"),
                    MemoryLayout.sequenceLayout(32, ValueLayout.JAVA_BYTE).withName("c_cc"),
                    MemoryLayout.paddingLayout(3),
                    ValueLayout.JAVA_INT.withName("c_ispeed"),
                    ValueLayout.JAVA_INT.withName("c_ospeed")
            );

    /**
     * Offset to the c_cc array in the termios structure.
     */
    public static final long TERMIOS_CC_OFFSET = IS_MACOS ? 32L : 17L;

    /**
     * Value layout for termios flags (int on Linux, long on macOS).
     */
    public static final ValueLayout TERMIOS_FLAG_LAYOUT = IS_MACOS
            ? ValueLayout.JAVA_LONG
            : ValueLayout.JAVA_INT;
}
