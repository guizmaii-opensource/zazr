package com.guizmaii.zazr.collection;

import com.guizmaii.zazr.*;
import com.guizmaii.zazr.collection.Stream.Cons;
import com.guizmaii.zazr.collection.Stream.Empty;
import com.guizmaii.zazr.collection.internal.AbstractIterator;
import com.guizmaii.zazr.collection.internal.Collections;
import com.guizmaii.zazr.collection.internal.Iterator;
import com.guizmaii.zazr.collection.internal.JavaConverters;
import com.guizmaii.zazr.collection.internal.StreamModule;
import com.guizmaii.zazr.collection.internal.StreamModule.*;
import com.guizmaii.zazr.collection.internal.TraversableModule;
import com.guizmaii.zazr.control.Either;
import com.guizmaii.zazr.control.Option;
import java.io.*;
import java.util.*;
import java.util.function.*;
import java.util.stream.Collector;
import java.util.stream.StreamSupport;
import org.jspecify.annotations.Nullable;

import static com.guizmaii.zazr.collection.internal.JavaConverters.ChangePolicy.IMMUTABLE;
import static com.guizmaii.zazr.collection.internal.JavaConverters.ChangePolicy.MUTABLE;
import static com.guizmaii.zazr.collection.internal.JavaConverters.ListView;

/**
 * An immutable {@code Stream} is lazy sequence of elements which may be infinitely long.
 * Its immutability makes it suitable for concurrent programming.
 * <p>
 * A {@code Stream} is composed of a {@code head} element and a lazy evaluated {@code tail} {@code Stream}.
 * <p>
 * There are two implementations of the {@code Stream} interface:
 *
 * <ul>
 * <li>{@link Empty}, which represents the empty {@code Stream}.</li>
 * <li>{@link Cons}, which represents a {@code Stream} containing one or more elements.</li>
 * </ul>
 *
 * Methods to obtain a {@code Stream}:
 *
 * <pre>
 * {@code
 * // factory methods
 * Stream.empty()                  // = Stream.of() = Empty.instance()
 * Stream.of(x)                    // = Stream.cons(x, Stream::empty)
 * Stream.of(Object...)            // e.g. Stream.of(1, 2, 3)
 * Stream.ofAll(Iterable)          // e.g. Stream.ofAll(List.of(1, 2, 3)) = 1, 2, 3
 * Stream.ofAll(<primitive array>) // e.g. Stream.ofAll(1, 2, 3) = 1, 2, 3
 *
 * // int sequences
 * Stream.from(0)                  // = 0, 1, 2, 3, ...
 * Stream.range(0, 3)              // = 0, 1, 2
 * Stream.rangeClosed(0, 3)        // = 0, 1, 2, 3
 *
 * // generators
 * Stream.cons(Object, Supplier)   // e.g. Stream.cons(current, () -> next(current));
 * Stream.continually(Supplier)    // e.g. Stream.continually(Math::random);
 * Stream.iterate(Object, Function)// e.g. Stream.iterate(1, i -> i * 2);
 * }
 * </pre>
 *
 * Factory method applications:
 *
 * <pre>
 * {@code
 * Stream<Integer>       s1 = Stream.of(1);
 * Stream<Integer>       s2 = Stream.of(1, 2, 3);
 *                       // = Stream.of(new Integer[] {1, 2, 3});
 *
 * Stream<int[]>         s3 = Stream.of(new int[] {1, 2, 3});
 * Stream<List<Integer>> s4 = Stream.of(List.of(1, 2, 3));
 *
 * Stream<Integer>       s5 = Stream.ofAll(1, 2, 3);
 * Stream<Integer>       s6 = Stream.ofAll(List.of(1, 2, 3));
 *
 * // cuckoo's egg
 * Stream<Integer[]>     s7 = Stream.<Integer[]> of(new Integer[] {1, 2, 3});
 * }
 * </pre>
 *
 * Example: Generating prime numbers
 *
 * <pre>
 * {@code
 * // = Stream(2L, 3L, 5L, 7L, ...)
 * Stream.iterate(2L, PrimeNumbers::nextPrimeFrom)
 *
 * // helpers
 *
 * static long nextPrimeFrom(long num) {
 *     return Stream.from(num + 1).find(PrimeNumbers::isPrime).get();
 * }
 *
 * static boolean isPrime(long num) {
 *     return !Stream.rangeClosed(2L, (long) Math.sqrt(num)).exists(d -> num % d == 0);
 * }
 * }
 * </pre>
 *
 * See Okasaki, Chris: <em>Purely Functional Data Structures</em> (p. 34 ff.). Cambridge, 2003.
 *
 * @param <T> component type of this Stream
 * @author Daniel Dietrich, Jörgen Andersson, Ruslan Sennov
 */
public interface Stream<T extends @Nullable Object> extends Traversable<T> {

    /**
     * Returns a {@link java.util.stream.Collector} which may be used in conjunction with
     * {@link java.util.stream.Stream#collect(java.util.stream.Collector)} to obtain a {@link Stream}.
     *
     * @param <T> Component type of the Stream.
     * @return A com.guizmaii.zazr.collection.Stream Collector.
     */
    static <T extends @Nullable Object> Collector<T, ArrayList<T>, Stream<T>> collector() {
        final Supplier<ArrayList<T>> supplier = ArrayList::new;
        final BiConsumer<ArrayList<T>, T> accumulator = ArrayList::add;
        final BinaryOperator<ArrayList<T>> combiner = (left, right) -> {
            left.addAll(right);
            return left;
        };
        final Function<ArrayList<T>, Stream<T>> finisher = Stream::ofAll;
        return Collector.of(supplier, accumulator, combiner, finisher);
    }

    /**
     * Creates a Stream which traverses along the concatenation of the given iterables.
     * <p>
     * Building the Stream is O(k) in the number of given iterables, since an iterator is eagerly
     * obtained from every one of them up front; only the traversal of the elements is lazy.
     *
     * @param iterables The iterables
     * @param <T>       Component type.
     * @return A new {@code Stream}
     */
    @SuppressWarnings("varargs")
    @SafeVarargs
    static <T extends @Nullable Object> Stream<T> concat(Iterable<? extends T> ... iterables) {
        return Iterator.concat(iterables).toStream();
    }

    /**
     * Creates a Stream which traverses along the concatenation of the given iterables.
     * <p>
     * The outer iterable is fully traversed and an iterator is eagerly obtained from every element
     * up front, so it must be finite (an infinite outer iterable causes this call to never return);
     * only the traversal of the resulting elements is lazy.
     *
     * @param iterables The iterable of iterables
     * @param <T>       Component type.
     * @return A new {@code Stream}
     */
    static <T extends @Nullable Object> Stream<T> concat(Iterable<? extends Iterable<? extends T>> iterables) {
        return Iterator.concat(iterables).toStream();
    }

    /**
     * Concatenates nested iterables into one lazy Stream. Static, like every {@code flatten} in zazr, because Java
     * cannot demand of an instance method that the receiver's element type be a collection. Unlike
     * {@link #concat(Iterable)}, the outer iterable is read lazily too: an inner iterable is opened only when the
     * result reaches it, so an infinite outer iterable, or an infinite inner one, is accepted. The outer iterable and
     * each inner one are iterated once, so one-shot iterables are accepted.
     * <p>
     * Complexity: lazy; the first element is found when this method is called (skipping the empty inner iterables
     * before it), each further one when the result reaches it. An outer iterable with infinitely many empty inner
     * ones and no element after them never yields, so the call does not return.
     *
     * @param nested Iterables of elements
     * @param <T>    Component type of the inner iterables
     * @return the inner elements, in order
     * @throws NullPointerException if {@code nested} is null, or when the result reaches a null inner iterable or a
     *                              null element
     */
    static <T extends @Nullable Object> Stream<T> flatten(Iterable<? extends Iterable<? extends T>> nested) {
        Objects.requireNonNull(nested, "nested is null");
        return StreamFactory.create(new FlatMapIterator<>(Iterator.ofAll(nested), Function.identity()));
    }

    /**
     * Returns an infinitely long Stream of {@code int} values starting from {@code value}.
     * <p>
     * The {@code Stream} extends to {@code Integer.MIN_VALUE} when passing {@code Integer.MAX_VALUE}.
     *
     * @param value a start int value
     * @return a new Stream of int values starting from {@code value}
     */
    static Stream<Integer> from(int value) {
        return Stream.ofAll(Iterator.from(value));
    }

    /**
     * Returns an infinite long Stream of {@code int} values starting from {@code value} and spaced by {@code step}.
     * <p>
     * The {@code Stream} extends to {@code Integer.MIN_VALUE} when passing {@code Integer.MAX_VALUE}.
     *
     * @param value a start int value
     * @param step  the step by which to advance on each next value
     * @return a new {@code Stream} of int values starting from {@code value}
     */
    static Stream<Integer> from(int value, int step) {
        return Stream.ofAll(Iterator.from(value, step));
    }

    /**
     * Returns an infinitely long Stream of {@code long} values starting from {@code value}.
     * <p>
     * The {@code Stream} extends to {@code Long.MIN_VALUE} when passing {@code Long.MAX_VALUE}.
     *
     * @param value a start long value
     * @return a new Stream of long values starting from {@code value}
     */
    static Stream<Long> from(long value) {
        return Stream.ofAll(Iterator.from(value));
    }

    /**
     * Returns an infinite long Stream of {@code long} values starting from {@code value} and spaced by {@code step}.
     * <p>
     * The {@code Stream} extends to {@code Long.MIN_VALUE} when passing {@code Long.MAX_VALUE}.
     *
     * @param value a start long value
     * @param step  the step by which to advance on each next value
     * @return a new {@code Stream} of long values starting from {@code value}
     */
    static Stream<Long> from(long value, long step) {
        return Stream.ofAll(Iterator.from(value, step));
    }

    /**
     * Generates a (theoretically) infinitely long Stream using a value Supplier.
     *
     * @param supplier A Supplier of Stream values
     * @param <T>      value type
     * @return A new Stream
     */
    static <T extends @Nullable Object> Stream<T> continually(Supplier<? extends T> supplier) {
        Objects.requireNonNull(supplier, "supplier is null");
        return Stream.ofAll(Iterator.continually(supplier));
    }

    /**
     * Generates a (theoretically) infinitely long Stream using a function to calculate the next value
     * based on the previous.
     *
     * @param seed The first value in the Stream
     * @param f    A function to calculate the next value based on the previous
     * @param <T>  value type
     * @return A new Stream
     */
    static <T extends @Nullable Object> Stream<T> iterate(T seed, Function<? super T, ? extends T> f) {
        Objects.requireNonNull(f, "f is null");
        return Stream.ofAll(Iterator.iterate(seed, f));
    }

    /**
     * Generates a (theoretically) infinitely long Stream using a repeatedly invoked supplier
     * that provides a {@code Some} for each next value and a {@code None} for the end.
     * The {@code Supplier} will be invoked only that many times until it returns {@code None},
     * and repeated iteration over the stream will produce the same values in the same order,
     * without any further invocations to the {@code Supplier}.
     *
     * @param supplier A Supplier of iterator values
     * @param <T> value type
     * @return A new Stream
     */
    static <T extends @Nullable Object> Stream<T> iterate(Supplier<? extends Option<? extends T>> supplier) {
        Objects.requireNonNull(supplier, "supplier is null");
        return Stream.ofAll(Iterator.iterate(supplier));
    }

    /**
     * Constructs a Stream of a head element and a tail supplier.
     *
     * @param head         The head element of the Stream
     * @param tailSupplier A supplier of the tail values. To end the stream, return {@link Stream#empty}.
     * @param <T>          value type
     * @return A new Stream
     */
    @SuppressWarnings("unchecked")
    static <T extends @Nullable Object> Stream<T> cons(T head, Supplier<? extends Stream<? extends T>> tailSupplier) {
        Objects.requireNonNull(head, "Stream: element is null");
        Objects.requireNonNull(tailSupplier, "tailSupplier is null");
        return new Cons.ConsImpl<>(head, (Supplier<Stream<T>>) tailSupplier);
    }

    /**
     * Returns the single instance of Empty. Convenience method for {@code Empty.instance()}.
     * <p>
     * Note: this method intentionally returns type {@code Stream} and not {@code Empty}. This comes in handy when folding.
     * If you explicitly need type {@code Empty} use {@linkplain Empty#instance()}.
     *
     * @param <T> Component type of Empty, determined by type inference in the particular context.
     * @return The empty list.
     */
    static <T extends @Nullable Object> Stream<T> empty() {
        return Empty.instance();
    }

    /**
     * Narrows a widened {@code Stream<? extends T>} to {@code Stream<T>}
     * by performing a type-safe cast. This is eligible because immutable/read-only
     * collections are covariant.
     *
     * @param stream A {@code Stream}.
     * @param <T>    Component type of the {@code Stream}.
     * @return the given {@code stream} instance as narrowed type {@code Stream<T>}.
     */
    @SuppressWarnings("unchecked")
    static <T extends @Nullable Object> Stream<T> narrow(Stream<? extends T> stream) {
        return (Stream<T>) stream;
    }

    /**
     * Returns a singleton {@code Stream}, i.e. a {@code Stream} of one element.
     *
     * @param element An element.
     * @param <T>     The component type
     * @return A new Stream instance containing the given element
     */
    static <T extends @Nullable Object> Stream<T> of(T element) {
        return cons(element, Empty::instance);
    }

    /**
     * Creates a Stream of the given elements.
     *
     * <pre>{@code  Stream.of(1, 2, 3, 4)
     * = Empty.instance().prepend(4).prepend(3).prepend(2).prepend(1)
     * = Stream.cons(1, () -> Stream.cons(2, () -> Stream.cons(3, () -> Stream.cons(4, Stream::empty))))}</pre>
     *
     * @param <T>      Component type of the Stream.
     * @param elements Zero or more elements.
     * @return A list containing the given elements in the same order.
     */
    @SafeVarargs
    static <T extends @Nullable Object> Stream<T> of(T ... elements) {
        Objects.requireNonNull(elements, "elements is null");
        for (T element : elements) {
            Objects.requireNonNull(element, "Stream.of: element is null");
        }
        return Stream.ofAll(new Iterator<T>() {
            int i = 0;

            @Override
            public boolean hasNext() {
                return i < elements.length;
            }

            @Override
            public T next() {
                return elements[i++];
            }
        });
    }

    /**
     * Returns a Stream containing {@code n} values of a given Function {@code f}
     * over a range of integer values from 0 to {@code n - 1}.
     *
     * @param <T> Component type of the Stream
     * @param n   The number of elements in the Stream
     * @param f   The Function computing element values
     * @return A Stream consisting of elements {@code f(0),f(1), ..., f(n - 1)}
     * @throws NullPointerException if {@code f} is null
     */
    static <T extends @Nullable Object> Stream<T> tabulate(int n, Function<? super Integer, ? extends T> f) {
        Objects.requireNonNull(f, "f is null");
        return Stream.ofAll(com.guizmaii.zazr.collection.internal.Collections.tabulate(n, f));
    }

    /**
     * Returns a Stream containing {@code n} values supplied by a given Supplier {@code s}.
     *
     * @param <T> Component type of the Stream
     * @param n   The number of elements in the Stream
     * @param s   The Supplier computing element values
     * @return A Stream of size {@code n}, where each element contains the result supplied by {@code s}.
     * @throws NullPointerException if {@code s} is null
     */
    static <T extends @Nullable Object> Stream<T> fill(int n, Supplier<? extends T> s) {
        Objects.requireNonNull(s, "s is null");
        return Stream.ofAll(com.guizmaii.zazr.collection.internal.Collections.fill(n, s));
    }

    /**
     * Returns a Stream containing {@code n} times the given {@code element}
     *
     * @param <T>     Component type of the Stream
     * @param n       The number of elements in the Stream
     * @param element The element
     * @return A Stream of size {@code n}, where each element is the given {@code element}.
     */
    static <T extends @Nullable Object> Stream<T> fill(int n, T element) {
        return Stream.ofAll(com.guizmaii.zazr.collection.internal.Collections.fillObject(n, element));
    }

    /**
     * Creates a Stream of the given elements.
     *
     * @param <T>      Component type of the Stream.
     * @param elements An Iterable of elements.
     * @return A Stream containing the given elements in the same order.
     */
    @SuppressWarnings("unchecked")
    static <T extends @Nullable Object> Stream<T> ofAll(Iterable<? extends T> elements) {
        Objects.requireNonNull(elements, "elements is null");
        if (elements instanceof Stream) {
            return (Stream<T>) elements;
        } else if (elements instanceof ListView
                && ((ListView<T, ?>) elements).getDelegate() instanceof Stream) {
            return (Stream<T>) ((ListView<T, ?>) elements).getDelegate();
        } else {
            return StreamFactory.create(elements.iterator());
        }
    }

    /**
     * Creates a Stream that contains the elements of the given {@link java.util.stream.Stream}.
     *
     * @param javaStream A {@link java.util.stream.Stream}
     * @param <T>        Component type of the Stream.
     * @return A Stream containing the given elements in the same order.
     */
    static <T extends @Nullable Object> Stream<T> ofAll(java.util.stream.Stream<? extends T> javaStream) {
        Objects.requireNonNull(javaStream, "javaStream is null");
        return StreamFactory.create(javaStream.iterator());
    }

    /**
     * Creates a Stream from boolean values.
     *
     * @param elements boolean values
     * @return A new Stream of Boolean values
     * @throws NullPointerException if elements is null
     */
    static Stream<Boolean> ofAll(boolean ... elements) {
        Objects.requireNonNull(elements, "elements is null");
        return Stream.ofAll(Iterator.ofAll(elements));
    }

    /**
     * Creates a Stream from byte values.
     *
     * @param elements byte values
     * @return A new Stream of Byte values
     * @throws NullPointerException if elements is null
     */
    static Stream<Byte> ofAll(byte ... elements) {
        Objects.requireNonNull(elements, "elements is null");
        return Stream.ofAll(Iterator.ofAll(elements));
    }

    /**
     * Creates a Stream from char values.
     *
     * @param elements char values
     * @return A new Stream of Character values
     * @throws NullPointerException if elements is null
     */
    static Stream<Character> ofAll(char ... elements) {
        Objects.requireNonNull(elements, "elements is null");
        return Stream.ofAll(Iterator.ofAll(elements));
    }

    /**
     * Creates a Stream values double values.
     *
     * @param elements double values
     * @return A new Stream of Double values
     * @throws NullPointerException if elements is null
     */
    static Stream<Double> ofAll(double ... elements) {
        Objects.requireNonNull(elements, "elements is null");
        return Stream.ofAll(Iterator.ofAll(elements));
    }

