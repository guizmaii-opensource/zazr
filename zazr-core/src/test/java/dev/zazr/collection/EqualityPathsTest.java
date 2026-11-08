package dev.zazr.collection;

import dev.zazr.Tuple;
import dev.zazr.Tuple2;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.function.Function;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/// `equals` of sets and maps of every pair of kinds, against a model count of their elements (entries), on every
/// subset of a small universe; and which way it decided, read from the `equals`, `hashCode` and `compare` calls
/// made by instrumented elements. The answers are those of a count, whichever way the decision is reached.
class EqualityPathsTest {

    /// The calls made on the elements of one test.
    private static final class Calls {
        int equals;
        int hashCodes;
        int compares;
        // when set, ARMED_REVERSED throws
        boolean armed;

        void reset() {
            equals = 0;
            hashCodes = 0;
            compares = 0;
        }
    }

    /// An element with a key and a tag. A strict one equals another of the same key and tag; a loose
    /// one equals another of the same key, whatever the tags. The hash code is the key modulo `hashes`,
    /// so `hashes == 1` makes every hash code collide. The natural order is by key, then by tag: under
    /// it, two loose elements of one key and two tags are told apart although equal.
    private static final class Elem implements Comparable<Elem> {
        private final int key;
        private final String tag;
        private final boolean loose;
        private final int hashes;
        private final Calls calls;

        Elem(int key, String tag, boolean loose, int hashes, Calls calls) {
            this.key = key;
            this.tag = tag;
            this.loose = loose;
            this.hashes = hashes;
            this.calls = calls;
        }

        @Override
        public boolean equals(Object that) {
            calls.equals++;
            return that instanceof Elem e && e.key == key && (loose || e.tag.equals(tag));
        }

        @Override
        public int hashCode() {
            calls.hashCodes++;
            return key % hashes;
        }

        @Override
        public int compareTo(Elem that) {
            calls.compares++;
            int byKey = Integer.compare(key, that.key);
            return byKey != 0 ? byKey : tag.compareTo(that.tag);
        }

        @Override
        public String toString() {
            return key + tag;
        }
    }

    // the order of the natural one, as another comparator: not equal to it
    private static final Comparator<Elem> BY_KEY_THEN_TAG = (a, b) -> {
        a.calls.compares++;
        int byKey = Integer.compare(a.key, b.key);
        return byKey != 0 ? byKey : a.tag.compareTo(b.tag);
    };

    // equates two strict elements of one key and two tags, which are not equal
    private static final Comparator<Elem> BY_KEY = (a, b) -> {
        a.calls.compares++;
        return Integer.compare(a.key, b.key);
    };

    // tells apart two loose elements of one key and two tags, which are equal
    private static final Comparator<Elem> BY_TAG_THEN_KEY = (a, b) -> {
        a.calls.compares++;
        int byTag = a.tag.compareTo(b.tag);
        return byTag != 0 ? byTag : Integer.compare(a.key, b.key);
    };

    // the reverse of the natural order, which throws once armed
    private static final Comparator<Elem> ARMED_REVERSED = (a, b) -> {
        if (a.calls.armed) {
            throw new IllegalStateException("armed");
        }
        return b.compareTo(a);
    };

    private record SetKind(String name, Function<java.util.List<Elem>, Set<Elem>> of) {
        @Override
        public String toString() {
            return name;
        }
    }

    // a set of none of Zazr's kinds, which answers every call as `delegate` does
    @SuppressWarnings("unchecked")
    private static <T> Set<T> anotherKind(Set<T> delegate) {
        return (Set<T>) java.lang.reflect.Proxy.newProxyInstance(
                Set.class.getClassLoader(), new Class<?>[] {Set.class}, (proxy, method, args) -> {
                    try {
                        return method.invoke(delegate, args);
                    } catch (java.lang.reflect.InvocationTargetException e) {
                        throw e.getCause();
                    }
                });
    }

