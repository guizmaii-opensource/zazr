package com.guizmaii.zazr.collection;

import com.guizmaii.zazr.Tuple;
import com.guizmaii.zazr.Tuple2;
import com.guizmaii.zazr.control.Either;
import com.guizmaii.zazr.control.Option;
import java.util.Comparator;
import java.util.Objects;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import org.jspecify.annotations.Nullable;

/**
 * A {@link Vector} with at least one element (ZIO's {@code NonEmptyChunk}): a wrapper, not a subtype, so that every
 * method can state in its return type whether the result is still non-empty.
 * <p>
 * The contract, per method family:
 * <ul>
 * <li>operations that preserve or grow the size return a {@code NonEmptyVector}: {@code map}, {@code flatMap},
 * {@code append*}, {@code prepend*}, {@code concat}, {@code reverse}, {@code distinct*}, {@code sorted}, {@code sortBy},
 * {@code zip*}, {@code scanLeft}, {@code update}, {@code tap}; {@code grouped} and {@code groupBy} return non-empty
 * groups;</li>
 * <li>operations that can shrink return a {@link Vector}: {@code filter}, {@code reject}, {@code collect},
 * {@code flatMapAll}, {@code tail}, {@code init}, {@code drop*}, {@code take*}, {@code slice}, {@code remove*},
 * {@code duplicates*}, {@code partitionMap};</li>
 * <li>operations that are partial on a {@code Vector} are total here: {@code head}, {@code last}, {@code max}, {@code min},
 * {@code maxBy}, {@code minBy}, {@code reduce*}, {@code reduceMap};</li>
 * <li>narrowing back to a {@code NonEmptyVector} returns an {@link Option}: {@code tailNonEmpty}, {@code initNonEmpty},
 * as do the searches {@code find}, {@code findLast}, {@code indexOfOption}.</li>
 * </ul>
 * Two {@code flatMap}s cannot share a name (a lambda argument would be ambiguous), so {@code flatMap} is the one whose
 * function returns a {@code NonEmptyVector} and {@code flatMapAll} the one whose function returns any {@code Iterable}.
 * <p>
 * Like every collection, it rejects null elements. Equal only to another {@code NonEmptyVector} with the same elements
 * in the same order, never to a {@code Vector}; use {@link #toVector()} to compare across the two.
 *
 * @param <A> Component type.
 */
public final class NonEmptyVector<A extends @Nullable Object> implements Iterable<A> {

    private final Vector<A> vector;

    private NonEmptyVector(Vector<A> vector) { this.vector = vector; }

    // -- constructors

    /**
     * A {@code NonEmptyVector} of {@code head} followed by {@code tail}.
     *
     * @param head The first element
     * @param tail The remaining elements
     * @param <A>  Component type
     * @return a non-empty vector
     * @throws NullPointerException if {@code head}, {@code tail} or an element of {@code tail} is null
     */
    @SafeVarargs
    @SuppressWarnings("varargs")
    public static <A extends @Nullable Object> NonEmptyVector<A> of(A head, A... tail) {
        Objects.requireNonNull(head, "NonEmptyVector: head is null");
        Objects.requireNonNull(tail, "NonEmptyVector: tail is null");
        final Vector.Builder<A> builder = Vector.newBuilder(tail.length + 1);
        builder.add(head);
        for (A element : tail) {
            builder.add(Objects.requireNonNull(element, "NonEmptyVector: element is null"));
        }
        return new NonEmptyVector<>(builder.result());
    }

    /**
     * A {@code NonEmptyVector} of {@code head} followed by the elements of {@code tail}, in iteration order (ZIO's
     * {@code NonEmptyChunk.fromIterable(a, as)}). Not an overload of {@link #of(Object, Object...)}: when the elements are
     * themselves iterables, {@code of(x, y)} would silently pick the {@code Iterable} overload and splice {@code y}.
     *
     * @param head The first element
     * @param tail The remaining elements
     * @param <A>  Component type
     * @return a non-empty vector
     * @throws NullPointerException if {@code head}, {@code tail} or an element of {@code tail} is null
     */
    public static <A extends @Nullable Object> NonEmptyVector<A> fromIterable(A head, Iterable<? extends A> tail) {
        Objects.requireNonNull(head, "NonEmptyVector: head is null");
        Objects.requireNonNull(tail, "NonEmptyVector: tail is null");
        final Vector.Builder<A> builder = Vector.newBuilder();
        builder.add(head);
        return new NonEmptyVector<>(addAll(builder, tail).result());
    }

