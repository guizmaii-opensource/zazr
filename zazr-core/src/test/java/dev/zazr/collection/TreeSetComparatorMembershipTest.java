package dev.zazr.collection;

import dev.zazr.collection.internal.RedBlackTree;
import dev.zazr.collection.internal.RedBlackTreeValidity;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Objects;
import java.util.function.Function;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

/// `intersect`, `retainAll`, `diff` and `removeAll` of a `TreeSet` decide membership with the receiver's comparator,
/// as `contains` does, for every kind of argument, under a comparator that equates unequal objects. The elements kept
/// are the receiver's objects, and the receiver comes back when nothing changes.
class TreeSetComparatorMembershipTest {

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

    private static final Comparator<Item> BY_KEY = Comparator.comparingInt(item -> item.key);

    // a comparator that tells the tags apart, so that it is not the receiver's
    private static final Comparator<Item> BY_TAG_THEN_KEY =
            Comparator.<Item, String>comparing(item -> item.tag).thenComparingInt(item -> item.key);

    private static final Item A = new Item(1, "own");
    private static final Item B = new Item(2, "own");
    private static final Item C = new Item(3, "own");

    private static TreeSet<Item> receiver() {
        return TreeSet.of(BY_KEY, A, B, C);
    }

    // an item the comparator equates with the receiver's item of `key`, but not equal to it
    private static Item other(int key) {
        return new Item(key, "other");
    }

    /// A kind of argument, made from the given items.
    private record Kind(String name, Function<java.util.List<Item>, Iterable<Item>> of) {}

    // the kinds `intersect` and `diff` take
    private static final java.util.List<Kind> SETS = java.util.List.of(
            new Kind("a TreeSet of the same comparator", items -> TreeSet.ofAll(BY_KEY, items)),
            new Kind("a TreeSet of another comparator", items -> TreeSet.ofAll(BY_TAG_THEN_KEY, items)),
            new Kind("a HashSet", HashSet::ofAll),
            new Kind("a LinkedHashSet", LinkedHashSet::ofAll));

    // the kinds `retainAll` and `removeAll` take besides
    private static final java.util.List<Kind> ITERABLES = java.util.List.of(
            new Kind("a java.util.List", ArrayList::new),
            new Kind("a java.util.HashSet", java.util.HashSet::new),
            new Kind("a one-shot Iterable", TreeSetComparatorMembershipTest::oneShot));

    private static java.util.List<Kind> allKinds() {
        java.util.List<Kind> kinds = new ArrayList<>(SETS);
        kinds.addAll(ITERABLES);
        return kinds;
    }

    // an Iterable whose iterator can be asked for once only
    private static Iterable<Item> oneShot(java.util.List<Item> items) {
        boolean[] used = {false};
        return () -> {
            assertThat(used[0]).as("iterated once").isFalse();
            used[0] = true;
            return items.iterator();
        };
    }

    @SuppressWarnings("unchecked")
    private static Set<Item> set(Kind kind, java.util.List<Item> items) {
        return (Set<Item>) kind.of().apply(items);
    }

    // the object stored under the key of `item`
    private static Item storedUnder(TreeSet<Item> set, Item item) {
        return set.find(e -> BY_KEY.compare(e, item) == 0).get();
    }

    // -- intersect and retainAll: an element stays when the argument holds one the comparator equates with it

    @Test
    void intersectKeepsTheReceiversObjectForAnEquatedElementOfEveryKind() {
        for (Kind kind : SETS) {
            TreeSet<Item> result = receiver().intersect(set(kind, java.util.List.of(other(2), other(9))));
            assertThat(result.size()).as(kind.name()).isOne();
            assertThat(storedUnder(result, B)).as(kind.name()).isSameAs(B);
        }
    }

    @Test
    void retainAllKeepsTheReceiversObjectForAnEquatedElementOfEveryKind() {
        for (Kind kind : allKinds()) {
            TreeSet<Item> result = receiver().retainAll(kind.of().apply(java.util.List.of(other(2), other(9))));
            assertThat(result.size()).as(kind.name()).isOne();
            assertThat(storedUnder(result, B)).as(kind.name()).isSameAs(B);
        }
    }

