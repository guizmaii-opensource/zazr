package com.guizmaii.zazr.collection;

import com.guizmaii.zazr.control.Option;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static com.guizmaii.zazr.TestComparators.toStringComparator;
import static java.util.Comparator.nullsFirst;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

public class TreeSetTest extends AbstractSortedSetTest {

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

    @Override
    protected <T> TreeSet<T> of(Comparator<? super T> comparator, T element) {
        return TreeSet.of(comparator, element);
    }

    @Override
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

    @Override
    protected boolean useIsEqualToInsteadOfIsSameAs() {
        return true;
    }

    @Override
    protected TreeSet<Character> range(char from, char toExclusive) {
        return TreeSet.range(from, toExclusive);
    }

    @Override
    protected TreeSet<Character> rangeBy(char from, char toExclusive, int step) {
        return TreeSet.rangeBy(from, toExclusive, step);
    }

    @Override
    protected TreeSet<Double> rangeBy(double from, double toExclusive, double step) {
        return TreeSet.rangeBy(from, toExclusive, step);
    }

    @Override
    protected TreeSet<Integer> range(int from, int toExclusive) {
        return TreeSet.range(from, toExclusive);
    }

    @Override
    protected TreeSet<Integer> rangeBy(int from, int toExclusive, int step) {
        return TreeSet.rangeBy(from, toExclusive, step);
    }

    @Override
    protected TreeSet<Long> range(long from, long toExclusive) {
        return TreeSet.range(from, toExclusive);
    }

    @Override
    protected TreeSet<Long> rangeBy(long from, long toExclusive, long step) {
        return TreeSet.rangeBy(from, toExclusive, step);
    }

    @Override
    protected TreeSet<Character> rangeClosed(char from, char toInclusive) {
        return TreeSet.rangeClosed(from, toInclusive);
    }

    @Override
    protected TreeSet<Character> rangeClosedBy(char from, char toInclusive, int step) {
        return TreeSet.rangeClosedBy(from, toInclusive, step);
    }

    @Override
    protected TreeSet<Double> rangeClosedBy(double from, double toInclusive, double step) {
        return TreeSet.rangeClosedBy(from, toInclusive, step);
    }

    @Override
    protected TreeSet<Integer> rangeClosed(int from, int toInclusive) {
        return TreeSet.rangeClosed(from, toInclusive);
    }

    @Override
    protected TreeSet<Integer> rangeClosedBy(int from, int toInclusive, int step) {
        return TreeSet.rangeClosedBy(from, toInclusive, step);
    }

    @Override
    protected TreeSet<Long> rangeClosed(long from, long toInclusive) {
        return TreeSet.rangeClosed(from, toInclusive);
    }

    @Override
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
            final Traversable<Integer> value = of(1, 2, 3);
            assertThat(value.toSortedSet()).isSameAs(value);
        }

        @Test
        public void shouldReturnSelfOnConvertToSortedSetWithSameComparator() {
            final TreeSet<Integer> value = of(1, 2, 3);
            assertThat(value.toSortedSet(value.comparator())).isSameAs(value);
        }

        @Test
        public void shouldNotReturnSelfOnConvertToSortedSetWithDifferentComparator() {
            final Traversable<Integer> value = of(1, 2, 3);
            assertThat(value.toSortedSet(Integer::compareTo)).isNotSameAs(value);
        }

        @Test
        public void shouldPreserveComparatorOnConvertToSortedSetWithoutDistinctComparator() {
            final Traversable<Integer> value = TreeSet.of(Comparators.naturalComparator().reversed(), 1, 2, 3);
            assertThat(value.toSortedSet().mkString(",")).isEqualTo("3,2,1");
        }
    }

    // -- helpers

    private static Comparator<Integer> inverseIntComparator() {
        return (i1, i2) -> Integer.compare(i2, i1);
    }

    // -- ignored tests

    @Override
    @Test
    @Disabled
    public void shouldCalculateAverageOfDoubleAndFloat() {
        // it is not possible to create a TreeSet containing unrelated types
    }

    @Override
    @Test
    @Disabled
    public void shouldZipAllEmptyAndNonNil() {
    }

    @Override
    @Test
    @Disabled
    public void shouldZipAllNonEmptyAndNil() {
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
}
