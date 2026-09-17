package com.guizmaii.zazr.collection;

import com.guizmaii.zazr.*;
import com.guizmaii.zazr.collection.JavaConverters.ListView;
import com.guizmaii.zazr.collection.VectorModule.Combinations;
import com.guizmaii.zazr.control.Option;
import java.util.*;
import java.util.function.*;
import java.util.stream.Collector;
import org.jspecify.annotations.Nullable;

import static com.guizmaii.zazr.collection.Collections.withSize;
import static com.guizmaii.zazr.collection.JavaConverters.ChangePolicy.IMMUTABLE;
import static com.guizmaii.zazr.collection.JavaConverters.ChangePolicy.MUTABLE;

/**
 * Vector is the default Seq implementation that provides effectively constant time access to any element.
 * Many other operations (e.g. `tail`, `drop`, `slice`) are also effectively constant.
 *
 * The implementation is based on a `bit-mapped trie`, a very wide and shallow tree (i.e. depth ≤ 6).
 *
 * @param <T> Component type of the Vector.
 * @author Ruslan Sennov, Pap Lőrinc
 */
public final class Vector<T extends @Nullable Object> implements IndexedSeq<T> {

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

    @Override
    public Vector<T> append(T element) { return appendAll(com.guizmaii.zazr.collection.List.of(element)); }

    @Override
    public Vector<T> appendAll(Iterable<? extends T> iterable) {
        Objects.requireNonNull(iterable, "iterable is null");
        if (isEmpty()) {
            return ofAll(iterable);
        }
        if (com.guizmaii.zazr.collection.Collections.isEmpty(iterable)){
            return this;
        }
        if (!com.guizmaii.zazr.collection.Collections.isTraversableAgain(iterable)) {
            // a one-shot source (a Vavr Iterator, typically wrapping a java.util.stream): build it once with the builder, then append by path copy
            return appendAll(ofAll(iterable));
        }
        return new Vector<>(trie.appendAll(iterable));
    }

    @Override
    public java.util.List<T> asJava() {
        return JavaConverters.asJava(this, IMMUTABLE);
    }

    @Override
    public Vector<T> asJava(Consumer<? super java.util.List<T>> action) {
        return Collections.asJava(this, action, IMMUTABLE);
    }

    @Override
    public java.util.List<T> asJavaMutable() {
        return JavaConverters.asJava(this, MUTABLE);
    }

    @Override
    public Vector<T> asJavaMutable(Consumer<? super java.util.List<T>> action) {
        return Collections.asJava(this, action, MUTABLE);
    }

    @Override
    public Vector<Vector<T>> combinations() { return rangeClosed(0, length()).map(this::combinations).flatMap(Function.identity()); }

    @Override
    public Vector<Vector<T>> combinations(int k) { return Combinations.apply(this, Math.max(k, 0)); }

    @Override
    public Iterator<Vector<T>> crossProduct(int power) { return com.guizmaii.zazr.collection.Collections.crossProduct(empty(), this, power); }

    @Override
    public Vector<T> distinct() { return distinctBy(Function.identity()); }

    @Override
    public Vector<T> distinctBy(Comparator<? super T> comparator) {
        Objects.requireNonNull(comparator, "comparator is null");
        final java.util.Set<T> seen = new java.util.TreeSet<>(comparator);
        return filter(seen::add);
    }

    @Override
    public <U extends @Nullable Object> Vector<T> distinctBy(Function<? super T, ? extends U> keyExtractor) {
        Objects.requireNonNull(keyExtractor, "keyExtractor is null");
        final java.util.Set<U> seen = new java.util.HashSet<>(length());
        return filter(t -> seen.add(keyExtractor.apply(t)));
    }

    @Override
    public Vector<T> distinctByKeepLast(Comparator<? super T> comparator) {
        Objects.requireNonNull(comparator, "comparator is null");
        return ofAll(iterator().distinctByKeepLast(comparator));
    }

    @Override
    public <U extends @Nullable Object> Vector<T> distinctByKeepLast(Function<? super T, ? extends U> keyExtractor) {
        Objects.requireNonNull(keyExtractor, "keyExtractor is null");
        return ofAll(iterator().distinctByKeepLast(keyExtractor));
    }

    @Override
    public Vector<T> drop(int n) {
        return wrap(trie.drop(n));
    }