    private static final java.util.List<SetKind> SET_KINDS = java.util.List.of(
            new SetKind("HashSet", HashSet::ofAll),
            new SetKind("LinkedHashSet", LinkedHashSet::ofAll),
            new SetKind("TreeSet natural", TreeSet::ofAll),
            new SetKind("TreeSet natural, reversed", items -> TreeSet.ofAll(Comparator.<Elem>reverseOrder(), items)),
            new SetKind("TreeSet by key then tag", items -> TreeSet.ofAll(BY_KEY_THEN_TAG, items)),
            new SetKind("TreeSet by key", items -> TreeSet.ofAll(BY_KEY, items)),
            new SetKind("TreeSet by key, reversed", items -> TreeSet.ofAll(BY_KEY.reversed(), items)),
            new SetKind("TreeSet by tag then key", items -> TreeSet.ofAll(BY_TAG_THEN_KEY, items)),
            new SetKind("another kind of set", items -> anotherKind(TreeSet.ofAll(BY_TAG_THEN_KEY, items))));

    // every subset of `universe`, in the order of the universe
    private static <T> java.util.List<java.util.List<T>> subsets(java.util.List<T> universe) {
        java.util.List<java.util.List<T>> subsets = new ArrayList<>();
        for (int mask = 0; mask < 1 << universe.size(); mask++) {
            java.util.List<T> subset = new ArrayList<>();
            for (int i = 0; i < universe.size(); i++) {
                if ((mask & 1 << i) != 0) {
                    subset.add(universe.get(i));
                }
            }
            subsets.add(subset);
        }
        return subsets;
    }

    // how many times each element is held, compared with equals
    private static java.util.Map<Object, Integer> counts(Iterable<?> collection) {
        java.util.Map<Object, Integer> counts = new java.util.HashMap<>();
        for (Object element : collection) {
            counts.merge(element, 1, Integer::sum);
        }
        return counts;
    }

    /// A collection made by one kind, with its model count and its non-empty wrapper (null for an empty one, or
    /// when the kind has none).
    private record Made(
            String what,
            Object collection,
            java.util.Map<Object, Integer> counts,
            @Nullable Object wrapped) {}

    // `x.equals(y)` and `y.equals(x)` answer `expected`, and equal collections hash alike
    private static void check(String what, Object x, Object y, boolean expected) {
        assertThat(x.equals(y)).as(what).isEqualTo(expected);
        assertThat(y.equals(x)).as(what + ", the other way").isEqualTo(expected);
        if (expected) {
            assertThat(x.hashCode()).as(what).isEqualTo(y.hashCode());
        }
    }

    // checks every pair of `made`, and returns how many are equal
    private static long checkEveryPair(java.util.List<Made> made) {
        return made.stream()
                .flatMap(a -> made.stream().filter(b -> checkPair(a, b)))
                .count();
    }

    // checks the pair against the model, plain and wrapped, and returns whether it is equal
    private static boolean checkPair(Made a, Made b) {
        boolean expected = a.counts().equals(b.counts());
        String what = a.what() + " and " + b.what();
        check(what, a.collection(), b.collection(), expected);
        if (a.wrapped() != null && b.wrapped() != null) {
            check(what + ", wrapped", a.wrapped(), b.wrapped(), expected);
            check(what + ", one wrapped", a.wrapped(), b.collection(), false);
        }
        return expected;
    }

    private static @Nullable Object wrapSet(Set<Elem> set) {
        if (set.isEmpty()) {
            return null;
        } else if (set instanceof TreeSet<Elem> sorted) {
            return NonEmptySortedSet.fromSortedSet(sorted).get();
        } else if (set instanceof HashSet<Elem> hash) {
            return NonEmptySet.fromSet(hash).get();
        } else {
            return null;
        }
    }

    private static long checkEverySetPair(java.util.List<Elem> universe) {
        java.util.List<Made> made = new ArrayList<>();
        for (java.util.List<Elem> subset : subsets(universe)) {
            for (SetKind kind : SET_KINDS) {
                Set<Elem> set = kind.of().apply(subset);
                made.add(new Made(kind + " " + set, set, counts(set), wrapSet(set)));
            }
        }
        return checkEveryPair(made);
    }

    private static java.util.List<Elem> elements(boolean loose, int hashes, Calls calls, String... keyTags) {
        java.util.List<Elem> elements = new ArrayList<>();
        for (String keyTag : keyTags) {
            elements.add(new Elem(keyTag.charAt(0) - '0', keyTag.substring(1), loose, hashes, calls));
        }
        return elements;
    }

