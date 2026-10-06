package dev.zazr.collection;

import java.util.ArrayList;
import java.util.Random;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

/// `HashSet.removeAll` of any iterable but a HashSet, which removes the elements one by one, compared with its
/// definition ([ArrangementModels#removeAll]): the same elements in the same iteration order, the receiver returned
/// when nothing is removed, the receiver unchanged, and a null element rejected wherever it is.
class HashSetRemoveAllTest {

    private static final long SEED = 20261006L;

    /// An element of a chosen hash code: two of one hash and different ids collide.
    record Key(int hash, int id) {
        @Override
        public int hashCode() {
            return hash;
        }
    }

    private static Key randomKey(Random random, int range) {
        int hash = (random.nextInt(8) == 0) ? random.nextInt() : random.nextInt(range);
        return new Key(hash, random.nextInt(2));
    }

    // the forms an argument can take: a java.util.List, a one-shot iterable, a Vector, a LinkedHashSet, a TreeSet
    private static java.util.List<Iterable<Key>> forms(java.util.List<Key> elements) {
        java.util.List<Iterable<Key>> result = new ArrayList<>();
        result.add(elements);
        result.add(oneShot(elements));
        result.add(Vector.ofAll(elements));
        result.add(LinkedHashSet.ofAll(elements));
        result.add(TreeSet.ofAll(java.util.Comparator.comparingInt(Key::hash).thenComparingInt(Key::id), elements));
        return result;
    }

    private static Iterable<Key> oneShot(java.util.List<Key> elements) {
        java.util.Iterator<Key> iterator = elements.iterator();
        boolean[] used = {false};
        return () -> {
            assertThat(used[0]).as("iterated once").isFalse();
            used[0] = true;
            return iterator;
        };
    }

    // the elements in iteration order
    private static java.util.List<Key> order(HashSet<Key> set) {
        java.util.List<Key> result = new ArrayList<>();
        for (Key key : set) {
            result.add(key);
        }
        return result;
    }

    private static void assertSameSet(HashSet<Key> expected, HashSet<Key> actual) {
        assertThat(actual).isEqualTo(expected);
        assertThat(order(actual)).containsExactlyElementsOf(order(expected));
        assertThat(actual.hashCode()).isEqualTo(expected.hashCode());
    }

    @Test
    void removeAllIsThatOfTheDefinition() {
        Random random = new Random(SEED);
        int[] sizes = {0, 1, 2, 3, 31, 32, 33, 100, 1023, 1024, 1025, 3000};
        for (int size : sizes) {
            for (int round = 0; round < 6; round++) {
                int range = (round % 2 == 0) ? 4 * size + 1 : size / 2 + 1;
                java.util.List<Key> keys = new ArrayList<>();
                for (int i = 0; i < size; i++) {
                    keys.add(randomKey(random, range));
                }
                HashSet<Key> set = HashSet.ofAll(keys);
                java.util.List<Key> before = order(set);
                int count = switch (round) {
                    case 0 -> 0;
                    case 1 -> 1;
                    case 2 -> size;
                    case 3 -> 3 * size + 2;
                    default -> random.nextInt(size + 3);
                };
                java.util.List<Key> removals = new ArrayList<>();
                for (int i = 0; i < count; i++) {
                    removals.add(
                            (size > 0 && random.nextBoolean())
                                    ? keys.get(random.nextInt(size))
                                    : randomKey(random, range));
                }
                HashSet<Key> expected = ArrangementModels.removeAll(set, removals);
                for (Iterable<Key> form : forms(removals)) {
                    HashSet<Key> actual = set.removeAll(form);
                    assertSameSet(expected, actual);
                    if (expected == set) {
                        assertThat(actual).isSameAs(set);
                    }
                }
                assertSameSet(expected, (HashSet<Key>) set.diff(LinkedHashSet.ofAll(removals)));
                assertThat(order(set)).as("the receiver is unchanged").containsExactlyElementsOf(before);
            }
        }
    }

    @Test
    void removingEverythingGivesTheEmptySet() {
        HashSet<Integer> set = HashSet.range(0, 2000);
        HashSet<Integer> none = set.removeAll(Vector.range(0, 2000).reverse());
        assertThat(none).isEmpty();
        assertThat(none).isSameAs(HashSet.empty());
        assertThat(set).hasSize(2000);
        // an element missing from the set before the last one: the rest is still read
        assertThat(HashSet.of(1).removeAll(java.util.List.of(1, 2, 3))).isSameAs(HashSet.empty());
    }

    @Test
    void removingNothingReturnsTheReceiver() {
        HashSet<Integer> set = HashSet.range(0, 100);
        assertThat(set.removeAll(java.util.List.of())).isSameAs(set);
        assertThat(set.removeAll(java.util.List.of(100, 200, -1))).isSameAs(set);
        HashSet<Integer> empty = HashSet.empty();
        assertThat(empty.removeAll(java.util.List.of(1))).isSameAs(empty);
    }

    @Test
    void aNullElementIsRejectedWherever() {
        HashSet<Integer> set = HashSet.of(1, 2);
        java.util.List<Integer> lateNull = new ArrayList<>(java.util.List.of(1, 2, 3));
        lateNull.add(null);
        assertThatNullPointerException()
                .isThrownBy(() -> set.removeAll(lateNull))
                .withMessage("HashSet: element is null");
        assertThatNullPointerException()
                .isThrownBy(() -> ArrangementModels.removeAll(set, lateNull))
                .withMessage("HashSet: element is null");
        java.util.List<Integer> firstNull = new ArrayList<>();
        firstNull.add(null);
        assertThatNullPointerException()
                .isThrownBy(() -> set.removeAll(firstNull))
                .withMessage("HashSet: element is null");
        assertThat(set).containsExactlyInAnyOrder(1, 2);
        // an empty set reads nothing
        assertThat(HashSet.<Integer>empty().removeAll(lateNull)).isEmpty();
    }

    @Test
    void theOtherSetsDelegate() {
        NonEmptySet<Integer> set = NonEmptySet.of(1, 2, 3);
        assertThat(set.removeAll(java.util.List.of(2, 4))).containsExactlyInAnyOrder(1, 3);
        assertThat(set.diff(TreeSet.of(1, 3))).containsExactly(2);
    }
}
