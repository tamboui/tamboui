/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.css.parser;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import dev.tamboui.css.model.Stylesheet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CssParserValidationTest {

    @Test
    @DisplayName("valid stylesheet produces no diagnostics")
    void validStylesheetProducesNoDiagnostics() {
        String css = "$bg: #123456;\n" +
                     "$fg: white;\n" +
                     "Panel {\n" +
                     "    background: $bg;\n" +
                     "    color: $fg;\n" +
                     "    width: 50%;\n" +
                     "    height: 10;\n" +
                     "    padding: 1 2;\n" +
                     "    margin: 0;\n" +
                     "    text-style: bold;\n" +
                     "}\n" +
                     "#sidebar {\n" +
                     "    color: inherit;\n" +
                     "    direction: horizontal;\n" +
                     "    flex: space-between;\n" +
                     "}";

        List<CssDiagnostic> diagnostics = CssParser.validate(css);
        assertThat(diagnostics).isEmpty();
    }

    @Test
    @DisplayName("out-of-range values produce OUT_OF_RANGE diagnostics")
    void outOfRangeValuesProduceDiagnostics() {
        String css = "Panel {\n" +
                     "    color: rgb(300, 0, 0);\n" +
                     "    width: 150%;\n" +
                     "    height: 99999999999;\n" +
                     "}";

        List<CssDiagnostic> diagnostics = CssParser.validate(css);
        assertThat(diagnostics).hasSize(3);

        CssDiagnostic d1 = diagnostics.get(0);
        assertThat(d1.kind()).isEqualTo(CssDiagnostic.Kind.OUT_OF_RANGE);
        assertThat(d1.property()).isEqualTo("color");
        assertThat(d1.rawValue()).isEqualTo("rgb(300, 0, 0)");
        assertThat(d1.reason()).contains("out-of-range");
        assertThat(d1.line()).isEqualTo(2);

        CssDiagnostic d2 = diagnostics.get(1);
        assertThat(d2.kind()).isEqualTo(CssDiagnostic.Kind.OUT_OF_RANGE);
        assertThat(d2.property()).isEqualTo("width");
        assertThat(d2.rawValue()).isEqualTo("150%");
        assertThat(d2.reason()).contains("out-of-range");
        assertThat(d2.line()).isEqualTo(3);

        CssDiagnostic d3 = diagnostics.get(2);
        assertThat(d3.kind()).isEqualTo(CssDiagnostic.Kind.OUT_OF_RANGE);
        assertThat(d3.property()).isEqualTo("height");
        assertThat(d3.rawValue()).isEqualTo("99999999999");
        assertThat(d3.reason()).contains("out-of-range");
        assertThat(d3.line()).isEqualTo(4);
    }

    @Test
    @DisplayName("ratio with zero denominator is diagnosed as OUT_OF_RANGE")
    void ratioZeroDenominatorIsOutOfRange() {
        String css = "Panel { width: 1/0; }";
        List<CssDiagnostic> diagnostics = CssParser.validate(css);
        assertThat(diagnostics).hasSize(1);
        assertThat(diagnostics.get(0).kind()).isEqualTo(CssDiagnostic.Kind.OUT_OF_RANGE);
        assertThat(diagnostics.get(0).property()).isEqualTo("width");
        assertThat(diagnostics.get(0).rawValue()).isEqualTo("1/0");
    }

    @Test
    @DisplayName("indexed color out of range is diagnosed as OUT_OF_RANGE")
    void indexedColorOutOfRangeIsOutOfRange() {
        String css = "Panel { color: indexed(300); }";
        List<CssDiagnostic> diagnostics = CssParser.validate(css);
        assertThat(diagnostics).hasSize(1);
        assertThat(diagnostics.get(0).kind()).isEqualTo(CssDiagnostic.Kind.OUT_OF_RANGE);
        assertThat(diagnostics.get(0).property()).isEqualTo("color");
        assertThat(diagnostics.get(0).rawValue()).isEqualTo("indexed(300)");
    }

    @Test
    @DisplayName("unconvertible values produce UNCONVERTIBLE_VALUE diagnostics")
    void unconvertibleValuesProduceDiagnostics() {
        String css = "Panel { color: not-a-color; }";

        List<CssDiagnostic> diagnostics = CssParser.validate(css);
        assertThat(diagnostics).hasSize(1);

        CssDiagnostic d = diagnostics.get(0);
        assertThat(d.kind()).isEqualTo(CssDiagnostic.Kind.UNCONVERTIBLE_VALUE);
        assertThat(d.property()).isEqualTo("color");
        assertThat(d.rawValue()).isEqualTo("not-a-color");
        assertThat(d.reason()).contains("unconvertible");
    }

    @Test
    @DisplayName("unknown properties produce UNKNOWN_PROPERTY diagnostics")
    void unknownPropertyProducesDiagnostics() {
        String css = "Panel {\n" +
                     "    colour: red;\n" +
                     "    widht: 10;\n" +
                     "}";

        List<CssDiagnostic> diagnostics = CssParser.validate(css);
        assertThat(diagnostics).hasSize(2);

        CssDiagnostic d1 = diagnostics.get(0);
        assertThat(d1.kind()).isEqualTo(CssDiagnostic.Kind.UNKNOWN_PROPERTY);
        assertThat(d1.property()).isEqualTo("colour");
        assertThat(d1.rawValue()).isEqualTo("red");
        assertThat(d1.reason()).contains("unknown property");

        CssDiagnostic d2 = diagnostics.get(1);
        assertThat(d2.kind()).isEqualTo(CssDiagnostic.Kind.UNKNOWN_PROPERTY);
        assertThat(d2.property()).isEqualTo("widht");
        assertThat(d2.rawValue()).isEqualTo("10");
    }

    @Test
    @DisplayName("unresolved variables produce UNRESOLVED_VARIABLE diagnostics")
    void unresolvedVariableProducesDiagnostics() {
        String css = "$fg: red;\n" +
                     "Panel { color: $missing-var; background: $fg; }";

        List<CssDiagnostic> diagnostics = CssParser.validate(css);
        assertThat(diagnostics).hasSize(1);

        CssDiagnostic d = diagnostics.get(0);
        assertThat(d.kind()).isEqualTo(CssDiagnostic.Kind.UNRESOLVED_VARIABLE);
        assertThat(d.property()).isEqualTo("color");
        assertThat(d.rawValue()).isEqualTo("$missing-var");
        assertThat(d.reason()).contains("missing-var");
    }

    @Test
    @DisplayName("chained unresolved variables produce UNRESOLVED_VARIABLE diagnostics")
    void chainedUnresolvedVariableProducesDiagnostics() {
        String css = "$fg: $undef;\n" +
                     "Panel { color: $fg; }";

        List<CssDiagnostic> diagnostics = CssParser.validate(css);
        assertThat(diagnostics).hasSize(1);
        assertThat(diagnostics.get(0).kind()).isEqualTo(CssDiagnostic.Kind.UNRESOLVED_VARIABLE);
        assertThat(diagnostics.get(0).reason()).contains("undef");
    }

    @Test
    @DisplayName("validate(String) matches validate(Stylesheet)")
    void validateStringMatchesValidateStylesheet() {
        String css = "Panel { color: bogus; }";
        Stylesheet stylesheet = CssParser.parse(css);

        List<CssDiagnostic> d1 = CssParser.validate(css);
        List<CssDiagnostic> d2 = CssParser.validate(stylesheet);

        assertThat(d1).isEqualTo(d2);
    }

    @Test
    @DisplayName("strict parse throws CssParseException on invalid declarations")
    void strictParseThrowsOnInvalidDeclarations() {
        String css = "Panel { color: bogus; }";

        assertThatThrownBy(() -> CssParser.parse(css, true))
                .isInstanceOf(CssParseException.class)
                .hasMessageContaining("CSS validation failed")
                .satisfies(e -> {
                    CssParseException cpe = (CssParseException) e;
                    assertThat(cpe.diagnostics()).hasSize(1);
                    assertThat(cpe.diagnostics().get(0).property()).isEqualTo("color");
                    assertThat(cpe.position().line()).isEqualTo(1);
                });
    }

    @Test
    @DisplayName("non-strict parse succeeds on invalid declarations")
    void nonStrictParseSucceedsOnInvalidDeclarations() {
        String css = "Panel { color: bogus; }";

        Stylesheet s1 = CssParser.parse(css, false);
        assertThat(s1).isNotNull();

        // Default parse(css) is non-strict
        Stylesheet s2 = CssParser.parse(css);
        assertThat(s2).isNotNull();
    }

    @Test
    @DisplayName("strict parse succeeds on valid stylesheet")
    void strictParseSucceedsOnValidStylesheet() {
        String css = "Panel { color: red; width: 10; }";
        Stylesheet stylesheet = CssParser.parse(css, true);
        assertThat(stylesheet.rules()).hasSize(1);
    }

    @Test
    @DisplayName("diagnostic fields and toString format")
    void diagnosticFieldsAndToString() {
        String css = "/* line 1 */\n" +
                     "Panel {\n" +
                     "    color: bogus;\n" +
                     "}";

        List<CssDiagnostic> diagnostics = CssParser.validate(css);
        assertThat(diagnostics).hasSize(1);

        CssDiagnostic d = diagnostics.get(0);
        assertThat(d.selector().toCss()).isEqualTo("Panel");
        assertThat(d.line()).isEqualTo(3);
        assertThat(d.column()).isGreaterThan(0);
        assertThat(d.property()).isEqualTo("color");
        assertThat(d.rawValue()).isEqualTo("bogus");
        assertThat(d.kind().code()).isEqualTo("unconvertible");

        String str = d.toString();
        assertThat(str).contains("line 3");
        assertThat(str).contains("Panel { color: bogus }");
        assertThat(str).contains("unconvertible");
    }
}
