/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.widgets.tabs;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class TabsStateTest {

    @Test
    @DisplayName("Initial state has no selection")
    void initialState() {
        TabsState state = new TabsState();
        assertThat(state.selected()).isNull();
    }

    @Test
    @DisplayName("Constructor with index sets selection")
    void constructorWithIndex() {
        TabsState state = new TabsState(2);
        assertThat(state.selected()).isEqualTo(2);
    }

    @Test
    @DisplayName("Constructor clamps negative index")
    void constructorClampsNegative() {
        TabsState state = new TabsState(-5);
        assertThat(state.selected()).isEqualTo(0);
    }

    @Test
    @DisplayName("select sets the selected tab")
    void select() {
        TabsState state = new TabsState();
        state.select(3);
        assertThat(state.selected()).isEqualTo(3);
    }

    @Test
    @DisplayName("select clamps to zero")
    void selectClampsToZero() {
        TabsState state = new TabsState();
        state.select(-10);
        assertThat(state.selected()).isEqualTo(0);
    }

    @Test
    @DisplayName("clearSelection removes selection")
    void clearSelection() {
        TabsState state = new TabsState(2);
        state.clearSelection();
        assertThat(state.selected()).isNull();
    }

    @Test
    @DisplayName("selectFirst selects first tab")
    void selectFirst() {
        TabsState state = new TabsState();
        state.selectFirst();
        assertThat(state.selected()).isEqualTo(0);
    }

    @Test
    @DisplayName("selectLast selects last tab")
    void selectLast() {
        TabsState state = new TabsState();
        state.selectLast(5);
        assertThat(state.selected()).isEqualTo(4);
    }

    @Test
    @DisplayName("selectLast does nothing with zero tabs")
    void selectLastEmpty() {
        TabsState state = new TabsState();
        state.selectLast(0);
        assertThat(state.selected()).isNull();
    }

    @Test
    @DisplayName("selectNext moves to next tab")
    void selectNext() {
        TabsState state = new TabsState(1);
        state.selectNext(5);
        assertThat(state.selected()).isEqualTo(2);
    }

    @Test
    @DisplayName("selectNext wraps to first at end")
    void selectNextWraps() {
        TabsState state = new TabsState(4);
        state.selectNext(5);
        assertThat(state.selected()).isEqualTo(0);
    }

    @Test
    @DisplayName("selectNext selects first when nothing selected")
    void selectNextFromNull() {
        TabsState state = new TabsState();
        state.selectNext(5);
        assertThat(state.selected()).isEqualTo(0);
    }

    @Test
    @DisplayName("selectNext does nothing with zero tabs")
    void selectNextEmpty() {
        TabsState state = new TabsState();
        state.selectNext(0);
        assertThat(state.selected()).isNull();
    }

    @Test
    @DisplayName("selectPrevious moves to previous tab")
    void selectPrevious() {
        TabsState state = new TabsState(3);
        state.selectPrevious(5);
        assertThat(state.selected()).isEqualTo(2);
    }

    @Test
    @DisplayName("selectPrevious wraps to last at beginning")
    void selectPreviousWraps() {
        TabsState state = new TabsState(0);
        state.selectPrevious(5);
        assertThat(state.selected()).isEqualTo(4);
    }

    @Test
    @DisplayName("selectPrevious selects last when nothing selected")
    void selectPreviousFromNull() {
        TabsState state = new TabsState();
        state.selectPrevious(5);
        assertThat(state.selected()).isEqualTo(4);
    }

    @Test
    @DisplayName("selectPrevious does nothing with zero tabs")
    void selectPreviousEmpty() {
        TabsState state = new TabsState();
        state.selectPrevious(0);
        assertThat(state.selected()).isNull();
    }
}
