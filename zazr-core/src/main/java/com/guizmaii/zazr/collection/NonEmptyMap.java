package com.guizmaii.zazr.collection;

import com.guizmaii.zazr.Tuple;
import com.guizmaii.zazr.Tuple2;
import com.guizmaii.zazr.collection.internal.NonEmptyModule;
import com.guizmaii.zazr.control.Option;
import java.util.Comparator;
import java.util.Objects;
import java.util.Spliterator;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collector;
import org.jspecify.annotations.Nullable;

/**
 * A {@link HashMap} with at least one entry (zio-prelude's {@code NonEmptyMap}): a wrapper, not a subtype, so that
 * every method can state in its return type whether the result is still non-empty.
 * <p>
 * The contract, per method family:
 * <ul>
 * <li>operations that cannot remove every entry return a {@code NonEmptyMap}: {@code put}, {@code merge},
 * {@code computeIfAbsent}, {@code computeIfPresent}, {@code map}, {@code mapBoth}, {@code mapKeys}, {@code mapValues},
 * {@code flatMap}, {@code replace}, {@code replaceAll}, {@code replaceValue}, {@code tap}; {@code keySet} returns a
 * {@link NonEmptySet}, {@code values} a {@link NonEmptyVector}, and {@code groupBy} non-empty groups;</li>
 * <li>operations that can shrink return a {@link HashMap}: {@code filter}, {@code filterKeys}, {@code filterValues},
 * {@code reject}, {@code rejectKeys}, {@code rejectValues}, {@code collect}, {@code flatMapAll}, {@code remove},
 * {@code removeAll}, {@code retainAll}, {@code partition};</li>
 * <li>operations that are partial on a {@code HashMap} are total here: {@code max}, {@code min}, {@code maxBy},
 * {@code minBy}, {@code reduce}, {@code reduceMap};</li>
 * <li>narrowing back to a {@code NonEmptyMap} returns an {@link Option}: {@link #fromMap(HashMap)},
 * {@link #fromIterable(Iterable)}, {@link HashMap#toNonEmptyMap()}.</li>
 * </ul>
 * A hash map has no order, so there is no {@code head}; {@link NonEmptySortedMap} has one.
 * <p>
 * Two {@code flatMap}s cannot share a name (a lambda argument would be ambiguous), so {@code flatMap} is the one whose
 * function returns a {@code NonEmptyMap} and {@code flatMapAll} the one whose function returns any {@code Iterable} of
 * entries.
 * <p>
 * Like every map, it rejects null keys and values. Equal to another {@code NonEmptyMap} or {@code NonEmptySortedMap}
 * with the same entries, as maps are equal to maps, never to a plain {@link Map}; use {@link #toMap()} to compare
 * across the two.
 *
 * @param <K> Key type
 * @param <V> Value type
 */
public final class NonEmptyMap<K extends @Nullable Object, V extends @Nullable Object> implements Iterable<Tuple2<K, V>> {

    private final HashMap<K, V> map;

    private NonEmptyMap(HashMap<K, V> map) { this.map = map; }

    // -- constructors

    /**
     * A {@code NonEmptyMap} of one entry.
     *
     * @param key   The key
     * @param value The value
     * @param <K>   Key type
     * @param <V>   Value type
     * @return a non-empty map of size 1
     * @throws NullPointerException if {@code key} or {@code value} is null
     */
    public static <K extends @Nullable Object, V extends @Nullable Object> NonEmptyMap<K, V> single(K key, V value) {
        Objects.requireNonNull(key, "NonEmptyMap: key is null");
        Objects.requireNonNull(value, "NonEmptyMap: value is null");
        return new NonEmptyMap<>(HashMap.of(key, value));
    }

    /**
     * A {@code NonEmptyMap} of {@code head} and the entries of {@code tail}; of two entries with the same key, the
     * later one wins.
     *
     * @param head An entry
     * @param tail The other entries
     * @param <K>  Key type
     * @param <V>  Value type
     * @return a non-empty map
     * @throws NullPointerException if {@code head}, {@code tail}, an entry of {@code tail}, or a key or value is null
     */
    @SafeVarargs
    @SuppressWarnings("varargs")
    public static <K extends @Nullable Object, V extends @Nullable Object> NonEmptyMap<K, V> of(Tuple2<? extends K, ? extends V> head, Tuple2<? extends K, ? extends V>... tail) {
        Objects.requireNonNull(head, "NonEmptyMap: head is null");
        Objects.requireNonNull(tail, "NonEmptyMap: tail is null");
        final HashMap.Builder<K, V> builder = HashMap.newBuilder();
        put(builder, head);
        for (Tuple2<? extends K, ? extends V> entry : tail) {
            put(builder, entry);
        }
        return new NonEmptyMap<>(builder.result());
    }

    /**
     * A {@code NonEmptyMap} of {@code head} and the entries of {@code tail}; of two entries with the same key, the
     * later one wins.
     *
     * @param head An entry
     * @param tail The other entries, read once
     * @param <K>  Key type
     * @param <V>  Value type
     * @return a non-empty map
     * @throws NullPointerException if {@code head}, {@code tail}, an entry of {@code tail}, or a key or value is null
     */
    public static <K extends @Nullable Object, V extends @Nullable Object> NonEmptyMap<K, V> fromIterable(Tuple2<? extends K, ? extends V> head, Iterable<? extends Tuple2<? extends K, ? extends V>> tail) {
        Objects.requireNonNull(head, "NonEmptyMap: head is null");
        Objects.requireNonNull(tail, "NonEmptyMap: tail is null");
        final HashMap.Builder<K, V> builder = HashMap.newBuilder();
        put(builder, head);
        return new NonEmptyMap<>(putAll(builder, tail).result());
    }

