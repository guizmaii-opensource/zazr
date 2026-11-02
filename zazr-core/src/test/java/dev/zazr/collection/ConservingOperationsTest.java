package dev.zazr.collection;

import dev.zazr.Tuple;
import dev.zazr.Tuple2;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.StringJoiner;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.function.Supplier;
import org.assertj.core.api.SoftAssertions;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/// An operation that changes nothing returns its receiver itself: on every sequence, each public method that returns
/// the sequence's own type (or a tuple of two) is either a conserving row, or a row that says why it is left out. The
/// coverage test reads the methods by reflection, so a new operation fails it until it gets a row.
///
/// A conserving row is checked on receivers of several sizes (the trie boundaries for `Vector`, both shapes of a
/// `Queue`): the no-op call returns the receiver itself, or a tuple with the receiver on one side and an empty
/// sequence on the other; the receiver is unchanged; and a call that changes one element returns another instance,
/// equal to the expected result.
class ConservingOperationsTest {

    private static final int[] SIZES = {1, 2, 3, 31, 32, 33, 1023, 1024, 1025};

    /// One conserving row: `noOp` applied to `prepare(receiver)` returns that receiver (or a tuple with it on one
    /// side); `change` applied to `changeInput` returns another instance, equal to `expected` (or holding the same
    /// elements in any order, when `expected` is null). A row without `change` is an operation that never changes
    /// anything (`tap`, the conversion to the same type).
    private record Row<C>(
            String signature,
            Function<C, C> prepare,
            Function<C, Object> noOp,
            @Nullable C changeInput,
            @Nullable Function<C, Object> change,
            @Nullable Object expected) {}

    private static final class Table<C> {
        final Class<?> type;
        final Supplier<java.util.List<C>> receivers;
        final java.util.List<Row<C>> rows = new ArrayList<>();
        final Map<String, String> left = new LinkedHashMap<>();

        Table(Class<?> type, Supplier<java.util.List<C>> receivers) {
            this.type = type;
            this.receivers = receivers;
        }

        Table<C> keep(
                String signature,
                Function<C, Object> noOp,
                C changeInput,
                Function<C, Object> change,
                Object expected) {
            rows.add(new Row<>(signature, Function.identity(), noOp, changeInput, change, expected));
            return this;
        }

        /// A row whose no-op holds only on a prepared receiver, such as `reverse` on one element.
        Table<C> keepOn(
                String signature,
                Function<C, C> prepare,
                Function<C, Object> noOp,
                C changeInput,
                Function<C, Object> change,
                @Nullable Object expected) {
            rows.add(new Row<>(signature, prepare, noOp, changeInput, change, expected));
            return this;
        }

        Table<C> same(String signature, Function<C, Object> noOp) {
            rows.add(new Row<>(signature, Function.identity(), noOp, null, null, null));
            return this;
        }

        Table<C> leave(String reason, String... signatures) {
            for (String signature : signatures) {
                left.put(signature, reason);
            }
            return this;
        }
    }

    // -- the reasons an operation is left out

    private static final String ALWAYS_CHANGES = "always changes the elements: there is no no-op call";
    private static final String NEW_COLLECTION = "returns other elements than the receiver's, never the receiver";
    private static final String KEEP_LAST =
            "builds its result before it can know that every element is kept: a check after the copy saves nothing";
    private static final String NEVER_EMPTY = "its argument is a NonEmptyVector, never empty: it always adds";
    private static final String LAZY =
            "decides lazily: knowing that nothing changes would read cells the call itself leaves unread";

    // -- the receivers

    private static java.util.List<Vector<Integer>> vectors() {
        java.util.List<Vector<Integer>> receivers = new ArrayList<>();
        for (int size : SIZES) {
            receivers.add(Vector.range(1, size + 1));
        }
        return receivers;
    }

    private static java.util.List<List<Integer>> lists() {
        java.util.List<List<Integer>> receivers = new ArrayList<>();
        for (int size : new int[] {1, 2, 3, 33}) {
            receivers.add(List.range(1, size + 1));
        }
        return receivers;
    }

    /// Both shapes of a Queue: every element at the front (`ofAll`), and most of them at the back (enqueued).
    private static java.util.List<Queue<Integer>> queues() {
        java.util.List<Queue<Integer>> receivers = new ArrayList<>();
        for (int size : new int[] {1, 2, 3, 33}) {
            receivers.add(Queue.ofAll(List.range(1, size + 1)));
            Queue<Integer> none = Queue.empty();
            receivers.add(List.range(1, size + 1).foldLeft(none, Queue::enqueue));
        }
        return receivers;
    }

    /// Lists whose cells are all evaluated, so that a call that checks only what is already known can see them.
    private static java.util.List<LazyList<Integer>> lazyLists() {
        java.util.List<LazyList<Integer>> receivers = new ArrayList<>();
        for (int size : new int[] {1, 2, 3, 33}) {
            LazyList<Integer> list = LazyList.range(1, size + 1);
            list.size();
            receivers.add(list);
        }
        return receivers;
    }

    private static java.util.List<NonEmptyVector<Integer>> nonEmptyVectors() {
        java.util.List<NonEmptyVector<Integer>> receivers = new ArrayList<>();
        for (int size : SIZES) {
            receivers.add(Vector.range(1, size + 1).toNonEmptyVector().get());
        }
        return receivers;
    }

    // -- the tables

