package com.guizmaii.zazr.collection;

import com.guizmaii.zazr.Tuple2;
import com.guizmaii.zazr.control.Option;
import java.util.Comparator;
import java.util.function.*;
import org.jspecify.annotations.Nullable;

/**
 * An immutable {@code SortedMap} interface.
 *
 * @param <K> Key type
 * @param <V> Value type
 * @author Daniel Dietrich
 */
public interface SortedMap<K extends @Nullable Object, V extends @Nullable Object> extends Map<K, V> {

    /**
     * Narrows a widened {@code SortedMap<? extends K, ? extends V>} to {@code SortedMap<K, V>}
     * by performing a type-safe cast. This is eligible because immutable/read-only
     * collections are covariant.
     * <p>
     * CAUTION: If {@code K} is narrowed, the underlying {@code Comparator} might fail!
     *
     * @param sortedMap A {@code SortedMap}.
     * @param <K>       Key type
     * @param <V>       Value type
     * @return the given {@code sortedMap} instance as narrowed type {@code SortedMap<K, V>}.
     */
    @SuppressWarnings("unchecked")
    static <K extends @Nullable Object, V extends @Nullable Object> SortedMap<K, V> narrow(SortedMap<? extends K, ? extends V> sortedMap) {
        return (SortedMap<K, V>) sortedMap;
    }

    /**
     * The comparator that orders the keys; the iteration order of the entries is consistent with it.
     *
     * @return the comparator defining the key order
     */
    Comparator<K> comparator();

    /**
     * An unmodifiable {@link java.util.NavigableMap} view of this map, in the order of its keys: nothing is copied,
     * reads go through to this map, which never changes, and every mutator of the view throws
     * {@link UnsupportedOperationException}, {@code pollFirstEntry} and {@code pollLastEntry} included. The
     * sub-maps, the head and tail maps, the descending map and the key sets are views too.
     * <p>
     * Complexity: O(1); {@code get}, {@code containsKey}, {@code firstKey}, {@code ceilingEntry} and the other
     * navigation methods of the view are O(log n).
     *
     * @return an unmodifiable {@code java.util.NavigableMap} view
     */
    @Override
    java.util.NavigableMap<K, V> asJavaMap();

    /**
     * Same as {@link #mapBoth(Function, Function)}, using a specific comparator for keys of the codomain of the given
     * {@code keyMapper}.
     *
     * @param <K2>          key's component type of the map result
     * @param <V2>          value's component type of the map result
     * @param keyComparator A comparator for keys of type K2
     * @param keyMapper     a {@code Function} that maps the keys of type {@code K} to keys of type {@code K2}
     * @param valueMapper   a {@code Function} that the values of type {@code V} to values of type {@code V2}
     * @return a new {@code SortedMap}
     * @throws NullPointerException if {@code keyMapper} or {@code valueMapper} is null
     */
    <K2 extends @Nullable Object, V2 extends @Nullable Object> SortedMap<K2, V2> mapBoth(Comparator<? super K2> keyComparator,
                                     Function<? super K, ? extends K2> keyMapper, Function<? super V, ? extends V2> valueMapper);

    /**
     * Same as {@link #flatMap(BiFunction)} but using a specific comparator for keys of the codomain of the given
     * {@code mapper}.
     *
     * @param keyComparator A comparator for keys of type K2
     * @param mapper        A function which maps key/value pairs to Iterables map entries
     * @param <K2>          New key type
     * @param <V2>          New value type
     * @return A new Map instance containing mapped entries
     */
    <K2 extends @Nullable Object, V2 extends @Nullable Object> SortedMap<K2, V2> flatMap(Comparator<? super K2> keyComparator, BiFunction<? super K, ? super V, ? extends Iterable<Tuple2<K2, V2>>> mapper);

