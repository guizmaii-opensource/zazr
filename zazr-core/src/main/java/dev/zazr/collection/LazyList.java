package dev.zazr.collection;

import dev.zazr.*;
import dev.zazr.collection.LazyList.Cons;
import dev.zazr.collection.LazyList.Empty;
import dev.zazr.collection.internal.AbstractIterator;
import dev.zazr.collection.internal.Collections;
import dev.zazr.collection.internal.Iterator;
import dev.zazr.collection.internal.JavaConverters;
import dev.zazr.collection.internal.LazyListModule;
import dev.zazr.collection.internal.LazyListModule.*;
import dev.zazr.collection.internal.TraversableModule;
import dev.zazr.control.Either;
import dev.zazr.control.Option;
import java.io.*;
import java.util.*;
import java.util.function.*;
import java.util.stream.Collector;
import org.jspecify.annotations.Nullable;


/**
 * An immutable {@code LazyList} is lazy sequence of elements which may be infinitely long.
 * Its immutability makes it suitable for concurrent programming.
 * <p>
 * A {@code LazyList} is composed of a {@code head} element and a lazy evaluated {@code tail} {@code LazyList}.
 * <p>
 * There are two implementations of the {@code LazyList} interface:
 *
 * <ul>
 * <li>{@link Empty}, which represents the empty {@code LazyList}.</li>
 * <li>{@link Cons}, which represents a {@code LazyList} containing one or more elements.</li>
 * </ul>
 *
 * Methods to obtain a {@code LazyList}:
 *
 * <pre>
 * {@code
 * // factory methods
 * LazyList.empty()                  // = LazyList.of() = Empty.instance()
 * LazyList.of(x)                    // = LazyList.cons(x, LazyList::empty)
 * LazyList.of(Object...)            // e.g. LazyList.of(1, 2, 3)
 * LazyList.ofAll(Iterable)          // e.g. LazyList.ofAll(List.of(1, 2, 3)) = 1, 2, 3
 * LazyList.ofAll(<primitive array>) // e.g. LazyList.ofAll(1, 2, 3) = 1, 2, 3
 *
 * // int sequences
 * LazyList.from(0)                  // = 0, 1, 2, 3, ...
 * LazyList.range(0, 3)              // = 0, 1, 2
 * LazyList.rangeClosed(0, 3)        // = 0, 1, 2, 3
 *
 * // generators
 * LazyList.cons(Object, Supplier)   // e.g. LazyList.cons(current, () -> next(current));
 * LazyList.continually(Supplier)    // e.g. LazyList.continually(Math::random);
 * LazyList.iterate(Object, Function)// e.g. LazyList.iterate(1, i -> i * 2);
 * }
 * </pre>
 *
 * Factory method applications:
 *
 * <pre>
 * {@code
 * LazyList<Integer>       s1 = LazyList.of(1);
 * LazyList<Integer>       s2 = LazyList.of(1, 2, 3);
 *                       // = LazyList.of(new Integer[] {1, 2, 3});
 *
 * LazyList<int[]>         s3 = LazyList.of(new int[] {1, 2, 3});
 * LazyList<List<Integer>> s4 = LazyList.of(List.of(1, 2, 3));
 *
 * LazyList<Integer>       s5 = LazyList.ofAll(1, 2, 3);
 * LazyList<Integer>       s6 = LazyList.ofAll(List.of(1, 2, 3));
 *
 * // cuckoo's egg
 * LazyList<Integer[]>     s7 = LazyList.<Integer[]> of(new Integer[] {1, 2, 3});
 * }
 * </pre>
 *
 * Example: Generating prime numbers
 *
 * <pre>
 * {@code
 * // = LazyList(2L, 3L, 5L, 7L, ...)
 * LazyList.iterate(2L, PrimeNumbers::nextPrimeFrom)
 *
 * // helpers
 *
 * static long nextPrimeFrom(long num) {
 *     return LazyList.from(num + 1).find(PrimeNumbers::isPrime).get();
 * }
 *
 * static boolean isPrime(long num) {
 *     return !LazyList.rangeClosed(2L, (long) Math.sqrt(num)).exists(d -> num % d == 0);
 * }
 * }
 * </pre>
 *
 * See Okasaki, Chris: <em>Purely Functional Data Structures</em> (p. 34 ff.). Cambridge, 2003.
 * <p>
 * Complexity: a lazy call computes now only what its note says, and each further element when the result reaches it.
 * The methods without a note of their own that read every element (the folds, {@code reduce}, {@code count},
 * {@code sum}, {@code mkString}, {@code forEach}, the conversions to other collections, {@code equals} and
 * {@code hashCode}) are O(n) and never return on an infinite LazyList; {@code exists}, {@code forAll}, {@code find} and
 * {@code contains} stop at the first element that decides, {@code existsUnique} at the second match, and each
 * {@code ...Option} variant costs what the method it wraps costs. {@code toString} shows only the elements already
 * computed.
 *
 * @param <T> component type of this LazyList
 * @author Daniel Dietrich, Jörgen Andersson, Ruslan Sennov
 */
public interface LazyList<T extends @Nullable Object> extends Traversable<T> {

    /**
     * Returns a {@link java.util.stream.Collector} which may be used in conjunction with
     * {@link java.util.stream.Stream#collect(java.util.stream.Collector)} to obtain a {@link LazyList}.
     *
     * @param <T> Component type of the LazyList.
     * @return A dev.zazr.collection.LazyList Collector.
     */
    static <T extends @Nullable Object> Collector<T, ArrayList<T>, LazyList<T>> collector() {
        final Supplier<ArrayList<T>> supplier = ArrayList::new;
        final BiConsumer<ArrayList<T>, T> accumulator = ArrayList::add;
        final BinaryOperator<ArrayList<T>> combiner = (left, right) -> {
            left.addAll(right);
            return left;
        };
        final Function<ArrayList<T>, LazyList<T>> finisher = LazyList::ofAll;
        return Collector.of(supplier, accumulator, combiner, finisher);
    }

    /**
     * Creates a LazyList which traverses along the concatenation of the given iterables.
     * <p>
     * Building the LazyList is O(k) in the number of given iterables, since an iterator is eagerly
     * obtained from every one of them up front; only the traversal of the elements is lazy.
     * <p>
     * Complexity: O(k) for k iterables: their iterators are taken, and the first element is computed now (past any
     * empty iterables before it); the others when the result reaches them.
     *
     * @param iterables The iterables
     * @param <T>       Component type.
     * @return A new {@code LazyList}
     */
    @SuppressWarnings("varargs")
    @SafeVarargs
    static <T extends @Nullable Object> LazyList<T> concat(Iterable<? extends T> ... iterables) {
        return Iterator.concat(iterables).toLazyList();
    }

    /**
     * Creates a LazyList which traverses along the concatenation of the given iterables.
     * <p>
     * The outer iterable is fully traversed and an iterator is eagerly obtained from every element
     * up front, so it must be finite (an infinite outer iterable causes this call to never return);
     * only the traversal of the resulting elements is lazy.
     * <p>
     * Complexity: O(k) for k iterables: the outer iterable is read whole, so an infinite one never returns; each
     * iterator is taken and the first element computed now, the others when the result reaches them.
     *
     * @param iterables The iterable of iterables
     * @param <T>       Component type.
     * @return A new {@code LazyList}
     */
    static <T extends @Nullable Object> LazyList<T> concat(Iterable<? extends Iterable<? extends T>> iterables) {
        return Iterator.concat(iterables).toLazyList();
    }

    /**
     * Concatenates nested iterables into one lazy LazyList. Static, like every {@code flatten} in Zazr, because Java
     * cannot demand of an instance method that the receiver's element type be a collection. Unlike
     * {@link #concat(Iterable)}, the outer iterable is read lazily too: an inner iterable is opened only when the
     * result reaches it, so an infinite outer iterable, or an infinite inner one, is accepted. The outer iterable and
     * each inner one are iterated once, so one-shot iterables are accepted.
     * <p>
     * Complexity: lazy; the first element is found now, past the empty inner iterables before it, and each further one
     * when the result reaches it. An outer iterable with infinitely many empty inner ones and no element after them
     * never returns.
     *
     * @param nested Iterables of elements
     * @param <T>    Component type of the inner iterables
     * @return the inner elements, in order
     * @throws NullPointerException if {@code nested} is null, or when the result reaches a null inner iterable or a
     *                              null element
     */
    static <T extends @Nullable Object> LazyList<T> flatten(Iterable<? extends Iterable<? extends T>> nested) {
        Objects.requireNonNull(nested, "nested is null");
        return LazyListFactory.create(new FlatMapIterator<>(Iterator.ofAll(nested), Function.identity()));
    }

    /**
     * Returns an infinitely long LazyList of {@code int} values starting from {@code value}.
     * <p>
     * The {@code LazyList} extends to {@code Integer.MIN_VALUE} when passing {@code Integer.MAX_VALUE}.
     *
     * @param value a start int value
     * @return a new LazyList of int values starting from {@code value}
     */
    static LazyList<Integer> from(int value) {
        return LazyList.ofAll(Iterator.from(value));
    }

    /**
     * Returns an infinite long LazyList of {@code int} values starting from {@code value} and spaced by {@code step}.
     * <p>
     * The {@code LazyList} extends to {@code Integer.MIN_VALUE} when passing {@code Integer.MAX_VALUE}.
     *
     * @param value a start int value
     * @param step  the step by which to advance on each next value
     * @return a new {@code LazyList} of int values starting from {@code value}
     */
    static LazyList<Integer> from(int value, int step) {
        return LazyList.ofAll(Iterator.from(value, step));
    }

    /**
     * Returns an infinitely long LazyList of {@code long} values starting from {@code value}.
     * <p>
     * The {@code LazyList} extends to {@code Long.MIN_VALUE} when passing {@code Long.MAX_VALUE}.
     *
     * @param value a start long value
     * @return a new LazyList of long values starting from {@code value}
     */
    static LazyList<Long> from(long value) {
        return LazyList.ofAll(Iterator.from(value));
    }

    /**
     * Returns an infinite long LazyList of {@code long} values starting from {@code value} and spaced by {@code step}.
     * <p>
     * The {@code LazyList} extends to {@code Long.MIN_VALUE} when passing {@code Long.MAX_VALUE}.
     *
     * @param value a start long value
     * @param step  the step by which to advance on each next value
     * @return a new {@code LazyList} of long values starting from {@code value}
     */
    static LazyList<Long> from(long value, long step) {
        return LazyList.ofAll(Iterator.from(value, step));
    }

    /**
     * Generates a (theoretically) infinitely long LazyList using a value Supplier.
     *
     * @param supplier A Supplier of LazyList values
     * @param <T>      value type
     * @return A new LazyList
     */
    static <T extends @Nullable Object> LazyList<T> continually(Supplier<? extends T> supplier) {
        Objects.requireNonNull(supplier, "supplier is null");
        return LazyList.ofAll(Iterator.continually(supplier));
    }

    /**
     * Generates a (theoretically) infinitely long LazyList using a function to calculate the next value
     * based on the previous.
     *
     * @param seed The first value in the LazyList
     * @param f    A function to calculate the next value based on the previous
     * @param <T>  value type
     * @return A new LazyList
     */
    static <T extends @Nullable Object> LazyList<T> iterate(T seed, Function<? super T, ? extends T> f) {
        Objects.requireNonNull(f, "f is null");
        return LazyList.ofAll(Iterator.iterate(seed, f));
    }

    /**
     * Generates a (theoretically) infinitely long LazyList using a repeatedly invoked supplier
     * that provides a {@code Some} for each next value and a {@code None} for the end.
     * The {@code Supplier} will be invoked only that many times until it returns {@code None},
     * and repeated iteration over the lazy list will produce the same values in the same order,
     * without any further invocations to the {@code Supplier}.
     *
     * @param supplier A Supplier of iterator values
     * @param <T> value type
     * @return A new LazyList
     * @throws NullPointerException if {@code supplier} is null, or, when the lazy list reaches it, returns null
     */
    static <T extends @Nullable Object> LazyList<T> iterate(Supplier<? extends Option<? extends T>> supplier) {
        Objects.requireNonNull(supplier, "supplier is null");
        return LazyList.ofAll(Iterator.iterate(supplier, "LazyList.iterate: supplier returned null"));
    }

    /**
     * Constructs a LazyList of a head element and a tail supplier.
     *
     * @param head         The head element of the LazyList
     * @param tailSupplier A supplier of the tail values. To end the lazy list, return {@link LazyList#empty}.
     * @param <T>          value type
     * @return A new LazyList
     * @throws NullPointerException if {@code head} or {@code tailSupplier} is null; {@code tail()} throws it when
     *                              {@code tailSupplier} returns null
     */
    @SuppressWarnings("unchecked")
    static <T extends @Nullable Object> LazyList<T> cons(T head, Supplier<? extends LazyList<? extends T>> tailSupplier) {
        Objects.requireNonNull(head, "LazyList: element is null");
        Objects.requireNonNull(tailSupplier, "tailSupplier is null");
        return new Cons.ConsImpl<>(head, (Supplier<LazyList<T>>) tailSupplier);
    }

    /**
     * Returns the single instance of Empty. Convenience method for {@code Empty.instance()}.
     * <p>
     * Note: this method intentionally returns type {@code LazyList} and not {@code Empty}. This comes in handy when folding.
     * If you explicitly need type {@code Empty} use {@linkplain Empty#instance()}.
     *
     * @param <T> Component type of Empty, determined by type inference in the particular context.
     * @return The empty list.
     */
    static <T extends @Nullable Object> LazyList<T> empty() {
        return Empty.instance();
    }

    /**
     * Narrows a widened {@code LazyList<? extends T>} to {@code LazyList<T>}
     * by performing a type-safe cast. This is eligible because immutable/read-only
     * collections are covariant.
     *
     * @param stream A {@code LazyList}.
     * @param <T>    Component type of the {@code LazyList}.
     * @return the given {@code stream} instance as narrowed type {@code LazyList<T>}.
     */
    @SuppressWarnings("unchecked")
    static <T extends @Nullable Object> LazyList<T> narrow(LazyList<? extends T> stream) {
        return (LazyList<T>) stream;
    }

    /**
     * Returns a singleton {@code LazyList}, i.e. a {@code LazyList} of one element.
     *
     * @param element An element.
     * @param <T>     The component type
     * @return A new LazyList instance containing the given element
     */
    static <T extends @Nullable Object> LazyList<T> of(T element) {
        return cons(element, Empty::instance);
    }