    /**
     * Wraps a {@link HashMap} if it is not empty.
     * <p>
     * Complexity: O(1).
     *
     * @param map A map
     * @param <K> Key type
     * @param <V> Value type
     * @return {@code Some(nonEmptyMap)} sharing {@code map}'s entries, or {@code None} if {@code map} is empty
     * @throws NullPointerException if {@code map} is null
     */
    public static <K extends @Nullable Object, V extends @Nullable Object> Option<NonEmptyMap<K, V>> fromMap(HashMap<K, V> map) {
        Objects.requireNonNull(map, "NonEmptyMap.fromMap: map is null");
        return map.isEmpty() ? Option.none() : Option.some(new NonEmptyMap<>(map));
    }

    /**
     * Copies entries if there is at least one; a {@code HashMap} is wrapped without copying. Of two entries with the
     * same key, the later one wins.
     *
     * @param entries The entries, read once
     * @param <K>     Key type
     * @param <V>     Value type
     * @return {@code Some(nonEmptyMap)} of the entries, or {@code None} if there is none
     * @throws NullPointerException if {@code entries}, an entry, or a key or value is null
     */
    @SuppressWarnings("unchecked")
    public static <K extends @Nullable Object, V extends @Nullable Object> Option<NonEmptyMap<K, V>> fromIterable(Iterable<? extends Tuple2<? extends K, ? extends V>> entries) {
        Objects.requireNonNull(entries, "NonEmptyMap.fromIterable: entries is null");
        if (entries instanceof HashMap) {
            return fromMap((HashMap<K, V>) entries);
        }
        return fromMap(putAll(HashMap.<K, V> newBuilder(), entries).result());
    }

    /**
     * Wraps a {@link HashMap} known to be non-empty.
     * <p>
     * Complexity: O(1).
     *
     * @param map A non-empty map
     * @param <K> Key type
     * @param <V> Value type
     * @return a non-empty map sharing {@code map}'s entries
     * @throws IllegalArgumentException if {@code map} is empty
     * @throws NullPointerException     if {@code map} is null
     */
    public static <K extends @Nullable Object, V extends @Nullable Object> NonEmptyMap<K, V> unsafeFromMap(HashMap<K, V> map) {
        Objects.requireNonNull(map, "NonEmptyMap.unsafeFromMap: map is null");
        if (map.isEmpty()) {
            throw new IllegalArgumentException("NonEmptyMap.unsafeFromMap: map is empty");
        }
        return new NonEmptyMap<>(map);
    }

    /* an entry checked with messages naming this type, then put */
    private static <K extends @Nullable Object, V extends @Nullable Object> void put(HashMap.Builder<K, V> builder, @Nullable Tuple2<? extends K, ? extends V> entry) {
        Objects.requireNonNull(entry, "NonEmptyMap: entry is null");
        builder.put(Objects.requireNonNull(entry._1(), "NonEmptyMap: key is null"), Objects.requireNonNull(entry._2(), "NonEmptyMap: value is null"));
    }

    /* a HashMap holds no null, so it is put as is; anything else is checked entry by entry, naming this type */
    private static <K extends @Nullable Object, V extends @Nullable Object> HashMap.Builder<K, V> putAll(HashMap.Builder<K, V> builder, Iterable<? extends Tuple2<? extends K, ? extends V>> entries) {
        if (entries instanceof HashMap) {
            return builder.putAll(entries);
        }
        for (Tuple2<? extends K, ? extends V> entry : entries) {
            put(builder, entry);
        }
        return builder;
    }

    // -- returns NonEmptyMap: at least one entry is left

    /**
     * Complexity: effectively O(1), that of {@link HashMap#put(Object, Object)}.
     *
     * @param key   A key
     * @param value Its value
     * @return this map with {@code key} mapped to {@code value}
     * @throws NullPointerException if {@code key} or {@code value} is null
     */
    public NonEmptyMap<K, V> put(K key, V value) {
        Objects.requireNonNull(key, "NonEmptyMap.put: key is null");
        Objects.requireNonNull(value, "NonEmptyMap.put: value is null");
        return new NonEmptyMap<>(map.put(key, value));
    }

    /**
     * Complexity: effectively O(1), that of {@link HashMap#put(Tuple2)}.
     *
     * @param entry An entry
     * @return this map with {@code entry}
     * @throws NullPointerException if {@code entry}, its key or its value is null
     */
    public NonEmptyMap<K, V> put(Tuple2<? extends K, ? extends V> entry) {
        Objects.requireNonNull(entry, "NonEmptyMap.put: entry is null");
        return put(entry._1(), entry._2());
    }

    /**
     * Complexity: effectively O(1), that of {@link HashMap#put(Object, Object, BiFunction)}.
     *
     * @param key   A key
     * @param value A value
     * @param merge Combines the current value of {@code key}, if any, with {@code value}
     * @param <U>   Type of {@code value}
     * @return this map with {@code key} mapped to {@code value}, or to the merged value if {@code key} was present
     * @throws NullPointerException if an argument is null or {@code merge} returns null
     */
    public <U extends V> NonEmptyMap<K, V> put(K key, U value, BiFunction<? super V, ? super U, ? extends V> merge) {
        Objects.requireNonNull(key, "NonEmptyMap.put: key is null");
        Objects.requireNonNull(value, "NonEmptyMap.put: value is null");
        return new NonEmptyMap<>(map.put(key, value, merge));
    }

