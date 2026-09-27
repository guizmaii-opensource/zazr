package dev.zazr.collection;

import dev.zazr.control.Option;
import java.util.ArrayList;
import java.util.Collections;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.ToDoubleFunction;
import java.util.function.ToIntFunction;
import java.util.function.ToLongFunction;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

/**
 * The typed sums, products and averages of every collection that has them. The elements are the indices
 * {@code 0..n-1}, so a set keeps all of them, and the mapper reads the value of index {@code i} from an array.
 */
public class NumericAggregatesTest {

    private record Aggregates(
            Function<ToIntFunction<Integer>, Integer> sumInt,
            Function<ToLongFunction<Integer>, Long> sumLong,
            Function<ToDoubleFunction<Integer>, Double> sumDouble,
            Function<ToIntFunction<Integer>, Integer> productInt,
            Function<ToLongFunction<Integer>, Long> productLong,
            Function<ToDoubleFunction<Integer>, Double> productDouble,
            Function<ToDoubleFunction<Integer>, Option<Double>> average) {}

    private record Subject(String name, boolean nonEmpty, Function<java.util.List<Integer>, Aggregates> build) {
        Aggregates of(java.util.List<Integer> indices) {
            return build.apply(indices);
        }

        Aggregates ofSize(int size) {
            return of(IntStream.range(0, size).boxed().toList());
        }

        @Override
        public String toString() {
            return name;
        }
    }

    private static Aggregates aggregates(List<Integer> c) {
        return new Aggregates(
                c::sumInt, c::sumLong, c::sumDouble, c::productInt, c::productLong, c::productDouble, c::average);
    }

    private static Aggregates aggregates(LazyList<Integer> c) {
        return new Aggregates(
                c::sumInt, c::sumLong, c::sumDouble, c::productInt, c::productLong, c::productDouble, c::average);
    }

    private static Aggregates aggregates(Vector<Integer> c) {
        return new Aggregates(
                c::sumInt, c::sumLong, c::sumDouble, c::productInt, c::productLong, c::productDouble, c::average);
    }

    private static Aggregates aggregates(Queue<Integer> c) {
        return new Aggregates(
                c::sumInt, c::sumLong, c::sumDouble, c::productInt, c::productLong, c::productDouble, c::average);
    }

    private static Aggregates aggregates(Set<Integer> c) {
        return new Aggregates(
                c::sumInt, c::sumLong, c::sumDouble, c::productInt, c::productLong, c::productDouble, c::average);
    }

    private static Aggregates aggregates(NonEmptyVector<Integer> c) {
        return new Aggregates(
                c::sumInt,
                c::sumLong,
                c::sumDouble,
                c::productInt,
                c::productLong,
                c::productDouble,
                mapper -> Option.some(c.average(mapper)));
    }

    private static Aggregates aggregates(NonEmptySet<Integer> c) {
        return new Aggregates(
                c::sumInt,
                c::sumLong,
                c::sumDouble,
                c::productInt,
                c::productLong,
                c::productDouble,
                mapper -> Option.some(c.average(mapper)));
    }

    private static Aggregates aggregates(NonEmptySortedSet<Integer> c) {
        return new Aggregates(
                c::sumInt,
                c::sumLong,
                c::sumDouble,
                c::productInt,
                c::productLong,
                c::productDouble,
                mapper -> Option.some(c.average(mapper)));
    }

    static Stream<Subject> subjects() {
        return Stream.of(
                new Subject("List", false, indices -> aggregates(List.ofAll(indices))),
                new Subject("LazyList", false, indices -> aggregates(LazyList.ofAll(indices))),
                new Subject("Vector", false, indices -> aggregates(Vector.ofAll(indices))),
                new Subject("Queue", false, indices -> aggregates(Queue.ofAll(indices))),
                new Subject("HashSet", false, indices -> aggregates(HashSet.ofAll(indices))),
                new Subject("LinkedHashSet", false, indices -> aggregates(LinkedHashSet.ofAll(indices))),
                new Subject("TreeSet", false, indices -> aggregates(TreeSet.ofAll(indices))),
                new Subject(
                        "NonEmptyVector",
                        true,
                        indices -> aggregates(
                                NonEmptyVector.fromIterable(indices).getOrElseThrow(NoSuchElementException::new))),
                new Subject(
                        "NonEmptySet",
                        true,
                        indices -> aggregates(
                                NonEmptySet.fromIterable(indices).getOrElseThrow(NoSuchElementException::new))),
                new Subject(
                        "NonEmptySortedSet",
                        true,
                        indices -> aggregates(
                                NonEmptySortedSet.fromIterable(indices).getOrElseThrow(NoSuchElementException::new))));
    }