    @Test
    void everyPairOfSetsOfStrictElementsAnswersAsTheCount() {
        Calls calls = new Calls();
        assertThat(checkEverySetPair(elements(false, 1000, calls, "0x", "0y", "1x", "1y")))
                .isGreaterThan(1000);
        // every hash code the same
        assertThat(checkEverySetPair(elements(false, 1, calls, "0x", "0y", "1x", "2y")))
                .isGreaterThan(1000);
    }

    @Test
    void everyPairOfSetsOfLooseElementsAnswersAsTheCount() {
        Calls calls = new Calls();
        // 0x, 0y and 0z are equal: some kinds hold two or three of them, the others one
        assertThat(checkEverySetPair(elements(true, 1000, calls, "0x", "0y", "0z", "1x", "1y", "2x")))
                .isGreaterThan(1000);
        assertThat(checkEverySetPair(elements(true, 2, calls, "0x", "0y", "1x", "1y", "2x", "2y")))
                .isGreaterThan(1000);
    }

    // -- maps

    private record MapKind(String name, Function<java.util.List<Tuple2<Elem, Integer>>, Map<Elem, Integer>> of) {
        @Override
        public String toString() {
            return name;
        }
    }

    private static final java.util.List<MapKind> MAP_KINDS = java.util.List.of(
            new MapKind("HashMap", HashMap::ofEntries),
            new MapKind("LinkedHashMap", LinkedHashMap::ofEntries),
            new MapKind("TreeMap natural", TreeMap::ofEntries),
            new MapKind(
                    "TreeMap natural, reversed",
                    entries -> TreeMap.ofEntries(Comparator.<Elem>reverseOrder(), entries)),
            new MapKind("TreeMap by key then tag", entries -> TreeMap.ofEntries(BY_KEY_THEN_TAG, entries)),
            new MapKind("TreeMap by key", entries -> TreeMap.ofEntries(BY_KEY, entries)),
            new MapKind("TreeMap by key, reversed", entries -> TreeMap.ofEntries(BY_KEY.reversed(), entries)),
            new MapKind("TreeMap by tag then key", entries -> TreeMap.ofEntries(BY_TAG_THEN_KEY, entries)));

    private static @Nullable Object wrapMap(Map<Elem, Integer> map) {
        if (map.isEmpty()) {
            return null;
        } else if (map instanceof TreeMap<Elem, Integer> sorted) {
            return sorted.toNonEmptySortedMap().get();
        } else if (map instanceof HashMap<Elem, Integer> hash) {
            return hash.toNonEmptyMap().get();
        } else {
            return null;
        }
    }

    private static long checkEveryMapPair(java.util.List<Tuple2<Elem, Integer>> universe) {
        java.util.List<Made> made = new ArrayList<>();
        for (java.util.List<Tuple2<Elem, Integer>> subset : subsets(universe)) {
            for (MapKind kind : MAP_KINDS) {
                Map<Elem, Integer> map = kind.of().apply(subset);
                made.add(new Made(kind + " " + map, map, counts(map), wrapMap(map)));
            }
        }
        return checkEveryPair(made);
    }

    private static java.util.List<Tuple2<Elem, Integer>> entries(java.util.List<Elem> keys, Integer... values) {
        java.util.List<Tuple2<Elem, Integer>> entries = new ArrayList<>();
        for (int i = 0; i < keys.size(); i++) {
            entries.add(Tuple.of(keys.get(i), values[i]));
        }
        return entries;
    }

    @Test
    void everyPairOfMapsOfStrictKeysAnswersAsTheCount() {
        Calls calls = new Calls();
        assertThat(checkEveryMapPair(
                        entries(elements(false, 1000, calls, "0x", "0y", "0x", "1y", "1x"), 0, 0, 1, 0, 2)))
                .isGreaterThan(500);
        assertThat(checkEveryMapPair(entries(elements(false, 1, calls, "0x", "0y", "0x", "1y", "1x"), 0, 0, 1, 0, 2)))
                .isGreaterThan(500);
    }

    @Test
    void everyPairOfMapsOfLooseKeysAnswersAsTheCount() {
        Calls calls = new Calls();
        assertThat(checkEveryMapPair(
                        entries(elements(true, 1000, calls, "0x", "0y", "0x", "1x", "1y", "2x"), 0, 0, 1, 0, 0, 2)))
                .isGreaterThan(500);
        assertThat(checkEveryMapPair(
                        entries(elements(true, 2, calls, "0x", "0y", "0z", "1x", "1y", "2x"), 0, 0, 1, 0, 0, 2)))
                .isGreaterThan(500);
    }

