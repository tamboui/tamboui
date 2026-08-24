/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.widgets.toast;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import dev.tamboui.layout.Rect;
import dev.tamboui.style.Style;
import dev.tamboui.terminal.Frame;
import dev.tamboui.widgets.Clear;

/**
 * Queue manager for toast lifecycle state.
 * <p>
 * This class owns enqueueing, deduplication, timed expiration, and dismiss operations.
 */
public final class ToastEngine {

    private final int maxConcurrent;
    private final ToastPosition position;
    private final int offsetX;
    private final int offsetY;
    private final BorderMode borderMode;
    private final ProgressStyle defaultProgressStyle;
    private final ToastCopyHandler copyHandler;

    private final List<ActiveToast> activeToasts;
    private Rect avoidArea;
    private List<Rect> lastRenderedRects;
    private List<ActiveToast> lastRenderedToasts;

    private ToastEngine(Builder builder) {
        this.maxConcurrent = builder.maxConcurrent;
        this.position = builder.position;
        this.offsetX = builder.offsetX;
        this.offsetY = builder.offsetY;
        this.borderMode = builder.borderMode;
        this.defaultProgressStyle = builder.defaultProgressStyle;
        this.copyHandler = builder.copyHandler;
        this.activeToasts = new ArrayList<ActiveToast>();
        this.lastRenderedRects = Collections.emptyList();
        this.lastRenderedToasts = Collections.emptyList();
    }

    /**
     * Creates a new engine builder.
     *
     * @return a new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Enqueues a toast and returns its stable id.
     * <p>
     * With deduplication enabled, timed duplicates refresh remaining lifetime and sticky duplicates are skipped.
     *
     * @param toast the toast to enqueue
     * @return the active toast id
     */
    public String show(Toast toast) {
        Objects.requireNonNull(toast, "toast");
        String key = toast.deduplicationKey();
        if (key != null) {
            ActiveToast duplicate = findByDeduplicationKey(key);
            if (duplicate != null) {
                if (!toast.sticky() && duplicate.remaining != null) {
                    duplicate.remaining = toast.lifetime();
                }
                return duplicate.id;
            }
        }

        String id = UUID.randomUUID().toString();
        Duration remaining = toast.sticky() ? null : toast.lifetime();
        activeToasts.add(new ActiveToast(id, toast, remaining));
        return id;
    }

    /**
     * Advances timed toasts and evicts expired entries.
     *
     * @param elapsed elapsed time since previous tick
     */
    public void tick(Duration elapsed) {
        Objects.requireNonNull(elapsed, "elapsed");
        if (elapsed.isNegative() || elapsed.isZero()) {
            return;
        }

        Iterator<ActiveToast> iterator = activeToasts.iterator();
        while (iterator.hasNext()) {
            ActiveToast active = iterator.next();
            if (active.remaining == null) {
                continue;
            }
            active.remaining = active.remaining.minus(elapsed);
            if (active.remaining.isZero() || active.remaining.isNegative()) {
                iterator.remove();
            }
        }
    }

    /**
     * Dismisses one toast by id.
     *
     * @param id the toast id to dismiss
     */
    public void dismiss(String id) {
        if (id == null) {
            return;
        }
        for (Iterator<ActiveToast> iterator = activeToasts.iterator(); iterator.hasNext();) {
            if (id.equals(iterator.next().id)) {
                iterator.remove();
                break;
            }
        }
    }

    /**
     * Dismisses all active toasts.
     */
    public void dismissAll() {
        activeToasts.clear();
        lastRenderedRects = Collections.emptyList();
        lastRenderedToasts = Collections.emptyList();
    }

    /**
     * Dismisses the topmost active toast, if any.
     *
     * @return the dismissed toast id, or {@code null} when no toasts are active
     */
    public String dismissTop() {
        if (activeToasts.isEmpty()) {
            return null;
        }
        ActiveToast top = activeToasts.get(activeToasts.size() - 1);
        dismiss(top.id);
        return top.id;
    }

