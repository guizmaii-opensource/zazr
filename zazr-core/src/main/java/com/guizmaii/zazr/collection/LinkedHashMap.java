package com.guizmaii.zazr.collection;

import com.guizmaii.zazr.Tuple;
import com.guizmaii.zazr.Tuple2;
import com.guizmaii.zazr.collection.internal.AbstractIterator;
import com.guizmaii.zazr.collection.internal.Collections;
import com.guizmaii.zazr.collection.internal.Iterator;
import com.guizmaii.zazr.collection.internal.JavaConverters;
import com.guizmaii.zazr.collection.internal.MapViews;
import com.guizmaii.zazr.collection.internal.Maps;
import com.guizmaii.zazr.control.Option;
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
 *
 * @param <K> Key type
 * @param <V> Value type
 * @author Ruslan Sennov, Grzegorz Piwowarek
 */
public final class LinkedHashMap<K extends @Nullable Object, V extends @Nullable Object> implements Map<K, V> {

    private static final Object TOMBSTONE = new Object();

    private static final LinkedHashMap<?, ?> EMPTY = new LinkedHashMap<>(Vector.empty(), HashMap.empty(), 0, 0);

    private final Vector<K> list;

    private final HashMap<K, Slot<K, V>> map;

    private final int offset;

    private final int tombstones;

    private LinkedHashMap(Vector<K> list, HashMap<K, Slot<K, V>> map, int offset, int tombstones) {
        this.list = list;
        this.map = map;
        this.offset = offset;
        this.tombstones = tombstones;
    }

    private record Slot<K extends @Nullable Object, V extends @Nullable Object>(Tuple2<K, V> entry, int index) {}

    @SuppressWarnings("unchecked")
    private static <K extends @Nullable Object> K tombstone() {
        return (K) TOMBSTONE;
    }