    /**
     * Creates a Stream from float values.
     *
     * @param elements float values
     * @return A new Stream of Float values
     * @throws NullPointerException if elements is null
     */
    static Stream<Float> ofAll(float ... elements) {
        Objects.requireNonNull(elements, "elements is null");
        return Stream.ofAll(Iterator.ofAll(elements));
    }

    /**
     * Creates a Stream from int values.
     *
     * @param elements int values
     * @return A new Stream of Integer values
     * @throws NullPointerException if elements is null
     */
    static Stream<Integer> ofAll(int ... elements) {
        Objects.requireNonNull(elements, "elements is null");
        return Stream.ofAll(Iterator.ofAll(elements));
    }

    /**
     * Creates a Stream from long values.
     *
     * @param elements long values
     * @return A new Stream of Long values
     * @throws NullPointerException if elements is null
     */
    static Stream<Long> ofAll(long ... elements) {
        Objects.requireNonNull(elements, "elements is null");
        return Stream.ofAll(Iterator.ofAll(elements));
    }

    /**
     * Creates a Stream from short values.
     *
     * @param elements short values
     * @return A new Stream of Short values
     * @throws NullPointerException if elements is null
     */
    static Stream<Short> ofAll(short ... elements) {
        Objects.requireNonNull(elements, "elements is null");
        return Stream.ofAll(Iterator.ofAll(elements));
    }

    /**
     * Creates a Stream of char numbers starting from {@code from}, extending to {@code toExclusive - 1}.
     * <p>
     * Examples:
     * <pre>
     * {@code
     * Stream.range('a', 'a')  // = Stream()
     * Stream.range('c', 'a')  // = Stream()
     * Stream.range('a', 'd')  // = Stream('a', 'b', 'c')
     * }
     * </pre>
     *
     * @param from        the first char
     * @param toExclusive the last char + 1
     * @return a range of char values as specified or the empty Stream if {@code from >= toExclusive}
     */
    static Stream<Character> range(char from, char toExclusive) {
        return Stream.ofAll(Iterator.range(from, toExclusive));
    }

    /**
     * Creates a Stream of char numbers starting from {@code from}, extending to {@code toExclusive - 1},
     * with {@code step}.
     * <p>
     * Examples:
     * <pre>
     * {@code
     * Stream.rangeBy('a', 'c', 1)  // = Stream('a', 'b')
     * Stream.rangeBy('a', 'd', 2)  // = Stream('a', 'c')
     * Stream.rangeBy('d', 'a', -2) // = Stream('d', 'b')
     * Stream.rangeBy('d', 'a', 2)  // = Stream()
     * }
     * </pre>
     *
     * @param from        the first char
     * @param toExclusive the last char + 1
     * @param step        the step
     * @return a range of char values as specified or the empty Stream if<br>
     * {@code from >= toExclusive} and {@code step > 0} or<br>
     * {@code from <= toExclusive} and {@code step < 0}
     * @throws IllegalArgumentException if {@code step} is zero
     */
    static Stream<Character> rangeBy(char from, char toExclusive, int step) {
        return Stream.ofAll(Iterator.rangeBy(from, toExclusive, step));
    }

    /**
     * Creates a Stream of double numbers starting from {@code from}, extending up to but not including {@code toExclusive},
     * with {@code step}.
     * <p>
     * Examples:
     * <pre>
     * {@code
     * Stream.rangeBy(1.0, 3.0, 1.0)  // = Stream(1.0, 2.0)
     * Stream.rangeBy(1.0, 4.0, 2.0)  // = Stream(1.0, 3.0)
     * Stream.rangeBy(4.0, 1.0, -2.0) // = Stream(4.0, 2.0)
     * Stream.rangeBy(4.0, 1.0, 2.0)  // = Stream()
     * }
     * </pre>
     *
     * @param from        the first double
     * @param toExclusive the upper bound (exclusive)
     * @param step        the step
     * @return a range of double values as specified or the empty Stream if<br>
     * {@code from >= toExclusive} and {@code step > 0} or<br>
     * {@code from <= toExclusive} and {@code step < 0}
     * @throws IllegalArgumentException if {@code step} is zero
     */
    static Stream<Double> rangeBy(double from, double toExclusive, double step) {
        return Stream.ofAll(Iterator.rangeBy(from, toExclusive, step));
    }

    /**
     * Creates a Stream of int numbers starting from {@code from}, extending to {@code toExclusive - 1}.
     * <p>
     * Examples:
     * <pre>
     * {@code
     * Stream.range(0, 0)  // = Stream()
     * Stream.range(2, 0)  // = Stream()
     * Stream.range(-2, 2) // = Stream(-2, -1, 0, 1)
     * }
     * </pre>
     *
     * @param from        the first number
     * @param toExclusive the last number + 1
     * @return a range of int values as specified or the empty Stream if {@code from >= toExclusive}
     */
    static Stream<Integer> range(int from, int toExclusive) {
        return Stream.ofAll(Iterator.range(from, toExclusive));
    }

    /**
     * Creates a Stream of int numbers starting from {@code from}, extending to {@code toExclusive - 1},
     * with {@code step}.
     * <p>
     * Examples:
     * <pre>
     * {@code
     * Stream.rangeBy(1, 3, 1)  // = Stream(1, 2)
     * Stream.rangeBy(1, 4, 2)  // = Stream(1, 3)
     * Stream.rangeBy(4, 1, -2) // = Stream(4, 2)
     * Stream.rangeBy(4, 1, 2)  // = Stream()
     * }
     * </pre>
     *
     * @param from        the first number
     * @param toExclusive the last number + 1
     * @param step        the step
     * @return a range of int values as specified or the empty Stream if<br>
     * {@code from >= toExclusive} and {@code step > 0} or<br>
     * {@code from <= toExclusive} and {@code step < 0}
     * @throws IllegalArgumentException if {@code step} is zero
     */
    static Stream<Integer> rangeBy(int from, int toExclusive, int step) {
        return Stream.ofAll(Iterator.rangeBy(from, toExclusive, step));
    }

    /**
     * Creates a Stream of long numbers starting from {@code from}, extending to {@code toExclusive - 1}.
     * <p>
     * Examples:
     * <pre>
     * {@code
     * Stream.range(0L, 0L)  // = Stream()
     * Stream.range(2L, 0L)  // = Stream()
     * Stream.range(-2L, 2L) // = Stream(-2L, -1L, 0L, 1L)
     * }
     * </pre>
     *
     * @param from        the first number
     * @param toExclusive the last number + 1
     * @return a range of long values as specified or the empty Stream if {@code from >= toExclusive}
     */
    static Stream<Long> range(long from, long toExclusive) {
        return Stream.ofAll(Iterator.range(from, toExclusive));
    }

    /**
     * Creates a Stream of long numbers starting from {@code from}, extending to {@code toExclusive - 1},
     * with {@code step}.
     * <p>
     * Examples:
     * <pre>
     * {@code
     * Stream.rangeBy(1L, 3L, 1L)  // = Stream(1L, 2L)
     * Stream.rangeBy(1L, 4L, 2L)  // = Stream(1L, 3L)
     * Stream.rangeBy(4L, 1L, -2L) // = Stream(4L, 2L)
     * Stream.rangeBy(4L, 1L, 2L)  // = Stream()
     * }
     * </pre>
     *
     * @param from        the first number
     * @param toExclusive the last number + 1
     * @param step        the step
     * @return a range of long values as specified or the empty Stream if<br>
     * {@code from >= toExclusive} and {@code step > 0} or<br>
     * {@code from <= toExclusive} and {@code step < 0}
     * @throws IllegalArgumentException if {@code step} is zero
     */
    static Stream<Long> rangeBy(long from, long toExclusive, long step) {
        return Stream.ofAll(Iterator.rangeBy(from, toExclusive, step));
    }

    /**
     * Creates a Stream of char numbers starting from {@code from}, extending to {@code toInclusive}.
     * <p>
     * Examples:
     * <pre>
     * {@code
     * Stream.rangeClosed('a', 'a')  // = Stream('a')
     * Stream.rangeClosed('c', 'a')  // = Stream()
     * Stream.rangeClosed('a', 'd')  // = Stream('a', 'b', 'c', 'd')
     * }
     * </pre>
     *
     * @param from        the first char
     * @param toInclusive the last char
     * @return a range of char values as specified or the empty Stream if {@code from > toInclusive}
     */
    static Stream<Character> rangeClosed(char from, char toInclusive) {
        return Stream.ofAll(Iterator.rangeClosed(from, toInclusive));
    }

    static Stream<Character> rangeClosedBy(char from, char toInclusive, int step) {
        return Stream.ofAll(Iterator.rangeClosedBy(from, toInclusive, step));
    }

    static Stream<Double> rangeClosedBy(double from, double toInclusive, double step) {
        return Stream.ofAll(Iterator.rangeClosedBy(from, toInclusive, step));
    }

    /**
     * Creates a Stream of int numbers starting from {@code from}, extending to {@code toInclusive}.
     * <p>
     * Examples:
     * <pre>
     * {@code
     * Stream.rangeClosed(0, 0)  // = Stream(0)
     * Stream.rangeClosed(2, 0)  // = Stream()
     * Stream.rangeClosed(-2, 2) // = Stream(-2, -1, 0, 1, 2)
     * }
     * </pre>
     *
     * @param from        the first number
     * @param toInclusive the last number
     * @return a range of int values as specified or the empty Stream if {@code from > toInclusive}
     */
    static Stream<Integer> rangeClosed(int from, int toInclusive) {
        return Stream.ofAll(Iterator.rangeClosed(from, toInclusive));
    }

    /**
     * Creates a Stream of int numbers starting from {@code from}, extending to {@code toInclusive},
     * with {@code step}.
     * <p>
     * Examples:
     * <pre>
     * {@code
     * Stream.rangeClosedBy(1, 3, 1)  // = Stream(1, 2, 3)
     * Stream.rangeClosedBy(1, 4, 2)  // = Stream(1, 3)
     * Stream.rangeClosedBy(4, 1, -2) // = Stream(4, 2)
     * Stream.rangeClosedBy(4, 1, 2)  // = Stream()
     * }
     * </pre>
     *
     * @param from        the first number
     * @param toInclusive the last number
     * @param step        the step
     * @return a range of int values as specified or the empty Stream if<br>
     * {@code from > toInclusive} and {@code step > 0} or<br>
     * {@code from < toInclusive} and {@code step < 0}
     * @throws IllegalArgumentException if {@code step} is zero
     */
    static Stream<Integer> rangeClosedBy(int from, int toInclusive, int step) {
        return Stream.ofAll(Iterator.rangeClosedBy(from, toInclusive, step));
    }

    /**
     * Creates a Stream of long numbers starting from {@code from}, extending to {@code toInclusive}.
     * <p>
     * Examples:
     * <pre>
     * {@code
     * Stream.rangeClosed(0L, 0L)  // = Stream(0L)
     * Stream.rangeClosed(2L, 0L)  // = Stream()
     * Stream.rangeClosed(-2L, 2L) // = Stream(-2L, -1L, 0L, 1L, 2L)
     * }
     * </pre>
     *
     * @param from        the first number
     * @param toInclusive the last number
     * @return a range of long values as specified or the empty Stream if {@code from > toInclusive}
     */
    static Stream<Long> rangeClosed(long from, long toInclusive) {
        return Stream.ofAll(Iterator.rangeClosed(from, toInclusive));
    }

    /**
     * Creates a Stream of long numbers starting from {@code from}, extending to {@code toInclusive},
     * with {@code step}.
     * <p>
     * Examples:
     * <pre>
     * {@code
     * Stream.rangeClosedBy(1L, 3L, 1L)  // = Stream(1L, 2L, 3L)
     * Stream.rangeClosedBy(1L, 4L, 2L)  // = Stream(1L, 3L)
     * Stream.rangeClosedBy(4L, 1L, -2L) // = Stream(4L, 2L)
     * Stream.rangeClosedBy(4L, 1L, 2L)  // = Stream()
     * }
     * </pre>
     *
     * @param from        the first number
     * @param toInclusive the last number
     * @param step        the step
     * @return a range of long values as specified or the empty Stream if<br>
     * {@code from > toInclusive} and {@code step > 0} or<br>
     * {@code from < toInclusive} and {@code step < 0}
     * @throws IllegalArgumentException if {@code step} is zero
     */
    static Stream<Long> rangeClosedBy(long from, long toInclusive, long step) {
        return Stream.ofAll(Iterator.rangeClosedBy(from, toInclusive, step));
    }

    /**
     * Transposes the rows and columns of a {@link Stream} matrix.
     * <p>
     * Complexity: O(rows * columns); the whole matrix is forced.
     *
     * @param <T> matrix element type
     * @param matrix to be transposed.
     * @return a transposed {@link Stream} matrix.
     * @throws IllegalArgumentException if the row lengths of {@code matrix} differ.
     * <p>
     * ex: {@code
     * Stream.transpose(Stream(Stream(1,2,3), Stream(4,5,6))) → Stream(Stream(1,4), Stream(2,5), Stream(3,6))
     * }
     */
    static <T extends @Nullable Object> Stream<Stream<T>> transpose(Stream<Stream<T>> matrix) {
        return com.guizmaii.zazr.collection.internal.Collections.transpose(matrix, Stream::ofAll, Stream::of);
    }

    /**
     * Creates a Stream from a seed value and a function.
     * The function takes the seed at first.
     * The function should return {@code None} when it's
     * done generating the Stream, otherwise {@code Some} {@code Tuple}
     * of the element for the next call and the value to add to the
     * resulting Stream.
     * <p>
     * Example:
     * <pre>
     * {@code
     * Stream.unfoldRight(10, x -> x == 0
     *             ? Option.none()
     *             : Option.some(new Tuple2<>(x, x-1)));
     * // Stream(10, 9, 8, 7, 6, 5, 4, 3, 2, 1))
     * }
     * </pre>
     *
     * @param <T>  type of seeds
     * @param <U>  type of unfolded values
     * @param seed the start value for the iteration
     * @param f    the function to get the next step of the iteration
     * @return a Stream with the values built up by the iteration
     * @throws NullPointerException if {@code f} is null
     */
    static <T extends @Nullable Object, U extends @Nullable Object> Stream<U> unfoldRight(T seed, Function<? super T, Option<Tuple2<? extends U, ? extends T>>> f) {
        return Iterator.unfoldRight(seed, f).toStream();
    }

    /**
     * Creates a Stream from a seed value and a function.
     * The function takes the seed at first.
     * The function should return {@code None} when it's
     * done generating the Stream, otherwise {@code Some} {@code Tuple}
     * of the value to add to the resulting Stream and
     * the element for the next call.
     * <p>
     * Example:
     * <pre>
     * {@code
     * Stream.unfoldLeft(10, x -> x == 0
     *             ? Option.none()
     *             : Option.some(new Tuple2<>(x-1, x)));
     * // Stream(1, 2, 3, 4, 5, 6, 7, 8, 9, 10))
     * }
     * </pre>
     *
     * @param <T>  type of seeds
     * @param <U>  type of unfolded values
     * @param seed the start value for the iteration
     * @param f    the function to get the next step of the iteration
     * @return a Stream with the values built up by the iteration
     * @throws NullPointerException if {@code f} is null
     */
    static <T extends @Nullable Object, U extends @Nullable Object> Stream<U> unfoldLeft(T seed, Function<? super T, Option<Tuple2<? extends T, ? extends U>>> f) {
        return Iterator.unfoldLeft(seed, f).toStream();
    }

    /**
     * Creates a Stream from a seed value and a function.
     * The function takes the seed at first.
     * The function should return {@code None} when it's
     * done generating the Stream, otherwise {@code Some} {@code Tuple}
     * of the value to add to the resulting Stream and
     * the element for the next call.
     * <p>
     * Example:
     * <pre>
     * {@code
     * Stream.unfold(10, x -> x == 0
     *             ? Option.none()
     *             : Option.some(new Tuple2<>(x-1, x)));
     * // Stream(1, 2, 3, 4, 5, 6, 7, 8, 9, 10))
     * }
     * </pre>
     *
     * @param <T>  type of seeds and unfolded values
     * @param seed the start value for the iteration
     * @param f    the function to get the next step of the iteration
     * @return a Stream with the values built up by the iteration
     * @throws NullPointerException if {@code f} is null
     */
    static <T extends @Nullable Object> Stream<T> unfold(T seed, Function<? super T, Option<Tuple2<? extends T, ? extends T>>> f) {
        return Iterator.unfold(seed, f).toStream();
    }

    /**
     * Repeats an element infinitely often.
     *
     * @param t   An element
     * @param <T> Element type
     * @return A new Stream containing infinite {@code t}'s.
     */
    static <T extends @Nullable Object> Stream<T> continually(T t) {
        return Stream.ofAll(Iterator.continually(t));
    }

    /**
     * Whether {@code that} occurs in this Stream as a contiguous slice.
     * <p>
     * Complexity: O(n * m) for a slice of m elements; the elements are forced until the slice is found.
     *
     * @param that the slice to look for
     * @return true if {@code that} occurs contiguously in this Stream (an empty slice always does)
     * @throws NullPointerException if {@code that} is null
     */
    default boolean containsSlice(Iterable<? extends T> that) {
        Objects.requireNonNull(that, "that is null");
        return indexOfSlice(that) >= 0;
    }

    /**
     * Whether this Stream ends with {@code that}.
     * <p>
     * Complexity: O(n + m) for m elements of {@code that}; the whole Stream is forced.
     *
     * @param that the suffix to test
     * @return true if the last {@code m} elements equal {@code that} (an empty {@code that} is always a suffix)
     * @throws NullPointerException if {@code that} is null
     */
    default boolean endsWith(Iterable<? extends T> that) {
        Objects.requireNonNull(that, "that is null");
        final Stream<? extends T> suffix = Stream.ofAll(that);
        final int skipped = length() - suffix.length();
        if (skipped < 0) {
            return false;
        }
        final Iterator<T> i = Iterator.ofAll(this).drop(skipped);
        final java.util.Iterator<? extends T> j = suffix.iterator();
        while (i.hasNext() && j.hasNext()) {
            if (!Objects.equals(i.next(), j.next())) {
                return false;
            }
        }
        return !j.hasNext();
    }

    /**
     * The index of the first occurrence of {@code element}, or -1.
     * <p>
     * Complexity: O(n); the elements are forced until the element is found.
     *
     * @param element the element to find
     * @return the index of its first occurrence, or -1 if absent
     */
    default int indexOf(T element) {
        return indexOf(element, 0);
    }

