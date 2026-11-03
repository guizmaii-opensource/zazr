package dev.zazr.collection;

import java.util.Random;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/// `TreeSet.diff` and `union` with a TreeSet, and `TreeMap.putAll` of a TreeMap, of the same comparator: the receiver
/// or the argument comes back when the result is either.
class TreeSharingTest {

    private static final long SEED = 20261103L;
    private static final int[] SIZES = {1, 2, 3, 31, 32, 33, 1023, 1024, 1025};

    private static TreeSet<Integer> randomSet(Random random, int size, int from) {
        @SuppressWarnings("Var")
        TreeSet<Integer> set = TreeSet.empty();
        while (set.size() < size) {
            set = set.add(Integer.valueOf(1_000_000 + from + random.nextInt(4 * size)));
        }
        return set;
    }

    @Test
    void diffOfADisjointSetIsTheReceiver() {
        Random random = new Random(SEED);
        for (int size : SIZES) {
            TreeSet<Integer> set = randomSet(random, size, 0);
            // interleaved with the receiver: the odd numbers against the even ones
            TreeSet<Integer> odd = set.map(i -> 2 * i + 1);
            TreeSet<Integer> even = set.map(i -> 2 * i);
            assertThat(set.diff(randomSet(random, size, 8 * size))).isSameAs(set);
            assertThat(even.diff(odd)).isSameAs(even);
        }
    }

    @Test
    void diffOfItselfIsEmptyAndOfASubsetHoldsTheRest() {
        Random random = new Random(SEED + 1);
        for (int size : SIZES) {
            TreeSet<Integer> set = randomSet(random, size, 0);
            assertThat(set.diff(set)).isEmpty();
            Integer last = set.last();
            assertThat(set.diff(set.remove(last))).containsExactly(last);
            assertThat(set.diff(set.add(0))).isEmpty();
        }
    }

    @Test
    void unionWithItselfIsTheReceiver() {
        TreeSet<Integer> set = randomSet(new Random(SEED + 2), 1000, 0);
        assertThat(set.union(set)).isSameAs(set);
    }

    @Test
    void putAllOfAMapWithOneValueReplacedIsThatMap() {
        Random random = new Random(SEED + 3);
        for (int size : SIZES) {
            @SuppressWarnings("Var")
            TreeMap<Integer, Object> map = TreeMap.empty();
            for (Integer k : randomSet(random, size, 0)) {
                map = map.put(k, new Object());
            }
            Integer key = map.keySet().toVector().get(random.nextInt(size));
            TreeMap<Integer, Object> replaced = map.put(key, new Object());
            assertThat(map.putAll(replaced)).isSameAs(replaced);
            assertThat(map.putAll(map)).isSameAs(map);
            TreeMap<Integer, Object> added = map.put(-1, new Object());
            assertThat(map.putAll(added)).isEqualTo(added);
            assertThat(map.putAll(map.remove(key))).isEqualTo(map);
        }
    }
}
