package com.guizmaii.zazr.collection;

import com.guizmaii.zazr.Tuple;
import com.guizmaii.zazr.control.Either;
import com.guizmaii.zazr.control.Option;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Spliterator;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Every method is checked at sizes 1, 2, 31, 32, 33, 1023, 1024 and 1025 (the boundaries of the 32-wide trie) against
 * the equivalent {@link HashSet} call, and on elements whose hashes collide, which the trie keeps in collision leaves.
 */
public class NonEmptySetTest {

    static final int[] SIZES = { 1, 2, 31, 32, 33, 1023, 1024, 1025 };

    /* (size, set) */
    static Stream<Arguments> sets() {
        final ArrayList<Arguments> cases = new ArrayList<>();
        for (int n : SIZES) {
            cases.add(Arguments.of(n, HashSet.range(0, n)));
        }
        return cases.stream();
    }

    /* an element whose hash is shared by four values, so that sets of them hold collision leaves */
    record Colliding(int value) implements Comparable<Colliding> {
        @Override
        public int hashCode() { return value >> 2; }

        @Override
        public int compareTo(Colliding that) { return Integer.compare(value, that.value); }
    }

    static HashSet<Colliding> colliding(int n) {
        return HashSet.range(0, n).map(Colliding::new);
    }

    static <A> NonEmptySet<A> nes(HashSet<A> set) {
        return NonEmptySet.unsafeFromSet(set);
    }

    /* a one-shot iterable: a second iterator() throws, so an operation that reads its argument twice fails */
    static <A> Iterable<A> once(Iterable<A> elements) {
        final boolean[] read = { false };
        return () -> {
            if (read[0]) {
                throw new IllegalStateException("read twice");
            }
            read[0] = true;
            return elements.iterator();
        };
    }

    @Nested
    class Constructors {

        @Test
        public void shouldBuildOfHeadAndVarargsTail() {
            assertThat(NonEmptySet.of(1).toSet()).isEqualTo(HashSet.of(1));
            assertThat(NonEmptySet.of(1, 2, 1).toSet()).isEqualTo(HashSet.of(1, 2));
            for (int n : SIZES) {
                final Integer[] tail = Vector.range(1, n).toArray(Integer[]::new);
                assertThat(NonEmptySet.of(0, tail).toSet()).isEqualTo(HashSet.range(0, n));
                assertThat(NonEmptySet.of(0, tail).size()).isEqualTo(n);
                assertThat(NonEmptySet.of(0, Vector.range(0, n).toArray(Integer[]::new)).size()).isEqualTo(n);
            }
        }

        @Test
        public void shouldBuildFromHeadAndIterableTail() {
            for (int n : SIZES) {
                final HashSet<Integer> expected = HashSet.range(0, n);
                assertThat(NonEmptySet.fromIterable(0, HashSet.range(1, n)).toSet()).isEqualTo(expected);
                assertThat(NonEmptySet.fromIterable(0, once(Vector.range(0, n))).toSet()).isEqualTo(expected);
                assertThat(NonEmptySet.fromIterable(0, List.<Integer> empty()).toSet()).isEqualTo(HashSet.of(0));
                assertThat(NonEmptySet.fromIterable(new Colliding(0), once(colliding(n))).toSet()).isEqualTo(colliding(n));
            }
        }

        @Test
        public void shouldTakeIterablesAsElementsNotAsTail() {
            final NonEmptySet<Integer> x = NonEmptySet.of(1);
            final NonEmptySet<Integer> y = NonEmptySet.of(2, 3);
            assertThat(NonEmptySet.of(x, y).size()).isEqualTo(2);
            assertThat(NonEmptySet.of(java.util.List.of(1), java.util.List.of(2, 3)).size()).isEqualTo(2);
            assertThat(NonEmptySet.fromIterable(x, Vector.of(y)).size()).isEqualTo(2);
        }

        @Test
        public void shouldBuildSingle() {
            assertThat(NonEmptySet.single("a").toSet()).isEqualTo(HashSet.of("a"));
            assertThat(NonEmptySet.single("a").size()).isEqualTo(1);
        }

        @Test
        public void shouldWrapANonEmptySetWithoutCopying() {
            for (int n : SIZES) {
                final HashSet<Integer> set = HashSet.range(0, n);
                assertThat(NonEmptySet.fromSet(set).get().toSet()).isSameAs(set);
                assertThat(NonEmptySet.unsafeFromSet(set).toSet()).isSameAs(set);
                assertThat(set.toNonEmptySet().get().toSet()).isSameAs(set);
                assertThat(NonEmptySet.fromIterable(set).get().toSet()).isSameAs(set);
            }
        }