    /**
     * Matches and transforms the entries in one pass into a {@code SortedMap} ordered by {@code keyComparator};
     * see {@link Map#collect(BiFunction)}.
     *
     * @param keyComparator the order of the new keys
     * @param mapper        a function from a key and a value to {@code Some} of the new entry or {@code None}; it
     *                      must not return {@code null}
     * @param <K2>          the new key type
     * @param <V2>          the new value type
     * @return a {@code SortedMap} of the collected entries
     * @throws NullPointerException if an argument is null, or if {@code mapper} returns {@code null} for an entry
     */
    <K2 extends @Nullable Object, V2 extends @Nullable Object> SortedMap<K2, V2> collect(Comparator<? super K2> keyComparator, BiFunction<? super K, ? super V, ? extends Option<? extends Tuple2<K2, V2>>> mapper);

    /**
     * Same as {@link #map(BiFunction)}, using a specific comparator for keys of the codomain of the given
     * {@code mapper}.
     *
     * @param keyComparator A comparator for keys of type K2
     * @param <K2>          key's component type of the map result
     * @param <V2>          value's component type of the map result
     * @param mapper        a {@code Function} that maps entries of type {@code (K, V)} to entries of type {@code (K2, V2)}
     * @return a new {@code SortedMap}
     * @throws NullPointerException if {@code mapper} is null
     */
    <K2 extends @Nullable Object, V2 extends @Nullable Object> SortedMap<K2, V2> map(Comparator<? super K2> keyComparator, BiFunction<? super K, ? super V, Tuple2<K2, V2>> mapper);

    // -- Adjusted return types of Map methods

    // -- Positional operations, in key order

    /**
     * The first entry in key order: the entry with the smallest key.
     * <p>
     * Complexity: O(log n) (the leftmost path of the tree).
     *
     * @return the entry with the smallest key
     * @throws java.util.NoSuchElementException if this map is empty
     */
    Tuple2<K, V> head();

    /**
     * The first entry in key order, if any.
     *
     * @return {@code Some} of the entry with the smallest key, or {@code None} if this map is empty
     */
    Option<Tuple2<K, V>> headOption();

    /**
     * The last entry in key order: the entry with the largest key.
     * <p>
     * Complexity: O(log n) (the rightmost path of the tree).
     *
     * @return the entry with the largest key
     * @throws java.util.NoSuchElementException if this map is empty
     */
    Tuple2<K, V> last();

    /**
     * The last entry in key order, if any.
     *
     * @return {@code Some} of the entry with the largest key, or {@code None} if this map is empty
     */
    Option<Tuple2<K, V>> lastOption();

    /**
     * All entries but the last, with the same key comparator.
     * <p>
     * Complexity: O(log n) (one rank split of the tree).
     *
     * @return this map without the entry with the largest key
     * @throws UnsupportedOperationException if this map is empty
     */
    SortedMap<K, V> init();

    /**
     * All entries but the last, if this map is not empty.
     * <p>
     * Complexity: O(log n) (one {@code init}).
     *
     * @return {@code Some} of {@link #init()}, or {@code None} if this map is empty
     */
    Option<? extends SortedMap<K, V>> initOption();

    /**
     * All entries but the first, with the same key comparator.
     * <p>
     * Complexity: O(log n) (one rank split of the tree).
     *
     * @return this map without the entry with the smallest key
     * @throws UnsupportedOperationException if this map is empty
     */
    SortedMap<K, V> tail();

    /**
     * All entries but the first, if this map is not empty.
     * <p>
     * Complexity: O(log n) (one {@code tail}).
     *
     * @return {@code Some} of {@link #tail()}, or {@code None} if this map is empty
     */
    Option<? extends SortedMap<K, V>> tailOption();

    /**
     * The first {@code n} entries in key order, with the same key comparator: empty if {@code n <= 0},
     * this map if {@code n >= size()}.
     * <p>
     * Complexity: O(log n) (one rank split of the tree).
     *
     * @param n the number of entries to keep
     * @return the {@code n} entries with the smallest keys
     */
    SortedMap<K, V> take(int n);

