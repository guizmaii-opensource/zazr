package com.guizmaii.zazr.collection;

import com.guizmaii.zazr.Tuple2;
import com.guizmaii.zazr.collection.internal.NonEmptyModule;
import com.guizmaii.zazr.control.Either;
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
 * A {@link HashSet} with at least one element (zio-prelude's {@code NonEmptySet}): a wrapper, not a subtype, so that
 * every method can state in its return type whether the result is still non-empty.
 * <p>
 * The contract, per method family:
 * <ul>
 * <li>operations that cannot remove every element return a {@code NonEmptySet}: {@code add}, {@code addAll},
 * {@code union}, {@code map}, {@code flatMap}, {@code as}, {@code replace}, {@code replaceAll}, {@code tap};
 * {@code groupBy} returns non-empty groups;</li>
 * <li>operations that can shrink return a {@link HashSet}: {@code filter}, {@code reject}, {@code collect},
 * {@code flatMapAll}, {@code remove}, {@code removeAll}, {@code retainAll}, {@code intersect}, {@code diff},
 * {@code partition}, {@code partitionMap};</li>
 * <li>operations that are partial on a {@code HashSet} are total here: {@code max}, {@code min}, {@code maxBy},
 * {@code minBy}, {@code reduce}, {@code reduceMap}, {@code average};</li>
 * <li>narrowing back to a {@code NonEmptySet} returns an {@link Option}: {@link #fromSet(HashSet)},
 * {@link #fromIterable(Iterable)}, {@link HashSet#toNonEmptySet()}.</li>
 * </ul>
 * A hash set has no order, so there is no {@code head}: the first element of an iteration is not a meaningful value.
 * {@link NonEmptySortedSet} has a total {@code head} and {@code last}.
 * <p>
 * Two {@code flatMap}s cannot share a name (a lambda argument would be ambiguous), so {@code flatMap} is the one whose
 * function returns a {@code NonEmptySet} and {@code flatMapAll} the one whose function returns any {@code Iterable}.
 * The set operations take a {@link Set}; a {@code NonEmptySet} argument goes through {@link #addAll(Iterable)},
 * {@link #removeAll(Iterable)} and {@link #retainAll(Iterable)}, which take any {@code Iterable}.
 * <p>
 * Like every collection, it rejects null elements. Equal to another {@code NonEmptySet} or {@code NonEmptySortedSet}
 * with the same elements, as sets are equal to sets, never to a plain {@code Set}; use {@link #toSet()} to compare
 * across the two.
 *
 * @param <A> Component type.
 */
public final class NonEmptySet<A extends @Nullable Object> implements Iterable<A> {

    private final HashSet<A> set;

    private NonEmptySet(HashSet<A> set) { this.set = set; }

    // -- constructors

    /**
     * A {@code NonEmptySet} of {@code head} and the elements of {@code tail}; duplicates are kept once.
     *
     * @param head An element
     * @param tail The other elements
     * @param <A>  Component type
     * @return a non-empty set
     * @throws NullPointerException if {@code head}, {@code tail} or an element of {@code tail} is null
     */
    @SafeVarargs
    @SuppressWarnings("varargs")
    public static <A extends @Nullable Object> NonEmptySet<A> of(A head, A... tail) {
        Objects.requireNonNull(head, "NonEmptySet: head is null");
        Objects.requireNonNull(tail, "NonEmptySet: tail is null");
        final HashSet.Builder<A> builder = HashSet.newBuilder();
        builder.add(head);
        for (A element : tail) {
            builder.add(Objects.requireNonNull(element, "NonEmptySet: element is null"));
        }
        return new NonEmptySet<>(builder.result());
    }

    /**
     * A {@code NonEmptySet} of {@code head} and the elements of {@code tail}. Not an overload of
     * {@link #of(Object, Object[])}: when the elements are themselves iterables, {@code of(x, y)} would silently pick
     * the {@code Iterable} overload and splice {@code y}.
     *
     * @param head An element
     * @param tail The other elements, read once
     * @param <A>  Component type
     * @return a non-empty set
     * @throws NullPointerException if {@code head}, {@code tail} or an element of {@code tail} is null
     */
    public static <A extends @Nullable Object> NonEmptySet<A> fromIterable(A head, Iterable<? extends A> tail) {
        Objects.requireNonNull(head, "NonEmptySet: head is null");
        Objects.requireNonNull(tail, "NonEmptySet: tail is null");
        final HashSet.Builder<A> builder = HashSet.newBuilder();
        builder.add(head);
        return new NonEmptySet<>(addAll(builder, tail).result());
    }

    /**
     * A {@code NonEmptySet} of one element.
     *
     * @param element The element
     * @param <A>     Component type
     * @return a non-empty set of size 1
     * @throws NullPointerException if {@code element} is null
     */
    public static <A extends @Nullable Object> NonEmptySet<A> single(A element) {
        Objects.requireNonNull(element, "NonEmptySet: element is null");
        return new NonEmptySet<>(HashSet.of(element));
    }

    /**
     * Wraps a {@link HashSet} if it is not empty.
     * <p>
     * Complexity: O(1).
     *
     * @param set A set
     * @param <A> Component type
     * @return {@code Some(nonEmptySet)} sharing {@code set}'s elements, or {@code None} if {@code set} is empty
     * @throws NullPointerException if {@code set} is null
     */
    public static <A extends @Nullable Object> Option<NonEmptySet<A>> fromSet(HashSet<A> set) {
        Objects.requireNonNull(set, "NonEmptySet.fromSet: set is null");
        return set.isEmpty() ? Option.none() : Option.some(new NonEmptySet<>(set));
    }

    /**
     * Copies an {@link Iterable} if it yields at least one element; a {@code HashSet} is wrapped without copying.
     *
     * @param iterable The elements, read once
     * @param <A>      Component type
     * @return {@code Some(nonEmptySet)} of the distinct elements, or {@code None} if {@code iterable} is empty
     * @throws NullPointerException if {@code iterable} or an element is null
     */
    @SuppressWarnings("unchecked")
    public static <A extends @Nullable Object> Option<NonEmptySet<A>> fromIterable(Iterable<? extends A> iterable) {
        Objects.requireNonNull(iterable, "NonEmptySet.fromIterable: iterable is null");
        if (iterable instanceof HashSet) {
            return fromSet((HashSet<A>) iterable);
        }
        return fromSet(addAll(HashSet.<A> newBuilder(), iterable).result());
    }

    /**
     * Wraps a {@link HashSet} known to be non-empty.
     * <p>
     * Complexity: O(1).
     *
     * @param set A non-empty set
     * @param <A> Component type
     * @return a non-empty set sharing {@code set}'s elements
     * @throws IllegalArgumentException if {@code set} is empty
     * @throws NullPointerException     if {@code set} is null
     */
    public static <A extends @Nullable Object> NonEmptySet<A> unsafeFromSet(HashSet<A> set) {
        Objects.requireNonNull(set, "NonEmptySet.unsafeFromSet: set is null");
        if (set.isEmpty()) {
            throw new IllegalArgumentException("NonEmptySet.unsafeFromSet: set is empty");
        }
        return new NonEmptySet<>(set);
    }

    /**
     * The union of a non-empty set of non-empty sets. Static, like every {@code flatten} in Zazr, because Java cannot
     * demand of an instance method that the receiver's element type be a collection.
     *
     * @param nested Non-empty sets
     * @param <A>    Component type of the inner sets
     * @return the elements of every inner set
     * @throws NullPointerException if {@code nested} is null
     */
    public static <A extends @Nullable Object> NonEmptySet<A> flatten(NonEmptySet<? extends NonEmptySet<? extends A>> nested) {
        Objects.requireNonNull(nested, "nested is null");
        final HashSet.Builder<A> builder = HashSet.newBuilder();
        for (NonEmptySet<? extends A> inner : nested) {
            builder.addAll(inner.set);
        }
        return new NonEmptySet<>(builder.result());
    }

    /* a HashSet cannot hold a null, so it is added as is; anything else is checked element by element, naming this type */
    private static <A extends @Nullable Object> HashSet.Builder<A> addAll(HashSet.Builder<A> builder, Iterable<? extends A> elements) {
        if (elements instanceof HashSet) {
            return builder.addAll(elements);
        }
        for (A element : elements) {
            builder.add(Objects.requireNonNull(element, "NonEmptySet: element is null"));
        }
        return builder;
    }

    // -- returns NonEmptySet: at least one element is left

    /**
     * Complexity: effectively O(1), that of {@link HashSet#add(Object)}.
     *
     * @param element An element
     * @return this set with {@code element}
     * @throws NullPointerException if {@code element} is null
     */
    public NonEmptySet<A> add(A element) {
        Objects.requireNonNull(element, "NonEmptySet.add: element is null");
        return wrap(set.add(element));
    }

    /**
     * Accepts the possibly empty type and returns the non-empty one; a {@code NonEmptySet} is an {@code Iterable} too.
     * <p>
     * Complexity: O(m) for m elements, each an effectively O(1) insertion, that of {@link HashSet#addAll(Iterable)}.
     *
     * @param elements Elements to add, possibly none, read once
     * @return this set with {@code elements}
     * @throws NullPointerException if {@code elements} or one of them is null
     */
    public NonEmptySet<A> addAll(Iterable<? extends A> elements) { return wrap(set.addAll(elements)); }

    /**
     * Complexity: O(m) for a set of m elements, each an effectively O(1) insertion, that of {@link HashSet#union(Set)}.
     *
     * @param elements A set, possibly empty
     * @return the elements of both sets
     * @throws NullPointerException if {@code elements} is null
     */
    public NonEmptySet<A> union(Set<? extends A> elements) { return wrap(set.union(elements)); }

    /**
     * @param mapper A function
     * @param <B>    Component type of the result
     * @return the distinct results of {@code mapper}; fewer elements than this set if two results are equal, never none
     * @throws NullPointerException if {@code mapper} is null or returns null
     */
    public <B extends @Nullable Object> NonEmptySet<B> map(Function<? super A, ? extends B> mapper) {
        return new NonEmptySet<>(set.map(mapper));
    }

    /**
     * The union of the non-empty sets {@code mapper} returns for each element. For a function that returns any
     * {@code Iterable}, possibly empty, see {@link #flatMapAll(Function)}.
     *
     * @param mapper A function returning a non-empty set
     * @param <B>    Component type of the result
     * @return the union of the results
     * @throws NullPointerException if {@code mapper} is null or returns null
     */
    public <B extends @Nullable Object> NonEmptySet<B> flatMap(Function<? super A, ? extends NonEmptySet<? extends B>> mapper) {
        Objects.requireNonNull(mapper, "mapper is null");
        final HashSet.Builder<B> builder = HashSet.newBuilder();
        for (A element : set) {
            builder.addAll(Objects.requireNonNull(mapper.apply(element), "NonEmptySet.flatMap: mapper returned null").set);
        }
        return new NonEmptySet<>(builder.result());
    }

    /**
     * @param value The element of the result
     * @param <B>   Component type of the result
     * @return the set of {@code value} alone
     * @throws NullPointerException if {@code value} is null
     */
    public <B extends @Nullable Object> NonEmptySet<B> as(B value) {
        Objects.requireNonNull(value, "NonEmptySet.as: value is null");
        return new NonEmptySet<>(HashSet.of(value));
    }

    /**
     * Complexity: effectively O(1), that of {@link HashSet#replace(Object, Object)}.
     *
     * @param currentElement An element
     * @param newElement     Its replacement
     * @return this set with {@code newElement} instead of {@code currentElement}, or this set if {@code currentElement}
     *         is absent
     * @throws NullPointerException if {@code currentElement} or {@code newElement} is null
     */
    public NonEmptySet<A> replace(A currentElement, A newElement) {
        Objects.requireNonNull(currentElement, "NonEmptySet.replace: currentElement is null");
        Objects.requireNonNull(newElement, "NonEmptySet.replace: newElement is null");
        return wrap(set.replace(currentElement, newElement));
    }

    /**
     * Complexity: effectively O(1), that of {@link #replace(Object, Object)}: a set holds an element once.
     *
     * @param currentElement An element
     * @param newElement     Its replacement
     * @return the same as {@link #replace(Object, Object)}
     * @throws NullPointerException if {@code currentElement} or {@code newElement} is null
     */
    public NonEmptySet<A> replaceAll(A currentElement, A newElement) { return replace(currentElement, newElement); }

    /**
     * Runs {@code action} on every element.
     *
     * @param action A side effect
     * @return this set
     * @throws NullPointerException if {@code action} is null
     */
    public NonEmptySet<A> tap(Consumer<? super A> action) {
        set.tap(action);
        return this;
    }

    /**
     * Groups the elements by the key {@code classifier} computes.
     *
     * @param classifier Computes the key of an element
     * @param <K>        Key type
     * @return the groups, each non-empty
     * @throws NullPointerException if {@code classifier} is null or returns null
     */
    public <K extends @Nullable Object> HashMap<K, NonEmptySet<A>> groupBy(Function<? super A, ? extends K> classifier) {
        final HashMap.Builder<K, NonEmptySet<A>> groups = HashMap.newBuilder();
        for (Tuple2<K, HashSet<A>> group : set.<K> groupBy(classifier)) {
            groups.put(group._1(), new NonEmptySet<>(group._2()));
        }
        return groups.result();
    }

    /* the plain operations that cannot empty a non-empty set return the same instance when nothing changes */
    private NonEmptySet<A> wrap(HashSet<A> result) { return result == set ? this : new NonEmptySet<>(result); }

    // -- returns HashSet: the result may be empty

    /**
     * Complexity: O(1).
     *
     * @return the wrapped set
     */
    public HashSet<A> toSet() { return set; }

    /**
     * @param predicate A test
     * @return the elements that pass {@code predicate}
     * @throws NullPointerException if {@code predicate} is null
     */
    public HashSet<A> filter(Predicate<? super A> predicate) { return set.filter(predicate); }

    /**
     * @param predicate A test
     * @return the elements that fail {@code predicate}
     * @throws NullPointerException if {@code predicate} is null
     */
    public HashSet<A> reject(Predicate<? super A> predicate) { return set.reject(predicate); }

    /**
     * Maps and filters in one pass: keeps the {@code Some} results.
     *
     * @param mapper A function returning an {@link Option}
     * @param <B>    Component type of the result
     * @return the defined results
     * @throws NullPointerException if {@code mapper} is null or returns null
     */
    public <B extends @Nullable Object> HashSet<B> collect(Function<? super A, ? extends Option<? extends B>> mapper) {
        return set.collect(mapper);
    }

    /**
     * The union of the iterables {@code mapper} returns for each element; they may be empty, so the result may be too.
     * For a function that returns a {@code NonEmptySet}, see {@link #flatMap(Function)}.
     *
     * @param mapper A function returning an {@link Iterable}
     * @param <B>    Component type of the result
     * @return the union of the results
     * @throws NullPointerException if {@code mapper} is null or returns null
     */
    public <B extends @Nullable Object> HashSet<B> flatMapAll(Function<? super A, ? extends Iterable<? extends B>> mapper) {
        return set.flatMap(mapper);
    }

    /**
     * Complexity: effectively O(1), that of {@link HashSet#remove(Object)}.
     *
     * @param element An element
     * @return this set without {@code element}
     */
    public HashSet<A> remove(A element) { return set.remove(element); }

    /**
     * Complexity: O(n + m) for m given elements, that of {@link HashSet#removeAll(Iterable)}.
     *
     * @param elements Elements
     * @return this set without {@code elements}
     * @throws NullPointerException if {@code elements} is null
     */
    public HashSet<A> removeAll(Iterable<? extends A> elements) { return set.removeAll(elements); }

    /**
     * Complexity: O(n + m) for m given elements, that of {@link HashSet#retainAll(Iterable)}.
     *
     * @param elements Elements
     * @return the elements of this set that are among {@code elements}
     * @throws NullPointerException if {@code elements} is null
     */
    public HashSet<A> retainAll(Iterable<? extends A> elements) { return set.retainAll(elements); }

    /**
     * Complexity: O(n + m) for a set of m elements, that of {@link HashSet#intersect(Set)}.
     *
     * @param elements A set
     * @return the elements in both sets
     * @throws NullPointerException if {@code elements} is null
     */
    public HashSet<A> intersect(Set<? extends A> elements) { return set.intersect(elements); }

    /**
     * Complexity: O(n + m) for a set of m elements, that of {@link HashSet#diff(Set)}.
     *
     * @param elements A set
     * @return the elements of this set that are not in {@code elements}
     * @throws NullPointerException if {@code elements} is null
     */
    public HashSet<A> diff(Set<? extends A> elements) { return set.diff(elements); }

    /**
     * @param predicate A test
     * @return the elements that pass {@code predicate} and those that fail it; either may be empty
     * @throws NullPointerException if {@code predicate} is null
     */
    public Tuple2<HashSet<A>, HashSet<A>> partition(Predicate<? super A> predicate) { return set.partition(predicate); }

    /**
     * Splits the elements into a left and a right side according to the {@link Either} {@code f} returns for each.
     * Either side may be empty.
     *
     * @param f   Classifies an element
     * @param <L> Component type of the left side
     * @param <R> Component type of the right side
     * @return the left values and the right values
     * @throws NullPointerException if {@code f} is null or returns null
     */
    public <L extends @Nullable Object, R extends @Nullable Object> Tuple2<HashSet<L>, HashSet<R>> partitionMap(Function<? super A, ? extends Either<? extends L, ? extends R>> f) {
        return set.partitionMap(f);
    }

    // -- total: what is partial on a HashSet

    /**
     * The greatest element in the natural order of the elements, as {@link HashSet#max()}.
     * <p>
     * Complexity: O(n), every element compared once.
     *
     * @return the greatest element; {@code NaN} compares as the greatest {@code Double} or {@code Float}
     * @throws ClassCastException if the elements are not {@link Comparable}
     */
    public A max() { return NonEmptyModule.max(set); }

    /**
     * The least element in the natural order of the elements, as {@link HashSet#min()}.
     * <p>
     * Complexity: O(n), every element compared once.
     *
     * @return the least element; among {@code Double}s or {@code Float}s, a {@code NaN} whenever one is present
     * @throws ClassCastException if the elements are not {@link Comparable}
     */
    public A min() { return NonEmptyModule.min(set); }

    /**
     * @param comparator The order
     * @return the greatest element under {@code comparator}; of several, the first in iteration order
     * @throws NullPointerException if {@code comparator} is null
     */
    public A maxBy(Comparator<? super A> comparator) { return NonEmptyModule.max(set, comparator); }

    /**
     * @param comparator The order
     * @return the least element under {@code comparator}; of several, the first in iteration order
     * @throws NullPointerException if {@code comparator} is null
     */
    public A minBy(Comparator<? super A> comparator) { return NonEmptyModule.min(set, comparator); }

    /**
     * @param f   Computes the key, once per element
     * @param <U> Key type
     * @return the element with the greatest key; of several, the first in iteration order
     * @throws NullPointerException if {@code f} is null
     */
    public <U extends Comparable<? super U>> A maxBy(Function<? super A, ? extends U> f) { return NonEmptyModule.maxBy(set, f); }

    /**
     * @param f   Computes the key, once per element
     * @param <U> Key type
     * @return the element with the least key; of several, the first in iteration order
     * @throws NullPointerException if {@code f} is null
     */
    public <U extends Comparable<? super U>> A minBy(Function<? super A, ? extends U> f) { return NonEmptyModule.minBy(set, f); }

    /**
     * Combines the elements with {@code op} in iteration order, which a hash set does not define: {@code op} should be
     * associative and commutative for the result to be independent of it.
     *
     * @param op Combines two elements
     * @return the combined elements
     * @throws NullPointerException if {@code op} is null
     */
    public A reduce(BiFunction<? super A, ? super A, ? extends A> op) { return NonEmptyModule.reduce(set, op); }

    /**
     * Maps every element and combines the results, in one pass and in iteration order: {@code op} should be
     * associative and commutative.
     *
     * @param mapper Maps an element
     * @param op     Combines two mapped values
     * @param <B>    Result type
     * @return the combined mapped values
     * @throws NullPointerException if {@code mapper} or {@code op} is null
     */
    public <B extends @Nullable Object> B reduceMap(Function<? super A, ? extends B> mapper, BiFunction<? super B, ? super B, ? extends B> op) {
        return NonEmptyModule.reduceMap(set, mapper, op);
    }

    /**
     * Folds the elements with {@code combine}, starting from {@code zero}, which must be its neutral element; as for
     * {@link #reduce(BiFunction)}, {@code combine} should be associative and commutative.
     *
     * @param zero    The neutral element of {@code combine}
     * @param combine Combines two elements
     * @return the folded result
     * @throws NullPointerException if {@code combine} is null
     */
    public A fold(A zero, BiFunction<? super A, ? super A, ? extends A> combine) { return set.fold(zero, combine); }

    /**
     * @return the sum of the elements, which must be {@link Number}s, with the arithmetic of {@link HashSet#sum()}
     * @throws UnsupportedOperationException if an element is not a {@code Number}
     */
    public Number sum() { return set.sum(); }

    /**
     * @return the product of the elements, which must be {@link Number}s, with the arithmetic of
     *         {@link HashSet#product()}
     * @throws UnsupportedOperationException if an element is not a {@code Number}
     */
    public Number product() { return set.product(); }

    /**
     * The average of the elements, which must be {@link Number}s: the value {@link HashSet#average()} holds.
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
     * Complexity: effectively O(1), that of {@link HashSet#contains(Object)}.
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
     * @return the elements folded in iteration order
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
     * Complexity: O(1) to create; a whole walk is O(n), that of {@link HashSet#iterator()}.
     */
    @Override
    public java.util.Iterator<A> iterator() { return set.iterator(); }

    /**
     * @return the wrapped set's spliterator, which reports its size and that the elements are distinct
     */
    @Override
    public Spliterator<A> spliterator() { return set.spliterator(); }

    /**
     * @return a sequential {@link java.util.stream.Stream} over the elements
     */
    public java.util.stream.Stream<A> stream() { return set.stream(); }

    /**
     * An unmodifiable {@link java.util.Set} view of the elements, the one {@link HashSet#asJava()} gives: nothing is
     * copied and every mutator of the view throws {@link UnsupportedOperationException}.
     * <p>
     * Complexity: O(1); {@code contains} on the view is effectively O(1).
     *
     * @return an unmodifiable {@code java.util.Set} view
     */
    public java.util.Set<A> asJava() { return set.asJava(); }

    /**
     * @return the elements in a new array
     */
    public Object[] toArray() { return set.toArray(); }

    /**
     * @param arrayFactory Makes an array of the given length
     * @return the elements in a new array made by {@code arrayFactory}
     * @throws NullPointerException if {@code arrayFactory} is null
     */
    public A[] toArray(IntFunction<A[]> arrayFactory) { return set.toArray(arrayFactory); }

    /**
     * @return the elements as a {@link Vector}, in iteration order
     */
    public Vector<A> toVector() { return set.toVector(); }

    /**
     * @return the elements as a {@link List}, in iteration order
     */
    public List<A> toList() { return set.toList(); }

    /**
     * @return the elements as a {@link Queue}, in iteration order
     */
    public Queue<A> toQueue() { return set.toQueue(); }

    /**
     * @return the elements as a {@link Stream}, in iteration order
     */
    public Stream<A> toStream() { return set.toStream(); }

    /**
     * @return the elements as a {@link LinkedHashSet}, in iteration order
     */
    public Set<A> toLinkedSet() { return set.toLinkedSet(); }

    /**
     * @return the elements as a {@link TreeSet} in their natural order
     * @throws ClassCastException if the elements are not {@link Comparable}
     */
    public SortedSet<A> toSortedSet() { return set.toSortedSet(); }

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
     * @return the elements as the entries of a new {@link LinkedHashMap}, in iteration order
     * @throws NullPointerException if an argument is null or returns null
     */
    public <K extends @Nullable Object, V extends @Nullable Object> Map<K, V> toLinkedMap(Function<? super A, ? extends K> keyMapper, Function<? super A, ? extends V> valueMapper) {
        return set.toLinkedMap(keyMapper, valueMapper);
    }

    /**
     * @param f   The entry an element becomes
     * @param <K> Key type
     * @param <V> Value type
     * @return the elements as the entries of a new {@link LinkedHashMap}, in iteration order
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
     * @return an element that passes {@code predicate}, the first in iteration order
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

    // -- Object

    /**
     * Sets are equal to sets: a {@code NonEmptySet} is equal to a {@code NonEmptySet} or a {@code NonEmptySortedSet}
     * with the same elements, and never to a plain {@link Set}.
     */
    @Override
    public boolean equals(@Nullable Object o) {
        return o == this
                || (o instanceof NonEmptySet<?> that && set.equals(that.set))
                || (o instanceof NonEmptySortedSet<?> sorted && set.equals(sorted.toSortedSet()));
    }

    @Override
    public int hashCode() { return set.hashCode(); }

    @Override
    public String toString() { return set.mkString("NonEmptySet(", ", ", ")"); }
}
