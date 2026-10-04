package dev.zazr.collection;

import dev.zazr.Tuple;
import dev.zazr.Tuple2;
import dev.zazr.collection.internal.Maps;
import dev.zazr.collection.internal.RedBlackTree;
import dev.zazr.collection.internal.RedBlackTreeModule;
import dev.zazr.control.Option;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/// The key lookups of `TreeMap` (`get`, both `getOrElse`, `containsKey`, `contains`, the `ABSENT` lookup of the
/// internal helpers, `put` with a merge function and `updateWith`) and of `NonEmptySortedMap`, against a
/// `java.util.TreeMap` holding the same puts under the same comparator: natural (the default and an explicit one),
/// reversed, and two that map many keys to one. Every key of a range wider than the stored ones is looked up, so both
/// hits and misses on each side of every stored key are covered, at the sizes around the boundaries.
public class TreeMapLookupTest {

    private static final long SEED = 20261004L;
    private static final int[] SIZES = {0, 1, 2, 3, 31, 32, 33, 1023, 1024, 1025};

    private static final Comparator<Integer> NATURAL = Comparator.naturalOrder();
    private static final Comparator<Integer> REVERSED = Comparator.reverseOrder();
    // two keys a multiple of 1000 apart are the same key
    private static final Comparator<Integer> MODULO_1000 = Comparator.comparingInt(i -> Math.floorMod(i, 1000));
    // at most 7 distinct keys, whatever is put
    private static final Comparator<Integer> MODULO_7 = Comparator.comparingInt(i -> Math.floorMod(i, 7));

    /// A map and its model, built from the same random puts (each value names the put, so the last one wins in both).
    /// A put of a key equal to a stored one stores the key given, so the model removes the key before each put.
    private record Fixture(
            String name, TreeMap<Integer, String> map, java.util.TreeMap<Integer, String> model, int bound) {

        /// Every key from `-bound` to `bound` on the small maps, every 17th on the large ones (for the writes, whose
        /// results are compared whole).
        int[] sampledKeys() {
            int step = map.size() > 100 ? 17 : 1;
            return java.util.stream.IntStream.iterate(-bound, key -> key <= bound, key -> key + step)
                    .toArray();
        }
    }

    // a put on the model as on the map: the key given replaces an equal stored one
    private static void put(java.util.TreeMap<Integer, String> model, Integer key, String value) {
        model.remove(key);
        model.put(key, value);
    }

    // `defaultNatural`: `TreeMap.empty()`, the natural order the map recognises as its own, rather than `comparator`
    private static Fixture fixture(
            String name, Comparator<Integer> comparator, boolean defaultNatural, int size, Random random) {
        java.util.TreeMap<Integer, String> model = new java.util.TreeMap<>(comparator);
        int bound = 2 * size + 2;
        TreeMap<Integer, String> empty = defaultNatural ? TreeMap.empty() : TreeMap.empty(comparator);
        List<Integer> puts = new ArrayList<>();
        for (int i = 0; i < 2 * size && model.size() < size; i++) {
            puts.add(random.nextInt(2 * bound + 1) - bound);
        }
        TreeMap<Integer, String> map = Vector.ofAll(puts).zipWithIndex().foldLeft(empty, (acc, put) -> {
            String value = "v" + put._2();
            put(model, put._1(), value);
            return acc.put(put._1(), value);
        });
        return new Fixture(name + " size " + map.size(), map, model, bound + 3);
    }

    private static List<Fixture> fixtures() {
        Random random = new Random(SEED);
        List<Fixture> fixtures = new ArrayList<>();
        for (int size : SIZES) {
            fixtures.add(fixture("natural (default)", NATURAL, true, size, random));
            fixtures.add(fixture("natural", NATURAL, false, size, random));
            fixtures.add(fixture("reversed", REVERSED, false, size, random));
            fixtures.add(fixture("modulo 1000", MODULO_1000, false, size, random));
            fixtures.add(fixture("modulo 7", MODULO_7, false, size, random));
        }
        return fixtures;
    }

    private static <K, V> List<Tuple2<K, V>> entries(java.util.Map<K, V> model) {
        List<Tuple2<K, V>> entries = new ArrayList<>();
        model.forEach((key, value) -> entries.add(Tuple.of(key, value)));
        return entries;
    }

