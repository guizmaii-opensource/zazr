package com.guizmaii.zazr.collection;

import com.guizmaii.zazr.Tuple;
import com.guizmaii.zazr.Tuple2;
import com.guizmaii.zazr.control.Option;
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
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestTemplate;

import static java.util.Arrays.asList;
import static java.util.Comparator.comparingInt;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeout;

public class QueueTest extends AbstractTraversableTest {

    // -- construction

    @Override
    protected <T> Collector<T, ArrayList<T>, Queue<T>> collector() {
        return Queue.collector();
    }

    @Override
    protected <T> Queue<T> empty() {
        return Queue.empty();
    }

    @Override
    protected <T> Queue<T> of(T element) {
        return Queue.of(element);
    }

    @SuppressWarnings("varargs")
    @SafeVarargs
    @Override
    protected final <T> Queue<T> of(T... elements) {
        return Queue.of(elements);
    }

    @Override
    protected <T> Queue<T> ofAll(Iterable<? extends T> elements) {
        return Queue.ofAll(elements);
    }

    @Override
    protected <T extends Comparable<? super T>> Queue<T> ofJavaStream(java.util.stream.Stream<? extends T> javaStream) {
        return Queue.ofAll(javaStream);
    }

    @Override
    protected Queue<Boolean> ofAll(boolean... elements) {
        return Queue.ofAll(elements);
    }

    @Override
    protected Queue<Byte> ofAll(byte... elements) {
        return Queue.ofAll(elements);
    }

    @Override
    protected Queue<Character> ofAll(char... elements) {
        return Queue.ofAll(elements);
    }

    @Override
    protected Queue<Double> ofAll(double... elements) {
        return Queue.ofAll(elements);
    }

    @Override
    protected Queue<Float> ofAll(float... elements) {
        return Queue.ofAll(elements);
    }

    @Override
    protected Queue<Integer> ofAll(int... elements) {
        return Queue.ofAll(elements);
    }

    @Override
    protected Queue<Long> ofAll(long... elements) {
        return Queue.ofAll(elements);
    }

    @Override
    protected Queue<Short> ofAll(short... elements) {
        return Queue.ofAll(elements);
    }

    @Override
    protected <T> Queue<T> tabulate(int n, Function<? super Integer, ? extends T> f) {
        return Queue.tabulate(n, f);
    }

    @Override
    protected <T> Queue<T> fill(int n, Supplier<? extends T> s) {
        return Queue.fill(n, s);
    }

    protected <T> Traversable<T> fill(int n, T element) {
        return Queue.fill(n, element);
    }

    protected Queue<Character> range(char from, char toExclusive) {
        return Queue.range(from, toExclusive);
    }

    protected Queue<Character> rangeBy(char from, char toExclusive, int step) {
        return Queue.rangeBy(from, toExclusive, step);
    }

    protected Queue<Double> rangeBy(double from, double toExclusive, double step) {
        return Queue.rangeBy(from, toExclusive, step);
    }

    protected Queue<Integer> range(int from, int toExclusive) {
        return Queue.range(from, toExclusive);
    }

    protected Queue<Integer> rangeBy(int from, int toExclusive, int step) {
        return Queue.rangeBy(from, toExclusive, step);
    }

    protected Queue<Long> range(long from, long toExclusive) {
        return Queue.range(from, toExclusive);
    }

    protected Queue<Long> rangeBy(long from, long toExclusive, long step) {
        return Queue.rangeBy(from, toExclusive, step);
    }

    protected Queue<Character> rangeClosed(char from, char toInclusive) {
        return Queue.rangeClosed(from, toInclusive);
    }

    protected Queue<Character> rangeClosedBy(char from, char toInclusive, int step) {
        return Queue.rangeClosedBy(from, toInclusive, step);
    }

    protected Queue<Double> rangeClosedBy(double from, double toInclusive, double step) {
        return Queue.rangeClosedBy(from, toInclusive, step);
    }

    protected Queue<Integer> rangeClosed(int from, int toInclusive) {
        return Queue.rangeClosed(from, toInclusive);
    }

    protected Queue<Integer> rangeClosedBy(int from, int toInclusive, int step) {
        return Queue.rangeClosedBy(from, toInclusive, step);
    }

    protected Queue<Long> rangeClosed(long from, long toInclusive) {
        return Queue.rangeClosed(from, toInclusive);
    }

    protected Queue<Long> rangeClosedBy(long from, long toInclusive, long step) {
        return Queue.rangeClosedBy(from, toInclusive, step);
    }

    protected <T> Queue<Queue<T>> transpose(Queue<Queue<T>> rows) {
        return Queue.transpose(rows);
    }

    protected boolean useIsEqualToInsteadOfIsSameAs() {
        return false;
    }

    @Nested
    class QueueStaticNarrowTests {
        @Test
        public void shouldNarrowQueue() {
            final Queue<Double> doubles = of(1.0d);
            final Queue<Number> numbers = Queue.narrow(doubles);
            final int actual = numbers.enqueue(new BigDecimal("2.0")).sum().intValue();
            assertThat(actual).isEqualTo(3);
        }
    }

    @Nested
    class StaticOfallTests {
        @Test
        public void shouldReadAOneShotIterableOnce() {
            final AtomicInteger walks = new AtomicInteger();
            final Iterable<Integer> that = () -> {
                walks.incrementAndGet();
                return java.util.List.of(1, 2, 3).iterator();
            };
            assertThat(Queue.ofAll(that)).isEqualTo(Queue.of(1, 2, 3));
            assertThat(walks.get()).isEqualTo(1);
            assertThat(Queue.ofAll(java.util.stream.Stream.of(1, 2)::iterator)).isEqualTo(Queue.of(1, 2));
            assertThat(Queue.ofAll(java.util.stream.Stream.<Integer>empty()::iterator)).isSameAs(Queue.empty());
        }

        @Test
        public void shouldReturnSelfWhenIterableIsInstanceOfQueue() {
            final Queue<Integer> source = ofAll(1, 2, 3);
            final Queue<Integer> target = Queue.ofAll(source);
            assertThat(target).isSameAs(source);
        }

        @Test
        public void shouldReturnSelfWhenIterableIsInstanceOfListView() {
            final JavaConverters.ListView<Integer, Queue<Integer>> source = JavaConverters
              .asJava(ofAll(1, 2, 3), JavaConverters.ChangePolicy.IMMUTABLE);
            final Queue<Integer> target = Queue.ofAll(source);
            assertThat(target).isSameAs(source.getDelegate());
        }
    }

    @Nested
    class PartitionTests {
        @Test
        public void shouldPartitionInOneIteration() {
            final AtomicInteger count = new AtomicInteger(0);
            final Tuple2<Queue<Integer>, Queue<Integer>> results = of(1, 2, 3).partition(i -> {
                count.incrementAndGet();
                return true;
            });
            assertThat(results._1()).isEqualTo(of(1, 2, 3));
            assertThat(results._2()).isEqualTo(of());
            assertThat(count.get()).isEqualTo(3);
        }
    }

    @Nested
    class PeekTests {
        @Test
        public void shouldFailPeekOfEmpty() {
            assertThrows(NoSuchElementException.class, () -> Queue.empty().peek());
        }

        @Test
        public void shouldReturnPeekOfNonEmpty() {
            assertThat(Queue.of(1).peek()).isEqualTo(1);
        }

        @Test
        public void shouldReturnPeekOption() {
            assertThat(Queue.empty().peekOption()).isEqualTo(Option.none());
            assertThat(Queue.of(1).peekOption()).isEqualTo(Option.some(1));
        }
    }

    @Nested
    class DequeueTests {
        @Test
        public void shouldFailDequeueOfEmpty() {
            assertThrows(NoSuchElementException.class, () -> Queue.empty().dequeue());
        }

        @Test
        public void shouldDequeueOfNonEmpty() {
            assertThat(Queue.of(1, 2, 3).dequeue()).isEqualTo(Tuple.of(1, Queue.of(2, 3)));
        }

        @Test
        public void shouldDequeueOption() {
            assertThat(Queue.empty().dequeueOption()).isEqualTo(Option.none());
            assertThat(Queue.of(1, 2, 3).dequeueOption()).isEqualTo(Option.some(Tuple.of(1, Queue.of(2, 3))));
        }
    }

    // -- special cases

    private Queue<Integer> enqueued() {
        return Queue.of(1).enqueue(2, 3, 1, 5, 6);
    }

    @Nested
    class QueueGetTests {
        @Test
        public void shouldGetFrontEnc() {
            assertThat(enqueued().get(0)).isEqualTo(1);
        }

        @Test
        public void shouldGetRearEnc() {
            assertThat(enqueued().get(1)).isEqualTo(2);
        }
    }

    @Nested
    class TakeTests {
        @Test
        public void shouldTakeFrontEnc() {
            assertThat(enqueued().take(1)).isEqualTo(of(1));
        }
    }

    @Nested
    class InsertallTests {
        @Test
        public void shouldInsertAllEnc() {
            assertThat(enqueued().insertAll(0, List.of(91, 92))).isEqualTo(of(91, 92, 1, 2, 3, 1, 5, 6));
            assertThat(enqueued().insertAll(1, List.of(91, 92))).isEqualTo(of(1, 91, 92, 2, 3, 1, 5, 6));
            assertThat(enqueued().insertAll(2, List.of(91, 92))).isEqualTo(of(1, 2, 91, 92, 3, 1, 5, 6));
            assertThat(enqueued().insertAll(6, List.of(91, 92))).isEqualTo(of(1, 2, 3, 1, 5, 6, 91, 92));
        }
    }

    @Nested
    class InsertTests {
        @Test
        public void shouldInsertEnc() {
            assertThat(enqueued().insert(0, 9)).isEqualTo(of(9, 1, 2, 3, 1, 5, 6));
            assertThat(enqueued().insert(1, 9)).isEqualTo(of(1, 9, 2, 3, 1, 5, 6));
            assertThat(enqueued().insert(2, 9)).isEqualTo(of(1, 2, 9, 3, 1, 5, 6));
            assertThat(enqueued().insert(6, 9)).isEqualTo(of(1, 2, 3, 1, 5, 6, 9));
        }
    }

    @Nested
    class IntersperseTests {
        @Test
        public void shouldIntersperseEnc() {
            assertThat(enqueued().intersperse(9)).isEqualTo(of(1, 9, 2, 9, 3, 9, 1, 9, 5, 9, 6));
        }
    }

