package com.guizmaii.zazr.test;

import com.guizmaii.zazr.collection.List;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The scalar generators: ranges, edges, distributions and characters.
 */
class GenScalarsTest {

    private static <A> List<A> values(Gen<A> gen, int n) {
        return gen.runCollectN(n, new CheckConfig(n, 100, 17L, 1000));
    }

    private static <A> List<A> values(Gen<A> gen) {
        return values(gen, 200);
    }

    // -- booleans

    @Test
    void booleanValueGivesBothValuesAsOften() {
        final List<Boolean> drawn = values(Gen.booleanValue(), 10_000);
        assertThat(drawn.count(b -> b)).isBetween(4_700, 5_300);
    }

    // -- ints

    @Test
    void intValueStaysInRangeAndReachesBothEnds() {
        assertThat(values(Gen.intValue(0, 10))).allMatch(i -> i >= 0 && i <= 10).contains(0, 10);
        assertThat(values(Gen.intValue(-5, 5))).allMatch(i -> i >= -5 && i <= 5).contains(-5, 5);
        assertThat(values(Gen.intValue(Integer.MIN_VALUE, Integer.MAX_VALUE))).contains(Integer.MIN_VALUE, Integer.MAX_VALUE);
        assertThat(values(Gen.intValue())).contains(Integer.MIN_VALUE, Integer.MAX_VALUE, -1, 0, 1);
    }

    @Test
    void intValueFavoursTheEdges() {
        final List<Integer> drawn = values(Gen.intValue(-1_000_000, 1_000_000), 10_000);
        assertThat(drawn).contains(-1_000_000, -999_999, -1, 0, 1, 999_999, 1_000_000);
        final int edges = drawn.count(i -> i == -1_000_000 || i == -999_999 || i == -1 || i == 0 || i == 1 || i == 999_999 || i == 1_000_000);
        assertThat(edges).isBetween(4_700, 5_300);
        // the other half is uniform
        assertThat(drawn.filter(i -> i > 1 && i < 999_999).size()).isBetween(2_200, 2_800);
    }

    @Test
    void intValueOfAnEdgeLessRangeStaysInIt() {
        assertThat(values(Gen.intValue(100, 101))).containsOnly(100, 101);
        assertThat(values(Gen.intValue(Integer.MAX_VALUE - 1, Integer.MAX_VALUE))).containsOnly(Integer.MAX_VALUE - 1, Integer.MAX_VALUE);
        assertThat(values(Gen.intValue(Integer.MIN_VALUE, Integer.MIN_VALUE + 1))).containsOnly(Integer.MIN_VALUE, Integer.MIN_VALUE + 1);
    }

    @Test
    void intValueOfOneValueIsConstant() {
        assertThat(values(Gen.intValue(7, 7))).containsOnly(7);
        assertThat(Gen.intValue(7, 7).runCollect(CheckConfig.defaults())).isEqualTo(List.of(7));
    }

    @Test
    void intValueRejectsReversedBounds() {
        assertThatThrownBy(() -> Gen.intValue(1, 0)).isInstanceOf(IllegalArgumentException.class).hasMessage("min 1 > max 0");
    }

    // -- longs

