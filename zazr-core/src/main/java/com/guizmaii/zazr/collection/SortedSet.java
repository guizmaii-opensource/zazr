package com.guizmaii.zazr.collection;

import com.guizmaii.zazr.Tuple2;
import com.guizmaii.zazr.control.Option;
import java.util.Comparator;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import org.jspecify.annotations.Nullable;

/**
 * A {@link Set} whose elements are kept in the order of a {@link Comparator}; {@link TreeSet} is the implementation.
 * <p>
 * Specific SortedSet operations:
 *
 * <ul>
 * <li>{@link #comparator()}</li>
 * <li>{@link #flatMap(Comparator, Function)}</li>
 * <li>{@link #map(Comparator, Function)}</li>
 * </ul>
 *
 * @param <T> Component type
 * @author Daniel Dietrich
 */
public interface SortedSet<T extends @Nullable Object> extends Set<T> {

    /**
     * Narrows a widened {@code SortedSet<? extends T>} to {@code SortedSet<T>}
     * by performing a type-safe cast. This is eligible because immutable/read-only
     * collections are covariant.
     * <p>
     * CAUTION: The underlying {@code Comparator} might fail!
     *
     * @param sortedSet A {@code SortedSet}.
     * @param <T>       Component type of the {@code SortedSet}.
     * @return the given {@code sortedSet} instance as narrowed type {@code SortedSet<T>}.
     */
    @SuppressWarnings("unchecked")
    static <T extends @Nullable Object> SortedSet<T> narrow(SortedSet<? extends T> sortedSet) {
        return (SortedSet<T>) sortedSet;
    }

    /**
     * The comparator that orders the elements; the iteration order is consistent with it.
     *
     * @return the comparator defining the order
     */
    Comparator<T> comparator();

    /**
     * An unmodifiable {@link java.util.NavigableSet} view of this set, in the comparator's order: nothing is copied,
     * reads go through to this set, which never changes, and every mutator of the view throws
     * {@link UnsupportedOperationException}, {@code pollFirst} and {@code pollLast} included. The sub-sets, the
     * head and tail sets and the descending set are views too. A mutable copy is
     * {@code new java.util.TreeSet<>(set.asJava())}.
     * <p>
     * Complexity: O(1): nothing is copied. On the view, {@code contains}, {@code first}, {@code ceiling} and the other
     * navigation methods are O(log n).
     *
     * @return an unmodifiable {@code java.util.NavigableSet} view
     */
    @Override
    java.util.NavigableSet<T> asJava();

    /**
     * Same as {@link #flatMap(Function)} but using a specific comparator for values of the codomain of the given
     * {@code mapper}.
     *
     * <p>
     * Complexity: O(n + k log k) for k elements produced by {@code mapper}: they are sorted, then the new tree is built
     * in one pass; O(n + k) when they come out in order.
     *
     * @param comparator A comparator for values of type U
     * @param mapper     A function which maps values of type T to Iterables of values of type U
     * @param <U>        Type of flat-mapped values
     * @return A new Set instance containing mapped values
     */
    <U extends @Nullable Object> SortedSet<U> flatMap(Comparator<? super U> comparator, Function<? super T, ? extends Iterable<? extends U>> mapper);

    /**
     * Same as {@link #map(Function)} but using a specific comparator for values of the codomain of the given
     * {@code mapper}.
     *
     * <p>
     * Complexity: O(n log n): the results are sorted, then the new tree is built in one pass; O(n) when {@code mapper}
     * keeps the order.
     *
     * @param comparator A comparator for values of type U
     * @param mapper     A function which maps values of type T to values of type U
     * @param <U>        Type of mapped values
     * @return A new Set instance containing mapped values
     */
    <U extends @Nullable Object> SortedSet<U> map(Comparator<? super U> comparator, Function<? super T, ? extends U> mapper);

    // -- Positional operations, in the comparator's order

    /**
     * The first element in the comparator's order: the minimum.
     * <p>
     * Complexity: O(log n): the smallest element is found by walking down the tree, with no comparison.
     *
     * @return the minimum
     * @throws java.util.NoSuchElementException if this set is empty
     */
    T head();

    /**
     * The first element in the comparator's order, if any.
     *
     * <p>
     * Complexity: O(log n), as {@link #head()}.
     *
     * @return {@code Some} of the minimum, or {@code None} if this set is empty
     */
    Option<T> headOption();

    /**
     * The last element in the comparator's order: the maximum.
     * <p>
     * Complexity: O(log n): the largest element is found by walking down the tree, with no comparison.
     *
     * @return the maximum
     * @throws java.util.NoSuchElementException if this set is empty
     */
    T last();

