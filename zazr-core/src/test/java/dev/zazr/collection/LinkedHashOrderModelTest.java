package dev.zazr.collection;

import dev.zazr.Tuple;
import dev.zazr.Tuple2;
import dev.zazr.collection.internal.InsertionOrder;
import java.util.ArrayList;
import java.util.Random;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/// LinkedHashMap and LinkedHashSet against a model of their insertion order, a list of the entries: a key put again
/// keeps its first position and takes the last key object and value, a removed key put again goes last, and `replace`
/// gives the replaced entry's position to the new one, removing the new key from elsewhere. Random operations run on
/// random older versions, every result is compared with its model on every observable (order, equality, hash code,
/// text, lookups, both iteration orders, the positional operations), and every version is compared again at the end.
public class LinkedHashOrderModelTest {

    /* a key equal to another of the same id: the tag tells the key objects apart, and shows in the text */
    private static final class Key {
        final int id;
        final int tag;

        Key(int id, int tag) {
            this.id = id;
            this.tag = tag;
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof Key key && key.id == id;
        }

        @Override
        public int hashCode() {
            // a few ids share a hash, so that the hash map holds collisions
            return id / 3;
        }

        @Override
        public String toString() {
            return id + ":" + tag;
        }
    }

    /* the order starts 40 ordinals before the last one, so the 41st new key renumbers the keys from 0 */
    private static final LinkedHashMap<Key, Integer> FAR_START =
            LinkedHashMap.emptyStartingAt(InsertionOrder.LIMIT - 40);

    private record Version(LinkedHashMap<Key, Integer> map, java.util.List<Tuple2<Key, Integer>> model) {}

    private static int indexOfKey(java.util.List<Tuple2<Key, Integer>> model, Key key) {
        for (int i = 0; i < model.size(); i++) {
            if (model.get(i)._1().equals(key)) {
                return i;
            }
        }
        return -1;
    }

    private static java.util.List<Tuple2<Key, Integer>> put(
            java.util.List<Tuple2<Key, Integer>> model, Key key, int v) {
        java.util.List<Tuple2<Key, Integer>> next = new ArrayList<>(model);
        int index = indexOfKey(next, key);
        if (index >= 0) {
            next.set(index, Tuple.of(key, v));
        } else {
            next.add(Tuple.of(key, v));
        }
        return next;
    }

    private static java.util.List<Tuple2<Key, Integer>> remove(java.util.List<Tuple2<Key, Integer>> model, Key key) {
        java.util.List<Tuple2<Key, Integer>> next = new ArrayList<>(model);
        int index = indexOfKey(next, key);
        if (index >= 0) {
            next.remove(index);
        }
        return next;
    }

    private static java.util.List<Tuple2<Key, Integer>> replace(
            java.util.List<Tuple2<Key, Integer>> model, Tuple2<Key, Integer> current, Tuple2<Key, Integer> next) {
        if (current.equals(next) || !model.contains(current)) {
            return model;
        }
        java.util.List<Tuple2<Key, Integer>> result = new ArrayList<>(model);
        if (!current._1().equals(next._1())) {
            int obsolete = indexOfKey(result, next._1());
            if (obsolete >= 0) {
                result.remove(obsolete);
            }
        }
        result.set(indexOfKey(result, current._1()), next);
        return result;
    }

    private static java.util.List<Tuple2<Key, Integer>> slice(
            java.util.List<Tuple2<Key, Integer>> model, int from, int until) {
        int start = Math.max(0, Math.min(from, model.size()));
        int end = Math.max(start, Math.min(until, model.size()));
        return new ArrayList<>(model.subList(start, end));
    }