        @Test
        public void shouldReturnNoneFromEmptyInput() {
            assertThat(NonEmptySet.fromSet(HashSet.<Integer> empty())).isEqualTo(Option.none());
            assertThat(HashSet.<Integer> empty().toNonEmptySet()).isEqualTo(Option.none());
            assertThat(NonEmptySet.fromIterable(HashSet.<Integer> empty())).isEqualTo(Option.none());
            assertThat(NonEmptySet.fromIterable(java.util.List.<Integer> of())).isEqualTo(Option.none());
            assertThat(NonEmptySet.fromIterable(once(List.<Integer> empty()))).isEqualTo(Option.none());
        }

        @Test
        public void shouldCopyANonEmptyIterable() {
            for (int n : SIZES) {
                final HashSet<Integer> expected = HashSet.range(0, n);
                assertThat(NonEmptySet.fromIterable(once(Vector.range(0, n).appendAll(Vector.range(0, n)))).get().toSet()).isEqualTo(expected);
                assertThat(NonEmptySet.fromIterable(new java.util.ArrayList<>(expected.asJava())).get().toSet()).isEqualTo(expected);
                assertThat(NonEmptySet.fromIterable(colliding(n).toList()).get().toSet()).isEqualTo(colliding(n));
            }
        }

        @Test
        public void shouldThrowOnUnsafeFromEmptySet() {
            assertThatIllegalArgumentException()
              .isThrownBy(() -> NonEmptySet.unsafeFromSet(HashSet.empty()))
              .withMessage("NonEmptySet.unsafeFromSet: set is empty");
        }

        @Test
        public void shouldRejectNullsNamingTheType() {
            assertThatNullPointerException().isThrownBy(() -> NonEmptySet.of(null)).withMessage("NonEmptySet: head is null");
            assertThatNullPointerException().isThrownBy(() -> NonEmptySet.of(1, (Integer[]) null)).withMessage("NonEmptySet: tail is null");
            assertThatNullPointerException().isThrownBy(() -> NonEmptySet.of(1, 2, null)).withMessage("NonEmptySet: element is null");
            assertThatNullPointerException().isThrownBy(() -> NonEmptySet.fromIterable(null, Vector.of(1))).withMessage("NonEmptySet: head is null");
            assertThatNullPointerException().isThrownBy(() -> NonEmptySet.fromIterable(1, (Iterable<Integer>) null)).withMessage("NonEmptySet: tail is null");
            assertThatNullPointerException().isThrownBy(() -> NonEmptySet.fromIterable(1, Arrays.asList(2, null))).withMessage("NonEmptySet: element is null");
            assertThatNullPointerException().isThrownBy(() -> NonEmptySet.single(null)).withMessage("NonEmptySet: element is null");
            assertThatNullPointerException().isThrownBy(() -> NonEmptySet.fromSet(null)).withMessage("NonEmptySet.fromSet: set is null");
            assertThatNullPointerException().isThrownBy(() -> NonEmptySet.fromIterable(null)).withMessage("NonEmptySet.fromIterable: iterable is null");
            assertThatNullPointerException().isThrownBy(() -> NonEmptySet.fromIterable(Arrays.asList(1, null))).withMessage("NonEmptySet: element is null");
            assertThatNullPointerException().isThrownBy(() -> NonEmptySet.unsafeFromSet(null)).withMessage("NonEmptySet.unsafeFromSet: set is null");
            assertThatNullPointerException().isThrownBy(() -> NonEmptySet.flatten(null)).withMessage("nested is null");
        }

        @Test
        public void shouldRejectNullArguments() {
            final NonEmptySet<Integer> nes = NonEmptySet.of(1, 2);
            assertThatNullPointerException().isThrownBy(() -> nes.add(null)).withMessage("NonEmptySet.add: element is null");
            assertThatNullPointerException().isThrownBy(() -> nes.replace(null, 1)).withMessage("NonEmptySet.replace: currentElement is null");
            assertThatNullPointerException().isThrownBy(() -> nes.replace(3, null)).withMessage("NonEmptySet.replace: newElement is null");
            assertThatNullPointerException().isThrownBy(() -> nes.replaceAll(3, null)).withMessage("NonEmptySet.replace: newElement is null");
            assertThatNullPointerException().isThrownBy(() -> nes.as(null)).withMessage("NonEmptySet.as: value is null");
            assertThatNullPointerException().isThrownBy(() -> nes.addAll(Arrays.asList(3, null)));
            assertThatNullPointerException().isThrownBy(() -> nes.addAll(null));
            assertThatNullPointerException().isThrownBy(() -> nes.union(null));
            assertThatNullPointerException().isThrownBy(() -> nes.map(i -> null));
            assertThatNullPointerException().isThrownBy(() -> nes.map(null));
            assertThatNullPointerException().isThrownBy(() -> nes.flatMap(null));
            assertThatNullPointerException().isThrownBy(() -> nes.flatMap(i -> null)).withMessage("NonEmptySet.flatMap: mapper returned null");
            assertThatNullPointerException().isThrownBy(() -> nes.tap(null));
            assertThatNullPointerException().isThrownBy(() -> nes.groupBy(null));
            assertThatNullPointerException().isThrownBy(() -> nes.groupBy(i -> null));
            assertThatNullPointerException().isThrownBy(() -> nes.maxBy((Comparator<Integer>) null));
            assertThatNullPointerException().isThrownBy(() -> nes.minBy((Comparator<Integer>) null));
            assertThatNullPointerException().isThrownBy(() -> nes.maxBy((Function<Integer, Integer>) null));
            assertThatNullPointerException().isThrownBy(() -> nes.minBy((Function<Integer, Integer>) null));
            assertThatNullPointerException().isThrownBy(() -> nes.reduce(null));
            assertThatNullPointerException().isThrownBy(() -> nes.reduceMap(null, Integer::sum));
            assertThatNullPointerException().isThrownBy(() -> nes.reduceMap(i -> i, null));
            assertThatNullPointerException().isThrownBy(() -> nes.fold(0, null));
        }
    }

