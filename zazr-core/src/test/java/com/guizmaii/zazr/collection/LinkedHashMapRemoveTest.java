package com.guizmaii.zazr.collection;

import com.guizmaii.zazr.Tuple;
import com.guizmaii.zazr.Tuple2;
import java.util.Random;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class LinkedHashMapRemoveTest {

    // -- performance: remove(key) must not scan the insertion-order structure

    @Test
    public void shouldRemoveInReverseOrderInSubQuadraticTime() {
        final int n = 50_000;
        LinkedHashMap<Integer, Integer> map = LinkedHashMap.empty();
        for (int i = 0; i < n; i++) {
            map = map.put(i, i);
        }
        final long start = System.nanoTime();
        LinkedHashMap<Integer, Integer> result = map;
        for (int i = n - 1; i >= 0; i--) {
            result = result.remove(i);
        }
        final long elapsedMs = (System.nanoTime() - start) / 1_000_000;
        assertThat(result.isEmpty()).isTrue();
        // O(n) per remove takes seconds here; O(log n) takes tens of milliseconds.
        // The bound is deliberately loose to stay robust on slow CI machines.
        assertThat(elapsedMs).isLessThan(2_000);
    }

    // -- semantics: random interleaving must match java.util.LinkedHashMap

    @Test
    public void shouldMatchJavaLinkedHashMapUnderRandomPutRemoveInterleaving() {
        for (long seed = 0; seed < 5; seed++) {
            final Random random = new Random(seed);
            LinkedHashMap<Integer, Integer> actual = LinkedHashMap.empty();
            final java.util.LinkedHashMap<Integer, Integer> expected = new java.util.LinkedHashMap<>();
            for (int op = 0; op < 5_000; op++) {
                final int key = random.nextInt(200);
                if (random.nextInt(3) == 0) {
                    actual = actual.remove(key);
                    expected.remove(key);
                } else {
                    actual = actual.put(key, op);
                    expected.put(key, op);
                }
            }
            assertThat(actual.size()).isEqualTo(expected.size());
            final java.util.Iterator<java.util.Map.Entry<Integer, Integer>> expectedIterator = expected.entrySet().iterator();
            for (Tuple2<Integer, Integer> entry : actual) {
                final java.util.Map.Entry<Integer, Integer> expectedEntry = expectedIterator.next();
                assertThat(entry._1()).isEqualTo(expectedEntry.getKey());
                assertThat(entry._2()).isEqualTo(expectedEntry.getValue());
            }
            assertThat(expectedIterator.hasNext()).isFalse();
        }
    }

    @Test
    public void shouldKeepHeadAndLastConsistentWhileRemovingFromBothEnds() {
        final int n = 1_001;
        LinkedHashMap<Integer, Integer> map = LinkedHashMap.empty();
        for (int i = 0; i < n; i++) {
            map = map.put(i, i);
        }
        int lo = 0, hi = n - 1;
        while (lo < hi) {
            assertThat(map.iterator().next()).isEqualTo(Tuple.of(lo, lo));
            assertThat(map.toList().last()).isEqualTo(Tuple.of(hi, hi));
            map = map.remove(lo++).remove(hi--);
        }
        assertThat(map.size()).isEqualTo(1);
        assertThat(map.iterator().next()).isEqualTo(map.toList().last());
    }

    @Test
    public void shouldPreserveOrderAfterRemovingEveryOtherKey() {
        final int n = 1_000;
        LinkedHashMap<Integer, Integer> map = LinkedHashMap.empty();
        for (int i = 0; i < n; i++) {
            map = map.put(i, i);
        }
        for (int i = 0; i < n; i += 2) {
            map = map.remove(i);
        }
        assertThat(map.keySet().toList())
                .isEqualTo(List.range(0, n).filter(i -> i % 2 == 1));
        assertThat(map.iterator().next()).isEqualTo(Tuple.of(1, 1));
        assertThat(map.toList().last()).isEqualTo(Tuple.of(n - 1, n - 1));
    }

    @Test
    public void shouldPreserveInsertionPointWhenRemovedKeyIsReinserted() {
        LinkedHashMap<String, Integer> map = LinkedHashMap.of("a", 1, "b", 2, "c", 3)
                .remove("b")
                .put("b", 4);
        assertThat(new java.util.ArrayList<>(map.keySet().asJava())).containsExactly("a", "c", "b");
    }

    @Test
    public void shouldKeepTheOrderWhenRemovingEitherEndAfterInteriorRemovals() {
        LinkedHashMap<Integer, Integer> map = LinkedHashMap.empty();
        for (int i = 0; i < 100; i++) {
            map = map.put(i, i);
        }
        for (int i = 10; i < 90; i += 3) {
            map = map.remove(i);
        }
        final java.util.List<Integer> keys = new java.util.ArrayList<>(map.keySet().asJava());
        assertThat(new java.util.ArrayList<>(map.remove(keys.get(0)).keySet().asJava())).isEqualTo(keys.subList(1, keys.size()));
        assertThat(new java.util.ArrayList<>(map.remove(keys.get(keys.size() - 1)).keySet().asJava())).isEqualTo(keys.subList(0, keys.size() - 1));
    }

    @Test
    public void shouldCutRunsOfMarkersAtBothEndsInOneSlice() {
        // a live key at each end, and runs of removed keys next to them: removing an end cuts the whole run
        LinkedHashMap<Integer, Integer> map = LinkedHashMap.empty();
        for (int i = 0; i < 100; i++) {
            map = map.put(i, i);
        }
        for (int i = 1; i < 30; i++) {
            map = map.remove(i).remove(99 - i);
        }
        final LinkedHashMap<Integer, Integer> both = map.remove(0).remove(99);
        assertThat(both.keySet().toList()).isEqualTo(List.range(30, 70));
        assertThat(both.head()).isEqualTo(Tuple.of(30, 30));
        assertThat(both.last()).isEqualTo(Tuple.of(69, 69));
        assertThat(both.take(2).keySet().toList()).isEqualTo(List.of(30, 31));
        assertThat(both.takeRight(2).keySet().toList()).isEqualTo(List.of(68, 69));
        assertThat(both.tail().init().keySet().toList()).isEqualTo(List.range(31, 69));
        assertThat(both.put(0, 0).keySet().toList()).isEqualTo(List.range(30, 70).append(0));
        assertThat(both.remove(30).remove(69).keySet().toList()).isEqualTo(List.range(31, 69));
        // the older version is untouched and can be cut again
        assertThat(map.keySet().toList()).isEqualTo(List.of(0).appendAll(List.range(30, 70)).append(99));
        assertThat(map.remove(99).keySet().toList()).isEqualTo(List.of(0).appendAll(List.range(30, 70)));
    }

    @Test
    public void shouldMatchJavaLinkedHashMapUnderRandomEndRemovalsAndSlices() {
        for (long seed = 0; seed < 20; seed++) {
            final Random random = new Random(seed);
            LinkedHashMap<Integer, Integer> actual = LinkedHashMap.empty();
            java.util.LinkedHashMap<Integer, Integer> expected = new java.util.LinkedHashMap<>();
            LinkedHashMap<Integer, Integer> older = actual;
            java.util.LinkedHashMap<Integer, Integer> olderExpected = new java.util.LinkedHashMap<>();
            for (int op = 0; op < 400; op++) {
                final java.util.List<Integer> keys = new java.util.ArrayList<>(expected.keySet());
                switch (random.nextInt(8)) {
                    case 0, 1 -> {
                        final int key = random.nextInt(60);
                        actual = actual.put(key, op);
                        expected.put(key, op);
                    }
                    case 2 -> {
                        final int key = random.nextInt(60);
                        actual = actual.remove(key);
                        expected.remove(key);
                    }
                    case 3 -> {
                        if (!keys.isEmpty()) {
                            final int key = random.nextBoolean() ? keys.get(0) : keys.get(keys.size() - 1);
                            actual = actual.remove(key);
                            expected.remove(key);
                        }
                    }
                    case 4 -> {
                        final int k = random.nextInt(keys.size() + 2) - 1;
                        final boolean fromLeft = random.nextBoolean();
                        actual = fromLeft ? actual.take(k) : actual.drop(k);
                        final java.util.List<Integer> kept = fromLeft ? keys.subList(0, Math.max(0, Math.min(k, keys.size()))) : keys.subList(Math.max(0, Math.min(k, keys.size())), keys.size());
                        final java.util.LinkedHashMap<Integer, Integer> next = new java.util.LinkedHashMap<>();
                        for (Integer key : kept) {
                            next.put(key, expected.get(key));
                        }
                        expected = next;
                    }
                    case 5 -> {
                        older = actual;
                        olderExpected = new java.util.LinkedHashMap<>(expected);
                    }
                    default -> {
                        actual = older;
                        expected = new java.util.LinkedHashMap<>(olderExpected);
                    }
                }
                assertThat(actual.size()).isEqualTo(expected.size());
                assertThat(actual.toList()).isEqualTo(List.ofAll(expected.entrySet()).map(e -> Tuple.of(e.getKey(), e.getValue())));
            }
        }
    }

}
