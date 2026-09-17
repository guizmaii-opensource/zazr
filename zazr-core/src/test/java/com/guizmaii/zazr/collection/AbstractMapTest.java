package com.guizmaii.zazr.collection;

import com.guizmaii.zazr.Tuple;
import com.guizmaii.zazr.Tuple2;
import com.guizmaii.zazr.control.Option;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Collector;
import org.assertj.core.api.IterableAssert;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;

public abstract class AbstractMapTest extends AbstractTraversableTest {

    @Override
    protected <T> IterableAssert<T> assertThat(Iterable<T> actual) {
        return new IterableAssert<T>(actual) {
            @Override
            public IterableAssert<T> isEqualTo(Object obj) {
                @SuppressWarnings("unchecked")
                final Iterable<T> expected = (Iterable<T>) obj;
                final java.util.Map<T, Integer> actualMap = countMap(actual);
                final java.util.Map<T, Integer> expectedMap = countMap(expected);
                AbstractMapTest.super.assertThat(actualMap.size()).isEqualTo(expectedMap.size());
                actualMap.forEach((k, v) -> AbstractMapTest.super.assertThat(v).isEqualTo(expectedMap.get(k)));
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
                return AbstractMapTest.this::ofAll;
            }

            @Override
            public Set<Characteristics> characteristics() {
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

    protected abstract String className();

    abstract <T1, T2> java.util.Map<T1, T2> javaEmptyMap();

    protected abstract <T1 extends Comparable<? super T1>, T2> Map<T1, T2> emptyMap();

    protected abstract <K extends Comparable<? super K>, V, T extends V> Collector<T, ArrayList<T>, ? extends Map<K, V>> collectorWithMapper(
            Function<? super T,? extends K> keyMapper);

    protected abstract <K extends Comparable<? super K>, V, T> Collector<T, ArrayList<T>, ? extends Map<K, V>> collectorWithMappers(
            Function<? super T,? extends K> keyMapper, Function<? super T, ? extends V> valueMapper);

    protected boolean emptyMapShouldBeSingleton() {
        return true;
    }

    protected abstract <T> Collector<Tuple2<Integer, T>, ArrayList<Tuple2<Integer, T>>, ? extends Map<Integer, T>> mapCollector();

    @SuppressWarnings("unchecked")
    protected abstract <K extends Comparable<? super K>, V> Map<K, V> mapOfTuples(Tuple2<? extends K, ? extends V>... entries);

    protected abstract <K extends Comparable<? super K>, V> Map<K, V> mapOfTuples(Iterable<? extends Tuple2<? extends K, ? extends V>> entries);

    @SuppressWarnings("unchecked")
    protected abstract <K extends Comparable<? super K>, V> Map<K, V> mapOfEntries(java.util.Map.Entry<? extends K, ? extends V>... entries);

    protected abstract <K extends Comparable<? super K>, V> Map<K, V> mapOf(K k1, V v1);

    protected abstract <K extends Comparable<? super K>, V> Map<K, V> mapOf(K k1, V v1, K k2, V v2);

    protected abstract <K extends Comparable<? super K>, V> Map<K, V> mapOf(K k1, V v1, K k2, V v2, K k3, V v3);

    protected abstract <T, K extends Comparable<? super K>, V> Map<K, V> mapOf(java.util.stream.Stream<? extends T> stream,
            Function<? super T, ? extends K> keyMapper,
            Function<? super T, ? extends V> valueMapper);

    protected abstract <T, K extends Comparable<? super K>, V> Map<K, V> mapOf(java.util.stream.Stream<? extends T> stream,
            Function<? super T, Tuple2<? extends K, ? extends V>> f);

    protected abstract <K extends Comparable<? super K>, V> Map<K, V> mapTabulate(int n, Function<? super Integer, ? extends Tuple2<? extends K, ? extends V>> f);

    protected abstract <K extends Comparable<? super K>, V> Map<K, V> mapFill(int n, Supplier<? extends Tuple2<? extends K, ? extends V>> s);

    @Override
    protected boolean useIsEqualToInsteadOfIsSameAs() {
        return true;
    }

    @Override
    protected int getPeekNonNilPerformingAnAction() {
        return 1;
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
        return ofAll(com.guizmaii.zazr.collection.Iterator.ofAll(javaStream.iterator()));
    }

    @Override
    protected IntMap<Boolean> ofAll(boolean... elements) {
        return ofAll(com.guizmaii.zazr.collection.Iterator.ofAll(elements));
    }

    @Override
    protected IntMap<Byte> ofAll(byte... elements) {
        return ofAll(com.guizmaii.zazr.collection.Iterator.ofAll(elements));
    }

    @Override
    protected IntMap<Character> ofAll(char... elements) {
        return ofAll(com.guizmaii.zazr.collection.Iterator.ofAll(elements));
    }

    @Override
    protected IntMap<Double> ofAll(double... elements) {
        return ofAll(com.guizmaii.zazr.collection.Iterator.ofAll(elements));
    }

    @Override
    protected IntMap<Float> ofAll(float... elements) {
        return ofAll(com.guizmaii.zazr.collection.Iterator.ofAll(elements));
    }

    @Override
    protected IntMap<Integer> ofAll(int... elements) {
        return ofAll(com.guizmaii.zazr.collection.Iterator.ofAll(elements));
    }

    @Override
    protected IntMap<Long> ofAll(long... elements) {
        return ofAll(com.guizmaii.zazr.collection.Iterator.ofAll(elements));
    }

    @Override
    protected IntMap<Short> ofAll(short... elements) {
        return ofAll(com.guizmaii.zazr.collection.Iterator.ofAll(elements));
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
        assertThat(mapOf(Stream.range(0, 4).toJavaStream()
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
        assertThat(mapOf(Stream.range(0, 4).toJavaStream(), i ->
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
    class HeadTests {
        @Test
        public void shouldThrowWhenHeadEmpty() {
            assertThrows(NoSuchElementException.class, () -> emptyMap().head());
        }
    }

    @Nested
    class InitTests {
        @Test
        public void shouldThrowWhenInitEmpty() {
            assertThrows(UnsupportedOperationException.class, () -> emptyMap().init());
        }
    }

    @Nested
    class TailTests {
        @Test
        public void shouldThrowWhenTailEmpty() {
            assertThrows(UnsupportedOperationException.class, () -> emptyMap().tail());
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
        @Test
        public void shouldConvertToJavaMap() {
            final Map<Integer, String> actual = mapOf(1, "1", 2, "2", 3, "3");
            final java.util.Map<Integer, String> expected = asJavaMap(asJavaEntry(1, "1"), asJavaEntry(2, "2"), asJavaEntry(3, "3"));
            assertThat(actual.toJavaMap()).isEqualTo(expected);
        }
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

        @Test
        @SuppressWarnings("unchecked")
        public void shouldReturnKeysIterator() {
            final Iterator<Integer> actual = mapOfTuples(Tuple.of(1, 11), Tuple.of(2, 22), Tuple.of(3, 33)).keysIterator();
            assertThat(actual).isEqualTo(com.guizmaii.zazr.collection.Iterator.of(1, 2, 3));
        }
    }

    @Nested
    class ValuesTests {
        @Test
        @SuppressWarnings("unchecked")
        public void shouldReturnValuesSeq() {
            final Seq<Integer> actual = mapOfTuples(Tuple.of(1, 11), Tuple.of(2, 22), Tuple.of(3, 33)).values();
            assertThat(actual).isEqualTo(com.guizmaii.zazr.collection.Iterator.of(11, 22, 33));
        }

        @Test
        @SuppressWarnings("unchecked")
        public void shouldReturnValuesIterator() {
            final Iterator<Integer> actual = mapOfTuples(Tuple.of(1, 11), Tuple.of(2, 22), Tuple.of(3, 33)).valuesIterator();
            assertThat(actual).isEqualTo(com.guizmaii.zazr.collection.Iterator.of(11, 22, 33));
        }
    }

    // -- biMap

    @Test
    public void shouldBiMapEmpty() {
        assertThat(emptyInt().bimap(i -> i + 1, o -> o)).isEqualTo(com.guizmaii.zazr.collection.Vector.empty());
    }

    @Test
    public void shouldBiMapNonEmpty() {
        final Seq<Tuple2<Integer, String>> expected = Stream.of(Tuple.of(2, "1!"), Tuple.of(3, "2!"));
        final Seq<Tuple2<Integer, String>> actual = emptyInt().put(1, "1").put(2, "2").bimap(i -> i + 1, s -> s + "!").toStream();
        assertThat(actual).isEqualTo(expected);
    }

    // -- orElse

    // DEV-Note: IntMap converts `other` to map

    @Override
    @Test
    public void shouldCaclEmptyOrElseSameOther() {
        Iterable<Integer> other = of(42);
        assertThat(empty().orElse(other)).isEqualTo(other);
    }

    @Test
    public void shouldCaclEmptyOrElseSameSupplier() {
        Iterable<Integer> other = of(42);
        Supplier<Iterable<Integer>> supplier = () -> other;
        assertThat(empty().orElse(supplier)).isEqualTo(other);
    }

    // -- map

    @Test
    public void shouldMapEmpty() {
        assertThat(emptyInt().map(Tuple2::_1)).isEqualTo(com.guizmaii.zazr.collection.Vector.empty());
    }

    @Test
    public void shouldMapNonEmpty() {
        final Seq<Integer> expected = com.guizmaii.zazr.collection.Vector.of(1, 2);
        final Seq<Integer> actual = emptyInt().put(1, "1").put(2, "2").map(Tuple2::_1);
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
                .mapKeys(k -> k * 118).mapKeys(Integer::toHexString).mapKeys(AbstractMapTest::md5).mapKeys(String::length);
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
                .mapKeys(k -> k * 118).mapKeys(Integer::toHexString).mapKeys(AbstractMapTest::md5)//Unique key mappers
                .mapKeys(String::length, (v1, v2) -> com.guizmaii.zazr.collection.List.of(v1.split("#")).append(v2).sorted().mkString("#"));
        final Map<Integer, String> expected = emptyIntString().put(32, "1#2#3");
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void shouldReturnModifiedValuesMap() {
        assertThat(emptyIntString().put(1, "1").put(2, "2").mapValues(Integer::parseInt)).isEqualTo(emptyInt().put(1, 1).put(2, 2));
    }

    @Test
    public void shouldReturnListWithMappedValues() {
        assertThat(emptyIntInt().put(1, 1).put(2, 2).iterator((a, b) -> a + b).toList()).isEqualTo(com.guizmaii.zazr.collection.List.of(2, 4));
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
        if (map.isOrdered()) {
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
        if (map.isOrdered()) {
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
            final String expected = map.stringPrefix() + "((3, b))";

            assertThat(actual).isEqualTo(expected);
        }

        @Test
        public void shouldPutExistingKeyAndEqualValue() {
            final Map<IntMod2, String> map = mapOf(new IntMod2(1), "a");

            // we need to compare Strings because equals (intentionally) does not work for IntMod2
            final String actual = map.put(new IntMod2(3), "a").toString();
            final String expected = map.stringPrefix() + "((3, a))";

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
    class TransformTests {
        @Test
        public void shouldTransform() {
            final Map<?, ?> actual = emptyIntInt().put(1, 11).transform(map -> map.put(2, 22));
            assertThat(actual).isEqualTo(emptyIntInt().put(1, 11).put(2, 22));
        }
    }

    // -- unzip

    @Test
    public void shouldUnzipIdentityNil() {
        assertThat(emptyMap().unzip()).isEqualTo(Tuple.of(Stream.empty(), Stream.empty()));
    }

    @Test
    public void shouldUnzipIdentityNonNil() {
        final Map<Integer, Integer> map = emptyIntInt().put(0, 10).put(1, 11).put(2, 12);
        final Tuple actual = map.unzip();
        final Tuple expected = Tuple.of(Stream.of(0, 1, 2), Stream.of(10, 11, 12));
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void shouldUnzipNil() {
        assertThat(emptyMap().unzip(x -> Tuple.of(x, x))).isEqualTo(Tuple.of(Stream.empty(), Stream.empty()));
        assertThat(emptyMap().unzip((k, v) -> Tuple.of(Tuple.of(k, v), Tuple.of(k, v))))
                .isEqualTo(Tuple.of(Stream.empty(), Stream.empty()));
    }

    @Test
    public void shouldUnzipNonNil() {
        final Map<Integer, Integer> map = emptyIntInt().put(0, 0).put(1, 1);
        final Tuple actual = map.unzip(entry -> Tuple.of(entry._1(), entry._2() + 1));
        final Tuple expected = Tuple.of(Stream.of(0, 1), Stream.of(1, 2));
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void shouldUnzip3Nil() {
        assertThat(emptyMap().unzip3(x -> Tuple.of(x, x, x))).isEqualTo(Tuple.of(Stream.empty(), Stream.empty(), Stream.empty()));
        assertThat(emptyMap().unzip3((k, v) -> Tuple.of(Tuple.of(k, v), Tuple.of(k, v), Tuple.of(k, v))))
                .isEqualTo(Tuple.of(Stream.empty(), Stream.empty(), Stream.empty()));
    }

    @Test
    public void shouldUnzip3NonNil() {
        final Map<Integer, Integer> map = emptyIntInt().put(0, 0).put(1, 1);
        final Tuple actual = map.unzip3(entry -> Tuple.of(entry._1(), entry._2() + 1, entry._2() + 5));
        final Tuple expected = Tuple.of(Stream.of(0, 1), Stream.of(1, 2), Stream.of(5, 6));
        assertThat(actual).isEqualTo(expected);
    }

    // -- zip

    @Test
    public void shouldZipNils() {
        final Seq<Tuple2<Tuple2<Integer, Object>, Object>> actual = emptyInt().zip(com.guizmaii.zazr.collection.List.empty());
        assertThat(actual).isEqualTo(Stream.empty());
    }

    @Test
    public void shouldZipEmptyAndNonNil() {
        final Seq<Tuple2<Tuple2<Integer, Object>, Integer>> actual = emptyInt().zip(com.guizmaii.zazr.collection.List.of(1));
        assertThat(actual).isEqualTo(Stream.empty());
    }

    @Test
    public void shouldZipNonEmptyAndNil() {
        final Seq<Tuple2<Tuple2<Integer, Integer>, Object>> actual = emptyIntInt().put(0, 1).zip(com.guizmaii.zazr.collection.List.empty());
        assertThat(actual).isEqualTo(Stream.empty());
    }

    @Test
    public void shouldZipNonNilsIfThisIsSmaller() {
        final Seq<Tuple2<Tuple2<Integer, Integer>, Integer>> actual = emptyIntInt()
                .put(0, 0)
                .put(1, 1)
                .zip(com.guizmaii.zazr.collection.List.of(5, 6, 7));
        assertThat(actual).isEqualTo(Stream.of(Tuple.of(Tuple.of(0, 0), 5), Tuple.of(Tuple.of(1, 1), 6)));
    }

    @Test
    public void shouldZipNonNilsIfThatIsSmaller() {
        final Seq<Tuple2<Tuple2<Integer, Integer>, Integer>> actual = emptyIntInt()
                .put(0, 0)
                .put(1, 1)
                .put(2, 2)
                .zip(com.guizmaii.zazr.collection.List.of(5, 6));
        assertThat(actual).isEqualTo(Stream.of(Tuple.of(Tuple.of(0, 0), 5), Tuple.of(Tuple.of(1, 1), 6)));
    }

    @Test
    public void shouldZipNonNilsOfSameSize() {
        final Seq<Tuple2<Tuple2<Integer, Integer>, Integer>> actual = emptyIntInt()
                .put(0, 0)
                .put(1, 1)
                .put(2, 2)
                .zip(com.guizmaii.zazr.collection.List.of(5, 6, 7));
        assertThat(actual).isEqualTo(
                Stream.of(Tuple.of(Tuple.of(0, 0), 5), Tuple.of(Tuple.of(1, 1), 6), Tuple.of(Tuple.of(2, 2), 7)));
    }

    @Test
    public void shouldThrowIfZipWithThatIsNull() {
        assertThrows(NullPointerException.class, () -> emptyMap().zip(null));
    }

    // -- zipWithIndex

    @Test
    public void shouldZipNilWithIndex() {
        assertThat(emptyMap().zipWithIndex()).isEqualTo(Stream.empty());
    }

    @Test
    public void shouldZipNonNilWithIndex() {
        final Seq<Tuple2<Tuple2<Integer, Integer>, Integer>> actual = emptyIntInt()
                .put(0, 0)
                .put(1, 1)
                .put(2, 2)
                .zipWithIndex();
        assertThat(actual).isEqualTo(
                Stream.of(Tuple.of(Tuple.of(0, 0), 0), Tuple.of(Tuple.of(1, 1), 1), Tuple.of(Tuple.of(2, 2), 2)));
    }

    // -- zipAll

    @Test
    public void shouldZipAllNils() {
        final Seq<Tuple2<Tuple2<Integer, Object>, Object>> actual = emptyInt().zipAll(empty(), Tuple.of(-1, "x"), "z");
        assertThat(actual).isEqualTo(Stream.empty());
    }

    @Test
    public void shouldZipAllEmptyAndNonNil() {
        final Seq<Tuple2<Tuple2<Integer, Object>, Object>> actual = emptyInt().zipAll(com.guizmaii.zazr.collection.List.of(1), Tuple.of(-1, "x"), "z");
        assertThat(actual).isEqualTo(Stream.of(Tuple.of(Tuple.of(-1, "x"), 1)));
    }

    @Test
    public void shouldZipAllNonEmptyAndNil() {
        final Seq<Tuple2<Tuple2<Integer, Object>, Object>> actual = emptyInt().put(0, 1).zipAll(empty(), Tuple.of(-1, "x"), "z");
        assertThat(actual).isEqualTo(Stream.of(Tuple.of(Tuple.of(0, 1), "z")));
    }

    @Test
    public void shouldRejectNullZipAllFillValues() {
        assertThatNullPointerException().isThrownBy(() -> emptyInt().zipAll(com.guizmaii.zazr.collection.List.of(1), null, "z"));
        assertThatNullPointerException().isThrownBy(() -> emptyInt().zipAll(com.guizmaii.zazr.collection.List.of(1), Tuple.of(-1, "x"), null));
    }

    @Test
    public void shouldZipAllNonNilsIfThisIsSmaller() {
        final Seq<Tuple2<Tuple2<Integer, Integer>, String>> actual = emptyIntInt()
                .put(1, 1)
                .put(2, 2)
                .zipAll(of("a", "b", "c"), Tuple.of(9, 10), "z");
        final Seq<Tuple2<Tuple2<Object, Object>, String>> expected = Stream.of(Tuple.of(Tuple.of(1, 1), "a"),
                Tuple.of(Tuple.of(2, 2), "b"), Tuple.of(Tuple.of(9, 10), "c"));
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void shouldZipAllNonNilsIfThisIsMoreSmaller() {
        final Seq<Tuple2<Tuple2<Integer, Integer>, String>> actual = emptyIntInt()
                .put(1, 1)
                .put(2, 2)
                .zipAll(of("a", "b", "c", "d"), Tuple.of(9, 10), "z");
        final Seq<Tuple2<Tuple2<Object, Object>, String>> expected = Stream.of(Tuple.of(Tuple.of(1, 1), "a"),
                Tuple.of(Tuple.of(2, 2), "b"), Tuple.of(Tuple.of(9, 10), "c"), Tuple.of(Tuple.of(9, 10), "d"));
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void shouldZipAllNonNilsIfThatIsSmaller() {
        final Seq<Tuple2<Tuple2<Integer, Integer>, String>> actual = emptyIntInt()
                .put(1, 1)
                .put(2, 2)
                .put(3, 3)
                .zipAll(this.of("a", "b"), Tuple.of(9, 10), "z");
        final Seq<Tuple2<Tuple2<Object, Object>, String>> expected = Stream.of(Tuple.of(Tuple.of(1, 1), "a"),
                Tuple.of(Tuple.of(2, 2), "b"), Tuple.of(Tuple.of(3, 3), "z"));
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void shouldZipAllNonNilsIfThatIsMoreSmaller() {
        final Seq<Tuple2<Tuple2<Integer, Integer>, String>> actual = emptyIntInt()
                .put(1, 1)
                .put(2, 2)
                .put(3, 3)
                .put(4, 4)
                .zipAll(of("a", "b"), Tuple.of(9, 10), "z");
        final Seq<Tuple2<Tuple2<Object, Object>, String>> expected = Stream.of(Tuple.of(Tuple.of(1, 1), "a"),
                Tuple.of(Tuple.of(2, 2), "b"), Tuple.of(Tuple.of(3, 3), "z"), Tuple.of(Tuple.of(4, 4), "z"));
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void shouldZipAllNonNilsOfSameSize() {
        final Seq<Tuple2<Tuple2<Integer, Integer>, String>> actual = emptyIntInt()
                .put(1, 1)
                .put(2, 2)
                .put(3, 3)
                .zipAll(of("a", "b", "c"), Tuple.of(9, 10), "z");
        final Seq<Tuple2<Tuple2<Object, Object>, String>> expected = Stream.of(Tuple.of(Tuple.of(1, 1), "a"),
                Tuple.of(Tuple.of(2, 2), "b"), Tuple.of(Tuple.of(3, 3), "c"));
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void shouldThrowIfZipAllWithThatIsNull() {
        assertThrows(NullPointerException.class, () -> emptyMap().zipAll(null, null, null));
    }

    // -- special cases

    @Override
    public void shouldComputeDistinctOfNonEmptyTraversable() {
        final Map<Integer, Object> testee = emptyInt().put(1, 1).put(2, 2).put(3, 3);
        assertThat(testee.distinct()).isEqualTo(testee);
    }

    @Override
    public void shouldReturnSomeTailWhenCallingTailOptionOnNonNil() {
        assertThat(of(1, 2, 3).tailOption().get()).isEqualTo(Option.some(of(2, 3)).get());
    }

    @Override
    public void shouldFoldRightNonNil() {
        final String actual = of('a', 'b', 'c').foldRight("", (x, xs) -> x + xs);
        final com.guizmaii.zazr.collection.List<String> expected = com.guizmaii.zazr.collection.List.of('a', 'b', 'c').permutations().map(com.guizmaii.zazr.collection.List::mkString);
        assertThat(actual).isIn(expected);
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
            assertThatNullPointerException().isThrownBy(() -> AbstractMapTest.this.<String, String>mapOf("1", null));
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

    @Override
    @Test
    @SuppressWarnings("unchecked")
    public void shouldPartitionIntsInOddAndEvenHavingOddAndEvenNumbers() {
        assertThat(of(1, 2, 3, 4).partition(i -> i % 2 != 0))
                .isEqualTo(Tuple.of(mapOfTuples(Tuple.of(0, 1), Tuple.of(2, 3)),
                        mapOfTuples(Tuple.of(1, 2), Tuple.of(3, 4))));
    }

    @Override
    @Test
    @SuppressWarnings("unchecked")
    public void shouldSpanNonNil() {
        assertThat(of(0, 1, 2, 3).span(i -> i < 2))
                .isEqualTo(Tuple.of(mapOfTuples(Tuple.of(0, 0), Tuple.of(1, 1)),
                mapOfTuples(Tuple.of(2, 2), Tuple.of(3, 3))));
    }

    @Override
    @Test
    @SuppressWarnings("unchecked")
    public void shouldSpanAndNotTruncate() {
        assertThat(of(1, 1, 2, 2, 3, 3).span(x -> x % 2 == 1))
                .isEqualTo(Tuple.of(mapOfTuples(Tuple.of(0,1), Tuple.of(1, 1)),
                        mapOfTuples(Tuple.of(2, 2), Tuple.of(3, 2),
                                Tuple.of(4, 3), Tuple.of(5, 3))));
        assertThat(of(1, 1, 2, 2, 4, 4).span(x -> x == 1))
                .isEqualTo(Tuple.of(mapOfTuples(Tuple.of(0,1), Tuple.of(1, 1)),
                        mapOfTuples(Tuple.of(2, 2), Tuple.of(3, 2),
                        Tuple.of(4, 4), Tuple.of(5, 4))));
    }

    @Override
    @Test
    public void shouldNonNilGroupByIdentity() {
        final Map<?, ?> actual = of('a', 'b', 'c').groupBy(Function.identity());
        final Map<?, ?> expected = com.guizmaii.zazr.collection.LinkedHashMap.empty().put('a', mapOf(0, 'a')).put('b', mapOf(1,'b'))
                .put('c', mapOf(2,'c'));
        assertThat(actual).isEqualTo(expected);
    }

    // -- null values: everything but get(k) itself must work on a map holding a null value, since Some(null) does not exist

    @Nested
    class NullValueTests {

        @Test
        public void shouldRejectOfWithNullValue() {
            assertThatNullPointerException().isThrownBy(() -> AbstractMapTest.this.<String, String>mapOf("k", null));
        }

        @Test
        public void shouldRejectPutOfNullValue() {
            assertThatNullPointerException().isThrownBy(() -> AbstractMapTest.this.<String, String>mapOf("k", "v").put("k", null));
        }

        @Test
        public void shouldRejectPutWithMergeResultingInNull() {
            assertThatNullPointerException().isThrownBy(() -> AbstractMapTest.this.<String, String>mapOf("k", "v").put("k", "w", (a, b) -> null));
        }

        @Test
        public void shouldRejectComputeIfPresentWithNullResult() {
            assertThatNullPointerException().isThrownBy(() -> AbstractMapTest.this.<String, String>mapOf("k", "v").computeIfPresent("k", (k, v) -> null));
        }

        @Test
        public void shouldRejectComputeIfAbsentWithNullResult() {
            assertThatNullPointerException().isThrownBy(() -> AbstractMapTest.this.<String, String>emptyMap().computeIfAbsent("k", k -> null));
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
    // must treat an absent key exactly like AbstractMapTest.get/getOrElse do, and the sentinel
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
            final Tuple2<String, ? extends Map<String, String>> result = AbstractMapTest.this.<String, String>emptyMap().computeIfAbsent("k", k -> "computed");
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
            final Map<String, String> map = AbstractMapTest.this.<String, String>emptyMap().put("k", "v", (a, b) -> "merged");
            assertThat(map).isEqualTo(mapOf("k", "v"));
        }

        @Test
        public void mapKeysCollapsingOntoAbsentTargetTakesTheMappedValue() {
            final Map<String, String> map = mapOf("a", "1");
            assertThat(map.mapKeys(k -> "x", (v1, v2) -> "merged")).isEqualTo(mapOf("x", "1"));
        }
    }
}
