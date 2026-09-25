package com.guizmaii.zazr.collection;

import com.guizmaii.zazr.Tuple;
import com.guizmaii.zazr.Tuple2;
import com.guizmaii.zazr.collection.internal.Collections;
import com.guizmaii.zazr.collection.internal.HashArrayMappedTrie;
import com.guizmaii.zazr.collection.internal.HashArrayMappedTrieBuilder;
import com.guizmaii.zazr.collection.internal.Iterator;
import com.guizmaii.zazr.collection.internal.JavaConverters;
import com.guizmaii.zazr.collection.internal.MapViews;
import com.guizmaii.zazr.collection.internal.Maps;
import com.guizmaii.zazr.control.Option;
import java.util.Objects;
import java.util.function.*;
import java.util.stream.Collector;
import org.jspecify.annotations.Nullable;

/**
 * An immutable {@code HashMap} implementation based on a
 * <a href="https://en.wikipedia.org/wiki/Hash_array_mapped_trie">Hash array mapped trie (HAMT)</a>.
 * <p>
 * Complexity: lookups, insertions and removals by key are effectively O(1); an insertion or a removal copies a few
 * small arrays and shares the rest with the original map. The methods without a note of their own (map, filter,
 * the folds, the conversions) walk the entries once, O(n), and those that build a new map insert each kept entry in
 * effectively O(1).
 *
 * @param <K> Key type
 * @param <V> Value type
 * @author Ruslan Sennov, Patryk Najda, Daniel Dietrich
 */
public final class HashMap<K extends @Nullable Object, V extends @Nullable Object> implements Map<K, V> {

    private static final HashMap<?, ?> EMPTY = new HashMap<>(HashArrayMappedTrie.empty());

    private final HashArrayMappedTrie<K, V> trie;

    private HashMap(HashArrayMappedTrie<K, V> trie) {
        this.trie = trie;
    }

    /**
     * Returns a {@link java.util.stream.Collector} which may be used in conjunction with
     * {@link java.util.stream.Stream#collect(java.util.stream.Collector)} to obtain a {@link HashMap}.
     *
     * @param <K> The key type
     * @param <V> The value type
     * @return A {@link HashMap} Collector.
     */
    public static <K extends @Nullable Object, V extends @Nullable Object> Collector<Tuple2<K, V>, Builder<K, V>, HashMap<K, V>> collector() {
        final Supplier<Builder<K, V>> supplier = HashMap::newBuilder;
        final BiConsumer<Builder<K, V>, Tuple2<K, V>> accumulator = Builder::put;
        final BinaryOperator<Builder<K, V>> combiner = (left, right) -> left.putAll(right.result());
        final Function<Builder<K, V>, HashMap<K, V>> finisher = Builder::result;
        return Collector.of(supplier, accumulator, combiner, finisher);
    }

    /**
     * Returns a {@link java.util.stream.Collector} which may be used in conjunction with
     * {@link java.util.stream.Stream#collect(java.util.stream.Collector)} to obtain a {@link HashMap}.
     *
     * @param keyMapper The key mapper
     * @param <K> The key type
     * @param <V> The value type
     * @param <T> Initial {@link java.util.stream.Stream} elements type
     * @return A {@link HashMap} Collector.
     */
    public static <K extends @Nullable Object, V extends @Nullable Object, T extends V> Collector<T, Builder<K, V>, HashMap<K, V>> collector(Function<? super T, ? extends K> keyMapper) {
        Objects.requireNonNull(keyMapper, "keyMapper is null");
        return HashMap.collector(keyMapper, v -> v);
    }

    /**
     * Returns a {@link java.util.stream.Collector} which may be used in conjunction with
     * {@link java.util.stream.Stream#collect(java.util.stream.Collector)} to obtain a {@link HashMap}.
     *
     * @param keyMapper The key mapper
     * @param valueMapper The value mapper
     * @param <K> The key type
     * @param <V> The value type
     * @param <T> Initial {@link java.util.stream.Stream} elements type
     * @return A {@link HashMap} Collector.
     */
    public static <K extends @Nullable Object, V extends @Nullable Object, T extends @Nullable Object> Collector<T, Builder<K, V>, HashMap<K, V>> collector(
            Function<? super T, ? extends K> keyMapper, Function<? super T, ? extends V> valueMapper) {
        Objects.requireNonNull(keyMapper, "keyMapper is null");
        Objects.requireNonNull(valueMapper, "valueMapper is null");
        final Supplier<Builder<K, V>> supplier = HashMap::newBuilder;
        final BiConsumer<Builder<K, V>, T> accumulator = (builder, t) -> builder.put(keyMapper.apply(t), valueMapper.apply(t));
        final BinaryOperator<Builder<K, V>> combiner = (left, right) -> left.putAll(right.result());
        final Function<Builder<K, V>, HashMap<K, V>> finisher = Builder::result;
        return Collector.of(supplier, accumulator, combiner, finisher);
    }

    /**
     * Returns a new {@link Builder}: the cheapest way to build a HashMap from many entries. The builder updates the
     * nodes of the trie it creates in place, where successive puts copy the path from the root to the new entry.
     *
     * @param <K> The key type
     * @param <V> The value type
     * @return an empty builder
     */
    public static <K extends @Nullable Object, V extends @Nullable Object> Builder<K, V> newBuilder() {
        return new Builder<>();
    }

