package com.guizmaii.zazr.collection;

import com.guizmaii.zazr.Tuple;
import com.guizmaii.zazr.Tuple2;
import com.guizmaii.zazr.Tuple3;
import com.guizmaii.zazr.collection.JavaConverters.ChangePolicy;
import com.guizmaii.zazr.collection.JavaConverters.ListView;
import com.guizmaii.zazr.control.Either;
import com.guizmaii.zazr.control.Option;
import java.math.BigDecimal;
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

/**
 * Tests Vector's whole API: the Traversable cases (inherited), the sequence cases that were shared through
 * {@code AbstractSeqTest} while Vector shared the sequence interface (folded in with #66), and the Vector-specific
 * ones.
 */
public class VectorTest extends AbstractTraversableRangeTest {

    @Override
    protected <T> Collector<T, Vector.Builder<T>, Vector<T>> collector() {
        return Vector.collector();
    }

    @Override
    protected <T> Vector<T> empty() {
        return Vector.empty();
    }

    @Override
    protected <T> Vector<T> of(T element) {
        return Vector.of(element);
    }

    @SuppressWarnings("varargs")
    @SafeVarargs
    @Override
    protected final <T> Vector<T> of(T... elements) {
        return Vector.of(elements);
    }

    @Override
    protected <T> Vector<T> ofAll(Iterable<? extends T> elements) {
        return Vector.ofAll(elements);
    }

    @Override
    protected <T extends Comparable<? super T>> Vector<T> ofJavaStream(java.util.stream.Stream<? extends T> javaStream) {
        return Vector.ofAll(javaStream);
    }

    @Override
    protected Vector<Boolean> ofAll(boolean... elements) {
        return Vector.ofAll(elements);
    }

    @Override
    protected Vector<Byte> ofAll(byte... elements) {
        return Vector.ofAll(elements);
    }

    @Override
    protected Vector<Character> ofAll(char... elements) {
        return Vector.ofAll(elements);
    }

    @Override
    protected Vector<Double> ofAll(double... elements) {
        return Vector.ofAll(elements);
    }

    @Override
    protected Vector<Float> ofAll(float... elements) {
        return Vector.ofAll(elements);
    }

    @Override
    protected Vector<Integer> ofAll(int... elements) {
        return Vector.ofAll(elements);
    }

    @Override
    protected Vector<Long> ofAll(long... elements) {
        return Vector.ofAll(elements);
    }

    @Override
    protected Vector<Short> ofAll(short... elements) {
        return Vector.ofAll(elements);
    }

    @Override
    protected <T> Vector<T> tabulate(int n, Function<? super Integer, ? extends T> f) {
        return Vector.tabulate(n, f);
    }

    @Override
    protected <T> Vector<T> fill(int n, Supplier<? extends T> s) {
        return Vector.fill(n, s);
    }

    protected <T> Vector<T> fill(int n, T element) {
        return Vector.fill(n, element);
    }

    @Override
    protected Vector<Character> range(char from, char toExclusive) {
        return Vector.range(from, toExclusive);
    }

    @Override
    protected Vector<Character> rangeBy(char from, char toExclusive, int step) {
        return Vector.rangeBy(from, toExclusive, step);
    }

    @Override
    protected Vector<Double> rangeBy(double from, double toExclusive, double step) {
        return Vector.rangeBy(from, toExclusive, step);
    }

    //fixme: delete, when useIsEqualToInsteadOfIsSameAs() will be eliminated from AbstractValueTest class
    @Override
    protected boolean useIsEqualToInsteadOfIsSameAs() {
        return false;
    }

    @Override
    protected Vector<Integer> range(int from, int toExclusive) {
        return Vector.range(from, toExclusive);
    }

    @Override
    protected Vector<Integer> rangeBy(int from, int toExclusive, int step) {
        return Vector.rangeBy(from, toExclusive, step);
    }

    @Override
    protected Vector<Long> range(long from, long toExclusive) {
        return Vector.range(from, toExclusive);
    }

    @Override
    protected Vector<Long> rangeBy(long from, long toExclusive, long step) {
        return Vector.rangeBy(from, toExclusive, step);
    }

    @Override
    protected Vector<Character> rangeClosed(char from, char toInclusive) {
        return Vector.rangeClosed(from, toInclusive);
    }

    @Override
    protected Vector<Character> rangeClosedBy(char from, char toInclusive, int step) {
        return Vector.rangeClosedBy(from, toInclusive, step);
    }

    @Override
    protected Vector<Double> rangeClosedBy(double from, double toInclusive, double step) {
        return Vector.rangeClosedBy(from, toInclusive, step);
    }

    @Override
    protected Vector<Integer> rangeClosed(int from, int toInclusive) {
        return Vector.rangeClosed(from, toInclusive);
    }

    @Override
    protected Vector<Integer> rangeClosedBy(int from, int toInclusive, int step) {
        return Vector.rangeClosedBy(from, toInclusive, step);
    }

    @Override
    protected Vector<Long> rangeClosed(long from, long toInclusive) {
        return Vector.rangeClosed(from, toInclusive);
    }

    @Override
    protected Vector<Long> rangeClosedBy(long from, long toInclusive, long step) {
        return Vector.rangeClosedBy(from, toInclusive, step);
    }

    protected <T> Vector<Vector<T>> transpose(Vector<Vector<T>> rows) {
        return Vector.transpose(rows);
    }

    @Nested
    class VectorStaticNarrowTests {
        @Test
        public void shouldNarrowVector() {
            final Vector<Double> doubles = of(1.0d);
            final Vector<Number> numbers = Vector.narrow(doubles);
            final int actual = numbers.append(new BigDecimal("2.0")).sum().intValue();
            assertThat(actual).isEqualTo(3);
        }
    }

    @Nested
    class StaticOfallTests {
        @Test
        public void shouldReturnSelfWhenIterableIsInstanceOfVector() {
            final Vector<Integer> source = ofAll(1, 2, 3);
            final Vector<Integer> target = Vector.ofAll(source);
            assertThat(target).isSameAs(source);
        }

        @Test
        public void shouldReturnSelfWhenIterableIsInstanceOfListView() {
            final ListView<Integer, Vector<Integer>> source = JavaConverters
                    .asJava(ofAll(1, 2, 3), ChangePolicy.IMMUTABLE);
            final Vector<Integer> target = Vector.ofAll(source);
            assertThat(target).isSameAs(source.getDelegate());
        }
    }

    @Nested
    class PartitionTests {
        @Test
        public void shouldPartitionInOneIteration() {
            final AtomicInteger count = new AtomicInteger(0);
            final Vector<Integer> values = ofAll(1, 2, 3);
            final Tuple2<Vector<Integer>, Vector<Integer>> results = values.partition(v -> {
                count.incrementAndGet();
                return true;
            });
            assertThat(results._1()).isEqualTo(ofAll(1, 2, 3));
            assertThat(results._2()).isEmpty();
            assertThat(count.get()).isEqualTo(3);
        }
    }

    @Nested
    class PrimitivesTests {
        @Test
        public void shouldRejectNullOnPrimitiveVector() {
            final Vector<Integer> primitives = rangeClosed(0, 2);

            assertThatNullPointerException().isThrownBy(() -> primitives.append(null));
            assertThatNullPointerException().isThrownBy(() -> primitives.prepend(null));
            assertThatNullPointerException().isThrownBy(() -> primitives.update(1, (Integer) null));
        }

        @Test
        public void shouldAddObjectToPrimitiveVector() {
            final String object = "String";
            final Vector<Object> primitives = Vector.narrow(rangeClosed(0, 2));

            assertThat(primitives.append(object)).isEqualTo(of(0, 1, 2, object));
            assertThat(primitives.prepend(object)).isEqualTo(of(object, 0, 1, 2));
            assertThat(primitives.update(1, object)).isEqualTo(of(0, object, 2));
        }

        @Test
        public void shouldThrowForVoidType() {
            assertThrows(IllegalArgumentException.class, () -> ArrayType.of(void.class));
        }
    }

    @Nested
    class UnfoldTests {
        @Test
        public void shouldUnfoldRightToEmpty() {
            assertThat(Vector.unfoldRight(0, x -> Option.none())).isEqualTo(empty());
        }

        @Test
        public void shouldUnfoldRightSimpleVector() {
            assertThat(
                    Vector.unfoldRight(10, x -> x == 0
                                                ? Option.none()
                                                : Option.some(new Tuple2<>(x, x - 1))))
                    .isEqualTo(of(10, 9, 8, 7, 6, 5, 4, 3, 2, 1));
        }

        @Test
        public void shouldUnfoldLeftToEmpty() {
            assertThat(Vector.unfoldLeft(0, x -> Option.none())).isEqualTo(empty());
        }

        @Test
        public void shouldUnfoldLeftSimpleVector() {
            assertThat(
                    Vector.unfoldLeft(10, x -> x == 0
                                               ? Option.none()
                                               : Option.some(new Tuple2<>(x - 1, x))))
                    .isEqualTo(of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10));
        }

        @Test
        public void shouldUnfoldToEmpty() {
            assertThat(Vector.unfold(0, x -> Option.none())).isEqualTo(empty());
        }