    @Test
    void intersectAndRetainAllOfElementsTheComparatorDoesNotEquateAreEmpty() {
        for (Kind kind : allKinds()) {
            java.util.List<Item> items = java.util.List.of(other(0), other(9));
            if (SETS.contains(kind)) {
                assertThat(receiver().intersect(set(kind, items)))
                        .as(kind.name())
                        .isEmpty();
            }
            assertThat(receiver().retainAll(kind.of().apply(items)))
                    .as(kind.name())
                    .isEmpty();
        }
    }

    @Test
    void intersectAndRetainAllOfAnElementEquatedWithEachAreTheReceiver() {
        for (Kind kind : allKinds()) {
            java.util.List<Item> items = java.util.List.of(other(3), other(1), other(2), other(9));
            TreeSet<Item> set = receiver();
            if (SETS.contains(kind)) {
                assertThat(set.intersect(set(kind, items))).as(kind.name()).isSameAs(set);
            }
            assertThat(set.retainAll(kind.of().apply(items))).as(kind.name()).isSameAs(set);
        }
    }

    @Test
    void retainAllCountsAReceiverElementOnceWhenSeveralGivenElementsAreEquatedWithIt() {
        // two given items equated with A, none with B: B goes, and A is not counted twice
        java.util.List<Item> items = java.util.List.of(other(1), new Item(1, "third"));
        TreeSet<Item> set = TreeSet.of(BY_KEY, A, B);
        for (Kind kind : java.util.List.of(SETS.get(2), SETS.get(3), ITERABLES.get(0), ITERABLES.get(1))) {
            TreeSet<Item> result = set.retainAll(kind.of().apply(items));
            assertThat(result.size()).as(kind.name()).isOne();
            assertThat(storedUnder(result, A)).as(kind.name()).isSameAs(A);
            assertThat(set.intersect(HashSet.ofAll(items))).as(kind.name()).hasSize(1);
        }
        TreeSet<Item> single = TreeSet.of(BY_KEY, A);
        assertThat(single.retainAll(items)).isSameAs(single);
    }

    @Test
    void intersectAndRetainAllOfAnEmptyArgumentAreEmpty() {
        for (Kind kind : allKinds()) {
            if (SETS.contains(kind)) {
                assertThat(receiver().intersect(set(kind, java.util.List.of())))
                        .as(kind.name())
                        .isEmpty();
            }
            assertThat(receiver().retainAll(kind.of().apply(java.util.List.of())))
                    .as(kind.name())
                    .isEmpty();
        }
    }

    // -- diff and removeAll: an element goes when the argument holds one the comparator equates with it

    @Test
    void diffRemovesAnEquatedElementOfEveryKind() {
        for (Kind kind : SETS) {
            TreeSet<Item> result = receiver().diff(set(kind, java.util.List.of(other(2), other(9))));
            assertThat(result).as(kind.name()).containsExactly(A, C);
            assertThat(storedUnder(result, A)).as(kind.name()).isSameAs(A);
            assertThat(storedUnder(result, C)).as(kind.name()).isSameAs(C);
        }
    }

    @Test
    void removeAllRemovesAnEquatedElementOfEveryKind() {
        for (Kind kind : allKinds()) {
            TreeSet<Item> result = receiver().removeAll(kind.of().apply(java.util.List.of(other(2), other(9))));
            assertThat(result).as(kind.name()).containsExactly(A, C);
            assertThat(storedUnder(result, A)).as(kind.name()).isSameAs(A);
            assertThat(storedUnder(result, C)).as(kind.name()).isSameAs(C);
        }
    }

    @Test
    void diffAndRemoveAllOfElementsTheComparatorDoesNotEquateAreTheReceiver() {
        for (Kind kind : allKinds()) {
            java.util.List<Item> items = java.util.List.of(other(0), other(9));
            TreeSet<Item> set = receiver();
            if (SETS.contains(kind)) {
                assertThat(set.diff(set(kind, items))).as(kind.name()).isSameAs(set);
            }
            assertThat(set.removeAll(kind.of().apply(items))).as(kind.name()).isSameAs(set);
        }
    }

    @Test
    void diffAndRemoveAllOfAnElementEquatedWithEachAreEmpty() {
        for (Kind kind : allKinds()) {
            java.util.List<Item> items = java.util.List.of(other(3), other(1), other(2));
            if (SETS.contains(kind)) {
                assertThat(receiver().diff(set(kind, items))).as(kind.name()).isEmpty();
            }
            assertThat(receiver().removeAll(kind.of().apply(items)))
                    .as(kind.name())
                    .isEmpty();
        }
    }