    /**
     * The first index at which {@code that} occurs as a contiguous slice, or -1.
     * <p>
     * Complexity: O(n * m) for a slice of m elements; the elements are forced until the slice is found.
     *
     * @param that the slice to find
     * @return the index of its first occurrence, or -1 (an empty slice occurs at 0)
     * @throws NullPointerException if {@code that} is null
     */
    default int indexOfSlice(Iterable<? extends T> that) {
        return indexOfSlice(that, 0);
    }

    /**
     * The first index at or after {@code from} at which {@code that} occurs as a contiguous slice, or -1.
     * <p>
     * Complexity: O(n * m) for a slice of m elements; the elements are forced until the slice is found.
     *
     * @param that the slice to find
     * @param from the first position to look at
     * @return the index of its first occurrence at or after {@code from}, or -1
     * @throws NullPointerException if {@code that} is null
     */
    default int indexOfSlice(Iterable<? extends T> that, int from) {
        Objects.requireNonNull(that, "that is null");
        return StreamModule.Slice.indexOfSlice(this, that, from);
    }

    /**
     * The index of the first element satisfying {@code predicate}, or -1.
     * <p>
     * Complexity: O(n); the elements are forced until one satisfies the predicate.
     *
     * @param predicate the condition
     * @return the first index of a satisfying element, or -1
     * @throws NullPointerException if {@code predicate} is null
     */
    default int indexWhere(Predicate<? super T> predicate) {
        return indexWhere(predicate, 0);
    }

    /**
     * The index of the first element at or after {@code from} satisfying {@code predicate}, or -1. A negative
     * {@code from} counts as 0.
     * <p>
     * Complexity: O(n); the elements are forced until one satisfies the predicate.
     *
     * @param predicate the condition
     * @param from      the first position to look at
     * @return the first index {@code >= from} of a satisfying element, or -1
     * @throws NullPointerException if {@code predicate} is null
     */
    default int indexWhere(Predicate<? super T> predicate, int from) {
        Objects.requireNonNull(predicate, "predicate is null");
        int i = Math.max(from, 0);
        Stream<T> these = drop(i);
        while (!these.isEmpty()) {
            if (predicate.test(these.head())) {
                return i;
            }
            i++;
            these = these.tail();
        }
        return -1;
    }

    /**
     * The index of the last occurrence of {@code element}, or -1.
     * <p>
     * Complexity: O(n); the whole Stream is forced.
     *
     * @param element the element to find
     * @return the index of its last occurrence, or -1 if absent
     */
    default int lastIndexOf(T element) {
        return lastIndexOf(element, Integer.MAX_VALUE);
    }

    /**
     * The last index at which {@code that} occurs as a contiguous slice, or -1.
     * <p>
     * Complexity: O(n * m) for a slice of m elements; the whole Stream is forced.
     *
     * @param that the slice to find
     * @return the index of its last occurrence, or -1
     * @throws NullPointerException if {@code that} is null
     */
    default int lastIndexOfSlice(Iterable<? extends T> that) {
        return lastIndexOfSlice(that, Integer.MAX_VALUE);
    }

    /**
     * The last index at or before {@code end} at which {@code that} occurs as a contiguous slice, or -1.
     * <p>
     * Complexity: O(n * m) for a slice of m elements; the elements up to {@code end} are forced.
     *
     * @param that the slice to find
     * @param end  the last position to look at
     * @return the index of its last occurrence at or before {@code end}, or -1
     * @throws NullPointerException if {@code that} is null
     */
    default int lastIndexOfSlice(Iterable<? extends T> that, int end) {
        Objects.requireNonNull(that, "that is null");
        return StreamModule.Slice.lastIndexOfSlice(this, that, end);
    }

    /**
     * The index of the last element satisfying {@code predicate}, or -1.
     * <p>
     * Complexity: O(n); the whole Stream is forced.
     *
     * @param predicate the condition
     * @return the last index of a satisfying element, or -1
     * @throws NullPointerException if {@code predicate} is null
     */
    default int lastIndexWhere(Predicate<? super T> predicate) {
        return lastIndexWhere(predicate, length() - 1);
    }

    /**
     * The index of the last element at or before {@code end} satisfying {@code predicate}, or -1.
     * <p>
     * Complexity: O(n); the elements up to {@code end} are forced.
     *
     * @param predicate the condition
     * @param end       the last position to look at
     * @return the last index {@code <= end} of a satisfying element, or -1
     * @throws NullPointerException if {@code predicate} is null
     */
    default int lastIndexWhere(Predicate<? super T> predicate, int end) {
        Objects.requireNonNull(predicate, "predicate is null");
        int i = 0;
        Stream<T> these = this;
        int last = -1;
        while (!these.isEmpty() && i <= end) {
            if (predicate.test(these.head())) {
                last = i;
            }
            these = these.tail();
            i++;
        }
        return last;
    }

    /**
     * The length of the longest prefix whose elements all satisfy {@code predicate}.
     * <p>
     * Complexity: O(k); the k elements of that prefix are forced, plus the first one that is not.
     *
     * @param predicate the condition
     * @return the length of the prefix
     * @throws NullPointerException if {@code predicate} is null
     */
    default int prefixLength(Predicate<? super T> predicate) {
        return segmentLength(predicate, 0);
    }

    /**
     * The position of {@code element} in this Stream, which must already be sorted in ascending natural order; the
     * result is undefined otherwise. The search is linear, as a Stream has no indexed access.
     * <p>
     * Complexity: O(n); the elements are forced until one is not smaller than {@code element}.
     *
     * @param element the element to find
     * @return the index of the element if it is present; otherwise {@code (-(insertion point) - 1)}, the insertion
     *         point being the index at which the element would be inserted
     * @throws ClassCastException if {@code T} is not {@code Comparable}
     */
    @SuppressWarnings("unchecked")
    default int search(T element) {
        final ToIntFunction<T> comparison = ((Comparable<T>) element)::compareTo;
        return StreamModule.Search.linearSearch(this, comparison);
    }

    /**
     * The position of {@code element} in this Stream, which must already be sorted in ascending order according to
     * {@code comparator}; the result is undefined otherwise. The search is linear, as a Stream has no indexed access.
     * <p>
     * Complexity: O(n); the elements are forced until one is not smaller than {@code element}.
     *
     * @param element    the element to find
     * @param comparator the order this Stream is sorted by
     * @return the index of the element if it is present; otherwise {@code (-(insertion point) - 1)}, the insertion
     *         point being the index at which the element would be inserted
     * @throws NullPointerException if {@code comparator} is null
     */
    default int search(T element, Comparator<? super T> comparator) {
        Objects.requireNonNull(comparator, "comparator is null");
        final ToIntFunction<T> comparison = current -> comparator.compare(element, current);
        return StreamModule.Search.linearSearch(this, comparison);
    }

    /**
     * The length of the longest run of elements satisfying {@code predicate} starting at {@code from}.
     * <p>
     * Complexity: O(from + k); the elements of that run are forced, plus the first one that is not.
     *
     * @param predicate the condition
     * @param from      the first position to look at
     * @return the length of the run
     * @throws NullPointerException if {@code predicate} is null
     */
    default int segmentLength(Predicate<? super T> predicate, int from) {
        Objects.requireNonNull(predicate, "predicate is null");
        int i = 0;
        Stream<T> these = this.drop(from);
        while (!these.isEmpty() && predicate.test(these.head())) {
            i++;
            these = these.tail();
        }
        return i;
    }

    /**
     * Whether this Stream starts with {@code that}: {@code startsWith(that, 0)}.
     * <p>
     * Complexity: O(m) for m elements of {@code that}; only those elements are forced.
     *
     * @param that the prefix to test
     * @return true if the first {@code m} elements equal {@code that} (an empty {@code that} is always a prefix)
     * @throws NullPointerException if {@code that} is null
     */
    default boolean startsWith(Iterable<? extends T> that) {
        return startsWith(that, 0);
    }

    /**
     * Whether the elements from {@code offset} on start with {@code that}. {@code that} is walked once, so a
     * one-shot iterator is accepted.
     * <p>
     * Complexity: O(offset + m) for m elements of {@code that}; only those elements are forced.
     *
     * @param that   the prefix to test
     * @param offset the position in this Stream at which the prefix should start
     * @return false if {@code offset} is negative; otherwise true if {@code that} equals the {@code m} elements from
     *         {@code offset} on (an empty {@code that} is always a prefix, even beyond the end)
     * @throws NullPointerException if {@code that} is null
     */
    default boolean startsWith(Iterable<? extends T> that, int offset) {
        Objects.requireNonNull(that, "that is null");
        if (offset < 0) {
            return false;
        }
        final Iterator<T> i = Iterator.ofAll(this).drop(offset);
        final java.util.Iterator<? extends T> j = that.iterator();
        while (i.hasNext() && j.hasNext()) {
            if (!Objects.equals(i.next(), j.next())) {
                return false;
            }
        }
        return !j.hasNext();
    }

    /**
     * {@link #indexOf(Object)} as an {@link Option}: {@code None} for -1.
     *
     * @param element the element to find
     * @return {@code Some(index)} of its first occurrence, or {@code None}
     */
    default Option<Integer> indexOfOption(T element) {
        return Collections.indexOption(indexOf(element));
    }

    /**
     * {@link #indexOf(Object, int)} as an {@link Option}: {@code None} for -1.
     *
     * @param element the element to find
     * @param from    the first position to look at
     * @return {@code Some(index)} of its first occurrence at or after {@code from}, or {@code None}
     */
    default Option<Integer> indexOfOption(T element, int from) {
        return Collections.indexOption(indexOf(element, from));
    }

    /**
     * {@link #indexOfSlice(Iterable)} as an {@link Option}: {@code None} for -1.
     *
     * @param that the slice to find
     * @return {@code Some(index)} of its first occurrence, or {@code None}
     * @throws NullPointerException if {@code that} is null
     */
    default Option<Integer> indexOfSliceOption(Iterable<? extends T> that) {
        return Collections.indexOption(indexOfSlice(that));
    }

    /**
     * {@link #indexOfSlice(Iterable, int)} as an {@link Option}: {@code None} for -1.
     *
     * @param that the slice to find
     * @param from the first position to look at
     * @return {@code Some(index)} of its first occurrence at or after {@code from}, or {@code None}
     * @throws NullPointerException if {@code that} is null
     */
    default Option<Integer> indexOfSliceOption(Iterable<? extends T> that, int from) {
        return Collections.indexOption(indexOfSlice(that, from));
    }

    /**
     * {@link #indexWhere(Predicate)} as an {@link Option}: {@code None} for -1.
     *
     * @param predicate the condition
     * @return {@code Some(index)} of the first satisfying element, or {@code None}
     * @throws NullPointerException if {@code predicate} is null
     */
    default Option<Integer> indexWhereOption(Predicate<? super T> predicate) {
        return Collections.indexOption(indexWhere(predicate));
    }

    /**
     * {@link #indexWhere(Predicate, int)} as an {@link Option}: {@code None} for -1.
     *
     * @param predicate the condition
     * @param from      the first position to look at
     * @return {@code Some(index)} of the first satisfying element at or after {@code from}, or {@code None}
     * @throws NullPointerException if {@code predicate} is null
     */
    default Option<Integer> indexWhereOption(Predicate<? super T> predicate, int from) {
        return Collections.indexOption(indexWhere(predicate, from));
    }

    /**
     * {@link #lastIndexOf(Object)} as an {@link Option}: {@code None} for -1.
     *
     * @param element the element to find
     * @return {@code Some(index)} of its last occurrence, or {@code None}
     */
    default Option<Integer> lastIndexOfOption(T element) {
        return Collections.indexOption(lastIndexOf(element));
    }

    /**
     * {@link #lastIndexOf(Object, int)} as an {@link Option}: {@code None} for -1.
     *
     * @param element the element to find
     * @param end     the last position to look at
     * @return {@code Some(index)} of its last occurrence at or before {@code end}, or {@code None}
     */
    default Option<Integer> lastIndexOfOption(T element, int end) {
        return Collections.indexOption(lastIndexOf(element, end));
    }

    /**
     * {@link #lastIndexOfSlice(Iterable)} as an {@link Option}: {@code None} for -1.
     *
     * @param that the slice to find
     * @return {@code Some(index)} of its last occurrence, or {@code None}
     * @throws NullPointerException if {@code that} is null
     */
    default Option<Integer> lastIndexOfSliceOption(Iterable<? extends T> that) {
        return Collections.indexOption(lastIndexOfSlice(that));
    }

    /**
     * {@link #lastIndexOfSlice(Iterable, int)} as an {@link Option}: {@code None} for -1.
     *
     * @param that the slice to find
     * @param end  the last position to look at
     * @return {@code Some(index)} of its last occurrence at or before {@code end}, or {@code None}
     * @throws NullPointerException if {@code that} is null
     */
    default Option<Integer> lastIndexOfSliceOption(Iterable<? extends T> that, int end) {
        return Collections.indexOption(lastIndexOfSlice(that, end));
    }

    /**
     * {@link #lastIndexWhere(Predicate)} as an {@link Option}: {@code None} for -1.
     *
     * @param predicate the condition
     * @return {@code Some(index)} of the last satisfying element, or {@code None}
     * @throws NullPointerException if {@code predicate} is null
     */
    default Option<Integer> lastIndexWhereOption(Predicate<? super T> predicate) {
        return Collections.indexOption(lastIndexWhere(predicate));
    }

    /**
     * {@link #lastIndexWhere(Predicate, int)} as an {@link Option}: {@code None} for -1.
     *
     * @param predicate the condition
     * @param end       the last position to look at
     * @return {@code Some(index)} of the last satisfying element at or before {@code end}, or {@code None}
     * @throws NullPointerException if {@code predicate} is null
     */
    default Option<Integer> lastIndexWhereOption(Predicate<? super T> predicate, int end) {
        return Collections.indexOption(lastIndexWhere(predicate, end));
    }

    /**
     * Folds the elements from the right: starts with {@code zero} and combines each element, from the last to the
     * first, with the accumulator.
     * <pre>{@code
     * // = 24
     * List.of('4', '2').foldRight(0, (x, acc) -> acc * 10 + x - '0');
     * }</pre>
     * <p>
     * The elements are folded from the end: this Stream is reversed first, which forces it, then folded from the
     * left, so the recursion depth does not grow with the length.
     *
     * @param <U>  the type of the accumulator
     * @param zero the initial accumulator
     * @param f    combines the next element (from the right) and the accumulator so far
     * @return the final accumulator, {@code zero} on an empty sequence
     * @throws NullPointerException if {@code f} is null
     */
    default <U extends @Nullable Object> U foldRight(U zero, BiFunction<? super T, ? super U, ? extends U> f) {
        Objects.requireNonNull(f, "f is null");
        return reverse().foldLeft(zero, (xs, x) -> f.apply(x, xs));
    }

    /**
     * Returns a new Stream with the given element appended at the end.
     * <p>
     * Complexity: O(1); the head is forced, the rest of this Stream stays deferred and the element is reached last.
     *
     * @param element the element to append
     * @return a new Stream ending with the given element
     */
    default Stream<T> append(T element) {
        return isEmpty() ? Stream.of(element) : new Cons.AppendElements<>(head(), com.guizmaii.zazr.collection.Queue.of(element), this::tail);
    }

    /**
     * Returns a new Stream with the given elements appended at the end, in iteration order.
     * <p>
     * Complexity: O(1); the head of this Stream and of {@code elements} is forced, the rest stays deferred.
     *
     * @param elements the elements to append
     * @return a new Stream ending with the given elements, or this Stream if there are none
     * @throws NullPointerException if {@code elements} is null
     */
    default Stream<T> appendAll(Iterable<? extends T> elements) {
        Objects.requireNonNull(elements, "elements is null");
        if (!Collections.isTraversableAgain(elements)) {
            // a one-shot source is read exactly once, into a memoising Stream that also answers whether it is empty
            final Stream<T> that = Stream.ofAll(elements);
            return that.isEmpty() ? this : appendAll(that);
        } else if (Collections.isEmpty(elements)) {
            return this;
        } else if (isEmpty()) {
            return Stream.ofAll(elements);
        } else {
            return Stream.ofAll(Iterator.concat(this, elements));
        }
    }

    /**
     * Appends itself to the end of stream with {@code mapper} function.
     * <p>
     * <strong>Example:</strong>
     * <p>
     * Well known Scala code for Fibonacci infinite sequence
     * <pre>
     * {@code
     * val fibs:Stream[Int] = 0 #:: 1 #:: (fibs zip fibs.tail).map{ t => t._1() + t._2() }
     * }
     * </pre>
     * can be transformed to
     * <pre>
     * {@code
     * Stream.of(0, 1).appendSelf(self -> self.zip(self.tail()).map(t -> t._1() + t._2()));
     * }
     * </pre>
     * <p>
     * Complexity: O(1); the result is built lazily and each element is forced when it is reached.
     *
     * @param mapper an mapper
     * @return this Stream if it is empty, otherwise a new Stream obtained by appending this Stream, mapped by {@code mapper}, to itself
     */
    default Stream<T> appendSelf(Function<? super Stream<T>, ? extends Stream<T>> mapper) {
        Objects.requireNonNull(mapper, "mapper is null");
        return isEmpty() ? this : new AppendSelf<>((Cons<T>) this, mapper).stream();
    }

    /**
     * Returns an immutable {@link java.util.List} view of this Stream: reads go through to this Stream, mutators throw
     * {@link UnsupportedOperationException}.
     * <p>
     * Complexity: O(1); {@code get(i)} on the view forces the first {@code i + 1} elements.
     *
     * @return an immutable {@code java.util.List} view
     */
    default java.util.List<T> asJava() {
        return JavaConverters.asJava(this, IMMUTABLE);
    }

    /**
     * Passes an immutable {@link java.util.List} view of this Stream to {@code action} and returns this Stream.
     * <p>
     * Complexity: O(1) to create the view.
     *
     * @param action receives the view
     * @return this Stream
     * @throws NullPointerException if {@code action} is null
     * @see #asJava()
     */
    default Stream<T> asJava(Consumer<? super java.util.List<T>> action) {
        Objects.requireNonNull(action, "action is null");
        action.accept(asJava());
        return this;
    }

    /**
     * Returns a mutable {@link java.util.List} view of this Stream: every mutator replaces the view's underlying Stream
     * by a new one; this Stream is never modified.
     * <p>
     * Complexity: O(1); each mutator costs what the corresponding Stream operation costs.
     *
     * @return a mutable {@code java.util.List} view
     */
    default java.util.List<T> asJavaMutable() {
        return JavaConverters.asJava(this, MUTABLE);
    }