    @Nested
    class ReturnsNonEmptySet {

        @ParameterizedTest
        @MethodSource("com.guizmaii.zazr.collection.NonEmptySetTest#sets")
        public void shouldAddAndKeepTheOriginal(int n, HashSet<Integer> set) {
            final NonEmptySet<Integer> nes = nes(set);
            assertThat(nes.add(-1).toSet()).isEqualTo(set.add(-1));
            assertThat(nes.add(-1).size()).isEqualTo(n + 1);
            assertThat(nes.add(0)).isSameAs(nes);
            assertThat(nes.toSet()).isSameAs(set);
            assertThat(nes.size()).isEqualTo(n);
        }

        @ParameterizedTest
        @MethodSource("com.guizmaii.zazr.collection.NonEmptySetTest#sets")
        public void shouldAddAllAndUnion(int n, HashSet<Integer> set) {
            final NonEmptySet<Integer> nes = nes(set);
            for (int m : new int[] { 0, 1, 32, 33, 1025 }) {
                final HashSet<Integer> that = HashSet.range(n / 2, n / 2 + m);
                assertThat(nes.addAll(that).toSet()).isEqualTo(set.addAll(that));
                assertThat(nes.addAll(once(that.toVector())).toSet()).isEqualTo(set.addAll(that));
                assertThat(nes.union(that).toSet()).isEqualTo(set.union(that));
                assertThat(nes.union(that.toSortedSet()).toSet()).isEqualTo(set.union(that));
            }
            assertThat(nes.addAll(nes(HashSet.of(-1))).toSet()).isEqualTo(set.add(-1));
            assertThat(nes.addAll(HashSet.empty())).isSameAs(nes);
            assertThat(nes.union(HashSet.empty())).isSameAs(nes);
            assertThat(nes.addAll(set)).isSameAs(nes);
        }

        @ParameterizedTest
        @MethodSource("com.guizmaii.zazr.collection.NonEmptySetTest#sets")
        public void shouldMapCollapsingEqualResults(int n, HashSet<Integer> set) {
            final NonEmptySet<Integer> nes = nes(set);
            assertThat(nes.map(i -> i * 2).toSet()).isEqualTo(set.map(i -> i * 2));
            assertThat(nes.map(i -> i * 2).size()).isEqualTo(n);
            assertThat(nes.map(i -> 0).toSet()).isEqualTo(HashSet.of(0));
            assertThat(nes.map(i -> i % 3).size()).isEqualTo(Math.min(n, 3));
            assertThat(nes.as("x").toSet()).isEqualTo(HashSet.of("x"));
        }

        @ParameterizedTest
        @MethodSource("com.guizmaii.zazr.collection.NonEmptySetTest#sets")
        public void shouldFlatMapToNonEmptySets(int n, HashSet<Integer> set) {
            final NonEmptySet<Integer> nes = nes(set);
            assertThat(nes.flatMap(i -> NonEmptySet.of(i, -i - 1)).toSet()).isEqualTo(set.flatMap(i -> HashSet.of(i, -i - 1)));
            assertThat(nes.flatMap(i -> NonEmptySet.of(i, -i - 1)).size()).isEqualTo(2 * n);
            assertThat(nes.flatMap(i -> NonEmptySet.of(0)).toSet()).isEqualTo(HashSet.of(0));
            assertThat(nes.flatMap(i -> nes(HashSet.range(i, i + 33))).toSet()).isEqualTo(set.flatMap(i -> HashSet.range(i, i + 33)));
        }

