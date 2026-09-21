package com.guizmaii.zazr.collection;

import com.guizmaii.zazr.*;
import com.guizmaii.zazr.Tuple;
import com.guizmaii.zazr.Tuple2;
import com.guizmaii.zazr.control.Option;
import com.guizmaii.zazr.control.Try;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
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

import static com.guizmaii.zazr.collection.Stream.concat;
import static java.util.Comparator.comparingInt;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class StreamTest extends AbstractTraversableRangeTest {

    // -- construction

    @Override
    protected <T> Collector<T, ArrayList<T>, Stream<T>> collector() {
        return Stream.collector();
    }

    @Override
    protected <T> Stream<T> empty() {
        return Stream.empty();
    }

    @Override
    protected <T> Stream<T> of(T element) {
        return Stream.of(element);
    }

    @SuppressWarnings("varargs")
    @SafeVarargs
    @Override
    protected final <T> Stream<T> of(T... elements) {
        return Stream.of(elements);
    }

    @Override
    protected <T> Stream<T> ofAll(Iterable<? extends T> elements) {
        return Stream.ofAll(elements);
    }

    @Override
    protected <T extends Comparable<? super T>> Stream<T> ofJavaStream(java.util.stream.Stream<? extends T> javaStream) {
        return Stream.ofAll(javaStream);
    }

    @Override
    protected Stream<Boolean> ofAll(boolean... elements) {
        return Stream.ofAll(elements);
    }

    @Override
    protected Stream<Byte> ofAll(byte... elements) {
        return Stream.ofAll(elements);
    }

    @Override
    protected Stream<Character> ofAll(char... elements) {
        return Stream.ofAll(elements);
    }

    @Override
    protected Stream<Double> ofAll(double... elements) {
        return Stream.ofAll(elements);
    }

    @Override
    protected Stream<Float> ofAll(float... elements) {
        return Stream.ofAll(elements);
    }

    @Override
    protected Stream<Integer> ofAll(int... elements) {
        return Stream.ofAll(elements);
    }

    @Override
    protected Stream<Long> ofAll(long... elements) {
        return Stream.ofAll(elements);
    }

    @Override
    protected Stream<Short> ofAll(short... elements) {
        return Stream.ofAll(elements);
    }

    @Override
    protected <T> Stream<T> tabulate(int n, Function<? super Integer, ? extends T> f) {
        return Stream.tabulate(n, f);
    }

    @Override
    protected <T> Stream<T> fill(int n, Supplier<? extends T> s) {
        return Stream.fill(n, s);
    }

    protected <T> Traversable<T> fill(int n, T element) {
        return Stream.fill(n, element);
    }

    @Override
    protected Stream<Character> range(char from, char toExclusive) {
        return Stream.range(from, toExclusive);
    }

    @Override
    protected Stream<Character> rangeBy(char from, char toExclusive, int step) {
        return Stream.rangeBy(from, toExclusive, step);
    }

    @Override
    protected Stream<Double> rangeBy(double from, double toExclusive, double step) {
        return Stream.rangeBy(from, toExclusive, step);
    }

    @Override
    protected Stream<Integer> range(int from, int toExclusive) {
        return Stream.range(from, toExclusive);
    }

    @Override
    protected Stream<Integer> rangeBy(int from, int toExclusive, int step) {
        return Stream.rangeBy(from, toExclusive, step);
    }

    @Override
    protected Stream<Long> range(long from, long toExclusive) {
        return Stream.range(from, toExclusive);
    }

    @Override
    protected Stream<Long> rangeBy(long from, long toExclusive, long step) {
        return Stream.rangeBy(from, toExclusive, step);
    }

    @Override
    protected Stream<Character> rangeClosed(char from, char toInclusive) {
        return Stream.rangeClosed(from, toInclusive);
    }

    @Override
    protected Stream<Character> rangeClosedBy(char from, char toInclusive, int step) {
        return Stream.rangeClosedBy(from, toInclusive, step);
    }

    @Override
    protected Stream<Double> rangeClosedBy(double from, double toInclusive, double step) {
        return Stream.rangeClosedBy(from, toInclusive, step);
    }

    @Override
    protected Stream<Integer> rangeClosed(int from, int toInclusive) {
        return Stream.rangeClosed(from, toInclusive);
    }

    @Override
    protected Stream<Integer> rangeClosedBy(int from, int toInclusive, int step) {
        return Stream.rangeClosedBy(from, toInclusive, step);
    }

    @Override
    protected Stream<Long> rangeClosed(long from, long toInclusive) {
        return Stream.rangeClosed(from, toInclusive);
    }

    @Override
    protected Stream<Long> rangeClosedBy(long from, long toInclusive, long step) {
        return Stream.rangeClosedBy(from, toInclusive, step);
    }

    protected <T> Stream<Stream<T>> transpose(Stream<Stream<T>> rows) {
        return Stream.transpose(rows);
    }

    //fixme: delete, when useIsEqualToInsteadOfIsSameAs() will be eliminated from AbstractValueTest class
    @Override
    protected boolean useIsEqualToInsteadOfIsSameAs() {
        return true;
    }

    @Test
    public void shouldRemoveNonExistingElement() {
        final Stream<Integer> t = of(1, 2, 3);
        assertThat(t.remove(4)).isEqualTo(t).isNotSameAs(t);
    }

    @Test
    public void shouldRemoveFirstElementByPredicateNonExisting() {
        final Stream<Integer> t = of(1, 2, 3);
        assertThat(t.removeFirst(v -> v == 4)).isEqualTo(t).isNotSameAs(t);
    }

    @Test
    public void shouldRemoveLastElementByPredicateNonExisting() {
        final Stream<Integer> t = of(1, 2, 3);
        assertThat(t.removeLast(v -> v == 4)).isEqualTo(t).isNotSameAs(t);
    }

    @Test
    public void shouldNotRemoveAllNonExistingElementsFromNonNil() {
        final Stream<Integer> t = of(1, 2, 3);
        assertThat(t.removeAll(of(4, 5))).isEqualTo(t).isNotSameAs(t);
    }

    @SuppressWarnings("deprecation")
    @Test
    public void shouldRemoveExistingElements() {
        final Stream<Integer> seq = of(1, 2, 3);
        assertThat(seq.removeAll(i -> i == 1)).isEqualTo(of(2, 3));
        assertThat(seq.removeAll(i -> i == 2)).isEqualTo(of(1, 3));
        assertThat(seq.removeAll(i -> i == 3)).isEqualTo(of(1, 2));
        assertThat(seq.removeAll(ignore -> true)).isEmpty();
        assertThat(seq.removeAll(ignore -> false)).isEqualTo(of(1, 2, 3)).isNotSameAs(of(1, 2, 3));
    }

    @SuppressWarnings("deprecation")
    @Test
    public void shouldNotRemoveAllNonMatchedElementsFromNonNil() {
        final Stream<Integer> t = of(1, 2, 3);
        final Predicate<Integer> isTooBig = i -> i >= 4;
        assertThat(t.removeAll(isTooBig)).isEqualTo(t).isNotSameAs(t);
    }

    @Test
    public void shouldNotRemoveAllNonObjectsElementsFromNonNil() {
        final Stream<Integer> seq = of(1, 2, 3);
        assertThat(seq.removeAll(4)).isEqualTo(seq).isNotSameAs(seq);
    }

    @Nested
    class StaticConcatTests {
        @Test
        public void shouldConcatEmptyIterableIterable() {
            final Iterable<Iterable<Integer>> empty = List.empty();
            assertThat(concat(empty)).isSameAs(empty());
        }

        @Test
        public void shouldConcatNonEmptyIterableIterable() {
            final Iterable<Iterable<Integer>> itIt = List.of(List.of(1, 2), List.of(3));
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
    class StaticFromIntTests {
        @Test
        public void shouldGenerateIntStream() {
            assertThat(Stream.from(-1).take(3)).isEqualTo(Stream.of(-1, 0, 1));
        }

        @Test
        public void shouldGenerateOverflowingIntStream() {
            //noinspection NumericOverflow
            assertThat(Stream.from(Integer.MAX_VALUE).take(2))
                    .isEqualTo(Stream.of(Integer.MAX_VALUE, Integer.MAX_VALUE + 1));
        }
    }

    @Nested
    class StaticFromIntIntTests {
        @Test
        public void shouldGenerateIntStreamWithStep() {
            assertThat(Stream.from(-1, 6).take(3)).isEqualTo(Stream.of(-1, 5, 11));
        }

        @Test
        public void shouldGenerateOverflowingIntStreamWithStep() {
            //noinspection NumericOverflow
            assertThat(Stream.from(Integer.MAX_VALUE, 2).take(2))
                    .isEqualTo(Stream.of(Integer.MAX_VALUE, Integer.MAX_VALUE + 2));
        }
    }

    @Nested
    class StaticFromLongTests {
        @Test
        public void shouldGenerateLongStream() {
            assertThat(Stream.from(-1L).take(3)).isEqualTo(Stream.of(-1L, 0L, 1L));
        }

        @Test
        public void shouldGenerateOverflowingLongStream() {
            //noinspection NumericOverflow
            assertThat(Stream.from(Long.MAX_VALUE).take(2))
                    .isEqualTo(Stream.of(Long.MAX_VALUE, Long.MAX_VALUE + 1));
        }
    }

    @Nested
    class StaticFromLongLongTests {
        @Test
        public void shouldGenerateLongStreamWithStep() {
            assertThat(Stream.from(-1L, 5L).take(3)).isEqualTo(Stream.of(-1L, 4L, 9L));
        }

        @Test
        public void shouldGenerateOverflowingLongStreamWithStep() {
            //noinspection NumericOverflow
            assertThat(Stream.from(Long.MAX_VALUE, 2).take(2))
                    .isEqualTo(Stream.of(Long.MAX_VALUE, Long.MAX_VALUE + 2));
        }
    }

    @Nested
    class StaticContinuallySupplierTests {
        @Test
        public void shouldGenerateInfiniteStreamBasedOnSupplier() {
            assertThat(Stream.continually(() -> 1).take(13).reduce((i, j) -> i + j)).isEqualTo(13);
        }
    }

    @Nested
    class StaticIterateTFunctionTests {
        @Test
        public void shouldGenerateInfiniteStreamBasedOnSupplierWithAccessToPreviousValue() {
            assertThat(Stream.iterate(2, (i) -> i + 2).take(3).reduce((i, j) -> i + j)).isEqualTo(12);
        }
    }

    @Nested
    class StaticIterateSupplierOptionTests {
        @Test
        public void shouldGenerateInfiniteStreamBasedOnOptionSupplier() {
            assertThat(Stream.iterate(() -> Option.some(1)).take(5).reduce((i, j) -> i + j)).isEqualTo(5);
        }
    }

    @Nested
    class StaticContinuallyTTests {
        @Test
        public void shouldGenerateInfiniteStreamBasedOnRepeatedElement() {
            assertThat(Stream.continually(2).take(3).reduce((i, j) -> i + j)).isEqualTo(6);
        }
    }

    @Nested
    class StaticConsTSupplierTests {
        @Test
        public void shouldBuildStreamBasedOnHeadAndTailSupplierWithAccessToHead() {
            assertThat(Stream.cons(1, () -> Stream.cons(2, Stream::empty))).isEqualTo(Stream.of(1, 2));
        }
    }

    @Nested
    class StreamStaticNarrowTests {
        @Test
        public void shouldNarrowStream() {
            final Stream<Double> doubles = of(1.0d);
            final Stream<Number> numbers = Stream.narrow(doubles);
            final int actual = numbers.append(new BigDecimal("2.0")).sum().intValue();
            assertThat(actual).isEqualTo(3);
        }
    }

    @Nested
    class StaticOfallTests {
        @Test
        public void shouldReturnSelfWhenIterableIsInstanceOfStream() {
            final Stream<Integer> source = ofAll(1, 2, 3);
            final Stream<Integer> target = Stream.ofAll(source);
            assertThat(target).isSameAs(source);
        }

        @Test
        public void shouldReturnSelfWhenIterableIsInstanceOfListView() {
            final JavaConverters.ListView<Integer, Stream<Integer>> source = JavaConverters
                    .asJava(ofAll(1, 2, 3), JavaConverters.ChangePolicy.IMMUTABLE);
            final Stream<Integer> target = Stream.ofAll(source);
            assertThat(target).isSameAs(source.getDelegate());
        }
    }

    @Nested
    class AppendTests {
        @Test
        public void shouldAppendMillionTimes() {
            final int bigNum = 1_000_000;
            assertThat(Stream.range(0, bigNum).foldLeft(Stream.empty(), Stream::append).length()).isEqualTo(bigNum);
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
            assertThat(of(1, 2, 3).appendAll(Stream.from(4)).take(6)).isEqualTo(of(1, 2, 3, 4, 5, 6));
        }

        @Test
        public void shouldAppendAllToInfiniteStream() {
            assertThat(Stream.from(1).appendAll(Stream.continually(() -> -1)).take(6)).isEqualTo(of(1, 2, 3, 4, 5, 6));
        }
    }

    @Nested
    class StreamCombinationsTests {
        @Test
        public void shouldComputeCombinationsOfEmptyStream() {
            assertThat(Stream.empty().combinations()).isEqualTo(Stream.of(Stream.empty()));
        }

        @Test
        public void shouldComputeCombinationsOfNonEmptyStream() {
            assertThat(Stream.of(1, 2, 3).combinations()).isEqualTo(Stream.of(Stream.empty(), Stream.of(1), Stream.of(2),
                    Stream.of(3), Stream.of(1, 2), Stream.of(1, 3), Stream.of(2, 3), Stream.of(1, 2, 3)));
        }
    }

    @Nested
    class StreamCombinationsKTests {
        @Test
        public void shouldComputeKCombinationsOfEmptyStream() {
            assertThat(Stream.empty().combinations(1)).isEqualTo(Stream.empty());
        }

        @Test
        public void shouldComputeKCombinationsOfNonEmptyStream() {
            assertThat(Stream.of(1, 2, 3).combinations(2))
                    .isEqualTo(Stream.of(Stream.of(1, 2), Stream.of(1, 3), Stream.of(2, 3)));
        }
    }

    @Nested
    class FlatmapTests {
        @Test
        public void shouldFlatMapInfiniteTraversable() {
            assertThat(Stream.iterate(1, i -> i + 1).flatMap(i -> List.of(i, 2 * i)).take(7))
                    .isEqualTo(Stream.of(1, 2, 2, 4, 3, 6, 4));
        }
    }

    @Nested
    class PartitionTests {
        @Test
        public void shouldPartitionInTwoIterations() {
            final AtomicInteger count = new AtomicInteger(0);
            final Tuple2<Stream<Integer>, Stream<Integer>> results = Stream.of(1, 2, 3).partition(i -> {
                count.incrementAndGet();
                return true;
            });
            assertThat(results._1()).isEqualTo(of(1, 2, 3));
            assertThat(results._2()).isEqualTo(of());
            assertThat(count.get()).isEqualTo(6);
        }

        @Test
        public void shouldPartitionLazily() {
            final java.util.Set<Integer> itemsCalled = new java.util.HashSet<>();

            final Stream<Integer> infiniteStream = Stream.iterate(0, i -> i + 1);
            assertThat(itemsCalled).isEmpty();

            final Tuple2<Stream<Integer>, Stream<Integer>> results = infiniteStream.partition(i -> {
                itemsCalled.add(i);
                return i % 2 == 0;
            });
            assertThat(itemsCalled).containsExactly(0, 1);
            assertThat(results._1().head()).isEqualTo(0);
            assertThat(results._2().head()).isEqualTo(1);
            assertThat(results._1().take(3)).isEqualTo(of(0, 2, 4));
            assertThat(results._2().take(3)).isEqualTo(of(1, 3, 5));
            assertThat(itemsCalled).containsExactly(0, 1, 2, 3, 4, 5);
        }
    }

    @Nested
    class StreamPermutationsTests {
        @Test
        public void shouldComputePermutationsOfEmptyStream() {
            assertThat(Stream.empty().permutations()).isEqualTo(Stream.empty());
        }

        @Test
        public void shouldComputePermutationsOfNonEmptyStream() {
            assertThat(Stream.of(1, 2, 3).permutations()).isEqualTo(Stream.ofAll(Stream.of(Stream.of(1, 2, 3),
                    Stream.of(1, 3, 2), Stream.of(2, 1, 3), Stream.of(2, 3, 1), Stream.of(3, 1, 2), Stream.of(3, 2, 1))));
        }
    }

    @Nested
    class AppendselfTests {
        @Test
        public void shouldRecurrentlyCalculateFibonacci() {
            assertThat(Stream.of(1, 1).appendSelf(self -> self.zip(self.tail()).map(t -> t._1() + t._2())).take(10))
                    .isEqualTo(Stream.of(1, 1, 2, 3, 5, 8, 13, 21, 34, 55));
        }

        @Test
        public void shouldRecurrentlyCalculatePrimes() {
            assertThat(Stream
                    .of(2)
                    .appendSelf(self -> Stream
                            .iterate(3, i -> i + 2)
                            .filter(i -> self.takeWhile(j -> j * j <= i).forAll(k -> i % k > 0)))
                    .take(10)).isEqualTo(Stream.of(2, 3, 5, 7, 11, 13, 17, 19, 23, 29));
        }

        @Test
        public void shouldDoNothingOnNil() {
            assertThat(Stream.empty().appendSelf(self -> self)).isEqualTo(Stream.empty());
        }

        @Test
        public void shouldRecurrentlyCalculateArithmeticProgression() {
            assertThat(Stream.of(1).appendSelf(self -> self.map(t -> t + 1)).take(4)).isEqualTo(Stream.of(1, 2, 3, 4));
        }

        @Test
        public void shouldRecurrentlyCalculateGeometricProgression() {
            assertThat(Stream.of(1).appendSelf(self -> self.map(t -> t * 2)).take(4)).isEqualTo(Stream.of(1, 2, 4, 8));
        }
    }

    @Nested
    class StreamContainssliceTests {
        @Test
        public void shouldRecognizeInfiniteDoesContainSlice() {
            final boolean actual = Stream.iterate(1, i -> i + 1).containsSlice(of(12, 13, 14));
            assertThat(actual).isTrue();
        }
    }

    @Nested
    class CycleTests {
        @Test
        public void shouldCycleEmptyStream() {
            assertThat(empty().cycle()).isEqualTo(empty());
        }

        @Test
        public void shouldCycleNonEmptyStream() {
            assertThat(of(1, 2, 3).cycle().take(9)).isEqualTo(of(1, 2, 3, 1, 2, 3, 1, 2, 3));
        }
    }

    @Nested
    class CycleIntTests {
        @Test
        public void shouldCycleTimesEmptyStream() {
            assertThat(empty().cycle(3)).isEqualTo(empty());
        }

        @Test
        public void shouldCycleTimesNonEmptyStream() {
            assertThat(of(1, 2, 3).cycle(-1)).isEqualTo(empty());
            assertThat(of(1, 2, 3).cycle(0)).isEqualTo(empty());
            assertThat(of(1, 2, 3).cycle(1)).isEqualTo(of(1, 2, 3));
            assertThat(of(1, 2, 3).cycle(3)).isEqualTo(of(1, 2, 3, 1, 2, 3, 1, 2, 3));
        }
    }

    @Nested
    class DropuntilTests {
        @Test
        public void shouldDropInfiniteStreamUntilPredicate() {
            final Stream<Integer> naturalNumbers = Stream.iterate(0, i -> i + 1);
            final Stream<Integer> naturalNumbersBiggerThanTen = naturalNumbers.dropUntil(i -> i > 10);
            final Integer firstNaturalNumberBiggerThanTen = naturalNumbersBiggerThanTen.head();
            assertThat(firstNaturalNumberBiggerThanTen).isEqualTo(11);
        }
    }

    @Nested
    class DroprightTests {
        @Test
        public void shouldLazyDropRight() {
            assertThat(Stream.from(1).takeUntil(i -> i == 18).dropRight(7)).isEqualTo(Stream.range(1, 11));
        }
    }

    @Nested
    class ExtendTests {
        @Test
        public void shouldExtendStreamWithConstantValue() {
            assertThat(Stream.of(1, 2, 3).extend(42).take(6)).isEqualTo(of(1, 2, 3, 42, 42, 42));
        }

        @Test
        public void shouldExtendStreamWithSupplier() {
            assertThat(Stream.of(1, 2, 3).extend(() -> 42).take(6)).isEqualTo(of(1, 2, 3, 42, 42, 42));
        }

        @Test
        public void shouldExtendStreamWithFunction() {
            assertThat(Stream.of(1, 2, 3).extend(i -> i + 1).take(6)).isEqualTo(of(1, 2, 3, 4, 5, 6));
        }

        @Test
        public void shouldExtendEmptyStreamWithConstantValue() {
            assertThat(Stream.of().extend(42).take(6)).isEqualTo(of(42, 42, 42, 42, 42, 42));
        }

        @Test
        public void shouldExtendEmptyStreamWithSupplier() {
            assertThat(Stream.of().extend(() -> 42).take(6)).isEqualTo(of(42, 42, 42, 42, 42, 42));
        }

        @Test
        public void shouldReturnAnEmptyStreamWhenExtendingAnEmptyStreamWithFunction() {
            assertThat(Stream.<Integer> of().extend(i -> i + 1)).isEqualTo(of());
        }

        @Test
        public void shouldReturnTheOriginalStreamWhenTryingToExtendInfiniteStreamWithConstantValue() {
            assertThat(Stream.continually(1).extend(42).take(6)).isEqualTo(of(1, 1, 1, 1, 1, 1));
        }

        @Test
        public void shouldReturnTheOriginalStreamWhenTryingToExtendInfiniteStreamWithSupplier() {
            assertThat(Stream.continually(1).extend(() -> 42).take(6)).isEqualTo(of(1, 1, 1, 1, 1, 1));
        }

        @Test
        public void shouldReturnTheOriginalStreamWhenTryingToExtendInfiniteStreamWithFunction() {
            assertThat(Stream.continually(1).extend(i -> i + 1).take(6)).isEqualTo(of(1, 1, 1, 1, 1, 1));
        }
    }


    // -- subSequence(int, int)

    @Disabled
    @Test
    public void shouldReturnSameInstanceIfSubSequenceStartsAtZeroAndEndsAtLastElement() {
        // Stream is lazy
    }

    @Nested
    class TailTests {
        @Test
        public void shouldEvaluateTailAtMostOnce() {
            final int[] counter = { 0 };
            final Stream<Integer> stream = Stream.continually(() -> counter[0]++);
            // this test ensures that the `tail.append(100)` does not modify the tail elements
            final Stream<Integer> tail = stream.tail().append(100);
            final String expected = stream.drop(1).take(3).mkString(",");
            final String actual = tail.take(3).mkString(",");
            assertThat(expected).isEqualTo("1,2,3");
            assertThat(actual).isEqualTo(expected);
        }

        @Test
        public void shouldNotProduceStackOverflow() {
            Stream.range(0, 1_000_000)
                    .map(String::valueOf)
                    .foldLeft(Stream.<String> empty(), Stream::append)
                    .mkString();
        }

        @Test // See #327, #594
        public void shouldNotEvaluateHeadOfTailWhenCallingIteratorHasNext() {

            final Integer[] vals = new Integer[] { 1, 2, 3, 4, 5, 6, 7, 8, 9 };

            final CheckedFunction2<StringBuilder, Integer, Void> doStuff = (builder, i) -> {
                builder.append(i);
                if (i == 5) {
                    throw new Exception("Some error !!!");
                } else {
                    return null;
                }
            };

            final StringBuilder actual = new StringBuilder();
            final CheckedFunction1<Integer, Void> consumer1 = doStuff.apply(actual);
            Stream.of(vals)
                    .map(v -> Try.run(() -> consumer1.apply(v)))
                    .find(Try::isFailure)
                    .getOrElse(() -> Try.success(Tuple.empty()));

            final StringBuilder expected = new StringBuilder();
            final CheckedFunction1<Integer, Void> consumer2 = doStuff.apply(expected);
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
        final Predicate<Integer> hiddenThrow = i -> {
            if (i == 0) {
                return true;
            } else {
                throw new IllegalArgumentException();
            }
        };
        assertThat(Stream.from(0).filter(hiddenThrow).take(1).sum().intValue()).isEqualTo(0);
    }

    @Disabled
    @Test
    public void shouldTakeZeroOfEmptyFilteredInfiniteStream() {
        assertThat(Stream.continually(1).filter(i -> false).take(0).isEmpty()).isTrue();
    }

    @Disabled
    @Test
    public void shouldTakeZeroOfEmptyFlatMappedInfiniteStream() {
        assertThat(Stream.continually(1).flatMap(i -> Stream.empty()).take(0).isEmpty()).isTrue();
    }

    @Disabled
    @Override
    @Test
    public void shouldReturnSameInstanceIfTakeAll() {
        // the size of a possibly infinite stream is unknown
    }

    @Nested
    class TostreamTests {
        @Test
        public void shouldReturnSelfOnConvertToStream() {
            final Traversable<Integer> value = of(1, 2, 3);
            assertThat(value.toStream()).isSameAs(value);
        }
    }

    // -- toString

    @Test
    public void shouldStringifyNil() {
        assertThat(empty().toString()).isEqualTo("Stream()");
    }

    @Test
    public void shouldStringifyNonNil() {
        assertThat(of(1, 2, 3).toString()).isEqualTo("Stream(1, ?)");
    }

    @Test
    public void shouldStringifyNonNilEvaluatingFirstTail() {
        final Stream<Integer> stream = this.of(1, 2, 3);
        stream.tail(); // evaluates second head element
        assertThat(stream.toString()).isEqualTo("Stream(1, 2, ?)");
    }

    @Test
    public void shouldStringifyNonNilAndNilTail() {
        final Stream<Integer> stream = this.of(1);
        stream.tail(); // evaluates empty tail
        assertThat(stream.toString()).isEqualTo("Stream(1)");
    }

    @Nested
    class UnfoldTests {
        @Test
        public void shouldUnfoldRightToEmpty() {
            assertThat(Stream.unfoldRight(0, x -> Option.none())).isEqualTo(empty());
        }

        @Test
        public void shouldUnfoldRightSimpleStream() {
            assertThat(
                    Stream.unfoldRight(10, x -> x == 0
                                                ? Option.none()
                                                : Option.some(new Tuple2<>(x, x - 1))))
                    .isEqualTo(of(10, 9, 8, 7, 6, 5, 4, 3, 2, 1));
        }

        @Test
        public void shouldUnfoldLeftToEmpty() {
            assertThat(Stream.unfoldLeft(0, x -> Option.none())).isEqualTo(empty());
        }

        @Test
        public void shouldUnfoldLeftSimpleStream() {
            assertThat(
                    Stream.unfoldLeft(10, x -> x == 0
                                               ? Option.none()
                                               : Option.some(new Tuple2<>(x - 1, x))))
                    .isEqualTo(of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10));
        }

        @Test
        public void shouldUnfoldToEmpty() {
            assertThat(Stream.unfold(0, x -> Option.none())).isEqualTo(empty());
        }

        @Test
        public void shouldUnfoldSimpleStream() {
            assertThat(
                    Stream.unfold(10, x -> x == 0
                                           ? Option.none()
                                           : Option.some(new Tuple2<>(x - 1, x))))
                    .isEqualTo(of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10));
        }
    }

    @Nested
    class StreamSpliteratorTests {
        @Test
        public void shouldNotHaveSizedSpliterator() {
            assertThat(of(1, 2, 3).spliterator().hasCharacteristics(Spliterator.SIZED | Spliterator.SUBSIZED)).isFalse();
        }

        @Test
        public void shouldReturnSizeWhenSpliterator() {
            assertThat(of(1, 2, 3).spliterator().getExactSizeIfKnown()).isEqualTo(-1);
        }
    }

    @Nested
    class RemoveAtLazyBoundsTests {
        @Test
        public void shouldThrowIndexOutOfBoundsWhenRemovingIndexZeroFromEmptyStream() {
            assertThrows(IndexOutOfBoundsException.class, () -> empty().removeAt(0));
        }

        @Test
        public void shouldThrowIndexOutOfBoundsWhenRemovingIndexEqualToLengthOnceTraversed() {
            assertThrows(IndexOutOfBoundsException.class, () -> of(1, 2).removeAt(2).toList());
        }
    }

    @Nested
    class DistinctByFunctionNullCheckTests {
        @Test
        public void shouldThrowWhenDistinctByKeyExtractorIsNullOnEmptyStream() {
            assertThrows(NullPointerException.class, () -> empty().distinctBy((Function<Object, Object>) null));
        }
    }

    @Nested
    class CollectTests {

        @Test
        public void shouldCollectLazilyAfterTheFirstKeptElement() {
            final AtomicInteger calls = new AtomicInteger();
            final Stream<Integer> actual = Stream.from(1).collect(i -> {
                calls.incrementAndGet();
                return i % 2 == 0 ? Option.some(i) : Option.none();
            });
            assertThat(calls.get()).isEqualTo(2); // 1 dropped, 2 kept as the head; the rest waits
            assertThat(actual.take(3)).isEqualTo(Stream.of(2, 4, 6));
        }

        @Test
        public void shouldCallTheCollectMapperOncePerElement() {
            final AtomicInteger calls = new AtomicInteger();
            Stream.of(1, 2, 3, 4).collect(i -> {
                calls.incrementAndGet();
                return i % 2 == 0 ? Option.some(i) : Option.none();
            }).toList();
            assertThat(calls.get()).isEqualTo(4);
        }

        @Test
        public void shouldReturnTheEmptyStreamWhenNothingIsKept() {
            assertThat(Stream.of(1, 2, 3).collect(i -> Option.none())).isEqualTo(Stream.empty());
        }
    }

    // -- the sequence cases, one copy per type


    @Nested
    class FillIntSupplierTests {
        @Test
        public void shouldReturnManyAfterFillWithConstantSupplier() {
            assertThat(fill(17, () -> 7))
                    .hasSize(17)
                    .containsOnly(7);
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
            assertThat(fill(17, 7))
                    .hasSize(17)
                    .containsOnly(7);
        }
    }

    // -- append

    @Test
    public void shouldAppendElementToNil() {
        final Stream<Integer> actual = this.<Integer> empty().append(1);
        final Stream<Integer> expected = of(1);
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void shouldRejectAppendOfNullElement() {
        assertThatNullPointerException().isThrownBy(() -> this.<Integer> empty().append(null));
    }

    @Test
    public void shouldAppendElementToNonNil() {
        final Stream<Integer> actual = of(1, 2).append(3);
        final Stream<Integer> expected = of(1, 2, 3);
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
        final Stream<Object> actual = empty().appendAll(empty());
        final Stream<Object> expected = empty();
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void shouldAppendAllNonNilToNil() {
        final Stream<Integer> actual = this.<Integer> empty().appendAll(of(1, 2, 3));
        final Stream<Integer> expected = of(1, 2, 3);
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void shouldAppendAllNilToNonNil() {
        final Stream<Integer> actual = of(1, 2, 3).appendAll(empty());
        final Stream<Integer> expected = of(1, 2, 3);
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void shouldAppendAllNonNilToNonNil() {
        final Stream<Integer> actual = of(1, 2, 3).appendAll(of(4, 5, 6));
        final Stream<Integer> expected = of(1, 2, 3, 4, 5, 6);
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void shouldAppendAllWhenUsedWithTypeHierarchy() {
        final Stream<SomeInterface> empty = of();
        final Stream<SomeInterface> all = empty
          .appendAll(of(OneEnum.values()))
          .appendAll(of(SecondEnum.values()));

        assertThat(all).isEqualTo(this.<SomeInterface>of(OneEnum.A1, OneEnum.A2, OneEnum.A3, SecondEnum.A1, SecondEnum.A2, SecondEnum.A3));
    }

    @Test
    public void shouldReturnSameStreamWhenEmptyAppendAllEmpty() {
        final Stream<Integer> empty = empty();
        assertThat(empty.appendAll(empty())).isSameAs(empty);
    }

    @Test
    public void shouldReturnSameStreamWhenEmptyAppendAllNonEmpty() {
        final Stream<Integer> seq = of(1, 2, 3);
        assertThat(empty().appendAll(seq)).isSameAs(seq);
    }

    @Test
    public void shouldReturnSameStreamWhenNonEmptyAppendAllEmpty() {
        final Stream<Integer> seq = of(1, 2, 3);
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
    class AsjavamutableTests {
        @Test
        public void shouldConvertAsJava() {
            final java.util.List<Integer> list = of(1, 2, 3).asJavaMutable();
            list.add(4);
            assertThat(list).isEqualTo(Arrays.asList(1, 2, 3, 4));
        }

        @Test
        public void shouldConvertAsJavaWithConsumer() {
            final Stream<Integer> seq = of(1, 2, 3).asJavaMutable(list -> {
                assertThat(list).isEqualTo(Arrays.asList(1, 2, 3));
                list.add(4);
            });
            assertThat(seq).isEqualTo(of(1, 2, 3, 4));
        }

        @Test
        public void shouldConvertAsJavaAndRethrowException() {
            assertThatThrownBy(() -> of(1, 2, 3).asJavaMutable(list -> { throw new RuntimeException("test");}))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("test");
        }

        @Test
        public void shouldConvertAsJavaImmutable() {
            final java.util.List<Integer> list = of(1, 2, 3).asJava();
            assertThat(list).isEqualTo(Arrays.asList(1, 2, 3));
            assertThatThrownBy(() -> list.add(4)).isInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        public void shouldConvertAsJavaImmutableWithConsumer() {
            final Stream<Integer> seq = of(1, 2, 3).asJava(list -> {
                assertThat(list).isEqualTo(Arrays.asList(1, 2, 3));
                assertThatThrownBy(() -> list.add(4)).isInstanceOf(UnsupportedOperationException.class);
            });
            assertThat(seq).isEqualTo(of(1, 2, 3));
        }

        @Test
        public void shouldConvertAsJavaImmutableAndRethrowException() {
            assertThatThrownBy(() -> of(1, 2, 3).asJava(list -> { throw new RuntimeException("test");}))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("test");
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
            final boolean actual = empty().containsSlice(of(1, 2, 3));
            assertThat(actual).isFalse();
        }

        @Test
        public void shouldRecognizeNonNilDoesContainSlice() {
            final boolean actual = of(1, 2, 3, 4, 5).containsSlice(of(2, 3));
            assertThat(actual).isTrue();
        }

        @Test
        public void shouldRecognizeNonNilDoesNotContainSlice() {
            final boolean actual = of(1, 2, 3, 4, 5).containsSlice(of(2, 1, 4));
            assertThat(actual).isFalse();
        }
    }

    @Nested
    class CrossproductTests {
        @Test
        public void shouldCalculateCrossProductOfNil() {
            final Iterator<Tuple2<Object, Object>> actual = empty().crossProduct();
            assertThat(actual).isEmpty();
        }

        @SuppressWarnings("unchecked")
        @Test
        public void shouldCalculateCrossProductOfNonNil() {
            final List<Tuple2<Integer, Integer>> actual = of(1, 2, 3).crossProduct().toList();
            final List<Tuple2<Integer, Integer>> expected = List.of(Tuple.of(1, 1), Tuple.of(1, 2), Tuple.of(1, 3),
                    Tuple.of(2, 1), Tuple.of(2, 2), Tuple.of(2, 3), Tuple.of(3, 1), Tuple.of(3, 2), Tuple.of(3, 3));
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
            final Iterator<Tuple2<Object, Object>> actual = empty().crossProduct(empty());
            assertThat(actual).isEmpty();
        }

        @Test
        public void shouldCalculateCrossProductOfNilAndNonNil() {
            final Iterator<Tuple2<Object, Object>> actual = empty().crossProduct(of(1, 2, 3));
            assertThat(actual).isEmpty();
        }

        @Test
        public void shouldCalculateCrossProductOfNonNilAndNil() {
            final Iterator<Tuple2<Integer, Integer>> actual = of(1, 2, 3).crossProduct(empty());
            assertThat(actual).isEmpty();
        }

        @SuppressWarnings("unchecked")
        @Test
        public void shouldCalculateCrossProductOfNonNilAndNonNil() {
            final List<Tuple2<Integer, Character>> actual = of(1, 2, 3).crossProduct(of('a', 'b')).toList();
            final List<Tuple2<Integer, Character>> expected = of(Tuple.of(1, 'a'), Tuple.of(1, 'b'),
                    Tuple.of(2, 'a'), Tuple.of(2, 'b'), Tuple.of(3, 'a'), Tuple.of(3, 'b')).toList();
            assertThat(actual).isEqualTo(expected);
        }

        @Test
        public void shouldThrowWhenCalculatingCrossProductAndThatIsNull() {
            assertThrows(NullPointerException.class, () -> empty().crossProduct(null));
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
            final Stream<Integer> values = of(1, 2, 3);
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
        final Stream<Integer> values = of(1, 2, 3);
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
        public void shouldNotFindIndexOfElementWhenStreamIsEmpty() {
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
        public void shouldNotFindIndexOfSliceWhenStreamIsEmpty() {
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
        public void shouldNotFindLastIndexOfElementWhenStreamIsEmpty() {
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
        public void shouldNotFindLastIndexOfSliceWhenStreamIsEmpty() {
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
            assertThat(of(1, 2, 3, 1, 2, 3, 4).lastIndexOfSliceOption(of(2, 3), 2)).isEqualTo(Option.some(1));
            assertThat(of(1, 2, 3, 1, 2, 3).lastIndexOfSliceOption(of(1, 2, 3), 2)).isEqualTo(Option.some(0));
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
            assertThat(of(0, 1, 2, -1, 0, 1, 2).indexWhereOption(i -> i == 0, 3)).isEqualTo(Option.some(4));
            assertThat(of(0, 1, 2, -1, 0, 1, 2).indexWhereOption(i -> i == 1, 3)).isEqualTo(Option.some(5));
            assertThat(of(0, 1, 2, -1, 0, 1, 2).indexWhereOption(i -> i == 2, 3)).isEqualTo(Option.some(6));
            assertThat(of(0, 1, 2, -1, 0, 1, 2).indexWhereOption(i -> i == 8, 3)).isEqualTo(Option.none());
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

            assertThat(of(0, 1, 2, -1, 0, 1, 2).lastIndexWhereOption(i -> i == 0)).isEqualTo(Option.some(4));
            assertThat(of(0, 1, 2, -1, 0, 1, 2).lastIndexWhereOption(i -> i == 1)).isEqualTo(Option.some(5));
            assertThat(of(0, 1, 2, -1, 0, 1, 2).lastIndexWhereOption(i -> i == 2)).isEqualTo(Option.some(6));
            assertThat(of(0, 1, 2, -1, 0, 1, 2).lastIndexWhereOption(i -> i == 8)).isEqualTo(Option.none());
            assertThat(of(0, 1, 2, -1, 0, 1, 2).lastIndexWhereOption(i -> i == 0, 3)).isEqualTo(Option.some(0));
            assertThat(of(0, 1, 2, -1, 0, 1, 2).lastIndexWhereOption(i -> i == 1, 3)).isEqualTo(Option.some(1));
            assertThat(of(0, 1, 2, -1, 0, 1, 2).lastIndexWhereOption(i -> i == 2, 3)).isEqualTo(Option.some(2));
            assertThat(of(0, 1, 2, -1, 0, 1, 2).lastIndexWhereOption(i -> i == 8, 3)).isEqualTo(Option.none());
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
        final Stream<Integer> actual = this.<Integer> empty().insert(0, 1);
        final Stream<Integer> expected = of(1);
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void shouldInsertInFrontOfElement() {
        final Stream<Integer> actual = of(4).insert(0, 1);
        final Stream<Integer> expected = of(1, 4);
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void shouldInsertBehindOfElement() {
        final Stream<Integer> actual = of(4).insert(1, 5);
        final Stream<Integer> expected = of(4, 5);
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void shouldInsertIntoStream() {
        final Stream<Integer> actual = of(1, 2, 3).insert(2, 4);
        final Stream<Integer> expected = of(1, 2, 4, 3);
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void shouldThrowWhenInsertOnNonNilWithNegativeIndex() {
        assertThrows(IndexOutOfBoundsException.class, () -> of(1).insert(-1, 9));
    }

    @Test
    public void shouldThrowWhenInsertOnNilWithNegativeIndex() {
        assertThrows(IndexOutOfBoundsException.class, () -> this.<Integer> empty().insert(-1, 9));
    }

    @Test
    public void shouldThrowOnInsertWhenExceedingUpperBound() {
        assertThrows(IndexOutOfBoundsException.class, () -> this.<Integer> empty().insert(1, 9));
    }

    // -- insertAll

    @Test
    public void shouldInsertAllIntoNil() {
        final Stream<Integer> actual = this.<Integer> empty().insertAll(0, of(1, 2, 3));
        final Stream<Integer> expected = of(1, 2, 3);
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void shouldInsertAllInFrontOfElement() {
        final Stream<Integer> actual = of(4).insertAll(0, of(1, 2, 3));
        final Stream<Integer> expected = of(1, 2, 3, 4);
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void shouldInsertAllBehindOfElement() {
        final Stream<Integer> actual = of(4).insertAll(1, of(1, 2, 3));
        final Stream<Integer> expected = of(4, 1, 2, 3);
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void shouldInsertAllIntoStream() {
        final Stream<Integer> actual = of(1, 2, 3).insertAll(2, of(4, 5));
        final Stream<Integer> expected = of(1, 2, 4, 5, 3);
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
    public void shouldReturnSameStreamWhenEmptyInsertAllEmpty() {
        final Stream<Integer> empty = empty();
        assertThat(empty.insertAll(0, empty())).isSameAs(empty);
    }

    @Test
    public void shouldReturnSameStreamWhenEmptyInsertAllNonEmpty() {
        final Stream<Integer> seq = of(1, 2, 3);
        assertThat(empty().insertAll(0, seq)).isSameAs(seq);
    }

    @Test
    public void shouldReturnSameStreamWhenNonEmptyInsertAllEmpty() {
        final Stream<Integer> seq = of(1, 2, 3);
        assertThat(seq.insertAll(0, empty())).isSameAs(seq);
    }

    // -- intersperse

    @Test
    public void shouldIntersperseNil() {
        assertThat(this.<Character> empty().intersperse(',')).isEmpty();
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
    class IteratorIntTests {
        @Test
        public void shouldThrowWhenNilIteratorStartingAtIndex() {
            assertThrows(IndexOutOfBoundsException.class, () -> empty().iterator(1));
        }

        @Test
        public void shouldIterateFirstElementOfNonNilStartingAtIndex() {
            assertThat(of(1, 2, 3).iterator(1).next()).isEqualTo(2);
        }

        @Test
        public void shouldFullyIterateNonNilStartingAtIndex() {
            int actual = -1;
            for (Iterator<Integer> iter = of(1, 2, 3).iterator(1); iter.hasNext(); ) {
                actual = iter.next();
            }
            assertThat(actual).isEqualTo(3);
        }
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
            final Stream<Integer> seq = of(1);
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
            final Stream<Integer> seq = of(1);
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
            final Stream<Character> s = of('1', '2', '3');
            assertThat(empty().patch(0, s, 0)).isEqualTo(s);
            assertThat(empty().patch(-1, s, -1)).isEqualTo(s);
            assertThat(empty().patch(-1, s, 1)).isEqualTo(s);
            assertThat(empty().patch(1, s, -1)).isEqualTo(s);
            assertThat(empty().patch(1, s, 1)).isEqualTo(s);
        }

        @Test
        public void shouldPatchNonEmptyByEmpty() {
            final Stream<Character> s = of('1', '2', '3');
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
            final Stream<Character> s = of('1', '2', '3');
            final Stream<Character> d = of('4', '5', '6');
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
        public void shouldComputePermutationsOfEmptyStream() {
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
        public void shouldComputePermutationsOfNonEmptyStream() {
            assertThat(of(1, 2, 3).permutations())
                    .isEqualTo(of(of(1, 2, 3), of(1, 3, 2), of(2, 1, 3), of(2, 3, 1), of(3, 1, 2), of(3, 2, 1)));
        }
    }

    // -- map

    @Test
    public void shouldMapTransformedStream() {
        final Function<Integer, Integer> mapper = o -> o + 1;
        assertThat(this.<Integer> empty().map(mapper)).isEmpty();
        assertThat(of(3, 1, 4, 1, 5).map(mapper)).isEqualTo(of(4, 2, 5, 2, 6));
        assertThat(of(3, 1, 4, 1, 5, 9, 2).sorted().distinct().drop(1).init().remove(5).map(mapper).tail()).isEqualTo(of(4, 5));
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
        final Stream<Integer> actual = this.<Integer> empty().prepend(1);
        final Stream<Integer> expected = of(1);
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void shouldPrependElementToNonNil() {
        final Stream<Integer> actual = of(2, 3).prepend(1);
        final Stream<Integer> expected = of(1, 2, 3);
        assertThat(actual).isEqualTo(expected);
    }

    // -- prependAll

    @Test
    public void shouldThrowOnPrependAllOfNull() {
        assertThrows(NullPointerException.class, () -> empty().prependAll(null));
    }

    @Test
    public void shouldPrependAllNilToNil() {
        final Stream<Integer> actual = this.<Integer> empty().prependAll(empty());
        final Stream<Integer> expected = empty();
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void shouldPrependAllNilToNonNil() {
        final Stream<Integer> actual = of(1, 2, 3).prependAll(empty());
        final Stream<Integer> expected = of(1, 2, 3);
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void shouldPrependAllNonNilToNil() {
        final Stream<Integer> actual = this.<Integer> empty().prependAll(of(1, 2, 3));
        final Stream<Integer> expected = of(1, 2, 3);
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void shouldPrependAllNonNilToNonNil() {
        final Stream<Integer> expected = range(0, 100);

        final Stream<Integer> actualFirstPartLarger = range(90, 100).prependAll(range(0, 90));
        assertThat(actualFirstPartLarger).isEqualTo(expected);

        final Stream<Integer> actualSecondPartLarger = range(10, 100).prependAll(range(0, 10));
        assertThat(actualSecondPartLarger).isEqualTo(expected);
    }

    @Test
    public void shouldReturnSameStreamWhenEmptyPrependAllEmpty() {
        final Stream<Integer> empty = empty();
        assertThat(empty.prependAll(empty())).isSameAs(empty);
    }

    @Test
    public void shouldReturnSameStreamWhenEmptyPrependAllNonEmpty() {
        final Stream<Integer> seq = of(1, 2, 3);
        assertThat(empty().prependAll(seq)).isSameAs(seq);
    }

    @Test
    public void shouldReturnSameStreamWhenNonEmptyPrependAllEmpty() {
        final Stream<Integer> seq = of(1, 2, 3);
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
    public void shouldReturnSameStreamWhenNonEmptyRemoveAllEmpty() {
        final Stream<Integer> seq = of(1, 2, 3);
        assertThat(seq.removeAll(empty())).isSameAs(seq);
    }

    @Test
    public void shouldReturnSameStreamWhenEmptyRemoveAllNonEmpty() {
        final Stream<Integer> empty = empty();
        assertThat(empty.removeAll(of(1, 2, 3))).isSameAs(empty);
    }

    // -- removeAll(Predicate)

    @SuppressWarnings("deprecation")
    @Test
    public void shouldRemoveNonExistingElements() {
        assertThat(this.<Integer> empty().removeAll(i -> i == 0)).isSameAs(empty());
        assertThat(of(1, 2, 3).removeAll(i -> i != 0)).isSameAs(empty());
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
        final Stream<Integer> seq = of(1, 2, 3);
        assertThat(seq.removeAll((Integer) null)).isEqualTo(seq);
    }

    @Nested
    class RemoveatIndexTests {
        @Test
        public void shouldRemoveIndexAtNil() {
            assertThrows(IndexOutOfBoundsException.class, () -> assertThat(empty().removeAt(1)).isEmpty());
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
            assertThat(of(3, 1, 4, 1, 5, 9, 2).removeAt(0).removeAt(0).removeAt(4).removeAt(3).removeAt(1)).isEqualTo(of(4, 5));
        }

        @Test
        public void shouldRemoveIndexOutOfBoundsLeft() {
            assertThrows(IndexOutOfBoundsException.class, () -> assertThat(of(1, 2, 3).removeAt(-1)).isEqualTo(of(1, 2, 3)));
        }

        @Test
        public void shouldRemoveIndexOutOfBoundsRight() {
            assertThrows(IndexOutOfBoundsException.class, () -> assertThat(of(1, 2, 3).removeAt(5)).isEqualTo(of(1, 2, 3)));
        }

        @Test
        public void shouldRemoveIndexEqualToLength() {
            assertThrows(IndexOutOfBoundsException.class, () -> assertThat(of(1, 2, 3).removeAt(3)).isEqualTo(of(1, 2, 3)));
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

    // -- reverseIterator

    @Test
    public void shouldCreateReverseIteratorOfEmpty() {
        assertThat(ofAll(empty()).reverseIterator()).isEmpty();
    }

    @Test
    public void shouldCreateReverseIteratorOfSingle() {
        assertThat(ofAll(this.of("a")).reverseIterator().toList()).isEqualTo(Iterator.of("a").toList());
    }

    @Test
    public void shouldCreateReverseIteratorOfNonEmpty() {
        assertThat(ofAll(of("a", "b", "c")).reverseIterator().toList()).isEqualTo(Iterator.of("c", "b", "a").toList());
    }

    @Nested
    class TodoRotateleftTests {
        @Test
        public void shouldRotateLeftOnEmpty() {
            assertThat(empty().rotateLeft(1)).isSameAs(empty());
        }

        @Test
        public void shouldRotateLeftOnSingle() {
            Stream<Integer> seq = of(1);
            assertThat(seq.rotateLeft(1)).isSameAs(seq);
        }

        @Test
        public void shouldRotateLeftForZero() {
            Stream<Integer> seq = of(1, 2, 3, 4, 5);
            assertThat(seq.rotateLeft(0)).isSameAs(seq);
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
            Stream<Integer> seq = of(1, 2, 3, 4, 5);
            assertThat(seq.rotateLeft(seq.length() * 3)).isSameAs(seq);
        }
    }

    @Nested
    class RotaterightTests {
        @Test
        public void shouldRotateByTheMostNegativeDistance() {
            // Integer.MIN_VALUE has no positive negation: the distance is taken modulo the length, not negated
            assertThat(of(1, 2, 3, 4).rotateLeft(Integer.MIN_VALUE)).isEqualTo(of(1, 2, 3, 4).rotateLeft(Math.floorMod(Integer.MIN_VALUE, 4)));
            assertThat(of(1, 2, 3, 4).rotateRight(Integer.MIN_VALUE)).isEqualTo(of(1, 2, 3, 4).rotateRight(Math.floorMod(Integer.MIN_VALUE, 4)));
            assertThat(of(1, 2, 3).rotateLeft(Integer.MIN_VALUE)).isEqualTo(of(2, 3, 1));
            assertThat(of(1, 2, 3).rotateRight(Integer.MIN_VALUE)).isEqualTo(of(3, 1, 2));
        }
        @Test
        public void shouldRotateRightOnEmpty() {
            assertThat(empty().rotateRight(1)).isSameAs(empty());
        }

        @Test
        public void shouldRotateRightOnSingle() {
            Stream<Integer> seq = of(1);
            assertThat(seq.rotateRight(1)).isSameAs(seq);
        }

        @Test
        public void shouldRotateRightForZero() {
            Stream<Integer> seq = of(1, 2, 3, 4, 5);
            assertThat(seq.rotateRight(0)).isSameAs(seq);
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
            Stream<Integer> seq = of(1, 2, 3, 4, 5);
            assertThat(seq.rotateRight(seq.length() * 3)).isSameAs(seq);
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
            final Stream<Integer> shuffled = of(1, 2, 3).shuffle();
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
            assertThrows(IndexOutOfBoundsException.class, () -> of(1).update(1, 2));
        }

        @Test
        public void shouldThrowWhenUpdatedWithIndexExceedingByTwoOnNonNil() {
            assertThrows(IndexOutOfBoundsException.class, () -> of(1).update(2, 2));
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
            final Stream<Character> actual = ofAll("hello".toCharArray()).update(0, Character::toUpperCase);
            final Stream<Character> expected = ofAll("Hello".toCharArray());
            assertThat(actual).isEqualTo(expected);
        }
    }

    // -- slice(beginIndex, endIndex)

    @Test
    public void shouldReturnNilWhenSliceFrom0To0OnNil() {
        final Stream<Integer> actual = this.<Integer> empty().slice(0, 0);
        assertThat(actual).isEmpty();
    }

    @Test
    public void shouldReturnNilWhenSliceFrom0To0OnNonNil() {
        final Stream<Integer> actual = of(1).slice(0, 0);
        assertThat(actual).isEmpty();
    }

    @Test
    public void shouldReturnStreamWithFirstElementWhenSliceFrom0To1OnNonNil() {
        final Stream<Integer> actual = of(1).slice(0, 1);
        assertThat(actual).isEqualTo(of(1));
    }

    @Test
    public void shouldReturnNilWhenSliceFrom1To1OnNonNil() {
        final Stream<Integer> actual = of(1).slice(1, 1);
        assertThat(actual).isEmpty();
    }

    @Test
    public void shouldReturnSliceWhenIndicesAreWithinRange() {
        final Stream<Integer> actual = of(1, 2, 3).slice(1, 3);
        assertThat(actual).isEqualTo(of(2, 3));
    }

    @Test
    public void shouldReturnNilOnSliceWhenIndicesBothAreUpperBound() {
        final Stream<Integer> actual = of(1, 2, 3).slice(3, 3);
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
        assertThat(this.<Integer> empty().sorted((i, j) -> j - i)).isEmpty();
    }

    @Test
    public void shouldSortNonNilUsingComparator() {
        assertThat(of(3, 4, 1, 2).sorted((i, j) -> j - i)).isEqualTo(of(4, 3, 2, 1));
    }

    // -- sortBy(Function)

    @Test
    public void shouldSortByNilUsingFunction() {
        assertThat(this.<String> empty().sortBy(String::length)).isEmpty();
    }

    @Test
    public void shouldSortByNonNilUsingFunction() {
        final Stream<String> testee = of("aaa", "b", "cc");
        final Stream<String> actual = testee.sortBy(String::length);
        final Stream<String> expected = of("b", "cc", "aaa");
        assertThat(actual).isEqualTo(expected);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void shouldSortByFunctionWhenElementsAreInfiniteStreams() {
        final Stream<Integer> stream1 = Stream.continually(1);
        final Stream<Integer> stream2 = Stream.continually(2);
        final Stream<Stream<Integer>> testee = of(stream2, stream1);
        final Stream<Stream<Integer>> actual = testee.sortBy(Stream::head);
        final Stream<Stream<Integer>> expected = of(stream1, stream2);
        assertThat(actual).isEqualTo(expected);
    }

    // -- sortBy(Comparator, Function)

    @Test
    public void shouldSortByNilUsingComparatorAndFunction() {
        assertThat(this.<String> empty().sortBy(String::length)).isEmpty();
    }

    @Test
    public void shouldSortByNonNilUsingComparatorAndFunction() {
        final Stream<String> testee = of("aaa", "b", "cc");
        final Stream<String> actual = testee.sortBy((i1, i2) -> i2 - i1, String::length);
        final Stream<String> expected = of("aaa", "cc", "b");
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
            assertThat(of(1, 2, 3).spliterator().hasCharacteristics(Spliterator.SORTED)).isFalse();
        }

        @Test
        public void shouldHaveOrderedSpliterator() {
            assertThat(of(1, 2, 3).spliterator().hasCharacteristics(Spliterator.ORDERED)).isTrue();
        }

        @Test
        public void shouldNotHaveDistinctSpliterator() {
            assertThat(of(1, 2, 3).spliterator().hasCharacteristics(Spliterator.DISTINCT)).isFalse();
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
        final Stream<Integer> actual = this.<Integer> empty().subSequence(0);
        assertThat(actual).isEmpty();
    }

    @Test
    public void shouldReturnIdentityWhenSubSequenceFrom0OnNonNil() {
        final Stream<Integer> actual = of(1).subSequence(0);
        assertThat(actual).isEqualTo(of(1));
    }

    @Test
    public void shouldReturnNilWhenSubSequenceFrom1OnStreamOf1() {
        final Stream<Integer> actual = of(1).subSequence(1);
        assertThat(actual).isEmpty();
    }

    @Test
    public void shouldReturnSubSequenceWhenIndexIsWithinRange() {
        final Stream<Integer> actual = of(1, 2, 3).subSequence(1);
        assertThat(actual).isEqualTo(of(2, 3));
    }

    @Test
    public void shouldReturnNilWhenSubSequenceBeginningWithSize() {
        final Stream<Integer> actual = of(1, 2, 3).subSequence(3);
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
        final Stream<Integer> seq = of(1, 2, 3);
        assertThat(seq.subSequence(0)).isSameAs(seq);
    }

    // -- subSequence(beginIndex, endIndex)

    @Test
    public void shouldReturnNilWhenSubSequenceFrom0To0OnNil() {
        final Stream<Integer> actual = this.<Integer> empty().subSequence(0, 0);
        assertThat(actual).isEmpty();
    }

    @Test
    public void shouldReturnNilWhenSubSequenceFrom0To0OnNonNil() {
        final Stream<Integer> actual = of(1).subSequence(0, 0);
        assertThat(actual).isEmpty();
    }

    @Test
    public void shouldReturnStreamWithFirstElementWhenSubSequenceFrom0To1OnNonNil() {
        final Stream<Integer> actual = of(1).subSequence(0, 1);
        assertThat(actual).isEqualTo(of(1));
    }

    @Test
    public void shouldReturnNilWhenSubSequenceFrom1To1OnNonNil() {
        final Stream<Integer> actual = of(1).subSequence(1, 1);
        assertThat(actual).isEmpty();
    }

    @Test
    public void shouldReturnSubSequenceWhenIndicesAreWithinRange() {
        final Stream<Integer> actual = of(1, 2, 3).subSequence(1, 3);
        assertThat(actual).isEqualTo(of(2, 3));
    }

    @Test
    public void shouldReturnNilWhenOnSubSequenceIndicesBothAreUpperBound() {
        final Stream<Integer> actual = of(1, 2, 3).subSequence(3, 3);
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
        assertThrows(IndexOutOfBoundsException.class, () -> of(1, 2, 3).subSequence(1, 4).mkString()); // force computation of last element, e.g. because Stream is lazy
    }

    @Test
    public void shouldThrowOnSubSequenceWhenBeginIndexIsGreaterThanEndIndex() {
        assertThrows(IllegalArgumentException.class, () -> of(1, 2, 3).subSequence(2, 1).mkString()); // force computation of last element, e.g. because Stream is lazy
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
        assertThat(this.<Integer> empty().search(42, Integer::compareTo)).isEqualTo(-1);
        assertThat(of(10, 20, 30).search(25, Integer::compareTo)).isEqualTo(-3);
    }

    @Nested
    class TransposeTests {
        @Test
        public void shouldTransposeIfEmpty() {
            final Stream<Stream<Integer>> actual = empty();
            assertThat(transpose(actual)).isSameAs(actual);
        }

        @Test
        public void shouldTransposeIfIs1x0() {
            final Stream<Stream<Integer>> actual = of(empty());
            assertThat(transpose(actual)).isSameAs(actual);
        }

        @Test
        public void shouldTransposeIfIs1x1() {
            final Stream<Stream<Integer>> actual = of(of(1));
            assertThat(transpose(actual)).isSameAs(actual);
        }

        @Test
        public void shouldTransposeIfSingleValued() {
            final Stream<Stream<Integer>> actual = of(of(0));
            final Stream<Stream<Integer>> expected = of(of(0));
            assertThat(transpose(actual)).isEqualTo(expected);
        }

        @Test
        @SuppressWarnings("unchecked")
        public void shouldTransposeIfMultiValuedColumn() {
            final Stream<Stream<Integer>> actual = of(of(0, 1, 2));
            final Stream<Stream<Integer>> expected = of(of(0), of(1), of(2));
            assertThat(transpose(actual)).isEqualTo(expected);
        }

        @Test
        @SuppressWarnings("unchecked")
        public void shouldTransposeIfMultiValuedRow() {
            final Stream<Stream<Integer>> actual = of(of(0), of(1), of(2));
            final Stream<Stream<Integer>> expected = of(of(0, 1, 2));
            assertThat(transpose(actual)).isEqualTo(expected);
        }

        @Test
        @SuppressWarnings("unchecked")
        public void shouldTransposeIfMultiValuedIfSymmetric() {
            final Stream<Stream<Integer>> actual = of(
                    of(1, 2, 3),
                    of(4, 5, 6),
                    of(7, 8, 9));
            final Stream<Stream<Integer>> expected = of(
                    of(1, 4, 7),
                    of(2, 5, 8),
                    of(3, 6, 9));
            assertThat(transpose(actual)).isEqualTo(expected);
        }

        @Test
        @SuppressWarnings("unchecked")
        public void shouldTransposeIfMultiValuedWithMoreColumnsThanRows() {
            final Stream<Stream<Integer>> actual = of(
                    of(1, 2, 3),
                    of(4, 5, 6));
            final Stream<Stream<Integer>> expected = of(
                    of(1, 4),
                    of(2, 5),
                    of(3, 6));
            assertThat(transpose(actual)).isEqualTo(expected);
        }

        @Test
        @SuppressWarnings("unchecked")
        public void shouldTransposeIfMultiValuedWithMoreRowsThanColumns() {
            final Stream<Stream<Integer>> actual = of(
                    of(1, 2),
                    of(3, 4),
                    of(5, 6));
            final Stream<Stream<Integer>> expected = of(
                    of(1, 3, 5),
                    of(2, 4, 6));
            assertThat(transpose(actual)).isEqualTo(expected);
        }

        @Test
        @SuppressWarnings("unchecked")
        public void shouldBeEqualIfTransposedTwice() {
            final Stream<Stream<Integer>> actual = of(
                    of(1, 2, 3),
                    of(4, 5, 6));
            final Stream<Stream<Integer>> transposed = transpose(actual);
            assertThat(transpose(transposed)).isEqualTo(actual);
        }

        @Test
        @SuppressWarnings("unchecked")
        public void shouldNotTransposeForMissingOrEmptyValues() {
            assertThrows(IllegalArgumentException.class, () -> {
                final Stream<Stream<Integer>> actual = of(
                  of(),
                  of(0, 1),
                  of(2, 3, 4, 5),
                  of(),
                  of(6, 7, 8));
                transpose(actual);
            });
        }
    }

    @Nested
    class NonStreamArgumentTests {
        @Test
        public void shouldStartWithANonStreamIterable() {
            assertThat(of(1, 3, 4).startsWith(List.of(1, 3))).isTrue();
            assertThat(of(1, 2, 3, 4).startsWith(List.of(1, 2, 4))).isFalse();
            assertThat(of(1, 2).startsWith(List.of(1, 2, 4))).isFalse();
        }

        @Test
        public void shouldEndWithANonStreamIterable() {
            assertThat(of(1, 3, 4).endsWith(List.of(3, 4))).isTrue();
            assertThat(of(1, 2, 3, 4).endsWith(List.of(2, 3, 5))).isFalse();
        }
    }

    // -- distinctByKeepLast(Comparator)

    @TestTemplate
    public void shouldComputeDistinctByKeepLastOfEmptyStreamUsingComparator() {
        final Comparator<Integer> comparator = comparingInt(i -> i);
        if (useIsEqualToInsteadOfIsSameAs()) {
            assertThat(this.<Integer>empty().distinctByKeepLast(comparator)).isEqualTo(empty());
        } else {
            assertThat(this.<Integer>empty().distinctByKeepLast(comparator)).isSameAs(empty());
        }
    }

    @TestTemplate
    public void shouldComputeDistinctByKeepLastOfNonEmptyStreamUsingComparator() {
        final Comparator<String> comparator = comparingInt(s -> (s.charAt(1)));
        final Stream<String> distinct = of("1a", "2a", "3b", "4b", "3a", "5c")
                .distinctByKeepLast(comparator);
        assertThat(distinct).isEqualTo(of("4b", "3a", "5c"));
    }

    @TestTemplate
    public void shouldReturnSameInstanceWhenDistinctByKeepLastComparatorEmptyStream() {
        final Stream<?> empty = empty();
        assertThat(empty.distinctByKeepLast(Comparators.naturalComparator())).isSameAs(empty);
    }

    // -- distinctByKeepLast(Function)

    @TestTemplate
    public void shouldComputeDistinctByKeepLastOfEmptyStreamUsingKeyExtractor() {
        if (useIsEqualToInsteadOfIsSameAs()) {
            assertThat(empty().distinctByKeepLast(Function.identity())).isEqualTo(empty());
        } else {
            assertThat(empty().distinctByKeepLast(Function.identity())).isSameAs(empty());
        }
    }

    @TestTemplate
    public void shouldComputeDistinctByKeepLastOfNonEmptyStreamUsingKeyExtractor() {
        final Function<String, Character> function = c -> c.charAt(1);
        final Stream<String> distinct = of("1a", "2a", "3b", "4b", "3a", "5c")
                .distinctByKeepLast(function);
        assertThat(distinct).isEqualTo(of("4b", "3a", "5c"));
    }

    @TestTemplate
    public void shouldReturnSameInstanceWhenDistinctByKeepLastFunctionEmptyStream() {
        final Stream<?> empty = empty();
        assertThat(empty.distinctByKeepLast(Function.identity())).isSameAs(empty);
    }

    private interface SomeInterface {
    }

    enum OneEnum implements SomeInterface {
        A1, A2, A3;
    }

    enum SecondEnum implements SomeInterface {
        A1, A2, A3;
    }

    // -- removeAll / retainAll with null elements go through a HashSet, whose contains must not use Option

    @Nested
    class NullElementRemoveAllTests {

        @Test
        public void shouldRejectNullOnOf() {
            assertThatNullPointerException().isThrownBy(() -> StreamTest.this.<Integer>of((Integer) null));
        }

        @Test
        public void shouldRejectNullOnOfVarargs() {
            assertThatNullPointerException().isThrownBy(() -> StreamTest.this.<Integer>of(1, null));
        }
    }

    @Nested
    class TraversableOnlyTests {
        @Test
        public void shouldImplementTraversableOnly() {
            assertThat(Traversable.class.isAssignableFrom(Stream.class)).isTrue();
            assertThat(Stream.class.getInterfaces()).containsExactly(Traversable.class);
            // no sequence interface above Traversable: a Stream reaches Traversable directly
            assertThat(supertypeNames(Stream.class)).containsExactlyInAnyOrder("Traversable", "Foldable", "Iterable");
        }

        @Test
        public void shouldHaveTheSameSupertypesOnBothCases() {
            assertThat(supertypeNames(Stream.empty().getClass())).contains("Stream", "Traversable", "Foldable", "Iterable");
            assertThat(supertypeNames(Stream.of(1).getClass())).contains("Stream", "Traversable", "Foldable", "Iterable");
        }

        @Test
        public void shouldHaveAnOrderedButNotSizedSpliterator() {
            assertThat(of(1, 2, 3).spliterator().hasCharacteristics(Spliterator.ORDERED)).isTrue();
            assertThat(of(1, 2, 3).spliterator().hasCharacteristics(Spliterator.SIZED)).isFalse();
        }
    }


    @Nested
    class LazySearchTests {

        @Test
        public void shouldSearchAnInfiniteStreamWithoutForcingItWhole() {
            final Stream<Integer> naturals = Stream.from(1);
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
            assertThat(naturals.iterator(3).take(2).toList()).isEqualTo(List.of(4, 5));
            assertThat(naturals.crossProduct(Stream.from(1)).take(3).map(Tuple2::_2).toList()).isEqualTo(List.of(1, 2, 3));
        }

        @Test
        public void shouldForceEachCellAtMostOnce() {
            final java.util.concurrent.atomic.AtomicInteger forced = new java.util.concurrent.atomic.AtomicInteger();
            final Stream<Integer> counted = Stream.from(1).map(i -> {
                forced.incrementAndGet();
                return i;
            });
            assertThat(counted.indexOf(3)).isEqualTo(2);
            final int afterFirstSearch = forced.get();
            assertThat(counted.indexOf(3)).isEqualTo(2);
            assertThat(forced.get()).isEqualTo(afterFirstSearch); // memoised: the same cells are not recomputed
        }

        @Test
        public void shouldReturnAStream() {
            final Stream<Integer> stream = Stream.of(1, 2, 3);
            assertThat(stream.rotateLeft(1)).isInstanceOf(Stream.class).isEqualTo(Stream.of(2, 3, 1));
            assertThat(stream.rotateRight(1)).isInstanceOf(Stream.class).isEqualTo(Stream.of(3, 1, 2));
            assertThat(stream.sortBy(i -> -i)).isInstanceOf(Stream.class).isEqualTo(Stream.of(3, 2, 1));
            assertThat(stream.crossProduct(2).next()).isInstanceOf(Stream.class);
        }

        @Test
        public void shouldHandleTheEmptyAndSingleCases() {
            final Stream<Integer> empty = Stream.empty();
            assertThat(empty.reverseIterator().hasNext()).isFalse();
            assertThat(empty.indexOf(1)).isEqualTo(-1);
            assertThat(empty.lastIndexOf(1)).isEqualTo(-1);
            assertThat(empty.indexWhere(i -> true)).isEqualTo(-1);
            assertThat(empty.lastIndexWhere(i -> true)).isEqualTo(-1);
            assertThat(empty.prefixLength(i -> true)).isEqualTo(0);
            assertThat(empty.segmentLength(i -> true, 0)).isEqualTo(0);
            assertThat(empty.containsSlice(Stream.empty())).isTrue();
            assertThat(empty.startsWith(Stream.empty())).isTrue();
            assertThat(empty.endsWith(Stream.empty())).isTrue();
            assertThat(empty.search(1)).isEqualTo(-1);
            assertThat(empty.crossProduct().hasNext()).isFalse();
            final Stream<Integer> one = Stream.of(1);
            assertThat(one.reverseIterator().toList()).isEqualTo(List.of(1));
            assertThat(one.crossProduct().toList().length()).isEqualTo(1);
            assertThat(one.endsWith(Stream.of(1))).isTrue();
            assertThat(one.endsWith(Stream.of(0, 1))).isFalse();
            assertThat(one.lastIndexOfSlice(Stream.of(1))).isEqualTo(0);
        }

        @Test
        public void shouldRejectNullArgumentsOfEveryNewlyDeclaredMethod() {
            final Stream<Integer> stream = Stream.of(1, 2, 3);
            assertThatNullPointerException().isThrownBy(() -> stream.containsSlice(null)).withMessage("that is null");
            assertThatNullPointerException().isThrownBy(() -> stream.crossProduct(null)).withMessage("that is null");
            assertThatNullPointerException().isThrownBy(() -> stream.endsWith(null)).withMessage("that is null");
            assertThatNullPointerException().isThrownBy(() -> stream.startsWith(null)).withMessage("that is null");
            assertThatNullPointerException().isThrownBy(() -> stream.indexOfSlice(null)).withMessage("that is null");
            assertThatNullPointerException().isThrownBy(() -> stream.lastIndexOfSlice(null)).withMessage("that is null");
            assertThatNullPointerException().isThrownBy(() -> stream.indexWhere(null)).withMessage("predicate is null");
            assertThatNullPointerException().isThrownBy(() -> stream.lastIndexWhere(null)).withMessage("predicate is null");
            assertThatNullPointerException().isThrownBy(() -> stream.segmentLength(null, 0)).withMessage("predicate is null");
            assertThatNullPointerException().isThrownBy(() -> stream.search(1, null)).withMessage("comparator is null");
            assertThatNullPointerException().isThrownBy(() -> stream.sortBy(null, Function.identity())).withMessage("comparator is null");
            assertThatNullPointerException().isThrownBy(() -> stream.sortBy(Comparator.naturalOrder(), null)).withMessage("mapper is null");
            assertThatNullPointerException().isThrownBy(() -> stream.asJava(null)).withMessage("action is null");
            assertThatNullPointerException().isThrownBy(() -> stream.asJavaMutable(null)).withMessage("action is null");
        }
    }

}
