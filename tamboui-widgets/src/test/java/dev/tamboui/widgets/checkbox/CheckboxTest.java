/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.widgets.checkbox;

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

class CheckboxTest {

    @Nested
    @DisplayName("Basic rendering")
    class BasicRendering {

        @Test
        @DisplayName("renders unchecked state with default symbol")
        void rendersUncheckedDefault() {
            Checkbox checkbox = Checkbox.builder().build();
            Rect area = new Rect(0, 0, 10, 1);
            Buffer buffer = Buffer.empty(area);
            CheckboxState state = new CheckboxState(false);

            checkbox.render(area, buffer, state);

            Buffer expected = Buffer.empty(area);
            expected.setString(0, 0, "[ ]", Style.EMPTY);
            BufferAssertions.assertThat(buffer).isEqualTo(expected);
        }

        @Test
        @DisplayName("renders checked state with default symbol")
        void rendersCheckedDefault() {
            Checkbox checkbox = Checkbox.builder().build();
            Rect area = new Rect(0, 0, 10, 1);
            Buffer buffer = Buffer.empty(area);
            CheckboxState state = new CheckboxState(true);

            checkbox.render(area, buffer, state);

            Buffer expected = Buffer.empty(area);
            expected.setString(0, 0, "[x]", Style.EMPTY);
            BufferAssertions.assertThat(buffer).isEqualTo(expected);
        }

        @Test
        @DisplayName("does not render in empty area")
        void doesNotRenderInEmptyArea() {
            Checkbox checkbox = Checkbox.builder().build();
            Rect emptyArea = new Rect(0, 0, 0, 0);
            Rect bufferArea = new Rect(0, 0, 10, 1);
            Buffer buffer = Buffer.empty(bufferArea);
            CheckboxState state = new CheckboxState(true);

            checkbox.render(emptyArea, buffer, state);

            // Buffer should remain empty
            BufferAssertions.assertThat(buffer).isEqualTo(Buffer.empty(bufferArea));
        }

        @Test
        @DisplayName("truncates symbol if area too narrow")
        void truncatesIfTooNarrow() {
            Checkbox checkbox = Checkbox.builder().build();
            Rect area = new Rect(0, 0, 2, 1);
            Buffer buffer = Buffer.empty(area);
            CheckboxState state = new CheckboxState(true);

            checkbox.render(area, buffer, state);

            Buffer expected = Buffer.empty(area);
            expected.setString(0, 0, "[x", Style.EMPTY);
            BufferAssertions.assertThat(buffer).isEqualTo(expected);
        }
    }

    @Nested
    @DisplayName("Custom symbols")
    class CustomSymbols {

        @Test
        @DisplayName("uses custom checked symbol")
        void usesCustomCheckedSymbol() {
            Checkbox checkbox = Checkbox.builder()
                    .checkedSymbol("[✓]")
                    .build();
            Rect area = new Rect(0, 0, 10, 1);
            Buffer buffer = Buffer.empty(area);
            CheckboxState state = new CheckboxState(true);

            checkbox.render(area, buffer, state);

            assertThat(checkbox.checkedSymbol()).isEqualTo("[✓]");
            Buffer expected = Buffer.empty(area);
            expected.setString(0, 0, "[✓]", Style.EMPTY);
            BufferAssertions.assertThat(buffer).isEqualTo(expected);
        }

        @Test
        @DisplayName("uses custom unchecked symbol")
        void usesCustomUncheckedSymbol() {
            Checkbox checkbox = Checkbox.builder()
                    .uncheckedSymbol("( )")
                    .build();
            Rect area = new Rect(0, 0, 10, 1);
            Buffer buffer = Buffer.empty(area);
            CheckboxState state = new CheckboxState(false);

            checkbox.render(area, buffer, state);

            assertThat(checkbox.uncheckedSymbol()).isEqualTo("( )");
            Buffer expected = Buffer.empty(area);
            expected.setString(0, 0, "( )", Style.EMPTY);
            BufferAssertions.assertThat(buffer).isEqualTo(expected);
        }
    }

    @Nested
    @DisplayName("Colors and styles")
    class ColorsAndStyles {

        @Test
        @DisplayName("applies checked color")
        void appliesCheckedColor() {
            Checkbox checkbox = Checkbox.builder()
                    .checkedColor(Color.GREEN)
                    .build();
            Rect area = new Rect(0, 0, 10, 1);
            Buffer buffer = Buffer.empty(area);
            CheckboxState state = new CheckboxState(true);

            checkbox.render(area, buffer, state);

            Buffer expected = Buffer.empty(area);
            expected.setString(0, 0, "[x]", Style.EMPTY.fg(Color.GREEN));
            BufferAssertions.assertThat(buffer).isEqualTo(expected);
        }

        @Test
        @DisplayName("applies unchecked color")
        void appliesUncheckedColor() {
            Checkbox checkbox = Checkbox.builder()
                    .uncheckedColor(Color.DARK_GRAY)
                    .build();
            Rect area = new Rect(0, 0, 10, 1);
            Buffer buffer = Buffer.empty(area);
            CheckboxState state = new CheckboxState(false);

            checkbox.render(area, buffer, state);

            Buffer expected = Buffer.empty(area);
            expected.setString(0, 0, "[ ]", Style.EMPTY.fg(Color.DARK_GRAY));
            BufferAssertions.assertThat(buffer).isEqualTo(expected);
        }

