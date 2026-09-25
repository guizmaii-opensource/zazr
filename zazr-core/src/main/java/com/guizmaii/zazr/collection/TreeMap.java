package com.guizmaii.zazr.collection;

import com.guizmaii.zazr.Tuple;
import com.guizmaii.zazr.Tuple2;
import com.guizmaii.zazr.collection.internal.Collections;
import com.guizmaii.zazr.collection.internal.Comparators;
import com.guizmaii.zazr.collection.internal.Iterator;
import com.guizmaii.zazr.collection.internal.JavaConverters;
import com.guizmaii.zazr.collection.internal.Maps;
import com.guizmaii.zazr.collection.internal.RedBlackTree;
import com.guizmaii.zazr.collection.internal.RedBlackTreeBuilder;
import com.guizmaii.zazr.collection.internal.RedBlackTreeModule;
import com.guizmaii.zazr.collection.internal.TreeViews;
import com.guizmaii.zazr.control.Option;
import java.util.Comparator;
import java.util.Objects;
import java.util.function.*;
import java.util.stream.Collector;
import org.jspecify.annotations.Nullable;

/**
 * An immutable {@link SortedMap} implementation backed by a Red-Black tree.
 *
 * <p>
 * Provides efficient sorted key access and typical map operations in a functional style.
 *
 * @param <K> the type of keys maintained by this map
 * @param <V> the type of mapped values
 * @author Daniel Dietrich
 */
// DEV-NOTE: prefer entries.min().get() over iterator().next() for better performance
public final class TreeMap<K extends @Nullable Object, V extends @Nullable Object> implements SortedMap<K, V> {

    private final RedBlackTree<Tuple2<K, V>> entries;

    private TreeMap(RedBlackTree<Tuple2<K, V>> entries) {
        this.entries = entries;
    }

    /**
     * Returns a {@link Collector} which may be used in conjunction with
     * {@link java.util.stream.Stream#collect(Collector)} to obtain a
     * {@link TreeMap}.
     * <p>
     * The natural comparator is used to compare TreeMap keys.
     *
     * @param <K> The key type
     * @param <V> The value type
     * @return A {@link TreeMap} Collector.
     */
    public static <K extends Comparable<? super K>, V extends @Nullable Object> Collector<Tuple2<K, V>, Builder<K, V>, TreeMap<K, V>> collector() {
        return createCollector(EntryComparator.natural());
    }

    /**
     * Returns a {@link Collector} which may be used in conjunction with
     * {@link java.util.stream.Stream#collect(Collector)} to obtain a
     * {@link TreeMap}.
     *
     * @param <K>           The key type
     * @param <V>           The value type
     * @param keyComparator The comparator used to sort the entries by their key.
     * @return A {@link TreeMap} Collector.
     */
    public static <K extends @Nullable Object, V extends @Nullable Object> Collector<Tuple2<K, V>, Builder<K, V>, TreeMap<K, V>> collector(Comparator<? super K> keyComparator) {
        return createCollector(EntryComparator.of(keyComparator));
    }

    /**
     * Returns a {@link java.util.stream.Collector} which may be used in conjunction with
     * {@link java.util.stream.Stream#collect(java.util.stream.Collector)} to obtain a {@link TreeMap}.
     * <p>
     * The natural comparator is used to compare TreeMap keys.
     *
     * @param keyMapper The key mapper
     * @param <K> The key type
     * @param <V> The value type
     * @param <T> Initial {@link java.util.stream.Stream} elements type
     * @return A {@link TreeMap} Collector.
     */
    public static <K extends Comparable<? super K>, V extends @Nullable Object, T extends V> Collector<T, Builder<K, V>, TreeMap<K, V>> collector(
      Function<? super T, ? extends K> keyMapper) {
        Objects.requireNonNull(keyMapper, "keyMapper is null");
        return createCollector(EntryComparator.natural(), keyMapper, v -> v);
    }

    /**
     * Returns a {@link java.util.stream.Collector} which may be used in conjunction with
     * {@link java.util.stream.Stream#collect(java.util.stream.Collector)} to obtain a {@link TreeMap}.
     * <p>
     * The natural comparator is used to compare TreeMap keys.
     *
     * @param keyMapper The key mapper
     * @param valueMapper The value mapper
     * @param <K> The key type
     * @param <V> The value type
     * @param <T> Initial {@link java.util.stream.Stream} elements type
     * @return A {@link TreeMap} Collector.
     */
    public static <K extends Comparable<? super K>, V extends @Nullable Object, T extends @Nullable Object> Collector<T, Builder<K, V>, TreeMap<K, V>> collector(
      Function<? super T, ? extends K> keyMapper,
      Function<? super T, ? extends V> valueMapper) {
        Objects.requireNonNull(keyMapper, "keyMapper is null");
        Objects.requireNonNull(valueMapper, "valueMapper is null");
        return createCollector(EntryComparator.natural(), keyMapper, valueMapper);
    }

    /**
     * Returns a {@link java.util.stream.Collector} which may be used in conjunction with
     * {@link java.util.stream.Stream#collect(java.util.stream.Collector)} to obtain a {@link TreeMap}.
     *
     * @param keyMapper The key mapper
     * @param <K> The key type
     * @param <V> The value type
     * @param <T> Initial {@link java.util.stream.Stream} elements type
     * @param keyComparator The comparator used to sort the entries by their key.
     * @return A {@link TreeMap} Collector.
     */
    public static <K extends @Nullable Object, V extends @Nullable Object, T extends V> Collector<T, Builder<K, V>, TreeMap<K, V>> collector(
      Comparator<? super K> keyComparator,
      Function<? super T, ? extends K> keyMapper) {
        Objects.requireNonNull(keyComparator, "keyComparator is null");
        Objects.requireNonNull(keyMapper, "keyMapper is null");
        return createCollector(EntryComparator.of(keyComparator), keyMapper, v -> v);
    }

    /**
     * Returns a {@link java.util.stream.Collector} which may be used in conjunction with
     * {@link java.util.stream.Stream#collect(java.util.stream.Collector)} to obtain a {@link TreeMap}.
     *
     * @param keyMapper The key mapper
     * @param valueMapper The value mapper
     * @param <K> The key type
     * @param <V> The value type
     * @param <T> Initial {@link java.util.stream.Stream} elements type
     * @param keyComparator The comparator used to sort the entries by their key.
     * @return A {@link TreeMap} Collector.
     */
    public static <K extends @Nullable Object, V extends @Nullable Object, T extends @Nullable Object> Collector<T, Builder<K, V>, TreeMap<K, V>> collector(
      Comparator<? super K> keyComparator,
      Function<? super T, ? extends K> keyMapper, Function<? super T, ? extends V> valueMapper) {
        Objects.requireNonNull(keyComparator, "keyComparator is null");
        Objects.requireNonNull(keyMapper, "keyMapper is null");
        Objects.requireNonNull(valueMapper, "valueMapper is null");
        return createCollector(EntryComparator.of(keyComparator), keyMapper, valueMapper);
    }

    /**
     * Returns a new {@link Builder} ordered by the natural order of the keys.
     *
     * @param <K> The key type
     * @param <V> The value type
     * @return an empty builder
     */
    public static <K extends Comparable<? super K>, V extends @Nullable Object> Builder<K, V> newBuilder() {
        return new Builder<>(EntryComparator.natural());
    }

    /**
     * Returns a new {@link Builder}: the cheapest way to build a TreeMap from many entries. The entries are buffered,
     * and {@link Builder#result()} sorts them once and builds the balanced tree bottom-up, instead of rebalancing the
     * tree after every insertion.
     *
     * @param keyComparator the order of the keys
     * @param <K>           The key type
     * @param <V>           The value type
     * @return an empty builder
     * @throws NullPointerException if {@code keyComparator} is null
     */
    public static <K extends @Nullable Object, V extends @Nullable Object> Builder<K, V> newBuilder(Comparator<? super K> keyComparator) {
        return new Builder<>(EntryComparator.of(keyComparator));
    }

    /**
     * Returns the empty TreeMap. The underlying key comparator is the natural comparator of K.
     *
     * @param <K> The key type
     * @param <V> The value type
     * @return A new empty TreeMap.
     */
    public static <K extends Comparable<? super K>, V extends @Nullable Object> TreeMap<K, V> empty() {
        return new TreeMap<>(RedBlackTree.empty(EntryComparator.natural()));
    }

    /**
     * Returns the empty TreeMap using the given key comparator.
     *
     * @param <K>           The key type
     * @param <V>           The value type
     * @param keyComparator The comparator used to sort the entries by their key.
     * @return A new empty TreeMap.
     */
    public static <K extends @Nullable Object, V extends @Nullable Object> TreeMap<K, V> empty(Comparator<? super K> keyComparator) {
        return new TreeMap<>(RedBlackTree.empty(EntryComparator.of(keyComparator)));
    }

