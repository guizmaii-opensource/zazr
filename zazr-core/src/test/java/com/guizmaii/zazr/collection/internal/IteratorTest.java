package com.guizmaii.zazr.collection.internal;

import com.guizmaii.zazr.Tuple;
import com.guizmaii.zazr.Tuple2;
import com.guizmaii.zazr.collection.List;
import com.guizmaii.zazr.collection.Queue;
import com.guizmaii.zazr.collection.Stream;
import com.guizmaii.zazr.collection.Vector;
import com.guizmaii.zazr.control.Option;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.Comparator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Supplier;
import org.assertj.core.api.ObjectAssert;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static com.guizmaii.zazr.collection.internal.Iterator.concat;
import static com.guizmaii.zazr.collection.internal.Iterator.continually;
import static com.guizmaii.zazr.collection.internal.Iterator.from;
import static com.guizmaii.zazr.collection.internal.Iterator.iterate;
import static com.guizmaii.zazr.collection.internal.Iterator.rangeBy;
import static com.guizmaii.zazr.collection.internal.Iterator.rangeClosedBy;
import static java.util.Comparator.comparingInt;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeout;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

/**
 * The internal single-pass cursor: its factories, its lazy combinators, the single-use contract of
 * {@code hasNext}/{@code next}, and the null rejection at the {@code AbstractIterator.next()} funnel.
 */
class IteratorTest {

    private static <T> Iterator<T> empty() {
        return Iterator.empty();
    }

    @SafeVarargs
    private static <T> Iterator<T> of(T... elements) {
        return Iterator.of(elements);
    }

    // an Iterator is compared by the elements it yields: it has no equals of its own
    private static <T> List<T> list(Iterable<T> elements) {
        return List.ofAll(elements);
    }

    // identity checks on an Iterator, which is both an Iterable and a java.util.Iterator for AssertJ
    private static ObjectAssert<Object> assertThatIterator(Object iterator) {
        return assertThat(iterator);
    }

    // -- static of(), of(T...)

    @Test
    public void shouldFailOfEmptyArgList() {
        assertThrows(NoSuchElementException.class, () -> of().next());
    }

    @Test
    public void shouldCreateNil() {
        assertThat(list(empty())).isEmpty();
    }

    @Test
    public void shouldYieldTheOneElementOfOf() {
        assertThat(list(Iterator.of(1))).isEqualTo(List.of(1));
    }

    @Test
    public void shouldCreateInstanceOfElements() {
        assertThat(list(of(1, 2, 3))).isEqualTo(List.of(1, 2, 3));
    }

    @Test
    public void shouldCreateListOfIterable() {
        assertThat(list(Iterator.ofAll(java.util.List.of(1, 2)))).isEqualTo(List.of(1, 2));
    }

    @Test
    public void shouldCreateStreamFromEmptyJavaUtilStream() {
        assertThat(list(Iterator.ofAll(java.util.stream.Stream.<Integer> empty().iterator()))).isEmpty();
    }

    @Test
    public void shouldCreateStreamFromNonEmptyJavaUtilStream() {
        assertThat(list(Iterator.ofAll(java.util.stream.Stream.of(1, 2, 3).iterator()))).isEqualTo(List.of(1, 2, 3));
    }

    // -- isEmpty

    @Test
    public void shouldRecognizeNil() {
        assertThat(empty().isEmpty()).isTrue();
    }

    @Test
    public void shouldRecognizeNonNil() {
        assertThat(of(1).isEmpty()).isFalse();
    }

    @Test
    public void shouldCalculateIsEmpty() {
        assertThat(empty().isEmpty()).isTrue();
        assertThat(of(1).isEmpty()).isFalse();
    }

    // -- hasNext, next

    @Test
    public void shouldNotHasNextWhenNilIterator() {
        assertThat(empty().hasNext()).isFalse();
    }

    @Test
    public void shouldThrowOnNextWhenNilIterator() {
        assertThrows(NoSuchElementException.class, () -> empty().next());
    }

    @Test
    public void shouldIterateFirstElementOfNonNil() {
        assertThat(of(1, 2, 3).next()).isEqualTo(1);
    }

    @Test
    public void shouldFullyIterateNonNil() {
        final Iterator<Integer> iterator = of(1, 2, 3);
        int actual;
        for (int i = 1; i <= 3; i++) {
            actual = iterator.next();
            assertThat(actual).isEqualTo(i);
        }
        assertThat(iterator.hasNext()).isFalse();
    }

    @Test
    public void shouldThrowWhenCallingNextOnEmptyIterator() {
        assertThatThrownBy(() -> empty().next()).isInstanceOf(NoSuchElementException.class);
    }

    @Test
    public void shouldThrowWhenCallingNextTooOftenOnNonEmptyIterator() {
        final Iterator<Integer> iterator = of(1);
        assertThatThrownBy(() -> {
            iterator.next();
            iterator.next();
        }).isInstanceOf(NoSuchElementException.class);
    }

    @Nested
    class StaticOfallTests {
        @Test
        public void shouldFailOfEmptyIterable() {
            assertThrows(NoSuchElementException.class, () -> Iterator.ofAll(List.empty()).next());
        }

        @Test
        public void shouldReturnTheSameInstanceWhenTheIterableIsAnIterator() {
            final Iterator<Integer> iterator = of(1, 2);
            assertThatIterator(Iterator.ofAll((Iterable<Integer>) iterator)).isSameAs(iterator);
            assertThatIterator(Iterator.ofAll((java.util.Iterator<Integer>) iterator)).isSameAs(iterator);
        }

        @Test
        public void shouldWrapAJavaIterator() {
            assertThat(list(Iterator.ofAll(java.util.List.of(1, 2, 3).iterator()))).isEqualTo(List.of(1, 2, 3));
        }

        @Test
        public void shouldWrapAnIterable() {
            assertThat(list(Iterator.ofAll(java.util.List.of(1, 2, 3)))).isEqualTo(List.of(1, 2, 3));
            assertThat(list(Iterator.ofAll(Vector.of(1, 2, 3)))).isEqualTo(List.of(1, 2, 3));
        }

        @Test
        public void shouldRejectANullIterable() {
            assertThatNullPointerException().isThrownBy(() -> Iterator.ofAll((Iterable<Integer>) null));
            assertThatNullPointerException().isThrownBy(() -> Iterator.ofAll((java.util.Iterator<Integer>) null));
        }

        @Test
        public void shouldFailOfEmptyBoolean() {
            assertThrows(NoSuchElementException.class, () -> Iterator.ofAll(new boolean[0]).next());
        }

        @Test
        public void shouldFailOfEmptyByte() {
            assertThrows(NoSuchElementException.class, () -> Iterator.ofAll(new byte[0]).next());
        }

        @Test
        public void shouldFailOfEmptyChar() {
            assertThrows(NoSuchElementException.class, () -> Iterator.ofAll(new char[0]).next());
        }

        @Test
        public void shouldFailOfEmptyDouble() {
            assertThrows(NoSuchElementException.class, () -> Iterator.ofAll(new double[0]).next());
        }

        @Test
        public void shouldFailOfEmptyFloat() {
            assertThrows(NoSuchElementException.class, () -> Iterator.ofAll(new float[0]).next());
        }

        @Test
        public void shouldFailOfEmptyInt() {
            assertThrows(NoSuchElementException.class, () -> Iterator.ofAll(new int[0]).next());
        }

        @Test
        public void shouldFailOfEmptyLong() {
            assertThrows(NoSuchElementException.class, () -> Iterator.ofAll(new long[0]).next());
        }

        @Test
        public void shouldFailOfEmptyShort() {
            assertThrows(NoSuchElementException.class, () -> Iterator.ofAll(new short[0]).next());
        }

        @Test
        public void shouldCreateListOfPrimitiveBooleanArray() {
            assertThat(list(Iterator.ofAll(true, false))).isEqualTo(List.of(true, false));
        }

        @Test
        public void shouldCreateListOfPrimitiveByteArray() {
            assertThat(list(Iterator.ofAll((byte) 1, (byte) 2, (byte) 3))).isEqualTo(List.of((byte) 1, (byte) 2, (byte) 3));
        }

        @Test
        public void shouldCreateListOfPrimitiveCharArray() {
            assertThat(list(Iterator.ofAll('a', 'b', 'c'))).isEqualTo(List.of('a', 'b', 'c'));
        }

        @Test
        public void shouldCreateListOfPrimitiveDoubleArray() {
            assertThat(list(Iterator.ofAll(1d, 2d, 3d))).isEqualTo(List.of(1d, 2d, 3d));
        }

        @Test
        public void shouldCreateListOfPrimitiveFloatArray() {
            assertThat(list(Iterator.ofAll(1f, 2f, 3f))).isEqualTo(List.of(1f, 2f, 3f));
        }

        @Test
        public void shouldCreateListOfPrimitiveIntArray() {
            assertThat(list(Iterator.ofAll(1, 2, 3))).isEqualTo(List.of(1, 2, 3));
        }

        @Test
        public void shouldCreateListOfPrimitiveLongArray() {
            assertThat(list(Iterator.ofAll(1L, 2L, 3L))).isEqualTo(List.of(1L, 2L, 3L));
        }

        @Test
        public void shouldCreateListOfPrimitiveShortArray() {
            assertThat(list(Iterator.ofAll((short) 1, (short) 2, (short) 3))).isEqualTo(List.of((short) 1, (short) 2, (short) 3));
        }
    }

    // -- static tabulate, fill

    @Test
    public void shouldTabulateTheSeq() {
        assertThat(list(Iterator.tabulate(3, i -> i * i))).isEqualTo(List.of(0, 1, 4));
    }

    @Test
    public void shouldTabulateTheSeqCallingTheFunctionInTheRightOrder() {
        final java.util.LinkedList<Integer> ints = new java.util.LinkedList<>(java.util.List.of(0, 1, 2));
        assertThat(list(Iterator.tabulate(3, i -> ints.remove()))).isEqualTo(List.of(0, 1, 2));
    }

    @Test
    public void shouldTabulateTheSeqWith0Elements() {
        assertThatIterator(Iterator.tabulate(0, i -> i)).isSameAs(empty());
    }

    @Test
    public void shouldTabulateTheSeqWith0ElementsWhenNIsNegative() {
        assertThatIterator(Iterator.tabulate(-1, i -> i)).isSameAs(empty());
    }

    @Nested
    class FillIntSupplierTests {
        @Test
        public void shouldReturnManyAfterFillWithConstantSupplier() {
            assertThat(list(Iterator.fill(17, () -> 7))).hasSize(17);
        }

        @Test
        public void shouldFillTheSeqCallingTheSupplierInTheRightOrder() {
            final java.util.LinkedList<Integer> ints = new java.util.LinkedList<>(java.util.List.of(0, 1));
            assertThat(list(Iterator.fill(2, ints::remove))).isEqualTo(List.of(0, 1));
        }

