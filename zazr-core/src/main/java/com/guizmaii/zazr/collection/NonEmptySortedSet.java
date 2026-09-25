package com.guizmaii.zazr.collection;

import com.guizmaii.zazr.Tuple2;
import com.guizmaii.zazr.collection.internal.Comparators;
import com.guizmaii.zazr.collection.internal.NonEmptyModule;
import com.guizmaii.zazr.control.Option;
import java.util.Comparator;
import java.util.Objects;
import java.util.Spliterator;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collector;
import org.jspecify.annotations.Nullable;

/**
 * A {@link TreeSet} with at least one element (zio-prelude's {@code NonEmptySortedSet}): a wrapper, not a subtype, so
 * that every method can state in its return type whether the result is still non-empty.
 * <p>
 * The contract is {@link NonEmptySet}'s, plus the positional operations of a sorted set:
 * <ul>
 * <li>operations that cannot remove every element return a {@code NonEmptySortedSet}: {@code add}, {@code addAll},
 * {@code union}, {@code map}, {@code flatMap}, {@code as}, {@code replace}, {@code replaceAll}, {@code tap};
 * {@code grouped}, {@code sliding}, {@code slideBy} and {@code groupBy} return non-empty groups, and
 * {@code zipWithIndex} a {@link NonEmptyVector};</li>
 * <li>operations that can shrink return a {@link TreeSet} with the same comparator: {@code filter}, {@code reject},
 * {@code collect}, {@code flatMapAll}, {@code remove}, {@code removeAll}, {@code retainAll}, {@code intersect},
 * {@code diff}, {@code partition}, {@code tail}, {@code init}, {@code take*}, {@code drop*};</li>
 * <li>operations that are partial on a {@code TreeSet} are total here: {@code head}, {@code last}, {@code max},
 * {@code min}, {@code maxBy}, {@code minBy}, {@code reduce}, {@code reduceMap}, {@code average};</li>
 * <li>narrowing back to a {@code NonEmptySortedSet} returns an {@link Option}: {@code tailNonEmpty},
 * {@code initNonEmpty}, {@link #fromSortedSet(TreeSet)}, {@link TreeSet#toNonEmptySortedSet()}.</li>
 * </ul>
 * {@code head} and {@code last} are the least and the greatest element in the comparator's order; {@code min} and
 * {@code max} use the natural order of the elements, as on every set.
 * <p>
 * Like every collection, it rejects null elements. Equal to another {@code NonEmptySortedSet} or {@code NonEmptySet}
 * with the same elements, as sets are equal to sets, never to a plain {@link Set}; use {@link #toSortedSet()} to
 * compare across the two.
 *
 * @param <A> Component type.
 */
public final class NonEmptySortedSet<A extends @Nullable Object> implements Iterable<A> {

    private final TreeSet<A> set;

    private NonEmptySortedSet(TreeSet<A> set) { this.set = set; }

    // -- constructors

    /**
     * A {@code NonEmptySortedSet} of {@code head} and the elements of {@code tail}, in their natural order.
     *
     * @param head An element
     * @param tail The other elements
     * @param <A>  Component type
     * @return a non-empty sorted set
     * @throws NullPointerException if {@code head}, {@code tail} or an element of {@code tail} is null
     */
    @SafeVarargs
    @SuppressWarnings("varargs")
    public static <A extends Comparable<? super A>> NonEmptySortedSet<A> of(A head, A... tail) {
        return of(Comparators.naturalComparator(), head, tail);
    }

    /**
     * A {@code NonEmptySortedSet} of {@code head} and the elements of {@code tail}, ordered by {@code comparator}.
     *
     * @param comparator The order
     * @param head       An element
     * @param tail       The other elements
     * @param <A>        Component type
     * @return a non-empty sorted set
     * @throws NullPointerException if {@code comparator}, {@code head}, {@code tail} or an element of {@code tail} is
     *                              null
     */
    @SafeVarargs
    @SuppressWarnings("varargs")
    public static <A extends @Nullable Object> NonEmptySortedSet<A> of(Comparator<? super A> comparator, A head, A... tail) {
        Objects.requireNonNull(comparator, "comparator is null");
        Objects.requireNonNull(head, "NonEmptySortedSet: head is null");
        Objects.requireNonNull(tail, "NonEmptySortedSet: tail is null");
        final TreeSet.Builder<A> builder = TreeSet.newBuilder(comparator);
        builder.add(head);
        for (A element : tail) {
            builder.add(Objects.requireNonNull(element, "NonEmptySortedSet: element is null"));
        }
        return new NonEmptySortedSet<>(builder.result());
    }

    /**
     * A {@code NonEmptySortedSet} of {@code head} and the elements of {@code tail}, in their natural order. Not an
     * overload of {@link #of(Comparable, Comparable[])}, for the reason given on {@link NonEmptySet}.
     *
     * @param head An element
     * @param tail The other elements, read once
     * @param <A>  Component type
     * @return a non-empty sorted set
     * @throws NullPointerException if {@code head}, {@code tail} or an element of {@code tail} is null
     */
    public static <A extends Comparable<? super A>> NonEmptySortedSet<A> fromIterable(A head, Iterable<? extends A> tail) {
        return fromIterable(Comparators.naturalComparator(), head, tail);
    }

