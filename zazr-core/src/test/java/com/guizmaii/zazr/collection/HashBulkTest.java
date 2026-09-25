package com.guizmaii.zazr.collection;

import com.guizmaii.zazr.Tuple2;
import java.util.ArrayList;
import java.util.Random;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** The bulk operations of HashMap and HashSet that work on whole subtrees when both sides are hash collections:
 *  which of two equal keys each keeps, when each returns the receiver, and that the other kinds of argument give the
 *  same answers. The subtree algorithms themselves are checked by ChampBulkTest. */
public class HashBulkTest {

    /** Equal when the id is; the tag tells two equal instances apart. A few hash codes make collision nodes. */
    static final class Key {
        final int id;
        final int tag;

        Key(int id, int tag) {
            this.id = id;
            this.tag = tag;
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof Key k && k.id == id;
        }

        @Override
        public int hashCode() {
            return id % 50;
        }
    }

    private static HashSet<Key> set(int from, int to, int tag) {
        HashSet<Key> result = HashSet.empty();
        for (int i = from; i < to; i++) {
            result = result.add(new Key(i, tag));
        }
        return result;
    }

    private static HashMap<Key, Integer> map(int from, int to, int tag) {
        HashMap<Key, Integer> result = HashMap.empty();
        for (int i = from; i < to; i++) {
            result = result.put(new Key(i, tag), tag);
        }
        return result;
    }

    private static java.util.Map<Integer, Integer> tags(Iterable<Key> keys) {
        final java.util.Map<Integer, Integer> result = new java.util.HashMap<>();
        keys.forEach(k -> result.put(k.id, k.tag));
        return result;
    }

    // -- HashSet

    @Test
    public void shouldUnionTwoHashSetsKeepingTheElementsOfTheReceiver() {
        final HashSet<Key> left = set(0, 300, 1);
        final HashSet<Key> right = set(200, 500, 2);
        final HashSet<Key> union = left.union(right);
        assertThat(union.size()).isEqualTo(500);
        tags(union).forEach((id, tag) -> assertThat(tag).isEqualTo(id < 300 ? 1 : 2));
        assertThat(left.addAll(right)).isEqualTo(union);
        tags(left.addAll(right)).forEach((id, tag) -> assertThat(tag).isEqualTo(id < 300 ? 1 : 2));
        // the same answers as with another kind of set
        assertThat(tags(left.union(LinkedHashSet.ofAll(right)))).isEqualTo(tags(union));
        // no new element: the receiver itself, its own elements kept
        assertThat(left.union(set(0, 100, 3))).isSameAs(left);
        assertThat(left.addAll(set(0, 100, 3))).isSameAs(left);
        assertThat(left.union(HashSet.empty())).isSameAs(left);
        assertThat(HashSet.<Key> empty().union(right)).isSameAs(right);
        // the argument holds everything: its elements, but the receiver's where they are equal
        final HashSet<Key> small = set(0, 10, 4);
        final HashSet<Key> all = small.union(right.addAll(set(0, 10, 5)));
        assertThat(all).hasSize(310);
        tags(all).forEach((id, tag) -> assertThat(tag).isEqualTo(id < 10 ? 4 : 2));
    }

    @Test
    public void shouldRemoveAHashSetKeepingTheElementsOfTheReceiver() {
        final HashSet<Key> left = set(0, 300, 1);
        final HashSet<Key> right = set(200, 500, 2);
        final HashSet<Key> diff = left.diff(right);
        assertThat(diff.size()).isEqualTo(200);
        tags(diff).forEach((id, tag) -> assertThat(tag).isEqualTo(1));
        assertThat(left.removeAll(right)).isEqualTo(diff);
        assertThat(left.removeAll(LinkedHashSet.ofAll(right))).isEqualTo(diff);
        assertThat(left.removeAll(set(1000, 1100, 2))).isSameAs(left);
        assertThat(left.removeAll(HashSet.empty())).isSameAs(left);
        assertThat(left.removeAll(set(0, 300, 9))).isSameAs(HashSet.empty());
        assertThat(HashSet.<Key> empty().removeAll(right)).isSameAs(HashSet.empty());
    }