        @Test
        public void shouldFillTheSeqWith0Elements() {
            assertThatIterator(Iterator.fill(0, () -> 1)).isSameAs(empty());
        }

        @Test
        public void shouldFillTheSeqWith0ElementsWhenNIsNegative() {
            assertThatIterator(Iterator.fill(-1, () -> 1)).isSameAs(empty());
        }
    }

    @Nested
    class FillIntTTests {
        @Test
        public void shouldReturnEmptyAfterFillWithZeroCount() {
            assertThatIterator(Iterator.fill(0, 7)).isSameAs(empty());
        }

        @Test
        public void shouldReturnEmptyAfterFillWithNegativeCount() {
            assertThatIterator(Iterator.fill(-1, 7)).isSameAs(empty());
        }

        @Test
        public void shouldReturnManyAfterFillWithConstant() {
            assertThat(list(Iterator.fill(17, 7))).hasSize(17);
            assertThat(list(Iterator.fill(3, 7))).isEqualTo(List.of(7, 7, 7));
        }
    }

    // -- static concat

    @Nested
    class StaticConcatTests {
        @Test
        public void shouldConcatEmptyIterableIterable() {
            final Iterable<Iterable<Integer>> empty = List.empty();
            assertThatIterator(concat(empty)).isSameAs(Iterator.empty());
        }

        @Test
        public void shouldConcatNonEmptyIterableIterable() {
            final Iterable<Iterable<Integer>> itIt = List.of(List.of(1, 2), List.of(3));
            assertThat(list(concat(itIt))).isEqualTo(List.of(1, 2, 3));
        }

        @Test
        public void shouldConcatEmptyArrayIterable() {
            assertThatIterator(concat()).isSameAs(Iterator.empty());
        }

        @Test
        public void shouldConcatNonEmptyArrayIterable() {
            assertThat(list(concat(List.of(1, 2), List.of(3)))).isEqualTo(List.of(1, 2, 3));
        }

        @Test
        public void shouldConcatNestedConcatIterators() {
            assertThat(list(concat(List.of(1, 2), List.of(3), concat(List.of(4, 5))))).isEqualTo(List.of(1, 2, 3, 4, 5));
            assertThat(list(concat(concat(List.of(4, 5)), List.of(1, 2), List.of(3)))).isEqualTo(List.of(4, 5, 1, 2, 3));
        }

        @Test
        public void shouldConcatToConcatIterator() {
            assertThat(list(concat(List.of(1, 2)).concat(List.of(3).iterator()))).isEqualTo(List.of(1, 2, 3));
        }
    }

    // -- concat(java.util.Iterator)

    @Nested
    class ConcatTests {
        @Test
        public void shouldConcatThisNonEmptyWithEmpty() {
            final Iterator<Integer> it = Iterator.of(1);
            assertThatIterator(it.concat(Iterator.<Integer> empty())).isSameAs(it);
        }

        @Test
        public void shouldConcatThisEmptyWithNonEmpty() {
            final Iterator<Integer> it = Iterator.of(1);
            assertThatIterator(Iterator.<Integer> empty().concat(it)).isSameAs(it);
        }

        @Test
        public void shouldConcatThisNonEmptyWithNonEmpty() {
            assertThat(list(Iterator.of(1).concat(Iterator.of(2)))).isEqualTo(List.of(1, 2));
        }

        @Test
        public void shouldWrapAPlainJavaIteratorOnConcat() {
            assertThat(list(Iterator.of(1).concat(java.util.List.of(2, 3).iterator()))).isEqualTo(List.of(1, 2, 3));
        }
    }

    // -- static from

    @Nested
    class StaticFromIntTests {
        @Test
        public void shouldGenerateIntStream() {
            assertThat(list(from(-1).take(3))).isEqualTo(List.of(-1, 0, 1));
        }

        @Test
        public void shouldGenerateOverflowingIntStream() {
            //noinspection NumericOverflow
            assertThat(list(from(Integer.MAX_VALUE).take(2))).isEqualTo(List.of(Integer.MAX_VALUE, Integer.MAX_VALUE + 1));
        }
    }

    @Nested
    class StaticFromIntIntTests {
        @Test
        public void shouldGenerateIntStreamWithStep() {
            assertThat(list(from(-1, 6).take(3))).isEqualTo(List.of(-1, 5, 11));
        }

        @Test
        public void shouldGenerateOverflowingIntStreamWithStep() {
            //noinspection NumericOverflow
            assertThat(list(from(Integer.MAX_VALUE, 2).take(2))).isEqualTo(List.of(Integer.MAX_VALUE, Integer.MAX_VALUE + 2));
        }
    }

    @Nested
    class StaticFromLongTests {
        @Test
        public void shouldGenerateLongStream() {
            assertThat(list(from(-1L).take(3))).isEqualTo(List.of(-1L, 0L, 1L));
        }

        @Test
        public void shouldGenerateOverflowingLongStream() {
            //noinspection NumericOverflow
            assertThat(list(from(Long.MAX_VALUE).take(2))).isEqualTo(List.of(Long.MAX_VALUE, Long.MAX_VALUE + 1));
        }
    }

    @Nested
    class StaticFromLongLongTests {
        @Test
        public void shouldGenerateLongStreamWithStep() {
            assertThat(list(from(-1L, 5L).take(3))).isEqualTo(List.of(-1L, 4L, 9L));
        }

        @Test
        public void shouldGenerateOverflowingLongStreamWithStep() {
            //noinspection NumericOverflow
            assertThat(list(from(Long.MAX_VALUE, 2).take(2))).isEqualTo(List.of(Long.MAX_VALUE, Long.MAX_VALUE + 2));
        }
    }

    // -- static continually, iterate

    @Nested
    class StaticContinuallySupplierTests {
        @Test
        public void shouldGenerateInfiniteStreamBasedOnSupplier() {
            assertThat(continually(() -> 1).take(13).foldLeft(0, Integer::sum)).isEqualTo(13);
        }

        @Test
        public void shouldGenerateInfiniteStreamBasedOnConstant() {
            assertThat(continually(1).take(13).foldLeft(0, Integer::sum)).isEqualTo(13);
        }
    }

    @Nested
    class StaticIterateTFunctionTests {
        @Test
        public void shouldGenerateInfiniteStreamBasedOnSupplierWithAccessToPreviousValue() {
            assertThat(iterate(2, (i) -> i + 2).take(3).foldLeft(0, Integer::sum)).isEqualTo(12);
        }

        @Test
        public void shouldNotCallSupplierUntilNecessary() {
            assertThat(iterate(2, (i) -> {
                throw new RuntimeException();
            }).next()).isEqualTo(2);
        }
    }

    // -- static iterate(Supplier<Option>)

    static class OptionSupplier implements Supplier<Option<Integer>> {

        int cnt;
        final int end;

        OptionSupplier(int start) {
            this(start, Integer.MAX_VALUE);
        }

        OptionSupplier(int start, int end) {
            this.cnt = start;
            this.end = end;
        }

        @Override
        public Option<Integer> get() {
            Option<Integer> res;
            if (cnt < end) {
                res = Option.some(cnt);
            } else {
                res = Option.none();
            }
            cnt++;
            return res;
        }
    }

    @Test
    public void shouldGenerateInfiniteStreamBasedOnOptionSupplier() {
        assertThat(Iterator.iterate(new OptionSupplier(1)).take(5).foldLeft(0, Integer::sum)).isEqualTo(15);
    }

    @Test
    public void shouldGenerateFiniteStreamBasedOnOptionSupplier() {
        assertThat(Iterator.iterate(new OptionSupplier(1, 4)).take(50000).foldLeft(0, Integer::sum)).isEqualTo(6);
    }

    // -- static unfold, unfoldLeft, unfoldRight

    @Nested
    class UnfoldrightTests {
        @Test
        void shouldUnfoldRightLazily() {
            AtomicInteger calls = new AtomicInteger();

            Iterator<Integer> it = Iterator.unfoldRight(1, i -> {
                calls.incrementAndGet();
                return i <= 3 ? Option.some(Tuple.of(i, i + 1)) : Option.none();
            });

            assertThat(calls.get()).isZero();

            assertThat(it.hasNext()).isTrue();
            assertThat(calls.get()).isEqualTo(1);

            assertThat(it.next()).isEqualTo(1);
            assertThat(calls.get()).isEqualTo(1);

            assertThat(it.hasNext()).isTrue();
            assertThat(calls.get()).isEqualTo(2);

            assertThat(it.next()).isEqualTo(2);
            assertThat(calls.get()).isEqualTo(2);

            assertThat(it.hasNext()).isTrue();
            assertThat(calls.get()).isEqualTo(3);

            assertThat(it.next()).isEqualTo(3);
            assertThat(calls.get()).isEqualTo(3);

            assertThat(it.hasNext()).isFalse();
            assertThat(calls.get()).isEqualTo(4);
        }

        @Test
        public void shouldUnfoldRightToEmpty() {
            assertThat(list(Iterator.unfoldRight(0, x -> Option.none()))).isEqualTo(List.empty());
        }

        @Test
        public void shouldUnfoldRightSimpleList() {
            assertThat(list(
                    Iterator.unfoldRight(10, x -> x == 0
                                                  ? Option.none()
                                                  : Option.some(new Tuple2<>(x, x - 1)))))
                    .isEqualTo(List.of(10, 9, 8, 7, 6, 5, 4, 3, 2, 1));
        }

        @Test
        public void shouldUnfoldLeftToEmpty() {
            assertThat(list(Iterator.unfoldLeft(0, x -> Option.none()))).isEqualTo(List.empty());
        }

        @Test
        public void shouldUnfoldLeftSimpleList() {
            assertThat(list(
                    Iterator.unfoldLeft(10, x -> x == 0
                                                 ? Option.none()
                                                 : Option.some(new Tuple2<>(x - 1, x)))))
                    .isEqualTo(List.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10));
        }

        @Test
        public void shouldUnfoldToEmpty() {
            assertThat(list(Iterator.unfold(0, x -> Option.none()))).isEqualTo(List.empty());
        }

