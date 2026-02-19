///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS dev.tamboui:tamboui-widgets:LATEST
//DEPS dev.tamboui:tamboui-jline3-backend:LATEST

/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.demo;

import java.io.IOException;
import java.util.List;

import dev.tamboui.layout.Constraint;
import dev.tamboui.layout.Layout;
import dev.tamboui.layout.Rect;
import dev.tamboui.style.Color;
import dev.tamboui.style.Style;
import dev.tamboui.terminal.Backend;
import dev.tamboui.terminal.BackendFactory;
import dev.tamboui.terminal.Frame;
import dev.tamboui.terminal.Terminal;
import dev.tamboui.text.Line;
import dev.tamboui.text.Span;
import dev.tamboui.text.Text;
import dev.tamboui.widgets.block.Block;
import dev.tamboui.widgets.block.BorderType;
import dev.tamboui.widgets.block.Borders;
import dev.tamboui.widgets.block.Title;
import dev.tamboui.widgets.common.SizedWidget;
import dev.tamboui.widgets.paragraph.Paragraph;
import dev.tamboui.widgets.tree.GuideStyle;
import dev.tamboui.widgets.tree.TreeNode;
import dev.tamboui.widgets.tree.TreeState;
import dev.tamboui.widgets.tree.TreeWidget;

/**
 * Demo TUI application showcasing the TreeWidget.
 * <p>
 * Demonstrates TreeWidget features using only the widget API (no toolkit):
 * <ul>
 *   <li>Tree navigation with expand/collapse</li>
 *   <li>Custom node rendering with SizedWidget</li>
 *   <li>Guide styles (Unicode, ASCII, None)</li>
 *   <li>Highlight styles and symbols</li>
 *   <li>Scrollbar support</li>
 * </ul>
 */
public class TreeWidgetDemo {

    // ════════════════════════════════════════════════════════════════
    // Data Model
    // ════════════════════════════════════════════════════════════════

    /**
     * Represents a file or directory with metadata.
     */
    record FileInfo(
            String name,
            FileType type,
            long sizeBytes,
            FileStatus status
    ) {
        enum FileType {DIRECTORY, JAVA, KOTLIN, XML, YAML, JSON, MARKDOWN, GRADLE, TEXT, BINARY}

        enum FileStatus {NORMAL, MODIFIED, NEW, IGNORED}

        static FileInfo dir(String name) {
            return new FileInfo(name, FileType.DIRECTORY, 0, FileStatus.NORMAL);
        }

        static FileInfo file(String name, FileType type, long size) {
            return new FileInfo(name, type, size, FileStatus.NORMAL);
        }

        static FileInfo file(String name, FileType type, long size, FileStatus status) {
            return new FileInfo(name, type, size, status);
        }

        String icon() {
            return switch (type) {
                case DIRECTORY -> "\uD83D\uDCC1"; // 📁
                case JAVA -> "\u2615"; // ☕
                case KOTLIN -> "K";
                case XML -> "\uD83D\uDCCB"; // 📋
                case YAML, JSON -> "\u2699"; // ⚙
                case MARKDOWN -> "\uD83D\uDCDD"; // 📝
                case GRADLE -> "\uD83D\uDC18"; // 🐘
                case TEXT -> "\uD83D\uDCC4"; // 📄
                case BINARY -> "\uD83D\uDCE6"; // 📦
            };
        }

        String formattedSize() {
            if (type == FileType.DIRECTORY) {
                return "";
            }
            if (sizeBytes < 1024) {
                return sizeBytes + " B";
            }
            if (sizeBytes < 1024 * 1024) {
                return String.format("%.1f KB", sizeBytes / 1024.0);
            }
            return String.format("%.1f MB", sizeBytes / (1024.0 * 1024));
        }

        Color statusColor() {
            return switch (status) {
                case MODIFIED -> Color.YELLOW;
                case NEW -> Color.GREEN;
                case IGNORED -> Color.DARK_GRAY;
                case NORMAL -> null;
            };
        }
    }

    // ════════════════════════════════════════════════════════════════
    // Application State
    // ════════════════════════════════════════════════════════════════

    private boolean running = true;
    private final TreeState treeState = new TreeState();
    private final List<TreeNode<FileInfo>> roots;
    private List<TreeWidget.FlatEntry<TreeNode<FileInfo>>> lastFlatEntries;
    private GuideStyle currentGuideStyle = GuideStyle.UNICODE;

    private TreeWidgetDemo() {
        this.roots = buildTreeData();
    }

    /**
     * Demo entry point.
     *
     * @param args the CLI arguments
     * @throws Exception on unexpected error
     */
    public static void main(String[] args) throws Exception {
        new TreeWidgetDemo().run();
    }

