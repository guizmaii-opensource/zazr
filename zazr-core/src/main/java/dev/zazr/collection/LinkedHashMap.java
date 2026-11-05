package dev.zazr.collection;

import dev.zazr.Tuple;
import dev.zazr.Tuple2;
import dev.zazr.collection.internal.AbstractIterator;
import dev.zazr.collection.internal.Collections;
import dev.zazr.collection.internal.InsertionOrder;
import dev.zazr.collection.internal.Iterator;
import dev.zazr.collection.internal.JavaConverters;
import dev.zazr.collection.internal.MapViews;
import dev.zazr.collection.internal.Maps;
import dev.zazr.collection.internal.VectorBuilder;
import dev.zazr.control.Option;
import java.util.ArrayList;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.BinaryOperator;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collector;
import org.jspecify.annotations.Nullable;

/**
 * An immutable {@code LinkedHashMap} implementation that has predictable (insertion-order) iteration.
 * <p>
 * The insertion order is a trie of the keys, indexed by the ordinal each key got when it was first put, beside a
 * {@link HashMap} from each key to its entry and ordinal. A removed key leaves no gap: the trie holds only the keys
 * present, and counts them, so the key at a given position is found by one walk from its root.
 * <p>
 * A key given more than once keeps the position of its first occurrence and takes the key object and the value of
 * its last, whichever way the map is built: {@link #put(Object, Object)} on a key already present, the
 * {@link Builder}, and every factory, collector and bulk operation ({@code of}, {@code ofEntries}, {@code ofAll},
 * {@code collector()}, {@code tabulate}, {@code fill}, {@code mapBoth}, {@code mapKeys}, {@code map},
 * {@link #putAll(Iterable)}, and {@code merge(that)} on an empty map), which gives the map that putting the entries
 * one by one gives. For example,
 * {@code ofEntries((1, a), (2, b), (1, c))} iterates as {@code (1, c), (2, b)}. The operations that combine the values
 * of a repeated key, {@code merge(that, f)} and {@code mapKeys(keyMapper, valueMerge)}, keep its first position too
 * and take its last key object, with the combined value. On a map that is not empty, {@code merge(that)} keeps the
 * key object and the value of a key already present and adds only the keys it does not hold.
 * <p>
 * Complexity: lookups, insertions and removals are effectively O(1), on any version of a map, older ones included;
 * finding a position ({@code tail}, {@code take}, {@code drop}) is effectively O(1) too. The methods without a note of
 * their own (map, filter, the folds, the conversions) walk the entries once, O(n).
 *
 * @param <K> Key type
 * @param <V> Value type
 * @author Ruslan Sennov, Grzegorz Piwowarek
 */
public final class LinkedHashMap<K extends @Nullable Object, V extends @Nullable Object> implements Map<K, V> {

    private static final LinkedHashMap<?, ?> EMPTY = new LinkedHashMap<>(InsertionOrder.empty(), HashMap.empty());

    private final InsertionOrder<K> order;

    private final HashMap<K, Slot<K, V>> map;

    private LinkedHashMap(InsertionOrder<K> order, HashMap<K, Slot<K, V>> map) {
        this.order = order;
        this.map = map;
    }

    /* the entry of a key and the ordinal of the key in the insertion order */
    private record Slot<K extends @Nullable Object, V extends @Nullable Object>(Tuple2<K, V> entry, int index) {}

    /// The empty map whose first key gets the ordinal `ordinal` in the insertion order: the tests start near
    /// [InsertionOrder#LIMIT] to reach the renumbering of the keys.
    static <K extends @Nullable Object, V extends @Nullable Object> LinkedHashMap<K, V> emptyStartingAt(int ordinal) {
        return new LinkedHashMap<>(InsertionOrder.startingAt(ordinal), HashMap.empty());
    }

    /**
     * Returns a {@link java.util.stream.Collector} which may be used in conjunction with
     * {@link java.util.stream.Stream#collect(java.util.stream.Collector)} to obtain a {@link LinkedHashMap}.
     *
     * @param <K> The key type
     * @param <V> The value type
     * @return A {@link LinkedHashMap} Collector.
     */
    public static <K extends @Nullable Object, V extends @Nullable Object>
            Collector<Tuple2<K, V>, Builder<K, V>, LinkedHashMap<K, V>> collector() {
        Supplier<Builder<K, V>> supplier = LinkedHashMap::newBuilder;
        BiConsumer<Builder<K, V>, Tuple2<K, V>> accumulator = Builder::put;
        BinaryOperator<Builder<K, V>> combiner = (left, right) -> left.putAll(right.result());
        Function<Builder<K, V>, LinkedHashMap<K, V>> finisher = Builder::result;
        return Collector.of(supplier, accumulator, combiner, finisher);
    }

    /**
     * Returns a {@link java.util.stream.Collector} which may be used in conjunction with
     * {@link java.util.stream.Stream#collect(java.util.stream.Collector)} to obtain a {@link LinkedHashMap}.
     *
     * @param keyMapper The key mapper
     * @param <K> The key type
     * @param <V> The value type
     * @param <T> Initial {@link java.util.stream.Stream} elements type
     * @return A {@link LinkedHashMap} Collector.
     */
    public static <K extends @Nullable Object, V extends @Nullable Object, T extends V>
            Collector<T, Builder<K, V>, LinkedHashMap<K, V>> collector(Function<? super T, ? extends K> keyMapper) {
        Objects.requireNonNull(keyMapper, "keyMapper is null");
        return LinkedHashMap.collector(keyMapper, v -> v);
    }

    /**
     * Returns a {@link java.util.stream.Collector} which may be used in conjunction with
     * {@link java.util.stream.Stream#collect(java.util.stream.Collector)} to obtain a {@link LinkedHashMap}.
     *
     * @param keyMapper The key mapper
     * @param valueMapper The value mapper
     * @param <K> The key type
     * @param <V> The value type
     * @param <T> Initial {@link java.util.stream.Stream} elements type
     * @return A {@link LinkedHashMap} Collector.
     */
    public static <K extends @Nullable Object, V extends @Nullable Object, T extends @Nullable Object>
            Collector<T, Builder<K, V>, LinkedHashMap<K, V>> collector(
                    Function<? super T, ? extends K> keyMapper, Function<? super T, ? extends V> valueMapper) {
        Objects.requireNonNull(keyMapper, "keyMapper is null");
        Objects.requireNonNull(valueMapper, "valueMapper is null");
        Supplier<Builder<K, V>> supplier = LinkedHashMap::newBuilder;
        BiConsumer<Builder<K, V>, T> accumulator =
                (builder, t) -> builder.put(keyMapper.apply(t), valueMapper.apply(t));
        BinaryOperator<Builder<K, V>> combiner = (left, right) -> left.putAll(right.result());
        Function<Builder<K, V>, LinkedHashMap<K, V>> finisher = Builder::result;
        return Collector.of(supplier, accumulator, combiner, finisher);
    }

    /**
     * Returns a new {@link Builder}: the cheapest way to build a LinkedHashMap from many entries. The builder keeps one
     * list of keys and one hash map, where successive puts make a new map, and a new insertion order, at each step.
     *
     * @param <K> The key type
     * @param <V> The value type
     * @return an empty builder
     */
    public static <K extends @Nullable Object, V extends @Nullable Object> Builder<K, V> newBuilder() {
        return new Builder<>(16);
    }

    @SuppressWarnings("unchecked")
    public static <K extends @Nullable Object, V extends @Nullable Object> LinkedHashMap<K, V> empty() {
        return (LinkedHashMap<K, V>) EMPTY;
    }

    /**
     * Narrows a {@code LinkedHashMap<? extends K, ? extends V>} to {@code LinkedHashMap<K, V>} via a
     * type-safe cast. Safe here because the map is immutable and no elements
     * can be added that would violate the type (covariance)
     *
     * @param linkedHashMap the map to narrow
     * @param <K>           the target key type
     * @param <V>           the target value type
     * @return the same map viewed as {@code LinkedHashMap<K, V>}
     */
    @SuppressWarnings("unchecked")
    public static <K extends @Nullable Object, V extends @Nullable Object> LinkedHashMap<K, V> narrow(
            LinkedHashMap<? extends K, ? extends V> linkedHashMap) {
        return (LinkedHashMap<K, V>) linkedHashMap;
    }

    /**
     * Returns a singleton {@code LinkedHashMap}, i.e. a {@code LinkedHashMap} of one element.
     *
     * @param entry A map entry.
     * @param <K>   The key type
     * @param <V>   The value type
     * @return A new Map containing the given entry
     */
    @SuppressWarnings("unchecked")
    public static <K extends @Nullable Object, V extends @Nullable Object> LinkedHashMap<K, V> of(
            Tuple2<? extends K, ? extends V> entry) {
        Objects.requireNonNull(entry, "entry is null");
        Builder<K, V> builder = new Builder<>(1);
        builder.putChecked((Tuple2<K, V>) entry);
        return builder.result();
    }