        @ParameterizedTest
        @MethodSource("com.guizmaii.zazr.collection.NonEmptySetTest#sets")
        public void shouldReplace(int n, HashSet<Integer> set) {
            final NonEmptySet<Integer> nes = nes(set);
            assertThat(nes.replace(0, -1).toSet()).isEqualTo(set.replace(0, -1));
            assertThat(nes.replace(0, -1).size()).isEqualTo(n);
            assertThat(nes.replaceAll(0, -1).toSet()).isEqualTo(set.replaceAll(0, -1));
            assertThat(nes.replace(-5, -1)).isSameAs(nes);
            assertThat(nes.replace(0, 0)).isEqualTo(nes);
            // replacing by an element already present shrinks the set, never below one
            assertThat(nes.replace(0, n - 1).size()).isEqualTo(Math.max(1, n - 1));
        }

        @ParameterizedTest
        @MethodSource("com.guizmaii.zazr.collection.NonEmptySetTest#sets")
        public void shouldTapEveryElementAndReturnItself(int n, HashSet<Integer> set) {
            final NonEmptySet<Integer> nes = nes(set);
            final java.util.HashSet<Integer> seen = new java.util.HashSet<>();
            assertThat(nes.tap(seen::add)).isSameAs(nes);
            assertThat(seen).isEqualTo(set.asJava());
        }

        @ParameterizedTest
        @MethodSource("com.guizmaii.zazr.collection.NonEmptySetTest#sets")
        public void shouldGroupByIntoNonEmptyGroups(int n, HashSet<Integer> set) {
            final NonEmptySet<Integer> nes = nes(set);
            for (Function<Integer, Integer> classifier : java.util.List.<Function<Integer, Integer>> of(i -> 0, i -> i % 3, i -> i)) {
                final HashMap<Integer, NonEmptySet<Integer>> groups = nes.groupBy(classifier);
                assertThat(groups.mapValues(NonEmptySet::toSet)).isEqualTo(set.groupBy(classifier));
                assertThat(groups.values().forAll(group -> group.size() >= 1)).isTrue();
            }
        }
    }

    @Nested
    class ReturnsHashSet {

        @ParameterizedTest
        @MethodSource("com.guizmaii.zazr.collection.NonEmptySetTest#sets")
        public void shouldFilterRejectAndCollect(int n, HashSet<Integer> set) {
            final NonEmptySet<Integer> nes = nes(set);
            assertThat(nes.filter(i -> i % 2 == 0)).isEqualTo(set.filter(i -> i % 2 == 0));
            assertThat(nes.filter(i -> false)).isEmpty();
            assertThat(nes.reject(i -> i % 2 == 0)).isEqualTo(set.reject(i -> i % 2 == 0));
            assertThat(nes.reject(i -> true)).isEmpty();
            assertThat(nes.collect(i -> i % 2 == 0 ? Option.some(-i) : Option.none())).isEqualTo(set.collect(i -> i % 2 == 0 ? Option.some(-i) : Option.none()));
            assertThat(nes.collect(i -> Option.none())).isEmpty();
            assertThat(nes.flatMapAll(i -> i % 2 == 0 ? List.of(i, -i - 1) : List.<Integer> empty())).isEqualTo(set.flatMap(i -> i % 2 == 0 ? List.of(i, -i - 1) : List.<Integer> empty()));
            assertThat(nes.flatMapAll(i -> List.<Integer> empty())).isEmpty();
        }

        @ParameterizedTest
        @MethodSource("com.guizmaii.zazr.collection.NonEmptySetTest#sets")
        public void shouldRemoveDownToEmpty(int n, HashSet<Integer> set) {
            final NonEmptySet<Integer> nes = nes(set);
            assertThat(nes.remove(0)).isEqualTo(set.remove(0));
            assertThat(nes.remove(0).size()).isEqualTo(n - 1);
            assertThat(nes.remove(-1)).isEqualTo(set);
            assertThat(nes.removeAll(set)).isEmpty();
            assertThat(nes.removeAll(once(Vector.range(0, n / 2)))).isEqualTo(set.removeAll(Vector.range(0, n / 2)));
            assertThat(nes.retainAll(once(Vector.range(0, n / 2)))).isEqualTo(set.retainAll(Vector.range(0, n / 2)));
            assertThat(nes.retainAll(List.empty())).isEmpty();
            assertThat(nes.removeAll(nes)).isEmpty();
            assertThat(nes.retainAll(nes)).isEqualTo(set);
        }

        @ParameterizedTest
        @MethodSource("com.guizmaii.zazr.collection.NonEmptySetTest#sets")
        public void shouldIntersectAndDiff(int n, HashSet<Integer> set) {
            final NonEmptySet<Integer> nes = nes(set);
            for (int m : new int[] { 0, 1, 32, 33, 1025 }) {
                final HashSet<Integer> that = HashSet.range(n / 2, n / 2 + m);
                assertThat(nes.intersect(that)).isEqualTo(set.intersect(that));
                assertThat(nes.diff(that)).isEqualTo(set.diff(that));
                assertThat(nes.intersect(that.toSortedSet())).isEqualTo(set.intersect(that));
            }
            assertThat(nes.intersect(HashSet.of(-1))).isEmpty();
            assertThat(nes.diff(set)).isEmpty();
        }