    private static Table<Vector<Integer>> vectorTable() {
        Vector<Integer> abc = Vector.of(1, 2, 3);
        return new Table<Vector<Integer>>(Vector.class, ConservingOperationsTest::vectors)
                .keep(
                        "appendAll(Iterable)",
                        v -> v.appendAll(List.empty()),
                        abc,
                        v -> v.appendAll(List.of(4)),
                        Vector.of(1, 2, 3, 4))
                .keep(
                        "appendAll(Iterable)",
                        v -> v.appendAll(Vector.empty()),
                        abc,
                        v -> v.appendAll(Vector.of(4)),
                        Vector.of(1, 2, 3, 4))
                .keep("distinct()", Vector::distinct, Vector.of(1, 2, 1), Vector::distinct, Vector.of(1, 2))
                .keep(
                        "distinctBy(Comparator)",
                        v -> v.distinctBy(Comparator.naturalOrder()),
                        Vector.of(1, 2, 1),
                        v -> v.distinctBy(Comparator.naturalOrder()),
                        Vector.of(1, 2))
                .keep(
                        "distinctBy(Function)",
                        v -> v.distinctBy(Function.identity()),
                        Vector.of(1, 2, 1),
                        v -> v.distinctBy(Function.identity()),
                        Vector.of(1, 2))
                .keep("drop(int)", v -> v.drop(0), abc, v -> v.drop(1), Vector.of(2, 3))
                .keep("dropRight(int)", v -> v.dropRight(0), abc, v -> v.dropRight(1), Vector.of(1, 2))
                .keep(
                        "dropRightUntil(Predicate)",
                        v -> v.dropRightUntil(x -> true),
                        abc,
                        v -> v.dropRightUntil(x -> x < 3),
                        Vector.of(1, 2))
                .keep(
                        "dropRightWhile(Predicate)",
                        v -> v.dropRightWhile(x -> false),
                        abc,
                        v -> v.dropRightWhile(x -> x == 3),
                        Vector.of(1, 2))
                .keep(
                        "dropUntil(Predicate)",
                        v -> v.dropUntil(x -> true),
                        abc,
                        v -> v.dropUntil(x -> x > 1),
                        Vector.of(2, 3))
                .keep(
                        "dropWhile(Predicate)",
                        v -> v.dropWhile(x -> false),
                        abc,
                        v -> v.dropWhile(x -> x == 1),
                        Vector.of(2, 3))
                .keep("filter(Predicate)", v -> v.filter(x -> true), abc, v -> v.filter(x -> x != 2), Vector.of(1, 3))
                .keep(
                        "insertAll(int, Iterable)",
                        v -> v.insertAll(v.size(), List.empty()),
                        abc,
                        v -> v.insertAll(1, List.of(9)),
                        Vector.of(1, 9, 2, 3))
                .keep(
                        "insertAll(int, Iterable)",
                        v -> v.insertAll(v.size() / 2, java.util.List.of()),
                        abc,
                        v -> v.insertAll(3, java.util.List.of(9)),
                        Vector.of(1, 2, 3, 9))
                .keepOn(
                        "intersperse(Object)",
                        v -> v.take(1),
                        v -> v.intersperse(0),
                        abc,
                        v -> v.intersperse(0),
                        Vector.of(1, 0, 2, 0, 3))
                .keep(
                        "leftPadTo(int, Object)",
                        v -> v.leftPadTo(v.size(), 0),
                        abc,
                        v -> v.leftPadTo(4, 0),
                        Vector.of(0, 1, 2, 3))
                .keep(
                        "orElse(Iterable)",
                        v -> v.orElse(List.of(9)),
                        Vector.empty(),
                        v -> v.orElse(List.of(9)),
                        Vector.of(9))
                .keep(
                        "orElse(Supplier)",
                        v -> v.orElse(() -> List.of(9)),
                        Vector.empty(),
                        v -> v.orElse(() -> List.of(9)),
                        Vector.of(9))
                .keep("padTo(int, Object)", v -> v.padTo(v.size(), 0), abc, v -> v.padTo(4, 0), Vector.of(1, 2, 3, 0))
                .keep(
                        "partition(Predicate)",
                        v -> v.partition(x -> true),
                        abc,
                        v -> v.partition(x -> x == 1),
                        Tuple.of(Vector.of(1), Vector.of(2, 3)))
                .keep(
                        "partition(Predicate)",
                        v -> v.partition(x -> false),
                        abc,
                        v -> v.partition(x -> x != 1),
                        Tuple.of(Vector.of(2, 3), Vector.of(1)))
                .keep(
                        "patch(int, Iterable, int)",
                        v -> v.patch(1, List.empty(), 0),
                        abc,
                        v -> v.patch(1, List.empty(), 1),
                        Vector.of(1, 3))
                .keep(
                        "prependAll(Iterable)",
                        v -> v.prependAll(List.empty()),
                        abc,
                        v -> v.prependAll(List.of(0)),
                        Vector.of(0, 1, 2, 3))
                .keep("reject(Predicate)", v -> v.reject(x -> false), abc, v -> v.reject(x -> x == 2), Vector.of(1, 3))
                .keep("remove(Object)", v -> v.remove(0), abc, v -> v.remove(2), Vector.of(1, 3))
                .keep(
                        "removeAll(Iterable)",
                        v -> v.removeAll(List.of(0)),
                        abc,
                        v -> v.removeAll(List.of(2)),
                        Vector.of(1, 3))
                .keep("removeAll(Object)", v -> v.removeAll(0), abc, v -> v.removeAll(2), Vector.of(1, 3))
                .keep(
                        "removeFirst(Predicate)",
                        v -> v.removeFirst(x -> false),
                        abc,
                        v -> v.removeFirst(x -> x == 2),
                        Vector.of(1, 3))
                .keep(
                        "removeLast(Predicate)",
                        v -> v.removeLast(x -> false),
                        abc,
                        v -> v.removeLast(x -> x == 2),
                        Vector.of(1, 3))
                .keep("replace(Object, Object)", v -> v.replace(0, 9), abc, v -> v.replace(2, 9), Vector.of(1, 9, 3))
                .keep(
                        "replaceAll(Object, Object)",
                        v -> v.replaceAll(0, 9),
                        abc,
                        v -> v.replaceAll(2, 9),
                        Vector.of(1, 9, 3))
                .keep("retainAll(Iterable)", v -> v.retainAll(v), abc, v -> v.retainAll(List.of(1, 3)), Vector.of(1, 3))
                .keepOn("reverse()", v -> v.take(1), Vector::reverse, abc, Vector::reverse, Vector.of(3, 2, 1))
                .keep("rotateLeft(int)", v -> v.rotateLeft(v.size()), abc, v -> v.rotateLeft(1), Vector.of(2, 3, 1))
                .keep("rotateRight(int)", v -> v.rotateRight(0), abc, v -> v.rotateRight(1), Vector.of(3, 1, 2))
                .keepOn("shuffle()", v -> v.take(1), Vector::shuffle, Vector.range(0, 100), Vector::shuffle, null)
                .keep("slice(int, int)", v -> v.slice(-1, Integer.MAX_VALUE), abc, v -> v.slice(1, 3), Vector.of(2, 3))
                .keepOn(
                        "sortBy(Comparator, Function)",
                        v -> v.take(1),
                        v -> v.sortBy(Comparator.naturalOrder(), x -> -x),
                        abc,
                        v -> v.sortBy(Comparator.naturalOrder(), x -> -x),
                        Vector.of(3, 2, 1))
                .keepOn(
                        "sortBy(Function)",
                        v -> v.take(1),
                        v -> v.sortBy(x -> -x),
                        abc,
                        v -> v.sortBy(x -> -x),
                        Vector.of(3, 2, 1))
                .keepOn(
                        "sorted()",
                        v -> v.take(1),
                        Vector::sorted,
                        Vector.of(3, 1, 2),
                        Vector::sorted,
                        Vector.of(1, 2, 3))
                .keepOn(
                        "sorted(Comparator)",
                        v -> v.take(1),
                        v -> v.sorted(Comparator.reverseOrder()),
                        abc,
                        v -> v.sorted(Comparator.reverseOrder()),
                        Vector.of(3, 2, 1))
                .keep(
                        "span(Predicate)",
                        v -> v.span(x -> true),
                        abc,
                        v -> v.span(x -> x == 1),
                        Tuple.of(Vector.of(1), Vector.of(2, 3)))
                .keep(
                        "span(Predicate)",
                        v -> v.span(x -> false),
                        abc,
                        v -> v.span(x -> x < 3),
                        Tuple.of(Vector.of(1, 2), Vector.of(3)))
                .keep(
                        "splitAt(Predicate)",
                        v -> v.splitAt(x -> false),
                        abc,
                        v -> v.splitAt(x -> x == 2),
                        Tuple.of(Vector.of(1), Vector.of(2, 3)))
                .keep(
                        "splitAt(int)",
                        v -> v.splitAt(v.size()),
                        abc,
                        v -> v.splitAt(1),
                        Tuple.of(Vector.of(1), Vector.of(2, 3)))
                .keep(
                        "splitAt(int)",
                        v -> v.splitAt(0),
                        abc,
                        v -> v.splitAt(2),
                        Tuple.of(Vector.of(1, 2), Vector.of(3)))
                .keep(
                        "splitAtInclusive(Predicate)",
                        v -> v.splitAtInclusive(x -> false),
                        abc,
                        v -> v.splitAtInclusive(x -> x == 2),
                        Tuple.of(Vector.of(1, 2), Vector.of(3)))
                .keep("subSequence(int)", v -> v.subSequence(0), abc, v -> v.subSequence(1), Vector.of(2, 3))
                .keep(
                        "subSequence(int, int)",
                        v -> v.subSequence(0, v.size()),
                        abc,
                        v -> v.subSequence(1, 3),
                        Vector.of(2, 3))
                .keep("take(int)", v -> v.take(v.size()), abc, v -> v.take(2), Vector.of(1, 2))
                .keep("takeRight(int)", v -> v.takeRight(Integer.MAX_VALUE), abc, v -> v.takeRight(2), Vector.of(2, 3))
                .keep(
                        "takeRightUntil(Predicate)",
                        v -> v.takeRightUntil(x -> false),
                        abc,
                        v -> v.takeRightUntil(x -> x == 1),
                        Vector.of(2, 3))
                .keep(
                        "takeRightWhile(Predicate)",
                        v -> v.takeRightWhile(x -> true),
                        abc,
                        v -> v.takeRightWhile(x -> x > 1),
                        Vector.of(2, 3))
                .keep(
                        "takeUntil(Predicate)",
                        v -> v.takeUntil(x -> false),
                        abc,
                        v -> v.takeUntil(x -> x == 3),
                        Vector.of(1, 2))
                .keep(
                        "takeWhile(Predicate)",
                        v -> v.takeWhile(x -> true),
                        abc,
                        v -> v.takeWhile(x -> x < 3),
                        Vector.of(1, 2))
                .same("tap(Consumer)", v -> v.tap(x -> {}))
                .same("toVector()", Vector::toVector)
                .keep(
                        "update(int, Function)",
                        v -> v.update(0, x -> x),
                        abc,
                        v -> v.update(0, x -> 9),
                        Vector.of(9, 2, 3))
                .keep(
                        "update(int, Object)",
                        v -> v.update(v.size() - 1, v.last()),
                        abc,
                        v -> v.update(2, 9),
                        Vector.of(1, 2, 9))
                .leave(ALWAYS_CHANGES, "append(Object)", "init()", "insert(int, Object)", "prepend(Object)")
                .leave(ALWAYS_CHANGES, "removeAt(int)", "scan(Object, BiFunction)", "tail()")
                .leave(NEW_COLLECTION, "duplicates()", "duplicatesBy(Function)")
                .leave(KEEP_LAST, "distinctByKeepLast(Comparator)", "distinctByKeepLast(Function)");
    }