    /**
     * Returns the empty HashMap.
     *
     * @param <K> The key type
     * @param <V> The value type
     * @return The empty HashMap (a shared singleton instance).
     */
    @SuppressWarnings("unchecked")
    public static <K extends @Nullable Object, V extends @Nullable Object> HashMap<K, V> empty() {
        return (HashMap<K, V>) EMPTY;
    }

    /**
     * Narrows a {@code HashMap<? extends K, ? extends V>} to {@code HashMap<K, V>} via a
     * type-safe cast. Safe here because the map is immutable and no elements
     * can be added that would violate the type (covariance)
     *
     * @param hashMap the map to narrow
     * @param <K>     the target key type
     * @param <V>     the target value type
     * @return the same map viewed as {@code HashMap<K, V>}
     */
    @SuppressWarnings("unchecked")
    public static <K extends @Nullable Object, V extends @Nullable Object> HashMap<K, V> narrow(HashMap<? extends K, ? extends V> hashMap) {
        return (HashMap<K, V>) hashMap;
    }

    /**
     * Returns a singleton {@code HashMap}, i.e. a {@code HashMap} of one element.
     *
     * @param entry A map entry.
     * @param <K>   The key type
     * @param <V>   The value type
     * @return A new Map containing the given entry
     */
    public static <K extends @Nullable Object, V extends @Nullable Object> HashMap<K, V> of(Tuple2<? extends K, ? extends V> entry) {
        return new HashMap<>(HashArrayMappedTrie.<K, V> empty().put(entry._1(), entry._2()));
    }

    /**
     * Returns a {@code HashMap}, from a source java.util.Map.
     *
     * @param map A map
     * @param <K> The key type
     * @param <V> The value type
     * @return A HashMap containing the given map; the {@link #asJavaMap()} view of a HashMap gives that HashMap
     *         back, not a copy
     */
    @SuppressWarnings("unchecked")
    public static <K extends @Nullable Object, V extends @Nullable Object> HashMap<K, V> ofAll(java.util.Map<? extends K, ? extends V> map) {
        Objects.requireNonNull(map, "map is null");
        if (JavaConverters.underlying(map) instanceof HashMap<?, ?> underlying) {
            return (HashMap<K, V>) underlying;
        }
        final HashArrayMappedTrieBuilder<K, V> builder = new HashArrayMappedTrieBuilder<>("HashMap.Builder");
        for (java.util.Map.Entry<? extends K, ? extends V> entry : map.entrySet()) {
            putChecked(builder, entry.getKey(), entry.getValue());
        }
        return wrap(builder.result());
    }

    /**
     * Returns a {@code HashMap}, from entries mapped from stream.
     *
     * @param stream      the source stream
     * @param keyMapper   the key mapper
     * @param valueMapper the value mapper
     * @param <T>         The stream element type
     * @param <K>         The key type
     * @param <V>         The value type
     * @return A HashMap containing the mapped entries
     */
    public static <T extends @Nullable Object, K extends @Nullable Object, V extends @Nullable Object> HashMap<K, V> ofAll(java.util.stream.Stream<? extends T> stream,
                                                Function<? super T, ? extends K> keyMapper,
                                                Function<? super T, ? extends V> valueMapper) {
        return Maps.ofStream(empty(), stream, keyMapper, valueMapper);
    }

    /**
     * Returns a {@code HashMap}, from entries mapped from stream.
     *
     * @param stream      the source stream
     * @param entryMapper the entry mapper
     * @param <T>         The stream element type
     * @param <K>         The key type
     * @param <V>         The value type
     * @return A HashMap containing the mapped entries
     */
    public static <T extends @Nullable Object, K extends @Nullable Object, V extends @Nullable Object> HashMap<K, V> ofAll(java.util.stream.Stream<? extends T> stream,
                                                Function<? super T, Tuple2<? extends K, ? extends V>> entryMapper) {
        return Maps.ofStream(empty(), stream, entryMapper);
    }

    /**
     * Returns a singleton {@code HashMap}, i.e. a {@code HashMap} of one element.
     *
     * @param key   A singleton map key.
     * @param value A singleton map value.
     * @param <K>   The key type
     * @param <V>   The value type
     * @return A new Map containing the given entry
     */
    public static <K extends @Nullable Object, V extends @Nullable Object> HashMap<K, V> of(K key, V value) {
        return new HashMap<>(HashArrayMappedTrie.<K, V> empty().put(key, value));
    }

    /**
     * Creates a HashMap of the given list of key-value pairs.
     *
     * @param k1  a key for the map
     * @param v1  the value for k1
     * @param k2  a key for the map
     * @param v2  the value for k2
     * @param <K> The key type
     * @param <V> The value type
     * @return A new Map containing the given entries
     */
    public static <K extends @Nullable Object, V extends @Nullable Object> HashMap<K, V> of(K k1, V v1, K k2, V v2) {
        return of(k1, v1).put(k2, v2);
    }