    /**
     * The last {@code n} entries in key order, with the same key comparator: empty if {@code n <= 0},
     * this map if {@code n >= size()}.
     * <p>
     * Complexity: O(log n) (one rank split of the tree).
     *
     * @param n the number of entries to keep
     * @return the {@code n} entries with the largest keys
     */
    SortedMap<K, V> takeRight(int n);

    /**
     * The longest prefix, in key order, of entries satisfying {@code predicate}.
     * <p>
     * Complexity: O(k + log n) for a prefix of k entries (one walk, then one rank split of the tree).
     *
     * @param predicate tested on the entries from the smallest key
     * @return the entries before the first one not satisfying {@code predicate}
     * @throws NullPointerException if {@code predicate} is null
     */
    SortedMap<K, V> takeWhile(Predicate<? super Tuple2<K, V>> predicate);

    /**
     * The longest prefix, in key order, of entries not satisfying {@code predicate}.
     * <p>
     * Complexity: O(k + log n) for a prefix of k entries (one walk, then one rank split of the tree).
     *
     * @param predicate tested on the entries from the smallest key
     * @return the entries before the first one satisfying {@code predicate}
     * @throws NullPointerException if {@code predicate} is null
     */
    SortedMap<K, V> takeUntil(Predicate<? super Tuple2<K, V>> predicate);

    /**
     * All entries but the first {@code n} in key order, with the same key comparator: this map if
     * {@code n <= 0}, empty if {@code n >= size()}.
     * <p>
     * Complexity: O(log n) (one rank split of the tree).
     *
     * @param n the number of entries to drop
     * @return the entries after the {@code n} with the smallest keys
     */
    SortedMap<K, V> drop(int n);

    /**
     * All entries but the last {@code n} in key order, with the same key comparator: this map if
     * {@code n <= 0}, empty if {@code n >= size()}.
     * <p>
     * Complexity: O(log n) (one rank split of the tree).
     *
     * @param n the number of entries to drop
     * @return the entries before the {@code n} with the largest keys
     */
    SortedMap<K, V> dropRight(int n);

    /**
     * The entries from the first one, in key order, that does not satisfy {@code predicate}.
     * <p>
     * Complexity: O(k + log n) for k dropped entries (one walk, then one rank split of the tree).
     *
     * @param predicate tested on the entries from the smallest key
     * @return the entries from the first one not satisfying {@code predicate}
     * @throws NullPointerException if {@code predicate} is null
     */
    SortedMap<K, V> dropWhile(Predicate<? super Tuple2<K, V>> predicate);

    /**
     * The entries from the first one, in key order, that satisfies {@code predicate}.
     * <p>
     * Complexity: O(k + log n) for k dropped entries (one walk, then one rank split of the tree).
     *
     * @param predicate tested on the entries from the smallest key
     * @return the entries from the first one satisfying {@code predicate}
     * @throws NullPointerException if {@code predicate} is null
     */
    SortedMap<K, V> dropUntil(Predicate<? super Tuple2<K, V>> predicate);

    /**
     * The entries paired with their rank in key order, from 0.
     * <p>
     * Complexity: O(n).
     *
     * @return the pairs (entry, rank), in order
     */
    Vector<Tuple2<Tuple2<K, V>, Integer>> zipWithIndex();

    /**
     * The blocks of {@code size} consecutive entries in key order, each a map with the same
     * comparator; the last block is smaller when {@code size} does not divide {@code size()}. The same as
     * {@code sliding(size, size)}.
     * <p>
     * Complexity: O((n / size) log n) (one rank slice of the tree per block, sharing its subtrees).
     *
     * @param size the block size, positive
     * @return the blocks, in order; empty if this map is empty
     * @throws IllegalArgumentException if {@code size} is not positive
     */
    Vector<? extends SortedMap<K, V>> grouped(int size);

    /**
     * The windows of {@code size} consecutive entries in key order, each starting one entry after
     * the previous, each a map with the same key comparator. The same as {@code sliding(size, 1)}.
     * <p>
     * Complexity: O(n log n) (one rank slice of the tree per window, sharing its subtrees).
     *
     * @param size the window size, positive
     * @return the windows, in order; empty if this map is empty
     * @throws IllegalArgumentException if {@code size} is not positive
     */
    Vector<? extends SortedMap<K, V>> sliding(int size);

