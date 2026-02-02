/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.widgets.toggle;

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

class ToggleTest {

    @Nested
    @DisplayName("Single symbol mode (default)")
    class SingleSymbolMode {

        @Test
        @DisplayName("renders off state with default symbol")
        void rendersOffDefault() {
            Toggle toggle = Toggle.builder().build();
            Rect area = new Rect(0, 0, 10, 1);
            Buffer buffer = Buffer.empty(area);
            ToggleState state = new ToggleState(false);

            toggle.render(area, buffer, state);

            Buffer expected = Buffer.empty(area);
            expected.setString(0, 0, "[OFF]", Style.EMPTY);
            BufferAssertions.assertThat(buffer).isEqualTo(expected);
        }

        @Test
        @DisplayName("renders on state with default symbol and green color")
        void rendersOnDefault() {
            Toggle toggle = Toggle.builder().build();
            Rect area = new Rect(0, 0, 10, 1);
            Buffer buffer = Buffer.empty(area);
            ToggleState state = new ToggleState(true);

            toggle.render(area, buffer, state);

            Buffer expected = Buffer.empty(area);
            expected.setString(0, 0, "[ON ]", Style.EMPTY.fg(Color.GREEN));
            BufferAssertions.assertThat(buffer).isEqualTo(expected);
        }

        @Test
        @DisplayName("does not render in empty area")
        void doesNotRenderInEmptyArea() {
            Toggle toggle = Toggle.builder().build();
            Rect emptyArea = new Rect(0, 0, 0, 0);
            Rect bufferArea = new Rect(0, 0, 10, 1);
            Buffer buffer = Buffer.empty(bufferArea);
            ToggleState state = new ToggleState(true);

            toggle.render(emptyArea, buffer, state);

            BufferAssertions.assertThat(buffer).isEqualTo(Buffer.empty(bufferArea));
        }

        @Test
        @DisplayName("truncates symbol if area too narrow")
        void truncatesIfTooNarrow() {
            Toggle toggle = Toggle.builder().build();
            Rect area = new Rect(0, 0, 3, 1);
            Buffer buffer = Buffer.empty(area);
            ToggleState state = new ToggleState(true);

            toggle.render(area, buffer, state);

            Buffer expected = Buffer.empty(area);
            expected.setString(0, 0, "[ON", Style.EMPTY.fg(Color.GREEN));
            BufferAssertions.assertThat(buffer).isEqualTo(expected);
        }

        @Test
        @DisplayName("uses custom on symbol")
        void usesCustomOnSymbol() {
            Toggle toggle = Toggle.builder()
                    .onSymbol("◉ YES")
                    .build();
            Rect area = new Rect(0, 0, 10, 1);
            Buffer buffer = Buffer.empty(area);
            ToggleState state = new ToggleState(true);

            toggle.render(area, buffer, state);

            assertThat(toggle.onSymbol()).isEqualTo("◉ YES");
            Buffer expected = Buffer.empty(area);
            expected.setString(0, 0, "◉ YES", Style.EMPTY.fg(Color.GREEN));
            BufferAssertions.assertThat(buffer).isEqualTo(expected);
        }

        @Test
        @DisplayName("uses custom off symbol")
        void usesCustomOffSymbol() {
            Toggle toggle = Toggle.builder()
                    .offSymbol("○ NO ")
                    .build();
            Rect area = new Rect(0, 0, 10, 1);
            Buffer buffer = Buffer.empty(area);
            ToggleState state = new ToggleState(false);

            toggle.render(area, buffer, state);

            assertThat(toggle.offSymbol()).isEqualTo("○ NO ");
            Buffer expected = Buffer.empty(area);
            expected.setString(0, 0, "○ NO ", Style.EMPTY);
            BufferAssertions.assertThat(buffer).isEqualTo(expected);
        }

        @Test
        @DisplayName("applies custom on color")
        void appliesCustomOnColor() {
            Toggle toggle = Toggle.builder()
                    .onColor(Color.CYAN)
                    .build();
            Rect area = new Rect(0, 0, 10, 1);
            Buffer buffer = Buffer.empty(area);
            ToggleState state = new ToggleState(true);

            toggle.render(area, buffer, state);

            Buffer expected = Buffer.empty(area);
            expected.setString(0, 0, "[ON ]", Style.EMPTY.fg(Color.CYAN));
            BufferAssertions.assertThat(buffer).isEqualTo(expected);
        }

        @Test
        @DisplayName("applies off color")
        void appliesOffColor() {
            Toggle toggle = Toggle.builder()
                    .offColor(Color.RED)
                    .build();
            Rect area = new Rect(0, 0, 10, 1);
            Buffer buffer = Buffer.empty(area);
            ToggleState state = new ToggleState(false);

            toggle.render(area, buffer, state);

            Buffer expected = Buffer.empty(area);
            expected.setString(0, 0, "[OFF]", Style.EMPTY.fg(Color.RED));
            BufferAssertions.assertThat(buffer).isEqualTo(expected);
        }

