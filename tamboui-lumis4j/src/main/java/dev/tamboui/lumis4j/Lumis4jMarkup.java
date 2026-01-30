/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.lumis4j;

import dev.tamboui.style.Color;
import dev.tamboui.style.Style;
import dev.tamboui.text.MarkupParser;
import dev.tamboui.text.Text;
import io.roastedroot.lumis4j.core.Formatter;
import io.roastedroot.lumis4j.core.Lang;
import io.roastedroot.lumis4j.core.Lumis;
import io.roastedroot.lumis4j.core.LumisResult;
import io.roastedroot.lumis4j.core.Theme;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Renders a source string as TamboUI markup using lumis4j for syntax highlighting.
 *
 * <p>Converts source to HTML via lumis4j (html-linked), then to BBCode-style markup, and provides a
 * {@link MarkupParser.StyleResolver} that maps lumis4j highlight class names to terminal styles.
 *
 * <p>Example:
 *
 * <pre>{@code
 * String markup = Lumis4jMarkup.sourceToMarkup(sourceCode, "java");
 * Text text = MarkupParser.parse(markup, Lumis4jMarkup.lumis4jStyleResolver());
 * }</pre>
 */
public final class Lumis4jMarkup {

  /**
   * Map from file extension (lowercase) to lumis4j language identifier. Mirrors the suffix-to-
   * language mapping used by lumis4j (e.g. lumis4j.java).
   */
  public static final Map<String, String> EXT_TO_LANG;

  static {
    Map<String, String> m = new HashMap<>();
    m.put("java", Lang.JAVA.value());
    m.put("py", Lang.PYTHON.value());
    m.put("pyw", Lang.PYTHON.value());
    m.put("js", Lang.JAVASCRIPT.value());
    m.put("mjs", Lang.JAVASCRIPT.value());
    m.put("cjs", Lang.JAVASCRIPT.value());
    m.put("ts", Lang.TYPESCRIPT.value());
    m.put("tsx", Lang.TSX.value());
    m.put("go", Lang.GO.value());
    m.put("rs", Lang.RUST.value());
    m.put("rb", Lang.RUBY.value());
    m.put("kt", Lang.KOTLIN.value());
    m.put("kts", Lang.KOTLIN.value());
    m.put("sql", Lang.SQL.value());
    m.put("html", Lang.HTML.value());
    m.put("htm", Lang.HTML.value());
    m.put("css", Lang.CSS.value());
    m.put("scss", Lang.SCSS.value());
    m.put("json", Lang.JSON.value());
    m.put("xml", Lang.XML.value());
    m.put("yaml", Lang.YAML.value());
    m.put("yml", Lang.YAML.value());
    m.put("md", Lang.MARKDOWN.value());
    m.put("markdown", Lang.MARKDOWN.value());
    m.put("mdx", Lang.MARKDOWN.value());
    m.put("sh", Lang.BASH.value());
    m.put("bash", Lang.BASH.value());
    m.put("c", Lang.C.value());
    m.put("h", Lang.C.value());
    m.put("cpp", Lang.CPP.value());
    m.put("cc", Lang.CPP.value());
    m.put("cxx", Lang.CPP.value());
    m.put("hpp", Lang.CPP.value());
    m.put("cs", Lang.C_SHARP.value());
    m.put("csx", Lang.C_SHARP.value());
    m.put("clj", Lang.CLOJURE.value());
    m.put("cljc", Lang.CLOJURE.value());
    m.put("ex", Lang.ELIXIR.value());
    m.put("exs", Lang.ELIXIR.value());
    m.put("erl", Lang.ERLANG.value());
    m.put("hrl", Lang.ERLANG.value());
    m.put("fs", Lang.F_SHARP.value());
    m.put("fsx", Lang.F_SHARP.value());
    m.put("hs", Lang.HASKELL.value());
    m.put("lhs", Lang.HASKELL.value());
    m.put("elm", Lang.ELM.value());
    m.put("lua", Lang.LUA.value());
    m.put("ml", Lang.OCAML.value());
    m.put("mli", Lang.OCAML_INTERFACE.value());
    m.put("ps1", Lang.POWERSHELL.value());
    m.put("toml", Lang.TOML.value());
    m.put("vue", Lang.VUE.value());
    m.put("dart", Lang.DART.value());
    m.put("swift", Lang.SWIFT.value());
    m.put("zig", Lang.ZIG.value());
    m.put("scala", Lang.SCALA.value());
    m.put("sc", Lang.SCALA.value());
    m.put("php", Lang.PHP.value());
    m.put("pl", Lang.PERL.value());
    m.put("tf", Lang.HCL.value());
    m.put("graphql", Lang.GRAPHQL.value());
    m.put("gql", Lang.GRAPHQL.value());
    m.put("r", Lang.R.value());
    m.put("asm", Lang.ASSEMBLY.value());
    m.put("s", Lang.ASSEMBLY.value());
    m.put("nix", Lang.NIX.value());
    m.put("tex", Lang.LATEX.value());
    m.put("vim", Lang.VIM.value());
    m.put("diff", Lang.DIFF.value());
    m.put("patch", Lang.DIFF.value());
    m.put("csv", Lang.CSV.value());
    m.put("proto", Lang.PROTO_BUF.value());
    m.put("eex", Lang.EEX.value());
    m.put("heex", Lang.HEX.value());
    m.put("erb", Lang.ERB.value());
    m.put("ejs", Lang.EJS.value());
    m.put("svelte", Lang.SVELTE.value());
    m.put("astro", Lang.ASTRO.value());
    m.put("m", Lang.OBJ_C.value());
    m.put("mm", Lang.OBJ_C.value());
    m.put("gleam", Lang.GLEAM.value());
    m.put("dockerfile", Lang.DOCKERFILE.value());
    m.put("makefile", Lang.MAKE.value());
    EXT_TO_LANG = Collections.unmodifiableMap(m);
  }

  private Lumis4jMarkup() {}

  /**
   * Converts source code to BBCode-style markup using lumis4j syntax highlighting.
   *
   * @param source the raw source code
   * @param lang the language identifier (e.g. "java", "python", "rust"), or null for plaintext
   * @return markup string with {@code [class]content[/class]} tags, or plain source if highlighting
   *     fails
   */
  public static String sourceToMarkup(String source, String lang) {
    if (source == null) {
      return "";
    }
    Lang lumisLang = langToLang(lang);
    try (Lumis lumis =
        Lumis.builder()
            .withLang(lumisLang)
            .withTheme(Theme.GITHUB_DARK)
            .withFormatter(Formatter.HTML_LINKED)
            .build()) {
      LumisResult result = lumis.highlight(source);
      if (result.success()) {
        return HtmlToBbcode.convert(result.string());
      }
    } catch (Exception ignored) {
      // fall through to plain
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
  public static Text sourceToText(String source, String lang) {
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
    return EXT_TO_LANG.get(ext);
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
}
