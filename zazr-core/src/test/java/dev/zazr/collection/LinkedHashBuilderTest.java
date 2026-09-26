package dev.zazr.collection;

import dev.zazr.Tuple;
import dev.zazr.Tuple2;
import java.util.ArrayList;
import java.util.Random;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/// LinkedHashSet.Builder and LinkedHashMap.Builder: boundaries, oracles, adoption, reuse and nulls. Which of equal keys
/// or elements is kept, object by object, is checked against successive puts and adds by LinkedHashRepeatedKeyTest.
public class LinkedHashBuilderTest {

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

    private static <T> LinkedHashSet<T> adds(Iterable<T> elements) {
        LinkedHashSet<T> set = LinkedHashSet.empty();
        for (T element : elements) {
            set = set.add(element);
        }
        return set;
    }

    private static <K, V> LinkedHashMap<K, V> puts(Iterable<Tuple2<K, V>> entries) {
        LinkedHashMap<K, V> map = LinkedHashMap.empty();
        for (Tuple2<K, V> entry : entries) {
            map = map.put(entry._1(), entry._2());
        }
        return map;
    }

    // -- LinkedHashSet

    @Test
    public void shouldBuildTheLinkedHashSetOfSuccessiveAddsAtEveryBoundary() {
        for (int size : SIZES) {
            final java.util.List<Integer> input = IntStream.range(0, size).map(i -> size - 1 - i).boxed().toList();
            final LinkedHashSet.Builder<Integer> builder = LinkedHashSet.newBuilder();
            for (Integer element : input) {
                builder.add(element);
            }
            assertThat(builder.size()).isEqualTo(size);
            final LinkedHashSet<Integer> built = builder.result();
            assertThat(javaList(built)).isEqualTo(input);
            assertThat(built).isEqualTo(adds(input));
            assertThat(javaList(LinkedHashSet.ofAll(input))).isEqualTo(input);
            assertThat(built.size()).isEqualTo(size);
            if (size > 0) {
                assertThat(built.head()).isEqualTo(input.get(0));
                assertThat(built.last()).isEqualTo(input.get(size - 1));
            }
        }
        assertThat(LinkedHashSet.newBuilder().result()).isSameAs(LinkedHashSet.empty());
        assertThat(LinkedHashSet.newBuilder().addAll(java.util.List.of()).result()).isSameAs(LinkedHashSet.empty());
    }