    @Test
    public void shouldFilterAHashSetReturningTheReceiverWhenNothingIsDropped() {
        final HashSet<Key> set = set(0, 300, 1);
        assertThat(set.filter(k -> true)).isSameAs(set);
        assertThat(set.reject(k -> false)).isSameAs(set);
        assertThat(set.filter(k -> false)).isSameAs(HashSet.empty());
        assertThat(set.reject(k -> k.id % 3 == 0)).hasSize(200);
        assertThat(set.filter(k -> k.id % 3 == 0)).hasSize(100).allMatch(k -> k.id % 3 == 0);
        assertThatThrownBy(() -> set.filter(null)).isInstanceOf(NullPointerException.class).hasMessage("predicate is null");
        assertThatThrownBy(() -> set.reject(null)).isInstanceOf(NullPointerException.class).hasMessage("predicate is null");
    }

    @Test
    public void shouldTellWhetherAHashSetContainsAllOfAnother() {
        final HashSet<Key> set = set(0, 300, 1);
        assertThat(set.containsAll(set(0, 300, 2))).isTrue();
        assertThat(set.containsAll(set(100, 200, 2))).isTrue();
        assertThat(set.containsAll(set(250, 350, 2))).isFalse();
        assertThat(set.containsAll(HashSet.empty())).isTrue();
        assertThat(HashSet.<Key> empty().containsAll(set)).isFalse();
        assertThat(set.containsAll(java.util.List.of(new Key(5, 0)))).isTrue();
        assertThatThrownBy(() -> set.containsAll(null)).isInstanceOf(NullPointerException.class).hasMessage("elements is null");
    }

    @Test
    public void shouldCompareAndHashHashSetsAsAnySet() {
        final Random random = new Random(1);
        for (int round = 0; round < 50; round++) {
            final java.util.List<Integer> ids = new ArrayList<>();
            for (int i = 0; i < random.nextInt(400); i++) {
                ids.add(random.nextInt(1000));
            }
            HashSet<Key> a = HashSet.empty();
            for (Integer id : ids) {
                a = a.add(new Key(id, 1));
            }
            final java.util.List<Integer> shuffled = new ArrayList<>(ids);
            java.util.Collections.shuffle(shuffled, random);
            HashSet<Key> b = HashSet.empty();
            for (Integer id : shuffled) {
                b = b.add(new Key(id, 2));
            }
            assertThat(a).isEqualTo(b);
            assertThat(a.hashCode()).isEqualTo(b.hashCode());
            assertThat(a.hashCode()).isEqualTo(LinkedHashSet.ofAll(a).hashCode());
            assertThat(a.equals(LinkedHashSet.ofAll(a))).isTrue();
            assertThat(a.asJava().hashCode() + 1).isEqualTo(a.hashCode());
            if (!ids.isEmpty()) {
                final HashSet<Key> c = b.remove(new Key(ids.get(0), 0)).add(new Key(5000, 0));
                assertThat(a.equals(c)).isFalse();
            }
        }
        assertThat(HashSet.empty().hashCode()).isEqualTo(1);
        assertThat(HashSet.of(1).equals(HashSet.of("1"))).isFalse();
    }

    // -- HashMap