    // -- which way it decided

    private static final int N = 200;

    // N strict elements of distinct hash codes: keys 0 to N - 1, then `last` instead of N - 1
    private static java.util.List<Elem> keys(Calls calls, int hashes, int last) {
        java.util.List<Elem> elements = new ArrayList<>();
        for (int i = 0; i < N; i++) {
            elements.add(new Elem(i == N - 1 ? last : i, "x", false, hashes, calls));
        }
        return elements;
    }

    private static java.util.List<Elem> keys(Calls calls) {
        return keys(calls, 1_000_000, N - 1);
    }

    // a.equals(b) with the calls counted from zero
    private static boolean counted(Calls calls, Object a, Object b) {
        calls.reset();
        return a.equals(b);
    }

    @Test
    void equalTreeSetsOfTwoOrdersArePairedByLookupsAfterTheHashSums() {
        Calls calls = new Calls();
        TreeSet<Elem> natural = TreeSet.ofAll(keys(calls));
        TreeSet<Elem> reversed = TreeSet.ofAll(Comparator.reverseOrder(), keys(calls));
        assertThat(counted(calls, natural, reversed)).isTrue();
        // the first pair of the in-order walk, then one per element; two lookups per element
        assertThat(calls.equals).isEqualTo(N + 1);
        // one hash code per element of each side, and none for a count
        assertThat(calls.hashCodes).isEqualTo(2 * N);
        assertThat(calls.compares).isPositive().isLessThanOrEqualTo(N * 2 * 2 * 9);
        assertThat(counted(calls, reversed, natural)).isTrue();
        assertThat(calls.equals).isEqualTo(N + 1);
        assertThat(calls.hashCodes).isEqualTo(2 * N);
    }

    @Test
    void equalTreeSetsOfTwoComparatorsOfOneOrderAreWalkedInOrder() {
        Calls calls = new Calls();
        TreeSet<Elem> natural = TreeSet.ofAll(keys(calls));
        TreeSet<Elem> byKeyThenTag = TreeSet.ofAll(BY_KEY_THEN_TAG, keys(calls));
        assertThat(counted(calls, natural, byKeyThenTag)).isTrue();
        assertThat(calls.equals).isEqualTo(N);
        assertThat(calls.hashCodes).isZero();
        assertThat(calls.compares).isZero();
        assertThat(counted(calls, byKeyThenTag, natural)).isTrue();
        assertThat(calls.compares).isZero();
    }

    @Test
    void treeSetsOfTwoOrdersThatDifferInTheLastElementDifferByTheirHashSums() {
        Calls calls = new Calls();
        TreeSet<Elem> natural = TreeSet.ofAll(keys(calls));
        TreeSet<Elem> reversed = TreeSet.ofAll(Comparator.reverseOrder(), keys(calls, 1_000_000, N + 5));
        assertThat(counted(calls, natural, reversed)).isFalse();
        // one hash code per element of each side, none for a count, and no lookup
        assertThat(calls.hashCodes).isEqualTo(2 * N);
        assertThat(calls.compares).isZero();
        assertThat(counted(calls, reversed, natural)).isFalse();
        assertThat(calls.hashCodes).isEqualTo(2 * N);
        assertThat(calls.compares).isZero();
    }

    @Test
    void treeSetsOfTwoOrdersThatDifferWithTheSameHashSumAreCounted() {
        Calls calls = new Calls();
        // every hash code the same
        TreeSet<Elem> natural = TreeSet.ofAll(keys(calls, 1, N - 1));
        TreeSet<Elem> reversed = TreeSet.ofAll(Comparator.reverseOrder(), keys(calls, 1, N + 5));
        assertThat(counted(calls, natural, reversed)).isFalse();
        // the hash sums, the lookups, then the count
        assertThat(calls.hashCodes).isGreaterThan(2 * N);
        assertThat(calls.compares).isPositive();
        assertThat(counted(calls, reversed, natural)).isFalse();
        assertThat(calls.hashCodes).isGreaterThan(2 * N);
        assertThat(calls.compares).isPositive();
    }

