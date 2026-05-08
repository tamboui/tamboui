/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.markdown.internal;

import java.util.ArrayList;
import java.util.List;

import org.commonmark.ext.gfm.strikethrough.Strikethrough;
import org.commonmark.node.AbstractVisitor;
import org.commonmark.node.Code;
import org.commonmark.node.CustomNode;
import org.commonmark.node.Emphasis;
import org.commonmark.node.HardLineBreak;
import org.commonmark.node.HtmlInline;
import org.commonmark.node.Image;
import org.commonmark.node.Link;
import org.commonmark.node.Node;
import org.commonmark.node.SoftLineBreak;
import org.commonmark.node.StrongEmphasis;
import org.commonmark.node.Text;

import dev.tamboui.markdown.MarkdownStyles;
import dev.tamboui.style.Overflow;
import dev.tamboui.style.Style;
import dev.tamboui.text.CharWidth;
import dev.tamboui.text.Line;
import dev.tamboui.text.Span;

/**
 * Renders the inline children of a markdown block node into a list of
 * width-fitted {@link Line}s. Style is composed top-down as the visitor
 * descends into emphasis, strong, code, and link nodes.
 */
public final class MarkdownInlineRenderer {

    private static final String ELLIPSIS = "...";

    private MarkdownInlineRenderer() {
    }

    /**
     * Renders the inline children of {@code parent} into lines using the
     * given overflow strategy.
     *
     * @param parent the block node whose inline children should be rendered
     * @param baseStyle the style applied to plain text (e.g. heading style)
     * @param maxWidth target width in columns; values &lt;= 0 produce an empty list
     * @param styles the style palette
     * @param overflow how to handle lines wider than {@code maxWidth}
     * @return a list of lines that all fit within {@code maxWidth}
     */
    public static List<Line> render(
        Node parent, Style baseStyle, int maxWidth, MarkdownStyles styles, Overflow overflow) {
        if (maxWidth <= 0) {
            return new ArrayList<>();
        }
        SpanCollector collector = new SpanCollector(baseStyle, styles);
        Node child = parent.getFirstChild();
        while (child != null) {
            child.accept(collector);
            child = child.getNext();
        }
        return applyOverflow(collector.lineGroups, maxWidth, overflow);
    }

    /**
     * Walks the inline children of {@code parent} and emits a list of plain
     * styled spans (without wrapping). Useful for table cells where wrapping
     * is delegated to the {@code Table} widget.
     *
     * @param parent the block node whose inline children should be rendered
     * @param baseStyle the base style applied to plain text
     * @param styles the style palette
     * @return a list of spans flattened from the inline children
     */
    public static List<Span> renderSpans(Node parent, Style baseStyle, MarkdownStyles styles) {
        SpanCollector collector = new SpanCollector(baseStyle, styles);
        Node child = parent.getFirstChild();
        while (child != null) {
            child.accept(collector);
            child = child.getNext();
        }
        List<Span> flat = new ArrayList<>();
        for (List<Span> group : collector.lineGroups) {
            flat.addAll(group);
        }
        return flat;
    }

    private static List<Line> applyOverflow(
        List<List<Span>> sourceLines, int maxWidth, Overflow overflow) {
        List<Line> result = new ArrayList<>();
        switch (overflow) {
            case CLIP:
                for (List<Span> spans : sourceLines) {
                    result.add(clipSpans(spans, maxWidth));
                }
                break;
            case WRAP_CHARACTER:
                for (List<Span> spans : sourceLines) {
                    wrapByCharacter(spans, maxWidth, result);
                }
                break;
            case ELLIPSIS:
                for (List<Span> spans : sourceLines) {
                    result.add(ellipsizeSpans(spans, maxWidth, EllipsisPosition.END));
                }
                break;
            case ELLIPSIS_START:
                for (List<Span> spans : sourceLines) {
                    result.add(ellipsizeSpans(spans, maxWidth, EllipsisPosition.START));
                }
                break;
            case ELLIPSIS_MIDDLE:
                for (List<Span> spans : sourceLines) {
                    result.add(ellipsizeSpans(spans, maxWidth, EllipsisPosition.MIDDLE));
                }
                break;
            case WRAP_WORD:
            default:
                for (List<Span> spans : sourceLines) {
                    wrapByWord(spans, maxWidth, result);
                }
                break;
        }
        if (result.isEmpty()) {
            result.add(Line.empty());
        }
        return result;
    }

