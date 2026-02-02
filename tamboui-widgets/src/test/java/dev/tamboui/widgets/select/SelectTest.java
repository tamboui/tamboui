/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.widgets.select;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import dev.tamboui.assertj.BufferAssertions;
import dev.tamboui.buffer.Buffer;
import dev.tamboui.layout.Rect;
import dev.tamboui.style.Color;
import dev.tamboui.style.Modifier;
import dev.tamboui.style.Style;
import dev.tamboui.style.TestStylePropertyResolver;

import static org.assertj.core.api.Assertions.assertThat;

class SelectTest {

    @Nested
    @DisplayName("Basic rendering")
    class BasicRendering {

        @Test
        @DisplayName("renders selected value with default indicators")
        void rendersSelectedValueDefault() {
            Select select = Select.builder().build();
            Rect area = new Rect(0, 0, 20, 1);
            Buffer buffer = Buffer.empty(area);
            SelectState state = new SelectState("Option A", "Option B", "Option C");

            select.render(area, buffer, state);

            Buffer expected = Buffer.empty(area);
            Style indicatorStyle = Style.EMPTY.fg(Color.DARK_GRAY);
            expected.setString(0, 0, "< ", indicatorStyle);
            expected.setString(2, 0, "Option A", Style.EMPTY);
            expected.setString(10, 0, " >", indicatorStyle);
            BufferAssertions.assertThat(buffer).isEqualTo(expected);
        }

        @Test
        @DisplayName("renders second option when selected")
        void rendersSecondOption() {
            Select select = Select.builder().build();
            Rect area = new Rect(0, 0, 20, 1);
            Buffer buffer = Buffer.empty(area);
            SelectState state = new SelectState("Option A", "Option B", "Option C");
            state.selectIndex(1);

            select.render(area, buffer, state);

            Buffer expected = Buffer.empty(area);
            Style indicatorStyle = Style.EMPTY.fg(Color.DARK_GRAY);
            expected.setString(0, 0, "< ", indicatorStyle);
            expected.setString(2, 0, "Option B", Style.EMPTY);
            expected.setString(10, 0, " >", indicatorStyle);
            BufferAssertions.assertThat(buffer).isEqualTo(expected);
        }

        @Test
        @DisplayName("does not render in empty area")
        void doesNotRenderInEmptyArea() {
            Select select = Select.builder().build();
            Rect emptyArea = new Rect(0, 0, 0, 0);
            Rect bufferArea = new Rect(0, 0, 20, 1);
            Buffer buffer = Buffer.empty(bufferArea);
            SelectState state = new SelectState("Option A");

            select.render(emptyArea, buffer, state);

            BufferAssertions.assertThat(buffer).isEqualTo(Buffer.empty(bufferArea));
        }

        @Test
        @DisplayName("truncates if area too narrow")
        void truncatesIfTooNarrow() {
            Select select = Select.builder().build();
            Rect area = new Rect(0, 0, 8, 1);
            Buffer buffer = Buffer.empty(area);
            SelectState state = new SelectState("Long Option Name");

            select.render(area, buffer, state);

            Buffer expected = Buffer.empty(area);
            Style indicatorStyle = Style.EMPTY.fg(Color.DARK_GRAY);
            expected.setString(0, 0, "< ", indicatorStyle);
            expected.setString(2, 0, "Long", Style.EMPTY);
            expected.setString(6, 0, " >", indicatorStyle);
            BufferAssertions.assertThat(buffer).isEqualTo(expected);
        }

        @Test
        @DisplayName("handles empty options list")
        void handlesEmptyOptions() {
            Select select = Select.builder().build();
            Rect area = new Rect(0, 0, 20, 1);
            Buffer buffer = Buffer.empty(area);
            SelectState state = new SelectState();

            select.render(area, buffer, state);

            Buffer expected = Buffer.empty(area);
            Style indicatorStyle = Style.EMPTY.fg(Color.DARK_GRAY);
            expected.setString(0, 0, "< ", indicatorStyle);
            expected.setString(2, 0, " >", indicatorStyle);
            BufferAssertions.assertThat(buffer).isEqualTo(expected);
        }
    }

    @Nested
    @DisplayName("Custom indicators")
    class CustomIndicators {

        @Test
        @DisplayName("uses custom left indicator")
        void usesCustomLeftIndicator() {
            Select select = Select.builder()
                    .leftIndicator("◄ ")
                    .build();
            Rect area = new Rect(0, 0, 20, 1);
            Buffer buffer = Buffer.empty(area);
            SelectState state = new SelectState("Test");

            select.render(area, buffer, state);

            assertThat(select.leftIndicator()).isEqualTo("◄ ");
            Buffer expected = Buffer.empty(area);
            Style indicatorStyle = Style.EMPTY.fg(Color.DARK_GRAY);
            expected.setString(0, 0, "◄ ", indicatorStyle);
            expected.setString(2, 0, "Test", Style.EMPTY);
            expected.setString(6, 0, " >", indicatorStyle);
            BufferAssertions.assertThat(buffer).isEqualTo(expected);
        }

