/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.toolkit.elements;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import dev.tamboui.buffer.Buffer;
import dev.tamboui.buffer.Cell;
import dev.tamboui.css.engine.StyleEngine;
import dev.tamboui.layout.Direction;
import dev.tamboui.layout.Rect;
import dev.tamboui.style.Color;
import dev.tamboui.terminal.Frame;
import dev.tamboui.toolkit.element.DefaultRenderContext;

import static dev.tamboui.assertj.BufferAssertions.assertThat;
import static dev.tamboui.toolkit.Toolkit.*;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for DialogElement.
 */
class DialogElementTest {

    @Test
    @DisplayName("styleAttributes exposes title")
    void styleAttributes_exposesTitle() {
        assertThat(dialog().title("Confirm").styleAttributes()).containsEntry("title", "Confirm");
    }

    @Test
    @DisplayName("Attribute selector [title] affects Dialog border color")
    void attributeSelector_title_affectsBorderColor() {
        StyleEngine styleEngine = StyleEngine.create();
        styleEngine.addStylesheet("test",
                "DialogElement[title=\"Confirm\"] { border-color: cyan; }");
        styleEngine.setActiveStylesheet("test");

        DefaultRenderContext context = DefaultRenderContext.createEmpty();
        context.setStyleEngine(styleEngine);

        Rect area = new Rect(0, 0, 40, 10);
        Buffer buffer = Buffer.empty(area);
        Frame frame = Frame.forTesting(buffer);

        dialog("Confirm", text("Delete file?")).rounded().render(frame, area, context);

        // Dialog centers itself, so we need to find the border with cyan color
        // For a 40x10 area with default minWidth=20 and content, the dialog will be
        // centered
        int borderX = -1, borderY = -1;
        for (int x = 0; x < 40 && borderX < 0; x++) {
            for (int y = 0; y < 10 && borderX < 0; y++) {
                Cell cell = buffer.get(x, y);
                if ("╭".equals(cell.symbol())) {
                    borderX = x;
                    borderY = y;
                }
            }
        }
        assertThat(borderX).as("Should find dialog border").isGreaterThanOrEqualTo(0);
        assertThat(buffer).at(borderX, borderY).hasSymbol("╭").hasForeground(Color.CYAN);
    }

    // ============ preferredWidth tests ============

    @Test
    @DisplayName("preferredWidth() returns fixedWidth when set")
    void preferredWidth_fixedWidth() {
        DialogElement dialog = dialog().width(50);
        assertThat(dialog.preferredWidth()).isEqualTo(50);
    }

    @Test
    @DisplayName("preferredWidth() with no title uses minWidth")
    void preferredWidth_noTitle() {
        DialogElement dialog = dialog(); // minWidth default = 20
        // minWidth (20) + padding*2 (2*2=4) + borders (2) = 26
        assertThat(dialog.preferredWidth()).isEqualTo(26);
    }

    @Test
    @DisplayName("preferredWidth() based on title length")
    void preferredWidth_withTitle() {
        DialogElement dialog = dialog("Short"); // "Short" = 5
        // max(minWidth=20, titleWidth=5) + padding*2 (4) + borders (2) = 26
        assertThat(dialog.preferredWidth()).isEqualTo(26);
    }

    @Test
    @DisplayName("preferredWidth() with long title")
    void preferredWidth_longTitle() {
        DialogElement dialog = dialog("This is a very long dialog title"); // 32
        // max(minWidth=20, titleWidth=32) + padding*2 (4) + borders (2) = 38
        assertThat(dialog.preferredWidth()).isEqualTo(38);
    }

    @Test
    @DisplayName("preferredWidth() with custom minWidth")
    void preferredWidth_customMinWidth() {
        DialogElement dialog = dialog().minWidth(30);
        // minWidth (30) + padding*2 (4) + borders (2) = 36
        assertThat(dialog.preferredWidth()).isEqualTo(36);
    }

    @Test
    @DisplayName("preferredWidth() with custom padding")
    void preferredWidth_customPadding() {
        DialogElement dialog = dialog().padding(3);
        // minWidth (20) + padding*2 (3*2=6) + borders (2) = 28
        assertThat(dialog.preferredWidth()).isEqualTo(28);
    }

    @Test
    @DisplayName("preferredWidth() vertical direction with children")
    void preferredWidth_verticalChildren() {
        DialogElement dialog = dialog(text("Short"), // 5
                text("Much longer text") // 16
        );
        // max(minWidth=20, max(5,16)=16) + padding*2 (4) + borders (2) = 26
        assertThat(dialog.preferredWidth()).isEqualTo(26);
    }

    @Test
    @DisplayName("preferredWidth() horizontal direction with children")
    void preferredWidth_horizontalChildren() {
        DialogElement dialog = dialog(text("Yes"), // 3
                text("No") // 2
        ).horizontal();
        // max(minWidth=20, 3+2=5) + padding*2 (4) + borders (2) = 26
        assertThat(dialog.preferredWidth()).isEqualTo(26);
    }

    @Test
    @DisplayName("preferredWidth() horizontal with spacing")
    void preferredWidth_horizontalWithSpacing() {
        DialogElement dialog = dialog(text("Button1"), // 7
                text("Button2"), // 7
                text("Button3") // 7
        ).horizontal().spacing(2);
        // max(minWidth=20, 7+2+7+2+7=25) + padding*2 (4) + borders (2) = 31
        assertThat(dialog.preferredWidth()).isEqualTo(31);
    }

    @Test
    @DisplayName("preferredWidth() with wide children exceeding minWidth")
    void preferredWidth_wideChildren() {
        DialogElement dialog = dialog(text("This is a very long line that exceeds minWidth") // 46
        );
        // max(minWidth=20, 46) + padding*2 (4) + borders (2) = 52
        assertThat(dialog.preferredWidth()).isEqualTo(52);
    }

    @Test
    @DisplayName("preferredWidth() with tabs in dialog")
    void preferredWidth_withTabs() {
        DialogElement dialog = dialog(tabs("Save", "Cancel").divider(" | ") // 13
        ).minWidth(10);
        // max(minWidth=10, 13) + padding*2 (4) + borders (2) = 19
        assertThat(dialog.preferredWidth()).isEqualTo(19);
    }

    @Test
    @DisplayName("preferredWidth() with direction method")
    void preferredWidth_withDirectionMethod() {
        DialogElement vertical = dialog(text("A"), text("BBB")).direction(Direction.VERTICAL);
        DialogElement horizontal = dialog(text("A"), text("BBB")).direction(Direction.HORIZONTAL);

        // Vertical: max(minWidth=20, max(1,3)=3) + padding*2 (4) + borders (2) = 26
        assertThat(vertical.preferredWidth()).isEqualTo(26);
        // Horizontal: max(minWidth=20, 1+3=4) + padding*2 (4) + borders (2) = 26
        assertThat(horizontal.preferredWidth()).isEqualTo(26);
    }

    @Test
    @DisplayName("Dialog with fixed width overrides everything")
    void preferredWidth_fixedWidthOverrides() {
        DialogElement dialog = dialog("Very Long Title That Should Be Ignored").width(30)
                .minWidth(40).padding(5);
        // Fixed width overrides all calculations
        assertThat(dialog.preferredWidth()).isEqualTo(30);
    }
}