    private static <K, V> List<Tuple2<K, V>> entries(Iterable<Tuple2<K, V>> map) {
        List<Tuple2<K, V>> entries = new ArrayList<>();
        map.forEach(entries::add);
        return entries;
    }

    // -- the reads

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void readsAgreeWithTheModel() {
        for (Fixture fixture : fixtures()) {
            TreeMap<Integer, String> map = fixture.map();
            // the sentinel is no String: the internal helpers read the map as one of Objects
            Map<Integer, Object> objects = (Map) map;
            java.util.TreeMap<Integer, String> model = fixture.model();
            AtomicInteger supplied = new AtomicInteger();
            Supplier<String> supplier = () -> "s" + supplied.incrementAndGet();
            @SuppressWarnings("Var")
            int misses = 0;
            for (int key = -fixture.bound(); key <= fixture.bound(); key++) {
                String expected = model.get(key);
                String context = fixture.name() + " key " + key;
                if (expected == null) {
                    misses++;
                }
                assertThat(map.get(key)).as(context).isEqualTo(Option.ofNullable(expected));
                assertThat(map.getOrElse(key, "default"))
                        .as(context)
                        .isEqualTo(expected == null ? "default" : expected);
                assertThat(map.getOrElse(key, supplier))
                        .as(context)
                        .isEqualTo(expected == null ? "s" + misses : expected);
                assertThat(map.containsKey(key)).as(context).isEqualTo(model.containsKey(key));
                assertThat(Maps.getOrAbsent(objects, key))
                        .as(context)
                        .isSameAs(expected == null ? Maps.ABSENT : expected);
                assertThat(map.contains(Tuple.of(key, expected == null ? "v0" : expected)))
                        .as(context)
                        .isEqualTo(expected != null);
                assertThat(map.contains(Tuple.of(key, "not a value")))
                        .as(context)
                        .isFalse();
            }
            assertThat(supplied.get()).isEqualTo(misses);
        }
    }

    @Test
    void getReturnsTheStoredValueObject() {
        String value = new String("value");
        TreeMap<String, String> map = TreeMap.of(String.CASE_INSENSITIVE_ORDER, "Key", value);
        assertThat(map.get("KEY").get()).isSameAs(value);
        assertThat(map.getOrElse("key", "default")).isSameAs(value);
        assertThat(map.getOrElse("kEy", () -> "default")).isSameAs(value);
        assertThat(map.containsKey("KEY")).isTrue();
        // a lookup leaves the key object stored
        assertThat(map.head()._1()).isEqualTo("Key");
    }

    @Test
    void eagerGetOrElseReturnsANullDefault() {
        TreeMap<Integer, String> map = TreeMap.of(1, "one");
        assertThat(map.getOrElse(2, (String) null)).isNull();
        assertThat(map.getOrElse(1, (String) null)).isEqualTo("one");
    }

