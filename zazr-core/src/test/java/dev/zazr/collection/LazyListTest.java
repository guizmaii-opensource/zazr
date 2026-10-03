package dev.zazr.collection;

import dev.zazr.*;
import dev.zazr.Tuple;
import dev.zazr.Tuple2;
import dev.zazr.collection.internal.Comparators;
import dev.zazr.control.Either;
import dev.zazr.control.Option;
import dev.zazr.control.Try;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.NoSuchElementException;
import java.util.Spliterator;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collector;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestTemplate;

import static dev.zazr.collection.LazyList.concat;
import static java.util.Comparator.comparingInt;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeout;

public class LazyListTest extends AbstractTraversableTest {

    // -- construction

    @Override
    protected <T> Collector<T, ArrayList<T>, LazyList<T>> collector() {
        return LazyList.collector();
    }

    @Override
    protected <T> LazyList<T> empty() {
        return LazyList.empty();
    }

    @Override
    protected <T> LazyList<T> of(T element) {
        return LazyList.of(element);
    }

    @SuppressWarnings("varargs")
    @SafeVarargs
    @Override
    protected final <T> LazyList<T> of(T... elements) {
        return LazyList.of(elements);
    }

    @Override
    protected <T> LazyList<T> ofAll(Iterable<? extends T> elements) {
        return LazyList.ofAll(elements);
    }

    @Override
    protected <T extends Comparable<? super T>> LazyList<T> ofJavaStream(
            java.util.stream.Stream<? extends T> javaStream) {
        return LazyList.ofAll(javaStream);
    }

    @Override
    protected LazyList<Boolean> ofAll(boolean... elements) {
        return LazyList.ofAll(elements);
    }

    @Override
    protected LazyList<Byte> ofAll(byte... elements) {
        return LazyList.ofAll(elements);
    }

    @Override
    protected LazyList<Character> ofAll(char... elements) {
        return LazyList.ofAll(elements);
    }

    @Override
    protected LazyList<Double> ofAll(double... elements) {
        return LazyList.ofAll(elements);
    }

    @Override
    protected LazyList<Float> ofAll(float... elements) {
        return LazyList.ofAll(elements);
    }

    @Override
    protected LazyList<Integer> ofAll(int... elements) {
        return LazyList.ofAll(elements);
    }

    @Override
    protected LazyList<Long> ofAll(long... elements) {
        return LazyList.ofAll(elements);
    }

    @Override
    protected LazyList<Short> ofAll(short... elements) {
        return LazyList.ofAll(elements);
    }

    @Override
    protected <T> LazyList<T> tabulate(int n, Function<? super Integer, ? extends T> f) {
        return LazyList.tabulate(n, f);
    }

    @Override
    protected <T> LazyList<T> fill(int n, Supplier<? extends T> s) {
        return LazyList.fill(n, s);
    }

    protected <T> Traversable<T> fill(int n, T element) {
        return LazyList.fill(n, element);
    }

    protected LazyList<Character> range(char from, char toExclusive) {
        return LazyList.range(from, toExclusive);
    }

    protected LazyList<Character> rangeBy(char from, char toExclusive, int step) {
        return LazyList.rangeBy(from, toExclusive, step);
    }

    protected LazyList<Double> rangeBy(double from, double toExclusive, double step) {
        return LazyList.rangeBy(from, toExclusive, step);
    }

    protected LazyList<Integer> range(int from, int toExclusive) {
        return LazyList.range(from, toExclusive);
    }

    protected LazyList<Integer> rangeBy(int from, int toExclusive, int step) {
        return LazyList.rangeBy(from, toExclusive, step);
    }

    protected LazyList<Long> range(long from, long toExclusive) {
        return LazyList.range(from, toExclusive);
    }

    protected LazyList<Long> rangeBy(long from, long toExclusive, long step) {
        return LazyList.rangeBy(from, toExclusive, step);
    }

    protected LazyList<Character> rangeClosed(char from, char toInclusive) {
        return LazyList.rangeClosed(from, toInclusive);
    }

    protected LazyList<Character> rangeClosedBy(char from, char toInclusive, int step) {
        return LazyList.rangeClosedBy(from, toInclusive, step);
    }

    protected LazyList<Double> rangeClosedBy(double from, double toInclusive, double step) {
        return LazyList.rangeClosedBy(from, toInclusive, step);
    }

    protected LazyList<Integer> rangeClosed(int from, int toInclusive) {
        return LazyList.rangeClosed(from, toInclusive);
    }

    protected LazyList<Integer> rangeClosedBy(int from, int toInclusive, int step) {
        return LazyList.rangeClosedBy(from, toInclusive, step);
    }

    protected LazyList<Long> rangeClosed(long from, long toInclusive) {
        return LazyList.rangeClosed(from, toInclusive);
    }

    protected LazyList<Long> rangeClosedBy(long from, long toInclusive, long step) {
        return LazyList.rangeClosedBy(from, toInclusive, step);
    }

    protected <T> LazyList<LazyList<T>> transpose(LazyList<LazyList<T>> rows) {
        return LazyList.transpose(rows);
    }

    protected boolean useIsEqualToInsteadOfIsSameAs() {
        return true;
    }

    @Test
    public void shouldRemoveNonExistingElement() {
        LazyList<Integer> t = of(1, 2, 3);
        assertThat(t.remove(4)).isEqualTo(t).isNotSameAs(t);
    }

    @Test
    public void shouldRemoveFirstElementByPredicateNonExisting() {
        LazyList<Integer> t = of(1, 2, 3);
        assertThat(t.removeFirst(v -> v == 4)).isEqualTo(t).isNotSameAs(t);
    }

    @Test
    public void shouldRemoveLastElementByPredicateNonExisting() {
        LazyList<Integer> t = of(1, 2, 3);
        assertThat(t.removeLast(v -> v == 4)).isEqualTo(t).isNotSameAs(t);
    }

    @Test
    public void shouldNotRemoveAllNonExistingElementsFromNonNil() {
        LazyList<Integer> t = of(1, 2, 3);
        assertThat(t.removeAll(of(4, 5))).isEqualTo(t).isNotSameAs(t);
    }

    @SuppressWarnings("deprecation")
    @Test
    public void shouldRemoveExistingElements() {
        LazyList<Integer> seq = of(1, 2, 3);
        assertThat(seq.removeAll(i -> i == 1)).isEqualTo(of(2, 3));
        assertThat(seq.removeAll(i -> i == 2)).isEqualTo(of(1, 3));
        assertThat(seq.removeAll(i -> i == 3)).isEqualTo(of(1, 2));
        assertThat(seq.removeAll(ignore -> true)).isEmpty();
        assertThat(seq.removeAll(ignore -> false)).isEqualTo(of(1, 2, 3)).isNotSameAs(of(1, 2, 3));
    }

    @SuppressWarnings("deprecation")
    @Test
    public void shouldNotRemoveAllNonMatchedElementsFromNonNil() {
        LazyList<Integer> t = of(1, 2, 3);
        Predicate<Integer> isTooBig = i -> i >= 4;
        assertThat(t.removeAll(isTooBig)).isEqualTo(t).isNotSameAs(t);
    }

    @Test
    public void shouldNotRemoveAllNonObjectsElementsFromNonNil() {
        LazyList<Integer> seq = of(1, 2, 3);
        assertThat(seq.removeAll(4)).isEqualTo(seq).isNotSameAs(seq);
    }

    @Nested
    class StaticConcatTests {
        @Test
        public void shouldConcatEmptyIterableIterable() {
            Iterable<Iterable<Integer>> empty = List.empty();
            assertThat(concat(empty)).isSameAs(empty());
        }

        @Test
        public void shouldConcatNonEmptyIterableIterable() {
            Iterable<Iterable<Integer>> itIt = List.of(List.of(1, 2), List.of(3));
            assertThat(concat(itIt)).isEqualTo(of(1, 2, 3));
        }

        @Test
        public void shouldConcatEmptyArrayIterable() {
            assertThat(concat()).isSameAs(empty());
        }

        @Test
        public void shouldConcatNonEmptyArrayIterable() {
            assertThat(concat(List.of(1, 2), List.of(3))).isEqualTo(of(1, 2, 3));
        }
    }

    @Nested
    class StaticOfAllJavaStreamTests {
        @Test
        public void shouldNameTheJavaStreamParameterWhenItIsNull() {
            assertThatNullPointerException()
                    .isThrownBy(() -> LazyList.ofAll((java.util.stream.Stream<Integer>) null))
                    .withMessage("javaStream is null");
        }
    }

    @Nested
    class StaticFromIntTests {
        @Test
        public void shouldGenerateIntStream() {
            assertThat(LazyList.from(-1).take(3)).isEqualTo(LazyList.of(-1, 0, 1));
        }

        @Test
        public void shouldGenerateOverflowingIntStream() {
            //noinspection NumericOverflow
            assertThat(LazyList.from(Integer.MAX_VALUE).take(2))
                    .isEqualTo(LazyList.of(Integer.MAX_VALUE, Integer.MAX_VALUE + 1));
        }
    }

    @Nested
    class StaticFromIntIntTests {
        @Test
        public void shouldGenerateIntStreamWithStep() {
            assertThat(LazyList.from(-1, 6).take(3)).isEqualTo(LazyList.of(-1, 5, 11));
        }

        @Test
        public void shouldGenerateOverflowingIntStreamWithStep() {
            //noinspection NumericOverflow
            assertThat(LazyList.from(Integer.MAX_VALUE, 2).take(2))
                    .isEqualTo(LazyList.of(Integer.MAX_VALUE, Integer.MAX_VALUE + 2));
        }
    }

    @Nested
    class StaticFromLongTests {
        @Test
        public void shouldGenerateLongStream() {
            assertThat(LazyList.from(-1L).take(3)).isEqualTo(LazyList.of(-1L, 0L, 1L));
        }

        @Test
        public void shouldGenerateOverflowingLongStream() {
            //noinspection NumericOverflow
            assertThat(LazyList.from(Long.MAX_VALUE).take(2))
                    .isEqualTo(LazyList.of(Long.MAX_VALUE, Long.MAX_VALUE + 1));
        }
    }

    @Nested
    class StaticFromLongLongTests {
        @Test
        public void shouldGenerateLongStreamWithStep() {
            assertThat(LazyList.from(-1L, 5L).take(3)).isEqualTo(LazyList.of(-1L, 4L, 9L));
        }

        @Test
        public void shouldGenerateOverflowingLongStreamWithStep() {
            //noinspection NumericOverflow
            assertThat(LazyList.from(Long.MAX_VALUE, 2).take(2))
                    .isEqualTo(LazyList.of(Long.MAX_VALUE, Long.MAX_VALUE + 2));
        }
    }

    @Nested
    class StaticContinuallySupplierTests {
        @Test
        public void shouldGenerateInfiniteLazyListBasedOnSupplier() {
            assertThat(LazyList.continually(() -> 1).take(13).reduce((i, j) -> i + j))
                    .isEqualTo(13);
        }
    }

    @Nested
    class StaticIterateTFunctionTests {
        @Test
        public void shouldGenerateInfiniteLazyListBasedOnSupplierWithAccessToPreviousValue() {
            assertThat(LazyList.iterate(2, (i) -> i + 2).take(3).reduce((i, j) -> i + j))
                    .isEqualTo(12);
        }
    }

    @Nested
    class StaticIterateSupplierOptionTests {
        @Test
        public void shouldGenerateInfiniteLazyListBasedOnOptionSupplier() {
            assertThat(LazyList.iterate(() -> Option.some(1)).take(5).reduce((i, j) -> i + j))
                    .isEqualTo(5);
        }
    }

    @Nested
    class StaticContinuallyTTests {
        @Test
        public void shouldGenerateInfiniteLazyListBasedOnRepeatedElement() {
            assertThat(LazyList.continually(2).take(3).reduce((i, j) -> i + j)).isEqualTo(6);
        }
    }

    @Nested
    class StaticConsTSupplierTests {
        @Test
        public void shouldBuildLazyListBasedOnHeadAndTailSupplierWithAccessToHead() {
            assertThat(LazyList.cons(1, () -> LazyList.cons(2, LazyList::empty)))
                    .isEqualTo(LazyList.of(1, 2));
        }
    }

    @Nested
    class LazyListStaticNarrowTests {
        @Test
        public void shouldNarrowLazyList() {
            LazyList<Double> doubles = of(1.0d);
            LazyList<Number> numbers = LazyList.narrow(doubles);
            int actual = numbers.append(new BigDecimal("2.0")).sumInt(Number::intValue);
            assertThat(actual).isEqualTo(3);
        }
    }

    @Nested
    class StaticOfallTests {
        @Test
        public void shouldReturnSelfWhenIterableIsInstanceOfLazyList() {
            LazyList<Integer> source = ofAll(1, 2, 3);
            LazyList<Integer> target = LazyList.ofAll(source);
            assertThat(target).isSameAs(source);
        }

        @Test
        public void shouldReturnSelfWhenIterableIsInstanceOfListView() {
            LazyList<Integer> persistent = ofAll(1, 2, 3);
            LazyList<Integer> target = LazyList.ofAll(persistent.asJava());
            assertThat(target).isSameAs(persistent);
        }
    }

    @Nested
    class AppendTests {
        @Test
        public void shouldAppendMillionTimes() {
            int bigNum = 1_000_000;
            assertThat(LazyList.range(0, bigNum)
                            .foldLeft(LazyList.empty(), LazyList::append)
                            .size())
                    .isEqualTo(bigNum);
        }
    }

    @Nested
    class AppendallTests {
        @Test
        public void shouldAppendAll() {
            assertThat(of(1, 2, 3).appendAll(of(4, 5, 6))).isEqualTo(of(1, 2, 3, 4, 5, 6));
        }

        @Test
        public void shouldAppendAllIfThisIsEmpty() {
            assertThat(empty().appendAll(of(4, 5, 6))).isEqualTo(of(4, 5, 6));
        }

        @Test
        public void shouldAppendAllIfThatIsInfinite() {
            assertThat(of(1, 2, 3).appendAll(LazyList.from(4)).take(6)).isEqualTo(of(1, 2, 3, 4, 5, 6));
        }

        @Test
        public void shouldAppendAllToInfiniteLazyList() {
            assertThat(LazyList.from(1)
                            .appendAll(LazyList.continually(() -> -1))
                            .take(6))
                    .isEqualTo(of(1, 2, 3, 4, 5, 6));
        }
    }

    @Nested
    class LazyListCombinationsTests {
        @Test
        public void shouldComputeCombinationsOfEmptyLazyList() {
            assertThat(LazyList.empty().combinations()).isEqualTo(LazyList.of(LazyList.empty()));
        }

        @Test
        public void shouldComputeCombinationsOfNonEmptyLazyList() {
            assertThat(LazyList.of(1, 2, 3).combinations())
                    .isEqualTo(LazyList.of(
                            LazyList.empty(),
                            LazyList.of(1),
                            LazyList.of(2),
                            LazyList.of(3),
                            LazyList.of(1, 2),
                            LazyList.of(1, 3),
                            LazyList.of(2, 3),
                            LazyList.of(1, 2, 3)));
        }
    }

    @Nested
    class LazyListCombinationsKTests {
        @Test
        public void shouldComputeKCombinationsOfEmptyLazyList() {
            assertThat(LazyList.empty().combinations(1)).isEqualTo(LazyList.empty());
        }

        @Test
        public void shouldComputeKCombinationsOfNonEmptyLazyList() {
            assertThat(LazyList.of(1, 2, 3).combinations(2))
                    .isEqualTo(LazyList.of(LazyList.of(1, 2), LazyList.of(1, 3), LazyList.of(2, 3)));
        }
    }

    @Nested
    class FlatmapTests {
        @Test
        public void shouldFlatMapInfiniteTraversable() {
            assertThat(LazyList.iterate(1, i -> i + 1)
                            .flatMap(i -> List.of(i, 2 * i))
                            .take(7))
                    .isEqualTo(LazyList.of(1, 2, 2, 4, 3, 6, 4));
        }
    }

    @Nested
    class PartitionTests {
        @Test
        public void shouldPartitionInTwoIterations() {
            AtomicInteger count = new AtomicInteger(0);
            Tuple2<LazyList<Integer>, LazyList<Integer>> results = LazyList.of(1, 2, 3)
                    .partition(i -> {
                        count.incrementAndGet();
                        return true;
                    });
            assertThat(results._1()).isEqualTo(of(1, 2, 3));
            assertThat(results._2()).isEqualTo(of());
            assertThat(count.get()).isEqualTo(6);
        }

        @Test
        public void shouldPartitionLazily() {
            java.util.Set<Integer> itemsCalled = new java.util.HashSet<>();

            LazyList<Integer> infiniteLazyList = LazyList.iterate(0, i -> i + 1);
            assertThat(itemsCalled).isEmpty();

            Tuple2<LazyList<Integer>, LazyList<Integer>> results = infiniteLazyList.partition(i -> {
                itemsCalled.add(i);
                return i % 2 == 0;
            });
            assertThat(itemsCalled).isEmpty(); // nothing is evaluated before a side is read
            assertThat(results._1().head()).isEqualTo(0);
            assertThat(results._2().head()).isEqualTo(1);
            assertThat(itemsCalled).containsExactly(0, 1);
            assertThat(results._2().head()).isEqualTo(1);
            assertThat(results._1().take(3)).isEqualTo(of(0, 2, 4));
            assertThat(results._2().take(3)).isEqualTo(of(1, 3, 5));
            assertThat(itemsCalled).containsExactly(0, 1, 2, 3, 4, 5);
        }
    }

    @Nested
    class LazyListPermutationsTests {
        @Test
        public void shouldComputePermutationsOfEmptyLazyList() {
            assertThat(LazyList.empty().permutations()).isEqualTo(LazyList.empty());
        }

        @Test
        public void shouldComputePermutationsOfNonEmptyLazyList() {
            assertThat(LazyList.of(1, 2, 3).permutations())
                    .isEqualTo(LazyList.ofAll(LazyList.of(
                            LazyList.of(1, 2, 3),
                            LazyList.of(1, 3, 2),
                            LazyList.of(2, 1, 3),
                            LazyList.of(2, 3, 1),
                            LazyList.of(3, 1, 2),
                            LazyList.of(3, 2, 1))));
        }
    }

    @Nested
    class AppendselfTests {
        @Test
        public void shouldRecurrentlyCalculateFibonacci() {
            assertThat(LazyList.of(1, 1)
                            .appendSelf(self -> self.zip(self.tail()).map(t -> t._1() + t._2()))
                            .take(10))
                    .isEqualTo(LazyList.of(1, 1, 2, 3, 5, 8, 13, 21, 34, 55));
        }

        @Test
        public void shouldRecurrentlyCalculatePrimes() {
            assertThat(LazyList.of(2)
                            .appendSelf(self -> LazyList.iterate(3, i -> i + 2)
                                    .filter(i -> self.takeWhile(j -> j * j <= i).forAll(k -> i % k > 0)))
                            .take(10))
                    .isEqualTo(LazyList.of(2, 3, 5, 7, 11, 13, 17, 19, 23, 29));
        }

        @Test
        public void shouldDoNothingOnNil() {
            assertThat(LazyList.empty().appendSelf(self -> self)).isEqualTo(LazyList.empty());
        }

        @Test
        public void shouldRecurrentlyCalculateArithmeticProgression() {
            assertThat(LazyList.of(1).appendSelf(self -> self.map(t -> t + 1)).take(4))
                    .isEqualTo(LazyList.of(1, 2, 3, 4));
        }

        @Test
        public void shouldRecurrentlyCalculateGeometricProgression() {
            assertThat(LazyList.of(1).appendSelf(self -> self.map(t -> t * 2)).take(4))
                    .isEqualTo(LazyList.of(1, 2, 4, 8));
        }
    }

    @Nested
    class LazyListContainssliceTests {
        @Test
        public void shouldRecognizeInfiniteDoesContainSlice() {
            boolean actual = LazyList.iterate(1, i -> i + 1).containsSlice(of(12, 13, 14));
            assertThat(actual).isTrue();
        }
    }

    @Nested
    class CycleTests {
        @Test
        public void shouldCycleEmptyLazyList() {
            assertThat(empty().cycle()).isEqualTo(empty());
        }

        @Test
        public void shouldCycleNonEmptyLazyList() {
            assertThat(of(1, 2, 3).cycle().take(9)).isEqualTo(of(1, 2, 3, 1, 2, 3, 1, 2, 3));
        }
    }

    @Nested
    class CycleIntTests {
        @Test
        public void shouldCycleTimesEmptyLazyList() {
            assertThat(empty().cycle(3)).isEqualTo(empty());
        }

        @Test
        public void shouldCycleTimesNonEmptyLazyList() {
            assertThat(of(1, 2, 3).cycle(-1)).isEqualTo(empty());
            assertThat(of(1, 2, 3).cycle(0)).isEqualTo(empty());
            assertThat(of(1, 2, 3).cycle(1)).isEqualTo(of(1, 2, 3));
            assertThat(of(1, 2, 3).cycle(3)).isEqualTo(of(1, 2, 3, 1, 2, 3, 1, 2, 3));
        }
    }

    @Nested
    class DropuntilTests {
        @Test
        public void shouldDropInfiniteLazyListUntilPredicate() {
            LazyList<Integer> naturalNumbers = LazyList.iterate(0, i -> i + 1);
            LazyList<Integer> naturalNumbersBiggerThanTen = naturalNumbers.dropUntil(i -> i > 10);
            Integer firstNaturalNumberBiggerThanTen = naturalNumbersBiggerThanTen.head();
            assertThat(firstNaturalNumberBiggerThanTen).isEqualTo(11);
        }
    }

    @Nested
    class DroprightTests {
        @Test
        public void shouldLazyDropRight() {
            assertThat(LazyList.from(1).takeUntil(i -> i == 18).dropRight(7)).isEqualTo(LazyList.range(1, 11));
        }
    }

    @Nested
    class ExtendTests {
        @Test
        public void shouldExtendLazyListWithConstantValue() {
            assertThat(LazyList.of(1, 2, 3).extend(42).take(6)).isEqualTo(of(1, 2, 3, 42, 42, 42));
        }

        @Test
        public void shouldExtendLazyListWithSupplier() {
            assertThat(LazyList.of(1, 2, 3).extend(() -> 42).take(6)).isEqualTo(of(1, 2, 3, 42, 42, 42));
        }

        @Test
        public void shouldExtendLazyListWithFunction() {
            assertThat(LazyList.of(1, 2, 3).extend(i -> i + 1).take(6)).isEqualTo(of(1, 2, 3, 4, 5, 6));
        }

        @Test
        public void shouldExtendEmptyLazyListWithConstantValue() {
            assertThat(LazyList.of().extend(42).take(6)).isEqualTo(of(42, 42, 42, 42, 42, 42));
        }

        @Test
        public void shouldExtendEmptyLazyListWithSupplier() {
            assertThat(LazyList.of().extend(() -> 42).take(6)).isEqualTo(of(42, 42, 42, 42, 42, 42));
        }

        @Test
        public void shouldReturnAnEmptyLazyListWhenExtendingAnEmptyLazyListWithFunction() {
            assertThat(LazyList.<Integer>of().extend(i -> i + 1)).isEqualTo(of());
        }

        @Test
        public void shouldReturnTheOriginalLazyListWhenTryingToExtendInfiniteLazyListWithConstantValue() {
            assertThat(LazyList.continually(1).extend(42).take(6)).isEqualTo(of(1, 1, 1, 1, 1, 1));
        }

        @Test
        public void shouldReturnTheOriginalLazyListWhenTryingToExtendInfiniteLazyListWithSupplier() {
            assertThat(LazyList.continually(1).extend(() -> 42).take(6)).isEqualTo(of(1, 1, 1, 1, 1, 1));
        }

        @Test
        public void shouldReturnTheOriginalLazyListWhenTryingToExtendInfiniteLazyListWithFunction() {
            assertThat(LazyList.continually(1).extend(i -> i + 1).take(6)).isEqualTo(of(1, 1, 1, 1, 1, 1));
        }
    }

    // -- subSequence(int, int)

    @Disabled
    @Test
    public void shouldReturnSameInstanceIfSubSequenceStartsAtZeroAndEndsAtLastElement() {
        // LazyList is lazy
    }

    @Nested
    class TailTests {
        @Test
        public void shouldEvaluateTailAtMostOnce() {
            int[] counter = {0};
            LazyList<Integer> stream = LazyList.continually(() -> counter[0]++);
            // this test ensures that the `tail.append(100)` does not modify the tail elements
            LazyList<Integer> tail = stream.tail().append(100);
            String expected = stream.drop(1).take(3).mkString(",");
            String actual = tail.take(3).mkString(",");
            assertThat(expected).isEqualTo("1,2,3");
            assertThat(actual).isEqualTo(expected);
        }

        @Test
        public void shouldNotProduceStackOverflow() {
            LazyList.range(0, 1_000_000)
                    .map(String::valueOf)
                    .foldLeft(LazyList.<String>empty(), LazyList::append)
                    .mkString();
        }

        @Test // See #327, #594
        public void shouldNotEvaluateHeadOfTailWhenCallingIteratorHasNext() {

            Integer[] vals = new Integer[] {1, 2, 3, 4, 5, 6, 7, 8, 9};

            CheckedFunction2<StringBuilder, Integer, Void> doStuff = (builder, i) -> {
                builder.append(i);
                if (i == 5) {
                    throw new Exception("Some error !!!");
                } else {
                    return null;
                }
            };

            StringBuilder actual = new StringBuilder();
            CheckedFunction1<Integer, Void> consumer1 = doStuff.apply(actual);
            LazyList.of(vals)
                    .map(v -> Try.run(() -> consumer1.apply(v)))
                    .find(Try::isFailure)
                    .getOrElse(() -> Try.success(Tuple.empty()));

            StringBuilder expected = new StringBuilder();
            CheckedFunction1<Integer, Void> consumer2 = doStuff.apply(expected);
            java.util.stream.Stream.of(vals)
                    .map(v -> Try.run(() -> consumer2.apply(v)))
                    .filter(Try::isFailure)
                    .findFirst()
                    .orElseGet(() -> Try.success(Tuple.empty()));

            assertThat(actual.toString()).isEqualTo(expected.toString());
        }
    }

    // -- take

    @Test
    public void shouldNotEvaluateNPlusOneWhenTakeN() {
        Predicate<Integer> hiddenThrow = i -> {
            if (i == 0) {
                return true;
            } else {
                throw new IllegalArgumentException();
            }
        };
        assertThat(LazyList.from(0).filter(hiddenThrow).take(1).sumInt(Number::intValue))
                .isEqualTo(0);
    }

    @Disabled
    @Test
    public void shouldTakeZeroOfEmptyFilteredInfiniteLazyList() {
        assertThat(LazyList.continually(1).filter(i -> false).take(0).isEmpty()).isTrue();
    }

    @Disabled
    @Test
    public void shouldTakeZeroOfEmptyFlatMappedInfiniteLazyList() {
        assertThat(LazyList.continually(1)
                        .flatMap(i -> LazyList.empty())
                        .take(0)
                        .isEmpty())
                .isTrue();
    }

    @Disabled
    @Test
    public void shouldReturnSameInstanceIfTakeAll() {
        // the size of a possibly infinite LazyList is unknown
    }

    @Nested
    class ToLazyListTests {
        @Test
        public void shouldReturnSelfOnConvertToLazyList() {
            LazyList<Integer> value = of(1, 2, 3);
            assertThat(value.toLazyList()).isSameAs(value);
        }
    }

    // -- toString

    @Test
    public void shouldStringifyNil() {
        assertThat(empty().toString()).isEqualTo("LazyList()");
    }

    @Test
    public void shouldStringifyNonNil() {
        LazyList<Integer> list = of(1, 2, 3);
        assertThat(list.toString()).isEqualTo("LazyList(<not computed>)");
        list.head(); // evaluates the first cell
        assertThat(list.toString()).isEqualTo("LazyList(1, <not computed>)");
    }

    @Test
    public void shouldStringifyNonNilEvaluatingFirstTail() {
        LazyList<Integer> stream = this.of(1, 2, 3);
        stream.tail().head(); // evaluates the first two cells
        assertThat(stream.toString()).isEqualTo("LazyList(1, 2, <not computed>)");
    }

    @Test
    public void shouldStringifyNonNilAndNilTail() {
        LazyList<Integer> stream = this.of(1);
        stream.tail(); // evaluates empty tail
        assertThat(stream.toString()).isEqualTo("LazyList(1)");
    }

    @Nested
    class ToStringTests {

        // a list whose cells, once evaluated, loop back to its first cell: each cell links straight to the next one,
        // with no copy of a cell closing the loop
        @SafeVarargs
        private static <T> LazyList<T> loop(T... elements) {
            return loop(Vector.of(elements));
        }

        private static <T> LazyList<T> loop(Vector<T> elements) {
            @SuppressWarnings("unchecked")
            LazyList<T>[] self = new LazyList[1];
            self[0] = LazyList.defer(() -> elements.foldRight(self[0], (e, list) -> list.prepend(e)));
            return self[0];
        }

        // the first n elements of list, evaluated
        private static <T> LazyList<T> read(LazyList<T> list, int n) {
            list.take(n).size();
            return list;
        }

        private static String shown(Vector<String> parts) {
            return parts.mkString("LazyList(", ", ", ")");
        }

        @Test
        void showsAFiniteListAsItIsEvaluated() {
            LazyList<Integer> list = LazyList.of(1, 2, 3);
            assertThat(list.toString()).isEqualTo("LazyList(<not computed>)");
            read(list, 1);
            assertThat(list.toString()).isEqualTo("LazyList(1, <not computed>)");
            read(list, 2);
            assertThat(list.toString()).isEqualTo("LazyList(1, 2, <not computed>)");
            read(list, 3);
            assertThat(list.toString()).isEqualTo("LazyList(1, 2, 3, <not computed>)");
            list.size();
            assertThat(list.toString()).isEqualTo("LazyList(1, 2, 3)");
        }

        @Test
        void showsEveryLengthEvaluatedOrNot() {
            // odd and even lengths: the scout stops at the end on either of its two steps
            for (int n = 1; n <= 12; n++) {
                for (int evaluated = 0; evaluated <= n; evaluated++) {
                    LazyList<Integer> list = read(LazyList.range(0, n), evaluated);
                    Vector<String> elements = Vector.range(0, evaluated).map(String::valueOf);
                    String expected =
                            evaluated == 0 ? "LazyList(<not computed>)" : shown(elements.append("<not computed>"));
                    assertThat(list.toString()).as("%d of %d", evaluated, n).isEqualTo(expected);
                }
                LazyList<Integer> whole = LazyList.range(0, n);
                whole.size();
                assertThat(whole.toString()).isEqualTo(LazyList.range(0, n).mkString("LazyList(", ", ", ")"));
            }
        }

        @Test
        void showsTheEmptyList() {
            assertThat(LazyList.empty().toString()).isEqualTo("LazyList()");
            LazyList<Integer> deferred = LazyList.defer(LazyList::empty);
            assertThat(deferred.toString()).isEqualTo("LazyList(<not computed>)");
            assertThat(deferred.isEmpty()).isTrue();
            assertThat(deferred.toString()).isEqualTo("LazyList()");
        }

        @Test
        void showsTheEvaluatedPrefixOfAnInfiniteList() {
            assertThat(read(LazyList.from(1), 5).toString()).isEqualTo("LazyList(1, 2, 3, 4, 5, <not computed>)");
            AtomicInteger next = new AtomicInteger();
            LazyList<Integer> continually = read(LazyList.continually(next::incrementAndGet), 4);
            assertThat(continually.toString()).isEqualTo("LazyList(1, 2, 3, 4, <not computed>)");
            assertThat(read(LazyList.iterate(1, i -> i * 2), 3).toString())
                    .isEqualTo("LazyList(1, 2, 4, <not computed>)");
        }

        @Test
        void showsACycleAsTheIssueReportsIt() {
            LazyList<Integer> cyclic = LazyList.of(1, 2, 3).cycle();
            cyclic.drop(5).head();
            assertThat(cyclic.toString()).isEqualTo("LazyList(1, 2, 3, <cycle>)");
        }

        @Test
        void showsACycleOnceItIsClosed() {
            LazyList<Integer> cyclic = LazyList.of(1, 2, 3).cycle();
            assertThat(cyclic.toString()).isEqualTo("LazyList(<not computed>)");
            read(cyclic, 2);
            assertThat(cyclic.toString()).isEqualTo("LazyList(1, 2, <not computed>)");
            read(cyclic, 3);
            assertThat(cyclic.toString()).isEqualTo("LazyList(1, 2, 3, <not computed>)");
            read(cyclic, 4);
            assertThat(cyclic.toString()).isEqualTo("LazyList(1, 2, 3, <cycle>)");
        }

        @Test
        void showsCyclesOfLengthOneAndTwo() {
            assertThat(read(LazyList.of(1).cycle(), 3).toString()).isEqualTo("LazyList(1, <cycle>)");
            assertThat(read(LazyList.of(1, 2).cycle(), 5).toString()).isEqualTo("LazyList(1, 2, <cycle>)");
            assertThat(read(loop(1), 1).toString()).isEqualTo("LazyList(1, <cycle>)");
            assertThat(read(loop(1, 2), 2).toString()).isEqualTo("LazyList(1, 2, <cycle>)");
            assertThat(read(LazyList.continually(7).take(1).cycle(), 3).toString())
                    .isEqualTo("LazyList(7, <cycle>)");
        }

        @Test
        void showsASelfReferencingCons() {
            @SuppressWarnings("unchecked")
            LazyList<Integer>[] self = new LazyList[1];
            self[0] = LazyList.cons(1, () -> self[0]);
            assertThat(self[0].toString()).isEqualTo("LazyList(1, <not computed>)");
            read(self[0], 3);
            assertThat(self[0].toString()).isEqualTo("LazyList(1, <cycle>)");
        }

        @Test
        void showsAppendSelf() {
            LazyList<Integer> cyclic = read(LazyList.of(1, 2, 3).appendSelf(s -> s), 10);
            assertThat(cyclic.toString()).isEqualTo("LazyList(1, 2, 3, <cycle>)");
            // 1, 2, 3, then 2, 3 again and again: the loop is 2, 3
            LazyList<Integer> fromSecond = read(LazyList.of(1, 2, 3).appendSelf(LazyList::tail), 10);
            assertThat(fromSecond.toString()).isEqualTo("LazyList(1, 2, 3, <cycle>)");
            assertThat(fromSecond.take(7)).isEqualTo(LazyList.of(1, 2, 3, 2, 3, 2, 3));
        }

        @Test
        void showsCyclesOfManyLengthsAfterPrefixesOfManyLengths() {
            for (int length = 1; length <= 20; length++) {
                for (int prefix = 0; prefix <= 20; prefix++) {
                    Vector<String> cycle = Vector.range(0, length).map(i -> "c" + i);
                    Vector<String> before = Vector.range(0, prefix).map(i -> "p" + i);
                    String expected = shown(before.appendAll(cycle).append("<cycle>"));
                    // a loop closed by a straight link back, behind prefix cells that each link to the next
                    LazyList<String> straight = before.foldRight(loop(cycle), (e, l) -> l.prepend(e));
                    assertThat(read(straight, 2 * (prefix + length) + 2).toString())
                            .as("straight loop of %d after %d", length, prefix)
                            .isEqualTo(expected);
                    // a loop closed by a copy of its first cell, as cycle() builds it, appended to a prefix
                    LazyList<String> copied = LazyList.ofAll(before)
                            .appendAll(LazyList.ofAll(cycle).cycle());
                    assertThat(read(copied, 2 * (prefix + length) + 2).toString())
                            .as("cycle() of %d after %d", length, prefix)
                            .isEqualTo(expected);
                }
            }
        }