    /**
     * Passes a mutable {@link java.util.List} view of this Stream to {@code action} and returns the Stream the view holds
     * afterwards: this Stream if the action only read, a new one reflecting the writes otherwise.
     * <p>
     * Complexity: O(1) to create the view.
     *
     * @param action receives the view
     * @return this Stream, or a new Stream reflecting the modifications made through the view
     * @throws NullPointerException if {@code action} is null
     * @see #asJavaMutable()
     */
    default Stream<T> asJavaMutable(Consumer<? super java.util.List<T>> action) {
        Objects.requireNonNull(action, "action is null");
        final ListView<T, Stream<T>> view = JavaConverters.asJava(this, MUTABLE);
        action.accept(view);
        return view.getDelegate();
    }

    /**
     * All combinations of the elements, for every size from 0 to {@code length()}, by position.
     * <p>
     * Complexity: O(2^n) combinations; the whole Stream is forced.
     *
     * @return the combinations, shortest first
     */
    default Stream<Stream<T>> combinations() {
        return Stream.rangeClosed(0, length()).map(this::combinations).flatMap(Function.identity());
    }

    /**
     * All combinations of {@code k} elements, by position, in lexicographic position order. A negative {@code k}
     * counts as 0, and a {@code k} greater than {@code length()} gives no combination.
     * <p>
     * Complexity: O(n choose k) combinations; the whole Stream is forced.
     *
     * @param k the size of each combination
     * @return the combinations
     */
    default Stream<Stream<T>> combinations(int k) {
        return Combinations.apply(this, Math.max(k, 0));
    }

    /**
     * Repeat the elements of this Stream infinitely.
     * <p>
     * Example:
     * <pre>
     * {@code
     * // = 1, 2, 3, 1, 2, 3, 1, 2, 3, ...
     * Stream.of(1, 2, 3).cycle();
     * }
     * </pre>
     * <p>
     * Complexity: O(1); the result is infinite and each element is forced when it is reached.
     *
     * @return this Stream if it is empty, otherwise a new Stream containing this elements cycled.
     */
    default Stream<T> cycle() {
        return isEmpty() ? this : appendSelf(Function.identity());
    }

    /**
     * Repeat the elements of this Stream {@code count} times.
     * <p>
     * Example:
     * <pre>
     * {@code
     * // = empty
     * Stream.of(1, 2, 3).cycle(0);
     *
     * // = 1, 2, 3
     * Stream.of(1, 2, 3).cycle(1);
     *
     * // = 1, 2, 3, 1, 2, 3, 1, 2, 3
     * Stream.of(1, 2, 3).cycle(3);
     * }
     * </pre>
     * <p>
     * Complexity: lazy; each element is forced when the result reaches it.
     *
     * @param count the number of cycles to be performed
     * @return A new Stream containing this elements cycled {@code count} times.
     */
    default Stream<T> cycle(int count) {
        if (count <= 0 || isEmpty()) {
            return empty();
        } else {
            final Stream<T> self = this;
            return Stream.ofAll(new Iterator<T>() {
                Stream<T> stream = self;
                int i = count - 1;

                @Override
                public boolean hasNext() {
                    return !stream.isEmpty() || i > 0;
                }

                @Override
                public T next() {
                    if (stream.isEmpty()) {
                        i--;
                        stream = self;
                    }
                    final T result = stream.head();
                    stream = stream.tail();
                    return result;
                }
            });
        }
    }

    /**
     * Returns a new {@code Stream} containing the elements of this instance
     * with all duplicates removed. Element equality is determined using {@code equals}.
     * <p>
     * Complexity: lazy; each element is forced and hashed when the result reaches it.
     *
     * @return a new {@code Stream} without duplicate elements
     */
    default Stream<T> distinct() {
        return distinctBy(Function.identity());
    }

    /**
     * Returns a new {@code Stream} containing the elements of this instance
     * without duplicates, as determined by the given {@code comparator}; the first of two equal elements is kept.
     * <p>
     * Complexity: lazy; O(log n) comparisons per element when the result reaches it.
     *
     * @param comparator a comparator used to determine equality of elements
     * @return a new {@code Stream} with duplicates removed
     * @throws NullPointerException if {@code comparator} is null
     */
    default Stream<T> distinctBy(Comparator<? super T> comparator) {
        Objects.requireNonNull(comparator, "comparator is null");
        final java.util.Set<T> seen = new java.util.TreeSet<>(comparator);
        return filter(seen::add);
    }

    /**
     * Returns a new {@code Stream} containing the elements of this instance
     * without duplicates, based on keys extracted from elements using {@code keyExtractor}.
     * <p>
     * The first occurrence of each key is retained in the resulting sequence.
     * <p>
     * Complexity: lazy; one key per element when the result reaches it.
     *
     * @param keyExtractor a function to extract keys for determining uniqueness
     * @param <U>          the type of key
     * @return a new {@code Stream} with duplicates removed based on keys
     * @throws NullPointerException if {@code keyExtractor} is null
     */
    default <U extends @Nullable Object> Stream<T> distinctBy(Function<? super T, ? extends U> keyExtractor) {
        Objects.requireNonNull(keyExtractor, "keyExtractor is null");
        final java.util.Set<U> seen = new java.util.HashSet<>();
        return filter(t -> seen.add(keyExtractor.apply(t)));
    }

    /**
     * The complement of {@link #distinct()}: the elements occurring more than once, each once, in order of first
     * occurrence. {@code Stream.of(3, 1, 3, 2, 1, 3).duplicates()} is {@code Stream.of(3, 1)}. {@code isEmpty()} on
     * the result is the "all distinct" test.
     * <p>
     * Complexity: O(n), one hash lookup per element; the whole Stream is forced, so it does not terminate on an
     * infinite Stream (whether an element repeats, and so whether it comes before the next one, is known only at the
     * end).
     *
     * @return a new Stream of the repeated elements
     */
    default Stream<T> duplicates() {
        return duplicatesBy(Function.identity());
    }

    /**
     * {@link #duplicates()} under a key: the first element of each key occurring more than once, in order of first
     * occurrence. One pass, the key computed once per element.
     * <p>
     * Complexity: O(n), one key and one hash lookup per element; the whole Stream is forced, so it does not terminate
     * on an infinite Stream.
     *
     * @param keyExtractor computes the key an element is compared by
     * @param <U>          the key type
     * @return a new Stream of the first element of each repeated key
     * @throws NullPointerException if {@code keyExtractor} is null
     */
    default <U extends @Nullable Object> Stream<T> duplicatesBy(Function<? super T, ? extends U> keyExtractor) {
        Objects.requireNonNull(keyExtractor, "keyExtractor is null");
        final java.util.List<T> duplicated = Collections.duplicatesBy(this, keyExtractor);
        return duplicated.isEmpty() ? empty() : ofAll(duplicated);
    }

    /**
     * The elements without duplicates, keeping the last occurrence of each group of elements the comparator calls
     * equal, in the order of those last occurrences.
     * <p>
     * Complexity: O(n log n) comparisons; the whole Stream is forced, because the last occurrence decides.
     *
     * @param comparator decides which elements are duplicates
     * @return a new Stream
     * @throws NullPointerException if {@code comparator} is null
     */
    default Stream<T> distinctByKeepLast(Comparator<? super T> comparator) {
        Objects.requireNonNull(comparator, "comparator is null");
        return ofAll(Iterator.ofAll(this).distinctByKeepLast(comparator));
    }

    /**
     * The elements without duplicates, keeping the last occurrence of each key, in the order of those last
     * occurrences.
     * <p>
     * Complexity: O(n), one key per element; the whole Stream is forced, because the last occurrence decides.
     *
     * @param keyExtractor computes the key an element is deduplicated by
     * @param <U>          the key type
     * @return a new Stream
     * @throws NullPointerException if {@code keyExtractor} is null
     */
    default <U extends @Nullable Object> Stream<T> distinctByKeepLast(Function<? super T, ? extends U> keyExtractor) {
        Objects.requireNonNull(keyExtractor, "keyExtractor is null");
        return ofAll(Iterator.ofAll(this).distinctByKeepLast(keyExtractor));
    }

    /**
     * Returns a new {@code Stream} without the first {@code n} elements,
     * or an empty instance if this contains fewer than {@code n} elements.
     * <p>
     * Complexity: O(n); the first {@code n} elements are forced, the rest stays deferred.
     *
     * @param n the number of elements to drop
     * @return a new instance excluding the first {@code n} elements
     */
    default Stream<T> drop(int n) {
        Stream<T> stream = this;
        while (n-- > 0 && !stream.isEmpty()) {
            stream = stream.tail();
        }
        return stream;
    }

    /**
     * Returns a new {@code Stream} starting from the first element
     * that satisfies the given {@code predicate}, dropping all preceding elements.
     * <p>
     * Complexity: O(k); the k skipped elements are forced, the rest stays deferred.
     *
     * @param predicate a condition tested on each element
     * @return a new instance starting from the first element matching the predicate
     * @throws NullPointerException if {@code predicate} is null
     */
    default Stream<T> dropUntil(Predicate<? super T> predicate) {
        Objects.requireNonNull(predicate, "predicate is null");
        return dropWhile(predicate.negate());
    }

    /**
     * Returns a new {@code Stream} starting from the first element
     * that does not satisfy the given {@code predicate}, dropping all preceding elements.
     * <p>
     * This is equivalent to {@code dropUntil(predicate.negate())}, which is useful
     * for method references that cannot be negated directly.
     * <p>
     * Complexity: O(k); the k skipped elements are forced, the rest stays deferred.
     *
     * @param predicate a condition tested on each element
     * @return a new instance starting from the first element not matching the predicate
     * @throws NullPointerException if {@code predicate} is null
     */
    default Stream<T> dropWhile(Predicate<? super T> predicate) {
        Objects.requireNonNull(predicate, "predicate is null");
        Stream<T> stream = this;
        while (!stream.isEmpty() && predicate.test(stream.head())) {
            stream = stream.tail();
        }
        return stream;
    }

    /**
     * Returns a new {@code Stream} without the last {@code n} elements,
     * or an empty instance if this contains fewer than {@code n} elements.
     * <p>
     * Complexity: lazy; the result runs {@code n} elements behind this Stream, so it works on an infinite Stream.
     *
     * @param n the number of elements to drop from the end
     * @return a new instance excluding the last {@code n} elements
     */
    default Stream<T> dropRight(int n) {
        if (n <= 0) {
            return this;
        } else {
            return DropRight.apply(take(n).toList(), List.empty(), drop(n));
        }
    }

    /**
     * The elements up to and including the last one satisfying {@code predicate}: the elements after it are dropped.
     * <p>
     * Complexity: O(n); the whole Stream is forced, because the last matching element decides.
     *
     * @param predicate the condition, tested from the end
     * @return a new Stream
     * @throws NullPointerException if {@code predicate} is null
     */
    default Stream<T> dropRightUntil(Predicate<? super T> predicate) {
        Objects.requireNonNull(predicate, "predicate is null");
        return reverse().dropUntil(predicate).reverse();
    }

    /**
     * The elements up to and including the last one not satisfying {@code predicate}, that is
     * {@code dropRightUntil(predicate.negate())}.
     * <p>
     * Complexity: O(n); the whole Stream is forced, because the last matching element decides.
     *
     * @param predicate the condition, tested from the end
     * @return a new Stream
     * @throws NullPointerException if {@code predicate} is null
     */
    default Stream<T> dropRightWhile(Predicate<? super T> predicate) {
        Objects.requireNonNull(predicate, "predicate is null");
        return dropRightUntil(predicate.negate());
    }

    /**
     * Returns a new traversable containing only the elements that satisfy the given predicate.
     * <p>
     * Complexity: lazy; the elements are forced until the first match, the rest on demand.
     *
     * @param predicate the condition to test elements
     * @return a traversable with elements matching the predicate
     * @throws NullPointerException if {@code predicate} is null
     */
    default Stream<T> filter(Predicate<? super T> predicate) {
        Objects.requireNonNull(predicate, "predicate is null");
        if (isEmpty()) {
            return this;
        } else {
            Stream<T> stream = this;
            while (!stream.isEmpty() && !predicate.test(stream.head())) {
                stream = stream.tail();
            }
            final Stream<T> finalStream = stream;
            return stream.isEmpty() ? Stream.empty()
                                    : cons(stream.head(), () -> finalStream.tail().filter(predicate));
        }
    }

    default Stream<T> reject(Predicate<? super T> predicate) {
        Objects.requireNonNull(predicate, "predicate is null");
        return Collections.reject(this, predicate, kept -> filter(kept));
    }

    default <U extends @Nullable Object> Stream<U> flatMap(Function<? super T, ? extends Iterable<? extends U>> mapper) {
        Objects.requireNonNull(mapper, "mapper is null");
        return isEmpty() ? Empty.instance() : Stream.ofAll(new FlatMapIterator<>(Iterator.ofAll(this), mapper));
    }

    /**
     * The element at {@code index}.
     * <p>
     * Complexity: O(index); the first {@code index + 1} elements are forced.
     *
     * @param index the position
     * @return the element at that position
     * @throws IndexOutOfBoundsException if {@code index} is negative or not less than {@code length()}
     */
    default T get(int index) {
        if (isEmpty()) {
            throw new IndexOutOfBoundsException("get(" + index + ") on Nil");
        }
        if (index < 0) {
            throw new IndexOutOfBoundsException("get(" + index + ")");
        }
        Stream<T> stream = this;
        for (int i = index - 1; i >= 0; i--) {
            stream = stream.tail();
            if (stream.isEmpty()) {
                throw new IndexOutOfBoundsException("get(" + index + ") on Stream of size " + (index - i));
            }
        }
        return stream.head();
    }

    default <C extends @Nullable Object> Map<C, Stream<T>> groupBy(Function<? super T, ? extends C> classifier) {
        return com.guizmaii.zazr.collection.internal.Collections.groupBy(this, classifier, Stream::ofAll);
    }

    /**
     * The index of the first occurrence of {@code element} at or after {@code from}, or -1. A negative {@code from}
     * counts as 0.
     * <p>
     * Complexity: O(n); the elements are forced until the element is found.
     *
     * @param element the element to find
     * @param from    the first position to look at
     * @return the first index {@code >= from} of the element, or -1 if absent
     */
    default int indexOf(T element, int from) {
        int index = 0;
        for (Stream<T> stream = this; !stream.isEmpty(); stream = stream.tail(), index++) {
            if (index >= from && Objects.equals(stream.head(), element)) {
                return index;
            }
        }
        return -1;
    }

    /**
     * Returns all elements of this Stream except the last one.
     * <p>
     * This is the dual of {@link #tail()}.
     * <p>
     * Complexity: lazy; the result runs one element behind this Stream, so only the first two elements are forced.
     *
     * @return a new instance containing all elements except the last
     * @throws UnsupportedOperationException if this Stream is empty
     */
    default Stream<T> init() {
        if (isEmpty()) {
            throw new UnsupportedOperationException("init of empty stream");
        } else {
            final Stream<T> tail = tail();
            if (tail.isEmpty()) {
                return Empty.instance();
            } else {
                return cons(head(), tail::init);
            }
        }
    }

    /**
     * Returns all elements of this Stream except the last one, wrapped in an {@code Option}.
     * <p>
     * This is the dual of {@link #tailOption()}.
     * <p>
     * Complexity: lazy; see {@link #init()}.
     *
     * @return {@code Some(traversable)} if non-empty, or {@code None} if this Stream is empty
     */
    default Option<Stream<T>> initOption() {
        return isEmpty() ? Option.none() : Option.some(init());
    }

    /**
     * Because {@code Stream} is lazy, only {@code index < 0} (and {@code index > 0} on an empty Stream)
     * is detected when this method is called; for {@code index > length()} the
     * {@code IndexOutOfBoundsException} is thrown only once the returned Stream is traversed as far
     * as the offending position.
     * <p>
     * Complexity: lazy; the first {@code index} elements are forced when the result reaches them.
     */
    default Stream<T> insert(int index, T element) {
        if (index < 0) {
            throw new IndexOutOfBoundsException("insert(" + index + ", e)");
        } else if (index == 0) {
            return cons(element, () -> this);
        } else if (isEmpty()) {
            throw new IndexOutOfBoundsException("insert(" + index + ", e) on Nil");
        } else {
            return cons(head(), () -> tail().insert(index - 1, element));
        }
    }

    /**
     * Because {@code Stream} is lazy, only {@code index < 0} (and {@code index > 0} on an empty Stream)
     * is detected when this method is called; for {@code index > length()} the
     * {@code IndexOutOfBoundsException} is thrown only once the returned Stream is traversed as far
     * as the offending position.
     * <p>
     * Complexity: lazy; the first {@code index} elements are forced when the result reaches them.
     */
    default Stream<T> insertAll(int index, Iterable<? extends T> elements) {
        Objects.requireNonNull(elements, "elements is null");
        if (index < 0) {
            throw new IndexOutOfBoundsException("insertAll(" + index + ", elements)");
        } else if (index == 0) {
            return isEmpty() ? Stream.ofAll(elements) : Stream.<T> ofAll(elements).appendAll(this);
        } else if (isEmpty()) {
            throw new IndexOutOfBoundsException("insertAll(" + index + ", elements) on Nil");
        } else {
            return cons(head(), () -> tail().insertAll(index - 1, elements));
        }
    }

    /**
     * The elements with {@code element} inserted between every two of them.
     * <p>
     * Complexity: lazy; one element is forced, the rest on demand.
     *
     * @param element the separator
     * @return a new Stream, or this Stream if it is empty
     */
    default Stream<T> intersperse(T element) {
        if (isEmpty()) {
            return this;
        } else {
            return cons(head(), () -> {
                final Stream<T> tail = tail();
                return tail.isEmpty() ? tail : cons(element, () -> tail.intersperse(element));
            });
        }
    }

    /**
     * Returns the last element of this Stream.
     * <p>
     * Complexity: O(n); the whole Stream is forced, so it does not terminate on an infinite Stream.
     *
     * @return the last element
     * @throws NoSuchElementException if this Stream is empty
     */
    default T last() {
        return Collections.last(this);
    }