    /**
     * The windows of {@code size} consecutive entries in key order, each starting {@code step}
     * entries after the previous, each a map with the same key comparator. The window rule is {@link Vector}'s: the
     * last window is shorter than {@code size} when it reaches the end, a window whose entries all belong to the
     * previous one is not produced, a map smaller than {@code size} is one window and an empty map has none.
     * <p>
     * Complexity: O((n / step) log n) (one rank slice of the tree per window, sharing its subtrees).
     *
     * @param size the window size, positive
     * @param step the distance between two window starts, positive
     * @return the windows, in order
     * @throws IllegalArgumentException if {@code size} or {@code step} is not positive
     */
    Vector<? extends SortedMap<K, V>> sliding(int size, int step);

    /**
     * The maximal runs of consecutive entries, in key order, with the same key, computed once per
     * entry by {@code classifier}; each run is a map with the same key comparator, and the runs together are this map.
     * <p>
     * Complexity: O(n + r log n) for r runs (one walk, then one rank slice of the tree per run).
     *
     * @param classifier the key of an entry; two consecutive entries are in the same run when their keys are equal
     * @return the runs, in order; empty if this map is empty
     * @throws NullPointerException if {@code classifier} is null
     */
    Vector<? extends SortedMap<K, V>> slideBy(Function<? super Tuple2<K, V>, ?> classifier);

    @Override
    <K2 extends @Nullable Object, V2 extends @Nullable Object> SortedMap<K2, V2> mapBoth(Function<? super K, ? extends K2> keyMapper, Function<? super V, ? extends V2> valueMapper);

    @Override
    Tuple2<V, ? extends SortedMap<K, V>> computeIfAbsent(K key, Function<? super K, ? extends V> mappingFunction);

    @Override
    Tuple2<Option<V>, ? extends SortedMap<K, V>> computeIfPresent(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction);

    @Override
    SortedMap<K, V> filter(Predicate<? super Tuple2<K, V>> predicate);

    @Override
    SortedMap<K, V> reject(Predicate<? super Tuple2<K, V>> predicate);

    @Override
    SortedMap<K, V> filter(BiPredicate<? super K, ? super V> predicate);

    @Override
    SortedMap<K, V> reject(BiPredicate<? super K, ? super V> predicate);

    @Override
    SortedMap<K, V> filterKeys(Predicate<? super K> predicate);

    @Override
    SortedMap<K, V> rejectKeys(Predicate<? super K> predicate);

    @Override
    SortedMap<K, V> filterValues(Predicate<? super V> predicate);

    @Override
    SortedMap<K, V> rejectValues(Predicate<? super V> predicate);

    /**
     * {@inheritDoc}
     * <p>
     * Complexity: O(n log n) (the kept entries inserted into a new tree).
     */
    @Override
    @Deprecated
    SortedMap<K, V> removeAll(BiPredicate<? super K, ? super V> predicate);

    @Override
    @Deprecated
    SortedMap<K, V> removeKeys(Predicate<? super K> predicate);

    @Override
    @Deprecated
    SortedMap<K, V> removeValues(Predicate<? super V> predicate);

    @Override
    <K2 extends @Nullable Object, V2 extends @Nullable Object> SortedMap<K2, V2> flatMap(BiFunction<? super K, ? super V, ? extends Iterable<Tuple2<K2, V2>>> mapper);

    @Override
    <C extends @Nullable Object> Map<C, ? extends SortedMap<K, V>> groupBy(Function<? super Tuple2<K, V>, ? extends C> classifier);

    @Override
    SortedSet<K> keySet();

