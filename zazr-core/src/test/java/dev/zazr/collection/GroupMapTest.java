package dev.zazr.collection;

import dev.zazr.Tuple;
import dev.zazr.Tuple2;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.function.Function;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

/// `groupMap` and `groupMapReduce` on every collection: the groups and their order, the key order, the types of the
/// groups, the sizes the tries change shape at, the number of calls of each function, and the arguments that are null.
/// A function that returns null is in `NullResultTest`.
public class GroupMapTest {

    private static final int[] SIZES = {0, 1, 2, 31, 32, 33, 1023, 1024, 1025};

    private static final Function<Integer, Integer> KEY = i -> i % 7;
    private static final Function<Integer, String> VALUE = i -> "v" + i;
    private static final BiFunction<String, String, String> REDUCE = (a, b) -> a + "," + b;

    private static final Function<Tuple2<Integer, Integer>, Integer> ENTRY_KEY = e -> e._1() % 7;
    private static final Function<Tuple2<Integer, Integer>, String> ENTRY_VALUE = e -> "v" + e._1() + "=" + e._2();

    // -- the sequences

    @Test
    public void shouldGroupAndMapTheElementsOfASequenceInOrder() {
        var words = Vector.of("apple", "bob", "avocado", "cat", "banana", "cherry");
        var expected = LinkedHashMap.of('a', Vector.of(5, 7), 'b', Vector.of(3, 6), 'c', Vector.of(3, 6));
        assertThat(words.groupMap(w -> w.charAt(0), String::length)).isEqualTo(expected);
        assertThat(List.ofAll(words).groupMap(w -> w.charAt(0), String::length))
                .isEqualTo(expected.mapValues(List::ofAll));
        assertThat(Queue.ofAll(words).groupMap(w -> w.charAt(0), String::length))
                .isEqualTo(expected.mapValues(Queue::ofAll));
        assertThat(LazyList.ofAll(words).groupMap(w -> w.charAt(0), String::length))
                .isEqualTo(expected.mapValues(LazyList::ofAll));
        assertThat(words.groupMap(w -> w.charAt(0), String::length).keySet().toVector())
                .containsExactly('a', 'b', 'c');
    }

    @Test
    public void shouldGroupAndReduceTheElementsOfASequenceFromTheLeft() {
        var words = Vector.of("b", "a", "b", "c", "a", "b");
        var counts = LinkedHashMap.of("b", 3, "a", 2, "c", 1);
        assertThat(words.groupMapReduce(w -> w, w -> 1, Integer::sum)).isEqualTo(counts);
        assertThat(List.ofAll(words).groupMapReduce(w -> w, w -> 1, Integer::sum))
                .isEqualTo(counts);
        assertThat(Queue.ofAll(words).groupMapReduce(w -> w, w -> 1, Integer::sum))
                .isEqualTo(counts);
        assertThat(LazyList.ofAll(words).groupMapReduce(w -> w, w -> 1, Integer::sum))
                .isEqualTo(counts);
        assertThat(words.groupMapReduce(w -> w, w -> 1, Integer::sum).keySet().toVector())
                .containsExactly("b", "a", "c");

        var indexed = Vector.of(1, 2, 3, 4, 5, 6, 7, 8);
        assertThat(indexed.groupMapReduce(i -> i % 3, String::valueOf, (a, b) -> a + b))
                .isEqualTo(LinkedHashMap.of(1, "147", 2, "258", 0, "36"));
    }

    @Test
    public void shouldGroupASequenceOfEverySizeAsGroupByThenMapDoes() {
        for (int size : SIZES) {
            Vector<Integer> vector = Vector.range(0, size);
            checkSequence(vector, vector.groupMap(KEY, VALUE), vector.groupMapReduce(KEY, VALUE, REDUCE));
            List<Integer> list = List.range(0, size);
            checkSequence(list, list.groupMap(KEY, VALUE), list.groupMapReduce(KEY, VALUE, REDUCE));
            Queue<Integer> queue = Vector.range(size / 2, size).foldLeft(Queue.range(0, size / 2), Queue::enqueue);
            checkSequence(queue, queue.groupMap(KEY, VALUE), queue.groupMapReduce(KEY, VALUE, REDUCE));
            LazyList<Integer> lazy = LazyList.range(0, size);
            checkSequence(lazy, lazy.groupMap(KEY, VALUE), lazy.groupMapReduce(KEY, VALUE, REDUCE));
        }
    }