    /**
     * Runs the demo application.
     *
     * @throws Exception if an error occurs during execution
     */
    public void run() throws Exception {
        try (Backend backend = BackendFactory.create()) {
            backend.enableRawMode();
            backend.enterAlternateScreen();
            backend.hideCursor();

            Terminal<Backend> terminal = new Terminal<>(backend);

            backend.onResize(() -> terminal.draw(this::ui));

            while (running) {
                terminal.draw(this::ui);

                int c = backend.read(100);
                if (c == -2 || c == -1) {
                    continue;
                }

                handleInput(c, backend);
            }
        }
    }

    private void handleInput(int c, Backend backend) throws IOException {
        if (c == 27) {
            int next = backend.peek(50);
            if (next == '[') {
                backend.read(50);
                int code = backend.read(50);
                handleEscapeSequence(code);
            }
            return;
        }

        switch (c) {
            case 'q', 'Q', 3 -> running = false;
            case 'j', 'J' -> selectNext();
            case 'k', 'K' -> treeState.selectPrevious();
            case 'l', 'L' -> expandSelected();
            case 'h', 'H' -> collapseSelected();
            case ' ', '\r', '\n' -> toggleSelected();
            case 'g' -> treeState.selectFirst();
            case 'G' -> selectLast();
            case '1' -> currentGuideStyle = GuideStyle.UNICODE;
            case '2' -> currentGuideStyle = GuideStyle.ASCII;
            case '3' -> currentGuideStyle = GuideStyle.NONE;
        }
    }

    private void handleEscapeSequence(int code) {
        switch (code) {
            case 'A' -> treeState.selectPrevious(); // Up
            case 'B' -> selectNext(); // Down
            case 'C' -> expandSelected(); // Right
            case 'D' -> collapseSelected(); // Left
        }
    }

    private void selectNext() {
        if (lastFlatEntries != null && !lastFlatEntries.isEmpty()) {
            treeState.selectNext(lastFlatEntries.size() - 1);
        }
    }

    private void selectLast() {
        if (lastFlatEntries != null && !lastFlatEntries.isEmpty()) {
            treeState.selectLast(lastFlatEntries.size() - 1);
        }
    }

    private void expandSelected() {
        if (lastFlatEntries == null || lastFlatEntries.isEmpty()) {
            return;
        }
        int idx = Math.min(treeState.selected(), lastFlatEntries.size() - 1);
        TreeNode<FileInfo> node = lastFlatEntries.get(idx).node();
        if (!node.isLeaf()) {
            if (node.isExpanded()) {
                if (!node.children().isEmpty() && idx + 1 < lastFlatEntries.size()) {
                    treeState.select(idx + 1);
                }
            } else {
                node.expanded(true);
            }
        }
    }

    private void collapseSelected() {
        if (lastFlatEntries == null || lastFlatEntries.isEmpty()) {
            return;
        }
        int idx = Math.min(treeState.selected(), lastFlatEntries.size() - 1);
        TreeWidget.FlatEntry<TreeNode<FileInfo>> entry = lastFlatEntries.get(idx);
        TreeNode<FileInfo> node = entry.node();
        if (node.isExpanded() && !node.isLeaf()) {
            node.expanded(false);
        } else {
            TreeNode<FileInfo> parent = entry.parent();
            if (parent != null) {
                for (int i = 0; i < lastFlatEntries.size(); i++) {
                    if (lastFlatEntries.get(i).node() == parent) {
                        treeState.select(i);
                        break;
                    }
                }
            }
        }
    }

    private void toggleSelected() {
        if (lastFlatEntries == null || lastFlatEntries.isEmpty()) {
            return;
        }
        int idx = Math.min(treeState.selected(), lastFlatEntries.size() - 1);
        TreeNode<FileInfo> node = lastFlatEntries.get(idx).node();
        if (!node.isLeaf()) {
            node.toggleExpanded();
        }
    }

    // ════════════════════════════════════════════════════════════════
    // Tree Data Construction
    // ════════════════════════════════════════════════════════════════