    // every order of the values, so a result that depended on the iteration order would show
    private static java.util.List<int[]> permutations(int[] values) {
        java.util.List<int[]> result = new ArrayList<>();
        permute(values.clone(), 0, result);
        return result;
    }

    private static void permute(int[] values, int from, java.util.List<int[]> result) {
        if (from == values.length) {
            result.add(values.clone());
        }
        for (int i = from; i < values.length; i++) {
            swap(values, from, i);
            permute(values, from + 1, result);
            swap(values, from, i);
        }
    }

    private static void swap(int[] values, int i, int j) {
        int tmp = values[i];
        values[i] = values[j];
        values[j] = tmp;
    }

    private static java.util.List<long[]> permutations(long[] values) {
        java.util.List<Integer> order =
                IntStream.range(0, values.length).boxed().toList();
        return permutations(order.stream().mapToInt(i -> i).toArray()).stream()
                .map(p -> IntStream.of(p).mapToLong(i -> values[i]).toArray())
                .toList();
    }

    private static void assertIntSum(Subject subject, int expected, int... values) {
        for (int[] p : permutations(values)) {
            assertThat(subject.ofSize(p.length).sumInt().apply(i -> p[i])).isEqualTo(expected);
        }
    }

    private static void assertIntSumOverflows(Subject subject, int... values) {
        for (int[] p : permutations(values)) {
            assertThatThrownBy(() -> subject.ofSize(p.length).sumInt().apply(i -> p[i]))
                    .isInstanceOf(ArithmeticException.class);
        }
    }

    private static void assertLongSum(Subject subject, long expected, long... values) {
        for (long[] p : permutations(values)) {
            assertThat(subject.ofSize(p.length).sumLong().apply(i -> p[i])).isEqualTo(expected);
        }
    }

    private static void assertLongSumOverflows(Subject subject, long... values) {
        for (long[] p : permutations(values)) {
            assertThatThrownBy(() -> subject.ofSize(p.length).sumLong().apply(i -> p[i]))
                    .isInstanceOf(ArithmeticException.class);
        }
    }

    private static void assertIntProduct(Subject subject, int expected, int... values) {
        for (int[] p : permutations(values)) {
            assertThat(subject.ofSize(p.length).productInt().apply(i -> p[i])).isEqualTo(expected);
        }
    }

    private static void assertIntProductOverflows(Subject subject, int... values) {
        for (int[] p : permutations(values)) {
            assertThatThrownBy(() -> subject.ofSize(p.length).productInt().apply(i -> p[i]))
                    .isInstanceOf(ArithmeticException.class);
        }
    }

    private static void assertLongProduct(Subject subject, long expected, long... values) {
        for (long[] p : permutations(values)) {
            assertThat(subject.ofSize(p.length).productLong().apply(i -> p[i])).isEqualTo(expected);
        }
    }

    private static void assertLongProductOverflows(Subject subject, long... values) {
        for (long[] p : permutations(values)) {
            assertThatThrownBy(() -> subject.ofSize(p.length).productLong().apply(i -> p[i]))
                    .isInstanceOf(ArithmeticException.class);
        }
    }

    private static double sumDouble(Subject subject, double... values) {
        return subject.ofSize(values.length).sumDouble().apply(i -> values[i]);
    }

    private static double productDouble(Subject subject, double... values) {
        return subject.ofSize(values.length).productDouble().apply(i -> values[i]);
    }

    private static double average(Subject subject, double... values) {
        return subject.ofSize(values.length).average().apply(i -> values[i]).get();
    }