        @Test
        void keepsTheLastCellOfALoopThatLinksStraightBack() {
            // 9, then 1, 0 again and again: the loop starts at the second cell and its last cell links straight back
            LazyList<Integer> list = read(loop(1, 0).prepend(9), 6);
            assertThat(list.toString()).isEqualTo("LazyList(9, 1, 0, <cycle>)");
        }

        @Test
        void leavesOutTheLastCellOfALoopWhenItShowsTheSameElements() {
            // a, then b, a again and again: the same elements as a, b again and again
            String a = "a";
            LazyList<String> list = read(loop("b", a).prepend(a), 6);
            assertThat(list.toString()).isEqualTo("LazyList(a, b, <cycle>)");
        }

        @Test
        void showsAFailedCellAsNotComputed() {
            @SuppressWarnings("unchecked")
            LazyList<Integer>[] self = new LazyList[1];
            self[0] = LazyList.defer(() -> self[0]);
            assertThatThrownBy(() -> self[0].isEmpty()).isInstanceOf(IllegalStateException.class);
            assertThat(self[0].toString()).isEqualTo("LazyList(<not computed>)");
            LazyList<Integer> failedTail = LazyList.cons(1, () -> {
                throw new IllegalArgumentException("boom");
            });
            assertThatThrownBy(() -> failedTail.tail().isEmpty()).isInstanceOf(IllegalArgumentException.class);
            assertThat(failedTail.toString()).isEqualTo("LazyList(1, <not computed>)");
        }

        @Test
        void evaluatesNothing() {
            AtomicInteger computed = new AtomicInteger();
            LazyList<Integer> counted = LazyList.continually(computed::incrementAndGet);
            LazyList<Integer> cyclic = counted.take(3).cycle();
            LazyList<Integer> mapped = cyclic.map(i -> i * 10);
            for (int reads = 0; reads <= 8; reads++) {
                read(mapped, reads);
                int before = computed.get();
                String unused = mapped.toString() + cyclic.toString() + counted.toString();
                assertThat(unused).isNotEmpty();
                assertThat(computed.get()).as("after %d reads", reads).isEqualTo(before);
            }
            assertThat(cyclic.toString()).isEqualTo("LazyList(1, 2, 3, <cycle>)");
            assertThat(counted.toString()).isEqualTo("LazyList(1, 2, 3, <not computed>)");
            assertThat(computed.get()).isEqualTo(3);
        }
    }

    @Test
    public void shouldNameLazyListInTheMessagesOfAnEmptyList() {
        LazyList<Integer> empty = LazyList.defer(LazyList::empty);
        assertThatThrownBy(empty::head)
                .isInstanceOf(NoSuchElementException.class)
                .hasMessage("head of empty LazyList");
        assertThatThrownBy(empty::tail)
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessage("tail of empty LazyList");
        assertThatThrownBy(empty::init)
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessage("init of empty LazyList");
    }

    @Nested
    class UnfoldTests {
        @Test
        public void shouldUnfoldToEmpty() {
            assertThat(LazyList.unfold(0, x -> Option.none())).isEqualTo(empty());
        }

        @Test
        public void shouldUnfoldElementFirstInTheOrderProduced() {
            assertThat(LazyList.unfold(10, x -> x == 0 ? Option.none() : Option.some(Tuple.of(x, x - 1))))
                    .isEqualTo(of(10, 9, 8, 7, 6, 5, 4, 3, 2, 1));
        }

        @Test
        public void shouldUnfoldWithAStateOfAnotherType() {
            // the Fibonacci numbers below 100, the state being the pair of the last two
            LazyList<Integer> fibonacci = LazyList.unfold(
                    Tuple.of(0, 1),
                    s -> s._1() > 100
                            ? Option.none()
                            : Option.some(Tuple.of(s._1(), Tuple.of(s._2(), s._1() + s._2()))));
            assertThat(fibonacci).isEqualTo(of(0, 1, 1, 2, 3, 5, 8, 13, 21, 34, 55, 89));
            LazyList<String> labels =
                    LazyList.unfold(1, i -> i > 3 ? Option.none() : Option.some(Tuple.of("#" + i, i + 1)));
            assertThat(labels.mkString(",")).isEqualTo("#1,#2,#3");
        }

        @Test
        public void shouldRejectANullResultFromF() {
            assertThatNullPointerException()
                    .isThrownBy(() -> LazyList.unfold(0, x -> null).size())
                    .withMessage("LazyList.unfold: f returned null");
            assertThatNullPointerException()
                    .isThrownBy(() -> LazyList.unfold(0, x -> x < 2 ? Option.some(Tuple.of(x, x + 1)) : null)
                            .size())
                    .withMessage("LazyList.unfold: f returned null");
            assertThatNullPointerException()
                    .isThrownBy(() -> LazyList.unfold(0, null))
                    .withMessage("f is null");
        }

        @Test
        public void shouldUnfoldAnInfiniteGeneratorLazily() {
            java.util.concurrent.atomic.AtomicInteger calls = new java.util.concurrent.atomic.AtomicInteger();
            LazyList<Integer> naturals = LazyList.unfold(0, x -> {
                calls.incrementAndGet();
                return Option.some(Tuple.of(x, x + 1));
            });
            assertThat(calls.get()).isZero();
            assertThat(naturals.take(5)).isEqualTo(LazyList.of(0, 1, 2, 3, 4));
            assertThat(calls.get()).isEqualTo(5);
            // memoised: reading the same prefix again calls nothing
            assertThat(naturals.take(5).toVector()).isEqualTo(Vector.of(0, 1, 2, 3, 4));
            assertThat(calls.get()).isEqualTo(5);
            assertThat(naturals.get(9)).isEqualTo(9);
            assertThat(calls.get()).isEqualTo(10);
        }

        @Test
        public void shouldRejectANullElementWhenTheListReachesIt() {
            LazyList<Integer> list = LazyList.unfold(0, x -> Option.some(Tuple.of(x == 2 ? null : x, x + 1)));
            assertThat(list.take(2)).isEqualTo(LazyList.of(0, 1));
            assertThatNullPointerException().isThrownBy(() -> list.get(2)).withMessage("LazyList: element is null");
        }
    }

    @Nested
    class LazyListSpliteratorTests {
        @Test
        public void shouldNotHaveSizedSpliterator() {
            assertThat(of(1, 2, 3).spliterator().hasCharacteristics(Spliterator.SIZED | Spliterator.SUBSIZED))
                    .isFalse();
        }

        @Test
        public void shouldReturnSizeWhenSpliterator() {
            assertThat(of(1, 2, 3).spliterator().getExactSizeIfKnown()).isEqualTo(-1);
        }
    }

    @Nested
    class RemoveAtLazyBoundsTests {
        @Test
        public void shouldThrowIndexOutOfBoundsWhenRemovingIndexZeroFromEmptyLazyList() {
            assertThrows(IndexOutOfBoundsException.class, () -> empty().removeAt(0));
        }

        @Test
        public void shouldThrowIndexOutOfBoundsWhenRemovingIndexEqualToLengthOnceTraversed() {
            assertThrows(
                    IndexOutOfBoundsException.class, () -> of(1, 2).removeAt(2).toList());
        }
    }

    @Nested
    class DistinctByFunctionNullCheckTests {
        @Test
        public void shouldThrowWhenDistinctByKeyExtractorIsNullOnEmptyLazyList() {
            assertThrows(NullPointerException.class, () -> empty().distinctBy((Function<Object, Object>) null));
        }
    }

    @Nested
    class CollectTests {

        @Test
        public void shouldCollectLazilyAfterTheFirstKeptElement() {
            AtomicInteger calls = new AtomicInteger();
            LazyList<Integer> actual = LazyList.from(1).collect(i -> {
                calls.incrementAndGet();
                return i % 2 == 0 ? Option.some(i) : Option.none();
            });
            assertThat(calls.get()).isZero(); // nothing is evaluated before the result is read
            assertThat(actual.head()).isEqualTo(2);
            assertThat(calls.get()).isEqualTo(2); // 1 dropped, 2 kept as the head; the rest waits
            assertThat(actual.take(3)).isEqualTo(LazyList.of(2, 4, 6));
        }

        @Test
        public void shouldCallTheCollectMapperOncePerElement() {
            AtomicInteger calls = new AtomicInteger();
            LazyList.of(1, 2, 3, 4)
                    .collect(i -> {
                        calls.incrementAndGet();
                        return i % 2 == 0 ? Option.some(i) : Option.none();
                    })
                    .toList();
            assertThat(calls.get()).isEqualTo(4);
        }

        @Test
        public void shouldReturnTheEmptyLazyListWhenNothingIsKept() {
            assertThat(LazyList.of(1, 2, 3).collect(i -> Option.none())).isEqualTo(LazyList.empty());
        }
    }

    // -- the sequence cases, one copy per type

    @Nested
    class FillIntSupplierTests {
        @Test
        public void shouldReturnManyAfterFillWithConstantSupplier() {
            assertThat(fill(17, () -> 7)).hasSize(17).containsOnly(7);
        }
    }

    @Nested
    class FillIntTTests {
        @Test
        public void shouldReturnEmptyAfterFillWithZeroCount() {
            assertThat(fill(0, 7)).isEqualTo(empty());
        }

        @Test
        public void shouldReturnEmptyAfterFillWithNegativeCount() {
            assertThat(fill(-1, 7)).isEqualTo(empty());
        }

        @Test
        public void shouldReturnManyAfterFillWithConstant() {
            assertThat(fill(17, 7)).hasSize(17).containsOnly(7);
        }
    }

    // -- append

