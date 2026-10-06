package dev.zazr.collection.internal;

import dev.zazr.collection.Vector;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Objects;
import java.util.Random;
import org.junit.jupiter.api.Test;

import static dev.zazr.collection.internal.ChampValidity.assertSameShape;
import static dev.zazr.collection.internal.ChampValidity.assertValid;
import static org.assertj.core.api.Assertions.assertThat;

/// The removals of `HashSet.removeAll`, which update the root of the trie in place: after each one, the trie is the
/// one the persistent removals of the same elements give, node for node, and the trie it started from is unchanged.
public class ChampRemoveInPlaceTest {

    private static final long SEED = 20261006L;

    /// A key whose hash code is `hash`, mixed: two keys of one hash and different ids collide.
    record Key(int hash, int id) {
        @Override
        public int hashCode() {
            return ChampValidity.hashCodeFor(hash);
        }
    }

    private static BitmapIndexedSetNode<Key> persistent(java.util.List<Key> elements) {
        return Vector.ofAll(elements).foldLeft(SetNode.<Key>empty(), (trie, element) -> trie.updated(element, false));
    }

    // removes `removals` one by one, in place, checking each result against the persistent removal; returns the trie
    private static BitmapIndexedSetNode<Key> removeChecked(
            BitmapIndexedSetNode<Key> trie, java.util.List<Key> removals) {
        Object owner = new Object();
        @SuppressWarnings("Var")
        BitmapIndexedSetNode<Key> expected = trie;
        @SuppressWarnings("Var")
        BitmapIndexedSetNode<Key> actual = trie;
        for (Key key : removals) {
            BitmapIndexedSetNode<Key> before = actual;
            expected = expected.removed(key);
            actual = actual.removeInPlace(owner, key, Objects.hashCode(key));
            if (expected.size() == before.size()) {
                assertThat(actual)
                        .as("an absent element leaves the root as it is")
                        .isSameAs(before);
            }
            assertValid(actual);
            assertSameShape(expected, actual, true);
        }
        return actual;
    }

    @Test
    public void removesAtTheNodeBoundaries() {
        for (int size : new int[] {1, 2, 3, 31, 32, 33, 1023, 1024, 1025, 2000}) {
            java.util.List<Key> keys = new ArrayList<>();
            for (int i = 0; i < size; i++) {
                keys.add(new Key(i, 0));
            }
            BitmapIndexedSetNode<Key> trie = persistent(keys);
            BitmapIndexedSetNode<Key> copy = persistent(keys);
            java.util.List<Key> removals = new ArrayList<>(keys);
            Collections.shuffle(removals, new Random(SEED + size));
            BitmapIndexedSetNode<Key> emptied = removeChecked(trie, removals);
            assertThat(emptied.size()).isZero();
            assertSameShape(copy, trie, true);
        }
    }

    @Test
    public void removesRandomElementsWithCollisions() {
        Random random = new Random(SEED);
        for (int round = 0; round < 400; round++) {
            int size = random.nextInt(4) == 0 ? random.nextInt(2000) : random.nextInt(70);
            java.util.List<Key> keys = new ArrayList<>();
            for (int i = 0; i < size; i++) {
                keys.add(randomKey(random));
            }
            BitmapIndexedSetNode<Key> trie = persistent(keys);
            BitmapIndexedSetNode<Key> copy = persistent(keys);
            java.util.List<Key> removals = new ArrayList<>();
            int count = random.nextInt(size + 5);
            for (int i = 0; i < count; i++) {
                // present, absent, or a second time
                removals.add((size > 0 && random.nextBoolean()) ? keys.get(random.nextInt(size)) : randomKey(random));
            }
            removeChecked(trie, removals);
            assertSameShape(copy, trie, true);
        }
    }

    @Test
    public void promotesTheOnlyChildAndCopiesItOnTheNextRemoval() {
        // two keys under one slot of the root: the root holds a single child
        Key a = new Key(1, 0);
        Key b = new Key(1 | (1 << 5), 0);
        BitmapIndexedSetNode<Key> trie = persistent(java.util.List.of(a, b));
        assertThat(trie.nodeArity()).isEqualTo(1);
        Object owner = new Object();
        BitmapIndexedSetNode<Key> one = trie.removeInPlace(owner, a, Objects.hashCode(a));
        assertSameShape(trie.removed(a), one, true);
        assertThat(one.owner)
                .as("the promoted node is the persistent removal's own")
                .isNull();
        BitmapIndexedSetNode<Key> none = one.removeInPlace(owner, b, Objects.hashCode(b));
        assertThat(none).isNotSameAs(one);
        assertThat(none.size()).isZero();
        assertThat(one.size()).as("the promoted node is not updated in place").isEqualTo(1);
    }

    @Test
    public void updatesInPlaceOnlyTheRootItOwns() {
        java.util.List<Key> keys = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            keys.add(new Key(i, 0));
        }
        BitmapIndexedSetNode<Key> trie = persistent(keys);
        Object owner = new Object();
        BitmapIndexedSetNode<Key> first = trie.removeInPlace(owner, keys.get(0), Objects.hashCode(keys.get(0)));
        assertThat(first).isNotSameAs(trie);
        assertThat(first.owner).isSameAs(owner);
        BitmapIndexedSetNode<Key> second = first.removeInPlace(owner, keys.get(1), Objects.hashCode(keys.get(1)));
        assertThat(second).as("updated in place").isSameAs(first);
        BitmapIndexedSetNode<Key> other =
                second.removeInPlace(new Object(), keys.get(2), Objects.hashCode(keys.get(2)));
        assertThat(other).as("another owner copies").isNotSameAs(second);
        assertThat(second.size()).isEqualTo(98);
        assertThat(other.size()).isEqualTo(97);
    }

    private static final int[] HOT_HASHES = {
        0, 1, 2, 31, 32, 33, 1023, 1024, 1025, -1, Integer.MIN_VALUE, Integer.MAX_VALUE, 1 << 30, 1 << 25, 1 << 20
    };

    private static Key randomKey(Random random) {
        int hash = switch (random.nextInt(4)) {
            case 0 -> HOT_HASHES[random.nextInt(HOT_HASHES.length)];
            case 1 -> random.nextInt(64);
            case 2 -> random.nextInt(64) << (5 * (1 + random.nextInt(5)));
            default -> random.nextInt();
        };
        return new Key(hash, random.nextInt(3));
    }
}