    /**
     * {@inheritDoc}
     * <p>
     * The result is ordered by the natural order of {@code K2}; use {@link #collect(Comparator, BiFunction)} to
     * choose the order.
     */
    @Override
    <K2 extends @Nullable Object, V2 extends @Nullable Object> SortedMap<K2, V2> collect(BiFunction<? super K, ? super V, ? extends Option<? extends Tuple2<K2, V2>>> mapper);

    @Override
    <K2 extends @Nullable Object, V2 extends @Nullable Object> SortedMap<K2, V2> map(BiFunction<? super K, ? super V, Tuple2<K2, V2>> mapper);

    @Override
    <K2 extends @Nullable Object> SortedMap<K2, V> mapKeys(Function<? super K, ? extends K2> keyMapper);

    @Override
    <K2 extends @Nullable Object> SortedMap<K2, V> mapKeys(Function<? super K, ? extends K2> keyMapper, BiFunction<? super V, ? super V, ? extends V> valueMerge);

    @Override
    <V2 extends @Nullable Object> SortedMap<K, V2> mapValues(Function<? super V, ? extends V2> valueMapper);

    @Override
    SortedMap<K, V> merge(Map<? extends K, ? extends V> that);

    @Override
    <U extends V> SortedMap<K, V> merge(Map<? extends K, U> that, BiFunction<? super V, ? super U, ? extends V> collisionResolution);

    @Override
    SortedMap<K, V> orElse(Iterable<? extends Tuple2<K, V>> other);

    @Override
    SortedMap<K, V> orElse(Supplier<? extends Iterable<? extends Tuple2<K, V>>> supplier);

    @Override
    Tuple2<? extends SortedMap<K, V>, ? extends SortedMap<K, V>> partition(Predicate<? super Tuple2<K, V>> predicate);

    @Override
    SortedMap<K, V> tap(Consumer<? super Tuple2<K, V>> action);

    @Override
    SortedMap<K, V> put(K key, V value);

    @Override
    SortedMap<K, V> put(Tuple2<? extends K, ? extends V> entry);

    @Override
    <U extends V> SortedMap<K, V> put(K key, U value, BiFunction<? super V, ? super U, ? extends V> merge);

    @Override
    <U extends V> SortedMap<K, V> put(Tuple2<? extends K, U> entry, BiFunction<? super V, ? super U, ? extends V> merge);

    /**
     * {@inheritDoc}
     * <p>
     * Complexity: O(log n) (one lookup and one deletion in the tree).
     */
    @Override
    SortedMap<K, V> remove(K key);

    /**
     * {@inheritDoc}
     * <p>
     * Complexity: O(m log n) for m given keys (one lookup, and a deletion when present, per key).
     */
    @Override
    SortedMap<K, V> removeAll(Iterable<? extends K> keys);

    /**
     * {@inheritDoc}
     * <p>
     * Complexity: O(log n) (one lookup and one insertion in the tree).
     */
    @Override
    SortedMap<K, V> replace(K key, V oldValue, V newValue);

    /**
     * {@inheritDoc}
     * <p>
     * Complexity: O(log n) (one lookup, one deletion and one insertion in the tree).
     */
    @Override
    SortedMap<K, V> replace(Tuple2<K, V> currentElement, Tuple2<K, V> newElement);

    @Override
    SortedMap<K, V> replaceValue(K key, V value);

    /**
     * {@inheritDoc}
     * <p>
     * Complexity: O(n log n) (every entry inserted into a new tree).
     */
    @Override
    SortedMap<K, V> replaceAll(BiFunction<? super K, ? super V, ? extends V> function);

    /**
     * {@inheritDoc}
     * <p>
     * Complexity: that of {@link #replace(Tuple2, Tuple2)}: a map holds an entry once.
     */
    @Override
    SortedMap<K, V> replaceAll(Tuple2<K, V> currentElement, Tuple2<K, V> newElement);

    /**
     * {@inheritDoc}
     * <p>
     * Complexity: O(m log n) for m given entries (one lookup per entry, the present ones inserted into a new tree).
     */
    @Override
    SortedMap<K, V> retainAll(Iterable<? extends Tuple2<K, V>> elements);

}