    /// `groupMap` against `groupBy` then `map`, and `groupMapReduce` against `reduceLeft` of each such group, with
    /// the same key order.
    private static void checkSequence(
            Traversable<Integer> source,
            Map<Integer, ? extends Traversable<String>> grouped,
            Map<Integer, String> reduced) {
        LinkedHashMap<Integer, Vector<String>> expected = source.foldLeft(
                LinkedHashMap.empty(),
                (groups, element) ->
                        groups.put(KEY.apply(element), Vector.of(VALUE.apply(element)), Vector::appendAll));
        assertThat(grouped.mapValues(Traversable::toVector)).isEqualTo(expected);
        assertThat(grouped.keySet().toVector()).isEqualTo(expected.keySet().toVector());
        for (Tuple2<Integer, ? extends Traversable<String>> group : grouped) {
            assertThat(group._2().toVector()).isEqualTo(expected.get(group._1()).get());
        }
        assertThat(reduced).isEqualTo(expected.mapValues(group -> group.reduceLeft(REDUCE)));
        assertThat(reduced.keySet().toVector()).isEqualTo(expected.keySet().toVector());
    }

    @Test
    public void shouldGroupTheSequenceTypeItIsCalledOn() {
        assertThat(Vector.of(1).groupMap(KEY, VALUE).get(1).get()).isInstanceOf(Vector.class);
        assertThat(List.of(1).groupMap(KEY, VALUE).get(1).get()).isInstanceOf(List.class);
        assertThat(Queue.of(1).groupMap(KEY, VALUE).get(1).get()).isInstanceOf(Queue.class);
        assertThat(LazyList.of(1).groupMap(KEY, VALUE).get(1).get()).isInstanceOf(LazyList.class);
        assertThat(Vector.of(1).groupMap(KEY, VALUE)).isInstanceOf(LinkedHashMap.class);
        assertThat(Vector.of(1).groupMapReduce(KEY, VALUE, REDUCE)).isInstanceOf(LinkedHashMap.class);
    }

    @Test
    public void shouldGroupALazyListReadFromAOneShotIterator() {
        java.util.Iterator<Integer> once = Vector.range(0, 20).iterator();
        LazyList<Integer> lazy = LazyList.ofAll(() -> once);
        Map<Integer, LazyList<String>> first = lazy.groupMap(KEY, VALUE);
        assertThat(lazy.groupMap(KEY, VALUE)).isEqualTo(first);
        assertThat(first.get(3).get()).containsExactly("v3", "v10", "v17");
        assertThat(lazy.groupMapReduce(KEY, VALUE, REDUCE).get(3).get()).isEqualTo("v3,v10,v17");
    }

    // -- the sets

    @Test
    public void shouldGroupAndMapTheElementsOfASet() {
        var numbers = LinkedHashSet.of(10, 21, 12, 23, 14);
        var grouped = numbers.groupMap(i -> i % 2, i -> i / 10);
        assertThat(grouped).isEqualTo(LinkedHashMap.of(0, LinkedHashSet.of(1), 1, LinkedHashSet.of(2)));
        assertThat(grouped.keySet().toVector()).containsExactly(0, 1);
        assertThat(LinkedHashSet.of(3, 1, 2, 5)
                        .groupMap(i -> 0, i -> i * 10)
                        .get(0)
                        .get())
                .containsExactly(30, 10, 20, 50);
        assertThat(HashSet.of(10, 21, 12).groupMap(i -> i % 2, i -> i / 10))
                .isEqualTo(LinkedHashMap.of(0, HashSet.of(1), 1, HashSet.of(2)));
        assertThat(numbers.groupMapReduce(i -> i % 2, i -> 1, Integer::sum)).isEqualTo(LinkedHashMap.of(0, 3, 1, 2));
    }

    @Test
    public void shouldGroupATreeSetInItsOrderIntoHashSets() {
        TreeSet<Integer> descending = TreeSet.of(Comparator.reverseOrder(), 1, 2, 3, 4, 5, 6);
        Map<Integer, HashSet<String>> grouped = descending.groupMap(i -> i % 3, VALUE);
        assertThat(grouped.keySet().toVector()).containsExactly(0, 2, 1);
        assertThat(grouped)
                .isEqualTo(LinkedHashMap.of(
                        0, HashSet.of("v6", "v3"), 2, HashSet.of("v5", "v2"), 1, HashSet.of("v4", "v1")));
        assertThat(grouped.get(0).get()).isInstanceOf(HashSet.class);
        assertThat(descending.groupMapReduce(i -> i % 3, VALUE, REDUCE))
                .isEqualTo(LinkedHashMap.of(0, "v6,v3", 2, "v5,v2", 1, "v4,v1"));
        // values that are not comparable can be grouped
        assertThat(TreeSet.of(1, 2).groupMap(i -> 0, i -> new Object()).get(0).get())
                .hasSize(2);
    }

