package com.guizmaii.zazr.collection;

import com.guizmaii.zazr.Tuple;
import com.guizmaii.zazr.Tuple2;
import com.guizmaii.zazr.Tuple3;
import com.guizmaii.zazr.collection.List.Nil;
import com.guizmaii.zazr.collection.internal.AbstractIterator;
import com.guizmaii.zazr.collection.internal.Collections;
import com.guizmaii.zazr.collection.internal.Iterator;
import com.guizmaii.zazr.collection.internal.JavaConverters;
import com.guizmaii.zazr.collection.internal.ListModule;
import com.guizmaii.zazr.collection.internal.ListModule.Combinations;
import com.guizmaii.zazr.collection.internal.ListModule.SplitAt;
import com.guizmaii.zazr.collection.internal.TraversableModule;
import com.guizmaii.zazr.control.Either;
import com.guizmaii.zazr.control.Option;
import java.io.*;
import java.util.*;
import java.util.function.*;
import java.util.stream.Collector;
import org.jspecify.annotations.Nullable;


/**
 * An immutable {@code List} is an eager sequence of elements. Its immutability makes it suitable for concurrent programming.
 * <p>
 * A {@code List} is composed of a {@code head} element and a {@code tail} {@code List}.
 * <p>
 * There are two implementations of the {@code List} interface:
 *
 * <ul>
 * <li>{@link Nil}, which represents the empty {@code List}.</li>
 * <li>{@link Cons}, which represents a {@code List} containing one or more elements.</li>
 * </ul>
 *
 * Both are public records of a sealed interface, so a {@code List} is deconstructed with a record pattern:
 *
 * <pre>
 * {@code
 * static int sum(List<Integer> list) {
 *     return switch (list) {
 *         case Cons(var head, var tail) -> head + sum(tail);
 *         case Nil() -> 0;
 *     };
 * }
 * }
 * </pre>
 *
 * A {@code List} is a {@code Stack} in the sense that it stores elements allowing a last-in-first-out (LIFO) retrieval.
 * <p>
 * Stack API:
 *
 * <ul>
 * <li>{@link #peek()}</li>
 * <li>{@link #peekOption()}</li>
 * <li>{@link #pop()}</li>
 * <li>{@link #popOption()}</li>
 * <li>{@link #pop2()}</li>
 * <li>{@link #pop2Option()}</li>
 * <li>{@link #push(Object)}</li>
 * <li>{@link #push(Object[])}</li>
 * <li>{@link #pushAll(Iterable)}</li>
 * </ul>
 *
 * Methods to obtain a {@code List}:
 *
 * <pre>
 * {@code
 * // factory methods
 * List.empty()                        // = List.of() = Nil.instance()
 * List.of(x)                          // = new Cons<>(x, Nil.instance())
 * List.of(Object...)                  // e.g. List.of(1, 2, 3)
 * List.ofAll(Iterable)                // e.g. List.ofAll(Stream.of(1, 2, 3)) = 1, 2, 3
 * List.ofAll(<primitive array>) // e.g. List.ofAll(new int[] {1, 2, 3}) = 1, 2, 3
 *
 * // int sequences
 * List.range(0, 3)              // = 0, 1, 2
 * List.rangeClosed(0, 3)        // = 0, 1, 2, 3
 * }
 * </pre>
 *
 * Note: the stack-style methods listed above are provided directly on {@code List} for convenience.
 * <p>
 * If operating on a {@code List}, please prefer
 *
 * <ul>
 * <li>{@link #prepend(Object)} over {@link #push(Object)}</li>
 * <li>{@link #prependAll(Iterable)} over {@link #pushAll(Iterable)}</li>
 * <li>{@link #tail()} over {@link #pop()}</li>
 * <li>{@link #tailOption()} over {@link #popOption()}</li>
 * </ul>
 *
 * Factory method applications:
 *
 * <pre>
 * {@code
 * List<Integer>       s1 = List.of(1);
 * List<Integer>       s2 = List.of(1, 2, 3);
 *                           // = List.of(new Integer[] {1, 2, 3});
 *
 * List<int[]>         s3 = List.ofAll(1, 2, 3);
 * List<List<Integer>> s4 = List.ofAll(List.of(1, 2, 3));
 *
 * List<Integer>       s5 = List.ofAll(1, 2, 3);
 * List<Integer>       s6 = List.ofAll(List.of(1, 2, 3));
 *
 * // cuckoo's egg
 * List<Integer[]>     s7 = List.<Integer[]> of(new Integer[] {1, 2, 3});
 * }
 * </pre>
 *
 * Example: Converting a String to digits
 *
 * <pre>
 * {@code
 * // = List(1, 2, 3)
 * List.of("123".toCharArray()).map(c -> Character.digit(c, 10))
 * }
 * </pre>
 *
 * See Okasaki, Chris: <em>Purely Functional Data Structures</em> (p. 7 ff.). Cambridge, 2003.
 *
 * @param <T> Component type of the List
 * @author Daniel Dietrich
 */
public sealed interface List<T extends @Nullable Object> extends Traversable<T> permits List.Cons, List.Nil {

    /**
     * Returns a {@link java.util.stream.Collector} which may be used in conjunction with
     * {@link java.util.stream.Stream#collect(java.util.stream.Collector)} to obtain a {@link List}.
     *
     * @param <T> Component type of the List.
     * @return A com.guizmaii.zazr.collection.List Collector.
     */
    static <T extends @Nullable Object> Collector<T, ArrayList<T>, List<T>> collector() {
        final Supplier<ArrayList<T>> supplier = ArrayList::new;
        final BiConsumer<ArrayList<T>, T> accumulator = ArrayList::add;
        final BinaryOperator<ArrayList<T>> combiner = (left, right) -> {
            left.addAll(right);
            return left;
        };
        final Function<ArrayList<T>, List<T>> finisher = List::ofAll;
        return Collector.of(supplier, accumulator, combiner, finisher);
    }

    /**
     * Returns the single instance of Nil. Convenience method for {@code Nil.instance()}.
     * <p>
     * Note: this method intentionally returns type {@code List} and not {@code Nil}. This comes in handy when folding.
     * If you explicitly need type {@code Nil} use {@linkplain Nil#instance()}.
     *
     * @param <T> Component type of Nil, determined by type inference in the particular context.
     * @return The empty list.
     */
    static <T extends @Nullable Object> List<T> empty() {
        return Nil.instance();
    }

    @Override
    boolean isEmpty();

    /**
     * Narrows a widened {@code List<? extends T>} to {@code List<T>}
     * by performing a type-safe cast. This is eligible because immutable/read-only
     * collections are covariant.
     *
     * @param list A {@code List}.
     * @param <T>  Component type of the {@code List}.
     * @return the given {@code list} instance as narrowed type {@code List<T>}.
     */
    @SuppressWarnings("unchecked")
    static <T extends @Nullable Object> List<T> narrow(List<? extends T> list) {
        return (List<T>) list;
    }

    /**
     * Returns a singleton {@code List}, i.e. a {@code List} of one element.
     *
     * @param element An element.
     * @param <T>     The component type
     * @return A new List instance containing the given element
     */
    static <T extends @Nullable Object> List<T> of(T element) {
        return new Cons<>(element, Nil.instance());
    }

    /**
     * Creates a List of the given elements.
     * <pre>
     * {@code
     *   List.of(1, 2, 3, 4)
     * = Nil.instance().prepend(4).prepend(3).prepend(2).prepend(1)
     * = new Cons(1, new Cons(2, new Cons(3, new Cons(4, Nil.instance()))))
     * }
     * </pre>
     *
     * @param <T>      Component type of the List.
     * @param elements Zero or more elements.
     * @return A list containing the given elements in the same order.
     * @throws NullPointerException if {@code elements} is null
     */
    @SafeVarargs
    static <T extends @Nullable Object> List<T> of(T ... elements) {
        Objects.requireNonNull(elements, "elements is null");
        List<T> result = Nil.instance();
        for (int i = elements.length - 1; i >= 0; i--) {
            result = result.prepend(elements[i]);
        }
        return result;
    }

    /**
     * Creates a List of the given elements.
     * <p>
     * The resulting list has the same iteration order as the given iterable of elements
     * if the iteration order of the elements is stable.
     *
     * @param <T>      Component type of the List.
     * @param elements An Iterable of elements.
     * @return A list containing the given elements in the same order.
     * @throws NullPointerException if {@code elements} is null
     */
    @SuppressWarnings("unchecked")
    static <T extends @Nullable Object> List<T> ofAll(Iterable<? extends T> elements) {
        Objects.requireNonNull(elements, "elements is null");
        if (elements instanceof List) {
            return (List<T>) elements;
        } else if (JavaConverters.underlying(elements) instanceof List<?> underlying) {
            return (List<T>) underlying;
        } else if (elements instanceof java.util.List) {
            List<T> result = Nil.instance();
            final java.util.List<T> list = (java.util.List<T>) elements;
            final ListIterator<T> iterator = list.listIterator(list.size());
            while (iterator.hasPrevious()) {
                result = result.prepend(iterator.previous());
            }
            return result;
        } else if (elements instanceof NavigableSet) {
            List<T> result = Nil.instance();
            final java.util.Iterator<T> iterator = ((NavigableSet<T>) elements).descendingIterator();
            while (iterator.hasNext()) {
                result = result.prepend(iterator.next());
            }
            return result;
        } else {
            List<T> result = Nil.instance();
            for (T element : elements) {
                result = result.prepend(element);
            }
            return result.reverse();
        }
    }

    /**
     * Creates a List that contains the elements of the given {@link java.util.stream.Stream}.
     *
     * @param javaStream A {@link java.util.stream.Stream}
     * @param <T>        Component type of the Stream.
     * @return A List containing the given elements in the same order.
     */
    static <T extends @Nullable Object> List<T> ofAll(java.util.stream.Stream<? extends T> javaStream) {
        Objects.requireNonNull(javaStream, "javaStream is null");
        final java.util.Iterator<? extends T> iterator = javaStream.iterator();
        List<T> list = List.empty();
        while (iterator.hasNext()) {
            list = list.prepend(iterator.next());
        }
        return list.reverse();
    }

    /**
     * Concatenates nested iterables into one List. Static, like every {@code flatten} in Zazr, because Java cannot
     * demand of an instance method that the receiver's element type be a collection. The outer iterable and each inner
     * one are iterated once, so one-shot iterables are accepted.
     * <p>
     * Complexity: O(n) for n inner elements in total: one cell per element, built reversed and reversed once.
     *
     * @param nested Iterables of elements
     * @param <T>    Component type of the inner iterables
     * @return the inner elements, in order
     * @throws NullPointerException if {@code nested}, an inner iterable or an element is null
     */
    static <T extends @Nullable Object> List<T> flatten(Iterable<? extends Iterable<? extends T>> nested) {
        Objects.requireNonNull(nested, "nested is null");
        List<T> reversed = empty();
        for (Iterable<? extends T> inner : nested) {
            for (T element : inner) {
                reversed = reversed.prepend(element);
            }
        }
        return reversed.reverse();
    }

    /**
     * Creates a List from boolean values.
     *
     * @param elements boolean values
     * @return A new List of Boolean values
     * @throws NullPointerException if elements is null
     */
    static List<Boolean> ofAll(boolean ... elements) {
        Objects.requireNonNull(elements, "elements is null");
        return ofAll(Iterator.ofAll(elements));
    }

    /**
     * Creates a List from byte values.
     *
     * @param elements byte values
     * @return A new List of Byte values
     * @throws NullPointerException if elements is null
     */
    static List<Byte> ofAll(byte ... elements) {
        Objects.requireNonNull(elements, "elements is null");
        return ofAll(Iterator.ofAll(elements));
    }

    /**
     * Creates a List from char values.
     *
     * @param elements char values
     * @return A new List of Character values
     * @throws NullPointerException if elements is null
     */
    static List<Character> ofAll(char ... elements) {
        Objects.requireNonNull(elements, "elements is null");
        return ofAll(Iterator.ofAll(elements));
    }

    /**
     * Creates a List from double values.
     *
     * @param elements double values
     * @return A new List of Double values
     * @throws NullPointerException if elements is null
     */
    static List<Double> ofAll(double ... elements) {
        Objects.requireNonNull(elements, "elements is null");
        return ofAll(Iterator.ofAll(elements));
    }

    /**
     * Creates a List from float values.
     *
     * @param elements a float values
     * @return A new List of Float values
     * @throws NullPointerException if elements is null
     */
    static List<Float> ofAll(float ... elements) {
        Objects.requireNonNull(elements, "elements is null");
        return ofAll(Iterator.ofAll(elements));
    }

    /**
     * Creates a List from int values.
     *
     * @param elements int values
     * @return A new List of Integer values
     * @throws NullPointerException if elements is null
     */
    static List<Integer> ofAll(int ... elements) {
        Objects.requireNonNull(elements, "elements is null");
        return ofAll(Iterator.ofAll(elements));
    }

    /**
     * Creates a List from long values.
     *
     * @param elements long values
     * @return A new List of Long values
     * @throws NullPointerException if elements is null
     */
    static List<Long> ofAll(long ... elements) {
        Objects.requireNonNull(elements, "elements is null");
        return ofAll(Iterator.ofAll(elements));
    }