    @Test
    void treeSetsUnderAComparatorThatTellsEqualElementsApartDifferByTheirHashSumsWithNoLookup() {
        Calls calls = new Calls();
        // 1b equals 1c, but 2a is not 3d; by tag then key 2a comes first
        TreeSet<Elem> byTag = TreeSet.ofAll(BY_TAG_THEN_KEY, elements(true, 1000, calls, "1b", "2a"));
        TreeSet<Elem> natural = TreeSet.ofAll(elements(true, 1000, calls, "1c", "3d"));
        assertThat(counted(calls, byTag, natural)).isFalse();
        assertThat(calls.hashCodes).isEqualTo(4);
        assertThat(calls.compares).isZero();
        assertThat(counted(calls, natural, byTag)).isFalse();
        assertThat(calls.hashCodes).isEqualTo(4);
        assertThat(calls.compares).isZero();
        // the same with every hash code the same: the hash sums are equal, and the count decides
        TreeSet<Elem> collidingByTag = TreeSet.ofAll(BY_TAG_THEN_KEY, elements(true, 1, calls, "1b", "2a"));
        TreeSet<Elem> collidingNatural = TreeSet.ofAll(elements(true, 1, calls, "1c", "3d"));
        check("colliding byTag and natural", collidingByTag, collidingNatural, false);
    }

    @Test
    void equalTreeSetsTheLookupsCannotPairAreCounted() {
        Calls calls = new Calls();
        // 1b equals 1c and 2a equals 2d, but by tag then key 2a comes first, and the natural order tells 2a from 2d
        TreeSet<Elem> byTag = TreeSet.ofAll(BY_TAG_THEN_KEY, elements(true, 1000, calls, "1b", "2a"));
        TreeSet<Elem> natural = TreeSet.ofAll(elements(true, 1000, calls, "1c", "2d"));
        assertThat(byTag.head().key).isEqualTo(2);
        assertThat(counted(calls, byTag, natural)).isTrue();
        assertThat(calls.hashCodes).isGreaterThan(4);
        assertThat(counted(calls, natural, byTag)).isTrue();
        assertThat(calls.hashCodes).isGreaterThan(4);
        assertThat(byTag.hashCode()).isEqualTo(natural.hashCode());
    }

    @Test
    void twoEqualElementsOfOneTreeAreNotPairedWithOneElementOfTheOther() {
        Calls calls = new Calls();
        // 1a and 1b are equal: each finds 1a in byKey, which finds back 1a only
        TreeSet<Elem> twins = TreeSet.ofAll(BY_TAG_THEN_KEY, elements(true, 1000, calls, "1a", "1b"));
        TreeSet<Elem> byKey = TreeSet.ofAll(BY_KEY, elements(true, 1000, calls, "1a", "2a"));
        assertThat(twins.size()).isEqualTo(2);
        check("twins and byKey", twins, byKey, false);
        // the same with equal hash sums
        TreeSet<Elem> collidingTwins = TreeSet.ofAll(BY_TAG_THEN_KEY, elements(true, 1, calls, "1a", "1b"));
        TreeSet<Elem> collidingByKey = TreeSet.ofAll(BY_KEY, elements(true, 1, calls, "1a", "2a"));
        check("colliding twins and byKey", collidingTwins, collidingByKey, false);
    }

    @Test
    void aComparatorThatThrowsOnTheOtherTreeLeavesTheAnswerToTheCount() {
        Calls calls = new Calls();
        TreeSet<Elem> natural = TreeSet.ofAll(keys(calls));
        TreeSet<Elem> armed = TreeSet.ofAll(ARMED_REVERSED, keys(calls));
        TreeMap<Elem, Integer> naturalMap = TreeMap.ofEntries(keyed(calls, N - 1));
        TreeMap<Elem, Integer> armedMap = TreeMap.ofEntries(ARMED_REVERSED, keyed(calls, N - 1));
        // without N - 1, and without 0
        TreeSet<Elem> lessTheLast = natural.init();
        TreeSet<Elem> lessTheFirst = armed.init();
        calls.armed = true;
        check("natural and armed", natural, armed, true);
        check("natural and armed maps", naturalMap, armedMap, true);
        check("natural and armed, one element less", lessTheLast, lessTheFirst, false);
    }