    @Nested
    class QueueIndexofTests {
        @Test
        public void shouldNotFindIndexOfElementWhenStartIsGreaterEnc() {
            assertThat(enqueued().indexOf(2, 2)).isEqualTo(-1);

            assertThat(enqueued().indexOfOption(2, 2)).isEqualTo(Option.none());
        }

        @Test
        public void shouldFindIndexOfFirstElementEnc() {
            assertThat(enqueued().indexOf(1)).isEqualTo(0);

            assertThat(enqueued().indexOfOption(1)).isEqualTo(Option.some(0));
        }

        @Test
        public void shouldFindIndexOfInnerElementEnc() {
            assertThat(enqueued().indexOf(2)).isEqualTo(1);

            assertThat(enqueued().indexOfOption(2)).isEqualTo(Option.some(1));
        }

        @Test
        public void shouldFindIndexOfLastElementEnc() {
            assertThat(enqueued().indexOf(3)).isEqualTo(2);

            assertThat(enqueued().indexOfOption(3)).isEqualTo(Option.some(2));
        }
    }

    @Nested
    class QueueLastindexofTests {
        @Test
        public void shouldNotFindLastIndexOfElementWhenEndIdLessEnc() {
            assertThat(enqueued().lastIndexOf(3, 1)).isEqualTo(-1);

            assertThat(enqueued().lastIndexOfOption(3, 1)).isEqualTo((Option.none()));
        }

        @Test
        public void shouldFindLastIndexOfElementEnc() {
            assertThat(enqueued().lastIndexOf(1)).isEqualTo(3);
        }

        @Test
        public void shouldFindLastIndexOfElementWithEndEnc() {
            assertThat(enqueued().lastIndexOf(1, 1)).isEqualTo(0);

            assertThat(enqueued().lastIndexOfOption(1, 1)).isEqualTo(Option.some(0));
        }
    }

    @Nested
    class UnfoldTests {
        @Test
        public void shouldUnfoldRightToEmpty() {
            assertThat(Queue.unfoldRight(0, x -> Option.none())).isEqualTo(empty());
        }

        @Test
        public void shouldUnfoldRightSimpleQueue() {
            assertThat(Queue.unfoldRight(10, x ->
              x == 0 ? Option.none() : Option.some(new Tuple2<>(x, x - 1)))
            ).isEqualTo(of(10, 9, 8, 7, 6, 5, 4, 3, 2, 1));
        }

        @Test
        public void shouldUnfoldLeftToEmpty() {
            assertThat(Queue.unfoldLeft(0, x -> Option.none())).isEqualTo(empty());
        }

        @Test
        public void shouldUnfoldLeftSimpleQueue() {
            assertThat(Queue.unfoldLeft(10, x ->
              x == 0 ? Option.none() : Option.some(new Tuple2<>(x - 1, x)))
            ).isEqualTo(of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10));
        }

        @Test
        public void shouldUnfoldToEmpty() {
            assertThat(Queue.unfold(0, x -> Option.none())).isEqualTo(empty());
        }

