package com.guizmaii.zazr.collection;

import com.guizmaii.zazr.Tuple;
import com.guizmaii.zazr.Tuple2;
import com.guizmaii.zazr.collection.internal.Maps;
import com.guizmaii.zazr.control.Option;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.NoSuchElementException;
import java.util.Spliterator;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Collector;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.IterableAssert;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static java.util.Arrays.asList;
import static java.util.Comparator.comparingInt;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** The HashMap API: the shared cases over the values (through {@code IntMap}), the map cases over the entries, and the hash-specific ones. */
public class HashMapTest extends AbstractTraversableTest {

    @Override
    protected <T> IterableAssert<T> assertThat(Iterable<T> actual) {
        return new IterableAssert<T>(actual) {
            @Override
            public IterableAssert<T> isEqualTo(Object obj) {
                @SuppressWarnings("unchecked")
                final Iterable<T> expected = (Iterable<T>) obj;
                final java.util.Map<T, Integer> actualMap = countMap(actual);
                final java.util.Map<T, Integer> expectedMap = countMap(expected);
                HashMapTest.super.assertThat(actualMap.size()).isEqualTo(expectedMap.size());
                actualMap.forEach((k, v) -> HashMapTest.super.assertThat(v).isEqualTo(expectedMap.get(k)));
                return this;
            }

            private java.util.Map<T, Integer> countMap(Iterable<? extends T> it) {
                final java.util.HashMap<T, Integer> cnt = new java.util.HashMap<>();
                it.forEach(i -> cnt.merge(i, 1, (v1, v2) -> v1 + v2));
                return cnt;
            }
        };
    }

    @Override
    protected <T> Collector<T, ArrayList<T>, IntMap<T>> collector() {
        final Collector<Tuple2<Integer, T>, ?, ? extends Map<Integer, T>> mapCollector = mapCollector();
        return new Collector<T, ArrayList<T>, IntMap<T>>() {
            @Override
            public Supplier<ArrayList<T>> supplier() {
                return ArrayList::new;
            }

            @Override
            public BiConsumer<ArrayList<T>, T> accumulator() {
                return ArrayList::add;
            }

            @Override
            public BinaryOperator<ArrayList<T>> combiner() {
                return (left, right) -> {
                    left.addAll(right);
                    return left;
                };
            }

            @Override
            public Function<ArrayList<T>, IntMap<T>> finisher() {
                return HashMapTest.this::ofAll;
            }

            @Override
            public java.util.Set<Characteristics> characteristics() {
                return mapCollector.characteristics();
            }
        };
    }

    @Override
    protected <T> IntMap<T> empty() {
        return IntMap.of(emptyMap());
    }

    @Override
    protected boolean emptyShouldBeSingleton() {
        return false;
    }

    private <T> Map<Integer, T> emptyInt() {
        return emptyMap();
    }

    protected Map<Integer, Integer> emptyIntInt() {
        return emptyMap();
    }

    private Map<Integer, String> emptyIntString() {
        return emptyMap();
    }

    protected boolean emptyMapShouldBeSingleton() {
        return true;
    }

    @Override
    protected <T> IntMap<T> of(T element) {
        Map<Integer, T> map = emptyMap();
        map = map.put(0, element);
        return IntMap.of(map);
    }

    @SuppressWarnings("unchecked")
    @Override
    protected <T> IntMap<T> of(T... elements) {
        Map<Integer, T> map = emptyMap();
        for (T element : elements) {
            map = map.put(map.size(), element);
        }
        return IntMap.of(map);
    }

    @Override
    protected <T> IntMap<T> ofAll(Iterable<? extends T> elements) {
        Map<Integer, T> map = emptyMap();
        for (T element : elements) {
            map = map.put(map.size(), element);
        }
        return IntMap.of(map);
    }

    @Override
    protected <T extends Comparable<? super T>> IntMap<T> ofJavaStream(java.util.stream.Stream<? extends T> javaStream) {
        return ofAll(Vector.ofAll(javaStream));
    }

    @Override
    protected IntMap<Boolean> ofAll(boolean... elements) {
        return ofAll(Vector.ofAll(elements));
    }

    @Override
    protected IntMap<Byte> ofAll(byte... elements) {
        return ofAll(Vector.ofAll(elements));
    }

    @Override
    protected IntMap<Character> ofAll(char... elements) {
        return ofAll(Vector.ofAll(elements));
    }

    @Override
    protected IntMap<Double> ofAll(double... elements) {
        return ofAll(Vector.ofAll(elements));
    }

    @Override
    protected IntMap<Float> ofAll(float... elements) {
        return ofAll(Vector.ofAll(elements));
    }

    @Override
    protected IntMap<Integer> ofAll(int... elements) {
        return ofAll(Vector.ofAll(elements));
    }

    @Override
    protected IntMap<Long> ofAll(long... elements) {
        return ofAll(Vector.ofAll(elements));
    }

    @Override
    protected IntMap<Short> ofAll(short... elements) {
        return ofAll(Vector.ofAll(elements));
    }

    @Override
    protected <T> IntMap<T> tabulate(int n, Function<? super Integer, ? extends T> f) {
        Map<Integer, T> map = emptyMap();
        for (int i = 0; i < n; i++) {
            map = map.put(map.size(), f.apply(i));
        }
        return IntMap.of(map);
    }

    @Override
    protected <T> IntMap<T> fill(int n, Supplier<? extends T> s) {
        return tabulate(n, anything -> s.get());
    }
    // the type name a map built by mapOf(...) prints; empty() is an IntMap wrapper, whose name differs
    private String mapPrefix() {
        final String empty = emptyMap().toString();
        return empty.substring(0, empty.length() - "()".length());
    }

    // -- narrow

    @Test
    public void shouldNarrowMap() {
        final Map<Integer, Double> int2doubleMap = mapOf(1, 1.0d);
        final Map<Number, Number> number2numberMap = Map.narrow(int2doubleMap);
        final int actual = number2numberMap.put(new BigDecimal("2"), new BigDecimal("2.0")).values().sum().intValue();
        assertThat(actual).isEqualTo(3);
    }

    @Nested
    class MappersCollectorTests {
        @Test
        public void shouldCollectWithKeyMapper() {
            Map<Integer, Integer> map = java.util.stream.Stream.of(1, 2, 3).collect(collectorWithMapper(i -> i * 2));
            assertThat(map).isEqualTo(mapOf(2, 1, 4, 2, 6, 3));
        }

        @Test
        public void shouldCollectWithKeyValueMappers() {
            Map<Integer, String> map = java.util.stream.Stream.of(1, 2, 3).collect(collectorWithMappers(i -> i * 2, String::valueOf));
            assertThat(map).isEqualTo(mapOf(2, "1", 4, "2", 6, "3"));
        }
    }

    // -- construction

    @Test
    public void shouldBeTheSame() {
        assertThat(mapOf(1, 2)).isEqualTo(emptyInt().put(1, 2));
    }

    protected static java.util.Map.Entry<Integer, String> asJavaEntry(int key, String value) {
        return new java.util.AbstractMap.SimpleEntry<>(key, value);
    }

    @SafeVarargs
    protected final <K, V> java.util.Map<K, V> asJavaMap(java.util.Map.Entry<K, V>... entries) {
        final java.util.Map<K, V> results = javaEmptyMap();
        for (java.util.Map.Entry<K, V> entry : entries) {
            results.put(entry.getKey(), entry.getValue());
        }
        return results;
    }

    @Test
    public void shouldConstructFromJavaStream() {
        final java.util.stream.Stream<Integer> javaStream = java.util.stream.Stream.of(1, 2, 3);
        final Map<String, Integer> map = mapOf(javaStream, String::valueOf, Function.identity());
        assertThat(map).isEqualTo(this.<String, Integer> emptyMap().put("1", 1).put("2", 2).put("3", 3));
    }

    @Test
    public void shouldConstructFromJavaStreamWithDuplicatedKeys() {
        assertThat(mapOf(Stream.range(0, 4).stream()
                , i -> Math.max(1, Math.min(i, 2))
                , i -> String.valueOf(i + 1)
        ))
                .hasSize(2)
                .isEqualTo(mapOf(1, "2", 2, "4"));
    }

    @Test
    public void shouldConstructFromJavaStreamEntries() {
        final java.util.stream.Stream<Integer> javaStream = java.util.stream.Stream.of(1, 2, 3);
        final Map<String, Integer> map = mapOf(javaStream, i -> Tuple.of(String.valueOf(i), i));
        assertThat(map).isEqualTo(this.<String, Integer> emptyMap().put("1", 1).put("2", 2).put("3", 3));
    }

