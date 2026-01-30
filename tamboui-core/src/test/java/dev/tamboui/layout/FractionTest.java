/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.layout;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FractionTest {

    @Test
    @DisplayName("Zero fraction")
    void zeroFraction() {
        assertThat(Fraction.ZERO.numerator()).isEqualTo(0);
        assertThat(Fraction.ZERO.denominator()).isEqualTo(1);
        assertThat(Fraction.ZERO.isZero()).isTrue();
        assertThat(Fraction.of(0, 5)).isSameAs(Fraction.ZERO);
    }

    @Test
    @DisplayName("One fraction")
    void oneFraction() {
        assertThat(Fraction.ONE.numerator()).isEqualTo(1);
        assertThat(Fraction.ONE.denominator()).isEqualTo(1);
        assertThat(Fraction.of(1)).isSameAs(Fraction.ONE);
        assertThat(Fraction.of(3, 3)).isEqualTo(Fraction.ONE);
    }

    @Test
    @DisplayName("Negative one fraction")
    void negativeOneFraction() {
        assertThat(Fraction.NEG_ONE.numerator()).isEqualTo(-1);
        assertThat(Fraction.NEG_ONE.denominator()).isEqualTo(1);
        assertThat(Fraction.of(-1)).isSameAs(Fraction.NEG_ONE);
    }

    @Test
    @DisplayName("Fractions are normalized")
    void normalized() {
        Fraction f = Fraction.of(6, 8);
        assertThat(f.numerator()).isEqualTo(3);
        assertThat(f.denominator()).isEqualTo(4);
    }

    @Test
    @DisplayName("Negative denominator is normalized")
    void negativeDenominatorNormalized() {
        Fraction f = Fraction.of(3, -4);
        assertThat(f.numerator()).isEqualTo(-3);
        assertThat(f.denominator()).isEqualTo(4);
    }

    @Test
    @DisplayName("Division by zero throws")
    void divisionByZeroThrows() {
        assertThatThrownBy(() -> Fraction.of(1, 0)).isInstanceOf(ArithmeticException.class);
    }

    @Test
    @DisplayName("Addition")
    void addition() {
        Fraction a = Fraction.of(1, 3);
        Fraction b = Fraction.of(1, 6);
        Fraction sum = a.add(b);
        assertThat(sum).isEqualTo(Fraction.of(1, 2));
    }

    @Test
    @DisplayName("Subtraction")
    void subtraction() {
        Fraction a = Fraction.of(1, 2);
        Fraction b = Fraction.of(1, 3);
        Fraction diff = a.subtract(b);
        assertThat(diff).isEqualTo(Fraction.of(1, 6));
    }

    @Test
    @DisplayName("Multiplication")
    void multiplication() {
        Fraction a = Fraction.of(2, 3);
        Fraction b = Fraction.of(3, 4);
        Fraction product = a.multiply(b);
        assertThat(product).isEqualTo(Fraction.of(1, 2));
    }

    @Test
    @DisplayName("Division")
    void division() {
        Fraction a = Fraction.of(1, 2);
        Fraction b = Fraction.of(3, 4);
        Fraction quotient = a.divide(b);
        assertThat(quotient).isEqualTo(Fraction.of(2, 3));
    }

    @Test
    @DisplayName("Division by zero fraction throws")
    void divisionByZeroFractionThrows() {
        Fraction a = Fraction.of(1, 2);
        assertThatThrownBy(() -> a.divide(Fraction.ZERO)).isInstanceOf(ArithmeticException.class);
    }

    @Test
    @DisplayName("Negation")
    void negation() {
        Fraction f = Fraction.of(3, 4);
        Fraction neg = f.negate();
        assertThat(neg).isEqualTo(Fraction.of(-3, 4));
        assertThat(neg.negate()).isEqualTo(f);
    }

    @Test
    @DisplayName("Absolute value")
    void absoluteValue() {
        assertThat(Fraction.of(-3, 4).abs()).isEqualTo(Fraction.of(3, 4));
        assertThat(Fraction.of(3, 4).abs()).isEqualTo(Fraction.of(3, 4));
        assertThat(Fraction.ZERO.abs()).isEqualTo(Fraction.ZERO);
    }

    @Test
    @DisplayName("Reciprocal")
    void reciprocal() {
        Fraction f = Fraction.of(3, 4);
        assertThat(f.reciprocal()).isEqualTo(Fraction.of(4, 3));
    }

    @Test
    @DisplayName("Reciprocal of zero throws")
    void reciprocalOfZeroThrows() {
        assertThatThrownBy(() -> Fraction.ZERO.reciprocal())
                .isInstanceOf(ArithmeticException.class);
    }

    @Test
    @DisplayName("To double")
    void toDoubleConversion() {
        assertThat(Fraction.of(1, 2).toDouble()).isEqualTo(0.5);
        assertThat(Fraction.of(1, 3).toDouble()).isCloseTo(0.333333,
                org.assertj.core.data.Offset.offset(0.00001));
    }

    @Test
    @DisplayName("To long")
    void toLongConversion() {
        assertThat(Fraction.of(7, 3).toLong()).isEqualTo(2);
        assertThat(Fraction.of(-7, 3).toLong()).isEqualTo(-2);
    }

    @Test
    @DisplayName("To int")
    void toIntConversion() {
        assertThat(Fraction.of(7, 3).toInt()).isEqualTo(2);
    }

    @Test
    @DisplayName("From double - integer")
    void fromDoubleInteger() {
        Fraction f = Fraction.fromDouble(5.0);
        assertThat(f).isEqualTo(Fraction.of(5));
    }

    @Test
    @DisplayName("From double - simple fraction")
    void fromDoubleSimple() {
        Fraction f = Fraction.fromDouble(0.5);
        assertThat(f).isEqualTo(Fraction.of(1, 2));
    }

    @Test
    @DisplayName("From double - negative")
    void fromDoubleNegative() {
        Fraction f = Fraction.fromDouble(-0.25);
        assertThat(f).isEqualTo(Fraction.of(-1, 4));
    }

    @Test
    @DisplayName("Comparison")
    void comparison() {
        Fraction a = Fraction.of(1, 3);
        Fraction b = Fraction.of(1, 2);
        Fraction c = Fraction.of(2, 6);

        assertThat(a.compareTo(b)).isLessThan(0);
        assertThat(b.compareTo(a)).isGreaterThan(0);
        assertThat(a.compareTo(c)).isEqualTo(0);
    }

    @Test
    @DisplayName("Signum")
    void signum() {
        assertThat(Fraction.of(3, 4).signum()).isEqualTo(1);
        assertThat(Fraction.of(-3, 4).signum()).isEqualTo(-1);
        assertThat(Fraction.ZERO.signum()).isEqualTo(0);
    }

    @Test
    @DisplayName("Is positive/negative")
    void positiveNegative() {
        assertThat(Fraction.of(1, 2).isPositive()).isTrue();
        assertThat(Fraction.of(1, 2).isNegative()).isFalse();
        assertThat(Fraction.of(-1, 2).isPositive()).isFalse();
        assertThat(Fraction.of(-1, 2).isNegative()).isTrue();
        assertThat(Fraction.ZERO.isPositive()).isFalse();
        assertThat(Fraction.ZERO.isNegative()).isFalse();
    }

    @Test
    @DisplayName("To string")
    void toStringRepresentation() {
        assertThat(Fraction.of(3, 4).toString()).isEqualTo("3/4");
        assertThat(Fraction.of(5).toString()).isEqualTo("5");
        assertThat(Fraction.ZERO.toString()).isEqualTo("0");
    }

    @Test
    @DisplayName("Equality and hash code")
    void equalityAndHashCode() {
        Fraction a = Fraction.of(2, 4);
        Fraction b = Fraction.of(1, 2);

        assertThat(a).isEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
    }

    @Test
    @DisplayName("Adding zero returns same instance")
    void addingZeroOptimization() {
        Fraction f = Fraction.of(3, 4);
        assertThat(f.add(Fraction.ZERO)).isSameAs(f);
        assertThat(Fraction.ZERO.add(f)).isSameAs(f);
    }

    @Test
    @DisplayName("Multiplying by one returns same instance")
    void multiplyingByOneOptimization() {
        Fraction f = Fraction.of(3, 4);
        assertThat(f.multiply(Fraction.ONE)).isSameAs(f);
        assertThat(Fraction.ONE.multiply(f)).isSameAs(f);
    }

    @Test
    @DisplayName("Third representation is exact")
    void thirdIsExact() {
        // Classic floating-point problem: 1/3 + 1/3 + 1/3 should equal 1
        Fraction third = Fraction.of(1, 3);
        Fraction sum = third.add(third).add(third);
        assertThat(sum).isEqualTo(Fraction.ONE);
    }

    @Test
    @DisplayName("Ratio preservation - layout use case")
    void ratioPreservation() {
        // Simulate layout calculation: 100 pixels split into 1:2:1 ratio
        int total = 100;
        Fraction first = Fraction.of(total).multiply(Fraction.of(1, 4));
        Fraction second = Fraction.of(total).multiply(Fraction.of(2, 4));
        Fraction third = Fraction.of(total).multiply(Fraction.of(1, 4));

        // Sum should exactly equal total
        Fraction sum = first.add(second).add(third);
        assertThat(sum).isEqualTo(Fraction.of(total));

        // Individual sizes
        assertThat(first.toInt()).isEqualTo(25);
        assertThat(second.toInt()).isEqualTo(50);
        assertThat(third.toInt()).isEqualTo(25);
    }
}