    /**
     * Narrows a {@code TreeMap<? extends K, ? extends V>} to {@code TreeMap<K, V>} via a
     * type-safe cast. Safe here because the map is immutable and no elements
     * can be added that would violate the type (covariance)
     * <p>
     * CAUTION: If {@code K} is narrowed, the underlying {@code Comparator} might fail!
     *
     * @param treeMap the map to narrow
     * @param <K>     the target key type
     * @param <V>     the target value type
     * @return the same map viewed as {@code TreeMap<K, V>}
     */
    @SuppressWarnings("unchecked")
    public static <K extends @Nullable Object, V extends @Nullable Object> TreeMap<K, V> narrow(TreeMap<? extends K, ? extends V> treeMap) {
        return (TreeMap<K, V>) treeMap;
    }

    /**
     * Returns a singleton {@code TreeMap}, i.e. a {@code TreeMap} of one entry.
     * The underlying key comparator is the natural comparator of K.
     *
     * @param <K>   The key type
     * @param <V>   The value type
     * @param entry A map entry.
     * @return A new TreeMap containing the given entry.
     */
    public static <K extends Comparable<? super K>, V extends @Nullable Object> TreeMap<K, V> of(Tuple2<? extends K, ? extends V> entry) {
        Objects.requireNonNull(entry, "entry is null");
        return createFromTuple(EntryComparator.natural(), entry);
    }

    /**
     * Returns a singleton {@code TreeMap}, i.e. a {@code TreeMap} of one entry using a specific key comparator.
     *
     * @param <K>           The key type
     * @param <V>           The value type
     * @param keyComparator The comparator used to sort the entries by their key.
     * @param entry         A map entry.
     * @return A new TreeMap containing the given entry.
     */
    public static <K extends @Nullable Object, V extends @Nullable Object> TreeMap<K, V> of(Comparator<? super K> keyComparator, Tuple2<? extends K, ? extends V> entry) {
        Objects.requireNonNull(entry, "entry is null");
        return createFromTuple(EntryComparator.of(keyComparator), entry);
    }

    /**
     * Returns a {@code TreeMap}, from a source java.util.Map.
     *
     * @param map A map
     * @param <K> The key type
     * @param <V> The value type
     * @return A new Map containing the given map; the {@link #asJavaMap()} view of a TreeMap in the natural order
     *         gives that TreeMap back, not a copy
     */
    public static <K extends Comparable<? super K>, V extends @Nullable Object> TreeMap<K, V> ofAll(java.util.Map<? extends K, ? extends V> map) {
        Objects.requireNonNull(map, "map is null");
        return createFromMap(EntryComparator.natural(), map);
    }

    /**
     * Returns a {@code TreeMap}, from entries mapped from stream.
     *
     * @param stream      the source stream
     * @param keyMapper   the key mapper
     * @param valueMapper the value mapper
     * @param <T>         The stream element type
     * @param <K>         The key type
     * @param <V>         The value type
     * @return A new Map
     */
    public static <T extends @Nullable Object, K extends Comparable<? super K>, V extends @Nullable Object> TreeMap<K, V> ofAll(java.util.stream.Stream<? extends T> stream,
                                                                              Function<? super T, ? extends K> keyMapper,
                                                                              Function<? super T, ? extends V> valueMapper) {
        return Maps.ofStream(TreeMap.<K, V> empty(), stream, keyMapper, valueMapper);
    }

    /**
     * Returns a {@code TreeMap}, from entries mapped from stream.
     *
     * @param keyComparator The comparator used to sort the entries by their key.
     * @param stream        the source stream
     * @param keyMapper     the key mapper
     * @param valueMapper   the value mapper
     * @param <T>           The stream element type
     * @param <K>           The key type
     * @param <V>           The value type
     * @return A new Map
     */
    public static <T extends @Nullable Object, K extends @Nullable Object, V extends @Nullable Object> TreeMap<K, V> ofAll(Comparator<? super K> keyComparator,
                                                java.util.stream.Stream<? extends T> stream,
                                                Function<? super T, ? extends K> keyMapper,
                                                Function<? super T, ? extends V> valueMapper) {
        return Maps.ofStream(empty(keyComparator), stream, keyMapper, valueMapper);
    }

    /**
     * Returns a {@code TreeMap}, from entries mapped from stream.
     *
     * @param stream      the source stream
     * @param entryMapper the entry mapper
     * @param <T>         The stream element type
     * @param <K>         The key type
     * @param <V>         The value type
     * @return A new Map
     */
    public static <T extends @Nullable Object, K extends Comparable<? super K>, V extends @Nullable Object> TreeMap<K, V> ofAll(java.util.stream.Stream<? extends T> stream,
                                                                              Function<? super T, Tuple2<? extends K, ? extends V>> entryMapper) {
        return Maps.ofStream(TreeMap.<K, V> empty(), stream, entryMapper, "TreeMap.ofAll: entryMapper returned null");
    }

    /**
     * Returns a {@code TreeMap}, from entries mapped from stream.
     *
     * @param keyComparator The comparator used to sort the entries by their key.
     * @param stream      the source stream
     * @param entryMapper the entry mapper
     * @param <T>         The stream element type
     * @param <K>         The key type
     * @param <V>         The value type
     * @return A new Map
     */
    public static <T extends @Nullable Object, K extends @Nullable Object, V extends @Nullable Object> TreeMap<K, V> ofAll(Comparator<? super K> keyComparator,
                                                java.util.stream.Stream<? extends T> stream,
                                                Function<? super T, Tuple2<? extends K, ? extends V>> entryMapper) {
        return Maps.ofStream(empty(keyComparator), stream, entryMapper, "TreeMap.ofAll: entryMapper returned null");
    }

    /**
     * Returns a {@code TreeMap}, from a source java.util.Map.
     *
     * @param keyComparator The comparator used to sort the entries by their key.
     * @param map           A map
     * @param <K>           The key type
     * @param <V>           The value type
     * @return A new Map containing the given map; the {@link #asJavaMap()} view of a TreeMap ordered by
     *         {@code keyComparator} (the same instance) gives that TreeMap back, not a copy
     */
    public static <K extends @Nullable Object, V extends @Nullable Object> TreeMap<K, V> ofAll(Comparator<? super K> keyComparator, java.util.Map<? extends K, ? extends V> map) {
        Objects.requireNonNull(map, "map is null");
        return createFromMap(EntryComparator.of(keyComparator), map);
    }

    /**
     * Returns a singleton {@code TreeMap}, i.e. a {@code TreeMap} of one element.
     *
     * @param key   A singleton map key.
     * @param value A singleton map value.
     * @param <K>   The key type
     * @param <V>   The value type
     * @return A new Map containing the given entry
     */
    public static <K extends Comparable<? super K>, V extends @Nullable Object> TreeMap<K, V> of(K key, V value) {
        return createFromPairs(EntryComparator.natural(), key, value);
    }

    /**
     * Creates a {@code TreeMap} of the given list of key-value pairs.
     *
     * @param k1  a key for the map
     * @param v1  the value for k1
     * @param k2  a key for the map
     * @param v2  the value for k2
     * @param <K> The key type
     * @param <V> The value type
     * @return A new Map containing the given entries
     */
    public static <K extends Comparable<? super K>, V extends @Nullable Object> TreeMap<K, V> of(K k1, V v1, K k2, V v2) {
        return createFromPairs(EntryComparator.natural(), k1, v1, k2, v2);
    }

    /**
     * Creates a {@code TreeMap} of the given list of key-value pairs.
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
    public static <K extends Comparable<? super K>, V extends @Nullable Object> TreeMap<K, V> of(K k1, V v1, K k2, V v2, K k3, V v3) {
        return createFromPairs(EntryComparator.natural(), k1, v1, k2, v2, k3, v3);
    }

    /**
     * Creates a {@code TreeMap} of the given list of key-value pairs.
     *
     * @param k1  a key for the map
     * @param v1  the value for k1
     * @param k2  a key for the map
     * @param v2  the value for k2
     * @param k3  a key for the map
     * @param v3  the value for k3
     * @param k4  a key for the map
     * @param v4  the value for k4
     * @param <K> The key type
     * @param <V> The value type
     * @return A new Map containing the given entries
     */
    public static <K extends Comparable<? super K>, V extends @Nullable Object> TreeMap<K, V> of(K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4) {
        return createFromPairs(EntryComparator.natural(), k1, v1, k2, v2, k3, v3, k4, v4);
    }

