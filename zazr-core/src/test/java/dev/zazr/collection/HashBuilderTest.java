package dev.zazr.collection;

import dev.zazr.Tuple;
import dev.zazr.Tuple2;
import java.util.ArrayList;
import java.util.Random;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** HashSet.Builder and HashMap.Builder through the public API; the shape of the trie and the ownership of its nodes are
 *  checked by ChampMapTest and ChampSetTest. */
public class HashBuilderTest {

    private static final long SEED = 20260925L;
    private static final int[] SIZES = { 0, 1, 2, 31, 32, 33, 1023, 1024, 1025 };

    /** A key whose hash code is chosen by the test; two keys are equal when both the hash and the id are. */
    record Key(int hash, int id) {
        @Override
        public int hashCode() {
            return hash;
        }
    }

    private static <T> java.util.List<T> javaList(Iterable<T> elements) {
        final java.util.List<T> result = new ArrayList<>();
        elements.forEach(result::add);
        return result;
    }

    private static <K, V> java.util.Map<K, V> javaMap(Map<K, V> map) {
        final java.util.Map<K, V> result = new java.util.HashMap<>();
        map.forEach(t -> result.put(t._1(), t._2()));
        return result;
    }

    // successive persistent puts, the reference the builders are compared with, since ofAll and ofEntries now use the
    // builders themselves. The persistent HashMap.put of each element on itself builds a trie of the same shape as the
    // HashSet of the elements, so its entries in iteration order give the elements in the order of that trie.
    private static <T> java.util.List<T> addedSet(Iterable<T> elements) {
        HashMap<T, T> map = HashMap.empty();
        for (T element : elements) {
            map = map.put(element, element);
        }
        final java.util.List<T> result = new ArrayList<>();
        map.forEach(entry -> result.add(entry._1()));
        return result;
    }

    private static <K, V> HashMap<K, V> putMap(Iterable<Tuple2<K, V>> entries) {
        HashMap<K, V> map = HashMap.empty();
        for (Tuple2<K, V> entry : entries) {
            map = map.put(entry._1(), entry._2());
        }
        return map;
    }

    // -- HashSet

    @Test
    public void shouldBuildTheHashSetOfOfAllAtEveryBoundary() {
        for (int size : SIZES) {
            final java.util.List<Integer> input = IntStream.range(0, size).boxed().toList();
            final HashSet<Integer> built = HashSet.<Integer> newBuilder().addAll(input).result();
            // the same trie, so the same iteration order
            assertThat(javaList(built)).isEqualTo(addedSet(input));
            assertThat(javaList(HashSet.ofAll(input))).isEqualTo(javaList(built));
            assertThat(built.size()).isEqualTo(size);
        }
        assertThat(HashSet.newBuilder().result()).isSameAs(HashSet.empty());
    }

    @Test
    public void shouldMatchTheJdkHashSetOnRandomInputsWithCollisions() {
        final Random random = new Random(SEED);
        for (int round = 0; round < 200; round++) {
            final int size = random.nextInt(3000);
            final int hashRange = 1 + random.nextInt(round % 2 == 0 ? 64 : Integer.MAX_VALUE);
            final HashSet.Builder<Key> builder = HashSet.newBuilder();
            final java.util.Set<Key> oracle = new java.util.HashSet<>();
            final java.util.List<Key> input = new ArrayList<>();
            for (int i = 0; i < size; i++) {
                final Key key = new Key(random.nextInt(hashRange) - hashRange / 2, random.nextInt(3));
                input.add(key);
                oracle.add(key);
                if (random.nextBoolean()) {
                    builder.add(key);
                } else {
                    builder.addAll(java.util.List.of(key));
                }
                if (random.nextInt(100) == 0) {
                    assertThat(builder.size()).isEqualTo(oracle.size());
                }
            }
            assertThat(builder.size()).isEqualTo(oracle.size());
            final HashSet<Key> built = builder.result();
            assertThat(new java.util.HashSet<>(javaList(built))).isEqualTo(oracle);
            assertThat(javaList(built)).isEqualTo(addedSet(input));
        }
    }

