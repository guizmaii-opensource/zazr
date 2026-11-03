package dev.zazr.collection;

import dev.zazr.Tuple;
import dev.zazr.Tuple2;
import java.util.ArrayList;
import java.util.function.Function;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/// Which of two equal objects the sets and maps keep, checked on the stored objects with `isSameAs`:
///
/// - `add`, `addAll`, `union`, `intersect` and `retainAll` of a set keep the receiver's element;
/// - `retainAll` of a map keeps the receiver's key and value objects;
/// - `replace` stores an equal element or entry that is another object, and returns the receiver only for the
///   objects already there.
///
/// The equal element is placed alone, beside a few others and deep in a large trie or tree, on either side, and among
/// keys whose hashes collide.
class EqualObjectRuleTest {

    /// A key equal to another of the same `value`, whatever its object; its hash is `hash`, so that keys collide.
    private static final class Key implements Comparable<Key> {
        private final int value;
        private final int hash;

        Key(int value, int hash) {
            this.value = value;
            this.hash = hash;
        }

        @Override
        public boolean equals(Object that) {
            return that instanceof Key key && key.value == value;
        }

        @Override
        public int hashCode() {
            return hash;
        }

        @Override
        public int compareTo(Key that) {
            return Integer.compare(value, that.value);
        }

        @Override
        public String toString() {
            return "Key(" + value + ")";
        }
    }

    /// How the keys of a test are hashed: by value, or all with one hash.
    private enum Hashing {
        SPREAD,
        COLLIDING;

        Key key(int value) {
            return new Key(value, this == SPREAD ? value : 7);
        }

        int[] sizes() {
            return this == SPREAD ? new int[] {1, 2, 33, 1025} : new int[] {1, 2, 3};
        }
    }

    private record SetKind(String name, Function<Iterable<Key>, Set<Key>> ofAll) {}

    private static final java.util.List<SetKind> SETS = java.util.List.of(
            new SetKind("HashSet", HashSet::ofAll),
            new SetKind("LinkedHashSet", LinkedHashSet::ofAll),
            new SetKind("TreeSet", TreeSet::ofAll));