        @Test
        @DisplayName("applies base style modifiers")
        void appliesBaseStyleModifiers() {
            Checkbox checkbox = Checkbox.builder()
                    .style(Style.EMPTY.bold())
                    .build();
            Rect area = new Rect(0, 0, 10, 1);
            Buffer buffer = Buffer.empty(area);
            CheckboxState state = new CheckboxState(true);

            checkbox.render(area, buffer, state);

            assertThat(buffer.get(0, 0).style().addModifiers()).contains(Modifier.BOLD);
        }
    }

    @Nested
    @DisplayName("CSS property resolution")
    class CssPropertyResolution {

        @Test
        @DisplayName("resolves checked symbol from style resolver")
        void resolvesCheckedSymbolFromCss() {
            Checkbox checkbox = Checkbox.builder()
                    .styleResolver(TestStylePropertyResolver.of("checkbox-checked-symbol", "[ON]"))
                    .build();
            Rect area = new Rect(0, 0, 10, 1);
            Buffer buffer = Buffer.empty(area);
            CheckboxState state = new CheckboxState(true);

            checkbox.render(area, buffer, state);

            Buffer expected = Buffer.empty(area);
            expected.setString(0, 0, "[ON]", Style.EMPTY);
            BufferAssertions.assertThat(buffer).isEqualTo(expected);
        }

        @Test
        @DisplayName("resolves unchecked symbol from style resolver")
        void resolvesUncheckedSymbolFromCss() {
            Checkbox checkbox = Checkbox.builder()
                    .styleResolver(TestStylePropertyResolver.of("checkbox-unchecked-symbol", "[--]"))
                    .build();
            Rect area = new Rect(0, 0, 10, 1);
            Buffer buffer = Buffer.empty(area);
            CheckboxState state = new CheckboxState(false);

            checkbox.render(area, buffer, state);

            Buffer expected = Buffer.empty(area);
            expected.setString(0, 0, "[--]", Style.EMPTY);
            BufferAssertions.assertThat(buffer).isEqualTo(expected);
        }

        @Test
        @DisplayName("resolves checked color from style resolver")
        void resolvesCheckedColorFromCss() {
            Checkbox checkbox = Checkbox.builder()
                    .styleResolver(TestStylePropertyResolver.of("checkbox-checked-color", Color.CYAN))
                    .build();
            Rect area = new Rect(0, 0, 10, 1);
            Buffer buffer = Buffer.empty(area);
            CheckboxState state = new CheckboxState(true);

            checkbox.render(area, buffer, state);

            Buffer expected = Buffer.empty(area);
            expected.setString(0, 0, "[x]", Style.EMPTY.fg(Color.CYAN));
            BufferAssertions.assertThat(buffer).isEqualTo(expected);
        }

        @Test
        @DisplayName("explicit value takes precedence over CSS")
        void explicitTakesPrecedenceOverCss() {
            Checkbox checkbox = Checkbox.builder()
                    .checkedSymbol("[YES]")
                    .styleResolver(TestStylePropertyResolver.of("checkbox-checked-symbol", "[NO]"))
                    .build();
            Rect area = new Rect(0, 0, 10, 1);
            Buffer buffer = Buffer.empty(area);
            CheckboxState state = new CheckboxState(true);

            checkbox.render(area, buffer, state);

            // Explicit "[YES]" should take precedence over CSS "[NO]"
            Buffer expected = Buffer.empty(area);
            expected.setString(0, 0, "[YES]", Style.EMPTY);
            BufferAssertions.assertThat(buffer).isEqualTo(expected);
        }
    }

    @Nested
    @DisplayName("Width calculation")
    class WidthCalculation {

        @Test
        @DisplayName("width returns max of both symbols")
        void widthReturnsMaxOfBothSymbols() {
            Checkbox checkbox = Checkbox.builder()
                    .checkedSymbol("[ON]")
                    .uncheckedSymbol("[OFF]")
                    .build();

            assertThat(checkbox.width()).isEqualTo(5); // "[OFF]" is longer
        }

        @Test
        @DisplayName("default width is 3")
        void defaultWidthIs3() {
            Checkbox checkbox = Checkbox.builder().build();

            assertThat(checkbox.width()).isEqualTo(3); // "[x]" and "[ ]"
        }
    }

    @Nested
    @DisplayName("CheckboxState")
    class CheckboxStateTests {

        @Test
        @DisplayName("default state is unchecked")
        void defaultStateIsUnchecked() {
            CheckboxState state = new CheckboxState();
            assertThat(state.isChecked()).isFalse();
        }

        @Test
        @DisplayName("toggle changes state")
        void toggleChangesState() {
            CheckboxState state = new CheckboxState(false);

            boolean newValue = state.toggle();

            assertThat(newValue).isTrue();
            assertThat(state.isChecked()).isTrue();
        }

        @Test
        @DisplayName("value() is alias for isChecked()")
        void valueIsAliasForIsChecked() {
            CheckboxState state = new CheckboxState(true);
            assertThat(state.value()).isEqualTo(state.isChecked());
        }

        @Test
        @DisplayName("setValue() is alias for setChecked()")
        void setValueIsAliasForSetChecked() {
            CheckboxState state = new CheckboxState(false);
            state.setValue(true);
            assertThat(state.isChecked()).isTrue();
        }
    }
}