    @Test
    void longValueStaysInRangeAndReachesBothEnds() {
        assertThat(values(Gen.longValue(0, 10))).allMatch(i -> i >= 0 && i <= 10).contains(0L, 10L);
        assertThat(values(Gen.longValue(Long.MIN_VALUE, Long.MAX_VALUE))).contains(Long.MIN_VALUE, Long.MAX_VALUE);
        assertThat(values(Gen.longValue())).contains(Long.MIN_VALUE, Long.MAX_VALUE, -1L, 0L, 1L);
        assertThat(values(Gen.longValue(Long.MIN_VALUE + 5, Long.MAX_VALUE))).allMatch(l -> l >= Long.MIN_VALUE + 5);
        assertThat(values(Gen.longValue(Long.MIN_VALUE, Long.MAX_VALUE - 5))).allMatch(l -> l <= Long.MAX_VALUE - 5);
        assertThat(values(Gen.longValue(3, 3))).containsOnly(3L);
        assertThatThrownBy(() -> Gen.longValue(1, 0)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void longValueIsUniformOutsideTheEdges() {
        final List<Long> drawn = values(Gen.longValue(Long.MIN_VALUE + 5, Long.MAX_VALUE), 10_000);
        assertThat(drawn.count(l -> l > 1 && l < Long.MAX_VALUE - 1)).isBetween(2_200, 2_800);
        final List<Long> full = values(Gen.longValue(Long.MIN_VALUE, Long.MAX_VALUE), 10_000);
        assertThat(full.count(l -> l > 1 && l < Long.MAX_VALUE - 1)).isBetween(2_200, 2_800);
    }

    // -- doubles

    @Test
    void doubleValueIsBetweenZeroAndOne() {
        final List<Double> drawn = values(Gen.doubleValue(), 5_000);
        assertThat(drawn).allMatch(d -> d >= 0.0 && d < 1.0);
        assertThat(drawn.average().get()).isBetween(0.45, 0.55);
    }

    @Test
    void doubleValueStaysInRangeAndReachesTheEdges() {
        final List<Double> drawn = values(Gen.doubleValue(-2.0, 3.0), 2_000);
        assertThat(drawn).allMatch(d -> d >= -2.0 && d <= 3.0)
                .contains(-2.0, Math.nextUp(-2.0), -1.0, -Double.MIN_VALUE, -0.0, 0.0, Double.MIN_VALUE, 1.0, Math.nextDown(3.0), 3.0);
        assertThat(values(Gen.doubleValue(0.5, 0.75), 500)).allMatch(d -> d >= 0.5 && d <= 0.75).contains(0.5, 0.75);
        assertThat(values(Gen.doubleValue(-Double.MAX_VALUE, Double.MAX_VALUE), 500)).allMatch(Double::isFinite)
                .contains(-Double.MAX_VALUE, Double.MAX_VALUE);
        assertThat(values(Gen.doubleValue(1.5, 1.5))).containsOnly(1.5);
    }

    @Test
    void doubleValueRejectsInvalidBounds() {
        assertThatThrownBy(() -> Gen.doubleValue(1.0, 0.0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Gen.doubleValue(Double.NaN, 0.0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Gen.doubleValue(0.0, Double.NaN)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Gen.doubleValue(Double.NEGATIVE_INFINITY, 0.0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Gen.doubleValue(0.0, Double.POSITIVE_INFINITY)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Gen.doubleValue(0.0, -0.0)).isInstanceOf(IllegalArgumentException.class);
    }

    // -- chars

    @Test
    void charValueStaysInRange() {
        assertThat(values(Gen.charValue('a', 'e'))).containsOnly('a', 'b', 'c', 'd', 'e');
        assertThat(values(Gen.charValue())).contains(Character.MIN_VALUE, Character.MAX_VALUE);
        assertThatThrownBy(() -> Gen.charValue('b', 'a')).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void theCharacterClassesHoldTheirCharacters() {
        assertThat(values(Gen.alphaChar(), 5_000)).allMatch(c -> (c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z'))
                .contains('A', 'Z', 'a', 'z').doesNotContain('0');
        final List<Character> alphaNumeric = values(Gen.alphaNumericChar(), 5_000);
        assertThat(alphaNumeric).allMatch(c -> Character.isLetterOrDigit(c) && c < 128);
        assertThat(alphaNumeric.distinct().size()).isEqualTo(62);
        assertThat(values(Gen.numericChar(), 500)).containsOnly('0', '1', '2', '3', '4', '5', '6', '7', '8', '9');
        assertThat(values(Gen.asciiChar(), 500)).allMatch(c -> c <= 127).contains('\u0000', '\u007F');
        assertThat(values(Gen.printableChar(), 500)).allMatch(c -> c >= '!' && c <= '~').contains('!', '~');
    }

    @Test
    void unicodeCharHasNoSurrogate() {
        final List<Character> drawn = values(Gen.unicodeChar(), 5_000);
        assertThat(drawn).noneMatch(Character::isSurrogate).allMatch(c -> c != '￾' && c != '￿')
                .contains('\u0000', '퟿', '', '�');
    }

    // -- strings

    @Test
    void stringLengthsReachFromZeroToTheSize() {
        final List<String> drawn = Gen.string(Gen.alphaChar()).withSize(20).runCollectN(500, new CheckConfig(500, 100, 3L, 1000));
        assertThat(drawn).allMatch(s -> s.length() <= 20 && s.chars().allMatch(Character::isLetter));
        assertThat(drawn.map(String::length)).contains(0, 1, 19, 20);
    }

    @Test
    void stringLengthsGrowWithTheSizeOfTheRun() {
        final List<String> drawn = values(Gen.string());
        assertThat(drawn.head()).isEmpty();
        assertThat(drawn.zipWithIndex()).allMatch(t -> t._1().length() <= Runner.size(new CheckConfig(200, 100, 17L, 1000), t._2()));
        assertThat(drawn).allMatch(s -> s.chars().noneMatch(c -> Character.isSurrogate((char) c)));
    }

    @Test
    void stringNHasItsLength() {
        assertThat(values(Gen.stringN(5, Gen.numericChar()))).allMatch(s -> s.length() == 5 && s.chars().allMatch(Character::isDigit));
        assertThat(values(Gen.stringN(0, Gen.numericChar()))).containsOnly("");
        assertThatThrownBy(() -> Gen.stringN(-1, Gen.numericChar())).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Gen.stringN(1, null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> Gen.string(null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> values(Gen.stringN(1, Gen.constant(null)))).isInstanceOf(NullPointerException.class);
    }

    @Test
    void alphaNumericStringHoldsDigitsAndLetters() {
        assertThat(values(Gen.alphaNumericString())).allMatch(s -> s.matches("[0-9A-Za-z]*")).anyMatch(s -> s.length() > 10);
    }

    // -- date-times

    @Test
    void localDateTimeStaysInRange() {
        final LocalDateTime min = LocalDateTime.of(2020, 1, 1, 0, 0, 0, 500);
        final LocalDateTime max = LocalDateTime.of(2020, 1, 2, 0, 0, 0, 100);
        final List<LocalDateTime> drawn = values(Gen.localDateTime(min, max), 2_000);
        assertThat(drawn).allMatch(d -> !d.isBefore(min) && !d.isAfter(max));
        // the first and the last seconds are reached, and the bounds themselves once the nanosecond is clamped
        assertThat(drawn).anyMatch(d -> d.toEpochSecond(ZoneOffset.UTC) == min.toEpochSecond(ZoneOffset.UTC))
                .anyMatch(d -> d.toEpochSecond(ZoneOffset.UTC) == max.toEpochSecond(ZoneOffset.UTC))
                .contains(min, max);
        assertThat(values(Gen.localDateTime(min, min))).containsOnly(min);
    }

    @Test
    void localDateTimeCoversTheWholeRange() {
        assertThat(values(Gen.localDateTime(), 2_000)).contains(LocalDateTime.MIN, LocalDateTime.MAX);
    }

    @Test
    void localDateTimeRejectsInvalidBounds() {
        final LocalDateTime now = LocalDateTime.of(2020, 1, 1, 0, 0);
        assertThatThrownBy(() -> Gen.localDateTime(now, now.minusNanos(1))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Gen.localDateTime(null, now)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> Gen.localDateTime(now, null)).isInstanceOf(NullPointerException.class);
    }
}
