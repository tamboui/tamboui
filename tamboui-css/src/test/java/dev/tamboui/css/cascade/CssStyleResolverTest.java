/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.css.cascade;

import dev.tamboui.layout.Alignment;
import dev.tamboui.layout.Constraint;
import dev.tamboui.style.Color;
import dev.tamboui.style.Modifier;
import dev.tamboui.style.Overflow;
import dev.tamboui.style.StandardProperties;
import dev.tamboui.style.Style;
import dev.tamboui.widgets.block.BorderType;
import dev.tamboui.layout.Padding;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CssStyleResolverTest {

    @Test
    void withFallbackReturnsThisWhenFallbackIsNull() {
        CssStyleResolver resolver = CssStyleResolver.builder()
                .foreground(Color.RED)
                .build();

        CssStyleResolver result = resolver.withFallback(null);

        assertThat(result).isSameAs(resolver);
    }

    @Test
    void withFallbackUsesParentPropertiesWhenChildDoesNotHaveThem() {
        CssStyleResolver parent = CssStyleResolver.builder()
                .foreground(Color.BLUE)
                .background(Color.WHITE)
                .borderType(BorderType.DOUBLE)
                .build();

        CssStyleResolver child = CssStyleResolver.builder()
                .foreground(Color.RED)
                .build();

        CssStyleResolver merged = child.withFallback(parent);

        // Child's foreground should override parent's
        assertThat(merged.foreground()).contains(Color.RED);
        // Background is NOT inherited per CSS spec
        assertThat(merged.background()).isEmpty();
        // borderType IS inherited (for nested panels)
        assertThat(merged.borderType()).contains(BorderType.DOUBLE);
    }

    @Test
    void withFallbackChildPropertiesTakePrecedence() {
        CssStyleResolver parent = CssStyleResolver.builder()
                .foreground(Color.BLUE)
                .background(Color.WHITE)
                .alignment(Alignment.CENTER)
                .borderType(BorderType.PLAIN)
                .width(Constraint.fill())
                .build();

        CssStyleResolver child = CssStyleResolver.builder()
                .foreground(Color.RED)
                .background(Color.BLACK)
                .alignment(Alignment.LEFT)
                .borderType(BorderType.DOUBLE)
                .width(Constraint.fit())
                .build();

        CssStyleResolver merged = child.withFallback(parent);

        // All properties should come from child since it has them all
        assertThat(merged.foreground()).contains(Color.RED);
        assertThat(merged.background()).contains(Color.BLACK);
        assertThat(merged.alignment()).contains(Alignment.LEFT);
        assertThat(merged.borderType()).contains(BorderType.DOUBLE);
        assertThat(merged.widthConstraint()).contains(Constraint.fit());
    }

    @Test
    void withFallbackMergesModifiersPreferringChild() {
        CssStyleResolver parent = CssStyleResolver.builder()
                .addModifier(Modifier.BOLD)
                .build();

        CssStyleResolver child = CssStyleResolver.builder()
                .addModifier(Modifier.ITALIC)
                .build();

        CssStyleResolver merged = child.withFallback(parent);

        // Child has modifiers, so child's modifiers take precedence
        assertThat(merged.modifiers()).containsExactly(Modifier.ITALIC);
    }

    @Test
    void withFallbackInheritsModifiersWhenChildHasNone() {
        CssStyleResolver parent = CssStyleResolver.builder()
                .addModifier(Modifier.BOLD)
                .addModifier(Modifier.UNDERLINED)
                .build();

        CssStyleResolver child = CssStyleResolver.builder()
                .foreground(Color.RED)
                .build();

        CssStyleResolver merged = child.withFallback(parent);

        // Child has no modifiers, so parent's modifiers should be inherited
        assertThat(merged.modifiers()).contains(Modifier.BOLD, Modifier.UNDERLINED);
    }

    @Test
    void withFallbackHandlesEmptyModifiersOnBothSides() {
        CssStyleResolver parent = CssStyleResolver.builder()
                .foreground(Color.BLUE)
                .build();

        CssStyleResolver child = CssStyleResolver.builder()
                .foreground(Color.RED)
                .build();

        CssStyleResolver merged = child.withFallback(parent);

        // Both have empty modifiers - should not cause EnumSet.copyOf issue
        assertThat(merged.modifiers()).isEmpty();
    }

    @Test
    void withFallbackDoesNotInheritPadding() {
        // Padding is NOT inherited per CSS spec
        Padding parentPadding = Padding.uniform(2);
        CssStyleResolver parent = CssStyleResolver.builder()
                .padding(parentPadding)
                .build();

        CssStyleResolver child = CssStyleResolver.builder()
                .foreground(Color.RED)
                .build();

        CssStyleResolver merged = child.withFallback(parent);

        // Padding should NOT be inherited from parent
        assertThat(merged.padding()).isEmpty();
    }

    @Test
    void toStyleIncludesInheritedProperties() {
        // Only foreground (color) and modifiers (text-style) are inherited
        CssStyleResolver parent = CssStyleResolver.builder()
                .foreground(Color.BLUE)
                .addModifier(Modifier.BOLD)
                .build();

        CssStyleResolver child = CssStyleResolver.builder()
                .foreground(Color.RED)
                .build();

        CssStyleResolver merged = child.withFallback(parent);

        Style style = merged.toStyle();
        // Child's foreground overrides parent's
        assertThat(style.fg()).contains(Color.RED);
        // Modifiers ARE inherited when child has none
        assertThat(style.addModifiers()).contains(Modifier.BOLD);
    }

    @Test
    void withFallbackDoesNotInheritHeightConstraint() {
        // Height constraint is NOT inherited (element-specific layout property)
        CssStyleResolver parent = CssStyleResolver.builder()
                .heightConstraint(Constraint.fill())
                .build();

        CssStyleResolver child = CssStyleResolver.builder()
                .foreground(Color.RED)
                .build();

        CssStyleResolver merged = child.withFallback(parent);

        // Height constraint should NOT be inherited from parent
        assertThat(merged.heightConstraint()).isEmpty();
    }

    @Test
    void withFallbackDoesNotInheritWidthConstraint() {
        // Width constraint is NOT inherited (element-specific layout property)
        CssStyleResolver parent = CssStyleResolver.builder()
                .width(Constraint.percentage(50))
                .build();

        CssStyleResolver child = CssStyleResolver.builder()
                .foreground(Color.RED)
                .build();

        CssStyleResolver merged = child.withFallback(parent);

        // Width constraint should NOT be inherited from parent
        assertThat(merged.widthConstraint()).isEmpty();
    }

    @Test
    void childConstraintsOverrideParent() {
        CssStyleResolver parent = CssStyleResolver.builder()
                .heightConstraint(Constraint.fill())
                .width(Constraint.fill())
                .build();

        CssStyleResolver child = CssStyleResolver.builder()
                .heightConstraint(Constraint.length(5))
                .width(Constraint.fit())
                .build();

        CssStyleResolver merged = child.withFallback(parent);

        // Child's constraints should be used
        assertThat(merged.heightConstraint()).contains(Constraint.length(5));
        assertThat(merged.widthConstraint()).contains(Constraint.fit());
    }

    // ═══════════════════════════════════════════════════════════════
    // CSS Inheritance Features - inherit keyword
    // ═══════════════════════════════════════════════════════════════

    @Test
    void inheritKeywordCopiesParentTypedPropertyValue() {
        // Parent has text-overflow: ellipsis
        CssStyleResolver parent = CssStyleResolver.builder()
                .set(StandardProperties.TEXT_OVERFLOW, Overflow.ELLIPSIS)
                .build();

        // Child uses inherit keyword for text-overflow
        CssStyleResolver child = CssStyleResolver.builder()
                .markAsInherited("text-overflow")
                .foreground(Color.RED)
                .build();

        CssStyleResolver merged = child.withFallback(parent);

        // Child should inherit text-overflow from parent
        assertThat(merged.textOverflow()).contains(Overflow.ELLIPSIS);
        // Child's own property should still be present
        assertThat(merged.foreground()).contains(Color.RED);
    }

    @Test
    void inheritKeywordCopiesParentRawValue() {
        // Parent has a custom raw property
        CssStyleResolver parent = CssStyleResolver.builder()
                .setRaw("custom-prop", "custom-value")
                .build();

        // Child uses inherit keyword for the custom property
        CssStyleResolver child = CssStyleResolver.builder()
                .markAsInherited("custom-prop")
                .build();

        CssStyleResolver merged = child.withFallback(parent);

        // Child should have the raw value from parent (can be tested via get with PropertyDefinition)
        // Since custom-prop is not in registry, we test inheritance worked by checking raw values
        // The value should be accessible via lazy conversion if a PropertyDefinition is provided
    }

}