        @Test
        @DisplayName("uses custom right indicator")
        void usesCustomRightIndicator() {
            Select select = Select.builder()
                    .rightIndicator(" ►")
                    .build();
            Rect area = new Rect(0, 0, 20, 1);
            Buffer buffer = Buffer.empty(area);
            SelectState state = new SelectState("Test");

            select.render(area, buffer, state);

            assertThat(select.rightIndicator()).isEqualTo(" ►");
            Buffer expected = Buffer.empty(area);
            Style indicatorStyle = Style.EMPTY.fg(Color.DARK_GRAY);
            expected.setString(0, 0, "< ", indicatorStyle);
            expected.setString(2, 0, "Test", Style.EMPTY);
            expected.setString(6, 0, " ►", indicatorStyle);
            BufferAssertions.assertThat(buffer).isEqualTo(expected);
        }

        @Test
        @DisplayName("uses empty indicators")
        void usesEmptyIndicators() {
            Select select = Select.builder()
                    .leftIndicator("")
                    .rightIndicator("")
                    .build();
            Rect area = new Rect(0, 0, 20, 1);
            Buffer buffer = Buffer.empty(area);
            SelectState state = new SelectState("Test");

            select.render(area, buffer, state);

            Buffer expected = Buffer.empty(area);
            expected.setString(0, 0, "Test", Style.EMPTY);
            BufferAssertions.assertThat(buffer).isEqualTo(expected);
        }
    }

    @Nested
    @DisplayName("Colors and styles")
    class ColorsAndStyles {

        @Test
        @DisplayName("applies selected color")
        void appliesSelectedColor() {
            Select select = Select.builder()
                    .selectedColor(Color.CYAN)
                    .build();
            Rect area = new Rect(0, 0, 20, 1);
            Buffer buffer = Buffer.empty(area);
            SelectState state = new SelectState("Test");

            select.render(area, buffer, state);

            Buffer expected = Buffer.empty(area);
            Style indicatorStyle = Style.EMPTY.fg(Color.DARK_GRAY);
            expected.setString(0, 0, "< ", indicatorStyle);
            expected.setString(2, 0, "Test", Style.EMPTY.fg(Color.CYAN));
            expected.setString(6, 0, " >", indicatorStyle);
            BufferAssertions.assertThat(buffer).isEqualTo(expected);
        }

        @Test
        @DisplayName("applies indicator color")
        void appliesIndicatorColor() {
            Select select = Select.builder()
                    .indicatorColor(Color.YELLOW)
                    .build();
            Rect area = new Rect(0, 0, 20, 1);
            Buffer buffer = Buffer.empty(area);
            SelectState state = new SelectState("Test");

            select.render(area, buffer, state);

            Buffer expected = Buffer.empty(area);
            Style indicatorStyle = Style.EMPTY.fg(Color.YELLOW);
            expected.setString(0, 0, "< ", indicatorStyle);
            expected.setString(2, 0, "Test", Style.EMPTY);
            expected.setString(6, 0, " >", indicatorStyle);
            BufferAssertions.assertThat(buffer).isEqualTo(expected);
        }

        @Test
        @DisplayName("applies base style modifiers")
        void appliesBaseStyleModifiers() {
            Select select = Select.builder()
                    .style(Style.EMPTY.bold())
                    .build();
            Rect area = new Rect(0, 0, 20, 1);
            Buffer buffer = Buffer.empty(area);
            SelectState state = new SelectState("Test");

            select.render(area, buffer, state);

            assertThat(buffer.get(2, 0).style().addModifiers()).contains(Modifier.BOLD);
        }
    }

    @Nested
    @DisplayName("CSS property resolution")
    class CssPropertyResolution {

        @Test
        @DisplayName("resolves left indicator from style resolver")
        void resolvesLeftIndicatorFromCss() {
            Select select = Select.builder()
                    .styleResolver(TestStylePropertyResolver.of("select-left-indicator", "["))
                    .build();
            Rect area = new Rect(0, 0, 20, 1);
            Buffer buffer = Buffer.empty(area);
            SelectState state = new SelectState("Test");

            select.render(area, buffer, state);

            Buffer expected = Buffer.empty(area);
            Style indicatorStyle = Style.EMPTY.fg(Color.DARK_GRAY);
            expected.setString(0, 0, "[", indicatorStyle);
            expected.setString(1, 0, "Test", Style.EMPTY);
            expected.setString(5, 0, " >", indicatorStyle);
            BufferAssertions.assertThat(buffer).isEqualTo(expected);
        }