    /**
     * Complexity: effectively O(1), that of {@link HashMap#put(Tuple2, BiFunction)}.
     *
     * @param entry An entry
     * @param merge Combines the current value of the entry's key, if any, with the entry's value
     * @param <U>   Type of the entry's value
     * @return this map with {@code entry}, or with the merged value if its key was present
     * @throws NullPointerException if an argument, the entry's key or value is null, or {@code merge} returns null
     */
    public <U extends V> NonEmptyMap<K, V> put(Tuple2<? extends K, U> entry, BiFunction<? super V, ? super U, ? extends V> merge) {
        Objects.requireNonNull(entry, "NonEmptyMap.put: entry is null");
        return put(entry._1(), entry._2(), merge);
    }

    /**
     * Accepts the possibly empty type and returns the non-empty one; of two entries with the same key, {@code that}'s
     * value wins only where this map has none.
     *
     * @param that A map, possibly empty
     * @return the entries of both maps, this map's values kept on shared keys
     * @throws NullPointerException if {@code that} is null
     */
    public NonEmptyMap<K, V> merge(Map<? extends K, ? extends V> that) { return wrap(map.merge(that)); }

    /**
     * @param that                A map, possibly empty
     * @param collisionResolution Combines the two values of a shared key
     * @param <U>                 Value type of {@code that}
     * @return the entries of both maps, shared keys mapped to the combined value
     * @throws NullPointerException if an argument is null
     */
    public <U extends V> NonEmptyMap<K, V> merge(Map<? extends K, U> that, BiFunction<? super V, ? super U, ? extends V> collisionResolution) {
        return wrap(map.merge(that, collisionResolution));
    }

    /**
     * @param key             A key
     * @param mappingFunction Computes the value of an absent key
     * @return the value of {@code key}, computed if it was absent, and this map with it
     * @throws NullPointerException if {@code mappingFunction} is null or returns null
     */
    public Tuple2<V, NonEmptyMap<K, V>> computeIfAbsent(K key, Function<? super K, ? extends V> mappingFunction) {
        final Tuple2<V, HashMap<K, V>> result = map.computeIfAbsent(key, mappingFunction);
        return Tuple.of(result._1(), wrap(result._2()));
    }

    /**
     * @param key               A key
     * @param remappingFunction Computes the new value of a present key from its current value
     * @return the new value of {@code key}, if it was present, and this map with it
     * @throws NullPointerException if {@code remappingFunction} is null or returns null
     */
    public Tuple2<Option<V>, NonEmptyMap<K, V>> computeIfPresent(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
        final Tuple2<Option<V>, HashMap<K, V>> result = map.computeIfPresent(key, remappingFunction);
        return Tuple.of(result._1(), wrap(result._2()));
    }

    /**
     * Maps every entry; of two results with the same key, the later one wins, so the result may have fewer entries,
     * never none.
     *
     * @param mapper Maps a key and its value to an entry
     * @param <K2>   Key type of the result
     * @param <V2>   Value type of the result
     * @return the mapped entries
     * @throws NullPointerException if {@code mapper} is null or returns null
     */
    public <K2 extends @Nullable Object, V2 extends @Nullable Object> NonEmptyMap<K2, V2> map(BiFunction<? super K, ? super V, Tuple2<K2, V2>> mapper) {
        return new NonEmptyMap<>(map.map(mapper));
    }

    /**
     * @param keyMapper   Maps a key
     * @param valueMapper Maps a value
     * @param <K2>        Key type of the result
     * @param <V2>        Value type of the result
     * @return the mapped entries; of two with the same mapped key, the later one wins
     * @throws NullPointerException if an argument is null or returns null
     */
    public <K2 extends @Nullable Object, V2 extends @Nullable Object> NonEmptyMap<K2, V2> mapBoth(Function<? super K, ? extends K2> keyMapper, Function<? super V, ? extends V2> valueMapper) {
        return new NonEmptyMap<>(map.mapBoth(keyMapper, valueMapper));
    }

    /**
     * @param keyMapper Maps a key
     * @param <K2>      Key type of the result
     * @return the entries with mapped keys; of two with the same mapped key, the later one wins
     * @throws NullPointerException if {@code keyMapper} is null or returns null
     */
    public <K2 extends @Nullable Object> NonEmptyMap<K2, V> mapKeys(Function<? super K, ? extends K2> keyMapper) {
        return new NonEmptyMap<>(map.mapKeys(keyMapper));
    }

    /**
     * @param keyMapper  Maps a key
     * @param valueMerge Combines the values of two keys mapped to the same key
     * @param <K2>       Key type of the result
     * @return the entries with mapped keys
     * @throws NullPointerException if an argument is null or returns null
     */
    public <K2 extends @Nullable Object> NonEmptyMap<K2, V> mapKeys(Function<? super K, ? extends K2> keyMapper, BiFunction<? super V, ? super V, ? extends V> valueMerge) {
        return new NonEmptyMap<>(map.mapKeys(keyMapper, valueMerge));
    }

    /**
     * @param valueMapper Maps a value
     * @param <V2>        Value type of the result
     * @return the same keys, with mapped values
     * @throws NullPointerException if {@code valueMapper} is null or returns null
     */
    public <V2 extends @Nullable Object> NonEmptyMap<K, V2> mapValues(Function<? super V, ? extends V2> valueMapper) {
        return new NonEmptyMap<>(map.mapValues(valueMapper));
    }

    /**
     * The entries of the non-empty maps {@code mapper} returns for each entry; of two with the same key, the later one
     * wins. For a function that returns any {@code Iterable} of entries, see {@link #flatMapAll(BiFunction)}.
     *
     * @param mapper Maps a key and its value to a non-empty map
     * @param <K2>   Key type of the result
     * @param <V2>   Value type of the result
     * @return the entries of the results
     * @throws NullPointerException if {@code mapper} is null or returns null
     */
    public <K2 extends @Nullable Object, V2 extends @Nullable Object> NonEmptyMap<K2, V2> flatMap(BiFunction<? super K, ? super V, ? extends NonEmptyMap<? extends K2, ? extends V2>> mapper) {
        Objects.requireNonNull(mapper, "mapper is null");
        final HashMap.Builder<K2, V2> builder = HashMap.newBuilder();
        for (Tuple2<K, V> entry : map) {
            builder.putAll(Objects.requireNonNull(mapper.apply(entry._1(), entry._2()), "NonEmptyMap.flatMap: mapper returned null").map);
        }
        return new NonEmptyMap<>(builder.result());
    }