    @Test
    public void shouldGroupASetOfEverySizeAsGroupByThenMapDoes() {
        for (int size : SIZES) {
            HashSet<Integer> hash = HashSet.range(0, size);
            checkSet(hash, hash.groupMap(KEY, VALUE), hash.groupMapReduce(KEY, VALUE, REDUCE));
            LinkedHashSet<Integer> linked = LinkedHashSet.range(0, size);
            checkSet(linked, linked.groupMap(KEY, VALUE), linked.groupMapReduce(KEY, VALUE, REDUCE));
            TreeSet<Integer> tree = TreeSet.range(0, size);
            checkSet(tree, tree.groupMap(KEY, VALUE), tree.groupMapReduce(KEY, VALUE, REDUCE));
        }
    }

    private static void checkSet(
            Set<Integer> source, Map<Integer, ? extends Set<String>> grouped, Map<Integer, String> reduced) {
        // the values of a group in the set's iteration order, the keys in the order of their first element
        LinkedHashMap<Integer, Vector<String>> expected = source.foldLeft(
                LinkedHashMap.empty(),
                (groups, element) ->
                        groups.put(KEY.apply(element), Vector.of(VALUE.apply(element)), Vector::appendAll));
        assertThat(grouped).isEqualTo(expected.mapValues(HashSet::ofAll));
        assertThat(grouped.keySet().toVector()).isEqualTo(expected.keySet().toVector());
        assertThat(reduced).isEqualTo(expected.mapValues(group -> group.reduceLeft(REDUCE)));
        assertThat(reduced.keySet().toVector()).isEqualTo(expected.keySet().toVector());
        assertThat(grouped.values().foldLeft(0, (total, group) -> total + group.size()))
                .isEqualTo(source.size());
    }

    @Test
    public void shouldKeepOneOfEqualValuesInASetGroupAndReduceThemAll() {
        var set = HashSet.of(1, 2, 3, 4);
        assertThat(set.groupMap(i -> 0, i -> i % 2)).isEqualTo(LinkedHashMap.of(0, HashSet.of(0, 1)));
        assertThat(set.groupMapReduce(i -> 0, i -> 1, Integer::sum)).isEqualTo(LinkedHashMap.of(0, 4));
    }

    // -- the maps

    @Test
    public void shouldGroupAndMapTheEntriesOfAMapIntoVectors() {
        var prices = LinkedHashMap.of("tea", 3, "coffee", 4, "cake", 5, "water", 1);
        Map<Integer, Vector<String>> byPrice = prices.groupMap(e -> e._2() % 2, Tuple2::_1);
        assertThat(byPrice).isEqualTo(LinkedHashMap.of(1, Vector.of("tea", "cake", "water"), 0, Vector.of("coffee")));
        assertThat(byPrice.keySet().toVector()).containsExactly(1, 0);
        assertThat(prices.groupMapReduce(e -> e._2() % 2, Tuple2::_2, Integer::sum))
                .isEqualTo(LinkedHashMap.of(1, 9, 0, 4));
        assertThat(TreeMap.of("b", 1, "a", 2, "c", 3)
                        .groupMap(e -> 0, Tuple2::_1)
                        .get(0)
                        .get())
                .containsExactly("a", "b", "c");
    }

    @Test
    public void shouldGroupAMapOfEverySizeAsGroupByThenMapDoes() {
        for (int size : SIZES) {
            Vector<Tuple2<Integer, Integer>> entries = Vector.range(0, size).map(i -> Tuple.of(i, -i));
            checkMap(HashMap.ofEntries(entries));
            checkMap(LinkedHashMap.ofEntries(entries));
            checkMap(TreeMap.<Integer, Integer>ofEntries(entries));
        }
    }

