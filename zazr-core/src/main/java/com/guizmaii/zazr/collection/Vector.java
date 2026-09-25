package com.guizmaii.zazr.collection;

import com.guizmaii.zazr.*;
import com.guizmaii.zazr.collection.internal.AbstractIterator;
import com.guizmaii.zazr.collection.internal.ArrayType;
import com.guizmaii.zazr.collection.internal.BitMappedTrie;
import com.guizmaii.zazr.collection.internal.Collections;
import com.guizmaii.zazr.collection.internal.Iterator;
import com.guizmaii.zazr.collection.internal.JavaConverters;
import com.guizmaii.zazr.collection.internal.TraversableModule;
import com.guizmaii.zazr.collection.internal.VectorModule;
import com.guizmaii.zazr.collection.internal.VectorModule.Combinations;
import com.guizmaii.zazr.control.Either;
import com.guizmaii.zazr.control.Option;
import java.util.*;
import java.util.function.*;
import java.util.stream.Collector;
import org.jspecify.annotations.Nullable;

import static com.guizmaii.zazr.collection.internal.Collections.withSize;

/**
 * The default sequence: an immutable, indexed sequence with effectively constant time access to any element.
 * Many other operations ({@code update}, {@code append}, {@code prepend}, {@code tail}, {@code drop}, {@code take},
 * {@code slice}) are effectively constant too.
 * <p>
 * The elements are kept in a very wide and shallow tree of arrays of 32 elements, at most six levels deep. Vector
 * declares its whole API itself and implements only {@link Traversable}: every positional method carries a
 * {@code Complexity:} line in its javadoc, where "effectively O(1)" means at most six array lookups, or a copy of at
 * most six small arrays of 32 elements, whatever the size: the result shares every other element with this Vector.
 * <p>
 * Complexity: the methods without a note of their own ({@code map}, {@code filter}, {@code flatMap}, the folds,
 * {@code groupBy}, the conversions, and the factories such as {@code ofAll} and {@code range}) walk the elements once:
 * O(n), where n is the size of the result for a factory and {@code flatMap}. {@code containsAll} is O(n * m): one
 * {@code contains} per element of its argument.
 * <p>
 * A Vector built from primitive values ({@code range}, {@code ofAll(int[])} and the other primitive {@code ofAll},
 * {@code filter} of those) keeps them in primitive arrays. All the arrays of a Vector have one element type, so the
 * first write of a value of another class ({@code append}, {@code prepend}, {@code update}, {@code insert} and their
 * bulk forms, through {@link #narrow(Vector)} for example) converts every element to objects: O(n). The result holds
 * objects, and later writes on it are effectively O(1) again; a write on the original primitive Vector pays the O(n)
 * again each time.
 *
 * @param <T> Component type of the Vector.
 * @author Ruslan Sennov, Pap Lőrinc
 */
public final class Vector<T extends @Nullable Object> implements Traversable<T> {

    private static final Vector<?> EMPTY = new Vector<>(BitMappedTrie.empty());

    final BitMappedTrie<T> trie;
    private Vector(BitMappedTrie<T> trie) { this.trie = trie; }

    @SuppressWarnings("ObjectEquality")
    private Vector<T> wrap(BitMappedTrie<T> trie) {
        return (trie == this.trie)
               ? this
               : ofAll(trie);
    }

    private static <T extends @Nullable Object> Vector<T> ofAll(BitMappedTrie<T> trie) {
        return (trie.length() == 0)
               ? empty()
               : new Vector<>(trie);
    }

    /**
     * Returns the empty Vector.
     *
     * @param <T> Component type.
     * @return The empty Vector.
     */
    @SuppressWarnings("unchecked")
    public static <T extends @Nullable Object> Vector<T> empty() { return (Vector<T>) EMPTY; }

    /**
     * Returns a {@link Collector} which may be used in conjunction with
     * {@link java.util.stream.Stream#collect(Collector)} to obtain a {@link Vector}.
     *
     * @param <T> Component type of the Vector.
     * @return A {@link Vector} Collector.
     */
    public static <T extends @Nullable Object> Collector<T, Builder<T>, Vector<T>> collector() {
        final Supplier<Builder<T>> supplier = Vector::newBuilder;
        final BiConsumer<Builder<T>, T> accumulator = Builder::add;
        final BinaryOperator<Builder<T>> combiner = (left, right) -> left.addAll(right.result());
        final Function<Builder<T>, Vector<T>> finisher = Builder::result;
        return Collector.of(supplier, accumulator, combiner, finisher);
    }

    /**
     * Returns a new {@link Builder}: the cheapest way to build a Vector element by element or from a source of unknown
     * size. There is no full-size intermediate buffer: elements go into 32-wide leaf arrays that the resulting Vector
     * uses as they are, and only a partial final leaf is trimmed once.
     *
     * @param <T> Component type of the Vector.
     * @return an empty builder
     */
    public static <T extends @Nullable Object> Builder<T> newBuilder() {
        return new Builder<>(BitMappedTrie.BRANCHING_FACTOR);
    }

    /**
     * Returns a new {@link Builder} for a Vector of about {@code sizeHint} elements. The hint is not a limit. A hint of
     * {@code 32} or fewer pre-sizes the first leaf, so a Vector of exactly that many elements is built without any
     * array copy (a smaller result still trims the leaf once); a larger hint currently changes nothing and the builder
     * costs the same as with {@link #newBuilder()}.
     *
     * @param sizeHint the expected number of elements, {@code >= 0}
     * @param <T>      Component type of the Vector.
     * @return an empty builder
     * @throws IllegalArgumentException if {@code sizeHint < 0}
     */
    public static <T extends @Nullable Object> Builder<T> newBuilder(int sizeHint) {
        if (sizeHint < 0) {
            throw new IllegalArgumentException("sizeHint must not be negative: " + sizeHint);
        }
        return new Builder<>(Math.max(1, Math.min(sizeHint, BitMappedTrie.BRANCHING_FACTOR)));
    }

    /**
     * Narrows a {@code Vector<? extends T>} to {@code Vector<T>} via a
     * type-safe cast. Safe here because the vector is immutable and no elements
     * can be added that would violate the type (covariance)
     *
     * @param vector the vector to narrow
     * @param <T>    the target element type
     * @return the same vector viewed as {@code Vector<T>}
     */
    @SuppressWarnings("unchecked")
    public static <T extends @Nullable Object> Vector<T> narrow(Vector<? extends T> vector) { return (Vector<T>) vector; }

    /**
     * Returns a singleton {@code Vector}, i.e. a {@code Vector} of one element.
     *
     * @param element An element.
     * @param <T>     The component type
     * @return A new Vector instance containing the given element
     */
    public static <T extends @Nullable Object> Vector<T> of(T element) {
        return ofAll(BitMappedTrie.ofAll(new Object[]{element}));
    }

    /**
     * Creates a Vector of the given elements.
     *
     * @param <T>      Component type of the Vector.
     * @param elements Zero or more elements.
     * @return A vector containing the given elements in the same order.
     * @throws NullPointerException if {@code elements} is null
     */
    @SafeVarargs
    @SuppressWarnings("varargs")
    public static <T extends @Nullable Object> Vector<T> of(T ... elements) {
        Objects.requireNonNull(elements, "elements is null");
        return ofAll(BitMappedTrie.ofAll(elements));
    }

    /**
     * Returns a Vector containing {@code n} values of a given Function {@code f}
     * over a range of integer values from 0 to {@code n - 1}.
     *
     * @param <T> Component type of the Vector
     * @param n   The number of elements in the Vector
     * @param f   The Function computing element values
     * @return A Vector consisting of elements {@code f(0),f(1), ..., f(n - 1)}
     * @throws NullPointerException if {@code f} is null
     */
    public static <T extends @Nullable Object> Vector<T> tabulate(int n, Function<? super Integer, ? extends T> f) {
        Objects.requireNonNull(f, "f is null");
        final Builder<T> builder = newBuilder(Math.max(n, 0));
        builder.addTabulated(n, f);
        return builder.result();
    }

    /**
     * Returns a Vector containing {@code n} values supplied by a given Supplier {@code s}.
     *
     * @param <T> Component type of the Vector
     * @param n   The number of elements in the Vector
     * @param s   The Supplier computing element values
     * @return A Vector of size {@code n}, where each element contains the result supplied by {@code s}.
     * @throws NullPointerException if {@code s} is null
     */
    public static <T extends @Nullable Object> Vector<T> fill(int n, Supplier<? extends T> s) {
        Objects.requireNonNull(s, "s is null");
        final Builder<T> builder = newBuilder(Math.max(n, 0));
        builder.addTabulated(n, i -> s.get());
        return builder.result();
    }

    /**
     * Returns a Vector containing {@code n} times the given {@code element}
     *
     * @param <T>     Component type of the Vector
     * @param n       The number of elements in the Vector
     * @param element The element
     * @return A Vector of size {@code n}, where each element is the given {@code element}.
     */
    public static <T extends @Nullable Object> Vector<T> fill(int n, T element) {
        final Builder<T> builder = newBuilder(Math.max(n, 0));
        builder.addRepeated(n, element);
        return builder.result();
    }

    /**
     * Creates a Vector of the given elements.
     * <p>
     * The resulting vector has the same iteration order as the given iterable of elements
     * if the iteration order of the elements is stable.
     *
     * @param <T>      Component type of the Vector.
     * @param iterable An Iterable of elements.
     * @return A vector containing the given elements in the same order.
     * @throws NullPointerException if {@code iterable} is null
     */
    @SuppressWarnings("unchecked")
    public static <T extends @Nullable Object> Vector<T> ofAll(Iterable<? extends T> iterable) {
        Objects.requireNonNull(iterable, "iterable is null");
        if (iterable instanceof Traversable && com.guizmaii.zazr.collection.internal.Collections.isEmpty(iterable)) {
            return empty();
        }
        if (iterable instanceof Vector) {
            return (Vector<T>) iterable;
        }
        if (JavaConverters.underlying(iterable) instanceof Vector<?> underlying) {
            return (Vector<T>) underlying;
        }
        if (com.guizmaii.zazr.collection.internal.Collections.isTraversableAgain(iterable)) {
            // a sized source (a JDK Collection, a Vavr Traversable): one bulk copy into a flat array, then grouped into
            // leaves, is cheaper than element-wise adds; the builder pays off for one-shot and unsized sources only
            return ofAll(BitMappedTrie.ofAll(withSize(iterable).toArray()));
        }
        return Vector.<T> newBuilder().addAll(iterable).result();
    }

    /**
     * Creates a Vector that contains the elements of the given {@link java.util.stream.Stream}.
     *
     * @param javaStream A {@link java.util.stream.Stream}
     * @param <T>        Component type of the Stream.
     * @return A Vector containing the given elements in the same order.
     */
    public static <T extends @Nullable Object> Vector<T> ofAll(java.util.stream.Stream<? extends T> javaStream) {
        Objects.requireNonNull(javaStream, "javaStream is null");
        final Builder<T> builder = newBuilder();
        javaStream.forEachOrdered(builder::add);
        return builder.result();
    }

    /**
     * Creates a Vector from boolean values.
     *
     * @param elements boolean values
     * @return A new Vector of Boolean values
     * @throws NullPointerException if elements is null
     */
    public static Vector<Boolean> ofAll(boolean ... elements) {
        Objects.requireNonNull(elements, "elements is null");
        return ofAll(BitMappedTrie.ofAll(elements));
    }

    /**
     * Creates a Vector from byte values.
     *
     * @param elements byte values
     * @return A new Vector of Byte values
     * @throws NullPointerException if elements is null
     */
    public static Vector<Byte> ofAll(byte ... elements) {
        Objects.requireNonNull(elements, "elements is null");
        return ofAll(BitMappedTrie.ofAll(elements));
    }

    /**
     * Creates a Vector from char values.
     *
     * @param elements char values
     * @return A new Vector of Character values
     * @throws NullPointerException if elements is null
     */
    public static Vector<Character> ofAll(char ... elements) {
        Objects.requireNonNull(elements, "elements is null");
        return ofAll(BitMappedTrie.ofAll(elements));
    }

    /**
     * Creates a Vector from double values.
     *
     * @param elements double values
     * @return A new Vector of Double values
     * @throws NullPointerException if elements is null
     */
    public static Vector<Double> ofAll(double ... elements) {
        Objects.requireNonNull(elements, "elements is null");
        return ofAll(BitMappedTrie.ofAll(elements));
    }

    /**
     * Creates a Vector from float values.
     *
     * @param elements float values
     * @return A new Vector of Float values
     * @throws NullPointerException if elements is null
     */
    public static Vector<Float> ofAll(float ... elements) {
        Objects.requireNonNull(elements, "elements is null");
        return ofAll(BitMappedTrie.ofAll(elements));
    }

    /**
     * Creates a Vector from int values.
     *
     * @param elements int values
     * @return A new Vector of Integer values
     * @throws NullPointerException if elements is null
     */
    public static Vector<Integer> ofAll(int ... elements) {
        Objects.requireNonNull(elements, "elements is null");
        return ofAll(BitMappedTrie.ofAll(elements));
    }

    /**
     * Creates a Vector from long values.
     *
     * @param elements long values
     * @return A new Vector of Long values
     * @throws NullPointerException if elements is null
     */
    public static Vector<Long> ofAll(long ... elements) {
        Objects.requireNonNull(elements, "elements is null");
        return ofAll(BitMappedTrie.ofAll(elements));
    }

    /**
     * Creates a Vector from short values.
     *
     * @param elements short values
     * @return A new Vector of Short values
     * @throws NullPointerException if elements is null
     */
    public static Vector<Short> ofAll(short ... elements) {
        Objects.requireNonNull(elements, "elements is null");
        return ofAll(BitMappedTrie.ofAll(elements));
    }

    /**
     * Creates a Vector of char numbers starting from {@code from}, extending to {@code toExclusive - 1}.
     * <p>
     * Examples:
     * <pre>
     * {@code
     * Vector.range('a', 'a')  // = Vector()
     * Vector.range('b', 'a')  // = Vector()
     * Vector.range('a', 'c')  // = Vector('a', 'b')
     * }
     * </pre>
     *
     * @param from        the first char
     * @param toExclusive the last char + 1
     * @return a range of char values as specified or the empty range if {@code from >= toExclusive}
     */
    public static Vector<Character> range(char from, char toExclusive) {
        return ofAll(ArrayType.<char[]> asPrimitives(char.class, Iterator.range(from, toExclusive)));
    }

    /**
     * Creates a Vector of char numbers starting from {@code from}, extending to {@code toExclusive - 1},
     * with {@code step}.
     * <p>
     * Examples:
     * <pre>
     * {@code
     * Vector.rangeBy('a', 'c', 1)  // = Vector('a', 'b')
     * Vector.rangeBy('a', 'd', 2)  // = Vector('a', 'c')
     * Vector.rangeBy('d', 'a', -2) // = Vector('d', 'b')
     * Vector.rangeBy('d', 'a', 2)  // = Vector()
     * }
     * </pre>
     *
     * @param from        the first char
     * @param toExclusive the last char + 1
     * @param step        the step
     * @return a range of char values as specified or the empty range if<br>
     * {@code from >= toExclusive} and {@code step > 0} or<br>
     * {@code from <= toExclusive} and {@code step < 0}
     * @throws IllegalArgumentException if {@code step} is zero
     */
    public static Vector<Character> rangeBy(char from, char toExclusive, int step) {
        return ofAll(ArrayType.<char[]> asPrimitives(char.class, Iterator.rangeBy(from, toExclusive, step)));
    }

    /**
     * Creates a Vector of double numbers starting from {@code from}, extending up to (but not including) {@code toExclusive},
     * with {@code step}.
     * <p>
     * Examples:
     * <pre>
     * {@code
     * Vector.rangeBy(1.0, 3.0, 1.0)  // = Vector(1.0, 2.0)
     * Vector.rangeBy(1.0, 4.0, 2.0)  // = Vector(1.0, 3.0)
     * Vector.rangeBy(4.0, 1.0, -2.0) // = Vector(4.0, 2.0)
     * Vector.rangeBy(4.0, 1.0, 2.0)  // = Vector()
     * }
     * </pre>
     *
     * @param from        the first double
     * @param toExclusive the upper bound (exclusive)
     * @param step        the step
     * @return a range of double values as specified or the empty range if<br>
     * {@code from >= toExclusive} and {@code step > 0} or<br>
     * {@code from <= toExclusive} and {@code step < 0}
     * @throws IllegalArgumentException if {@code step} is zero
     */
    public static Vector<Double> rangeBy(double from, double toExclusive, double step) {
        return ofAll(ArrayType.<double[]> asPrimitives(double.class, Iterator.rangeBy(from, toExclusive, step)));
    }

    /**
     * Creates a Vector of int numbers starting from {@code from}, extending to {@code toExclusive - 1}.
     * <p>
     * Examples:
     * <pre>
     * {@code
     * Vector.range(0, 0)  // = Vector()
     * Vector.range(2, 0)  // = Vector()
     * Vector.range(-2, 2) // = Vector(-2, -1, 0, 1)
     * }
     * </pre>
     *
     * @param from        the first number
     * @param toExclusive the last number + 1
     * @return a range of int values as specified or the empty range if {@code from >= toExclusive}
     */
    public static Vector<Integer> range(int from, int toExclusive) {
        return ofAll(ArrayType.<int[]> asPrimitives(int.class, Iterator.range(from, toExclusive)));
    }

    /**
     * Creates a Vector of int numbers starting from {@code from}, extending to {@code toExclusive - 1},
     * with {@code step}.
     * <p>
     * Examples:
     * <pre>
     * {@code
     * Vector.rangeBy(1, 3, 1)  // = Vector(1, 2)
     * Vector.rangeBy(1, 4, 2)  // = Vector(1, 3)
     * Vector.rangeBy(4, 1, -2) // = Vector(4, 2)
     * Vector.rangeBy(4, 1, 2)  // = Vector()
     * }
     * </pre>
     *
     * @param from        the first number
     * @param toExclusive the last number + 1
     * @param step        the step
     * @return a range of int values as specified or the empty range if<br>
     * {@code from >= toExclusive} and {@code step > 0} or<br>
     * {@code from <= toExclusive} and {@code step < 0}
     * @throws IllegalArgumentException if {@code step} is zero
     */
    public static Vector<Integer> rangeBy(int from, int toExclusive, int step) {
        return ofAll(ArrayType.<int[]> asPrimitives(int.class, Iterator.rangeBy(from, toExclusive, step)));
    }

