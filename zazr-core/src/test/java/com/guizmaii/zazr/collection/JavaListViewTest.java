package com.guizmaii.zazr.collection;

import java.util.ArrayList;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.IntStream;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The {@link java.util.List} views of the four sequences against {@link ArrayList}: every read of
 * {@link JavaViewContract#list}, on sub-lists of sub-lists and on the reversed views, for 0, 1, 32, 33 and 1025
 * elements.
 */
class JavaListViewTest {

    private static final java.util.Map<String, Function<java.util.List<Integer>, java.util.List<Integer>>> VIEWS = java.util.Map.of(
            "Vector", elements -> Vector.ofAll(elements).asJava(),
            "List", elements -> List.ofAll(elements).asJava(),
            "Queue", elements -> Queue.ofAll(elements).asJava(),
            "Stream", elements -> Stream.ofAll(elements).asJava(),
            "NonEmptyVector", elements -> elements.isEmpty() ? Vector.<Integer> empty().asJava() : NonEmptyVector.fromIterable(elements).get().asJava());

    @TestFactory
    java.util.stream.Stream<DynamicTest> shouldReadLikeAnArrayList() {
        return VIEWS.entrySet().stream().sorted(java.util.Map.Entry.comparingByKey()).flatMap(view -> IntStream.of(JavaViewContract.SIZES).mapToObj(n ->
                DynamicTest.dynamicTest(view.getKey() + " of " + n, () -> {
                    final java.util.List<Integer> elements = JavaViewContract.pairs(n);
                    final JavaViewContract contract = JavaViewContract.ordered();
                    contract.list(view.getKey() + "(" + n + ").asJava()", view.getValue().apply(elements), new ArrayList<>(elements), JavaViewContract.elementProbes(n), 0);
                    assertThat(contract.checks()).isPositive();
                })));
    }

    @Nested
    class StreamLaziness {

        /** An infinite Stream counting the elements it computes. */
        private Stream<Integer> counted(AtomicInteger forced) {
            return Stream.iterate(0, i -> {
                forced.incrementAndGet();
                return i + 1;
            });
        }

        @Test
        void shouldForceNoMoreThanTheReadsNeed() {
            final AtomicInteger forced = new AtomicInteger();
            final java.util.List<Integer> view = counted(forced).asJava();
            assertThat(forced).hasValue(0);
            assertThat(view.isEmpty()).isFalse();
            assertThat(view.getFirst()).isEqualTo(0);
            assertThat(view.get(5)).isEqualTo(5);
            assertThat(forced).hasValue(5);
            assertThat(view.contains(7)).isTrue();
            assertThat(view.indexOf(9)).isEqualTo(9);
            assertThat(forced).hasValue(9);
            final java.util.Iterator<Integer> iterator = view.iterator();
            assertThat(iterator.next()).isEqualTo(0);
            assertThat(iterator.next()).isEqualTo(1);
            assertThat(view.stream().limit(12).toList()).hasSize(12);
            assertThat(forced).hasValue(11);
            assertThat(view.subList(3, 20).get(2)).isEqualTo(5);
            assertThat(forced).hasValue(19);
            final java.util.ListIterator<Integer> listIterator = view.listIterator(30);
            assertThat(listIterator.next()).isEqualTo(30);
            assertThat(listIterator.previous()).isEqualTo(30);
            assertThat(listIterator.previous()).isEqualTo(29);
            assertThat(forced).hasValue(30);
            assertThat(view.equals(java.util.List.of(0, 1, 2))).isFalse();
            assertThat(forced).hasValue(30);
        }

        @Test
        void shouldCheckTheSubListBoundsOfAFiniteStreamWithoutTheWholeLength() {
            final java.util.List<Integer> view = Stream.of(0, 1, 2).asJava();
            assertThatThrownBy(() -> view.subList(0, 4)).isInstanceOf(IndexOutOfBoundsException.class);
            assertThatThrownBy(() -> view.listIterator(4)).isInstanceOf(IndexOutOfBoundsException.class);
            assertThat(view.subList(1, 3)).containsExactly(1, 2);
            assertThat(view.listIterator(3).hasNext()).isFalse();
        }

        @Test
        void shouldIterateTheListIteratorOfALinearSequenceForwardInOneWalk() {
            final java.util.ListIterator<Integer> iterator = List.of(1, 2, 3).asJava().listIterator(1);
            assertThat(iterator.next()).isEqualTo(2);
            assertThat(iterator.next()).isEqualTo(3);
            assertThat(iterator.hasNext()).isFalse();
            assertThatThrownBy(iterator::next).isInstanceOf(NoSuchElementException.class);
            assertThat(iterator.previous()).isEqualTo(3);
            assertThat(iterator.next()).isEqualTo(3);
        }
    }
}
