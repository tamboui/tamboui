/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.lumis4j;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import dev.tamboui.style.Color;
import dev.tamboui.style.Style;
import dev.tamboui.text.MarkupParser;
import dev.tamboui.text.Text;

import io.roastedroot.lumis4j.core.Formatter;
import io.roastedroot.lumis4j.core.Lang;
import io.roastedroot.lumis4j.core.Lumis;
import io.roastedroot.lumis4j.core.LumisResult;
import io.roastedroot.lumis4j.core.Theme;

/**
 * Renders a source string as TamboUI markup using lumis4j for syntax highlighting.
 */
public final class Lumis4jMarkup implements AutoCloseable {

  /**
   * Map from file extension (lowercase) to lumis4j language identifier. Mirrors the suffix-to-
   * language mapping used by lumis4j (e.g. lumis4j.java).
   */
  public static final Map<String, String> EXT_TO_LANG;

  static {
    Map<String, String> m = new HashMap<>();
    m.put("py", Lang.PYTHON.value());
    m.put("pyw", Lang.PYTHON.value());
    m.put("js", Lang.JAVASCRIPT.value());
    m.put("mjs", Lang.JAVASCRIPT.value());
    m.put("cjs", Lang.JAVASCRIPT.value());
    m.put("ts", Lang.TYPESCRIPT.value());
    m.put("tsx", Lang.TSX.value());
    m.put("rb", Lang.RUBY.value());
    m.put("kt", Lang.KOTLIN.value());
    m.put("kts", Lang.KOTLIN.value());
    m.put("htm", Lang.HTML.value());
    m.put("scss", Lang.SCSS.value());
    m.put("yml", Lang.YAML.value());
    m.put("mdx", Lang.MARKDOWN.value());
    m.put("bash", Lang.BASH.value());
    m.put("h", Lang.C.value());
    m.put("cc", Lang.CPP.value());
    m.put("cxx", Lang.CPP.value());
    m.put("hpp", Lang.CPP.value());
    m.put("csx", Lang.C_SHARP.value());
    m.put("cljc", Lang.CLOJURE.value());
    m.put("exs", Lang.ELIXIR.value());
    m.put("hrl", Lang.ERLANG.value());
    m.put("fsx", Lang.F_SHARP.value());
    m.put("lhs", Lang.HASKELL.value());
    m.put("mli", Lang.OCAML_INTERFACE.value());
    m.put("sc", Lang.SCALA.value());
    m.put("tf", Lang.HCL.value());
    m.put("gql", Lang.GRAPHQL.value());
    m.put("asm", Lang.ASSEMBLY.value());
    m.put("s", Lang.ASSEMBLY.value());
    m.put("patch", Lang.DIFF.value());
    m.put("proto", Lang.PROTO_BUF.value());
    m.put("heex", Lang.HEX.value());
    m.put("svelte", Lang.SVELTE.value());
    m.put("astro", Lang.ASTRO.value());
    m.put("mm", Lang.OBJ_C.value());
    EXT_TO_LANG = Collections.unmodifiableMap(m);
  }

  private final Lumis lumis =
      Lumis.builder().build();

  /**
   * Creates a new Lumis4jMarkup.
   */
  public Lumis4jMarkup() {}

  /**
   * Converts source code to BBCode-style markup using lumis4j syntax highlighting.
   *
   * @param source the raw source code
   * @param lang the language identifier (e.g. "java", "python", "rust"), or null for plaintext
   * @return markup string with {@code [class]content[/class]} tags, or plain source if highlighting
   *     fails
   */
  public String sourceToMarkup(String source, String lang) {
    if (source == null) {
      return "";
    }
    Lang lumisLang = langToLang(lang);
    LumisResult result = lumis
            .highlighter()
            .withLang(lumisLang)
            .withTheme(Theme.DRACULA)
            .withFormatter(Formatter.BBCODE)
            .build()
            .highlight(source);
    if (result.success()) {
      return result.string();
    }
    return source;
  }