    @Test
    public void shouldAppendElementToNil() {
        LazyList<Integer> actual = this.<Integer>empty().append(1);
        LazyList<Integer> expected = of(1);
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void shouldRejectAppendOfNullElement() {
        assertThatNullPointerException().isThrownBy(() -> this.<Integer>empty().append(null));
    }

    @Test
    public void shouldAppendElementToNonNil() {
        LazyList<Integer> actual = of(1, 2).append(3);
        LazyList<Integer> expected = of(1, 2, 3);
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void shouldMixAppendAndPrepend() {
        assertThat(of(1).append(2).prepend(0).prepend(-1).append(3).append(4)).isEqualTo(of(-1, 0, 1, 2, 3, 4));
    }

    // -- appendAll

    @Test
    public void shouldThrowOnAppendAllOfNull() {
        assertThrows(NullPointerException.class, () -> empty().appendAll(null));
    }

    @Test
    public void shouldAppendAllNilToNil() {
        LazyList<Object> actual = empty().appendAll(empty());
        LazyList<Object> expected = empty();
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void shouldAppendAllNonNilToNil() {
        LazyList<Integer> actual = this.<Integer>empty().appendAll(of(1, 2, 3));
        LazyList<Integer> expected = of(1, 2, 3);
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void shouldAppendAllNilToNonNil() {
        LazyList<Integer> actual = of(1, 2, 3).appendAll(empty());
        LazyList<Integer> expected = of(1, 2, 3);
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void shouldAppendAllNonNilToNonNil() {
        LazyList<Integer> actual = of(1, 2, 3).appendAll(of(4, 5, 6));
        LazyList<Integer> expected = of(1, 2, 3, 4, 5, 6);
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void shouldAppendAllWhenUsedWithTypeHierarchy() {
        LazyList<SomeInterface> empty = of();
        LazyList<SomeInterface> all = empty.appendAll(of(OneEnum.values())).appendAll(of(SecondEnum.values()));

        assertThat(all)
                .isEqualTo(this.<SomeInterface>of(
                        OneEnum.A1, OneEnum.A2, OneEnum.A3, SecondEnum.A1, SecondEnum.A2, SecondEnum.A3));
    }

    @Test
    public void shouldReturnSameLazyListWhenEmptyAppendAllEmpty() {
        LazyList<Integer> empty = empty();
        assertThat(empty.appendAll(empty())).isSameAs(empty);
    }

    @Test
    public void shouldReturnSameLazyListWhenEmptyAppendAllNonEmpty() {
        LazyList<Integer> seq = of(1, 2, 3);
        assertThat(empty().appendAll(seq)).isSameAs(seq);
    }

    @Test
    public void shouldReturnSameLazyListWhenNonEmptyAppendAllEmpty() {
        LazyList<Integer> seq = of(1, 2, 3);
        assertThat(seq.appendAll(empty())).isEqualTo(seq);
    }

    @Nested
    class CombinationsTests {
        @Test
        public void shouldComputeCombinationsOfEmptyList() {
            assertThat(empty().combinations()).isEqualTo(of(empty()));
        }

        @SuppressWarnings("unchecked")
        @Test
        public void shouldComputeCombinationsOfNonEmptyList() {
            assertThat(of(1, 2, 3).combinations())
                    .isEqualTo(of(empty(), of(1), of(2), of(3), of(1, 2), of(1, 3), of(2, 3), of(1, 2, 3)));
        }
    }

    @Nested
    class AsjavaTests {
        @Test
        public void shouldConvertAsJavaImmutable() {
            java.util.List<Integer> list = of(1, 2, 3).asJava();
            assertThat(list).isEqualTo(Arrays.asList(1, 2, 3));
            assertThatThrownBy(() -> list.add(4)).isInstanceOf(UnsupportedOperationException.class);
        }
    }

    @Nested
    class CombinationsKTests {
        @Test
        public void shouldComputeKCombinationsOfEmptyList() {
            assertThat(empty().combinations(1)).isEmpty();
        }

        @SuppressWarnings("unchecked")
        @Test
        public void shouldComputeKCombinationsOfNonEmptyList() {
            assertThat(of(1, 2, 3).combinations(2)).isEqualTo(of(of(1, 2), of(1, 3), of(2, 3)));
        }

        @Test
        public void shouldComputeKCombinationsOfNegativeK() {
            assertThat(of(1).combinations(-1)).isEqualTo(of(empty()));
        }
    }

    @Nested
    class ContainssliceTests {
        @Test
        public void shouldRecognizeNilNotContainsSlice() {
            boolean actual = empty().containsSlice(of(1, 2, 3));
            assertThat(actual).isFalse();
        }

        @Test
        public void shouldRecognizeNonNilDoesContainSlice() {
            boolean actual = of(1, 2, 3, 4, 5).containsSlice(of(2, 3));
            assertThat(actual).isTrue();
        }

        @Test
        public void shouldRecognizeNonNilDoesNotContainSlice() {
            boolean actual = of(1, 2, 3, 4, 5).containsSlice(of(2, 1, 4));
            assertThat(actual).isFalse();
        }
    }

    @Nested
    class CrossproductTests {
        @Test
        public void shouldCalculateCrossProductOfNil() {
            LazyList<Tuple2<Object, Object>> actual = empty().crossProduct();
            assertThat(actual).isEmpty();
        }

        @SuppressWarnings("unchecked")
        @Test
        public void shouldCalculateCrossProductOfNonNil() {
            List<Tuple2<Integer, Integer>> actual = of(1, 2, 3).crossProduct().toList();
            List<Tuple2<Integer, Integer>> expected = List.of(
                    Tuple.of(1, 1),
                    Tuple.of(1, 2),
                    Tuple.of(1, 3),
                    Tuple.of(2, 1),
                    Tuple.of(2, 2),
                    Tuple.of(2, 3),
                    Tuple.of(3, 1),
                    Tuple.of(3, 2),
                    Tuple.of(3, 3));
            assertThat(actual).isEqualTo(expected);
        }
    }

    @Nested
    class CrossproductIntTests {
        @Test
        public void shouldCalculateCrossProductPower() {
            assertThat(of(1, 2).crossProduct(0).toList()).isEqualTo(List.of(empty()));
            assertThat(of(1, 2).crossProduct(1).toList()).isEqualTo(List.of(of(1), of(2)));
            assertThat(of(1, 2).crossProduct(2).toList()).isEqualTo(List.of(of(1, 1), of(1, 2), of(2, 1), of(2, 2)));
        }

        @Test
        public void shouldCrossProductPowerBeLazy() {
            assertThat(range(0, 10).crossProduct(100).take(1).head()).isEqualTo(tabulate(100, i -> 0));
        }

        @Test
        public void shouldCrossProductOfNegativePowerBeEmpty() {
            assertThat(of(1, 2).crossProduct(-1).toList()).isEqualTo(List.empty());
        }
    }

    @Nested
    class CrossproductIterableTests {
        @Test
        public void shouldCalculateCrossProductOfNilAndNil() {
            LazyList<Tuple2<Object, Object>> actual = empty().crossProduct(empty());
            assertThat(actual).isEmpty();
        }

        @Test
        public void shouldCalculateCrossProductOfNilAndNonNil() {
            LazyList<Tuple2<Object, Object>> actual = empty().crossProduct(of(1, 2, 3));
            assertThat(actual).isEmpty();
        }

        @Test
        public void shouldCalculateCrossProductOfNonNilAndNil() {
            LazyList<Tuple2<Integer, Integer>> actual = of(1, 2, 3).crossProduct(empty());
            assertThat(actual).isEmpty();
        }

        @SuppressWarnings("unchecked")
        @Test
        public void shouldCalculateCrossProductOfNonNilAndNonNil() {
            List<Tuple2<Integer, Character>> actual =
                    of(1, 2, 3).crossProduct(of('a', 'b')).toList();
            List<Tuple2<Integer, Character>> expected = of(
                            Tuple.of(1, 'a'),
                            Tuple.of(1, 'b'),
                            Tuple.of(2, 'a'),
                            Tuple.of(2, 'b'),
                            Tuple.of(3, 'a'),
                            Tuple.of(3, 'b'))
                    .toList();
            assertThat(actual).isEqualTo(expected);
        }

        @Test
        public void shouldThrowWhenCalculatingCrossProductAndThatIsNull() {
            assertThrows(NullPointerException.class, () -> empty().crossProduct(null));
        }

        @Test
        public void shouldCalculateCrossProductWithAOneShotArgument() {
            // a java.util.stream can be iterated once: the argument is read exactly once
            Iterable<Character> oneShot = java.util.stream.Stream.of('a', 'b')::iterator;
            assertThat(of(1, 2).crossProduct(oneShot).toList())
                    .isEqualTo(List.of(Tuple.of(1, 'a'), Tuple.of(1, 'b'), Tuple.of(2, 'a'), Tuple.of(2, 'b')));
        }
    }

    @Nested
    class DroprightuntilTests {
        @Test
        public void shouldDropRightUntilNoneOnNil() {
            assertThat(empty().dropRightUntil(ignored -> true)).isSameAs(empty());
        }

        @Test
        public void shouldDropRightUntilNoneIfPredicateIsTrue() {
            LazyList<Integer> values = of(1, 2, 3);
            assertThat(values.dropRightUntil(ignored -> true)).isEqualTo(values);
        }

        @Test
        public void shouldDropRightUntilAllIfPredicateIsFalse() {
            assertThat(of(1, 2, 3).dropRightUntil(ignored -> false)).isEqualTo(empty());
        }

        @Test
        public void shouldDropRightUntilCorrect() {
            assertThat(of(1, 2, 3).dropRightUntil(i -> i <= 2)).isEqualTo(of(1, 2));
        }
    }

    // -- dropRightWhile

    @Test
    public void shouldDropRightWhileNoneOnNil() {
        assertThat(empty().dropRightWhile(ignored -> true)).isSameAs(empty());
    }

    @Test
    public void shouldDropRightWhileNoneIfPredicateIsFalse() {
        LazyList<Integer> values = of(1, 2, 3);
        assertThat(values.dropRightWhile(ignored -> false)).isEqualTo(values);
    }

    @Test
    public void shouldDropRightWhileAllIfPredicateIsTrue() {
        assertThat(of(1, 2, 3).dropRightWhile(ignored -> true)).isEqualTo(empty());
    }

    @Test
    public void shouldDropRightWhileAccordingToPredicate() {
        assertThat(of(1, 2, 3).dropRightWhile(i -> i > 2)).isEqualTo(of(1, 2));
    }

    @Test
    public void shouldDropRightWhileAndNotTruncate() {
        assertThat(of(1, 2, 3).dropRightWhile(i -> i % 2 == 1)).isEqualTo(of(1, 2));
    }

    @Nested
    class GetTests {
        @Test
        public void shouldThrowWhenGetWithNegativeIndexOnNil() {
            assertThrows(IndexOutOfBoundsException.class, () -> empty().get(-1));
        }

        @Test
        public void shouldThrowWhenGetWithNegativeIndexOnNonNil() {
            assertThrows(IndexOutOfBoundsException.class, () -> of(1).get(-1));
        }

        @Test
        public void shouldThrowWhenGetOnNil() {
            assertThrows(IndexOutOfBoundsException.class, () -> empty().get(0));
        }

        @Test
        public void shouldThrowWhenGetWithTooBigIndexOnNonNil() {
            assertThrows(IndexOutOfBoundsException.class, () -> of(1).get(1));
        }

        @Test
        public void shouldGetFirstElement() {
            assertThat(of(1, 2, 3).get(0)).isEqualTo(1);
        }

        @Test
        public void shouldGetLastElement() {
            assertThat(of(1, 2, 3).get(2)).isEqualTo(3);
        }
    }

    @Nested
    class IndexofTests {
        @Test
        public void shouldNotFindIndexOfElementWhenLazyListIsEmpty() {
            assertThat(empty().indexOf(1)).isEqualTo(-1);

            assertThat(empty().indexOfOption(1)).isEqualTo(Option.none());
        }

        @Test
        public void shouldNotFindIndexOfElementWhenStartIsGreater() {
            assertThat(of(1, 2, 3, 4).indexOf(2, 2)).isEqualTo(-1);

            assertThat(of(1, 2, 3, 4).indexOfOption(2, 2)).isEqualTo(Option.none());
        }

        @Test
        public void shouldFindIndexOfFirstElement() {
            assertThat(of(1, 2, 3).indexOf(1)).isEqualTo(0);

            assertThat(of(1, 2, 3).indexOfOption(1)).isEqualTo(Option.some(0));
        }

        @Test
        public void shouldFindIndexOfInnerElement() {
            assertThat(of(1, 2, 3).indexOf(2)).isEqualTo(1);

            assertThat(of(1, 2, 3).indexOfOption(2)).isEqualTo(Option.some(1));
        }

        @Test
        public void shouldFindIndexOfLastElement() {
            assertThat(of(1, 2, 3).indexOf(3)).isEqualTo(2);

            assertThat(of(1, 2, 3).indexOfOption(3)).isEqualTo(Option.some(2));
        }
    }

    @Nested
    class IndexofsliceTests {
        @Test
        public void shouldNotFindIndexOfSliceWhenLazyListIsEmpty() {
            assertThat(empty().indexOfSlice(of(2, 3))).isEqualTo(-1);

            assertThat(empty().indexOfSliceOption(of(2, 3))).isEqualTo(Option.none());
        }

        @Test
        public void shouldNotFindEmptySliceWhenStartIsGreaterThanLength() {
            assertThat(of(1, 2, 3).indexOfSlice(empty(), 4)).isEqualTo(-1);
            assertThat(of(1, 2, 3).indexOfSliceOption(empty(), 4)).isEqualTo(Option.none());
        }

        @Test
        public void shouldNotFindIndexOfSliceWhenStartIsGreater() {
            assertThat(of(1, 2, 3, 4).indexOfSlice(of(2, 3), 2)).isEqualTo(-1);

            assertThat(of(1, 2, 3, 4).indexOfSliceOption(of(2, 3), 2)).isEqualTo(Option.none());
        }

        @Test
        public void shouldFindIndexOfFirstSlice() {
            assertThat(of(1, 2, 3, 4).indexOfSlice(of(1, 2))).isEqualTo(0);

            assertThat(of(1, 2, 3, 4).indexOfSliceOption(of(1, 2))).isEqualTo(Option.some(0));
        }

        @Test
        public void shouldFindIndexOfInnerSlice() {
            assertThat(of(1, 2, 3, 4).indexOfSlice(of(2, 3))).isEqualTo(1);

            assertThat(of(1, 2, 3, 4).indexOfSliceOption(of(2, 3))).isEqualTo(Option.some(1));
        }

        @Test
        public void shouldFindIndexOfLastSlice() {
            assertThat(of(1, 2, 3).indexOfSlice(of(2, 3))).isEqualTo(1);

            assertThat(of(1, 2, 3).indexOfSliceOption(of(2, 3))).isEqualTo(Option.some(1));
        }
    }

    @Nested
    class LastindexofTests {
        @Test
        public void shouldNotFindLastIndexOfElementWhenLazyListIsEmpty() {
            assertThat(empty().lastIndexOf(1)).isEqualTo(-1);

            assertThat(empty().lastIndexOfOption(1)).isEqualTo(Option.none());
        }

        @Test
        public void shouldNotFindLastIndexOfElementWhenEndIdLess() {
            assertThat(of(1, 2, 3, 4).lastIndexOf(3, 1)).isEqualTo(-1);

            assertThat(of(1, 2, 3, 4).lastIndexOfOption(3, 1)).isEqualTo(Option.none());
        }

        @Test
        public void shouldFindLastIndexOfElement() {
            assertThat(of(1, 2, 3, 1, 2, 3).lastIndexOf(1)).isEqualTo(3);

            assertThat(of(1, 2, 3, 1, 2, 3).lastIndexOfOption(1)).isEqualTo(Option.some(3));
        }

        @Test
        public void shouldFindLastIndexOfElementWithEnd() {
            assertThat(of(1, 2, 3, 1, 2, 3).lastIndexOf(1, 1)).isEqualTo(0);

            assertThat(of(1, 2, 3, 1, 2, 3).lastIndexOfOption(1, 1)).isEqualTo(Option.some(0));
        }
    }

    @Nested
    class LastindexofsliceTests {
        @Test
        public void shouldNotFindLastIndexOfSliceWhenLazyListIsEmpty() {
            assertThat(empty().lastIndexOfSlice(of(2, 3))).isEqualTo(-1);

            assertThat(empty().lastIndexOfSliceOption(of(2, 3))).isEqualTo(Option.none());
        }

        @Test
        public void shouldNotFindLastIndexOfSliceWhenEndIdLess() {
            assertThat(of(1, 2, 3, 4, 5).lastIndexOfSlice(of(3, 4), 1)).isEqualTo(-1);

            assertThat(of(1, 2, 3, 4, 5).lastIndexOfSliceOption(of(3, 4), 1)).isEqualTo(Option.none());
        }

        @Test
        public void shouldFindLastIndexOfSlice() {
            assertThat(of(1, 2, 3, 1, 2).lastIndexOfSlice(empty())).isEqualTo(5);
            assertThat(of(1, 2, 3, 1, 2).lastIndexOfSlice(of(2))).isEqualTo(4);
            assertThat(of(1, 2, 3, 1, 2, 3, 4).lastIndexOfSlice(of(2, 3))).isEqualTo(4);
            assertThat(of(1, 2, 3, 1, 2, 3).lastIndexOfSlice(of(1, 2, 3))).isEqualTo(3);

            assertThat(of(1, 2, 3, 1, 2).lastIndexOfSliceOption(empty())).isEqualTo(Option.some(5));
            assertThat(of(1, 2, 3, 1, 2).lastIndexOfSliceOption(of(2))).isEqualTo(Option.some(4));
            assertThat(of(1, 2, 3, 1, 2, 3, 4).lastIndexOfSliceOption(of(2, 3))).isEqualTo(Option.some(4));
            assertThat(of(1, 2, 3, 1, 2, 3).lastIndexOfSliceOption(of(1, 2, 3))).isEqualTo(Option.some(3));
        }

        @Test
        public void shouldFindLastIndexOfSliceWithEnd() {
            assertThat(empty().lastIndexOfSlice(empty(), -1)).isEqualTo(-1);
            assertThat(empty().lastIndexOfSlice(empty(), 0)).isEqualTo(0);
            assertThat(of(1, 2, 3, 1, 2, 3).lastIndexOfSlice(empty(), -1)).isEqualTo(-1);
            assertThat(of(1, 2, 3, 1, 2, 3).lastIndexOfSlice(empty(), 2)).isEqualTo(2);
            assertThat(of(1, 2, 3, 1, 2, 3).lastIndexOfSlice(of(2), -1)).isEqualTo(-1);
            assertThat(of(1, 2, 3, 1, 2, 3).lastIndexOfSlice(of(2), 2)).isEqualTo(1);
            assertThat(of(1, 2, 3, 1, 2, 3).lastIndexOfSlice(of(2, 3), 2)).isEqualTo(1);
            assertThat(of(1, 2, 3, 1, 2, 3, 4).lastIndexOfSlice(of(2, 3), 2)).isEqualTo(1);
            assertThat(of(1, 2, 3, 1, 2, 3).lastIndexOfSlice(of(1, 2, 3), 2)).isEqualTo(0);

            assertThat(empty().lastIndexOfSliceOption(empty(), -1)).isEqualTo(Option.none());
            assertThat(empty().lastIndexOfSliceOption(empty(), 0)).isEqualTo(Option.some(0));
            assertThat(of(1, 2, 3, 1, 2, 3).lastIndexOfSliceOption(empty(), -1)).isEqualTo(Option.none());
            assertThat(of(1, 2, 3, 1, 2, 3).lastIndexOfSliceOption(empty(), 2)).isEqualTo(Option.some(2));
            assertThat(of(1, 2, 3, 1, 2, 3).lastIndexOfSliceOption(of(2), -1)).isEqualTo(Option.none());
            assertThat(of(1, 2, 3, 1, 2, 3).lastIndexOfSliceOption(of(2), 2)).isEqualTo(Option.some(1));
            assertThat(of(1, 2, 3, 1, 2, 3).lastIndexOfSliceOption(of(2, 3), 2)).isEqualTo(Option.some(1));
            assertThat(of(1, 2, 3, 1, 2, 3, 4).lastIndexOfSliceOption(of(2, 3), 2))
                    .isEqualTo(Option.some(1));
            assertThat(of(1, 2, 3, 1, 2, 3).lastIndexOfSliceOption(of(1, 2, 3), 2))
                    .isEqualTo(Option.some(0));
        }
    }

    @Nested
    class IndexwhereTests {
        @Test
        public void shouldCalculateIndexWhere() {
            assertThat(of(0, 1, 2, -1, 0, 1, 2).indexWhere(i -> i == 0)).isEqualTo(0);
            assertThat(of(0, 1, 2, -1, 0, 1, 2).indexWhere(i -> i == 1)).isEqualTo(1);
            assertThat(of(0, 1, 2, -1, 0, 1, 2).indexWhere(i -> i == 2)).isEqualTo(2);
            assertThat(of(0, 1, 2, -1, 0, 1, 2).indexWhere(i -> i == 8)).isEqualTo(-1);
            assertThat(of(0, 1, 2, -1, 0, 1, 2).indexWhere(i -> i == 0, 3)).isEqualTo(4);
            assertThat(of(0, 1, 2, -1, 0, 1, 2).indexWhere(i -> i == 1, 3)).isEqualTo(5);
            assertThat(of(0, 1, 2, -1, 0, 1, 2).indexWhere(i -> i == 2, 3)).isEqualTo(6);
            assertThat(of(0, 1, 2, -1, 0, 1, 2).indexWhere(i -> i == 8, 3)).isEqualTo(-1);

            assertThat(of(0, 1, 2, -1, 0, 1, 2).indexWhereOption(i -> i == 0)).isEqualTo(Option.some(0));
            assertThat(of(0, 1, 2, -1, 0, 1, 2).indexWhereOption(i -> i == 1)).isEqualTo(Option.some(1));
            assertThat(of(0, 1, 2, -1, 0, 1, 2).indexWhereOption(i -> i == 2)).isEqualTo(Option.some(2));
            assertThat(of(0, 1, 2, -1, 0, 1, 2).indexWhereOption(i -> i == 8)).isEqualTo(Option.none());
            assertThat(of(0, 1, 2, -1, 0, 1, 2).indexWhereOption(i -> i == 0, 3))
                    .isEqualTo(Option.some(4));
            assertThat(of(0, 1, 2, -1, 0, 1, 2).indexWhereOption(i -> i == 1, 3))
                    .isEqualTo(Option.some(5));
            assertThat(of(0, 1, 2, -1, 0, 1, 2).indexWhereOption(i -> i == 2, 3))
                    .isEqualTo(Option.some(6));
            assertThat(of(0, 1, 2, -1, 0, 1, 2).indexWhereOption(i -> i == 8, 3))
                    .isEqualTo(Option.none());
        }

        @Test
        public void shouldTreatNegativeFromAsZeroInIndexWhere() {
            assertThat(of(1, 2, 3).indexWhere(i -> i == 1, -1)).isEqualTo(0);
            assertThat(of(1, 2, 3).indexWhere(i -> i == 2, -3)).isEqualTo(1);
            assertThat(of(1, 2, 3).indexWhere(i -> i == 8, -1)).isEqualTo(-1);
            assertThat(of(1, 2, 3).indexWhereOption(i -> i == 1, -1)).isEqualTo(Option.some(0));
        }

        @Test
        public void shouldFailIndexWhereNullPredicate() {
            assertThrows(NullPointerException.class, () -> of(1).indexWhere(null));
        }

        @Test
        public void shouldFailIndexWhereNullPredicateFrom() {
            assertThrows(NullPointerException.class, () -> of(1).indexWhere(null, 0));
        }
    }

    @Nested
    class LastindexwhereTests {
        @Test
        public void shouldCalculateLastIndexWhere() {
            assertThat(of(0, 1, 2, -1, 0, 1, 2).lastIndexWhere(i -> i == 0)).isEqualTo(4);
            assertThat(of(0, 1, 2, -1, 0, 1, 2).lastIndexWhere(i -> i == 1)).isEqualTo(5);
            assertThat(of(0, 1, 2, -1, 0, 1, 2).lastIndexWhere(i -> i == 2)).isEqualTo(6);
            assertThat(of(0, 1, 2, -1, 0, 1, 2).lastIndexWhere(i -> i == 8)).isEqualTo(-1);
            assertThat(of(0, 1, 2, -1, 0, 1, 2).lastIndexWhere(i -> i == 0, 3)).isEqualTo(0);
            assertThat(of(0, 1, 2, -1, 0, 1, 2).lastIndexWhere(i -> i == 1, 3)).isEqualTo(1);
            assertThat(of(0, 1, 2, -1, 0, 1, 2).lastIndexWhere(i -> i == 2, 3)).isEqualTo(2);
            assertThat(of(0, 1, 2, -1, 0, 1, 2).lastIndexWhere(i -> i == 8, 3)).isEqualTo(-1);

            assertThat(of(0, 1, 2, -1, 0, 1, 2).lastIndexWhereOption(i -> i == 0))
                    .isEqualTo(Option.some(4));
            assertThat(of(0, 1, 2, -1, 0, 1, 2).lastIndexWhereOption(i -> i == 1))
                    .isEqualTo(Option.some(5));
            assertThat(of(0, 1, 2, -1, 0, 1, 2).lastIndexWhereOption(i -> i == 2))
                    .isEqualTo(Option.some(6));
            assertThat(of(0, 1, 2, -1, 0, 1, 2).lastIndexWhereOption(i -> i == 8))
                    .isEqualTo(Option.none());
            assertThat(of(0, 1, 2, -1, 0, 1, 2).lastIndexWhereOption(i -> i == 0, 3))
                    .isEqualTo(Option.some(0));
            assertThat(of(0, 1, 2, -1, 0, 1, 2).lastIndexWhereOption(i -> i == 1, 3))
                    .isEqualTo(Option.some(1));
            assertThat(of(0, 1, 2, -1, 0, 1, 2).lastIndexWhereOption(i -> i == 2, 3))
                    .isEqualTo(Option.some(2));
            assertThat(of(0, 1, 2, -1, 0, 1, 2).lastIndexWhereOption(i -> i == 8, 3))
                    .isEqualTo(Option.none());
        }

        @Test
        public void shouldReturnMinusOneForNegativeEndInLastIndexWhere() {
            assertThat(of(1, 2, 3).lastIndexWhere(i -> true, -1)).isEqualTo(-1);
            assertThat(of(1, 2, 3).lastIndexWhere(i -> true, -5)).isEqualTo(-1);
            assertThat(of(1, 2, 3).lastIndexWhereOption(i -> true, -5)).isEqualTo(Option.none());
        }

        @Test
        public void shouldFailLastIndexWhereNullPredicate() {
            assertThrows(NullPointerException.class, () -> of(1).lastIndexWhere(null));
        }

        @Test
        public void shouldFailLastIndexWhereNullPredicateFrom() {
            assertThrows(NullPointerException.class, () -> of(1).lastIndexWhere(null, 0));
        }
    }

    @Nested
    class EndswithTests {
        @Test
        public void shouldTestEndsWith() {
            assertThat(empty().endsWith(empty())).isTrue();
            assertThat(empty().endsWith(of(1))).isFalse();
            assertThat(of(1, 2, 3, 4).endsWith(empty())).isTrue();
            assertThat(of(1, 2, 3, 4).endsWith(of(4))).isTrue();
            assertThat(of(1, 2, 3, 4).endsWith(of(3, 4))).isTrue();
            assertThat(of(1, 2, 3, 4).endsWith(of(1, 2, 3, 4))).isTrue();
            assertThat(of(1, 2, 3, 4).endsWith(of(0, 1, 2, 3, 4))).isFalse();
            assertThat(of(1, 2, 3, 4).endsWith(of(2, 3, 5))).isFalse();
        }
    }

    @Nested
    class EqualityTests {
        @Test
        public void shouldObeyEqualityConstraints() {

            // sequential collections
            assertThat(empty().equals(List.empty())).isTrue();
            assertThat(of(1).equals(List.of(1))).isTrue();
            assertThat(of(1, 2, 3).equals(List.of(1, 2, 3))).isTrue();
            assertThat(of(1, 2, 3).equals(List.of(3, 2, 1))).isFalse();

            // other classes
            assertThat(empty().equals(HashMap.empty())).isFalse();
            assertThat(empty().equals(HashSet.empty())).isFalse();

            assertThat(empty().equals(LinkedHashMap.empty())).isFalse();
            assertThat(empty().equals(LinkedHashSet.empty())).isFalse();

            assertThat(empty().equals(TreeMap.empty())).isFalse();
            assertThat(empty().equals(TreeSet.empty())).isFalse();
        }
    }

    // -- insert

    @Test
    public void shouldInsertIntoNil() {
        LazyList<Integer> actual = this.<Integer>empty().insert(0, 1);
        LazyList<Integer> expected = of(1);
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void shouldInsertInFrontOfElement() {
        LazyList<Integer> actual = of(4).insert(0, 1);
        LazyList<Integer> expected = of(1, 4);
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void shouldInsertBehindOfElement() {
        LazyList<Integer> actual = of(4).insert(1, 5);
        LazyList<Integer> expected = of(4, 5);
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void shouldInsertIntoLazyList() {
        LazyList<Integer> actual = of(1, 2, 3).insert(2, 4);
        LazyList<Integer> expected = of(1, 2, 4, 3);
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void shouldThrowWhenInsertOnNonNilWithNegativeIndex() {
        assertThrows(IndexOutOfBoundsException.class, () -> of(1).insert(-1, 9));
    }

    @Test
    public void shouldThrowWhenInsertOnNilWithNegativeIndex() {
        assertThrows(
                IndexOutOfBoundsException.class, () -> this.<Integer>empty().insert(-1, 9));
    }

    @Test
    public void shouldThrowOnInsertWhenExceedingUpperBound() {
        assertThrows(
                IndexOutOfBoundsException.class, () -> this.<Integer>empty().insert(1, 9));
    }

    // -- insertAll

    @Test
    public void shouldInsertAllIntoNil() {
        LazyList<Integer> actual = this.<Integer>empty().insertAll(0, of(1, 2, 3));
        LazyList<Integer> expected = of(1, 2, 3);
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void shouldInsertAllInFrontOfElement() {
        LazyList<Integer> actual = of(4).insertAll(0, of(1, 2, 3));
        LazyList<Integer> expected = of(1, 2, 3, 4);
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void shouldInsertAllBehindOfElement() {
        LazyList<Integer> actual = of(4).insertAll(1, of(1, 2, 3));
        LazyList<Integer> expected = of(4, 1, 2, 3);
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void shouldInsertAllIntoLazyList() {
        LazyList<Integer> actual = of(1, 2, 3).insertAll(2, of(4, 5));
        LazyList<Integer> expected = of(1, 2, 4, 5, 3);
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void shouldThrowOnInsertAllWithNil() {
        assertThrows(NullPointerException.class, () -> empty().insertAll(0, null));
    }

    @Test
    public void shouldThrowWhenInsertOnNonNilAllWithNegativeIndex() {
        assertThrows(IndexOutOfBoundsException.class, () -> of(1).insertAll(-1, empty()));
    }

    @Test
    public void shouldThrowWhenInsertOnNilAllWithNegativeIndex() {
        assertThrows(IndexOutOfBoundsException.class, () -> empty().insertAll(-1, empty()));
    }

    @Test
    public void shouldThrowOnInsertAllWhenExceedingUpperBound() {
        assertThrows(IndexOutOfBoundsException.class, () -> empty().insertAll(1, empty()));
    }

    @Test
    public void shouldReturnSameLazyListWhenEmptyInsertAllEmpty() {
        LazyList<Integer> empty = empty();
        assertThat(empty.insertAll(0, empty())).isSameAs(empty);
    }

    @Test
    public void shouldReturnSameLazyListWhenEmptyInsertAllNonEmpty() {
        LazyList<Integer> seq = of(1, 2, 3);
        assertThat(empty().insertAll(0, seq)).isSameAs(seq);
    }

    @Test
    public void shouldReturnSameLazyListWhenNonEmptyInsertAllEmpty() {
        LazyList<Integer> seq = of(1, 2, 3);
        assertThat(seq.insertAll(0, empty())).isSameAs(seq);
    }

    // -- intersperse

    @Test
    public void shouldIntersperseNil() {
        assertThat(this.<Character>empty().intersperse(',')).isEmpty();
    }

    @Test
    public void shouldIntersperseSingleton() {
        assertThat(of('a').intersperse(',')).isEqualTo(of('a'));
    }

    @Test
    public void shouldIntersperseMultipleElements() {
        assertThat(of('a', 'b').intersperse(',')).isEqualTo(of('a', ',', 'b'));
    }

    @Nested
    class PadtoTests {
        @Test
        public void shouldPadEmptyToEmpty() {
            assertThat(empty().padTo(0, 1)).isSameAs(empty());
        }

        @Test
        public void shouldPadEmptyToNonEmpty() {
            assertThat(empty().padTo(2, 1)).isEqualTo(of(1, 1));
        }

        @Test
        public void shouldPadNonEmptyZeroLen() {
            LazyList<Integer> seq = of(1);
            assertThat(seq.padTo(0, 2)).isSameAs(seq);
        }

        @Test
        public void shouldPadNonEmpty() {
            assertThat(of(1).padTo(2, 1)).isEqualTo(of(1, 1));
            assertThat(of(1).padTo(2, 2)).isEqualTo(of(1, 2));
            assertThat(of(1).padTo(3, 2)).isEqualTo(of(1, 2, 2));
        }
    }

    @Nested
    class LeftpadtoTests {
        @Test
        public void shouldLeftPadEmptyToEmpty() {
            assertThat(empty().leftPadTo(0, 1)).isSameAs(empty());
        }

        @Test
        public void shouldLeftPadEmptyToNonEmpty() {
            assertThat(empty().leftPadTo(2, 1)).isEqualTo(of(1, 1));
        }

        @Test
        public void shouldLeftPadNonEmptyZeroLen() {
            LazyList<Integer> seq = of(1);
            assertThat(seq.leftPadTo(0, 2)).isSameAs(seq);
        }

        @Test
        public void shouldLeftPadNonEmpty() {
            assertThat(of(1).leftPadTo(2, 1)).isEqualTo(of(1, 1));
            assertThat(of(1).leftPadTo(2, 2)).isEqualTo(of(2, 1));
            assertThat(of(1).leftPadTo(3, 2)).isEqualTo(of(2, 2, 1));
        }
    }

    @Nested
    class PatchTests {
        @Test
        public void shouldPatchEmptyByEmpty() {
            assertThat(empty().patch(0, empty(), 0)).isEmpty();
            assertThat(empty().patch(-1, empty(), -1)).isEmpty();
            assertThat(empty().patch(-1, empty(), 1)).isEmpty();
            assertThat(empty().patch(1, empty(), -1)).isEmpty();
            assertThat(empty().patch(1, empty(), 1)).isEmpty();
        }

        @Test
        public void shouldPatchEmptyByNonEmpty() {
            LazyList<Character> s = of('1', '2', '3');
            assertThat(empty().patch(0, s, 0)).isEqualTo(s);
            assertThat(empty().patch(-1, s, -1)).isEqualTo(s);
            assertThat(empty().patch(-1, s, 1)).isEqualTo(s);
            assertThat(empty().patch(1, s, -1)).isEqualTo(s);
            assertThat(empty().patch(1, s, 1)).isEqualTo(s);
        }

        @Test
        public void shouldPatchNonEmptyByEmpty() {
            LazyList<Character> s = of('1', '2', '3');
            assertThat(s.patch(-1, empty(), -1)).isEqualTo(of('1', '2', '3'));
            assertThat(s.patch(-1, empty(), 0)).isEqualTo(of('1', '2', '3'));
            assertThat(s.patch(-1, empty(), 1)).isEqualTo(of('2', '3'));
            assertThat(s.patch(-1, empty(), 3)).isEmpty();
            assertThat(s.patch(0, empty(), -1)).isEqualTo(of('1', '2', '3'));
            assertThat(s.patch(0, empty(), 0)).isEqualTo(of('1', '2', '3'));
            assertThat(s.patch(0, empty(), 1)).isEqualTo(of('2', '3'));
            assertThat(s.patch(0, empty(), 3)).isEmpty();
            assertThat(s.patch(1, empty(), -1)).isEqualTo(of('1', '2', '3'));
            assertThat(s.patch(1, empty(), 0)).isEqualTo(of('1', '2', '3'));
            assertThat(s.patch(1, empty(), 1)).isEqualTo(of('1', '3'));
            assertThat(s.patch(1, empty(), 3)).isEqualTo(of('1'));
            assertThat(s.patch(4, empty(), -1)).isEqualTo(of('1', '2', '3'));
            assertThat(s.patch(4, empty(), 0)).isEqualTo(of('1', '2', '3'));
            assertThat(s.patch(4, empty(), 1)).isEqualTo(of('1', '2', '3'));
            assertThat(s.patch(4, empty(), 3)).isEqualTo(of('1', '2', '3'));
        }

        @Test
        public void shouldPatchNonEmptyByNonEmpty() {
            LazyList<Character> s = of('1', '2', '3');
            LazyList<Character> d = of('4', '5', '6');
            assertThat(s.patch(-1, d, -1)).isEqualTo(of('4', '5', '6', '1', '2', '3'));
            assertThat(s.patch(-1, d, 0)).isEqualTo(of('4', '5', '6', '1', '2', '3'));
            assertThat(s.patch(-1, d, 1)).isEqualTo(of('4', '5', '6', '2', '3'));
            assertThat(s.patch(-1, d, 3)).isEqualTo(of('4', '5', '6'));
            assertThat(s.patch(0, d, -1)).isEqualTo(of('4', '5', '6', '1', '2', '3'));
            assertThat(s.patch(0, d, 0)).isEqualTo(of('4', '5', '6', '1', '2', '3'));
            assertThat(s.patch(0, d, 1)).isEqualTo(of('4', '5', '6', '2', '3'));
            assertThat(s.patch(0, d, 3)).isEqualTo(of('4', '5', '6'));
            assertThat(s.patch(1, d, -1)).isEqualTo(of('1', '4', '5', '6', '2', '3'));
            assertThat(s.patch(1, d, 0)).isEqualTo(of('1', '4', '5', '6', '2', '3'));
            assertThat(s.patch(1, d, 1)).isEqualTo(of('1', '4', '5', '6', '3'));
            assertThat(s.patch(1, d, 3)).isEqualTo(of('1', '4', '5', '6'));
            assertThat(s.patch(4, d, -1)).isEqualTo(of('1', '2', '3', '4', '5', '6'));
            assertThat(s.patch(4, d, 0)).isEqualTo(of('1', '2', '3', '4', '5', '6'));
            assertThat(s.patch(4, d, 1)).isEqualTo(of('1', '2', '3', '4', '5', '6'));
            assertThat(s.patch(4, d, 3)).isEqualTo(of('1', '2', '3', '4', '5', '6'));
        }
    }

    @Nested
    class PermutationsTests {
        @Test
        public void shouldComputePermutationsOfEmptyLazyList() {
            assertThat(empty().permutations()).isEmpty();
        }

        @Test
        public void shouldComputePermutationsOfSingleton() {
            assertThat(of(1).permutations()).isEqualTo(of(of(1)));
        }

        @SuppressWarnings("unchecked")
        @Test
        public void shouldComputePermutationsOfRepeatedElements() {
            assertThat(of(1, 1).permutations()).isEqualTo(of(of(1, 1)));
            assertThat(of(1, 2, 2).permutations()).isEqualTo(of(of(1, 2, 2), of(2, 1, 2), of(2, 2, 1)));
        }

        @SuppressWarnings("unchecked")
        @Test
        public void shouldComputePermutationsOfNonEmptyLazyList() {
            assertThat(of(1, 2, 3).permutations())
                    .isEqualTo(of(of(1, 2, 3), of(1, 3, 2), of(2, 1, 3), of(2, 3, 1), of(3, 1, 2), of(3, 2, 1)));
        }
    }

    // -- map

    @Test
    public void shouldMapTransformedLazyList() {
        Function<Integer, Integer> mapper = o -> o + 1;
        assertThat(this.<Integer>empty().map(mapper)).isEmpty();
        assertThat(of(3, 1, 4, 1, 5).map(mapper)).isEqualTo(of(4, 2, 5, 2, 6));
        assertThat(of(3, 1, 4, 1, 5, 9, 2)
                        .sorted()
                        .distinct()
                        .drop(1)
                        .init()
                        .remove(5)
                        .map(mapper)
                        .tail())
                .isEqualTo(of(4, 5));
    }

    @Nested
    class PrefixlengthTests {
        @Test
        public void shouldCalculatePrefixLength() {
            assertThat(of(1, 3, 5, 6).prefixLength(i -> (i & 1) > 0)).isEqualTo(3);
            assertThat(of(1, 3, 5).prefixLength(i -> (i & 1) > 0)).isEqualTo(3);
            assertThat(of(2).prefixLength(i -> (i & 1) > 0)).isEqualTo(0);
            assertThat(empty().prefixLength(i -> true)).isEqualTo(0);
        }

        @Test
        public void shouldThrowPrefixLengthNullPredicate() {
            assertThrows(NullPointerException.class, () -> of(1).prefixLength(null));
        }
    }

    @Nested
    class SegmentlengthTests {
        @Test
        public void shouldCalculateSegmentLength() {
            assertThat(of(1, 3, 5, 6).segmentLength(i -> (i & 1) > 0, 1)).isEqualTo(2);
            assertThat(of(1, 3, 5).segmentLength(i -> (i & 1) > 0, 1)).isEqualTo(2);
            assertThat(of(2, 2).segmentLength(i -> (i & 1) > 0, 1)).isEqualTo(0);
            assertThat(of(2).segmentLength(i -> (i & 1) > 0, 1)).isEqualTo(0);
            assertThat(empty().segmentLength(i -> true, 1)).isEqualTo(0);
        }

        @Test
        public void shouldTreatNegativeFromAsZeroInSegmentLength() {
            assertThat(of(1, 3, 5, 6).segmentLength(i -> (i & 1) > 0, -1)).isEqualTo(3);
            assertThat(of(2, 3).segmentLength(i -> (i & 1) > 0, -2)).isEqualTo(0);
            assertThat(empty().segmentLength(i -> true, -1)).isEqualTo(0);
        }

        @Test
        public void shouldThrowSegmentLengthNullPredicate() {
            assertThrows(NullPointerException.class, () -> of(1).segmentLength(null, 0));
        }
    }

    // -- prepend

    @Test
    public void shouldPrependElementToNil() {
        LazyList<Integer> actual = this.<Integer>empty().prepend(1);
        LazyList<Integer> expected = of(1);
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void shouldPrependElementToNonNil() {
        LazyList<Integer> actual = of(2, 3).prepend(1);
        LazyList<Integer> expected = of(1, 2, 3);
        assertThat(actual).isEqualTo(expected);
    }

    // -- prependAll

    @Test
    public void shouldThrowOnPrependAllOfNull() {
        assertThrows(NullPointerException.class, () -> empty().prependAll(null));
    }

    @Test
    public void shouldPrependAllNilToNil() {
        LazyList<Integer> actual = this.<Integer>empty().prependAll(empty());
        LazyList<Integer> expected = empty();
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void shouldPrependAllNilToNonNil() {
        LazyList<Integer> actual = of(1, 2, 3).prependAll(empty());
        LazyList<Integer> expected = of(1, 2, 3);
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void shouldPrependAllNonNilToNil() {
        LazyList<Integer> actual = this.<Integer>empty().prependAll(of(1, 2, 3));
        LazyList<Integer> expected = of(1, 2, 3);
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void shouldPrependAllNonNilToNonNil() {
        LazyList<Integer> expected = range(0, 100);

        LazyList<Integer> actualFirstPartLarger = range(90, 100).prependAll(range(0, 90));
        assertThat(actualFirstPartLarger).isEqualTo(expected);

        LazyList<Integer> actualSecondPartLarger = range(10, 100).prependAll(range(0, 10));
        assertThat(actualSecondPartLarger).isEqualTo(expected);
    }

    @Test
    public void shouldReturnSameLazyListWhenEmptyPrependAllEmpty() {
        LazyList<Integer> empty = empty();
        assertThat(empty.prependAll(empty())).isSameAs(empty);
    }

    @Test
    public void shouldReturnSameLazyListWhenEmptyPrependAllNonEmpty() {
        LazyList<Integer> seq = of(1, 2, 3);
        assertThat(empty().prependAll(seq)).isSameAs(seq);
    }

    @Test
    public void shouldReturnSameLazyListWhenNonEmptyPrependAllEmpty() {
        LazyList<Integer> seq = of(1, 2, 3);
        assertThat(seq.prependAll(empty())).isSameAs(seq);
    }

    // -- remove

    @Test
    public void shouldRemoveElementFromNil() {
        assertThat(empty().remove(null)).isEmpty();
    }

    @Test
    public void shouldRemoveFirstElement() {
        assertThat(of(1, 2, 3).remove(1)).isEqualTo(of(2, 3));
    }

    @Test
    public void shouldRemoveLastElement() {
        assertThat(of(1, 2, 3).remove(3)).isEqualTo(of(1, 2));
    }

    @Test
    public void shouldRemoveInnerElement() {
        assertThat(of(1, 2, 3).remove(2)).isEqualTo(of(1, 3));
    }

    @Test
    public void shouldNotRemoveDuplicateElement() {
        assertThat(of(1, 2, 3, 1, 2).remove(1).remove(3)).isEqualTo(of(2, 1, 2));
    }

    // -- removeFirst(Predicate)

    @Test
    public void shouldRemoveFirstElementByPredicateFromNil() {
        assertThat(empty().removeFirst(v -> true)).isEmpty();
    }

    @Test
    public void shouldRemoveFirstElementByPredicateBegin() {
        assertThat(of(1, 2, 3).removeFirst(v -> v == 1)).isEqualTo(of(2, 3));
    }

    @Test
    public void shouldRemoveFirstElementByPredicateBeginM() {
        assertThat(of(1, 2, 1, 3).removeFirst(v -> v == 1)).isEqualTo(of(2, 1, 3));
    }

    @Test
    public void shouldRemoveFirstElementByPredicateEnd() {
        assertThat(of(1, 2, 3).removeFirst(v -> v == 3)).isEqualTo(of(1, 2));
    }

    @Test
    public void shouldRemoveFirstElementByPredicateInner() {
        assertThat(of(1, 2, 3, 4, 5).removeFirst(v -> v == 3)).isEqualTo(of(1, 2, 4, 5));
    }

    @Test
    public void shouldRemoveFirstElementByPredicateInnerM() {
        assertThat(of(1, 2, 3, 2, 5).removeFirst(v -> v == 2)).isEqualTo(of(1, 3, 2, 5));
    }

    // -- removeLast(Predicate)

    @Test
    public void shouldRemoveLastElementByPredicateFromNil() {
        assertThat(empty().removeLast(v -> true)).isEmpty();
    }

    @Test
    public void shouldRemoveLastElementByPredicateBegin() {
        assertThat(of(1, 2, 3).removeLast(v -> v == 1)).isEqualTo(of(2, 3));
    }

    @Test
    public void shouldRemoveLastElementByPredicateEnd() {
        assertThat(of(1, 2, 3).removeLast(v -> v == 3)).isEqualTo(of(1, 2));
    }

    @Test
    public void shouldRemoveLastElementByPredicateEndM() {
        assertThat(of(1, 3, 2, 3).removeLast(v -> v == 3)).isEqualTo(of(1, 3, 2));
    }

    @Test
    public void shouldRemoveLastElementByPredicateInner() {
        assertThat(of(1, 2, 3, 4, 5).removeLast(v -> v == 3)).isEqualTo(of(1, 2, 4, 5));
    }

    @Test
    public void shouldRemoveLastElementByPredicateInnerM() {
        assertThat(of(1, 2, 3, 2, 5).removeLast(v -> v == 2)).isEqualTo(of(1, 2, 3, 5));
    }

    // -- removeAll(Iterable)

    @Test
    public void shouldRemoveAllElementsFromNil() {
        assertThat(empty().removeAll(of(1, 2, 3))).isEmpty();
    }

    @Test
    public void shouldRemoveAllExistingElementsFromNonNil() {
        assertThat(of(1, 2, 3, 1, 2, 3).removeAll(of(1, 2))).isEqualTo(of(3, 3));
    }

    @Test
    public void shouldReturnSameLazyListWhenNonEmptyRemoveAllEmpty() {
        LazyList<Integer> seq = of(1, 2, 3);
        assertThat(seq.removeAll(empty())).isSameAs(seq);
    }

    @Test
    public void shouldReturnSameLazyListWhenEmptyRemoveAllNonEmpty() {
        LazyList<Integer> empty = empty();
        assertThat(empty.removeAll(of(1, 2, 3))).isSameAs(empty);
    }

    // -- removeAll(Predicate)

    @SuppressWarnings("deprecation")
    @Test
    public void shouldRemoveNonExistingElements() {
        assertThat(this.<Integer>empty().removeAll(i -> i == 0)).isSameAs(empty());
        assertThat(of(1, 2, 3).removeAll(i -> i != 0)).isEmpty();
    }

    @SuppressWarnings("deprecation")
    @Test
    public void shouldRemoveAllElementsByPredicateFromNil() {
        assertThat(empty().removeAll(o -> true)).isEmpty();
    }

    @SuppressWarnings("deprecation")
    @Test
    public void shouldRemoveAllExistingElements() {
        assertThat(of(1, 2, 3, 4, 5, 6).removeAll(ignored -> true)).isEmpty();
    }

    @SuppressWarnings("deprecation")
    @Test
    public void shouldRemoveAllMatchedElementsFromNonNil() {
        assertThat(of(1, 2, 3, 4, 5, 6).removeAll(i -> i % 2 == 0)).isEqualTo(of(1, 3, 5));
    }

    // -- removeAll(Object)

    @Test
    public void shouldRemoveAllObjectsFromNil() {
        assertThat(empty().removeAll(1)).isEmpty();
    }

    @Test
    public void shouldRemoveAllExistingObjectsFromNonNil() {
        assertThat(of(1, 2, 3, 1, 2, 3).removeAll(1)).isEqualTo(of(2, 3, 2, 3));
    }

    @Test
    public void shouldNotRemoveAbsentNullFromNonEmpty() {
        LazyList<Integer> seq = of(1, 2, 3);
        assertThat(seq.removeAll((Integer) null)).isEqualTo(seq);
    }

    @Nested
    class RemoveatIndexTests {
        @Test
        public void shouldRemoveIndexAtNil() {
            assertThrows(
                    IndexOutOfBoundsException.class,
                    () -> assertThat(empty().removeAt(1)).isEmpty());
        }

        @Test
        public void shouldRemoveIndexAtNonNil() {
            assertThat(of(1, 2, 3).removeAt(1)).isEqualTo(of(1, 3));
        }

        @Test
        public void shouldRemoveIndexAtBegin() {
            assertThat(of(1, 2, 3).removeAt(0)).isEqualTo(of(2, 3));
        }

        @Test
        public void shouldRemoveIndexAtEnd() {
            assertThat(of(1, 2, 3).removeAt(2)).isEqualTo(of(1, 2));
        }

        @Test
        public void shouldRemoveMultipleTimes() {
            assertThat(of(3, 1, 4, 1, 5, 9, 2)
                            .removeAt(0)
                            .removeAt(0)
                            .removeAt(4)
                            .removeAt(3)
                            .removeAt(1))
                    .isEqualTo(of(4, 5));
        }

        @Test
        public void shouldRemoveIndexOutOfBoundsLeft() {
            assertThrows(
                    IndexOutOfBoundsException.class,
                    () -> assertThat(of(1, 2, 3).removeAt(-1)).isEqualTo(of(1, 2, 3)));
        }

        @Test
        public void shouldRemoveIndexOutOfBoundsRight() {
            assertThrows(
                    IndexOutOfBoundsException.class,
                    () -> assertThat(of(1, 2, 3).removeAt(5)).isEqualTo(of(1, 2, 3)));
        }

        @Test
        public void shouldRemoveIndexEqualToLength() {
            assertThrows(
                    IndexOutOfBoundsException.class,
                    () -> assertThat(of(1, 2, 3).removeAt(3)).isEqualTo(of(1, 2, 3)));
        }
    }

    @Nested
    class ReverseTests {
        @Test
        public void shouldReverseNil() {
            assertThat(empty().reverse()).isEmpty();
        }

        @Test
        public void shouldReverseNonNil() {
            assertThat(of(1, 2, 3).reverse()).isEqualTo(of(3, 2, 1));
        }
    }

    // -- reverse().iterator()

    @Test
    public void shouldIterateEmptyInReverse() {
        assertThat(ofAll(empty()).reverse().iterator().hasNext()).isFalse();
    }

    @Test
    public void shouldIterateSingleInReverse() {
        java.util.Iterator<String> iterator = ofAll(this.of("a")).reverse().iterator();
        assertThat(List.ofAll(() -> iterator)).isEqualTo(List.of("a"));
    }

    @Test
    public void shouldIterateNonEmptyInReverse() {
        java.util.Iterator<String> iterator = ofAll(of("a", "b", "c")).reverse().iterator();
        assertThat(List.ofAll(() -> iterator)).isEqualTo(List.of("c", "b", "a"));
    }

    @Nested
    class TodoRotateleftTests {
        @Test
        public void shouldRotateLeftOnEmpty() {
            assertThat(empty().rotateLeft(1)).isSameAs(empty());
        }

        @Test
        public void shouldRotateLeftOnSingle() {
            LazyList<Integer> seq = of(1);
            assertThat(seq.rotateLeft(1)).isEqualTo(seq);
        }

        @Test
        public void shouldRotateLeftForZero() {
            LazyList<Integer> seq = of(1, 2, 3, 4, 5);
            assertThat(seq.rotateLeft(0)).isSameAs(seq);
        }

        @Test
        public void shouldRotateLeftByZeroOnAnInfiniteLazyList() {
            // == on purpose: a failure must not make AssertJ format an infinite LazyList
            LazyList<Integer> naturals = LazyList.from(1);
            assertThat(naturals.rotateLeft(0) == naturals)
                    .as("rotateLeft(0) returns the receiver")
                    .isTrue();
            assertThat(naturals.rotateLeft(0).take(3)).isEqualTo(LazyList.of(1, 2, 3));
        }

        @Test
        public void shouldNotForceTheLazyListToRotateLeftByZero() {
            AtomicInteger forced = new AtomicInteger();
            LazyList<Integer> counted = LazyList.from(1).map(i -> {
                forced.incrementAndGet();
                return i;
            });
            int before = forced.get();
            assertThat(counted.rotateLeft(0)).isSameAs(counted);
            assertThat(forced.get()).isEqualTo(before);
        }

        @Test
        public void shouldRotateLeftForNegativeLessThatLen() {
            assertThat(of(1, 2, 3, 4, 5).rotateLeft(-2)).isEqualTo(of(4, 5, 1, 2, 3));
        }

        @Test
        public void shouldRotateLeftForPositiveLessThatLen() {
            assertThat(of(1, 2, 3, 4, 5).rotateLeft(2)).isEqualTo(of(3, 4, 5, 1, 2));
        }

        @Test
        public void shouldRotateLeftForPositiveGreaterThatLen() {
            assertThat(of(1, 2, 3, 4, 5).rotateLeft(5 + 2)).isEqualTo(of(3, 4, 5, 1, 2));
        }

        @Test
        public void shouldRotateLeftForPositiveModuloLen() {
            LazyList<Integer> seq = of(1, 2, 3, 4, 5);
            assertThat(seq.rotateLeft(seq.size() * 3)).isEqualTo(seq);
        }
    }

    @Nested
    class RotaterightTests {
        @Test
        public void shouldRotateByTheMostNegativeDistance() {
            // Integer.MIN_VALUE has no positive negation: the distance is taken modulo the length, not negated
            assertThat(of(1, 2, 3, 4).rotateLeft(Integer.MIN_VALUE))
                    .isEqualTo(of(1, 2, 3, 4).rotateLeft(Math.floorMod(Integer.MIN_VALUE, 4)));
            assertThat(of(1, 2, 3, 4).rotateRight(Integer.MIN_VALUE))
                    .isEqualTo(of(1, 2, 3, 4).rotateRight(Math.floorMod(Integer.MIN_VALUE, 4)));
            assertThat(of(1, 2, 3).rotateLeft(Integer.MIN_VALUE)).isEqualTo(of(2, 3, 1));
            assertThat(of(1, 2, 3).rotateRight(Integer.MIN_VALUE)).isEqualTo(of(3, 1, 2));
        }

        @Test
        public void shouldRotateRightOnEmpty() {
            assertThat(empty().rotateRight(1)).isSameAs(empty());
        }

        @Test
        public void shouldRotateRightOnSingle() {
            LazyList<Integer> seq = of(1);
            assertThat(seq.rotateRight(1)).isEqualTo(seq);
        }

        @Test
        public void shouldRotateRightForZero() {
            LazyList<Integer> seq = of(1, 2, 3, 4, 5);
            assertThat(seq.rotateRight(0)).isSameAs(seq);
        }

        @Test
        public void shouldRotateRightByZeroOnAnInfiniteLazyList() {
            // == on purpose: a failure must not make AssertJ format an infinite LazyList
            LazyList<Integer> naturals = LazyList.from(1);
            assertThat(naturals.rotateRight(0) == naturals)
                    .as("rotateRight(0) returns the receiver")
                    .isTrue();
            assertThat(naturals.rotateRight(0).take(3)).isEqualTo(LazyList.of(1, 2, 3));
        }

        @Test
        public void shouldNotForceTheLazyListToRotateRightByZero() {
            AtomicInteger forced = new AtomicInteger();
            LazyList<Integer> counted = LazyList.from(1).map(i -> {
                forced.incrementAndGet();
                return i;
            });
            int before = forced.get();
            assertThat(counted.rotateRight(0)).isSameAs(counted);
            assertThat(forced.get()).isEqualTo(before);
        }

        @Test
        public void shouldRotateRightForNegativeLessThatLen() {
            assertThat(of(1, 2, 3, 4, 5).rotateRight(-2)).isEqualTo(of(3, 4, 5, 1, 2));
        }

        @Test
        public void shouldRotateRightForPositiveLessThatLen() {
            assertThat(of(1, 2, 3, 4, 5).rotateRight(2)).isEqualTo(of(4, 5, 1, 2, 3));
        }

        @Test
        public void shouldRotateRightForPositiveGreaterThatLen() {
            assertThat(of(1, 2, 3, 4, 5).rotateRight(5 + 2)).isEqualTo(of(4, 5, 1, 2, 3));
        }

        @Test
        public void shouldRotateRightForPositiveModuloLen() {
            LazyList<Integer> seq = of(1, 2, 3, 4, 5);
            assertThat(seq.rotateRight(seq.size() * 3)).isEqualTo(seq);
        }
    }

    @Nested
    class ShuffleTests {
        @Test
        public void shouldShuffleEmpty() {
            assertThat(empty().shuffle().isEmpty());
        }

        @Test
        public void shouldShuffleHaveSameLength() {
            assertThat(of(1, 2, 3).shuffle().size()).isEqualTo(of(1, 2, 3).size());
        }

        @Test
        public void shouldShuffleHaveSameElements() {
            LazyList<Integer> shuffled = of(1, 2, 3).shuffle();
            assertThat(shuffled.indexOf(1)).isNotEqualTo(-1);
            assertThat(shuffled.indexOf(2)).isNotEqualTo(-1);
            assertThat(shuffled.indexOf(3)).isNotEqualTo(-1);
            assertThat(shuffled.indexOf(4)).isEqualTo(-1);
        }
    }

    @Nested
    class TakerightuntilTests {
        @Test
        public void shouldTakeRightUntilNoneOnNil() {
            assertThat(empty().takeRightUntil(x -> true)).isEqualTo(empty());
        }

        @Test
        public void shouldTakeRightUntilAllOnFalseCondition() {
            assertThat(of(1, 2, 3).takeRightUntil(x -> false)).isEqualTo(of(1, 2, 3));
        }

        @Test
        public void shouldTakeRightUntilAllOnTrueCondition() {
            assertThat(of(1, 2, 3).takeRightUntil(x -> true)).isEqualTo(empty());
        }

        @Test
        public void shouldTakeRightUntilAsExpected() {
            assertThat(of(2, 3, 4, 6).takeRightUntil(x -> x % 2 != 0)).isEqualTo(of(4, 6));
        }
    }

    @Nested
    class TakerightwhileTests {
        @Test
        public void shouldTakeRightWhileNoneOnNil() {
            assertThat(empty().takeRightWhile(x -> true)).isEqualTo(empty());
        }

        @Test
        public void shouldTakeRightWhileAllOnFalseCondition() {
            assertThat(of(1, 2, 3).takeRightWhile(x -> false)).isEqualTo(empty());
        }

        @Test
        public void shouldTakeRightWhileAllOnTrueCondition() {
            assertThat(of(1, 2, 3).takeRightWhile(x -> true)).isEqualTo(of(1, 2, 3));
        }

        @Test
        public void shouldTakeRightWhileAsExpected() {
            assertThat(of(2, 3, 4, 6).takeRightWhile(x -> x % 2 == 0)).isEqualTo(of(4, 6));
        }
    }

    @Nested
    class UpdateTests {
        @Test
        public void shouldThrowWhenUpdatedWithNegativeIndexOnNil() {
            assertThrows(IndexOutOfBoundsException.class, () -> empty().update(-1, (Integer) null));
        }

        @Test
        public void shouldThrowWhenUpdatedWithNegativeIndexOnNonNil() {
            assertThrows(IndexOutOfBoundsException.class, () -> of(1).update(-1, 2));
        }

        @Test
        public void shouldThrowWhenUpdatedOnNil() {
            assertThrows(IndexOutOfBoundsException.class, () -> empty().update(0, (Integer) null));
        }

        @Test
        public void shouldThrowWhenUpdatedWithIndexExceedingByOneOnNonNil() {
            assertThrows(
                    IndexOutOfBoundsException.class, () -> of(1).update(1, 2).toList());
        }

        @Test
        public void shouldThrowWhenUpdatedWithIndexExceedingByTwoOnNonNil() {
            assertThrows(
                    IndexOutOfBoundsException.class, () -> of(1).update(2, 2).toList());
        }

        @Test
        public void shouldUpdateFirstElement() {
            assertThat(of(1, 2, 3).update(0, 4)).isEqualTo(of(4, 2, 3));
        }

        @Test
        public void shouldUpdateLastElement() {
            assertThat(of(1, 2, 3).update(2, 4)).isEqualTo(of(1, 2, 4));
        }
    }

    @Nested
    class HigherOrderUpdateTests {
        @Test
        public void shouldUpdateViaFunction() throws Exception {
            LazyList<Character> actual = ofAll("hello".toCharArray()).update(0, Character::toUpperCase);
            LazyList<Character> expected = ofAll("Hello".toCharArray());
            assertThat(actual).isEqualTo(expected);
        }
    }

    // -- slice(beginIndex, endIndex)

    @Test
    public void shouldReturnNilWhenSliceFrom0To0OnNil() {
        LazyList<Integer> actual = this.<Integer>empty().slice(0, 0);
        assertThat(actual).isEmpty();
    }

    @Test
    public void shouldReturnNilWhenSliceFrom0To0OnNonNil() {
        LazyList<Integer> actual = of(1).slice(0, 0);
        assertThat(actual).isEmpty();
    }

    @Test
    public void shouldReturnLazyListWithFirstElementWhenSliceFrom0To1OnNonNil() {
        LazyList<Integer> actual = of(1).slice(0, 1);
        assertThat(actual).isEqualTo(of(1));
    }

    @Test
    public void shouldReturnNilWhenSliceFrom1To1OnNonNil() {
        LazyList<Integer> actual = of(1).slice(1, 1);
        assertThat(actual).isEmpty();
    }

    @Test
    public void shouldReturnSliceWhenIndicesAreWithinRange() {
        LazyList<Integer> actual = of(1, 2, 3).slice(1, 3);
        assertThat(actual).isEqualTo(of(2, 3));
    }

    @Test
    public void shouldReturnNilOnSliceWhenIndicesBothAreUpperBound() {
        LazyList<Integer> actual = of(1, 2, 3).slice(3, 3);
        assertThat(actual).isEmpty();
    }

    @Test
    public void shouldComputeSliceOnNonNilWhenBeginIndexIsGreaterThanEndIndex() {
        assertThat(of(1, 2, 3).slice(1, 0)).isEmpty();
    }

    @Test
    public void shouldComputeSliceOnNilWhenBeginIndexIsGreaterThanEndIndex() {
        assertThat(empty().slice(1, 0)).isEmpty();
    }

    @Test
    public void shouldComputeSliceOnNonNilWhenBeginIndexExceedsLowerBound() {
        assertThat(of(1, 2, 3).slice(-1, 2)).isEqualTo(of(1, 2));
    }

    @Test
    public void shouldComputeSliceOnNilWhenBeginIndexExceedsLowerBound() {
        assertThat(empty().slice(-1, 2)).isEmpty();
    }

    @Test
    public void shouldThrowWhenSlice2OnNil() {
        assertThat(empty().slice(0, 1)).isEmpty();
    }

    @Test
    public void shouldComputeSliceWhenEndIndexExceedsUpperBound() {
        assertThat(of(1, 2, 3).slice(1, 4)).isEqualTo(of(2, 3));
    }

    @Test
    public void shouldComputeSliceWhenBeginIndexIsGreaterThanEndIndex() {
        assertThat(of(1, 2, 3).slice(2, 1)).isEmpty();
    }

    @Test
    public void shouldComputeSliceWhenBeginIndexAndEndIndexAreBothOutOfBounds() {
        assertThat(of(1, 2, 3).slice(-10, 10)).isEqualTo(of(1, 2, 3));
    }

    @Nested
    class SortedTests {
        @Test
        public void shouldSortNil() {
            assertThat(empty().sorted()).isEmpty();
        }

        @Test
        public void shouldSortNonNil() {
            assertThat(of(3, 4, 1, 2).sorted()).isEqualTo(of(1, 2, 3, 4));
        }
    }

    // -- sorted(Comparator)

    @Test
    public void shouldSortNilUsingComparator() {
        assertThat(this.<Integer>empty().sorted((i, j) -> j - i)).isEmpty();
    }

    @Test
    public void shouldSortNonNilUsingComparator() {
        assertThat(of(3, 4, 1, 2).sorted((i, j) -> j - i)).isEqualTo(of(4, 3, 2, 1));
    }

    // -- sortBy(Function)

    @Test
    public void shouldSortByNilUsingFunction() {
        assertThat(this.<String>empty().sortBy(String::length)).isEmpty();
    }

    @Test
    public void shouldSortByNonNilUsingFunction() {
        LazyList<String> testee = of("aaa", "b", "cc");
        LazyList<String> actual = testee.sortBy(String::length);
        LazyList<String> expected = of("b", "cc", "aaa");
        assertThat(actual).isEqualTo(expected);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void shouldSortByFunctionWhenElementsAreInfiniteLazyLists() {
        LazyList<Integer> stream1 = LazyList.continually(1);
        LazyList<Integer> stream2 = LazyList.continually(2);
        LazyList<LazyList<Integer>> testee = of(stream2, stream1);
        LazyList<LazyList<Integer>> actual = testee.sortBy(LazyList::head);
        LazyList<LazyList<Integer>> expected = of(stream1, stream2);
        assertThat(actual).isEqualTo(expected);
    }

    // -- sortBy(Comparator, Function)

    @Test
    public void shouldSortByNilUsingComparatorAndFunction() {
        assertThat(this.<String>empty().sortBy(String::length)).isEmpty();
    }

    @Test
    public void shouldSortByNonNilUsingComparatorAndFunction() {
        LazyList<String> testee = of("aaa", "b", "cc");
        LazyList<String> actual = testee.sortBy((i1, i2) -> i2 - i1, String::length);
        LazyList<String> expected = of("aaa", "cc", "b");
        assertThat(actual).isEqualTo(expected);
    }

    @Nested
    class SplitatIndexTests {
        @Test
        public void shouldSplitAtNil() {
            assertThat(empty().splitAt(1)).isEqualTo(Tuple.of(empty(), empty()));
        }

        @Test
        public void shouldSplitAtNonNil() {
            assertThat(of(1, 2, 3).splitAt(1)).isEqualTo(Tuple.of(of(1), of(2, 3)));
        }

        @Test
        public void shouldSplitAtBegin() {
            assertThat(of(1, 2, 3).splitAt(0)).isEqualTo(Tuple.of(empty(), of(1, 2, 3)));
        }

        @Test
        public void shouldSplitAtEnd() {
            assertThat(of(1, 2, 3).splitAt(3)).isEqualTo(Tuple.of(of(1, 2, 3), empty()));
        }

        @Test
        public void shouldSplitAtOutOfBounds() {
            assertThat(of(1, 2, 3).splitAt(5)).isEqualTo(Tuple.of(of(1, 2, 3), empty()));
            assertThat(of(1, 2, 3).splitAt(-1)).isEqualTo(Tuple.of(empty(), of(1, 2, 3)));
        }
    }

    @Nested
    class SplitatPredicateTests {
        @Test
        public void shouldSplitPredicateAtNil() {
            assertThat(empty().splitAt(e -> true)).isEqualTo(Tuple.of(empty(), empty()));
        }

        @Test
        public void shouldSplitPredicateAtNonNil() {
            assertThat(of(1, 2, 3).splitAt(e -> e == 2)).isEqualTo(Tuple.of(of(1), of(2, 3)));
        }

        @Test
        public void shouldSplitAtPredicateBegin() {
            assertThat(of(1, 2, 3).splitAt(e -> e == 1)).isEqualTo(Tuple.of(empty(), of(1, 2, 3)));
        }

        @Test
        public void shouldSplitAtPredicateEnd() {
            assertThat(of(1, 2, 3).splitAt(e -> e == 3)).isEqualTo(Tuple.of(of(1, 2), of(3)));
        }

        @Test
        public void shouldSplitAtPredicateNotFound() {
            assertThat(of(1, 2, 3).splitAt(e -> e == 5)).isEqualTo(Tuple.of(of(1, 2, 3), empty()));
        }
    }

    @Nested
    class SplitatinclusivePredicateTests {
        @Test
        public void shouldSplitInclusivePredicateAtNil() {
            assertThat(empty().splitAtInclusive(e -> true)).isEqualTo(Tuple.of(empty(), empty()));
        }

        @Test
        public void shouldSplitInclusivePredicateAtNonNil() {
            assertThat(of(1, 2, 3).splitAtInclusive(e -> e == 2)).isEqualTo(Tuple.of(of(1, 2), of(3)));
        }

        @Test
        public void shouldSplitAtInclusivePredicateBegin() {
            assertThat(of(1, 2, 3).splitAtInclusive(e -> e == 1)).isEqualTo(Tuple.of(of(1), of(2, 3)));
        }

        @Test
        public void shouldSplitAtInclusivePredicateEnd() {
            assertThat(of(1, 2, 3).splitAtInclusive(e -> e == 3)).isEqualTo(Tuple.of(of(1, 2, 3), empty()));
        }

        @Test
        public void shouldSplitAtInclusivePredicateNotFound() {
            assertThat(of(1, 2, 3).splitAtInclusive(e -> e == 5)).isEqualTo(Tuple.of(of(1, 2, 3), empty()));
        }
    }

    @Nested
    class SpliteratorTests {
        @Test
        public void shouldNotHaveSortedSpliterator() {
            assertThat(of(1, 2, 3).spliterator().hasCharacteristics(Spliterator.SORTED))
                    .isFalse();
        }

        @Test
        public void shouldHaveOrderedSpliterator() {
            assertThat(of(1, 2, 3).spliterator().hasCharacteristics(Spliterator.ORDERED))
                    .isTrue();
        }

        @Test
        public void shouldNotHaveDistinctSpliterator() {
            assertThat(of(1, 2, 3).spliterator().hasCharacteristics(Spliterator.DISTINCT))
                    .isFalse();
        }
    }

    @Nested
    class StartswithTests {
        @Test
        public void shouldStartsNilOfNilCalculate() {
            assertThat(empty().startsWith(empty())).isTrue();
        }

        @Test
        public void shouldStartsNilOfNonNilCalculate() {
            assertThat(empty().startsWith(of(1))).isFalse();
        }

        @Test
        public void shouldStartsNilOfNilWithOffsetCalculate() {
            assertThat(empty().startsWith(empty(), 1)).isTrue();
        }

        @Test
        public void shouldStartsNilOfNonNilWithOffsetCalculate() {
            assertThat(empty().startsWith(of(1), 1)).isFalse();
        }

        @Test
        public void shouldStartsNonNilOfNilCalculate() {
            assertThat(of(1, 2, 3).startsWith(empty())).isTrue();
        }

        @Test
        public void shouldStartsNonNilOfNonNilCalculate() {
            assertThat(of(1, 2, 3).startsWith(of(1, 2))).isTrue();
            assertThat(of(1, 2, 3).startsWith(of(1, 2, 3))).isTrue();
            assertThat(of(1, 2, 3).startsWith(of(1, 2, 3, 4))).isFalse();
            assertThat(of(1, 2, 3).startsWith(of(1, 3))).isFalse();
        }

        @Test
        public void shouldStartsNonNilOfNilWithOffsetCalculate() {
            assertThat(of(1, 2, 3).startsWith(empty(), 1)).isTrue();
        }

        @Test
        public void shouldNotStartsNonNilOfNonNilWithNegativeOffsetCalculate() {
            assertThat(of(1, 2, 3).startsWith(of(1), -1)).isFalse();
        }

        @Test
        public void shouldNotStartsNonNilOfNonNilWithOffsetEqualLengthCalculate() {
            assertThat(of(1, 2, 3).startsWith(of(3), 3)).isFalse();
        }

        @Test
        public void shouldNotStartsNonNilOfNonNilWithOffsetEndCalculate() {
            assertThat(of(1, 2, 3).startsWith(of(3), 2)).isTrue();
        }

        @Test
        public void shouldStartsNonNilOfNonNilWithOffsetAtStartCalculate() {
            assertThat(of(1, 2, 3).startsWith(of(1), 0)).isTrue();
        }

        @Test
        public void shouldStartsNonNilOfNonNilWithOffsetCalculate1() {
            assertThat(of(1, 2, 3).startsWith(of(2, 3), 1)).isTrue();
        }

        @Test
        public void shouldStartsNonNilOfNonNilWithOffsetCalculate2() {
            assertThat(of(1, 2, 3).startsWith(of(2, 3, 4), 1)).isFalse();
        }

        @Test
        public void shouldStartsNonNilOfNonNilWithOffsetCalculate3() {
            assertThat(of(1, 2, 3).startsWith(of(2, 4), 1)).isFalse();
        }
    }

    // -- subSequence(beginIndex)

    @Test
    public void shouldReturnNilWhenSubSequenceFrom0OnNil() {
        LazyList<Integer> actual = this.<Integer>empty().subSequence(0);
        assertThat(actual).isEmpty();
    }

    @Test
    public void shouldReturnIdentityWhenSubSequenceFrom0OnNonNil() {
        LazyList<Integer> actual = of(1).subSequence(0);
        assertThat(actual).isEqualTo(of(1));
    }

    @Test
    public void shouldReturnNilWhenSubSequenceFrom1OnLazyListOf1() {
        LazyList<Integer> actual = of(1).subSequence(1);
        assertThat(actual).isEmpty();
    }

    @Test
    public void shouldReturnSubSequenceWhenIndexIsWithinRange() {
        LazyList<Integer> actual = of(1, 2, 3).subSequence(1);
        assertThat(actual).isEqualTo(of(2, 3));
    }

    @Test
    public void shouldReturnNilWhenSubSequenceBeginningWithSize() {
        LazyList<Integer> actual = of(1, 2, 3).subSequence(3);
        assertThat(actual).isEmpty();
    }

    @Test
    public void shouldThrowWhenSubSequenceOnNil() {
        assertThrows(IndexOutOfBoundsException.class, () -> empty().subSequence(1));
    }

    @Test
    public void shouldThrowWhenSubSequenceWithOutOfLowerBound() {
        assertThrows(IndexOutOfBoundsException.class, () -> of(1, 2, 3).subSequence(-1));
    }

    @Test
    public void shouldThrowWhenSubSequenceWithOutOfUpperBound() {
        assertThrows(IndexOutOfBoundsException.class, () -> of(1, 2, 3).subSequence(4));
    }

    @Test
    public void shouldReturnSameInstanceIfSubSequenceStartsAtZero() {
        LazyList<Integer> seq = of(1, 2, 3);
        assertThat(seq.subSequence(0)).isSameAs(seq);
    }

    // -- subSequence(beginIndex, endIndex)

    @Test
    public void shouldReturnNilWhenSubSequenceFrom0To0OnNil() {
        LazyList<Integer> actual = this.<Integer>empty().subSequence(0, 0);
        assertThat(actual).isEmpty();
    }

    @Test
    public void shouldReturnNilWhenSubSequenceFrom0To0OnNonNil() {
        LazyList<Integer> actual = of(1).subSequence(0, 0);
        assertThat(actual).isEmpty();
    }

    @Test
    public void shouldReturnLazyListWithFirstElementWhenSubSequenceFrom0To1OnNonNil() {
        LazyList<Integer> actual = of(1).subSequence(0, 1);
        assertThat(actual).isEqualTo(of(1));
    }

    @Test
    public void shouldReturnNilWhenSubSequenceFrom1To1OnNonNil() {
        LazyList<Integer> actual = of(1).subSequence(1, 1);
        assertThat(actual).isEmpty();
    }

    @Test
    public void shouldReturnSubSequenceWhenIndicesAreWithinRange() {
        LazyList<Integer> actual = of(1, 2, 3).subSequence(1, 3);
        assertThat(actual).isEqualTo(of(2, 3));
    }

    @Test
    public void shouldReturnNilWhenOnSubSequenceIndicesBothAreUpperBound() {
        LazyList<Integer> actual = of(1, 2, 3).subSequence(3, 3);
        assertThat(actual).isEmpty();
    }

    @Test
    public void shouldThrowOnSubSequenceOnNonNilWhenBeginIndexIsGreaterThanEndIndex() {
        assertThrows(IllegalArgumentException.class, () -> of(1, 2, 3).subSequence(1, 0));
    }

    @Test
    public void shouldThrowOnSubSequenceOnNilWhenBeginIndexIsGreaterThanEndIndex() {
        assertThrows(IllegalArgumentException.class, () -> empty().subSequence(1, 0));
    }

    @Test
    public void shouldThrowOnSubSequenceOnNonNilWhenBeginIndexExceedsLowerBound() {
        assertThrows(IndexOutOfBoundsException.class, () -> of(1, 2, 3).subSequence(-1, 2));
    }

    @Test
    public void shouldThrowOnSubSequenceOnNilWhenBeginIndexExceedsLowerBound() {
        assertThrows(IndexOutOfBoundsException.class, () -> empty().subSequence(-1, 2));
    }

    @Test
    public void shouldThrowWhenSubSequence2OnNil() {
        assertThrows(IndexOutOfBoundsException.class, () -> empty().subSequence(0, 1));
    }

    @Test
    public void shouldThrowOnSubSequenceWhenEndIndexExceedsUpperBound() {
        assertThrows(
                IndexOutOfBoundsException.class,
                () -> of(1, 2, 3)
                        .subSequence(1, 4)
                        .mkString()); // force computation of last element, e.g. because LazyList is lazy
    }

    @Test
    public void shouldThrowOnSubSequenceWhenBeginIndexIsGreaterThanEndIndex() {
        assertThrows(
                IllegalArgumentException.class,
                () -> of(1, 2, 3)
                        .subSequence(2, 1)
                        .mkString()); // force computation of last element, e.g. because LazyList is lazy
    }

    @Nested
    class SearchElementTests {
        @Test
        public void shouldSearchIndexForPresentElements() {
            assertThat(of(1, 2, 3, 4, 5, 6).search(3)).isEqualTo(2);
        }

        @Test
        public void shouldSearchNegatedInsertionPointMinusOneForAbsentElements() {
            assertThat(empty().search(42)).isEqualTo(-1);
            assertThat(of(10, 20, 30).search(25)).isEqualTo(-3);
        }
    }

    // -- search(element,comparator)

    @Test
    public void shouldSearchIndexForPresentElementsUsingComparator() {
        assertThat(of(1, 2, 3, 4, 5, 6).search(3, Integer::compareTo)).isEqualTo(2);
    }

    @Test
    public void shouldSearchNegatedInsertionPointMinusOneForAbsentElementsUsingComparator() {
        assertThat(this.<Integer>empty().search(42, Integer::compareTo)).isEqualTo(-1);
        assertThat(of(10, 20, 30).search(25, Integer::compareTo)).isEqualTo(-3);
    }

    @Nested
    class TransposeTests {
        @Test
        public void shouldTransposeIfEmpty() {
            LazyList<LazyList<Integer>> actual = empty();
            assertThat(transpose(actual)).isSameAs(actual);
        }

        @Test
        public void shouldTransposeIfIs1x0() {
            LazyList<LazyList<Integer>> actual = of(empty());
            assertThat(transpose(actual)).isSameAs(actual);
        }

        @Test
        public void shouldTransposeIfIs1x1() {
            LazyList<LazyList<Integer>> actual = of(of(1));
            assertThat(transpose(actual)).isSameAs(actual);
        }

        @Test
        public void shouldTransposeIfSingleValued() {
            LazyList<LazyList<Integer>> actual = of(of(0));
            LazyList<LazyList<Integer>> expected = of(of(0));
            assertThat(transpose(actual)).isEqualTo(expected);
        }

        @Test
        @SuppressWarnings("unchecked")
        public void shouldTransposeIfMultiValuedColumn() {
            LazyList<LazyList<Integer>> actual = of(of(0, 1, 2));
            LazyList<LazyList<Integer>> expected = of(of(0), of(1), of(2));
            assertThat(transpose(actual)).isEqualTo(expected);
        }

        @Test
        @SuppressWarnings("unchecked")
        public void shouldTransposeIfMultiValuedRow() {
            LazyList<LazyList<Integer>> actual = of(of(0), of(1), of(2));
            LazyList<LazyList<Integer>> expected = of(of(0, 1, 2));
            assertThat(transpose(actual)).isEqualTo(expected);
        }

        @Test
        @SuppressWarnings("unchecked")
        public void shouldTransposeIfMultiValuedIfSymmetric() {
            LazyList<LazyList<Integer>> actual = of(of(1, 2, 3), of(4, 5, 6), of(7, 8, 9));
            LazyList<LazyList<Integer>> expected = of(of(1, 4, 7), of(2, 5, 8), of(3, 6, 9));
            assertThat(transpose(actual)).isEqualTo(expected);
        }

        @Test
        @SuppressWarnings("unchecked")
        public void shouldTransposeIfMultiValuedWithMoreColumnsThanRows() {
            LazyList<LazyList<Integer>> actual = of(of(1, 2, 3), of(4, 5, 6));
            LazyList<LazyList<Integer>> expected = of(of(1, 4), of(2, 5), of(3, 6));
            assertThat(transpose(actual)).isEqualTo(expected);
        }

        @Test
        @SuppressWarnings("unchecked")
        public void shouldTransposeIfMultiValuedWithMoreRowsThanColumns() {
            LazyList<LazyList<Integer>> actual = of(of(1, 2), of(3, 4), of(5, 6));
            LazyList<LazyList<Integer>> expected = of(of(1, 3, 5), of(2, 4, 6));
            assertThat(transpose(actual)).isEqualTo(expected);
        }

        @Test
        @SuppressWarnings("unchecked")
        public void shouldBeEqualIfTransposedTwice() {
            LazyList<LazyList<Integer>> actual = of(of(1, 2, 3), of(4, 5, 6));
            LazyList<LazyList<Integer>> transposed = transpose(actual);
            assertThat(transpose(transposed)).isEqualTo(actual);
        }

        @Test
        @SuppressWarnings("unchecked")
        public void shouldNotTransposeForMissingOrEmptyValues() {
            assertThrows(IllegalArgumentException.class, () -> {
                LazyList<LazyList<Integer>> actual = of(of(), of(0, 1), of(2, 3, 4, 5), of(), of(6, 7, 8));
                transpose(actual);
            });
        }
    }

    @Nested
    class NonLazyListArgumentTests {
        @Test
        public void shouldStartWithANonLazyListIterable() {
            assertThat(of(1, 3, 4).startsWith(List.of(1, 3))).isTrue();
            assertThat(of(1, 2, 3, 4).startsWith(List.of(1, 2, 4))).isFalse();
            assertThat(of(1, 2).startsWith(List.of(1, 2, 4))).isFalse();
        }

        @Test
        public void shouldEndWithANonLazyListIterable() {
            assertThat(of(1, 3, 4).endsWith(List.of(3, 4))).isTrue();
            assertThat(of(1, 2, 3, 4).endsWith(List.of(2, 3, 5))).isFalse();
        }
    }

    // -- distinctByKeepLast(Comparator)

    @TestTemplate
    public void shouldComputeDistinctByKeepLastOfEmptyLazyListUsingComparator() {
        Comparator<Integer> comparator = comparingInt(i -> i);
        if (useIsEqualToInsteadOfIsSameAs()) {
            assertThat(this.<Integer>empty().distinctByKeepLast(comparator)).isEqualTo(empty());
        } else {
            assertThat(this.<Integer>empty().distinctByKeepLast(comparator)).isSameAs(empty());
        }
    }

    @TestTemplate
    public void shouldComputeDistinctByKeepLastOfNonEmptyLazyListUsingComparator() {
        Comparator<String> comparator = comparingInt(s -> (s.charAt(1)));
        LazyList<String> distinct = of("1a", "2a", "3b", "4b", "3a", "5c").distinctByKeepLast(comparator);
        assertThat(distinct).isEqualTo(of("4b", "3a", "5c"));
    }

    @TestTemplate
    public void shouldReturnSameInstanceWhenDistinctByKeepLastComparatorEmptyLazyList() {
        LazyList<?> empty = empty();
        assertThat(empty.distinctByKeepLast(Comparators.naturalComparator())).isSameAs(empty);
    }

    // -- distinctByKeepLast(Function)

    @TestTemplate
    public void shouldComputeDistinctByKeepLastOfEmptyLazyListUsingKeyExtractor() {
        if (useIsEqualToInsteadOfIsSameAs()) {
            assertThat(empty().distinctByKeepLast(Function.identity())).isEqualTo(empty());
        } else {
            assertThat(empty().distinctByKeepLast(Function.identity())).isSameAs(empty());
        }
    }

    @TestTemplate
    public void shouldComputeDistinctByKeepLastOfNonEmptyLazyListUsingKeyExtractor() {
        Function<String, Character> function = c -> c.charAt(1);
        LazyList<String> distinct = of("1a", "2a", "3b", "4b", "3a", "5c").distinctByKeepLast(function);
        assertThat(distinct).isEqualTo(of("4b", "3a", "5c"));
    }

    @TestTemplate
    public void shouldReturnSameInstanceWhenDistinctByKeepLastFunctionEmptyLazyList() {
        LazyList<?> empty = empty();
        assertThat(empty.distinctByKeepLast(Function.identity())).isSameAs(empty);
    }

    private interface SomeInterface {}

    enum OneEnum implements SomeInterface {
        A1,
        A2,
        A3;
    }

    enum SecondEnum implements SomeInterface {
        A1,
        A2,
        A3;
    }

    // -- removeAll / retainAll with null elements go through a HashSet, whose contains must not use Option

    @Nested
    class NullElementRemoveAllTests {

        @Test
        public void shouldRejectNullOnOf() {
            assertThatNullPointerException().isThrownBy(() -> LazyListTest.this.<Integer>of((Integer) null));
        }

        @Test
        public void shouldRejectNullOnOfVarargs() {
            assertThatNullPointerException().isThrownBy(() -> LazyListTest.this.<Integer>of(1, null));
        }
    }

    @Nested
    class TraversableOnlyTests {
        @Test
        public void shouldImplementTraversableOnly() {
            assertThat(Traversable.class.isAssignableFrom(LazyList.class)).isTrue();
            assertThat(LazyList.class.getInterfaces()).containsExactly(Traversable.class);
            // no sequence interface above Traversable: a LazyList reaches Traversable directly
            assertThat(supertypeNames(LazyList.class)).containsExactlyInAnyOrder("Traversable", "Iterable");
        }

        @Test
        public void shouldHaveTheSameSupertypesOnBothCases() {
            assertThat(supertypeNames(LazyList.empty().getClass()))
                    .containsExactlyInAnyOrder("LazyList", "Traversable", "Iterable");
            assertThat(supertypeNames(LazyList.of(1).getClass()))
                    .containsExactlyInAnyOrder("LazyList", "Traversable", "Iterable");
        }

        @Test
        public void shouldHaveAnOrderedButNotSizedSpliterator() {
            assertThat(of(1, 2, 3).spliterator().hasCharacteristics(Spliterator.ORDERED))
                    .isTrue();
            assertThat(of(1, 2, 3).spliterator().hasCharacteristics(Spliterator.SIZED))
                    .isFalse();
        }
    }

    @Nested
    class LazySearchTests {

        @Test
        public void shouldSearchAnInfiniteLazyListWithoutForcingItWhole() {
            LazyList<Integer> naturals = LazyList.from(1);
            assertThat(naturals.indexOf(5)).isEqualTo(4);
            assertThat(naturals.indexOf(5, 2)).isEqualTo(4);
            assertThat(naturals.indexOfOption(5)).isEqualTo(Option.some(4));
            assertThat(naturals.indexWhere(i -> i > 3)).isEqualTo(3);
            assertThat(naturals.indexWhere(i -> i > 3, 10)).isEqualTo(10);
            assertThat(naturals.indexWhereOption(i -> i > 3)).isEqualTo(Option.some(3));
            assertThat(naturals.indexOfSlice(List.of(4, 5))).isEqualTo(3);
            assertThat(naturals.indexOfSliceOption(List.of(4, 5))).isEqualTo(Option.some(3));
            assertThat(naturals.containsSlice(List.of(4, 5, 6))).isTrue();
            assertThat(naturals.startsWith(List.of(1, 2, 3))).isTrue();
            assertThat(naturals.startsWith(List.of(3, 4), 2)).isTrue();
            assertThat(naturals.prefixLength(i -> i < 4)).isEqualTo(3);
            assertThat(naturals.segmentLength(i -> i < 6, 2)).isEqualTo(3);
            assertThat(naturals.search(4)).isEqualTo(3);
            assertThat(naturals.search(4, Comparator.naturalOrder())).isEqualTo(3);
            assertThat(naturals.drop(3).take(2).toList()).isEqualTo(List.of(4, 5));
            assertThat(naturals.crossProduct(LazyList.from(1))
                            .take(3)
                            .map(Tuple2::_2)
                            .toList())
                    .isEqualTo(List.of(1, 2, 3));
        }

        @Test
        public void shouldForceEachCellAtMostOnce() {
            java.util.concurrent.atomic.AtomicInteger forced = new java.util.concurrent.atomic.AtomicInteger();
            LazyList<Integer> counted = LazyList.from(1).map(i -> {
                forced.incrementAndGet();
                return i;
            });
            assertThat(counted.indexOf(3)).isEqualTo(2);
            int afterFirstSearch = forced.get();
            assertThat(counted.indexOf(3)).isEqualTo(2);
            assertThat(forced.get()).isEqualTo(afterFirstSearch); // memoised: the same cells are not recomputed
        }

        @Test
        public void shouldReturnALazyList() {
            LazyList<Integer> stream = LazyList.of(1, 2, 3);
            assertThat(stream.rotateLeft(1)).isInstanceOf(LazyList.class).isEqualTo(LazyList.of(2, 3, 1));
            assertThat(stream.rotateRight(1)).isInstanceOf(LazyList.class).isEqualTo(LazyList.of(3, 1, 2));
            assertThat(stream.sortBy(i -> -i)).isInstanceOf(LazyList.class).isEqualTo(LazyList.of(3, 2, 1));
            assertThat(stream.crossProduct(2).head()).isInstanceOf(LazyList.class);
        }

        @Test
        public void shouldHandleTheEmptyAndSingleCases() {
            LazyList<Integer> empty = LazyList.empty();
            assertThat(empty.reverse().iterator().hasNext()).isFalse();
            assertThat(empty.indexOf(1)).isEqualTo(-1);
            assertThat(empty.lastIndexOf(1)).isEqualTo(-1);
            assertThat(empty.indexWhere(i -> true)).isEqualTo(-1);
            assertThat(empty.lastIndexWhere(i -> true)).isEqualTo(-1);
            assertThat(empty.prefixLength(i -> true)).isEqualTo(0);
            assertThat(empty.segmentLength(i -> true, 0)).isEqualTo(0);
            assertThat(empty.containsSlice(LazyList.empty())).isTrue();
            assertThat(empty.startsWith(LazyList.empty())).isTrue();
            assertThat(empty.endsWith(LazyList.empty())).isTrue();
            assertThat(empty.search(1)).isEqualTo(-1);
            assertThat(empty.crossProduct().isEmpty()).isTrue();
            LazyList<Integer> one = LazyList.of(1);
            assertThat(one.reverse().toList()).isEqualTo(List.of(1));
            assertThat(one.crossProduct().toList().size()).isEqualTo(1);
            assertThat(one.endsWith(LazyList.of(1))).isTrue();
            assertThat(one.endsWith(LazyList.of(0, 1))).isFalse();
            assertThat(one.lastIndexOfSlice(LazyList.of(1))).isEqualTo(0);
        }

        @Test
        public void shouldRejectNullArgumentsOfEveryNewlyDeclaredMethod() {
            LazyList<Integer> stream = LazyList.of(1, 2, 3);
            assertThatNullPointerException()
                    .isThrownBy(() -> stream.containsSlice(null))
                    .withMessage("that is null");
            assertThatNullPointerException()
                    .isThrownBy(() -> stream.crossProduct(null))
                    .withMessage("that is null");
            assertThatNullPointerException()
                    .isThrownBy(() -> stream.endsWith(null))
                    .withMessage("that is null");
            assertThatNullPointerException()
                    .isThrownBy(() -> stream.startsWith(null))
                    .withMessage("that is null");
            assertThatNullPointerException()
                    .isThrownBy(() -> stream.indexOfSlice(null))
                    .withMessage("that is null");
            assertThatNullPointerException()
                    .isThrownBy(() -> stream.lastIndexOfSlice(null))
                    .withMessage("that is null");
            assertThatNullPointerException()
                    .isThrownBy(() -> stream.indexWhere(null))
                    .withMessage("predicate is null");
            assertThatNullPointerException()
                    .isThrownBy(() -> stream.lastIndexWhere(null))
                    .withMessage("predicate is null");
            assertThatNullPointerException()
                    .isThrownBy(() -> stream.segmentLength(null, 0))
                    .withMessage("predicate is null");
            assertThatNullPointerException()
                    .isThrownBy(() -> stream.search(1, null))
                    .withMessage("comparator is null");
            assertThatNullPointerException()
                    .isThrownBy(() -> stream.sortBy(null, Function.identity()))
                    .withMessage("comparator is null");
            assertThatNullPointerException()
                    .isThrownBy(() -> stream.sortBy(Comparator.naturalOrder(), null))
                    .withMessage("mapper is null");
        }
    }

    // -- the one-pass and positional cases every sequence answers

    // -- distinct

    @TestTemplate
    public void shouldComputeDistinctOfEmptyTraversable() {
        if (useIsEqualToInsteadOfIsSameAs()) {
            assertThat(empty().distinct()).isEqualTo(empty());
        } else {
            assertThat(empty().distinct()).isSameAs(empty());
        }
    }

    @TestTemplate
    public void shouldComputeDistinctOfNonEmptyTraversable() {
        LazyList<Integer> testee = of(1, 1, 2, 2, 3, 3);
        LazyList<Integer> actual = testee.distinct();
        LazyList<Integer> expected = of(1, 2, 3);
        assertThat(actual).isEqualTo(expected);
        if (isDistinct()) {
            assertThat(actual).isSameAs(testee);
        }
    }

    // -- distinctBy(Comparator)

    @TestTemplate
    public void shouldComputeDistinctByOfEmptyTraversableUsingComparator() {
        Comparator<Integer> comparator = comparingInt(i -> i);
        if (useIsEqualToInsteadOfIsSameAs()) {
            assertThat(this.<Integer>empty().distinctBy(comparator)).isEqualTo(empty());
        } else {
            assertThat(this.<Integer>empty().distinctBy(comparator)).isSameAs(empty());
        }
    }

    @TestTemplate
    public void shouldComputeDistinctByOfNonEmptyTraversableUsingComparator() {
        Comparator<String> comparator = comparingInt(s -> (s.charAt(1)));
        LazyList<String> distinct =
                of("1a", "2a", "3a", "3b", "4b", "5c").distinctBy(comparator).map(s -> s.substring(1));
        assertThat(distinct).isEqualTo(of("a", "b", "c"));
    }

    @TestTemplate
    public void shouldReturnSameInstanceWhenDistinctByComparatorEmptyTraversable() {
        LazyList<?> empty = empty();
        assertThat(empty.distinctBy(Comparators.naturalComparator())).isSameAs(empty);
    }

    // -- distinctBy(Function)

    @TestTemplate
    public void shouldComputeDistinctByOfEmptyTraversableUsingKeyExtractor() {
        if (useIsEqualToInsteadOfIsSameAs()) {
            assertThat(empty().distinctBy(Function.identity())).isEqualTo(empty());
        } else {
            assertThat(empty().distinctBy(Function.identity())).isSameAs(empty());
        }
    }

    @TestTemplate
    public void shouldComputeDistinctByOfNonEmptyTraversableUsingKeyExtractor() {
        Function<String, Character> function = c -> c.charAt(1);
        LazyList<String> distinct =
                of("1a", "2a", "3a", "3b", "4b", "5c").distinctBy(function).map(s -> s.substring(1));
        assertThat(distinct).isEqualTo(of("a", "b", "c"));
    }

    @TestTemplate
    public void shouldReturnSameInstanceWhenDistinctByFunctionEmptyTraversable() {
        LazyList<?> empty = empty();
        assertThat(empty.distinctBy(Function.identity())).isSameAs(empty);
    }

    // -- drop

    @TestTemplate
    public void shouldDropNoneOnNil() {
        if (useIsEqualToInsteadOfIsSameAs()) {
            assertThat(empty().drop(1)).isEqualTo(empty());
        } else {
            assertThat(empty().drop(1)).isSameAs(empty());
        }
    }

    @TestTemplate
    public void shouldDropNoneIfCountIsNegative() {
        assertThat(of(1, 2, 3).drop(-1)).isEqualTo(of(1, 2, 3));
    }

    @TestTemplate
    public void shouldDropAsExpectedIfCountIsLessThanSize() {
        assertThat(of(1, 2, 3).drop(2)).isEqualTo(of(3));
    }

    @TestTemplate
    public void shouldDropAllIfCountExceedsSize() {
        if (useIsEqualToInsteadOfIsSameAs()) {
            assertThat(of(1, 2, 3).drop(4)).isEqualTo(empty());
        } else {
            assertThat(of(1, 2, 3).drop(4)).isSameAs(empty());
        }
    }

    @TestTemplate
    public void shouldReturnSameInstanceWhenDropZeroCount() {
        LazyList<Integer> t = of(1, 2, 3);
        assertThat(t.drop(0)).isSameAs(t);
    }

    @TestTemplate
    public void shouldReturnSameInstanceWhenDropNegativeCount() {
        LazyList<Integer> t = of(1, 2, 3);
        assertThat(t.drop(-1)).isSameAs(t);
    }

    @TestTemplate
    public void shouldReturnSameInstanceWhenEmptyDropOne() {
        LazyList<?> empty = empty();
        assertThat(empty.drop(1)).isSameAs(empty);
    }

    // -- dropRight

    @TestTemplate
    public void shouldDropRightNoneOnNil() {
        if (useIsEqualToInsteadOfIsSameAs()) {
            assertThat(empty().dropRight(1)).isEqualTo(empty());
        } else {
            assertThat(empty().dropRight(1)).isSameAs(empty());
        }
    }

    @TestTemplate
    public void shouldDropRightNoneIfCountIsNegative() {
        assertThat(of(1, 2, 3).dropRight(-1)).isEqualTo(of(1, 2, 3));
    }

    @TestTemplate
    public void shouldDropRightAsExpectedIfCountIsLessThanSize() {
        assertThat(of(1, 2, 3).dropRight(2)).isEqualTo(of(1));
    }

    @TestTemplate
    public void shouldDropRightAllIfCountExceedsSize() {
        if (useIsEqualToInsteadOfIsSameAs()) {
            assertThat(of(1, 2, 3).dropRight(4)).isEqualTo(empty());
        } else {
            assertThat(of(1, 2, 3).dropRight(4)).isSameAs(empty());
        }
    }

    @TestTemplate
    public void shouldReturnSameInstanceWhenDropRightZeroCount() {
        LazyList<Integer> t = of(1, 2, 3);
        assertThat(t.dropRight(0)).isSameAs(t);
    }

    @TestTemplate
    public void shouldReturnSameInstanceWhenDropRightNegativeCount() {
        LazyList<Integer> t = of(1, 2, 3);
        assertThat(t.dropRight(-1)).isSameAs(t);
    }

    @TestTemplate
    public void shouldReturnSameInstanceWhenEmptyDropRightOne() {
        LazyList<?> empty = empty();
        assertThat(empty.dropRight(1)).isSameAs(empty);
    }

    // -- dropUntil

    @TestTemplate
    public void shouldDropUntilNoneOnNil() {
        if (useIsEqualToInsteadOfIsSameAs()) {
            assertThat(empty().dropUntil(ignored -> true)).isEqualTo(empty());
        } else {
            assertThat(empty().dropUntil(ignored -> true)).isSameAs(empty());
        }
    }

    @TestTemplate
    public void shouldDropUntilNoneIfPredicateIsTrue() {
        if (useIsEqualToInsteadOfIsSameAs()) {
            assertThat(of(1, 2, 3).dropUntil(ignored -> true)).isEqualTo(of(1, 2, 3));
        } else {
            LazyList<Integer> t = of(1, 2, 3);
            assertThat(t.dropUntil(ignored -> true)).isSameAs(t);
        }
    }

    @TestTemplate
    public void shouldDropUntilAllIfPredicateIsFalse() {
        if (useIsEqualToInsteadOfIsSameAs()) {
            assertThat(of(1, 2, 3).dropUntil(ignored -> false)).isEqualTo(empty());
        } else {
            assertThat(of(1, 2, 3).dropUntil(ignored -> false)).isSameAs(empty());
        }
    }

    @TestTemplate
    public void shouldDropUntilCorrect() {
        assertThat(of(1, 2, 3).dropUntil(i -> i >= 2)).isEqualTo(of(2, 3));
    }

    @TestTemplate
    public void shouldReturnSameInstanceWhenEmptyDropUntil() {
        LazyList<?> empty = empty();
        assertThat(empty.dropUntil(ignored -> true)).isSameAs(empty);
    }

    // -- dropWhile

    @TestTemplate
    public void shouldDropWhileNoneOnNil() {
        LazyList<?> empty = empty();
        LazyList<?> actual = empty.dropWhile(ignored -> true);
        if (useIsEqualToInsteadOfIsSameAs()) {
            assertThat(actual).isEqualTo(empty);
        } else {
            assertThat(actual).isSameAs(empty);
        }
    }

    @TestTemplate
    public void shouldDropWhileNoneIfPredicateIsFalse() {
        if (useIsEqualToInsteadOfIsSameAs()) {
            assertThat(of(1, 2, 3).dropWhile(ignored -> false)).isEqualTo(of(1, 2, 3));
        } else {
            LazyList<Integer> t = of(1, 2, 3);
            assertThat(t.dropWhile(ignored -> false)).isSameAs(t);
        }
    }

    @TestTemplate
    public void shouldDropWhileAllIfPredicateIsTrue() {
        LazyList<Integer> actual = of(1, 2, 3).dropWhile(ignored -> true);
        if (useIsEqualToInsteadOfIsSameAs()) {
            assertThat(actual).isEqualTo(empty());
        } else {
            assertThat(actual).isSameAs(empty());
        }
    }

    @TestTemplate
    public void shouldDropWhileAccordingToPredicate() {
        assertThat(of(1, 2, 3).dropWhile(i -> i < 2)).isEqualTo(of(2, 3));
    }

    @TestTemplate
    public void shouldDropWhileAndNotTruncate() {
        assertThat(of(1, 2, 3).dropWhile(i -> i % 2 == 1)).isEqualTo(of(2, 3));
    }

    @TestTemplate
    public void shouldReturnSameInstanceWhenEmptyDropWhile() {
        LazyList<?> empty = empty();
        assertThat(empty.dropWhile(ignored -> true)).isSameAs(empty);
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

    @TestTemplate
    public void shouldBeAwareOfExistingNonUniqueElement() {
        assertThat(of(1, 1, 2).existsUnique(i -> i == 1)).isFalse();
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
            LazyList<Integer> t = of(1, 2, 3);
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
        LazyList<?> empty = empty();
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
            LazyList<Integer> t = of(1, 2, 3);
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
        LazyList<?> empty = empty();
        assertThat(empty.reject(v -> true)).isSameAs(empty);
    }

    // -- findLast

    @TestTemplate
    public void shouldFindLastOfNil() {
        assertThat(empty().findLast(ignored -> true)).isEqualTo(Option.none());
    }

    @TestTemplate
    public void shouldFindLastOfNonNil() {
        assertThat(of(1, 2, 3, 4).findLast(i -> i % 2 == 0)).isEqualTo(Option.some(4));
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
        AtomicInteger calls = new AtomicInteger();
        LazyList<Integer> actual = this.<Integer>empty().collect(i -> {
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
        assertThat(of(1, 2, 3, 4).collect(i -> i % 2 == 0 ? Option.some("e" + i) : Option.none()))
                .isEqualTo(of("e2", "e4"));
    }

    @TestTemplate
    public void shouldCollectWithASwitchInsideTheLambda() {
        LazyList<Integer> actual = of(1, 2, 3).collect(i -> switch (i) {
            case Integer odd when odd % 2 == 1 -> Option.some(odd * 10);
            default -> Option.none();
        });
        assertThat(actual).isEqualTo(of(10, 30));
    }

    @TestTemplate
    public void shouldCallTheCollectMapperOncePerElement() {
        AtomicInteger calls = new AtomicInteger();
        of(1, 2, 3)
                .collect(i -> {
                    calls.incrementAndGet();
                    return i == 2 ? Option.none() : Option.some(i);
                })
                .size();
        assertThat(calls.get()).isEqualTo(3);
    }

    @TestTemplate
    public void shouldRejectNullOptionFromCollectMapper() {
        // the message names the concrete type, which toString prints before the parenthesis (List, IntMap, Iterator...)
        String type = of(1).toString().substring(0, of(1).toString().indexOf('('));
        assertThatThrownBy(() -> of(1).collect(i -> null).size())
                .isInstanceOf(NullPointerException.class)
                .hasMessage(type + ".collect: mapper returned null");
    }

    @TestTemplate
    public void shouldThrowOnCollectWithNullMapper() {
        Function<Integer, Option<Integer>> mapper = null;
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

    // -- foldRight

    @TestTemplate
    public void shouldFoldRightNil() {
        assertThat(this.<String>empty().foldRight("", (x, xs) -> x + xs)).isEqualTo("");
    }

    @TestTemplate
    public void shouldThrowWhenFoldRightNullOperator() {
        assertThrows(NullPointerException.class, () -> this.<String>empty().foldRight(null, null));
    }

    @TestTemplate
    public void shouldFoldRightNonNil() {
        assertThat(of("a", "b", "c").foldRight("!", (x, xs) -> x + xs)).isEqualTo("abc!");
    }

    // -- forEachWithIndex

    @TestTemplate
    public void shouldConsumeNoElementWithIndexWhenEmpty() {
        boolean[] actual = {false};
        boolean[] expected = {false};
        empty().forEachWithIndex((chr, index) -> actual[0] = true);
        assertThat(actual).isEqualTo(expected);
    }

    @TestTemplate
    public void shouldConsumeEachElementWithIndexWhenNonEmpty() {
        java.util.List<Tuple2<Character, Integer>> actual = new java.util.ArrayList<>();
        java.util.List<Tuple2<Character, Integer>> expected =
                Arrays.asList(Tuple.of('a', 0), Tuple.of('b', 1), Tuple.of('c', 2));
        ofAll('a', 'b', 'c').forEachWithIndex((chr, index) -> actual.add(Tuple.of(chr, index)));
        assertThat(actual).isEqualTo(expected);
    }

    // -- groupBy

    @TestTemplate
    public void shouldNilGroupBy() {
        assertThat(empty().groupBy(Function.identity())).isEqualTo(LinkedHashMap.empty());
    }

    @TestTemplate
    public void shouldNonNilGroupByIdentity() {
        Map<?, ?> actual = of('a', 'b', 'c').groupBy(Function.identity());
        Map<?, ?> expected =
                LinkedHashMap.empty().put('a', of('a')).put('b', of('b')).put('c', of('c'));
        assertThat(actual).isEqualTo(expected);
    }

    @TestTemplate
    public void shouldNonNilGroupByEqual() {
        Map<?, ?> actual = of('a', 'b', 'c').groupBy(c -> 1);
        Map<?, ?> expected = LinkedHashMap.empty().put(1, of('a', 'b', 'c'));
        assertThat(actual).isEqualTo(expected);
    }

    // -- arrangeBy

    @TestTemplate
    public void shouldNilArrangeBy() {
        assertThat(empty().arrangeBy(Function.identity())).isEqualTo(Option.some(LinkedHashMap.empty()));
    }

    @TestTemplate
    public void shouldNonNilArrangeByIdentity() {
        Option<Map<Character, Character>> actual = of('a', 'b', 'c').arrangeBy(Function.identity());
        Option<Map<?, ?>> expected =
                Option.some(LinkedHashMap.empty().put('a', 'a').put('b', 'b').put('c', 'c'));
        assertThat(actual).isEqualTo(expected);
    }

    @TestTemplate
    public void shouldNonNilArrangeByEqual() {
        Option<Map<Integer, Character>> actual = of('a', 'b', 'c').arrangeBy(c -> 1);
        Option<Map<?, ?>> expected = Option.none();
        assertThat(actual).isEqualTo(expected);
    }

    // -- grouped

    @TestTemplate
    public void shouldGroupedNil() {
        assertThat(empty().grouped(1).isEmpty()).isTrue();
    }

    @TestTemplate
    public void shouldThrowWhenGroupedWithSizeZero() {
        assertThrows(IllegalArgumentException.class, () -> empty().grouped(0));
    }

    @TestTemplate
    public void shouldThrowWhenGroupedWithNegativeSize() {
        assertThrows(IllegalArgumentException.class, () -> empty().grouped(-1));
    }

    @TestTemplate
    public void shouldGroupedTraversableWithEqualSizedBlocks() {
        List<LazyList<Integer>> actual = of(1, 2, 3, 4).grouped(2).toList();
        List<LazyList<Integer>> expected = List.of(LazyList.of(1, 2), LazyList.of(3, 4));
        assertThat(actual).isEqualTo(expected);
    }

    @TestTemplate
    public void shouldGroupedTraversableWithRemainder() {
        List<LazyList<Integer>> actual = of(1, 2, 3, 4, 5).grouped(2).toList();
        List<LazyList<Integer>> expected = List.of(LazyList.of(1, 2), LazyList.of(3, 4), LazyList.of(5));
        assertThat(actual).isEqualTo(expected);
    }

    @TestTemplate
    public void shouldGroupedWhenTraversableLengthIsSmallerThanBlockSize() {
        List<LazyList<Integer>> actual = of(1, 2, 3, 4).grouped(5).toList();
        List<LazyList<Integer>> expected = List.of(LazyList.of(1, 2, 3, 4));
        assertThat(actual).isEqualTo(expected);
    }

    // -- head

    @TestTemplate
    public void shouldThrowWhenHeadOnNil() {
        assertThrows(NoSuchElementException.class, () -> empty().head());
    }

    @TestTemplate
    public void shouldReturnHeadOfNonNil() {
        assertThat(of(1, 2, 3).head()).isEqualTo(1);
    }

    // -- headOption

    @TestTemplate
    public void shouldReturnNoneWhenCallingHeadOptionOnNil() {
        assertThat(empty().headOption().isEmpty()).isTrue();
    }

    @TestTemplate
    public void shouldReturnSomeHeadWhenCallingHeadOptionOnNonNil() {
        assertThat(of(1, 2, 3).headOption()).isEqualTo(Option.some(1));
    }

    // -- init

    @TestTemplate
    public void shouldThrowWhenInitOfNil() {
        assertThrows(UnsupportedOperationException.class, () -> empty().init());
    }

    @TestTemplate
    public void shouldGetInitOfNonNil() {
        assertThat(of(1, 2, 3).init()).isEqualTo(of(1, 2));
    }

    // -- initOption

    @TestTemplate
    public void shouldReturnNoneWhenCallingInitOptionOnNil() {
        assertThat(empty().initOption().isEmpty()).isTrue();
    }

    @TestTemplate
    public void shouldReturnSomeInitWhenCallingInitOptionOnNonNil() {
        assertThat(of(1, 2, 3).initOption()).isEqualTo(Option.some(of(1, 2)));
    }

    // -- last

    @TestTemplate
    public void shouldThrowWhenLastOnNil() {
        assertThrows(NoSuchElementException.class, () -> empty().last());
    }

    @TestTemplate
    public void shouldReturnLastOfNonNil() {
        assertThat(of(1, 2, 3).last()).isEqualTo(3);
    }

    // -- lastOption

    @TestTemplate
    public void shouldReturnNoneWhenCallingLastOptionOnNil() {
        assertThat(empty().lastOption().isEmpty()).isTrue();
    }

    @TestTemplate
    public void shouldReturnSomeLastWhenCallingLastOptionOnNonNil() {
        assertThat(of(1, 2, 3).lastOption()).isEqualTo(Option.some(3));
    }

    // -- length

    @TestTemplate
    public void shouldComputeLengthOfNil() {
        assertThat(empty().size()).isEqualTo(0);
    }

    @TestTemplate
    public void shouldComputeLengthOfNonNil() {
        assertThat(of(1, 2, 3).size()).isEqualTo(3);
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
        assertThat(of(Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY).max().get())
                .isEqualTo(Double.POSITIVE_INFINITY);
    }

    @TestTemplate
    public void shouldCalculateMaxOfFloatPositiveAndNegativeInfinity() {
        assertThat(of(Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY).max().get())
                .isEqualTo(Float.POSITIVE_INFINITY);
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
        int[] cnt = {0};
        assertThat(of(1, 2, 3).maxBy(i -> {
                    cnt[0]++;
                    return i;
                }))
                .isEqualTo(Option.some(3));
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
        assertThat(of(Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY).min().get())
                .isEqualTo(Double.NEGATIVE_INFINITY);
    }

    @TestTemplate
    public void shouldCalculateMinOfFloatPositiveAndNegativeInfinity() {
        assertThat(of(Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY).min().get())
                .isEqualTo(Double.NEGATIVE_INFINITY);
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
        int[] cnt = {0};
        assertThat(of(1, 2, 3).minBy(i -> {
                    cnt[0]++;
                    return i;
                }))
                .isEqualTo(Option.some(1));
        assertThat(cnt[0]).isEqualTo(3);
    }

    // -- orElse

    @TestTemplate
    public void shouldCaclEmptyOrElseSameOther() {
        Iterable<Integer> other = of(42);
        assertThat(empty().orElse(other)).isSameAs(other);
    }

    @TestTemplate
    public void shouldCaclEmptyOrElseEqualOther() {
        assertThat(empty().orElse(Arrays.asList(1, 2))).isEqualTo(of(1, 2));
    }

    @TestTemplate
    public void shouldCaclNonemptyOrElseOther() {
        LazyList<Integer> src = of(42);
        assertThat(src.orElse(List.of(1))).isEqualTo(src);
    }

    @TestTemplate
    public void shouldCaclEmptyOrElseSameSupplier() {
        Iterable<Integer> other = of(42);
        Supplier<Iterable<Integer>> supplier = () -> other;
        assertThat(empty().orElse(supplier)).isSameAs(other);
    }

    @TestTemplate
    public void shouldCaclEmptyOrElseEqualSupplier() {
        assertThat(empty().orElse(() -> Arrays.asList(1, 2))).isEqualTo(of(1, 2));
    }

    @TestTemplate
    public void shouldCaclNonemptyOrElseSupplier() {
        LazyList<Integer> src = of(42);
        assertThat(src.orElse(() -> List.of(1))).isEqualTo(src);
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

    // -- reduceLeftOption

    @TestTemplate
    public void shouldThrowWhenReduceLeftOptionNil() {
        assertThat(this.<String>empty().reduceLeftOption((a, b) -> a + b)).isSameAs(Option.none());
    }

    @TestTemplate
    public void shouldThrowWhenReduceLeftOptionNullOperator() {
        assertThrows(NullPointerException.class, () -> this.<String>empty().reduceLeftOption(null));
    }

    @TestTemplate
    public void shouldReduceLeftOptionNonNil() {
        assertThat(of("a", "b", "c").reduceLeftOption((xs, x) -> xs + x)).isEqualTo(Option.some("abc"));
    }

    // -- reduceLeft

    @TestTemplate
    public void shouldThrowWhenReduceLeftNil() {
        assertThrows(NoSuchElementException.class, () -> this.<String>empty().reduceLeft((a, b) -> a + b));
    }

    @TestTemplate
    public void shouldThrowWhenReduceLeftNullOperator() {
        assertThrows(NullPointerException.class, () -> this.<String>empty().reduceLeft(null));
    }

    @TestTemplate
    public void shouldReduceLeftNonNil() {
        assertThat(of("a", "b", "c").reduceLeft((xs, x) -> xs + x)).isEqualTo("abc");
    }

    // -- reduceRightOption

    @TestTemplate
    public void shouldThrowWhenReduceRightOptionNil() {
        assertThat(this.<String>empty().reduceRightOption((a, b) -> a + b)).isSameAs(Option.none());
    }

    @TestTemplate
    public void shouldThrowWhenReduceRightOptionNullOperator() {
        assertThrows(NullPointerException.class, () -> this.<String>empty().reduceRightOption(null));
    }

    @TestTemplate
    public void shouldReduceRightOptionNonNil() {
        assertThat(of("a", "b", "c").reduceRightOption((x, xs) -> x + xs)).isEqualTo(Option.some("abc"));
    }

    // -- reduceRight

    @TestTemplate
    public void shouldThrowWhenReduceRightNil() {
        assertThrows(NoSuchElementException.class, () -> this.<String>empty().reduceRight((a, b) -> a + b));
    }

    @TestTemplate
    public void shouldThrowWhenReduceRightNullOperator() {
        assertThrows(NullPointerException.class, () -> this.<String>empty().reduceRight(null));
    }

    @TestTemplate
    public void shouldReduceRightNonNil() {
        assertThat(of("a", "b", "c").reduceRight((x, xs) -> x + xs)).isEqualTo("abc");
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
    public void shouldReplaceFirstOccurrenceOfNonNilUsingCurrNewWhenMultipleOccurrencesExist() {
        LazyList<Integer> testee = of(0, 1, 2, 1);
        LazyList<Integer> actual = testee.replace(1, 3);
        LazyList<Integer> expected = of(0, 3, 2, 1);
        assertThat(actual).isEqualTo(expected);
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
            LazyList<Integer> src = of(0, 1, 2);
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
            LazyList<Integer> src = of(0, 1, 2, 1);
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
        LazyList<Object> empty = empty();
        LazyList<Object> actual = empty.retainAll(of(1, 2, 3));
        if (useIsEqualToInsteadOfIsSameAs()) {
            assertThat(actual).isEqualTo(empty);
        } else {
            assertThat(actual).isSameAs(empty);
        }
    }

    @TestTemplate
    public void shouldRetainAllExistingElementsFromNonNil() {
        LazyList<Integer> src = of(1, 2, 3, 2, 1, 3);
        LazyList<Integer> expected = of(1, 2, 2, 1);
        LazyList<Integer> actual = src.retainAll(of(1, 2));
        assertThat(actual).isEqualTo(expected);
    }

    @TestTemplate
    public void shouldRetainAllElementsFromNonNil() {
        LazyList<Integer> src = of(1, 2, 1, 2, 2);
        LazyList<Integer> expected = of(1, 2, 1, 2, 2);
        LazyList<Integer> actual = src.retainAll(of(1, 2));
        if (useIsEqualToInsteadOfIsSameAs()) {
            assertThat(actual).isEqualTo(expected);
        } else {
            assertThat(actual).isSameAs(src);
        }
    }

    @TestTemplate
    public void shouldNotRetainAllNonExistingElementsFromNonNil() {
        LazyList<Integer> src = of(1, 2, 3);
        LazyList<Object> expected = empty();
        LazyList<Integer> actual = src.retainAll(of(4, 5));
        if (useIsEqualToInsteadOfIsSameAs()) {
            assertThat(actual).isEqualTo(expected);
        } else {
            assertThat(actual).isSameAs(expected);
        }
    }

    // -- scan, scanLeft, scanRight

    @TestTemplate
    public void shouldScanEmpty() {
        LazyList<Integer> testee = empty();
        LazyList<Integer> actual = testee.scan(0, (s1, s2) -> s1 + s2);
        assertThat(actual).isEqualTo(this.of(0));
    }

    @TestTemplate
    public void shouldScanLeftEmpty() {
        LazyList<Integer> testee = empty();
        LazyList<Integer> actual = testee.scanLeft(0, (s1, s2) -> s1 + s2);
        assertThat(actual).isEqualTo(of(0));
    }

    @TestTemplate
    public void shouldScanRightEmpty() {
        LazyList<Integer> testee = empty();
        LazyList<Integer> actual = testee.scanRight(0, (s1, s2) -> s1 + s2);
        assertThat(actual).isEqualTo(of(0));
    }

    @TestTemplate
    public void shouldScanNonEmpty() {
        LazyList<Integer> testee = of(1, 2, 3);
        LazyList<Integer> actual = testee.scan(0, (acc, s) -> acc + s);
        assertThat(actual).isEqualTo(of(0, 1, 3, 6));
    }

    @TestTemplate
    public void shouldScanLeftNonEmpty() {
        LazyList<Integer> testee = of(1, 2, 3);
        LazyList<String> actual = testee.scanLeft("x", (acc, i) -> acc + i);
        assertThat(actual).isEqualTo(of("x", "x1", "x12", "x123"));
    }

    @TestTemplate
    public void shouldScanRightNonEmpty() {
        LazyList<Integer> testee = of(1, 2, 3);
        LazyList<String> actual = testee.scanRight("x", (i, acc) -> acc + i);
        assertThat(actual).isEqualTo(of("x321", "x32", "x3", "x"));
    }

    @TestTemplate
    public void shouldScanWithNonComparable() {
        LazyList<NonComparable> testee = of(new NonComparable("a"));
        List<NonComparable> actual =
                List.ofAll(testee.scan(new NonComparable("x"), (u1, u2) -> new NonComparable(u1.value + u2.value)));
        List<NonComparable> expected = List.of("x", "xa").map(NonComparable::new);
        assertThat(actual).containsAll(expected);
        assertThat(expected).containsAll(actual);
        assertThat(actual.size()).isEqualTo(expected.size());
    }

    @TestTemplate
    public void shouldScanLeftWithNonComparable() {
        LazyList<NonComparable> testee = of(new NonComparable("a"));
        List<NonComparable> actual =
                List.ofAll(testee.scanLeft(new NonComparable("x"), (u1, u2) -> new NonComparable(u1.value + u2.value)));
        List<NonComparable> expected = List.of("x", "xa").map(NonComparable::new);
        assertThat(actual).containsAll(expected);
        assertThat(expected).containsAll(actual);
        assertThat(actual.size()).isEqualTo(expected.size());
    }

    @TestTemplate
    public void shouldScanRightWithNonComparable() {
        LazyList<NonComparable> testee = of(new NonComparable("a"));
        List<NonComparable> actual = List.ofAll(
                testee.scanRight(new NonComparable("x"), (u1, u2) -> new NonComparable(u1.value + u2.value)));
        List<NonComparable> expected = List.of("ax", "x").map(NonComparable::new);
        assertThat(actual).containsAll(expected);
        assertThat(expected).containsAll(actual);
        assertThat(actual.size()).isEqualTo(expected.size());
    }

    // -- slideBy(classifier)

    @TestTemplate
    public void shouldSlideNilByClassifier() {
        assertThat(empty().slideBy(Function.identity())).isEmpty();
    }

    @TestTemplate
    public void shouldTerminateSlideByClassifier() {
        assertTimeout(Duration.ofSeconds(1), () -> {
            AtomicInteger ai = new AtomicInteger(0);
            List<List<String>> expected = List.of(List.of("a", "-"), List.of("-"), List.of("d"));
            List<List<String>> actual = List.of("a", "-", "-", "d")
                    .slideBy(x -> x.equals("-") ? ai.getAndIncrement() : ai.get())
                    .toList();
            assertThat(actual).containsAll(expected);
            assertThat(expected).containsAll(actual);
        });
    }

    @TestTemplate
    public void shouldSlideSingularByClassifier() {
        List<LazyList<Integer>> actual = of(1).slideBy(Function.identity()).toList();
        List<LazyList<Integer>> expected = List.of(LazyList.of(1));
        assertThat(actual).isEqualTo(expected);
    }

    @TestTemplate
    public void shouldSlideNonNilByIdentityClassifier() {
        List<LazyList<Integer>> actual =
                of(1, 2, 3).slideBy(Function.identity()).toList();
        List<LazyList<Integer>> expected = List.of(LazyList.of(1), LazyList.of(2), LazyList.of(3));
        assertThat(actual).isEqualTo(expected);
    }

    @TestTemplate
    public void shouldSlideNonNilByConstantClassifier() {
        List<LazyList<Integer>> actual = of(1, 2, 3).slideBy(e -> "same").toList();
        List<LazyList<Integer>> expected = List.of(LazyList.of(1, 2, 3));
        assertThat(actual).isEqualTo(expected);
    }

    @TestTemplate
    public void shouldSlideNonNilBySomeClassifier() {
        List<LazyList<Integer>> actual =
                of(10, 20, 30, 42, 52, 60, 72).slideBy(e -> e % 10).toList();
        List<LazyList<Integer>> expected =
                List.of(LazyList.of(10, 20, 30), LazyList.of(42, 52), LazyList.of(60), LazyList.of(72));
        assertThat(actual).isEqualTo(expected);
    }

    @TestTemplate
    public void shouldSlideByClassifierReturningNull() {
        List<LazyList<Integer>> actual = of(1, 2, 3).slideBy(e -> null).toList();
        List<LazyList<Integer>> expected = List.of(LazyList.of(1, 2, 3));
        assertThat(actual).isEqualTo(expected);
    }

    // -- sliding(size)

    @TestTemplate
    public void shouldThrowWhenSlidingNilByZeroSize() {
        assertThrows(IllegalArgumentException.class, () -> empty().sliding(0));
    }

    @TestTemplate
    public void shouldThrowWhenSlidingNilByNegativeSize() {
        assertThrows(IllegalArgumentException.class, () -> empty().sliding(-1));
    }

    @TestTemplate
    public void shouldThrowWhenSlidingNonNilByZeroSize() {
        assertThrows(IllegalArgumentException.class, () -> of(1).sliding(0));
    }

    @TestTemplate
    public void shouldThrowWhenSlidingNonNilByNegativeSize() {
        assertThrows(IllegalArgumentException.class, () -> of(1).sliding(-1));
    }

    @TestTemplate
    public void shouldSlideNilBySize() {
        assertThat(empty().sliding(1)).isEmpty();
    }

    @TestTemplate
    public void shouldSlideNonNilBySize1() {
        List<LazyList<Integer>> actual = of(1, 2, 3).sliding(1).toList();
        List<LazyList<Integer>> expected = List.of(LazyList.of(1), LazyList.of(2), LazyList.of(3));
        assertThat(actual).isEqualTo(expected);
    }

    @TestTemplate
    public void shouldSlideNonNilBySize2() {
        List<LazyList<Integer>> actual = of(1, 2, 3, 4, 5).sliding(2).toList();
        List<LazyList<Integer>> expected =
                List.of(LazyList.of(1, 2), LazyList.of(2, 3), LazyList.of(3, 4), LazyList.of(4, 5));
        assertThat(actual).isEqualTo(expected);
    }

    // -- sliding(size, step)

    @TestTemplate
    public void shouldThrowWhenSlidingNilByPositiveStepAndNegativeSize() {
        assertThrows(IllegalArgumentException.class, () -> empty().sliding(-1, 1));
    }

    @TestTemplate
    public void shouldThrowWhenSlidingNilByNegativeStepAndNegativeSize() {
        assertThrows(IllegalArgumentException.class, () -> empty().sliding(-1, -1));
    }

    @TestTemplate
    public void shouldThrowWhenSlidingNilByNegativeStepAndPositiveSize() {
        assertThrows(IllegalArgumentException.class, () -> empty().sliding(1, -1));
    }

    @TestTemplate
    public void shouldSlideNilBySizeAndStep() {
        assertThat(empty().sliding(1, 1).isEmpty()).isTrue();
    }

    @TestTemplate
    public void shouldSlide5ElementsBySize2AndStep3() {
        List<LazyList<Integer>> actual = of(1, 2, 3, 4, 5).sliding(2, 3).toList();
        List<LazyList<Integer>> expected = List.of(LazyList.of(1, 2), LazyList.of(4, 5));
        assertThat(actual).isEqualTo(expected);
    }

    @TestTemplate
    public void shouldSlide5ElementsBySize2AndStep4() {
        List<LazyList<Integer>> actual = of(1, 2, 3, 4, 5).sliding(2, 4).toList();
        List<LazyList<Integer>> expected = List.of(LazyList.of(1, 2), LazyList.of(5));
        assertThat(actual).isEqualTo(expected);
    }

    @TestTemplate
    public void shouldSlide5ElementsBySize2AndStep5() {
        List<LazyList<Integer>> actual = of(1, 2, 3, 4, 5).sliding(2, 5).toList();
        List<LazyList<Integer>> expected = List.of(LazyList.of(1, 2));
        assertThat(actual).isEqualTo(expected);
    }

    @TestTemplate
    public void shouldSlide4ElementsBySize5AndStep3() {
        List<LazyList<Integer>> actual = of(1, 2, 3, 4).sliding(5, 3).toList();
        List<LazyList<Integer>> expected = List.of(LazyList.of(1, 2, 3, 4));
        assertThat(actual).isEqualTo(expected);
    }

    @TestTemplate
    public void shouldSlide7ElementsBySize1AndStep3() {
        List<LazyList<Integer>> actual = of(1, 2, 3, 4, 5, 6, 7).sliding(1, 3).toList();
        List<LazyList<Integer>> expected = List.of(LazyList.of(1), LazyList.of(4), LazyList.of(7));
        assertThat(actual).isEqualTo(expected);
    }

    @TestTemplate
    public void shouldSlide7ElementsBySize2AndStep3() {
        List<LazyList<Integer>> actual = of(1, 2, 3, 4, 5, 6, 7).sliding(2, 3).toList();
        List<LazyList<Integer>> expected = List.of(LazyList.of(1, 2), LazyList.of(4, 5), LazyList.of(7));
        assertThat(actual).isEqualTo(expected);
    }

    // -- span

    @TestTemplate
    public void shouldSpanNil() {
        assertThat(this.<Integer>empty().span(i -> i < 2)).isEqualTo(Tuple.of(empty(), empty()));
    }

    @TestTemplate
    public void shouldSpanNonNil() {
        assertThat(of(0, 1, 2, 3).span(i -> i < 2)).isEqualTo(Tuple.of(of(0, 1), of(2, 3)));
    }

    @TestTemplate
    public void shouldSpanAndNotTruncate() {
        assertThat(of(1, 1, 2, 2, 3, 3).span(x -> x % 2 == 1)).isEqualTo(Tuple.of(of(1, 1), of(2, 2, 3, 3)));
        assertThat(of(1, 1, 2, 2, 4, 4).span(x -> x == 1)).isEqualTo(Tuple.of(of(1, 1), of(2, 2, 4, 4)));
    }

    // -- take

    @TestTemplate
    public void shouldTakeNoneOnNil() {
        if (useIsEqualToInsteadOfIsSameAs()) {
            assertThat(empty().take(1)).isEqualTo(empty());
        } else {
            assertThat(empty().take(1)).isSameAs(empty());
        }
    }

    @TestTemplate
    public void shouldTakeNoneIfCountIsNegative() {
        if (useIsEqualToInsteadOfIsSameAs()) {
            assertThat(of(1, 2, 3).take(-1)).isEqualTo(empty());
        } else {
            assertThat(of(1, 2, 3).take(-1)).isSameAs(empty());
        }
    }

    @TestTemplate
    public void shouldTakeAsExpectedIfCountIsLessThanSize() {
        assertThat(of(1, 2, 3).take(2)).isEqualTo(of(1, 2));
    }

    @TestTemplate
    public void shouldTakeAllIfCountExceedsSize() {
        if (useIsEqualToInsteadOfIsSameAs()) {
            assertThat(of(1, 2, 3).take(4)).isEqualTo(of(1, 2, 3));
        } else {
            LazyList<Integer> t = of(1, 2, 3);
            assertThat(t.take(4)).isSameAs(t);
        }
    }

    // -- takeRight

    @TestTemplate
    public void shouldTakeRightNoneOnNil() {
        if (useIsEqualToInsteadOfIsSameAs()) {
            assertThat(empty().takeRight(1)).isEqualTo(empty());
        } else {
            assertThat(empty().takeRight(1)).isSameAs(empty());
        }
    }

    @TestTemplate
    public void shouldTakeRightNoneIfCountIsNegative() {
        if (useIsEqualToInsteadOfIsSameAs()) {
            assertThat(of(1, 2, 3).takeRight(-1)).isEqualTo(empty());
        } else {
            assertThat(of(1, 2, 3).takeRight(-1)).isSameAs(empty());
        }
    }

    @TestTemplate
    public void shouldTakeRightAsExpectedIfCountIsLessThanSize() {
        assertThat(of(1, 2, 3).takeRight(2)).isEqualTo(of(2, 3));
    }

    @TestTemplate
    public void shouldTakeRightAllIfCountExceedsSize() {
        if (useIsEqualToInsteadOfIsSameAs()) {
            assertThat(of(1, 2, 3).takeRight(4)).isEqualTo(of(1, 2, 3));
        } else {
            LazyList<Integer> t = of(1, 2, 3);
            assertThat(t.takeRight(4)).isSameAs(t);
        }
    }

    @TestTemplate
    public void shouldReturnSameInstanceIfTakeRightAll() {
        LazyList<?> t = of(1, 2, 3);
        assertThat(t.takeRight(3)).isEqualTo(t);
        assertThat(t.takeRight(4)).isEqualTo(t);
    }

    // -- takeUntil

    @TestTemplate
    public void shouldTakeUntilNoneOnNil() {
        if (useIsEqualToInsteadOfIsSameAs()) {
            assertThat(empty().takeUntil(x -> true)).isEqualTo(empty());
        } else {
            assertThat(empty().takeUntil(x -> true)).isSameAs(empty());
        }
    }

    @TestTemplate
    public void shouldTakeUntilAllOnFalseCondition() {
        LazyList<Integer> t = of(1, 2, 3);
        if (useIsEqualToInsteadOfIsSameAs()) {
            assertThat(of(1, 2, 3).takeUntil(x -> false)).isEqualTo(of(1, 2, 3));
        } else {
            assertThat(t.takeUntil(x -> false)).isSameAs(t);
        }
    }

    @TestTemplate
    public void shouldTakeUntilAllOnTrueCondition() {
        if (useIsEqualToInsteadOfIsSameAs()) {
            assertThat(of(1, 2, 3).takeUntil(x -> true)).isEqualTo(empty());
        } else {
            assertThat(of(1, 2, 3).takeUntil(x -> true)).isSameAs(empty());
        }
    }

    @TestTemplate
    public void shouldTakeUntilAsExpected() {
        assertThat(of(2, 4, 5, 6).takeUntil(x -> x % 2 != 0)).isEqualTo(of(2, 4));
    }

    @TestTemplate
    public void shouldReturnSameInstanceWhenEmptyTakeUntil() {
        LazyList<?> empty = empty();
        assertThat(empty.takeUntil(ignored -> false)).isSameAs(empty);
    }

    // -- takeWhile

    @TestTemplate
    public void shouldTakeWhileNoneOnNil() {
        if (useIsEqualToInsteadOfIsSameAs()) {
            assertThat(empty().takeWhile(x -> true)).isEqualTo(empty());
        } else {
            assertThat(empty().takeWhile(x -> true)).isSameAs(empty());
        }
    }

    @TestTemplate
    public void shouldTakeWhileAllOnFalseCondition() {
        if (useIsEqualToInsteadOfIsSameAs()) {
            assertThat(of(1, 2, 3).takeWhile(x -> false)).isEqualTo(empty());
        } else {
            assertThat(of(1, 2, 3).takeWhile(x -> false)).isSameAs(empty());
        }
    }

    @TestTemplate
    public void shouldTakeWhileAllOnTrueCondition() {
        LazyList<Integer> t = of(1, 2, 3);
        if (useIsEqualToInsteadOfIsSameAs()) {
            assertThat(of(1, 2, 3).takeWhile(x -> true)).isEqualTo(of(1, 2, 3));
        } else {
            assertThat(t.takeWhile(x -> true)).isSameAs(t);
        }
    }

    @TestTemplate
    public void shouldTakeWhileAsExpected() {
        assertThat(of(2, 4, 5, 6).takeWhile(x -> x % 2 == 0)).isEqualTo(of(2, 4));
    }

    @TestTemplate
    public void shouldReturnSameInstanceWhenEmptyTakeWhile() {
        LazyList<?> empty = empty();
        assertThat(empty.takeWhile(ignored -> false)).isSameAs(empty);
    }

    // -- tail

    @TestTemplate
    public void shouldThrowWhenTailOnNil() {
        assertThrows(UnsupportedOperationException.class, () -> empty().tail());
    }

    @TestTemplate
    public void shouldReturnTailOfNonNil() {
        assertThat(of(1, 2, 3).tail()).isEqualTo(of(2, 3));
    }

    // -- tailOption

    @TestTemplate
    public void shouldReturnNoneWhenCallingTailOptionOnNil() {
        assertThat(empty().tailOption().isEmpty()).isTrue();
    }

    @TestTemplate
    public void shouldReturnSomeTailWhenCallingTailOptionOnNonNil() {
        assertThat(of(1, 2, 3).tailOption()).isEqualTo(Option.some(of(2, 3)));
    }

    // -- unzip

    @TestTemplate
    public void shouldUnzipNil() {
        assertThat(empty().unzip(x -> Tuple.of(x, x))).isEqualTo(Tuple.of(empty(), empty()));
    }

    @TestTemplate
    public void shouldUnzipNonNil() {
        Tuple actual = of(0, 1).unzip(i -> Tuple.of(i, (char) ((short) 'a' + i)));
        Tuple expected = Tuple.of(of(0, 1), of('a', 'b'));
        assertThat(actual).isEqualTo(expected);
    }

    @TestTemplate
    public void shouldUnzip3Nil() {
        assertThat(empty().unzip3(x -> Tuple.of(x, x, x))).isEqualTo(Tuple.of(empty(), empty(), empty()));
    }

    @TestTemplate
    public void shouldUnzip3NonNil() {
        Tuple actual = of(0, 1).unzip3(i -> Tuple.of(i, (char) ((short) 'a' + i), (char) ((short) 'a' + i + 1)));
        Tuple expected = Tuple.of(of(0, 1), of('a', 'b'), of('b', 'c'));
        assertThat(actual).isEqualTo(expected);
    }

    // -- zip

    @TestTemplate
    public void shouldZipNils() {
        LazyList<?> actual = empty().zip(empty());
        assertThat(actual).isEmpty();
    }

    @TestTemplate
    public void shouldZipEmptyAndNonNil() {
        LazyList<?> actual = empty().zip(of(1));
        assertThat(actual).isEmpty();
    }

    @TestTemplate
    public void shouldZipNonEmptyAndNil() {
        LazyList<?> actual = of(1).zip(empty());
        assertThat(actual).isEmpty();
    }

    @TestTemplate
    public void shouldZipNonNilsIfThisIsSmaller() {
        LazyList<Tuple2<Integer, String>> actual = of(1, 2).zip(of("a", "b", "c"));
        @SuppressWarnings("unchecked")
        LazyList<Tuple2<Integer, String>> expected = of(Tuple.of(1, "a"), Tuple.of(2, "b"));
        assertThat(actual).isEqualTo(expected);
    }

    @TestTemplate
    public void shouldZipNonNilsIfThatIsSmaller() {
        LazyList<Tuple2<Integer, String>> actual = of(1, 2, 3).zip(of("a", "b"));
        @SuppressWarnings("unchecked")
        LazyList<Tuple2<Integer, String>> expected = of(Tuple.of(1, "a"), Tuple.of(2, "b"));
        assertThat(actual).isEqualTo(expected);
    }

    @TestTemplate
    public void shouldZipNonNilsOfSameSize() {
        LazyList<Tuple2<Integer, String>> actual = of(1, 2, 3).zip(of("a", "b", "c"));
        @SuppressWarnings("unchecked")
        LazyList<Tuple2<Integer, String>> expected = of(Tuple.of(1, "a"), Tuple.of(2, "b"), Tuple.of(3, "c"));
        assertThat(actual).isEqualTo(expected);
    }

    @TestTemplate
    @SuppressWarnings("unchecked")
    public void shouldZipWithNonNilsOfSameSize() {
        LazyList<Tuple2<Integer, String>> actual = of(1, 2, 3).zipWith(of("a", "b", "c"), Tuple::of);
        LazyList<Tuple2<Integer, String>> expected = of(Tuple.of(1, "a"), Tuple.of(2, "b"), Tuple.of(3, "c"));
        assertThat(actual).isEqualTo(expected);
    }

    @TestTemplate
    public void shouldThrowIfZipWithThatIsNull() {
        assertThrows(NullPointerException.class, () -> empty().zip(null));
    }

    // -- zipAll

    @TestTemplate
    public void shouldZipAllNils() {
        LazyList<?> actual = empty().zipAll(empty(), 0, 0);
        assertThat(actual).isEmpty();
    }

    @TestTemplate
    public void shouldZipAllEmptyAndNonNil() {
        LazyList<?> actual = empty().zipAll(of(1), 0, 0);
        LazyList<Tuple2<Object, Integer>> expected = of(Tuple.of(0, 1));
        assertThat(actual).isEqualTo(expected);
    }

    @TestTemplate
    public void shouldZipAllNonEmptyAndNil() {
        LazyList<?> actual = of(1).zipAll(empty(), 0, 0);
        LazyList<Tuple2<Integer, Object>> expected = of(Tuple.of(1, 0));
        assertThat(actual).isEqualTo(expected);
    }

    @TestTemplate
    public void shouldRejectNullZipAllFillValues() {
        assertThrows(NullPointerException.class, () -> empty().zipAll(of(1), null, 0));
        assertThrows(NullPointerException.class, () -> empty().zipAll(of(1), 0, null));
    }

    @TestTemplate
    public void shouldZipAllNonNilsIfThisIsSmaller() {
        LazyList<Tuple2<Integer, String>> actual = of(1, 2).zipAll(of("a", "b", "c"), 9, "z");
        @SuppressWarnings("unchecked")
        LazyList<Tuple2<Integer, String>> expected = of(Tuple.of(1, "a"), Tuple.of(2, "b"), Tuple.of(9, "c"));
        assertThat(actual).isEqualTo(expected);
    }

    @TestTemplate
    public void shouldZipAllNonNilsIfThatIsSmaller() {
        LazyList<Tuple2<Integer, String>> actual = of(1, 2, 3).zipAll(of("a", "b"), 9, "z");
        @SuppressWarnings("unchecked")
        LazyList<Tuple2<Integer, String>> expected = of(Tuple.of(1, "a"), Tuple.of(2, "b"), Tuple.of(3, "z"));
        assertThat(actual).isEqualTo(expected);
    }

    @TestTemplate
    public void shouldZipAllNonNilsOfSameSize() {
        LazyList<Tuple2<Integer, String>> actual = of(1, 2, 3).zipAll(of("a", "b", "c"), 9, "z");
        @SuppressWarnings("unchecked")
        LazyList<Tuple2<Integer, String>> expected = of(Tuple.of(1, "a"), Tuple.of(2, "b"), Tuple.of(3, "c"));
        assertThat(actual).isEqualTo(expected);
    }

    @TestTemplate
    public void shouldThrowIfZipAllWithThatIsNull() {
        assertThrows(NullPointerException.class, () -> empty().zipAll(null, null, null));
    }

    // -- zipWithIndex

    @TestTemplate
    public void shouldZipNilWithIndex() {
        assertThat(this.<String>empty().zipWithIndex()).isEqualTo(this.<Tuple2<String, Integer>>empty());
    }

    @TestTemplate
    public void shouldZipNonNilWithIndex() {
        LazyList<Tuple2<String, Integer>> actual = of("a", "b", "c").zipWithIndex();
        @SuppressWarnings("unchecked")
        LazyList<Tuple2<String, Integer>> expected = of(Tuple.of("a", 0), Tuple.of("b", 1), Tuple.of("c", 2));
        assertThat(actual).isEqualTo(expected);
    }

    @TestTemplate
    @SuppressWarnings("unchecked")
    public void shouldZipNonNilWithIndexWithMapper() {
        LazyList<Tuple2<String, Integer>> actual = of("a", "b", "c").zipWithIndex(Tuple::of);
        LazyList<Tuple2<String, Integer>> expected = of(Tuple.of("a", 0), Tuple.of("b", 1), Tuple.of("c", 2));
        assertThat(actual).isEqualTo(expected);
    }

    // -- toArray(IntFunction)

    @TestTemplate
    public void shouldConvertNilToJavaArray() {
        Integer[] actual = List.<Integer>empty().toArray(Integer[]::new);
        Integer[] expected = new Integer[] {};
        assertThat(actual).isEqualTo(expected);
    }

    @TestTemplate
    public void shouldConvertNonNilToJavaArray() {
        Integer[] array = of(1, 2).toArray(Integer[]::new);
        Integer[] expected = new Integer[] {1, 2};
        assertThat(array).isEqualTo(expected);
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
        int[] effect = {0};
        LazyList<Integer> actual = of(1).tap(i -> effect[0] = i);
        assertThat(actual).isEqualTo(of(1));
        assertThat(effect[0]).isEqualTo(1);
    }

    @TestTemplate
    public void shouldTapEveryElement() {
        int[] sum = {0};
        LazyList<Integer> actual = of(1, 2, 3).tap(i -> sum[0] += i);
        assertThat(actual).isEqualTo(of(1, 2, 3)); // consumes every element in the lazy case
        assertThat(sum[0]).isEqualTo(6);
    }

    @TestTemplate
    public void shouldReturnThisOnTapOfEagerCollection() {
        LazyList<Integer> testee = of(1, 2, 3);
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
        assertThrows(
                IllegalStateException.class,
                () -> of(1, 2).tap(i -> {
                            throw new IllegalStateException();
                        })
                        .size());
    }

    // -- collect(Collector)

    @TestTemplate
    public void shouldCollectWithACollector() {
        java.util.List<Integer> actual = of(1, 2, 3).collect(java.util.stream.Collectors.toList());
        assertThat(actual).containsExactlyInAnyOrder(1, 2, 3);
    }

    @TestTemplate
    public void shouldCollectWithSupplierAccumulatorAndCombiner() {
        ArrayList<Integer> actual = of(1, 2, 3).collect(ArrayList<Integer>::new, ArrayList::add, ArrayList::addAll);
        assertThat(actual).containsExactlyInAnyOrder(1, 2, 3);
    }

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
        assertThat(of(1, 5, 9).toLinkedMap(Function.identity(), Function.identity()))
                .isEqualTo(LinkedHashMap.of(1, 1, 5, 5, 9, 9));
    }

    @TestTemplate
    public void shouldConvertToSortedMap() {
        assertThat(of(9, 5, 1).toSortedMap(i -> Tuple.of(i, i))).isEqualTo(TreeMap.of(1, 1, 5, 5, 9, 9));
        assertThat(this.<Integer>empty().toSortedMap(i -> Tuple.of(i, i))).isEqualTo(TreeMap.empty());
    }

    @TestTemplate
    public void shouldConvertToSortedMapTwoFunctions() {
        assertThat(of(9, 5, 1).toSortedMap(Function.identity(), Function.identity()))
                .isEqualTo(TreeMap.of(1, 1, 5, 5, 9, 9));
    }

    @TestTemplate
    public void shouldConvertToSortedMapWithComparator() {
        Comparator<Integer> comparator = ((Comparator<Integer>) Integer::compareTo).reversed();
        assertThat(of(9, 5, 1).toSortedMap(comparator, i -> Tuple.of(i, i)))
                .isEqualTo(TreeMap.of(comparator, 9, 9, 5, 5, 1, 1));
    }

    @TestTemplate
    public void shouldConvertToSortedMapTwoFunctionsWithComparator() {
        Comparator<Integer> comparator = ((Comparator<Integer>) Integer::compareTo).reversed();
        assertThat(of(9, 5, 1).toSortedMap(comparator, Function.identity(), Function.identity()))
                .isEqualTo(TreeMap.of(comparator, 9, 9, 5, 5, 1, 1));
    }

    @TestTemplate
    public void shouldConvertToQueue() {
        assertThat(of(1, 2, 3).toQueue()).isEqualTo(Queue.of(1, 2, 3));
        assertThat(empty().toQueue()).isSameAs(Queue.empty());
    }

    @TestTemplate
    public void shouldConvertToLinkedSet() {
        LazyList<Integer> value = of(3, 7, 1, 15, 0);
        Set<Integer> set = value.toLinkedSet();
        List<Integer> itemsInOrder = true ? value.toList() : List.of(3, 7, 1, 15, 0);
        assertThat(set).isEqualTo(itemsInOrder.foldLeft(LinkedHashSet.empty(), LinkedHashSet::add));
        assertThat(empty().toLinkedSet()).isSameAs(LinkedHashSet.empty());
    }

    @TestTemplate
    public void shouldConvertToSortedSetWithoutComparatorOnComparable() {
        assertThat(of(3, 7, 1, 15, 0).toSortedSet()).isEqualTo(TreeSet.of(0, 1, 3, 7, 15));
    }

    @TestTemplate
    public void shouldThrowOnConvertToSortedSetWithoutComparatorOnNonComparable() {
        assertThrows(
                ClassCastException.class, () -> of(new Object(), new Object()).toSortedSet());
    }

    @TestTemplate
    public void shouldConvertToSortedSet() {
        Comparator<Integer> comparator = Comparator.comparingInt(Integer::bitCount);
        assertThat(of(3, 7, 1, 15, 0).toSortedSet(comparator.reversed()))
                .isEqualTo(TreeSet.of(comparator.reversed(), 0, 1, 3, 7, 15));
    }

    @TestTemplate
    public void shouldConvertToLazyList() {
        assertThat(of(1, 2, 3).toLazyList()).isEqualTo(LazyList.of(1, 2, 3));
        assertThat(empty().toLazyList()).isSameAs(LazyList.empty());
    }

    // -- the range factories

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
            assertThat(rangeClosedBy((char) (Character.MAX_VALUE - 2), Character.MAX_VALUE, 3))
                    .isEqualTo(of((char) (Character.MAX_VALUE - 2)));
            assertThat(rangeClosedBy((char) (Character.MAX_VALUE - 3), Character.MAX_VALUE, 3))
                    .isEqualTo(of((char) (Character.MAX_VALUE - 3), Character.MAX_VALUE));
            assertThat(rangeClosedBy('c', 'a', -1)).isEqualTo(of('c', 'b', 'a'));
            assertThat(rangeClosedBy('e', 'a', -2)).isEqualTo(of('e', 'c', 'a'));
            assertThat(rangeClosedBy('e', (char) ('a' - 1), -2)).isEqualTo(of('e', 'c', 'a'));
            assertThat(rangeClosedBy((char) (Character.MIN_VALUE + 2), Character.MIN_VALUE, -3))
                    .isEqualTo(of((char) (Character.MIN_VALUE + 2)));
            assertThat(rangeClosedBy((char) (Character.MIN_VALUE + 3), Character.MIN_VALUE, -3))
                    .isEqualTo(of((char) (Character.MIN_VALUE + 3), Character.MIN_VALUE));

            // double
            assertThat(rangeClosedBy(1.0, 3.0, 1.0)).isEqualTo(of(1.0, 2.0, 3.0));
            assertThat(rangeClosedBy(1.0, 5.0, 2.0)).isEqualTo(of(1.0, 3.0, 5.0));
            assertThat(rangeClosedBy(1.0, 6.0, 2.0)).isEqualTo(of(1.0, 3.0, 5.0));
            assertThat(rangeClosedBy(Double.MAX_VALUE - 2.0E307, Double.MAX_VALUE, 3.0E307))
                    .isEqualTo(of(Double.MAX_VALUE - 2.0E307));
            assertThat(rangeClosedBy(3.0, 1.0, -1.0)).isEqualTo(of(3.0, 2.0, 1.0));
            assertThat(rangeClosedBy(5.0, 1.0, -2.0)).isEqualTo(of(5.0, 3.0, 1.0));
            assertThat(rangeClosedBy(5.0, 0.0, -2.0)).isEqualTo(of(5.0, 3.0, 1.0));
            assertThat(rangeClosedBy(-Double.MAX_VALUE + 2.0E307, -Double.MAX_VALUE, -3.0E307))
                    .isEqualTo(of(-Double.MAX_VALUE + 2.0E307));

            // int
            assertThat(rangeClosedBy(1, 3, 1)).isEqualTo(of(1, 2, 3));
            assertThat(rangeClosedBy(1, 5, 2)).isEqualTo(of(1, 3, 5));
            assertThat(rangeClosedBy(1, 6, 2)).isEqualTo(of(1, 3, 5));
            assertThat(rangeClosedBy(Integer.MAX_VALUE - 2, Integer.MAX_VALUE, 3))
                    .isEqualTo(of(Integer.MAX_VALUE - 2));
            assertThat(rangeClosedBy(Integer.MAX_VALUE - 3, Integer.MAX_VALUE, 3))
                    .isEqualTo(of(Integer.MAX_VALUE - 3, Integer.MAX_VALUE));
            assertThat(rangeClosedBy(3, 1, -1)).isEqualTo(of(3, 2, 1));
            assertThat(rangeClosedBy(5, 1, -2)).isEqualTo(of(5, 3, 1));
            assertThat(rangeClosedBy(5, 0, -2)).isEqualTo(of(5, 3, 1));
            assertThat(rangeClosedBy(Integer.MIN_VALUE + 2, Integer.MIN_VALUE, -3))
                    .isEqualTo(of(Integer.MIN_VALUE + 2));
            assertThat(rangeClosedBy(Integer.MIN_VALUE + 3, Integer.MIN_VALUE, -3))
                    .isEqualTo(of(Integer.MIN_VALUE + 3, Integer.MIN_VALUE));

            // long
            assertThat(rangeClosedBy(1L, 3L, 1)).isEqualTo(of(1L, 2L, 3L));
            assertThat(rangeClosedBy(1L, 5L, 2)).isEqualTo(of(1L, 3L, 5L));
            assertThat(rangeClosedBy(1L, 6L, 2)).isEqualTo(of(1L, 3L, 5L));
            assertThat(rangeClosedBy(Long.MAX_VALUE - 2, Long.MAX_VALUE, 3)).isEqualTo(of(Long.MAX_VALUE - 2));
            assertThat(rangeClosedBy(Long.MAX_VALUE - 3, Long.MAX_VALUE, 3))
                    .isEqualTo(of(Long.MAX_VALUE - 3, Long.MAX_VALUE));
            assertThat(rangeClosedBy(3L, 1L, -1)).isEqualTo(of(3L, 2L, 1L));
            assertThat(rangeClosedBy(5L, 1L, -2)).isEqualTo(of(5L, 3L, 1L));
            assertThat(rangeClosedBy(5L, 0L, -2)).isEqualTo(of(5L, 3L, 1L));
            assertThat(rangeClosedBy(Long.MIN_VALUE + 2, Long.MIN_VALUE, -3)).isEqualTo(of(Long.MIN_VALUE + 2));
            assertThat(rangeClosedBy(Long.MIN_VALUE + 3, Long.MIN_VALUE, -3))
                    .isEqualTo(of(Long.MIN_VALUE + 3, Long.MIN_VALUE));
        }

        @Test
        public void shouldCreateRangeClosedByWhereFromAndToEqualMIN_VALUE() {

            // char
            assertThat(rangeClosedBy(Character.MIN_VALUE, Character.MIN_VALUE, 1))
                    .isEqualTo(of(Character.MIN_VALUE));
            assertThat(rangeClosedBy(Character.MIN_VALUE, Character.MIN_VALUE, 3))
                    .isEqualTo(of(Character.MIN_VALUE));
            assertThat(rangeClosedBy(Character.MIN_VALUE, Character.MIN_VALUE, -1))
                    .isEqualTo(of(Character.MIN_VALUE));
            assertThat(rangeClosedBy(Character.MIN_VALUE, Character.MIN_VALUE, -3))
                    .isEqualTo(of(Character.MIN_VALUE));

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
            assertThat(rangeClosedBy(Character.MAX_VALUE, Character.MAX_VALUE, 1))
                    .isEqualTo(of(Character.MAX_VALUE));
            assertThat(rangeClosedBy(Character.MAX_VALUE, Character.MAX_VALUE, 3))
                    .isEqualTo(of(Character.MAX_VALUE));
            assertThat(rangeClosedBy(Character.MAX_VALUE, Character.MAX_VALUE, -1))
                    .isEqualTo(of(Character.MAX_VALUE));
            assertThat(rangeClosedBy(Character.MAX_VALUE, Character.MAX_VALUE, -3))
                    .isEqualTo(of(Character.MAX_VALUE));

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
            assertThat(rangeClosedBy(Integer.MIN_VALUE, Integer.MIN_VALUE + 2, 1))
                    .isEqualTo(of(Integer.MIN_VALUE, Integer.MIN_VALUE + 1, Integer.MIN_VALUE + 2));
            assertThat(rangeClosedBy(Integer.MAX_VALUE, Integer.MAX_VALUE - 2, -1))
                    .isEqualTo(of(Integer.MAX_VALUE, Integer.MAX_VALUE - 1, Integer.MAX_VALUE - 2));
            assertThat(rangeClosedBy(Integer.MIN_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE))
                    .isEqualTo(of(Integer.MIN_VALUE, -1, Integer.MAX_VALUE - 1));
            assertThat(rangeClosedBy(Integer.MAX_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE))
                    .isEqualTo(of(Integer.MAX_VALUE, -1));
            assertThat(rangeClosedBy(Integer.MIN_VALUE, Integer.MIN_VALUE + 1, 5))
                    .isEqualTo(of(Integer.MIN_VALUE));

            // long
            assertThat(rangeClosedBy(Long.MIN_VALUE, Long.MIN_VALUE + 2, 1L))
                    .isEqualTo(of(Long.MIN_VALUE, Long.MIN_VALUE + 1, Long.MIN_VALUE + 2));
            assertThat(rangeClosedBy(Long.MAX_VALUE, Long.MAX_VALUE - 2, -1L))
                    .isEqualTo(of(Long.MAX_VALUE, Long.MAX_VALUE - 1, Long.MAX_VALUE - 2));
            assertThat(rangeClosedBy(Long.MIN_VALUE, Long.MAX_VALUE, Long.MAX_VALUE))
                    .isEqualTo(of(Long.MIN_VALUE, -1L, Long.MAX_VALUE - 1));
            assertThat(rangeClosedBy(Long.MAX_VALUE, Long.MIN_VALUE, Long.MIN_VALUE))
                    .isEqualTo(of(Long.MAX_VALUE, -1L));
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
            assertThat(rangeBy((char) (Character.MAX_VALUE - 3), Character.MAX_VALUE, 3))
                    .isEqualTo(of((char) (Character.MAX_VALUE - 3)));
            assertThat(rangeBy((char) (Character.MAX_VALUE - 4), Character.MAX_VALUE, 3))
                    .isEqualTo(of((char) (Character.MAX_VALUE - 4), (char) (Character.MAX_VALUE - 1)));
            assertThat(rangeBy((char) (Character.MIN_VALUE + 3), Character.MIN_VALUE, -3))
                    .isEqualTo(of((char) (Character.MIN_VALUE + 3)));
            assertThat(rangeBy((char) (Character.MIN_VALUE + 4), Character.MIN_VALUE, -3))
                    .isEqualTo(of((char) (Character.MIN_VALUE + 4), (char) (Character.MIN_VALUE + 1)));

            // double
            assertThat(rangeBy(1.0, 3.0, 1.0)).isEqualTo(of(1.0, 2.0));
            assertThat(rangeBy(1.0, 4.0, 2.0)).isEqualTo(of(1.0, 3.0));
            assertThat(rangeBy(3.0, 1.0, -1.0)).isEqualTo(of(3.0, 2.0));
            assertThat(rangeBy(4.0, 1.0, -2.0)).isEqualTo(of(4.0, 2.0));
            assertThat(rangeBy(Double.MAX_VALUE - 3.0E307, Double.MAX_VALUE, 3.0E307))
                    .isEqualTo(of(Double.MAX_VALUE - 3.0E307));
            assertThat(rangeBy(-Double.MAX_VALUE + 3.0E307, -Double.MAX_VALUE, -3.0E307))
                    .isEqualTo(of(-Double.MAX_VALUE + 3.0E307));

            // int
            assertThat(rangeBy(1, 3, 1)).isEqualTo(of(1, 2));
            assertThat(rangeBy(1, 4, 2)).isEqualTo(of(1, 3));
            assertThat(rangeBy(3, 1, -1)).isEqualTo(of(3, 2));
            assertThat(rangeBy(4, 1, -2)).isEqualTo(of(4, 2));
            assertThat(rangeBy(Integer.MAX_VALUE - 3, Integer.MAX_VALUE, 3)).isEqualTo(of(Integer.MAX_VALUE - 3));
            assertThat(rangeBy(Integer.MAX_VALUE - 4, Integer.MAX_VALUE, 3))
                    .isEqualTo(of(Integer.MAX_VALUE - 4, Integer.MAX_VALUE - 1));
            assertThat(rangeBy(Integer.MIN_VALUE + 3, Integer.MIN_VALUE, -3)).isEqualTo(of(Integer.MIN_VALUE + 3));
            assertThat(rangeBy(Integer.MIN_VALUE + 4, Integer.MIN_VALUE, -3))
                    .isEqualTo(of(Integer.MIN_VALUE + 4, Integer.MIN_VALUE + 1));

            // long
            assertThat(rangeBy(1L, 3L, 1L)).isEqualTo(of(1L, 2L));
            assertThat(rangeBy(1L, 4L, 2L)).isEqualTo(of(1L, 3L));
            assertThat(rangeBy(3L, 1L, -1L)).isEqualTo(of(3L, 2L));
            assertThat(rangeBy(4L, 1L, -2L)).isEqualTo(of(4L, 2L));
            assertThat(rangeBy(Long.MAX_VALUE - 3, Long.MAX_VALUE, 3)).isEqualTo(of(Long.MAX_VALUE - 3));
            assertThat(rangeBy(Long.MAX_VALUE - 4, Long.MAX_VALUE, 3))
                    .isEqualTo(of(Long.MAX_VALUE - 4, Long.MAX_VALUE - 1));
            assertThat(rangeBy(Long.MIN_VALUE + 3, Long.MIN_VALUE, -3)).isEqualTo(of(Long.MIN_VALUE + 3));
            assertThat(rangeBy(Long.MIN_VALUE + 4, Long.MIN_VALUE, -3))
                    .isEqualTo(of(Long.MIN_VALUE + 4, Long.MIN_VALUE + 1));
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
            assertThat(rangeBy(Integer.MIN_VALUE, Integer.MIN_VALUE + 2, 1))
                    .isEqualTo(of(Integer.MIN_VALUE, Integer.MIN_VALUE + 1));
            assertThat(rangeBy(Integer.MAX_VALUE, Integer.MAX_VALUE - 2, -1))
                    .isEqualTo(of(Integer.MAX_VALUE, Integer.MAX_VALUE - 1));

            // long
            assertThat(rangeBy(5L, Long.MIN_VALUE, 1L)).isEmpty();
            assertThat(rangeBy(5L, Long.MAX_VALUE, -1L)).isEmpty();
            assertThat(rangeBy(Long.MIN_VALUE, Long.MIN_VALUE + 2, 1L))
                    .isEqualTo(of(Long.MIN_VALUE, Long.MIN_VALUE + 1));
            assertThat(rangeBy(Long.MAX_VALUE, Long.MAX_VALUE - 2, -1L))
                    .isEqualTo(of(Long.MAX_VALUE, Long.MAX_VALUE - 1));
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

    @Nested
    class LazyWindowsAndProductsTests {

        /** An infinite LazyList counting how many of its elements have been forced. */
        private LazyList<Integer> counted(AtomicInteger forced) {
            return LazyList.continually(forced::incrementAndGet);
        }

        @Test
        public void shouldGroupIntoLazyListsOfLazyLists() {
            LazyList<LazyList<Integer>> groups = of(1, 2, 3, 4, 5).grouped(2);
            assertThat(groups).isInstanceOf(LazyList.class);
            assertThat(groups).isEqualTo(LazyList.of(LazyList.of(1, 2), LazyList.of(3, 4), LazyList.of(5)));
            assertThat(groups.head()).isInstanceOf(LazyList.class);
        }

        @Test
        public void shouldSlideIntoLazyListsOfLazyLists() {
            assertThat(of(1, 2, 3, 4).sliding(3))
                    .isInstanceOf(LazyList.class)
                    .isEqualTo(LazyList.of(LazyList.of(1, 2, 3), LazyList.of(2, 3, 4)));
            assertThat(of(1, 2, 3, 4, 5).sliding(2, 3)).isEqualTo(LazyList.of(LazyList.of(1, 2), LazyList.of(4, 5)));
            assertThat(of(1, 2, 3, 4, 5).sliding(2, 4)).isEqualTo(LazyList.of(LazyList.of(1, 2), LazyList.of(5)));
            assertThat(of(1, 2).sliding(5)).isEqualTo(LazyList.of(LazyList.of(1, 2)));
            assertThat(of(1, 2, 3, 4).sliding(3).head()).isInstanceOf(LazyList.class);
        }

        @Test
        public void shouldSlideByIntoLazyListsOfLazyLists() {
            LazyList<LazyList<Integer>> runs = of(1, 2, 3, 10, 12, 5, 7, 20, 29).slideBy(x -> x / 10);
            assertThat(runs).isInstanceOf(LazyList.class);
            assertThat(runs)
                    .isEqualTo(LazyList.of(
                            LazyList.of(1, 2, 3), LazyList.of(10, 12), LazyList.of(5, 7), LazyList.of(20, 29)));
            assertThat(runs.head()).isInstanceOf(LazyList.class);
        }

        @Test
        public void shouldHaveNoWindowOnTheEmptyLazyList() {
            assertThat(empty().grouped(1)).isEqualTo(LazyList.empty());
            assertThat(empty().sliding(1)).isEqualTo(LazyList.empty());
            assertThat(empty().sliding(1, 2)).isEqualTo(LazyList.empty());
            assertThat(empty().slideBy(Function.identity())).isEqualTo(LazyList.empty());
            assertThat(empty().crossProduct()).isEqualTo(LazyList.empty());
            assertThat(empty().crossProduct(2)).isEqualTo(LazyList.empty());
            assertThat(empty().crossProduct(of(1))).isEqualTo(LazyList.empty());
            assertThat(empty().crossProduct(0)).isEqualTo(LazyList.of(LazyList.empty()));
        }

        @Test
        public void shouldRejectANonPositiveSizeOrStep() {
            assertThrows(IllegalArgumentException.class, () -> of(1).grouped(0));
            assertThrows(IllegalArgumentException.class, () -> of(1).sliding(0));
            assertThrows(IllegalArgumentException.class, () -> of(1).sliding(1, 0));
            assertThrows(IllegalArgumentException.class, () -> of(1).sliding(-1, 1));
            assertThrows(NullPointerException.class, () -> of(1).slideBy(null));
        }

        @Test
        public void shouldGroupAnInfiniteLazyList() {
            assertThat(LazyList.from(1).grouped(3).take(2))
                    .isEqualTo(LazyList.of(LazyList.of(1, 2, 3), LazyList.of(4, 5, 6)));
            assertThat(LazyList.from(1).grouped(3).head()).isInstanceOf(LazyList.class);
        }

        @Test
        public void shouldSlideAnInfiniteLazyList() {
            assertThat(LazyList.from(1).sliding(2, 3).take(3))
                    .isEqualTo(LazyList.of(LazyList.of(1, 2), LazyList.of(4, 5), LazyList.of(7, 8)));
            assertThat(LazyList.from(1).sliding(3).take(2))
                    .isEqualTo(LazyList.of(LazyList.of(1, 2, 3), LazyList.of(2, 3, 4)));
            assertThat(LazyList.from(1).slideBy(i -> i / 3).take(2))
                    .isEqualTo(LazyList.of(LazyList.of(1, 2), LazyList.of(3, 4, 5)));
        }

        @Test
        public void shouldForceOnlyTheFirstGroupsWhenGrouping() {
            AtomicInteger forced = new AtomicInteger();
            LazyList<Integer> stream = counted(forced);
            assertThat(forced.get()).isZero(); // creating a LazyList evaluates nothing
            LazyList<LazyList<Integer>> groups = stream.grouped(3);
            // the call forces nothing: the first group is a lazy view of the source
            assertThat(forced.get()).isZero();
            assertThat(groups.head()).isEqualTo(LazyList.of(1, 2, 3)); // consuming the group forces its elements
            assertThat(forced.get()).isEqualTo(3);
            // reaching the tail forces one element past the group, to know whether another group follows
            assertThat(groups.tail().head()).isEqualTo(LazyList.of(4, 5, 6));
            assertThat(forced.get()).isEqualTo(6);
            assertThat(groups.take(4).last()).isEqualTo(LazyList.of(10, 11, 12));
            assertThat(forced.get()).isEqualTo(12);
        }

        @Test
        public void shouldForceOnlyTheFirstWindowsWhenSliding() {
            AtomicInteger forced = new AtomicInteger();
            LazyList<LazyList<Integer>> windows = counted(forced).sliding(2, 3);
            assertThat(forced.get()).isZero(); // the call forces nothing
            assertThat(windows.head()).isEqualTo(LazyList.of(1, 2));
            assertThat(forced.get()).isEqualTo(2);
            // reaching the tail forces the skipped element and the head of the next window
            assertThat(windows.tail().head()).isEqualTo(LazyList.of(4, 5));
            assertThat(forced.get()).isEqualTo(5);
        }

        @Test
        public void shouldForceOnlyTheFirstRunWhenSlidingBy() {
            AtomicInteger forced = new AtomicInteger();
            LazyList<LazyList<Integer>> runs = counted(forced).slideBy(i -> (i - 1) / 3);
            assertThat(forced.get()).isZero(); // the call forces nothing
            assertThat(runs.head()).isEqualTo(LazyList.of(1, 2, 3));
            assertThat(forced.get()).isEqualTo(4); // the first run of three, and the element that ends it
            assertThat(runs.tail().head()).isEqualTo(LazyList.of(4, 5, 6));
            assertThat(forced.get()).isEqualTo(7);
        }

        @Test
        public void shouldBuildTheCrossProductLazily() {
            AtomicInteger forced = new AtomicInteger();
            LazyList<Tuple2<Integer, Integer>> pairs = counted(forced).crossProduct(LazyList.from(1));
            assertThat(forced.get()).isZero(); // the call forces nothing
            assertThat(pairs.head()).isEqualTo(Tuple.of(1, 1));
            assertThat(forced.get()).isEqualTo(1); // one pair: the head of this LazyList and the head of that
            assertThat(pairs.take(3)).isEqualTo(LazyList.of(Tuple.of(1, 1), Tuple.of(1, 2), Tuple.of(1, 3)));
            assertThat(forced.get()).isEqualTo(1); // the right-hand side varies fastest, the left-hand head is reused
            AtomicInteger forcedForThePower = new AtomicInteger();
            LazyList<LazyList<Integer>> power = counted(forcedForThePower).crossProduct(2);
            assertThat(forcedForThePower.get()).isZero();
            assertThat(power.take(2)).isEqualTo(LazyList.of(LazyList.of(1, 1), LazyList.of(1, 2)));
            assertThat(forcedForThePower.get()).isEqualTo(2);
            assertThat(LazyList.from(1).crossProduct(LazyList.from(1)).take(5))
                    .isEqualTo(LazyList.of(
                            Tuple.of(1, 1), Tuple.of(1, 2), Tuple.of(1, 3), Tuple.of(1, 4), Tuple.of(1, 5)));
        }

        @Test
        public void shouldBuildTheCrossProductSquareAndPowerLazily() {
            assertThat(LazyList.from(1).crossProduct().take(3))
                    .isEqualTo(LazyList.of(Tuple.of(1, 1), Tuple.of(1, 2), Tuple.of(1, 3)));
            assertThat(LazyList.from(1).crossProduct(2).take(3))
                    .isEqualTo(LazyList.of(LazyList.of(1, 1), LazyList.of(1, 2), LazyList.of(1, 3)));
            assertThat(LazyList.from(1).crossProduct(2).head()).isInstanceOf(LazyList.class);
            assertThat(of(1, 2).crossProduct(2))
                    .isInstanceOf(LazyList.class)
                    .isEqualTo(LazyList.of(LazyList.of(1, 1), LazyList.of(1, 2), LazyList.of(2, 1), LazyList.of(2, 2)));
            assertThat(of(1, 2).crossProduct()).isInstanceOf(LazyList.class);
            assertThat(of(1, 2).crossProduct(List.of('a')))
                    .isInstanceOf(LazyList.class)
                    .isEqualTo(LazyList.of(Tuple.of(1, 'a'), Tuple.of(2, 'a')));
        }

        @Test
        public void shouldMemoiseTheCrossProductArgument() {
            AtomicInteger walks = new AtomicInteger();
            Iterable<Character> that = () -> {
                walks.incrementAndGet();
                return java.util.List.of('a', 'b').iterator();
            };
            LazyList<Tuple2<Integer, Character>> product = of(1, 2, 3).crossProduct(that);
            assertThat(product.toList())
                    .isEqualTo(List.of(
                            Tuple.of(1, 'a'),
                            Tuple.of(1, 'b'),
                            Tuple.of(2, 'a'),
                            Tuple.of(2, 'b'),
                            Tuple.of(3, 'a'),
                            Tuple.of(3, 'b')));
            assertThat(walks.get()).isEqualTo(1);
        }
    }

    // -- one-shot arguments (a java.util.stream can be iterated once): every argument is read exactly once

    @Nested
    class OneShotArgumentTests {
        private <T> Iterable<T> oneShot(T... elements) {
            return java.util.stream.Stream.of(elements)::iterator;
        }

        @Test
        public void shouldAppendAllFromAOneShotArgument() {
            assertThat(of(1).appendAll(oneShot(2, 3))).isEqualTo(of(1, 2, 3));
            assertThat(of(1).appendAll(oneShot())).isEqualTo(of(1));
            assertThat(empty().appendAll(oneShot(2, 3))).isEqualTo(of(2, 3));
        }

        @Test
        public void shouldPrependAllFromAOneShotArgument() {
            assertThat(of(3).prependAll(oneShot(1, 2))).isEqualTo(of(1, 2, 3));
            assertThat(of(3).prependAll(oneShot())).isEqualTo(of(3));
            assertThat(empty().prependAll(oneShot(1, 2))).isEqualTo(of(1, 2));
        }

        @Test
        public void shouldInsertAllFromAOneShotArgument() {
            assertThat(of(1, 4).insertAll(1, oneShot(2, 3))).isEqualTo(of(1, 2, 3, 4));
            assertThat(of(1, 4).insertAll(0, oneShot(2, 3))).isEqualTo(of(2, 3, 1, 4));
            assertThat(of(1, 4).insertAll(2, oneShot(2, 3))).isEqualTo(of(1, 4, 2, 3));
        }

        @Test
        public void shouldPatchFromAOneShotArgument() {
            assertThat(of(1, 2, 3, 4).patch(1, oneShot(9, 8), 2)).isEqualTo(of(1, 9, 8, 4));
            assertThat(of(1, 2, 3, 4).patch(0, oneShot(9), 0)).isEqualTo(of(9, 1, 2, 3, 4));
        }

        @Test
        public void shouldFindTheLastIndexOfAOneShotSlice() {
            assertThat(of(1, 2, 3, 4).lastIndexOfSlice(oneShot(2, 3))).isEqualTo(1);
            assertThat(of(1, 2, 3, 2, 3).lastIndexOfSlice(oneShot(2, 3))).isEqualTo(3);
            assertThat(of(1, 2, 3, 4).lastIndexOfSlice(oneShot(2, 3), 0)).isEqualTo(-1);
            assertThat(of(1, 2, 3, 4).lastIndexOfSlice(oneShot())).isEqualTo(4);
            assertThat(empty().lastIndexOfSlice(oneShot(2, 3))).isEqualTo(-1);
            assertThat(of(1, 2, 3, 4).indexOfSlice(oneShot(2, 3))).isEqualTo(1);
        }

        @Test
        public void shouldConcatOneShotIterables() {
            Iterable<Iterable<Integer>> outer =
                    java.util.stream.Stream.<Iterable<Integer>>of(oneShot(1, 2), oneShot(3))::iterator;
            assertThat(LazyList.concat(outer)).isEqualTo(of(1, 2, 3));
            assertThat(LazyList.concat(java.util.stream.Stream.<Iterable<Integer>>empty()::iterator))
                    .isEmpty();
            assertThat(LazyList.concat(oneShot(1, 2), oneShot(3))).isEqualTo(of(1, 2, 3));
        }

        @Test
        public void shouldAppendAOneShotArgumentToAnInfiniteLazyList() {
            // the argument is not probed before the receiver is consumed: a memoised copy answers the emptiness
            assertThat(LazyList.from(1).appendAll(oneShot(0)).take(3)).isEqualTo(of(1, 2, 3));
        }
    }

    // -- partitionMap and flatten, at the empty/1/32/33 boundaries

    @SafeVarargs
    private static <T> Iterable<T> oneShotOf(T... elements) {
        // a java.util.stream can be iterated once: a second iterator() throws IllegalStateException
        return java.util.stream.Stream.of(elements)::iterator;
    }

    @Nested
    class PartitionMapTests {

        @Test
        public void shouldPartitionMapLikePartitionAtEveryBoundary() {
            for (int n : new int[] {0, 1, 32, 33}) {
                LazyList<Integer> source = LazyList.range(0, n);
                Tuple2<LazyList<String>, LazyList<Integer>> actual =
                        source.partitionMap(i -> i % 3 == 0 ? Either.left("e" + i) : Either.right(i));
                Tuple2<LazyList<Integer>, LazyList<Integer>> expected = source.partition(i -> i % 3 == 0);
                assertThat(actual._1()).isEqualTo(expected._1().map(i -> "e" + i));
                assertThat(actual._2()).isEqualTo(expected._2());
                assertThat(actual._1().size() + actual._2().size()).isEqualTo(n);
                assertThat(source.partitionMap(i -> Either.<Integer, String>left(i)))
                        .isEqualTo(Tuple.of(source, LazyList.empty()));
                assertThat(source.partitionMap(i -> Either.<String, Integer>right(i)))
                        .isEqualTo(Tuple.of(LazyList.empty(), source));
            }
        }

        @Test
        public void shouldKeepTheSourceOrderOnEachSide() {
            Tuple2<LazyList<Integer>, LazyList<String>> actual = LazyList.of(5, 2, 8, 1, 9, 4)
                    .partitionMap(i -> i % 2 == 0 ? Either.left(i) : Either.right("o" + i));
            assertThat(actual).isEqualTo(Tuple.of(LazyList.of(2, 8, 4), LazyList.of("o5", "o1", "o9")));
        }

        @Test
        public void shouldCallTheFunctionOncePerElementInOrder() {
            for (int n : new int[] {0, 1, 32, 33}) {
                java.util.List<Integer> seen = new ArrayList<>();
                Tuple2<LazyList<Integer>, LazyList<Integer>> sides = LazyList.range(0, n)
                        .partitionMap(i -> {
                            seen.add(i);
                            return i % 2 == 0 ? Either.left(i) : Either.right(i);
                        });
                // both sides forced to the end, each in turn: every element was classified once, in order
                sides._1().size();
                sides._2().size();
                sides._1().size();
                assertThat(List.ofAll(seen)).isEqualTo(List.range(0, n));
            }
        }

        @Test
        public void shouldBeLazyAndMemoiseTheResultsOfTheFunction() {
            java.util.List<Integer> seen = new ArrayList<>();
            Tuple2<LazyList<Integer>, LazyList<String>> sides = LazyList.from(0).partitionMap(i -> {
                seen.add(i);
                return i % 3 == 0 ? Either.left(i) : Either.right("r" + i);
            });
            // nothing is evaluated before a side is read
            assertThat(seen).isEmpty();
            assertThat(sides._1().take(3).toList()).isEqualTo(List.of(0, 3, 6));
            assertThat(seen).containsExactly(0, 1, 2, 3, 4, 5, 6);
            assertThat(sides._2().take(4).toList()).isEqualTo(List.of("r1", "r2", "r4", "r5"));
            assertThat(seen).containsExactly(0, 1, 2, 3, 4, 5, 6);
            assertThat(sides._2().take(5).toList()).isEqualTo(List.of("r1", "r2", "r4", "r5", "r7"));
            assertThat(sides._1().take(3).toList()).isEqualTo(List.of(0, 3, 6));
            assertThat(seen).containsExactly(0, 1, 2, 3, 4, 5, 6, 7);
        }

        @Test
        public void shouldPartitionAnInfiniteLazyList() {
            Tuple2<LazyList<Integer>, LazyList<Integer>> sides =
                    LazyList.from(0).partitionMap(i -> i % 2 == 0 ? Either.left(i) : Either.right(i));
            assertThat(sides._1().take(33).toList()).isEqualTo(List.range(0, 66).filter(i -> i % 2 == 0));
            assertThat(sides._2().take(33).toList()).isEqualTo(List.range(0, 66).filter(i -> i % 2 != 0));
        }

        @Test
        public void shouldRejectANullEitherWhenASideReachesIt() {
            Tuple2<LazyList<Integer>, LazyList<Integer>> sides =
                    LazyList.from(0).partitionMap(i -> i == 4 ? null : i % 2 == 0 ? Either.left(i) : Either.right(i));
            assertThat(sides._1().take(2).toList()).isEqualTo(List.of(0, 2));
            assertThat(sides._2().take(2).toList()).isEqualTo(List.of(1, 3));
            assertThatNullPointerException()
                    .isThrownBy(() -> sides._1().take(3).toList())
                    .withMessage("LazyList.partitionMap: f returned null");
        }

        @Test
        public void shouldReturnTheEmptyLazyListForAnEmptySide() {
            Tuple2<LazyList<Integer>, LazyList<Integer>> none =
                    LazyList.<Integer>empty().partitionMap(Either::left);
            assertThat(none._1()).isSameAs(LazyList.empty());
            assertThat(none._2()).isSameAs(LazyList.empty());
            assertThat(LazyList.of(1, 2)
                            .partitionMap(Either::<Integer, Integer>left)
                            ._2())
                    .isEmpty();
            assertThat(LazyList.of(1, 2)
                            .partitionMap(Either::<Integer, Integer>right)
                            ._1())
                    .isEmpty();
        }

        @Test
        public void shouldRejectNullFunctionAndNullEither() {
            assertThatNullPointerException()
                    .isThrownBy(() -> LazyList.of(1).partitionMap(null))
                    .withMessage("f is null");
            for (int n : new int[] {1, 32, 33}) {
                int last = n - 1;
                assertThatNullPointerException()
                        .isThrownBy(() -> LazyList.range(0, n)
                                .partitionMap(i -> i == last ? null : Either.<Integer, Integer>left(i))
                                ._1()
                                .toList())
                        .withMessage("LazyList.partitionMap: f returned null");
            }
        }
    }

    @Nested
    class FlattenTests {

        @Test
        public void shouldFlattenAtEveryBoundary() {
            for (int n : new int[] {0, 1, 32, 33}) {
                LazyList<Integer> inner = LazyList.range(0, n);
                assertThat(LazyList.flatten(LazyList.of(inner))).isEqualTo(inner);
                assertThat(LazyList.flatten(LazyList.of(inner, inner))).isEqualTo(inner.appendAll(inner));
                assertThat(LazyList.flatten(LazyList.of(LazyList.<Integer>empty(), inner, LazyList.<Integer>empty())))
                        .isEqualTo(inner);
                assertThat(LazyList.flatten(
                                java.util.List.of(Vector.range(0, n), new java.util.ArrayList<>(inner.asJava()))))
                        .isEqualTo(inner.appendAll(inner));
                // n inner iterables of one element each
                assertThat(LazyList.flatten(inner.map(LazyList::of))).isEqualTo(inner);
            }
        }

        @Test
        public void shouldFlattenEmptiesToTheEmptyLazyList() {
            assertThat(LazyList.flatten(LazyList.<LazyList<Integer>>empty())).isSameAs(LazyList.empty());
            assertThat(LazyList.flatten(LazyList.of(LazyList.<Integer>empty()))).isEmpty();
            assertThat(LazyList.flatten(LazyList.of(
                            LazyList.<Integer>empty(), Vector.<Integer>empty(), java.util.List.<Integer>of())))
                    .isEmpty();
            assertThat(LazyList.flatten(java.util.List.<java.util.List<Integer>>of()))
                    .isSameAs(LazyList.empty());
        }

        @Test
        public void shouldWidenTheElementType() {
            LazyList<Number> numbers = LazyList.flatten(LazyList.of(LazyList.of(1), LazyList.of(2.0)));
            assertThat(numbers).isEqualTo(LazyList.<Number>of(1, 2.0));
        }

        @Test
        public void shouldReadOneShotIterablesOnce() {
            assertThat(LazyList.flatten(oneShotOf(oneShotOf(1, 2), oneShotOf(), oneShotOf(3))))
                    .isEqualTo(LazyList.of(1, 2, 3));
            assertThat(LazyList.flatten(LazyList.<Iterable<Integer>>empty())).isSameAs(LazyList.empty());
            assertThat(LazyList.<Integer>flatten(oneShotOf())).isEmpty();
        }

        @Test
        public void shouldRejectNulls() {
            assertThatNullPointerException()
                    .isThrownBy(() -> LazyList.flatten(null))
                    .withMessage("nested is null");
            // lazy: a null inner iterable or element fails when the result reaches it
            assertThatNullPointerException()
                    .isThrownBy(() -> LazyList.flatten(java.util.Arrays.asList(null, LazyList.of(1)))
                            .isEmpty());
            assertThatNullPointerException()
                    .isThrownBy(() -> LazyList.flatten(java.util.Arrays.asList(LazyList.of(1), null))
                            .toList());
            assertThatNullPointerException()
                    .isThrownBy(() -> LazyList.flatten(LazyList.of(java.util.Arrays.asList(1, null)))
                            .toList())
                    .withMessage("LazyList: element is null");
        }
    }

    @Nested
    class DuplicatesTests {

        @Test
        public void shouldReturnDuplicatesInOrderOfFirstOccurrence() {
            assertThat(LazyList.of(3, 1, 3, 2, 1, 3).duplicates()).isEqualTo(LazyList.of(3, 1));
            assertThat(LazyList.of(1, 2, 2, 1).duplicates()).isEqualTo(LazyList.of(1, 2));
            assertThat(LazyList.of("a", "b", "c").duplicates()).isEmpty();
            assertThat(LazyList.<Integer>empty().duplicates()).isSameAs(LazyList.empty());
            assertThat(LazyList.of(1).duplicates()).isEmpty();
        }

        @Test
        public void shouldReturnTheFirstElementOfEachDuplicatedKey() {
            assertThat(LazyList.of("aa", "b", "cc", "dd", "e").duplicatesBy(String::length))
                    .isEqualTo(LazyList.of("aa", "b"));
            assertThat(LazyList.of("aa", "b", "cc", "dd", "eee").duplicatesBy(String::length))
                    .isEqualTo(LazyList.of("aa"));
            assertThat(LazyList.of("b", "aa", "e", "cc").duplicatesBy(String::length))
                    .isEqualTo(LazyList.of("b", "aa"));
            assertThat(LazyList.of("a", "bb").duplicatesBy(String::length)).isEmpty();
            assertThatNullPointerException()
                    .isThrownBy(() -> LazyList.of(1).duplicatesBy(null))
                    .withMessage("keyExtractor is null");
        }

        @Test
        public void shouldFindDuplicatesOfANullKey() {
            // an LazyList never holds a null element, but a key extractor may return null for several of them
            assertThat(LazyList.of("a", "b").duplicatesBy(s -> null)).isEqualTo(LazyList.of("a"));
            assertThat(LazyList.of("a", "bb", "c").duplicatesBy(s -> s.length() == 1 ? null : s))
                    .isEqualTo(LazyList.of("a"));
            assertThat(LazyList.of("a").duplicatesBy(s -> null)).isEmpty();
        }

        @Test
        public void shouldComputeTheKeyOncePerElementInOrder() {
            java.util.List<Integer> seen = new ArrayList<>();
            assertThat(LazyList.range(0, 33).duplicatesBy(i -> {
                        seen.add(i);
                        return i % 5;
                    }))
                    .isEqualTo(LazyList.of(0, 1, 2, 3, 4));
            assertThat(LazyList.ofAll(seen)).isEqualTo(LazyList.range(0, 33));
        }

        @Test
        public void shouldFindDuplicatesAtEveryBoundary() {
            for (int n : new int[] {0, 1, 32, 33}) {
                LazyList<Integer> source = LazyList.range(0, n);
                assertThat(source.duplicates()).isEmpty();
                assertThat(source.appendAll(source).duplicates()).isEqualTo(source);
                assertThat(source.appendAll(source.reverse()).duplicates()).isEqualTo(source);
                assertThat(source.duplicatesBy(i -> i % 5))
                        .isEqualTo(source.take(Math.max(n - 5, 0)).take(5));
            }
        }
    }

    @Nested
    class LazyFlattenTests {

        /** An infinite outer Iterable counting the inner iterables it has handed out; one-shot like a java.util.stream. */
        private Iterable<List<Integer>> countingOuter(AtomicInteger opened) {
            return java.util.stream.Stream.iterate(0, i -> i + 1).map(i -> {
                opened.incrementAndGet();
                return List.of(i, i);
            })::iterator;
        }

        @Test
        public void shouldFlattenAnInfiniteOuterIterableLazily() {
            AtomicInteger opened = new AtomicInteger();
            LazyList<Integer> flat = LazyList.flatten(countingOuter(opened));
            assertThat(opened.get()).isZero(); // nothing is evaluated before the result is read
            assertThat(flat.take(5).toList()).isEqualTo(List.of(0, 0, 1, 1, 2));
            assertThat(opened.get()).isEqualTo(3);
            // memoised: reading the same prefix again opens nothing
            assertThat(flat.take(5).toList()).isEqualTo(List.of(0, 0, 1, 1, 2));
            assertThat(opened.get()).isEqualTo(3);
        }

        @Test
        public void shouldFlattenAnInfiniteInnerIterable() {
            assertThat(LazyList.flatten(List.of(LazyList.from(0))).take(3).toList())
                    .isEqualTo(List.of(0, 1, 2));
            assertThat(LazyList.flatten(List.of(List.of(-1), LazyList.from(0), List.of(-2)))
                            .take(3)
                            .toList())
                    .isEqualTo(List.of(-1, 0, 1));
            assertThat(LazyList.flatten(LazyList.from(0).map(i -> LazyList.from(i)))
                            .take(3)
                            .toList())
                    .isEqualTo(List.of(0, 1, 2));
        }

        @Test
        public void shouldSkipEmptyInnerIterablesOfAnInfiniteOuter() {
            LazyList<Integer> flat =
                    LazyList.flatten(LazyList.from(0).map(i -> i % 3 == 0 ? List.of(i) : List.<Integer>empty()));
            assertThat(flat.take(4).toList()).isEqualTo(List.of(0, 3, 6, 9));
        }

        @Test
        public void shouldRejectANullElementWhenTheResultReachesIt() {
            LazyList<Integer> flat = LazyList.flatten(List.of(List.of(1), java.util.Arrays.asList(2, null)));
            assertThat(flat.take(2).toList()).isEqualTo(List.of(1, 2));
            assertThatNullPointerException().isThrownBy(flat::toList);
        }
    }

    @Nested
    class InfiniteDuplicatesTests {

        @Test
        public void shouldForceTheWholeLazyList() {
            AtomicInteger forced = new AtomicInteger();
            LazyList<Integer> source = LazyList.range(0, 40).map(i -> {
                forced.incrementAndGet();
                return i % 7;
            });
            assertThat(source.duplicates()).isEqualTo(LazyList.range(0, 7));
            assertThat(forced.get()).isEqualTo(40);
        }
    }

    /// Walks to a start index close to the end of a long LazyList: the walk is a loop, not one stack frame per element.
    @Nested
    class DeepIndexTests {
        private static final int SIZE = 1_000_000;
        private static final int START = 999_000;

        private LazyList<Integer> longLazyList() {
            return LazyList.range(0, SIZE);
        }

        @Test
        public void sliceStartsDeepWithoutOverflow() {
            assertThat(longLazyList().slice(START, START + 3)).isEqualTo(LazyList.of(START, START + 1, START + 2));
            assertThat(longLazyList().slice(START, SIZE + 10).size()).isEqualTo(SIZE - START);
        }

        @Test
        public void sliceStartsDeepInAnAppendedLazyListWithoutOverflow() {
            LazyList<Integer> appended = LazyList.range(0, SIZE - 1).append(SIZE - 1);
            assertThat(appended.slice(START, START + 2)).isEqualTo(LazyList.of(START, START + 1));
            assertThat(appended.subSequence(SIZE - 2, SIZE)).isEqualTo(LazyList.of(SIZE - 2, SIZE - 1));
        }

        @Test
        public void subSequenceFromStartsDeepWithoutOverflow() {
            LazyList<Integer> actual = longLazyList().subSequence(START);
            assertThat(actual.size()).isEqualTo(SIZE - START);
            assertThat(actual.head()).isEqualTo(START);
        }

        @Test
        public void subSequenceFromToStartsDeepWithoutOverflow() {
            assertThat(longLazyList().subSequence(START, START + 3))
                    .isEqualTo(LazyList.of(START, START + 1, START + 2));
            assertThat(longLazyList().subSequence(START, SIZE).size()).isEqualTo(SIZE - START);
        }

        @Test
        public void subSequenceFromToPastTheEndThrowsOnTraversalAfterADeepStart() {
            LazyList<Integer> actual = longLazyList().subSequence(START, SIZE + 1);
            assertThatThrownBy(actual::size)
                    .isInstanceOf(IndexOutOfBoundsException.class)
                    .hasMessage("subSequence of Nil");
        }

        @Test
        public void otherIndexedMethodsStartDeepWithoutOverflow() {
            assertThat(longLazyList().drop(START).head()).isEqualTo(START);
            assertThat(longLazyList().get(START)).isEqualTo(START);
            assertThat(longLazyList().update(START, -1).drop(START).take(2)).isEqualTo(LazyList.of(-1, START + 1));
            assertThat(longLazyList().insert(START, -1).drop(START).take(2)).isEqualTo(LazyList.of(-1, START));
            assertThat(longLazyList()
                            .insertAll(START, List.of(-1, -2))
                            .drop(START)
                            .take(3))
                    .isEqualTo(LazyList.of(-1, -2, START));
            assertThat(longLazyList().removeAt(START).drop(START).head()).isEqualTo(START + 1);
            assertThat(longLazyList().splitAt(START)._2().head()).isEqualTo(START);
            assertThat(longLazyList().patch(START, List.of(-1), 2).drop(START).take(2))
                    .isEqualTo(LazyList.of(-1, START + 2));
            assertThat(longLazyList().dropRight(START).size()).isEqualTo(SIZE - START);
            assertThat(longLazyList().takeRight(SIZE - START).head()).isEqualTo(START);
        }
    }

    /// The part of a slice after its start is forced only when the result reaches it, so a slice of an infinite
    /// LazyList works.
    @Nested
    class SliceLazinessTests {
        private LazyList<Integer> counted(AtomicInteger forced) {
            return LazyList.continually(forced::getAndIncrement);
        }

        @Test
        public void sliceForcesTheStartOnlyAndTheRestOnDemand() {
            AtomicInteger forced = new AtomicInteger();
            LazyList<Integer> actual = counted(forced).slice(5, 8);
            assertThat(forced.get()).isZero(); // nothing is evaluated before the result is read
            assertThat(actual.toList()).isEqualTo(List.of(5, 6, 7));
            assertThat(forced.get()).isEqualTo(8);
        }

        @Test
        public void subSequenceForcesTheStartOnlyAndTheRestOnDemand() {
            AtomicInteger forced = new AtomicInteger();
            LazyList<Integer> actual = counted(forced).subSequence(5, 8);
            assertThat(forced.get()).isEqualTo(6);
            assertThat(actual.toList()).isEqualTo(List.of(5, 6, 7));
            assertThat(forced.get()).isEqualTo(8);
        }

        @Test
        public void subSequenceFromForcesTheStartOnly() {
            AtomicInteger forced = new AtomicInteger();
            LazyList<Integer> actual = counted(forced).subSequence(5);
            assertThat(forced.get()).isEqualTo(5); // the bounds are checked now, the result is not read
            assertThat(actual.take(3).toList()).isEqualTo(List.of(5, 6, 7));
            assertThat(forced.get()).isEqualTo(8);
        }

        @Test
        public void sliceToTheLargestIndexOfAnInfiniteLazyList() {
            assertThat(LazyList.from(0).slice(5, Integer.MAX_VALUE).take(3)).isEqualTo(LazyList.of(5, 6, 7));
            assertThat(LazyList.from(0).subSequence(5, Integer.MAX_VALUE).take(3))
                    .isEqualTo(LazyList.of(5, 6, 7));
        }

        @Test
        public void sliceClampsANegativeRangeToEmpty() {
            assertThat(LazyList.of(1, 2, 3).slice(-5, -1)).isEmpty();
            assertThat(LazyList.of(1, 2, 3).slice(-1, 0)).isEmpty();
            assertThat(LazyList.of(1, 2, 3).slice(-1, 1)).isEqualTo(LazyList.of(1));
            assertThat(LazyList.of(1, 2, 3).slice(1, 10)).isEqualTo(LazyList.of(2, 3));
            assertThat(LazyList.of(1, 2, 3).slice(3, 10)).isEmpty();
            assertThat(LazyList.of(1, 2, 3).slice(10, 20)).isEmpty();
            assertThat(LazyList.empty().slice(0, 1)).isEmpty();
            assertThat(LazyList.empty().slice(-1, 1)).isEmpty();
        }

        @Test
        public void subSequenceBoundsAreCheckedAsBefore() {
            assertThatThrownBy(() -> LazyList.of(1).subSequence(5, 5))
                    .isInstanceOf(IndexOutOfBoundsException.class)
                    .hasMessage("subSequence of Nil");
            assertThat(LazyList.of(1, 2).subSequence(2, 2)).isEmpty();
            assertThat(LazyList.of(1, 2).subSequence(1, 2)).isEqualTo(LazyList.of(2));
            assertThatThrownBy(() -> LazyList.of(1, 2).subSequence(2, 3))
                    .isInstanceOf(IndexOutOfBoundsException.class)
                    .hasMessage("subSequence of Nil");
            assertThatThrownBy(() -> LazyList.of(1, 2).subSequence(5, 6))
                    .isInstanceOf(IndexOutOfBoundsException.class)
                    .hasMessage("subSequence of Nil");
            assertThatThrownBy(() -> LazyList.empty().subSequence(0, 1))
                    .isInstanceOf(IndexOutOfBoundsException.class)
                    .hasMessage("subSequence of Nil");
            assertThatThrownBy(() -> LazyList.of(1, 2).subSequence(-1, 1))
                    .isInstanceOf(IndexOutOfBoundsException.class);
            assertThatThrownBy(() -> LazyList.of(1, 2).subSequence(2, 1)).isInstanceOf(IllegalArgumentException.class);
            LazyList<Integer> pastTheEnd = LazyList.of(1, 2).subSequence(1, 3);
            assertThat(pastTheEnd.head()).isEqualTo(2);
            assertThatThrownBy(() -> pastTheEnd.tail().isEmpty())
                    .isInstanceOf(IndexOutOfBoundsException.class)
                    .hasMessage("subSequence of Nil");
        }
    }

    @Nested
    class ForcedCellsTests {

        /** An infinite LazyList 1, 2, 3, ... counting how many of its elements have been forced. */
        private LazyList<Integer> counted(AtomicInteger forced) {
            return LazyList.continually(forced::incrementAndGet);
        }

        private int[] indices(int n) {
            return new int[] {Integer.MIN_VALUE, -1, 0, 1, n - 1, n, n + 1, Integer.MAX_VALUE};
        }

        @Test
        public void patchForcesTheElementsAsTheResultReachesThem() {
            AtomicInteger forced = new AtomicInteger();
            LazyList<Integer> patched = counted(forced).patch(10, List.of(-1, -2, -3), 5);
            assertThat(forced.get()).isZero();
            assertThat(patched.take(15).toList()).isEqualTo(List.range(1, 11).appendAll(List.of(-1, -2, -3, 16, 17)));
            assertThat(forced.get()).isEqualTo(17);
        }

        @Test
        public void patchAtTheStartWithNothingForcesTheReplacedElementsForItsHead() {
            AtomicInteger forced = new AtomicInteger();
            LazyList<Integer> patched = counted(forced).patch(0, List.empty(), 5);
            assertThat(forced.get()).isZero();
            assertThat(patched.head()).isEqualTo(6);
            assertThat(forced.get()).isEqualTo(6);
            AtomicInteger forcedToo = new AtomicInteger();
            LazyList<Integer> replaced = counted(forcedToo).patch(0, List.of(-1), 5);
            assertThat(forcedToo.get()).isZero();
            assertThat(replaced.take(2).toList()).isEqualTo(List.of(-1, 6));
            assertThat(forcedToo.get()).isEqualTo(6);
        }

        @Test
        public void patchAgreesWithVector() {
            List<List<Integer>> replacements = List.of(List.empty(), List.of(-1), List.of(-1, -2, -3));
            for (int n : new int[] {0, 1, 5}) {
                LazyList<Integer> stream = LazyList.range(0, n);
                Vector<Integer> vector = Vector.range(0, n);
                for (int from : indices(n)) {
                    for (int replaced : indices(n)) {
                        if ((long) Math.max(from, 0) + Math.max(replaced, 0) > Integer.MAX_VALUE) {
                            continue; // Vector adds the two
                        }
                        for (List<Integer> that : replacements) {
                            assertThat(stream.patch(from, that, replaced).toVector())
                                    .as("patch(%d, %s, %d) of %d", from, that, replaced, n)
                                    .isEqualTo(vector.patch(from, that, replaced));
                        }
                    }
                }
            }
            assertThat(LazyList.of(1, 2).patch(Integer.MAX_VALUE, List.of(9), Integer.MAX_VALUE))
                    .isEqualTo(LazyList.of(1, 2, 9));
            assertThatNullPointerException()
                    .isThrownBy(() -> LazyList.of(1).patch(0, null, 0))
                    .withMessage("that is null");
        }

        @Test
        public void dropRightForcesTheDroppedElementsAndOneMore() {
            AtomicInteger forced = new AtomicInteger();
            LazyList<Integer> dropped = counted(forced).dropRight(10);
            assertThat(forced.get()).isZero();
            assertThat(dropped.take(3).toList()).isEqualTo(List.of(1, 2, 3));
            assertThat(forced.get()).isEqualTo(13);
        }

        @Test
        public void lastIndexOfSliceForcesNoMoreThanEndPlusTheSliceLength() {
            AtomicInteger forced = new AtomicInteger();
            assertThat(counted(forced).lastIndexOfSlice(List.of(11, 12), 20)).isEqualTo(10);
            assertThat(forced.get()).isEqualTo(21);
            forced.set(0);
            assertThat(counted(forced).lastIndexOfSlice(List.of(21, 22), 20)).isEqualTo(20);
            assertThat(forced.get()).isEqualTo(22);
            forced.set(0);
            assertThat(counted(forced).lastIndexOfSlice(List.empty(), 20)).isEqualTo(20);
            assertThat(forced.get()).isEqualTo(20);
            forced.set(0);
            assertThat(counted(forced).lastIndexOfSlice(List.of(1), 0)).isEqualTo(0);
            assertThat(forced.get()).isEqualTo(1);
            forced.set(0);
            assertThat(counted(forced).lastIndexOfSlice(List.of(1), -1)).isEqualTo(-1);
            assertThat(forced.get()).isZero();
        }

        @Test
        public void lastIndexOfSliceAgreesWithVector() {
            List<List<Integer>> slices = List.of(
                    List.empty(), List.of(1), List.of(1, 2), List.of(2, 1, 2), List.of(3), List.of(1, 2, 1, 2, 1, 2));
            for (Vector<Integer> vector :
                    List.of(Vector.<Integer>empty(), Vector.of(1), Vector.of(1, 2, 1, 2, 1), Vector.of(2, 2, 2))) {
                LazyList<Integer> stream = LazyList.ofAll(vector);
                for (List<Integer> slice : slices) {
                    assertThat(stream.lastIndexOfSlice(slice))
                            .as("lastIndexOfSlice(%s) of %s", slice, vector)
                            .isEqualTo(vector.lastIndexOfSlice(slice));
                    for (int end : indices(vector.size())) {
                        assertThat(stream.lastIndexOfSlice(slice, end))
                                .as("lastIndexOfSlice(%s, %d) of %s", slice, end, vector)
                                .isEqualTo(vector.lastIndexOfSlice(slice, end));
                    }
                }
            }
        }
    }

    @Nested
    class SubSequenceBoundsTests {

        // what `operation` returns, or the class of the exception it throws
        private Object valueOrExceptionClass(Supplier<?> operation) {
            try {
                return operation.get();
            } catch (RuntimeException e) {
                return e.getClass();
            }
        }

        private int[] indices(int n) {
            return new int[] {Integer.MIN_VALUE, -1, 0, 1, n - 1, n, n + 1, Integer.MAX_VALUE};
        }

        @Test
        public void subSequenceThrowsExactlyWhenVectorThrows() {
            for (int n : new int[] {0, 1, 5}) {
                Vector<Integer> vector = Vector.range(0, n);
                for (int from : indices(n)) {
                    int begin = from;
                    Object single = valueOrExceptionClass(() -> vector.subSequence(begin));
                    Object singleLazyList = valueOrExceptionClass(
                            () -> LazyList.range(0, n).subSequence(begin).toVector());
                    assertThat(singleLazyList)
                            .as("subSequence(%d) on %d", begin, n)
                            .isEqualTo(single);
                    for (int to : indices(n)) {
                        String call = "subSequence(" + from + ", " + to + ") on " + n;
                        Try<Vector<Integer>> expectedTry = Try.of(() -> vector.subSequence(from, to));
                        Class<?> expected =
                                expectedTry.isFailure() ? expectedTry.getCause().getClass() : null;
                        Vector<Integer> expectedResult = expectedTry.getOrNull();
                        LazyList<Integer> stream = LazyList.range(0, n);
                        LazyList<Integer> result;
                        try {
                            result = stream.subSequence(from, to);
                        } catch (RuntimeException e) {
                            assertThat(e.getClass()).as(call).isEqualTo(expected);
                            continue;
                        }
                        if (expected == IndexOutOfBoundsException.class && from < to) {
                            // an end past the end of a lazy LazyList throws when the traversal reaches it
                            assertThatThrownBy(result::toVector).as(call).isInstanceOf(IndexOutOfBoundsException.class);
                        } else {
                            assertThat(expected).as(call).isNull();
                            assertThat(result.toVector()).as(call).isEqualTo(expectedResult);
                        }
                    }
                }
            }
        }

        @Test
        public void subSequenceChecksAnEmptyOrReversedRangeWithoutForcingPastIt() {
            AtomicInteger forced = new AtomicInteger();
            LazyList<Integer> stream = LazyList.continually(forced::incrementAndGet);
            assertThat(stream.subSequence(10, 10)).isEmpty();
            assertThat(forced.get()).isEqualTo(10);
            assertThatThrownBy(() -> stream.subSequence(30, 20)).isInstanceOf(IllegalArgumentException.class);
            assertThat(forced.get()).isEqualTo(20);
            assertThatThrownBy(() -> LazyList.of(1, 2).subSequence(5, 3))
                    .isInstanceOf(IndexOutOfBoundsException.class)
                    .hasMessage("subSequence of Nil");
        }
    }

    @Nested
    class FailedForceTests {

        /** A one-shot source of 0, 1, 2, ... whose next() call number `failAt` (from 1) throws, after consuming. */
        private final class FailingSource implements java.util.Iterator<Integer> {

            final int failAt;
            int calls;

            FailingSource(int failAt) {
                this.failAt = failAt;
            }

            @Override
            public boolean hasNext() {
                return true;
            }

            @Override
            public Integer next() {
                calls++;
                if (calls == failAt) {
                    throw new IllegalStateException("source failed at call " + calls);
                }
                return calls - 1;
            }
        }

        /** Reads the LazyList (and the LazyLists in it) to the end: the elements read, then what stopped the walk. */
        private Tuple2<Vector<Object>, Throwable> walk(LazyList<?> stream) {
            java.util.List<Object> read = new ArrayList<>();
            try {
                for (LazyList<?> s = stream; !s.isEmpty(); s = s.tail()) {
                    if (read.size() == 1_000) {
                        throw new AssertionError("the walk went past the failure: "
                                + Vector.ofAll(read).take(10));
                    }
                    Object head = s.head();
                    read.add(head instanceof Traversable<?> nested ? nested.toVector() : head);
                }
            } catch (RuntimeException failure) {
                return Tuple.of(Vector.ofAll(read), failure);
            }
            throw new AssertionError("the walk ended without a failure: " + Vector.ofAll(read));
        }

        /** Walks three times, through tail(), the iterator, get and toVector: the same elements and exception each time. */
        private void assertFailsTheSameWay(String name, LazyList<?> result, FailingSource source) {
            Tuple2<Vector<Object>, Throwable> first = walk(result);
            int calls = source.calls;
            for (int i = 0; i < 2; i++) {
                Tuple2<Vector<Object>, Throwable> again = walk(result);
                assertThat(again._1()).as(name).isEqualTo(first._1());
                assertThat(again._2()).as(name).isSameAs(first._2());
                assertThatThrownBy(result::toVector).as(name).isSameAs(first._2());
                assertThatThrownBy(() -> result.iterator().forEachRemaining(x -> {}))
                        .as(name)
                        .isSameAs(first._2());
                assertThatThrownBy(() -> result.get(1_000)).as(name).isSameAs(first._2());
                assertThat(source.calls).as(name).isEqualTo(calls);
            }
        }

        private <R> void check(String name, Function<LazyList<Integer>, LazyList<R>> operation) {
            for (int failAt = 2; failAt <= 5; failAt++) {
                FailingSource source = new FailingSource(failAt);
                LazyList<Integer> stream = LazyList.ofAll(() -> source);
                LazyList<R> result;
                try {
                    result = operation.apply(stream);
                } catch (IllegalStateException failure) {
                    // an operation that reads ahead met the failure now: calling it again meets the same one
                    int calls = source.calls;
                    for (int i = 0; i < 2; i++) {
                        assertThatThrownBy(() -> operation.apply(stream))
                                .as(name)
                                .isSameAs(failure);
                    }
                    assertThat(source.calls).as(name).isEqualTo(calls);
                    continue;
                }
                assertFailsTheSameWay(name + " failing at " + failAt, result, source);
            }
        }

        @Test
        public void everyLazyOperationKeepsTheFailureOfItsSource() {
            check("ofAll", s -> s);
            check("map", s -> s.map(x -> x * 10));
            check("tap", s -> s.tap(x -> {}));
            check("filter", s -> s.filter(x -> x % 2 == 0));
            check("reject", s -> s.reject(x -> x % 2 == 1));
            check("collect", s -> s.collect(x -> Option.some(x)));
            check("flatMap", s -> s.flatMap(x -> List.of(x, x)));
            check("distinct", LazyList::distinct);
            check("distinctBy", s -> s.distinctBy(x -> x));
            check("retainAll", s -> s.retainAll(LazyList.range(0, 100)));
            check("removeAll", s -> s.removeAll(List.of(1)));
            check("append", s -> s.append(-1));
            check("appendAll", s -> s.appendAll(List.of(-1, -2)));
            check("appendAll into", s -> LazyList.of(-1).append(-2).appendAll(s));
            check("prependAll", s -> s.prependAll(List.of(-1, -2)));
            check("prependAll into", s -> LazyList.of(-1).prependAll(s));
            check("insert", s -> s.insert(1, -1));
            check("insertAll", s -> s.insertAll(1, List.of(-1, -2)));
            check("patch", s -> s.patch(1, List.of(-1, -2), 1));
            check("intersperse", s -> s.intersperse(-1));
            check("take", s -> s.take(100));
            check("takeWhile", s -> s.takeWhile(x -> x < 100));
            check("dropWhile", s -> s.dropWhile(x -> x < 1));
            check("removeAt", s -> s.removeAt(1));
            check("replace", s -> s.replace(1, -1));
            check("zip", s -> s.zip(LazyList.from(0)));
            check("zip into", s -> LazyList.from(0).zip(s));
            check("zipWith", s -> s.zipWith(LazyList.from(0), Integer::sum));
            check("zipAll", s -> s.zipAll(List.of(1), -1, -2));
            check("zipWithIndex", LazyList::zipWithIndex);
            check("scanLeft", s -> s.scanLeft(0, Integer::sum));
            check("scan", s -> s.scan(0, Integer::sum));
            check("sliding", s -> s.sliding(2));
            check("grouped", s -> s.grouped(2));
            check("slideBy", s -> s.slideBy(x -> x / 2));
            check("cycle", LazyList::cycle);
            check("init", LazyList::init);
            check("extend", s -> s.extend(-1));
            check("appendSelf", s -> s.appendSelf(self -> self.map(x -> -x)));
        }

        @Test
        public void aFunctionThatFailsOnceKeepsFailing() {
            AtomicInteger calls = new AtomicInteger();
            LazyList<Integer> mapped = LazyList.range(0, 10).map(x -> {
                calls.incrementAndGet();
                if (x == 3 && calls.get() == 4) {
                    throw new IllegalStateException("mapper failed once");
                }
                return x;
            });
            Throwable first = walk(mapped)._2();
            assertThat(first.getMessage()).isEqualTo("mapper failed once");
            assertThatThrownBy(mapped::toVector).isSameAs(first);
            assertThatThrownBy(mapped::toVector).isSameAs(first);
            assertThat(calls.get()).isEqualTo(4);
        }

        @Test
        public void scanLeftIsNeverTruncatedByAFailedForce() {
            AtomicInteger calls = new AtomicInteger();
            LazyList<Integer> sums = LazyList.range(0, 5).scanLeft(0, (acc, x) -> {
                if (calls.incrementAndGet() == 2) {
                    throw new IllegalStateException("operation failed");
                }
                return acc + x;
            });
            Tuple2<Vector<Object>, Throwable> first = walk(sums);
            assertThat(first._1()).isEqualTo(Vector.of(0, 0));
            for (int i = 0; i < 2; i++) {
                assertThat(walk(sums)._1()).isEqualTo(first._1());
                assertThatThrownBy(sums::toVector).isSameAs(first._2());
            }
            assertThat(calls.get()).isEqualTo(2);
        }

        @Test
        public void aRejectedNullElementIsNeverDropped() {
            java.util.Iterator<Integer> source =
                    java.util.Arrays.asList(1, null, 3).iterator();
            LazyList<Integer> stream = LazyList.ofAll(() -> source);
            Throwable first = walk(stream)._2();
            assertThat(first).isInstanceOf(NullPointerException.class);
            for (int i = 0; i < 2; i++) {
                assertThat(walk(stream)._1()).isEqualTo(Vector.of(1));
                assertThatThrownBy(stream::toVector).isSameAs(first);
                assertThatThrownBy(() -> stream.get(1)).isSameAs(first);
            }
            assertThat(source.next()).isEqualTo(3); // the element after the null was never read
        }

        @Test
        public void aNullTailFromConsIsTheSameFailureEachTime() {
            AtomicInteger calls = new AtomicInteger();
            LazyList<Integer> stream = LazyList.cons(1, () -> {
                calls.incrementAndGet();
                return null;
            });
            Throwable first = walk(stream)._2();
            assertThat(first.getMessage()).isEqualTo("LazyList.cons: tailSupplier returned null");
            // the tail is a list of its own: reading it, not asking for it, calls the supplier
            LazyList<Integer> tail = stream.tail();
            assertThat(calls.get()).isEqualTo(1);
            assertThatThrownBy(tail::isEmpty).isSameAs(first);
            assertThatThrownBy(tail::head).isSameAs(first);
            assertThat(calls.get()).isEqualTo(1);
        }

        @Test
        public void aTailThatNeedsItselfFailsInsteadOfOverflowing() {
            LazyList<Integer>[] self = new LazyList[1];
            self[0] = LazyList.cons(1, () -> self[0].tail());
            LazyList<Integer> tail = self[0].tail();
            assertThatThrownBy(tail::isEmpty)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("LazyList: evaluating this list needs the list itself");
            Throwable first = walk(self[0])._2();
            assertThatThrownBy(tail::isEmpty).isSameAs(first);
            LazyList<Integer>[] itself = new LazyList[1];
            itself[0] = LazyList.defer(() -> itself[0]);
            assertThatThrownBy(itself[0]::isEmpty)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("LazyList: evaluating this list needs the list itself");
        }

        @Test
        public void aVirtualMachineErrorIsNotKept() {
            AtomicInteger calls = new AtomicInteger();
            LazyList<Integer> stream = LazyList.cons(1, () -> {
                if (calls.incrementAndGet() == 1) {
                    throw new StackOverflowError("simulated");
                }
                return LazyList.of(2);
            });
            LazyList<Integer> tail = stream.tail();
            assertThatThrownBy(tail::isEmpty).isInstanceOf(StackOverflowError.class);
            assertThat(stream.toString()).isEqualTo("LazyList(1, <not computed>)");
            assertThat(tail).isEqualTo(LazyList.of(2));
            assertThat(stream.tail()).isSameAs(stream.tail());
            assertThat(calls.get()).isEqualTo(2);
        }

        @Test
        public void aFailureMetWithNoMemoryLeftIsKept() throws Exception {
            // a JVM of its own, with a small heap that the probe fills before the failure is met
            String javaHome = System.getProperty("java.home");
            String classPath = java.nio.file.Path.of(LazyList.class
                            .getProtectionDomain()
                            .getCodeSource()
                            .getLocation()
                            .toURI())
                    + java.io.File.pathSeparator
                    + java.nio.file.Path.of(FailedTailOutOfMemoryProbe.class
                            .getProtectionDomain()
                            .getCodeSource()
                            .getLocation()
                            .toURI());
            Process process = new ProcessBuilder(
                            java.nio.file.Path.of(javaHome, "bin", "java").toString(),
                            "-Xmx64m",
                            "-XX:+UseSerialGC",
                            "-cp",
                            classPath,
                            FailedTailOutOfMemoryProbe.class.getName())
                    .redirectErrorStream(true)
                    .start();
            String output = new String(process.getInputStream().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8)
                    .strip();
            assertThat(process.waitFor()).as(output).isEqualTo(0);
            assertThat(output).isEqualTo("RuntimeException: boom (same)");
        }

        @Test
        public void toStringShowsAFailedTailAsNotComputed() {
            LazyList<Integer> stream = LazyList.ofAll(() -> new FailingSource(3));
            walk(stream);
            assertThat(stream.toString()).isEqualTo("LazyList(0, 1, <not computed>)");
        }

        @Test
        public void aFailureIsKeptAcrossThreads() throws Exception {
            FailingSource source = new FailingSource(2);
            LazyList<Integer> stream = LazyList.ofAll(() -> source);
            Throwable first = walk(stream)._2();
            java.util.concurrent.atomic.AtomicReference<Throwable> seen =
                    new java.util.concurrent.atomic.AtomicReference<>();
            Thread thread = Thread.ofVirtual().start(() -> seen.set(walk(stream)._2()));
            thread.join();
            assertThat(seen.get()).isSameAs(first);
            assertThat(source.calls).isEqualTo(2);
        }
    }

    @Nested
    class LazyConcatenationTests {

        private static final int LOOP = 10_000;

        /** An infinite LazyList counting how many of its elements have been computed. */
        private LazyList<Integer> counted(AtomicInteger forced) {
            return LazyList.continually(forced::incrementAndGet);
        }

        /** A one-shot Iterable counting the elements read from it. */
        private Iterable<Integer> oneShot(AtomicInteger read, int size) {
            java.util.Iterator<Integer> iterator = new java.util.Iterator<>() {
                int next;

                @Override
                public boolean hasNext() {
                    return next < size;
                }

                @Override
                public Integer next() {
                    read.incrementAndGet();
                    return next++;
                }
            };
            return () -> iterator;
        }

        /** Walks the whole LazyList, checking each element and that each cell keeps its tail. */
        private void walkChecking(LazyList<Integer> stream, Vector<Integer> expected) {
            LazyList<Integer> end = Vector.range(0, expected.size()).foldLeft(stream, (s, index) -> {
                assertThat(s.isEmpty())
                        .as("ends at %d of %d", index, expected.size())
                        .isFalse();
                assertThat(s.head()).isEqualTo(expected.get(index));
                assertThat(s.tail()).isSameAs(s.tail());
                return s.tail();
            });
            assertThat(end.isEmpty()).as("goes beyond %d", expected.size()).isTrue();
        }

        // -- infinite and one-shot arguments

        @Test
        public void appendAllAfterAppendReturnsAtOnceOnAnInfiniteArgument() {
            AtomicInteger forced = new AtomicInteger();
            LazyList<Integer> result = LazyList.of(1).append(2).appendAll(counted(forced));
            assertThat(forced.get()).isZero();
            assertThat(result.take(5)).isEqualTo(LazyList.of(1, 2, 1, 2, 3));
            assertThat(forced.get()).isEqualTo(3);
            assertThat(LazyList.of(1).append(2).appendAll(LazyList.from(0)).take(4))
                    .isEqualTo(LazyList.of(1, 2, 0, 1));
        }

        @Test
        public void appendAllReadsNothingOfItsArgumentNow() {
            AtomicInteger read = new AtomicInteger();
            LazyList<Integer> plain = LazyList.of(-1).appendAll(oneShot(read, 5));
            assertThat(read.get()).isZero();
            AtomicInteger readAfterAppend = new AtomicInteger();
            LazyList<Integer> appended = LazyList.of(-2).append(-1).appendAll(oneShot(readAfterAppend, 5));
            assertThat(readAfterAppend.get()).isZero();
            assertThat(plain).isEqualTo(LazyList.of(-1, 0, 1, 2, 3, 4));
            assertThat(appended).isEqualTo(LazyList.of(-2, -1, 0, 1, 2, 3, 4));
            // the one-shot source is read once, however many LazyLists share it
            assertThat(appended.append(9)).isEqualTo(LazyList.of(-2, -1, 0, 1, 2, 3, 4, 9));
            assertThat(appended.appendAll(List.of(8, 9))).isEqualTo(LazyList.of(-2, -1, 0, 1, 2, 3, 4, 8, 9));
            assertThat(read.get()).isEqualTo(5);
            assertThat(readAfterAppend.get()).isEqualTo(5);
        }

        @Test
        public void appendAllOfNothingReturnsThisLazyList() {
            LazyList<Integer> plain = LazyList.of(1, 2);
            LazyList<Integer> appended = plain.append(3);
            assertThat(plain.appendAll(List.empty())).isSameAs(plain);
            assertThat(appended.appendAll(List.empty())).isSameAs(appended);
            assertThat(appended.appendAll(LazyList.empty())).isSameAs(appended);
            LazyList<Integer> empty = LazyList.empty();
            assertThat(empty.appendAll(plain)).isSameAs(plain);
            assertThat(empty.appendAll(appended)).isSameAs(appended);
            assertThatNullPointerException()
                    .isThrownBy(() -> appended.appendAll(null))
                    .withMessage("elements is null");
            assertThatNullPointerException()
                    .isThrownBy(() -> plain.appendAll(null))
                    .withMessage("elements is null");
        }

        @Test
        public void prependAllOfAnAppendedLazyListReturnsAtOnceOnAnInfiniteLazyList() {
            AtomicInteger forced = new AtomicInteger();
            LazyList<Integer> result =
                    counted(forced).prependAll(LazyList.of(-2).append(-1));
            assertThat(forced.get()).isZero();
            assertThat(result.take(4)).isEqualTo(LazyList.of(-2, -1, 1, 2));
            assertThat(forced.get()).isEqualTo(2);
        }

        @Test
        public void insertAllOfAnAppendedLazyListReturnsAtOnceOnAnInfiniteLazyList() {
            AtomicInteger forced = new AtomicInteger();
            LazyList<Integer> atZero =
                    counted(forced).insertAll(0, LazyList.of(-2).append(-1));
            assertThat(forced.get()).isZero();
            assertThat(atZero.take(3)).isEqualTo(LazyList.of(-2, -1, 1));
            LazyList<Integer> atTwo =
                    LazyList.from(0).insertAll(2, LazyList.of(-2).append(-1));
            assertThat(atTwo.take(6)).isEqualTo(LazyList.of(0, 1, -2, -1, 2, 3));
            assertThat(LazyList.from(0).insertAll(1, LazyList.from(100)).take(3))
                    .isEqualTo(LazyList.of(0, 100, 101));
        }

        @Test
        public void patchReturnsAtOnceOnInfiniteLazyLists() {
            assertThat(LazyList.from(0).patch(1, LazyList.from(100), 2).take(3)).isEqualTo(LazyList.of(0, 100, 101));
            assertThat(LazyList.from(0).patch(1, LazyList.of(-2).append(-1), 2).take(5))
                    .isEqualTo(LazyList.of(0, -2, -1, 3, 4));
        }

        @Test
        public void extendAfterAppendReturnsAtOnce() {
            assertThat(LazyList.of(1).append(2).extend(0).take(4)).isEqualTo(LazyList.of(1, 2, 0, 0));
            AtomicInteger calls = new AtomicInteger();
            assertThat(LazyList.of(1).append(2).extend(calls::incrementAndGet).take(4))
                    .isEqualTo(LazyList.of(1, 2, 1, 2));
        }

        // -- memoised tails

        @Test
        public void theTailOfAnAppendedLazyListIsKept() {
            LazyList<Integer> appended =
                    LazyList.range(0, 3).append(3).append(4).appendAll(List.of(5, 6));
            Vector<Integer> expected = Vector.range(0, 7);
            walkChecking(appended, expected);
            // the same cells on a second walk
            for (LazyList<Integer> first = appended, second = appended;
                    !first.isEmpty();
                    first = first.tail(), second = second.tail()) {
                assertThat(second).isSameAs(first);
            }
            assertThat(appended.drop(4)).isEqualTo(appended.drop(4));
            assertThat(appended.tailOption().get()).isSameAs(appended.tail());
            assertThat(appended.get(5)).isEqualTo(5);
        }

        @Test
        public void anOlderVersionStaysValid() {
            LazyList<Integer> base = LazyList.of(0).append(1);
            LazyList<Integer> left = base.append(2);
            LazyList<Integer> right = base.appendAll(List.of(3, 4));
            assertThat(left.toVector()).isEqualTo(Vector.of(0, 1, 2));
            assertThat(right.toVector()).isEqualTo(Vector.of(0, 1, 3, 4));
            assertThat(base.toVector()).isEqualTo(Vector.of(0, 1));
            LazyList<Integer> tail = left.tail();
            assertThat(tail.append(5).toVector()).isEqualTo(Vector.of(1, 2, 5));
            assertThat(left.toVector()).isEqualTo(Vector.of(0, 1, 2));
        }

        // -- loops

        private int depth() {
            return StackWalker.getInstance().walk(frames -> frames.count()).intValue();
        }

        /**
         * The LazyList of the single element i, whose tail records how deep the stack is when a walk reaches its end: with
         * appends layered one over the other, reaching it would go through one call per layer.
         */
        private LazyList<Integer> recording(int i, AtomicInteger deepest) {
            return LazyList.cons(i, () -> {
                deepest.accumulateAndGet(depth(), Math::max);
                return LazyList.empty();
            });
        }

        /** Walks the LazyList, checks it, and returns how much deeper than the walk the recorded tails were computed. */
        private int walk(LazyList<Integer> stream, Vector<Integer> expected, AtomicInteger deepest) {
            int walkDepth = depth();
            walkChecking(stream, expected);
            return deepest.get() - walkDepth;
        }

        // a bound on the calls between the walk and a recorded tail, whatever the number of appends
        private static final int FEW_CALLS = 40;

        @Test
        public void aLoopOfAppendsReadsBackOneStepPerElement() {
            AtomicInteger deepest = new AtomicInteger();
            LazyList<Integer> stream = Vector.range(1, LOOP)
                    .foldLeft(
                            LazyList.of(0),
                            (acc, i) -> i % 2 == 0 ? acc.append(i) : acc.appendAll(recording(i, deepest)));
            assertThat(walk(stream, Vector.range(0, LOOP), deepest)).isBetween(1, FEW_CALLS);
        }

        @Test
        public void aLoopOfAppendAllsReadsBackOneStepPerElement() {
            AtomicInteger deepest = new AtomicInteger();
            LazyList<Integer> plain = Vector.range(1, LOOP)
                    .foldLeft(
                            LazyList.of(0),
                            (acc, i) -> i % 2 == 0 ? acc.appendAll(List.of(i)) : acc.appendAll(recording(i, deepest)));
            assertThat(walk(plain, Vector.range(0, LOOP), deepest)).isBetween(1, FEW_CALLS);
            AtomicInteger deepestNested = new AtomicInteger();
            // each argument is itself a LazyList built by append
            LazyList<Integer> appendedLazyLists = Vector.range(1, LOOP)
                    .foldLeft(
                            LazyList.of(0),
                            (acc, i) ->
                                    acc.appendAll(recording(i, deepestNested).append(-i)));
            Vector<Integer> expected =
                    Vector.of(0).appendAll(Vector.range(1, LOOP).flatMap(i -> List.of(i, -i)));
            assertThat(walk(appendedLazyLists, expected, deepestNested)).isBetween(1, FEW_CALLS);
        }

        @Test
        public void aLoopOfPrependAllsReadsBackOneStepPerElement() {
            AtomicInteger deepest = new AtomicInteger();
            LazyList<Integer> stream = Vector.rangeBy(LOOP - 2, -1, -1)
                    .foldLeft(
                            LazyList.of(LOOP - 1),
                            (acc, i) ->
                                    i % 2 == 0 ? acc.prependAll(List.of(i)) : acc.prependAll(recording(i, deepest)));
            assertThat(walk(stream, Vector.range(0, LOOP), deepest)).isBetween(1, FEW_CALLS);
            AtomicInteger deepestAppended = new AtomicInteger();
            LazyList<Integer> appended = Vector.rangeBy(LOOP - 2, -1, -1)
                    .foldLeft(
                            LazyList.of(LOOP - 1),
                            (acc, i) ->
                                    acc.prependAll(recording(i, deepestAppended).append(-i)));
            Vector<Integer> expected =
                    Vector.range(0, LOOP - 1).flatMap(i -> List.of(i, -i)).append(LOOP - 1);
            assertThat(walk(appended, expected, deepestAppended)).isBetween(1, FEW_CALLS);
        }

        @Test
        public void aLoopOfInsertAllsReadsBackOneStepPerElement() {
            AtomicInteger deepest = new AtomicInteger();
            LazyList<Integer> stream = Vector.rangeBy(LOOP - 2, -1, -1)
                    .foldLeft(LazyList.of(LOOP - 1), (acc, i) -> acc.insertAll(0, recording(i, deepest)));
            assertThat(walk(stream, Vector.range(0, LOOP), deepest)).isBetween(1, FEW_CALLS);
        }

        @Test
        public void aLoopOfPrependsAndAppendsReadsBackOneStepPerElement() {
            AtomicInteger deepest = new AtomicInteger();
            LazyList<Integer> stream = Vector.range(1, LOOP)
                    .foldLeft(LazyList.of(0), (acc, i) -> acc.prepend(-i).appendAll(recording(i, deepest)));
            assertThat(walk(stream, Vector.range(-LOOP + 1, LOOP), deepest)).isBetween(1, FEW_CALLS);
        }

        @Test
        public void theDepthOfAReadDoesNotGrowWithTheNumberOfAppends() {
            for (Function<Integer, LazyList<Integer>> build : java.util.List.<Function<Integer, LazyList<Integer>>>of(
                    n -> Vector.range(1, n).foldLeft(LazyList.of(0), (acc, i) -> acc.append(i)),
                    n -> Vector.range(1, n)
                            .foldLeft(
                                    LazyList.of(0), (acc, i) -> acc.prepend(-i).append(i)),
                    n -> Vector.range(1, n).foldLeft(LazyList.of(0), (acc, i) -> acc.prependAll(List.of(-i))),
                    n -> Vector.range(1, n)
                            .foldLeft(
                                    LazyList.of(0),
                                    (acc, i) -> acc.appendAll(LazyList.of(i).append(-i))))) {
                assertThat(deepestRead(build.apply(10_000))).isEqualTo(deepestRead(build.apply(1_000)));
            }
        }

        // how much deeper than the caller the stack goes while the last cell of the list is evaluated
        private int deepestRead(LazyList<Integer> list) {
            AtomicInteger deepest = new AtomicInteger();
            int start = depth();
            LazyList<Integer> recorded = list.appendAll(LazyList.defer(() -> {
                deepest.set(depth());
                return LazyList.empty();
            }));
            recorded.size();
            return deepest.get() - start;
        }

        @Test
        public void aLoopOfTailsAndAppendsReadsBackOneStepPerElement() {
            AtomicInteger deepest = new AtomicInteger();
            LazyList<Integer> stream = Vector.range(3, LOOP)
                    .foldLeft(LazyList.range(0, 3), (acc, i) -> acc.tail().appendAll(recording(i, deepest)));
            assertThat(walk(stream, Vector.range(LOOP - 3, LOOP), deepest)).isBetween(1, FEW_CALLS);
        }

        @Test
        public void aLoopOfAppendsOfNestedAppendedLazyListsIsReadWithoutOverflowingTheStack() {
            // a LazyList that starts with a plain cell whose tail is the previous result: nested one level deeper at
            // each step
            LazyList<Integer> stream = Vector.range(1, LOOP)
                    .foldLeft(
                            LazyList.of(0),
                            (previous, i) -> LazyList.cons(i, () -> previous).append(-i));

            Vector<Integer> read = stream.toVector();
            assertThat(read.size()).isEqualTo(2 * LOOP - 1);
            assertThat(read.take(3)).isEqualTo(Vector.of(LOOP - 1, LOOP - 2, LOOP - 3));
            assertThat(read.takeRight(3)).isEqualTo(Vector.of(-(LOOP - 3), -(LOOP - 2), -(LOOP - 1)));
        }
    }

    @Nested
    class InfiniteEqualsTests {

        private Vector<Traversable<Integer>> sequencesOf(Integer... elements) {
            return Vector.of(
                    List.of(elements),
                    Vector.of(elements),
                    Queue.of(elements),
                    LazyList.of(elements),
                    LazyList.of(elements).append(0).init());
        }

        @Test
        public void anInfiniteLazyListIsNotEqualToAFiniteSequence() {
            for (Traversable<Integer> finite :
                    sequencesOf(0, 1, 2).appendAll(sequencesOf()).appendAll(sequencesOf(0))) {
                LazyList<Integer> infinite = LazyList.from(0);
                assertThat(infinite.equals(finite)).as("%s", finite).isFalse();
                assertThat(finite.equals(infinite)).as("%s", finite).isFalse();
                LazyList<Integer> appended = LazyList.of(0).append(1).appendAll(LazyList.from(2));
                assertThat(appended.equals(finite)).as("%s", finite).isFalse();
                assertThat(finite.equals(appended)).as("%s", finite).isFalse();
            }
        }

        @Test
        public void anInfiniteLazyListIsNotEqualToAFiniteSequenceThatDiffers() {
            for (Traversable<Integer> finite : sequencesOf(1, 2)) {
                assertThat(LazyList.from(0).equals(finite)).as("%s", finite).isFalse();
                assertThat(finite.equals(LazyList.from(0))).as("%s", finite).isFalse();
            }
        }

        @Test
        public void equalsStopsAtTheFirstDifference() {
            AtomicInteger forced = new AtomicInteger();
            LazyList<Integer> stream = LazyList.continually(forced::incrementAndGet); // 1, 2, 3, ...
            assertThat(stream.equals(List.of(1, 2, 9, 4))).isFalse();
            assertThat(forced.get()).isEqualTo(3);
            assertThat(Vector.of(1, 2, 3, 4, 9).equals(stream)).isFalse();
            assertThat(forced.get()).isEqualTo(5);
        }

        @Test
        public void equalsStopsOneElementAfterTheEndOfTheShorterSide() {
            AtomicInteger forced = new AtomicInteger();
            LazyList<Integer> stream = LazyList.continually(forced::incrementAndGet);
            assertThat(stream.equals(Queue.of(1, 2, 3))).isFalse();
            // the fourth element answers whether the LazyList goes on
            assertThat(forced.get()).isEqualTo(4);
        }

        @Test
        public void twoInfiniteLazyListsThatDifferAreNotEqual() {
            assertThat(LazyList.from(0).equals(LazyList.from(0).update(1_000, -1)))
                    .isFalse();
            assertThat(LazyList.from(0).equals(LazyList.from(1))).isFalse();
        }

        @Test
        public void finiteSequencesOfEveryTypeAreEqualToALazyListWithTheSameElements() {
            for (Traversable<Integer> finite : sequencesOf(0, 1, 2)) {
                for (Traversable<Integer> other : sequencesOf(0, 1, 2)) {
                    assertThat(finite.equals(other)).as("%s %s", finite, other).isTrue();
                }
                for (Traversable<Integer> shorter : sequencesOf(0, 1)) {
                    assertThat(finite.equals(shorter))
                            .as("%s %s", finite, shorter)
                            .isFalse();
                    assertThat(shorter.equals(finite))
                            .as("%s %s", shorter, finite)
                            .isFalse();
                }
            }
        }

        @Test
        public void aLazyListIsEqualToItself() {
            LazyList<Integer> infinite = LazyList.from(0);
            assertThat(infinite.equals(infinite)).isTrue();
        }
    }
}
