//DEPS dev.tamboui:tamboui-widgets:LATEST
//DEPS dev.tamboui:tamboui-toolkit:LATEST
//DEPS dev.tamboui:tamboui-jline3-backend:LATEST
//FILES styles/tree-demo.tcss=../../resources/styles/tree-demo.tcss
/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.demo.tree;

import java.io.IOException;
import java.util.Arrays;

import dev.tamboui.css.engine.StyleEngine;
import dev.tamboui.layout.Flex;
import dev.tamboui.style.Color;
import dev.tamboui.toolkit.app.ToolkitRunner;
import dev.tamboui.toolkit.element.Element;
import dev.tamboui.toolkit.element.StyledElement;
import dev.tamboui.toolkit.elements.TreeElement;
import dev.tamboui.tui.TuiConfig;
import dev.tamboui.widgets.tree.GuideStyle;
import dev.tamboui.widgets.tree.TreeNode;

import static dev.tamboui.toolkit.Toolkit.*;

/**
 * Demo showcasing a tree view with custom node rendering and CSS styling.
 * <p>
 * This demo demonstrates:
 * <ul>
 *   <li>Model/view separation: {@code FileInfo} data model vs. rendering</li>
 *   <li>Custom {@code nodeRenderer} for rich styled content</li>
 *   <li>Icons, colors, and metadata displayed per node</li>
 *   <li>CSS styling with {@code :selected} pseudo-class</li>
 *   <li>Lazy loading of children</li>
 * </ul>
 */
public class TreeDemo {

    // ════════════════════════════════════════════════════════════════
    // Data Model - pure data, no view logic
    // ════════════════════════════════════════════════════════════════