    /**
     * Returns a {@link java.util.stream.Collector} which may be used in conjunction with
     * {@link java.util.stream.Stream#collect(java.util.stream.Collector)} to obtain a {@link LinkedHashMap}.
     *
     * @param <K> The key type
     * @param <V> The value type
     * @return A {@link LinkedHashMap} Collector.
     */
    public static <K extends @Nullable Object, V extends @Nullable Object> Collector<Tuple2<K, V>, ArrayList<Tuple2<K, V>>, LinkedHashMap<K, V>> collector() {
        final Supplier<ArrayList<Tuple2<K, V>>> supplier = ArrayList::new;
        final BiConsumer<ArrayList<Tuple2<K, V>>, Tuple2<K, V>> accumulator = ArrayList::add;
        final BinaryOperator<ArrayList<Tuple2<K, V>>> combiner = (left, right) -> {
            left.addAll(right);
            return left;
        };
        final Function<ArrayList<Tuple2<K, V>>, LinkedHashMap<K, V>> finisher = LinkedHashMap::ofEntries;
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
    public static <K extends @Nullable Object, V extends @Nullable Object, T extends V> Collector<T, ArrayList<T>, LinkedHashMap<K, V>> collector(Function<? super T, ? extends K> keyMapper) {
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
    public static <K extends @Nullable Object, V extends @Nullable Object, T extends @Nullable Object> Collector<T, ArrayList<T>, LinkedHashMap<K, V>> collector(
            Function<? super T, ? extends K> keyMapper, Function<? super T, ? extends V> valueMapper) {
        Objects.requireNonNull(keyMapper, "keyMapper is null");
        Objects.requireNonNull(valueMapper, "valueMapper is null");
        final Supplier<ArrayList<T>> supplier = ArrayList::new;
        final BiConsumer<ArrayList<T>, T> accumulator = ArrayList::add;
        final BinaryOperator<ArrayList<T>> combiner = (left, right) -> {
            left.addAll(right);
            return left;
        };
        final Function<ArrayList<T>, LinkedHashMap<K, V>> finisher = arr -> LinkedHashMap.ofEntries(Iterator.ofAll(arr)
                .map(t -> Tuple.of(keyMapper.apply(t), valueMapper.apply(t))));
        return Collector.of(supplier, accumulator, combiner, finisher);
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
    public static <K extends @Nullable Object, V extends @Nullable Object> LinkedHashMap<K, V> narrow(LinkedHashMap<? extends K, ? extends V> linkedHashMap) {
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
    public static <K extends @Nullable Object, V extends @Nullable Object> LinkedHashMap<K, V> of(Tuple2<? extends K, ? extends V> entry) {
        final HashMap<K, V> map = HashMap.of(entry);
        final Vector<K> list = Vector.of(((Tuple2<K, V>) entry)._1());
        return wrap(list, map);
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
    public static <K extends @Nullable Object, V extends @Nullable Object> LinkedHashMap<K, V> ofAll(java.util.Map<? extends K, ? extends V> map) {
        Objects.requireNonNull(map, "map is null");
        if (JavaConverters.underlying(map) instanceof LinkedHashMap<?, ?> underlying) {
            return (LinkedHashMap<K, V>) underlying;
        }
        LinkedHashMap<K, V> result = LinkedHashMap.empty();
        for (java.util.Map.Entry<? extends K, ? extends V> entry : map.entrySet()) {
            result = result.put(entry.getKey(), entry.getValue());
        }
        return result;
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
    public static <T extends @Nullable Object, K extends @Nullable Object, V extends @Nullable Object> LinkedHashMap<K, V> ofAll(java.util.stream.Stream<? extends T> stream,
            Function<? super T, Tuple2<? extends K, ? extends V>> entryMapper) {
        return Maps.ofStream(empty(), stream, entryMapper);
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
    public static <T extends @Nullable Object, K extends @Nullable Object, V extends @Nullable Object> LinkedHashMap<K, V> ofAll(java.util.stream.Stream<? extends T> stream,
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
        final HashMap<K, V> map = HashMap.of(key, value);
        final Vector<K> list = Vector.of(key);
        return wrap(list, map);
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
    public static <K extends @Nullable Object, V extends @Nullable Object> LinkedHashMap<K, V> of(K k1, V v1, K k2, V v2) {
        final HashMap<K, V> map = HashMap.of(k1, v1, k2, v2);
        final Vector<K> list = Vector.of(k1, k2);
        return wrapNonUnique(list, map);
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
    public static <K extends @Nullable Object, V extends @Nullable Object> LinkedHashMap<K, V> of(K k1, V v1, K k2, V v2, K k3, V v3) {
        final HashMap<K, V> map = HashMap.of(k1, v1, k2, v2, k3, v3);
        final Vector<K> list = Vector.of(k1, k2, k3);
        return wrapNonUnique(list, map);
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
    public static <K extends @Nullable Object, V extends @Nullable Object> LinkedHashMap<K, V> of(K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4) {
        final HashMap<K, V> map = HashMap.of(k1, v1, k2, v2, k3, v3, k4, v4);
        final Vector<K> list = Vector.of(k1, k2, k3, k4);
        return wrapNonUnique(list, map);
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
    public static <K extends @Nullable Object, V extends @Nullable Object> LinkedHashMap<K, V> of(K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4, K k5, V v5) {
        final HashMap<K, V> map = HashMap.of(k1, v1, k2, v2, k3, v3, k4, v4, k5, v5);
        final Vector<K> list = Vector.of(k1, k2, k3, k4, k5);
        return wrapNonUnique(list, map);
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
    public static <K extends @Nullable Object, V extends @Nullable Object> LinkedHashMap<K, V> of(K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4, K k5, V v5, K k6, V v6) {
        final HashMap<K, V> map = HashMap.of(k1, v1, k2, v2, k3, v3, k4, v4, k5, v5, k6, v6);
        final Vector<K> list = Vector.of(k1, k2, k3, k4, k5, k6);
        return wrapNonUnique(list, map);
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
    public static <K extends @Nullable Object, V extends @Nullable Object> LinkedHashMap<K, V> of(K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4, K k5, V v5, K k6, V v6, K k7, V v7) {
        final HashMap<K, V> map = HashMap.of(k1, v1, k2, v2, k3, v3, k4, v4, k5, v5, k6, v6, k7, v7);
        final Vector<K> list = Vector.of(k1, k2, k3, k4, k5, k6, k7);
        return wrapNonUnique(list, map);
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
    public static <K extends @Nullable Object, V extends @Nullable Object> LinkedHashMap<K, V> of(K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4, K k5, V v5, K k6, V v6, K k7, V v7, K k8, V v8) {
        final HashMap<K, V> map = HashMap.of(k1, v1, k2, v2, k3, v3, k4, v4, k5, v5, k6, v6, k7, v7, k8, v8);
        final Vector<K> list = Vector.of(k1, k2, k3, k4, k5, k6, k7, k8);
        return wrapNonUnique(list, map);
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
    public static <K extends @Nullable Object, V extends @Nullable Object> LinkedHashMap<K, V> of(K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4, K k5, V v5, K k6, V v6, K k7, V v7, K k8, V v8, K k9, V v9) {
        final HashMap<K, V> map = HashMap.of(k1, v1, k2, v2, k3, v3, k4, v4, k5, v5, k6, v6, k7, v7, k8, v8, k9, v9);
        final Vector<K> list = Vector.of(k1, k2, k3, k4, k5, k6, k7, k8, k9);
        return wrapNonUnique(list, map);
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
    public static <K extends @Nullable Object, V extends @Nullable Object> LinkedHashMap<K, V> of(K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4, K k5, V v5, K k6, V v6, K k7, V v7, K k8, V v8, K k9, V v9, K k10, V v10) {
        final HashMap<K, V> map = HashMap.of(k1, v1, k2, v2, k3, v3, k4, v4, k5, v5, k6, v6, k7, v7, k8, v8, k9, v9, k10, v10);
        final Vector<K> list = Vector.of(k1, k2, k3, k4, k5, k6, k7, k8, k9, k10);
        return wrapNonUnique(list, map);
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
     * @throws NullPointerException if {@code f} is null
     */
    @SuppressWarnings("unchecked")
    public static <K extends @Nullable Object, V extends @Nullable Object> LinkedHashMap<K, V> tabulate(int n, Function<? super Integer, ? extends Tuple2<? extends K, ? extends V>> f) {
        Objects.requireNonNull(f, "f is null");
        return ofEntries(Collections.tabulate(n, (Function<? super Integer, ? extends Tuple2<K, V>>) f));
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
     * @throws NullPointerException if {@code s} is null
     */
    @SuppressWarnings("unchecked")
    public static <K extends @Nullable Object, V extends @Nullable Object> LinkedHashMap<K, V> fill(int n, Supplier<? extends Tuple2<? extends K, ? extends V>> s) {
        Objects.requireNonNull(s, "s is null");
        return ofEntries(Collections.fill(n, (Supplier<? extends Tuple2<K, V>>) s));
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
    public static <K extends @Nullable Object, V extends @Nullable Object> LinkedHashMap<K, V> ofEntries(java.util.Map.Entry<? extends K, ? extends V> ... entries) {
        HashMap<K, V> map = HashMap.empty();
        Vector<K> list = Vector.empty();
        for (java.util.Map.Entry<? extends K, ? extends V> entry : entries) {
            Objects.requireNonNull(entry, "LinkedHashMap.ofEntries: entry is null");
            map = map.put(entry.getKey(), entry.getValue());
            list = list.append(entry.getKey());
        }
        return wrapNonUnique(list, map);
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
    public static <K extends @Nullable Object, V extends @Nullable Object> LinkedHashMap<K, V> ofEntries(Tuple2<? extends K, ? extends V> ... entries) {
        Objects.requireNonNull(entries, "entries is null");
        for (Tuple2<? extends K, ? extends V> entry : entries) {
            Objects.requireNonNull(entry, "LinkedHashMap.ofEntries: entry is null");
        }
        final HashMap<K, V> map = HashMap.ofEntries(entries);
        Vector<K> list = Vector.empty();
        for (Tuple2<? extends K, ? extends V> entry : entries) {
            list = list.append(entry._1());
        }
        return wrapNonUnique(list, map);
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
    public static <K extends @Nullable Object, V extends @Nullable Object> LinkedHashMap<K, V> ofEntries(Iterable<? extends Tuple2<? extends K, ? extends V>> entries) {
        Objects.requireNonNull(entries, "entries is null");
        if (entries instanceof LinkedHashMap) {
            return (LinkedHashMap<K, V>) entries;
        } else if (JavaConverters.underlying(entries) instanceof LinkedHashMap<?, ?> underlying) {
            return (LinkedHashMap<K, V>) underlying;
        } else {
            HashMap<K, V> map = HashMap.empty();
            Vector<K> list = Vector.empty();
            for (Tuple2<? extends K, ? extends V> entry : entries) {
                Objects.requireNonNull(entry, "LinkedHashMap.ofEntries: entry is null");
                map = map.put(entry);
                list = list.append(entry._1());
            }
            return wrapNonUnique(list, map);
        }
    }

    @Override
    public <K2 extends @Nullable Object, V2 extends @Nullable Object> LinkedHashMap<K2, V2> mapBoth(Function<? super K, ? extends K2> keyMapper, Function<? super V, ? extends V2> valueMapper) {
        Objects.requireNonNull(keyMapper, "keyMapper is null");
        Objects.requireNonNull(valueMapper, "valueMapper is null");
        final Iterator<Tuple2<K2, V2>> entries = Iterator.ofAll(this).map(entry -> Tuple.of(keyMapper.apply(entry._1()), valueMapper.apply(entry._2())));
        return LinkedHashMap.ofEntries(entries);
    }

    @Override
    public Tuple2<V, LinkedHashMap<K, V>> computeIfAbsent(K key, Function<? super K, ? extends V> mappingFunction) {
        return Maps.computeIfAbsent(this, key, mappingFunction);
    }

    @Override
    public Tuple2<Option<V>, LinkedHashMap<K, V>> computeIfPresent(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
        return Maps.computeIfPresent(this, key, remappingFunction);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Complexity: effectively O(1) (one hash lookup).
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
    public <K2 extends @Nullable Object, V2 extends @Nullable Object> LinkedHashMap<K2, V2> flatMap(BiFunction<? super K, ? super V, ? extends Iterable<Tuple2<K2, V2>>> mapper) {
        Objects.requireNonNull(mapper, "mapper is null");
        return foldLeft(LinkedHashMap.<K2, V2> empty(), (acc, entry) -> {
            for (Tuple2<? extends K2, ? extends V2> mappedEntry : mapper.apply(entry._1(), entry._2())) {
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
        return map.getOrElse(key, null);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Complexity: effectively O(1) (one hash lookup).
     */
    @Override
    public Option<V> get(K key) {
        final Slot<K, V> slot = slotOrNull(key);
        return slot == null ? Option.none() : Option.some(slot.entry()._2());
    }

    @Override
    public V getOrElse(K key, V defaultValue) {
        final Slot<K, V> slot = slotOrNull(key);
        return slot == null ? defaultValue : slot.entry()._2();
    }

    @Override
    public <C extends @Nullable Object> Map<C, LinkedHashMap<K, V>> groupBy(Function<? super Tuple2<K, V>, ? extends C> classifier) {
        return Maps.groupBy(this, this::createFromEntries, classifier);
    }

    @Override
    public boolean isEmpty() {
        return map.isEmpty();
    }

    /**
     * {@inheritDoc}
     * <p>
     * Complexity: O(1) to create; each step is effectively O(1) (one hash lookup per key of the insertion order,
     * skipping the removed keys' markers), a whole walk O(n).
     */
    @Override
    public java.util.Iterator<Tuple2<K, V>> iterator() {
        final java.util.Iterator<K> slots = list.iterator();
        return new AbstractIterator<Tuple2<K, V>>() {
            private @Nullable K nextKey;
            private boolean nextKeyDefined;

            @Override
            public boolean hasNext() {
                while (!nextKeyDefined && slots.hasNext()) {
                    final K key = slots.next();
                    if (key != TOMBSTONE) {
                        nextKey = key;
                        nextKeyDefined = true;
                    }
                }
                return nextKeyDefined;
            }

            @Override
            // hasNext() sets nextKey whenever it sets nextKeyDefined, and AbstractIterator only
            // calls getNext() after hasNext() returned true.
            @SuppressWarnings("NullAway")
            protected Tuple2<K, V> getNext() {
                nextKeyDefined = false;
                return map.get(nextKey).get().entry();
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
     * Complexity: O(1); {@code get} and {@code containsKey} on the view are effectively O(1), and so is each step of
     * its iterators, in either order.
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
     * Complexity: O(1) to create; each step is effectively O(1) (one positional read and one hash lookup per key,
     * skipping the removed keys' markers), a whole walk O(n).
     *
     * @return a new iterator
     */
    java.util.Iterator<Tuple2<K, V>> reverseIterator() {
        return new AbstractIterator<Tuple2<K, V>>() {
            private int index = list.size() - 1;
            private @Nullable Tuple2<K, V> next;

            @Override
            public boolean hasNext() {
                while (next == null && index >= 0) {
                    final K key = list.get(index--);
                    if (key != TOMBSTONE) {
                        next = entryAt(key);
                    }
                }
                return next != null;
            }

            @Override
            // AbstractIterator only calls getNext() after hasNext() returned true, which set next
            @SuppressWarnings("NullAway")
            protected Tuple2<K, V> getNext() {
                final Tuple2<K, V> entry = next;
                next = null;
                return entry;
            }
        };
    }

    /**
     * {@inheritDoc}
     * <p>
     * Complexity: O(1) (a LinkedHashSet view sharing this map).
     */
    @SuppressWarnings("unchecked")
    @Override
    public Set<K> keySet() {
        return LinkedHashSet.wrap((LinkedHashMap<K, Object>) this);
    }

    @Override
    public <K2 extends @Nullable Object, V2 extends @Nullable Object> LinkedHashMap<K2, V2> collect(BiFunction<? super K, ? super V, ? extends Option<? extends Tuple2<K2, V2>>> mapper) {
        Objects.requireNonNull(mapper, "mapper is null");
        return foldLeft(LinkedHashMap.empty(), (acc, entry) -> {
            final Option<? extends Tuple2<K2, V2>> collected = Objects.requireNonNull(mapper.apply(entry._1(), entry._2()), "LinkedHashMap.collect: mapper returned null");
            return collected.isDefined() ? acc.put(collected.get()) : acc;
        });
    }

    @Override
    public <K2 extends @Nullable Object, V2 extends @Nullable Object> LinkedHashMap<K2, V2> map(BiFunction<? super K, ? super V, Tuple2<K2, V2>> mapper) {
        Objects.requireNonNull(mapper, "mapper is null");
        return foldLeft(LinkedHashMap.empty(), (acc, entry) -> acc.put(entry.map(mapper)));
    }

    @Override
    public <K2 extends @Nullable Object> LinkedHashMap<K2, V> mapKeys(Function<? super K, ? extends K2> keyMapper) {
        Objects.requireNonNull(keyMapper, "keyMapper is null");
        return map((k, v) -> Tuple.of(keyMapper.apply(k), v));
    }

    @Override
    public <K2 extends @Nullable Object> LinkedHashMap<K2, V> mapKeys(Function<? super K, ? extends K2> keyMapper, BiFunction<? super V, ? super V, ? extends V> valueMerge) {
        return Collections.mapKeys(this, LinkedHashMap.empty(), keyMapper, valueMerge);
    }

    @Override
    public <W extends @Nullable Object> LinkedHashMap<K, W> mapValues(Function<? super V, ? extends W> mapper) {
        Objects.requireNonNull(mapper, "mapper is null");
        return map((k, v) -> Tuple.of(k, mapper.apply(v)));
    }

    @Override
    public LinkedHashMap<K, V> merge(Map<? extends K, ? extends V> that) {
        return Maps.merge(this, this::createFromEntries, that);
    }

    @Override
    public <U extends V> LinkedHashMap<K, V> merge(Map<? extends K, U> that,
                                                   BiFunction<? super V, ? super U, ? extends V> collisionResolution) {
        return Maps.merge(this, this::createFromEntries, that, collisionResolution);
    }

    @Override
    public LinkedHashMap<K, V> orElse(Iterable<? extends Tuple2<K, V>> other) {
        return isEmpty() ? ofEntries(other) : this;
    }

    @Override
    public LinkedHashMap<K, V> orElse(Supplier<? extends Iterable<? extends Tuple2<K, V>>> supplier) {
        return isEmpty() ? ofEntries(supplier.get()) : this;
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
     * Complexity: effectively O(1) (one lookup and one {@link #put(Object, Object)}).
     */
    @Override
    public <U extends V> LinkedHashMap<K, V> put(K key, U value, BiFunction<? super V, ? super U, ? extends V> merge) {
        return Maps.put(this, key, value, merge);
    }

    /**
     * Associates the specified value with the specified key in this map.
     * If the map previously contained a mapping for the key, both the key and
     * the value are replaced by the specified ones, keeping the original
     * insertion order.
     * <p>
     * Overwriting an existing key and inserting a new key both run in effectively
     * constant time (O(log32 n)): the insertion-order structure is left untouched
     * when a key is overwritten and appended to when a new key is inserted.
     * <p>
     * Complexity: effectively O(1) (one hash lookup and one hash insertion; a new key is appended to the insertion
     * order).
     *
     * @param key   key with which the specified value is to be associated
     * @param value value to be associated with the specified key
     * @return A new Map containing these elements and that entry.
     */
    @Override
    public LinkedHashMap<K, V> put(K key, V value) {
        Objects.requireNonNull(key, "LinkedHashMap: key is null");
        Objects.requireNonNull(value, "LinkedHashMap: value is null");
        final Slot<K, V> existing = slotOrNull(key);
        if (existing != null) {
            return new LinkedHashMap<>(list, map.put(key, new Slot<>(Tuple.of(key, value), existing.index())), offset, tombstones);
        } else {
            return new LinkedHashMap<>(list.append(key), map.put(key, new Slot<>(Tuple.of(key, value), offset + list.size())), offset, tombstones);
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * Complexity: effectively O(1), that of {@link #put(Object, Object)}.
     */
    @Override
    public LinkedHashMap<K, V> put(Tuple2<? extends K, ? extends V> entry) {
        return Maps.put(this, entry);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Complexity: effectively O(1) (one lookup and one {@link #put(Object, Object)}).
     */
    @Override
    public <U extends V> LinkedHashMap<K, V> put(Tuple2<? extends K, U> entry,
                                                 BiFunction<? super V, ? super U, ? extends V> merge) {
        return Maps.put(this, entry, merge);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Complexity: effectively O(1) (one hash removal and one marker in the insertion order), amortised: when the
     * markers outnumber the entries, the insertion order is rebuilt in O(n).
     */
    @Override
    public LinkedHashMap<K, V> remove(K key) {
        final Slot<K, V> existing = slotOrNull(key);
        if (existing == null) {
            return this;
        }
        final HashMap<K, Slot<K, V>> newMap = map.remove(key);
        if (newMap.isEmpty()) {
            return empty();
        }
        final K tombstone = tombstone();
        final Vector<K> newList = list.update(existing.index() - offset, tombstone);
        return normalized(newList, newMap, offset, tombstones + 1);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Complexity: O(n) (the kept entries copied into a new map).
     */
    @Override
    @Deprecated
    public LinkedHashMap<K, V> removeAll(BiPredicate<? super K, ? super V> predicate) {
        Objects.requireNonNull(predicate, "predicate is null");
        return reject(predicate);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Complexity: O(m + n) for m given keys (a hash set of them, a filter of the hash map, then the insertion order
     * rebuilt).
     */
    @Override
    public LinkedHashMap<K, V> removeAll(Iterable<? extends K> keys) {
        Objects.requireNonNull(keys, "keys is null");
        final HashSet<K> toRemove = HashSet.ofAll(keys);
        final HashMap<K, Slot<K, V>> newMap = map.filter(t -> !toRemove.contains(t._1()));
        return newMap.size() == map.size() ? this : reindex(list, newMap);
    }

    @Override
    @Deprecated
    public LinkedHashMap<K, V> removeKeys(Predicate<? super K> predicate) {
        Objects.requireNonNull(predicate, "predicate is null");
        return rejectKeys(predicate);
    }

    @Override
    @Deprecated
    public LinkedHashMap<K, V> removeValues(Predicate<? super V> predicate) {
        Objects.requireNonNull(predicate, "predicate is null");
        return rejectValues(predicate);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Complexity: effectively O(1) amortised, as {@link #remove(Object)}; the new entry takes the position of the
     * replaced one.
     */
    @Override
    public LinkedHashMap<K, V> replace(Tuple2<K, V> currentElement, Tuple2<K, V> newElement) {
        Objects.requireNonNull(currentElement, "currentElement is null");
        Objects.requireNonNull(newElement, "newElement is null");
        Objects.requireNonNull(newElement._1(), "LinkedHashMap: key is null");
        Objects.requireNonNull(newElement._2(), "LinkedHashMap: value is null");

        // We replace the whole element, i.e. key and value have to be present.
        if (!Objects.equals(currentElement, newElement) && contains(currentElement)) {
            return replaceKey(currentElement._1(), newElement);
        } else {
            return this;
        }
    }

    /// This map with the entry of `currentKey`, which must be present, replaced by `newElement` at the same position
    /// in the iteration order; an entry of `newElement`'s key elsewhere in the map is removed. The value of
    /// `currentKey` plays no part, which is what [LinkedHashSet#replace] needs for a key set whose values are not its
    /// elements.
    LinkedHashMap<K, V> replaceKey(K currentKey, Tuple2<K, V> newElement) {
        Vector<K> newList = list;
        HashMap<K, Slot<K, V>> newMap = map;
        int newTombstones = tombstones;

        final K newKey = newElement._1();

        // If current key and new key are equal, the key keeps its position,
        // otherwise we need to remove an already present newKey from the order manually.
        if (!Objects.equals(currentKey, newKey) && newMap.containsKey(newKey)) {
            final Slot<K, V> obsolete = newMap.get(newKey).get();
            final K tombstone = tombstone();
            newList = newList.update(obsolete.index() - offset, tombstone);
            newMap = newMap.remove(newKey);
            newTombstones++;
        }

        final Slot<K, V> currentSlot = newMap.get(currentKey).get();
        newList = newList.update(currentSlot.index() - offset, newKey);
        newMap = newMap.remove(currentKey).put(newKey, new Slot<>(newElement, currentSlot.index()));

        return normalized(newList, newMap, offset, newTombstones);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Complexity: effectively O(1) amortised, that of {@link #replace(Tuple2, Tuple2)}: a map holds an entry once.
     */
    @Override
    public LinkedHashMap<K, V> replaceAll(Tuple2<K, V> currentElement, Tuple2<K, V> newElement) {
        return Maps.replaceAll(this, currentElement, newElement);
    }

    @Override
    public LinkedHashMap<K, V> replaceValue(K key, V value) {
        return Maps.replaceValue(this, key, value);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Complexity: effectively O(1) (one lookup and one {@code put} of an existing key).
     */
    @Override
    public LinkedHashMap<K, V> replace(K key, V oldValue, V newValue) {
        return Maps.replace(this, key, oldValue, newValue);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Complexity: O(n) (every entry put into a new map).
     */
    @Override
    public LinkedHashMap<K, V> replaceAll(BiFunction<? super K, ? super V, ? extends V> function) {
        return Maps.replaceAll(this, function);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Complexity: O(m + n) for m given entries (a hash set of them, then the kept entries copied into a new map).
     */
    @Override
    public LinkedHashMap<K, V> retainAll(Iterable<? extends Tuple2<K, V>> elements) {
        return Collections.retainAll(this, elements, kept -> filter(kept));
    }

    @Override
    public int size() {
        return map.size();
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

    // -- Positional operations, in insertion order

    /**
     * The first entry in insertion order.
     * <p>
     * Complexity: effectively O(1) (the first key of the insertion order, then one hash lookup).
     *
     * @return the entry inserted first among those present
     * @throws java.util.NoSuchElementException if this map is empty
     */
    public Tuple2<K, V> head() {
        if (isEmpty()) {
            throw new java.util.NoSuchElementException("head of empty LinkedHashMap");
        }
        return entryAt(list.head());
    }

    /**
     * The first entry in insertion order, if any.
     *
     * @return {@code Some} of {@link #head()}, or {@code None} if this map is empty
     */
    public Option<Tuple2<K, V>> headOption() {
        return isEmpty() ? Option.none() : Option.some(head());
    }

    /**
     * The last entry in insertion order.
     * <p>
     * Complexity: effectively O(1) (the last key of the insertion order, then one hash lookup).
     *
     * @return the entry inserted last among those present
     * @throws java.util.NoSuchElementException if this map is empty
     */
    public Tuple2<K, V> last() {
        if (isEmpty()) {
            throw new java.util.NoSuchElementException("last of empty LinkedHashMap");
        }
        return entryAt(list.last());
    }

    /**
     * The last entry in insertion order, if any.
     *
     * @return {@code Some} of {@link #last()}, or {@code None} if this map is empty
     */
    public Option<Tuple2<K, V>> lastOption() {
        return isEmpty() ? Option.none() : Option.some(last());
    }

    /**
     * All entries but the last in insertion order.
     * <p>
     * Complexity: effectively O(1) (one key removed from the hash map, the insertion order sliced), plus a walk past
     * the removed keys' markers next to the last entry, if any.
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
     * Complexity: effectively O(1) (one {@code init}).
     *
     * @return {@code Some} of {@link #init()}, or {@code None} if this map is empty
     */
    public Option<LinkedHashMap<K, V>> initOption() {
        return isEmpty() ? Option.none() : Option.some(init());
    }

    /**
     * All entries but the first in insertion order.
     * <p>
     * Complexity: effectively O(1) (one key removed from the hash map, the insertion order sliced), plus a walk past
     * the removed keys' markers next to the first entry, if any.
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
     * Complexity: effectively O(1) (one {@code tail}).
     *
     * @return {@code Some} of {@link #tail()}, or {@code None} if this map is empty
     */
    public Option<LinkedHashMap<K, V>> tailOption() {
        return isEmpty() ? Option.none() : Option.some(tail());
    }

    /**
     * The first {@code n} entries in insertion order: empty if {@code n <= 0}, this map if {@code n >= size()}.
     * <p>
     * Complexity: effectively O(min(n, size - n)) (the smaller of the kept and the removed keys is inserted into or
     * removed from the hash map; the insertion order is sliced). After removals, finding the cut also walks the
     * insertion order from the nearer end past the removed keys' markers.
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
     * Complexity: effectively O(min(n, size - n)), that of {@link #take(int)}, counted from the other end.
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
     * Complexity: O(k) for a prefix of k entries (one walk), then one {@link #take(int)}.
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
     * Complexity: O(k) for a prefix of k entries (one walk), then one {@link #take(int)}.
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
     * Complexity: effectively O(min(n, size - n)), that of {@link #take(int)}.
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
     * Complexity: effectively O(min(n, size - n)), that of {@link #take(int)}.
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
     * Complexity: O(k) for k dropped entries (one walk), then one {@link #drop(int)}.
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
     * Complexity: O(k) for k dropped entries (one walk), then one {@link #drop(int)}.
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
        final Vector.Builder<Tuple2<Tuple2<K, V>, Integer>> builder = Vector.newBuilder(size());
        int index = 0;
        for (K key : list) {
            if (key != TOMBSTONE) {
                builder.add(Tuple.of(entryAt(key), index++));
            }
        }
        return builder.result();
    }

    /**
     * The blocks of {@code size} consecutive entries in insertion order; the last block is smaller when
     * {@code size} does not divide {@code size()}. The same as {@code sliding(size, size)}.
     * <p>
     * Complexity: O(n), that of {@link #sliding(int, int)} with a step of {@code size}.
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
     * Complexity: O(n * size), that of {@link #sliding(int, int)} with a step of 1.
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
     * Complexity: O(n + (n / step) * min(size, n - size)): O(n) to drop the removed keys' markers from the insertion
     * order if there are any, then per window that of {@link #take(int)} on a window of {@code size} entries,
     * effectively O(min(size, n - size)).
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
     * Complexity: O(n) walk (plus O(n) to drop the removed keys' markers from the insertion order if there are any),
     * then per run that of {@link #take(int)} on the run.
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
        final int length = size();
        if (length == 0) {
            return Vector.empty();
        }
        // without markers, every rank is its own index into the insertion order, so each window is found in O(1)
        final LinkedHashMap<K, V> compact = compacted();
        final Vector.Builder<R> builder = Vector.newBuilder();
        // past the first, a window is produced only while it holds at least one entry the previous one did not
        for (long start = 0; start < length && (start == 0 || start - step + size < length); start += step) {
            builder.add(wrap.apply(compact.slice((int) start, (int) Math.min(start + size, length))));
        }
        return builder.result();
    }

    // slideBy(classifier) over the entries, each run wrapped by `wrap`
    private <R extends @Nullable Object> Vector<R> runs(Function<? super Tuple2<K, V>, ?> classifier, Function<LinkedHashMap<K, V>, R> wrap) {
        Objects.requireNonNull(classifier, "classifier is null");
        return runs(classifier, false, wrap);
    }

    // slideBy(classifier) over the keys alone, each run wrapped by `wrap`: LinkedHashSet's, with no entry looked up
    <R extends @Nullable Object> Vector<R> runsByKey(Function<? super K, ?> classifier, Function<LinkedHashMap<K, V>, R> wrap) {
        Objects.requireNonNull(classifier, "classifier is null");
        return runs(classifier, true, wrap);
    }

    // the classifier takes a key when `byKey`, an entry otherwise
    @SuppressWarnings("unchecked")
    private <R extends @Nullable Object> Vector<R> runs(Function<?, ?> classifier, boolean byKey, Function<LinkedHashMap<K, V>, R> wrap) {
        if (isEmpty()) {
            return Vector.empty();
        }
        final Function<Object, ?> classify = (Function<Object, ?>) classifier;
        final LinkedHashMap<K, V> compact = compacted();
        final Vector.Builder<R> builder = Vector.newBuilder();
        final Vector<K> keys = compact.list;
        final int length = keys.size();
        Object key = classify.apply(byKey ? keys.get(0) : compact.entryAt(keys.get(0)));
        int start = 0;
        for (int index = 1; index < length; index++) {
            final Object next = classify.apply(byKey ? keys.get(index) : compact.entryAt(keys.get(index)));
            if (!Objects.equals(key, next)) {
                builder.add(wrap.apply(compact.slice(start, index)));
                start = index;
                key = next;
            }
        }
        builder.add(wrap.apply(compact.slice(start, length)));
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
        int length = 0;
        for (K key : list) {
            if (key != TOMBSTONE) {
                if (predicate.test(entryAt(key)) != expected) {
                    break;
                }
                length++;
            }
        }
        return length;
    }

    // the number of leading keys, in insertion order, for which predicate returns `expected`: LinkedHashSet's
    // takeWhile and its siblings, with no entry looked up
    int countLeadingKeys(Predicate<? super K> predicate, boolean expected) {
        int length = 0;
        for (K key : list) {
            if (key != TOMBSTONE) {
                if (predicate.test(key) != expected) {
                    break;
                }
                length++;
            }
        }
        return length;
    }

    // the keys paired with their position in insertion order: LinkedHashSet's zipWithIndex, with no entry looked up
    Vector<Tuple2<K, Integer>> zipKeysWithIndex() {
        final Vector.Builder<Tuple2<K, Integer>> builder = Vector.newBuilder(size());
        int index = 0;
        for (K key : list) {
            if (key != TOMBSTONE) {
                builder.add(Tuple.of(key, index++));
            }
        }
        return builder.result();
    }

    // this map with no removed keys' markers in the insertion order: this map itself when it has none
    private LinkedHashMap<K, V> compacted() {
        return tombstones == 0 ? this : reindex(list, map);
    }

    // the index in `list` of the entry of the given rank (0 <= rank < size()), walked from the nearer end
    private int indexOfRank(int rank) {
        if (tombstones == 0) {
            return rank;
        }
        final int size = size();
        if (rank < size - rank) {
            int live = -1;
            for (int i = 0; ; i++) {
                if (list.get(i) != TOMBSTONE && ++live == rank) {
                    return i;
                }
            }
        } else {
            int live = size;
            for (int i = list.size() - 1; ; i--) {
                if (list.get(i) != TOMBSTONE && --live == rank) {
                    return i;
                }
            }
        }
    }

    /**
     * The entries of rank {@code from} (inclusive) to {@code until} (exclusive) in insertion order, clamped; this map
     * when nothing is cut off, the empty map when nothing is kept. The insertion order is sliced and the slots keep
     * their absolute index (the offset moves with the cut), so the hash map changes by the smaller of the kept and
     * the removed keys: rebuilt from the kept ones, or the removed ones taken out of it.
     * <p>
     * Complexity: effectively O(min(n, size - n)), that of {@link #take(int)}.
     */
    LinkedHashMap<K, V> slice(int from, int until) {
        final int size = size();
        final int start = Math.max(from, 0);
        final int end = Math.min(until, size);
        if (start >= end) {
            return empty();
        } else if (start == 0 && end == size) {
            return this;
        }
        final int lo = indexOfRank(start);
        final int hi = indexOfRank(end - 1) + 1;
        final int kept = end - start;
        final Vector<K> newList = list.slice(lo, hi);
        HashMap<K, Slot<K, V>> newMap;
        if (kept <= size - kept) {
            newMap = HashMap.empty();
            for (int i = lo; i < hi; i++) {
                final K key = list.get(i);
                if (key != TOMBSTONE) {
                    newMap = newMap.put(key, slotAt(key));
                }
            }
        } else {
            newMap = map;
            for (int i = 0; i < lo; i++) {
                final K key = list.get(i);
                if (key != TOMBSTONE) {
                    newMap = newMap.remove(key);
                }
            }
            final int listSize = list.size();
            for (int i = hi; i < listSize; i++) {
                final K key = list.get(i);
                if (key != TOMBSTONE) {
                    newMap = newMap.remove(key);
                }
            }
        }
        return normalized(newList, newMap, offset + lo, (hi - lo) - kept);
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
        return mkString("LinkedHashMap(", ", ", ")");
    }

    /**
     * Construct Map with given values and key order.
     *
     * @param list The list of keys with unique entries.
     * @param map  The map of key-value tuples.
     * @param <K>  The key type
     * @param <V>  The value type
     * @return A new Map containing the given map with given key order
     */
    private static <K extends @Nullable Object, V extends @Nullable Object> LinkedHashMap<K, V> wrap(Vector<K> list, HashMap<K, V> map) {
        if (list.isEmpty()) {
            return empty();
        }
        HashMap<K, Slot<K, V>> indexed = HashMap.empty();
        int index = 0;
        for (K key : list) {
            indexed = indexed.put(key, new Slot<>(map.getEntry(key).get(), index++));
        }
        return new LinkedHashMap<>(list, indexed, 0, 0);
    }

    /**
     * Construct Map with given values and key order.
     *
     * @param list The list of keys with possibly non-unique entries.
     * @param map  The map of key-value tuples.
     * @param <K>  The key type
     * @param <V>  The value type
     * @return A new Map containing the given map with given key order
     */
    private static <K extends @Nullable Object, V extends @Nullable Object> LinkedHashMap<K, V> wrapNonUnique(Vector<K> list, HashMap<K, V> map) {
        if (list.size() == map.size()) {
            return wrap(list, map);
        }
        // Keep the last occurrence of every key, matching the value precedence in `map`.
        return wrap(list.reverse().distinct().reverse().toVector(), map);
    }

    private static <K extends @Nullable Object, V extends @Nullable Object> LinkedHashMap<K, V> normalized(Vector<K> list, HashMap<K, Slot<K, V>> map, int offset, int tombstones) {
        while (list.head() == TOMBSTONE) {
            list = list.tail();
            offset++;
            tombstones--;
        }
        while (list.last() == TOMBSTONE) {
            list = list.init();
            tombstones--;
        }
        if (tombstones > map.size()) {
            return reindex(list, map);
        }
        return new LinkedHashMap<>(list, map, offset, tombstones);
    }

    private static <K extends @Nullable Object, V extends @Nullable Object> LinkedHashMap<K, V> reindex(Vector<K> list, HashMap<K, Slot<K, V>> survivors) {
        if (survivors.isEmpty()) {
            return empty();
        }
        final ArrayList<K> liveKeys = new ArrayList<>(survivors.size());
        for (K key : list) {
            if (key != TOMBSTONE && survivors.containsKey(key)) {
                liveKeys.add(key);
            }
        }
        HashMap<K, Slot<K, V>> indexed = HashMap.empty();
        int index = 0;
        for (K key : liveKeys) {
            indexed = indexed.put(key, new Slot<>(survivors.get(key).get().entry(), index++));
        }
        return new LinkedHashMap<>(Vector.ofAll(liveKeys), indexed, 0, 0);
    }

    // We need this method to narrow the argument of `ofEntries`.
    // If this method is static with type args <K, V>, the jdk fails to infer types at the call site.
    private LinkedHashMap<K, V> createFromEntries(Iterable<Tuple2<K, V>> tuples) {
        return LinkedHashMap.ofEntries(tuples);
    }

}