        @ParameterizedTest
        @MethodSource("com.guizmaii.zazr.collection.NonEmptySetTest#sets")
        public void shouldPartition(int n, HashSet<Integer> set) {
            final NonEmptySet<Integer> nes = nes(set);
            assertThat(nes.partition(i -> i % 2 == 0)).isEqualTo(set.partition(i -> i % 2 == 0));
            assertThat(nes.partition(i -> true)._2()).isEmpty();
            assertThat(nes.partition(i -> false)._1()).isEmpty();
            final Function<Integer, Either<String, Integer>> f = i -> i % 3 == 0 ? Either.left("x" + i) : Either.right(i);
            assertThat(nes.partitionMap(f)).isEqualTo(set.partitionMap(f));
            assertThat(nes.partitionMap(i -> Either.<String, Integer> right(i))._1()).isEmpty();
        }
    }

    @Nested
    class Total {

        @ParameterizedTest
        @MethodSource("com.guizmaii.zazr.collection.NonEmptySetTest#sets")
        public void shouldReturnMaxAndMin(int n, HashSet<Integer> set) {
            final NonEmptySet<Integer> nes = nes(set);
            assertThat(nes.max()).isEqualTo(set.max().get()).isEqualTo(n - 1);
            assertThat(nes.min()).isEqualTo(set.min().get()).isEqualTo(0);
            assertThat(nes.maxBy(Comparator.reverseOrder())).isEqualTo(0);
            assertThat(nes.minBy(Comparator.reverseOrder())).isEqualTo(n - 1);
            assertThat(nes.maxBy(i -> -i)).isEqualTo(0);
            assertThat(nes.minBy(i -> -i)).isEqualTo(n - 1);
            // ties go to the first element in iteration order, as on HashSet
            assertThat(nes.maxBy(i -> 0)).isEqualTo(set.maxBy(i -> 0).get());
            assertThat(nes.minBy(i -> 0)).isEqualTo(set.minBy(i -> 0).get());
            assertThat(nes.maxBy((a, b) -> 0)).isEqualTo(set.maxBy((a, b) -> 0).get());
            assertThat(nes.minBy((a, b) -> 0)).isEqualTo(set.minBy((a, b) -> 0).get());
        }

        @Test
        public void shouldTreatNaNAsHashSetDoes() {
            final NonEmptySet<Double> doubles = NonEmptySet.of(1.0, Double.NaN, -1.0);
            assertThat(doubles.min()).isEqualTo(doubles.toSet().min().get()).isNaN();
            assertThat(doubles.max()).isEqualTo(doubles.toSet().max().get()).isNaN();
            final NonEmptySet<Float> floats = NonEmptySet.of(1.0f, Float.NaN);
            assertThat(floats.min()).isEqualTo(floats.toSet().min().get()).isNaN();
            assertThatThrownBy(() -> NonEmptySet.of(new Object(), new Object()).max()).isInstanceOf(ClassCastException.class);
        }

        @ParameterizedTest
        @MethodSource("com.guizmaii.zazr.collection.NonEmptySetTest#sets")
        public void shouldReduceAndFold(int n, HashSet<Integer> set) {
            final NonEmptySet<Integer> nes = nes(set);
            assertThat(nes.reduce(Integer::sum)).isEqualTo(set.reduce(Integer::sum)).isEqualTo(n * (n - 1) / 2);
            assertThat(nes.reduceMap(i -> (long) i, Long::sum)).isEqualTo((long) n * (n - 1) / 2);
            assertThat(nes.fold(0, Integer::sum)).isEqualTo(n * (n - 1) / 2);
            assertThat(nes.foldLeft("", (s, i) -> s + i).length()).isEqualTo(set.foldLeft("", (s, i) -> s + i).length());
            assertThat(nes.sum()).isEqualTo(set.sum());
            assertThat(nes.product()).isEqualTo(set.product());
            assertThat(nes.average()).isEqualTo(set.average().get());
            assertThatThrownBy(() -> NonEmptySet.of("a").average()).isInstanceOf(UnsupportedOperationException.class);
        }

