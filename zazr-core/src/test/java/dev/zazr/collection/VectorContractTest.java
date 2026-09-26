package dev.zazr.collection;

import dev.zazr.control.Option;
import java.util.ArrayList;
import java.util.function.Function;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The exception types and messages, and the identity of the results, of {@link Vector} at every size around the
 * boundaries of its tree, for Vectors built in different ways; and the concatenation of Vectors of every shape.
 */
public class VectorContractTest {

    private static final int[] SIZES = { 0, 1, 2, 31, 32, 33, 64, 65, 1023, 1024, 1025, 1057, 32768, 32769, 33825 };

    /* the same elements 0 .. n-1, built in six ways */
    private static java.util.List<Vector<Integer>> histories(int n) {
        final java.util.List<Integer> list = IntStream.range(0, n).boxed().toList();
        Vector<Integer> appended = Vector.empty();
        Vector<Integer> prepended = Vector.empty();
        for (int i = 0; i < n; i++) {
            appended = appended.append(i);
            prepended = prepended.prepend(n - 1 - i);
        }
        final Vector<Integer> sliced = Vector.range(-40, n + 40).slice(40, n + 40);
        return java.util.List.of(Vector.ofAll(list), Vector.range(0, n), Vector.ofAll(IntStream.range(0, n).toArray()), appended, prepended,
                sliced);
    }

