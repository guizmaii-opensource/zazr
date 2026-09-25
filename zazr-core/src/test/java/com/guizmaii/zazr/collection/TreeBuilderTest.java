package com.guizmaii.zazr.collection;

import com.guizmaii.zazr.Tuple;
import com.guizmaii.zazr.Tuple2;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Random;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** TreeSet.Builder and TreeMap.Builder through the public API; the shape of the tree is checked by RedBlackTreeBuilderTest. */
public class TreeBuilderTest {

    private static final long SEED = 20260925L;
    private static final int[] SIZES = { 0, 1, 2, 31, 32, 33, 1023, 1024, 1025 };
    private static final Comparator<Integer> NATURAL = Comparator.naturalOrder();
    private static final Comparator<Integer> REVERSED = Comparator.reverseOrder();

    private static java.util.List<Integer> shuffled(int size, Random random) {
        final java.util.List<Integer> values = new ArrayList<>(IntStream.range(0, size).boxed().toList());
        java.util.Collections.shuffle(values, random);
        return values;
    }

    private static <T> java.util.List<T> javaList(Iterable<T> elements) {
        final java.util.List<T> result = new ArrayList<>();
        elements.forEach(result::add);
        return result;
    }

    // successive persistent insertions keeping the last of equal elements (TreeSet.add keeps the first): the reference
    // the builders are compared with, since ofAll and ofEntries now use the builders themselves
    private static <T> TreeSet<T> insertedSet(Comparator<? super T> order, Iterable<T> elements) {
        TreeSet<T> set = TreeSet.empty(order);
        for (T element : elements) {
            set = set.remove(element).add(element);
        }
        return set;
    }

    private static <K, V> TreeMap<K, V> insertedMap(Comparator<? super K> order, Iterable<Tuple2<K, V>> entries) {
        TreeMap<K, V> map = TreeMap.empty(order);
        for (Tuple2<K, V> entry : entries) {
            map = map.put(entry._1(), entry._2());
        }
        return map;
    }

    // -- TreeSet

    @Test
    public void shouldBuildTheTreeSetOfOfAllAtEveryBoundary() {
        final Random random = new Random(SEED);
        for (int size : SIZES) {
            for (java.util.List<Integer> input : java.util.List.of(IntStream.range(0, size).boxed().toList(), shuffled(size, random))) {
                final TreeSet<Integer> built = TreeSet.<Integer> newBuilder().addAll(input).result();
                assertThat(built).isEqualTo(insertedSet(NATURAL, input));
                assertThat(TreeSet.ofAll(input)).isEqualTo(built);
                assertThat(javaList(built)).isEqualTo(IntStream.range(0, size).boxed().toList());
                assertThat(built.size()).isEqualTo(size);
            }
        }
    }

    @Test
    public void shouldMatchTheJdkTreeSetOnRandomInputs() {
        final Random random = new Random(SEED);
        for (Comparator<Integer> order : java.util.List.of(NATURAL, REVERSED)) {
            for (int round = 0; round < 200; round++) {
                final int size = random.nextInt(2000);
                final int range = 1 + random.nextInt(Math.max(1, size * 2));
                final TreeSet.Builder<Integer> builder = TreeSet.newBuilder(order);
                final java.util.TreeSet<Integer> oracle = new java.util.TreeSet<>(order);
                final java.util.List<Integer> input = new ArrayList<>();
                for (int i = 0; i < size; i++) {
                    final int value = random.nextInt(range);
                    input.add(value);
                    oracle.add(value);
                    if (random.nextBoolean()) {
                        builder.add(value);
                    } else {
                        builder.addAll(java.util.List.of(value));
                    }
                }
                assertThat(builder.size()).isEqualTo(oracle.size());
                final TreeSet<Integer> built = builder.result();
                assertThat(javaList(built)).isEqualTo(new ArrayList<>(oracle));
                assertThat(built).isEqualTo(insertedSet(order, input));
                assertThat(built.comparator()).isSameAs(order);
            }
        }
    }

    @Test
    public void shouldKeepTheLastOfEqualElementsAsOfAllDoes() {
        final java.util.List<String> input = java.util.List.of("b", "A", "a", "B", "c", "b", "C");
        final TreeSet<String> built = TreeSet.newBuilder(String.CASE_INSENSITIVE_ORDER).addAll(input).result();
        assertThat(javaList(built)).containsExactly("a", "b", "C");
        assertThat(javaList(built)).isEqualTo(javaList(insertedSet(String.CASE_INSENSITIVE_ORDER, input)));
        assertThat(javaList(TreeSet.ofAll(String.CASE_INSENSITIVE_ORDER, input))).isEqualTo(javaList(built));
    }