    @Test
    void diffAndRemoveAllOfAnEmptyArgumentAreTheReceiver() {
        for (Kind kind : allKinds()) {
            TreeSet<Item> set = receiver();
            if (SETS.contains(kind)) {
                assertThat(set.diff(set(kind, java.util.List.of())))
                        .as(kind.name())
                        .isSameAs(set);
            }
            assertThat(set.removeAll(kind.of().apply(java.util.List.of())))
                    .as(kind.name())
                    .isSameAs(set);
        }
    }

    // -- diff and removeAll delete the elements found one by one while they are few, otherwise remove them in one
    // walk

    // the items of keys 0 to 7, and the receiver of them
    private static java.util.List<Item> eight() {
        java.util.List<Item> own = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            own.add(new Item(i, "own"));
        }
        return own;
    }

    @Test
    void removeAllAndDiffOfOneTwoThreeAndManyFoundElements() {
        java.util.List<Item> own = eight();
        TreeSet<Item> set = TreeSet.ofAll(BY_KEY, own);
        // the keys to remove, with absent ones and repeats among them: one, two, three and seven found
        int[][] cases = {{9, 5}, {9, 5, 2}, {5, 9, 2, 7}, {1, 9, 3, 1, 5, 7, 6, 0, 2, 10, 3}};
        for (int[] keys : cases) {
            java.util.List<Item> given = new ArrayList<>();
            java.util.Set<Integer> removedKeys = new java.util.HashSet<>();
            for (int key : keys) {
                given.add(new Item(key, "given" + given.size()));
                removedKeys.add(key);
            }
            String at = java.util.Arrays.toString(keys);
            for (Kind kind : allKinds()) {
                java.util.List<TreeSet<Item>> results = new ArrayList<>();
                results.add(set.removeAll(kind.of().apply(given)));
                if (SETS.contains(kind)) {
                    results.add(set.diff(set(kind, given)));
                }
                for (TreeSet<Item> result : results) {
                    java.util.List<Item> expected = new ArrayList<>();
                    for (Item item : own) {
                        if (!removedKeys.contains(item.key)) {
                            expected.add(item);
                        }
                    }
                    assertThat(result).as(at + ", " + kind.name()).containsExactlyElementsOf(expected);
                    for (Item item : expected) {
                        assertThat(storedUnder(result, item))
                                .as(at + ", " + kind.name())
                                .isSameAs(item);
                    }
                }
            }
        }
    }

    @Test
    void removeAllOfManyElementsOfWhichFewAreFoundIsTheReceiverOrHoldsItsObjects() {
        java.util.List<Item> own = eight();
        TreeSet<Item> set = TreeSet.ofAll(BY_KEY, own);
        java.util.List<Item> absent = java.util.List.of(other(8), other(9), other(10), other(11), other(-1));
        assertThat(set.removeAll(absent)).isSameAs(set);
        assertThat(set.removeAll(oneShot(absent))).isSameAs(set);
        // two found, deleted one by one; the others are equated with them or absent
        java.util.List<Item> twice = java.util.List.of(other(3), other(4), new Item(3, "again"), other(9), other(4));
        TreeSet<Item> result = set.removeAll(twice);
        assertThat(result).containsExactly(own.get(0), own.get(1), own.get(2), own.get(5), own.get(6), own.get(7));
        // every element removed, in one walk
        assertThat(set.removeAll(oneShot(own))).isEmpty();
        assertThat(set.diff(HashSet.ofAll(own))).isEmpty();
    }

    // -- the limit between the two: below it the elements found are deleted, past it one walk removes them

    /// A comparator by key that counts its calls. A lookup and a deletion call it, the walk does not.
    private static final class Counting implements Comparator<Item> {
        private int calls;

        @Override
        public int compare(Item left, Item right) {
            calls++;
            return Integer.compare(left.key, right.key);
        }
    }

    @SuppressWarnings("unchecked")
    private static RedBlackTree<Item> tree(TreeSet<Item> set) {
        try {
            java.lang.reflect.Field field = TreeSet.class.getDeclaredField("tree");
            field.setAccessible(true);
            return (RedBlackTree<Item>) field.get(set);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
    }

    @Test
    void theDeletionLimitGrowsWithTheSizeUpToEight() {
        assertThat(TreeSet.deletionLimit(1)).isEqualTo(2);
        assertThat(TreeSet.deletionLimit(40)).isEqualTo(2);
        assertThat(TreeSet.deletionLimit(256)).isEqualTo(3);
        assertThat(TreeSet.deletionLimit(512)).isEqualTo(6);
        assertThat(TreeSet.deletionLimit(1023)).isEqualTo(8);
        assertThat(TreeSet.deletionLimit(4096)).isEqualTo(8);
        assertThat(TreeSet.deletionLimit(100_000)).isEqualTo(8);
        assertThat(TreeSet.deletionLimit(Integer.MAX_VALUE)).isEqualTo(8);
    }

    // removeAll, and diff for a set, of `given` from a receiver of the items 0 to n - 1 under a counting comparator,
    // for every kind of argument (a TreeSet of BY_KEY is one of another comparator here): the result holds the
    // receiver's objects but those of `removedKeys`, is a valid tree, and calls the comparator past the lookups of the
    // argument's elements exactly when `deleted`
    private static void assertRemoves(
            int n, java.util.List<Item> given, java.util.Set<Integer> removedKeys, boolean deleted) {
        Counting counting = new Counting();
        java.util.List<Item> own = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            own.add(new Item(i, "own"));
        }
        TreeSet<Item> set = TreeSet.ofAll(counting, own);
        java.util.List<Item> expected = new ArrayList<>();
        for (Item item : own) {
            if (!removedKeys.contains(item.key)) {
                expected.add(item);
            }
        }
        for (Kind kind : allKinds()) {
            String at = "n = " + n + ", " + given.size() + " given, " + kind.name();
            // the lookups alone, of the elements the argument holds
            counting.calls = 0;
            for (Item item : kind.of().apply(given)) {
                assertThat(set.contains(item)).as(at).isEqualTo(removedKeys.contains(item.key));
            }
            int lookups = counting.calls;
            java.util.List<TreeSet<Item>> results = new ArrayList<>();
            Iterable<Item> argument = kind.of().apply(given);
            counting.calls = 0;
            results.add(set.removeAll(argument));
            int calls = counting.calls;
            if (SETS.contains(kind)) {
                Set<Item> other = set(kind, given);
                counting.calls = 0;
                results.add(set.diff(other));
                assertThat(counting.calls).as("diff, " + at).isEqualTo(calls);
            }
            if (deleted) {
                assertThat(calls).as("deleted one by one, " + at).isGreaterThan(lookups);
            } else {
                assertThat(calls).as("removed in one walk, " + at).isEqualTo(lookups);
            }
            for (TreeSet<Item> result : results) {
                assertThat(result).as(at).containsExactlyElementsOf(expected);
                for (Item item : expected) {
                    assertThat(storedUnder(result, item)).as(at).isSameAs(item);
                }
                RedBlackTreeValidity.assertValid(tree(result));
            }
        }
        // persistence
        assertThat(set).containsExactlyElementsOf(own);
    }

    @Test
    void removeAllAndDiffDeleteUpToTheLimitAndWalkPastIt() {
        // the limit is 2 at 40 and 3 at 256 (the size decides), 6 at 512 and 8 at 4096 (the cap)
        for (int n : new int[] {40, 256, 512, 4096}) {
            int limit = TreeSet.deletionLimit(n);
            for (int k : new int[] {1, limit, limit + 1, 2 * limit + 3}) {
                java.util.List<Item> given = new ArrayList<>();
                java.util.Set<Integer> removedKeys = new java.util.HashSet<>();
                given.add(other(n + 5));
                for (int i = 0; i < k; i++) {
                    int key = (i * (n / k) + n / (2 * k)) % n;
                    given.add(new Item(key, "given" + i));
                    removedKeys.add(key);
                    given.add(other(-1 - i));
                }
                // the first one found again: counted once
                given.add(new Item(given.get(1).key, "again"));
                assertRemoves(n, given, removedKeys, k <= limit);
            }
        }
    }

    @Test
    void removeAllAndDiffCountTheDistinctObjectsFoundNotTheHits() {
        for (int n : new int[] {256, 4096}) {
            int limit = TreeSet.deletionLimit(n);
            // `limit` distinct elements, each given three times: more hits than the limit, still deleted
            java.util.List<Item> given = new ArrayList<>();
            java.util.Set<Integer> removedKeys = new java.util.HashSet<>();
            for (int round = 0; round < 3; round++) {
                for (int i = 0; i < limit; i++) {
                    given.add(new Item(7 * i + 1, "given" + round));
                    removedKeys.add(7 * i + 1);
                }
            }
            assertRemoves(n, given, removedKeys, true);
            // one element given many times
            java.util.List<Item> repeated = new ArrayList<>();
            for (int i = 0; i < 4 * limit; i++) {
                repeated.add(new Item(n / 2, "given" + i));
            }
            assertRemoves(n, repeated, java.util.Set.of(n / 2), true);
        }
    }

    // -- an empty receiver comes back without reading its argument

    @Test
    void anEmptyReceiverDoesNotReadItsArgument() {
        TreeSet<Item> empty = TreeSet.empty(BY_KEY);
        @SuppressWarnings("unchecked")
        Set<Item> unreadable = (Set<Item>) java.lang.reflect.Proxy.newProxyInstance(
                Set.class.getClassLoader(), new Class<?>[] {Set.class}, (proxy, method, arguments) -> {
                    throw new AssertionError("the argument is read: " + method.getName());
                });
        assertThat(empty.intersect(unreadable)).isSameAs(empty);
        assertThat(empty.retainAll(unreadable)).isSameAs(empty);
        assertThat(empty.diff(unreadable)).isSameAs(empty);
        assertThat(empty.removeAll(unreadable)).isSameAs(empty);
    }

    // -- larger receivers: every element of the set looked up, and the result a valid set of the receiver's objects

    @Test
    void retainAllAndRemoveAllOfEveryOtherElementOfALargeSet() {
        for (int n : new int[] {31, 32, 33, 1023, 1024, 1025}) {
            java.util.List<Item> own = new ArrayList<>();
            java.util.List<Item> everyOther = new ArrayList<>();
            for (int i = 0; i < n; i++) {
                own.add(new Item(i, "own"));
                if (i % 2 == 0) {
                    everyOther.add(other(i));
                }
            }
            TreeSet<Item> set = TreeSet.ofAll(BY_KEY, own);
            TreeSet<Item> retained = set.retainAll(everyOther);
            TreeSet<Item> removed = set.removeAll(everyOther);
            assertThat(retained.size()).as("retainAll of " + n).isEqualTo((n + 1) / 2);
            assertThat(removed.size()).as("removeAll of " + n).isEqualTo(n / 2);
            for (Item item : own) {
                TreeSet<Item> side = item.key % 2 == 0 ? retained : removed;
                assertThat(storedUnder(side, item)).as(n + ": " + item).isSameAs(item);
            }
            // persistence
            assertThat(set.size()).isEqualTo(n);
        }
    }

    // -- nulls go to the comparator, as in `contains`

    @Test
    void aNullGivenElementGoesToTheComparator() {
        java.util.List<Integer> withNull = java.util.Arrays.asList(1, null);
        assertThatNullPointerException().isThrownBy(() -> TreeSet.of(1, 2).retainAll(withNull));
        assertThatNullPointerException().isThrownBy(() -> TreeSet.of(1, 2).removeAll(withNull));
        Comparator<Integer> nullsFirst = Comparator.nullsFirst(Comparator.naturalOrder());
        assertThat(TreeSet.of(nullsFirst, 1, 2).retainAll(withNull)).containsExactly(1);
        assertThat(TreeSet.of(nullsFirst, 1, 2).removeAll(withNull)).containsExactly(2);
        // an empty set compares nothing
        assertThat(TreeSet.<Integer>empty().retainAll(withNull)).isEmpty();
        assertThat(TreeSet.<Integer>empty().removeAll(withNull)).isEmpty();
    }

    // -- the non-empty sorted set delegates

    @Test
    void nonEmptySortedSetDecidesMembershipWithItsComparator() {
        NonEmptySortedSet<Item> set = NonEmptySortedSet.fromIterable(BY_KEY, A, java.util.List.of(B, C));
        java.util.List<Item> items = java.util.List.of(other(2), other(9));
        TreeSet<Item> retained = set.retainAll(items);
        TreeSet<Item> intersection = set.intersect(HashSet.ofAll(items));
        assertThat(retained.size()).isOne();
        assertThat(storedUnder(retained, B)).isSameAs(B);
        assertThat(intersection.size()).isOne();
        assertThat(storedUnder(intersection, B)).isSameAs(B);
        assertThat(set.removeAll(items)).containsExactly(A, C);
        assertThat(set.diff(HashSet.ofAll(items))).containsExactly(A, C);
        java.util.List<Item> all = java.util.List.of(other(1), other(2), other(3));
        assertThat(set.retainAll(all)).isSameAs(set.toSortedSet());
    }
}
