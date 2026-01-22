/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.pygments;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static dev.tamboui.pygments.Pygments.pygments;

class PygmentsTest {

    @Test
    void testPygments() {
        Pygments.Result result = pygments().highlightWithInfo(
            "test.java",
            "print('Hello, World!')",
            Duration.ofSeconds(1)
        );
        assertThat(result).isNotNull();
    }
}