    /**
     * A {@code NonEmptySortedSet} of {@code head} and the elements of {@code tail}, ordered by {@code comparator}.
     *
     * @param comparator The order
     * @param head       An element
     * @param tail       The other elements, read once
     * @param <A>        Component type
     * @return a non-empty sorted set
     * @throws NullPointerException if {@code comparator}, {@code head}, {@code tail} or an element of {@code tail} is
     *                              null
     */
    public static <A extends @Nullable Object> NonEmptySortedSet<A> fromIterable(Comparator<? super A> comparator, A head, Iterable<? extends A> tail) {
        Objects.requireNonNull(comparator, "comparator is null");
        Objects.requireNonNull(head, "NonEmptySortedSet: head is null");
        Objects.requireNonNull(tail, "NonEmptySortedSet: tail is null");
        final TreeSet.Builder<A> builder = TreeSet.newBuilder(comparator);
        builder.add(head);
        return new NonEmptySortedSet<>(addAll(builder, tail).result());
    }

    /**
     * A {@code NonEmptySortedSet} of one element, in natural order.
     *
     * @param element The element
     * @param <A>     Component type
     * @return a non-empty sorted set of size 1
     * @throws NullPointerException if {@code element} is null
     */
    public static <A extends Comparable<? super A>> NonEmptySortedSet<A> single(A element) {
        return single(Comparators.naturalComparator(), element);
    }

    /**
     * A {@code NonEmptySortedSet} of one element, ordered by {@code comparator}.
     *
     * @param comparator The order
     * @param element    The element
     * @param <A>        Component type
     * @return a non-empty sorted set of size 1
     * @throws NullPointerException if {@code comparator} or {@code element} is null
     */
    public static <A extends @Nullable Object> NonEmptySortedSet<A> single(Comparator<? super A> comparator, A element) {
        Objects.requireNonNull(comparator, "comparator is null");
        Objects.requireNonNull(element, "NonEmptySortedSet: element is null");
        return new NonEmptySortedSet<>(TreeSet.of(comparator, element));
    }

    /**
     * Wraps a {@link TreeSet} if it is not empty; the comparator is the set's.
     * <p>
     * Complexity: O(1).
     *
     * @param set A sorted set
     * @param <A> Component type
     * @return {@code Some(nonEmptySortedSet)} sharing {@code set}'s elements, or {@code None} if {@code set} is empty
     * @throws NullPointerException if {@code set} is null
     */
    public static <A extends @Nullable Object> Option<NonEmptySortedSet<A>> fromSortedSet(TreeSet<A> set) {
        Objects.requireNonNull(set, "NonEmptySortedSet.fromSortedSet: set is null");
        return set.isEmpty() ? Option.none() : Option.some(new NonEmptySortedSet<>(set));
    }

    /**
     * Copies an {@link Iterable} in natural order if it yields at least one element.
     *
     * @param iterable The elements, read once
     * @param <A>      Component type
     * @return {@code Some(nonEmptySortedSet)} of the distinct elements, or {@code None} if {@code iterable} is empty
     * @throws NullPointerException if {@code iterable} or an element is null
     */
    public static <A extends Comparable<? super A>> Option<NonEmptySortedSet<A>> fromIterable(Iterable<? extends A> iterable) {
        return fromIterable(Comparators.naturalComparator(), iterable);
    }

    /**
     * Copies an {@link Iterable}, ordered by {@code comparator}, if it yields at least one element.
     *
     * @param comparator The order
     * @param iterable   The elements, read once
     * @param <A>        Component type
     * @return {@code Some(nonEmptySortedSet)} of the distinct elements, or {@code None} if {@code iterable} is empty
     * @throws NullPointerException if {@code comparator}, {@code iterable} or an element is null
     */
    public static <A extends @Nullable Object> Option<NonEmptySortedSet<A>> fromIterable(Comparator<? super A> comparator, Iterable<? extends A> iterable) {
        Objects.requireNonNull(comparator, "comparator is null");
        Objects.requireNonNull(iterable, "NonEmptySortedSet.fromIterable: iterable is null");
        return fromSortedSet(addAll(TreeSet.<A> newBuilder(comparator), iterable).result());
    }

    /**
     * Wraps a {@link TreeSet} known to be non-empty.
     * <p>
     * Complexity: O(1).
     *
     * @param set A non-empty sorted set
     * @param <A> Component type
     * @return a non-empty sorted set sharing {@code set}'s elements
     * @throws IllegalArgumentException if {@code set} is empty
     * @throws NullPointerException     if {@code set} is null
     */
    public static <A extends @Nullable Object> NonEmptySortedSet<A> unsafeFromSortedSet(TreeSet<A> set) {
        Objects.requireNonNull(set, "NonEmptySortedSet.unsafeFromSortedSet: set is null");
        if (set.isEmpty()) {
            throw new IllegalArgumentException("NonEmptySortedSet.unsafeFromSortedSet: set is empty");
        }
        return new NonEmptySortedSet<>(set);
    }

    /**
     * The union of a non-empty set of non-empty sets, in natural order.
     * <p>
     * Complexity: O(n log n) comparisons for n inner elements in total, that of {@link TreeSet#flatten(Iterable)}.
     *
     * @param nested Non-empty sorted sets
     * @param <A>    Component type of the inner sets
     * @return the elements of every inner set
     * @throws NullPointerException if {@code nested} is null
     */
    public static <A extends Comparable<? super A>> NonEmptySortedSet<A> flatten(NonEmptySortedSet<? extends NonEmptySortedSet<? extends A>> nested) {
        return flatten(Comparators.naturalComparator(), nested);
    }

    /**
     * The union of a non-empty set of non-empty sets, ordered by {@code comparator}.
     * <p>
     * Complexity: O(n log n) comparisons for n inner elements in total, that of
     * {@link TreeSet#flatten(Comparator, Iterable)}.
     *
     * @param comparator The order of the result
     * @param nested     Non-empty sorted sets
     * @param <A>        Component type of the inner sets
     * @return the elements of every inner set
     * @throws NullPointerException if {@code comparator} or {@code nested} is null
     */
    public static <A extends @Nullable Object> NonEmptySortedSet<A> flatten(Comparator<? super A> comparator, NonEmptySortedSet<? extends NonEmptySortedSet<? extends A>> nested) {
        return new NonEmptySortedSet<>(TreeSet.flatten(comparator, nested));
    }

