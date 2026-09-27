package dev.zazr.collection;

import dev.zazr.Tuple;
import dev.zazr.Tuple2;
import java.util.Random;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class LinkedHashMapRemoveTest {

    // -- performance: remove(key) must not scan the insertion-order structure

    @Test
    public void shouldRemoveInReverseOrderInSubQuadraticTime() {
        int n = 50_000;
        LinkedHashMap<Integer, Integer> map = ascending(n);
        long start = System.nanoTime();
        LinkedHashMap<Integer, Integer> result = Vector.rangeBy(n - 1, -1, -1).foldLeft(map, LinkedHashMap::remove);
        long elapsedMs = (System.nanoTime() - start) / 1_000_000;
        assertThat(result.isEmpty()).isTrue();
        // O(n) per remove takes seconds here; O(log n) takes tens of milliseconds.
        // The bound is deliberately loose to stay robust on slow CI machines.
        assertThat(elapsedMs).isLessThan(2_000);
    }

    // -- semantics: random interleaving must match java.util.LinkedHashMap

    @Test
    public void shouldMatchJavaLinkedHashMapUnderRandomPutRemoveInterleaving() {
        for (long seed = 0; seed < 5; seed++) {
            Random random = new Random(seed);
            java.util.LinkedHashMap<Integer, Integer> expected = new java.util.LinkedHashMap<>();
            LinkedHashMap<Integer, Integer> actual = Vector.range(0, 5_000)
                    .foldLeft(LinkedHashMap.<Integer, Integer>empty(), (acc, op) -> {
                        int key = random.nextInt(200);
                        if (random.nextInt(3) == 0) {
                            expected.remove(key);
                            return acc.remove(key);
                        }
                        expected.put(key, op);
                        return acc.put(key, op);
                    });
            assertThat(actual.size()).isEqualTo(expected.size());
            java.util.Iterator<java.util.Map.Entry<Integer, Integer>> expectedIterator =
                    expected.entrySet().iterator();
            for (Tuple2<Integer, Integer> entry : actual) {
                java.util.Map.Entry<Integer, Integer> expectedEntry = expectedIterator.next();
                assertThat(entry._1()).isEqualTo(expectedEntry.getKey());
                assertThat(entry._2()).isEqualTo(expectedEntry.getValue());
            }
            assertThat(expectedIterator.hasNext()).isFalse();
        }
    }

    @Test
    public void shouldKeepHeadAndLastConsistentWhileRemovingFromBothEnds() {
        int n = 1_001;
        // the lowest key `i` and the highest `n - 1 - i` removed at step `i`, until they meet
        LinkedHashMap<Integer, Integer> map = Vector.range(0, n / 2).foldLeft(ascending(n), (acc, i) -> {
            int lo = i;
            int hi = n - 1 - i;
            assertThat(acc.iterator().next()).isEqualTo(Tuple.of(lo, lo));
            assertThat(acc.toList().last()).isEqualTo(Tuple.of(hi, hi));
            return acc.remove(lo).remove(hi);
        });
        assertThat(map.size()).isEqualTo(1);
        assertThat(map.iterator().next()).isEqualTo(map.toList().last());
    }

    @Test
    public void shouldPreserveOrderAfterRemovingEveryOtherKey() {
        int n = 1_000;
        LinkedHashMap<Integer, Integer> map = Vector.rangeBy(0, n, 2).foldLeft(ascending(n), LinkedHashMap::remove);
        assertThat(map.keySet().toList()).isEqualTo(List.range(0, n).filter(i -> i % 2 == 1));
        assertThat(map.iterator().next()).isEqualTo(Tuple.of(1, 1));
        assertThat(map.toList().last()).isEqualTo(Tuple.of(n - 1, n - 1));
    }

    @Test
    public void shouldPreserveInsertionPointWhenRemovedKeyIsReinserted() {
        LinkedHashMap<String, Integer> map =
                LinkedHashMap.of("a", 1, "b", 2, "c", 3).remove("b").put("b", 4);
        assertThat(new java.util.ArrayList<>(map.keySet().asJava())).containsExactly("a", "c", "b");
    }

    @Test
    public void shouldKeepTheOrderWhenRemovingEitherEndAfterInteriorRemovals() {
        LinkedHashMap<Integer, Integer> map = Vector.rangeBy(10, 90, 3).foldLeft(ascending(100), LinkedHashMap::remove);
        java.util.List<Integer> keys = new java.util.ArrayList<>(map.keySet().asJava());
        assertThat(new java.util.ArrayList<>(map.remove(keys.get(0)).keySet().asJava()))
                .isEqualTo(keys.subList(1, keys.size()));
        assertThat(new java.util.ArrayList<>(
                        map.remove(keys.get(keys.size() - 1)).keySet().asJava()))
                .isEqualTo(keys.subList(0, keys.size() - 1));
    }

    @Test
    public void shouldCutRunsOfMarkersAtBothEndsInOneSlice() {
        // a live key at each end, and runs of removed keys next to them: removing an end cuts the whole run
        LinkedHashMap<Integer, Integer> map = Vector.range(1, 30)
                .foldLeft(ascending(100), (acc, i) -> acc.remove(i).remove(99 - i));
        LinkedHashMap<Integer, Integer> both = map.remove(0).remove(99);
        assertThat(both.keySet().toList()).isEqualTo(List.range(30, 70));
        assertThat(both.head()).isEqualTo(Tuple.of(30, 30));
        assertThat(both.last()).isEqualTo(Tuple.of(69, 69));
        assertThat(both.take(2).keySet().toList()).isEqualTo(List.of(30, 31));
        assertThat(both.takeRight(2).keySet().toList()).isEqualTo(List.of(68, 69));
        assertThat(both.tail().init().keySet().toList()).isEqualTo(List.range(31, 69));
        assertThat(both.put(0, 0).keySet().toList())
                .isEqualTo(List.range(30, 70).append(0));
        assertThat(both.remove(30).remove(69).keySet().toList()).isEqualTo(List.range(31, 69));
        // the older version is untouched and can be cut again
        assertThat(map.keySet().toList())
                .isEqualTo(List.of(0).appendAll(List.range(30, 70)).append(99));
        assertThat(map.remove(99).keySet().toList()).isEqualTo(List.of(0).appendAll(List.range(30, 70)));
    }

    @Test
    public void shouldMatchJavaLinkedHashMapUnderRandomEndRemovalsAndSlices() {
        for (long seed = 0; seed < 20; seed++) {
            Random random = new Random(seed);
            Models start = new Models(
                    new Model(LinkedHashMap.empty(), new java.util.LinkedHashMap<>()),
                    new Model(LinkedHashMap.empty(), new java.util.LinkedHashMap<>()));
            Vector.range(0, 400).foldLeft(start, (models, op) -> {
                Models next = randomEndStep(random, op, models);
                Model current = next.current();
                assertThat(current.actual().size()).isEqualTo(current.expected().size());
                assertThat(current.actual().toList())
                        .isEqualTo(
                                List.ofAll(current.expected().entrySet()).map(e -> Tuple.of(e.getKey(), e.getValue())));
                return next;
            });
        }
    }

    // the keys 0 to n - 1, each mapped to itself, put in ascending order
    private static LinkedHashMap<Integer, Integer> ascending(int n) {
        return Vector.range(0, n).foldLeft(LinkedHashMap.<Integer, Integer>empty(), (map, i) -> map.put(i, i));
    }

    // the value under test and the java.util.LinkedHashMap it must match
    private record Model(LinkedHashMap<Integer, Integer> actual, java.util.LinkedHashMap<Integer, Integer> expected) {}

    // the current model, and the one the last snapshot saved
    private record Models(Model current, Model saved) {}

    // a random put, removal of any key or of an end, take or drop, snapshot, or return to the snapshot, applied to the
    // value under test and to its model (whose java.util.LinkedHashMap is updated in place)
    private static Models randomEndStep(Random random, int op, Models models) {
        LinkedHashMap<Integer, Integer> actual = models.current().actual();
        java.util.LinkedHashMap<Integer, Integer> expected = models.current().expected();
        java.util.List<Integer> keys = new java.util.ArrayList<>(expected.keySet());
        return switch (random.nextInt(8)) {
            case 0, 1 -> {
                int key = random.nextInt(60);
                expected.put(key, op);
                yield new Models(new Model(actual.put(key, op), expected), models.saved());
            }
            case 2 -> {
                int key = random.nextInt(60);
                expected.remove(key);
                yield new Models(new Model(actual.remove(key), expected), models.saved());
            }
            case 3 -> {
                if (keys.isEmpty()) {
                    yield models;
                }
                int key = random.nextBoolean() ? keys.get(0) : keys.get(keys.size() - 1);
                expected.remove(key);
                yield new Models(new Model(actual.remove(key), expected), models.saved());
            }
            case 4 -> {
                int k = random.nextInt(keys.size() + 2) - 1;
                boolean fromLeft = random.nextBoolean();
                java.util.List<Integer> kept = fromLeft
                        ? keys.subList(0, Math.max(0, Math.min(k, keys.size())))
                        : keys.subList(Math.max(0, Math.min(k, keys.size())), keys.size());
                java.util.LinkedHashMap<Integer, Integer> next = new java.util.LinkedHashMap<>();
                for (Integer key : kept) {
                    next.put(key, expected.get(key));
                }
                yield new Models(new Model(fromLeft ? actual.take(k) : actual.drop(k), next), models.saved());
            }
            case 5 -> new Models(models.current(), new Model(actual, new java.util.LinkedHashMap<>(expected)));
            default ->
                new Models(
                        new Model(
                                models.saved().actual(),
                                new java.util.LinkedHashMap<>(models.saved().expected())),
                        models.saved());
        };
    }
}
