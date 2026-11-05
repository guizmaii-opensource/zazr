package dev.zazr.collection;

import dev.zazr.Tuple;
import dev.zazr.Tuple2;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Objects;
import java.util.Random;
import java.util.function.BiFunction;
import java.util.function.Function;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/// `equals` of every kind of map compares the keys and the values with their own `equals`, whatever the key
/// comparator of a `TreeMap`: it is symmetric, and equal maps have equal hash codes. The comparator decides the
/// lookups, not `equals`.
class MapEqualityTest {

    /// A key equal to another of the same id and tag; `BY_ID` compares the ids only.
    private static final class Key {
        private final int id;
        private final String tag;

        Key(int id, String tag) {
            this.id = id;
            this.tag = tag;
        }

        @Override
        public boolean equals(Object that) {
            return that instanceof Key key && key.id == id && key.tag.equals(tag);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id, tag);
        }

        @Override
        public String toString() {
            return id + tag;
        }
    }

    /// A key equal to another of the same id, whatever the tags; `BY_TAG_THEN_ID_LOOSE` tells the tags apart, so a
    /// `TreeMap` ordered by it can hold two loose keys equal to each other.
    private static final class Loose {
        private final int id;
        private final String tag;

        Loose(int id, String tag) {
            this.id = id;
            this.tag = tag;
        }

        @Override
        public boolean equals(Object that) {
            return that instanceof Loose loose && loose.id == id;
        }

        @Override
        public int hashCode() {
            return id;
        }

        @Override
        public String toString() {
            return id + tag;
        }
    }

    /// A key equal to another of the same id, whatever the tags, whose hash code is the last digit of the id:
    /// `1a` and `11a` are unequal with the same hash code.
    private static final class Mod {
        private final int id;
        private final String tag;

        Mod(int id, String tag) {
            this.id = id;
            this.tag = tag;
        }

        @Override
        public boolean equals(Object that) {
            return that instanceof Mod mod && mod.id == id;
        }

        @Override
        public int hashCode() {
            return id % 10;
        }

        @Override
        public String toString() {
            return id + tag;
        }
    }

    private static final Comparator<Mod> BY_TAG_THEN_ID_MOD =
            Comparator.<Mod, String>comparing(mod -> mod.tag).thenComparingInt(mod -> mod.id);

    private static final Comparator<Key> BY_ID = Comparator.comparingInt(key -> key.id);

    private static final Comparator<Key> BY_TAG_THEN_ID =
            Comparator.<Key, String>comparing(key -> key.tag).thenComparingInt(key -> key.id);

    private static final Comparator<Loose> BY_TAG_THEN_ID_LOOSE =
            Comparator.<Loose, String>comparing(loose -> loose.tag).thenComparingInt(loose -> loose.id);

    private static final Comparator<Loose> BY_ID_LOOSE = Comparator.comparingInt(loose -> loose.id);

    /// A kind of map, made from the given entries.
    private record Kind<K>(String name, Function<java.util.List<Tuple2<K, Integer>>, Map<K, Integer>> of) {
        Map<K, Integer> make(java.util.List<Tuple2<K, Integer>> entries) {
            return of.apply(entries);
        }

        @Override
        public String toString() {
            return name;
        }
    }

    private static final java.util.List<Kind<Key>> KINDS = java.util.List.of(
            new Kind<>("HashMap", HashMap::ofEntries),
            new Kind<>("LinkedHashMap", LinkedHashMap::ofEntries),
            new Kind<>("TreeMap by id", entries -> TreeMap.ofEntries(BY_ID, entries)),
            new Kind<>("TreeMap by id, reversed", entries -> TreeMap.ofEntries(BY_ID.reversed(), entries)),
            new Kind<>("TreeMap by tag then id", entries -> TreeMap.ofEntries(BY_TAG_THEN_ID, entries)));

    private static final java.util.List<Kind<Loose>> LOOSE_KINDS = java.util.List.of(
            new Kind<>("HashMap", HashMap::ofEntries),
            new Kind<>("LinkedHashMap", LinkedHashMap::ofEntries),
            new Kind<>("TreeMap by id", entries -> TreeMap.ofEntries(BY_ID_LOOSE, entries)),
            new Kind<>("TreeMap by tag then id", entries -> TreeMap.ofEntries(BY_TAG_THEN_ID_LOOSE, entries)),
            new Kind<>(
                    "TreeMap by tag then id, reversed",
                    entries -> TreeMap.ofEntries(BY_TAG_THEN_ID_LOOSE.reversed(), entries)));

    private static Key own(int id) {
        return new Key(id, "own");
    }

    // a key BY_ID equates with own(id), but not equal to it
    private static Key other(int id) {
        return new Key(id, "other");
    }

    private static <K> Tuple2<K, Integer> entry(K key, int value) {
        return Tuple.of(key, value);
    }

    // `a.equals(b)` and `b.equals(a)` both answer `expected`, and equal maps hash alike
    private static void assertEquality(Object a, Object b, boolean expected) {
        String pair = a + " and " + b;
        assertThat(a.equals(b)).as(pair).isEqualTo(expected);
        assertThat(b.equals(a)).as(pair + ", the other way").isEqualTo(expected);
        if (expected) {
            assertThat(a.hashCode()).as(pair).isEqualTo(b.hashCode());
        }
    }

    private static <K> void assertEveryPair(
            java.util.List<Kind<K>> kinds,
            java.util.List<Tuple2<K, Integer>> left,
            java.util.List<Tuple2<K, Integer>> right,
            boolean expected) {
        for (Kind<K> a : kinds) {
            for (Kind<K> b : kinds) {
                Map<K, Integer> x = a.make(left);
                Map<K, Integer> y = b.make(right);
                assertThat(x.size()).as(a + " of " + left).isEqualTo(left.size());
                assertThat(y.size()).as(b + " of " + right).isEqualTo(right.size());
                String what = a + " " + x + " and " + b + " " + y;
                assertThat(x.equals(y)).as(what).isEqualTo(expected);
                assertThat(y.equals(x)).as(what + ", the other way").isEqualTo(expected);
                if (expected) {
                    assertThat(x.hashCode()).as(what).isEqualTo(y.hashCode());
                }
            }
        }
    }

    @Test
    void mapsOfTheSameEntriesAreEqualWhateverTheirKinds() {
        assertEveryPair(
                KINDS,
                java.util.List.of(entry(own(1), 10), entry(own(2), 20)),
                java.util.List.of(entry(own(2), 20), entry(own(1), 10)),
                true);
    }

    @Test
    void mapsThatDifferByAKeyTheComparatorEquatesAreNotEqual() {
        java.util.List<Tuple2<Key, Integer>> owns = java.util.List.of(entry(own(1), 10), entry(own(2), 20));
        java.util.List<Tuple2<Key, Integer>> withOther = java.util.List.of(entry(own(1), 10), entry(other(2), 20));
        assertEveryPair(KINDS, owns, withOther, false);
        // the tree finds the other's key and its value, and still the two are not equal
        TreeMap<Key, Integer> tree = TreeMap.ofEntries(BY_ID, owns);
        assertThat(tree.contains(entry(other(2), 20))).isTrue();
        assertEquality(tree, HashMap.ofEntries(withOther), false);
        assertEquality(tree, TreeMap.ofEntries(BY_ID, withOther), false);
    }

    @Test
    void mapsThatDifferByAValueAreNotEqual() {
        assertEveryPair(
                KINDS,
                java.util.List.of(entry(own(1), 10), entry(own(2), 20)),
                java.util.List.of(entry(own(1), 10), entry(own(2), 21)),
                false);
    }

    @Test
    void mapsOfDifferentSizesAreNotEqual() {
        assertEveryPair(KINDS, java.util.List.of(entry(own(1), 10)), java.util.List.of(), false);
        assertEveryPair(
                KINDS,
                java.util.List.of(entry(own(1), 10), entry(own(2), 20)),
                java.util.List.of(entry(own(1), 10)),
                false);
    }

    @Test
    void emptyMapsAreEqualWhateverTheirKinds() {
        assertEveryPair(KINDS, java.util.List.of(), java.util.List.of(), true);
    }

    @Test
    void largerMapsCompareAcrossKinds() {
        for (int size : new int[] {31, 32, 33, 1023, 1024, 1025}) {
            java.util.List<Tuple2<Key, Integer>> owns = new ArrayList<>();
            java.util.List<Tuple2<Key, Integer>> oneOther = new ArrayList<>();
            for (int i = 0; i < size; i++) {
                owns.add(entry(own(i), i));
                oneOther.add(entry(i == size / 2 ? other(i) : own(i), i));
            }
            assertEveryPair(KINDS, owns, new ArrayList<>(owns.reversed()), true);
            assertEveryPair(KINDS, owns, oneOther, false);
        }
    }

    @Test
    void mapsOfDifferentKeyTypesAreNotEqual() {
        java.util.List<Map<?, ?>> maps = java.util.List.of(
                HashMap.of(1, "a"),
                LinkedHashMap.of(1, "a"),
                TreeMap.of(1, "a"),
                HashMap.of("1", "a"),
                LinkedHashMap.of("1", "a"),
                TreeMap.of("1", "a"));
        for (int i = 0; i < maps.size(); i++) {
            for (int j = 0; j < maps.size(); j++) {
                assertEquality(maps.get(i), maps.get(j), i / 3 == j / 3);
            }
        }
    }

    @Test
    void aMapIsNotEqualToASetOfItsEntriesAJavaMapOrNull() {
        for (Kind<Key> kind : KINDS) {
            Map<Key, Integer> map = kind.make(java.util.List.of(entry(own(1), 10)));
            assertThat(map.equals(HashSet.of(entry(own(1), 10))))
                    .as(kind.name())
                    .isFalse();
            assertThat(map.equals(java.util.Map.of(own(1), 10))).as(kind.name()).isFalse();
            assertThat(map.equals(null)).as(kind.name()).isFalse();
            assertThat(map.equals(map)).as(kind.name()).isTrue();
        }
    }

    @Test
    void theNonEmptyMapsCompareTheKeysWithEquals() {
        NonEmptyMap<Key, Integer> hash = HashMap.ofEntries(java.util.List.of(entry(own(1), 10), entry(own(2), 20)))
                .toNonEmptyMap()
                .get();
        NonEmptySortedMap<Key, Integer> sorted = TreeMap.ofEntries(
                        BY_ID, java.util.List.of(entry(own(2), 20), entry(own(1), 10)))
                .toNonEmptySortedMap()
                .get();
        NonEmptySortedMap<Key, Integer> sortedOther = TreeMap.ofEntries(
                        BY_ID, java.util.List.of(entry(own(1), 10), entry(other(2), 20)))
                .toNonEmptySortedMap()
                .get();
        NonEmptyMap<Key, Integer> hashOther = HashMap.ofEntries(
                        java.util.List.of(entry(own(1), 10), entry(other(2), 20)))
                .toNonEmptyMap()
                .get();
        assertEquality(hash, sorted, true);
        assertEquality(hash, sortedOther, false);
        assertEquality(sorted, hashOther, false);
        assertEquality(sorted, sortedOther, false);
        // never equal to a plain map
        assertEquality(sorted, sorted.toSortedMap(), false);
        assertEquality(hash, hash.toMap(), false);
    }

    // a map of none of Zazr's kinds, which answers every call as `delegate` does
    @SuppressWarnings("unchecked")
    private static <K, V> Map<K, V> anotherKind(Map<K, V> delegate) {
        return (Map<K, V>) java.lang.reflect.Proxy.newProxyInstance(
                Map.class.getClassLoader(), new Class<?>[] {Map.class}, (proxy, method, args) -> {
                    try {
                        return method.invoke(delegate, args);
                    } catch (java.lang.reflect.InvocationTargetException e) {
                        throw e.getCause();
                    }
                });
    }

    @Test
    void anotherKindOfMapIsComparedByItsEntriesNotByItsLookups() {
        Map<Key, Integer> withOther =
                anotherKind(TreeMap.ofEntries(BY_ID, java.util.List.of(entry(own(1), 10), entry(other(2), 20))));
        Map<Key, Integer> owns =
                anotherKind(TreeMap.ofEntries(BY_ID, java.util.List.of(entry(own(2), 20), entry(own(1), 10))));
        assertThat(withOther.contains(entry(own(2), 20))).isTrue();
        for (Kind<Key> kind : KINDS) {
            Map<Key, Integer> map = kind.make(java.util.List.of(entry(own(1), 10), entry(own(2), 20)));
            assertEquality(map, withOther, false);
            assertEquality(map, owns, true);
        }
    }

    // -- a comparator that tells equal keys apart

    @Test
    void aTreeMapHoldingTwoEqualKeysIsNotEqualToAMapOfTwoDifferentOnes() {
        TreeMap<Loose, Integer> twins = TreeMap.ofEntries(
                BY_TAG_THEN_ID_LOOSE, java.util.List.of(entry(new Loose(1, "a"), 1), entry(new Loose(1, "b"), 1)));
        assertThat(twins.size()).isEqualTo(2);
        java.util.List<Tuple2<Loose, Integer>> different =
                java.util.List.of(entry(new Loose(1, "a"), 1), entry(new Loose(2, "a"), 1));
        for (Kind<Loose> kind : LOOSE_KINDS) {
            assertEquality(twins, kind.make(different), false);
        }
        assertEquality(
                twins,
                TreeMap.ofEntries(
                        BY_TAG_THEN_ID_LOOSE.reversed(),
                        java.util.List.of(entry(new Loose(1, "c"), 1), entry(new Loose(1, "d"), 1))),
                true);
    }

    @Test
    void equalKeysStoredInAnotherOrderStillMakeEqualMaps() {
        TreeMap<Loose, Integer> first = TreeMap.ofEntries(
                BY_TAG_THEN_ID_LOOSE, java.util.List.of(entry(new Loose(1, "a"), 1), entry(new Loose(2, "b"), 2)));
        TreeMap<Loose, Integer> second = TreeMap.ofEntries(
                BY_TAG_THEN_ID_LOOSE, java.util.List.of(entry(new Loose(1, "c"), 1), entry(new Loose(2, "a"), 2)));
        assertThat(first.head()._1().id).isEqualTo(1);
        assertThat(second.head()._1().id).isEqualTo(2);
        assertEquality(first, second, true);
        for (Kind<Loose> kind : LOOSE_KINDS) {
            assertEquality(
                    first,
                    kind.make(java.util.List.of(entry(new Loose(2, "x"), 2), entry(new Loose(1, "y"), 1))),
                    true);
        }
        // the same keys, another value
        assertEquality(
                first,
                TreeMap.ofEntries(
                        BY_TAG_THEN_ID_LOOSE,
                        java.util.List.of(entry(new Loose(1, "c"), 1), entry(new Loose(2, "a"), 3))),
                false);
    }

    @Test
    void twoEqualKeysAreNotTakenForTwoDifferentOnesOfTheSameHashSum() {
        // 1a and 1b are equal; 1a and 11a are not, and have the same hash code: both sides sum to the same hash
        TreeMap<Mod, Integer> twins = TreeMap.ofEntries(
                BY_TAG_THEN_ID_MOD, java.util.List.of(entry(new Mod(1, "a"), 1), entry(new Mod(1, "b"), 1)));
        java.util.List<Tuple2<Mod, Integer>> different =
                java.util.List.of(entry(new Mod(1, "a"), 1), entry(new Mod(11, "a"), 1));
        assertThat(twins.size()).isEqualTo(2);
        assertThat(twins.hashCode()).isEqualTo(HashMap.ofEntries(different).hashCode());
        assertEquality(twins, HashMap.ofEntries(different), false);
        assertEquality(twins, LinkedHashMap.ofEntries(different), false);
        assertEquality(twins, TreeMap.ofEntries(BY_TAG_THEN_ID_MOD, different), false);
        assertEquality(twins, TreeMap.ofEntries(BY_TAG_THEN_ID_MOD.reversed(), different), false);
        assertEquality(
                twins,
                TreeMap.ofEntries(
                        BY_TAG_THEN_ID_MOD, java.util.List.of(entry(new Mod(1, "c"), 1), entry(new Mod(1, "d"), 1))),
                true);
    }

    // -- random maps

    // the entries of `map` and how many times each is held, compared with equals
    private static java.util.Map<Object, Integer> counts(Iterable<?> map) {
        java.util.Map<Object, Integer> counts = new java.util.HashMap<>();
        for (Object entry : map) {
            counts.merge(entry, 1, Integer::sum);
        }
        return counts;
    }

    private static <K> void checkRandomPairs(
            java.util.List<Kind<K>> kinds, BiFunction<Integer, String, K> key, long seed) {
        Random random = new Random(seed);
        long equalPairs = java.util.stream.IntStream.range(0, 400)
                .mapToLong(round -> {
                    java.util.List<Tuple2<K, Integer>> left = randomEntries(random, key);
                    java.util.List<Tuple2<K, Integer>> right = randomEntries(random, key);
                    return kinds.stream()
                            .flatMap(a -> kinds.stream().map(b -> checkPair(a.make(left), b.make(right))))
                            .filter(equal -> equal)
                            .count();
                })
                .sum();
        assertThat(equalPairs).isGreaterThan(400);
    }

    // checks `x.equals(y)` both ways against a count of the entries, and returns whether they are equal
    private static <K> boolean checkPair(Map<K, Integer> x, Map<K, Integer> y) {
        boolean expected = x.size() == y.size() && counts(x).equals(counts(y));
        String what = x + " and " + y;
        assertThat(x.equals(y)).as(what).isEqualTo(expected);
        assertThat(y.equals(x)).as(what + ", the other way").isEqualTo(expected);
        if (expected) {
            assertThat(x.hashCode()).as(what).isEqualTo(y.hashCode());
        }
        return expected;
    }

    private static <K> java.util.List<Tuple2<K, Integer>> randomEntries(
            Random random, BiFunction<Integer, String, K> key) {
        int size = random.nextInt(4);
        java.util.List<Tuple2<K, Integer>> entries = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            entries.add(entry(key.apply(random.nextInt(3), random.nextBoolean() ? "x" : "y"), random.nextInt(2)));
        }
        return entries;
    }

    @Test
    void randomMapsAreEqualWhenTheirEntriesAreEqual() {
        checkRandomPairs(KINDS, Key::new, 347L);
    }

    @Test
    void randomMapsUnderAComparatorThatTellsEqualKeysApart() {
        checkRandomPairs(LOOSE_KINDS, Loose::new, 3470L);
    }
}