    /**
     * The index of the last occurrence of {@code element} at or before {@code end}, or -1.
     * <p>
     * Complexity: O(n); the elements up to {@code end} are forced.
     *
     * @param element the element to find
     * @param end     the last position to look at
     * @return the last index {@code <= end} of the element, or -1 if absent
     */
    default int lastIndexOf(T element, int end) {
        int result = -1, index = 0;
        for (Stream<T> stream = this; index <= end && !stream.isEmpty(); stream = stream.tail(), index++) {
            if (Objects.equals(stream.head(), element)) {
                result = index;
            }
        }
        return result;
    }

    /**
     * Returns the number of elements in this Stream.
     * <p>
     * Equivalent to {@link #size()}.
     * <p>
     * Complexity: O(n); the whole Stream is forced, so it does not terminate on an infinite Stream.
     *
     * @return the number of elements
     */
    default int length() {
        return foldLeft(0, (n, ignored) -> n + 1);
    }

    default <U extends @Nullable Object> Stream<U> map(Function<? super T, ? extends U> mapper) {
        Objects.requireNonNull(mapper, "mapper is null");
        if (isEmpty()) {
            return Empty.instance();
        } else {
            return cons(mapper.apply(head()), () -> tail().map(mapper));
        }
    }

    default <U extends @Nullable Object> Stream<U> collect(Function<? super T, ? extends Option<? extends U>> mapper) {
        Objects.requireNonNull(mapper, "mapper is null");
        // walk to the first kept element now, the rest lazily; the Option found on the way is the head, so the
        // mapper never runs twice for an element
        Stream<T> stream = this;
        while (!stream.isEmpty()) {
            final Option<? extends U> collected = Objects.requireNonNull(mapper.apply(stream.head()), "Stream.collect: mapper returned null");
            if (collected.isDefined()) {
                final Stream<T> tail = stream.tail();
                return cons(collected.get(), () -> tail.collect(mapper));
            }
            stream = stream.tail();
        }
        return Empty.instance();
    }

    default <U extends @Nullable Object> Stream<U> as(U value) {
        return map(ignored -> value);
    }

    /**
     * This Stream padded on the right with {@code element} until it is {@code length} long.
     * <p>
     * Complexity: lazy; the padding is appended without forcing this Stream.
     *
     * @param length  the target length
     * @param element the padding element
     * @return a new Stream, or this Stream if {@code length} is not positive
     */
    default Stream<T> padTo(int length, T element) {
        if (length <= 0) {
            return this;
        } else if (isEmpty()) {
            return Stream.continually(element).take(length);
        } else {
            return cons(head(), () -> tail().padTo(length - 1, element));
        }
    }

    /**
     * This Stream padded on the left with {@code element} until it is {@code length} long.
     * <p>
     * Complexity: O(n); the whole Stream is forced, because its length decides how much padding is needed.
     *
     * @param length  the target length
     * @param element the padding element
     * @return a new Stream, or this Stream if it is already at least {@code length} long
     */
    default Stream<T> leftPadTo(int length, T element) {
        final int actualLength = length();
        if (length <= actualLength) {
            return this;
        } else {
            return Stream.continually(element).take(length - actualLength).appendAll(this);
        }
    }

    default Stream<T> orElse(Iterable<? extends T> other) {
        return isEmpty() ? ofAll(other) : this;
    }

    default Stream<T> orElse(Supplier<? extends Iterable<? extends T>> supplier) {
        return isEmpty() ? ofAll(supplier.get()) : this;
    }

    /**
     * This Stream with {@code replaced} elements from {@code from} on replaced by {@code that}. A negative
     * {@code from} or {@code replaced} counts as 0.
     * <p>
     * Complexity: lazy; the elements are forced as the result reaches them.
     *
     * @param from     the first replaced position
     * @param that     the replacement elements
     * @param replaced how many elements are replaced
     * @return a new Stream
     * @throws NullPointerException if {@code that} is null
     */
    default Stream<T> patch(int from, Iterable<? extends T> that, int replaced) {
        from = Math.max(from, 0);
        replaced = Math.max(replaced, 0);
        Stream<T> result = take(from).appendAll(that);
        from += replaced;
        result = result.appendAll(drop(from));
        return result;
    }

    default Tuple2<Stream<T>, Stream<T>> partition(Predicate<? super T> predicate) {
        Objects.requireNonNull(predicate, "predicate is null");
        return Tuple.of(filter(predicate), filter(predicate.negate()));
    }

    /**
     * Splits the elements into a left and a right side according to the {@link Either} {@code f} returns for each: the
     * generalisation of {@link #partition(Predicate)}. Lazy like {@code partition}: both sides are Streams read from
     * one memoised Stream of the results of {@code f}, so {@code f} is called once per element, in order, when
     * either side first reaches that element, and never again.
     * <p>
     * Complexity: lazy; as every Stream is head-strict, each side is forced to its first element when this method is
     * called, and each further element of a side is found when that side reaches it. The results of {@code f} that
     * one side has passed stay memoised until the other side has passed them too. On an infinite Stream, a side
     * that never receives an element is searched forever, so the call does not return (as with {@code partition}).
     *
     * @param f   Classifies an element
     * @param <L> Component type of the left side
     * @param <R> Component type of the right side
     * @return the left values and the right values, each in the order of the elements they come from
     * @throws NullPointerException if {@code f} is null, or when it returns null for an element a side reaches
     */
    default <L extends @Nullable Object, R extends @Nullable Object> Tuple2<Stream<L>, Stream<R>> partitionMap(Function<? super T, ? extends Either<? extends L, ? extends R>> f) {
        Objects.requireNonNull(f, "f is null");
        final Stream<Either<? extends L, ? extends R>> results =
                this.<Either<? extends L, ? extends R>> map(element -> Objects.requireNonNull(f.apply(element), "Stream.partitionMap: f returned null"));
        return Tuple.of(lefts(results), rights(results));
    }

    // the left values of a Stream of results, found lazily: skips the Rights to the next Left, now, the rest on demand
    private static <L extends @Nullable Object> Stream<L> lefts(Stream<? extends Either<? extends L, ?>> results) {
        Stream<? extends Either<? extends L, ?>> stream = results;
        while (!stream.isEmpty()) {
            if (stream.head() instanceof Either.Left<? extends L, ?>(var left)) {
                final Stream<? extends Either<? extends L, ?>> rest = stream;
                return cons(left, () -> lefts(rest.tail()));
            }
            stream = stream.tail();
        }
        return empty();
    }

    // the right values of a Stream of results, found lazily: skips the Lefts to the next Right, now, the rest on demand
    private static <R extends @Nullable Object> Stream<R> rights(Stream<? extends Either<?, ? extends R>> results) {
        Stream<? extends Either<?, ? extends R>> stream = results;
        while (!stream.isEmpty()) {
            if (stream.head() instanceof Either.Right<?, ? extends R>(var right)) {
                final Stream<? extends Either<?, ? extends R>> rest = stream;
                return cons(right, () -> rights(rest.tail()));
            }
            stream = stream.tail();
        }
        return empty();
    }

    /**
     * Runs {@code action} on every element and returns this instance, to observe the elements in the middle of a
     * chain of calls. The action runs on the head now and on each other element when that element is evaluated.
     * Whatever the action throws propagates to the caller.
     *
     * @param action what to do with each element
     * @return this Stream if it is empty; otherwise a new, structurally equal Stream whose elements are handed to
     *         {@code action} lazily as they are traversed
     * @throws NullPointerException if {@code action} is null
     */
    default Stream<T> tap(Consumer<? super T> action) {
        Objects.requireNonNull(action, "action is null");
        if (isEmpty()) {
            return this;
        } else {
            final T head = head();
            action.accept(head);
            return cons(head, () -> tail().tap(action));
        }
    }

    /**
     * All distinct permutations of the elements.
     * <p>
     * Complexity: O(n!) permutations; the whole Stream is forced.
     *
     * @return the permutations
     */
    default Stream<Stream<T>> permutations() {
        if (isEmpty()) {
            return Empty.instance();
        } else {
            final Stream<T> tail = tail();
            if (tail.isEmpty()) {
                return Stream.of(this);
            } else {
                final Stream<Stream<T>> zero = Empty.instance();
                return distinct().foldLeft(zero, (xs, x) -> {
                    final Function<Stream<T>, Stream<T>> prepend = l -> l.prepend(x);
                    return xs.appendAll(remove(x).permutations().map(prepend));
                });
            }
        }
    }

    /**
     * A new Stream with {@code element} in front of this one.
     * <p>
     * Complexity: O(1); nothing is forced.
     *
     * @param element the new head
     * @return a new Stream starting with the given element
     */
    default Stream<T> prepend(T element) {
        return cons(element, () -> this);
    }

    /**
     * A new Stream with {@code elements} in front of this one, in iteration order.
     * <p>
     * Complexity: O(1); the head of this Stream is forced, the rest stays deferred.
     *
     * @param elements the elements to prepend
     * @return a new Stream starting with the given elements, or this Stream if there are none
     * @throws NullPointerException if {@code elements} is null
     */
    default Stream<T> prependAll(Iterable<? extends T> elements) {
        Objects.requireNonNull(elements, "elements is null");
        if (isEmpty()) {
            if (elements instanceof Stream) {
                @SuppressWarnings("unchecked")
                final Stream<T> stream = (Stream<T>) elements;
                return stream;
            } else {
                return Stream.ofAll(elements);
            }
        } else {
            return Stream.<T> ofAll(elements).appendAll(this);
        }
    }

    /**
     * This Stream without the first occurrence of {@code element}.
     * <p>
     * Complexity: lazy; the elements are forced until the first occurrence, the rest on demand.
     *
     * @param element the element to remove
     * @return a new Stream, or this Stream if it is empty
     */
    default Stream<T> remove(T element) {
        if (isEmpty()) {
            return this;
        } else {
            final T head = head();
            return Objects.equals(head, element) ? tail() : cons(head, () -> tail().remove(element));
        }
    }

    /**
     * This Stream without the first element satisfying {@code predicate}.
     * <p>
     * Complexity: lazy; the elements are forced until the first match, the rest on demand.
     *
     * @param predicate the condition
     * @return a new Stream, or this Stream if it is empty
     * @throws NullPointerException if {@code predicate} is null
     */
    default Stream<T> removeFirst(Predicate<T> predicate) {
        Objects.requireNonNull(predicate, "predicate is null");
        if (isEmpty()) {
            return this;
        } else {
            final T head = head();
            return predicate.test(head) ? tail() : cons(head, () -> tail().removeFirst(predicate));
        }
    }

    /**
     * This Stream without the last element satisfying {@code predicate}.
     * <p>
     * Complexity: O(n); the whole Stream is forced, because the last match decides.
     *
     * @param predicate the condition
     * @return a new Stream, or this Stream if it is empty
     * @throws NullPointerException if {@code predicate} is null
     */
    default Stream<T> removeLast(Predicate<T> predicate) {
        return isEmpty() ? this : reverse().removeFirst(predicate).reverse();
    }

    /**
     * Because {@code Stream} is lazy, only {@code index < 0} and an empty Stream are detected when
     * this method is called; for {@code index >= length()} on a non-empty Stream the
     * {@code IndexOutOfBoundsException} is thrown only once the returned Stream is traversed as far
     * as the offending position.
     * <p>
     * Complexity: lazy; the first {@code index} elements are forced when the result reaches them.
     */
    default Stream<T> removeAt(int index) {
        if (index < 0) {
            throw new IndexOutOfBoundsException("removeAt(" + index + ")");
        } else if (isEmpty()) {
            throw new IndexOutOfBoundsException("removeAt(" + index + ") on Nil");
        } else if (index == 0) {
            return tail();
        } else {
            return cons(head(), () -> tail().removeAt(index - 1));
        }
    }

    /**
     * This Stream without any occurrence of {@code element}.
     * <p>
     * Complexity: lazy; each element is forced when the result reaches it.
     *
     * @param element the element to remove
     * @return a new Stream
     */
    default Stream<T> removeAll(T element) {
        return com.guizmaii.zazr.collection.internal.Collections.removeAll(this, element, kept -> filter(kept));
    }

    /**
     * This Stream without any occurrence of any of {@code elements}.
     * <p>
     * Complexity: lazy; the removed elements are hashed once, then each element is forced when the result reaches it.
     *
     * @param elements the elements to remove
     * @return a new Stream
     * @throws NullPointerException if {@code elements} is null
     */
    default Stream<T> removeAll(Iterable<? extends T> elements) {
        return com.guizmaii.zazr.collection.internal.Collections.removeAll(this, elements, kept -> filter(kept));
    }

    /**
     * This Stream without the elements satisfying {@code predicate}.
     * <p>
     * Complexity: lazy; each element is forced when the result reaches it.
     *
     * @deprecated use {@link #reject(Predicate)}
     * @param predicate the condition
     * @return a new Stream
     * @throws NullPointerException if {@code predicate} is null
     */
    @Deprecated
    default Stream<T> removeAll(Predicate<? super T> predicate) {
        Objects.requireNonNull(predicate, "predicate is null");
        return reject(predicate);
    }

    /**
     * Replaces the first occurrence of {@code currentElement} with {@code newElement}, if it exists.
     * <p>
     * Complexity: lazy; the elements are forced until the first occurrence, the rest on demand.
     *
     * @param currentElement the element to be replaced
     * @param newElement     the replacement element
     * @return a new Stream with the first occurrence of {@code currentElement} replaced by {@code newElement}
     */
    default Stream<T> replace(T currentElement, T newElement) {
        if (isEmpty()) {
            return this;
        } else {
            final T head = head();
            if (Objects.equals(head, currentElement)) {
                return cons(newElement, this::tail);
            } else {
                return cons(head, () -> tail().replace(currentElement, newElement));
            }
        }
    }

    /**
     * Replaces all occurrences of {@code currentElement} with {@code newElement}.
     * <p>
     * Complexity: lazy; each element is forced when the result reaches it.
     *
     * @param currentElement the element to be replaced
     * @param newElement     the replacement element
     * @return a new Stream with all occurrences of {@code currentElement} replaced by {@code newElement}
     */
    default Stream<T> replaceAll(T currentElement, T newElement) {
        if (isEmpty()) {
            return this;
        } else {
            final T head = head();
            final T newHead = Objects.equals(head, currentElement) ? newElement : head;
            return cons(newHead, () -> tail().replaceAll(currentElement, newElement));
        }
    }

    /**
     * Retains only the elements from this Stream that are contained in the given {@code elements}.
     * <p>
     * Complexity: lazy; the retained elements are hashed once, then each element is forced when the result reaches it.
     *
     * @param elements the elements to keep
     * @return a new Stream containing only the elements present in {@code elements}, in their original order
     * @throws NullPointerException if {@code elements} is null
     */
    default Stream<T> retainAll(Iterable<? extends T> elements) {
        return com.guizmaii.zazr.collection.internal.Collections.retainAll(this, elements, kept -> filter(kept));
    }

    /**
     * The elements in reverse order.
     * <p>
     * Complexity: O(n); the whole Stream is forced.
     *
     * @return a new Stream, or this Stream if it is empty
     */
    default Stream<T> reverse() {
        return isEmpty() ? this : foldLeft(Stream.empty(), Stream::prepend);
    }

    /**
     * Rotates the elements {@code n} positions to the left: {@code Stream(1, 2, 3, 4, 5).rotateLeft(2)} is
     * {@code Stream(3, 4, 5, 1, 2)}. A negative {@code n} rotates right; {@code n} is taken modulo the length.
     * <p>
     * Complexity: O(n); the whole Stream is forced, because its length decides the rotation. {@code n == 0} is
     * O(1) and forces nothing, so it works on an infinite Stream.
     *
     * @param n the distance
     * @return the rotated Stream, or this Stream if the rotation is a multiple of the length
     */
    default Stream<T> rotateLeft(int n) {
        // n == 0 before length(): a no-op rotation must not walk the elements, let alone force a lazy sequence
        if (n == 0 || isEmpty()) {
            return this;
        }
        final int k = Math.floorMod(n, length());
        return (k == 0) ? this : drop(k).appendAll(take(k));
    }

    /**
     * Rotates the elements {@code n} positions to the right: {@code Stream(1, 2, 3, 4, 5).rotateRight(2)} is
     * {@code Stream(4, 5, 1, 2, 3)}. A negative {@code n} rotates left; {@code n} is taken modulo the length.
     * <p>
     * Complexity: O(n); the whole Stream is forced, because its length decides the rotation. {@code n == 0} is
     * O(1) and forces nothing, so it works on an infinite Stream.
     *
     * @param n the distance
     * @return the rotated Stream, or this Stream if the rotation is a multiple of the length
     */
    default Stream<T> rotateRight(int n) {
        // n == 0 before length(): a no-op rotation must not walk the elements, let alone force a lazy sequence
        if (n == 0 || isEmpty()) {
            return this;
        }
        final int k = Math.floorMod(n, length());
        return (k == 0) ? this : takeRight(k).appendAll(dropRight(k));
    }

    /**
     * Computes a prefix scan of the elements of this Stream.
     * <p>
     * The neutral element {@code zero} may be applied more than once.
     * <p>
     * Complexity: lazy; each element is forced when the result reaches it.
     *
     * @param zero      the neutral element for the operator
     * @param operation an associative binary operator
     * @return a new Stream containing the prefix scan of the elements
     * @throws NullPointerException if {@code operation} is null
     */
    default Stream<T> scan(T zero, BiFunction<? super T, ? super T, ? extends T> operation) {
        return scanLeft(zero, operation);
    }

    /**
     * Produces a collection containing cumulative results of applying the operator from left to right.
     * <p>
     * The results are produced as the underlying elements are consumed, so {@code scanLeft} terminates even for
     * an infinite Stream as long as only a finite prefix of the result is consumed. Contrast with
     * {@link #scanRight}, which is not lazy and will not terminate for an infinite Stream.
     * <p>
     * Complexity: lazy; each element is forced when the result reaches it.
     *
     * @param <U>       the type of the resulting elements
     * @param zero      the initial value
     * @param operation a binary operator applied to the intermediate result and each element
     * @return a new Stream containing the cumulative results
     * @throws NullPointerException if {@code operation} is null
     */
    default <U extends @Nullable Object> Stream<U> scanLeft(U zero, BiFunction<? super U, ? super T, ? extends U> operation) {
        // lazily streams the elements of an iterator
        return com.guizmaii.zazr.collection.internal.Collections.scanLeft(this, zero, operation, Iterator::toStream);
    }