    @Test
    public void shouldMergeAHashMapKeepingTheEntriesOfTheReceiver() {
        final HashMap<Key, Integer> left = map(0, 300, 1);
        final HashMap<Key, Integer> right = map(200, 500, 2);
        final HashMap<Key, Integer> merged = left.merge(right);
        assertThat(merged.size()).isEqualTo(500);
        merged.forEach((k, v) -> {
            assertThat(k.tag).isEqualTo(k.id < 300 ? 1 : 2);
            assertThat(v).isEqualTo(k.tag);
        });
        assertThat(left.merge(LinkedHashMap.ofEntries(right))).isEqualTo(merged);
        // nothing new: the receiver itself
        assertThat(left.merge(map(0, 300, 3))).isSameAs(left);
        assertThat(left.merge(map(10, 20, 3))).isSameAs(left);
        assertThat(map(0, 1, 1).merge(map(0, 1, 2))).isEqualTo(map(0, 1, 1));
        assertThat(left.merge(HashMap.empty())).isSameAs(left);
        assertThat(HashMap.<Key, Integer> empty().merge(right)).isSameAs(right);
    }

    @Test
    public void shouldFilterAHashMapInEveryWay() {
        final HashMap<Key, Integer> map = map(0, 300, 1).put(new Key(7, 9), 9);
        assertThat(map.filter((k, v) -> true)).isSameAs(map);
        assertThat(map.filter((k, v) -> false)).isSameAs(HashMap.empty());
        assertThat(map.filter((k, v) -> v == 9)).hasSize(1);
        assertThat(map.reject((k, v) -> v == 9)).hasSize(299);
        assertThat(map.filter(t -> t._1().id < 100)).hasSize(100);
        assertThat(map.reject(t -> t._1().id < 100)).hasSize(200);
        assertThat(map.filterKeys(k -> k.id % 2 == 0)).hasSize(150);
        assertThat(map.rejectKeys(k -> k.id % 2 == 0)).hasSize(150);
        assertThat(map.filterValues(v -> v == 1)).hasSize(299);
        assertThat(map.rejectValues(v -> v == 1)).hasSize(1);
        for (Tuple2<Key, Integer> entry : map.filterKeys(k -> k.id == 7)) {
            assertThat(entry._1().tag).isEqualTo(9);
        }
        assertThatThrownBy(() -> map.filterKeys(null)).isInstanceOf(NullPointerException.class).hasMessage("predicate is null");
        assertThatThrownBy(() -> map.filter((java.util.function.BiPredicate<Key, Integer>) null))
                .isInstanceOf(NullPointerException.class).hasMessage("predicate is null");
    }

    @Test
    public void shouldMapTheValuesKeepingTheKeys() {
        final HashMap<Key, Integer> map = map(0, 300, 1);
        final HashMap<Key, String> mapped = map.mapValues(v -> "v" + v);
        assertThat(mapped.size()).isEqualTo(300);
        mapped.forEach((k, v) -> assertThat(v).isEqualTo("v1"));
        assertThat(map.mapValues(v -> v)).isSameAs(map);
        assertThat(map.replaceAll((k, v) -> k.id)).allMatch(t -> t._2() == t._1().id);
        assertThatThrownBy(() -> map.mapValues(v -> null)).isInstanceOf(NullPointerException.class).hasMessage("HashMap: value is null");
        assertThatThrownBy(() -> map.mapValues(null)).isInstanceOf(NullPointerException.class).hasMessage("valueMapper is null");
        assertThatThrownBy(() -> HashMap.<Key, Integer> empty().replaceAll(null)).isInstanceOf(NullPointerException.class)
                .hasMessage("function is null");
    }

    @Test
    public void shouldCompareHashMapsAsAnyMap() {
        final HashMap<Key, Integer> a = map(0, 300, 1);
        HashMap<Key, Integer> b = HashMap.empty();
        for (int i = 299; i >= 0; i--) {
            b = b.put(new Key(i, 2), 1);
        }
        assertThat(a).isEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
        assertThat(a.equals(LinkedHashMap.ofEntries(a))).isTrue();
        assertThat(a.equals(b.put(new Key(5, 0), 2))).isFalse();
        assertThat(a.equals(b.remove(new Key(5, 0)).put(new Key(5000, 0), 1))).isFalse();
        assertThat(HashMap.of(1, 1).equals(HashMap.of("1", 1))).isFalse();
    }
}