    private List<TreeNode<FileInfo>> buildTreeData() {
        TreeNode<FileInfo> src = node("src", FileInfo.dir("src"))
                .add(node("main", FileInfo.dir("main"))
                        .add(node("java", FileInfo.dir("java"))
                                .add(leaf("App.java", FileInfo.file("App.java", FileInfo.FileType.JAVA, 2048, FileInfo.FileStatus.MODIFIED)))
                                .add(leaf("Config.java", FileInfo.file("Config.java", FileInfo.FileType.JAVA, 1536)))
                                .add(leaf("Router.java", FileInfo.file("Router.java", FileInfo.FileType.JAVA, 3072, FileInfo.FileStatus.NEW)))
                                .expanded())
                        .add(node("kotlin", FileInfo.dir("kotlin"))
                                .add(leaf("Extensions.kt", FileInfo.file("Extensions.kt", FileInfo.FileType.KOTLIN, 892))))
                        .add(node("resources", FileInfo.dir("resources"))
                                .add(leaf("application.yml", FileInfo.file("application.yml", FileInfo.FileType.YAML, 512))))
                        .expanded())
                .add(node("test", FileInfo.dir("test"))
                        .add(node("java", FileInfo.dir("java"))
                                .add(leaf("AppTest.java", FileInfo.file("AppTest.java", FileInfo.FileType.JAVA, 1792)))))
                .expanded();

        TreeNode<FileInfo> docs = node("docs", FileInfo.dir("docs"))
                .add(leaf("README.md", FileInfo.file("README.md", FileInfo.FileType.MARKDOWN, 4096)))
                .add(leaf("CONTRIBUTING.md", FileInfo.file("CONTRIBUTING.md", FileInfo.FileType.MARKDOWN, 2048)));

        TreeNode<FileInfo> rootFiles = node(".root-files", FileInfo.dir(".root-files"))
                .add(leaf("build.gradle.kts", FileInfo.file("build.gradle.kts", FileInfo.FileType.GRADLE, 2048, FileInfo.FileStatus.MODIFIED)))
                .add(leaf("settings.gradle.kts", FileInfo.file("settings.gradle.kts", FileInfo.FileType.GRADLE, 512)))
                .add(leaf(".gitignore", FileInfo.file(".gitignore", FileInfo.FileType.TEXT, 256)))
                .expanded();

        return List.of(src, docs, rootFiles);
    }

    private static TreeNode<FileInfo> node(String label, FileInfo data) {
        return TreeNode.of(label, data);
    }

    private static TreeNode<FileInfo> leaf(String label, FileInfo data) {
        return TreeNode.of(label, data).leaf();
    }

    // ════════════════════════════════════════════════════════════════
    // UI Rendering
    // ════════════════════════════════════════════════════════════════

    private void ui(Frame frame) {
        Rect area = frame.area();

        List<Rect> layout = Layout.vertical()
                .constraints(
                        Constraint.length(3),  // Header
                        Constraint.fill(),     // Main content
                        Constraint.length(3)   // Footer
                )
                .split(area);

        renderHeader(frame, layout.get(0));
        renderMainContent(frame, layout.get(1));
        renderFooter(frame, layout.get(2));
    }

    private void renderHeader(Frame frame, Rect area) {
        Block headerBlock = Block.builder()
                .borders(Borders.ALL)
                .borderType(BorderType.ROUNDED)
                .borderStyle(Style.EMPTY.fg(Color.CYAN))
                .title(Title.from(
                        Line.from(
                                Span.raw(" TamboUI ").bold().cyan(),
                                Span.raw("TreeWidget Demo ").yellow()
                        )
                ).centered())
                .build();

        frame.renderWidget(headerBlock, area);
    }

    private void renderMainContent(Frame frame, Rect area) {
        List<Rect> cols = Layout.horizontal()
                .constraints(
                        Constraint.percentage(65),
                        Constraint.percentage(35)
                )
                .spacing(1)
                .split(area);

        renderTree(frame, cols.get(0));
        renderDetails(frame, cols.get(1));
    }

    private void renderTree(Frame frame, Rect area) {
        TreeWidget<TreeNode<FileInfo>> treeWidget = TreeWidget.<TreeNode<FileInfo>>builder()
                .roots(roots)
                .children(TreeNode::children)
                .isLeaf(TreeNode::isLeaf)
                .expansionState(TreeNode::isExpanded, TreeNode::expanded)
                .nodeRenderer(this::renderNode)
                .guideStyle(currentGuideStyle)
                .highlightStyle(Style.EMPTY.reversed())
                .highlightSymbol("\u25B6 ") // ▶
                .scrollbar()
                .scrollbarThumbStyle(Style.EMPTY.fg(Color.CYAN))
                .block(Block.builder()
                        .borders(Borders.ALL)
                        .borderType(BorderType.ROUNDED)
                        .borderStyle(Style.EMPTY.fg(Color.WHITE))
                        .title(Title.from(" Project Files "))
                        .build())
                .build();

        frame.renderStatefulWidget(treeWidget, area, treeState);
        this.lastFlatEntries = treeWidget.lastFlatEntries();
    }

    /**
     * Renders a tree node using SizedWidget with right-aligned metadata.
     */
    private SizedWidget renderNode(TreeNode<FileInfo> node) {
        FileInfo info = node.data();
        if (info == null) {
            return SizedWidget.of(Paragraph.from(node.label()));
        }

        // Create a custom widget that renders left content and right-aligned metadata
        return SizedWidget.of((rect, buffer) -> {
            if (rect.isEmpty()) {
                return;
            }

            // Build left content: icon + name
            Line leftLine = buildLeftContent(info);

            // Build right content: size + badge
            Line rightLine = buildRightContent(info);

            int leftWidth = leftLine.width();
            int rightWidth = rightLine.width();
            int availableWidth = rect.width();

            // Render left content
            buffer.setLine(rect.left(), rect.top(), leftLine);

            // Render right content at right edge (if there's room)
            if (rightWidth > 0 && leftWidth + rightWidth + 1 < availableWidth) {
                int rightX = rect.right() - rightWidth;
                buffer.setLine(rightX, rect.top(), rightLine);
            }
        });
    }