    @Test
    public void shouldKeepTheFirstOfEqualElementsEverywhere() {
        final String first = new String("e");
        final String last = new String("e");
        final java.util.List<String> input = java.util.List.of(first, "x", last);
        final java.util.function.Function<HashSet<String>, String> kept = set -> set.find("e"::equals).get();
        assertThat(kept.apply(HashSet.<String> newBuilder().add(first).add("x").add(last).result())).isSameAs(first);
        assertThat(kept.apply(HashSet.<String> newBuilder().addAll(HashSet.of("y")).addAll(input).result())).isSameAs(first);
        assertThat(kept.apply(HashSet.<String> newBuilder().add(first).addAll(HashSet.of(last, "y")).result())).isSameAs(first);
        assertThat(kept.apply(HashSet.ofAll(input))).isSameAs(first);
        assertThat(kept.apply(HashSet.of(first, "x", last))).isSameAs(first);
        assertThat(kept.apply(input.stream().collect(HashSet.collector()))).isSameAs(first);
        assertThat(kept.apply(HashSet.flatten(java.util.List.of(java.util.List.of(first), java.util.List.of(last))))).isSameAs(first);
        final HashSet<String> set = HashSet.of(first, "x");
        assertThat(kept.apply(set.add(last))).isSameAs(first);
        // with a new element or without, the element already there stays
        assertThat(set.addAll(java.util.List.of(last))).isSameAs(set);
        assertThat(kept.apply(set.addAll(java.util.List.of(last, "y")))).isSameAs(first);
        assertThat(kept.apply(set.addAll(HashSet.of(last, "y")))).isSameAs(first);
        assertThat(kept.apply(set.union(HashSet.of(last, "y")))).isSameAs(first);
        assertThat(kept.apply(set.union(LinkedHashSet.of(last, "y")))).isSameAs(first);
        assertThat(set.union(HashSet.of(last))).isSameAs(set);
        assertThat(kept.apply(set.map(e -> e.equals("x") ? last : e))).isSameAs(first);
        assertThat(kept.apply(set.flatMap(e -> java.util.List.of(e, last)))).isSameAs(first);
        assertThat(kept.apply(set.collect(e -> dev.zazr.control.Option.some(e.equals("x") ? last : e)))).isSameAs(first);
        assertThat(kept.apply(set.partitionMap(e -> dev.zazr.control.Either.<String, String> left(e.equals("x") ? last : e))._1()))
                .isSameAs(first);
        // intersect keeps the elements of the receiver, whichever side is smaller
        final HashSet<String> big = HashSet.of(first, "x", "y", "z");
        assertThat(kept.apply(big.intersect(HashSet.of(last)))).isSameAs(first);
        assertThat(kept.apply(big.intersect(HashSet.of(last, "x", "y", "z", "w")))).isSameAs(first);
        assertThat(kept.apply(big.intersect(LinkedHashSet.of(last)))).isSameAs(first);
        assertThat(big.intersect(HashSet.of(new String("x"), last, "y", "z"))).isSameAs(big);
        assertThat(kept.apply(HashSet.of(last).intersect(big))).isSameAs(last);
        // the same answers on colliding hash codes, over every node boundary
        for (int size : new int[] { 1, 2, 31, 32, 33, 1023, 1024, 1025 }) {
            final java.util.List<Key> firsts = new ArrayList<>();
            final java.util.List<Key> lasts = new ArrayList<>();
            for (int i = 0; i < size; i++) {
                firsts.add(new Key(i % 40, i));
                lasts.add(new Key(i % 40, i));
            }
            final java.util.List<Key> both = new ArrayList<>(firsts);
            both.addAll(lasts);
            final HashSet<Key> built = HashSet.<Key> newBuilder().addAll(both).result();
            final HashSet<Key> unioned = HashSet.ofAll(firsts).union(HashSet.ofAll(lasts).add(new Key(-1, -1)));
            final HashSet<Key> added = HashSet.ofAll(firsts).addAll(lasts);
            for (HashSet<Key> result : java.util.List.of(built, HashSet.ofAll(both), unioned, added)) {
                final java.util.Set<Key> identities = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());
                result.forEach(identities::add);
                assertThat(identities).containsAll(firsts);
            }
        }
    }

    @Test
    public void shouldAdoptAHashSetAndNeverChangeIt() {
        final Random random = new Random(SEED);
        for (int size : SIZES) {
            final HashSet<Integer> source = HashSet.ofAll(IntStream.range(0, size).boxed().toList());
            final java.util.List<Integer> sourceBefore = javaList(source);
            final HashSet.Builder<Integer> left = HashSet.<Integer> newBuilder().addAll(source);
            final HashSet.Builder<Integer> right = HashSet.<Integer> newBuilder().addAll(source.asJava());
            final java.util.Set<Integer> leftOracle = new java.util.HashSet<>(sourceBefore);
            final java.util.Set<Integer> rightOracle = new java.util.HashSet<>(sourceBefore);
            for (int i = 0; i < 100; i++) {
                final int l = random.nextInt(2 * size + 10);
                final int r = -1 - random.nextInt(2 * size + 10);
                left.add(l);
                right.add(r);
                leftOracle.add(l);
                rightOracle.add(r);
            }
            final HashSet<Integer> l = left.result();
            final HashSet<Integer> r = right.result();
            assertThat(javaList(source)).isEqualTo(sourceBefore);
            assertThat(source.size()).isEqualTo(size);
            assertThat(new java.util.HashSet<>(javaList(l))).isEqualTo(leftOracle);
            assertThat(new java.util.HashSet<>(javaList(r))).isEqualTo(rightOracle);
            assertThat(l.size()).isEqualTo(leftOracle.size());
            assertThat(r.size()).isEqualTo(rightOracle.size());
        }
    }

    @Test
    public void shouldNeverChangeAnAdoptedHashSetOrHashMapBuiltByPersistentAdditions() {
        // sources made by persistent add and put, whose nodes no builder owns; the first write adds a child to the root
        final HashSet<Integer> set = HashSet.<Integer> empty().add(0).add(1);
        final HashSet<Integer> builtSet = HashSet.<Integer> newBuilder().addAll(set).add(2).result();
        assertThat(javaList(set)).containsExactlyInAnyOrder(0, 1);
        assertThat(set.size()).isEqualTo(2);
        assertThat(set.contains(2)).isFalse();
        assertThat(builtSet).isEqualTo(HashSet.of(0, 1, 2));

        final HashMap<Integer, String> map = HashMap.<Integer, String> empty().put(0, "a").put(1, "b");
        final HashMap<Integer, String> builtMap = HashMap.<Integer, String> newBuilder().putAll(map).put(2, "c").put(0, "z").result();
        assertThat(javaMap(map)).isEqualTo(java.util.Map.of(0, "a", 1, "b"));
        assertThat(map.size()).isEqualTo(2);
        assertThat(builtMap).isEqualTo(HashMap.of(0, "z", 1, "b", 2, "c"));
    }

    @Test
    public void shouldBuildTheSameHashSetFromNothingAsAfterAnAdoptedOne() {
        final HashSet<Integer> source = HashSet.ofAll(IntStream.range(0, 1025).boxed().toList());
        final java.util.List<Integer> more = IntStream.range(500, 2000).boxed().toList();
        final HashSet<Integer> adopted = HashSet.<Integer> newBuilder().addAll(source).addAll(more).result();
        final HashSet<Integer> fromNothing = HashSet.<Integer> newBuilder().addAll(javaList(source)).addAll(more).result();
        assertThat(adopted).isEqualTo(fromNothing);
        assertThat(javaList(adopted)).isEqualTo(javaList(fromNothing));
        assertThat(adopted).isEqualTo(HashSet.range(0, 2000));
        // a set added to a non-empty builder is added element by element
        final HashSet<Integer> merged = HashSet.<Integer> newBuilder().add(-1).addAll(source).addAll(HashSet.empty()).result();
        assertThat(merged).isEqualTo(source.add(-1));
    }

    @Test
    public void shouldBuildAHashSetThatSupportsTheSetOperations() {
        final HashSet<Integer> built = HashSet.<Integer> newBuilder().addAll(IntStream.range(0, 1025).boxed().toList()).result();
        final HashSet<Integer> changed = built.remove(3).add(5000).removeAll(java.util.List.of(0, 1, 2));
        assertThat(changed.size()).isEqualTo(1022);
        assertThat(changed.contains(5000)).isTrue();
        assertThat(changed.contains(3)).isFalse();
        assertThat(built.size()).isEqualTo(1025);
        assertThat(built.contains(3)).isTrue();
        assertThat(built.contains(5000)).isFalse();
        assertThat(HashSet.ofAll(built)).isSameAs(built);
    }

    @Test
    public void shouldRefuseHashSetBuilderUseAfterResult() {
        final HashSet.Builder<Integer> builder = HashSet.<Integer> newBuilder().add(1);
        final HashSet<Integer> built = builder.result();
        assertThatThrownBy(() -> builder.add(2)).isInstanceOf(IllegalStateException.class)
                .hasMessage("result() has already been called on this HashSet.Builder");
        assertThatThrownBy(() -> builder.add(null)).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> builder.addAll(java.util.List.of(3))).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> builder.addAll(HashSet.of(3))).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> builder.addAll(null)).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(builder::size).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(builder::result).isInstanceOf(IllegalStateException.class);
        assertThat(javaList(built)).containsExactly(1);
    }

    @Test
    public void shouldRejectNullsInTheHashSetBuilderAndKeepWhatCameBefore() {
        assertThatThrownBy(() -> HashSet.newBuilder().add(null)).isInstanceOf(NullPointerException.class)
                .hasMessage("HashSet.Builder.add: element is null");
        assertThatThrownBy(() -> HashSet.newBuilder().addAll(null)).isInstanceOf(NullPointerException.class);
        final HashSet.Builder<Integer> builder = HashSet.newBuilder();
        assertThatThrownBy(() -> builder.addAll(java.util.Arrays.asList(3, 1, null, 2))).isInstanceOf(NullPointerException.class);
        assertThat(builder.result()).isEqualTo(HashSet.of(1, 3));
    }

    @Test
    public void shouldCollectAHashSetInParallel() {
        final java.util.List<Integer> input = new ArrayList<>();
        final Random random = new Random(SEED);
        for (int i = 0; i < 20_000; i++) {
            input.add(random.nextInt(5000));
        }
        final HashSet<Integer> expected = HashSet.ofAll(addedSet(input));
        assertThat(input.stream().collect(HashSet.collector())).isEqualTo(expected);
        assertThat(input.parallelStream().collect(HashSet.collector())).isEqualTo(expected);
    }

    // -- HashMap

    @Test
    public void shouldBuildTheHashMapOfOfEntriesAtEveryBoundary() {
        for (int size : SIZES) {
            final java.util.List<Tuple2<Integer, String>> entries = IntStream.range(0, size).mapToObj(k -> Tuple.of(k, "v" + k)).toList();
            final HashMap<Integer, String> built = HashMap.<Integer, String> newBuilder().putAll(entries).result();
            assertThat(built).isEqualTo(putMap(entries));
            assertThat(javaList(built)).isEqualTo(javaList(putMap(entries)));
            assertThat(javaList(HashMap.ofEntries(entries))).isEqualTo(javaList(built));
            assertThat(built.size()).isEqualTo(size);
        }
        assertThat(HashMap.newBuilder().result()).isSameAs(HashMap.empty());
    }

    @Test
    public void shouldMatchTheJdkHashMapOnRandomInputsWithCollisions() {
        final Random random = new Random(SEED);
        for (int round = 0; round < 200; round++) {
            final int size = random.nextInt(3000);
            final int hashRange = 1 + random.nextInt(round % 2 == 0 ? 64 : Integer.MAX_VALUE);
            final HashMap.Builder<Key, Integer> builder = HashMap.newBuilder();
            final java.util.Map<Key, Integer> oracle = new java.util.HashMap<>();
            final java.util.List<Tuple2<Key, Integer>> input = new ArrayList<>();
            for (int i = 0; i < size; i++) {
                final Key key = new Key(random.nextInt(hashRange) - hashRange / 2, random.nextInt(3));
                input.add(Tuple.of(key, i));
                oracle.put(key, i);
                switch (random.nextInt(3)) {
                    case 0 -> builder.put(key, i);
                    case 1 -> builder.put(Tuple.of(key, i));
                    default -> builder.putAll(java.util.List.of(Tuple.of(key, i)));
                }
            }
            assertThat(builder.size()).isEqualTo(oracle.size());
            final HashMap<Key, Integer> built = builder.result();
            assertThat(javaMap(built)).isEqualTo(oracle);
            assertThat(javaList(built)).isEqualTo(javaList(putMap(input)));
        }
    }

    @Test
    public void shouldKeepTheLastEntryOfEqualKeysAsOfEntriesDoes() {
        final String first = new String("k");
        final String last = new String("k");
        final java.util.List<Tuple2<String, Integer>> input = java.util.List.of(Tuple.of(first, 1), Tuple.of("x", 2), Tuple.of(last, 3));
        final HashMap<String, Integer> built = HashMap.<String, Integer> newBuilder().putAll(input).result();
        assertThat(built.get("k").get()).isEqualTo(3);
        assertThat(built.keySet().find("k"::equals).get()).isSameAs(last);
        assertThat(putMap(input).keySet().find("k"::equals).get()).isSameAs(last);
        assertThat(HashMap.ofEntries(input).keySet().find("k"::equals).get()).isSameAs(last);
    }

    @Test
    public void shouldAdoptAHashMapAndNeverChangeIt() {
        final Random random = new Random(SEED);
        for (int size : SIZES) {
            final HashMap<Integer, Integer> source = HashMap.ofEntries(IntStream.range(0, size).mapToObj(k -> Tuple.of(k, k)).toList());
            final java.util.List<Tuple2<Integer, Integer>> sourceBefore = javaList(source);
            final HashMap.Builder<Integer, Integer> left = HashMap.<Integer, Integer> newBuilder().putAll(source);
            final HashMap.Builder<Integer, Integer> right = HashMap.<Integer, Integer> newBuilder().putAll(source.asJava());
            final java.util.Map<Integer, Integer> leftOracle = javaMap(source);
            final java.util.Map<Integer, Integer> rightOracle = javaMap(source);
            for (int i = 0; i < 100; i++) {
                final int key = random.nextInt(2 * size + 10);
                left.put(key, -1);
                right.put(key, -2);
                leftOracle.put(key, -1);
                rightOracle.put(key, -2);
            }
            final HashMap<Integer, Integer> l = left.result();
            final HashMap<Integer, Integer> r = right.result();
            assertThat(javaList(source)).isEqualTo(sourceBefore);
            assertThat(source.size()).isEqualTo(size);
            assertThat(javaMap(l)).isEqualTo(leftOracle);
            assertThat(javaMap(r)).isEqualTo(rightOracle);
            assertThat(l.size()).isEqualTo(leftOracle.size());
            assertThat(r.size()).isEqualTo(rightOracle.size());
        }
    }

    @Test
    public void shouldBuildTheSameHashMapFromNothingAsAfterAnAdoptedOne() {
        final HashMap<Integer, Integer> source = HashMap.ofEntries(IntStream.range(0, 1025).mapToObj(k -> Tuple.of(k, k)).toList());
        final java.util.List<Tuple2<Integer, Integer>> more = IntStream.range(500, 2000).mapToObj(k -> Tuple.of(k, -k)).toList();
        final HashMap<Integer, Integer> adopted = HashMap.<Integer, Integer> newBuilder().putAll(source).putAll(more).result();
        final HashMap<Integer, Integer> fromNothing = HashMap.<Integer, Integer> newBuilder().putAll(javaList(source)).putAll(more).result();
        assertThat(adopted).isEqualTo(fromNothing);
        assertThat(javaList(adopted)).isEqualTo(javaList(fromNothing));
        assertThat(adopted.get(499).get()).isEqualTo(499);
        assertThat(adopted.get(500).get()).isEqualTo(-500);
        // a map put into a non-empty builder is put entry by entry, and its entries win
        final HashMap<Integer, Integer> merged = HashMap.<Integer, Integer> newBuilder().put(1, -1).put(-1, -1).putAll(source)
                .putAll(HashMap.empty()).result();
        assertThat(merged).isEqualTo(source.put(-1, -1));
    }

    @Test
    public void shouldBuildAHashMapThatSupportsTheMapOperations() {
        final HashMap.Builder<Integer, Integer> builder = HashMap.newBuilder();
        for (int i = 0; i < 1025; i++) {
            builder.put(i, -i);
        }
        final HashMap<Integer, Integer> built = builder.result();
        final HashMap<Integer, Integer> changed = built.remove(3).put(5000, 0).put(4, 4);
        assertThat(changed.size()).isEqualTo(1025);
        assertThat(changed.get(4).get()).isEqualTo(4);
        assertThat(built.get(4).get()).isEqualTo(-4);
        assertThat(built.containsKey(3)).isTrue();
        assertThat(built.containsKey(5000)).isFalse();
        assertThat(HashMap.ofAll(built.asJavaMap())).isSameAs(built);
    }

    @Test
    public void shouldRefuseHashMapBuilderUseAfterResult() {
        final HashMap.Builder<Integer, String> builder = HashMap.<Integer, String> newBuilder().put(1, "a");
        final HashMap<Integer, String> built = builder.result();
        assertThatThrownBy(() -> builder.put(2, "b")).isInstanceOf(IllegalStateException.class)
                .hasMessage("result() has already been called on this HashMap.Builder");
        assertThatThrownBy(() -> builder.put(null, null)).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> builder.put(Tuple.of(2, "b"))).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> builder.put(null)).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> builder.putAll(java.util.List.of(Tuple.of(3, "c")))).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> builder.putAll(HashMap.of(3, "c"))).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(builder::size).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(builder::result).isInstanceOf(IllegalStateException.class);
        assertThat(javaList(built)).containsExactly(Tuple.of(1, "a"));
    }

    @Test
    public void shouldRejectNullsInTheHashMapBuilderAndKeepWhatCameBefore() {
        assertThatThrownBy(() -> HashMap.<Integer, String> newBuilder().put(null, "a")).isInstanceOf(NullPointerException.class)
                .hasMessage("HashMap.Builder.put: key is null");
        assertThatThrownBy(() -> HashMap.<Integer, String> newBuilder().put(1, null)).isInstanceOf(NullPointerException.class)
                .hasMessage("HashMap.Builder.put: value is null");
        assertThatThrownBy(() -> HashMap.<Integer, String> newBuilder().put(null)).isInstanceOf(NullPointerException.class)
                .hasMessage("HashMap.Builder.put: entry is null");
        assertThatThrownBy(() -> HashMap.<Integer, String> newBuilder().put(Tuple.of(null, "a"))).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> HashMap.<Integer, String> newBuilder().put(Tuple.of(1, null))).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> HashMap.<Integer, String> newBuilder().putAll(null)).isInstanceOf(NullPointerException.class);
        final HashMap.Builder<Integer, String> builder = HashMap.newBuilder();
        assertThatThrownBy(() -> builder.putAll(java.util.Arrays.asList(Tuple.of(2, "b"), Tuple.of(1, "a"), Tuple.of(3, null))))
                .isInstanceOf(NullPointerException.class);
        assertThat(builder.result()).isEqualTo(HashMap.of(1, "a", 2, "b"));
    }

    @Test
    public void shouldCollectAHashMapInParallel() {
        final java.util.List<Tuple2<Integer, Integer>> input = new ArrayList<>();
        final Random random = new Random(SEED);
        for (int i = 0; i < 20_000; i++) {
            input.add(Tuple.of(random.nextInt(5000), i));
        }
        // the later of equal keys in encounter order wins, as sequentially
        final HashMap<Integer, Integer> expected = putMap(input);
        assertThat(input.stream().collect(HashMap.collector())).isEqualTo(expected);
        assertThat(input.parallelStream().collect(HashMap.collector())).isEqualTo(expected);
        assertThat(input.parallelStream().collect(HashMap.<Integer, Integer, Tuple2<Integer, Integer>> collector(Tuple2::_1, Tuple2::_2)))
                .isEqualTo(expected);
    }
}