    /**
     * A {@code NonEmptyVector} of one element.
     *
     * @param element The element
     * @param <A>     Component type
     * @return a non-empty vector of size 1
     * @throws NullPointerException if {@code element} is null
     */
    public static <A extends @Nullable Object> NonEmptyVector<A> single(A element) {
        Objects.requireNonNull(element, "NonEmptyVector: element is null");
        return new NonEmptyVector<>(Vector.of(element));
    }

    /**
     * Wraps a {@link Vector} if it is not empty.
     *
     * @param vector A vector
     * @param <A>    Component type
     * @return {@code Some(nonEmptyVector)} sharing {@code vector}'s elements, or {@code None} if {@code vector} is empty
     * @throws NullPointerException if {@code vector} is null
     */
    public static <A extends @Nullable Object> Option<NonEmptyVector<A>> fromVector(Vector<A> vector) {
        Objects.requireNonNull(vector, "NonEmptyVector.fromVector: vector is null");
        return vector.isEmpty() ? Option.none() : Option.some(new NonEmptyVector<>(vector));
    }

    /**
     * Copies an {@link Iterable} if it yields at least one element.
     *
     * @param iterable The elements
     * @param <A>      Component type
     * @return {@code Some(nonEmptyVector)} of the elements in iteration order, or {@code None} if {@code iterable} is empty
     * @throws NullPointerException if {@code iterable} or an element is null
     */
    @SuppressWarnings("unchecked")
    public static <A extends @Nullable Object> Option<NonEmptyVector<A>> fromIterable(Iterable<? extends A> iterable) {
        Objects.requireNonNull(iterable, "NonEmptyVector.fromIterable: iterable is null");
        if (iterable instanceof Vector) {
            return fromVector((Vector<A>) iterable);
        }
        return fromVector(addAll(Vector.<A> newBuilder(), iterable).result());
    }

    /**
     * Wraps a {@link Vector} known to be non-empty.
     *
     * @param vector A non-empty vector
     * @param <A>    Component type
     * @return a non-empty vector sharing {@code vector}'s elements
     * @throws IllegalArgumentException if {@code vector} is empty
     * @throws NullPointerException     if {@code vector} is null
     */
    public static <A extends @Nullable Object> NonEmptyVector<A> unsafeFromVector(Vector<A> vector) {
        Objects.requireNonNull(vector, "NonEmptyVector.unsafeFromVector: vector is null");
        if (vector.isEmpty()) {
            throw new IllegalArgumentException("NonEmptyVector.unsafeFromVector: vector is empty");
        }
        return new NonEmptyVector<>(vector);
    }

    /**
     * Concatenates a non-empty vector of non-empty vectors. Static, like every {@code flatten} in zazr, because Java
     * cannot demand of an instance method that the receiver's element type be a collection.
     *
     * @param nested Non-empty vectors
     * @param <A>    Component type of the inner vectors
     * @return the inner elements, in order
     * @throws NullPointerException if {@code nested} is null
     */
    public static <A extends @Nullable Object> NonEmptyVector<A> flatten(NonEmptyVector<? extends NonEmptyVector<? extends A>> nested) {
        Objects.requireNonNull(nested, "nested is null");
        final Vector.Builder<A> builder = Vector.newBuilder();
        for (NonEmptyVector<? extends A> inner : nested) {
            builder.addAll(inner.vector);
        }
        return new NonEmptyVector<>(builder.result());
    }

    /* a Vector cannot hold a null, so it is appended by leaf copies; anything else is checked element by element, naming this type */
    private static <A extends @Nullable Object> Vector.Builder<A> addAll(Vector.Builder<A> builder, Iterable<? extends A> elements) {
        if (elements instanceof Vector) {
            return builder.addAll(elements);
        }
        for (A element : elements) {
            builder.add(Objects.requireNonNull(element, "NonEmptyVector: element is null"));
        }
        return builder;
    }

    // -- returns NonEmptyVector: the size is preserved or grows

    /**
     * Applies {@code mapper} to every element.
     *
     * @param mapper A function
     * @param <B>    Component type of the result
     * @return a non-empty vector of the same size
     * @throws NullPointerException if {@code mapper} is null or returns null
     */
    public <B extends @Nullable Object> NonEmptyVector<B> map(Function<? super A, ? extends B> mapper) {
        return new NonEmptyVector<>(vector.map(mapper));
    }