  /**
   * Returns a style resolver that maps lumis4j highlight class names to TamboUI styles.
   *
   * <p>Use with {@link MarkupParser#parse(String, MarkupParser.StyleResolver)} to render markup
   * produced by {@link #sourceToMarkup(String, String)}.
   *
   * @return resolver for lumis4j tag names (e.g. comment, keyword-import, type, variable)
   */
  public static MarkupParser.StyleResolver lumis4jStyleResolver() {
    return Lumis4jMarkup::styleForTag;
  }

  /**
   * Converts source to markup and parses it into styled {@link Text} using the default lumis4j
   * style resolver.
   *
   * @param source the raw source code
   * @param lang the language identifier (e.g. "java", "python"), or null for plaintext
   * @return styled text suitable for Paragraph or other widgets
   */
  public Text sourceToText(String source, String lang) {
    String markup = sourceToMarkup(source, lang);
    return MarkupParser.parse(markup, lumis4jStyleResolver(), false);
  }

  /**
   * Guesses the lumis4j language from a file name using the same suffix-to-language mapping as
   * lumis4j (e.g. lumis4j.java). Handles special cases: "Dockerfile" and "Makefile" (no extension).
   *
   * @param fileName the file name (e.g. "MyFile.java", "Dockerfile")
   * @return the language identifier (e.g. "java", "dockerfile"), or null for plain text
   */
  public static String guessLangFromFileName(String fileName) {
    if (fileName == null || fileName.isEmpty()) {
      return null;
    }
    String name = fileName.toLowerCase();
    if (name.equals("dockerfile")) {
      return Lang.DOCKERFILE.value();
    }
    if (name.equals("makefile")) {
      return Lang.MAKE.value();
    }
    int dot = name.lastIndexOf('.');
    if (dot < 0) {
      return null;
    }
    String ext = name.substring(dot + 1);
    return EXT_TO_LANG.getOrDefault(ext, ext);
  }

  private static Lang langToLang(String lang) {
    if (lang == null || lang.isEmpty()) {
      return Lang.PLAINTEXT;
    }
    String normalized = lang.toLowerCase().trim().replace('-', '_');
    try {
      return Lang.fromString(normalized);
    } catch (IllegalArgumentException e) {
      try {
        return Lang.fromString(normalized.replace("_", ""));
      } catch (IllegalArgumentException e2) {
        return Lang.PLAINTEXT;
      }
    }
  }

  private static Style styleForTag(String tagName) {
    if (tagName == null) {
      return null;
    }
    String t = tagName.toLowerCase();
    // Comments
    if (t.startsWith("comment")) {
      return Style.EMPTY.fg(Color.GRAY).dim();
    }
    // Keywords
    if (t.startsWith("keyword")) {
      return Style.EMPTY.fg(Color.MAGENTA).bold();
    }
    // Types
    if (t.equals("type") || t.startsWith("type-")) {
      return Style.EMPTY.fg(Color.CYAN);
    }
    // Strings and characters
    if (t.startsWith("string") || t.startsWith("character")) {
      return Style.EMPTY.fg(Color.GREEN);
    }
    // Functions/methods
    if (t.startsWith("function")) {
      return Style.EMPTY.fg(Color.YELLOW);
    }
    // Variables
    if (t.startsWith("variable")) {
      return Style.EMPTY.fg(Color.WHITE);
    }
    // Constants
    if (t.startsWith("constant")) {
      return Style.EMPTY.fg(Color.CYAN);
    }
    // Numbers
    if (t.startsWith("number")) {
      return Style.EMPTY.fg(Color.CYAN);
    }
    // Booleans
    if (t.startsWith("boolean")) {
      return Style.EMPTY.fg(Color.MAGENTA);
    }
    // Punctuation
    if (t.startsWith("punctuation")) {
      return Style.EMPTY.fg(Color.GRAY);
    }
    // Attributes, operators
    if (t.startsWith("attribute") || t.equals("operator")) {
      return Style.EMPTY.fg(Color.YELLOW);
    }
    // Default: dim gray for unknown
    return Style.EMPTY.fg(Color.GRAY).dim();
  }

  @Override
  public void close() {
    if (lumis != null) {
      lumis.close();
    }
  }
}
