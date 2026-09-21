package com.guizmaii.zazr.collection;

import com.guizmaii.zazr.Tuple;
import com.guizmaii.zazr.Tuple2;
import com.guizmaii.zazr.control.Option;
import java.math.BigDecimal;
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

import static java.util.Comparator.comparingInt;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class QueueTest extends AbstractTraversableRangeTest {

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

    @Override
    protected Queue<Character> range(char from, char toExclusive) {
        return Queue.range(from, toExclusive);
    }

    @Override
    protected Queue<Character> rangeBy(char from, char toExclusive, int step) {
        return Queue.rangeBy(from, toExclusive, step);
    }

    @Override
    protected Queue<Double> rangeBy(double from, double toExclusive, double step) {
        return Queue.rangeBy(from, toExclusive, step);
    }

    @Override
    protected Queue<Integer> range(int from, int toExclusive) {
        return Queue.range(from, toExclusive);
    }

    @Override
    protected Queue<Integer> rangeBy(int from, int toExclusive, int step) {
        return Queue.rangeBy(from, toExclusive, step);
    }

    @Override
    protected Queue<Long> range(long from, long toExclusive) {
        return Queue.range(from, toExclusive);
    }

    @Override
    protected Queue<Long> rangeBy(long from, long toExclusive, long step) {
        return Queue.rangeBy(from, toExclusive, step);
    }

    @Override
    protected Queue<Character> rangeClosed(char from, char toInclusive) {
        return Queue.rangeClosed(from, toInclusive);
    }

    @Override
    protected Queue<Character> rangeClosedBy(char from, char toInclusive, int step) {
        return Queue.rangeClosedBy(from, toInclusive, step);
    }

    @Override
    protected Queue<Double> rangeClosedBy(double from, double toInclusive, double step) {
        return Queue.rangeClosedBy(from, toInclusive, step);
    }

    @Override
    protected Queue<Integer> rangeClosed(int from, int toInclusive) {
        return Queue.rangeClosed(from, toInclusive);
    }

    @Override
    protected Queue<Integer> rangeClosedBy(int from, int toInclusive, int step) {
        return Queue.rangeClosedBy(from, toInclusive, step);
    }

    @Override
    protected Queue<Long> rangeClosed(long from, long toInclusive) {
        return Queue.rangeClosed(from, toInclusive);
    }

    @Override
    protected Queue<Long> rangeClosedBy(long from, long toInclusive, long step) {
        return Queue.rangeClosedBy(from, toInclusive, step);
    }

    protected <T> Queue<Queue<T>> transpose(Queue<Queue<T>> rows) {
        return Queue.transpose(rows);
    }

    //fixme: delete, when useIsEqualToInsteadOfIsSameAs() will be eliminated from AbstractValueTest class
    @Override
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
            final Traversable<Integer> value = of(1, 2, 3);
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
            Queue<Integer> seq = of(1);
            assertThat(seq.rotateLeft(1)).isSameAs(seq);
        }

        @Test
        public void shouldRotateLeftForZero() {
            Queue<Integer> seq = of(1, 2, 3, 4, 5);
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
            assertThat(supertypeNames(Queue.class)).containsExactlyInAnyOrder("Traversable", "Foldable", "Iterable");
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
            assertThat(queue.reverseIterator().toList()).isEqualTo(com.guizmaii.zazr.collection.List.of(5, 4));
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
            assertThat(queue.reverseIterator().toList()).isEqualTo(com.guizmaii.zazr.collection.List.of(4, 3, 2, 1));
            assertThat(queue.iterator(2).toList()).isEqualTo(com.guizmaii.zazr.collection.List.of(3, 4));
            assertThat(queue.search(3)).isEqualTo(2);
            assertThat(queue.search(5)).isEqualTo(-5);
        }

        @Test
        public void shouldHandleTheEmptyAndSingleCases() {
            final Queue<Integer> empty = Queue.empty();
            assertThat(empty.reverseIterator().hasNext()).isFalse();
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
            assertThat(empty.crossProduct().hasNext()).isFalse();
            final Queue<Integer> one = Queue.of(1);
            assertThat(one.reverseIterator().toList()).isEqualTo(com.guizmaii.zazr.collection.List.of(1));
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
            assertThat(queue.crossProduct(2).next()).isInstanceOf(Queue.class);
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

}