    /**
     * Returns the id of the topmost toast whose most recently rendered rectangle contains the point.
     * <p>
     * Hit testing uses the rectangles produced by the last {@link #render} or {@link #computeLayout} pass.
     *
     * @param x column coordinate
     * @param y row coordinate
     * @return the toast id at the point, or {@code null} when none match
     */
    public String toastIdAt(int x, int y) {
        for (int i = lastRenderedRects.size() - 1; i >= 0; i--) {
            if (i < lastRenderedToasts.size() && lastRenderedRects.get(i).contains(x, y)) {
                return lastRenderedToasts.get(i).id;
            }
        }
        return null;
    }

    /**
     * Computes the copyable text for an active toast and notifies the configured copy handler.
     *
     * @param id the toast id
     * @return the copyable text, or {@code null} when the id is not active
     */
    public String requestCopy(String id) {
        for (ActiveToast active : activeToasts) {
            if (active.id.equals(id)) {
                String text = copyText(active);
                if (copyHandler != null) {
                    copyHandler.onCopyRequested(active.id, text);
                }
                return text;
            }
        }
        return null;
    }

    /**
     * Returns the number of currently active toasts.
     *
     * @return active toast count
     */
    public int visibleCount() {
        return activeToasts.size();
    }

    /**
     * Renders all active toasts into the given area.
     *
     * @param frame the frame to render into
     * @param area the screen area available for toast placement
     */
    public void render(Frame frame, Rect area) {
        render(frame, area, null);
    }

    /**
     * Renders all active toasts into the given area using externally resolved styles.
     *
     * @param frame the frame to render into
     * @param area the screen area available for toast placement
     * @param styles host-resolved styles, or {@code null} to use widget defaults
     */
    public void render(Frame frame, Rect area, RenderStyles styles) {
        Objects.requireNonNull(frame, "frame");
        Objects.requireNonNull(area, "area");
        List<ActiveToast> visibleToasts = visibleToasts();
        List<Rect> rects = computeLayout(area);
        for (int i = 0; i < rects.size() && i < visibleToasts.size(); i++) {
            ActiveToast active = visibleToasts.get(i);
            Rect rect = rects.get(i);
            ToastRenderSnapshot snapshot = toSnapshot(active, Math.max(1, rect.width() - 2), styles);
            frame.renderWidget(Clear.INSTANCE, rect);
            frame.renderWidget(new ToastWidget(snapshot), rect);
        }
    }

    /**
     * Sets an optional area that toast layout should avoid when possible.
     *
     * @param avoidArea area to avoid, or {@code null} to disable avoidance
     */
    public void setAvoidArea(Rect avoidArea) {
        this.avoidArea = avoidArea;
    }

    /**
     * Returns toast rectangles from the most recent layout/render pass.
     *
     * @return unmodifiable toast rectangles
     */
    public List<Rect> lastRenderedRects() {
        return lastRenderedRects;
    }

    /**
     * Returns active toast ids in queue order.
     *
     * @return ids of active toasts
     */
    public List<String> activeIds() {
        List<String> ids = new ArrayList<String>(activeToasts.size());
        for (ActiveToast toast : activeToasts) {
            ids.add(toast.id);
        }
        return Collections.unmodifiableList(ids);
    }

    /**
     * Returns the configured max concurrent value.
     *
     * @return max concurrent setting
     */
    public int maxConcurrent() {
        return maxConcurrent;
    }

    /**
     * Returns the configured anchor position.
     *
     * @return toast position
     */
    public ToastPosition position() {
        return position;
    }

    int offsetX() {
        return offsetX;
    }

    int offsetY() {
        return offsetY;
    }

    BorderMode borderMode() {
        return borderMode;
    }

    ProgressStyle defaultProgressStyle() {
        return defaultProgressStyle;
    }

    ToastCopyHandler copyHandler() {
        return copyHandler;
    }

    List<ActiveToast> activeToasts() {
        return Collections.unmodifiableList(activeToasts);
    }