    private static Table<List<Integer>> listTable() {
        List<Integer> abc = List.of(1, 2, 3);
        return new Table<List<Integer>>(List.class, ConservingOperationsTest::lists)
                .keep(
                        "appendAll(Iterable)",
                        l -> l.appendAll(Vector.empty()),
                        abc,
                        l -> l.appendAll(List.of(4)),
                        Vector.of(1, 2, 3, 4))
                .keep("distinct()", List::distinct, List.of(1, 2, 1), List::distinct, Vector.of(1, 2))
                .keep(
                        "distinctBy(Comparator)",
                        l -> l.distinctBy(Comparator.naturalOrder()),
                        List.of(1, 2, 1),
                        l -> l.distinctBy(Comparator.naturalOrder()),
                        Vector.of(1, 2))
                .keep(
                        "distinctBy(Function)",
                        l -> l.distinctBy(Function.identity()),
                        List.of(1, 2, 1),
                        l -> l.distinctBy(Function.identity()),
                        Vector.of(1, 2))
                .keep("drop(int)", l -> l.drop(0), abc, l -> l.drop(1), Vector.of(2, 3))
                .keep("dropRight(int)", l -> l.dropRight(0), abc, l -> l.dropRight(1), Vector.of(1, 2))
                .keep(
                        "dropRightUntil(Predicate)",
                        l -> l.dropRightUntil(x -> true),
                        abc,
                        l -> l.dropRightUntil(x -> x < 3),
                        Vector.of(1, 2))
                .keep(
                        "dropRightWhile(Predicate)",
                        l -> l.dropRightWhile(x -> false),
                        abc,
                        l -> l.dropRightWhile(x -> x == 3),
                        Vector.of(1, 2))
                .keep(
                        "dropUntil(Predicate)",
                        l -> l.dropUntil(x -> true),
                        abc,
                        l -> l.dropUntil(x -> x > 1),
                        Vector.of(2, 3))
                .keep(
                        "dropWhile(Predicate)",
                        l -> l.dropWhile(x -> false),
                        abc,
                        l -> l.dropWhile(x -> x == 1),
                        Vector.of(2, 3))
                .keep("filter(Predicate)", l -> l.filter(x -> true), abc, l -> l.filter(x -> x != 2), Vector.of(1, 3))
                .keep(
                        "insertAll(int, Iterable)",
                        l -> l.insertAll(l.size() / 2, Vector.empty()),
                        abc,
                        l -> l.insertAll(1, List.of(9)),
                        Vector.of(1, 9, 2, 3))
                .keepOn(
                        "intersperse(Object)",
                        l -> l.take(1),
                        l -> l.intersperse(0),
                        abc,
                        l -> l.intersperse(0),
                        Vector.of(1, 0, 2, 0, 3))
                .keep(
                        "leftPadTo(int, Object)",
                        l -> l.leftPadTo(l.size(), 0),
                        abc,
                        l -> l.leftPadTo(4, 0),
                        Vector.of(0, 1, 2, 3))
                .keep(
                        "orElse(Iterable)",
                        l -> l.orElse(List.of(9)),
                        List.empty(),
                        l -> l.orElse(Vector.of(9)),
                        Vector.of(9))
                .keep(
                        "orElse(Supplier)",
                        l -> l.orElse(() -> List.of(9)),
                        List.empty(),
                        l -> l.orElse(() -> Vector.of(9)),
                        Vector.of(9))
                .keep("padTo(int, Object)", l -> l.padTo(l.size(), 0), abc, l -> l.padTo(4, 0), Vector.of(1, 2, 3, 0))
                .keep(
                        "partition(Predicate)",
                        l -> l.partition(x -> true),
                        abc,
                        l -> l.partition(x -> x == 1),
                        Tuple.of(Vector.of(1), Vector.of(2, 3)))
                .keep(
                        "partition(Predicate)",
                        l -> l.partition(x -> false),
                        abc,
                        l -> l.partition(x -> x != 1),
                        Tuple.of(Vector.of(2, 3), Vector.of(1)))
                .keep(
                        "patch(int, Iterable, int)",
                        l -> l.patch(1, Vector.empty(), 0),
                        abc,
                        l -> l.patch(1, Vector.empty(), 1),
                        Vector.of(1, 3))
                .keep(
                        "prependAll(Iterable)",
                        l -> l.prependAll(Vector.empty()),
                        abc,
                        l -> l.prependAll(List.of(0)),
                        Vector.of(0, 1, 2, 3))
                .keep("push(Object[])", List::push, abc, l -> l.push(0), Vector.of(0, 1, 2, 3))
                .keep(
                        "pushAll(Iterable)",
                        l -> l.pushAll(List.empty()),
                        abc,
                        l -> l.pushAll(List.of(0)),
                        Vector.of(0, 1, 2, 3))
                .keep("reject(Predicate)", l -> l.reject(x -> false), abc, l -> l.reject(x -> x == 2), Vector.of(1, 3))
                .keep("remove(Object)", l -> l.remove(0), abc, l -> l.remove(2), Vector.of(1, 3))
                .keep(
                        "removeAll(Iterable)",
                        l -> l.removeAll(List.of(0)),
                        abc,
                        l -> l.removeAll(List.of(2)),
                        Vector.of(1, 3))
                .keep("removeAll(Object)", l -> l.removeAll(0), abc, l -> l.removeAll(2), Vector.of(1, 3))
                .keep(
                        "removeFirst(Predicate)",
                        l -> l.removeFirst(x -> false),
                        abc,
                        l -> l.removeFirst(x -> x == 2),
                        Vector.of(1, 3))
                .keep(
                        "removeLast(Predicate)",
                        l -> l.removeLast(x -> false),
                        abc,
                        l -> l.removeLast(x -> x == 2),
                        Vector.of(1, 3))
                .keep("replace(Object, Object)", l -> l.replace(0, 9), abc, l -> l.replace(2, 9), Vector.of(1, 9, 3))
                .keep(
                        "replaceAll(Object, Object)",
                        l -> l.replaceAll(0, 9),
                        abc,
                        l -> l.replaceAll(2, 9),
                        Vector.of(1, 9, 3))
                .keep("retainAll(Iterable)", l -> l.retainAll(l), abc, l -> l.retainAll(List.of(1, 3)), Vector.of(1, 3))
                .keepOn("reverse()", l -> l.take(1), List::reverse, abc, List::reverse, Vector.of(3, 2, 1))
                .keep("rotateLeft(int)", l -> l.rotateLeft(l.size()), abc, l -> l.rotateLeft(1), Vector.of(2, 3, 1))
                .keep("rotateRight(int)", l -> l.rotateRight(0), abc, l -> l.rotateRight(1), Vector.of(3, 1, 2))
                .keepOn("shuffle()", l -> l.take(1), List::shuffle, List.range(0, 100), List::shuffle, null)
                .keep("slice(int, int)", l -> l.slice(-1, Integer.MAX_VALUE), abc, l -> l.slice(1, 3), Vector.of(2, 3))
                .keepOn(
                        "sortBy(Comparator, Function)",
                        l -> l.take(1),
                        l -> l.sortBy(Comparator.naturalOrder(), x -> -x),
                        abc,
                        l -> l.sortBy(Comparator.naturalOrder(), x -> -x),
                        Vector.of(3, 2, 1))
                .keepOn(
                        "sortBy(Function)",
                        l -> l.take(1),
                        l -> l.sortBy(x -> -x),
                        abc,
                        l -> l.sortBy(x -> -x),
                        Vector.of(3, 2, 1))
                .keepOn("sorted()", l -> l.take(1), List::sorted, List.of(3, 1, 2), List::sorted, Vector.of(1, 2, 3))
                .keepOn(
                        "sorted(Comparator)",
                        l -> l.take(1),
                        l -> l.sorted(Comparator.reverseOrder()),
                        abc,
                        l -> l.sorted(Comparator.reverseOrder()),
                        Vector.of(3, 2, 1))
                .keep(
                        "span(Predicate)",
                        l -> l.span(x -> true),
                        abc,
                        l -> l.span(x -> x == 1),
                        Tuple.of(Vector.of(1), Vector.of(2, 3)))
                .keep(
                        "span(Predicate)",
                        l -> l.span(x -> false),
                        abc,
                        l -> l.span(x -> x < 3),
                        Tuple.of(Vector.of(1, 2), Vector.of(3)))
                .keep(
                        "splitAt(Predicate)",
                        l -> l.splitAt(x -> false),
                        abc,
                        l -> l.splitAt(x -> x == 2),
                        Tuple.of(Vector.of(1), Vector.of(2, 3)))
                .keep(
                        "splitAt(int)",
                        l -> l.splitAt(l.size()),
                        abc,
                        l -> l.splitAt(1),
                        Tuple.of(Vector.of(1), Vector.of(2, 3)))
                .keep(
                        "splitAt(int)",
                        l -> l.splitAt(0),
                        abc,
                        l -> l.splitAt(2),
                        Tuple.of(Vector.of(1, 2), Vector.of(3)))
                .keep(
                        "splitAtInclusive(Predicate)",
                        l -> l.splitAtInclusive(x -> false),
                        abc,
                        l -> l.splitAtInclusive(x -> x == 2),
                        Tuple.of(Vector.of(1, 2), Vector.of(3)))
                .keep("subSequence(int)", l -> l.subSequence(0), abc, l -> l.subSequence(1), Vector.of(2, 3))
                .keep(
                        "subSequence(int, int)",
                        l -> l.subSequence(0, l.size()),
                        abc,
                        l -> l.subSequence(1, 3),
                        Vector.of(2, 3))
                .keep("take(int)", l -> l.take(l.size()), abc, l -> l.take(2), Vector.of(1, 2))
                .keep("takeRight(int)", l -> l.takeRight(Integer.MAX_VALUE), abc, l -> l.takeRight(2), Vector.of(2, 3))
                .keep(
                        "takeRightUntil(Predicate)",
                        l -> l.takeRightUntil(x -> false),
                        abc,
                        l -> l.takeRightUntil(x -> x == 1),
                        Vector.of(2, 3))
                .keep(
                        "takeRightWhile(Predicate)",
                        l -> l.takeRightWhile(x -> true),
                        abc,
                        l -> l.takeRightWhile(x -> x > 1),
                        Vector.of(2, 3))
                .keep(
                        "takeUntil(Predicate)",
                        l -> l.takeUntil(x -> false),
                        abc,
                        l -> l.takeUntil(x -> x == 3),
                        Vector.of(1, 2))
                .keep(
                        "takeWhile(Predicate)",
                        l -> l.takeWhile(x -> true),
                        abc,
                        l -> l.takeWhile(x -> x < 3),
                        Vector.of(1, 2))
                .same("tap(Consumer)", l -> l.tap(x -> {}))
                .same("toList()", List::toList)
                .keep(
                        "update(int, Function)",
                        l -> l.update(0, x -> x),
                        abc,
                        l -> l.update(0, x -> 9),
                        Vector.of(9, 2, 3))
                .keep(
                        "update(int, Object)",
                        l -> l.update(l.size() - 1, l.last()),
                        abc,
                        l -> l.update(2, 9),
                        Vector.of(1, 2, 9))
                .leave(ALWAYS_CHANGES, "append(Object)", "init()", "insert(int, Object)", "prepend(Object)", "pop()")
                .leave(ALWAYS_CHANGES, "push(Object)", "removeAt(int)", "scan(Object, BiFunction)", "tail()")
                .leave(NEW_COLLECTION, "duplicates()", "duplicatesBy(Function)")
                .leave(KEEP_LAST, "distinctByKeepLast(Comparator)", "distinctByKeepLast(Function)");
    }