    // `count` keys of the values from `from`
    private static java.util.List<Key> keys(Hashing hashing, int from, int count) {
        java.util.List<Key> result = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            result.add(hashing.key(from + i));
        }
        return result;
    }

    private static java.util.List<Key> concat(java.util.List<Key> first, java.util.List<Key> second) {
        java.util.List<Key> result = new ArrayList<>(first);
        result.addAll(second);
        return result;
    }

    // the object a collection holds for the element equal to `element`
    private static <T> T stored(Iterable<T> collection, T element) {
        for (T t : collection) {
            if (t.equals(element)) {
                return t;
            }
        }
        throw new AssertionError(element + " is not in " + collection);
    }

    private static <T> void assertHoldsObjects(Iterable<T> result, java.util.List<T> objects, String description) {
        for (T object : objects) {
            assertThat(stored(result, object)).as(description + ": " + object).isSameAs(object);
        }
    }

    // -- sets: the receiver's element stays

    @Test
    void shouldKeepTheReceiversElementOfTwoEqualOnes() {
        for (Hashing hashing : Hashing.values()) {
            for (SetKind kind : SETS) {
                for (int n : hashing.sizes()) {
                    for (int m : hashing.sizes()) {
                        checkKeepsTheReceivers(hashing, kind, n, m);
                    }
                }
            }
        }
    }

    // a receiver of n elements, and an argument of m whose first element equals the receiver's last, and whose other
    // elements are new
    private static void checkKeepsTheReceivers(Hashing hashing, SetKind kind, int n, int m) {
        java.util.List<Key> own = keys(hashing, 0, n);
        java.util.List<Key> given = concat(keys(hashing, n - 1, 1), keys(hashing, n, m - 1));
        Key equal = given.getFirst();
        Set<Key> set = kind.ofAll().apply(own);
        String at = kind.name() + " " + hashing + " of " + n + " with " + m;
        for (SetKind argumentKind : SETS) {
            Set<Key> argument = argumentKind.ofAll().apply(given);
            String with = at + " " + argumentKind.name();

            Set<Key> union = set.union(argument);
            assertThat(union.size()).as(with).isEqualTo(n + m - 1);
            assertHoldsObjects(union, own, "union " + with);
            assertHoldsObjects(union, given.subList(1, m), "union, the new elements, " + with);
            assertThat(stored(union, equal)).isNotSameAs(equal);

            Set<Key> intersection = set.intersect(argument);
            assertThat(intersection.size()).as(with).isOne();
            assertThat(stored(intersection, equal)).as("intersect " + with).isSameAs(own.getLast());

            Set<Key> retained = set.retainAll(argument);
            assertThat(retained.size()).as(with).isOne();
            assertThat(stored(retained, equal)).as("retainAll " + with).isSameAs(own.getLast());
        }
        Set<Key> added = set.addAll(given);
        assertHoldsObjects(added, own, "addAll " + at);
        assertThat(set.add(equal)).as("add " + at).isSameAs(set);
        assertThat(set.retainAll(given).size()).as(at).isOne();
        assertThat(stored(set.retainAll(given), equal)).as("retainAll " + at).isSameAs(own.getLast());
    }

    @Test
    void shouldReturnTheSetForAnEqualArgument() {
        for (Hashing hashing : Hashing.values()) {
            for (SetKind kind : SETS) {
                for (int n : hashing.sizes()) {
                    Set<Key> set = kind.ofAll().apply(keys(hashing, 0, n));
                    java.util.List<Key> copies = keys(hashing, 0, n);
                    for (SetKind argumentKind : SETS) {
                        Set<Key> argument = argumentKind.ofAll().apply(copies);
                        String at = kind.name() + " " + hashing + " of " + n + " with " + argumentKind.name();
                        assertThat(set.union(argument)).as("union " + at).isSameAs(set);
                        assertThat(set.intersect(argument)).as("intersect " + at).isSameAs(set);
                        assertThat(set.retainAll(argument)).as("retainAll " + at).isSameAs(set);
                        assertThat(set.addAll(argument)).as("addAll " + at).isSameAs(set);
                    }
                }
            }
        }
    }

    @Test
    void shouldKeepTheReceiversElementsOnTheNonEmptySets() {
        Key own = new Key(1, 1);
        Key equal = new Key(1, 1);
        NonEmptySet<Key> set = NonEmptySet.fromIterable(own, java.util.List.of(new Key(2, 2)));
        NonEmptySortedSet<Key> sorted = NonEmptySortedSet.fromIterable(own, java.util.List.of(new Key(2, 2)));
        assertThat(stored(set.union(HashSet.of(equal, new Key(3, 3))), equal)).isSameAs(own);
        assertThat(stored(set.intersect(HashSet.of(equal)), equal)).isSameAs(own);
        assertThat(stored(sorted.union(TreeSet.of(equal, new Key(3, 3))), equal)).isSameAs(own);
        assertThat(stored(sorted.intersect(TreeSet.of(equal)), equal)).isSameAs(own);
    }

    // -- sets: replace stores an equal element that is another object

    @Test
    void shouldStoreAnEqualElementThatIsAnotherObject() {
        for (Hashing hashing : Hashing.values()) {
            for (int n : hashing.sizes()) {
                java.util.List<Key> own = keys(hashing, 0, n);
                java.util.List<Set<Key>> sets = new ArrayList<>();
                for (SetKind kind : SETS) {
                    sets.add(kind.ofAll().apply(own));
                }
                // a key set, whose map holds values that are not its elements
                sets.add(LinkedHashMap.ofEntries(own.stream().map(k -> Tuple.of(k, "v")).toList()).keySet());
                for (Set<Key> set : sets) {
                    String at = set.getClass().getSimpleName() + " " + hashing + " of " + n;
                    Key replacedKey = own.get(n / 2);
                    Key equal = hashing.key(n / 2);
                    Set<Key> replaced = set.replace(replacedKey, equal);
                    assertThat(replaced).as(at).isNotSameAs(set).isEqualTo(set);
                    assertThat(stored(replaced, equal)).as("replace " + at).isSameAs(equal);
                    assertHoldsObjects(replaced, without(own, n / 2), "replace, the others, " + at);
                    if (set instanceof LinkedHashSet<Key>) {
                        // in the same position
                        assertThat(java.util.List.copyOf(replaced.toVector().asJava()))
                                .as(at)
                                .containsExactlyElementsOf(own);
                        assertThat(replaced.toVector().get(n / 2)).as(at).isSameAs(equal);
                    }
                    assertThat(set.replace(equal, replacedKey)).as("replace by the object there, " + at).isSameAs(set);
                    assertThat(set.replace(hashing.key(n), equal)).as("replace of an absent, " + at).isSameAs(set);
                }
            }
        }
    }

    private static java.util.List<Key> without(java.util.List<Key> keys, int index) {
        java.util.List<Key> result = new ArrayList<>(keys);
        result.remove(index);
        return result;
    }

    // -- maps: retainAll keeps the receiver's entries

    private record MapKind(String name, Function<java.util.List<Tuple2<Key, Key>>, Map<Key, Key>> ofEntries) {}

    private static final java.util.List<MapKind> MAPS = java.util.List.of(
            new MapKind("HashMap", HashMap::ofEntries),
            new MapKind("LinkedHashMap", LinkedHashMap::ofEntries),
            new MapKind("TreeMap", TreeMap::ofEntries),
            new MapKind(
                    "NonEmptyMap",
                    entries -> NonEmptyMap.fromIterable(entries.getFirst(), entries.subList(1, entries.size()))),
            new MapKind(
                    "NonEmptySortedMap",
                    entries -> NonEmptySortedMap.fromIterable(entries.getFirst(), entries.subList(1, entries.size()))));

    // `count` entries of the keys from 0, each with a value of the key's value plus a million
    private static java.util.List<Tuple2<Key, Key>> entries(Hashing hashing, int count) {
        java.util.List<Tuple2<Key, Key>> result = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            result.add(Tuple.of(hashing.key(i), new Key(1_000_000 + i, i)));
        }
        return result;
    }

    // the stored entry of the key equal to `key`
    private static Tuple2<Key, Key> storedEntry(Map<Key, Key> map, Key key) {
        for (Tuple2<Key, Key> entry : map) {
            if (entry._1().equals(key)) {
                return entry;
            }
        }
        throw new AssertionError(key + " is not a key of " + map);
    }

    @Test
    void shouldRetainTheMapsOwnEntries() {
        for (Hashing hashing : Hashing.values()) {
            for (MapKind kind : MAPS) {
                for (int n : hashing.sizes()) {
                    java.util.List<Tuple2<Key, Key>> own = entries(hashing, n);
                    java.util.List<Tuple2<Key, Key>> copies = entries(hashing, n);
                    Map<Key, Key> map = kind.ofEntries().apply(own);
                    String at = kind.name() + " " + hashing + " of " + n;

                    assertThat(map.retainAll(copies)).as("retainAll of equal entries, " + at).isSameAs(map);

                    // the last entry, a copy given twice, an equal key with another value, an absent key
                    Tuple2<Key, Key> last = own.getLast();
                    java.util.List<Tuple2<Key, Key>> given = java.util.List.of(
                            copies.getLast(),
                            copies.getLast(),
                            Tuple.of(hashing.key(0), new Key(-1, 0)),
                            Tuple.of(hashing.key(n), new Key(1_000_000 + n, n)));
                    Map<Key, Key> retained = map.retainAll(given);
                    assertThat(retained.size()).as(at).isOne();
                    Tuple2<Key, Key> entry = storedEntry(retained, last._1());
                    assertThat(entry._1()).as("retainAll, the key, " + at).isSameAs(last._1());
                    assertThat(entry._2()).as("retainAll, the value, " + at).isSameAs(last._2());
                    if (n > 1) {
                        assertThat(retained).as(at).isNotSameAs(map);
                    }

                    // every entry but the first, as copies
                    Map<Key, Key> most = map.retainAll(copies.subList(1, n));
                    assertThat(most.size()).as(at).isEqualTo(n - 1);
                    for (Tuple2<Key, Key> kept : own.subList(1, n)) {
                        Tuple2<Key, Key> stored = storedEntry(most, kept._1());
                        assertThat(stored._1()).as("retainAll of most, " + at).isSameAs(kept._1());
                        assertThat(stored._2()).as("retainAll of most, " + at).isSameAs(kept._2());
                    }
                }
            }
        }
    }

    // -- maps: replace(entry, entry) stores an equal key or value that is another object

    @Test
    void shouldReplaceAnEntryByAnEqualOneOfOtherObjects() {
        for (Hashing hashing : Hashing.values()) {
            for (int n : hashing.sizes()) {
                java.util.List<Tuple2<Key, Key>> own = entries(hashing, n);
                java.util.List<Tuple2<Key, Key>> copies = entries(hashing, n);
                LinkedHashMap<Key, Key> map = LinkedHashMap.ofEntries(own);
                String at = hashing + " of " + n;
                int i = n / 2;
                Tuple2<Key, Key> current = own.get(i);
                Tuple2<Key, Key> copy = copies.get(i);

                LinkedHashMap<Key, Key> newKey = map.replace(current, Tuple.of(copy._1(), current._2()));
                assertThat(newKey).as(at).isNotSameAs(map).isEqualTo(map);
                assertThat(newKey.toVector().get(i)._1()).as("the new key, in place, " + at).isSameAs(copy._1());
                assertThat(newKey.toVector().get(i)._2()).as(at).isSameAs(current._2());
                assertThat(newKey.keySet().toVector().get(i)).as("the key set, " + at).isSameAs(copy._1());

                LinkedHashMap<Key, Key> newValue = map.replace(current, Tuple.of(current._1(), copy._2()));
                assertThat(newValue).as(at).isNotSameAs(map).isEqualTo(map);
                assertThat(newValue.toVector().get(i)._1()).as(at).isSameAs(current._1());
                assertThat(newValue.toVector().get(i)._2()).as("the new value, in place, " + at).isSameAs(copy._2());

                // a new tuple of the objects already there, asked for with an equal copy
                assertThat(map.replace(copy, Tuple.of(current._1(), current._2())))
                        .as("replace by the objects there, " + at)
                        .isSameAs(map);
                assertThat(map.replaceAll(copy, Tuple.of(current._1(), current._2())))
                        .as(at)
                        .isSameAs(map);
                assertThat(map.replace(Tuple.of(current._1(), new Key(-1, 0)), copy))
                        .as("replace of another value, " + at)
                        .isSameAs(map);
                assertThat(map.replace(Tuple.of(hashing.key(n), current._2()), copy))
                        .as("replace of an absent key, " + at)
                        .isSameAs(map);
            }
        }
    }
}