    @Test
    void aSetOfAnotherKindThatDiffersByItsHashSumIsNotCounted() {
        Calls calls = new Calls();
        HashSet<Elem> hash = HashSet.ofAll(keys(calls));
        TreeSet<Elem> tree = TreeSet.ofAll(keys(calls));
        Set<Elem> other = anotherKind(HashSet.ofAll(keys(calls, 1_000_000, N + 5)));
        Set<Elem> same = anotherKind(HashSet.ofAll(keys(calls)));
        for (Set<Elem> set : java.util.List.<Set<Elem>>of(hash, tree)) {
            assertThat(counted(calls, set, other))
                    .as(set.getClass().getSimpleName())
                    .isFalse();
            // the two hash sums, and no lookup in a count
            assertThat(calls.hashCodes).isEqualTo(2 * N);
            assertThat(calls.equals).isZero();
            assertThat(counted(calls, set, same)).isTrue();
            assertThat(calls.equals).isEqualTo(N);
        }
    }

    // N entries of strict keys and distinct hash codes, the last value `last`
    private static java.util.List<Tuple2<Elem, Integer>> keyed(Calls calls, int last) {
        java.util.List<Tuple2<Elem, Integer>> entries = new ArrayList<>();
        for (Elem key : keys(calls)) {
            entries.add(Tuple.of(key, key.key == N - 1 ? last : key.key));
        }
        return entries;
    }

    @Test
    void treeMapsOfTwoOrdersArePairedByKeyLookups() {
        Calls calls = new Calls();
        TreeMap<Elem, Integer> natural = TreeMap.ofEntries(keyed(calls, N - 1));
        TreeMap<Elem, Integer> reversed = TreeMap.ofEntries(Comparator.reverseOrder(), keyed(calls, N - 1));
        TreeMap<Elem, Integer> byKeyThenTag = TreeMap.ofEntries(BY_KEY_THEN_TAG, keyed(calls, N - 1));
        TreeMap<Elem, Integer> lastValue = TreeMap.ofEntries(Comparator.reverseOrder(), keyed(calls, -1));
        assertThat(counted(calls, natural, reversed)).isTrue();
        // one per entry, and one more if the first pair of the walk compares its keys before its values
        assertThat(calls.equals).isBetween(N, N + 1);
        // the hash sums first: one hash code per key of each side
        assertThat(calls.hashCodes).isEqualTo(2 * N);
        assertThat(calls.compares).isPositive();
        assertThat(counted(calls, reversed, natural)).isTrue();
        assertThat(calls.hashCodes).isEqualTo(2 * N);
        assertThat(counted(calls, natural, byKeyThenTag)).isTrue();
        assertThat(calls.hashCodes).isZero();
        assertThat(calls.compares).isZero();
        // the same keys, the last value different: the hash sums, no lookup and no count
        assertThat(counted(calls, natural, lastValue)).isFalse();
        assertThat(calls.hashCodes).isEqualTo(2 * N);
        assertThat(calls.compares).isZero();
        assertThat(counted(calls, lastValue, natural)).isFalse();
        assertThat(calls.hashCodes).isEqualTo(2 * N);
        assertThat(calls.compares).isZero();
    }

    @Test
    void treeMapsOfTwoOrdersThatDifferOnlyInValuesWithTheSameHashSumAreLookedUpThenCounted() {
        Calls calls = new Calls();
        // the values of the keys 0 and 1 swapped: an entry hashes as 31 times its key's hash plus its value's, so
        // the sums are equal
        java.util.List<Tuple2<Elem, Integer>> swapped = new ArrayList<>();
        for (Tuple2<Elem, Integer> entry : keyed(calls, N - 1)) {
            int key = entry._1().key;
            swapped.add(Tuple.of(entry._1(), key == 0 ? 1 : key == 1 ? 0 : entry._2()));
        }
        TreeMap<Elem, Integer> natural = TreeMap.ofEntries(keyed(calls, N - 1));
        TreeMap<Elem, Integer> reversed = TreeMap.ofEntries(Comparator.reverseOrder(), swapped);
        assertThat(natural.hashCode()).isEqualTo(reversed.hashCode());
        assertThat(counted(calls, natural, reversed)).isFalse();
        // the hash sums, the lookups, then the count
        assertThat(calls.hashCodes).isGreaterThan(2 * N);
        assertThat(calls.compares).isPositive();
        assertThat(counted(calls, reversed, natural)).isFalse();
        assertThat(calls.hashCodes).isGreaterThan(2 * N);
        assertThat(calls.compares).isPositive();
    }
}
