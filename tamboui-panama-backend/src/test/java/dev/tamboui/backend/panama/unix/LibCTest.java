/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.backend.panama.unix;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for LibC bindings.
 */
@EnabledOnOs({OS.LINUX, OS.MAC})
class LibCTest {

    @Test
    void fileDescriptorConstantsAreDefined() {
        assertEquals(0, LibC.STDIN_FILENO);
        assertEquals(1, LibC.STDOUT_FILENO);
    }

    @Test
    void termiosConstantsAreDefined() {
        assertEquals(0, LibC.TCSANOW);
        assertEquals(1, LibC.TCSADRAIN);
        assertEquals(2, LibC.TCSAFLUSH);
    }

    @Test
    void pollConstantsAreDefined() {
        assertEquals((short) 0x0001, LibC.POLLIN);
        assertEquals((short) 0x0002, LibC.POLLPRI);
        assertEquals((short) 0x0008, LibC.POLLERR);
        assertEquals((short) 0x0010, LibC.POLLHUP);
        assertEquals((short) 0x0020, LibC.POLLNVAL);
    }

    @Test
    void tiocgwinszIsDetected() {
        // TIOCGWINSZ should be non-zero on Unix systems
        assertTrue(LibC.TIOCGWINSZ != 0, "TIOCGWINSZ should be detected");
    }

    @Test
    void termiosLayoutHasExpectedSize() {
        // termios struct should be at least 60 bytes on Linux
        assertTrue(LibC.TERMIOS_LAYOUT.byteSize() >= 44,
                "termios layout should have reasonable size");
    }

    @Test
    void winsizeLayoutHasExpectedSize() {
        // winsize struct is exactly 8 bytes (4 shorts)
        assertEquals(8, LibC.WINSIZE_LAYOUT.byteSize(),
                "winsize layout should be 8 bytes");
    }

    @Test
    void pollfdLayoutHasExpectedSize() {
        // pollfd struct is exactly 8 bytes (int + 2 shorts)
        assertEquals(8, LibC.POLLFD_LAYOUT.byteSize(),
                "pollfd layout should be 8 bytes");
    }

    @Test
    void canAllocateTermios() {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment termios = LibC.allocateTermios(arena);
            assertNotNull(termios);
            assertEquals(LibC.TERMIOS_LAYOUT.byteSize(), termios.byteSize());
        }
    }

    @Test
    void canAllocateWinsize() {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment winsize = LibC.allocateWinsize(arena);
            assertNotNull(winsize);
            assertEquals(8, winsize.byteSize());
        }
    }

    @Test
    void canAllocatePollfd() {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment pollfd = LibC.allocatePollfd(arena);
            assertNotNull(pollfd);
            assertEquals(8, pollfd.byteSize());
        }
    }

    @Test
    void isattyWorksForStdin() {
        // isatty should return 0 or 1 without throwing
        int result = LibC.isatty(LibC.STDIN_FILENO);
        assertTrue(result == 0 || result == 1,
                "isatty should return 0 or 1");
    }

    @Test
    void ioctlCanBeCalledWithoutCrashing() {
        // ioctl should not crash, even if it returns an error
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment winsize = LibC.allocateWinsize(arena);
            int result = LibC.ioctl(LibC.STDOUT_FILENO, LibC.TIOCGWINSZ, winsize);
            // Result can be 0 (success) or -1 (not a tty)
            assertTrue(result == 0 || result == -1,
                    "ioctl should return 0 or -1, got: " + result);
            if (result == -1) {
                // errno should be set
                int errno = LibC.getLastErrno();
                assertTrue(errno > 0, "errno should be set on failure");
            }
        }
    }
}