    @ParameterizedTest
    @MethodSource("subjects")
    void emptyCollectionsGiveTheIdentity(Subject subject) {
        assumeFalse(subject.nonEmpty());
        Aggregates empty = subject.ofSize(0);
        assertThat(empty.sumInt().apply(i -> 7)).isZero();
        assertThat(empty.sumLong().apply(i -> 7L)).isZero();
        assertThat(empty.sumDouble().apply(i -> 7.0)).isEqualTo(0.0);
        assertThat(empty.productInt().apply(i -> 7)).isEqualTo(1);
        assertThat(empty.productLong().apply(i -> 7L)).isEqualTo(1L);
        assertThat(empty.productDouble().apply(i -> 7.0)).isEqualTo(1.0);
        assertThat(empty.average().apply(i -> 7.0)).isEqualTo(Option.none());
    }

    @ParameterizedTest
    @MethodSource("subjects")
    void oneElementGivesItsValue(Subject subject) {
        Aggregates one = subject.ofSize(1);
        assertThat(one.sumInt().apply(i -> -7)).isEqualTo(-7);
        assertThat(one.sumLong().apply(i -> Long.MIN_VALUE)).isEqualTo(Long.MIN_VALUE);
        assertThat(one.sumDouble().apply(i -> -0.5)).isEqualTo(-0.5);
        assertThat(one.productInt().apply(i -> Integer.MIN_VALUE)).isEqualTo(Integer.MIN_VALUE);
        assertThat(one.productLong().apply(i -> Long.MIN_VALUE)).isEqualTo(Long.MIN_VALUE);
        assertThat(one.productDouble().apply(i -> -0.5)).isEqualTo(-0.5);
        assertThat(one.average().apply(i -> -0.5)).isEqualTo(Option.some(-0.5));
    }

    @ParameterizedTest
    @MethodSource("subjects")
    void exactResults(Subject subject) {
        Aggregates ten = subject.ofSize(10);
        assertThat(ten.sumInt().apply(i -> i + 1)).isEqualTo(55);
        assertThat(ten.sumLong().apply(i -> i + 1L)).isEqualTo(55L);
        assertThat(ten.sumDouble().apply(i -> i + 1.0)).isEqualTo(55.0);
        assertThat(ten.productInt().apply(i -> i + 1)).isEqualTo(3_628_800);
        assertThat(ten.productLong().apply(i -> i + 1L)).isEqualTo(3_628_800L);
        assertThat(ten.productDouble().apply(i -> i + 1.0)).isEqualTo(3_628_800.0);
        assertThat(ten.average().apply(i -> i + 1.0)).isEqualTo(Option.some(5.5));
        assertThat(ten.sumInt().apply(i -> i % 2 == 0 ? -i : i)).isEqualTo(5);
        assertThat(ten.productInt().apply(i -> i % 2 == 0 ? -1 : 2)).isEqualTo(-32);
        assertThat(ten.productLong().apply(i -> i % 2 == 0 ? -1L : 2L)).isEqualTo(-32L);
        Aggregates large = subject.ofSize(1025);
        assertThat(large.sumInt().apply(i -> i)).isEqualTo(1024 * 1025 / 2);
        assertThat(large.sumLong().apply(i -> (long) i * Integer.MAX_VALUE))
                .isEqualTo(1024L * 1025 / 2 * Integer.MAX_VALUE);
        assertThat(large.average().apply(i -> i)).isEqualTo(Option.some(512.0));
    }

    @ParameterizedTest
    @MethodSource("subjects")
    void sumIntThrowsExactlyWhenTheResultDoesNotFit(Subject subject) {
        assertIntSum(subject, Integer.MAX_VALUE, Integer.MAX_VALUE - 1, 1);
        assertIntSum(subject, Integer.MIN_VALUE, Integer.MIN_VALUE + 1, -1);
        assertIntSum(subject, Integer.MAX_VALUE, Integer.MAX_VALUE, 1, -1);
        assertIntSum(subject, Integer.MIN_VALUE, Integer.MIN_VALUE, -1, 1);
        assertIntSum(subject, -2, Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE);
        assertIntSumOverflows(subject, Integer.MAX_VALUE, 1);
        assertIntSumOverflows(subject, Integer.MIN_VALUE, -1);
        assertIntSumOverflows(subject, Integer.MAX_VALUE, Integer.MAX_VALUE);
        assertIntSumOverflows(subject, Integer.MIN_VALUE, Integer.MIN_VALUE, 1);
    }