    /**
     * Creates a {@code TreeMap} of the given list of key-value pairs.
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
    public static <K extends Comparable<? super K>, V extends @Nullable Object> TreeMap<K, V> of(K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4, K k5, V v5) {
        return createFromPairs(EntryComparator.natural(), k1, v1, k2, v2, k3, v3, k4, v4, k5, v5);
    }

    /**
     * Creates a {@code TreeMap} of the given list of key-value pairs.
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
    public static <K extends Comparable<? super K>, V extends @Nullable Object> TreeMap<K, V> of(K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4, K k5, V v5, K k6, V v6) {
        return createFromPairs(EntryComparator.natural(), k1, v1, k2, v2, k3, v3, k4, v4, k5, v5, k6, v6);
    }

    /**
     * Creates a {@code TreeMap} of the given list of key-value pairs.
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
    public static <K extends Comparable<? super K>, V extends @Nullable Object> TreeMap<K, V> of(K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4, K k5, V v5, K k6, V v6, K k7, V v7) {
        return createFromPairs(EntryComparator.natural(), k1, v1, k2, v2, k3, v3, k4, v4, k5, v5, k6, v6, k7, v7);
    }

    /**
     * Creates a {@code TreeMap} of the given list of key-value pairs.
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
    public static <K extends Comparable<? super K>, V extends @Nullable Object> TreeMap<K, V> of(K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4, K k5, V v5, K k6, V v6, K k7, V v7, K k8, V v8) {
        return createFromPairs(EntryComparator.natural(), k1, v1, k2, v2, k3, v3, k4, v4, k5, v5, k6, v6, k7, v7, k8, v8);
    }

    /**
     * Creates a {@code TreeMap} of the given list of key-value pairs.
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
    public static <K extends Comparable<? super K>, V extends @Nullable Object> TreeMap<K, V> of(K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4, K k5, V v5, K k6, V v6, K k7, V v7, K k8, V v8, K k9, V v9) {
        return createFromPairs(EntryComparator.natural(), k1, v1, k2, v2, k3, v3, k4, v4, k5, v5, k6, v6, k7, v7, k8, v8, k9, v9);
    }

    /**
     * Creates a {@code TreeMap} of the given list of key-value pairs.
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
    public static <K extends Comparable<? super K>, V extends @Nullable Object> TreeMap<K, V> of(K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4, K k5, V v5, K k6, V v6, K k7, V v7, K k8, V v8, K k9, V v9, K k10, V v10) {
        return createFromPairs(EntryComparator.natural(), k1, v1, k2, v2, k3, v3, k4, v4, k5, v5, k6, v6, k7, v7, k8, v8, k9, v9, k10, v10);
    }

    /**
     * Returns a singleton {@code TreeMap}, i.e. a {@code TreeMap} of one element.
     *
     * @param keyComparator The comparator used to sort the entries by their key.
     * @param key           A singleton map key.
     * @param value         A singleton map value.
     * @param <K>           The key type
     * @param <V>           The value type
     * @return A new Map containing the given entry
     */
    public static <K extends @Nullable Object, V extends @Nullable Object> TreeMap<K, V> of(Comparator<? super K> keyComparator, K key, V value) {
        return createFromPairs(EntryComparator.of(keyComparator), key, value);
    }

    /**
     * Creates a {@code TreeMap} of the given list of key-value pairs.
     *
     * @param keyComparator The comparator used to sort the entries by their key.
     * @param k1            a key for the map
     * @param v1            the value for k1
     * @param k2            a key for the map
     * @param v2            the value for k2
     * @param <K>           The key type
     * @param <V>           The value type
     * @return A new Map containing the given entries
     */
    public static <K extends @Nullable Object, V extends @Nullable Object> TreeMap<K, V> of(Comparator<? super K> keyComparator, K k1, V v1, K k2, V v2) {
        return createFromPairs(EntryComparator.of(keyComparator), k1, v1, k2, v2);
    }

    /**
     * Creates a {@code TreeMap} of the given list of key-value pairs.
     *
     * @param keyComparator The comparator used to sort the entries by their key.
     * @param k1            a key for the map
     * @param v1            the value for k1
     * @param k2            a key for the map
     * @param v2            the value for k2
     * @param k3            a key for the map
     * @param v3            the value for k3
     * @param <K>           The key type
     * @param <V>           The value type
     * @return A new Map containing the given entries
     */
    public static <K extends @Nullable Object, V extends @Nullable Object> TreeMap<K, V> of(Comparator<? super K> keyComparator, K k1, V v1, K k2, V v2, K k3, V v3) {
        return createFromPairs(EntryComparator.of(keyComparator), k1, v1, k2, v2, k3, v3);
    }

    /**
     * Creates a {@code TreeMap} of the given list of key-value pairs.
     *
     * @param keyComparator The comparator used to sort the entries by their key.
     * @param k1            a key for the map
     * @param v1            the value for k1
     * @param k2            a key for the map
     * @param v2            the value for k2
     * @param k3            a key for the map
     * @param v3            the value for k3
     * @param k4            a key for the map
     * @param v4            the value for k4
     * @param <K>           The key type
     * @param <V>           The value type
     * @return A new Map containing the given entries
     */
    public static <K extends @Nullable Object, V extends @Nullable Object> TreeMap<K, V> of(Comparator<? super K> keyComparator, K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4) {
        return createFromPairs(EntryComparator.of(keyComparator), k1, v1, k2, v2, k3, v3, k4, v4);
    }

    /**
     * Creates a {@code TreeMap} of the given list of key-value pairs.
     *
     * @param keyComparator The comparator used to sort the entries by their key.
     * @param k1            a key for the map
     * @param v1            the value for k1
     * @param k2            a key for the map
     * @param v2            the value for k2
     * @param k3            a key for the map
     * @param v3            the value for k3
     * @param k4            a key for the map
     * @param v4            the value for k4
     * @param k5            a key for the map
     * @param v5            the value for k5
     * @param <K>           The key type
     * @param <V>           The value type
     * @return A new Map containing the given entries
     */
    public static <K extends @Nullable Object, V extends @Nullable Object> TreeMap<K, V> of(Comparator<? super K> keyComparator, K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4, K k5, V v5) {
        return createFromPairs(EntryComparator.of(keyComparator), k1, v1, k2, v2, k3, v3, k4, v4, k5, v5);
    }

    /**
     * Creates a {@code TreeMap} of the given list of key-value pairs.
     *
     * @param keyComparator The comparator used to sort the entries by their key.
     * @param k1            a key for the map
     * @param v1            the value for k1
     * @param k2            a key for the map
     * @param v2            the value for k2
     * @param k3            a key for the map
     * @param v3            the value for k3
     * @param k4            a key for the map
     * @param v4            the value for k4
     * @param k5            a key for the map
     * @param v5            the value for k5
     * @param k6            a key for the map
     * @param v6            the value for k6
     * @param <K>           The key type
     * @param <V>           The value type
     * @return A new Map containing the given entries
     */
    public static <K extends @Nullable Object, V extends @Nullable Object> TreeMap<K, V> of(Comparator<? super K> keyComparator, K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4, K k5, V v5, K k6, V v6) {
        return createFromPairs(EntryComparator.of(keyComparator), k1, v1, k2, v2, k3, v3, k4, v4, k5, v5, k6, v6);
    }

    /**
     * Creates a {@code TreeMap} of the given list of key-value pairs.
     *
     * @param keyComparator The comparator used to sort the entries by their key.
     * @param k1            a key for the map
     * @param v1            the value for k1
     * @param k2            a key for the map
     * @param v2            the value for k2
     * @param k3            a key for the map
     * @param v3            the value for k3
     * @param k4            a key for the map
     * @param v4            the value for k4
     * @param k5            a key for the map
     * @param v5            the value for k5
     * @param k6            a key for the map
     * @param v6            the value for k6
     * @param k7            a key for the map
     * @param v7            the value for k7
     * @param <K>           The key type
     * @param <V>           The value type
     * @return A new Map containing the given entries
     */
    public static <K extends @Nullable Object, V extends @Nullable Object> TreeMap<K, V> of(Comparator<? super K> keyComparator, K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4, K k5, V v5, K k6, V v6, K k7, V v7) {
        return createFromPairs(EntryComparator.of(keyComparator), k1, v1, k2, v2, k3, v3, k4, v4, k5, v5, k6, v6, k7, v7);
    }

    /**
     * Creates a {@code TreeMap} of the given list of key-value pairs.
     *
     * @param keyComparator The comparator used to sort the entries by their key.
     * @param k1            a key for the map
     * @param v1            the value for k1
     * @param k2            a key for the map
     * @param v2            the value for k2
     * @param k3            a key for the map
     * @param v3            the value for k3
     * @param k4            a key for the map
     * @param v4            the value for k4
     * @param k5            a key for the map
     * @param v5            the value for k5
     * @param k6            a key for the map
     * @param v6            the value for k6
     * @param k7            a key for the map
     * @param v7            the value for k7
     * @param k8            a key for the map
     * @param v8            the value for k8
     * @param <K>           The key type
     * @param <V>           The value type
     * @return A new Map containing the given entries
     */
    public static <K extends @Nullable Object, V extends @Nullable Object> TreeMap<K, V> of(Comparator<? super K> keyComparator, K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4, K k5, V v5, K k6, V v6, K k7, V v7, K k8, V v8) {
        return createFromPairs(EntryComparator.of(keyComparator), k1, v1, k2, v2, k3, v3, k4, v4, k5, v5, k6, v6, k7, v7, k8, v8);
    }