    /**
     * Concatenates the non-empty vectors {@code mapper} returns for each element. For a function that returns any
     * {@code Iterable}, possibly empty, see {@link #flatMapAll(Function)}.
     *
     * @param mapper A function returning a non-empty vector
     * @param <B>    Component type of the result
     * @return a non-empty vector of at least this size
     * @throws NullPointerException if {@code mapper} is null or returns null
     */
    public <B extends @Nullable Object> NonEmptyVector<B> flatMap(Function<? super A, ? extends NonEmptyVector<? extends B>> mapper) {
        Objects.requireNonNull(mapper, "mapper is null");
        final Vector.Builder<B> builder = Vector.newBuilder();
        for (A element : vector) {
            builder.addAll(Objects.requireNonNull(mapper.apply(element), "NonEmptyVector.flatMap: mapper returned null").vector);
        }
        return new NonEmptyVector<>(builder.result());
    }

    /**
     * @param element An element
     * @return this vector followed by {@code element}
     * @throws NullPointerException if {@code element} is null
     */
    public NonEmptyVector<A> append(A element) {
        Objects.requireNonNull(element, "NonEmptyVector.append: element is null");
        return new NonEmptyVector<>(vector.append(element));
    }

    /**
     * Accepts the possibly empty type and returns the non-empty one.
     *
     * @param elements Elements to append, possibly none
     * @return this vector followed by {@code elements}
     * @throws NullPointerException if {@code elements} is null
     */
    public NonEmptyVector<A> appendAll(Vector<? extends A> elements) {
        return new NonEmptyVector<>(vector.appendAll(elements));
    }

    /**
     * @param elements Elements to append
     * @return this vector followed by {@code elements}
     * @throws NullPointerException if {@code elements} is null
     */
    public NonEmptyVector<A> appendAll(NonEmptyVector<? extends A> elements) {
        Objects.requireNonNull(elements, "elements is null");
        return new NonEmptyVector<>(vector.appendAll(elements.vector));
    }

    /**
     * @param that A non-empty vector
     * @return this vector followed by {@code that}; the same as {@link #appendAll(NonEmptyVector)}
     * @throws NullPointerException if {@code that} is null
     */
    public NonEmptyVector<A> concat(NonEmptyVector<? extends A> that) { return appendAll(that); }

    /**
     * @param element An element
     * @return {@code element} followed by this vector
     * @throws NullPointerException if {@code element} is null
     */
    public NonEmptyVector<A> prepend(A element) {
        Objects.requireNonNull(element, "NonEmptyVector.prepend: element is null");
        return new NonEmptyVector<>(vector.prepend(element));
    }

    /**
     * Accepts the possibly empty type and returns the non-empty one.
     *
     * @param elements Elements to prepend, possibly none
     * @return {@code elements} followed by this vector
     * @throws NullPointerException if {@code elements} is null
     */
    public NonEmptyVector<A> prependAll(Vector<? extends A> elements) {
        return new NonEmptyVector<>(vector.prependAll(elements));
    }

    /**
     * @param elements Elements to prepend
     * @return {@code elements} followed by this vector
     * @throws NullPointerException if {@code elements} is null
     */
    public NonEmptyVector<A> prependAll(NonEmptyVector<? extends A> elements) {
        Objects.requireNonNull(elements, "elements is null");
        return new NonEmptyVector<>(vector.prependAll(elements.vector));
    }

    /**
     * @return the elements in reverse order
     */
    public NonEmptyVector<A> reverse() { return new NonEmptyVector<>(vector.reverse()); }

    /**
     * @return the distinct elements, each at its first occurrence
     */
    public NonEmptyVector<A> distinct() { return new NonEmptyVector<>(vector.distinct()); }

    /**
     * @param comparator Decides which elements are equal
     * @return the elements distinct under {@code comparator}, each at its first occurrence
     * @throws NullPointerException if {@code comparator} is null
     */
    public NonEmptyVector<A> distinctBy(Comparator<? super A> comparator) {
        return new NonEmptyVector<>(vector.distinctBy(comparator));
    }

    /**
     * @param keyExtractor Computes the key elements are distinct by
     * @param <K>          Key type
     * @return the elements with distinct keys, each at its first occurrence
     * @throws NullPointerException if {@code keyExtractor} is null
     */
    public <K extends @Nullable Object> NonEmptyVector<A> distinctBy(Function<? super A, ? extends K> keyExtractor) {
        return new NonEmptyVector<>(vector.distinctBy(keyExtractor));
    }

    /**
     * @return the elements sorted by their natural order
     * @throws ClassCastException if the elements are not {@link Comparable}
     */
    public NonEmptyVector<A> sorted() { return new NonEmptyVector<>(vector.sorted()); }

    /**
     * @param comparator The order
     * @return the elements sorted by {@code comparator} (stable)
     * @throws NullPointerException if {@code comparator} is null
     */
    public NonEmptyVector<A> sorted(Comparator<? super A> comparator) {
        return new NonEmptyVector<>(vector.sorted(comparator));
    }

