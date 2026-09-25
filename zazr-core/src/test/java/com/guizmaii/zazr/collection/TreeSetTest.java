package com.guizmaii.zazr.collection;

import com.guizmaii.zazr.Tuple;
import com.guizmaii.zazr.Tuple2;
import com.guizmaii.zazr.control.Option;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.NoSuchElementException;
import java.util.Spliterator;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestTemplate;

import static com.guizmaii.zazr.TestComparators.toStringComparator;
import static java.util.Arrays.asList;
import static java.util.Comparator.comparingInt;
import static java.util.Comparator.naturalOrder;
import static java.util.Comparator.nullsFirst;
import static java.util.Comparator.reverseOrder;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TreeSetTest extends AbstractTraversableTest {

    @Override
    protected <T> Collector<T, ArrayList<T>, ? extends TreeSet<T>> collector() {
        return TreeSet.collector(Comparators.naturalComparator());
    }

    @Override
    protected <T> TreeSet<T> empty() {
        return TreeSet.empty(Comparators.naturalComparator());
    }

    @Override
    protected boolean emptyShouldBeSingleton() {
        return false;
    }

    @Override
    protected <T> TreeSet<T> of(T element) {
        return TreeSet.of(Comparators.naturalComparator(), element);
    }

    protected <T> TreeSet<T> of(Comparator<? super T> comparator, T element) {
        return TreeSet.of(comparator, element);
    }

    @SafeVarargs
    @SuppressWarnings("varargs")
    protected final <T> TreeSet<T> of(Comparator<? super T> comparator, T... elements) {
        return TreeSet.of(comparator, elements);
    }

    @Override
    @SafeVarargs
    @SuppressWarnings("varargs")
    protected final <T> TreeSet<T> of(T... elements) {
        return TreeSet.<T> of(Comparators.naturalComparator(), elements);
    }

    @Override
    protected <T> TreeSet<T> ofAll(Iterable<? extends T> elements) {
        return TreeSet.ofAll(Comparators.naturalComparator(), elements);
    }

    @Override
    protected <T extends Comparable<? super T>> TreeSet<T> ofJavaStream(java.util.stream.Stream<? extends T> javaStream) {
        return TreeSet.ofAll(javaStream);
    }

    @Override
    protected TreeSet<Boolean> ofAll(boolean... elements) {
        return TreeSet.ofAll(elements);
    }

    @Override
    protected TreeSet<Byte> ofAll(byte... elements) {
        return TreeSet.ofAll(elements);
    }

    @Override
    protected TreeSet<Character> ofAll(char... elements) {
        return TreeSet.ofAll(elements);
    }

    @Override
    protected TreeSet<Double> ofAll(double... elements) {
        return TreeSet.ofAll(elements);
    }

    @Override
    protected TreeSet<Float> ofAll(float... elements) {
        return TreeSet.ofAll(elements);
    }

    @Override
    protected TreeSet<Integer> ofAll(int... elements) {
        return TreeSet.ofAll(elements);
    }

    @Override
    protected TreeSet<Long> ofAll(long... elements) {
        return TreeSet.ofAll(elements);
    }

    @Override
    protected TreeSet<Short> ofAll(short... elements) {
        return TreeSet.ofAll(elements);
    }

    @Override
    protected <T> TreeSet<T> tabulate(int n, Function<? super Integer, ? extends T> f) {
        return TreeSet.tabulate(Comparators.naturalComparator(), n, f);
    }

    @Override
    protected <T> TreeSet<T> fill(int n, Supplier<? extends T> s) {
        return TreeSet.fill(Comparators.naturalComparator(), n, s);
    }

    // a TreeSet carries its comparator, so an empty result is a new instance and the folded cases use isEqualTo
    private boolean useIsEqualToInsteadOfIsSameAs() {
        return true;
    }

    protected TreeSet<Character> range(char from, char toExclusive) {
        return TreeSet.range(from, toExclusive);
    }

    protected TreeSet<Character> rangeBy(char from, char toExclusive, int step) {
        return TreeSet.rangeBy(from, toExclusive, step);
    }

    protected TreeSet<Double> rangeBy(double from, double toExclusive, double step) {
        return TreeSet.rangeBy(from, toExclusive, step);
    }

    protected TreeSet<Integer> range(int from, int toExclusive) {
        return TreeSet.range(from, toExclusive);
    }

    protected TreeSet<Integer> rangeBy(int from, int toExclusive, int step) {
        return TreeSet.rangeBy(from, toExclusive, step);
    }

    protected TreeSet<Long> range(long from, long toExclusive) {
        return TreeSet.range(from, toExclusive);
    }

    protected TreeSet<Long> rangeBy(long from, long toExclusive, long step) {
        return TreeSet.rangeBy(from, toExclusive, step);
    }

    protected TreeSet<Character> rangeClosed(char from, char toInclusive) {
        return TreeSet.rangeClosed(from, toInclusive);
    }

    protected TreeSet<Character> rangeClosedBy(char from, char toInclusive, int step) {
        return TreeSet.rangeClosedBy(from, toInclusive, step);
    }

    protected TreeSet<Double> rangeClosedBy(double from, double toInclusive, double step) {
        return TreeSet.rangeClosedBy(from, toInclusive, step);
    }

    protected TreeSet<Integer> rangeClosed(int from, int toInclusive) {
        return TreeSet.rangeClosed(from, toInclusive);
    }

    protected TreeSet<Integer> rangeClosedBy(int from, int toInclusive, int step) {
        return TreeSet.rangeClosedBy(from, toInclusive, step);
    }

    protected TreeSet<Long> rangeClosed(long from, long toInclusive) {
        return TreeSet.rangeClosed(from, toInclusive);
    }

    protected TreeSet<Long> rangeClosedBy(long from, long toInclusive, long step) {
        return TreeSet.rangeClosedBy(from, toInclusive, step);
    }

    @Nested
    class CollectorTests {
        @Test
        public void shouldCollectEmpty() {
            final TreeSet<Integer> actual = java.util.stream.Stream.<Integer>empty().collect(TreeSet.collector());
            assertThat(actual).isEmpty();
        }

        @Test
        public void shouldCollectNonEmpty() {
            final TreeSet<Integer> actual = java.util.stream.Stream.of(1, 2, 3).collect(TreeSet.collector());
            final TreeSet<Integer> expected = of(1, 2, 3);
            assertThat(actual).isEqualTo(expected);
        }
    }

    @Nested
    class ConstructTests {
        @Test
        public void shouldConstructEmptySetWithExplicitComparator() {
            final TreeSet<Integer> ts = TreeSet.<Integer> of(Comparators.naturalComparator()
                .reversed())
                .addAll(Vector.of(1, 2, 3));
            assertThat(ts.toVector()).isEqualTo(Vector.of(3, 2, 1));
        }

        @Test
        public void shouldConstructStreamFromEmptyJavaStream() {
            final TreeSet<Integer> actual = ofJavaStream(java.util.stream.Stream.<Integer>empty());
            final TreeSet<Integer> expected = empty();
            assertThat(actual).isEqualTo(expected);
        }

        @Test
        public void shouldConstructStreamFromNonEmptyJavaStream() {
            final TreeSet<Integer> actual = TreeSet.ofAll(Comparators.naturalComparator(), java.util.stream.Stream.of(1, 2, 3));
            final TreeSet<Integer> expected = of(1, 2, 3);
            assertThat(actual).isEqualTo(expected);
        }

        @Test
        public void shouldConstructStreamFromNonEmptyJavaStreamWithoutComparator() {
            final TreeSet<Integer> actual = ofJavaStream(java.util.stream.Stream.of(1, 2, 3));
            final TreeSet<Integer> expected = of(1, 2, 3);
            assertThat(actual).isEqualTo(expected);
        }

        @Test
        public void shouldConstructFromTreeSetWithoutComparator() {
            final TreeSet<Integer> actual = TreeSet.ofAll(TreeSet.of(1));
            final TreeSet<Integer> expected = of(1);
            assertThat(actual).isEqualTo(expected);
        }
    }

    @Nested
    class StaticNarrowTests {
        @Test
        public void shouldNarrowTreeSet() {
            final TreeSet<Double> doubles = TreeSet.of(toStringComparator(), 1.0d);
            final TreeSet<Number> numbers = TreeSet.narrow(doubles);
            final int actual = numbers.add(new BigDecimal("2.0")).sum().intValue();
            assertThat(actual).isEqualTo(3);
        }
    }

    @Nested
    class TreeSetAddallTests {
        @Test
        public void shouldKeepComparator() {
            final List<Integer> actual = TreeSet.empty(inverseIntComparator()).addAll(TreeSet.of(1, 2, 3)).toList();
            final List<Integer> expected = List.of(3, 2, 1);
            assertThat(actual).isEqualTo(expected);
        }
    }
    

    @Nested
    class TreeSetRemoveallTests {
        @Test
        public void shouldKeepComparatorOnRemoveAll() {
            final TreeSet<Integer> ts = TreeSet.of(Comparators.naturalComparator()
                .reversed(), 1, 2, 3)
                .removeAll(Vector.of(1, 2, 3))
                .addAll(Vector.of(4, 5, 6));
            assertThat(ts.toVector()).isEqualTo(Vector.of(6, 5, 4));
        }
    }

    @Nested
    class TreeSetDiffTests {
        @Test
        public void shouldCalculateDiffIfNotTreeSet() {
            final TreeSet<Integer> actual = of(1, 2, 3).diff(HashSet.of(1, 2));
            final TreeSet<Integer> expected = of(3);
            assertThat(actual).isEqualTo(expected);
        }

        @Test
        public void shouldCalculateDiffOfTreeSetWithDifferentComparator() {
            final TreeSet<Integer> actual = of(1, 2, 3, 4, 5).diff(TreeSet.of(inverseIntComparator(), 2, 4, 6));
            assertThat(actual.toList()).isEqualTo(List.of(1, 3, 5));
        }
    }

    @Nested
    class UnionTests {
        @Test
        public void shouldCalculateUnionIfNotTreeSet() {
            final TreeSet<Integer> actual = of(1, 2, 3).union(HashSet.of(4));
            final TreeSet<Integer> expected = of(1, 2, 3, 4);
            assertThat(actual).isEqualTo(expected);
        }

        @Test
        public void shouldCalculateUnionOfTreeSetWithDifferentComparator() {
            final TreeSet<Integer> actual = of(1, 2, 3, 4, 5).union(TreeSet.of(inverseIntComparator(), 2, 4, 6));
            assertThat(actual.toList()).isEqualTo(List.of(1, 2, 3, 4, 5, 6));
        }

        @Test
        public void shouldKeepComparatorOnUnionOfEmptyWithTreeSetWithDifferentComparator() {
            final TreeSet<Integer> actual = TreeSet.empty(inverseIntComparator()).union(TreeSet.of(1, 2, 3));
            assertThat(actual.toList()).isEqualTo(List.of(3, 2, 1));
        }
    }

    @Nested
    class TreeSetIntersectTests {
        @Test
        public void shouldCalculateEmptyIntersectIfNotTreeSet() {
            final TreeSet<Integer> actual = of(1, 2, 3).intersect(HashSet.of(4));
            assertThat(actual).isEmpty();
        }

        @Test
        public void shouldCalculateIntersectIfNotTreeSet() {
            final TreeSet<Integer> actual = of(1, 2, 3).intersect(HashSet.of(3));
            final TreeSet<Integer> expected = of(3);
            assertThat(actual).isEqualTo(expected);
        }

        @Test
        public void shouldCalculateIntersectOfTreeSetWithDifferentComparator() {
            final TreeSet<Integer> actual = of(1, 2, 3, 4, 5).intersect(TreeSet.of(inverseIntComparator(), 2, 4, 6));
            assertThat(actual.toList()).isEqualTo(List.of(2, 4));
        }
    }

    @Nested
    class FillTests {
        @Test
        public void shouldFillWithoutComparator() {
            final TreeSet<Integer> actual = TreeSet.fill(3, () -> 1);
            final TreeSet<Integer> expected = of(1, 1, 1);
            assertThat(actual).isEqualTo(expected);
        }
    }

    @Nested
    class TabulateTests {
        @Test
        public void shouldTabulateWithoutComparator() {
            final TreeSet<Integer> actual = TreeSet.tabulate(3, Function.identity());
            final TreeSet<Integer> expected = of(0, 1, 2);
            assertThat(actual).isEqualTo(expected);
        }
    }

    @Nested
    class TosortedsetTests {
        @Test
        public void shouldReturnSelfOnConvertToSortedSet() {
            final TreeSet<Integer> value = of(1, 2, 3);
            assertThat(value.toSortedSet()).isSameAs(value);
        }

        @Test
        public void shouldReturnSelfOnConvertToSortedSetWithSameComparator() {
            final TreeSet<Integer> value = of(1, 2, 3);
            assertThat(value.toSortedSet(value.comparator())).isSameAs(value);
        }

        @Test
        public void shouldNotReturnSelfOnConvertToSortedSetWithDifferentComparator() {
            final TreeSet<Integer> value = of(1, 2, 3);
            assertThat(value.toSortedSet(Integer::compareTo)).isNotSameAs(value);
        }

        @Test
        public void shouldPreserveComparatorOnConvertToSortedSetWithoutDistinctComparator() {
            final TreeSet<Integer> value = TreeSet.of(Comparators.naturalComparator().reversed(), 1, 2, 3);
            assertThat(value.toSortedSet().mkString(",")).isEqualTo("3,2,1");
        }
    }

    // -- helpers

    private static Comparator<Integer> inverseIntComparator() {
        return (i1, i2) -> Integer.compare(i2, i1);
    }

    // -- ignored tests

    @Test
    @Disabled
    public void shouldCalculateAverageOfDoubleAndFloat() {
        // it is not possible to create a TreeSet containing unrelated types
    }

    // -- null elements are rejected at construction (design 3.9)

    @Test
    public void shouldRejectNullElementOnOf() {
        assertThatNullPointerException().isThrownBy(() -> of(nullsFirst(Comparators.naturalComparator()), (Integer) null));
    }

    @Test
    public void shouldRejectNullElementOnAdd() {
        assertThatNullPointerException().isThrownBy(() -> TreeSet.<Integer>empty(nullsFirst(Comparators.naturalComparator())).add(null));
    }

    @Nested
    class CollectTests {

        @Test
        public void shouldCollectWithAComparator() {
            final TreeSet<String> actual = TreeSet.of(1, 2, 3)
              .collect(java.util.Comparator.reverseOrder(), i -> i == 2 ? Option.<String>none() : Option.some("v" + i));
            assertThat(actual.mkString()).isEqualTo("v3v1");
            assertThat(actual.comparator().compare("a", "b")).isGreaterThan(0);
        }

        @Test
        public void shouldCollectWithTheNaturalOrder() {
            assertThat(TreeSet.of(3, 1, 2).collect(i -> i == 2 ? Option.<String>none() : Option.some("v" + i)).mkString()).isEqualTo("v1v3");
        }

        @Test
        public void shouldThrowOnCollectWithNullComparator() {
            org.junit.jupiter.api.Assertions.assertThrows(NullPointerException.class, () -> TreeSet.of(1).collect(null, i -> Option.some(i)));
        }
    }

    // -- average

    @TestTemplate
    public void shouldReturnNoneWhenComputingAverageOfNil() {
        assertThat(empty().average()).isEqualTo(Option.none());
    }

    @TestTemplate
    public void shouldThrowWhenComputingAverageOfStrings() {
        assertThrows(UnsupportedOperationException.class, () -> of("1", "2", "3").average());
    }

    @TestTemplate
    public void shouldComputeAverageOfByte() {
        assertThat(of((byte) 1, (byte) 2).average().get()).isEqualTo(1.5);
    }

    @TestTemplate
    public void shouldComputeAverageOfDouble() {
        assertThat(of(.1, .2, .3).average().get()).isEqualTo(.2, within(10e-17));
    }

    @TestTemplate
    public void shouldComputeAverageOfFloat() {
        assertThat(of(.1f, .2f, .3f).average().get()).isEqualTo(.2, within(10e-9));
    }

    @TestTemplate
    public void shouldComputeAverageOfInt() {
        assertThat(of(1, 2, 3).average().get()).isEqualTo(2);
    }

    @TestTemplate
    public void shouldComputeAverageOfLong() {
        assertThat(of(1L, 2L, 3L).average().get()).isEqualTo(2);
    }

    @TestTemplate
    public void shouldComputeAverageOfShort() {
        assertThat(of((short) 1, (short) 2, (short) 3).average().get()).isEqualTo(2);
    }

    @TestTemplate
    public void shouldComputeAverageOfBigInteger() {
        assertThat(of(BigInteger.ZERO, BigInteger.ONE).average().get()).isEqualTo(.5);
    }

    @TestTemplate
    public void shouldComputeAverageOfBigDecimal() {
        assertThat(of(BigDecimal.ZERO, BigDecimal.ONE).average().get()).isEqualTo(.5);
    }

    @TestTemplate
    public void shouldComputeAverageAndCompensateErrors() {
        // Kahan's summation algorithm (used by DoubleStream.average()) returns 0.0 (false)
        // Neumaier's modification of Kahan's algorithm returns 0.75 (correct)
        assertThat(of(1.0, +10e100, 2.0, -10e100).average().get()).isEqualTo(0.75);
    }

    @TestTemplate
    public void shouldCalculateAverageOfDoublesContainingNaN() {
        assertThat(of(1.0, Double.NaN, 2.0).average().get()).isNaN();
    }

    @TestTemplate
    public void shouldCalculateAverageOfFloatsContainingNaN() {
        assertThat(of(1.0f, Float.NaN, 2.0f).average().get()).isNaN();
    }

    @TestTemplate
    public void shouldCalculateAverageOfDoublePositiveAndNegativeInfinity() {
        assertThat(of(Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY).average().get()).isNaN();
    }

    @TestTemplate
    public void shouldCalculateAverageOfFloatPositiveAndNegativeInfinity() {
        assertThat(of(Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY).average().get()).isNaN();
    }

    // -- existsUnique

    @TestTemplate
    public void shouldBeAwareOfExistingUniqueElement() {
        assertThat(of(1, 2).existsUnique(i -> i == 1)).isTrue();
    }

    @TestTemplate
    public void shouldBeAwareOfNonExistingUniqueElement() {
        assertThat(this.<Integer>empty().existsUnique(i -> i == 1)).isFalse();
    }

    // -- filter

    @TestTemplate
    public void shouldFilterExistingElements() {
        assertThat(of(1, 2, 3).filter(i -> i == 1)).isEqualTo(of(1));
        assertThat(of(1, 2, 3).filter(i -> i == 2)).isEqualTo(of(2));
        assertThat(of(1, 2, 3).filter(i -> i == 3)).isEqualTo(of(3));
        if (useIsEqualToInsteadOfIsSameAs()) {
            assertThat(of(1, 2, 3).filter(ignore -> true)).isEqualTo(of(1, 2, 3));
        } else {
            final Set<Integer> t = of(1, 2, 3);
            assertThat(t.filter(ignore -> true)).isSameAs(t);
        }
    }

    @TestTemplate
    public void shouldFilterNonExistingElements() {
        if (useIsEqualToInsteadOfIsSameAs()) {
            assertThat(this.<Integer>empty().filter(i -> i == 0)).isEqualTo(empty());
            assertThat(of(1, 2, 3).filter(i -> i == 0)).isEqualTo(empty());
        } else {
            assertThat(this.<Integer>empty().filter(i -> i == 0)).isSameAs(empty());
            assertThat(of(1, 2, 3).filter(i -> i == 0)).isSameAs(empty());
        }
    }

    @TestTemplate
    public void shouldReturnSameInstanceWhenFilteringEmptyTraversable() {
        final Set<?> empty = empty();
        assertThat(empty.filter(v -> true)).isSameAs(empty);
    }

    // -- reject

    @TestTemplate
    public void shouldRejectExistingElements() {
        assertThat(of(1, 2, 3).reject(i -> i == 1)).isEqualTo(of(2, 3));
        assertThat(of(1, 2, 3).reject(i -> i == 2)).isEqualTo(of(1, 3));
        assertThat(of(1, 2, 3).reject(i -> i == 3)).isEqualTo(of(1, 2));
        if (useIsEqualToInsteadOfIsSameAs()) {
            assertThat(of(1, 2, 3).reject(ignore -> false)).isEqualTo(of(1, 2, 3));
        } else {
            final Set<Integer> t = of(1, 2, 3);
            assertThat(t.reject(ignore -> false)).isSameAs(t);
        }
    }

    @TestTemplate
    public void shouldRejectNonExistingElements() {
        if (useIsEqualToInsteadOfIsSameAs()) {
            assertThat(this.<Integer>empty().reject(i -> i == 0)).isEqualTo(empty());
            assertThat(of(1, 2, 3).reject(i -> i > 0)).isEqualTo(empty());
        } else {
            assertThat(this.<Integer>empty().reject(i -> i == 0)).isSameAs(empty());
            assertThat(of(1, 2, 3).reject(i -> i > 0)).isSameAs(empty());
        }
    }

    @TestTemplate
    public void shouldReturnSameInstanceWhenRejectingEmptyTraversable() {
        final Set<?> empty = empty();
        assertThat(empty.reject(v -> true)).isSameAs(empty);
    }

    // -- flatMap

    @TestTemplate
    public void shouldFlatMapEmpty() {
        if (useIsEqualToInsteadOfIsSameAs()) {
            assertThat(empty().flatMap(v -> of(v, 0))).isEqualTo(empty());
        } else {
            assertThat(empty().flatMap(v -> of(v, 0))).isSameAs(empty());
        }
    }

    @TestTemplate
    public void shouldFlatMapNonEmpty() {
        assertThat(of(1, 2, 3).flatMap(v -> of(v, 0))).isEqualTo(of(1, 0, 2, 0, 3, 0));
    }

    // -- collect

    @TestTemplate
    public void shouldCollectNothingFromEmpty() {
        final AtomicInteger calls = new AtomicInteger();
        final Set<Integer> actual = this.<Integer>empty().collect(i -> {
            calls.incrementAndGet();
            return Option.some(i);
        });
        assertThat(actual).isEqualTo(empty());
        assertThat(calls.get()).isEqualTo(0);
    }

    @TestTemplate
    public void shouldCollectNothingWhenEveryElementIsDropped() {
        assertThat(of(1, 2, 3).collect(i -> Option.none())).isEqualTo(empty());
    }

    @TestTemplate
    public void shouldCollectEveryElementWhenEveryElementIsKept() {
        assertThat(of(1, 2, 3).collect(i -> Option.some(i * 10))).isEqualTo(of(10, 20, 30));
    }

    @TestTemplate
    public void shouldCollectTheKeptElementsInOrder() {
        assertThat(of(1, 2, 3, 4).collect(i -> i % 2 == 0 ? Option.some("e" + i) : Option.none())).isEqualTo(of("e2", "e4"));
    }

    @TestTemplate
    public void shouldCollectWithASwitchInsideTheLambda() {
        final Set<Integer> actual = of(1, 2, 3).collect(i -> switch (i) {
            case Integer odd when odd % 2 == 1 -> Option.some(odd * 10);
            default -> Option.none();
        });
        assertThat(actual).isEqualTo(of(10, 30));
    }

    @TestTemplate
    public void shouldCallTheCollectMapperOncePerElement() {
        final AtomicInteger calls = new AtomicInteger();
        of(1, 2, 3).collect(i -> {
            calls.incrementAndGet();
            return i == 2 ? Option.none() : Option.some(i);
        }).size();
        assertThat(calls.get()).isEqualTo(3);
    }

    @TestTemplate
    public void shouldRejectNullOptionFromCollectMapper() {
        // the message names the concrete type, which toString prints before the parenthesis (List, IntMap, HashSet...)
        final String type = of(1).toString().substring(0, of(1).toString().indexOf('('));
        assertThatThrownBy(() -> of(1).collect(i -> null).size())
          .isInstanceOf(NullPointerException.class)
          .hasMessage(type + ".collect: mapper returned null");
    }

    @TestTemplate
    public void shouldThrowOnCollectWithNullMapper() {
        final Function<Integer, Option<Integer>> mapper = null;
        assertThrows(NullPointerException.class, () -> of(1).collect(mapper));
    }

    // -- fold

    @TestTemplate
    public void shouldFoldNil() {
        assertThat(this.<String>empty().fold("", (a, b) -> a + b)).isEqualTo("");
    }

    @TestTemplate
    public void shouldThrowWhenFoldNullOperator() {
        assertThrows(NullPointerException.class, () -> this.<String>empty().fold(null, null));
    }

    @TestTemplate
    public void shouldFoldSingleElement() {
        assertThat(of(1).fold(0, (a, b) -> a + b)).isEqualTo(1);
    }

    @TestTemplate
    public void shouldFoldMultipleElements() {
        assertThat(of(1, 2, 3).fold(0, (a, b) -> a + b)).isEqualTo(6);
    }

    // -- groupBy

    @TestTemplate
    public void shouldNilGroupBy() {
        assertThat(empty().groupBy(Function.identity())).isEqualTo(LinkedHashMap.empty());
    }

    @TestTemplate
    public void shouldNonNilGroupByIdentity() {
        final Map<?, ?> actual = of('a', 'b', 'c').groupBy(Function.identity());
        final Map<?, ?> expected = LinkedHashMap.empty().put('a', of('a')).put('b', of('b')).put('c', of('c'));
        assertThat(actual).isEqualTo(expected);
    }

    @TestTemplate
    public void shouldNonNilGroupByEqual() {
        final Map<?, ?> actual = of('a', 'b', 'c').groupBy(c -> 1);
        final Map<?, ?> expected = LinkedHashMap.empty().put(1, of('a', 'b', 'c'));
        assertThat(actual).isEqualTo(expected);
    }

    // -- arrangeBy

    @TestTemplate
    public void shouldNilArrangeBy() {
        assertThat(empty().arrangeBy(Function.identity())).isEqualTo(Option.some(LinkedHashMap.empty()));
    }

    @TestTemplate
    public void shouldNonNilArrangeByIdentity() {
        final Option<Map<Character, Character>> actual = of('a', 'b', 'c').arrangeBy(Function.identity());
        final Option<Map<?, ?>> expected = Option.some(LinkedHashMap.empty().put('a', 'a').put('b', 'b').put('c', 'c'));
        assertThat(actual).isEqualTo(expected);
    }

    @TestTemplate
    public void shouldNonNilArrangeByEqual() {
        final Option<Map<Integer, Character>> actual = of('a', 'b', 'c').arrangeBy(c -> 1);
        final Option<Map<?, ?>> expected = Option.none();
        assertThat(actual).isEqualTo(expected);
    }

    // -- max

    @TestTemplate
    public void shouldReturnNoneWhenComputingMaxOfNil() {
        assertThat(empty().max()).isEqualTo(Option.none());
    }

    @TestTemplate
    public void shouldComputeMaxOfOneValue() {
        assertThat(of(5).max()).isEqualTo(Option.some(5));
    }

    @TestTemplate
    public void shouldComputeMaxOfStrings() {
        assertThat(of("1", "2", "3").max()).isEqualTo(Option.some("3"));
    }

    @TestTemplate
    public void shouldComputeMaxOfBoolean() {
        assertThat(of(true, false).max()).isEqualTo(Option.some(true));
    }

    @TestTemplate
    public void shouldComputeMaxOfByte() {
        assertThat(of((byte) 1, (byte) 2).max()).isEqualTo(Option.some((byte) 2));
    }

    @TestTemplate
    public void shouldComputeMaxOfChar() {
        assertThat(of('a', 'b', 'c').max()).isEqualTo(Option.some('c'));
    }

    @TestTemplate
    public void shouldComputeMaxOfDouble() {
        assertThat(of(.1, .2, .3).max()).isEqualTo(Option.some(.3));
    }

    @TestTemplate
    public void shouldComputeMaxOfFloat() {
        assertThat(of(.1f, .2f, .3f).max()).isEqualTo(Option.some(.3f));
    }

    @TestTemplate
    public void shouldComputeMaxOfInt() {
        assertThat(of(1, 2, 3).max()).isEqualTo(Option.some(3));
    }

    @TestTemplate
    public void shouldComputeMaxOfLong() {
        assertThat(of(1L, 2L, 3L).max()).isEqualTo(Option.some(3L));
    }

    @TestTemplate
    public void shouldComputeMaxOfShort() {
        assertThat(of((short) 1, (short) 2, (short) 3).max()).isEqualTo(Option.some((short) 3));
    }

    @TestTemplate
    public void shouldComputeMaxOfBigInteger() {
        assertThat(of(BigInteger.ZERO, BigInteger.ONE).max()).isEqualTo(Option.some(BigInteger.ONE));
    }

    @TestTemplate
    public void shouldComputeMaxOfBigDecimal() {
        assertThat(of(BigDecimal.ZERO, BigDecimal.ONE).max()).isEqualTo(Option.some(BigDecimal.ONE));
    }

    @TestTemplate
    public void shouldThrowNPEWhenMaxOfNullAndInt() {
        assertThrows(NullPointerException.class, () -> of(null, 1).max());
    }

    @TestTemplate
    public void shouldThrowNPEWhenMaxOfIntAndNull() {
        assertThrows(NullPointerException.class, () -> of(1, null).max());
    }

    @TestTemplate
    public void shouldCalculateMaxOfDoublesContainingNaN() {
        assertThat(of(1.0, Double.NaN, 2.0).max().get()).isNaN();
    }

    @TestTemplate
    public void shouldCalculateMaxOfFloatsContainingNaN() {
        assertThat(of(1.0f, Float.NaN, 2.0f).max().get()).isEqualTo(Float.NaN);
    }

    @TestTemplate
    public void shouldThrowClassCastExceptionWhenTryingToCalculateMaxOfDoubleAndFloat() {
        assertThatThrownBy(() -> this.<Number>of(1.0, 1.0f).max()).isInstanceOf(ClassCastException.class);
    }

    @TestTemplate
    public void shouldCalculateMaxOfDoublePositiveAndNegativeInfinity() {
        assertThat(of(Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY).max()
          .get()).isEqualTo(Double.POSITIVE_INFINITY);
    }

    @TestTemplate
    public void shouldCalculateMaxOfFloatPositiveAndNegativeInfinity() {
        assertThat(of(Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY).max().get()).isEqualTo(Float.POSITIVE_INFINITY);
    }

    // -- maxBy(Comparator)

    @TestTemplate
    public void shouldThrowWhenMaxByWithNullComparator() {
        assertThrows(NullPointerException.class, () -> of(1).maxBy((Comparator<Integer>) null));
    }

    @TestTemplate
    public void shouldThrowWhenMaxByOfNil() {
        assertThat(empty().maxBy((o1, o2) -> 0)).isEqualTo(Option.none());
    }

    @TestTemplate
    public void shouldCalculateMaxByOfInts() {
        assertThat(of(1, 2, 3).maxBy(comparingInt(i -> i))).isEqualTo(Option.some(3));
    }

    @TestTemplate
    public void shouldCalculateInverseMaxByOfInts() {
        assertThat(of(1, 2, 3).maxBy((i1, i2) -> i2 - i1)).isEqualTo(Option.some(1));
    }

    // -- maxBy(Function)

    @TestTemplate
    public void shouldThrowWhenMaxByWithNullFunction() {
        assertThrows(NullPointerException.class, () -> of(1).maxBy((Function<Integer, Integer>) null));
    }

    @TestTemplate
    public void shouldThrowWhenMaxByFunctionOfNil() {
        assertThat(this.<Integer>empty().maxBy(i -> i)).isEqualTo(Option.none());
    }

    @TestTemplate
    public void shouldCalculateMaxByFunctionOfInts() {
        assertThat(of(1, 2, 3).maxBy(i -> i)).isEqualTo(Option.some(3));
    }

    @TestTemplate
    public void shouldCalculateInverseMaxByFunctionOfInts() {
        assertThat(of(1, 2, 3).maxBy(i -> -i)).isEqualTo(Option.some(1));
    }

    @TestTemplate
    public void shouldCallMaxFunctionOncePerElement() {
        final int[] cnt = {0};
        assertThat(of(1, 2, 3).maxBy(i -> {
            cnt[0]++;
            return i;
        })).isEqualTo(Option.some(3));
        assertThat(cnt[0]).isEqualTo(3);
    }

    // -- min

    @TestTemplate
    public void shouldReturnNoneWhenComputingMinOfNil() {
        assertThat(empty().min()).isEqualTo(Option.none());
    }

    @TestTemplate
    public void shouldComputeMinOfOneValue() {
        assertThat(of(5).min()).isEqualTo(Option.some(5));
    }

    @TestTemplate
    public void shouldComputeMinOfStrings() {
        assertThat(of("1", "2", "3").min()).isEqualTo(Option.some("1"));
    }

    @TestTemplate
    public void shouldComputeMinOfBoolean() {
        assertThat(of(true, false).min()).isEqualTo(Option.some(false));
    }

    @TestTemplate
    public void shouldComputeMinOfByte() {
        assertThat(of((byte) 1, (byte) 2).min()).isEqualTo(Option.some((byte) 1));
    }

    @TestTemplate
    public void shouldComputeMinOfChar() {
        assertThat(of('a', 'b', 'c').min()).isEqualTo(Option.some('a'));
    }

    @TestTemplate
    public void shouldComputeMinOfDouble() {
        assertThat(of(.1, .2, .3).min()).isEqualTo(Option.some(.1));
    }

    @TestTemplate
    public void shouldComputeMinOfFloat() {
        assertThat(of(.1f, .2f, .3f).min()).isEqualTo(Option.some(.1f));
    }

    @TestTemplate
    public void shouldComputeMinOfInt() {
        assertThat(of(1, 2, 3).min()).isEqualTo(Option.some(1));
    }

    @TestTemplate
    public void shouldComputeMinOfLong() {
        assertThat(of(1L, 2L, 3L).min()).isEqualTo(Option.some(1L));
    }

    @TestTemplate
    public void shouldComputeMinOfShort() {
        assertThat(of((short) 1, (short) 2, (short) 3).min()).isEqualTo(Option.some((short) 1));
    }

    @TestTemplate
    public void shouldComputeMinOfBigInteger() {
        assertThat(of(BigInteger.ZERO, BigInteger.ONE).min()).isEqualTo(Option.some(BigInteger.ZERO));
    }

    @TestTemplate
    public void shouldComputeMinOfBigDecimal() {
        assertThat(of(BigDecimal.ZERO, BigDecimal.ONE).min()).isEqualTo(Option.some(BigDecimal.ZERO));
    }

    @TestTemplate
    public void shouldThrowNPEWhenMinOfNullAndInt() {
        assertThrows(NullPointerException.class, () -> of(null, 1).min());
    }

    @TestTemplate
    public void shouldThrowNPEWhenMinOfIntAndNull() {
        assertThrows(NullPointerException.class, () -> of(1, null).min());
    }

    @TestTemplate
    public void shouldCalculateMinOfDoublesContainingNaN() {
        assertThat(of(1.0, Double.NaN, 2.0).min().get()).isNaN();
    }

    @TestTemplate
    public void shouldCalculateMinOfFloatsContainingNaN() {
        assertThat(of(1.0f, Float.NaN, 2.0f).min().get()).isEqualTo(Float.NaN);
    }

    @TestTemplate
    public void shouldThrowClassCastExceptionWhenTryingToCalculateMinOfDoubleAndFloat() {
        assertThatThrownBy(() -> this.<Number>of(1.0, 1.0f).min()).isInstanceOf(ClassCastException.class);
    }

    @TestTemplate
    public void shouldCalculateMinOfDoublePositiveAndNegativeInfinity() {
        assertThat(of(Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY).min()
          .get()).isEqualTo(Double.NEGATIVE_INFINITY);
    }

    @TestTemplate
    public void shouldCalculateMinOfFloatPositiveAndNegativeInfinity() {
        assertThat(of(Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY).min()
          .get()).isEqualTo(Double.NEGATIVE_INFINITY);
    }

    // -- minBy(Comparator)

    @TestTemplate
    public void shouldThrowWhenMinByWithNullComparator() {
        assertThrows(NullPointerException.class, () -> of(1).minBy((Comparator<Integer>) null));
    }

    @TestTemplate
    public void shouldThrowWhenMinByOfNil() {
        assertThat(empty().minBy((o1, o2) -> 0)).isEqualTo(Option.none());
    }

    @TestTemplate
    public void shouldCalculateMinByOfInts() {
        assertThat(of(1, 2, 3).minBy(comparingInt(i -> i))).isEqualTo(Option.some(1));
    }

    @TestTemplate
    public void shouldCalculateInverseMinByOfInts() {
        assertThat(of(1, 2, 3).minBy((i1, i2) -> i2 - i1)).isEqualTo(Option.some(3));
    }

    // -- minBy(Function)

    @TestTemplate
    public void shouldThrowWhenMinByWithNullFunction() {
        assertThrows(NullPointerException.class, () -> of(1).minBy((Function<Integer, Integer>) null));
    }

    @TestTemplate
    public void shouldThrowWhenMinByFunctionOfNil() {
        assertThat(this.<Integer>empty().minBy(i -> i)).isEqualTo(Option.none());
    }

    @TestTemplate
    public void shouldCalculateMinByFunctionOfInts() {
        assertThat(of(1, 2, 3).minBy(i -> i)).isEqualTo(Option.some(1));
    }

    @TestTemplate
    public void shouldCalculateInverseMinByFunctionOfInts() {
        assertThat(of(1, 2, 3).minBy(i -> -i)).isEqualTo(Option.some(3));
    }

    @TestTemplate
    public void shouldCallMinFunctionOncePerElement() {
        final int[] cnt = {0};
        assertThat(of(1, 2, 3).minBy(i -> {
            cnt[0]++;
            return i;
        })).isEqualTo(Option.some(1));
        assertThat(cnt[0]).isEqualTo(3);
    }

    // -- orElse

    @TestTemplate
    public void shouldCaclEmptyOrElseSameOther() {
        final Iterable<Integer> other = of(42);
        assertThat(empty().orElse(other)).isSameAs(other);
    }

    @TestTemplate
    public void shouldCaclEmptyOrElseEqualOther() {
        assertThat(empty().orElse(Arrays.asList(1, 2))).isEqualTo(of(1, 2));
    }

    @TestTemplate
    public void shouldCaclNonemptyOrElseOther() {
        final Set<Integer> src = of(42);
        assertThat(src.orElse(List.of(1))).isSameAs(src);
    }

    @TestTemplate
    public void shouldCaclEmptyOrElseSameSupplier() {
        final Iterable<Integer> other = of(42);
        final Supplier<Iterable<Integer>> supplier = () -> other;
        assertThat(empty().orElse(supplier)).isSameAs(other);
    }

    @TestTemplate
    public void shouldCaclEmptyOrElseEqualSupplier() {
        assertThat(empty().orElse(() -> Arrays.asList(1, 2))).isEqualTo(of(1, 2));
    }

    @TestTemplate
    public void shouldCaclNonemptyOrElseSupplier() {
        final Set<Integer> src = of(42);
        assertThat(src.orElse(() -> List.of(1))).isSameAs(src);
    }

    // -- partition

    @TestTemplate
    public void shouldThrowWhenPartitionNilAndPredicateIsNull() {
        assertThrows(NullPointerException.class, () -> empty().partition(null));
    }

    @TestTemplate
    public void shouldPartitionNil() {
        assertThat(empty().partition(e -> true)).isEqualTo(Tuple.of(empty(), empty()));
    }

    @TestTemplate
    public void shouldPartitionIntsInOddAndEvenHavingOddAndEvenNumbers() {
        assertThat(of(1, 2, 3, 4).partition(i -> i % 2 != 0)).isEqualTo(Tuple.of(of(1, 3), of(2, 4)));
    }

    @TestTemplate
    public void shouldPartitionIntsInOddAndEvenHavingOnlyOddNumbers() {
        assertThat(of(1, 3).partition(i -> i % 2 != 0)).isEqualTo(Tuple.of(of(1, 3), empty()));
    }

    @TestTemplate
    public void shouldPartitionIntsInOddAndEvenHavingOnlyEvenNumbers() {
        assertThat(of(2, 4).partition(i -> i % 2 != 0)).isEqualTo(Tuple.of(empty(), of(2, 4)));
    }

    // -- product

    @TestTemplate
    public void shouldComputeProductOfNil() {
        assertThat(empty().product()).isEqualTo(1);
    }

    @TestTemplate
    public void shouldThrowWhenComputingProductOfStrings() {
        assertThrows(UnsupportedOperationException.class, () -> of("1", "2", "3").product());
    }

    @TestTemplate
    public void shouldComputeProductOfByte() {
        assertThat(of((byte) 1, (byte) 2).product()).isEqualTo(2L);
    }

    @TestTemplate
    public void shouldComputeProductOfDouble() {
        assertThat(of(.1, .2, .3).product().doubleValue()).isEqualTo(.006, within(10e-18));
    }

    @TestTemplate
    public void shouldComputeProductOfFloat() {
        assertThat(of(.1f, .2f, .3f).product().doubleValue()).isEqualTo(.006, within(10e-10));
    }

    @TestTemplate
    public void shouldComputeProductOfInt() {
        assertThat(of(1, 2, 3).product()).isEqualTo(6L);
    }

    @TestTemplate
    public void shouldComputeProductOfLong() {
        assertThat(of(1L, 2L, 3L).product()).isEqualTo(6L);
    }

    @TestTemplate
    public void shouldComputeProductOfShort() {
        assertThat(of((short) 1, (short) 2, (short) 3).product()).isEqualTo(6L);
    }

    @TestTemplate
    public void shouldComputeProductOfBigInteger() {
        assertThat(of(BigInteger.ZERO, BigInteger.ONE).product()).isEqualTo(BigInteger.ZERO);
    }

    @TestTemplate
    public void shouldComputeProductOfBigDecimal() {
        assertThat(of(BigDecimal.ZERO, BigDecimal.ONE).product()).isEqualTo(BigDecimal.ZERO);
    }

    // -- reduceOption

    @TestTemplate
    public void shouldThrowWhenReduceOptionNil() {
        assertThat(this.<String>empty().reduceOption((a, b) -> a + b)).isSameAs(Option.none());
    }

    @TestTemplate
    public void shouldThrowWhenReduceOptionNullOperator() {
        assertThrows(NullPointerException.class, () -> this.<String>empty().reduceOption(null));
    }

    @TestTemplate
    public void shouldReduceOptionNonNil() {
        assertThat(of(1, 2, 3).reduceOption((a, b) -> a + b)).isEqualTo(Option.some(6));
    }

    // -- reduce

    @TestTemplate
    public void shouldThrowWhenReduceNil() {
        assertThrows(NoSuchElementException.class, () -> this.<String>empty().reduce((a, b) -> a + b));
    }

    @TestTemplate
    public void shouldThrowWhenReduceNullOperator() {
        assertThrows(NullPointerException.class, () -> this.<String>empty().reduce(null));
    }

    @TestTemplate
    public void shouldReduceNonNil() {
        assertThat(of(1, 2, 3).reduce((a, b) -> a + b)).isEqualTo(6);
    }

    // -- replace(curr, new)

    @TestTemplate
    public void shouldReplaceElementOfNilUsingCurrNew() {
        if (useIsEqualToInsteadOfIsSameAs()) {
            assertThat(this.<Integer>empty().replace(1, 2)).isEqualTo(empty());
        } else {
            assertThat(this.<Integer>empty().replace(1, 2)).isSameAs(empty());
        }
    }

    @TestTemplate
    public void shouldReplaceElementOfNonNilUsingCurrNewWhenOneOccurrenceExists() {
        assertThat(of(0, 1, 2).replace(1, 3)).isEqualTo(of(0, 3, 2));
    }

    @TestTemplate
    public void shouldReplaceElementOfNonNilUsingCurrNewWhenNoOccurrenceExists() {
        if (useIsEqualToInsteadOfIsSameAs()) {
            assertThat(of(0, 1, 2).replace(33, 3)).isEqualTo(of(0, 1, 2));
        } else {
            final Set<Integer> src = of(0, 1, 2);
            assertThat(src.replace(33, 3)).isSameAs(src);
        }
    }

    // -- replaceAll(curr, new)

    @TestTemplate
    public void shouldReplaceAllElementsOfNilUsingCurrNew() {
        if (useIsEqualToInsteadOfIsSameAs()) {
            assertThat(this.<Integer>empty().replaceAll(1, 2)).isEqualTo(empty());
        } else {
            assertThat(this.<Integer>empty().replaceAll(1, 2)).isSameAs(empty());
        }
    }

    @TestTemplate
    public void shouldReplaceAllElementsOfNonNilUsingCurrNonExistingNew() {
        if (useIsEqualToInsteadOfIsSameAs()) {
            assertThat(of(0, 1, 2, 1).replaceAll(33, 3)).isEqualTo(of(0, 1, 2, 1));
        } else {
            final Set<Integer> src = of(0, 1, 2, 1);
            assertThat(src.replaceAll(33, 3)).isSameAs(src);
        }
    }

    @TestTemplate
    public void shouldReplaceAllElementsOfNonNilUsingCurrNew() {
        assertThat(of(0, 1, 2, 1).replaceAll(1, 3)).isEqualTo(of(0, 3, 2, 3));
    }

    // -- retainAll

    @TestTemplate
    public void shouldRetainAllElementsFromNil() {
        final Set<Object> empty = empty();
        final Set<Object> actual = empty.retainAll(of(1, 2, 3));
        if (useIsEqualToInsteadOfIsSameAs()) {
            assertThat(actual).isEqualTo(empty);
        } else {
            assertThat(actual).isSameAs(empty);
        }
    }

    @TestTemplate
    public void shouldRetainAllExistingElementsFromNonNil() {
        final Set<Integer> src = of(1, 2, 3, 2, 1, 3);
        final Set<Integer> expected = of(1, 2, 2, 1);
        final Set<Integer> actual = src.retainAll(of(1, 2));
        assertThat(actual).isEqualTo(expected);
    }

    @TestTemplate
    public void shouldRetainAllElementsFromNonNil() {
        final Set<Integer> src = of(1, 2, 1, 2, 2);
        final Set<Integer> expected = of(1, 2, 1, 2, 2);
        final Set<Integer> actual = src.retainAll(of(1, 2));
        if (useIsEqualToInsteadOfIsSameAs()) {
            assertThat(actual).isEqualTo(expected);
        } else {
            assertThat(actual).isSameAs(src);
        }
    }

    @TestTemplate
    public void shouldNotRetainAllNonExistingElementsFromNonNil() {
        final Set<Integer> src = of(1, 2, 3);
        final Set<Object> expected = empty();
        final Set<Integer> actual = src.retainAll(of(4, 5));
        if (useIsEqualToInsteadOfIsSameAs()) {
            assertThat(actual).isEqualTo(expected);
        } else {
            assertThat(actual).isSameAs(expected);
        }
    }

    // -- sum

    @TestTemplate
    public void shouldComputeSumOfNil() {
        assertThat(empty().sum()).isEqualTo(0);
    }

    @TestTemplate
    public void shouldThrowWhenComputingSumOfStrings() {
        assertThrows(UnsupportedOperationException.class, () -> of("1", "2", "3").sum());
    }

    @TestTemplate
    public void shouldComputeSumOfByte() {
        assertThat(of((byte) 1, (byte) 2).sum()).isEqualTo(3L);
    }

    @TestTemplate
    public void shouldComputeSumOfDouble() {
        assertThat(of(.1, .2, .3).sum().doubleValue()).isEqualTo(.6, within(10e-16));
    }

    @TestTemplate
    public void shouldComputeSumOfFloat() {
        assertThat(of(.1f, .2f, .3f).sum().doubleValue()).isEqualTo(.6, within(10e-8));
    }

    @TestTemplate
    public void shouldComputeSumOfInt() {
        assertThat(of(1, 2, 3).sum()).isEqualTo(6L);
    }

    @TestTemplate
    public void shouldComputeSumOfLong() {
        assertThat(of(1L, 2L, 3L).sum()).isEqualTo(6L);
    }

    @TestTemplate
    public void shouldComputeSumOfShort() {
        assertThat(of((short) 1, (short) 2, (short) 3).sum()).isEqualTo(6L);
    }

    @TestTemplate
    public void shouldComputeSumOfBigInteger() {
        assertThat(of(BigInteger.ZERO, BigInteger.ONE).sum()).isEqualTo(BigInteger.ONE);
    }

    @TestTemplate
    public void shouldComputeSumOfBigDecimal() {
        assertThat(of(BigDecimal.ZERO, BigDecimal.ONE).sum()).isEqualTo(BigDecimal.ONE);
    }

    // -- toJavaList

    @TestTemplate
    public void shouldConvertNilToArrayList() {
        assertThat(this.<Integer>empty().toJavaList()).isEqualTo(new ArrayList<Integer>());
    }

    @TestTemplate
    public void shouldConvertNonNilToArrayList() {
        assertThat(of(1, 2, 3).toJavaList()).isEqualTo(asList(1, 2, 3));
    }

    // -- toJavaMap(Function)

    @TestTemplate
    public void shouldConvertNilToHashMap() {
        assertThat(this.<Integer>empty().toJavaMap(x -> Tuple.of(x, x))).isEqualTo(new java.util.HashMap<>());
    }

    @TestTemplate
    public void shouldConvertNonNilToHashMap() {
        final java.util.Map<Integer, Integer> expected = new java.util.HashMap<>();
        expected.put(1, 1);
        expected.put(2, 2);
        assertThat(of(1, 2).toJavaMap(x -> Tuple.of(x, x))).isEqualTo(expected);
    }

    // -- toJavaSet

    @TestTemplate
    public void shouldConvertNilToHashSet() {
        assertThat(this.<Integer>empty().toJavaSet()).isEqualTo(new java.util.HashSet<>());
    }

    @TestTemplate
    public void shouldConvertNonNilToHashSet() {
        final java.util.Set<Integer> expected = new java.util.HashSet<>();
        expected.add(2);
        expected.add(1);
        expected.add(3);
        assertThat(of(1, 2, 2, 3).toJavaSet()).containsExactlyInAnyOrderElementsOf(expected);
    }

    // -- as

    @TestTemplate
    public void shouldReplaceEveryElementWithAs() {
        assertThat(empty().as(1)).isEqualTo(empty().as(2));
        assertThat(of(2).as(1)).isEqualTo(of(3).as(1));
        assertThat(of(2).as(1)).isEqualTo(of(3).map(ignored -> 1));
        assertThat(of(3).as(2)).isEqualTo(of(1).map(ignored -> 2));
        assertThat(of(1, 2, 3).as("x")).isEqualTo(of("x", "x", "x"));
    }

    // -- tap

    @TestTemplate
    public void shouldTapNil() {
        assertThat(empty().tap(t -> {})).isEqualTo(empty());
    }

    @TestTemplate
    public void shouldTapNonNilPerformingNoAction() {
        assertThat(of(1).tap(t -> {})).isEqualTo(of(1));
    }

    @TestTemplate
    public void shouldTapSingleValuePerformingAnAction() {
        final int[] effect = {0};
        final Set<Integer> actual = of(1).tap(i -> effect[0] = i);
        assertThat(actual).isEqualTo(of(1));
        assertThat(effect[0]).isEqualTo(1);
    }

    @TestTemplate
    public void shouldTapEveryElement() {
        final int[] sum = {0};
        final Set<Integer> actual = of(1, 2, 3).tap(i -> sum[0] += i);
        assertThat(actual).isEqualTo(of(1, 2, 3)); // consumes every element in the lazy case
        assertThat(sum[0]).isEqualTo(6);
    }

    @TestTemplate
    public void shouldReturnThisOnTapOfEagerCollection() {
        final Set<Integer> testee = of(1, 2, 3);
        if (hasDefiniteSize()) {
            assertThat(testee.tap(i -> {})).isSameAs(testee);
        }
    }

    @TestTemplate
    public void shouldThrowOnTapWithNullAction() {
        assertThrows(NullPointerException.class, () -> of(1).tap(null));
    }

    @TestTemplate
    public void shouldPropagateWhatTheTapActionThrows() {
        assertThrows(IllegalStateException.class, () -> of(1, 2).tap(i -> {
            throw new IllegalStateException();
        }).size());
    }

    // -- collect(Collector)

    @TestTemplate
    public void shouldCollectWithACollector() {
        final java.util.List<Integer> actual = of(1, 2, 3).collect(java.util.stream.Collectors.toList());
        assertThat(actual).containsExactlyInAnyOrder(1, 2, 3);
    }

    @TestTemplate
    public void shouldCollectWithSupplierAccumulatorAndCombiner() {
        final ArrayList<Integer> actual = of(1, 2, 3).collect(ArrayList<Integer>::new, ArrayList::add, ArrayList::addAll);
        assertThat(actual).containsExactlyInAnyOrder(1, 2, 3);
    }

    // -- single

    @TestTemplate
    public void shouldSingleFailEmpty() {
        assertThrows(NoSuchElementException.class, () -> empty().single());
    }

    @TestTemplate
    public void shouldSingleFailTwo() {
        assertThrows(NoSuchElementException.class, () -> of(1, 2).single());
    }

    @TestTemplate
    public void shouldSingleWork() {
        assertThat(of(1).single()).isEqualTo(1);
    }

    // -- singleOption

    @TestTemplate
    public void shouldSingleOptionFailEmpty() {
        assertThat(empty().singleOption()).isEqualTo(Option.none());
    }

    @TestTemplate
    public void shouldSingleOptionFailTwo() {
        assertThat(of(1, 2).singleOption()).isEqualTo(Option.none());
    }

    @TestTemplate
    public void shouldSingleOptionWork() {
        assertThat(of(1).singleOption()).isEqualTo(Option.some(1));
    }

    // -- Conversions toXxx()

    @TestTemplate
    public void shouldConvertToHashMap() {
        assertThat(of(9, 5, 1).toMap(i -> Tuple.of(i, i))).isEqualTo(HashMap.of(1, 1, 5, 5, 9, 9));
        assertThat(empty().toMap(i -> Tuple.of(i, i))).isSameAs(HashMap.empty());
    }

    @TestTemplate
    public void shouldConvertToHashMapTwoFunctions() {
        assertThat(of(9, 5, 1).toMap(Function.identity(), Function.identity())).isEqualTo(HashMap.of(1, 1, 5, 5, 9, 9));
    }

    @TestTemplate
    public void shouldConvertToLinkedMap() {
        assertThat(of(1, 5, 9).toLinkedMap(i -> Tuple.of(i, i))).isEqualTo(LinkedHashMap.of(1, 1, 5, 5, 9, 9));
        assertThat(empty().toLinkedMap(i -> Tuple.of(i, i))).isSameAs(LinkedHashMap.empty());
    }

    @TestTemplate
    public void shouldConvertToLinkedMapTwoFunctions() {
        assertThat(of(1, 5, 9).toLinkedMap(Function.identity(), Function.identity())).isEqualTo(LinkedHashMap.of(1, 1, 5, 5, 9, 9));
    }

    @TestTemplate
    public void shouldConvertToSortedMap() {
        assertThat(of(9, 5, 1).toSortedMap(i -> Tuple.of(i, i))).isEqualTo(TreeMap.of(1, 1, 5, 5, 9, 9));
        assertThat(this.<Integer>empty().toSortedMap(i -> Tuple.of(i, i))).isEqualTo(TreeMap.empty());
    }

    @TestTemplate
    public void shouldConvertToSortedMapTwoFunctions() {
        assertThat(of(9, 5, 1).toSortedMap(Function.identity(), Function.identity())).isEqualTo(TreeMap.of(1, 1, 5, 5, 9, 9));
    }

    @TestTemplate
    public void shouldConvertToSortedMapWithComparator() {
        final Comparator<Integer> comparator = ((Comparator<Integer>) Integer::compareTo).reversed();
        assertThat(of(9, 5, 1).toSortedMap(comparator, i -> Tuple.of(i, i))).isEqualTo(TreeMap.of(comparator, 9, 9, 5, 5, 1, 1));
    }

    @TestTemplate
    public void shouldConvertToSortedMapTwoFunctionsWithComparator() {
        final Comparator<Integer> comparator = ((Comparator<Integer>) Integer::compareTo).reversed();
        assertThat(of(9, 5, 1).toSortedMap(comparator, Function.identity(), Function.identity())).isEqualTo(TreeMap.of(comparator, 9, 9, 5, 5, 1, 1));
    }

    @TestTemplate
    public void shouldConvertToQueue() {
        assertThat(of(1, 2, 3).toQueue()).isEqualTo(Queue.of(1, 2, 3));
        assertThat(empty().toQueue()).isSameAs(Queue.empty());
    }

    @TestTemplate
    public void shouldConvertToLinkedSet() {
        final Set<Integer> value = of(3, 7, 1, 15, 0);
        final Set<Integer> set = value.toLinkedSet();
        final List<Integer> itemsInOrder = value.toList();
        assertThat(set).isEqualTo(itemsInOrder.foldLeft(LinkedHashSet.empty(), LinkedHashSet::add));
        assertThat(empty().toLinkedSet()).isSameAs(LinkedHashSet.empty());
    }

    @TestTemplate
    public void shouldConvertToSortedSetWithoutComparatorOnComparable() {
        assertThat(of(3, 7, 1, 15, 0).toSortedSet()).isEqualTo(TreeSet.of(0, 1, 3, 7, 15));
    }

    @TestTemplate
    public void shouldConvertToSortedSet() {
        final Comparator<Integer> comparator = Comparator.comparingInt(Integer::bitCount);
        assertThat(of(3, 7, 1, 15, 0).toSortedSet(comparator.reversed())).isEqualTo(TreeSet.of(comparator.reversed(), 0, 1, 3, 7, 15));
    }

    @TestTemplate
    public void shouldConvertToStream() {
        assertThat(of(1, 2, 3).toStream()).isEqualTo(Stream.of(1, 2, 3));
        assertThat(empty().toStream()).isSameAs(Stream.empty());
    }

    @TestTemplate
    public void shouldConvertToJavaCollectionUsingSupplier() {
        final java.util.List<Integer> ints = of(1, 2, 3).toJavaCollection(ArrayList::new);
        assertThat(ints).isEqualTo(asList(1, 2, 3));
    }

    @TestTemplate
    public void shouldConvertToJavaList() {
        final java.util.List<Integer> list = of(1, 2, 3).toJavaList();
        assertThat(list).isEqualTo(asList(1, 2, 3));
        assertThat(empty().toJavaList()).isEmpty();
    }

    @TestTemplate
    public void shouldConvertToJavaListUsingSupplier() {
        final java.util.List<Integer> ints = of(1, 2, 3).toJavaList(ArrayList::new);
        assertThat(ints).isEqualTo(asList(1, 2, 3));
    }

    @TestTemplate
    public void shouldConvertToJavaMapUsingFunction() {
        final java.util.Map<Integer, Integer> map = of(1, 2, 3).toJavaMap(v -> Tuple.of(v, v));
        assertThat(map).isEqualTo(java.util.Map.of(1, 1, 2, 2, 3, 3));
        assertThat(empty().toJavaMap(v -> Tuple.of(v, v))).isEqualTo(java.util.Map.of());
    }

    @TestTemplate
    public void shouldConvertToJavaMapUsingSupplierAndFunction() {
        final java.util.Map<Integer, Integer> map = of(1, 2, 3).toJavaMap(java.util.HashMap::new, i -> Tuple.of(i, i));
        assertThat(map).isEqualTo(java.util.Map.of(1, 1, 2, 2, 3, 3));
    }

    @TestTemplate
    public void shouldConvertToJavaMapUsingSupplierAndTwoFunction() {
        final java.util.Map<Integer, String> map = of(1, 2, 3).toJavaMap(java.util.HashMap::new, Function.identity(), String::valueOf);
        assertThat(map).isEqualTo(java.util.Map.of(1, "1", 2, "2", 3, "3"));
    }

    @TestTemplate
    public void shouldConvertToJavaSet() {
        final java.util.Set<Integer> set = of(1, 2, 3).toJavaSet();
        assertThat(set).containsExactlyInAnyOrderElementsOf(java.util.Set.of(1, 2, 3));
        assertThat(empty().toJavaSet()).isEmpty();
    }

    @TestTemplate
    public void shouldConvertToJavaSetUsingSupplier() {
        final java.util.Set<Integer> set = of(1, 2, 3).toJavaSet(java.util.HashSet::new);
        assertThat(set).containsExactlyInAnyOrderElementsOf(java.util.Set.of(1, 2, 3));
    }

    @TestTemplate
    public void shouldConvertToJavaParallelStream() {
        final java.util.stream.Stream<Integer> s1 = of(1, 2, 3).toJavaParallelStream();
        assertThat(s1.isParallel()).isTrue();
        final java.util.stream.Stream<Integer> s2 = java.util.stream.Stream.of(1, 2, 3);
        assertThat(List.ofAll(s1::iterator)).isEqualTo(List.ofAll(s2::iterator));
    }

    // -- static range, rangeBy, rangeClosed, rangeClosedBy

    // ------------------------------------------------------------------------
    // static range, rangeBy, rangeClosed, rangeCloseBy tests
    //
    // Basically there are the following tests scenarios:
    //
    // * step == 0
    // * from == to, step < 0
    // * from == to, step > 0
    // * from < to, step < 0
    // * from < to, step > 0
    // * from > to, step < 0
    // * from > to, step > 0
    //
    // Additionally we have to test these special cases
    // (where MIN/MAX may also be NEGATIVE_INFINITY, POSITIVE_INFINITY):
    //
    // * from = MAX, to = MAX, step < 0
    // * from = MAX, to = MAX, step > 0
    // * from = MIN, to = MIN, step < 0
    // * from = MIN, to = MIN, step > 0
    // * from = MAX - x, to = MAX, step > 0, 0 < x < step
    // * from = MAX - step, to = MAX, step > 0
    // * from = MAX - x, to = MAX, step > 0, x > step
    // * from = MIN, to = MIN - x, step < 0, 0 > x > step
    // * from = MIN, to = MIN - step, step < 0
    // * from = MIN, to = MIN - x, step < 0, x < step
    //
    // All of these scenarios are multiplied with
    //
    // * the inclusive and exclusive case
    // * the with and without step case
    //
    // ------------------------------------------------------------------------

    @Nested
    class StaticRangeclosedTests {
        @Test
        public void shouldCreateRangeClosedWhereFromIsGreaterThanTo() {
            assertThat(rangeClosed('b', 'a')).isEmpty();
            assertThat(rangeClosed(1, 0)).isEmpty();
            assertThat(rangeClosed(1L, 0L)).isEmpty();
        }

        @Test
        public void shouldCreateRangeClosedWhereFromEqualsTo() {
            assertThat(rangeClosed('a', 'a')).isEqualTo(of('a'));
            assertThat(rangeClosed(0, 0)).isEqualTo(of(0));
            assertThat(rangeClosed(0L, 0L)).isEqualTo(of(0L));
        }

        @Test
        public void shouldCreateRangeClosedWhereFromIsLessThanTo() {
            assertThat(rangeClosed('a', 'c')).isEqualTo(of('a', 'b', 'c'));
            assertThat(rangeClosed(1, 3)).isEqualTo(of(1, 2, 3));
            assertThat(rangeClosed(1L, 3L)).isEqualTo(of(1L, 2L, 3L));
        }

        @Test
        public void shouldCreateRangeClosedWhereFromAndToEqualMIN_VALUE() {
            assertThat(rangeClosed(Character.MIN_VALUE, Character.MIN_VALUE)).isEqualTo(of(Character.MIN_VALUE));
            assertThat(rangeClosed(Integer.MIN_VALUE, Integer.MIN_VALUE)).isEqualTo(of(Integer.MIN_VALUE));
            assertThat(rangeClosed(Long.MIN_VALUE, Long.MIN_VALUE)).isEqualTo(of(Long.MIN_VALUE));
        }

        @Test
        public void shouldCreateRangeClosedWhereFromAndToEqualMAX_VALUE() {
            assertThat(rangeClosed(Character.MAX_VALUE, Character.MAX_VALUE)).isEqualTo(of(Character.MAX_VALUE));
            assertThat(rangeClosed(Integer.MAX_VALUE, Integer.MAX_VALUE)).isEqualTo(of(Integer.MAX_VALUE));
            assertThat(rangeClosed(Long.MAX_VALUE, Long.MAX_VALUE)).isEqualTo(of(Long.MAX_VALUE));
        }
    }

    @Nested
    class StaticRangeclosedbyTests {
        @Test
        public void shouldCreateRangeClosedByWhereFromIsGreaterThanToAndStepWrongDirection() {

            // char
            assertThat(rangeClosedBy('b', 'a', 1)).isEmpty();
            assertThat(rangeClosedBy('b', 'a', 3)).isEmpty();
            assertThat(rangeClosedBy('a', 'b', -1)).isEmpty();
            assertThat(rangeClosedBy('a', 'b', -3)).isEmpty();

            // double
            assertThat(rangeClosedBy(1.0, 0.0, 1.0)).isEmpty();
            assertThat(rangeClosedBy(1.0, 0.0, 3.0)).isEmpty();
            assertThat(rangeClosedBy(0.0, 1.0, -1.0)).isEmpty();
            assertThat(rangeClosedBy(0.0, 1.0, -3.0)).isEmpty();

            // int
            assertThat(rangeClosedBy(1, 0, 1)).isEmpty();
            assertThat(rangeClosedBy(1, 0, 3)).isEmpty();
            assertThat(rangeClosedBy(0, 1, -1)).isEmpty();
            assertThat(rangeClosedBy(0, 1, -3)).isEmpty();

            // long
            assertThat(rangeClosedBy(1L, 0L, 1L)).isEmpty();
            assertThat(rangeClosedBy(1L, 0L, 3L)).isEmpty();
            assertThat(rangeClosedBy(0L, 1L, -1L)).isEmpty();
            assertThat(rangeClosedBy(0L, 1L, -3L)).isEmpty();
        }

        @Test
        public void shouldCreateRangeClosedByWhereFromEqualsTo() {

            // char
            assertThat(rangeClosedBy('a', 'a', 1)).isEqualTo(of('a'));
            assertThat(rangeClosedBy('a', 'a', 3)).isEqualTo(of('a'));
            assertThat(rangeClosedBy('a', 'a', -1)).isEqualTo(of('a'));
            assertThat(rangeClosedBy('a', 'a', -3)).isEqualTo(of('a'));

            // double
            assertThat(rangeClosedBy(0.0, 0.0, 1.0)).isEqualTo(of(0.0));
            assertThat(rangeClosedBy(0.0, 0.0, 3.0)).isEqualTo(of(0.0));
            assertThat(rangeClosedBy(0.0, 0.0, -1.0)).isEqualTo(of(0.0));
            assertThat(rangeClosedBy(0.0, 0.0, -3.0)).isEqualTo(of(0.0));

            // int
            assertThat(rangeClosedBy(0, 0, 1)).isEqualTo(of(0));
            assertThat(rangeClosedBy(0, 0, 3)).isEqualTo(of(0));
            assertThat(rangeClosedBy(0, 0, -1)).isEqualTo(of(0));
            assertThat(rangeClosedBy(0, 0, -3)).isEqualTo(of(0));

            // long
            assertThat(rangeClosedBy(0L, 0L, 1L)).isEqualTo(of(0L));
            assertThat(rangeClosedBy(0L, 0L, 3L)).isEqualTo(of(0L));
            assertThat(rangeClosedBy(0L, 0L, -1L)).isEqualTo(of(0L));
            assertThat(rangeClosedBy(0L, 0L, -3L)).isEqualTo(of(0L));
        }

        @Test
        public void shouldCreateRangeClosedByWhereFromIsLessThanToAndStepCorrectDirection() {

            // char
            assertThat(rangeClosedBy('a', 'c', 1)).isEqualTo(of('a', 'b', 'c'));
            assertThat(rangeClosedBy('a', 'e', 2)).isEqualTo(of('a', 'c', 'e'));
            assertThat(rangeClosedBy('a', 'f', 2)).isEqualTo(of('a', 'c', 'e'));
            assertThat(rangeClosedBy((char) (Character.MAX_VALUE - 2), Character.MAX_VALUE, 3)).isEqualTo(of((char) (Character.MAX_VALUE - 2)));
            assertThat(rangeClosedBy((char) (Character.MAX_VALUE - 3), Character.MAX_VALUE, 3)).isEqualTo(of((char) (Character.MAX_VALUE - 3), Character.MAX_VALUE));
            assertThat(rangeClosedBy('c', 'a', -1)).isEqualTo(of('c', 'b', 'a'));
            assertThat(rangeClosedBy('e', 'a', -2)).isEqualTo(of('e', 'c', 'a'));
            assertThat(rangeClosedBy('e', (char) ('a' - 1), -2)).isEqualTo(of('e', 'c', 'a'));
            assertThat(rangeClosedBy((char) (Character.MIN_VALUE + 2), Character.MIN_VALUE, -3)).isEqualTo(of((char) (Character.MIN_VALUE + 2)));
            assertThat(rangeClosedBy((char) (Character.MIN_VALUE + 3), Character.MIN_VALUE, -3)).isEqualTo(of((char) (Character.MIN_VALUE + 3), Character.MIN_VALUE));

            // double
            assertThat(rangeClosedBy(1.0, 3.0, 1.0)).isEqualTo(of(1.0, 2.0, 3.0));
            assertThat(rangeClosedBy(1.0, 5.0, 2.0)).isEqualTo(of(1.0, 3.0, 5.0));
            assertThat(rangeClosedBy(1.0, 6.0, 2.0)).isEqualTo(of(1.0, 3.0, 5.0));
            assertThat(rangeClosedBy(Double.MAX_VALUE - 2.0E307, Double.MAX_VALUE, 3.0E307)).isEqualTo(of(Double.MAX_VALUE - 2.0E307));
            assertThat(rangeClosedBy(3.0, 1.0, -1.0)).isEqualTo(of(3.0, 2.0, 1.0));
            assertThat(rangeClosedBy(5.0, 1.0, -2.0)).isEqualTo(of(5.0, 3.0, 1.0));
            assertThat(rangeClosedBy(5.0, 0.0, -2.0)).isEqualTo(of(5.0, 3.0, 1.0));
            assertThat(rangeClosedBy(-Double.MAX_VALUE + 2.0E307, -Double.MAX_VALUE, -3.0E307)).isEqualTo(of(-Double.MAX_VALUE + 2.0E307));

            // int
            assertThat(rangeClosedBy(1, 3, 1)).isEqualTo(of(1, 2, 3));
            assertThat(rangeClosedBy(1, 5, 2)).isEqualTo(of(1, 3, 5));
            assertThat(rangeClosedBy(1, 6, 2)).isEqualTo(of(1, 3, 5));
            assertThat(rangeClosedBy(Integer.MAX_VALUE - 2, Integer.MAX_VALUE, 3)).isEqualTo(of(Integer.MAX_VALUE - 2));
            assertThat(rangeClosedBy(Integer.MAX_VALUE - 3, Integer.MAX_VALUE, 3)).isEqualTo(of(Integer.MAX_VALUE - 3, Integer.MAX_VALUE));
            assertThat(rangeClosedBy(3, 1, -1)).isEqualTo(of(3, 2, 1));
            assertThat(rangeClosedBy(5, 1, -2)).isEqualTo(of(5, 3, 1));
            assertThat(rangeClosedBy(5, 0, -2)).isEqualTo(of(5, 3, 1));
            assertThat(rangeClosedBy(Integer.MIN_VALUE + 2, Integer.MIN_VALUE, -3)).isEqualTo(of(Integer.MIN_VALUE + 2));
            assertThat(rangeClosedBy(Integer.MIN_VALUE + 3, Integer.MIN_VALUE, -3)).isEqualTo(of(Integer.MIN_VALUE + 3, Integer.MIN_VALUE));

            // long
            assertThat(rangeClosedBy(1L, 3L, 1)).isEqualTo(of(1L, 2L, 3L));
            assertThat(rangeClosedBy(1L, 5L, 2)).isEqualTo(of(1L, 3L, 5L));
            assertThat(rangeClosedBy(1L, 6L, 2)).isEqualTo(of(1L, 3L, 5L));
            assertThat(rangeClosedBy(Long.MAX_VALUE - 2, Long.MAX_VALUE, 3)).isEqualTo(of(Long.MAX_VALUE - 2));
            assertThat(rangeClosedBy(Long.MAX_VALUE - 3, Long.MAX_VALUE, 3)).isEqualTo(of(Long.MAX_VALUE - 3, Long.MAX_VALUE));
            assertThat(rangeClosedBy(3L, 1L, -1)).isEqualTo(of(3L, 2L, 1L));
            assertThat(rangeClosedBy(5L, 1L, -2)).isEqualTo(of(5L, 3L, 1L));
            assertThat(rangeClosedBy(5L, 0L, -2)).isEqualTo(of(5L, 3L, 1L));
            assertThat(rangeClosedBy(Long.MIN_VALUE + 2, Long.MIN_VALUE, -3)).isEqualTo(of(Long.MIN_VALUE + 2));
            assertThat(rangeClosedBy(Long.MIN_VALUE + 3, Long.MIN_VALUE, -3)).isEqualTo(of(Long.MIN_VALUE + 3, Long.MIN_VALUE));
        }

        @Test
        public void shouldCreateRangeClosedByWhereFromAndToEqualMIN_VALUE() {

            // char
            assertThat(rangeClosedBy(Character.MIN_VALUE, Character.MIN_VALUE, 1)).isEqualTo(of(Character.MIN_VALUE));
            assertThat(rangeClosedBy(Character.MIN_VALUE, Character.MIN_VALUE, 3)).isEqualTo(of(Character.MIN_VALUE));
            assertThat(rangeClosedBy(Character.MIN_VALUE, Character.MIN_VALUE, -1)).isEqualTo(of(Character.MIN_VALUE));
            assertThat(rangeClosedBy(Character.MIN_VALUE, Character.MIN_VALUE, -3)).isEqualTo(of(Character.MIN_VALUE));

            // double
            assertThat(rangeClosedBy(-Double.MAX_VALUE, -Double.MAX_VALUE, 1)).isEqualTo(of(-Double.MAX_VALUE));
            assertThat(rangeClosedBy(-Double.MAX_VALUE, -Double.MAX_VALUE, 3)).isEqualTo(of(-Double.MAX_VALUE));
            assertThat(rangeClosedBy(-Double.MAX_VALUE, -Double.MAX_VALUE, -1)).isEqualTo(of(-Double.MAX_VALUE));
            assertThat(rangeClosedBy(-Double.MAX_VALUE, -Double.MAX_VALUE, -3)).isEqualTo(of(-Double.MAX_VALUE));

            // int
            assertThat(rangeClosedBy(Integer.MIN_VALUE, Integer.MIN_VALUE, 1)).isEqualTo(of(Integer.MIN_VALUE));
            assertThat(rangeClosedBy(Integer.MIN_VALUE, Integer.MIN_VALUE, 3)).isEqualTo(of(Integer.MIN_VALUE));
            assertThat(rangeClosedBy(Integer.MIN_VALUE, Integer.MIN_VALUE, -1)).isEqualTo(of(Integer.MIN_VALUE));
            assertThat(rangeClosedBy(Integer.MIN_VALUE, Integer.MIN_VALUE, -3)).isEqualTo(of(Integer.MIN_VALUE));

            // long
            assertThat(rangeClosedBy(Long.MIN_VALUE, Long.MIN_VALUE, 1)).isEqualTo(of(Long.MIN_VALUE));
            assertThat(rangeClosedBy(Long.MIN_VALUE, Long.MIN_VALUE, 3)).isEqualTo(of(Long.MIN_VALUE));
            assertThat(rangeClosedBy(Long.MIN_VALUE, Long.MIN_VALUE, -1)).isEqualTo(of(Long.MIN_VALUE));
            assertThat(rangeClosedBy(Long.MIN_VALUE, Long.MIN_VALUE, -3)).isEqualTo(of(Long.MIN_VALUE));
        }

        @Test
        public void shouldCreateRangeClosedByWhereFromAndToEqualMAX_VALUE() {

            // char
            assertThat(rangeClosedBy(Character.MAX_VALUE, Character.MAX_VALUE, 1)).isEqualTo(of(Character.MAX_VALUE));
            assertThat(rangeClosedBy(Character.MAX_VALUE, Character.MAX_VALUE, 3)).isEqualTo(of(Character.MAX_VALUE));
            assertThat(rangeClosedBy(Character.MAX_VALUE, Character.MAX_VALUE, -1)).isEqualTo(of(Character.MAX_VALUE));
            assertThat(rangeClosedBy(Character.MAX_VALUE, Character.MAX_VALUE, -3)).isEqualTo(of(Character.MAX_VALUE));

            // double
            assertThat(rangeClosedBy(Double.MAX_VALUE, Double.MAX_VALUE, 1)).isEqualTo(of(Double.MAX_VALUE));
            assertThat(rangeClosedBy(Double.MAX_VALUE, Double.MAX_VALUE, 3)).isEqualTo(of(Double.MAX_VALUE));
            assertThat(rangeClosedBy(Double.MAX_VALUE, Double.MAX_VALUE, -1)).isEqualTo(of(Double.MAX_VALUE));
            assertThat(rangeClosedBy(Double.MAX_VALUE, Double.MAX_VALUE, -3)).isEqualTo(of(Double.MAX_VALUE));

            // int
            assertThat(rangeClosedBy(Integer.MAX_VALUE, Integer.MAX_VALUE, 1)).isEqualTo(of(Integer.MAX_VALUE));
            assertThat(rangeClosedBy(Integer.MAX_VALUE, Integer.MAX_VALUE, 3)).isEqualTo(of(Integer.MAX_VALUE));
            assertThat(rangeClosedBy(Integer.MAX_VALUE, Integer.MAX_VALUE, -1)).isEqualTo(of(Integer.MAX_VALUE));
            assertThat(rangeClosedBy(Integer.MAX_VALUE, Integer.MAX_VALUE, -3)).isEqualTo(of(Integer.MAX_VALUE));

            // long
            assertThat(rangeClosedBy(Long.MAX_VALUE, Long.MAX_VALUE, 1)).isEqualTo(of(Long.MAX_VALUE));
            assertThat(rangeClosedBy(Long.MAX_VALUE, Long.MAX_VALUE, 3)).isEqualTo(of(Long.MAX_VALUE));
            assertThat(rangeClosedBy(Long.MAX_VALUE, Long.MAX_VALUE, -1)).isEqualTo(of(Long.MAX_VALUE));
            assertThat(rangeClosedBy(Long.MAX_VALUE, Long.MAX_VALUE, -3)).isEqualTo(of(Long.MAX_VALUE));
        }

        @Test
        public void shouldCreateRangeClosedByStartingAtTypeBoundary() {

            // int
            assertThat(rangeClosedBy(Integer.MIN_VALUE, Integer.MIN_VALUE + 2, 1)).isEqualTo(of(Integer.MIN_VALUE, Integer.MIN_VALUE + 1, Integer.MIN_VALUE + 2));
            assertThat(rangeClosedBy(Integer.MAX_VALUE, Integer.MAX_VALUE - 2, -1)).isEqualTo(of(Integer.MAX_VALUE, Integer.MAX_VALUE - 1, Integer.MAX_VALUE - 2));
            assertThat(rangeClosedBy(Integer.MIN_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE)).isEqualTo(of(Integer.MIN_VALUE, -1, Integer.MAX_VALUE - 1));
            assertThat(rangeClosedBy(Integer.MAX_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE)).isEqualTo(of(Integer.MAX_VALUE, -1));
            assertThat(rangeClosedBy(Integer.MIN_VALUE, Integer.MIN_VALUE + 1, 5)).isEqualTo(of(Integer.MIN_VALUE));

            // long
            assertThat(rangeClosedBy(Long.MIN_VALUE, Long.MIN_VALUE + 2, 1L)).isEqualTo(of(Long.MIN_VALUE, Long.MIN_VALUE + 1, Long.MIN_VALUE + 2));
            assertThat(rangeClosedBy(Long.MAX_VALUE, Long.MAX_VALUE - 2, -1L)).isEqualTo(of(Long.MAX_VALUE, Long.MAX_VALUE - 1, Long.MAX_VALUE - 2));
            assertThat(rangeClosedBy(Long.MIN_VALUE, Long.MAX_VALUE, Long.MAX_VALUE)).isEqualTo(of(Long.MIN_VALUE, -1L, Long.MAX_VALUE - 1));
            assertThat(rangeClosedBy(Long.MAX_VALUE, Long.MIN_VALUE, Long.MIN_VALUE)).isEqualTo(of(Long.MAX_VALUE, -1L));
            assertThat(rangeClosedBy(Long.MIN_VALUE, Long.MIN_VALUE + 1, 5L)).isEqualTo(of(Long.MIN_VALUE));
        }
    }

    @Nested
    class StaticRangeTests {
        @Test
        public void shouldCreateRangeWhereFromIsGreaterThanTo() {
            assertThat(range('b', 'a').isEmpty());
            assertThat(range(1, 0)).isEmpty();
            assertThat(range(1L, 0L)).isEmpty();
        }

        @Test
        public void shouldCreateRangeWhereFromEqualsTo() {
            assertThat(range('a', 'a')).isEmpty();
            assertThat(range(0, 0)).isEmpty();
            assertThat(range(0L, 0L)).isEmpty();
        }

        @Test
        public void shouldCreateRangeWhereFromIsLessThanTo() {
            assertThat(range('a', 'c')).isEqualTo(of('a', 'b'));
            assertThat(range(1, 3)).isEqualTo(of(1, 2));
            assertThat(range(1L, 3L)).isEqualTo(of(1L, 2L));
        }

        @Test
        public void shouldCreateRangeWhereFromAndToEqualMIN_VALUE() {
            assertThat(range(Character.MIN_VALUE, Character.MIN_VALUE)).isEmpty();
            assertThat(range(Integer.MIN_VALUE, Integer.MIN_VALUE)).isEmpty();
            assertThat(range(Long.MIN_VALUE, Long.MIN_VALUE)).isEmpty();
        }

        @Test
        public void shouldCreateRangeWhereFromAndToEqualMAX_VALUE() {
            assertThat(range(Character.MAX_VALUE, Character.MAX_VALUE)).isEmpty();
            assertThat(range(Integer.MAX_VALUE, Integer.MAX_VALUE)).isEmpty();
            assertThat(range(Long.MAX_VALUE, Long.MAX_VALUE)).isEmpty();
        }
    }

    @Nested
    class StaticRangebyTests {
        @Test
        public void shouldCreateRangeByWhereFromIsGreaterThanToAndStepWrongDirection() {

            // char
            assertThat(rangeBy('b', 'a', 1)).isEmpty();
            assertThat(rangeBy('b', 'a', 3)).isEmpty();
            assertThat(rangeBy('a', 'b', -1)).isEmpty();
            assertThat(rangeBy('a', 'b', -3)).isEmpty();

            // double
            assertThat(rangeBy(1.0, 0.0, 1.0)).isEmpty();
            assertThat(rangeBy(1.0, 0.0, 3.0)).isEmpty();
            assertThat(rangeBy(0.0, 1.0, -1.0)).isEmpty();
            assertThat(rangeBy(0.0, 1.0, -3.0)).isEmpty();

            // int
            assertThat(rangeBy(1, 0, 1)).isEmpty();
            assertThat(rangeBy(1, 0, 3)).isEmpty();
            assertThat(rangeBy(0, 1, -1)).isEmpty();
            assertThat(rangeBy(0, 1, -3)).isEmpty();

            // long
            assertThat(rangeBy(1L, 0L, 1L)).isEmpty();
            assertThat(rangeBy(1L, 0L, 3L)).isEmpty();
            assertThat(rangeBy(0L, 1L, -1L)).isEmpty();
            assertThat(rangeBy(0L, 1L, -3L)).isEmpty();
        }

        @Test
        public void shouldCreateRangeByWithBigStep() {
            // step * (from - toExclusive) < 0 because of overflow
            // int
            assertThat(rangeBy(3_721, 2_000_000, 3_721)).isNotEmpty();
            // long
            assertThat(rangeBy(3_221_000L, 200_000_000_000L, 154_221_000L)).isNotEmpty();
        }

        @Test
        public void shouldCreateRangeByWhereFromEqualsTo() {

            // char
            assertThat(rangeBy('a', 'a', 1)).isEmpty();
            assertThat(rangeBy('a', 'a', 3)).isEmpty();
            assertThat(rangeBy('a', 'a', -1)).isEmpty();
            assertThat(rangeBy('a', 'a', -3)).isEmpty();

            // double
            assertThat(rangeBy(0.0, 0.0, 1.0)).isEmpty();
            assertThat(rangeBy(0.0, 0.0, 3.0)).isEmpty();
            assertThat(rangeBy(0.0, 0.0, -1.0)).isEmpty();
            assertThat(rangeBy(0.0, 0.0, -3.0)).isEmpty();

            // int
            assertThat(rangeBy(0, 0, 1)).isEmpty();
            assertThat(rangeBy(0, 0, 3)).isEmpty();
            assertThat(rangeBy(0, 0, -1)).isEmpty();
            assertThat(rangeBy(0, 0, -3)).isEmpty();

            // long
            assertThat(rangeBy(0L, 0L, 1L)).isEmpty();
            assertThat(rangeBy(0L, 0L, 3L)).isEmpty();
            assertThat(rangeBy(0L, 0L, -1L)).isEmpty();
            assertThat(rangeBy(0L, 0L, -3L)).isEmpty();
        }

        @Test
        public void shouldCreateRangeByWhereFromIsLessThanToAndStepCorrectDirection() {

            // char
            assertThat(rangeBy('a', 'c', 1)).isEqualTo(of('a', 'b'));
            assertThat(rangeBy('a', 'd', 2)).isEqualTo(of('a', 'c'));
            assertThat(rangeBy('c', 'a', -1)).isEqualTo(of('c', 'b'));
            assertThat(rangeBy('d', 'a', -2)).isEqualTo(of('d', 'b'));
            assertThat(rangeBy((char) (Character.MAX_VALUE - 3), Character.MAX_VALUE, 3)).isEqualTo(of((char) (Character.MAX_VALUE - 3)));
            assertThat(rangeBy((char) (Character.MAX_VALUE - 4), Character.MAX_VALUE, 3)).isEqualTo(of((char) (Character.MAX_VALUE - 4), (char) (Character.MAX_VALUE - 1)));
            assertThat(rangeBy((char) (Character.MIN_VALUE + 3), Character.MIN_VALUE, -3)).isEqualTo(of((char) (Character.MIN_VALUE + 3)));
            assertThat(rangeBy((char) (Character.MIN_VALUE + 4), Character.MIN_VALUE, -3)).isEqualTo(of((char) (Character.MIN_VALUE + 4), (char) (Character.MIN_VALUE + 1)));

            // double
            assertThat(rangeBy(1.0, 3.0, 1.0)).isEqualTo(of(1.0, 2.0));
            assertThat(rangeBy(1.0, 4.0, 2.0)).isEqualTo(of(1.0, 3.0));
            assertThat(rangeBy(3.0, 1.0, -1.0)).isEqualTo(of(3.0, 2.0));
            assertThat(rangeBy(4.0, 1.0, -2.0)).isEqualTo(of(4.0, 2.0));
            assertThat(rangeBy(Double.MAX_VALUE - 3.0E307, Double.MAX_VALUE, 3.0E307)).isEqualTo(of(Double.MAX_VALUE - 3.0E307));
            assertThat(rangeBy(-Double.MAX_VALUE + 3.0E307, -Double.MAX_VALUE, -3.0E307)).isEqualTo(of(-Double.MAX_VALUE + 3.0E307));

            // int
            assertThat(rangeBy(1, 3, 1)).isEqualTo(of(1, 2));
            assertThat(rangeBy(1, 4, 2)).isEqualTo(of(1, 3));
            assertThat(rangeBy(3, 1, -1)).isEqualTo(of(3, 2));
            assertThat(rangeBy(4, 1, -2)).isEqualTo(of(4, 2));
            assertThat(rangeBy(Integer.MAX_VALUE - 3, Integer.MAX_VALUE, 3)).isEqualTo(of(Integer.MAX_VALUE - 3));
            assertThat(rangeBy(Integer.MAX_VALUE - 4, Integer.MAX_VALUE, 3)).isEqualTo(of(Integer.MAX_VALUE - 4, Integer.MAX_VALUE - 1));
            assertThat(rangeBy(Integer.MIN_VALUE + 3, Integer.MIN_VALUE, -3)).isEqualTo(of(Integer.MIN_VALUE + 3));
            assertThat(rangeBy(Integer.MIN_VALUE + 4, Integer.MIN_VALUE, -3)).isEqualTo(of(Integer.MIN_VALUE + 4, Integer.MIN_VALUE + 1));

            // long
            assertThat(rangeBy(1L, 3L, 1L)).isEqualTo(of(1L, 2L));
            assertThat(rangeBy(1L, 4L, 2L)).isEqualTo(of(1L, 3L));
            assertThat(rangeBy(3L, 1L, -1L)).isEqualTo(of(3L, 2L));
            assertThat(rangeBy(4L, 1L, -2L)).isEqualTo(of(4L, 2L));
            assertThat(rangeBy(Long.MAX_VALUE - 3, Long.MAX_VALUE, 3)).isEqualTo(of(Long.MAX_VALUE - 3));
            assertThat(rangeBy(Long.MAX_VALUE - 4, Long.MAX_VALUE, 3)).isEqualTo(of(Long.MAX_VALUE - 4, Long.MAX_VALUE - 1));
            assertThat(rangeBy(Long.MIN_VALUE + 3, Long.MIN_VALUE, -3)).isEqualTo(of(Long.MIN_VALUE + 3));
            assertThat(rangeBy(Long.MIN_VALUE + 4, Long.MIN_VALUE, -3)).isEqualTo(of(Long.MIN_VALUE + 4, Long.MIN_VALUE + 1));
        }

        @Test
        public void shouldCreateRangeByWhereFromAndToEqualMIN_VALUE() {

            // char
            assertThat(rangeBy(Character.MIN_VALUE, Character.MIN_VALUE, 1)).isEmpty();
            assertThat(rangeBy(Character.MIN_VALUE, Character.MIN_VALUE, 3)).isEmpty();
            assertThat(rangeBy(Character.MIN_VALUE, Character.MIN_VALUE, -1)).isEmpty();
            assertThat(rangeBy(Character.MIN_VALUE, Character.MIN_VALUE, -3)).isEmpty();

            // double
            assertThat(rangeBy(-Double.MAX_VALUE, -Double.MAX_VALUE, 1.0)).isEmpty();
            assertThat(rangeBy(-Double.MAX_VALUE, -Double.MAX_VALUE, 3.0)).isEmpty();
            assertThat(rangeBy(-Double.MAX_VALUE, -Double.MAX_VALUE, -1.0)).isEmpty();
            assertThat(rangeBy(-Double.MAX_VALUE, -Double.MAX_VALUE, -3.0)).isEmpty();

            // int
            assertThat(rangeBy(Integer.MIN_VALUE, Integer.MIN_VALUE, 1)).isEmpty();
            assertThat(rangeBy(Integer.MIN_VALUE, Integer.MIN_VALUE, 3)).isEmpty();
            assertThat(rangeBy(Integer.MIN_VALUE, Integer.MIN_VALUE, -1)).isEmpty();
            assertThat(rangeBy(Integer.MIN_VALUE, Integer.MIN_VALUE, -3)).isEmpty();

            // long
            assertThat(rangeBy(Long.MIN_VALUE, Long.MIN_VALUE, 1L)).isEmpty();
            assertThat(rangeBy(Long.MIN_VALUE, Long.MIN_VALUE, 3L)).isEmpty();
            assertThat(rangeBy(Long.MIN_VALUE, Long.MIN_VALUE, -1L)).isEmpty();
            assertThat(rangeBy(Long.MIN_VALUE, Long.MIN_VALUE, -3L)).isEmpty();
        }

        @Test
        public void shouldCreateRangeByWhereFromAndToEqualMAX_VALUE() {

            // char
            assertThat(rangeBy(Character.MAX_VALUE, Character.MAX_VALUE, 1)).isEmpty();
            assertThat(rangeBy(Character.MAX_VALUE, Character.MAX_VALUE, 3)).isEmpty();
            assertThat(rangeBy(Character.MAX_VALUE, Character.MAX_VALUE, -1)).isEmpty();
            assertThat(rangeBy(Character.MAX_VALUE, Character.MAX_VALUE, -3)).isEmpty();

            // double
            assertThat(rangeBy(Double.MAX_VALUE, Double.MAX_VALUE, 1.0)).isEmpty();
            assertThat(rangeBy(Double.MAX_VALUE, Double.MAX_VALUE, 3.0)).isEmpty();
            assertThat(rangeBy(Double.MAX_VALUE, Double.MAX_VALUE, -1.0)).isEmpty();
            assertThat(rangeBy(Double.MAX_VALUE, Double.MAX_VALUE, -3.0)).isEmpty();

            // int
            assertThat(rangeBy(Integer.MAX_VALUE, Integer.MAX_VALUE, 1)).isEmpty();
            assertThat(rangeBy(Integer.MAX_VALUE, Integer.MAX_VALUE, 3)).isEmpty();
            assertThat(rangeBy(Integer.MAX_VALUE, Integer.MAX_VALUE, -1)).isEmpty();
            assertThat(rangeBy(Integer.MAX_VALUE, Integer.MAX_VALUE, -3)).isEmpty();

            // long
            assertThat(rangeBy(Long.MAX_VALUE, Long.MAX_VALUE, 1L)).isEmpty();
            assertThat(rangeBy(Long.MAX_VALUE, Long.MAX_VALUE, 3L)).isEmpty();
            assertThat(rangeBy(Long.MAX_VALUE, Long.MAX_VALUE, -1L)).isEmpty();
            assertThat(rangeBy(Long.MAX_VALUE, Long.MAX_VALUE, -3L)).isEmpty();
        }

        // step == 0

        @Test
        public void shouldProhibitCharRangeByStepZero() {
            assertThrows(IllegalArgumentException.class, () -> rangeBy('a', 'b', 0));
        }

        @Test
        public void shouldProhibitDoubleRangeByStepZero() {
            assertThrows(IllegalArgumentException.class, () -> rangeBy(0.0, 1.0, 0.0));
        }

        @Test
        public void shouldProhibitIntRangeByStepZero() {
            assertThrows(IllegalArgumentException.class, () -> rangeBy(0, 1, 0));
        }

        @Test
        public void shouldProhibitLongRangeByStepZero() {
            assertThrows(IllegalArgumentException.class, () -> rangeBy(0L, 1L, 0L));
        }

        @Test
        public void shouldProhibitCharRangeClosedByStepZero() {
            assertThrows(IllegalArgumentException.class, () -> rangeClosedBy('a', 'b', 0));
        }

        @Test
        public void shouldProhibitDoubleRangeClosedByStepZero() {
            assertThrows(IllegalArgumentException.class, () -> rangeClosedBy(0.0, 1.0, 0.0));
        }

        @Test
        public void shouldProhibitIntRangeClosedByStepZero() {
            assertThrows(IllegalArgumentException.class, () -> rangeClosedBy(0, 1, 0));
        }

        @Test
        public void shouldProhibitLongRangeClosedByStepZero() {
            assertThrows(IllegalArgumentException.class, () -> rangeClosedBy(0L, 1L, 0L));
        }

        @Test
        public void shouldProhibitRangeClosedByStepZeroWhenFromEqualsTo() {
            assertThrows(IllegalArgumentException.class, () -> rangeClosedBy('a', 'a', 0));
            assertThrows(IllegalArgumentException.class, () -> rangeClosedBy(1.0, 1.0, 0.0));
            assertThrows(IllegalArgumentException.class, () -> rangeClosedBy(1, 1, 0));
            assertThrows(IllegalArgumentException.class, () -> rangeClosedBy(1L, 1L, 0L));
        }

        // toExclusive at the type boundary

        @Test
        public void shouldCreateRangeByWhereToExclusiveIsTypeBoundary() {

            // int
            assertThat(rangeBy(5, Integer.MIN_VALUE, 1)).isEmpty();
            assertThat(rangeBy(5, Integer.MAX_VALUE, -1)).isEmpty();
            assertThat(rangeBy(Integer.MIN_VALUE, Integer.MIN_VALUE + 2, 1)).isEqualTo(of(Integer.MIN_VALUE, Integer.MIN_VALUE + 1));
            assertThat(rangeBy(Integer.MAX_VALUE, Integer.MAX_VALUE - 2, -1)).isEqualTo(of(Integer.MAX_VALUE, Integer.MAX_VALUE - 1));

            // long
            assertThat(rangeBy(5L, Long.MIN_VALUE, 1L)).isEmpty();
            assertThat(rangeBy(5L, Long.MAX_VALUE, -1L)).isEmpty();
            assertThat(rangeBy(Long.MIN_VALUE, Long.MIN_VALUE + 2, 1L)).isEqualTo(of(Long.MIN_VALUE, Long.MIN_VALUE + 1));
            assertThat(rangeBy(Long.MAX_VALUE, Long.MAX_VALUE - 2, -1L)).isEqualTo(of(Long.MAX_VALUE, Long.MAX_VALUE - 1));
        }

        // double special cases

        @Test
        public void shouldProhibitDoubleRangeClosedByToEqualsNaN() {
            assertThrows(IllegalArgumentException.class, () -> rangeClosedBy(0.0, Double.NaN, 1.0));
        }

        @Test
        public void shouldProhibitDoubleRangeClosedByFromEqualNaN() {
            assertThrows(IllegalArgumentException.class, () -> rangeClosedBy(Double.NaN, 0.0, 1.0));
        }

        @Test
        public void shouldProhibitDoubleRangeClosedByStepEqualNaN() {
            assertThrows(IllegalArgumentException.class, () -> rangeClosedBy(0.0, 10.0, Double.NaN));
        }

        @Test
        public void shouldProhibitDoubleRangeByToEqualsNaN() {
            assertThrows(IllegalArgumentException.class, () -> rangeBy(0.0, Double.NaN, 1.0));
        }

        @Test
        public void shouldProhibitDoubleRangeByFromEqualNaN() {
            assertThrows(IllegalArgumentException.class, () -> rangeBy(Double.NaN, 0.0, 1.0));
        }

        @Test
        public void shouldProhibitDoubleRangeByStepEqualNaN() {
            assertThrows(IllegalArgumentException.class, () -> rangeBy(0.0, 10.0, Double.NaN));
        }
    }

    // -- the set cases

    @Nested
    class FillIntSupplierTests {
        @Test
        public void shouldReturnSingleAfterFillWithConstant() {
            assertThat(fill(17, () -> 7))
                    .hasSize(1)
                    .isEqualTo(of(7));
        }
    }

    @Nested
    class AddTests {
        @Test
        public void shouldNotAddAnExistingElementTwice() {
            final Set<IntMod2> set = of(new IntMod2(2));
            assertThat(set.add(new IntMod2(4))).isSameAs(set);
        }
    }

    @Nested
    class AddallTests {
        @Test
        public void shouldAddAllOfIterable() {
            assertThat(of(1, 2, 3).addAll(of(2, 3, 4))).isEqualTo(of(1, 2, 3, 4));
        }

        @Test
        public void shouldReturnSameSetWhenAddAllEmptyToNonEmpty() {
            final Set<Integer> set = of(1, 2, 3);
            assertThat(set.addAll(empty())).isSameAs(set);
        }

        @Test
        public void shouldReturnSameSetWhenAddAllNonEmptyToEmpty() {
            final Set<Integer> set = of(1, 2, 3);
            if (set instanceof SortedSet) {
                assertThat(empty().addAll(set)).isEqualTo(set);
            } else {
                assertThat(empty().addAll(set)).isSameAs(set);
            }
        }

        @Test
        public void shouldReturnSameSetWhenAddAllContainedElements() {
            final Set<Integer> set = of(1, 2, 3);
            assertThat(set.addAll(of(1, 2, 3))).isSameAs(set);
        }
    }

    @Nested
    class DiffTests {
        @Test
        public void shouldCalculateDifference() {
            assertThat(of(1, 2, 3).diff(of(2))).isEqualTo(of(1, 3));
            assertThat(of(1, 2, 3).diff(of(5))).isEqualTo(of(1, 2, 3));
            assertThat(of(1, 2, 3).diff(of(1, 2, 3))).isEqualTo(empty());
        }

        @Test
        public void shouldReturnSameSetWhenEmptyDiffNonEmpty() {
            final Set<Integer> empty = empty();
            assertThat(empty.diff(of(1, 2))).isSameAs(empty);
        }

        @Test
        public void shouldReturnSameSetWhenNonEmptyDiffEmpty() {
            final Set<Integer> set = of(1, 2);
            assertThat(set.diff(empty())).isSameAs(set);
        }
    }

    @Nested
    class EqualityTests {
        @Test
        public void shouldObeyEqualityConstraints() {

            // sequential collections
            assertThat(empty().equals(HashSet.empty())).isTrue();
            assertThat(of(1).equals(HashSet.of(1))).isTrue();
            assertThat(of(1, 2, 3).equals(HashSet.of(1, 2, 3))).isTrue();
            assertThat(of(1, 2, 3).equals(HashSet.of(3, 2, 1))).isTrue();

            // other classes
            assertThat(empty().equals(List.empty())).isFalse();
            assertThat(empty().equals(HashMap.empty())).isFalse();

            assertThat(empty().equals(LinkedHashMap.empty())).isFalse();

            assertThat(empty().equals(TreeMap.empty())).isFalse();
        }
    }

    @Nested
    class IntersectTests {
        @Test
        public void shouldCalculateIntersect() {
            assertThat(of(1, 2, 3).intersect(of(2))).isEqualTo(of(2));
            assertThat(of(1, 2, 3).intersect(of(5))).isEqualTo(empty());
            assertThat(of(1, 2, 3).intersect(of(1, 2, 3))).isEqualTo(of(1, 2, 3));
        }

        @Test
        public void shouldReturnSameSetWhenEmptyIntersectNonEmpty() {
            final Set<Integer> empty = empty();
            assertThat(empty.intersect(of(1, 2))).isSameAs(empty);
        }

        @Test
        public void shouldReturnSameSetWhenNonEmptyIntersectEmpty() {
            final Set<Integer> set = of(1, 2);
            final Set<Integer> empty = empty();
            if (set instanceof SortedSet) {
                assertThat(set.intersect(empty)).isEqualTo(empty);
            } else {
                assertThat(set.intersect(empty)).isSameAs(empty);
            }
        }
    }

    @Nested
    class MapTests {
        @Test
        public void shouldMapDistinctElementsToOneElement() {
            assertThat(of(1, 2, 3).map(i -> 0)).isEqualTo(of(0));
        }
    }

    @Nested
    class RemoveTests {
        @Test
        public void shouldRemoveElement() {
            assertThat(of(1, 2, 3).remove(2)).isEqualTo(of(1, 3));
            assertThat(of(1, 2, 3).remove(5)).isEqualTo(of(1, 2, 3));
            assertThat(empty().remove(5)).isEqualTo(empty());
        }
    }

    @Nested
    class PartitionTests {
        @Test
        public void shouldPartitionInOneIteration() {
            final AtomicInteger count = new AtomicInteger(0);
            final Tuple2<? extends Set<Integer>, ? extends Set<Integer>> results = of(1, 2, 3).partition(i -> {
                count.incrementAndGet();
                return true;
            });
            assertThat(results._1()).isEqualTo(of(1, 2, 3));
            assertThat(results._2()).isEqualTo(of());
            assertThat(count.get()).isEqualTo(3);
        }
    }

    @Nested
    class RemoveallTests {
        @Test
        public void shouldRemoveAllElements() {
            assertThat(of(1, 2, 3).removeAll(of(2))).isEqualTo(of(1, 3));
            assertThat(of(1, 2, 3).removeAll(of(5))).isEqualTo(of(1, 2, 3));
        }

        @Test
        public void shouldReturnSameSetWhenNonEmptyRemoveAllEmpty() {
            final Set<Integer> set = of(1, 2, 3);
            assertThat(set.removeAll(empty())).isSameAs(set);
        }

        @Test
        public void shouldReturnSameSetWhenEmptyRemoveAllNonEmpty() {
            final Set<Integer> empty = empty();
            assertThat(empty.removeAll(of(1, 2, 3))).isSameAs(empty);
        }
    }

    // -- union

    @Test
    public void shouldCalculateUnion() {
        assertThat(of(1, 2, 3).union(of(2))).isEqualTo(of(1, 2, 3));
        assertThat(of(1, 2, 3).union(of(5))).isEqualTo(of(1, 2, 3, 5));
        assertThat(of(1, 2, 3).union(of(1, 2, 3))).isEqualTo(of(1, 2, 3));
    }

    @Test
    public void shouldReturnSameSetWhenEmptyUnionNonEmpty() {
        final Set<Integer> set = of(1, 2);
        if (set instanceof SortedSet) {
            assertThat(empty().union(set)).isEqualTo(set);
        } else {
            assertThat(empty().union(set)).isSameAs(set);
        }
    }

    @Test
    public void shouldReturnSameSetWhenNonEmptyUnionEmpty() {
        final Set<Integer> set = of(1, 2);
        assertThat(set.union(empty())).isSameAs(set);
    }

    // disabled tests

    @Test
    public void shouldBeAwareOfExistingNonUniqueElement() {
        // sets have only distinct elements
    }

    @Test
    public void shouldReplaceFirstOccurrenceOfNonNilUsingCurrNewWhenMultipleOccurrencesExist() {
        // sets have only distinct elements
    }

    @Nested
    class SpliteratorTests {
        @Test
        public void shouldHaveSizedSpliterator() {
            assertThat(of(1, 2, 3).spliterator().hasCharacteristics(Spliterator.SIZED | Spliterator.SUBSIZED)).isTrue();
        }

        @Test
        public void shouldHaveDistinctSpliterator() {
            assertThat(of(1, 2, 3).spliterator().hasCharacteristics(Spliterator.DISTINCT)).isTrue();
        }

        @Test
        public void shouldReturnSizeWhenSpliterator() {
            assertThat(of(1, 2, 3).spliterator().getExactSizeIfKnown()).isEqualTo(3);
        }
    }

    // -- null elements: every construction and insertion path rejects null (design 3.9)

    @Nested
    class NullElementTests {

        @Test
        public void shouldRejectNullOnOf() {
            assertThatNullPointerException().isThrownBy(() -> TreeSetTest.this.<Integer>of((Integer) null));
        }

        @Test
        public void shouldRejectNullOnOfVarargs() {
            assertThatNullPointerException().isThrownBy(() -> TreeSetTest.this.<Integer>of(1, null));
        }

        @Test
        public void shouldRejectNullOnAdd() {
            assertThatNullPointerException().isThrownBy(() -> TreeSetTest.this.<Integer>empty().add(null));
        }

        @Test
        public void shouldRejectNullOnAddAll() {
            assertThatNullPointerException().isThrownBy(() -> TreeSetTest.this.<Integer>empty().addAll(java.util.Arrays.asList(1, null)));
        }
    }

    // -- the sorted set cases

    // -- static narrow

    @Test
    public void shouldNarrowSortedSet() {
        final SortedSet<Double> doubles = of(toStringComparator(), 1.0d);
        final SortedSet<Number> numbers = SortedSet.narrow(doubles);
        final int actual = numbers.add(new BigDecimal("2.0")).sum().intValue();
        assertThat(actual).isEqualTo(3);
    }

    @Test
    public void shouldReturnComparator() {
        assertThat(of(1).comparator()).isNotNull();
    }

    @Test
    public void shouldNarrowSet() {
        // makes no sense because disjoint types share not the same ordering
    }

    @Override
    @Test
    public void shouldNarrowTraversable() {
        // makes no sense because disjoint types share not the same ordering
    }

    @Nested
    class EqualsTests {
        @Test
        public void shouldBeEqualWhenHavingSameElementsAndDifferentOrder() {
            final SortedSet<Integer> set1 = of(naturalOrder(), 1, 2, 3);
            final SortedSet<Integer> set2 = of(reverseOrder(), 3, 2, 1);
            assertThat(set1).isEqualTo(set2);
        }
    }

    // -- toSortedSet

    @Test
    @Disabled("a sorted set returns itself from toSortedSet(), so nothing can throw")
    public void shouldThrowOnConvertToSortedSetWithoutComparatorOnNonComparable() {
    }

    @Nested
    class SortedSetSpliteratorTests {
        @Test
        public void shouldHaveSortedSpliterator() {
            assertThat(of(1, 2, 3).spliterator().hasCharacteristics(Spliterator.SORTED)).isTrue();
        }

        @Test
        public void shouldReportTheNaturalOrderAsANullComparator() {
            assertThat(TreeSet.of(3, 1, 2).spliterator().getComparator()).isNull();
            assertThat(TreeSet.of(3, 1, 2).stream().sorted().toList()).isEqualTo(java.util.List.of(1, 2, 3));
        }

        @Test
        public void shouldReportTheComparatorOfAnotherOrder() {
            final Comparator<Integer> reversed = reverseOrder();
            final TreeSet<Integer> set = TreeSet.of(reversed, 3, 1, 2);
            assertThat(set.spliterator().getComparator()).isSameAs(reversed);
            assertThat(set.spliterator().hasCharacteristics(Spliterator.SORTED)).isTrue();
            assertThat(set.toJavaList()).isEqualTo(java.util.List.of(3, 2, 1));
            // java.util.stream sorts, as the reported comparator is not the natural order
            assertThat(set.stream().sorted().toList()).isEqualTo(java.util.List.of(1, 2, 3));
            assertThat(set.stream().sorted(reversed).toList()).isEqualTo(java.util.List.of(3, 2, 1));
            assertThat(TreeSet.of(reversed, 3, 1, 2).stream().parallel().sorted().toList()).isEqualTo(java.util.List.of(1, 2, 3));
        }

        @Test
        public void shouldReadAOneShotIterableOnceWhenBuilding() {
            final AtomicInteger walks = new AtomicInteger();
            final Iterable<Integer> that = () -> {
                walks.incrementAndGet();
                return java.util.List.of(3, 1, 2).iterator();
            };
            assertThat(TreeSet.ofAll(that)).isEqualTo(TreeSet.of(1, 2, 3));
            assertThat(walks.get()).isEqualTo(1);
            assertThat(TreeSet.ofAll(java.util.stream.Stream.of(2, 1)::iterator)).isEqualTo(TreeSet.of(1, 2));
            assertThat(TreeSet.ofAll(reverseOrder(), java.util.stream.Stream.of(1, 2)::iterator).toJavaList()).isEqualTo(java.util.List.of(2, 1));
            assertThat(TreeSet.ofAll(java.util.stream.Stream.<Integer>empty()::iterator)).isEqualTo(TreeSet.empty());
        }

        @Test
        public void shouldHaveOrderedSpliterator() {
            assertThat(of(1, 2, 3).spliterator().hasCharacteristics(Spliterator.ORDERED)).isTrue();
        }
    }

    // -- toArray(IntFunction)

    @Test
    public void shouldConvertNilToJavaArray() {
        final Integer[] actual = TreeSetTest.this.<Integer>empty().toArray(Integer[]::new);
        final Integer[] expected = new Integer[]{};
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void shouldConvertNonNilToJavaArray() {
        final Integer[] array = of(1, 2).toArray(Integer[]::new);
        assertThat(array).containsExactlyInAnyOrder(1, 2);
    }

    // -- size

    @Nested
    class SizeTests {
        @Test
        public void shouldCountTheDistinctElements() {
            assertThat(empty().size()).isEqualTo(0);
            assertThat(of(1, 2, 3).size()).isEqualTo(3);
            assertThat(of(1, 2, 3, 2, 1).size()).isEqualTo(3);
            assertThat(of(1, 2, 3).add(4).size()).isEqualTo(4);
            assertThat(of(1, 2, 3).add(3).size()).isEqualTo(3);
            assertThat(of(1, 2, 3).remove(2).size()).isEqualTo(2);
            assertThat(of(1, 2, 3).remove(9).size()).isEqualTo(3);
        }
    }

    // -- fold and reduce on a set: the iteration order is the set's own, so a commutative operation is used

    @Nested
    class FoldAndReduceTests {
        @Test
        public void shouldFoldWithACommutativeOperation() {
            assertThat(of(1, 2, 3, 4).fold(0, Integer::sum)).isEqualTo(10);
            assertThat(of(1, 2, 3, 4).fold(1, (a, b) -> a * b)).isEqualTo(24);
            assertThat(TreeSetTest.this.<Integer>empty().fold(7, Integer::sum)).isEqualTo(7);
        }

        @Test
        public void shouldReduceWithACommutativeOperation() {
            assertThat(of(1, 2, 3, 4).reduce(Integer::sum)).isEqualTo(10);
            assertThat(of(1, 2, 3, 4).reduce(Math::max)).isEqualTo(4);
            assertThat(of(5).reduce(Integer::sum)).isEqualTo(5);
            assertThat(of(1, 2, 3, 4).reduceOption(Integer::sum)).isEqualTo(Option.some(10));
            assertThat(TreeSetTest.this.<Integer>empty().reduceOption(Integer::sum)).isEqualTo(Option.none());
            assertThrows(NoSuchElementException.class, () -> TreeSetTest.this.<Integer>empty().reduce(Integer::sum));
        }

        @Test
        public void shouldCombineEveryElementExactlyOnceWhenReducing() {
            final Set<Integer> set = of(1, 2, 3, 4, 5);
            final java.util.List<Integer> seen = new ArrayList<>();
            final int sum = set.reduce((a, b) -> {
                if (seen.isEmpty()) {
                    seen.add(a);
                }
                seen.add(b);
                return a + b;
            });
            assertThat(sum).isEqualTo(15);
            assertThat(new java.util.HashSet<>(seen)).isEqualTo(java.util.Set.of(1, 2, 3, 4, 5));
            assertThat(seen.size()).isEqualTo(5);
        }
    }

    // -- asJava: the read-only java.util.Collection view of the set

    @Nested
    class AsJavaTests {
        @Test
        public void shouldViewTheDistinctElementsAsAJavaCollection() {
            final Set<Integer> set = of(1, 2, 3, 2);
            final java.util.Collection<Integer> view = set.asJava();
            assertThat(view.size()).isEqualTo(3);
            assertThat(new java.util.HashSet<>(view)).isEqualTo(java.util.Set.of(1, 2, 3));
            assertThat(HashSet.ofAll(view)).isEqualTo(HashSet.of(1, 2, 3));
            assertThat(view.contains(3)).isTrue();
            assertThat(view.contains(4)).isFalse();
            assertThrows(UnsupportedOperationException.class, () -> view.add(4));
            assertThrows(UnsupportedOperationException.class, () -> view.remove(1));
        }

        @Test
        public void shouldIterateTheJavaViewInTheSetsOrder() {
            final Set<Integer> set = of(3, 1, 2);
            assertThat(List.ofAll(set.asJava())).isEqualTo(set.toList());
        }
    }

    // -- the folded cases in comparator order

    @Nested
    class ComparatorOrderTests {
        @Test
        public void shouldFoldInComparatorOrder() {
            assertThat(of(3, 1, 2).foldLeft("", (acc, x) -> acc + x)).isEqualTo("123");
            assertThat(of(reverseOrder(), 3, 1, 2).foldLeft("", (acc, x) -> acc + x)).isEqualTo("321");
            assertThat(of(3, 1, 2).reduce((a, b) -> a * 10 + b)).isEqualTo(123);
        }

        @Test
        public void shouldKeepTheComparatorOrderInTheJavaView() {
            assertThat(new ArrayList<>(of(3, 1, 2).asJava())).isEqualTo(asList(1, 2, 3));
            assertThat(new ArrayList<>(of(reverseOrder(), 3, 1, 2).asJava())).isEqualTo(asList(3, 2, 1));
        }
    }

    // -- positional operations, in the comparator's order

    @Nested
    class PositionalTests {

        private static final int[] WINDOW_SIZES = { 1, 2, 3, 5, 69, 70, 71, Integer.MAX_VALUE };
        private static final int[] WINDOW_STEPS = { 1, 2, 3, 70, 71, Integer.MAX_VALUE };

        private java.util.List<TreeSet<Integer>> receivers() {
            return java.util.List.of(
                    TreeSet.<Integer> empty(),
                    TreeSet.of(7),
                    mk(5, 3, 9, 1, 7),
                    mkReversed(5, 3, 9, 1, 7),
                    mk(Vector.range(0, 70)),
                    mkReversed(Vector.range(0, 70)),
                    mk(Vector.range(0, 100)).removeAll(Vector.range(0, 100).filter(i -> i % 3 == 0)));
        }

        private int[] counts(int size) {
            return new int[] { Integer.MIN_VALUE, -1, 0, 1, 2, size / 2, size - 1, size, size + 1, Integer.MAX_VALUE };
        }

        // Vector's own takeRight/dropRight compute length - n, so the reference is given an n that cannot overflow
        private int clamp(int n, int size) {
            return Math.max(-1, Math.min(n, size + 1));
        }

        // `actual` holds exactly `expected`, in order, and behaves as a TreeSet built from it
        private void assertValid(TreeSet<Integer> receiver, TreeSet<Integer> actual, Vector<Integer> expected) {
            assertEquals(expected, actual.toVector());
            assertEquals(expected.size(), actual.size());
            assertSame(receiver.comparator(), actual.comparator());
            for (Integer element : expected) {
                assertTrue(actual.contains(element));
            }
            // the result is a tree like any other: an insertion and a removal land in order
            assertEquals(TreeSet.ofAll(receiver.comparator(), expected.append(1000)).toVector(), actual.add(1000).toVector());
            if (!expected.isEmpty()) {
                assertEquals(expected.tail(), actual.remove(expected.head()).toVector());
                assertEquals(expected.init(), actual.remove(expected.last()).toVector());
            }
        }

        @Test
        public void shouldTakeAndDropLikeTheSequenceOfTheElements() {
            for (TreeSet<Integer> receiver : receivers()) {
                final Vector<Integer> elements = receiver.toVector();
                final int size = receiver.size();
                for (int n : counts(size)) {
                    final int m = clamp(n, size);
                    final TreeSet<Integer> take = receiver.take(n);
                    final TreeSet<Integer> takeRight = receiver.takeRight(n);
                    final TreeSet<Integer> drop = receiver.drop(n);
                    final TreeSet<Integer> dropRight = receiver.dropRight(n);
                    assertValid(receiver, take, elements.take(m));
                    assertValid(receiver, takeRight, elements.takeRight(m));
                    assertValid(receiver, drop, elements.drop(m));
                    assertValid(receiver, dropRight, elements.dropRight(m));
                    if (n >= size) {
                        assertSame(receiver, take);
                        assertSame(receiver, takeRight);
                    }
                    if (n <= 0) {
                        assertSame(receiver, drop);
                        assertSame(receiver, dropRight);
                    }
                }
                assertEquals(elements, receiver.toVector());
            }
        }

        @Test
        public void shouldReturnTheFirstAndTheLastElement() {
            for (TreeSet<Integer> receiver : receivers()) {
                final Vector<Integer> elements = receiver.toVector();
                if (elements.isEmpty()) {
                    assertEquals("head of empty TreeSet", assertThrows(NoSuchElementException.class, receiver::head).getMessage());
                    assertEquals("last of empty TreeSet", assertThrows(NoSuchElementException.class, receiver::last).getMessage());
                    assertEquals(Option.none(), receiver.headOption());
                    assertEquals(Option.none(), receiver.lastOption());
                } else {
                    assertEquals(elements.head(), receiver.head());
                    assertEquals(elements.last(), receiver.last());
                    assertEquals(Option.some(elements.head()), receiver.headOption());
                    assertEquals(Option.some(elements.last()), receiver.lastOption());
                }
            }
            assertEquals(1, mk(5, 3, 9, 1, 7).head());
            assertEquals(9, mk(5, 3, 9, 1, 7).last());
            assertEquals(9, mkReversed(5, 3, 9, 1, 7).head());
            assertEquals(1, mkReversed(5, 3, 9, 1, 7).last());
        }

        @Test
        public void shouldDropTheFirstOrTheLastElementWithTailAndInit() {
            for (TreeSet<Integer> receiver : receivers()) {
                final Vector<Integer> elements = receiver.toVector();
                if (elements.isEmpty()) {
                    assertEquals("tail of empty TreeSet", assertThrows(UnsupportedOperationException.class, receiver::tail).getMessage());
                    assertEquals("init of empty TreeSet", assertThrows(UnsupportedOperationException.class, receiver::init).getMessage());
                    assertEquals(Option.none(), receiver.tailOption());
                    assertEquals(Option.none(), receiver.initOption());
                } else {
                    final TreeSet<Integer> tail = receiver.tail();
                    final TreeSet<Integer> init = receiver.init();
                    assertValid(receiver, tail, elements.tail());
                    assertValid(receiver, init, elements.init());
                    final Option<TreeSet<Integer>> tailOption = receiver.tailOption();
                    final Option<TreeSet<Integer>> initOption = receiver.initOption();
                    assertValid(receiver, tailOption.get(), elements.tail());
                    assertValid(receiver, initOption.get(), elements.init());
                }
            }
        }

        @Test
        public void shouldTakeAndDropWhileOrUntilAPredicateHolds() {
            final java.util.List<java.util.function.Predicate<Integer>> predicates = java.util.List.of(
                    e -> true, e -> false, e -> e < 5, e -> e >= 5, e -> e % 2 == 1, e -> e != 40);
            for (TreeSet<Integer> receiver : receivers()) {
                final Vector<Integer> elements = receiver.toVector();
                for (java.util.function.Predicate<Integer> predicate : predicates) {
                    final TreeSet<Integer> takeWhile = receiver.takeWhile(predicate);
                    final TreeSet<Integer> takeUntil = receiver.takeUntil(predicate);
                    final TreeSet<Integer> dropWhile = receiver.dropWhile(predicate);
                    final TreeSet<Integer> dropUntil = receiver.dropUntil(predicate);
                    assertValid(receiver, takeWhile, elements.takeWhile(predicate));
                    assertValid(receiver, takeUntil, elements.takeUntil(predicate));
                    assertValid(receiver, dropWhile, elements.dropWhile(predicate));
                    assertValid(receiver, dropUntil, elements.dropUntil(predicate));
                }
                // the walk stops at the first element that ends the prefix
                final int[] calls = { 0 };
                receiver.takeWhile(e -> {
                    calls[0]++;
                    return false;
                });
                assertEquals(receiver.isEmpty() ? 0 : 1, calls[0]);
                assertThrows(NullPointerException.class, () -> receiver.takeWhile(null));
                assertThrows(NullPointerException.class, () -> receiver.takeUntil(null));
                assertThrows(NullPointerException.class, () -> receiver.dropWhile(null));
                assertThrows(NullPointerException.class, () -> receiver.dropUntil(null));
            }
        }

        @Test
        public void shouldZipWithThePosition() {
            for (TreeSet<Integer> receiver : receivers()) {
                final Vector<Tuple2<Integer, Integer>> zipped = receiver.zipWithIndex();
                assertEquals(receiver.toVector().zipWithIndex(), zipped);
                for (int i = 0; i < zipped.size(); i++) {
                    assertEquals(i, zipped.get(i)._2());
                }
            }
        }

        @Test
        public void shouldGroupAndSlideLikeTheSequenceOfTheElements() {
            for (TreeSet<Integer> receiver : receivers()) {
                final Vector<Integer> elements = receiver.toVector();
                for (int size : WINDOW_SIZES) {
                    for (int step : WINDOW_STEPS) {
                        final Vector<TreeSet<Integer>> windows = receiver.sliding(size, step);
                        final Vector<Vector<Integer>> expected = elements.sliding(size, step);
                        assertEquals(expected.size(), windows.size());
                        for (int i = 0; i < windows.size(); i++) {
                            assertValid(receiver, windows.get(i), expected.get(i));
                        }
                    }
                    final Vector<TreeSet<Integer>> groups = receiver.grouped(size);
                    assertEquals(elements.grouped(size), groups.map(TreeSet::toVector));
                    groups.forEach(group -> assertValid(receiver, group, group.toVector()));
                    final Vector<TreeSet<Integer>> windows = receiver.sliding(size);
                    assertEquals(elements.sliding(size), windows.map(TreeSet::toVector));
                    windows.forEach(window -> assertValid(receiver, window, window.toVector()));
                }
            }
        }

        @Test
        public void shouldSlideFollowingTheWindowRules() {
            assertEquals(Vector.of(Vector.of(1, 2, 3), Vector.of(2, 3, 4)), mk(1, 2, 3, 4).sliding(3).map(this::keys));
            assertEquals(Vector.of(Vector.of(1, 2), Vector.of(4, 5)), mk(1, 2, 3, 4, 5).sliding(2, 3).map(this::keys));
            assertEquals(Vector.of(Vector.of(1, 2), Vector.of(5)), mk(1, 2, 3, 4, 5).sliding(2, 4).map(this::keys));
            assertEquals(Vector.of(Vector.of(1, 2, 3), Vector.of(3, 4, 5)), mk(1, 2, 3, 4, 5).sliding(3, 2).map(this::keys));
            assertEquals(Vector.of(Vector.of(1, 2, 3), Vector.of(3, 4, 5), Vector.of(5, 6)),
                    mk(1, 2, 3, 4, 5, 6).sliding(3, 2).map(this::keys));
            assertEquals(Vector.of(Vector.of(1)), mk(1, 2, 3).sliding(1, 3).map(this::keys));
            assertEquals(Vector.of(Vector.of(1, 2)), mk(1, 2).sliding(5).map(this::keys));
            assertEquals(Vector.of(Vector.of(1)), mk(1).sliding(1).map(this::keys));
            assertEquals(Vector.of(Vector.of(1, 2), Vector.of(3, 4), Vector.of(5)), mk(1, 2, 3, 4, 5).grouped(2).map(this::keys));
            assertEquals(Vector.of(Vector.of(1, 2, 3), Vector.of(10, 12), Vector.of(20, 29)),
                    mk(1, 2, 3, 10, 12, 20, 29).slideBy(e -> e / 10).map(this::keys));
            // a huge step or size does not overflow the window start
            assertEquals(Vector.of(Vector.range(0, 3)), mk(Vector.range(0, 40)).sliding(3, Integer.MAX_VALUE).map(this::keys));
            assertEquals(Vector.of(Vector.range(0, 40)),
                    mk(Vector.range(0, 40)).sliding(Integer.MAX_VALUE, Integer.MAX_VALUE).map(this::keys));
            assertTrue(TreeSet.<Integer> empty().sliding(1).isEmpty());
            assertTrue(TreeSet.<Integer> empty().sliding(2, 3).isEmpty());
            assertTrue(TreeSet.<Integer> empty().grouped(2).isEmpty());
        }

        @Test
        public void shouldRejectANonPositiveWindowSizeOrStep() {
            for (TreeSet<Integer> receiver : receivers()) {
                assertThrows(IllegalArgumentException.class, () -> receiver.grouped(0));
                assertThrows(IllegalArgumentException.class, () -> receiver.grouped(-1));
                assertThrows(IllegalArgumentException.class, () -> receiver.sliding(0));
                assertThrows(IllegalArgumentException.class, () -> receiver.sliding(2, 0));
                assertThrows(IllegalArgumentException.class, () -> receiver.sliding(0, 2));
                assertThrows(IllegalArgumentException.class, () -> receiver.sliding(-1, -1));
            }
        }

        @Test
        public void shouldSlideByCallingTheClassifierOncePerElement() {
            for (TreeSet<Integer> receiver : receivers()) {
                final Vector<Integer> elements = receiver.toVector();
                final java.util.List<Integer> seen = new java.util.ArrayList<>();
                final Vector<TreeSet<Integer>> runs = receiver.slideBy(e -> {
                    seen.add(e);
                    return e / 3;
                });
                assertEquals(new java.util.ArrayList<>(elements.asJava()), seen);
                final Vector<Vector<Integer>> expected = elements.slideBy(e -> e / 3);
                assertEquals(expected.size(), runs.size());
                for (int i = 0; i < runs.size(); i++) {
                    assertValid(receiver, runs.get(i), expected.get(i));
                }
                assertEquals(receiver.isEmpty() ? 0 : 1, receiver.slideBy(e -> "same").size());
                assertEquals(receiver.size(), receiver.slideBy(e -> e).size());
                assertThrows(NullPointerException.class, () -> receiver.slideBy(null));
            }
        }

        @Test
        public void shouldDeclareThePositionalMembersWithTheOwnType() throws Exception {
            for (String name : new String[] { "init", "tail" }) {
                assertEquals(TreeSet.class, TreeSet.class.getDeclaredMethod(name).getReturnType());
                assertEquals(SortedSet.class, SortedSet.class.getDeclaredMethod(name).getReturnType());
            }
            for (String name : new String[] { "take", "takeRight", "drop", "dropRight" }) {
                assertEquals(TreeSet.class, TreeSet.class.getDeclaredMethod(name, int.class).getReturnType());
                assertEquals(SortedSet.class, SortedSet.class.getDeclaredMethod(name, int.class).getReturnType());
            }
            for (String name : new String[] { "takeWhile", "takeUntil", "dropWhile", "dropUntil" }) {
                assertEquals(TreeSet.class, TreeSet.class.getDeclaredMethod(name, java.util.function.Predicate.class).getReturnType());
                assertEquals(SortedSet.class, SortedSet.class.getDeclaredMethod(name, java.util.function.Predicate.class).getReturnType());
            }
            assertEquals("com.guizmaii.zazr.control.Option<com.guizmaii.zazr.collection.TreeSet<T>>",
                    TreeSet.class.getDeclaredMethod("tailOption").getGenericReturnType().getTypeName());
            assertEquals("com.guizmaii.zazr.control.Option<com.guizmaii.zazr.collection.TreeSet<T>>",
                    TreeSet.class.getDeclaredMethod("initOption").getGenericReturnType().getTypeName());
            assertEquals("com.guizmaii.zazr.collection.Vector<com.guizmaii.zazr.collection.TreeSet<T>>",
                    TreeSet.class.getDeclaredMethod("grouped", int.class).getGenericReturnType().getTypeName());
            assertEquals("com.guizmaii.zazr.collection.Vector<com.guizmaii.zazr.collection.TreeSet<T>>",
                    TreeSet.class.getDeclaredMethod("sliding", int.class).getGenericReturnType().getTypeName());
            assertEquals("com.guizmaii.zazr.collection.Vector<com.guizmaii.zazr.collection.TreeSet<T>>",
                    TreeSet.class.getDeclaredMethod("sliding", int.class, int.class).getGenericReturnType().getTypeName());
            assertEquals("com.guizmaii.zazr.collection.Vector<com.guizmaii.zazr.collection.TreeSet<T>>",
                    TreeSet.class.getDeclaredMethod("slideBy", Function.class).getGenericReturnType().getTypeName());
            assertEquals("com.guizmaii.zazr.collection.Vector<com.guizmaii.zazr.Tuple2<T, java.lang.Integer>>",
                    TreeSet.class.getDeclaredMethod("zipWithIndex").getGenericReturnType().getTypeName());
            final java.util.Set<String> declared = new java.util.HashSet<>();
            for (java.lang.reflect.Method method : SortedSet.class.getDeclaredMethods()) {
                declared.add(method.getName());
            }
            assertTrue(declared.containsAll(ORDERED_POSITIONAL_MEMBERS));
        }

        @Test
        public void shouldKeepTheReversedComparatorInTheResults() {
            final TreeSet<Integer> reversed = mkReversed(1, 2, 3, 4, 5);
            assertEquals(Vector.of(5, 4), reversed.take(2).toVector());
            assertEquals(Vector.of(2, 1), reversed.takeRight(2).toVector());
            assertEquals(Vector.of(3, 2, 1), reversed.drop(2).toVector());
            assertEquals(Vector.of(5, 4, 3), reversed.takeWhile(x -> x > 2).toVector());
            assertEquals(Vector.of(Vector.of(5, 4), Vector.of(3, 2), Vector.of(1)), reversed.grouped(2).map(this::keys));
            assertSame(reversed.comparator(), reversed.take(0).comparator());
            assertSame(reversed.comparator(), reversed.drop(9).comparator());
            // the comparator keeps ordering what is added to a result
            assertEquals(Vector.of(9, 5, 4), reversed.take(2).add(9).toVector());
        }

        private Vector<Integer> keys(TreeSet<Integer> set) {
            return set.toVector();
        }

        private TreeSet<Integer> mk(Integer... elements) {
            return TreeSet.of(elements);
        }

        private TreeSet<Integer> mk(Iterable<Integer> elements) {
            return TreeSet.ofAll(elements);
        }

        private TreeSet<Integer> mkReversed(Integer... elements) {
            return TreeSet.of(reverseOrder(), elements);
        }

        private TreeSet<Integer> mkReversed(Iterable<Integer> elements) {
            return TreeSet.ofAll(reverseOrder(), elements);
        }
    }
}