    /**
     * Creates a {@code TreeMap} of the given list of key-value pairs.
     *
     * @param keyComparator The comparator used to sort the entries by their key.
     * @param k1            a key for the map
     * @param v1            the value for k1
     * @param k2            a key for the map
     * @param v2            the value for k2
     * @param k3            a key for the map
     * @param v3            the value for k3
     * @param k4            a key for the map
     * @param v4            the value for k4
     * @param k5            a key for the map
     * @param v5            the value for k5
     * @param k6            a key for the map
     * @param v6            the value for k6
     * @param k7            a key for the map
     * @param v7            the value for k7
     * @param k8            a key for the map
     * @param v8            the value for k8
     * @param k9            a key for the map
     * @param v9            the value for k9
     * @param <K>           The key type
     * @param <V>           The value type
     * @return A new Map containing the given entries
     */
    public static <K extends @Nullable Object, V extends @Nullable Object> TreeMap<K, V> of(Comparator<? super K> keyComparator, K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4, K k5, V v5, K k6, V v6, K k7, V v7, K k8, V v8, K k9, V v9) {
        return createFromPairs(EntryComparator.of(keyComparator), k1, v1, k2, v2, k3, v3, k4, v4, k5, v5, k6, v6, k7, v7, k8, v8, k9, v9);
    }

    /**
     * Creates a {@code TreeMap} of the given list of key-value pairs.
     *
     * @param keyComparator The comparator used to sort the entries by their key.
     * @param k1            a key for the map
     * @param v1            the value for k1
     * @param k2            a key for the map
     * @param v2            the value for k2
     * @param k3            a key for the map
     * @param v3            the value for k3
     * @param k4            a key for the map
     * @param v4            the value for k4
     * @param k5            a key for the map
     * @param v5            the value for k5
     * @param k6            a key for the map
     * @param v6            the value for k6
     * @param k7            a key for the map
     * @param v7            the value for k7
     * @param k8            a key for the map
     * @param v8            the value for k8
     * @param k9            a key for the map
     * @param v9            the value for k9
     * @param k10           a key for the map
     * @param v10           the value for k10
     * @param <K>           The key type
     * @param <V>           The value type
     * @return A new Map containing the given entries
     */
    public static <K extends @Nullable Object, V extends @Nullable Object> TreeMap<K, V> of(Comparator<? super K> keyComparator, K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4, K k5, V v5, K k6, V v6, K k7, V v7, K k8, V v8, K k9, V v9, K k10, V v10) {
        return createFromPairs(EntryComparator.of(keyComparator), k1, v1, k2, v2, k3, v3, k4, v4, k5, v5, k6, v6, k7, v7, k8, v8, k9, v9, k10, v10);
    }

    /**
     * Returns a TreeMap containing {@code n} values of a given Function {@code f}
     * over a range of integer values from 0 to {@code n - 1}.
     *
     * @param <K>           The key type
     * @param <V>           The value type
     * @param keyComparator The comparator used to sort the entries by their key
     * @param n             The number of times to call {@code f}
     * @param f             The Function computing element values
     * @return A TreeMap containing the entries {@code f(0), f(1), ..., f(n - 1)}; entries whose keys are equal
     *         per {@code keyComparator} collapse (the later one wins), so the result may contain fewer than
     *         {@code n} entries. Empty if {@code n <= 0}.
     * @throws NullPointerException if {@code keyComparator} or {@code f} are null
     */
    public static <K extends @Nullable Object, V extends @Nullable Object> TreeMap<K, V> tabulate(Comparator<? super K> keyComparator, int n, Function<? super Integer, ? extends Tuple2<? extends K, ? extends V>> f) {
        Objects.requireNonNull(f, "f is null");
        return createTreeMap(EntryComparator.of(keyComparator), Collections.tabulate(n, i -> Objects.requireNonNull(f.apply(i), "TreeMap.tabulate: f returned null")));
    }

    /**
     * Returns a TreeMap containing {@code n} values of a given Function {@code f}
     * over a range of integer values from 0 to {@code n - 1}.
     * The underlying key comparator is the natural comparator of K.
     *
     * @param <K> The key type
     * @param <V> The value type
     * @param n   The number of times to call {@code f}
     * @param f   The Function computing element values
     * @return A TreeMap containing the entries {@code f(0), f(1), ..., f(n - 1)}; entries with equal keys collapse
     *         (the later one wins), so the result may contain fewer than {@code n} entries. Empty if {@code n <= 0}.
     * @throws NullPointerException if {@code f} is null or returns null
     */
    public static <K extends Comparable<? super K>, V extends @Nullable Object> TreeMap<K, V> tabulate(int n, Function<? super Integer, ? extends Tuple2<? extends K, ? extends V>> f) {
        Objects.requireNonNull(f, "f is null");
        return createTreeMap(EntryComparator.natural(), Collections.tabulate(n, i -> Objects.requireNonNull(f.apply(i), "TreeMap.tabulate: f returned null")));
    }

    /**
     * Returns a TreeMap containing tuples returned by {@code n} calls to a given Supplier {@code s}.
     *
     * @param <K>           The key type
     * @param <V>           The value type
     * @param keyComparator The comparator used to sort the entries by their key
     * @param n             The number of times to call {@code s}
     * @param s             The Supplier computing element values
     * @return A TreeMap containing the entries supplied by {@code s}; entries whose keys are equal per
     *         {@code keyComparator} collapse (the later one wins), so the result may contain fewer than
     *         {@code n} entries. Empty if {@code n <= 0}.
     * @throws NullPointerException if {@code keyComparator} or {@code s} are null
     */
    @SuppressWarnings("unchecked")
    public static <K extends @Nullable Object, V extends @Nullable Object> TreeMap<K, V> fill(Comparator<? super K> keyComparator, int n, Supplier<? extends Tuple2<? extends K, ? extends V>> s) {
        Objects.requireNonNull(s, "s is null");
        return createTreeMap(EntryComparator.of(keyComparator), Collections.fill(n, () -> Objects.requireNonNull(s.get(), "TreeMap.fill: s returned null")));
    }

    /**
     * Returns a TreeMap containing tuples returned by {@code n} calls to a given Supplier {@code s}.
     * The underlying key comparator is the natural comparator of K.
     *
     * @param <K> The key type
     * @param <V> The value type
     * @param n   The number of times to call {@code s}
     * @param s   The Supplier computing element values
     * @return A TreeMap containing the entries supplied by {@code s}; entries with equal keys collapse
     *         (the later one wins), so the result may contain fewer than {@code n} entries. Empty if {@code n <= 0}.
     * @throws NullPointerException if {@code s} is null or returns null
     */
    public static <K extends Comparable<? super K>, V extends @Nullable Object> TreeMap<K, V> fill(int n, Supplier<? extends Tuple2<? extends K, ? extends V>> s) {
        Objects.requireNonNull(s, "s is null");
        return createTreeMap(EntryComparator.natural(), Collections.fill(n, () -> Objects.requireNonNull(s.get(), "TreeMap.fill: s returned null")));
    }

    /**
     * Creates a {@code TreeMap} of the given entries using the natural key comparator.
     *
     * @param <K>     The key type
     * @param <V>     The value type
     * @param entries Map entries
     * @return A new TreeMap containing the given entries.
     */
    @SuppressWarnings("varargs")
    @SafeVarargs
    public static <K extends Comparable<? super K>, V extends @Nullable Object> TreeMap<K, V> ofEntries(Tuple2<? extends K, ? extends V> ... entries) {
        return createFromTuples(EntryComparator.natural(), entries);
    }

    /**
     * Creates a {@code TreeMap} of the given entries using the given key comparator.
     *
     * @param <K>           The key type
     * @param <V>           The value type
     * @param keyComparator The comparator used to sort the entries by their key.
     * @param entries       Map entries
     * @return A new TreeMap containing the given entries.
     */
    @SuppressWarnings({ "unchecked", "varargs" })
    @SafeVarargs
    public static <K extends @Nullable Object, V extends @Nullable Object> TreeMap<K, V> ofEntries(Comparator<? super K> keyComparator, Tuple2<? extends K, ? extends V> ... entries) {
        return createFromTuples(EntryComparator.of(keyComparator), entries);
    }

    /**
     * Creates a {@code TreeMap} of the given entries using the natural key comparator.
     *
     * @param <K>     The key type
     * @param <V>     The value type
     * @param entries Map entries
     * @return A new TreeMap containing the given entries.
     */
    @SuppressWarnings("varargs")
    @SafeVarargs
    public static <K extends Comparable<? super K>, V extends @Nullable Object> TreeMap<K, V> ofEntries(java.util.Map.Entry<? extends K, ? extends V> ... entries) {
        return createFromMapEntries(EntryComparator.natural(), entries);
    }