        @ParameterizedTest
        @MethodSource("com.guizmaii.zazr.collection.NonEmptySetTest#sets")
        public void shouldQuery(int n, HashSet<Integer> set) {
            final NonEmptySet<Integer> nes = nes(set);
            assertThat(nes.size()).isEqualTo(n);
            assertThat(nes.contains(n - 1)).isTrue();
            assertThat(nes.contains(n)).isFalse();
            assertThat(nes.containsAll(once(Vector.range(0, n)))).isTrue();
            assertThat(nes.containsAll(List.of(0, n))).isFalse();
            assertThat(nes.exists(i -> i == n - 1)).isTrue();
            assertThat(nes.existsUnique(i -> i == 0)).isTrue();
            assertThat(nes.existsUnique(i -> i >= 0)).isEqualTo(n == 1);
            assertThat(nes.forAll(i -> i < n)).isTrue();
            assertThat(nes.count(i -> i % 2 == 0)).isEqualTo((n + 1) / 2);
            assertThat(nes.find(i -> i == n - 1)).isEqualTo(Option.some(n - 1));
            assertThat(nes.find(i -> i == n)).isEqualTo(Option.none());
            assertThat(nes.arrangeBy(i -> -i)).isEqualTo(set.arrangeBy(i -> -i));
            assertThat(nes.arrangeBy(i -> 0)).isEqualTo(n == 1 ? set.arrangeBy(i -> 0) : Option.none());
            if (n == 1) {
                assertThat(nes.single()).isEqualTo(0);
            } else {
                assertThatThrownBy(nes::single).isInstanceOf(java.util.NoSuchElementException.class);
            }
        }

        @ParameterizedTest
        @MethodSource("com.guizmaii.zazr.collection.NonEmptySetTest#sets")
        public void shouldIterateStreamAndConvert(int n, HashSet<Integer> set) {
            final NonEmptySet<Integer> nes = nes(set);
            final java.util.List<Integer> iterated = new java.util.ArrayList<>();
            nes.forEach(iterated::add);
            assertThat(iterated).isEqualTo(new java.util.ArrayList<>(set.toVector().asJava()));
            assertThat(nes.iterator()).toIterable().containsExactlyElementsOf(set);
            assertThat(nes.spliterator().characteristics()).isEqualTo(set.spliterator().characteristics());
            assertThat(nes.spliterator().hasCharacteristics(Spliterator.DISTINCT)).isTrue();
            assertThat(nes.spliterator().getExactSizeIfKnown()).isEqualTo(n);
            assertThat(nes.stream().collect(Collectors.toSet())).isEqualTo(set.asJava());
            assertThat(nes.asJava()).isEqualTo(set.asJava());
            assertThatThrownBy(() -> nes.asJava().add(-1)).isInstanceOf(UnsupportedOperationException.class);
            assertThat(nes.mkString()).isEqualTo(set.mkString());
            assertThat(nes.mkString(", ")).isEqualTo(set.mkString(", "));
            assertThat(nes.mkString("<", ", ", ">")).isEqualTo(set.mkString("<", ", ", ">"));
            assertThat(nes.collect(Collectors.toList())).isEqualTo(set.collect(Collectors.toList()));
            assertThat(nes.<java.util.ArrayList<Integer>> collect(java.util.ArrayList::new, java.util.ArrayList::add, java.util.ArrayList::addAll)).isEqualTo(new java.util.ArrayList<>(set.toVector().asJava()));
            assertThat(nes.toArray()).isEqualTo(set.toArray());
            assertThat(nes.toArray(Integer[]::new)).isEqualTo(set.toArray(Integer[]::new));
            assertThat(nes.toVector()).isEqualTo(set.toVector());
            assertThat(nes.toList()).isEqualTo(set.toList());
            assertThat(nes.toQueue()).isEqualTo(set.toQueue());
            assertThat(nes.toStream()).isEqualTo(set.toStream());
            assertThat(nes.toLinkedSet()).isEqualTo(set.toLinkedSet());
            assertThat(nes.toSortedSet()).isEqualTo(set.toSortedSet());
            assertThat(nes.toSortedSet(Comparator.reverseOrder()).head()).isEqualTo(n - 1);
            assertThat(nes.toMap(i -> i, i -> -i)).isEqualTo(set.toMap(i -> i, i -> -i));
            assertThat(nes.toMap(i -> Tuple.of(i % 5, i))).isEqualTo(set.toMap(i -> Tuple.of(i % 5, i)));
            assertThat(nes.toLinkedMap(i -> i, i -> -i)).isEqualTo(set.toLinkedMap(i -> i, i -> -i));
            assertThat(nes.toLinkedMap(i -> Tuple.of(i, -i))).isEqualTo(set.toLinkedMap(i -> Tuple.of(i, -i)));
            assertThat(nes.toSortedMap(i -> i, i -> -i)).isEqualTo(set.toSortedMap(i -> i, i -> -i));
            assertThat(nes.toSortedMap(i -> Tuple.of(i, -i))).isEqualTo(set.toSortedMap(i -> Tuple.of(i, -i)));
            assertThat(nes.toSortedMap(Comparator.<Integer> reverseOrder(), i -> i, i -> -i).head()._1()).isEqualTo(n - 1);
            assertThat(nes.toSortedMap(Comparator.<Integer> reverseOrder(), i -> Tuple.of(i, -i)).head()._1()).isEqualTo(n - 1);
        }
    }

    /**
     * Elements whose hashes collide: the trie keeps them in collision leaves, which every operation must walk, add to
     * and remove from like any other node.
     */
    @Nested
    class Collisions {