    /**
     * Creates a List from short values.
     *
     * @param elements short values
     * @return A new List of Short values
     * @throws NullPointerException if elements is null
     */
    static List<Short> ofAll(short ... elements) {
        Objects.requireNonNull(elements, "elements is null");
        return ofAll(Iterator.ofAll(elements));
    }

    /**
     * Returns a List containing {@code n} values of a given Function {@code f}
     * over a range of integer values from 0 to {@code n - 1}.
     *
     * @param <T> Component type of the List
     * @param n   The number of elements in the List
     * @param f   The Function computing element values
     * @return A List consisting of elements {@code f(0),f(1), ..., f(n - 1)}
     * @throws NullPointerException if {@code f} is null
     */
    static <T extends @Nullable Object> List<T> tabulate(int n, Function<? super Integer, ? extends T> f) {
        Objects.requireNonNull(f, "f is null");
        return Collections.tabulate(n, f, empty(), List::of);
    }

    /**
     * Returns a List containing {@code n} values supplied by a given Supplier {@code s}.
     *
     * @param <T> Component type of the List
     * @param n   The number of elements in the List
     * @param s   The Supplier computing element values
     * @return A List of size {@code n}, where each element contains the result supplied by {@code s}.
     * @throws NullPointerException if {@code s} is null
     */
    static <T extends @Nullable Object> List<T> fill(int n, Supplier<? extends T> s) {
        Objects.requireNonNull(s, "s is null");
        return Collections.fill(n, s, empty(), List::of);
    }

    /**
     * Returns a List containing {@code n} times the given {@code element}
     *
     * @param <T>     Component type of the List
     * @param n       The number of elements in the List
     * @param element The element
     * @return A List of size {@code n}, where each element is the given {@code element}.
     */
    static <T extends @Nullable Object> List<T> fill(int n, T element) {
        return Collections.fillObject(n, element, empty(), List::of);
    }

    /**
     * Creates a List of char numbers starting from {@code from}, extending to {@code toExclusive - 1}.
     * <p>
     * Examples:
     * <pre>
     * {@code
     * List.range('a', 'a')  // = List()
     * List.range('c', 'a')  // = List()
     * List.range('a', 'd')  // = List('a', 'b', 'c')
     * }
     * </pre>
     *
     * @param from        the first char
     * @param toExclusive the last char + 1
     * @return a range of char values as specified or the empty range if {@code from >= toExclusive}
     */
    static List<Character> range(char from, char toExclusive) {
        return ofAll(Iterator.range(from, toExclusive));
    }

    /**
     * Creates a List of char numbers starting from {@code from}, extending to {@code toExclusive - 1},
     * with {@code step}.
     * <p>
     * Examples:
     * <pre>
     * {@code
     * List.rangeBy('a', 'c', 1)  // = List('a', 'b')
     * List.rangeBy('a', 'd', 2)  // = List('a', 'c')
     * List.rangeBy('d', 'a', -2) // = List('d', 'b')
     * List.rangeBy('d', 'a', 2)  // = List()
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
    static List<Character> rangeBy(char from, char toExclusive, int step) {
        return ofAll(Iterator.rangeBy(from, toExclusive, step));
    }

    /**
     * Creates a List of double numbers starting from {@code from}, extending up to but not including {@code toExclusive},
     * with {@code step}.
     * <p>
     * Examples:
     * <pre>
     * {@code
     * List.rangeBy(1.0, 3.0, 1.0)  // = List(1.0, 2.0)
     * List.rangeBy(1.0, 4.0, 2.0)  // = List(1.0, 3.0)
     * List.rangeBy(4.0, 1.0, -2.0) // = List(4.0, 2.0)
     * List.rangeBy(4.0, 1.0, 2.0)  // = List()
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
    static List<Double> rangeBy(double from, double toExclusive, double step) {
        return ofAll(Iterator.rangeBy(from, toExclusive, step));
    }

    /**
     * Creates a List of int numbers starting from {@code from}, extending to {@code toExclusive - 1}.
     * <p>
     * Examples:
     * <pre>
     * {@code
     * List.range(0, 0)  // = List()
     * List.range(2, 0)  // = List()
     * List.range(-2, 2) // = List(-2, -1, 0, 1)
     * }
     * </pre>
     *
     * @param from        the first number
     * @param toExclusive the last number + 1
     * @return a range of int values as specified or the empty range if {@code from >= toExclusive}
     */
    static List<Integer> range(int from, int toExclusive) {
        return ofAll(Iterator.range(from, toExclusive));
    }

    /**
     * Creates a List of int numbers starting from {@code from}, extending to {@code toExclusive - 1},
     * with {@code step}.
     * <p>
     * Examples:
     * <pre>
     * {@code
     * List.rangeBy(1, 3, 1)  // = List(1, 2)
     * List.rangeBy(1, 4, 2)  // = List(1, 3)
     * List.rangeBy(4, 1, -2) // = List(4, 2)
     * List.rangeBy(4, 1, 2)  // = List()
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
    static List<Integer> rangeBy(int from, int toExclusive, int step) {
        return ofAll(Iterator.rangeBy(from, toExclusive, step));
    }

    /**
     * Creates a List of long numbers starting from {@code from}, extending to {@code toExclusive - 1}.
     * <p>
     * Examples:
     * <pre>
     * {@code
     * List.range(0L, 0L)  // = List()
     * List.range(2L, 0L)  // = List()
     * List.range(-2L, 2L) // = List(-2L, -1L, 0L, 1L)
     * }
     * </pre>
     *
     * @param from        the first number
     * @param toExclusive the last number + 1
     * @return a range of long values as specified or the empty range if {@code from >= toExclusive}
     */
    static List<Long> range(long from, long toExclusive) {
        return ofAll(Iterator.range(from, toExclusive));
    }

    /**
     * Creates a List of long numbers starting from {@code from}, extending to {@code toExclusive - 1},
     * with {@code step}.
     * <p>
     * Examples:
     * <pre>
     * {@code
     * List.rangeBy(1L, 3L, 1L)  // = List(1L, 2L)
     * List.rangeBy(1L, 4L, 2L)  // = List(1L, 3L)
     * List.rangeBy(4L, 1L, -2L) // = List(4L, 2L)
     * List.rangeBy(4L, 1L, 2L)  // = List()
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
    static List<Long> rangeBy(long from, long toExclusive, long step) {
        return ofAll(Iterator.rangeBy(from, toExclusive, step));
    }

    /**
     * Creates a List of char numbers starting from {@code from}, extending to {@code toInclusive}.
     * <p>
     * Examples:
     * <pre>
     * {@code
     * List.rangeClosed('a', 'a')  // = List('a')
     * List.rangeClosed('c', 'a')  // = List()
     * List.rangeClosed('a', 'd')  // = List('a', 'b', 'c', 'd')
     * }
     * </pre>
     *
     * @param from        the first char
     * @param toInclusive the last char
     * @return a range of char values as specified or the empty range if {@code from > toInclusive}
     */
    static List<Character> rangeClosed(char from, char toInclusive) {
        return ofAll(Iterator.rangeClosed(from, toInclusive));
    }

    /**
     * Creates a List of char numbers starting from {@code from}, extending to {@code toInclusive},
     * with {@code step}.
     * <p>
     * Examples:
     * <pre>
     * {@code
     * List.rangeClosedBy('a', 'c', 1)  // = List('a', 'b', 'c')
     * List.rangeClosedBy('a', 'd', 2)  // = List('a', 'c')
     * List.rangeClosedBy('d', 'a', -2) // = List('d', 'b')
     * List.rangeClosedBy('d', 'a', 2)  // = List()
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
    static List<Character> rangeClosedBy(char from, char toInclusive, int step) {
        return ofAll(Iterator.rangeClosedBy(from, toInclusive, step));
    }

    /**
     * Creates a List of double numbers starting from {@code from}, extending to {@code toInclusive},
     * with {@code step}.
     * <p>
     * Examples:
     * <pre>
     * {@code
     * List.rangeClosedBy(1.0, 3.0, 1.0)  // = List(1.0, 2.0, 3.0)
     * List.rangeClosedBy(1.0, 4.0, 2.0)  // = List(1.0, 3.0)
     * List.rangeClosedBy(4.0, 1.0, -2.0) // = List(4.0, 2.0)
     * List.rangeClosedBy(4.0, 1.0, 2.0)  // = List()
     * }
     * </pre>
     *
     * @param from        the first double
     * @param toInclusive the upper bound (inclusive)
     * @param step        the step
     * @return a range of double values as specified or the empty range if<br>
     * {@code from > toInclusive} and {@code step > 0} or<br>
     * {@code from < toInclusive} and {@code step < 0}
     * @throws IllegalArgumentException if {@code step} is zero and {@code from != toInclusive}
     *                                  (if {@code from == toInclusive}, a singleton list is returned regardless of {@code step})
     */
    static List<Double> rangeClosedBy(double from, double toInclusive, double step) {
        return ofAll(Iterator.rangeClosedBy(from, toInclusive, step));
    }

    /**
     * Creates a List of int numbers starting from {@code from}, extending to {@code toInclusive}.
     * <p>
     * Examples:
     * <pre>
     * {@code
     * List.rangeClosed(0, 0)  // = List(0)
     * List.rangeClosed(2, 0)  // = List()
     * List.rangeClosed(-2, 2) // = List(-2, -1, 0, 1, 2)
     * }
     * </pre>
     *
     * @param from        the first number
     * @param toInclusive the last number
     * @return a range of int values as specified or the empty range if {@code from > toInclusive}
     */
    static List<Integer> rangeClosed(int from, int toInclusive) {
        return ofAll(Iterator.rangeClosed(from, toInclusive));
    }

    /**
     * Creates a List of int numbers starting from {@code from}, extending to {@code toInclusive},
     * with {@code step}.
     * <p>
     * Examples:
     * <pre>
     * {@code
     * List.rangeClosedBy(1, 3, 1)  // = List(1, 2, 3)
     * List.rangeClosedBy(1, 4, 2)  // = List(1, 3)
     * List.rangeClosedBy(4, 1, -2) // = List(4, 2)
     * List.rangeClosedBy(4, 1, 2)  // = List()
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
    static List<Integer> rangeClosedBy(int from, int toInclusive, int step) {
        return ofAll(Iterator.rangeClosedBy(from, toInclusive, step));
    }

    /**
     * Creates a List of long numbers starting from {@code from}, extending to {@code toInclusive}.
     * <p>
     * Examples:
     * <pre>
     * {@code
     * List.rangeClosed(0L, 0L)  // = List(0L)
     * List.rangeClosed(2L, 0L)  // = List()
     * List.rangeClosed(-2L, 2L) // = List(-2L, -1L, 0L, 1L, 2L)
     * }
     * </pre>
     *
     * @param from        the first number
     * @param toInclusive the last number
     * @return a range of long values as specified or the empty range if {@code from > toInclusive}
     */
    static List<Long> rangeClosed(long from, long toInclusive) {
        return ofAll(Iterator.rangeClosed(from, toInclusive));
    }

    /**
     * Creates a List of long numbers starting from {@code from}, extending to {@code toInclusive},
     * with {@code step}.
     * <p>
     * Examples:
     * <pre>
     * {@code
     * List.rangeClosedBy(1L, 3L, 1L)  // = List(1L, 2L, 3L)
     * List.rangeClosedBy(1L, 4L, 2L)  // = List(1L, 3L)
     * List.rangeClosedBy(4L, 1L, -2L) // = List(4L, 2L)
     * List.rangeClosedBy(4L, 1L, 2L)  // = List()
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
    static List<Long> rangeClosedBy(long from, long toInclusive, long step) {
        return ofAll(Iterator.rangeClosedBy(from, toInclusive, step));
    }

    /**
     * Transposes the rows and columns of a {@link List} matrix.
     * <p>
     * Complexity: O(rows * columns).
     *
     * @param <T> matrix element type
     * @param matrix to be transposed.
     * @return a transposed {@link List} matrix.
     * @throws IllegalArgumentException if the row lengths of {@code matrix} differ.
     * <p>
     * ex: {@code
     * List.transpose(List(List(1,2,3), List(4,5,6))) → List(List(1,4), List(2,5), List(3,6))
     * }
     */
    static <T extends @Nullable Object> List<List<T>> transpose(List<List<T>> matrix) {
        return Collections.transpose(matrix, List::ofAll, List::of);
    }

    /**
     * Creates a list from a seed value and a function.
     * The function takes the seed at first.
     * The function should return {@code None} when it's
     * done generating the list, otherwise {@code Some} {@code Tuple}
     * of the value to add to the resulting list and the element for
     * the next call.
     * <p>
     * Example:
     * <pre>
     * {@code
     * List.unfoldRight(10, x -> x == 0
     *             ? Option.none()
     *             : Option.some(new Tuple2<>(x, x-1)));
     * // List(10, 9, 8, 7, 6, 5, 4, 3, 2, 1))
     * }
     * </pre>
     *
     * @param <T>  type of seeds
     * @param <U>  type of unfolded values
     * @param seed the start value for the iteration
     * @param f    the function to get the next step of the iteration
     * @return a list with the values built up by the iteration
     * @throws NullPointerException if {@code f} is null
     */
    static <T extends @Nullable Object, U extends @Nullable Object> List<U> unfoldRight(T seed, Function<? super T, Option<Tuple2<? extends U, ? extends T>>> f) {
        return Iterator.unfoldRight(seed, f).toList();
    }