    /* a TreeSet cannot hold a null, so it is added as is; anything else is checked element by element, naming this type */
    private static <A extends @Nullable Object> TreeSet.Builder<A> addAll(TreeSet.Builder<A> builder, Iterable<? extends A> elements) {
        if (elements instanceof TreeSet) {
            return builder.addAll(elements);
        }
        for (A element : elements) {
            builder.add(Objects.requireNonNull(element, "NonEmptySortedSet: element is null"));
        }
        return builder;
    }

    // -- returns NonEmptySortedSet: at least one element is left

    /**
     * Complexity: O(log n), that of {@link TreeSet#add(Object)}.
     *
     * @param element An element
     * @return this set with {@code element}
     * @throws NullPointerException if {@code element} is null
     */
    public NonEmptySortedSet<A> add(A element) {
        Objects.requireNonNull(element, "NonEmptySortedSet.add: element is null");
        return wrap(set.add(element));
    }

    /**
     * Accepts the possibly empty type and returns the non-empty one; a non-empty set is an {@code Iterable} too.
     * <p>
     * Complexity: O(m log(n + m)) for m elements, that of {@link TreeSet#addAll(Iterable)}.
     *
     * @param elements Elements to add, possibly none, read once
     * @return this set with {@code elements}
     * @throws NullPointerException if {@code elements} or one of them is null
     */
    public NonEmptySortedSet<A> addAll(Iterable<? extends A> elements) { return wrap(set.addAll(elements)); }

    /**
     * Complexity: O(m log(n + m)) for a set of m elements, that of {@link TreeSet#union(Set)}.
     *
     * @param elements A set, possibly empty
     * @return the elements of both sets, with this set's comparator
     * @throws NullPointerException if {@code elements} is null
     */
    public NonEmptySortedSet<A> union(Set<? extends A> elements) { return wrap(set.union(elements)); }

    /**
     * @param mapper A function
     * @param <B>    Component type of the result
     * @return the distinct results of {@code mapper}, in their natural order
     * @throws NullPointerException if {@code mapper} is null or returns null
     * @throws ClassCastException   if the results are not {@link Comparable}
     */
    public <B extends @Nullable Object> NonEmptySortedSet<B> map(Function<? super A, ? extends B> mapper) {
        return new NonEmptySortedSet<>(set.map(mapper));
    }

    /**
     * @param comparator The order of the result
     * @param mapper     A function
     * @param <B>        Component type of the result
     * @return the distinct results of {@code mapper}, ordered by {@code comparator}
     * @throws NullPointerException if {@code comparator} or {@code mapper} is null, or {@code mapper} returns null
     */
    public <B extends @Nullable Object> NonEmptySortedSet<B> map(Comparator<? super B> comparator, Function<? super A, ? extends B> mapper) {
        return new NonEmptySortedSet<>(set.map(comparator, mapper));
    }

    /**
     * The union, in natural order, of the non-empty sets {@code mapper} returns for each element. For a function that
     * returns any {@code Iterable}, see {@link #flatMapAll(Function)}.
     *
     * @param mapper A function returning a non-empty sorted set
     * @param <B>    Component type of the result
     * @return the union of the results
     * @throws NullPointerException if {@code mapper} is null or returns null
     * @throws ClassCastException   if the results are not {@link Comparable}
     */
    public <B extends @Nullable Object> NonEmptySortedSet<B> flatMap(Function<? super A, ? extends NonEmptySortedSet<? extends B>> mapper) {
        return flatMap(Comparators.naturalComparator(), mapper);
    }

    /**
     * The union, ordered by {@code comparator}, of the non-empty sets {@code mapper} returns for each element.
     *
     * @param comparator The order of the result
     * @param mapper     A function returning a non-empty sorted set
     * @param <B>        Component type of the result
     * @return the union of the results
     * @throws NullPointerException if {@code comparator} or {@code mapper} is null, or {@code mapper} returns null
     */
    public <B extends @Nullable Object> NonEmptySortedSet<B> flatMap(Comparator<? super B> comparator, Function<? super A, ? extends NonEmptySortedSet<? extends B>> mapper) {
        Objects.requireNonNull(comparator, "comparator is null");
        Objects.requireNonNull(mapper, "mapper is null");
        final TreeSet.Builder<B> builder = TreeSet.newBuilder(comparator);
        for (A element : set) {
            builder.addAll(Objects.requireNonNull(mapper.apply(element), "NonEmptySortedSet.flatMap: mapper returned null").set);
        }
        return new NonEmptySortedSet<>(builder.result());
    }

    /**
     * @param value The element of the result
     * @param <B>   Component type of the result
     * @return the set of {@code value} alone, in natural order
     * @throws NullPointerException if {@code value} is null
     */
    public <B extends @Nullable Object> NonEmptySortedSet<B> as(B value) {
        Objects.requireNonNull(value, "NonEmptySortedSet.as: value is null");
        return new NonEmptySortedSet<>(TreeSet.of(Comparators.naturalComparator(), value));
    }

    /**
     * Complexity: O(log n), that of {@link TreeSet#replace(Object, Object)}.
     *
     * @param currentElement An element
     * @param newElement     Its replacement
     * @return this set with {@code newElement} instead of {@code currentElement}, or this set if {@code currentElement}
     *         is absent
     * @throws NullPointerException if {@code currentElement} or {@code newElement} is null
     */
    public NonEmptySortedSet<A> replace(A currentElement, A newElement) {
        Objects.requireNonNull(currentElement, "NonEmptySortedSet.replace: currentElement is null");
        Objects.requireNonNull(newElement, "NonEmptySortedSet.replace: newElement is null");
        return wrap(set.replace(currentElement, newElement));
    }