        @Test
        public void shouldUnfoldSimpleVector() {
            assertThat(
                    Vector.unfold(10, x -> x == 0
                                           ? Option.none()
                                           : Option.some(new Tuple2<>(x - 1, x))))
                    .isEqualTo(of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10));
        }
    }

    // -- dropRightWhile

    @Test
    public void shouldDropRightWhileCorrect() {
        assertThat(ofAll("abc  ".toCharArray()).dropRightWhile(Character::isWhitespace)).isEqualTo(ofAll("abc".toCharArray()));
    }

    @Nested
    class TostringTests {
        @Test
        public void shouldStringifyNil() {
            assertThat(empty().toString()).isEqualTo("Vector()");
        }

        @Test
        public void shouldStringifyNonNil() {
            assertThat(of(1, 2, 3).toString()).isEqualTo("Vector(1, 2, 3)");
        }

        @Test
        public void shouldRejectNullElementOnOf() {
            assertThatNullPointerException().isThrownBy(() -> of(null, 1, 2, 3));
        }
    }

    @Nested
    class TovectorTests {
        @Test
        public void shouldReturnSelfOnConvertToVector() {
            final Traversable<Integer> value = of(1, 2, 3);
            assertThat(value.toVector()).isSameAs(value);
        }
    }

    @Nested
    class IndexOfNegativeFromTests {
        @Test
        public void shouldClampNegativeFromToZeroWhenSearchingIndexOf() {
            assertThat(of(1, 2, 3).indexOf(1, -1)).isEqualTo(0);
            assertThat(of(1, 2, 3).indexOf(3, -5)).isEqualTo(2);
        }

        @Test
        public void shouldReturnMinusOneWhenElementNotFoundFromNegativeIndex() {
            assertThat(of(1, 2, 3).indexOf(4, -1)).isEqualTo(-1);
            assertThat(empty().indexOf(1, -1)).isEqualTo(-1);
        }
    }

    @Nested
    class CollectTests {

        // the 32-wide trie: empty, one leaf, a full leaf, one past it (a second level), and around the third level
        private final int[] sizes = { 0, 1, 31, 32, 33, 1023, 1024, 1025 };

        @Test
        public void shouldCollectNothingWhenEveryElementIsDroppedAtTheLeafBoundaries() {
            for (int n : sizes) {
                assertThat(Vector.range(0, n).collect(i -> Option.none())).isEqualTo(Vector.empty());
            }
        }

        @Test
        public void shouldCollectEveryElementWhenEveryElementIsKeptAtTheLeafBoundaries() {
            for (int n : sizes) {
                final Vector<Integer> actual = Vector.range(0, n).collect(i -> Option.some(i + 1));
                assertThat(actual).isEqualTo(Vector.range(1, n + 1));
                assertThat(actual.length()).isEqualTo(n);
            }
        }

        @Test
        public void shouldCollectTheKeptElementsAtTheLeafBoundaries() {
            for (int n : sizes) {
                final Vector<Integer> actual = Vector.range(0, n).collect(i -> i % 2 == 0 ? Option.some(i) : Option.none());
                assertThat(actual).isEqualTo(Vector.range(0, n).filter(i -> i % 2 == 0));
                assertThat(actual.length()).isEqualTo((n + 1) / 2);
            }
        }

        @Test
        public void shouldCollectTheSameFromObjectLeavesAndPrimitiveLeaves() {
            final Function<Integer, Option<Integer>> mapper = i -> i % 3 == 0 ? Option.some(i * 2) : Option.none();
            for (int n : sizes) {
                final Vector<Integer> primitive = Vector.range(0, n); // int[] leaves
                final Vector<Integer> boxed = Vector.ofAll(primitive.toJavaList()); // Object[] leaves
                assertThat(boxed.collect(mapper)).isEqualTo(primitive.collect(mapper));
            }
        }

        @Test
        public void shouldCollectAcrossTheLeafBoundaryInOrder() {
            assertThat(Vector.range(0, 33).collect(i -> i >= 31 ? Option.some(i) : Option.none())).isEqualTo(Vector.of(31, 32));
        }

        @Test
        public void shouldRejectANullOptionAtEveryPosition() {
            for (int n : new int[] { 1, 32, 33 }) {
                final int last = n - 1;
                final NullPointerException e = assertThrows(NullPointerException.class,
                  () -> Vector.range(0, n).collect(i -> i == last ? null : Option.some(i)));
                assertThat(e.getMessage()).isEqualTo("Vector.collect: mapper returned null");
            }
        }
    }

    // partitionMap, duplicates, duplicatesBy, flatten and toNonEmptyVector, at the empty/1/32/33/1023/1024/1025 boundaries and on both leaf representations

    static java.util.List<Vector<Integer>> bothRepresentations(int n) {
        final Vector<Integer> primitive = Vector.range(0, n);
        return java.util.List.of(primitive, Vector.ofAll(primitive.toJavaList()));
    }

    @Nested
    class PartitionMapTests {

        @Test
        public void shouldPartitionMapLikePartitionAtEveryBoundary() {
            for (int n : new int[] { 0, 1, 32, 33, 1023, 1024, 1025 }) {
                for (Vector<Integer> vector : bothRepresentations(n)) {
                    final Tuple2<Vector<String>, Vector<Integer>> actual = vector.partitionMap(i -> i % 2 == 0 ? Either.left("e" + i) : Either.right(i));
                    final Tuple2<Vector<Integer>, Vector<Integer>> expected = vector.partition(i -> i % 2 == 0);
                    assertThat(actual._1()).isEqualTo(expected._1().map(i -> "e" + i));
                    assertThat(actual._2()).isEqualTo(expected._2());
                    assertThat(actual._1().size() + actual._2().size()).isEqualTo(n);
                }
            }
        }

        @Test
        public void shouldLeaveOneSideEmpty() {
            for (int n : new int[] { 0, 1, 32, 33, 1023, 1024, 1025 }) {
                for (Vector<Integer> vector : bothRepresentations(n)) {
                    assertThat(vector.partitionMap(i -> Either.<Integer, String> left(i))).isEqualTo(Tuple.of(vector, Vector.empty()));
                    assertThat(vector.partitionMap(i -> Either.<String, Integer> right(i))).isEqualTo(Tuple.of(Vector.empty(), vector));
                }
            }
            assertThat(Vector.<Integer> empty().partitionMap(i -> Either.<Integer, Integer> left(i))._1()).isSameAs(Vector.empty());
        }

        @Test
        public void shouldRejectNullFunctionAndNullEither() {
            assertThatNullPointerException().isThrownBy(() -> Vector.of(1).partitionMap(null)).withMessage("f is null");
            for (int n : new int[] { 1, 32, 33 }) {
                final int last = n - 1;
                final NullPointerException e = assertThrows(NullPointerException.class,
                  () -> Vector.range(0, n).partitionMap(i -> i == last ? null : Either.<Integer, Integer> left(i)));
                assertThat(e.getMessage()).isEqualTo("Vector.partitionMap: f returned null");
            }
        }
    }

    @Nested
    class DuplicatesTests {

        @Test
        public void shouldReturnDuplicatesInOrderOfFirstOccurrence() {
            assertThat(Vector.of(3, 1, 3, 2, 1, 3).duplicates()).isEqualTo(Vector.of(3, 1));
            assertThat(Vector.of(1, 2, 2, 1).duplicates()).isEqualTo(Vector.of(1, 2));
            assertThat(Vector.of("a", "b", "c").duplicates()).isEqualTo(Vector.empty());
            assertThat(Vector.of("a", "b", "c").duplicates()).isSameAs(Vector.empty());
            assertThat(Vector.<Integer> empty().duplicates()).isSameAs(Vector.empty());
        }

        @Test
        public void shouldReturnTheFirstElementOfEachDuplicatedKey() {
            assertThat(Vector.of("aa", "b", "cc", "dd", "e").duplicatesBy(String::length)).isEqualTo(Vector.of("aa", "b"));
            assertThat(Vector.of("aa", "b", "cc", "dd", "eee").duplicatesBy(String::length)).isEqualTo(Vector.of("aa"));
            assertThat(Vector.of("b", "aa", "e", "cc").duplicatesBy(String::length)).isEqualTo(Vector.of("b", "aa"));
            assertThat(Vector.of("a", "bb").duplicatesBy(String::length)).isEqualTo(Vector.empty());
            assertThatNullPointerException().isThrownBy(() -> Vector.of(1).duplicatesBy(null)).withMessage("keyExtractor is null");
        }

        @Test
        public void shouldFindDuplicatesOfANullKey() {
            // a Vector never holds a null element, but a key extractor may return null for several of them
            assertThat(Vector.of("a", "b").duplicatesBy(s -> null)).isEqualTo(Vector.of("a"));
            assertThat(Vector.of("a", "bb", "c").duplicatesBy(s -> s.length() == 1 ? null : s)).isEqualTo(Vector.of("a"));
            assertThat(Vector.of("a").duplicatesBy(s -> null)).isEqualTo(Vector.empty());
        }

        @Test
        public void shouldFindDuplicatesAtEveryBoundary() {
            for (int n : new int[] { 0, 1, 32, 33, 1023, 1024, 1025 }) {
                for (Vector<Integer> vector : bothRepresentations(n)) {
                    assertThat(vector.duplicates()).isEqualTo(Vector.empty());
                    assertThat(vector.appendAll(vector).duplicates()).isEqualTo(vector);
                    assertThat(vector.appendAll(vector.reverse()).duplicates()).isEqualTo(vector);
                    assertThat(vector.duplicatesBy(i -> i % 5)).isEqualTo(vector.take(Math.max(n - 5, 0)).take(5));
                    assertThat(vector.duplicatesBy(i -> i % 5).isEmpty()).isEqualTo(n <= 5);
                }
            }
        }
    }

    @Nested
    class FlattenTests {

        @Test
        public void shouldFlattenNestedIterablesAtEveryBoundary() {
            for (int n : new int[] { 0, 1, 32, 33, 1023, 1024, 1025 }) {
                for (Vector<Integer> inner : bothRepresentations(n)) {
                    assertThat(Vector.flatten(Vector.of(inner, inner))).isEqualTo(inner.appendAll(inner));
                    assertThat(Vector.flatten(java.util.List.of(inner.toJavaList(), inner))).isEqualTo(inner.appendAll(inner));
                    assertThat(Vector.flatten(Vector.of(Vector.<Integer> empty(), inner, Vector.<Integer> empty()))).isEqualTo(inner);
                    assertThat(Vector.flatten(Vector.of(inner))).isEqualTo(inner);
                }
            }
            assertThat(Vector.flatten(Vector.<Vector<Integer>> empty())).isSameAs(Vector.empty());
            assertThat(Vector.flatten(Vector.of(Vector.<Integer> empty()))).isSameAs(Vector.empty());
            final Vector<Number> numbers = Vector.flatten(Vector.of(Vector.of(1), Vector.of(2.0)));
            assertThat(numbers).isEqualTo(Vector.<Number> of(1, 2.0));
        }

        @Test
        public void shouldRejectNulls() {
            assertThatNullPointerException().isThrownBy(() -> Vector.flatten(null)).withMessage("nested is null");
            assertThatNullPointerException().isThrownBy(() -> Vector.flatten(java.util.Arrays.asList(Vector.of(1), null)));
            assertThatNullPointerException().isThrownBy(() -> Vector.flatten(Vector.of(java.util.Arrays.asList(1, null))));
        }
    }

    @Nested
    class GroupedHugeSizeTests {
        @Test
        public void shouldGroupIntoOneGroupWhenSizeExceedsTheVector() {
            for (int n : new int[] { 1, 32, 33, 1025 }) {
                for (Vector<Integer> vector : bothRepresentations(n)) {
                    assertThat(vector.grouped(Integer.MAX_VALUE).toList()).isEqualTo(List.of(vector));
                    assertThat(vector.sliding(Integer.MAX_VALUE).toList()).isEqualTo(List.of(vector));
                    assertThat(vector.sliding(2, Integer.MAX_VALUE).toList()).isEqualTo(List.of(vector.take(2)));
                    assertThat(vector.sliding(Integer.MAX_VALUE, 1).toList()).isEqualTo(List.of(vector));
                    assertThat(vector.grouped(n + 1).toList()).isEqualTo(List.of(vector));
                }
            }
            assertThat(Vector.empty().grouped(Integer.MAX_VALUE).isEmpty()).isTrue();
            // the first group grows past the initial capacity in every step configuration
            assertThat(Vector.range(0, 100).grouped(40).toList()).isEqualTo(List.of(Vector.range(0, 40), Vector.range(40, 80), Vector.range(80, 100)));
            assertThat(Vector.range(0, 100).sliding(40, 30).toList()).isEqualTo(List.of(Vector.range(0, 40), Vector.range(30, 70), Vector.range(60, 100)));
            assertThat(Vector.range(0, 101).sliding(40, 30).toList()).isEqualTo(List.of(Vector.range(0, 40), Vector.range(30, 70), Vector.range(60, 100), Vector.range(90, 101)));
            assertThat(Vector.range(0, 100).sliding(40, 50).toList()).isEqualTo(List.of(Vector.range(0, 40), Vector.range(50, 90)));
        }
    }

    @Nested
    class ToNonEmptyVectorTests {

        @Test
        public void shouldNarrowToNonEmptyVector() {
            assertThat(Vector.<Integer> empty().toNonEmptyVector()).isEqualTo(Option.none());
            for (int n : new int[] { 1, 32, 33, 1023, 1024, 1025 }) {
                for (Vector<Integer> vector : bothRepresentations(n)) {
                    final Option<NonEmptyVector<Integer>> actual = vector.toNonEmptyVector();
                    assertThat(actual.isDefined()).isTrue();
                    assertThat(actual.get().toVector()).isSameAs(vector);
                    assertThat(actual.get().size()).isEqualTo(n);
                }
            }
        }
    }

    // -- the sequence cases

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
        final Vector<Integer> actual = this.<Integer> empty().append(1);
        final Vector<Integer> expected = of(1);
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void shouldRejectAppendOfNullElement() {
        assertThatNullPointerException().isThrownBy(() -> this.<Integer> empty().append(null));
    }

    @Test
    public void shouldAppendElementToNonNil() {
        final Vector<Integer> actual = of(1, 2).append(3);
        final Vector<Integer> expected = of(1, 2, 3);
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
        final Vector<Object> actual = empty().appendAll(empty());
        final Vector<Object> expected = empty();
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void shouldAppendAllNonNilToNil() {
        final Vector<Integer> actual = this.<Integer> empty().appendAll(of(1, 2, 3));
        final Vector<Integer> expected = of(1, 2, 3);
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void shouldAppendAllNilToNonNil() {
        final Vector<Integer> actual = of(1, 2, 3).appendAll(empty());
        final Vector<Integer> expected = of(1, 2, 3);
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void shouldAppendAllNonNilToNonNil() {
        final Vector<Integer> actual = of(1, 2, 3).appendAll(of(4, 5, 6));
        final Vector<Integer> expected = of(1, 2, 3, 4, 5, 6);
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void shouldAppendAllWhenUsedWithTypeHierarchy() {
        final Vector<SomeInterface> empty = of();
        final Vector<SomeInterface> all = empty
          .appendAll(of(OneEnum.values()))
          .appendAll(of(SecondEnum.values()));

        assertThat(all).isEqualTo(this.<SomeInterface>of(OneEnum.A1, OneEnum.A2, OneEnum.A3, SecondEnum.A1, SecondEnum.A2, SecondEnum.A3));
    }

    @Test
    public void shouldReturnSameVectorWhenEmptyAppendAllEmpty() {
        final Vector<Integer> empty = empty();
        assertThat(empty.appendAll(empty())).isSameAs(empty);
    }

    @Test
    public void shouldReturnSameVectorWhenEmptyAppendAllNonEmpty() {
        final Vector<Integer> seq = of(1, 2, 3);
        assertThat(empty().appendAll(seq)).isSameAs(seq);
    }

    @Test
    public void shouldReturnSameVectorWhenNonEmptyAppendAllEmpty() {
        final Vector<Integer> seq = of(1, 2, 3);
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
            final Vector<Integer> seq = of(1, 2, 3).asJavaMutable(list -> {
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
            final Vector<Integer> seq = of(1, 2, 3).asJava(list -> {
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
            final Vector<Integer> values = of(1, 2, 3);
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
        final Vector<Integer> values = of(1, 2, 3);
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
        public void shouldNotFindIndexOfElementWhenVectorIsEmpty() {
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
        public void shouldNotFindIndexOfSliceWhenVectorIsEmpty() {
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
        public void shouldNotFindLastIndexOfElementWhenVectorIsEmpty() {
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
        public void shouldNotFindLastIndexOfSliceWhenVectorIsEmpty() {
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
        final Vector<Integer> actual = this.<Integer> empty().insert(0, 1);
        final Vector<Integer> expected = of(1);
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void shouldInsertInFrontOfElement() {
        final Vector<Integer> actual = of(4).insert(0, 1);
        final Vector<Integer> expected = of(1, 4);
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void shouldInsertBehindOfElement() {
        final Vector<Integer> actual = of(4).insert(1, 5);
        final Vector<Integer> expected = of(4, 5);
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void shouldInsertIntoVector() {
        final Vector<Integer> actual = of(1, 2, 3).insert(2, 4);
        final Vector<Integer> expected = of(1, 2, 4, 3);
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
        final Vector<Integer> actual = this.<Integer> empty().insertAll(0, of(1, 2, 3));
        final Vector<Integer> expected = of(1, 2, 3);
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void shouldInsertAllInFrontOfElement() {
        final Vector<Integer> actual = of(4).insertAll(0, of(1, 2, 3));
        final Vector<Integer> expected = of(1, 2, 3, 4);
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void shouldInsertAllBehindOfElement() {
        final Vector<Integer> actual = of(4).insertAll(1, of(1, 2, 3));
        final Vector<Integer> expected = of(4, 1, 2, 3);
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void shouldInsertAllIntoVector() {
        final Vector<Integer> actual = of(1, 2, 3).insertAll(2, of(4, 5));
        final Vector<Integer> expected = of(1, 2, 4, 5, 3);
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
    public void shouldReturnSameVectorWhenEmptyInsertAllEmpty() {
        final Vector<Integer> empty = empty();
        assertThat(empty.insertAll(0, empty())).isSameAs(empty);
    }

    @Test
    public void shouldReturnSameVectorWhenEmptyInsertAllNonEmpty() {
        final Vector<Integer> seq = of(1, 2, 3);
        assertThat(empty().insertAll(0, seq)).isSameAs(seq);
    }

    @Test
    public void shouldReturnSameVectorWhenNonEmptyInsertAllEmpty() {
        final Vector<Integer> seq = of(1, 2, 3);
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
            final Vector<Integer> seq = of(1);
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
            final Vector<Integer> seq = of(1);
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
            final Vector<Character> s = of('1', '2', '3');
            assertThat(empty().patch(0, s, 0)).isEqualTo(s);
            assertThat(empty().patch(-1, s, -1)).isEqualTo(s);
            assertThat(empty().patch(-1, s, 1)).isEqualTo(s);
            assertThat(empty().patch(1, s, -1)).isEqualTo(s);
            assertThat(empty().patch(1, s, 1)).isEqualTo(s);
        }

        @Test
        public void shouldPatchNonEmptyByEmpty() {
            final Vector<Character> s = of('1', '2', '3');
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
            final Vector<Character> s = of('1', '2', '3');
            final Vector<Character> d = of('4', '5', '6');
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
        public void shouldComputePermutationsOfEmptyVector() {
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
        public void shouldComputePermutationsOfNonEmptyVector() {
            assertThat(of(1, 2, 3).permutations())
                    .isEqualTo(of(of(1, 2, 3), of(1, 3, 2), of(2, 1, 3), of(2, 3, 1), of(3, 1, 2), of(3, 2, 1)));
        }
    }

    // -- map

    @Test
    public void shouldMapTransformedVector() {
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
        final Vector<Integer> actual = this.<Integer> empty().prepend(1);
        final Vector<Integer> expected = of(1);
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void shouldPrependElementToNonNil() {
        final Vector<Integer> actual = of(2, 3).prepend(1);
        final Vector<Integer> expected = of(1, 2, 3);
        assertThat(actual).isEqualTo(expected);
    }

    // -- prependAll

    @Test
    public void shouldThrowOnPrependAllOfNull() {
        assertThrows(NullPointerException.class, () -> empty().prependAll(null));
    }

    @Test
    public void shouldPrependAllNilToNil() {
        final Vector<Integer> actual = this.<Integer> empty().prependAll(empty());
        final Vector<Integer> expected = empty();
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void shouldPrependAllNilToNonNil() {
        final Vector<Integer> actual = of(1, 2, 3).prependAll(empty());
        final Vector<Integer> expected = of(1, 2, 3);
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void shouldPrependAllNonNilToNil() {
        final Vector<Integer> actual = this.<Integer> empty().prependAll(of(1, 2, 3));
        final Vector<Integer> expected = of(1, 2, 3);
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void shouldPrependAllNonNilToNonNil() {
        final Vector<Integer> expected = range(0, 100);

        final Vector<Integer> actualFirstPartLarger = range(90, 100).prependAll(range(0, 90));
        assertThat(actualFirstPartLarger).isEqualTo(expected);

        final Vector<Integer> actualSecondPartLarger = range(10, 100).prependAll(range(0, 10));
        assertThat(actualSecondPartLarger).isEqualTo(expected);
    }

    @Test
    public void shouldReturnSameVectorWhenEmptyPrependAllEmpty() {
        final Vector<Integer> empty = empty();
        assertThat(empty.prependAll(empty())).isSameAs(empty);
    }

    @Test
    public void shouldReturnSameVectorWhenEmptyPrependAllNonEmpty() {
        final Vector<Integer> seq = of(1, 2, 3);
        assertThat(empty().prependAll(seq)).isSameAs(seq);
    }

    @Test
    public void shouldReturnSameVectorWhenNonEmptyPrependAllEmpty() {
        final Vector<Integer> seq = of(1, 2, 3);
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
        final Vector<Integer> t = of(1, 2, 3);
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
        final Vector<Integer> t = of(1, 2, 3);
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
        final Vector<Integer> t = of(1, 2, 3);
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
        final Vector<Integer> t = of(1, 2, 3);
        assertThat(t.removeAll(of(4, 5))).isSameAs(t);
    }

    @Test
    public void shouldReturnSameVectorWhenNonEmptyRemoveAllEmpty() {
        final Vector<Integer> seq = of(1, 2, 3);
        assertThat(seq.removeAll(empty())).isSameAs(seq);
    }

    @Test
    public void shouldReturnSameVectorWhenEmptyRemoveAllNonEmpty() {
        final Vector<Integer> empty = empty();
        assertThat(empty.removeAll(of(1, 2, 3))).isSameAs(empty);
    }

    // -- removeAll(Predicate)

    @SuppressWarnings("deprecation")
    @Test
    public void shouldRemoveExistingElements() {
        final Vector<Integer> seq = of(1, 2, 3);
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
        final Vector<Integer> t = of(1, 2, 3);
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
        final Vector<Integer> seq = of(1, 2, 3);
        assertThat(seq.removeAll(4)).isSameAs(seq);
    }

    @Test
    public void shouldNotRemoveAbsentNullFromNonEmpty() {
        final Vector<Integer> seq = of(1, 2, 3);
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
            Vector<Integer> seq = of(1);
            assertThat(seq.rotateLeft(1)).isSameAs(seq);
        }

        @Test
        public void shouldRotateLeftForZero() {
            Vector<Integer> seq = of(1, 2, 3, 4, 5);
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
            Vector<Integer> seq = of(1, 2, 3, 4, 5);
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
            Vector<Integer> seq = of(1);
            assertThat(seq.rotateRight(1)).isSameAs(seq);
        }

        @Test
        public void shouldRotateRightForZero() {
            Vector<Integer> seq = of(1, 2, 3, 4, 5);
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
            Vector<Integer> seq = of(1, 2, 3, 4, 5);
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
            final Vector<Integer> shuffled = of(1, 2, 3).shuffle();
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
            final Vector<Character> actual = ofAll("hello".toCharArray()).update(0, Character::toUpperCase);
            final Vector<Character> expected = ofAll("Hello".toCharArray());
            assertThat(actual).isEqualTo(expected);
        }
    }

    // -- slice(beginIndex, endIndex)

    @Test
    public void shouldReturnNilWhenSliceFrom0To0OnNil() {
        final Vector<Integer> actual = this.<Integer> empty().slice(0, 0);
        assertThat(actual).isEmpty();
    }

    @Test
    public void shouldReturnNilWhenSliceFrom0To0OnNonNil() {
        final Vector<Integer> actual = of(1).slice(0, 0);
        assertThat(actual).isEmpty();
    }

    @Test
    public void shouldReturnVectorWithFirstElementWhenSliceFrom0To1OnNonNil() {
        final Vector<Integer> actual = of(1).slice(0, 1);
        assertThat(actual).isEqualTo(of(1));
    }

    @Test
    public void shouldReturnNilWhenSliceFrom1To1OnNonNil() {
        final Vector<Integer> actual = of(1).slice(1, 1);
        assertThat(actual).isEmpty();
    }

    @Test
    public void shouldReturnSliceWhenIndicesAreWithinRange() {
        final Vector<Integer> actual = of(1, 2, 3).slice(1, 3);
        assertThat(actual).isEqualTo(of(2, 3));
    }

    @Test
    public void shouldReturnNilOnSliceWhenIndicesBothAreUpperBound() {
        final Vector<Integer> actual = of(1, 2, 3).slice(3, 3);
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
        final Vector<String> testee = of("aaa", "b", "cc");
        final Vector<String> actual = testee.sortBy(String::length);
        final Vector<String> expected = of("b", "cc", "aaa");
        assertThat(actual).isEqualTo(expected);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void shouldSortByFunctionWhenElementsAreInfiniteStreams() {
        final Stream<Integer> stream1 = Stream.continually(1);
        final Stream<Integer> stream2 = Stream.continually(2);
        final Vector<Stream<Integer>> testee = of(stream2, stream1);
        final Vector<Stream<Integer>> actual = testee.sortBy(Stream::head);
        final Vector<Stream<Integer>> expected = of(stream1, stream2);
        assertThat(actual).isEqualTo(expected);
    }

    // -- sortBy(Comparator, Function)

    @Test
    public void shouldSortByNilUsingComparatorAndFunction() {
        assertThat(this.<String> empty().sortBy(String::length)).isEmpty();
    }

    @Test
    public void shouldSortByNonNilUsingComparatorAndFunction() {
        final Vector<String> testee = of("aaa", "b", "cc");
        final Vector<String> actual = testee.sortBy((i1, i2) -> i2 - i1, String::length);
        final Vector<String> expected = of("aaa", "cc", "b");
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
    public void shouldReturnNilWhenSubVectoruenceFrom0OnNil() {
        final Vector<Integer> actual = this.<Integer> empty().subSequence(0);
        assertThat(actual).isEmpty();
    }

    @Test
    public void shouldReturnIdentityWhenSubVectoruenceFrom0OnNonNil() {
        final Vector<Integer> actual = of(1).subSequence(0);
        assertThat(actual).isEqualTo(of(1));
    }

    @Test
    public void shouldReturnNilWhenSubVectoruenceFrom1OnVectorOf1() {
        final Vector<Integer> actual = of(1).subSequence(1);
        assertThat(actual).isEmpty();
    }

    @Test
    public void shouldReturnSubVectoruenceWhenIndexIsWithinRange() {
        final Vector<Integer> actual = of(1, 2, 3).subSequence(1);
        assertThat(actual).isEqualTo(of(2, 3));
    }

    @Test
    public void shouldReturnNilWhenSubVectoruenceBeginningWithSize() {
        final Vector<Integer> actual = of(1, 2, 3).subSequence(3);
        assertThat(actual).isEmpty();
    }

    @Test
    public void shouldThrowWhenSubVectoruenceOnNil() {
        assertThrows(IndexOutOfBoundsException.class, () -> empty().subSequence(1));
    }

    @Test
    public void shouldThrowWhenSubVectoruenceWithOutOfLowerBound() {
        assertThrows(IndexOutOfBoundsException.class, () -> of(1, 2, 3).subSequence(-1));
    }

    @Test
    public void shouldThrowWhenSubVectoruenceWithOutOfUpperBound() {
        assertThrows(IndexOutOfBoundsException.class, () -> of(1, 2, 3).subSequence(4));
    }

    @Test
    public void shouldReturnSameInstanceIfSubVectoruenceStartsAtZero() {
        final Vector<Integer> seq = of(1, 2, 3);
        assertThat(seq.subSequence(0)).isSameAs(seq);
    }

    // -- subSequence(beginIndex, endIndex)

    @Test
    public void shouldReturnNilWhenSubVectoruenceFrom0To0OnNil() {
        final Vector<Integer> actual = this.<Integer> empty().subSequence(0, 0);
        assertThat(actual).isEmpty();
    }

    @Test
    public void shouldReturnNilWhenSubVectoruenceFrom0To0OnNonNil() {
        final Vector<Integer> actual = of(1).subSequence(0, 0);
        assertThat(actual).isEmpty();
    }

    @Test
    public void shouldReturnVectorWithFirstElementWhenSubVectoruenceFrom0To1OnNonNil() {
        final Vector<Integer> actual = of(1).subSequence(0, 1);
        assertThat(actual).isEqualTo(of(1));
    }

    @Test
    public void shouldReturnNilWhenSubVectoruenceFrom1To1OnNonNil() {
        final Vector<Integer> actual = of(1).subSequence(1, 1);
        assertThat(actual).isEmpty();
    }

    @Test
    public void shouldReturnSubVectoruenceWhenIndicesAreWithinRange() {
        final Vector<Integer> actual = of(1, 2, 3).subSequence(1, 3);
        assertThat(actual).isEqualTo(of(2, 3));
    }

    @Test
    public void shouldReturnNilWhenOnSubVectoruenceIndicesBothAreUpperBound() {
        final Vector<Integer> actual = of(1, 2, 3).subSequence(3, 3);
        assertThat(actual).isEmpty();
    }

    @Test
    public void shouldThrowOnSubVectoruenceOnNonNilWhenBeginIndexIsGreaterThanEndIndex() {
        assertThrows(IllegalArgumentException.class, () -> of(1, 2, 3).subSequence(1, 0));
    }

    @Test
    public void shouldThrowOnSubVectoruenceOnNilWhenBeginIndexIsGreaterThanEndIndex() {
        assertThrows(IllegalArgumentException.class, () -> empty().subSequence(1, 0));
    }

    @Test
    public void shouldThrowOnSubVectoruenceOnNonNilWhenBeginIndexExceedsLowerBound() {
        assertThrows(IndexOutOfBoundsException.class, () -> of(1, 2, 3).subSequence(-1, 2));
    }

    @Test
    public void shouldThrowOnSubVectoruenceOnNilWhenBeginIndexExceedsLowerBound() {
        assertThrows(IndexOutOfBoundsException.class, () -> empty().subSequence(-1, 2));
    }

    @Test
    public void shouldThrowWhenSubVectoruence2OnNil() {
        assertThrows(IndexOutOfBoundsException.class, () -> empty().subSequence(0, 1));
    }

    @Test
    public void shouldThrowOnSubVectoruenceWhenEndIndexExceedsUpperBound() {
        assertThrows(IndexOutOfBoundsException.class, () -> of(1, 2, 3).subSequence(1, 4).mkString()); // force computation of last element, e.g. because Stream is lazy
    }

    @Test
    public void shouldThrowOnSubVectoruenceWhenBeginIndexIsGreaterThanEndIndex() {
        assertThrows(IllegalArgumentException.class, () -> of(1, 2, 3).subSequence(2, 1).mkString()); // force computation of last element, e.g. because Stream is lazy
    }

    @Test
    public void shouldReturnSameInstanceIfSubVectoruenceStartsAtZeroAndEndsAtLastElement() {
        final Vector<Integer> seq = of(1, 2, 3);
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
            final Vector<Vector<Integer>> actual = empty();
            assertThat(transpose(actual)).isSameAs(actual);
        }

        @Test
        public void shouldTransposeIfIs1x0() {
            final Vector<Vector<Integer>> actual = of(empty());
            assertThat(transpose(actual)).isSameAs(actual);
        }

        @Test
        public void shouldTransposeIfIs1x1() {
            final Vector<Vector<Integer>> actual = of(of(1));
            assertThat(transpose(actual)).isSameAs(actual);
        }

        @Test
        public void shouldTransposeIfSingleValued() {
            final Vector<Vector<Integer>> actual = of(of(0));
            final Vector<Vector<Integer>> expected = of(of(0));
            assertThat(transpose(actual)).isEqualTo(expected);
        }

        @Test
        @SuppressWarnings("unchecked")
        public void shouldTransposeIfMultiValuedColumn() {
            final Vector<Vector<Integer>> actual = of(of(0, 1, 2));
            final Vector<Vector<Integer>> expected = of(of(0), of(1), of(2));
            assertThat(transpose(actual)).isEqualTo(expected);
        }

        @Test
        @SuppressWarnings("unchecked")
        public void shouldTransposeIfMultiValuedRow() {
            final Vector<Vector<Integer>> actual = of(of(0), of(1), of(2));
            final Vector<Vector<Integer>> expected = of(of(0, 1, 2));
            assertThat(transpose(actual)).isEqualTo(expected);
        }

        @Test
        @SuppressWarnings("unchecked")
        public void shouldTransposeIfMultiValuedIfSymmetric() {
            final Vector<Vector<Integer>> actual = of(
                    of(1, 2, 3),
                    of(4, 5, 6),
                    of(7, 8, 9));
            final Vector<Vector<Integer>> expected = of(
                    of(1, 4, 7),
                    of(2, 5, 8),
                    of(3, 6, 9));
            assertThat(transpose(actual)).isEqualTo(expected);
        }

        @Test
        @SuppressWarnings("unchecked")
        public void shouldTransposeIfMultiValuedWithMoreColumnsThanRows() {
            final Vector<Vector<Integer>> actual = of(
                    of(1, 2, 3),
                    of(4, 5, 6));
            final Vector<Vector<Integer>> expected = of(
                    of(1, 4),
                    of(2, 5),
                    of(3, 6));
            assertThat(transpose(actual)).isEqualTo(expected);
        }

        @Test
        @SuppressWarnings("unchecked")
        public void shouldTransposeIfMultiValuedWithMoreRowsThanColumns() {
            final Vector<Vector<Integer>> actual = of(
                    of(1, 2),
                    of(3, 4),
                    of(5, 6));
            final Vector<Vector<Integer>> expected = of(
                    of(1, 3, 5),
                    of(2, 4, 6));
            assertThat(transpose(actual)).isEqualTo(expected);
        }

        @Test
        @SuppressWarnings("unchecked")
        public void shouldBeEqualIfTransposedTwice() {
            final Vector<Vector<Integer>> actual = of(
                    of(1, 2, 3),
                    of(4, 5, 6));
            final Vector<Vector<Integer>> transposed = transpose(actual);
            assertThat(transpose(transposed)).isEqualTo(actual);
        }

        @Test
        @SuppressWarnings("unchecked")
        public void shouldNotTransposeForMissingOrEmptyValues() {
            assertThrows(IllegalArgumentException.class, () -> {
                final Vector<Vector<Integer>> actual = of(
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
    class NonVectorArgumentTests {
        @Test
        public void shouldStartWithANonVectorIterable() {
            assertThat(of(1, 3, 4).startsWith(Stream.of(1, 3))).isTrue();
            assertThat(of(1, 2, 3, 4).startsWith(Stream.of(1, 2, 4))).isFalse();
            assertThat(of(1, 2).startsWith(Stream.of(1, 2, 4))).isFalse();
        }

        @Test
        public void shouldEndWithANonVectorIterable() {
            assertThat(of(1, 3, 4).endsWith(Stream.of(3, 4))).isTrue();
            assertThat(of(1, 2, 3, 4).endsWith(Stream.of(2, 3, 5))).isFalse();
        }
    }

    // -- distinctByKeepLast(Comparator)

    @TestTemplate
    public void shouldComputeDistinctByKeepLastOfEmptyVectorUsingComparator() {
        final Comparator<Integer> comparator = comparingInt(i -> i);
        assertThat(this.<Integer>empty().distinctByKeepLast(comparator)).isSameAs(empty());
    }

    @TestTemplate
    public void shouldComputeDistinctByKeepLastOfNonEmptyVectorUsingComparator() {
        final Comparator<String> comparator = comparingInt(s -> (s.charAt(1)));
        final Vector<String> distinct = of("1a", "2a", "3b", "4b", "3a", "5c")
                .distinctByKeepLast(comparator);
        assertThat(distinct).isEqualTo(of("4b", "3a", "5c"));
    }

    @TestTemplate
    public void shouldReturnSameInstanceWhenDistinctByKeepLastComparatorEmptyVector() {
        final Vector<?> empty = empty();
        assertThat(empty.distinctByKeepLast(Comparators.naturalComparator())).isSameAs(empty);
    }

    // -- distinctByKeepLast(Function)

    @TestTemplate
    public void shouldComputeDistinctByKeepLastOfEmptyVectorUsingKeyExtractor() {
        assertThat(empty().distinctByKeepLast(Function.identity())).isSameAs(empty());
    }

    @TestTemplate
    public void shouldComputeDistinctByKeepLastOfNonEmptyVectorUsingKeyExtractor() {
        final Function<String, Character> function = c -> c.charAt(1);
        final Vector<String> distinct = of("1a", "2a", "3b", "4b", "3a", "5c")
                .distinctByKeepLast(function);
        assertThat(distinct).isEqualTo(of("4b", "3a", "5c"));
    }

    @TestTemplate
    public void shouldReturnSameInstanceWhenDistinctByKeepLastFunctionEmptyVector() {
        final Vector<?> empty = empty();
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
            assertThatNullPointerException().isThrownBy(() -> VectorTest.this.<Integer>of((Integer) null));
        }

        @Test
        public void shouldRejectNullOnOfVarargs() {
            assertThatNullPointerException().isThrownBy(() -> VectorTest.this.<Integer>of(1, null));
        }
    }

    // -- the sequence methods Vector declares itself (design 3.7), tested at the 32-wide trie boundaries, on int[] and
    // Object[] leaves, with and without a trie offset

    static final int[] BOUNDARIES = { 0, 1, 31, 32, 33, 1023, 1024, 1025 };

    /* range(0, n) in every representation the trie has: primitive and Object leaves, each with and without an offset */
    static java.util.List<Vector<Integer>> representations(int n) {
        final Vector<Integer> primitive = Vector.range(0, n);
        final Vector<Integer> boxed = Vector.ofAll(primitive.toJavaList());
        final Vector<Integer> offsetPrimitive = Vector.range(-5, n).drop(5);
        final Vector<Integer> offsetBoxed = Vector.ofAll(Vector.range(-5, n).toJavaList()).drop(5);
        return java.util.List.of(primitive, boxed, offsetPrimitive, offsetBoxed);
    }

    /* the positions worth probing in a vector of n elements: the leaf boundaries and the ends */
    static int[] positions(int n) {
        return java.util.stream.IntStream.of(0, 1, 31, 32, 33, 1023, 1024, n - 1, n).filter(i -> i >= 0 && i <= n).distinct().sorted().toArray();
    }

    @Nested
    class TraversableOnlyTests {
        @Test
        public void shouldImplementTraversableOnly() {
            assertThat(Traversable.class.isAssignableFrom(Vector.class)).isTrue();
            assertThat(Vector.class.getInterfaces()).containsExactly(Traversable.class);
            assertThat(Vector.class.getSuperclass()).isEqualTo(Object.class);
            // no sequence interface above Traversable: a Vector reaches Traversable directly
            assertThat(supertypeNames(Vector.class)).containsExactlyInAnyOrder("Traversable", "Foldable", "Iterable");
        }

        @Test
        public void shouldHaveSizedAndOrderedSpliterator() {
            assertThat(of(1, 2, 3).spliterator().hasCharacteristics(Spliterator.SIZED | Spliterator.SUBSIZED | Spliterator.ORDERED)).isTrue();
            assertThat(of(1, 2, 3).spliterator().getExactSizeIfKnown()).isEqualTo(3);
            assertThat(empty().spliterator().getExactSizeIfKnown()).isEqualTo(0);
        }
    }

    @Nested
    class LastTests {
        @Test
        public void shouldReturnTheLastElementAtEveryBoundary() {
            for (int n : BOUNDARIES) {
                for (Vector<Integer> vector : representations(n)) {
                    if (n == 0) {
                        assertThatThrownBy(vector::last).isInstanceOf(NoSuchElementException.class).hasMessage("last of empty Vector");
                        assertThat(vector.lastOption()).isEqualTo(Option.none());
                    } else {
                        assertThat(vector.last()).isEqualTo(n - 1);
                        assertThat(vector.lastOption()).isEqualTo(Option.some(n - 1));
                        assertThat(vector.take(1).last()).isEqualTo(0);
                    }
                }
            }
        }
    }

    @Nested
    class FoldRightTests {
        @Test
        public void shouldFoldFromTheRightAtEveryBoundary() {
            for (int n : BOUNDARIES) {
                for (Vector<Integer> vector : representations(n)) {
                    final String expected = vector.reverse().foldLeft("", (acc, i) -> acc + i + ",");
                    assertThat(vector.foldRight("", (i, acc) -> acc + i + ",")).isEqualTo(expected);
                    assertThat(vector.foldRight(0, Integer::sum)).isEqualTo(n * (n - 1) / 2);
                }
            }
        }

        @Test
        public void shouldReturnZeroForEmptyAndRejectNullFunction() {
            final Object zero = new Object();
            assertThat(Vector.<Integer> empty().foldRight(zero, (i, acc) -> acc)).isSameAs(zero);
            assertThatNullPointerException().isThrownBy(() -> of(1).foldRight(0, null)).withMessage("f is null");
        }
    }

    @Nested
    class IndexSearchTests {

        private int referenceIndexOf(Vector<Integer> vector, int element, int from) {
            for (int i = Math.max(from, 0); i < vector.length(); i++) {
                if (vector.get(i) == element) {
                    return i;
                }
            }
            return -1;
        }

        private int referenceLastIndexOf(Vector<Integer> vector, int element, int end) {
            for (int i = Math.min(end, vector.length() - 1); i >= 0; i--) {
                if (vector.get(i) == element) {
                    return i;
                }
            }
            return -1;
        }

        @Test
        public void shouldFindIndexOfAtEveryBoundary() {
            for (int n : BOUNDARIES) {
                for (Vector<Integer> vector : representations(n)) {
                    for (int element : new int[] { -1, 0, 1, 31, 32, 33, n - 1, n }) {
                        for (int from : new int[] { Integer.MIN_VALUE, -1, 0, 1, 31, 32, 33, n - 1, n, n + 1, Integer.MAX_VALUE }) {
                            final int expected = referenceIndexOf(vector, element, from);
                            assertThat(vector.indexOf(element, from)).isEqualTo(expected);
                            assertThat(vector.indexOfOption(element, from)).isEqualTo(expected < 0 ? Option.none() : Option.some(expected));
                            assertThat(vector.indexWhere(i -> i == element, from)).isEqualTo(expected);
                            assertThat(vector.indexWhereOption(i -> i == element, from)).isEqualTo(expected < 0 ? Option.none() : Option.some(expected));
                        }
                        final int expected = referenceIndexOf(vector, element, 0);
                        assertThat(vector.indexOf(element)).isEqualTo(expected);
                        assertThat(vector.indexOfOption(element)).isEqualTo(expected < 0 ? Option.none() : Option.some(expected));
                        assertThat(vector.indexWhere(i -> i == element)).isEqualTo(expected);
                        assertThat(vector.indexWhereOption(i -> i == element)).isEqualTo(expected < 0 ? Option.none() : Option.some(expected));
                    }
                }
            }
        }

        @Test
        public void shouldFindLastIndexOfAtEveryBoundary() {
            for (int n : BOUNDARIES) {
                for (Vector<Integer> vector : representations(n)) {
                    for (int element : new int[] { -1, 0, 1, 31, 32, 33, n - 1, n }) {
                        for (int end : new int[] { Integer.MIN_VALUE, -1, 0, 1, 31, 32, 33, n - 1, n, n + 1, Integer.MAX_VALUE }) {
                            final int expected = referenceLastIndexOf(vector, element, end);
                            assertThat(vector.lastIndexOf(element, end)).isEqualTo(expected);
                            assertThat(vector.lastIndexOfOption(element, end)).isEqualTo(expected < 0 ? Option.none() : Option.some(expected));
                            assertThat(vector.lastIndexWhere(i -> i == element, end)).isEqualTo(expected);
                            assertThat(vector.lastIndexWhereOption(i -> i == element, end)).isEqualTo(expected < 0 ? Option.none() : Option.some(expected));
                        }
                        final int expected = referenceLastIndexOf(vector, element, Integer.MAX_VALUE);
                        assertThat(vector.lastIndexOf(element)).isEqualTo(expected);
                        assertThat(vector.lastIndexOfOption(element)).isEqualTo(expected < 0 ? Option.none() : Option.some(expected));
                        assertThat(vector.lastIndexWhere(i -> i == element)).isEqualTo(expected);
                        assertThat(vector.lastIndexWhereOption(i -> i == element)).isEqualTo(expected < 0 ? Option.none() : Option.some(expected));
                    }
                }
            }
        }

        @Test
        public void shouldFindTheLastOfRepeatedElements() {
            final Vector<Integer> repeated = Vector.range(0, 33).appendAll(Vector.range(0, 33));
            assertThat(repeated.lastIndexOf(0)).isEqualTo(33);
            assertThat(repeated.lastIndexOf(32)).isEqualTo(65);
            assertThat(repeated.lastIndexOf(0, 32)).isEqualTo(0);
            assertThat(repeated.lastIndexWhere(i -> i == 31)).isEqualTo(64);
            assertThat(repeated.indexOf(31, 32)).isEqualTo(64);
        }

        @Test
        public void shouldComputeSegmentLengthAndPrefixLengthAtEveryBoundary() {
            for (int n : BOUNDARIES) {
                for (Vector<Integer> vector : representations(n)) {
                    for (int k : new int[] { 0, 1, 31, 32, 33, n, n + 1 }) {
                        assertThat(vector.prefixLength(i -> i < k)).isEqualTo(Math.min(k, n));
                        for (int from : new int[] { -1, 0, 1, 31, 32, 33, n, n + 1 }) {
                            final int start = Math.max(from, 0);
                            assertThat(vector.segmentLength(i -> i < k, from)).isEqualTo(Math.max(0, Math.min(k, n) - start));
                        }
                    }
                }
            }
        }

        @Test
        public void shouldRejectNullPredicates() {
            final Vector<Integer> vector = of(1, 2, 3);
            assertThatNullPointerException().isThrownBy(() -> vector.indexWhere(null)).withMessage("predicate is null");
            assertThatNullPointerException().isThrownBy(() -> vector.indexWhere(null, 1)).withMessage("predicate is null");
            assertThatNullPointerException().isThrownBy(() -> vector.indexWhereOption(null)).withMessage("predicate is null");
            assertThatNullPointerException().isThrownBy(() -> vector.indexWhereOption(null, 1)).withMessage("predicate is null");
            assertThatNullPointerException().isThrownBy(() -> vector.lastIndexWhere(null)).withMessage("predicate is null");
            assertThatNullPointerException().isThrownBy(() -> vector.lastIndexWhere(null, 1)).withMessage("predicate is null");
            assertThatNullPointerException().isThrownBy(() -> vector.lastIndexWhereOption(null)).withMessage("predicate is null");
            assertThatNullPointerException().isThrownBy(() -> vector.lastIndexWhereOption(null, 1)).withMessage("predicate is null");
            assertThatNullPointerException().isThrownBy(() -> vector.segmentLength(null, 0)).withMessage("predicate is null");
            assertThatNullPointerException().isThrownBy(() -> vector.prefixLength(null)).withMessage("predicate is null");
        }
    }

    @Nested
    class SliceSearchTests {
        @Test
        public void shouldFindSlicesAcrossLeafBoundariesInEveryArgumentShape() {
            for (int n : BOUNDARIES) {
                for (Vector<Integer> vector : representations(n)) {
                    for (int k : positions(n)) {
                        if (k + 3 > n) {
                            continue;
                        }
                        final Vector<Integer> slice = vector.slice(k, k + 3);
                        // a Vector, a JDK list (traversable again), a lazy Stream and a one-shot Iterator
                        for (Iterable<Integer> shape : java.util.List.<Iterable<Integer>> of(slice, slice.toJavaList(), Stream.ofAll(slice))) {
                            assertThat(vector.indexOfSlice(shape)).isEqualTo(k);
                            assertThat(vector.indexOfSliceOption(shape)).isEqualTo(Option.some(k));
                            assertThat(vector.lastIndexOfSlice(shape)).isEqualTo(k);
                            assertThat(vector.lastIndexOfSliceOption(shape)).isEqualTo(Option.some(k));
                            assertThat(vector.containsSlice(shape)).isTrue();
                        }
                        assertThat(vector.indexOfSlice(Iterator.ofAll(slice))).isEqualTo(k);
                        assertThat(vector.lastIndexOfSlice(Iterator.ofAll(slice))).isEqualTo(k);
                        assertThat(vector.containsSlice(Iterator.ofAll(slice))).isTrue();
                        assertThat(vector.indexOfSlice(slice, k)).isEqualTo(k);
                        assertThat(vector.indexOfSlice(slice, k + 1)).isEqualTo(-1);
                        assertThat(vector.indexOfSliceOption(slice, k + 1)).isEqualTo(Option.none());
                        assertThat(vector.lastIndexOfSlice(slice, k)).isEqualTo(k);
                        assertThat(vector.lastIndexOfSlice(slice, k - 1)).isEqualTo(-1);
                        assertThat(vector.lastIndexOfSliceOption(slice, k - 1)).isEqualTo(Option.none());
                        assertThat(vector.containsSlice(slice.append(-1))).isFalse();
                        assertThat(vector.containsSlice(slice.prepend(-1))).isFalse();
                    }
                    assertThat(vector.indexOfSlice(Vector.of(n, n + 1))).isEqualTo(-1);
                    assertThat(vector.lastIndexOfSlice(Vector.of(n, n + 1))).isEqualTo(-1);
                    assertThat(vector.containsSlice(Vector.of(n, n + 1))).isFalse();
                    // the empty slice is everywhere: first at 0, last at the end, and at any offset up to the end
                    assertThat(vector.indexOfSlice(Vector.empty())).isEqualTo(0);
                    assertThat(vector.indexOfSlice(Vector.empty(), n)).isEqualTo(n);
                    assertThat(vector.indexOfSlice(Vector.empty(), n + 1)).isEqualTo(-1);
                    assertThat(vector.lastIndexOfSlice(Vector.empty())).isEqualTo(n);
                    assertThat(vector.lastIndexOfSlice(Vector.empty(), -1)).isEqualTo(-1);
                    assertThat(vector.containsSlice(Vector.empty())).isTrue();
                }
            }
        }

        @Test
        public void shouldFindOverlappingSlices() {
            final Vector<Integer> vector = of(1, 2, 1, 2, 1);
            assertThat(vector.indexOfSlice(of(1, 2, 1))).isEqualTo(0);
            assertThat(vector.lastIndexOfSlice(of(1, 2, 1))).isEqualTo(2);
            assertThat(vector.indexOfSlice(of(1, 2, 1), 1)).isEqualTo(2);
            assertThat(vector.lastIndexOfSlice(of(1, 2, 1), 1)).isEqualTo(0);
            assertThat(vector.indexOfSlice(of(2, 1, 2))).isEqualTo(1);
            assertThat(vector.lastIndexOfSlice(of(2, 1, 2))).isEqualTo(1);
        }

        @Test
        public void shouldIterateAOneShotSliceOnlyOnce() {
            // a java.util.stream can be iterated once: a second iterator() call throws IllegalStateException
            final Vector<Integer> vector = of(1, 2, 3, 2, 3);
            assertThat(vector.indexOfSlice(java.util.stream.Stream.of(2, 3)::iterator)).isEqualTo(1);
            assertThat(vector.indexOfSlice(java.util.stream.Stream.of(2, 3)::iterator, 2)).isEqualTo(3);
            assertThat(vector.lastIndexOfSlice(java.util.stream.Stream.of(2, 3)::iterator)).isEqualTo(3);
            assertThat(vector.lastIndexOfSlice(java.util.stream.Stream.of(2, 3)::iterator, 2)).isEqualTo(1);
            assertThat(vector.lastIndexOfSlice(java.util.stream.Stream.<Integer> empty()::iterator)).isEqualTo(5);
            assertThat(vector.lastIndexOfSlice(java.util.stream.Stream.<Integer> empty()::iterator, 2)).isEqualTo(2);
            assertThat(vector.containsSlice(java.util.stream.Stream.of(3, 2)::iterator)).isTrue();
            assertThat(vector.containsSlice(java.util.stream.Stream.of(3, 1)::iterator)).isFalse();
            assertThat(Vector.<Integer> empty().indexOfSlice(java.util.stream.Stream.<Integer> empty()::iterator)).isEqualTo(0);
            assertThat(Vector.<Integer> empty().indexOfSlice(java.util.stream.Stream.of(1)::iterator)).isEqualTo(-1);
            assertThat(Vector.<Integer> empty().lastIndexOfSlice(java.util.stream.Stream.<Integer> empty()::iterator)).isEqualTo(0);
            assertThat(Vector.<Integer> empty().lastIndexOfSlice(java.util.stream.Stream.of(1)::iterator)).isEqualTo(-1);
        }

        @Test
        public void shouldRejectNullSlices() {
            final Vector<Integer> vector = of(1, 2, 3);
            assertThatNullPointerException().isThrownBy(() -> vector.indexOfSlice(null)).withMessage("that is null");
            assertThatNullPointerException().isThrownBy(() -> vector.indexOfSlice(null, 0)).withMessage("that is null");
            assertThatNullPointerException().isThrownBy(() -> vector.lastIndexOfSlice(null)).withMessage("that is null");
            assertThatNullPointerException().isThrownBy(() -> vector.lastIndexOfSlice(null, 0)).withMessage("that is null");
            assertThatNullPointerException().isThrownBy(() -> vector.containsSlice(null)).withMessage("that is null");
            assertThatNullPointerException().isThrownBy(() -> Vector.<Integer> empty().indexOfSlice(null)).withMessage("that is null");
        }
    }

    @Nested
    class StartsWithEndsWithTests {
        @Test
        public void shouldTestPrefixesAndSuffixesInEveryArgumentShapeAtEveryBoundary() {
            for (int n : BOUNDARIES) {
                for (Vector<Integer> vector : representations(n)) {
                    for (int k : positions(n)) {
                        final Vector<Integer> prefix = vector.take(k);
                        final Vector<Integer> suffix = vector.drop(k);
                        // a Vector (compared by index), a JDK list, a Stream and a one-shot Iterator (walked once)
                        assertThat(vector.startsWith(prefix)).isTrue();
                        assertThat(vector.startsWith(prefix.toJavaList())).isTrue();
                        assertThat(vector.startsWith(Stream.ofAll(prefix))).isTrue();
                        assertThat(vector.startsWith(Iterator.ofAll(prefix))).isTrue();
                        assertThat(vector.startsWith(suffix, k)).isTrue();
                        assertThat(vector.startsWith(suffix.toJavaList(), k)).isTrue();
                        assertThat(vector.startsWith(Iterator.ofAll(suffix), k)).isTrue();
                        assertThat(vector.endsWith(suffix)).isTrue();
                        assertThat(vector.endsWith(suffix.toJavaList())).isTrue();
                        assertThat(vector.endsWith(Stream.ofAll(suffix))).isTrue();
                        assertThat(vector.endsWith(Iterator.ofAll(suffix))).isTrue();
                        // one element too many, or one element wrong, in either shape
                        assertThat(vector.startsWith(prefix.append(-1))).isFalse();
                        assertThat(vector.startsWith(prefix.append(-1).toJavaList())).isFalse();
                        assertThat(vector.startsWith(suffix.append(-1), k)).isFalse();
                        assertThat(vector.startsWith(suffix.append(-1).toJavaList(), k)).isFalse();
                        assertThat(vector.endsWith(suffix.prepend(-1))).isFalse();
                        assertThat(vector.endsWith(suffix.prepend(-1).toJavaList())).isFalse();
                        if (k < n) {
                            assertThat(vector.startsWith(prefix.append(-1).appendAll(vector.drop(k + 1)))).isFalse();
                            assertThat(vector.endsWith(vector.take(k).append(-1).appendAll(vector.drop(k + 1)))).isFalse();
                            assertThat(vector.startsWith(suffix.update(0, -1), k)).isFalse();
                            assertThat(vector.startsWith(suffix.update(0, -1).toJavaList(), k)).isFalse();
                        }
                    }
                    // an empty prefix starts anywhere, even beyond the end; a negative offset never matches
                    assertThat(vector.startsWith(Vector.empty(), n + 1)).isTrue();
                    assertThat(vector.startsWith(java.util.List.of(), n + 1)).isTrue();
                    assertThat(vector.startsWith(Vector.empty(), -1)).isFalse();
                    assertThat(vector.startsWith(vector, -1)).isFalse();
                    assertThat(vector.startsWith(Vector.of(0), n)).isFalse();
                    assertThat(vector.startsWith(java.util.List.of(0), n)).isFalse();
                    assertThat(vector.startsWith(Vector.of(n - 1), n + 1)).isFalse();
                    assertThat(vector.endsWith(Vector.empty())).isTrue();
                    assertThat(vector.endsWith(vector.prepend(-1))).isFalse();
                }
            }
        }

        @Test
        public void shouldRejectNullArguments() {
            final Vector<Integer> vector = of(1, 2, 3);
            assertThatNullPointerException().isThrownBy(() -> vector.startsWith(null)).withMessage("that is null");
            assertThatNullPointerException().isThrownBy(() -> vector.startsWith(null, 1)).withMessage("that is null");
            assertThatNullPointerException().isThrownBy(() -> vector.endsWith(null)).withMessage("that is null");
        }
    }

    @Nested
    class SearchTests {
        @Test
        public void shouldBinarySearchAtEveryBoundary() {
            for (int n : BOUNDARIES) {
                for (Vector<Integer> vector : representations(n)) {
                    for (int element : new int[] { 0, 1, 31, 32, 33, n - 1 }) {
                        if (element < 0 || element >= n) {
                            continue;
                        }
                        assertThat(vector.search(element)).isEqualTo(element);
                        assertThat(vector.search(element, Integer::compare)).isEqualTo(element);
                        assertThat(vector.reverse().search(element, Comparator.reverseOrder())).isEqualTo(n - 1 - element);
                    }
                    assertThat(vector.search(-1)).isEqualTo(-1);
                    assertThat(vector.search(n)).isEqualTo(-(n + 1));
                    assertThat(vector.search(n, Integer::compare)).isEqualTo(-(n + 1));
                    assertThat(vector.search(Integer.MAX_VALUE)).isEqualTo(-(n + 1));
                }
            }
            assertThat(of(10, 20, 30).search(25)).isEqualTo(-3);
            assertThat(of(10, 20, 30).search(5)).isEqualTo(-1);
            assertThat(of(10, 20, 30).search(35)).isEqualTo(-4);
        }

        @Test
        public void shouldRejectNullComparatorAndNonComparableElements() {
            assertThatNullPointerException().isThrownBy(() -> of(1).search(1, null)).withMessage("comparator is null");
            assertThatThrownBy(() -> Vector.of(new Object()).search(new Object())).isInstanceOf(ClassCastException.class);
            assertThat(Vector.<Object> empty().search(new Object())).isEqualTo(-1);
        }
    }

    @Nested
    class ReverseAtBoundariesTests {
        @Test
        public void shouldReverseAndIterateBackwardsAtEveryBoundary() {
            for (int n : BOUNDARIES) {
                for (Vector<Integer> vector : representations(n)) {
                    final java.util.List<Integer> expected = new java.util.ArrayList<>(vector.toJavaList());
                    java.util.Collections.reverse(expected);
                    assertThat(vector.reverse().toJavaList()).isEqualTo(expected);
                    assertThat(vector.reverse().reverse()).isEqualTo(vector);
                    final Iterator<Integer> iterator = vector.reverseIterator();
                    assertThat(iterator.toJavaList()).isEqualTo(expected);
                    assertThat(iterator.hasNext()).isFalse();
                    assertThatThrownBy(iterator::next).isInstanceOf(NoSuchElementException.class);
                    if (n <= 1) {
                        assertThat(vector.reverse()).isSameAs(vector);
                    }
                }
            }
        }
    }

    @Nested
    class CartesianProductTests {
        @Test
        public void shouldComputeTheCartesianSquareAtEveryBoundary() {
            for (int n : new int[] { 0, 1, 31, 32, 33 }) {
                for (Vector<Integer> vector : representations(n)) {
                    final List<Tuple2<Integer, Integer>> pairs = vector.crossProduct().toList();
                    assertThat(pairs.size()).isEqualTo(n * n);
                    if (n > 0) {
                        assertThat(pairs.head()).isEqualTo(Tuple.of(0, 0));
                        if (n > 1) {
                            assertThat(pairs.get(n)).isEqualTo(Tuple.of(1, 0));
                        }
                        assertThat(pairs.last()).isEqualTo(Tuple.of(n - 1, n - 1));
                    }
                }
            }
        }

        @Test
        public void shouldComputeTheProductWithEveryArgumentShape() {
            final Vector<Integer> vector = of(1, 2, 3);
            final List<Tuple2<Integer, Character>> expected = List.of(Tuple.of(1, 'a'), Tuple.of(1, 'b'), Tuple.of(2, 'a'), Tuple.of(2, 'b'), Tuple.of(3, 'a'), Tuple.of(3, 'b'));
            assertThat(vector.crossProduct(Vector.of('a', 'b')).toList()).isEqualTo(expected);
            assertThat(vector.crossProduct(java.util.List.of('a', 'b')).toList()).isEqualTo(expected);
            assertThat(vector.crossProduct(Stream.of('a', 'b')).toList()).isEqualTo(expected);
            assertThat(vector.crossProduct(Iterator.of('a', 'b')).toList()).isEqualTo(expected);
            assertThat(vector.crossProduct(Vector.range(0, 33)).size()).isEqualTo(99);
            assertThat(Vector.range(0, 33).crossProduct(vector).size()).isEqualTo(99);
            assertThat(vector.crossProduct(Vector.empty())).isEmpty();
            assertThat(Vector.empty().crossProduct(vector)).isEmpty();
            // the argument stays lazy: an infinite iterator works with take
            assertThat(vector.crossProduct(Iterator.from(0)).take(3).toList()).isEqualTo(List.of(Tuple.of(1, 0), Tuple.of(1, 1), Tuple.of(1, 2)));
            assertThat(Vector.of(1).crossProduct(Iterator.from(0)).take(3).toList()).isEqualTo(List.of(Tuple.of(1, 0), Tuple.of(1, 1), Tuple.of(1, 2)));
            assertThat(vector.crossProduct(Iterator.from(0)).take(5).toList().last()).isEqualTo(Tuple.of(1, 4));
            // and a one-shot argument is walked once, memoised for the next element of the receiver
            assertThat(vector.crossProduct(java.util.stream.Stream.of('a', 'b')::iterator).toList()).isEqualTo(expected);
            assertThatNullPointerException().isThrownBy(() -> vector.crossProduct((Iterable<Integer>) null)).withMessage("that is null");
        }

        @Test
        public void shouldComputeThePowerLazilyWithVectorResults() {
            final Vector<Integer> vector = of(0, 1, 2);
            assertThat(vector.crossProduct(-1).toList()).isEqualTo(List.empty());
            assertThat(vector.crossProduct(0).toList()).isEqualTo(List.of(Vector.empty()));
            assertThat(vector.crossProduct(1).toList()).isEqualTo(List.of(of(0), of(1), of(2)));
            final List<Vector<Integer>> cubes = vector.crossProduct(3).toList();
            assertThat(cubes.size()).isEqualTo(27);
            assertThat(cubes.head()).isEqualTo(of(0, 0, 0));
            assertThat(cubes.get(1)).isEqualTo(of(0, 0, 1));
            assertThat(cubes.last()).isEqualTo(of(2, 2, 2));
            assertThat(cubes.distinct().size()).isEqualTo(27);
            for (Vector<Integer> cube : cubes) {
                assertThat(cube).isInstanceOf(Vector.class);
            }
            assertThat(Vector.range(0, 33).crossProduct(2).size()).isEqualTo(33 * 33);
            assertThat(Vector.range(0, 10).crossProduct(100).take(1).head()).isEqualTo(Vector.fill(100, 0));
            assertThat(Vector.<Integer> empty().crossProduct(2)).isEmpty();
            assertThat(Vector.<Integer> empty().crossProduct(0).toList()).isEqualTo(List.of(Vector.empty()));
        }
    }

    @Nested
    class UntilWhileTests {

        private int[] cuts(int n) {
            return positions(n);
        }

        @Test
        public void shouldDropAndTakeByPredicateFromBothEndsAtEveryBoundary() {
            for (int n : BOUNDARIES) {
                for (Vector<Integer> vector : representations(n)) {
                    for (int k : cuts(n)) {
                        // the elements are 0 .. n - 1, so "i >= k" cuts the vector at k
                        assertThat(vector.dropUntil(i -> i >= k)).isEqualTo(vector.drop(k));
                        assertThat(vector.dropWhile(i -> i < k)).isEqualTo(vector.drop(k));
                        assertThat(vector.takeUntil(i -> i >= k)).isEqualTo(vector.take(k));
                        assertThat(vector.takeWhile(i -> i < k)).isEqualTo(vector.take(k));
                        assertThat(vector.dropRightUntil(i -> i < k)).isEqualTo(vector.take(k));
                        assertThat(vector.dropRightWhile(i -> i >= k)).isEqualTo(vector.take(k));
                        assertThat(vector.takeRightUntil(i -> i < k)).isEqualTo(vector.drop(k));
                        assertThat(vector.takeRightWhile(i -> i >= k)).isEqualTo(vector.drop(k));
                        assertThat(vector.splitAt(i -> i >= k)).isEqualTo(Tuple.of(vector.take(k), vector.drop(k)));
                        assertThat(vector.span(i -> i < k)).isEqualTo(Tuple.of(vector.take(k), vector.drop(k)));
                        if (k < n) {
                            assertThat(vector.splitAtInclusive(i -> i >= k)).isEqualTo(Tuple.of(vector.take(k + 1), vector.drop(k + 1)));
                        }
                    }
                    assertThat(vector.splitAtInclusive(i -> false)).isEqualTo(Tuple.of(vector, Vector.empty()));
                }
            }
        }

        @Test
        public void shouldReturnTheSameInstanceOrTheEmptySingletonWhenNothingChanges() {
            for (int n : BOUNDARIES) {
                for (Vector<Integer> vector : representations(n)) {
                    assertThat(vector.takeUntil(i -> false)).isSameAs(vector);
                    assertThat(vector.takeWhile(i -> true)).isSameAs(vector);
                    assertThat(vector.takeRightUntil(i -> false)).isSameAs(vector);
                    assertThat(vector.takeRightWhile(i -> true)).isSameAs(vector);
                    assertThat(vector.dropUntil(i -> true)).isSameAs(vector);
                    assertThat(vector.dropWhile(i -> false)).isSameAs(vector);
                    assertThat(vector.dropRightUntil(i -> true)).isSameAs(vector);
                    assertThat(vector.dropRightWhile(i -> false)).isSameAs(vector);
                    assertThat(vector.dropUntil(i -> false)).isSameAs(Vector.empty());
                    assertThat(vector.dropWhile(i -> true)).isSameAs(Vector.empty());
                    assertThat(vector.dropRightUntil(i -> false)).isSameAs(Vector.empty());
                    assertThat(vector.dropRightWhile(i -> true)).isSameAs(Vector.empty());
                    assertThat(vector.takeUntil(i -> true)).isSameAs(Vector.empty());
                    assertThat(vector.takeWhile(i -> false)).isSameAs(Vector.empty());
                    assertThat(vector.takeRightUntil(i -> true)).isSameAs(Vector.empty());
                    assertThat(vector.takeRightWhile(i -> false)).isSameAs(Vector.empty());
                }
            }
        }

        @Test
        public void shouldRejectNullPredicates() {
            for (Vector<Integer> vector : java.util.List.of(Vector.<Integer> empty(), of(1, 2, 3))) {
                assertThatNullPointerException().isThrownBy(() -> vector.dropUntil(null)).withMessage("predicate is null");
                assertThatNullPointerException().isThrownBy(() -> vector.dropWhile(null)).withMessage("predicate is null");
                assertThatNullPointerException().isThrownBy(() -> vector.dropRightUntil(null)).withMessage("predicate is null");
                assertThatNullPointerException().isThrownBy(() -> vector.dropRightWhile(null)).withMessage("predicate is null");
                assertThatNullPointerException().isThrownBy(() -> vector.takeUntil(null)).withMessage("predicate is null");
                assertThatNullPointerException().isThrownBy(() -> vector.takeWhile(null)).withMessage("predicate is null");
                assertThatNullPointerException().isThrownBy(() -> vector.takeRightUntil(null)).withMessage("predicate is null");
                assertThatNullPointerException().isThrownBy(() -> vector.takeRightWhile(null)).withMessage("predicate is null");
                assertThatNullPointerException().isThrownBy(() -> vector.splitAt(null)).withMessage("predicate is null");
                assertThatNullPointerException().isThrownBy(() -> vector.splitAtInclusive(null)).withMessage("predicate is null");
                assertThatNullPointerException().isThrownBy(() -> vector.span(null)).withMessage("predicate is null");
            }
        }
    }

    @Nested
    class RotateTests {

        private java.util.List<Integer> rotatedLeft(Vector<Integer> vector, int n) {
            final java.util.List<Integer> list = new java.util.ArrayList<>(vector.toJavaList());
            if (!list.isEmpty()) {
                java.util.Collections.rotate(list, -Math.floorMod(n, list.size()));
            }
            return list;
        }

        private java.util.List<Integer> rotatedRight(Vector<Integer> vector, int n) {
            final java.util.List<Integer> list = new java.util.ArrayList<>(vector.toJavaList());
            if (!list.isEmpty()) {
                java.util.Collections.rotate(list, Math.floorMod(n, list.size()));
            }
            return list;
        }

        @Test
        public void shouldRotateByAnyDistanceAtEveryBoundary() {
            for (int n : BOUNDARIES) {
                for (Vector<Integer> vector : representations(n)) {
                    for (int distance : new int[] { Integer.MIN_VALUE, -2 * n - 3, -n - 1, -n, -33, -32, -31, -1, 0, 1, 31, 32, 33, n - 1, n, n + 1, 2 * n + 3, Integer.MAX_VALUE }) {
                        assertThat(vector.rotateLeft(distance).toJavaList()).isEqualTo(rotatedLeft(vector, distance));
                        assertThat(vector.rotateRight(distance).toJavaList()).isEqualTo(rotatedRight(vector, distance));
                        if (distance != Integer.MIN_VALUE) { // -MIN_VALUE overflows
                            assertThat(vector.rotateRight(distance)).isEqualTo(vector.rotateLeft(-distance));
                        }
                        if (n == 0 || Math.floorMod(distance, n) == 0) {
                            assertThat(vector.rotateLeft(distance)).isSameAs(vector);
                            assertThat(vector.rotateRight(distance)).isSameAs(vector);
                        } else {
                            assertThat(vector.rotateLeft(distance).rotateRight(distance)).isEqualTo(vector);
                        }
                    }
                }
            }
        }

        @Test
        public void shouldRotateTheOverflowingDistances() {
            final Vector<Integer> vector = of(1, 2, 3, 4, 5);
            assertThat(vector.rotateLeft(Integer.MIN_VALUE)).isEqualTo(of(3, 4, 5, 1, 2));
            assertThat(vector.rotateRight(Integer.MIN_VALUE)).isEqualTo(of(4, 5, 1, 2, 3));
            assertThat(vector.rotateLeft(Integer.MAX_VALUE)).isEqualTo(of(3, 4, 5, 1, 2));
            assertThat(vector.rotateRight(Integer.MAX_VALUE)).isEqualTo(of(4, 5, 1, 2, 3));
        }
    }

    @Nested
    class SortTests {
        @Test
        public void shouldSortAtEveryBoundary() {
            for (int n : BOUNDARIES) {
                for (Vector<Integer> vector : representations(n)) {
                    final Vector<Integer> shuffled = vector.shuffle();
                    final java.util.List<Integer> before = shuffled.toJavaList();
                    assertThat(shuffled.size()).isEqualTo(n);
                    assertThat(shuffled.sorted()).isEqualTo(vector);
                    assertThat(shuffled.sorted(Comparator.reverseOrder())).isEqualTo(vector.reverse());
                    assertThat(shuffled.sortBy(i -> -i)).isEqualTo(vector.reverse());
                    assertThat(shuffled.sortBy(Comparator.reverseOrder(), i -> -i)).isEqualTo(vector);
                    assertThat(shuffled.sorted().toJavaList()).isEqualTo(vector.toJavaList());
                    if (n <= 1) {
                        assertThat(shuffled).isSameAs(vector);
                    }
                    // the receiver is never touched: the sorts copy to an array, Arrays.sort that copy and regroup it
                    assertThat(shuffled.toJavaList()).isEqualTo(before);
                }
            }
        }

        @Test
        public void shouldLeaveTheReceiverUnchangedWhenTheComparatorThrows() {
            // three elements at least: two elements are sorted with one comparison, before the n / 2 threshold
            for (int n : new int[] { 3, 32, 33, 1025 }) {
                for (Vector<Integer> vector : representations(n)) {
                    final Vector<Integer> shuffled = vector.shuffle();
                    final java.util.List<Integer> before = shuffled.toJavaList();
                    final java.util.concurrent.atomic.AtomicInteger calls = new java.util.concurrent.atomic.AtomicInteger();
                    final Comparator<Integer> failing = (a, b) -> {
                        if (calls.incrementAndGet() > n / 2) {
                            throw new IllegalStateException("mid-sort");
                        }
                        return Integer.compare(a, b);
                    };
                    assertThatThrownBy(() -> shuffled.sorted(failing)).isInstanceOf(IllegalStateException.class).hasMessage("mid-sort");
                    assertThat(shuffled.toJavaList()).isEqualTo(before);
                    calls.set(0);
                    assertThatThrownBy(() -> shuffled.sortBy(failing, i -> i)).isInstanceOf(IllegalStateException.class).hasMessage("mid-sort");
                    assertThat(shuffled.toJavaList()).isEqualTo(before);
                    assertThat(shuffled.sorted()).isEqualTo(vector);
                }
            }
        }

        @Test
        public void shouldSortStablyAtEveryBoundary() {
            for (int n : BOUNDARIES) {
                for (Vector<Integer> vector : representations(n)) {
                    // equal keys keep their order: sorting by parity leaves the evens, then the odds, each in original order
                    final Vector<Integer> expected = vector.filter(i -> i % 2 == 0).appendAll(vector.filter(i -> i % 2 != 0));
                    assertThat(vector.sortBy(i -> i % 2)).isEqualTo(expected);
                    assertThat(vector.sorted(Comparator.comparingInt(i -> i % 2))).isEqualTo(expected);
                    assertThat(vector.sortBy(Comparator.reverseOrder(), i -> i % 2)).isEqualTo(vector.filter(i -> i % 2 != 0).appendAll(vector.filter(i -> i % 2 == 0)));
                    assertThat(vector.sortBy(i -> 0)).isEqualTo(vector);
                }
            }
        }

        @Test
        public void shouldReturnTheEmptyInstanceAndRejectNulls() {
            final Vector<Integer> empty = Vector.empty();
            assertThat(empty.sorted()).isSameAs(empty);
            assertThat(empty.sorted(Comparator.reverseOrder())).isSameAs(empty);
            assertThat(empty.sortBy(i -> i)).isSameAs(empty);
            assertThat(empty.sortBy(Comparator.reverseOrder(), i -> i)).isSameAs(empty);
            assertThatNullPointerException().isThrownBy(() -> of(1).sorted(null)).withMessage("comparator is null");
            assertThatNullPointerException().isThrownBy(() -> of(1).sortBy(null, i -> i)).withMessage("comparator is null");
            assertThatNullPointerException().isThrownBy(() -> of(1).sortBy(Comparator.<Integer> naturalOrder(), null)).withMessage("mapper is null");
            assertThatNullPointerException().isThrownBy(() -> of(1).sortBy((Function<Integer, Integer>) null)).withMessage("mapper is null");
            assertThatThrownBy(() -> Vector.of(new Object(), new Object()).sorted()).isInstanceOf(ClassCastException.class);
        }
    }

    @Nested
    class UnzipTests {
        @Test
        public void shouldUnzipAtEveryBoundary() {
            for (int n : BOUNDARIES) {
                for (Vector<Integer> vector : representations(n)) {
                    final Tuple2<Vector<Integer>, Vector<String>> pairs = vector.unzip(i -> Tuple.of(i, "s" + i));
                    assertThat(pairs._1()).isEqualTo(vector);
                    assertThat(pairs._2()).isEqualTo(vector.map(i -> "s" + i));
                    final Tuple3<Vector<Integer>, Vector<String>, Vector<Long>> triples = vector.unzip3(i -> Tuple.of(i, "s" + i, (long) i));
                    assertThat(triples._1()).isEqualTo(vector);
                    assertThat(triples._2()).isEqualTo(vector.map(i -> "s" + i));
                    assertThat(triples._3()).isEqualTo(vector.map(Integer::longValue));
                    if (n == 0) {
                        assertThat(pairs._1()).isSameAs(Vector.empty());
                        assertThat(pairs._2()).isSameAs(Vector.empty());
                        assertThat(triples._3()).isSameAs(Vector.empty());
                    }
                }
            }
        }

        @Test
        public void shouldRejectNulls() {
            assertThatNullPointerException().isThrownBy(() -> of(1).unzip(null)).withMessage("unzipper is null");
            assertThatNullPointerException().isThrownBy(() -> of(1).unzip3(null)).withMessage("unzipper is null");
            assertThatNullPointerException().isThrownBy(() -> of(1).unzip(i -> null));
            assertThatNullPointerException().isThrownBy(() -> of(1).unzip3(i -> null));
            assertThatNullPointerException().isThrownBy(() -> of(1).unzip(i -> Tuple.of(i, null)));
            assertThatNullPointerException().isThrownBy(() -> of(1).unzip3(i -> Tuple.of(i, i, null)));
        }
    }

    @Nested
    class JavaListViewTests {
        @Test
        public void shouldReadThroughTheImmutableViewAtEveryBoundary() {
            for (int n : BOUNDARIES) {
                for (Vector<Integer> vector : representations(n)) {
                    final java.util.List<Integer> view = vector.asJava();
                    assertThat(view.size()).isEqualTo(n);
                    assertThat(view.isEmpty()).isEqualTo(n == 0);
                    assertThat(new java.util.ArrayList<>(view)).isEqualTo(vector.toJavaList());
                    assertThat(view).isEqualTo(vector.toJavaList());
                    assertThat(view.hashCode()).isEqualTo(vector.toJavaList().hashCode());
                    for (int k : positions(n)) {
                        if (k < n) {
                            assertThat(view.get(k)).isEqualTo(k);
                            assertThat(view.indexOf(k)).isEqualTo(k);
                            assertThat(view.lastIndexOf(k)).isEqualTo(k);
                            assertThat(view.contains(k)).isTrue();
                        }
                        assertThat(view.subList(k, n)).isEqualTo(vector.drop(k).toJavaList());
                        assertThat(view.subList(0, k)).isEqualTo(vector.take(k).toJavaList());
                    }
                    assertThat(view.indexOf(-1)).isEqualTo(-1);
                    assertThat(view.lastIndexOf(n)).isEqualTo(-1);
                    assertThat(view.contains(n)).isFalse();
                    assertThat(view.containsAll(vector.take(3).toJavaList())).isTrue();
                    assertThatThrownBy(() -> view.get(n)).isInstanceOf(IndexOutOfBoundsException.class);
                    assertThatThrownBy(() -> view.add(1)).isInstanceOf(UnsupportedOperationException.class);
                    assertThatThrownBy(() -> view.add(0, 1)).isInstanceOf(UnsupportedOperationException.class);
                    assertThatThrownBy(() -> view.set(0, 1)).isInstanceOf(UnsupportedOperationException.class);
                    assertThatThrownBy(() -> view.remove(0)).isInstanceOf(UnsupportedOperationException.class);
                    if (n > 0) {
                        // sort and clear return early on an empty list, as java.util.Collections.unmodifiableList does
                        assertThatThrownBy(() -> view.sort(Comparator.reverseOrder())).isInstanceOf(UnsupportedOperationException.class);
                        assertThatThrownBy(view::clear).isInstanceOf(UnsupportedOperationException.class);
                    }
                    assertThat(Vector.ofAll(view)).isSameAs(vector);
                }
            }
        }

        @Test
        public void shouldWriteThroughTheMutableViewWithEveryOperation() {
            for (int n : new int[] { 1, 32, 33, 1025 }) {
                for (Vector<Integer> vector : representations(n)) {
                    final java.util.List<Integer> view = vector.asJavaMutable();
                    final java.util.List<Integer> reference = new java.util.ArrayList<>(vector.toJavaList());
                    view.add(-1);
                    reference.add(-1);
                    view.add(0, -2);
                    reference.add(0, -2);
                    view.addAll(java.util.List.of(-3, -4));
                    reference.addAll(java.util.List.of(-3, -4));
                    view.addAll(1, java.util.List.of(-5, -6));
                    reference.addAll(1, java.util.List.of(-5, -6));
                    assertThat(view.set(1, -7)).isEqualTo(-5);
                    reference.set(1, -7);
                    assertThat(view.remove(Integer.valueOf(-6))).isTrue();
                    reference.remove(Integer.valueOf(-6));
                    assertThat(view.remove(Integer.valueOf(-99))).isFalse();
                    assertThat(view.remove(0)).isEqualTo(-2);
                    reference.remove(0);
                    assertThat(view.removeAll(java.util.List.of(-3, -99))).isTrue();
                    reference.removeAll(java.util.List.of(-3, -99));
                    assertThat(view.retainAll(reference)).isFalse();
                    view.sort(Comparator.reverseOrder());
                    reference.sort(Comparator.reverseOrder());
                    assertThat(view).isEqualTo(reference);
                    assertThat(view.subList(1, view.size())).isEqualTo(reference.subList(1, reference.size()));
                    final java.util.Iterator<Integer> iterator = view.iterator();
                    iterator.next();
                    iterator.remove();
                    reference.remove(0);
                    assertThat(view).isEqualTo(reference);
                    assertThat(vector.size()).isEqualTo(n); // the Vector itself never changes
                    view.clear();
                    assertThat(view).isEmpty();
                    assertThat(view.retainAll(java.util.List.of(1))).isFalse();
                }
            }
        }

        @Test
        public void shouldPassTheViewToTheActionAndReturnTheVectorItHolds() {
            final Vector<Integer> vector = of(1, 2, 3);
            final java.util.List<java.util.List<Integer>> seen = new java.util.ArrayList<>();
            assertThat(vector.asJava(seen::add)).isSameAs(vector);
            assertThat(seen.get(0)).isEqualTo(java.util.List.of(1, 2, 3));
            assertThatThrownBy(() -> seen.get(0).add(4)).isInstanceOf(UnsupportedOperationException.class);
            assertThat(vector.asJavaMutable(list -> { })).isSameAs(vector);
            final Vector<Integer> modified = vector.asJavaMutable(list -> { list.add(4); list.remove(0); });
            assertThat(modified).isEqualTo(of(2, 3, 4));
            assertThat(vector).isEqualTo(of(1, 2, 3));
            assertThatNullPointerException().isThrownBy(() -> vector.asJava(null)).withMessage("action is null");
            assertThatNullPointerException().isThrownBy(() -> vector.asJavaMutable(null)).withMessage("action is null");
        }
    }

    @Nested
    class IteratorFromIndexTests {
        @Test
        public void shouldIterateFromAnyValidIndexAtEveryBoundary() {
            for (int n : BOUNDARIES) {
                for (Vector<Integer> vector : representations(n)) {
                    for (int k : positions(n)) {
                        assertThat(vector.iterator(k).toList()).isEqualTo(vector.drop(k).toList());
                    }
                    assertThat(vector.iterator(n).hasNext()).isFalse();
                    assertThatThrownBy(() -> vector.iterator(n + 1)).isInstanceOf(IndexOutOfBoundsException.class);
                    assertThatThrownBy(() -> vector.iterator(-1)).isInstanceOf(IndexOutOfBoundsException.class);
                }
            }
        }
    }

    @Nested
    class EqualityAcrossSequenceTypesTests {
        @Test
        public void shouldEqualEveryOrderedSequenceWithTheSameElementsInBothDirections() {
            final Vector<Integer> vector = of(1, 2, 3);
            for (Traversable<Integer> other : java.util.List.of(Vector.of(1, 2, 3), List.of(1, 2, 3), Queue.of(1, 2, 3), Stream.of(1, 2, 3))) {
                assertThat(vector.equals(other)).isTrue();
                assertThat(other.equals(vector)).isTrue();
                assertThat(vector.hashCode()).isEqualTo(other.hashCode());
            }
            for (Object other : java.util.List.of(List.of(3, 2, 1), List.of(1, 2), HashSet.of(1, 2, 3), LinkedHashSet.of(1, 2, 3), java.util.List.of(1, 2, 3), "Vector(1, 2, 3)")) {
                assertThat(vector.equals(other)).isFalse();
                assertThat(other.equals(vector)).isFalse();
            }
            assertThat(Vector.empty().equals(List.empty())).isTrue();
            assertThat(List.empty().equals(Vector.empty())).isTrue();
            assertThat(Vector.range(0, 1025).equals(List.range(0, 1025))).isTrue();
            assertThat(List.range(0, 1025).equals(Vector.range(0, 1025))).isTrue();
            assertThat(Vector.range(0, 1025).equals(Vector.ofAll(Vector.range(0, 1025).toJavaList()))).isTrue();
        }
    }

    @Nested
    class InsertRemoveUpdateBoundaryTests {
        @Test
        public void shouldInsertRemoveAndUpdateAtEveryBoundaryPosition() {
            for (int n : new int[] { 1, 31, 32, 33, 1023, 1024, 1025 }) {
                for (Vector<Integer> vector : representations(n)) {
                    for (int i : positions(n)) {
                        final java.util.List<Integer> reference = new java.util.ArrayList<>(vector.toJavaList());
                        reference.add(i, -1);
                        assertThat(vector.insert(i, -1).toJavaList()).isEqualTo(reference);
                        reference.add(i + 1, -2);
                        assertThat(vector.insertAll(i, Vector.of(-1, -2)).toJavaList()).isEqualTo(reference);
                        assertThat(vector.insertAll(i, java.util.List.of(-1, -2)).toJavaList()).isEqualTo(reference);
                        assertThat(vector.insertAll(i, Iterator.of(-1, -2)).toJavaList()).isEqualTo(reference);
                        assertThat(vector.insertAll(i, Vector.empty())).isEqualTo(vector);
                        if (i < n) {
                            final java.util.List<Integer> removed = new java.util.ArrayList<>(vector.toJavaList());
                            removed.remove(i);
                            assertThat(vector.removeAt(i).toJavaList()).isEqualTo(removed);
                            assertThat(vector.remove(i).toJavaList()).isEqualTo(removed);
                            assertThat(vector.removeFirst(e -> e == i).toJavaList()).isEqualTo(removed);
                            assertThat(vector.removeLast(e -> e == i).toJavaList()).isEqualTo(removed);
                            final java.util.List<Integer> updated = new java.util.ArrayList<>(vector.toJavaList());
                            updated.set(i, -1);
                            assertThat(vector.update(i, -1).toJavaList()).isEqualTo(updated);
                            assertThat(vector.update(i, e -> -1).toJavaList()).isEqualTo(updated);
                            assertThat(vector.replace(i, -1).toJavaList()).isEqualTo(updated);
                            assertThat(vector.replaceAll(i, -1).toJavaList()).isEqualTo(updated);
                            assertThat(vector.patch(i, Vector.of(-1), 1).toJavaList()).isEqualTo(updated);
                        }
                    }
                    assertThatThrownBy(() -> vector.insert(n + 1, 0)).isInstanceOf(IndexOutOfBoundsException.class);
                    assertThatThrownBy(() -> vector.insert(-1, 0)).isInstanceOf(IndexOutOfBoundsException.class);
                    assertThatThrownBy(() -> vector.removeAt(n)).isInstanceOf(IndexOutOfBoundsException.class);
                    assertThatThrownBy(() -> vector.update(n, 0)).isInstanceOf(IndexOutOfBoundsException.class);
                    assertThatNullPointerException().isThrownBy(() -> vector.insert(0, null));
                    assertThatNullPointerException().isThrownBy(() -> vector.update(0, (Integer) null));
                    assertThatNullPointerException().isThrownBy(() -> vector.update(0, e -> null));
                    assertThat(vector.remove(n)).isSameAs(vector);
                    assertThat(vector.replace(n, -1)).isSameAs(vector);
                    assertThat(vector.replaceAll(n, -1)).isSameAs(vector);
                }
            }
        }
    }

    @Nested
    class ScanTests {
        @Test
        public void shouldScanFromBothEndsAtEveryBoundary() {
            for (int n : BOUNDARIES) {
                for (Vector<Integer> vector : representations(n)) {
                    final Vector<Integer> left = vector.scanLeft(0, Integer::sum);
                    final Vector<Integer> right = vector.scanRight(0, Integer::sum);
                    assertThat(left.size()).isEqualTo(n + 1);
                    assertThat(right.size()).isEqualTo(n + 1);
                    assertThat(left.head()).isEqualTo(0);
                    assertThat(right.last()).isEqualTo(0);
                    assertThat(left.last()).isEqualTo(n * (n - 1) / 2);
                    assertThat(right.head()).isEqualTo(n * (n - 1) / 2);
                    for (int k : positions(n)) {
                        assertThat(left.get(k)).isEqualTo(k * (k - 1) / 2);
                        assertThat(right.get(k)).isEqualTo(n * (n - 1) / 2 - k * (k - 1) / 2);
                    }
                    assertThat(vector.scan(0, Integer::sum)).isEqualTo(left);
                }
            }
        }
    }

    @Nested
    class PaddingTests {
        @Test
        public void shouldPadOnBothSidesAtEveryBoundary() {
            for (int n : BOUNDARIES) {
                for (Vector<Integer> vector : representations(n)) {
                    for (int extra : new int[] { 1, 31, 32, 33 }) {
                        assertThat(vector.padTo(n + extra, -1)).isEqualTo(vector.appendAll(Vector.fill(extra, -1)));
                        assertThat(vector.leftPadTo(n + extra, -1)).isEqualTo(Vector.fill(extra, -1).appendAll(vector));
                    }
                    assertThat(vector.padTo(n, -1)).isSameAs(vector);
                    assertThat(vector.padTo(n - 1, -1)).isSameAs(vector);
                    assertThat(vector.leftPadTo(n, -1)).isSameAs(vector);
                    assertThat(vector.leftPadTo(-1, -1)).isSameAs(vector);
                    assertThat(vector.intersperse(-1).size()).isEqualTo(Math.max(2 * n - 1, 0));
                    if (n > 1) {
                        assertThat(vector.intersperse(-1).get(1)).isEqualTo(-1);
                        assertThat(vector.intersperse(-1).last()).isEqualTo(n - 1);
                    }
                }
            }
        }
    }

    @Nested
    class CombinatoricsTests {
        @Test
        public void shouldProduceVectorsOfVectors() {
            for (Vector<Integer> vector : representations(4)) {
                final Vector<Vector<Integer>> permutations = vector.permutations();
                assertThat(permutations.size()).isEqualTo(24);
                assertThat(permutations.distinct().size()).isEqualTo(24);
                assertThat(permutations.head()).isEqualTo(vector);
                assertThat(permutations.last()).isEqualTo(vector.reverse());
                assertThat(vector.combinations().size()).isEqualTo(16);
                assertThat(vector.combinations(2)).isEqualTo(Vector.of(of(0, 1), of(0, 2), of(0, 3), of(1, 2), of(1, 3), of(2, 3)));
                assertThat(vector.combinations(5)).isEmpty();
                assertThat(vector.combinations(0)).isEqualTo(Vector.of(Vector.empty()));
            }
            for (Vector<Integer> vector : representations(33)) {
                assertThat(vector.combinations(2).size()).isEqualTo(33 * 32 / 2);
                assertThat(vector.combinations(1)).isEqualTo(vector.map(Vector::of));
            }
        }
    }
}
