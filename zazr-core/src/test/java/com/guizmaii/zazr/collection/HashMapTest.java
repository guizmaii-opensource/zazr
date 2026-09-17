package com.guizmaii.zazr.collection;

import com.guizmaii.zazr.Tuple2;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Spliterator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Stream;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;


public class HashMapTest extends AbstractMapTest {

    @Override
    protected String className() {
        return "HashMap";
    }

    @Override
    <T1, T2> java.util.Map<T1, T2> javaEmptyMap() {
        return new java.util.HashMap<>();
    }

    @Override
    protected <T1 extends Comparable<? super T1>, T2> HashMap<T1, T2> emptyMap() {
        return HashMap.empty();
    }

    @Override
    protected <K extends Comparable<? super K>, V, T extends V> Collector<T, ArrayList<T>, ? extends Map<K, V>> collectorWithMapper(Function<? super T, ? extends K> keyMapper) {
        return HashMap.collector(keyMapper);
    }

    @Override
    protected <K extends Comparable<? super K>, V, T> Collector<T, ArrayList<T>, ? extends Map<K, V>> collectorWithMappers(Function<? super T, ? extends K> keyMapper, Function<? super T, ? extends V> valueMapper) {
        return HashMap.collector(keyMapper, valueMapper);
    }

    @Override
    protected <T> Collector<Tuple2<Integer, T>, ArrayList<Tuple2<Integer, T>>, ? extends Map<Integer, T>> mapCollector() {
        return HashMap.collector();
    }

    @SuppressWarnings("varargs")
    @SafeVarargs
    @Override
    protected final <K extends Comparable<? super K>, V> HashMap<K, V> mapOfTuples(Tuple2<? extends K, ? extends V>... entries) {
        return HashMap.ofEntries(entries);
    }

    @Override
    protected <K extends Comparable<? super K>, V> Map<K, V> mapOfTuples(Iterable<? extends Tuple2<? extends K, ? extends V>> entries) {
        return HashMap.ofEntries(entries);
    }

    @SuppressWarnings("varargs")
    @SafeVarargs
    @Override
    protected final <K extends Comparable<? super K>, V> HashMap<K, V> mapOfEntries(java.util.Map.Entry<? extends K, ? extends V>... entries) {
        return HashMap.ofEntries(entries);
    }

    @Override
    protected <K extends Comparable<? super K>, V> HashMap<K, V> mapOf(K k1, V v1) {
        return HashMap.of(k1, v1);
    }

    @Override
    protected <K extends Comparable<? super K>, V> HashMap<K, V> mapOf(K k1, V v1, K k2, V v2) {
        return HashMap.of(k1, v1, k2, v2);
    }

    @Override
    protected <K extends Comparable<? super K>, V> HashMap<K, V> mapOf(K k1, V v1, K k2, V v2, K k3, V v3) {
        return HashMap.of(k1, v1, k2, v2, k3, v3);
    }

    @Override
    protected <T, K extends Comparable<? super K>, V> Map<K, V> mapOf(Stream<? extends T> stream, Function<? super T, ? extends K> keyMapper, Function<? super T, ? extends V> valueMapper) {
        return HashMap.ofAll(stream, keyMapper, valueMapper);
    }

    @Override
    protected <T, K extends Comparable<? super K>, V> Map<K, V> mapOf(Stream<? extends T> stream, Function<? super T, Tuple2<? extends K, ? extends V>> f) {
        return HashMap.ofAll(stream, f);
    }

    @Override
    protected <K extends Comparable<? super K>, V> HashMap<K, V> mapTabulate(int n, Function<? super Integer, ? extends Tuple2<? extends K, ? extends V>> f) {
        return HashMap.tabulate(n, f);
    }

    @Override
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

    @Nested
    class IssequentialTests {
        @Test
        public void shouldReturnFalseWhenIsSequentialCalled() {
            assertThat(of(1, 2, 3).isSequential()).isFalse();
        }
    }

}