        @Test
        public void shouldUnfoldSimpleList() {
            assertThat(list(
                    Iterator.unfold(10, x -> x == 0
                                             ? Option.none()
                                             : Option.some(new Tuple2<>(x - 1, x)))))
                    .isEqualTo(List.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10));
        }
    }

    // -- static range, rangeBy, rangeClosed, rangeClosedBy

    @Nested
    class RangeTests {
        @Test
        public void shouldCreateIntRanges() {
            assertThat(list(Iterator.range(1, 4))).isEqualTo(List.of(1, 2, 3));
            assertThat(list(Iterator.range(1, 1))).isEmpty();
            assertThat(list(Iterator.range(4, 1))).isEmpty();
            assertThat(list(Iterator.rangeClosed(1, 3))).isEqualTo(List.of(1, 2, 3));
            assertThat(list(Iterator.rangeClosed(1, 1))).isEqualTo(List.of(1));
            assertThat(list(Iterator.rangeClosed(3, 1))).isEmpty();
            assertThat(list(Iterator.rangeBy(1, 6, 2))).isEqualTo(List.of(1, 3, 5));
            assertThat(list(Iterator.rangeBy(6, 1, -2))).isEqualTo(List.of(6, 4, 2));
            assertThat(list(Iterator.rangeBy(1, 6, -2))).isEmpty();
            assertThat(list(Iterator.rangeClosedBy(1, 5, 2))).isEqualTo(List.of(1, 3, 5));
            assertThat(list(Iterator.rangeClosedBy(5, 1, -2))).isEqualTo(List.of(5, 3, 1));
            assertThrows(IllegalArgumentException.class, () -> Iterator.rangeBy(1, 3, 0));
            assertThrows(IllegalArgumentException.class, () -> Iterator.rangeClosedBy(1, 3, 0));
        }

        @Test
        public void shouldCreateIntRangesAtTheBoundsOfInt() {
            assertThat(list(Iterator.rangeClosed(Integer.MAX_VALUE - 1, Integer.MAX_VALUE))).isEqualTo(List.of(Integer.MAX_VALUE - 1, Integer.MAX_VALUE));
            assertThat(list(Iterator.rangeClosedBy(Integer.MAX_VALUE - 3, Integer.MAX_VALUE, 3))).isEqualTo(List.of(Integer.MAX_VALUE - 3, Integer.MAX_VALUE));
            assertThat(list(Iterator.rangeClosedBy(Integer.MIN_VALUE + 3, Integer.MIN_VALUE, -3))).isEqualTo(List.of(Integer.MIN_VALUE + 3, Integer.MIN_VALUE));
            assertThat(list(Iterator.range(Integer.MIN_VALUE, Integer.MIN_VALUE + 2))).isEqualTo(List.of(Integer.MIN_VALUE, Integer.MIN_VALUE + 1));
        }

        @Test
        public void shouldCreateLongRanges() {
            assertThat(list(Iterator.range(1L, 4L))).isEqualTo(List.of(1L, 2L, 3L));
            assertThat(list(Iterator.range(4L, 1L))).isEmpty();
            assertThat(list(Iterator.rangeClosed(1L, 3L))).isEqualTo(List.of(1L, 2L, 3L));
            assertThat(list(Iterator.rangeBy(1L, 6L, 2L))).isEqualTo(List.of(1L, 3L, 5L));
            assertThat(list(Iterator.rangeClosedBy(5L, 1L, -2L))).isEqualTo(List.of(5L, 3L, 1L));
            assertThat(list(Iterator.rangeClosedBy(Long.MAX_VALUE - 3, Long.MAX_VALUE, 3))).isEqualTo(List.of(Long.MAX_VALUE - 3, Long.MAX_VALUE));
            assertThrows(IllegalArgumentException.class, () -> Iterator.rangeBy(1L, 3L, 0L));
        }

        @Test
        public void shouldCreateCharRanges() {
            assertThat(list(Iterator.range('a', 'd'))).isEqualTo(List.of('a', 'b', 'c'));
            assertThat(list(Iterator.range('d', 'a'))).isEmpty();
            assertThat(list(Iterator.rangeClosed('a', 'c'))).isEqualTo(List.of('a', 'b', 'c'));
            assertThat(list(Iterator.rangeBy('a', 'd', 2))).isEqualTo(List.of('a', 'c'));
            assertThat(list(Iterator.rangeBy('d', 'a', -2))).isEqualTo(List.of('d', 'b'));
            assertThat(list(Iterator.rangeBy('d', 'a', 2))).isEmpty();
            assertThat(list(Iterator.rangeClosedBy('a', 'e', 2))).isEqualTo(List.of('a', 'c', 'e'));
        }

        @Test
        public void shouldCreateDoubleRanges() {
            assertThat(list(Iterator.rangeBy(1.0, 2.0, 0.5))).isEqualTo(List.of(1.0, 1.5));
            assertThat(list(Iterator.rangeBy(2.0, 1.0, -0.5))).isEqualTo(List.of(2.0, 1.5));
            assertThat(list(Iterator.rangeBy(1.0, 1.0, 0.5))).isEmpty();
            assertThat(list(Iterator.rangeClosedBy(1.0, 2.0, 0.5))).isEqualTo(List.of(1.0, 1.5, 2.0));
            assertThat(list(Iterator.rangeClosedBy(0.1, 0.4, 0.1))).isEqualTo(List.of(0.1, 0.2, 0.3, 0.4));
            assertThrows(IllegalArgumentException.class, () -> Iterator.rangeBy(1.0, 2.0, 0.0));
        }

        @Test
        public void shouldCreateBigDecimalRanges() {
            assertThat(list(Iterator.rangeBy(new BigDecimal("1.0"), new BigDecimal("3.0"), new BigDecimal("1.0"))))
                    .isEqualTo(List.of(new BigDecimal("1.0"), new BigDecimal("2.0")));
            assertThat(list(Iterator.rangeBy(new BigDecimal("4.0"), new BigDecimal("1.0"), new BigDecimal("-2.0"))))
                    .isEqualTo(List.of(new BigDecimal("4.0"), new BigDecimal("2.0")));
            assertThat(list(Iterator.rangeBy(new BigDecimal("4.0"), new BigDecimal("1.0"), new BigDecimal("2.0")))).isEmpty();
            assertThat(list(Iterator.rangeBy(new BigDecimal("1.0"), new BigDecimal("1.00"), new BigDecimal("1")))).isEmpty();
            assertThrows(IllegalArgumentException.class, () -> Iterator.rangeBy(BigDecimal.ONE, BigDecimal.TEN, BigDecimal.ZERO));
        }

        @Test
        public void shouldCreateDoubleRangeByFromInfinity() {
            assertThat(list(rangeBy(Double.NEGATIVE_INFINITY, 0.0, 1.0).take(2))).isEqualTo(List.of(Double.NEGATIVE_INFINITY, -Double.MAX_VALUE));
            assertThat(list(rangeBy(Double.POSITIVE_INFINITY, 0.0, -1.0).take(2))).isEqualTo(List.of(Double.POSITIVE_INFINITY, Double.MAX_VALUE));
        }

        @Test
        public void shouldCreateDoubleRangeClosedByFromInfinity() {
            assertThat(list(rangeClosedBy(Double.NEGATIVE_INFINITY, 0.0, 1.0).take(2))).isEqualTo(List.of(Double.NEGATIVE_INFINITY, -Double.MAX_VALUE));
            assertThat(list(rangeClosedBy(Double.POSITIVE_INFINITY, 0.0, -1.0).take(2))).isEqualTo(List.of(Double.POSITIVE_INFINITY, Double.MAX_VALUE));
        }

        @Test
        public void shouldCreateDoubleRangeByFromMaxToInfinity() {
            assertThat(list(rangeBy(Double.MAX_VALUE, Double.POSITIVE_INFINITY, 3E307))).isEqualTo(List.of(Double.MAX_VALUE));
            assertThat(list(rangeBy(-Double.MAX_VALUE, Double.NEGATIVE_INFINITY, -3E307))).isEqualTo(List.of(-Double.MAX_VALUE));
        }

        @Test
        public void shouldCreateDoubleRangeClosedByFromMaxToInfinity() {
            assertThat(list(rangeClosedBy(Double.MAX_VALUE, Double.POSITIVE_INFINITY, 3E307))).isEqualTo(List.of(Double.MAX_VALUE));
            assertThat(list(rangeClosedBy(-Double.MAX_VALUE, Double.NEGATIVE_INFINITY, -3E307))).isEqualTo(List.of(-Double.MAX_VALUE));
        }
    }

    // -- distinctBy(Comparator), distinctBy(Function)

    @Test
    public void shouldComputeDistinctByOfEmptyTraversableUsingComparator() {
        final Comparator<Integer> comparator = comparingInt(i -> i);
        assertThat(list(Iterator.<Integer> empty().distinctBy(comparator))).isEmpty();
    }

    @Test
    public void shouldReturnSameInstanceWhenDistinctByComparatorEmptyTraversable() {
        final Iterator<?> empty = empty();
        assertThatIterator(empty.distinctBy(Comparators.naturalComparator())).isSameAs(empty);
    }

    @Test
    public void shouldComputeDistinctByOfNonEmptyTraversableUsingComparator() {
        final Comparator<String> comparator = comparingInt(s -> (s.charAt(1)));
        final Iterator<String> distinct = of("1a", "2a", "3a", "3b", "4b", "5c").distinctBy(comparator).map(s -> s.substring(1));
        assertThat(list(distinct)).isEqualTo(List.of("a", "b", "c"));
    }

    @Test
    public void shouldComputeDistinctByOfEmptyTraversableUsingKeyExtractor() {
        assertThat(list(empty().distinctBy(Function.identity()))).isEmpty();
    }

    @Test
    public void shouldReturnSameInstanceWhenDistinctByFunctionEmptyTraversable() {
        final Iterator<?> empty = empty();
        assertThatIterator(empty.distinctBy(Function.identity())).isSameAs(empty);
    }

    @Test
    public void shouldComputeDistinctByOfNonEmptyTraversableUsingKeyExtractor() {
        final Function<String, Character> function = c -> c.charAt(1);
        final Iterator<String> distinct = of("1a", "2a", "3a", "3b", "4b", "5c").distinctBy(function).map(s -> s.substring(1));
        assertThat(list(distinct)).isEqualTo(List.of("a", "b", "c"));
    }

    @Test
    public void shouldThrowOnDistinctByWithNullArgument() {
        assertThrows(NullPointerException.class, () -> of(1).distinctBy((Comparator<Integer>) null));
        assertThrows(NullPointerException.class, () -> of(1).distinctBy((Function<Integer, Integer>) null));
    }

    // -- distinctByKeepLast

    @Test
    public void shouldKeepTheLastOccurrenceOnDistinctByKeepLastUsingComparator() {
        assertThat(list(of(1, 2, 2, 3, 1).distinctByKeepLast(Comparator.naturalOrder()))).isEqualTo(List.of(2, 3, 1));
        assertThatIterator(Iterator.<Integer> empty().distinctByKeepLast(Comparator.naturalOrder())).isSameAs(empty());
    }

    @Test
    public void shouldKeepTheLastOccurrenceOnDistinctByKeepLastUsingKeyExtractor() {
        assertThat(list(of("a", "ab", "abc", "b").distinctByKeepLast(String::length))).isEqualTo(List.of("ab", "abc", "b"));
        assertThatIterator(Iterator.<String> empty().distinctByKeepLast(String::length)).isSameAs(empty());
    }

    @Test
    public void shouldThrowOnDistinctByKeepLastWithNullArgument() {
        assertThrows(NullPointerException.class, () -> of(1).distinctByKeepLast((Comparator<Integer>) null));
        assertThrows(NullPointerException.class, () -> of(1).distinctByKeepLast((Function<Integer, Integer>) null));
    }

    // -- drop

    @Test
    public void shouldDropNoneOnNil() {
        assertThatIterator(empty().drop(1)).isSameAs(empty());
    }

    @Test
    public void shouldDropNoneIfCountIsNegative() {
        assertThat(list(of(1, 2, 3).drop(-1))).isEqualTo(List.of(1, 2, 3));
    }

    @Test
    public void shouldDropAsExpectedIfCountIsLessThanSize() {
        assertThat(list(of(1, 2, 3).drop(2))).isEqualTo(List.of(3));
    }

    @Test
    public void shouldDropAllIfCountExceedsSize() {
        assertThat(list(of(1, 2, 3).drop(4))).isEqualTo(List.empty());
    }

    @Test
    public void shouldReturnSameInstanceWhenDropZeroCount() {
        final Iterator<Integer> t = of(1, 2, 3);
        assertThatIterator(t.drop(0)).isSameAs(t);
    }

    @Test
    public void shouldReturnSameInstanceWhenDropNegativeCount() {
        final Iterator<Integer> t = of(1, 2, 3);
        assertThatIterator(t.drop(-1)).isSameAs(t);
    }

    @Test
    public void shouldReturnSameInstanceWhenEmptyDropOne() {
        final Iterator<?> empty = empty();
        assertThatIterator(empty.drop(1)).isSameAs(empty);
    }

    @Test
    public void shouldDropLazily() {
        final AtomicInteger pulled = new AtomicInteger();
        final Iterator<Integer> dropped = Iterator.iterate(1, i -> {
            pulled.incrementAndGet();
            return i + 1;
        }).drop(2);
        assertThat(pulled.get()).isEqualTo(0);
        assertThat(dropped.next()).isEqualTo(3);
    }

    // -- dropRight

    @Test
    public void shouldDropRightNoneOnNil() {
        assertThatIterator(empty().dropRight(1)).isSameAs(empty());
    }

    @Test
    public void shouldDropRightNoneIfCountIsNegative() {
        assertThat(list(of(1, 2, 3).dropRight(-1))).isEqualTo(List.of(1, 2, 3));
    }

    @Test
    public void shouldDropRightAsExpectedIfCountIsLessThanSize() {
        assertThat(list(of(1, 2, 3).dropRight(2))).isEqualTo(List.of(1));
    }

    @Test
    public void shouldDropRightAllIfCountExceedsSize() {
        assertThat(list(of(1, 2, 3).dropRight(4))).isEqualTo(List.empty());
    }

    @Test
    public void shouldReturnSameInstanceWhenDropRightZeroCount() {
        final Iterator<Integer> t = of(1, 2, 3);
        assertThatIterator(t.dropRight(0)).isSameAs(t);
    }

    @Test
    public void shouldReturnSameInstanceWhenDropRightNegativeCount() {
        final Iterator<Integer> t = of(1, 2, 3);
        assertThatIterator(t.dropRight(-1)).isSameAs(t);
    }

    @Test
    public void shouldReturnSameInstanceWhenEmptyDropRightOne() {
        final Iterator<?> empty = empty();
        assertThatIterator(empty.dropRight(1)).isSameAs(empty);
    }

    // -- dropWhile

    @Test
    public void shouldDropWhileNoneOnNil() {
        final Iterator<?> empty = empty();
        assertThatIterator(empty.dropWhile(ignored -> true)).isSameAs(empty);
    }

    @Test
    public void shouldDropWhileNoneIfPredicateIsFalse() {
        assertThat(list(of(1, 2, 3).dropWhile(ignored -> false))).isEqualTo(List.of(1, 2, 3));
    }

    @Test
    public void shouldDropWhileAllIfPredicateIsTrue() {
        assertThat(list(of(1, 2, 3).dropWhile(ignored -> true))).isEqualTo(List.empty());
    }

    @Test
    public void shouldDropWhileAccordingToPredicate() {
        assertThat(list(of(1, 2, 3).dropWhile(i -> i < 2))).isEqualTo(List.of(2, 3));
    }

    @Test
    public void shouldDropWhileAndNotTruncate() {
        assertThat(list(of(1, 2, 3).dropWhile(i -> i % 2 == 1))).isEqualTo(List.of(2, 3));
    }

    @Test
    public void shouldReturnSameInstanceWhenEmptyDropWhile() {
        final Iterator<?> empty = empty();
        assertThatIterator(empty.dropWhile(ignored -> true)).isSameAs(empty);
    }

    @Test
    public void shouldThrowOnDropWhileWithNullPredicate() {
        assertThrows(NullPointerException.class, () -> of(1).dropWhile(null));
    }

    // -- filter

    @Test
    public void shouldFilterExistingElements() {
        assertThat(list(of(1, 2, 3).filter(i -> i == 1))).isEqualTo(List.of(1));
        assertThat(list(of(1, 2, 3).filter(i -> i == 2))).isEqualTo(List.of(2));
        assertThat(list(of(1, 2, 3).filter(i -> i == 3))).isEqualTo(List.of(3));
        assertThat(list(of(1, 2, 3).filter(ignore -> true))).isEqualTo(List.of(1, 2, 3));
    }

    @Test
    public void shouldFilterNonExistingElements() {
        assertThat(list(Iterator.<Integer> empty().filter(i -> i == 0))).isEqualTo(List.empty());
        assertThat(list(of(1, 2, 3).filter(i -> i == 0))).isEqualTo(List.empty());
    }

    @Test
    public void shouldReturnSameInstanceWhenFilteringEmptyTraversable() {
        final Iterator<?> empty = empty();
        assertThatIterator(empty.filter(v -> true)).isSameAs(empty);
    }

    @Test
    public void shouldFilterLazilyAndTestEachElementOnce() {
        final AtomicInteger tests = new AtomicInteger();
        final Iterator<Integer> filtered = of(1, 2, 3, 4).filter(i -> {
            tests.incrementAndGet();
            return i % 2 == 0;
        });
        assertThat(tests.get()).isEqualTo(0);
        assertThat(filtered.hasNext()).isTrue();
        assertThat(filtered.hasNext()).isTrue();
        assertThat(tests.get()).isEqualTo(2);
        assertThat(filtered.next()).isEqualTo(2);
        assertThat(filtered.next()).isEqualTo(4);
        assertThat(filtered.hasNext()).isFalse();
        assertThat(tests.get()).isEqualTo(4);
    }

    @Test
    public void shouldThrowOnFilterWithNullPredicate() {
        assertThrows(NullPointerException.class, () -> of(1).filter(null));
    }

    // -- flatMap

    @Test
    public void shouldFlatMapEmpty() {
        assertThatIterator(empty().flatMap(v -> of(v, 0))).isSameAs(empty());
    }

    @Test
    public void shouldFlatMapNonEmpty() {
        assertThat(list(of(1, 2, 3).flatMap(v -> of(v, 0)))).isEqualTo(List.of(1, 0, 2, 0, 3, 0));
    }

    @Test
    public void shouldFlatMapOverAnyIterable() {
        assertThat(list(of(1, 2).flatMap(v -> java.util.List.of(v, v)))).isEqualTo(List.of(1, 1, 2, 2));
        assertThat(list(of(1, 2).flatMap(v -> Vector.empty()))).isEqualTo(List.empty());
    }

    @Test
    public void shouldThrowOnFlatMapWithNullMapper() {
        assertThrows(NullPointerException.class, () -> of(1).flatMap(null));
    }

    // -- map

    @Test
    public void shouldMapEmpty() {
        assertThatIterator(empty().map(i -> i)).isSameAs(empty());
    }

    @Test
    public void shouldMapNonEmpty() {
        assertThat(list(of(1, 2, 3).map(i -> i * 2))).isEqualTo(List.of(2, 4, 6));
    }

    @Test
    public void shouldMapLazily() {
        final AtomicInteger calls = new AtomicInteger();
        final Iterator<Integer> mapped = of(1, 2, 3).map(i -> {
            calls.incrementAndGet();
            return i * 2;
        });
        assertThat(calls.get()).isEqualTo(0);
        assertThat(mapped.hasNext()).isTrue();
        assertThat(calls.get()).isEqualTo(0);
        assertThat(mapped.next()).isEqualTo(2);
        assertThat(calls.get()).isEqualTo(1);
    }

    @Test
    public void shouldThrowOnMapWithNullMapper() {
        assertThrows(NullPointerException.class, () -> of(1).map(null));
    }

    // -- collect

    @Test
    public void shouldCollectNothingFromEmpty() {
        final AtomicInteger calls = new AtomicInteger();
        final Iterator<Integer> actual = Iterator.<Integer> empty().collect(i -> {
            calls.incrementAndGet();
            return Option.some(i);
        });
        assertThat(list(actual)).isEqualTo(List.empty());
        assertThat(calls.get()).isEqualTo(0);
    }

    @Test
    public void shouldCollectNothingWhenEveryElementIsDropped() {
        assertThat(list(of(1, 2, 3).collect(i -> Option.none()))).isEqualTo(List.empty());
    }

    @Test
    public void shouldCollectEveryElementWhenEveryElementIsKept() {
        assertThat(list(of(1, 2, 3).collect(i -> Option.some(i * 10)))).isEqualTo(List.of(10, 20, 30));
    }

    @Test
    public void shouldCollectTheKeptElementsInOrder() {
        assertThat(list(of(1, 2, 3, 4).collect(i -> i % 2 == 0 ? Option.some("e" + i) : Option.none()))).isEqualTo(List.of("e2", "e4"));
    }

    @Test
    public void shouldCollectWithASwitchInsideTheLambda() {
        final Iterator<Integer> actual = of(1, 2, 3).collect(i -> switch (i) {
            case Integer odd when odd % 2 == 1 -> Option.some(odd * 10);
            default -> Option.none();
        });
        assertThat(list(actual)).isEqualTo(List.of(10, 30));
    }

    @Test
    public void shouldCallTheCollectMapperOncePerElement() {
        final AtomicInteger calls = new AtomicInteger();
        list(of(1, 2, 3).collect(i -> {
            calls.incrementAndGet();
            return i == 2 ? Option.none() : Option.some(i);
        }));
        assertThat(calls.get()).isEqualTo(3);
    }

    @Test
    public void shouldRejectNullOptionFromCollectMapper() {
        assertThatThrownBy(() -> list(of(1).collect(i -> null)))
          .isInstanceOf(NullPointerException.class)
          .hasMessage("Iterator.collect: mapper returned null");
    }

    @Test
    public void shouldThrowOnCollectWithNullMapper() {
        final Function<Integer, Option<Integer>> mapper = null;
        assertThrows(NullPointerException.class, () -> of(1).collect(mapper));
    }

    @Nested
    class CollectTests {

        @Test
        public void shouldNotCallTheCollectMapperBeforeTheFirstElementIsRequested() {
            final AtomicInteger calls = new AtomicInteger();
            final Iterator<Integer> actual = Iterator.of(1, 2, 3).collect(i -> {
                calls.incrementAndGet();
                return Option.some(i);
            });
            assertThat(calls.get()).isEqualTo(0);
            assertThat(actual.hasNext()).isTrue();
            assertThat(calls.get()).isEqualTo(1);
            assertThat(actual.next()).isEqualTo(1);
        }

        @Test
        public void shouldCallTheCollectMapperOncePerElementAcrossHasNextCalls() {
            final AtomicInteger calls = new AtomicInteger();
            final Iterator<Integer> actual = Iterator.of(1, 2, 3, 4).collect(i -> {
                calls.incrementAndGet();
                return i % 2 == 0 ? Option.some(i) : Option.none();
            });
            assertThat(actual.hasNext()).isTrue();
            assertThat(actual.hasNext()).isTrue();
            assertThat(actual.next()).isEqualTo(2);
            assertThat(actual.next()).isEqualTo(4);
            assertThat(actual.hasNext()).isFalse();
            assertThat(calls.get()).isEqualTo(4);
        }

        @Test
        public void shouldReturnTheEmptyIteratorForAnEmptySource() {
            assertThatIterator(Iterator.<Integer>empty().collect(i -> Option.some(i))).isSameAs(Iterator.empty());
        }
    }

    // -- take

    @Test
    public void shouldTakeNoneOnNil() {
        assertThatIterator(empty().take(1)).isSameAs(empty());
    }

    @Test
    public void shouldTakeNoneIfCountIsNegative() {
        assertThatIterator(of(1, 2, 3).take(-1)).isSameAs(empty());
    }

    @Test
    public void shouldTakeAsExpectedIfCountIsLessThanSize() {
        assertThat(list(of(1, 2, 3).take(2))).isEqualTo(List.of(1, 2));
    }

    @Test
    public void shouldTakeAllIfCountExceedsSize() {
        assertThat(list(of(1, 2, 3).take(4))).isEqualTo(List.of(1, 2, 3));
    }

    @Test
    public void shouldTakeFromAnInfiniteSourceWithoutPullingPastTheCount() {
        final AtomicInteger pulled = new AtomicInteger();
        assertThat(list(Iterator.continually(pulled::incrementAndGet).take(3))).isEqualTo(List.of(1, 2, 3));
        assertThat(pulled.get()).isEqualTo(3);
    }

    // -- takeRight

    @Test
    public void shouldTakeRightNoneOnNil() {
        assertThat(list(empty().takeRight(1))).isEmpty();
    }

    @Test
    public void shouldTakeRightNoneIfCountIsNegative() {
        assertThatIterator(of(1, 2, 3).takeRight(-1)).isSameAs(empty());
    }

    @Test
    public void shouldTakeRightAsExpectedIfCountIsLessThanSize() {
        assertThat(list(of(1, 2, 3).takeRight(2))).isEqualTo(List.of(2, 3));
    }

    @Test
    public void shouldTakeRightAllIfCountExceedsSize() {
        assertThat(list(of(1, 2, 3).takeRight(4))).isEqualTo(List.of(1, 2, 3));
    }

    // -- takeWhile

    @Test
    public void shouldTakeWhileNoneOnNil() {
        assertThatIterator(empty().takeWhile(x -> true)).isSameAs(empty());
    }

    @Test
    public void shouldTakeWhileAllOnFalseCondition() {
        assertThat(list(of(1, 2, 3).takeWhile(x -> false))).isEqualTo(List.empty());
    }

    @Test
    public void shouldTakeWhileAllOnTrueCondition() {
        assertThat(list(of(1, 2, 3).takeWhile(x -> true))).isEqualTo(List.of(1, 2, 3));
    }

    @Test
    public void shouldTakeWhileAsExpected() {
        assertThat(list(of(2, 4, 5, 6).takeWhile(x -> x % 2 == 0))).isEqualTo(List.of(2, 4));
    }

    @Test
    public void shouldReturnSameInstanceWhenEmptyTakeWhile() {
        final Iterator<?> empty = empty();
        assertThatIterator(empty.takeWhile(ignored -> false)).isSameAs(empty);
    }

    @Test
    public void shouldNotPullPastTheFirstRejectedElementOnTakeWhile() {
        final AtomicInteger pulled = new AtomicInteger();
        final Iterator<Integer> taken = Iterator.continually(pulled::incrementAndGet).takeWhile(i -> i < 3);
        assertThat(list(taken)).isEqualTo(List.of(1, 2));
        assertThat(pulled.get()).isEqualTo(3);
    }

    @Test
    public void shouldThrowOnTakeWhileWithNullPredicate() {
        assertThrows(NullPointerException.class, () -> of(1).takeWhile(null));
    }

    // -- zip

    @Test
    public void shouldZipNils() {
        assertThat(list(empty().zip(empty()))).isEmpty();
    }

    @Test
    public void shouldZipEmptyAndNonNil() {
        assertThat(list(empty().zip(of(1)))).isEmpty();
    }

    @Test
    public void shouldZipNonEmptyAndNil() {
        assertThat(list(of(1).zip(empty()))).isEmpty();
    }

    @Test
    public void shouldZipNonNilsIfThisIsSmaller() {
        assertThat(list(of(1, 2).zip(of("a", "b", "c")))).isEqualTo(List.of(Tuple.of(1, "a"), Tuple.of(2, "b")));
    }

    @Test
    public void shouldZipNonNilsIfThatIsSmaller() {
        assertThat(list(of(1, 2, 3).zip(of("a", "b")))).isEqualTo(List.of(Tuple.of(1, "a"), Tuple.of(2, "b")));
    }

    @Test
    public void shouldZipNonNilsOfSameSize() {
        assertThat(list(of(1, 2, 3).zip(of("a", "b", "c")))).isEqualTo(List.of(Tuple.of(1, "a"), Tuple.of(2, "b"), Tuple.of(3, "c")));
    }

    @Test
    public void shouldZipWithNonNilsOfSameSize() {
        assertThat(list(of(1, 2, 3).zipWith(of("a", "b", "c"), Tuple::of))).isEqualTo(List.of(Tuple.of(1, "a"), Tuple.of(2, "b"), Tuple.of(3, "c")));
    }

    @Test
    public void shouldZipWithAnInfiniteThat() {
        assertThat(list(of(1, 2, 3).zip(Iterator.from(10)))).isEqualTo(List.of(Tuple.of(1, 10), Tuple.of(2, 11), Tuple.of(3, 12)));
    }

    @Test
    public void shouldThrowIfZipWithThatIsNull() {
        assertThrows(NullPointerException.class, () -> empty().zip(null));
        assertThrows(NullPointerException.class, () -> of(1).zipWith(of(1), null));
    }

    // -- zipAll

    @Test
    public void shouldZipAllNils() {
        assertThat(list(empty().zipAll(empty(), 0, 0))).isEmpty();
    }

    @Test
    public void shouldZipAllEmptyAndNonNil() {
        assertThat(list(empty().zipAll(of(1), 0, 0))).isEqualTo(List.of(Tuple.of(0, 1)));
    }

    @Test
    public void shouldZipAllNonEmptyAndNil() {
        assertThat(list(of(1).zipAll(empty(), 0, 0))).isEqualTo(List.of(Tuple.of(1, 0)));
    }

    @Test
    public void shouldRejectNullZipAllFillValues() {
        assertThrows(NullPointerException.class, () -> empty().zipAll(of(1), null, 0));
        assertThrows(NullPointerException.class, () -> empty().zipAll(of(1), 0, null));
    }

    @Test
    public void shouldZipAllNonNilsIfThisIsSmaller() {
        assertThat(list(of(1, 2).zipAll(of("a", "b", "c"), 9, "z"))).isEqualTo(List.of(Tuple.of(1, "a"), Tuple.of(2, "b"), Tuple.of(9, "c")));
    }

    @Test
    public void shouldZipAllNonNilsIfThatIsSmaller() {
        assertThat(list(of(1, 2, 3).zipAll(of("a", "b"), 9, "z"))).isEqualTo(List.of(Tuple.of(1, "a"), Tuple.of(2, "b"), Tuple.of(3, "z")));
    }

    @Test
    public void shouldZipAllNonNilsOfSameSize() {
        assertThat(list(of(1, 2, 3).zipAll(of("a", "b", "c"), 9, "z"))).isEqualTo(List.of(Tuple.of(1, "a"), Tuple.of(2, "b"), Tuple.of(3, "c")));
    }

    @Test
    public void shouldThrowIfZipAllWithThatIsNull() {
        assertThrows(NullPointerException.class, () -> empty().zipAll(null, null, null));
    }

    // -- zipWithIndex

    @Test
    public void shouldZipNilWithIndex() {
        assertThat(list(Iterator.<String> empty().zipWithIndex())).isEmpty();
    }

    @Test
    public void shouldZipNonNilWithIndex() {
        assertThat(list(of("a", "b", "c").zipWithIndex())).isEqualTo(List.of(Tuple.of("a", 0), Tuple.of("b", 1), Tuple.of("c", 2)));
    }

    @Test
    public void shouldZipNonNilWithIndexWithMapper() {
        assertThat(list(of("a", "b", "c").zipWithIndex(Tuple::of))).isEqualTo(List.of(Tuple.of("a", 0), Tuple.of("b", 1), Tuple.of("c", 2)));
    }

    @Test
    public void shouldThrowOnZipWithIndexWithNullMapper() {
        assertThrows(NullPointerException.class, () -> of(1).zipWithIndex(null));
    }

    // -- scanLeft

    @Test
    public void shouldScanLeftEmpty() {
        assertThat(list(Iterator.<Integer> empty().scanLeft(0, (s1, s2) -> s1 + s2))).isEqualTo(List.of(0));
    }

    @Test
    public void shouldScanLeftNonEmpty() {
        assertThat(list(of(1, 2, 3).scanLeft("x", (acc, i) -> acc + i))).isEqualTo(List.of("x", "x1", "x12", "x123"));
    }

    @Test
    public void shouldScanLeftWithNonComparable() {
        final List<NonComparable> actual = list(of(new NonComparable("a")).scanLeft(new NonComparable("x"), (u1, u2) -> new NonComparable(u1.value + u2.value)));
        final List<NonComparable> expected = List.of("x", "xa").map(NonComparable::new);
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void shouldScanLeftLazily() {
        final AtomicInteger calls = new AtomicInteger();
        final Iterator<Integer> scanned = Iterator.from(1).scanLeft(0, (acc, i) -> {
            calls.incrementAndGet();
            return acc + i;
        });
        assertThat(calls.get()).isEqualTo(0);
        assertThat(list(scanned.take(4))).isEqualTo(List.of(0, 1, 3, 6));
        assertThat(calls.get()).isEqualTo(3);
    }

    @Test
    public void shouldThrowOnScanLeftWithNullOperation() {
        assertThrows(NullPointerException.class, () -> of(1).scanLeft(0, null));
    }

    // -- slideBy(classifier)

    @Test
    public void shouldSlideNilByClassifier() {
        assertThatIterator(empty().slideBy(Function.identity())).isSameAs(empty());
    }

    @Test
    public void shouldTerminateSlideByClassifier() {
        assertTimeout(Duration.ofSeconds(1), () -> {
            AtomicInteger ai = new AtomicInteger(0);
            List<Vector<String>> expected = List.of(Vector.of("a", "-"), Vector.of("-"), Vector.of("d"));
            List<Vector<String>> actual = list(of("a", "-", "-", "d")
              .slideBy(x -> x.equals("-") ? ai.getAndIncrement() : ai.get()));
            assertThat(actual).isEqualTo(expected);
        });
    }

    @Test
    public void shouldSlideSingularByClassifier() {
        assertThat(list(of(1).slideBy(Function.identity()))).isEqualTo(List.of(Vector.of(1)));
    }

    @Test
    public void shouldSlideNonNilByIdentityClassifier() {
        assertThat(list(of(1, 2, 3).slideBy(Function.identity()))).isEqualTo(List.of(Vector.of(1), Vector.of(2), Vector.of(3)));
    }

    @Test
    public void shouldSlideNonNilByConstantClassifier() {
        assertThat(list(of(1, 2, 3).slideBy(e -> "same"))).isEqualTo(List.of(Vector.of(1, 2, 3)));
    }

    @Test
    public void shouldSlideNonNilBySomeClassifier() {
        assertThat(list(of(10, 20, 30, 42, 52, 60, 72).slideBy(e -> e % 10)))
          .isEqualTo(List.of(Vector.of(10, 20, 30), Vector.of(42, 52), Vector.of(60), Vector.of(72)));
    }

    @Test
    public void shouldSlideByClassifierReturningNull() {
        assertThat(list(of(1, 2, 3).slideBy(e -> null))).isEqualTo(List.of(Vector.of(1, 2, 3)));
    }

    @Test
    public void shouldSlideByClassifierOneRunAtATime() {
        final AtomicInteger pulled = new AtomicInteger();
        final Iterator<Vector<Integer>> runs = Iterator.continually(pulled::incrementAndGet).slideBy(i -> (i - 1) / 2);
        assertThat(pulled.get()).isEqualTo(0);
        assertThat(runs.next()).isEqualTo(Vector.of(1, 2));
        // the first element of the next run is read to close the previous one
        assertThat(pulled.get()).isEqualTo(3);
    }

    @Test
    public void shouldThrowOnSlideByWithNullClassifier() {
        assertThrows(NullPointerException.class, () -> of(1).slideBy(null));
    }

    // -- sliding(size)

    @Test
    public void shouldThrowWhenSlidingNilByZeroSize() {
        assertThrows(IllegalArgumentException.class, () -> empty().sliding(0));
    }

    @Test
    public void shouldThrowWhenSlidingNilByNegativeSize() {
        assertThrows(IllegalArgumentException.class, () -> empty().sliding(-1));
    }

    @Test
    public void shouldThrowWhenSlidingNonNilByZeroSize() {
        assertThrows(IllegalArgumentException.class, () -> of(1).sliding(0));
    }

    @Test
    public void shouldThrowWhenSlidingNonNilByNegativeSize() {
        assertThrows(IllegalArgumentException.class, () -> of(1).sliding(-1));
    }

    @Test
    public void shouldSlideNilBySize() {
        assertThat(list(empty().sliding(1))).isEmpty();
    }

    @Test
    public void shouldSlideNonNilBySize1() {
        assertThat(list(of(1, 2, 3).sliding(1))).isEqualTo(List.of(Vector.of(1), Vector.of(2), Vector.of(3)));
    }

    @Test
    public void shouldSlideNonNilBySize2() {
        assertThat(list(of(1, 2, 3, 4, 5).sliding(2))).isEqualTo(List.of(Vector.of(1, 2), Vector.of(2, 3), Vector.of(3, 4), Vector.of(4, 5)));
    }

    @Test
    public void shouldSlideAShorterSourceIntoOneWindow() {
        assertThat(list(of(1, 2).sliding(5))).isEqualTo(List.of(Vector.of(1, 2)));
        assertThat(list(of(1, 2).sliding(Integer.MAX_VALUE))).isEqualTo(List.of(Vector.of(1, 2)));
    }

    @Test
    public void shouldSlideAnInfiniteSourceOneWindowAtATime() {
        final AtomicInteger pulled = new AtomicInteger();
        final Iterator<Vector<Integer>> windows = Iterator.continually(pulled::incrementAndGet).sliding(2);
        assertThat(pulled.get()).isEqualTo(2);
        assertThat(windows.next()).isEqualTo(Vector.of(1, 2));
        assertThat(windows.next()).isEqualTo(Vector.of(2, 3));
        assertThat(pulled.get()).isEqualTo(4);
    }

    // -- sliding(size, step)

    @Test
    public void shouldThrowWhenSlidingNilByPositiveStepAndNegativeSize() {
        assertThrows(IllegalArgumentException.class, () -> empty().sliding(-1, 1));
    }

    @Test
    public void shouldThrowWhenSlidingNilByNegativeStepAndNegativeSize() {
        assertThrows(IllegalArgumentException.class, () -> empty().sliding(-1, -1));
    }

    @Test
    public void shouldThrowWhenSlidingNilByNegativeStepAndPositiveSize() {
        assertThrows(IllegalArgumentException.class, () -> empty().sliding(1, -1));
    }

    @Test
    public void shouldSlideNilBySizeAndStep() {
        assertThat(empty().sliding(1, 1).isEmpty()).isTrue();
    }

    @Test
    public void shouldSlide5ElementsBySize2AndStep3() {
        assertThat(list(of(1, 2, 3, 4, 5).sliding(2, 3))).isEqualTo(List.of(Vector.of(1, 2), Vector.of(4, 5)));
    }

    @Test
    public void shouldSlide5ElementsBySize2AndStep4() {
        assertThat(list(of(1, 2, 3, 4, 5).sliding(2, 4))).isEqualTo(List.of(Vector.of(1, 2), Vector.of(5)));
    }

    @Test
    public void shouldSlide5ElementsBySize2AndStep5() {
        assertThat(list(of(1, 2, 3, 4, 5).sliding(2, 5))).isEqualTo(List.of(Vector.of(1, 2)));
    }

    @Test
    public void shouldSlide4ElementsBySize5AndStep3() {
        assertThat(list(of(1, 2, 3, 4).sliding(5, 3))).isEqualTo(List.of(Vector.of(1, 2, 3, 4)));
    }

    @Test
    public void shouldSlide7ElementsBySize1AndStep3() {
        assertThat(list(of(1, 2, 3, 4, 5, 6, 7).sliding(1, 3))).isEqualTo(List.of(Vector.of(1), Vector.of(4), Vector.of(7)));
    }

    @Test
    public void shouldSlide7ElementsBySize2AndStep3() {
        assertThat(list(of(1, 2, 3, 4, 5, 6, 7).sliding(2, 3))).isEqualTo(List.of(Vector.of(1, 2), Vector.of(4, 5), Vector.of(7)));
    }

    @Test
    public void shouldSlideWithAStepSmallerThanTheSize() {
        assertThat(list(of(1, 2, 3, 4, 5).sliding(3, 2))).isEqualTo(List.of(Vector.of(1, 2, 3), Vector.of(3, 4, 5)));
        assertThat(list(of(1, 2, 3, 4, 5, 6).sliding(3, 2))).isEqualTo(List.of(Vector.of(1, 2, 3), Vector.of(3, 4, 5), Vector.of(5, 6)));
    }

    @Test
    public void shouldGroupBySlidingWithStepEqualToSize() {
        assertThat(list(of(1, 2, 3, 4, 5).sliding(2, 2))).isEqualTo(List.of(Vector.of(1, 2), Vector.of(3, 4), Vector.of(5)));
        assertThat(list(of(1, 2, 3, 4).sliding(2, 2))).isEqualTo(List.of(Vector.of(1, 2), Vector.of(3, 4)));
    }

    @Test
    public void shouldThrowOnNextWhenTheWindowsAreExhausted() {
        final Iterator<Vector<Integer>> windows = of(1, 2).sliding(2);
        windows.next();
        assertThat(windows.hasNext()).isFalse();
        assertThrows(NoSuchElementException.class, windows::next);
    }

    // -- span

    @Test
    public void shouldSpanNil() {
        final Tuple2<Iterator<Integer>, Iterator<Integer>> actual = Iterator.<Integer> empty().span(i -> i < 2);
        assertThatIterator(actual._1()).isSameAs(empty());
        assertThatIterator(actual._2()).isSameAs(empty());
    }

    @Test
    public void shouldSpanNonNil() {
        final Tuple2<Iterator<Integer>, Iterator<Integer>> actual = of(0, 1, 2, 3).span(i -> i < 2);
        assertThat(list(actual._1())).isEqualTo(List.of(0, 1));
        assertThat(list(actual._2())).isEqualTo(List.of(2, 3));
    }

    @Test
    public void shouldSpanAndNotTruncate() {
        final Tuple2<Iterator<Integer>, Iterator<Integer>> odd = of(1, 1, 2, 2, 3, 3).span(x -> x % 2 == 1);
        assertThat(list(odd._1())).isEqualTo(List.of(1, 1));
        assertThat(list(odd._2())).isEqualTo(List.of(2, 2, 3, 3));
        final Tuple2<Iterator<Integer>, Iterator<Integer>> ones = of(1, 1, 2, 2, 4, 4).span(x -> x == 1);
        assertThat(list(ones._1())).isEqualTo(List.of(1, 1));
        assertThat(list(ones._2())).isEqualTo(List.of(2, 2, 4, 4));
    }

    @Test
    public void shouldThrowOnSpanWithNullPredicate() {
        assertThrows(NullPointerException.class, () -> of(1).span(null));
    }

    // -- intersperse

    @Test
    public void shouldIntersperseNil() {
        assertThatIterator(Iterator.<Integer> empty().intersperse(0)).isSameAs(empty());
    }

    @Test
    public void shouldIntersperseOneElementWithNothing() {
        assertThat(list(of(1).intersperse(0))).isEqualTo(List.of(1));
    }

    @Test
    public void shouldIntersperseTheElementBetweenTheOthers() {
        assertThat(list(of(1, 2, 3).intersperse(0))).isEqualTo(List.of(1, 0, 2, 0, 3));
    }

    @Test
    public void shouldIntersperseAnInfiniteSource() {
        assertThat(list(Iterator.from(1).intersperse(0).take(5))).isEqualTo(List.of(1, 0, 2, 0, 3));
    }

    // -- foldLeft, find, headOption, mkString

    @Test
    public void shouldFoldLeftNil() {
        assertThat(Iterator.<String> empty().foldLeft("", (xs, x) -> xs + x)).isEqualTo("");
    }

    @Test
    public void shouldThrowWhenFoldLeftNullOperator() {
        assertThrows(NullPointerException.class, () -> Iterator.<String> empty().foldLeft(null, null));
    }

    @Test
    public void shouldFoldLeftNonNil() {
        assertThat(of("a", "b", "c").foldLeft("!", (xs, x) -> xs + x)).isEqualTo("!abc");
    }

    @Test
    public void shouldConsumeTheElementsOnFoldLeft() {
        final Iterator<Integer> iterator = of(1, 2, 3);
        assertThat(iterator.foldLeft(0, Integer::sum)).isEqualTo(6);
        assertThat(iterator.hasNext()).isFalse();
    }

    @Test
    public void shouldFindFirstOfNil() {
        assertThat(empty().find(ignored -> true)).isEqualTo(Option.none());
    }

    @Test
    public void shouldFindFirstOfNonNil() {
        assertThat(of(1, 2, 3, 4).find(i -> i % 2 == 0)).isEqualTo(Option.some(2));
    }

    @Test
    public void shouldFindTheFirstMatchAndStopThere() {
        final Iterator<Integer> iterator = of(1, 2, 3, 4);
        assertThat(iterator.find(i -> i % 2 == 0)).isEqualTo(Option.some(2));
        assertThat(iterator.next()).isEqualTo(3);
        assertThat(of(1, 2, 3).find(i -> i > 5)).isEqualTo(Option.none());
        assertThrows(NullPointerException.class, () -> of(1).find(null));
    }

    @Test
    public void shouldReturnNoneWhenCallingHeadOptionOnNil() {
        assertThat(empty().headOption()).isEqualTo(Option.none());
    }

    @Test
    public void shouldReturnSomeHeadWhenCallingHeadOptionOnNonNil() {
        assertThat(of(1, 2, 3).headOption()).isEqualTo(Option.some(1));
    }

    @Test
    public void shouldGiveTheHeadAsAnOptionAndConsumeIt() {
        final Iterator<Integer> iterator = of(1, 2);
        assertThat(iterator.headOption()).isEqualTo(Option.some(1));
        assertThat(iterator.headOption()).isEqualTo(Option.some(2));
        assertThat(iterator.headOption()).isEqualTo(Option.none());
    }

    @Test
    public void shouldMkStringWithDelimiterAndPrefixAndSuffixNil() {
        assertThat(empty().mkString("[", ",", "]")).isEqualTo("[]");
    }

    @Test
    public void shouldMkStringWithDelimiterAndPrefixAndSuffixNonNil() {
        assertThat(of('a', 'b', 'c').mkString("[", ",", "]")).isEqualTo("[a,b,c]");
        assertThat(of(1).mkString("", "-", "")).isEqualTo("1");
    }

    // -- toList, toVector, toQueue, toStream

    @Test
    public void shouldConvertToList() {
        assertThat(of(1, 2, 3).toList()).isEqualTo(List.of(1, 2, 3));
        assertThatIterator(empty().toList()).isSameAs(List.empty());
    }

    @Test
    public void shouldConvertToVector() {
        assertThat(of(1, 2, 3).toVector()).isEqualTo(Vector.of(1, 2, 3));
        assertThatIterator(empty().toVector()).isSameAs(Vector.empty());
    }

    @Test
    public void shouldConvertToQueue() {
        assertThat(of(1, 2, 3).toQueue()).isEqualTo(Queue.of(1, 2, 3));
        assertThatIterator(empty().toQueue()).isSameAs(Queue.empty());
    }

    @Test
    public void shouldConvertToStream() {
        assertThat(of(1, 2, 3).toStream()).isEqualTo(Stream.of(1, 2, 3));
        assertThatIterator(empty().toStream()).isSameAs(Stream.empty());
    }

    @Test
    public void shouldConvertToStreamLazily() {
        final AtomicInteger pulled = new AtomicInteger();
        final Stream<Integer> stream = Iterator.continually(pulled::incrementAndGet).toStream();
        assertThat(pulled.get()).isEqualTo(1);
        assertThat(stream.take(3)).isEqualTo(Stream.of(1, 2, 3));
    }

    // -- Iterable

    @Test
    public void shouldBeItsOwnIterator() {
        final Iterator<Integer> iterator = of(1, 2, 3);
        assertThatIterator(iterator.iterator()).isSameAs(iterator);
        int sum = 0;
        for (int i : iterator) {
            sum += i;
        }
        assertThat(sum).isEqualTo(6);
        assertThat(iterator.hasNext()).isFalse();
    }

    @Test
    public void shouldNotSupportRemove() {
        final Iterator<Integer> iterator = of(1, 2, 3);
        iterator.next();
        assertThrows(UnsupportedOperationException.class, iterator::remove);
    }

    // -- hasNext

    @Test
    public void multipleHasNext() {
        multipleHasNext(() -> Iterator.of(1));
        multipleHasNext(() -> Iterator.of(1, 2, 3));
        multipleHasNext(() -> Iterator.ofAll(true, true, false, true));
        multipleHasNext(() -> Iterator.ofAll(new byte[] {1, 2, 3, 4}));
        multipleHasNext(() -> Iterator.ofAll(new char[] {1, 2, 3, 4}));
        multipleHasNext(() -> Iterator.ofAll(new double[] {1, 2, 3, 4}));
        multipleHasNext(() -> Iterator.ofAll(new float[] {1, 2, 3, 4}));
        multipleHasNext(() -> Iterator.ofAll(1, 2, 3, 4));
        multipleHasNext(() -> Iterator.ofAll(new long[] {1, 2, 3, 4}));
        multipleHasNext(() -> Iterator.ofAll(new short[] {1, 2, 3, 4}));
        multipleHasNext(() -> Iterator.ofAll(java.util.List.of(1, 2, 3).iterator()));
        multipleHasNext(() -> Iterator.ofAll(java.util.List.of(1, 2, 3)));

        multipleHasNext(() -> Iterator.concat(List.of(Iterator.empty(), Iterator.of(1, 2, 3))));
        multipleHasNext(() -> Iterator.concat(List.of(Iterator.of(1, 2, 3), Iterator.of(1, 2, 3))));
        multipleHasNext(() -> Iterator.concat(Iterator.of(1, 2, 3), Iterator.of(1, 2, 3)));
        multipleHasNext(() -> Iterator.continually(() -> 1), 5);
        multipleHasNext(() -> Iterator.continually(1), 5);
        multipleHasNext(() -> Iterator.fill(3, () -> 1));
        multipleHasNext(() -> Iterator.from(1), 5);
        multipleHasNext(() -> Iterator.from(1, 2), 5);
        multipleHasNext(() -> Iterator.from(1L), 5);
        multipleHasNext(() -> Iterator.from(1L, 2L), 5);
        multipleHasNext(() -> Iterator.iterate(1, i -> i + 1), 5);
        multipleHasNext(() -> Iterator.iterate(new OptionSupplier(1)), 5);
        multipleHasNext(() -> Iterator.tabulate(10, i -> i + 1));
        multipleHasNext(() -> Iterator.unfold(10, x -> x == 0 ? Option.none() : Option.some(new Tuple2<>(x - 1, x))));
        multipleHasNext(() -> Iterator.unfoldLeft(10, x -> x == 0 ? Option.none() : Option.some(new Tuple2<>(x - 1, x))));
        multipleHasNext(() -> Iterator.unfoldRight(10, x -> x == 0 ? Option.none() : Option.some(new Tuple2<>(x, x - 1))));

        multipleHasNext(() -> Iterator.range('a', 'd'));
        multipleHasNext(() -> Iterator.range(1, 4));
        multipleHasNext(() -> Iterator.range(1L, 4L));
        multipleHasNext(() -> Iterator.rangeClosed('a', 'd'));
        multipleHasNext(() -> Iterator.rangeClosed(1, 4));
        multipleHasNext(() -> Iterator.rangeClosed(1L, 4L));
        multipleHasNext(() -> Iterator.rangeBy('a', 'd', 1));
        multipleHasNext(() -> Iterator.rangeBy(1, 4, 1));
        multipleHasNext(() -> Iterator.rangeBy(1d, 4d, 1));
        multipleHasNext(() -> Iterator.rangeBy(1L, 4L, 1));
        multipleHasNext(() -> Iterator.rangeClosedBy('a', 'd', 1));
        multipleHasNext(() -> Iterator.rangeClosedBy(1, 4, 1));
        multipleHasNext(() -> Iterator.rangeClosedBy(1d, 4d, 1));
        multipleHasNext(() -> Iterator.rangeClosedBy(1L, 4L, 1));

        multipleHasNext(() -> Iterator.of(1, 2, 3).concat(Iterator.of(1, 2, 3)));
        multipleHasNext(() -> Iterator.of(1, 2, 1, 2, 1, 2).distinctBy(e -> e % 2));
        multipleHasNext(() -> Iterator.of(1, 2, 1, 2, 1, 2).distinctBy(Comparator.comparingInt(e -> e % 2)));
        multipleHasNext(() -> Iterator.of(1, 2, 1, 2, 1, 2).distinctByKeepLast(e -> e % 2));
        multipleHasNext(() -> Iterator.of(1, 2, 1, 2, 1, 2).distinctByKeepLast(Comparator.comparingInt(e -> e % 2)));
        multipleHasNext(() -> Iterator.of(1, 2, 3, 4).drop(1));
        multipleHasNext(() -> Iterator.of(1, 2, 3, 4).dropRight(1));
        multipleHasNext(() -> Iterator.of(1, 2, 3, 4).dropWhile(e -> e == 1));
        multipleHasNext(() -> Iterator.of(1, 2, 3, 4).filter(e -> e > 1));
        multipleHasNext(() -> Iterator.of(1, 2, 3, 4).flatMap(e -> Iterator.of(e, e + 1)));
        multipleHasNext(() -> Iterator.of(1, 2, 3, 4).collect(e -> e > 1 ? Option.some(e) : Option.none()));
        multipleHasNext(() -> Iterator.of(1, 2, 3, 4).intersperse(-1));
        multipleHasNext(() -> Iterator.of(1, 2, 3).map(i -> i * 2));
        multipleHasNext(() -> Iterator.of(1, 2, 3).scanLeft(1, (a, b) -> a + b));
        multipleHasNext(() -> Iterator.of(1, 2, 3).slideBy(Function.identity()));
        multipleHasNext(() -> Iterator.of(1, 2, 3, 4).sliding(2));
        multipleHasNext(() -> Iterator.of(1, 2, 3, 4).sliding(2, 1));
        multipleHasNext(() -> Iterator.of(1, 2, 3, 4).span(i -> i < 3)._1());
        multipleHasNext(() -> Iterator.of(1, 2, 3, 4).span(i -> i < 3)._2());
        multipleHasNext(() -> Iterator.of(1, 2, 3, 4).take(3));
        multipleHasNext(() -> Iterator.of(1, 2, 3, 4).takeRight(3));
        multipleHasNext(() -> Iterator.of(1, 2, 3, 4).takeWhile(i -> i < 4));
        multipleHasNext(() -> Iterator.of(1, 2, 3, 4).zip(Iterator.from(1)));
        multipleHasNext(() -> Iterator.of(1, 2, 3, 4).zipAll(Iterator.of(1, 2), -1, -2));
        multipleHasNext(() -> Iterator.of(1, 2, 3, 4).zipWith(Iterator.of(1, 2), (a, b) -> a + b));
        multipleHasNext(() -> Iterator.of(1, 2, 3, 4).zipWithIndex());
        multipleHasNext(() -> Iterator.of(1, 2, 3, 4).zipWithIndex((a, i) -> a + i));
    }

    private <T> void multipleHasNext(Supplier<Iterator<T>> it) {
        multipleHasNext(it, -1);
    }

    private <T> void multipleHasNext(Supplier<Iterator<T>> it, int maxLen) {
        final Iterator<T> testee1 = it.get();
        final Iterator<T> testee2 = it.get();
        // ask 2 times
        assertThat(testee2.hasNext()).isTrue();
        assertThat(testee2.hasNext()).isTrue();
        // results should be still the same
        if (maxLen >= 0) {
            assertThat(testee1.take(maxLen).toList()).isEqualTo(testee2.take(maxLen).toList());
        } else {
            assertThat(testee1.toList()).isEqualTo(testee2.toList());
        }
    }

    // -- next

    @Nested
    class NextTests {
        @Test
        public void shouldThrowOnNextWhenEmpty() {
            assertThatThrownBy(Iterator.empty()::next).isInstanceOf(NoSuchElementException.class);
        }

        @Test
        public void shouldReturnValueOnNextWhenIteratorOfOneElementAndNextWasNotCalled() {
            final Iterator<Object> iterator = Iterator.of(1);
            assertThatIterator(iterator.next()).isSameAs(1);
        }

        @Test
        public void shouldThrowOnNextWhenIteratorOfOneElementAndNextWasCalled() {
            final Iterator<Object> iterator = Iterator.of(1);
            iterator.next();
            assertThatThrownBy(iterator::next).isInstanceOf(NoSuchElementException.class);
        }

        @Test
        public void shouldThrowOnNextWhenACombinatorIsExhausted() {
            final Iterator<Integer> mapped = Iterator.of(1).map(i -> i);
            mapped.next();
            assertThatThrownBy(mapped::next).isInstanceOf(NoSuchElementException.class).hasMessage("next() on empty iterator");
        }
    }

    // -- class initialization

    @Test
    public void shouldNotDeadlockOnConcurrentClassInitialization() {
        assertTimeoutPreemptively(Duration.ofSeconds(5), () -> {
            final ExecutorService executorService = Executors.newFixedThreadPool(2);
            executorService.execute(new ClassInitializer("com.guizmaii.zazr.collection.internal.Iterator"));
            executorService.execute(new ClassInitializer("com.guizmaii.zazr.collection.internal.AbstractIterator"));
            executorService.shutdown();
            // a cycle between the two class initialisations would hang here
            Iterator.empty().iterator();
        });

    }

    static class ClassInitializer implements Runnable {

        private String type;

        ClassInitializer(String type) {
            this.type = type;
        }

        @Override
        public void run() {
            try {
                Class.forName(type);
            } catch (ClassNotFoundException e) {
                throw new Error(e);
            }
        }
    }

    // -- toString

    @Test
    public void shouldConformEmptyStringRepresentation() {
        assertThat(empty().toString()).isEqualTo("EmptyIterator()");
    }

    @Test
    public void shouldConformNonEmptyStringRepresentation() {
        // an Iterator is single-pass: toString never consumes it
        assertThat(of("a", "b", "c").toString()).isEqualTo("Iterator(?)");
    }

    @Test
    public void shouldPrintAnExhaustedIteratorAsEmpty() {
        final Iterator<String> iterator = of("a");
        iterator.next();
        assertThat(iterator.toString()).isEqualTo("Iterator()");
    }

    // -- null elements are rejected (design 3.9)

    @Test
    public void shouldRejectNullElementOnConstruction() {
        assertThrows(NullPointerException.class, () -> of((Integer) null));
    }

    @Test
    public void shouldRejectNullElementOnOf() {
        assertThatNullPointerException().isThrownBy(() -> Iterator.of((Integer) null));
    }

    @Test
    public void shouldRejectNullElementOnOfVarargs() {
        assertThatNullPointerException().isThrownBy(() -> Iterator.of(1, null));
    }

    @Test
    public void shouldRejectNullElementOnTabulate() {
        assertThatNullPointerException().isThrownBy(() -> Iterator.tabulate(1, i -> null).toList());
    }

    @Test
    public void shouldRejectNullElementOnFillSupplier() {
        assertThatNullPointerException().isThrownBy(() -> Iterator.<Integer> fill(1, () -> null).toList());
    }

    @Test
    public void shouldRejectNullElementOnFillObject() {
        assertThatNullPointerException().isThrownBy(() -> Iterator.fill(1, (Integer) null));
    }

    @Test
    public void shouldRejectNullResultOnMap() {
        assertThatNullPointerException().isThrownBy(() -> Iterator.of(1).map(i -> null).toList());
    }

    @Test
    public void shouldRejectNullResultOnFlatMap() {
        assertThatNullPointerException().isThrownBy(() -> Iterator.of(1).flatMap(i -> Iterator.of((Integer) null)).toList());
    }

    @Test
    public void shouldRejectNullResultOnContinuallySupplier() {
        assertThatNullPointerException().isThrownBy(() -> Iterator.continually(() -> null).next());
    }

    @Test
    public void shouldRejectNullElementOnContinually() {
        assertThatNullPointerException().isThrownBy(() -> Iterator.continually((Integer) null));
    }

    @Test
    public void shouldRejectNullSeedOnIterate() {
        assertThatNullPointerException().isThrownBy(() -> Iterator.iterate((Integer) null, i -> i));
    }

    @Test
    public void shouldRejectNullResultOnIterate() {
        assertThatNullPointerException().isThrownBy(() -> Iterator.iterate(1, i -> null).drop(1).next());
    }

    @Test
    public void shouldRejectNullElementOnIntersperse() {
        assertThatNullPointerException().isThrownBy(() -> Iterator.of(1, 2).intersperse(null));
    }

    @Test
    public void shouldRejectNullResultOnZipWith() {
        assertThatNullPointerException().isThrownBy(() -> Iterator.of(1).zipWith(Iterator.of(2), (a, b) -> null).toList());
    }

    @Test
    public void shouldRejectNullFillValuesOnZipAll() {
        assertThatNullPointerException().isThrownBy(() -> Iterator.of(1).zipAll(Iterator.of(2), null, 9));
        assertThatNullPointerException().isThrownBy(() -> Iterator.of(1).zipAll(Iterator.of(2), 9, null));
    }

    @Test
    public void shouldRejectNullResultOnZipWithIndex() {
        assertThatNullPointerException().isThrownBy(() -> Iterator.of(1).zipWithIndex((t, i) -> null).toList());
    }

    @Test
    public void shouldRejectNullResultOnUnfold() {
        assertThatNullPointerException().isThrownBy(() -> Iterator.unfold(1, i -> Option.some(Tuple.of(i, null))).toList());
    }

    @Test
    public void shouldRejectNullZeroOnScanLeft() {
        assertThatNullPointerException().isThrownBy(() -> Iterator.of(1).scanLeft(null, (acc, t) -> acc));
    }

    @Test
    public void shouldRejectNullResultOnScanLeft() {
        assertThatNullPointerException().isThrownBy(() -> Iterator.of(1, 2).scanLeft(0, (acc, t) -> null).toList());
    }

    @Test
    public void shouldRejectNullElementOfAWrappedJavaIterator() {
        final java.util.List<Integer> withNull = new java.util.ArrayList<>();
        withNull.add(null);
        assertThatNullPointerException().isThrownBy(() -> Iterator.ofAll(withNull.iterator()).next())
                .withMessage("Iterator: element is null");
    }

    // -- every Iterator funnels through AbstractIterator.next(), the single place that rejects a
    // null element produced by a user function; these pin the exact message at that one boundary.

    @Test
    public void shouldRejectNullFromMapAtTheFunnel() {
        assertThatNullPointerException().isThrownBy(() -> Iterator.of(1).map(x -> null).next())
                .withMessage("Iterator: element is null");
    }

    @Test
    public void shouldRejectNullFromFlatMapAtTheFunnel() {
        assertThatNullPointerException().isThrownBy(() -> Iterator.of(1).flatMap(x -> Iterator.of(2).map(i -> null)).next())
                .withMessage("Iterator: element is null");
    }

    @Test
    public void shouldRejectNullFromZipWithAtTheFunnel() {
        assertThatNullPointerException().isThrownBy(() -> Iterator.of(1).zipWith(Iterator.of(2), (a, b) -> null).next())
                .withMessage("Iterator: element is null");
    }

    @Test
    public void shouldRejectNullFromTabulateAtTheFunnel() {
        assertThatNullPointerException().isThrownBy(() -> Iterator.tabulate(1, i -> null).next())
                .withMessage("Iterator: element is null");
    }

    @Test
    public void shouldRejectNullFromFillSupplierAtTheFunnel() {
        assertThatNullPointerException().isThrownBy(() -> Iterator.<Integer> fill(1, () -> null).next())
                .withMessage("Iterator: element is null");
    }

    @Test
    public void shouldRejectNullFromUnfoldAtTheFunnel() {
        assertThatNullPointerException().isThrownBy(() -> Iterator.unfold(1, i -> Option.some(Tuple.of(i, (Integer) null))).next())
                .withMessage("Iterator: element is null");
    }

    // -- helpers

    /**
     * Wraps a String in order to ensure that it is not Comparable.
     */
    static final class NonComparable {

        final String value;

        NonComparable(String value) {
            this.value = value;
        }

        @Override
        public int hashCode() {
            return Objects.hash(value);
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            } else if (obj instanceof NonComparable) {
                final NonComparable that = (NonComparable) obj;
                return Objects.equals(this.value, that.value);
            } else {
                return false;
            }
        }

        @Override
        public String toString() {
            return value;
        }
    }
}