    private static void wrapByWord(List<Span> spans, int maxWidth, List<Line> out) {
        List<Span> current = new ArrayList<>();
        int currentWidth = 0;
        for (Span span : spans) {
            String content = span.content();
            int idx = 0;
            int len = content.length();
            while (idx < len) {
                int wordEnd = nextWordEnd(content, idx);
                String segment = content.substring(idx, wordEnd);
                int segWidth = CharWidth.of(segment);

                if (currentWidth + segWidth <= maxWidth) {
                    appendSegment(current, segment, span.style());
                    currentWidth += segWidth;
                    idx = wordEnd;
                    continue;
                }

                if (currentWidth == 0) {
                    String head = CharWidth.substringByWidth(segment, maxWidth);
                    if (head.isEmpty()) {
                        idx = wordEnd;
                        continue;
                    }
                    appendSegment(current, head, span.style());
                    out.add(toLine(current));
                    current = new ArrayList<>();
                    currentWidth = 0;
                    idx += head.length();
                } else {
                    out.add(toLine(current));
                    current = new ArrayList<>();
                    currentWidth = 0;
                    while (idx < len && content.charAt(idx) == ' ') {
                        idx++;
                    }
                }
            }
        }
        out.add(toLine(current));
    }

    private static void wrapByCharacter(List<Span> spans, int maxWidth, List<Line> out) {
        List<Span> current = new ArrayList<>();
        int currentWidth = 0;
        for (Span span : spans) {
            String content = span.content();
            int idx = 0;
            while (idx < content.length()) {
                int remaining = maxWidth - currentWidth;
                if (remaining <= 0) {
                    out.add(toLine(current));
                    current = new ArrayList<>();
                    currentWidth = 0;
                    remaining = maxWidth;
                }
                String tail = content.substring(idx);
                String head = CharWidth.substringByWidth(tail, remaining);
                if (head.isEmpty()) {
                    out.add(toLine(current));
                    current = new ArrayList<>();
                    currentWidth = 0;
                    continue;
                }
                appendSegment(current, head, span.style());
                currentWidth += CharWidth.of(head);
                idx += head.length();
            }
        }
        out.add(toLine(current));
    }

    private static Line clipSpans(List<Span> spans, int maxWidth) {
        List<Span> out = new ArrayList<>();
        int remaining = maxWidth;
        for (Span span : spans) {
            if (remaining <= 0) {
                break;
            }
            int w = span.width();
            if (w <= remaining) {
                out.add(span);
                remaining -= w;
            } else {
                String head = CharWidth.substringByWidth(span.content(), remaining);
                if (!head.isEmpty()) {
                    out.add(Span.styled(head, span.style()));
                }
                break;
            }
        }
        return toLine(out);
    }

    private static Line ellipsizeSpans(List<Span> spans, int maxWidth, EllipsisPosition position) {
        int total = totalWidth(spans);
        if (total <= maxWidth) {
            return toLine(new ArrayList<>(spans));
        }
        Style style = spans.isEmpty() ? Style.EMPTY : spans.get(0).style();
        String text = spansToString(spans);
        int ellipsisWidth = CharWidth.of(ELLIPSIS);
        if (maxWidth <= ellipsisWidth) {
            return Line.from(Span.styled(CharWidth.substringByWidth(text, maxWidth), style));
        }
        int available = maxWidth - ellipsisWidth;
        String truncated;
        switch (position) {
            case START:
                truncated = ELLIPSIS + CharWidth.substringByWidthFromEnd(text, available);
                break;
            case MIDDLE: {
                int leftWidth = (available + 1) / 2;
                int rightWidth = available / 2;
                truncated = CharWidth.substringByWidth(text, leftWidth)
                    + ELLIPSIS
                    + CharWidth.substringByWidthFromEnd(text, rightWidth);
                break;
            }
            case END:
            default:
                truncated = CharWidth.substringByWidth(text, available) + ELLIPSIS;
        }
        return Line.from(Span.styled(truncated, style));
    }

    private static int totalWidth(List<Span> spans) {
        int total = 0;
        for (Span span : spans) {
            total += span.width();
        }
        return total;
    }

    private static String spansToString(List<Span> spans) {
        StringBuilder sb = new StringBuilder();
        for (Span span : spans) {
            sb.append(span.content());
        }
        return sb.toString();
    }

    private enum EllipsisPosition {
        START, MIDDLE, END
    }