        @Test
        @DisplayName("applies base style modifiers")
        void appliesBaseStyleModifiers() {
            Toggle toggle = Toggle.builder()
                    .style(Style.EMPTY.bold())
                    .build();
            Rect area = new Rect(0, 0, 10, 1);
            Buffer buffer = Buffer.empty(area);
            ToggleState state = new ToggleState(true);

            toggle.render(area, buffer, state);

            assertThat(buffer.get(0, 0).style().addModifiers()).contains(Modifier.BOLD);
        }

        @Test
        @DisplayName("default width is 5")
        void defaultWidthIs5() {
            Toggle toggle = Toggle.builder().build();
            assertThat(toggle.width()).isEqualTo(5); // "[ON ]" and "[OFF]"
        }
    }

    @Nested
    @DisplayName("CSS property resolution")
    class CssPropertyResolution {

        @Test
        @DisplayName("resolves on symbol from style resolver")
        void resolvesOnSymbolFromCss() {
            Toggle toggle = Toggle.builder()
                    .styleResolver(TestStylePropertyResolver.of("toggle-on-symbol", "ENABLED"))
                    .build();
            Rect area = new Rect(0, 0, 10, 1);
            Buffer buffer = Buffer.empty(area);
            ToggleState state = new ToggleState(true);

            toggle.render(area, buffer, state);

            Buffer expected = Buffer.empty(area);
            expected.setString(0, 0, "ENABLED", Style.EMPTY.fg(Color.GREEN));
            BufferAssertions.assertThat(buffer).isEqualTo(expected);
        }

        @Test
        @DisplayName("resolves off symbol from style resolver")
        void resolvesOffSymbolFromCss() {
            Toggle toggle = Toggle.builder()
                    .styleResolver(TestStylePropertyResolver.of("toggle-off-symbol", "DISABLED"))
                    .build();
            Rect area = new Rect(0, 0, 10, 1);
            Buffer buffer = Buffer.empty(area);
            ToggleState state = new ToggleState(false);

            toggle.render(area, buffer, state);

            Buffer expected = Buffer.empty(area);
            expected.setString(0, 0, "DISABLED", Style.EMPTY);
            BufferAssertions.assertThat(buffer).isEqualTo(expected);
        }

        @Test
        @DisplayName("resolves on color from style resolver")
        void resolvesOnColorFromCss() {
            Toggle toggle = Toggle.builder()
                    .styleResolver(TestStylePropertyResolver.of("toggle-on-color", Color.YELLOW))
                    .build();
            Rect area = new Rect(0, 0, 10, 1);
            Buffer buffer = Buffer.empty(area);
            ToggleState state = new ToggleState(true);

            toggle.render(area, buffer, state);

            Buffer expected = Buffer.empty(area);
            expected.setString(0, 0, "[ON ]", Style.EMPTY.fg(Color.YELLOW));
            BufferAssertions.assertThat(buffer).isEqualTo(expected);
        }

        @Test
        @DisplayName("explicit value takes precedence over CSS")
        void explicitTakesPrecedenceOverCss() {
            Toggle toggle = Toggle.builder()
                    .onSymbol("YES")
                    .styleResolver(TestStylePropertyResolver.of("toggle-on-symbol", "NO"))
                    .build();
            Rect area = new Rect(0, 0, 10, 1);
            Buffer buffer = Buffer.empty(area);
            ToggleState state = new ToggleState(true);

            toggle.render(area, buffer, state);

            Buffer expected = Buffer.empty(area);
            expected.setString(0, 0, "YES", Style.EMPTY.fg(Color.GREEN));
            BufferAssertions.assertThat(buffer).isEqualTo(expected);
        }
    }

    @Nested
    @DisplayName("Inline choice mode")
    class InlineChoiceMode {

        @Test
        @DisplayName("renders inline choice with default labels when on")
        void rendersInlineChoiceDefaultOn() {
            Toggle toggle = Toggle.builder()
                    .inlineChoice(true)
                    .build();
            Rect area = new Rect(0, 0, 20, 1);
            Buffer buffer = Buffer.empty(area);
            ToggleState state = new ToggleState(true);

            toggle.render(area, buffer, state);

            // "● Yes / ○ No" with Yes selected (green) and No unselected (gray)
            Buffer expected = Buffer.empty(area);
            Style selectedStyle = Style.EMPTY.fg(Color.GREEN);
            Style unselectedStyle = Style.EMPTY.fg(Color.DARK_GRAY);
            expected.setString(0, 0, "● Yes", selectedStyle);
            expected.setString(5, 0, " / ", unselectedStyle);
            expected.setString(8, 0, "○ No", unselectedStyle);
            BufferAssertions.assertThat(buffer).isEqualTo(expected);
        }

