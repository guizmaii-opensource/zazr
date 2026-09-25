package com.guizmaii.zazr.test.legacy;

import com.guizmaii.zazr.Tuple;
import com.guizmaii.zazr.Tuple2;
import com.guizmaii.zazr.collection.List;
import com.guizmaii.zazr.collection.Stream;
import java.util.Arrays;
import java.util.Random;
import java.util.function.Predicate;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class GenTest {

    // equally distributed random number generator
    static final Random RANDOM = new Random();

    // number of tries to assert a property
    static final int TRIES = 1000;

    // -- of

    @Test
    public void shouldIntersperseMultipleGeneratos() throws Exception {
        Gen<Integer> gen = Gen.of(0).intersperse(Gen.of(1));
        assertThat(gen.apply(RANDOM)).isEqualTo(0);
        assertThat(gen.apply(RANDOM)).isEqualTo(1);
        assertThat(gen.apply(RANDOM)).isEqualTo(0);
        assertThat(gen.apply(RANDOM)).isEqualTo(1);
    }
    @Test
    public void shouldCreateConstantGenOfElement() {
        final Gen<Integer> gen = Gen.of(1);
        assertThat(gen.apply(RANDOM)).isEqualTo(1);
        assertThat(gen.apply(RANDOM)).isEqualTo(1);
        assertThat(gen.apply(RANDOM)).isEqualTo(1);
    }

    @Test
    public void shouldCreateGenOfSeedAndFunction() {
        final Gen<Integer> gen = Gen.of(1, i -> i + 1);
        assertThat(gen.apply(RANDOM)).isEqualTo(1);
        assertThat(gen.apply(RANDOM)).isEqualTo(2);
        assertThat(gen.apply(RANDOM)).isEqualTo(3);
    }

    @Test
    public void shouldThrowWhenSeedOrFunctionIsNull() {
        assertThrows(NullPointerException.class, () -> Gen.of(null, i -> 1));
        assertThrows(NullPointerException.class, () -> Gen.<Integer>of(1, null));
    }

    @Test
    public void shouldThrowWhenTheFunctionReturnsNull() {
        final Gen<Integer> gen = Gen.of(1, i -> null);
        assertThat(gen.apply(RANDOM)).isEqualTo(1);
        assertThrows(NullPointerException.class, () -> gen.apply(RANDOM));
    }

    @Test
    public void shouldThrowWhenInterspersedGenIsNull() {
        assertThrows(NullPointerException.class, () -> Gen.of(0).intersperse(null));
    }

    // -- random number generator (rng)

    @Test
    public void shouldUseCustomRandomNumberGenerator() {
        final Random rng = new Random() {
            private static final long serialVersionUID = 1L;

            @Override
            public int nextInt(int bound) {
                return 0;
            }
        };
        final Gen<Integer> gen = Gen.choose(1, 2);
        final Number actual = Stream.continually(() -> gen.apply(rng)).take(10).sum();
        assertThat(actual).isEqualTo(10L);
    }

    // -- choose(int, int)

    @Test
    public void shouldChooseIntBetweenMinMax() {
        assertForAll(() -> Gen.choose(-1, 1).apply(RANDOM), i -> i >= -1 && i <= 1);
    }

    @Test
    public void shouldChooseIntWhenMinEqualsMax() {
        assertForAll(() -> Gen.choose(0, 0).apply(RANDOM), i -> i == 0);
    }

    @Test
    public void shouldChooseIntWhenRangeCrossesIntegerBoundary() {
        assertForAll(() -> Gen.choose(Integer.MIN_VALUE, 0).apply(RANDOM), i -> i >= Integer.MIN_VALUE && i <= 0);
    }

    @Test
    public void shouldChooseIntBetweenFullIntRange() {
        assertForAllDiverse(() -> Gen.choose(Integer.MIN_VALUE, Integer.MAX_VALUE).apply(RANDOM),
                i -> i >= Integer.MIN_VALUE && i <= Integer.MAX_VALUE);
    }

    @Test
    public void shouldChooseIntWhenBoundsAreReversed() {
        assertForAll(() -> Gen.choose(1, -1).apply(RANDOM), i -> i >= -1 && i <= 1);
    }

    @Test
    public void shouldFavorIntEdgesWhileSamplingTheFullRange() {
        assertEdgeCoverage(Gen.choose(-10000, 10000), List.of(-10000, -9999, -1, 0, 1, 9999, 10000));
    }

    @Test
    public void shouldChooseAcrossWideIntRangesWithoutOverflow() {
        final int[][] ranges = {
                { Integer.MIN_VALUE, Integer.MAX_VALUE },
                { Integer.MIN_VALUE, 0 },
                { -1, Integer.MAX_VALUE },
                { Integer.MIN_VALUE, Integer.MIN_VALUE + 10 },
                { Integer.MAX_VALUE - 10, Integer.MAX_VALUE }
        };
        for (int[] range : ranges) {
            final List<Integer> values = samples(Gen.choose(range[0], range[1]));
            assertThat(values).allMatch(i -> i >= range[0] && i <= range[1]);
            assertThat(values).contains(range[0], range[1]);
            assertThat(samples(Gen.choose(range[1], range[0]))).isEqualTo(values);
        }
    }

    // -- choose(long, long)

    @Test
    public void shouldChooseLongBetweenMinMax() {
        assertForAll(() -> Gen.choose(-1L, 1L).apply(RANDOM), l -> l >= -1L && l <= 1L);
    }

    @Test
    public void shouldChooseLongWhenMinEqualsMax() {
        assertForAll(() -> Gen.choose(0L, 0L).apply(RANDOM), l -> l == 0L);
    }

    @Test
    public void shouldChooseLongWhenRangeCrossesLongBoundary() {
        assertForAll(() -> Gen.choose(Long.MIN_VALUE, 0L).apply(RANDOM), l -> l >= Long.MIN_VALUE && l <= 0L);
    }

    @Test
    public void shouldChooseLongBetweenFullLongRange() {
        assertForAllDiverse(() -> Gen.choose(Long.MIN_VALUE, Long.MAX_VALUE).apply(RANDOM),
                l -> l >= Long.MIN_VALUE && l <= Long.MAX_VALUE);
    }

    @Test
    public void shouldFavorLongEdgesWhileSamplingTheFullRange() {
        assertEdgeCoverage(Gen.choose(Long.MIN_VALUE, Long.MAX_VALUE),
                List.of(Long.MIN_VALUE, Long.MIN_VALUE + 1, -1L, 0L, 1L, Long.MAX_VALUE - 1, Long.MAX_VALUE));
    }

    @Test
    public void shouldChooseAcrossWideLongRangesWithoutOverflow() {
        final long[][] ranges = {
                { Long.MIN_VALUE, Long.MAX_VALUE },
                { Long.MIN_VALUE, 0 },
                { -1, Long.MAX_VALUE },
                { 0, Long.MAX_VALUE - 1 },
                { Long.MIN_VALUE, Long.MIN_VALUE + 10 },
                { Long.MAX_VALUE - 10, Long.MAX_VALUE }
        };
        for (long[] range : ranges) {
            final List<Long> values = samples(Gen.choose(range[0], range[1]));
            assertThat(values).allMatch(l -> l >= range[0] && l <= range[1]);
            assertThat(values).contains(range[0], range[1]);
            assertThat(samples(Gen.choose(range[1], range[0]))).isEqualTo(values);
        }
    }

    @Test
    public void shouldReachEveryLongInNarrowRangesBeyondDoublePrecision() {
        for (long min : new long[] { Long.MIN_VALUE, 1L << 53, Long.MAX_VALUE - 10 }) {
            assertThat(samples(Gen.choose(min, min + 10))).containsAll(List.rangeClosed(min, min + 10));
        }
    }

    // -- choose(double, double)

    @Test
    public void shouldChooseDoubleBetweenMinMax() {
        assertForAll(() -> Gen.choose(-1.0d, 1.0d).apply(RANDOM), d -> d >= -1.0d && d <= 1.0d);
    }

    @Test
    public void shouldChooseDoubleWhenMinEqualsMax() {
        assertForAll(() -> Gen.choose(0.0d, 0.0d).apply(RANDOM), d -> d == 0.0d);
    }

    @Test
    public void shouldFavorDoubleEdgesWhileSamplingTheFullRange() {
        assertEdgeCoverage(Gen.choose(-10000.0, 10000.0), List.of(-10000.0, Math.nextUp(-10000.0),
                -1.0, -Double.MIN_VALUE, -0.0, 0.0, Double.MIN_VALUE, 1.0, Math.nextDown(10000.0), 10000.0));
    }

    @Test
    public void shouldChooseFiniteDoublesWithinExtremeAndNarrowRanges() {
        final double[][] ranges = {
                { -Double.MAX_VALUE, Double.MAX_VALUE },
                { Math.nextDown(Double.MAX_VALUE), Double.MAX_VALUE },
                { -Double.MAX_VALUE, Math.nextUp(-Double.MAX_VALUE) },
                { Double.MIN_VALUE, 2 * Double.MIN_VALUE },
                { -2 * Double.MIN_VALUE, -Double.MIN_VALUE },
                { 0.0, 10000.0 },
                { -10000.0, -0.0 }
        };
        for (double[] range : ranges) {
            final List<Double> values = samples(Gen.choose(range[0], range[1]));
            assertThat(values).allMatch(d -> Double.isFinite(d)
                    && Double.compare(d, range[0]) >= 0 && Double.compare(d, range[1]) <= 0);
            assertThat(values).contains(range[0], range[1]);
            assertThat(samples(Gen.choose(range[1], range[0]))).isEqualTo(values);
        }
    }

    @Test
    public void shouldGenerateBothSignedZerosWhenTheyAreTheBounds() {
        assertThat(samples(Gen.choose(-0.0, 0.0))).containsOnly(-0.0, 0.0).contains(-0.0, 0.0);
        assertThat(samples(Gen.choose(0.0, -0.0))).isEqualTo(samples(Gen.choose(-0.0, 0.0)));
        assertThat(samples(Gen.choose(-0.0, -0.0))).containsOnly(-0.0);
        assertThat(samples(Gen.choose(0.0, 0.0))).containsOnly(0.0);
    }

    @Test
    public void shouldThrowWhenChooseDoubleAndMinIsNegativeInfinite() {
        assertThrows(IllegalArgumentException.class, () -> {
        Gen.choose(Double.NEGATIVE_INFINITY, 0.0d);
    });
    }

    @Test
    public void shouldThrowWhenChooseDoubleAndMinIsPositiveInfinite() {
        assertThrows(IllegalArgumentException.class, () -> {
        Gen.choose(Double.POSITIVE_INFINITY, 0.0d);
    });
    }

    @Test
    public void shouldThrowWhenChooseDoubleAndMinIsNotANumber() {
        assertThrows(IllegalArgumentException.class, () -> {
        Gen.choose(Double.NaN, 0.0d);
    });
    }

    @Test
    public void shouldThrowWhenChooseDoubleAndMaxIsNegativeInfinite() {
        assertThrows(IllegalArgumentException.class, () -> {
        Gen.choose(0.0d, Double.NEGATIVE_INFINITY);
    });
    }

    @Test
    public void shouldThrowWhenChooseDoubleAndMaxIsPositiveInfinite() {
        assertThrows(IllegalArgumentException.class, () -> {
        Gen.choose(0.0d, Double.POSITIVE_INFINITY);
    });
    }

    @Test
    public void shouldThrowWhenChooseDoubleAndMaxIsNotANumber() {
        assertThrows(IllegalArgumentException.class, () -> {
        Gen.choose(0.0d, Double.NaN);
    });
    }

    // -- choose(char, char)

    @Test
    public void shouldChooseCharBetweenMinMax() {
        assertForAll(() -> Gen.choose('A', 'Z').apply(RANDOM), c -> c >= 'A' && c <= 'Z');
    }

    @Test
    public void shouldChooseCharWhenMinEqualsMax() {
        assertForAll(() -> Gen.choose('a', 'a').apply(RANDOM), c -> c == 'a');
    }

    @Test
    public void shouldChooseCharBetweenFullCharRange() {
        assertForAll(() -> Gen.choose(Character.MIN_VALUE, Character.MAX_VALUE).apply(RANDOM),
                c -> c >= Character.MIN_VALUE && c <= Character.MAX_VALUE);
    }

    @Test
    public void shouldFavorCharRangeBoundaries() {
        assertEdgeCoverage(Gen.choose(Character.MIN_VALUE, Character.MAX_VALUE),
                List.of(Character.MIN_VALUE, (char) 1, (char) (Character.MAX_VALUE - 1), Character.MAX_VALUE));
        assertThat(samples(Gen.choose('z', 'a'))).isEqualTo(samples(Gen.choose('a', 'z')));
    }

    @Test
    public void shouldNotFavorPositionsInExplicitChoices() {
        final List<Integer> values = List.range(0, 10);
        final List<Gen<Integer>> generators = values.map(Gen::of);
        assertUniformChoices(Gen.choose(values.toArray(Integer[]::new)));
        assertUniformChoices(Gen.choose(values));
        assertUniformChoices(Gen.choose("0123456789".toCharArray()).map(c -> c - '0'));
        assertUniformChoices(Gen.oneOf(generators));
        assertUniformChoices(Gen.oneOf(Gen.of(0), Gen.of(1), Gen.of(2), Gen.of(3), Gen.of(4),
                Gen.of(5), Gen.of(6), Gen.of(7), Gen.of(8), Gen.of(9)));
    }

    @Test
    public void shouldPreserveGeneratorWeightsWithoutFavoringFirstOrLast() {
        final List<Integer> values = samples(Gen.frequency(
                Tuple.of(1, Gen.of(0)), Tuple.of(8, Gen.of(1)), Tuple.of(1, Gen.of(2))));
        assertThat(values.count(i -> i == 0)).isBetween(70, 130);
        assertThat(values.count(i -> i == 1)).isBetween(750, 850);
        assertThat(values.count(i -> i == 2)).isBetween(70, 130);
    }

    @Test
    public void shouldReplayWithTheSameSeedWhenReusingAGenerator() {
        final Gen<Integer> integers = Gen.choose(-10000, 10000);
        final Gen<Long> longs = Gen.choose(Long.MIN_VALUE, Long.MAX_VALUE);
        final Gen<Double> doubles = Gen.choose(-Double.MAX_VALUE, Double.MAX_VALUE);
        assertThat(samples(integers)).isEqualTo(samples(integers));
        assertThat(samples(longs)).isEqualTo(samples(longs));
        assertThat(samples(doubles)).isEqualTo(samples(doubles));
    }

    // -- choose(array)

    @Test
    public void shouldChooseFromASingleArray() throws Exception {
        Integer[] i = { 1 };
        assertForAll(() -> Gen.choose(i).apply(RANDOM), c -> c == 1);
    }

    @Test
    public void shouldFailOnEmptyArray() throws Exception {
        Integer[] i = {};
        assertThrows(RuntimeException.class, () -> Gen.choose(i).apply(RANDOM));
    }

    // -- Choose(enum)

    enum testEnum {
        value1
    }

    @Test
    public void shouldChooseFromEnum() throws Exception {
        assertForAll(() -> Gen.choose(testEnum.class).apply(RANDOM), e -> Arrays.asList(testEnum.values()).contains(e));
    }

    // -- Choose(iterable)
    @Test
    public void shouldChooseFromIterable() throws Exception {
        List<Integer> i = List.of(1);
        assertForAll(() -> Gen.choose(i).apply(RANDOM), c -> c == 1);
    }

    @Test
    public void shouldChooseFromIterableWithInstancesOfGenericInterface() {
        List<Supplier<String>> i = List.of(() -> "test", () -> "test");

        Supplier<String> supplier = Gen.choose(i).apply(RANDOM);

        assertThat(supplier.get()).isEqualTo("test");
    }

    @Test
    public void shouldFailOnEmptyIterable() throws Exception {
        List<Integer> i = List.empty();
        assertThrows(RuntimeException.class, () -> Gen.choose(i).apply(RANDOM));
    }

    // -- fail

    @Test
    public void shouldFailAlwaysWhenCallingFailingGen() {
        assertThrows(RuntimeException.class, () -> {
        Gen.fail().apply(RANDOM);
    });
    }

    // -- frequency(VarArgs)

    @Test
    public void shouldThrowWhenCallingFrequencyOfVarArgsAndArgIsNull() {
        assertThrows(NullPointerException.class, () -> {
        Gen.frequency((Tuple2<Integer, Gen<Object>>) null);
    });
    }

    @Test
    public void shouldThrowWhenCallingFrequencyOfVarArgsAndArgIsEmpty() {
        assertThrows(IllegalArgumentException.class, () -> {
        @SuppressWarnings("unchecked")
        final Tuple2<Integer, Gen<Object>>[] empty = (Tuple2<Integer, Gen<Object>>[]) new Tuple2<?, ?>[0];
        Gen.frequency(empty);
    });
    }

    @Test
    public void shouldThrowWhenCallingFrequencyOfVarArgsAndAllFrequenciesAreNonPositive() {
        assertThrows(IllegalArgumentException.class, () -> {
        final Gen<Integer> gen = Gen.frequency(Tuple.of(-1, Gen.of(-1)), Tuple.of(0, Gen.of(0)));
        gen.apply(RANDOM);
    });
    }

    @Test
    public void shouldIgnoreGeneratorsWithNonPositiveFrequency() {
        final Gen<Integer> gen = Gen.frequency(Tuple.of(-1, Gen.of(-1)), Tuple.of(0, Gen.of(0)), Tuple.of(1, Gen.of(1)));
        assertForAll(() -> gen.apply(RANDOM), i -> i == 1);
    }

    @Test
    public void shouldGenerateElementsAccordingToFrequencyGivenVarArgs() {
        final Gen<Integer> gen = Gen.frequency(Tuple.of(0, Gen.of(-1)), Tuple.of(1, Gen.of(1)));
        assertForAll(() -> gen.apply(RANDOM), i -> i != -1);
    }

    // -- frequency(Iterable)

    @Test
    public void shouldThrowWhenCallingFrequencyOfIterableAndArgIsNull() {
        assertThrows(NullPointerException.class, () -> {
        Gen.frequency((Iterable<Tuple2<Integer, Gen<Object>>>) null);
    });
    }

    @Test
    public void shouldThrowWhenCallingFrequencyOfIterableAndArgIsEmpty() {
        assertThrows(IllegalArgumentException.class, () -> {
        Gen.frequency(List.empty());
    });
    }

    @Test
    public void shouldThrowWhenCallingFrequencyOfIterableAndAllFrequenciesAreNonPositive() {
        assertThrows(IllegalArgumentException.class, () -> {
        final Gen<Integer> gen = Gen.frequency(List.of(Tuple.of(-1, Gen.of(-1)), Tuple.of(0, Gen.of(0))));
        gen.apply(RANDOM);
    });
    }

    @Test
    public void shouldGenerateElementsAccordingToFrequencyGivenAnIterable() {
        final Gen<Integer> gen = Gen.frequency(List.of(Tuple.of(0, Gen.of(-1)), Tuple.of(1, Gen.of(1))));
        assertForAll(() -> gen.apply(RANDOM), i -> i != -1);
    }

    // -- oneOf(VarArgs)

    @Test
    public void shouldThrowWhenCallingOneOfAndVarArgsIsNull() {
        assertThrows(NullPointerException.class, () -> {
        Gen.oneOf((Gen<Object>[]) null);
    });
    }

    @Test
    public void shouldThrowWhenCallingOneOfAndVarArgsIsEmpty() {
        assertThrows(IllegalArgumentException.class, () -> {
        @SuppressWarnings("unchecked")
        final Gen<Object>[] empty = (Gen<Object>[]) new Gen<?>[0];
        Gen.oneOf(empty);
    });
    }

    @Test
    public void shouldReturnOneOfGivenVarArgs() {
        final Gen<Integer> gen = Gen.oneOf(Gen.of(1), Gen.of(2));
        assertForAll(() -> gen.apply(RANDOM), i -> i == 1 || i == 2);
    }

    // -- oneOf(Iterable)

    @Test
    public void shouldThrowWhenCallingOneOfAndIterableIsNull() {
        assertThrows(NullPointerException.class, () -> {
        Gen.oneOf((Iterable<Gen<Object>>) null);
    });
    }

    @Test
    public void shouldThrowWhenCallingOneOfAndIterableIsEmpty() {
        assertThrows(IllegalArgumentException.class, () -> {
        Gen.oneOf(List.empty());
    });
    }

    @Test
    public void shouldReturnOneOfGivenIterable() {
        final Gen<Integer> gen = Gen.oneOf(List.of(Gen.of(1), Gen.of(2)));
        assertForAll(() -> gen.apply(RANDOM), i -> i == 1 || i == 2);
    }

    // -- arbitrary

    @Test
    public void shouldConvertGenToArbitrary() {
        assertThat(Gen.of(1).arbitrary()).isInstanceOf(Arbitrary.class);
    }

    // -- map

    @Test
    public void shouldThrowWhenCallingMapWithNullArg() {
        assertThrows(NullPointerException.class, () -> {
        Gen.of(1).map(null);
    });
    }

    @Test
    public void shouldMapGen() {
        final Gen<Integer> gen = Gen.of(1).map(i -> i * 2);
        assertForAll(() -> gen.apply(RANDOM), i -> i % 2 == 0);
    }

    // -- flatMap

    @Test
    public void shouldThrowWhenCallingFlatMapWithNullArg() {
        assertThrows(NullPointerException.class, () -> {
        Gen.of(1).flatMap(null);
    });
    }

    @Test
    public void shouldFlatMapGen() {
        final Gen<Integer> gen = Gen.of(1).flatMap(i -> Gen.of(i * 2));
        assertForAll(() -> gen.apply(RANDOM), i -> i % 2 == 0);
    }

    // -- filter

    @Test
    public void shouldThrowWhenCallingFilterWithNullArg() {
        assertThrows(NullPointerException.class, () -> {
        Gen.of(1).filter(null);
    });
    }

    @Test
    public void shouldFilterGenerator() {
        final Gen<Integer> gen = Gen.choose(1, 2).filter(i -> i % 2 == 0);
        assertForAll(() -> gen.apply(RANDOM), i -> i == 2);
    }

    @Test
    public void shouldDetectEmptyFilter() {
        assertThrows(IllegalStateException.class, () -> {
        Gen.of(1).filter(ignored -> false).apply(RANDOM);
    });
    }

    // -- tap

    @Test
    public void shouldTapGen() {
        final int[] actual = new int[] { -1 };
        final int expected = Gen.of(1).tap(i -> actual[0] = i).apply(new Random());
        assertThat(actual[0]).isEqualTo(expected);
    }

    // helpers

    private static <T> List<T> samples(Gen<T> gen) {
        final Random random = new Random(0L);
        return List.fill(TRIES, () -> gen.apply(random));
    }

    private static <T> void assertEdgeCoverage(Gen<T> gen, List<T> edges) {
        final List<T> values = samples(gen);
        assertThat(values).containsAll(edges);
        assertThat(values.count(edges::contains)).isBetween(400, 600);
    }

    private static void assertUniformChoices(Gen<Integer> gen) {
        final List<Integer> values = samples(gen);
        for (int i = 0; i < 10; i++) {
            final int value = i;
            assertThat(values.count(n -> n == value)).isBetween(70, 130);
        }
    }

    <T> void assertForAll(Supplier<T> supplier, Predicate<T> property) {
        for (int i = 0; i < TRIES; i++) {
            final T element = supplier.get();
            if (!property.test(element)) {
                throw new AssertionError("predicate did not hold for " + element);
            }
        }
    }

    // like assertForAll, but also asserts the generator produces more than a couple of distinct
    // values over TRIES draws, catching an overflow that silently narrows a wide range down to it
    <T> void assertForAllDiverse(Supplier<T> supplier, Predicate<T> property) {
        final java.util.Set<T> seen = new java.util.HashSet<>();
        for (int i = 0; i < TRIES; i++) {
            final T element = supplier.get();
            if (!property.test(element)) {
                throw new AssertionError("predicate did not hold for " + element);
            }
            seen.add(element);
        }
        if (seen.size() < 50) {
            throw new AssertionError("expected a diverse spread of generated values but got only " + seen.size() + " distinct values: " + seen);
        }
    }
}