    /**
     * Creates a {@code TreeMap} of the given entries using the given key comparator.
     *
     * @param <K>           The key type
     * @param <V>           The value type
     * @param keyComparator The comparator used to sort the entries by their key.
     * @param entries       Map entries
     * @return A new TreeMap containing the given entries.
     */
    @SuppressWarnings("varargs")
    @SafeVarargs
    public static <K extends @Nullable Object, V extends @Nullable Object> TreeMap<K, V> ofEntries(Comparator<? super K> keyComparator, java.util.Map.Entry<? extends K, ? extends V> ... entries) {
        return createFromMapEntries(EntryComparator.of(keyComparator), entries);
    }

    /**
     * Creates a {@code TreeMap} of the given entries.
     *
     * @param <K>     The key type
     * @param <V>     The value type
     * @param entries Map entries
     * @return A new TreeMap containing the given entries.
     */
    public static <K extends Comparable<? super K>, V extends @Nullable Object> TreeMap<K, V> ofEntries(Iterable<? extends Tuple2<? extends K, ? extends V>> entries) {
        return createTreeMap(EntryComparator.natural(), entries);
    }

    /**
     * Creates a {@code TreeMap} of the given entries.
     *
     * @param <K>           The key type
     * @param <V>           The value type
     * @param keyComparator The comparator used to sort the entries by their key.
     * @param entries       Map entries
     * @return A new TreeMap containing the given entries.
     */
    public static <K extends @Nullable Object, V extends @Nullable Object> TreeMap<K, V> ofEntries(Comparator<? super K> keyComparator, Iterable<? extends Tuple2<? extends K, ? extends V>> entries) {
        return createTreeMap(EntryComparator.of(keyComparator), entries);
    }

    // -- TreeMap API

    @Override
    public <K2 extends @Nullable Object, V2 extends @Nullable Object> TreeMap<K2, V2> mapBoth(Function<? super K, ? extends K2> keyMapper, Function<? super V, ? extends V2> valueMapper) {
        return mapBoth(this, EntryComparator.natural(), keyMapper, valueMapper);
    }

    @Override
    public <K2 extends @Nullable Object, V2 extends @Nullable Object> TreeMap<K2, V2> mapBoth(Comparator<? super K2> keyComparator,
                                          Function<? super K, ? extends K2> keyMapper, Function<? super V, ? extends V2> valueMapper) {
        return mapBoth(this, EntryComparator.of(keyComparator), keyMapper, valueMapper);
    }

    @Override
    public Tuple2<V, TreeMap<K, V>> computeIfAbsent(K key, Function<? super K, ? extends V> mappingFunction) {
        return Maps.computeIfAbsent(this, key, mappingFunction);
    }

    @Override
    public Tuple2<Option<V>, TreeMap<K, V>> computeIfPresent(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
        return Maps.computeIfPresent(this, key, remappingFunction);
    }

