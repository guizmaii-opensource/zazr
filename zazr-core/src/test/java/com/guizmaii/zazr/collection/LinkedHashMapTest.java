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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The LinkedHashMap API: the shared cases over the values (through {@code IntMap}), the map cases over the entries, and the insertion-order ones. */
public class LinkedHashMapTest extends AbstractTraversableTest {

    @Override
    protected <T> IterableAssert<T> assertThat(Iterable<T> actual) {
        return new IterableAssert<T>(actual) {
            @Override
            public IterableAssert<T> isEqualTo(Object obj) {
                @SuppressWarnings("unchecked")
                final Iterable<T> expected = (Iterable<T>) obj;
                final java.util.Map<T, Integer> actualMap = countMap(actual);
                final java.util.Map<T, Integer> expectedMap = countMap(expected);
                LinkedHashMapTest.super.assertThat(actualMap.size()).isEqualTo(expectedMap.size());
                actualMap.forEach((k, v) -> LinkedHashMapTest.super.assertThat(v).isEqualTo(expectedMap.get(k)));
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
        final Collector<Tuple2<Integer, T>, ArrayList<Tuple2<Integer, T>>, ? extends Map<Integer, T>> mapCollector = mapCollector();
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
                return (left, right) -> fromTuples(mapCollector.combiner().apply(toTuples(left), toTuples(right)));
            }

            @Override
            public Function<ArrayList<T>, IntMap<T>> finisher() {
                return LinkedHashMapTest.this::ofAll;
            }

            @Override
            public java.util.Set<Characteristics> characteristics() {
                return mapCollector.characteristics();
            }

            private ArrayList<Tuple2<Integer, T>> toTuples(java.util.List<T> list) {
                final ArrayList<Tuple2<Integer, T>> result = new ArrayList<>();
                Stream.ofAll(list)
                        .zipWithIndex()
                        .map(tu -> Tuple.of(tu._2(), tu._1()))
                        .forEach(result::add);
                return result;
            }

            private ArrayList<T> fromTuples(java.util.List<Tuple2<Integer, T>> list) {
                final ArrayList<T> result = new ArrayList<>();
                Stream.ofAll(list)
                        .map(tu -> tu._2())
                        .forEach(result::add);
                return result;
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
                .mapKeys(k -> k * 118).mapKeys(Integer::toHexString).mapKeys(LinkedHashMapTest::md5).mapKeys(String::length);
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
                .mapKeys(k -> k * 118).mapKeys(Integer::toHexString).mapKeys(LinkedHashMapTest::md5)//Unique key mappers
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
            assertThatNullPointerException().isThrownBy(() -> LinkedHashMapTest.this.<String, String>mapOf("1", null));
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
            assertThatNullPointerException().isThrownBy(() -> LinkedHashMapTest.this.<String, String>mapOf("k", null));
        }

        @Test
        public void shouldRejectPutOfNullValue() {
            assertThatNullPointerException().isThrownBy(() -> LinkedHashMapTest.this.<String, String>mapOf("k", "v").put("k", null));
        }

        @Test
        public void shouldRejectPutWithMergeResultingInNull() {
            assertThatNullPointerException().isThrownBy(() -> LinkedHashMapTest.this.<String, String>mapOf("k", "v").put("k", "w", (a, b) -> null));
        }

        @Test
        public void shouldRejectComputeIfPresentWithNullResult() {
            assertThatNullPointerException().isThrownBy(() -> LinkedHashMapTest.this.<String, String>mapOf("k", "v").computeIfPresent("k", (k, v) -> null));
        }

        @Test
        public void shouldRejectComputeIfAbsentWithNullResult() {
            assertThatNullPointerException().isThrownBy(() -> LinkedHashMapTest.this.<String, String>emptyMap().computeIfAbsent("k", k -> null));
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
            final Tuple2<String, ? extends Map<String, String>> result = LinkedHashMapTest.this.<String, String>emptyMap().computeIfAbsent("k", k -> "computed");
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
            final Map<String, String> map = LinkedHashMapTest.this.<String, String>emptyMap().put("k", "v", (a, b) -> "merged");
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
        assertThat(this.<Integer, Integer>emptyMap().fold(entry(0, 0), LinkedHashMapTest::sumOfEntries)).isEqualTo(entry(0, 0));
    }

    @Test
    public void shouldThrowWhenFoldNullOperator() {
        assertThrows(NullPointerException.class, () -> this.<Integer, Integer>emptyMap().fold(null, null));
    }

    @Test
    public void shouldFoldSingleElement() {
        assertThat(entries(1).fold(entry(0, 0), LinkedHashMapTest::sumOfEntries)).isEqualTo(entry(0, 1));
    }

    @Test
    public void shouldFoldMultipleElements() {
        assertThat(entries(1, 2, 3).fold(entry(0, 0), LinkedHashMapTest::sumOfEntries)).isEqualTo(entry(3, 6));
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
        assertThat(this.<Integer, Integer>emptyMap().reduceOption(LinkedHashMapTest::sumOfEntries)).isSameAs(Option.none());
    }

    @Test
    public void shouldThrowWhenReduceOptionNullOperator() {
        assertThrows(NullPointerException.class, () -> this.<Integer, Integer>emptyMap().reduceOption(null));
    }

    @Test
    public void shouldReduceOptionNonNil() {
        assertThat(entries(1, 2, 3).reduceOption(LinkedHashMapTest::sumOfEntries)).isEqualTo(Option.some(entry(3, 6)));
    }

    // -- reduce

    @Test
    public void shouldThrowWhenReduceNil() {
        assertThrows(NoSuchElementException.class, () -> this.<Integer, Integer>emptyMap().reduce(LinkedHashMapTest::sumOfEntries));
    }

    @Test
    public void shouldThrowWhenReduceNullOperator() {
        assertThrows(NullPointerException.class, () -> this.<Integer, Integer>emptyMap().reduce(null));
    }

    @Test
    public void shouldReduceNonNil() {
        assertThat(entries(1, 2, 3).reduce(LinkedHashMapTest::sumOfEntries)).isEqualTo(entry(3, 6));
    }

    @Test
    public void shouldReduceEntriesOfOneAndTwoElementMaps() {
        assertThat(entries(7).reduce(LinkedHashMapTest::sumOfEntries)).isEqualTo(entry(0, 7));
        assertThat(entries(7, 8).reduce(LinkedHashMapTest::sumOfEntries)).isEqualTo(entry(1, 15));
        assertThat(entries(7, 8).fold(entry(100, 100), LinkedHashMapTest::sumOfEntries)).isEqualTo(entry(101, 115));
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

    // -- LinkedHashMap

    protected String className() {
        return "LinkedHashMap";
    }

    <T1, T2> java.util.Map<T1, T2> javaEmptyMap() {
        return new java.util.LinkedHashMap<>();
    }

    protected <T1 extends Comparable<? super T1>, T2> LinkedHashMap<T1, T2> emptyMap() {
        return LinkedHashMap.empty();
    }

    protected <K extends Comparable<? super K>, V, T extends V> Collector<T, ArrayList<T>, ? extends Map<K, V>> collectorWithMapper(Function<? super T, ? extends K> keyMapper) {
        return LinkedHashMap.collector(keyMapper);
    }

    protected <K extends Comparable<? super K>, V, T> Collector<T, ArrayList<T>, ? extends Map<K, V>> collectorWithMappers(Function<? super T, ? extends K> keyMapper, Function<? super T, ? extends V> valueMapper) {
        return LinkedHashMap.collector(keyMapper, valueMapper);
    }

    protected <T> Collector<Tuple2<Integer, T>, ArrayList<Tuple2<Integer, T>>, ? extends Map<Integer, T>> mapCollector() {
        return LinkedHashMap.collector();
    }

    @SuppressWarnings("varargs")
    @SafeVarargs
    protected final <K extends Comparable<? super K>, V> LinkedHashMap<K, V> mapOfTuples(Tuple2<? extends K, ? extends V>... entries) {
        return LinkedHashMap.ofEntries(entries);
    }

    protected <K extends Comparable<? super K>, V> Map<K, V> mapOfTuples(Iterable<? extends Tuple2<? extends K, ? extends V>> entries) {
        return LinkedHashMap.ofEntries(entries);
    }

    @SuppressWarnings("varargs")
    @SafeVarargs
    protected final <K extends Comparable<? super K>, V> LinkedHashMap<K, V> mapOfEntries(java.util.Map.Entry<? extends K, ? extends V>... entries) {
        return LinkedHashMap.ofEntries(entries);
    }

    protected <K extends Comparable<? super K>, V> LinkedHashMap<K, V> mapOf(K k1, V v1) {
        return LinkedHashMap.of(k1, v1);
    }

    protected <K extends Comparable<? super K>, V> Map<K, V> mapOf(K k1, V v1, K k2, V v2) {
        return LinkedHashMap.of(k1, v1, k2, v2);
    }

    protected <K extends Comparable<? super K>, V> Map<K, V> mapOf(K k1, V v1, K k2, V v2, K k3, V v3) {
        return LinkedHashMap.of(k1, v1, k2, v2, k3, v3);
    }

    protected <T, K extends Comparable<? super K>, V> Map<K, V> mapOf(java.util.stream.Stream<? extends T> stream, Function<? super T, ? extends K> keyMapper, Function<? super T, ? extends V> valueMapper) {
        return LinkedHashMap.ofAll(stream, keyMapper, valueMapper);
    }

    protected <T, K extends Comparable<? super K>, V> Map<K, V> mapOf(java.util.stream.Stream<? extends T> stream, Function<? super T, Tuple2<? extends K, ? extends V>> f) {
        return LinkedHashMap.ofAll(stream, f);
    }

    protected <K extends Comparable<? super K>, V> LinkedHashMap<K, V> mapTabulate(int n, Function<? super Integer, ? extends Tuple2<? extends K, ? extends V>> f) {
        return LinkedHashMap.tabulate(n, f);
    }

    protected <K extends Comparable<? super K>, V> LinkedHashMap<K, V> mapFill(int n, Supplier<? extends Tuple2<? extends K, ? extends V>> s) {
        return LinkedHashMap.fill(n, s);
    }

    @Test
    public void shouldKeepOrder() {
        final List<Character> actual = LinkedHashMap.<Integer, Character> empty().put(3, 'a').put(2, 'b').put(1, 'c').foldLeft(List.empty(), (s, t) -> s.append(t._2()));
        Assertions.assertThat(actual).isEqualTo(List.of('a', 'b', 'c'));
    }

    @Test
    public void shouldKeepValuesOrder() {
        final List<Character> actual = LinkedHashMap.<Integer, Character> empty().put(3, 'a').put(2, 'b').put(1, 'c').values().foldLeft(List.empty(), List::append);
        Assertions.assertThat(actual).isEqualTo(List.of('a', 'b', 'c'));
    }

    @Nested
    class StaticNarrowTests {
        @Test
        public void shouldNarrowLinkedHashMap() {
            final LinkedHashMap<Integer, Double> int2doubleMap = mapOf(1, 1.0d);
            final LinkedHashMap<Number, Number> number2numberMap = LinkedHashMap.narrow(int2doubleMap);
            final int actual = number2numberMap.put(new BigDecimal("2"), new BigDecimal("2.0")).values().sum().intValue();
            assertThat(actual).isEqualTo(3);
        }
    }

    @Nested
    class StaticOfallIterableTests {
        @Test
        public void shouldWrapMap() {
            final java.util.Map<Integer, Integer> source = new java.util.HashMap<>();
            source.put(1, 2);
            source.put(3, 4);
            assertThat(LinkedHashMap.ofAll(source)).isEqualTo(emptyIntInt().put(1, 2).put(3, 4));
        }
    }

    @Nested
    class StaticOfentriesTests {
        @Test
        public void shouldRejectNullEntryInOfEntries() {
            Assertions.assertThatNullPointerException()
                    .isThrownBy(() -> LinkedHashMap.ofEntries((Tuple2<Integer, String>) null))
                    .withMessage("LinkedHashMap.ofEntries: entry is null");
            Assertions.assertThatNullPointerException()
                    .isThrownBy(() -> LinkedHashMap.ofEntries((java.util.Map.Entry<Integer, String>) null))
                    .withMessage("LinkedHashMap.ofEntries: entry is null");
            final java.util.List<Tuple2<Integer, String>> withNullEntry = new java.util.ArrayList<>();
            withNullEntry.add(Tuple.of(1, "a"));
            withNullEntry.add(null);
            Assertions.assertThatNullPointerException()
                    .isThrownBy(() -> LinkedHashMap.ofEntries(withNullEntry))
                    .withMessage("LinkedHashMap.ofEntries: entry is null");
        }
    }

    @Nested
    class LinkedHashMapKeysetTests {
        @Test
        public void shouldKeepKeySetOrder() {
            final Set<Integer> keySet = LinkedHashMap.of(4, "d", 1, "a", 2, "b").keySet();
            assertThat(keySet.mkString()).isEqualTo("412");
        }
    }

    /// The key set of a LinkedHashMap is a LinkedHashSet over the map itself, whose values are not the keys: every
    /// operation on it matches a LinkedHashSet built from the same keys, order included.
    @Nested
    class KeySetAsLinkedHashSetTests {
        // values that differ from the keys, so an operation that looks at them cannot pass by accident
        private final LinkedHashMap<String, Integer> map = LinkedHashMap.of("a", 1, "b", 2, "c", 3, "d", 4);

        private LinkedHashSet<String> keys(LinkedHashMap<String, Integer> source) {
            return (LinkedHashSet<String>) source.keySet();
        }

        private LinkedHashSet<String> sameKeys(LinkedHashMap<String, Integer> source) {
            return LinkedHashSet.ofAll(source.toList().map(Tuple2::_1));
        }

        private void assertSameOrder(Set<String> actual, Set<String> expected, List<String> literal) {
            Assertions.assertThat(actual.toList()).isEqualTo(literal);
            Assertions.assertThat(expected.toList()).isEqualTo(literal);
        }

        @Test
        public void replacePresentElement() {
            assertSameOrder(keys(map).replace("b", "z"), sameKeys(map).replace("b", "z"), List.of("a", "z", "c", "d"));
        }

        @Test
        public void replaceAbsentElement() {
            final LinkedHashSet<String> keys = keys(map);
            Assertions.assertThat(keys.replace("x", "z")).isSameAs(keys);
            assertSameOrder(keys.replace("x", "z"), sameKeys(map).replace("x", "z"), List.of("a", "b", "c", "d"));
        }

        @Test
        public void replaceByItself() {
            final LinkedHashSet<String> keys = keys(map);
            Assertions.assertThat(keys.replace("b", "b")).isSameAs(keys);
        }

        @Test
        public void replaceByAnElementAlreadyPresent() {
            assertSameOrder(keys(map).replace("a", "c"), sameKeys(map).replace("a", "c"), List.of("c", "b", "d"));
            assertSameOrder(keys(map).replace("c", "a"), sameKeys(map).replace("c", "a"), List.of("b", "a", "d"));
        }

        @Test
        public void replaceFirstAndLast() {
            assertSameOrder(keys(map).replace("a", "z"), sameKeys(map).replace("a", "z"), List.of("z", "b", "c", "d"));
            assertSameOrder(keys(map).replace("d", "z"), sameKeys(map).replace("d", "z"), List.of("a", "b", "c", "z"));
        }

        @Test
        public void replaceAfterRemovingTheHeadFromTheMap() {
            final LinkedHashMap<String, Integer> removed = map.remove("a");
            assertSameOrder(keys(removed).replace("b", "z"), sameKeys(removed).replace("b", "z"), List.of("z", "c", "d"));
            assertSameOrder(keys(removed).replace("d", "b"), sameKeys(removed).replace("d", "b"), List.of("c", "b"));
        }

        @Test
        public void replaceAfterRemovingAMiddleKeyFromTheMap() {
            final LinkedHashMap<String, Integer> removed = map.remove("b");
            assertSameOrder(keys(removed).replace("c", "z"), sameKeys(removed).replace("c", "z"), List.of("a", "z", "d"));
            assertSameOrder(keys(removed).replace("d", "a"), sameKeys(removed).replace("d", "a"), List.of("c", "a"));
        }

        @Test
        public void replaceAfterRemovingFromTheKeySet() {
            assertSameOrder(keys(map).remove("a").replace("b", "z"), sameKeys(map).remove("a").replace("b", "z"),
                    List.of("z", "c", "d"));
            assertSameOrder(keys(map).remove("b").replace("c", "z"), sameKeys(map).remove("b").replace("c", "z"),
                    List.of("a", "z", "d"));
            assertSameOrder(keys(map).remove("b").replace("d", "a"), sameKeys(map).remove("b").replace("d", "a"),
                    List.of("c", "a"));
        }

        @Test
        public void replaceAfterOperationsThatKeepTheMapValues() {
            assertSameOrder(keys(map).add("e").replace("b", "z"), sameKeys(map).add("e").replace("b", "z"),
                    List.of("a", "z", "c", "d", "e"));
            assertSameOrder(keys(map).filter(k -> true).replace("b", "z"), sameKeys(map).replace("b", "z"),
                    List.of("a", "z", "c", "d"));
            assertSameOrder(keys(map).tail().replace("b", "z"), sameKeys(map).tail().replace("b", "z"),
                    List.of("z", "c", "d"));
            assertSameOrder(keys(map).init().replace("b", "z"), sameKeys(map).init().replace("b", "z"),
                    List.of("a", "z", "c"));
            assertSameOrder(keys(map).drop(1).replace("d", "z"), sameKeys(map).drop(1).replace("d", "z"),
                    List.of("b", "c", "z"));
        }

        @Test
        public void replaceAllMatchesReplace() {
            assertSameOrder(keys(map).replaceAll("b", "z"), sameKeys(map).replaceAll("b", "z"), List.of("a", "z", "c", "d"));
        }

        @Test
        public void replaceByNullOnlyFailsWhenTheElementIsPresent() {
            final LinkedHashSet<String> keys = keys(map);
            Assertions.assertThat(keys.replace("x", null)).isSameAs(keys);
            assertThatNullPointerException().isThrownBy(() -> keys.replace("b", null));
            assertThatNullPointerException().isThrownBy(() -> sameKeys(map).replace("b", null));
        }

        @Test
        public void replaceLeavesTheMapUnchanged() {
            keys(map).replace("b", "z");
            Assertions.assertThat(map.toList()).isEqualTo(List.of(Tuple.of("a", 1), Tuple.of("b", 2), Tuple.of("c", 3), Tuple.of("d", 4)));
        }

        @Test
        public void everyOtherOperationMatchesALinkedHashSetOfTheSameKeys() {
            final LinkedHashSet<String> keys = keys(map);
            final LinkedHashSet<String> same = sameKeys(map);
            final Set<String> other = LinkedHashSet.of("c", "x", "a");
            assertSameOrder(keys.add("b"), same.add("b"), List.of("a", "b", "c", "d"));
            assertSameOrder(keys.add("e"), same.add("e"), List.of("a", "b", "c", "d", "e"));
            assertSameOrder(keys.addAll(List.of("e", "a", "f")), same.addAll(List.of("e", "a", "f")), List.of("a", "b", "c", "d", "e", "f"));
            assertSameOrder(keys.remove("c"), same.remove("c"), List.of("a", "b", "d"));
            assertSameOrder(keys.removeAll(List.of("a", "d")), same.removeAll(List.of("a", "d")), List.of("b", "c"));
            assertSameOrder(keys.retainAll(List.of("d", "b")), same.retainAll(List.of("d", "b")), List.of("b", "d"));
            assertSameOrder(keys.diff(other), same.diff(other), List.of("b", "d"));
            assertSameOrder(keys.intersect(other), same.intersect(other), List.of("a", "c"));
            assertSameOrder(keys.union(other), same.union(other), List.of("a", "b", "c", "d", "x"));
            assertSameOrder(keys.filter(k -> !k.equals("b")), same.filter(k -> !k.equals("b")), List.of("a", "c", "d"));
            assertSameOrder(keys.map(String::toUpperCase), same.map(String::toUpperCase), List.of("A", "B", "C", "D"));
            assertSameOrder(keys.tail(), same.tail(), List.of("b", "c", "d"));
            assertSameOrder(keys.init(), same.init(), List.of("a", "b", "c"));
            assertSameOrder(keys.take(2), same.take(2), List.of("a", "b"));
            assertSameOrder(keys.takeRight(2), same.takeRight(2), List.of("c", "d"));
            assertSameOrder(keys.drop(2), same.drop(2), List.of("c", "d"));
            assertSameOrder(keys.dropRight(2), same.dropRight(2), List.of("a", "b"));
            assertSameOrder(keys.takeWhile(k -> k.compareTo("c") < 0), same.takeWhile(k -> k.compareTo("c") < 0), List.of("a", "b"));
            assertSameOrder(keys.dropWhile(k -> k.compareTo("c") < 0), same.dropWhile(k -> k.compareTo("c") < 0), List.of("c", "d"));
            Assertions.assertThat(keys.contains("c")).isTrue();
            Assertions.assertThat(keys.contains("x")).isFalse();
            Assertions.assertThat(keys.head()).isEqualTo(same.head());
            Assertions.assertThat(keys.last()).isEqualTo(same.last());
            Assertions.assertThat(keys.size()).isEqualTo(same.size());
            Assertions.assertThat(keys.zipWithIndex().toList()).isEqualTo(same.zipWithIndex().toList());
            Assertions.assertThat(keys.sliding(2).map(Traversable::toList).toList())
                    .isEqualTo(same.sliding(2).map(Traversable::toList).toList());
            Assertions.assertThat(List.ofAll(keys::iterator)).isEqualTo(List.ofAll(same::iterator));
            Assertions.assertThat(new java.util.ArrayList<>(keys.asJava())).isEqualTo(new java.util.ArrayList<>(same.asJava()));
            Assertions.assertThat(keys).isEqualTo(same);
            Assertions.assertThat(keys.hashCode()).isEqualTo(same.hashCode());
            Assertions.assertThat(keys.toString()).isEqualTo(same.toString());
        }
    }

    @Nested
    class MapTests {
        @Test
        public void shouldReturnModifiedKeysMapWithNonUniqueMapperAndPredictableOrder() {
            final Map<Integer, String> actual = LinkedHashMap.of(3, "3").put(1, "1").put(2, "2")
                    .mapKeys(Integer::toHexString).mapKeys(String::length);
            final Map<Integer, String> expected = LinkedHashMap.of(1, "2");
            assertThat(actual).isEqualTo(expected);
        }
    }
    

    @Nested
    class LinkedHashMapPutTests {
        @Test
        public void shouldKeepOrderWhenPuttingAnExistingKeyAndNonExistingValue() {
            final Map<Integer, String> map = mapOf(1, "a", 2, "b", 3, "c");
            final Map<Integer, String> actual = map.put(1, "d");
            final Map<Integer, String> expected = mapOf(1, "d", 2, "b", 3, "c");
            assertThat(actual.toList()).isEqualTo(expected.toList());
        }

        @Test
        public void shouldKeepOrderWhenPuttingAnExistingKeyAndExistingValue() {
            final Map<Integer, String> map = mapOf(1, "a", 2, "b", 3, "c");
            final Map<Integer, String> actual = map.put(1, "a");
            final Map<Integer, String> expected = mapOf(1, "a", 2, "b", 3, "c");
            assertThat(actual.toList()).isEqualTo(expected.toList());
        }

        @Test
        public void shouldReuseOrderStructureWhenOverwritingAnExistingKey() throws Exception {
            final LinkedHashMap<Integer, String> map = LinkedHashMap.of(1, "a", 2, "b", 3, "c");
            final LinkedHashMap<Integer, String> actual = map.put(2, "B");
            assertThat(orderStructureOf(actual)).isSameAs(orderStructureOf(map));
        }

        @Test
        public void shouldSurfaceTheReplacedKeyInstanceAfterOverwrite() {
            final Map<IntMod2, String> map = LinkedHashMap.of(new IntMod2(1), "a").put(new IntMod2(3), "b");
            assertThat(map.toString()).isEqualTo("LinkedHashMap((3, b))");
            assertThat(map.keySet().iterator().next().toString()).isEqualTo("3");
        }

        private static Object orderStructureOf(LinkedHashMap<?, ?> map) throws Exception {
            var list = LinkedHashMap.class.getDeclaredField("list");
            list.setAccessible(true);
            return list.get(map);
        }
    }

    @Nested
    class ReplaceTests {
        @Test
        public void shouldReturnSameInstanceIfReplacingNonExistingPairUsingNonExistingKey() {
            final Map<Integer, String> map = LinkedHashMap.of(1, "a", 2, "b");
            final Map<Integer, String> actual = map.replace(Tuple.of(0, "?"), Tuple.of(0, "!"));
            assertThat(actual).isSameAs(map);
        }

        @Test
        public void shouldReturnSameInstanceIfReplacingNonExistingPairUsingExistingKey() {
            final Map<Integer, String> map = LinkedHashMap.of(1, "a", 2, "b");
            final Map<Integer, String> actual = map.replace(Tuple.of(2, "?"), Tuple.of(2, "!"));
            assertThat(actual).isSameAs(map);
        }

        @Test
        public void shouldPreserveOrderWhenReplacingExistingPairWithSameKeyAndDifferentValue() {
            final Map<Integer, String> map = LinkedHashMap.of(1, "a", 2, "b", 3, "c");
            final Map<Integer, String> actual = map.replace(Tuple.of(2, "b"), Tuple.of(2, "B"));
            final Map<Integer, String> expected = LinkedHashMap.of(1, "a", 2, "B", 3, "c");
            assertThat(actual).isEqualTo(expected);
            Assertions.assertThat(List.ofAll(actual)).isEqualTo(List.ofAll(expected));
        }

        @Test
        public void shouldPreserveOrderWhenReplacingExistingPairWithDifferentKeyValue() {
            final Map<Integer, String> map = LinkedHashMap.of(1, "a", 2, "b", 3, "c");
            final Map<Integer, String> actual = map.replace(Tuple.of(2, "b"), Tuple.of(4, "B"));
            final Map<Integer, String> expected = LinkedHashMap.of(1, "a", 4, "B", 3, "c");
            assertThat(actual).isEqualTo(expected);
            Assertions.assertThat(List.ofAll(actual)).isEqualTo(List.ofAll(expected));
        }

        @Test
        public void shouldPreserveOrderWhenReplacingExistingPairAndRemoveOtherIfKeyAlreadyExists() {
            final Map<Integer, String> map = LinkedHashMap.of(1, "a", 2, "b", 3, "c", 4, "d", 5, "e");
            final Map<Integer, String> actual = map.replace(Tuple.of(2, "b"), Tuple.of(4, "B"));
            final Map<Integer, String> expected = LinkedHashMap.of(1, "a", 4, "B", 3, "c", 5, "e");
            assertThat(actual).isEqualTo(expected);
            Assertions.assertThat(List.ofAll(actual)).isEqualTo(List.ofAll(expected));
        }

        @Test
        public void shouldReturnSameInstanceWhenReplacingExistingPairWithIdentity() {
            final Map<Integer, String> map = LinkedHashMap.of(1, "a", 2, "b", 3, "c");
            final Map<Integer, String> actual = map.replace(Tuple.of(2, "b"), Tuple.of(2, "b"));
            assertThat(actual).isSameAs(map);
        }
    }

    @Nested
    class SpliteratorTests {
        @Test
        public void shouldNotHaveSortedSpliterator() {
            assertThat(of(1, 2, 3).spliterator().hasCharacteristics(Spliterator.SORTED)).isFalse();
        }

        @Test
        public void shouldHaveOrderedSpliterator() {
            assertThat(of(1, 2, 3).spliterator().hasCharacteristics(Spliterator.ORDERED)).isTrue();
        }
    }

    @Nested
    class RetainAllTests {
        @Test
        public void shouldPreserveInsertionOrderWhenRetainingAll() {
            final LinkedHashMap<Integer, String> map = LinkedHashMap.of(1, "a", 2, "b", 3, "c", 4, "d");
            final LinkedHashMap<Integer, String> actual = map.retainAll(List.of(Tuple.of(4, "d"), Tuple.of(2, "b"), Tuple.of(9, "z")));
            assertThat(actual.toList()).isEqualTo(List.of(Tuple.of(2, "b"), Tuple.of(4, "d")));
        }
    }

    // -- positional operations, in insertion order

    @Nested
    class PositionalTests {

        private static final int[] WINDOW_SIZES = { 1, 2, 3, 5, 69, 70, 71, Integer.MAX_VALUE };
        private static final int[] WINDOW_STEPS = { 1, 2, 3, 70, 71, Integer.MAX_VALUE };

        private java.util.List<LinkedHashMap<Integer, String>> receivers() {
            final LinkedHashMap<Integer, String> five = mk(5, 3, 9, 1, 7);
            LinkedHashMap<Integer, String> everyThirdRemoved = mk(Vector.range(0, 70));
            for (int i = 0; i < 70; i += 3) {
                everyThirdRemoved = everyThirdRemoved.remove(i);
            }
            // 35 markers for 35 entries: the most the insertion order keeps before it is rebuilt
            LinkedHashMap<Integer, String> atThreshold = mk(Vector.range(0, 70));
            for (int i = 10; i < 45; i++) {
                atThreshold = atThreshold.remove(i);
            }
            final java.util.List<Integer> shuffled = new java.util.ArrayList<>(Vector.range(0, 70).asJava());
            java.util.Collections.shuffle(shuffled, new java.util.Random(72));
            return java.util.List.of(
                    LinkedHashMap.<Integer, String> empty(),
                    mk(7),
                    five,
                    five.put(3, "again"),
                    five.remove(9),
                    five.remove(5),
                    five.remove(5).remove(9),
                    five.remove(7),
                    five.remove(3).put(3, "back"),
                    mk(Vector.range(0, 70)),
                    mk(shuffled),
                    everyThirdRemoved,
                    atThreshold);
        }

        private int[] counts(int size) {
            return new int[] { Integer.MIN_VALUE, -1, 0, 1, 2, size / 2, size - 1, size, size + 1, Integer.MAX_VALUE };
        }

        // Vector's own takeRight/dropRight compute length - n, so the reference is given an n that cannot overflow
        private int clamp(int n, int size) {
            return Math.max(-1, Math.min(n, size + 1));
        }

        // `actual` holds exactly `expected`, in order, and behaves as a LinkedHashMap built from it
        private void assertValid(LinkedHashMap<Integer, String> receiver, LinkedHashMap<Integer, String> actual, Vector<Tuple2<Integer, String>> expected) {
            assertEquals(expected, actual.toVector());
            assertEquals(expected.size(), actual.size());
            if (expected.isEmpty()) {
                assertSame(LinkedHashMap.empty(), actual);
            }
            for (Tuple2<Integer, String> entry : expected) {
                assertEquals(Option.some(entry._2()), actual.get(entry._1()));
            }
            for (Tuple2<Integer, String> entry : receiver) {
                assertEquals(expected.contains(entry), actual.containsKey(entry._1()));
            }
            // the result's insertion order is consistent with its hash map: a new key goes last, an existing key keeps
            // its position, and a removal takes out exactly that entry
            assertEquals(expected.append(Tuple.of(1000, "new")), actual.put(1000, "new").toVector());
            for (int i = 0; i < expected.size(); i++) {
                final Tuple2<Integer, String> entry = expected.get(i);
                assertEquals(expected.update(i, Tuple.of(entry._1(), "changed")), actual.put(entry._1(), "changed").toVector());
                assertEquals(expected.removeAt(i), actual.remove(entry._1()).toVector());
            }
        }

        @Test
        public void shouldTakeAndDropLikeTheSequenceOfTheElements() {
            for (LinkedHashMap<Integer, String> receiver : receivers()) {
                final Vector<Tuple2<Integer, String>> elements = receiver.toVector();
                final int size = receiver.size();
                for (int n : counts(size)) {
                    final int m = clamp(n, size);
                    final LinkedHashMap<Integer, String> take = receiver.take(n);
                    final LinkedHashMap<Integer, String> takeRight = receiver.takeRight(n);
                    final LinkedHashMap<Integer, String> drop = receiver.drop(n);
                    final LinkedHashMap<Integer, String> dropRight = receiver.dropRight(n);
                    assertValid(receiver, take, elements.take(m));
                    assertValid(receiver, takeRight, elements.takeRight(m));
                    assertValid(receiver, drop, elements.drop(m));
                    assertValid(receiver, dropRight, elements.dropRight(m));
                    if (n >= size) {
                        assertSame(receiver, take);
                        assertSame(receiver, takeRight);
                    }
                    if (n <= 0) {
                        assertSame(receiver, drop);
                        assertSame(receiver, dropRight);
                    }
                }
                assertEquals(elements, receiver.toVector());
            }
        }

        @Test
        public void shouldReturnTheFirstAndTheLastElement() {
            for (LinkedHashMap<Integer, String> receiver : receivers()) {
                final Vector<Tuple2<Integer, String>> elements = receiver.toVector();
                if (elements.isEmpty()) {
                    assertEquals("head of empty LinkedHashMap", assertThrows(NoSuchElementException.class, receiver::head).getMessage());
                    assertEquals("last of empty LinkedHashMap", assertThrows(NoSuchElementException.class, receiver::last).getMessage());
                    assertEquals(Option.none(), receiver.headOption());
                    assertEquals(Option.none(), receiver.lastOption());
                } else {
                    assertEquals(elements.head(), receiver.head());
                    assertEquals(elements.last(), receiver.last());
                    assertEquals(Option.some(elements.head()), receiver.headOption());
                    assertEquals(Option.some(elements.last()), receiver.lastOption());
                }
            }
            assertEquals(Tuple.of(5, "v5"), mk(5, 3, 9, 1, 7).head());
            assertEquals(Tuple.of(7, "v7"), mk(5, 3, 9, 1, 7).last());
            assertEquals(Tuple.of(3, "v3"), mk(5, 3, 9, 1, 7).remove(5).head());
            assertEquals(Tuple.of(1, "v1"), mk(5, 3, 9, 1, 7).remove(7).last());
        }

        @Test
        public void shouldDropTheFirstOrTheLastElementWithTailAndInit() {
            for (LinkedHashMap<Integer, String> receiver : receivers()) {
                final Vector<Tuple2<Integer, String>> elements = receiver.toVector();
                if (elements.isEmpty()) {
                    assertEquals("tail of empty LinkedHashMap", assertThrows(UnsupportedOperationException.class, receiver::tail).getMessage());
                    assertEquals("init of empty LinkedHashMap", assertThrows(UnsupportedOperationException.class, receiver::init).getMessage());
                    assertEquals(Option.none(), receiver.tailOption());
                    assertEquals(Option.none(), receiver.initOption());
                } else {
                    final LinkedHashMap<Integer, String> tail = receiver.tail();
                    final LinkedHashMap<Integer, String> init = receiver.init();
                    assertValid(receiver, tail, elements.tail());
                    assertValid(receiver, init, elements.init());
                    final Option<LinkedHashMap<Integer, String>> tailOption = receiver.tailOption();
                    final Option<LinkedHashMap<Integer, String>> initOption = receiver.initOption();
                    assertValid(receiver, tailOption.get(), elements.tail());
                    assertValid(receiver, initOption.get(), elements.init());
                }
            }
        }

        @Test
        public void shouldTakeAndDropWhileOrUntilAPredicateHolds() {
            final java.util.List<java.util.function.Predicate<Tuple2<Integer, String>>> predicates = java.util.List.of(
                    e -> true, e -> false, e -> e._1() < 5, e -> e._1() >= 5, e -> e._1() % 2 == 1, e -> e._1() != 40);
            for (LinkedHashMap<Integer, String> receiver : receivers()) {
                final Vector<Tuple2<Integer, String>> elements = receiver.toVector();
                for (java.util.function.Predicate<Tuple2<Integer, String>> predicate : predicates) {
                    final LinkedHashMap<Integer, String> takeWhile = receiver.takeWhile(predicate);
                    final LinkedHashMap<Integer, String> takeUntil = receiver.takeUntil(predicate);
                    final LinkedHashMap<Integer, String> dropWhile = receiver.dropWhile(predicate);
                    final LinkedHashMap<Integer, String> dropUntil = receiver.dropUntil(predicate);
                    assertValid(receiver, takeWhile, elements.takeWhile(predicate));
                    assertValid(receiver, takeUntil, elements.takeUntil(predicate));
                    assertValid(receiver, dropWhile, elements.dropWhile(predicate));
                    assertValid(receiver, dropUntil, elements.dropUntil(predicate));
                }
                // the walk stops at the first element that ends the prefix
                final int[] calls = { 0 };
                receiver.takeWhile(e -> {
                    calls[0]++;
                    return false;
                });
                assertEquals(receiver.isEmpty() ? 0 : 1, calls[0]);
                assertThrows(NullPointerException.class, () -> receiver.takeWhile(null));
                assertThrows(NullPointerException.class, () -> receiver.takeUntil(null));
                assertThrows(NullPointerException.class, () -> receiver.dropWhile(null));
                assertThrows(NullPointerException.class, () -> receiver.dropUntil(null));
            }
        }

        @Test
        public void shouldZipWithThePosition() {
            for (LinkedHashMap<Integer, String> receiver : receivers()) {
                final Vector<Tuple2<Tuple2<Integer, String>, Integer>> zipped = receiver.zipWithIndex();
                assertEquals(receiver.toVector().zipWithIndex(), zipped);
                for (int i = 0; i < zipped.size(); i++) {
                    assertEquals(i, zipped.get(i)._2());
                }
            }
        }

        @Test
        public void shouldGroupAndSlideLikeTheSequenceOfTheElements() {
            for (LinkedHashMap<Integer, String> receiver : receivers()) {
                final Vector<Tuple2<Integer, String>> elements = receiver.toVector();
                for (int size : WINDOW_SIZES) {
                    for (int step : WINDOW_STEPS) {
                        final Vector<LinkedHashMap<Integer, String>> windows = receiver.sliding(size, step);
                        final Vector<Vector<Tuple2<Integer, String>>> expected = elements.sliding(size, step);
                        assertEquals(expected.size(), windows.size());
                        for (int i = 0; i < windows.size(); i++) {
                            assertValid(receiver, windows.get(i), expected.get(i));
                        }
                    }
                    final Vector<LinkedHashMap<Integer, String>> groups = receiver.grouped(size);
                    assertEquals(elements.grouped(size), groups.map(LinkedHashMap::toVector));
                    groups.forEach(group -> assertValid(receiver, group, group.toVector()));
                    final Vector<LinkedHashMap<Integer, String>> windows = receiver.sliding(size);
                    assertEquals(elements.sliding(size), windows.map(LinkedHashMap::toVector));
                    windows.forEach(window -> assertValid(receiver, window, window.toVector()));
                }
            }
        }

        @Test
        public void shouldSlideFollowingTheWindowRules() {
            assertEquals(Vector.of(Vector.of(1, 2, 3), Vector.of(2, 3, 4)), mk(1, 2, 3, 4).sliding(3).map(this::keys));
            assertEquals(Vector.of(Vector.of(1, 2), Vector.of(4, 5)), mk(1, 2, 3, 4, 5).sliding(2, 3).map(this::keys));
            assertEquals(Vector.of(Vector.of(1, 2), Vector.of(5)), mk(1, 2, 3, 4, 5).sliding(2, 4).map(this::keys));
            assertEquals(Vector.of(Vector.of(1, 2, 3), Vector.of(3, 4, 5)), mk(1, 2, 3, 4, 5).sliding(3, 2).map(this::keys));
            assertEquals(Vector.of(Vector.of(1, 2, 3), Vector.of(3, 4, 5), Vector.of(5, 6)),
                    mk(1, 2, 3, 4, 5, 6).sliding(3, 2).map(this::keys));
            assertEquals(Vector.of(Vector.of(1)), mk(1, 2, 3).sliding(1, 3).map(this::keys));
            assertEquals(Vector.of(Vector.of(1, 2)), mk(1, 2).sliding(5).map(this::keys));
            assertEquals(Vector.of(Vector.of(1)), mk(1).sliding(1).map(this::keys));
            assertEquals(Vector.of(Vector.of(1, 2), Vector.of(3, 4), Vector.of(5)), mk(1, 2, 3, 4, 5).grouped(2).map(this::keys));
            assertEquals(Vector.of(Vector.of(1, 2, 3), Vector.of(10, 12), Vector.of(20, 29)),
                    mk(1, 2, 3, 10, 12, 20, 29).slideBy(e -> e._1() / 10).map(this::keys));
            // a huge step or size does not overflow the window start
            assertEquals(Vector.of(Vector.range(0, 3)), mk(Vector.range(0, 40)).sliding(3, Integer.MAX_VALUE).map(this::keys));
            assertEquals(Vector.of(Vector.range(0, 40)),
                    mk(Vector.range(0, 40)).sliding(Integer.MAX_VALUE, Integer.MAX_VALUE).map(this::keys));
            assertTrue(LinkedHashMap.<Integer, String> empty().sliding(1).isEmpty());
            assertTrue(LinkedHashMap.<Integer, String> empty().sliding(2, 3).isEmpty());
            assertTrue(LinkedHashMap.<Integer, String> empty().grouped(2).isEmpty());
        }

        @Test
        public void shouldRejectANonPositiveWindowSizeOrStep() {
            for (LinkedHashMap<Integer, String> receiver : receivers()) {
                assertThrows(IllegalArgumentException.class, () -> receiver.grouped(0));
                assertThrows(IllegalArgumentException.class, () -> receiver.grouped(-1));
                assertThrows(IllegalArgumentException.class, () -> receiver.sliding(0));
                assertThrows(IllegalArgumentException.class, () -> receiver.sliding(2, 0));
                assertThrows(IllegalArgumentException.class, () -> receiver.sliding(0, 2));
                assertThrows(IllegalArgumentException.class, () -> receiver.sliding(-1, -1));
            }
        }

        @Test
        public void shouldSlideByCallingTheClassifierOncePerElement() {
            for (LinkedHashMap<Integer, String> receiver : receivers()) {
                final Vector<Tuple2<Integer, String>> elements = receiver.toVector();
                final java.util.List<Tuple2<Integer, String>> seen = new java.util.ArrayList<>();
                final Vector<LinkedHashMap<Integer, String>> runs = receiver.slideBy(e -> {
                    seen.add(e);
                    return e._1() / 3;
                });
                assertEquals(new java.util.ArrayList<>(elements.asJava()), seen);
                final Vector<Vector<Tuple2<Integer, String>>> expected = elements.slideBy(e -> e._1() / 3);
                assertEquals(expected.size(), runs.size());
                for (int i = 0; i < runs.size(); i++) {
                    assertValid(receiver, runs.get(i), expected.get(i));
                }
                assertEquals(receiver.isEmpty() ? 0 : 1, receiver.slideBy(e -> "same").size());
                assertEquals(receiver.size(), receiver.slideBy(e -> e).size());
                assertThrows(NullPointerException.class, () -> receiver.slideBy(null));
            }
        }

        @Test
        public void shouldDeclareThePositionalMembersWithTheOwnType() throws Exception {
            for (String name : new String[] { "init", "tail" }) {
                assertEquals(LinkedHashMap.class, LinkedHashMap.class.getDeclaredMethod(name).getReturnType());
            }
            for (String name : new String[] { "take", "takeRight", "drop", "dropRight" }) {
                assertEquals(LinkedHashMap.class, LinkedHashMap.class.getDeclaredMethod(name, int.class).getReturnType());
            }
            for (String name : new String[] { "takeWhile", "takeUntil", "dropWhile", "dropUntil" }) {
                assertEquals(LinkedHashMap.class, LinkedHashMap.class.getDeclaredMethod(name, java.util.function.Predicate.class).getReturnType());
            }
            assertEquals("com.guizmaii.zazr.control.Option<com.guizmaii.zazr.collection.LinkedHashMap<K, V>>",
                    LinkedHashMap.class.getDeclaredMethod("tailOption").getGenericReturnType().getTypeName());
            assertEquals("com.guizmaii.zazr.control.Option<com.guizmaii.zazr.collection.LinkedHashMap<K, V>>",
                    LinkedHashMap.class.getDeclaredMethod("initOption").getGenericReturnType().getTypeName());
            assertEquals("com.guizmaii.zazr.collection.Vector<com.guizmaii.zazr.collection.LinkedHashMap<K, V>>",
                    LinkedHashMap.class.getDeclaredMethod("grouped", int.class).getGenericReturnType().getTypeName());
            assertEquals("com.guizmaii.zazr.collection.Vector<com.guizmaii.zazr.collection.LinkedHashMap<K, V>>",
                    LinkedHashMap.class.getDeclaredMethod("sliding", int.class).getGenericReturnType().getTypeName());
            assertEquals("com.guizmaii.zazr.collection.Vector<com.guizmaii.zazr.collection.LinkedHashMap<K, V>>",
                    LinkedHashMap.class.getDeclaredMethod("sliding", int.class, int.class).getGenericReturnType().getTypeName());
            assertEquals("com.guizmaii.zazr.collection.Vector<com.guizmaii.zazr.collection.LinkedHashMap<K, V>>",
                    LinkedHashMap.class.getDeclaredMethod("slideBy", Function.class).getGenericReturnType().getTypeName());
            assertEquals("com.guizmaii.zazr.collection.Vector<com.guizmaii.zazr.Tuple2<com.guizmaii.zazr.Tuple2<K, V>, java.lang.Integer>>",
                    LinkedHashMap.class.getDeclaredMethod("zipWithIndex").getGenericReturnType().getTypeName());
            final java.util.Set<String> declared = new java.util.HashSet<>();
            for (java.lang.reflect.Method method : LinkedHashMap.class.getDeclaredMethods()) {
                declared.add(method.getName());
            }
            assertTrue(declared.containsAll(ORDERED_POSITIONAL_MEMBERS));
        }

        @Test
        public void shouldKeepTheInsertionOrderAfterAPutOfAnExistingKey() {
            final LinkedHashMap<Integer, String> map = mk(3, 1, 2).put(3, "three").put(1, "one");
            assertEquals(Tuple.of(3, "three"), map.head());
            assertEquals(Tuple.of(2, "v2"), map.last());
            assertEquals(Vector.of(3, 1), keys(map.take(2)));
            assertEquals(Vector.of(1, 2), keys(map.tail()));
            assertEquals(Vector.of(Tuple.of(Tuple.of(3, "three"), 0), Tuple.of(Tuple.of(1, "one"), 1), Tuple.of(Tuple.of(2, "v2"), 2)),
                    map.zipWithIndex());
            // a removed key put again goes last
            assertEquals(Vector.of(1, 2, 3), keys(map.remove(3).put(3, "back")));
            assertEquals(Tuple.of(3, "back"), map.remove(3).put(3, "back").last());
        }

        @Test
        public void shouldCoverEveryRepresentationOfTheInsertionOrder() throws Exception {
            // offset > 0, markers of removed keys in the middle, and as many markers as keys (one more rebuilds)
            final java.util.List<LinkedHashMap<Integer, String>> receivers = receivers();
            final java.util.Set<String> shapes = new java.util.HashSet<>();
            for (LinkedHashMap<Integer, String> receiver : receivers) {
                final int offset = representation(receiver, "offset");
                final int tombstones = representation(receiver, "tombstones");
                if (offset > 0) {
                    shapes.add(tombstones > 0 ? "offset and markers" : "offset");
                }
                if (tombstones > 0 && tombstones == receiver.size()) {
                    shapes.add("markers at the rebuild threshold");
                } else if (tombstones > 0) {
                    shapes.add("markers");
                }
            }
            assertEquals(java.util.Set.of("offset", "offset and markers", "markers", "markers at the rebuild threshold"), shapes);
        }

        private int representation(LinkedHashMap<Integer, String> receiver, String field) throws Exception {
            final java.lang.reflect.Field declared = LinkedHashMap.class.getDeclaredField(field);
            declared.setAccessible(true);
            return (int) declared.get(receiver);
        }

        private Vector<Integer> keys(LinkedHashMap<Integer, String> map) {
            return map.toVector().map(Tuple2::_1);
        }

        private LinkedHashMap<Integer, String> mk(Integer... keys) {
            return mk(Vector.of(keys));
        }

        private LinkedHashMap<Integer, String> mk(Iterable<Integer> keys) {
            LinkedHashMap<Integer, String> map = LinkedHashMap.empty();
            for (Integer key : keys) {
                map = map.put(key, "v" + key);
            }
            return map;
        }
    }
}
