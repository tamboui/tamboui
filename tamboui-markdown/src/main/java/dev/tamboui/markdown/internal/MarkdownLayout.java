/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.markdown.internal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.commonmark.ext.gfm.tables.TableBlock;
import org.commonmark.ext.gfm.tables.TableBody;
import org.commonmark.ext.gfm.tables.TableCell;
import org.commonmark.ext.gfm.tables.TableHead;
import org.commonmark.ext.gfm.tables.TableRow;
import org.commonmark.ext.task.list.items.TaskListItemMarker;
import org.commonmark.node.BlockQuote;
import org.commonmark.node.BulletList;
import org.commonmark.node.FencedCodeBlock;
import org.commonmark.node.Heading;
import org.commonmark.node.HtmlBlock;
import org.commonmark.node.IndentedCodeBlock;
import org.commonmark.node.LinkReferenceDefinition;
import org.commonmark.node.ListBlock;
import org.commonmark.node.ListItem;
import org.commonmark.node.Node;
import org.commonmark.node.OrderedList;
import org.commonmark.node.Paragraph;
import org.commonmark.node.ThematicBreak;

import dev.tamboui.layout.Constraint;
import dev.tamboui.markdown.MarkdownStyles;
import dev.tamboui.style.Color;
import dev.tamboui.style.Overflow;
import dev.tamboui.style.Style;
import dev.tamboui.text.CharWidth;
import dev.tamboui.text.Line;
import dev.tamboui.text.Span;
import dev.tamboui.widgets.table.Cell;
import dev.tamboui.widgets.table.Row;
import dev.tamboui.widgets.table.Table;
import dev.tamboui.widgets.table.TableState;

/**
 * Walks the markdown AST and produces an ordered list of {@link RenderedChunk}s
 * sized to fit a given content width. The result is consumed by
 * {@code MarkdownView} which lays the chunks out vertically.
 */
public final class MarkdownLayout {

    private static final char HORIZONTAL_RULE_GLYPH = '─';

    private MarkdownLayout() {
    }

    /**
     * Lays out the children of {@code root} into renderable chunks.
     *
     * @param root the root document node
     * @param width content width in columns
     * @param styles the style palette
     * @param overflow how to handle prose lines wider than {@code width}
     * @return an ordered list of chunks; never null, possibly empty
     */
    public static List<RenderedChunk> layout(Node root, int width, MarkdownStyles styles, Overflow overflow) {
        List<RenderedChunk> chunks = new ArrayList<>();
        if (width <= 0) {
            return chunks;
        }
        boolean firstBlock = true;
        Node node = root.getFirstChild();
        while (node != null) {
            if (node instanceof LinkReferenceDefinition) {
                node = node.getNext();
                continue;
            }
            if (!firstBlock && needsSpacingBefore(node)) {
                chunks.add(new LinesChunk(Collections.singletonList(Line.empty())));
            }
            renderBlock(node, width, styles, overflow, chunks);
            firstBlock = false;
            node = node.getNext();
        }
        return chunks;
    }

    private static boolean needsSpacingBefore(Node node) {
        return node instanceof Paragraph
            || node instanceof Heading
            || node instanceof BulletList
            || node instanceof OrderedList
            || node instanceof BlockQuote
            || node instanceof FencedCodeBlock
            || node instanceof IndentedCodeBlock
            || node instanceof HtmlBlock
            || node instanceof TableBlock
            || node instanceof ThematicBreak;
    }

    private static void renderBlock(
        Node node, int width, MarkdownStyles styles, Overflow overflow, List<RenderedChunk> out) {
        if (node instanceof Heading) {
            renderHeading((Heading) node, width, styles, overflow, out);
        } else if (node instanceof Paragraph) {
            out.add(new LinesChunk(MarkdownInlineRenderer.render(node, Style.EMPTY, width, styles, overflow)));
        } else if (node instanceof BulletList || node instanceof OrderedList) {
            out.add(new LinesChunk(renderList((ListBlock) node, "", 0, width, styles, overflow)));
        } else if (node instanceof BlockQuote) {
            out.add(new LinesChunk(renderBlockQuote((BlockQuote) node, width, styles, overflow)));
        } else if (node instanceof FencedCodeBlock) {
            FencedCodeBlock fenced = (FencedCodeBlock) node;
            out.add(CodeBlockBuilder.build(fenced.getLiteral(), fenced.getInfo(), width, styles));
        } else if (node instanceof IndentedCodeBlock) {
            out.add(CodeBlockBuilder.build(((IndentedCodeBlock) node).getLiteral(), null, width, styles));
        } else if (node instanceof HtmlBlock) {
            out.add(new LinesChunk(renderHtmlBlock(((HtmlBlock) node).getLiteral(), width, styles)));
        } else if (node instanceof ThematicBreak) {
            out.add(new LinesChunk(Collections.singletonList(buildHorizontalRule(width, styles))));
        } else if (node instanceof TableBlock) {
            out.add(buildTable((TableBlock) node, width, styles));
        }
    }