    private static void checkMap(Map<Integer, Integer> map) {
        Map<Integer, Vector<String>> grouped = map.groupMap(ENTRY_KEY, ENTRY_VALUE);
        Map<Integer, String> reduced = map.groupMapReduce(ENTRY_KEY, ENTRY_VALUE, REDUCE);
        LinkedHashMap<Integer, Vector<String>> expected = map.foldLeft(
                LinkedHashMap.empty(),
                (groups, entry) ->
                        groups.put(ENTRY_KEY.apply(entry), Vector.of(ENTRY_VALUE.apply(entry)), Vector::appendAll));
        assertThat(grouped).isEqualTo(expected);
        assertThat(grouped.keySet().toVector()).isEqualTo(expected.keySet().toVector());
        for (Tuple2<Integer, Vector<String>> group : grouped) {
            assertThat(group._2()).isEqualTo(expected.get(group._1()).get());
        }
        assertThat(reduced).isEqualTo(expected.mapValues(group -> group.reduceLeft(REDUCE)));
        assertThat(reduced.keySet().toVector()).isEqualTo(expected.keySet().toVector());
    }

    // -- the non-empty collections

    @Test
    public void shouldGroupANonEmptyCollectionIntoANonEmptyMap() {
        for (int size : SIZES) {
            if (size == 0) {
                continue;
            }
            Vector<Integer> range = Vector.range(0, size);
            NonEmptyVector<Integer> vector = NonEmptyVector.unsafeFromVector(range);
            NonEmptyMap<Integer, NonEmptyVector<String>> vectorGroups = vector.groupMap(KEY, VALUE);
            assertThat(vectorGroups.toMap().mapValues(NonEmptyVector::toVector)).isEqualTo(range.groupMap(KEY, VALUE));
            assertThat(vector.groupMapReduce(KEY, VALUE, REDUCE).toMap())
                    .isEqualTo(range.groupMapReduce(KEY, VALUE, REDUCE));

            HashSet<Integer> hash = HashSet.range(0, size);
            NonEmptySet<Integer> set = NonEmptySet.unsafeFromSet(hash);
            NonEmptyMap<Integer, NonEmptySet<String>> setGroups = set.groupMap(KEY, VALUE);
            assertThat(setGroups.toMap().mapValues(NonEmptySet::toSet)).isEqualTo(hash.groupMap(KEY, VALUE));
            assertThat(set.groupMapReduce(KEY, VALUE, REDUCE).toMap())
                    .isEqualTo(hash.groupMapReduce(KEY, VALUE, REDUCE));

            TreeSet<Integer> tree = TreeSet.range(0, size);
            NonEmptySortedSet<Integer> sorted = NonEmptySortedSet.unsafeFromSortedSet(tree);
            NonEmptyMap<Integer, NonEmptySet<String>> sortedGroups = sorted.groupMap(KEY, VALUE);
            assertThat(sortedGroups.toMap().mapValues(NonEmptySet::toSet)).isEqualTo(tree.groupMap(KEY, VALUE));
            assertThat(sorted.groupMapReduce(KEY, VALUE, REDUCE).toMap())
                    .isEqualTo(tree.groupMapReduce(KEY, VALUE, REDUCE));

            HashMap<Integer, Integer> hashMap = HashMap.ofEntries(range.map(i -> Tuple.of(i, -i)));
            NonEmptyMap<Integer, Integer> map = NonEmptyMap.unsafeFromMap(hashMap);
            NonEmptyMap<Integer, NonEmptyVector<String>> mapGroups = map.groupMap(ENTRY_KEY, ENTRY_VALUE);
            assertThat(mapGroups.toMap().mapValues(NonEmptyVector::toVector))
                    .isEqualTo(hashMap.groupMap(ENTRY_KEY, ENTRY_VALUE));
            assertThat(map.groupMapReduce(ENTRY_KEY, ENTRY_VALUE, REDUCE).toMap())
                    .isEqualTo(hashMap.groupMapReduce(ENTRY_KEY, ENTRY_VALUE, REDUCE));

            TreeMap<Integer, Integer> treeMap = TreeMap.ofEntries(range.map(i -> Tuple.of(i, -i)));
            NonEmptySortedMap<Integer, Integer> sortedMap = NonEmptySortedMap.unsafeFromSortedMap(treeMap);
            NonEmptyMap<Integer, NonEmptyVector<String>> sortedMapGroups = sortedMap.groupMap(ENTRY_KEY, ENTRY_VALUE);
            assertThat(sortedMapGroups.toMap().mapValues(NonEmptyVector::toVector))
                    .isEqualTo(treeMap.groupMap(ENTRY_KEY, ENTRY_VALUE));
            assertThat(sortedMap.groupMapReduce(ENTRY_KEY, ENTRY_VALUE, REDUCE).toMap())
                    .isEqualTo(treeMap.groupMapReduce(ENTRY_KEY, ENTRY_VALUE, REDUCE));
        }
    }