    /**
     * @param mapper Computes the sort key
     * @param <U>    Key type
     * @return the elements sorted by the natural order of their keys (stable)
     * @throws NullPointerException if {@code mapper} is null
     */
    public <U extends Comparable<? super U>> NonEmptyVector<A> sortBy(Function<? super A, ? extends U> mapper) {
        return new NonEmptyVector<>(vector.sortBy(mapper));
    }

    /**
     * @param comparator The order of the keys
     * @param mapper     Computes the sort key
     * @param <U>        Key type
     * @return the elements sorted by their keys under {@code comparator} (stable)
     * @throws NullPointerException if {@code comparator} or {@code mapper} is null
     */
    public <U extends @Nullable Object> NonEmptyVector<A> sortBy(Comparator<? super U> comparator, Function<? super A, ? extends U> mapper) {
        return new NonEmptyVector<>(vector.sortBy(comparator, mapper));
    }

    /**
     * Pairs the elements of both vectors by position, up to the shorter size; both are non-empty, so the result is too.
     *
     * @param that A non-empty vector
     * @param <B>  Component type of {@code that}
     * @return the pairs
     * @throws NullPointerException if {@code that} is null
     */
    public <B extends @Nullable Object> NonEmptyVector<Tuple2<A, B>> zip(NonEmptyVector<? extends B> that) {
        Objects.requireNonNull(that, "that is null");
        return new NonEmptyVector<>(vector.zip(that.vector));
    }

    /**
     * Combines the elements of both vectors by position, up to the shorter size.
     *
     * @param that   A non-empty vector
     * @param mapper Combines two elements
     * @param <B>    Component type of {@code that}
     * @param <R>    Component type of the result
     * @return the combined elements
     * @throws NullPointerException if {@code that} or {@code mapper} is null, or {@code mapper} returns null
     */
    public <B extends @Nullable Object, R extends @Nullable Object> NonEmptyVector<R> zipWith(NonEmptyVector<? extends B> that, BiFunction<? super A, ? super B, ? extends R> mapper) {
        Objects.requireNonNull(that, "that is null");
        return new NonEmptyVector<>(vector.zipWith(that.vector, mapper));
    }

    /**
     * @return each element paired with its index
     */
    public NonEmptyVector<Tuple2<A, Integer>> zipWithIndex() { return new NonEmptyVector<>(vector.zipWithIndex()); }

    /**
     * @param mapper Combines an element and its index
     * @param <B>    Component type of the result
     * @return the combined elements
     * @throws NullPointerException if {@code mapper} is null or returns null
     */
    public <B extends @Nullable Object> NonEmptyVector<B> zipWithIndex(BiFunction<? super A, ? super Integer, ? extends B> mapper) {
        return new NonEmptyVector<>(vector.zipWithIndex(mapper));
    }

    /**
     * The running left fold: {@code zero}, then each intermediate result. Has {@code size() + 1} elements.
     *
     * @param zero      The initial accumulator
     * @param operation Combines the accumulator and an element
     * @param <B>       Accumulator type
     * @return the intermediate results, starting with {@code zero}
     * @throws NullPointerException if {@code zero} or {@code operation} is null, or {@code operation} returns null
     */
    public <B extends @Nullable Object> NonEmptyVector<B> scanLeft(B zero, BiFunction<? super B, ? super A, ? extends B> operation) {
        return new NonEmptyVector<>(vector.scanLeft(zero, operation));
    }

    /**
     * @param index   A position
     * @param element The new element
     * @return this vector with {@code element} at {@code index}
     * @throws IndexOutOfBoundsException if {@code index} is not in {@code [0, size())}
     * @throws NullPointerException      if {@code element} is null
     */
    public NonEmptyVector<A> update(int index, A element) {
        Objects.requireNonNull(element, "NonEmptyVector.update: element is null");
        return new NonEmptyVector<>(vector.update(index, element));
    }

    /**
     * @param index   A position
     * @param updater Computes the new element from the current one
     * @return this vector with {@code updater} applied at {@code index}
     * @throws IndexOutOfBoundsException if {@code index} is not in {@code [0, size())}
     * @throws NullPointerException      if {@code updater} is null or returns null
     */
    public NonEmptyVector<A> update(int index, Function<? super A, ? extends A> updater) {
        return new NonEmptyVector<>(vector.update(index, updater));
    }

    /**
     * Runs {@code action} on every element, in order.
     *
     * @param action A side effect
     * @return this vector
     * @throws NullPointerException if {@code action} is null
     */
    public NonEmptyVector<A> tap(Consumer<? super A> action) {
        vector.tap(action);
        return this;
    }

