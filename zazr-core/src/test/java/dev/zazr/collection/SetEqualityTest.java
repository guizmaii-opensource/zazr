package dev.zazr.collection;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Objects;
import java.util.Random;
import java.util.function.Function;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/// `equals` of every kind of set compares the elements with their own `equals`, whatever the comparator of a
/// `TreeSet`: it is symmetric, and equal sets have equal hash codes. The comparator decides `contains`, not `equals`.
class SetEqualityTest {

    /// An item equal to another of the same key and tag; `BY_KEY` compares the keys only.
    private static final class Item {
        private final int key;
        private final String tag;

        Item(int key, String tag) {
            this.key = key;
            this.tag = tag;
        }

        @Override
        public boolean equals(Object that) {
            return that instanceof Item item && item.key == key && item.tag.equals(tag);
        }

        @Override
        public int hashCode() {
            return Objects.hash(key, tag);
        }

        @Override
        public String toString() {
            return key + tag;
        }
    }

    /// An item equal to another of the same key, whatever the tags; `BY_TAG_THEN_KEY_LOOSE` tells the tags apart, so
    /// a `TreeSet` ordered by it can hold two loose items equal to each other.
    private static final class Loose {
        private final int key;
        private final String tag;

        Loose(int key, String tag) {
            this.key = key;
            this.tag = tag;
        }

        @Override
        public boolean equals(Object that) {
            return that instanceof Loose loose && loose.key == key;
        }

        @Override
        public int hashCode() {
            return key;
        }

        @Override
        public String toString() {
            return key + tag;
        }
    }

    private static final Comparator<Item> BY_KEY = Comparator.comparingInt(item -> item.key);

    // equates the same items as BY_KEY, in the reverse order: not equal to BY_KEY
    private static final Comparator<Item> BY_KEY_REVERSED = BY_KEY.reversed();

    // consistent with equals
    private static final Comparator<Item> BY_TAG_THEN_KEY =
            Comparator.<Item, String>comparing(item -> item.tag).thenComparingInt(item -> item.key);

    // two loose items it tells apart can be equal
    private static final Comparator<Loose> BY_TAG_THEN_KEY_LOOSE =
            Comparator.<Loose, String>comparing(loose -> loose.tag).thenComparingInt(loose -> loose.key);

    private static final Comparator<Loose> BY_KEY_LOOSE = Comparator.comparingInt(loose -> loose.key);

    /// A kind of set, made from the given elements.
    private record Kind<T>(String name, Function<java.util.List<T>, Set<T>> of) {
        Set<T> make(java.util.List<T> elements) {
            return of.apply(elements);
        }

        @Override
        public String toString() {
            return name;
        }
    }

    private static final java.util.List<Kind<Item>> KINDS = java.util.List.of(
            new Kind<>("HashSet", HashSet::ofAll),
            new Kind<>("LinkedHashSet", LinkedHashSet::ofAll),
            new Kind<>("TreeSet by key", items -> TreeSet.ofAll(BY_KEY, items)),
            new Kind<>("TreeSet by key, reversed", items -> TreeSet.ofAll(BY_KEY_REVERSED, items)),
            new Kind<>("TreeSet by tag then key", items -> TreeSet.ofAll(BY_TAG_THEN_KEY, items)));

    private static final java.util.List<Kind<Loose>> LOOSE_KINDS = java.util.List.of(
            new Kind<>("HashSet", HashSet::ofAll),
            new Kind<>("LinkedHashSet", LinkedHashSet::ofAll),
            new Kind<>("TreeSet by key", items -> TreeSet.ofAll(BY_KEY_LOOSE, items)),
            new Kind<>("TreeSet by tag then key", items -> TreeSet.ofAll(BY_TAG_THEN_KEY_LOOSE, items)),
            new Kind<>(
                    "TreeSet by tag then key, reversed",
                    items -> TreeSet.ofAll(BY_TAG_THEN_KEY_LOOSE.reversed(), items)));