    /**
     * Complexity: effectively O(1), that of {@link HashMap#replace(Tuple2, Tuple2)}.
     *
     * @param currentElement An entry
     * @param newElement     Its replacement
     * @return this map with {@code newElement} instead of {@code currentElement}, or this map if {@code currentElement}
     *         is absent
     * @throws NullPointerException if {@code newElement}, its key or its value is null
     */
    public NonEmptyMap<K, V> replace(Tuple2<K, V> currentElement, Tuple2<K, V> newElement) {
        Objects.requireNonNull(newElement, "NonEmptyMap.replace: newElement is null");
        Objects.requireNonNull(newElement._1(), "NonEmptyMap.replace: key is null");
        Objects.requireNonNull(newElement._2(), "NonEmptyMap.replace: value is null");
        return wrap(map.replace(currentElement, newElement));
    }

    /**
     * Complexity: effectively O(1), that of {@link #replace(Tuple2, Tuple2)}: a map holds an entry once.
     *
     * @param currentElement An entry
     * @param newElement     Its replacement
     * @return the same as {@link #replace(Tuple2, Tuple2)}
     * @throws NullPointerException if {@code newElement}, its key or its value is null
     */
    public NonEmptyMap<K, V> replaceAll(Tuple2<K, V> currentElement, Tuple2<K, V> newElement) { return replace(currentElement, newElement); }

    /**
     * Complexity: effectively O(1), that of {@link HashMap#replace(Object, Object, Object)}.
     *
     * @param key      A key
     * @param oldValue Its expected value
     * @param newValue Its new value
     * @return this map with {@code key} mapped to {@code newValue} if it was mapped to {@code oldValue}, or this map
     * @throws NullPointerException if {@code newValue} is null
     */
    public NonEmptyMap<K, V> replace(K key, V oldValue, V newValue) {
        Objects.requireNonNull(newValue, "NonEmptyMap.replace: newValue is null");
        return wrap(map.replace(key, oldValue, newValue));
    }

    /**
     * Complexity: O(n), that of {@link HashMap#replaceAll(BiFunction)}.
     *
     * @param function Computes the new value of a key from its current value
     * @return the same keys, with the new values
     * @throws NullPointerException if {@code function} is null or returns null
     */
    public NonEmptyMap<K, V> replaceAll(BiFunction<? super K, ? super V, ? extends V> function) { return wrap(map.replaceAll(function)); }

    /**
     * @param key   A key
     * @param value Its new value
     * @return this map with {@code key} mapped to {@code value} if it was present, or this map
     * @throws NullPointerException if {@code value} is null
     */
    public NonEmptyMap<K, V> replaceValue(K key, V value) {
        Objects.requireNonNull(value, "NonEmptyMap.replaceValue: value is null");
        return wrap(map.replaceValue(key, value));
    }

    /**
     * Runs {@code action} on every entry.
     *
     * @param action A side effect
     * @return this map
     * @throws NullPointerException if {@code action} is null
     */
    public NonEmptyMap<K, V> tap(Consumer<? super Tuple2<K, V>> action) {
        map.tap(action);
        return this;
    }

    /**
     * Complexity: O(n), the keys copied into a new set, that of {@link HashMap#keySet()}.
     *
     * @return the keys, a non-empty set
     */
    public NonEmptySet<K> keySet() {
        final Set<K> keys = map.keySet();
        return NonEmptySet.unsafeFromSet(keys instanceof HashSet<K> hashSet ? hashSet : HashSet.ofAll(keys));
    }

    /**
     * Complexity: O(n), that of {@link HashMap#values()}.
     *
     * @return the values, one per entry, in iteration order
     */
    public NonEmptyVector<V> values() { return NonEmptyVector.unsafeFromVector(map.values()); }

    /**
     * Groups the entries by the key {@code classifier} computes.
     *
     * @param classifier Computes the group of an entry
     * @param <C>        Group key type
     * @return the groups, each non-empty
     * @throws NullPointerException if {@code classifier} is null or returns null
     */
    public <C extends @Nullable Object> HashMap<C, NonEmptyMap<K, V>> groupBy(Function<? super Tuple2<K, V>, ? extends C> classifier) {
        final HashMap.Builder<C, NonEmptyMap<K, V>> groups = HashMap.newBuilder();
        for (Tuple2<C, HashMap<K, V>> group : map.<C> groupBy(classifier)) {
            groups.put(group._1(), new NonEmptyMap<>(group._2()));
        }
        return groups.result();
    }

    /* the plain operations that cannot empty a non-empty map return the same instance when nothing changes */
    private NonEmptyMap<K, V> wrap(HashMap<K, V> result) { return result == map ? this : new NonEmptyMap<>(result); }

    // -- returns HashMap: the result may be empty

    /**
     * Complexity: O(1).
     *
     * @return the wrapped map
     */
    public HashMap<K, V> toMap() { return map; }

    /**
     * @param predicate A test of a key and its value
     * @return the entries that pass {@code predicate}
     * @throws NullPointerException if {@code predicate} is null
     */
    public HashMap<K, V> filter(BiPredicate<? super K, ? super V> predicate) { return map.filter(predicate); }