    private static void renderHeading(
        Heading heading, int width, MarkdownStyles styles, Overflow overflow, List<RenderedChunk> out) {
        Style headingStyle = styles.heading(heading.getLevel());
        List<Line> lines = MarkdownInlineRenderer.render(heading, headingStyle, width, styles, overflow);
        if (heading.getLevel() <= 2) {
            char glyph = heading.getLevel() == 1 ? '═' : HORIZONTAL_RULE_GLYPH;
            String rule = repeat(glyph, width);
            List<Line> withRule = new ArrayList<>(lines);
            withRule.add(Line.styled(rule, Style.EMPTY.fg(Color.GRAY)));
            out.add(new LinesChunk(withRule));
        } else {
            out.add(new LinesChunk(lines));
        }
    }

    private static List<Line> renderList(
        ListBlock list, String indent, int depth, int width, MarkdownStyles styles, Overflow overflow) {
        List<Line> result = new ArrayList<>();
        boolean ordered = list instanceof OrderedList;
        int counter = ordered ? startNumber((OrderedList) list) : 0;
        Node child = list.getFirstChild();
        while (child != null) {
            if (child instanceof ListItem) {
                String marker = ordered ? (counter++ + ". ") : "• ";
                List<Span> prefix = new ArrayList<>();
                if (!indent.isEmpty()) {
                    prefix.add(Span.styled(indent, styles.listMarker()));
                }
                prefix.add(Span.styled(marker, styles.listMarker()));
                Boolean taskChecked = taskCheckedFlag((ListItem) child);
                if (taskChecked != null) {
                    String symbol = (taskChecked ? styles.taskCheckedSymbol() : styles.taskUncheckedSymbol()) + " ";
                    Style symbolStyle = taskChecked ? styles.taskChecked() : styles.taskUnchecked();
                    prefix.add(Span.styled(symbol, symbolStyle));
                }
                int prefixWidth = totalSpanWidth(prefix);
                String continuationIndent = repeat(' ', prefixWidth);
                renderListItem(
                    (ListItem) child, prefix, continuationIndent, depth, width, styles, overflow, result);
            }
            child = child.getNext();
        }
        return result;
    }

    private static void renderListItem(ListItem item, List<Span> prefix, String continuationIndent,
                                       int depth, int width, MarkdownStyles styles, Overflow overflow,
                                       List<Line> out) {
        Node child = item.getFirstChild();
        boolean firstParagraph = true;
        int prefixWidth = totalSpanWidth(prefix);
        while (child != null) {
            if (child instanceof Paragraph) {
                int contentWidth = Math.max(1, width - prefixWidth);
                List<Line> paragraphLines = MarkdownInlineRenderer.render(
                    child, Style.EMPTY, contentWidth, styles, overflow);
                for (int i = 0; i < paragraphLines.size(); i++) {
                    if (firstParagraph && i == 0) {
                        out.add(prependSpans(prefix, paragraphLines.get(i)));
                    } else {
                        out.add(prependIndent(continuationIndent, paragraphLines.get(i)));
                    }
                }
                firstParagraph = false;
            } else if (child instanceof BulletList || child instanceof OrderedList) {
                List<Line> sub = renderList((ListBlock) child, continuationIndent, depth + 1, width, styles, overflow);
                out.addAll(sub);
            } else if (child instanceof TaskListItemMarker) {
                // Already consumed by taskCheckedFlag; skip.
            } else {
                List<RenderedChunk> embedded = new ArrayList<>();
                int contentWidth = Math.max(1, width - prefixWidth);
                renderBlock(child, contentWidth, styles, overflow, embedded);
                for (RenderedChunk chunk : embedded) {
                    if (chunk instanceof LinesChunk) {
                        for (Line line : ((LinesChunk) chunk).lines()) {
                            if (firstParagraph) {
                                out.add(prependSpans(prefix, line));
                                firstParagraph = false;
                            } else {
                                out.add(prependIndent(continuationIndent, line));
                            }
                        }
                    }
                }
            }
            child = child.getNext();
        }
    }

    private static Boolean taskCheckedFlag(ListItem item) {
        Node first = item.getFirstChild();
        Node target = first instanceof Paragraph ? first.getFirstChild() : first;
        if (target instanceof TaskListItemMarker) {
            return ((TaskListItemMarker) target).isChecked();
        }
        return null;
    }

    private static int totalSpanWidth(List<Span> spans) {
        int total = 0;
        for (Span span : spans) {
            total += span.width();
        }
        return total;
    }

    private static Line prependSpans(List<Span> prefixSpans, Line line) {
        List<Span> spans = new ArrayList<>(prefixSpans);
        spans.addAll(line.spans());
        return Line.from(spans);
    }