    List<Rect> computeLayout(Rect area) {
        Objects.requireNonNull(area, "area");
        if (area.isEmpty()) {
            lastRenderedRects = Collections.emptyList();
            lastRenderedToasts = Collections.emptyList();
            return lastRenderedRects;
        }

        List<ActiveToast> visibleToasts = visibleToasts();
        List<Rect> rects = new ArrayList<Rect>(visibleToasts.size());
        if (visibleToasts.isEmpty()) {
            lastRenderedRects = Collections.emptyList();
            lastRenderedToasts = Collections.emptyList();
            return lastRenderedRects;
        }

        int topBound = area.top();
        int bottomBound = area.bottom();
        int leftBound = area.left();
        int rightBound = area.right();

        int cursorY = startsFromBottom() ? (bottomBound - 1 - offsetY) : (topBound + offsetY);

        for (ActiveToast active : visibleToasts) {
            ToastSizing.Dimension measured = ToastSizing.measure(active.toast, ToastSizing.DEFAULT_MAX_WIDTH);
            int width = Math.max(1, Math.min(area.width(), measured.width() + 2));
            int height = Math.max(1, Math.min(area.height(), measured.height()));
            int x = anchoredX(area, width);
            int y = startsFromBottom() ? (cursorY - height + 1) : cursorY;
            Rect candidate = area.clamp(new Rect(x, y, width, height));
            candidate = shiftAwayFromAvoidArea(candidate, area);
            rects.add(candidate);
            cursorY = startsFromBottom() ? (candidate.top() - 1 - 1) : (candidate.bottom() + 1);
        }

        lastRenderedRects = Collections.unmodifiableList(rects);
        lastRenderedToasts = Collections.unmodifiableList(visibleToasts);
        return lastRenderedRects;
    }

    private ActiveToast findByDeduplicationKey(String key) {
        for (ActiveToast active : activeToasts) {
            if (key.equals(active.toast.deduplicationKey())) {
                return active;
            }
        }
        return null;
    }

    private List<ActiveToast> visibleToasts() {
        if (activeToasts.isEmpty()) {
            return Collections.emptyList();
        }

        List<ActiveToast> stickies = new ArrayList<ActiveToast>();
        List<ActiveToast> timed = new ArrayList<ActiveToast>();
        for (ActiveToast active : activeToasts) {
            if (active.toast.sticky()) {
                stickies.add(active);
            } else {
                timed.add(active);
            }
        }

        List<ActiveToast> visible = new ArrayList<ActiveToast>(stickies);
        int timedBudget = maxConcurrent - stickies.size();
        if (timed.isEmpty()) {
            return visible;
        }

        if (timedBudget <= 0) {
            // Sticky toasts fill or exceed the cap; timed toasts may still render (transient exceed).
            visible.addAll(timed);
            return visible;
        }

        if (timed.size() <= timedBudget) {
            visible.addAll(timed);
        } else {
            int start = timed.size() - timedBudget;
            visible.addAll(timed.subList(start, timed.size()));
        }
        return visible;
    }

    private static String copyText(ActiveToast active) {
        if (active.toast.title() == null || active.toast.title().isEmpty()) {
            return active.toast.message();
        }
        return active.toast.title() + "\n" + active.toast.message();
    }

    private ToastRenderSnapshot toSnapshot(ActiveToast active, int messageWidth, RenderStyles styles) {
        Toast toast = active.toast;
        List<String> lines = ToastSizing.wrapMessage(toast.message(), messageWidth);
        Style defaultTypeStyle = Style.EMPTY.fg(toast.type().accentColor());
        Style typeStyle = styles != null ? styles.typeStyle(toast.type()) : defaultTypeStyle;
        if (toast.backgroundColor() != null) {
            typeStyle = typeStyle.bg(toast.backgroundColor());
        }
        Style titleStyle = typeStyle.bold();
        Style progressBarStyle = typeStyle;
        Style messageStyle = Style.EMPTY;
        if (toast.backgroundColor() != null) {
            messageStyle = messageStyle.bg(toast.backgroundColor());
        }
        if (styles != null) {
            titleStyle = titleStyle.patch(styles.titleStyle());
            progressBarStyle = progressBarStyle.patch(styles.progressStyle());
        }
        return ToastRenderSnapshot.from(
                active.id,
                toast,
                lines,
                remainingFraction(active),
                borderMode,
                typeStyle,
                titleStyle,
                messageStyle,
                progressBarStyle);
    }

