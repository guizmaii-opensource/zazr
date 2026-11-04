package dev.zazr.collection.internal;

import java.util.ArrayList;
import java.util.NoSuchElementException;
import java.util.Random;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/// The insertion order of LinkedHashMap against a model: the keys present, in order, each with its ordinal. Every
/// operation is checked on its result, and every result is checked again at the end, so that older versions are seen
/// to stay as they were.
public class InsertionOrderTest {

    /* a version of the order and the model of it: keys.get(i) has the ordinal ordinals.get(i) */
    private record Version(
            InsertionOrder<Integer> order, java.util.List<Integer> keys, java.util.List<Integer> ordinals) {}

    private static Version emptyAt(int ordinal) {
        return new Version(InsertionOrder.startingAt(ordinal), new ArrayList<>(), new ArrayList<>());
    }

    private static Version append(Version version, int key) {
        int ordinal = version.order().nextOrdinal();
        java.util.List<Integer> keys = new ArrayList<>(version.keys());
        java.util.List<Integer> ordinals = new ArrayList<>(version.ordinals());
        keys.add(key);
        ordinals.add(ordinal);
        InsertionOrder<Integer> appended = version.order().append(key);
        assertThat(appended.nextOrdinal()).isEqualTo(ordinal + 1);
        return new Version(appended, keys, ordinals);
    }

    private static Version removeAt(Version version, int rank) {
        java.util.List<Integer> keys = new ArrayList<>(version.keys());
        java.util.List<Integer> ordinals = new ArrayList<>(version.ordinals());
        keys.remove(rank);
        int ordinal = ordinals.remove(rank);
        InsertionOrder<Integer> removed = version.order().remove(ordinal);
        assertThat(removed.nextOrdinal()).isEqualTo(version.order().nextOrdinal());
        return new Version(removed, keys, ordinals);
    }

    private static Version updateAt(Version version, int rank, int key) {
        java.util.List<Integer> keys = new ArrayList<>(version.keys());
        keys.set(rank, key);
        return new Version(version.order().update(version.ordinals().get(rank), key), keys, version.ordinals());
    }

    private static Version slice(Version version, int from, int until) {
        InsertionOrder<Integer> sliced = version.order().slice(from, until);
        assertThat(sliced.nextOrdinal()).isEqualTo(version.order().nextOrdinal());
        return new Version(
                sliced,
                new ArrayList<>(version.keys().subList(from, until)),
                new ArrayList<>(version.ordinals().subList(from, until)));
    }

    private static void check(Version version, Random random) {
        InsertionOrder<Integer> order = version.order();
        java.util.List<Integer> keys = version.keys();
        int size = keys.size();
        assertThat(order.size()).isEqualTo(size);
        assertThat(order.isEmpty()).isEqualTo(size == 0);
        assertThat(order.isFull()).isEqualTo(order.nextOrdinal() == InsertionOrder.LIMIT);
        assertThat(read(order.iterator())).isEqualTo(keys);
        java.util.List<Integer> reversed = new ArrayList<>(keys);
        java.util.Collections.reverse(reversed);
        assertThat(read(order.reverseIterator())).isEqualTo(reversed);
        int from = random.nextInt(size + 1);
        assertThat(read(order.iterator(from))).isEqualTo(keys.subList(from, size));
        assertThat(read(order.iterator(size))).isEmpty();
        if (size > 0) {
            assertThat(order.first()).isEqualTo(keys.get(0));
            assertThat(order.last()).isEqualTo(keys.get(size - 1));
            assertThat(order.slice(0, size)).isSameAs(order);
        }
    }

    private static java.util.List<Integer> read(java.util.Iterator<Integer> iterator) {
        java.util.List<Integer> keys = new ArrayList<>();
        while (iterator.hasNext()) {
            keys.add(iterator.next());
        }
        assertThatThrownBy(iterator::next).isInstanceOf(NoSuchElementException.class);
        return keys;
    }

    // `steps` random operations from `start`, each on a random older version, appends weighted by `appendWeight`
    private static void randomWalk(Version start, int steps, int appendWeight, long seed) {
        Random random = new Random(seed);
        java.util.List<Version> versions = new ArrayList<>();
        versions.add(start);
        for (int step = 0; step < steps; step++) {
            // mostly the latest version, sometimes an older one
            Version version = random.nextInt(4) == 0
                    ? versions.get(random.nextInt(versions.size()))
                    : versions.get(versions.size() - 1);
            int size = version.keys().size();
            int operation = size == 0 ? 0 : random.nextInt(appendWeight + 4);
            Version next;
            if (version.order().isFull()) {
                next = size == 0 ? version : removeAt(version, random.nextInt(size));
            } else if (operation < appendWeight || size == 0) {
                next = append(version, step);
            } else if (operation == appendWeight) {
                next = removeAt(version, random.nextInt(size));
            } else if (operation == appendWeight + 1) {
                next = updateAt(version, random.nextInt(size), -step);
            } else if (operation == appendWeight + 2) {
                // a run of removals at a random place, which empties leaves and branches
                @SuppressWarnings("Var")
                Version removed = version;
                int rank = random.nextInt(size);
                int count = Math.min(1 + random.nextInt(80), size - rank);
                for (int i = 0; i < count; i++) {
                    removed = removeAt(removed, rank);
                }
                next = removed;
            } else {
                int from = random.nextInt(size);
                int until = from + 1 + random.nextInt(size - from);
                next = slice(version, from, until);
            }
            check(next, random);
            versions.add(next);
        }
        Random again = new Random(seed + 1);
        for (Version version : versions) {
            check(version, again);
        }
    }