    /**
     * @param predicate A test of an entry
     * @return the entries that pass {@code predicate}
     * @throws NullPointerException if {@code predicate} is null
     */
    public HashMap<K, V> filter(Predicate<? super Tuple2<K, V>> predicate) { return map.filter(predicate); }

    /**
     * @param predicate A test of a key
     * @return the entries whose key passes {@code predicate}
     * @throws NullPointerException if {@code predicate} is null
     */
    public HashMap<K, V> filterKeys(Predicate<? super K> predicate) { return map.filterKeys(predicate); }

    /**
     * @param predicate A test of a value
     * @return the entries whose value passes {@code predicate}
     * @throws NullPointerException if {@code predicate} is null
     */
    public HashMap<K, V> filterValues(Predicate<? super V> predicate) { return map.filterValues(predicate); }

    /**
     * @param predicate A test of a key and its value
     * @return the entries that fail {@code predicate}
     * @throws NullPointerException if {@code predicate} is null
     */
    public HashMap<K, V> reject(BiPredicate<? super K, ? super V> predicate) { return map.reject(predicate); }

    /**
     * @param predicate A test of an entry
     * @return the entries that fail {@code predicate}
     * @throws NullPointerException if {@code predicate} is null
     */
    public HashMap<K, V> reject(Predicate<? super Tuple2<K, V>> predicate) { return map.reject(predicate); }

    /**
     * @param predicate A test of a key
     * @return the entries whose key fails {@code predicate}
     * @throws NullPointerException if {@code predicate} is null
     */
    public HashMap<K, V> rejectKeys(Predicate<? super K> predicate) { return map.rejectKeys(predicate); }

    /**
     * @param predicate A test of a value
     * @return the entries whose value fails {@code predicate}
     * @throws NullPointerException if {@code predicate} is null
     */
    public HashMap<K, V> rejectValues(Predicate<? super V> predicate) { return map.rejectValues(predicate); }

    /**
     * Maps and filters in one pass: keeps the {@code Some} results.
     *
     * @param mapper Maps a key and its value to an optional entry
     * @param <K2>   Key type of the result
     * @param <V2>   Value type of the result
     * @return the defined results
     * @throws NullPointerException if {@code mapper} is null or returns null
     */
    public <K2 extends @Nullable Object, V2 extends @Nullable Object> HashMap<K2, V2> collect(BiFunction<? super K, ? super V, ? extends Option<? extends Tuple2<K2, V2>>> mapper) {
        return map.collect(mapper);
    }

    /**
     * The entries of the iterables {@code mapper} returns; they may be empty, so the result may be too. For a function
     * that returns a {@code NonEmptyMap}, see {@link #flatMap(BiFunction)}.
     *
     * @param mapper Maps a key and its value to entries
     * @param <K2>   Key type of the result
     * @param <V2>   Value type of the result
     * @return the entries of the results
     * @throws NullPointerException if {@code mapper} is null or returns null
     */
    public <K2 extends @Nullable Object, V2 extends @Nullable Object> HashMap<K2, V2> flatMapAll(BiFunction<? super K, ? super V, ? extends Iterable<Tuple2<K2, V2>>> mapper) {
        return map.flatMap(mapper);
    }

    /**
     * Complexity: effectively O(1), that of {@link HashMap#remove(Object)}.
     *
     * @param key A key
     * @return this map without {@code key}
     */
    public HashMap<K, V> remove(K key) { return map.remove(key); }

    /**
     * Complexity: O(m) for m given keys, that of {@link HashMap#removeAll(Iterable)}.
     *
     * @param keys Keys
     * @return this map without {@code keys}
     * @throws NullPointerException if {@code keys} is null
     */
    public HashMap<K, V> removeAll(Iterable<? extends K> keys) { return map.removeAll(keys); }

    /**
     * Complexity: O(m) for m given entries, that of {@link HashMap#retainAll(Iterable)}.
     *
     * @param elements Entries
     * @return the entries of this map that are among {@code elements}
     * @throws NullPointerException if {@code elements} is null
     */
    public HashMap<K, V> retainAll(Iterable<? extends Tuple2<K, V>> elements) { return map.retainAll(elements); }

    /**
     * @param predicate A test of an entry
     * @return the entries that pass {@code predicate} and those that fail it; either may be empty
     * @throws NullPointerException if {@code predicate} is null
     */
    public Tuple2<HashMap<K, V>, HashMap<K, V>> partition(Predicate<? super Tuple2<K, V>> predicate) { return map.partition(predicate); }

    // -- total: what is partial on a HashMap

    /**
     * The greatest entry in the natural order of the entries, as {@link HashMap#max()}.
     * <p>
     * Complexity: O(n), every entry compared once.
     *
     * @return the greatest entry
     * @throws ClassCastException if the keys or values are not {@link Comparable}
     */
    public Tuple2<K, V> max() { return NonEmptyModule.max(map); }

    /**
     * The least entry in the natural order of the entries, as {@link HashMap#min()}.
     * <p>
     * Complexity: O(n), every entry compared once.
     *
     * @return the least entry
     * @throws ClassCastException if the keys or values are not {@link Comparable}
     */
    public Tuple2<K, V> min() { return NonEmptyModule.min(map); }

    /**
     * @param comparator The order
     * @return the greatest entry under {@code comparator}; of several, the first in iteration order
     * @throws NullPointerException if {@code comparator} is null
     */
    public Tuple2<K, V> maxBy(Comparator<? super Tuple2<K, V>> comparator) { return NonEmptyModule.max(map, comparator); }

    /**
     * @param comparator The order
     * @return the least entry under {@code comparator}; of several, the first in iteration order
     * @throws NullPointerException if {@code comparator} is null
     */
    public Tuple2<K, V> minBy(Comparator<? super Tuple2<K, V>> comparator) { return NonEmptyModule.min(map, comparator); }

