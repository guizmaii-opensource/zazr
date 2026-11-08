package dev.zazr.collection;

import dev.zazr.collection.internal.RedBlackTree;
import dev.zazr.collection.internal.RedBlackTreeValidity;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Objects;
import java.util.function.Function;
import org.jspecify.annotations.Nullable;
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
            new Kind("a Vector", Vector::ofAll),
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

    // -- the switch points: the first `limit` elements found are deleted one by one, the next `3 * limit` are kept
    // without a lookup and deleted when the argument ends there, and past them the elements found are marked and
    // dropped in one filter of the tree

    /// A comparator by key that counts its calls. A lookup, a mark and a deletion call it; the filter does not.
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
    void theDeletionLimitIsTheSizeOverTwiceItsBitLength() {
        assertThat(TreeSet.deletionLimit(1)).isEqualTo(1);
        assertThat(TreeSet.deletionLimit(2)).isEqualTo(1);
        assertThat(TreeSet.deletionLimit(3)).isEqualTo(1);
        assertThat(TreeSet.deletionLimit(40)).isEqualTo(3);
        assertThat(TreeSet.deletionLimit(256)).isEqualTo(14);
        assertThat(TreeSet.deletionLimit(1000)).isEqualTo(50);
        assertThat(TreeSet.deletionLimit(4096)).isEqualTo(157);
        assertThat(TreeSet.deletionLimit(5000)).isEqualTo(192);
        assertThat(TreeSet.deletionLimit(100_000)).isEqualTo(2941);
        assertThat(TreeSet.deletionLimit(Integer.MAX_VALUE)).isEqualTo(34_636_833);
    }

    /// An argument whose iterator records, when it is asked for each element and when it first says it has no more,
    /// how many comparator calls were made so far. `sized` gives the argument with its size known in advance (a
    /// `java.util.Collection`), `oneShot` without, and its iterator can be asked for once only.
    private static final class Recording {
        private final Counting counting;
        private final java.util.List<Item> items;
        private final int[] callsAtRead;
        private int callsAtEnd = -1;
        private int reads;
        private int iterators;

        Recording(Counting counting, java.util.List<Item> items) {
            this.counting = counting;
            this.items = items;
            this.callsAtRead = new int[items.size()];
        }

        java.util.Iterator<Item> iterator() {
            assertThat(iterators++).as("iterated once").isZero();
            return new java.util.Iterator<>() {
                @Override
                public boolean hasNext() {
                    boolean hasNext = reads < items.size();
                    if (!hasNext && callsAtEnd < 0) {
                        callsAtEnd = counting.calls;
                    }
                    return hasNext;
                }

                @Override
                public Item next() {
                    callsAtRead[reads] = counting.calls;
                    return items.get(reads++);
                }
            };
        }

        Iterable<Item> oneShot() {
            return this::iterator;
        }

        java.util.Collection<Item> sized() {
            return new java.util.AbstractCollection<>() {
                @Override
                public java.util.Iterator<Item> iterator() {
                    return Recording.this.iterator();
                }

                @Override
                public int size() {
                    return items.size();
                }
            };
        }

        // the elements read with no comparator call before the next one is asked for, or the end: those kept
        int kept() {
            return (int) java.util.stream.IntStream.range(0, reads)
                    .filter(i -> (i + 1 < reads ? callsAtRead[i + 1] : callsAtEnd) == callsAtRead[i])
                    .count();
        }
    }

    /// How a removal ran: `kept` elements read without a comparator call before the next read, and `tail` comparator
    /// calls after the end of the argument (deletions, which the filter does not make).
    private record Run(int kept, int tail) {}

    // the items 0 to n - 1
    private static java.util.List<Item> own(int n) {
        java.util.List<Item> own = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            own.add(new Item(i, "own"));
        }
        return own;
    }

    // `result` holds the very objects of `expected`, in order, and is a valid tree
    private static void assertSameObjects(String at, TreeSet<Item> result, TreeSet<Item> expected) {
        assertThat(result.size()).as(at).isEqualTo(expected.size());
        java.util.Iterator<Item> expectedItems = expected.iterator();
        for (Item item : result) {
            assertThat(item).as(at).isSameAs(expectedItems.next());
        }
        RedBlackTreeValidity.assertValid(tree(result));
    }

    // removeAll of `given` from the items 0 to n - 1 under a counting comparator, given `sized` or one-shot: the
    // result is the receiver's objects that folding `remove` over `given` keeps (the receiver itself when that removes
    // nothing), the argument is read once, the receiver is unchanged, and how the removal ran is returned
    private static Run removal(int n, java.util.List<Item> given, boolean sized) {
        Counting counting = new Counting();
        java.util.List<Item> own = own(n);
        TreeSet<Item> set = TreeSet.ofAll(counting, own);
        TreeSet<Item> expected = Vector.ofAll(given).foldLeft(set, TreeSet::remove);
        Recording recording = new Recording(counting, given);
        counting.calls = 0;
        TreeSet<Item> result = set.removeAll(sized ? recording.sized() : recording.oneShot());
        int calls = counting.calls;
        String at = "n = " + n + ", " + given.size() + " given, " + (sized ? "sized" : "one-shot");
        assertSameObjects(at, result, expected);
        if (expected.size() == n) {
            assertThat(result).as(at).isSameAs(set);
        }
        assertThat(recording.reads).as(at).isEqualTo(given.size());
        assertThat(recording.callsAtEnd).as(at).isNotNegative();
        assertThat(set).containsExactlyElementsOf(own);
        return new Run(recording.kept(), calls - recording.callsAtEnd);
    }

    // removeAll, and diff for a set, of `given` from the items 0 to n - 1, for every kind of argument (a TreeSet of
    // BY_KEY is one of another comparator here): the result is the receiver's objects that folding `remove` keeps
    private static void assertRemovesForEveryKind(int n, java.util.List<Item> given) {
        TreeSet<Item> set = TreeSet.ofAll(new Counting(), own(n));
        TreeSet<Item> expected = Vector.ofAll(given).foldLeft(set, TreeSet::remove);
        for (Kind kind : allKinds()) {
            String at = "n = " + n + ", " + given.size() + " given, " + kind.name();
            assertSameObjects("removeAll, " + at, set.removeAll(kind.of().apply(given)), expected);
            if (SETS.contains(kind)) {
                assertSameObjects("diff, " + at, set.diff(set(kind, given)), expected);
            }
        }
        // a NonEmptyVector, which is no Traversable and so is not among the kinds made of no item
        if (!given.isEmpty()) {
            NonEmptyVector<Item> vector = NonEmptyVector.fromIterable(given.get(0), given.subList(1, given.size()));
            String at = "n = " + n + ", " + given.size() + " given, a NonEmptyVector";
            assertSameObjects("removeAll, " + at, set.removeAll(vector), expected);
        }
    }

    // -- a NonEmptyVector argument, whose size is known in advance

    @Test
    void removeAllAndAddAllOfANonEmptyVector() {
        for (int n = 0; n <= 6; n++) {
            TreeSet<Integer> set = n == 0 ? TreeSet.empty() : TreeSet.rangeClosed(1, n);
            for (NonEmptyVector<Integer> vector : java.util.List.of(
                    NonEmptyVector.of(1), NonEmptyVector.of(1, 2), NonEmptyVector.of(2, 9, 1, 2, 4, 3))) {
                String at = "n = " + n + ", " + vector;
                TreeSet<Integer> removed = vector.toVector().foldLeft(set, TreeSet::remove);
                TreeSet<Integer> added = vector.toVector().foldLeft(set, TreeSet::add);
                assertThat(set.removeAll(vector)).as("removeAll, " + at).containsExactlyElementsOf(removed);
                assertThat(set.addAll(vector)).as("addAll, " + at).containsExactlyElementsOf(added);
            }
        }
    }

    // the items of the first k keys of `keys`, tagged so that they equal no item of the receiver
    private static java.util.List<Item> given(java.util.List<Integer> keys, int k) {
        java.util.List<Item> given = new ArrayList<>();
        for (int i = 0; i < k; i++) {
            given.add(new Item(keys.get(i), "given" + i));
        }
        return given;
    }

    // the keys 0 to n - 1 in an order of their own
    private static java.util.List<Integer> shuffled(int n) {
        java.util.List<Integer> keys = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            keys.add(i);
        }
        java.util.Collections.shuffle(keys, new java.util.Random(n));
        return keys;
    }

    @Test
    void removeAllAndDiffOnEachSideOfTheSwitchPoints() {
        for (int n : new int[] {1000, 5000}) {
            int limit = TreeSet.deletionLimit(n);
            int bound = 3 * limit;
            java.util.List<Integer> keys = shuffled(n);
            for (int k : new int[] {
                1,
                limit,
                limit + 1,
                limit + 3,
                limit + bound - 1,
                limit + bound,
                limit + bound + 1,
                limit + 2 * bound,
                n / 2,
                n
            }) {
                java.util.List<Item> given = given(keys, k);
                String at = "n = " + n + ", k = " + k;
                Run oneShot = removal(n, given, false);
                Run sized = removal(n, given, true);
                if (k <= limit) {
                    assertThat(oneShot).as("deleted one by one, " + at).isEqualTo(new Run(0, 0));
                    assertThat(sized).as("deleted one by one, " + at).isEqualTo(new Run(0, 0));
                } else if (k <= limit + bound) {
                    assertThat(oneShot.kept()).as("kept, " + at).isEqualTo(k - limit);
                    assertThat(oneShot.tail()).as("kept, then deleted, " + at).isPositive();
                    assertThat(sized)
                            .as("deleted one by one as the size says, " + at)
                            .isEqualTo(new Run(0, 0));
                } else {
                    assertThat(oneShot).as("filtered, " + at).isEqualTo(new Run(bound - 1, 0));
                    assertThat(sized).as("filtered, " + at).isEqualTo(new Run(bound - 1, 0));
                }
                assertRemovesForEveryKind(n, given);
            }
        }
    }

    @Test
    void removeAllAndDiffPastTheKeptElementsWithFewFoundDeleteThem() {
        for (int n : new int[] {1000, 5000}) {
            int limit = TreeSet.deletionLimit(n);
            int bound = 3 * limit;
            java.util.List<Integer> keys = shuffled(n);
            // `limit` found, then absent elements and repeats past the kept ones
            java.util.List<Item> start = given(keys, limit);
            for (int i = 0; i <= bound; i++) {
                start.add(i % 2 == 0 ? other(n + i) : new Item(keys.get(i % limit), "again" + i));
            }
            // then nothing more found: the elements found first are the only ones removed
            for (boolean sized : new boolean[] {false, true}) {
                assertThat(removal(n, start, sized)).as("n = " + n + ", none").isEqualTo(new Run(bound - 1, 0));
            }
            assertRemovesForEveryKind(n, start);
            // then a few more found, each twice, and absent elements: those few are deleted one by one
            for (int few : new int[] {1, 3, limit - 1}) {
                java.util.List<Item> given = new ArrayList<>(start);
                for (int i = 0; i < few; i++) {
                    given.add(new Item(keys.get(limit + i), "late" + i));
                    given.add(other(-1 - i));
                    given.add(new Item(keys.get(limit + i), "twice" + i));
                }
                String at = "n = " + n + ", " + few + " more";
                for (boolean sized : new boolean[] {false, true}) {
                    Run run = removal(n, given, sized);
                    assertThat(run.kept()).as(at).isEqualTo(bound - 1);
                    assertThat(run.tail()).as("deleted after the end, " + at).isPositive();
                }
                assertRemovesForEveryKind(n, given);
            }
            // `limit` more found: one filter
            java.util.List<Item> many = new ArrayList<>(start);
            many.addAll(given(keys.subList(limit, n), limit));
            for (boolean sized : new boolean[] {false, true}) {
                assertThat(removal(n, many, sized))
                        .as("n = " + n + ", limit more")
                        .isEqualTo(new Run(bound - 1, 0));
            }
            assertRemovesForEveryKind(n, many);
        }
    }

    @Test
    void removeAllAndDiffOfSmallReceivers() {
        java.util.Random random = new java.util.Random(3);
        for (int n = 0; n <= 3; n++) {
            for (int length = 0; length <= 10; length++) {
                for (int round = 0; round < 20; round++) {
                    // keys from -1 to n: absent ones at both ends, repeats among them
                    java.util.List<Item> given = new ArrayList<>();
                    for (int i = 0; i < length; i++) {
                        given.add(new Item(random.nextInt(n + 2) - 1, "given" + i));
                    }
                    if (n > 0) {
                        removal(n, given, false);
                        removal(n, given, true);
                    }
                    assertRemovesForEveryKind(n, given);
                }
            }
            // every element, then repeats and absent ones
            java.util.List<Item> all = given(shuffled(n), n);
            all.add(other(n));
            all.addAll(given(shuffled(n), n));
            all.add(other(-1));
            assertRemovesForEveryKind(n, all);
        }
    }

    @Test
    void aNullGivenElementGoesToTheComparatorAfterEveryElementIsRemoved() {
        Comparator<Integer> nullsFirst = Comparator.nullsFirst(Comparator.naturalOrder());
        for (int n : new int[] {1, 2, 3, 4, 1000}) {
            java.util.List<Integer> keys = new ArrayList<>();
            for (int i = 0; i < n; i++) {
                keys.add(i);
            }
            TreeSet<Integer> set = TreeSet.ofAll(keys);
            TreeSet<Integer> nullable = TreeSet.ofAll(nullsFirst, keys);
            // the null right after the last element, or after repeats of it
            for (int repeats : new int[] {0, 1, 2, 3, 4 * n}) {
                java.util.List<@Nullable Integer> given = new ArrayList<>(keys);
                for (int i = 0; i < repeats; i++) {
                    given.add(n - 1);
                }
                given.add(null);
                String at = "n = " + n + ", " + repeats + " repeats";
                // a list, whose size is known in advance, and a one-shot Iterable, whose size is not
                java.util.List<Iterable<Integer>> arguments = java.util.List.of(given, () -> given.iterator());
                for (Iterable<Integer> argument : arguments) {
                    assertThatNullPointerException().as(at).isThrownBy(() -> set.removeAll(argument));
                    assertThat(nullable.removeAll(argument)).as(at).isEmpty();
                }
            }
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