    private static Table<Queue<Integer>> queueTable() {
        Queue<Integer> abc = Queue.of(1, 2, 3);
        return new Table<Queue<Integer>>(Queue.class, ConservingOperationsTest::queues)
                .keep(
                        "appendAll(Iterable)",
                        q -> q.appendAll(List.empty()),
                        abc,
                        q -> q.appendAll(List.of(4)),
                        Vector.of(1, 2, 3, 4))
                .keep("distinct()", Queue::distinct, Queue.of(1, 2, 1), Queue::distinct, Vector.of(1, 2))
                .keep(
                        "distinctBy(Comparator)",
                        q -> q.distinctBy(Comparator.naturalOrder()),
                        Queue.of(1, 2, 1),
                        q -> q.distinctBy(Comparator.naturalOrder()),
                        Vector.of(1, 2))
                .keep(
                        "distinctBy(Function)",
                        q -> q.distinctBy(Function.identity()),
                        Queue.of(1, 2, 1),
                        q -> q.distinctBy(Function.identity()),
                        Vector.of(1, 2))
                .keep("drop(int)", q -> q.drop(0), abc, q -> q.drop(1), Vector.of(2, 3))
                .keep("dropRight(int)", q -> q.dropRight(0), abc, q -> q.dropRight(1), Vector.of(1, 2))
                .keep(
                        "dropRightUntil(Predicate)",
                        q -> q.dropRightUntil(x -> true),
                        abc,
                        q -> q.dropRightUntil(x -> x < 3),
                        Vector.of(1, 2))
                .keep(
                        "dropRightWhile(Predicate)",
                        q -> q.dropRightWhile(x -> false),
                        abc,
                        q -> q.dropRightWhile(x -> x == 3),
                        Vector.of(1, 2))
                .keep(
                        "dropUntil(Predicate)",
                        q -> q.dropUntil(x -> true),
                        abc,
                        q -> q.dropUntil(x -> x > 1),
                        Vector.of(2, 3))
                .keep(
                        "dropWhile(Predicate)",
                        q -> q.dropWhile(x -> false),
                        abc,
                        q -> q.dropWhile(x -> x == 1),
                        Vector.of(2, 3))
                .keep("enqueue(Object[])", Queue::enqueue, abc, q -> q.enqueue(4, 5), Vector.of(1, 2, 3, 4, 5))
                .keep(
                        "enqueueAll(Iterable)",
                        q -> q.enqueueAll(List.empty()),
                        abc,
                        q -> q.enqueueAll(List.of(4)),
                        Vector.of(1, 2, 3, 4))
                .keep("filter(Predicate)", q -> q.filter(x -> true), abc, q -> q.filter(x -> x != 2), Vector.of(1, 3))
                .keep(
                        "insertAll(int, Iterable)",
                        q -> q.insertAll(q.size(), Vector.empty()),
                        abc,
                        q -> q.insertAll(1, List.of(9)),
                        Vector.of(1, 9, 2, 3))
                .keep(
                        "insertAll(int, Iterable)",
                        q -> q.insertAll(1, Vector.empty()),
                        abc,
                        q -> q.insertAll(3, List.of(9)),
                        Vector.of(1, 2, 3, 9))
                .keepOn(
                        "intersperse(Object)",
                        q -> q.take(1),
                        q -> q.intersperse(0),
                        abc,
                        q -> q.intersperse(0),
                        Vector.of(1, 0, 2, 0, 3))
                .keep(
                        "leftPadTo(int, Object)",
                        q -> q.leftPadTo(q.size(), 0),
                        abc,
                        q -> q.leftPadTo(4, 0),
                        Vector.of(0, 1, 2, 3))
                .keep(
                        "orElse(Iterable)",
                        q -> q.orElse(List.of(9)),
                        Queue.empty(),
                        q -> q.orElse(List.of(9)),
                        Vector.of(9))
                .keep(
                        "orElse(Supplier)",
                        q -> q.orElse(() -> List.of(9)),
                        Queue.empty(),
                        q -> q.orElse(() -> List.of(9)),
                        Vector.of(9))
                .keep("padTo(int, Object)", q -> q.padTo(q.size(), 0), abc, q -> q.padTo(4, 0), Vector.of(1, 2, 3, 0))
                .keep(
                        "partition(Predicate)",
                        q -> q.partition(x -> true),
                        abc,
                        q -> q.partition(x -> x == 1),
                        Tuple.of(Vector.of(1), Vector.of(2, 3)))
                .keep(
                        "partition(Predicate)",
                        q -> q.partition(x -> false),
                        abc,
                        q -> q.partition(x -> x != 1),
                        Tuple.of(Vector.of(2, 3), Vector.of(1)))
                .keep(
                        "patch(int, Iterable, int)",
                        q -> q.patch(1, List.empty(), 0),
                        abc,
                        q -> q.patch(1, List.empty(), 1),
                        Vector.of(1, 3))
                .keep(
                        "prependAll(Iterable)",
                        q -> q.prependAll(List.empty()),
                        abc,
                        q -> q.prependAll(List.of(0)),
                        Vector.of(0, 1, 2, 3))
                .keep("reject(Predicate)", q -> q.reject(x -> false), abc, q -> q.reject(x -> x == 2), Vector.of(1, 3))
                .keep("remove(Object)", q -> q.remove(0), abc, q -> q.remove(2), Vector.of(1, 3))
                .keep(
                        "removeAll(Iterable)",
                        q -> q.removeAll(List.of(0)),
                        abc,
                        q -> q.removeAll(List.of(2)),
                        Vector.of(1, 3))
                .keep("removeAll(Object)", q -> q.removeAll(0), abc, q -> q.removeAll(2), Vector.of(1, 3))
                .keep(
                        "removeFirst(Predicate)",
                        q -> q.removeFirst(x -> false),
                        abc,
                        q -> q.removeFirst(x -> x == 2),
                        Vector.of(1, 3))
                .keep(
                        "removeLast(Predicate)",
                        q -> q.removeLast(x -> false),
                        abc,
                        q -> q.removeLast(x -> x == 2),
                        Vector.of(1, 3))
                .keep("replace(Object, Object)", q -> q.replace(0, 9), abc, q -> q.replace(2, 9), Vector.of(1, 9, 3))
                .keep(
                        "replaceAll(Object, Object)",
                        q -> q.replaceAll(0, 9),
                        abc,
                        q -> q.replaceAll(2, 9),
                        Vector.of(1, 9, 3))
                .keep("retainAll(Iterable)", q -> q.retainAll(q), abc, q -> q.retainAll(List.of(1, 3)), Vector.of(1, 3))
                .keepOn("reverse()", q -> q.take(1), Queue::reverse, abc, Queue::reverse, Vector.of(3, 2, 1))
                .keep("rotateLeft(int)", q -> q.rotateLeft(q.size()), abc, q -> q.rotateLeft(1), Vector.of(2, 3, 1))
                .keep("rotateRight(int)", q -> q.rotateRight(0), abc, q -> q.rotateRight(1), Vector.of(3, 1, 2))
                .keepOn(
                        "shuffle()",
                        q -> q.take(1),
                        Queue::shuffle,
                        Queue.ofAll(List.range(0, 100)),
                        Queue::shuffle,
                        null)
                .keep("slice(int, int)", q -> q.slice(-1, Integer.MAX_VALUE), abc, q -> q.slice(1, 3), Vector.of(2, 3))
                .keepOn(
                        "sortBy(Comparator, Function)",
                        q -> q.take(1),
                        q -> q.sortBy(Comparator.naturalOrder(), x -> -x),
                        abc,
                        q -> q.sortBy(Comparator.naturalOrder(), x -> -x),
                        Vector.of(3, 2, 1))
                .keepOn(
                        "sortBy(Function)",
                        q -> q.take(1),
                        q -> q.sortBy(x -> -x),
                        abc,
                        q -> q.sortBy(x -> -x),
                        Vector.of(3, 2, 1))
                .keepOn("sorted()", q -> q.take(1), Queue::sorted, Queue.of(3, 1, 2), Queue::sorted, Vector.of(1, 2, 3))
                .keepOn(
                        "sorted(Comparator)",
                        q -> q.take(1),
                        q -> q.sorted(Comparator.reverseOrder()),
                        abc,
                        q -> q.sorted(Comparator.reverseOrder()),
                        Vector.of(3, 2, 1))
                .keep(
                        "span(Predicate)",
                        q -> q.span(x -> true),
                        abc,
                        q -> q.span(x -> x == 1),
                        Tuple.of(Vector.of(1), Vector.of(2, 3)))
                .keep(
                        "span(Predicate)",
                        q -> q.span(x -> false),
                        abc,
                        q -> q.span(x -> x < 3),
                        Tuple.of(Vector.of(1, 2), Vector.of(3)))
                .keep(
                        "splitAt(Predicate)",
                        q -> q.splitAt(x -> false),
                        abc,
                        q -> q.splitAt(x -> x == 2),
                        Tuple.of(Vector.of(1), Vector.of(2, 3)))
                .keep(
                        "splitAt(Predicate)",
                        q -> q.splitAt(x -> true),
                        abc,
                        q -> q.splitAt(x -> x == 3),
                        Tuple.of(Vector.of(1, 2), Vector.of(3)))
                .keep(
                        "splitAt(int)",
                        q -> q.splitAt(q.size()),
                        abc,
                        q -> q.splitAt(1),
                        Tuple.of(Vector.of(1), Vector.of(2, 3)))
                .keep(
                        "splitAt(int)",
                        q -> q.splitAt(0),
                        abc,
                        q -> q.splitAt(2),
                        Tuple.of(Vector.of(1, 2), Vector.of(3)))
                .keep(
                        "splitAtInclusive(Predicate)",
                        q -> q.splitAtInclusive(x -> false),
                        abc,
                        q -> q.splitAtInclusive(x -> x == 2),
                        Tuple.of(Vector.of(1, 2), Vector.of(3)))
                .keep("subSequence(int)", q -> q.subSequence(0), abc, q -> q.subSequence(1), Vector.of(2, 3))
                .keep(
                        "subSequence(int, int)",
                        q -> q.subSequence(0, q.size()),
                        abc,
                        q -> q.subSequence(1, 3),
                        Vector.of(2, 3))
                .keep("take(int)", q -> q.take(q.size()), abc, q -> q.take(2), Vector.of(1, 2))
                .keep("takeRight(int)", q -> q.takeRight(Integer.MAX_VALUE), abc, q -> q.takeRight(2), Vector.of(2, 3))
                .keep(
                        "takeRightUntil(Predicate)",
                        q -> q.takeRightUntil(x -> false),
                        abc,
                        q -> q.takeRightUntil(x -> x == 1),
                        Vector.of(2, 3))
                .keep(
                        "takeRightWhile(Predicate)",
                        q -> q.takeRightWhile(x -> true),
                        abc,
                        q -> q.takeRightWhile(x -> x > 1),
                        Vector.of(2, 3))
                .keep(
                        "takeUntil(Predicate)",
                        q -> q.takeUntil(x -> false),
                        abc,
                        q -> q.takeUntil(x -> x == 3),
                        Vector.of(1, 2))
                .keep(
                        "takeWhile(Predicate)",
                        q -> q.takeWhile(x -> true),
                        abc,
                        q -> q.takeWhile(x -> x < 3),
                        Vector.of(1, 2))
                .same("tap(Consumer)", q -> q.tap(x -> {}))
                .same("toQueue()", Queue::toQueue)
                .keep(
                        "update(int, Function)",
                        q -> q.update(0, x -> x),
                        abc,
                        q -> q.update(0, x -> 9),
                        Vector.of(9, 2, 3))
                .keep(
                        "update(int, Object)",
                        q -> q.update(q.size() - 1, q.last()),
                        abc,
                        q -> q.update(2, 9),
                        Vector.of(1, 2, 9))
                .leave(ALWAYS_CHANGES, "append(Object)", "enqueue(Object)", "init()", "insert(int, Object)")
                .leave(ALWAYS_CHANGES, "prepend(Object)", "removeAt(int)", "scan(Object, BiFunction)", "tail()")
                .leave(NEW_COLLECTION, "duplicates()", "duplicatesBy(Function)")
                .leave(KEEP_LAST, "distinctByKeepLast(Comparator)", "distinctByKeepLast(Function)");
    }