    /**
     * Complexity: O(log n), that of {@link #replace(Object, Object)}: a set holds an element once.
     *
     * @param currentElement An element
     * @param newElement     Its replacement
     * @return the same as {@link #replace(Object, Object)}
     * @throws NullPointerException if {@code currentElement} or {@code newElement} is null
     */
    public NonEmptySortedSet<A> replaceAll(A currentElement, A newElement) { return replace(currentElement, newElement); }

    /**
     * Runs {@code action} on every element, in order.
     *
     * @param action A side effect
     * @return this set
     * @throws NullPointerException if {@code action} is null
     */
    public NonEmptySortedSet<A> tap(Consumer<? super A> action) {
        set.tap(action);
        return this;
    }

    /**
     * Groups the elements by the key {@code classifier} computes; each group keeps this set's comparator.
     *
     * @param classifier Computes the key of an element
     * @param <K>        Key type
     * @return the groups, each non-empty
     * @throws NullPointerException if {@code classifier} is null or returns null
     */
    public <K extends @Nullable Object> HashMap<K, NonEmptySortedSet<A>> groupBy(Function<? super A, ? extends K> classifier) {
        final HashMap.Builder<K, NonEmptySortedSet<A>> groups = HashMap.newBuilder();
        for (Tuple2<K, TreeSet<A>> group : set.<K> groupBy(classifier)) {
            groups.put(group._1(), new NonEmptySortedSet<>(group._2()));
        }
        return groups.result();
    }

    /**
     * Complexity: O((n / size) log n), that of {@link TreeSet#grouped(int)}.
     *
     * @param size The block size
     * @return the blocks of {@code size} consecutive elements, each non-empty; only the last may be smaller
     * @throws IllegalArgumentException if {@code size} is not positive
     */
    public Vector<NonEmptySortedSet<A>> grouped(int size) { return set.grouped(size).map(NonEmptySortedSet::new); }

    /**
     * Complexity: O(n log n), that of {@link TreeSet#sliding(int)}.
     *
     * @param size The window size
     * @return the windows of {@code size} consecutive elements, each non-empty
     * @throws IllegalArgumentException if {@code size} is not positive
     */
    public Vector<NonEmptySortedSet<A>> sliding(int size) { return set.sliding(size).map(NonEmptySortedSet::new); }

    /**
     * Complexity: O((n / step) log n), that of {@link TreeSet#sliding(int, int)}.
     *
     * @param size The window size
     * @param step The distance between two window starts
     * @return the windows, each non-empty, with {@link Vector}'s window rule
     * @throws IllegalArgumentException if {@code size} or {@code step} is not positive
     */
    public Vector<NonEmptySortedSet<A>> sliding(int size, int step) { return set.sliding(size, step).map(NonEmptySortedSet::new); }

    /**
     * Complexity: O(n + r log n) for r runs, that of {@link TreeSet#slideBy(Function)}.
     *
     * @param classifier The key of an element
     * @return the maximal runs of consecutive elements with the same key, each non-empty
     * @throws NullPointerException if {@code classifier} is null
     */
    public Vector<NonEmptySortedSet<A>> slideBy(Function<? super A, ?> classifier) {
        return set.slideBy(classifier).map(NonEmptySortedSet::new);
    }

    /**
     * Complexity: O(n), that of {@link TreeSet#zipWithIndex()}.
     *
     * @return the elements paired with their rank in the comparator's order, from 0
     */
    public NonEmptyVector<Tuple2<A, Integer>> zipWithIndex() { return NonEmptyVector.unsafeFromVector(set.zipWithIndex()); }

    /* the plain operations that cannot empty a non-empty set return the same instance when nothing changes */
    private NonEmptySortedSet<A> wrap(TreeSet<A> result) { return result == set ? this : new NonEmptySortedSet<>(result); }

    // -- returns TreeSet: the result may be empty

    /**
     * Complexity: O(1).
     *
     * @return the wrapped set
     */
    public TreeSet<A> toSortedSet() { return set; }

    /**
     * @return the order of the elements
     */
    public Comparator<A> comparator() { return set.comparator(); }

    /**
     * @param predicate A test
     * @return the elements that pass {@code predicate}
     * @throws NullPointerException if {@code predicate} is null
     */
    public TreeSet<A> filter(Predicate<? super A> predicate) { return set.filter(predicate); }

    /**
     * @param predicate A test
     * @return the elements that fail {@code predicate}
     * @throws NullPointerException if {@code predicate} is null
     */
    public TreeSet<A> reject(Predicate<? super A> predicate) { return set.reject(predicate); }

    /**
     * @param mapper A function returning an {@link Option}
     * @param <B>    Component type of the result
     * @return the defined results, in their natural order
     * @throws NullPointerException if {@code mapper} is null or returns null
     */
    public <B extends @Nullable Object> TreeSet<B> collect(Function<? super A, ? extends Option<? extends B>> mapper) {
        return set.collect(mapper);
    }

    /**
     * @param comparator The order of the result
     * @param mapper     A function returning an {@link Option}
     * @param <B>        Component type of the result
     * @return the defined results, ordered by {@code comparator}
     * @throws NullPointerException if an argument is null or {@code mapper} returns null
     */
    public <B extends @Nullable Object> TreeSet<B> collect(Comparator<? super B> comparator, Function<? super A, ? extends Option<? extends B>> mapper) {
        return set.collect(comparator, mapper);
    }