    @Test
    public void shouldBuildEmptyTreeSetWithItsComparator() {
        final TreeSet<Integer> built = TreeSet.newBuilder(REVERSED).result();
        assertThat(built).isEmpty();
        assertThat(built.comparator()).isSameAs(REVERSED);
        assertThat(javaList(built.add(1).add(2))).containsExactly(2, 1);
        assertThat(TreeSet.<Integer> newBuilder().result()).isEqualTo(TreeSet.empty());
    }

    @Test
    public void shouldBuildATreeSetThatSupportsTheTreeOperations() {
        final TreeSet<Integer> built = TreeSet.<Integer> newBuilder().addAll(shuffled(1025, new Random(SEED))).result();
        final java.util.TreeSet<Integer> oracle = new java.util.TreeSet<>(IntStream.range(0, 1025).boxed().toList());
        assertThat(javaList(built.remove(512).add(2000).removeAll(java.util.List.of(0, 1, 2))))
                .isEqualTo(IntStream.concat(IntStream.range(3, 1025).filter(i -> i != 512), IntStream.of(2000)).boxed().toList());
        assertThat(javaList(built.take(10))).isEqualTo(new ArrayList<>(oracle).subList(0, 10));
        assertThat(built.drop(1000).size()).isEqualTo(25);
        assertThat(built.union(TreeSet.range(1000, 1100)).size()).isEqualTo(1100);
        assertThat(built.contains(1024)).isTrue();
        assertThat(built.contains(1025)).isFalse();
        // ofAll of a TreeSet of the same comparator is that set
        assertThat(TreeSet.ofAll(built.comparator(), built)).isSameAs(built);
    }