    /**
     * Creates a lookup-only entry for the given key.
     * <p>
     * {@code entries} is ordered by key alone, so the value of the probe is never read -- it only
     * has to be type-compatible. Kept in one place so the null value is justified exactly once.
     */
    @SuppressWarnings("NullAway")
    private static <K extends @Nullable Object, V extends @Nullable Object> Tuple2<K, V> lookupEntry(K key) {
        return new Tuple2<>(key, null);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Complexity: O(log n) comparisons.
     */
    @Override
    public boolean containsKey(K key) {
        return entries.contains(lookupEntry(key));
    }

    @Override
    public TreeMap<K, V> filter(BiPredicate<? super K, ? super V> predicate) {
        return Maps.filter(this, this::createFromEntries, predicate);
    }

    @Override
    public TreeMap<K, V> reject(BiPredicate<? super K, ? super V> predicate) {
        return Maps.reject(this, this::createFromEntries, predicate);
    }

    @Override
    public TreeMap<K, V> filter(Predicate<? super Tuple2<K, V>> predicate) {
        return Maps.filter(this, this::createFromEntries, predicate);
    }

    @Override
    public TreeMap<K, V> reject(Predicate<? super Tuple2<K, V>> predicate) {
        return Maps.reject(this, this::createFromEntries, predicate);
    }

    @Override
    public TreeMap<K, V> filterKeys(Predicate<? super K> predicate) {
        return Maps.filterKeys(this, this::createFromEntries, predicate);
    }

    @Override
    public TreeMap<K, V> rejectKeys(Predicate<? super K> predicate) {
        return Maps.rejectKeys(this, this::createFromEntries, predicate);
    }

    @Override
    public TreeMap<K, V> filterValues(Predicate<? super V> predicate) {
        return Maps.filterValues(this, this::createFromEntries, predicate);
    }

    @Override
    public TreeMap<K, V> rejectValues(Predicate<? super V> predicate) {
        return Maps.rejectValues(this, this::createFromEntries, predicate);
    }

    @Override
    public <K2 extends @Nullable Object, V2 extends @Nullable Object> TreeMap<K2, V2> flatMap(BiFunction<? super K, ? super V, ? extends Iterable<Tuple2<K2, V2>>> mapper) {
        return flatMap(this, EntryComparator.natural(), mapper);
    }

    @Override
    public <K2 extends @Nullable Object, V2 extends @Nullable Object> TreeMap<K2, V2> flatMap(Comparator<? super K2> keyComparator,
                                            BiFunction<? super K, ? super V, ? extends Iterable<Tuple2<K2, V2>>> mapper) {
        return flatMap(this, EntryComparator.of(keyComparator), mapper);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Complexity: O(log n) comparisons.
     */
    @Override
    public Option<V> get(K key) {
        return entries.find(TreeMap.<K, V>lookupEntry(key)).map(Tuple2::_2);
    }

    @Override
    public V getOrElse(K key, V defaultValue) {
        return get(key).getOrElse(defaultValue);
    }

    @Override
    public <C extends @Nullable Object> Map<C, TreeMap<K, V>> groupBy(Function<? super Tuple2<K, V>, ? extends C> classifier) {
        return Maps.groupBy(this, this::createFromEntries, classifier, "TreeMap.groupBy: classifier returned null");
    }

    @Override
    public boolean isEmpty() {
        return entries.isEmpty();
    }

    /**
     * {@inheritDoc}
     * <p>
     * Complexity: O(log n) to create (the path to the least entry); a whole walk is O(n).
     */
    @Override
    public java.util.Iterator<Tuple2<K, V>> iterator() {
        return entries.iterator();
    }

    @Override
    public SortedSet<K> keySet() {
        return TreeSet.ofAll(comparator(), Iterator.ofAll(this).map(Tuple2::_1));
    }

    @Override
    public <K2 extends @Nullable Object, V2 extends @Nullable Object> TreeMap<K2, V2> collect(BiFunction<? super K, ? super V, ? extends Option<? extends Tuple2<K2, V2>>> mapper) {
        return collect(this, EntryComparator.natural(), mapper);
    }

    @Override
    public <K2 extends @Nullable Object, V2 extends @Nullable Object> TreeMap<K2, V2> collect(Comparator<? super K2> keyComparator, BiFunction<? super K, ? super V, ? extends Option<? extends Tuple2<K2, V2>>> mapper) {
        Objects.requireNonNull(keyComparator, "keyComparator is null");
        return collect(this, EntryComparator.of(keyComparator), mapper);
    }

    @Override
    public <K2 extends @Nullable Object, V2 extends @Nullable Object> TreeMap<K2, V2> map(BiFunction<? super K, ? super V, Tuple2<K2, V2>> mapper) {
        return map(this, EntryComparator.natural(), mapper);
    }

    @Override
    public <K2 extends @Nullable Object, V2 extends @Nullable Object> TreeMap<K2, V2> map(Comparator<? super K2> keyComparator,
                                        BiFunction<? super K, ? super V, Tuple2<K2, V2>> mapper) {
        Objects.requireNonNull(keyComparator, "keyComparator is null");
        return map(this, EntryComparator.of(keyComparator), mapper);
    }

    @Override
    public <K2 extends @Nullable Object> TreeMap<K2, V> mapKeys(Function<? super K, ? extends K2> keyMapper) {
        Objects.requireNonNull(keyMapper, "keyMapper is null");
        return map((k, v) -> Tuple.of(keyMapper.apply(k), v));
    }

    @Override
    public <K2 extends @Nullable Object> TreeMap<K2, V> mapKeys(Function<? super K, ? extends K2> keyMapper, BiFunction<? super V, ? super V, ? extends V> valueMerge) {
        final Comparator<K2> comparator = Comparators.naturalComparator();
        return Collections.mapKeys(this, TreeMap.<K2, V> empty(comparator), keyMapper, valueMerge);
    }

    @Override
    public <W extends @Nullable Object> TreeMap<K, W> mapValues(Function<? super V, ? extends W> valueMapper) {
        Objects.requireNonNull(valueMapper, "valueMapper is null");
        return map(comparator(), (k, v) -> Tuple.of(k, valueMapper.apply(v)));
    }

    @Override
    public TreeMap<K, V> merge(Map<? extends K, ? extends V> that) {
        return Maps.merge(this, this::createFromEntries, that);
    }

    @Override
    public <U extends V> TreeMap<K, V> merge(Map<? extends K, U> that,
                                             BiFunction<? super V, ? super U, ? extends V> collisionResolution) {
        return Maps.merge(this, this::createFromEntries, that, collisionResolution);
    }

    /**
     * Returns this {@code TreeMap} if it is nonempty,
     * otherwise {@code TreeMap} created from iterable, using existing comparator.
     *
     * @param other An alternative {@code Traversable}
     * @return this {@code TreeMap} if it is nonempty,
     * otherwise {@code TreeMap} created from iterable, using existing comparator.
     */
    @Override
    public TreeMap<K, V> orElse(Iterable<? extends Tuple2<K, V>> other) {
        return isEmpty() ? ofEntries(comparator(), other) : this;
    }

    /**
     * Returns this {@code TreeMap} if it is nonempty,
     * otherwise {@code TreeMap} created from result of evaluating supplier, using existing comparator.
     *
     * @param supplier A supplier of alternative entries, evaluated only if this map is empty
     * @return this {@code TreeMap} if it is nonempty,
     * otherwise {@code TreeMap} created from result of evaluating supplier, using existing comparator.
     */
    @Override
    public TreeMap<K, V> orElse(Supplier<? extends Iterable<? extends Tuple2<K, V>>> supplier) {
        Objects.requireNonNull(supplier, "supplier is null");
        return isEmpty() ? ofEntries(comparator(), Objects.requireNonNull(supplier.get(), "TreeMap.orElse: supplier returned null")) : this;
    }

    @Override
    public Tuple2<TreeMap<K, V>, TreeMap<K, V>> partition(Predicate<? super Tuple2<K, V>> predicate) {
        return Maps.partition(this, this::createFromEntries, predicate);
    }

    @Override
    public TreeMap<K, V> tap(Consumer<? super Tuple2<K, V>> action) {
        return Maps.tap(this, action);
    }

    @Override
    public <U extends V> TreeMap<K, V> put(K key, U value, BiFunction<? super V, ? super U, ? extends V> merge) {
        return Maps.put(this, key, value, merge);
    }

    @Override
    public TreeMap<K, V> put(K key, V value) {
        Objects.requireNonNull(key, "TreeMap: key is null");
        Objects.requireNonNull(value, "TreeMap: value is null");
        return new TreeMap<>(entries.insert(new Tuple2<>(key, value)));
    }

    @Override
    public TreeMap<K, V> put(Tuple2<? extends K, ? extends V> entry) {
        return Maps.put(this, entry);
    }

    @Override
    public <U extends V> TreeMap<K, V> put(Tuple2<? extends K, U> entry,
                                           BiFunction<? super V, ? super U, ? extends V> merge) {
        return Maps.put(this, entry, merge);
    }

    @Override
    public TreeMap<K, V> remove(K key) {
        final Tuple2<K, V> entry = lookupEntry(key);
        if (entries.contains(entry)) {
            return new TreeMap<>(entries.delete(entry));
        } else {
            return this;
        }
    }

    @Override
    @Deprecated
    public TreeMap<K, V> removeAll(BiPredicate<? super K, ? super V> predicate) {
        Objects.requireNonNull(predicate, "predicate is null");
        return reject(predicate);
    }

    @Override
    public TreeMap<K, V> removeAll(Iterable<? extends K> keys) {
        RedBlackTree<Tuple2<K, V>> removed = entries;
        for (K key : keys) {
            final Tuple2<K, V> entry = lookupEntry(key);
            if (removed.contains(entry)) {
                removed = removed.delete(entry);
            }
        }
        if (removed.size() == entries.size()) {
            return this;
        } else {
            return new TreeMap<>(removed);
        }
    }

    @Override
    @Deprecated
    public TreeMap<K, V> removeKeys(Predicate<? super K> predicate) {
        Objects.requireNonNull(predicate, "predicate is null");
        return rejectKeys(predicate);
    }

    @Override
    @Deprecated
    public TreeMap<K, V> removeValues(Predicate<? super V> predicate) {
        Objects.requireNonNull(predicate, "predicate is null");
        return rejectValues(predicate);
    }

    @Override
    public TreeMap<K, V> replace(Tuple2<K, V> currentElement, Tuple2<K, V> newElement) {
        return Maps.replace(this, currentElement, newElement);
    }

    @Override
    public TreeMap<K, V> replaceAll(Tuple2<K, V> currentElement, Tuple2<K, V> newElement) {
        return Maps.replaceAll(this, currentElement, newElement);
    }

    @Override
    public TreeMap<K, V> replaceValue(K key, V value) {
        return Maps.replaceValue(this, key, value);
    }

    @Override
    public TreeMap<K, V> replace(K key, V oldValue, V newValue) {
        return Maps.replace(this, key, oldValue, newValue);
    }

    @Override
    public TreeMap<K, V> replaceAll(BiFunction<? super K, ? super V, ? extends V> function) {
        Objects.requireNonNull(function, "function is null");
        return map(comparator(), (k, v) -> Tuple.of(k, function.apply(k, v)));
    }

    @Override
    public TreeMap<K, V> retainAll(Iterable<? extends Tuple2<K, V>> elements) {
        Objects.requireNonNull(elements, "elements is null");
        RedBlackTree<Tuple2<K, V>> tree = RedBlackTree.empty(entries.comparator());
        for (Tuple2<K, V> entry : elements) {
            if (contains(entry)) {
                tree = tree.insert(entry);
            }
        }
        return new TreeMap<>(tree);
    }

    @Override
    public int size() {
        return entries.size();
    }

    /**
     * {@inheritDoc}
     * <p>
     * Complexity: O(n).
     */
    @Override
    public Vector<V> values() {
        return Vector.ofAll(Iterator.ofAll(this).map(Tuple2::_2));
    }

    // -- Object

    // -- Positional operations, in the comparator's order

    @Override
    public Tuple2<K, V> head() {
        if (entries.isEmpty()) {
            throw new java.util.NoSuchElementException("head of empty TreeMap");
        }
        return RedBlackTreeModule.Node.minimum((RedBlackTreeModule.Node<Tuple2<K, V>>) entries);
    }

    @Override
    public Option<Tuple2<K, V>> headOption() {
        return entries.min();
    }

    @Override
    public Tuple2<K, V> last() {
        if (entries.isEmpty()) {
            throw new java.util.NoSuchElementException("last of empty TreeMap");
        }
        return RedBlackTreeModule.Node.maximum((RedBlackTreeModule.Node<Tuple2<K, V>>) entries);
    }

    @Override
    public Option<Tuple2<K, V>> lastOption() {
        return entries.max();
    }

    @Override
    public TreeMap<K, V> init() {
        if (entries.isEmpty()) {
            throw new UnsupportedOperationException("init of empty TreeMap");
        }
        return new TreeMap<>(RedBlackTreeModule.Node.take(entries, entries.size() - 1));
    }

    @Override
    public Option<TreeMap<K, V>> initOption() {
        return entries.isEmpty() ? Option.none() : Option.some(init());
    }

    @Override
    public TreeMap<K, V> tail() {
        if (entries.isEmpty()) {
            throw new UnsupportedOperationException("tail of empty TreeMap");
        }
        return new TreeMap<>(RedBlackTreeModule.Node.drop(entries, 1));
    }

    @Override
    public Option<TreeMap<K, V>> tailOption() {
        return entries.isEmpty() ? Option.none() : Option.some(tail());
    }

    @Override
    public TreeMap<K, V> take(int n) {
        return slice(0, n);
    }

    @Override
    public TreeMap<K, V> takeRight(int n) {
        // n <= 0 keeps nothing; the subtraction cannot overflow because size() >= 0
        return n <= 0 ? slice(0, 0) : slice(entries.size() - n, entries.size());
    }

    @Override
    public TreeMap<K, V> takeWhile(Predicate<? super Tuple2<K, V>> predicate) {
        Objects.requireNonNull(predicate, "predicate is null");
        return slice(0, RedBlackTreeModule.Node.prefixLength(entries, predicate, true));
    }

    @Override
    public TreeMap<K, V> takeUntil(Predicate<? super Tuple2<K, V>> predicate) {
        Objects.requireNonNull(predicate, "predicate is null");
        return slice(0, RedBlackTreeModule.Node.prefixLength(entries, predicate, false));
    }

    @Override
    public TreeMap<K, V> drop(int n) {
        return slice(n, entries.size());
    }

    @Override
    public TreeMap<K, V> dropRight(int n) {
        return n <= 0 ? this : slice(0, entries.size() - n);
    }

    @Override
    public TreeMap<K, V> dropWhile(Predicate<? super Tuple2<K, V>> predicate) {
        Objects.requireNonNull(predicate, "predicate is null");
        return slice(RedBlackTreeModule.Node.prefixLength(entries, predicate, true), entries.size());
    }

    @Override
    public TreeMap<K, V> dropUntil(Predicate<? super Tuple2<K, V>> predicate) {
        Objects.requireNonNull(predicate, "predicate is null");
        return slice(RedBlackTreeModule.Node.prefixLength(entries, predicate, false), entries.size());
    }

    @Override
    public Vector<Tuple2<Tuple2<K, V>, Integer>> zipWithIndex() {
        return RedBlackTreeModule.Node.zipWithIndex(entries);
    }

    @Override
    public Vector<TreeMap<K, V>> grouped(int size) {
        return sliding(size, size);
    }

    @Override
    public Vector<TreeMap<K, V>> sliding(int size) {
        return sliding(size, 1);
    }

    @Override
    public Vector<TreeMap<K, V>> sliding(int size, int step) {
        return RedBlackTreeModule.Node.sliding(entries, size, step, TreeMap::new);
    }

    @Override
    public Vector<TreeMap<K, V>> slideBy(Function<? super Tuple2<K, V>, ?> classifier) {
        return RedBlackTreeModule.Node.slideBy(entries, classifier, TreeMap::new);
    }

    // the elements of rank from (inclusive) to until (exclusive), clamped; this map when nothing is cut off
    private TreeMap<K, V> slice(int from, int until) {
        final RedBlackTree<Tuple2<K, V>> sliced = RedBlackTreeModule.Node.slice(entries, from, until);
        return sliced == entries ? this : new TreeMap<>(sliced);
    }

    @Override
    public boolean equals(@Nullable Object o) {
        return Collections.equals(this, o);
    }

    @Override
    public int hashCode() {
        return Collections.hashUnordered(this);
    }

    @Override
    public String toString() {
        return mkString("TreeMap(", ", ", ")");
    }

    // -- private helpers

    private static <K extends @Nullable Object, K2 extends @Nullable Object, V extends @Nullable Object, V2 extends @Nullable Object> TreeMap<K2, V2> mapBoth(TreeMap<K, V> map, EntryComparator<K2, V2> entryComparator,
            Function<? super K, ? extends K2> keyMapper, Function<? super V, ? extends V2> valueMapper) {
        Objects.requireNonNull(keyMapper, "keyMapper is null");
        Objects.requireNonNull(valueMapper, "valueMapper is null");
        return createTreeMap(entryComparator, map.entries, entry -> entry.map(keyMapper, valueMapper));
    }

    private static <K extends @Nullable Object, V extends @Nullable Object, K2 extends @Nullable Object, V2 extends @Nullable Object> TreeMap<K2, V2> flatMap(TreeMap<K, V> map, EntryComparator<K2, V2> entryComparator,
            BiFunction<? super K, ? super V, ? extends Iterable<Tuple2<K2, V2>>> mapper) {
        Objects.requireNonNull(mapper, "mapper is null");
        return createTreeMap(entryComparator, map.entries.iterator().flatMap(entry -> Objects.requireNonNull(mapper.apply(entry._1(), entry._2()), "TreeMap.flatMap: mapper returned null")));
    }
    private static <K extends @Nullable Object, K2 extends @Nullable Object, V extends @Nullable Object, V2 extends @Nullable Object> TreeMap<K2, V2> collect(TreeMap<K, V> map, EntryComparator<K2, V2> entryComparator,
            BiFunction<? super K, ? super V, ? extends Option<? extends Tuple2<K2, V2>>> mapper) {
        Objects.requireNonNull(mapper, "mapper is null");
        // Iterator.collect drops the None entries in one pass; the null check runs here so that the message names this type
        return createTreeMap(entryComparator, map.entries.iterator().collect(entry -> Objects.requireNonNull(mapper.apply(entry._1(), entry._2()), "TreeMap.collect: mapper returned null")));
    }


    private static <K extends @Nullable Object, K2 extends @Nullable Object, V extends @Nullable Object, V2 extends @Nullable Object> TreeMap<K2, V2> map(TreeMap<K, V> map, EntryComparator<K2, V2> entryComparator,
            BiFunction<? super K, ? super V, Tuple2<K2, V2>> mapper) {
        Objects.requireNonNull(mapper, "mapper is null");
        return createTreeMap(entryComparator, map.entries, entry -> Objects.requireNonNull(mapper.apply(entry._1(), entry._2()), "TreeMap.map: mapper returned null"));
    }

    // -- internal factory methods

    private static <K extends @Nullable Object, V extends @Nullable Object> Collector<Tuple2<K, V>, Builder<K, V>, TreeMap<K, V>> createCollector(EntryComparator<K, V> entryComparator) {
        final Supplier<Builder<K, V>> supplier = () -> new Builder<>(entryComparator);
        final BiConsumer<Builder<K, V>, Tuple2<K, V>> accumulator = Builder::put;
        final BinaryOperator<Builder<K, V>> combiner = (left, right) -> left.putAll(right.result());
        final Function<Builder<K, V>, TreeMap<K, V>> finisher = Builder::result;
        return Collector.of(supplier, accumulator, combiner, finisher);
    }

    private static <K extends @Nullable Object, V extends @Nullable Object, T extends @Nullable Object> Collector<T, Builder<K, V>, TreeMap<K, V>> createCollector(
      EntryComparator<K, V> entryComparator,
      Function<? super T, ? extends K> keyMapper, Function<? super T, ? extends V> valueMapper) {
        final Supplier<Builder<K, V>> supplier = () -> new Builder<>(entryComparator);
        final BiConsumer<Builder<K, V>, T> accumulator = (builder, t) -> builder.put(keyMapper.apply(t), valueMapper.apply(t));
        final BinaryOperator<Builder<K, V>> combiner = (left, right) -> left.putAll(right.result());
        final Function<Builder<K, V>, TreeMap<K, V>> finisher = Builder::result;
        return Collector.of(supplier, accumulator, combiner, finisher);
    }

    private static <K extends @Nullable Object, V extends @Nullable Object> TreeMap<K, V> createTreeMap(EntryComparator<K, V> entryComparator,
                                                      Iterable<? extends Tuple2<? extends K, ? extends V>> entries) {
        Objects.requireNonNull(entries, "entries is null");
        final Builder<K, V> builder = new Builder<>(entryComparator);
        for (Tuple2<? extends K, ? extends V> entry : entries) {
            builder.put(Objects.requireNonNull(entry, "TreeMap.ofEntries: entry is null"));
        }
        return builder.result();
    }

    private static <K extends @Nullable Object, K2 extends @Nullable Object, V extends @Nullable Object, V2 extends @Nullable Object> TreeMap<K2, V2> createTreeMap(EntryComparator<K2, V2> entryComparator,
                                                                Iterable<Tuple2<K, V>> entries, Function<Tuple2<K, V>, Tuple2<K2, V2>> entryMapper) {
        final Builder<K2, V2> builder = new Builder<>(entryComparator);
        for (Tuple2<K, V> entry : entries) {
            builder.put(Objects.requireNonNull(entryMapper.apply(entry), "TreeMap.map: entry is null"));
        }
        return builder.result();
    }

    @SuppressWarnings("unchecked")
    private static <K extends @Nullable Object, V extends @Nullable Object> TreeMap<K, V> createFromMap(EntryComparator<K, V> entryComparator, java.util.Map<? extends K, ? extends V> map) {
        Objects.requireNonNull(map, "map is null");
        if (JavaConverters.underlying(map) instanceof TreeMap<?, ?> underlying && underlying.comparator() == entryComparator.keyComparator()) {
            return (TreeMap<K, V>) underlying;
        }
        final Builder<K, V> builder = new Builder<>(entryComparator);
        for (java.util.Map.Entry<K, V> entry : ((java.util.Map<K, V>) map).entrySet()) {
            builder.put(entry.getKey(), entry.getValue());
        }
        return builder.result();
    }

    @SuppressWarnings("unchecked")
    private static <K extends @Nullable Object, V extends @Nullable Object> TreeMap<K, V> createFromTuple(EntryComparator<K, V> entryComparator, Tuple2<? extends K, ? extends V> entry) {
        Objects.requireNonNull(entry, "entry is null");
        requireKey(entry._1());
        requireValue(entry._2());
        return new TreeMap<>(RedBlackTree.of(entryComparator, (Tuple2<K, V>) entry));
    }

    private static <K extends @Nullable Object> K requireKey(K key) {
        return Objects.requireNonNull(key, "TreeMap: key is null");
    }

    private static <V extends @Nullable Object> V requireValue(V value) {
        return Objects.requireNonNull(value, "TreeMap: value is null");
    }

    @SuppressWarnings("unchecked")
    private static <K extends @Nullable Object, V extends @Nullable Object> TreeMap<K, V> createFromTuples(EntryComparator<K, V> entryComparator, Tuple2<? extends K, ? extends V> ... entries) {
        Objects.requireNonNull(entries, "entries is null");
        final Builder<K, V> builder = new Builder<>(entryComparator);
        for (Tuple2<? extends K, ? extends V> entry : entries) {
            builder.put(Objects.requireNonNull(entry, "entries: entry is null"));
        }
        return builder.result();
    }

    @SafeVarargs
    private static <K extends @Nullable Object, V extends @Nullable Object> TreeMap<K, V> createFromMapEntries(EntryComparator<K, V> entryComparator, java.util.Map.Entry<? extends K, ? extends V> ... entries) {
        Objects.requireNonNull(entries, "entries is null");
        final Builder<K, V> builder = new Builder<>(entryComparator);
        for (java.util.Map.Entry<? extends K, ? extends V> entry : entries) {
            builder.put(entry.getKey(), entry.getValue());
        }
        return builder.result();
    }

    @SuppressWarnings("unchecked")
    private static <K extends @Nullable Object, V extends @Nullable Object> TreeMap<K, V> createFromPairs(EntryComparator<K, V> entryComparator, Object ... pairs) {
        RedBlackTree<Tuple2<K, V>> tree = RedBlackTree.empty(entryComparator);
        for (int i = 0; i < pairs.length; i += 2) {
            final K key = requireKey((K) pairs[i]);
            final V value = requireValue((V) pairs[i + 1]);
            tree = tree.insert(Tuple.of(key, value));
        }
        return new TreeMap<>(tree);
    }

    private TreeMap<K, V> createFromEntries(Iterable<Tuple2<K, V>> tuples) {
        return createTreeMap((EntryComparator<K, V>) entries.comparator(), tuples);
    }

    private TreeMap<K, V> emptyInstance() {
        return isEmpty() ? this : new TreeMap<>(entries.emptyInstance());
    }

    /**
     * An unmodifiable {@link java.util.NavigableMap} view of this TreeMap, in the order of its keys: nothing is copied,
     * reads go through to this map, which never changes, and every mutator of the view (including those of its key
     * sets, values, entry set, iterators and sub-views, and {@code setValue} on its entries) throws
     * {@link UnsupportedOperationException}, {@code pollFirstEntry} and {@code pollLastEntry} included.
     * {@code subMap}, {@code headMap}, {@code tailMap}, {@code descendingMap} and the key sets are views too, with the
     * bounds rules of {@link java.util.TreeMap}; {@code comparator()} is {@code null} when this map uses the natural
     * order of its keys. The view equals any {@code java.util.Map} with the same mappings. A mutable copy is
     * {@code new java.util.TreeMap<>(map.asJavaMap())}; {@code TreeMap.ofAll} given the view and this map's comparator
     * returns this map without copying.
     * <p>
     * Complexity: O(1); {@code get}, {@code containsKey}, {@code size}, {@code firstKey}, {@code lastKey} and the
     * {@code ceiling}, {@code floor}, {@code higher} and {@code lower} lookups on the view and on its sub-views are
     * O(log n), an iterator is O(log n) to create and amortized O(1) per step.
     *
     * @return an unmodifiable {@code java.util.NavigableMap} view
     */
    @Override
    public java.util.NavigableMap<K, V> asJavaMap() {
        return TreeViews.asJavaMap(this, entries, comparator());
    }

    @Override
    public Comparator<K> comparator() {
        return ((EntryComparator<K, V>) entries.comparator()).keyComparator();
    }

    /**
     * A mutable, single-use accumulator that builds a {@link TreeMap}. Entries are appended to an array;
     * {@link #result()} sorts it by key (a stable sort, with O(n) comparisons when the entries were added in key
     * order), keeps the last added of the entries whose keys the comparator finds equal, key and value, as successive
     * insertions would, and builds a balanced red-black tree bottom-up: one array plus exactly one node per distinct
     * key, where successive insertions allocate O(log n) nodes per entry and rebalance.
     * <p>
     * Not thread-safe. After {@link #result()} has been called, or after the comparator has thrown, every method
     * throws {@link IllegalStateException}; create a new builder instead. The comparator is first called by
     * {@link #size()} or {@link #result()}, not by {@code put}.
     *
     * @param <K> The key type
     * @param <V> The value type
     */
    public static final class Builder<K extends @Nullable Object, V extends @Nullable Object> {

        private final RedBlackTreeBuilder<Tuple2<K, V>> entries;

        private Builder(EntryComparator<K, V> entryComparator) {
            this.entries = new RedBlackTreeBuilder<>(entryComparator, "TreeMap.Builder");
        }

        /**
         * Adds one entry. Of entries whose keys the comparator finds equal, the one added last is kept.
         *
         * @param key   the key, never null
         * @param value the value, never null
         * @return this builder
         * @throws IllegalStateException if {@link #result()} has already been called
         * @throws NullPointerException if {@code key} or {@code value} is null
         */
        public Builder<K, V> put(K key, V value) {
            entries.checkOpen();
            entries.add(Tuple.of(requireKey(key), requireValue(value)));
            return this;
        }

        /**
         * Adds one entry. Of entries whose keys the comparator finds equal, the one added last is kept.
         *
         * @param entry the entry, whose key and value are never null
         * @return this builder
         * @throws IllegalStateException if {@link #result()} has already been called
         * @throws NullPointerException if {@code entry}, its key or its value is null
         */
        @SuppressWarnings("unchecked")
        public Builder<K, V> put(Tuple2<? extends K, ? extends V> entry) {
            entries.checkOpen();
            Objects.requireNonNull(entry, "TreeMap.Builder.put: entry is null");
            requireKey(entry._1());
            requireValue(entry._2());
            // a Tuple2 is immutable: it is stored as it is
            entries.add((Tuple2<K, V>) entry);
            return this;
        }

        /**
         * Adds all entries of the given iterable, in iteration order. A null entry, key or value part-way through is
         * rejected only when reached: the builder keeps the entries added before it.
         *
         * @param entries the entries to add
         * @return this builder
         * @throws IllegalStateException if {@link #result()} has already been called
         * @throws NullPointerException if {@code entries} is null, or if it yields a null entry, key or value
         */
        public Builder<K, V> putAll(Iterable<? extends Tuple2<? extends K, ? extends V>> entries) {
            this.entries.checkOpen();
            Objects.requireNonNull(entries, "entries is null");
            for (Tuple2<? extends K, ? extends V> entry : entries) {
                put(entry);
            }
            return this;
        }

        /**
         * Returns the number of distinct keys added so far. It sorts the entries buffered since the last call, so it
         * costs O(n log n) comparisons, O(n) when the entries were added in key order.
         *
         * @return the size the TreeMap would have
         * @throws IllegalStateException if {@link #result()} has already been called
         */
        public int size() {
            return entries.size();
        }

        /**
         * Builds the TreeMap. The builder cannot be used afterwards.
         *
         * @return a TreeMap of the entries added, ordered by the key comparator of this builder
         * @throws IllegalStateException if {@link #result()} has already been called
         */
        public TreeMap<K, V> result() {
            return new TreeMap<>(entries.result());
        }
    }

    // -- internal types

    private interface EntryComparator<K extends @Nullable Object, V extends @Nullable Object> extends Comparator<Tuple2<K, V>> {

        static <K extends @Nullable Object, V extends @Nullable Object> EntryComparator<K, V> of(Comparator<? super K> keyComparator) {
            Objects.requireNonNull(keyComparator, "keyComparator is null");
            return new Specific<>(keyComparator);
        }

        static <K extends @Nullable Object, V extends @Nullable Object> EntryComparator<K, V> natural() {
            return Natural.instance();
        }

        Comparator<K> keyComparator();

        // -- internal impls

        final class Specific<K extends @Nullable Object, V extends @Nullable Object> implements EntryComparator<K, V> {

            private final Comparator<K> keyComparator;

            @SuppressWarnings("unchecked")
            Specific(Comparator<? super K> keyComparator) {
                this.keyComparator = (Comparator<K>) keyComparator;
            }

            @Override
            public int compare(Tuple2<K, V> e1, Tuple2<K, V> e2) {
                return keyComparator.compare(e1._1(), e2._1());
            }

            @Override
            public Comparator<K> keyComparator() {
                return keyComparator;
            }
        }

        final class Natural<K extends @Nullable Object, V extends @Nullable Object> implements EntryComparator<K, V> {

            private static final Natural<?, ?> INSTANCE = new Natural<>();

            // hidden
            private Natural() {
            }

            @SuppressWarnings("unchecked")
            public static <K extends @Nullable Object, V extends @Nullable Object> Natural<K, V> instance() {
                return (Natural<K, V>) INSTANCE;
            }

            @SuppressWarnings("unchecked")
            @Override
            public int compare(Tuple2<K, V> e1, Tuple2<K, V> e2) {
                final K key1 = e1._1();
                final K key2 = e2._1();
                return ((Comparable<K>) key1).compareTo(key2);
            }

            @Override
            public Comparator<K> keyComparator() {
                return Comparators.naturalComparator();
            }

            @Override
            public boolean equals(@Nullable Object obj) {
                return obj instanceof Natural;
            }

            @Override
            public int hashCode() {
                return 1;
            }

        }
    }
}