    /**
     * The union, in natural order, of the iterables {@code mapper} returns; they may be empty, so the result may be too.
     *
     * @param mapper A function returning an {@link Iterable}
     * @param <B>    Component type of the result
     * @return the union of the results
     * @throws NullPointerException if {@code mapper} is null or returns null
     */
    public <B extends @Nullable Object> TreeSet<B> flatMapAll(Function<? super A, ? extends Iterable<? extends B>> mapper) {
        return set.flatMap(mapper);
    }

    /**
     * The union, ordered by {@code comparator}, of the iterables {@code mapper} returns.
     *
     * @param comparator The order of the result
     * @param mapper     A function returning an {@link Iterable}
     * @param <B>        Component type of the result
     * @return the union of the results
     * @throws NullPointerException if an argument is null or {@code mapper} returns null
     */
    public <B extends @Nullable Object> TreeSet<B> flatMapAll(Comparator<? super B> comparator, Function<? super A, ? extends Iterable<? extends B>> mapper) {
        return set.flatMap(comparator, mapper);
    }

    /**
     * Complexity: O(log n), that of {@link TreeSet#remove(Object)}.
     *
     * @param element An element
     * @return this set without {@code element}
     */
    public TreeSet<A> remove(A element) { return set.remove(element); }

    /**
     * Complexity: O(m + n log n) for m given elements, that of {@link TreeSet#removeAll(Iterable)}.
     *
     * @param elements Elements
     * @return this set without {@code elements}
     * @throws NullPointerException if {@code elements} is null
     */
    public TreeSet<A> removeAll(Iterable<? extends A> elements) { return set.removeAll(elements); }

    /**
     * Complexity: O(m + n log n) for m given elements, that of {@link TreeSet#retainAll(Iterable)}.
     *
     * @param elements Elements
     * @return the elements of this set that are among {@code elements}
     * @throws NullPointerException if {@code elements} is null
     */
    public TreeSet<A> retainAll(Iterable<? extends A> elements) { return set.retainAll(elements); }

    /**
     * Complexity: O((n + m) log n) for a set of m elements, that of {@link TreeSet#intersect(Set)}.
     *
     * @param elements A set
     * @return the elements in both sets
     * @throws NullPointerException if {@code elements} is null
     */
    public TreeSet<A> intersect(Set<? extends A> elements) { return set.intersect(elements); }

    /**
     * Complexity: O((n + m) log n) for a set of m elements, that of {@link TreeSet#diff(Set)}.
     *
     * @param elements A set
     * @return the elements of this set that are not in {@code elements}
     * @throws NullPointerException if {@code elements} is null
     */
    public TreeSet<A> diff(Set<? extends A> elements) { return set.diff(elements); }

    /**
     * @param predicate A test
     * @return the elements that pass {@code predicate} and those that fail it; either may be empty
     * @throws NullPointerException if {@code predicate} is null
     */
    public Tuple2<TreeSet<A>, TreeSet<A>> partition(Predicate<? super A> predicate) { return set.partition(predicate); }

    /**
     * Complexity: O(log n), that of {@link TreeSet#tail()}.
     *
     * @return all elements but the first; empty when {@code size()} is 1. See {@link #tailNonEmpty()}.
     */
    public TreeSet<A> tail() { return set.tail(); }

    /**
     * Complexity: O(log n), that of {@link TreeSet#init()}.
     *
     * @return all elements but the last; empty when {@code size()} is 1. See {@link #initNonEmpty()}.
     */
    public TreeSet<A> init() { return set.init(); }

    /**
     * Complexity: O(log n), that of {@link TreeSet#take(int)}.
     *
     * @param n A count
     * @return the {@code n} smallest elements; none if {@code n <= 0}
     */
    public TreeSet<A> take(int n) { return set.take(n); }

    /**
     * Complexity: O(log n), that of {@link TreeSet#takeRight(int)}.
     *
     * @param n A count
     * @return the {@code n} largest elements; none if {@code n <= 0}
     */
    public TreeSet<A> takeRight(int n) { return set.takeRight(n); }

    /**
     * Complexity: O(k + log n) for a prefix of k elements, that of {@link TreeSet#takeWhile(Predicate)}.
     *
     * @param predicate A test
     * @return the leading elements that pass {@code predicate}
     * @throws NullPointerException if {@code predicate} is null
     */
    public TreeSet<A> takeWhile(Predicate<? super A> predicate) { return set.takeWhile(predicate); }

    /**
     * Complexity: O(k + log n) for a prefix of k elements, that of {@link TreeSet#takeUntil(Predicate)}.
     *
     * @param predicate A test
     * @return the leading elements that fail {@code predicate}
     * @throws NullPointerException if {@code predicate} is null
     */
    public TreeSet<A> takeUntil(Predicate<? super A> predicate) { return set.takeUntil(predicate); }

    /**
     * Complexity: O(log n), that of {@link TreeSet#drop(int)}.
     *
     * @param n A count
     * @return all elements but the {@code n} smallest; all of them if {@code n <= 0}
     */
    public TreeSet<A> drop(int n) { return set.drop(n); }

    /**
     * Complexity: O(log n), that of {@link TreeSet#dropRight(int)}.
     *
     * @param n A count
     * @return all elements but the {@code n} largest; all of them if {@code n <= 0}
     */
    public TreeSet<A> dropRight(int n) { return set.dropRight(n); }

    /**
     * Complexity: O(k + log n) for k dropped elements, that of {@link TreeSet#dropWhile(Predicate)}.
     *
     * @param predicate A test
     * @return the elements from the first one that fails {@code predicate} on
     * @throws NullPointerException if {@code predicate} is null
     */
    public TreeSet<A> dropWhile(Predicate<? super A> predicate) { return set.dropWhile(predicate); }