    /**
     * The last element in the comparator's order, if any.
     *
     * <p>
     * Complexity: O(log n), as {@link #last()}.
     *
     * @return {@code Some} of the maximum, or {@code None} if this set is empty
     */
    Option<T> lastOption();

    /**
     * All elements but the last, with the same comparator.
     * <p>
     * Complexity: O(log n): the tree is cut without visiting the elements, and the result shares the rest of the
     * tree.
     *
     * @return this set without its maximum
     * @throws UnsupportedOperationException if this set is empty
     */
    SortedSet<T> init();

    /**
     * All elements but the last, if this set is not empty.
     * <p>
     * Complexity: O(log n), as {@link #init()}.
     *
     * @return {@code Some} of {@link #init()}, or {@code None} if this set is empty
     */
    Option<? extends SortedSet<T>> initOption();

    /**
     * All elements but the first, with the same comparator.
     * <p>
     * Complexity: O(log n): the tree is cut without visiting the elements, and the result shares the rest of the
     * tree.
     *
     * @return this set without its minimum
     * @throws UnsupportedOperationException if this set is empty
     */
    SortedSet<T> tail();

    /**
     * All elements but the first, if this set is not empty.
     * <p>
     * Complexity: O(log n), as {@link #tail()}.
     *
     * @return {@code Some} of {@link #tail()}, or {@code None} if this set is empty
     */
    Option<? extends SortedSet<T>> tailOption();

    /**
     * The first {@code n} elements in the comparator's order, with the same comparator: empty if {@code n <= 0},
     * this set if {@code n >= size()}.
     * <p>
     * Complexity: O(log n): the tree is cut at that position without visiting the elements, and the result shares
     * the rest of the tree.
     *
     * @param n the number of elements to keep
     * @return the {@code n} smallest elements
     */
    SortedSet<T> take(int n);

    /**
     * The last {@code n} elements in the comparator's order, with the same comparator: empty if {@code n <= 0},
     * this set if {@code n >= size()}.
     * <p>
     * Complexity: O(log n): the tree is cut at that position without visiting the elements, and the result shares
     * the rest of the tree.
     *
     * @param n the number of elements to keep
     * @return the {@code n} largest elements
     */
    SortedSet<T> takeRight(int n);

    /**
     * The longest prefix, in the comparator's order, of elements satisfying {@code predicate}.
     * <p>
     * Complexity: O(k + log n) for a prefix of k elements: the prefix is walked, then the tree is cut after it.
     *
     * @param predicate tested on the elements from the smallest
     * @return the elements before the first one not satisfying {@code predicate}
     * @throws NullPointerException if {@code predicate} is null
     */
    SortedSet<T> takeWhile(Predicate<? super T> predicate);

    /**
     * The longest prefix, in the comparator's order, of elements not satisfying {@code predicate}.
     * <p>
     * Complexity: O(k + log n) for a prefix of k elements: the prefix is walked, then the tree is cut after it.
     *
     * @param predicate tested on the elements from the smallest
     * @return the elements before the first one satisfying {@code predicate}
     * @throws NullPointerException if {@code predicate} is null
     */
    SortedSet<T> takeUntil(Predicate<? super T> predicate);

    /**
     * All elements but the first {@code n} in the comparator's order, with the same comparator: this set if
     * {@code n <= 0}, empty if {@code n >= size()}.
     * <p>
     * Complexity: O(log n): the tree is cut at that position without visiting the elements, and the result shares
     * the rest of the tree.
     *
     * @param n the number of elements to drop
     * @return the elements after the {@code n} smallest
     */
    SortedSet<T> drop(int n);

    /**
     * All elements but the last {@code n} in the comparator's order, with the same comparator: this set if
     * {@code n <= 0}, empty if {@code n >= size()}.
     * <p>
     * Complexity: O(log n): the tree is cut at that position without visiting the elements, and the result shares
     * the rest of the tree.
     *
     * @param n the number of elements to drop
     * @return the elements before the {@code n} largest
     */
    SortedSet<T> dropRight(int n);

    /**
     * The elements from the first one, in the comparator's order, that does not satisfy {@code predicate}.
     * <p>
     * Complexity: O(k + log n) for k dropped elements: they are walked, then the tree is cut after them.
     *
     * @param predicate tested on the elements from the smallest
     * @return the elements from the first one not satisfying {@code predicate}
     * @throws NullPointerException if {@code predicate} is null
     */
    SortedSet<T> dropWhile(Predicate<? super T> predicate);