        @Test
        @DisplayName("resolves right indicator from style resolver")
        void resolvesRightIndicatorFromCss() {
            Select select = Select.builder()
                    .styleResolver(TestStylePropertyResolver.of("select-right-indicator", "]"))
                    .build();
            Rect area = new Rect(0, 0, 20, 1);
            Buffer buffer = Buffer.empty(area);
            SelectState state = new SelectState("Test");

            select.render(area, buffer, state);

            Buffer expected = Buffer.empty(area);
            Style indicatorStyle = Style.EMPTY.fg(Color.DARK_GRAY);
            expected.setString(0, 0, "< ", indicatorStyle);
            expected.setString(2, 0, "Test", Style.EMPTY);
            expected.setString(6, 0, "]", indicatorStyle);
            BufferAssertions.assertThat(buffer).isEqualTo(expected);
        }

        @Test
        @DisplayName("resolves selected color from style resolver")
        void resolvesSelectedColorFromCss() {
            Select select = Select.builder()
                    .styleResolver(TestStylePropertyResolver.of("select-selected-color", Color.MAGENTA))
                    .build();
            Rect area = new Rect(0, 0, 20, 1);
            Buffer buffer = Buffer.empty(area);
            SelectState state = new SelectState("Test");

            select.render(area, buffer, state);

            Buffer expected = Buffer.empty(area);
            Style indicatorStyle = Style.EMPTY.fg(Color.DARK_GRAY);
            expected.setString(0, 0, "< ", indicatorStyle);
            expected.setString(2, 0, "Test", Style.EMPTY.fg(Color.MAGENTA));
            expected.setString(6, 0, " >", indicatorStyle);
            BufferAssertions.assertThat(buffer).isEqualTo(expected);
        }

        @Test
        @DisplayName("explicit value takes precedence over CSS")
        void explicitTakesPrecedenceOverCss() {
            Select select = Select.builder()
                    .leftIndicator("<<")
                    .styleResolver(TestStylePropertyResolver.of("select-left-indicator", ">>"))
                    .build();
            Rect area = new Rect(0, 0, 20, 1);
            Buffer buffer = Buffer.empty(area);
            SelectState state = new SelectState("Test");

            select.render(area, buffer, state);

            Buffer expected = Buffer.empty(area);
            Style indicatorStyle = Style.EMPTY.fg(Color.DARK_GRAY);
            expected.setString(0, 0, "<<", indicatorStyle);
            expected.setString(2, 0, "Test", Style.EMPTY);
            expected.setString(6, 0, " >", indicatorStyle);
            BufferAssertions.assertThat(buffer).isEqualTo(expected);
        }
    }

    @Nested
    @DisplayName("Width calculation")
    class WidthCalculation {

        @Test
        @DisplayName("width includes indicators and value")
        void widthIncludesIndicatorsAndValue() {
            Select select = Select.builder().build();
            SelectState state = new SelectState("Test");

            // "< Test >" = 2 + 4 + 2 = 8
            assertThat(select.width(state)).isEqualTo(8);
        }

        @Test
        @DisplayName("minWidth returns indicators only")
        void minWidthReturnsIndicatorsOnly() {
            Select select = Select.builder().build();

            // "< " + " >" = 2 + 2 = 4
            assertThat(select.minWidth()).isEqualTo(4);
        }

        @Test
        @DisplayName("custom indicators affect width")
        void customIndicatorsAffectWidth() {
            Select select = Select.builder()
                    .leftIndicator("◄◄ ")
                    .rightIndicator(" ►►")
                    .build();
            SelectState state = new SelectState("ABC");

            // "◄◄ ABC ►►" = 3 + 3 + 3 = 9
            assertThat(select.width(state)).isEqualTo(9);
        }
    }

    @Nested
    @DisplayName("SelectState")
    class SelectStateTests {

        @Test
        @DisplayName("default selection is first option")
        void defaultSelectionIsFirst() {
            SelectState state = new SelectState("A", "B", "C");
            assertThat(state.selectedIndex()).isEqualTo(0);
            assertThat(state.selectedValue()).isEqualTo("A");
        }

        @Test
        @DisplayName("selectNext cycles through options")
        void selectNextCycles() {
            SelectState state = new SelectState("A", "B", "C");

            state.selectNext();
            assertThat(state.selectedValue()).isEqualTo("B");

            state.selectNext();
            assertThat(state.selectedValue()).isEqualTo("C");

            state.selectNext();
            assertThat(state.selectedValue()).isEqualTo("A"); // wraps
        }

        @Test
        @DisplayName("selectPrevious cycles through options")
        void selectPreviousCycles() {
            SelectState state = new SelectState("A", "B", "C");

            state.selectPrevious();
            assertThat(state.selectedValue()).isEqualTo("C"); // wraps

            state.selectPrevious();
            assertThat(state.selectedValue()).isEqualTo("B");
        }

        @Test
        @DisplayName("value() is alias for selectedValue()")
        void valueIsAliasForSelectedValue() {
            SelectState state = new SelectState("Test");
            assertThat(state.value()).isEqualTo(state.selectedValue());
        }

        @Test
        @DisplayName("empty state returns empty string")
        void emptyStateReturnsEmptyString() {
            SelectState state = new SelectState();
            assertThat(state.selectedValue()).isEqualTo("");
            assertThat(state.selectedIndex()).isEqualTo(-1);
        }
    }
}