    /**
     * @param f   Computes the key of an entry, once per entry
     * @param <U> Key type
     * @return the entry with the greatest key; of several, the first in iteration order
     * @throws NullPointerException if {@code f} is null
     */
    public <U extends Comparable<? super U>> Tuple2<K, V> maxBy(Function<? super Tuple2<K, V>, ? extends U> f) { return NonEmptyModule.maxBy(map, f); }

    /**
     * @param f   Computes the key of an entry, once per entry
     * @param <U> Key type
     * @return the entry with the least key; of several, the first in iteration order
     * @throws NullPointerException if {@code f} is null
     */
    public <U extends Comparable<? super U>> Tuple2<K, V> minBy(Function<? super Tuple2<K, V>, ? extends U> f) { return NonEmptyModule.minBy(map, f); }

    /**
     * Combines the entries with {@code op} in iteration order, which a hash map does not define: {@code op} should be
     * associative and commutative for the result to be independent of it.
     *
     * @param op Combines two entries
     * @return the combined entries
     * @throws NullPointerException if {@code op} is null
     */
    public Tuple2<K, V> reduce(BiFunction<? super Tuple2<K, V>, ? super Tuple2<K, V>, ? extends Tuple2<K, V>> op) { return NonEmptyModule.reduce(map, op); }

    /**
     * Maps every entry and combines the results, in one pass and in iteration order: {@code op} should be associative
     * and commutative.
     *
     * @param mapper Maps an entry
     * @param op     Combines two mapped values
     * @param <B>    Result type
     * @return the combined mapped values
     * @throws NullPointerException if {@code mapper} or {@code op} is null
     */
    public <B extends @Nullable Object> B reduceMap(Function<? super Tuple2<K, V>, ? extends B> mapper, BiFunction<? super B, ? super B, ? extends B> op) {
        return NonEmptyModule.reduceMap(map, mapper, op);
    }

    /**
     * @param zero    The neutral element of {@code combine}
     * @param combine Combines two entries; it should be associative and commutative
     * @return the entries folded in iteration order, starting from {@code zero}
     * @throws NullPointerException if {@code combine} is null
     */
    public Tuple2<K, V> fold(Tuple2<K, V> zero, BiFunction<? super Tuple2<K, V>, ? super Tuple2<K, V>, ? extends Tuple2<K, V>> combine) {
        return map.fold(zero, combine);
    }

    /**
     * @return the only entry
     * @throws java.util.NoSuchElementException if there is more than one entry
     */
    public Tuple2<K, V> single() { return map.single(); }

    /**
     * @return the number of entries, at least 1
     */
    public int size() { return map.size(); }

    /**
     * Complexity: effectively O(1), that of {@link HashMap#get(Object)}.
     *
     * @param key A key
     * @return the value of {@code key}, if present
     */
    public Option<V> get(K key) { return map.get(key); }

    /**
     * @param key          A key
     * @param defaultValue The result when {@code key} is absent
     * @return the value of {@code key}, or {@code defaultValue}
     */
    public V getOrElse(K key, V defaultValue) { return map.getOrElse(key, defaultValue); }

    /**
     * Complexity: effectively O(1), that of {@link HashMap#containsKey(Object)}.
     *
     * @param key A key
     * @return whether {@code key} is present
     */
    public boolean containsKey(K key) { return map.containsKey(key); }

    /**
     * @param value A value
     * @return whether a key is mapped to {@code value}; O(n)
     */
    public boolean containsValue(V value) { return map.containsValue(value); }

    /**
     * Complexity: effectively O(1), that of {@link HashMap#contains(Tuple2)}.
     *
     * @param element An entry
     * @return whether this map has {@code element}'s key mapped to {@code element}'s value
     */
    public boolean contains(Tuple2<K, V> element) { return map.contains(element); }

    /**
     * @param elements Entries
     * @return whether every one of {@code elements} is in this map
     * @throws NullPointerException if {@code elements} is null
     */
    public boolean containsAll(Iterable<? extends Tuple2<K, V>> elements) { return map.containsAll(elements); }

    /**
     * @param predicate A test
     * @return whether at least one entry passes {@code predicate}
     * @throws NullPointerException if {@code predicate} is null
     */
    public boolean exists(Predicate<? super Tuple2<K, V>> predicate) { return map.exists(predicate); }

    /**
     * @param predicate A test
     * @return whether exactly one entry passes {@code predicate}
     * @throws NullPointerException if {@code predicate} is null
     */
    public boolean existsUnique(Predicate<? super Tuple2<K, V>> predicate) { return map.existsUnique(predicate); }

    /**
     * @param predicate A test
     * @return whether every entry passes {@code predicate}
     * @throws NullPointerException if {@code predicate} is null
     */
    public boolean forAll(Predicate<? super Tuple2<K, V>> predicate) { return map.forAll(predicate); }

    /**
     * @param predicate A test
     * @return how many entries pass {@code predicate}
     * @throws NullPointerException if {@code predicate} is null
     */
    public int count(Predicate<? super Tuple2<K, V>> predicate) { return map.count(predicate); }

    /**
     * @param zero The initial accumulator
     * @param f    Combines the accumulator and an entry
     * @param <B>  Accumulator type
     * @return the entries folded in iteration order
     * @throws NullPointerException if {@code f} is null
     */
    public <B extends @Nullable Object> B foldLeft(B zero, BiFunction<? super B, ? super Tuple2<K, V>, ? extends B> f) {
        return map.foldLeft(zero, f);
    }

    /**
     * Runs {@code action} on every key and its value.
     *
     * @param action A side effect
     * @throws NullPointerException if {@code action} is null
     */
    public void forEach(BiConsumer<K, V> action) { map.forEach(action); }