    @Override
    public Vector<T> dropUntil(Predicate<? super T> predicate) {
        return com.guizmaii.zazr.collection.Collections.dropUntil(this, predicate);
    }

    @Override
    public Vector<T> dropWhile(Predicate<? super T> predicate) {
        Objects.requireNonNull(predicate, "predicate is null");
        return dropUntil(predicate.negate());
    }

    @Override
    public Vector<T> dropRight(int n) {
        return take(length() - n);
    }

    @Override
    public Vector<T> dropRightUntil(Predicate<? super T> predicate) {
        return com.guizmaii.zazr.collection.Collections.dropRightUntil(this, predicate);
    }

    @Override
    public Vector<T> dropRightWhile(Predicate<? super T> predicate) {
        Objects.requireNonNull(predicate, "predicate is null");
        return dropRightUntil(predicate.negate());
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

    @Override
    public T get(int index) {
        if (isValid(index)) {
            return trie.get(index);
        } else {
            throw new IndexOutOfBoundsException("get(" + index + ")");
        }
    }
    private boolean isValid(int index) { return (index >= 0) && (index < length()); }

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

    @Override
    public Iterator<Vector<T>> grouped(int size) { return sliding(size, size); }

    @Override
    public int indexOf(T element, int from) {
        for (int i = Math.max(from, 0); i < length(); i++) {
            if (Objects.equals(get(i), element)) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public Vector<T> init() {
        if (nonEmpty()) {
            return dropRight(1);
        } else {
            throw new UnsupportedOperationException("init of empty Vector");
        }
    }

    @Override
    public Option<Vector<T>> initOption() { return isEmpty() ? Option.none() : Option.some(init()); }

    @Override
    public Vector<T> insert(int index, T element) { return insertAll(index, Iterator.of(element)); }

    @Override
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

    @Override
    public Vector<T> intersperse(T element) { return ofAll(iterator().intersperse(element)); }

    @Override
    public boolean isEmpty() { return length() == 0; }

    @Override
    public Iterator<T> iterator() {
        return isEmpty() ? Iterator.empty()
                         : trie.iterator();
    }

    @Override
    public int lastIndexOf(T element, int end) {
        for (int i = Math.min(end, length() - 1); i >= 0; i--) {
            if (Objects.equals(get(i), element)) {
                return i;
            }
        }
        return -1;
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

    @Override
    public Vector<T> padTo(int length, T element) {
        final int actualLength = length();
        return (length <= actualLength)
               ? this
               : appendAll(Iterator.continually(element)
                .take(length - actualLength));
    }

    @Override
    public Vector<T> leftPadTo(int length, T element) {
        if (length <= length()) {
            return this;
        } else {
            final Iterator<T> prefix = Iterator.continually(element).take(length - length());
            return prependAll(prefix);
        }
    }

    @Override
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

    @Override
    public Vector<T> tap(Consumer<? super T> action) {
        Objects.requireNonNull(action, "action is null");
        forEach(action);
        return this;
    }

    @Override
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

    @Override
    public Vector<T> prepend(T element) { return prependAll(com.guizmaii.zazr.collection.List.of(element)); }

    @Override
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

    @Override
    public Vector<T> remove(T element) {
        for (int i = 0; i < length(); i++) {
            if (Objects.equals(get(i), element)) {
                return removeAt(i);
            }
        }
        return this;
    }

    @Override
    public Vector<T> removeFirst(Predicate<T> predicate) {
        Objects.requireNonNull(predicate, "predicate is null");
        for (int i = 0; i < length(); i++) {
            if (predicate.test(get(i))) {
                return removeAt(i);
            }
        }
        return this;
    }

    @Override
    public Vector<T> removeLast(Predicate<T> predicate) {
        Objects.requireNonNull(predicate, "predicate is null");
        for (int i = length() - 1; i >= 0; i--) {
            if (predicate.test(get(i))) {
                return removeAt(i);
            }
        }
        return this;
    }

    @Override
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

    @Override
    public Vector<T> removeAll(T element) {
        return com.guizmaii.zazr.collection.Collections.removeAll(this, element);
    }

    @Override
    public Vector<T> removeAll(Iterable<? extends T> elements) {
        return com.guizmaii.zazr.collection.Collections.removeAll(this, elements);
    }

    @Override
    @Deprecated
    public Vector<T> removeAll(Predicate<? super T> predicate) {
        Objects.requireNonNull(predicate, "predicate is null");
        return reject(predicate);
    }

    @Override
    public Vector<T> replace(T currentElement, T newElement) {
        return indexOfOption(currentElement)
                .map(i -> update(i, newElement))
                .getOrElse(this);
    }

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

    @Override
    public Vector<T> retainAll(Iterable<? extends T> elements) {
        return com.guizmaii.zazr.collection.Collections.retainAll(this, elements);
    }

    @Override
    public Vector<T> reverse() {
        return (length() <= 1) ? this : ofAll(reverseIterator());
    }

    @Override
    public Vector<T> rotateLeft(int n) {
        return Collections.rotateLeft(this, n);
    }

    @Override
    public Vector<T> rotateRight(int n) {
        return Collections.rotateRight(this, n);
    }

    @Override
    public Vector<T> scan(T zero, BiFunction<? super T, ? super T, ? extends T> operation) {
        return scanLeft(zero, operation);
    }

    @Override
    public <U extends @Nullable Object> Vector<U> scanLeft(U zero, BiFunction<? super U, ? super T, ? extends U> operation) {
        return com.guizmaii.zazr.collection.Collections.scanLeft(this, zero, operation, Iterator::toVector);
    }

    @Override
    public <U extends @Nullable Object> Vector<U> scanRight(U zero, BiFunction<? super T, ? super U, ? extends U> operation) {
        return com.guizmaii.zazr.collection.Collections.scanRight(this, zero, operation, Iterator::toVector);
    }

    @Override
    public Vector<T> shuffle() {
        return com.guizmaii.zazr.collection.Collections.shuffle(this, Vector::ofAll);
    }

    @Override
    public Vector<T> slice(int beginIndex, int endIndex) {
        if ((beginIndex >= endIndex) || (beginIndex >= size()) || isEmpty()) {
            return empty();
        } else if ((beginIndex <= 0) && (endIndex >= length())) {
            return this;
        } else {
            return take(endIndex).drop(beginIndex);
        }
    }

    @Override
    public Iterator<Vector<T>> slideBy(Function<? super T, ?> classifier) {
        return iterator().slideBy(classifier).map(Vector::ofAll);
    }

    @Override
    public Iterator<Vector<T>> sliding(int size) {
        return sliding(size, 1);
    }

    @Override
    public Iterator<Vector<T>> sliding(int size, int step) {
        return iterator().sliding(size, step).map(Vector::ofAll);
    }

    @Override
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

    @Override
    public Vector<T> sorted(Comparator<? super T> comparator) {
        Objects.requireNonNull(comparator, "comparator is null");
        return isEmpty() ? this : toJavaStream().sorted(comparator).collect(collector());
    }

    @Override
    public <U extends Comparable<? super U>> Vector<T> sortBy(Function<? super T, ? extends U> mapper) {
        return sortBy(U::compareTo, mapper);
    }

    @Override
    public <U extends @Nullable Object> Vector<T> sortBy(Comparator<? super U> comparator, Function<? super T, ? extends U> mapper) {
        return Collections.sortBy(this, comparator, mapper, collector());
    }

    @Override
    public Tuple2<Vector<T>, Vector<T>> span(Predicate<? super T> predicate) {
        Objects.requireNonNull(predicate, "predicate is null");
        return Tuple.of(takeWhile(predicate), dropWhile(predicate));
    }

    @Override
    public Tuple2<Vector<T>, Vector<T>> splitAt(int n) {
        return Tuple.of(take(n), drop(n));
    }

    @Override
    public Tuple2<Vector<T>, Vector<T>> splitAt(Predicate<? super T> predicate) {
        Objects.requireNonNull(predicate, "predicate is null");
        final Vector<T> init = takeWhile(predicate.negate());
        return Tuple.of(init, drop(init.size()));
    }

    @Override
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

    @Override
    public Vector<T> subSequence(int beginIndex) {
        if ((beginIndex >= 0) && (beginIndex <= length())) {
            return drop(beginIndex);
        } else {
            throw new IndexOutOfBoundsException("subSequence(" + beginIndex + ")");
        }
    }

    @Override
    public Vector<T> subSequence(int beginIndex, int endIndex) {
        Collections.subSequenceRangeCheck(beginIndex, endIndex, length());
        return slice(beginIndex, endIndex);
    }

    @Override
    public Vector<T> tail() {
        if (nonEmpty()) {
            return drop(1);
        } else {
            throw new UnsupportedOperationException("tail of empty Vector");
        }
    }

    @Override
    public Option<Vector<T>> tailOption() { return isEmpty() ? Option.none() : Option.some(tail()); }

    @Override
    public Vector<T> take(int n) {
        return wrap(trie.take(n));
    }

    @Override
    public Vector<T> takeUntil(Predicate<? super T> predicate) {
        return com.guizmaii.zazr.collection.Collections.takeUntil(this, predicate);
    }

    @Override
    public Vector<T> takeWhile(Predicate<? super T> predicate) {
        Objects.requireNonNull(predicate, "predicate is null");
        return takeUntil(predicate.negate());
    }

    @Override
    public Vector<T> takeRight(int n) {
        return drop(length() - n);
    }

    @Override
    public Vector<T> takeRightUntil(Predicate<? super T> predicate) {
        return com.guizmaii.zazr.collection.Collections.takeRightUntil(this, predicate);
    }

    @Override
    public Vector<T> takeRightWhile(Predicate<? super T> predicate) {
        Objects.requireNonNull(predicate, "predicate is null");
        return takeRightUntil(predicate.negate());
    }

    @Override
    public <T1 extends @Nullable Object, T2 extends @Nullable Object> Tuple2<Vector<T1>, Vector<T2>> unzip(Function<? super T, Tuple2<? extends T1, ? extends T2>> unzipper) {
        Objects.requireNonNull(unzipper, "unzipper is null");
        Vector<T1> xs = empty();
        Vector<T2> ys = empty();
        for (int i = 0; i < length(); i++) {
            final Tuple2<? extends T1, ? extends T2> t = unzipper.apply(get(i));
            xs = xs.append(t._1());
            ys = ys.append(t._2());
        }
        return Tuple.of(xs, ys);
    }

    @Override
    public <T1 extends @Nullable Object, T2 extends @Nullable Object, T3 extends @Nullable Object> Tuple3<Vector<T1>, Vector<T2>, Vector<T3>> unzip3(Function<? super T, Tuple3<? extends T1, ? extends T2, ? extends T3>> unzipper) {
        Objects.requireNonNull(unzipper, "unzipper is null");
        Vector<T1> xs = empty();
        Vector<T2> ys = empty();
        Vector<T3> zs = empty();
        for (int i = 0; i < length(); i++) {
            final Tuple3<? extends T1, ? extends T2, ? extends T3> t = unzipper.apply(get(i));
            xs = xs.append(t._1());
            ys = ys.append(t._2());
            zs = zs.append(t._3());
        }
        return Tuple.of(xs, ys, zs);
    }

    @Override
    public Vector<T> update(int index, T element) {
        if (isValid(index)) {
            return wrap(trie.update(index, element));
        } else {
            throw new IndexOutOfBoundsException("update(" + index + ")");
        }
    }

    @Override
    public Vector<T> update(int index, Function<? super T, ? extends T> updater) {
        Objects.requireNonNull(updater, "updater is null");
        return update(index, updater.apply(get(index)));
    }

    @Override
    public <U extends @Nullable Object> Vector<Tuple2<T, U>> zip(Iterable<? extends U> that) {
        return zipWith(that, Tuple::of);
    }

    @Override
    public <U extends @Nullable Object, R extends @Nullable Object> Vector<R> zipWith(Iterable<? extends U> that, BiFunction<? super T, ? super U, ? extends R> mapper) {
        Objects.requireNonNull(that, "that is null");
        Objects.requireNonNull(mapper, "mapper is null");
        return ofAll(iterator().zipWith(that, mapper));
    }

    @Override
    public <U extends @Nullable Object> Vector<Tuple2<T, U>> zipAll(Iterable<? extends U> that, T thisElem, U thatElem) {
        Objects.requireNonNull(that, "that is null");
        return ofAll(iterator().zipAll(that, thisElem, thatElem));
    }

    @Override
    public Vector<Tuple2<T, Integer>> zipWithIndex() {
        return zipWithIndex(Tuple::of);
    }

    @Override
    public <U extends @Nullable Object> Vector<U> zipWithIndex(BiFunction<? super T, ? super Integer, ? extends U> mapper) {
        Objects.requireNonNull(mapper, "mapper is null");
        return ofAll(iterator().zipWithIndex(mapper));
    }

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

}
