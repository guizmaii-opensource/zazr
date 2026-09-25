package com.guizmaii.zazr.collection;

import com.guizmaii.zazr.Tuple;
import com.guizmaii.zazr.Tuple2;
import com.guizmaii.zazr.Tuple3;
import com.guizmaii.zazr.collection.internal.Comparators;
import com.guizmaii.zazr.collection.internal.TraversableModule;
import com.guizmaii.zazr.control.Either;
import com.guizmaii.zazr.control.Option;
import java.util.Comparator;
import java.util.Objects;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.ObjIntConsumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collector;
import org.jspecify.annotations.Nullable;

/**
 * A {@link Vector} with at least one element (ZIO's {@code NonEmptyChunk}): a wrapper, not a subtype, so that every
 * method can state in its return type whether the result is still non-empty.
 * <p>
 * The contract, per method family:
 * <ul>
 * <li>operations that preserve or grow the size return a {@code NonEmptyVector}: {@code map}, {@code flatMap},
 * {@code as}, {@code append*}, {@code prepend*}, {@code concat}, {@code insert*}, {@code intersperse}, {@code padTo},
 * {@code leftPadTo}, {@code reverse}, {@code rotate*}, {@code shuffle}, {@code replace*}, {@code distinct*},
 * {@code sorted}, {@code sortBy}, {@code zip(NonEmptyVector)}, {@code zipWith(NonEmptyVector, ...)}, {@code zipAll},
 * {@code zipWithIndex}, {@code scan}, {@code scanLeft}, {@code scanRight}, {@code update}, {@code tap},
 * {@code permutations}, {@code combinations()}, {@code crossProduct()}, {@code crossProduct(NonEmptyVector)};
 * {@code unzip} and {@code unzip3} return tuples of them; {@code grouped}, {@code sliding}, {@code slideBy} and
 * {@code groupBy} return non-empty groups, as {@code splitAtInclusive} returns a non-empty first part;</li>
 * <li>operations that can shrink return a {@link Vector}, or a tuple of them: {@code filter}, {@code reject},
 * {@code collect}, {@code flatMapAll}, {@code tail}, {@code init}, {@code drop*}, {@code take*}, {@code slice},
 * {@code subSequence}, {@code patch}, {@code remove*}, {@code retainAll}, {@code duplicates*}, {@code partition},
 * {@code partitionMap}, {@code span}, {@code splitAt}, the {@code zip}, {@code zipWith} and {@code crossProduct} of
 * any {@code Iterable}, {@code crossProduct(int)}, {@code combinations(int)};</li>
 * <li>operations that are partial on a {@code Vector} are total here: {@code head}, {@code last}, {@code max}, {@code min},
 * {@code maxBy}, {@code minBy}, {@code reduce*}, {@code reduceMap}, {@code average};</li>
 * <li>narrowing back to a {@code NonEmptyVector} returns an {@link Option}: {@code tailNonEmpty}, {@code initNonEmpty},
 * as do the searches {@code find}, {@code findLast}, the {@code *Option} index searches and {@code arrangeBy}.</li>
 * </ul>
 * The {@code Option} forms of what is total here ({@code headOption}, {@code reduceOption}, ...) are absent, and so are
 * the members that are constant on a non-empty collection ({@code isEmpty}, {@code nonEmpty}, {@code orElse},
 * {@code toNonEmptyVector}). The size is {@link #size()}.
 * Two {@code flatMap}s cannot share a name (a lambda argument would be ambiguous), so {@code flatMap} is the one whose
 * function returns a {@code NonEmptyVector} and {@code flatMapAll} the one whose function returns any {@code Iterable}.
 * <p>
 * Like every collection, it rejects null elements. Equal only to another {@code NonEmptyVector} with the same elements
 * in the same order, never to a {@code Vector}; use {@link #toVector()} to compare across the two.
 * <p>
 * Complexity: every method costs what the same method of the wrapped {@link Vector} costs; wrapping and unwrapping
 * copy nothing. The methods without a note of their own ({@code map}, {@code flatMap}, {@code filter}, the folds,
 * {@code groupBy}, the conversions and the factories) walk the elements once: O(n).
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
     * Concatenates a non-empty vector of non-empty vectors. Static, like every {@code flatten} in Zazr, because Java
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

    /**
     * Transposes a matrix given as non-empty rows of the same size: {@code ((1, 2, 3), (4, 5, 6))} becomes
     * {@code ((1, 4), (2, 5), (3, 6))}. Static, like {@link Vector#transpose(Vector)}, because Java cannot demand of an
     * instance method that the receiver's element type be a collection.
     * <p>
     * Complexity: O(rows * columns), as {@link Vector#transpose(Vector)}.
     *
     * @param matrix The rows
     * @param <A>    Component type of the rows
     * @return the columns, one per element of a row, each with one element per row
     * @throws IllegalArgumentException if the rows differ in size
     * @throws NullPointerException     if {@code matrix} is null
     */
    public static <A extends @Nullable Object> NonEmptyVector<NonEmptyVector<A>> transpose(NonEmptyVector<? extends NonEmptyVector<? extends A>> matrix) {
        Objects.requireNonNull(matrix, "matrix is null");
        final Vector<Vector<A>> rows = matrix.vector.map(row -> Vector.<A> narrow(row.toVector()));
        return new NonEmptyVector<>(Vector.transpose(rows).map(NonEmptyVector::new));
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
     * Complexity: effectively O(1), as {@link Vector#append(Object)}, including its O(n) case on primitive values.
     *
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
     * <p>
     * Complexity: O(m) for m appended elements, as {@link Vector#appendAll(Iterable)},
     * including its O(n) case on primitive values.
     *
     * @param elements Elements to append, possibly none
     * @return this vector followed by {@code elements}
     * @throws NullPointerException if {@code elements} is null
     */
    public NonEmptyVector<A> appendAll(Vector<? extends A> elements) {
        return new NonEmptyVector<>(vector.appendAll(elements));
    }

    /**
     * Complexity: O(m) for m appended elements, as {@link Vector#appendAll(Iterable)},
     * including its O(n) case on primitive values.
     *
     * @param elements Elements to append
     * @return this vector followed by {@code elements}
     * @throws NullPointerException if {@code elements} is null
     */
    public NonEmptyVector<A> appendAll(NonEmptyVector<? extends A> elements) {
        Objects.requireNonNull(elements, "elements is null");
        return new NonEmptyVector<>(vector.appendAll(elements.vector));
    }

    /**
     * Complexity: O(m) for m appended elements, as {@link #appendAll(NonEmptyVector)},
     * including its O(n) case on primitive values.
     *
     * @param that A non-empty vector
     * @return this vector followed by {@code that}; the same as {@link #appendAll(NonEmptyVector)}
     * @throws NullPointerException if {@code that} is null
     */
    public NonEmptyVector<A> concat(NonEmptyVector<? extends A> that) { return appendAll(that); }

    /**
     * Complexity: effectively O(1), as {@link Vector#prepend(Object)}, including its O(n) case on primitive values.
     *
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
     * <p>
     * Complexity: O(m) for m prepended elements, as {@link Vector#prependAll(Iterable)},
     * including its O(n) case on primitive values.
     *
     * @param elements Elements to prepend, possibly none
     * @return {@code elements} followed by this vector
     * @throws NullPointerException if {@code elements} is null
     */
    public NonEmptyVector<A> prependAll(Vector<? extends A> elements) {
        return new NonEmptyVector<>(vector.prependAll(elements));
    }

    /**
     * Complexity: O(m) for m prepended elements, as {@link Vector#prependAll(Iterable)},
     * including its O(n) case on primitive values.
     *
     * @param elements Elements to prepend
     * @return {@code elements} followed by this vector
     * @throws NullPointerException if {@code elements} is null
     */
    public NonEmptyVector<A> prependAll(NonEmptyVector<? extends A> elements) {
        Objects.requireNonNull(elements, "elements is null");
        return new NonEmptyVector<>(vector.prependAll(elements.vector));
    }

    /**
     * Complexity: O(n), as {@link Vector#reverse()}.
     *
     * @return the elements in reverse order
     */
    public NonEmptyVector<A> reverse() { return new NonEmptyVector<>(vector.reverse()); }

    /**
     * Complexity: O(n), as {@link Vector#distinct()}.
     *
     * @return the distinct elements, each at its first occurrence
     */
    public NonEmptyVector<A> distinct() { return new NonEmptyVector<>(vector.distinct()); }

    /**
     * Complexity: O(n log n) comparisons, as {@link Vector#distinctBy(Comparator)}.
     *
     * @param comparator Decides which elements are equal
     * @return the elements distinct under {@code comparator}, each at its first occurrence
     * @throws NullPointerException if {@code comparator} is null
     */
    public NonEmptyVector<A> distinctBy(Comparator<? super A> comparator) {
        return new NonEmptyVector<>(vector.distinctBy(comparator));
    }

    /**
     * Complexity: O(n), as {@link Vector#distinctBy(Function)}.
     *
     * @param keyExtractor Computes the key elements are distinct by
     * @param <K>          Key type
     * @return the elements with distinct keys, each at its first occurrence
     * @throws NullPointerException if {@code keyExtractor} is null
     */
    public <K extends @Nullable Object> NonEmptyVector<A> distinctBy(Function<? super A, ? extends K> keyExtractor) {
        return new NonEmptyVector<>(vector.distinctBy(keyExtractor));
    }

    /**
     * Complexity: O(n log n) comparisons, as {@link Vector#sorted()}.
     *
     * @return the elements sorted by their natural order
     * @throws ClassCastException if the elements are not {@link Comparable}
     */
    public NonEmptyVector<A> sorted() { return new NonEmptyVector<>(vector.sorted()); }

    /**
     * Complexity: O(n log n) comparisons, as {@link Vector#sorted(Comparator)}.
     *
     * @param comparator The order
     * @return the elements sorted by {@code comparator} (stable)
     * @throws NullPointerException if {@code comparator} is null
     */
    public NonEmptyVector<A> sorted(Comparator<? super A> comparator) {
        return new NonEmptyVector<>(vector.sorted(comparator));
    }

    /**
     * Complexity: O(n log n) comparisons, as {@link Vector#sortBy(Function)}.
     *
     * @param mapper Computes the sort key
     * @param <U>    Key type
     * @return the elements sorted by the natural order of their keys (stable)
     * @throws NullPointerException if {@code mapper} is null
     */
    public <U extends Comparable<? super U>> NonEmptyVector<A> sortBy(Function<? super A, ? extends U> mapper) {
        return new NonEmptyVector<>(vector.sortBy(mapper));
    }

    /**
     * Complexity: O(n log n) comparisons, as {@link Vector#sortBy(Comparator, Function)}.
     *
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
     * <p>
     * Complexity: O(min(n, m)) for m elements of {@code that}, as {@link Vector#zip(Iterable)}.
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
     * <p>
     * Complexity: O(min(n, m)) for m elements of {@code that}, as {@link Vector#zipWith(Iterable, BiFunction)}.
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
     * Complexity: O(n), as {@link Vector#zipWithIndex()}.
     *
     * @return each element paired with its index
     */
    public NonEmptyVector<Tuple2<A, Integer>> zipWithIndex() { return new NonEmptyVector<>(vector.zipWithIndex()); }

    /**
     * Complexity: O(n), as {@link Vector#zipWithIndex(BiFunction)}.
     *
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
     * <p>
     * Complexity: O(n), as {@link Vector#scanLeft(Object, BiFunction)}.
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
     * Complexity: effectively O(1), as {@link Vector#update(int, Object)}, including its O(n) case on primitive values.
     *
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
     * Complexity: effectively O(1), as {@link Vector#update(int, Function)},
     * including its O(n) case on primitive values.
     *
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
     * <p>
     * Complexity: O(n / size), as {@link Vector#grouped(int)}: each block shares its elements with this vector.
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

    /**
     * Replaces every element by {@code value}.
     * <p>
     * Complexity: O(n), as {@link Vector#as(Object)}.
     *
     * @param value The element of the result
     * @param <B>   Component type of the result
     * @return a non-empty vector of the same size, every element {@code value}
     * @throws NullPointerException if {@code value} is null
     */
    public <B extends @Nullable Object> NonEmptyVector<B> as(B value) {
        Objects.requireNonNull(value, "NonEmptyVector.as: value is null");
        return new NonEmptyVector<>(vector.as(value));
    }

    /**
     * Accepts any iterable, possibly empty, and returns the non-empty type. The elements are read once.
     * <p>
     * Complexity: O(m) for m appended elements, as {@link Vector#appendAll(Iterable)},
     * including its O(n) case on primitive values.
     *
     * @param elements Elements to append, possibly none
     * @return this vector followed by {@code elements}
     * @throws NullPointerException if {@code elements} or one of its elements is null
     */
    public NonEmptyVector<A> appendAll(Iterable<? extends A> elements) {
        Objects.requireNonNull(elements, "elements is null");
        return new NonEmptyVector<>(vector.appendAll(addAll(Vector.newBuilder(), elements).result()));
    }

    /**
     * Accepts any iterable, possibly empty, and returns the non-empty type. The elements are read once.
     * <p>
     * Complexity: O(m) for m prepended elements, as {@link Vector#prependAll(Iterable)},
     * including its O(n) case on primitive values.
     *
     * @param elements Elements to prepend, possibly none
     * @return {@code elements} followed by this vector
     * @throws NullPointerException if {@code elements} or one of its elements is null
     */
    public NonEmptyVector<A> prependAll(Iterable<? extends A> elements) {
        Objects.requireNonNull(elements, "elements is null");
        return new NonEmptyVector<>(vector.prependAll(addAll(Vector.newBuilder(), elements).result()));
    }

    /**
     * Inserts an element at {@code index}; the elements from that position on shift right by one.
     * <p>
     * Complexity: O(min(i, n - i)), as {@link Vector#insert(int, Object)}, including its O(n) case on primitive values.
     *
     * @param index   A position, {@code 0 <= index <= size()}
     * @param element The element to insert
     * @return this vector with {@code element} at {@code index}
     * @throws IndexOutOfBoundsException if {@code index} is not in {@code [0, size()]}
     * @throws NullPointerException      if {@code element} is null
     */
    public NonEmptyVector<A> insert(int index, A element) {
        Objects.requireNonNull(element, "NonEmptyVector.insert: element is null");
        return new NonEmptyVector<>(vector.insert(index, element));
    }

    /**
     * Inserts the elements, possibly none, at {@code index}, in iteration order; the elements from that position on
     * shift right. The elements are read once.
     * <p>
     * Complexity: O(m + min(i, n - i)) for m inserted elements, as {@link Vector#insertAll(int, Iterable)},
     * including its O(n) case on primitive values.
     *
     * @param index    A position, {@code 0 <= index <= size()}
     * @param elements The elements to insert
     * @return this vector with {@code elements} at {@code index}
     * @throws IndexOutOfBoundsException if {@code index} is not in {@code [0, size()]}
     * @throws NullPointerException      if {@code elements} or one of its elements is null
     */
    public NonEmptyVector<A> insertAll(int index, Iterable<? extends A> elements) {
        Objects.requireNonNull(elements, "elements is null");
        return new NonEmptyVector<>(vector.insertAll(index, addAll(Vector.newBuilder(), elements).result()));
    }

    /**
     * Puts {@code element} between every two elements.
     * <p>
     * Complexity: O(n), as {@link Vector#intersperse(Object)}.
     *
     * @param element The separator
     * @return a non-empty vector of {@code 2 * size() - 1} elements
     * @throws NullPointerException if {@code element} is null, whatever the size
     */
    public NonEmptyVector<A> intersperse(A element) {
        Objects.requireNonNull(element, "NonEmptyVector.intersperse: element is null");
        return new NonEmptyVector<>(vector.intersperse(element));
    }

    /**
     * Appends copies of {@code element} until there are {@code length} elements.
     * <p>
     * Complexity: O(k) for the k elements appended, as {@link Vector#padTo(int, Object)},
     * including its O(n) case on primitive values.
     *
     * @param length  The target size
     * @param element The padding element
     * @return this vector if it already has {@code length} or more elements, otherwise this vector padded to it
     * @throws NullPointerException if {@code element} is null, whether or not padding is needed
     */
    public NonEmptyVector<A> padTo(int length, A element) {
        Objects.requireNonNull(element, "NonEmptyVector.padTo: element is null");
        return new NonEmptyVector<>(vector.padTo(length, element));
    }

    /**
     * Prepends copies of {@code element} until there are {@code length} elements.
     * <p>
     * Complexity: O(k) for the k elements prepended, as {@link Vector#leftPadTo(int, Object)},
     * including its O(n) case on primitive values.
     *
     * @param length  The target size
     * @param element The padding element
     * @return this vector if it already has {@code length} or more elements, otherwise this vector padded to it
     * @throws NullPointerException if {@code element} is null, whether or not padding is needed
     */
    public NonEmptyVector<A> leftPadTo(int length, A element) {
        Objects.requireNonNull(element, "NonEmptyVector.leftPadTo: element is null");
        return new NonEmptyVector<>(vector.leftPadTo(length, element));
    }

    /**
     * Rotates the elements {@code n} positions to the left: {@code (1, 2, 3, 4, 5).rotateLeft(2)} is
     * {@code (3, 4, 5, 1, 2)}. A negative {@code n} rotates right; {@code n} is taken modulo the size.
     * <p>
     * Complexity: O(k), where k is the distance modulo the size, as {@link Vector#rotateLeft(int)}: a negative
     * distance can cost O(n).
     *
     * @param n The distance
     * @return the rotated elements
     */
    public NonEmptyVector<A> rotateLeft(int n) { return new NonEmptyVector<>(vector.rotateLeft(n)); }

    /**
     * Rotates the elements {@code n} positions to the right: {@code (1, 2, 3, 4, 5).rotateRight(2)} is
     * {@code (4, 5, 1, 2, 3)}. A negative {@code n} rotates left; {@code n} is taken modulo the size.
     * <p>
     * Complexity: O(n - k), where k is the distance modulo the size, as {@link Vector#rotateRight(int)}:
     * {@code rotateRight(1)} is O(n).
     *
     * @param n The distance
     * @return the rotated elements
     */
    public NonEmptyVector<A> rotateRight(int n) { return new NonEmptyVector<>(vector.rotateRight(n)); }

    /**
     * Complexity: O(n), as {@link Vector#shuffle()}.
     *
     * @return the elements in a uniformly random order
     */
    public NonEmptyVector<A> shuffle() { return new NonEmptyVector<>(vector.shuffle()); }

    /**
     * Complexity: O(n) to find the element, then one effectively O(1) update, as
     * {@link Vector#replace(Object, Object)}.
     *
     * @param currentElement The element to replace
     * @param newElement     The replacement
     * @return this vector with the first occurrence of {@code currentElement} replaced by {@code newElement}
     * @throws NullPointerException if {@code newElement} is null, whether or not {@code currentElement} occurs
     */
    public NonEmptyVector<A> replace(A currentElement, A newElement) {
        Objects.requireNonNull(newElement, "NonEmptyVector.replace: newElement is null");
        return new NonEmptyVector<>(vector.replace(currentElement, newElement));
    }

    /**
     * Complexity: O(n) plus one effectively O(1) update per occurrence, as {@link Vector#replaceAll(Object, Object)}.
     *
     * @param currentElement The element to replace
     * @param newElement     The replacement
     * @return this vector with every occurrence of {@code currentElement} replaced by {@code newElement}
     * @throws NullPointerException if {@code newElement} is null, whether or not {@code currentElement} occurs
     */
    public NonEmptyVector<A> replaceAll(A currentElement, A newElement) {
        Objects.requireNonNull(newElement, "NonEmptyVector.replaceAll: newElement is null");
        return new NonEmptyVector<>(vector.replaceAll(currentElement, newElement));
    }

    /**
     * {@link #scanLeft(Object, BiFunction)} with an accumulator of the element type. Has {@code size() + 1} elements.
     * <p>
     * Complexity: O(n), as {@link Vector#scan(Object, BiFunction)}.
     *
     * @param zero      The initial accumulator
     * @param operation Combines the accumulator and an element
     * @return the intermediate results, starting with {@code zero}
     * @throws NullPointerException if {@code zero} or {@code operation} is null, or {@code operation} returns null
     */
    public NonEmptyVector<A> scan(A zero, BiFunction<? super A, ? super A, ? extends A> operation) {
        Objects.requireNonNull(zero, "NonEmptyVector.scan: zero is null");
        Objects.requireNonNull(operation, "operation is null");
        return new NonEmptyVector<>(vector.scan(zero, (acc, element) -> Objects.requireNonNull(operation.apply(acc, element), "NonEmptyVector.scan: operation returned null")));
    }

    /**
     * The running right fold: each intermediate result, then {@code zero} last. Has {@code size() + 1} elements.
     * <p>
     * Complexity: O(n), as {@link Vector#scanRight(Object, BiFunction)}.
     *
     * @param zero      The initial accumulator
     * @param operation Combines an element and the accumulator
     * @param <B>       Accumulator type
     * @return the intermediate results, ending with {@code zero}
     * @throws NullPointerException if {@code zero} or {@code operation} is null, or {@code operation} returns null
     */
    public <B extends @Nullable Object> NonEmptyVector<B> scanRight(B zero, BiFunction<? super A, ? super B, ? extends B> operation) {
        Objects.requireNonNull(zero, "NonEmptyVector.scanRight: zero is null");
        Objects.requireNonNull(operation, "operation is null");
        return new NonEmptyVector<>(vector.scanRight(zero, (element, acc) -> Objects.requireNonNull(operation.apply(element, acc), "NonEmptyVector.scanRight: operation returned null")));
    }

    /**
     * Pairs the elements of both sides by position, up to the longer size, filling the shorter side; at least as long
     * as this vector, so non-empty. {@code that} is read once.
     * <p>
     * Complexity: O(max(n, m)) for m elements of {@code that}, as {@link Vector#zipAll(Iterable, Object, Object)}.
     *
     * @param that     The right-hand elements, possibly none
     * @param thisElem Fills this side when it is the shorter
     * @param thatElem Fills {@code that} side when it is the shorter
     * @param <B>      Component type of {@code that}
     * @return the pairs
     * @throws NullPointerException if an argument or an element of {@code that} is null
     */
    public <B extends @Nullable Object> NonEmptyVector<Tuple2<A, B>> zipAll(Iterable<? extends B> that, A thisElem, B thatElem) {
        Objects.requireNonNull(that, "that is null");
        Objects.requireNonNull(thisElem, "NonEmptyVector.zipAll: thisElem is null");
        Objects.requireNonNull(thatElem, "NonEmptyVector.zipAll: thatElem is null");
        return new NonEmptyVector<>(vector.zipAll(addAll(Vector.<B> newBuilder(), that).result(), thisElem, thatElem));
    }

    /**
     * Complexity: O(n log n) comparisons, as {@link Vector#distinctByKeepLast(Comparator)}.
     *
     * @param comparator Decides which elements are equal
     * @return the elements distinct under {@code comparator}, each at its last occurrence
     * @throws NullPointerException if {@code comparator} is null
     */
    public NonEmptyVector<A> distinctByKeepLast(Comparator<? super A> comparator) {
        return new NonEmptyVector<>(vector.distinctByKeepLast(comparator));
    }

    /**
     * Complexity: O(n), as {@link Vector#distinctByKeepLast(Function)}.
     *
     * @param keyExtractor Computes the key elements are distinct by
     * @param <K>          Key type
     * @return the elements with distinct keys, each at its last occurrence
     * @throws NullPointerException if {@code keyExtractor} is null or returns null
     */
    public <K extends @Nullable Object> NonEmptyVector<A> distinctByKeepLast(Function<? super A, ? extends K> keyExtractor) {
        Objects.requireNonNull(keyExtractor, "keyExtractor is null");
        return new NonEmptyVector<>(vector.distinctByKeepLast(element -> Objects.requireNonNull(keyExtractor.apply(element), "NonEmptyVector.distinctByKeepLast: keyExtractor returned null")));
    }

    /**
     * Splits every element into two with {@code unzipper}: one pass, two builders. Both sides have this vector's size.
     * <p>
     * Complexity: O(n).
     *
     * @param unzipper Splits an element
     * @param <T1>     Component type of the first side
     * @param <T2>     Component type of the second side
     * @return the first halves and the second halves, each in order
     * @throws NullPointerException if {@code unzipper} is null, returns null, or returns a tuple with a null component
     */
    public <T1 extends @Nullable Object, T2 extends @Nullable Object> Tuple2<NonEmptyVector<T1>, NonEmptyVector<T2>> unzip(Function<? super A, Tuple2<? extends T1, ? extends T2>> unzipper) {
        Objects.requireNonNull(unzipper, "unzipper is null");
        final Vector.Builder<T1> xs = Vector.newBuilder(size());
        final Vector.Builder<T2> ys = Vector.newBuilder(size());
        for (A element : vector) {
            final Tuple2<? extends T1, ? extends T2> t = Objects.requireNonNull(unzipper.apply(element), "NonEmptyVector.unzip: unzipper returned null");
            xs.add(Objects.requireNonNull(t._1(), "NonEmptyVector.unzip: unzipper returned a null component"));
            ys.add(Objects.requireNonNull(t._2(), "NonEmptyVector.unzip: unzipper returned a null component"));
        }
        return Tuple.of(new NonEmptyVector<>(xs.result()), new NonEmptyVector<>(ys.result()));
    }

    /**
     * Splits every element into three with {@code unzipper}: one pass, three builders. The three sides have this
     * vector's size.
     * <p>
     * Complexity: O(n).
     *
     * @param unzipper Splits an element
     * @param <T1>     Component type of the first side
     * @param <T2>     Component type of the second side
     * @param <T3>     Component type of the third side
     * @return the three sides, each in order
     * @throws NullPointerException if {@code unzipper} is null, returns null, or returns a tuple with a null component
     */
    public <T1 extends @Nullable Object, T2 extends @Nullable Object, T3 extends @Nullable Object> Tuple3<NonEmptyVector<T1>, NonEmptyVector<T2>, NonEmptyVector<T3>> unzip3(Function<? super A, Tuple3<? extends T1, ? extends T2, ? extends T3>> unzipper) {
        Objects.requireNonNull(unzipper, "unzipper is null");
        final Vector.Builder<T1> xs = Vector.newBuilder(size());
        final Vector.Builder<T2> ys = Vector.newBuilder(size());
        final Vector.Builder<T3> zs = Vector.newBuilder(size());
        for (A element : vector) {
            final Tuple3<? extends T1, ? extends T2, ? extends T3> t = Objects.requireNonNull(unzipper.apply(element), "NonEmptyVector.unzip3: unzipper returned null");
            xs.add(Objects.requireNonNull(t._1(), "NonEmptyVector.unzip3: unzipper returned a null component"));
            ys.add(Objects.requireNonNull(t._2(), "NonEmptyVector.unzip3: unzipper returned a null component"));
            zs.add(Objects.requireNonNull(t._3(), "NonEmptyVector.unzip3: unzipper returned a null component"));
        }
        return Tuple.of(new NonEmptyVector<>(xs.result()), new NonEmptyVector<>(ys.result()), new NonEmptyVector<>(zs.result()));
    }

    /**
     * The windows of {@code size} consecutive elements, each starting one element after the previous; a vector shorter
     * than {@code size} is one window. The same as {@code sliding(size, 1)}.
     * <p>
     * Complexity: O(n), as {@link Vector#sliding(int)}: each window shares its elements with this vector.
     *
     * @param size The window size
     * @return the windows, at least one, each non-empty
     * @throws IllegalArgumentException if {@code size} is not positive
     */
    public Vector<NonEmptyVector<A>> sliding(int size) { return vector.sliding(size).map(NonEmptyVector::new); }

    /**
     * The windows of {@code size} consecutive elements, each starting {@code step} elements after the previous; the
     * last one is shorter when it reaches the end, and a window whose elements all belong to the previous one is not
     * produced. A vector shorter than {@code size} is one window.
     * <p>
     * Complexity: O(n / step), as {@link Vector#sliding(int, int)}: each window shares its elements with this vector.
     *
     * @param size The window size
     * @param step The distance between two window starts
     * @return the windows, at least one, each non-empty
     * @throws IllegalArgumentException if {@code size} or {@code step} is not positive
     */
    public Vector<NonEmptyVector<A>> sliding(int size, int step) { return vector.sliding(size, step).map(NonEmptyVector::new); }

    /**
     * The elements in maximal runs of consecutive elements with the same key, computed once per element by
     * {@code classifier}; the runs concatenate back to this vector.
     * <p>
     * Complexity: O(n), as {@link Vector#slideBy(Function)}: each run shares its elements with this vector.
     *
     * @param classifier The key of an element
     * @return the runs, at least one, each non-empty
     * @throws NullPointerException if {@code classifier} is null
     */
    public Vector<NonEmptyVector<A>> slideBy(Function<? super A, ?> classifier) { return vector.slideBy(classifier).map(NonEmptyVector::new); }

    /**
     * All distinct permutations of the elements, in the order the distinct elements first occur.
     * <p>
     * Complexity: O(n! * n^2) in the worst case (all elements distinct), as {@link Vector#permutations()}.
     *
     * @return the permutations, at least one, each of this vector's size
     */
    public NonEmptyVector<NonEmptyVector<A>> permutations() {
        return new NonEmptyVector<>(vector.permutations().map(NonEmptyVector::new));
    }

    /**
     * All combinations of the elements, for every size from 0 to {@code size()}, by position; the first one is empty
     * and the last one is this vector's elements.
     * <p>
     * Complexity: O(n * 2^n), as {@link Vector#combinations()}.
     *
     * @return the combinations, ordered by size, then by position
     */
    public NonEmptyVector<Vector<A>> combinations() { return new NonEmptyVector<>(vector.combinations()); }

    /**
     * The Cartesian square: every pair {@code (a, b)} of elements, {@code a} varying slowest.
     * <p>
     * Complexity: O(n^2), as {@link Vector#crossProduct()}.
     *
     * @return the {@code size() * size()} pairs
     */
    public NonEmptyVector<Tuple2<A, A>> crossProduct() { return new NonEmptyVector<>(vector.crossProduct()); }

    /**
     * The Cartesian product with a non-empty vector: every pair {@code (a, b)}, {@code a} varying slowest.
     * <p>
     * Complexity: O(n * m) for m elements of {@code that}, as {@link Vector#crossProduct(Iterable)}.
     *
     * @param that The right-hand elements
     * @param <B>  Component type of {@code that}
     * @return the {@code size() * that.size()} pairs
     * @throws NullPointerException if {@code that} is null
     */
    public <B extends @Nullable Object> NonEmptyVector<Tuple2<A, B>> crossProduct(NonEmptyVector<? extends B> that) {
        Objects.requireNonNull(that, "that is null");
        return new NonEmptyVector<>(vector.crossProduct(that.vector));
    }

    // -- returns Vector: the result may be empty

    /**
     * Complexity: O(1): the wrapped Vector is returned, nothing is copied.
     *
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
     * <p>
     * Complexity: O(n), as {@link Vector#duplicates()}.
     *
     * @return the duplicated elements
     */
    public Vector<A> duplicates() { return vector.duplicates(); }

    /**
     * {@link #duplicates()} under a key: the first element of each key occurring more than once, in order of first
     * occurrence. O(n).
     * <p>
     * Complexity: O(n), as {@link Vector#duplicatesBy(Function)}.
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
     * Complexity: effectively O(1), as {@link Vector#tail()}.
     *
     * @return all elements but the first; empty when {@code size()} is 1. See {@link #tailNonEmpty()}.
     */
    public Vector<A> tail() { return vector.tail(); }

    /**
     * Complexity: effectively O(1), as {@link Vector#init()}.
     *
     * @return all elements but the last; empty when {@code size()} is 1. See {@link #initNonEmpty()}.
     */
    public Vector<A> init() { return vector.init(); }

    /**
     * Complexity: effectively O(1), as {@link Vector#drop(int)}.
     *
     * @param n A count
     * @return all elements but the first {@code n}; all of them if {@code n <= 0}, none if {@code n >= size()}
     */
    public Vector<A> drop(int n) { return vector.drop(n); }

    /**
     * Complexity: effectively O(1), as {@link Vector#dropRight(int)}.
     *
     * @param n A count
     * @return all elements but the last {@code n}; all of them if {@code n <= 0}, none if {@code n >= size()}
     */
    public Vector<A> dropRight(int n) { return vector.dropRight(n); }

    /**
     * Complexity: O(k) for k dropped elements, as {@link Vector#dropWhile(Predicate)}.
     *
     * @param predicate A test
     * @return the elements from the first one that fails {@code predicate} on
     * @throws NullPointerException if {@code predicate} is null
     */
    public Vector<A> dropWhile(Predicate<? super A> predicate) { return vector.dropWhile(predicate); }

    /**
     * Complexity: O(k) for k dropped elements, as {@link Vector#dropUntil(Predicate)}.
     *
     * @param predicate A test
     * @return the elements from the first one that passes {@code predicate} on
     * @throws NullPointerException if {@code predicate} is null
     */
    public Vector<A> dropUntil(Predicate<? super A> predicate) { return vector.dropUntil(predicate); }

    /**
     * Complexity: O(k) for k dropped elements, as {@link Vector#dropRightWhile(Predicate)}.
     *
     * @param predicate A test
     * @return the elements up to and including the last one that fails {@code predicate}
     * @throws NullPointerException if {@code predicate} is null
     */
    public Vector<A> dropRightWhile(Predicate<? super A> predicate) { return vector.dropRightWhile(predicate); }

    /**
     * Complexity: O(k) for k dropped elements, as {@link Vector#dropRightUntil(Predicate)}.
     *
     * @param predicate A test
     * @return the elements up to and including the last one that passes {@code predicate}
     * @throws NullPointerException if {@code predicate} is null
     */
    public Vector<A> dropRightUntil(Predicate<? super A> predicate) { return vector.dropRightUntil(predicate); }

    /**
     * Complexity: effectively O(1), as {@link Vector#take(int)}.
     *
     * @param n A count
     * @return the first {@code n} elements; none if {@code n <= 0}, all of them if {@code n >= size()}
     */
    public Vector<A> take(int n) { return vector.take(n); }

    /**
     * Complexity: effectively O(1), as {@link Vector#takeRight(int)}.
     *
     * @param n A count
     * @return the last {@code n} elements; none if {@code n <= 0}, all of them if {@code n >= size()}
     */
    public Vector<A> takeRight(int n) { return vector.takeRight(n); }

    /**
     * Complexity: O(k) for k taken elements, as {@link Vector#takeWhile(Predicate)}.
     *
     * @param predicate A test
     * @return the leading elements that pass {@code predicate}
     * @throws NullPointerException if {@code predicate} is null
     */
    public Vector<A> takeWhile(Predicate<? super A> predicate) { return vector.takeWhile(predicate); }

    /**
     * Complexity: O(k) for k taken elements, as {@link Vector#takeUntil(Predicate)}.
     *
     * @param predicate A test
     * @return the leading elements that fail {@code predicate}
     * @throws NullPointerException if {@code predicate} is null
     */
    public Vector<A> takeUntil(Predicate<? super A> predicate) { return vector.takeUntil(predicate); }

    /**
     * Complexity: O(k) for k taken elements, as {@link Vector#takeRightWhile(Predicate)}.
     *
     * @param predicate A test
     * @return the trailing elements that pass {@code predicate}
     * @throws NullPointerException if {@code predicate} is null
     */
    public Vector<A> takeRightWhile(Predicate<? super A> predicate) { return vector.takeRightWhile(predicate); }

    /**
     * Complexity: O(k) for k taken elements, as {@link Vector#takeRightUntil(Predicate)}.
     *
     * @param predicate A test
     * @return the trailing elements that fail {@code predicate}
     * @throws NullPointerException if {@code predicate} is null
     */
    public Vector<A> takeRightUntil(Predicate<? super A> predicate) { return vector.takeRightUntil(predicate); }

    /**
     * Complexity: effectively O(1), as {@link Vector#slice(int, int)}.
     *
     * @param beginIndex The first index, inclusive
     * @param endIndex   The last index, exclusive
     * @return the elements in {@code [beginIndex, endIndex)}, both clamped to {@code [0, size()]}
     */
    public Vector<A> slice(int beginIndex, int endIndex) { return vector.slice(beginIndex, endIndex); }

    /**
     * Complexity: O(min(i, n - i)), as {@link Vector#removeAt(int)}.
     *
     * @param index A position
     * @return this vector without the element at {@code index}
     * @throws IndexOutOfBoundsException if {@code index} is not in {@code [0, size())}
     */
    public Vector<A> removeAt(int index) { return vector.removeAt(index); }

    /**
     * Complexity: O(n), as {@link Vector#remove(Object)}.
     *
     * @param element An element
     * @return this vector without the first occurrence of {@code element}
     */
    public Vector<A> remove(A element) { return vector.remove(element); }

    /**
     * Complexity: O(n), as {@link Vector#removeAll(Object)}.
     *
     * @param element An element
     * @return this vector without any occurrence of {@code element}
     */
    public Vector<A> removeAll(A element) { return vector.removeAll(element); }

    /**
     * Complexity: O(n + m) for an argument of m elements, as {@link Vector#removeAll(Iterable)}.
     *
     * @param elements Elements
     * @return this vector without any occurrence of any of {@code elements}
     * @throws NullPointerException if {@code elements} is null
     */
    public Vector<A> removeAll(Iterable<? extends A> elements) { return vector.removeAll(elements); }

    /**
     * Complexity: O(n), as {@link Vector#removeAll(Predicate)}.
     *
     * @param predicate A test
     * @return this vector without the elements that pass {@code predicate}
     * @throws NullPointerException if {@code predicate} is null
     */
    public Vector<A> removeAll(Predicate<? super A> predicate) { return vector.removeAll(predicate); }

    /**
     * Complexity: O(n), as {@link Vector#removeFirst(Predicate)}.
     *
     * @param predicate A test
     * @return this vector without the first element that passes {@code predicate}; all of them if none does
     * @throws NullPointerException if {@code predicate} is null
     */
    @SuppressWarnings("unchecked")
    public Vector<A> removeFirst(Predicate<? super A> predicate) { return vector.removeFirst((Predicate<A>) predicate); }

    /**
     * Complexity: O(n), as {@link Vector#removeLast(Predicate)}.
     *
     * @param predicate A test
     * @return this vector without the last element that passes {@code predicate}; all of them if none does
     * @throws NullPointerException if {@code predicate} is null
     */
    @SuppressWarnings("unchecked")
    public Vector<A> removeLast(Predicate<? super A> predicate) { return vector.removeLast((Predicate<A>) predicate); }

    /**
     * Complexity: O(n + m) for an argument of m elements, as {@link Vector#retainAll(Iterable)}.
     *
     * @param elements The elements to keep
     * @return the elements of this vector present in {@code elements}, in this vector's order
     * @throws NullPointerException if {@code elements} is null
     */
    public Vector<A> retainAll(Iterable<? extends A> elements) { return vector.retainAll(elements); }

    /**
     * {@link Vector#patch(int, Iterable, int)} on the elements of this vector: replaces the {@code replaced} elements
     * from {@code from} on by the elements of {@code that}, read once, with {@code from} and {@code replaced} handled
     * exactly as that method handles them.
     * <p>
     * Complexity: O(n + m) for m elements of {@code that}, as {@link Vector#patch(int, Iterable, int)}: a patch
     * near the end is O(m).
     *
     * @param from     The first position to replace
     * @param that     The replacement elements
     * @param replaced How many elements to replace
     * @return the patched elements; empty when every element is replaced by none
     * @throws NullPointerException if {@code that} or one of its elements is null
     */
    public Vector<A> patch(int from, Iterable<? extends A> that, int replaced) {
        Objects.requireNonNull(that, "that is null");
        return vector.patch(from, addAll(Vector.newBuilder(), that).result(), replaced);
    }

    /**
     * Unlike {@link #drop(int)}, an out-of-range index throws.
     * <p>
     * Complexity: effectively O(1), as {@link Vector#subSequence(int)}.
     *
     * @param beginIndex The first position, {@code 0 <= beginIndex <= size()}
     * @return the elements from {@code beginIndex} on; empty when it is {@code size()}
     * @throws IndexOutOfBoundsException if {@code beginIndex} is not in {@code [0, size()]}
     */
    public Vector<A> subSequence(int beginIndex) { return vector.subSequence(beginIndex); }

    /**
     * Unlike {@link #slice(int, int)}, out-of-range or reversed indices throw.
     * <p>
     * Complexity: effectively O(1), as {@link Vector#subSequence(int, int)}.
     *
     * @param beginIndex The first position, inclusive, {@code >= 0}
     * @param endIndex   The last position, exclusive, {@code <= size()}
     * @return the elements in {@code [beginIndex, endIndex)}; empty when both are equal
     * @throws IndexOutOfBoundsException if {@code beginIndex < 0} or {@code endIndex > size()}
     * @throws IllegalArgumentException  if {@code beginIndex > endIndex}
     */
    public Vector<A> subSequence(int beginIndex, int endIndex) { return vector.subSequence(beginIndex, endIndex); }

    /**
     * Complexity: effectively O(1), as {@link Vector#splitAt(int)}.
     *
     * @param n The split position, clamped to {@code [0, size()]}
     * @return {@code (take(n), drop(n))}
     */
    public Tuple2<Vector<A>, Vector<A>> splitAt(int n) { return vector.splitAt(n); }

    /**
     * Splits before the first element that passes {@code predicate}; that element starts the second part.
     * <p>
     * Complexity: O(k) for k elements before the split, then an effectively O(1) split, as
     * {@link Vector#splitAt(Predicate)}.
     *
     * @param predicate A test
     * @return the elements before the first match and the rest; all of them and an empty vector if none matches
     * @throws NullPointerException if {@code predicate} is null
     */
    public Tuple2<Vector<A>, Vector<A>> splitAt(Predicate<? super A> predicate) { return vector.splitAt(predicate); }

    /**
     * Splits after the first element that passes {@code predicate}; that element ends the first part, which therefore
     * always has at least one element.
     * <p>
     * Complexity: O(k) for k elements up to the split, then an effectively O(1) split, as
     * {@link Vector#splitAtInclusive(Predicate)}.
     *
     * @param predicate A test
     * @return the elements up to and including the first match, and the rest; all of them and an empty vector if none
     *         matches
     * @throws NullPointerException if {@code predicate} is null
     */
    public Tuple2<NonEmptyVector<A>, Vector<A>> splitAtInclusive(Predicate<? super A> predicate) {
        final Tuple2<Vector<A>, Vector<A>> split = vector.splitAtInclusive(predicate);
        return Tuple.of(new NonEmptyVector<>(split._1()), split._2());
    }

    /**
     * Complexity: O(k) for k elements before the split, then an effectively O(1) split, as
     * {@link Vector#span(Predicate)}.
     *
     * @param predicate A test
     * @return the longest prefix whose elements pass {@code predicate}, and the rest
     * @throws NullPointerException if {@code predicate} is null
     */
    public Tuple2<Vector<A>, Vector<A>> span(Predicate<? super A> predicate) { return vector.span(predicate); }

    /**
     * Complexity: O(n), one pass.
     *
     * @param predicate A test
     * @return the elements that pass {@code predicate} and those that fail it, each in order
     * @throws NullPointerException if {@code predicate} is null
     */
    public Tuple2<Vector<A>, Vector<A>> partition(Predicate<? super A> predicate) { return vector.partition(predicate); }

    /**
     * Pairs the elements by position, up to the shorter size; empty when {@code that} is. For a non-empty argument,
     * see {@link #zip(NonEmptyVector)}.
     * <p>
     * Complexity: O(min(n, m)) for m elements of {@code that}, as {@link Vector#zip(Iterable)}.
     *
     * @param that The right-hand elements, read once
     * @param <B>  Component type of {@code that}
     * @return the pairs
     * @throws NullPointerException if {@code that} or one of its elements is null
     */
    public <B extends @Nullable Object> Vector<Tuple2<A, B>> zip(Iterable<? extends B> that) {
        Objects.requireNonNull(that, "that is null");
        return vector.zip(addAll(Vector.<B> newBuilder(), that).result());
    }

    /**
     * Combines the elements by position, up to the shorter size; empty when {@code that} is. For a non-empty
     * argument, see {@link #zipWith(NonEmptyVector, BiFunction)}.
     * <p>
     * Complexity: O(min(n, m)) for m elements of {@code that}, as {@link Vector#zipWith(Iterable, BiFunction)}.
     *
     * @param that   The right-hand elements, read once
     * @param mapper Combines two elements
     * @param <B>    Component type of {@code that}
     * @param <R>    Component type of the result
     * @return the combined elements
     * @throws NullPointerException if {@code that}, one of its elements or {@code mapper} is null, or {@code mapper}
     *                              returns null
     */
    public <B extends @Nullable Object, R extends @Nullable Object> Vector<R> zipWith(Iterable<? extends B> that, BiFunction<? super A, ? super B, ? extends R> mapper) {
        Objects.requireNonNull(that, "that is null");
        Objects.requireNonNull(mapper, "mapper is null");
        return vector.zipWith(addAll(Vector.<B> newBuilder(), that).result(), (a, b) -> Objects.requireNonNull(mapper.apply(a, b), "NonEmptyVector.zipWith: mapper returned null"));
    }

    /**
     * The Cartesian product with any iterable: every pair {@code (a, b)}, {@code a} varying slowest; empty when
     * {@code that} is. For a non-empty argument, see {@link #crossProduct(NonEmptyVector)}.
     * <p>
     * Complexity: O(n * m) for m elements of {@code that}, as {@link Vector#crossProduct(Iterable)}.
     *
     * @param that The right-hand elements, read once
     * @param <B>  Component type of {@code that}
     * @return the pairs
     * @throws NullPointerException if {@code that} or one of its elements is null
     */
    public <B extends @Nullable Object> Vector<Tuple2<A, B>> crossProduct(Iterable<? extends B> that) {
        Objects.requireNonNull(that, "that is null");
        return vector.crossProduct(addAll(Vector.<B> newBuilder(), that).result());
    }

    /**
     * The Cartesian power: every vector of {@code power} elements drawn from this one, in lexicographic position
     * order. {@code power == 0} gives one empty vector; a negative power gives none.
     * <p>
     * Complexity: O(power * n^power), as {@link Vector#crossProduct(int)}.
     *
     * @param power The size of each result
     * @return the vectors
     */
    public Vector<Vector<A>> crossProduct(int power) { return vector.crossProduct(power); }

    /**
     * All combinations of {@code k} elements, selected by position (equal elements are distinct positions).
     * <p>
     * Complexity: O(k * C(n, k) + C(n, 0) + ... + C(n, k)), as {@link Vector#combinations(int)}: as k approaches n,
     * the work approaches O(2^n) while the result shrinks.
     *
     * @param k The size of each combination; {@code k <= 0} gives one empty combination
     * @return the k-combinations, in position order; none when {@code k > size()}
     */
    public Vector<Vector<A>> combinations(int k) { return vector.combinations(k); }

    // -- total: what is partial on a Vector

    /**
     * Complexity: effectively O(1), as {@link Vector#head()}.
     *
     * @return the first element
     */
    public A head() { return vector.get(0); }

    /**
     * Complexity: effectively O(1), as {@link Vector#last()}.
     *
     * @return the last element
     */
    public A last() { return vector.get(vector.size() - 1); }

    /**
     * Complexity: O(n), every element compared once.
     *
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
     * Complexity: O(n), every element compared once.
     *
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
     * Complexity: O(n), as {@link Vector#reduceLeft(BiFunction)}.
     *
     * @param op Combines two elements
     * @return the elements combined from the left: {@code op(op(a0, a1), a2)...}
     * @throws NullPointerException if {@code op} is null
     */
    public A reduceLeft(BiFunction<? super A, ? super A, ? extends A> op) { return vector.reduceLeft(op); }

    /**
     * Complexity: O(n), as {@link Vector#reduceRight(BiFunction)}.
     *
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
     * Complexity: O(1), as {@link Vector#size()}.
     *
     * @return the number of elements, at least 1
     */
    public int size() { return vector.size(); }

    /**
     * Complexity: effectively O(1), as {@link Vector#get(int)}.
     *
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
     * Complexity: O(n), as {@link Vector#foldRight(Object, BiFunction)}.
     *
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
     * Complexity: O(n).
     *
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
     * Complexity: O(n).
     *
     * @param element An element
     * @return the index of the first element equal to {@code element}, or -1. See {@link #indexOfOption(Object)}
     */
    public int indexOf(A element) { return vector.indexOf(element); }

    /**
     * Complexity: O(n), as {@link Vector#indexOf(Object, int)}.
     *
     * @param element An element
     * @param from    The first position searched; a negative one counts as 0
     * @return the index of the first element at or after {@code from} equal to {@code element}, or -1
     */
    public int indexOf(A element, int from) { return vector.indexOf(element, from); }

    /**
     * Complexity: O(n), as {@link Vector#indexWhere(Predicate)}.
     *
     * @param predicate A test
     * @return the index of the first element that passes {@code predicate}, or -1
     * @throws NullPointerException if {@code predicate} is null
     */
    public int indexWhere(Predicate<? super A> predicate) { return vector.indexWhere(predicate); }

    /**
     * Complexity: O(n), as {@link Vector#indexWhere(Predicate, int)}.
     *
     * @param predicate A test
     * @param from      The first position searched; a negative one counts as 0
     * @return the index of the first element at or after {@code from} that passes {@code predicate}, or -1
     * @throws NullPointerException if {@code predicate} is null
     */
    public int indexWhere(Predicate<? super A> predicate, int from) { return vector.indexWhere(predicate, from); }

    /**
     * Complexity: O(n), as {@link Vector#lastIndexOf(Object)}.
     *
     * @param element An element
     * @return the index of the last element equal to {@code element}, or -1
     */
    public int lastIndexOf(A element) { return vector.lastIndexOf(element); }

    /**
     * Complexity: O(n), as {@link Vector#lastIndexOf(Object, int)}.
     *
     * @param element An element
     * @param end     The last position searched
     * @return the index of the last element at or before {@code end} equal to {@code element}, or -1
     */
    public int lastIndexOf(A element, int end) { return vector.lastIndexOf(element, end); }

    /**
     * Complexity: O(n), as {@link Vector#lastIndexWhere(Predicate)}.
     *
     * @param predicate A test
     * @return the index of the last element that passes {@code predicate}, or -1
     * @throws NullPointerException if {@code predicate} is null
     */
    public int lastIndexWhere(Predicate<? super A> predicate) { return vector.lastIndexWhere(predicate); }

    /**
     * Complexity: O(n), as {@link Vector#lastIndexWhere(Predicate, int)}.
     *
     * @param predicate A test
     * @param end       The last position searched
     * @return the index of the last element at or before {@code end} that passes {@code predicate}, or -1
     * @throws NullPointerException if {@code predicate} is null
     */
    public int lastIndexWhere(Predicate<? super A> predicate, int end) { return vector.lastIndexWhere(predicate, end); }

    /**
     * Complexity: O(n * m) for a slice of m elements, as {@link Vector#indexOfSlice(Iterable)}.
     *
     * @param that A slice
     * @return the index of the first occurrence of {@code that} as a contiguous slice, or -1; 0 for an empty slice
     * @throws NullPointerException if {@code that} is null
     */
    public int indexOfSlice(Iterable<? extends A> that) { return vector.indexOfSlice(that); }

    /**
     * Complexity: O(n * m) for a slice of m elements, as {@link Vector#indexOfSlice(Iterable, int)}.
     *
     * @param that A slice
     * @param from The first position searched
     * @return the index of the first occurrence of {@code that} at or after {@code from}, or -1
     * @throws NullPointerException if {@code that} is null
     */
    public int indexOfSlice(Iterable<? extends A> that, int from) { return vector.indexOfSlice(that, from); }

    /**
     * Complexity: O(n * m) for a slice of m elements, as {@link Vector#lastIndexOfSlice(Iterable)}.
     *
     * @param that A slice
     * @return the index of the last occurrence of {@code that} as a contiguous slice, or -1
     * @throws NullPointerException if {@code that} is null
     */
    public int lastIndexOfSlice(Iterable<? extends A> that) { return vector.lastIndexOfSlice(that); }

    /**
     * Complexity: O(n * m) for a slice of m elements, as {@link Vector#lastIndexOfSlice(Iterable, int)}.
     *
     * @param that A slice
     * @param end  The last position an occurrence may start at
     * @return the index of the last occurrence of {@code that} starting at or before {@code end}, or -1
     * @throws NullPointerException if {@code that} is null
     */
    public int lastIndexOfSlice(Iterable<? extends A> that, int end) { return vector.lastIndexOfSlice(that, end); }

    /**
     * Complexity: O(m) for m elements of {@code that}, as {@link Vector#startsWith(Iterable)}.
     *
     * @param that A prefix
     * @return whether the first elements equal {@code that}; true for an empty {@code that}
     * @throws NullPointerException if {@code that} is null
     */
    public boolean startsWith(Iterable<? extends A> that) { return vector.startsWith(that); }

    /**
     * Complexity: O(m) for m elements of {@code that}, as {@link Vector#startsWith(Iterable, int)}.
     *
     * @param that   A prefix
     * @param offset The position the prefix starts at
     * @return false if {@code offset} is negative; otherwise whether the elements from {@code offset} on start with
     *         {@code that}
     * @throws NullPointerException if {@code that} is null
     */
    public boolean startsWith(Iterable<? extends A> that, int offset) { return vector.startsWith(that, offset); }

    /**
     * Complexity: O(m) for m elements of {@code that}, as {@link Vector#endsWith(Iterable)}.
     *
     * @param that A suffix
     * @return whether the last elements equal {@code that}; true for an empty {@code that}
     * @throws NullPointerException if {@code that} is null
     */
    public boolean endsWith(Iterable<? extends A> that) { return vector.endsWith(that); }

    /**
     * Complexity: O(n * m) for a slice of m elements, as {@link Vector#containsSlice(Iterable)}.
     *
     * @param that A slice
     * @return whether {@code that} occurs as a contiguous slice; true for an empty {@code that}
     * @throws NullPointerException if {@code that} is null
     */
    public boolean containsSlice(Iterable<? extends A> that) { return vector.containsSlice(that); }

    /**
     * Complexity: O(n * m) for an argument of m elements: one {@link #contains(Object)} per element, each O(n).
     *
     * @param elements Elements
     * @return whether every one of {@code elements} is present
     * @throws NullPointerException if {@code elements} is null
     */
    public boolean containsAll(Iterable<? extends A> elements) { return vector.containsAll(elements); }

    /**
     * Binary search in elements sorted in their natural order (otherwise the result is undefined).
     * <p>
     * Complexity: O(log n) comparisons, each an effectively O(1) access, as {@link Vector#search(Object)}.
     *
     * @param element The element to find
     * @return its index if present, otherwise {@code -(insertion point) - 1}
     * @throws ClassCastException if the elements are not {@link Comparable}
     */
    public int search(A element) { return vector.search(element); }

    /**
     * Binary search in elements sorted by {@code comparator} (otherwise the result is undefined).
     * <p>
     * Complexity: O(log n) comparisons, each an effectively O(1) access, as
     * {@link Vector#search(Object, Comparator)}.
     *
     * @param element    The element to find
     * @param comparator The order of the elements
     * @return its index if present, otherwise {@code -(insertion point) - 1}
     * @throws NullPointerException if {@code comparator} is null
     */
    public int search(A element, Comparator<? super A> comparator) { return vector.search(element, comparator); }

    /**
     * Complexity: O(k) for a run of k elements, as {@link Vector#segmentLength(Predicate, int)}.
     *
     * @param predicate A test
     * @param from      The first position of the run; a negative one counts as 0
     * @return the length of the longest run of elements from {@code from} on that pass {@code predicate}
     * @throws NullPointerException if {@code predicate} is null
     */
    public int segmentLength(Predicate<? super A> predicate, int from) { return vector.segmentLength(predicate, from); }

    /**
     * Complexity: O(k) for a prefix of k elements, as {@link Vector#prefixLength(Predicate)}.
     *
     * @param predicate A test
     * @return the length of the longest prefix whose elements pass {@code predicate}
     * @throws NullPointerException if {@code predicate} is null
     */
    public int prefixLength(Predicate<? super A> predicate) { return vector.prefixLength(predicate); }

    /**
     * Complexity: O(n).
     *
     * @param predicate A test
     * @return whether exactly one element passes {@code predicate}
     * @throws NullPointerException if {@code predicate} is null
     */
    public boolean existsUnique(Predicate<? super A> predicate) { return vector.existsUnique(predicate); }

    /**
     * Complexity: O(n), every element compared once.
     *
     * @return the greatest element in the natural order of the elements; the first one, if several are equal
     * @throws ClassCastException if the elements are not {@link Comparable}
     */
    public A max() { return max(Comparators.naturalComparator()); }

    /**
     * Complexity: O(n), every element compared once.
     *
     * @return the least element in the natural order of the elements; the first one, if several are equal. Among
     *         {@code Double}s or {@code Float}s, a {@code NaN} is the result whenever one is present, as on
     *         {@link Vector#min()}
     * @throws ClassCastException if the elements are not {@link Comparable}
     */
    @SuppressWarnings("unchecked")
    public A min() {
        final java.util.Iterator<A> iterator = vector.iterator();
        final A head = iterator.next();
        if (head instanceof Double first) {
            double min = first;
            while (iterator.hasNext()) {
                min = Math.min(min, (Double) iterator.next());
            }
            return (A) (Double) min;
        } else if (head instanceof Float first) {
            float min = first;
            while (iterator.hasNext()) {
                min = Math.min(min, (Float) iterator.next());
            }
            return (A) (Float) min;
        } else {
            return min(Comparators.naturalComparator());
        }
    }

    /**
     * Complexity: O(n), every element compared once, as {@link #max(Comparator)}.
     *
     * @param comparator The order
     * @return the greatest element; the first one, if several are equal under {@code comparator}
     * @throws NullPointerException if {@code comparator} is null
     */
    public A maxBy(Comparator<? super A> comparator) { return max(comparator); }

    /**
     * Complexity: O(n), every element compared once, as {@link #min(Comparator)}.
     *
     * @param comparator The order
     * @return the least element; the first one, if several are equal under {@code comparator}
     * @throws NullPointerException if {@code comparator} is null
     */
    public A minBy(Comparator<? super A> comparator) { return min(comparator); }

    /**
     * Folds the elements from the left with {@code combine}, starting from {@code zero}, which must be its neutral
     * element.
     * <p>
     * Complexity: O(n), as {@link Vector#fold(Object, BiFunction)}.
     *
     * @param zero    The neutral element of {@code combine}
     * @param combine Combines two elements
     * @return {@code combine(combine(combine(zero, a0), a1), a2)...}
     * @throws NullPointerException if {@code combine} is null
     */
    public A fold(A zero, BiFunction<? super A, ? super A, ? extends A> combine) { return vector.fold(zero, combine); }

    /**
     * The sum of the elements, which must be {@link Number}s, with the arithmetic of {@link Vector#sum()}.
     * <p>
     * Complexity: O(n), as {@link Vector#sum()}.
     *
     * @return the sum
     * @throws UnsupportedOperationException if an element is not a {@code Number}
     */
    public Number sum() { return vector.sum(); }

    /**
     * The product of the elements, which must be {@link Number}s, with the arithmetic of {@link Vector#product()}.
     * <p>
     * Complexity: O(n), as {@link Vector#product()}.
     *
     * @return the product
     * @throws UnsupportedOperationException if an element is not a {@code Number}
     */
    public Number product() { return vector.product(); }

    /**
     * The average of the elements, which must be {@link Number}s, summed as {@code double}s with Neumaier
     * compensation: the value {@link Vector#average()} holds.
     * <p>
     * Complexity: O(n), one compensated pass.
     *
     * @return the average
     * @throws UnsupportedOperationException if an element is not a {@code Number}
     */
    public double average() {
        try {
            final double[] sum = TraversableModule.neumaierSum(vector, element -> ((Number) element).doubleValue());
            return sum[0] / sum[1];
        } catch (ClassCastException x) {
            throw new UnsupportedOperationException("Elements are not numeric", x);
        }
    }

    /**
     * The only element.
     * <p>
     * Complexity: O(1).
     *
     * @return the element
     * @throws java.util.NoSuchElementException if there is more than one element
     */
    public A single() { return vector.single(); }

    /**
     * Arranges the elements by a key that must be unique.
     * <p>
     * Complexity: O(n), as {@link Vector#arrangeBy(Function)}.
     *
     * @param getKey The key of an element
     * @param <K>    Key type
     * @return {@code Some} of the map from each key to its element, or {@code None} if two elements share a key
     * @throws NullPointerException if {@code getKey} is null or returns null
     */
    public <K extends @Nullable Object> Option<Map<K, A>> arrangeBy(Function<? super A, ? extends K> getKey) {
        Objects.requireNonNull(getKey, "getKey is null");
        return vector.arrangeBy(element -> Objects.requireNonNull(getKey.apply(element), "NonEmptyVector.arrangeBy: getKey returned null"));
    }

    /**
     * Runs {@code action} on each element with its position, from 0, without boxing the index.
     * <p>
     * Complexity: O(n).
     *
     * @param action A side effect
     * @throws NullPointerException if {@code action} is null
     */
    public void forEachWithIndex(ObjIntConsumer<? super A> action) { vector.forEachWithIndex(action); }

    /**
     * Complexity: O(n), as {@link Vector#collect(Collector)}.
     *
     * @param collector A collector
     * @param <R>       Result type
     * @param <C>       The collector's accumulation type
     * @return the elements collected, as {@code stream().collect(collector)} does
     * @throws NullPointerException if {@code collector} is null
     */
    public <R extends @Nullable Object, C extends @Nullable Object> R collect(Collector<? super A, C, R> collector) {
        return vector.collect(collector);
    }

    /**
     * Complexity: O(n), as {@link Vector#collect(Supplier, BiConsumer, BiConsumer)}.
     *
     * @param supplier    Makes a new result container
     * @param accumulator Adds an element to a container
     * @param combiner    Merges two containers
     * @param <R>         Result type
     * @return the elements collected, as {@code stream().collect(supplier, accumulator, combiner)} does
     * @throws NullPointerException if an argument is null
     */
    public <R extends @Nullable Object> R collect(Supplier<R> supplier, BiConsumer<R, ? super A> accumulator, BiConsumer<R, R> combiner) {
        return vector.collect(supplier, accumulator, combiner);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Complexity: O(1) to create, and O(1) per step, as {@link Vector#iterator()}.
     */
    @Override
    public java.util.Iterator<A> iterator() { return vector.iterator(); }

    @Override
    public Spliterator<A> spliterator() {
        return Spliterators.spliterator(iterator(), vector.size(), Spliterator.ORDERED | Spliterator.IMMUTABLE | Spliterator.NONNULL);
    }

    /**
     * @return a sequential {@link java.util.stream.Stream} over the elements
     */
    public java.util.stream.Stream<A> stream() { return vector.stream(); }

    /**
     * An unmodifiable {@link java.util.List} view of the elements, the one {@link Vector#asJava()} gives: nothing is
     * copied and every mutator of the view throws {@link UnsupportedOperationException}. A mutable copy is
     * {@code new java.util.ArrayList<>(nonEmpty.asJava())}; {@code Vector.ofAll} given the view returns
     * {@link #toVector()} without copying.
     * <p>
     * Complexity: O(1): nothing is copied. {@code get} on the view is effectively O(1), as {@link #get(int)}.
     *
     * @return an unmodifiable {@code java.util.List} view
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

    /**
     * Complexity: O(n).
     *
     * @return the elements as a {@link Queue}, in order
     */
    public Queue<A> toQueue() { return vector.toQueue(); }

    /**
     * Complexity: O(n).
     *
     * @return the elements as a {@link Stream}, in order
     */
    public Stream<A> toStream() { return vector.toStream(); }

    /**
     * Complexity: O(n).
     *
     * @return the distinct elements as a {@link LinkedHashSet}, in order
     */
    public Set<A> toLinkedSet() { return vector.toLinkedSet(); }

    /**
     * Complexity: O(n log n).
     *
     * @return the distinct elements as a {@link TreeSet} in their natural order
     * @throws ClassCastException if the elements are not {@link Comparable}
     */
    public SortedSet<A> toSortedSet() { return vector.toSortedSet(); }

    /**
     * Complexity: O(n log n).
     *
     * @param comparator The order
     * @return the distinct elements as a {@link TreeSet} ordered by {@code comparator}
     * @throws NullPointerException if {@code comparator} is null
     */
    public SortedSet<A> toSortedSet(Comparator<? super A> comparator) { return vector.toSortedSet(comparator); }

    /**
     * Complexity: O(n).
     *
     * @return a new array of the elements, in order
     */
    public Object[] toArray() { return vector.toArray(); }

    /**
     * Complexity: O(n).
     *
     * @param arrayFactory Makes an array of the given length
     * @return a new array of the elements, in order, of the type {@code arrayFactory} makes
     * @throws NullPointerException if {@code arrayFactory} is null
     */
    public A[] toArray(IntFunction<A[]> arrayFactory) { return vector.toArray(arrayFactory); }

    /**
     * The elements as the entries of a new {@link HashMap}; of two entries with the same key, the later one wins.
     * <p>
     * Complexity: O(n), one entry built per element.
     *
     * @param keyMapper   The key of an element
     * @param valueMapper The value of an element
     * @param <K>         Key type
     * @param <V>         Value type
     * @return the map
     * @throws NullPointerException if an argument is null or returns null
     */
    public <K extends @Nullable Object, V extends @Nullable Object> Map<K, V> toMap(Function<? super A, ? extends K> keyMapper, Function<? super A, ? extends V> valueMapper) {
        return vector.toMap(entryMapper(keyMapper, valueMapper, "NonEmptyVector.toMap"));
    }

    /**
     * The elements as the entries of a new {@link HashMap}; of two entries with the same key, the later one wins.
     * <p>
     * Complexity: O(n), one entry built per element.
     *
     * @param f   The entry an element becomes
     * @param <K> Key type
     * @param <V> Value type
     * @return the map
     * @throws NullPointerException if {@code f} is null, returns null, or returns an entry with a null key or value
     */
    public <K extends @Nullable Object, V extends @Nullable Object> Map<K, V> toMap(Function<? super A, ? extends Tuple2<? extends K, ? extends V>> f) {
        return vector.toMap(checkedEntries(f, "NonEmptyVector.toMap"));
    }

    /**
     * The elements as the entries of a new {@link LinkedHashMap}, in order; of two entries with the same key, the
     * later one wins the value and the earlier one the position.
     * <p>
     * Complexity: O(n), one entry built per element.
     *
     * @param keyMapper   The key of an element
     * @param valueMapper The value of an element
     * @param <K>         Key type
     * @param <V>         Value type
     * @return the map
     * @throws NullPointerException if an argument is null or returns null
     */
    public <K extends @Nullable Object, V extends @Nullable Object> Map<K, V> toLinkedMap(Function<? super A, ? extends K> keyMapper, Function<? super A, ? extends V> valueMapper) {
        return vector.toLinkedMap(entryMapper(keyMapper, valueMapper, "NonEmptyVector.toLinkedMap"));
    }

    /**
     * The elements as the entries of a new {@link LinkedHashMap}, in order; of two entries with the same key, the
     * later one wins the value and the earlier one the position.
     * <p>
     * Complexity: O(n), one entry built per element.
     *
     * @param f   The entry an element becomes
     * @param <K> Key type
     * @param <V> Value type
     * @return the map
     * @throws NullPointerException if {@code f} is null, returns null, or returns an entry with a null key or value
     */
    public <K extends @Nullable Object, V extends @Nullable Object> Map<K, V> toLinkedMap(Function<? super A, ? extends Tuple2<? extends K, ? extends V>> f) {
        return vector.toLinkedMap(checkedEntries(f, "NonEmptyVector.toLinkedMap"));
    }

    /**
     * The elements as the entries of a new {@link TreeMap} in the natural order of the keys; of two entries with the
     * same key, the later one wins.
     * <p>
     * Complexity: O(n log n), one entry built per element.
     *
     * @param keyMapper   The key of an element
     * @param valueMapper The value of an element
     * @param <K>         Key type
     * @param <V>         Value type
     * @return the map
     * @throws NullPointerException if an argument is null or returns null
     */
    public <K extends Comparable<? super K>, V extends @Nullable Object> SortedMap<K, V> toSortedMap(Function<? super A, ? extends K> keyMapper, Function<? super A, ? extends V> valueMapper) {
        return vector.toSortedMap(entryMapper(keyMapper, valueMapper, "NonEmptyVector.toSortedMap"));
    }

    /**
     * The elements as the entries of a new {@link TreeMap} in the natural order of the keys; of two entries with the
     * same key, the later one wins.
     * <p>
     * Complexity: O(n log n), one entry built per element.
     *
     * @param f   The entry an element becomes
     * @param <K> Key type
     * @param <V> Value type
     * @return the map
     * @throws NullPointerException if {@code f} is null, returns null, or returns an entry with a null key or value
     */
    public <K extends Comparable<? super K>, V extends @Nullable Object> SortedMap<K, V> toSortedMap(Function<? super A, ? extends Tuple2<? extends K, ? extends V>> f) {
        return vector.toSortedMap(checkedEntries(f, "NonEmptyVector.toSortedMap"));
    }

    /**
     * The elements as the entries of a new {@link TreeMap} ordered by {@code comparator}; of two entries with the
     * same key, the later one wins.
     * <p>
     * Complexity: O(n log n), one entry built per element.
     *
     * @param comparator  The order of the keys
     * @param keyMapper   The key of an element
     * @param valueMapper The value of an element
     * @param <K>         Key type
     * @param <V>         Value type
     * @return the map
     * @throws NullPointerException if an argument is null or a mapper returns null
     */
    public <K extends @Nullable Object, V extends @Nullable Object> SortedMap<K, V> toSortedMap(Comparator<? super K> comparator, Function<? super A, ? extends K> keyMapper, Function<? super A, ? extends V> valueMapper) {
        Objects.requireNonNull(comparator, "comparator is null");
        return vector.toSortedMap(comparator, entryMapper(keyMapper, valueMapper, "NonEmptyVector.toSortedMap"));
    }

    /**
     * The elements as the entries of a new {@link TreeMap} ordered by {@code comparator}; of two entries with the
     * same key, the later one wins.
     * <p>
     * Complexity: O(n log n), one entry built per element.
     *
     * @param comparator The order of the keys
     * @param f          The entry an element becomes
     * @param <K>        Key type
     * @param <V>        Value type
     * @return the map
     * @throws NullPointerException if an argument is null, or {@code f} returns null or an entry with a null key or
     *                              value
     */
    public <K extends @Nullable Object, V extends @Nullable Object> SortedMap<K, V> toSortedMap(Comparator<? super K> comparator, Function<? super A, ? extends Tuple2<? extends K, ? extends V>> f) {
        Objects.requireNonNull(comparator, "comparator is null");
        return vector.toSortedMap(comparator, checkedEntries(f, "NonEmptyVector.toSortedMap"));
    }

    /* the entry of an element, its key and value checked, reported under the calling method's name */
    private static <A extends @Nullable Object, K extends @Nullable Object, V extends @Nullable Object> Function<A, Tuple2<K, V>> entryMapper(
            Function<? super A, ? extends K> keyMapper, Function<? super A, ? extends V> valueMapper, String method) {
        Objects.requireNonNull(keyMapper, "keyMapper is null");
        Objects.requireNonNull(valueMapper, "valueMapper is null");
        return element -> {
            final K key = keyMapper.apply(element);
            if (key == null) {
                throw new NullPointerException(method + ": keyMapper returned null");
            }
            final V value = valueMapper.apply(element);
            if (value == null) {
                throw new NullPointerException(method + ": valueMapper returned null");
            }
            return Tuple.of(key, value);
        };
    }

    /* f, with its entry, key and value checked, reported under the calling method's name */
    private static <A extends @Nullable Object, E extends @Nullable Tuple2<?, ?>> Function<A, E> checkedEntries(Function<? super A, ? extends E> f, String method) {
        Objects.requireNonNull(f, "f is null");
        return element -> {
            final E entry = f.apply(element);
            if (entry == null) {
                throw new NullPointerException(method + ": f returned null");
            }
            if (entry._1() == null) {
                throw new NullPointerException(method + ": f returned an entry with a null key");
            }
            if (entry._2() == null) {
                throw new NullPointerException(method + ": f returned an entry with a null value");
            }
            return entry;
        };
    }

    // -- returns Option

    /**
     * @param predicate A test
     * @return the first element that passes {@code predicate}
     * @throws NullPointerException if {@code predicate} is null
     */
    public Option<A> find(Predicate<? super A> predicate) { return vector.find(predicate); }

    /**
     * Complexity: O(n), as {@link Vector#findLast(Predicate)}.
     *
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
     * Complexity: O(n), as {@link Vector#indexOfOption(Object, int)}.
     *
     * @param element An element
     * @param from    The first position searched; a negative one counts as 0
     * @return the index of the first element at or after {@code from} equal to {@code element}
     */
    public Option<Integer> indexOfOption(A element, int from) { return vector.indexOfOption(element, from); }

    /**
     * Complexity: O(n), as {@link Vector#indexWhereOption(Predicate)}.
     *
     * @param predicate A test
     * @return the index of the first element that passes {@code predicate}
     * @throws NullPointerException if {@code predicate} is null
     */
    public Option<Integer> indexWhereOption(Predicate<? super A> predicate) { return vector.indexWhereOption(predicate); }

    /**
     * Complexity: O(n), as {@link Vector#indexWhereOption(Predicate, int)}.
     *
     * @param predicate A test
     * @param from      The first position searched; a negative one counts as 0
     * @return the index of the first element at or after {@code from} that passes {@code predicate}
     * @throws NullPointerException if {@code predicate} is null
     */
    public Option<Integer> indexWhereOption(Predicate<? super A> predicate, int from) { return vector.indexWhereOption(predicate, from); }

    /**
     * Complexity: O(n), as {@link Vector#lastIndexOfOption(Object)}.
     *
     * @param element An element
     * @return the index of the last element equal to {@code element}
     */
    public Option<Integer> lastIndexOfOption(A element) { return vector.lastIndexOfOption(element); }

    /**
     * Complexity: O(n), as {@link Vector#lastIndexOfOption(Object, int)}.
     *
     * @param element An element
     * @param end     The last position searched
     * @return the index of the last element at or before {@code end} equal to {@code element}
     */
    public Option<Integer> lastIndexOfOption(A element, int end) { return vector.lastIndexOfOption(element, end); }

    /**
     * Complexity: O(n), as {@link Vector#lastIndexWhereOption(Predicate)}.
     *
     * @param predicate A test
     * @return the index of the last element that passes {@code predicate}
     * @throws NullPointerException if {@code predicate} is null
     */
    public Option<Integer> lastIndexWhereOption(Predicate<? super A> predicate) { return vector.lastIndexWhereOption(predicate); }

    /**
     * Complexity: O(n), as {@link Vector#lastIndexWhereOption(Predicate, int)}.
     *
     * @param predicate A test
     * @param end       The last position searched
     * @return the index of the last element at or before {@code end} that passes {@code predicate}
     * @throws NullPointerException if {@code predicate} is null
     */
    public Option<Integer> lastIndexWhereOption(Predicate<? super A> predicate, int end) { return vector.lastIndexWhereOption(predicate, end); }

    /**
     * Complexity: O(n * m) for a slice of m elements, as {@link Vector#indexOfSliceOption(Iterable)}.
     *
     * @param that A slice
     * @return the index of the first occurrence of {@code that} as a contiguous slice
     * @throws NullPointerException if {@code that} is null
     */
    public Option<Integer> indexOfSliceOption(Iterable<? extends A> that) { return vector.indexOfSliceOption(that); }

    /**
     * Complexity: O(n * m) for a slice of m elements, as {@link Vector#indexOfSliceOption(Iterable, int)}.
     *
     * @param that A slice
     * @param from The first position searched
     * @return the index of the first occurrence of {@code that} at or after {@code from}
     * @throws NullPointerException if {@code that} is null
     */
    public Option<Integer> indexOfSliceOption(Iterable<? extends A> that, int from) { return vector.indexOfSliceOption(that, from); }

    /**
     * Complexity: O(n * m) for a slice of m elements, as {@link Vector#lastIndexOfSliceOption(Iterable)}.
     *
     * @param that A slice
     * @return the index of the last occurrence of {@code that} as a contiguous slice
     * @throws NullPointerException if {@code that} is null
     */
    public Option<Integer> lastIndexOfSliceOption(Iterable<? extends A> that) { return vector.lastIndexOfSliceOption(that); }

    /**
     * Complexity: O(n * m) for a slice of m elements, as {@link Vector#lastIndexOfSliceOption(Iterable, int)}.
     *
     * @param that A slice
     * @param end  The last position an occurrence may start at
     * @return the index of the last occurrence of {@code that} starting at or before {@code end}
     * @throws NullPointerException if {@code that} is null
     */
    public Option<Integer> lastIndexOfSliceOption(Iterable<? extends A> that, int end) { return vector.lastIndexOfSliceOption(that, end); }

    /**
     * Complexity: effectively O(1), as {@link Vector#tail()}.
     *
     * @return all elements but the first, if there are any
     */
    public Option<NonEmptyVector<A>> tailNonEmpty() {
        return vector.size() == 1 ? Option.none() : Option.some(new NonEmptyVector<>(vector.tail()));
    }

    /**
     * Complexity: effectively O(1), as {@link Vector#init()}.
     *
     * @return all elements but the last, if there are any
     */
    public Option<NonEmptyVector<A>> initNonEmpty() {
        return vector.size() == 1 ? Option.none() : Option.some(new NonEmptyVector<>(vector.init()));
    }

    // -- Object

    /**
     * Whether {@code o} is a {@code NonEmptyVector} with equal elements in the same order.
     * <p>
     * Complexity: O(n) when {@code o} is a {@code NonEmptyVector} of the same size, O(1) otherwise.
     *
     * @param o any object
     * @return true if {@code o} is a {@code NonEmptyVector} of the same elements
     */
    @Override
    public boolean equals(@Nullable Object o) {
        return o == this || (o instanceof NonEmptyVector<?> that && vector.equals(that.vector));
    }

    /**
     * The hash of the wrapped Vector.
     * <p>
     * Complexity: O(n), as {@link Vector#hashCode()}: computed again at every call.
     *
     * @return the hash code
     */
    @Override
    public int hashCode() { return vector.hashCode(); }

    @Override
    public String toString() { return vector.mkString("NonEmptyVector(", ", ", ")"); }
}