    /**
     * Complexity: O(k + log n) for k dropped elements, that of {@link TreeSet#dropUntil(Predicate)}.
     *
     * @param predicate A test
     * @return the elements from the first one that passes {@code predicate} on
     * @throws NullPointerException if {@code predicate} is null
     */
    public TreeSet<A> dropUntil(Predicate<? super A> predicate) { return set.dropUntil(predicate); }

    // -- total: what is partial on a TreeSet

    /**
     * Complexity: O(log n), that of {@link TreeSet#head()}.
     *
     * @return the least element in the comparator's order
     */
    public A head() { return set.head(); }

    /**
     * Complexity: O(log n), that of {@link TreeSet#last()}.
     *
     * @return the greatest element in the comparator's order
     */
    public A last() { return set.last(); }

    /**
     * The greatest element in the natural order of the elements, as {@link TreeSet#max()}; the comparator is not
     * consulted, {@link #last()} is the greatest element in its order.
     * <p>
     * Complexity: O(n), every element compared once.
     *
     * @return the greatest element; {@code NaN} compares as the greatest {@code Double} or {@code Float}
     * @throws ClassCastException if the elements are not {@link Comparable}
     */
    public A max() { return NonEmptyModule.max(set); }

    /**
     * The least element in the natural order of the elements, as {@link TreeSet#min()}; the comparator is not
     * consulted, {@link #head()} is the least element in its order.
     * <p>
     * Complexity: O(n), every element compared once.
     *
     * @return the least element; among {@code Double}s or {@code Float}s, a {@code NaN} whenever one is present
     * @throws ClassCastException if the elements are not {@link Comparable}
     */
    public A min() { return NonEmptyModule.min(set); }

    /**
     * @param comparator The order
     * @return the greatest element under {@code comparator}; of several, the first in this set's order
     * @throws NullPointerException if {@code comparator} is null
     */
    public A maxBy(Comparator<? super A> comparator) { return NonEmptyModule.max(set, comparator); }

    /**
     * @param comparator The order
     * @return the least element under {@code comparator}; of several, the first in this set's order
     * @throws NullPointerException if {@code comparator} is null
     */
    public A minBy(Comparator<? super A> comparator) { return NonEmptyModule.min(set, comparator); }

    /**
     * @param f   Computes the key, once per element
     * @param <U> Key type
     * @return the element with the greatest key; of several, the first in this set's order
     * @throws NullPointerException if {@code f} is null
     */
    public <U extends Comparable<? super U>> A maxBy(Function<? super A, ? extends U> f) { return NonEmptyModule.maxBy(set, f); }

    /**
     * @param f   Computes the key, once per element
     * @param <U> Key type
     * @return the element with the least key; of several, the first in this set's order
     * @throws NullPointerException if {@code f} is null
     */
    public <U extends Comparable<? super U>> A minBy(Function<? super A, ? extends U> f) { return NonEmptyModule.minBy(set, f); }

    /**
     * @param op Combines two elements
     * @return the elements combined from the left, in the comparator's order: {@code op(op(a0, a1), a2)...}
     * @throws NullPointerException if {@code op} is null
     */
    public A reduce(BiFunction<? super A, ? super A, ? extends A> op) { return NonEmptyModule.reduce(set, op); }

    /**
     * Maps every element and combines the results from the left, in one pass.
     *
     * @param mapper Maps an element
     * @param op     Combines two mapped values
     * @param <B>    Result type
     * @return {@code op(op(mapper(a0), mapper(a1)), mapper(a2))...}
     * @throws NullPointerException if {@code mapper} or {@code op} is null
     */
    public <B extends @Nullable Object> B reduceMap(Function<? super A, ? extends B> mapper, BiFunction<? super B, ? super B, ? extends B> op) {
        return NonEmptyModule.reduceMap(set, mapper, op);
    }

    /**
     * @param zero    The neutral element of {@code combine}
     * @param combine Combines two elements
     * @return the elements folded from the left, in the comparator's order, starting from {@code zero}
     * @throws NullPointerException if {@code combine} is null
     */
    public A fold(A zero, BiFunction<? super A, ? super A, ? extends A> combine) { return set.fold(zero, combine); }

    /**
     * @return the sum of the elements, which must be {@link Number}s, with the arithmetic of {@link TreeSet#sum()}
     * @throws UnsupportedOperationException if an element is not a {@code Number}
     */
    public Number sum() { return set.sum(); }

    /**
     * @return the product of the elements, which must be {@link Number}s, with the arithmetic of
     *         {@link TreeSet#product()}
     * @throws UnsupportedOperationException if an element is not a {@code Number}
     */
    public Number product() { return set.product(); }

    /**
     * The average of the elements, which must be {@link Number}s: the value {@link TreeSet#average()} holds.
     *
     * @return the average
     * @throws UnsupportedOperationException if an element is not a {@code Number}
     */
    public double average() { return NonEmptyModule.average(set); }

    /**
     * @return the only element
     * @throws java.util.NoSuchElementException if there is more than one element
     */
    public A single() { return set.single(); }

    /**
     * @return the number of elements, at least 1
     */
    public int size() { return set.size(); }

    /**
     * Complexity: O(log n), that of {@link TreeSet#contains(Object)}.
     *
     * @param element An element
     * @return whether {@code element} is in this set
     */
    public boolean contains(A element) { return set.contains(element); }

    /**
     * @param elements Elements
     * @return whether every one of {@code elements} is in this set
     * @throws NullPointerException if {@code elements} is null
     */
    public boolean containsAll(Iterable<? extends A> elements) { return set.containsAll(elements); }

    /**
     * @param predicate A test
     * @return whether at least one element passes {@code predicate}
     * @throws NullPointerException if {@code predicate} is null
     */
    public boolean exists(Predicate<? super A> predicate) { return set.exists(predicate); }

