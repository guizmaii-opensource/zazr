package dev.zazr.collection;

import dev.zazr.Tuple;
import dev.zazr.Tuple2;
import dev.zazr.collection.internal.RedBlackTree;
import dev.zazr.collection.internal.RedBlackTreeValidity;
import dev.zazr.control.Option;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.stream.Stream;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

import static org.assertj.core.api.Assertions.assertThat;

/// The operations of the sets and maps that change nothing return the receiver itself: a table of rows, each an
/// operation given arguments that change nothing, run on every type at the sizes where the tries and trees change
/// shape. A second table changes one element with the same operations: the result is a new collection, equal to the
/// one built from the expected elements, in the same order. A reflective test checks that every public operation of
/// the types that returns their own type is in the table or excluded with a reason, so a new operation is checked too.
class ConservingSetMapTest {

    private static final int[] SIZES = {1, 2, 31, 32, 33, 1025};
    private static final int[] CHANGE_SIZES = {2, 32, 33, 1025};

    // above the Integer cache: each call makes a new object, equal to the others of its value
    private static Integer element(int i) {
        return Integer.valueOf(1_000 + i);
    }

    private static final Integer ABSENT = -1;

    private static java.util.List<Integer> elements(int n) {
        java.util.List<Integer> result = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            result.add(element(i));
        }
        return result;
    }

    private static java.util.List<Tuple2<Integer, Integer>> entries(int n) {
        java.util.List<Tuple2<Integer, Integer>> result = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            result.add(Tuple.of(element(i), element(-i)));
        }
        return result;
    }

    private static <T> T first(Iterable<T> collection) {
        return collection.iterator().next();
    }

    private static <T> java.util.List<T> order(Iterable<T> collection) {
        java.util.List<T> result = new ArrayList<>();
        collection.forEach(result::add);
        return result;
    }

    /// A no-op: `op` returns `expected` of the receiver, the receiver itself unless said otherwise.
    record Row<C>(String name, Function<C, ?> op, Function<C, ?> expected) {}

    private static <C> Row<C> row(String name, Function<C, ?> op) {
        return new Row<>(name, op, c -> c);
    }

    // a no-op whose result is not the receiver's type: the collection it wraps comes back
    private static <C> Row<C> row(String name, Function<C, ?> op, Function<C, ?> expected) {
        return new Row<>(name, op, expected);
    }

    /// A change of one element: `op` returns a new collection equal to `expected`, in its order.
    record Change<C>(String name, Function<C, ?> op, Function<Integer, ?> expected) {}

    private static <C> Change<C> change(String name, Function<C, ?> op, Function<Integer, ?> expected) {
        return new Change<>(name, op, expected);
    }

    record Table<C>(String type, Class<?> cls, IntFunction<C> make, java.util.List<Row<C>> rows) {}

    private static java.util.List<Table<?>> tables() {
        return java.util.List.of(
                new Table<>("HashSet", HashSet.class, n -> HashSet.ofAll(elements(n)), hashSetRows()),
                new Table<>(
                        "LinkedHashSet", LinkedHashSet.class, n -> LinkedHashSet.ofAll(elements(n)), linkedSetRows()),
                new Table<>("TreeSet", TreeSet.class, ConservingSetMapTest::treeSet, treeSetRows()),
                new Table<>("HashMap", HashMap.class, n -> HashMap.ofEntries(entries(n)), hashMapRows()),
                new Table<>(
                        "LinkedHashMap",
                        LinkedHashMap.class,
                        n -> LinkedHashMap.ofEntries(entries(n)),
                        linkedMapRows()),
                new Table<>("TreeMap", TreeMap.class, n -> TreeMap.ofEntries(entries(n)), treeMapRows()),
                new Table<>(
                        "NonEmptySet",
                        NonEmptySet.class,
                        n -> NonEmptySet.fromSet(HashSet.ofAll(elements(n))).get(),
                        nonEmptySetRows()),
                new Table<>(
                        "NonEmptySortedSet",
                        NonEmptySortedSet.class,
                        n -> NonEmptySortedSet.fromSortedSet(treeSet(n)).get(),
                        nonEmptySortedSetRows()),
                new Table<>(
                        "NonEmptyMap",
                        NonEmptyMap.class,
                        n -> NonEmptyMap.fromMap(HashMap.ofEntries(entries(n))).get(),
                        nonEmptyMapRows()),
                new Table<>(
                        "NonEmptySortedMap",
                        NonEmptySortedMap.class,
                        n -> NonEmptySortedMap.fromSortedMap(TreeMap.ofEntries(entries(n)))
                                .get(),
                        nonEmptySortedMapRows()));
    }

    private static TreeSet<Integer> treeSet(int n) {
        return TreeSet.ofAll(elements(n));
    }

    @TestFactory
    Stream<DynamicTest> shouldReturnTheReceiverWhenNothingChanges() {
        return tables().stream().flatMap(ConservingSetMapTest::noOpTests);
    }

    private static <C> Stream<DynamicTest> noOpTests(Table<C> table) {
        return table.rows().stream()
                .flatMap(row -> java.util.Arrays.stream(SIZES).mapToObj(n -> {
                    String name = table.type() + "::" + row.name() + " at " + n;
                    return DynamicTest.dynamicTest(name, () -> {
                        C receiver = table.make().apply(n);
                        java.util.List<?> before = order((Iterable<?>) receiver);
                        Object expected = row.expected().apply(receiver);
                        assertThat(row.op().apply(receiver)).as(name).isSameAs(expected);
                        assertThat(order((Iterable<?>) receiver))
                                .as("receiver unchanged")
                                .isEqualTo(before);
                    });
                }));
    }

    // -- sets

    private static <S extends Set<Integer>> java.util.List<Row<S>> setRows() {
        return java.util.List.of(
                row("add of a present element", s -> s.add(first(s))),
                row("addAll of nothing", s -> s.addAll(java.util.List.of())),
                row("addAll of itself", s -> s.addAll(s)),
                row("addAll of present elements", s -> s.addAll(Vector.ofAll(s))),
                row("addAll of a part of itself", s -> s.addAll(s.filter(e -> e % 2 == 0))),
                row("addAll of itself as a set of another kind", s -> s.addAll(LinkedHashSet.ofAll(s))),
                row("union with an empty HashSet", s -> s.union(HashSet.empty())),
                row("union with an empty TreeSet", s -> s.union(TreeSet.<Integer>empty())),
                row("union with itself", s -> s.union(s)),
                row("union with equal elements", s -> s.union(LinkedHashSet.ofAll(elements(s.size())))),
                row("remove of an absent element", s -> s.remove(ABSENT)),
                row("removeAll of nothing", s -> s.removeAll(java.util.List.of())),
                row("removeAll of absent elements", s -> s.removeAll(java.util.List.of(ABSENT, -2))),
                row("removeAll of an empty HashSet", s -> s.removeAll(HashSet.empty())),
                row("diff with an empty set", s -> s.diff(HashSet.empty())),
                row("diff with a disjoint HashSet", s -> s.diff(HashSet.of(ABSENT))),
                row("intersect with itself", s -> s.intersect(s)),
                row(
                        "intersect with a HashSet superset",
                        s -> s.intersect(HashSet.ofAll(s).add(ABSENT))),
                row("retainAll of itself", s -> s.retainAll(s)),
                row("retainAll of a superset", s -> s.retainAll(Vector.ofAll(s).append(ABSENT))),
                row("filter keeping everything", s -> s.filter(e -> true)),
                row("reject dropping nothing", s -> s.reject(e -> false)),
                row(
                        "partition, everything on the left",
                        s -> s.partition(e -> true)._1()),
                row(
                        "partition, everything on the right",
                        s -> s.partition(e -> false)._2()),
                row("replace of an absent element", s -> s.replace(ABSENT, -2)),
                row("replaceAll of an absent element", s -> s.replaceAll(ABSENT, -2)),
                row("replace of an element by itself", s -> s.replace(first(s), first(s))),
                row("replaceAll of an element by itself", s -> s.replaceAll(first(s), first(s))),
                row("tap", s -> s.tap(e -> {})),
                row("orElse(Iterable)", s -> s.orElse(HashSet.of(ABSENT))),
                row("orElse(Supplier) of a supplier", s -> s.orElse(() -> HashSet.of(ABSENT))),
                row("mapConserve to the same elements", s -> s.mapConserve(e -> e)));
    }

    private static <C> java.util.List<Row<C>> concat(java.util.List<Row<C>> rows, java.util.List<Row<C>> more) {
        java.util.List<Row<C>> result = new ArrayList<>(rows);
        result.addAll(more);
        return result;
    }

    private static java.util.List<Row<HashSet<Integer>>> hashSetRows() {
        return concat(
                setRows(),
                java.util.List.of(
                        row("toSet", s -> s.toSet()),
                        row("union with an equal HashSet", s -> s.union(HashSet.ofAll(elements(s.size())))),
                        row("addAll of an equal HashSet", s -> s.addAll(HashSet.ofAll(elements(s.size())))),
                        row("intersect with a larger HashSet", s -> s.intersect(s.add(ABSENT))),
                        row("diff with a disjoint LinkedHashSet", s -> s.diff(LinkedHashSet.of(ABSENT)))));
    }

    private static java.util.List<Row<LinkedHashSet<Integer>>> linkedSetRows() {
        return concat(
                setRows(),
                java.util.List.of(
                        row("toLinkedSet", s -> s.toLinkedSet()),
                        row("take of the size", s -> s.take(s.size())),
                        row("take of more", s -> s.take(s.size() + 1)),
                        row("takeRight of the size", s -> s.takeRight(s.size())),
                        row("drop of 0", s -> s.drop(0)),
                        row("drop of a negative count", s -> s.drop(-1)),
                        row("dropRight of 0", s -> s.dropRight(0)),
                        row("takeWhile of everything", s -> s.takeWhile(e -> true)),
                        row("takeUntil of nothing", s -> s.takeUntil(e -> false)),
                        row("dropWhile of nothing", s -> s.dropWhile(e -> false)),
                        row("dropUntil of the first", s -> s.dropUntil(e -> true)),
                        row(
                                "union with an equal LinkedHashSet",
                                s -> s.union(LinkedHashSet.ofAll(elements(s.size()))))));
    }

    private static java.util.List<Row<TreeSet<Integer>>> treeSetRows() {
        return concat(
                setRows(),
                java.util.List.of(
                        row("toSortedSet", s -> s.toSortedSet()),
                        row("toSortedSet of the same comparator", s -> s.toSortedSet(s.comparator())),
                        row("take of the size", s -> s.take(s.size())),
                        row("take of more", s -> s.take(s.size() + 1)),
                        row("takeRight of the size", s -> s.takeRight(s.size())),
                        row("drop of 0", s -> s.drop(0)),
                        row("drop of a negative count", s -> s.drop(-1)),
                        row("dropRight of 0", s -> s.dropRight(0)),
                        row("takeWhile of everything", s -> s.takeWhile(e -> true)),
                        row("takeUntil of nothing", s -> s.takeUntil(e -> false)),
                        row("dropWhile of nothing", s -> s.dropWhile(e -> false)),
                        row("dropUntil of the first", s -> s.dropUntil(e -> true)),
                        row("rangeFrom of the minimum", s -> s.rangeFrom(s.head())),
                        row("rangeUntil past the maximum", s -> s.rangeUntil(s.last() + 1)),
                        row("rangeTo of the maximum", s -> s.rangeTo(s.last())),
                        row("rangeFromUntil of the whole", s -> s.rangeFromUntil(s.head(), s.last() + 1)),
                        row("union with itself as a TreeSet", s -> s.union(TreeSet.ofAll(s))),
                        row(
                                "union with an empty TreeSet of the same comparator",
                                s -> s.union(TreeSet.empty(s.comparator())))));
    }

    // -- maps

    private static <M extends Map<Integer, Integer>> java.util.List<Row<M>> mapRows() {
        return java.util.List.of(
                row("put(Object,Object) of a present key and value", m -> m.put(first(m)._1(), first(m)._2())),
                row("put(Tuple2) of a present entry", m -> m.put(first(m))),
                row(
                        "put(Object,Object,BiFunction) with a merge keeping the value",
                        m -> m.put(first(m)._1(), first(m)._2(), (a, b) -> a)),
                row(
                        "put(Tuple2,BiFunction) of an entry with a merge keeping the value",
                        m -> m.put(first(m), (a, b) -> a)),
                row("putAll of nothing", m -> m.putAll(java.util.List.of())),
                row("putAll of itself", m -> m.putAll(m)),
                row("putAll of its entries", m -> m.putAll(Vector.ofAll(m))),
                row("putAll of its entries as a java.util.List", m -> m.putAll(order(m))),
                row("merge(Map) with a part of itself", m -> m.merge(m.filterKeys(k -> k % 2 == 0))),
                row("merge(Map) with an empty map", m -> m.merge(HashMap.empty())),
                row("merge(Map) with itself", m -> m.merge(m)),
                row("merge(Map) with equal keys", m -> m.merge(HashMap.ofEntries(entries(m.size())))),
                row(
                        "merge(Map,BiFunction) with a resolution, of an empty map",
                        m -> m.merge(HashMap.<Integer, Integer>empty(), (a, b) -> a)),
                row("remove of an absent key", m -> m.remove(ABSENT)),
                row("removeAll of nothing", m -> m.removeAll(java.util.List.of())),
                row("removeAll of absent keys", m -> m.removeAll(java.util.List.of(ABSENT, -2))),
                row("filter(BiPredicate) keeping everything", m -> m.filter((k, v) -> true)),
                row("filter(Predicate) of entries keeping everything", m -> m.filter(e -> true)),
                row("filterKeys keeping everything", m -> m.filterKeys(k -> true)),
                row("filterValues keeping everything", m -> m.filterValues(v -> true)),
                row("reject(BiPredicate) dropping nothing", m -> m.reject((k, v) -> false)),
                row("reject(Predicate) of entries dropping nothing", m -> m.reject(e -> false)),
                row("rejectKeys dropping nothing", m -> m.rejectKeys(k -> false)),
                row("rejectValues dropping nothing", m -> m.rejectValues(v -> false)),
                row(
                        "partition, everything on the left",
                        m -> m.partition(e -> true)._1()),
                row(
                        "partition, everything on the right",
                        m -> m.partition(e -> false)._2()),
                row("replace(Tuple2,Tuple2) of an absent entry", m -> m.replace(Tuple.of(ABSENT, 1), Tuple.of(-2, 2))),
                row(
                        "replaceAll(Tuple2,Tuple2) of an absent entry",
                        m -> m.replaceAll(Tuple.of(ABSENT, 1), Tuple.of(-2, 2))),
                row("replaceValue of an absent key", m -> m.replaceValue(ABSENT, 1)),
                row("replaceValue by the same value", m -> m.replaceValue(first(m)._1(), first(m)._2())),
                row("replace(Object,Object,Object) of an absent value", m -> m.replace(first(m)._1(), ABSENT, 1)),
                row(
                        "replace(Object,Object,Object) of a value by itself",
                        m -> m.replace(first(m)._1(), first(m)._2(), first(m)._2())),
                row("replaceAll(BiFunction) with the same values", m -> m.replaceAll((k, v) -> v)),
                row("mapValues to the same values", m -> m.mapValues(v -> v)),
                row(
                        "computeIfAbsent of a present key",
                        m -> m.computeIfAbsent(first(m)._1(), k -> 1)._2()),
                row(
                        "computeIfPresent of an absent key",
                        m -> m.computeIfPresent(ABSENT, (k, v) -> v)._2()),
                row(
                        "computeIfPresent to the same value",
                        m -> m.computeIfPresent(first(m)._1(), (k, v) -> v)._2()),
                row("updateWith to the same value", m -> m.updateWith(first(m)._1(), o -> o)),
                row("updateWith of an absent key to nothing", m -> m.updateWith(ABSENT, o -> Option.none())),
                row("tap", m -> m.tap(e -> {})),
                row("orElse(Iterable)", m -> m.orElse(HashMap.of(ABSENT, 1))),
                row("orElse(Supplier) of a supplier", m -> m.orElse(() -> HashMap.of(ABSENT, 1))));
    }

    private static java.util.List<Row<HashMap<Integer, Integer>>> hashMapRows() {
        return concat(
                mapRows(),
                java.util.List.of(
                        row("putAll of an empty HashMap", m -> m.putAll(HashMap.empty())),
                        row("merge(Map) with an empty HashMap", m -> m.merge(HashMap.empty()))));
    }

    private static java.util.List<Row<LinkedHashMap<Integer, Integer>>> linkedMapRows() {
        return concat(
                mapRows(),
                java.util.List.of(
                        row("replace(Tuple2,Tuple2) of a present entry by itself", m -> m.replace(first(m), first(m))),
                        row("take of the size", m -> m.take(m.size())),
                        row("take of more", m -> m.take(m.size() + 1)),
                        row("takeRight of the size", m -> m.takeRight(m.size())),
                        row("drop of 0", m -> m.drop(0)),
                        row("drop of a negative count", m -> m.drop(-1)),
                        row("dropRight of 0", m -> m.dropRight(0)),
                        row("takeWhile of everything", m -> m.takeWhile(e -> true)),
                        row("takeUntil of nothing", m -> m.takeUntil(e -> false)),
                        row("dropWhile of nothing", m -> m.dropWhile(e -> false)),
                        row("dropUntil of the first", m -> m.dropUntil(e -> true)),
                        row("putAll of an equal LinkedHashMap", m -> m.putAll(LinkedHashMap.ofEntries(m))),
                        row("retainAll of its entries", m -> m.retainAll(Vector.ofAll(m)))));
    }

    private static java.util.List<Row<TreeMap<Integer, Integer>>> treeMapRows() {
        return concat(
                mapRows(),
                java.util.List.of(
                        row("replace(Tuple2,Tuple2) of a present entry by itself", m -> m.replace(first(m), first(m))),
                        row("take of the size", m -> m.take(m.size())),
                        row("take of more", m -> m.take(m.size() + 1)),
                        row("takeRight of the size", m -> m.takeRight(m.size())),
                        row("drop of 0", m -> m.drop(0)),
                        row("drop of a negative count", m -> m.drop(-1)),
                        row("dropRight of 0", m -> m.dropRight(0)),
                        row("takeWhile of everything", m -> m.takeWhile(e -> true)),
                        row("takeUntil of nothing", m -> m.takeUntil(e -> false)),
                        row("dropWhile of nothing", m -> m.dropWhile(e -> false)),
                        row("dropUntil of the first", m -> m.dropUntil(e -> true)),
                        row(
                                "rangeFrom of the minimum",
                                m -> m.rangeFrom(m.head()._1())),
                        row(
                                "rangeUntil past the maximum",
                                m -> m.rangeUntil(m.last()._1() + 1)),
                        row("rangeTo of the maximum", m -> m.rangeTo(m.last()._1())),
                        row(
                                "rangeFromUntil of the whole",
                                m -> m.rangeFromUntil(m.head()._1(), m.last()._1() + 1)),
                        row("putAll of an empty TreeMap", m -> m.putAll(TreeMap.<Integer, Integer>empty())),
                        row("removeAll of an empty TreeSet", m -> m.removeAll(TreeSet.<Integer>empty())),
                        row("removeAll of a disjoint TreeSet", m -> m.removeAll(TreeSet.of(ABSENT)))));
    }

    // -- the non-empty wrappers: their own type when it cannot be empty, else the collection they wrap

    private static java.util.List<Row<NonEmptySet<Integer>>> nonEmptySetRows() {
        Function<NonEmptySet<Integer>, ?> wrapped = NonEmptySet::toSet;
        return java.util.List.of(
                row("add of a present element", s -> s.add(first(s))),
                row("addAll of nothing", s -> s.addAll(java.util.List.of())),
                row("addAll of itself", s -> s.addAll(s)),
                row("union with an empty set", s -> s.union(HashSet.empty())),
                row("replace of an absent element", s -> s.replace(ABSENT, -2)),
                row("replace of an element by itself", s -> s.replace(first(s), first(s))),
                row("replaceAll of an absent element", s -> s.replaceAll(ABSENT, -2)),
                row("tap", s -> s.tap(e -> {})),
                row("mapConserve to the same elements", s -> s.mapConserve(e -> e)),
                row("filter keeping everything", s -> s.filter(e -> true), wrapped),
                row("reject dropping nothing", s -> s.reject(e -> false), wrapped),
                row("remove of an absent element", s -> s.remove(ABSENT), wrapped),
                row("removeAll of nothing", s -> s.removeAll(java.util.List.of()), wrapped),
                row("retainAll of itself", s -> s.retainAll(s), wrapped),
                row("intersect with itself", s -> s.intersect(s.toSet()), wrapped),
                row("diff with an empty set", s -> s.diff(HashSet.empty()), wrapped),
                row(
                        "partition, everything on the left",
                        s -> s.partition(e -> true)._1(),
                        wrapped));
    }

    private static java.util.List<Row<NonEmptySortedSet<Integer>>> nonEmptySortedSetRows() {
        Function<NonEmptySortedSet<Integer>, ?> wrapped = NonEmptySortedSet::toSortedSet;
        return java.util.List.of(
                row("add of a present element", s -> s.add(s.head())),
                row("addAll of nothing", s -> s.addAll(java.util.List.of())),
                row("union with an empty set", s -> s.union(HashSet.empty())),
                row("replace of an absent element", s -> s.replace(ABSENT, -2)),
                row("replace of an element by itself", s -> s.replace(s.head(), s.head())),
                row("replaceAll of an absent element", s -> s.replaceAll(ABSENT, -2)),
                row("tap", s -> s.tap(e -> {})),
                row("mapConserve to the same elements", s -> s.mapConserve(e -> e)),
                row("filter keeping everything", s -> s.filter(e -> true), wrapped),
                row("remove of an absent element", s -> s.remove(ABSENT), wrapped),
                row("take of the size", s -> s.take(s.size()), wrapped),
                row("drop of 0", s -> s.drop(0), wrapped),
                row("rangeFrom of the minimum", s -> s.rangeFrom(s.head()), wrapped),
                row(
                        "partition, everything on the right",
                        s -> s.partition(e -> false)._2(),
                        wrapped));
    }

    private static java.util.List<Row<NonEmptyMap<Integer, Integer>>> nonEmptyMapRows() {
        Function<NonEmptyMap<Integer, Integer>, ?> wrapped = NonEmptyMap::toMap;
        return java.util.List.of(
                row("put(Object,Object) of a present key and value", m -> m.put(first(m)._1(), first(m)._2())),
                row("put(Tuple2) of a present entry", m -> m.put(first(m))),
                row(
                        "put(Object,Object,BiFunction) with a merge keeping the value",
                        m -> m.put(first(m)._1(), first(m)._2(), (a, b) -> a)),
                row(
                        "put(Tuple2,BiFunction) of an entry with a merge keeping the value",
                        m -> m.put(first(m), (a, b) -> a)),
                row("putAll of nothing", m -> m.putAll(java.util.List.of())),
                row("putAll of itself", m -> m.putAll(m)),
                row("merge(Map) with an empty map", m -> m.merge(HashMap.empty())),
                row(
                        "merge(Map,BiFunction) with a resolution, of an empty map",
                        m -> m.merge(HashMap.<Integer, Integer>empty(), (a, b) -> a)),
                row("replace(Tuple2,Tuple2) of an absent entry", m -> m.replace(Tuple.of(ABSENT, 1), Tuple.of(-2, 2))),
                row(
                        "replaceAll(Tuple2,Tuple2) of an absent entry",
                        m -> m.replaceAll(Tuple.of(ABSENT, 1), Tuple.of(-2, 2))),
                row(
                        "replace(Object,Object,Object) of a value by itself",
                        m -> m.replace(first(m)._1(), first(m)._2(), first(m)._2())),
                row("replaceValue of an absent key", m -> m.replaceValue(ABSENT, 1)),
                row("replaceAll(BiFunction) with the same values", m -> m.replaceAll((k, v) -> v)),
                row("mapValues to the same values", m -> m.mapValues(v -> v)),
                row(
                        "computeIfAbsent of a present key",
                        m -> m.computeIfAbsent(first(m)._1(), k -> 1)._2()),
                row(
                        "computeIfPresent to the same value",
                        m -> m.computeIfPresent(first(m)._1(), (k, v) -> v)._2()),
                row("tap", m -> m.tap(e -> {})),
                row("filter(BiPredicate) keeping everything", m -> m.filter((k, v) -> true), wrapped),
                row("remove of an absent key", m -> m.remove(ABSENT), wrapped),
                row(
                        "partition, everything on the left",
                        m -> m.partition(e -> true)._1(),
                        wrapped));
    }

    private static java.util.List<Row<NonEmptySortedMap<Integer, Integer>>> nonEmptySortedMapRows() {
        Function<NonEmptySortedMap<Integer, Integer>, ?> wrapped = NonEmptySortedMap::toSortedMap;
        return java.util.List.of(
                row(
                        "put(Object,Object) of a present key and value",
                        m -> m.put(m.head()._1(), m.head()._2())),
                row("put(Tuple2) of a present entry", m -> m.put(m.head())),
                row(
                        "put(Object,Object,BiFunction) with a merge keeping the value",
                        m -> m.put(m.head()._1(), m.head()._2(), (a, b) -> a)),
                row(
                        "put(Tuple2,BiFunction) of an entry with a merge keeping the value",
                        m -> m.put(m.head(), (a, b) -> a)),
                row("putAll of nothing", m -> m.putAll(java.util.List.of())),
                row("putAll of itself", m -> m.putAll(m)),
                row("merge(Map) with an empty map", m -> m.merge(HashMap.empty())),
                row(
                        "merge(Map,BiFunction) with a resolution, of an empty map",
                        m -> m.merge(HashMap.<Integer, Integer>empty(), (a, b) -> a)),
                row("replace(Tuple2,Tuple2) of an absent entry", m -> m.replace(Tuple.of(ABSENT, 1), Tuple.of(-2, 2))),
                row(
                        "replaceAll(Tuple2,Tuple2) of an absent entry",
                        m -> m.replaceAll(Tuple.of(ABSENT, 1), Tuple.of(-2, 2))),
                row(
                        "replace(Object,Object,Object) of a value by itself",
                        m -> m.replace(m.head()._1(), m.head()._2(), m.head()._2())),
                row("replaceValue of an absent key", m -> m.replaceValue(ABSENT, 1)),
                row("replaceAll(BiFunction) with the same values", m -> m.replaceAll((k, v) -> v)),
                row("mapValues to the same values", m -> m.mapValues(v -> v)),
                row(
                        "computeIfAbsent of a present key",
                        m -> m.computeIfAbsent(m.head()._1(), k -> 1)._2()),
                row(
                        "computeIfPresent to the same value",
                        m -> m.computeIfPresent(m.head()._1(), (k, v) -> v)._2()),
                row("replace(Tuple2,Tuple2) of a present entry by itself", m -> m.replace(m.head(), m.head())),
                row("tap", m -> m.tap(e -> {})),
                row("filter(BiPredicate) keeping everything", m -> m.filter((k, v) -> true), wrapped),
                row("remove of an absent key", m -> m.remove(ABSENT), wrapped),
                row("take of the size", m -> m.take(m.size()), wrapped),
                row(
                        "partition, everything on the left",
                        m -> m.partition(e -> true)._1(),
                        wrapped));
    }

    // -- an empty receiver returns the other side

    @Test
    void shouldReturnTheOtherSideOfAnEmptyReceiver() {
        HashSet<Integer> hashSet = HashSet.ofAll(elements(33));
        assertThat(HashSet.<Integer>empty().union(hashSet)).isSameAs(hashSet);
        assertThat(HashSet.<Integer>empty().addAll(hashSet)).isSameAs(hashSet);
        LinkedHashSet<Integer> linkedSet = LinkedHashSet.ofAll(elements(33));
        assertThat(LinkedHashSet.<Integer>empty().union(linkedSet)).isSameAs(linkedSet);
        assertThat(LinkedHashSet.<Integer>empty().addAll(linkedSet)).isSameAs(linkedSet);
        TreeSet<Integer> treeSet = treeSet(33);
        assertThat(TreeSet.<Integer>empty().union(treeSet)).isSameAs(treeSet);
        assertThat(TreeSet.<Integer>empty().addAll(treeSet)).isSameAs(treeSet);
        HashMap<Integer, Integer> hashMap = HashMap.ofEntries(entries(33));
        assertThat(HashMap.<Integer, Integer>empty().putAll(hashMap)).isSameAs(hashMap);
        assertThat(HashMap.<Integer, Integer>empty().merge(hashMap)).isSameAs(hashMap);
        LinkedHashMap<Integer, Integer> linkedMap = LinkedHashMap.ofEntries(entries(33));
        assertThat(LinkedHashMap.<Integer, Integer>empty().putAll(linkedMap)).isSameAs(linkedMap);
        assertThat(LinkedHashMap.<Integer, Integer>empty().merge(linkedMap)).isSameAs(linkedMap);
        TreeMap<Integer, Integer> treeMap = TreeMap.ofEntries(entries(33));
        assertThat(TreeMap.<Integer, Integer>empty().putAll(treeMap)).isSameAs(treeMap);
        assertThat(TreeMap.<Integer, Integer>empty().merge(treeMap)).isSameAs(treeMap);
        // a TreeSet of another comparator is not the other side: its elements are sorted again
        TreeSet<Integer> reversed = TreeSet.ofAll(Comparator.reverseOrder(), elements(33));
        assertThat(TreeSet.<Integer>empty().union(reversed))
                .isNotSameAs(reversed)
                .containsExactlyElementsOf(treeSet);
    }

    // -- a change of one element is a new collection

    @TestFactory
    Stream<DynamicTest> shouldReturnANewCollectionWhenOneElementChanges() {
        return Stream.of(
                        changeTests("HashSet", n -> HashSet.ofAll(elements(n)), setChanges(HashSet::ofAll)),
                        changeTests(
                                "LinkedHashSet",
                                n -> LinkedHashSet.ofAll(elements(n)),
                                setChanges(LinkedHashSet::ofAll)),
                        changeTests("TreeSet", ConservingSetMapTest::treeSet, setChanges(TreeSet::ofAll)),
                        changeTests("HashMap", n -> HashMap.ofEntries(entries(n)), mapChanges(HashMap::ofEntries)),
                        changeTests(
                                "LinkedHashMap",
                                n -> LinkedHashMap.ofEntries(entries(n)),
                                mapChanges(LinkedHashMap::ofEntries)),
                        changeTests("TreeMap", n -> TreeMap.ofEntries(entries(n)), mapChanges(TreeMap::ofEntries)))
                .flatMap(Function.identity());
    }

    private static <C> Stream<DynamicTest> changeTests(
            String type, IntFunction<C> make, java.util.List<Change<C>> rows) {
        return rows.stream()
                .flatMap(row -> java.util.Arrays.stream(CHANGE_SIZES).mapToObj(n -> {
                    String name = type + "::" + row.name() + " at " + n;
                    return DynamicTest.dynamicTest(name, () -> {
                        C receiver = make.apply(n);
                        java.util.List<?> before = order((Iterable<?>) receiver);
                        Object result = row.op().apply(receiver);
                        Object expected = row.expected().apply(n);
                        assertThat(result).as(name).isNotSameAs(receiver).isEqualTo(expected);
                        assertThat(order((Iterable<?>) result))
                                .as(name + ", order")
                                .isEqualTo(order((Iterable<?>) expected));
                        assertThat(order((Iterable<?>) receiver))
                                .as("receiver unchanged")
                                .isEqualTo(before);
                    });
                }));
    }

    // the elements 0 until n, with `drop` left out and `add` added
    private static java.util.List<Integer> without(int n, int drop, int add) {
        java.util.List<Integer> result = new ArrayList<>(elements(n));
        result.remove(element(drop));
        if (add != Integer.MIN_VALUE) {
            result.add(add);
        }
        return result;
    }

    // the elements 0 until n, with ABSENT in the place of the first
    private static java.util.List<Integer> replaced(int n) {
        java.util.List<Integer> result = new ArrayList<>(elements(n));
        result.set(0, ABSENT);
        return result;
    }

    private static <S extends Set<Integer>> java.util.List<Change<S>> setChanges(
            Function<java.util.List<Integer>, S> ofAll) {
        int none = Integer.MIN_VALUE;
        return java.util.List.of(
                change("add of an absent element", s -> s.add(ABSENT), n -> ofAll.apply(without(n, -5, ABSENT))),
                change(
                        "addAll of one absent element",
                        s -> s.addAll(java.util.List.of(ABSENT)),
                        n -> ofAll.apply(without(n, -5, ABSENT))),
                change(
                        "union with one absent element",
                        s -> s.union(HashSet.of(ABSENT)),
                        n -> ofAll.apply(without(n, -5, ABSENT))),
                change("remove of the first", s -> s.remove(element(0)), n -> ofAll.apply(without(n, 0, none))),
                change(
                        "removeAll of the first",
                        s -> s.removeAll(HashSet.of(element(0))),
                        n -> ofAll.apply(without(n, 0, none))),
                change(
                        "diff with the first",
                        s -> s.diff(HashSet.of(element(0))),
                        n -> ofAll.apply(without(n, 0, none))),
                change(
                        "filter dropping the first",
                        s -> s.filter(e -> !e.equals(element(0))),
                        n -> ofAll.apply(without(n, 0, none))),
                change(
                        "reject of the first",
                        s -> s.reject(e -> e.equals(element(0))),
                        n -> ofAll.apply(without(n, 0, none))),
                change(
                        "retainAll of all but the first",
                        s -> s.retainAll(without(s.size(), 0, none)),
                        n -> ofAll.apply(without(n, 0, none))),
                change(
                        "partition, all but the first on the left",
                        s -> s.partition(e -> !e.equals(element(0)))._1(),
                        n -> ofAll.apply(without(n, 0, none))),
                change(
                        "partition, the first on the left",
                        s -> s.partition(e -> e.equals(element(0)))._1(),
                        n -> ofAll.apply(java.util.List.of(element(0)))),
                change(
                        "filter dropping the last",
                        s -> s.filter(e -> !e.equals(element(s.size() - 1))),
                        n -> ofAll.apply(without(n, n - 1, none))),
                change(
                        "partition, the last on the right",
                        s -> s.partition(e -> !e.equals(element(s.size() - 1)))._1(),
                        n -> ofAll.apply(without(n, n - 1, none))),
                change("replace of the first", s -> s.replace(element(0), ABSENT), n -> ofAll.apply(replaced(n))),
                change(
                        "mapConserve of the first",
                        s -> s.mapConserve(e -> e.equals(element(0)) ? ABSENT : e),
                        n -> ofAll.apply(replaced(n))),
                // an equal element that is another object is a change: the set holds the new one
                change(
                        "mapConserve of the first to an equal object",
                        s -> s.mapConserve(e -> e.equals(element(0)) ? element(0) : e),
                        n -> ofAll.apply(elements(n))));
    }

    private static java.util.List<Tuple2<Integer, Integer>> entriesWith(int n, int key, Integer value) {
        java.util.List<Tuple2<Integer, Integer>> result = new ArrayList<>(entries(n));
        result.replaceAll(e -> e._1().equals(element(key)) ? Tuple.of(e._1(), value) : e);
        return result;
    }

    private static java.util.List<Tuple2<Integer, Integer>> entriesWithout(int n, int key) {
        java.util.List<Tuple2<Integer, Integer>> result = new ArrayList<>(entries(n));
        result.removeIf(e -> e._1().equals(element(key)));
        return result;
    }

    private static <M extends Map<Integer, Integer>> java.util.List<Change<M>> mapChanges(
            Function<java.util.List<Tuple2<Integer, Integer>>, M> ofEntries) {
        Integer value = 7;
        return java.util.List.of(
                change(
                        "put of a new value",
                        m -> m.put(element(0), value),
                        n -> ofEntries.apply(entriesWith(n, 0, value))),
                change(
                        "put of an equal value, another object",
                        m -> m.put(element(0), element(0)),
                        n -> ofEntries.apply(entriesWith(n, 0, element(0)))),
                change(
                        "put of an equal key, another object",
                        m -> m.put(element(0), m.get(element(0)).get()),
                        n -> ofEntries.apply(entries(n))),
                change(
                        "putAll of one new value",
                        m -> m.putAll(java.util.List.of(Tuple.of(element(0), value))),
                        n -> ofEntries.apply(entriesWith(n, 0, value))),
                change("remove of the first", m -> m.remove(element(0)), n -> ofEntries.apply(entriesWithout(n, 0))),
                change(
                        "filter dropping the first",
                        m -> m.filterKeys(k -> !k.equals(element(0))),
                        n -> ofEntries.apply(entriesWithout(n, 0))),
                change(
                        "reject of the first",
                        m -> m.rejectKeys(k -> k.equals(element(0))),
                        n -> ofEntries.apply(entriesWithout(n, 0))),
                change(
                        "partition, all but the first on the left",
                        m -> m.partition(e -> !e._1().equals(element(0)))._1(),
                        n -> ofEntries.apply(entriesWithout(n, 0))),
                change(
                        "mapValues of one value",
                        m -> m.mapValues(v -> v.equals(element(0)) ? value : v),
                        n -> ofEntries.apply(entriesWith(n, 0, value))),
                change(
                        "replaceAll of one value",
                        m -> m.replaceAll((k, v) -> k.equals(element(0)) ? value : v),
                        n -> ofEntries.apply(entriesWith(n, 0, value))),
                change(
                        "computeIfPresent of one value",
                        m -> m.computeIfPresent(element(0), (k, v) -> value)._2(),
                        n -> ofEntries.apply(entriesWith(n, 0, value))),
                change(
                        "filter dropping the last",
                        m -> m.filterKeys(k -> !k.equals(element(m.size() - 1))),
                        n -> ofEntries.apply(entriesWithout(n, n - 1))),
                change(
                        "filter dropping every other entry",
                        m -> m.filterKeys(k -> k % 2 == 0),
                        n -> ofEntries.apply(
                                entries(n).stream().filter(e -> e._1() % 2 == 0).toList())),
                change(
                        "filter dropping the last eleven",
                        m -> m.filterKeys(k -> k < element(m.size() - 11)),
                        n -> ofEntries.apply(entries(n).stream()
                                .filter(e -> e._1() < element(n - 11))
                                .toList())),
                change(
                        "filter of entries dropping the last",
                        m -> m.filter(e -> !e._1().equals(element(m.size() - 1))),
                        n -> ofEntries.apply(entriesWithout(n, n - 1))),
                change(
                        "mapValues of the last value",
                        m -> m.mapValues(v -> v.equals(element(-(m.size() - 1))) ? value : v),
                        n -> ofEntries.apply(entriesWith(n, n - 1, value))),
                change(
                        "replaceAll of the last value",
                        m -> m.replaceAll((k, v) -> k.equals(element(m.size() - 1)) ? value : v),
                        n -> ofEntries.apply(entriesWith(n, n - 1, value))));
    }

    // -- the trees keep their shape: puts and value changes against a model

    @SuppressWarnings("unchecked")
    private static <K, V> RedBlackTree<Tuple2<K, V>> tree(TreeMap<K, V> map) {
        try {
            java.lang.reflect.Field field = TreeMap.class.getDeclaredField("entries");
            field.setAccessible(true);
            return (RedBlackTree<Tuple2<K, V>>) field.get(map);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
    }

    // the entries of `result` that are the very entry objects of `map`
    private static int sharedEntries(TreeMap<Integer, Integer> map, TreeMap<Integer, Integer> result) {
        java.util.Set<Object> entries = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());
        entries.addAll(order(map));
        return (int) order(result).stream().filter(entries::contains).count();
    }

    @Test
    void shouldKeepTheTreeMapValidThroughPutsAndValueChanges() {
        java.util.Random random = new java.util.Random(20261103L);
        for (int round = 0; round < 200; round++) {
            TreeMap<Integer, Integer> map = randomPuts(random, random.nextInt(70));
            if (map.isEmpty()) {
                continue;
            }
            Integer changed = order(map.keySet()).get(random.nextInt(map.size()));
            Integer value = element(-1);
            TreeMap<Integer, Integer> mapped = map.replaceAll((k, v) -> k.equals(changed) ? value : v);
            RedBlackTreeValidity.assertValid(tree(mapped));
            assertThat(mapped.get(changed).get()).isSameAs(value);
            // the subtrees off the path to the changed entry are shared, so are their entries
            assertThat(sharedEntries(map, mapped)).isEqualTo(map.size() - 1);
            assertThat(map.replaceAll((k, v) -> v)).isSameAs(map);
        }
    }

    // n random puts, each checked against a java.util.TreeMap and the red-black invariants; the last map
    private static TreeMap<Integer, Integer> randomPuts(java.util.Random random, int n) {
        java.util.TreeMap<Integer, Integer> model = new java.util.TreeMap<>();
        java.util.List<TreeMap<Integer, Integer>> maps = new ArrayList<>(java.util.List.of(TreeMap.empty()));
        for (int i = 0; i < n; i++) {
            TreeMap<Integer, Integer> map = maps.getLast();
            Integer k = element(random.nextInt(100));
            Integer v = element(random.nextInt(4));
            TreeMap<Integer, Integer> next = map.put(k, v);
            Tuple2<Integer, Integer> stored = order(map).stream()
                    .filter(e -> e._1().equals(k))
                    .findFirst()
                    .orElse(null);
            // the same key and value objects give the same map; anything else a new one
            boolean same = stored != null && stored._1() == k && stored._2() == v;
            assertThat(next == map).isEqualTo(same);
            model.put(k, v);
            RedBlackTreeValidity.assertValid(tree(next));
            assertThat(order(next))
                    .isEqualTo(order(model.entrySet().stream()
                            .map(e -> Tuple.of(e.getKey(), e.getValue()))
                            .toList()));
            maps.add(next);
        }
        return maps.getLast();
    }

    // -- the LinkedHashMap filters call the predicate once per entry, in iteration order

    @Test
    void shouldCallTheLinkedHashMapPredicateOncePerEntryInOrder() {
        // removed and put-back keys leave gaps in the insertion order and move 3 and 0 to the end
        LinkedHashMap<Integer, Integer> map = LinkedHashMap.ofEntries(entries(12))
                .removeAll(java.util.List.of(element(0), element(3), element(7)))
                .put(element(3), 3)
                .put(element(0), 0);
        java.util.List<Integer> keys = order(map.keySet());
        java.util.List<Integer> rejectedKeys = new ArrayList<>(java.util.List.of(ABSENT));
        rejectedKeys.addAll(java.util.List.of(keys.getFirst(), keys.get(4), keys.getLast(), element(3)));
        for (Integer rejected : rejectedKeys) {
            java.util.List<Integer> calls = new ArrayList<>();
            LinkedHashMap<Integer, Integer> filtered = map.filter((k, v) -> {
                calls.add(k);
                return !k.equals(rejected);
            });
            assertThat(calls).as("filter without " + rejected).isEqualTo(keys);
            java.util.List<Integer> kept = new ArrayList<>(keys);
            kept.remove(rejected);
            assertThat(order(filtered.keySet())).isEqualTo(kept);
            assertThat(filtered == map).isEqualTo(rejected.equals(ABSENT));
            java.util.List<Integer> keyCalls = new ArrayList<>();
            map.rejectKeys(k -> {
                keyCalls.add(k);
                return k.equals(rejected);
            });
            assertThat(keyCalls).as("rejectKeys of " + rejected).isEqualTo(keys);
        }
    }

    // -- an equal object that is not the one there still replaces it

    @Test
    void shouldStoreAnEqualObjectThatIsNotTheOneThere() {
        Integer key = element(0);
        Integer equalKey = element(0);
        Integer equalValue = element(0);
        java.util.List<Map<Integer, Integer>> maps =
                java.util.List.of(HashMap.of(key, 1), LinkedHashMap.of(key, 1), TreeMap.of(key, 1));
        for (Map<Integer, Integer> map : maps) {
            Integer value = map.get(key).get();
            Map<Integer, Integer> sameKeyNewValue = map.put(key, equalValue);
            assertThat(sameKeyNewValue).isNotSameAs(map);
            assertThat(sameKeyNewValue.get(key).get()).isSameAs(equalValue);
            Map<Integer, Integer> newKeySameValue = map.put(equalKey, value);
            assertThat(newKeySameValue).isNotSameAs(map);
            assertThat(first(newKeySameValue)._1()).isSameAs(equalKey);
            assertThat(map.put(key, value)).isSameAs(map);
        }
        // a LinkedHashSet keeps the element there when given an equal one
        java.util.List<Set<Integer>> sets = java.util.List.of(HashSet.of(key), TreeSet.of(key));
        for (Set<Integer> set : sets) {
            Set<Integer> replaced = set.replace(key, equalKey);
            assertThat(replaced).isNotSameAs(set);
            assertThat(first(replaced)).isSameAs(equalKey);
            assertThat(set.replace(equalKey, key)).isSameAs(set);
        }
    }

    // -- every operation that returns the type is in the table, or excluded with a reason

    private static final java.util.Map<String, String> EXCLUDED = java.util.Map.ofEntries(
            java.util.Map.entry("map", "maps to new elements"),
            java.util.Map.entry("flatMap", "maps to new elements"),
            java.util.Map.entry("flatMapAll", "maps to new elements"),
            java.util.Map.entry("collect", "maps to new elements"),
            java.util.Map.entry("as", "maps to new elements"),
            java.util.Map.entry("mapKeys", "maps to new keys"),
            java.util.Map.entry("mapBoth", "maps to new entries"),
            java.util.Map.entry("partitionMap", "maps to new elements"),
            java.util.Map.entry("toMap", "maps to new entries"),
            java.util.Map.entry("toSortedMap", "maps to new entries"),
            java.util.Map.entry("groupBy", "groups the entries"),
            java.util.Map.entry("groupMap", "groups the entries"),
            java.util.Map.entry("groupMapReduce", "groups the entries"),
            java.util.Map.entry("init", "removes an element"),
            java.util.Map.entry("tail", "removes an element"),
            // the result holds the given entry objects, which are equal to those of the map, not the same
            java.util.Map.entry("HashMap::retainAll", "keeps the given entries"),
            java.util.Map.entry("TreeMap::retainAll", "keeps the given entries"),
            java.util.Map.entry("NonEmptyMap::retainAll", "keeps the given entries"),
            java.util.Map.entry("NonEmptySortedMap::retainAll", "keeps the given entries"));

    // An overloaded method is named with its parameter types, as `filter(BiPredicate)`, and so is the row for it: the
    // first word of a row's name. A method with one form is named alone.
    private static String signature(Method method, boolean overloaded) {
        if (!overloaded) {
            return method.getName();
        }
        java.util.StringJoiner parameters = new java.util.StringJoiner(",", method.getName() + "(", ")");
        for (Class<?> type : method.getParameterTypes()) {
            parameters.add(type.getSimpleName());
        }
        return parameters.toString();
    }

    @Test
    void shouldCoverEveryOperationReturningTheType() {
        java.util.List<String> missing = new ArrayList<>();
        for (Table<?> table : tables()) {
            java.util.Set<String> covered = new java.util.TreeSet<>();
            for (Row<?> row : table.rows()) {
                covered.add(row.name().split(" ")[0].replaceAll(",$", ""));
            }
            java.util.List<Method> methods = new ArrayList<>();
            for (Method method : table.cls().getDeclaredMethods()) {
                int modifiers = method.getModifiers();
                boolean returnsItself = method.getReturnType() == table.cls()
                        || (method.getReturnType() == Tuple2.class
                                && method.getGenericReturnType()
                                        .getTypeName()
                                        .contains(table.cls().getName() + "<"));
                if (Modifier.isPublic(modifiers)
                        && !Modifier.isStatic(modifiers)
                        && !method.isBridge()
                        && !method.isSynthetic()
                        && returnsItself
                        && !EXCLUDED.containsKey(method.getName())
                        && !EXCLUDED.containsKey(table.type() + "::" + method.getName())) {
                    methods.add(method);
                }
            }
            for (Method method : methods) {
                boolean overloaded = methods.stream()
                                .filter(m -> m.getName().equals(method.getName()))
                                .count()
                        > 1;
                String signature = signature(method, overloaded);
                if (!covered.contains(signature)) {
                    missing.add(table.type() + "::" + signature);
                }
            }
        }
        assertThat(missing).isEmpty();
    }
}