    private double remainingFraction(ActiveToast active) {
        if (active.toast.sticky() || active.remaining == null) {
            return Double.NaN;
        }
        long lifetimeMillis = active.toast.lifetime().toMillis();
        if (lifetimeMillis <= 0) {
            return 0.0;
        }
        double fraction = (double) active.remaining.toMillis() / (double) lifetimeMillis;
        return Math.max(0.0, Math.min(1.0, fraction));
    }

    private int anchoredX(Rect area, int width) {
        switch (position) {
            case TOP_LEFT:
            case BOTTOM_LEFT:
                return area.left() + offsetX;
            case TOP_RIGHT:
            case BOTTOM_RIGHT:
                return area.right() - offsetX - width;
            case CENTER:
            default:
                return area.left() + ((area.width() - width) / 2) + offsetX;
        }
    }

    private boolean startsFromBottom() {
        return position == ToastPosition.BOTTOM_LEFT || position == ToastPosition.BOTTOM_RIGHT;
    }

    private Rect shiftAwayFromAvoidArea(Rect candidate, Rect layoutArea) {
        if (avoidArea == null || avoidArea.isEmpty()) {
            return candidate;
        }
        int minX = layoutArea.left();
        int maxX = layoutArea.right() - candidate.width();
        int minY = layoutArea.top();
        int maxY = layoutArea.bottom() - candidate.height();

        int dx = 0;
        int dy = 0;
        switch (position) {
            case TOP_LEFT:
                dx = 1;
                dy = 1;
                break;
            case TOP_RIGHT:
                dx = -1;
                dy = 1;
                break;
            case BOTTOM_LEFT:
                dx = 1;
                dy = -1;
                break;
            case BOTTOM_RIGHT:
                dx = -1;
                dy = -1;
                break;
            case CENTER:
            default:
                dy = -1;
                break;
        }

        Rect shifted = candidate;
        int maxSteps = Math.max(layoutArea.width(), layoutArea.height()) * 2;
        for (int step = 0; step < maxSteps && shouldShiftFromAvoidArea(shifted, avoidArea, dx, dy); step++) {
            int nextX = clamp(shifted.x() + dx, minX, maxX);
            int nextY = clamp(shifted.y() + dy, minY, maxY);
            if (nextX == shifted.x() && nextY == shifted.y()) {
                break;
            }
            shifted = new Rect(nextX, nextY, shifted.width(), shifted.height());
        }
        return shifted;
    }

    private static boolean overlaps(Rect first, Rect second) {
        return !first.intersection(second).isEmpty();
    }

    private static boolean shouldShiftFromAvoidArea(Rect candidate, Rect avoid, int dx, int dy) {
        if (overlaps(candidate, avoid)) {
            return true;
        }

        boolean yOverlap = candidate.bottom() > avoid.top() && candidate.top() < avoid.bottom();
        boolean xOverlap = candidate.right() > avoid.left() && candidate.left() < avoid.right();
        if (dx < 0 && yOverlap && candidate.right() >= avoid.left()) {
            return true;
        }
        if (dx > 0 && yOverlap && candidate.left() <= avoid.right()) {
            return true;
        }
        if (dy < 0 && xOverlap && candidate.bottom() >= avoid.top()) {
            return true;
        }
        if (dy > 0 && xOverlap && candidate.top() <= avoid.bottom()) {
            return true;
        }
        return false;
    }

    private static int clamp(int value, int min, int max) {
        if (max < min) {
            return min;
        }
        return Math.max(min, Math.min(max, value));
    }

    static final class ActiveToast {
        final String id;
        final Toast toast;
        Duration remaining;

        ActiveToast(String id, Toast toast, Duration remaining) {
            this.id = id;
            this.toast = toast;
            this.remaining = remaining;
        }
    }

    /**
     * Host-provided style set used when rendering through toolkit integrations.
     */
    public static final class RenderStyles {
        private final Style infoStyle;
        private final Style successStyle;
        private final Style warningStyle;
        private final Style errorStyle;
        private final Style titleStyle;
        private final Style progressStyle;

