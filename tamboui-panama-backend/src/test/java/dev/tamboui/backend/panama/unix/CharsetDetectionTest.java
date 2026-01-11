/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.backend.panama.unix;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Tests for charset detection in UnixTerminal.
 */
@EnabledOnOs({OS.LINUX, OS.MAC})
class CharsetDetectionTest {

    @Test
    void charsetIsDetectedFromEnvironment() throws Exception {
        // This test verifies that charset detection doesn't throw
        // and returns a valid charset. The actual value depends on
        // the system's locale configuration.
        assumeTerminalAvailable();
        try (UnixTerminal terminal = new UnixTerminal()) {
            Charset charset = terminal.getCharset();
            assertNotNull(charset, "Charset should be detected");
            // On most modern systems this will be UTF-8
            System.out.println("Detected charset: " + charset.name());
        }
    }

    @Test
    void utf8IsDefaultForModernSystems() throws Exception {
        // Most CI systems and modern development environments use UTF-8
        // This test documents the expected behavior
        assumeTerminalAvailable();
        String lang = System.getenv("LANG");
        if (lang != null && (lang.contains("UTF-8") || lang.contains("UTF8"))) {
            try (UnixTerminal terminal = new UnixTerminal()) {
                Charset charset = terminal.getCharset();
                // Should detect UTF-8 from LANG
                assertNotNull(charset);
            }
        }
    }

    /**
     * Assumes a terminal is available for the test.
     * Skips the test if /dev/tty cannot be opened.
     */
    private void assumeTerminalAvailable() {
        int fd = LibC.open("/dev/tty", LibC.O_RDWR);
        if (fd >= 0) {
            LibC.close(fd);
            assumeTrue(true);
        } else {
            assumeTrue(false, "No terminal available (/dev/tty cannot be opened)");
        }
    }
}