    @Test
    public void shouldConstructFromJavaStreamEntriesWithDuplicatedKeys() {
        assertThat(mapOf(Stream.range(0, 4).stream(), i ->
                Map.entry(Math.max(1, Math.min(i, 2)), String.valueOf(i + 1))
        ))
                .hasSize(2)
                .isEqualTo(mapOf(1, "2", 2, "4"));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldConstructFromUtilEntries() {
        final Map<Integer, String> actual = mapOfEntries(asJavaEntry(1, "1"), asJavaEntry(2, "2"), asJavaEntry(3, "3"));
        final Map<Integer, String> expected = this.<Integer, String>emptyMap().put(1, "1").put(2, "2").put(3, "3");
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldConstructFromUtilEntriesWithDuplicatedKeys() {
        assertThat(mapOfEntries(
                asJavaEntry(1, "1"), asJavaEntry(1, "2"),
                asJavaEntry(2, "3"), asJavaEntry(2, "4")
        ))
                .hasSize(2)
                .isEqualTo(mapOf(1, "2", 2, "4"));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldConstructFromEntriesVararg() {
        final Map<String, Integer> actual = mapOfTuples(Map.entry("1", 1), Map.entry("2", 2), Map.entry("3", 3));
        final Map<String, Integer> expected = this.<String, Integer>emptyMap().put("1", 1).put("2", 2).put("3", 3);
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldConstructFromEntriesVarargWithDuplicatedKeys() {
        assertThat(mapOfTuples(
                Map.entry(1, "1"), Map.entry(1, "2"),
                Map.entry(2, "3"), Map.entry(2, "4")
        ))
                .hasSize(2)
                .isEqualTo(mapOf(1, "2", 2, "4"));
    }

    @Test
    public void shouldConstructFromEntriesIterable() {
        final Map<String, Integer> actual = mapOfTuples(asList(Map.entry("1", 1), Map.entry("2", 2), Map.entry("3", 3)));
        final Map<String, Integer> expected = this.<String, Integer>emptyMap().put("1", 1).put("2", 2).put("3", 3);
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void shouldConstructFromEntriesIterableWithDuplicatedKeys() {
        assertThat(mapOfTuples(asList(
                Map.entry(1, "1"), Map.entry(1, "2"),
                Map.entry(2, "3"), Map.entry(2, "4")
        )))
                .hasSize(2)
                .isEqualTo(mapOf(1, "2", 2, "4"));
    }

    @Test
    public void shouldConstructFromPairs() {
        final Map<String, Integer> actual = mapOf("1", 1, "2", 2, "3", 3);
        final Map<String, Integer> expected = this.<String, Integer>emptyMap().put("1", 1).put("2", 2).put("3", 3);
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void shouldConstructFromPairsWithDuplicatedKeys() {
        final Map<Integer, String> actual = mapOf(1, "1", 1, "2", 2, "3");
        final Map<Integer, String> expected = this.<Integer, String>emptyMap().put(1, "2").put(2, "3");
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void shouldConstructWithTabulate() {
        final Map<String, Integer> actual = mapTabulate(4, i -> Tuple.of(i.toString(), i));
        final Map<String, Integer> expected = this.<String, Integer>emptyMap().put("0", 0).put("1", 1).put("2", 2).put("3", 3);
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void shouldConstructWithTabulateWithDuplicatedKeys() {
        assertThat(mapTabulate(4, i ->
                Tuple.of(Math.max(1, Math.min(i, 2)), String.valueOf(i + 1))
        ))
                .hasSize(2)
                .isEqualTo(mapOf(1, "2", 2, "4"));
    }

    @Test
    public void shouldConstructWithFill() {
        AtomicInteger i = new AtomicInteger();
        final Map<String, Integer> actual = mapFill(4, () -> Tuple.of(String.valueOf(i.get()), i.getAndIncrement()));
        final Map<String, Integer> expected = this.<String, Integer>emptyMap().put("0", 0).put("1", 1).put("2", 2).put("3", 3);
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void shouldConstructWithFillWithDuplicatedKeys() {
        AtomicInteger i = new AtomicInteger();
        assertThat(mapFill(4, () ->
                Tuple.of(Math.max(1, Math.min(i.get(), 2)), String.valueOf(i.getAndIncrement() + 1))
        ))
                .hasSize(2)
                .isEqualTo(mapOf(1, "2", 2, "4"));
    }

    @Nested
    class EqualityTests {
        @Test
        public void shouldObeyEqualityConstraints() {

            // sequential collections
            assertThat(emptyMap().equals(com.guizmaii.zazr.collection.HashMap.empty())).isTrue();
            assertThat(mapOf(1, "a").equals(com.guizmaii.zazr.collection.HashMap.of(1, "a"))).isTrue();
            assertThat(mapOf(1, "a", 2, "b", 3, "c").equals(com.guizmaii.zazr.collection.HashMap.of(1, "a", 2, "b",3, "c"))).isTrue();
            assertThat(mapOf(1, "a", 2, "b", 3, "c").equals(com.guizmaii.zazr.collection.HashMap.of(3, "c", 2, "b",1, "a"))).isTrue();

            // other classes
            assertThat(empty().equals(com.guizmaii.zazr.collection.List.empty())).isFalse();
            assertThat(empty().equals(com.guizmaii.zazr.collection.HashSet.empty())).isFalse();

            assertThat(empty().equals(com.guizmaii.zazr.collection.LinkedHashSet.empty())).isFalse();

            assertThat(empty().equals(com.guizmaii.zazr.collection.TreeSet.empty())).isFalse();
        }
    }

    @Nested
    class TostringTests {
        @Test
        public void shouldMakeString() {
            assertThat(emptyMap().toString()).isEqualTo(className() + "()");
            assertThat(emptyInt().put(1, 2).toString()).isEqualTo(className() + "(" + Tuple.of(1, 2) + ")");
        }
    }

    @Nested
    class TojavamapTests {
    }

    @Nested
    class ContainsTests {
        @Test
        public void shouldFindKey() {
            assertThat(emptyInt().put(1, 2).containsKey(1)).isTrue();
            assertThat(emptyInt().put(1, 2).containsKey(2)).isFalse();
        }

        @Test
        public void shouldFindValue() {
            assertThat(emptyInt().put(1, 2).containsValue(2)).isTrue();
            assertThat(emptyInt().put(1, 2).containsValue(1)).isFalse();
        }

        @Test
        public void shouldRecognizeNotContainedKeyValuePair() {
            final com.guizmaii.zazr.collection.TreeMap<String, Integer> testee = com.guizmaii.zazr.collection.TreeMap.of(Tuple.of("one", 1));
            assertThat(testee.contains(Tuple.of("one", 0))).isFalse();
        }

        @Test
        public void shouldRecognizeContainedKeyValuePair() {
            final com.guizmaii.zazr.collection.TreeMap<String, Integer> testee = com.guizmaii.zazr.collection.TreeMap.of(Tuple.of("one", 1));
            assertThat(testee.contains(Tuple.of("one", 1))).isTrue();
        }
    }

    @Nested
    class FlatmapTests {
        @SuppressWarnings("unchecked")
        @Test
        public void shouldFlatMapUsingBiFunction() {
            final Map<Integer, Integer> testee = mapOfTuples(Tuple.of(1, 11), Tuple.of(2, 22), Tuple.of(3, 33));
            final Map<String, String> actual = testee
                    .flatMap((k, v) -> com.guizmaii.zazr.collection.List.of(Tuple.of(String.valueOf(k), String.valueOf(v)),
                            Tuple.of(String.valueOf(k * 10), String.valueOf(v * 10))));
            final Map<String, String> expected = mapOfTuples(Tuple.of("1", "11"), Tuple.of("10", "110"), Tuple.of("2", "22"),
                    Tuple.of("20", "220"), Tuple.of("3", "33"), Tuple.of("30", "330"));
            assertThat(actual).isEqualTo(expected);
        }
    }

    @Nested
    class KeysetTests {
        @Test
        @SuppressWarnings("unchecked")
        public void shouldReturnKeySet() {
            final com.guizmaii.zazr.collection.Set<Integer> actual = mapOfTuples(Tuple.of(1, 11), Tuple.of(2, 22), Tuple.of(3, 33)).keySet();
            assertThat(actual).isEqualTo(com.guizmaii.zazr.collection.HashSet.of(1, 2, 3));
        }

    }

    // -- mapBoth

    @Test
    public void shouldMapBothEmpty() {
        assertThat(emptyInt().mapBoth(i -> i + 1, o -> o)).isEqualTo(com.guizmaii.zazr.collection.Vector.empty());
    }

    @Test
    public void shouldMapBothNonEmpty() {
        final Stream<Tuple2<Integer, String>> expected = Stream.of(Tuple.of(2, "1!"), Tuple.of(3, "2!"));
        final Stream<Tuple2<Integer, String>> actual = emptyInt().put(1, "1").put(2, "2").mapBoth(i -> i + 1, s -> s + "!").toStream();
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void shouldReturnEmptySetWhenAskedForTuple2SetOfAnEmptyMap() {
        assertThat(emptyMap().toSet()).isEqualTo(com.guizmaii.zazr.collection.HashSet.empty());
    }

    @Test
    public void shouldReturnTuple2SetOfANonEmptyMap() {
        assertThat(emptyInt().put(1, "1").put(2, "2").toSet()).isEqualTo(com.guizmaii.zazr.collection.HashSet.of(Tuple.of(1, "1"), Tuple.of(2, "2")));
    }

    @Test
    public void shouldReturnModifiedKeysMap() {
        final Map<String, String> actual = emptyIntString().put(1, "1").put(2, "2").mapKeys(k -> k * 12).mapKeys(Integer::toHexString).mapKeys(String::toUpperCase);
        final Map<String, String> expected = this.<String, String> emptyMap().put("C", "1").put("18", "2");
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void shouldReturnModifiedKeysMapWithNonUniqueMapper() {
        final Map<Integer, String> actual = emptyIntString()
                .put(1, "1").put(2, "2").put(3, "3")
                .mapKeys(k -> k * 118).mapKeys(Integer::toHexString).mapKeys(HashMapTest::md5).mapKeys(String::length);
        assertThat(actual).hasSize(1);
        assertThat(actual.values()).hasSize(1);
        //In different cases (based on items order) transformed map may contain different values
        assertThat(actual.values().head()).isIn("1", "2", "3");
    }

    public static String md5(String src) {
        try {
            final MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(src.getBytes(StandardCharsets.UTF_8));
            return toHexString(md.digest());
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Returns a string in the hexadecimal format.
     *
     * @param bytes the converted bytes
     * @return the hexadecimal string representing the bytes data
     * @throws IllegalArgumentException if the byte array is null
     */
    public static String toHexString(byte[] bytes) {
        if (bytes == null) {
            throw new IllegalArgumentException("byte array must not be null");
        }

        final StringBuilder hex = new StringBuilder(bytes.length * 2);
        for (byte aByte : bytes) {
            hex.append(Character.forDigit((aByte & 0XF0) >> 4, 16));
            hex.append(Character.forDigit((aByte & 0X0F), 16));
        }
        return hex.toString();
    }

    @Test
    public void shouldReturnModifiedKeysMapWithNonUniqueMapperAndMergedValues() {
        final Map<Integer, String> actual = emptyIntString()
                .put(1, "1").put(2, "2").put(3, "3")
                .mapKeys(k -> k * 118).mapKeys(Integer::toHexString).mapKeys(HashMapTest::md5)//Unique key mappers
                .mapKeys(String::length, (v1, v2) -> com.guizmaii.zazr.collection.List.of(v1.split("#")).append(v2).sorted().mkString("#"));
        final Map<Integer, String> expected = emptyIntString().put(32, "1#2#3");
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void shouldReturnModifiedValuesMap() {
        assertThat(emptyIntString().put(1, "1").put(2, "2").mapValues(Integer::parseInt)).isEqualTo(emptyInt().put(1, 1).put(2, 2));
    }

    // -- merge(Map)

    @Test
    public void shouldMerge() {
        final Map<Integer, Integer> m1 = emptyIntInt().put(1, 1).put(2, 2);
        final Map<Integer, Integer> m2 = emptyIntInt().put(1, 1).put(4, 4);
        final Map<Integer, Integer> m3 = emptyIntInt().put(3, 3).put(4, 4);
        assertThat(m1.merge(m2)).isEqualTo(emptyIntInt().put(1, 1).put(2, 2).put(4, 4));
        assertThat(m1.merge(m3)).isEqualTo(emptyIntInt().put(1, 1).put(2, 2).put(3, 3).put(4, 4));
    }

    @Test
    public void shouldReturnSameMapWhenMergeNonEmptyWithEmpty() {
        final Map<Integer, String> map = mapOf(1, "a", 2, "b", 3, "c");
        assertThat(map.merge(emptyMap())).isSameAs(map);
    }

    @Test
    public void shouldReturnSameMapWhenMergeEmptyWithNonEmpty() {
        final Map<Integer, String> map = mapOf(1, "a", 2, "b", 3, "c");
        if (map instanceof SortedMap) {
            assertThat(this.<Integer, String> emptyMap().merge(map)).isEqualTo(map);
        } else {
            assertThat(this.<Integer, String> emptyMap().merge(map)).isSameAs(map);
        }
    }

    // -- merge(Map, BiFunction)

    @Test
    public void shouldMergeCollisions() {
        final Map<Integer, Integer> m1 = emptyIntInt().put(1, 1).put(2, 2);
        final Map<Integer, Integer> m2 = emptyIntInt().put(1, 2).put(4, 4);
        final Map<Integer, Integer> m3 = emptyIntInt().put(3, 3).put(4, 4);
        assertThat(emptyIntInt().merge(m2, Math::max)).isEqualTo(m2);
        assertThat(m2.merge(emptyIntInt(), Math::max)).isEqualTo(m2);
        assertThat(m1.merge(m2, Math::max)).isEqualTo(emptyIntInt().put(1, 2).put(2, 2).put(4, 4));
        assertThat(m1.merge(m3, Math::max)).isEqualTo(emptyIntInt().put(1, 1).put(2, 2).put(3, 3).put(4, 4));
    }

    @Test
    public void shouldReturnSameMapWhenMergeNonEmptyWithEmptyUsingCollisionResolution() {
        final Map<Integer, Integer> map = mapOf(1, 1, 2, 2, 3, 3);
        assertThat(map.merge(emptyMap(), Math::max)).isSameAs(map);
    }

    @Test
    public void shouldReturnSameMapWhenMergeEmptyWithNonEmptyUsingCollisionResolution() {
        final Map<Integer, Integer> map = mapOf(1, 1, 2, 2, 3, 3);
        if (map instanceof SortedMap) {
            assertThat(this.<Integer, Integer> emptyMap().merge(map, Math::max)).isEqualTo(map);
        } else {
            assertThat(this.<Integer, Integer> emptyMap().merge(map, Math::max)).isSameAs(map);
        }
    }

    @Nested
    class Equality2Tests {
        @Test
        public void shouldIgnoreOrderOfEntriesWhenComparingForEquality() {
            final Map<?, ?> map1 = emptyInt().put(1, 'a').put(2, 'b').put(3, 'c');
            final Map<?, ?> map2 = emptyInt().put(3, 'c').put(2, 'b').put(1, 'a').remove(2).put(2, 'b');
            assertThat(map1).isEqualTo(map2);
        }
    }

    @Nested
    class PutTests {
        @Test
        public void shouldPutTuple() {
            assertThat(emptyIntInt().put(Tuple.of(1, 2))).isEqualTo(emptyIntInt().put(1, 2));
        }

        @Test
        public void shouldPutExistingKeyAndNonEqualValue() {
            final Map<IntMod2, String> map = mapOf(new IntMod2(1), "a");

            // we need to compare Strings because equals (intentionally) does not work for IntMod2
            final String actual = map.put(new IntMod2(3), "b").toString();
            final String expected = mapPrefix() + "((3, b))";

            assertThat(actual).isEqualTo(expected);
        }

        @Test
        public void shouldPutExistingKeyAndEqualValue() {
            final Map<IntMod2, String> map = mapOf(new IntMod2(1), "a");

            // we need to compare Strings because equals (intentionally) does not work for IntMod2
            final String actual = map.put(new IntMod2(3), "a").toString();
            final String expected = mapPrefix() + "((3, a))";

            assertThat(actual).isEqualTo(expected);
        }
    }

    @Nested
    class RemoveTests {
        @Test
        public void shouldRemoveKey() {
            final Map<Integer, Object> src = emptyInt().put(1, 'a').put(2, 'b').put(3, 'c');
            assertThat(src.remove(2)).isEqualTo(emptyInt().put(1, 'a').put(3, 'c'));
            assertThat(src.remove(33)).isSameAs(src);
        }
    }

    @Nested
    class RemoveallTests {
        @Test
        public void shouldRemoveAllKeys() {
            final Map<Integer, Object> src = emptyInt().put(1, 'a').put(2, 'b').put(3, 'c');
            assertThat(src.removeAll(com.guizmaii.zazr.collection.List.of(1, 3))).isEqualTo(emptyInt().put(2, 'b'));
            assertThat(src.removeAll(com.guizmaii.zazr.collection.List.of(33))).isSameAs(src);
            assertThat(src.removeAll(com.guizmaii.zazr.collection.List.empty())).isSameAs(src);
        }

        @Test
        public void shouldReturnSameMapWhenNonEmptyRemoveAllEmpty() {
            final Map<Integer, String> map = mapOf(1, "a", 2, "b", 3, "c");
            assertThat(map.removeAll(com.guizmaii.zazr.collection.List.empty())).isSameAs(map);
        }

        @Test
        public void shouldReturnSameMapWhenEmptyRemoveAllNonEmpty() {
            final Map<Integer, String> empty = emptyMap();
            assertThat(empty.removeAll(com.guizmaii.zazr.collection.List.of(1, 2, 3))).isSameAs(empty);
        }
    }

    @Nested
    class ForeachTests {
        @Test
        public void forEachByKeyValue() {
            final Map<Integer, Integer> map = mapOf(1, 2).put(3, 4);
            final int[] result = { 0 };
            map.forEach((k, v) -> {
                result[0] += k + v;
            });
            assertThat(result[0]).isEqualTo(10);
        }

        @Test
        public void forEachByKeyValueVisitsTheEntriesInIterationOrder() {
            // every node boundary, and keys of one hash in collision nodes
            for (int size : new int[] { 0, 1, 2, 31, 32, 33, 1023, 1024, 1025 }) {
                for (boolean colliding : new boolean[] { false, true }) {
                    HashMap<Object, Integer> map = HashMap.empty();
                    for (int i = 0; i < size; i++) {
                        map = map.put(colliding ? new CollidingKey(i % 7, i) : Integer.valueOf(i), i);
                    }
                    final java.util.List<Object> walked = new java.util.ArrayList<>();
                    map.forEach((k, v) -> {
                        walked.add(k);
                        walked.add(v);
                    });
                    final java.util.List<Object> iterated = new java.util.ArrayList<>();
                    for (Tuple2<Object, Integer> entry : map) {
                        iterated.add(entry._1());
                        iterated.add(entry._2());
                    }
                    org.assertj.core.api.Assertions.assertThat(walked).isEqualTo(iterated).hasSize(2 * size);
                }
            }
            org.assertj.core.api.Assertions.assertThatThrownBy(() -> HashMap.of(1, 2).forEach((BiConsumer<Integer, Integer>) null))
                    .isInstanceOf(NullPointerException.class).hasMessage("action is null");
        }

        // equal when the id is, with one of a few hash codes
        private record CollidingKey(int hash, int id) {
            @Override
            public int hashCode() {
                return hash;
            }
        }

        @Test
        public void forEachByTuple() {
            final Map<Integer, Integer> map = mapOf(1, 2).put(3, 4);
            final int[] result = { 0 };
            map.forEach(t -> {
                result[0] += t._1() + t._2();
            });
            assertThat(result[0]).isEqualTo(10);
        }
    }

    // -- put with merge function

    @Test
    public void putWithWasntPresent() {
        final Map<Integer, Integer> map = mapOf(1, 2)
                .put(2, 3, (x, y) -> x + y);
        assertThat(map).isEqualTo(emptyIntInt().put(1, 2).put(2, 3));
    }

    @Test
    public void putWithWasPresent() {
        final Map<Integer, Integer> map = mapOf(1, 2)
                .put(1, 3, (x, y) -> x + y);
        assertThat(map).isEqualTo(emptyIntInt().put(1, 5));
    }

    @Test
    public void putWithTupleWasntPresent() {
        final Map<Integer, Integer> map = mapOf(1, 2)
                .put(Tuple.of(2, 3), (x, y) -> x + y);
        assertThat(map).isEqualTo(emptyIntInt().put(1, 2).put(2, 3));
    }

    @Test
    public void putWithTupleWasPresent() {
        final Map<Integer, Integer> map = mapOf(1, 2)
                .put(Tuple.of(1, 3), (x, y) -> x + y);
        assertThat(map).isEqualTo(emptyIntInt().put(1, 5));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void shouldTabulateTheSeq() {
        final Function<Number, Tuple2<Long, Float>> f = i -> new Tuple2<>(i.longValue(), i.floatValue());
        final Map<Long, Float> map = mapTabulate(3, f);
        assertThat(map).isEqualTo(mapOfTuples(new Tuple2<>(0l, 0f), new Tuple2<>(1l, 1f), new Tuple2<>(2l, 2f)));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void shouldTabulateTheSeqCallingTheFunctionInTheRightOrder() {
        final LinkedList<Integer> ints = new LinkedList<>(asList(0, 0, 1, 1, 2, 2));
        final Function<Integer, Tuple2<Long, Float>> f = i -> new Tuple2<>(ints.remove().longValue(), ints.remove().floatValue());
        final Map<Long, Float> map = mapTabulate(3, f);
        assertThat(map).isEqualTo(mapOfTuples(new Tuple2<>(0l, 0f), new Tuple2<>(1l, 1f), new Tuple2<>(2l, 2f)));
    }

    @Test
    public void shouldTabulateTheSeqWith0Elements() {
        assertThat(mapTabulate(0, i -> new Tuple2<>(i, i))).isEqualTo(empty());
    }

    @Test
    public void shouldTabulateTheSeqWith0ElementsWhenNIsNegative() {
        assertThat(mapTabulate(-1, i -> new Tuple2<>(i, i))).isEqualTo(empty());
    }

    // -- fill(int, Supplier)

    @SuppressWarnings("unchecked")
    @Test
    public void shouldFillTheSeqCallingTheSupplierInTheRightOrder() {
        final LinkedList<Integer> ints = new LinkedList<>(asList(0, 0, 1, 1, 2, 2));
        final Supplier<Tuple2<Long, Float>> s = () -> new Tuple2<>(ints.remove().longValue(), ints.remove().floatValue());
        final Map<Long, Float> actual = mapFill(3, s);
        assertThat(actual).isEqualTo(mapOfTuples(new Tuple2<>(0l, 0f), new Tuple2<>(1l, 1f), new Tuple2<>(2l, 2f)));
    }

    @Test
    public void shouldFillTheSeqWith0Elements() {
        assertThat(mapFill(0, () -> new Tuple2<>(1, 1))).isEqualTo(empty());
    }

    @Test
    public void shouldReturnSingleMapAfterFillWithConstantKeys() {
        AtomicInteger value = new AtomicInteger(83);
        assertThat(mapFill(17, () -> Tuple.of(7, value.getAndIncrement())))
                .hasSize(1)
                .isEqualTo(mapOf(7, value.decrementAndGet()));
    }

    @Test
    public void shouldFillTheSeqWith0ElementsWhenNIsNegative() {
        assertThat(mapFill(-1, () -> new Tuple2<>(1, 1))).isEqualTo(empty());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void mapOfTuplesShouldReturnTheSingletonEmpty() {
        if (!emptyMapShouldBeSingleton()) { return; }
        assertThat(mapOfTuples()).isSameAs(emptyMap());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void mapOfEntriesShouldReturnTheSingletonEmpty() {
        if (!emptyMapShouldBeSingleton()) { return; }
        assertThat(mapOfEntries()).isSameAs(emptyMap());
    }

    @Nested
    class FilterTests {
        @Test
        public void shouldBiFilterWork() throws Exception {
            final Map<Integer, String> src = mapTabulate(20, n -> Tuple.of(n, Integer.toHexString(n)));
            final Pattern isDigits = Pattern.compile("^\\d+$");
            final Map<Integer, String> dst = src.filter((k, v) -> k % 2 == 0 && isDigits.matcher(v).matches());
            assertThat(dst).isEqualTo(emptyIntString().put(0, "0").put(2, "2").put(4, "4").put(6, "6").put(8, "8").put(16, "10").put(18, "12"));
        }

        @Test
        public void shouldKeyFilterWork() throws Exception {
            final Map<Integer, String> src = mapTabulate(20, n -> Tuple.of(n, Integer.toHexString(n)));
            final Map<Integer, String> dst = src.filterKeys(k -> k % 2 == 0);
            assertThat(dst).isEqualTo(emptyIntString().put(0, "0").put(2, "2").put(4, "4").put(6, "6").put(8, "8").put(10, "a").put(12, "c").put(14, "e").put(16, "10").put(18, "12"));
        }

        @Test
        public void shouldValueFilterWork() throws Exception {
            final Map<Integer, String> src = mapTabulate(10, n -> Tuple.of(n, Integer.toHexString(n)));
            final Pattern isDigits = Pattern.compile("^\\d+$");
            final Map<Integer, String> dst = src.filterValues(v -> isDigits.matcher(v).matches());
            assertThat(dst).isEqualTo(emptyIntString().put(0, "0").put(1, "1").put(2, "2").put(3, "3").put(4, "4").put(5, "5").put(6, "6").put(7, "7").put(8, "8").put(9, "9"));
        }
    }

    @Nested
    class RejectTests {
        @Test
        public void shouldBiRejectWork() throws Exception {
            final Map<Integer, String> src = mapTabulate(20, n -> Tuple.of(n, Integer.toHexString(n)));
            final Pattern isDigits = Pattern.compile("^\\d+$");
            final Map<Integer, String> dst = src.reject((k, v) -> k % 2 == 0 && isDigits.matcher(v).matches());
            assertThat(dst).isEqualTo(emptyIntString().put(1, "1").put(3, "3").put(5, "5").put(7, "7").put(9, "9").put(10, "a").put(11, "b").put(12, "c").put(13, "d").put(14, "e").put(15, "f").put(17, "11").put(19, "13"));
        }

        @Test
        public void shouldKeyRejectWork() throws Exception {
            final Map<Integer, String> src = mapTabulate(20, n -> Tuple.of(n, Integer.toHexString(n)));
            final Map<Integer, String> dst = src.rejectKeys(k -> k % 2 == 0);
            assertThat(dst).isEqualTo(emptyIntString().put(1, "1").put(3, "3").put(5, "5").put(7, "7").put(9, "9").put(11, "b").put(13, "d").put(15, "f").put(17, "11").put(19, "13"));
        }

        @Test
        public void shouldValueRejectWork() throws Exception {
            final Map<Integer, String> src = mapTabulate(15, n -> Tuple.of(n, Integer.toHexString(n)));
            final Pattern isDigits = Pattern.compile("^\\d+$");
            final Map<Integer, String> dst = src.rejectValues(v -> isDigits.matcher(v).matches());
            assertThat(dst).isEqualTo(emptyIntString().put(10, "a").put(11, "b").put(12, "c").put(13, "d").put(14, "e"));
        }
    }

    @Nested
    class RemoveByFilterTests {
        @SuppressWarnings("deprecation")
        @Test
        public void shouldBiRemoveWork() throws Exception {
            final Map<Integer, String> src = mapTabulate(20, n -> Tuple.of(n, Integer.toHexString(n)));
            final Pattern isDigits = Pattern.compile("^\\d+$");
            final Map<Integer, String> dst = src.removeAll((k, v) -> k % 2 == 0 && isDigits.matcher(v).matches());
            assertThat(dst).isEqualTo(emptyIntString().put(1, "1").put(3, "3").put(5, "5").put(7, "7").put(9, "9").put(10, "a").put(11, "b").put(12, "c").put(13, "d").put(14, "e").put(15, "f").put(17, "11").put(19, "13"));
        }

        @SuppressWarnings("deprecation")
        @Test
        public void shouldKeyRemoveWork() throws Exception {
            final Map<Integer, String> src = mapTabulate(20, n -> Tuple.of(n, Integer.toHexString(n)));
            final Map<Integer, String> dst = src.removeKeys(k -> k % 2 == 0);
            assertThat(dst).isEqualTo(emptyIntString().put(1, "1").put(3, "3").put(5, "5").put(7, "7").put(9, "9").put(11, "b").put(13, "d").put(15, "f").put(17, "11").put(19, "13"));
        }

        @SuppressWarnings("deprecation")
        @Test
        public void shouldValueRemoveWork() throws Exception {
            final Map<Integer, String> src = mapTabulate(20, n -> Tuple.of(n, Integer.toHexString(n)));
            final Pattern isDigits = Pattern.compile("^\\d+$");
            final Map<Integer, String> dst = src.removeValues(v -> isDigits.matcher(v).matches());
            assertThat(dst).isEqualTo(emptyIntString().put(10, "a").put(11, "b").put(12, "c").put(13, "d").put(14, "e").put(15, "f"));
        }
    }

    @Nested
    class ComputeifabsentTests {
        @Test
        public void shouldComputeIfAbsent() {
            final Map<Integer, String> map = emptyIntString().put(1, "v");
            assertThat(map.computeIfAbsent(1, k -> "b")).isEqualTo(Tuple.of("v", map));
            assertThat(map.computeIfAbsent(2, k -> "n")).isEqualTo(Tuple.of("n", emptyIntString().put(1, "v").put(2, "n")));
        }
    }

    @Nested
    class ComputeIfPresentTests {
        @Test
        public void shouldComputeIfPresent() {
            final Map<Integer, String> map = emptyIntString().put(1, "v");
            assertThat(map.computeIfPresent(1, (k, v) -> "b")).isEqualTo(Tuple.of(Option.some("b"), emptyIntString().put(1, "b")));
            assertThat(map.computeIfPresent(2, (k, v) -> "n")).isEqualTo(Tuple.of(Option.none(), map));
        }

        @Test
        public void shouldRejectComputeIfPresentWithNullResult() {
            // Some(null) does not exist (design 3.9), so a remapping to null cannot be reported
            final Map<Integer, String> map = emptyIntString().put(1, "v");
            assertThatThrownBy(() -> map.computeIfPresent(1, (k, v) -> null)).isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    class GetWithNullsTests {
        @Test
        public void shouldRejectPutOfNullValue() {
            assertThatNullPointerException().isThrownBy(() -> mapOf("1", "a").put("2", null));
        }

        @Test
        public void shouldRejectOfWithNullValue() {
            assertThatNullPointerException().isThrownBy(() -> HashMapTest.this.<String, String>mapOf("1", null));
        }

        @Test
        public void shouldReturnSameInstanceIfReplacingCurrentValueWithNonExistingKey() {
            final Map<Integer, String> map = mapOf(1, "a", 2, "b");
            final Map<Integer, String> actual = map.replaceValue(3, "?");
            assertThat(actual).isSameAs(map);
        }

        @Test
        public void shouldReplaceCurrentValueForExistingKey() {
            final Map<Integer, String> map = mapOf(1, "a", 2, "b");
            final Map<Integer, String> actual = map.replaceValue(2, "c");
            final Map<Integer, String> expected = mapOf(1, "a", 2, "c");
            assertThat(actual).isEqualTo(expected);
        }

        @Test
        public void shouldReplaceCurrentValueForExistingKeyAndEqualOldValue() {
            final Map<Integer, String> map = mapOf(1, "a", 2, "b");
            final Map<Integer, String> actual = map.replace(2, "b", "c");
            final Map<Integer, String> expected = mapOf(1, "a", 2, "c");
            assertThat(actual).isEqualTo(expected);
        }

        @Test
        public void shouldReturnSameInstanceForExistingKeyAndNonEqualOldValue() {
            final Map<Integer, String> map = mapOf(1, "a", 2, "b");
            final Map<Integer, String> actual = map.replace(2, "d", "c");
            assertThat(actual).isSameAs(map);
        }

        @Test
        public void shouldReturnSameInstanceIfReplacingCurrentValueWithOldValueWithNonExistingKey() {
            final Map<Integer, String> map = mapOf(1, "a", 2, "b");
            final Map<Integer, String> actual = map.replace(3, "?", "!");
            assertThat(actual).isSameAs(map);
        }

        @Test
        public void shouldNotReplaceTupleWhenValueDoesNotMatch() {
            final Map<Integer, String> map = mapOf(1, "a", 2, "b");
            final Map<Integer, String> actual = map.replace(Tuple.of(2, "x"), Tuple.of(2, "c"));
            assertThat(actual).isSameAs(map);
        }

        @Test
        public void shouldReplaceAllValuesWithFunctionResult() {
            final Map<Integer, String> map = mapOf(1, "a", 2, "b");
            final Map<Integer, String> actual = map.replaceAll((integer, s) -> s + integer);
            final Map<Integer, String> expected = mapOf(1, "a1", 2, "b2");
            assertThat(actual).isEqualTo(expected);
        }

        @Test
        public void shouldRejectPutOfNullKey() {
            assertThatNullPointerException().isThrownBy(() -> mapOf(1, "a").put(null, "b"));
        }
    }

    @Nested
    class GetorelseTests {
        @Test
        public void shouldReturnDefaultValue() {
            final Map<String, String> map = mapOf("1", "a").put("2", "b");
            assertThat(map.getOrElse("3", "3")).isEqualTo("3");
        }
    }

    @Nested
    class PartitionTests {
        @Test
        public void shouldPartitionInOneIteration() {
            final AtomicInteger count = new AtomicInteger(0);
            final Map<String, Integer> map = mapOf("1", 1, "2", 2, "3", 3);
            final Tuple2<? extends Map<String, Integer>, ? extends Map<String, Integer>> results = map.partition(entry -> {
                count.incrementAndGet();
                return true;
            });
            assertThat(results._1()).isEqualTo(mapOf("1", 1, "2", 2, "3", 3));
            assertThat(results._2()).isEmpty();
            assertThat(count.get()).isEqualTo(3);
        }
    }

    // -- spliterator

    @Test
    public void shouldHaveSizedSpliterator() {
        assertThat(of(1, 2, 3).spliterator().hasCharacteristics(Spliterator.SIZED | Spliterator.SUBSIZED)).isTrue();
    }

    @Test
    public void shouldHaveDistinctSpliterator() {
        assertThat(of(1, 2, 3).spliterator().hasCharacteristics(Spliterator.DISTINCT)).isTrue();
    }

    @Test
    public void shouldReturnSizeWhenSpliterator() {
        assertThat(of(1, 2, 3).spliterator().getExactSizeIfKnown()).isEqualTo(3);
    }

    // -- null values: everything but get(k) itself must work on a map holding a null value, since Some(null) does not exist

    @Nested
    class NullValueTests {

        @Test
        public void shouldRejectOfWithNullValue() {
            assertThatNullPointerException().isThrownBy(() -> HashMapTest.this.<String, String>mapOf("k", null));
        }

        @Test
        public void shouldRejectPutOfNullValue() {
            assertThatNullPointerException().isThrownBy(() -> HashMapTest.this.<String, String>mapOf("k", "v").put("k", null));
        }

        @Test
        public void shouldRejectPutWithMergeResultingInNull() {
            assertThatNullPointerException().isThrownBy(() -> HashMapTest.this.<String, String>mapOf("k", "v").put("k", "w", (a, b) -> null));
        }

        @Test
        public void shouldRejectComputeIfPresentWithNullResult() {
            assertThatNullPointerException().isThrownBy(() -> HashMapTest.this.<String, String>mapOf("k", "v").computeIfPresent("k", (k, v) -> null));
        }

        @Test
        public void shouldRejectComputeIfAbsentWithNullResult() {
            assertThatNullPointerException().isThrownBy(() -> HashMapTest.this.<String, String>emptyMap().computeIfAbsent("k", k -> null));
        }

        @Test
        public void shouldRejectReplaceWithNullValue() {
            final Map<String, String> map = mapOf("k", "v");
            assertThatNullPointerException().isThrownBy(() -> map.replace(Tuple.of("k", "v"), Tuple.<String, String>of("k", null)));
            assertThatNullPointerException().isThrownBy(() -> map.replaceAll((k, v) -> null));
        }

        @Test
        public void shouldRejectMapKeysWithMergeResultingInNull() {
            final Map<String, String> map = mapOf("a", "1", "b", "2");
            assertThatNullPointerException().isThrownBy(() -> map.mapKeys(k -> "x", (v1, v2) -> null));
        }
    }

    // -- the ABSENT sentinel (Maps.java) must never leak: every path that reads it internally
    // must treat an absent key exactly like get and getOrElse do, and the sentinel
    // itself must never equal a real user value.

    @Nested
    class AbsentSentinelTests {

        @Test
        public void sentinelIsNotEqualToAnyUserObject() {
            assertThat(Maps.ABSENT).isNotEqualTo("anything");
            assertThat(Maps.ABSENT).isNotEqualTo((Object) null);
            assertThat(Maps.ABSENT.equals(new Object())).isFalse();
        }

        @Test
        public void containsTreatsAbsentKeyAsAbsent() {
            final Map<String, String> map = mapOf("k", "v");
            assertThat(map.contains(Tuple.of("missing", "v"))).isFalse();
        }

        @Test
        public void computeIfAbsentOnAbsentKeyComputes() {
            final Tuple2<String, ? extends Map<String, String>> result = HashMapTest.this.<String, String>emptyMap().computeIfAbsent("k", k -> "computed");
            assertThat(result._1()).isEqualTo("computed");
        }

        @Test
        public void computeIfPresentOnAbsentKeyIsNoop() {
            final Map<String, String> map = mapOf("k", "v");
            final Tuple2<Option<String>, ? extends Map<String, String>> result = map.computeIfPresent("missing", (k, v) -> "x");
            assertThat(result._1()).isEqualTo(Option.none());
            assertThat(result._2()).isSameAs(map);
        }

        @Test
        public void mergeOnAbsentKeyTakesTheOtherMapsValue() {
            final Map<String, String> map = mapOf("a", "1");
            final Map<String, String> merged = map.merge(mapOf("b", "2"), (a, b) -> a);
            assertThat(merged).isEqualTo(mapOf("a", "1", "b", "2"));
        }

        @Test
        public void putWithMergeOnAbsentKeyPutsWithoutMerging() {
            final Map<String, String> map = HashMapTest.this.<String, String>emptyMap().put("k", "v", (a, b) -> "merged");
            assertThat(map).isEqualTo(mapOf("k", "v"));
        }

        @Test
        public void mapKeysCollapsingOntoAbsentTargetTakesTheMappedValue() {
            final Map<String, String> map = mapOf("a", "1");
            assertThat(map.mapKeys(k -> "x", (v1, v2) -> "merged")).isEqualTo(mapOf("x", "1"));
        }
    }

    // -- collect(BiFunction)

    @Test
    public void shouldCollectEntriesIntoAMapOfTheSameKind() {
        final Map<Integer, String> map = mapOf(1, "a", 2, "b", 3, "c");
        final Map<String, Integer> actual = map.collect((k, v) -> k % 2 == 1 ? Option.some(Tuple.of(v, k * 10)) : Option.none());
        assertThat(actual).isEqualTo(this.<String, Integer>emptyMap().put("a", 10).put("c", 30));
        assertThat(actual.getClass()).isSameAs(map.getClass());
    }

    @Test
    public void shouldCollectNothingWhenEveryEntryIsDropped() {
        assertThat(mapOf(1, "a", 2, "b").collect((k, v) -> Option.none())).isEqualTo(emptyMap());
    }

    @Test
    public void shouldCollectNothingFromAnEmptyMap() {
        final AtomicInteger calls = new AtomicInteger();
        assertThat(this.<Integer, String>emptyMap().collect((k, v) -> {
            calls.incrementAndGet();
            return Option.some(Tuple.of(k, v));
        })).isEqualTo(emptyMap());
        assertThat(calls.get()).isEqualTo(0);
    }

    @Test
    public void shouldKeepTheLastEntryOnKeyCollisionWhenCollecting() {
        final Map<Integer, String> map = mapOf(1, "a", 2, "b", 3, "c");
        final Map<Integer, String> actual = map.collect((k, v) -> Option.some(Tuple.of(0, v)));
        assertThat(actual.size()).isEqualTo(1);
        assertThat(actual.get(0)).isEqualTo(Option.some(map.toList().last()._2()));
    }

    @Test
    public void shouldRejectNullOptionFromCollectBiFunction() {
        final Map<Integer, String> map = mapOf(1, "a");
        final NullPointerException e = assertThrows(NullPointerException.class, () -> map.collect((k, v) -> null));
        assertThat(e.getMessage()).isEqualTo(map.getClass().getSimpleName() + ".collect: mapper returned null");
    }

    @Test
    public void shouldThrowOnCollectWithNullBiFunction() {
        final java.util.function.BiFunction<Integer, String, Option<Tuple2<Integer, String>>> mapper = null;
        assertThrows(NullPointerException.class, () -> mapOf(1, "a").collect(mapper));
    }
    // -- the one-pass and entry-wise operations, on the map itself (its elements are the entries)

    /** A map of the given values under the keys {@code 0 .. n - 1}. */
    @SafeVarargs
    private <T> Map<Integer, T> entries(T... values) {
        return of(values).original();
    }

    private static <K, V> Tuple2<K, V> entry(K key, V value) {
        return Tuple.of(key, value);
    }

    private static Tuple2<Integer, Integer> sumOfEntries(Tuple2<Integer, Integer> a, Tuple2<Integer, Integer> b) {
        return Tuple.of(a._1() + b._1(), a._2() + b._2());
    }

    // -- existsUnique

    @Test
    public void shouldBeAwareOfExistingUniqueElement() {
        assertThat(entries(1, 2).existsUnique(t -> t._2() == 1)).isTrue();
    }

    @Test
    public void shouldBeAwareOfNonExistingUniqueElement() {
        assertThat(this.<Integer, Integer>emptyMap().existsUnique(t -> t._2() == 1)).isFalse();
    }

    @Test
    public void shouldBeAwareOfExistingNonUniqueElement() {
        assertThat(entries(1, 1, 2).existsUnique(t -> t._2() == 1)).isFalse();
    }

    // -- filter(Predicate)

    @Test
    public void shouldFilterExistingElements() {
        assertThat(entries(1, 2, 3).filter(t -> t._2() == 1)).isEqualTo(mapOf(0, 1));
        assertThat(entries(1, 2, 3).filter(t -> t._2() == 2)).isEqualTo(mapOf(1, 2));
        assertThat(entries(1, 2, 3).filter(t -> t._2() == 3)).isEqualTo(mapOf(2, 3));
        assertThat(entries(1, 2, 3).filter(ignore -> true)).isEqualTo(entries(1, 2, 3));
    }

    @Test
    public void shouldFilterNonExistingElements() {
        assertThat(this.<Integer, Integer>emptyMap().filter(t -> t._2() == 0)).isEqualTo(emptyMap());
        assertThat(entries(1, 2, 3).filter(t -> t._2() == 0)).isEqualTo(emptyMap());
    }

    @Test
    public void shouldReturnSameInstanceWhenFilteringEmptyTraversable() {
        final Map<Integer, Integer> empty = emptyMap();
        if (emptyMapShouldBeSingleton()) {
            assertThat(empty.filter(v -> true)).isSameAs(empty);
        } else {
            assertThat(empty.filter(v -> true)).isEqualTo(empty);
        }
    }

    // -- reject(Predicate)

    @Test
    public void shouldRejectExistingElements() {
        assertThat(entries(1, 2, 3).reject(t -> t._2() == 1)).isEqualTo(mapOf(1, 2, 2, 3));
        assertThat(entries(1, 2, 3).reject(t -> t._2() == 2)).isEqualTo(mapOf(0, 1, 2, 3));
        assertThat(entries(1, 2, 3).reject(t -> t._2() == 3)).isEqualTo(mapOf(0, 1, 1, 2));
        assertThat(entries(1, 2, 3).reject(ignore -> false)).isEqualTo(entries(1, 2, 3));
    }

    @Test
    public void shouldRejectNonExistingElements() {
        assertThat(this.<Integer, Integer>emptyMap().reject(t -> t._2() == 0)).isEqualTo(emptyMap());
        assertThat(entries(1, 2, 3).reject(t -> t._2() > 0)).isEqualTo(emptyMap());
    }

    @Test
    public void shouldReturnSameInstanceWhenRejectingEmptyTraversable() {
        final Map<Integer, Integer> empty = emptyMap();
        if (emptyMapShouldBeSingleton()) {
            assertThat(empty.reject(v -> true)).isSameAs(empty);
        } else {
            assertThat(empty.reject(v -> true)).isEqualTo(empty);
        }
    }

    // -- fold

    @Test
    public void shouldFoldNil() {
        assertThat(this.<Integer, Integer>emptyMap().fold(entry(0, 0), HashMapTest::sumOfEntries)).isEqualTo(entry(0, 0));
    }

    @Test
    public void shouldThrowWhenFoldNullOperator() {
        assertThrows(NullPointerException.class, () -> this.<Integer, Integer>emptyMap().fold(null, null));
    }

    @Test
    public void shouldFoldSingleElement() {
        assertThat(entries(1).fold(entry(0, 0), HashMapTest::sumOfEntries)).isEqualTo(entry(0, 1));
    }

    @Test
    public void shouldFoldMultipleElements() {
        assertThat(entries(1, 2, 3).fold(entry(0, 0), HashMapTest::sumOfEntries)).isEqualTo(entry(3, 6));
    }

    // -- groupBy

    @Test
    public void shouldNilGroupBy() {
        assertThat(emptyMap().groupBy(Function.identity())).isEqualTo(LinkedHashMap.empty());
    }

    @Test
    public void shouldNonNilGroupByIdentity() {
        final Map<?, ?> actual = entries('a', 'b', 'c').groupBy(Tuple2::_2);
        final Map<?, ?> expected = LinkedHashMap.empty().put('a', mapOf(0, 'a')).put('b', mapOf(1, 'b')).put('c', mapOf(2, 'c'));
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void shouldNonNilGroupByEqual() {
        final Map<?, ?> actual = entries('a', 'b', 'c').groupBy(c -> 1);
        final Map<?, ?> expected = LinkedHashMap.empty().put(1, entries('a', 'b', 'c'));
        assertThat(actual).isEqualTo(expected);
    }

    // -- arrangeBy

    @Test
    public void shouldNilArrangeBy() {
        assertThat(emptyMap().arrangeBy(Function.identity())).isEqualTo(Option.some(LinkedHashMap.empty()));
    }

    @Test
    public void shouldNonNilArrangeByIdentity() {
        final Option<Map<Character, Tuple2<Integer, Character>>> actual = entries('a', 'b', 'c').arrangeBy(Tuple2::_2);
        final Option<Map<?, ?>> expected = Option.some(LinkedHashMap.empty().put('a', entry(0, 'a')).put('b', entry(1, 'b')).put('c', entry(2, 'c')));
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void shouldNonNilArrangeByEqual() {
        final Option<Map<Integer, Tuple2<Integer, Character>>> actual = entries('a', 'b', 'c').arrangeBy(c -> 1);
        final Option<Map<?, ?>> expected = Option.none();
        assertThat(actual).isEqualTo(expected);
    }

    // -- max: the natural order of the entries, key first (Tuple2 is Comparable component-wise)

    @Test
    public void shouldReturnNoneWhenComputingMaxOfNil() {
        assertThat(emptyMap().max()).isEqualTo(Option.none());
    }

    @Test
    public void shouldComputeMaxOfOneValue() {
        assertThat(mapOf(5, 5).max()).isEqualTo(Option.some(entry(5, 5)));
    }

    @Test
    public void shouldComputeMaxOfStrings() {
        assertThat(mapOf("1", "1", "2", "2", "3", "3").max()).isEqualTo(Option.some(entry("3", "3")));
    }

    @Test
    public void shouldComputeMaxOfBoolean() {
        assertThat(mapOf(true, true, false, false).max()).isEqualTo(Option.some(entry(true, true)));
    }

    @Test
    public void shouldComputeMaxOfByte() {
        assertThat(mapOf((byte) 1, (byte) 1, (byte) 2, (byte) 2).max()).isEqualTo(Option.some(entry((byte) 2, (byte) 2)));
    }

    @Test
    public void shouldComputeMaxOfChar() {
        assertThat(mapOf('a', 'a', 'b', 'b', 'c', 'c').max()).isEqualTo(Option.some(entry('c', 'c')));
    }

    @Test
    public void shouldComputeMaxOfDouble() {
        assertThat(mapOf(.1, .1, .2, .2, .3, .3).max()).isEqualTo(Option.some(entry(.3, .3)));
    }

    @Test
    public void shouldComputeMaxOfFloat() {
        assertThat(mapOf(.1f, .1f, .2f, .2f, .3f, .3f).max()).isEqualTo(Option.some(entry(.3f, .3f)));
    }

    @Test
    public void shouldComputeMaxOfInt() {
        assertThat(mapOf(1, 1, 2, 2, 3, 3).max()).isEqualTo(Option.some(entry(3, 3)));
    }

    @Test
    public void shouldComputeMaxOfLong() {
        assertThat(mapOf(1L, 1L, 2L, 2L, 3L, 3L).max()).isEqualTo(Option.some(entry(3L, 3L)));
    }

    @Test
    public void shouldComputeMaxOfShort() {
        assertThat(mapOf((short) 1, (short) 1, (short) 2, (short) 2, (short) 3, (short) 3).max()).isEqualTo(Option.some(entry((short) 3, (short) 3)));
    }

    @Test
    public void shouldComputeMaxOfBigInteger() {
        assertThat(mapOf(BigInteger.ZERO, BigInteger.ZERO, BigInteger.ONE, BigInteger.ONE).max()).isEqualTo(Option.some(entry(BigInteger.ONE, BigInteger.ONE)));
    }

    @Test
    public void shouldComputeMaxOfBigDecimal() {
        assertThat(mapOf(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ONE, BigDecimal.ONE).max()).isEqualTo(Option.some(entry(BigDecimal.ONE, BigDecimal.ONE)));
    }

    @Test
    public void shouldThrowNPEWhenMaxOfNullAndInt() {
        assertThrows(NullPointerException.class, () -> this.<Integer, Integer>mapOf(null, 1).max());
    }

    @Test
    public void shouldThrowNPEWhenMaxOfIntAndNull() {
        assertThrows(NullPointerException.class, () -> this.<Integer, Integer>mapOf(1, null).max());
    }

    @Test
    public void shouldCalculateMaxOfDoublesContainingNaN() {
        // Double.compareTo, which the entries compare their keys with, orders NaN above every other double
        assertThat(mapOf(1.0, "a", Double.NaN, "b", 2.0, "c").max().get()._1()).isNaN();
    }

    @Test
    public void shouldCalculateMaxOfFloatsContainingNaN() {
        assertThat(mapOf(1.0f, "a", Float.NaN, "b", 2.0f, "c").max().get()._1()).isEqualTo(Float.NaN);
    }

    @Test
    public void shouldCalculateMaxOfDoublePositiveAndNegativeInfinity() {
        assertThat(mapOf(Double.POSITIVE_INFINITY, "+", Double.NEGATIVE_INFINITY, "-").max().get()._1()).isEqualTo(Double.POSITIVE_INFINITY);
    }

    @Test
    public void shouldCalculateMaxOfFloatPositiveAndNegativeInfinity() {
        assertThat(mapOf(Float.POSITIVE_INFINITY, "+", Float.NEGATIVE_INFINITY, "-").max().get()._1()).isEqualTo(Float.POSITIVE_INFINITY);
    }

    /** A map whose keys are not mutually comparable (a Double and a Float), so that comparing its entries fails. */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private Map<Comparable, String> mixedKeyMap() {
        return this.<Comparable, String>mapOfTuples((Tuple2) entry(1.0, "a"), (Tuple2) entry(1.0f, "b"));
    }

    @Test
    public void shouldThrowClassCastExceptionWhenTryingToCalculateMaxOfDoubleAndFloat() {
        assertThatThrownBy(() -> mixedKeyMap().max()).isInstanceOf(ClassCastException.class);
    }

    @Test
    public void shouldThrowClassCastExceptionWhenTryingToCalculateMinOfDoubleAndFloat() {
        assertThatThrownBy(() -> mixedKeyMap().min()).isInstanceOf(ClassCastException.class);
    }

    @Test
    public void shouldThrowOnConvertToSortedSetWithoutComparatorOnNonComparable() {
        assertThrows(ClassCastException.class, () -> mixedKeyMap().toSortedSet());
    }

    // -- maxBy(Comparator)

    @Test
    public void shouldThrowWhenMaxByWithNullComparator() {
        assertThrows(NullPointerException.class, () -> mapOf(1, 1).maxBy((Comparator<Tuple2<Integer, Integer>>) null));
    }

    @Test
    public void shouldThrowWhenMaxByOfNil() {
        assertThat(emptyMap().maxBy((o1, o2) -> 0)).isEqualTo(Option.none());
    }

    @Test
    public void shouldCalculateMaxByOfInts() {
        assertThat(entries(1, 2, 3).maxBy(comparingInt(Tuple2::_2))).isEqualTo(Option.some(entry(2, 3)));
    }

    @Test
    public void shouldCalculateInverseMaxByOfInts() {
        assertThat(entries(1, 2, 3).maxBy((t1, t2) -> t2._2() - t1._2())).isEqualTo(Option.some(entry(0, 1)));
    }

    // -- maxBy(Function)

    @Test
    public void shouldThrowWhenMaxByWithNullFunction() {
        assertThrows(NullPointerException.class, () -> mapOf(1, 1).maxBy((Function<Tuple2<Integer, Integer>, Integer>) null));
    }

    @Test
    public void shouldThrowWhenMaxByFunctionOfNil() {
        assertThat(this.<Integer, Integer>emptyMap().maxBy(Tuple2::_2)).isEqualTo(Option.none());
    }

    @Test
    public void shouldCalculateMaxByFunctionOfInts() {
        assertThat(entries(1, 2, 3).maxBy(Tuple2::_2)).isEqualTo(Option.some(entry(2, 3)));
    }

    @Test
    public void shouldCalculateInverseMaxByFunctionOfInts() {
        assertThat(entries(1, 2, 3).maxBy(t -> -t._2())).isEqualTo(Option.some(entry(0, 1)));
    }

    @Test
    public void shouldCallMaxFunctionOncePerElement() {
        final int[] cnt = {0};
        assertThat(entries(1, 2, 3).maxBy(t -> {
            cnt[0]++;
            return t._2();
        })).isEqualTo(Option.some(entry(2, 3)));
        assertThat(cnt[0]).isEqualTo(3);
    }

    // -- min

    @Test
    public void shouldReturnNoneWhenComputingMinOfNil() {
        assertThat(emptyMap().min()).isEqualTo(Option.none());
    }

    @Test
    public void shouldComputeMinOfOneValue() {
        assertThat(mapOf(5, 5).min()).isEqualTo(Option.some(entry(5, 5)));
    }

    @Test
    public void shouldComputeMinOfStrings() {
        assertThat(mapOf("1", "1", "2", "2", "3", "3").min()).isEqualTo(Option.some(entry("1", "1")));
    }

    @Test
    public void shouldComputeMinOfBoolean() {
        assertThat(mapOf(true, true, false, false).min()).isEqualTo(Option.some(entry(false, false)));
    }

    @Test
    public void shouldComputeMinOfByte() {
        assertThat(mapOf((byte) 1, (byte) 1, (byte) 2, (byte) 2).min()).isEqualTo(Option.some(entry((byte) 1, (byte) 1)));
    }

    @Test
    public void shouldComputeMinOfChar() {
        assertThat(mapOf('a', 'a', 'b', 'b', 'c', 'c').min()).isEqualTo(Option.some(entry('a', 'a')));
    }

    @Test
    public void shouldComputeMinOfDouble() {
        assertThat(mapOf(.1, .1, .2, .2, .3, .3).min()).isEqualTo(Option.some(entry(.1, .1)));
    }

    @Test
    public void shouldComputeMinOfFloat() {
        assertThat(mapOf(.1f, .1f, .2f, .2f, .3f, .3f).min()).isEqualTo(Option.some(entry(.1f, .1f)));
    }

    @Test
    public void shouldComputeMinOfInt() {
        assertThat(mapOf(1, 1, 2, 2, 3, 3).min()).isEqualTo(Option.some(entry(1, 1)));
    }

    @Test
    public void shouldComputeMinOfLong() {
        assertThat(mapOf(1L, 1L, 2L, 2L, 3L, 3L).min()).isEqualTo(Option.some(entry(1L, 1L)));
    }

    @Test
    public void shouldComputeMinOfShort() {
        assertThat(mapOf((short) 1, (short) 1, (short) 2, (short) 2, (short) 3, (short) 3).min()).isEqualTo(Option.some(entry((short) 1, (short) 1)));
    }

    @Test
    public void shouldComputeMinOfBigInteger() {
        assertThat(mapOf(BigInteger.ZERO, BigInteger.ZERO, BigInteger.ONE, BigInteger.ONE).min()).isEqualTo(Option.some(entry(BigInteger.ZERO, BigInteger.ZERO)));
    }

    @Test
    public void shouldComputeMinOfBigDecimal() {
        assertThat(mapOf(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ONE, BigDecimal.ONE).min()).isEqualTo(Option.some(entry(BigDecimal.ZERO, BigDecimal.ZERO)));
    }

    @Test
    public void shouldThrowNPEWhenMinOfNullAndInt() {
        assertThrows(NullPointerException.class, () -> this.<Integer, Integer>mapOf(null, 1).min());
    }

    @Test
    public void shouldThrowNPEWhenMinOfIntAndNull() {
        assertThrows(NullPointerException.class, () -> this.<Integer, Integer>mapOf(1, null).min());
    }

    @Test
    public void shouldCalculateMinOfDoublesContainingNaN() {
        // Double.compareTo, which the entries compare their keys with, orders NaN above every other double
        assertThat(mapOf(1.0, "a", Double.NaN, "b", 2.0, "c").min().get()._1()).isEqualTo(1.0);
    }

    @Test
    public void shouldCalculateMinOfFloatsContainingNaN() {
        assertThat(mapOf(1.0f, "a", Float.NaN, "b", 2.0f, "c").min().get()._1()).isEqualTo(1.0f);
    }

    @Test
    public void shouldCalculateMinOfDoublePositiveAndNegativeInfinity() {
        assertThat(mapOf(Double.POSITIVE_INFINITY, "+", Double.NEGATIVE_INFINITY, "-").min().get()._1()).isEqualTo(Double.NEGATIVE_INFINITY);
    }

    @Test
    public void shouldCalculateMinOfFloatPositiveAndNegativeInfinity() {
        assertThat(mapOf(Float.POSITIVE_INFINITY, "+", Float.NEGATIVE_INFINITY, "-").min().get()._1()).isEqualTo(Float.NEGATIVE_INFINITY);
    }

    // -- minBy(Comparator)

    @Test
    public void shouldThrowWhenMinByWithNullComparator() {
        assertThrows(NullPointerException.class, () -> mapOf(1, 1).minBy((Comparator<Tuple2<Integer, Integer>>) null));
    }

    @Test
    public void shouldThrowWhenMinByOfNil() {
        assertThat(emptyMap().minBy((o1, o2) -> 0)).isEqualTo(Option.none());
    }

    @Test
    public void shouldCalculateMinByOfInts() {
        assertThat(entries(1, 2, 3).minBy(comparingInt(Tuple2::_2))).isEqualTo(Option.some(entry(0, 1)));
    }

    @Test
    public void shouldCalculateInverseMinByOfInts() {
        assertThat(entries(1, 2, 3).minBy((t1, t2) -> t2._2() - t1._2())).isEqualTo(Option.some(entry(2, 3)));
    }

    // -- minBy(Function)

    @Test
    public void shouldThrowWhenMinByWithNullFunction() {
        assertThrows(NullPointerException.class, () -> mapOf(1, 1).minBy((Function<Tuple2<Integer, Integer>, Integer>) null));
    }

    @Test
    public void shouldThrowWhenMinByFunctionOfNil() {
        assertThat(this.<Integer, Integer>emptyMap().minBy(Tuple2::_2)).isEqualTo(Option.none());
    }

    @Test
    public void shouldCalculateMinByFunctionOfInts() {
        assertThat(entries(1, 2, 3).minBy(Tuple2::_2)).isEqualTo(Option.some(entry(0, 1)));
    }

    @Test
    public void shouldCalculateInverseMinByFunctionOfInts() {
        assertThat(entries(1, 2, 3).minBy(t -> -t._2())).isEqualTo(Option.some(entry(2, 3)));
    }

    @Test
    public void shouldCallMinFunctionOncePerElement() {
        final int[] cnt = {0};
        assertThat(entries(1, 2, 3).minBy(t -> {
            cnt[0]++;
            return t._2();
        })).isEqualTo(Option.some(entry(0, 1)));
        assertThat(cnt[0]).isEqualTo(3);
    }

    // -- orElse

    @Test
    public void shouldCaclEmptyOrElseSameOther() {
        final Map<Integer, Integer> other = mapOf(42, 42);
        assertThat(this.<Integer, Integer>emptyMap().orElse(other)).isEqualTo(other);
    }

    @Test
    public void shouldCaclEmptyOrElseEqualOther() {
        assertThat(this.<Integer, Integer>emptyMap().orElse(asList(entry(1, 1), entry(2, 2)))).isEqualTo(mapOf(1, 1, 2, 2));
    }

    @Test
    public void shouldCaclNonemptyOrElseOther() {
        final Map<Integer, Integer> src = mapOf(42, 42);
        assertThat(src.orElse(List.of(entry(1, 1)))).isSameAs(src);
    }

    @Test
    public void shouldCaclEmptyOrElseSameSupplier() {
        final Map<Integer, Integer> other = mapOf(42, 42);
        final Supplier<Iterable<Tuple2<Integer, Integer>>> supplier = () -> other;
        assertThat(this.<Integer, Integer>emptyMap().orElse(supplier)).isEqualTo(other);
    }

    @Test
    public void shouldCaclEmptyOrElseEqualSupplier() {
        assertThat(this.<Integer, Integer>emptyMap().orElse(() -> asList(entry(1, 1), entry(2, 2)))).isEqualTo(mapOf(1, 1, 2, 2));
    }

    @Test
    public void shouldCaclNonemptyOrElseSupplier() {
        final Map<Integer, Integer> src = mapOf(42, 42);
        assertThat(src.orElse(() -> List.of(entry(1, 1)))).isSameAs(src);
    }

    // -- partition

    @Test
    public void shouldThrowWhenPartitionNilAndPredicateIsNull() {
        assertThrows(NullPointerException.class, () -> emptyMap().partition(null));
    }

    @Test
    public void shouldPartitionNil() {
        assertThat(emptyMap().partition(e -> true)).isEqualTo(Tuple.of(emptyMap(), emptyMap()));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldPartitionIntsInOddAndEvenHavingOddAndEvenNumbers() {
        assertThat(entries(1, 2, 3, 4).partition(t -> t._2() % 2 != 0))
                .isEqualTo(Tuple.of(mapOfTuples(Tuple.of(0, 1), Tuple.of(2, 3)),
                        mapOfTuples(Tuple.of(1, 2), Tuple.of(3, 4))));
    }

    @Test
    public void shouldPartitionIntsInOddAndEvenHavingOnlyOddNumbers() {
        assertThat(entries(1, 3).partition(t -> t._2() % 2 != 0)).isEqualTo(Tuple.of(entries(1, 3), emptyMap()));
    }

    @Test
    public void shouldPartitionIntsInOddAndEvenHavingOnlyEvenNumbers() {
        assertThat(entries(2, 4).partition(t -> t._2() % 2 != 0)).isEqualTo(Tuple.of(emptyMap(), entries(2, 4)));
    }

    // -- reduceOption

    @Test
    public void shouldThrowWhenReduceOptionNil() {
        assertThat(this.<Integer, Integer>emptyMap().reduceOption(HashMapTest::sumOfEntries)).isSameAs(Option.none());
    }

    @Test
    public void shouldThrowWhenReduceOptionNullOperator() {
        assertThrows(NullPointerException.class, () -> this.<Integer, Integer>emptyMap().reduceOption(null));
    }

    @Test
    public void shouldReduceOptionNonNil() {
        assertThat(entries(1, 2, 3).reduceOption(HashMapTest::sumOfEntries)).isEqualTo(Option.some(entry(3, 6)));
    }

    // -- reduce

    @Test
    public void shouldThrowWhenReduceNil() {
        assertThrows(NoSuchElementException.class, () -> this.<Integer, Integer>emptyMap().reduce(HashMapTest::sumOfEntries));
    }

    @Test
    public void shouldThrowWhenReduceNullOperator() {
        assertThrows(NullPointerException.class, () -> this.<Integer, Integer>emptyMap().reduce(null));
    }

    @Test
    public void shouldReduceNonNil() {
        assertThat(entries(1, 2, 3).reduce(HashMapTest::sumOfEntries)).isEqualTo(entry(3, 6));
    }

    @Test
    public void shouldReduceEntriesOfOneAndTwoElementMaps() {
        assertThat(entries(7).reduce(HashMapTest::sumOfEntries)).isEqualTo(entry(0, 7));
        assertThat(entries(7, 8).reduce(HashMapTest::sumOfEntries)).isEqualTo(entry(1, 15));
        assertThat(entries(7, 8).fold(entry(100, 100), HashMapTest::sumOfEntries)).isEqualTo(entry(101, 115));
    }

    // -- replace(entry, entry)

    @Test
    public void shouldReplaceElementOfNilUsingCurrNew() {
        assertThat(this.<Integer, Integer>emptyMap().replace(entry(0, 1), entry(0, 2))).isEqualTo(emptyMap());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldReplaceFirstOccurrenceOfNonNilUsingCurrNewWhenMultipleOccurrencesExist() {
        final Map<Integer, Integer> testee = entries(0, 1, 2, 1);
        final Map<Integer, Integer> actual = testee.replace(entry(1, 1), entry(1, 3));
        final Map<Integer, Integer> expected = mapOfTuples(entry(0, 0), entry(1, 3), entry(2, 2), entry(3, 1));
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldReplaceElementOfNonNilUsingCurrNewWhenOneOccurrenceExists() {
        assertThat(entries(0, 1, 2).replace(entry(1, 1), entry(1, 3))).isEqualTo(mapOfTuples(entry(0, 0), entry(1, 3), entry(2, 2)));
    }

    @Test
    public void shouldReplaceElementOfNonNilUsingCurrNewWhenNoOccurrenceExists() {
        assertThat(entries(0, 1, 2).replace(entry(1, 33), entry(1, 3))).isEqualTo(entries(0, 1, 2));
    }

    // -- replaceAll(entry, entry)

    @Test
    public void shouldReplaceAllElementsOfNilUsingCurrNew() {
        assertThat(this.<Integer, Integer>emptyMap().replaceAll(entry(0, 1), entry(0, 2))).isEqualTo(emptyMap());
    }

    @Test
    public void shouldReplaceAllElementsOfNonNilUsingCurrNonExistingNew() {
        assertThat(entries(0, 1, 2, 1).replaceAll(entry(1, 33), entry(1, 3))).isEqualTo(entries(0, 1, 2, 1));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldReplaceAllElementsOfNonNilUsingCurrNew() {
        // a map holds an entry once, so replaceAll replaces that one entry
        assertThat(entries(0, 1, 2, 1).replaceAll(entry(1, 1), entry(1, 3))).isEqualTo(mapOfTuples(entry(0, 0), entry(1, 3), entry(2, 2), entry(3, 1)));
    }

    // -- retainAll

    @Test
    public void shouldRetainAllElementsFromNil() {
        final Map<Integer, Integer> empty = emptyMap();
        final Map<Integer, Integer> actual = empty.retainAll(entries(1, 2, 3));
        assertThat(actual).isEqualTo(empty);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldRetainAllExistingElementsFromNonNil() {
        final Map<Integer, Integer> src = entries(1, 2, 3, 2, 1, 3);
        final Map<Integer, Integer> expected = mapOfTuples(entry(0, 1), entry(1, 2), entry(3, 2), entry(4, 1));
        final Map<Integer, Integer> actual = src.retainAll(List.of(entry(0, 1), entry(1, 2), entry(3, 2), entry(4, 1)));
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void shouldRetainAllElementsFromNonNil() {
        final Map<Integer, Integer> src = entries(1, 2, 1, 2, 2);
        final Map<Integer, Integer> actual = src.retainAll(entries(1, 2, 1, 2, 2));
        assertThat(actual).isEqualTo(src);
    }

    @Test
    public void shouldNotRetainAllNonExistingElementsFromNonNil() {
        final Map<Integer, Integer> src = entries(1, 2, 3);
        final Map<Integer, Integer> actual = src.retainAll(List.of(entry(0, 4), entry(9, 5)));
        assertThat(actual).isEqualTo(emptyMap());
    }

    // -- single

    @Test
    public void shouldSingleFailEmpty() {
        assertThrows(NoSuchElementException.class, () -> emptyMap().single());
    }

    @Test
    public void shouldSingleFailTwo() {
        assertThrows(NoSuchElementException.class, () -> entries(1, 2).single());
    }

    @Test
    public void shouldSingleWork() {
        assertThat(entries(1).single()).isEqualTo(entry(0, 1));
    }

    // -- singleOption

    @Test
    public void shouldSingleOptionFailEmpty() {
        assertThat(emptyMap().singleOption()).isEqualTo(Option.none());
    }

    @Test
    public void shouldSingleOptionFailTwo() {
        assertThat(entries(1, 2).singleOption()).isEqualTo(Option.none());
    }

    @Test
    public void shouldSingleOptionWork() {
        assertThat(entries(1).singleOption()).isEqualTo(Option.some(entry(0, 1)));
    }

    // -- tap

    @Test
    public void shouldTapNil() {
        assertThat(emptyMap().tap(t -> {})).isEqualTo(emptyMap());
    }

    @Test
    public void shouldTapNonNilPerformingNoAction() {
        assertThat(entries(1).tap(t -> {})).isEqualTo(entries(1));
    }

    @Test
    public void shouldTapSingleValuePerformingAnAction() {
        final int[] effect = {0};
        final Map<Integer, Integer> actual = entries(1).tap(t -> effect[0] = t._2());
        assertThat(actual).isEqualTo(entries(1));
        assertThat(effect[0]).isEqualTo(1);
    }

    @Test
    public void shouldTapEveryElement() {
        final int[] sum = {0};
        final Map<Integer, Integer> actual = entries(1, 2, 3).tap(t -> sum[0] += t._2());
        assertThat(actual).isEqualTo(entries(1, 2, 3));
        assertThat(sum[0]).isEqualTo(6);
    }

    @Test
    public void shouldReturnThisOnTapOfEagerCollection() {
        final Map<Integer, Integer> testee = entries(1, 2, 3);
        assertThat(testee.tap(t -> {})).isSameAs(testee);
    }

    @Test
    public void shouldThrowOnTapWithNullAction() {
        assertThrows(NullPointerException.class, () -> entries(1).tap(null));
    }

    @Test
    public void shouldPropagateWhatTheTapActionThrows() {
        assertThrows(IllegalStateException.class, () -> entries(1, 2).tap(t -> {
            throw new IllegalStateException();
        }).size());
    }

    // -- collect(Collector)

    @Test
    public void shouldCollectWithACollector() {
        final java.util.List<Tuple2<Integer, Integer>> actual = entries(1, 2, 3).collect(java.util.stream.Collectors.toList());
        assertThat(actual).containsExactlyInAnyOrder(entry(0, 1), entry(1, 2), entry(2, 3));
    }

    @Test
    public void shouldCollectWithSupplierAccumulatorAndCombiner() {
        final ArrayList<Tuple2<Integer, Integer>> actual = entries(1, 2, 3).collect(ArrayList<Tuple2<Integer, Integer>>::new, ArrayList::add, ArrayList::addAll);
        assertThat(actual).containsExactlyInAnyOrder(entry(0, 1), entry(1, 2), entry(2, 3));
    }

    // -- toArray

    @Test
    public void shouldConvertNilToJavaArray() {
        final Object[] actual = emptyMap().toArray();
        assertThat(actual).isEqualTo(new Object[]{});
    }

    @Test
    public void shouldConvertNonNilToJavaArray() {
        final Object[] array = entries(1, 2).toArray();
        assertThat(array).containsExactlyInAnyOrder(entry(0, 1), entry(1, 2));
    }

    // -- the conversions

    @Test
    public void shouldConvertToHashMap() {
        assertThat(entries(9, 5, 1).toMap(t -> Tuple.of(t._2(), t._2()))).isEqualTo(HashMap.of(1, 1, 5, 5, 9, 9));
        assertThat(this.<Integer, Integer>emptyMap().toMap(t -> Tuple.of(t._2(), t._2()))).isSameAs(HashMap.empty());
    }

    @Test
    public void shouldConvertToHashMapTwoFunctions() {
        assertThat(entries(9, 5, 1).toMap(Tuple2::_2, Tuple2::_2)).isEqualTo(HashMap.of(1, 1, 5, 5, 9, 9));
    }

    @Test
    public void shouldConvertToLinkedMap() {
        assertThat(entries(1, 5, 9).toLinkedMap(t -> Tuple.of(t._2(), t._2()))).isEqualTo(LinkedHashMap.of(1, 1, 5, 5, 9, 9));
        assertThat(this.<Integer, Integer>emptyMap().toLinkedMap(t -> Tuple.of(t._2(), t._2()))).isSameAs(LinkedHashMap.empty());
    }

    @Test
    public void shouldConvertToLinkedMapTwoFunctions() {
        assertThat(entries(1, 5, 9).toLinkedMap(Tuple2::_2, Tuple2::_2)).isEqualTo(LinkedHashMap.of(1, 1, 5, 5, 9, 9));
    }

    @Test
    public void shouldConvertToSortedMap() {
        assertThat(entries(9, 5, 1).toSortedMap(t -> Tuple.of(t._2(), t._2()))).isEqualTo(TreeMap.of(1, 1, 5, 5, 9, 9));
        assertThat(this.<Integer, Integer>emptyMap().toSortedMap(t -> Tuple.of(t._2(), t._2()))).isEqualTo(TreeMap.empty());
    }

    @Test
    public void shouldConvertToSortedMapTwoFunctions() {
        assertThat(entries(9, 5, 1).toSortedMap(Tuple2::_2, Tuple2::_2)).isEqualTo(TreeMap.of(1, 1, 5, 5, 9, 9));
    }

    @Test
    public void shouldConvertToSortedMapWithComparator() {
        final Comparator<Integer> comparator = ((Comparator<Integer>) Integer::compareTo).reversed();
        assertThat(entries(9, 5, 1).toSortedMap(comparator, t -> Tuple.of(t._2(), t._2()))).isEqualTo(TreeMap.of(comparator, 9, 9, 5, 5, 1, 1));
    }

    @Test
    public void shouldConvertToSortedMapTwoFunctionsWithComparator() {
        final Comparator<Integer> comparator = ((Comparator<Integer>) Integer::compareTo).reversed();
        assertThat(entries(9, 5, 1).toSortedMap(comparator, Tuple2::_2, Tuple2::_2)).isEqualTo(TreeMap.of(comparator, 9, 9, 5, 5, 1, 1));
    }

    @Test
    public void shouldConvertToQueue() {
        assertThat(entries(1, 2, 3).toQueue()).isEqualTo(Queue.of(entry(0, 1), entry(1, 2), entry(2, 3)));
        assertThat(emptyMap().toQueue()).isSameAs(Queue.empty());
    }

    @Test
    public void shouldConvertToLinkedSet() {
        final Map<Integer, Integer> value = entries(3, 7, 1, 15, 0);
        final Set<Tuple2<Integer, Integer>> set = value.toLinkedSet();
        assertThat(set).isEqualTo(value.toList().foldLeft(LinkedHashSet.empty(), LinkedHashSet::add));
        Assertions.assertThat(new java.util.ArrayList<>(set.asJava())).isEqualTo(new java.util.ArrayList<>(value.asJava()));
        assertThat(emptyMap().toLinkedSet()).isSameAs(LinkedHashSet.empty());
    }

    @Test
    public void shouldConvertToSortedSetWithoutComparatorOnComparable() {
        assertThat(entries(3, 7, 1, 15, 0).toSortedSet()).isEqualTo(TreeSet.of(entry(0, 3), entry(1, 7), entry(2, 1), entry(3, 15), entry(4, 0)));
    }

    @Test
    public void shouldConvertToSortedSet() {
        final Comparator<Tuple2<Integer, Integer>> comparator = Comparator.comparingInt(t -> Integer.bitCount(t._2()));
        assertThat(entries(3, 7, 1, 15, 0).toSortedSet(comparator.reversed()))
                .isEqualTo(TreeSet.of(comparator.reversed(), entry(0, 3), entry(1, 7), entry(2, 1), entry(3, 15), entry(4, 0)));
    }

    @Test
    public void shouldConvertToStream() {
        assertThat(entries(1, 2, 3).toStream()).isEqualTo(Stream.of(entry(0, 1), entry(1, 2), entry(2, 3)));
        assertThat(emptyMap().toStream()).isSameAs(Stream.empty());
    }

    // -- values

    @Nested
    class ValuesTests {
        @Test
        @SuppressWarnings("unchecked")
        public void shouldReturnValuesVector() {
            final Vector<Integer> actual = mapOfTuples(Tuple.of(1, 11), Tuple.of(2, 22), Tuple.of(3, 33)).values();
            assertThat(actual).isEqualTo(Vector.of(11, 22, 33));
        }

        @Test
        public void shouldReturnValuesInTheIterationOrderOfTheMap() {
            final Map<Integer, String> map = mapOf(3, "c", 1, "a", 2, "b");
            Assertions.assertThat(new java.util.ArrayList<>(map.values().asJava())).isEqualTo(new java.util.ArrayList<>(map.toList().map(Tuple2::_2).asJava()));
            Assertions.assertThat(new java.util.ArrayList<>(map.values().asJava())).isEqualTo(new java.util.ArrayList<>(map.keySet().toList().map(k -> map.get(k).get()).asJava()));
        }

        @Test
        public void shouldReturnNoValuesOfAnEmptyMap() {
            assertThat(emptyMap().values()).isSameAs(Vector.empty());
        }
    }

    // -- asJava

    @Nested
    class AsJavaTests {
        @Test
        public void shouldViewTheEntriesAsAJavaCollection() {
            final Map<Integer, String> map = mapOf(1, "a", 2, "b", 3, "c");
            final java.util.Collection<Tuple2<Integer, String>> view = map.asJava();
            assertThat(view.size()).isEqualTo(3);
            assertThat(view.contains(entry(2, "b"))).isTrue();
            assertThat(view.contains(entry(2, "x"))).isFalse();
            Assertions.assertThat(new ArrayList<>(view)).isEqualTo(new java.util.ArrayList<>(map.asJava()));
            assertThrows(UnsupportedOperationException.class, () -> view.add(entry(4, "d")));
            assertThrows(UnsupportedOperationException.class, view::clear);
        }
    }

        @Test
        public void shouldRefuseClearOnAnEmptyJavaView() {
            // unlike a java.util.List view, whose clear() on an empty list is a no-op as in the JDK's AbstractList,
            // the Collection view throws on every mutator, as Collections.unmodifiableCollection does
            assertThrows(UnsupportedOperationException.class, () -> HashMap.<Integer, Integer>empty().asJava().clear());
        }

    // -- HashMap

    protected String className() {
        return "HashMap";
    }

    <T1, T2> java.util.Map<T1, T2> javaEmptyMap() {
        return new java.util.HashMap<>();
    }

    protected <T1 extends Comparable<? super T1>, T2> HashMap<T1, T2> emptyMap() {
        return HashMap.empty();
    }

    protected <K extends Comparable<? super K>, V, T extends V> Collector<T, ?, ? extends Map<K, V>> collectorWithMapper(Function<? super T, ? extends K> keyMapper) {
        return HashMap.collector(keyMapper);
    }

    protected <K extends Comparable<? super K>, V, T> Collector<T, ?, ? extends Map<K, V>> collectorWithMappers(Function<? super T, ? extends K> keyMapper, Function<? super T, ? extends V> valueMapper) {
        return HashMap.collector(keyMapper, valueMapper);
    }

    protected <T> Collector<Tuple2<Integer, T>, ?, ? extends Map<Integer, T>> mapCollector() {
        return HashMap.collector();
    }

    @SuppressWarnings("varargs")
    @SafeVarargs
    protected final <K extends Comparable<? super K>, V> HashMap<K, V> mapOfTuples(Tuple2<? extends K, ? extends V>... entries) {
        return HashMap.ofEntries(entries);
    }

    protected <K extends Comparable<? super K>, V> Map<K, V> mapOfTuples(Iterable<? extends Tuple2<? extends K, ? extends V>> entries) {
        return HashMap.ofEntries(entries);
    }

    @SuppressWarnings("varargs")
    @SafeVarargs
    protected final <K extends Comparable<? super K>, V> HashMap<K, V> mapOfEntries(java.util.Map.Entry<? extends K, ? extends V>... entries) {
        return HashMap.ofEntries(entries);
    }

    protected <K extends Comparable<? super K>, V> HashMap<K, V> mapOf(K k1, V v1) {
        return HashMap.of(k1, v1);
    }

    protected <K extends Comparable<? super K>, V> HashMap<K, V> mapOf(K k1, V v1, K k2, V v2) {
        return HashMap.of(k1, v1, k2, v2);
    }

    protected <K extends Comparable<? super K>, V> HashMap<K, V> mapOf(K k1, V v1, K k2, V v2, K k3, V v3) {
        return HashMap.of(k1, v1, k2, v2, k3, v3);
    }

    protected <T, K extends Comparable<? super K>, V> Map<K, V> mapOf(java.util.stream.Stream<? extends T> stream, Function<? super T, ? extends K> keyMapper, Function<? super T, ? extends V> valueMapper) {
        return HashMap.ofAll(stream, keyMapper, valueMapper);
    }

    protected <T, K extends Comparable<? super K>, V> Map<K, V> mapOf(java.util.stream.Stream<? extends T> stream, Function<? super T, Tuple2<? extends K, ? extends V>> f) {
        return HashMap.ofAll(stream, f);
    }

    protected <K extends Comparable<? super K>, V> HashMap<K, V> mapTabulate(int n, Function<? super Integer, ? extends Tuple2<? extends K, ? extends V>> f) {
        return HashMap.tabulate(n, f);
    }

    protected <K extends Comparable<? super K>, V> HashMap<K, V> mapFill(int n, Supplier<? extends Tuple2<? extends K, ? extends V>> s) {
        return HashMap.fill(n, s);
    }

    @Nested
    class StaticNarrowTests {
        @Test
        public void shouldNarrowHashMap() {
            final HashMap<Integer, Double> int2doubleMap = mapOf(1, 1.0d);
            final HashMap<Number, Number> number2numberMap = HashMap.narrow(int2doubleMap);
            final int actual = number2numberMap.put(new BigDecimal("2"), new BigDecimal("2.0")).values().sum().intValue();
            assertThat(actual).isEqualTo(3);
        }

        @Test
        public void shouldWrapMap() {
            final java.util.Map<Integer, Integer> source = new java.util.HashMap<>();
            source.put(1, 2);
            source.put(3, 4);
            assertThat(HashMap.ofAll(source)).isEqualTo(emptyIntInt().put(1, 2).put(3, 4));
        }
    }

    @Nested
    class SpecificTests {
        @Test
        public void shouldRejectNullKeyOnPut() {
            Assertions.assertThatNullPointerException().isThrownBy(() -> HashMap.empty().put(null, 1));
        }

        @Test
        public void shouldRejectNullValueOnPut() {
            Assertions.assertThatNullPointerException().isThrownBy(() -> HashMap.<Integer, Integer>empty().put(1, null));
        }

        @Test
        public void shouldRejectNullEntryInOfEntries() {
            Assertions.assertThatNullPointerException()
                    .isThrownBy(() -> HashMap.ofEntries((Tuple2<Integer, String>) null))
                    .withMessage("HashMap.ofEntries: entry is null");
            Assertions.assertThatNullPointerException()
                    .isThrownBy(() -> HashMap.ofEntries((java.util.Map.Entry<Integer, String>) null))
                    .withMessage("HashMap.ofEntries: entry is null");
            final java.util.List<Tuple2<Integer, String>> withNullEntry = new java.util.ArrayList<>();
            withNullEntry.add(Tuple.of(1, "a"));
            withNullEntry.add(null);
            Assertions.assertThatNullPointerException()
                    .isThrownBy(() -> HashMap.ofEntries(withNullEntry))
                    .withMessage("HashMap.ofEntries: entry is null");
        }

        @Test
        public void shouldCalculateBigHashCode() {
            HashMap<Integer, Integer> h1 = HashMap.empty();
            HashMap<Integer, Integer> h2 = HashMap.empty();
            final int count = 1234;
            for (int i = 0; i <= count; i++) {
                h1 = h1.put(i, i);
                h2 = h2.put(count - i, count - i);
            }
            Assertions.assertThat(h1.hashCode() == h2.hashCode()).isTrue();
        }

        @Test
        public void shouldEqualsIgnoreOrder() {
            HashMap<String, Integer> map = HashMap.<String, Integer> empty().put("Aa", 1).put("BB", 2);
            HashMap<String, Integer> map2 = HashMap.<String, Integer> empty().put("BB", 2).put("Aa", 1);
            Assertions.assertThat(map.hashCode()).isEqualTo(map2.hashCode());
            Assertions.assertThat(map).isEqualTo(map2);
        }
    }

    @Nested
    class SpliteratorTests {
        @Test
        public void shouldNotHaveSortedSpliterator() {
            assertThat(of(1, 2, 3).spliterator().hasCharacteristics(Spliterator.SORTED)).isFalse();
        }

        @Test
        public void shouldNotHaveOrderedSpliterator() {
            assertThat(of(1, 2, 3).spliterator().hasCharacteristics(Spliterator.ORDERED)).isFalse();
        }
    }

    // -- no positional member: the hash order is not a promised order

    @Nested
    class NoPositionalMembersTests {
        @Test
        public void shouldDeclareNoPositionalMemberOnTheHashTypeNorItsInterface() {
            for (Class<?> type : java.util.List.<Class<?>> of(HashMap.class, Map.class)) {
                final java.util.Set<String> names = new java.util.HashSet<>();
                for (java.lang.reflect.Method method : type.getMethods()) {
                    names.add(method.getName());
                }
                for (java.lang.reflect.Method method : type.getDeclaredMethods()) {
                    names.add(method.getName());
                }
                names.retainAll(ORDERED_POSITIONAL_MEMBERS);
                org.junit.jupiter.api.Assertions.assertEquals(java.util.Set.of(), names, type.getSimpleName());
            }
        }
    }
}
