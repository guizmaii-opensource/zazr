package dev.zazr.collection;

import dev.zazr.Tuple2;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Predicate;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

/// `partition(BiPredicate)` of every map against `partition(Predicate)` of the same map: the same sides, in the same
/// order, holding the key and value objects of the map, the map itself as the side that gets every entry, and one call
/// of the predicate per entry, in iteration order, with the key and the value the map holds.
class MapPartitionBiPredicateTest {

    private static final int[] SIZES = {0, 1, 2, 31, 32, 33, 1023, 1024, 1025};

    /// A key of a chosen hash code: two of one hash and different ids collide.
    record Key(int hash, int id) implements Comparable<Key> {
        @Override
        public int hashCode() {
            return hash;
        }

        @Override
        public int compareTo(Key that) {
            int byHash = Integer.compare(hash, that.hash);
            return byHash != 0 ? byHash : Integer.compare(id, that.id);
        }
    }

    // four keys per hash, so that most of the trie is collision nodes; values are new objects
    private static java.util.List<Tuple2<Key, Integer>> entries(int size) {
        java.util.List<Tuple2<Key, Integer>> entries = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            // a scrambled order, so that the insertion order of a LinkedHashMap is not the key order
            int n = (int) ((i * 7919L) % size);
            entries.add(new Tuple2<>(new Key((n / 4) * 0x9E3779B9, n % 4), Integer.valueOf(100_000 + n)));
        }
        return entries;
    }

    // a split that reads both the key and the value
    private static final BiPredicate<Key, Integer> HALF = (key, value) -> (key.id() + value) % 2 == 0;
    private static final BiPredicate<Key, Integer> ALL = (key, value) -> true;
    private static final BiPredicate<Key, Integer> NONE = (key, value) -> false;

    /// Partitions `map` both ways with `predicate` and compares the results. `self` is the map a side is when it gets
    /// every entry (the wrapped map, for a non-empty map).
    private static <M extends Iterable<Tuple2<Key, Integer>>> void compare(
            Iterable<Tuple2<Key, Integer>> map,
            Object self,
            Function<Predicate<Tuple2<Key, Integer>>, Tuple2<M, M>> byEntry,
            Function<BiPredicate<Key, Integer>, Tuple2<M, M>> byKeyAndValue,
            BiPredicate<Key, Integer> predicate,
            String label) {
        java.util.List<Object[]> entryCalls = new ArrayList<>();
        java.util.List<Object[]> pairCalls = new ArrayList<>();
        Tuple2<M, M> expected = byEntry.apply(entry -> {
            entryCalls.add(new Object[] {entry._1(), entry._2()});
            return predicate.test(entry._1(), entry._2());
        });
        Tuple2<M, M> actual = byKeyAndValue.apply((key, value) -> {
            pairCalls.add(new Object[] {key, value});
            return predicate.test(key, value);
        });

        // one call per entry, in iteration order, with the key and the value objects the map holds
        java.util.List<Tuple2<Key, Integer>> held = new ArrayList<>();
        map.forEach(held::add);
        assertThat(pairCalls).as(label + ": calls").hasSize(held.size());
        assertThat(entryCalls).as(label + ": calls of the Tuple2 overload").hasSize(held.size());
        for (int i = 0; i < held.size(); i++) {
            assertThat(pairCalls.get(i)[0])
                    .as(label + ": key of call " + i)
                    .isSameAs(held.get(i)._1());
            assertThat(pairCalls.get(i)[1])
                    .as(label + ": value of call " + i)
                    .isSameAs(held.get(i)._2());
            assertThat(pairCalls.get(i)[0]).isSameAs(entryCalls.get(i)[0]);
            assertThat(pairCalls.get(i)[1]).isSameAs(entryCalls.get(i)[1]);
        }

        // the same sides, of the same class, in the same order
        java.util.Map<Key, Tuple2<Key, Integer>> byKey = new java.util.HashMap<>();
        held.forEach(entry -> byKey.put(entry._1(), entry));
        for (int side = 0; side < 2; side++) {
            M want = side == 0 ? expected._1() : expected._2();
            M got = side == 0 ? actual._1() : actual._2();
            assertThat(got).as(label + ": side " + side).isEqualTo(want);
            assertThat(got.getClass()).isEqualTo(want.getClass());
            assertThat(List.ofAll(got)).as(label + ": order of side " + side).isEqualTo(List.ofAll(want));
            for (Tuple2<Key, Integer> entry : got) {
                Tuple2<Key, Integer> original = byKey.get(entry._1());
                assertThat(entry._1()).as(label + ": key object").isSameAs(original._1());
                assertThat(entry._2()).as(label + ": value object").isSameAs(original._2());
                assertThat(predicate.test(entry._1(), entry._2())).isEqualTo(side == 0);
            }
            assertThat(got == self)
                    .as(label + ": side " + side + " is the map itself")
                    .isEqualTo(want == self);
        }
        int kept = (int) held.stream()
                .filter(entry -> predicate.test(entry._1(), entry._2()))
                .count();
        if (kept == held.size() && !held.isEmpty()) {
            assertThat(actual._1()).as(label + ": every entry kept").isSameAs(self);
        }
        if (kept == 0 && !held.isEmpty()) {
            assertThat(actual._2()).as(label + ": every entry rejected").isSameAs(self);
        }
    }

    @Test
    void hashMapPartitionsAsTheTuple2Overload() {
        for (int size : SIZES) {
            HashMap<Key, Integer> map = HashMap.ofEntries(entries(size));
            for (BiPredicate<Key, Integer> predicate : java.util.List.of(HALF, ALL, NONE)) {
                compare(map, map, map::partition, map::partition, predicate, "HashMap of " + size);
            }
        }
    }

    @Test
    void linkedHashMapPartitionsAsTheTuple2Overload() {
        for (int size : SIZES) {
            LinkedHashMap<Key, Integer> map = LinkedHashMap.ofEntries(entries(size));
            for (BiPredicate<Key, Integer> predicate : java.util.List.of(HALF, ALL, NONE)) {
                compare(map, map, map::partition, map::partition, predicate, "LinkedHashMap of " + size);
            }
        }
    }

    @Test
    void treeMapPartitionsAsTheTuple2Overload() {
        for (int size : SIZES) {
            for (Comparator<Key> order :
                    java.util.List.of(Comparator.<Key>naturalOrder(), Comparator.<Key>reverseOrder())) {
                TreeMap<Key, Integer> map = TreeMap.ofEntries(order, entries(size));
                for (BiPredicate<Key, Integer> predicate : java.util.List.of(HALF, ALL, NONE)) {
                    compare(map, map, map::partition, map::partition, predicate, "TreeMap of " + size);
                    Tuple2<TreeMap<Key, Integer>, TreeMap<Key, Integer>> sides = map.partition(predicate);
                    assertThat(sides._1().comparator()).isSameAs(map.comparator());
                    assertThat(sides._2().comparator()).isSameAs(map.comparator());
                }
            }
        }
    }

    @Test
    void theMapInterfacesPartitionAsTheTuple2Overload() {
        for (int size : SIZES) {
            Map<Key, Integer> map = HashMap.ofEntries(entries(size));
            SortedMap<Key, Integer> sorted = TreeMap.ofEntries(entries(size));
            for (BiPredicate<Key, Integer> predicate : java.util.List.of(HALF, ALL, NONE)) {
                compare(map, map, p -> narrow(map.partition(p)), p -> narrow(map.partition(p)), predicate, "Map");
                compare(
                        sorted,
                        sorted,
                        p -> narrow(sorted.partition(p)),
                        p -> narrow(sorted.partition(p)),
                        predicate,
                        "SortedMap");
            }
        }
    }

    @Test
    void nonEmptyMapPartitionsAsTheTuple2Overload() {
        for (int size : SIZES) {
            if (size == 0) {
                continue;
            }
            NonEmptyMap<Key, Integer> map =
                    NonEmptyMap.fromMap(HashMap.ofEntries(entries(size))).get();
            for (BiPredicate<Key, Integer> predicate : java.util.List.of(HALF, ALL, NONE)) {
                compare(map, map.toMap(), map::partition, map::partition, predicate, "NonEmptyMap of " + size);
            }
        }
    }

    @Test
    void nonEmptySortedMapPartitionsAsTheTuple2Overload() {
        for (int size : SIZES) {
            if (size == 0) {
                continue;
            }
            NonEmptySortedMap<Key, Integer> map = NonEmptySortedMap.fromSortedMap(
                            TreeMap.ofEntries(Comparator.<Key>reverseOrder(), entries(size)))
                    .get();
            for (BiPredicate<Key, Integer> predicate : java.util.List.of(HALF, ALL, NONE)) {
                compare(
                        map,
                        map.toSortedMap(),
                        map::partition,
                        map::partition,
                        predicate,
                        "NonEmptySortedMap of " + size);
            }
        }
    }

    @Test
    void anEmptyMapPartitionsIntoTwoEmptyMapsWithoutCallingThePredicate() {
        BiPredicate<Key, Integer> never = (key, value) -> {
            throw new AssertionError("called on an empty map");
        };
        for (Map<Key, Integer> map : java.util.List.<Map<Key, Integer>>of(
                HashMap.empty(), LinkedHashMap.empty(), TreeMap.<Key, Integer>empty())) {
            Tuple2<? extends Map<Key, Integer>, ? extends Map<Key, Integer>> sides = map.partition(never);
            assertThat(sides._1()).isEmpty();
            assertThat(sides._2()).isEmpty();
            assertThat(sides._1().getClass()).isEqualTo(map.getClass());
            assertThat(sides._2().getClass()).isEqualTo(map.getClass());
        }
    }

    @Test
    void aNullPredicateIsRejectedOnEveryMap() {
        BiPredicate<Key, Integer> none = null;
        java.util.List<Runnable> calls = java.util.List.of(
                () -> HashMap.<Key, Integer>empty().partition(none),
                () -> LinkedHashMap.<Key, Integer>empty().partition(none),
                () -> TreeMap.<Key, Integer>empty().partition(none),
                () -> HashMap.ofEntries(entries(3)).partition(none),
                () -> LinkedHashMap.ofEntries(entries(3)).partition(none),
                () -> TreeMap.ofEntries(entries(3)).partition(none),
                () -> NonEmptyMap.fromMap(HashMap.ofEntries(entries(3))).get().partition(none),
                () -> NonEmptySortedMap.fromSortedMap(TreeMap.ofEntries(entries(3)))
                        .get()
                        .partition(none));
        for (Runnable call : calls) {
            assertThatNullPointerException().isThrownBy(call::run).withMessage("predicate is null");
        }
    }

    @SuppressWarnings("unchecked")
    private static <M extends Iterable<Tuple2<Key, Integer>>> Tuple2<M, M> narrow(
            Tuple2<? extends Iterable<Tuple2<Key, Integer>>, ? extends Iterable<Tuple2<Key, Integer>>> sides) {
        return (Tuple2<M, M>) sides;
    }
}