    /**
     * Returns a {@code LinkedHashMap}, from a source java.util.Map.
     *
     * @param map A map
     * @param <K> The key type
     * @param <V> The value type
     * @return A new Map containing the given map; the {@link #asJavaMap()} view of a LinkedHashMap (not its
     *         {@code reversed()} view) gives that LinkedHashMap back, not a copy
     */
    @SuppressWarnings("unchecked")
    public static <K extends @Nullable Object, V extends @Nullable Object> LinkedHashMap<K, V> ofAll(
            java.util.Map<? extends K, ? extends V> map) {
        Objects.requireNonNull(map, "map is null");
        if (JavaConverters.underlying(map) instanceof LinkedHashMap<?, ?> underlying) {
            return (LinkedHashMap<K, V>) underlying;
        }
        Builder<K, V> builder = new Builder<>(map.size());
        for (java.util.Map.Entry<? extends K, ? extends V> entry : map.entrySet()) {
            builder.putChecked(entry.getKey(), entry.getValue());
        }
        return builder.result();
    }

    /**
     * Returns a {@code LinkedHashMap}, from entries mapped from stream.
     *
     * @param stream      the source stream
     * @param entryMapper the entry mapper
     * @param <T>         The stream element type
     * @param <K>         The key type
     * @param <V>         The value type
     * @return A new Map
     */
    public static <T extends @Nullable Object, K extends @Nullable Object, V extends @Nullable Object>
            LinkedHashMap<K, V> ofAll(
                    java.util.stream.Stream<? extends T> stream,
                    Function<? super T, Tuple2<? extends K, ? extends V>> entryMapper) {
        return Maps.ofStream(empty(), stream, entryMapper, "LinkedHashMap.ofAll: entryMapper returned null");
    }

    /**
     * Returns a {@code LinkedHashMap}, from entries mapped from stream.
     *
     * @param stream      the source stream
     * @param keyMapper   the key mapper
     * @param valueMapper the value mapper
     * @param <T>         The stream element type
     * @param <K>         The key type
     * @param <V>         The value type
     * @return A new Map
     */
    public static <T extends @Nullable Object, K extends @Nullable Object, V extends @Nullable Object>
            LinkedHashMap<K, V> ofAll(
                    java.util.stream.Stream<? extends T> stream,
                    Function<? super T, ? extends K> keyMapper,
                    Function<? super T, ? extends V> valueMapper) {
        return Maps.ofStream(empty(), stream, keyMapper, valueMapper);
    }

    /**
     * Returns a singleton {@code LinkedHashMap}, i.e. a {@code LinkedHashMap} of one element.
     *
     * @param key   A singleton map key.
     * @param value A singleton map value.
     * @param <K>   The key type
     * @param <V>   The value type
     * @return A new Map containing the given entry
     */
    public static <K extends @Nullable Object, V extends @Nullable Object> LinkedHashMap<K, V> of(K key, V value) {
        Builder<K, V> builder = new Builder<>(1);
        builder.putChecked(key, value);
        return builder.result();
    }

    /**
     * Creates a LinkedHashMap of the given list of key-value pairs.
     *
     * @param k1  a key for the map
     * @param v1  the value for k1
     * @param k2  a key for the map
     * @param v2  the value for k2
     * @param <K> The key type
     * @param <V> The value type
     * @return A new Map containing the given entries
     */
    public static <K extends @Nullable Object, V extends @Nullable Object> LinkedHashMap<K, V> of(
            K k1, V v1, K k2, V v2) {
        Builder<K, V> builder = new Builder<>(2);
        builder.putChecked(k1, v1);
        builder.putChecked(k2, v2);
        return builder.result();
    }

    /**
     * Creates a LinkedHashMap of the given list of key-value pairs.
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
    public static <K extends @Nullable Object, V extends @Nullable Object> LinkedHashMap<K, V> of(
            K k1, V v1, K k2, V v2, K k3, V v3) {
        Builder<K, V> builder = new Builder<>(3);
        builder.putChecked(k1, v1);
        builder.putChecked(k2, v2);
        builder.putChecked(k3, v3);
        return builder.result();
    }

    /**
     * Creates a LinkedHashMap of the given list of key-value pairs.
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
    public static <K extends @Nullable Object, V extends @Nullable Object> LinkedHashMap<K, V> of(
            K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4) {
        Builder<K, V> builder = new Builder<>(4);
        builder.putChecked(k1, v1);
        builder.putChecked(k2, v2);
        builder.putChecked(k3, v3);
        builder.putChecked(k4, v4);
        return builder.result();
    }

    /**
     * Creates a LinkedHashMap of the given list of key-value pairs.
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
    public static <K extends @Nullable Object, V extends @Nullable Object> LinkedHashMap<K, V> of(
            K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4, K k5, V v5) {
        Builder<K, V> builder = new Builder<>(5);
        builder.putChecked(k1, v1);
        builder.putChecked(k2, v2);
        builder.putChecked(k3, v3);
        builder.putChecked(k4, v4);
        builder.putChecked(k5, v5);
        return builder.result();
    }

    /**
     * Creates a LinkedHashMap of the given list of key-value pairs.
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
    public static <K extends @Nullable Object, V extends @Nullable Object> LinkedHashMap<K, V> of(
            K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4, K k5, V v5, K k6, V v6) {
        Builder<K, V> builder = new Builder<>(6);
        builder.putChecked(k1, v1);
        builder.putChecked(k2, v2);
        builder.putChecked(k3, v3);
        builder.putChecked(k4, v4);
        builder.putChecked(k5, v5);
        builder.putChecked(k6, v6);
        return builder.result();
    }

    /**
     * Creates a LinkedHashMap of the given list of key-value pairs.
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
    public static <K extends @Nullable Object, V extends @Nullable Object> LinkedHashMap<K, V> of(
            K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4, K k5, V v5, K k6, V v6, K k7, V v7) {
        Builder<K, V> builder = new Builder<>(7);
        builder.putChecked(k1, v1);
        builder.putChecked(k2, v2);
        builder.putChecked(k3, v3);
        builder.putChecked(k4, v4);
        builder.putChecked(k5, v5);
        builder.putChecked(k6, v6);
        builder.putChecked(k7, v7);
        return builder.result();
    }

    /**
     * Creates a LinkedHashMap of the given list of key-value pairs.
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
    public static <K extends @Nullable Object, V extends @Nullable Object> LinkedHashMap<K, V> of(
            K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4, K k5, V v5, K k6, V v6, K k7, V v7, K k8, V v8) {
        Builder<K, V> builder = new Builder<>(8);
        builder.putChecked(k1, v1);
        builder.putChecked(k2, v2);
        builder.putChecked(k3, v3);
        builder.putChecked(k4, v4);
        builder.putChecked(k5, v5);
        builder.putChecked(k6, v6);
        builder.putChecked(k7, v7);
        builder.putChecked(k8, v8);
        return builder.result();
    }

    /**
     * Creates a LinkedHashMap of the given list of key-value pairs.
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
    public static <K extends @Nullable Object, V extends @Nullable Object> LinkedHashMap<K, V> of(
            K k1,
            V v1,
            K k2,
            V v2,
            K k3,
            V v3,
            K k4,
            V v4,
            K k5,
            V v5,
            K k6,
            V v6,
            K k7,
            V v7,
            K k8,
            V v8,
            K k9,
            V v9) {
        Builder<K, V> builder = new Builder<>(9);
        builder.putChecked(k1, v1);
        builder.putChecked(k2, v2);
        builder.putChecked(k3, v3);
        builder.putChecked(k4, v4);
        builder.putChecked(k5, v5);
        builder.putChecked(k6, v6);
        builder.putChecked(k7, v7);
        builder.putChecked(k8, v8);
        builder.putChecked(k9, v9);
        return builder.result();
    }

    /**
     * Creates a LinkedHashMap of the given list of key-value pairs.
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
    public static <K extends @Nullable Object, V extends @Nullable Object> LinkedHashMap<K, V> of(
            K k1,
            V v1,
            K k2,
            V v2,
            K k3,
            V v3,
            K k4,
            V v4,
            K k5,
            V v5,
            K k6,
            V v6,
            K k7,
            V v7,
            K k8,
            V v8,
            K k9,
            V v9,
            K k10,
            V v10) {
        Builder<K, V> builder = new Builder<>(10);
        builder.putChecked(k1, v1);
        builder.putChecked(k2, v2);
        builder.putChecked(k3, v3);
        builder.putChecked(k4, v4);
        builder.putChecked(k5, v5);
        builder.putChecked(k6, v6);
        builder.putChecked(k7, v7);
        builder.putChecked(k8, v8);
        builder.putChecked(k9, v9);
        builder.putChecked(k10, v10);
        return builder.result();
    }

    /**
     * Returns a LinkedHashMap containing up to {@code n} values of a given Function {@code f}
     * over a range of integer values from 0 to {@code n - 1}.
     *
     * @param <K> The key type
     * @param <V> The value type
     * @param n   The number of times to call {@code f}
     * @param f   The Function computing element values
     * @return A LinkedHashMap containing the entries {@code f(0), f(1), ..., f(n - 1)}; entries with equal keys collapse
     *         (the later value wins, keeping the earlier position), so the result may contain fewer than {@code n} entries.
     *         Empty if {@code n <= 0}.
     * @throws NullPointerException if {@code f} is null or returns null
     */
    @SuppressWarnings("unchecked")
    public static <K extends @Nullable Object, V extends @Nullable Object> LinkedHashMap<K, V> tabulate(
            int n, Function<? super Integer, ? extends Tuple2<? extends K, ? extends V>> f) {
        Objects.requireNonNull(f, "f is null");
        return ofEntries(Collections.tabulate(
                n, i -> Objects.requireNonNull(f.apply(i), "LinkedHashMap.tabulate: f returned null")));
    }