    /**
     * @param predicate A test
     * @return whether exactly one element passes {@code predicate}
     * @throws NullPointerException if {@code predicate} is null
     */
    public boolean existsUnique(Predicate<? super A> predicate) { return set.existsUnique(predicate); }

    /**
     * @param predicate A test
     * @return whether every element passes {@code predicate}
     * @throws NullPointerException if {@code predicate} is null
     */
    public boolean forAll(Predicate<? super A> predicate) { return set.forAll(predicate); }

    /**
     * @param predicate A test
     * @return how many elements pass {@code predicate}
     * @throws NullPointerException if {@code predicate} is null
     */
    public int count(Predicate<? super A> predicate) { return set.count(predicate); }

    /**
     * @param zero The initial accumulator
     * @param f    Combines the accumulator and an element
     * @param <B>  Accumulator type
     * @return the elements folded from the left, in the comparator's order
     * @throws NullPointerException if {@code f} is null
     */
    public <B extends @Nullable Object> B foldLeft(B zero, BiFunction<? super B, ? super A, ? extends B> f) {
        return set.foldLeft(zero, f);
    }

    /**
     * @return the elements' {@code toString()}s, concatenated
     */
    public String mkString() { return set.mkString(); }

    /**
     * @param delimiter Put between elements
     * @return the elements' {@code toString()}s, joined by {@code delimiter}
     */
    public String mkString(CharSequence delimiter) { return set.mkString(delimiter); }

    /**
     * @param prefix    Put first
     * @param delimiter Put between elements
     * @param suffix    Put last
     * @return the elements' {@code toString()}s, joined by {@code delimiter}, between {@code prefix} and {@code suffix}
     */
    public String mkString(CharSequence prefix, CharSequence delimiter, CharSequence suffix) {
        return set.mkString(prefix, delimiter, suffix);
    }

    /**
     * @param collector A collector
     * @param <R>       Result type
     * @param <C>       The collector's accumulation type
     * @return the elements collected, as {@code stream().collect(collector)} does
     * @throws NullPointerException if {@code collector} is null
     */
    public <R extends @Nullable Object, C extends @Nullable Object> R collect(Collector<? super A, C, R> collector) {
        return set.collect(collector);
    }