    /**
     * Creates a HashMap of the given list of key-value pairs.
     *
     * @param k1  a key for the map
     * @param v1  the value for k1
     * @param k2  a key for the map
     * @param v2  the value for k2
     * @param k3  a key for the map
     * @param v3  the value for k3
     * @param <K> The key type
     * @param <V> The value type
     * @return A new Map containing the given entries
     */
    public static <K extends @Nullable Object, V extends @Nullable Object> HashMap<K, V> of(K k1, V v1, K k2, V v2, K k3, V v3) {
        return of(k1, v1, k2, v2).put(k3, v3);
    }

    /**
     * Creates a HashMap of the given list of key-value pairs.
     *
     * @param <K> The key type
     * @param <V> The value type
     * @param k1  a key for the map
     * @param v1  the value for k1
     * @param k2  a key for the map
     * @param v2  the value for k2
     * @param k3  a key for the map
     * @param v3  the value for k3
     * @param k4  a key for the map
     * @param v4  the value for k4
     * @return A new Map containing the given entries
     */
    public static <K extends @Nullable Object, V extends @Nullable Object> HashMap<K, V> of(K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4) {
        return of(k1, v1, k2, v2, k3, v3).put(k4, v4);
    }

    /**
     * Creates a HashMap of the given list of key-value pairs.
     *
     * @param k1  a key for the map
     * @param v1  the value for k1
     * @param k2  a key for the map
     * @param v2  the value for k2
     * @param k3  a key for the map
     * @param v3  the value for k3
     * @param k4  a key for the map
     * @param v4  the value for k4
     * @param k5  a key for the map
     * @param v5  the value for k5
     * @param <K> The key type
     * @param <V> The value type
     * @return A new Map containing the given entries
     */
    public static <K extends @Nullable Object, V extends @Nullable Object> HashMap<K, V> of(K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4, K k5, V v5) {
        return of(k1, v1, k2, v2, k3, v3, k4, v4).put(k5, v5);
    }

    /**
     * Creates a HashMap of the given list of key-value pairs.
     *
     * @param k1  a key for the map
     * @param v1  the value for k1
     * @param k2  a key for the map
     * @param v2  the value for k2
     * @param k3  a key for the map
     * @param v3  the value for k3
     * @param k4  a key for the map
     * @param v4  the value for k4
     * @param k5  a key for the map
     * @param v5  the value for k5
     * @param k6  a key for the map
     * @param v6  the value for k6
     * @param <K> The key type
     * @param <V> The value type
     * @return A new Map containing the given entries
     */
    public static <K extends @Nullable Object, V extends @Nullable Object> HashMap<K, V> of(K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4, K k5, V v5, K k6, V v6) {
        return of(k1, v1, k2, v2, k3, v3, k4, v4, k5, v5).put(k6, v6);
    }

    /**
     * Creates a HashMap of the given list of key-value pairs.
     *
     * @param k1  a key for the map
     * @param v1  the value for k1
     * @param k2  a key for the map
     * @param v2  the value for k2
     * @param k3  a key for the map
     * @param v3  the value for k3
     * @param k4  a key for the map
     * @param v4  the value for k4
     * @param k5  a key for the map
     * @param v5  the value for k5
     * @param k6  a key for the map
     * @param v6  the value for k6
     * @param k7  a key for the map
     * @param v7  the value for k7
     * @param <K> The key type
     * @param <V> The value type
     * @return A new Map containing the given entries
     */
    public static <K extends @Nullable Object, V extends @Nullable Object> HashMap<K, V> of(K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4, K k5, V v5, K k6, V v6, K k7, V v7) {
        return of(k1, v1, k2, v2, k3, v3, k4, v4, k5, v5, k6, v6).put(k7, v7);
    }

    /**
     * Creates a HashMap of the given list of key-value pairs.
     *
     * @param k1  a key for the map
     * @param v1  the value for k1
     * @param k2  a key for the map
     * @param v2  the value for k2
     * @param k3  a key for the map
     * @param v3  the value for k3
     * @param k4  a key for the map
     * @param v4  the value for k4
     * @param k5  a key for the map
     * @param v5  the value for k5
     * @param k6  a key for the map
     * @param v6  the value for k6
     * @param k7  a key for the map
     * @param v7  the value for k7
     * @param k8  a key for the map
     * @param v8  the value for k8
     * @param <K> The key type
     * @param <V> The value type
     * @return A new Map containing the given entries
     */
    public static <K extends @Nullable Object, V extends @Nullable Object> HashMap<K, V> of(K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4, K k5, V v5, K k6, V v6, K k7, V v7, K k8, V v8) {
        return of(k1, v1, k2, v2, k3, v3, k4, v4, k5, v5, k6, v6, k7, v7).put(k8, v8);
    }