    /**
     * Returns a LinkedHashMap containing tuples returned by {@code n} calls to a given Supplier {@code s}.
     *
     * @param <K> The key type
     * @param <V> The value type
     * @param n   The number of times to call {@code s}
     * @param s   The Supplier computing element values
     * @return A LinkedHashMap containing the entries supplied by {@code s}; entries with equal keys collapse
     *         (the later value wins, keeping the earlier position), so the result may contain fewer than {@code n} entries.
     *         Empty if {@code n <= 0}.
     * @throws NullPointerException if {@code s} is null or returns null
     */
    @SuppressWarnings("unchecked")
    public static <K extends @Nullable Object, V extends @Nullable Object> LinkedHashMap<K, V> fill(
            int n, Supplier<? extends Tuple2<? extends K, ? extends V>> s) {
        Objects.requireNonNull(s, "s is null");
        return ofEntries(
                Collections.fill(n, () -> Objects.requireNonNull(s.get(), "LinkedHashMap.fill: s returned null")));
    }

    /**
     * Creates a LinkedHashMap of the given entries.
     *
     * @param entries Map entries
     * @param <K>     The key type
     * @param <V>     The value type
     * @return A new Map containing the given entries
     */
    @SuppressWarnings("unchecked")
    public static <K extends @Nullable Object, V extends @Nullable Object> LinkedHashMap<K, V> ofEntries(
            java.util.Map.Entry<? extends K, ? extends V>... entries) {
        Objects.requireNonNull(entries, "entries is null");
        Builder<K, V> builder = new Builder<>(entries.length);
        for (java.util.Map.Entry<? extends K, ? extends V> entry : entries) {
            Objects.requireNonNull(entry, "LinkedHashMap.ofEntries: entry is null");
            builder.putChecked(entry.getKey(), entry.getValue());
        }
        return builder.result();
    }

    /**
     * Creates a LinkedHashMap of the given entries.
     *
     * @param entries Map entries
     * @param <K>     The key type
     * @param <V>     The value type
     * @return A new Map containing the given entries
     */
    @SuppressWarnings("unchecked")
    public static <K extends @Nullable Object, V extends @Nullable Object> LinkedHashMap<K, V> ofEntries(
            Tuple2<? extends K, ? extends V>... entries) {
        Objects.requireNonNull(entries, "entries is null");
        Builder<K, V> builder = new Builder<>(entries.length);
        for (Tuple2<? extends K, ? extends V> entry : entries) {
            Objects.requireNonNull(entry, "LinkedHashMap.ofEntries: entry is null");
            builder.putChecked((Tuple2<K, V>) entry);
        }
        return builder.result();
    }

    /**
     * Creates a LinkedHashMap of the given entries.
     *
     * @param entries Map entries
     * @param <K>     The key type
     * @param <V>     The value type
     * @return A LinkedHashMap containing the given entries (the same instance if {@code entries} is already a
     *         LinkedHashMap, or the {@link #asJava()} view of one)
     */
    @SuppressWarnings("unchecked")
    public static <K extends @Nullable Object, V extends @Nullable Object> LinkedHashMap<K, V> ofEntries(
            Iterable<? extends Tuple2<? extends K, ? extends V>> entries) {
        Objects.requireNonNull(entries, "entries is null");
        if (entries instanceof LinkedHashMap) {
            return (LinkedHashMap<K, V>) entries;
        } else if (JavaConverters.underlying(entries) instanceof LinkedHashMap<?, ?> underlying) {
            return (LinkedHashMap<K, V>) underlying;
        } else {
            Builder<K, V> builder =
                    new Builder<>(entries instanceof java.util.Collection<?> collection ? collection.size() : 10);
            for (Tuple2<? extends K, ? extends V> entry : entries) {
                Objects.requireNonNull(entry, "LinkedHashMap.ofEntries: entry is null");
                builder.putChecked((Tuple2<K, V>) entry);
            }
            return builder.result();
        }
    }