    private static Line prependIndent(String indent, Line line) {
        if (indent.isEmpty()) {
            return line;
        }
        List<Span> spans = new ArrayList<>();
        spans.add(Span.raw(indent));
        spans.addAll(line.spans());
        return Line.from(spans);
    }

    private static int startNumber(OrderedList list) {
        Integer markerStart = list.getMarkerStartNumber();
        return markerStart != null ? markerStart : 1;
    }

    private static List<Line> renderBlockQuote(
        BlockQuote quote, int width, MarkdownStyles styles, Overflow overflow) {
        String prefix = styles.blockquotePrefix() + " ";
        int contentWidth = Math.max(1, width - CharWidth.of(prefix));
        List<RenderedChunk> inner = new ArrayList<>();
        Node child = quote.getFirstChild();
        boolean firstChild = true;
        while (child != null) {
            if (!firstChild && needsSpacingBefore(child)) {
                inner.add(new LinesChunk(Collections.singletonList(Line.empty())));
            }
            renderBlock(child, contentWidth, styles, overflow, inner);
            firstChild = false;
            child = child.getNext();
        }
        List<Line> out = new ArrayList<>();
        for (RenderedChunk chunk : inner) {
            if (chunk instanceof LinesChunk) {
                for (Line line : ((LinesChunk) chunk).lines()) {
                    List<Span> spans = new ArrayList<>();
                    spans.add(Span.styled(prefix, styles.blockquote()));
                    for (Span span : line.spans()) {
                        spans.add(Span.styled(span.content(), span.style().patch(styles.blockquote())));
                    }
                    out.add(Line.from(spans));
                }
            }
        }
        return out;
    }

    private static List<Line> renderHtmlBlock(String literal, int width, MarkdownStyles styles) {
        List<Line> out = new ArrayList<>();
        for (String raw : literal.split("\n", -1)) {
            String fitted = CharWidth.of(raw) <= width ? raw : CharWidth.substringByWidth(raw, width);
            out.add(Line.from(Span.styled(fitted, styles.html())));
        }
        if (!out.isEmpty() && out.get(out.size() - 1).isEmpty()) {
            out.remove(out.size() - 1);
        }
        return out;
    }

    private static Line buildHorizontalRule(int width, MarkdownStyles styles) {
        return Line.from(Span.styled(repeat(HORIZONTAL_RULE_GLYPH, width), styles.horizontalRule()));
    }

    private static RenderedChunk buildTable(TableBlock tableBlock, int width, MarkdownStyles styles) {
        Row header = null;
        List<Row> rows = new ArrayList<>();
        int columnCount = 0;
        Node section = tableBlock.getFirstChild();
        while (section != null) {
            if (section instanceof TableHead) {
                Node first = section.getFirstChild();
                if (first instanceof TableRow) {
                    Row built = buildRow((TableRow) first, styles, true);
                    header = built;
                    columnCount = Math.max(columnCount, built.cells().size());
                }
            } else if (section instanceof TableBody) {
                Node row = section.getFirstChild();
                while (row != null) {
                    if (row instanceof TableRow) {
                        Row built = buildRow((TableRow) row, styles, false);
                        rows.add(built);
                        columnCount = Math.max(columnCount, built.cells().size());
                    }
                    row = row.getNext();
                }
            }
            section = section.getNext();
        }

        if (columnCount == 0) {
            return new LinesChunk(Collections.singletonList(Line.empty()));
        }

        List<Constraint> widths = new ArrayList<>(columnCount);
        int per = Math.max(1, 100 / columnCount);
        for (int i = 0; i < columnCount; i++) {
            widths.add(Constraint.percentage(per));
        }

        Table.Builder tableBuilder = Table.builder()
            .rows(rows)
            .widths(widths);
        if (header != null) {
            tableBuilder.header(header);
        }
        Table table = tableBuilder.build();
        int height = rows.size() + (header != null ? 1 : 0);
        TableState state = new TableState();
        return new WidgetChunk((area, buffer) -> table.render(area, buffer, state), height);
    }

    private static Row buildRow(TableRow source, MarkdownStyles styles, boolean header) {
        List<Cell> cells = new ArrayList<>();
        Node cellNode = source.getFirstChild();
        while (cellNode != null) {
            if (cellNode instanceof TableCell) {
                Style baseStyle = header ? styles.strong() : Style.EMPTY;
                List<Span> spans = MarkdownInlineRenderer.renderSpans(cellNode, baseStyle, styles);
                cells.add(Cell.from(Line.from(spans)));
            }
            cellNode = cellNode.getNext();
        }
        return Row.from(cells);
    }

    private static String repeat(char c, int count) {
        if (count <= 0) {
            return "";
        }
        char[] buf = new char[count];
        Arrays.fill(buf, c);
        return new String(buf);
    }
}