    @ParameterizedTest
    @MethodSource("subjects")
    void sumLongThrowsExactlyWhenTheResultDoesNotFit(Subject subject) {
        assertLongSum(subject, Long.MAX_VALUE, Long.MAX_VALUE - 1, 1L);
        assertLongSum(subject, Long.MIN_VALUE, Long.MIN_VALUE + 1, -1L);
        assertLongSum(subject, Long.MAX_VALUE, Long.MAX_VALUE, 1L, -1L);
        assertLongSum(subject, Long.MIN_VALUE, Long.MIN_VALUE, -1L, 1L);
        assertLongSum(subject, -2L, Long.MAX_VALUE, Long.MAX_VALUE, Long.MIN_VALUE, Long.MIN_VALUE);
        assertLongSum(subject, 0L, Long.MIN_VALUE, Long.MIN_VALUE, Long.MAX_VALUE, Long.MAX_VALUE, 2L);
        assertLongSum(subject, (long) Integer.MAX_VALUE * 2, Integer.MAX_VALUE, Integer.MAX_VALUE);
        assertLongSumOverflows(subject, Long.MAX_VALUE, 1L);
        assertLongSumOverflows(subject, Long.MIN_VALUE, -1L);
        assertLongSumOverflows(subject, Long.MAX_VALUE, Long.MAX_VALUE);
        assertLongSumOverflows(subject, Long.MIN_VALUE, Long.MIN_VALUE);
        assertLongSumOverflows(subject, Long.MAX_VALUE, Long.MAX_VALUE, Long.MAX_VALUE);
        assertLongSumOverflows(subject, Long.MIN_VALUE, Long.MIN_VALUE, 1L);
    }

    @ParameterizedTest
    @MethodSource("subjects")
    void productIntThrowsExactlyWhenTheResultDoesNotFit(Subject subject) {
        assertIntProduct(subject, Integer.MIN_VALUE, 65_536, -32_768);
        assertIntProduct(subject, Integer.MIN_VALUE, Integer.MIN_VALUE, 1);
        assertIntProduct(subject, Integer.MIN_VALUE, Integer.MIN_VALUE, -1, -1);
        assertIntProduct(subject, 2_147_395_600, 46_340, 46_340);
        assertIntProduct(subject, -Integer.MAX_VALUE, Integer.MAX_VALUE, -1);
        assertIntProduct(subject, 0, Integer.MAX_VALUE, 2, 0);
        assertIntProduct(subject, 0, Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MIN_VALUE, 0);
        assertIntProductOverflows(subject, 65_536, 32_768);
        assertIntProductOverflows(subject, Integer.MIN_VALUE, -1);
        assertIntProductOverflows(subject, 46_341, 46_341);
        assertIntProductOverflows(subject, Integer.MAX_VALUE, 2, 1);
        assertIntProductOverflows(subject, Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE);
    }

    @ParameterizedTest
    @MethodSource("subjects")
    void productLongThrowsExactlyWhenTheResultDoesNotFit(Subject subject) {
        long twoTo31 = 1L << 31;
        long twoTo32 = 1L << 32;
        assertLongProduct(subject, Long.MIN_VALUE, twoTo32, -twoTo31);
        assertLongProduct(subject, Long.MIN_VALUE, Long.MIN_VALUE, 1L);
        assertLongProduct(subject, Long.MIN_VALUE, Long.MIN_VALUE, -1L, -1L);
        assertLongProduct(subject, Long.MIN_VALUE, 1L << 62, 2L, -1L);
        assertLongProduct(subject, 9_223_372_030_926_249_001L, 3_037_000_499L, 3_037_000_499L);
        assertLongProduct(subject, -Long.MAX_VALUE, Long.MAX_VALUE, -1L);
        assertLongProduct(subject, 0L, Long.MAX_VALUE, 2L, 0L);
        assertLongProduct(subject, 0L, twoTo32, twoTo32, 0L);
        assertLongProduct(subject, 0L, Long.MAX_VALUE, Long.MAX_VALUE, Long.MIN_VALUE, 0L);
        assertLongProductOverflows(subject, twoTo32, twoTo31);
        assertLongProductOverflows(subject, 1L << 62, 2L);
        assertLongProductOverflows(subject, Long.MIN_VALUE, -1L);
        assertLongProductOverflows(subject, 3_037_000_500L, 3_037_000_500L);
        assertLongProductOverflows(subject, twoTo32, twoTo32);
        assertLongProductOverflows(subject, twoTo32, -twoTo32, 1L);
        assertLongProductOverflows(subject, Long.MAX_VALUE, Long.MAX_VALUE, Long.MAX_VALUE);
        assertLongProductOverflows(subject, Long.MIN_VALUE, Long.MIN_VALUE, -1L);
    }