    private static int nextWordEnd(String s, int start) {
        int len = s.length();
        if (start >= len) {
            return len;
        }
        char first = s.charAt(start);
        if (first == ' ') {
            int i = start;
            while (i < len && s.charAt(i) == ' ') {
                i++;
            }
            return i;
        }
        int i = start;
        while (i < len && s.charAt(i) != ' ') {
            i++;
        }
        return i;
    }

    private static void appendSegment(List<Span> current, String content, Style style) {
        if (content.isEmpty()) {
            return;
        }
        if (!current.isEmpty()) {
            Span last = current.get(current.size() - 1);
            if (last.style().equals(style)) {
                current.set(current.size() - 1, Span.styled(last.content() + content, style));
                return;
            }
        }
        current.add(Span.styled(content, style));
    }

    private static Line toLine(List<Span> spans) {
        if (spans.isEmpty()) {
            return Line.empty();
        }
        return Line.from(new ArrayList<>(spans));
    }

    /**
     * Walks inline AST nodes and accumulates spans grouped by hard-break
     * boundaries. {@code SoftLineBreak} renders as a single space; only
     * {@code HardLineBreak} starts a new line group.
     */
    private static final class SpanCollector extends AbstractVisitor {

        private final Style baseStyle;
        private final MarkdownStyles styles;
        private final List<List<Span>> lineGroups = new ArrayList<>();
        private List<Span> current = new ArrayList<>();
        private Style activeStyle;
        private String activeLink;

        SpanCollector(Style baseStyle, MarkdownStyles styles) {
            this.baseStyle = baseStyle;
            this.styles = styles;
            this.activeStyle = baseStyle;
            lineGroups.add(current);
        }

        @Override
        public void visit(Text text) {
            push(text.getLiteral());
        }

        @Override
        public void visit(Emphasis emphasis) {
            withStyle(merge(activeStyle, styles.emphasis()), () -> visitChildren(emphasis));
        }

        @Override
        public void visit(StrongEmphasis strong) {
            withStyle(merge(activeStyle, styles.strong()), () -> visitChildren(strong));
        }

        @Override
        public void visit(Code code) {
            Style previous = activeStyle;
            activeStyle = merge(activeStyle, styles.inlineCode());
            push(code.getLiteral());
            activeStyle = previous;
        }

        @Override
        public void visit(Link link) {
            String previousLink = activeLink;
            activeLink = link.getDestination();
            withStyle(merge(activeStyle, styles.link()), () -> visitChildren(link));
            activeLink = previousLink;
        }

        @Override
        public void visit(Image image) {
            String alt = collectPlainText(image);
            String dest = image.getDestination() == null ? "" : image.getDestination();
            String label = "[image: " + alt + "](" + dest + ")";
            Style previous = activeStyle;
            activeStyle = merge(activeStyle, styles.link());
            push(label);
            activeStyle = previous;
        }

        @Override
        public void visit(HtmlInline htmlInline) {
            Style previous = activeStyle;
            activeStyle = merge(activeStyle, styles.html());
            push(htmlInline.getLiteral());
            activeStyle = previous;
        }

        @Override
        public void visit(SoftLineBreak softLineBreak) {
            push(" ");
        }

        @Override
        public void visit(HardLineBreak hardLineBreak) {
            current = new ArrayList<>();
            lineGroups.add(current);
        }

        @Override
        public void visit(CustomNode customNode) {
            if (customNode instanceof Strikethrough) {
                withStyle(merge(activeStyle, styles.strikethrough()), () -> visitChildren(customNode));
            } else {
                visitChildren(customNode);
            }
        }

        private void push(String content) {
            if (content.isEmpty()) {
                return;
            }
            Style style = activeStyle;
            if (activeLink != null) {
                style = style.hyperlink(activeLink);
            }
            current.add(Span.styled(content, style));
        }

        private void withStyle(Style style, Runnable action) {
            Style previous = activeStyle;
            activeStyle = style;
            action.run();
            activeStyle = previous;
        }

        private static Style merge(Style base, Style overlay) {
            return base.patch(overlay);
        }

        private static String collectPlainText(Node parent) {
            StringBuilder sb = new StringBuilder();
            collectPlainTextInto(parent, sb);
            return sb.toString();
        }

        private static void collectPlainTextInto(Node parent, StringBuilder sb) {
            Node child = parent.getFirstChild();
            while (child != null) {
                if (child instanceof Text) {
                    sb.append(((Text) child).getLiteral());
                } else if (child instanceof Code) {
                    sb.append(((Code) child).getLiteral());
                } else {
                    collectPlainTextInto(child, sb);
                }
                child = child.getNext();
            }
        }
    }
}