    // not lazy!
    /**
     * Produces a collection containing cumulative results of applying the operator from right to left.
     * <p>
     * The head of the result is the last cumulative result.
     * <p>
     * Complexity: O(n); the whole Stream is forced, because the fold starts at the end.
     *
     * @param <U>       the type of the resulting elements
     * @param zero      the initial value
     * @param operation a binary operator applied to each element and the intermediate result
     * @return a new Stream containing the cumulative results
     * @throws NullPointerException if {@code operation} is null
     */
    default <U extends @Nullable Object> Stream<U> scanRight(U zero, BiFunction<? super T, ? super U, ? extends U> operation) {
        return com.guizmaii.zazr.collection.internal.Collections.scanRight(this, zero, operation, Iterator::toStream);
    }

    /**
     * The elements in a random order, drawn from a default source of randomness.
     * <p>
     * Complexity: O(n); the whole Stream is forced.
     *
     * @return a new Stream, or this Stream if it has fewer than two elements
     */
    default Stream<T> shuffle() {
        return com.guizmaii.zazr.collection.internal.Collections.shuffle(this, Stream::ofAll);
    }

    /**
     * The elements from {@code beginIndex} inclusive to {@code endIndex} exclusive, both clamped to the bounds of
     * this Stream.
     * <p>
     * Complexity: lazy; the elements up to {@code endIndex} are forced as the result reaches them.
     *
     * @param beginIndex the first position
     * @param endIndex   the position after the last one
     * @return a new Stream, empty if the range is empty
     */
    default Stream<T> slice(int beginIndex, int endIndex) {
        if (beginIndex >= endIndex || isEmpty()) {
            return empty();
        } else {
            final int lowerBound = Math.max(beginIndex, 0);
            if (lowerBound == 0) {
                return cons(head(), () -> tail().slice(0, endIndex - 1));
            } else {
                return tail().slice(lowerBound - 1, endIndex - 1);
            }
        }
    }

    /**
     * The elements in ascending natural order (a stable sort).
     * <p>
     * Complexity: O(n log n) comparisons; the whole Stream is forced.
     *
     * @return a new sorted Stream, or this Stream if it is empty
     * @throws ClassCastException if {@code T} is not {@code Comparable}
     */
    default Stream<T> sorted() {
        return isEmpty() ? this : stream().sorted().collect(Stream.collector());
    }

    /**
     * The elements in the order of {@code comparator} (a stable sort).
     * <p>
     * Complexity: O(n log n) comparisons; the whole Stream is forced.
     *
     * @param comparator the order
     * @return a new sorted Stream, or this Stream if it is empty
     * @throws NullPointerException if {@code comparator} is null
     */
    default Stream<T> sorted(Comparator<? super T> comparator) {
        Objects.requireNonNull(comparator, "comparator is null");
        return isEmpty() ? this : stream().sorted(comparator).collect(Stream.collector());
    }

    /**
     * The elements sorted by the natural order of the key {@code mapper} computes (a stable sort).
     * <p>
     * Complexity: O(n log n) comparisons; the whole Stream is forced. The key is recomputed at every comparison.
     *
     * @param mapper computes the sort key
     * @param <U>    the key type
     * @return a new sorted Stream, or this Stream if it is empty
     * @throws NullPointerException if {@code mapper} is null
     */
    default <U extends Comparable<? super U>> Stream<T> sortBy(Function<? super T, ? extends U> mapper) {
        return sortBy(U::compareTo, mapper);
    }

    /**
     * The elements sorted by {@code comparator} applied to the key {@code mapper} computes (a stable sort).
     * <p>
     * Complexity: O(n log n) comparisons; the whole Stream is forced. The key is recomputed at every comparison.
     *
     * @param comparator the order of the keys
     * @param mapper     computes the sort key
     * @param <U>        the key type
     * @return a new sorted Stream, or this Stream if it is empty
     * @throws NullPointerException if {@code comparator} or {@code mapper} is null
     */
    default <U extends @Nullable Object> Stream<T> sortBy(Comparator<? super U> comparator, Function<? super T, ? extends U> mapper) {
        Objects.requireNonNull(comparator, "comparator is null");
        Objects.requireNonNull(mapper, "mapper is null");
        return sorted((e1, e2) -> comparator.compare(mapper.apply(e1), mapper.apply(e2)));
    }

    /**
     * Splits this {@code Stream} into a prefix and remainder according to the given {@code predicate}.
     * <p>
     * The first element of the returned {@code Tuple} is the longest prefix of elements satisfying {@code predicate},
     * and the second element is the remaining elements.
     * <p>
     * Complexity: O(k); the k elements of the prefix are forced, the suffix stays deferred.
     *
     * @param predicate a predicate used to determine the prefix
     * @return a {@code Tuple} containing the prefix and remainder
     * @throws NullPointerException if {@code predicate} is null
     */
    default Tuple2<Stream<T>, Stream<T>> span(Predicate<? super T> predicate) {
        Objects.requireNonNull(predicate, "predicate is null");
        return Tuple.of(takeWhile(predicate), dropWhile(predicate));
    }

    /**
     * This Stream split in two at position {@code n}: the first {@code n} elements and the rest.
     * <p>
     * Complexity: O(n); the first {@code n} elements are forced, the suffix stays deferred.
     *
     * @param n the position of the split
     * @return the prefix and the suffix
     */
    default Tuple2<Stream<T>, Stream<T>> splitAt(int n) {
        return Tuple.of(take(n), drop(n));
    }

    /**
     * This Stream split in two before the first element satisfying {@code predicate}. If no element satisfies it, the
     * whole Stream is the first part.
     * <p>
     * Complexity: O(k); the k elements before the split are forced, the suffix stays deferred.
     *
     * @param predicate the condition
     * @return the prefix and the suffix
     */
    default Tuple2<Stream<T>, Stream<T>> splitAt(Predicate<? super T> predicate) {
        Objects.requireNonNull(predicate, "predicate is null");
        return Tuple.of(takeWhile(predicate.negate()), dropWhile(predicate.negate()));
    }

    /**
     * This Stream split in two after the first element satisfying {@code predicate}. If no element satisfies it, the
     * whole Stream is the first part.
     * <p>
     * Complexity: O(k); the k elements up to the split are forced, the suffix stays deferred.
     *
     * @param predicate the condition
     * @return the prefix including the matching element, and the suffix
     */
    default Tuple2<Stream<T>, Stream<T>> splitAtInclusive(Predicate<? super T> predicate) {
        final Tuple2<Stream<T>, Stream<T>> split = splitAt(predicate);
        if (split._2().isEmpty()) {
            return split;
        } else {
            return Tuple.of(split._1().append(split._2().head()), split._2().tail());
        }
    }

    /**
     * The elements from {@code beginIndex} on.
     * <p>
     * Complexity: O(beginIndex); the elements before {@code beginIndex} are forced, the rest stays deferred.
     *
     * @param beginIndex the first position
     * @return a new Stream
     * @throws IndexOutOfBoundsException if {@code beginIndex} is negative or greater than {@code length()}
     */
    default Stream<T> subSequence(int beginIndex) {
        if (beginIndex < 0) {
            throw new IndexOutOfBoundsException("subSequence(" + beginIndex + ")");
        }
        Stream<T> result = this;
        for (int i = 0; i < beginIndex; i++, result = result.tail()) {
            if (result.isEmpty()) {
                throw new IndexOutOfBoundsException("subSequence(" + beginIndex + ") on Stream of size " + i);
            }
        }
        return result;
    }

    /**
     * The elements from {@code beginIndex} inclusive to {@code endIndex} exclusive.
     * <p>
     * Complexity: O(beginIndex); the elements before {@code beginIndex} are forced, the rest when the result
     * reaches them.
     * <p>
     * Because {@code Stream} is lazy, {@code beginIndex} is validated eagerly, but if
     * {@code endIndex > length()} the {@code IndexOutOfBoundsException} is only thrown once the
     * returned Stream is traversed as far as the offending position, not when this method is called.
     *
     * @param beginIndex the first position
     * @param endIndex   the position after the last one
     * @return a new Stream
     * @throws IndexOutOfBoundsException if {@code beginIndex} is negative, or once the traversal passes the end
     * @throws IllegalArgumentException  if {@code beginIndex} is greater than {@code endIndex}
     */
    default Stream<T> subSequence(int beginIndex, int endIndex) {
        if (beginIndex < 0) {
            throw new IndexOutOfBoundsException("subSequence(" + beginIndex + ", " + endIndex + ")");
        }
        if (beginIndex > endIndex) {
            throw new IllegalArgumentException("subSequence(" + beginIndex + ", " + endIndex + ")");
        }
        if (beginIndex == endIndex) {
            return Empty.instance();
        } else if (isEmpty()) {
            throw new IndexOutOfBoundsException("subSequence of Nil");
        } else if (beginIndex == 0) {
            return cons(head(), () -> tail().subSequence(0, endIndex - 1));
        } else {
            return tail().subSequence(beginIndex - 1, endIndex - 1);
        }
    }

    /**
     * Returns a new {@code Stream} without its first element.
     * <p>
     * Complexity: O(1); the tail is forced when it is asked for, and memoised.
     *
     * @return a new {@code Stream} containing all elements except the first
     * @throws UnsupportedOperationException if this {@code Stream} is empty
     */
    Stream<T> tail();

    /**
     * Returns a new {@code Stream} without its first element as an {@code Option}.
     * <p>
     * Complexity: O(1); see {@link #tail()}.
     *
     * @return {@code Some(traversable)} if non-empty, otherwise {@code None}
     */
    default Option<Stream<T>> tailOption() {
        return isEmpty() ? Option.none() : Option.some(tail());
    }

    /**
     * Returns the first {@code n} elements of this {@code Stream}, or all elements if {@code n} exceeds the length.
     * <p>
     * If {@code n < 0}, an empty instance is returned. If {@code n > length()}, the full instance is returned.
     * <p>
     * Complexity: lazy; O(1), one element is forced and the rest on demand.
     *
     * @param n the number of elements to take
     * @return a new {@code Stream} containing the first {@code n} elements
     */
    default Stream<T> take(int n) {
        if (n < 1 || isEmpty()) {
            return empty();
        } else if (n == 1) {
            return cons(head(), Stream::empty);
        } else {
            return cons(head(), () -> tail().take(n - 1));
        }
    }

    /**
     * Takes elements from this {@code Stream} until the given predicate holds for an element.
     * <p>
     * Equivalent to {@code takeWhile(predicate.negate())}, but useful when using method references
     * that cannot be negated directly.
     * <p>
     * Complexity: lazy; each element is forced when the result reaches it.
     *
     * @param predicate a condition tested sequentially on the elements
     * @return a new {@code Stream} containing all elements before the first one that satisfies the predicate
     * @throws NullPointerException if {@code predicate} is null
     */
    default Stream<T> takeUntil(Predicate<? super T> predicate) {
        Objects.requireNonNull(predicate, "predicate is null");
        return takeWhile(predicate.negate());
    }

    /**
     * Takes elements from this {@code Stream} while the given predicate holds.
     * <p>
     * Complexity: lazy; each element is forced when the result reaches it.
     *
     * @param predicate a condition tested sequentially on the elements
     * @return a new {@code Stream} containing all elements up to (but not including) the first one
     *         that does not satisfy the predicate
     * @throws NullPointerException if {@code predicate} is null
     */
    default Stream<T> takeWhile(Predicate<? super T> predicate) {
        Objects.requireNonNull(predicate, "predicate is null");
        if (isEmpty()) {
            return Empty.instance();
        } else {
            final T head = head();
            if (predicate.test(head)) {
                return cons(head, () -> tail().takeWhile(predicate));
            } else {
                return Empty.instance();
            }
        }
    }

    /**
     * Returns the last {@code n} elements of this {@code Stream}, or all elements if {@code n} exceeds the length.
     * <p>
     * If {@code n < 0}, an empty instance is returned. If {@code n > length()}, the full instance is returned.
     * <p>
     * Complexity: O(n); the whole Stream is forced, because the last {@code n} elements decide.
     *
     * @param n the number of elements to take from the end
     * @return a new {@code Stream} containing the last {@code n} elements
     */
    default Stream<T> takeRight(int n) {
        Stream<T> right = this;
        Stream<T> remaining = drop(n);
        while (!remaining.isEmpty()) {
            right = right.tail();
            remaining = remaining.tail();
        }
        return right;
    }

    /**
     * The longest suffix whose elements, from the end, do not satisfy {@code predicate}.
     * <p>
     * Complexity: O(n); the whole Stream is forced, because the last matching element decides.
     *
     * @param predicate the condition, tested from the end
     * @return a new Stream
     * @throws NullPointerException if {@code predicate} is null
     */
    default Stream<T> takeRightUntil(Predicate<? super T> predicate) {
        Objects.requireNonNull(predicate, "predicate is null");
        return reverse().takeUntil(predicate).reverse();
    }

    /**
     * The longest suffix whose elements, from the end, all satisfy {@code predicate}.
     * <p>
     * Complexity: O(n); the whole Stream is forced, because the last matching element decides.
     *
     * @param predicate the condition, tested from the end
     * @return a new Stream
     * @throws NullPointerException if {@code predicate} is null
     */
    default Stream<T> takeRightWhile(Predicate<? super T> predicate) {
        Objects.requireNonNull(predicate, "predicate is null");
        return takeRightUntil(predicate.negate());
    }

    default <T1 extends @Nullable Object, T2 extends @Nullable Object> Tuple2<Stream<T1>, Stream<T2>> unzip(
      Function<? super T, Tuple2<? extends T1, ? extends T2>> unzipper) {
        Objects.requireNonNull(unzipper, "unzipper is null");
        final Stream<Tuple2<? extends T1, ? extends T2>> stream = map(unzipper);
        final Stream<T1> stream1 = stream.map(t -> t._1());
        final Stream<T2> stream2 = stream.map(t -> t._2());
        return Tuple.of(stream1, stream2);
    }

    default <T1 extends @Nullable Object, T2 extends @Nullable Object, T3 extends @Nullable Object> Tuple3<Stream<T1>, Stream<T2>, Stream<T3>> unzip3(
      Function<? super T, Tuple3<? extends T1, ? extends T2, ? extends T3>> unzipper) {
        Objects.requireNonNull(unzipper, "unzipper is null");
        final Stream<Tuple3<? extends T1, ? extends T2, ? extends T3>> stream = map(unzipper);
        final Stream<T1> stream1 = stream.map(t -> t._1());
        final Stream<T2> stream2 = stream.map(t -> t._2());
        final Stream<T3> stream3 = stream.map(t -> t._3());
        return Tuple.of(stream1, stream2, stream3);
    }

    /**
     * This Stream with the element at {@code index} replaced by {@code element}.
     * <p>
     * Complexity: O(index); the first {@code index + 1} elements are forced.
     *
     * @param index   the position to update
     * @param element the new element
     * @return a new Stream
     * @throws IndexOutOfBoundsException if {@code index} is negative or not less than {@code length()}
     */
    default Stream<T> update(int index, T element) {
        if (isEmpty()) {
            throw new IndexOutOfBoundsException("update(" + index + ", e) on Nil");
        }
        if (index < 0) {
            throw new IndexOutOfBoundsException("update(" + index + ", e)");
        }
        Stream<T> preceding = Empty.instance();
        Stream<T> tail = this;
        for (int i = index; i > 0; i--, tail = tail.tail()) {
            if (tail.isEmpty()) {
                throw new IndexOutOfBoundsException("update at " + index);
            }
            preceding = preceding.prepend(tail.head());
        }
        if (tail.isEmpty()) {
            throw new IndexOutOfBoundsException("update at " + index);
        }
        // skip the current head element because it is replaced
        return preceding.reverse().appendAll(tail.tail().prepend(element));
    }

    /**
     * This Stream with the element at {@code index} replaced by what {@code updater} computes from it.
     * <p>
     * Complexity: O(index); the first {@code index + 1} elements are forced.
     *
     * @param index   the position to update
     * @param updater computes the new element from the current one
     * @return a new Stream
     * @throws IndexOutOfBoundsException if {@code index} is negative or not less than {@code length()}
     * @throws NullPointerException      if {@code updater} is null
     */
    default Stream<T> update(int index, Function<? super T, ? extends T> updater) {
        Objects.requireNonNull(updater, "updater is null");
        return update(index, updater.apply(get(index)));
    }

    /**
     * Returns a {@code Stream} formed by pairing elements of this {@code Stream} with elements of another
     * {@code Iterable}. Pairing stops when either collection runs out of elements; any remaining elements in the longer
     * collection are ignored.
     * <p>
     * The length of the resulting {@code Stream} is the minimum of the lengths of this {@code Stream} and
     * {@code that}.
     * <p>
     * Complexity: lazy; O(min(n, m)) pairs when consumed.
     *
     * @param <U>  the type of elements in the second half of each pair
     * @param that an {@code Iterable} providing the second element of each pair
     * @return a new {@code Stream} containing pairs of corresponding elements
     * @throws NullPointerException if {@code that} is null
     */
    default <U extends @Nullable Object> Stream<Tuple2<T, U>> zip(Iterable<? extends U> that) {
        return zipWith(that, Tuple::of);
    }

    /**
     * Returns a {@code Stream} by combining elements of this {@code Stream} with elements of another
     * {@code Iterable} using a mapping function. Pairing stops when either collection runs out of elements.
     * <p>
     * The length of the resulting {@code Stream} is the minimum of the lengths of this {@code Stream} and
     * {@code that}.
     * <p>
     * Complexity: lazy; O(min(n, m)) results when consumed.
     *
     * @param <U>    the type of elements in the second parameter of the mapper
     * @param <R>    the type of elements in the resulting {@code Stream}
     * @param that   an {@code Iterable} providing the second parameter of the mapper
     * @param mapper a function that combines elements from this and {@code that} into a new element
     * @return a new {@code Stream} containing mapped elements
     * @throws NullPointerException if {@code that} or {@code mapper} is null
     */
    default <U extends @Nullable Object, R extends @Nullable Object> Stream<R> zipWith(Iterable<? extends U> that, BiFunction<? super T, ? super U, ? extends R> mapper) {
        Objects.requireNonNull(that, "that is null");
        Objects.requireNonNull(mapper, "mapper is null");
        return Stream.ofAll(Iterator.ofAll(this).zipWith(that, mapper));
    }