    private static Version step(Version version, Random random, int step, int ids) {
        LinkedHashMap<Key, Integer> map = version.map();
        java.util.List<Tuple2<Key, Integer>> model = version.model();
        int size = model.size();
        Key key = new Key(random.nextInt(ids), step);
        int n = random.nextInt(size + 3) - 1;
        int operation = random.nextInt(size == 0 ? 3 : 18);
        return switch (operation) {
            case 0, 1 -> new Version(map.put(key, step), put(model, key, step));
            case 2 -> new Version(map.put(Tuple.of(key, step)), put(model, key, step));
            case 3, 4, 5 -> new Version(map.remove(key), remove(model, key));
            case 6 -> {
                Tuple2<Key, Integer> current = model.get(random.nextInt(size));
                Tuple2<Key, Integer> next = Tuple.of(random.nextBoolean() ? current._1() : key, step);
                yield new Version(map.replace(current, next), replace(model, current, next));
            }
            case 7 -> new Version(map.tail(), slice(model, 1, size));
            case 8 -> new Version(map.init(), slice(model, 0, size - 1));
            case 9 -> new Version(map.take(n), slice(model, 0, n));
            case 10 -> new Version(map.drop(n), slice(model, Math.max(n, 0), size));
            case 11 -> new Version(map.takeRight(n), n <= 0 ? new ArrayList<>() : slice(model, size - n, size));
            case 12 -> new Version(map.dropRight(n), n <= 0 ? model : slice(model, 0, size - n));
            case 13 -> {
                int bound = random.nextInt(ids);
                int prefix = 0;
                while (prefix < size && model.get(prefix)._1().id < bound) {
                    prefix++;
                }
                yield random.nextBoolean()
                        ? new Version(map.takeWhile(e -> e._1().id < bound), slice(model, 0, prefix))
                        : new Version(map.dropWhile(e -> e._1().id < bound), slice(model, prefix, size));
            }
            case 14 -> {
                java.util.List<Key> keys =
                        java.util.List.of(key, model.get(random.nextInt(size))._1());
                java.util.List<Tuple2<Key, Integer>> expected = remove(remove(model, keys.get(0)), keys.get(1));
                yield new Version(map.removeAll(keys), expected);
            }
            case 15 -> {
                // the builder, given this map and then one more entry
                LinkedHashMap<Key, Integer> built = LinkedHashMap.<Key, Integer>newBuilder()
                        .putAll(map)
                        .put(key, step)
                        .result();
                yield new Version(built, put(model, key, step));
            }
            case 16 -> {
                // a map rebuilt from the model's entries, then the same put
                LinkedHashMap<Key, Integer> rebuilt = LinkedHashMap.ofEntries(model);
                yield new Version(rebuilt.put(key, step), put(model, key, step));
            }
            // removing then putting the key again sends it last
            default -> new Version(map.put(key, step).remove(key).put(key, step), put(remove(model, key), key, step));
        };
    }