    /**
     * Splits the elements into consecutive groups of {@code size}; only the last one may be shorter.
     *
     * @param size The group size
     * @return the groups, each non-empty
     * @throws IllegalArgumentException if {@code size} is not positive
     */
    public Vector<NonEmptyVector<A>> grouped(int size) {
        return Vector.ofAll(vector.grouped(size).map(NonEmptyVector::new));
    }

    /**
     * Groups the elements by the key {@code classifier} computes; each group keeps the order of this vector.
     *
     * @param classifier Computes the key of an element
     * @param <K>        Key type
     * @return the groups, each non-empty
     * @throws NullPointerException if {@code classifier} is null or returns null
     */
    public <K extends @Nullable Object> HashMap<K, NonEmptyVector<A>> groupBy(Function<? super A, ? extends K> classifier) {
        HashMap<K, NonEmptyVector<A>> groups = HashMap.empty();
        for (Tuple2<K, Vector<A>> group : vector.<K> groupBy(classifier)) {
            groups = groups.put(group._1(), new NonEmptyVector<>(group._2()));
        }
        return groups;
    }

    // -- returns Vector: the result may be empty

    /**
     * @return the wrapped vector; O(1)
     */
    public Vector<A> toVector() { return vector; }

    /**
     * @param predicate A test
     * @return the elements that pass {@code predicate}
     * @throws NullPointerException if {@code predicate} is null
     */
    public Vector<A> filter(Predicate<? super A> predicate) { return vector.filter(predicate); }

    /**
     * @param predicate A test
     * @return the elements that fail {@code predicate}
     * @throws NullPointerException if {@code predicate} is null
     */
    public Vector<A> reject(Predicate<? super A> predicate) { return vector.reject(predicate); }

    /**
     * Maps and filters in one pass: keeps the {@code Some} results.
     *
     * @param mapper A function returning an {@link Option}
     * @param <B>    Component type of the result
     * @return the defined results, in order
     * @throws NullPointerException if {@code mapper} is null or returns null
     */
    public <B extends @Nullable Object> Vector<B> collect(Function<? super A, ? extends Option<? extends B>> mapper) {
        return vector.collect(mapper);
    }

    /**
     * Concatenates the iterables {@code mapper} returns for each element; they may be empty, so the result may be too.
     * For a function that returns a {@code NonEmptyVector}, see {@link #flatMap(Function)}.
     *
     * @param mapper A function returning an {@link Iterable}
     * @param <B>    Component type of the result
     * @return the concatenation
     * @throws NullPointerException if {@code mapper} is null, returns null, or returns an iterable yielding null
     */
    public <B extends @Nullable Object> Vector<B> flatMapAll(Function<? super A, ? extends Iterable<? extends B>> mapper) {
        return vector.flatMap(mapper);
    }

    /**
     * Splits the elements into a left and a right side according to the {@link Either} {@code f} returns for each; one
     * pass, no intermediate list. Either side may be empty.
     *
     * @param f   Classifies an element
     * @param <L> Component type of the left side
     * @param <R> Component type of the right side
     * @return the left values and the right values, each in order
     * @throws NullPointerException if {@code f} is null or returns null
     */
    public <L extends @Nullable Object, R extends @Nullable Object> Tuple2<Vector<L>, Vector<R>> partitionMap(Function<? super A, ? extends Either<? extends L, ? extends R>> f) {
        Objects.requireNonNull(f, "f is null");
        // Vector's loop, not a delegation, so that a null result is reported under this type's name
        final Vector.Builder<L> lefts = Vector.newBuilder();
        final Vector.Builder<R> rights = Vector.newBuilder();
        for (A element : vector) {
            switch (Objects.requireNonNull(f.apply(element), "NonEmptyVector.partitionMap: f returned null")) {
                case Either.Left(var left) -> lefts.add(left);
                case Either.Right(var right) -> rights.add(right);
            }
        }
        return Tuple.of(lefts.result(), rights.result());
    }

    /**
     * The complement of {@link #distinct()}: the elements occurring more than once, each once, in order of first
     * occurrence. {@code isEmpty()} on the result is the "all distinct" test. O(n).
     *
     * @return the duplicated elements
     */
    public Vector<A> duplicates() { return vector.duplicates(); }

    /**
     * {@link #duplicates()} under a key: the first element of each key occurring more than once, in order of first
     * occurrence. O(n).
     *
     * @param keyExtractor Computes the key
     * @param <K>          Key type
     * @return the first element of each duplicated key
     * @throws NullPointerException if {@code keyExtractor} is null
     */
    public <K extends @Nullable Object> Vector<A> duplicatesBy(Function<? super A, ? extends K> keyExtractor) {
        return vector.duplicatesBy(keyExtractor);
    }

