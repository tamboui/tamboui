/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.backend.panama.unix;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for platform detection in PlatformConstants.
 */
class PlatformConstantsTest {

    @Test
    void platformDetectionIsConsistent() {
        // Only one of these should be true
        var isWindows = PlatformConstants.isWindows();
        var isMacOS = PlatformConstants.isMacOS();
        var isLinux = PlatformConstants.isLinux();

        // Exactly one platform should be detected
        var platformCount = (isWindows ? 1 : 0) + (isMacOS ? 1 : 0) + (isLinux ? 1 : 0);
        assertTrue(platformCount == 1, "Exactly one platform should be detected");

        // isUnix should be true if not Windows
        if (isWindows) {
            assertFalse(PlatformConstants.isUnix());
        } else {
            assertTrue(PlatformConstants.isUnix());
        }
    }

    @Test
    void termiosLayoutIsNotNull() {
        assertNotNull(PlatformConstants.TERMIOS_LAYOUT);
    }

    @Test
    void termiosCcOffsetIsPositive() {
        assertTrue(PlatformConstants.TERMIOS_CC_OFFSET > 0);
    }

    @Test
    void termiosFlagLayoutIsNotNull() {
        assertNotNull(PlatformConstants.TERMIOS_FLAG_LAYOUT);
    }

    @Test
    void currentPlatformIsDetected() {
        var osName = System.getProperty("os.name", "").toLowerCase();

        if (osName.contains("windows")) {
            assertTrue(PlatformConstants.isWindows());
            assertFalse(PlatformConstants.isUnix());
        } else if (osName.contains("mac") || osName.contains("darwin")) {
            assertTrue(PlatformConstants.isMacOS());
            assertTrue(PlatformConstants.isUnix());
        } else {
            assertTrue(PlatformConstants.isLinux());
            assertTrue(PlatformConstants.isUnix());
        }
    }
}