    /**
     * @return the entries' {@code toString()}s, concatenated
     */
    public String mkString() { return map.mkString(); }

    /**
     * @param delimiter Put between entries
     * @return the entries' {@code toString()}s, joined by {@code delimiter}
     */
    public String mkString(CharSequence delimiter) { return map.mkString(delimiter); }

    /**
     * @param prefix    Put first
     * @param delimiter Put between entries
     * @param suffix    Put last
     * @return the entries' {@code toString()}s, joined by {@code delimiter}, between {@code prefix} and {@code suffix}
     */
    public String mkString(CharSequence prefix, CharSequence delimiter, CharSequence suffix) {
        return map.mkString(prefix, delimiter, suffix);
    }

    /**
     * @param collector A collector
     * @param <R>       Result type
     * @param <A>       The collector's accumulation type
     * @return the entries collected, as {@code stream().collect(collector)} does
     * @throws NullPointerException if {@code collector} is null
     */
    public <R extends @Nullable Object, A extends @Nullable Object> R collect(Collector<? super Tuple2<K, V>, A, R> collector) {
        return map.collect(collector);
    }

    /**
     * @param supplier    Makes a new result container
     * @param accumulator Adds an entry to a container
     * @param combiner    Merges two containers
     * @param <R>         Result type
     * @return the entries collected, as {@code stream().collect(supplier, accumulator, combiner)} does
     * @throws NullPointerException if an argument is null
     */
    public <R extends @Nullable Object> R collect(Supplier<R> supplier, BiConsumer<R, ? super Tuple2<K, V>> accumulator, BiConsumer<R, R> combiner) {
        return map.collect(supplier, accumulator, combiner);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Complexity: O(1) to create; a whole walk is O(n), that of {@link HashMap#iterator()}.
     */
    @Override
    public java.util.Iterator<Tuple2<K, V>> iterator() { return map.iterator(); }

    /**
     * @return the wrapped map's spliterator
     */
    @Override
    public Spliterator<Tuple2<K, V>> spliterator() { return map.spliterator(); }

    /**
     * @return a sequential {@link java.util.stream.Stream} over the entries
     */
    public java.util.stream.Stream<Tuple2<K, V>> stream() { return map.stream(); }

    /**
     * An unmodifiable {@link java.util.Collection} view of the entries, the one {@link HashMap#asJava()} gives.
     * <p>
     * Complexity: O(1).
     *
     * @return an unmodifiable view of the entries
     */
    public java.util.Collection<Tuple2<K, V>> asJava() { return map.asJava(); }

    /**
     * An unmodifiable {@link java.util.Map} view of the entries, the one {@link HashMap#asJavaMap()} gives: nothing is
     * copied and every mutator of the view throws {@link UnsupportedOperationException}.
     * <p>
     * Complexity: O(1); {@code get} and {@code containsKey} on the view are effectively O(1).
     *
     * @return an unmodifiable {@code java.util.Map} view
     */
    public java.util.Map<K, V> asJavaMap() { return map.asJavaMap(); }

    /**
     * @return the entries in a new array
     */
    public Object[] toArray() { return map.toArray(); }

    /**
     * @param arrayFactory Makes an array of the given length
     * @return the entries in a new array made by {@code arrayFactory}
     * @throws NullPointerException if {@code arrayFactory} is null
     */
    public Tuple2<K, V>[] toArray(IntFunction<Tuple2<K, V>[]> arrayFactory) { return map.toArray(arrayFactory); }

    /**
     * @return the entries as a {@link Vector}, in iteration order
     */
    public Vector<Tuple2<K, V>> toVector() { return map.toVector(); }

    /**
     * @return the entries as a {@link List}, in iteration order
     */
    public List<Tuple2<K, V>> toList() { return map.toList(); }

    /**
     * @return the entries as a {@link Queue}, in iteration order
     */
    public Queue<Tuple2<K, V>> toQueue() { return map.toQueue(); }

    /**
     * @return the entries as a {@link Stream}, in iteration order
     */
    public Stream<Tuple2<K, V>> toStream() { return map.toStream(); }

    /**
     * @return the entries as a {@link HashSet}
     */
    public Set<Tuple2<K, V>> toSet() { return map.toSet(); }

    /**
     * @return the entries as a {@link LinkedHashSet}, in iteration order
     */
    public Set<Tuple2<K, V>> toLinkedSet() { return map.toLinkedSet(); }

    /**
     * @return the entries as a {@link TreeSet} in their natural order
     * @throws ClassCastException if the keys or values are not {@link Comparable}
     */
    public SortedSet<Tuple2<K, V>> toSortedSet() { return map.toSortedSet(); }

    /**
     * @param comparator The order
     * @return the entries as a {@link TreeSet} ordered by {@code comparator}
     * @throws NullPointerException if {@code comparator} is null
     */
    public SortedSet<Tuple2<K, V>> toSortedSet(Comparator<? super Tuple2<K, V>> comparator) { return map.toSortedSet(comparator); }

    /**
     * @param keyMapper   The key of an entry
     * @param valueMapper The value of an entry
     * @param <K2>        Key type
     * @param <V2>        Value type
     * @return the entries as the entries of a new {@link HashMap}; of two with the same key, the later wins
     * @throws NullPointerException if an argument is null or returns null
     */
    public <K2 extends @Nullable Object, V2 extends @Nullable Object> Map<K2, V2> toMap(Function<? super Tuple2<K, V>, ? extends K2> keyMapper, Function<? super Tuple2<K, V>, ? extends V2> valueMapper) {
        return map.toMap(keyMapper, valueMapper);
    }

    /**
     * @param f    The new entry of an entry
     * @param <K2> Key type
     * @param <V2> Value type
     * @return the entries as the entries of a new {@link HashMap}; of two with the same key, the later wins
     * @throws NullPointerException if {@code f} is null or returns null
     */
    public <K2 extends @Nullable Object, V2 extends @Nullable Object> Map<K2, V2> toMap(Function<? super Tuple2<K, V>, ? extends Tuple2<? extends K2, ? extends V2>> f) {
        return map.toMap(f);
    }

    /**
     * @param keyMapper   The key of an entry
     * @param valueMapper The value of an entry
     * @param <K2>        Key type
     * @param <V2>        Value type
     * @return the entries as the entries of a new {@link LinkedHashMap}, in iteration order
     * @throws NullPointerException if an argument is null or returns null
     */
    public <K2 extends @Nullable Object, V2 extends @Nullable Object> Map<K2, V2> toLinkedMap(Function<? super Tuple2<K, V>, ? extends K2> keyMapper, Function<? super Tuple2<K, V>, ? extends V2> valueMapper) {
        return map.toLinkedMap(keyMapper, valueMapper);
    }

    /**
     * @param f    The new entry of an entry
     * @param <K2> Key type
     * @param <V2> Value type
     * @return the entries as the entries of a new {@link LinkedHashMap}, in iteration order
     * @throws NullPointerException if {@code f} is null or returns null
     */
    public <K2 extends @Nullable Object, V2 extends @Nullable Object> Map<K2, V2> toLinkedMap(Function<? super Tuple2<K, V>, ? extends Tuple2<? extends K2, ? extends V2>> f) {
        return map.toLinkedMap(f);
    }

    /**
     * @param keyMapper   The key of an entry
     * @param valueMapper The value of an entry
     * @param <K2>        Key type
     * @param <V2>        Value type
     * @return the entries as the entries of a new {@link TreeMap} in the natural order of the keys
     * @throws NullPointerException if an argument is null or returns null
     */
    public <K2 extends Comparable<? super K2>, V2 extends @Nullable Object> SortedMap<K2, V2> toSortedMap(Function<? super Tuple2<K, V>, ? extends K2> keyMapper, Function<? super Tuple2<K, V>, ? extends V2> valueMapper) {
        return map.toSortedMap(keyMapper, valueMapper);
    }

    /**
     * @param f    The new entry of an entry
     * @param <K2> Key type
     * @param <V2> Value type
     * @return the entries as the entries of a new {@link TreeMap} in the natural order of the keys
     * @throws NullPointerException if {@code f} is null or returns null
     */
    public <K2 extends Comparable<? super K2>, V2 extends @Nullable Object> SortedMap<K2, V2> toSortedMap(Function<? super Tuple2<K, V>, ? extends Tuple2<? extends K2, ? extends V2>> f) {
        return map.toSortedMap(f);
    }

    /**
     * @param comparator  The order of the keys
     * @param keyMapper   The key of an entry
     * @param valueMapper The value of an entry
     * @param <K2>        Key type
     * @param <V2>        Value type
     * @return the entries as the entries of a new {@link TreeMap} ordered by {@code comparator}
     * @throws NullPointerException if an argument is null or a mapper returns null
     */
    public <K2 extends @Nullable Object, V2 extends @Nullable Object> SortedMap<K2, V2> toSortedMap(Comparator<? super K2> comparator, Function<? super Tuple2<K, V>, ? extends K2> keyMapper, Function<? super Tuple2<K, V>, ? extends V2> valueMapper) {
        return map.toSortedMap(comparator, keyMapper, valueMapper);
    }

    /**
     * @param comparator The order of the keys
     * @param f          The new entry of an entry
     * @param <K2>       Key type
     * @param <V2>       Value type
     * @return the entries as the entries of a new {@link TreeMap} ordered by {@code comparator}
     * @throws NullPointerException if an argument is null or {@code f} returns null
     */
    public <K2 extends @Nullable Object, V2 extends @Nullable Object> SortedMap<K2, V2> toSortedMap(Comparator<? super K2> comparator, Function<? super Tuple2<K, V>, ? extends Tuple2<? extends K2, ? extends V2>> f) {
        return map.toSortedMap(comparator, f);
    }

    // -- returns Option

    /**
     * @param predicate A test
     * @return an entry that passes {@code predicate}, the first in iteration order
     * @throws NullPointerException if {@code predicate} is null
     */
    public Option<Tuple2<K, V>> find(Predicate<? super Tuple2<K, V>> predicate) { return map.find(predicate); }

    /**
     * Arranges the entries by a key that must be unique.
     *
     * @param getKey The key of an entry
     * @param <K2>   Key type
     * @return {@code Some} of the map from each key to its entry, or {@code None} if two entries share a key
     * @throws NullPointerException if {@code getKey} is null
     */
    public <K2 extends @Nullable Object> Option<Map<K2, Tuple2<K, V>>> arrangeBy(Function<? super Tuple2<K, V>, ? extends K2> getKey) {
        return map.arrangeBy(getKey);
    }

    // -- Object

    /**
     * Maps are equal to maps: a {@code NonEmptyMap} is equal to a {@code NonEmptyMap} or a {@code NonEmptySortedMap}
     * with the same entries, and never to a plain {@link Map}.
     */
    @Override
    public boolean equals(@Nullable Object o) {
        return o == this
                || (o instanceof NonEmptyMap<?, ?> that && map.equals(that.map))
                || (o instanceof NonEmptySortedMap<?, ?> sorted && map.equals(sorted.toSortedMap()));
    }

    @Override
    public int hashCode() { return map.hashCode(); }

    @Override
    public String toString() { return map.mkString("NonEmptyMap(", ", ", ")"); }
}