    @Test
    public void shouldKeepTheOrderOfANonEmptyCollectionInEachGroup() {
        assertThat(NonEmptyVector.of(5, 3, 8, 1).groupMap(i -> 0, VALUE).get(0).get())
                .containsExactly("v5", "v3", "v8", "v1");
        assertThat(NonEmptyVector.of(5, 3, 8, 1)
                        .groupMapReduce(i -> 0, VALUE, REDUCE)
                        .get(0)
                        .get())
                .isEqualTo("v5,v3,v8,v1");
        assertThat(NonEmptySortedSet.of(Comparator.reverseOrder(), 1, 2, 3)
                        .groupMapReduce(i -> 0, VALUE, REDUCE)
                        .get(0)
                        .get())
                .isEqualTo("v3,v2,v1");
        assertThat(NonEmptySortedMap.of(Tuple.of(2, "b"), Tuple.of(1, "a"))
                        .groupMap(e -> 0, Tuple2::_2)
                        .get(0)
                        .get())
                .containsExactly("a", "b");
    }

    // -- the calls of the functions

    @Test
    public void shouldCallKeyAndValueOncePerElementAndReduceOncePerElementAfterTheFirstOfItsGroup() {
        AtomicInteger keys = new AtomicInteger();
        AtomicInteger values = new AtomicInteger();
        AtomicInteger reduces = new AtomicInteger();
        Vector<Integer> elements = Vector.range(0, 100);
        elements.groupMapReduce(
                i -> {
                    keys.incrementAndGet();
                    return i % 7;
                },
                i -> {
                    values.incrementAndGet();
                    return i;
                },
                (a, b) -> {
                    reduces.incrementAndGet();
                    return a + b;
                });
        assertThat(keys.get()).isEqualTo(100);
        assertThat(values.get()).isEqualTo(100);
        assertThat(reduces.get()).isEqualTo(100 - 7);

        AtomicInteger mapped = new AtomicInteger();
        List.range(0, 50).groupMap(KEY, i -> mapped.incrementAndGet());
        assertThat(mapped.get()).isEqualTo(50);
    }

    @Test
    public void shouldNotChangeTheReceiver() {
        Vector<Integer> vector = Vector.of(1, 2, 3);
        HashMap<Integer, String> map = HashMap.of(1, "a", 2, "b");
        vector.groupMap(KEY, VALUE);
        vector.groupMapReduce(KEY, VALUE, REDUCE);
        map.groupMap(e -> e._1(), Tuple2::_2);
        assertThat(vector).containsExactly(1, 2, 3);
        assertThat(map).containsExactlyInAnyOrder(Tuple.of(1, "a"), Tuple.of(2, "b"));
    }

    // -- null arguments

    @FunctionalInterface
    private interface Call {
        void run(
                Function<Object, Object> key,
                Function<Object, Object> value,
                BiFunction<Object, Object, Object> reduce);
    }