        private RenderStyles(Style infoStyle,
                             Style successStyle,
                             Style warningStyle,
                             Style errorStyle,
                             Style titleStyle,
                             Style progressStyle) {
            this.infoStyle = Objects.requireNonNull(infoStyle, "infoStyle");
            this.successStyle = Objects.requireNonNull(successStyle, "successStyle");
            this.warningStyle = Objects.requireNonNull(warningStyle, "warningStyle");
            this.errorStyle = Objects.requireNonNull(errorStyle, "errorStyle");
            this.titleStyle = Objects.requireNonNull(titleStyle, "titleStyle");
            this.progressStyle = Objects.requireNonNull(progressStyle, "progressStyle");
        }

        /**
         * Creates a complete style set for all toast render parts.
         *
         * @param infoStyle info toast style
         * @param successStyle success toast style
         * @param warningStyle warning toast style
         * @param errorStyle error toast style
         * @param titleStyle title style overlay
         * @param progressStyle progress bar style overlay
         * @return style set instance
         */
        public static RenderStyles of(Style infoStyle,
                                      Style successStyle,
                                      Style warningStyle,
                                      Style errorStyle,
                                      Style titleStyle,
                                      Style progressStyle) {
            return new RenderStyles(infoStyle, successStyle, warningStyle, errorStyle, titleStyle, progressStyle);
        }

        Style typeStyle(ToastType type) {
            switch (type) {
                case SUCCESS:
                    return successStyle;
                case WARNING:
                    return warningStyle;
                case ERROR:
                    return errorStyle;
                case INFO:
                default:
                    return infoStyle;
            }
        }

        Style titleStyle() {
            return titleStyle;
        }

        Style progressStyle() {
            return progressStyle;
        }
    }

    /**
     * Builder for {@link ToastEngine}.
     */
    public static final class Builder {
        private int maxConcurrent = 4;
        private ToastPosition position = ToastPosition.BOTTOM_RIGHT;
        private int offsetX;
        private int offsetY;
        private BorderMode borderMode = BorderMode.SIDE_RAILS;
        private ProgressStyle defaultProgressStyle = ProgressStyle.FULL_BLOCK;
        private ToastCopyHandler copyHandler;

        private Builder() {
        }

        /**
         * Sets max concurrent visible toasts target.
         *
         * @param maxConcurrent maximum target count
         * @return this builder
         */
        public Builder maxConcurrent(int maxConcurrent) {
            if (maxConcurrent < 1) {
                throw new IllegalArgumentException("maxConcurrent must be >= 1");
            }
            this.maxConcurrent = maxConcurrent;
            return this;
        }

        /**
         * Sets engine anchor position.
         *
         * @param position the position
         * @return this builder
         */
        public Builder position(ToastPosition position) {
            this.position = Objects.requireNonNull(position, "position");
            return this;
        }

        /**
         * Sets x-axis offset from position anchor.
         *
         * @param offsetX x offset
         * @return this builder
         */
        public Builder offsetX(int offsetX) {
            this.offsetX = offsetX;
            return this;
        }

        /**
         * Sets y-axis offset from position anchor.
         *
         * @param offsetY y offset
         * @return this builder
         */
        public Builder offsetY(int offsetY) {
            this.offsetY = offsetY;
            return this;
        }

        /**
         * Sets default border mode.
         *
         * @param borderMode border mode
         * @return this builder
         */
        public Builder borderMode(BorderMode borderMode) {
            this.borderMode = Objects.requireNonNull(borderMode, "borderMode");
            return this;
        }

        /**
         * Sets default progress style.
         *
         * @param defaultProgressStyle progress style
         * @return this builder
         */
        public Builder defaultProgressStyle(ProgressStyle defaultProgressStyle) {
            this.defaultProgressStyle = Objects.requireNonNull(defaultProgressStyle, "defaultProgressStyle");
            return this;
        }

        /**
         * Sets copy request callback.
         *
         * @param copyHandler copy callback
         * @return this builder
         */
        public Builder onCopyRequested(ToastCopyHandler copyHandler) {
            this.copyHandler = copyHandler;
            return this;
        }

        /**
         * Builds the engine.
         *
         * @return a new toast engine
         */
        public ToastEngine build() {
            return new ToastEngine(this);
        }
    }
}