        @Test
        public void shouldUnfoldSimpleQueue() {
            assertThat(Queue.unfold(10, x ->
              x == 0 ? Option.none() : Option.some(new Tuple2<>(x - 1, x)))
            ).isEqualTo(of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10));
        }
    }

    @Nested
    class EqualsTests {
        @Test
        public void shouldCheckHashCodeWhenComparing() {
            assertThat(Queue.of(0, 1).equals(Queue.of(0, 0))).isFalse();
        }
    }

    @Nested
    class ToqueueTests {
        @Test
        public void shouldReturnSelfOnConvertToQueue() {
            final Queue<Integer> value = of(1, 2, 3);
            assertThat(value.toQueue()).isSameAs(value);
        }
    }

    @Nested
    class QueueSpliteratorTests {
        @Test
        public void shouldHaveSizedSpliterator() {
            assertThat(of(1, 2, 3).spliterator().hasCharacteristics(Spliterator.SIZED | Spliterator.SUBSIZED)).isTrue();
        }

        @Test
        public void shouldReturnSizeWhenSpliterator() {
            assertThat(of(1, 2, 3).spliterator().getExactSizeIfKnown()).isEqualTo(3);
        }
    }

    @Nested
    class FlatmapTests {
        @Test
        public void shouldFlatMapCorrectlyWhenRearIsNonEmpty() {
            Queue<Integer> queue = Queue.of(1, 2).enqueue(3).enqueue(4);

            Queue<Integer> result = queue.flatMap(x -> Queue.of(x, x * 10));

            assertThat(result).isEqualTo(Queue.of(1, 10, 2, 20, 3, 30, 4, 40));
        }
    }

    @Nested
    class ReplaceTests {
        @Test
        public void shouldReplaceOnlyFirstOccurrenceWhenRearIsNonEmpty() {
            Queue<Integer> queue = Queue.of(1, 2, 3).enqueue(3);

            Queue<Integer> result = queue.replace(3, 42);

            assertThat(result).isEqualTo(Queue.of(1, 2, 42, 3));
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
        final Queue<Integer> actual = this.<Integer> empty().append(1);
        final Queue<Integer> expected = of(1);
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void shouldRejectAppendOfNullElement() {
        assertThatNullPointerException().isThrownBy(() -> this.<Integer> empty().append(null));
    }

    @Test
    public void shouldAppendElementToNonNil() {
        final Queue<Integer> actual = of(1, 2).append(3);
        final Queue<Integer> expected = of(1, 2, 3);
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
        final Queue<Object> actual = empty().appendAll(empty());
        final Queue<Object> expected = empty();
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void shouldAppendAllNonNilToNil() {
        final Queue<Integer> actual = this.<Integer> empty().appendAll(of(1, 2, 3));
        final Queue<Integer> expected = of(1, 2, 3);
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void shouldAppendAllNilToNonNil() {
        final Queue<Integer> actual = of(1, 2, 3).appendAll(empty());
        final Queue<Integer> expected = of(1, 2, 3);
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void shouldAppendAllNonNilToNonNil() {
        final Queue<Integer> actual = of(1, 2, 3).appendAll(of(4, 5, 6));
        final Queue<Integer> expected = of(1, 2, 3, 4, 5, 6);
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void shouldAppendAllWhenUsedWithTypeHierarchy() {
        final Queue<SomeInterface> empty = of();
        final Queue<SomeInterface> all = empty
          .appendAll(of(OneEnum.values()))
          .appendAll(of(SecondEnum.values()));

        assertThat(all).isEqualTo(this.<SomeInterface>of(OneEnum.A1, OneEnum.A2, OneEnum.A3, SecondEnum.A1, SecondEnum.A2, SecondEnum.A3));
    }

    @Test
    public void shouldReturnSameQueueWhenEmptyAppendAllEmpty() {
        final Queue<Integer> empty = empty();
        assertThat(empty.appendAll(empty())).isSameAs(empty);
    }

    @Test
    public void shouldReturnSameQueueWhenEmptyAppendAllNonEmpty() {
        final Queue<Integer> seq = of(1, 2, 3);
        assertThat(empty().appendAll(seq)).isSameAs(seq);
    }

    @Test
    public void shouldReturnSameQueueWhenNonEmptyAppendAllEmpty() {
        final Queue<Integer> seq = of(1, 2, 3);
        assertThat(seq.appendAll(empty())).isSameAs(seq);
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
            final Queue<Integer> seq = of(1, 2, 3).asJavaMutable(list -> {
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
            final Queue<Integer> seq = of(1, 2, 3).asJava(list -> {
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
            final Queue<Tuple2<Object, Object>> actual = empty().crossProduct();
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
        public void shouldCalculateCrossProductPowerNow() {
            final Queue<Queue<Integer>> actual = range(0, 3).crossProduct(3);
            assertThat(actual).isInstanceOf(Queue.class);
            assertThat(actual.size()).isEqualTo(27);
            assertThat(actual.head()).isEqualTo(tabulate(3, i -> 0));
            assertThat(actual.last()).isEqualTo(tabulate(3, i -> 2));
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
            final Queue<Tuple2<Object, Object>> actual = empty().crossProduct(empty());
            assertThat(actual).isEmpty();
        }

        @Test
        public void shouldCalculateCrossProductOfNilAndNonNil() {
            final Queue<Tuple2<Object, Object>> actual = empty().crossProduct(of(1, 2, 3));
            assertThat(actual).isEmpty();
        }

        @Test
        public void shouldCalculateCrossProductOfNonNilAndNil() {
            final Queue<Tuple2<Integer, Integer>> actual = of(1, 2, 3).crossProduct(empty());
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

        @Test
        public void shouldCalculateCrossProductWithAOneShotArgument() {
            // a java.util.stream can be iterated once: the argument is read exactly once
            final Iterable<Character> oneShot = java.util.stream.Stream.of('a', 'b')::iterator;
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
            final Queue<Integer> values = of(1, 2, 3);
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
        final Queue<Integer> values = of(1, 2, 3);
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
        public void shouldNotFindIndexOfElementWhenQueueIsEmpty() {
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
        public void shouldNotFindIndexOfSliceWhenQueueIsEmpty() {
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
        public void shouldNotFindLastIndexOfElementWhenQueueIsEmpty() {
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
        public void shouldNotFindLastIndexOfSliceWhenQueueIsEmpty() {
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
        final Queue<Integer> actual = this.<Integer> empty().insert(0, 1);
        final Queue<Integer> expected = of(1);
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void shouldInsertInFrontOfElement() {
        final Queue<Integer> actual = of(4).insert(0, 1);
        final Queue<Integer> expected = of(1, 4);
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void shouldInsertBehindOfElement() {
        final Queue<Integer> actual = of(4).insert(1, 5);
        final Queue<Integer> expected = of(4, 5);
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void shouldInsertIntoQueue() {
        final Queue<Integer> actual = of(1, 2, 3).insert(2, 4);
        final Queue<Integer> expected = of(1, 2, 4, 3);
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
        final Queue<Integer> actual = this.<Integer> empty().insertAll(0, of(1, 2, 3));
        final Queue<Integer> expected = of(1, 2, 3);
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void shouldInsertAllInFrontOfElement() {
        final Queue<Integer> actual = of(4).insertAll(0, of(1, 2, 3));
        final Queue<Integer> expected = of(1, 2, 3, 4);
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void shouldInsertAllBehindOfElement() {
        final Queue<Integer> actual = of(4).insertAll(1, of(1, 2, 3));
        final Queue<Integer> expected = of(4, 1, 2, 3);
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void shouldInsertAllIntoQueue() {
        final Queue<Integer> actual = of(1, 2, 3).insertAll(2, of(4, 5));
        final Queue<Integer> expected = of(1, 2, 4, 5, 3);
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
    public void shouldReturnSameQueueWhenEmptyInsertAllEmpty() {
        final Queue<Integer> empty = empty();
        assertThat(empty.insertAll(0, empty())).isSameAs(empty);
    }

    @Test
    public void shouldReturnSameQueueWhenEmptyInsertAllNonEmpty() {
        final Queue<Integer> seq = of(1, 2, 3);
        assertThat(empty().insertAll(0, seq)).isSameAs(seq);
    }

    @Test
    public void shouldReturnSameQueueWhenNonEmptyInsertAllEmpty() {
        final Queue<Integer> seq = of(1, 2, 3);
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
            final Queue<Integer> seq = of(1);
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
            final Queue<Integer> seq = of(1);
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
            final Queue<Character> s = of('1', '2', '3');
            assertThat(empty().patch(0, s, 0)).isEqualTo(s);
            assertThat(empty().patch(-1, s, -1)).isEqualTo(s);
            assertThat(empty().patch(-1, s, 1)).isEqualTo(s);
            assertThat(empty().patch(1, s, -1)).isEqualTo(s);
            assertThat(empty().patch(1, s, 1)).isEqualTo(s);
        }

        @Test
        public void shouldPatchNonEmptyByEmpty() {
            final Queue<Character> s = of('1', '2', '3');
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
            final Queue<Character> s = of('1', '2', '3');
            final Queue<Character> d = of('4', '5', '6');
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
        public void shouldComputePermutationsOfEmptyQueue() {
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
        public void shouldComputePermutationsOfNonEmptyQueue() {
            assertThat(of(1, 2, 3).permutations())
                    .isEqualTo(of(of(1, 2, 3), of(1, 3, 2), of(2, 1, 3), of(2, 3, 1), of(3, 1, 2), of(3, 2, 1)));
        }
    }

    // -- map

    @Test
    public void shouldMapTransformedQueue() {
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
        final Queue<Integer> actual = this.<Integer> empty().prepend(1);
        final Queue<Integer> expected = of(1);
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void shouldPrependElementToNonNil() {
        final Queue<Integer> actual = of(2, 3).prepend(1);
        final Queue<Integer> expected = of(1, 2, 3);
        assertThat(actual).isEqualTo(expected);
    }

    // -- prependAll

    @Test
    public void shouldThrowOnPrependAllOfNull() {
        assertThrows(NullPointerException.class, () -> empty().prependAll(null));
    }

    @Test
    public void shouldPrependAllNilToNil() {
        final Queue<Integer> actual = this.<Integer> empty().prependAll(empty());
        final Queue<Integer> expected = empty();
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void shouldPrependAllNilToNonNil() {
        final Queue<Integer> actual = of(1, 2, 3).prependAll(empty());
        final Queue<Integer> expected = of(1, 2, 3);
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void shouldPrependAllNonNilToNil() {
        final Queue<Integer> actual = this.<Integer> empty().prependAll(of(1, 2, 3));
        final Queue<Integer> expected = of(1, 2, 3);
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void shouldPrependAllNonNilToNonNil() {
        final Queue<Integer> expected = range(0, 100);

        final Queue<Integer> actualFirstPartLarger = range(90, 100).prependAll(range(0, 90));
        assertThat(actualFirstPartLarger).isEqualTo(expected);

        final Queue<Integer> actualSecondPartLarger = range(10, 100).prependAll(range(0, 10));
        assertThat(actualSecondPartLarger).isEqualTo(expected);
    }

    @Test
    public void shouldReturnSameQueueWhenEmptyPrependAllEmpty() {
        final Queue<Integer> empty = empty();
        assertThat(empty.prependAll(empty())).isSameAs(empty);
    }

    @Test
    public void shouldReturnSameQueueWhenEmptyPrependAllNonEmpty() {
        final Queue<Integer> seq = of(1, 2, 3);
        assertThat(empty().prependAll(seq)).isSameAs(seq);
    }

    @Test
    public void shouldReturnSameQueueWhenNonEmptyPrependAllEmpty() {
        final Queue<Integer> seq = of(1, 2, 3);
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

    @Test
    public void shouldRemoveNonExistingElement() {
        final Queue<Integer> t = of(1, 2, 3);
        assertThat(t.remove(4)).isSameAs(t);
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

    @Test
    public void shouldRemoveFirstElementByPredicateNonExisting() {
        final Queue<Integer> t = of(1, 2, 3);
        assertThat(t.removeFirst(v -> v == 4)).isSameAs(t);
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

    @Test
    public void shouldRemoveLastElementByPredicateNonExisting() {
        final Queue<Integer> t = of(1, 2, 3);
        assertThat(t.removeLast(v -> v == 4)).isSameAs(t);
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
    public void shouldNotRemoveAllNonExistingElementsFromNonNil() {
        final Queue<Integer> t = of(1, 2, 3);
        assertThat(t.removeAll(of(4, 5))).isSameAs(t);
    }

    @Test
    public void shouldReturnSameQueueWhenNonEmptyRemoveAllEmpty() {
        final Queue<Integer> seq = of(1, 2, 3);
        assertThat(seq.removeAll(empty())).isSameAs(seq);
    }

    @Test
    public void shouldReturnSameQueueWhenEmptyRemoveAllNonEmpty() {
        final Queue<Integer> empty = empty();
        assertThat(empty.removeAll(of(1, 2, 3))).isSameAs(empty);
    }

    // -- removeAll(Predicate)

    @SuppressWarnings("deprecation")
    @Test
    public void shouldRemoveExistingElements() {
        final Queue<Integer> seq = of(1, 2, 3);
        assertThat(seq.removeAll(i -> i == 1)).isEqualTo(of(2, 3));
        assertThat(seq.removeAll(i -> i == 2)).isEqualTo(of(1, 3));
        assertThat(seq.removeAll(i -> i == 3)).isEqualTo(of(1, 2));
        assertThat(seq.removeAll(ignore -> true)).isEmpty();
        assertThat(seq.removeAll(ignore -> false)).isSameAs(seq);
    }

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

    @SuppressWarnings("deprecation")
    @Test
    public void shouldNotRemoveAllNonMatchedElementsFromNonNil() {
        final Queue<Integer> t = of(1, 2, 3);
        final Predicate<Integer> isTooBig = i -> i >= 4;
        assertThat(t.removeAll(isTooBig)).isSameAs(t);
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
    public void shouldNotRemoveAllNonObjectsElementsFromNonNil() {
        final Queue<Integer> seq = of(1, 2, 3);
        assertThat(seq.removeAll(4)).isSameAs(seq);
    }

    @Test
    public void shouldNotRemoveAbsentNullFromNonEmpty() {
        final Queue<Integer> seq = of(1, 2, 3);
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

    // -- reverse().iterator()

    @Test
    public void shouldIterateEmptyInReverse() {
        assertThat(ofAll(empty()).reverse().iterator().hasNext()).isFalse();
    }

    @Test
    public void shouldIterateSingleInReverse() {
        final java.util.Iterator<String> iterator = ofAll(this.of("a")).reverse().iterator();
        assertThat(List.ofAll(() -> iterator)).isEqualTo(List.of("a"));
    }

    @Test
    public void shouldIterateNonEmptyInReverse() {
        final java.util.Iterator<String> iterator = ofAll(of("a", "b", "c")).reverse().iterator();
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
            Queue<Integer> seq = of(1);
            assertThat(seq.rotateLeft(1)).isSameAs(seq);
        }

        @Test
        public void shouldRotateLeftForZero() {
            Queue<Integer> seq = of(1, 2, 3, 4, 5);
            assertThat(seq.rotateLeft(0)).isSameAs(seq);
        }

        @Test
        public void shouldRotateByZeroWithoutWalkingTheElements() {
            // the zero fast path answers before length(), which walks a Queue
            final Queue<Integer> none = empty();
            final Queue<Integer> one = of(1);
            final Queue<Integer> many = of(1, 2, 3, 4, 5);
            assertThat(none.rotateLeft(0)).isSameAs(none);
            assertThat(none.rotateRight(0)).isSameAs(none);
            assertThat(one.rotateLeft(0)).isSameAs(one);
            assertThat(one.rotateRight(0)).isSameAs(one);
            assertThat(many.rotateLeft(0)).isSameAs(many);
            assertThat(many.rotateRight(0)).isSameAs(many);
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
            Queue<Integer> seq = of(1, 2, 3, 4, 5);
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
            Queue<Integer> seq = of(1);
            assertThat(seq.rotateRight(1)).isSameAs(seq);
        }

        @Test
        public void shouldRotateRightForZero() {
            Queue<Integer> seq = of(1, 2, 3, 4, 5);
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
            Queue<Integer> seq = of(1, 2, 3, 4, 5);
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
            final Queue<Integer> shuffled = of(1, 2, 3).shuffle();
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
            final Queue<Character> actual = ofAll("hello".toCharArray()).update(0, Character::toUpperCase);
            final Queue<Character> expected = ofAll("Hello".toCharArray());
            assertThat(actual).isEqualTo(expected);
        }
    }

    // -- slice(beginIndex, endIndex)

    @Test
    public void shouldReturnNilWhenSliceFrom0To0OnNil() {
        final Queue<Integer> actual = this.<Integer> empty().slice(0, 0);
        assertThat(actual).isEmpty();
    }

    @Test
    public void shouldReturnNilWhenSliceFrom0To0OnNonNil() {
        final Queue<Integer> actual = of(1).slice(0, 0);
        assertThat(actual).isEmpty();
    }

    @Test
    public void shouldReturnQueueWithFirstElementWhenSliceFrom0To1OnNonNil() {
        final Queue<Integer> actual = of(1).slice(0, 1);
        assertThat(actual).isEqualTo(of(1));
    }

    @Test
    public void shouldReturnNilWhenSliceFrom1To1OnNonNil() {
        final Queue<Integer> actual = of(1).slice(1, 1);
        assertThat(actual).isEmpty();
    }

    @Test
    public void shouldReturnSliceWhenIndicesAreWithinRange() {
        final Queue<Integer> actual = of(1, 2, 3).slice(1, 3);
        assertThat(actual).isEqualTo(of(2, 3));
    }

    @Test
    public void shouldReturnNilOnSliceWhenIndicesBothAreUpperBound() {
        final Queue<Integer> actual = of(1, 2, 3).slice(3, 3);
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
        final Queue<String> testee = of("aaa", "b", "cc");
        final Queue<String> actual = testee.sortBy(String::length);
        final Queue<String> expected = of("b", "cc", "aaa");
        assertThat(actual).isEqualTo(expected);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void shouldSortByFunctionWhenElementsAreInfiniteStreams() {
        final Stream<Integer> stream1 = Stream.continually(1);
        final Stream<Integer> stream2 = Stream.continually(2);
        final Queue<Stream<Integer>> testee = of(stream2, stream1);
        final Queue<Stream<Integer>> actual = testee.sortBy(Stream::head);
        final Queue<Stream<Integer>> expected = of(stream1, stream2);
        assertThat(actual).isEqualTo(expected);
    }

    // -- sortBy(Comparator, Function)

    @Test
    public void shouldSortByNilUsingComparatorAndFunction() {
        assertThat(this.<String> empty().sortBy(String::length)).isEmpty();
    }

    @Test
    public void shouldSortByNonNilUsingComparatorAndFunction() {
        final Queue<String> testee = of("aaa", "b", "cc");
        final Queue<String> actual = testee.sortBy((i1, i2) -> i2 - i1, String::length);
        final Queue<String> expected = of("aaa", "cc", "b");
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
        final Queue<Integer> actual = this.<Integer> empty().subSequence(0);
        assertThat(actual).isEmpty();
    }

    @Test
    public void shouldReturnIdentityWhenSubSequenceFrom0OnNonNil() {
        final Queue<Integer> actual = of(1).subSequence(0);
        assertThat(actual).isEqualTo(of(1));
    }

    @Test
    public void shouldReturnNilWhenSubSequenceFrom1OnQueueOf1() {
        final Queue<Integer> actual = of(1).subSequence(1);
        assertThat(actual).isEmpty();
    }

    @Test
    public void shouldReturnSubSequenceWhenIndexIsWithinRange() {
        final Queue<Integer> actual = of(1, 2, 3).subSequence(1);
        assertThat(actual).isEqualTo(of(2, 3));
    }

    @Test
    public void shouldReturnNilWhenSubSequenceBeginningWithSize() {
        final Queue<Integer> actual = of(1, 2, 3).subSequence(3);
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
        final Queue<Integer> seq = of(1, 2, 3);
        assertThat(seq.subSequence(0)).isSameAs(seq);
    }

    // -- subSequence(beginIndex, endIndex)

    @Test
    public void shouldReturnNilWhenSubSequenceFrom0To0OnNil() {
        final Queue<Integer> actual = this.<Integer> empty().subSequence(0, 0);
        assertThat(actual).isEmpty();
    }

    @Test
    public void shouldReturnNilWhenSubSequenceFrom0To0OnNonNil() {
        final Queue<Integer> actual = of(1).subSequence(0, 0);
        assertThat(actual).isEmpty();
    }

    @Test
    public void shouldReturnQueueWithFirstElementWhenSubSequenceFrom0To1OnNonNil() {
        final Queue<Integer> actual = of(1).subSequence(0, 1);
        assertThat(actual).isEqualTo(of(1));
    }

    @Test
    public void shouldReturnNilWhenSubSequenceFrom1To1OnNonNil() {
        final Queue<Integer> actual = of(1).subSequence(1, 1);
        assertThat(actual).isEmpty();
    }

    @Test
    public void shouldReturnSubSequenceWhenIndicesAreWithinRange() {
        final Queue<Integer> actual = of(1, 2, 3).subSequence(1, 3);
        assertThat(actual).isEqualTo(of(2, 3));
    }

    @Test
    public void shouldReturnNilWhenOnSubSequenceIndicesBothAreUpperBound() {
        final Queue<Integer> actual = of(1, 2, 3).subSequence(3, 3);
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

    @Test
    public void shouldReturnSameInstanceIfSubSequenceStartsAtZeroAndEndsAtLastElement() {
        final Queue<Integer> seq = of(1, 2, 3);
        assertThat(seq.subSequence(0, 3)).isSameAs(seq);
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
            final Queue<Queue<Integer>> actual = empty();
            assertThat(transpose(actual)).isSameAs(actual);
        }

        @Test
        public void shouldTransposeIfIs1x0() {
            final Queue<Queue<Integer>> actual = of(empty());
            assertThat(transpose(actual)).isSameAs(actual);
        }

        @Test
        public void shouldTransposeIfIs1x1() {
            final Queue<Queue<Integer>> actual = of(of(1));
            assertThat(transpose(actual)).isSameAs(actual);
        }

        @Test
        public void shouldTransposeIfSingleValued() {
            final Queue<Queue<Integer>> actual = of(of(0));
            final Queue<Queue<Integer>> expected = of(of(0));
            assertThat(transpose(actual)).isEqualTo(expected);
        }

        @Test
        @SuppressWarnings("unchecked")
        public void shouldTransposeIfMultiValuedColumn() {
            final Queue<Queue<Integer>> actual = of(of(0, 1, 2));
            final Queue<Queue<Integer>> expected = of(of(0), of(1), of(2));
            assertThat(transpose(actual)).isEqualTo(expected);
        }

        @Test
        @SuppressWarnings("unchecked")
        public void shouldTransposeIfMultiValuedRow() {
            final Queue<Queue<Integer>> actual = of(of(0), of(1), of(2));
            final Queue<Queue<Integer>> expected = of(of(0, 1, 2));
            assertThat(transpose(actual)).isEqualTo(expected);
        }

        @Test
        @SuppressWarnings("unchecked")
        public void shouldTransposeIfMultiValuedIfSymmetric() {
            final Queue<Queue<Integer>> actual = of(
                    of(1, 2, 3),
                    of(4, 5, 6),
                    of(7, 8, 9));
            final Queue<Queue<Integer>> expected = of(
                    of(1, 4, 7),
                    of(2, 5, 8),
                    of(3, 6, 9));
            assertThat(transpose(actual)).isEqualTo(expected);
        }

        @Test
        @SuppressWarnings("unchecked")
        public void shouldTransposeIfMultiValuedWithMoreColumnsThanRows() {
            final Queue<Queue<Integer>> actual = of(
                    of(1, 2, 3),
                    of(4, 5, 6));
            final Queue<Queue<Integer>> expected = of(
                    of(1, 4),
                    of(2, 5),
                    of(3, 6));
            assertThat(transpose(actual)).isEqualTo(expected);
        }

        @Test
        @SuppressWarnings("unchecked")
        public void shouldTransposeIfMultiValuedWithMoreRowsThanColumns() {
            final Queue<Queue<Integer>> actual = of(
                    of(1, 2),
                    of(3, 4),
                    of(5, 6));
            final Queue<Queue<Integer>> expected = of(
                    of(1, 3, 5),
                    of(2, 4, 6));
            assertThat(transpose(actual)).isEqualTo(expected);
        }

        @Test
        @SuppressWarnings("unchecked")
        public void shouldBeEqualIfTransposedTwice() {
            final Queue<Queue<Integer>> actual = of(
                    of(1, 2, 3),
                    of(4, 5, 6));
            final Queue<Queue<Integer>> transposed = transpose(actual);
            assertThat(transpose(transposed)).isEqualTo(actual);
        }

        @Test
        @SuppressWarnings("unchecked")
        public void shouldNotTransposeForMissingOrEmptyValues() {
            assertThrows(IllegalArgumentException.class, () -> {
                final Queue<Queue<Integer>> actual = of(
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
    class NonQueueArgumentTests {
        @Test
        public void shouldStartWithANonQueueIterable() {
            assertThat(of(1, 3, 4).startsWith(Stream.of(1, 3))).isTrue();
            assertThat(of(1, 2, 3, 4).startsWith(Stream.of(1, 2, 4))).isFalse();
            assertThat(of(1, 2).startsWith(Stream.of(1, 2, 4))).isFalse();
        }

        @Test
        public void shouldEndWithANonQueueIterable() {
            assertThat(of(1, 3, 4).endsWith(Stream.of(3, 4))).isTrue();
            assertThat(of(1, 2, 3, 4).endsWith(Stream.of(2, 3, 5))).isFalse();
        }
    }

    // -- distinctByKeepLast(Comparator)

    @TestTemplate
    public void shouldComputeDistinctByKeepLastOfEmptyQueueUsingComparator() {
        final Comparator<Integer> comparator = comparingInt(i -> i);
        if (useIsEqualToInsteadOfIsSameAs()) {
            assertThat(this.<Integer>empty().distinctByKeepLast(comparator)).isEqualTo(empty());
        } else {
            assertThat(this.<Integer>empty().distinctByKeepLast(comparator)).isSameAs(empty());
        }
    }

    @TestTemplate
    public void shouldComputeDistinctByKeepLastOfNonEmptyQueueUsingComparator() {
        final Comparator<String> comparator = comparingInt(s -> (s.charAt(1)));
        final Queue<String> distinct = of("1a", "2a", "3b", "4b", "3a", "5c")
                .distinctByKeepLast(comparator);
        assertThat(distinct).isEqualTo(of("4b", "3a", "5c"));
    }

    @TestTemplate
    public void shouldReturnSameInstanceWhenDistinctByKeepLastComparatorEmptyQueue() {
        final Queue<?> empty = empty();
        assertThat(empty.distinctByKeepLast(Comparators.naturalComparator())).isSameAs(empty);
    }

    // -- distinctByKeepLast(Function)

    @TestTemplate
    public void shouldComputeDistinctByKeepLastOfEmptyQueueUsingKeyExtractor() {
        if (useIsEqualToInsteadOfIsSameAs()) {
            assertThat(empty().distinctByKeepLast(Function.identity())).isEqualTo(empty());
        } else {
            assertThat(empty().distinctByKeepLast(Function.identity())).isSameAs(empty());
        }
    }

    @TestTemplate
    public void shouldComputeDistinctByKeepLastOfNonEmptyQueueUsingKeyExtractor() {
        final Function<String, Character> function = c -> c.charAt(1);
        final Queue<String> distinct = of("1a", "2a", "3b", "4b", "3a", "5c")
                .distinctByKeepLast(function);
        assertThat(distinct).isEqualTo(of("4b", "3a", "5c"));
    }

    @TestTemplate
    public void shouldReturnSameInstanceWhenDistinctByKeepLastFunctionEmptyQueue() {
        final Queue<?> empty = empty();
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
            assertThatNullPointerException().isThrownBy(() -> QueueTest.this.<Integer>of((Integer) null));
        }

        @Test
        public void shouldRejectNullOnOfVarargs() {
            assertThatNullPointerException().isThrownBy(() -> QueueTest.this.<Integer>of(1, null));
        }
    }

    @Nested
    class TraversableOnlyTests {
        @Test
        public void shouldImplementTraversableOnly() {
            assertThat(Traversable.class.isAssignableFrom(Queue.class)).isTrue();
            assertThat(Queue.class.getInterfaces()).containsExactly(Traversable.class);
            assertThat(Queue.class.getSuperclass()).isEqualTo(Object.class);
            // no sequence interface above Traversable: a Queue reaches Traversable directly
            assertThat(supertypeNames(Queue.class)).containsExactlyInAnyOrder("Traversable", "Iterable");
        }

        @Test
        public void shouldHaveSizedAndOrderedSpliterator() {
            assertThat(of(1, 2, 3).spliterator().hasCharacteristics(Spliterator.SIZED | Spliterator.SUBSIZED | Spliterator.ORDERED)).isTrue();
            assertThat(of(1, 2, 3).spliterator().getExactSizeIfKnown()).isEqualTo(3);
        }
    }


    @Nested
    class BothEndsTests {

        /** A Queue whose front has been refilled from the rear: front = [4, 5], rear = []. */
        private Queue<Integer> rebalanced() {
            final Queue<Integer> queue = Queue.of(1, 2, 3).enqueue(4).enqueue(5);
            return queue.tail().tail().tail();
        }

        /** A Queue with elements on both sides: front = [1, 2], rear = [4, 3] (that is 3 then 4). */
        private Queue<Integer> bothSides() {
            return Queue.of(1, 2).enqueue(3).enqueue(4);
        }

        @Test
        public void shouldRebalanceTheRearIntoTheFrontOnDequeue() {
            final Queue<Integer> queue = rebalanced();
            assertThat(queue).isEqualTo(Queue.of(4, 5));
            assertThat(queue.head()).isEqualTo(4);
            assertThat(queue.last()).isEqualTo(5);
            assertThat(queue.dequeue()).isEqualTo(Tuple.of(4, Queue.of(5)));
            assertThat(queue.get(0)).isEqualTo(4);
            assertThat(queue.get(1)).isEqualTo(5);
            assertThat(queue.reverse().toList()).isEqualTo(com.guizmaii.zazr.collection.List.of(5, 4));
        }

        @Test
        public void shouldReadAcrossTheFrontRearBoundary() {
            final Queue<Integer> queue = bothSides();
            assertThat(queue).isEqualTo(Queue.of(1, 2, 3, 4));
            assertThat(queue.get(0)).isEqualTo(1);
            assertThat(queue.get(1)).isEqualTo(2);
            assertThat(queue.get(2)).isEqualTo(3);
            assertThat(queue.get(3)).isEqualTo(4);
            assertThatThrownBy(() -> queue.get(4)).isInstanceOf(IndexOutOfBoundsException.class);
            assertThat(queue.length()).isEqualTo(4);
            assertThat(queue.head()).isEqualTo(1);
            assertThat(queue.last()).isEqualTo(4);
            assertThat(queue.indexOf(3)).isEqualTo(2);
            assertThat(queue.lastIndexOf(4)).isEqualTo(3);
            assertThat(queue.indexWhere(i -> i > 2)).isEqualTo(2);
            assertThat(queue.lastIndexWhere(i -> i < 3)).isEqualTo(1);
            assertThat(queue.segmentLength(i -> i < 4, 0)).isEqualTo(3);
            assertThat(queue.prefixLength(i -> i < 3)).isEqualTo(2);
            assertThat(queue.indexOfSlice(Queue.of(2, 3))).isEqualTo(1);
            assertThat(queue.lastIndexOfSlice(Queue.of(3, 4))).isEqualTo(2);
            assertThat(queue.containsSlice(Queue.of(2, 3, 4))).isTrue();
            assertThat(queue.startsWith(Queue.of(1, 2, 3))).isTrue();
            assertThat(queue.endsWith(Queue.of(3, 4))).isTrue();
            assertThat(queue.endsWith(Queue.of(2, 4))).isFalse();
            assertThat(queue.reverse().toList()).isEqualTo(com.guizmaii.zazr.collection.List.of(4, 3, 2, 1));
            assertThat(queue.drop(2).toList()).isEqualTo(com.guizmaii.zazr.collection.List.of(3, 4));
            assertThat(queue.search(3)).isEqualTo(2);
            assertThat(queue.search(5)).isEqualTo(-5);
        }

        @Test
        public void shouldHandleTheEmptyAndSingleCases() {
            final Queue<Integer> empty = Queue.empty();
            assertThat(empty.reverse().iterator().hasNext()).isFalse();
            assertThat(empty.indexOf(1)).isEqualTo(-1);
            assertThat(empty.lastIndexOf(1)).isEqualTo(-1);
            assertThat(empty.indexWhere(i -> true)).isEqualTo(-1);
            assertThat(empty.lastIndexWhere(i -> true)).isEqualTo(-1);
            assertThat(empty.prefixLength(i -> true)).isEqualTo(0);
            assertThat(empty.segmentLength(i -> true, 0)).isEqualTo(0);
            assertThat(empty.containsSlice(Queue.empty())).isTrue();
            assertThat(empty.startsWith(Queue.empty())).isTrue();
            assertThat(empty.endsWith(Queue.empty())).isTrue();
            assertThat(empty.search(1)).isEqualTo(-1);
            assertThat(empty.crossProduct().isEmpty()).isTrue();
            final Queue<Integer> one = Queue.of(1);
            assertThat(one.reverse().toList()).isEqualTo(com.guizmaii.zazr.collection.List.of(1));
            assertThat(one.crossProduct().toList().length()).isEqualTo(1);
            assertThat(one.endsWith(Queue.of(1))).isTrue();
            assertThat(one.endsWith(Queue.of(0, 1))).isFalse();
        }

        @Test
        public void shouldReturnAQueue() {
            final Queue<Integer> queue = bothSides();
            assertThat(queue.rotateLeft(1)).isInstanceOf(Queue.class).isEqualTo(Queue.of(2, 3, 4, 1));
            assertThat(queue.rotateRight(1)).isInstanceOf(Queue.class).isEqualTo(Queue.of(4, 1, 2, 3));
            assertThat(queue.sortBy(i -> -i)).isInstanceOf(Queue.class).isEqualTo(Queue.of(4, 3, 2, 1));
            assertThat(queue.crossProduct(2).head()).isInstanceOf(Queue.class);
        }

        @Test
        public void shouldRejectNullArgumentsOfEveryNewlyDeclaredMethod() {
            final Queue<Integer> queue = Queue.of(1, 2, 3);
            assertThatNullPointerException().isThrownBy(() -> queue.containsSlice(null)).withMessage("that is null");
            assertThatNullPointerException().isThrownBy(() -> queue.crossProduct(null)).withMessage("that is null");
            assertThatNullPointerException().isThrownBy(() -> queue.endsWith(null)).withMessage("that is null");
            assertThatNullPointerException().isThrownBy(() -> queue.startsWith(null)).withMessage("that is null");
            assertThatNullPointerException().isThrownBy(() -> queue.indexOfSlice(null)).withMessage("that is null");
            assertThatNullPointerException().isThrownBy(() -> queue.lastIndexOfSlice(null)).withMessage("that is null");
            assertThatNullPointerException().isThrownBy(() -> queue.indexWhere(null)).withMessage("predicate is null");
            assertThatNullPointerException().isThrownBy(() -> queue.lastIndexWhere(null)).withMessage("predicate is null");
            assertThatNullPointerException().isThrownBy(() -> queue.segmentLength(null, 0)).withMessage("predicate is null");
            assertThatNullPointerException().isThrownBy(() -> queue.search(1, null)).withMessage("comparator is null");
            assertThatNullPointerException().isThrownBy(() -> queue.sortBy(null, Function.identity())).withMessage("comparator is null");
            assertThatNullPointerException().isThrownBy(() -> queue.sortBy(Comparator.naturalOrder(), null)).withMessage("mapper is null");
            assertThatNullPointerException().isThrownBy(() -> queue.asJava(null)).withMessage("action is null");
            assertThatNullPointerException().isThrownBy(() -> queue.asJavaMutable(null)).withMessage("action is null");
        }
    }


    // -- the one-pass and positional cases every sequence answers

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
    public void shouldCalculateAverageOfDoubleAndFloat() {
        assertThat(this.<Number>of(1.0, 1.0f).average().get()).isEqualTo(1.0);
    }

    @TestTemplate
    public void shouldCalculateAverageOfDoublePositiveAndNegativeInfinity() {
        assertThat(of(Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY).average().get()).isNaN();
    }

    @TestTemplate
    public void shouldCalculateAverageOfFloatPositiveAndNegativeInfinity() {
        assertThat(of(Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY).average().get()).isNaN();
    }

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
        final Queue<Integer> testee = of(1, 1, 2, 2, 3, 3);
        final Queue<Integer> actual = testee.distinct();
        final Queue<Integer> expected = of(1, 2, 3);
        assertThat(actual).isEqualTo(expected);
        if (isDistinct()) {
            assertThat(actual).isSameAs(testee);
        }
    }

    // -- distinctBy(Comparator)

    @TestTemplate
    public void shouldComputeDistinctByOfEmptyTraversableUsingComparator() {
        final Comparator<Integer> comparator = comparingInt(i -> i);
        if (useIsEqualToInsteadOfIsSameAs()) {
            assertThat(this.<Integer>empty().distinctBy(comparator)).isEqualTo(empty());
        } else {
            assertThat(this.<Integer>empty().distinctBy(comparator)).isSameAs(empty());
        }
    }

    @TestTemplate
    public void shouldComputeDistinctByOfNonEmptyTraversableUsingComparator() {
        final Comparator<String> comparator = comparingInt(s -> (s.charAt(1)));
        final Queue<String> distinct = of("1a", "2a", "3a", "3b", "4b", "5c").distinctBy(comparator)
          .map(s -> s.substring(1));
        assertThat(distinct).isEqualTo(of("a", "b", "c"));
    }

    @TestTemplate
    public void shouldReturnSameInstanceWhenDistinctByComparatorEmptyTraversable() {
        final Queue<?> empty = empty();
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
        final Function<String, Character> function = c -> c.charAt(1);
        final Queue<String> distinct = of("1a", "2a", "3a", "3b", "4b", "5c").distinctBy(function)
          .map(s -> s.substring(1));
        assertThat(distinct).isEqualTo(of("a", "b", "c"));
    }

    @TestTemplate
    public void shouldReturnSameInstanceWhenDistinctByFunctionEmptyTraversable() {
        final Queue<?> empty = empty();
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
        final Queue<Integer> t = of(1, 2, 3);
        assertThat(t.drop(0)).isSameAs(t);
    }

    @TestTemplate
    public void shouldReturnSameInstanceWhenDropNegativeCount() {
        final Queue<Integer> t = of(1, 2, 3);
        assertThat(t.drop(-1)).isSameAs(t);
    }

    @TestTemplate
    public void shouldReturnSameInstanceWhenEmptyDropOne() {
        final Queue<?> empty = empty();
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
        final Queue<Integer> t = of(1, 2, 3);
        assertThat(t.dropRight(0)).isSameAs(t);
    }

    @TestTemplate
    public void shouldReturnSameInstanceWhenDropRightNegativeCount() {
        final Queue<Integer> t = of(1, 2, 3);
        assertThat(t.dropRight(-1)).isSameAs(t);
    }

    @TestTemplate
    public void shouldReturnSameInstanceWhenEmptyDropRightOne() {
        final Queue<?> empty = empty();
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
            final Queue<Integer> t = of(1, 2, 3);
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
        final Queue<?> empty = empty();
        assertThat(empty.dropUntil(ignored -> true)).isSameAs(empty);
    }

    // -- dropWhile

    @TestTemplate
    public void shouldDropWhileNoneOnNil() {
        final Queue<?> empty = empty();
        final Queue<?> actual = empty.dropWhile(ignored -> true);
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
            final Queue<Integer> t = of(1, 2, 3);
            assertThat(t.dropWhile(ignored -> false)).isSameAs(t);
        }
    }

    @TestTemplate
    public void shouldDropWhileAllIfPredicateIsTrue() {
        final Queue<Integer> actual = of(1, 2, 3).dropWhile(ignored -> true);
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
        final Queue<?> empty = empty();
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
            final Queue<Integer> t = of(1, 2, 3);
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
        final Queue<?> empty = empty();
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
            final Queue<Integer> t = of(1, 2, 3);
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
        final Queue<?> empty = empty();
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
        final AtomicInteger calls = new AtomicInteger();
        final Queue<Integer> actual = this.<Integer>empty().collect(i -> {
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
        final Queue<Integer> actual = of(1, 2, 3).collect(i -> switch (i) {
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
        // the message names the concrete type, which toString prints before the parenthesis (List, IntMap, Iterator...)
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
        final boolean[] actual = {false};
        final boolean[] expected = {false};
        empty().forEachWithIndex((chr, index) -> actual[0] = true);
        assertThat(actual).isEqualTo(expected);
    }

    @TestTemplate
    public void shouldConsumeEachElementWithIndexWhenNonEmpty() {
        final java.util.List<Tuple2<Character, Integer>> actual = new java.util.ArrayList<>();
        final java.util.List<Tuple2<Character, Integer>> expected = Arrays.asList(Tuple.of('a', 0), Tuple.of('b', 1), Tuple.of('c', 2));
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
        final List<Queue<Integer>> actual = of(1, 2, 3, 4).grouped(2).toList();
        final List<Queue<Integer>> expected = List.of(Queue.of(1, 2), Queue.of(3, 4));
        assertThat(actual).isEqualTo(expected);
    }

    @TestTemplate
    public void shouldGroupedTraversableWithRemainder() {
        final List<Queue<Integer>> actual = of(1, 2, 3, 4, 5).grouped(2).toList();
        final List<Queue<Integer>> expected = List.of(Queue.of(1, 2), Queue.of(3, 4), Queue.of(5));
        assertThat(actual).isEqualTo(expected);
    }

    @TestTemplate
    public void shouldGroupedWhenTraversableLengthIsSmallerThanBlockSize() {
        final List<Queue<Integer>> actual = of(1, 2, 3, 4).grouped(5).toList();
        final List<Queue<Integer>> expected = List.of(Queue.of(1, 2, 3, 4));
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
        assertThat(empty().length()).isEqualTo(0);
    }

    @TestTemplate
    public void shouldComputeLengthOfNonNil() {
        assertThat(of(1, 2, 3).length()).isEqualTo(3);
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
        final Queue<Integer> src = of(42);
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
        final Queue<Integer> src = of(42);
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
        final Queue<Integer> testee = of(0, 1, 2, 1);
        final Queue<Integer> actual = testee.replace(1, 3);
        final Queue<Integer> expected = of(0, 3, 2, 1);
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
            final Queue<Integer> src = of(0, 1, 2);
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
            final Queue<Integer> src = of(0, 1, 2, 1);
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
        final Queue<Object> empty = empty();
        final Queue<Object> actual = empty.retainAll(of(1, 2, 3));
        if (useIsEqualToInsteadOfIsSameAs()) {
            assertThat(actual).isEqualTo(empty);
        } else {
            assertThat(actual).isSameAs(empty);
        }
    }

    @TestTemplate
    public void shouldRetainAllExistingElementsFromNonNil() {
        final Queue<Integer> src = of(1, 2, 3, 2, 1, 3);
        final Queue<Integer> expected = of(1, 2, 2, 1);
        final Queue<Integer> actual = src.retainAll(of(1, 2));
        assertThat(actual).isEqualTo(expected);
    }

    @TestTemplate
    public void shouldRetainAllElementsFromNonNil() {
        final Queue<Integer> src = of(1, 2, 1, 2, 2);
        final Queue<Integer> expected = of(1, 2, 1, 2, 2);
        final Queue<Integer> actual = src.retainAll(of(1, 2));
        if (useIsEqualToInsteadOfIsSameAs()) {
            assertThat(actual).isEqualTo(expected);
        } else {
            assertThat(actual).isSameAs(src);
        }
    }

    @TestTemplate
    public void shouldNotRetainAllNonExistingElementsFromNonNil() {
        final Queue<Integer> src = of(1, 2, 3);
        final Queue<Object> expected = empty();
        final Queue<Integer> actual = src.retainAll(of(4, 5));
        if (useIsEqualToInsteadOfIsSameAs()) {
            assertThat(actual).isEqualTo(expected);
        } else {
            assertThat(actual).isSameAs(expected);
        }
    }

    // -- scan, scanLeft, scanRight

    @TestTemplate
    public void shouldScanEmpty() {
        final Queue<Integer> testee = empty();
        final Queue<Integer> actual = testee.scan(0, (s1, s2) -> s1 + s2);
        assertThat(actual).isEqualTo(this.of(0));
    }

    @TestTemplate
    public void shouldScanLeftEmpty() {
        final Queue<Integer> testee = empty();
        final Queue<Integer> actual = testee.scanLeft(0, (s1, s2) -> s1 + s2);
        assertThat(actual).isEqualTo(of(0));
    }

    @TestTemplate
    public void shouldScanRightEmpty() {
        final Queue<Integer> testee = empty();
        final Queue<Integer> actual = testee.scanRight(0, (s1, s2) -> s1 + s2);
        assertThat(actual).isEqualTo(of(0));
    }

    @TestTemplate
    public void shouldScanNonEmpty() {
        final Queue<Integer> testee = of(1, 2, 3);
        final Queue<Integer> actual = testee.scan(0, (acc, s) -> acc + s);
        assertThat(actual).isEqualTo(of(0, 1, 3, 6));
    }

    @TestTemplate
    public void shouldScanLeftNonEmpty() {
        final Queue<Integer> testee = of(1, 2, 3);
        final Queue<String> actual = testee.scanLeft("x", (acc, i) -> acc + i);
        assertThat(actual).isEqualTo(of("x", "x1", "x12", "x123"));
    }

    @TestTemplate
    public void shouldScanRightNonEmpty() {
        final Queue<Integer> testee = of(1, 2, 3);
        final Queue<String> actual = testee.scanRight("x", (i, acc) -> acc + i);
        assertThat(actual).isEqualTo(of("x321", "x32", "x3", "x"));
    }

    @TestTemplate
    public void shouldScanWithNonComparable() {
        final Queue<NonComparable> testee = of(new NonComparable("a"));
        final List<NonComparable> actual = List.ofAll(testee.scan(new NonComparable("x"), (u1, u2) -> new NonComparable(u1.value + u2.value)));
        final List<NonComparable> expected = List.of("x", "xa").map(NonComparable::new);
        assertThat(actual).containsAll(expected);
        assertThat(expected).containsAll(actual);
        assertThat(actual.length()).isEqualTo(expected.length());
    }

    @TestTemplate
    public void shouldScanLeftWithNonComparable() {
        final Queue<NonComparable> testee = of(new NonComparable("a"));
        final List<NonComparable> actual = List.ofAll(testee.scanLeft(new NonComparable("x"), (u1, u2) -> new NonComparable(u1.value + u2.value)));
        final List<NonComparable> expected = List.of("x", "xa").map(NonComparable::new);
        assertThat(actual).containsAll(expected);
        assertThat(expected).containsAll(actual);
        assertThat(actual.length()).isEqualTo(expected.length());
    }

    @TestTemplate
    public void shouldScanRightWithNonComparable() {
        final Queue<NonComparable> testee = of(new NonComparable("a"));
        final List<NonComparable> actual = List.ofAll(testee.scanRight(new NonComparable("x"), (u1, u2) -> new NonComparable(u1.value + u2.value)));
        final List<NonComparable> expected = List.of("ax", "x").map(NonComparable::new);
        assertThat(actual).containsAll(expected);
        assertThat(expected).containsAll(actual);
        assertThat(actual.length()).isEqualTo(expected.length());
    }

    // -- slideBy(classifier)

    @TestTemplate
    public void shouldSlideNilByClassifier() {
        assertThat(empty().slideBy(Function.identity())).isEmpty();
    }

    @TestTemplate
    public void shouldTerminateSlideByClassifier() {
        assertTimeout(Duration.ofSeconds(1),() -> {
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
        final List<Queue<Integer>> actual = of(1).slideBy(Function.identity()).toList();
        final List<Queue<Integer>> expected = List.of(Queue.of(1));
        assertThat(actual).isEqualTo(expected);
    }

    @TestTemplate
    public void shouldSlideNonNilByIdentityClassifier() {
        final List<Queue<Integer>> actual = of(1, 2, 3).slideBy(Function.identity()).toList();
        final List<Queue<Integer>> expected = List.of(Queue.of(1), Queue.of(2), Queue.of(3));
        assertThat(actual).isEqualTo(expected);
    }

    @TestTemplate
    public void shouldSlideNonNilByConstantClassifier() {
        final List<Queue<Integer>> actual = of(1, 2, 3).slideBy(e -> "same").toList();
        final List<Queue<Integer>> expected = List.of(Queue.of(1, 2, 3));
        assertThat(actual).isEqualTo(expected);
    }

    @TestTemplate
    public void shouldSlideNonNilBySomeClassifier() {
        final List<Queue<Integer>> actual = of(10, 20, 30, 42, 52, 60, 72).slideBy(e -> e % 10).toList();
        final List<Queue<Integer>> expected = List.of(Queue.of(10, 20, 30), Queue.of(42, 52), Queue.of(60), Queue.of(72));
        assertThat(actual).isEqualTo(expected);
    }

    @TestTemplate
    public void shouldSlideByClassifierReturningNull() {
        final List<Queue<Integer>> actual = of(1, 2, 3).slideBy(e -> null).toList();
        final List<Queue<Integer>> expected = List.of(Queue.of(1, 2, 3));
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
        final List<Queue<Integer>> actual = of(1, 2, 3).sliding(1).toList();
        final List<Queue<Integer>> expected = List.of(Queue.of(1), Queue.of(2), Queue.of(3));
        assertThat(actual).isEqualTo(expected);
    }

    @TestTemplate
    public void shouldSlideNonNilBySize2() {
        final List<Queue<Integer>> actual = of(1, 2, 3, 4, 5).sliding(2).toList();
        final List<Queue<Integer>> expected = List.of(Queue.of(1, 2), Queue.of(2, 3), Queue.of(3, 4), Queue.of(4, 5));
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
        final List<Queue<Integer>> actual = of(1, 2, 3, 4, 5).sliding(2, 3).toList();
        final List<Queue<Integer>> expected = List.of(Queue.of(1, 2), Queue.of(4, 5));
        assertThat(actual).isEqualTo(expected);
    }

    @TestTemplate
    public void shouldSlide5ElementsBySize2AndStep4() {
        final List<Queue<Integer>> actual = of(1, 2, 3, 4, 5).sliding(2, 4).toList();
        final List<Queue<Integer>> expected = List.of(Queue.of(1, 2), Queue.of(5));
        assertThat(actual).isEqualTo(expected);
    }

    @TestTemplate
    public void shouldSlide5ElementsBySize2AndStep5() {
        final List<Queue<Integer>> actual = of(1, 2, 3, 4, 5).sliding(2, 5).toList();
        final List<Queue<Integer>> expected = List.of(Queue.of(1, 2));
        assertThat(actual).isEqualTo(expected);
    }

    @TestTemplate
    public void shouldSlide4ElementsBySize5AndStep3() {
        final List<Queue<Integer>> actual = of(1, 2, 3, 4).sliding(5, 3).toList();
        final List<Queue<Integer>> expected = List.of(Queue.of(1, 2, 3, 4));
        assertThat(actual).isEqualTo(expected);
    }

    @TestTemplate
    public void shouldSlide7ElementsBySize1AndStep3() {
        final List<Queue<Integer>> actual = of(1, 2, 3, 4, 5, 6, 7).sliding(1, 3).toList();
        final List<Queue<Integer>> expected = List.of(Queue.of(1), Queue.of(4), Queue.of(7));
        assertThat(actual).isEqualTo(expected);
    }

    @TestTemplate
    public void shouldSlide7ElementsBySize2AndStep3() {
        final List<Queue<Integer>> actual = of(1, 2, 3, 4, 5, 6, 7).sliding(2, 3).toList();
        final List<Queue<Integer>> expected = List.of(Queue.of(1, 2), Queue.of(4, 5), Queue.of(7));
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
            final Queue<Integer> t = of(1, 2, 3);
            assertThat(t.take(4)).isSameAs(t);
        }
    }

    @TestTemplate
    public void shouldReturnSameInstanceIfTakeAll() {
        final Queue<?> t = of(1, 2, 3);
        assertThat(t.take(3)).isSameAs(t);
        assertThat(t.take(4)).isSameAs(t);
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
            final Queue<Integer> t = of(1, 2, 3);
            assertThat(t.takeRight(4)).isSameAs(t);
        }
    }

    @TestTemplate
    public void shouldReturnSameInstanceIfTakeRightAll() {
        final Queue<?> t = of(1, 2, 3);
        assertThat(t.takeRight(3)).isSameAs(t);
        assertThat(t.takeRight(4)).isSameAs(t);
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
        final Queue<Integer> t = of(1, 2, 3);
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
        final Queue<?> empty = empty();
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
        final Queue<Integer> t = of(1, 2, 3);
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
        final Queue<?> empty = empty();
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
        final Tuple actual = of(0, 1).unzip(i -> Tuple.of(i, (char) ((short) 'a' + i)));
        final Tuple expected = Tuple.of(of(0, 1), of('a', 'b'));
        assertThat(actual).isEqualTo(expected);
    }

    @TestTemplate
    public void shouldUnzip3Nil() {
        assertThat(empty().unzip3(x -> Tuple.of(x, x, x))).isEqualTo(Tuple.of(empty(), empty(), empty()));
    }

    @TestTemplate
    public void shouldUnzip3NonNil() {
        final Tuple actual = of(0, 1).unzip3(i -> Tuple.of(i, (char) ((short) 'a' + i), (char) ((short) 'a' + i + 1)));
        final Tuple expected = Tuple.of(of(0, 1), of('a', 'b'), of('b', 'c'));
        assertThat(actual).isEqualTo(expected);
    }

    // -- zip

    @TestTemplate
    public void shouldZipNils() {
        final Queue<?> actual = empty().zip(empty());
        assertThat(actual).isEmpty();
    }

    @TestTemplate
    public void shouldZipEmptyAndNonNil() {
        final Queue<?> actual = empty().zip(of(1));
        assertThat(actual).isEmpty();
    }

    @TestTemplate
    public void shouldZipNonEmptyAndNil() {
        final Queue<?> actual = of(1).zip(empty());
        assertThat(actual).isEmpty();
    }

    @TestTemplate
    public void shouldZipNonNilsIfThisIsSmaller() {
        final Queue<Tuple2<Integer, String>> actual = of(1, 2).zip(of("a", "b", "c"));
        @SuppressWarnings("unchecked") final Queue<Tuple2<Integer, String>> expected = of(Tuple.of(1, "a"), Tuple.of(2, "b"));
        assertThat(actual).isEqualTo(expected);
    }

    @TestTemplate
    public void shouldZipNonNilsIfThatIsSmaller() {
        final Queue<Tuple2<Integer, String>> actual = of(1, 2, 3).zip(of("a", "b"));
        @SuppressWarnings("unchecked") final Queue<Tuple2<Integer, String>> expected = of(Tuple.of(1, "a"), Tuple.of(2, "b"));
        assertThat(actual).isEqualTo(expected);
    }

    @TestTemplate
    public void shouldZipNonNilsOfSameSize() {
        final Queue<Tuple2<Integer, String>> actual = of(1, 2, 3).zip(of("a", "b", "c"));
        @SuppressWarnings("unchecked") final Queue<Tuple2<Integer, String>> expected = of(Tuple.of(1, "a"), Tuple.of(2, "b"), Tuple.of(3, "c"));
        assertThat(actual).isEqualTo(expected);
    }

    @TestTemplate
    @SuppressWarnings("unchecked")
    public void shouldZipWithNonNilsOfSameSize() {
        final Queue<Tuple2<Integer, String>> actual = of(1, 2, 3).zipWith(of("a", "b", "c"), Tuple::of);
        final Queue<Tuple2<Integer, String>> expected = of(Tuple.of(1, "a"), Tuple.of(2, "b"), Tuple.of(3, "c"));
        assertThat(actual).isEqualTo(expected);
    }

    @TestTemplate
    public void shouldThrowIfZipWithThatIsNull() {
        assertThrows(NullPointerException.class, () -> empty().zip(null));
    }

    // -- zipAll

    @TestTemplate
    public void shouldZipAllNils() {
        final Queue<?> actual = empty().zipAll(empty(), 0, 0);
        assertThat(actual).isEmpty();
    }

    @TestTemplate
    public void shouldZipAllEmptyAndNonNil() {
        final Queue<?> actual = empty().zipAll(of(1), 0, 0);
        final Queue<Tuple2<Object, Integer>> expected = of(Tuple.of(0, 1));
        assertThat(actual).isEqualTo(expected);
    }

    @TestTemplate
    public void shouldZipAllNonEmptyAndNil() {
        final Queue<?> actual = of(1).zipAll(empty(), 0, 0);
        final Queue<Tuple2<Integer, Object>> expected = of(Tuple.of(1, 0));
        assertThat(actual).isEqualTo(expected);
    }

    @TestTemplate
    public void shouldRejectNullZipAllFillValues() {
        assertThrows(NullPointerException.class, () -> empty().zipAll(of(1), null, 0));
        assertThrows(NullPointerException.class, () -> empty().zipAll(of(1), 0, null));
    }

    @TestTemplate
    public void shouldZipAllNonNilsIfThisIsSmaller() {
        final Queue<Tuple2<Integer, String>> actual = of(1, 2).zipAll(of("a", "b", "c"), 9, "z");
        @SuppressWarnings("unchecked") final Queue<Tuple2<Integer, String>> expected = of(Tuple.of(1, "a"), Tuple.of(2, "b"), Tuple.of(9, "c"));
        assertThat(actual).isEqualTo(expected);
    }

    @TestTemplate
    public void shouldZipAllNonNilsIfThatIsSmaller() {
        final Queue<Tuple2<Integer, String>> actual = of(1, 2, 3).zipAll(of("a", "b"), 9, "z");
        @SuppressWarnings("unchecked") final Queue<Tuple2<Integer, String>> expected = of(Tuple.of(1, "a"), Tuple.of(2, "b"), Tuple.of(3, "z"));
        assertThat(actual).isEqualTo(expected);
    }

    @TestTemplate
    public void shouldZipAllNonNilsOfSameSize() {
        final Queue<Tuple2<Integer, String>> actual = of(1, 2, 3).zipAll(of("a", "b", "c"), 9, "z");
        @SuppressWarnings("unchecked") final Queue<Tuple2<Integer, String>> expected = of(Tuple.of(1, "a"), Tuple.of(2, "b"), Tuple.of(3, "c"));
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
        final Queue<Tuple2<String, Integer>> actual = of("a", "b", "c").zipWithIndex();
        @SuppressWarnings("unchecked") final Queue<Tuple2<String, Integer>> expected = of(Tuple.of("a", 0), Tuple.of("b", 1), Tuple.of("c", 2));
        assertThat(actual).isEqualTo(expected);
    }

    @TestTemplate
    @SuppressWarnings("unchecked")
    public void shouldZipNonNilWithIndexWithMapper() {
        final Queue<Tuple2<String, Integer>> actual = of("a", "b", "c").zipWithIndex(Tuple::of);
        final Queue<Tuple2<String, Integer>> expected = of(Tuple.of("a", 0), Tuple.of("b", 1), Tuple.of("c", 2));
        assertThat(actual).isEqualTo(expected);
    }

    // -- toArray(IntFunction)

    @TestTemplate
    public void shouldConvertNilToJavaArray() {
        final Integer[] actual = List.<Integer>empty().toArray(Integer[]::new);
        final Integer[] expected = new Integer[]{};
        assertThat(actual).isEqualTo(expected);
    }

    @TestTemplate
    public void shouldConvertNonNilToJavaArray() {
        final Integer[] array = of(1, 2).toArray(Integer[]::new);
        final Integer[] expected = new Integer[]{1, 2};
        assertThat(array).isEqualTo(expected);
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
        final int[] effect = {0};
        final Queue<Integer> actual = of(1).tap(i -> effect[0] = i);
        assertThat(actual).isEqualTo(of(1));
        assertThat(effect[0]).isEqualTo(1);
    }

    @TestTemplate
    public void shouldTapEveryElement() {
        final int[] sum = {0};
        final Queue<Integer> actual = of(1, 2, 3).tap(i -> sum[0] += i);
        assertThat(actual).isEqualTo(of(1, 2, 3)); // consumes every element in the lazy case
        assertThat(sum[0]).isEqualTo(6);
    }

    @TestTemplate
    public void shouldReturnThisOnTapOfEagerCollection() {
        final Queue<Integer> testee = of(1, 2, 3);
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
        final Queue<Integer> value = of(3, 7, 1, 15, 0);
        final Set<Integer> set = value.toLinkedSet();
        final List<Integer> itemsInOrder = true ? value.toList() : List.of(3, 7, 1, 15, 0);
        assertThat(set).isEqualTo(itemsInOrder.foldLeft(LinkedHashSet.empty(), LinkedHashSet::add));
        assertThat(empty().toLinkedSet()).isSameAs(LinkedHashSet.empty());
    }

    @TestTemplate
    public void shouldConvertToSortedSetWithoutComparatorOnComparable() {
        assertThat(of(3, 7, 1, 15, 0).toSortedSet()).isEqualTo(TreeSet.of(0, 1, 3, 7, 15));
    }

    @TestTemplate
    public void shouldThrowOnConvertToSortedSetWithoutComparatorOnNonComparable() {
        assertThrows(ClassCastException.class, () -> of(new Object(), new Object()).toSortedSet());
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


    @Nested
    class WindowsAndProductsTests {

        /** A Queue whose front was refilled from the rear by a dequeue: front = [2, 3, 4, 5], rear = []. */
        private Queue<Integer> rebalanced() {
            return Queue.of(1).enqueue(2).enqueue(3).enqueue(4).enqueue(5).dequeue()._2();
        }

        @Test
        public void shouldGroupIntoQueuesOfQueues() {
            final Queue<Queue<Integer>> groups = of(1, 2, 3, 4, 5).grouped(2);
            assertThat(groups).isInstanceOf(Queue.class);
            assertThat(groups).isEqualTo(Queue.of(Queue.of(1, 2), Queue.of(3, 4), Queue.of(5)));
            assertThat(groups.head()).isInstanceOf(Queue.class);
            assertThat(groups.head()).isEqualTo(Queue.of(1, 2));
        }

        @Test
        public void shouldSlideIntoQueuesOfQueues() {
            assertThat(of(1, 2, 3, 4).sliding(3)).isInstanceOf(Queue.class).isEqualTo(Queue.of(Queue.of(1, 2, 3), Queue.of(2, 3, 4)));
            assertThat(of(1, 2, 3, 4, 5).sliding(2, 3)).isEqualTo(Queue.of(Queue.of(1, 2), Queue.of(4, 5)));
            assertThat(of(1, 2, 3, 4, 5).sliding(2, 4)).isEqualTo(Queue.of(Queue.of(1, 2), Queue.of(5)));
            assertThat(of(1, 2).sliding(5)).isEqualTo(Queue.of(Queue.of(1, 2)));
            assertThat(of(1, 2, 3, 4).sliding(3).head()).isInstanceOf(Queue.class);
        }

        @Test
        public void shouldSlideByIntoQueuesOfQueues() {
            final Queue<Queue<Integer>> runs = of(1, 2, 3, 10, 12, 5, 7, 20, 29).slideBy(x -> x / 10);
            assertThat(runs).isInstanceOf(Queue.class);
            assertThat(runs).isEqualTo(Queue.of(Queue.of(1, 2, 3), Queue.of(10, 12), Queue.of(5, 7), Queue.of(20, 29)));
            assertThat(runs.head()).isInstanceOf(Queue.class);
        }

        @Test
        public void shouldBuildTheWindowsNow() {
            final AtomicInteger keys = new AtomicInteger();
            final Queue<Queue<Integer>> runs = of(1, 2, 3).slideBy(x -> {
                keys.incrementAndGet();
                return x;
            });
            final int keysAtReturn = keys.get();
            assertThat(keysAtReturn).isGreaterThanOrEqualTo(3); // every element is classified before the call returns
            assertThat(runs.size()).isEqualTo(3);
            assertThat(runs.toList()).isEqualTo(List.of(Queue.of(1), Queue.of(2), Queue.of(3)));
            assertThat(keys.get()).isEqualTo(keysAtReturn); // and none of them afterwards
        }

        @Test
        public void shouldHaveNoWindowOnTheEmptyQueue() {
            assertThat(empty().grouped(1)).isEqualTo(Queue.empty());
            assertThat(empty().sliding(1)).isEqualTo(Queue.empty());
            assertThat(empty().sliding(1, 2)).isEqualTo(Queue.empty());
            assertThat(empty().slideBy(Function.identity())).isEqualTo(Queue.empty());
            assertThat(empty().crossProduct()).isEqualTo(Queue.empty());
            assertThat(empty().crossProduct(2)).isEqualTo(Queue.empty());
            assertThat(empty().crossProduct(of(1))).isEqualTo(Queue.empty());
            assertThat(empty().crossProduct(0)).isEqualTo(Queue.of(Queue.empty()));
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
        public void shouldBuildTheCrossProductsAsQueues() {
            assertThat(of(1, 2).crossProduct()).isInstanceOf(Queue.class)
                    .isEqualTo(Queue.of(Tuple.of(1, 1), Tuple.of(1, 2), Tuple.of(2, 1), Tuple.of(2, 2)));
            assertThat(of(1, 2).crossProduct(2)).isInstanceOf(Queue.class)
                    .isEqualTo(Queue.of(Queue.of(1, 1), Queue.of(1, 2), Queue.of(2, 1), Queue.of(2, 2)));
            assertThat(of(1, 2).crossProduct(2).head()).isInstanceOf(Queue.class);
            assertThat(of(1, 2).crossProduct(List.of('a', 'b'))).isInstanceOf(Queue.class)
                    .isEqualTo(Queue.of(Tuple.of(1, 'a'), Tuple.of(1, 'b'), Tuple.of(2, 'a'), Tuple.of(2, 'b')));
        }

        @Test
        public void shouldWalkTheCrossProductArgumentNow() {
            final AtomicInteger walks = new AtomicInteger();
            final Iterable<Character> that = () -> {
                walks.incrementAndGet();
                return java.util.List.of('a', 'b').iterator();
            };
            final Queue<Tuple2<Integer, Character>> product = of(1, 2, 3).crossProduct(that);
            final int walksAtReturn = walks.get();
            assertThat(walksAtReturn).isEqualTo(1); // the argument is copied, once, before the call returns
            assertThat(product.size()).isEqualTo(6);
            assertThat(product.toList()).isEqualTo(List.of(Tuple.of(1, 'a'), Tuple.of(1, 'b'), Tuple.of(2, 'a'), Tuple.of(2, 'b'), Tuple.of(3, 'a'), Tuple.of(3, 'b')));
            assertThat(walks.get()).isEqualTo(walksAtReturn); // and not walked again afterwards
            assertThat(of(1, 2, 3).crossProduct(java.util.List.of())).isEqualTo(Queue.empty());
        }

        @Test
        public void shouldWindowAQueueWithBothEndsPopulated() {
            final Queue<Integer> queue = bothSides();
            assertThat(queue.grouped(3)).isEqualTo(Queue.of(Queue.of(1, 2, 3), Queue.of(4)));
            assertThat(queue.sliding(2)).isEqualTo(Queue.of(Queue.of(1, 2), Queue.of(2, 3), Queue.of(3, 4)));
            assertThat(queue.sliding(3, 2)).isEqualTo(Queue.of(Queue.of(1, 2, 3), Queue.of(3, 4)));
            assertThat(queue.slideBy(i -> i / 3)).isEqualTo(Queue.of(Queue.of(1, 2), Queue.of(3, 4)));
            assertThat(queue.crossProduct().size()).isEqualTo(16);
            assertThat(queue.crossProduct().head()).isEqualTo(Tuple.of(1, 1));
            assertThat(queue.crossProduct().last()).isEqualTo(Tuple.of(4, 4));
            assertThat(queue.crossProduct(2).size()).isEqualTo(16);
            assertThat(queue.crossProduct(of('a', 'b')).toList()).isEqualTo(List.of(Tuple.of(1, 'a'), Tuple.of(1, 'b'), Tuple.of(2, 'a'), Tuple.of(2, 'b'), Tuple.of(3, 'a'), Tuple.of(3, 'b'), Tuple.of(4, 'a'), Tuple.of(4, 'b')));
        }

        @Test
        public void shouldWindowAQueueAfterADequeueRefilledTheFront() {
            final Queue<Integer> queue = rebalanced();
            assertThat(queue).isEqualTo(Queue.of(2, 3, 4, 5));
            assertThat(queue.grouped(3)).isEqualTo(Queue.of(Queue.of(2, 3, 4), Queue.of(5)));
            assertThat(queue.sliding(2, 2)).isEqualTo(Queue.of(Queue.of(2, 3), Queue.of(4, 5)));
            assertThat(queue.slideBy(i -> i % 2)).isEqualTo(Queue.of(Queue.of(2), Queue.of(3), Queue.of(4), Queue.of(5)));
            assertThat(queue.crossProduct().size()).isEqualTo(16);
            assertThat(queue.crossProduct(1)).isEqualTo(Queue.of(Queue.of(2), Queue.of(3), Queue.of(4), Queue.of(5)));
            assertThat(queue.crossProduct(of(0))).isEqualTo(Queue.of(Tuple.of(2, 0), Tuple.of(3, 0), Tuple.of(4, 0), Tuple.of(5, 0)));
        }

        private Queue<Integer> bothSides() {
            return Queue.of(1, 2).enqueue(3).enqueue(4);
        }
    }

}