    /**
     * The elements from the first one, in the comparator's order, that satisfies {@code predicate}.
     * <p>
     * Complexity: O(k + log n) for k dropped elements: they are walked, then the tree is cut after them.
     *
     * @param predicate tested on the elements from the smallest
     * @return the elements from the first one satisfying {@code predicate}
     * @throws NullPointerException if {@code predicate} is null
     */
    SortedSet<T> dropUntil(Predicate<? super T> predicate);

    /**
     * The elements paired with their rank in the comparator's order, from 0.
     * <p>
     * Complexity: O(n): one walk in order.
     *
     * @return the pairs (element, rank), in order
     */
    Vector<Tuple2<T, Integer>> zipWithIndex();

    /**
     * The blocks of {@code size} consecutive elements in the comparator's order, each a set with the same
     * comparator; the last block is smaller when {@code size} does not divide {@code size()}. The same as
     * {@code sliding(size, size)}.
     * <p>
     * Complexity: O((n / size) log n): the tree is cut once per block, in O(log n); the blocks share the tree's
     * structure, and the elements are not copied.
     *
     * @param size the block size, positive
     * @return the blocks, in order; empty if this set is empty
     * @throws IllegalArgumentException if {@code size} is not positive
     */
    Vector<? extends SortedSet<T>> grouped(int size);

    /**
     * The windows of {@code size} consecutive elements in the comparator's order, each starting one element after
     * the previous, each a set with the same comparator. The same as {@code sliding(size, 1)}.
     * <p>
     * Complexity: O(n log n): the tree is cut once per window, in O(log n); the windows share the tree's structure,
     * and the elements are not copied.
     *
     * @param size the window size, positive
     * @return the windows, in order; empty if this set is empty
     * @throws IllegalArgumentException if {@code size} is not positive
     */
    Vector<? extends SortedSet<T>> sliding(int size);

    /**
     * The windows of {@code size} consecutive elements in the comparator's order, each starting {@code step}
     * elements after the previous, each a set with the same comparator. The window rule is {@link Vector}'s: the
     * last window is shorter than {@code size} when it reaches the end, a window whose elements all belong to the
     * previous one is not produced, a set smaller than {@code size} is one window and an empty set has none.
     * <p>
     * Complexity: O((n / step) log n): the tree is cut once per window, in O(log n); the windows share the tree's
     * structure, and the elements are not copied.
     *
     * @param size the window size, positive
     * @param step the distance between two window starts, positive
     * @return the windows, in order
     * @throws IllegalArgumentException if {@code size} or {@code step} is not positive
     */
    Vector<? extends SortedSet<T>> sliding(int size, int step);

    /**
     * The maximal runs of consecutive elements, in the comparator's order, with the same key, computed once per
     * element by {@code classifier}; each run is a set with the same comparator, and the runs together are this set.
     * <p>
     * Complexity: O(n + k log n) for k runs: one walk finds them, then the tree is cut once per run, in O(log n).
     *
     * @param classifier the key of an element; two consecutive elements are in the same run when their keys are equal
     * @return the runs, in order; empty if this set is empty
     * @throws NullPointerException if {@code classifier} is null
     */
    Vector<? extends SortedSet<T>> slideBy(Function<? super T, ?> classifier);

    // -- Adjusted return types of Set methods

    /**
     * {@inheritDoc}
     * <p>
     * Complexity: O(log n): one lookup, then one insertion when the element is new.
     */
    @Override
    SortedSet<T> add(T element);

    /**
     * {@inheritDoc}
     * <p>
     * Complexity: O(m log(n + m)) for m elements: one lookup each, then one insertion for each new one.
     */
    @Override
    SortedSet<T> addAll(Iterable<? extends T> elements);

    /**
     * {@inheritDoc}
     * <p>
     * Complexity: O(n + m) for a set of m elements: they are put in a hash set, then the new tree is built from the
     * kept elements, which come in order, in one pass.
     * When {@code elements} is a TreeSet with an equal comparator (for a lambda, the same object), the cost is
     * O(m log(n / m + 1)), m then being the smaller of the two sizes: the two trees are cut and joined, not rebuilt.
     */
    @Override
    SortedSet<T> diff(Set<? extends T> elements);

    /**
     * {@inheritDoc}
     * <p>
     * Complexity: O(n): the new tree is built from the kept elements, which come in order, in one pass.
     */
    @Override
    SortedSet<T> filter(Predicate<? super T> predicate);

    /**
     * {@inheritDoc}
     * <p>
     * Complexity: O(n): the new tree is built from the kept elements, which come in order, in one pass.
     */
    @Override
    SortedSet<T> reject(Predicate<? super T> predicate);

