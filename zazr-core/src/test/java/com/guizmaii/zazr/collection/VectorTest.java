package com.guizmaii.zazr.collection;

import com.guizmaii.zazr.Tuple;
import com.guizmaii.zazr.Tuple2;
import com.guizmaii.zazr.collection.JavaConverters.ChangePolicy;
import com.guizmaii.zazr.collection.JavaConverters.ListView;
import com.guizmaii.zazr.control.Either;
import com.guizmaii.zazr.control.Option;
import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class VectorTest extends AbstractIndexedSeqTest {

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

    @Override
    protected <T> Traversable<T> fill(int n, T element) {
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

    @Override
    @SuppressWarnings("unchecked")
    protected <T> Vector<Vector<T>> transpose(Seq<? extends Seq<T>> rows) {
        return Vector.transpose((Vector<Vector<T>>) rows);
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
    public void shouldDropRightWhileNoneOnNil() {
        assertThat(empty().dropRightWhile(ignored -> true)).isEqualTo(empty());
    }

    @Test
    public void shouldDropRightWhileNoneIfPredicateIsFalse() {
        assertThat(of(1, 2, 3).dropRightWhile(ignored -> false)).isEqualTo(of(1, 2, 3));
    }

    @Test
    public void shouldDropRightWhileAllIfPredicateIsTrue() {
        assertThat(of(1, 2, 3).dropRightWhile(ignored -> true)).isEqualTo(empty());
    }

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

    // partitionMap, duplicates, duplicatesBy, flatten and nonEmpty, at the empty/1/32/33 boundaries and on both leaf representations

    static java.util.List<Vector<Integer>> bothRepresentations(int n) {
        final Vector<Integer> primitive = Vector.range(0, n);
        return java.util.List.of(primitive, Vector.ofAll(primitive.toJavaList()));
    }

    @Nested
    class PartitionMapTests {

        @Test
        public void shouldPartitionMapLikePartitionAtEveryBoundary() {
            for (int n : new int[] { 0, 1, 32, 33 }) {
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
            for (int n : new int[] { 0, 1, 32, 33 }) {
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
        public void shouldFindDuplicatesAtEveryBoundary() {
            for (int n : new int[] { 0, 1, 32, 33 }) {
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
            for (int n : new int[] { 0, 1, 32, 33 }) {
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
    class ToNonEmptyVectorTests {

        @Test
        public void shouldNarrowToNonEmptyVector() {
            assertThat(Vector.<Integer> empty().toNonEmptyVector()).isEqualTo(Option.none());
            for (int n : new int[] { 1, 32, 33 }) {
                for (Vector<Integer> vector : bothRepresentations(n)) {
                    final Option<NonEmptyVector<Integer>> actual = vector.toNonEmptyVector();
                    assertThat(actual.isDefined()).isTrue();
                    assertThat(actual.get().toVector()).isSameAs(vector);
                    assertThat(actual.get().size()).isEqualTo(n);
                }
            }
        }
    }
}
