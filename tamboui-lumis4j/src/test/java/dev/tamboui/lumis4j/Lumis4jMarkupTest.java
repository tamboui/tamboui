/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.lumis4j;

import java.beans.Transient;
import java.time.Duration;

import dev.tamboui.text.MarkupParser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import dev.tamboui.text.Text;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTimeout;

class Lumis4jMarkupTest {

  // --- sourceToMarkup ---

  @Test
  @DisplayName("sourceToMarkup returns empty string for null source")
  void sourceToMarkupNullSource() {
    try (Lumis4jMarkup markup = new Lumis4jMarkup()) {
      String result = markup.sourceToMarkup(null, "java");
      assertThat(result).isEmpty();
    }
  }

  @Test
  @DisplayName("sourceToMarkup returns content as-is for empty string")
  void sourceToMarkupEmptySource() {
    try (Lumis4jMarkup markup = new Lumis4jMarkup()) {
      String result = markup.sourceToMarkup("", "java");
      assertThat(result).isEmpty();
    }
  }

  @Test
  @DisplayName("sourceToMarkup with null lang treats as plaintext")
  void sourceToMarkupNullLang() {
    try (Lumis4jMarkup markup = new Lumis4jMarkup()) {
      String source = "hello world";
      String result = markup.sourceToMarkup(source, null);
      assertThat(result).isEqualTo(source);
    }
  }

  @Test
  @DisplayName("sourceToMarkup with empty lang treats as plaintext")
  void sourceToMarkupEmptyLang() {
    try (Lumis4jMarkup markup = new Lumis4jMarkup()) {
      String source = "plain text";
      String result = markup.sourceToMarkup(source, "");
      assertThat(result).isEqualTo(source);
    }
  }

  @Test
  @DisplayName("sourceToMarkup Java code produces BBCODE markup with tags")
  void sourceToMarkupJavaProducesTags() {
    try (Lumis4jMarkup markup = new Lumis4jMarkup()) {
      String source = "public class Foo { // comment\n  private int x;\n}";
      String result = markup.sourceToMarkup(source, "java");
      assertThat(result).contains("[keyword-modifier]public[/keyword-modifier]");
      assertThat(result).contains("public");
      assertThat(result).contains("class");
      assertThat(result).contains("Foo");
      assertThat(result).contains("// comment");
      assertThat(result).contains("[");
      assertThat(result).contains("]");
    }
  }

  @Test
  @DisplayName("sourceToMarkup unknown lang returns source as fallback")
  void sourceToMarkupUnknownLang() {
    try (Lumis4jMarkup markup = new Lumis4jMarkup()) {
      String source = "some random text";
      String result = markup.sourceToMarkup(source, "nonexistent-lang-xyz");
      assertThat(result).isEqualTo(source);
    }
  }

  @Test
  @DisplayName("sourceToMarkup markdown produces markup")
  void sourceToMarkupMarkdown() {
    try (Lumis4jMarkup markup = new Lumis4jMarkup()) {
      String source = "# Title\n\n- item one\n- item two";
      String result = markup.sourceToMarkup(source, "md");
      assertThat(result).isNotEmpty();
      assertThat(result).contains("# Title");
    }
  }

  // --- sourceToText ---

  @Test
  @DisplayName("sourceToText returns styled Text with lines")
  void sourceToTextReturnsText() {
    try (Lumis4jMarkup markup = new Lumis4jMarkup()) {
      String source = "public class X {}";
      Text text = markup.sourceToText(source, "java");
      assertThat(text).isNotNull();
      assertThat(text.lines()).isNotEmpty();
    }
  }

  @Test
  @DisplayName("sourceToText plaintext produces single line")
  void sourceToTextPlaintext() {
    try (Lumis4jMarkup markup = new Lumis4jMarkup()) {
      Text text = markup.sourceToText("hello", null);
      assertThat(text.lines()).hasSize(1);
      assertThat(text.lines().get(0).spans()).isNotEmpty();
      assertThat(text.lines().get(0).spans().get(0).content()).isEqualTo("hello");
    }
  }

  @Test
  @DisplayName("sourceToText multi-line source produces multiple lines")
  void sourceToTextMultiline() {
    try (Lumis4jMarkup markup = new Lumis4jMarkup()) {
      Text text = markup.sourceToText("line1\nline2\nline3", null);
      assertThat(text.lines()).hasSize(3);
    }
  }

  // --- guessLangFromFileName ---

  @Test
  @DisplayName("guessLangFromFileName returns null for null")
  void guessLangNull() {
    assertThat(Lumis4jMarkup.guessLangFromFileName(null)).isNull();
  }

  @Test
  @DisplayName("guessLangFromFileName returns null for empty string")
  void guessLangEmpty() {
    assertThat(Lumis4jMarkup.guessLangFromFileName("")).isNull();
  }

  @Test
  @DisplayName("guessLangFromFileName maps .java to java")
  void guessLangJava() {
    assertThat(Lumis4jMarkup.guessLangFromFileName("Foo.java")).isEqualTo("java");
    assertThat(Lumis4jMarkup.guessLangFromFileName("path/to/Bar.java")).isEqualTo("java");
  }