    /**
     * Creates a Vector of long numbers starting from {@code from}, extending to {@code toExclusive - 1}.
     * <p>
     * Examples:
     * <pre>
     * {@code
     * Vector.range(0L, 0L)  // = Vector()
     * Vector.range(2L, 0L)  // = Vector()
     * Vector.range(-2L, 2L) // = Vector(-2L, -1L, 0L, 1L)
     * }
     * </pre>
     *
     * @param from        the first number
     * @param toExclusive the last number + 1
     * @return a range of long values as specified or the empty range if {@code from >= toExclusive}
     */
    public static Vector<Long> range(long from, long toExclusive) {
        return ofAll(ArrayType.<long[]> asPrimitives(long.class, Iterator.range(from, toExclusive)));
    }

    /**
     * Creates a Vector of long numbers starting from {@code from}, extending to {@code toExclusive - 1},
     * with {@code step}.
     * <p>
     * Examples:
     * <pre>
     * {@code
     * Vector.rangeBy(1L, 3L, 1L)  // = Vector(1L, 2L)
     * Vector.rangeBy(1L, 4L, 2L)  // = Vector(1L, 3L)
     * Vector.rangeBy(4L, 1L, -2L) // = Vector(4L, 2L)
     * Vector.rangeBy(4L, 1L, 2L)  // = Vector()
     * }
     * </pre>
     *
     * @param from        the first number
     * @param toExclusive the last number + 1
     * @param step        the step
     * @return a range of long values as specified or the empty range if<br>
     * {@code from >= toExclusive} and {@code step > 0} or<br>
     * {@code from <= toExclusive} and {@code step < 0}
     * @throws IllegalArgumentException if {@code step} is zero
     */
    public static Vector<Long> rangeBy(long from, long toExclusive, long step) {
        return ofAll(ArrayType.<long[]> asPrimitives(long.class, Iterator.rangeBy(from, toExclusive, step)));
    }

    /**
     * Creates a Vector of char numbers starting from {@code from}, extending to {@code toInclusive}.
     * <p>
     * Examples:
     * <pre>
     * {@code
     * Vector.rangeClosed('a', 'a')  // = Vector('a')
     * Vector.rangeClosed('b', 'a')  // = Vector()
     * Vector.rangeClosed('a', 'c')  // = Vector('a', 'b', 'c')
     * }
     * </pre>
     *
     * @param from        the first char
     * @param toInclusive the last char
     * @return a range of char values as specified or the empty range if {@code from > toInclusive}
     */
    public static Vector<Character> rangeClosed(char from, char toInclusive) {
        return ofAll(ArrayType.<char[]> asPrimitives(char.class, Iterator.rangeClosed(from, toInclusive)));
    }

    /**
     * Creates a Vector of char numbers starting from {@code from}, extending to {@code toInclusive},
     * with {@code step}.
     * <p>
     * Examples:
     * <pre>
     * {@code
     * Vector.rangeClosedBy('a', 'c', 1)  // = Vector('a', 'b', 'c')
     * Vector.rangeClosedBy('a', 'd', 2)  // = Vector('a', 'c')
     * Vector.rangeClosedBy('d', 'a', -2) // = Vector('d', 'b')
     * Vector.rangeClosedBy('d', 'a', 2)  // = Vector()
     * }
     * </pre>
     *
     * @param from        the first char
     * @param toInclusive the last char
     * @param step        the step
     * @return a range of char values as specified or the empty range if<br>
     * {@code from > toInclusive} and {@code step > 0} or<br>
     * {@code from < toInclusive} and {@code step < 0}
     * @throws IllegalArgumentException if {@code step} is zero
     */
    public static Vector<Character> rangeClosedBy(char from, char toInclusive, int step) {
        return ofAll(ArrayType.<char[]> asPrimitives(char.class, Iterator.rangeClosedBy(from, toInclusive, step)));
    }

    /**
     * Creates a Vector of double numbers starting from {@code from}, extending to {@code toInclusive},
     * with {@code step}.
     * <p>
     * Examples:
     * <pre>
     * {@code
     * Vector.rangeClosedBy(1.0, 3.0, 1.0)  // = Vector(1.0, 2.0, 3.0)
     * Vector.rangeClosedBy(1.0, 4.0, 2.0)  // = Vector(1.0, 3.0)
     * Vector.rangeClosedBy(4.0, 1.0, -2.0) // = Vector(4.0, 2.0)
     * Vector.rangeClosedBy(4.0, 1.0, 2.0)  // = Vector()
     * }
     * </pre>
     *
     * @param from        the first double
     * @param toInclusive the last double
     * @param step        the step
     * @return a range of double values as specified or the empty range if<br>
     * {@code from > toInclusive} and {@code step > 0} or<br>
     * {@code from < toInclusive} and {@code step < 0}
     * @throws IllegalArgumentException if {@code step} is zero
     */
    public static Vector<Double> rangeClosedBy(double from, double toInclusive, double step) {
        return ofAll(ArrayType.<double[]> asPrimitives(double.class, Iterator.rangeClosedBy(from, toInclusive, step)));
    }

    /**
     * Creates a Vector of int numbers starting from {@code from}, extending to {@code toInclusive}.
     * <p>
     * Examples:
     * <pre>
     * {@code
     * Vector.rangeClosed(0, 0)  // = Vector(0)
     * Vector.rangeClosed(2, 0)  // = Vector()
     * Vector.rangeClosed(-2, 2) // = Vector(-2, -1, 0, 1, 2)
     * }
     * </pre>
     *
     * @param from        the first number
     * @param toInclusive the last number
     * @return a range of int values as specified or the empty range if {@code from > toInclusive}
     */
    public static Vector<Integer> rangeClosed(int from, int toInclusive) {
        return ofAll(ArrayType.<int[]> asPrimitives(int.class, Iterator.rangeClosed(from, toInclusive)));
    }

    /**
     * Creates a Vector of int numbers starting from {@code from}, extending to {@code toInclusive},
     * with {@code step}.
     * <p>
     * Examples:
     * <pre>
     * {@code
     * Vector.rangeClosedBy(1, 3, 1)  // = Vector(1, 2, 3)
     * Vector.rangeClosedBy(1, 4, 2)  // = Vector(1, 3)
     * Vector.rangeClosedBy(4, 1, -2) // = Vector(4, 2)
     * Vector.rangeClosedBy(4, 1, 2)  // = Vector()
     * }
     * </pre>
     *
     * @param from        the first number
     * @param toInclusive the last number
     * @param step        the step
     * @return a range of int values as specified or the empty range if<br>
     * {@code from > toInclusive} and {@code step > 0} or<br>
     * {@code from < toInclusive} and {@code step < 0}
     * @throws IllegalArgumentException if {@code step} is zero
     */
    public static Vector<Integer> rangeClosedBy(int from, int toInclusive, int step) {
        return ofAll(ArrayType.<int[]> asPrimitives(int.class, Iterator.rangeClosedBy(from, toInclusive, step)));
    }

    /**
     * Creates a Vector of long numbers starting from {@code from}, extending to {@code toInclusive}.
     * <p>
     * Examples:
     * <pre>
     * {@code
     * Vector.rangeClosed(0L, 0L)  // = Vector(0L)
     * Vector.rangeClosed(2L, 0L)  // = Vector()
     * Vector.rangeClosed(-2L, 2L) // = Vector(-2L, -1L, 0L, 1L, 2L)
     * }
     * </pre>
     *
     * @param from        the first number
     * @param toInclusive the last number
     * @return a range of long values as specified or the empty range if {@code from > toInclusive}
     */
    public static Vector<Long> rangeClosed(long from, long toInclusive) {
        return ofAll(ArrayType.<long[]> asPrimitives(long.class, Iterator.rangeClosed(from, toInclusive)));
    }

    /**
     * Creates a Vector of long numbers starting from {@code from}, extending to {@code toInclusive},
     * with {@code step}.
     * <p>
     * Examples:
     * <pre>
     * {@code
     * Vector.rangeClosedBy(1L, 3L, 1L)  // = Vector(1L, 2L, 3L)
     * Vector.rangeClosedBy(1L, 4L, 2L)  // = Vector(1L, 3L)
     * Vector.rangeClosedBy(4L, 1L, -2L) // = Vector(4L, 2L)
     * Vector.rangeClosedBy(4L, 1L, 2L)  // = Vector()
     * }
     * </pre>
     *
     * @param from        the first number
     * @param toInclusive the last number
     * @param step        the step
     * @return a range of long values as specified or the empty range if<br>
     * {@code from > toInclusive} and {@code step > 0} or<br>
     * {@code from < toInclusive} and {@code step < 0}
     * @throws IllegalArgumentException if {@code step} is zero
     */
    public static Vector<Long> rangeClosedBy(long from, long toInclusive, long step) {
        return ofAll(ArrayType.<long[]> asPrimitives(long.class, Iterator.rangeClosedBy(from, toInclusive, step)));
    }

    /**
     * Transposes the rows and columns of a {@link Vector} matrix.
     *
     * @param <T> matrix element type
     * @param matrix to be transposed.
     * @return a transposed {@link Vector} matrix.
     * @throws IllegalArgumentException if the row lengths of {@code matrix} differ.
     * <p>
     * ex: {@code
     * Vector.transpose(Vector(Vector(1,2,3), Vector(4,5,6))) → Vector(Vector(1,4), Vector(2,5), Vector(3,6))
     * }
     * <p>
     * Complexity: O(rows * columns); the matrix itself is returned when it has no or one element.
     */
    public static <T extends @Nullable Object> Vector<Vector<T>> transpose(Vector<Vector<T>> matrix) {
        return com.guizmaii.zazr.collection.internal.Collections.transpose(matrix, Vector::ofAll, Vector::of);
    }

    /**
     * Creates a Vector from a seed value and a function.
     * The function takes the seed at first.
     * The function should return {@code None} when it's
     * done generating the Vector, otherwise {@code Some} {@code Tuple}
     * of the value to add to the resulting Vector and the element for
     * the next call.
     * <p>
     * Example:
     * <pre>
     * {@code
     * Vector.unfoldRight(10, x -> x == 0
     *             ? Option.none()
     *             : Option.some(new Tuple2<>(x, x-1)));
     * // Vector(10, 9, 8, 7, 6, 5, 4, 3, 2, 1)
     * }
     * </pre>
     *
     * @param <T>  type of seeds
     * @param <U>  type of unfolded values
     * @param seed the start value for the iteration
     * @param f    the function to get the next step of the iteration
     * @return a Vector with the values built up by the iteration
     * @throws NullPointerException if {@code f} is null
     */
    public static <T extends @Nullable Object, U extends @Nullable Object> Vector<U> unfoldRight(T seed, Function<? super T, Option<Tuple2<? extends U, ? extends T>>> f) {
        return Iterator.unfoldRight(seed, f).toVector();
    }

    /**
     * Creates a Vector from a seed value and a function.
     * The function takes the seed at first.
     * The function should return {@code None} when it's
     * done generating the Vector, otherwise {@code Some} {@code Tuple}
     * of the element for the next call and
     * the value to add to the resulting Vector.
     * <p>
     * Example:
     * <pre>
     * {@code
     * Vector.unfoldLeft(10, x -> x == 0
     *             ? Option.none()
     *             : Option.some(new Tuple2<>(x-1, x)));
     * // Vector(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
     * }
     * </pre>
     *
     * @param <T>  type of seeds
     * @param <U>  type of unfolded values
     * @param seed the start value for the iteration
     * @param f    the function to get the next step of the iteration
     * @return a Vector with the values built up by the iteration
     * @throws NullPointerException if {@code f} is null
     */
    public static <T extends @Nullable Object, U extends @Nullable Object> Vector<U> unfoldLeft(T seed, Function<? super T, Option<Tuple2<? extends T, ? extends U>>> f) {
        return Iterator.unfoldLeft(seed, f).toVector();
    }

    /**
     * Creates a Vector from a seed value and a function.
     * The function takes the seed at first.
     * The function should return {@code None} when it's
     * done generating the Vector, otherwise {@code Some} {@code Tuple}
     * of the element for the next call and
     * the value to add to the resulting Vector.
     * <p>
     * Example:
     * <pre>
     * {@code
     * Vector.unfold(10, x -> x == 0
     *             ? Option.none()
     *             : Option.some(new Tuple2<>(x-1, x)));
     * // Vector(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
     * }
     * </pre>
     *
     * @param <T>  type of seeds and unfolded values
     * @param seed the start value for the iteration
     * @param f    the function to get the next step of the iteration
     * @return a Vector with the values built up by the iteration
     * @throws NullPointerException if {@code f} is null
     */
    public static <T extends @Nullable Object> Vector<T> unfold(T seed, Function<? super T, Option<Tuple2<? extends T, ? extends T>>> f) {
        return Iterator.unfold(seed, f).toVector();
    }

    /**
     * Concatenates nested iterables into one Vector, in one pass over the builder. Static, like every {@code flatten} in
     * Zazr, because Java cannot demand of an instance method that the receiver's element type be a collection.
     * <p>
     * Complexity: O(m) for m inner elements in total, each added to the result once.
     *
     * @param nested Iterables of elements
     * @param <T>    Component type of the inner iterables
     * @return the inner elements, in order
     * @throws NullPointerException if {@code nested}, an inner iterable or an element is null
     */
    public static <T extends @Nullable Object> Vector<T> flatten(Iterable<? extends Iterable<? extends T>> nested) {
        Objects.requireNonNull(nested, "nested is null");
        final Builder<T> builder = newBuilder();
        for (Iterable<? extends T> inner : nested) {
            builder.addAll(inner);
        }
        return builder.result();
    }

    // -- the sequence API. Vector implements only Traversable and declares every sequence method itself,
    // with Vector return types; every positional method states its cost.
    // "Effectively O(1)" means O(log32 n): a trie access or a path copy of at most six nodes.

    /**
     * Appends an element.
     * <p>
     * Complexity: effectively O(1): copies a few small arrays of 32 elements, not the Vector. On a Vector of primitive
     * values, a value of another class first converts every element: O(n), paid again at each such write on the same
     * Vector (see the class documentation).
     *
     * @param element the element to append
     * @return a new Vector ending with {@code element}
     * @throws NullPointerException if {@code element} is null
     */
    public Vector<T> append(T element) { return appendAll(com.guizmaii.zazr.collection.List.of(element)); }

    /**
     * Appends all elements of the given iterable, in iteration order.
     * <p>
     * Complexity: O(m) for m appended elements, even when this Vector is much shorter than the argument; the elements
     * of this Vector are shared, not copied. O(1) when this Vector is empty and {@code iterable} is a Vector, which is
     * returned as is. On a Vector of primitive values, a value of another class first converts every element: O(n),
     * paid again at each such write on the same Vector (see the class documentation).
     *
     * @param iterable the elements to append
     * @return a new Vector ending with the given elements, or this Vector if there are none
     * @throws NullPointerException if {@code iterable} or one of its elements is null
     */
    public Vector<T> appendAll(Iterable<? extends T> iterable) {
        Objects.requireNonNull(iterable, "iterable is null");
        if (isEmpty()) {
            return ofAll(iterable);
        }
        if (!com.guizmaii.zazr.collection.internal.Collections.isTraversableAgain(iterable)) {
            // a one-shot source (an Iterator, typically wrapping a java.util.stream) is read exactly once: built with the
            // builder, which also answers whether there is anything to append, then appended by path copy
            final Vector<T> elements = ofAll(iterable);
            return elements.isEmpty() ? this : appendAll(elements);
        }
        if (com.guizmaii.zazr.collection.internal.Collections.isEmpty(iterable)) {
            return this;
        }
        return new Vector<>(trie.appendAll(iterable));
    }

    /**
     * An unmodifiable {@link java.util.List} view of this Vector, in its order: nothing is copied, reads go through
     * to this Vector, which never changes, and every mutator of the view (including those of its iterators and
     * sub-lists) throws {@link UnsupportedOperationException}. {@code reversed()} and {@code subList} are views too.
     * A mutable copy is {@code new java.util.ArrayList<>(vector.asJava())}; {@code Vector.ofAll} given the view
     * returns this Vector without copying.
     * <p>
     * Complexity: O(1): nothing is copied. {@code get} on the view is effectively O(1), as {@link #get(int)}, and each
     * step of its iterator is O(1).
     *
     * @return an unmodifiable {@code java.util.List} view
     */
    public java.util.List<T> asJava() {
        return JavaConverters.asJava(this);
    }

    /**
     * All combinations of the elements, for every size from 0 to {@code length()}, by position:
     * {@code Vector(1, 2).combinations()} is {@code Vector(Vector(), Vector(1), Vector(2), Vector(1, 2))}.
     * <p>
     * Complexity: O(n * 2^n): the 2^n combinations hold n * 2^(n - 1) elements in all, and building them visits as
     * many partial choices.
     *
     * @return the combinations, ordered by size, then by position
     */
    public Vector<Vector<T>> combinations() { return rangeClosed(0, length()).map(this::combinations).flatMap(Function.identity()); }

    /**
     * All combinations of {@code k} elements, selected by position (equal elements are distinct positions).
     * <p>
     * Complexity: O(k * C(n, k) + C(n, 0) + ... + C(n, k)): the C(n, k) combinations of k elements are built, and
     * every choice of fewer than k elements is visited on the way, even one that cannot be completed. That is
     * O(k * C(n, k)) for k up to n / 2, and up to O(2^n) above: {@code combinations(n)} does O(2^n) work to return
     * one combination.
     *
     * @param k the size of each combination; {@code k <= 0} gives one empty combination
     * @return the k-combinations, in position order
     */
    public Vector<Vector<T>> combinations(int k) { return Combinations.apply(this, Math.max(k, 0)); }

    /**
     * Whether this Vector contains {@code that} as a contiguous slice.
     * <p>
     * Complexity: O(n * m) for a slice of m elements.
     *
     * @param that the slice to look for
     * @return true if {@code that} occurs contiguously in this Vector (an empty slice always does)
     * @throws NullPointerException if {@code that} is null
     */
    public boolean containsSlice(Iterable<? extends T> that) {
        Objects.requireNonNull(that, "that is null");
        return indexOfSlice(that) >= 0;
    }

    /**
     * Returns a new {@code Vector} containing the elements of this instance
     * with all duplicates removed. Element equality is determined using {@code equals}.
     * <p>
     * Complexity: O(n).
     *
     * @return a new {@code Vector} without duplicate elements
     */
    public Vector<T> distinct() { return distinctBy(Function.identity()); }

