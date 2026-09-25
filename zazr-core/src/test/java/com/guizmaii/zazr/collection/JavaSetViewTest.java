package com.guizmaii.zazr.collection;

import java.util.Comparator;
import java.util.stream.IntStream;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The {@link java.util.Set} views against the JDK sets with the same elements: {@link HashSet} against
 * {@link java.util.HashSet}, {@link LinkedHashSet} against {@link java.util.LinkedHashSet} (with the reversed
 * views), {@link TreeSet} against {@link java.util.TreeSet} in natural and reversed order (with the descending
 * views, and the head, tail and sub sets of each, nested), for 0, 1, 32, 33 and 1025 elements.
 */
class JavaSetViewTest {

    @TestFactory
    java.util.stream.Stream<DynamicTest> shouldReadAHashSetLikeAJavaHashSet() {
        return IntStream.of(JavaViewContract.SIZES).mapToObj(n -> DynamicTest.dynamicTest("HashSet of " + n, () -> {
            final java.util.List<Integer> elements = JavaViewContract.evens(n);
            JavaViewContract.unordered().set("HashSet(" + n + ").asJava()", HashSet.ofAll(elements).asJava(), new java.util.HashSet<>(elements),
                    JavaViewContract.keyProbes(n));
        }));
    }

    @TestFactory
    java.util.stream.Stream<DynamicTest> shouldReadALinkedHashSetLikeAJavaLinkedHashSet() {
        return IntStream.of(JavaViewContract.SIZES).mapToObj(n -> DynamicTest.dynamicTest("LinkedHashSet of " + n, () -> {
            final java.util.List<Integer> elements = shuffled(JavaViewContract.evens(n));
            JavaViewContract.ordered().sequencedSet("LinkedHashSet(" + n + ").asJava()", LinkedHashSet.ofAll(elements).asJava(),
                    new java.util.LinkedHashSet<>(elements), JavaViewContract.keyProbes(n), 0);
        }));
    }

    @TestFactory
    java.util.stream.Stream<DynamicTest> shouldReadALinkedHashSetWithRemovedElementsLikeAJavaLinkedHashSet() {
        return IntStream.of(JavaViewContract.SIZES).mapToObj(n -> DynamicTest.dynamicTest("LinkedHashSet of " + n + " minus every third", () -> {
            final java.util.List<Integer> elements = shuffled(JavaViewContract.evens(n));
            LinkedHashSet<Integer> set = LinkedHashSet.ofAll(elements);
            final java.util.LinkedHashSet<Integer> reference = new java.util.LinkedHashSet<>(elements);
            for (int i = 0; i < elements.size(); i += 3) {
                set = set.remove(elements.get(i));
                reference.remove(elements.get(i));
            }
            JavaViewContract.ordered().sequencedSet("LinkedHashSet(" + n + ").remove(...).asJava()", set.asJava(), reference, JavaViewContract.keyProbes(n), 0);
        }));
    }

    @TestFactory
    java.util.stream.Stream<DynamicTest> shouldReadATreeSetLikeAJavaTreeSet() {
        return JavaViewContract.comparators().stream().flatMap(comparator -> IntStream.of(JavaViewContract.SIZES).mapToObj(n ->
                DynamicTest.dynamicTest("TreeSet of " + n + " by " + name(comparator), () -> {
                    final java.util.List<Integer> elements = shuffled(JavaViewContract.evens(n));
                    final java.util.TreeSet<Integer> reference = new java.util.TreeSet<>(comparator == Comparator.<Integer> naturalOrder() ? null : comparator);
                    reference.addAll(elements);
                    JavaViewContract.ordered().navigableSet("TreeSet(" + n + ", " + name(comparator) + ").asJava()", treeSet(comparator, elements).asJava(), reference,
                            JavaViewContract.keyProbes(n), JavaViewContract.bounds(n), 0);
                })));
    }

    @Test
    void shouldReportTheNaturalOrderAsANullComparator() {
        assertThat(TreeSet.of(1, 2).asJava().comparator()).isNull();
        assertThat(TreeSet.of(1, 2).asJava().spliterator().getComparator()).isNull();
        assertThat(TreeSet.of(1, 2).asJava().subSet(0, 5).comparator()).isNull();
        assertThat(TreeSet.of(1, 2).asJava().descendingSet().comparator()).isEqualTo(java.util.Collections.reverseOrder());
        assertThat(TreeSet.of(Comparator.reverseOrder(), 1, 2).asJava().comparator()).isEqualTo(Comparator.reverseOrder());
        final Comparator<Integer> byAbs = Comparator.comparingInt(Math::abs);
        assertThat(TreeSet.of(byAbs, 1, -2).asJava().comparator()).isSameAs(byAbs);
        assertThat(TreeSet.of(byAbs, 1, -2).asJava().descendingSet().comparator()).isEqualTo(java.util.Collections.reverseOrder(byAbs));
    }

    @Test
    void shouldRejectNullAndForeignBoundsAsAJavaTreeSetDoes() {
        final java.util.NavigableSet<Integer> view = TreeSet.<Integer> empty().asJava();
        final java.util.TreeSet<Integer> reference = new java.util.TreeSet<>();
        for (java.util.NavigableSet<Integer> set : java.util.List.of(view, reference)) {
            assertThatThrownBy(() -> set.headSet(null)).isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> set.tailSet(null, false)).isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> set.subSet(null, 3)).isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> set.contains(null)).isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> set.subSet(3, 1)).isInstanceOf(IllegalArgumentException.class);
            assertThat(set.ceiling(null)).isNull();
        }
        final java.util.NavigableSet<Object> foreign = (java.util.NavigableSet) TreeSet.of(1, 2).asJava();
        assertThatThrownBy(() -> foreign.headSet(new Object())).isInstanceOf(ClassCastException.class);
        // a bound is compared only with itself when the view is made, as by a java.util.TreeSet
        assertThat(foreign.headSet("a")).isNotNull();
        assertThat(new java.util.TreeSet<Object>(java.util.List.of(1, 2)).headSet("a")).isNotNull();
        assertThatThrownBy(() -> foreign.contains(new Object())).isInstanceOf(ClassCastException.class);
        final java.util.NavigableSet<Object> emptyForeign = (java.util.NavigableSet) TreeSet.empty().asJava();
        assertThatThrownBy(() -> emptyForeign.contains(new Object())).isInstanceOf(ClassCastException.class);
    }

    static <T> java.util.List<T> shuffled(java.util.List<T> elements) {
        final java.util.List<T> result = new java.util.ArrayList<>(elements);
        java.util.Collections.shuffle(result, new java.util.Random(42));
        return result;
    }

    static TreeSet<Integer> treeSet(Comparator<Integer> comparator, java.util.List<Integer> elements) {
        return comparator == Comparator.<Integer> naturalOrder() ? TreeSet.ofAll(elements) : TreeSet.ofAll(comparator, elements);
    }

    static String name(Comparator<Integer> comparator) {
        return comparator == Comparator.<Integer> naturalOrder() ? "natural order" : "reversed order";
    }
}