    private static java.util.List<Tuple2<String, Call>> calls() {
        java.util.List<Tuple2<String, Call>> calls = new ArrayList<>();
        // a reduce argument of null marks the groupMap call
        calls.add(Tuple.of("Vector", (k, v, r) -> groupBoth(Vector.of(1), k, v, r)));
        calls.add(Tuple.of("List", (k, v, r) -> {
            if (r == null) {
                List.of(1).groupMap(k, v);
            } else {
                List.of(1).groupMapReduce(k, v, r);
            }
        }));
        calls.add(Tuple.of("Queue", (k, v, r) -> {
            if (r == null) {
                Queue.of(1).groupMap(k, v);
            } else {
                Queue.of(1).groupMapReduce(k, v, r);
            }
        }));
        calls.add(Tuple.of("LazyList", (k, v, r) -> {
            if (r == null) {
                LazyList.of(1).groupMap(k, v);
            } else {
                LazyList.of(1).groupMapReduce(k, v, r);
            }
        }));
        calls.add(Tuple.of("HashSet", (k, v, r) -> groupBoth(HashSet.of(1), k, v, r)));
        calls.add(Tuple.of("LinkedHashSet", (k, v, r) -> groupBoth(LinkedHashSet.of(1), k, v, r)));
        calls.add(Tuple.of("TreeSet", (k, v, r) -> groupBoth(TreeSet.of(1), k, v, r)));
        calls.add(Tuple.of("HashMap", (k, v, r) -> groupBoth(HashMap.of(1, 1), k, v, r)));
        calls.add(Tuple.of("LinkedHashMap", (k, v, r) -> groupBoth(LinkedHashMap.of(1, 1), k, v, r)));
        calls.add(Tuple.of("TreeMap", (k, v, r) -> groupBoth(TreeMap.of(1, 1), k, v, r)));
        calls.add(Tuple.of("NonEmptyVector", (k, v, r) -> {
            if (r == null) {
                NonEmptyVector.of(1).groupMap(k, v);
            } else {
                NonEmptyVector.of(1).groupMapReduce(k, v, r);
            }
        }));
        calls.add(Tuple.of("NonEmptySet", (k, v, r) -> {
            if (r == null) {
                NonEmptySet.of(1).groupMap(k, v);
            } else {
                NonEmptySet.of(1).groupMapReduce(k, v, r);
            }
        }));
        calls.add(Tuple.of("NonEmptySortedSet", (k, v, r) -> {
            if (r == null) {
                NonEmptySortedSet.of(1).groupMap(k, v);
            } else {
                NonEmptySortedSet.of(1).groupMapReduce(k, v, r);
            }
        }));
        calls.add(Tuple.of("NonEmptyMap", (k, v, r) -> {
            if (r == null) {
                NonEmptyMap.of(Tuple.of(1, 1)).groupMap(k, v);
            } else {
                NonEmptyMap.of(Tuple.of(1, 1)).groupMapReduce(k, v, r);
            }
        }));
        calls.add(Tuple.of("NonEmptySortedMap", (k, v, r) -> {
            if (r == null) {
                NonEmptySortedMap.of(Tuple.of(1, 1)).groupMap(k, v);
            } else {
                NonEmptySortedMap.of(Tuple.of(1, 1)).groupMapReduce(k, v, r);
            }
        }));
        return calls;
    }

    private static <T> void groupBoth(
            Traversable<T> source,
            Function<Object, Object> key,
            Function<Object, Object> value,
            BiFunction<Object, Object, Object> reduce) {
        switch (source) {
            case Vector<T> vector when reduce == null -> vector.groupMap(key, value);
            case Vector<T> vector -> vector.groupMapReduce(key, value, reduce);
            case Set<T> set when reduce == null -> set.groupMap(key, value);
            case Set<T> set -> set.groupMapReduce(key, value, reduce);
            case Map<?, ?> map when reduce == null -> map.groupMap(key, value);
            case Map<?, ?> map -> map.groupMapReduce(key, value, reduce);
            default -> throw new IllegalArgumentException(source.toString());
        }
    }

    @Test
    public void shouldRejectANullFunctionArgument() {
        Function<Object, Object> f = x -> x;
        BiFunction<Object, Object, Object> r = (a, b) -> a;
        for (Tuple2<String, Call> call : calls()) {
            assertThatNullPointerException()
                    .as(call._1() + ".groupMap")
                    .isThrownBy(() -> call._2().run(null, f, null))
                    .withMessage("key is null");
            assertThatNullPointerException()
                    .as(call._1() + ".groupMap")
                    .isThrownBy(() -> call._2().run(f, null, null))
                    .withMessage("value is null");
            assertThatNullPointerException()
                    .as(call._1() + ".groupMapReduce")
                    .isThrownBy(() -> call._2().run(null, f, r))
                    .withMessage("key is null");
            assertThatNullPointerException()
                    .as(call._1() + ".groupMapReduce")
                    .isThrownBy(() -> call._2().run(f, null, r))
                    .withMessage("value is null");
        }
    }

    @Test
    public void shouldRejectANullReduceArgument() {
        assertThatNullPointerException()
                .isThrownBy(() -> Vector.of(1).groupMapReduce(KEY, VALUE, null))
                .withMessage("reduce is null");
        assertThatNullPointerException()
                .isThrownBy(() -> Vector.<Integer>empty().groupMapReduce(KEY, VALUE, null))
                .withMessage("reduce is null");
        assertThatNullPointerException()
                .isThrownBy(() -> NonEmptySet.of(1).groupMapReduce(KEY, VALUE, null))
                .withMessage("reduce is null");
    }

    @Test
    public void shouldRejectANullFunctionArgumentOnAnEmptyCollection() {
        assertThatNullPointerException()
                .isThrownBy(() -> List.<Integer>empty().groupMap(null, VALUE))
                .withMessage("key is null");
        assertThatNullPointerException()
                .isThrownBy(() -> HashMap.<Integer, Integer>empty().groupMap(ENTRY_KEY, null))
                .withMessage("value is null");
    }
}