    /**
     * {@inheritDoc}
     * <p>
     * Complexity: O(n + k log k) for k elements produced by {@code mapper}: they are sorted, then the new tree is built
     * in one pass; O(n + k) when they come out in order.
     */
    @Override
    <U extends @Nullable Object> SortedSet<U> flatMap(Function<? super T, ? extends Iterable<? extends U>> mapper);

    /**
     * {@inheritDoc}
     * <p>
     * Complexity: O(n): each group gets its elements in order, and its tree is built from them in one pass.
     */
    @Override
    <C extends @Nullable Object> Map<C, ? extends SortedSet<T>> groupBy(Function<? super T, ? extends C> classifier);

    /**
     * {@inheritDoc}
     * <p>
     * Complexity: O(n + m) for a set of m elements: they are put in a hash set, then the new tree is built from the
     * kept elements, which come in order, in one pass.
     * When {@code elements} is a TreeSet with an equal comparator (for a lambda, the same object), the cost is
     * O(m log(n / m + 1)), m then being the smaller of the two sizes: the two trees are cut and joined, not rebuilt.
     */
    @Override
    SortedSet<T> intersect(Set<? extends T> elements);

    /**
     * {@inheritDoc}
     * <p>
     * Complexity: O(n log n): the results are sorted, then the new tree is built in one pass; O(n) when {@code mapper}
     * keeps the order.
     */
    @Override
    <U extends @Nullable Object> SortedSet<U> map(Function<? super T, ? extends U> mapper);

    /**
     * {@inheritDoc}
     * <p>
     * Complexity: O(n log n): the collected elements are sorted, then the new tree is built in one pass; O(n) when
     * they come out in order.
     */
    @Override
    <U extends @Nullable Object> SortedSet<U> collect(Function<? super T, ? extends Option<? extends U>> mapper);

    @Override
    default <U extends @Nullable Object> SortedSet<U> as(U value) {
        return map((o1, o2) -> 0, ignored -> value);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Complexity: O(m log m) for the m elements of {@code other} when this set is empty: they are sorted, then the new
     * tree is built in one pass (O(m) when they come sorted); O(1) when this set is not empty.
     */
    @Override
    SortedSet<T> orElse(Iterable<? extends T> other);

    /**
     * {@inheritDoc}
     * <p>
     * Complexity: O(m log m) for the m supplied elements when this set is empty: they are sorted, then the new tree is
     * built in one pass (O(m) when they come sorted); O(1) when this set is not empty, and the supplier is not
     * called.
     */
    @Override
    SortedSet<T> orElse(Supplier<? extends Iterable<? extends T>> supplier);

    /**
     * {@inheritDoc}
     * <p>
     * Complexity: O(n): both results get their elements in order, and their trees are built from them in one pass.
     */
    @Override
    Tuple2<? extends SortedSet<T>, ? extends SortedSet<T>> partition(Predicate<? super T> predicate);

    @Override
    SortedSet<T> tap(Consumer<? super T> action);

    /**
     * {@inheritDoc}
     * <p>
     * Complexity: O(log n): one deletion from the tree.
     */
    @Override
    SortedSet<T> remove(T element);

    /**
     * {@inheritDoc}
     * <p>
     * Complexity: O(n + m) for m elements: they are put in a hash set, then the new tree is built from the kept
     * elements, which come in order, in one pass, even when a single element is removed.
     */
    @Override
    SortedSet<T> removeAll(Iterable<? extends T> elements);

    /**
     * {@inheritDoc}
     * <p>
     * Complexity: O(log n): one lookup, one deletion and one insertion.
     */
    @Override
    SortedSet<T> replace(T currentElement, T newElement);

    /**
     * {@inheritDoc}
     * <p>
     * Complexity: O(log n), as {@link #replace(Object, Object)}: a set holds an element once.
     */
    @Override
    SortedSet<T> replaceAll(T currentElement, T newElement);

    /**
     * {@inheritDoc}
     * <p>
     * Complexity: O(n + m) for m elements: they are put in a hash set, then the new tree is built from the kept
     * elements, which come in order, in one pass.
     */
    @Override
    SortedSet<T> retainAll(Iterable<? extends T> elements);

    /**
     * {@inheritDoc}
     * <p>
     * Complexity: O(m log(n + m)) for a set of m elements: one lookup each, then one insertion for each new one.
     * When {@code elements} is a TreeSet with an equal comparator (for a lambda, the same object), the cost is
     * O(m log(n / m + 1)), m then being the smaller of the two sizes: the two trees are cut and joined, not rebuilt.
     */
    @Override
    SortedSet<T> union(Set<? extends T> elements);
}