    @Test
    void lazyGetOrElseChecksItsSupplier() {
        TreeMap<Integer, String> map = TreeMap.of(1, "one");
        assertThatThrownBy(() -> map.getOrElse(1, (Supplier<String>) null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("supplier is null");
        assertThatThrownBy(() -> map.getOrElse(2, () -> null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("TreeMap.getOrElse: supplier returned null");
    }

    // -- the writes that look up first

    @Test
    void putWithMergeAgreesWithTheModel() {
        for (Fixture fixture : fixtures()) {
            TreeMap<Integer, String> map = fixture.map();
            List<Tuple2<Integer, String>> before = entries(map);
            for (int key : fixture.sampledKeys()) {
                java.util.TreeMap<Integer, String> model = new java.util.TreeMap<>(fixture.model());
                String current = model.get(key);
                List<String> merged = new ArrayList<>();
                TreeMap<Integer, String> result = map.put(key, "new", (oldValue, newValue) -> {
                    merged.add(oldValue + "/" + newValue);
                    return oldValue + "+" + newValue;
                });
                put(model, key, current == null ? "new" : current + "+new");
                String context = fixture.name() + " key " + key;
                assertThat(entries(result)).as(context).isEqualTo(entries(model));
                assertThat(merged).as(context).isEqualTo(current == null ? List.of() : List.of(current + "/new"));

                TreeMap<Integer, String> viaEntry = map.put(Tuple.of(key, "new"), (o, n) -> o + "+" + n);
                assertThat(entries(viaEntry)).as(context).isEqualTo(entries(model));
            }
            // persistence: the receiver never changes
            assertThat(entries(map)).isEqualTo(before);
        }
    }

    @Test
    void updateWithAgreesWithTheModel() {
        for (Fixture fixture : fixtures()) {
            TreeMap<Integer, String> map = fixture.map();
            List<Tuple2<Integer, String>> before = entries(map);
            for (int key : fixture.sampledKeys()) {
                String current = fixture.model().get(key);
                String context = fixture.name() + " key " + key;
                List<Option<String>> received = new ArrayList<>();

                // present: append to the value; absent: put a new one
                TreeMap<Integer, String> updated = map.updateWith(key, previous -> {
                    received.add(previous);
                    return Option.some(previous.isEmpty() ? "fresh" : previous.get() + "!");
                });
                java.util.TreeMap<Integer, String> model = new java.util.TreeMap<>(fixture.model());
                put(model, key, current == null ? "fresh" : current + "!");
                assertThat(entries(updated)).as(context).isEqualTo(entries(model));
                assertThat(received).as(context).containsExactly(Option.ofNullable(current));

                // None: remove, or the map itself when the key is absent
                TreeMap<Integer, String> removed = map.updateWith(key, _ -> Option.none());
                java.util.TreeMap<Integer, String> removedModel = new java.util.TreeMap<>(fixture.model());
                removedModel.remove(key);
                assertThat(entries(removed)).as(context).isEqualTo(entries(removedModel));
                if (current == null) {
                    assertThat(removed).as(context).isSameAs(map);
                }

                // the value held, or None for an absent key: the map itself
                assertThat(map.updateWith(key, previous -> previous))
                        .as(context)
                        .isSameAs(map);
            }
            assertThat(entries(map)).isEqualTo(before);
        }
    }

    @Test
    void updateWithHandsTheStoredValueObject() {
        String value = new String("value");
        TreeMap<String, String> map = TreeMap.of(String.CASE_INSENSITIVE_ORDER, "Key", value);
        AtomicReference<Option<String>> received = new AtomicReference<>();
        TreeMap<String, String> result = map.updateWith("KEY", previous -> {
            received.set(previous);
            return previous;
        });
        assertThat(received.get().get()).isSameAs(value);
        assertThat(result).isSameAs(map);
        assertThat(result.head()._1()).isEqualTo("Key");
    }

    @Test
    void writesTakeTheGivenKeyObject() {
        // as before the lookups took no Option: a put of an equal key stores the key given, the merge too
        TreeMap<String, Integer> map = TreeMap.of(String.CASE_INSENSITIVE_ORDER, "A", 1);
        assertThat(entries(map.put("a", 2, Integer::sum))).containsExactly(Tuple.of("a", 3));
        assertThat(entries(map.put(Tuple.of("a", 2), Integer::sum))).containsExactly(Tuple.of("a", 3));
        assertThat(entries(map.updateWith("a", previous -> previous.map(v -> v + 1))))
                .containsExactly(Tuple.of("a", 2));
        assertThat(entries(map)).containsExactly(Tuple.of("A", 1));
    }

    // -- null and incomparable keys, as before

    @Test
    void naturalOrderRejectsANullKeyOnlyWhenThereIsAKeyToCompare() {
        TreeMap<String, Integer> empty = TreeMap.empty();
        assertThat(empty.get(null)).isEqualTo(Option.none());
        assertThat(empty.getOrElse(null, 9)).isEqualTo(9);
        assertThat(empty.getOrElse(null, () -> 9)).isEqualTo(9);
        assertThat(empty.containsKey(null)).isFalse();

        TreeMap<String, Integer> map = TreeMap.of("a", 1);
        assertThatThrownBy(() -> map.get(null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> map.getOrElse(null, 9)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> map.getOrElse(null, () -> 9)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> map.containsKey(null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> map.put(null, 2, Integer::sum)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> map.updateWith(null, previous -> previous)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void anExplicitComparatorRejectsANullKeyAsItDecides() {
        TreeMap<Integer, Integer> natural = TreeMap.of(NATURAL, 1, 1);
        assertThatThrownBy(() -> natural.get(null)).isInstanceOf(NullPointerException.class);

        TreeMap<Integer, Integer> nullsFirst = TreeMap.of(Comparator.nullsFirst(NATURAL), 1, 1);
        assertThat(nullsFirst.get(null)).isEqualTo(Option.none());
        assertThat(nullsFirst.getOrElse(null, 9)).isEqualTo(9);
        assertThat(nullsFirst.containsKey(null)).isFalse();
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void naturalOrderRejectsAnIncomparableKey() {
        TreeMap<Object, Integer> map = (TreeMap) TreeMap.of("a", 1);
        assertThatThrownBy(() -> map.get(new Object())).isInstanceOf(ClassCastException.class);
        assertThatThrownBy(() -> map.containsKey(new Object())).isInstanceOf(ClassCastException.class);
        TreeMap<Object, Integer> empty = (TreeMap) TreeMap.<String, Integer>empty();
        assertThat(empty.get(new Object())).isEqualTo(Option.none());
    }

    @Test
    void theLookupKeyIsTheFirstArgumentOfEachComparison() {
        List<Integer> firstArguments = new ArrayList<>();
        Comparator<Integer> recording = (a, b) -> {
            firstArguments.add(a);
            return Integer.compare(a, b);
        };
        TreeMap<Integer, String> map = TreeMap.of(recording, 1, "a", 2, "b", 3, "c");
        firstArguments.clear();
        map.get(42);
        map.getOrElse(42, "x");
        map.containsKey(42);
        assertThat(firstArguments).isNotEmpty().containsOnly(42);
    }

    // -- the non-empty map

    @Test
    void nonEmptySortedMapAgreesWithTheModel() {
        for (Fixture fixture : fixtures()) {
            if (fixture.map().isEmpty()) {
                continue;
            }
            NonEmptySortedMap<Integer, String> map =
                    fixture.map().toNonEmptySortedMap().get();
            java.util.TreeMap<Integer, String> model = fixture.model();
            for (int key : fixture.sampledKeys()) {
                String expected = model.get(key);
                String context = fixture.name() + " key " + key;
                assertThat(map.get(key)).as(context).isEqualTo(Option.ofNullable(expected));
                assertThat(map.getOrElse(key, "default"))
                        .as(context)
                        .isEqualTo(expected == null ? "default" : expected);
                assertThat(map.getOrElse(key, () -> "lazy"))
                        .as(context)
                        .isEqualTo(expected == null ? "lazy" : expected);
                assertThat(map.containsKey(key)).as(context).isEqualTo(model.containsKey(key));
                TreeMap<Integer, String> updated =
                        map.updateWith(key, previous -> Option.some(previous.getOrElse("fresh") + "!"));
                java.util.TreeMap<Integer, String> updatedModel = new java.util.TreeMap<>(model);
                put(updatedModel, key, (expected == null ? "fresh" : expected) + "!");
                assertThat(entries(updated)).as(context).isEqualTo(entries(updatedModel));
            }
        }
        NonEmptySortedMap<Integer, String> one = NonEmptySortedMap.of(Tuple.of(1, "one"));
        assertThatThrownBy(() -> one.getOrElse(2, () -> null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("NonEmptySortedMap.getOrElse: supplier returned null");
    }

    // -- the raw lookup itself, on a set's tree

    @Test
    void findByKeyOnTheElementsOfASet() {
        for (Comparator<Integer> order : List.of(NATURAL, REVERSED, MODULO_7)) {
            RedBlackTree<Integer> tree = RedBlackTree.of(order, 3, 9, -3, 26, 0);
            java.util.TreeSet<Integer> model = new java.util.TreeSet<>(order);
            model.addAll(List.of(3, 9, -3, 26, 0));
            for (int key = -30; key <= 30; key++) {
                Integer expected = model.contains(key) ? model.ceiling(key) : null;
                assertThat(RedBlackTreeModule.Node.findByKey(tree, key, order, false))
                        .as(order + " key " + key)
                        .isEqualTo(expected);
            }
            RedBlackTree<Integer> empty = RedBlackTree.empty(order);
            Integer none = RedBlackTreeModule.Node.findByKey(empty, 1, order, false);
            assertThat(none).isNull();
        }
    }
}