    private static Item own(int key) {
        return new Item(key, "own");
    }

    // an item BY_KEY equates with own(key), but not equal to it
    private static Item other(int key) {
        return new Item(key, "other");
    }

    // `a.equals(b)` and `b.equals(a)` both answer `expected`, and equal sets hash alike
    private static void assertEquality(Object a, Object b, boolean expected) {
        String pair = a + " and " + b;
        assertThat(a.equals(b)).as(pair).isEqualTo(expected);
        assertThat(b.equals(a)).as(pair + ", the other way").isEqualTo(expected);
        if (expected) {
            assertThat(a.hashCode()).as(pair).isEqualTo(b.hashCode());
        }
    }

    private static <T> void assertEveryPair(
            java.util.List<Kind<T>> kinds, java.util.List<T> left, java.util.List<T> right, boolean expected) {
        for (Kind<T> a : kinds) {
            for (Kind<T> b : kinds) {
                Set<T> x = a.make(left);
                Set<T> y = b.make(right);
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
    void setsOfTheSameElementsAreEqualWhateverTheirKinds() {
        assertEveryPair(
                KINDS, java.util.List.of(own(1), own(2), own(3)), java.util.List.of(own(3), own(1), own(2)), true);
    }

    @Test
    void setsThatDifferByAnElementTheComparatorEquatesAreNotEqual() {
        java.util.List<Item> owns = java.util.List.of(own(1), own(2), own(3));
        java.util.List<Item> withOther = java.util.List.of(own(1), other(2), own(3));
        assertEveryPair(KINDS, owns, withOther, false);
        // each contains the other's elements by its comparator, and still the two are not equal
        TreeSet<Item> tree = TreeSet.ofAll(BY_KEY, owns);
        HashSet<Item> hash = HashSet.ofAll(withOther);
        assertThat(tree.contains(other(2))).isTrue();
        assertThat(TreeSet.ofAll(BY_KEY, withOther).containsAll(owns)).isTrue();
        assertEquality(tree, hash, false);
        assertEquality(tree, TreeSet.ofAll(BY_KEY, withOther), false);
    }

    @Test
    void setsThatDifferOnlyByEveryElementAreNotEqual() {
        assertEveryPair(KINDS, java.util.List.of(own(1), own(2)), java.util.List.of(other(1), other(2)), false);
        assertEveryPair(KINDS, java.util.List.of(own(1)), java.util.List.of(other(1)), false);
    }

    @Test
    void setsOfDifferentElementsOrSizesAreNotEqual() {
        assertEveryPair(KINDS, java.util.List.of(own(1), own(2)), java.util.List.of(own(1), own(3)), false);
        assertEveryPair(KINDS, java.util.List.of(own(1), own(2)), java.util.List.of(own(1)), false);
        assertEveryPair(KINDS, java.util.List.of(own(1)), java.util.List.of(), false);
    }

    @Test
    void emptySetsAreEqualWhateverTheirKinds() {
        assertEveryPair(KINDS, java.util.List.of(), java.util.List.of(), true);
        assertEquality(TreeSet.empty(BY_KEY), TreeSet.<String>empty(), true);
        assertThat(TreeSet.empty(BY_KEY).hashCode()).isEqualTo(1);
    }

    @Test
    void largerSetsCompareAcrossKinds() {
        for (int size : new int[] {31, 32, 33, 1023, 1024, 1025}) {
            java.util.List<Item> owns = new ArrayList<>();
            java.util.List<Item> oneOther = new ArrayList<>();
            for (int i = 0; i < size; i++) {
                owns.add(own(i));
                oneOther.add(i == size / 2 ? other(i) : own(i));
            }
            java.util.List<Item> reversed = new ArrayList<>(owns.reversed());
            assertEveryPair(KINDS, owns, reversed, true);
            assertEveryPair(KINDS, owns, oneOther, false);
        }
    }

    @Test
    void setsOfDifferentElementTypesAreNotEqual() {
        Set<Object> ints = HashSet.of(1, 2);
        Set<Object> strings = HashSet.of("1", "2");
        java.util.List<Set<?>> sets = java.util.List.of(
                ints,
                LinkedHashSet.of(1, 2),
                TreeSet.of(1, 2),
                strings,
                LinkedHashSet.of("1", "2"),
                TreeSet.of("1", "2"));
        for (int i = 0; i < sets.size(); i++) {
            for (int j = 0; j < sets.size(); j++) {
                assertEquality(sets.get(i), sets.get(j), i / 3 == j / 3);
            }
        }
    }

    @Test
    void aSetIsNotEqualToASequenceAJavaSetOrNull() {
        for (Kind<Item> kind : KINDS) {
            Set<Item> set = kind.make(java.util.List.of(own(1)));
            assertThat(set.equals(List.of(own(1)))).as(kind.name()).isFalse();
            assertThat(set.equals(Vector.of(own(1)))).as(kind.name()).isFalse();
            assertThat(set.equals(java.util.Set.of(own(1)))).as(kind.name()).isFalse();
            assertThat(set.equals(null)).as(kind.name()).isFalse();
            assertThat(set.equals(set)).as(kind.name()).isTrue();
        }
    }

    @Test
    void theNonEmptySetsCompareTheElementsWithEquals() {
        NonEmptySet<Item> hash = NonEmptySet.fromSet(HashSet.of(own(1), own(2))).get();
        NonEmptySortedSet<Item> sorted = NonEmptySortedSet.fromSortedSet(TreeSet.of(BY_KEY, own(2), own(1)))
                .get();
        NonEmptySortedSet<Item> sortedOther = NonEmptySortedSet.fromSortedSet(TreeSet.of(BY_KEY, own(1), other(2)))
                .get();
        NonEmptySet<Item> hashOther =
                NonEmptySet.fromSet(HashSet.of(own(1), other(2))).get();
        assertEquality(hash, sorted, true);
        assertEquality(
                sorted,
                NonEmptySortedSet.fromSortedSet(TreeSet.of(BY_TAG_THEN_KEY, own(1), own(2)))
                        .get(),
                true);
        assertEquality(hash, sortedOther, false);
        assertEquality(sorted, hashOther, false);
        assertEquality(sorted, sortedOther, false);
        // never equal to a plain set
        assertEquality(sorted, TreeSet.of(BY_KEY, own(1), own(2)), false);
        assertEquality(hash, HashSet.of(own(1), own(2)), false);
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

    @Test
    void anotherKindOfSetIsComparedByItsElementsNotByItsContains() {
        Set<Item> withOther = anotherKind(TreeSet.of(BY_KEY, own(1), other(2)));
        Set<Item> owns = anotherKind(TreeSet.of(BY_KEY, own(2), own(1)));
        assertThat(withOther.contains(own(2))).isTrue();
        for (Kind<Item> kind : KINDS) {
            Set<Item> set = kind.make(java.util.List.of(own(1), own(2)));
            assertEquality(set, withOther, false);
            assertEquality(set, owns, true);
        }
    }

    // -- a comparator that tells equal elements apart

    @Test
    void aTreeSetHoldingTwoEqualElementsIsNotEqualToASetOfTwoDifferentOnes() {
        TreeSet<Loose> twins = TreeSet.of(BY_TAG_THEN_KEY_LOOSE, new Loose(1, "a"), new Loose(1, "b"));
        assertThat(twins.size()).isEqualTo(2);
        java.util.List<Loose> different = java.util.List.of(new Loose(1, "a"), new Loose(2, "a"));
        for (Kind<Loose> kind : LOOSE_KINDS) {
            assertEquality(twins, kind.make(different), false);
        }
        // the same tree against twins of its own comparator, in another order
        assertEquality(twins, TreeSet.of(BY_TAG_THEN_KEY_LOOSE, new Loose(1, "c"), new Loose(1, "d")), true);
        assertEquality(twins, TreeSet.of(BY_TAG_THEN_KEY_LOOSE.reversed(), new Loose(1, "c"), new Loose(1, "d")), true);
        // two twins and one more, against one and two twins: the same elements, not as many times each
        TreeSet<Loose> moreOnes =
                TreeSet.of(BY_TAG_THEN_KEY_LOOSE, new Loose(1, "a"), new Loose(1, "b"), new Loose(2, "a"));
        TreeSet<Loose> moreTwos =
                TreeSet.of(BY_TAG_THEN_KEY_LOOSE, new Loose(1, "a"), new Loose(2, "b"), new Loose(2, "c"));
        assertEquality(moreOnes, moreTwos, false);
        assertEquality(moreOnes, TreeSet.ofAll(BY_TAG_THEN_KEY_LOOSE.reversed(), moreTwos), false);
    }

    @Test
    void equalElementsStoredInAnotherOrderStillMakeEqualSets() {
        // equal by key, but the tags put them in the opposite order in each tree
        TreeSet<Loose> first = TreeSet.of(BY_TAG_THEN_KEY_LOOSE, new Loose(1, "a"), new Loose(2, "b"));
        TreeSet<Loose> second = TreeSet.of(BY_TAG_THEN_KEY_LOOSE, new Loose(1, "c"), new Loose(2, "a"));
        assertThat(first.head().key).isEqualTo(1);
        assertThat(second.head().key).isEqualTo(2);
        assertEquality(first, second, true);
        for (Kind<Loose> kind : LOOSE_KINDS) {
            assertEquality(first, kind.make(java.util.List.of(new Loose(2, "x"), new Loose(1, "y"))), true);
        }
    }

    // -- random sets

    // the elements of `set` and how many times each is held, compared with equals
    private static java.util.Map<Object, Integer> counts(Iterable<?> set) {
        java.util.Map<Object, Integer> counts = new java.util.HashMap<>();
        for (Object element : set) {
            counts.merge(element, 1, Integer::sum);
        }
        return counts;
    }

    private static <T> void checkRandomPairs(
            java.util.List<Kind<T>> kinds, Function<Random, java.util.List<T>> elements, long seed) {
        Random random = new Random(seed);
        int equalPairs = 0;
        for (int round = 0; round < 400; round++) {
            java.util.List<T> left = elements.apply(random);
            java.util.List<T> right = elements.apply(random);
            for (Kind<T> a : kinds) {
                for (Kind<T> b : kinds) {
                    Set<T> x = a.make(left);
                    Set<T> y = b.make(right);
                    boolean expected = x.size() == y.size() && counts(x).equals(counts(y));
                    equalPairs += expected ? 1 : 0;
                    String what = a + " " + x + " and " + b + " " + y;
                    assertThat(x.equals(y)).as(what).isEqualTo(expected);
                    assertThat(y.equals(x)).as(what + ", the other way").isEqualTo(expected);
                    if (expected) {
                        assertThat(x.hashCode()).as(what).isEqualTo(y.hashCode());
                    }
                }
            }
        }
        // the generator makes equal pairs often enough for the check to mean something
        assertThat(equalPairs).isGreaterThan(400);
    }

    private static <T> java.util.List<T> randomElements(
            Random random, java.util.function.BiFunction<Integer, String, T> make) {
        int size = random.nextInt(5);
        java.util.List<T> elements = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            elements.add(make.apply(random.nextInt(3), random.nextBoolean() ? "x" : "y"));
        }
        return elements;
    }

    @Test
    void randomSetsAreEqualWhenTheirElementsAreEqual() {
        checkRandomPairs(KINDS, random -> randomElements(random, Item::new), 347L);
    }

    @Test
    void randomSetsUnderAComparatorThatTellsEqualElementsApart() {
        checkRandomPairs(LOOSE_KINDS, random -> randomElements(random, Loose::new), 3470L);
    }
}