    /**
     * Creates a HashMap of the given list of key-value pairs.
     *
     * @param k1  a key for the map
     * @param v1  the value for k1
     * @param k2  a key for the map
     * @param v2  the value for k2
     * @param k3  a key for the map
     * @param v3  the value for k3
     * @param k4  a key for the map
     * @param v4  the value for k4
     * @param k5  a key for the map
     * @param v5  the value for k5
     * @param k6  a key for the map
     * @param v6  the value for k6
     * @param k7  a key for the map
     * @param v7  the value for k7
     * @param k8  a key for the map
     * @param v8  the value for k8
     * @param k9  a key for the map
     * @param v9  the value for k9
     * @param <K> The key type
     * @param <V> The value type
     * @return A new Map containing the given entries
     */
    public static <K extends @Nullable Object, V extends @Nullable Object> HashMap<K, V> of(K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4, K k5, V v5, K k6, V v6, K k7, V v7, K k8, V v8, K k9, V v9) {
        return of(k1, v1, k2, v2, k3, v3, k4, v4, k5, v5, k6, v6, k7, v7, k8, v8).put(k9, v9);
    }

    /**
     * Creates a HashMap of the given list of key-value pairs.
     *
     * @param k1  a key for the map
     * @param v1  the value for k1
     * @param k2  a key for the map
     * @param v2  the value for k2
     * @param k3  a key for the map
     * @param v3  the value for k3
     * @param k4  a key for the map
     * @param v4  the value for k4
     * @param k5  a key for the map
     * @param v5  the value for k5
     * @param k6  a key for the map
     * @param v6  the value for k6
     * @param k7  a key for the map
     * @param v7  the value for k7
     * @param k8  a key for the map
     * @param v8  the value for k8
     * @param k9  a key for the map
     * @param v9  the value for k9
     * @param k10 a key for the map
     * @param v10 the value for k10
     * @param <K> The key type
     * @param <V> The value type
     * @return A new Map containing the given entries
     */
    public static <K extends @Nullable Object, V extends @Nullable Object> HashMap<K, V> of(K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4, K k5, V v5, K k6, V v6, K k7, V v7, K k8, V v8, K k9, V v9, K k10, V v10) {
        return of(k1, v1, k2, v2, k3, v3, k4, v4, k5, v5, k6, v6, k7, v7, k8, v8, k9, v9).put(k10, v10);
    }

    /**
     * Returns a HashMap containing up to {@code n} values of a given Function {@code f}
     * over a range of integer values from 0 to {@code n - 1}.
     *
     * @param <K> The key type
     * @param <V> The value type
     * @param n   The number of times to call {@code f}
     * @param f   The Function computing element values
     * @return A HashMap containing the entries {@code f(0), f(1), ..., f(n - 1)}; entries with equal keys collapse
     *         (the later one wins), so the result may contain fewer than {@code n} entries. Empty if {@code n <= 0}.
     * @throws NullPointerException if {@code f} is null
     */
    @SuppressWarnings("unchecked")
    public static <K extends @Nullable Object, V extends @Nullable Object> HashMap<K, V> tabulate(int n, Function<? super Integer, ? extends Tuple2<? extends K, ? extends V>> f) {
        Objects.requireNonNull(f, "f is null");
        return ofEntries(Collections.tabulate(n, (Function<? super Integer, ? extends Tuple2<K, V>>) f));
    }

    /**
     * Returns a HashMap containing tuples returned by {@code n} calls to a given Supplier {@code s}.
     *
     * @param <K> The key type
     * @param <V> The value type
     * @param n   The number of times to call {@code s}
     * @param s   The Supplier computing element values
     * @return A HashMap containing the entries supplied by {@code s}; entries with equal keys collapse
     *         (the later one wins), so the result may contain fewer than {@code n} entries. Empty if {@code n <= 0}.
     * @throws NullPointerException if {@code s} is null
     */
    @SuppressWarnings("unchecked")
    public static <K extends @Nullable Object, V extends @Nullable Object> HashMap<K, V> fill(int n, Supplier<? extends Tuple2<? extends K, ? extends V>> s) {
        Objects.requireNonNull(s, "s is null");
        return ofEntries(Collections.fill(n, (Supplier<? extends Tuple2<K, V>>) s));
    }

    /**
     * Creates a HashMap of the given entries.
     *
     * @param entries Map entries
     * @param <K>     The key type
     * @param <V>     The value type
     * @return A HashMap containing the given entries
     */
    @SafeVarargs
    public static <K extends @Nullable Object, V extends @Nullable Object> HashMap<K, V> ofEntries(java.util.Map.Entry<? extends K, ? extends V> ... entries) {
        Objects.requireNonNull(entries, "entries is null");
        final HashArrayMappedTrieBuilder<K, V> builder = new HashArrayMappedTrieBuilder<>("HashMap.Builder");
        for (java.util.Map.Entry<? extends K, ? extends V> entry : entries) {
            Objects.requireNonNull(entry, "HashMap.ofEntries: entry is null");
            putChecked(builder, entry.getKey(), entry.getValue());
        }
        return wrap(builder.result());
    }

    /**
     * Creates a HashMap of the given entries.
     *
     * @param entries Map entries
     * @param <K>     The key type
     * @param <V>     The value type
     * @return A HashMap containing the given entries
     */
    @SafeVarargs
    public static <K extends @Nullable Object, V extends @Nullable Object> HashMap<K, V> ofEntries(Tuple2<? extends K, ? extends V> ... entries) {
        Objects.requireNonNull(entries, "entries is null");
        final HashArrayMappedTrieBuilder<K, V> builder = new HashArrayMappedTrieBuilder<>("HashMap.Builder");
        for (Tuple2<? extends K, ? extends V> entry : entries) {
            Objects.requireNonNull(entry, "HashMap.ofEntries: entry is null");
            putChecked(builder, entry._1(), entry._2());
        }
        return wrap(builder.result());
    }

