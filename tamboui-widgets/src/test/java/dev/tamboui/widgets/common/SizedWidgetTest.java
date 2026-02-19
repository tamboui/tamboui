/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.widgets.common;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import dev.tamboui.widget.Widget;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for {@link SizedWidget}.
 */
class SizedWidgetTest {

    private static final Widget DUMMY_WIDGET = (area, buffer) -> {};

    @Test
    @DisplayName("of(widget) creates widget with default dimensions")
    void ofWidgetDefaults() {
        SizedWidget sized = SizedWidget.of(DUMMY_WIDGET);

        assertThat(sized.widget()).isSameAs(DUMMY_WIDGET);
        assertThat(sized.width()).isEqualTo(SizedWidget.DEFAULT);
        assertThat(sized.height()).isEqualTo(SizedWidget.DEFAULT);
        assertThat(sized.hasExplicitWidth()).isFalse();
        assertThat(sized.hasExplicitHeight()).isFalse();
    }

    @Test
    @DisplayName("ofHeight creates widget with explicit height")
    void ofHeight() {
        SizedWidget sized = SizedWidget.ofHeight(DUMMY_WIDGET, 3);

        assertThat(sized.widget()).isSameAs(DUMMY_WIDGET);
        assertThat(sized.width()).isEqualTo(SizedWidget.DEFAULT);
        assertThat(sized.height()).isEqualTo(3);
        assertThat(sized.hasExplicitWidth()).isFalse();
        assertThat(sized.hasExplicitHeight()).isTrue();
    }

    @Test
    @DisplayName("ofWidth creates widget with explicit width")
    void ofWidth() {
        SizedWidget sized = SizedWidget.ofWidth(DUMMY_WIDGET, 20);

        assertThat(sized.widget()).isSameAs(DUMMY_WIDGET);
        assertThat(sized.width()).isEqualTo(20);
        assertThat(sized.height()).isEqualTo(SizedWidget.DEFAULT);
        assertThat(sized.hasExplicitWidth()).isTrue();
        assertThat(sized.hasExplicitHeight()).isFalse();
    }

    @Test
    @DisplayName("of(widget, width, height) creates widget with both dimensions")
    void ofWidgetWidthHeight() {
        SizedWidget sized = SizedWidget.of(DUMMY_WIDGET, 15, 2);

        assertThat(sized.widget()).isSameAs(DUMMY_WIDGET);
        assertThat(sized.width()).isEqualTo(15);
        assertThat(sized.height()).isEqualTo(2);
        assertThat(sized.hasExplicitWidth()).isTrue();
        assertThat(sized.hasExplicitHeight()).isTrue();
    }

    @Test
    @DisplayName("heightOr returns height when explicit")
    void heightOrExplicit() {
        SizedWidget sized = SizedWidget.ofHeight(DUMMY_WIDGET, 5);
        assertThat(sized.heightOr(1)).isEqualTo(5);
    }

    @Test
    @DisplayName("heightOr returns default when not explicit")
    void heightOrDefault() {
        SizedWidget sized = SizedWidget.of(DUMMY_WIDGET);
        assertThat(sized.heightOr(1)).isEqualTo(1);
        assertThat(sized.heightOr(10)).isEqualTo(10);
    }

    @Test
    @DisplayName("widthOr returns width when explicit")
    void widthOrExplicit() {
        SizedWidget sized = SizedWidget.ofWidth(DUMMY_WIDGET, 25);
        assertThat(sized.widthOr(10)).isEqualTo(25);
    }

    @Test
    @DisplayName("widthOr returns default when not explicit")
    void widthOrDefault() {
        SizedWidget sized = SizedWidget.of(DUMMY_WIDGET);
        assertThat(sized.widthOr(10)).isEqualTo(10);
    }

    @Test
    @DisplayName("null widget throws NullPointerException")
    void nullWidgetThrows() {
        assertThatThrownBy(() -> SizedWidget.of(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("widget");
    }

    @Test
    @DisplayName("DEFAULT constant is -1")
    void defaultConstant() {
        assertThat(SizedWidget.DEFAULT).isEqualTo(-1);
    }
}