    /**
     * Represents a file or directory with metadata.
     * This is a pure data class with no view/rendering concerns.
     */
    record FileInfo(
            String name,
            FileType type,
            long sizeBytes,
            FileStatus status
    ) {
        enum FileType {DIRECTORY, JAVA, KOTLIN, XML, YAML, JSON, MARKDOWN, GRADLE, TEXT, BINARY, IMAGE}

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
                case DIRECTORY -> "📁";
                case JAVA -> "☕";
                case KOTLIN -> "🇰";
                case XML -> "📋";
                case YAML, JSON -> "⚙";
                case MARKDOWN -> "📝";
                case GRADLE -> "🐘";
                case IMAGE -> "🖼";
                case TEXT -> "📄";
                case BINARY -> "📦";
            };
        }

        String formattedSize() {
            if (type == FileType.DIRECTORY) return "";
            if (sizeBytes < 1024) return sizeBytes + " B";
            if (sizeBytes < 1024 * 1024) return String.format("%.1f KB", sizeBytes / 1024.0);
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

        String statusIndicator() {
            return switch (status) {
                case MODIFIED -> " M";
                case NEW -> " +";
                case IGNORED -> " ○";
                case NORMAL -> "";
            };
        }
    }

    // ════════════════════════════════════════════════════════════════
    // View - renders the model with styled elements
    // ════════════════════════════════════════════════════════════════

    private final TreeElement<FileInfo> treeElement;

    private TreeDemo() {
        treeElement = buildTree();
    }

    @SuppressWarnings("unchecked")
    private TreeElement<FileInfo> buildTree() {
        // Build the tree data model
        var src = node("src", FileInfo.dir("src"))
                .add(node("main", FileInfo.dir("main"))
                        .add(node("java", FileInfo.dir("java"))
                                .add(leaf("App.java", FileInfo.file("App.java", FileInfo.FileType.JAVA, 2048, FileInfo.FileStatus.MODIFIED)))
                                .add(leaf("Config.java", FileInfo.file("Config.java", FileInfo.FileType.JAVA, 1536)))
                                .add(leaf("Router.java", FileInfo.file("Router.java", FileInfo.FileType.JAVA, 3072, FileInfo.FileStatus.NEW)))
                                .expanded())
                        .add(node("kotlin", FileInfo.dir("kotlin"))
                                .add(leaf("Extensions.kt", FileInfo.file("Extensions.kt", FileInfo.FileType.KOTLIN, 892))))
                        .add(node("resources", FileInfo.dir("resources"))
                                .add(leaf("application.yml", FileInfo.file("application.yml", FileInfo.FileType.YAML, 512)))
                                .add(leaf("logback.xml", FileInfo.file("logback.xml", FileInfo.FileType.XML, 1024))))
                        .expanded())
                .add(node("test", FileInfo.dir("test"))
                        .add(node("java", FileInfo.dir("java"))
                                .add(leaf("AppTest.java", FileInfo.file("AppTest.java", FileInfo.FileType.JAVA, 1792)))
                                .add(leaf("RouterTest.java", FileInfo.file("RouterTest.java", FileInfo.FileType.JAVA, 2560)))))
                .expanded();

        var docs = node("docs", FileInfo.dir("docs"))
                .add(leaf("README.md", FileInfo.file("README.md", FileInfo.FileType.MARKDOWN, 4096)))
                .add(leaf("CONTRIBUTING.md", FileInfo.file("CONTRIBUTING.md", FileInfo.FileType.MARKDOWN, 2048)))
                .add(node("api", FileInfo.dir("api"))
                        .childrenLoader(() -> Arrays.asList(
                                leaf("endpoints.md", FileInfo.file("endpoints.md", FileInfo.FileType.MARKDOWN, 8192)),
                                leaf("authentication.md", FileInfo.file("authentication.md", FileInfo.FileType.MARKDOWN, 3072)),
                                leaf("rate-limiting.md", FileInfo.file("rate-limiting.md", FileInfo.FileType.MARKDOWN, 1536))
                        )));

        var build = node("build", FileInfo.dir("build"))
                .add(node("classes", FileInfo.dir("classes"))
                        .add(leaf("App.class", FileInfo.file("App.class", FileInfo.FileType.BINARY, 4096, FileInfo.FileStatus.IGNORED)))
                        .add(leaf("Config.class", FileInfo.file("Config.class", FileInfo.FileType.BINARY, 2048, FileInfo.FileStatus.IGNORED))))
                .add(node("libs", FileInfo.dir("libs"))
                        .childrenLoader(() -> Arrays.asList(
                                leaf("app-1.0.jar", FileInfo.file("app-1.0.jar", FileInfo.FileType.BINARY, 1024 * 1024)),
                                leaf("app-1.0-sources.jar", FileInfo.file("app-1.0-sources.jar", FileInfo.FileType.BINARY, 512 * 1024))
                        )));

        var rootFiles = node(".root-files", FileInfo.dir(".root-files"))
                .add(leaf("build.gradle.kts", FileInfo.file("build.gradle.kts", FileInfo.FileType.GRADLE, 2048, FileInfo.FileStatus.MODIFIED)))
                .add(leaf("settings.gradle.kts", FileInfo.file("settings.gradle.kts", FileInfo.FileType.GRADLE, 512)))
                .add(leaf(".gitignore", FileInfo.file(".gitignore", FileInfo.FileType.TEXT, 256)))
                .add(leaf("LICENSE", FileInfo.file("LICENSE", FileInfo.FileType.TEXT, 1024)))
                .expanded();

        // Create tree with custom node renderer
        // Note: highlightColor/highlightStyle can be removed when using CSS :selected
        TreeElement<FileInfo> tree = tree(src, docs, build, rootFiles);
        return tree
                .title("Project Files")
                .rounded()
                .borderColor(Color.WHITE)
                .highlightSymbol("▶ ")
                .scrollbar()
                .guideStyle(GuideStyle.UNICODE)
                // Custom renderer: each node displays icon, name, size, and status
                .nodeRenderer(this::renderNode)
                .id("tree")
                .focusable();
    }

    /**
     * Custom node renderer that creates rich styled content.
     * This demonstrates model/view separation and varied element types:
     * <ul>
     *   <li>Directories: bold icon + name</li>
     *   <li>Markdown files: multi-line with description preview</li>
     *   <li>Binary/JAR files: with size gauge</li>
     *   <li>Source files: icon + name + size + status badge</li>
     * </ul>
     * When using Row for node rendering, elements need explicit constraints
     * (like fit() or length()) since Row defaults unconstrained elements to fill().
     */
    private StyledElement<?> renderNode(TreeNode<FileInfo> node) {
        var info = node.data();
        if (info == null) {
            return text(node.label()).fit();
        }

        // Dispatch to type-specific renderers
        return switch (info.type()) {
            case DIRECTORY -> renderDirectory(info);
            case MARKDOWN -> renderMarkdown(info);
            case BINARY -> renderBinary(info);
            default -> renderSourceFile(info);
        };
    }

    /** Directories: bold icon + name */
    private StyledElement<?> renderDirectory(FileInfo info) {
        var iconElement = text(info.icon() + " ").fit();
        var nameElement = text(info.name()).bold().fit();
        return row(iconElement, nameElement, spacer()).flex(Flex.START);
    }

    /** Markdown files: multi-line with filename and description preview */
    private StyledElement<?> renderMarkdown(FileInfo info) {
        var iconElement = text(info.icon() + " ").fit();
        var nameElement = text(info.name()).cyan().fit();
        var sizeElement = text(info.formattedSize()).dim().fit();

        // First line: icon + name + size
        var firstLine = row(iconElement, nameElement, spacer(), sizeElement, text(" ").fit());

        // Second line: indented description preview
        var description = getMarkdownDescription(info.name());
        var secondLine = row(text("   ").fit(), text(description).dim().italic().fit());

        return column(firstLine, secondLine);
    }

    /** Get a mock description for markdown files */
    private String getMarkdownDescription(String filename) {
        return switch (filename) {
            case "README.md" -> "Project overview and getting started guide";
            case "CONTRIBUTING.md" -> "Guidelines for contributing to the project";
            case "endpoints.md" -> "REST API endpoint documentation";
            case "authentication.md" -> "Authentication and authorization flows";
            case "rate-limiting.md" -> "API rate limiting policies";
            default -> "Documentation file";
        };
    }

    /** Binary/JAR files: with size gauge showing relative size */
    private StyledElement<?> renderBinary(FileInfo info) {
        var iconElement = text(info.icon() + " ").fit();
        var nameElement = text(info.name()).fit();
        var statusColor = info.statusColor();
        if (statusColor != null) {
            nameElement = nameElement.fg(statusColor);
        }

        // Size gauge: visual bar representing file size (max 1MB = full bar)
        int gaugeWidth = 8;
        int filled = (int) Math.min(gaugeWidth, (info.sizeBytes() * gaugeWidth) / (1024 * 1024));
        var gaugeBar = "█".repeat(filled) + "░".repeat(gaugeWidth - filled);
        var gaugeElement = text("[" + gaugeBar + "]").dim().fit();

        var sizeElement = text(" " + info.formattedSize()).dim().fit();

        return row(iconElement, nameElement, spacer(), gaugeElement, sizeElement, text(" ").fit());
    }

    /** Source files: icon + name + size + optional status badge */
    private StyledElement<?> renderSourceFile(FileInfo info) {
        var iconElement = text(info.icon() + " ").fit();
        var nameElement = text(info.name()).fit();
        var statusColor = info.statusColor();
        if (statusColor != null) {
            nameElement = nameElement.fg(statusColor);
        }

        var leftContent = row(iconElement, nameElement);
        var sizeElement = text(info.formattedSize()).dim().fit();

        // Status badge with background color
        StyledElement<?> rightContent;
        if (info.status() == FileInfo.FileStatus.MODIFIED) {
            var badge = text(" M ").bg(Color.YELLOW).fg(Color.BLACK).fit();
            rightContent = row(sizeElement, text(" ").fit(), badge);
        } else if (info.status() == FileInfo.FileStatus.NEW) {
            var badge = text(" + ").bg(Color.GREEN).fg(Color.BLACK).fit();
            rightContent = row(sizeElement, text(" ").fit(), badge);
        } else if (info.status() == FileInfo.FileStatus.IGNORED) {
            rightContent = row(sizeElement, text(" ○").dim().fit());
        } else {
            rightContent = sizeElement;
        }

        return row(leftContent.fit(), spacer(), rightContent.fit(), text(" ").fit());
    }

    // Helper methods for cleaner tree construction
    private static TreeNode<FileInfo> node(String label, FileInfo data) {
        return TreeNode.of(label, data);
    }

    private static TreeNode<FileInfo> leaf(String label, FileInfo data) {
        return TreeNode.of(label, data).leaf();
    }

    /**
     * Demo entry point.
     *
     * @param args the CLI arguments
     * @throws Exception on unexpected error
     */
    public static void main(String[] args) throws Exception {
        new TreeDemo().run();
    }

    private void run() throws Exception {
        var styleEngine = createStyleEngine();
        var config = TuiConfig.builder()
                .mouseCapture(true)
                .build();

        try (var runner = ToolkitRunner.builder()
                .config(config)
                .styleEngine(styleEngine)
                .build()) {
            runner.run(this::render);
        }
    }

    private StyleEngine createStyleEngine() throws IOException {
        var engine = StyleEngine.create();
        engine.loadStylesheet("/styles/tree-demo.tcss");
        return engine;
    }

    private Element render() {
        var selected = treeElement.selectedNode();
        var info = selected != null ? selected.data() : null;

        return column(
                // Main content
                row(
                        // Tree panel (2/3 width)
                        treeElement.fill(2),

                        // Details panel (1/3 width)
                        panel(
                                column(
                                        text("Selected Node").bold().cyan(),
                                        text(""),
                                        info != null ? renderDetails(info) : text("(none selected)").dim()
                                )
                        ).title("Details").rounded().borderColor(Color.DARK_GRAY).fill()
                ).fill(),

                // Legend
                panel(
                        row(
                                text(" ").length(2),
                                text("M").yellow(), text(" Modified  "),
                                text("+").green(), text(" New  "),
                                text("○").dim(), text(" Ignored  "),
                                spacer(),
                                text("↑↓ Navigate  ←→ Collapse/Expand  Enter Toggle  q Quit").dim()
                        )
                ).rounded().borderColor(Color.DARK_GRAY).length(3)
        );
    }

    private Element renderDetails(FileInfo info) {
        return column(
                row(text("Name:   ").bold().length(8), text(info.name())),
                row(text("Type:   ").bold().length(8), text(info.type().name()).dim()),
                row(text("Size:   ").bold().length(8), text(info.formattedSize()).dim()),
                row(text("Status: ").bold().length(8), renderStatus(info.status())),
                text(""),
                row(text("Icon:   ").bold().length(8), text(info.icon()))
        );
    }

    private Element renderStatus(FileInfo.FileStatus status) {
        return switch (status) {
            case MODIFIED -> text("Modified").yellow();
            case NEW -> text("New").green();
            case IGNORED -> text("Ignored").dim();
            case NORMAL -> text("Normal").dim();
        };
    }
}