  @Test
  @DisplayName("guessLangFromFileName is case-insensitive for extension")
  void guessLangCaseInsensitive() {
    assertThat(Lumis4jMarkup.guessLangFromFileName("file.JAVA")).isEqualTo("java");
    assertThat(Lumis4jMarkup.guessLangFromFileName("readme.MD")).isEqualTo("markdown");
  }

  @Test
  @DisplayName("guessLangFromFileName Dockerfile returns dockerfile")
  void guessLangDockerfile() {
    assertThat(Lumis4jMarkup.guessLangFromFileName("Dockerfile")).isEqualTo("dockerfile");
  }

  @Test
  @DisplayName("guessLangFromFileName Makefile returns make")
  void guessLangMakefile() {
    assertThat(Lumis4jMarkup.guessLangFromFileName("Makefile")).isEqualTo("make");
  }

  @Test
  @DisplayName("guessLangFromFileName no extension returns null")
  void guessLangNoExtension() {
    assertThat(Lumis4jMarkup.guessLangFromFileName("README")).isNull();
  }

  @Test
  @DisplayName("guessLangFromFileName unknown extension returns null")
  void guessLangUnknownExtension() {
    assertThat(Lumis4jMarkup.guessLangFromFileName("file.xyz")).isNull();
  }

  @Test
  @DisplayName("guessLangFromFileName maps .md and .markdown to markdown")
  void guessLangMarkdown() {
    assertThat(Lumis4jMarkup.guessLangFromFileName("doc.md")).isEqualTo("markdown");
    assertThat(Lumis4jMarkup.guessLangFromFileName("doc.markdown")).isEqualTo("markdown");
  }

  // --- EXT_TO_LANG ---

  @Test
  @DisplayName("EXT_TO_LANG contains expected common extensions")
  void extToLangContainsCommon() {
    assertThat(Lumis4jMarkup.EXT_TO_LANG).containsEntry("java", "java");
    assertThat(Lumis4jMarkup.EXT_TO_LANG).containsEntry("py", "python");
    assertThat(Lumis4jMarkup.EXT_TO_LANG).containsEntry("md", "markdown");
    assertThat(Lumis4jMarkup.EXT_TO_LANG).containsEntry("rs", "rust");
    assertThat(Lumis4jMarkup.EXT_TO_LANG).containsEntry("go", "go");
  }

  // --- lumis4jStyleResolver ---

  @Test
  @DisplayName("lumis4jStyleResolver is non-null and usable with MarkupParser")
  void lumis4jStyleResolverUsable() {
    assertThat(Lumis4jMarkup.lumis4jStyleResolver()).isNotNull();
    try (Lumis4jMarkup markup = new Lumis4jMarkup()) {
      Text text = markup.sourceToText("// comment", "java");
      assertThat(text.lines()).hasSize(1);
      assertThat(text.lines().get(0).spans()).isNotEmpty();
    }
  }

  // --- Performance (largish content) ---

  private static final int LARGISH_CHARS = 30_000;

  @Test
  @DisplayName("sourceToMarkup completes within 15s for ~30k char markdown")
  void sourceToMarkupPerformanceLargishMarkdown() {
    String largishMd = largishMarkdownContent(LARGISH_CHARS);
    assertThat(largishMd.length()).isGreaterThanOrEqualTo(LARGISH_CHARS);

    try (Lumis4jMarkup markup = new Lumis4jMarkup()) {
      assertTimeout(Duration.ofSeconds(15), () -> {
        String result = markup.sourceToMarkup(largishMd, "md");
        assertThat(result).isNotEmpty();
        assertThat(result.length()).isGreaterThanOrEqualTo(LARGISH_CHARS);
      });
    }
  }

  @Test
  @DisplayName("sourceToText completes within 15s for ~30k char markdown")
  void sourceToTextPerformanceLargishMarkdown() {
    String largishMd = largishMarkdownContent(LARGISH_CHARS);

    try (Lumis4jMarkup markup = new Lumis4jMarkup()) {
      assertTimeout(Duration.ofSeconds(1), () -> {
        Text text = markup.sourceToText(largishMd, "md");
        assertThat(text).isNotNull();
        assertThat(text.lines()).isNotEmpty();
      });
    }
  }

  private static String largishMarkdownContent(int targetChars) {
    StringBuilder sb = new StringBuilder(targetChars + 1024);
    String block =
        "# Section\n\n"
            + "Paragraph with **bold** and *italic* and `code`.\n\n"
            + "- list item one\n"
            + "- list item two\n"
            + "- list item three\n\n"
            + "```java\npublic class Foo { }\n```\n\n";
    while (sb.length() < targetChars) {
      sb.append(block);
    }
    return sb.toString();
  }

  @Test
  void jsonToMarkup() {
    try (Lumis4jMarkup markup = new Lumis4jMarkup()) {
      String json = "{\n" +
        "\t\"postCreateCommand\": \"curl -Ls https://sh.jbang.dev | bash -s - app setup\",\n" +
        "\t\"extensions\": [\"vscjava.vscode-java-pack\"]\n" +
        "}";
      String result = markup.sourceToMarkup(json, "json");

      Text res = MarkupParser.parse(result);

      assertThat(res.rawContent()).contains("[/punctation-bracket]");
      assertThat(result).isNotEmpty();
      assertThat(result).contains("{\"name\": \"John\", \"age\": 30}");
    }
  }
}