    /**
     * Creates a LazyList of the given elements.
     *
     * <pre>{@code  LazyList.of(1, 2, 3, 4)
     * = Empty.instance().prepend(4).prepend(3).prepend(2).prepend(1)
     * = LazyList.cons(1, () -> LazyList.cons(2, () -> LazyList.cons(3, () -> LazyList.cons(4, LazyList::empty))))}</pre>
     *
     * @param <T>      Component type of the LazyList.
     * @param elements Zero or more elements.
     * @return A list containing the given elements in the same order.
     */
    @SafeVarargs
    static <T extends @Nullable Object> LazyList<T> of(T ... elements) {
        Objects.requireNonNull(elements, "elements is null");
        for (T element : elements) {
            Objects.requireNonNull(element, "LazyList.of: element is null");
        }
        return LazyList.ofAll(new Iterator<T>() {
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
     * Returns a LazyList containing {@code n} values of a given Function {@code f}
     * over a range of integer values from 0 to {@code n - 1}.
     *
     * @param <T> Component type of the LazyList
     * @param n   The number of elements in the LazyList
     * @param f   The Function computing element values
     * @return A LazyList consisting of elements {@code f(0),f(1), ..., f(n - 1)}
     * @throws NullPointerException if {@code f} is null
     */
    static <T extends @Nullable Object> LazyList<T> tabulate(int n, Function<? super Integer, ? extends T> f) {
        Objects.requireNonNull(f, "f is null");
        return LazyList.ofAll(dev.zazr.collection.internal.Collections.tabulate(n, f));
    }

    /**
     * Returns a LazyList containing {@code n} values supplied by a given Supplier {@code s}.
     *
     * @param <T> Component type of the LazyList
     * @param n   The number of elements in the LazyList
     * @param s   The Supplier computing element values
     * @return A LazyList of size {@code n}, where each element contains the result supplied by {@code s}.
     * @throws NullPointerException if {@code s} is null
     */
    static <T extends @Nullable Object> LazyList<T> fill(int n, Supplier<? extends T> s) {
        Objects.requireNonNull(s, "s is null");
        return LazyList.ofAll(dev.zazr.collection.internal.Collections.fill(n, s));
    }

    /**
     * Returns a LazyList containing {@code n} times the given {@code element}
     *
     * @param <T>     Component type of the LazyList
     * @param n       The number of elements in the LazyList
     * @param element The element
     * @return A LazyList of size {@code n}, where each element is the given {@code element}.
     */
    static <T extends @Nullable Object> LazyList<T> fill(int n, T element) {
        return LazyList.ofAll(dev.zazr.collection.internal.Collections.fillObject(n, element));
    }

    /**
     * Creates a LazyList of the given elements.
     *
     * @param <T>      Component type of the LazyList.
     * @param elements An Iterable of elements.
     * @return A LazyList containing the given elements in the same order.
     */
    @SuppressWarnings("unchecked")
    static <T extends @Nullable Object> LazyList<T> ofAll(Iterable<? extends T> elements) {
        Objects.requireNonNull(elements, "elements is null");
        if (elements instanceof LazyList) {
            return (LazyList<T>) elements;
        } else if (JavaConverters.underlying(elements) instanceof LazyList<?> underlying) {
            return (LazyList<T>) underlying;
        } else {
            return LazyListFactory.create(elements.iterator());
        }
    }

    /**
     * Creates a LazyList that contains the elements of the given {@link java.util.stream.Stream}.
     *
     * @param javaStream A {@link java.util.stream.Stream}
     * @param <T>        Component type of the LazyList.
     * @return A LazyList containing the given elements in the same order.
     */
    static <T extends @Nullable Object> LazyList<T> ofAll(java.util.stream.Stream<? extends T> javaStream) {
        Objects.requireNonNull(javaStream, "javaStream is null");
        return LazyListFactory.create(javaStream.iterator());
    }

    /**
     * Creates a LazyList from boolean values.
     *
     * @param elements boolean values
     * @return A new LazyList of Boolean values
     * @throws NullPointerException if elements is null
     */
    static LazyList<Boolean> ofAll(boolean ... elements) {
        Objects.requireNonNull(elements, "elements is null");
        return LazyList.ofAll(Iterator.ofAll(elements));
    }

    /**
     * Creates a LazyList from byte values.
     *
     * @param elements byte values
     * @return A new LazyList of Byte values
     * @throws NullPointerException if elements is null
     */
    static LazyList<Byte> ofAll(byte ... elements) {
        Objects.requireNonNull(elements, "elements is null");
        return LazyList.ofAll(Iterator.ofAll(elements));
    }

    /**
     * Creates a LazyList from char values.
     *
     * @param elements char values
     * @return A new LazyList of Character values
     * @throws NullPointerException if elements is null
     */
    static LazyList<Character> ofAll(char ... elements) {
        Objects.requireNonNull(elements, "elements is null");
        return LazyList.ofAll(Iterator.ofAll(elements));
    }

    /**
     * Creates a LazyList values double values.
     *
     * @param elements double values
     * @return A new LazyList of Double values
     * @throws NullPointerException if elements is null
     */
    static LazyList<Double> ofAll(double ... elements) {
        Objects.requireNonNull(elements, "elements is null");
        return LazyList.ofAll(Iterator.ofAll(elements));
    }

    /**
     * Creates a LazyList from float values.
     *
     * @param elements float values
     * @return A new LazyList of Float values
     * @throws NullPointerException if elements is null
     */
    static LazyList<Float> ofAll(float ... elements) {
        Objects.requireNonNull(elements, "elements is null");
        return LazyList.ofAll(Iterator.ofAll(elements));
    }

    /**
     * Creates a LazyList from int values.
     *
     * @param elements int values
     * @return A new LazyList of Integer values
     * @throws NullPointerException if elements is null
     */
    static LazyList<Integer> ofAll(int ... elements) {
        Objects.requireNonNull(elements, "elements is null");
        return LazyList.ofAll(Iterator.ofAll(elements));
    }

    /**
     * Creates a LazyList from long values.
     *
     * @param elements long values
     * @return A new LazyList of Long values
     * @throws NullPointerException if elements is null
     */
    static LazyList<Long> ofAll(long ... elements) {
        Objects.requireNonNull(elements, "elements is null");
        return LazyList.ofAll(Iterator.ofAll(elements));
    }

    /**
     * Creates a LazyList from short values.
     *
     * @param elements short values
     * @return A new LazyList of Short values
     * @throws NullPointerException if elements is null
     */
    static LazyList<Short> ofAll(short ... elements) {
        Objects.requireNonNull(elements, "elements is null");
        return LazyList.ofAll(Iterator.ofAll(elements));
    }

    /**
     * Creates a LazyList of char numbers starting from {@code from}, extending to {@code toExclusive - 1}.
     * <p>
     * Examples:
     * <pre>
     * {@code
     * LazyList.range('a', 'a')  // = LazyList()
     * LazyList.range('c', 'a')  // = LazyList()
     * LazyList.range('a', 'd')  // = LazyList('a', 'b', 'c')
     * }
     * </pre>
     *
     * @param from        the first char
     * @param toExclusive the last char + 1
     * @return a range of char values as specified or the empty LazyList if {@code from >= toExclusive}
     */
    static LazyList<Character> range(char from, char toExclusive) {
        return LazyList.ofAll(Iterator.range(from, toExclusive));
    }

    /**
     * Creates a LazyList of char numbers starting from {@code from}, extending to {@code toExclusive - 1},
     * with {@code step}.
     * <p>
     * Examples:
     * <pre>
     * {@code
     * LazyList.rangeBy('a', 'c', 1)  // = LazyList('a', 'b')
     * LazyList.rangeBy('a', 'd', 2)  // = LazyList('a', 'c')
     * LazyList.rangeBy('d', 'a', -2) // = LazyList('d', 'b')
     * LazyList.rangeBy('d', 'a', 2)  // = LazyList()
     * }
     * </pre>
     *
     * @param from        the first char
     * @param toExclusive the last char + 1
     * @param step        the step
     * @return a range of char values as specified or the empty LazyList if<br>
     * {@code from >= toExclusive} and {@code step > 0} or<br>
     * {@code from <= toExclusive} and {@code step < 0}
     * @throws IllegalArgumentException if {@code step} is zero
     */
    static LazyList<Character> rangeBy(char from, char toExclusive, int step) {
        return LazyList.ofAll(Iterator.rangeBy(from, toExclusive, step));
    }

    /**
     * Creates a LazyList of double numbers starting from {@code from}, extending up to but not including {@code toExclusive},
     * with {@code step}.
     * <p>
     * Examples:
     * <pre>
     * {@code
     * LazyList.rangeBy(1.0, 3.0, 1.0)  // = LazyList(1.0, 2.0)
     * LazyList.rangeBy(1.0, 4.0, 2.0)  // = LazyList(1.0, 3.0)
     * LazyList.rangeBy(4.0, 1.0, -2.0) // = LazyList(4.0, 2.0)
     * LazyList.rangeBy(4.0, 1.0, 2.0)  // = LazyList()
     * }
     * </pre>
     *
     * @param from        the first double
     * @param toExclusive the upper bound (exclusive)
     * @param step        the step
     * @return a range of double values as specified or the empty LazyList if<br>
     * {@code from >= toExclusive} and {@code step > 0} or<br>
     * {@code from <= toExclusive} and {@code step < 0}
     * @throws IllegalArgumentException if {@code step} is zero
     */
    static LazyList<Double> rangeBy(double from, double toExclusive, double step) {
        return LazyList.ofAll(Iterator.rangeBy(from, toExclusive, step));
    }

    /**
     * Creates a LazyList of int numbers starting from {@code from}, extending to {@code toExclusive - 1}.
     * <p>
     * Examples:
     * <pre>
     * {@code
     * LazyList.range(0, 0)  // = LazyList()
     * LazyList.range(2, 0)  // = LazyList()
     * LazyList.range(-2, 2) // = LazyList(-2, -1, 0, 1)
     * }
     * </pre>
     *
     * @param from        the first number
     * @param toExclusive the last number + 1
     * @return a range of int values as specified or the empty LazyList if {@code from >= toExclusive}
     */
    static LazyList<Integer> range(int from, int toExclusive) {
        return LazyList.ofAll(Iterator.range(from, toExclusive));
    }

    /**
     * Creates a LazyList of int numbers starting from {@code from}, extending to {@code toExclusive - 1},
     * with {@code step}.
     * <p>
     * Examples:
     * <pre>
     * {@code
     * LazyList.rangeBy(1, 3, 1)  // = LazyList(1, 2)
     * LazyList.rangeBy(1, 4, 2)  // = LazyList(1, 3)
     * LazyList.rangeBy(4, 1, -2) // = LazyList(4, 2)
     * LazyList.rangeBy(4, 1, 2)  // = LazyList()
     * }
     * </pre>
     *
     * @param from        the first number
     * @param toExclusive the last number + 1
     * @param step        the step
     * @return a range of int values as specified or the empty LazyList if<br>
     * {@code from >= toExclusive} and {@code step > 0} or<br>
     * {@code from <= toExclusive} and {@code step < 0}
     * @throws IllegalArgumentException if {@code step} is zero
     */
    static LazyList<Integer> rangeBy(int from, int toExclusive, int step) {
        return LazyList.ofAll(Iterator.rangeBy(from, toExclusive, step));
    }

    /**
     * Creates a LazyList of long numbers starting from {@code from}, extending to {@code toExclusive - 1}.
     * <p>
     * Examples:
     * <pre>
     * {@code
     * LazyList.range(0L, 0L)  // = LazyList()
     * LazyList.range(2L, 0L)  // = LazyList()
     * LazyList.range(-2L, 2L) // = LazyList(-2L, -1L, 0L, 1L)
     * }
     * </pre>
     *
     * @param from        the first number
     * @param toExclusive the last number + 1
     * @return a range of long values as specified or the empty LazyList if {@code from >= toExclusive}
     */
    static LazyList<Long> range(long from, long toExclusive) {
        return LazyList.ofAll(Iterator.range(from, toExclusive));
    }

    /**
     * Creates a LazyList of long numbers starting from {@code from}, extending to {@code toExclusive - 1},
     * with {@code step}.
     * <p>
     * Examples:
     * <pre>
     * {@code
     * LazyList.rangeBy(1L, 3L, 1L)  // = LazyList(1L, 2L)
     * LazyList.rangeBy(1L, 4L, 2L)  // = LazyList(1L, 3L)
     * LazyList.rangeBy(4L, 1L, -2L) // = LazyList(4L, 2L)
     * LazyList.rangeBy(4L, 1L, 2L)  // = LazyList()
     * }
     * </pre>
     *
     * @param from        the first number
     * @param toExclusive the last number + 1
     * @param step        the step
     * @return a range of long values as specified or the empty LazyList if<br>
     * {@code from >= toExclusive} and {@code step > 0} or<br>
     * {@code from <= toExclusive} and {@code step < 0}
     * @throws IllegalArgumentException if {@code step} is zero
     */
    static LazyList<Long> rangeBy(long from, long toExclusive, long step) {
        return LazyList.ofAll(Iterator.rangeBy(from, toExclusive, step));
    }

    /**
     * Creates a LazyList of char numbers starting from {@code from}, extending to {@code toInclusive}.
     * <p>
     * Examples:
     * <pre>
     * {@code
     * LazyList.rangeClosed('a', 'a')  // = LazyList('a')
     * LazyList.rangeClosed('c', 'a')  // = LazyList()
     * LazyList.rangeClosed('a', 'd')  // = LazyList('a', 'b', 'c', 'd')
     * }
     * </pre>
     *
     * @param from        the first char
     * @param toInclusive the last char
     * @return a range of char values as specified or the empty LazyList if {@code from > toInclusive}
     */
    static LazyList<Character> rangeClosed(char from, char toInclusive) {
        return LazyList.ofAll(Iterator.rangeClosed(from, toInclusive));
    }

    static LazyList<Character> rangeClosedBy(char from, char toInclusive, int step) {
        return LazyList.ofAll(Iterator.rangeClosedBy(from, toInclusive, step));
    }

    static LazyList<Double> rangeClosedBy(double from, double toInclusive, double step) {
        return LazyList.ofAll(Iterator.rangeClosedBy(from, toInclusive, step));
    }

    /**
     * Creates a LazyList of int numbers starting from {@code from}, extending to {@code toInclusive}.
     * <p>
     * Examples:
     * <pre>
     * {@code
     * LazyList.rangeClosed(0, 0)  // = LazyList(0)
     * LazyList.rangeClosed(2, 0)  // = LazyList()
     * LazyList.rangeClosed(-2, 2) // = LazyList(-2, -1, 0, 1, 2)
     * }
     * </pre>
     *
     * @param from        the first number
     * @param toInclusive the last number
     * @return a range of int values as specified or the empty LazyList if {@code from > toInclusive}
     */
    static LazyList<Integer> rangeClosed(int from, int toInclusive) {
        return LazyList.ofAll(Iterator.rangeClosed(from, toInclusive));
    }

    /**
     * Creates a LazyList of int numbers starting from {@code from}, extending to {@code toInclusive},
     * with {@code step}.
     * <p>
     * Examples:
     * <pre>
     * {@code
     * LazyList.rangeClosedBy(1, 3, 1)  // = LazyList(1, 2, 3)
     * LazyList.rangeClosedBy(1, 4, 2)  // = LazyList(1, 3)
     * LazyList.rangeClosedBy(4, 1, -2) // = LazyList(4, 2)
     * LazyList.rangeClosedBy(4, 1, 2)  // = LazyList()
     * }
     * </pre>
     *
     * @param from        the first number
     * @param toInclusive the last number
     * @param step        the step
     * @return a range of int values as specified or the empty LazyList if<br>
     * {@code from > toInclusive} and {@code step > 0} or<br>
     * {@code from < toInclusive} and {@code step < 0}
     * @throws IllegalArgumentException if {@code step} is zero
     */
    static LazyList<Integer> rangeClosedBy(int from, int toInclusive, int step) {
        return LazyList.ofAll(Iterator.rangeClosedBy(from, toInclusive, step));
    }

    /**
     * Creates a LazyList of long numbers starting from {@code from}, extending to {@code toInclusive}.
     * <p>
     * Examples:
     * <pre>
     * {@code
     * LazyList.rangeClosed(0L, 0L)  // = LazyList(0L)
     * LazyList.rangeClosed(2L, 0L)  // = LazyList()
     * LazyList.rangeClosed(-2L, 2L) // = LazyList(-2L, -1L, 0L, 1L, 2L)
     * }
     * </pre>
     *
     * @param from        the first number
     * @param toInclusive the last number
     * @return a range of long values as specified or the empty LazyList if {@code from > toInclusive}
     */
    static LazyList<Long> rangeClosed(long from, long toInclusive) {
        return LazyList.ofAll(Iterator.rangeClosed(from, toInclusive));
    }

    /**
     * Creates a LazyList of long numbers starting from {@code from}, extending to {@code toInclusive},
     * with {@code step}.
     * <p>
     * Examples:
     * <pre>
     * {@code
     * LazyList.rangeClosedBy(1L, 3L, 1L)  // = LazyList(1L, 2L, 3L)
     * LazyList.rangeClosedBy(1L, 4L, 2L)  // = LazyList(1L, 3L)
     * LazyList.rangeClosedBy(4L, 1L, -2L) // = LazyList(4L, 2L)
     * LazyList.rangeClosedBy(4L, 1L, 2L)  // = LazyList()
     * }
     * </pre>
     *
     * @param from        the first number
     * @param toInclusive the last number
     * @param step        the step
     * @return a range of long values as specified or the empty LazyList if<br>
     * {@code from > toInclusive} and {@code step > 0} or<br>
     * {@code from < toInclusive} and {@code step < 0}
     * @throws IllegalArgumentException if {@code step} is zero
     */
    static LazyList<Long> rangeClosedBy(long from, long toInclusive, long step) {
        return LazyList.ofAll(Iterator.rangeClosedBy(from, toInclusive, step));
    }

    /**
     * Transposes the rows and columns of a {@link LazyList} matrix.
     * <p>
     * Complexity: O(rows * columns); the whole matrix is computed now.
     *
     * @param <T> matrix element type
     * @param matrix to be transposed.
     * @return a transposed {@link LazyList} matrix.
     * @throws IllegalArgumentException if the row lengths of {@code matrix} differ.
     * <p>
     * ex: {@code
     * LazyList.transpose(LazyList(LazyList(1,2,3), LazyList(4,5,6))) → LazyList(LazyList(1,4), LazyList(2,5), LazyList(3,6))
     * }
     */
    static <T extends @Nullable Object> LazyList<LazyList<T>> transpose(LazyList<LazyList<T>> matrix) {
        return dev.zazr.collection.internal.Collections.transpose(matrix, LazyList::ofAll, LazyList::of);
    }

    /**
     * Creates a LazyList from a seed value and a function.
     * The function takes the seed at first.
     * The function should return {@code None} when it's
     * done generating the LazyList, otherwise {@code Some} {@code Tuple}
     * of the element for the next call and the value to add to the
     * resulting LazyList.
     * <p>
     * Example:
     * <pre>
     * {@code
     * LazyList.unfoldRight(10, x -> x == 0
     *             ? Option.none()
     *             : Option.some(new Tuple2<>(x, x-1)));
     * // LazyList(10, 9, 8, 7, 6, 5, 4, 3, 2, 1))
     * }
     * </pre>
     *
     * @param <T>  type of seeds
     * @param <U>  type of unfolded values
     * @param seed the start value for the iteration
     * @param f    the function to get the next step of the iteration
     * @return a LazyList with the values built up by the iteration
     * @throws NullPointerException if {@code f} is null or returns null
     */
    static <T extends @Nullable Object, U extends @Nullable Object> LazyList<U> unfoldRight(T seed, Function<? super T, Option<Tuple2<? extends U, ? extends T>>> f) {
        return Iterator.unfoldRight(seed, f, "LazyList.unfoldRight: f returned null").toLazyList();
    }

    /**
     * Creates a LazyList from a seed value and a function.
     * The function takes the seed at first.
     * The function should return {@code None} when it's
     * done generating the LazyList, otherwise {@code Some} {@code Tuple}
     * of the value to add to the resulting LazyList and
     * the element for the next call.
     * <p>
     * Example:
     * <pre>
     * {@code
     * LazyList.unfoldLeft(10, x -> x == 0
     *             ? Option.none()
     *             : Option.some(new Tuple2<>(x-1, x)));
     * // LazyList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10))
     * }
     * </pre>
     *
     * @param <T>  type of seeds
     * @param <U>  type of unfolded values
     * @param seed the start value for the iteration
     * @param f    the function to get the next step of the iteration
     * @return a LazyList with the values built up by the iteration
     * @throws NullPointerException if {@code f} is null or returns null
     */
    static <T extends @Nullable Object, U extends @Nullable Object> LazyList<U> unfoldLeft(T seed, Function<? super T, Option<Tuple2<? extends T, ? extends U>>> f) {
        return Iterator.unfoldLeft(seed, f, "LazyList.unfoldLeft: f returned null").toLazyList();
    }

    /**
     * Creates a LazyList from a seed value and a function.
     * The function takes the seed at first.
     * The function should return {@code None} when it's
     * done generating the LazyList, otherwise {@code Some} {@code Tuple}
     * of the value to add to the resulting LazyList and
     * the element for the next call.
     * <p>
     * Example:
     * <pre>
     * {@code
     * LazyList.unfold(10, x -> x == 0
     *             ? Option.none()
     *             : Option.some(new Tuple2<>(x-1, x)));
     * // LazyList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10))
     * }
     * </pre>
     *
     * @param <T>  type of seeds and unfolded values
     * @param seed the start value for the iteration
     * @param f    the function to get the next step of the iteration
     * @return a LazyList with the values built up by the iteration
     * @throws NullPointerException if {@code f} is null or returns null
     */
    static <T extends @Nullable Object> LazyList<T> unfold(T seed, Function<? super T, Option<Tuple2<? extends T, ? extends T>>> f) {
        return Iterator.unfold(seed, f, "LazyList.unfold: f returned null").toLazyList();
    }

    /**
     * Repeats an element infinitely often.
     *
     * @param t   An element
     * @param <T> Element type
     * @return A new LazyList containing infinite {@code t}'s.
     */
    static <T extends @Nullable Object> LazyList<T> continually(T t) {
        return LazyList.ofAll(Iterator.continually(t));
    }

    /**
     * Whether {@code that} occurs in this LazyList as a contiguous slice.
     * <p>
     * Complexity: O(n * m) for a slice of m elements; the elements are computed up to one past the first match.
     *
     * @param that the slice to look for
     * @return true if {@code that} occurs contiguously in this LazyList (an empty slice always does)
     * @throws NullPointerException if {@code that} is null, or if the search reaches a null element of {@code that};
     *                              the slice is read only as far as the comparisons go, so a null past them is not seen
     */
    default boolean containsSlice(Iterable<? extends T> that) {
        Objects.requireNonNull(that, "that is null");
        return indexOfSlice(that) >= 0;
    }

    /**
     * Whether this LazyList ends with {@code that}.
     * <p>
     * Complexity: O(n + m) for m elements of {@code that}; the whole LazyList is computed.
     *
     * @param that the suffix to test
     * @return true if the last {@code m} elements equal {@code that} (an empty {@code that} is always a suffix)
     * @throws NullPointerException if {@code that} is null
     */
    default boolean endsWith(Iterable<? extends T> that) {
        Objects.requireNonNull(that, "that is null");
        final LazyList<? extends T> suffix = LazyList.ofAll(that);
        final int skipped = size() - suffix.size();
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
     * Complexity: O(n); the elements are computed until the element is found.
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
     * Complexity: O(n * m) for a slice of m elements; the elements are computed up to one past the first match.
     *
     * @param that the slice to find
     * @return the index of its first occurrence, or -1 (an empty slice occurs at 0)
     * @throws NullPointerException if {@code that} is null, or if the search reaches a null element of {@code that};
     *                              the slice is read only as far as the comparisons go, so a null past them is not seen
     */
    default int indexOfSlice(Iterable<? extends T> that) {
        return indexOfSlice(that, 0);
    }

    /**
     * The first index at or after {@code from} at which {@code that} occurs as a contiguous slice, or -1.
     * <p>
     * Complexity: O(n * m) for a slice of m elements; the elements are computed up to one past the first match.
     *
     * @param that the slice to find
     * @param from the first position to look at
     * @return the index of its first occurrence at or after {@code from}, or -1
     * @throws NullPointerException if {@code that} is null, or if the search reaches a null element of {@code that};
     *                              the slice is read only as far as the comparisons go, so a null past them is not seen
     */
    default int indexOfSlice(Iterable<? extends T> that, int from) {
        Objects.requireNonNull(that, "that is null");
        return LazyListModule.Slice.indexOfSlice(this, that, from);
    }

    /**
     * The index of the first element satisfying {@code predicate}, or -1.
     * <p>
     * Complexity: O(n); the elements are computed until one satisfies the predicate.
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
     * Complexity: O(n); the elements are computed until one at or after {@code from} satisfies the predicate.
     *
     * @param predicate the condition
     * @param from      the first position to look at
     * @return the first index {@code >= from} of a satisfying element, or -1
     * @throws NullPointerException if {@code predicate} is null
     */
    default int indexWhere(Predicate<? super T> predicate, int from) {
        Objects.requireNonNull(predicate, "predicate is null");
        int i = Math.max(from, 0);
        LazyList<T> these = drop(i);
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
     * Complexity: O(n); the whole LazyList is computed.
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
     * Complexity: O(n * m) for a slice of m elements; the whole LazyList is computed.
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
     * Complexity: O(n * m) for a slice of m elements; the walk stops at most m elements past {@code end}, so it works
     * on an infinite LazyList.
     *
     * @param that the slice to find
     * @param end  the last position to look at
     * @return the index of its last occurrence at or before {@code end}, or -1
     * @throws NullPointerException if {@code that} is null
     */
    default int lastIndexOfSlice(Iterable<? extends T> that, int end) {
        Objects.requireNonNull(that, "that is null");
        return LazyListModule.Slice.lastIndexOfSlice(this, that, end);
    }

    /**
     * The index of the last element satisfying {@code predicate}, or -1.
     * <p>
     * Complexity: O(n); the whole LazyList is computed.
     *
     * @param predicate the condition
     * @return the last index of a satisfying element, or -1
     * @throws NullPointerException if {@code predicate} is null
     */
    default int lastIndexWhere(Predicate<? super T> predicate) {
        return lastIndexWhere(predicate, size() - 1);
    }

    /**
     * The index of the last element at or before {@code end} satisfying {@code predicate}, or -1.
     * <p>
     * Complexity: O(n); the elements up to one past {@code end} are computed, so it works on an infinite LazyList.
     *
     * @param predicate the condition
     * @param end       the last position to look at
     * @return the last index {@code <= end} of a satisfying element, or -1
     * @throws NullPointerException if {@code predicate} is null
     */
    default int lastIndexWhere(Predicate<? super T> predicate, int end) {
        Objects.requireNonNull(predicate, "predicate is null");
        int i = 0;
        LazyList<T> these = this;
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
     * Complexity: O(k) for a prefix of k elements; they are computed, and the first element after them.
     *
     * @param predicate the condition
     * @return the length of the prefix
     * @throws NullPointerException if {@code predicate} is null
     */
    default int prefixLength(Predicate<? super T> predicate) {
        return segmentLength(predicate, 0);
    }

    /**
     * The position of {@code element} in this LazyList, which must already be sorted in ascending natural order; the
     * result is undefined otherwise. The search is linear, as a LazyList has no indexed access.
     * <p>
     * Complexity: O(n); the elements are computed until one is not smaller than {@code element}.
     *
     * @param element the element to find
     * @return the index of the element if it is present; otherwise {@code (-(insertion point) - 1)}, the insertion
     *         point being the index at which the element would be inserted
     * @throws ClassCastException if {@code T} is not {@code Comparable}
     */
    @SuppressWarnings("unchecked")
    default int search(T element) {
        final ToIntFunction<T> comparison = ((Comparable<T>) element)::compareTo;
        return LazyListModule.Search.linearSearch(this, comparison);
    }

    /**
     * The position of {@code element} in this LazyList, which must already be sorted in ascending order according to
     * {@code comparator}; the result is undefined otherwise. The search is linear, as a LazyList has no indexed access.
     * <p>
     * Complexity: O(n); the elements are computed until one is not smaller than {@code element}.
     *
     * @param element    the element to find
     * @param comparator the order this LazyList is sorted by
     * @return the index of the element if it is present; otherwise {@code (-(insertion point) - 1)}, the insertion
     *         point being the index at which the element would be inserted
     * @throws NullPointerException if {@code comparator} is null
     */
    default int search(T element, Comparator<? super T> comparator) {
        Objects.requireNonNull(comparator, "comparator is null");
        final ToIntFunction<T> comparison = current -> comparator.compare(element, current);
        return LazyListModule.Search.linearSearch(this, comparison);
    }

    /**
     * The length of the longest run of elements satisfying {@code predicate} starting at {@code from}.
     * <p>
     * Complexity: O(i + k) for a run of k elements from index i; the elements up to the end of the run are computed,
     * and the first one after it.
     *
     * @param predicate the condition
     * @param from      the first position to look at
     * @return the length of the run
     * @throws NullPointerException if {@code predicate} is null
     */
    default int segmentLength(Predicate<? super T> predicate, int from) {
        Objects.requireNonNull(predicate, "predicate is null");
        int i = 0;
        LazyList<T> these = this.drop(from);
        while (!these.isEmpty() && predicate.test(these.head())) {
            i++;
            these = these.tail();
        }
        return i;
    }

    /**
     * Whether this LazyList starts with {@code that}: {@code startsWith(that, 0)}.
     * <p>
     * Complexity: O(m) for m elements of {@code that}; at most the first m + 1 elements are computed.
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
     * Complexity: O(i + m) for m elements of {@code that} from index i; at most the first i + m + 1 elements are
     * computed.
     *
     * @param that   the prefix to test
     * @param offset the position in this LazyList at which the prefix should start
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
     * @throws NullPointerException if {@code that} is null, or if the search reaches a null element of {@code that};
     *                              the slice is read only as far as the comparisons go, so a null past them is not seen
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
     * @throws NullPointerException if {@code that} is null, or if the search reaches a null element of {@code that};
     *                              the slice is read only as far as the comparisons go, so a null past them is not seen
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
     * The elements are folded from the end: this LazyList is reversed first, which computes all of it, then folded from
     * the left, so the recursion depth does not grow with the length.
     * <p>
     * Complexity: O(n); the whole LazyList is computed now and copied in reverse before the fold.
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
     * Returns a new LazyList with the given element appended at the end.
     * <p>
     * Complexity: O(1); nothing is computed now, and the element comes after the last one of this LazyList. Appending in
     * a loop stays O(1) per call.
     *
     * @param element the element to append
     * @return a new LazyList ending with the given element
     */
    default LazyList<T> append(T element) {
        return isEmpty() ? LazyList.of(element) : new Cons.AppendElements<>(head(), dev.zazr.collection.Queue.of(element), this::tail);
    }

    /**
     * Returns a new LazyList with the given elements appended at the end, in iteration order.
     * <p>
     * Complexity: O(m) for m elements on a LazyList built by {@link #append(Object)}, or a tail of one (the prefix of
     * {@link #splitAtInclusive(Predicate)} and the results of {@link #crossProduct(int)} are such LazyLists): the
     * elements are read now, so an infinite argument never returns. Otherwise O(1): nothing is computed now, but each
     * appendAll adds one step to reading every element of the result, so appendAll in a loop is quadratic: use append,
     * or build a Vector.
     *
     * @param elements the elements to append
     * @return a new LazyList ending with the given elements, or this LazyList if there are none
     * @throws NullPointerException if {@code elements} is null
     */
    default LazyList<T> appendAll(Iterable<? extends T> elements) {
        Objects.requireNonNull(elements, "elements is null");
        if (!Collections.isTraversableAgain(elements)) {
            // a one-shot source is read exactly once, into a memoising LazyList that also answers whether it is empty
            final LazyList<T> that = LazyList.ofAll(elements);
            return that.isEmpty() ? this : appendAll(that);
        } else if (Collections.isEmpty(elements)) {
            return this;
        } else if (isEmpty()) {
            return LazyList.ofAll(elements);
        } else {
            return LazyList.ofAll(Iterator.concat(this, elements));
        }
    }

    /**
     * Appends itself to the end of lazy list with {@code mapper} function.
     * <p>
     * <strong>Example:</strong>
     * <p>
     * Well known Scala code for Fibonacci infinite sequence
     * <pre>
     * {@code
     * val fibs:LazyList[Int] = 0 #:: 1 #:: (fibs zip fibs.tail).map{ t => t._1() + t._2() }
     * }
     * </pre>
     * can be transformed to
     * <pre>
     * {@code
     * LazyList.of(0, 1).appendSelf(self -> self.zip(self.tail()).map(t -> t._1() + t._2()));
     * }
     * </pre>
     * <p>
     * Complexity: O(1); nothing is computed now, each element when the result reaches it.
     *
     * @param mapper an mapper
     * @return this LazyList if it is empty, otherwise a new LazyList obtained by appending this LazyList, mapped by {@code mapper}, to itself
     * @throws NullPointerException if {@code mapper} is null, or, when the lazy list reaches it, returns null
     */
    default LazyList<T> appendSelf(Function<? super LazyList<T>, ? extends LazyList<T>> mapper) {
        Objects.requireNonNull(mapper, "mapper is null");
        return isEmpty() ? this : new AppendSelf<>((Cons<T>) this, mapper).stream();
    }

    /**
     * An unmodifiable {@link java.util.List} view of this LazyList, in its order: nothing is copied, reads go through
     * to this LazyList, which never changes, and every mutator of the view (including those of its iterators and
     * sub-lists) throws {@link UnsupportedOperationException}. {@code reversed()} and {@code subList} are views too.
     * A mutable copy is {@code new java.util.ArrayList<>(stream.asJava())}; {@code LazyList.ofAll} given the view
     * returns this LazyList without copying.
     * <p>
     * Complexity: O(1); the view computes no element before a read needs it: {@code get(i)} computes the first
     * {@code i + 1} elements, the iterator one element per step, and {@code size()}, {@code lastIndexOf},
     * {@code hashCode}, {@code getLast} and every read of {@code reversed()} compute the whole LazyList (they never
     * return on an infinite LazyList). The view counts the size once and keeps it.
     *
     * @return an unmodifiable {@code java.util.List} view
     */
    default java.util.List<T> asJava() {
        return JavaConverters.asJava(this);
    }

    /**
     * All combinations of the elements, for every size from 0 to {@code size()}, by position.
     * <p>
     * Complexity: O(n * 2^n) to read the 2^n combinations; the whole LazyList is computed now.
     *
     * @return the combinations, shortest first
     */
    default LazyList<LazyList<T>> combinations() {
        return LazyList.rangeClosed(0, size()).map(this::combinations).flatMap(Function.identity());
    }

    /**
     * All combinations of {@code k} elements, by position, in lexicographic position order. A negative {@code k}
     * counts as 0, and a {@code k} greater than {@code size()} gives no combination.
     * <p>
     * Complexity: lazy; the first k + 1 elements are computed now. Reading every combination costs O(n * C(n, k)) for a
     * small k, but the search explores every run of up to k positions, so it grows to O(n * 2^n) as k nears n, even
     * though few combinations remain. A k greater than the length pays all of it now, to return an empty LazyList.
     *
     * @param k the size of each combination
     * @return the combinations
     */
    default LazyList<LazyList<T>> combinations(int k) {
        return Combinations.apply(this, Math.max(k, 0));
    }

    /**
     * Repeat the elements of this LazyList infinitely.
     * <p>
     * Example:
     * <pre>
     * {@code
     * // = 1, 2, 3, 1, 2, 3, 1, 2, 3, ...
     * LazyList.of(1, 2, 3).cycle();
     * }
     * </pre>
     * <p>
     * Complexity: O(1); nothing is computed now, and the result is infinite.
     *
     * @return this LazyList if it is empty, otherwise a new LazyList containing this elements cycled.
     */
    default LazyList<T> cycle() {
        return isEmpty() ? this : appendSelf(Function.identity());
    }

    /**
     * Repeat the elements of this LazyList {@code count} times.
     * <p>
     * Example:
     * <pre>
     * {@code
     * // = empty
     * LazyList.of(1, 2, 3).cycle(0);
     *
     * // = 1, 2, 3
     * LazyList.of(1, 2, 3).cycle(1);
     *
     * // = 1, 2, 3, 1, 2, 3, 1, 2, 3
     * LazyList.of(1, 2, 3).cycle(3);
     * }
     * </pre>
     * <p>
     * Complexity: lazy; the result reads one element ahead of what it returns, so the first two elements are computed
     * now.
     *
     * @param count the number of cycles to be performed
     * @return A new LazyList containing this elements cycled {@code count} times.
     */
    default LazyList<T> cycle(int count) {
        if (count <= 0 || isEmpty()) {
            return empty();
        } else {
            final LazyList<T> self = this;
            return LazyList.ofAll(new Iterator<T>() {
                LazyList<T> stream = self;
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
     * Returns a new {@code LazyList} containing the elements of this instance
     * with all duplicates removed. Element equality is determined using {@code equals}.
     * <p>
     * Complexity: lazy; nothing is computed now. Moving to the next element skips and hashes the repeated ones before
     * it, which never ends on an infinite LazyList with no further new element.
     *
     * @return a new {@code LazyList} without duplicate elements
     */
    default LazyList<T> distinct() {
        return distinctBy(Function.identity());
    }

    /**
     * Returns a new {@code LazyList} containing the elements of this instance
     * without duplicates, as determined by the given {@code comparator}; the first of two equal elements is kept.
     * <p>
     * Complexity: lazy; O(log n) comparisons per element read. Moving to the next element skips the repeated ones, as
     * {@link #distinct()} does.
     *
     * @param comparator a comparator used to determine equality of elements
     * @return a new {@code LazyList} with duplicates removed
     * @throws NullPointerException if {@code comparator} is null
     */
    default LazyList<T> distinctBy(Comparator<? super T> comparator) {
        Objects.requireNonNull(comparator, "comparator is null");
        final java.util.Set<T> seen = new java.util.TreeSet<>(comparator);
        return filter(seen::add);
    }

    /**
     * Returns a new {@code LazyList} containing the elements of this instance
     * without duplicates, based on keys extracted from elements using {@code keyExtractor}.
     * <p>
     * The first occurrence of each key is retained in the resulting sequence.
     * <p>
     * Complexity: lazy; one key and one hash lookup per element read. Moving to the next element skips the repeated
     * ones, as {@link #distinct()} does.
     *
     * @param keyExtractor a function to extract keys for determining uniqueness
     * @param <U>          the type of key
     * @return a new {@code LazyList} with duplicates removed based on keys
     * @throws NullPointerException if {@code keyExtractor} is null
     */
    default <U extends @Nullable Object> LazyList<T> distinctBy(Function<? super T, ? extends U> keyExtractor) {
        Objects.requireNonNull(keyExtractor, "keyExtractor is null");
        final java.util.Set<U> seen = new java.util.HashSet<>();
        return filter(t -> seen.add(keyExtractor.apply(t)));
    }

    /**
     * The complement of {@link #distinct()}: the elements occurring more than once, each once, in order of first
     * occurrence. {@code LazyList.of(3, 1, 3, 2, 1, 3).duplicates()} is {@code LazyList.of(3, 1)}. {@code isEmpty()} on
     * the result is the "all distinct" test.
     * <p>
     * Complexity: O(n), one hash lookup per element; the whole LazyList is computed now, because whether an element
     * repeats is known only at the end.
     *
     * @return a new LazyList of the repeated elements
     */
    default LazyList<T> duplicates() {
        return duplicatesBy(Function.identity());
    }

    /**
     * {@link #duplicates()} under a key: the first element of each key occurring more than once, in order of first
     * occurrence. One pass, the key computed once per element.
     * <p>
     * Complexity: O(n), one key and one hash lookup per element; the whole LazyList is computed now.
     *
     * @param keyExtractor computes the key an element is compared by
     * @param <U>          the key type
     * @return a new LazyList of the first element of each repeated key
     * @throws NullPointerException if {@code keyExtractor} is null
     */
    default <U extends @Nullable Object> LazyList<T> duplicatesBy(Function<? super T, ? extends U> keyExtractor) {
        Objects.requireNonNull(keyExtractor, "keyExtractor is null");
        final java.util.List<T> duplicated = Collections.duplicatesBy(this, keyExtractor);
        return duplicated.isEmpty() ? empty() : ofAll(duplicated);
    }

    /**
     * The elements without duplicates, keeping the last occurrence of each group of elements the comparator calls
     * equal, in the order of those last occurrences.
     * <p>
     * Complexity: O(n log n) comparisons; the whole LazyList is computed now, because the last occurrence decides.
     *
     * @param comparator decides which elements are duplicates
     * @return a new LazyList
     * @throws NullPointerException if {@code comparator} is null
     */
    default LazyList<T> distinctByKeepLast(Comparator<? super T> comparator) {
        Objects.requireNonNull(comparator, "comparator is null");
        return ofAll(Iterator.ofAll(this).distinctByKeepLast(comparator));
    }

    /**
     * The elements without duplicates, keeping the last occurrence of each key, in the order of those last
     * occurrences.
     * <p>
     * Complexity: O(n), one key per element; the whole LazyList is computed now, because the last occurrence decides.
     *
     * @param keyExtractor computes the key an element is deduplicated by
     * @param <U>          the key type
     * @return a new LazyList
     * @throws NullPointerException if {@code keyExtractor} is null
     */
    default <U extends @Nullable Object> LazyList<T> distinctByKeepLast(Function<? super T, ? extends U> keyExtractor) {
        Objects.requireNonNull(keyExtractor, "keyExtractor is null");
        return ofAll(Iterator.ofAll(this).distinctByKeepLast(keyExtractor));
    }

    /**
     * Returns a new {@code LazyList} without the first {@code n} elements,
     * or an empty instance if this contains fewer than {@code n} elements.
     * <p>
     * Complexity: O(k + m) for k dropped elements; they and the first element kept are computed now, the rest when
     * the result reaches them. On a LazyList built by append, dropping into the m appended elements rebuilds them, at
     * every call. O(k) otherwise.
     *
     * @param n the number of elements to drop
     * @return a new instance excluding the first {@code n} elements
     */
    default LazyList<T> drop(int n) {
        LazyList<T> stream = this;
        while (n-- > 0 && !stream.isEmpty()) {
            stream = stream.tail();
        }
        return stream;
    }

    /**
     * Returns a new {@code LazyList} starting from the first element
     * that satisfies the given {@code predicate}, dropping all preceding elements.
     * <p>
     * Complexity: O(k) for k dropped elements; they and the first element kept are computed now, the rest when the
     * result reaches them.
     *
     * @param predicate a condition tested on each element
     * @return a new instance starting from the first element matching the predicate
     * @throws NullPointerException if {@code predicate} is null
     */
    default LazyList<T> dropUntil(Predicate<? super T> predicate) {
        Objects.requireNonNull(predicate, "predicate is null");
        return dropWhile(predicate.negate());
    }

    /**
     * Returns a new {@code LazyList} starting from the first element
     * that does not satisfy the given {@code predicate}, dropping all preceding elements.
     * <p>
     * This is equivalent to {@code dropUntil(predicate.negate())}, which is useful
     * for method references that cannot be negated directly.
     * <p>
     * Complexity: O(k) for k dropped elements; they and the first element kept are computed now, the rest when the
     * result reaches them.
     *
     * @param predicate a condition tested on each element
     * @return a new instance starting from the first element not matching the predicate
     * @throws NullPointerException if {@code predicate} is null
     */
    default LazyList<T> dropWhile(Predicate<? super T> predicate) {
        Objects.requireNonNull(predicate, "predicate is null");
        LazyList<T> stream = this;
        while (!stream.isEmpty() && predicate.test(stream.head())) {
            stream = stream.tail();
        }
        return stream;
    }

    /**
     * Returns a new {@code LazyList} without the last {@code n} elements,
     * or an empty instance if this contains fewer than {@code n} elements.
     * <p>
     * Complexity: O(k) for k dropped elements: the first k + 1 elements are computed now. The result then reads k
     * elements ahead of what it returns, so it works on an infinite LazyList.
     *
     * @param n the number of elements to drop from the end
     * @return a new instance excluding the last {@code n} elements
     */
    default LazyList<T> dropRight(int n) {
        if (n <= 0) {
            return this;
        } else {
            return DropRight.apply(take(n).toList(), List.empty(), drop(n));
        }
    }

    /**
     * The elements up to and including the last one satisfying {@code predicate}: the elements after it are dropped.
     * <p>
     * Complexity: O(n); the whole LazyList is computed now, because the last matching element decides.
     *
     * @param predicate the condition, tested from the end
     * @return a new LazyList
     * @throws NullPointerException if {@code predicate} is null
     */
    default LazyList<T> dropRightUntil(Predicate<? super T> predicate) {
        Objects.requireNonNull(predicate, "predicate is null");
        return reverse().dropUntil(predicate).reverse();
    }

    /**
     * The elements up to and including the last one not satisfying {@code predicate}, that is
     * {@code dropRightUntil(predicate.negate())}.
     * <p>
     * Complexity: O(n); the whole LazyList is computed now, because the last matching element decides.
     *
     * @param predicate the condition, tested from the end
     * @return a new LazyList
     * @throws NullPointerException if {@code predicate} is null
     */
    default LazyList<T> dropRightWhile(Predicate<? super T> predicate) {
        Objects.requireNonNull(predicate, "predicate is null");
        return dropRightUntil(predicate.negate());
    }

    /**
     * Returns a new traversable containing only the elements that satisfy the given predicate.
     * <p>
     * Complexity: lazy; the elements up to the first match are computed now. Moving to the next element skips every
     * element that does not match, which never ends on an infinite LazyList with no further match.
     *
     * @param predicate the condition to test elements
     * @return a traversable with elements matching the predicate
     * @throws NullPointerException if {@code predicate} is null
     */
    default LazyList<T> filter(Predicate<? super T> predicate) {
        Objects.requireNonNull(predicate, "predicate is null");
        if (isEmpty()) {
            return this;
        } else {
            LazyList<T> stream = this;
            while (!stream.isEmpty() && !predicate.test(stream.head())) {
                stream = stream.tail();
            }
            final LazyList<T> finalLazyList = stream;
            return stream.isEmpty() ? LazyList.empty()
                                    : cons(stream.head(), () -> finalLazyList.tail().filter(predicate));
        }
    }

    /**
     * The elements that do not satisfy {@code predicate}, in order: the complement of {@link #filter(Predicate)}.
     * <p>
     * Complexity: lazy, like {@link #filter(Predicate)}: the elements up to the first one kept are computed now. Moving
     * to the next element skips every element that satisfies the predicate, which never ends on an infinite LazyList with
     * nothing left to keep.
     *
     * @param predicate the condition of the elements left out
     * @return a new LazyList
     * @throws NullPointerException if {@code predicate} is null
     */
    default LazyList<T> reject(Predicate<? super T> predicate) {
        Objects.requireNonNull(predicate, "predicate is null");
        return Collections.reject(this, predicate, kept -> filter(kept));
    }

    /**
     * The elements of the iterables {@code mapper} returns for the elements of this LazyList, in order.
     * <p>
     * Complexity: lazy; the elements are computed now until {@code mapper} returns a non-empty result. Moving on skips
     * the empty results, which never ends on an infinite LazyList whose results are all empty from some point on.
     *
     * @param mapper maps an element to the elements that replace it
     * @param <U>    the element type of the result
     * @return a new LazyList
     * @throws NullPointerException if {@code mapper} is null
     */
    default <U extends @Nullable Object> LazyList<U> flatMap(Function<? super T, ? extends Iterable<? extends U>> mapper) {
        Objects.requireNonNull(mapper, "mapper is null");
        return isEmpty() ? Empty.instance() : LazyList.ofAll(new FlatMapIterator<>(Iterator.ofAll(this), mapper, "LazyList.flatMap: mapper returned null"));
    }

    /**
     * The element at {@code index}.
     * <p>
     * Complexity: O(i + m); the first i + 1 elements are computed. On a LazyList built by append, reaching the m
     * appended elements rebuilds them, at every call. O(i) otherwise.
     *
     * @param index the position
     * @return the element at that position
     * @throws IndexOutOfBoundsException if {@code index} is negative or not less than {@code size()}
     */
    default T get(int index) {
        if (isEmpty()) {
            throw new IndexOutOfBoundsException("get(" + index + ") on Nil");
        }
        if (index < 0) {
            throw new IndexOutOfBoundsException("get(" + index + ")");
        }
        LazyList<T> stream = this;
        for (int i = index - 1; i >= 0; i--) {
            stream = stream.tail();
            if (stream.isEmpty()) {
                throw new IndexOutOfBoundsException("get(" + index + ") on LazyList of size " + (index - i));
            }
        }
        return stream.head();
    }

    /**
     * The elements grouped by the key {@code classifier} computes, in a map ordered by the first occurrence of each
     * key; each group keeps the order of this LazyList.
     * <p>
     * Complexity: O(n), one key and one hash lookup per element; the whole LazyList is computed now.
     *
     * @param classifier the key of an element
     * @param <C>        the key type
     * @return the groups by key
     * @throws NullPointerException if {@code classifier} is null, or returns null
     */
    default <C extends @Nullable Object> Map<C, LazyList<T>> groupBy(Function<? super T, ? extends C> classifier) {
        return dev.zazr.collection.internal.Collections.groupBy(this, classifier, LazyList::ofAll, "LazyList.groupBy: classifier returned null");
    }

    /**
     * The index of the first occurrence of {@code element} at or after {@code from}, or -1. A negative {@code from}
     * counts as 0.
     * <p>
     * Complexity: O(n); the elements are computed until the element is found at or after {@code from}.
     *
     * @param element the element to find
     * @param from    the first position to look at
     * @return the first index {@code >= from} of the element, or -1 if absent
     */
    default int indexOf(T element, int from) {
        int index = 0;
        for (LazyList<T> stream = this; !stream.isEmpty(); stream = stream.tail(), index++) {
            if (index >= from && Objects.equals(stream.head(), element)) {
                return index;
            }
        }
        return -1;
    }

    /**
     * Returns all elements of this LazyList except the last one.
     * <p>
     * This is the dual of {@link #tail()}.
     * <p>
     * Complexity: lazy; the result reads one element ahead of what it returns, so the first two elements are computed
     * now.
     *
     * @return a new instance containing all elements except the last
     * @throws UnsupportedOperationException if this LazyList is empty
     */
    default LazyList<T> init() {
        if (isEmpty()) {
            throw new UnsupportedOperationException("init of empty stream");
        } else {
            final LazyList<T> tail = tail();
            if (tail.isEmpty()) {
                return Empty.instance();
            } else {
                return cons(head(), tail::init);
            }
        }
    }

    /**
     * Returns all elements of this LazyList except the last one, wrapped in an {@code Option}.
     * <p>
     * This is the dual of {@link #tailOption()}.
     * <p>
     * Complexity: lazy, as {@link #init()}.
     *
     * @return {@code Some(traversable)} if non-empty, or {@code None} if this LazyList is empty
     */
    default Option<LazyList<T>> initOption() {
        return isEmpty() ? Option.none() : Option.some(init());
    }

    /**
     * Because {@code LazyList} is lazy, only {@code index < 0} (and {@code index > 0} on an empty LazyList)
     * is detected when this method is called; for {@code index > size()} the
     * {@code IndexOutOfBoundsException} is thrown only once the returned LazyList is traversed as far
     * as the offending position.
     * <p>
     * Complexity: lazy; nothing is computed now. The result copies the elements before index i as it reaches them, and
     * shares the rest.
     */
    default LazyList<T> insert(int index, T element) {
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
     * Because {@code LazyList} is lazy, only {@code index < 0} (and {@code index > 0} on an empty LazyList)
     * is detected when this method is called; for {@code index > size()} the
     * {@code IndexOutOfBoundsException} is thrown only once the returned LazyList is traversed as far
     * as the offending position.
     * <p>
     * Complexity: O(n) when {@code elements} is a LazyList built by {@link #append(Object)}, or a tail of one (see
     * {@link #appendAll(Iterable)}): the rest of this LazyList is then read when the result reaches index i, now when i
     * is 0, so an infinite one never returns. Otherwise lazy: nothing is computed now, and the result copies the
     * elements before index i as it reaches them, then joins {@code elements} and the rest as appendAll does.
     */
    default LazyList<T> insertAll(int index, Iterable<? extends T> elements) {
        Objects.requireNonNull(elements, "elements is null");
        if (index < 0) {
            throw new IndexOutOfBoundsException("insertAll(" + index + ", elements)");
        } else if (index == 0) {
            return isEmpty() ? LazyList.ofAll(elements) : LazyList.<T> ofAll(elements).appendAll(this);
        } else if (isEmpty()) {
            throw new IndexOutOfBoundsException("insertAll(" + index + ", elements) on Nil");
        } else {
            return cons(head(), () -> tail().insertAll(index - 1, elements));
        }
    }

    /**
     * The elements with {@code element} inserted between every two of them.
     * <p>
     * Complexity: lazy; nothing is computed now, and reaching a separator computes the element after it.
     *
     * @param element the separator
     * @return a new LazyList, or this LazyList if it is empty
     */
    default LazyList<T> intersperse(T element) {
        if (isEmpty()) {
            return this;
        } else {
            return cons(head(), () -> {
                final LazyList<T> tail = tail();
                return tail.isEmpty() ? tail : cons(element, () -> tail.intersperse(element));
            });
        }
    }

    /**
     * Returns the last element of this LazyList.
     * <p>
     * Complexity: O(n); the whole LazyList is computed.
     *
     * @return the last element
     * @throws NoSuchElementException if this LazyList is empty
     */
    default T last() {
        return Collections.last(this);
    }

    /**
     * The index of the last occurrence of {@code element} at or before {@code end}, or -1.
     * <p>
     * Complexity: O(n); the elements up to one past {@code end} are computed, so it works on an infinite LazyList.
     *
     * @param element the element to find
     * @param end     the last position to look at
     * @return the last index {@code <= end} of the element, or -1 if absent
     */
    default int lastIndexOf(T element, int end) {
        int result = -1, index = 0;
        for (LazyList<T> stream = this; index <= end && !stream.isEmpty(); stream = stream.tail(), index++) {
            if (Objects.equals(stream.head(), element)) {
                result = index;
            }
        }
        return result;
    }


    /**
     * The elements transformed by {@code mapper}, in order.
     * <p>
     * Complexity: lazy; {@code mapper} runs on the first element now, and on each other one when the result reaches it.
     *
     * @param mapper transforms an element
     * @param <U>    the element type of the result
     * @return a new LazyList
     * @throws NullPointerException if {@code mapper} is null
     */
    default <U extends @Nullable Object> LazyList<U> map(Function<? super T, ? extends U> mapper) {
        Objects.requireNonNull(mapper, "mapper is null");
        if (isEmpty()) {
            return Empty.instance();
        } else {
            return cons(mapper.apply(head()), () -> tail().map(mapper));
        }
    }

    /**
     * The values {@code mapper} returns for the elements it keeps, in order: an element is kept when {@code mapper}
     * returns a {@code Some}, and {@code mapper} runs once per element.
     * <p>
     * Complexity: lazy, like {@link #filter(Predicate)}: the elements up to the first one kept, and the one after it,
     * are computed now. Moving to the next element skips every element {@code mapper} drops, which never ends on an
     * infinite LazyList with nothing left to keep.
     *
     * @param mapper the value of an element, or {@code None} to drop it
     * @param <U>    the element type of the result
     * @return a new LazyList
     * @throws NullPointerException if {@code mapper} is null, or returns null for an element
     */
    default <U extends @Nullable Object> LazyList<U> collect(Function<? super T, ? extends Option<? extends U>> mapper) {
        Objects.requireNonNull(mapper, "mapper is null");
        // walk to the first kept element now, the rest lazily; the Option found on the way is the head, so the
        // mapper never runs twice for an element
        LazyList<T> stream = this;
        while (!stream.isEmpty()) {
            final Option<? extends U> collected = Objects.requireNonNull(mapper.apply(stream.head()), "LazyList.collect: mapper returned null");
            if (collected.isDefined()) {
                final LazyList<T> tail = stream.tail();
                return cons(collected.get(), () -> tail.collect(mapper));
            }
            stream = stream.tail();
        }
        return Empty.instance();
    }

    default <U extends @Nullable Object> LazyList<U> as(U value) {
        return map(ignored -> value);
    }

    /**
     * This LazyList padded on the right with {@code element} until it is {@code length} long.
     * <p>
     * Complexity: lazy; nothing is computed now, each element when the result reaches it.
     *
     * @param length  the target length
     * @param element the padding element
     * @return a new LazyList, or this LazyList if {@code length} is not positive
     */
    default LazyList<T> padTo(int length, T element) {
        if (length <= 0) {
            return this;
        } else if (isEmpty()) {
            return LazyList.continually(element).take(length);
        } else {
            return cons(head(), () -> tail().padTo(length - 1, element));
        }
    }

    /**
     * This LazyList padded on the left with {@code element} until it is {@code length} long.
     * <p>
     * Complexity: O(n); the whole LazyList is computed now, because its length decides how much padding is needed.
     *
     * @param length  the target length
     * @param element the padding element
     * @return a new LazyList, or this LazyList if it is already at least {@code length} long
     */
    default LazyList<T> leftPadTo(int length, T element) {
        final int actualLength = size();
        if (length <= actualLength) {
            return this;
        } else {
            return LazyList.continually(element).take(length - actualLength).appendAll(this);
        }
    }

    default LazyList<T> orElse(Iterable<? extends T> other) {
        return isEmpty() ? ofAll(other) : this;
    }

    default LazyList<T> orElse(Supplier<? extends Iterable<? extends T>> supplier) {
        Objects.requireNonNull(supplier, "supplier is null");
        return isEmpty() ? ofAll(Objects.requireNonNull(supplier.get(), "LazyList.orElse: supplier returned null")) : this;
    }

    /**
     * This LazyList with {@code replaced} elements from {@code from} on replaced by {@code that}. A negative
     * {@code from} or {@code replaced} counts as 0.
     * <p>
     * Complexity: lazy; each element is computed when the result reaches it, and the replaced ones are skipped then.
     * When {@code from} is 0 and {@code that} is empty, the result starts after the replaced elements, so they are
     * computed now.
     *
     * @param from     the first replaced position
     * @param that     the replacement elements
     * @param replaced how many elements are replaced
     * @return a new LazyList
     * @throws NullPointerException if {@code that} is null
     */
    default LazyList<T> patch(int from, Iterable<? extends T> that, int replaced) {
        Objects.requireNonNull(that, "that is null");
        // LazyList.ofAll takes the replacement's iterator now and reads its first element (a LazyList is used as is); its
        // other elements and the cells of this LazyList are read as the result reaches them
        return patchFrom(this, Math.max(from, 0), LazyList.ofAll(that), Math.max(replaced, 0));
    }

    // The elements of stream before position `from`, then the replacement, then stream without the `replaced` elements
    // from `from` on; each cell is built when the result reaches it.
    private static <T extends @Nullable Object> LazyList<T> patchFrom(LazyList<T> stream, int from, LazyList<T> replacement, int replaced) {
        if (from > 0 && !stream.isEmpty()) {
            return cons(stream.head(), () -> patchFrom(stream.tail(), from - 1, replacement, replaced));
        } else {
            return concatThen(replacement, () -> stream.drop(replaced));
        }
    }

    // The elements of first, then those of the LazyList the supplier gives, asked for only when first is exhausted.
    private static <T extends @Nullable Object> LazyList<T> concatThen(LazyList<T> first, Supplier<LazyList<T>> rest) {
        if (first.isEmpty()) {
            return rest.get();
        } else {
            return cons(first.head(), () -> concatThen(first.tail(), rest));
        }
    }

    /**
     * The elements that satisfy {@code predicate} and those that do not, each in order.
     * <p>
     * Complexity: lazy; each side computes the elements up to its first one now, as {@link #filter(Predicate)} does, so
     * the call never returns on an infinite LazyList when one side stays empty. The predicate runs twice per element,
     * once for each side.
     *
     * @param predicate the condition
     * @return the matching elements and the others
     * @throws NullPointerException if {@code predicate} is null
     */
    default Tuple2<LazyList<T>, LazyList<T>> partition(Predicate<? super T> predicate) {
        Objects.requireNonNull(predicate, "predicate is null");
        return Tuple.of(filter(predicate), filter(predicate.negate()));
    }

    /**
     * Splits the elements into a left and a right side according to the {@link Either} {@code f} returns for each: the
     * generalisation of {@link #partition(Predicate)}. Lazy like {@code partition}: both sides are LazyLists read from
     * one LazyList of the results of {@code f}, each computed once and kept, so {@code f} is called once per element,
     * in order, when either side first reaches that element, and never again.
     * <p>
     * Complexity: lazy; each side computes the elements up to its first one now, the others when that side reaches
     * them, and {@code f} runs once per element. The values one side has passed are kept until the other side passes
     * them too. On an infinite LazyList whose elements all go to one side, the call never returns.
     *
     * @param f   Classifies an element
     * @param <L> Component type of the left side
     * @param <R> Component type of the right side
     * @return the left values and the right values, each in the order of the elements they come from
     * @throws NullPointerException if {@code f} is null, or when it returns null for an element a side reaches
     */
    default <L extends @Nullable Object, R extends @Nullable Object> Tuple2<LazyList<L>, LazyList<R>> partitionMap(Function<? super T, ? extends Either<? extends L, ? extends R>> f) {
        Objects.requireNonNull(f, "f is null");
        final LazyList<Either<? extends L, ? extends R>> results =
                this.<Either<? extends L, ? extends R>> map(element -> Objects.requireNonNull(f.apply(element), "LazyList.partitionMap: f returned null"));
        return Tuple.of(lefts(results), rights(results));
    }

    // the left values of a LazyList of results, found lazily: skips the Rights to the next Left, now, the rest on demand
    private static <L extends @Nullable Object> LazyList<L> lefts(LazyList<? extends Either<? extends L, ?>> results) {
        LazyList<? extends Either<? extends L, ?>> stream = results;
        while (!stream.isEmpty()) {
            if (stream.head() instanceof Either.Left<? extends L, ?>(var left)) {
                final LazyList<? extends Either<? extends L, ?>> rest = stream;
                return cons(left, () -> lefts(rest.tail()));
            }
            stream = stream.tail();
        }
        return empty();
    }

    // the right values of a LazyList of results, found lazily: skips the Lefts to the next Right, now, the rest on demand
    private static <R extends @Nullable Object> LazyList<R> rights(LazyList<? extends Either<?, ? extends R>> results) {
        LazyList<? extends Either<?, ? extends R>> stream = results;
        while (!stream.isEmpty()) {
            if (stream.head() instanceof Either.Right<?, ? extends R>(var right)) {
                final LazyList<? extends Either<?, ? extends R>> rest = stream;
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
     * <p>
     * Complexity: lazy; the action runs on the first element now, and on each other one when the result reaches it.
     *
     * @param action what to do with each element
     * @return this LazyList if it is empty; otherwise a new, structurally equal LazyList whose elements are handed to
     *         {@code action} lazily as they are traversed
     * @throws NullPointerException if {@code action} is null
     */
    default LazyList<T> tap(Consumer<? super T> action) {
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
     * Complexity: O(n! * n^2) to read every permutation of n distinct elements (fewer permutations when some are
     * equal). The whole LazyList is computed now, and O(n!) of the work is done before the call returns.
     *
     * @return the permutations
     */
    default LazyList<LazyList<T>> permutations() {
        if (isEmpty()) {
            return Empty.instance();
        } else {
            final LazyList<T> tail = tail();
            if (tail.isEmpty()) {
                return LazyList.of(this);
            } else {
                final LazyList<LazyList<T>> zero = Empty.instance();
                return distinct().foldLeft(zero, (xs, x) -> {
                    final Function<LazyList<T>, LazyList<T>> prepend = l -> l.prepend(x);
                    return xs.appendAll(remove(x).permutations().map(prepend));
                });
            }
        }
    }

    /**
     * A new LazyList with {@code element} in front of this one.
     * <p>
     * Complexity: O(1); nothing is computed.
     *
     * @param element the new head
     * @return a new LazyList starting with the given element
     */
    default LazyList<T> prepend(T element) {
        return cons(element, () -> this);
    }

    /**
     * A new LazyList with {@code elements} in front of this one, in iteration order.
     * <p>
     * Complexity: O(n) when {@code elements} is a LazyList built by {@link #append(Object)}, or a tail of one (see
     * {@link #appendAll(Iterable)}): this whole LazyList is then read now, so an infinite one never returns. Otherwise
     * O(1): nothing is computed now, but each prependAll adds one step to reading every element of the result, so
     * prependAll in a loop is quadratic.
     *
     * @param elements the elements to prepend
     * @return a new LazyList starting with the given elements, or this LazyList if there are none
     * @throws NullPointerException if {@code elements} is null
     */
    default LazyList<T> prependAll(Iterable<? extends T> elements) {
        Objects.requireNonNull(elements, "elements is null");
        if (isEmpty()) {
            if (elements instanceof LazyList) {
                @SuppressWarnings("unchecked")
                final LazyList<T> stream = (LazyList<T>) elements;
                return stream;
            } else {
                return LazyList.ofAll(elements);
            }
        } else {
            return LazyList.<T> ofAll(elements).appendAll(this);
        }
    }

    /**
     * This LazyList without the first occurrence of {@code element}.
     * <p>
     * Complexity: lazy; each element is compared when the result reaches it, and the elements after the removed one are
     * shared. When the first element is the one removed, the second is computed now.
     *
     * @param element the element to remove
     * @return a new LazyList, or this LazyList if it is empty
     */
    default LazyList<T> remove(T element) {
        if (isEmpty()) {
            return this;
        } else {
            final T head = head();
            return Objects.equals(head, element) ? tail() : cons(head, () -> tail().remove(element));
        }
    }

    /**
     * This LazyList without the first element satisfying {@code predicate}.
     * <p>
     * Complexity: lazy; each element is tested when the result reaches it, and the elements after the removed one are
     * shared. When the first element is the one removed, the second is computed now.
     *
     * @param predicate the condition
     * @return a new LazyList, or this LazyList if it is empty
     * @throws NullPointerException if {@code predicate} is null
     */
    default LazyList<T> removeFirst(Predicate<T> predicate) {
        Objects.requireNonNull(predicate, "predicate is null");
        if (isEmpty()) {
            return this;
        } else {
            final T head = head();
            return predicate.test(head) ? tail() : cons(head, () -> tail().removeFirst(predicate));
        }
    }

    /**
     * This LazyList without the last element satisfying {@code predicate}.
     * <p>
     * Complexity: O(n); the whole LazyList is computed now, because the last match decides.
     *
     * @param predicate the condition
     * @return a new LazyList, or this LazyList if it is empty
     * @throws NullPointerException if {@code predicate} is null
     */
    default LazyList<T> removeLast(Predicate<T> predicate) {
        return isEmpty() ? this : reverse().removeFirst(predicate).reverse();
    }

    /**
     * Because {@code LazyList} is lazy, only {@code index < 0} and an empty LazyList are detected when
     * this method is called; for {@code index >= size()} on a non-empty LazyList the
     * {@code IndexOutOfBoundsException} is thrown only once the returned LazyList is traversed as far
     * as the offending position.
     * <p>
     * Complexity: lazy; nothing is computed now (the second element when i is 0). The result copies the elements before
     * index i as it reaches them, and shares the rest.
     */
    default LazyList<T> removeAt(int index) {
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
     * This LazyList without any occurrence of {@code element}.
     * <p>
     * Complexity: lazy, like {@link #filter(Predicate)}: the elements up to the first one kept are computed now. Moving
     * to the next element skips every occurrence of {@code element}, which never ends on an infinite LazyList with
     * nothing left to keep.
     *
     * @param element the element to remove
     * @return a new LazyList
     */
    default LazyList<T> removeAll(T element) {
        return dev.zazr.collection.internal.Collections.removeAll(this, element, kept -> filter(kept));
    }

    /**
     * This LazyList without any occurrence of any of {@code elements}.
     * <p>
     * Complexity: lazy, like {@link #filter(Predicate)}: the m given elements are hashed now, and the elements up to
     * the first one kept are computed. Moving to the next element skips every removed element, which never ends on an
     * infinite LazyList with nothing left to keep.
     *
     * @param elements the elements to remove
     * @return a new LazyList
     * @throws NullPointerException if {@code elements} is null
     */
    default LazyList<T> removeAll(Iterable<? extends T> elements) {
        return dev.zazr.collection.internal.Collections.removeAll(this, elements, kept -> filter(kept));
    }

    /**
     * This LazyList without the elements satisfying {@code predicate}.
     * <p>
     * Complexity: lazy, like {@link #filter(Predicate)}: the elements up to the first one kept are computed now. Moving
     * to the next element skips every element that satisfies the predicate, which never ends on an infinite LazyList with
     * nothing left to keep.
     *
     * @deprecated use {@link #reject(Predicate)}
     * @param predicate the condition
     * @return a new LazyList
     * @throws NullPointerException if {@code predicate} is null
     */
    @Deprecated
    default LazyList<T> removeAll(Predicate<? super T> predicate) {
        Objects.requireNonNull(predicate, "predicate is null");
        return reject(predicate);
    }

    /**
     * Replaces the first occurrence of {@code currentElement} with {@code newElement}, if it exists.
     * <p>
     * Complexity: lazy; nothing is computed now. Each element is compared when the result reaches it, and the elements
     * after the replaced one are shared.
     *
     * @param currentElement the element to be replaced
     * @param newElement     the replacement element
     * @return a new LazyList with the first occurrence of {@code currentElement} replaced by {@code newElement}
     */
    default LazyList<T> replace(T currentElement, T newElement) {
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
     * Complexity: lazy; nothing is computed now, and each element is compared when the result reaches it.
     *
     * @param currentElement the element to be replaced
     * @param newElement     the replacement element
     * @return a new LazyList with all occurrences of {@code currentElement} replaced by {@code newElement}
     */
    default LazyList<T> replaceAll(T currentElement, T newElement) {
        if (isEmpty()) {
            return this;
        } else {
            final T head = head();
            final T newHead = Objects.equals(head, currentElement) ? newElement : head;
            return cons(newHead, () -> tail().replaceAll(currentElement, newElement));
        }
    }

    /**
     * Retains only the elements from this LazyList that are contained in the given {@code elements}.
     * <p>
     * Complexity: lazy, like {@link #filter(Predicate)}: the m given elements are hashed now, and the elements up to
     * the first one kept are computed. Moving to the next element skips every element that is not among them, which
     * never ends on an infinite LazyList with nothing left to keep.
     *
     * @param elements the elements to keep
     * @return a new LazyList containing only the elements present in {@code elements}, in their original order
     * @throws NullPointerException if {@code elements} is null
     */
    default LazyList<T> retainAll(Iterable<? extends T> elements) {
        return dev.zazr.collection.internal.Collections.retainAll(this, elements, kept -> filter(kept));
    }

    /**
     * The elements in reverse order.
     * <p>
     * Complexity: O(n); the whole LazyList is computed now.
     *
     * @return a new LazyList, or this LazyList if it is empty
     */
    default LazyList<T> reverse() {
        return isEmpty() ? this : foldLeft(LazyList.empty(), LazyList::prepend);
    }

    /**
     * Rotates the elements {@code n} positions to the left: {@code LazyList(1, 2, 3, 4, 5).rotateLeft(2)} is
     * {@code LazyList(3, 4, 5, 1, 2)}. A negative {@code n} rotates right; {@code n} is taken modulo the length.
     * <p>
     * Complexity: O(n); the whole LazyList is computed now, because its length decides the rotation. A rotation by 0 is
     * O(1) and works on an infinite LazyList.
     *
     * @param n the distance
     * @return the rotated LazyList, or this LazyList if the rotation is a multiple of the length
     */
    default LazyList<T> rotateLeft(int n) {
        // n == 0 before size(): a no-op rotation must not walk the elements, let alone force a lazy sequence
        if (n == 0 || isEmpty()) {
            return this;
        }
        final int k = Math.floorMod(n, size());
        return (k == 0) ? this : drop(k).appendAll(take(k));
    }

    /**
     * Rotates the elements {@code n} positions to the right: {@code LazyList(1, 2, 3, 4, 5).rotateRight(2)} is
     * {@code LazyList(4, 5, 1, 2, 3)}. A negative {@code n} rotates left; {@code n} is taken modulo the length.
     * <p>
     * Complexity: O(n); the whole LazyList is computed now, because its length decides the rotation. A rotation by 0 is
     * O(1) and works on an infinite LazyList.
     *
     * @param n the distance
     * @return the rotated LazyList, or this LazyList if the rotation is a multiple of the length
     */
    default LazyList<T> rotateRight(int n) {
        // n == 0 before size(): a no-op rotation must not walk the elements, let alone force a lazy sequence
        if (n == 0 || isEmpty()) {
            return this;
        }
        final int k = Math.floorMod(n, size());
        return (k == 0) ? this : takeRight(k).appendAll(dropRight(k));
    }

    /**
     * Computes a prefix scan of the elements of this LazyList.
     * <p>
     * The neutral element {@code zero} may be applied more than once.
     * <p>
     * Complexity: lazy; nothing is computed now, each element when the result reaches it.
     *
     * @param zero      the neutral element for the operator
     * @param operation an associative binary operator
     * @return a new LazyList containing the prefix scan of the elements
     * @throws NullPointerException if {@code operation} is null
     */
    default LazyList<T> scan(T zero, BiFunction<? super T, ? super T, ? extends T> operation) {
        return scanLeft(zero, operation);
    }

    /**
     * Produces a collection containing cumulative results of applying the operator from left to right.
     * <p>
     * The results are produced as the underlying elements are consumed, so {@code scanLeft} terminates even for
     * an infinite LazyList as long as only a finite prefix of the result is consumed. Contrast with
     * {@link #scanRight}, which is not lazy and will not terminate for an infinite LazyList.
     * <p>
     * Complexity: lazy; nothing is computed now, each element when the result reaches it.
     *
     * @param <U>       the type of the resulting elements
     * @param zero      the initial value
     * @param operation a binary operator applied to the intermediate result and each element
     * @return a new LazyList containing the cumulative results
     * @throws NullPointerException if {@code operation} is null
     */
    default <U extends @Nullable Object> LazyList<U> scanLeft(U zero, BiFunction<? super U, ? super T, ? extends U> operation) {
        // lazily streams the elements of an iterator
        return dev.zazr.collection.internal.Collections.scanLeft(this, zero, operation, Iterator::toLazyList);
    }

    // not lazy!
    /**
     * Produces a collection containing cumulative results of applying the operator from right to left.
     * <p>
     * The head of the result is the last cumulative result.
     * <p>
     * Complexity: O(n); the whole LazyList is computed now, because the fold starts at the end.
     *
     * @param <U>       the type of the resulting elements
     * @param zero      the initial value
     * @param operation a binary operator applied to each element and the intermediate result
     * @return a new LazyList containing the cumulative results
     * @throws NullPointerException if {@code operation} is null
     */
    default <U extends @Nullable Object> LazyList<U> scanRight(U zero, BiFunction<? super T, ? super U, ? extends U> operation) {
        return dev.zazr.collection.internal.Collections.scanRight(this, zero, operation, Iterator::toLazyList);
    }

    /**
     * The elements in a random order, drawn from a default source of randomness.
     * <p>
     * Complexity: O(n); the whole LazyList is computed now.
     *
     * @return a new LazyList, or this LazyList if it has fewer than two elements
     */
    default LazyList<T> shuffle() {
        return dev.zazr.collection.internal.Collections.shuffle(this, LazyList::ofAll);
    }

    /**
     * The elements from {@code beginIndex} inclusive to {@code endIndex} exclusive, both clamped to the bounds of
     * this LazyList.
     * <p>
     * Complexity: O(i); the first i + 1 elements are computed now, the rest up to index j when the result reaches them,
     * so it works on an infinite LazyList.
     *
     * @param beginIndex the first position
     * @param endIndex   the position after the last one
     * @return a new LazyList, empty if the range is empty
     */
    default LazyList<T> slice(int beginIndex, int endIndex) {
        final int lowerBound = Math.max(beginIndex, 0);
        if (lowerBound >= endIndex) {
            return empty();
        } else {
            // drop walks to the start in a loop; take is lazy past it
            return drop(lowerBound).take(endIndex - lowerBound);
        }
    }

    /**
     * The elements in ascending natural order (a stable sort).
     * <p>
     * Complexity: O(n log n) comparisons; the whole LazyList is computed now.
     *
     * @return a new sorted LazyList, or this LazyList if it is empty
     * @throws ClassCastException if {@code T} is not {@code Comparable}
     */
    default LazyList<T> sorted() {
        return isEmpty() ? this : stream().sorted().collect(LazyList.collector());
    }

    /**
     * The elements in the order of {@code comparator} (a stable sort).
     * <p>
     * Complexity: O(n log n) comparisons; the whole LazyList is computed now.
     *
     * @param comparator the order
     * @return a new sorted LazyList, or this LazyList if it is empty
     * @throws NullPointerException if {@code comparator} is null
     */
    default LazyList<T> sorted(Comparator<? super T> comparator) {
        Objects.requireNonNull(comparator, "comparator is null");
        return isEmpty() ? this : stream().sorted(comparator).collect(LazyList.collector());
    }

    /**
     * The elements sorted by the natural order of the key {@code mapper} computes (a stable sort).
     * <p>
     * Complexity: O(n log n) comparisons; the whole LazyList is computed now. The key is computed again at every
     * comparison.
     *
     * @param mapper computes the sort key
     * @param <U>    the key type
     * @return a new sorted LazyList, or this LazyList if it is empty
     * @throws NullPointerException if {@code mapper} is null
     */
    default <U extends Comparable<? super U>> LazyList<T> sortBy(Function<? super T, ? extends U> mapper) {
        return sortBy(U::compareTo, mapper);
    }

    /**
     * The elements sorted by {@code comparator} applied to the key {@code mapper} computes (a stable sort).
     * <p>
     * Complexity: O(n log n) comparisons; the whole LazyList is computed now. The key is computed again at every
     * comparison.
     *
     * @param comparator the order of the keys
     * @param mapper     computes the sort key
     * @param <U>        the key type
     * @return a new sorted LazyList, or this LazyList if it is empty
     * @throws NullPointerException if {@code comparator} or {@code mapper} is null
     */
    default <U extends @Nullable Object> LazyList<T> sortBy(Comparator<? super U> comparator, Function<? super T, ? extends U> mapper) {
        Objects.requireNonNull(comparator, "comparator is null");
        Objects.requireNonNull(mapper, "mapper is null");
        return sorted((e1, e2) -> comparator.compare(mapper.apply(e1), mapper.apply(e2)));
    }

    /**
     * Splits this {@code LazyList} into a prefix and remainder according to the given {@code predicate}.
     * <p>
     * The first element of the returned {@code Tuple} is the longest prefix of elements satisfying {@code predicate},
     * and the second element is the remaining elements.
     * <p>
     * Complexity: O(k) for a prefix of k elements; they and the first element after them are computed now, the rest of
     * the suffix when it is read.
     *
     * @param predicate a predicate used to determine the prefix
     * @return a {@code Tuple} containing the prefix and remainder
     * @throws NullPointerException if {@code predicate} is null
     */
    default Tuple2<LazyList<T>, LazyList<T>> span(Predicate<? super T> predicate) {
        Objects.requireNonNull(predicate, "predicate is null");
        return Tuple.of(takeWhile(predicate), dropWhile(predicate));
    }

    /**
     * This LazyList split in two at position {@code n}: the first {@code n} elements and the rest.
     * <p>
     * Complexity: O(k) for a split after k elements; the first k + 1 elements are computed now, the rest of the suffix
     * when it is read.
     *
     * @param n the position of the split
     * @return the prefix and the suffix
     */
    default Tuple2<LazyList<T>, LazyList<T>> splitAt(int n) {
        return Tuple.of(take(n), drop(n));
    }

    /**
     * This LazyList split in two before the first element satisfying {@code predicate}. If no element satisfies it, the
     * whole LazyList is the first part.
     * <p>
     * Complexity: O(k) for k elements before the split; they and the matching element are computed now, the rest of the
     * suffix when it is read.
     *
     * @param predicate the condition
     * @return the prefix and the suffix
     */
    default Tuple2<LazyList<T>, LazyList<T>> splitAt(Predicate<? super T> predicate) {
        Objects.requireNonNull(predicate, "predicate is null");
        return Tuple.of(takeWhile(predicate.negate()), dropWhile(predicate.negate()));
    }

    /**
     * This LazyList split in two after the first element satisfying {@code predicate}. If no element satisfies it, the
     * whole LazyList is the first part.
     * <p>
     * Complexity: O(k) for k elements up to and including the match; they and the element after the match are computed
     * now, the rest of the suffix when it is read.
     *
     * @param predicate the condition
     * @return the prefix including the matching element, and the suffix
     */
    default Tuple2<LazyList<T>, LazyList<T>> splitAtInclusive(Predicate<? super T> predicate) {
        final Tuple2<LazyList<T>, LazyList<T>> split = splitAt(predicate);
        if (split._2().isEmpty()) {
            return split;
        } else {
            return Tuple.of(split._1().append(split._2().head()), split._2().tail());
        }
    }

    /**
     * The elements from {@code beginIndex} on.
     * <p>
     * Complexity: O(i); the first i + 1 elements are computed now, the rest when the result reaches them.
     *
     * @param beginIndex the first position
     * @return a new LazyList
     * @throws IndexOutOfBoundsException if {@code beginIndex} is negative or greater than {@code size()}
     */
    default LazyList<T> subSequence(int beginIndex) {
        if (beginIndex < 0) {
            throw new IndexOutOfBoundsException("subSequence(" + beginIndex + ")");
        }
        LazyList<T> result = this;
        for (int i = 0; i < beginIndex; i++, result = result.tail()) {
            if (result.isEmpty()) {
                throw new IndexOutOfBoundsException("subSequence(" + beginIndex + ") on LazyList of size " + i);
            }
        }
        return result;
    }

    /**
     * The elements from {@code beginIndex} inclusive to {@code endIndex} exclusive.
     * <p>
     * Complexity: O(i); the first i + 1 elements are computed now, the rest up to index j when the result reaches them.
     * An empty range computes its first i elements too, to check that it is within this LazyList, and a reversed range
     * its first j.
     * <p>
     * The bounds are those of {@link Vector#subSequence(int, int)}: {@code IndexOutOfBoundsException} when
     * {@code beginIndex < 0} or {@code endIndex > size()}, otherwise {@code IllegalArgumentException} when
     * {@code beginIndex > endIndex}. Every such call throws when it is made, with one exception: because
     * {@code LazyList} is lazy, when {@code beginIndex < size() < endIndex} the {@code IndexOutOfBoundsException} is
     * thrown once the returned LazyList is traversed past its last element, not when this method is called.
     *
     * @param beginIndex the first position
     * @param endIndex   the position after the last one
     * @return a new LazyList
     * @throws IndexOutOfBoundsException if {@code beginIndex} is negative; if {@code endIndex} is past the end and
     *                                   {@code beginIndex} is not before the end, a reversed range included; or, when
     *                                   {@code beginIndex < size() < endIndex}, once the traversal passes the end
     * @throws IllegalArgumentException  if {@code beginIndex} is greater than {@code endIndex} and {@code endIndex} is
     *                                   within this LazyList
     */
    default LazyList<T> subSequence(int beginIndex, int endIndex) {
        if (beginIndex < 0) {
            throw new IndexOutOfBoundsException("subSequence(" + beginIndex + ", " + endIndex + ")");
        }
        if (beginIndex > endIndex) {
            // as in Vector, an end past the end of this LazyList is reported before the reversed range
            if (!hasAtLeast(this, endIndex)) {
                throw new IndexOutOfBoundsException("subSequence of Nil");
            }
            throw new IllegalArgumentException("subSequence(" + beginIndex + ", " + endIndex + ")");
        }
        if (beginIndex == endIndex) {
            if (!hasAtLeast(this, beginIndex)) {
                throw new IndexOutOfBoundsException("subSequence of Nil");
            }
            return Empty.instance();
        }
        LazyList<T> start = this;
        for (int i = 0; i < beginIndex && !start.isEmpty(); i++) {
            start = start.tail();
        }
        if (start.isEmpty()) {
            throw new IndexOutOfBoundsException("subSequence of Nil");
        }
        return takeExactly(start, endIndex - beginIndex);
    }

    // Whether stream has at least n elements; forces at most its first n.
    private static <T extends @Nullable Object> boolean hasAtLeast(LazyList<T> stream, int n) {
        return n <= 0 || !stream.drop(n - 1).isEmpty();
    }

    // The first n > 0 elements of a non-empty stream, lazily; throws once the traversal passes the end of the stream.
    private static <T extends @Nullable Object> LazyList<T> takeExactly(LazyList<T> stream, int n) {
        if (n == 1) {
            return cons(stream.head(), LazyList::empty);
        } else {
            return cons(stream.head(), () -> {
                final LazyList<T> tail = stream.tail();
                if (tail.isEmpty()) {
                    throw new IndexOutOfBoundsException("subSequence of Nil");
                }
                return takeExactly(tail, n - 1);
            });
        }
    }

    /**
     * Returns a new {@code LazyList} without its first element.
     * <p>
     * Complexity: O(k) for k elements computed or rebuilt. On a LazyList returned by filter, reject, retainAll,
     * removeAll, distinct, distinctBy, collect or flatMap, the first call computes the elements up to the next one
     * kept, and never returns on an infinite LazyList with no further match; later calls are O(1): the result is kept.
     * On a LazyList built by append, a call at the first appended element rebuilds the k appended elements, and nothing
     * is kept, so every such call pays it again. O(1) otherwise.
     *
     * @return a new {@code LazyList} containing all elements except the first
     * @throws UnsupportedOperationException if this {@code LazyList} is empty
     */
    LazyList<T> tail();

    /**
     * Returns a new {@code LazyList} without its first element as an {@code Option}.
     * <p>
     * Complexity: O(k) for k elements computed or rebuilt, as {@link #tail()}.
     *
     * @return {@code Some(traversable)} if non-empty, otherwise {@code None}
     */
    default Option<LazyList<T>> tailOption() {
        return isEmpty() ? Option.none() : Option.some(tail());
    }

    /**
     * Returns the first {@code n} elements of this {@code LazyList}, or all elements if {@code n} exceeds the length.
     * <p>
     * If {@code n < 0}, an empty instance is returned. If {@code n > size()}, the full instance is returned.
     * <p>
     * Complexity: lazy; nothing is computed now, each element when the result reaches it.
     *
     * @param n the number of elements to take
     * @return a new {@code LazyList} containing the first {@code n} elements
     */
    default LazyList<T> take(int n) {
        if (n < 1 || isEmpty()) {
            return empty();
        } else if (n == 1) {
            return cons(head(), LazyList::empty);
        } else {
            return cons(head(), () -> tail().take(n - 1));
        }
    }

    /**
     * Takes elements from this {@code LazyList} until the given predicate holds for an element.
     * <p>
     * Equivalent to {@code takeWhile(predicate.negate())}, but useful when using method references
     * that cannot be negated directly.
     * <p>
     * Complexity: lazy; nothing is computed now, and each element is tested when the result reaches it.
     *
     * @param predicate a condition tested sequentially on the elements
     * @return a new {@code LazyList} containing all elements before the first one that satisfies the predicate
     * @throws NullPointerException if {@code predicate} is null
     */
    default LazyList<T> takeUntil(Predicate<? super T> predicate) {
        Objects.requireNonNull(predicate, "predicate is null");
        return takeWhile(predicate.negate());
    }

    /**
     * Takes elements from this {@code LazyList} while the given predicate holds.
     * <p>
     * Complexity: lazy; nothing is computed now, and each element is tested when the result reaches it.
     *
     * @param predicate a condition tested sequentially on the elements
     * @return a new {@code LazyList} containing all elements up to (but not including) the first one
     *         that does not satisfy the predicate
     * @throws NullPointerException if {@code predicate} is null
     */
    default LazyList<T> takeWhile(Predicate<? super T> predicate) {
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
     * Returns the last {@code n} elements of this {@code LazyList}, or all elements if {@code n} exceeds the length.
     * <p>
     * If {@code n < 0}, an empty instance is returned. If {@code n > size()}, the full instance is returned.
     * <p>
     * Complexity: O(n); the whole LazyList is computed now, because the last elements are found by walking to the end.
     *
     * @param n the number of elements to take from the end
     * @return a new {@code LazyList} containing the last {@code n} elements
     */
    default LazyList<T> takeRight(int n) {
        LazyList<T> right = this;
        LazyList<T> remaining = drop(n);
        while (!remaining.isEmpty()) {
            right = right.tail();
            remaining = remaining.tail();
        }
        return right;
    }

    /**
     * The longest suffix whose elements, from the end, do not satisfy {@code predicate}.
     * <p>
     * Complexity: O(n); the whole LazyList is computed now, because the last matching element decides.
     *
     * @param predicate the condition, tested from the end
     * @return a new LazyList
     * @throws NullPointerException if {@code predicate} is null
     */
    default LazyList<T> takeRightUntil(Predicate<? super T> predicate) {
        Objects.requireNonNull(predicate, "predicate is null");
        return reverse().takeUntil(predicate).reverse();
    }

    /**
     * The longest suffix whose elements, from the end, all satisfy {@code predicate}.
     * <p>
     * Complexity: O(n); the whole LazyList is computed now, because the last matching element decides.
     *
     * @param predicate the condition, tested from the end
     * @return a new LazyList
     * @throws NullPointerException if {@code predicate} is null
     */
    default LazyList<T> takeRightWhile(Predicate<? super T> predicate) {
        Objects.requireNonNull(predicate, "predicate is null");
        return takeRightUntil(predicate.negate());
    }

    /**
     * Splits every element in two with {@code unzipper}: the first parts, and the second parts, each in order.
     * <p>
     * Complexity: lazy; {@code unzipper} runs on the first element now, and once on each other one, when either side
     * reaches it.
     *
     * @param unzipper splits an element
     * @param <T1>     the type of the first parts
     * @param <T2>     the type of the second parts
     * @return the first parts and the second parts
     * @throws NullPointerException if {@code unzipper} is null
     */
    default <T1 extends @Nullable Object, T2 extends @Nullable Object> Tuple2<LazyList<T1>, LazyList<T2>> unzip(
      Function<? super T, Tuple2<? extends T1, ? extends T2>> unzipper) {
        Objects.requireNonNull(unzipper, "unzipper is null");
        final LazyList<Tuple2<? extends T1, ? extends T2>> stream = map(element -> Objects.requireNonNull(unzipper.apply(element), "LazyList.unzip: unzipper returned null"));
        final LazyList<T1> stream1 = stream.map(t -> t._1());
        final LazyList<T2> stream2 = stream.map(t -> t._2());
        return Tuple.of(stream1, stream2);
    }

    /**
     * Splits every element in three with {@code unzipper}: the first, the second and the third parts, each in order.
     * <p>
     * Complexity: lazy; {@code unzipper} runs on the first element now, and once on each other one, when a side reaches
     * it.
     *
     * @param unzipper splits an element
     * @param <T1>     the type of the first parts
     * @param <T2>     the type of the second parts
     * @param <T3>     the type of the third parts
     * @return the first, the second and the third parts
     * @throws NullPointerException if {@code unzipper} is null
     */
    default <T1 extends @Nullable Object, T2 extends @Nullable Object, T3 extends @Nullable Object> Tuple3<LazyList<T1>, LazyList<T2>, LazyList<T3>> unzip3(
      Function<? super T, Tuple3<? extends T1, ? extends T2, ? extends T3>> unzipper) {
        Objects.requireNonNull(unzipper, "unzipper is null");
        final LazyList<Tuple3<? extends T1, ? extends T2, ? extends T3>> stream = map(element -> Objects.requireNonNull(unzipper.apply(element), "LazyList.unzip3: unzipper returned null"));
        final LazyList<T1> stream1 = stream.map(t -> t._1());
        final LazyList<T2> stream2 = stream.map(t -> t._2());
        final LazyList<T3> stream3 = stream.map(t -> t._3());
        return Tuple.of(stream1, stream2, stream3);
    }

    /**
     * This LazyList with the element at {@code index} replaced by {@code element}.
     * <p>
     * Complexity: O(i); the first i + 2 elements are computed now (the one after the replaced element too). The result
     * joins its two parts as {@link #appendAll(Iterable)} does, so each update adds one step to reading the elements
     * after index i.
     *
     * @param index   the position to update
     * @param element the new element
     * @return a new LazyList
     * @throws IndexOutOfBoundsException if {@code index} is negative or not less than {@code size()}
     */
    default LazyList<T> update(int index, T element) {
        if (isEmpty()) {
            throw new IndexOutOfBoundsException("update(" + index + ", e) on Nil");
        }
        if (index < 0) {
            throw new IndexOutOfBoundsException("update(" + index + ", e)");
        }
        LazyList<T> preceding = Empty.instance();
        LazyList<T> tail = this;
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
     * This LazyList with the element at {@code index} replaced by what {@code updater} computes from it.
     * <p>
     * Complexity: O(i), as {@link #update(int, Object)}, after one {@link #get(int)}.
     *
     * @param index   the position to update
     * @param updater computes the new element from the current one
     * @return a new LazyList
     * @throws IndexOutOfBoundsException if {@code index} is negative or not less than {@code size()}
     * @throws NullPointerException      if {@code updater} is null
     */
    default LazyList<T> update(int index, Function<? super T, ? extends T> updater) {
        Objects.requireNonNull(updater, "updater is null");
        return update(index, updater.apply(get(index)));
    }

    /**
     * Returns a {@code LazyList} formed by pairing elements of this {@code LazyList} with elements of another
     * {@code Iterable}. Pairing stops when either collection runs out of elements; any remaining elements in the longer
     * collection are ignored.
     * <p>
     * The length of the resulting {@code LazyList} is the minimum of the lengths of this {@code LazyList} and
     * {@code that}.
     * <p>
     * Complexity: lazy; nothing is computed now, and reading every pair costs O(min(n, m)).
     *
     * @param <U>  the type of elements in the second half of each pair
     * @param that an {@code Iterable} providing the second element of each pair
     * @return a new {@code LazyList} containing pairs of corresponding elements
     * @throws NullPointerException if {@code that} is null
     */
    default <U extends @Nullable Object> LazyList<Tuple2<T, U>> zip(Iterable<? extends U> that) {
        return zipWith(that, Tuple::of);
    }

    /**
     * Returns a {@code LazyList} by combining elements of this {@code LazyList} with elements of another
     * {@code Iterable} using a mapping function. Pairing stops when either collection runs out of elements.
     * <p>
     * The length of the resulting {@code LazyList} is the minimum of the lengths of this {@code LazyList} and
     * {@code that}.
     * <p>
     * Complexity: lazy; nothing is computed now, and reading every result costs O(min(n, m)).
     *
     * @param <U>    the type of elements in the second parameter of the mapper
     * @param <R>    the type of elements in the resulting {@code LazyList}
     * @param that   an {@code Iterable} providing the second parameter of the mapper
     * @param mapper a function that combines elements from this and {@code that} into a new element
     * @return a new {@code LazyList} containing mapped elements
     * @throws NullPointerException if {@code that} or {@code mapper} is null
     */
    default <U extends @Nullable Object, R extends @Nullable Object> LazyList<R> zipWith(Iterable<? extends U> that, BiFunction<? super T, ? super U, ? extends R> mapper) {
        Objects.requireNonNull(that, "that is null");
        Objects.requireNonNull(mapper, "mapper is null");
        return LazyList.ofAll(Iterator.ofAll(this).zipWith(that, mapper));
    }

    /**
     * Returns a {@code LazyList} formed by pairing elements of this {@code LazyList} with elements of another
     * {@code Iterable}, filling in placeholder elements when one collection is shorter than the other.
     * <p>
     * The length of the resulting {@code LazyList} is the maximum of the lengths of this {@code LazyList} and
     * {@code that}.
     * <p>
     * If this {@code LazyList} is shorter than {@code that}, {@code thisElem} is used as a filler. Conversely, if
     * {@code that} is shorter, {@code thatElem} is used.
     * <p>
     * Complexity: lazy; nothing is computed now, and reading every pair costs O(max(n, m)).
     *
     * @param <U>      the type of elements in the second half of each pair
     * @param iterable an {@code Iterable} providing the second element of each pair
     * @param thisElem the element used to fill missing values if this {@code LazyList} is shorter than {@code iterable}
     * @param thatElem the element used to fill missing values if {@code iterable} is shorter than this {@code LazyList}
     * @return a new {@code LazyList} containing pairs of elements, including fillers as needed
     * @throws NullPointerException if {@code iterable} is null
     */
    default <U extends @Nullable Object> LazyList<Tuple2<T, U>> zipAll(Iterable<? extends U> iterable, T thisElem, U thatElem) {
        Objects.requireNonNull(iterable, "iterable is null");
        return LazyList.ofAll(Iterator.ofAll(this).zipAll(iterable, thisElem, thatElem));
    }

    /**
     * Zips this {@code LazyList} with its indices, starting at 0.
     * <p>
     * Complexity: lazy; nothing is computed now, each element when the result reaches it.
     *
     * @return a new {@code LazyList} containing each element paired with its index
     */
    default LazyList<Tuple2<T, Integer>> zipWithIndex() {
        return zipWithIndex(Tuple::of);
    }

    /**
     * Zips this {@code LazyList} with its indices and maps the resulting pairs using the provided mapper.
     * <p>
     * Complexity: lazy; nothing is computed now, each element when the result reaches it.
     *
     * @param <U>    the type of elements in the resulting {@code LazyList}
     * @param mapper a function mapping an element and its index to a new element
     * @return a new {@code LazyList} containing the mapped elements
     * @throws NullPointerException if {@code mapper} is null
     */
    default <U extends @Nullable Object> LazyList<U> zipWithIndex(BiFunction<? super T, ? super Integer, ? extends U> mapper) {
        Objects.requireNonNull(mapper, "mapper is null");
        return LazyList.ofAll(Iterator.ofAll(this).zipWithIndex(mapper));
    }

    /**
     * Extends (continues) this {@code LazyList} with a constantly repeated value.
     * <p>
     * Complexity: O(1); nothing is computed now, and the result is infinite. On a LazyList built by
     * {@link #append(Object)}, or a tail of one, the call never returns: it reads the infinite extension now, as
     * {@link #appendAll(Iterable)} does.
     *
     * @param next value with which the lazy list should be extended
     * @return new {@code LazyList} composed from this lazy list extended with a LazyList of provided value
     */
    default LazyList<T> extend(T next) {
        return LazyList.ofAll(this.appendAll(LazyList.continually(next)));
    }

    /**
     * Extends (continues) this {@code LazyList} with values provided by a {@code Supplier}
     * <p>
     * Complexity: O(1); nothing is computed now, and the result is infinite. On a LazyList built by
     * {@link #append(Object)}, or a tail of one, the call never returns: it reads the infinite extension now, as
     * {@link #appendAll(Iterable)} does.
     *
     * @param nextSupplier a supplier which will provide values for extending a lazy list
     * @return new {@code LazyList} composed from this lazy list extended with values provided by the supplier
     */
    default LazyList<T> extend(Supplier<? extends T> nextSupplier) {
        Objects.requireNonNull(nextSupplier, "nextSupplier is null");
        return LazyList.ofAll(appendAll(LazyList.continually(nextSupplier)));
    }

    /**
     * Extends (continues) this {@code LazyList} with a LazyList of values created by applying
     * consecutively provided {@code Function} to the last element of the original LazyList.
     * <p>
     * If this LazyList is empty, it is returned unchanged (there is no last element to seed the
     * function); use {@link #extend(Object)} or {@link #extend(Supplier)} to extend an empty LazyList.
     * <p>
     * Complexity: O(1); the result reads one element ahead of what it returns, so the first two elements are computed
     * now. The result is infinite.
     *
     * @param nextFunction a function which calculates the next value based on the previous value
     * @return new {@code LazyList} composed from this lazy list extended with values calculated by the provided function
     */
    default LazyList<T> extend(Function<? super T, ? extends T> nextFunction) {
        Objects.requireNonNull(nextFunction, "nextFunction is null");
        if (isEmpty()) {
            return this;
        } else {
            final LazyList<T> that = this;
            return LazyList.ofAll(new AbstractIterator<T>() {

                LazyList<T> stream = that;
                @Nullable T last = null;

                @Override
                // `stream` is non-empty on entry, so `last` is always assigned before it is read.
                @SuppressWarnings("NullAway")
                protected T getNext() {
                    if (stream.isEmpty()) {
                        stream = LazyList.iterate(nextFunction.apply(last), nextFunction);
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
     * The empty LazyList.
     * <p>
     * This is a singleton, i.e. not Cloneable.
     *
     * @param <T> Component type of the LazyList.
     */
    final class Empty<T extends @Nullable Object> implements LazyList<T> {

        private static final Empty<?> INSTANCE = new Empty<>();

        // hidden
        private Empty() {
        }

        /**
         * Returns the singleton empty LazyList instance.
         *
         * @param <T> Component type of the LazyList
         * @return The empty LazyList
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
        public LazyList<T> tail() {
            throw new UnsupportedOperationException("tail of empty stream");
        }

        @Override
        public boolean equals(@Nullable Object o) {
            return dev.zazr.collection.internal.Collections.equals(this, o);
        }

        @Override
        public int hashCode() {
            return dev.zazr.collection.internal.Collections.hashOrdered(this);
        }

        @Override
        public String toString() {
            return "LazyList()";
        }

    }

    /**
     * Non-empty {@code LazyList}, consisting of a {@code head}, and {@code tail}.
     *
     * @param <T> Component type of the LazyList.
     */
    abstract class Cons<T extends @Nullable Object> implements LazyList<T> {

        final T head;
        final Lazy<LazyList<T>> tail;

        Cons(T head, Supplier<LazyList<T>> tail) {
            this(head, Lazy.of(Objects.requireNonNull(tail, "tail is null")));
        }

        // shares an already memoized tail instead of wrapping it in a second Lazy
        Cons(T head, Lazy<LazyList<T>> tail) {
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
            return new LazyListIterator<>(this);
        }

        @Override
        public boolean equals(@Nullable Object o) {
            return dev.zazr.collection.internal.Collections.equals(this, o);
        }

        @Override
        public int hashCode() {
            return dev.zazr.collection.internal.Collections.hashOrdered(this);
        }

        @Override
        public String toString() {
            final StringBuilder builder = new StringBuilder("LazyList(");
            LazyList<T> stream = this;
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

            ConsImpl(T head, Supplier<LazyList<T>> tail) {
                super(head, tail);
            }

            @Override
            public LazyList<T> tail() {
                return Objects.requireNonNull(tail.get(), "LazyList.cons: tailSupplier returned null");
            }

        }

        private static final class AppendElements<T extends @Nullable Object> extends Cons<T> {

            private final dev.zazr.collection.Queue<T> queue;

            AppendElements(T head, dev.zazr.collection.Queue<T> queue, Supplier<LazyList<T>> tail) {
                this(head, queue, Lazy.of(tail));
            }

            AppendElements(T head, dev.zazr.collection.Queue<T> queue, Lazy<LazyList<T>> tail) {
                super(head, tail);
                this.queue = queue;
            }

            @Override
            public LazyList<T> append(T element) {
                return new AppendElements<>(head, queue.append(element), tail);
            }

            @Override
            public LazyList<T> appendAll(Iterable<? extends T> elements) {
                Objects.requireNonNull(elements, "elements is null");
                return isEmpty() ? LazyList.ofAll(queue) : new AppendElements<>(head, queue.appendAll(elements), tail);
            }

            @Override
            public LazyList<T> tail() {
                final LazyList<T> t = tail.get();
                if (t.isEmpty()) {
                    return LazyList.ofAll(queue);
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
     * Complexity: O(1): the first element is always already computed.
     *
     * @return the head of this LazyList
     * @throws NoSuchElementException if this LazyList is empty
     */
    T head();

    // -- windows and products

    /**
     * The elements in consecutive blocks of {@code size}: {@code LazyList.of(1, 2, 3, 4, 5).grouped(2)} is
     * {@code LazyList(LazyList(1, 2), LazyList(3, 4), LazyList(5))}; the last block is smaller when {@code size} does not
     * divide the length. The same as {@code sliding(size, size)}.
     * <p>
     * Complexity: lazy; nothing is computed now, and reading every block costs O(n). Moving to the next block computes
     * the elements of the current one and one more, so an infinite LazyList can be grouped.
     *
     * @param size the block size, positive
     * @return the blocks, in order; empty if this LazyList is empty
     * @throws IllegalArgumentException if {@code size} is not positive
     */
    default LazyList<LazyList<T>> grouped(int size) {
        return sliding(size, size);
    }

    /**
     * The windows of {@code size} consecutive elements, each starting one element after the previous:
     * {@code LazyList.of(1, 2, 3, 4).sliding(3)} is {@code LazyList(LazyList(1, 2, 3), LazyList(2, 3, 4))}. A LazyList
     * shorter than {@code size} is one window. The same as {@code sliding(size, 1)}.
     * <p>
     * Complexity: lazy; nothing is computed now, and reading every window costs O(n * size). Moving to the next window
     * computes the elements of the current one and one more, so an infinite LazyList can be windowed.
     *
     * @param size the window size, positive
     * @return the windows, in order; empty if this LazyList is empty
     * @throws IllegalArgumentException if {@code size} is not positive
     */
    default LazyList<LazyList<T>> sliding(int size) {
        return sliding(size, 1);
    }

    /**
     * The windows of {@code size} consecutive elements, each starting {@code step} elements after the previous:
     * {@code LazyList.of(1, 2, 3, 4, 5).sliding(2, 3)} is {@code LazyList(LazyList(1, 2), LazyList(4, 5))} and
     * {@code sliding(2, 4)} is {@code LazyList(LazyList(1, 2), LazyList(5))}. The last window is shorter than
     * {@code size} when it reaches the end; a window whose elements all belong to the previous one is not
     * produced, so {@code LazyList.of(1, 2, 3, 4).sliding(3)} has two windows. A LazyList shorter than {@code size}
     * is one window; an empty LazyList has none.
     * <p>
     * Complexity: lazy; nothing is computed now, and reading every window costs O(n + (n / step) * size). Moving to the
     * next window computes the elements up to max(size, step) positions after the start of the current one, so an
     * infinite LazyList can be windowed.
     *
     * @param size the window size, positive
     * @param step the distance between two window starts, positive
     * @return the windows, in order
     * @throws IllegalArgumentException if {@code size} or {@code step} is not positive
     */
    default LazyList<LazyList<T>> sliding(int size, int step) {
        dev.zazr.collection.internal.Collections.checkWindow(size, step);
        return isEmpty() ? empty() : Windows.apply(this, size, step);
    }

    /**
     * The elements in maximal runs of consecutive elements with the same key, computed once per element by
     * {@code classifier}: {@code LazyList.of(1, 2, 3, 10, 12, 5, 7, 20, 29).slideBy(x -> x / 10)} is
     * {@code LazyList(LazyList(1, 2, 3), LazyList(10, 12), LazyList(5, 7), LazyList(20, 29))}. The runs concatenate back
     * to this LazyList.
     * <p>
     * Complexity: lazy; the first run is computed now, with the first element of the next one; each further run when
     * the result reaches it.
     *
     * @param classifier the key of an element; two consecutive elements are in the same run when their keys are
     *                   equal
     * @return the runs, in order; empty if this LazyList is empty
     * @throws NullPointerException if {@code classifier} is null
     */
    default LazyList<LazyList<T>> slideBy(Function<? super T, ?> classifier) {
        Objects.requireNonNull(classifier, "classifier is null");
        return LazyList.ofAll(Iterator.ofAll(this).slideBy(classifier).map(LazyList::ofAll));
    }

    /**
     * The Cartesian square of this LazyList: every pair {@code (a, b)} of elements, {@code a} varying slowest.
     * <p>
     * Complexity: lazy; nothing is computed now, and reading every pair costs O(n^2).
     *
     * @return the pairs
     */
    default LazyList<Tuple2<T, T>> crossProduct() {
        return crossProduct(this);
    }

    /**
     * The Cartesian power of this LazyList: every LazyList of {@code power} elements drawn from this one, in
     * lexicographic position order. {@code power == 0} gives one empty LazyList; a negative power gives no result.
     * <p>
     * Complexity: lazy; nothing is computed now, and reading the n^power results of {@code power} elements each costs
     * O(power * n^power).
     *
     * @param power the size of each result
     * @return the LazyLists
     */
    default LazyList<LazyList<T>> crossProduct(int power) {
        if (power < 0) {
            return empty();
        }
        LazyList<LazyList<T>> product = LazyList.of(LazyList.<T> empty());
        for (int i = 0; i < power; i++) {
            product = product.flatMap(el -> map(el::append));
        }
        return product;
    }

    /**
     * The Cartesian product of this LazyList and {@code that}: every pair {@code (a, b)} with {@code a} from this
     * LazyList and {@code b} from {@code that}, {@code a} varying slowest. {@code that} is read lazily and
     * each of its elements kept once read, so an infinite {@code that} works with {@code take}.
     * <p>
     * Complexity: lazy; nothing is computed now but the first element of {@code that}, and reading every pair costs O(n
     * * m). An empty {@code that} computes the whole LazyList now, so it never returns on an infinite one.
     *
     * @param that the right-hand elements
     * @param <U>  their type
     * @return the pairs
     * @throws NullPointerException if {@code that} is null
     */
    default <U extends @Nullable Object> LazyList<Tuple2<T, U>> crossProduct(Iterable<? extends U> that) {
        Objects.requireNonNull(that, "that is null");
        // a lazy, memoising LazyList: the result is lazy, so the argument stays lazy too
        final LazyList<U> other = LazyList.ofAll(that);
        return flatMap(a -> other.map(b -> Tuple.of(a, b)));
    }

    /**
     * Combines the elements from the right: the last with the one before it, the result with the one before that,
     * and so on.
     * <p>
     * Complexity: O(n); the whole LazyList is computed now and copied in reverse.
     *
     * @param op combines the next element and the result so far
     * @return the combined result
     * @throws NoSuchElementException if this LazyList is empty
     * @throws NullPointerException   if {@code op} is null
     */
    default T reduceRight(BiFunction<? super T, ? super T, ? extends T> op) {
        Objects.requireNonNull(op, "op is null");
        if (isEmpty()) {
            throw new NoSuchElementException("reduceRight on empty LazyList");
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
     * <p>
     * Complexity: O(n), every element compared once; the whole LazyList is computed.
     *
     * @return {@code Some(maximum)} if there is an element, {@code None} otherwise
     * @throws ClassCastException if two or more elements are not {@code Comparable}
     */
    default Option<T> max() {
        return TraversableModule.max(this);
    }

    /**
     * The greatest element according to {@code comparator}; of equal greatest elements, the first in this
     * LazyList's order.
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
     * element in this LazyList's order.
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
     * <p>
     * Complexity: O(n), every element compared once; the whole LazyList is computed.
     *
     * @return {@code Some(minimum)} if there is an element, {@code None} otherwise
     * @throws ClassCastException if two or more elements are not {@code Comparable}
     */
    default Option<T> min() {
        return TraversableModule.min(this);
    }

    /**
     * The least element according to {@code comparator}; of equal least elements, the first in this LazyList's
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
     * this LazyList's order.
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
     * @return the folded result, {@code zero} on an empty LazyList
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
     * @throws NoSuchElementException if this LazyList is empty
     * @throws NullPointerException   if {@code op} is null
     */
    default T reduce(BiFunction<? super T, ? super T, ? extends T> op) {
        return TraversableModule.reduceLeft(this, op);
    }

    /**
     * {@link #reduce(BiFunction)} as an {@code Option}: {@code None} on an empty LazyList.
     *
     * @param op combines two elements
     * @return {@code Some(result)}, or {@code None} if this LazyList is empty
     * @throws NullPointerException if {@code op} is null
     */
    default Option<T> reduceOption(BiFunction<? super T, ? super T, ? extends T> op) {
        return TraversableModule.reduceLeftOption(this, op);
    }

    /**
     * The only element.
     *
     * @return the element
     * @throws NoSuchElementException if this LazyList is empty or has more than one element
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
     * <p>
     * Complexity: O(n), as {@link #groupBy(Function)}; the whole LazyList is computed now.
     *
     * @param getKey the key of an element
     * @param <K>  the key type
     * @return {@code Some(map)} if the keys are unique, {@code None} otherwise
     * @throws NullPointerException if {@code getKey} is null
     */
    default <K extends @Nullable Object> Option<Map<K, T>> arrangeBy(Function<? super T, ? extends K> getKey) {
        Objects.requireNonNull(getKey, "getKey is null");
        return TraversableModule.arrangeBy(groupBy(element -> Objects.requireNonNull(getKey.apply(element), "LazyList.arrangeBy: getKey returned null")));
    }

    /**
     * The sum of the elements, which must be {@link Number}s: {@code Byte}, {@code Short}, {@code Integer} and
     * {@code Long} are summed as a {@code long}, {@code BigInteger} and {@code BigDecimal} with their own
     * arithmetic, any other {@code Number} as a {@code double} with Neumaier compensation. The arithmetic is chosen
     * from the first element. {@code 0} on an empty LazyList.
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
     * {@code 1} on an empty LazyList.
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
     * Complexity: O(n); every element is tested, so the whole LazyList is computed.
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
     * Complexity: O(1), as {@link #head()}.
     *
     * @return {@code Some(head)}, or {@code None} if this LazyList is empty
     */
    default Option<T> headOption() {
        return isEmpty() ? Option.none() : Option.some(head());
    }

    /**
     * The last element as an {@code Option}.
     * <p>
     * Complexity: O(n), as {@link #last()}.
     *
     * @return {@code Some(last)}, or {@code None} if this LazyList is empty
     */
    default Option<T> lastOption() {
        return isEmpty() ? Option.none() : Option.some(last());
    }

    /**
     * Combines the elements from the left: the first with the second, the result with the third, and so on.
     * <p>
     * Complexity: O(n); the whole LazyList is computed.
     *
     * @param op combines the result so far and the next element
     * @return the combined result
     * @throws NoSuchElementException if this LazyList is empty
     * @throws NullPointerException   if {@code op} is null
     */
    default T reduceLeft(BiFunction<? super T, ? super T, ? extends T> op) {
        return TraversableModule.reduceLeft(this, op);
    }

    /**
     * {@link #reduceLeft(BiFunction)} as an {@code Option}: {@code None} on an empty LazyList.
     *
     * @param op combines the result so far and the next element
     * @return {@code Some(result)}, or {@code None} if this LazyList is empty
     * @throws NullPointerException if {@code op} is null
     */
    default Option<T> reduceLeftOption(BiFunction<? super T, ? super T, ? extends T> op) {
        return TraversableModule.reduceLeftOption(this, op);
    }

    /**
     * {@link #reduceRight(BiFunction)} as an {@code Option}: {@code None} on an empty LazyList.
     *
     * @param op combines the next element and the result so far
     * @return {@code Some(result)}, or {@code None} if this LazyList is empty
     * @throws NullPointerException if {@code op} is null
     */
    default Option<T> reduceRightOption(BiFunction<? super T, ? super T, ? extends T> op) {
        Objects.requireNonNull(op, "op is null");
        return isEmpty() ? Option.none() : Option.some(reduceRight(op));
    }

    /**
     * The number of elements.
     * <p>
     * Complexity: O(n): the whole LazyList is computed and walked at each call, so it never returns on an infinite
     * LazyList.
     *
     * @return the number of elements
     */
    @Override
    default int size() {
        return foldLeft(0, (n, ignored) -> n + 1);
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
     * The elements as the entries of a new {@link HashMap}, each mapped to a key by {@code keyMapper} and to a
     * value by {@code valueMapper}; of two entries with the same key, the later one in this LazyList's order wins.
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
     * entries with the same key, the later one in this LazyList's order wins.
     *
     * @param f   the entry an element becomes
     * @param <K> the key type
     * @param <V> the value type
     * @return the new map
     * @throws NullPointerException if {@code f} is null or returns null
     */
    default <K extends @Nullable Object, V extends @Nullable Object> Map<K, V> toMap(Function<? super T, ? extends Tuple2<? extends K, ? extends V>> f) {
        final Function<Iterable<Tuple2<? extends K, ? extends V>>, Map<K, V>> ofAll = HashMap::ofEntries;
        return TraversableModule.toMap(this, HashMap.empty(), ofAll, f, "LazyList.toMap: f returned null");
    }

    /**
     * The elements as the entries of a new {@link LinkedHashMap}, in this LazyList's order, each mapped to a key by
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
     * The elements as the entries of a new {@link LinkedHashMap}, in this LazyList's order, each mapped to a key
     * and a value by {@code f}; of two entries with the same key, the later one wins the value and the earlier one
     * the position.
     *
     * @param f   the entry an element becomes
     * @param <K> the key type
     * @param <V> the value type
     * @return the new map
     * @throws NullPointerException if {@code f} is null or returns null
     */
    default <K extends @Nullable Object, V extends @Nullable Object> Map<K, V> toLinkedMap(Function<? super T, ? extends Tuple2<? extends K, ? extends V>> f) {
        final Function<Iterable<Tuple2<? extends K, ? extends V>>, Map<K, V>> ofAll = LinkedHashMap::ofEntries;
        return TraversableModule.toMap(this, LinkedHashMap.empty(), ofAll, f, "LazyList.toLinkedMap: f returned null");
    }

    /**
     * The elements as the entries of a new {@link TreeMap} in the natural order of the keys, each mapped to a key
     * by {@code keyMapper} and to a value by {@code valueMapper}; of two entries with the same key, the later one
     * in this LazyList's order wins.
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
     * and a value by {@code f}; of two entries with the same key, the later one in this LazyList's order wins.
     *
     * @param f   the entry an element becomes
     * @param <K> the key type
     * @param <V> the value type
     * @return the new map
     * @throws NullPointerException if {@code f} is null or returns null
     */
    default <K extends Comparable<? super K>, V extends @Nullable Object> SortedMap<K, V> toSortedMap(Function<? super T, ? extends Tuple2<? extends K, ? extends V>> f) {
        Objects.requireNonNull(f, "f is null");
        return toSortedMap(Comparator.naturalOrder(), f);
    }

    /**
     * The elements as the entries of a new {@link TreeMap} ordered by {@code comparator}, each mapped to a key by
     * {@code keyMapper} and to a value by {@code valueMapper}; of two entries with the same key, the later one in
     * this LazyList's order wins.
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
     * a value by {@code f}; of two entries with the same key, the later one in this LazyList's order wins.
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
        return TraversableModule.toMap(this, TreeMap.empty(comparator), ofAll, f, "LazyList.toSortedMap: f returned null");
    }

    /**
     * The elements as a {@link Queue}, in this LazyList's order.
     *
     * @return a {@code Queue} of the elements
     */
    default Queue<T> toQueue() {
        return TraversableModule.toTraversable(this, Queue.empty(), Queue::ofAll);
    }

    /**
     * The distinct elements as a {@link LinkedHashSet}, in this LazyList's order.
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
     * The elements as a {@link LazyList}, in this LazyList's order.
     *
     * @return a {@code LazyList} of the elements
     */
    default LazyList<T> toLazyList() {
        return TraversableModule.toTraversable(this, LazyList.empty(), LazyList::ofAll);
    }

}