    /**
     * Returns a new {@code Vector} containing the elements of this instance
     * without duplicates, as determined by the given {@code comparator}; the first of two equal elements is kept.
     * <p>
     * Complexity: O(n log n) comparisons.
     *
     * @param comparator a comparator used to determine equality of elements
     * @return a new {@code Vector} with duplicates removed
     * @throws NullPointerException if {@code comparator} is null
     */
    public Vector<T> distinctBy(Comparator<? super T> comparator) {
        Objects.requireNonNull(comparator, "comparator is null");
        final java.util.Set<T> seen = new java.util.TreeSet<>(comparator);
        return filter(seen::add);
    }

    /**
     * Returns a new {@code Vector} containing the elements of this instance
     * without duplicates, based on keys extracted from elements using {@code keyExtractor}.
     * <p>
     * The first occurrence of each key is retained in the resulting sequence.
     * <p>
     * Complexity: O(n).
     *
     * @param keyExtractor a function to extract keys for determining uniqueness
     * @param <U>          the type of key
     * @return a new {@code Vector} with duplicates removed based on keys
     * @throws NullPointerException if {@code keyExtractor} is null
     */
    public <U extends @Nullable Object> Vector<T> distinctBy(Function<? super T, ? extends U> keyExtractor) {
        Objects.requireNonNull(keyExtractor, "keyExtractor is null");
        final java.util.Set<U> seen = new java.util.HashSet<>(length());
        return filter(t -> seen.add(keyExtractor.apply(t)));
    }

    /**
     * The complement of {@link #distinct()}: the elements occurring more than once, each once, in order of first
     * occurrence. {@code isEmpty()} on the result is the "all distinct" test.
     * <p>
     * Complexity: O(n), one hash lookup per element.
     *
     * @return the duplicated elements
     */
    public Vector<T> duplicates() { return duplicatesBy(Function.identity()); }

    /**
     * {@link #duplicates()} under a key: the first element of each key occurring more than once, in order of first
     * occurrence. One pass.
     * <p>
     * Complexity: O(n), one key and one hash lookup per element.
     *
     * @param keyExtractor Computes the key
     * @param <U>          Key type
     * @return the first element of each duplicated key
     * @throws NullPointerException if {@code keyExtractor} is null
     */
    public <U extends @Nullable Object> Vector<T> duplicatesBy(Function<? super T, ? extends U> keyExtractor) {
        Objects.requireNonNull(keyExtractor, "keyExtractor is null");
        // the first element of every key, in first-occurrence order, plus the keys seen again: one pass, the key computed once
        final java.util.LinkedHashMap<U, T> first = new java.util.LinkedHashMap<>();
        final java.util.HashSet<U> duplicated = new java.util.HashSet<>();
        for (T element : this) {
            final U key = keyExtractor.apply(element);
            if (first.putIfAbsent(key, element) != null) {
                duplicated.add(key);
            }
        }
        if (duplicated.isEmpty()) {
            return empty();
        }
        final Builder<T> builder = newBuilder(duplicated.size());
        for (java.util.Map.Entry<U, T> entry : first.entrySet()) {
            if (duplicated.contains(entry.getKey())) {
                builder.add(entry.getValue());
            }
        }
        return builder.result();
    }

    /**
     * Removes the duplicates under {@code comparator}, keeping the last occurrence of each.
     * <p>
     * Complexity: O(n log n) comparisons.
     *
     * @param comparator decides which elements are equal
     * @return the distinct elements, each at the position of its last occurrence
     * @throws NullPointerException if {@code comparator} is null
     */
    public Vector<T> distinctByKeepLast(Comparator<? super T> comparator) {
        Objects.requireNonNull(comparator, "comparator is null");
        return ofAll(Iterator.ofAll(this).distinctByKeepLast(comparator));
    }

    /**
     * Removes the duplicates under {@code keyExtractor}, keeping the last occurrence of each key.
     * <p>
     * Complexity: O(n).
     *
     * @param keyExtractor computes the key
     * @param <U>          the key type
     * @return the elements with a distinct key, each at the position of its last occurrence
     * @throws NullPointerException if {@code keyExtractor} is null
     */
    public <U extends @Nullable Object> Vector<T> distinctByKeepLast(Function<? super T, ? extends U> keyExtractor) {
        Objects.requireNonNull(keyExtractor, "keyExtractor is null");
        return ofAll(Iterator.ofAll(this).distinctByKeepLast(keyExtractor));
    }

    /**
     * Returns a new {@code Vector} without the first {@code n} elements,
     * or an empty instance if this contains fewer than {@code n} elements.
     * <p>
     * Complexity: effectively O(1): the result shares its elements with this Vector; only a few small arrays at the
     * cut are copied.
     *
     * @param n the number of elements to drop
     * @return a new instance excluding the first {@code n} elements
     */
    public Vector<T> drop(int n) {
        return wrap(trie.drop(n));
    }

    /**
     * Returns a new {@code Vector} starting from the first element
     * that satisfies the given {@code predicate}, dropping all preceding elements.
     * <p>
     * Complexity: O(k) for k dropped elements, then one effectively O(1) {@code drop}.
     *
     * @param predicate a condition tested on each element
     * @return a new instance starting from the first element matching the predicate
     * @throws NullPointerException if {@code predicate} is null
     */
    public Vector<T> dropUntil(Predicate<? super T> predicate) {
        Objects.requireNonNull(predicate, "predicate is null");
        final int length = length();
        for (int i = 0; i < length; i++) {
            if (predicate.test(get(i))) {
                return drop(i);
            }
        }
        return empty();
    }

    /**
     * Returns a new {@code Vector} starting from the first element
     * that does not satisfy the given {@code predicate}, dropping all preceding elements.
     * <p>
     * This is equivalent to {@code dropUntil(predicate.negate())}, which is useful
     * for method references that cannot be negated directly.
     * <p>
     * Complexity: O(k) for k dropped elements, then one effectively O(1) {@code drop}.
     *
     * @param predicate a condition tested on each element
     * @return a new instance starting from the first element not matching the predicate
     * @throws NullPointerException if {@code predicate} is null
     */
    public Vector<T> dropWhile(Predicate<? super T> predicate) {
        Objects.requireNonNull(predicate, "predicate is null");
        return dropUntil(predicate.negate());
    }

    /**
     * Returns a new {@code Vector} without the last {@code n} elements,
     * or an empty instance if this contains fewer than {@code n} elements.
     * <p>
     * Complexity: effectively O(1): the result shares its elements with this Vector; only a few small arrays at the
     * cut are copied.
     *
     * @param n the number of elements to drop from the end
     * @return a new instance excluding the last {@code n} elements
     */
    public Vector<T> dropRight(int n) {
        // n <= 0 first: length() - n overflows for Integer.MIN_VALUE
        return n <= 0 ? this : take(length() - n);
    }

    /**
     * Drops elements from the end until one satisfies {@code predicate}; that element is kept.
     * <p>
     * Complexity: O(k) for k dropped elements, then one effectively O(1) {@code take}.
     *
     * @param predicate tested from the last element backwards
     * @return this Vector up to and including the last element satisfying {@code predicate}
     * @throws NullPointerException if {@code predicate} is null
     */
    public Vector<T> dropRightUntil(Predicate<? super T> predicate) {
        Objects.requireNonNull(predicate, "predicate is null");
        for (int i = length() - 1; i >= 0; i--) {
            if (predicate.test(get(i))) {
                return take(i + 1);
            }
        }
        return empty();
    }

    /**
     * Drops elements from the end while they satisfy {@code predicate}: {@code dropRightUntil(predicate.negate())}.
     * <p>
     * Complexity: O(k) for k dropped elements, then one effectively O(1) {@code take}.
     *
     * @param predicate tested from the last element backwards
     * @return this Vector up to and including the last element not satisfying {@code predicate}
     * @throws NullPointerException if {@code predicate} is null
     */
    public Vector<T> dropRightWhile(Predicate<? super T> predicate) {
        Objects.requireNonNull(predicate, "predicate is null");
        return dropRightUntil(predicate.negate());
    }

    /**
     * Whether this Vector ends with {@code that}. A Vector argument is compared in place; any other iterable is
     * materialised once.
     * <p>
     * Complexity: O(m) for m elements of {@code that}.
     *
     * @param that the suffix to test
     * @return true if the last {@code m} elements equal {@code that} (an empty {@code that} is always a suffix)
     * @throws NullPointerException if {@code that} is null
     */
    public boolean endsWith(Iterable<? extends T> that) {
        Objects.requireNonNull(that, "that is null");
        final Vector<? extends T> suffix = ofAll(that);
        final int suffixLength = suffix.length();
        int i = length() - suffixLength;
        if (i < 0) {
            return false;
        }
        for (int j = 0; j < suffixLength; j++, i++) {
            if (!Objects.equals(get(i), suffix.get(j))) {
                return false;
            }
        }
        return true;
    }

    public Vector<T> filter(Predicate<? super T> predicate) {
        Objects.requireNonNull(predicate, "predicate is null");
        // stays on the trie, not the builder: the flat-array filter keeps primitive leaves unboxed and, measured, is
        // 2x faster than the builder at 1 000 elements and equal at 100 000 (the JIT likes the branch-free bulk copies)
        return wrap(trie.filter(predicate));
    }

    public Vector<T> reject(Predicate<? super T> predicate) {
        Objects.requireNonNull(predicate, "predicate is null");
        return filter(predicate.negate());
    }

    public <U extends @Nullable Object> Vector<U> flatMap(Function<? super T, ? extends Iterable<? extends U>> mapper) {
        Objects.requireNonNull(mapper, "mapper is null");
        final Builder<U> builder = newBuilder();
        trie.<Object> visit((index, leaf, start, end) -> {
            for (int i = start; i < end; i++) {
                builder.addAll(mapper.apply(trie.type.getAt(leaf, i)));
            }
            return index + end - start;
        });
        return builder.result();
    }

    /**
     * Folds the elements from the right: starts with {@code zero} and combines each element, from the last to the
     * first, with the accumulator.
     * <pre>{@code
     * // = 24
     * List.of('4', '2').foldRight(0, (x, acc) -> acc * 10 + x - '0');
     * }</pre>
     * <p>
     * Complexity: O(n), walking the elements from the last to the first without copying.
     *
     * @param <U>  the type of the accumulator
     * @param zero the initial accumulator
     * @param f    combines the next element (from the right) and the accumulator so far
     * @return the final accumulator, {@code zero} on an empty sequence
     * @throws NullPointerException if {@code f} is null
     */
    public <U extends @Nullable Object> U foldRight(U zero, BiFunction<? super T, ? super U, ? extends U> f) {
        Objects.requireNonNull(f, "f is null");
        U xs = zero;
        for (int i = length() - 1; i >= 0; i--) {
            xs = f.apply(get(i), xs);
        }
        return xs;
    }

    /**
     * The element at {@code index}.
     * <p>
     * Complexity: effectively O(1): at most six array lookups, whatever the size.
     *
     * @param index a position, {@code 0 <= index < length()}
     * @return the element at that position
     * @throws IndexOutOfBoundsException if {@code index} is out of range
     */
    public T get(int index) {
        if (isValid(index)) {
            return trie.get(index);
        } else {
            throw new IndexOutOfBoundsException("get(" + index + ")");
        }
    }
    private boolean isValid(int index) { return (index >= 0) && (index < length()); }

    /**
     * Returns the first element of this non-empty {@code Vector}.
     * <p>
     * Complexity: effectively O(1).
     *
     * @return the first element
     * @throws NoSuchElementException if this {@code Vector} is empty
     */
    public T head() {
        if (nonEmpty()) {
            return get(0);
        } else {
            throw new NoSuchElementException("head of empty Vector");
        }
    }

    public <C extends @Nullable Object> Map<C, Vector<T>> groupBy(Function<? super T, ? extends C> classifier) { return com.guizmaii.zazr.collection.internal.Collections.groupBy(this, classifier, Vector::ofAll); }

    /**
     * The index of the first occurrence of {@code element}, or -1.
     * <p>
     * Complexity: O(n).
     *
     * @param element the element to find
     * @return its first index, or -1 if absent
     */
    public int indexOf(T element) {
        return indexOf(element, 0);
    }