    /**
     * @return all elements but the first; empty when {@code size()} is 1. See {@link #tailNonEmpty()}.
     */
    public Vector<A> tail() { return vector.tail(); }

    /**
     * @return all elements but the last; empty when {@code size()} is 1. See {@link #initNonEmpty()}.
     */
    public Vector<A> init() { return vector.init(); }

    /**
     * @param n A count
     * @return all elements but the first {@code n}; all of them if {@code n <= 0}, none if {@code n >= size()}
     */
    public Vector<A> drop(int n) { return vector.drop(n); }

    /**
     * @param n A count
     * @return all elements but the last {@code n}; all of them if {@code n <= 0}, none if {@code n >= size()}
     */
    public Vector<A> dropRight(int n) { return vector.dropRight(n); }

    /**
     * @param predicate A test
     * @return the elements from the first one that fails {@code predicate} on
     * @throws NullPointerException if {@code predicate} is null
     */
    public Vector<A> dropWhile(Predicate<? super A> predicate) { return vector.dropWhile(predicate); }

    /**
     * @param predicate A test
     * @return the elements from the first one that passes {@code predicate} on
     * @throws NullPointerException if {@code predicate} is null
     */
    public Vector<A> dropUntil(Predicate<? super A> predicate) { return vector.dropUntil(predicate); }

    /**
     * @param predicate A test
     * @return the elements up to and including the last one that fails {@code predicate}
     * @throws NullPointerException if {@code predicate} is null
     */
    public Vector<A> dropRightWhile(Predicate<? super A> predicate) { return vector.dropRightWhile(predicate); }

    /**
     * @param predicate A test
     * @return the elements up to and including the last one that passes {@code predicate}
     * @throws NullPointerException if {@code predicate} is null
     */
    public Vector<A> dropRightUntil(Predicate<? super A> predicate) { return vector.dropRightUntil(predicate); }

    /**
     * @param n A count
     * @return the first {@code n} elements; none if {@code n <= 0}, all of them if {@code n >= size()}
     */
    public Vector<A> take(int n) { return vector.take(n); }

    /**
     * @param n A count
     * @return the last {@code n} elements; none if {@code n <= 0}, all of them if {@code n >= size()}
     */
    public Vector<A> takeRight(int n) { return vector.takeRight(n); }

    /**
     * @param predicate A test
     * @return the leading elements that pass {@code predicate}
     * @throws NullPointerException if {@code predicate} is null
     */
    public Vector<A> takeWhile(Predicate<? super A> predicate) { return vector.takeWhile(predicate); }

    /**
     * @param predicate A test
     * @return the leading elements that fail {@code predicate}
     * @throws NullPointerException if {@code predicate} is null
     */
    public Vector<A> takeUntil(Predicate<? super A> predicate) { return vector.takeUntil(predicate); }

    /**
     * @param predicate A test
     * @return the trailing elements that pass {@code predicate}
     * @throws NullPointerException if {@code predicate} is null
     */
    public Vector<A> takeRightWhile(Predicate<? super A> predicate) { return vector.takeRightWhile(predicate); }

    /**
     * @param predicate A test
     * @return the trailing elements that fail {@code predicate}
     * @throws NullPointerException if {@code predicate} is null
     */
    public Vector<A> takeRightUntil(Predicate<? super A> predicate) { return vector.takeRightUntil(predicate); }

    /**
     * @param beginIndex The first index, inclusive
     * @param endIndex   The last index, exclusive
     * @return the elements in {@code [beginIndex, endIndex)}, both clamped to {@code [0, size()]}
     */
    public Vector<A> slice(int beginIndex, int endIndex) { return vector.slice(beginIndex, endIndex); }

    /**
     * @param index A position
     * @return this vector without the element at {@code index}
     * @throws IndexOutOfBoundsException if {@code index} is not in {@code [0, size())}
     */
    public Vector<A> removeAt(int index) { return vector.removeAt(index); }

    /**
     * @param element An element
     * @return this vector without the first occurrence of {@code element}
     */
    public Vector<A> remove(A element) { return vector.remove(element); }

    /**
     * @param element An element
     * @return this vector without any occurrence of {@code element}
     */
    public Vector<A> removeAll(A element) { return vector.removeAll(element); }

    /**
     * @param elements Elements
     * @return this vector without any occurrence of any of {@code elements}
     * @throws NullPointerException if {@code elements} is null
     */
    public Vector<A> removeAll(Iterable<? extends A> elements) { return vector.removeAll(elements); }

    /**
     * @param predicate A test
     * @return this vector without the elements that pass {@code predicate}
     * @throws NullPointerException if {@code predicate} is null
     */
    public Vector<A> removeAll(Predicate<? super A> predicate) { return vector.removeAll(predicate); }