    /**
     * Returns a {@code Stream} formed by pairing elements of this {@code Stream} with elements of another
     * {@code Iterable}, filling in placeholder elements when one collection is shorter than the other.
     * <p>
     * The length of the resulting {@code Stream} is the maximum of the lengths of this {@code Stream} and
     * {@code that}.
     * <p>
     * If this {@code Stream} is shorter than {@code that}, {@code thisElem} is used as a filler. Conversely, if
     * {@code that} is shorter, {@code thatElem} is used.
     * <p>
     * Complexity: lazy; O(max(n, m)) pairs when consumed.
     *
     * @param <U>      the type of elements in the second half of each pair
     * @param iterable an {@code Iterable} providing the second element of each pair
     * @param thisElem the element used to fill missing values if this {@code Stream} is shorter than {@code iterable}
     * @param thatElem the element used to fill missing values if {@code iterable} is shorter than this {@code Stream}
     * @return a new {@code Stream} containing pairs of elements, including fillers as needed
     * @throws NullPointerException if {@code iterable} is null
     */
    default <U extends @Nullable Object> Stream<Tuple2<T, U>> zipAll(Iterable<? extends U> iterable, T thisElem, U thatElem) {
        Objects.requireNonNull(iterable, "iterable is null");
        return Stream.ofAll(Iterator.ofAll(this).zipAll(iterable, thisElem, thatElem));
    }

    /**
     * Zips this {@code Stream} with its indices, starting at 0.
     * <p>
     * Complexity: lazy; each element is forced when the result reaches it.
     *
     * @return a new {@code Stream} containing each element paired with its index
     */
    default Stream<Tuple2<T, Integer>> zipWithIndex() {
        return zipWithIndex(Tuple::of);
    }

    /**
     * Zips this {@code Stream} with its indices and maps the resulting pairs using the provided mapper.
     * <p>
     * Complexity: lazy; each element is forced when the result reaches it.
     *
     * @param <U>    the type of elements in the resulting {@code Stream}
     * @param mapper a function mapping an element and its index to a new element
     * @return a new {@code Stream} containing the mapped elements
     * @throws NullPointerException if {@code mapper} is null
     */
    default <U extends @Nullable Object> Stream<U> zipWithIndex(BiFunction<? super T, ? super Integer, ? extends U> mapper) {
        Objects.requireNonNull(mapper, "mapper is null");
        return Stream.ofAll(Iterator.ofAll(this).zipWithIndex(mapper));
    }

    /**
     * Extends (continues) this {@code Stream} with a constantly repeated value.
     * <p>
     * Complexity: O(1); the result is infinite and each element is forced when it is reached.
     *
     * @param next value with which the stream should be extended
     * @return new {@code Stream} composed from this stream extended with a Stream of provided value
     */
    default Stream<T> extend(T next) {
        return Stream.ofAll(this.appendAll(Stream.continually(next)));
    }

    /**
     * Extends (continues) this {@code Stream} with values provided by a {@code Supplier}
     * <p>
     * Complexity: O(1); the result is infinite and each element is forced when it is reached.
     *
     * @param nextSupplier a supplier which will provide values for extending a stream
     * @return new {@code Stream} composed from this stream extended with values provided by the supplier
     */
    default Stream<T> extend(Supplier<? extends T> nextSupplier) {
        Objects.requireNonNull(nextSupplier, "nextSupplier is null");
        return Stream.ofAll(appendAll(Stream.continually(nextSupplier)));
    }

    /**
     * Extends (continues) this {@code Stream} with a Stream of values created by applying
     * consecutively provided {@code Function} to the last element of the original Stream.
     * <p>
     * If this Stream is empty, it is returned unchanged (there is no last element to seed the
     * function); use {@link #extend(Object)} or {@link #extend(Supplier)} to extend an empty Stream.
     * <p>
     * Complexity: O(1); the result is infinite and each element is forced when it is reached.
     *
     * @param nextFunction a function which calculates the next value based on the previous value
     * @return new {@code Stream} composed from this stream extended with values calculated by the provided function
     */
    default Stream<T> extend(Function<? super T, ? extends T> nextFunction) {
        Objects.requireNonNull(nextFunction, "nextFunction is null");
        if (isEmpty()) {
            return this;
        } else {
            final Stream<T> that = this;
            return Stream.ofAll(new AbstractIterator<T>() {

                Stream<T> stream = that;
                @Nullable T last = null;

                @Override
                // `stream` is non-empty on entry, so `last` is always assigned before it is read.
                @SuppressWarnings("NullAway")
                protected T getNext() {
                    if (stream.isEmpty()) {
                        stream = Stream.iterate(nextFunction.apply(last), nextFunction);
                    }
                    last = stream.head();
                    stream = stream.tail();
                    return last;
                }

                @Override
                public boolean hasNext() {
                    return true;
                }
            });
        }
    }

    /**
     * The empty Stream.
     * <p>
     * This is a singleton, i.e. not Cloneable.
     *
     * @param <T> Component type of the Stream.
     */
    final class Empty<T extends @Nullable Object> implements Stream<T> {

        private static final Empty<?> INSTANCE = new Empty<>();

        // hidden
        private Empty() {
        }

        /**
         * Returns the singleton empty Stream instance.
         *
         * @param <T> Component type of the Stream
         * @return The empty Stream
         */
        @SuppressWarnings("unchecked")
        public static <T extends @Nullable Object> Empty<T> instance() {
            return (Empty<T>) INSTANCE;
        }

        @Override
        public T head() {
            throw new NoSuchElementException("head of empty stream");
        }

        @Override
        public boolean isEmpty() {
            return true;
        }

        @Override
        public java.util.Iterator<T> iterator() {
            return Iterator.empty();
        }

        @Override
        public Stream<T> tail() {
            throw new UnsupportedOperationException("tail of empty stream");
        }

        @Override
        public boolean equals(@Nullable Object o) {
            return com.guizmaii.zazr.collection.internal.Collections.equals(this, o);
        }

        @Override
        public int hashCode() {
            return com.guizmaii.zazr.collection.internal.Collections.hashOrdered(this);
        }

        @Override
        public String toString() {
            return "Stream()";
        }

    }

    /**
     * Non-empty {@code Stream}, consisting of a {@code head}, and {@code tail}.
     *
     * @param <T> Component type of the Stream.
     */
    abstract class Cons<T extends @Nullable Object> implements Stream<T> {

        final T head;
        final Lazy<Stream<T>> tail;

        Cons(T head, Supplier<Stream<T>> tail) {
            this(head, Lazy.of(Objects.requireNonNull(tail, "tail is null")));
        }

        // shares an already memoized tail instead of wrapping it in a second Lazy
        Cons(T head, Lazy<Stream<T>> tail) {
            this.head = head;
            this.tail = tail;
        }

        @Override
        public T head() {
            return head;
        }

        @Override
        public boolean isEmpty() {
            return false;
        }

        @Override
        public java.util.Iterator<T> iterator() {
            return new StreamIterator<>(this);
        }

        @Override
        public boolean equals(@Nullable Object o) {
            return com.guizmaii.zazr.collection.internal.Collections.equals(this, o);
        }

        @Override
        public int hashCode() {
            return com.guizmaii.zazr.collection.internal.Collections.hashOrdered(this);
        }

        @Override
        public String toString() {
            final StringBuilder builder = new StringBuilder("Stream(");
            Stream<T> stream = this;
            while (stream != null && !stream.isEmpty()) {
                final Cons<T> cons = (Cons<T>) stream;
                builder.append(cons.head);
                if (cons.tail.isEvaluated()) {
                    stream = stream.tail();
                    if (!stream.isEmpty()) {
                        builder.append(", ");
                    }
                } else {
                    builder.append(", ?");
                    stream = null;
                }
            }
            return builder.append(")").toString();
        }

        private static final class ConsImpl<T extends @Nullable Object> extends Cons<T> {

            ConsImpl(T head, Supplier<Stream<T>> tail) {
                super(head, tail);
            }

            @Override
            public Stream<T> tail() {
                return tail.get();
            }

        }

        private static final class AppendElements<T extends @Nullable Object> extends Cons<T> {

            private final com.guizmaii.zazr.collection.Queue<T> queue;

            AppendElements(T head, com.guizmaii.zazr.collection.Queue<T> queue, Supplier<Stream<T>> tail) {
                this(head, queue, Lazy.of(tail));
            }

            AppendElements(T head, com.guizmaii.zazr.collection.Queue<T> queue, Lazy<Stream<T>> tail) {
                super(head, tail);
                this.queue = queue;
            }

            @Override
            public Stream<T> append(T element) {
                return new AppendElements<>(head, queue.append(element), tail);
            }

            @Override
            public Stream<T> appendAll(Iterable<? extends T> elements) {
                Objects.requireNonNull(elements, "elements is null");
                return isEmpty() ? Stream.ofAll(queue) : new AppendElements<>(head, queue.appendAll(elements), tail);
            }

            @Override
            public Stream<T> tail() {
                final Stream<T> t = tail.get();
                if (t.isEmpty()) {
                    return Stream.ofAll(queue);
                } else {
                    if (t instanceof ConsImpl) {
                        final ConsImpl<T> c = (ConsImpl<T>) t;
                        return new AppendElements<>(c.head(), queue, c.tail);
                    } else {
                        final AppendElements<T> a = (AppendElements<T>) t;
                        return new AppendElements<>(a.head(), a.queue.appendAll(queue), a.tail);
                    }
                }
            }

        }
    }

    /**
     * The first element, already evaluated.
     * <p>
     * Complexity: O(1).
     *
     * @return the head of this Stream
     * @throws NoSuchElementException if this Stream is empty
     */
    T head();

    // -- windows and products

    /**
     * The elements in consecutive blocks of {@code size}: {@code Stream.of(1, 2, 3, 4, 5).grouped(2)} is
     * {@code Stream(Stream(1, 2), Stream(3, 4), Stream(5))}; the last block is smaller when {@code size} does not
     * divide the length. The same as {@code sliding(size, size)}.
     * <p>
     * Complexity: lazy; a block is built when the result reaches it and its elements are forced when the block is
     * consumed, so an infinite Stream can be grouped. Whether a further block exists is decided when the result's
     * tail is reached, which forces one element past the end of the block.
     *
     * @param size the block size, positive
     * @return the blocks, in order; empty if this Stream is empty
     * @throws IllegalArgumentException if {@code size} is not positive
     */
    default Stream<Stream<T>> grouped(int size) {
        return sliding(size, size);
    }

    /**
     * The windows of {@code size} consecutive elements, each starting one element after the previous:
     * {@code Stream.of(1, 2, 3, 4).sliding(3)} is {@code Stream(Stream(1, 2, 3), Stream(2, 3, 4))}. A Stream
     * shorter than {@code size} is one window. The same as {@code sliding(size, 1)}.
     * <p>
     * Complexity: lazy; a window is built when the result reaches it and its elements are forced when the window is
     * consumed, so an infinite Stream can be windowed. Whether a further window exists is decided when the result's
     * tail is reached, which forces up to {@code max(size, step) + 1} elements past the window's start.
     *
     * @param size the window size, positive
     * @return the windows, in order; empty if this Stream is empty
     * @throws IllegalArgumentException if {@code size} is not positive
     */
    default Stream<Stream<T>> sliding(int size) {
        return sliding(size, 1);
    }

    /**
     * The windows of {@code size} consecutive elements, each starting {@code step} elements after the previous:
     * {@code Stream.of(1, 2, 3, 4, 5).sliding(2, 3)} is {@code Stream(Stream(1, 2), Stream(4, 5))} and
     * {@code sliding(2, 4)} is {@code Stream(Stream(1, 2), Stream(5))}. The last window is shorter than
     * {@code size} when it reaches the end; a window whose elements all belong to the previous one is not
     * produced, so {@code Stream.of(1, 2, 3, 4).sliding(3)} has two windows. A Stream shorter than {@code size}
     * is one window; an empty Stream has none.
     * <p>
     * Complexity: lazy; a window is built when the result reaches it and its elements are forced when the window is
     * consumed, so an infinite Stream can be windowed. Whether a further window exists is decided when the result's
     * tail is reached, which forces up to {@code max(size, step) + 1} elements past the window's start.
     *
     * @param size the window size, positive
     * @param step the distance between two window starts, positive
     * @return the windows, in order
     * @throws IllegalArgumentException if {@code size} or {@code step} is not positive
     */
    default Stream<Stream<T>> sliding(int size, int step) {
        com.guizmaii.zazr.collection.internal.Collections.checkWindow(size, step);
        return isEmpty() ? empty() : Windows.apply(this, size, step);
    }

    /**
     * The elements in maximal runs of consecutive elements with the same key, computed once per element by
     * {@code classifier}: {@code Stream.of(1, 2, 3, 10, 12, 5, 7, 20, 29).slideBy(x -> x / 10)} is
     * {@code Stream(Stream(1, 2, 3), Stream(10, 12), Stream(5, 7), Stream(20, 29))}. The runs concatenate back
     * to this Stream.
     * <p>
     * Complexity: lazy; the first run is built now, each further run when the result reaches it; a run is forced
     * whole, up to the first element of the next one.
     *
     * @param classifier the key of an element; two consecutive elements are in the same run when their keys are
     *                   equal
     * @return the runs, in order; empty if this Stream is empty
     * @throws NullPointerException if {@code classifier} is null
     */
    default Stream<Stream<T>> slideBy(Function<? super T, ?> classifier) {
        Objects.requireNonNull(classifier, "classifier is null");
        return Stream.ofAll(Iterator.ofAll(this).slideBy(classifier).map(Stream::ofAll));
    }

    /**
     * The Cartesian square of this Stream: every pair {@code (a, b)} of elements, {@code a} varying slowest.
     * <p>
     * Complexity: lazy; O(n^2) pairs when consumed.
     *
     * @return the pairs
     */
    default Stream<Tuple2<T, T>> crossProduct() {
        return crossProduct(this);
    }

    /**
     * The Cartesian power of this Stream: every Stream of {@code power} elements drawn from this one, in
     * lexicographic position order. {@code power == 0} gives one empty Stream; a negative power gives no result.
     * <p>
     * Complexity: lazy; O(n^power) Streams of size {@code power} when consumed.
     *
     * @param power the size of each result
     * @return the Streams
     */
    default Stream<Stream<T>> crossProduct(int power) {
        if (power < 0) {
            return empty();
        }
        Stream<Stream<T>> product = Stream.of(Stream.<T> empty());
        for (int i = 0; i < power; i++) {
            product = product.flatMap(el -> map(el::append));
        }
        return product;
    }

    /**
     * The Cartesian product of this Stream and {@code that}: every pair {@code (a, b)} with {@code a} from this
     * Stream and {@code b} from {@code that}, {@code a} varying slowest. {@code that} is walked lazily and
     * memoised, so an infinite {@code that} works with {@code take}.
     * <p>
     * Complexity: lazy; O(n * m) pairs when consumed.
     *
     * @param that the right-hand elements
     * @param <U>  their type
     * @return the pairs
     * @throws NullPointerException if {@code that} is null
     */
    default <U extends @Nullable Object> Stream<Tuple2<T, U>> crossProduct(Iterable<? extends U> that) {
        Objects.requireNonNull(that, "that is null");
        // a lazy, memoising Stream: the result is lazy, so the argument stays lazy too
        final Stream<U> other = Stream.ofAll(that);
        return flatMap(a -> other.map(b -> Tuple.of(a, b)));
    }

    /**
     * Combines the elements from the right: the last with the one before it, the result with the one before that,
     * and so on.
     * <p>
     * Complexity: O(n); the whole Stream is forced and reversed.
     *
     * @param op combines the next element and the result so far
     * @return the combined result
     * @throws NoSuchElementException if this Stream is empty
     * @throws NullPointerException   if {@code op} is null
     */
    default T reduceRight(BiFunction<? super T, ? super T, ? extends T> op) {
        Objects.requireNonNull(op, "op is null");
        if (isEmpty()) {
            throw new NoSuchElementException("reduceRight on empty Stream");
        }
        return reverse().reduceLeft((xs, x) -> op.apply(x, xs));
    }

    /**
     * Whether exactly one element satisfies {@code predicate}.
     *
     * @param predicate the condition to test
     * @return {@code true} if one and only one element matches, {@code false} otherwise
     * @throws NullPointerException if {@code predicate} is null
     */
    default boolean existsUnique(Predicate<? super T> predicate) {
        return TraversableModule.existsUnique(this, predicate);
    }

    /**
     * The greatest element in the natural order of the elements, which must be {@link Comparable}; the sort order
     * of a sorted collection is not consulted. {@code NaN} compares as the greatest {@code Double} or {@code Float}.
     *
     * @return {@code Some(maximum)} if there is an element, {@code None} otherwise
     * @throws ClassCastException if two or more elements are not {@code Comparable}
     */
    default Option<T> max() {
        return TraversableModule.max(this);
    }

    /**
     * The greatest element according to {@code comparator}; of equal greatest elements, the first in this
     * Stream's order.
     *
     * @param comparator the order
     * @return {@code Some(maximum)} if there is an element, {@code None} otherwise
     * @throws NullPointerException if {@code comparator} is null
     */
    default Option<T> maxBy(Comparator<? super T> comparator) {
        return TraversableModule.maxBy(this, comparator);
    }

    /**
     * The element whose key, computed once by {@code f}, is the greatest; of equal greatest keys, the first
     * element in this Stream's order.
     *
     * @param f   the key of an element
     * @param <U> the key type
     * @return {@code Some(element)} if there is an element, {@code None} otherwise
     * @throws NullPointerException if {@code f} is null
     */
    default <U extends Comparable<? super U>> Option<T> maxBy(Function<? super T, ? extends U> f) {
        return TraversableModule.maxBy(this, f);
    }

    /**
     * The least element in the natural order of the elements, which must be {@link Comparable}; the sort order of
     * a sorted collection is not consulted. Among {@code Double}s or {@code Float}s, a {@code NaN} is the result
     * whenever one is present.
     *
     * @return {@code Some(minimum)} if there is an element, {@code None} otherwise
     * @throws ClassCastException if two or more elements are not {@code Comparable}
     */
    default Option<T> min() {
        return TraversableModule.min(this);
    }

    /**
     * The least element according to {@code comparator}; of equal least elements, the first in this Stream's
     * order.
     *
     * @param comparator the order
     * @return {@code Some(minimum)} if there is an element, {@code None} otherwise
     * @throws NullPointerException if {@code comparator} is null
     */
    default Option<T> minBy(Comparator<? super T> comparator) {
        return TraversableModule.minBy(this, comparator);
    }

    /**
     * The element whose key, computed once by {@code f}, is the least; of equal least keys, the first element in
     * this Stream's order.
     *
     * @param f   the key of an element
     * @param <U> the key type
     * @return {@code Some(element)} if there is an element, {@code None} otherwise
     * @throws NullPointerException if {@code f} is null
     */
    default <U extends Comparable<? super U>> Option<T> minBy(Function<? super T, ? extends U> f) {
        return TraversableModule.minBy(this, f);
    }