    /**
     * The index of the first occurrence of {@code element} at or after {@code from}, or -1. A negative
     * {@code from} counts as 0.
     * <p>
     * Complexity: O(n).
     *
     * @param element the element to find
     * @param from    the first position to look at
     * @return the first index {@code >= from} of the element, or -1 if absent
     */
    public int indexOf(T element, int from) {
        for (int i = Math.max(from, 0); i < length(); i++) {
            if (Objects.equals(get(i), element)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * {@link #indexOf(Object)} as an {@link Option}: {@code None} for -1.
     * <p>
     * Complexity: O(n), as {@link #indexOf(Object)}.
     *
     * @param element the element to find
     * @return {@code Some(index)} of its first occurrence, or {@code None}
     */
    public Option<Integer> indexOfOption(T element) {
        return com.guizmaii.zazr.collection.internal.Collections.indexOption(indexOf(element));
    }

    /**
     * {@link #indexOf(Object, int)} as an {@link Option}: {@code None} for -1.
     * <p>
     * Complexity: O(n), as {@link #indexOf(Object, int)}.
     *
     * @param element the element to find
     * @param from    the first position to look at
     * @return {@code Some(index)} of its first occurrence at or after {@code from}, or {@code None}
     */
    public Option<Integer> indexOfOption(T element, int from) {
        return com.guizmaii.zazr.collection.internal.Collections.indexOption(indexOf(element, from));
    }

    /**
     * The first index at which {@code that} occurs as a contiguous slice, or -1.
     * <p>
     * Complexity: O(n * m) for a slice of m elements.
     *
     * @param that the slice to find
     * @return the index of its first occurrence, or -1 (an empty slice occurs at 0)
     * @throws NullPointerException if {@code that} is null
     */
    public int indexOfSlice(Iterable<? extends T> that) {
        return indexOfSlice(that, 0);
    }

    /**
     * The first index at or after {@code from} at which {@code that} occurs as a contiguous slice, or -1.
     * <p>
     * Complexity: O(n * m) for a slice of m elements.
     *
     * @param that the slice to find
     * @param from the first position to look at
     * @return the index of its first occurrence at or after {@code from}, or -1
     * @throws NullPointerException if {@code that} is null
     */
    public int indexOfSlice(Iterable<? extends T> that, int from) {
        Objects.requireNonNull(that, "that is null");
        return VectorModule.Slice.indexOfSlice(this, that, from);
    }

    /**
     * {@link #indexOfSlice(Iterable)} as an {@link Option}: {@code None} for -1.
     * <p>
     * Complexity: O(n * m) for a slice of m elements, as {@link #indexOfSlice(Iterable)}.
     *
     * @param that the slice to find
     * @return {@code Some(index)} of its first occurrence, or {@code None}
     * @throws NullPointerException if {@code that} is null
     */
    public Option<Integer> indexOfSliceOption(Iterable<? extends T> that) {
        return com.guizmaii.zazr.collection.internal.Collections.indexOption(indexOfSlice(that));
    }

    /**
     * {@link #indexOfSlice(Iterable, int)} as an {@link Option}: {@code None} for -1.
     * <p>
     * Complexity: O(n * m) for a slice of m elements, as {@link #indexOfSlice(Iterable, int)}.
     *
     * @param that the slice to find
     * @param from the first position to look at
     * @return {@code Some(index)} of its first occurrence at or after {@code from}, or {@code None}
     * @throws NullPointerException if {@code that} is null
     */
    public Option<Integer> indexOfSliceOption(Iterable<? extends T> that, int from) {
        return com.guizmaii.zazr.collection.internal.Collections.indexOption(indexOfSlice(that, from));
    }

    /**
     * The index of the first element satisfying {@code predicate}, or -1.
     * <p>
     * Complexity: O(n).
     *
     * @param predicate the condition
     * @return the first index of a satisfying element, or -1
     * @throws NullPointerException if {@code predicate} is null
     */
    public int indexWhere(Predicate<? super T> predicate) {
        return indexWhere(predicate, 0);
    }

    /**
     * The index of the first element at or after {@code from} satisfying {@code predicate}, or -1. A negative
     * {@code from} counts as 0.
     * <p>
     * Complexity: O(n).
     *
     * @param predicate the condition
     * @param from      the first position to look at
     * @return the first index {@code >= from} of a satisfying element, or -1
     * @throws NullPointerException if {@code predicate} is null
     */
    public int indexWhere(Predicate<? super T> predicate, int from) {
        Objects.requireNonNull(predicate, "predicate is null");
        final int length = length();
        for (int i = Math.max(from, 0); i < length; i++) {
            if (predicate.test(get(i))) {
                return i;
            }
        }
        return -1;
    }

    /**
     * {@link #indexWhere(Predicate)} as an {@link Option}: {@code None} for -1.
     * <p>
     * Complexity: O(n), as {@link #indexWhere(Predicate)}.
     *
     * @param predicate the condition
     * @return {@code Some(index)} of the first satisfying element, or {@code None}
     * @throws NullPointerException if {@code predicate} is null
     */
    public Option<Integer> indexWhereOption(Predicate<? super T> predicate) {
        return com.guizmaii.zazr.collection.internal.Collections.indexOption(indexWhere(predicate));
    }

    /**
     * {@link #indexWhere(Predicate, int)} as an {@link Option}: {@code None} for -1.
     * <p>
     * Complexity: O(n), as {@link #indexWhere(Predicate, int)}.
     *
     * @param predicate the condition
     * @param from      the first position to look at
     * @return {@code Some(index)} of the first satisfying element at or after {@code from}, or {@code None}
     * @throws NullPointerException if {@code predicate} is null
     */
    public Option<Integer> indexWhereOption(Predicate<? super T> predicate, int from) {
        return com.guizmaii.zazr.collection.internal.Collections.indexOption(indexWhere(predicate, from));
    }

    /**
     * Returns all elements of this Vector except the last one.
     * <p>
     * This is the dual of {@link #tail()}.
     * <p>
     * Complexity: effectively O(1): the result shares its elements with this Vector; only a few small arrays at the
     * cut are copied.
     *
     * @return a new instance containing all elements except the last
     * @throws UnsupportedOperationException if this Vector is empty
     */
    public Vector<T> init() {
        if (nonEmpty()) {
            return dropRight(1);
        } else {
            throw new UnsupportedOperationException("init of empty Vector");
        }
    }

    /**
     * Returns all elements of this Vector except the last one, wrapped in an {@code Option}.
     * <p>
     * This is the dual of {@link #tailOption()}.
     * <p>
     * Complexity: effectively O(1), as {@link #init()}.
     *
     * @return {@code Some(traversable)} if non-empty, or {@code None} if this Vector is empty
     */
    public Option<Vector<T>> initOption() { return isEmpty() ? Option.none() : Option.some(init()); }

    /**
     * Inserts an element at {@code index}; the elements from that position on shift right by one.
     * <p>
     * Complexity: O(min(i, n - i)): the elements on the shorter side of i are copied, those on the longer side are
     * shared. On a Vector of primitive values, a value of another class first converts every element: O(n), paid
     * again at each such write on the same Vector (see the class documentation).
     *
     * @param index   a position, {@code 0 <= index <= length()}
     * @param element the element to insert
     * @return a new Vector with {@code element} at {@code index}
     * @throws IndexOutOfBoundsException if {@code index} is out of range
     * @throws NullPointerException      if {@code element} is null
     */
    public Vector<T> insert(int index, T element) { return insertAll(index, Iterator.of(element)); }

    /**
     * Inserts the given elements at {@code index}, in iteration order; the elements from that position on shift
     * right.
     * <p>
     * Complexity: O(m + min(i, n - i)) for m inserted elements: the elements on the shorter side of i are copied,
     * those on the longer side are shared. On a Vector of primitive values, a value of another class first converts
     * every element: O(n), paid again at each such write on the same Vector (see the class documentation).
     *
     * @param index    a position, {@code 0 <= index <= length()}
     * @param elements the elements to insert
     * @return a new Vector with the elements at {@code index}, or this Vector if there are none
     * @throws IndexOutOfBoundsException if {@code index} is out of range
     * @throws NullPointerException      if {@code elements} or one of them is null
     */
    public Vector<T> insertAll(int index, Iterable<? extends T> elements) {
        Objects.requireNonNull(elements, "elements is null");
        if ((index >= 0) && (index <= length())) {
            final Vector<T> begin = take(index).appendAll(elements);
            final Vector<T> end = drop(index);
            return (begin.size() > end.size())
                   ? begin.appendAll(end)
                   : end.prependAll(begin);
        } else {
            throw new IndexOutOfBoundsException("insert(" + index + ", e) on Vector of length " + length());
        }
    }

    /**
     * Puts {@code element} between every two elements.
     * <p>
     * Complexity: O(n).
     *
     * @param element the separator
     * @return a new Vector of 2n - 1 elements, or this Vector if it has fewer than two
     * @throws NullPointerException if {@code element} is null and this Vector has at least two elements
     */
    public Vector<T> intersperse(T element) { return ofAll(Iterator.ofAll(this).intersperse(element)); }

    /**
     * {@inheritDoc}
     * <p>
     * Complexity: O(1).
     */
    @Override
    public boolean isEmpty() { return length() == 0; }

    /**
     * Narrows to a {@link NonEmptyVector}, whose operations that cannot shrink keep that type and whose {@code head},
     * {@code last}, {@code max}, {@code min} and {@code reduce} are total.
     * <p>
     * Complexity: O(1): the result wraps this Vector, nothing is copied.
     *
     * @return {@code Some(nonEmptyVector)} sharing this Vector's elements, or {@code None} if this Vector is empty
     */
    public Option<NonEmptyVector<T>> toNonEmptyVector() { return NonEmptyVector.fromVector(this); }

    /**
     * {@inheritDoc}
     * <p>
     * Complexity: O(1) to create, and O(1) per step: moving on to the next block of 32 elements takes at most six
     * array lookups.
     */
    @Override
    public java.util.Iterator<T> iterator() {
        return isEmpty() ? Iterator.empty()
                         : trie.iterator();
    }

    /**
     * Returns the last element of this Vector.
     * <p>
     * Complexity: effectively O(1).
     *
     * @return the last element
     * @throws NoSuchElementException if this Vector is empty
     */
    public T last() {
        if (isEmpty()) {
            throw new NoSuchElementException("last of empty Vector");
        }
        return get(length() - 1);
    }

    /**
     * The index of the last occurrence of {@code element}, or -1.
     * <p>
     * Complexity: O(n).
     *
     * @param element the element to find
     * @return its last index, or -1 if absent
     */
    public int lastIndexOf(T element) {
        return lastIndexOf(element, Integer.MAX_VALUE);
    }

    /**
     * The index of the last occurrence of {@code element} at or before {@code end}, or -1. An {@code end} beyond
     * the last index counts as the last index.
     * <p>
     * Complexity: O(n).
     *
     * @param element the element to find
     * @param end     the last position to look at
     * @return the last index {@code <= end} of the element, or -1 if absent
     */
    public int lastIndexOf(T element, int end) {
        for (int i = Math.min(end, length() - 1); i >= 0; i--) {
            if (Objects.equals(get(i), element)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * {@link #lastIndexOf(Object)} as an {@link Option}: {@code None} for -1.
     * <p>
     * Complexity: O(n), as {@link #lastIndexOf(Object)}.
     *
     * @param element the element to find
     * @return {@code Some(index)} of its last occurrence, or {@code None}
     */
    public Option<Integer> lastIndexOfOption(T element) {
        return com.guizmaii.zazr.collection.internal.Collections.indexOption(lastIndexOf(element));
    }

    /**
     * {@link #lastIndexOf(Object, int)} as an {@link Option}: {@code None} for -1.
     * <p>
     * Complexity: O(n), as {@link #lastIndexOf(Object, int)}.
     *
     * @param element the element to find
     * @param end     the last position to look at
     * @return {@code Some(index)} of its last occurrence at or before {@code end}, or {@code None}
     */
    public Option<Integer> lastIndexOfOption(T element, int end) {
        return com.guizmaii.zazr.collection.internal.Collections.indexOption(lastIndexOf(element, end));
    }

    /**
     * The last index at which {@code that} occurs as a contiguous slice, or -1.
     * <p>
     * Complexity: O(n * m) for a slice of m elements.
     *
     * @param that the slice to find
     * @return the index of its last occurrence, or -1 (an empty slice occurs at {@code length()})
     * @throws NullPointerException if {@code that} is null
     */
    public int lastIndexOfSlice(Iterable<? extends T> that) {
        return lastIndexOfSlice(that, Integer.MAX_VALUE);
    }

    /**
     * The last index at or before {@code end} at which {@code that} occurs as a contiguous slice, or -1.
     * <p>
     * Complexity: O(n * m) for a slice of m elements.
     *
     * @param that the slice to find
     * @param end  the last position to look at
     * @return the index of its last occurrence at or before {@code end}, or -1
     * @throws NullPointerException if {@code that} is null
     */
    public int lastIndexOfSlice(Iterable<? extends T> that, int end) {
        Objects.requireNonNull(that, "that is null");
        return VectorModule.Slice.lastIndexOfSlice(this, that, end);
    }

    /**
     * {@link #lastIndexOfSlice(Iterable)} as an {@link Option}: {@code None} for -1.
     * <p>
     * Complexity: O(n * m) for a slice of m elements, as {@link #lastIndexOfSlice(Iterable)}.
     *
     * @param that the slice to find
     * @return {@code Some(index)} of its last occurrence, or {@code None}
     * @throws NullPointerException if {@code that} is null
     */
    public Option<Integer> lastIndexOfSliceOption(Iterable<? extends T> that) {
        return com.guizmaii.zazr.collection.internal.Collections.indexOption(lastIndexOfSlice(that));
    }

    /**
     * {@link #lastIndexOfSlice(Iterable, int)} as an {@link Option}: {@code None} for -1.
     * <p>
     * Complexity: O(n * m) for a slice of m elements, as
     * {@link #lastIndexOfSlice(Iterable, int)}.
     *
     * @param that the slice to find
     * @param end  the last position to look at
     * @return {@code Some(index)} of its last occurrence at or before {@code end}, or {@code None}
     * @throws NullPointerException if {@code that} is null
     */
    public Option<Integer> lastIndexOfSliceOption(Iterable<? extends T> that, int end) {
        return com.guizmaii.zazr.collection.internal.Collections.indexOption(lastIndexOfSlice(that, end));
    }

    /**
     * The index of the last element satisfying {@code predicate}, or -1.
     * <p>
     * Complexity: O(n).
     *
     * @param predicate the condition
     * @return the last index of a satisfying element, or -1
     * @throws NullPointerException if {@code predicate} is null
     */
    public int lastIndexWhere(Predicate<? super T> predicate) {
        return lastIndexWhere(predicate, length() - 1);
    }

    /**
     * The index of the last element at or before {@code end} satisfying {@code predicate}, or -1. An {@code end}
     * beyond the last index counts as the last index; a negative one gives -1.
     * <p>
     * Complexity: O(n).
     *
     * @param predicate the condition
     * @param end       the last position to look at
     * @return the last index {@code <= end} of a satisfying element, or -1
     * @throws NullPointerException if {@code predicate} is null
     */
    public int lastIndexWhere(Predicate<? super T> predicate, int end) {
        Objects.requireNonNull(predicate, "predicate is null");
        int i = Math.max(-1, Math.min(end, length() - 1));
        while (i >= 0 && !predicate.test(get(i))) {
            i--;
        }
        return i;
    }

    /**
     * {@link #lastIndexWhere(Predicate)} as an {@link Option}: {@code None} for -1.
     * <p>
     * Complexity: O(n), as {@link #lastIndexWhere(Predicate)}.
     *
     * @param predicate the condition
     * @return {@code Some(index)} of the last satisfying element, or {@code None}
     * @throws NullPointerException if {@code predicate} is null
     */
    public Option<Integer> lastIndexWhereOption(Predicate<? super T> predicate) {
        return com.guizmaii.zazr.collection.internal.Collections.indexOption(lastIndexWhere(predicate));
    }

    /**
     * {@link #lastIndexWhere(Predicate, int)} as an {@link Option}: {@code None} for -1.
     * <p>
     * Complexity: O(n), as {@link #lastIndexWhere(Predicate, int)}.
     *
     * @param predicate the condition
     * @param end       the last position to look at
     * @return {@code Some(index)} of the last satisfying element at or before {@code end}, or {@code None}
     * @throws NullPointerException if {@code predicate} is null
     */
    public Option<Integer> lastIndexWhereOption(Predicate<? super T> predicate, int end) {
        return com.guizmaii.zazr.collection.internal.Collections.indexOption(lastIndexWhere(predicate, end));
    }

    /**
     * Returns the number of elements in this Vector.
     * <p>
     * Equivalent to {@link #size()}.
     * <p>
     * Complexity: O(1): the length is stored.
     *
     * @return the number of elements
     */
    public int length() { return trie.length(); }

    public <U extends @Nullable Object> Vector<U> map(Function<? super T, ? extends U> mapper) {
        Objects.requireNonNull(mapper, "mapper is null");
        if (trie.hasObjectLeaves()) {
            // measured (3 forks, two independent runs): on Object[] receivers the flat-array map is on par with the
            // builder at 100 000 elements and 1.2x faster at 1 000, so it keeps the trie's path
            return ofAll(trie.map(mapper));
        }
        // a primitive-backed receiver (Vector.range, ofAll(int[])): the builder is 1.5x faster at 100 000 (351 -> 227 µs)
        final Builder<U> builder = newBuilder(length());
        trie.<Object> visit((index, leaf, start, end) -> {
            builder.addMapped(trie.type, leaf, start, end, mapper);
            return index + end - start;
        });
        return builder.result();
    }

    public <U extends @Nullable Object> Vector<U> collect(Function<? super T, ? extends Option<? extends U>> mapper) {
        Objects.requireNonNull(mapper, "mapper is null");
        // one pass over the leaves straight into the builder, like flatMap: no intermediate collection
        final Builder<U> builder = newBuilder();
        trie.<Object> visit((index, leaf, start, end) -> {
            for (int i = start; i < end; i++) {
                final Option<? extends U> collected = Objects.requireNonNull(mapper.apply(trie.type.getAt(leaf, i)), "Vector.collect: mapper returned null");
                if (collected.isDefined()) {
                    builder.add(collected.get());
                }
            }
            return index + end - start;
        });
        return builder.result();
    }

    public <U extends @Nullable Object> Vector<U> as(U value) {
        return map(ignored -> value);
    }

    /**
     * This Vector if it is not empty, otherwise the elements of {@code other}.
     * <p>
     * Complexity: O(1) when this Vector is not empty or {@code other} is a Vector; otherwise O(m) for m elements of
     * {@code other}, which are copied.
     *
     * @param other the elements to use when this Vector is empty
     * @return this Vector, or a Vector of the elements of {@code other}
     * @throws NullPointerException if this Vector is empty and {@code other} is null
     */
    public Vector<T> orElse(Iterable<? extends T> other) {
        return isEmpty() ? ofAll(other) : this;
    }

    /**
     * This Vector if it is not empty, otherwise the elements {@code supplier} gives, which is called only then.
     * <p>
     * Complexity: O(1) when this Vector is not empty or the supplied iterable is a Vector; otherwise O(m) for the m
     * supplied elements, which are copied.
     *
     * @param supplier gives the elements to use when this Vector is empty
     * @return this Vector, or a Vector of the supplied elements
     * @throws NullPointerException if this Vector is empty and {@code supplier} is null or gives null
     */
    public Vector<T> orElse(Supplier<? extends Iterable<? extends T>> supplier) {
        return isEmpty() ? ofAll(supplier.get()) : this;
    }

    /**
     * Appends copies of {@code element} until the Vector has {@code length} elements.
     * <p>
     * Complexity: O(k) for the k elements appended. On a Vector of primitive values, a value of another class first
     * converts every element: O(n), paid again at each such write on the same Vector (see the class documentation).
     *
     * @param length  the target length
     * @param element the padding element
     * @return this Vector if it already has {@code length} or more elements, otherwise a new one padded to it
     * @throws NullPointerException if {@code element} is null and padding is needed
     */
    public Vector<T> padTo(int length, T element) {
        final int actualLength = length();
        return (length <= actualLength)
               ? this
               : appendAll(Iterator.continually(element)
                .take(length - actualLength));
    }

    /**
     * Prepends copies of {@code element} until the Vector has {@code length} elements.
     * <p>
     * Complexity: O(k) for the k elements prepended. On a Vector of primitive values, a value of another class first
     * converts every element: O(n), paid again at each such write on the same Vector (see the class documentation).
     *
     * @param length  the target length
     * @param element the padding element
     * @return this Vector if it already has {@code length} or more elements, otherwise a new one padded to it
     * @throws NullPointerException if {@code element} is null and padding is needed
     */
    public Vector<T> leftPadTo(int length, T element) {
        if (length <= length()) {
            return this;
        } else {
            final Iterator<T> prefix = Iterator.continually(element).take(length - length());
            return prependAll(prefix);
        }
    }

    /**
     * Replaces the {@code replaced} elements from {@code from} on by the elements of {@code that}. A negative
     * {@code from} or {@code replaced} counts as 0; a {@code from} beyond the end appends.
     * <p>
     * Complexity: O(n + m) for m elements of {@code that}: the elements before {@code from} are shared, and only
     * {@code that} and the elements after the replaced ones are copied, so a patch near the end is O(m).
     *
     * @param from     the first position to replace
     * @param that     the replacement elements
     * @param replaced how many elements to replace
     * @return a new Vector with the slice replaced
     * @throws NullPointerException if {@code that} or one of its elements is null
     */
    public Vector<T> patch(int from, Iterable<? extends T> that, int replaced) {
        from = Math.max(from, 0);
        replaced = Math.max(replaced, 0);

        // the end of the replaced range, saturated: from + replaced can overflow an int

        final int end = (int) Math.min((long) from + replaced, Integer.MAX_VALUE);

        return take(from).appendAll(that).appendAll(drop(end));
    }

    /**
     * Splits the elements into those that satisfy {@code predicate} and those that do not, each in order.
     * <p>
     * Complexity: O(n), one pass.
     *
     * @param predicate the condition
     * @return the elements that satisfy it, and those that do not
     * @throws NullPointerException if {@code predicate} is null
     */
    public Tuple2<Vector<T>, Vector<T>> partition(Predicate<? super T> predicate) {
        Objects.requireNonNull(predicate, "predicate is null");
        final ArrayList<T> left = new ArrayList<>(), right = new ArrayList<>();
        for (int i = 0; i < length(); i++) {
            final T t = get(i);
            (predicate.test(t) ? left : right).add(t);
        }
        return Tuple.of(ofAll(left), ofAll(right));
    }

    /**
     * Splits the elements into a left and a right side according to the {@link Either} {@code f} returns for each: the
     * generalisation of {@link #partition(Predicate)}. Two builders, one pass, no intermediate list.
     * <p>
     * Complexity: O(n), one pass.
     *
     * @param f   Classifies an element
     * @param <L> Component type of the left side
     * @param <R> Component type of the right side
     * @return the left values and the right values, each in order
     * @throws NullPointerException if {@code f} is null or returns null
     */
    public <L extends @Nullable Object, R extends @Nullable Object> Tuple2<Vector<L>, Vector<R>> partitionMap(Function<? super T, ? extends Either<? extends L, ? extends R>> f) {
        Objects.requireNonNull(f, "f is null");
        final Builder<L> lefts = newBuilder();
        final Builder<R> rights = newBuilder();
        for (T element : this) {
            switch (Objects.requireNonNull(f.apply(element), "Vector.partitionMap: f returned null")) {
                case Either.Left(var left) -> lefts.add(left);
                case Either.Right(var right) -> rights.add(right);
            }
        }
        return Tuple.of(lefts.result(), rights.result());
    }

    public Vector<T> tap(Consumer<? super T> action) {
        Objects.requireNonNull(action, "action is null");
        forEach(action);
        return this;
    }

    /**
     * All distinct permutations of the elements, in the order the distinct elements first occur.
     * <p>
     * Complexity: O(n! * n^2) in the worst case (all elements distinct): there are n! permutations of n elements, and
     * each is rebuilt once per element, at every level of the recursion.
     *
     * @return the permutations; none for the empty Vector
     */
    public Vector<Vector<T>> permutations() {
        if (isEmpty()) {
            return empty();
        } else if (length() == 1) {
            return of(this);
        } else {
            Vector<Vector<T>> results = empty();
            for (T t : distinct()) {
                for (Vector<T> ts : remove(t).permutations()) {
                    results = results.append(of(t).appendAll(ts));
                }
            }
            return results;
        }
    }

    /**
     * The length of the longest prefix whose elements all satisfy {@code predicate}: {@code segmentLength(predicate, 0)}.
     * <p>
     * Complexity: O(k) for a prefix of k elements.
     *
     * @param predicate the condition
     * @return the prefix length
     * @throws NullPointerException if {@code predicate} is null
     */
    public int prefixLength(Predicate<? super T> predicate) {
        return segmentLength(predicate, 0);
    }

    /**
     * Prepends an element.
     * <p>
     * Complexity: effectively O(1): copies a few small arrays of 32 elements, not the Vector. On a Vector of primitive
     * values, a value of another class first converts every element: O(n), paid again at each such write on the same
     * Vector (see the class documentation).
     *
     * @param element the element to prepend
     * @return a new Vector starting with {@code element}
     * @throws NullPointerException if {@code element} is null
     */
    public Vector<T> prepend(T element) { return prependAll(com.guizmaii.zazr.collection.List.of(element)); }

    /**
     * Prepends all elements of the given iterable, keeping their order.
     * <p>
     * Complexity: O(m) for m prepended elements, even when this Vector is much shorter than the argument; the elements
     * of this Vector are shared, not copied. O(1) when this Vector is empty and {@code iterable} is a Vector, which is
     * returned as is. On a Vector of primitive values, a value of another class first converts every element: O(n),
     * paid again at each such write on the same Vector (see the class documentation).
     *
     * @param iterable the elements to prepend
     * @return a new Vector starting with the given elements, or this Vector if there are none
     * @throws NullPointerException if {@code iterable} or one of its elements is null
     */
    public Vector<T> prependAll(Iterable<? extends T> iterable) {
        Objects.requireNonNull(iterable, "iterable is null");
        if (isEmpty()) {
            return ofAll(iterable);
        }
        if (!com.guizmaii.zazr.collection.internal.Collections.isTraversableAgain(iterable)) {
            // a one-shot source is read exactly once: built first, which also answers whether there is anything to prepend
            final Vector<T> elements = ofAll(iterable);
            return elements.isEmpty() ? this : prependAll(elements);
        }
        if (com.guizmaii.zazr.collection.internal.Collections.isEmpty(iterable)) {
            return this;
        }
        return new Vector<>(trie.prependAll(iterable));
    }

    /**
     * Removes the first occurrence of {@code element}.
     * <p>
     * Complexity: O(n).
     *
     * @param element the element to remove
     * @return a new Vector without that occurrence, or this Vector if the element is absent
     */
    public Vector<T> remove(T element) {
        for (int i = 0; i < length(); i++) {
            if (Objects.equals(get(i), element)) {
                return removeAt(i);
            }
        }
        return this;
    }

    /**
     * Removes the first element satisfying {@code predicate}.
     * <p>
     * Complexity: O(n).
     *
     * @param predicate the condition
     * @return a new Vector without that element, or this Vector if none satisfies the predicate
     * @throws NullPointerException if {@code predicate} is null
     */
    public Vector<T> removeFirst(Predicate<T> predicate) {
        Objects.requireNonNull(predicate, "predicate is null");
        for (int i = 0; i < length(); i++) {
            if (predicate.test(get(i))) {
                return removeAt(i);
            }
        }
        return this;
    }

    /**
     * Removes the last element satisfying {@code predicate}.
     * <p>
     * Complexity: O(n).
     *
     * @param predicate the condition
     * @return a new Vector without that element, or this Vector if none satisfies the predicate
     * @throws NullPointerException if {@code predicate} is null
     */
    public Vector<T> removeLast(Predicate<T> predicate) {
        Objects.requireNonNull(predicate, "predicate is null");
        for (int i = length() - 1; i >= 0; i--) {
            if (predicate.test(get(i))) {
                return removeAt(i);
            }
        }
        return this;
    }

    /**
     * Removes the element at {@code index}; the elements after it shift left by one.
     * <p>
     * Complexity: O(min(i, n - i)): the elements on the shorter side of i are copied, those on the longer side are
     * shared.
     *
     * @param index a position, {@code 0 <= index < length()}
     * @return a new Vector without the element at {@code index}
     * @throws IndexOutOfBoundsException if {@code index} is out of range
     */
    public Vector<T> removeAt(int index) {
        if (isValid(index)) {
            final Vector<T> begin = take(index);
            final Vector<T> end = drop(index + 1);
            return (begin.size() > end.size())
                   ? begin.appendAll(end)
                   : end.prependAll(begin);
        } else {
            throw new IndexOutOfBoundsException("removeAt(" + index + ")");
        }
    }

    /**
     * Removes every occurrence of {@code element}.
     * <p>
     * Complexity: O(n).
     *
     * @param element the element to remove
     * @return a new Vector without it, or this Vector if it is absent
     */
    public Vector<T> removeAll(T element) {
        return com.guizmaii.zazr.collection.internal.Collections.removeAll(this, element, kept -> filter(kept));
    }

    /**
     * Removes every occurrence of every given element.
     * <p>
     * Complexity: O(n + m) for an argument of m elements: they are hashed once, then one filter pass.
     *
     * @param elements the elements to remove
     * @return a new Vector without them, or this Vector if none is present
     * @throws NullPointerException if {@code elements} is null
     */
    public Vector<T> removeAll(Iterable<? extends T> elements) {
        return com.guizmaii.zazr.collection.internal.Collections.removeAll(this, elements, kept -> filter(kept));
    }

    /**
     * Removes every element satisfying {@code predicate}: {@link #reject(Predicate)}.
     * <p>
     * Complexity: O(n).
     *
     * @param predicate the condition
     * @return a new Vector of the elements not satisfying it, or this Vector if none does
     * @throws NullPointerException if {@code predicate} is null
     * @deprecated use {@link #reject(Predicate)}
     */
    @Deprecated
    public Vector<T> removeAll(Predicate<? super T> predicate) {
        Objects.requireNonNull(predicate, "predicate is null");
        return reject(predicate);
    }

    /**
     * Replaces the first occurrence of {@code currentElement} with {@code newElement}, if it exists.
     * <p>
     * Complexity: O(n) to find the element, then one effectively O(1) {@code update}.
     *
     * @param currentElement the element to be replaced
     * @param newElement     the replacement element
     * @return a new Vector with the first occurrence of {@code currentElement} replaced by {@code newElement}
     */
    public Vector<T> replace(T currentElement, T newElement) {
        return indexOfOption(currentElement)
                .map(i -> update(i, newElement))
                .getOrElse(this);
    }

    /**
     * Replaces all occurrences of {@code currentElement} with {@code newElement}.
     * <p>
     * Complexity: O(n) plus one effectively O(1) {@code update} per occurrence.
     *
     * @param currentElement the element to be replaced
     * @param newElement     the replacement element
     * @return a new Vector with all occurrences of {@code currentElement} replaced by {@code newElement}
     */
    public Vector<T> replaceAll(T currentElement, T newElement) {
        Vector<T> result = this;
        int index = 0;
        for (T value : this) {
            if (Objects.equals(value, currentElement)) {
                result = result.update(index, newElement);
            }
            index++;
        }
        return result;
    }

    /**
     * Retains only the elements from this Vector that are contained in the given {@code elements}.
     * <p>
     * Complexity: O(n + m) for an argument of m elements: they are hashed once, then one filter pass.
     *
     * @param elements the elements to keep
     * @return a new Vector containing only the elements present in {@code elements}, in their original order
     * @throws NullPointerException if {@code elements} is null
     */
    public Vector<T> retainAll(Iterable<? extends T> elements) {
        return com.guizmaii.zazr.collection.internal.Collections.retainAll(this, elements, kept -> filter(kept));
    }

    /**
     * The elements in reverse order.
     * <p>
     * Complexity: O(n).
     *
     * @return a new Vector, or this Vector if it has fewer than two elements
     */
    public Vector<T> reverse() {
        return (length() <= 1) ? this : ofAll(reverseIterator());
    }

    /**
     * An iterator over the elements from the last to the first, without copying.
     * <p>
     * Complexity: O(1) to create; each step is effectively O(1).
     *
     * @return the reverse iterator
     */
    private Iterator<T> reverseIterator() {
        return new AbstractIterator<T>() {
            private int i = Vector.this.length();

            @Override
            public boolean hasNext() {
                return i > 0;
            }

            @Override
            public T getNext() {
                return Vector.this.get(--i);
            }
        };
    }

    /**
     * Rotates the elements {@code n} positions to the left: {@code Vector(1, 2, 3, 4, 5).rotateLeft(2)} is
     * {@code Vector(3, 4, 5, 1, 2)}. A negative {@code n} rotates right; {@code n} is taken modulo the length.
     * <p>
     * Complexity: O(k), where k is the distance modulo the size: the first k elements are copied to the end, the
     * others are shared. A negative distance can cost O(n): {@code rotateLeft(-1)} copies all the elements but one.
     *
     * @param n the distance
     * @return the rotated Vector, or this Vector if the rotation is a multiple of the length
     */
    public Vector<T> rotateLeft(int n) {
        if (isEmpty()) {
            return this;
        }
        final int k = Math.floorMod(n, length());
        return (k == 0) ? this : drop(k).appendAll(take(k));
    }

    /**
     * Rotates the elements {@code n} positions to the right: {@code Vector(1, 2, 3, 4, 5).rotateRight(2)} is
     * {@code Vector(4, 5, 1, 2, 3)}. A negative {@code n} rotates left; {@code n} is taken modulo the length.
     * <p>
     * Complexity: O(n - k), where k is the distance modulo the size: the last k elements are shared and the n - k
     * elements before them are copied after them, so {@code rotateRight(1)} is O(n).
     *
     * @param n the distance
     * @return the rotated Vector, or this Vector if the rotation is a multiple of the length
     */
    public Vector<T> rotateRight(int n) {
        if (isEmpty()) {
            return this;
        }
        final int k = Math.floorMod(n, length());
        return (k == 0) ? this : takeRight(k).appendAll(dropRight(k));
    }

    /**
     * Computes a prefix scan of the elements of this Vector.
     * <p>
     * The neutral element {@code zero} may be applied more than once.
     * <p>
     * Complexity: O(n).
     *
     * @param zero      the neutral element for the operator
     * @param operation an associative binary operator
     * @return a new Vector containing the prefix scan of the elements
     * @throws NullPointerException if {@code operation} is null
     */
    public Vector<T> scan(T zero, BiFunction<? super T, ? super T, ? extends T> operation) {
        return scanLeft(zero, operation);
    }

    /**
     * Produces a collection containing cumulative results of applying the operator from left to right.
     * <p>
     * Complexity: O(n).
     *
     * @param <U>       the type of the resulting elements
     * @param zero      the initial value
     * @param operation a binary operator applied to the intermediate result and each element
     * @return a new Vector containing the cumulative results
     * @throws NullPointerException if {@code operation} is null
     */
    public <U extends @Nullable Object> Vector<U> scanLeft(U zero, BiFunction<? super U, ? super T, ? extends U> operation) {
        return com.guizmaii.zazr.collection.internal.Collections.scanLeft(this, zero, operation, Iterator::toVector);
    }

    /**
     * Produces a collection containing cumulative results of applying the operator from right to left.
     * <p>
     * The head of the result is the last cumulative result.
     * <p>
     * Complexity: O(n).
     *
     * @param <U>       the type of the resulting elements
     * @param zero      the initial value
     * @param operation a binary operator applied to each element and the intermediate result
     * @return a new Vector containing the cumulative results
     * @throws NullPointerException if {@code operation} is null
     */
    public <U extends @Nullable Object> Vector<U> scanRight(U zero, BiFunction<? super T, ? super U, ? extends U> operation) {
        return com.guizmaii.zazr.collection.internal.Collections.scanRight(this, zero, operation, Iterator::toVector);
    }

    /**
     * Binary search for {@code element} in this Vector, which must be sorted in natural order (otherwise the result
     * is undefined).
     * <p>
     * Complexity: O(log n) comparisons, each an effectively O(1) access.
     *
     * @param element the element to find
     * @return its index if present, otherwise {@code -(insertion point) - 1}, the insertion point being the index
     *         at which it would be inserted; the result is {@code >= 0} exactly when the element is present
     * @throws ClassCastException if the elements are not {@link Comparable}
     */
    @SuppressWarnings("unchecked")
    public int search(T element) {
        return VectorModule.Search.binarySearch(this, midIndex -> ((Comparable<? super T>) get(midIndex)).compareTo(element));
    }

    /**
     * Binary search for {@code element} in this Vector, which must be sorted by {@code comparator} (otherwise the
     * result is undefined).
     * <p>
     * Complexity: O(log n) comparisons, each an effectively O(1) access.
     *
     * @param element    the element to find
     * @param comparator the order of this Vector
     * @return its index if present, otherwise {@code -(insertion point) - 1}, the insertion point being the index
     *         at which it would be inserted; the result is {@code >= 0} exactly when the element is present
     * @throws NullPointerException if {@code comparator} is null
     */
    public int search(T element, Comparator<? super T> comparator) {
        Objects.requireNonNull(comparator, "comparator is null");
        return VectorModule.Search.binarySearch(this, midIndex -> comparator.compare(get(midIndex), element));
    }

    /**
     * The length of the longest run of elements starting at {@code from} that all satisfy {@code predicate}. A
     * negative {@code from} counts as 0; a {@code from} at or beyond the end gives 0.
     * <p>
     * Complexity: O(k) for a run of k elements.
     *
     * @param predicate the condition
     * @param from      the first position of the run
     * @return the run length
     * @throws NullPointerException if {@code predicate} is null
     */
    public int segmentLength(Predicate<? super T> predicate, int from) {
        Objects.requireNonNull(predicate, "predicate is null");
        final int len = length();
        final int start = Math.max(from, 0);
        int i = start;
        while (i < len && predicate.test(get(i))) {
            i++;
        }
        return i - start;
    }

    /**
     * The elements in a uniformly random order.
     * <p>
     * Complexity: O(n).
     *
     * @return a new Vector, or this Vector if it has fewer than two elements
     */
    public Vector<T> shuffle() {
        return com.guizmaii.zazr.collection.internal.Collections.shuffle(this, Vector::ofAll);
    }

    /**
     * The elements from {@code beginIndex} (inclusive) to {@code endIndex} (exclusive). Out-of-range indices are
     * clamped, and an empty or reversed range gives the empty Vector: {@code Vector(1, 2).slice(-10, 10)} is the
     * whole Vector, {@code slice(1, 0)} is empty. {@link #subSequence(int, int)} throws instead of clamping.
     * <p>
     * Complexity: effectively O(1): the result shares its elements with this Vector; only a few small arrays at the
     * cut are copied.
     *
     * @param beginIndex the first position (inclusive)
     * @param endIndex   the last position (exclusive)
     * @return the slice; this Vector when it covers everything
     */
    public Vector<T> slice(int beginIndex, int endIndex) {
        if ((beginIndex >= endIndex) || (beginIndex >= size()) || isEmpty()) {
            return empty();
        } else if ((beginIndex <= 0) && (endIndex >= length())) {
            return this;
        } else {
            return take(endIndex).drop(beginIndex);
        }
    }

    /**
     * The elements sorted in natural order (a stable sort).
     * <p>
     * Complexity: O(n log n) comparisons; the elements are copied to an array and sorted there.
     *
     * @return a new sorted Vector, or this Vector if it is empty
     * @throws ClassCastException if the elements are not {@link Comparable}
     */
    public Vector<T> sorted() {
        if (isEmpty()) {
            return this;
        } else {
            @SuppressWarnings("unchecked")
            final T[] list = (T[]) toArray();
            Arrays.sort(list);
            return Vector.of(list);
        }
    }

    /**
     * The elements sorted by {@code comparator} (a stable sort).
     * <p>
     * Complexity: O(n log n) comparisons; the elements are copied to an array and sorted there.
     *
     * @param comparator the order
     * @return a new sorted Vector, or this Vector if it is empty
     * @throws NullPointerException if {@code comparator} is null
     */
    public Vector<T> sorted(Comparator<? super T> comparator) {
        Objects.requireNonNull(comparator, "comparator is null");
        if (isEmpty()) {
            return this;
        }
        @SuppressWarnings("unchecked")
        final T[] array = (T[]) toArray();
        Arrays.sort(array, comparator);
        return Vector.of(array);
    }

    /**
     * The elements sorted by the natural order of the key {@code mapper} computes (a stable sort).
     * <p>
     * Complexity: O(n log n) comparisons; {@code mapper} is called again for both elements at every comparison.
     *
     * @param mapper computes the sort key
     * @param <U>    the key type
     * @return a new sorted Vector, or this Vector if it is empty
     * @throws NullPointerException if {@code mapper} is null
     */
    public <U extends Comparable<? super U>> Vector<T> sortBy(Function<? super T, ? extends U> mapper) {
        return sortBy(U::compareTo, mapper);
    }

    /**
     * The elements sorted by {@code comparator} applied to the key {@code mapper} computes (a stable sort).
     * <p>
     * Complexity: O(n log n) comparisons; {@code mapper} is called again for both elements at every comparison.
     *
     * @param comparator the order of the keys
     * @param mapper     computes the sort key
     * @param <U>        the key type
     * @return a new sorted Vector, or this Vector if it is empty
     * @throws NullPointerException if {@code comparator} or {@code mapper} is null
     */
    public <U extends @Nullable Object> Vector<T> sortBy(Comparator<? super U> comparator, Function<? super T, ? extends U> mapper) {
        Objects.requireNonNull(comparator, "comparator is null");
        Objects.requireNonNull(mapper, "mapper is null");
        return sorted((e1, e2) -> comparator.compare(mapper.apply(e1), mapper.apply(e2)));
    }

    /**
     * Splits this {@code Vector} into a prefix and remainder according to the given {@code predicate}.
     * <p>
     * The first element of the returned {@code Tuple} is the longest prefix of elements satisfying {@code predicate},
     * and the second element is the remaining elements.
     * <p>
     * Complexity: O(k) for k elements before the split, then an effectively O(1) split.
     *
     * @param predicate a predicate used to determine the prefix
     * @return a {@code Tuple} containing the prefix and remainder
     * @throws NullPointerException if {@code predicate} is null
     */
    public Tuple2<Vector<T>, Vector<T>> span(Predicate<? super T> predicate) {
        Objects.requireNonNull(predicate, "predicate is null");
        return Tuple.of(takeWhile(predicate), dropWhile(predicate));
    }

    /**
     * Splits at {@code n}: {@code (take(n), drop(n))}.
     * <p>
     * Complexity: effectively O(1): one {@link #take(int)} and one {@link #drop(int)}; both parts share their
     * elements with this Vector.
     *
     * @param n the split position; clamped to {@code [0, length()]}
     * @return the first {@code n} elements and the rest
     */
    public Tuple2<Vector<T>, Vector<T>> splitAt(int n) {
        return Tuple.of(take(n), drop(n));
    }

    /**
     * Splits before the first element satisfying {@code predicate}; that element starts the second part.
     * <p>
     * Complexity: O(k) for k elements before the split, then an effectively O(1) split.
     *
     * @param predicate the condition
     * @return the elements before the first match and the rest; everything and the empty Vector if none matches
     * @throws NullPointerException if {@code predicate} is null
     */
    public Tuple2<Vector<T>, Vector<T>> splitAt(Predicate<? super T> predicate) {
        Objects.requireNonNull(predicate, "predicate is null");
        final Vector<T> init = takeWhile(predicate.negate());
        return Tuple.of(init, drop(init.size()));
    }

    /**
     * Splits after the first element satisfying {@code predicate}; that element ends the first part.
     * <p>
     * Complexity: O(k) for k elements up to the split, then an effectively O(1) split.
     *
     * @param predicate the condition
     * @return the elements up to and including the first match and the rest; everything and the empty Vector if
     *         none matches
     * @throws NullPointerException if {@code predicate} is null
     */
    public Tuple2<Vector<T>, Vector<T>> splitAtInclusive(Predicate<? super T> predicate) {
        Objects.requireNonNull(predicate, "predicate is null");
        for (int i = 0; i < length(); i++) {
            final T value = get(i);
            if (predicate.test(value)) {
                return (i == (length() - 1)) ? Tuple.of(this, empty())
                                             : Tuple.of(take(i + 1), drop(i + 1));
            }
        }
        return Tuple.of(this, empty());
    }

    /**
     * Whether this Vector starts with {@code that}: {@code startsWith(that, 0)}.
     * <p>
     * Complexity: O(m) for m elements of {@code that}.
     *
     * @param that the prefix to test
     * @return true if the first {@code m} elements equal {@code that} (an empty {@code that} is always a prefix)
     * @throws NullPointerException if {@code that} is null
     */
    public boolean startsWith(Iterable<? extends T> that) {
        return startsWith(that, 0);
    }

    /**
     * Whether the elements from {@code offset} on start with {@code that}. A Vector argument is compared by index;
     * any other iterable is walked once, so a one-shot iterator is accepted.
     * <p>
     * Complexity: O(m) for m elements of {@code that}.
     *
     * @param that   the prefix to test
     * @param offset the position in this Vector at which the prefix should start
     * @return false if {@code offset} is negative; otherwise true if {@code that} equals the {@code m} elements from
     *         {@code offset} on (an empty {@code that} is always a prefix, even beyond the end)
     * @throws NullPointerException if {@code that} is null
     */
    public boolean startsWith(Iterable<? extends T> that, int offset) {
        Objects.requireNonNull(that, "that is null");
        if (offset < 0) {
            return false;
        }
        final int thisLength = length();
        if (that instanceof Vector<?> vector) {
            @SuppressWarnings("unchecked")
            final Vector<? extends T> thatVector = (Vector<? extends T>) vector;
            final int thatLength = thatVector.length();
            if (thatLength == 0) {
                return true; // an empty prefix starts anywhere, even past the end
            }
            if (thatLength > thisLength - offset) {
                return false;
            }
            for (int i = offset, j = 0; j < thatLength; i++, j++) {
                if (!Objects.equals(get(i), thatVector.get(j))) {
                    return false;
                }
            }
            return true;
        }
        int i = offset;
        final java.util.Iterator<? extends T> thatElements = that.iterator();
        while (i < thisLength && thatElements.hasNext()) {
            if (!Objects.equals(get(i), thatElements.next())) {
                return false;
            }
            i++;
        }
        return !thatElements.hasNext();
    }

    /**
     * The elements from {@code beginIndex} on. Unlike {@link #drop(int)}, an out-of-range index throws.
     * <p>
     * Complexity: effectively O(1), as {@link #drop(int)}.
     *
     * @param beginIndex the first position, {@code 0 <= beginIndex <= length()}
     * @return the elements from {@code beginIndex} on; this Vector when it is 0
     * @throws IndexOutOfBoundsException if {@code beginIndex} is out of range
     */
    public Vector<T> subSequence(int beginIndex) {
        if ((beginIndex >= 0) && (beginIndex <= length())) {
            return drop(beginIndex);
        } else {
            throw new IndexOutOfBoundsException("subSequence(" + beginIndex + ")");
        }
    }

    /**
     * The elements from {@code beginIndex} (inclusive) to {@code endIndex} (exclusive). Unlike
     * {@link #slice(int, int)}, out-of-range or reversed indices throw.
     * <p>
     * Complexity: effectively O(1), as {@link #slice(int, int)}.
     *
     * @param beginIndex the first position (inclusive), {@code >= 0}
     * @param endIndex   the last position (exclusive), {@code <= length()}
     * @return the elements in the range; this Vector when it covers everything
     * @throws IndexOutOfBoundsException if {@code beginIndex < 0} or {@code endIndex > length()}
     * @throws IllegalArgumentException  if {@code beginIndex > endIndex}
     */
    public Vector<T> subSequence(int beginIndex, int endIndex) {
        Collections.subSequenceRangeCheck(beginIndex, endIndex, length());
        return slice(beginIndex, endIndex);
    }

    /**
     * Returns a new {@code Vector} without its first element.
     * <p>
     * Complexity: effectively O(1): the result shares its elements with this Vector; only a few small arrays at the
     * cut are copied.
     *
     * @return a new {@code Vector} containing all elements except the first
     * @throws UnsupportedOperationException if this {@code Vector} is empty
     */
    public Vector<T> tail() {
        if (nonEmpty()) {
            return drop(1);
        } else {
            throw new UnsupportedOperationException("tail of empty Vector");
        }
    }

    /**
     * Returns a new {@code Vector} without its first element as an {@code Option}.
     * <p>
     * Complexity: effectively O(1), as {@link #tail()}.
     *
     * @return {@code Some(traversable)} if non-empty, otherwise {@code None}
     */
    public Option<Vector<T>> tailOption() { return isEmpty() ? Option.none() : Option.some(tail()); }

    /**
     * Returns the first {@code n} elements of this {@code Vector}, or all elements if {@code n} exceeds the length.
     * <p>
     * If {@code n < 0}, an empty instance is returned. If {@code n > length()}, the full instance is returned.
     * <p>
     * Complexity: effectively O(1): the result shares its elements with this Vector; only a few small arrays at the
     * cut are copied.
     *
     * @param n the number of elements to take
     * @return a new {@code Vector} containing the first {@code n} elements
     */
    public Vector<T> take(int n) {
        return wrap(trie.take(n));
    }

    /**
     * Takes elements from this {@code Vector} until the given predicate holds for an element.
     * <p>
     * Equivalent to {@code takeWhile(predicate.negate())}, but useful when using method references
     * that cannot be negated directly.
     * <p>
     * Complexity: O(k) for k taken elements, then one effectively O(1) {@code take}.
     *
     * @param predicate a condition tested sequentially on the elements
     * @return a new {@code Vector} containing all elements before the first one that satisfies the predicate
     * @throws NullPointerException if {@code predicate} is null
     */
    public Vector<T> takeUntil(Predicate<? super T> predicate) {
        Objects.requireNonNull(predicate, "predicate is null");
        final int length = length();
        for (int i = 0; i < length; i++) {
            if (predicate.test(get(i))) {
                return take(i);
            }
        }
        return this;
    }

    /**
     * Takes elements from this {@code Vector} while the given predicate holds.
     * <p>
     * Complexity: O(k) for k taken elements, then one effectively O(1) {@code take}.
     *
     * @param predicate a condition tested sequentially on the elements
     * @return a new {@code Vector} containing all elements up to (but not including) the first one
     *         that does not satisfy the predicate
     * @throws NullPointerException if {@code predicate} is null
     */
    public Vector<T> takeWhile(Predicate<? super T> predicate) {
        Objects.requireNonNull(predicate, "predicate is null");
        return takeUntil(predicate.negate());
    }

    /**
     * Returns the last {@code n} elements of this {@code Vector}, or all elements if {@code n} exceeds the length.
     * <p>
     * If {@code n < 0}, an empty instance is returned. If {@code n > length()}, the full instance is returned.
     * <p>
     * Complexity: effectively O(1): the result shares its elements with this Vector; only a few small arrays at the
     * cut are copied.
     *
     * @param n the number of elements to take from the end
     * @return a new {@code Vector} containing the last {@code n} elements
     */
    public Vector<T> takeRight(int n) {
        // n <= 0 first: length() - n overflows for Integer.MIN_VALUE
        return n <= 0 ? empty() : drop(length() - n);
    }

    /**
     * Takes elements from the end until one satisfies {@code predicate}; that element is excluded.
     * <p>
     * Complexity: O(k) for k taken elements, then one effectively O(1) {@code drop}.
     *
     * @param predicate tested from the last element backwards
     * @return the elements after the last one satisfying {@code predicate}; this Vector if none does
     * @throws NullPointerException if {@code predicate} is null
     */
    public Vector<T> takeRightUntil(Predicate<? super T> predicate) {
        Objects.requireNonNull(predicate, "predicate is null");
        for (int i = length() - 1; i >= 0; i--) {
            if (predicate.test(get(i))) {
                return drop(i + 1);
            }
        }
        return this;
    }

    /**
     * Takes elements from the end while they satisfy {@code predicate}: {@code takeRightUntil(predicate.negate())}.
     * <p>
     * Complexity: O(k) for k taken elements, then one effectively O(1) {@code drop}.
     *
     * @param predicate tested from the last element backwards
     * @return the elements after the last one not satisfying {@code predicate}; this Vector if all do
     * @throws NullPointerException if {@code predicate} is null
     */
    public Vector<T> takeRightWhile(Predicate<? super T> predicate) {
        Objects.requireNonNull(predicate, "predicate is null");
        return takeRightUntil(predicate.negate());
    }

    /**
     * Splits every element into two with {@code unzipper} and collects the halves: two builders, one pass.
     * <p>
     * Complexity: O(n).
     *
     * @param unzipper splits an element
     * @param <T1>     the type of the first halves
     * @param <T2>     the type of the second halves
     * @return the first halves and the second halves, each in order
     * @throws NullPointerException if {@code unzipper} is null, returns null, or returns a tuple with a null component
     */
    public <T1 extends @Nullable Object, T2 extends @Nullable Object> Tuple2<Vector<T1>, Vector<T2>> unzip(Function<? super T, Tuple2<? extends T1, ? extends T2>> unzipper) {
        Objects.requireNonNull(unzipper, "unzipper is null");
        final Builder<T1> xs = newBuilder(length());
        final Builder<T2> ys = newBuilder(length());
        for (T element : this) {
            final Tuple2<? extends T1, ? extends T2> t = unzipper.apply(element);
            xs.add(t._1());
            ys.add(t._2());
        }
        return Tuple.of(xs.result(), ys.result());
    }

    /**
     * Splits every element into three with {@code unzipper} and collects the thirds: three builders, one pass.
     * <p>
     * Complexity: O(n).
     *
     * @param unzipper splits an element
     * @param <T1>     the type of the first thirds
     * @param <T2>     the type of the second thirds
     * @param <T3>     the type of the third thirds
     * @return the three Vectors of thirds, each in order
     * @throws NullPointerException if {@code unzipper} is null, returns null, or returns a tuple with a null component
     */
    public <T1 extends @Nullable Object, T2 extends @Nullable Object, T3 extends @Nullable Object> Tuple3<Vector<T1>, Vector<T2>, Vector<T3>> unzip3(Function<? super T, Tuple3<? extends T1, ? extends T2, ? extends T3>> unzipper) {
        Objects.requireNonNull(unzipper, "unzipper is null");
        final Builder<T1> xs = newBuilder(length());
        final Builder<T2> ys = newBuilder(length());
        final Builder<T3> zs = newBuilder(length());
        for (T element : this) {
            final Tuple3<? extends T1, ? extends T2, ? extends T3> t = unzipper.apply(element);
            xs.add(t._1());
            ys.add(t._2());
            zs.add(t._3());
        }
        return Tuple.of(xs.result(), ys.result(), zs.result());
    }

    /**
     * Replaces the element at {@code index}.
     * <p>
     * Complexity: effectively O(1): copies a few small arrays of 32 elements, not the Vector. On a Vector of primitive
     * values, a value of another class first converts every element: O(n), paid again at each such write on the same
     * Vector (see the class documentation).
     *
     * @param index   a position, {@code 0 <= index < length()}
     * @param element the new element
     * @return a new Vector with {@code element} at {@code index}
     * @throws IndexOutOfBoundsException if {@code index} is out of range
     * @throws NullPointerException      if {@code element} is null
     */
    public Vector<T> update(int index, T element) {
        if (isValid(index)) {
            return wrap(trie.update(index, element));
        } else {
            throw new IndexOutOfBoundsException("update(" + index + ")");
        }
    }

    /**
     * Replaces the element at {@code index} by {@code updater} applied to it.
     * <p>
     * Complexity: effectively O(1): one {@link #get(int)} and one {@link #update(int, Object)}. On a Vector of
     * primitive values, a value of another class first converts every element: O(n), paid again at each such write on
     * the same Vector (see the class documentation).
     *
     * @param index   a position, {@code 0 <= index < length()}
     * @param updater computes the new element from the current one
     * @return a new Vector with the updated element at {@code index}
     * @throws IndexOutOfBoundsException if {@code index} is out of range
     * @throws NullPointerException      if {@code updater} is null or returns null
     */
    public Vector<T> update(int index, Function<? super T, ? extends T> updater) {
        Objects.requireNonNull(updater, "updater is null");
        return update(index, updater.apply(get(index)));
    }

    /**
     * Returns a {@code Vector} formed by pairing elements of this {@code Vector} with elements of another
     * {@code Iterable}. Pairing stops when either collection runs out of elements; any remaining elements in the longer
     * collection are ignored.
     * <p>
     * The length of the resulting {@code Vector} is the minimum of the lengths of this {@code Vector} and
     * {@code that}.
     * <p>
     * Complexity: O(min(n, m)) for m elements of {@code that}.
     *
     * @param <U>  the type of elements in the second half of each pair
     * @param that an {@code Iterable} providing the second element of each pair
     * @return a new {@code Vector} containing pairs of corresponding elements
     * @throws NullPointerException if {@code that} is null
     */
    public <U extends @Nullable Object> Vector<Tuple2<T, U>> zip(Iterable<? extends U> that) {
        return zipWith(that, Tuple::of);
    }

    /**
     * Returns a {@code Vector} by combining elements of this {@code Vector} with elements of another
     * {@code Iterable} using a mapping function. Pairing stops when either collection runs out of elements.
     * <p>
     * The length of the resulting {@code Vector} is the minimum of the lengths of this {@code Vector} and
     * {@code that}.
     * <p>
     * Complexity: O(min(n, m)) for m elements of {@code that}.
     *
     * @param <U>    the type of elements in the second parameter of the mapper
     * @param <R>    the type of elements in the resulting {@code Vector}
     * @param that   an {@code Iterable} providing the second parameter of the mapper
     * @param mapper a function that combines elements from this and {@code that} into a new element
     * @return a new {@code Vector} containing mapped elements
     * @throws NullPointerException if {@code that} or {@code mapper} is null
     */
    public <U extends @Nullable Object, R extends @Nullable Object> Vector<R> zipWith(Iterable<? extends U> that, BiFunction<? super T, ? super U, ? extends R> mapper) {
        Objects.requireNonNull(that, "that is null");
        Objects.requireNonNull(mapper, "mapper is null");
        return ofAll(Iterator.ofAll(this).zipWith(that, mapper));
    }

    /**
     * Returns a {@code Vector} formed by pairing elements of this {@code Vector} with elements of another
     * {@code Iterable}, filling in placeholder elements when one collection is shorter than the other.
     * <p>
     * The length of the resulting {@code Vector} is the maximum of the lengths of this {@code Vector} and
     * {@code that}.
     * <p>
     * If this {@code Vector} is shorter than {@code that}, {@code thisElem} is used as a filler. Conversely, if
     * {@code that} is shorter, {@code thatElem} is used.
     * <p>
     * Complexity: O(max(n, m)) for m elements of {@code that}.
     *
     * @param <U>      the type of elements in the second half of each pair
     * @param that     an {@code Iterable} providing the second element of each pair
     * @param thisElem the element used to fill missing values if this {@code Vector} is shorter than {@code that}
     * @param thatElem the element used to fill missing values if {@code that} is shorter than this {@code Vector}
     * @return a new {@code Vector} containing pairs of elements, including fillers as needed
     * @throws NullPointerException if {@code that} is null
     */
    public <U extends @Nullable Object> Vector<Tuple2<T, U>> zipAll(Iterable<? extends U> that, T thisElem, U thatElem) {
        Objects.requireNonNull(that, "that is null");
        return ofAll(Iterator.ofAll(this).zipAll(that, thisElem, thatElem));
    }

    /**
     * Zips this {@code Vector} with its indices, starting at 0.
     * <p>
     * Complexity: O(n).
     *
     * @return a new {@code Vector} containing each element paired with its index
     */
    public Vector<Tuple2<T, Integer>> zipWithIndex() {
        return zipWithIndex(Tuple::of);
    }

    /**
     * Zips this {@code Vector} with its indices and maps the resulting pairs using the provided mapper.
     * <p>
     * Complexity: O(n).
     *
     * @param <U>    the type of elements in the resulting {@code Vector}
     * @param mapper a function mapping an element and its index to a new element
     * @return a new {@code Vector} containing the mapped elements
     * @throws NullPointerException if {@code mapper} is null
     */
    public <U extends @Nullable Object> Vector<U> zipWithIndex(BiFunction<? super T, ? super Integer, ? extends U> mapper) {
        Objects.requireNonNull(mapper, "mapper is null");
        return ofAll(Iterator.ofAll(this).zipWithIndex(mapper));
    }

    /**
     * Whether {@code o} is a sequence with equal elements in the same order: another Vector or one of the other
     * ordered sequence types.
     * <p>
     * Complexity: O(n + m) for a sequence of m elements: the sizes are compared first (a {@link List}, a
     * {@link Queue} or a {@link Stream} counts its elements to answer), then the elements in order, up to the first difference. O(1)
     * for an object that is not a sequence.
     *
     * @param o any object
     * @return true if {@code o} is an ordered sequence of the same elements
     */
    @Override
    public boolean equals(@Nullable Object o) {
        return com.guizmaii.zazr.collection.internal.Collections.equals(this, o);
    }

    /**
     * The hash of the elements in order, the same as that of an equal List, Queue or Stream.
     * <p>
     * Complexity: O(n), computed again at every call: nothing is cached, which matters for a Vector used as a key of
     * a {@link HashMap} or an element of a {@link HashSet}.
     *
     * @return the hash code
     */
    @Override
    public int hashCode() {
        return com.guizmaii.zazr.collection.internal.Collections.hashOrdered(this);
    }

    @Override
    public String toString() { return mkString("Vector(", ", ", ")"); }

    /**
     * A mutable, single-use accumulator that builds a {@link Vector} element by element. Its invariant is that there is
     * never a full-size intermediate buffer: elements are written into 32-wide leaf arrays, each completed leaf is handed
     * to the Vector as it is (or, when the elements come from another Vector through {@link #addAll(Iterable)}, its
     * aligned full leaves are shared instead of copied), and {@link #result()} copies only the final leaf, once, when it
     * is partially filled. The internal nodes above the leaves are allocated as leaves complete.
     * <p>
     * Not thread-safe. After {@link #result()} has been called, every method throws {@link IllegalStateException};
     * create a new builder instead.
     *
     * @param <T> Component type of the Vector.
     */
    public static final class Builder<T extends @Nullable Object> {

        private static final int WIDTH = BitMappedTrie.BRANCHING_FACTOR;
        /* levels 1..6 above the leaves: 32^7 slots, more than any array can hold */
        private static final int LEVELS = 7;

        /* after result(): a zero-length leaf, so that add() falls into growOrCloseLeaf(), which throws; a live leaf is never empty */
        private static final Object[] DONE = new Object[0];

        /* the leaf currently being filled; its capacity is WIDTH, or the size hint when that is smaller */
        private Object[] leaf;
        private int leafLength;
        /* the number of elements in completed leaves (pushed or shared); size is lenRest + leafLength, as in Scala's VectorBuilder */
        private int lenRest;
        /* nodes[level] is the partially filled node at that level (children of nodes[level] live at level - 1);
         * allocated on the first completed leaf, so a Vector that fits in one leaf costs one array */
        private Object[] @Nullable [] nodes = EMPTY_NODES;
        private int[] nodeLengths = EMPTY_NODE_LENGTHS;
        private static final Object[] @Nullable [] EMPTY_NODES = new Object[0][];
        private static final int[] EMPTY_NODE_LENGTHS = new int[0];
        /* the highest level in use; 0 while everything still fits in one leaf */
        private int depth;
        private boolean done;

        Builder(int leafCapacity) {
            this.leaf = new Object[leafCapacity];
        }

        /**
         * Appends one element.
         *
         * @param element the element, never null
         * @return this builder
         * @throws IllegalStateException if {@link #result()} has already been called
         * @throws NullPointerException if {@code element} is null
         */
        public Builder<T> add(T element) {
            // the hot path is one branch and one array store: the open check lives in growOrCloseLeaf(), reached
            // through the zero-length DONE leaf, and the size is derived, not counted. It runs before the null
            // check so that add() on a closed builder always throws IllegalStateException, null argument or not.
            if (leafLength == leaf.length) {
                growOrCloseLeaf();
            }
            Objects.requireNonNull(element, "Vector.Builder.add: element is null");
            leaf[leafLength++] = element;
            return this;
        }

        /**
         * Appends all elements of the given iterable, in iteration order. Appending a {@link Vector} with {@code Object[]}
         * leaves copies whole leaf arrays and shares aligned full ones instead of iterating; a primitive-backed Vector
         * ({@code Vector.range}, {@code ofAll(int[])}) is boxed one element at a time.
         * <p>
         * If {@code elements} is not itself a {@code Vector} (which cannot contain a null element), it is consumed
         * one element at a time, and a null element part-way through is rejected only when reached: the builder keeps
         * whatever elements were added before it, and a later {@link #result()} returns that partial content, not an
         * empty or discarded builder.
         *
         * @param elements the elements to append
         * @return this builder
         * @throws IllegalStateException if {@link #result()} has already been called
         * @throws NullPointerException if {@code elements} is null, or if it yields a null element
         */
        @SuppressWarnings("unchecked")
        public Builder<T> addAll(Iterable<? extends T> elements) {
            checkOpen();
            Objects.requireNonNull(elements, "elements is null");
            if (elements instanceof Vector<?> vector) {
                addVector((Vector<? extends T>) vector);
            } else {
                for (T element : elements) {
                    add(element);
                }
            }
            return this;
        }

        /**
         * @return the number of elements added so far
         * @throws IllegalStateException if {@link #result()} has already been called
         */
        public int size() {
            checkOpen();
            return lenRest + leafLength;
        }

        /**
         * Builds the Vector. The builder cannot be used afterwards.
         *
         * @return a Vector of all elements added, in order
         * @throws IllegalStateException if {@link #result()} has already been called
         */
        public Vector<T> result() {
            checkOpen();
            final int size = lenRest + leafLength;
            if (size == 0) {
                finish();
                return empty();
            }
            // the current leaf is empty after a shared push (addAll of a Vector ending on a full leaf): it must not be appended
            @Nullable Object current = (leafLength == 0)
                                       ? null
                                       : (leafLength == leaf.length) ? leaf : Arrays.copyOf(leaf, leafLength);
            for (int level = 1; level <= depth; level++) {
                Object[] node = nodes[level];
                int nodeLength = nodeLengths[level];
                if (node != null && nodeLength == WIDTH) {
                    push(node, level + 1);
                    nodes[level] = null;
                    node = null;
                    nodeLength = 0;
                }
                if (node == null) {
                    current = (current == null) ? null : new Object[] { current };
                } else {
                    final Object[] combined = Arrays.copyOf(node, (current == null) ? nodeLength : nodeLength + 1);
                    if (current != null) {
                        combined[nodeLength] = current;
                    }
                    current = combined;
                }
            }
            Objects.requireNonNull(current); // size > 0, so at least one leaf reached the root
            // a root with a single child adds a level for nothing: drop it, as ofAll never produces one
            while (depth > 0 && ((Object[]) current).length == 1) {
                current = ((Object[]) current)[0];
                depth--;
            }
            final BitMappedTrie<T> trie = BitMappedTrie.ofBuilt(current, size, depth * BitMappedTrie.BRANCHING_BASE);
            finish();
            return new Vector<>(trie);
        }

        private void finish() {
            done = true;
            leaf = DONE;
            leafLength = 0;
            Arrays.fill(nodes, null);
        }

        private void checkOpen() {
            if (done) {
                throw new IllegalStateException("result() has already been called on this Vector.Builder");
            }
        }

        private void growOrCloseLeaf() {
            checkOpen();
            if (leaf.length < WIDTH) {
                leaf = Arrays.copyOf(leaf, WIDTH);
            } else {
                push(leaf, 1);
                lenRest += WIDTH;
                leaf = new Object[WIDTH];
                leafLength = 0;
            }
        }

        /* appends a completed child (a full leaf or a full node) to the node at the given level, opening it if needed */
        private void push(Object[] child, int level) {
            if (nodes.length == 0) {
                nodes = new Object[LEVELS][];
                nodeLengths = new int[LEVELS];
            }
            Object[] node = nodes[level];
            if (node == null) {
                node = new Object[WIDTH];
                nodes[level] = node;
                depth = Math.max(depth, level);
            } else if (nodeLengths[level] == WIDTH) {
                push(node, level + 1);
                node = new Object[WIDTH];
                nodes[level] = node;
                nodeLengths[level] = 0;
            }
            node[nodeLengths[level]++] = child;
        }

        /*
         * The bulk loops below keep the leaf state in locals: a per-element add() pays for field writes and the
         * open-check on every element, which is what separates it from writing into a flat array.
         */

        /* appends mapper(source[i]) for i in [start, end); source is a leaf read through its ArrayType (one boxing per element for a primitive leaf) */
        <S extends @Nullable Object> void addMapped(ArrayType<S> type, Object source, int start, int end, Function<? super S, ? extends T> mapper) {
            checkOpen();
            Object[] leaf = this.leaf;
            int leafLength = this.leafLength;
            for (int i = start; i < end; i++) {
                if (leafLength == leaf.length) {
                    this.leafLength = leafLength;
                    growOrCloseLeaf();
                    leaf = this.leaf;
                    leafLength = this.leafLength;
                }
                leaf[leafLength++] = Objects.requireNonNull(mapper.apply(type.getAt(source, i)), "Vector.map: element is null");
            }
            this.leafLength = leafLength;
        }

        /* appends n elements f(0) .. f(n - 1) */
        void addTabulated(int n, Function<? super Integer, ? extends T> f) {
            checkOpen();
            Object[] leaf = this.leaf;
            int leafLength = this.leafLength;
            for (int i = 0; i < n; i++) {
                if (leafLength == leaf.length) {
                    this.leafLength = leafLength;
                    growOrCloseLeaf();
                    leaf = this.leaf;
                    leafLength = this.leafLength;
                }
                leaf[leafLength++] = Objects.requireNonNull(f.apply(i), "Vector: element is null");
            }
            this.leafLength = leafLength;
        }

        /* appends the same element n times, one Arrays.fill per leaf */
        void addRepeated(int n, T element) {
            checkOpen();
            Objects.requireNonNull(element, "Vector.fill: element is null");
            int remaining = n;
            while (remaining > 0) {
                if (leafLength == leaf.length) {
                    growOrCloseLeaf();
                }
                final int count = Math.min(leaf.length - leafLength, remaining);
                Arrays.fill(leaf, leafLength, leafLength + count, element);
                leafLength += count;
                remaining -= count;
            }
        }

        private void addVector(Vector<? extends T> vector) {
            final BitMappedTrie<? extends T> trie = vector.trie;
            trie.<Object> visit((index, sourceLeaf, start, end) -> {
                addLeafRange(trie.type, sourceLeaf, start, end);
                return index + end - start;
            });
        }

        @SuppressWarnings("unchecked")
        private void addLeafRange(ArrayType<?> type, Object sourceLeaf, int start, int end) {
            if (sourceLeaf instanceof Object[] source) {
                if (leafLength == leaf.length) {
                    // an exactly full current leaf is pushed lazily on the next write; close it now so a full source leaf can be shared
                    growOrCloseLeaf();
                }
                if (leafLength == 0 && start == 0 && end == WIDTH && source.length == WIDTH) {
                    // a full, untrimmed leaf of the source: share it, nobody mutates leaves
                    push(source, 1);
                    lenRest += WIDTH;
                    return;
                }
                int from = start, remaining = end - start;
                while (remaining > 0) {
                    if (leafLength == leaf.length) {
                        growOrCloseLeaf();
                    }
                    final int count = Math.min(leaf.length - leafLength, remaining);
                    System.arraycopy(source, from, leaf, leafLength, count);
                    leafLength += count;
                    from += count;
                    remaining -= count;
                }
            } else {
                // a primitive leaf (int[], ...): elements are boxed one by one
                for (int i = start; i < end; i++) {
                    add((T) type.getAt(sourceLeaf, i));
                }
            }
        }
    }

    // -- windows and products

    /**
     * The elements in consecutive blocks of {@code size}: {@code Vector.of(1, 2, 3, 4, 5).grouped(2)} is
     * {@code Vector(Vector(1, 2), Vector(3, 4), Vector(5))}; the last block is smaller when {@code size} does not
     * divide the length. The same as {@code sliding(size, size)}.
     * <p>
     * Complexity: O(n / size): one effectively O(1) {@link #slice(int, int)} per block; each block shares its elements
     * with this Vector.
     *
     * @param size the block size, positive
     * @return the blocks, in order; empty if this Vector is empty
     * @throws IllegalArgumentException if {@code size} is not positive
     */
    public Vector<Vector<T>> grouped(int size) {
        return sliding(size, size);
    }

    /**
     * The windows of {@code size} consecutive elements, each starting one element after the previous:
     * {@code Vector.of(1, 2, 3, 4).sliding(3)} is {@code Vector(Vector(1, 2, 3), Vector(2, 3, 4))}. A Vector
     * shorter than {@code size} is one window. The same as {@code sliding(size, 1)}.
     * <p>
     * Complexity: O(n): one effectively O(1) {@link #slice(int, int)} per window; each window shares its elements with
     * this Vector, so no element is copied.
     *
     * @param size the window size, positive
     * @return the windows, in order; empty if this Vector is empty
     * @throws IllegalArgumentException if {@code size} is not positive
     */
    public Vector<Vector<T>> sliding(int size) {
        return sliding(size, 1);
    }

    /**
     * The windows of {@code size} consecutive elements, each starting {@code step} elements after the previous:
     * {@code Vector.of(1, 2, 3, 4, 5).sliding(2, 3)} is {@code Vector(Vector(1, 2), Vector(4, 5))} and
     * {@code sliding(2, 4)} is {@code Vector(Vector(1, 2), Vector(5))}. The last window is shorter than
     * {@code size} when it reaches the end; a window whose elements all belong to the previous one is not
     * produced, so {@code Vector.of(1, 2, 3, 4).sliding(3)} has two windows. A Vector shorter than {@code size} is
     * one window; an empty Vector has none.
     * <p>
     * Complexity: O(n / step): one effectively O(1) {@link #slice(int, int)} per window; each window shares its
     * elements with this Vector, so no element is copied.
     *
     * @param size the window size, positive
     * @param step the distance between two window starts, positive
     * @return the windows, in order
     * @throws IllegalArgumentException if {@code size} or {@code step} is not positive
     */
    public Vector<Vector<T>> sliding(int size, int step) {
        com.guizmaii.zazr.collection.internal.Collections.checkWindow(size, step);
        final int length = length();
        if (length == 0) {
            return empty();
        }
        final Builder<Vector<T>> builder = newBuilder();
        // past the first, a window is produced only while it holds at least one element the previous one did not
        for (long start = 0; start < length && (start == 0 || start - step + size < length); start += step) {
            builder.add(slice((int) start, (int) Math.min(start + size, length)));
        }
        return builder.result();
    }

    /**
     * The elements in maximal runs of consecutive elements with the same key, computed once per element by
     * {@code classifier}: {@code Vector.of(1, 2, 3, 10, 12, 5, 7, 20, 29).slideBy(x -> x / 10)} is
     * {@code Vector(Vector(1, 2, 3), Vector(10, 12), Vector(5, 7), Vector(20, 29))}. The runs concatenate back to
     * this Vector.
     * <p>
     * Complexity: O(n): one key per element, and one effectively O(1) {@link #slice(int, int)} per run; each run
     * shares its elements with this Vector.
     *
     * @param classifier the key of an element; two consecutive elements are in the same run when their keys are
     *                   equal
     * @return the runs, in order; empty if this Vector is empty
     * @throws NullPointerException if {@code classifier} is null
     */
    public Vector<Vector<T>> slideBy(Function<? super T, ?> classifier) {
        Objects.requireNonNull(classifier, "classifier is null");
        if (isEmpty()) {
            return empty();
        }
        final Builder<Vector<T>> builder = newBuilder();
        final java.util.Iterator<T> iterator = iterator();
        Object key = classifier.apply(iterator.next());
        int start = 0;
        int index = 1;
        while (iterator.hasNext()) {
            final Object next = classifier.apply(iterator.next());
            if (!Objects.equals(key, next)) {
                builder.add(slice(start, index));
                start = index;
                key = next;
            }
            index++;
        }
        builder.add(slice(start, index));
        return builder.result();
    }

    /**
     * The Cartesian square of this Vector: every pair {@code (a, b)} of elements, {@code a} varying slowest.
     * <p>
     * Complexity: O(n^2); the pairs are built now.
     *
     * @return the pairs
     */
    public Vector<Tuple2<T, T>> crossProduct() {
        return crossProduct(this);
    }

    /**
     * The Cartesian power of this Vector: every Vector of {@code power} elements drawn from this one, in
     * lexicographic position order. {@code power == 0} gives one empty Vector; a negative power gives no result.
     * <p>
     * Complexity: O(power * n^power): n^power Vectors of {@code power} elements each, built now.
     *
     * @param power the size of each result
     * @return the Vectors
     */
    public Vector<Vector<T>> crossProduct(int power) {
        if (power < 0) {
            return empty();
        }
        Vector<Vector<T>> product = Vector.of(Vector.<T> empty());
        for (int i = 0; i < power; i++) {
            product = product.flatMap(el -> map(el::append));
        }
        return product;
    }

    /**
     * The Cartesian product of this Vector and {@code that}: every pair {@code (a, b)} with {@code a} from this
     * Vector and {@code b} from {@code that}, {@code a} varying slowest. {@code that} is walked once.
     * <p>
     * Complexity: O(n * m) for m elements of {@code that}; the pairs are built now.
     *
     * @param that the right-hand elements
     * @param <U>  their type
     * @return the pairs
     * @throws NullPointerException if {@code that} is null
     */
    public <U extends @Nullable Object> Vector<Tuple2<T, U>> crossProduct(Iterable<? extends U> that) {
        Objects.requireNonNull(that, "that is null");
        final Vector<U> other = Vector.ofAll(that);
        return flatMap(a -> other.map(b -> Tuple.of(a, b)));
    }

    /**
     * Combines the elements from the right: the last with the one before it, the result with the one before that,
     * and so on.
     * <p>
     * Complexity: O(n), walking the elements from the last to the first without copying.
     *
     * @param op combines the next element and the result so far
     * @return the combined result
     * @throws NoSuchElementException if this Vector is empty
     * @throws NullPointerException   if {@code op} is null
     */
    public T reduceRight(BiFunction<? super T, ? super T, ? extends T> op) {
        Objects.requireNonNull(op, "op is null");
        if (isEmpty()) {
            throw new NoSuchElementException("reduceRight on empty Vector");
        }
        T xs = get(length() - 1);
        for (int i = length() - 2; i >= 0; i--) {
            xs = op.apply(get(i), xs);
        }
        return xs;
    }

    /**
     * Whether exactly one element satisfies {@code predicate}.
     *
     * @param predicate the condition to test
     * @return {@code true} if one and only one element matches, {@code false} otherwise
     * @throws NullPointerException if {@code predicate} is null
     */
    public boolean existsUnique(Predicate<? super T> predicate) {
        return TraversableModule.existsUnique(this, predicate);
    }

    /**
     * The greatest element in the natural order of the elements, which must be {@link Comparable}; the sort order
     * of a sorted collection is not consulted. {@code NaN} compares as the greatest {@code Double} or {@code Float}.
     * <p>
     * Complexity: O(n), every element compared once.
     *
     * @return {@code Some(maximum)} if there is an element, {@code None} otherwise
     * @throws ClassCastException if two or more elements are not {@code Comparable}
     */
    public Option<T> max() {
        return TraversableModule.max(this);
    }

    /**
     * The greatest element according to {@code comparator}; of equal greatest elements, the first in this
     * Vector's order.
     *
     * @param comparator the order
     * @return {@code Some(maximum)} if there is an element, {@code None} otherwise
     * @throws NullPointerException if {@code comparator} is null
     */
    public Option<T> maxBy(Comparator<? super T> comparator) {
        return TraversableModule.maxBy(this, comparator);
    }

    /**
     * The element whose key, computed once by {@code f}, is the greatest; of equal greatest keys, the first
     * element in this Vector's order.
     *
     * @param f   the key of an element
     * @param <U> the key type
     * @return {@code Some(element)} if there is an element, {@code None} otherwise
     * @throws NullPointerException if {@code f} is null
     */
    public <U extends Comparable<? super U>> Option<T> maxBy(Function<? super T, ? extends U> f) {
        return TraversableModule.maxBy(this, f);
    }

    /**
     * The least element in the natural order of the elements, which must be {@link Comparable}; the sort order of
     * a sorted collection is not consulted. Among {@code Double}s or {@code Float}s, a {@code NaN} is the result
     * whenever one is present.
     * <p>
     * Complexity: O(n), every element compared once.
     *
     * @return {@code Some(minimum)} if there is an element, {@code None} otherwise
     * @throws ClassCastException if two or more elements are not {@code Comparable}
     */
    public Option<T> min() {
        return TraversableModule.min(this);
    }

    /**
     * The least element according to {@code comparator}; of equal least elements, the first in this Vector's
     * order.
     *
     * @param comparator the order
     * @return {@code Some(minimum)} if there is an element, {@code None} otherwise
     * @throws NullPointerException if {@code comparator} is null
     */
    public Option<T> minBy(Comparator<? super T> comparator) {
        return TraversableModule.minBy(this, comparator);
    }

    /**
     * The element whose key, computed once by {@code f}, is the least; of equal least keys, the first element in
     * this Vector's order.
     *
     * @param f   the key of an element
     * @param <U> the key type
     * @return {@code Some(element)} if there is an element, {@code None} otherwise
     * @throws NullPointerException if {@code f} is null
     */
    public <U extends Comparable<? super U>> Option<T> minBy(Function<? super T, ? extends U> f) {
        return TraversableModule.minBy(this, f);
    }

    /**
     * Folds the elements with {@code combine}, starting from {@code zero}, which must be its neutral element.
     * The elements are combined from the left, so {@code combine} need not be associative.
     *
     * @param zero    the neutral element of {@code combine}
     * @param combine combines two elements
     * @return the folded result, {@code zero} on an empty Vector
     * @throws NullPointerException if {@code combine} is null
     */
    public T fold(T zero, BiFunction<? super T, ? super T, ? extends T> combine) {
        Objects.requireNonNull(combine, "combine is null");
        return foldLeft(zero, combine);
    }

    /**
     * Combines the elements with {@code op}, each result with the next element. The same as {@link #reduceLeft(BiFunction)}.
     *
     * @param op combines two elements
     * @return the combined result
     * @throws NoSuchElementException if this Vector is empty
     * @throws NullPointerException   if {@code op} is null
     */
    public T reduce(BiFunction<? super T, ? super T, ? extends T> op) {
        return TraversableModule.reduceLeft(this, op);
    }

    /**
     * {@link #reduce(BiFunction)} as an {@code Option}: {@code None} on an empty Vector.
     * <p>
     * Complexity: O(n), as {@link #reduceLeft(BiFunction)}.
     *
     * @param op combines two elements
     * @return {@code Some(result)}, or {@code None} if this Vector is empty
     * @throws NullPointerException if {@code op} is null
     */
    public Option<T> reduceOption(BiFunction<? super T, ? super T, ? extends T> op) {
        return TraversableModule.reduceLeftOption(this, op);
    }

    /**
     * The only element.
     *
     * @return the element
     * @throws NoSuchElementException if this Vector is empty or has more than one element
     */
    public T single() {
        return TraversableModule.single(this);
    }

    /**
     * The only element as an {@code Option}.
     *
     * @return {@code Some(element)} if there is exactly one element, {@code None} otherwise
     */
    public Option<T> singleOption() {
        return TraversableModule.singleOption(this);
    }

    /**
     * Arranges the elements by a key that must be unique: {@code Some} of the map from each key to its element,
     * or {@code None} as soon as two elements share a key. The same as {@code groupBy(getKey)} when every group is
     * a singleton.
     *
     * @param getKey the key of an element
     * @param <K>  the key type
     * @return {@code Some(map)} if the keys are unique, {@code None} otherwise
     * @throws NullPointerException if {@code getKey} is null
     */
    public <K extends @Nullable Object> Option<Map<K, T>> arrangeBy(Function<? super T, ? extends K> getKey) {
        Objects.requireNonNull(getKey, "getKey is null");
        return TraversableModule.arrangeBy(groupBy(getKey));
    }

    /**
     * The sum of the elements, which must be {@link Number}s: {@code Byte}, {@code Short}, {@code Integer} and
     * {@code Long} are summed as a {@code long}, {@code BigInteger} and {@code BigDecimal} with their own
     * arithmetic, any other {@code Number} as a {@code double} with Neumaier compensation. The arithmetic is chosen
     * from the first element. {@code 0} on an empty Vector.
     *
     * @return the sum
     * @throws UnsupportedOperationException if an element is not a {@code Number}
     */
    public Number sum() {
        return TraversableModule.sum(this);
    }

    /**
     * The product of the elements, which must be {@link Number}s: {@code Byte}, {@code Short}, {@code Integer} and
     * {@code Long} are multiplied as a {@code long}, {@code BigInteger} and {@code BigDecimal} with their own
     * arithmetic, any other {@code Number} as a {@code double}. The arithmetic is chosen from the first element.
     * {@code 1} on an empty Vector.
     *
     * @return the product
     * @throws UnsupportedOperationException if an element is not a {@code Number}
     */
    public Number product() {
        return TraversableModule.product(this);
    }

    /**
     * The average of the elements, which must be {@link Number}s, summed as {@code double}s with Neumaier
     * compensation.
     *
     * @return {@code Some(average)} if there is an element, {@code None} otherwise
     * @throws UnsupportedOperationException if an element is not a {@code Number}
     */
    public Option<Double> average() {
        return TraversableModule.average(this);
    }

    /**
     * The last element, in order, that satisfies {@code predicate}.
     * <p>
     * Complexity: O(n); every element is tested.
     *
     * @param predicate the condition to test
     * @return {@code Some(element)} of the last match, or {@code None} if no element matches
     * @throws NullPointerException if {@code predicate} is null
     */
    public Option<T> findLast(Predicate<? super T> predicate) {
        return TraversableModule.findLast(this, predicate);
    }

    /**
     * Runs {@code action} on each element with its position, from {@code 0}, without boxing the index.
     *
     * @param action what to do with each element and its index
     * @throws NullPointerException if {@code action} is null
     */
    public void forEachWithIndex(ObjIntConsumer<? super T> action) {
        TraversableModule.forEachWithIndex(this, action);
    }

    /**
     * The first element as an {@code Option}.
     * <p>
     * Complexity: effectively O(1), as {@link #head()}.
     *
     * @return {@code Some(head)}, or {@code None} if this Vector is empty
     */
    public Option<T> headOption() {
        return isEmpty() ? Option.none() : Option.some(head());
    }

    /**
     * The last element as an {@code Option}.
     * <p>
     * Complexity: effectively O(1), as {@link #last()}.
     *
     * @return {@code Some(last)}, or {@code None} if this Vector is empty
     */
    public Option<T> lastOption() {
        return isEmpty() ? Option.none() : Option.some(last());
    }

    /**
     * Combines the elements from the left: the first with the second, the result with the third, and so on.
     * <p>
     * Complexity: O(n).
     *
     * @param op combines the result so far and the next element
     * @return the combined result
     * @throws NoSuchElementException if this Vector is empty
     * @throws NullPointerException   if {@code op} is null
     */
    public T reduceLeft(BiFunction<? super T, ? super T, ? extends T> op) {
        return TraversableModule.reduceLeft(this, op);
    }

    /**
     * {@link #reduceLeft(BiFunction)} as an {@code Option}: {@code None} on an empty Vector.
     * <p>
     * Complexity: O(n), as {@link #reduceLeft(BiFunction)}.
     *
     * @param op combines the result so far and the next element
     * @return {@code Some(result)}, or {@code None} if this Vector is empty
     * @throws NullPointerException if {@code op} is null
     */
    public Option<T> reduceLeftOption(BiFunction<? super T, ? super T, ? extends T> op) {
        return TraversableModule.reduceLeftOption(this, op);
    }

    /**
     * {@link #reduceRight(BiFunction)} as an {@code Option}: {@code None} on an empty Vector.
     * <p>
     * Complexity: O(n), as {@link #reduceRight(BiFunction)}.
     *
     * @param op combines the next element and the result so far
     * @return {@code Some(result)}, or {@code None} if this Vector is empty
     * @throws NullPointerException if {@code op} is null
     */
    public Option<T> reduceRightOption(BiFunction<? super T, ? super T, ? extends T> op) {
        Objects.requireNonNull(op, "op is null");
        return isEmpty() ? Option.none() : Option.some(reduceRight(op));
    }

    /**
     * The number of elements; the same as {@link #length()}.
     * <p>
     * Complexity: O(1), as {@link #length()}.
     *
     * @return the number of elements
     */
    @Override
    public int size() {
        return length();
    }

    /**
     * Collects the elements with {@code collector}, as {@code stream().collect(collector)} does.
     *
     * @param <A>       the collector's accumulation type
     * @param <R>       the result type
     * @param collector the collector
     * @return the collected result
     */
    public <R extends @Nullable Object, A extends @Nullable Object> R collect(Collector<? super T, A, R> collector) {
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
    public <R extends @Nullable Object> R collect(Supplier<R> supplier, BiConsumer<R, ? super T> accumulator, BiConsumer<R, R> combiner) {
        return stream().collect(supplier, accumulator, combiner);
    }

    /**
     * The elements as the entries of a new {@link HashMap}, each mapped to a key by {@code keyMapper} and to a
     * value by {@code valueMapper}; of two entries with the same key, the later one in this Vector's order wins.
     *
     * @param keyMapper   the key of an element
     * @param valueMapper the value of an element
     * @param <K>         the key type
     * @param <V>         the value type
     * @return the new map
     * @throws NullPointerException if an argument is null
     */
    public <K extends @Nullable Object, V extends @Nullable Object> Map<K, V> toMap(Function<? super T, ? extends K> keyMapper, Function<? super T, ? extends V> valueMapper) {
        return toMap(TraversableModule.entryMapper(keyMapper, valueMapper));
    }

    /**
     * The elements as the entries of a new {@link HashMap}, each mapped to a key and a value by {@code f}; of two
     * entries with the same key, the later one in this Vector's order wins.
     *
     * @param f   the entry an element becomes
     * @param <K> the key type
     * @param <V> the value type
     * @return the new map
     * @throws NullPointerException if {@code f} is null
     */
    public <K extends @Nullable Object, V extends @Nullable Object> Map<K, V> toMap(Function<? super T, ? extends Tuple2<? extends K, ? extends V>> f) {
        final Function<Iterable<Tuple2<? extends K, ? extends V>>, Map<K, V>> ofAll = HashMap::ofEntries;
        return TraversableModule.toMap(this, HashMap.empty(), ofAll, f);
    }

    /**
     * The elements as the entries of a new {@link LinkedHashMap}, in this Vector's order, each mapped to a key by
     * {@code keyMapper} and to a value by {@code valueMapper}; of two entries with the same key, the later one
     * wins the value and the earlier one the position.
     *
     * @param keyMapper   the key of an element
     * @param valueMapper the value of an element
     * @param <K>         the key type
     * @param <V>         the value type
     * @return the new map
     * @throws NullPointerException if an argument is null
     */
    public <K extends @Nullable Object, V extends @Nullable Object> Map<K, V> toLinkedMap(Function<? super T, ? extends K> keyMapper, Function<? super T, ? extends V> valueMapper) {
        return toLinkedMap(TraversableModule.entryMapper(keyMapper, valueMapper));
    }

    /**
     * The elements as the entries of a new {@link LinkedHashMap}, in this Vector's order, each mapped to a key
     * and a value by {@code f}; of two entries with the same key, the later one wins the value and the earlier one
     * the position.
     *
     * @param f   the entry an element becomes
     * @param <K> the key type
     * @param <V> the value type
     * @return the new map
     * @throws NullPointerException if {@code f} is null
     */
    public <K extends @Nullable Object, V extends @Nullable Object> Map<K, V> toLinkedMap(Function<? super T, ? extends Tuple2<? extends K, ? extends V>> f) {
        final Function<Iterable<Tuple2<? extends K, ? extends V>>, Map<K, V>> ofAll = LinkedHashMap::ofEntries;
        return TraversableModule.toMap(this, LinkedHashMap.empty(), ofAll, f);
    }

    /**
     * The elements as the entries of a new {@link TreeMap} in the natural order of the keys, each mapped to a key
     * by {@code keyMapper} and to a value by {@code valueMapper}; of two entries with the same key, the later one
     * in this Vector's order wins.
     *
     * @param keyMapper   the key of an element
     * @param valueMapper the value of an element
     * @param <K>         the key type
     * @param <V>         the value type
     * @return the new map
     * @throws NullPointerException if an argument is null
     */
    public <K extends Comparable<? super K>, V extends @Nullable Object> SortedMap<K, V> toSortedMap(Function<? super T, ? extends K> keyMapper, Function<? super T, ? extends V> valueMapper) {
        return toSortedMap(TraversableModule.entryMapper(keyMapper, valueMapper));
    }

    /**
     * The elements as the entries of a new {@link TreeMap} in the natural order of the keys, each mapped to a key
     * and a value by {@code f}; of two entries with the same key, the later one in this Vector's order wins.
     *
     * @param f   the entry an element becomes
     * @param <K> the key type
     * @param <V> the value type
     * @return the new map
     * @throws NullPointerException if {@code f} is null
     */
    public <K extends Comparable<? super K>, V extends @Nullable Object> SortedMap<K, V> toSortedMap(Function<? super T, ? extends Tuple2<? extends K, ? extends V>> f) {
        Objects.requireNonNull(f, "f is null");
        return toSortedMap(Comparator.naturalOrder(), f);
    }

    /**
     * The elements as the entries of a new {@link TreeMap} ordered by {@code comparator}, each mapped to a key by
     * {@code keyMapper} and to a value by {@code valueMapper}; of two entries with the same key, the later one in
     * this Vector's order wins.
     *
     * @param comparator  the order of the keys
     * @param keyMapper   the key of an element
     * @param valueMapper the value of an element
     * @param <K>         the key type
     * @param <V>         the value type
     * @return the new map
     * @throws NullPointerException if an argument is null
     */
    public <K extends @Nullable Object, V extends @Nullable Object> SortedMap<K, V> toSortedMap(Comparator<? super K> comparator, Function<? super T, ? extends K> keyMapper, Function<? super T, ? extends V> valueMapper) {
        return toSortedMap(comparator, TraversableModule.entryMapper(keyMapper, valueMapper));
    }

    /**
     * The elements as the entries of a new {@link TreeMap} ordered by {@code comparator}, each mapped to a key and
     * a value by {@code f}; of two entries with the same key, the later one in this Vector's order wins.
     *
     * @param comparator the order of the keys
     * @param f          the entry an element becomes
     * @param <K>        the key type
     * @param <V>        the value type
     * @return the new map
     * @throws NullPointerException if an argument is null
     */
    public <K extends @Nullable Object, V extends @Nullable Object> SortedMap<K, V> toSortedMap(Comparator<? super K> comparator, Function<? super T, ? extends Tuple2<? extends K, ? extends V>> f) {
        Objects.requireNonNull(comparator, "comparator is null");
        final Function<Iterable<Tuple2<? extends K, ? extends V>>, SortedMap<K, V>> ofAll = t -> TreeMap.ofEntries(comparator, t);
        return TraversableModule.toMap(this, TreeMap.empty(comparator), ofAll, f);
    }

    /**
     * The elements as a {@link Queue}, in this Vector's order.
     *
     * @return a {@code Queue} of the elements
     */
    public Queue<T> toQueue() {
        return TraversableModule.toTraversable(this, Queue.empty(), Queue::ofAll);
    }

    /**
     * The distinct elements as a {@link LinkedHashSet}, in this Vector's order.
     *
     * @return a {@code LinkedHashSet} of the elements
     */
    public Set<T> toLinkedSet() {
        return TraversableModule.toTraversable(this, LinkedHashSet.empty(), LinkedHashSet::ofAll);
    }

    /**
     * The distinct elements as a {@link TreeSet} in their natural order; a {@code TreeSet} returns itself.
     *
     * @return a {@code TreeSet} of the elements
     * @throws ClassCastException if the elements are not {@link Comparable}
     */
    public SortedSet<T> toSortedSet() {
        return TraversableModule.toSortedSet(this);
    }

    /**
     * The distinct elements as a {@link TreeSet} ordered by {@code comparator}.
     *
     * @param comparator the order
     * @return a {@code TreeSet} of the elements
     * @throws NullPointerException if {@code comparator} is null
     */
    public SortedSet<T> toSortedSet(Comparator<? super T> comparator) {
        Objects.requireNonNull(comparator, "comparator is null");
        return TraversableModule.toTraversable(this, TreeSet.empty(comparator), values -> TreeSet.ofAll(comparator, values));
    }

    /**
     * The elements as a {@link Stream}, in this Vector's order.
     *
     * @return a {@code Stream} of the elements
     */
    public Stream<T> toStream() {
        return TraversableModule.toTraversable(this, Stream.empty(), Stream::ofAll);
    }

}