    @Test
    public void shouldMatchTheJdkLinkedHashSetOnRandomInputsWithCollisions() {
        final Random random = new Random(SEED);
        for (int round = 0; round < 200; round++) {
            final int size = random.nextInt(3000);
            final int hashRange = 1 + random.nextInt(round % 2 == 0 ? 64 : Integer.MAX_VALUE);
            final LinkedHashSet.Builder<Key> builder = LinkedHashSet.newBuilder();
            final java.util.LinkedHashSet<Key> oracle = new java.util.LinkedHashSet<>();
            for (int i = 0; i < size; i++) {
                final Key key = new Key(random.nextInt(hashRange) - hashRange / 2, random.nextInt(3));
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
            assertThat(javaList(builder.result())).isEqualTo(new ArrayList<>(oracle));
        }
    }

    @Test
    public void shouldReturnAnAdoptedLinkedHashSetAsItIs() {
        for (int size : SIZES) {
            final LinkedHashSet<Integer> source = LinkedHashSet.ofAll(IntStream.range(0, size).boxed().toList());
            final LinkedHashSet.Builder<Integer> builder = LinkedHashSet.<Integer> newBuilder().addAll(source);
            assertThat(builder.size()).isEqualTo(size);
            // equal elements add nothing, so the set is still the one adopted
            builder.addAll(IntStream.range(0, size).boxed().toList());
            if (size == 0) {
                assertThat(builder.result()).isSameAs(LinkedHashSet.empty());
            } else {
                assertThat(builder.result()).isSameAs(source);
                assertThat(LinkedHashSet.<Integer> newBuilder().addAll(source.asJava()).result()).isSameAs(source);
            }
        }
    }

    @Test
    public void shouldAdoptALinkedHashSetAndNeverChangeIt() {
        final Random random = new Random(SEED);
        for (int size : SIZES) {
            final LinkedHashSet<Integer> source = LinkedHashSet.ofAll(IntStream.range(0, size).boxed().toList());
            final java.util.List<Integer> sourceBefore = javaList(source);
            final LinkedHashSet.Builder<Integer> builder = LinkedHashSet.<Integer> newBuilder().addAll(source);
            final java.util.LinkedHashSet<Integer> oracle = new java.util.LinkedHashSet<>(sourceBefore);
            for (int i = 0; i < 100; i++) {
                final int element = random.nextInt(2 * size + 10);
                builder.add(element);
                oracle.add(element);
            }
            final LinkedHashSet<Integer> built = builder.result();
            assertThat(javaList(source)).isEqualTo(sourceBefore);
            assertThat(source.size()).isEqualTo(size);
            assertThat(javaList(built)).isEqualTo(new ArrayList<>(oracle));
        }
    }

    @Test
    public void shouldBuildALinkedHashSetThatSupportsTheSetOperations() {
        final LinkedHashSet<Integer> built = LinkedHashSet.<Integer> newBuilder().addAll(IntStream.range(0, 1025).boxed().toList()).result();
        final LinkedHashSet<Integer> changed = built.remove(3).add(5000).removeAll(java.util.List.of(0, 1, 2));
        assertThat(changed.size()).isEqualTo(1022);
        assertThat(changed.head()).isEqualTo(4);
        assertThat(changed.last()).isEqualTo(5000);
        assertThat(built.size()).isEqualTo(1025);
        assertThat(built.contains(3)).isTrue();
        assertThat(built.tail().head()).isEqualTo(1);
        assertThat(built.drop(1024).head()).isEqualTo(1024);
        assertThat(LinkedHashSet.ofAll(built)).isSameAs(built);
    }

    @Test
    public void shouldRefuseLinkedHashSetBuilderUseAfterResult() {
        final LinkedHashSet.Builder<Integer> builder = LinkedHashSet.<Integer> newBuilder().add(1);
        final LinkedHashSet<Integer> built = builder.result();
        assertThatThrownBy(() -> builder.add(2)).isInstanceOf(IllegalStateException.class)
                .hasMessage("result() has already been called on this LinkedHashSet.Builder");
        assertThatThrownBy(() -> builder.add(null)).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> builder.addAll(java.util.List.of(3))).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> builder.addAll(LinkedHashSet.of(3))).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> builder.addAll(null)).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(builder::size).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(builder::result).isInstanceOf(IllegalStateException.class);
        assertThat(javaList(built)).containsExactly(1);
        // an adopting builder is closed too
        final LinkedHashSet.Builder<Integer> adopting = LinkedHashSet.<Integer> newBuilder().addAll(LinkedHashSet.of(1, 2));
        adopting.result();
        assertThatThrownBy(() -> adopting.add(3)).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(adopting::size).isInstanceOf(IllegalStateException.class);
    }

    @Test
    public void shouldRejectNullsInTheLinkedHashSetBuilderAndKeepWhatCameBefore() {
        assertThatThrownBy(() -> LinkedHashSet.newBuilder().add(null)).isInstanceOf(NullPointerException.class)
                .hasMessage("LinkedHashSet.Builder.add: element is null");
        assertThatThrownBy(() -> LinkedHashSet.newBuilder().addAll(null)).isInstanceOf(NullPointerException.class)
                .hasMessage("elements is null");
        final LinkedHashSet.Builder<Integer> builder = LinkedHashSet.newBuilder();
        assertThatThrownBy(() -> builder.addAll(java.util.Arrays.asList(3, 1, null, 2))).isInstanceOf(NullPointerException.class);
        assertThat(javaList(builder.result())).containsExactly(3, 1);
    }

    @Test
    public void shouldCollectALinkedHashSetInParallel() {
        final java.util.List<Integer> input = new ArrayList<>();
        final Random random = new Random(SEED);
        for (int i = 0; i < 20_000; i++) {
            input.add(random.nextInt(5000));
        }
        final java.util.List<Integer> expected = new ArrayList<>(new java.util.LinkedHashSet<>(input));
        assertThat(javaList(input.stream().collect(LinkedHashSet.collector()))).isEqualTo(expected);
        assertThat(javaList(input.parallelStream().collect(LinkedHashSet.collector()))).isEqualTo(expected);
    }

    // -- LinkedHashMap

    @Test
    public void shouldBuildTheLinkedHashMapOfSuccessivePutsAtEveryBoundary() {
        for (int size : SIZES) {
            final java.util.List<Tuple2<Integer, String>> entries = IntStream.range(0, size).map(i -> size - 1 - i)
                    .mapToObj(k -> Tuple.of(k, "v" + k)).toList();
            final LinkedHashMap.Builder<Integer, String> builder = LinkedHashMap.newBuilder();
            for (Tuple2<Integer, String> entry : entries) {
                builder.put(entry._1(), entry._2());
            }
            assertThat(builder.size()).isEqualTo(size);
            final LinkedHashMap<Integer, String> built = builder.result();
            assertThat(javaList(built)).isEqualTo(entries);
            assertThat(built).isEqualTo(puts(entries));
            assertThat(javaList(LinkedHashMap.ofEntries(entries))).isEqualTo(entries);
            assertThat(built.size()).isEqualTo(size);
            if (size > 0) {
                assertThat(built.head()).isEqualTo(entries.get(0));
                assertThat(built.last()).isEqualTo(entries.get(size - 1));
                assertThat(built.get(0).get()).isEqualTo("v0");
            }
        }
        assertThat(LinkedHashMap.newBuilder().result()).isSameAs(LinkedHashMap.empty());
        assertThat(LinkedHashMap.newBuilder().putAll(java.util.List.of()).result()).isSameAs(LinkedHashMap.empty());
    }

    @Test
    public void shouldMatchTheJdkLinkedHashMapOnRandomInputsWithCollisions() {
        final Random random = new Random(SEED);
        for (int round = 0; round < 200; round++) {
            final int size = random.nextInt(3000);
            final int hashRange = 1 + random.nextInt(round % 2 == 0 ? 64 : Integer.MAX_VALUE);
            final LinkedHashMap.Builder<Key, Integer> builder = LinkedHashMap.newBuilder();
            // java.util.LinkedHashMap keeps the first position of a key and takes the last value, as put does
            final java.util.LinkedHashMap<Key, Integer> oracle = new java.util.LinkedHashMap<>();
            for (int i = 0; i < size; i++) {
                final Key key = new Key(random.nextInt(hashRange) - hashRange / 2, random.nextInt(3));
                oracle.put(key, i);
                switch (random.nextInt(3)) {
                    case 0 -> builder.put(key, i);
                    case 1 -> builder.put(Tuple.of(key, i));
                    default -> builder.putAll(java.util.List.of(Tuple.of(key, i)));
                }
            }
            assertThat(builder.size()).isEqualTo(oracle.size());
            final java.util.List<Tuple2<Key, Integer>> expected = new ArrayList<>();
            oracle.forEach((key, value) -> expected.add(Tuple.of(key, value)));
            assertThat(javaList(builder.result())).isEqualTo(expected);
        }
    }

    @Test
    public void shouldReturnAnAdoptedLinkedHashMapAsItIs() {
        for (int size : SIZES) {
            final LinkedHashMap<Integer, Integer> source = LinkedHashMap.ofEntries(IntStream.range(0, size).mapToObj(k -> Tuple.of(k, k)).toList());
            final LinkedHashMap.Builder<Integer, Integer> builder = LinkedHashMap.<Integer, Integer> newBuilder().putAll(source);
            assertThat(builder.size()).isEqualTo(size);
            if (size == 0) {
                assertThat(builder.result()).isSameAs(LinkedHashMap.empty());
            } else {
                assertThat(builder.result()).isSameAs(source);
                assertThat(LinkedHashMap.<Integer, Integer> newBuilder().putAll(source.asJava()).result()).isSameAs(source);
            }
        }
    }

    @Test
    public void shouldAdoptALinkedHashMapAndNeverChangeIt() {
        final Random random = new Random(SEED);
        for (int size : SIZES) {
            final LinkedHashMap<Integer, Integer> source = LinkedHashMap.ofEntries(IntStream.range(0, size).mapToObj(k -> Tuple.of(k, k)).toList());
            final java.util.List<Tuple2<Integer, Integer>> sourceBefore = javaList(source);
            final LinkedHashMap.Builder<Integer, Integer> builder = LinkedHashMap.<Integer, Integer> newBuilder().putAll(source);
            final java.util.LinkedHashMap<Integer, Integer> oracle = new java.util.LinkedHashMap<>();
            sourceBefore.forEach(entry -> oracle.put(entry._1(), entry._2()));
            for (int i = 0; i < 100; i++) {
                final int key = random.nextInt(2 * size + 10);
                builder.put(key, -i);
                oracle.put(key, -i);
            }
            final LinkedHashMap<Integer, Integer> built = builder.result();
            assertThat(javaList(source)).isEqualTo(sourceBefore);
            assertThat(source.size()).isEqualTo(size);
            final java.util.List<Tuple2<Integer, Integer>> expected = new ArrayList<>();
            oracle.forEach((key, value) -> expected.add(Tuple.of(key, value)));
            assertThat(javaList(built)).isEqualTo(expected);
        }
    }

    @Test
    public void shouldBuildALinkedHashMapThatSupportsTheMapOperations() {
        final LinkedHashMap.Builder<Integer, Integer> builder = LinkedHashMap.newBuilder();
        for (int i = 0; i < 1025; i++) {
            builder.put(i, -i);
        }
        final LinkedHashMap<Integer, Integer> built = builder.result();
        final LinkedHashMap<Integer, Integer> changed = built.remove(3).put(5000, 0).put(4, 4);
        assertThat(changed.size()).isEqualTo(1025);
        assertThat(changed.get(4).get()).isEqualTo(4);
        assertThat(changed.last()).isEqualTo(Tuple.of(5000, 0));
        assertThat(built.get(4).get()).isEqualTo(-4);
        assertThat(built.containsKey(3)).isTrue();
        assertThat(built.containsKey(5000)).isFalse();
        assertThat(built.drop(1024).head()).isEqualTo(Tuple.of(1024, -1024));
        assertThat(LinkedHashMap.ofAll(built.asJavaMap())).isSameAs(built);
    }

    @Test
    public void shouldRefuseLinkedHashMapBuilderUseAfterResult() {
        final LinkedHashMap.Builder<Integer, String> builder = LinkedHashMap.<Integer, String> newBuilder().put(1, "a");
        final LinkedHashMap<Integer, String> built = builder.result();
        assertThatThrownBy(() -> builder.put(2, "b")).isInstanceOf(IllegalStateException.class)
                .hasMessage("result() has already been called on this LinkedHashMap.Builder");
        assertThatThrownBy(() -> builder.put(null, null)).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> builder.put(Tuple.of(2, "b"))).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> builder.put(null)).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> builder.putAll(java.util.List.of(Tuple.of(3, "c")))).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> builder.putAll(LinkedHashMap.of(3, "c"))).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> builder.putAll(null)).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(builder::size).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(builder::result).isInstanceOf(IllegalStateException.class);
        assertThat(javaList(built)).containsExactly(Tuple.of(1, "a"));
        final LinkedHashMap.Builder<Integer, String> adopting = LinkedHashMap.<Integer, String> newBuilder().putAll(LinkedHashMap.of(1, "a"));
        adopting.result();
        assertThatThrownBy(() -> adopting.put(2, "b")).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(adopting::size).isInstanceOf(IllegalStateException.class);
    }

    @Test
    public void shouldRejectNullsInTheLinkedHashMapBuilderAndKeepWhatCameBefore() {
        assertThatThrownBy(() -> LinkedHashMap.<Integer, String> newBuilder().put(null, "a")).isInstanceOf(NullPointerException.class)
                .hasMessage("LinkedHashMap.Builder.put: key is null");
        assertThatThrownBy(() -> LinkedHashMap.<Integer, String> newBuilder().put(1, null)).isInstanceOf(NullPointerException.class)
                .hasMessage("LinkedHashMap.Builder.put: value is null");
        assertThatThrownBy(() -> LinkedHashMap.<Integer, String> newBuilder().put(null)).isInstanceOf(NullPointerException.class)
                .hasMessage("LinkedHashMap.Builder.put: entry is null");
        assertThatThrownBy(() -> LinkedHashMap.<Integer, String> newBuilder().put(Tuple.of(null, "a"))).isInstanceOf(NullPointerException.class)
                .hasMessage("LinkedHashMap.Builder.put: key is null");
        assertThatThrownBy(() -> LinkedHashMap.<Integer, String> newBuilder().put(Tuple.of(1, null))).isInstanceOf(NullPointerException.class)
                .hasMessage("LinkedHashMap.Builder.put: value is null");
        assertThatThrownBy(() -> LinkedHashMap.<Integer, String> newBuilder().putAll(null)).isInstanceOf(NullPointerException.class)
                .hasMessage("entries is null");
        final LinkedHashMap.Builder<Integer, String> builder = LinkedHashMap.newBuilder();
        assertThatThrownBy(() -> builder.putAll(java.util.Arrays.asList(Tuple.of(2, "b"), Tuple.of(1, "a"), Tuple.of(3, null))))
                .isInstanceOf(NullPointerException.class);
        assertThat(javaList(builder.result())).containsExactly(Tuple.of(2, "b"), Tuple.of(1, "a"));
    }

    @Test
    public void shouldCollectALinkedHashMapInParallel() {
        final java.util.List<Tuple2<Integer, Integer>> input = new ArrayList<>();
        final Random random = new Random(SEED);
        for (int i = 0; i < 20_000; i++) {
            input.add(Tuple.of(random.nextInt(5000), i));
        }
        final java.util.List<Tuple2<Integer, Integer>> expected = javaList(puts(input));
        assertThat(javaList(input.stream().collect(LinkedHashMap.collector()))).isEqualTo(expected);
        assertThat(javaList(input.parallelStream().collect(LinkedHashMap.collector()))).isEqualTo(expected);
        assertThat(javaList(input.parallelStream().collect(LinkedHashMap.<Integer, Integer, Tuple2<Integer, Integer>> collector(Tuple2::_1, Tuple2::_2))))
                .isEqualTo(expected);
    }

    @Test
    public void shouldKeepTheFactoryNullMessagesOnTheBuilderPaths() {
        final java.util.List<Integer> withNull = java.util.Arrays.asList(1, null);
        assertThatThrownBy(() -> LinkedHashSet.of(1, null)).isInstanceOf(NullPointerException.class)
                .hasMessage("LinkedHashSet.of: element is null");
        assertThatThrownBy(() -> LinkedHashSet.ofAll(withNull)).isInstanceOf(NullPointerException.class)
                .hasMessage("LinkedHashSet: element is null");
        assertThatThrownBy(() -> LinkedHashSet.flatten(java.util.List.of(withNull))).isInstanceOf(NullPointerException.class)
                .hasMessage("LinkedHashSet: element is null");
        final java.util.Map<Integer, String> nullValue = new java.util.HashMap<>();
        nullValue.put(1, null);
        assertThatThrownBy(() -> LinkedHashMap.ofAll(nullValue)).isInstanceOf(NullPointerException.class)
                .hasMessage("LinkedHashMap: value is null");
        final java.util.Map<Integer, String> nullKey = new java.util.HashMap<>();
        nullKey.put(null, "a");
        assertThatThrownBy(() -> LinkedHashMap.ofAll(nullKey)).isInstanceOf(NullPointerException.class)
                .hasMessage("LinkedHashMap: key is null");
        assertThatThrownBy(() -> LinkedHashMap.ofEntries(java.util.Arrays.asList(Tuple.of(1, "a"), Tuple.of(2, null))))
                .isInstanceOf(NullPointerException.class).hasMessage("LinkedHashMap: value is null");
    }

    @Test
    public void shouldCopyAReversedViewInsteadOfAdoptingIt() {
        final LinkedHashSet<Integer> set = LinkedHashSet.of(1, 2, 3);
        assertThat(javaList(LinkedHashSet.<Integer> newBuilder().addAll(set.asJava().reversed()).result())).containsExactly(3, 2, 1);
        final LinkedHashMap<Integer, String> map = LinkedHashMap.of(1, "a", 2, "b");
        final java.util.List<Tuple2<Integer, String>> reversed = new ArrayList<>();
        map.asJavaMap().reversed().forEach((key, value) -> reversed.add(Tuple.of(key, value)));
        assertThat(javaList(LinkedHashMap.<Integer, String> newBuilder().putAll(reversed).result()))
                .containsExactly(Tuple.of(2, "b"), Tuple.of(1, "a"));
    }
}
