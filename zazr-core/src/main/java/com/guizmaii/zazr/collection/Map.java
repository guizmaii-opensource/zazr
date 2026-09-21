package com.guizmaii.zazr.collection;

import com.guizmaii.zazr.*;
import com.guizmaii.zazr.control.Option;
import java.util.*;
import java.util.function.*;
import java.util.stream.Collector;
import java.util.stream.StreamSupport;
import org.jspecify.annotations.Nullable;

/**
 * An immutable {@code Map} interface.
 *
 * <p>
 * Represents a collection of key-value pairs with immutable operations.
 * Supports typical map operations such as querying, updating, filtering,
 * transforming, and iterating over entries. Provides convenient methods
 * for converting to standard Java maps.
 *
 * @param <K> Key type
 * @param <V> Value type
 * @author Daniel Dietrich, Ruslan Sennov, Grzegorz Piwowarek
 */
public interface Map<K extends @Nullable Object, V extends @Nullable Object> extends Traversable<Tuple2<K, V>> {

    /**
     * Narrows a widened {@code Map<? extends K, ? extends V>} to {@code Map<K, V>}
     * by performing a type-safe cast. This is eligible because immutable/read-only
     * collections are covariant.
     *
     * @param map A {@code Map}.
     * @param <K> Key type
     * @param <V> Value type
     * @return the given {@code map} instance as narrowed type {@code Map<K, V>}.
     */
    @SuppressWarnings("unchecked")
    static <K extends @Nullable Object, V extends @Nullable Object> Map<K, V> narrow(Map<? extends K, ? extends V> map) {
        return (Map<K, V>) map;
    }

    /**
     * Creates a key/value pair for use with {@link Map} factories.
     *
     * <p>
     * When imported statically, this method enables constructing maps in a
     * readable and type-safe way, for example:
     * <pre>{@code
     * HashMap.ofEntries(
     *     entry(k1, v1),
     *     entry(k2, v2),
     *     entry(k3, v3)
     * );
     * }</pre>
     *
     * @param key   the entry's key
     * @param value the entry's value
     * @param <K>   key type
     * @param <V>   value type
     * @return a key/value pair
     */
    static <K extends @Nullable Object, V extends @Nullable Object> Tuple2<K, V> entry(K key, V value) {
        return Tuple.of(key, value);
    }

    /**
     * Maps this {@code Map} to a new {@code Map} with different component type by applying a function to its elements.
     *
     * @param <K2>        key's component type of the map result
     * @param <V2>        value's component type of the map result
     * @param keyMapper   a {@code Function} that maps the keys of type {@code K} to keys of type {@code K2}
     * @param valueMapper a {@code Function} that the values of type {@code V} to values of type {@code V2}
     * @return a new {@code Map}
     * @throws NullPointerException if {@code keyMapper} or {@code valueMapper} is null
     */
    <K2 extends @Nullable Object, V2 extends @Nullable Object> Map<K2, V2> mapBoth(Function<? super K, ? extends K2> keyMapper, Function<? super V, ? extends V2> valueMapper);

    @Override
    default boolean contains(Tuple2<K, V> element) {
        // getOrElse via the ABSENT sentinel, not get: avoids allocating a Some just to test isDefined()
        final V value = Maps.getOrAbsent(this, element._1());
        return value != Maps.ABSENT && Objects.equals(value, element._2());
    }

    /**
     * If the specified key is not already associated with a value,
     * attempts to compute its value using the given mapping
     * function and enters it into this map.
     *
     * @param key             key whose presence in this map is to be tested
     * @param mappingFunction mapping function
     * @return the {@link Tuple2} of the existing or computed value associated with the specified key, and the current or modified map
     */
    Tuple2<V, ? extends Map<K, V>> computeIfAbsent(K key, Function<? super K, ? extends V> mappingFunction);

    /**
     * If the value for the specified key is present, attempts to
     * compute a new mapping given the key and its current mapped value.
     *
     * @param key               key whose presence in this map is to be tested
     * @param remappingFunction remapping function
     * @return the {@link Tuple2} of the {@code Some} of the value associated with the specified key
     * (or {@code None} if none), and the current or modified map
     * @throws NullPointerException if the key is present and {@code remappingFunction} returns {@code null}: the new
     *                              value is handed back as {@code Some}, which cannot hold {@code null} (design 3.9)
     */
    Tuple2<Option<V>, ? extends Map<K, V>> computeIfPresent(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction);

    /**
     * Returns <code>true</code> if this map contains a mapping for the specified key.
     *
     * @param key key whose presence in this map is to be tested
     * @return <code>true</code> if this map contains a mapping for the specified key
     */
    boolean containsKey(K key);