        @Test
        public void shouldKeepTheContractOnCollidingElements() {
            for (int n : SIZES) {
                final HashSet<Colliding> set = colliding(n);
                final NonEmptySet<Colliding> nes = nes(set);
                assertThat(nes.size()).isEqualTo(n);
                assertThat(nes.add(new Colliding(n)).toSet()).isEqualTo(set.add(new Colliding(n)));
                assertThat(nes.add(new Colliding(0))).isSameAs(nes);
                assertThat(nes.remove(new Colliding(0))).isEqualTo(set.remove(new Colliding(0)));
                assertThat(nes.contains(new Colliding(n - 1))).isTrue();
                assertThat(nes.contains(new Colliding(n))).isFalse();
                assertThat(nes.union(colliding(n + 3)).toSet()).isEqualTo(colliding(n + 3));
                assertThat(nes.intersect(colliding(n / 2))).isEqualTo(colliding(n / 2));
                assertThat(nes.diff(colliding(n))).isEmpty();
                assertThat(nes.replace(new Colliding(0), new Colliding(-4)).toSet()).isEqualTo(set.replace(new Colliding(0), new Colliding(-4)));
                assertThat(nes.max()).isEqualTo(new Colliding(n - 1));
                assertThat(nes.min()).isEqualTo(new Colliding(0));
                assertThat(nes.map(c -> c.value() >> 2).toSet()).isEqualTo(HashSet.range(0, (n + 3) / 4));
                assertThat(nes.groupBy(Colliding::hashCode).size()).isEqualTo((n + 3) / 4);
                assertThat(nes.filter(c -> c.value() % 4 == 0)).isEqualTo(set.filter(c -> c.value() % 4 == 0));
                assertThat(nes).isEqualTo(NonEmptySet.fromIterable(set.toVector().reverse()).get());
            }
        }
    }

    /**
     * Every method whose result is or holds a non-empty collection (directly, or inside a tuple, a {@code Vector}, a
     * map or an {@code Option}), called overload by overload on the inputs most likely to empty it. The wrapper is built
     * without a check on the internal paths, so this is what stands between a bug and an empty {@code NonEmptySet};
     * the last test makes sure no such overload is left out.
     */
    @Nested
    class NonEmptyGuarantee {

        /* signature -> calls of that overload, with arguments chosen to shrink the result as far as they can */
        static java.util.Map<String, Function<NonEmptySet<Integer>, java.util.List<Object>>> calls() {
            final java.util.Map<String, Function<NonEmptySet<Integer>, java.util.List<Object>>> calls = new java.util.HashMap<>();
            // constructors and narrowings
            calls.put("of(Object, Object[])", s -> java.util.List.of(NonEmptySet.of(s.max()), NonEmptySet.of(s.max(), s.max(), s.max())));
            calls.put("single(Object)", s -> java.util.List.of(NonEmptySet.single(s.max())));
            calls.put("fromIterable(Object, Iterable)", s -> java.util.List.of(NonEmptySet.fromIterable(s.max(), java.util.List.of()), NonEmptySet.fromIterable(s.max(), s)));
            calls.put("fromIterable(Iterable)", s -> java.util.List.of(NonEmptySet.fromIterable(s), NonEmptySet.fromIterable(s.asJava()), NonEmptySet.fromIterable(List.empty())));
            calls.put("fromSet(HashSet)", s -> java.util.List.of(NonEmptySet.fromSet(s.toSet()), NonEmptySet.fromSet(HashSet.empty())));
            calls.put("unsafeFromSet(HashSet)", s -> java.util.List.of(NonEmptySet.unsafeFromSet(s.toSet())));
            calls.put("flatten(NonEmptySet)", s -> java.util.List.of(NonEmptySet.flatten(NonEmptySet.single(s)), NonEmptySet.flatten(s.map(NonEmptySet::single))));
            // at least one element is left
            calls.put("add(Object)", s -> java.util.List.of(s.add(0), s.add(-1)));
            calls.put("addAll(Iterable)", s -> java.util.List.of(s.addAll(List.empty()), s.addAll(HashSet.empty()), s.addAll(s)));
            calls.put("union(Set)", s -> java.util.List.of(s.union(HashSet.empty()), s.union(TreeSet.empty()), s.union(s.toSet())));
            calls.put("map(Function)", s -> java.util.List.of(s.map(i -> 0), s.map(i -> i)));
            calls.put("flatMap(Function)", s -> java.util.List.of(s.flatMap(i -> NonEmptySet.single(0))));
            calls.put("as(Object)", s -> java.util.List.of(s.as("x")));
            calls.put("replace(Object, Object)", s -> java.util.List.of(s.replace(s.max(), s.min()), s.replace(-2, -1)));
            calls.put("replaceAll(Object, Object)", s -> java.util.List.of(s.replaceAll(s.max(), s.min()), s.replaceAll(-2, -1)));
            calls.put("tap(Consumer)", s -> java.util.List.of(s.tap(i -> { })));
            // non-empty sets inside another type
            calls.put("groupBy(Function)", s -> java.util.List.of(s.groupBy(i -> 0), s.groupBy(i -> i)));
            return calls;
        }