    /**
     * Creates a list from a seed value and a function.
     * The function takes the seed at first.
     * The function should return {@code None} when it's
     * done generating the list, otherwise {@code Some} {@code Tuple}
     * of the element for the next call and
     * the value to add to the resulting list.
     * <p>
     * Example:
     * <pre>
     * {@code
     * List.unfoldLeft(10, x -> x == 0
     *             ? Option.none()
     *             : Option.some(new Tuple2<>(x-1, x)));
     * // List(1, 2, 3, 4, 5, 6, 7, 8, 9, 10))
     * }
     * </pre>
     *
     * @param <T>  type of seeds
     * @param <U>  type of unfolded values
     * @param seed the start value for the iteration
     * @param f    the function to get the next step of the iteration
     * @return a list with the values built up by the iteration
     * @throws NullPointerException if {@code f} is null
     */
    static <T extends @Nullable Object, U extends @Nullable Object> List<U> unfoldLeft(T seed, Function<? super T, Option<Tuple2<? extends T, ? extends U>>> f) {
        return Iterator.unfoldRight(seed, f.andThen(tupleOpt -> tupleOpt.map(Tuple2::swap)))
          .foldLeft(List.empty(), List::prepend);
    }

    /**
     * Creates a list from a seed value and a function.
     * The function takes the seed at first.
     * The function should return {@code None} when it's
     * done generating the list, otherwise {@code Some} {@code Tuple}
     * of the element for the next call and
     * the value to add to the resulting list.
     * <p>
     * Example:
     * <pre>
     * {@code
     * List.unfold(10, x -> x == 0
     *             ? Option.none()
     *             : Option.some(new Tuple2<>(x-1, x)));
     * // List(1, 2, 3, 4, 5, 6, 7, 8, 9, 10))
     * }
     * </pre>
     *
     * @param <T>  type of seeds and unfolded values
     * @param seed the start value for the iteration
     * @param f    the function to get the next step of the iteration
     * @return a list with the values built up by the iteration
     * @throws NullPointerException if {@code f} is null
     */
    static <T extends @Nullable Object> List<T> unfold(T seed, Function<? super T, Option<Tuple2<? extends T, ? extends T>>> f) {
        return Iterator.unfoldRight(seed, f.andThen(tupleOpt -> tupleOpt.map(Tuple2::swap)))
          .foldLeft(List.empty(), List::prepend);
    }

    /**
     * Returns a new List with the given element appended at the end.
     * <p>
     * Complexity: O(n); every cell of this List is rebuilt.
     *
     * @param element the element to append
     * @return a new List ending with the given element
     */
    default List<T> append(T element) {
        return foldRight(of(element), (x, xs) -> xs.prepend(x));
    }

    /**
     * Returns a new List with the given elements appended at the end, in iteration order.
     * <p>
     * Complexity: O(n + m) for m appended elements; the elements are copied once and this List is rebuilt.
     *
     * @param elements the elements to append
     * @return a new List ending with the given elements, or this List if there are none
     * @throws NullPointerException if {@code elements} is null
     */
    default List<T> appendAll(Iterable<? extends T> elements) {
        Objects.requireNonNull(elements, "elements is null");
        return List.<T> ofAll(elements).prependAll(this);
    }

    /**
     * An unmodifiable {@link java.util.List} view of this List, in its order: nothing is copied, reads go through
     * to this List, which never changes, and every mutator of the view (including those of its iterators and
     * sub-lists) throws {@link UnsupportedOperationException}. {@code reversed()} and {@code subList} are views too.
     * A mutable copy is {@code new java.util.ArrayList<>(list.asJava())}; {@code List.ofAll} given the view
     * returns this List without copying.
     * <p>
     * Complexity: O(1); {@code get(i)} on the view is O(i), {@code size()} is O(1).
     *
     * @return an unmodifiable {@code java.util.List} view
     */
    default java.util.List<T> asJava() {
        return JavaConverters.asJava(this);
    }

    /**
     * All combinations of the elements, for every size from 0 to {@code length()}, by position.
     * <p>
     * Complexity: O(2^n) combinations.
     *
     * @return the combinations, shortest first
     */
    default List<List<T>> combinations() {
        return rangeClosed(0, length()).map(this::combinations).flatMap(Function.identity());
    }

    /**
     * All combinations of {@code k} elements, by position, in lexicographic position order. A negative {@code k}
     * counts as 0, and a {@code k} greater than {@code length()} gives no combination.
     * <p>
     * Complexity: O(C(n, k)) combinations.
     *
     * @param k the size of each combination
     * @return the combinations
     */
    default List<List<T>> combinations(int k) {
        return Combinations.apply(this, Math.max(k, 0));
    }

    /**
     * Whether {@code that} occurs in this List as a contiguous slice.
     * <p>
     * Complexity: O(n * m) for a slice of m elements.
     *
     * @param that the slice to look for
     * @return true if {@code that} occurs contiguously in this List (an empty slice always does)
     * @throws NullPointerException if {@code that} is null
     */
    default boolean containsSlice(Iterable<? extends T> that) {
        Objects.requireNonNull(that, "that is null");
        return indexOfSlice(that) >= 0;
    }

    /**
     * Returns a new {@code List} containing the elements of this instance
     * with all duplicates removed. Element equality is determined using {@code equals}.
     * <p>
     * Complexity: O(n).
     *
     * @return a new {@code List} without duplicate elements
     */
    default List<T> distinct() {
        return distinctBy(Function.identity());
    }

    /**
     * Returns a new {@code List} containing the elements of this instance
     * without duplicates, as determined by the given {@code comparator}; the first of two equal elements is kept.
     * <p>
     * Complexity: O(n log n) comparisons.
     *
     * @param comparator a comparator used to determine equality of elements
     * @return a new {@code List} with duplicates removed
     * @throws NullPointerException if {@code comparator} is null
     */
    default List<T> distinctBy(Comparator<? super T> comparator) {
        Objects.requireNonNull(comparator, "comparator is null");
        final java.util.Set<T> seen = new java.util.TreeSet<>(comparator);
        return filter(seen::add);
    }

    /**
     * Returns a new {@code List} containing the elements of this instance
     * without duplicates, based on keys extracted from elements using {@code keyExtractor}.
     * <p>
     * The first occurrence of each key is retained in the resulting sequence.
     * <p>
     * Complexity: O(n), one key per element.
     *
     * @param keyExtractor a function to extract keys for determining uniqueness
     * @param <U>          the type of key
     * @return a new {@code List} with duplicates removed based on keys
     * @throws NullPointerException if {@code keyExtractor} is null
     */
    default <U extends @Nullable Object> List<T> distinctBy(Function<? super T, ? extends U> keyExtractor) {
        Objects.requireNonNull(keyExtractor, "keyExtractor is null");
        final java.util.Set<U> seen = new java.util.HashSet<>();
        return filter(t -> seen.add(keyExtractor.apply(t)));
    }

    /**
     * The elements without duplicates, keeping the last occurrence of each group of elements the comparator calls
     * equal, in the order of those last occurrences.
     * <p>
     * Complexity: O(n log n) comparisons.
     *
     * @param comparator decides which elements are duplicates
     * @return a new List
     * @throws NullPointerException if {@code comparator} is null
     */
    default List<T> distinctByKeepLast(Comparator<? super T> comparator) {
        Objects.requireNonNull(comparator, "comparator is null");
        return ofAll(Iterator.ofAll(this).distinctByKeepLast(comparator));
    }

    /**
     * The elements without duplicates, keeping the last occurrence of each key, in the order of those last
     * occurrences.
     * <p>
     * Complexity: O(n), one key per element.
     *
     * @param keyExtractor computes the key an element is deduplicated by
     * @param <U>          the key type
     * @return a new List
     * @throws NullPointerException if {@code keyExtractor} is null
     */
    default <U extends @Nullable Object> List<T> distinctByKeepLast(Function<? super T, ? extends U> keyExtractor) {
        Objects.requireNonNull(keyExtractor, "keyExtractor is null");
        return ofAll(Iterator.ofAll(this).distinctByKeepLast(keyExtractor));
    }

    /**
     * Returns a new {@code List} without the first {@code n} elements,
     * or an empty instance if this contains fewer than {@code n} elements.
     * <p>
     * Complexity: O(n) for n dropped elements; the rest of this List is shared, not copied.
     *
     * @param n the number of elements to drop
     * @return a new instance excluding the first {@code n} elements
     */
    default List<T> drop(int n) {
        if (n <= 0) {
            return this;
        }
        if (n >= size()) {
            return empty();
        }
        List<T> list = this;
        for (long i = n; i > 0 && !list.isEmpty(); i--) {
            list = list.tail();
        }
        return list;
    }

    /**
     * Returns a new {@code List} starting from the first element
     * that satisfies the given {@code predicate}, dropping all preceding elements.
     * <p>
     * Complexity: O(k) for the k dropped elements; the rest of this List is shared, not copied.
     *
     * @param predicate a condition tested on each element
     * @return a new instance starting from the first element matching the predicate
     * @throws NullPointerException if {@code predicate} is null
     */
    default List<T> dropUntil(Predicate<? super T> predicate) {
        Objects.requireNonNull(predicate, "predicate is null");
        return dropWhile(predicate.negate());
    }

    /**
     * Returns a new {@code List} starting from the first element
     * that does not satisfy the given {@code predicate}, dropping all preceding elements.
     * <p>
     * This is equivalent to {@code dropUntil(predicate.negate())}, which is useful
     * for method references that cannot be negated directly.
     * <p>
     * Complexity: O(k) for the k dropped elements; the rest of this List is shared, not copied.
     *
     * @param predicate a condition tested on each element
     * @return a new instance starting from the first element not matching the predicate
     * @throws NullPointerException if {@code predicate} is null
     */
    default List<T> dropWhile(Predicate<? super T> predicate) {
        Objects.requireNonNull(predicate, "predicate is null");
        List<T> list = this;
        while (!list.isEmpty() && predicate.test(list.head())) {
            list = list.tail();
        }
        return list;
    }

    /**
     * Returns a new {@code List} without the last {@code n} elements,
     * or an empty instance if this contains fewer than {@code n} elements.
     * <p>
     * Complexity: O(n); the kept prefix is copied.
     *
     * @param n the number of elements to drop from the end
     * @return a new instance excluding the last {@code n} elements
     */
    default List<T> dropRight(int n) {
        if (n <= 0) {
            return this;
        }
        final int length = length();
        if (n >= length) {
            return empty();
        }
        return take(length - n);
    }

    /**
     * The elements up to and including the last one satisfying {@code predicate}: the elements after it are dropped.
     * <p>
     * Complexity: O(n); the List is reversed twice.
     *
     * @param predicate the condition, tested from the end
     * @return a new List
     * @throws NullPointerException if {@code predicate} is null
     */
    default List<T> dropRightUntil(Predicate<? super T> predicate) {
        Objects.requireNonNull(predicate, "predicate is null");
        return reverse().dropUntil(predicate).reverse();
    }

    /**
     * The elements up to and including the last one not satisfying {@code predicate}, that is
     * {@code dropRightUntil(predicate.negate())}.
     * <p>
     * Complexity: O(n); the List is reversed twice.
     *
     * @param predicate the condition, tested from the end
     * @return a new List
     * @throws NullPointerException if {@code predicate} is null
     */
    default List<T> dropRightWhile(Predicate<? super T> predicate) {
        Objects.requireNonNull(predicate, "predicate is null");
        return dropRightUntil(predicate.negate());
    }

    /**
     * The elements occurring more than once, each of them once, in order of first occurrence.
     * {@code List.of(3, 1, 3, 2, 1, 3).duplicates()} is {@code List.of(3, 1)}. An empty result means that all
     * elements are distinct.
     * <p>
     * Complexity: O(n), one hash lookup per element.
     *
     * @return a new List of the repeated elements
     */
    default List<T> duplicates() {
        return duplicatesBy(Function.identity());
    }

    /**
     * The elements whose key occurs more than once, one per repeated key, in order of first occurrence; the element
     * returned for a key is its first occurrence.
     * <p>
     * Complexity: O(n), one key and one hash lookup per element.
     *
     * @param keyExtractor computes the key an element is compared by
     * @param <U>          the key type
     * @return a new List of the elements with a repeated key
     * @throws NullPointerException if {@code keyExtractor} is null
     */
    default <U extends @Nullable Object> List<T> duplicatesBy(Function<? super T, ? extends U> keyExtractor) {
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
        final java.util.List<T> result = new ArrayList<>(duplicated.size());
        for (java.util.Map.Entry<U, T> entry : first.entrySet()) {
            if (duplicated.contains(entry.getKey())) {
                result.add(entry.getValue());
            }
        }
        return ofAll(result);
    }

