/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.widgets.common;

/**
 * Computes the scroll offset that keeps a selected item visible, with a number
 * of neighbouring items kept visible above and below it.
 * <p>
 * This is the equivalent of ratatui's {@code List::scroll_padding} (or vim's
 * {@code scrolloff}): with a padding of {@code n}, the list scrolls as soon as
 * the selection comes within {@code n} items of the top or bottom edge,
 * instead of only when it would leave the viewport. At the start and end of
 * the content the selection still reaches the edge.
 * <p>
 * When the padded span does not fit in the viewport, the padding is reduced
 * until it does, so the selection always stays visible.
 */
public final class ScrollPadding {

    private ScrollPadding() {
    }

    /**
     * Computes the scroll offset, in rows, that keeps the selected item and
     * up to {@code padding} items on each side of it visible.
     * <p>
     * The offset only moves when needed: if the padded span is already
     * visible, {@code offset} is returned unchanged (clamped to the valid
     * range).
     *
     * @param offset the current scroll offset in rows
     * @param itemHeights the height in rows of each item
     * @param selected the index of the selected item (clamped to the items)
     * @param padding the number of items to keep visible on each side of the selection
     * @param viewportHeight the number of visible rows
     * @return the new scroll offset, between 0 and the maximum scroll offset
     */
    public static int computeOffset(int offset, int[] itemHeights, int selected, int padding, int viewportHeight) {
        int count = itemHeights.length;
        if (count == 0) {
            return 0;
        }
        int[] tops = new int[count + 1];
        for (int i = 0; i < count; i++) {
            tops[i + 1] = tops[i] + itemHeights[i];
        }
        int sel = Math.max(0, Math.min(selected, count - 1));

        int pad = Math.max(0, Math.min(padding, count - 1));
        int spanTop;
        int spanBottom;
        while (true) {
            spanTop = tops[Math.max(0, sel - pad)];
            spanBottom = tops[Math.min(count - 1, sel + pad) + 1];
            if (pad == 0 || spanBottom - spanTop <= viewportHeight) {
                break;
            }
            pad--;
        }

        int result = offset;
        if (spanTop < result) {
            result = spanTop;
        } else if (spanBottom > result + viewportHeight) {
            result = spanBottom - viewportHeight;
        }
        int maxOffset = Math.max(0, tops[count] - viewportHeight);
        return Math.max(0, Math.min(result, maxOffset));
    }
}