        @Test
        @DisplayName("renders inline choice with default labels when off")
        void rendersInlineChoiceDefaultOff() {
            Toggle toggle = Toggle.builder()
                    .inlineChoice(true)
                    .build();
            Rect area = new Rect(0, 0, 20, 1);
            Buffer buffer = Buffer.empty(area);
            ToggleState state = new ToggleState(false);

            toggle.render(area, buffer, state);

            // "○ Yes / ● No" with No selected (green) and Yes unselected (gray)
            Buffer expected = Buffer.empty(area);
            Style selectedStyle = Style.EMPTY.fg(Color.GREEN);
            Style unselectedStyle = Style.EMPTY.fg(Color.DARK_GRAY);
            expected.setString(0, 0, "○ Yes", unselectedStyle);
            expected.setString(5, 0, " / ", unselectedStyle);
            expected.setString(8, 0, "● No", selectedStyle);
            BufferAssertions.assertThat(buffer).isEqualTo(expected);
        }

        @Test
        @DisplayName("renders inline choice with custom labels")
        void rendersInlineChoiceCustomLabels() {
            Toggle toggle = Toggle.builder()
                    .inlineChoice(true)
                    .onLabel("Enabled")
                    .offLabel("Disabled")
                    .build();
            Rect area = new Rect(0, 0, 30, 1);
            Buffer buffer = Buffer.empty(area);
            ToggleState state = new ToggleState(true);

            toggle.render(area, buffer, state);

            Style selectedStyle = Style.EMPTY.fg(Color.GREEN);
            Style unselectedStyle = Style.EMPTY.fg(Color.DARK_GRAY);
            Buffer expected = Buffer.empty(area);
            expected.setString(0, 0, "● Enabled", selectedStyle);
            expected.setString(9, 0, " / ", unselectedStyle);
            expected.setString(12, 0, "○ Disabled", unselectedStyle);
            BufferAssertions.assertThat(buffer).isEqualTo(expected);
        }

        @Test
        @DisplayName("applies custom selected/unselected colors")
        void appliesCustomColors() {
            Toggle toggle = Toggle.builder()
                    .inlineChoice(true)
                    .selectedColor(Color.CYAN)
                    .unselectedColor(Color.RED)
                    .build();
            Rect area = new Rect(0, 0, 20, 1);
            Buffer buffer = Buffer.empty(area);
            ToggleState state = new ToggleState(true);

            toggle.render(area, buffer, state);

            Style selectedStyle = Style.EMPTY.fg(Color.CYAN);
            Style unselectedStyle = Style.EMPTY.fg(Color.RED);
            Buffer expected = Buffer.empty(area);
            expected.setString(0, 0, "● Yes", selectedStyle);
            expected.setString(5, 0, " / ", unselectedStyle);
            expected.setString(8, 0, "○ No", unselectedStyle);
            BufferAssertions.assertThat(buffer).isEqualTo(expected);
        }

        @Test
        @DisplayName("custom indicators work")
        void customIndicatorsWork() {
            Toggle toggle = Toggle.builder()
                    .inlineChoice(true)
                    .selectedIndicator("▶")
                    .unselectedIndicator("▷")
                    .build();
            Rect area = new Rect(0, 0, 20, 1);
            Buffer buffer = Buffer.empty(area);
            ToggleState state = new ToggleState(true);

            toggle.render(area, buffer, state);

            Style selectedStyle = Style.EMPTY.fg(Color.GREEN);
            Style unselectedStyle = Style.EMPTY.fg(Color.DARK_GRAY);
            Buffer expected = Buffer.empty(area);
            expected.setString(0, 0, "▶ Yes", selectedStyle);
            expected.setString(5, 0, " / ", unselectedStyle);
            expected.setString(8, 0, "▷ No", unselectedStyle);
            BufferAssertions.assertThat(buffer).isEqualTo(expected);
        }

        @Test
        @DisplayName("width calculation includes both options")
        void widthIncludesBothOptions() {
            Toggle toggle = Toggle.builder()
                    .inlineChoice(true)
                    .onLabel("Yes")
                    .offLabel("No")
                    .separator(" / ")
                    .build();

            // "● Yes / ○ No" = 1 + 1 + 3 + 3 + 1 + 1 + 2 = 12
            assertThat(toggle.width()).isEqualTo(12);
        }

        @Test
        @DisplayName("isInlineChoice returns true")
        void isInlineChoiceReturnsTrue() {
            Toggle toggle = Toggle.builder().inlineChoice(true).build();
            assertThat(toggle.isInlineChoice()).isTrue();
        }
    }

    @Nested
    @DisplayName("ToggleState")
    class ToggleStateTests {

        @Test
        @DisplayName("default state is off")
        void defaultStateIsOff() {
            ToggleState state = new ToggleState();
            assertThat(state.isOn()).isFalse();
        }

        @Test
        @DisplayName("toggle changes state")
        void toggleChangesState() {
            ToggleState state = new ToggleState(false);

            boolean newValue = state.toggle();

            assertThat(newValue).isTrue();
            assertThat(state.isOn()).isTrue();
        }

        @Test
        @DisplayName("value() is alias for isOn()")
        void valueIsAliasForIsOn() {
            ToggleState state = new ToggleState(true);
            assertThat(state.value()).isEqualTo(state.isOn());
        }

        @Test
        @DisplayName("setValue() is alias for setOn()")
        void setValueIsAliasForSetOn() {
            ToggleState state = new ToggleState(false);
            state.setValue(true);
            assertThat(state.isOn()).isTrue();
        }
    }
}
