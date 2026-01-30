/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.toolkit.elements;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import dev.tamboui.layout.Constraint;

import static dev.tamboui.toolkit.Toolkit.*;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for Spacer.
 */
class SpacerTest {

    @Test
    @DisplayName("preferredWidth() returns 0 for fill spacer")
    void preferredWidth_fillSpacer() {
        Spacer spacer = spacer();
        assertThat(spacer.preferredWidth()).isEqualTo(0);
    }

    @Test
    @DisplayName("preferredWidth() returns length for fixed spacer")
    void preferredWidth_fixedLength() {
        Spacer spacer = Spacer.length(4);
        assertThat(spacer.preferredWidth()).isEqualTo(4);
    }

    @Test
    @DisplayName("preferredWidth() returns length for spacer constructor")
    void preferredWidth_constructor() {
        Spacer spacer = new Spacer(10);
        assertThat(spacer.preferredWidth()).isEqualTo(10);
    }

    @Test
    @DisplayName("preferredWidth() returns 0 for percentage spacer")
    void preferredWidth_percentage() {
        Spacer spacer = spacer().percent(50);
        assertThat(spacer.preferredWidth()).isEqualTo(0);
    }

    @Test
    @DisplayName("preferredHeight() returns 0 for fill spacer")
    void preferredHeight_fillSpacer() {
        Spacer spacer = spacer();
        assertThat(spacer.preferredHeight()).isEqualTo(0);
    }

    @Test
    @DisplayName("preferredHeight() returns length for fixed spacer")
    void preferredHeight_fixedLength() {
        Spacer spacer = Spacer.length(5);
        assertThat(spacer.preferredHeight()).isEqualTo(5);
    }

    @Test
    @DisplayName("Row with fixed spacer includes spacer in preferredWidth")
    void rowWithFixedSpacer() {
        Row row = row(text("Left"), // 4
                Spacer.length(4), // 4
                text("Right") // 5
        );
        // 4 + 4 + 5 = 13
        assertThat(row.preferredWidth()).isEqualTo(13);
    }

    @Test
    @DisplayName("Row with fill spacer does not include spacer in preferredWidth")
    void rowWithFillSpacer() {
        Row row = row(text("Left"), // 4
                spacer(), // 0
                text("Right") // 5
        );
        // 4 + 0 + 5 = 9
        assertThat(row.preferredWidth()).isEqualTo(9);
    }

    @Test
    @DisplayName("constraint() returns fill by default")
    void constraint_default() {
        Spacer spacer = spacer();
        Constraint c = spacer.constraint();
        assertThat(c).isInstanceOf(Constraint.Fill.class);
    }

    @Test
    @DisplayName("constraint() returns length for fixed spacer")
    void constraint_fixedLength() {
        Spacer spacer = Spacer.length(7);
        Constraint c = spacer.constraint();
        assertThat(c).isInstanceOf(Constraint.Length.class);
    }

    @Test
    @DisplayName("constraint() returns percentage after percent()")
    void constraint_percentage() {
        Spacer spacer = spacer().percent(25);
        Constraint c = spacer.constraint();
        assertThat(c).isInstanceOf(Constraint.Percentage.class);
    }
}