    // -- total: what is partial on a Vector

    /**
     * @return the first element
     */
    public A head() { return vector.get(0); }

    /**
     * @return the last element
     */
    public A last() { return vector.get(vector.length() - 1); }

    /**
     * @param comparator The order
     * @return the greatest element; the first one, if several are equal under {@code comparator}
     * @throws NullPointerException if {@code comparator} is null
     */
    public A max(Comparator<? super A> comparator) {
        Objects.requireNonNull(comparator, "comparator is null");
        final java.util.Iterator<A> iterator = vector.iterator();
        A max = iterator.next();
        while (iterator.hasNext()) {
            final A element = iterator.next();
            if (comparator.compare(element, max) > 0) {
                max = element;
            }
        }
        return max;
    }

    /**
     * @param comparator The order
     * @return the least element; the first one, if several are equal under {@code comparator}
     * @throws NullPointerException if {@code comparator} is null
     */
    public A min(Comparator<? super A> comparator) {
        Objects.requireNonNull(comparator, "comparator is null");
        final java.util.Iterator<A> iterator = vector.iterator();
        A min = iterator.next();
        while (iterator.hasNext()) {
            final A element = iterator.next();
            if (comparator.compare(element, min) < 0) {
                min = element;
            }
        }
        return min;
    }

    /**
     * @param f   Computes the key
     * @param <U> Key type
     * @return the element with the greatest key; the first one, if several keys are equal. {@code f} is applied once per element
     * @throws NullPointerException if {@code f} is null
     */
    public <U extends Comparable<? super U>> A maxBy(Function<? super A, ? extends U> f) {
        Objects.requireNonNull(f, "f is null");
        final java.util.Iterator<A> iterator = vector.iterator();
        A max = iterator.next();
        U maxKey = f.apply(max);
        while (iterator.hasNext()) {
            final A element = iterator.next();
            final U key = f.apply(element);
            if (key.compareTo(maxKey) > 0) {
                max = element;
                maxKey = key;
            }
        }
        return max;
    }

    /**
     * @param f   Computes the key
     * @param <U> Key type
     * @return the element with the least key; the first one, if several keys are equal. {@code f} is applied once per element
     * @throws NullPointerException if {@code f} is null
     */
    public <U extends Comparable<? super U>> A minBy(Function<? super A, ? extends U> f) {
        Objects.requireNonNull(f, "f is null");
        final java.util.Iterator<A> iterator = vector.iterator();
        A min = iterator.next();
        U minKey = f.apply(min);
        while (iterator.hasNext()) {
            final A element = iterator.next();
            final U key = f.apply(element);
            if (key.compareTo(minKey) < 0) {
                min = element;
                minKey = key;
            }
        }
        return min;
    }

    /**
     * @param op Combines two elements
     * @return the elements combined from the left: {@code op(op(a0, a1), a2)...}; the same as {@link #reduceLeft(BiFunction)}
     * @throws NullPointerException if {@code op} is null
     */
    public A reduce(BiFunction<? super A, ? super A, ? extends A> op) { return vector.reduceLeft(op); }

    /**
     * @param op Combines two elements
     * @return the elements combined from the left: {@code op(op(a0, a1), a2)...}
     * @throws NullPointerException if {@code op} is null
     */
    public A reduceLeft(BiFunction<? super A, ? super A, ? extends A> op) { return vector.reduceLeft(op); }

