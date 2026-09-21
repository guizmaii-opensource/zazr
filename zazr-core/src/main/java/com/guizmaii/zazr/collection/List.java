package com.guizmaii.zazr.collection;

import com.guizmaii.zazr.Tuple;
import com.guizmaii.zazr.Tuple2;
import com.guizmaii.zazr.Tuple3;
import com.guizmaii.zazr.collection.List.Nil;
import com.guizmaii.zazr.collection.ListModule.Combinations;
import com.guizmaii.zazr.collection.ListModule.SplitAt;
import com.guizmaii.zazr.control.Option;
import java.io.*;
import java.util.*;
import java.util.function.*;
import java.util.stream.Collector;
import org.jspecify.annotations.Nullable;

import static com.guizmaii.zazr.collection.JavaConverters.ChangePolicy.IMMUTABLE;
import static com.guizmaii.zazr.collection.JavaConverters.ChangePolicy.MUTABLE;
import static com.guizmaii.zazr.collection.JavaConverters.ListView;

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
        } else if (elements instanceof ListView
                && ((ListView<T, ?>) elements).getDelegate() instanceof List) {
            return (List<T>) ((ListView<T, ?>) elements).getDelegate();
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
     * Returns an immutable {@link java.util.List} view of this List: reads go through to this List, mutators throw
     * {@link UnsupportedOperationException}.
     * <p>
     * Complexity: O(1); {@code get(i)} on the view is O(i).
     *
     * @return an immutable {@code java.util.List} view
     */
    default java.util.List<T> asJava() {
        return JavaConverters.asJava(this, IMMUTABLE);
    }

    /**
     * Passes an immutable {@link java.util.List} view of this List to {@code action} and returns this List.
     * <p>
     * Complexity: O(1) to create the view.
     *
     * @param action receives the view
     * @return this List
     * @throws NullPointerException if {@code action} is null
     * @see #asJava()
     */
    default List<T> asJava(Consumer<? super java.util.List<T>> action) {
        Objects.requireNonNull(action, "action is null");
        action.accept(asJava());
        return this;
    }

    /**
     * Returns a mutable {@link java.util.List} view of this List: every mutator replaces the view's underlying List
     * by a new one; this List is never modified.
     * <p>
     * Complexity: O(1); each mutator costs what the corresponding List operation costs.
     *
     * @return a mutable {@code java.util.List} view
     */
    default java.util.List<T> asJavaMutable() {
        return JavaConverters.asJava(this, MUTABLE);
    }

    /**
     * Passes a mutable {@link java.util.List} view of this List to {@code action} and returns the List the view holds
     * afterwards: this List if the action only read, a new one reflecting the writes otherwise.
     * <p>
     * Complexity: O(1) to create the view.
     *
     * @param action receives the view
     * @return this List, or a new List reflecting the modifications made through the view
     * @throws NullPointerException if {@code action} is null
     * @see #asJavaMutable()
     */
    default List<T> asJavaMutable(Consumer<? super java.util.List<T>> action) {
        Objects.requireNonNull(action, "action is null");
        final ListView<T, List<T>> view = JavaConverters.asJava(this, MUTABLE);
        action.accept(view);
        return view.getDelegate();
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
     * Complexity: O(n choose k) combinations.
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
     * The Cartesian square of this List: every pair {@code (a, b)} of elements, {@code a} varying slowest.
     * <p>
     * Complexity: lazy; O(n^2) pairs when consumed.
     *
     * @return an iterator over the pairs
     */
    default Iterator<Tuple2<T, T>> crossProduct() {
        return crossProduct(this);
    }

    /**
     * The Cartesian power of this List: every List of {@code power} elements drawn from this one, in lexicographic
     * position order. {@code power == 0} gives one empty List; a negative power gives no result.
     * <p>
     * Complexity: lazy; O(n^power) Lists of size {@code power} when consumed.
     *
     * @param power the size of each result
     * @return an iterator over the Lists
     */
    default Iterator<List<T>> crossProduct(int power) {
        if (power < 0) {
            return Iterator.empty();
        }
        return Iterator.range(0, power).foldLeft(Iterator.of(List.<T> empty()), (product, ignored) -> product.flatMap(el -> map(el::append)));
    }

    /**
     * The Cartesian product of this List and {@code that}: every pair {@code (a, b)} with {@code a} from this List
     * and {@code b} from {@code that}, {@code a} varying slowest. {@code that} is walked lazily and memoised, so an
     * infinite {@code that} works with {@code take}.
     * <p>
     * Complexity: lazy; O(n * m) pairs when consumed.
     *
     * @param that the right-hand elements
     * @param <U>  their type
     * @return an iterator over the pairs
     * @throws NullPointerException if {@code that} is null
     */
    default <U extends @Nullable Object> Iterator<Tuple2<T, U>> crossProduct(Iterable<? extends U> that) {
        Objects.requireNonNull(that, "that is null");
        // a lazy, memoising Stream: the result is lazy, so the argument stays lazy too
        final Stream<U> other = Stream.ofAll(that);
        return Iterator.ofAll(this).flatMap(a -> other.map(b -> Tuple.of(a, b)));
    }

    /**
     * {@inheritDoc}
     * <p>
     * Complexity: O(n).
     */
    @Override
    default List<T> distinct() {
        return distinctBy(Function.identity());
    }

    /**
     * {@inheritDoc}
     * <p>
     * Complexity: O(n log n) comparisons.
     */
    @Override
    default List<T> distinctBy(Comparator<? super T> comparator) {
        Objects.requireNonNull(comparator, "comparator is null");
        final java.util.Set<T> seen = new java.util.TreeSet<>(comparator);
        return filter(seen::add);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Complexity: O(n), one key per element.
     */
    @Override
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
        return ofAll(iterator().distinctByKeepLast(comparator));
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
        return ofAll(iterator().distinctByKeepLast(keyExtractor));
    }

    /**
     * {@inheritDoc}
     * <p>
     * Complexity: O(n) for n dropped elements; the rest of this List is shared, not copied.
     */
    @Override
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
     * {@inheritDoc}
     * <p>
     * Complexity: O(k) for the k dropped elements; the rest of this List is shared, not copied.
     */
    @Override
    default List<T> dropUntil(Predicate<? super T> predicate) {
        Objects.requireNonNull(predicate, "predicate is null");
        return dropWhile(predicate.negate());
    }

    /**
     * {@inheritDoc}
     * <p>
     * Complexity: O(k) for the k dropped elements; the rest of this List is shared, not copied.
     */
    @Override
    default List<T> dropWhile(Predicate<? super T> predicate) {
        Objects.requireNonNull(predicate, "predicate is null");
        List<T> list = this;
        while (!list.isEmpty() && predicate.test(list.head())) {
            list = list.tail();
        }
        return list;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Complexity: O(n); the kept prefix is copied.
     */
    @Override
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
     * {@inheritDoc}
     * <p>
     * Complexity: O(n).
     */
    @Override
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

    @Override
    default List<T> reject(Predicate<? super T> predicate){
        Objects.requireNonNull(predicate, "predicate is null");
        return Collections.reject(this, predicate);
    }

    @Override
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
     * {@inheritDoc}
     * <p>
     * The elements are folded from the end: this List is reversed first, then folded from the left, so the recursion
     * depth does not grow with the length.
     */
    @Override
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

    @Override
    default <C extends @Nullable Object> Map<C, List<T>> groupBy(Function<? super T, ? extends C> classifier) {
        return Collections.groupBy(this, classifier, List::ofAll);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Complexity: lazy; O(size) per group when consumed.
     */
    @Override
    default Iterator<List<T>> grouped(int size) {
        return sliding(size, size);
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
     * {@inheritDoc}
     * <p>
     * Complexity: O(n); the kept prefix is copied.
     */
    @Override
    default List<T> init() {
        if (isEmpty()) {
            throw new UnsupportedOperationException("init of empty list");
        } else {
            return dropRight(1);
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * Complexity: O(n); the kept prefix is copied.
     */
    @Override
    default Option<List<T>> initOption() {
        return isEmpty() ? Option.none() : Option.some(init());
    }

    /**
     * {@inheritDoc}
     * <p>
     * Complexity: O(n); a cons list has no length field, so the cells are counted.
     */
    @Override
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
        return ofAll(iterator().intersperse(element));
    }

    /**
     * An iterator over the elements from {@code index} on.
     * <p>
     * Complexity: O(index) to reach the start, then O(1) per step.
     *
     * @param index the first position to iterate from
     * @return an iterator over the suffix
     * @throws IndexOutOfBoundsException if {@code index} is negative or greater than {@code length()}
     */
    default Iterator<T> iterator(int index) {
        return subSequence(index).iterator();
    }

    /**
     * {@inheritDoc}
     * <p>
     * Complexity: O(n).
     */
    @Override
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

    @Override
    default <U extends @Nullable Object> List<U> map(Function<? super T, ? extends U> mapper) {
        Objects.requireNonNull(mapper, "mapper is null");
        List<U> list = empty();
        for (T t : this) {
            list = list.prepend(mapper.apply(t));
        }
        return list.reverse();
    }

    @Override
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

    @Override
    default <U extends @Nullable Object> List<U> as(U value) {
        return map(ignored -> value);
    }

    @Override
    default List<T> orElse(Iterable<? extends T> other) {
        return isEmpty() ? ofAll(other) : this;
    }

    @Override
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

    @Override
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
    @Override
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
        return Collections.removeAll(this, element);
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
        return Collections.removeAll(this, elements);
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
     * {@inheritDoc}
     * <p>
     * Complexity: O(k) for the k elements before the replaced one; the rest of this List is shared, not copied.
     */
    @Override
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
     * {@inheritDoc}
     * <p>
     * Complexity: O(n).
     */
    @Override
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
     * {@inheritDoc}
     * <p>
     * Complexity: O(n + m) for m retained elements (they are hashed once, then one filter pass).
     */
    @Override
    default List<T> retainAll(Iterable<? extends T> elements) {
        return Collections.retainAll(this, elements);
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
     * An iterator over the elements from the last to the first.
     * <p>
     * Complexity: O(n) to create (the List is reversed first), then O(1) per step.
     *
     * @return the reverse iterator
     */
    default Iterator<T> reverseIterator() {
        return reverse().iterator();
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
     * {@inheritDoc}
     * <p>
     * Complexity: O(n).
     */
    @Override
    default List<T> scan(T zero, BiFunction<? super T, ? super T, ? extends T> operation) {
        return scanLeft(zero, operation);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Complexity: O(n).
     */
    @Override
    default <U extends @Nullable Object> List<U> scanLeft(U zero, BiFunction<? super U, ? super T, ? extends U> operation) {
        return Collections.scanLeft(this, zero, operation, Iterator::toList);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Complexity: O(n); the elements are walked from the end.
     */
    @Override
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

    @Override
    default Iterator<List<T>> slideBy(Function<? super T, ?> classifier) {
        return iterator().slideBy(classifier).map(List::ofAll);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Complexity: lazy; O(size) per window when consumed.
     */
    @Override
    default Iterator<List<T>> sliding(int size) {
        return sliding(size, 1);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Complexity: lazy; O(size) per window when consumed.
     */
    @Override
    default Iterator<List<T>> sliding(int size, int step) {
        return iterator().sliding(size, step).map(List::ofAll);
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
        return isEmpty() ? this : toJavaStream().sorted().collect(collector());
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
        return isEmpty() ? this : toJavaStream().sorted(comparator).collect(collector());
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
     * {@inheritDoc}
     * <p>
     * Complexity: O(n).
     */
    @Override
    default Tuple2<List<T>, List<T>> span(Predicate<? super T> predicate) {
        Objects.requireNonNull(predicate, "predicate is null");
        final Tuple2<Iterator<T>, Iterator<T>> itt = iterator().span(predicate);
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
        final Iterator<T> i = this.iterator().drop(offset);
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
     * {@inheritDoc}
     * <p>
     * Complexity: O(1); the tail is a field of the cons cell.
     */
    @Override
    List<T> tail();

    /**
     * {@inheritDoc}
     * <p>
     * Complexity: O(1); the tail is a field of the cons cell.
     */
    @Override
    default Option<List<T>> tailOption() {
        return isEmpty() ? Option.none() : Option.some(tail());
    }

    /**
     * {@inheritDoc}
     * <p>
     * Complexity: O(n) for n taken elements; the prefix is copied.
     */
    @Override
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
     * {@inheritDoc}
     * <p>
     * Complexity: O(k) for the k taken elements.
     */
    @Override
    default List<T> takeUntil(Predicate<? super T> predicate) {
        Objects.requireNonNull(predicate, "predicate is null");
        return takeWhile(predicate.negate());
    }

    /**
     * {@inheritDoc}
     * <p>
     * Complexity: O(k) for the k taken elements.
     */
    @Override
    default List<T> takeWhile(Predicate<? super T> predicate) {
        Objects.requireNonNull(predicate, "predicate is null");
        List<T> result = Nil.instance();
        for (List<T> list = this; !list.isEmpty() && predicate.test(list.head()); list = list.tail()) {
            result = result.prepend(list.head());
        }
        return result.length() == length() ? this : result.reverse();
    }

    /**
     * {@inheritDoc}
     * <p>
     * Complexity: O(n); the List is reversed twice.
     */
    @Override
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

    @Override
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

    @Override
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
     * {@inheritDoc}
     * <p>
     * Complexity: O(min(n, m)) for an argument of m elements.
     */
    @Override
    default <U extends @Nullable Object> List<Tuple2<T, U>> zip(Iterable<? extends U> that) {
        return zipWith(that, Tuple::of);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Complexity: O(min(n, m)) for an argument of m elements.
     */
    @Override
    default <U extends @Nullable Object, R extends @Nullable Object> List<R> zipWith(Iterable<? extends U> that, BiFunction<? super T, ? super U, ? extends R> mapper) {
        Objects.requireNonNull(that, "that is null");
        Objects.requireNonNull(mapper, "mapper is null");
        return ofAll(iterator().zipWith(that, mapper));
    }

    /**
     * {@inheritDoc}
     * <p>
     * Complexity: O(max(n, m)) for an argument of m elements.
     */
    @Override
    default <U extends @Nullable Object> List<Tuple2<T, U>> zipAll(Iterable<? extends U> that, T thisElem, U thatElem) {
        Objects.requireNonNull(that, "that is null");
        return ofAll(iterator().zipAll(that, thisElem, thatElem));
    }

    /**
     * {@inheritDoc}
     * <p>
     * Complexity: O(n).
     */
    @Override
    default List<Tuple2<T, Integer>> zipWithIndex() {
        return zipWithIndex(Tuple::of);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Complexity: O(n).
     */
    @Override
    default <U extends @Nullable Object> List<U> zipWithIndex(BiFunction<? super T, ? super Integer, ? extends U> mapper) {
        Objects.requireNonNull(mapper, "mapper is null");
        return ofAll(iterator().zipWithIndex(mapper));
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
}

interface ListModule {

    interface Combinations {

        static <T extends @Nullable Object> List<List<T>> apply(List<T> elements, int k) {
            if (k == 0) {
                return List.of(List.empty());
            } else {
                return elements.zipWithIndex().flatMap(
                        t -> apply(elements.drop(t._2() + 1), (k - 1)).map(c -> c.prepend(t._1()))
                );
            }
        }
    }

    interface SplitAt {

        static <T extends @Nullable Object> Tuple2<List<T>, List<T>> splitByPredicateReversed(List<T> source, Predicate<? super T> predicate) {
            Objects.requireNonNull(predicate, "predicate is null");
            List<T> init = Nil.instance();
            List<T> tail = source;
            while (!tail.isEmpty() && !predicate.test(tail.head())) {
                init = init.prepend(tail.head());
                tail = tail.tail();
            }
            return Tuple.of(init, tail);
        }
    }

    /** Slice searches over a cons list: the candidate start positions are the successive tails. */
    interface Slice {

        static <T extends @Nullable Object> int indexOfSlice(List<T> source, Iterable<? extends T> slice, int from) {
            if (source.isEmpty()) {
                return from == 0 && Collections.isEmpty(slice) ? 0 : -1;
            }
            return findFirstSlice(source, toList(slice), Math.max(from, 0));
        }

        static <T extends @Nullable Object> int lastIndexOfSlice(List<T> source, Iterable<? extends T> slice, int end) {
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
            final List<T> _slice = toList(slice);
            // lengths once, then counted down: List.length() walks the list
            final int sliceLength = _slice.length();
            int remaining = source.length();
            while (remaining >= sliceLength) {
                final int found = findNextSlice(source, _slice, remaining, sliceLength);
                if (found < 0) {
                    return result;
                }
                if (index + found > end) {
                    return result;
                }
                result = index + found;
                index += found + 1;
                remaining -= found + 1;
                source = source.drop(found + 1);
            }
            return result;
        }

        private static <T extends @Nullable Object> int findFirstSlice(List<T> source, List<T> slice, int from) {
            int index = 0;
            final int sliceLength = slice.length();
            // length once, then counted down: List.length() walks the list
            int remaining = source.length();
            while (remaining >= sliceLength) {
                if (index >= from && source.startsWith(slice)) {
                    return index;
                }
                if (source.isEmpty()) {
                    // only reachable for an empty slice with from > length()
                    return -1;
                }
                index++;
                remaining--;
                source = source.tail();
            }
            return -1;
        }

        // the offset of the next occurrence of the slice in source, or -1
        private static <T extends @Nullable Object> int findNextSlice(List<T> source, List<T> slice, int remaining, int sliceLength) {
            int index = 0;
            while (remaining >= sliceLength) {
                if (source.startsWith(slice)) {
                    return index;
                }
                index++;
                remaining--;
                source = source.tail();
            }
            return -1;
        }

        @SuppressWarnings("unchecked")
        private static <T extends @Nullable Object> List<T> toList(Iterable<? extends T> iterable) {
            return (iterable instanceof List) ? (List<T>) iterable : List.ofAll(iterable);
        }
    }

    interface Search {

        static <T extends @Nullable Object> int linearSearch(List<T> list, ToIntFunction<T> comparison) {
            int idx = 0;
            for (T current : list) {
                final int cmp = comparison.applyAsInt(current);
                if (cmp == 0) {
                    return idx;
                } else if (cmp < 0) {
                    return -(idx + 1);
                }
                idx += 1;
            }
            return -(idx + 1);
        }
    }
}
