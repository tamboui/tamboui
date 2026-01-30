/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.lumis4j;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Converts lumis4j HTML-linked output to BBCode-style markup suitable for {@link
 * dev.tamboui.text.MarkupParser}.
 *
 * <p>Replaces {@code <span class="aclass">string</span>} with {@code [aclass]string[/aclass]}, strips
 * div/pre/code wrappers, and handles nested spans.
 * 
 * Temporary workaround until upstream lumis4j supports BBCode-style markup or some other less verbose markup :)
 */
public final class HtmlToBbcode {

  private static final Pattern SPAN_PATTERN =
      Pattern.compile(
          "<span\\s+class=\"([^\"]+)\"[^>]*>((?:(?!<span).)*?)</span>",
          Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
  private static final Pattern DIV_OPEN_PATTERN =
      Pattern.compile("<div[^>]*>", Pattern.CASE_INSENSITIVE);
  private static final Pattern DIV_CLOSE_PATTERN =
      Pattern.compile("</div>", Pattern.CASE_INSENSITIVE);
  private static final Pattern LEADING_WRAPPER_PATTERN =
      Pattern.compile("^\\s*<pre[^>]*>\\s*<code[^>]*>\\s*", Pattern.CASE_INSENSITIVE);
  private static final Pattern TRAILING_WRAPPER_PATTERN =
      Pattern.compile("\\s*</code>\\s*</pre>\\s*$", Pattern.CASE_INSENSITIVE);

  private HtmlToBbcode() {}

  /**
   * Converts HTML (lumis4j html-linked) to BBCode-style markup.
   *
   * @param html HTML containing {@code <span class="...">...</span>} and optional div/pre/code
   *     wrappers
   * @return markup string with {@code [tag]content[/tag]} and wrappers stripped
   */
  public static String convert(String html) {
    if (html == null || html.isEmpty()) {
      return "";
    }
    String out = html;
    Matcher m = SPAN_PATTERN.matcher(out);
    while (m.find()) {
      String cls = m.group(1);
      String content = m.group(2);
      String replacement = "[" + cls + "]" + content + "[/" + cls + "]";
      out = out.substring(0, m.start()) + replacement + out.substring(m.end());
      m.reset(out);
    }
    out = DIV_OPEN_PATTERN.matcher(out).replaceAll("");
    out = DIV_CLOSE_PATTERN.matcher(out).replaceAll("");
    out = LEADING_WRAPPER_PATTERN.matcher(out).replaceFirst("");
    out = TRAILING_WRAPPER_PATTERN.matcher(out).replaceFirst("");
    return decodeHtmlEntities(out.trim());
  }

  /**
   * Decodes common HTML entities (e.g. {@code &quot;} {@code &amp;} {@code &lt;} {@code &gt;}
   * {@code &apos;} and numeric {@code &#NN;} / {@code &#xNN;}) in the string.
   */
  private static String decodeHtmlEntities(String s) {
    if (s == null || s.isEmpty()) {
      return s;
    }
    StringBuilder sb = new StringBuilder(s.length());
    int i = 0;
    while (i < s.length()) {
      if (s.charAt(i) == '&' && i + 1 < s.length()) {
        int end = s.indexOf(';', i + 1);
        if (end > i + 1) {
          String entity = s.substring(i + 1, end);
          String decoded = decodeEntity(entity);
          if (decoded != null) {
            sb.append(decoded);
            i = end + 1;
            continue;
          }
        }
      }
      sb.append(s.charAt(i));
      i++;
    }
    return sb.toString();
  }

  private static String decodeEntity(String entity) {
    switch (entity) {
      case "quot":
        return "\"";
      case "amp":
        return "&";
      case "lt":
        return "<";
      case "gt":
        return ">";
      case "apos":
        return "'";
      case "nbsp":
        return "\u00A0";
      case "lbrace":
        return "{";
      case "rbrace":
        return "}";
      default:
        break;
    }
    if (entity.startsWith("#")) {
      try {
        int codePoint =
            entity.length() > 1 && (entity.charAt(1) == 'x' || entity.charAt(1) == 'X')
                ? Integer.parseInt(entity.substring(2), 16)
                : Integer.parseInt(entity.substring(1), 10);
        return new String(Character.toChars(codePoint));
      } catch (NumberFormatException e) {
        return null;
      }
    }
    return null;
  }
}