    private static Table<LazyList<Integer>> lazyListTable() {
        LazyList<Integer> abc = LazyList.of(1, 2, 3);
        return new Table<LazyList<Integer>>(LazyList.class, ConservingOperationsTest::lazyLists)
                .keep(
                        "appendAll(Iterable)",
                        l -> l.appendAll(List.empty()),
                        abc,
                        l -> l.appendAll(List.of(4)),
                        Vector.of(1, 2, 3, 4))
                .keep("cycle(int)", l -> l.cycle(1), abc, l -> l.cycle(2), Vector.of(1, 2, 3, 1, 2, 3))
                .keep("drop(int)", l -> l.drop(0), abc, l -> l.drop(1), Vector.of(2, 3))
                .keep("dropRight(int)", l -> l.dropRight(0), abc, l -> l.dropRight(1), Vector.of(1, 2))
                .keep(
                        "leftPadTo(int, Object)",
                        l -> l.leftPadTo(0, 0),
                        abc,
                        l -> l.leftPadTo(4, 0),
                        Vector.of(0, 1, 2, 3))
                .keep(
                        "orElse(Iterable)",
                        l -> l.orElse(List.of(9)),
                        LazyList.empty(),
                        l -> l.orElse(List.of(9)),
                        Vector.of(9))
                .keep(
                        "orElse(Supplier)",
                        l -> l.orElse(() -> List.of(9)),
                        LazyList.empty(),
                        l -> l.orElse(() -> List.of(9)),
                        Vector.of(9))
                .keep("padTo(int, Object)", l -> l.padTo(0, 0), abc, l -> l.padTo(4, 0), Vector.of(1, 2, 3, 0))
                .keep(
                        "patch(int, Iterable, int)",
                        l -> l.patch(1, List.empty(), 0),
                        abc,
                        l -> l.patch(1, List.empty(), 1),
                        Vector.of(1, 3))
                .keep(
                        "prependAll(Iterable)",
                        l -> l.prependAll(List.empty()),
                        abc,
                        l -> l.prependAll(List.of(0)),
                        Vector.of(0, 1, 2, 3))
                .keep(
                        "removeAll(Iterable)",
                        l -> l.removeAll(List.empty()),
                        abc,
                        l -> l.removeAll(List.of(2)),
                        Vector.of(1, 3))
                .keepOn("reverse()", l -> l.take(1), LazyList::reverse, abc, LazyList::reverse, Vector.of(3, 2, 1))
                .keep("rotateLeft(int)", l -> l.rotateLeft(0), abc, l -> l.rotateLeft(1), Vector.of(2, 3, 1))
                .keep("rotateRight(int)", l -> l.rotateRight(0), abc, l -> l.rotateRight(1), Vector.of(3, 1, 2))
                .keepOn("shuffle()", l -> l.take(1), LazyList::shuffle, LazyList.range(0, 100), LazyList::shuffle, null)
                .keepOn(
                        "sortBy(Comparator, Function)",
                        l -> l.take(1),
                        l -> l.sortBy(Comparator.naturalOrder(), x -> -x),
                        abc,
                        l -> l.sortBy(Comparator.naturalOrder(), x -> -x),
                        Vector.of(3, 2, 1))
                .keepOn(
                        "sortBy(Function)",
                        l -> l.take(1),
                        l -> l.sortBy(x -> -x),
                        abc,
                        l -> l.sortBy(x -> -x),
                        Vector.of(3, 2, 1))
                .keepOn(
                        "sorted()",
                        l -> l.take(1),
                        LazyList::sorted,
                        LazyList.of(3, 1, 2),
                        LazyList::sorted,
                        Vector.of(1, 2, 3))
                .keepOn(
                        "sorted(Comparator)",
                        l -> l.take(1),
                        l -> l.sorted(Comparator.reverseOrder()),
                        abc,
                        l -> l.sorted(Comparator.reverseOrder()),
                        Vector.of(3, 2, 1))
                .keep(
                        "splitAt(int)",
                        l -> l.splitAt(0),
                        abc,
                        l -> l.splitAt(1),
                        Tuple.of(Vector.of(1), Vector.of(2, 3)))
                .keep("subSequence(int)", l -> l.subSequence(0), abc, l -> l.subSequence(1), Vector.of(2, 3))
                .same("toLazyList()", LazyList::toLazyList)
                .leave(ALWAYS_CHANGES, "append(Object)", "appendSelf(Function)", "cycle()", "extend(Function)")
                .leave(ALWAYS_CHANGES, "extend(Object)", "extend(Supplier)", "init()", "insert(int, Object)")
                .leave(ALWAYS_CHANGES, "prepend(Object)", "removeAt(int)", "scan(Object, BiFunction)", "tail()")
                .leave(NEW_COLLECTION, "duplicates()", "duplicatesBy(Function)")
                .leave(KEEP_LAST, "distinctByKeepLast(Comparator)", "distinctByKeepLast(Function)")
                .leave(
                        LAZY,
                        "distinct()",
                        "distinctBy(Comparator)",
                        "distinctBy(Function)",
                        "dropRightUntil(Predicate)")
                .leave(LAZY, "dropRightWhile(Predicate)", "dropUntil(Predicate)", "dropWhile(Predicate)")
                .leave(
                        LAZY,
                        "filter(Predicate)",
                        "insertAll(int, Iterable)",
                        "intersperse(Object)",
                        "partition(Predicate)")
                .leave(LAZY, "reject(Predicate)", "remove(Object)", "removeAll(Object)", "removeFirst(Predicate)")
                .leave(LAZY, "removeLast(Predicate)", "replace(Object, Object)", "replaceAll(Object, Object)")
                .leave(LAZY, "retainAll(Iterable)", "slice(int, int)", "span(Predicate)", "splitAt(Predicate)")
                .leave(LAZY, "splitAtInclusive(Predicate)", "subSequence(int, int)", "take(int)", "takeRight(int)")
                .leave(LAZY, "takeRightUntil(Predicate)", "takeRightWhile(Predicate)", "takeUntil(Predicate)")
                .leave(LAZY, "takeWhile(Predicate)", "tap(Consumer)", "update(int, Function)", "update(int, Object)");
    }