    private Line buildLeftContent(FileInfo info) {
        Span icon = Span.raw(info.icon() + " ");
        Span name = Span.raw(info.name());
        Color statusColor = info.statusColor();
        if (statusColor != null) {
            name = name.fg(statusColor);
        }

        if (info.type() == FileInfo.FileType.DIRECTORY) {
            return Line.from(icon, name.bold());
        }

        return Line.from(icon, name);
    }

    private Line buildRightContent(FileInfo info) {
        if (info.type() == FileInfo.FileType.DIRECTORY) {
            return Line.empty();
        }

        String size = info.formattedSize();
        if (size.isEmpty()) {
            return Line.empty();
        }

        Span sizeSpan = Span.raw(size).dim();

        if (info.status() == FileInfo.FileStatus.MODIFIED) {
            Span badge = Span.raw(" M").bg(Color.YELLOW).fg(Color.BLACK);
            return Line.from(sizeSpan, badge);
        } else if (info.status() == FileInfo.FileStatus.NEW) {
            Span badge = Span.raw(" +").bg(Color.GREEN).fg(Color.BLACK);
            return Line.from(sizeSpan, badge);
        }

        return Line.from(sizeSpan);
    }

    private void renderDetails(Frame frame, Rect area) {
        FileInfo info = getSelectedInfo();

        Text content;
        if (info != null) {
            content = Text.from(
                    Line.from(Span.raw("Name:   ").bold(), Span.raw(info.name())),
                    Line.from(Span.raw("Type:   ").bold(), Span.raw(info.type().name()).dim()),
                    Line.from(Span.raw("Size:   ").bold(), Span.raw(info.formattedSize()).dim()),
                    Line.from(Span.raw("Status: ").bold(), formatStatus(info.status())),
                    Line.empty(),
                    Line.from(Span.raw("Icon:   ").bold(), Span.raw(info.icon()))
            );
        } else {
            content = Text.from(Line.from(Span.raw("(no selection)").dim()));
        }

        Paragraph details = Paragraph.builder()
                .text(content)
                .block(Block.builder()
                        .borders(Borders.ALL)
                        .borderType(BorderType.ROUNDED)
                        .borderStyle(Style.EMPTY.fg(Color.DARK_GRAY))
                        .title(Title.from(" Details "))
                        .build())
                .build();

        frame.renderWidget(details, area);
    }

    private FileInfo getSelectedInfo() {
        if (lastFlatEntries == null || lastFlatEntries.isEmpty()) {
            return null;
        }
        int idx = Math.min(treeState.selected(), lastFlatEntries.size() - 1);
        TreeNode<FileInfo> node = lastFlatEntries.get(idx).node();
        return node.data();
    }

    private Span formatStatus(FileInfo.FileStatus status) {
        return switch (status) {
            case MODIFIED -> Span.raw("Modified").yellow();
            case NEW -> Span.raw("New").green();
            case IGNORED -> Span.raw("Ignored").dim();
            case NORMAL -> Span.raw("Normal").dim();
        };
    }

    private void renderFooter(Frame frame, Rect area) {
        String guideStyleName = switch (currentGuideStyle) {
            case UNICODE -> "Unicode";
            case ASCII -> "ASCII";
            case NONE -> "None";
        };

        Line helpLine = Line.from(
                Span.raw(" Guide: ").dim(),
                Span.raw(guideStyleName).bold().cyan(),
                Span.raw("   "),
                Span.raw("1/2/3").bold().yellow(),
                Span.raw(" Style  ").dim(),
                Span.raw("\u2191\u2193/jk").bold().yellow(),
                Span.raw(" Navigate  ").dim(),
                Span.raw("\u2190\u2192/hl").bold().yellow(),
                Span.raw(" Collapse/Expand  ").dim(),
                Span.raw("Space").bold().yellow(),
                Span.raw(" Toggle  ").dim(),
                Span.raw("q").bold().yellow(),
                Span.raw(" Quit").dim()
        );

        Paragraph footer = Paragraph.builder()
                .text(Text.from(helpLine))
                .block(Block.builder()
                        .borders(Borders.ALL)
                        .borderType(BorderType.ROUNDED)
                        .borderStyle(Style.EMPTY.fg(Color.DARK_GRAY))
                        .build())
                .build();

        frame.renderWidget(footer, area);
    }
}
