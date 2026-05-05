/**
 * Markdown rendering support for TamboUI TUI library.
 * <p>
 * Renders CommonMark plus GFM tables and task lists to a TamboUI {@code Buffer},
 * reusing existing widgets ({@code Block}, {@code Paragraph}, {@code Table}) for
 * sub-components. The renderer is tolerant of partial input: a sanitizer trims
 * trailing unmatched inline markers before parsing, so a stream of markdown
 * (for example produced by an LLM) can be displayed safely on every frame.
 */
module dev.tamboui.markdown {
    requires transitive dev.tamboui.core;
    requires transitive dev.tamboui.widgets;
    requires org.commonmark;
    requires org.commonmark.ext.gfm.tables;
    requires org.commonmark.ext.gfm.strikethrough;
    requires org.commonmark.ext.task.list.items;

    exports dev.tamboui.markdown;
}