    private static Table<NonEmptyVector<Integer>> nonEmptyVectorTable() {
        NonEmptyVector<Integer> abc = NonEmptyVector.of(1, 2, 3);
        NonEmptyVector<Integer> aba = NonEmptyVector.of(1, 2, 1);
        return new Table<NonEmptyVector<Integer>>(NonEmptyVector.class, ConservingOperationsTest::nonEmptyVectors)
                .keep(
                        "appendAll(Iterable)",
                        n -> n.appendAll(List.<Integer>empty()),
                        abc,
                        n -> n.appendAll(List.of(4)),
                        Vector.of(1, 2, 3, 4))
                .keep(
                        "appendAll(Vector)",
                        n -> n.appendAll(Vector.<Integer>empty()),
                        abc,
                        n -> n.appendAll(Vector.of(4)),
                        Vector.of(1, 2, 3, 4))
                .keep("distinct()", NonEmptyVector::distinct, aba, NonEmptyVector::distinct, Vector.of(1, 2))
                .keep(
                        "distinctBy(Comparator)",
                        n -> n.distinctBy(Comparator.naturalOrder()),
                        aba,
                        n -> n.distinctBy(Comparator.naturalOrder()),
                        Vector.of(1, 2))
                .keep(
                        "distinctBy(Function)",
                        n -> n.distinctBy(Function.identity()),
                        aba,
                        n -> n.distinctBy(Function.identity()),
                        Vector.of(1, 2))
                .keep(
                        "insertAll(int, Iterable)",
                        n -> n.insertAll(1, List.empty()),
                        abc,
                        n -> n.insertAll(1, List.of(9)),
                        Vector.of(1, 9, 2, 3))
                .keepOn(
                        "intersperse(Object)",
                        n -> NonEmptyVector.of(n.head()),
                        n -> n.intersperse(0),
                        abc,
                        n -> n.intersperse(0),
                        Vector.of(1, 0, 2, 0, 3))
                .keep(
                        "leftPadTo(int, Object)",
                        n -> n.leftPadTo(n.size(), 0),
                        abc,
                        n -> n.leftPadTo(4, 0),
                        Vector.of(0, 1, 2, 3))
                .keep("padTo(int, Object)", n -> n.padTo(n.size(), 0), abc, n -> n.padTo(4, 0), Vector.of(1, 2, 3, 0))
                .keep(
                        "prependAll(Iterable)",
                        n -> n.prependAll(List.<Integer>empty()),
                        abc,
                        n -> n.prependAll(List.of(0)),
                        Vector.of(0, 1, 2, 3))
                .keep(
                        "prependAll(Vector)",
                        n -> n.prependAll(Vector.<Integer>empty()),
                        abc,
                        n -> n.prependAll(Vector.of(0)),
                        Vector.of(0, 1, 2, 3))
                .keep("replace(Object, Object)", n -> n.replace(0, 9), abc, n -> n.replace(2, 9), Vector.of(1, 9, 3))
                .keep(
                        "replaceAll(Object, Object)",
                        n -> n.replaceAll(0, 9),
                        abc,
                        n -> n.replaceAll(2, 9),
                        Vector.of(1, 9, 3))
                .keepOn(
                        "reverse()",
                        n -> NonEmptyVector.of(n.head()),
                        NonEmptyVector::reverse,
                        abc,
                        NonEmptyVector::reverse,
                        Vector.of(3, 2, 1))
                .keep("rotateLeft(int)", n -> n.rotateLeft(0), abc, n -> n.rotateLeft(1), Vector.of(2, 3, 1))
                .keep("rotateRight(int)", n -> n.rotateRight(n.size()), abc, n -> n.rotateRight(1), Vector.of(3, 1, 2))
                .keepOn(
                        "shuffle()",
                        n -> NonEmptyVector.of(n.head()),
                        NonEmptyVector::shuffle,
                        Vector.range(0, 100).toNonEmptyVector().get(),
                        NonEmptyVector::shuffle,
                        null)
                .keepOn(
                        "sortBy(Comparator, Function)",
                        n -> NonEmptyVector.of(n.head()),
                        n -> n.sortBy(Comparator.naturalOrder(), x -> -x),
                        abc,
                        n -> n.sortBy(Comparator.naturalOrder(), x -> -x),
                        Vector.of(3, 2, 1))
                .keepOn(
                        "sortBy(Function)",
                        n -> NonEmptyVector.of(n.head()),
                        n -> n.sortBy(x -> -x),
                        abc,
                        n -> n.sortBy(x -> -x),
                        Vector.of(3, 2, 1))
                .keepOn(
                        "sorted()",
                        n -> NonEmptyVector.of(n.head()),
                        NonEmptyVector::sorted,
                        NonEmptyVector.of(3, 1, 2),
                        NonEmptyVector::sorted,
                        Vector.of(1, 2, 3))
                .keepOn(
                        "sorted(Comparator)",
                        n -> NonEmptyVector.of(n.head()),
                        n -> n.sorted(Comparator.reverseOrder()),
                        abc,
                        n -> n.sorted(Comparator.reverseOrder()),
                        Vector.of(3, 2, 1))
                .same("tap(Consumer)", n -> n.tap(x -> {}))
                .keep(
                        "update(int, Function)",
                        n -> n.update(0, x -> x),
                        abc,
                        n -> n.update(0, x -> 9),
                        Vector.of(9, 2, 3))
                .keep("update(int, Object)", n -> n.update(0, n.head()), abc, n -> n.update(2, 9), Vector.of(1, 2, 9))
                .leave(
                        ALWAYS_CHANGES,
                        "append(Object)",
                        "insert(int, Object)",
                        "prepend(Object)",
                        "scan(Object, BiFunction)")
                .leave(NEVER_EMPTY, "appendAll(NonEmptyVector)", "concat(NonEmptyVector)", "prependAll(NonEmptyVector)")
                .leave(KEEP_LAST, "distinctByKeepLast(Comparator)", "distinctByKeepLast(Function)");
    }