        @ParameterizedTest
        @MethodSource("com.guizmaii.zazr.collection.NonEmptySetTest#sets")
        public void shouldReturnOnlyNonEmptyNonEmptySets(int n, HashSet<Integer> set) {
            for (var call : calls().entrySet()) {
                for (Object result : call.getValue().apply(nes(set))) {
                    NonEmptyChecks.assertEveryNonEmptyCollectionIsNonEmpty(result, call.getKey());
                }
            }
        }

        @Test
        public void shouldCoverEveryOverloadWhoseResultHoldsANonEmptyCollection() {
            assertThat(calls().keySet()).containsExactlyInAnyOrderElementsOf(NonEmptyChecks.holding(NonEmptySet.class));
        }
    }

    /**
     * {@code NonEmptySet} has every operation of {@code HashSet}, under the name {@code HashSet} gives it, except the
     * ones listed here on purpose. The list is exact: a name missing from {@code NonEmptySet} and not listed fails, and
     * so does a listed name that {@code HashSet} lost or {@code NonEmptySet} gained.
     */
    @Nested
    class SameApiAsHashSet {

        static final java.util.Set<String> DELIBERATELY_ABSENT = java.util.Set.of(
                // the Option forms of what is total on a non-empty set
                "reduceOption", "singleOption",
                // constant on a non-empty set: false, true, this, Some(this)
                "isEmpty", "nonEmpty", "orElse", "toNonEmptySet"
        );

        @Test
        public void shouldHaveEveryHashSetMethodButTheDeliberateAbsences() {
            assertThat(NonEmptyChecks.missing(HashSet.class, NonEmptySet.class)).containsExactlyInAnyOrderElementsOf(DELIBERATELY_ABSENT);
        }

        @Test
        public void shouldHaveTheStaticFlatten() throws NoSuchMethodException {
            assertThat(java.lang.reflect.Modifier.isStatic(NonEmptySet.class.getMethod("flatten", NonEmptySet.class).getModifiers())).isTrue();
        }
    }

    @Nested
    class Flatten {

        @Test
        public void shouldFlattenNestedNonEmptySets() {
            for (int n : SIZES) {
                final NonEmptySet<NonEmptySet<Integer>> nested = nes(HashSet.range(0, n)).map(i -> nes(HashSet.range(i, i + 3)));
                final NonEmptySet<Integer> flat = NonEmptySet.flatten(nested);
                assertThat(flat.toSet()).isEqualTo(HashSet.range(0, n + 2));
            }
            final NonEmptySet<Number> covariant = NonEmptySet.flatten(NonEmptySet.of(NonEmptySet.of(1), NonEmptySet.of(2.0)));
            assertThat(covariant.toSet()).isEqualTo(HashSet.<Number> of(1, 2.0));
        }
    }

    @Nested
    class ObjectMethods {

        @ParameterizedTest
        @MethodSource("com.guizmaii.zazr.collection.NonEmptySetTest#sets")
        public void shouldBeEqualToANonEmptySetWithTheSameElements(int n, HashSet<Integer> set) {
            final NonEmptySet<Integer> nes = nes(set);
            final NonEmptySet<Integer> copy = NonEmptySet.fromIterable(set.toVector().reverse()).get();
            assertThat(nes).isEqualTo(nes);
            assertThat(nes).isEqualTo(copy);
            assertThat(nes.hashCode()).isEqualTo(copy.hashCode());
            assertThat(nes.hashCode()).isEqualTo(set.hashCode());
            assertThat(nes).isNotEqualTo(nes.add(n));
            assertThat(nes).isNotEqualTo(set);
            assertThat(set).isNotEqualTo(nes);
            assertThat(nes).isNotEqualTo(null);
            assertThat(nes).isNotEqualTo(set.toVector());
            // sets are equal to sets, whatever the representation
            final NonEmptySortedSet<Integer> sorted = NonEmptySortedSet.unsafeFromSortedSet(TreeSet.ofAll(Comparator.reverseOrder(), set));
            assertThat(nes).isEqualTo(sorted);
            assertThat(sorted).isEqualTo(nes);
            assertThat(nes.hashCode()).isEqualTo(sorted.hashCode());
            assertThat(nes).isNotEqualTo(sorted.add(n));
        }

        @Test
        public void shouldStringify() {
            assertThat(NonEmptySet.of(1).toString()).isEqualTo("NonEmptySet(1)");
            assertThat(nes(HashSet.range(0, 33)).toString()).isEqualTo(HashSet.range(0, 33).mkString("NonEmptySet(", ", ", ")"));
        }
    }
}