    /**
     * Creates a HashMap of the given entries.
     *
     * @param entries Map entries
     * @param <K>     The key type
     * @param <V>     The value type
     * @return A HashMap containing the given entries (the same instance if {@code entries} is already a HashMap, or
     *         the {@link #asJava()} view of one)
     */
    @SuppressWarnings("unchecked")
    public static <K extends @Nullable Object, V extends @Nullable Object> HashMap<K, V> ofEntries(Iterable<? extends Tuple2<? extends K, ? extends V>> entries) {
        Objects.requireNonNull(entries, "entries is null");
        if (entries instanceof HashMap) {
            return (HashMap<K, V>) entries;
        } else if (JavaConverters.underlying(entries) instanceof HashMap<?, ?> underlying) {
            return (HashMap<K, V>) underlying;
        } else {
            final HashArrayMappedTrieBuilder<K, V> builder = new HashArrayMappedTrieBuilder<>("HashMap.Builder");
            for (Tuple2<? extends K, ? extends V> entry : entries) {
                Objects.requireNonNull(entry, "HashMap.ofEntries: entry is null");
                putChecked(builder, entry._1(), entry._2());
            }
            return wrap(builder.result());
        }
    }

    @Override
    public <K2 extends @Nullable Object, V2 extends @Nullable Object> HashMap<K2, V2> mapBoth(Function<? super K, ? extends K2> keyMapper, Function<? super V, ? extends V2> valueMapper) {
        Objects.requireNonNull(keyMapper, "keyMapper is null");
        Objects.requireNonNull(valueMapper, "valueMapper is null");
        final Iterator<Tuple2<K2, V2>> entries = Iterator.ofAll(this).map(entry -> Tuple.of(keyMapper.apply(entry._1()), valueMapper.apply(entry._2())));
        return HashMap.ofEntries(entries);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Complexity: effectively O(1): one lookup, and one {@link #put(Object, Object)} when the key is absent.
     */
    @Override
    public Tuple2<V, HashMap<K, V>> computeIfAbsent(K key, Function<? super K, ? extends V> mappingFunction) {
        return Maps.computeIfAbsent(this, key, mappingFunction);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Complexity: effectively O(1): one lookup, and one {@link #put(Object, Object)} when the key is present.
     */
    @Override
    public Tuple2<Option<V>, HashMap<K, V>> computeIfPresent(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
        return Maps.computeIfPresent(this, key, remappingFunction);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Complexity: effectively O(1): one lookup of the key, then its value is compared.
     */
    @Override
    public boolean contains(Tuple2<K, V> element) {
        return Map.super.contains(element);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Complexity: effectively O(1): one hash lookup.
     */
    @Override
    public boolean containsKey(K key) {
        return trie.containsKey(key);
    }

    @Override
    public HashMap<K, V> filter(BiPredicate<? super K, ? super V> predicate) {
        return Maps.filter(this, this::createFromEntries, predicate);
    }

    @Override
    public HashMap<K, V> reject(BiPredicate<? super K, ? super V> predicate) {
        return Maps.reject(this, this::createFromEntries, predicate);
    }

    @Override
    public HashMap<K, V> filter(Predicate<? super Tuple2<K, V>> predicate) {
        return Maps.filter(this, this::createFromEntries, predicate);
    }

    @Override
    public HashMap<K, V> reject(Predicate<? super Tuple2<K, V>> predicate) {
        return Maps.reject(this, this::createFromEntries, predicate);
    }

    @Override
    public HashMap<K, V> filterKeys(Predicate<? super K> predicate) {
        return Maps.filterKeys(this, this::createFromEntries, predicate);
    }

    @Override
    public HashMap<K, V> rejectKeys(Predicate<? super K> predicate) {
        return Maps.rejectKeys(this, this::createFromEntries, predicate);
    }

    @Override
    public HashMap<K, V> filterValues(Predicate<? super V> predicate) {
        return Maps.filterValues(this, this::createFromEntries, predicate);
    }

    @Override
    public HashMap<K, V> rejectValues(Predicate<? super V> predicate) {
        return Maps.rejectValues(this, this::createFromEntries, predicate);
    }

    @Override
    public <K2 extends @Nullable Object, V2 extends @Nullable Object> HashMap<K2, V2> flatMap(BiFunction<? super K, ? super V, ? extends Iterable<Tuple2<K2, V2>>> mapper) {
        Objects.requireNonNull(mapper, "mapper is null");
        return foldLeft(HashMap.<K2, V2> empty(), (acc, entry) -> {
            for (Tuple2<? extends K2, ? extends V2> mappedEntry : mapper.apply(entry._1(), entry._2())) {
                acc = acc.put(mappedEntry);
            }
            return acc;
        });
    }

    /**
     * {@inheritDoc}
     * <p>
     * Complexity: effectively O(1): one hash lookup.
     */
    @Override
    public Option<V> get(K key) {
        return trie.get(key);
    }

    Option<Tuple2<K, V>> getEntry(K key) {
        return trie.getEntry(key);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Complexity: effectively O(1): one hash lookup.
     */
    @Override
    public V getOrElse(K key, V defaultValue) {
        return trie.getOrElse(key, defaultValue);
    }

    @Override
    public <C extends @Nullable Object> Map<C, HashMap<K, V>> groupBy(Function<? super Tuple2<K, V>, ? extends C> classifier) {
        return Maps.groupBy(this, this::createFromEntries, classifier);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Complexity: O(1).
     */
    @Override
    public boolean isEmpty() {
        return trie.isEmpty();
    }

    /**
     * {@inheritDoc}
     * <p>
     * Complexity: O(1) to create; a whole walk is O(n).
     */
    @Override
    public java.util.Iterator<Tuple2<K, V>> iterator() {
        return trie.iterator();
    }

    /**
     * {@inheritDoc}
     * <p>
     * Complexity: O(n): the keys are copied into a new HashSet.
     */
    @Override
    public Set<K> keySet() {
        return HashSet.ofAll(trie.keysIterator());
    }

    @Override
    public <K2 extends @Nullable Object, V2 extends @Nullable Object> HashMap<K2, V2> collect(BiFunction<? super K, ? super V, ? extends Option<? extends Tuple2<K2, V2>>> mapper) {
        Objects.requireNonNull(mapper, "mapper is null");
        return foldLeft(HashMap.empty(), (acc, entry) -> {
            final Option<? extends Tuple2<K2, V2>> collected = Objects.requireNonNull(mapper.apply(entry._1(), entry._2()), "HashMap.collect: mapper returned null");
            return collected.isDefined() ? acc.put(collected.get()) : acc;
        });
    }

    @Override
    public <K2 extends @Nullable Object, V2 extends @Nullable Object> HashMap<K2, V2> map(BiFunction<? super K, ? super V, Tuple2<K2, V2>> mapper) {
        Objects.requireNonNull(mapper, "mapper is null");
        return foldLeft(HashMap.empty(), (acc, entry) -> acc.put(entry.map(mapper)));
    }

    @Override
    public <K2 extends @Nullable Object> HashMap<K2, V> mapKeys(Function<? super K, ? extends K2> keyMapper) {
        Objects.requireNonNull(keyMapper, "keyMapper is null");
        return map((k, v) -> Tuple.of(keyMapper.apply(k), v));
    }

    @Override
    public <K2 extends @Nullable Object> HashMap<K2, V> mapKeys(Function<? super K, ? extends K2> keyMapper, BiFunction<? super V, ? super V, ? extends V> valueMerge) {
        return Collections.mapKeys(this, HashMap.empty(), keyMapper, valueMerge);
    }

    @Override
    public <V2 extends @Nullable Object> HashMap<K, V2> mapValues(Function<? super V, ? extends V2> valueMapper) {
        Objects.requireNonNull(valueMapper, "valueMapper is null");
        return map((k, v) -> Tuple.of(k, valueMapper.apply(v)));
    }

    /**
     * {@inheritDoc}
     * <p>
     * Complexity: O(m) for a map of m entries, one lookup and at most one {@link #put(Object, Object)} each; O(1)
     * when that map is empty, or when this map is empty and that map is a HashMap, which is returned as is.
     */
    @Override
    public HashMap<K, V> merge(Map<? extends K, ? extends V> that) {
        return Maps.merge(this, this::createFromEntries, that);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Complexity: O(m) for a map of m entries, one lookup and one {@link #put(Object, Object)} each; O(1) when that
     * map is empty, or when this map is empty and that map is a HashMap, which is returned as is.
     */
    @Override
    public <U extends V> HashMap<K, V> merge(Map<? extends K, U> that,
                                             BiFunction<? super V, ? super U, ? extends V> collisionResolution) {
        return Maps.merge(this, this::createFromEntries, that, collisionResolution);
    }

    @Override
    public HashMap<K, V> orElse(Iterable<? extends Tuple2<K, V>> other) {
        return isEmpty() ? ofEntries(other) : this;
    }

    @Override
    public HashMap<K, V> orElse(Supplier<? extends Iterable<? extends Tuple2<K, V>>> supplier) {
        return isEmpty() ? ofEntries(supplier.get()) : this;
    }

    @Override
    public Tuple2<HashMap<K, V>, HashMap<K, V>> partition(Predicate<? super Tuple2<K, V>> predicate) {
        return Maps.partition(this, this::createFromEntries, predicate);
    }

    @Override
    public HashMap<K, V> tap(Consumer<? super Tuple2<K, V>> action) {
        return Maps.tap(this, action);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Complexity: effectively O(1): one lookup and one {@link #put(Object, Object)}.
     */
    @Override
    public <U extends V> HashMap<K, V> put(K key, U value, BiFunction<? super V, ? super U, ? extends V> merge) {
        return Maps.put(this, key, value, merge);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Complexity: effectively O(1): one hash lookup, then a copy of a few small arrays; the rest is shared with this
     * map.
     */
    @Override
    public HashMap<K, V> put(K key, V value) {
        return new HashMap<>(trie.put(key, value));
    }

    /**
     * {@inheritDoc}
     * <p>
     * Complexity: effectively O(1), as {@link #put(Object, Object)}.
     */
    @Override
    public HashMap<K, V> put(Tuple2<? extends K, ? extends V> entry) {
        return Maps.put(this, entry);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Complexity: effectively O(1): one lookup and one {@link #put(Object, Object)}.
     */
    @Override
    public <U extends V> HashMap<K, V> put(Tuple2<? extends K, U> entry,
                                           BiFunction<? super V, ? super U, ? extends V> merge) {
        return Maps.put(this, entry, merge);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Complexity: effectively O(1): one hash lookup, then a copy of a few small arrays; the rest is shared with this
     * map.
     */
    @Override
    public HashMap<K, V> remove(K key) {
        final HashArrayMappedTrie<K, V> result = trie.remove(key);
        return result.size() == trie.size() ? this : wrap(result);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Complexity: O(n): every entry is tested, and the kept ones are put in a new map.
     */
    @Override
    @Deprecated
    public HashMap<K, V> removeAll(BiPredicate<? super K, ? super V> predicate) {
        Objects.requireNonNull(predicate, "predicate is null");
        return reject(predicate);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Complexity: O(m) for m given keys, each an effectively O(1) removal.
     */
    @Override
    public HashMap<K, V> removeAll(Iterable<? extends K> keys) {
        Objects.requireNonNull(keys, "keys is null");
        HashArrayMappedTrie<K, V> result = trie;
        for (K key : keys) {
            result = result.remove(key);
        }

        if (result.isEmpty()) {
            return empty();
        } else if (result.size() == trie.size()) {
            return this;
        } else {
            return wrap(result);
        }
    }

    @Override
    @Deprecated
    public HashMap<K, V> removeKeys(Predicate<? super K> predicate) {
        Objects.requireNonNull(predicate, "predicate is null");
        return rejectKeys(predicate);
    }

    @Override
    @Deprecated
    public HashMap<K, V> removeValues(Predicate<? super V> predicate) {
        Objects.requireNonNull(predicate, "predicate is null");
        return rejectValues(predicate);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Complexity: effectively O(1): one lookup, one removal and one insertion.
     */
    @Override
    public HashMap<K, V> replace(Tuple2<K, V> currentElement, Tuple2<K, V> newElement) {
        return Maps.replace(this, currentElement, newElement);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Complexity: effectively O(1), as {@link #replace(Tuple2, Tuple2)}: a map holds an entry once.
     */
    @Override
    public HashMap<K, V> replaceAll(Tuple2<K, V> currentElement, Tuple2<K, V> newElement) {
        return Maps.replaceAll(this, currentElement, newElement);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Complexity: effectively O(1): one lookup, and one {@link #put(Object, Object)} when the key is present.
     */
    @Override
    public HashMap<K, V> replaceValue(K key, V value) {
        return Maps.replaceValue(this, key, value);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Complexity: effectively O(1): one lookup, and one {@link #put(Object, Object)} when the key maps to
     * {@code oldValue}.
     */
    @Override
    public HashMap<K, V> replace(K key, V oldValue, V newValue) {
        return Maps.replace(this, key, oldValue, newValue);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Complexity: O(n): every entry is mapped and put in a new map.
     */
    @Override
    public HashMap<K, V> replaceAll(BiFunction<? super K, ? super V, ? extends V> function) {
        return Maps.replaceAll(this, function);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Complexity: O(m) for m given entries: each is looked up in this map and, when present, put in a new map.
     */
    @Override
    public HashMap<K, V> retainAll(Iterable<? extends Tuple2<K, V>> elements) {
        Objects.requireNonNull(elements, "elements is null");
        HashArrayMappedTrie<K, V> tree = HashArrayMappedTrie.empty();
        for (Tuple2<K, V> entry : elements) {
            if (contains(entry)) {
                tree = tree.put(entry._1(), entry._2());
            }
        }
        return wrap(tree);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Complexity: O(1): the size is stored, not counted.
     */
    @Override
    public int size() {
        return trie.size();
    }

    /**
     * An unmodifiable {@link java.util.Map} view of this HashMap: nothing is copied, reads go through to this map,
     * which never changes, and every mutator of the view (including those of its key set, values, entry set and
     * their iterators, and {@code setValue} on its entries) throws {@link UnsupportedOperationException}. The view
     * equals any {@code java.util.Map} with the same mappings. A mutable copy is
     * {@code new java.util.HashMap<>(map.asJavaMap())}; {@code HashMap.ofAll} given the view returns this map without
     * copying.
     * <p>
     * Complexity: O(1): nothing is copied. {@code get} and {@code containsKey} on the view are effectively O(1).
     *
     * @return an unmodifiable {@code java.util.Map} view
     */
    @Override
    public java.util.Map<K, V> asJavaMap() {
        return MapViews.asJavaMap(this, trie);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Complexity: O(n): the values are copied into a new Vector.
     */
    @Override
    public Vector<V> values() {
        return Vector.ofAll(trie.valuesIterator());
    }

    /**
     * {@inheritDoc}
     * <p>
     * Complexity: O(n log n) against a TreeMap, O(n) against a HashMap or a LinkedHashMap: after a size check,
     * each entry of this map is looked up in the other one. O(1) when the sizes differ.
     */
    @Override
    public boolean equals(@Nullable Object o) {
        return Collections.equals(this, o);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Complexity: O(n): computed from every entry on each call; it is not cached.
     */
    @Override
    public int hashCode() {
        return Collections.hashUnordered(this);
    }

    @Override
    public String toString() {
        return mkString("HashMap(", ", ", ")");
    }

    /**
     * A mutable, single-use accumulator that builds a {@link HashMap}. It is a transient trie: the internal nodes it
     * creates are updated in place while it is open, so a put allocates the new leaf and little else, where a
     * persistent put copies every node on the path from the root. The nodes of a map passed to
     * {@link #putAll(Iterable)} are shared, not copied, and copied only when a later put goes through them: that map
     * never changes. The HashMap returned by {@link #result()} is the one successive puts of the same entries would
     * give, and never changes either.
     * <p>
     * Of entries with equal keys, the one put last is kept, key and value, as with successive puts. Not thread-safe.
     * After {@link #result()} has been called, every method throws {@link IllegalStateException}; create a new builder
     * instead.
     *
     * @param <K> The key type
     * @param <V> The value type
     */
    public static final class Builder<K extends @Nullable Object, V extends @Nullable Object> {

        private final HashArrayMappedTrieBuilder<K, V> trie = new HashArrayMappedTrieBuilder<>("HashMap.Builder");

        private Builder() {
        }

        /**
         * Puts one entry, replacing the entry of an equal key.
         *
         * @param key   the key, never null
         * @param value the value, never null
         * @return this builder
         * @throws IllegalStateException if {@link #result()} has already been called
         * @throws NullPointerException if {@code key} or {@code value} is null
         */
        public Builder<K, V> put(K key, V value) {
            trie.checkOpen();
            Objects.requireNonNull(key, "HashMap.Builder.put: key is null");
            Objects.requireNonNull(value, "HashMap.Builder.put: value is null");
            trie.put(key, value);
            return this;
        }

        /**
         * Puts one entry, replacing the entry of an equal key.
         *
         * @param entry the entry, whose key and value are never null
         * @return this builder
         * @throws IllegalStateException if {@link #result()} has already been called
         * @throws NullPointerException if {@code entry}, its key or its value is null
         */
        public Builder<K, V> put(Tuple2<? extends K, ? extends V> entry) {
            trie.checkOpen();
            Objects.requireNonNull(entry, "HashMap.Builder.put: entry is null");
            return put(entry._1(), entry._2());
        }

        /**
         * Puts all entries of the given iterable, in iteration order. A {@link HashMap} (or the {@link HashMap#asJava()} view
         * of one) given to an empty builder is adopted without copying anything; its nodes are copied only when a later
         * put goes through them, so that map never changes. Otherwise the entries are put one by one, and a null entry,
         * key or value part-way through is rejected only when reached: the builder keeps the entries put before it.
         *
         * @param entries the entries to put
         * @return this builder
         * @throws IllegalStateException if {@link #result()} has already been called
         * @throws NullPointerException if {@code entries} is null, or if it yields a null entry, key or value
         */
        @SuppressWarnings("unchecked")
        public Builder<K, V> putAll(Iterable<? extends Tuple2<? extends K, ? extends V>> entries) {
            trie.checkOpen();
            Objects.requireNonNull(entries, "entries is null");
            if (entries instanceof HashMap<?, ?> map) {
                trie.putAll(((HashMap<K, V>) map).trie);
            } else if (JavaConverters.underlying(entries) instanceof HashMap<?, ?> map) {
                trie.putAll(((HashMap<K, V>) map).trie);
            } else {
                for (Tuple2<? extends K, ? extends V> entry : entries) {
                    put(entry);
                }
            }
            return this;
        }

        /**
         * @return the number of distinct keys put so far
         * @throws IllegalStateException if {@link #result()} has already been called
         */
        public int size() {
            return trie.size();
        }

        /**
         * Builds the HashMap. The builder cannot be used afterwards.
         *
         * @return a HashMap of the entries put
         * @throws IllegalStateException if {@link #result()} has already been called
         */
        public HashMap<K, V> result() {
            return wrap(trie.result());
        }
    }

    // the bulk factories: a transient trie, with the null checks and messages of a persistent put
    private static <K extends @Nullable Object, V extends @Nullable Object> void putChecked(HashArrayMappedTrieBuilder<K, V> builder, K key, V value) {
        Objects.requireNonNull(key, "HashMap: key is null");
        Objects.requireNonNull(value, "HashMap: value is null");
        builder.put(key, value);
    }

    private static <K extends @Nullable Object, V extends @Nullable Object> HashMap<K, V> wrap(HashArrayMappedTrie<K, V> trie) {
        return trie.isEmpty() ? empty() : new HashMap<>(trie);
    }

    // We need this method to narrow the argument of `ofEntries`.
    // If this method is static with type args <K, V>, the jdk fails to infer types at the call site.
    private HashMap<K, V> createFromEntries(Iterable<Tuple2<K, V>> tuples) {
        return HashMap.ofEntries(tuples);
    }

}
