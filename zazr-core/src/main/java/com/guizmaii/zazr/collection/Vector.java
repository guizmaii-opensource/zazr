package com.guizmaii.zazr.collection;

import com.guizmaii.zazr.*;
import com.guizmaii.zazr.collection.JavaConverters.ListView;
import com.guizmaii.zazr.collection.VectorModule.Combinations;
import com.guizmaii.zazr.control.Either;
import com.guizmaii.zazr.control.Option;
import java.util.*;
import java.util.function.*;
import java.util.stream.Collector;
import org.jspecify.annotations.Nullable;

import static com.guizmaii.zazr.collection.Collections.withSize;
import static com.guizmaii.zazr.collection.JavaConverters.ChangePolicy.IMMUTABLE;
import static com.guizmaii.zazr.collection.JavaConverters.ChangePolicy.MUTABLE;

/**
 * The default sequence: an immutable, indexed sequence with effectively constant time access to any element.
 * Many other operations ({@code update}, {@code append}, {@code prepend}, {@code tail}, {@code drop}, {@code take},
 * {@code slice}) are effectively constant too.
 * <p>
 * The implementation is based on a `bit-mapped trie`, a very wide and shallow tree (i.e. depth ≤ 6). Vector declares
 * its whole API itself and implements only {@link Traversable} (design 3.7): every positional method carries a
 * {@code Complexity:} line in its javadoc, where "effectively O(1)" means O(log32 n), a trie access or a path copy
 * of at most six nodes.
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
        if (iterable instanceof Traversable && com.guizmaii.zazr.collection.Collections.isEmpty(iterable)) {
            return empty();
        }
        if (iterable instanceof Vector) {
            return (Vector<T>) iterable;
        }
        if (iterable instanceof ListView
                && ((ListView<T, ?>) iterable).getDelegate() instanceof Vector) {
            return (Vector<T>) ((ListView<T, ?>) iterable).getDelegate();
        }
        if (com.guizmaii.zazr.collection.Collections.isTraversableAgain(iterable)) {
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
        return com.guizmaii.zazr.collection.Collections.transpose(matrix, Vector::ofAll, Vector::of);
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
     * zazr, because Java cannot demand of an instance method that the receiver's element type be a collection.
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

    // -- the sequence API. Vector implements only Traversable (design 3.7): every method below that was declared by
    // Seq or IndexedSeq is declared here with Vector return types, and every positional method states its cost.
    // "Effectively O(1)" means O(log32 n): a trie access or a path copy of at most six nodes.

    /**
     * Appends an element.
     * <p>
     * Complexity: effectively O(1) (a path copy; the last leaf is copied).
     *
     * @param element the element to append
     * @return a new Vector ending with {@code element}
     * @throws NullPointerException if {@code element} is null
     */
    public Vector<T> append(T element) { return appendAll(com.guizmaii.zazr.collection.List.of(element)); }

    /**
     * Appends all elements of the given iterable, in iteration order.
     * <p>
     * Complexity: O(m) for m appended elements (one leaf copy per 32 elements plus a path copy); O(1) when this
     * Vector is empty and {@code iterable} is a Vector, which is returned as is.
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
        if (com.guizmaii.zazr.collection.Collections.isEmpty(iterable)){
            return this;
        }
        if (!com.guizmaii.zazr.collection.Collections.isTraversableAgain(iterable)) {
            // a one-shot source (an Iterator, typically wrapping a java.util.stream): build it once with the builder, then append by path copy
            return appendAll(ofAll(iterable));
        }
        return new Vector<>(trie.appendAll(iterable));
    }

    /**
     * Returns an immutable {@link java.util.List} view of this Vector: reads go through to this Vector, mutators
     * throw {@link UnsupportedOperationException}.
     * <p>
     * Complexity: O(1); {@code get} on the view is effectively O(1).
     *
     * @return an immutable {@code java.util.List} view
     */
    public java.util.List<T> asJava() {
        return JavaConverters.asJava(this, IMMUTABLE);
    }

    /**
     * Passes an immutable {@link java.util.List} view of this Vector to {@code action} and returns this Vector.
     * <p>
     * Complexity: O(1) to create the view.
     *
     * @param action receives the view
     * @return this Vector
     * @throws NullPointerException if {@code action} is null
     * @see #asJava()
     */
    public Vector<T> asJava(Consumer<? super java.util.List<T>> action) {
        Objects.requireNonNull(action, "action is null");
        action.accept(asJava());
        return this;
    }

    /**
     * Returns a mutable {@link java.util.List} view of this Vector: every mutator replaces the view's underlying
     * Vector by a new one; this Vector is never modified.
     * <p>
     * Complexity: O(1); each mutator costs what the corresponding Vector operation costs.
     *
     * @return a mutable {@code java.util.List} view
     */
    public java.util.List<T> asJavaMutable() {
        return JavaConverters.asJava(this, MUTABLE);
    }

    /**
     * Passes a mutable {@link java.util.List} view of this Vector to {@code action} and returns the Vector the view
     * holds afterwards: this Vector if the action only read, a new one reflecting the writes otherwise.
     * <p>
     * Complexity: O(1) to create the view.
     *
     * @param action receives the view
     * @return this Vector, or a new Vector reflecting the modifications made through the view
     * @throws NullPointerException if {@code action} is null
     * @see #asJavaMutable()
     */
    public Vector<T> asJavaMutable(Consumer<? super java.util.List<T>> action) {
        Objects.requireNonNull(action, "action is null");
        final ListView<T, Vector<T>> view = JavaConverters.asJava(this, MUTABLE);
        action.accept(view);
        return view.getDelegate();
    }

    /**
     * All combinations of the elements, for every size from 0 to {@code length()}, by position:
     * {@code Vector(1, 2).combinations()} is {@code Vector(Vector(), Vector(1), Vector(2), Vector(1, 2))}.
     * <p>
     * Complexity: O(2^n) combinations, each of size up to n.
     *
     * @return the combinations, ordered by size, then by position
     */
    public Vector<Vector<T>> combinations() { return rangeClosed(0, length()).map(this::combinations).flatMap(Function.identity()); }

    /**
     * All combinations of {@code k} elements, selected by position (equal elements are distinct positions).
     * <p>
     * Complexity: O(C(n, k)) combinations of size k.
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
     * The Cartesian square of this Vector: every pair {@code (a, b)} of elements, {@code a} varying slowest.
     * <p>
     * Complexity: lazy; O(n^2) pairs when consumed.
     *
     * @return an iterator over the pairs
     */
    public Iterator<Tuple2<T, T>> crossProduct() {
        return crossProduct(this);
    }

    /**
     * The Cartesian power of this Vector: every Vector of {@code power} elements drawn from this one, in
     * lexicographic position order. {@code power == 0} gives one empty Vector; a negative power gives no result.
     * <p>
     * Complexity: lazy; O(n^power) Vectors of size {@code power} when consumed.
     *
     * @param power the size of each result
     * @return an iterator over the Vectors (its element type is decided in #68, with {@code sliding} and {@code grouped})
     */
    public Iterator<Vector<T>> crossProduct(int power) {
        if (power < 0) {
            return Iterator.empty();
        }
        return Iterator.range(0, power).foldLeft(Iterator.of(Vector.<T> empty()), (product, ignored) -> product.flatMap(el -> map(el::append)));
    }

    /**
     * The Cartesian product of this Vector and {@code that}: every pair {@code (a, b)} with {@code a} from this
     * Vector and {@code b} from {@code that}, {@code a} varying slowest. {@code that} is materialised once.
     * <p>
     * Complexity: lazy; O(n * m) pairs when consumed.
     *
     * @param that the right-hand elements
     * @param <U>  their type
     * @return an iterator over the pairs
     * @throws NullPointerException if {@code that} is null
     */
    public <U extends @Nullable Object> Iterator<Tuple2<T, U>> crossProduct(Iterable<? extends U> that) {
        Objects.requireNonNull(that, "that is null");
        final Vector<U> other = ofAll(that);
        return Iterator.ofAll(this).flatMap(a -> other.iterator().map(b -> Tuple.of(a, b)));
    }

    /**
     * {@inheritDoc}
     * <p>
     * Complexity: O(n).
     */
    @Override
    public Vector<T> distinct() { return distinctBy(Function.identity()); }

    /**
     * {@inheritDoc}
     * <p>
     * Complexity: O(n log n) comparisons.
     */
    @Override
    public Vector<T> distinctBy(Comparator<? super T> comparator) {
        Objects.requireNonNull(comparator, "comparator is null");
        final java.util.Set<T> seen = new java.util.TreeSet<>(comparator);
        return filter(seen::add);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Complexity: O(n).
     */
    @Override
    public <U extends @Nullable Object> Vector<T> distinctBy(Function<? super T, ? extends U> keyExtractor) {
        Objects.requireNonNull(keyExtractor, "keyExtractor is null");
        final java.util.Set<U> seen = new java.util.HashSet<>(length());
        return filter(t -> seen.add(keyExtractor.apply(t)));
    }

    /**
     * The complement of {@link #distinct()}: the elements occurring more than once, each once, in order of first
     * occurrence. {@code isEmpty()} on the result is the "all distinct" test. O(n).
     *
     * @return the duplicated elements
     */
    public Vector<T> duplicates() { return duplicatesBy(Function.identity()); }

    /**
     * {@link #duplicates()} under a key: the first element of each key occurring more than once, in order of first
     * occurrence. One pass, O(n).
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
        return ofAll(iterator().distinctByKeepLast(comparator));
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
        return ofAll(iterator().distinctByKeepLast(keyExtractor));
    }

    /**
     * {@inheritDoc}
     * <p>
     * Complexity: effectively O(1) (the path to the new first leaf is trimmed).
     */
    @Override
    public Vector<T> drop(int n) {
        return wrap(trie.drop(n));
    }

    /**
     * {@inheritDoc}
     * <p>
     * Complexity: O(k) for k dropped elements, then one effectively O(1) {@code drop}.
     */
    @Override
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
     * {@inheritDoc}
     * <p>
     * Complexity: O(k) for k dropped elements, then one effectively O(1) {@code drop}.
     */
    @Override
    public Vector<T> dropWhile(Predicate<? super T> predicate) {
        Objects.requireNonNull(predicate, "predicate is null");
        return dropUntil(predicate.negate());
    }

    /**
     * {@inheritDoc}
     * <p>
     * Complexity: effectively O(1) (the path to the new last leaf is trimmed).
     */
    @Override
    public Vector<T> dropRight(int n) {
        return take(length() - n);
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

    @Override
    public Vector<T> filter(Predicate<? super T> predicate) {
        Objects.requireNonNull(predicate, "predicate is null");
        // stays on the trie, not the builder: the flat-array filter keeps primitive leaves unboxed and, measured, is
        // 2x faster than the builder at 1 000 elements and equal at 100 000 (the JIT likes the branch-free bulk copies)
        return wrap(trie.filter(predicate));
    }

    @Override
    public Vector<T> reject(Predicate<? super T> predicate) {
        Objects.requireNonNull(predicate, "predicate is null");
        return filter(predicate.negate());
    }

    @Override
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
     * {@inheritDoc}
     * <p>
     * Complexity: O(n), walking the elements from the last to the first without copying.
     */
    @Override
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
     * Complexity: effectively O(1) (O(log32 n) trie access).
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
     * {@inheritDoc}
     * <p>
     * Complexity: effectively O(1).
     */
    @Override
    public T head() {
        if (nonEmpty()) {
            return get(0);
        } else {
            throw new NoSuchElementException("head of empty Vector");
        }
    }

    @Override
    public <C extends @Nullable Object> Map<C, Vector<T>> groupBy(Function<? super T, ? extends C> classifier) { return com.guizmaii.zazr.collection.Collections.groupBy(this, classifier, Vector::ofAll); }

    /**
     * {@inheritDoc}
     * <p>
     * Complexity: lazy; O(n) over all groups when consumed. The iterator element type is decided in #68.
     */
    @Override
    public Iterator<Vector<T>> grouped(int size) { return sliding(size, size); }

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
     *
     * @param element the element to find
     * @return {@code Some(index)} of its first occurrence, or {@code None}
     */
    public Option<Integer> indexOfOption(T element) {
        return com.guizmaii.zazr.collection.Collections.indexOption(indexOf(element));
    }

    /**
     * {@link #indexOf(Object, int)} as an {@link Option}: {@code None} for -1.
     *
     * @param element the element to find
     * @param from    the first position to look at
     * @return {@code Some(index)} of its first occurrence at or after {@code from}, or {@code None}
     */
    public Option<Integer> indexOfOption(T element, int from) {
        return com.guizmaii.zazr.collection.Collections.indexOption(indexOf(element, from));
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
     *
     * @param that the slice to find
     * @return {@code Some(index)} of its first occurrence, or {@code None}
     * @throws NullPointerException if {@code that} is null
     */
    public Option<Integer> indexOfSliceOption(Iterable<? extends T> that) {
        return com.guizmaii.zazr.collection.Collections.indexOption(indexOfSlice(that));
    }

    /**
     * {@link #indexOfSlice(Iterable, int)} as an {@link Option}: {@code None} for -1.
     *
     * @param that the slice to find
     * @param from the first position to look at
     * @return {@code Some(index)} of its first occurrence at or after {@code from}, or {@code None}
     * @throws NullPointerException if {@code that} is null
     */
    public Option<Integer> indexOfSliceOption(Iterable<? extends T> that, int from) {
        return com.guizmaii.zazr.collection.Collections.indexOption(indexOfSlice(that, from));
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
     *
     * @param predicate the condition
     * @return {@code Some(index)} of the first satisfying element, or {@code None}
     * @throws NullPointerException if {@code predicate} is null
     */
    public Option<Integer> indexWhereOption(Predicate<? super T> predicate) {
        return com.guizmaii.zazr.collection.Collections.indexOption(indexWhere(predicate));
    }

    /**
     * {@link #indexWhere(Predicate, int)} as an {@link Option}: {@code None} for -1.
     *
     * @param predicate the condition
     * @param from      the first position to look at
     * @return {@code Some(index)} of the first satisfying element at or after {@code from}, or {@code None}
     * @throws NullPointerException if {@code predicate} is null
     */
    public Option<Integer> indexWhereOption(Predicate<? super T> predicate, int from) {
        return com.guizmaii.zazr.collection.Collections.indexOption(indexWhere(predicate, from));
    }

    /**
     * {@inheritDoc}
     * <p>
     * Complexity: effectively O(1) (the path to the last leaf is trimmed).
     */
    @Override
    public Vector<T> init() {
        if (nonEmpty()) {
            return dropRight(1);
        } else {
            throw new UnsupportedOperationException("init of empty Vector");
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * Complexity: effectively O(1) (one {@code init}).
     */
    @Override
    public Option<Vector<T>> initOption() { return isEmpty() ? Option.none() : Option.some(init()); }

    /**
     * Inserts an element at {@code index}; the elements from that position on shift right by one.
     * <p>
     * Complexity: O(min(i, n - i)): the shorter side is re-appended or re-prepended element by element.
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
     * Complexity: O(m + min(i, n - i)) for m inserted elements.
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
    public Vector<T> intersperse(T element) { return ofAll(iterator().intersperse(element)); }

    @Override
    public boolean isEmpty() { return length() == 0; }

    /**
     * Narrows to a {@link NonEmptyVector}, whose operations that cannot shrink keep that type and whose {@code head},
     * {@code last}, {@code max}, {@code min} and {@code reduce} are total.
     *
     * @return {@code Some(nonEmptyVector)} sharing this Vector's elements, or {@code None} if this Vector is empty
     */
    public Option<NonEmptyVector<T>> toNonEmptyVector() { return NonEmptyVector.fromVector(this); }

    /**
     * {@inheritDoc}
     * <p>
     * Complexity: O(1) to create; each step is O(1) within a leaf and effectively O(1) at a leaf boundary.
     */
    @Override
    public Iterator<T> iterator() {
        return isEmpty() ? Iterator.empty()
                         : trie.iterator();
    }

    /**
     * An iterator over the elements from {@code index} on: {@code subSequence(index).iterator()}.
     * <p>
     * Complexity: effectively O(1) to create.
     *
     * @param index the first position to iterate, {@code 0 <= index <= length()}
     * @return an iterator starting at {@code index}, empty when {@code index == length()}
     * @throws IndexOutOfBoundsException if {@code index} is out of range
     */
    public Iterator<T> iterator(int index) {
        return subSequence(index).iterator();
    }

    /**
     * {@inheritDoc}
     * <p>
     * Complexity: effectively O(1).
     */
    @Override
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
     *
     * @param element the element to find
     * @return {@code Some(index)} of its last occurrence, or {@code None}
     */
    public Option<Integer> lastIndexOfOption(T element) {
        return com.guizmaii.zazr.collection.Collections.indexOption(lastIndexOf(element));
    }

    /**
     * {@link #lastIndexOf(Object, int)} as an {@link Option}: {@code None} for -1.
     *
     * @param element the element to find
     * @param end     the last position to look at
     * @return {@code Some(index)} of its last occurrence at or before {@code end}, or {@code None}
     */
    public Option<Integer> lastIndexOfOption(T element, int end) {
        return com.guizmaii.zazr.collection.Collections.indexOption(lastIndexOf(element, end));
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
     *
     * @param that the slice to find
     * @return {@code Some(index)} of its last occurrence, or {@code None}
     * @throws NullPointerException if {@code that} is null
     */
    public Option<Integer> lastIndexOfSliceOption(Iterable<? extends T> that) {
        return com.guizmaii.zazr.collection.Collections.indexOption(lastIndexOfSlice(that));
    }

    /**
     * {@link #lastIndexOfSlice(Iterable, int)} as an {@link Option}: {@code None} for -1.
     *
     * @param that the slice to find
     * @param end  the last position to look at
     * @return {@code Some(index)} of its last occurrence at or before {@code end}, or {@code None}
     * @throws NullPointerException if {@code that} is null
     */
    public Option<Integer> lastIndexOfSliceOption(Iterable<? extends T> that, int end) {
        return com.guizmaii.zazr.collection.Collections.indexOption(lastIndexOfSlice(that, end));
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
     *
     * @param predicate the condition
     * @return {@code Some(index)} of the last satisfying element, or {@code None}
     * @throws NullPointerException if {@code predicate} is null
     */
    public Option<Integer> lastIndexWhereOption(Predicate<? super T> predicate) {
        return com.guizmaii.zazr.collection.Collections.indexOption(lastIndexWhere(predicate));
    }

    /**
     * {@link #lastIndexWhere(Predicate, int)} as an {@link Option}: {@code None} for -1.
     *
     * @param predicate the condition
     * @param end       the last position to look at
     * @return {@code Some(index)} of the last satisfying element at or before {@code end}, or {@code None}
     * @throws NullPointerException if {@code predicate} is null
     */
    public Option<Integer> lastIndexWhereOption(Predicate<? super T> predicate, int end) {
        return com.guizmaii.zazr.collection.Collections.indexOption(lastIndexWhere(predicate, end));
    }

    @Override
    public int length() { return trie.length(); }

    @Override
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

    @Override
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

    @Override
    public <U extends @Nullable Object> Vector<U> as(U value) {
        return map(ignored -> value);
    }

    @Override
    public Vector<T> orElse(Iterable<? extends T> other) {
        return isEmpty() ? ofAll(other) : this;
    }

    @Override
    public Vector<T> orElse(Supplier<? extends Iterable<? extends T>> supplier) {
        return isEmpty() ? ofAll(supplier.get()) : this;
    }

    /**
     * Appends copies of {@code element} until the Vector has {@code length} elements.
     * <p>
     * Complexity: O(k) for the k elements appended.
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
     * Complexity: O(k) for the k elements prepended.
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
     * Complexity: O(n + m) for m elements of {@code that}.
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

        Vector<T> result = take(from).appendAll(that);
        from += replaced;
        result = result.appendAll(drop(from));
        return result;
    }

    @Override
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

    @Override
    public Vector<T> tap(Consumer<? super T> action) {
        Objects.requireNonNull(action, "action is null");
        forEach(action);
        return this;
    }

    /**
     * All distinct permutations of the elements, in the order the distinct elements first occur.
     * <p>
     * Complexity: O(n! * n) in the worst case (all elements distinct).
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
     * Complexity: effectively O(1) (a path copy; the first leaf is copied).
     *
     * @param element the element to prepend
     * @return a new Vector starting with {@code element}
     * @throws NullPointerException if {@code element} is null
     */
    public Vector<T> prepend(T element) { return prependAll(com.guizmaii.zazr.collection.List.of(element)); }

    /**
     * Prepends all elements of the given iterable, keeping their order.
     * <p>
     * Complexity: O(m) for m prepended elements (one leaf copy per 32 elements plus a path copy); O(1) when this
     * Vector is empty and {@code iterable} is a Vector, which is returned as is.
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
        if (com.guizmaii.zazr.collection.Collections.isEmpty(iterable)){
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
     * Complexity: O(min(i, n - i)): the shorter side is re-appended or re-prepended element by element.
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
        return com.guizmaii.zazr.collection.Collections.removeAll(this, element);
    }

    /**
     * Removes every occurrence of every given element.
     * <p>
     * Complexity: O(n + m) for m given elements.
     *
     * @param elements the elements to remove
     * @return a new Vector without them, or this Vector if none is present
     * @throws NullPointerException if {@code elements} is null
     */
    public Vector<T> removeAll(Iterable<? extends T> elements) {
        return com.guizmaii.zazr.collection.Collections.removeAll(this, elements);
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
     * {@inheritDoc}
     * <p>
     * Complexity: O(n) to find the element, then one effectively O(1) {@code update}.
     */
    @Override
    public Vector<T> replace(T currentElement, T newElement) {
        return indexOfOption(currentElement)
                .map(i -> update(i, newElement))
                .getOrElse(this);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Complexity: O(n) plus one effectively O(1) {@code update} per occurrence.
     */
    @Override
    public Vector<T> replaceAll(T currentElement, T newElement) {
        Vector<T> result = this;
        int index = 0;
        for (T value : iterator()) {
            if (Objects.equals(value, currentElement)) {
                result = result.update(index, newElement);
            }
            index++;
        }
        return result;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Complexity: O(n + m) for m retained elements (they are hashed once, then one filter pass).
     */
    @Override
    public Vector<T> retainAll(Iterable<? extends T> elements) {
        return com.guizmaii.zazr.collection.Collections.retainAll(this, elements);
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
    public Iterator<T> reverseIterator() {
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
     * Complexity: O(k) for the k = n mod length elements moved to the end.
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
     * Complexity: O(length - k) for k = n mod length: the elements before the moved suffix are re-appended.
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
     * {@inheritDoc}
     * <p>
     * Complexity: O(n).
     */
    @Override
    public Vector<T> scan(T zero, BiFunction<? super T, ? super T, ? extends T> operation) {
        return scanLeft(zero, operation);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Complexity: O(n).
     */
    @Override
    public <U extends @Nullable Object> Vector<U> scanLeft(U zero, BiFunction<? super U, ? super T, ? extends U> operation) {
        return com.guizmaii.zazr.collection.Collections.scanLeft(this, zero, operation, Iterator::toVector);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Complexity: O(n).
     */
    @Override
    public <U extends @Nullable Object> Vector<U> scanRight(U zero, BiFunction<? super T, ? super U, ? extends U> operation) {
        return com.guizmaii.zazr.collection.Collections.scanRight(this, zero, operation, Iterator::toVector);
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
        return com.guizmaii.zazr.collection.Collections.shuffle(this, Vector::ofAll);
    }

    /**
     * The elements from {@code beginIndex} (inclusive) to {@code endIndex} (exclusive). Out-of-range indices are
     * clamped, and an empty or reversed range gives the empty Vector: {@code Vector(1, 2).slice(-10, 10)} is the
     * whole Vector, {@code slice(1, 0)} is empty. {@link #subSequence(int, int)} throws instead of clamping.
     * <p>
     * Complexity: effectively O(1) (the paths to the new first and last leaves are trimmed).
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
     * {@inheritDoc}
     * <p>
     * Complexity: lazy; O(n) over all groups when consumed. The iterator element type is decided in #68.
     */
    @Override
    public Iterator<Vector<T>> slideBy(Function<? super T, ?> classifier) {
        return iterator().slideBy(classifier).map(Vector::ofAll);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Complexity: lazy; O(size) per window when consumed. The iterator element type is decided in #68.
     */
    @Override
    public Iterator<Vector<T>> sliding(int size) {
        return sliding(size, 1);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Complexity: lazy; O(size) per window when consumed. The iterator element type is decided in #68.
     */
    @Override
    public Iterator<Vector<T>> sliding(int size, int step) {
        return iterator().sliding(size, step).map(Vector::ofAll);
    }

    /**
     * The elements sorted in natural order (a stable sort).
     * <p>
     * Complexity: O(n log n) comparisons; the elements are copied to an array, sorted there and regrouped into leaves.
     *
     * @return a new sorted Vector, or this Vector if it is empty
     * @throws ClassCastException if the elements are not {@link Comparable}
     */
    public Vector<T> sorted() {
        if (isEmpty()) {
            return this;
        } else {
            @SuppressWarnings("unchecked")
            final T[] list = (T[]) toJavaArray();
            Arrays.sort(list);
            return Vector.of(list);
        }
    }

    /**
     * The elements sorted by {@code comparator} (a stable sort).
     * <p>
     * Complexity: O(n log n) comparisons; the elements are copied to an array, sorted there and regrouped into leaves.
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
        final T[] array = (T[]) toJavaArray();
        Arrays.sort(array, comparator);
        return Vector.of(array);
    }

    /**
     * The elements sorted by the natural order of the key {@code mapper} computes (a stable sort).
     * <p>
     * Complexity: O(n log n) comparisons; the key is recomputed at every comparison.
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
     * Complexity: O(n log n) comparisons; the key is recomputed at every comparison.
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
     * {@inheritDoc}
     * <p>
     * Complexity: O(k) for k elements before the split, then an effectively O(1) split.
     */
    @Override
    public Tuple2<Vector<T>, Vector<T>> span(Predicate<? super T> predicate) {
        Objects.requireNonNull(predicate, "predicate is null");
        return Tuple.of(takeWhile(predicate), dropWhile(predicate));
    }

    /**
     * Splits at {@code n}: {@code (take(n), drop(n))}.
     * <p>
     * Complexity: effectively O(1).
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
     * Complexity: effectively O(1).
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
     * Complexity: effectively O(1).
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
     * {@inheritDoc}
     * <p>
     * Complexity: effectively O(1) (the path to the first leaf is trimmed).
     */
    @Override
    public Vector<T> tail() {
        if (nonEmpty()) {
            return drop(1);
        } else {
            throw new UnsupportedOperationException("tail of empty Vector");
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * Complexity: effectively O(1) (one {@code tail}).
     */
    @Override
    public Option<Vector<T>> tailOption() { return isEmpty() ? Option.none() : Option.some(tail()); }

    /**
     * {@inheritDoc}
     * <p>
     * Complexity: effectively O(1) (the path to the new last leaf is trimmed).
     */
    @Override
    public Vector<T> take(int n) {
        return wrap(trie.take(n));
    }

    /**
     * {@inheritDoc}
     * <p>
     * Complexity: O(k) for k taken elements, then one effectively O(1) {@code take}.
     */
    @Override
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
     * {@inheritDoc}
     * <p>
     * Complexity: O(k) for k taken elements, then one effectively O(1) {@code take}.
     */
    @Override
    public Vector<T> takeWhile(Predicate<? super T> predicate) {
        Objects.requireNonNull(predicate, "predicate is null");
        return takeUntil(predicate.negate());
    }

    /**
     * {@inheritDoc}
     * <p>
     * Complexity: effectively O(1) (the path to the new first leaf is trimmed).
     */
    @Override
    public Vector<T> takeRight(int n) {
        return drop(length() - n);
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
     * Complexity: effectively O(1) (a path copy; the leaf holding the element is copied).
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
     * Complexity: effectively O(1) (one access and one path copy).
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
     * {@inheritDoc}
     * <p>
     * Complexity: O(min(n, m)) for m elements of {@code that}.
     */
    @Override
    public <U extends @Nullable Object> Vector<Tuple2<T, U>> zip(Iterable<? extends U> that) {
        return zipWith(that, Tuple::of);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Complexity: O(min(n, m)) for m elements of {@code that}.
     */
    @Override
    public <U extends @Nullable Object, R extends @Nullable Object> Vector<R> zipWith(Iterable<? extends U> that, BiFunction<? super T, ? super U, ? extends R> mapper) {
        Objects.requireNonNull(that, "that is null");
        Objects.requireNonNull(mapper, "mapper is null");
        return ofAll(iterator().zipWith(that, mapper));
    }

    /**
     * {@inheritDoc}
     * <p>
     * Complexity: O(max(n, m)) for m elements of {@code that}.
     */
    @Override
    public <U extends @Nullable Object> Vector<Tuple2<T, U>> zipAll(Iterable<? extends U> that, T thisElem, U thatElem) {
        Objects.requireNonNull(that, "that is null");
        return ofAll(iterator().zipAll(that, thisElem, thatElem));
    }

    /**
     * {@inheritDoc}
     * <p>
     * Complexity: O(n).
     */
    @Override
    public Vector<Tuple2<T, Integer>> zipWithIndex() {
        return zipWithIndex(Tuple::of);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Complexity: O(n).
     */
    @Override
    public <U extends @Nullable Object> Vector<U> zipWithIndex(BiFunction<? super T, ? super Integer, ? extends U> mapper) {
        Objects.requireNonNull(mapper, "mapper is null");
        return ofAll(iterator().zipWithIndex(mapper));
    }

    /**
     * Whether {@code o} is a sequence with equal elements in the same order: another Vector, or, until #68 decides
     * the final rule, one of the other ordered sequence types.
     *
     * @param o any object
     * @return true if {@code o} is an ordered sequence of the same elements
     */
    @Override
    public boolean equals(@Nullable Object o) {
        return com.guizmaii.zazr.collection.Collections.equals(this, o);
    }

    @Override
    public int hashCode() {
        return com.guizmaii.zazr.collection.Collections.hashOrdered(this);
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
}

interface VectorModule {
    final class Combinations {
        static <T extends @Nullable Object> Vector<Vector<T>> apply(Vector<T> elements, int k) {
            return (k == 0)
                   ? Vector.of(Vector.empty())
                   : elements.zipWithIndex().flatMap(
                    t -> apply(elements.drop(t._2() + 1), (k - 1)).map((Vector<T> c) -> c.prepend(t._1())));
        }
    }

    /* contiguous-slice search by index; the slice is materialised once (O(1) when it already is a Vector) */
    final class Slice {

        static <T extends @Nullable Object> int indexOfSlice(Vector<T> source, Iterable<? extends T> slice, int from) {
            if (source.isEmpty()) {
                return from == 0 && Collections.isEmpty(slice) ? 0 : -1;
            }
            final Vector<? extends T> _slice = Vector.ofAll(slice);
            final int maxIndex = source.length() - _slice.length();
            return findSlice(source, _slice, Math.max(from, 0), maxIndex);
        }

        static <T extends @Nullable Object> int lastIndexOfSlice(Vector<T> source, Iterable<? extends T> slice, int end) {
            if (end < 0) {
                return -1;
            } else if (source.isEmpty()) {
                return Collections.isEmpty(slice) ? 0 : -1;
            } else if (Collections.isEmpty(slice)) {
                final int len = source.length();
                return len < end ? len : end;
            }
            int index = 0;
            int result = -1;
            final Vector<? extends T> _slice = Vector.ofAll(slice);
            final int maxIndex = source.length() - _slice.length();
            while (index <= maxIndex) {
                int indexOfSlice = findSlice(source, _slice, index, maxIndex);
                if (indexOfSlice < 0) {
                    return result;
                }
                if (indexOfSlice <= end) {
                    result = indexOfSlice;
                    index = indexOfSlice + 1;
                } else {
                    return result;
                }
            }
            return result;
        }

        private static <T extends @Nullable Object> int findSlice(Vector<T> source, Vector<? extends T> slice, int index, int maxIndex) {
            while (index <= maxIndex) {
                if (source.startsWith(slice, index)) {
                    return index;
                }
                index++;
            }
            return -1;
        }
    }

    /* binary search over the indices; `comparison` compares the element at an index with the searched element */
    final class Search {

        static int binarySearch(Vector<?> vector, IntUnaryOperator comparison) {
            int low = 0;
            int high = vector.length() - 1;
            while (low <= high) {
                final int mid = (low + high) >>> 1;
                final int cmp = comparison.applyAsInt(mid);
                if (cmp < 0) {
                    low = mid + 1;
                } else if (cmp > 0) {
                    high = mid - 1;
                } else {
                    return mid;
                }
            }
            return -(low + 1);
        }
    }
}