    @ParameterizedTest
    @MethodSource("subjects")
    void sumDoubleCompensatesAndFollowsTheSpecialValues(Subject subject) {
        assertThat(sumDouble(subject, 1.0, 1e100, 1.0, -1e100)).isEqualTo(2.0);
        assertThat(sumDouble(subject, 1e16, 1.0, -1e16)).isEqualTo(1.0);
        assertThat(sumDouble(subject, 1.0, Double.NaN, 2.0)).isNaN();
        assertThat(sumDouble(subject, Double.POSITIVE_INFINITY, 1.0)).isEqualTo(Double.POSITIVE_INFINITY);
        assertThat(sumDouble(subject, Double.NEGATIVE_INFINITY, 1.0)).isEqualTo(Double.NEGATIVE_INFINITY);
        assertThat(sumDouble(subject, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY))
                .isEqualTo(Double.POSITIVE_INFINITY);
        assertThat(sumDouble(subject, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY))
                .isNaN();
        assertThat(sumDouble(subject, Double.MAX_VALUE, Double.MAX_VALUE)).isEqualTo(Double.POSITIVE_INFINITY);
        assertThat(sumDouble(subject, -Double.MAX_VALUE, -Double.MAX_VALUE)).isEqualTo(Double.NEGATIVE_INFINITY);
        assertThat(sumDouble(subject, Double.MAX_VALUE, -Double.MAX_VALUE, 1.0)).isEqualTo(1.0);
    }

    @ParameterizedTest
    @MethodSource("subjects")
    void productDoubleFollowsTheSpecialValues(Subject subject) {
        assertThat(productDouble(subject, 0.5, 4.0, -1.0)).isEqualTo(-2.0);
        assertThat(productDouble(subject, Double.MAX_VALUE, 2.0)).isEqualTo(Double.POSITIVE_INFINITY);
        assertThat(productDouble(subject, Double.MAX_VALUE, -2.0)).isEqualTo(Double.NEGATIVE_INFINITY);
        assertThat(productDouble(subject, 0.0, Double.POSITIVE_INFINITY)).isNaN();
        assertThat(productDouble(subject, Double.NaN, 1.0)).isNaN();
        assertThat(productDouble(subject, -1.0, Double.POSITIVE_INFINITY)).isEqualTo(Double.NEGATIVE_INFINITY);
        assertThat(productDouble(subject, Double.MIN_VALUE, 0.5)).isEqualTo(0.0);
    }

    @ParameterizedTest
    @MethodSource("subjects")
    void averageDividesTheCompensatedSum(Subject subject) {
        assertThat(average(subject, 1.0, 2.0)).isEqualTo(1.5);
        assertThat(average(subject, 1.0, 1e100, 2.0, -1e100)).isEqualTo(0.75);
        assertThat(average(subject, 1e16, 1.0, -1e16)).isEqualTo(1.0 / 3);
        assertThat(average(subject, 1.0, Double.NaN)).isNaN();
        assertThat(average(subject, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY))
                .isNaN();
        assertThat(average(subject, Double.MAX_VALUE, Double.MAX_VALUE)).isEqualTo(Double.POSITIVE_INFINITY);
        assertThat(average(subject, Double.NEGATIVE_INFINITY, 1.0)).isEqualTo(Double.NEGATIVE_INFINITY);
    }