    /**
     * @param op Combines two elements
     * @return the elements combined from the right: {@code op(a0, op(a1, a2))...}
     * @throws NullPointerException if {@code op} is null
     */
    public A reduceRight(BiFunction<? super A, ? super A, ? extends A> op) { return vector.reduceRight(op); }

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
        Objects.requireNonNull(mapper, "mapper is null");
        Objects.requireNonNull(op, "op is null");
        final java.util.Iterator<A> iterator = vector.iterator();
        B result = mapper.apply(iterator.next());
        while (iterator.hasNext()) {
            result = op.apply(result, mapper.apply(iterator.next()));
        }
        return result;
    }

    /**
     * @return the number of elements, at least 1
     */
    public int size() { return vector.length(); }

    /**
     * @param index A position
     * @return the element at {@code index}; effectively O(1)
     * @throws IndexOutOfBoundsException if {@code index} is not in {@code [0, size())}
     */
    public A get(int index) { return vector.get(index); }

    /**
     * @return the elements' {@code toString()}s, concatenated
     */
    public String mkString() { return vector.mkString(); }

    /**
     * @param delimiter Put between elements
     * @return the elements' {@code toString()}s, joined by {@code delimiter}
     */
    public String mkString(CharSequence delimiter) { return vector.mkString(delimiter); }

    /**
     * @param prefix    Put first
     * @param delimiter Put between elements
     * @param suffix    Put last
     * @return the elements' {@code toString()}s, joined by {@code delimiter}, between {@code prefix} and {@code suffix}
     */
    public String mkString(CharSequence prefix, CharSequence delimiter, CharSequence suffix) {
        return vector.mkString(prefix, delimiter, suffix);
    }

    /**
     * @param zero The initial accumulator
     * @param f    Combines the accumulator and an element
     * @param <B>  Accumulator type
     * @return {@code f(f(f(zero, a0), a1), a2)...}
     * @throws NullPointerException if {@code f} is null
     */
    public <B extends @Nullable Object> B foldLeft(B zero, BiFunction<? super B, ? super A, ? extends B> f) {
        return vector.foldLeft(zero, f);
    }

    /**
     * @param zero The initial accumulator
     * @param f    Combines an element and the accumulator
     * @param <B>  Accumulator type
     * @return {@code f(a0, f(a1, f(a2, zero)))...}
     * @throws NullPointerException if {@code f} is null
     */
    public <B extends @Nullable Object> B foldRight(B zero, BiFunction<? super A, ? super B, ? extends B> f) {
        return vector.foldRight(zero, f);
    }

    /**
     * @param element An element
     * @return whether an element equal to {@code element} is present
     */
    public boolean contains(A element) { return vector.contains(element); }

    /**
     * @param predicate A test
     * @return whether at least one element passes {@code predicate}
     * @throws NullPointerException if {@code predicate} is null
     */
    public boolean exists(Predicate<? super A> predicate) { return vector.exists(predicate); }

    /**
     * @param predicate A test
     * @return whether every element passes {@code predicate}
     * @throws NullPointerException if {@code predicate} is null
     */
    public boolean forAll(Predicate<? super A> predicate) { return vector.forAll(predicate); }

    /**
     * @param predicate A test
     * @return how many elements pass {@code predicate}
     * @throws NullPointerException if {@code predicate} is null
     */
    public int count(Predicate<? super A> predicate) { return vector.count(predicate); }

    /**
     * @param element An element
     * @return the index of the first element equal to {@code element}, or -1. See {@link #indexOfOption(Object)}
     */
    public int indexOf(A element) { return vector.indexOf(element); }

    @Override
    public java.util.Iterator<A> iterator() { return vector.iterator(); }

    @Override
    public Spliterator<A> spliterator() {
        return Spliterators.spliterator(iterator(), vector.length(), Spliterator.ORDERED | Spliterator.IMMUTABLE | Spliterator.NONNULL);
    }

    /**
     * @return a sequential {@link java.util.stream.Stream} over the elements
     */
    public java.util.stream.Stream<A> stream() { return vector.stream(); }

    /**
     * @return an immutable {@link java.util.List} view of the elements; O(1)
     */
    public java.util.List<A> asJava() { return vector.asJava(); }

    /**
     * @return the elements as a {@link List}
     */
    public List<A> toList() { return vector.toList(); }

    /**
     * @return the elements as a {@link Set}
     */
    public Set<A> toSet() { return vector.toSet(); }

    // -- returns Option

    /**
     * @param predicate A test
     * @return the first element that passes {@code predicate}
     * @throws NullPointerException if {@code predicate} is null
     */
    public Option<A> find(Predicate<? super A> predicate) { return vector.find(predicate); }

    /**
     * @param predicate A test
     * @return the last element that passes {@code predicate}
     * @throws NullPointerException if {@code predicate} is null
     */
    public Option<A> findLast(Predicate<? super A> predicate) { return vector.findLast(predicate); }

    /**
     * @param element An element
     * @return the index of the first element equal to {@code element}
     */
    public Option<Integer> indexOfOption(A element) { return vector.indexOfOption(element); }

    /**
     * @return all elements but the first, if there are any
     */
    public Option<NonEmptyVector<A>> tailNonEmpty() {
        return vector.length() == 1 ? Option.none() : Option.some(new NonEmptyVector<>(vector.tail()));
    }

    /**
     * @return all elements but the last, if there are any
     */
    public Option<NonEmptyVector<A>> initNonEmpty() {
        return vector.length() == 1 ? Option.none() : Option.some(new NonEmptyVector<>(vector.init()));
    }

    // -- Object

    @Override
    public boolean equals(@Nullable Object o) {
        return o == this || (o instanceof NonEmptyVector<?> that && vector.equals(that.vector));
    }

    @Override
    public int hashCode() { return vector.hashCode(); }

    @Override
    public String toString() { return vector.mkString("NonEmptyVector(", ", ", ")"); }
}