    private static void check(Version version) {
        LinkedHashMap<Key, Integer> map = version.map();
        java.util.List<Tuple2<Key, Integer>> model = version.model();
        int size = model.size();
        assertThat(map.toVector().asJava()).isEqualTo(model);
        StringBuilder text = new StringBuilder("LinkedHashMap(");
        for (int i = 0; i < size; i++) {
            text.append(i == 0 ? "" : ", ").append(model.get(i));
        }
        assertThat(map.toString()).isEqualTo(text.append(")").toString());
        assertThat(map.size()).isEqualTo(size);
        assertThat(map.isEmpty()).isEqualTo(size == 0);
        LinkedHashMap<Key, Integer> rebuilt = LinkedHashMap.ofEntries(model);
        assertThat(map).isEqualTo(rebuilt);
        assertThat(map.hashCode()).isEqualTo(rebuilt.hashCode());
        if (size == 0) {
            // an emptied map is the empty one, except the test's own empty map that starts at a far ordinal, kept by
            // the calls that change nothing
            if (map != FAR_START) {
                assertThat(map).isSameAs(LinkedHashMap.empty());
            }
            return;
        }
        assertThat(map.head().toString()).isEqualTo(model.get(0).toString());
        assertThat(map.last().toString()).isEqualTo(model.get(size - 1).toString());
        java.util.List<Tuple2<Key, Integer>> reversed = new ArrayList<>(model);
        java.util.Collections.reverse(reversed);
        java.util.List<Tuple2<Key, Integer>> walkedBack = new ArrayList<>();
        map.asJavaMap().reversed().forEach((k, v) -> walkedBack.add(Tuple.of(k, v)));
        assertThat(walkedBack.toString()).isEqualTo(reversed.toString());
        for (Tuple2<Key, Integer> entry : model) {
            assertThat(map.get(entry._1()).get()).isEqualTo(entry._2());
        }
        assertThat(map.keySet().toVector().asJava().toString())
                .isEqualTo(model.stream().map(Tuple2::_1).toList().toString());
        assertThat(map.values().asJava())
                .isEqualTo(model.stream().map(Tuple2::_2).toList());
        Vector<Tuple2<Tuple2<Key, Integer>, Integer>> zipped = map.zipWithIndex();
        for (int i = 0; i < size; i++) {
            assertThat(zipped.get(i)).isEqualTo(Tuple.of(model.get(i), i));
        }
        int window = 1 + size / 3;
        Vector<LinkedHashMap<Key, Integer>> grouped = map.grouped(window);
        for (int i = 0; i < grouped.size(); i++) {
            assertThat(grouped.get(i).toVector().asJava()).isEqualTo(slice(model, i * window, (i + 1) * window));
        }
        Vector<LinkedHashMap<Key, Integer>> runs = map.slideBy(e -> e._1().id % 3);
        assertThat(runs.flatMap(LinkedHashMap::toVector).asJava()).isEqualTo(model);
        // the set of the keys, over the same order
        LinkedHashSet<Key> set = LinkedHashSet.ofAll(map.keySet());
        java.util.List<Key> keys = model.stream().map(Tuple2::_1).toList();
        assertThat(set.toString())
                .isEqualTo("LinkedHashSet("
                        + String.join(", ", keys.stream().map(Key::toString).toList()) + ")");
        assertThat(set.tail().toVector().asJava()).isEqualTo(keys.subList(1, size));
        assertThat(set.init().toVector().asJava()).isEqualTo(keys.subList(0, size - 1));
        assertThat(set.take(size / 2).toVector().asJava()).isEqualTo(keys.subList(0, size / 2));
        assertThat(set.drop(size / 2).toVector().asJava()).isEqualTo(keys.subList(size / 2, size));
        assertThat(set.zipWithIndex().map(Tuple2::_2).asJava())
                .isEqualTo(Vector.range(0, size).asJava());
    }

    private static void randomWalk(Version start, int steps, int ids, long seed) {
        Random random = new Random(seed);
        java.util.List<Version> versions = new ArrayList<>();
        versions.add(start);
        for (int step = 0; step < steps; step++) {
            Version version = random.nextInt(3) == 0
                    ? versions.get(random.nextInt(versions.size()))
                    : versions.get(versions.size() - 1);
            Version next = step(version, random, step, ids);
            check(next);
            versions.add(next);
        }
        for (Version version : versions) {
            check(version);
        }
    }

    @Test
    public void randomOperationsOnSmallMaps() {
        randomWalk(new Version(LinkedHashMap.empty(), new ArrayList<>()), 4000, 120, 173);
    }

    @Test
    public void randomOperationsOnAMapOfThreeLevels() {
        @SuppressWarnings("Var")
        Version version = new Version(LinkedHashMap.empty(), new ArrayList<>());
        for (int id = 0; id < 1500; id++) {
            Key key = new Key(id, -1);
            version = new Version(version.map().put(key, id), put(version.model(), key, id));
        }
        check(version);
        randomWalk(version, 400, 3000, 174);
    }

    @Test
    public void keysAreNumberedAgainWhenTheOrdinalsRunOut() {
        LinkedHashMap<Key, Integer> start = FAR_START;
        randomWalk(new Version(start, new ArrayList<>()), 1500, 60, 175);
        @SuppressWarnings("Var")
        Version version = new Version(start, new ArrayList<>());
        for (int id = 0; id < 100; id++) {
            Key key = new Key(id, 0);
            version = new Version(version.map().put(key, id), put(version.model(), key, id));
            if (id % 7 == 0) {
                Key gone = new Key(id / 2, 0);
                version = new Version(version.map().remove(gone), remove(version.model(), gone));
            }
            check(version);
        }
    }
}