    @ParameterizedTest
    @MethodSource("subjects")
    void theMapperRunsOnceOnEveryElementInOnePass(Subject subject) {
        Aggregates five = subject.ofSize(5);
        AtomicInteger calls = new AtomicInteger();
        five.sumInt().apply(i -> calls.incrementAndGet());
        five.sumLong().apply(i -> calls.incrementAndGet());
        five.sumDouble().apply(i -> calls.incrementAndGet());
        five.productInt().apply(i -> calls.incrementAndGet());
        five.productLong().apply(i -> calls.incrementAndGet());
        five.productDouble().apply(i -> calls.incrementAndGet());
        five.average().apply(i -> calls.incrementAndGet());
        assertThat(calls.get()).isEqualTo(7 * 5);
        java.util.List<Integer> seen = Collections.synchronizedList(new ArrayList<>());
        five.sumInt().apply(i -> {
            seen.add(i);
            return i;
        });
        assertThat(seen).containsExactlyInAnyOrder(0, 1, 2, 3, 4);
    }

    @ParameterizedTest
    @MethodSource("subjects")
    void theMapperKeepsBeingCalledAfterAnOverflowOrAZero(Subject subject) {
        Aggregates three = subject.ofSize(3);
        AtomicInteger calls = new AtomicInteger();
        assertThat(three.productInt().apply(i -> {
                    calls.incrementAndGet();
                    return i == 0 ? 0 : Integer.MAX_VALUE;
                }))
                .isZero();
        assertThat(three.productLong().apply(i -> {
                    calls.incrementAndGet();
                    return i == 0 ? 0L : Long.MAX_VALUE;
                }))
                .isZero();
        assertThat(calls.get()).isEqualTo(6);
    }

    @ParameterizedTest
    @MethodSource("subjects")
    void aNullMapperThrows(Subject subject) {
        Aggregates two = subject.ofSize(2);
        assertThatNullPointerException().isThrownBy(() -> two.sumInt().apply(null));
        assertThatNullPointerException().isThrownBy(() -> two.sumLong().apply(null));
        assertThatNullPointerException().isThrownBy(() -> two.sumDouble().apply(null));
        assertThatNullPointerException().isThrownBy(() -> two.productInt().apply(null));
        assertThatNullPointerException().isThrownBy(() -> two.productLong().apply(null));
        assertThatNullPointerException().isThrownBy(() -> two.productDouble().apply(null));
        assertThatNullPointerException().isThrownBy(() -> two.average().apply(null));
    }

    @ParameterizedTest
    @MethodSource("subjects")
    void aNullMapperThrowsOnAnEmptyCollection(Subject subject) {
        assumeFalse(subject.nonEmpty());
        Aggregates empty = subject.ofSize(0);
        assertThatNullPointerException().isThrownBy(() -> empty.sumInt().apply(null));
        assertThatNullPointerException().isThrownBy(() -> empty.average().apply(null));
    }

    @Test
    void aLazyListOverAOneShotIterableGivesTheSameResultTwice() {
        java.util.Iterator<Integer> once = java.util.List.of(1, 2, 3, 4).iterator();
        LazyList<Integer> lazy = LazyList.ofAll(() -> once);
        assertThat(lazy.sumInt(i -> i)).isEqualTo(10);
        assertThat(lazy.sumInt(i -> i)).isEqualTo(10);
        assertThat(lazy.productLong(i -> i)).isEqualTo(24L);
        assertThat(lazy.average(i -> i)).isEqualTo(Option.some(2.5));
    }

    @Test
    void theCollectionsBuiltFromAOneShotIterableSumEveryElement() {
        java.util.List<Integer> values = java.util.List.of(3, 1, 4, 1, 5);
        assertThat(List.ofAll(oneShot(values)).sumInt(i -> i)).isEqualTo(14);
        assertThat(Vector.ofAll(oneShot(values)).sumLong(i -> i)).isEqualTo(14L);
        assertThat(Queue.ofAll(oneShot(values)).productInt(i -> i)).isEqualTo(60);
        assertThat(HashSet.ofAll(oneShot(values)).sumInt(i -> i)).isEqualTo(13);
        assertThat(NonEmptyVector.fromIterable(oneShot(values)).map(v -> v.sumDouble(i -> i)))
                .isEqualTo(Option.some(14.0));
    }

    private static Iterable<Integer> oneShot(java.util.List<Integer> values) {
        AtomicInteger iterations = new AtomicInteger();
        return () -> {
            if (iterations.getAndIncrement() > 0) {
                throw new IllegalStateException("iterated twice");
            }
            return values.iterator();
        };
    }
}
