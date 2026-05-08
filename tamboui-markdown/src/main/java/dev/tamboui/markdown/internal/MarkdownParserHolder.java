/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.markdown.internal;

import java.util.Arrays;

import org.commonmark.Extension;
import org.commonmark.ext.gfm.strikethrough.StrikethroughExtension;
import org.commonmark.ext.gfm.tables.TablesExtension;
import org.commonmark.ext.task.list.items.TaskListItemsExtension;
import org.commonmark.parser.Parser;

/**
 * Lazily-initialized shared parser configured with GFM tables and task-list extensions.
 * Constructing a CommonMark {@link Parser} is non-trivial; reusing a single instance
 * across renders is safe because the parser is stateless once built.
 */
public final class MarkdownParserHolder {

    private static final Parser PARSER;
    static {
        Iterable<Extension> extensions = Arrays.asList(
            TablesExtension.create(),
            StrikethroughExtension.create(),
            TaskListItemsExtension.create()
        );
        PARSER = Parser.builder().extensions(extensions).build();
    }

    private MarkdownParserHolder() {
    }

    /**
     * Returns the shared parser.
     *
     * @return the configured commonmark parser
     */
    public static Parser parser() {
        return PARSER;
    }
}