    private static java.util.List<Integer> withNull(int size, int at) {
        final java.util.List<Integer> list = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            list.add(i == at ? null : i);
        }
        return list;
    }

    private static Iterable<Integer> oneShot(java.util.List<Integer> list) {
        final java.util.Iterator<Integer> it = list.iterator();
        return () -> it;
    }

    @Test
    public void exceptionsAtEverySize() {
        for (int n : SIZES) {
            for (Vector<Integer> v : histories(n)) {
                assertElements(v, IntStream.range(0, n).boxed().toList());
                throwsExactly(() -> v.get(-1), IndexOutOfBoundsException.class, "get(-1)");
                throwsExactly(() -> v.get(n), IndexOutOfBoundsException.class, "get(" + n + ")");
                throwsExactly(() -> v.get(Integer.MIN_VALUE), IndexOutOfBoundsException.class, "get(-2147483648)");
                throwsExactly(() -> v.update(-1, (Integer) null), IndexOutOfBoundsException.class, "update(-1)");
                throwsExactly(() -> v.update(n, 1), IndexOutOfBoundsException.class, "update(" + n + ")");
                throwsExactly(() -> v.append(null), NullPointerException.class, "Vector: element is null");
                throwsExactly(() -> v.prepend(null), NullPointerException.class, "Vector: element is null");
                throwsExactly(() -> v.appendAll(null), NullPointerException.class, "iterable is null");
                throwsExactly(() -> v.prependAll(null), NullPointerException.class, "iterable is null");
                throwsExactly(() -> v.insert(-1, 1), IndexOutOfBoundsException.class, "insert(-1, e) on Vector of length " + n);
                throwsExactly(() -> v.insert(0, null), NullPointerException.class, "Iterator.of: element is null");
                throwsExactly(() -> v.removeAt(n), IndexOutOfBoundsException.class, "removeAt(" + n + ")");
                throwsExactly(() -> v.map(null), NullPointerException.class, "mapper is null");
                throwsExactly(() -> v.filter(null), NullPointerException.class, "predicate is null");
                throwsExactly(() -> v.collect(i -> null), NullPointerException.class, n == 0 ? "no exception" : "Vector.collect: mapper returned null");
                throwsExactly(() -> v.flatMap(i -> withNull(1, 0)), NullPointerException.class, n == 0 ? "no exception" : "Vector.Builder.add: element is null");
                throwsExactly(() -> v.map(i -> i == n - 1 ? null : i), NullPointerException.class, n == 0 ? "no exception" : "Vector: element is null");
                for (java.util.List<Integer> nulls : java.util.List.of(withNull(3, 1), withNull(40, 20))) {
                    throwsExactly(() -> v.appendAll(nulls), NullPointerException.class, "Vector: element is null");
                    throwsExactly(() -> v.prependAll(nulls), NullPointerException.class, "Vector: element is null");
                    throwsExactly(() -> v.appendAll(oneShot(nulls)), NullPointerException.class, "Vector.Builder.add: element is null");
                    throwsExactly(() -> v.prependAll(oneShot(nulls)), NullPointerException.class, "Vector.Builder.add: element is null");
                }
                if (n == 0) {
                    throwsExactly(v::head, java.util.NoSuchElementException.class, "head of empty Vector");
                    throwsExactly(v::last, java.util.NoSuchElementException.class, "last of empty Vector");
                    throwsExactly(v::tail, UnsupportedOperationException.class, "tail of empty Vector");
                    throwsExactly(v::init, UnsupportedOperationException.class, "init of empty Vector");
                } else {
                    throwsExactly(() -> v.update(0, (Integer) null), NullPointerException.class, "Vector.update: element is null");
                    throwsExactly(() -> v.update(n - 1, i -> null), NullPointerException.class, "Vector.update: element is null");
                    assertThat(v.head()).isZero();
                    assertThat(v.last()).isEqualTo(n - 1);
                    assertElements(v.tail(), IntStream.range(1, n).boxed().toList());
                    assertElements(v.init(), IntStream.range(0, n - 1).boxed().toList());
                }
            }
        }
    }

    /* the same elements in the same order, by iteration and by index */
    private static void assertElements(Vector<Integer> actual, java.util.List<Integer> expected) {
        if (actual.size() != expected.size()) {
            throw new AssertionError("size " + actual.size() + ", expected " + expected.size());
        }
        final java.util.Iterator<Integer> it = actual.iterator();
        for (int i = 0; i < expected.size(); i++) {
            final Integer e = expected.get(i);
            if (!e.equals(it.next()) || !e.equals(actual.get(i))) {
                throw new AssertionError("element " + i + ": " + actual.get(i) + ", expected " + e);
            }
        }
        if (it.hasNext()) {
            throw new AssertionError("the iterator goes beyond " + expected.size());
        }
    }

    /* the message "no exception" means that the call succeeds */
    private static void throwsExactly(Runnable call, Class<? extends Throwable> type, String message) {
        if ("no exception".equals(message)) {
            call.run();
        } else {
            assertThatThrownBy(call::run).isExactlyInstanceOf(type).hasMessage(message);
        }
    }

    @Test
    public void identityOfTheResultsAtEverySize() {
        for (int n : SIZES) {
            for (Vector<Integer> v : histories(n)) {
                final Vector<Integer> empty = Vector.empty();
                assertThat(v.drop(0)).isSameAs(v);
                assertThat(v.drop(Integer.MIN_VALUE)).isSameAs(v);
                assertThat(v.take(n)).isSameAs(v);
                assertThat(v.take(Integer.MAX_VALUE)).isSameAs(v);
                assertThat(v.dropRight(0)).isSameAs(v);
                assertThat(v.takeRight(Integer.MAX_VALUE)).isSameAs(v);
                assertThat(v.slice(Integer.MIN_VALUE, Integer.MAX_VALUE)).isSameAs(v);
                assertThat(v.drop(n)).isSameAs(empty);
                assertThat(v.take(0)).isSameAs(empty);
                assertThat(v.slice(1, 0)).isSameAs(empty);
                assertThat(v.slice(n, n + 1)).isSameAs(empty);
                assertThat(v.appendAll(java.util.List.of())).isSameAs(v);
                assertThat(v.prependAll(java.util.List.of())).isSameAs(v);
                assertThat(v.appendAll(empty)).isSameAs(v);
                assertThat(v.prependAll(oneShot(java.util.List.of()))).isSameAs(v);
                assertThat(v.filter(i -> true)).isSameAs(v);
                assertThat(v.filter(i -> false)).isSameAs(empty);
                assertThat(Vector.ofAll(v)).isSameAs(v);
                assertThat(Vector.ofAll(v.asJava())).isSameAs(v);
                assertThat(empty.appendAll(v)).isSameAs(v);
                assertThat(empty.prependAll(v.asJava())).isSameAs(v);
                assertThat(Vector.<Integer> newBuilder().addAll(v).result()).isEqualTo(v);
                if (n == 1) {
                    assertThat(v.tail()).isSameAs(empty);
                    assertThat(v.init()).isSameAs(empty);
                }
            }
        }
    }

    @Test
    public void nonEmptyVectorNamesItselfWhenRejectingANullElement() {
        for (int n : new int[] { 1, 32, 33, 1025 }) {
            final NonEmptyVector<Integer> nev = NonEmptyVector.fromVector(Vector.range(0, n)).get();
            throwsExactly(() -> nev.append(null), NullPointerException.class, "NonEmptyVector.append: element is null");
            throwsExactly(() -> nev.prepend(null), NullPointerException.class, "NonEmptyVector.prepend: element is null");
        }
    }

    @Test
    public void ofCopiesTheCallersArray() {
        for (int n : new int[] { 1, 31, 32, 33, 1025 }) {
            final Integer[] elements = IntStream.range(0, n).boxed().toArray(Integer[]::new);
            final Vector<Integer> v = Vector.of(elements);
            elements[0] = -1;
            elements[n - 1] = -1;
            assertElements(v, IntStream.range(0, n).boxed().toList());
        }
    }

    @Test
    public void filterStartsFromTheKeptPrefixAtEveryBoundary() {
        for (int n : SIZES) {
            for (Vector<Integer> v : histories(n)) {
                for (int firstRejected : new int[] { 0, 1, 31, 32, 33, 1023, 1024, 1025, n - 1 }) {
                    if (firstRejected < 0 || firstRejected >= n) {
                        continue;
                    }
                    final Function<Integer, Boolean> keep = i -> i < firstRejected || i % 3 == 0;
                    final java.util.List<Integer> expected = IntStream.range(0, n).filter(keep::apply).boxed().toList();
                    assertElements(v.filter(keep::apply), expected);
                    assertElements(v.reject(i -> !keep.apply(i)), expected);
                }
            }
        }
    }

    @Test
    public void concatenationOfEveryPairOfShapes() {
        final int[] sizes = { 0, 1, 2, 31, 32, 33, 65, 1024, 1025, 1057, 32769 };
        for (int left : sizes) {
            final java.util.List<Vector<Integer>> lefts = histories(left);
            for (int right : sizes) {
                final java.util.List<Integer> l = IntStream.range(0, left).boxed().toList();
                final java.util.List<Integer> r = IntStream.range(left, left + right).boxed().toList();
                final java.util.List<Integer> both = IntStream.range(0, left + right).boxed().toList();
                for (Vector<Integer> lv : lefts) {
                    final Vector<Integer> rv = Vector.range(left, left + right);
                    final Vector<Integer> rvDropped = Vector.range(left - 7, left + right).drop(7);
                    for (Iterable<Integer> argument : java.util.List.of(rv, rvDropped, rv.asJava(), r, oneShot(r), List.ofAll(r))) {
                        assertElements(lv.appendAll(argument), both);
                    }
                    final Vector<Integer> shifted = lv.map(i -> i + right);
                    final java.util.List<Integer> front = IntStream.range(0, right).boxed().toList();
                    final Vector<Integer> fv = Vector.range(0, right);
                    for (Iterable<Integer> argument : java.util.List.of(fv, fv.asJava(), front, oneShot(front), List.ofAll(front))) {
                        assertElements(shifted.prependAll(argument), both);
                    }
                    assertElements(lv, l);
                    assertElements(Vector.<Integer> newBuilder().addAll(lv).addAll(rv.asJava()).addAll(rvDropped.take(0)).result(), both);
                }
            }
        }
    }

    @Test
    public void collectFlatMapAndMapAtEverySize() {
        for (int n : SIZES) {
            for (Vector<Integer> v : histories(n)) {
                assertElements(v.collect(i -> i % 2 == 0 ? Option.some(-i) : Option.none()), IntStream.range(0, n).filter(i -> i % 2 == 0).map(i -> -i).boxed().toList());
                assertThat(v.flatMap(i -> Vector.of(i, i))).hasSize(2 * n);
                final Vector<Integer> mapped = v.map(i -> i + 1);
                assertElements(mapped, IntStream.range(1, n + 1).boxed().toList());
                assertThat(v.map(i -> i)).isEqualTo(v);
            }
        }
    }
}
