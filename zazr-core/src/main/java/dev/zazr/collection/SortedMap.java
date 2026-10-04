package dev.zazr.collection;

import dev.zazr.Tuple2;
import dev.zazr.control.Option;
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
    static <K extends @Nullable Object, V extends @Nullable Object> SortedMap<K, V> narrow(
            SortedMap<? extends K, ? extends V> sortedMap) {
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
     * sub-maps, the head and tail maps, the descending map and the key sets are views too. A mutable copy is
     * {@code new java.util.TreeMap<>(map.asJavaMap())}.
     * <p>
     * Complexity: O(1): nothing is copied. On the view, {@code get}, {@code containsKey}, {@code firstKey},
     * {@code ceilingEntry} and the other navigation methods are O(log n).
     *
     * @return an unmodifiable {@code java.util.NavigableMap} view
     */
    @Override
    java.util.NavigableMap<K, V> asJavaMap();

    /**
     * Same as {@link #mapBoth(Function, Function)}, using a specific comparator for keys of the codomain of the given
     * {@code keyMapper}.
     *
     * <p>
     * Complexity: O(n log n): the new entries are sorted, then the new tree is built in one pass; O(n) when the new
     * keys come out in order.
     *
     * @param <K2>          key's component type of the map result
     * @param <V2>          value's component type of the map result
     * @param keyComparator A comparator for keys of type K2
     * @param keyMapper     a {@code Function} that maps the keys of type {@code K} to keys of type {@code K2}
     * @param valueMapper   a {@code Function} that the values of type {@code V} to values of type {@code V2}
     * @return a new {@code SortedMap}
     * @throws NullPointerException if {@code keyMapper} or {@code valueMapper} is null
     */
    <K2 extends @Nullable Object, V2 extends @Nullable Object> SortedMap<K2, V2> mapBoth(
            Comparator<? super K2> keyComparator,
            Function<? super K, ? extends K2> keyMapper,
            Function<? super V, ? extends V2> valueMapper);

    /**
     * Same as {@link #flatMap(BiFunction)} but using a specific comparator for keys of the codomain of the given
     * {@code mapper}.
     *
     * <p>
     * Complexity: O(n + k log k) for k entries produced by {@code mapper}: they are sorted, then the new tree is built
     * in one pass; O(n + k) when their keys come out in order.
     *
     * @param keyComparator A comparator for keys of type K2
     * @param mapper        A function which maps key/value pairs to Iterables map entries
     * @param <K2>          New key type
     * @param <V2>          New value type
     * @return A new Map instance containing mapped entries
     */
    <K2 extends @Nullable Object, V2 extends @Nullable Object> SortedMap<K2, V2> flatMap(
            Comparator<? super K2> keyComparator,
            BiFunction<? super K, ? super V, ? extends Iterable<Tuple2<K2, V2>>> mapper);

    /**
     * Matches and transforms the entries in one pass into a {@code SortedMap} ordered by {@code keyComparator};
     * see {@link Map#collect(BiFunction)}.
     * <p>
     * Complexity: O(n log n): the collected entries are sorted, then the new tree is built in one pass; O(n) when their
     * keys come out in order.
     *
     * @param keyComparator the order of the new keys
     * @param mapper        a function from a key and a value to {@code Some} of the new entry or {@code None}; it
     *                      must not return {@code null}
     * @param <K2>          the new key type
     * @param <V2>          the new value type
     * @return a {@code SortedMap} of the collected entries
     * @throws NullPointerException if an argument is null, or if {@code mapper} returns {@code null} for an entry
     */
    <K2 extends @Nullable Object, V2 extends @Nullable Object> SortedMap<K2, V2> collect(
            Comparator<? super K2> keyComparator,
            BiFunction<? super K, ? super V, ? extends Option<? extends Tuple2<K2, V2>>> mapper);

    /**
     * Same as {@link #map(BiFunction)}, using a specific comparator for keys of the codomain of the given
     * {@code mapper}.
     *
     * <p>
     * Complexity: O(n log n): the new entries are sorted, then the new tree is built in one pass; O(n) when the new
     * keys come out in order.
     *
     * @param keyComparator A comparator for keys of type K2
     * @param <K2>          key's component type of the map result
     * @param <V2>          value's component type of the map result
     * @param mapper        a {@code Function} that maps entries of type {@code (K, V)} to entries of type {@code (K2, V2)}
     * @return a new {@code SortedMap}
     * @throws NullPointerException if {@code mapper} is null
     */
    <K2 extends @Nullable Object, V2 extends @Nullable Object> SortedMap<K2, V2> map(
            Comparator<? super K2> keyComparator, BiFunction<? super K, ? super V, Tuple2<K2, V2>> mapper);

    // -- Adjusted return types of Map methods

    // -- Ranges, by key

    /**
     * The entries whose key is greater than or equal to {@code from}, with the same key comparator: this map itself
     * when no key is below {@code from}. {@code from} need not be a key.
     * <p>
     * Complexity: O(log n): the tree is cut along the path of {@code from} without visiting the other entries, and
     * the result shares the rest of the tree.
     *
     * @param from the lower bound on the keys, inclusive
     * @return the entries from {@code from} on
     * @throws NullPointerException if {@code from} is null
     */
    SortedMap<K, V> rangeFrom(K from);

    /**
     * The entries whose key is less than {@code until}, with the same key comparator: this map itself when no key is
     * at or above {@code until}. {@code until} need not be a key.
     * <p>
     * Complexity: O(log n): the tree is cut along the path of {@code until} without visiting the other entries, and
     * the result shares the rest of the tree.
     *
     * @param until the upper bound on the keys, exclusive
     * @return the entries before {@code until}
     * @throws NullPointerException if {@code until} is null
     */
    SortedMap<K, V> rangeUntil(K until);

    /**
     * The entries whose key is less than or equal to {@code to}, with the same key comparator: this map itself when
     * no key is above {@code to}. {@code to} need not be a key.
     * <p>
     * Complexity: O(log n): the tree is cut along the path of {@code to} without visiting the other entries, and the
     * result shares the rest of the tree.
     *
     * @param to the upper bound on the keys, inclusive
     * @return the entries up to {@code to}
     * @throws NullPointerException if {@code to} is null
     */
    SortedMap<K, V> rangeTo(K to);

    /**
     * The entries whose key is greater than or equal to {@code from} and less than {@code until}, with the same key
     * comparator: this map itself when every key is in the range. The range is empty, and so is the result, when
     * {@code from} is not less than {@code until}; unlike {@link java.util.TreeMap#subMap(Object, Object)}, this does
     * not throw when {@code from} is greater. Neither bound need be a key.
     * <p>
     * Complexity: O(log n): the tree is cut along the paths of the two bounds without visiting the other entries, and
     * the result shares the rest of the tree.
     *
     * @param from  the lower bound on the keys, inclusive
     * @param until the upper bound on the keys, exclusive
     * @return the entries from {@code from} to {@code until}
     * @throws NullPointerException if {@code from} or {@code until} is null
     */
    SortedMap<K, V> rangeFromUntil(K from, K until);

    /**
     * The entry with the least key greater than or equal to {@code key}: the entry of {@code key} itself when this
     * map has one. The same as {@code rangeFrom(key).headOption()}, without building the range.
     * <p>
     * Complexity: O(log n): one walk down the tree.
     *
     * @param key the lower bound on the keys, inclusive
     * @return {@code Some} of that entry, or {@code None} if every key is less than {@code key}
     * @throws NullPointerException if {@code key} is null
     */
    Option<Tuple2<K, V>> minAfter(K key);

    /**
     * The entry with the greatest key strictly less than {@code key}: unlike {@link #minAfter(Object)}, the bound is
     * excluded. The same as {@code rangeUntil(key).lastOption()}, without building the range.
     * <p>
     * Complexity: O(log n): one walk down the tree.
     *
     * @param key the upper bound on the keys, exclusive
     * @return {@code Some} of that entry, or {@code None} if no key is less than {@code key}
     * @throws NullPointerException if {@code key} is null
     */
    Option<Tuple2<K, V>> maxBefore(K key);

    /**
     * An iterator over the entries whose key is greater than or equal to {@code start}, in key order: the same
     * entries as {@code rangeFrom(start).iterator()}, without building the range.
     * <p>
     * Complexity: O(log n) to create, then O(1) per step on average; a whole walk is O(k + log n) for k entries.
     *
     * @param start the lower bound on the keys, inclusive
     * @return an iterator from {@code start} on
     * @throws NullPointerException if {@code start} is null
     */
    java.util.Iterator<Tuple2<K, V>> iteratorFrom(K start);

    // -- Positional operations, in key order

    /**
     * The first entry in key order: the entry with the smallest key.
     * <p>
     * Complexity: O(log n): the entry with the smallest key is found by walking down the tree, with no comparison.
     *
     * @return the entry with the smallest key
     * @throws java.util.NoSuchElementException if this map is empty
     */
    Tuple2<K, V> head();

    /**
     * The first entry in key order, if any.
     *
     * <p>
     * Complexity: O(log n), as {@link #head()}.
     *
     * @return {@code Some} of the entry with the smallest key, or {@code None} if this map is empty
     */
    Option<Tuple2<K, V>> headOption();

    /**
     * The last entry in key order: the entry with the largest key.
     * <p>
     * Complexity: O(log n): the entry with the largest key is found by walking down the tree, with no comparison.
     *
     * @return the entry with the largest key
     * @throws java.util.NoSuchElementException if this map is empty
     */
    Tuple2<K, V> last();

    /**
     * The last entry in key order, if any.
     *
     * <p>
     * Complexity: O(log n), as {@link #last()}.
     *
     * @return {@code Some} of the entry with the largest key, or {@code None} if this map is empty
     */
    Option<Tuple2<K, V>> lastOption();

    /**
     * All entries but the last, with the same key comparator.
     * <p>
     * Complexity: O(log n): the tree is cut without visiting the entries, and the result shares the rest of the
     * tree.
     *
     * @return this map without the entry with the largest key
     * @throws UnsupportedOperationException if this map is empty
     */
    SortedMap<K, V> init();

    /**
     * All entries but the last, if this map is not empty.
     * <p>
     * Complexity: O(log n), as {@link #init()}.
     *
     * @return {@code Some} of {@link #init()}, or {@code None} if this map is empty
     */
    Option<? extends SortedMap<K, V>> initOption();

    /**
     * All entries but the first, with the same key comparator.
     * <p>
     * Complexity: O(log n): the tree is cut without visiting the entries, and the result shares the rest of the
     * tree.
     *
     * @return this map without the entry with the smallest key
     * @throws UnsupportedOperationException if this map is empty
     */
    SortedMap<K, V> tail();

    /**
     * All entries but the first, if this map is not empty.
     * <p>
     * Complexity: O(log n), as {@link #tail()}.
     *
     * @return {@code Some} of {@link #tail()}, or {@code None} if this map is empty
     */
    Option<? extends SortedMap<K, V>> tailOption();

    /**
     * The first {@code n} entries in key order, with the same key comparator: empty if {@code n <= 0},
     * this map if {@code n >= size()}.
     * <p>
     * Complexity: O(log n): the tree is cut at that position without visiting the entries, and the result shares
     * the rest of the tree.
     *
     * @param n the number of entries to keep
     * @return the {@code n} entries with the smallest keys
     */
    SortedMap<K, V> take(int n);

    /**
     * The last {@code n} entries in key order, with the same key comparator: empty if {@code n <= 0},
     * this map if {@code n >= size()}.
     * <p>
     * Complexity: O(log n): the tree is cut at that position without visiting the entries, and the result shares
     * the rest of the tree.
     *
     * @param n the number of entries to keep
     * @return the {@code n} entries with the largest keys
     */
    SortedMap<K, V> takeRight(int n);

    /**
     * The longest prefix, in key order, of entries satisfying {@code predicate}.
     * <p>
     * Complexity: O(k + log n) for a prefix of k entries: the prefix is walked, then the tree is cut after it.
     *
     * @param predicate tested on the entries from the smallest key
     * @return the entries before the first one not satisfying {@code predicate}
     * @throws NullPointerException if {@code predicate} is null
     */
    SortedMap<K, V> takeWhile(Predicate<? super Tuple2<K, V>> predicate);

    /**
     * The longest prefix, in key order, of entries not satisfying {@code predicate}.
     * <p>
     * Complexity: O(k + log n) for a prefix of k entries: the prefix is walked, then the tree is cut after it.
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
     * Complexity: O(log n): the tree is cut at that position without visiting the entries, and the result shares
     * the rest of the tree.
     *
     * @param n the number of entries to drop
     * @return the entries after the {@code n} with the smallest keys
     */
    SortedMap<K, V> drop(int n);

    /**
     * All entries but the last {@code n} in key order, with the same key comparator: this map if
     * {@code n <= 0}, empty if {@code n >= size()}.
     * <p>
     * Complexity: O(log n): the tree is cut at that position without visiting the entries, and the result shares
     * the rest of the tree.
     *
     * @param n the number of entries to drop
     * @return the entries before the {@code n} with the largest keys
     */
    SortedMap<K, V> dropRight(int n);

    /**
     * The entries from the first one, in key order, that does not satisfy {@code predicate}.
     * <p>
     * Complexity: O(k + log n) for k dropped entries: they are walked, then the tree is cut after them.
     *
     * @param predicate tested on the entries from the smallest key
     * @return the entries from the first one not satisfying {@code predicate}
     * @throws NullPointerException if {@code predicate} is null
     */
    SortedMap<K, V> dropWhile(Predicate<? super Tuple2<K, V>> predicate);

    /**
     * The entries from the first one, in key order, that satisfies {@code predicate}.
     * <p>
     * Complexity: O(k + log n) for k dropped entries: they are walked, then the tree is cut after them.
     *
     * @param predicate tested on the entries from the smallest key
     * @return the entries from the first one satisfying {@code predicate}
     * @throws NullPointerException if {@code predicate} is null
     */
    SortedMap<K, V> dropUntil(Predicate<? super Tuple2<K, V>> predicate);

    /**
     * The entries paired with their rank in key order, from 0.
     * <p>
     * Complexity: O(n): one walk in key order.
     *
     * @return the pairs (entry, rank), in order
     */
    Vector<Tuple2<Tuple2<K, V>, Integer>> zipWithIndex();

    /**
     * The blocks of {@code size} consecutive entries in key order, each a map with the same
     * comparator; the last block is smaller when {@code size} does not divide {@code size()}. The same as
     * {@code sliding(size, size)}.
     * <p>
     * Complexity: O((n / size) log n): the tree is cut once per block, in O(log n); the blocks share the tree's
     * structure, and the entries are not copied.
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
     * Complexity: O(n log n): the tree is cut once per window, in O(log n); the windows share the tree's structure,
     * and the entries are not copied.
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
     * Complexity: O((n / step) log n): the tree is cut once per window, in O(log n); the windows share the tree's
     * structure, and the entries are not copied.
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
     * Complexity: O(n + k log n) for k runs: one walk finds them, then the tree is cut once per run, in O(log n).
     *
     * @param classifier the key of an entry; two consecutive entries are in the same run when their keys are equal
     * @return the runs, in order; empty if this map is empty
     * @throws NullPointerException if {@code classifier} is null
     */
    Vector<? extends SortedMap<K, V>> slideBy(Function<? super Tuple2<K, V>, ?> classifier);

    /**
     * {@inheritDoc}
     * <p>
     * Complexity: O(n log n): the new entries are sorted, then the new tree is built in one pass; O(n) when the new
     * keys come out in order.
     */
    @Override
    <K2 extends @Nullable Object, V2 extends @Nullable Object> SortedMap<K2, V2> mapBoth(
            Function<? super K, ? extends K2> keyMapper, Function<? super V, ? extends V2> valueMapper);

    /**
     * {@inheritDoc}
     * <p>
     * Complexity: O(n): the new tree is built from the kept entries, which come in order, in one pass.
     */
    @Override
    SortedMap<K, V> filter(Predicate<? super Tuple2<K, V>> predicate);

    /**
     * {@inheritDoc}
     * <p>
     * Complexity: O(n): the new tree is built from the kept entries, which come in order, in one pass.
     */
    @Override
    SortedMap<K, V> reject(Predicate<? super Tuple2<K, V>> predicate);

    /**
     * {@inheritDoc}
     * <p>
     * Complexity: O(n): the new tree is built from the kept entries, which come in order, in one pass.
     */
    @Override
    SortedMap<K, V> filter(BiPredicate<? super K, ? super V> predicate);

    /**
     * {@inheritDoc}
     * <p>
     * Complexity: O(n): the new tree is built from the kept entries, which come in order, in one pass.
     */
    @Override
    SortedMap<K, V> reject(BiPredicate<? super K, ? super V> predicate);

    /**
     * {@inheritDoc}
     * <p>
     * Complexity: O(n): the new tree is built from the kept entries, which come in order, in one pass.
     */
    @Override
    SortedMap<K, V> filterKeys(Predicate<? super K> predicate);

    /**
     * {@inheritDoc}
     * <p>
     * Complexity: O(n): the new tree is built from the kept entries, which come in order, in one pass.
     */
    @Override
    SortedMap<K, V> rejectKeys(Predicate<? super K> predicate);

    /**
     * {@inheritDoc}
     * <p>
     * Complexity: O(n): the new tree is built from the kept entries, which come in order, in one pass.
     */
    @Override
    SortedMap<K, V> filterValues(Predicate<? super V> predicate);

    /**
     * {@inheritDoc}
     * <p>
     * Complexity: O(n): the new tree is built from the kept entries, which come in order, in one pass.
     */
    @Override
    SortedMap<K, V> rejectValues(Predicate<? super V> predicate);

    /**
     * {@inheritDoc}
     * <p>
     * Complexity: O(n): the new tree is built from the kept entries, which come in order, in one pass.
     */
    @Override
    @Deprecated
    SortedMap<K, V> removeAll(BiPredicate<? super K, ? super V> predicate);

    /**
     * {@inheritDoc}
     * <p>
     * Complexity: O(n): the new tree is built from the kept entries, which come in order, in one pass.
     */
    @Override
    @Deprecated
    SortedMap<K, V> removeKeys(Predicate<? super K> predicate);

    /**
     * {@inheritDoc}
     * <p>
     * Complexity: O(n): the new tree is built from the kept entries, which come in order, in one pass.
     */
    @Override
    @Deprecated
    SortedMap<K, V> removeValues(Predicate<? super V> predicate);

    /**
     * {@inheritDoc}
     * <p>
     * Complexity: O(n + k log k) for k entries produced by {@code mapper}: they are sorted, then the new tree is built
     * in one pass; O(n + k) when their keys come out in order.
     */
    @Override
    <K2 extends @Nullable Object, V2 extends @Nullable Object> SortedMap<K2, V2> flatMap(
            BiFunction<? super K, ? super V, ? extends Iterable<Tuple2<K2, V2>>> mapper);

    /**
     * {@inheritDoc}
     * <p>
     * Complexity: O(n): each group gets its entries in order, and its tree is built from them in one pass.
     */
    @Override
    <C extends @Nullable Object> Map<C, ? extends SortedMap<K, V>> groupBy(
            Function<? super Tuple2<K, V>, ? extends C> classifier);

    /**
     * {@inheritDoc}
     * <p>
     * Complexity: O(n), with no comparison: the keys are copied from the tree of the entries, in the same shape.
     */
    @Override
    SortedSet<K> keySet();

    /**
     * {@inheritDoc}
     * <p>
     * The result is ordered by the natural order of {@code K2}; use {@link #collect(Comparator, BiFunction)} to
     * choose the order.
     * <p>
     * Complexity: O(n log n): the collected entries are sorted, then the new tree is built in one pass; O(n) when their
     * keys come out in order.
     */
    @Override
    <K2 extends @Nullable Object, V2 extends @Nullable Object> SortedMap<K2, V2> collect(
            BiFunction<? super K, ? super V, ? extends Option<? extends Tuple2<K2, V2>>> mapper);

    /**
     * {@inheritDoc}
     * <p>
     * Complexity: O(n log n): the new entries are sorted, then the new tree is built in one pass; O(n) when the new
     * keys come out in order.
     */
    @Override
    <K2 extends @Nullable Object, V2 extends @Nullable Object> SortedMap<K2, V2> map(
            BiFunction<? super K, ? super V, Tuple2<K2, V2>> mapper);

    /**
     * {@inheritDoc}
     * <p>
     * Complexity: O(n log n): the new entries are sorted, then the new tree is built in one pass; O(n) when the new
     * keys come out in order.
     */
    @Override
    <K2 extends @Nullable Object> SortedMap<K2, V> mapKeys(Function<? super K, ? extends K2> keyMapper);

    /**
     * {@inheritDoc}
     * <p>
     * Complexity: O(n log n): one lookup and one insertion in a new tree per entry.
     */
    @Override
    <K2 extends @Nullable Object> SortedMap<K2, V> mapKeys(
            Function<? super K, ? extends K2> keyMapper, BiFunction<? super V, ? super V, ? extends V> valueMerge);

    /**
     * {@inheritDoc}
     * <p>
     * Complexity: O(n): the keys do not change, so the new tree is built from the entries, in order, in one pass.
     */
    @Override
    <V2 extends @Nullable Object> SortedMap<K, V2> mapValues(Function<? super V, ? extends V2> valueMapper);

    /**
     * {@inheritDoc}
     * <p>
     * Complexity: O(m log(n + m)) for the m entries of {@code that}: one lookup and at most one insertion each. When
     * this map is empty, the entries of {@code that} are sorted and built into a new tree, O(m log m).
     */
    @Override
    SortedMap<K, V> merge(Map<? extends K, ? extends V> that);

    /**
     * {@inheritDoc}
     * <p>
     * Complexity: O(m log(n + m)) for the m entries of {@code that}: one lookup and at most one insertion each. When
     * this map is empty, the entries of {@code that} are sorted and built into a new tree, O(m log m).
     */
    @Override
    <U extends V> SortedMap<K, V> merge(
            Map<? extends K, U> that, BiFunction<? super V, ? super U, ? extends V> collisionResolution);

    /**
     * {@inheritDoc}
     * <p>
     * Complexity: O(m log m) for the m entries of {@code other} when this map is empty: they are sorted, then the new
     * tree is built in one pass (O(m) when they come sorted); O(1) when this map is not empty.
     */
    @Override
    SortedMap<K, V> orElse(Iterable<? extends Tuple2<K, V>> other);

    /**
     * {@inheritDoc}
     * <p>
     * Complexity: O(m log m) for the m supplied entries when this map is empty: they are sorted, then the new tree is
     * built in one pass (O(m) when they come sorted); O(1) when this map is not empty, and the supplier is not
     * called.
     */
    @Override
    SortedMap<K, V> orElse(Supplier<? extends Iterable<? extends Tuple2<K, V>>> supplier);

    /**
     * {@inheritDoc}
     * <p>
     * Complexity: O(n): both results get their entries in order, and their trees are built from them in one pass.
     */
    @Override
    Tuple2<? extends SortedMap<K, V>, ? extends SortedMap<K, V>> partition(Predicate<? super Tuple2<K, V>> predicate);

    @Override
    SortedMap<K, V> tap(Consumer<? super Tuple2<K, V>> action);

    /**
     * {@inheritDoc}
     * <p>
     * Complexity: O(log n): one insertion; an entry with an equal key is replaced.
     */
    @Override
    SortedMap<K, V> put(K key, V value);

    /**
     * {@inheritDoc}
     * <p>
     * Complexity: O(log n): one insertion; an entry with an equal key is replaced.
     */
    @Override
    SortedMap<K, V> put(Tuple2<? extends K, ? extends V> entry);

    /**
     * {@inheritDoc}
     * <p>
     * Complexity: O(m log(n + m)) for m entries, one insertion each. When {@code entries} is a TreeMap with an equal
     * comparator (for a lambda, the same object), the cost is O(m log(n / m + 1)), m then being the smaller of the two
     * sizes: the two trees are cut and joined, not rebuilt. O(1) when {@code entries} is an empty map.
     */
    @Override
    SortedMap<K, V> putAll(Iterable<? extends Tuple2<? extends K, ? extends V>> entries);

    /**
     * {@inheritDoc}
     * <p>
     * Complexity: O(log n): one lookup, then one insertion.
     */
    @Override
    <U extends V> SortedMap<K, V> put(K key, U value, BiFunction<? super V, ? super U, ? extends V> merge);

    /**
     * {@inheritDoc}
     * <p>
     * Complexity: O(log n): one lookup, then one insertion.
     */
    @Override
    <U extends V> SortedMap<K, V> put(
            Tuple2<? extends K, U> entry, BiFunction<? super V, ? super U, ? extends V> merge);

    /**
     * {@inheritDoc}
     * <p>
     * Complexity: O(log n): one walk down the tree, then one deletion on the way back up when the key is present.
     */
    @Override
    SortedMap<K, V> remove(K key);

    /**
     * {@inheritDoc}
     * <p>
     * Complexity: O(log n): one lookup, then at most one insertion or deletion.
     */
    @Override
    SortedMap<K, V> updateWith(K key, Function<? super Option<V>, ? extends Option<? extends V>> f);

    /**
     * {@inheritDoc}
     * <p>
     * Complexity: O(m log n) for m keys: one walk down the tree each, then one deletion for each key present.
     */
    @Override
    SortedMap<K, V> removeAll(Iterable<? extends K> keys);

    /**
     * {@inheritDoc}
     * <p>
     * Complexity: O(log n): one lookup, then one insertion.
     */
    @Override
    SortedMap<K, V> replace(K key, V oldValue, V newValue);

    /**
     * {@inheritDoc}
     * <p>
     * Complexity: O(log n): one lookup, one deletion and one insertion.
     */
    @Override
    SortedMap<K, V> replace(Tuple2<K, V> currentElement, Tuple2<K, V> newElement);

    /**
     * {@inheritDoc}
     * <p>
     * Complexity: O(log n): one lookup, then one insertion when the key is present.
     */
    @Override
    SortedMap<K, V> replaceValue(K key, V value);

    /**
     * {@inheritDoc}
     * <p>
     * Complexity: O(n), with no comparison: the tree is copied in the same shape, with the new values.
     */
    @Override
    SortedMap<K, V> replaceAll(BiFunction<? super K, ? super V, ? extends V> function);

    /**
     * {@inheritDoc}
     * <p>
     * Complexity: O(log n), as {@link #replace(Tuple2, Tuple2)}: a map holds an entry once.
     */
    @Override
    SortedMap<K, V> replaceAll(Tuple2<K, V> currentElement, Tuple2<K, V> newElement);

    /**
     * {@inheritDoc}
     * <p>
     * Complexity: O(m log(n + m)) for m given entries: one lookup each, then the present ones are sorted and a new
     * tree is built from them in one pass.
     */
    @Override
    SortedMap<K, V> retainAll(Iterable<? extends Tuple2<K, V>> elements);
}