    /**
     * Returns <code>true</code> if this map maps one or more keys to the
     * specified value. This operation will require time linear in the map size.
     *
     * @param value value whose presence in this map is to be tested
     * @return <code>true</code> if this map maps one or more keys to the
     * specified value
     */
    default boolean containsValue(V value) {
        for (Tuple2<K, V> entry : this) {
            if (Objects.equals(entry._2(), value)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns a new Map consisting of all elements which satisfy the given predicate.
     *
     * @param predicate the predicate used to test elements
     * @return a new Map
     * @throws NullPointerException if {@code predicate} is null
     */
    Map<K, V> filter(BiPredicate<? super K, ? super V> predicate);

    /**
     * Returns a new Map consisting of all elements which do not satisfy the given predicate.
     *
     * @param predicate the predicate used to test elements
     * @return a new Map
     * @throws NullPointerException if {@code predicate} is null
     */
    Map<K, V> reject(BiPredicate<? super K, ? super V> predicate);

    /**
     * Returns a new Map consisting of all elements with keys which satisfy the given predicate.
     *
     * @param predicate the predicate used to test keys of elements
     * @return a new Map
     * @throws NullPointerException if {@code predicate} is null
     */
    Map<K, V> filterKeys(Predicate<? super K> predicate);

    /**
     * Returns a new Map consisting of all elements with keys which do not satisfy the given predicate.
     *
     * @param predicate the predicate used to test keys of elements
     * @return a new Map
     * @throws NullPointerException if {@code predicate} is null
     */
    Map<K, V> rejectKeys(Predicate<? super K> predicate);

    /**
     * Returns a new Map consisting of all elements with values which satisfy the given predicate.
     *
     * @param predicate the predicate used to test values of elements
     * @return a new Map
     * @throws NullPointerException if {@code predicate} is null
     */
    Map<K, V> filterValues(Predicate<? super V> predicate);

    /**
     * Returns a new Map consisting of all elements with values which do not satisfy the given predicate.
     *
     * @param predicate the predicate used to test values of elements
     * @return a new Map
     * @throws NullPointerException if {@code predicate} is null
     */
    Map<K, V> rejectValues(Predicate<? super V> predicate);

    /**
     * FlatMaps this {@code Map} to a new {@code Map} with different component type.
     *
     * @param mapper A mapper
     * @param <K2>   key's component type of the mapped {@code Map}
     * @param <V2>   value's component type of the mapped {@code Map}
     * @return A new {@code Map}.
     * @throws NullPointerException if {@code mapper} is null
     */
    <K2 extends @Nullable Object, V2 extends @Nullable Object> Map<K2, V2> flatMap(BiFunction<? super K, ? super V, ? extends Iterable<Tuple2<K2, V2>>> mapper);

    /**
     * Performs an action on key, value pair.
     *
     * @param action A {@code BiConsumer}
     * @throws NullPointerException if {@code action} is null
     */
    default void forEach(BiConsumer<K, V> action) {
        Objects.requireNonNull(action, "action is null");
        for (Tuple2<K, V> t : this) {
            action.accept(t._1(), t._2());
        }
    }

    /**
     * Returns the {@code Some} of value to which the specified key
     * is mapped, or {@code None} if this map contains no mapping for the key.
     *
     * @param key the key whose associated value is to be returned
     * @return the {@code Some} of value to which the specified key
     * is mapped, or {@code None} if this map contains no mapping
     * for the key
     */
    Option<V> get(K key);

    /**
     * Returns the value associated with a key, or a default value if the key is not contained in the map.
     *
     * @param key          the key
     * @param defaultValue a default value
     * @return the value associated with key if it exists, otherwise the default value.
     */
    V getOrElse(K key, V defaultValue);

    /**
     * Returns the keys contained in this map.
     *
     * @return {@code Set} of the keys contained in this map.
     */
    com.guizmaii.zazr.collection.Set<K> keySet();

    /**
     * Matches and transforms the entries in one pass into a {@code Map} of the same kind: {@code mapper} returns
     * {@code Some} of the new entry for an entry it keeps and {@code None} for one it drops. Two kept entries with
     * the same new key keep the later one in iteration order, as {@link #map(BiFunction)} does.
     * <pre>{@code
     * Map<String, Integer> adults = ages.collect((name, age) -> age >= 18 ? Option.some(Tuple.of(name, age)) : Option.none());
     * }</pre>
     *
     * @param mapper a function from a key and a value to {@code Some} of the new entry or {@code None}; it must not
     *               return {@code null}
     * @param <K2>   the new key type
     * @param <V2>   the new value type
     * @return a {@code Map} of the collected entries
     * @throws NullPointerException if {@code mapper} is null, or if it returns {@code null} for an entry
     */
    <K2 extends @Nullable Object, V2 extends @Nullable Object> Map<K2, V2> collect(BiFunction<? super K, ? super V, ? extends Option<? extends Tuple2<K2, V2>>> mapper);

    /**
     * Maps the entries of this {@code Map} to form a new {@code Map}.
     *
     * @param <K2>   key's component type of the map result
     * @param <V2>   value's component type of the map result
     * @param mapper a {@code Function} that maps entries of type {@code (K, V)} to entries of type {@code (K2, V2)}
     * @return a new {@code Map}
     * @throws NullPointerException if {@code mapper} is null
     */
    <K2 extends @Nullable Object, V2 extends @Nullable Object> Map<K2, V2> map(BiFunction<? super K, ? super V, Tuple2<K2, V2>> mapper);

    /**
     * Maps the keys of this {@code Map} while preserving the corresponding values.
     * <p>
     * The size of the result map may be smaller if {@code keyMapper} maps two or more distinct keys to the same new key.
     * In this case the value at the {@code latest} of the original keys is retained.
     * Order of keys is predictable in {@code TreeMap} (by comparator) and {@code LinkedHashMap} (insertion-order) and not predictable in {@code HashMap}.
     *
     * @param <K2>      the new key type
     * @param keyMapper a {@code Function} that maps keys of type {@code K} to keys of type {@code K2}
     * @return a new {@code Map}
     * @throws NullPointerException if {@code keyMapper} is null
     */
    <K2 extends @Nullable Object> Map<K2, V> mapKeys(Function<? super K, ? extends K2> keyMapper);

    /**
     * Maps the keys of this {@code Map} while preserving the corresponding values and applying a value merge function on collisions.
     * <p>
     * The size of the result map may be smaller if {@code keyMapper} maps two or more distinct keys to the same new key.
     * In this case the associated values will be combined using {@code valueMerge}.
     *
     * @param <K2>       the new key type
     * @param keyMapper  a {@code Function} that maps keys of type {@code K} to keys of type {@code K2}
     * @param valueMerge a {@code BiFunction} that merges values
     * @return a new {@code Map}
     * @throws NullPointerException if {@code keyMapper} is null
     */
    <K2 extends @Nullable Object> Map<K2, V> mapKeys(Function<? super K, ? extends K2> keyMapper, BiFunction<? super V, ? super V, ? extends V> valueMerge);

    /**
     * Maps the values of this {@code Map} while preserving the corresponding keys.
     *
     * @param <V2>        the new value type
     * @param valueMapper a {@code Function} that maps values of type {@code V} to values of type {@code V2}
     * @return a new {@code Map}
     * @throws NullPointerException if {@code valueMapper} is null
     */
    <V2 extends @Nullable Object> Map<K, V2> mapValues(Function<? super V, ? extends V2> valueMapper);

    /**
     * Creates a new map which by merging the entries of {@code this} map and {@code that} map.
     * <p>
     * If collisions occur, the value of {@code this} map is taken.
     *
     * @param that the other map
     * @return A merged map
     * @throws NullPointerException if that map is null
     */
    Map<K, V> merge(Map<? extends K, ? extends V> that);

    /**
     * Creates a new map which by merging the entries of {@code this} map and {@code that} map.
     * <p>
     * Uses the specified collision resolution function if two keys are the same.
     * The collision resolution function will always take the first argument from <code>this</code> map
     * and the second from <code>that</code> map.
     *
     * @param <U>                 value type of that Map
     * @param that                the other map
     * @param collisionResolution the collision resolution function
     * @return A merged map
     * @throws NullPointerException if that map or the given collision resolution function is null
     */
    <U extends V> Map<K, V> merge(Map<? extends K, U> that, BiFunction<? super V, ? super U, ? extends V> collisionResolution);

    /**
     * Associates the specified value with the specified key in this map.
     * If the map previously contained a mapping for the key, the old value is
     * replaced by the specified value.
     *
     * @param key   key with which the specified value is to be associated
     * @param value value to be associated with the specified key
     * @return A new Map containing these elements and that entry.
     */
    Map<K, V> put(K key, V value);

    /**
     * Convenience method for {@code put(entry._1(), entry._2())}.
     *
     * @param entry A Tuple2 containing the key and value
     * @return A new Map containing these elements and that entry.
     */
    Map<K, V> put(Tuple2<? extends K, ? extends V> entry);

    /**
     * Associates the specified value with the specified key in this map.
     * If the map previously contained a mapping for the key, the merge
     * function is used to combine the previous value to the value to
     * be inserted, and the result of that call is inserted in the map.
     *
     * @param <U>   the value type
     * @param key   key with which the specified value is to be associated
     * @param value value to be associated with the specified key
     * @param merge function taking the old and new values and merging them.
     * @return A new Map containing these elements and that entry.
     */
    <U extends V> Map<K, V> put(K key, U value, BiFunction<? super V, ? super U, ? extends V> merge);

    /**
     * Convenience method for {@code put(entry._1(), entry._2(), merge)}.
     *
     * @param <U>   the value type
     * @param entry A Tuple2 containing the key and value
     * @param merge function taking the old and new values and merging them.
     * @return A new Map containing these elements and that entry.
     */
    <U extends V> Map<K, V> put(Tuple2<? extends K, U> entry, BiFunction<? super V, ? super U, ? extends V> merge);

    /**
     * Removes the mapping for a key from this map if it is present.
     *
     * @param key key whose mapping is to be removed from the map
     * @return A new Map containing these elements without the entry
     * specified by that key.
     */
    Map<K, V> remove(K key);

    /**
     * Returns a new Map consisting of all elements which do not satisfy the given predicate.
     *
     * @deprecated Please use {@link #reject(BiPredicate)}
     * @param predicate the predicate used to test elements
     * @return a new Map
     * @throws NullPointerException if {@code predicate} is null
     */
    @Deprecated
    Map<K, V> removeAll(BiPredicate<? super K, ? super V> predicate);

    /**
     * Removes the mapping for a key from this map if it is present.
     *
     * @param keys keys are to be removed from the map
     * @return A new Map containing these elements without the entries
     * specified by that keys.
     */
    Map<K, V> removeAll(Iterable<? extends K> keys);

    /**
     * Returns a new Map consisting of all elements with keys which do not satisfy the given predicate.
     *
     * @deprecated Please use {@link #rejectKeys(Predicate)}
     * @param predicate the predicate used to test keys of elements
     * @return a new Map
     * @throws NullPointerException if {@code predicate} is null
     */
    @Deprecated
    Map<K, V> removeKeys(Predicate<? super K> predicate);

    /**
     * Returns a new Map consisting of all elements with values which do not satisfy the given predicate.
     *
     * @deprecated Please use {@link #rejectValues(Predicate)}
     * @param predicate the predicate used to test values of elements
     * @return a new Map
     * @throws NullPointerException if {@code predicate} is null
     */
    @Deprecated
    Map<K, V> removeValues(Predicate<? super V> predicate);

    @Override
    int size();

    /**
     * Converts this Vavr {@code Map} to a {@code java.util.Map} while preserving characteristics
     * like insertion order ({@code LinkedHashMap}) and sort order ({@code SortedMap}).
     *
     * @return a new {@code java.util.Map} instance
     */
    java.util.Map<K, V> toJavaMap();

    /**
     * The values of this map as a {@link Vector}, in this map's iteration order; the same key order as
     * {@link #keySet()} on the ordered maps.
     *
     * <pre>{@code
     * // = Vector("a", "b", "c")
     * TreeMap.of(1, "a", 2, "b", 3, "c").values()
     * }</pre>
     *
     * @return the values
     */
    Vector<V> values();

    // -- the entry-wise operations a map keeps

    /**
     * The entries that satisfy {@code predicate}.
     *
     * @param predicate the condition to keep an entry
     * @return a map of the matching entries; may be this instance if all of them match
     * @throws NullPointerException if {@code predicate} is null
     */
    Map<K, V> filter(Predicate<? super Tuple2<K, V>> predicate);

    /**
     * The entries that do not satisfy {@code predicate}; the same as {@code filter(predicate.negate())}.
     *
     * @param predicate the condition to drop an entry
     * @return a map of the entries that do not match; may be this instance if none of them matches
     * @throws NullPointerException if {@code predicate} is null
     */
    Map<K, V> reject(Predicate<? super Tuple2<K, V>> predicate);

    /**
     * Groups the entries by the key {@code classifier} computes for each of them.
     *
     * @param classifier the group key of an entry
     * @param <C>        the group key type
     * @return a map from each group key to the map of the entries with that key
     * @throws NullPointerException if {@code classifier} is null
     */
    <C extends @Nullable Object> Map<C, ? extends Map<K, V>> groupBy(Function<? super Tuple2<K, V>, ? extends C> classifier);

    /**
     * This map if it is non-empty, otherwise a map of the entries of {@code other}.
     *
     * @param other the entries to fall back on
     * @return this map if non-empty, otherwise a map of {@code other}
     * @throws NullPointerException if this map is empty and {@code other} is null
     */
    Map<K, V> orElse(Iterable<? extends Tuple2<K, V>> other);

    /**
     * This map if it is non-empty, otherwise a map of the entries {@code supplier} provides; the supplier is only
     * called when this map is empty.
     *
     * @param supplier provides the entries to fall back on
     * @return this map if non-empty, otherwise a map of {@code supplier.get()}
     * @throws NullPointerException if this map is empty and {@code supplier} is null
     */
    Map<K, V> orElse(Supplier<? extends Iterable<? extends Tuple2<K, V>>> supplier);

    /**
     * Splits the entries into those that satisfy {@code predicate} and those that do not.
     *
     * @param predicate the condition
     * @return the matching entries and the others, as two maps
     * @throws NullPointerException if {@code predicate} is null
     */
    Tuple2<? extends Map<K, V>, ? extends Map<K, V>> partition(Predicate<? super Tuple2<K, V>> predicate);

    /**
     * Runs {@code action} on every entry and returns this map, to observe the entries in the middle of a chain of
     * calls. Whatever the action throws propagates to the caller.
     *
     * @param action what to do with each entry
     * @return this map
     * @throws NullPointerException if {@code action} is null
     */
    Map<K, V> tap(Consumer<? super Tuple2<K, V>> action);

    /**
     * Replaces the entry {@code currentElement}, if it is one of the entries, with {@code newElement}: the key of
     * {@code currentElement} is removed and {@code newElement} is put.
     *
     * @param currentElement the entry to replace
     * @param newElement     its replacement
     * @return a map with the replacement made; this map if {@code currentElement} is not an entry
     * @throws NullPointerException if an argument is null
     */
    Map<K, V> replace(Tuple2<K, V> currentElement, Tuple2<K, V> newElement);

    /**
     * Replaces the entry for the specified key only if it is currently mapped to some value.
     *
     * @param key   the key of the element to be substituted.
     * @param value the new value to be associated with the key
     * @return a new map containing key mapped to value if key was contained before. The old map otherwise.
     */
    Map<K, V> replaceValue(K key, V value);

    /**
     * Replaces the entry for the specified key only if currently mapped to the specified value.
     *
     * @param key      the key of the element to be substituted.
     * @param oldValue the expected current value that the key is currently mapped to
     * @param newValue the new value to be associated with the key
     * @return a new map containing key mapped to newValue if key was contained before and oldValue matched. The old map otherwise.
     */
    Map<K, V> replace(K key, V oldValue, V newValue);

    /**
     * Replaces each entry's value with the result of invoking the given function on that entry until all entries have been processed or the function throws an exception.
     *
     * @param function function transforming key and current value to a new value
     * @return a new map with the same keySet but transformed values.
     */
    Map<K, V> replaceAll(BiFunction<? super K, ? super V, ? extends V> function);

    /**
     * The same as {@link #replace(Tuple2, Tuple2)}: a map holds an entry at most once.
     *
     * @param currentElement the entry to replace
     * @param newElement     its replacement
     * @return a map with the replacement made; this map if {@code currentElement} is not an entry
     */
    Map<K, V> replaceAll(Tuple2<K, V> currentElement, Tuple2<K, V> newElement);

    /**
     * Keeps only the entries that are also in {@code elements}.
     *
     * @param elements the entries to keep
     * @return a map of the entries of this map that are in {@code elements}; may be this instance if all are
     * @throws NullPointerException if {@code elements} is null
     */
    Map<K, V> retainAll(Iterable<? extends Tuple2<K, V>> elements);

    // -- reductions and conversions

    /**
     * Whether exactly one element satisfies {@code predicate}.
     *
     * @param predicate the condition to test
     * @return {@code true} if one and only one element matches, {@code false} otherwise
     * @throws NullPointerException if {@code predicate} is null
     */
    default boolean existsUnique(Predicate<? super Tuple2<K, V>> predicate) {
        return TraversableModule.existsUnique(this, predicate);
    }

    /**
     * The greatest element in the natural order of the elements, which must be {@link Comparable}; the sort order
     * of a sorted collection is not consulted. {@code NaN} compares as the greatest {@code Double} or {@code Float}.
     *
     * @return {@code Some(maximum)} if there is an element, {@code None} otherwise
     * @throws ClassCastException if two or more elements are not {@code Comparable}
     */
    default Option<Tuple2<K, V>> max() {
        return TraversableModule.max(this);
    }

    /**
     * The greatest element according to {@code comparator}; of equal greatest elements, the first in this
     * Map's order.
     *
     * @param comparator the order
     * @return {@code Some(maximum)} if there is an element, {@code None} otherwise
     * @throws NullPointerException if {@code comparator} is null
     */
    default Option<Tuple2<K, V>> maxBy(Comparator<? super Tuple2<K, V>> comparator) {
        return TraversableModule.maxBy(this, comparator);
    }

    /**
     * The element whose key, computed once by {@code f}, is the greatest; of equal greatest keys, the first
     * element in this Map's order.
     *
     * @param f   the key of an element
     * @param <U> the key type
     * @return {@code Some(element)} if there is an element, {@code None} otherwise
     * @throws NullPointerException if {@code f} is null
     */
    default <U extends Comparable<? super U>> Option<Tuple2<K, V>> maxBy(Function<? super Tuple2<K, V>, ? extends U> f) {
        return TraversableModule.maxBy(this, f);
    }

    /**
     * The least element in the natural order of the elements, which must be {@link Comparable}; the sort order of
     * a sorted collection is not consulted. Among {@code Double}s or {@code Float}s, a {@code NaN} is the result
     * whenever one is present.
     *
     * @return {@code Some(minimum)} if there is an element, {@code None} otherwise
     * @throws ClassCastException if two or more elements are not {@code Comparable}
     */
    default Option<Tuple2<K, V>> min() {
        return TraversableModule.min(this);
    }

    /**
     * The least element according to {@code comparator}; of equal least elements, the first in this Map's
     * order.
     *
     * @param comparator the order
     * @return {@code Some(minimum)} if there is an element, {@code None} otherwise
     * @throws NullPointerException if {@code comparator} is null
     */
    default Option<Tuple2<K, V>> minBy(Comparator<? super Tuple2<K, V>> comparator) {
        return TraversableModule.minBy(this, comparator);
    }

    /**
     * The element whose key, computed once by {@code f}, is the least; of equal least keys, the first element in
     * this Map's order.
     *
     * @param f   the key of an element
     * @param <U> the key type
     * @return {@code Some(element)} if there is an element, {@code None} otherwise
     * @throws NullPointerException if {@code f} is null
     */
    default <U extends Comparable<? super U>> Option<Tuple2<K, V>> minBy(Function<? super Tuple2<K, V>, ? extends U> f) {
        return TraversableModule.minBy(this, f);
    }

    /**
     * Folds the elements with {@code combine}, starting from {@code zero}, which must be its neutral element.
     * The elements are combined in this Map's iteration order, which a {@code HashMap} does not define: {@code combine} should be associative and commutative for the result to be independent of it.
     *
     * @param zero    the neutral element of {@code combine}
     * @param combine combines two elements
     * @return the folded result, {@code zero} on an empty Map
     * @throws NullPointerException if {@code combine} is null
     */
    default Tuple2<K, V> fold(Tuple2<K, V> zero, BiFunction<? super Tuple2<K, V>, ? super Tuple2<K, V>, ? extends Tuple2<K, V>> combine) {
        Objects.requireNonNull(combine, "combine is null");
        return foldLeft(zero, combine);
    }

    /**
     * Combines the elements with {@code op}, each result with the next element. The elements are combined in this Map's iteration order, which a {@code HashMap} does not define: {@code op} should be associative and commutative for the result to be independent of it.
     *
     * @param op combines two elements
     * @return the combined result
     * @throws NoSuchElementException if this Map is empty
     * @throws NullPointerException   if {@code op} is null
     */
    default Tuple2<K, V> reduce(BiFunction<? super Tuple2<K, V>, ? super Tuple2<K, V>, ? extends Tuple2<K, V>> op) {
        return TraversableModule.reduceLeft(this, op);
    }

    /**
     * {@link #reduce(BiFunction)} as an {@code Option}: {@code None} on an empty Map.
     *
     * @param op combines two elements
     * @return {@code Some(result)}, or {@code None} if this Map is empty
     * @throws NullPointerException if {@code op} is null
     */
    default Option<Tuple2<K, V>> reduceOption(BiFunction<? super Tuple2<K, V>, ? super Tuple2<K, V>, ? extends Tuple2<K, V>> op) {
        return TraversableModule.reduceLeftOption(this, op);
    }

    /**
     * The only element.
     *
     * @return the element
     * @throws NoSuchElementException if this Map is empty or has more than one element
     */
    default Tuple2<K, V> single() {
        return TraversableModule.single(this);
    }

    /**
     * The only element as an {@code Option}.
     *
     * @return {@code Some(element)} if there is exactly one element, {@code None} otherwise
     */
    default Option<Tuple2<K, V>> singleOption() {
        return TraversableModule.singleOption(this);
    }

    /**
     * Arranges the elements by a key that must be unique: {@code Some} of the map from each key to its element,
     * or {@code None} as soon as two elements share a key. The same as {@code groupBy(getKey)} when every group is
     * a singleton.
     *
     * @param getKey the key of an element
     * @param <K2>  the key type
     * @return {@code Some(map)} if the keys are unique, {@code None} otherwise
     * @throws NullPointerException if {@code getKey} is null
     */
    default <K2 extends @Nullable Object> Option<Map<K2, Tuple2<K, V>>> arrangeBy(Function<? super Tuple2<K, V>, ? extends K2> getKey) {
        Objects.requireNonNull(getKey, "getKey is null");
        return TraversableModule.arrangeBy(groupBy(getKey));
    }

    /**
     * Collects the elements with {@code collector}, as {@code stream().collect(collector)} does.
     *
     * @param <A>       the collector's accumulation type
     * @param <R>       the result type
     * @param collector the collector
     * @return the collected result
     */
    default <R extends @Nullable Object, A extends @Nullable Object> R collect(Collector<? super Tuple2<K, V>, A, R> collector) {
        return stream().collect(collector);
    }

    /**
     * Collects the elements with a supplier, an accumulator and a combiner, as
     * {@code stream().collect(supplier, accumulator, combiner)} does.
     *
     * @param <R>         the result type
     * @param supplier    makes a new result container
     * @param accumulator adds an element to a container
     * @param combiner    merges two containers
     * @return the collected result
     */
    default <R extends @Nullable Object> R collect(Supplier<R> supplier, BiConsumer<R, ? super Tuple2<K, V>> accumulator, BiConsumer<R, R> combiner) {
        return stream().collect(supplier, accumulator, combiner);
    }

    /**
     * The elements copied into a new mutable {@link java.util.Collection} that {@code factory} makes for the given
     * capacity, in this Map's order: {@code toJavaCollection(java.util.LinkedHashSet::new)}.
     *
     * @param factory makes an empty mutable collection with the given initial capacity
     * @param <C>     the collection type
     * @return the new collection, filled
     * @throws NullPointerException if {@code factory} is null
     */
    default <C extends java.util.Collection<Tuple2<K, V>>> C toJavaCollection(Function<Integer, C> factory) {
        return TraversableModule.toJavaCollection(this, factory);
    }

    /**
     * The elements copied into a new {@link java.util.ArrayList}, in this Map's order.
     *
     * @return the new list
     */
    default java.util.List<Tuple2<K, V>> toJavaList() {
        return TraversableModule.toJavaCollection(this, ArrayList::new, 10);
    }

    /**
     * The elements copied into a new mutable {@link java.util.List} that {@code factory} makes for the given
     * capacity, in this Map's order: {@code toJavaList(capacity -> new java.util.LinkedList<>())}.
     *
     * @param factory makes an empty mutable list with the given initial capacity
     * @param <LIST>  the list type
     * @return the new list, filled
     * @throws NullPointerException if {@code factory} is null
     */
    default <LIST extends java.util.List<Tuple2<K, V>>> LIST toJavaList(Function<Integer, LIST> factory) {
        return TraversableModule.toJavaCollection(this, factory);
    }

    /**
     * The elements as the entries of a new {@link java.util.HashMap}, each mapped to a key and a value by
     * {@code f}; of two entries with the same key, the later one in this Map's order wins.
     *
     * @param f   the entry an element becomes
     * @param <K2> the key type
     * @param <V2> the value type
     * @return the new map
     * @throws NullPointerException if {@code f} is null
     */
    default <K2 extends @Nullable Object, V2 extends @Nullable Object> java.util.Map<K2, V2> toJavaMap(Function<? super Tuple2<K, V>, ? extends Tuple2<? extends K2, ? extends V2>> f) {
        return TraversableModule.toJavaMap(this, java.util.HashMap::new, f);
    }

    /**
     * The elements as the entries of a new mutable {@link java.util.Map} that {@code factory} makes, each mapped
     * to a key by {@code keyMapper} and to a value by {@code valueMapper}; of two entries with the same key, the
     * later one in this Map's order wins.
     *
     * @param factory     makes an empty mutable map
     * @param keyMapper   the key of an element
     * @param valueMapper the value of an element
     * @param <K2>         the key type
     * @param <V2>         the value type
     * @param <MAP>       the map type
     * @return the new map, filled
     * @throws NullPointerException if an argument is null
     */
    default <K2 extends @Nullable Object, V2 extends @Nullable Object, MAP extends java.util.Map<K2, V2>> MAP toJavaMap(Supplier<MAP> factory, Function<? super Tuple2<K, V>, ? extends K2> keyMapper, Function<? super Tuple2<K, V>, ? extends V2> valueMapper) {
        return TraversableModule.toJavaMap(this, factory, TraversableModule.entryMapper(keyMapper, valueMapper));
    }

    /**
     * The elements as the entries of a new mutable {@link java.util.Map} that {@code factory} makes, each mapped
     * to a key and a value by {@code f}; of two entries with the same key, the later one in this Map's order
     * wins.
     *
     * @param factory makes an empty mutable map
     * @param f       the entry an element becomes
     * @param <K2>     the key type
     * @param <V2>     the value type
     * @param <MAP>   the map type
     * @return the new map, filled
     * @throws NullPointerException if an argument is null
     */
    default <K2 extends @Nullable Object, V2 extends @Nullable Object, MAP extends java.util.Map<K2, V2>> MAP toJavaMap(Supplier<MAP> factory, Function<? super Tuple2<K, V>, ? extends Tuple2<? extends K2, ? extends V2>> f) {
        return TraversableModule.toJavaMap(this, factory, f);
    }

    /**
     * The distinct elements copied into a new {@link java.util.HashSet}.
     *
     * @return the new set
     */
    default java.util.Set<Tuple2<K, V>> toJavaSet() {
        return TraversableModule.toJavaCollection(this, java.util.HashSet::new, 16);
    }

    /**
     * The elements copied into a new mutable {@link java.util.Set} that {@code factory} makes for the given
     * capacity: {@code toJavaSet(capacity -> new java.util.TreeSet<>(Comparator.reverseOrder()))}.
     *
     * @param factory makes an empty mutable set with the given initial capacity
     * @param <SET>   the set type
     * @return the new set, filled
     * @throws NullPointerException if {@code factory} is null
     */
    default <SET extends java.util.Set<Tuple2<K, V>>> SET toJavaSet(Function<Integer, SET> factory) {
        return TraversableModule.toJavaCollection(this, factory);
    }

    /**
     * A parallel {@link java.util.stream.Stream} over the elements, built on {@link #spliterator()}.
     *
     * @return a new parallel {@code java.util.stream.Stream}
     */
    default java.util.stream.Stream<Tuple2<K, V>> toJavaParallelStream() {
        return StreamSupport.stream(spliterator(), true);
    }

    /**
     * The elements as the entries of a new {@link HashMap}, each mapped to a key by {@code keyMapper} and to a
     * value by {@code valueMapper}; of two entries with the same key, the later one in this Map's order wins.
     *
     * @param keyMapper   the key of an element
     * @param valueMapper the value of an element
     * @param <K2>         the key type
     * @param <V2>         the value type
     * @return the new map
     * @throws NullPointerException if an argument is null
     */
    default <K2 extends @Nullable Object, V2 extends @Nullable Object> Map<K2, V2> toMap(Function<? super Tuple2<K, V>, ? extends K2> keyMapper, Function<? super Tuple2<K, V>, ? extends V2> valueMapper) {
        return toMap(TraversableModule.entryMapper(keyMapper, valueMapper));
    }

    /**
     * The elements as the entries of a new {@link HashMap}, each mapped to a key and a value by {@code f}; of two
     * entries with the same key, the later one in this Map's order wins.
     *
     * @param f   the entry an element becomes
     * @param <K2> the key type
     * @param <V2> the value type
     * @return the new map
     * @throws NullPointerException if {@code f} is null
     */
    default <K2 extends @Nullable Object, V2 extends @Nullable Object> Map<K2, V2> toMap(Function<? super Tuple2<K, V>, ? extends Tuple2<? extends K2, ? extends V2>> f) {
        final Function<Iterable<Tuple2<? extends K2, ? extends V2>>, Map<K2, V2>> ofAll = HashMap::ofEntries;
        return TraversableModule.toMap(this, HashMap.empty(), ofAll, f);
    }

    /**
     * The elements as the entries of a new {@link LinkedHashMap}, in this Map's order, each mapped to a key by
     * {@code keyMapper} and to a value by {@code valueMapper}; of two entries with the same key, the later one
     * wins the value and the earlier one the position.
     *
     * @param keyMapper   the key of an element
     * @param valueMapper the value of an element
     * @param <K2>         the key type
     * @param <V2>         the value type
     * @return the new map
     * @throws NullPointerException if an argument is null
     */
    default <K2 extends @Nullable Object, V2 extends @Nullable Object> Map<K2, V2> toLinkedMap(Function<? super Tuple2<K, V>, ? extends K2> keyMapper, Function<? super Tuple2<K, V>, ? extends V2> valueMapper) {
        return toLinkedMap(TraversableModule.entryMapper(keyMapper, valueMapper));
    }

    /**
     * The elements as the entries of a new {@link LinkedHashMap}, in this Map's order, each mapped to a key
     * and a value by {@code f}; of two entries with the same key, the later one wins the value and the earlier one
     * the position.
     *
     * @param f   the entry an element becomes
     * @param <K2> the key type
     * @param <V2> the value type
     * @return the new map
     * @throws NullPointerException if {@code f} is null
     */
    default <K2 extends @Nullable Object, V2 extends @Nullable Object> Map<K2, V2> toLinkedMap(Function<? super Tuple2<K, V>, ? extends Tuple2<? extends K2, ? extends V2>> f) {
        final Function<Iterable<Tuple2<? extends K2, ? extends V2>>, Map<K2, V2>> ofAll = LinkedHashMap::ofEntries;
        return TraversableModule.toMap(this, LinkedHashMap.empty(), ofAll, f);
    }

    /**
     * The elements as the entries of a new {@link TreeMap} in the natural order of the keys, each mapped to a key
     * by {@code keyMapper} and to a value by {@code valueMapper}; of two entries with the same key, the later one
     * in this Map's order wins.
     *
     * @param keyMapper   the key of an element
     * @param valueMapper the value of an element
     * @param <K2>         the key type
     * @param <V2>         the value type
     * @return the new map
     * @throws NullPointerException if an argument is null
     */
    default <K2 extends Comparable<? super K2>, V2 extends @Nullable Object> SortedMap<K2, V2> toSortedMap(Function<? super Tuple2<K, V>, ? extends K2> keyMapper, Function<? super Tuple2<K, V>, ? extends V2> valueMapper) {
        return toSortedMap(TraversableModule.entryMapper(keyMapper, valueMapper));
    }

    /**
     * The elements as the entries of a new {@link TreeMap} in the natural order of the keys, each mapped to a key
     * and a value by {@code f}; of two entries with the same key, the later one in this Map's order wins.
     *
     * @param f   the entry an element becomes
     * @param <K2> the key type
     * @param <V2> the value type
     * @return the new map
     * @throws NullPointerException if {@code f} is null
     */
    default <K2 extends Comparable<? super K2>, V2 extends @Nullable Object> SortedMap<K2, V2> toSortedMap(Function<? super Tuple2<K, V>, ? extends Tuple2<? extends K2, ? extends V2>> f) {
        Objects.requireNonNull(f, "f is null");
        return toSortedMap(Comparator.naturalOrder(), f);
    }

    /**
     * The elements as the entries of a new {@link TreeMap} ordered by {@code comparator}, each mapped to a key by
     * {@code keyMapper} and to a value by {@code valueMapper}; of two entries with the same key, the later one in
     * this Map's order wins.
     *
     * @param comparator  the order of the keys
     * @param keyMapper   the key of an element
     * @param valueMapper the value of an element
     * @param <K2>         the key type
     * @param <V2>         the value type
     * @return the new map
     * @throws NullPointerException if an argument is null
     */
    default <K2 extends @Nullable Object, V2 extends @Nullable Object> SortedMap<K2, V2> toSortedMap(Comparator<? super K2> comparator, Function<? super Tuple2<K, V>, ? extends K2> keyMapper, Function<? super Tuple2<K, V>, ? extends V2> valueMapper) {
        return toSortedMap(comparator, TraversableModule.entryMapper(keyMapper, valueMapper));
    }

    /**
     * The elements as the entries of a new {@link TreeMap} ordered by {@code comparator}, each mapped to a key and
     * a value by {@code f}; of two entries with the same key, the later one in this Map's order wins.
     *
     * @param comparator the order of the keys
     * @param f          the entry an element becomes
     * @param <K2>        the key type
     * @param <V2>        the value type
     * @return the new map
     * @throws NullPointerException if an argument is null
     */
    default <K2 extends @Nullable Object, V2 extends @Nullable Object> SortedMap<K2, V2> toSortedMap(Comparator<? super K2> comparator, Function<? super Tuple2<K, V>, ? extends Tuple2<? extends K2, ? extends V2>> f) {
        Objects.requireNonNull(comparator, "comparator is null");
        final Function<Iterable<Tuple2<? extends K2, ? extends V2>>, SortedMap<K2, V2>> ofAll = t -> TreeMap.ofEntries(comparator, t);
        return TraversableModule.toMap(this, TreeMap.empty(comparator), ofAll, f);
    }

    /**
     * The elements as a {@link Queue}, in this Map's order.
     *
     * @return a {@code Queue} of the elements
     */
    default Queue<Tuple2<K, V>> toQueue() {
        return TraversableModule.toTraversable(this, Queue.empty(), Queue::ofAll);
    }

    /**
     * The distinct elements as a {@link LinkedHashSet}, in this Map's order.
     *
     * @return a {@code LinkedHashSet} of the elements
     */
    default Set<Tuple2<K, V>> toLinkedSet() {
        return TraversableModule.toTraversable(this, LinkedHashSet.empty(), LinkedHashSet::ofAll);
    }

    /**
     * The distinct elements as a {@link TreeSet} in their natural order; a {@code TreeSet} returns itself.
     *
     * @return a {@code TreeSet} of the elements
     * @throws ClassCastException if the elements are not {@link Comparable}
     */
    default SortedSet<Tuple2<K, V>> toSortedSet() {
        return TraversableModule.toSortedSet(this);
    }

    /**
     * The distinct elements as a {@link TreeSet} ordered by {@code comparator}.
     *
     * @param comparator the order
     * @return a {@code TreeSet} of the elements
     * @throws NullPointerException if {@code comparator} is null
     */
    default SortedSet<Tuple2<K, V>> toSortedSet(Comparator<? super Tuple2<K, V>> comparator) {
        Objects.requireNonNull(comparator, "comparator is null");
        return TraversableModule.toTraversable(this, TreeSet.empty(comparator), values -> TreeSet.ofAll(comparator, values));
    }

    /**
     * The elements as a {@link Stream}, in this Map's order.
     *
     * @return a {@code Stream} of the elements
     */
    default Stream<Tuple2<K, V>> toStream() {
        return TraversableModule.toTraversable(this, Stream.empty(), Stream::ofAll);
    }
}