    /**
     * Whether this List ends with {@code that}.
     * <p>
     * Complexity: O(n + m) for m elements of {@code that}: the suffix is reached by walking this List.
     *
     * @param that the suffix to test
     * @return true if the last {@code m} elements equal {@code that} (an empty {@code that} is always a suffix)
     * @throws NullPointerException if {@code that} is null
     */
    default boolean endsWith(Iterable<? extends T> that) {
        Objects.requireNonNull(that, "that is null");
        final List<? extends T> suffix = List.ofAll(that);
        final int skipped = length() - suffix.length();
        if (skipped < 0) {
            return false;
        }
        List<T> these = drop(skipped);
        for (List<? extends T> other = suffix; !other.isEmpty(); other = other.tail(), these = these.tail()) {
            if (!Objects.equals(these.head(), other.head())) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns a new traversable containing only the elements that satisfy the given predicate.
     * <p>
     * Complexity: O(n).
     *
     * @param predicate the condition to test elements
     * @return a traversable with elements matching the predicate
     * @throws NullPointerException if {@code predicate} is null
     */
    default List<T> filter(Predicate<? super T> predicate) {
        Objects.requireNonNull(predicate, "predicate is null");
        if (isEmpty()) {
            return this;
        } else {
            final List<T> filtered = foldLeft(empty(), (xs, x) -> predicate.test(x) ? xs.prepend(x) : xs);
            if (filtered.isEmpty()) {
                return empty();
            } else if (filtered.length() == length()) {
                return this;
            } else {
                return filtered.reverse();
            }
        }
    }

    default List<T> reject(Predicate<? super T> predicate){
        Objects.requireNonNull(predicate, "predicate is null");
        return Collections.reject(this, predicate, kept -> filter(kept));
    }

    default <U extends @Nullable Object> List<U> flatMap(Function<? super T, ? extends Iterable<? extends U>> mapper) {
        Objects.requireNonNull(mapper, "mapper is null");
        List<U> list = empty();
        for (T t : this) {
            for (U u : mapper.apply(t)) {
                list = list.prepend(u);
            }
        }
        return list.reverse();
    }

    /**
     * Folds the elements from the right: starts with {@code zero} and combines each element, from the last to the
     * first, with the accumulator.
     * <pre>{@code
     * // = 24
     * List.of('4', '2').foldRight(0, (x, acc) -> acc * 10 + x - '0');
     * }</pre>
     * <p>
     * The elements are folded from the end: this List is reversed first, then folded from the left, so the recursion
     * depth does not grow with the length.
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
     * The element at {@code index}.
     * <p>
     * Complexity: O(index); the cells are walked one by one.
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
        List<T> list = this;
        for (int i = index - 1; i >= 0; i--) {
            list = list.tail();
            if (list.isEmpty()) {
                throw new IndexOutOfBoundsException("get(" + index + ") on List of length " + (index - i));
            }
        }
        return list.head();
    }

    default <C extends @Nullable Object> Map<C, List<T>> groupBy(Function<? super T, ? extends C> classifier) {
        return Collections.groupBy(this, classifier, List::ofAll);
    }

    /**
     * The index of the first occurrence of {@code element}, or -1.
     * <p>
     * Complexity: O(n).
     *
     * @param element the element to find
     * @return the index of its first occurrence, or -1 if absent
     */
    default int indexOf(T element) {
        return indexOf(element, 0);
    }

    /**
     * The index of the first occurrence of {@code element} at or after {@code from}, or -1. A negative {@code from}
     * counts as 0.
     * <p>
     * Complexity: O(n).
     *
     * @param element the element to find
     * @param from    the first position to look at
     * @return the first index {@code >= from} of the element, or -1 if absent
     */
    default int indexOf(T element, int from) {
        int index = 0;
        for (List<T> list = this; !list.isEmpty(); list = list.tail(), index++) {
            if (index >= from && Objects.equals(list.head(), element)) {
                return index;
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
     * The first index at which {@code that} occurs as a contiguous slice, or -1.
     * <p>
     * Complexity: O(n * m) for a slice of m elements.
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
     * Complexity: O(n * m) for a slice of m elements.
     *
     * @param that the slice to find
     * @param from the first position to look at
     * @return the index of its first occurrence at or after {@code from}, or -1
     * @throws NullPointerException if {@code that} is null
     */
    default int indexOfSlice(Iterable<? extends T> that, int from) {
        Objects.requireNonNull(that, "that is null");
        return ListModule.Slice.indexOfSlice(this, that, from);
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
     * The index of the first element satisfying {@code predicate}, or -1.
     * <p>
     * Complexity: O(n).
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
     * Complexity: O(n).
     *
     * @param predicate the condition
     * @param from      the first position to look at
     * @return the first index {@code >= from} of a satisfying element, or -1
     * @throws NullPointerException if {@code predicate} is null
     */
    default int indexWhere(Predicate<? super T> predicate, int from) {
        Objects.requireNonNull(predicate, "predicate is null");
        int i = Math.max(from, 0);
        List<T> these = drop(i);
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
     * Returns all elements of this List except the last one.
     * <p>
     * This is the dual of {@link #tail()}.
     * <p>
     * Complexity: O(n); the kept prefix is copied.
     *
     * @return a new instance containing all elements except the last
     * @throws UnsupportedOperationException if this List is empty
     */
    default List<T> init() {
        if (isEmpty()) {
            throw new UnsupportedOperationException("init of empty list");
        } else {
            return dropRight(1);
        }
    }

    /**
     * Returns all elements of this List except the last one, wrapped in an {@code Option}.
     * <p>
     * This is the dual of {@link #tailOption()}.
     * <p>
     * Complexity: O(n); the kept prefix is copied.
     *
     * @return {@code Some(traversable)} if non-empty, or {@code None} if this List is empty
     */
    default Option<List<T>> initOption() {
        return isEmpty() ? Option.none() : Option.some(init());
    }

    /**
     * Returns the number of elements in this List.
     * <p>
     * Equivalent to {@link #size()}.
     * <p>
     * Complexity: O(n); a cons list has no length field, so the cells are counted.
     *
     * @return the number of elements
     */
    int length();

    /**
     * A new List with {@code element} inserted at {@code index}, the elements from {@code index} on shifted right.
     * <p>
     * Complexity: O(index); the cells before the insertion point are copied, the rest is shared.
     *
     * @param index   the position of the inserted element
     * @param element the element to insert
     * @return a new List
     * @throws IndexOutOfBoundsException if {@code index} is negative or greater than {@code length()}
     */
    default List<T> insert(int index, T element) {
        if (index < 0) {
            throw new IndexOutOfBoundsException("insert(" + index + ", e)");
        }
        List<T> preceding = Nil.instance();
        List<T> tail = this;
        for (int i = index; i > 0; i--, tail = tail.tail()) {
            if (tail.isEmpty()) {
                throw new IndexOutOfBoundsException("insert(" + index + ", e) on List of length " + length());
            }
            preceding = preceding.prepend(tail.head());
        }
        List<T> result = tail.prepend(element);
        for (T next : preceding) {
            result = result.prepend(next);
        }
        return result;
    }

    /**
     * A new List with {@code elements} inserted at {@code index}, in iteration order, the elements from {@code index}
     * on shifted right.
     * <p>
     * Complexity: O(index + m) for m inserted elements; the cells before the insertion point are copied, the rest is
     * shared.
     *
     * @param index    the position of the first inserted element
     * @param elements the elements to insert
     * @return a new List
     * @throws IndexOutOfBoundsException if {@code index} is negative or greater than {@code length()}
     * @throws NullPointerException      if {@code elements} is null
     */
    default List<T> insertAll(int index, Iterable<? extends T> elements) {
        Objects.requireNonNull(elements, "elements is null");
        if (index < 0) {
            throw new IndexOutOfBoundsException("insertAll(" + index + ", elements)");
        }
        List<T> preceding = Nil.instance();
        List<T> tail = this;
        for (int i = index; i > 0; i--, tail = tail.tail()) {
            if (tail.isEmpty()) {
                throw new IndexOutOfBoundsException("insertAll(" + index + ", elements) on List of length " + length());
            }
            preceding = preceding.prepend(tail.head());
        }
        List<T> result = tail.prependAll(elements);
        for (T next : preceding) {
            result = result.prepend(next);
        }
        return result;
    }

    /**
     * The elements with {@code element} inserted between every two of them.
     * <p>
     * Complexity: O(n).
     *
     * @param element the separator
     * @return a new List, or this List if it is empty
     */
    default List<T> intersperse(T element) {
        return ofAll(Iterator.ofAll(this).intersperse(element));
    }

    /**
     * Returns the last element of this List.
     * <p>
     * Complexity: O(n).
     *
     * @return the last element
     * @throws NoSuchElementException if this List is empty
     */
    default T last() {
        return Collections.last(this);
    }

    /**
     * The index of the last occurrence of {@code element}, or -1.
     * <p>
     * Complexity: O(n).
     *
     * @param element the element to find
     * @return the index of its last occurrence, or -1 if absent
     */
    default int lastIndexOf(T element) {
        return lastIndexOf(element, Integer.MAX_VALUE);
    }

    /**
     * The index of the last occurrence of {@code element} at or before {@code end}, or -1.
     * <p>
     * Complexity: O(n).
     *
     * @param element the element to find
     * @param end     the last position to look at
     * @return the last index {@code <= end} of the element, or -1 if absent
     */
    default int lastIndexOf(T element, int end) {
        int result = -1, index = 0;
        for (List<T> list = this; index <= end && !list.isEmpty(); list = list.tail(), index++) {
            if (Objects.equals(list.head(), element)) {
                result = index;
            }
        }
        return result;
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
     * The last index at which {@code that} occurs as a contiguous slice, or -1.
     * <p>
     * Complexity: O(n * m) for a slice of m elements.
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
     * Complexity: O(n * m) for a slice of m elements.
     *
     * @param that the slice to find
     * @param end  the last position to look at
     * @return the index of its last occurrence at or before {@code end}, or -1
     * @throws NullPointerException if {@code that} is null
     */
    default int lastIndexOfSlice(Iterable<? extends T> that, int end) {
        Objects.requireNonNull(that, "that is null");
        return ListModule.Slice.lastIndexOfSlice(this, that, end);
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
     * The index of the last element satisfying {@code predicate}, or -1.
     * <p>
     * Complexity: O(n).
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
     * Complexity: O(n).
     *
     * @param predicate the condition
     * @param end       the last position to look at
     * @return the last index {@code <= end} of a satisfying element, or -1
     * @throws NullPointerException if {@code predicate} is null
     */
    default int lastIndexWhere(Predicate<? super T> predicate, int end) {
        Objects.requireNonNull(predicate, "predicate is null");
        int i = 0;
        List<T> these = this;
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

    default <U extends @Nullable Object> List<U> map(Function<? super T, ? extends U> mapper) {
        Objects.requireNonNull(mapper, "mapper is null");
        List<U> list = empty();
        for (T t : this) {
            list = list.prepend(mapper.apply(t));
        }
        return list.reverse();
    }

    default <U extends @Nullable Object> List<U> collect(Function<? super T, ? extends Option<? extends U>> mapper) {
        Objects.requireNonNull(mapper, "mapper is null");
        List<U> list = empty();
        for (T t : this) {
            final Option<? extends U> collected = Objects.requireNonNull(mapper.apply(t), "List.collect: mapper returned null");
            if (collected.isDefined()) {
                list = list.prepend(collected.get());
            }
        }
        return list.reverse();
    }

    default <U extends @Nullable Object> List<U> as(U value) {
        return map(ignored -> value);
    }

    default List<T> orElse(Iterable<? extends T> other) {
        return isEmpty() ? ofAll(other) : this;
    }

    default List<T> orElse(Supplier<? extends Iterable<? extends T>> supplier) {
        return isEmpty() ? ofAll(supplier.get()) : this;
    }

    /**
     * This List padded on the right with {@code element} until it is {@code length} long.
     * <p>
     * Complexity: O(n + k) for k added elements.
     *
     * @param length  the target length
     * @param element the padding element
     * @return a new List, or this List if it is already at least {@code length} long
     */
    default List<T> padTo(int length, T element) {
        final int actualLength = length();
        if (length <= actualLength) {
            return this;
        } else {
            return appendAll(Iterator.continually(element).take(length - actualLength));
        }
    }

    /**
     * This List padded on the left with {@code element} until it is {@code length} long.
     * <p>
     * Complexity: O(k) for k added elements; this List is shared, not copied.
     *
     * @param length  the target length
     * @param element the padding element
     * @return a new List, or this List if it is already at least {@code length} long
     */
    default List<T> leftPadTo(int length, T element) {
        final int actualLength = length();
        if (length <= actualLength) {
            return this;
        } else {
            return prependAll(Iterator.continually(element).take(length - actualLength));
        }
    }

    /**
     * This List with {@code replaced} elements from {@code from} on replaced by {@code that}. A negative
     * {@code from} or {@code replaced} counts as 0.
     * <p>
     * Complexity: O(n + m) for m replacement elements.
     *
     * @param from     the first replaced position
     * @param that     the replacement elements
     * @param replaced how many elements are replaced
     * @return a new List
     * @throws NullPointerException if {@code that} is null
     */
    default List<T> patch(int from, Iterable<? extends T> that, int replaced) {
        from = Math.max(from, 0);
        replaced = Math.max(replaced, 0);
        List<T> result = take(from).appendAll(that);
        from += replaced;
        result = result.appendAll(drop(from));
        return result;
    }

    default Tuple2<List<T>, List<T>> partition(Predicate<? super T> predicate) {
        Objects.requireNonNull(predicate, "predicate is null");
        List<T> left = empty(), right = empty();
        for (T t : this) {
            if (predicate.test(t)) {
                left = left.prepend(t);
            } else {
                right = right.prepend(t);
            }
        }
        return Tuple.of(left.reverse(), right.reverse());
    }

    /**
     * Splits the elements into a left and a right side according to the {@link Either} {@code f} returns for each: the
     * generalisation of {@link #partition(Predicate)}. One pass, {@code f} called once per element in order, no
     * intermediate list of {@code Either}s.
     * <p>
     * Complexity: O(n); each side is built reversed and reversed once.
     *
     * @param f   Classifies an element
     * @param <L> Component type of the left side
     * @param <R> Component type of the right side
     * @return the left values and the right values, each in the order of the elements they come from
     * @throws NullPointerException if {@code f} is null or returns null
     */
    default <L extends @Nullable Object, R extends @Nullable Object> Tuple2<List<L>, List<R>> partitionMap(Function<? super T, ? extends Either<? extends L, ? extends R>> f) {
        Objects.requireNonNull(f, "f is null");
        List<L> lefts = empty();
        List<R> rights = empty();
        for (T element : this) {
            switch (Objects.requireNonNull(f.apply(element), "List.partitionMap: f returned null")) {
                case Either.Left(var left) -> lefts = lefts.prepend(left);
                case Either.Right(var right) -> rights = rights.prepend(right);
            }
        }
        return Tuple.of(lefts.reverse(), rights.reverse());
    }

    /**
     * Returns the head element without modifying the List.
     * <p>
     * Complexity: O(1); the head is a field of the cons cell.
     *
     * @return the first element
     * @throws java.util.NoSuchElementException if this List is empty
     */
    default T peek() {
        if (isEmpty()) {
            throw new NoSuchElementException("peek of empty list");
        }
        return head();
    }

    /**
     * Returns the head element without modifying the List.
     * <p>
     * A {@code null} head throws {@link NullPointerException}, see {@link #headOption()}.
     * <p>
     * Complexity: O(1); the head is a field of the cons cell.
     *
     * @return {@code None} if this List is empty, otherwise a {@code Some} containing the head element
     */
    default Option<T> peekOption() {
        return isEmpty() ? Option.none() : Option.some(head());
    }

    /**
     * Performs an action on the head element of this {@code List}.
     *
     * @param action A {@code Consumer}
     * @return this {@code List}
     */
    default List<T> tap(Consumer<? super T> action) {
        Objects.requireNonNull(action, "action is null");
        forEach(action);
        return this;
    }

    /**
     * All distinct permutations of the elements.
     * <p>
     * Complexity: O(n!) permutations.
     *
     * @return the permutations
     */
    default List<List<T>> permutations() {
        if (isEmpty()) {
            return Nil.instance();
        } else {
            final List<T> tail = tail();
            if (tail.isEmpty()) {
                return of(this);
            } else {
                final List<List<T>> zero = Nil.instance();
                return distinct().foldLeft(zero, (xs, x) -> {
                    final Function<List<T>, List<T>> prepend = l -> l.prepend(x);
                    return xs.appendAll(remove(x).permutations().map(prepend));
                });
            }
        }
    }

    /**
     * Removes the head element from this List.
     * <p>
     * Complexity: O(1); the tail is a field of the cons cell.
     *
     * @return the elements of this List without the head element
     * @throws java.util.NoSuchElementException if this List is empty
     */
    default List<T> pop() {
        if (isEmpty()) {
            throw new NoSuchElementException("pop of empty list");
        }
        return tail();
    }

    /**
     * Removes the head element from this List.
     * <p>
     * Complexity: O(1); the tail is a field of the cons cell.
     *
     * @return {@code None} if this List is empty, otherwise a {@code Some} containing the elements of this List without the head element
     */
    default Option<List<T>> popOption() {
        return isEmpty() ? Option.none() : Option.some(pop());
    }

    /**
     * Removes the head element from this List.
     * <p>
     * Complexity: O(1); the head and the tail are fields of the cons cell.
     *
     * @return a tuple containing the head element and the remaining elements of this List
     * @throws java.util.NoSuchElementException if this List is empty
     */
    default Tuple2<T, List<T>> pop2() {
        if (isEmpty()) {
            throw new NoSuchElementException("pop2 of empty list");
        }
        return Tuple.of(head(), tail());
    }

    /**
     * Removes the head element from this List.
     * <p>
     * Complexity: O(1); the head and the tail are fields of the cons cell.
     *
     * @return {@code None} if this List is empty, otherwise {@code Some} {@code Tuple} containing the head element and the remaining elements of this List
     */
    default Option<Tuple2<T, List<T>>> pop2Option() {
        return isEmpty() ? Option.none() : Option.some(Tuple.of(head(), pop()));
    }

    /**
     * The length of the longest prefix whose elements all satisfy {@code predicate}.
     * <p>
     * Complexity: O(k) for the k elements of that prefix.
     *
     * @param predicate the condition
     * @return the length of the prefix
     * @throws NullPointerException if {@code predicate} is null
     */
    default int prefixLength(Predicate<? super T> predicate) {
        return segmentLength(predicate, 0);
    }

    /**
     * A new List with {@code element} in front of this one.
     * <p>
     * Complexity: O(1); this List becomes the tail of one new cell.
     *
     * @param element the new head
     * @return a new List starting with the given element
     */
    default List<T> prepend(T element) {
        return new Cons<>(element, this);
    }

    /**
     * A new List with {@code elements} in front of this one, in iteration order.
     * <p>
     * Complexity: O(m) for m prepended elements; this List is shared, not copied.
     *
     * @param elements the elements to prepend
     * @return a new List starting with the given elements, or this List if there are none
     * @throws NullPointerException if {@code elements} is null
     */
    default List<T> prependAll(Iterable<? extends T> elements) {
        Objects.requireNonNull(elements, "elements is null");
        return isEmpty() ? ofAll(elements) : ofAll(elements).reverse().foldLeft(this, List::prepend);
    }

    /**
     * Pushes a new element on top of this List.
     * <p>
     * Complexity: O(1); this List becomes the tail of one new cell.
     *
     * @param element The new element
     * @return a new {@code List} instance, containing the new element on top of this List
     */
    default List<T> push(T element) {
        return new Cons<>(element, this);
    }

    /**
     * Pushes the given elements on top of this List. A List has LIFO order, i.e. the last of the given elements is
     * the first which will be retrieved.
     * <p>
     * Complexity: O(m) for m pushed elements; this List is shared, not copied.
     *
     * @param elements Elements, may be empty
     * @return a new {@code List} instance, containing the new elements on top of this List
     * @throws NullPointerException if elements is null
     */
    @SuppressWarnings("unchecked")
    default List<T> push(T ... elements) {
        Objects.requireNonNull(elements, "elements is null");
        List<T> result = this;
        for (T element : elements) {
            result = result.prepend(element);
        }
        return result;
    }

    /**
     * Pushes the given elements on top of this List. A List has LIFO order, i.e. the last of the given elements is
     * the first which will be retrieved.
     * <p>
     * Complexity: O(m) for m pushed elements; this List is shared, not copied.
     *
     * @param elements An Iterable of elements, may be empty
     * @return a new {@code List} instance, containing the new elements on top of this List
     * @throws NullPointerException if elements is null
     */
    default List<T> pushAll(Iterable<T> elements) {
        Objects.requireNonNull(elements, "elements is null");
        List<T> result = this;
        for (T element : elements) {
            result = result.prepend(element);
        }
        return result;
    }

    /**
     * This List without the first occurrence of {@code element}.
     * <p>
     * Complexity: O(k) for the k elements before the removed one; the rest of this List is shared, not copied.
     *
     * @param element the element to remove
     * @return a new List, or this List if the element is absent
     */
    default List<T> remove(T element) {
        final Deque<T> preceding = new ArrayDeque<>(size());
        List<T> result = this;
        boolean found = false;
        while (!found && !result.isEmpty()) {
            final T head = result.head();
            if (Objects.equals(head, element)) {
                found = true;
            } else {
                preceding.addFirst(head);
            }
            result = result.tail();
        }
        if (!found) {
            return this;
        }
        for (T next : preceding) {
            result = result.prepend(next);
        }
        return result;
    }

    /**
     * This List without the first element satisfying {@code predicate}.
     * <p>
     * Complexity: O(k) for the k elements before the removed one; the rest of this List is shared, not copied.
     *
     * @param predicate the condition
     * @return a new List, or this List if no element satisfies the predicate
     * @throws NullPointerException if {@code predicate} is null
     */
    default List<T> removeFirst(Predicate<T> predicate) {
        Objects.requireNonNull(predicate, "predicate is null");
        List<T> init = empty();
        List<T> tail = this;
        while (!tail.isEmpty() && !predicate.test(tail.head())) {
            init = init.prepend(tail.head());
            tail = tail.tail();
        }
        if (tail.isEmpty()) {
            return this;
        } else {
            return init.foldLeft(tail.tail(), List::prepend);
        }
    }

    /**
     * This List without the last element satisfying {@code predicate}.
     * <p>
     * Complexity: O(n); the List is reversed twice.
     *
     * @param predicate the condition
     * @return a new List, or this List if no element satisfies the predicate
     * @throws NullPointerException if {@code predicate} is null
     */
    default List<T> removeLast(Predicate<T> predicate) {
        Objects.requireNonNull(predicate, "predicate is null");
        final List<T> removedAndReversed = reverse().removeFirst(predicate);
        return removedAndReversed.length() == length() ? this : removedAndReversed.reverse();
    }

    /**
     * This List without the element at {@code index}, the elements after it shifted left.
     * <p>
     * Complexity: O(index); the cells before the removed one are copied, the rest is shared.
     *
     * @param index the position of the removed element
     * @return a new List
     * @throws IndexOutOfBoundsException if {@code index} is negative or not less than {@code length()}
     */
    default List<T> removeAt(int index) {
        if (index < 0) {
            throw new IndexOutOfBoundsException("removeAt(" + index + ")");
        }
        if (isEmpty()) {
            throw new IndexOutOfBoundsException("removeAt(" + index + ") on Nil");
        }
        List<T> init = Nil.instance();
        List<T> tail = this;
        while (index > 0 && !tail.isEmpty()) {
            init = init.prepend(tail.head());
            tail = tail.tail();
            index--;
        }
        if (index > 0 || tail.isEmpty()) {
            throw new IndexOutOfBoundsException("removeAt(" + (index + init.length()) + ") on List of length " + length());
        }
        return init.reverse().appendAll(tail.tail());
    }

    /**
     * This List without any occurrence of {@code element}.
     * <p>
     * Complexity: O(n).
     *
     * @param element the element to remove
     * @return a new List, or this List if the element is absent
     */
    default List<T> removeAll(T element) {
        return Collections.removeAll(this, element, kept -> filter(kept));
    }

    /**
     * This List without any occurrence of any of {@code elements}.
     * <p>
     * Complexity: O(n + m) for m removed elements (they are hashed once, then one filter pass).
     *
     * @param elements the elements to remove
     * @return a new List, or this List if none of them occurs
     * @throws NullPointerException if {@code elements} is null
     */
    default List<T> removeAll(Iterable<? extends T> elements) {
        return Collections.removeAll(this, elements, kept -> filter(kept));
    }

    /**
     * This List without the elements satisfying {@code predicate}.
     * <p>
     * Complexity: O(n).
     *
     * @deprecated use {@link #reject(Predicate)}
     * @param predicate the condition
     * @return a new List
     * @throws NullPointerException if {@code predicate} is null
     */
    @Deprecated
    default List<T> removeAll(Predicate<? super T> predicate) {
        Objects.requireNonNull(predicate, "predicate is null");
        return reject(predicate);
    }

    /**
     * Replaces the first occurrence of {@code currentElement} with {@code newElement}, if it exists.
     * <p>
     * Complexity: O(k) for the k elements before the replaced one; the rest of this List is shared, not copied.
     *
     * @param currentElement the element to be replaced
     * @param newElement     the replacement element
     * @return a new List with the first occurrence of {@code currentElement} replaced by {@code newElement}
     */
    default List<T> replace(T currentElement, T newElement) {
        List<T> preceding = Nil.instance();
        List<T> tail = this;
        while (!tail.isEmpty() && !Objects.equals(tail.head(), currentElement)) {
            preceding = preceding.prepend(tail.head());
            tail = tail.tail();
        }
        if (tail.isEmpty()) {
            return this;
        }
        // skip the current head element because it is replaced
        List<T> result = tail.tail().prepend(newElement);
        for (T next : preceding) {
            result = result.prepend(next);
        }
        return result;
    }

    /**
     * Replaces all occurrences of {@code currentElement} with {@code newElement}.
     * <p>
     * Complexity: O(n).
     *
     * @param currentElement the element to be replaced
     * @param newElement     the replacement element
     * @return a new List with all occurrences of {@code currentElement} replaced by {@code newElement}
     */
    default List<T> replaceAll(T currentElement, T newElement) {
        List<T> result = Nil.instance();
        boolean changed = false;
        for (List<T> list = this; !list.isEmpty(); list = list.tail()) {
            final T head = list.head();
            if (Objects.equals(head, currentElement)) {
                result = result.prepend(newElement);
                changed = true;
            } else {
                result = result.prepend(head);
            }
        }
        return changed ? result.reverse() : this;
    }

    /**
     * Retains only the elements from this List that are contained in the given {@code elements}.
     * <p>
     * Complexity: O(n + m) for m retained elements (they are hashed once, then one filter pass).
     *
     * @param elements the elements to keep
     * @return a new List containing only the elements present in {@code elements}, in their original order
     * @throws NullPointerException if {@code elements} is null
     */
    default List<T> retainAll(Iterable<? extends T> elements) {
        return Collections.retainAll(this, elements, kept -> filter(kept));
    }

    /**
     * The elements in reverse order.
     * <p>
     * Complexity: O(n).
     *
     * @return a new List, or this List if it has fewer than two elements
     */
    default List<T> reverse() {
        return (isEmpty() || tail().isEmpty()) ? this : foldLeft(empty(), List::prepend);
    }

    /**
     * Rotates the elements {@code n} positions to the left: {@code List(1, 2, 3, 4, 5).rotateLeft(2)} is
     * {@code List(3, 4, 5, 1, 2)}. A negative {@code n} rotates right; {@code n} is taken modulo the length.
     * <p>
     * Complexity: O(n); O(1) for {@code n == 0}, which is answered without walking the elements.
     *
     * @param n the distance
     * @return the rotated List, or this List if the rotation is a multiple of the length
     */
    default List<T> rotateLeft(int n) {
        // n == 0 before length(): a no-op rotation must not walk the elements, let alone force a lazy sequence
        if (n == 0 || isEmpty()) {
            return this;
        }
        final int k = Math.floorMod(n, length());
        return (k == 0) ? this : drop(k).appendAll(take(k));
    }

    /**
     * Rotates the elements {@code n} positions to the right: {@code List(1, 2, 3, 4, 5).rotateRight(2)} is
     * {@code List(4, 5, 1, 2, 3)}. A negative {@code n} rotates left; {@code n} is taken modulo the length.
     * <p>
     * Complexity: O(n); O(1) for {@code n == 0}, which is answered without walking the elements.
     *
     * @param n the distance
     * @return the rotated List, or this List if the rotation is a multiple of the length
     */
    default List<T> rotateRight(int n) {
        // n == 0 before length(): a no-op rotation must not walk the elements, let alone force a lazy sequence
        if (n == 0 || isEmpty()) {
            return this;
        }
        final int k = Math.floorMod(n, length());
        return (k == 0) ? this : takeRight(k).appendAll(dropRight(k));
    }

    /**
     * Computes a prefix scan of the elements of this List.
     * <p>
     * The neutral element {@code zero} may be applied more than once.
     * <p>
     * Complexity: O(n).
     *
     * @param zero      the neutral element for the operator
     * @param operation an associative binary operator
     * @return a new List containing the prefix scan of the elements
     * @throws NullPointerException if {@code operation} is null
     */
    default List<T> scan(T zero, BiFunction<? super T, ? super T, ? extends T> operation) {
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
     * @return a new List containing the cumulative results
     * @throws NullPointerException if {@code operation} is null
     */
    default <U extends @Nullable Object> List<U> scanLeft(U zero, BiFunction<? super U, ? super T, ? extends U> operation) {
        return Collections.scanLeft(this, zero, operation, Iterator::toList);
    }

    /**
     * Produces a collection containing cumulative results of applying the operator from right to left.
     * <p>
     * The head of the result is the last cumulative result.
     * <p>
     * Complexity: O(n); the elements are walked from the end.
     *
     * @param <U>       the type of the resulting elements
     * @param zero      the initial value
     * @param operation a binary operator applied to each element and the intermediate result
     * @return a new List containing the cumulative results
     * @throws NullPointerException if {@code operation} is null
     */
    default <U extends @Nullable Object> List<U> scanRight(U zero, BiFunction<? super T, ? super U, ? extends U> operation) {
        return Collections.scanRight(this, zero, operation, Iterator::toList);
    }

    /**
     * The position of {@code element} in this List, which must already be sorted in ascending natural order; the
     * result is undefined otherwise. The search is linear, as a cons list has no indexed access.
     * <p>
     * Complexity: O(n).
     *
     * @param element the element to find
     * @return the index of the element if it is present; otherwise {@code (-(insertion point) - 1)}, the insertion
     *         point being the index at which the element would be inserted
     * @throws ClassCastException if {@code T} is not {@code Comparable}
     */
    @SuppressWarnings("unchecked")
    default int search(T element) {
        final ToIntFunction<T> comparison = ((Comparable<T>) element)::compareTo;
        return ListModule.Search.linearSearch(this, comparison);
    }

    /**
     * The position of {@code element} in this List, which must already be sorted in ascending order according to
     * {@code comparator}; the result is undefined otherwise. The search is linear, as a cons list has no indexed
     * access.
     * <p>
     * Complexity: O(n).
     *
     * @param element    the element to find
     * @param comparator the order this List is sorted by
     * @return the index of the element if it is present; otherwise {@code (-(insertion point) - 1)}, the insertion
     *         point being the index at which the element would be inserted
     * @throws NullPointerException if {@code comparator} is null
     */
    default int search(T element, Comparator<? super T> comparator) {
        Objects.requireNonNull(comparator, "comparator is null");
        final ToIntFunction<T> comparison = current -> comparator.compare(element, current);
        return ListModule.Search.linearSearch(this, comparison);
    }

    /**
     * The length of the longest run of elements satisfying {@code predicate} starting at {@code from}.
     * <p>
     * Complexity: O(from + k) for the k elements of that run.
     *
     * @param predicate the condition
     * @param from      the first position to look at
     * @return the length of the run
     * @throws NullPointerException if {@code predicate} is null
     */
    default int segmentLength(Predicate<? super T> predicate, int from) {
        Objects.requireNonNull(predicate, "predicate is null");
        int i = 0;
        List<T> these = this.drop(from);
        while (!these.isEmpty() && predicate.test(these.head())) {
            i++;
            these = these.tail();
        }
        return i;
    }

    /**
     * The elements in a random order, drawn from a default source of randomness.
     * <p>
     * Complexity: O(n).
     *
     * @return a new List, or this List if it has fewer than two elements
     */
    default List<T> shuffle() {
        return Collections.shuffle(this, List::ofAll);
    }

    /**
     * The elements from {@code beginIndex} inclusive to {@code endIndex} exclusive, both clamped to the bounds of
     * this List.
     * <p>
     * Complexity: O(endIndex).
     *
     * @param beginIndex the first position
     * @param endIndex   the position after the last one
     * @return a new List, empty if the range is empty
     */
    default List<T> slice(int beginIndex, int endIndex) {
        if (beginIndex >= endIndex || beginIndex >= length() || isEmpty()) {
            return empty();
        } else {
            List<T> result = Nil.instance();
            List<T> list = this;
            final long lowerBound = Math.max(beginIndex, 0);
            final long upperBound = Math.min(endIndex, length());
            for (int i = 0; i < upperBound; i++) {
                if (i >= lowerBound) {
                    result = result.prepend(list.head());
                }
                list = list.tail();
            }
            return result.reverse();
        }
    }

    /**
     * The elements in ascending natural order (a stable sort).
     * <p>
     * Complexity: O(n log n) comparisons.
     *
     * @return a new sorted List, or this List if it is empty
     * @throws ClassCastException if {@code T} is not {@code Comparable}
     */
    default List<T> sorted() {
        return isEmpty() ? this : stream().sorted().collect(collector());
    }

    /**
     * The elements in the order of {@code comparator} (a stable sort).
     * <p>
     * Complexity: O(n log n) comparisons.
     *
     * @param comparator the order
     * @return a new sorted List, or this List if it is empty
     * @throws NullPointerException if {@code comparator} is null
     */
    default List<T> sorted(Comparator<? super T> comparator) {
        Objects.requireNonNull(comparator, "comparator is null");
        return isEmpty() ? this : stream().sorted(comparator).collect(collector());
    }

    /**
     * The elements sorted by the natural order of the key {@code mapper} computes (a stable sort).
     * <p>
     * Complexity: O(n log n) comparisons; the key is recomputed at every comparison.
     *
     * @param mapper computes the sort key
     * @param <U>    the key type
     * @return a new sorted List, or this List if it is empty
     * @throws NullPointerException if {@code mapper} is null
     */
    default <U extends Comparable<? super U>> List<T> sortBy(Function<? super T, ? extends U> mapper) {
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
     * @return a new sorted List, or this List if it is empty
     * @throws NullPointerException if {@code comparator} or {@code mapper} is null
     */
    default <U extends @Nullable Object> List<T> sortBy(Comparator<? super U> comparator, Function<? super T, ? extends U> mapper) {
        Objects.requireNonNull(comparator, "comparator is null");
        Objects.requireNonNull(mapper, "mapper is null");
        return sorted((e1, e2) -> comparator.compare(mapper.apply(e1), mapper.apply(e2)));
    }

    /**
     * Splits this {@code List} into a prefix and remainder according to the given {@code predicate}.
     * <p>
     * The first element of the returned {@code Tuple} is the longest prefix of elements satisfying {@code predicate},
     * and the second element is the remaining elements.
     * <p>
     * Complexity: O(n).
     *
     * @param predicate a predicate used to determine the prefix
     * @return a {@code Tuple} containing the prefix and remainder
     * @throws NullPointerException if {@code predicate} is null
     */
    default Tuple2<List<T>, List<T>> span(Predicate<? super T> predicate) {
        Objects.requireNonNull(predicate, "predicate is null");
        final Tuple2<Iterator<T>, Iterator<T>> itt = Iterator.ofAll(this).span(predicate);
        return Tuple.of(ofAll(itt._1()), ofAll(itt._2()));
    }

    /**
     * This List split in two at position {@code n}: the first {@code n} elements and the rest.
     * <p>
     * Complexity: O(n); the prefix is copied, the suffix is shared.
     *
     * @param n the position of the split
     * @return the prefix and the suffix
     */
    default Tuple2<List<T>, List<T>> splitAt(int n) {
        if (isEmpty()) {
            return Tuple.of(empty(), empty());
        } else {
            List<T> init = Nil.instance();
            List<T> tail = this;
            while (n > 0 && !tail.isEmpty()) {
                init = init.prepend(tail.head());
                tail = tail.tail();
                n--;
            }
            return Tuple.of(init.reverse(), tail);
        }
    }

    /**
     * This List split in two before the first element satisfying {@code predicate}. If no element satisfies it, the
     * whole List is the first part.
     * <p>
     * Complexity: O(k) for the k elements before the split; the suffix is shared.
     *
     * @param predicate the condition
     * @return the prefix and the suffix
     */
    default Tuple2<List<T>, List<T>> splitAt(Predicate<? super T> predicate) {
        if (isEmpty()) {
            return Tuple.of(empty(), empty());
        } else {
            final Tuple2<List<T>, List<T>> t = SplitAt.splitByPredicateReversed(this, predicate);
            if (t._2().isEmpty()) {
                return Tuple.of(this, empty());
            } else {
                return Tuple.of(t._1().reverse(), t._2());
            }
        }
    }

    /**
     * This List split in two after the first element satisfying {@code predicate}. If no element satisfies it, the
     * whole List is the first part.
     * <p>
     * Complexity: O(k) for the k elements up to the split; the suffix is shared.
     *
     * @param predicate the condition
     * @return the prefix including the matching element, and the suffix
     */
    default Tuple2<List<T>, List<T>> splitAtInclusive(Predicate<? super T> predicate) {
        if (isEmpty()) {
            return Tuple.of(empty(), empty());
        } else {
            final Tuple2<List<T>, List<T>> t = SplitAt.splitByPredicateReversed(this, predicate);
            if (t._2().isEmpty() || t._2().tail().isEmpty()) {
                return Tuple.of(this, empty());
            } else {
                return Tuple.of(t._1().prepend(t._2().head()).reverse(), t._2().tail());
            }
        }
    }

    /**
     * Whether this List starts with {@code that}: {@code startsWith(that, 0)}.
     * <p>
     * Complexity: O(m) for m elements of {@code that}.
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
     * Complexity: O(offset + m) for m elements of {@code that}.
     *
     * @param that   the prefix to test
     * @param offset the position in this List at which the prefix should start
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
     * The elements from {@code beginIndex} on.
     * <p>
     * Complexity: O(beginIndex); the result shares the cells of this List.
     *
     * @param beginIndex the first position
     * @return a new List
     * @throws IndexOutOfBoundsException if {@code beginIndex} is negative or greater than {@code length()}
     */
    default List<T> subSequence(int beginIndex) {
        if (beginIndex < 0 || beginIndex > length()) {
            throw new IndexOutOfBoundsException("subSequence(" + beginIndex + ")");
        } else {
            return drop(beginIndex);
        }
    }

    /**
     * The elements from {@code beginIndex} inclusive to {@code endIndex} exclusive.
     * <p>
     * Complexity: O(endIndex).
     *
     * @param beginIndex the first position
     * @param endIndex   the position after the last one
     * @return a new List
     * @throws IndexOutOfBoundsException if the range is not within {@code [0, length()]}
     * @throws IllegalArgumentException  if {@code beginIndex} is greater than {@code endIndex}
     */
    default List<T> subSequence(int beginIndex, int endIndex) {
        Collections.subSequenceRangeCheck(beginIndex, endIndex, length());
        if (beginIndex == endIndex) {
            return empty();
        } else if (beginIndex == 0 && endIndex == length()) {
            return this;
        } else {
            List<T> result = Nil.instance();
            List<T> list = this;
            for (int i = 0; i < endIndex; i++, list = list.tail()) {
                if (i >= beginIndex) {
                    result = result.prepend(list.head());
                }
            }
            return result.reverse();
        }
    }

    /**
     * Returns a new {@code List} without its first element.
     * <p>
     * Complexity: O(1); the tail is a field of the cons cell.
     *
     * @return a new {@code List} containing all elements except the first
     * @throws UnsupportedOperationException if this {@code List} is empty
     */
    List<T> tail();

    /**
     * Returns a new {@code List} without its first element as an {@code Option}.
     * <p>
     * Complexity: O(1); the tail is a field of the cons cell.
     *
     * @return {@code Some(traversable)} if non-empty, otherwise {@code None}
     */
    default Option<List<T>> tailOption() {
        return isEmpty() ? Option.none() : Option.some(tail());
    }

    /**
     * Returns the first {@code n} elements of this {@code List}, or all elements if {@code n} exceeds the length.
     * <p>
     * If {@code n < 0}, an empty instance is returned. If {@code n > length()}, the full instance is returned.
     * <p>
     * Complexity: O(n) for n taken elements; the prefix is copied.
     *
     * @param n the number of elements to take
     * @return a new {@code List} containing the first {@code n} elements
     */
    default List<T> take(int n) {
        if (n <= 0) {
            return empty();
        }
        if (n >= length()) {
            return this;
        }
        List<T> result = Nil.instance();
        List<T> list = this;
        for (int i = 0; i < n; i++, list = list.tail()) {
            result = result.prepend(list.head());
        }
        return result.reverse();
    }

    /**
     * Takes elements from this {@code List} until the given predicate holds for an element.
     * <p>
     * Equivalent to {@code takeWhile(predicate.negate())}, but useful when using method references
     * that cannot be negated directly.
     * <p>
     * Complexity: O(k) for the k taken elements.
     *
     * @param predicate a condition tested sequentially on the elements
     * @return a new {@code List} containing all elements before the first one that satisfies the predicate
     * @throws NullPointerException if {@code predicate} is null
     */
    default List<T> takeUntil(Predicate<? super T> predicate) {
        Objects.requireNonNull(predicate, "predicate is null");
        return takeWhile(predicate.negate());
    }

    /**
     * Takes elements from this {@code List} while the given predicate holds.
     * <p>
     * Complexity: O(k) for the k taken elements.
     *
     * @param predicate a condition tested sequentially on the elements
     * @return a new {@code List} containing all elements up to (but not including) the first one
     *         that does not satisfy the predicate
     * @throws NullPointerException if {@code predicate} is null
     */
    default List<T> takeWhile(Predicate<? super T> predicate) {
        Objects.requireNonNull(predicate, "predicate is null");
        List<T> result = Nil.instance();
        for (List<T> list = this; !list.isEmpty() && predicate.test(list.head()); list = list.tail()) {
            result = result.prepend(list.head());
        }
        return result.length() == length() ? this : result.reverse();
    }

    /**
     * Returns the last {@code n} elements of this {@code List}, or all elements if {@code n} exceeds the length.
     * <p>
     * If {@code n < 0}, an empty instance is returned. If {@code n > length()}, the full instance is returned.
     * <p>
     * Complexity: O(n); the List is reversed twice.
     *
     * @param n the number of elements to take from the end
     * @return a new {@code List} containing the last {@code n} elements
     */
    default List<T> takeRight(int n) {
        if (n <= 0) {
            return empty();
        }
        if (n >= length()) {
            return this;
        }
        return reverse().take(n).reverse();
    }

    /**
     * The longest suffix whose elements, from the end, do not satisfy {@code predicate}.
     * <p>
     * Complexity: O(n); the List is reversed twice.
     *
     * @param predicate the condition, tested from the end
     * @return a new List
     * @throws NullPointerException if {@code predicate} is null
     */
    default List<T> takeRightUntil(Predicate<? super T> predicate) {
        Objects.requireNonNull(predicate, "predicate is null");
        return takeRightWhile(predicate.negate());
    }

    /**
     * The longest suffix whose elements, from the end, all satisfy {@code predicate}.
     * <p>
     * Complexity: O(n); the List is reversed twice.
     *
     * @param predicate the condition, tested from the end
     * @return a new List
     * @throws NullPointerException if {@code predicate} is null
     */
    default List<T> takeRightWhile(Predicate<? super T> predicate) {
        Objects.requireNonNull(predicate, "predicate is null");
        return reverse().takeWhile(predicate).reverse();
    }

    default <T1 extends @Nullable Object, T2 extends @Nullable Object> Tuple2<List<T1>, List<T2>> unzip(
      Function<? super T, Tuple2<? extends T1, ? extends T2>> unzipper) {
        Objects.requireNonNull(unzipper, "unzipper is null");
        List<T1> xs = Nil.instance();
        List<T2> ys = Nil.instance();
        for (T element : this) {
            final Tuple2<? extends T1, ? extends T2> t = unzipper.apply(element);
            xs = xs.prepend(t._1());
            ys = ys.prepend(t._2());
        }
        return Tuple.of(xs.reverse(), ys.reverse());
    }

    default <T1 extends @Nullable Object, T2 extends @Nullable Object, T3 extends @Nullable Object> Tuple3<List<T1>, List<T2>, List<T3>> unzip3(
      Function<? super T, Tuple3<? extends T1, ? extends T2, ? extends T3>> unzipper) {
        Objects.requireNonNull(unzipper, "unzipper is null");
        List<T1> xs = Nil.instance();
        List<T2> ys = Nil.instance();
        List<T3> zs = Nil.instance();
        for (T element : this) {
            final Tuple3<? extends T1, ? extends T2, ? extends T3> t = unzipper.apply(element);
            xs = xs.prepend(t._1());
            ys = ys.prepend(t._2());
            zs = zs.prepend(t._3());
        }
        return Tuple.of(xs.reverse(), ys.reverse(), zs.reverse());
    }

    /**
     * This List with the element at {@code index} replaced by {@code element}.
     * <p>
     * Complexity: O(index); the cells before it are copied, the rest is shared.
     *
     * @param index   the position to update
     * @param element the new element
     * @return a new List
     * @throws IndexOutOfBoundsException if {@code index} is negative or not less than {@code length()}
     */
    default List<T> update(int index, T element) {
        if (isEmpty()) {
            throw new IndexOutOfBoundsException("update(" + index + ", e) on Nil");
        }
        if (index < 0) {
            throw new IndexOutOfBoundsException("update(" + index + ", e)");
        }
        List<T> preceding = Nil.instance();
        List<T> tail = this;
        for (int i = index; i > 0; i--, tail = tail.tail()) {
            if (tail.isEmpty()) {
                throw new IndexOutOfBoundsException("update(" + index + ", e) on List of length " + length());
            }
            preceding = preceding.prepend(tail.head());
        }
        if (tail.isEmpty()) {
            throw new IndexOutOfBoundsException("update(" + index + ", e) on List of length " + length());
        }
        // skip the current head element because it is replaced
        List<T> result = tail.tail().prepend(element);
        for (T next : preceding) {
            result = result.prepend(next);
        }
        return result;
    }

    /**
     * This List with the element at {@code index} replaced by what {@code updater} computes from it.
     * <p>
     * Complexity: O(index); the element is read, then the cells before it are copied.
     *
     * @param index   the position to update
     * @param updater computes the new element from the current one
     * @return a new List
     * @throws IndexOutOfBoundsException if {@code index} is negative or not less than {@code length()}
     * @throws NullPointerException      if {@code updater} is null
     */
    default List<T> update(int index, Function<? super T, ? extends T> updater) {
        Objects.requireNonNull(updater, "updater is null");
        return update(index, updater.apply(get(index)));
    }

    /**
     * Returns a {@code List} formed by pairing elements of this {@code List} with elements of another
     * {@code Iterable}. Pairing stops when either collection runs out of elements; any remaining elements in the longer
     * collection are ignored.
     * <p>
     * The length of the resulting {@code List} is the minimum of the lengths of this {@code List} and
     * {@code that}.
     * <p>
     * Complexity: O(min(n, m)) for an argument of m elements.
     *
     * @param <U>  the type of elements in the second half of each pair
     * @param that an {@code Iterable} providing the second element of each pair
     * @return a new {@code List} containing pairs of corresponding elements
     * @throws NullPointerException if {@code that} is null
     */
    default <U extends @Nullable Object> List<Tuple2<T, U>> zip(Iterable<? extends U> that) {
        return zipWith(that, Tuple::of);
    }

    /**
     * Returns a {@code List} by combining elements of this {@code List} with elements of another
     * {@code Iterable} using a mapping function. Pairing stops when either collection runs out of elements.
     * <p>
     * The length of the resulting {@code List} is the minimum of the lengths of this {@code List} and
     * {@code that}.
     * <p>
     * Complexity: O(min(n, m)) for an argument of m elements.
     *
     * @param <U>    the type of elements in the second parameter of the mapper
     * @param <R>    the type of elements in the resulting {@code List}
     * @param that   an {@code Iterable} providing the second parameter of the mapper
     * @param mapper a function that combines elements from this and {@code that} into a new element
     * @return a new {@code List} containing mapped elements
     * @throws NullPointerException if {@code that} or {@code mapper} is null
     */
    default <U extends @Nullable Object, R extends @Nullable Object> List<R> zipWith(Iterable<? extends U> that, BiFunction<? super T, ? super U, ? extends R> mapper) {
        Objects.requireNonNull(that, "that is null");
        Objects.requireNonNull(mapper, "mapper is null");
        return ofAll(Iterator.ofAll(this).zipWith(that, mapper));
    }

    /**
     * Returns a {@code List} formed by pairing elements of this {@code List} with elements of another
     * {@code Iterable}, filling in placeholder elements when one collection is shorter than the other.
     * <p>
     * The length of the resulting {@code List} is the maximum of the lengths of this {@code List} and
     * {@code that}.
     * <p>
     * If this {@code List} is shorter than {@code that}, {@code thisElem} is used as a filler. Conversely, if
     * {@code that} is shorter, {@code thatElem} is used.
     * <p>
     * Complexity: O(max(n, m)) for an argument of m elements.
     *
     * @param <U>      the type of elements in the second half of each pair
     * @param that     an {@code Iterable} providing the second element of each pair
     * @param thisElem the element used to fill missing values if this {@code List} is shorter than {@code that}
     * @param thatElem the element used to fill missing values if {@code that} is shorter than this {@code List}
     * @return a new {@code List} containing pairs of elements, including fillers as needed
     * @throws NullPointerException if {@code that} is null
     */
    default <U extends @Nullable Object> List<Tuple2<T, U>> zipAll(Iterable<? extends U> that, T thisElem, U thatElem) {
        Objects.requireNonNull(that, "that is null");
        return ofAll(Iterator.ofAll(this).zipAll(that, thisElem, thatElem));
    }

    /**
     * Zips this {@code List} with its indices, starting at 0.
     * <p>
     * Complexity: O(n).
     *
     * @return a new {@code List} containing each element paired with its index
     */
    default List<Tuple2<T, Integer>> zipWithIndex() {
        return zipWithIndex(Tuple::of);
    }

    /**
     * Zips this {@code List} with its indices and maps the resulting pairs using the provided mapper.
     * <p>
     * Complexity: O(n).
     *
     * @param <U>    the type of elements in the resulting {@code List}
     * @param mapper a function mapping an element and its index to a new element
     * @return a new {@code List} containing the mapped elements
     * @throws NullPointerException if {@code mapper} is null
     */
    default <U extends @Nullable Object> List<U> zipWithIndex(BiFunction<? super T, ? super Integer, ? extends U> mapper) {
        Objects.requireNonNull(mapper, "mapper is null");
        return ofAll(Iterator.ofAll(this).zipWithIndex(mapper));
    }

    /**
     * The empty {@code List}. {@link List#empty()} and {@link #instance()} return a shared instance; {@code new Nil<>()}
     * is legal, a record constructor is public, and equal to it.
     * <p>
     * Equality is that of every {@code List}: a {@code Nil} equals any empty sequence, not only another {@code Nil},
     * so {@code equals} and {@code hashCode} are not the record defaults.
     *
     * @param <T> Component type of the List.
     */
    record Nil<T extends @Nullable Object>() implements List<T> {

        private static final Nil<?> INSTANCE = new Nil<>();

        /**
         * Returns the shared instance of the empty list.
         *
         * @param <T> Component type of the List
         * @return the shared instance of the empty list.
         */
        @SuppressWarnings("unchecked")
        public static <T extends @Nullable Object> Nil<T> instance() {
            return (Nil<T>) INSTANCE;
        }

        @Override
        public T head() {
            throw new NoSuchElementException("head of empty list");
        }

        @Override
        public int length() {
            return 0;
        }

        @Override
        public List<T> tail() {
            throw new UnsupportedOperationException("tail of empty list");
        }

        @Override
        public boolean isEmpty() {
            return true;
        }

        @Override
        public boolean equals(@Nullable Object o) {
            return Collections.equals(this, o);
        }

        @Override
        public int hashCode() {
            return Collections.hashOrdered(this);
        }

        @Override
        public String toString() {
            return "List()";
        }
    }

    /**
     * A non-empty {@code List}: a {@code head} element and a {@code tail} {@code List}. Neither may be
     * {@code null}: every construction and insertion path on {@code List} funnels through this
     * constructor, so it is the single boundary that rejects a null element.
     * <p>
     * Equality is that of every {@code List}: a {@code Cons} equals any sequence with the same elements in the same
     * order, so {@code equals}, {@code hashCode} and {@code toString} are not the record defaults.
     *
     * @param head the first element, never {@code null}
     * @param tail the remaining elements, never {@code null}
     * @param <T>  Component type of the List.
     */
    record Cons<T extends @Nullable Object>(T head, List<T> tail) implements List<T> {

        /**
         * Rejects a {@code null} head or tail.
         *
         * @throws NullPointerException if {@code head} or {@code tail} is null
         */
        public Cons {
            Objects.requireNonNull(head, "List: element is null");
            Objects.requireNonNull(tail, "tail is null");
        }

        @Override
        public int length() {
            // Walks the list: a record has no field for a cached length. Scala's List does the same;
            // a length component would leak into every record pattern and allow inconsistent instances.
            int length = 0;
            for (List<T> list = this; !list.isEmpty(); list = list.tail()) {
                length++;
            }
            return length;
        }

        @Override
        public boolean isEmpty() {
            return false;
        }

        @Override
        public boolean equals(@Nullable Object o) {
            return Collections.equals(this, o);
        }

        @Override
        public int hashCode() {
            return Collections.hashOrdered(this);
        }

        @Override
        public String toString() {
            return mkString("List(", ", ", ")");
        }
    }

    /**
     * The first element.
     * <p>
     * Complexity: O(1).
     *
     * @return the head of this List
     * @throws NoSuchElementException if this List is empty
     */
    T head();

    /**
     * An iterator over the elements, from the head on.
     * <p>
     * Complexity: O(1) to create, O(1) per step.
     *
     * @return a new iterator
     */
    @Override
    default java.util.Iterator<T> iterator() {
        final List<T> that = this;
        return new AbstractIterator<T>() {
            List<T> list = that;

            @Override
            public boolean hasNext() {
                return !list.isEmpty();
            }

            @Override
            public T getNext() {
                final T result = list.head();
                list = list.tail();
                return result;
            }
        };
    }

    // -- windows and products

    /**
     * The elements in consecutive blocks of {@code size}: {@code List.of(1, 2, 3, 4, 5).grouped(2)} is
     * {@code List(List(1, 2), List(3, 4), List(5))}; the last block is smaller when {@code size} does not divide
     * the length. The same as {@code sliding(size, size)}.
     * <p>
     * Complexity: O(n); each block is copied into its own List.
     *
     * @param size the block size, positive
     * @return the blocks, in order; empty if this List is empty
     * @throws IllegalArgumentException if {@code size} is not positive
     */
    default List<List<T>> grouped(int size) {
        return sliding(size, size);
    }

    /**
     * The windows of {@code size} consecutive elements, each starting one element after the previous:
     * {@code List.of(1, 2, 3, 4).sliding(3)} is {@code List(List(1, 2, 3), List(2, 3, 4))}. A List shorter than
     * {@code size} is one window. The same as {@code sliding(size, 1)}.
     * <p>
     * Complexity: O(n * size); each window is copied into its own List.
     *
     * @param size the window size, positive
     * @return the windows, in order; empty if this List is empty
     * @throws IllegalArgumentException if {@code size} is not positive
     */
    default List<List<T>> sliding(int size) {
        return sliding(size, 1);
    }

    /**
     * The windows of {@code size} consecutive elements, each starting {@code step} elements after the previous:
     * {@code List.of(1, 2, 3, 4, 5).sliding(2, 3)} is {@code List(List(1, 2), List(4, 5))} and
     * {@code sliding(2, 4)} is {@code List(List(1, 2), List(5))}. The last window is shorter than {@code size}
     * when it reaches the end; a window whose elements all belong to the previous one is not produced, so
     * {@code List.of(1, 2, 3, 4).sliding(3)} has two windows. A List shorter than {@code size} is one window; an
     * empty List has none.
     * <p>
     * Complexity: O(n * size / step); each window is copied into its own List.
     *
     * @param size the window size, positive
     * @param step the distance between two window starts, positive
     * @return the windows, in order
     * @throws IllegalArgumentException if {@code size} or {@code step} is not positive
     */
    default List<List<T>> sliding(int size, int step) {
        return ofAll(Iterator.ofAll(this).sliding(size, step).map(List::ofAll));
    }

    /**
     * The elements in maximal runs of consecutive elements with the same key, computed once per element by
     * {@code classifier}: {@code List.of(1, 2, 3, 10, 12, 5, 7, 20, 29).slideBy(x -> x / 10)} is
     * {@code List(List(1, 2, 3), List(10, 12), List(5, 7), List(20, 29))}. The runs concatenate back to this List.
     * <p>
     * Complexity: O(n); each run is copied into its own List.
     *
     * @param classifier the key of an element; two consecutive elements are in the same run when their keys are
     *                   equal
     * @return the runs, in order; empty if this List is empty
     * @throws NullPointerException if {@code classifier} is null
     */
    default List<List<T>> slideBy(Function<? super T, ?> classifier) {
        Objects.requireNonNull(classifier, "classifier is null");
        return ofAll(Iterator.ofAll(this).slideBy(classifier).map(List::ofAll));
    }

    /**
     * The Cartesian square of this List: every pair {@code (a, b)} of elements, {@code a} varying slowest.
     * <p>
     * Complexity: O(n^2); the pairs are built now.
     *
     * @return the pairs
     */
    default List<Tuple2<T, T>> crossProduct() {
        return crossProduct(this);
    }

    /**
     * The Cartesian power of this List: every List of {@code power} elements drawn from this one, in lexicographic
     * position order. {@code power == 0} gives one empty List; a negative power gives no result.
     * <p>
     * Complexity: O(n^power) Lists of size {@code power}, built now.
     *
     * @param power the size of each result
     * @return the Lists
     */
    default List<List<T>> crossProduct(int power) {
        if (power < 0) {
            return empty();
        }
        List<List<T>> product = List.of(List.<T> empty());
        for (int i = 0; i < power; i++) {
            product = product.flatMap(el -> map(el::append));
        }
        return product;
    }

    /**
     * The Cartesian product of this List and {@code that}: every pair {@code (a, b)} with {@code a} from this List
     * and {@code b} from {@code that}, {@code a} varying slowest. {@code that} is walked once.
     * <p>
     * Complexity: O(n * m) for m elements of {@code that}; the pairs are built now.
     *
     * @param that the right-hand elements
     * @param <U>  their type
     * @return the pairs
     * @throws NullPointerException if {@code that} is null
     */
    default <U extends @Nullable Object> List<Tuple2<T, U>> crossProduct(Iterable<? extends U> that) {
        Objects.requireNonNull(that, "that is null");
        final List<U> other = List.ofAll(that);
        return flatMap(a -> other.map(b -> Tuple.of(a, b)));
    }

    /**
     * Combines the elements from the right: the last with the one before it, the result with the one before that,
     * and so on.
     * <p>
     * Complexity: O(n); the List is reversed first.
     *
     * @param op combines the next element and the result so far
     * @return the combined result
     * @throws NoSuchElementException if this List is empty
     * @throws NullPointerException   if {@code op} is null
     */
    default T reduceRight(BiFunction<? super T, ? super T, ? extends T> op) {
        Objects.requireNonNull(op, "op is null");
        if (isEmpty()) {
            throw new NoSuchElementException("reduceRight on empty List");
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
     * Complexity: O(n), every element compared once.
     *
     * @return {@code Some(maximum)} if there is an element, {@code None} otherwise
     * @throws ClassCastException if two or more elements are not {@code Comparable}
     */
    default Option<T> max() {
        return TraversableModule.max(this);
    }

    /**
     * The greatest element according to {@code comparator}; of equal greatest elements, the first in this
     * List's order.
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
     * element in this List's order.
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
     * Complexity: O(n), every element compared once.
     *
     * @return {@code Some(minimum)} if there is an element, {@code None} otherwise
     * @throws ClassCastException if two or more elements are not {@code Comparable}
     */
    default Option<T> min() {
        return TraversableModule.min(this);
    }

    /**
     * The least element according to {@code comparator}; of equal least elements, the first in this List's
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
     * this List's order.
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
     * @return the folded result, {@code zero} on an empty List
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
     * @throws NoSuchElementException if this List is empty
     * @throws NullPointerException   if {@code op} is null
     */
    default T reduce(BiFunction<? super T, ? super T, ? extends T> op) {
        return TraversableModule.reduceLeft(this, op);
    }

    /**
     * {@link #reduce(BiFunction)} as an {@code Option}: {@code None} on an empty List.
     *
     * @param op combines two elements
     * @return {@code Some(result)}, or {@code None} if this List is empty
     * @throws NullPointerException if {@code op} is null
     */
    default Option<T> reduceOption(BiFunction<? super T, ? super T, ? extends T> op) {
        return TraversableModule.reduceLeftOption(this, op);
    }

    /**
     * The only element.
     *
     * @return the element
     * @throws NoSuchElementException if this List is empty or has more than one element
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
     * from the first element. {@code 0} on an empty List.
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
     * {@code 1} on an empty List.
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
     * Complexity: O(1), that of {@link #head()}.
     *
     * @return {@code Some(head)}, or {@code None} if this List is empty
     */
    default Option<T> headOption() {
        return isEmpty() ? Option.none() : Option.some(head());
    }

    /**
     * The last element as an {@code Option}.
     * <p>
     * Complexity: O(n), that of {@link #last()}.
     *
     * @return {@code Some(last)}, or {@code None} if this List is empty
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
     * @throws NoSuchElementException if this List is empty
     * @throws NullPointerException   if {@code op} is null
     */
    default T reduceLeft(BiFunction<? super T, ? super T, ? extends T> op) {
        return TraversableModule.reduceLeft(this, op);
    }

    /**
     * {@link #reduceLeft(BiFunction)} as an {@code Option}: {@code None} on an empty List.
     *
     * @param op combines the result so far and the next element
     * @return {@code Some(result)}, or {@code None} if this List is empty
     * @throws NullPointerException if {@code op} is null
     */
    default Option<T> reduceLeftOption(BiFunction<? super T, ? super T, ? extends T> op) {
        return TraversableModule.reduceLeftOption(this, op);
    }

    /**
     * {@link #reduceRight(BiFunction)} as an {@code Option}: {@code None} on an empty List.
     *
     * @param op combines the next element and the result so far
     * @return {@code Some(result)}, or {@code None} if this List is empty
     * @throws NullPointerException if {@code op} is null
     */
    default Option<T> reduceRightOption(BiFunction<? super T, ? super T, ? extends T> op) {
        Objects.requireNonNull(op, "op is null");
        return isEmpty() ? Option.none() : Option.some(reduceRight(op));
    }

    /**
     * The number of elements; the same as {@link #length()}.
     * <p>
     * Complexity: O(n), that of {@link #length()}.
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
     * The elements as the entries of a new {@link HashMap}, each mapped to a key by {@code keyMapper} and to a
     * value by {@code valueMapper}; of two entries with the same key, the later one in this List's order wins.
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
     * entries with the same key, the later one in this List's order wins.
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
     * The elements as the entries of a new {@link LinkedHashMap}, in this List's order, each mapped to a key by
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
     * The elements as the entries of a new {@link LinkedHashMap}, in this List's order, each mapped to a key
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
     * in this List's order wins.
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
     * and a value by {@code f}; of two entries with the same key, the later one in this List's order wins.
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
     * this List's order wins.
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
     * a value by {@code f}; of two entries with the same key, the later one in this List's order wins.
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
     * The elements as a {@link Queue}, in this List's order.
     *
     * @return a {@code Queue} of the elements
     */
    default Queue<T> toQueue() {
        return TraversableModule.toTraversable(this, Queue.empty(), Queue::ofAll);
    }

    /**
     * The distinct elements as a {@link LinkedHashSet}, in this List's order.
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
     * The elements as a {@link Stream}, in this List's order.
     *
     * @return a {@code Stream} of the elements
     */
    default Stream<T> toStream() {
        return TraversableModule.toTraversable(this, Stream.empty(), Stream::ofAll);
    }

}