    // -- the checks

    @Test
    void everyOperationOfEverySequenceHasARow() {
        assertCovered(vectorTable());
        assertCovered(listTable());
        assertCovered(queueTable());
        assertCovered(lazyListTable());
        assertCovered(nonEmptyVectorTable());
    }

    @Test
    void vectorReturnsItselfWhenNothingChanges() {
        check(vectorTable());
    }

    @Test
    void listReturnsItselfWhenNothingChanges() {
        check(listTable());
    }

    @Test
    void queueReturnsItselfWhenNothingChanges() {
        check(queueTable());
    }

    @Test
    void lazyListReturnsItselfWhenNothingChanges() {
        check(lazyListTable());
    }

    @Test
    void nonEmptyVectorReturnsItselfWhenNothingChanges() {
        check(nonEmptyVectorTable());
    }

    // -- sharing, call order and arguments

    @Test
    void listFilterSharesTheCellsAfterTheLastRemovedElement() {
        List<Integer> list = List.range(0, 10);
        List<Integer> filtered = list.filter(x -> x != 3 && x != 6);
        assertThat(filtered).containsExactly(0, 1, 2, 4, 5, 7, 8, 9);
        assertThat(filtered.drop(5)).isSameAs(list.drop(7));
        assertThat(list.filter(x -> x != 9)).containsExactly(0, 1, 2, 3, 4, 5, 6, 7, 8);
        assertThat(list.filter(x -> x != 0)).isSameAs(list.tail());
        assertThat(list.filter(x -> false)).isEmpty();
        assertThat(list).containsExactly(0, 1, 2, 3, 4, 5, 6, 7, 8, 9);
    }

    @Test
    void listFilterCallsThePredicateOncePerElementInOrder() {
        java.util.List<Integer> calls = new ArrayList<>();
        List<Integer> kept = List.range(0, 6).filter(x -> {
            calls.add(x);
            return x % 2 == 0;
        });
        assertThat(kept).containsExactly(0, 2, 4);
        assertThat(calls).containsExactly(0, 1, 2, 3, 4, 5);
        // distinctBy filters with a predicate that remembers what it has seen
        assertThat(List.of(1, 2, 1, 3, 2, 4).distinct()).containsExactly(1, 2, 3, 4);
    }