    /**
     * Folds the elements with {@code combine}, starting from {@code zero}, which must be its neutral element.
     * The elements are combined from the left, so {@code combine} need not be associative.
     *
     * @param zero    the neutral element of {@code combine}
     * @param combine combines two elements
     * @return the folded result, {@code zero} on an empty Stream
     * @throws NullPointerException if {@code combine} is null
     */
    default T fold(T zero, BiFunction<? super T, ? super T, ? extends T> combine) {
        Objects.requireNonNull(combine, "combine is null");
        return foldLeft(zero, combine);
    }

    /**
     * Combines the elements with {@code op}, each result with the next element. The same as {@link #reduceLeft(BiFunction)}.
     *
     * @param op combines two elements
     * @return the combined result
     * @throws NoSuchElementException if this Stream is empty
     * @throws NullPointerException   if {@code op} is null
     */
    default T reduce(BiFunction<? super T, ? super T, ? extends T> op) {
        return TraversableModule.reduceLeft(this, op);
    }

    /**
     * {@link #reduce(BiFunction)} as an {@code Option}: {@code None} on an empty Stream.
     *
     * @param op combines two elements
     * @return {@code Some(result)}, or {@code None} if this Stream is empty
     * @throws NullPointerException if {@code op} is null
     */
    default Option<T> reduceOption(BiFunction<? super T, ? super T, ? extends T> op) {
        return TraversableModule.reduceLeftOption(this, op);
    }

    /**
     * The only element.
     *
     * @return the element
     * @throws NoSuchElementException if this Stream is empty or has more than one element
     */
    default T single() {
        return TraversableModule.single(this);
    }

    /**
     * The only element as an {@code Option}.
     *
     * @return {@code Some(element)} if there is exactly one element, {@code None} otherwise
     */
    default Option<T> singleOption() {
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
    default <K extends @Nullable Object> Option<Map<K, T>> arrangeBy(Function<? super T, ? extends K> getKey) {
        Objects.requireNonNull(getKey, "getKey is null");
        return TraversableModule.arrangeBy(groupBy(getKey));
    }

    /**
     * The sum of the elements, which must be {@link Number}s: {@code Byte}, {@code Short}, {@code Integer} and
     * {@code Long} are summed as a {@code long}, {@code BigInteger} and {@code BigDecimal} with their own
     * arithmetic, any other {@code Number} as a {@code double} with Neumaier compensation. The arithmetic is chosen
     * from the first element. {@code 0} on an empty Stream.
     *
     * @return the sum
     * @throws UnsupportedOperationException if an element is not a {@code Number}
     */
    default Number sum() {
        return TraversableModule.sum(this);
    }

    /**
     * The product of the elements, which must be {@link Number}s: {@code Byte}, {@code Short}, {@code Integer} and
     * {@code Long} are multiplied as a {@code long}, {@code BigInteger} and {@code BigDecimal} with their own
     * arithmetic, any other {@code Number} as a {@code double}. The arithmetic is chosen from the first element.
     * {@code 1} on an empty Stream.
     *
     * @return the product
     * @throws UnsupportedOperationException if an element is not a {@code Number}
     */
    default Number product() {
        return TraversableModule.product(this);
    }

    /**
     * The average of the elements, which must be {@link Number}s, summed as {@code double}s with Neumaier
     * compensation.
     *
     * @return {@code Some(average)} if there is an element, {@code None} otherwise
     * @throws UnsupportedOperationException if an element is not a {@code Number}
     */
    default Option<Double> average() {
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
    default Option<T> findLast(Predicate<? super T> predicate) {
        return TraversableModule.findLast(this, predicate);
    }

    /**
     * Runs {@code action} on each element with its position, from {@code 0}, without boxing the index.
     *
     * @param action what to do with each element and its index
     * @throws NullPointerException if {@code action} is null
     */
    default void forEachWithIndex(ObjIntConsumer<? super T> action) {
        TraversableModule.forEachWithIndex(this, action);
    }

    /**
     * The first element as an {@code Option}.
     * <p>
     * Complexity: that of {@link #head()}.
     *
     * @return {@code Some(head)}, or {@code None} if this Stream is empty
     */
    default Option<T> headOption() {
        return isEmpty() ? Option.none() : Option.some(head());
    }

    /**
     * The last element as an {@code Option}.
     * <p>
     * Complexity: that of {@link #last()}.
     *
     * @return {@code Some(last)}, or {@code None} if this Stream is empty
     */
    default Option<T> lastOption() {
        return isEmpty() ? Option.none() : Option.some(last());
    }

    /**
     * Combines the elements from the left: the first with the second, the result with the third, and so on.
     * <p>
     * Complexity: O(n).
     *
     * @param op combines the result so far and the next element
     * @return the combined result
     * @throws NoSuchElementException if this Stream is empty
     * @throws NullPointerException   if {@code op} is null
     */
    default T reduceLeft(BiFunction<? super T, ? super T, ? extends T> op) {
        return TraversableModule.reduceLeft(this, op);
    }

    /**
     * {@link #reduceLeft(BiFunction)} as an {@code Option}: {@code None} on an empty Stream.
     *
     * @param op combines the result so far and the next element
     * @return {@code Some(result)}, or {@code None} if this Stream is empty
     * @throws NullPointerException if {@code op} is null
     */
    default Option<T> reduceLeftOption(BiFunction<? super T, ? super T, ? extends T> op) {
        return TraversableModule.reduceLeftOption(this, op);
    }

    /**
     * {@link #reduceRight(BiFunction)} as an {@code Option}: {@code None} on an empty Stream.
     *
     * @param op combines the next element and the result so far
     * @return {@code Some(result)}, or {@code None} if this Stream is empty
     * @throws NullPointerException if {@code op} is null
     */
    default Option<T> reduceRightOption(BiFunction<? super T, ? super T, ? extends T> op) {
        Objects.requireNonNull(op, "op is null");
        return isEmpty() ? Option.none() : Option.some(reduceRight(op));
    }

    /**
     * The number of elements; the same as {@link #length()}.
     * <p>
     * Complexity: that of {@link #length()}.
     *
     * @return the number of elements
     */
    @Override
    default int size() {
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
    default <R extends @Nullable Object, A extends @Nullable Object> R collect(Collector<? super T, A, R> collector) {
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
    default <R extends @Nullable Object> R collect(Supplier<R> supplier, BiConsumer<R, ? super T> accumulator, BiConsumer<R, R> combiner) {
        return stream().collect(supplier, accumulator, combiner);
    }

    /**
     * The elements copied into a new mutable {@link java.util.Collection} that {@code factory} makes for the given
     * capacity, in this Stream's order: {@code toJavaCollection(java.util.LinkedHashSet::new)}.
     *
     * @param factory makes an empty mutable collection with the given initial capacity
     * @param <C>     the collection type
     * @return the new collection, filled
     * @throws NullPointerException if {@code factory} is null
     */
    default <C extends java.util.Collection<T>> C toJavaCollection(Function<Integer, C> factory) {
        return TraversableModule.toJavaCollection(this, factory);
    }

    /**
     * The elements copied into a new {@link java.util.ArrayList}, in this Stream's order.
     *
     * @return the new list
     */
    default java.util.List<T> toJavaList() {
        return TraversableModule.toJavaCollection(this, ArrayList::new, 10);
    }

    /**
     * The elements copied into a new mutable {@link java.util.List} that {@code factory} makes for the given
     * capacity, in this Stream's order: {@code toJavaList(capacity -> new java.util.LinkedList<>())}.
     *
     * @param factory makes an empty mutable list with the given initial capacity
     * @param <LIST>  the list type
     * @return the new list, filled
     * @throws NullPointerException if {@code factory} is null
     */
    default <LIST extends java.util.List<T>> LIST toJavaList(Function<Integer, LIST> factory) {
        return TraversableModule.toJavaCollection(this, factory);
    }

    /**
     * The elements as the entries of a new {@link java.util.HashMap}, each mapped to a key and a value by
     * {@code f}; of two entries with the same key, the later one in this Stream's order wins.
     *
     * @param f   the entry an element becomes
     * @param <K> the key type
     * @param <V> the value type
     * @return the new map
     * @throws NullPointerException if {@code f} is null
     */
    default <K extends @Nullable Object, V extends @Nullable Object> java.util.Map<K, V> toJavaMap(Function<? super T, ? extends Tuple2<? extends K, ? extends V>> f) {
        return TraversableModule.toJavaMap(this, java.util.HashMap::new, f);
    }

    /**
     * The elements as the entries of a new mutable {@link java.util.Map} that {@code factory} makes, each mapped
     * to a key by {@code keyMapper} and to a value by {@code valueMapper}; of two entries with the same key, the
     * later one in this Stream's order wins.
     *
     * @param factory     makes an empty mutable map
     * @param keyMapper   the key of an element
     * @param valueMapper the value of an element
     * @param <K>         the key type
     * @param <V>         the value type
     * @param <MAP>       the map type
     * @return the new map, filled
     * @throws NullPointerException if an argument is null
     */
    default <K extends @Nullable Object, V extends @Nullable Object, MAP extends java.util.Map<K, V>> MAP toJavaMap(Supplier<MAP> factory, Function<? super T, ? extends K> keyMapper, Function<? super T, ? extends V> valueMapper) {
        return TraversableModule.toJavaMap(this, factory, TraversableModule.entryMapper(keyMapper, valueMapper));
    }

    /**
     * The elements as the entries of a new mutable {@link java.util.Map} that {@code factory} makes, each mapped
     * to a key and a value by {@code f}; of two entries with the same key, the later one in this Stream's order
     * wins.
     *
     * @param factory makes an empty mutable map
     * @param f       the entry an element becomes
     * @param <K>     the key type
     * @param <V>     the value type
     * @param <MAP>   the map type
     * @return the new map, filled
     * @throws NullPointerException if an argument is null
     */
    default <K extends @Nullable Object, V extends @Nullable Object, MAP extends java.util.Map<K, V>> MAP toJavaMap(Supplier<MAP> factory, Function<? super T, ? extends Tuple2<? extends K, ? extends V>> f) {
        return TraversableModule.toJavaMap(this, factory, f);
    }

    /**
     * The distinct elements copied into a new {@link java.util.HashSet}.
     *
     * @return the new set
     */
    default java.util.Set<T> toJavaSet() {
        return TraversableModule.toJavaCollection(this, java.util.HashSet::new, 16);
    }

    /**
     * The elements copied into a new mutable {@link java.util.Set} that {@code factory} makes for the given
     * capacity: {@code toJavaSet(capacity -> new java.util.TreeSet<>(Comparator.reverseOrder()))}.
     *
     * @param factory makes an empty mutable set with the given initial capacity
     * @param <SET>   the set type
     * @return the new set, filled
     * @throws NullPointerException if {@code factory} is null
     */
    default <SET extends java.util.Set<T>> SET toJavaSet(Function<Integer, SET> factory) {
        return TraversableModule.toJavaCollection(this, factory);
    }

    /**
     * A parallel {@link java.util.stream.Stream} over the elements, built on {@link #spliterator()}.
     *
     * @return a new parallel {@code java.util.stream.Stream}
     */
    default java.util.stream.Stream<T> toJavaParallelStream() {
        return StreamSupport.stream(spliterator(), true);
    }

    /**
     * The elements as the entries of a new {@link HashMap}, each mapped to a key by {@code keyMapper} and to a
     * value by {@code valueMapper}; of two entries with the same key, the later one in this Stream's order wins.
     *
     * @param keyMapper   the key of an element
     * @param valueMapper the value of an element
     * @param <K>         the key type
     * @param <V>         the value type
     * @return the new map
     * @throws NullPointerException if an argument is null
     */
    default <K extends @Nullable Object, V extends @Nullable Object> Map<K, V> toMap(Function<? super T, ? extends K> keyMapper, Function<? super T, ? extends V> valueMapper) {
        return toMap(TraversableModule.entryMapper(keyMapper, valueMapper));
    }

    /**
     * The elements as the entries of a new {@link HashMap}, each mapped to a key and a value by {@code f}; of two
     * entries with the same key, the later one in this Stream's order wins.
     *
     * @param f   the entry an element becomes
     * @param <K> the key type
     * @param <V> the value type
     * @return the new map
     * @throws NullPointerException if {@code f} is null
     */
    default <K extends @Nullable Object, V extends @Nullable Object> Map<K, V> toMap(Function<? super T, ? extends Tuple2<? extends K, ? extends V>> f) {
        final Function<Iterable<Tuple2<? extends K, ? extends V>>, Map<K, V>> ofAll = HashMap::ofEntries;
        return TraversableModule.toMap(this, HashMap.empty(), ofAll, f);
    }

    /**
     * The elements as the entries of a new {@link LinkedHashMap}, in this Stream's order, each mapped to a key by
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
    default <K extends @Nullable Object, V extends @Nullable Object> Map<K, V> toLinkedMap(Function<? super T, ? extends K> keyMapper, Function<? super T, ? extends V> valueMapper) {
        return toLinkedMap(TraversableModule.entryMapper(keyMapper, valueMapper));
    }

    /**
     * The elements as the entries of a new {@link LinkedHashMap}, in this Stream's order, each mapped to a key
     * and a value by {@code f}; of two entries with the same key, the later one wins the value and the earlier one
     * the position.
     *
     * @param f   the entry an element becomes
     * @param <K> the key type
     * @param <V> the value type
     * @return the new map
     * @throws NullPointerException if {@code f} is null
     */
    default <K extends @Nullable Object, V extends @Nullable Object> Map<K, V> toLinkedMap(Function<? super T, ? extends Tuple2<? extends K, ? extends V>> f) {
        final Function<Iterable<Tuple2<? extends K, ? extends V>>, Map<K, V>> ofAll = LinkedHashMap::ofEntries;
        return TraversableModule.toMap(this, LinkedHashMap.empty(), ofAll, f);
    }

    /**
     * The elements as the entries of a new {@link TreeMap} in the natural order of the keys, each mapped to a key
     * by {@code keyMapper} and to a value by {@code valueMapper}; of two entries with the same key, the later one
     * in this Stream's order wins.
     *
     * @param keyMapper   the key of an element
     * @param valueMapper the value of an element
     * @param <K>         the key type
     * @param <V>         the value type
     * @return the new map
     * @throws NullPointerException if an argument is null
     */
    default <K extends Comparable<? super K>, V extends @Nullable Object> SortedMap<K, V> toSortedMap(Function<? super T, ? extends K> keyMapper, Function<? super T, ? extends V> valueMapper) {
        return toSortedMap(TraversableModule.entryMapper(keyMapper, valueMapper));
    }

    /**
     * The elements as the entries of a new {@link TreeMap} in the natural order of the keys, each mapped to a key
     * and a value by {@code f}; of two entries with the same key, the later one in this Stream's order wins.
     *
     * @param f   the entry an element becomes
     * @param <K> the key type
     * @param <V> the value type
     * @return the new map
     * @throws NullPointerException if {@code f} is null
     */
    default <K extends Comparable<? super K>, V extends @Nullable Object> SortedMap<K, V> toSortedMap(Function<? super T, ? extends Tuple2<? extends K, ? extends V>> f) {
        Objects.requireNonNull(f, "f is null");
        return toSortedMap(Comparator.naturalOrder(), f);
    }

    /**
     * The elements as the entries of a new {@link TreeMap} ordered by {@code comparator}, each mapped to a key by
     * {@code keyMapper} and to a value by {@code valueMapper}; of two entries with the same key, the later one in
     * this Stream's order wins.
     *
     * @param comparator  the order of the keys
     * @param keyMapper   the key of an element
     * @param valueMapper the value of an element
     * @param <K>         the key type
     * @param <V>         the value type
     * @return the new map
     * @throws NullPointerException if an argument is null
     */
    default <K extends @Nullable Object, V extends @Nullable Object> SortedMap<K, V> toSortedMap(Comparator<? super K> comparator, Function<? super T, ? extends K> keyMapper, Function<? super T, ? extends V> valueMapper) {
        return toSortedMap(comparator, TraversableModule.entryMapper(keyMapper, valueMapper));
    }

    /**
     * The elements as the entries of a new {@link TreeMap} ordered by {@code comparator}, each mapped to a key and
     * a value by {@code f}; of two entries with the same key, the later one in this Stream's order wins.
     *
     * @param comparator the order of the keys
     * @param f          the entry an element becomes
     * @param <K>        the key type
     * @param <V>        the value type
     * @return the new map
     * @throws NullPointerException if an argument is null
     */
    default <K extends @Nullable Object, V extends @Nullable Object> SortedMap<K, V> toSortedMap(Comparator<? super K> comparator, Function<? super T, ? extends Tuple2<? extends K, ? extends V>> f) {
        Objects.requireNonNull(comparator, "comparator is null");
        final Function<Iterable<Tuple2<? extends K, ? extends V>>, SortedMap<K, V>> ofAll = t -> TreeMap.ofEntries(comparator, t);
        return TraversableModule.toMap(this, TreeMap.empty(comparator), ofAll, f);
    }

    /**
     * The elements as a {@link Queue}, in this Stream's order.
     *
     * @return a {@code Queue} of the elements
     */
    default Queue<T> toQueue() {
        return TraversableModule.toTraversable(this, Queue.empty(), Queue::ofAll);
    }

    /**
     * The distinct elements as a {@link LinkedHashSet}, in this Stream's order.
     *
     * @return a {@code LinkedHashSet} of the elements
     */
    default Set<T> toLinkedSet() {
        return TraversableModule.toTraversable(this, LinkedHashSet.empty(), LinkedHashSet::ofAll);
    }

    /**
     * The distinct elements as a {@link TreeSet} in their natural order; a {@code TreeSet} returns itself.
     *
     * @return a {@code TreeSet} of the elements
     * @throws ClassCastException if the elements are not {@link Comparable}
     */
    default SortedSet<T> toSortedSet() {
        return TraversableModule.toSortedSet(this);
    }

    /**
     * The distinct elements as a {@link TreeSet} ordered by {@code comparator}.
     *
     * @param comparator the order
     * @return a {@code TreeSet} of the elements
     * @throws NullPointerException if {@code comparator} is null
     */
    default SortedSet<T> toSortedSet(Comparator<? super T> comparator) {
        Objects.requireNonNull(comparator, "comparator is null");
        return TraversableModule.toTraversable(this, TreeSet.empty(comparator), values -> TreeSet.ofAll(comparator, values));
    }

    /**
     * The elements as a {@link Stream}, in this Stream's order.
     *
     * @return a {@code Stream} of the elements
     */
    default Stream<T> toStream() {
        return TraversableModule.toTraversable(this, Stream.empty(), Stream::ofAll);
    }

}