    @Override
    public <K2 extends @Nullable Object, V2 extends @Nullable Object> LinkedHashMap<K2, V2> mapBoth(
            Function<? super K, ? extends K2> keyMapper, Function<? super V, ? extends V2> valueMapper) {
        Objects.requireNonNull(keyMapper, "keyMapper is null");
        Objects.requireNonNull(valueMapper, "valueMapper is null");
        Iterator<Tuple2<K2, V2>> entries =
                Iterator.ofAll(this).map(entry -> Tuple.of(keyMapper.apply(entry._1()), valueMapper.apply(entry._2())));
        return LinkedHashMap.ofEntries(entries);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Complexity: effectively O(1): one lookup, and one {@link #put(Object, Object)} when the key is absent.
     */
    @Override
    public Tuple2<V, LinkedHashMap<K, V>> computeIfAbsent(K key, Function<? super K, ? extends V> mappingFunction) {
        return Maps.computeIfAbsent(this, key, mappingFunction);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Complexity: effectively O(1): one lookup, and one {@link #put(Object, Object)} when the key is present.
     */
    @Override
    public Tuple2<Option<V>, LinkedHashMap<K, V>> computeIfPresent(
            K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
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
        return map.containsKey(key);
    }

    @Override
    public LinkedHashMap<K, V> filter(BiPredicate<? super K, ? super V> predicate) {
        return Maps.filter(this, this::createFromEntries, predicate);
    }

    @Override
    public LinkedHashMap<K, V> reject(BiPredicate<? super K, ? super V> predicate) {
        return Maps.reject(this, this::createFromEntries, predicate);
    }

    @Override
    public LinkedHashMap<K, V> filter(Predicate<? super Tuple2<K, V>> predicate) {
        return Maps.filter(this, this::createFromEntries, predicate);
    }

    @Override
    public LinkedHashMap<K, V> reject(Predicate<? super Tuple2<K, V>> predicate) {
        return Maps.reject(this, this::createFromEntries, predicate);
    }

    @Override
    public LinkedHashMap<K, V> filterKeys(Predicate<? super K> predicate) {
        return Maps.filterKeys(this, this::createFromEntries, predicate);
    }

    @Override
    public LinkedHashMap<K, V> rejectKeys(Predicate<? super K> predicate) {
        return Maps.rejectKeys(this, this::createFromEntries, predicate);
    }

    @Override
    public LinkedHashMap<K, V> filterValues(Predicate<? super V> predicate) {
        return Maps.filterValues(this, this::createFromEntries, predicate);
    }

    @Override
    public LinkedHashMap<K, V> rejectValues(Predicate<? super V> predicate) {
        return Maps.rejectValues(this, this::createFromEntries, predicate);
    }

    @Override
    @SuppressWarnings("Var")
    public <K2 extends @Nullable Object, V2 extends @Nullable Object> LinkedHashMap<K2, V2> flatMap(
            BiFunction<? super K, ? super V, ? extends Iterable<Tuple2<K2, V2>>> mapper) {
        Objects.requireNonNull(mapper, "mapper is null");
        return foldLeft(LinkedHashMap.<K2, V2>empty(), (acc, entry) -> {
            for (Tuple2<? extends K2, ? extends V2> mappedEntry : Objects.requireNonNull(
                    mapper.apply(entry._1(), entry._2()), "LinkedHashMap.flatMap: mapper returned null")) {
                acc = acc.put(mappedEntry);
            }
            return acc;
        });
    }

    /**
     * Option-free slot lookup used on the hot paths; absence is signalled by {@code null}
     * so that reads and writes do not allocate an {@link Option}.
     */
    @SuppressWarnings("NullAway")
    private @Nullable Slot<K, V> slotOrNull(K key) {
        return map.getOrElse(key, (Slot<K, V>) null);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Complexity: effectively O(1): one hash lookup.
     */
    @Override
    public Option<V> get(K key) {
        Slot<K, V> slot = slotOrNull(key);
        return slot == null ? Option.none() : Option.some(slot.entry()._2());
    }

    /**
     * {@inheritDoc}
     * <p>
     * Complexity: effectively O(1): one hash lookup.
     */
    @Override
    public V getOrElse(K key, V defaultValue) {
        Slot<K, V> slot = slotOrNull(key);
        return slot == null ? defaultValue : slot.entry()._2();
    }

    /**
     * {@inheritDoc}
     * <p>
     * Complexity: effectively O(1): one hash lookup.
     */
    @Override
    public V getOrElse(K key, Supplier<? extends V> supplier) {
        return Maps.getOrElse(this, key, supplier, "LinkedHashMap.getOrElse: supplier returned null");
    }

    @Override
    public <C extends @Nullable Object> Map<C, LinkedHashMap<K, V>> groupBy(
            Function<? super Tuple2<K, V>, ? extends C> classifier) {
        return Maps.groupBy(
                this, this::createFromEntries, classifier, "LinkedHashMap.groupBy: classifier returned null");
    }

    @Override
    public <K2 extends @Nullable Object, U extends @Nullable Object> Map<K2, Vector<U>> groupMap(
            Function<? super Tuple2<K, V>, ? extends K2> key, Function<? super Tuple2<K, V>, ? extends U> value) {
        return Collections.groupMap(this, key, value, Vector::ofAll, "LinkedHashMap.groupMap");
    }

    @Override
    public <K2 extends @Nullable Object, U extends @Nullable Object> Map<K2, U> groupMapReduce(
            Function<? super Tuple2<K, V>, ? extends K2> key,
            Function<? super Tuple2<K, V>, ? extends U> value,
            BiFunction<? super U, ? super U, ? extends U> reduce) {
        return Collections.groupMapReduceToMap(this, key, value, reduce, "LinkedHashMap.groupMapReduce");
    }

    /**
     * {@inheritDoc}
     * <p>
     * Complexity: O(1).
     */
    @Override
    public boolean isEmpty() {
        return map.isEmpty();
    }

    /**
     * {@inheritDoc}
     * <p>
     * Complexity: effectively O(1) to create; a whole walk is O(n), one hash lookup per entry.
     */
    @Override
    public java.util.Iterator<Tuple2<K, V>> iterator() {
        java.util.Iterator<K> keys = order.iterator();
        return new AbstractIterator<Tuple2<K, V>>() {
            @Override
            public boolean hasNext() {
                return keys.hasNext();
            }

            @Override
            protected Tuple2<K, V> getNext() {
                return entryAt(keys.next());
            }
        };
    }

    /**
     * An unmodifiable {@link java.util.SequencedMap} view of this LinkedHashMap, in insertion order: nothing is
     * copied, reads go through to this map, which never changes, and every mutator of the view (including those of
     * its key set, values, entry set and their iterators, and {@code setValue} on its entries) throws
     * {@link UnsupportedOperationException}. {@code reversed()} is a view in reverse insertion order. The view equals
     * any {@code java.util.Map} with the same mappings. A mutable copy is
     * {@code new java.util.LinkedHashMap<>(map.asJavaMap())}; {@code LinkedHashMap.ofAll} given the view returns this
     * map without copying.
     * <p>
     * Complexity: O(1): nothing is copied. {@code get} and {@code containsKey} on the view are effectively O(1); its
     * iterators, in either order, walk the whole map in O(n).
     *
     * @return an unmodifiable {@code java.util.SequencedMap} view
     */
    @Override
    public java.util.SequencedMap<K, V> asJavaMap() {
        return MapViews.asJavaMap(this, this::reverseIterator);
    }

    /**
     * The entries in reverse insertion order, which the reversed views of {@code asJavaMap()} and of
     * {@link LinkedHashSet#asJava()} walk.
     * <p>
     * Complexity: effectively O(1) to create; a whole walk is O(n), one hash lookup per entry.
     *
     * @return a new iterator
     */
    java.util.Iterator<Tuple2<K, V>> reverseIterator() {
        java.util.Iterator<K> keys = order.reverseIterator();
        return new AbstractIterator<Tuple2<K, V>>() {
            @Override
            public boolean hasNext() {
                return keys.hasNext();
            }

            @Override
            protected Tuple2<K, V> getNext() {
                return entryAt(keys.next());
            }
        };
    }

    /**
     * {@inheritDoc}
     * <p>
     * Complexity: O(1): a LinkedHashSet view that shares this map; nothing is copied.
     */
    @SuppressWarnings("unchecked")
    @Override
    public Set<K> keySet() {
        return LinkedHashSet.wrap((LinkedHashMap<K, Object>) this);
    }

    @Override
    public <K2 extends @Nullable Object, V2 extends @Nullable Object> LinkedHashMap<K2, V2> collect(
            BiFunction<? super K, ? super V, ? extends Option<? extends Tuple2<K2, V2>>> mapper) {
        Objects.requireNonNull(mapper, "mapper is null");
        return foldLeft(LinkedHashMap.empty(), (acc, entry) -> {
            Option<? extends Tuple2<K2, V2>> collected = Objects.requireNonNull(
                    mapper.apply(entry._1(), entry._2()), "LinkedHashMap.collect: mapper returned null");
            return collected.isDefined() ? acc.put(collected.get()) : acc;
        });
    }

    @Override
    public <K2 extends @Nullable Object, V2 extends @Nullable Object> LinkedHashMap<K2, V2> map(
            BiFunction<? super K, ? super V, Tuple2<K2, V2>> mapper) {
        Objects.requireNonNull(mapper, "mapper is null");
        return foldLeft(
                LinkedHashMap.empty(),
                (acc, entry) -> acc.put(Objects.requireNonNull(
                        mapper.apply(entry._1(), entry._2()), "LinkedHashMap.map: mapper returned null")));
    }

    @Override
    public <K2 extends @Nullable Object> LinkedHashMap<K2, V> mapKeys(Function<? super K, ? extends K2> keyMapper) {
        Objects.requireNonNull(keyMapper, "keyMapper is null");
        return map((k, v) -> Tuple.of(keyMapper.apply(k), v));
    }

    @Override
    public <K2 extends @Nullable Object> LinkedHashMap<K2, V> mapKeys(
            Function<? super K, ? extends K2> keyMapper, BiFunction<? super V, ? super V, ? extends V> valueMerge) {
        return Collections.mapKeys(this, LinkedHashMap.empty(), keyMapper, valueMerge);
    }

    @Override
    public <W extends @Nullable Object> LinkedHashMap<K, W> mapValues(Function<? super V, ? extends W> mapper) {
        Objects.requireNonNull(mapper, "mapper is null");
        return transformed((k, v) -> mapper.apply(v));
    }

    // The same keys in the same order with the values `f` computes, called once per entry in iteration order: each
    // value that is not the object already there is put, so this map comes back when none changes.
    @SuppressWarnings({"Var", "unchecked"})
    private <W extends @Nullable Object> LinkedHashMap<K, W> transformed(
            BiFunction<? super K, ? super V, ? extends W> f) {
        LinkedHashMap<K, W> result = (LinkedHashMap<K, W>) (LinkedHashMap<K, ?>) this;
        for (Tuple2<K, V> entry : this) {
            W value = f.apply(entry._1(), entry._2());
            if (value != entry._2()) {
                result = result.put(entry._1(), value);
            }
        }
        return result;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Complexity: O(m) for a map of m entries, one lookup and at most one {@link #put(Object, Object)} each; O(1)
     * when that map is empty, or when this map is empty and that map is a LinkedHashMap, which is returned as is.
     */
    @Override
    public LinkedHashMap<K, V> merge(Map<? extends K, ? extends V> that) {
        return Maps.merge(this, this::createFromEntries, that);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Complexity: O(m) for a map of m entries, one lookup and one {@link #put(Object, Object)} each; O(1) when that
     * map is empty, or when this map is empty and that map is a LinkedHashMap, which is returned as is.
     */
    @Override
    public <U extends V> LinkedHashMap<K, V> merge(
            Map<? extends K, U> that, BiFunction<? super V, ? super U, ? extends V> collisionResolution) {
        return Maps.merge(this, this::createFromEntries, that, collisionResolution);
    }

    @Override
    public LinkedHashMap<K, V> orElse(Iterable<? extends Tuple2<K, V>> other) {
        return isEmpty() ? ofEntries(other) : this;
    }

    @Override
    public LinkedHashMap<K, V> orElse(Supplier<? extends Iterable<? extends Tuple2<K, V>>> supplier) {
        Objects.requireNonNull(supplier, "supplier is null");
        return isEmpty()
                ? ofEntries(Objects.requireNonNull(supplier.get(), "LinkedHashMap.orElse: supplier returned null"))
                : this;
    }

    @Override
    public Tuple2<LinkedHashMap<K, V>, LinkedHashMap<K, V>> partition(Predicate<? super Tuple2<K, V>> predicate) {
        return Maps.partition(this, this::createFromEntries, predicate);
    }

    @Override
    public LinkedHashMap<K, V> tap(Consumer<? super Tuple2<K, V>> action) {
        return Maps.tap(this, action);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Complexity: effectively O(1): one lookup and one {@link #put(Object, Object)}.
     */
    @Override
    public <U extends V> LinkedHashMap<K, V> put(K key, U value, BiFunction<? super V, ? super U, ? extends V> merge) {
        return Maps.put(this, key, value, merge);
    }

    /**
     * Associates the specified value with the specified key in this map.
     * If the map previously contained a mapping for the key, both the key and
     * the value are replaced by the specified ones, and the key keeps its
     * position in the insertion order.
     * <p>
     * Complexity: effectively O(1): one hash lookup and one insertion. An existing key keeps its position; a new key
     * is added at the end of the insertion order.
     *
     * @param key   key with which the specified value is to be associated
     * @param value value to be associated with the specified key
     * @return A new Map containing these elements and that entry.
     */
    @Override
    public LinkedHashMap<K, V> put(K key, V value) {
        Objects.requireNonNull(key, "LinkedHashMap: key is null");
        Objects.requireNonNull(value, "LinkedHashMap: value is null");
        Slot<K, V> existing = slotOrNull(key);
        if (existing != null && existing.entry()._1() == key && existing.entry()._2() == value) {
            // the key and the value are the objects already there
            return this;
        }
        return putEntry(Tuple.of(key, value), existing);
    }

    // put(entry._1(), entry._2()) storing `entry` itself, whose key and value are not null
    private LinkedHashMap<K, V> putEntry(Tuple2<K, V> entry) {
        Slot<K, V> existing = slotOrNull(entry._1());
        if (existing != null
                && existing.entry()._1() == entry._1()
                && existing.entry()._2() == entry._2()) {
            // the key and the value are the objects already there
            return this;
        }
        return putEntry(entry, existing);
    }

    // putEntry(entry) once the slot of its key is known: `existing`, or null when the key is absent
    private LinkedHashMap<K, V> putEntry(Tuple2<K, V> entry, @Nullable Slot<K, V> existing) {
        K key = entry._1();
        if (existing != null) {
            // the insertion order holds the key object of the entry, which the positional operations read
            int ordinal = existing.index();
            InsertionOrder<K> newOrder = existing.entry()._1() == key ? order : order.update(ordinal, key);
            return new LinkedHashMap<>(newOrder, map.put(key, new Slot<>(entry, ordinal)));
        }
        return appendEntry(entry);
    }

    // this map with `entry`, whose key is absent, last in the insertion order
    private LinkedHashMap<K, V> appendEntry(Tuple2<K, V> entry) {
        if (order.isFull()) {
            // every ordinal has been given: the keys are numbered again from 0, in the same order
            return reindex(order, map).appendEntry(entry);
        }
        K key = entry._1();
        return new LinkedHashMap<>(order.append(key), map.put(key, new Slot<>(entry, order.nextOrdinal())));
    }

    /// This map with `key` mapped to `value` if `key` is absent, this map itself if it is present: the key and the
    /// value already there are kept, which is what [LinkedHashSet#add] needs for an element already in the set.
    ///
    /// Complexity: effectively O(1), that of [#put(Object, Object)].
    LinkedHashMap<K, V> putIfAbsent(K key, V value) {
        Objects.requireNonNull(key, "LinkedHashMap: key is null");
        Objects.requireNonNull(value, "LinkedHashMap: value is null");
        if (map.containsKey(key)) {
            return this;
        }
        return appendEntry(Tuple.of(key, value));
    }

    /**
     * {@inheritDoc}
     * <p>
     * Complexity: effectively O(1), as {@link #put(Object, Object)}.
     */
    @Override
    public LinkedHashMap<K, V> put(Tuple2<? extends K, ? extends V> entry) {
        return Maps.put(this, entry);
    }

    /**
     * {@inheritDoc}
     * <p>
     * A key already present keeps its position and takes the entry's key object and value; a new key goes to the end
     * of the insertion order.
     * <p>
     * Complexity: O(m) for m entries, one effectively O(1) {@link #put(Object, Object)} each. When this map is empty,
     * the entries are built into a new map as {@link #ofEntries(Iterable)} does, and a LinkedHashMap is returned as
     * is.
     */
    @Override
    public LinkedHashMap<K, V> putAll(Iterable<? extends Tuple2<? extends K, ? extends V>> entries) {
        Objects.requireNonNull(entries, "entries is null");
        if (isEmpty()) {
            return ofEntries(entries);
        }
        @SuppressWarnings("Var")
        LinkedHashMap<K, V> result = this;
        for (Tuple2<? extends K, ? extends V> entry : entries) {
            Objects.requireNonNull(entry, "entry is null");
            result = result.put(entry._1(), entry._2());
        }
        return result;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Complexity: effectively O(1): one lookup and one {@link #put(Object, Object)}.
     */
    @Override
    public <U extends V> LinkedHashMap<K, V> put(
            Tuple2<? extends K, U> entry, BiFunction<? super V, ? super U, ? extends V> merge) {
        return Maps.put(this, entry, merge);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Complexity: effectively O(1): one hash removal, and the key taken out of the insertion order on the path from its
     * root, on any version of the map.
     */
    @Override
    public LinkedHashMap<K, V> remove(K key) {
        Slot<K, V> existing = slotOrNull(key);
        if (existing == null) {
            return this;
        }
        HashMap<K, Slot<K, V>> newMap = map.remove(key);
        if (newMap.isEmpty()) {
            return empty();
        }
        return new LinkedHashMap<>(order.remove(existing.index()), newMap);
    }

    /**
     * {@inheritDoc}
     * <p>
     * A key already present keeps its position in the insertion order; a new key goes to the end.
     * <p>
     * Complexity: effectively O(1): one hash lookup, then at most one {@link #put(Object, Object)} or one
     * {@link #remove(Object)}.
     */
    @Override
    public LinkedHashMap<K, V> updateWith(K key, Function<? super Option<V>, ? extends Option<? extends V>> f) {
        return Maps.updateWith(this, key, f, "LinkedHashMap.updateWith: f returned null");
    }

    /**
     * {@inheritDoc}
     * <p>
     * Complexity: O(n + m) for m given keys: they are put in a hash set, every entry of this map is checked against it,
     * and the map is rebuilt without the removed ones.
     */
    @Override
    public LinkedHashMap<K, V> removeAll(Iterable<? extends K> keys) {
        Objects.requireNonNull(keys, "keys is null");
        HashSet<K> toRemove = HashSet.ofAll(keys);
        HashMap<K, Slot<K, V>> newMap = map.filter(t -> !toRemove.contains(t._1()));
        return newMap.size() == map.size() ? this : reindex(order, newMap);
    }

    /**
     * {@inheritDoc}
     * <p>
     * The new entry takes the position of the replaced one. This map is returned when the key and the value of
     * {@code newElement} are the objects already there; an equal key or value that is another object is stored.
     * <p>
     * Complexity: effectively O(1): the entry is replaced, and an entry of the key of {@code newElement} elsewhere in
     * the map is removed, as {@link #remove(Object)}.
     */
    @Override
    public LinkedHashMap<K, V> replace(Tuple2<K, V> currentElement, Tuple2<K, V> newElement) {
        Objects.requireNonNull(currentElement, "currentElement is null");
        Objects.requireNonNull(newElement, "newElement is null");
        Objects.requireNonNull(newElement._1(), "LinkedHashMap: key is null");
        Objects.requireNonNull(newElement._2(), "LinkedHashMap: value is null");

        // the whole entry is replaced, so its key and its value must both be there
        Slot<K, V> existing = slotOrNull(currentElement._1());
        if (existing == null || !Objects.equals(existing.entry()._2(), currentElement._2())) {
            return this;
        } else if (existing.entry()._1() == newElement._1() && existing.entry()._2() == newElement._2()) {
            // replaced by the key and the value objects already there
            return this;
        } else {
            return replaceKey(currentElement._1(), newElement);
        }
    }

    /// The key object this map holds for `key`, or `null` when `key` is absent: what [LinkedHashSet#replace] compares
    /// with the new element, since the values of a key set are not its elements.
    ///
    /// Complexity: effectively O(1): one hash lookup.
    @Nullable
    K keyOrNull(K key) {
        Slot<K, V> slot = slotOrNull(key);
        return slot == null ? null : slot.entry()._1();
    }

    /// This map with the entry of `currentKey`, which must be present, replaced by `newElement` at the same position
    /// in the iteration order; an entry of `newElement`'s key elsewhere in the map is removed. The value of
    /// `currentKey` plays no part, which is what [LinkedHashSet#replace] needs for a key set whose values are not
    /// its elements.
    @SuppressWarnings("Var")
    LinkedHashMap<K, V> replaceKey(K currentKey, Tuple2<K, V> newElement) {
        InsertionOrder<K> newOrder = order;
        HashMap<K, Slot<K, V>> newMap = map;

        K newKey = newElement._1();

        // If current key and new key are equal, the key keeps its position,
        // otherwise we need to remove an already present newKey from the order manually.
        if (!Objects.equals(currentKey, newKey) && newMap.containsKey(newKey)) {
            Slot<K, V> obsolete = newMap.get(newKey).get();
            newOrder = newOrder.remove(obsolete.index());
            newMap = newMap.remove(newKey);
        }

        Slot<K, V> currentSlot = newMap.get(currentKey).get();
        newOrder = newOrder.update(currentSlot.index(), newKey);
        newMap = newMap.remove(currentKey).put(newKey, new Slot<>(newElement, currentSlot.index()));

        return new LinkedHashMap<>(newOrder, newMap);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Complexity: effectively O(1), as {@link #replace(Tuple2, Tuple2)}: a map holds an entry once. The new entry takes
     * the position of the replaced one.
     */
    @Override
    public LinkedHashMap<K, V> replaceAll(Tuple2<K, V> currentElement, Tuple2<K, V> newElement) {
        return replace(currentElement, newElement);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Complexity: effectively O(1): one lookup, and one {@link #put(Object, Object)} when the key is present.
     */
    @Override
    public LinkedHashMap<K, V> replaceValue(K key, V value) {
        return Maps.replaceValue(this, key, value);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Complexity: effectively O(1): one lookup, and one {@link #put(Object, Object)} of the existing key when it maps
     * to {@code oldValue}.
     */
    @Override
    public LinkedHashMap<K, V> replace(K key, V oldValue, V newValue) {
        return Maps.replace(this, key, oldValue, newValue);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Complexity: O(n): every entry is mapped and put in a new map.
     */
    @Override
    public LinkedHashMap<K, V> replaceAll(BiFunction<? super K, ? super V, ? extends V> function) {
        Objects.requireNonNull(function, "function is null");
        return transformed(function);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Complexity: O(n + m) for m given entries: they are put in a hash set, then every entry of this map is checked
     * against it and the kept ones are put in a new map.
     */
    @Override
    public LinkedHashMap<K, V> retainAll(Iterable<? extends Tuple2<K, V>> elements) {
        return Collections.retainAll(this, elements, kept -> filter(kept));
    }

    /**
     * {@inheritDoc}
     * <p>
     * Complexity: O(1): the size is stored, not counted.
     */
    @Override
    public int size() {
        return map.size();
    }

    /**
     * {@inheritDoc}
     * <p>
     * Complexity: O(n): the values are copied into a new Vector.
     */
    @Override
    public Vector<V> values() {
        if (isEmpty()) {
            return Vector.empty();
        }
        VectorBuilder<V> values = VectorBuilder.forExactly(size());
        for (java.util.Iterator<K> keys = order.iterator(); keys.hasNext(); ) {
            values.addUnchecked(entryAt(keys.next())._2());
        }
        return Vector.ofAll(values.result());
    }

    // -- Positional operations, in insertion order

    /**
     * The first entry in insertion order.
     * <p>
     * Complexity: effectively O(1): the first key of the insertion order, found from its root, then one hash lookup.
     *
     * @return the entry inserted first among those present
     * @throws java.util.NoSuchElementException if this map is empty
     */
    public Tuple2<K, V> head() {
        if (isEmpty()) {
            throw new java.util.NoSuchElementException("head of empty LinkedHashMap");
        }
        return entryAt(order.first());
    }

    /**
     * The first entry in insertion order, if any.
     * <p>
     * Complexity: effectively O(1), as {@link #head()}.
     *
     * @return {@code Some} of {@link #head()}, or {@code None} if this map is empty
     */
    public Option<Tuple2<K, V>> headOption() {
        return isEmpty() ? Option.none() : Option.some(head());
    }

    /**
     * The last entry in insertion order.
     * <p>
     * Complexity: effectively O(1): the last key of the insertion order, found from its root, then one hash lookup.
     *
     * @return the entry inserted last among those present
     * @throws java.util.NoSuchElementException if this map is empty
     */
    public Tuple2<K, V> last() {
        if (isEmpty()) {
            throw new java.util.NoSuchElementException("last of empty LinkedHashMap");
        }
        return entryAt(order.last());
    }

    /**
     * The last entry in insertion order, if any.
     * <p>
     * Complexity: effectively O(1), as {@link #last()}.
     *
     * @return {@code Some} of {@link #last()}, or {@code None} if this map is empty
     */
    public Option<Tuple2<K, V>> lastOption() {
        return isEmpty() ? Option.none() : Option.some(last());
    }

    /**
     * All entries but the last in insertion order.
     * <p>
     * Complexity: effectively O(1): the insertion order is cut along one path from its root, and one key is taken out
     * of the hash map, on any version of the map.
     *
     * @return this map without its last entry
     * @throws UnsupportedOperationException if this map is empty
     */
    public LinkedHashMap<K, V> init() {
        if (isEmpty()) {
            throw new UnsupportedOperationException("init of empty LinkedHashMap");
        }
        return slice(0, size() - 1);
    }

    /**
     * All entries but the last in insertion order, if this map is not empty.
     * <p>
     * Complexity: effectively O(1), as {@link #init()}.
     *
     * @return {@code Some} of {@link #init()}, or {@code None} if this map is empty
     */
    public Option<LinkedHashMap<K, V>> initOption() {
        return isEmpty() ? Option.none() : Option.some(init());
    }

    /**
     * All entries but the first in insertion order.
     * <p>
     * Complexity: effectively O(1): the insertion order is cut along one path from its root, and one key is taken out
     * of the hash map, on any version of the map.
     *
     * @return this map without its first entry
     * @throws UnsupportedOperationException if this map is empty
     */
    public LinkedHashMap<K, V> tail() {
        if (isEmpty()) {
            throw new UnsupportedOperationException("tail of empty LinkedHashMap");
        }
        return slice(1, size());
    }

    /**
     * All entries but the first in insertion order, if this map is not empty.
     * <p>
     * Complexity: effectively O(1), as {@link #tail()}.
     *
     * @return {@code Some} of {@link #tail()}, or {@code None} if this map is empty
     */
    public Option<LinkedHashMap<K, V>> tailOption() {
        return isEmpty() ? Option.none() : Option.some(tail());
    }

    /**
     * The first {@code n} entries in insertion order: empty if {@code n <= 0}, this map if {@code n >= size()}.
     * <p>
     * Complexity: O(min(k, n - k)) for k kept entries: the smaller side, kept or dropped, is added to or taken out of a
     * hash map, and the rest is shared; the insertion order is cut along the paths from its root to the cut.
     *
     * @param n the number of entries to keep
     * @return the {@code n} entries inserted first
     */
    public LinkedHashMap<K, V> take(int n) {
        return slice(0, n);
    }

    /**
     * The last {@code n} entries in insertion order: empty if {@code n <= 0}, this map if {@code n >= size()}.
     * <p>
     * Complexity: O(min(k, n - k)) for k kept entries, as {@link #take(int)} from the other end.
     *
     * @param n the number of entries to keep
     * @return the {@code n} entries inserted last
     */
    public LinkedHashMap<K, V> takeRight(int n) {
        return n <= 0 ? empty() : slice(size() - n, size());
    }

    /**
     * The longest prefix, in insertion order, of entries satisfying {@code predicate}.
     * <p>
     * Complexity: O(k) for a prefix of k entries: one walk, then one {@link #take(int)}.
     *
     * @param predicate tested on the entries from the first inserted
     * @return the entries before the first one not satisfying {@code predicate}
     * @throws NullPointerException if {@code predicate} is null
     */
    public LinkedHashMap<K, V> takeWhile(Predicate<? super Tuple2<K, V>> predicate) {
        Objects.requireNonNull(predicate, "predicate is null");
        return slice(0, countLeading(predicate, true));
    }

    /**
     * The longest prefix, in insertion order, of entries not satisfying {@code predicate}.
     * <p>
     * Complexity: O(k) for a prefix of k entries: one walk, then one {@link #take(int)}.
     *
     * @param predicate tested on the entries from the first inserted
     * @return the entries before the first one satisfying {@code predicate}
     * @throws NullPointerException if {@code predicate} is null
     */
    public LinkedHashMap<K, V> takeUntil(Predicate<? super Tuple2<K, V>> predicate) {
        Objects.requireNonNull(predicate, "predicate is null");
        return slice(0, countLeading(predicate, false));
    }

    /**
     * All entries but the first {@code n} in insertion order: this map if {@code n <= 0}, empty if
     * {@code n >= size()}.
     * <p>
     * Complexity: O(min(k, n - k)) for k dropped entries, as {@link #take(int)}.
     *
     * @param n the number of entries to drop
     * @return the entries after the {@code n} inserted first
     */
    public LinkedHashMap<K, V> drop(int n) {
        return slice(n, size());
    }

    /**
     * All entries but the last {@code n} in insertion order: this map if {@code n <= 0}, empty if
     * {@code n >= size()}.
     * <p>
     * Complexity: O(min(k, n - k)) for k dropped entries, as {@link #take(int)}.
     *
     * @param n the number of entries to drop
     * @return the entries before the {@code n} inserted last
     */
    public LinkedHashMap<K, V> dropRight(int n) {
        return n <= 0 ? this : slice(0, size() - n);
    }

    /**
     * The entries from the first one, in insertion order, that does not satisfy {@code predicate}.
     * <p>
     * Complexity: O(k) for k dropped entries: one walk, then one {@link #drop(int)}.
     *
     * @param predicate tested on the entries from the first inserted
     * @return the entries from the first one not satisfying {@code predicate}
     * @throws NullPointerException if {@code predicate} is null
     */
    public LinkedHashMap<K, V> dropWhile(Predicate<? super Tuple2<K, V>> predicate) {
        Objects.requireNonNull(predicate, "predicate is null");
        return slice(countLeading(predicate, true), size());
    }

    /**
     * The entries from the first one, in insertion order, that satisfies {@code predicate}.
     * <p>
     * Complexity: O(k) for k dropped entries: one walk, then one {@link #drop(int)}.
     *
     * @param predicate tested on the entries from the first inserted
     * @return the entries from the first one satisfying {@code predicate}
     * @throws NullPointerException if {@code predicate} is null
     */
    public LinkedHashMap<K, V> dropUntil(Predicate<? super Tuple2<K, V>> predicate) {
        Objects.requireNonNull(predicate, "predicate is null");
        return slice(countLeading(predicate, false), size());
    }

    /**
     * The entries paired with their position in insertion order, from 0.
     * <p>
     * Complexity: O(n).
     *
     * @return the pairs (entry, position), in order
     */
    public Vector<Tuple2<Tuple2<K, V>, Integer>> zipWithIndex() {
        Vector.Builder<Tuple2<Tuple2<K, V>, Integer>> builder = Vector.newBuilder(size());
        @SuppressWarnings("Var")
        int index = 0;
        for (java.util.Iterator<K> keys = order.iterator(); keys.hasNext(); ) {
            builder.add(Tuple.of(entryAt(keys.next()), index++));
        }
        return builder.result();
    }

    /**
     * The blocks of {@code size} consecutive entries in insertion order; the last block is smaller when
     * {@code size} does not divide {@code size()}. The same as {@code sliding(size, size)}.
     * <p>
     * Complexity: O(n), as {@link #sliding(int, int)} with a step of {@code size}.
     *
     * @param size the block size, positive
     * @return the blocks, in order; empty if this map is empty
     * @throws IllegalArgumentException if {@code size} is not positive
     */
    public Vector<LinkedHashMap<K, V>> grouped(int size) {
        return sliding(size, size);
    }

    /**
     * The windows of {@code size} consecutive entries in insertion order, each starting one entry after the
     * previous. The same as {@code sliding(size, 1)}.
     * <p>
     * Complexity: O(n + n * min(size, n - size)), as {@link #sliding(int, int)} with a step of 1.
     *
     * @param size the window size, positive
     * @return the windows, in order; empty if this map is empty
     * @throws IllegalArgumentException if {@code size} is not positive
     */
    public Vector<LinkedHashMap<K, V>> sliding(int size) {
        return sliding(size, 1);
    }

    /**
     * The windows of {@code size} consecutive entries in insertion order, each starting {@code step} entries after
     * the previous. The window rule is {@link Vector}'s: the last window is shorter than {@code size} when it reaches
     * the end, a window whose entries all belong to the previous one is not produced, a map smaller than
     * {@code size} is one window and an empty map has none.
     * <p>
     * Complexity: O(n + (n / step) * min(size, n - size)): each window costs a {@link #take(int)}, O(min(size, n -
     * size)).
     *
     * @param size the window size, positive
     * @param step the distance between two window starts, positive
     * @return the windows, in order
     * @throws IllegalArgumentException if {@code size} or {@code step} is not positive
     */
    public Vector<LinkedHashMap<K, V>> sliding(int size, int step) {
        return windows(size, step, Function.identity());
    }

    /**
     * The maximal runs of consecutive entries, in insertion order, with the same key, computed once per entry by
     * {@code classifier}; the runs together are this map.
     * <p>
     * Complexity: O(n): one walk, and each run is cut in time proportional to its length.
     *
     * @param classifier the key of an entry; two consecutive entries are in the same run when their keys are equal
     * @return the runs, in order; empty if this map is empty
     * @throws NullPointerException if {@code classifier} is null
     */
    public Vector<LinkedHashMap<K, V>> slideBy(Function<? super Tuple2<K, V>, ?> classifier) {
        return runs(classifier, Function.identity());
    }

    // sliding(size, step), each window wrapped by `wrap`; LinkedHashSet passes its own wrapper
    <R extends @Nullable Object> Vector<R> windows(int size, int step, Function<LinkedHashMap<K, V>, R> wrap) {
        Collections.checkWindow(size, step);
        int length = size();
        if (length == 0) {
            return Vector.empty();
        }
        Vector.Builder<R> builder = Vector.newBuilder();
        // past the first, a window is produced only while it holds at least one entry the previous one did not
        for (long start = 0; start < length && (start == 0 || start - step + size < length); start += step) {
            builder.add(wrap.apply(slice((int) start, (int) Math.min(start + size, length))));
        }
        return builder.result();
    }

    // slideBy(classifier) over the entries, each run wrapped by `wrap`
    private <R extends @Nullable Object> Vector<R> runs(
            Function<? super Tuple2<K, V>, ?> classifier, Function<LinkedHashMap<K, V>, R> wrap) {
        Objects.requireNonNull(classifier, "classifier is null");
        return runs(classifier, false, wrap);
    }

    // slideBy(classifier) over the keys alone, each run wrapped by `wrap`: LinkedHashSet's, with no entry looked up
    <R extends @Nullable Object> Vector<R> runsByKey(
            Function<? super K, ?> classifier, Function<LinkedHashMap<K, V>, R> wrap) {
        Objects.requireNonNull(classifier, "classifier is null");
        return runs(classifier, true, wrap);
    }

    // the classifier takes a key when `byKey`, an entry otherwise
    @SuppressWarnings("unchecked")
    private <R extends @Nullable Object> Vector<R> runs(
            Function<?, ?> classifier, boolean byKey, Function<LinkedHashMap<K, V>, R> wrap) {
        if (isEmpty()) {
            return Vector.empty();
        }
        Function<Object, ?> classify = (Function<Object, ?>) classifier;
        Vector.Builder<R> builder = Vector.newBuilder();
        java.util.Iterator<K> keys = order.iterator();
        int length = size();
        K firstKey = keys.next();
        @SuppressWarnings("Var")
        Object key = classify.apply(byKey ? firstKey : entryAt(firstKey));
        @SuppressWarnings("Var")
        int start = 0;
        for (int index = 1; index < length; index++) {
            K nextKey = keys.next();
            Object next = classify.apply(byKey ? nextKey : entryAt(nextKey));
            if (!Objects.equals(key, next)) {
                builder.add(wrap.apply(slice(start, index)));
                start = index;
                key = next;
            }
        }
        builder.add(wrap.apply(slice(start, length)));
        return builder.result();
    }

    // the entry of a key present in this map
    private Tuple2<K, V> entryAt(K key) {
        return slotAt(key).entry();
    }

    // the slot of a key present in this map
    @SuppressWarnings("NullAway")
    private Slot<K, V> slotAt(K key) {
        return slotOrNull(key);
    }

    // the number of leading entries, in insertion order, for which predicate returns `expected`
    private int countLeading(Predicate<? super Tuple2<K, V>> predicate, boolean expected) {
        @SuppressWarnings("Var")
        int length = 0;
        for (java.util.Iterator<K> keys = order.iterator(); keys.hasNext(); length++) {
            if (predicate.test(entryAt(keys.next())) != expected) {
                break;
            }
        }
        return length;
    }

    // the number of leading keys, in insertion order, for which predicate returns `expected`: LinkedHashSet's
    // takeWhile and its siblings, with no entry looked up
    int countLeadingKeys(Predicate<? super K> predicate, boolean expected) {
        @SuppressWarnings("Var")
        int length = 0;
        for (java.util.Iterator<K> keys = order.iterator(); keys.hasNext(); length++) {
            if (predicate.test(keys.next()) != expected) {
                break;
            }
        }
        return length;
    }

    // the keys paired with their position in insertion order: LinkedHashSet's zipWithIndex, with no entry looked up
    Vector<Tuple2<K, Integer>> zipKeysWithIndex() {
        Vector.Builder<Tuple2<K, Integer>> builder = Vector.newBuilder(size());
        @SuppressWarnings("Var")
        int index = 0;
        for (java.util.Iterator<K> keys = order.iterator(); keys.hasNext(); ) {
            builder.add(Tuple.of(keys.next(), index++));
        }
        return builder.result();
    }

    /**
     * The entries of rank {@code from} (inclusive) to {@code until} (exclusive) in insertion order, clamped; this map
     * when nothing is cut off, the empty map when nothing is kept. The insertion order is cut at both ranks and the
     * slots keep their ordinals, so the hash map changes by the smaller of the kept and the removed keys: rebuilt from
     * the kept ones, or the removed ones taken out of it.
     * <p>
     * Complexity: O(min(k, n - k)) for k kept entries, as {@link #take(int)}.
     */
    LinkedHashMap<K, V> slice(int from, int until) {
        int size = size();
        int start = Math.max(from, 0);
        int end = Math.min(until, size);
        if (start >= end) {
            return empty();
        } else if (start == 0 && end == size) {
            return this;
        }
        int kept = end - start;
        if (kept == size - 1) {
            // one entry cut off an end (tail, init): taken out of the order by its ordinal, as remove does
            K key = start == 1 ? order.first() : order.last();
            return new LinkedHashMap<>(order.remove(slotAt(key).index()), map.remove(key));
        }
        @SuppressWarnings("Var")
        HashMap<K, Slot<K, V>> newMap;
        if (kept <= size - kept) {
            newMap = HashMap.empty();
            java.util.Iterator<K> keys = order.iterator(start);
            for (int i = 0; i < kept; i++) {
                K key = keys.next();
                newMap = newMap.put(key, slotAt(key));
            }
        } else {
            newMap = map;
            if (start > 0) {
                java.util.Iterator<K> before = order.iterator();
                for (int i = 0; i < start; i++) {
                    newMap = newMap.remove(before.next());
                }
            }
            if (end < size) {
                java.util.Iterator<K> after = order.iterator(end);
                for (int i = end; i < size; i++) {
                    newMap = newMap.remove(after.next());
                }
            }
        }
        return new LinkedHashMap<>(order.slice(start, end), newMap);
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
        return mkString("LinkedHashMap(", ", ", ")");
    }

    // the keys of `order` that `survivors` holds, in order, numbered again from 0, each with its entry in `survivors`
    private static <K extends @Nullable Object, V extends @Nullable Object> LinkedHashMap<K, V> reindex(
            InsertionOrder<K> order, HashMap<K, Slot<K, V>> survivors) {
        if (survivors.isEmpty()) {
            return empty();
        }
        ArrayList<K> liveKeys = new ArrayList<>(survivors.size());
        for (java.util.Iterator<K> keys = order.iterator(); keys.hasNext(); ) {
            K key = keys.next();
            if (survivors.containsKey(key)) {
                liveKeys.add(key);
            }
        }
        @SuppressWarnings("Var")
        HashMap<K, Slot<K, V>> indexed = HashMap.empty();
        @SuppressWarnings("Var")
        int index = 0;
        for (K key : liveKeys) {
            indexed = indexed.put(key, new Slot<>(survivors.get(key).get().entry(), index++));
        }
        return new LinkedHashMap<>(InsertionOrder.ofAll(liveKeys), indexed);
    }

    /**
     * A mutable, single-use accumulator that builds a {@link LinkedHashMap}. The intermediate maps are not built: the
     * keys go into an array list in insertion order, each key maps to its entry and position in a hash map, and the
     * insertion order is built from the list once, in {@link #result()}. A {@link LinkedHashMap} passed to
     * {@link #putAll(Iterable)} on an empty builder is adopted without copying anything, and the puts that follow are
     * those of the map, each making a new map: that map never changes.
     * <p>
     * The map returned by {@link #result()} is the one successive {@link LinkedHashMap#put(Object, Object)} calls of
     * the same entries would give: a key put more than once keeps the position of its first occurrence and takes the
     * key object and the value of its last. Not thread-safe. After {@link #result()} has been called, every method
     * throws {@link IllegalStateException}; create a new builder instead.
     *
     * @param <K> The key type
     * @param <V> The value type
     */
    public static final class Builder<K extends @Nullable Object, V extends @Nullable Object> {

        /* the name in the message of a closed builder: LinkedHashSet.Builder is one of these too */
        private final String name;
        /* the keys in insertion order: keys.get(i) has the ordinal i */
        private ArrayList<K> keys;
        /* each key to its entry and its ordinal */
        private HashMap<K, Slot<K, V>> slots = HashMap.empty();
        /* a map given to putAll on an empty builder: the content of the builder from then on, which puts replace */
        private @Nullable LinkedHashMap<K, V> adopted;
        private boolean done;

        Builder(String name, int expectedSize) {
            this.name = name;
            this.keys = new ArrayList<>(expectedSize);
        }

        private Builder(int expectedSize) {
            this("LinkedHashMap.Builder", expectedSize);
        }

        /**
         * Puts one entry. A key already put keeps its position and takes the given key object and value.
         *
         * @param key   the key, never null
         * @param value the value, never null
         * @return this builder
         * @throws IllegalStateException if {@link #result()} has already been called
         * @throws NullPointerException if {@code key} or {@code value} is null
         */
        public Builder<K, V> put(K key, V value) {
            checkOpen();
            Objects.requireNonNull(key, "LinkedHashMap.Builder.put: key is null");
            Objects.requireNonNull(value, "LinkedHashMap.Builder.put: value is null");
            putEntry(Tuple.of(key, value));
            return this;
        }

        /**
         * Puts one entry. A key already put keeps its position and takes the key object and the value of the entry.
         *
         * @param entry the entry, whose key and value are never null
         * @return this builder
         * @throws IllegalStateException if {@link #result()} has already been called
         * @throws NullPointerException if {@code entry}, its key or its value is null
         */
        @SuppressWarnings("unchecked")
        public Builder<K, V> put(Tuple2<? extends K, ? extends V> entry) {
            checkOpen();
            Objects.requireNonNull(entry, "LinkedHashMap.Builder.put: entry is null");
            Objects.requireNonNull(entry._1(), "LinkedHashMap.Builder.put: key is null");
            Objects.requireNonNull(entry._2(), "LinkedHashMap.Builder.put: value is null");
            putEntry((Tuple2<K, V>) entry);
            return this;
        }

        /**
         * Puts all entries of the given iterable, in iteration order. A {@link LinkedHashMap} (or the
         * {@link LinkedHashMap#asJava()} view of one) given to an empty builder is adopted without copying anything:
         * {@link #result()} returns it as it is if nothing else is put, and it is copied only when a later put follows,
         * so that map never changes. Otherwise the entries are put one by one, and a null entry, key or value part-way
         * through is rejected only when reached: the builder keeps the entries put before it.
         *
         * @param entries the entries to put
         * @return this builder
         * @throws IllegalStateException if {@link #result()} has already been called
         * @throws NullPointerException if {@code entries} is null, or if it yields a null entry, key or value
         */
        @SuppressWarnings("unchecked")
        public Builder<K, V> putAll(Iterable<? extends Tuple2<? extends K, ? extends V>> entries) {
            checkOpen();
            Objects.requireNonNull(entries, "entries is null");
            if (entries instanceof LinkedHashMap<?, ?> map && adopt((LinkedHashMap<K, V>) map)) {
                return this;
            } else if (JavaConverters.underlying(entries) instanceof LinkedHashMap<?, ?> map
                    && adopt((LinkedHashMap<K, V>) map)) {
                return this;
            }
            for (Tuple2<? extends K, ? extends V> entry : entries) {
                put(entry);
            }
            return this;
        }

        /**
         * @return the number of distinct keys put so far
         * @throws IllegalStateException if {@link #result()} has already been called
         */
        public int size() {
            checkOpen();
            return adopted != null ? adopted.size() : slots.size();
        }

        /**
         * Builds the LinkedHashMap. The builder cannot be used afterwards.
         *
         * @return a LinkedHashMap of the entries put, in the order their keys were first put
         * @throws IllegalStateException if {@link #result()} has already been called
         */
        public LinkedHashMap<K, V> result() {
            checkOpen();
            done = true;
            LinkedHashMap<K, V> result;
            if (adopted != null) {
                result = adopted;
            } else if (slots.isEmpty()) {
                result = empty();
            } else {
                result = new LinkedHashMap<>(InsertionOrder.ofAll(keys), slots);
            }
            adopted = null;
            keys = new ArrayList<>(0);
            slots = HashMap.empty();
            return result;
        }

        // the factories: the null checks and messages of LinkedHashMap.put, on an open builder
        private void putChecked(K key, V value) {
            Objects.requireNonNull(key, "LinkedHashMap: key is null");
            Objects.requireNonNull(value, "LinkedHashMap: value is null");
            putEntry(Tuple.of(key, value));
        }

        private void putChecked(Tuple2<K, V> entry) {
            Objects.requireNonNull(entry._1(), "LinkedHashMap: key is null");
            Objects.requireNonNull(entry._2(), "LinkedHashMap: value is null");
            putEntry(entry);
        }

        // LinkedHashMap.put: an absent key is appended, a present one keeps its position and takes the new key object
        @SuppressWarnings("NullAway")
        private void putEntry(Tuple2<K, V> entry) {
            LinkedHashMap<K, V> map = adopted;
            if (map != null) {
                adopted = map.putEntry(entry);
                return;
            }
            K key = entry._1();
            @Nullable Slot<K, V> existing = slots.getOrElse(key, (Slot<K, V>) null);
            if (existing == null) {
                slots = slots.put(key, new Slot<>(entry, keys.size()));
                keys.add(key);
            } else {
                slots = slots.put(key, new Slot<>(entry, existing.index()));
                keys.set(existing.index(), key);
            }
        }

        /// LinkedHashMap.putIfAbsent, for LinkedHashSet.Builder: a key already put keeps its key object and value.
        /// The caller has checked that the builder is open and that neither argument is null.
        void putIfAbsent(K key, V value) {
            LinkedHashMap<K, V> map = adopted;
            if (map != null) {
                adopted = map.putIfAbsent(key, value);
            } else if (!slots.containsKey(key)) {
                slots = slots.put(key, new Slot<>(Tuple.of(key, value), keys.size()));
                keys.add(key);
            }
        }

        /// Takes `map` as the content of an empty builder, and says whether it did. The caller has checked that the
        /// builder is open.
        boolean adopt(LinkedHashMap<K, V> map) {
            if (adopted != null || !slots.isEmpty()) {
                return false;
            }
            adopted = map;
            return true;
        }

        void checkOpen() {
            if (done) {
                throw new IllegalStateException("result() has already been called on this " + name);
            }
        }
    }

    // We need this method to narrow the argument of `ofEntries`.
    // If this method is static with type args <K, V>, the jdk fails to infer types at the call site.
    private LinkedHashMap<K, V> createFromEntries(Iterable<Tuple2<K, V>> tuples) {
        return LinkedHashMap.ofEntries(tuples);
    }
}