    @Test
    void listReplaceAllSharesTheCellsAfterTheLastReplacedElement() {
        List<Integer> list = List.of(0, 1, 2, 1, 4, 5);
        List<Integer> replaced = list.replaceAll(1, 9);
        assertThat(replaced).containsExactly(0, 9, 2, 9, 4, 5);
        assertThat(replaced.drop(4)).isSameAs(list.drop(4));
        assertThat(list.replaceAll(7, null)).isSameAs(list);
        assertThatThrownBy(() -> list.replaceAll(1, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("List: element is null");
    }

    @Test
    void listUpdateSharesTheCellsAfterTheUpdatedOne() {
        List<Integer> list = List.range(0, 10);
        List<Integer> updated = list.update(3, 9);
        assertThat(updated).containsExactly(0, 1, 2, 9, 4, 5, 6, 7, 8, 9);
        assertThat(updated.drop(4)).isSameAs(list.drop(4));
        assertThat(list.update(0, 9).tail()).isSameAs(list.tail());
        assertThatThrownBy(() -> list.update(10, 1))
                .isInstanceOf(IndexOutOfBoundsException.class)
                .hasMessage("update(10, e) on List of length 10");
        assertThatThrownBy(() -> list.update(3, (Integer) null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("List: element is null");
    }

    @Test
    void anUpdateWithAnEqualElementThatIsNotTheSameIsAChange() {
        // outside the Integer cache: two boxings of 1000 are two instances
        Integer stored = Integer.valueOf(1000);
        Integer other = Integer.valueOf(1000);
        assertThat(other).isEqualTo(stored).isNotSameAs(stored);
        assertThat(Vector.of(stored).update(0, other))
                .isNotSameAs(Vector.of(stored))
                .containsExactly(1000);
        List<Integer> list = List.of(stored);
        assertThat(list.update(0, other)).isNotSameAs(list).containsExactly(1000);
        Queue<Integer> queue = Queue.of(stored);
        assertThat(queue.update(0, other)).isNotSameAs(queue).containsExactly(1000);
        Vector<Integer> vector = Vector.of(stored);
        assertThat(vector.update(0, stored)).isSameAs(vector);
    }

    @Test
    void listSpanAndSplitAtShareTheRest() {
        List<Integer> list = List.range(0, 10);
        java.util.List<Integer> calls = new ArrayList<>();
        Tuple2<List<Integer>, List<Integer>> span = list.span(x -> {
            calls.add(x);
            return x < 3;
        });
        assertThat(span._1()).containsExactly(0, 1, 2);
        assertThat(span._2()).isSameAs(list.drop(3));
        assertThat(calls).containsExactly(0, 1, 2, 3);
        Tuple2<List<Integer>, List<Integer>> split = list.splitAt(4);
        assertThat(split._1()).containsExactly(0, 1, 2, 3);
        assertThat(split._2()).isSameAs(list.drop(4));
        assertThat(List.<Integer>empty().span(x -> true)._1()).isEmpty();
        assertThat(List.<Integer>empty().splitAt(1)._2()).isEmpty();
    }

    @Test
    void anEmptyOneShotArgumentIsReadOnce() {
        Vector<Integer> vector = Vector.of(1, 2, 3);
        assertThat(vector.insertAll(1, once())).isEqualTo(vector);
        assertThat(vector.insertAll(1, once(9))).containsExactly(1, 9, 2, 3);
        assertThat(vector.patch(1, once(), 0)).isEqualTo(vector);
        List<Integer> list = List.of(1, 2, 3);
        assertThat(list.insertAll(1, once())).isEqualTo(list);
        assertThat(list.insertAll(1, once(9))).containsExactly(1, 9, 2, 3);
        assertThat(list.patch(1, once(), 0)).isEqualTo(list);
        assertThat(list.patch(1, once(9), 0)).containsExactly(1, 9, 2, 3);
        Queue<Integer> queue = Queue.of(1, 2, 3);
        assertThat(queue.patch(1, once(), 0)).isEqualTo(queue);
        LazyList<Integer> lazyList = LazyList.of(1, 2, 3);
        assertThat(lazyList.patch(1, once(), 0)).isEqualTo(lazyList);
        assertThat(lazyList.patch(1, once(9), 0)).containsExactly(1, 9, 2, 3);
    }

    @Test
    void anInsertionOfNothingStillChecksTheIndex() {
        assertThatThrownBy(() -> List.of(1, 2, 3).insertAll(5, List.empty()))
                .isInstanceOf(IndexOutOfBoundsException.class)
                .hasMessage("insertAll(5, elements) on List of length 3");
        assertThatThrownBy(() -> List.of(1, 2, 3).insertAll(-1, List.empty()))
                .isInstanceOf(IndexOutOfBoundsException.class)
                .hasMessage("insertAll(-1, elements)");
        assertThatThrownBy(() -> Vector.of(1, 2, 3).insertAll(4, List.empty()))
                .isInstanceOf(IndexOutOfBoundsException.class);
        assertThatThrownBy(() -> Queue.of(1, 2, 3).insertAll(4, List.empty()))
                .isInstanceOf(IndexOutOfBoundsException.class);
    }

    @Test
    void aConcatenationWithAnEmptyReceiverIsTheArgument() {
        Vector<Integer> vector = Vector.of(1, 2);
        assertThat(Vector.<Integer>empty().appendAll(vector)).isSameAs(vector);
        assertThat(Vector.<Integer>empty().prependAll(vector)).isSameAs(vector);
        List<Integer> list = List.of(1, 2);
        assertThat(List.<Integer>empty().appendAll(list)).isSameAs(list);
        assertThat(List.<Integer>empty().prependAll(list)).isSameAs(list);
        Queue<Integer> queue = Queue.of(1, 2);
        assertThat(Queue.<Integer>empty().appendAll(queue)).isSameAs(queue);
        assertThat(Queue.<Integer>empty().prependAll(queue)).isSameAs(queue);
        LazyList<Integer> lazyList = LazyList.of(1, 2);
        assertThat(LazyList.<Integer>empty().appendAll(lazyList)).isSameAs(lazyList);
        assertThat(LazyList.<Integer>empty().prependAll(lazyList)).isSameAs(lazyList);
    }

    @Test
    void theLazyListChecksReadNothing() {
        LazyList<Integer> unread = LazyList.continually(() -> {
            throw new AssertionError("an element was read");
        });
        assertThat(unread.patch(3, List.empty(), 0)).isSameAs(unread);
        assertThat(unread.cycle(1)).isSameAs(unread);
        assertThat(unread.appendAll(List.empty())).isSameAs(unread);
        // not known to be non-empty: a lazy alternative, still unread
        assertThat(unread.orElse(List.of(9))).isNotSameAs(unread);
        LazyList<Integer> read = LazyList.range(1, 4);
        LazyList<Integer> pending = read.orElse(List.of(9));
        assertThat(pending).isNotSameAs(read).containsExactly(1, 2, 3);
        assertThat(read.orElse(List.of(9))).isSameAs(read);
        assertThat(LazyList.<Integer>empty().orElse(() -> List.of(9))).containsExactly(9);
    }

    @SafeVarargs
    private static Iterable<Integer> once(Integer... elements) {
        java.util.Iterator<Integer> iterator = java.util.Arrays.asList(elements).iterator();
        boolean[] used = {false};
        return () -> {
            if (used[0]) {
                throw new IllegalStateException("read twice");
            }
            used[0] = true;
            return iterator;
        };
    }

    private static <C> void assertCovered(Table<C> table) {
        TreeSet<String> rows = new TreeSet<>(table.left.keySet());
        for (Row<C> row : table.rows) {
            assertThat(table.left)
                    .as(table.type.getSimpleName() + " both keeps and leaves " + row.signature())
                    .doesNotContainKey(row.signature());
            rows.add(row.signature());
        }
        assertThat(rows).as(table.type.getSimpleName() + ": the rows").isEqualTo(sameTypeOperations(table.type));
    }

    // every failing row reported at once
    private static <C> void check(Table<C> table) {
        SoftAssertions softly = new SoftAssertions();
        for (Row<C> row : table.rows) {
            for (C receiver : table.receivers.get()) {
                C prepared = row.prepare().apply(receiver);
                Vector<Object> before = Vector.ofAll(elements(prepared));
                Object result = row.noOp().apply(prepared);
                String label =
                        table.type.getSimpleName() + "." + row.signature() + " on " + before.size() + " elements";
                if (result instanceof Tuple2<?, ?> parts) {
                    boolean leftIsWhole = parts._1() == prepared;
                    softly.assertThat(leftIsWhole || parts._2() == prepared)
                            .as(label + ": a side is the receiver")
                            .isTrue();
                    softly.assertThat(elements(leftIsWhole ? parts._2() : parts._1()))
                            .as(label + ": the other side")
                            .isEmpty();
                } else {
                    softly.assertThat(result).as(label).isSameAs(prepared);
                }
                softly.assertThat(Vector.ofAll(elements(prepared)))
                        .as(label + ": the receiver")
                        .isEqualTo(before);
            }
            Function<C, Object> change = row.change();
            C changeInput = row.changeInput();
            if (change != null && changeInput != null) {
                Vector<Object> before = Vector.ofAll(elements(changeInput));
                Object changed = change.apply(changeInput);
                String label = table.type.getSimpleName() + "." + row.signature() + " changing " + before;
                if (changed instanceof Tuple2<?, ?> parts && row.expected() instanceof Tuple2<?, ?> expected) {
                    softly.assertThat(parts._1()).as(label).isNotSameAs(changeInput);
                    softly.assertThat(parts._2()).as(label).isNotSameAs(changeInput);
                    softly.assertThat(Vector.ofAll(elements(parts._1())))
                            .as(label)
                            .isEqualTo(expected._1());
                    softly.assertThat(Vector.ofAll(elements(parts._2())))
                            .as(label)
                            .isEqualTo(expected._2());
                } else {
                    softly.assertThat(changed).as(label).isNotSameAs(changeInput);
                    if (row.expected() == null) {
                        softly.assertThat(
                                        Vector.ofAll(elements(changed)).sorted(Comparator.comparing(Object::toString)))
                                .as(label)
                                .isEqualTo(before.sorted(Comparator.comparing(Object::toString)));
                    } else {
                        softly.assertThat(Vector.ofAll(elements(changed)))
                                .as(label)
                                .isEqualTo(row.expected());
                    }
                }
                softly.assertThat(Vector.ofAll(elements(changeInput)))
                        .as(label + ": the receiver")
                        .isEqualTo(before);
            }
        }
        softly.assertAll();
    }

    @SuppressWarnings("unchecked")
    private static Iterable<Object> elements(Object sequence) {
        return (Iterable<Object>) sequence;
    }

    /// The public instance methods of `type` that return `type` of the same elements, or a tuple of two of them.
    private static TreeSet<String> sameTypeOperations(Class<?> type) {
        TreeSet<String> signatures = new TreeSet<>();
        for (Method method : type.getMethods()) {
            if (Modifier.isStatic(method.getModifiers()) || method.isBridge() || method.isSynthetic()) {
                continue;
            }
            Type returned = method.getGenericReturnType();
            boolean tuple = returned instanceof ParameterizedType parameterized
                    && parameterized.getRawType() == Tuple2.class
                    && isSameType(parameterized.getActualTypeArguments()[0], type)
                    && isSameType(parameterized.getActualTypeArguments()[1], type);
            if (isSameType(returned, type) || tuple) {
                StringJoiner signature = new StringJoiner(", ", method.getName() + "(", ")");
                for (Class<?> parameter : method.getParameterTypes()) {
                    signature.add(parameter.getSimpleName());
                }
                signatures.add(signature.toString());
            }
        }
        return signatures;
    }

    // `type` of its own element type: the type's own variable, or the one of Traversable it inherits a method from
    private static boolean isSameType(Type returned, Class<?> type) {
        return returned instanceof ParameterizedType parameterized
                && parameterized.getRawType() == type
                && parameterized.getActualTypeArguments()[0] instanceof TypeVariable<?> variable
                && (variable.getGenericDeclaration() == type || variable.getGenericDeclaration() == Traversable.class);
    }
}
