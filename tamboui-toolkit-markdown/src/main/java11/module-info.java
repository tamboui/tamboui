/**
 * Toolkit DSL element wrapping {@code MarkdownView} so markdown content can
 * participate in the toolkit's styling and layout system.
 */
module dev.tamboui.toolkit.markdown {
    requires transitive dev.tamboui.markdown;
    requires transitive dev.tamboui.toolkit;

    exports dev.tamboui.toolkit.markdown;
}