    /**
     * @param supplier    Makes a new result container
     * @param accumulator Adds an element to a container
     * @param combiner    Merges two containers
     * @param <R>         Result type
     * @return the elements collected, as {@code stream().collect(supplier, accumulator, combiner)} does
     * @throws NullPointerException if an argument is null
     */
    public <R extends @Nullable Object> R collect(Supplier<R> supplier, BiConsumer<R, ? super A> accumulator, BiConsumer<R, R> combiner) {
        return set.collect(supplier, accumulator, combiner);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Complexity: O(1) to create; a whole walk is O(n), in the comparator's order.
     */
    @Override
    public java.util.Iterator<A> iterator() { return set.iterator(); }

    /**
     * @return the wrapped set's spliterator, which reports its size and that the elements are distinct and sorted
     */
    @Override
    public Spliterator<A> spliterator() { return set.spliterator(); }

    /**
     * @return a sequential {@link java.util.stream.Stream} over the elements, in the comparator's order
     */
    public java.util.stream.Stream<A> stream() { return set.stream(); }

    /**
     * An unmodifiable {@link java.util.NavigableSet} view of the elements, the one {@link TreeSet#asJava()} gives:
     * nothing is copied and every mutator of the view throws {@link UnsupportedOperationException}.
     * <p>
     * Complexity: O(1); {@code contains} on the view is O(log n).
     *
     * @return an unmodifiable {@code java.util.NavigableSet} view
     */
    public java.util.NavigableSet<A> asJava() { return set.asJava(); }

    /**
     * @return the elements in a new array, in order
     */
    public Object[] toArray() { return set.toArray(); }

    /**
     * @param arrayFactory Makes an array of the given length
     * @return the elements in a new array made by {@code arrayFactory}, in order
     * @throws NullPointerException if {@code arrayFactory} is null
     */
    public A[] toArray(IntFunction<A[]> arrayFactory) { return set.toArray(arrayFactory); }

    /**
     * @return the elements as a {@link Vector}, in order
     */
    public Vector<A> toVector() { return set.toVector(); }

    /**
     * @return the elements as a {@link List}, in order
     */
    public List<A> toList() { return set.toList(); }

    /**
     * @return the elements as a {@link Queue}, in order
     */
    public Queue<A> toQueue() { return set.toQueue(); }

    /**
     * @return the elements as a {@link Stream}, in order
     */
    public Stream<A> toStream() { return set.toStream(); }

    /**
     * @return the elements as a {@link HashSet}
     */
    public Set<A> toSet() { return set.toSet(); }

    /**
     * @return the elements as a {@link LinkedHashSet}, in order
     */
    public Set<A> toLinkedSet() { return set.toLinkedSet(); }

    /**
     * @param comparator The order
     * @return the elements as a {@link TreeSet} ordered by {@code comparator}
     * @throws NullPointerException if {@code comparator} is null
     */
    public SortedSet<A> toSortedSet(Comparator<? super A> comparator) { return set.toSortedSet(comparator); }

    /**
     * @param keyMapper   The key of an element
     * @param valueMapper The value of an element
     * @param <K>         Key type
     * @param <V>         Value type
     * @return the elements as the entries of a new {@link HashMap}; of two entries with the same key, the later wins
     * @throws NullPointerException if an argument is null or returns null
     */
    public <K extends @Nullable Object, V extends @Nullable Object> Map<K, V> toMap(Function<? super A, ? extends K> keyMapper, Function<? super A, ? extends V> valueMapper) {
        return set.toMap(keyMapper, valueMapper);
    }

    /**
     * @param f   The entry an element becomes
     * @param <K> Key type
     * @param <V> Value type
     * @return the elements as the entries of a new {@link HashMap}; of two entries with the same key, the later wins
     * @throws NullPointerException if {@code f} is null or returns null
     */
    public <K extends @Nullable Object, V extends @Nullable Object> Map<K, V> toMap(Function<? super A, ? extends Tuple2<? extends K, ? extends V>> f) {
        return set.toMap(f);
    }

    /**
     * @param keyMapper   The key of an element
     * @param valueMapper The value of an element
     * @param <K>         Key type
     * @param <V>         Value type
     * @return the elements as the entries of a new {@link LinkedHashMap}, in order
     * @throws NullPointerException if an argument is null or returns null
     */
    public <K extends @Nullable Object, V extends @Nullable Object> Map<K, V> toLinkedMap(Function<? super A, ? extends K> keyMapper, Function<? super A, ? extends V> valueMapper) {
        return set.toLinkedMap(keyMapper, valueMapper);
    }

    /**
     * @param f   The entry an element becomes
     * @param <K> Key type
     * @param <V> Value type
     * @return the elements as the entries of a new {@link LinkedHashMap}, in order
     * @throws NullPointerException if {@code f} is null or returns null
     */
    public <K extends @Nullable Object, V extends @Nullable Object> Map<K, V> toLinkedMap(Function<? super A, ? extends Tuple2<? extends K, ? extends V>> f) {
        return set.toLinkedMap(f);
    }

    /**
     * @param keyMapper   The key of an element
     * @param valueMapper The value of an element
     * @param <K>         Key type
     * @param <V>         Value type
     * @return the elements as the entries of a new {@link TreeMap} in the natural order of the keys
     * @throws NullPointerException if an argument is null or returns null
     */
    public <K extends Comparable<? super K>, V extends @Nullable Object> SortedMap<K, V> toSortedMap(Function<? super A, ? extends K> keyMapper, Function<? super A, ? extends V> valueMapper) {
        return set.toSortedMap(keyMapper, valueMapper);
    }

    /**
     * @param f   The entry an element becomes
     * @param <K> Key type
     * @param <V> Value type
     * @return the elements as the entries of a new {@link TreeMap} in the natural order of the keys
     * @throws NullPointerException if {@code f} is null or returns null
     */
    public <K extends Comparable<? super K>, V extends @Nullable Object> SortedMap<K, V> toSortedMap(Function<? super A, ? extends Tuple2<? extends K, ? extends V>> f) {
        return set.toSortedMap(f);
    }

    /**
     * @param comparator  The order of the keys
     * @param keyMapper   The key of an element
     * @param valueMapper The value of an element
     * @param <K>         Key type
     * @param <V>         Value type
     * @return the elements as the entries of a new {@link TreeMap} ordered by {@code comparator}
     * @throws NullPointerException if an argument is null or a mapper returns null
     */
    public <K extends @Nullable Object, V extends @Nullable Object> SortedMap<K, V> toSortedMap(Comparator<? super K> comparator, Function<? super A, ? extends K> keyMapper, Function<? super A, ? extends V> valueMapper) {
        return set.toSortedMap(comparator, keyMapper, valueMapper);
    }

    /**
     * @param comparator The order of the keys
     * @param f          The entry an element becomes
     * @param <K>        Key type
     * @param <V>        Value type
     * @return the elements as the entries of a new {@link TreeMap} ordered by {@code comparator}
     * @throws NullPointerException if an argument is null or {@code f} returns null
     */
    public <K extends @Nullable Object, V extends @Nullable Object> SortedMap<K, V> toSortedMap(Comparator<? super K> comparator, Function<? super A, ? extends Tuple2<? extends K, ? extends V>> f) {
        return set.toSortedMap(comparator, f);
    }

    // -- returns Option

    /**
     * @param predicate A test
     * @return the first element, in the comparator's order, that passes {@code predicate}
     * @throws NullPointerException if {@code predicate} is null
     */
    public Option<A> find(Predicate<? super A> predicate) { return set.find(predicate); }

    /**
     * Arranges the elements by a key that must be unique.
     *
     * @param getKey The key of an element
     * @param <K>    Key type
     * @return {@code Some} of the map from each key to its element, or {@code None} if two elements share a key
     * @throws NullPointerException if {@code getKey} is null
     */
    public <K extends @Nullable Object> Option<Map<K, A>> arrangeBy(Function<? super A, ? extends K> getKey) {
        return set.arrangeBy(getKey);
    }

    /**
     * Complexity: O(log n), that of {@link TreeSet#tail()}.
     *
     * @return all elements but the first, if there are any
     */
    public Option<NonEmptySortedSet<A>> tailNonEmpty() {
        return set.size() == 1 ? Option.none() : Option.some(new NonEmptySortedSet<>(set.tail()));
    }

    /**
     * Complexity: O(log n), that of {@link TreeSet#init()}.
     *
     * @return all elements but the last, if there are any
     */
    public Option<NonEmptySortedSet<A>> initNonEmpty() {
        return set.size() == 1 ? Option.none() : Option.some(new NonEmptySortedSet<>(set.init()));
    }

    // -- Object

    /**
     * Sets are equal to sets: a {@code NonEmptySortedSet} is equal to a {@code NonEmptySortedSet} or a
     * {@code NonEmptySet} with the same elements, and never to a plain {@link Set}.
     */
    @Override
    public boolean equals(@Nullable Object o) {
        return o == this
                || (o instanceof NonEmptySortedSet<?> that && set.equals(that.set))
                || (o instanceof NonEmptySet<?> hashed && set.equals(hashed.toSet()));
    }

    @Override
    public int hashCode() { return set.hashCode(); }

    @Override
    public String toString() { return set.mkString("NonEmptySortedSet(", ", ", ")"); }
}
