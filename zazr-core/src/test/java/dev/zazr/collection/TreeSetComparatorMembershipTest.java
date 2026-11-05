package dev.zazr.collection;

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