    @Test
    public void shouldRefuseTreeSetBuilderUseAfterResult() {
        final TreeSet.Builder<Integer> builder = TreeSet.<Integer> newBuilder().add(1);
        final TreeSet<Integer> built = builder.result();
        assertThatThrownBy(() -> builder.add(2)).isInstanceOf(IllegalStateException.class)
                .hasMessage("result() has already been called on this TreeSet.Builder");
        // the closed check comes first, even for a null argument
        assertThatThrownBy(() -> builder.add(null)).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> builder.addAll(java.util.List.of(3))).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> builder.addAll(null)).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(builder::size).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(builder::result).isInstanceOf(IllegalStateException.class);
        assertThat(javaList(built)).containsExactly(1);
    }

    @Test
    public void shouldRejectNullsInTheTreeSetBuilderAndKeepWhatCameBefore() {
        assertThatThrownBy(() -> TreeSet.newBuilder(NATURAL).add(null)).isInstanceOf(NullPointerException.class)
                .hasMessage("TreeSet.Builder.add: element is null");
        assertThatThrownBy(() -> TreeSet.newBuilder(NATURAL).addAll(null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> TreeSet.newBuilder(null)).isInstanceOf(NullPointerException.class);
        final TreeSet.Builder<Integer> builder = TreeSet.newBuilder(NATURAL);
        assertThatThrownBy(() -> builder.addAll(java.util.Arrays.asList(3, 1, null, 2))).isInstanceOf(NullPointerException.class);
        assertThat(javaList(builder.result())).containsExactly(1, 3);
    }

    @Test
    public void shouldRefuseTheTreeSetBuilderAfterItsComparatorThrew() {
        final Comparator<Object> failing = (a, b) -> {
            throw new ClassCastException("not comparable");
        };
        final TreeSet.Builder<Object> builder = TreeSet.newBuilder(failing).add("a");
        // one element is never compared
        builder.add(new Object());
        assertThatThrownBy(builder::result).isInstanceOf(ClassCastException.class);
        assertThatThrownBy(() -> builder.add("b")).isInstanceOf(IllegalStateException.class);
        assertThat(javaList(TreeSet.newBuilder(failing).add("only").result())).containsExactly("only");
    }

    @Test
    public void shouldCollectATreeSetInParallel() {
        final java.util.List<Integer> input = new ArrayList<>();
        final Random random = new Random(SEED);
        for (int i = 0; i < 20_000; i++) {
            input.add(random.nextInt(5000));
        }
        final TreeSet<Integer> expected = insertedSet(NATURAL, input);
        assertThat(input.stream().collect(TreeSet.collector())).isEqualTo(expected);
        assertThat(input.parallelStream().collect(TreeSet.collector())).isEqualTo(expected);
        assertThat(javaList(input.parallelStream().collect(TreeSet.collector(REVERSED))))
                .isEqualTo(javaList(insertedSet(REVERSED, input)));
        // the later of equal elements in encounter order wins, as sequentially
        final java.util.List<String> strings = new ArrayList<>();
        for (int i = 0; i < 10_000; i++) {
            strings.add((i % 2 == 0 ? "k" : "K") + (i % 100));
        }
        final TreeSet<String> sequential = insertedSet(String.CASE_INSENSITIVE_ORDER, strings);
        assertThat(javaList(strings.parallelStream().collect(TreeSet.collector(String.CASE_INSENSITIVE_ORDER))))
                .isEqualTo(javaList(sequential));
    }

    // -- TreeMap

    @Test
    public void shouldBuildTheTreeMapOfOfEntriesAtEveryBoundary() {
        final Random random = new Random(SEED);
        for (int size : SIZES) {
            for (java.util.List<Integer> keys : java.util.List.of(IntStream.range(0, size).boxed().toList(), shuffled(size, random))) {
                final java.util.List<Tuple2<Integer, String>> entries = keys.stream().map(k -> Tuple.of(k, "v" + k)).toList();
                final TreeMap<Integer, String> built = TreeMap.<Integer, String> newBuilder().putAll(entries).result();
                assertThat(built).isEqualTo(insertedMap(NATURAL, entries));
                assertThat(TreeMap.ofEntries(entries)).isEqualTo(built);
                assertThat(javaList(built.keySet())).isEqualTo(IntStream.range(0, size).boxed().toList());
                assertThat(built.size()).isEqualTo(size);
            }
        }
    }

    @Test
    public void shouldMatchTheJdkTreeMapOnRandomInputs() {
        final Random random = new Random(SEED);
        for (Comparator<Integer> order : java.util.List.of(NATURAL, REVERSED)) {
            for (int round = 0; round < 200; round++) {
                final int size = random.nextInt(2000);
                final int range = 1 + random.nextInt(Math.max(1, size * 2));
                final TreeMap.Builder<Integer, Integer> builder = TreeMap.newBuilder(order);
                final java.util.TreeMap<Integer, Integer> oracle = new java.util.TreeMap<>(order);
                final java.util.List<Tuple2<Integer, Integer>> input = new ArrayList<>();
                for (int i = 0; i < size; i++) {
                    final int key = random.nextInt(range);
                    input.add(Tuple.of(key, i));
                    oracle.put(key, i);
                    switch (random.nextInt(3)) {
                        case 0 -> builder.put(key, i);
                        case 1 -> builder.put(Tuple.of(key, i));
                        default -> builder.putAll(java.util.List.of(Tuple.of(key, i)));
                    }
                }
                assertThat(builder.size()).isEqualTo(oracle.size());
                final TreeMap<Integer, Integer> built = builder.result();
                final java.util.Map<Integer, Integer> actual = new java.util.HashMap<>();
                built.forEach(t -> actual.put(t._1(), t._2()));
                assertThat(actual).isEqualTo(oracle);
                assertThat(javaList(built.keySet())).isEqualTo(new ArrayList<>(oracle.keySet()));
                assertThat(built).isEqualTo(insertedMap(order, input));
                assertThat(built.comparator()).isSameAs(order);
            }
        }
    }

    @Test
    public void shouldKeepTheLastEntryOfEqualKeysAsOfEntriesDoes() {
        final java.util.List<Tuple2<String, Integer>> input = java.util.List.of(Tuple.of("b", 1), Tuple.of("A", 2),
                Tuple.of("a", 3), Tuple.of("B", 4), Tuple.of("c", 5));
        final TreeMap<String, Integer> built = TreeMap.<String, Integer> newBuilder(String.CASE_INSENSITIVE_ORDER).putAll(input).result();
        assertThat(javaList(built)).containsExactly(Tuple.of("a", 3), Tuple.of("B", 4), Tuple.of("c", 5));
        assertThat(javaList(built)).isEqualTo(javaList(insertedMap(String.CASE_INSENSITIVE_ORDER, input)));
        assertThat(javaList(TreeMap.ofEntries(String.CASE_INSENSITIVE_ORDER, input))).isEqualTo(javaList(built));
    }

    @Test
    public void shouldStoreTheGivenEntries() {
        final Tuple2<Integer, String> entry = Tuple.of(1, "one");
        final TreeMap<Integer, String> built = TreeMap.<Integer, String> newBuilder().put(entry).result();
        assertThat(built.head()).isSameAs(entry);
    }

    @Test
    public void shouldBuildEmptyTreeMapWithItsComparator() {
        final TreeMap<Integer, String> built = TreeMap.<Integer, String> newBuilder(REVERSED).result();
        assertThat(built).isEmpty();
        assertThat(built.comparator()).isSameAs(REVERSED);
        assertThat(javaList(built.put(1, "a").put(2, "b").keySet())).containsExactly(2, 1);
        assertThat(TreeMap.<Integer, String> newBuilder().result()).isEqualTo(TreeMap.empty());
    }

    @Test
    public void shouldBuildATreeMapThatSupportsTheTreeOperations() {
        final TreeMap.Builder<Integer, Integer> builder = TreeMap.newBuilder();
        for (Integer key : shuffled(1025, new Random(SEED))) {
            builder.put(key, -key);
        }
        final TreeMap<Integer, Integer> built = builder.result();
        assertThat(built.get(512).get()).isEqualTo(-512);
        assertThat(built.remove(512).put(2000, 0).size()).isEqualTo(1025);
        assertThat(javaList(built.take(3))).containsExactly(Tuple.of(0, 0), Tuple.of(1, -1), Tuple.of(2, -2));
        assertThat(javaList(built.drop(1024))).containsExactly(Tuple.of(1024, -1024));
        assertThat(TreeMap.ofAll(built.comparator(), built.asJavaMap())).isSameAs(built);
    }

    @Test
    public void shouldRefuseTreeMapBuilderUseAfterResult() {
        final TreeMap.Builder<Integer, String> builder = TreeMap.<Integer, String> newBuilder().put(1, "a");
        final TreeMap<Integer, String> built = builder.result();
        assertThatThrownBy(() -> builder.put(2, "b")).isInstanceOf(IllegalStateException.class)
                .hasMessage("result() has already been called on this TreeMap.Builder");
        assertThatThrownBy(() -> builder.put(null, null)).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> builder.put(Tuple.of(2, "b"))).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> builder.put(null)).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> builder.putAll(java.util.List.of(Tuple.of(3, "c")))).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(builder::size).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(builder::result).isInstanceOf(IllegalStateException.class);
        assertThat(javaList(built)).containsExactly(Tuple.of(1, "a"));
    }

    @Test
    public void shouldRejectNullsInTheTreeMapBuilderAndKeepWhatCameBefore() {
        assertThatThrownBy(() -> TreeMap.<Integer, String> newBuilder().put(null, "a")).isInstanceOf(NullPointerException.class)
                .hasMessage("TreeMap: key is null");
        assertThatThrownBy(() -> TreeMap.<Integer, String> newBuilder().put(1, null)).isInstanceOf(NullPointerException.class)
                .hasMessage("TreeMap: value is null");
        assertThatThrownBy(() -> TreeMap.<Integer, String> newBuilder().put(null)).isInstanceOf(NullPointerException.class)
                .hasMessage("TreeMap.Builder.put: entry is null");
        assertThatThrownBy(() -> TreeMap.<Integer, String> newBuilder().put(Tuple.of(null, "a"))).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> TreeMap.<Integer, String> newBuilder().put(Tuple.of(1, null))).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> TreeMap.<Integer, String> newBuilder().putAll(null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> TreeMap.newBuilder(null)).isInstanceOf(NullPointerException.class);
        final TreeMap.Builder<Integer, String> builder = TreeMap.newBuilder();
        assertThatThrownBy(() -> builder.putAll(java.util.Arrays.asList(Tuple.of(2, "b"), Tuple.of(1, "a"), Tuple.of(3, null))))
                .isInstanceOf(NullPointerException.class);
        assertThat(javaList(builder.result())).containsExactly(Tuple.of(1, "a"), Tuple.of(2, "b"));
    }

    @Test
    public void shouldCollectATreeMapInParallel() {
        final java.util.List<Tuple2<Integer, Integer>> input = new ArrayList<>();
        final Random random = new Random(SEED);
        for (int i = 0; i < 20_000; i++) {
            input.add(Tuple.of(random.nextInt(5000), i));
        }
        // the later of equal keys in encounter order wins, as sequentially
        final TreeMap<Integer, Integer> expected = insertedMap(NATURAL, input);
        assertThat(input.stream().collect(TreeMap.collector())).isEqualTo(expected);
        assertThat(input.parallelStream().collect(TreeMap.collector())).isEqualTo(expected);
        assertThat(javaList(input.parallelStream().collect(TreeMap.<Integer, Integer> collector(REVERSED))))
                .isEqualTo(javaList(insertedMap(REVERSED, input)));
        assertThat(input.parallelStream().collect(TreeMap.<Integer, Integer, Tuple2<Integer, Integer>> collector(Tuple2::_1, Tuple2::_2)))
                .isEqualTo(expected);
    }
}
