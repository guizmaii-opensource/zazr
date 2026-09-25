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
    void booleansGiveBothValuesAsOften() {
        final List<Boolean> drawn = values(Gen.booleans(), 10_000);
        assertThat(drawn.count(b -> b)).isBetween(4_700, 5_300);
    }

    // -- ints

    @Test
    void integersStayInRangeAndReachBothEnds() {
        assertThat(values(Gen.integers(0, 10))).allMatch(i -> i >= 0 && i <= 10).contains(0, 10);
        assertThat(values(Gen.integers(-5, 5))).allMatch(i -> i >= -5 && i <= 5).contains(-5, 5);
        assertThat(values(Gen.integers(Integer.MIN_VALUE, Integer.MAX_VALUE))).contains(Integer.MIN_VALUE, Integer.MAX_VALUE);
        assertThat(values(Gen.integers())).contains(Integer.MIN_VALUE, Integer.MAX_VALUE, -1, 0, 1);
    }

    @Test
    void integersFavourTheEdges() {
        final List<Integer> drawn = values(Gen.integers(-1_000_000, 1_000_000), 10_000);
        assertThat(drawn).contains(-1_000_000, -999_999, -1, 0, 1, 999_999, 1_000_000);
        final int edges = drawn.count(i -> i == -1_000_000 || i == -999_999 || i == -1 || i == 0 || i == 1 || i == 999_999 || i == 1_000_000);
        assertThat(edges).isBetween(4_700, 5_300);
        // the other half is uniform
        assertThat(drawn.filter(i -> i > 1 && i < 999_999).size()).isBetween(2_200, 2_800);
    }

    @Test
    void integersOfAnEdgeLessRangeStayInIt() {
        assertThat(values(Gen.integers(100, 101))).containsOnly(100, 101);
        assertThat(values(Gen.integers(Integer.MAX_VALUE - 1, Integer.MAX_VALUE))).containsOnly(Integer.MAX_VALUE - 1, Integer.MAX_VALUE);
        assertThat(values(Gen.integers(Integer.MIN_VALUE, Integer.MIN_VALUE + 1))).containsOnly(Integer.MIN_VALUE, Integer.MIN_VALUE + 1);
    }

    @Test
    void integersOfOneValueAreConstant() {
        assertThat(values(Gen.integers(7, 7))).containsOnly(7);
        assertThat(Gen.integers(7, 7).runCollect(CheckConfig.defaults())).isEqualTo(List.of(7));
    }

    @Test
    void integersRejectReversedBounds() {
        assertThatThrownBy(() -> Gen.integers(1, 0)).isInstanceOf(IllegalArgumentException.class).hasMessage("min 1 > max 0");
    }

    // -- longs

    @Test
    void longsStayInRangeAndReachBothEnds() {
        assertThat(values(Gen.longs(0, 10))).allMatch(i -> i >= 0 && i <= 10).contains(0L, 10L);
        assertThat(values(Gen.longs(Long.MIN_VALUE, Long.MAX_VALUE))).contains(Long.MIN_VALUE, Long.MAX_VALUE);
        assertThat(values(Gen.longs())).contains(Long.MIN_VALUE, Long.MAX_VALUE, -1L, 0L, 1L);
        assertThat(values(Gen.longs(Long.MIN_VALUE + 5, Long.MAX_VALUE))).allMatch(l -> l >= Long.MIN_VALUE + 5);
        assertThat(values(Gen.longs(Long.MIN_VALUE, Long.MAX_VALUE - 5))).allMatch(l -> l <= Long.MAX_VALUE - 5);
        assertThat(values(Gen.longs(3, 3))).containsOnly(3L);
        assertThatThrownBy(() -> Gen.longs(1, 0)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void longsAreUniformOutsideTheEdges() {
        final List<Long> drawn = values(Gen.longs(Long.MIN_VALUE + 5, Long.MAX_VALUE), 10_000);
        assertThat(drawn.count(l -> l > 1 && l < Long.MAX_VALUE - 1)).isBetween(2_200, 2_800);
        final List<Long> full = values(Gen.longs(Long.MIN_VALUE, Long.MAX_VALUE), 10_000);
        assertThat(full.count(l -> l > 1 && l < Long.MAX_VALUE - 1)).isBetween(2_200, 2_800);
    }

    // -- doubles

    @Test
    void doublesAreBetweenZeroAndOne() {
        final List<Double> drawn = values(Gen.doubles(), 5_000);
        assertThat(drawn).allMatch(d -> d >= 0.0 && d < 1.0);
        assertThat(drawn.average().get()).isBetween(0.45, 0.55);
    }

    @Test
    void doublesStayInRangeAndReachTheEdges() {
        final List<Double> drawn = values(Gen.doubles(-2.0, 3.0), 2_000);
        assertThat(drawn).allMatch(d -> d >= -2.0 && d <= 3.0)
                .contains(-2.0, Math.nextUp(-2.0), -1.0, -Double.MIN_VALUE, -0.0, 0.0, Double.MIN_VALUE, 1.0, Math.nextDown(3.0), 3.0);
        assertThat(values(Gen.doubles(0.5, 0.75), 500)).allMatch(d -> d >= 0.5 && d <= 0.75).contains(0.5, 0.75);
        assertThat(values(Gen.doubles(-Double.MAX_VALUE, Double.MAX_VALUE), 500)).allMatch(Double::isFinite)
                .contains(-Double.MAX_VALUE, Double.MAX_VALUE);
        assertThat(values(Gen.doubles(1.5, 1.5))).containsOnly(1.5);
    }

    @Test
    void doublesRejectInvalidBounds() {
        assertThatThrownBy(() -> Gen.doubles(1.0, 0.0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Gen.doubles(Double.NaN, 0.0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Gen.doubles(0.0, Double.NaN)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Gen.doubles(Double.NEGATIVE_INFINITY, 0.0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Gen.doubles(0.0, Double.POSITIVE_INFINITY)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Gen.doubles(0.0, -0.0)).isInstanceOf(IllegalArgumentException.class);
    }

    // -- chars

    @Test
    void charsStayInRange() {
        assertThat(values(Gen.chars('a', 'e'))).containsOnly('a', 'b', 'c', 'd', 'e');
        assertThat(values(Gen.chars())).contains(Character.MIN_VALUE, Character.MAX_VALUE);
        assertThatThrownBy(() -> Gen.chars('b', 'a')).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void theCharacterClassesHoldTheirCharacters() {
        assertThat(values(Gen.alphaChars(), 5_000)).allMatch(c -> (c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z'))
                .contains('A', 'Z', 'a', 'z').doesNotContain('0');
        final List<Character> alphaNumeric = values(Gen.alphaNumericChars(), 5_000);
        assertThat(alphaNumeric).allMatch(c -> Character.isLetterOrDigit(c) && c < 128);
        assertThat(alphaNumeric.distinct().size()).isEqualTo(62);
        assertThat(values(Gen.numericChars(), 500)).containsOnly('0', '1', '2', '3', '4', '5', '6', '7', '8', '9');
        assertThat(values(Gen.asciiChars(), 500)).allMatch(c -> c <= 127).contains('\u0000', '\u007F');
        assertThat(values(Gen.printableChars(), 500)).allMatch(c -> c >= '!' && c <= '~').contains('!', '~');
    }

    @Test
    void unicodeCharsHaveNoSurrogate() {
        final List<Character> drawn = values(Gen.unicodeChars(), 5_000);
        assertThat(drawn).noneMatch(Character::isSurrogate).allMatch(c -> c != '￾' && c != '￿')
                .contains('\u0000', '퟿', '', '�');
    }

    // -- strings

    @Test
    void stringLengthsReachFromZeroToTheSize() {
        final List<String> drawn = Gen.strings(Gen.alphaChars()).withSize(20).runCollectN(500, new CheckConfig(500, 100, 3L, 1000));
        assertThat(drawn).allMatch(s -> s.length() <= 20 && s.chars().allMatch(Character::isLetter));
        assertThat(drawn.map(String::length)).contains(0, 1, 19, 20);
    }

    @Test
    void stringLengthsGrowWithTheSizeOfTheRun() {
        final List<String> drawn = values(Gen.strings());
        assertThat(drawn.head()).isEmpty();
        assertThat(drawn.zipWithIndex()).allMatch(t -> t._1().length() <= Runner.size(new CheckConfig(200, 100, 17L, 1000), t._2()));
        assertThat(drawn).allMatch(s -> s.chars().noneMatch(c -> Character.isSurrogate((char) c)));
    }

    @Test
    void stringsNHaveTheirLength() {
        assertThat(values(Gen.stringsN(5, Gen.numericChars()))).allMatch(s -> s.length() == 5 && s.chars().allMatch(Character::isDigit));
        assertThat(values(Gen.stringsN(0, Gen.numericChars()))).containsOnly("");
        assertThatThrownBy(() -> Gen.stringsN(-1, Gen.numericChars())).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Gen.stringsN(1, null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> Gen.strings(null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> values(Gen.stringsN(1, Gen.constant(null)))).isInstanceOf(NullPointerException.class);
    }

    @Test
    void alphaNumericStringsHoldDigitsAndLetters() {
        assertThat(values(Gen.alphaNumericStrings())).allMatch(s -> s.matches("[0-9A-Za-z]*")).anyMatch(s -> s.length() > 10);
    }

    // -- date-times

    @Test
    void localDateTimesStayInRange() {
        final LocalDateTime min = LocalDateTime.of(2020, 1, 1, 0, 0, 0, 500);
        final LocalDateTime max = LocalDateTime.of(2020, 1, 2, 0, 0, 0, 100);
        final List<LocalDateTime> drawn = values(Gen.localDateTimes(min, max), 2_000);
        assertThat(drawn).allMatch(d -> !d.isBefore(min) && !d.isAfter(max));
        // the first and the last seconds are reached, and the bounds themselves once the nanosecond is clamped
        assertThat(drawn).anyMatch(d -> d.toEpochSecond(ZoneOffset.UTC) == min.toEpochSecond(ZoneOffset.UTC))
                .anyMatch(d -> d.toEpochSecond(ZoneOffset.UTC) == max.toEpochSecond(ZoneOffset.UTC))
                .contains(min, max);
        assertThat(values(Gen.localDateTimes(min, min))).containsOnly(min);
    }

    @Test
    void localDateTimesCoverTheWholeRange() {
        assertThat(values(Gen.localDateTimes(), 2_000)).contains(LocalDateTime.MIN, LocalDateTime.MAX);
    }

    @Test
    void localDateTimesRejectInvalidBounds() {
        final LocalDateTime now = LocalDateTime.of(2020, 1, 1, 0, 0);
        assertThatThrownBy(() -> Gen.localDateTimes(now, now.minusNanos(1))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Gen.localDateTimes(null, now)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> Gen.localDateTimes(now, null)).isInstanceOf(NullPointerException.class);
    }
}