    @Test
    public void randomOperationsOnASmallOrder() {
        randomWalk(emptyAt(0), 3000, 2, 1);
    }

    @Test
    public void randomOperationsOnAnOrderOfThreeLevels() {
        @SuppressWarnings("Var")
        Version version = emptyAt(0);
        for (int key = 0; key < 3000; key++) {
            version = append(version, key);
        }
        check(version, new Random(2));
        randomWalk(version, 600, 3, 2);
    }

    @Test
    public void randomOperationsFromAFarOrdinal() {
        // the first key starts a path of four levels at once
        randomWalk(emptyAt(40_000), 1500, 3, 3);
    }

    @Test
    public void appendsUpToTheLastOrdinal() {
        @SuppressWarnings("Var")
        Version version = emptyAt(InsertionOrder.LIMIT - 1500);
        while (!version.order().isFull()) {
            version = append(version, version.order().nextOrdinal());
        }
        assertThat(version.order().nextOrdinal()).isEqualTo(InsertionOrder.LIMIT);
        check(version, new Random(4));
        randomWalk(version, 300, 0, 4);
        assertThat(InsertionOrder.startingAt(InsertionOrder.LIMIT).isFull()).isTrue();
        assertThatThrownBy(() -> InsertionOrder.startingAt(-1)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> InsertionOrder.startingAt(Integer.MIN_VALUE))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void ofAllGivesTheOrderOfTheSameAppends() {
        int[] sizes = {0, 1, 2, 31, 32, 33, 63, 64, 65, 96, 1023, 1024, 1025, 1056, 1057, 32 * 1024 + 33};
        for (int size : sizes) {
            java.util.List<Integer> keys = new ArrayList<>();
            @SuppressWarnings("Var")
            InsertionOrder<Integer> appended = InsertionOrder.empty();
            for (int key = 0; key < size; key++) {
                keys.add(key);
                appended = appended.append(key);
            }
            InsertionOrder<Integer> built = InsertionOrder.ofAll(keys);
            assertThat(built.nextOrdinal()).isEqualTo(size).isEqualTo(appended.nextOrdinal());
            assertThat(read(built.iterator())).isEqualTo(read(appended.iterator()));
            if (size == 0) {
                assertThat(built).isSameAs(InsertionOrder.empty());
            }
            // the i-th key has the ordinal i
            Version version = new Version(built, keys, new ArrayList<>(keys));
            check(version, new Random(size));
            // the built order goes on as the appended one does
            randomWalk(version, size > 2000 ? 100 : 400, 2, size);
        }
    }

    @Test
    public void removingEveryKeyLeavesAnEmptyOrderThatGoesOn() {
        @SuppressWarnings("Var")
        Version version = emptyAt(0);
        for (int key = 0; key < 1100; key++) {
            version = append(version, key);
        }
        // from the front, then from the back
        @SuppressWarnings("Var")
        Version front = version;
        while (!front.keys().isEmpty()) {
            front = removeAt(front, 0);
        }
        check(front, new Random(5));
        check(append(front, 7), new Random(5));
        @SuppressWarnings("Var")
        Version back = version;
        while (!back.keys().isEmpty()) {
            back = removeAt(back, back.keys().size() - 1);
        }
        check(back, new Random(6));
        Version again = append(append(back, 1), 2);
        check(again, new Random(6));
        assertThat(again.ordinals()).containsExactly(1100, 1101);
    }

    @Test
    public void slicesKeepTheirKeysAndOrdinals() {
        @SuppressWarnings("Var")
        Version version = emptyAt(0);
        for (int key = 0; key < 1200; key++) {
            version = append(version, key);
        }
        Random random = new Random(7);
        int[] cuts = {0, 1, 31, 32, 33, 63, 64, 1023, 1024, 1025, 1056, 1150, 1199};
        for (int from : cuts) {
            for (int until : cuts) {
                if (from < until) {
                    Version sliced = slice(version, from, until);
                    check(sliced, random);
                    check(append(removeAt(sliced, 0), -1), random);
                }
            }
            if (from < 1200) {
                check(slice(version, from, 1200), random);
            }
        }
    }
}
