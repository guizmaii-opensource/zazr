package com.guizmaii.zazr.collection;

import com.guizmaii.zazr.*;
import com.guizmaii.zazr.collection.internal.Collections;
import com.guizmaii.zazr.collection.internal.Iterator;
import com.guizmaii.zazr.collection.internal.JavaConverters;
import com.guizmaii.zazr.collection.internal.SetViews;
import com.guizmaii.zazr.control.Either;
import com.guizmaii.zazr.control.Option;
import java.io.*;
import java.util.ArrayList;
import java.util.Objects;
import java.util.function.*;
import java.util.stream.Collector;
import org.jspecify.annotations.Nullable;

/**
 * An immutable, hash-based {@link Set} implementation with predictable (insertion-order) iteration.
 * <p>
 * It is a {@link LinkedHashMap} of its elements and has the same costs: removing an element leaves a marker in the
 * insertion order, rebuilt in O(n) once the markers outnumber the elements, amortised over a chain of removals. On
 * an older version used again, removing or slicing can pay the rebuild each time, and after removals, finding a
 * position by rank ({@code tail}, {@code init}, {@code take}, {@code drop}) walks past the markers in the way.
 * <p>
 * An element given more than once keeps the position and the object of its first occurrence, whichever way the set
 * is built: {@link #add(Object)} of an element already present returns the set unchanged, and every factory,
 * collector and bulk operation ({@code of}, {@code ofAll}, {@code collector()}, {@code tabulate}, {@code fill},
 * {@code flatten}, {@code addAll}, {@code union}, {@code map}, {@code flatMap}) gives the set that adding the elements
 * one by one gives.
 *
 * @param <T> Component type
 * @author Ruslan Sennov, Patryk Najda, Daniel Dietrich
 */
public final class LinkedHashSet<T extends @Nullable Object> implements Set<T> {

    private static final LinkedHashSet<?> EMPTY = new LinkedHashSet<>(LinkedHashMap.empty());

    private final LinkedHashMap<T, Object> map;

    private LinkedHashSet(LinkedHashMap<T, Object> map) {
        this.map = map;
    }

    /**
     * Returns the empty LinkedHashSet.
     *
     * @param <T> Component type
     * @return The empty LinkedHashSet.
     */
    @SuppressWarnings("unchecked")
    public static <T extends @Nullable Object> LinkedHashSet<T> empty() {
        return (LinkedHashSet<T>) EMPTY;
    }

    static <T extends @Nullable Object> LinkedHashSet<T> wrap(LinkedHashMap<T, Object> map) {
        return new LinkedHashSet<>(map);
    }

    /**
     * Returns a {@link Collector} which may be used in conjunction with
     * {@link java.util.stream.Stream#collect(Collector)} to obtain a {@link LinkedHashSet}.
     *
     * @param <T> Component type of the LinkedHashSet.
     * @return A com.guizmaii.zazr.collection.LinkedHashSet Collector.
     */
    public static <T extends @Nullable Object> Collector<T, ArrayList<T>, LinkedHashSet<T>> collector() {
        final Supplier<ArrayList<T>> supplier = ArrayList::new;
        final BiConsumer<ArrayList<T>, T> accumulator = ArrayList::add;
        final BinaryOperator<ArrayList<T>> combiner = (left, right) -> {
            left.addAll(right);
            return left;
        };
        final Function<ArrayList<T>, LinkedHashSet<T>> finisher = LinkedHashSet::ofAll;
        return Collector.of(supplier, accumulator, combiner, finisher);
    }

    /**
     * Narrows a {@code LinkedHashSet<? extends T>} to {@code LinkedHashSet<T>} via a
     * type-safe cast. Safe here because the set is immutable and no elements
     * can be added that would violate the type (covariance)
     *
     * @param linkedHashSet the set to narrow
     * @param <T>           the target element type
     * @return the same set viewed as {@code LinkedHashSet<T>}
     */
    @SuppressWarnings("unchecked")
    public static <T extends @Nullable Object> LinkedHashSet<T> narrow(LinkedHashSet<? extends T> linkedHashSet) {
        return (LinkedHashSet<T>) linkedHashSet;
    }

    /**
     * Returns a singleton {@code LinkedHashSet}, i.e. a {@code LinkedHashSet} of one element.
     *
     * @param element An element.
     * @param <T>     The component type
     * @return A new LinkedHashSet instance containing the given element
     */
    public static <T extends @Nullable Object> LinkedHashSet<T> of(T element) {
        return LinkedHashSet.<T> empty().add(element);
    }

    /**
     * Creates a LinkedHashSet of the given elements.
     *
     * <pre>{@code LinkedHashSet.of(1, 2, 3, 4)}</pre>
     *
     * @param <T>      Component type of the LinkedHashSet.
     * @param elements Zero or more elements.
     * @return A set containing the given elements.
     * @throws NullPointerException if {@code elements} is null
     */
    @SafeVarargs
    public static <T extends @Nullable Object> LinkedHashSet<T> of(T ... elements) {
        Objects.requireNonNull(elements, "elements is null");
        LinkedHashMap<T, Object> map = LinkedHashMap.empty();
        for (T element : elements) {
            Objects.requireNonNull(element, "LinkedHashSet.of: element is null");
            map = map.putIfAbsent(element, element);
        }
        return map.isEmpty() ? LinkedHashSet.empty() : new LinkedHashSet<>(map);
    }

    /**
     * Returns a LinkedHashSet containing {@code n} values of a given Function {@code f}
     * over a range of integer values from 0 to {@code n - 1}.
     *
     * @param <T> Component type of the LinkedHashSet
     * @param n   The number of times {@code f} is invoked (equal results are deduplicated, so the
     *            resulting set may contain fewer than {@code n} elements)
     * @param f   The Function computing element values
     * @return A LinkedHashSet consisting of the distinct elements {@code f(0),f(1), ..., f(n - 1)}
     * @throws NullPointerException if {@code f} is null
     */
    public static <T extends @Nullable Object> LinkedHashSet<T> tabulate(int n, Function<? super Integer, ? extends T> f) {
        Objects.requireNonNull(f, "f is null");
        return Collections.tabulate(n, f, LinkedHashSet.empty(), LinkedHashSet::of);
    }

    /**
     * Returns a LinkedHashSet containing the distinct values returned by {@code n} calls to a given
     * Supplier {@code s}.
     *
     * @param <T> Component type of the LinkedHashSet
     * @param n   The number of times {@code s} is invoked
     * @param s   The Supplier computing element values
     * @return A LinkedHashSet of at most {@code n} distinct values supplied by {@code s}.
     * @throws NullPointerException if {@code s} is null
     */
    public static <T extends @Nullable Object> LinkedHashSet<T> fill(int n, Supplier<? extends T> s) {
        Objects.requireNonNull(s, "s is null");
        return Collections.fill(n, s, LinkedHashSet.empty(), LinkedHashSet::of);
    }

    /**
     * Creates a LinkedHashSet of the given elements.
     *
     * @param elements Set elements
     * @param <T>      The value type
     * @return A LinkedHashSet containing the given elements; if {@code elements} is already a
     *         LinkedHashSet, or the {@link #asJava()} view of one (not its {@code reversed()} view), that
     *         LinkedHashSet is returned unchanged.
     */
    @SuppressWarnings("unchecked")
    public static <T extends @Nullable Object> LinkedHashSet<T> ofAll(Iterable<? extends T> elements) {
        Objects.requireNonNull(elements, "elements is null");
        if (elements instanceof LinkedHashSet) {
            return (LinkedHashSet<T>) elements;
        } else if (JavaConverters.underlying(elements) instanceof LinkedHashSet<?> underlying) {
            return (LinkedHashSet<T>) underlying;
        } else {
            final LinkedHashMap<T, Object> mao = addAll(LinkedHashMap.empty(), elements);
            return mao.isEmpty() ? empty() : new LinkedHashSet<>(mao);
        }
    }

    /**
     * Creates a LinkedHashSet that contains the elements of the given {@link java.util.stream.Stream}.
     *
     * @param javaStream A {@link java.util.stream.Stream}
     * @param <T>        Component type of the Stream.
     * @return A LinkedHashSet containing the given elements in the same order.
     */
    public static <T extends @Nullable Object> LinkedHashSet<T> ofAll(java.util.stream.Stream<? extends T> javaStream) {
        Objects.requireNonNull(javaStream, "javaStream is null");
        return ofAll(Iterator.ofAll(javaStream.iterator()));
    }

    /**
     * The union of nested iterables, in the order in which their elements first occur. Static, like every {@code flatten} in Zazr, because Java cannot demand of an
     * instance method that the receiver's element type be a collection. The outer iterable and each inner one are
     * iterated once, so one-shot iterables are accepted.
     * <p>
     * Complexity: O(n) for n inner elements in total, one effectively O(1) insertion each.
     *
     * @param nested Iterables of elements
     * @param <T>    Component type of the inner iterables
     * @return the distinct inner elements, in order of first occurrence
     * @throws NullPointerException if {@code nested}, an inner iterable or an element is null
     */
    public static <T extends @Nullable Object> LinkedHashSet<T> flatten(Iterable<? extends Iterable<? extends T>> nested) {
        Objects.requireNonNull(nested, "nested is null");
        LinkedHashMap<T, Object> all = LinkedHashMap.empty();
        for (Iterable<? extends T> inner : nested) {
            all = addAll(all, inner);
        }
        return all.isEmpty() ? empty() : new LinkedHashSet<>(all);
    }

    /**
     * Creates a LinkedHashSet from boolean values.
     *
     * @param elements boolean values
     * @return A new LinkedHashSet of Boolean values
     * @throws NullPointerException if elements is null
     */
    public static LinkedHashSet<Boolean> ofAll(boolean ... elements) {
        Objects.requireNonNull(elements, "elements is null");
        return LinkedHashSet.ofAll(Iterator.ofAll(elements));
    }

    /**
     * Creates a LinkedHashSet from byte values.
     *
     * @param elements byte values
     * @return A new LinkedHashSet of Byte values
     * @throws NullPointerException if elements is null
     */
    public static LinkedHashSet<Byte> ofAll(byte ... elements) {
        Objects.requireNonNull(elements, "elements is null");
        return LinkedHashSet.ofAll(Iterator.ofAll(elements));
    }

    /**
     * Creates a LinkedHashSet from char values.
     *
     * @param elements char values
     * @return A new LinkedHashSet of Character values
     * @throws NullPointerException if elements is null
     */
    public static LinkedHashSet<Character> ofAll(char ... elements) {
        Objects.requireNonNull(elements, "elements is null");
        return LinkedHashSet.ofAll(Iterator.ofAll(elements));
    }

    /**
     * Creates a LinkedHashSet from double values.
     *
     * @param elements double values
     * @return A new LinkedHashSet of Double values
     * @throws NullPointerException if elements is null
     */
    public static LinkedHashSet<Double> ofAll(double ... elements) {
        Objects.requireNonNull(elements, "elements is null");
        return LinkedHashSet.ofAll(Iterator.ofAll(elements));
    }

    /**
     * Creates a LinkedHashSet from float values.
     *
     * @param elements a float values
     * @return A new LinkedHashSet of Float values
     * @throws NullPointerException if elements is null
     */
    public static LinkedHashSet<Float> ofAll(float ... elements) {
        Objects.requireNonNull(elements, "elements is null");
        return LinkedHashSet.ofAll(Iterator.ofAll(elements));
    }

    /**
     * Creates a LinkedHashSet from int values.
     *
     * @param elements int values
     * @return A new LinkedHashSet of Integer values
     * @throws NullPointerException if elements is null
     */
    public static LinkedHashSet<Integer> ofAll(int ... elements) {
        Objects.requireNonNull(elements, "elements is null");
        return LinkedHashSet.ofAll(Iterator.ofAll(elements));
    }

    /**
     * Creates a LinkedHashSet from long values.
     *
     * @param elements long values
     * @return A new LinkedHashSet of Long values
     * @throws NullPointerException if elements is null
     */
    public static LinkedHashSet<Long> ofAll(long ... elements) {
        Objects.requireNonNull(elements, "elements is null");
        return LinkedHashSet.ofAll(Iterator.ofAll(elements));
    }

    /**
     * Creates a LinkedHashSet from short values.
     *
     * @param elements short values
     * @return A new LinkedHashSet of Short values
     * @throws NullPointerException if elements is null
     */
    public static LinkedHashSet<Short> ofAll(short ... elements) {
        Objects.requireNonNull(elements, "elements is null");
        return LinkedHashSet.ofAll(Iterator.ofAll(elements));
    }

    /**
     * Creates a LinkedHashSet of int numbers starting from {@code from}, extending to {@code toExclusive - 1}.
     * <p>
     * Examples:
     * <pre>
     * {@code
     * LinkedHashSet.range(0, 0)  // = LinkedHashSet()
     * LinkedHashSet.range(2, 0)  // = LinkedHashSet()
     * LinkedHashSet.range(-2, 2) // = LinkedHashSet(-2, -1, 0, 1)
     * }
     * </pre>
     *
     * @param from        the first number
     * @param toExclusive the last number + 1
     * @return a range of int values as specified or the empty range if {@code from >= toExclusive}
     */
    public static LinkedHashSet<Integer> range(int from, int toExclusive) {
        return LinkedHashSet.ofAll(Iterator.range(from, toExclusive));
    }

    /**
     * Creates a LinkedHashSet of char numbers starting from {@code from}, extending to {@code toExclusive - 1}.
     * <p>
     * Examples:
     * <pre>
     * {@code
     * LinkedHashSet.range('a', 'a')  // = LinkedHashSet()
     * LinkedHashSet.range('c', 'a')  // = LinkedHashSet()
     * LinkedHashSet.range('a', 'c')  // = LinkedHashSet('a', 'b')
     * }
     * </pre>
     *
     * @param from        the first number
     * @param toExclusive the last number + 1
     * @return a range of char values as specified or the empty range if {@code from >= toExclusive}
     */
    public static LinkedHashSet<Character> range(char from, char toExclusive) {
        return LinkedHashSet.ofAll(Iterator.range(from, toExclusive));
    }

    /**
     * Creates a LinkedHashSet of int numbers starting from {@code from}, extending to {@code toExclusive - 1},
     * with {@code step}.
     * <p>
     * Examples:
     * <pre>
     * {@code
     * LinkedHashSet.rangeBy(1, 3, 1)  // = LinkedHashSet(1, 2)
     * LinkedHashSet.rangeBy(1, 4, 2)  // = LinkedHashSet(1, 3)
     * LinkedHashSet.rangeBy(4, 1, -2) // = LinkedHashSet(4, 2)
     * LinkedHashSet.rangeBy(4, 1, 2)  // = LinkedHashSet()
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
    public static LinkedHashSet<Integer> rangeBy(int from, int toExclusive, int step) {
        return LinkedHashSet.ofAll(Iterator.rangeBy(from, toExclusive, step));
    }

    /**
     * Creates a LinkedHashSet of char numbers starting from {@code from}, extending to {@code toExclusive - 1},
     * with {@code step}.
     * <p>
     * Examples:
     * <pre>
     * {@code
     * LinkedHashSet.rangeBy('a', 'c', 1)  // = LinkedHashSet('a', 'b')
     * LinkedHashSet.rangeBy('a', 'd', 2)  // = LinkedHashSet('a', 'c')
     * LinkedHashSet.rangeBy('d', 'a', -2) // = LinkedHashSet('d', 'b')
     * LinkedHashSet.rangeBy('d', 'a', 2)  // = LinkedHashSet()
     * }
     * </pre>
     *
     * @param from        the first number
     * @param toExclusive the last number + 1
     * @param step        the step
     * @return a range of char values as specified or the empty range if<br>
     * {@code from >= toExclusive} and {@code step > 0} or<br>
     * {@code from <= toExclusive} and {@code step < 0}
     * @throws IllegalArgumentException if {@code step} is zero
     */
    public static LinkedHashSet<Character> rangeBy(char from, char toExclusive, int step) {
        return LinkedHashSet.ofAll(Iterator.rangeBy(from, toExclusive, step));
    }

    /**
     * Creates a LinkedHashSet of double numbers starting from {@code from}, extending to {@code toExclusive - 1},
     * with {@code step}.
     * <p>
     * Examples:
     * <pre>
     * {@code
     * LinkedHashSet.rangeBy(1.0, 3.0, 1.0)  // = LinkedHashSet(1.0, 2.0)
     * LinkedHashSet.rangeBy(1.0, 4.0, 2.0)  // = LinkedHashSet(1.0, 3.0)
     * LinkedHashSet.rangeBy(4.0, 1.0, -2.0) // = LinkedHashSet(4.0, 2.0)
     * LinkedHashSet.rangeBy(4.0, 1.0, 2.0)  // = LinkedHashSet()
     * }
     * </pre>
     *
     * @param from        the first number
     * @param toExclusive the last number + 1
     * @param step        the step
     * @return a range of double values as specified or the empty range if<br>
     * {@code from >= toExclusive} and {@code step > 0} or<br>
     * {@code from <= toExclusive} and {@code step < 0}
     * @throws IllegalArgumentException if {@code step} is zero
     */
    public static LinkedHashSet<Double> rangeBy(double from, double toExclusive, double step) {
        return LinkedHashSet.ofAll(Iterator.rangeBy(from, toExclusive, step));
    }

    /**
     * Creates a LinkedHashSet of long numbers starting from {@code from}, extending to {@code toExclusive - 1}.
     * <p>
     * Examples:
     * <pre>
     * {@code
     * LinkedHashSet.range(0L, 0L)  // = LinkedHashSet()
     * LinkedHashSet.range(2L, 0L)  // = LinkedHashSet()
     * LinkedHashSet.range(-2L, 2L) // = LinkedHashSet(-2L, -1L, 0L, 1L)
     * }
     * </pre>
     *
     * @param from        the first number
     * @param toExclusive the last number + 1
     * @return a range of long values as specified or the empty range if {@code from >= toExclusive}
     */
    public static LinkedHashSet<Long> range(long from, long toExclusive) {
        return LinkedHashSet.ofAll(Iterator.range(from, toExclusive));
    }

    /**
     * Creates a LinkedHashSet of long numbers starting from {@code from}, extending to {@code toExclusive - 1},
     * with {@code step}.
     * <p>
     * Examples:
     * <pre>
     * {@code
     * LinkedHashSet.rangeBy(1L, 3L, 1L)  // = LinkedHashSet(1L, 2L)
     * LinkedHashSet.rangeBy(1L, 4L, 2L)  // = LinkedHashSet(1L, 3L)
     * LinkedHashSet.rangeBy(4L, 1L, -2L) // = LinkedHashSet(4L, 2L)
     * LinkedHashSet.rangeBy(4L, 1L, 2L)  // = LinkedHashSet()
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
    public static LinkedHashSet<Long> rangeBy(long from, long toExclusive, long step) {
        return LinkedHashSet.ofAll(Iterator.rangeBy(from, toExclusive, step));
    }

    /**
     * Creates a LinkedHashSet of int numbers starting from {@code from}, extending to {@code toInclusive}.
     * <p>
     * Examples:
     * <pre>
     * {@code
     * LinkedHashSet.rangeClosed(0, 0)  // = LinkedHashSet(0)
     * LinkedHashSet.rangeClosed(2, 0)  // = LinkedHashSet()
     * LinkedHashSet.rangeClosed(-2, 2) // = LinkedHashSet(-2, -1, 0, 1, 2)
     * }
     * </pre>
     *
     * @param from        the first number
     * @param toInclusive the last number
     * @return a range of int values as specified or the empty range if {@code from > toInclusive}
     */
    public static LinkedHashSet<Integer> rangeClosed(int from, int toInclusive) {
        return LinkedHashSet.ofAll(Iterator.rangeClosed(from, toInclusive));
    }

    /**
     * Creates a LinkedHashSet of char numbers starting from {@code from}, extending to {@code toInclusive}.
     * <p>
     * Examples:
     * <pre>
     * {@code
     * LinkedHashSet.rangeClosed('a', 'a')  // = LinkedHashSet('a')
     * LinkedHashSet.rangeClosed('c', 'a')  // = LinkedHashSet()
     * LinkedHashSet.rangeClosed('a', 'c')  // = LinkedHashSet('a', 'b', 'c')
     * }
     * </pre>
     *
     * @param from        the first number
     * @param toInclusive the last number
     * @return a range of char values as specified or the empty range if {@code from > toInclusive}
     */
    public static LinkedHashSet<Character> rangeClosed(char from, char toInclusive) {
        return LinkedHashSet.ofAll(Iterator.rangeClosed(from, toInclusive));
    }

    /**
     * Creates a LinkedHashSet of int numbers starting from {@code from}, extending to {@code toInclusive},
     * with {@code step}.
     * <p>
     * Examples:
     * <pre>
     * {@code
     * LinkedHashSet.rangeClosedBy(1, 3, 1)  // = LinkedHashSet(1, 2, 3)
     * LinkedHashSet.rangeClosedBy(1, 4, 2)  // = LinkedHashSet(1, 3)
     * LinkedHashSet.rangeClosedBy(4, 1, -2) // = LinkedHashSet(4, 2)
     * LinkedHashSet.rangeClosedBy(4, 1, 2)  // = LinkedHashSet()
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
    public static LinkedHashSet<Integer> rangeClosedBy(int from, int toInclusive, int step) {
        return LinkedHashSet.ofAll(Iterator.rangeClosedBy(from, toInclusive, step));
    }

    /**
     * Creates a LinkedHashSet of char numbers starting from {@code from}, extending to {@code toInclusive},
     * with {@code step}.
     * <p>
     * Examples:
     * <pre>
     * {@code
     * LinkedHashSet.rangeClosedBy('a', 'c', 1)  // = LinkedHashSet('a', 'b', 'c')
     * LinkedHashSet.rangeClosedBy('a', 'd', 2)  // = LinkedHashSet('a', 'c')
     * LinkedHashSet.rangeClosedBy('d', 'a', -2) // = LinkedHashSet('d', 'b')
     * LinkedHashSet.rangeClosedBy('d', 'a', 2)  // = LinkedHashSet()
     * }
     * </pre>
     *
     * @param from        the first number
     * @param toInclusive the last number
     * @param step        the step
     * @return a range of char values as specified or the empty range if<br>
     * {@code from > toInclusive} and {@code step > 0} or<br>
     * {@code from < toInclusive} and {@code step < 0}
     * @throws IllegalArgumentException if {@code step} is zero
     */
    public static LinkedHashSet<Character> rangeClosedBy(char from, char toInclusive, int step) {
        return LinkedHashSet.ofAll(Iterator.rangeClosedBy(from, toInclusive, step));
    }

    /**
     * Creates a LinkedHashSet of double numbers starting from {@code from}, extending to {@code toInclusive},
     * with {@code step}.
     * <p>
     * Examples:
     * <pre>
     * {@code
     * LinkedHashSet.rangeClosedBy(1.0, 3.0, 1.0)  // = LinkedHashSet(1.0, 2.0, 3.0)
     * LinkedHashSet.rangeClosedBy(1.0, 4.0, 2.0)  // = LinkedHashSet(1.0, 3.0)
     * LinkedHashSet.rangeClosedBy(4.0, 1.0, -2.0) // = LinkedHashSet(4.0, 2.0)
     * LinkedHashSet.rangeClosedBy(4.0, 1.0, 2.0)  // = LinkedHashSet()
     * }
     * </pre>
     *
     * @param from        the first number
     * @param toInclusive the last number
     * @param step        the step
     * @return a range of double values as specified or the empty range if<br>
     * {@code from > toInclusive} and {@code step > 0} or<br>
     * {@code from < toInclusive} and {@code step < 0}
     * @throws IllegalArgumentException if {@code step} is zero
     */
    public static LinkedHashSet<Double> rangeClosedBy(double from, double toInclusive, double step) {
        return LinkedHashSet.ofAll(Iterator.rangeClosedBy(from, toInclusive, step));
    }

    /**
     * Creates a LinkedHashSet of long numbers starting from {@code from}, extending to {@code toInclusive}.
     * <p>
     * Examples:
     * <pre>
     * {@code
     * LinkedHashSet.rangeClosed(0L, 0L)  // = LinkedHashSet(0L)
     * LinkedHashSet.rangeClosed(2L, 0L)  // = LinkedHashSet()
     * LinkedHashSet.rangeClosed(-2L, 2L) // = LinkedHashSet(-2L, -1L, 0L, 1L, 2L)
     * }
     * </pre>
     *
     * @param from        the first number
     * @param toInclusive the last number
     * @return a range of long values as specified or the empty range if {@code from > toInclusive}
     */
    public static LinkedHashSet<Long> rangeClosed(long from, long toInclusive) {
        return LinkedHashSet.ofAll(Iterator.rangeClosed(from, toInclusive));
    }

    /**
     * Creates a LinkedHashSet of long numbers starting from {@code from}, extending to {@code toInclusive},
     * with {@code step}.
     * <p>
     * Examples:
     * <pre>
     * {@code
     * LinkedHashSet.rangeClosedBy(1L, 3L, 1L)  // = LinkedHashSet(1L, 2L, 3L)
     * LinkedHashSet.rangeClosedBy(1L, 4L, 2L)  // = LinkedHashSet(1L, 3L)
     * LinkedHashSet.rangeClosedBy(4L, 1L, -2L) // = LinkedHashSet(4L, 2L)
     * LinkedHashSet.rangeClosedBy(4L, 1L, 2L)  // = LinkedHashSet()
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
    public static LinkedHashSet<Long> rangeClosedBy(long from, long toInclusive, long step) {
        return LinkedHashSet.ofAll(Iterator.rangeClosedBy(from, toInclusive, step));
    }

    /**
     * Adds the given element to this set. If an equal element is already contained, this instance is
     * returned unchanged and the existing element is retained (the given {@code element} is discarded).
     * <p>
     * Complexity: effectively O(1) (one hash lookup, then a hash insertion and an append to the insertion order when
     * the element is new).
     *
     * @param element The element to be added.
     * @return A set containing all elements of this set and also {@code element}.
     */
    @Override
    public LinkedHashSet<T> add(T element) {
        Objects.requireNonNull(element, "LinkedHashSet.add: element is null");
        final LinkedHashMap<T, Object> that = map.putIfAbsent(element, element);
        return that == map ? this : new LinkedHashSet<>(that);
    }

    /**
     * Adds all of the given elements that are not already contained in this set, in encounter order.
     * If no new element is added, this instance is returned unchanged (or, if this set is empty and
     * {@code elements} is a {@code LinkedHashSet}, the given {@code elements} instance). An element
     * already in this set keeps its position and its object, as with {@link #add(Object)}; an element repeated
     * in {@code elements} is added at its first occurrence.
     * <p>
     * Complexity: O(m) for m elements, each an effectively O(1) {@link #add(Object)}; O(1) when this set is empty and
     * {@code elements} is a LinkedHashSet, which is returned as is.
     *
     * @param elements The elements to be added.
     * @return A set containing all elements of this set and the given {@code elements}.
     */
    @Override
    public LinkedHashSet<T> addAll(Iterable<? extends T> elements) {
        Objects.requireNonNull(elements, "elements is null");
        if (isEmpty() && elements instanceof LinkedHashSet) {
            @SuppressWarnings("unchecked")
            final LinkedHashSet<T> set = (LinkedHashSet<T>) elements;
            return set;
        }
        final LinkedHashMap<T, Object> that = addAll(map, elements);
        if (that.size() == map.size()) {
            return this;
        } else {
            return new LinkedHashSet<>(that);
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * Complexity: effectively O(1) (one hash lookup).
     */
    @Override
    public boolean contains(T element) {
        return map.containsKey(element);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Complexity: O(n + m) for a set of m elements (a hash set of them, then the kept elements copied into a new set).
     */
    @Override
    public LinkedHashSet<T> diff(Set<? extends T> elements) {
        Objects.requireNonNull(elements, "elements is null");
        if (isEmpty() || elements.isEmpty()) {
            return this;
        } else {
            return removeAll(elements);
        }
    }

    @Override
    public LinkedHashSet<T> filter(Predicate<? super T> predicate) {
        Objects.requireNonNull(predicate, "predicate is null");
        final LinkedHashSet<T> filtered = LinkedHashSet.ofAll(Iterator.ofAll(this).filter(predicate));
        return filtered.size() == size() ? this : filtered;
    }

    @Override
    public LinkedHashSet<T> reject(Predicate<? super T> predicate) {
        Objects.requireNonNull(predicate, "predicate is null");
        return filter(predicate.negate());
    }

    @Override
    public <U extends @Nullable Object> LinkedHashSet<U> flatMap(Function<? super T, ? extends Iterable<? extends U>> mapper) {
        Objects.requireNonNull(mapper, "mapper is null");
        if (isEmpty()) {
            return empty();
        } else {
            final LinkedHashMap<U, Object> that = foldLeft(LinkedHashMap.empty(),
                    (tree, t) -> addAll(tree, mapper.apply(t)));
            return new LinkedHashSet<>(that);
        }
    }

    @Override
    public <C extends @Nullable Object> Map<C, LinkedHashSet<T>> groupBy(Function<? super T, ? extends C> classifier) {
        return Collections.groupBy(this, classifier, LinkedHashSet::ofAll);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Complexity: O(n + m) for a set of m elements (a hash set of them, then the kept elements copied into a new set).
     */
    @Override
    public LinkedHashSet<T> intersect(Set<? extends T> elements) {
        Objects.requireNonNull(elements, "elements is null");
        if (isEmpty() || elements.isEmpty()) {
            return empty();
        } else {
            return retainAll(elements);
        }
    }

    @Override
    public boolean isEmpty() {
        return map.isEmpty();
    }

    @Override
    public int size() {
        return map.size();
    }

    /**
     * {@inheritDoc}
     * <p>
     * Complexity: O(1) to create; each step is effectively O(1) (one hash lookup), a whole walk O(n).
     */
    @Override
    public java.util.Iterator<T> iterator() {
        return Iterator.ofAll(map).map(t -> t._1());
    }

    /**
     * An unmodifiable {@link java.util.SequencedSet} view of this LinkedHashSet, in insertion order: nothing is
     * copied, reads go through to this set, which never changes, and every mutator of the view (including those of
     * its iterator) throws {@link UnsupportedOperationException}. {@code reversed()} is a view in reverse insertion
     * order. The view equals any {@code java.util.Set} with the same elements. A mutable copy is
     * {@code new java.util.LinkedHashSet<>(set.asJava())}; {@code LinkedHashSet.ofAll} given the view returns this
     * set without copying.
     * <p>
     * Complexity: O(1); {@code contains} on the view is effectively O(1), and each step of its iterator, in either
     * order, is effectively O(1).
     *
     * @return an unmodifiable {@code java.util.SequencedSet} view
     */
    @Override
    public java.util.SequencedSet<T> asJava() {
        return SetViews.asJava(this, () -> Iterator.ofAll(map.reverseIterator()).map(Tuple2::_1));
    }

    @Override
    public <U extends @Nullable Object> LinkedHashSet<U> map(Function<? super T, ? extends U> mapper) {
        Objects.requireNonNull(mapper, "mapper is null");
        if (isEmpty()) {
            return empty();
        } else {
            final LinkedHashMap<U, Object> that = foldLeft(LinkedHashMap.empty(), (tree, t) -> {
                final U u = mapper.apply(t);
                return tree.putIfAbsent(u, u);
            });
            return new LinkedHashSet<>(that);
        }
    }

    @Override
    public <U extends @Nullable Object> LinkedHashSet<U> collect(Function<? super T, ? extends Option<? extends U>> mapper) {
        Objects.requireNonNull(mapper, "mapper is null");
        if (isEmpty()) {
            return empty();
        }
        LinkedHashMap<U, Object> that = LinkedHashMap.empty();
        for (T t : this) {
            final Option<? extends U> collected = Objects.requireNonNull(mapper.apply(t), "LinkedHashSet.collect: mapper returned null");
            if (collected.isDefined()) {
                final U u = collected.get();
                that = that.putIfAbsent(u, u);
            }
        }
        return new LinkedHashSet<>(that);
    }

    @Override
    public <U extends @Nullable Object> LinkedHashSet<U> as(U value) {
        return map(ignored -> value);
    }

    @Override
    public LinkedHashSet<T> orElse(Iterable<? extends T> other) {
        return isEmpty() ? ofAll(other) : this;
    }

    @Override
    public LinkedHashSet<T> orElse(Supplier<? extends Iterable<? extends T>> supplier) {
        return isEmpty() ? ofAll(supplier.get()) : this;
    }

    @Override
    public Tuple2<LinkedHashSet<T>, LinkedHashSet<T>> partition(Predicate<? super T> predicate) {
        return Collections.partition(this, LinkedHashSet::ofAll, predicate);
    }

    /**
     * Splits the elements into a left and a right side according to the {@link Either} {@code f} returns for each: the
     * generalisation of {@link #partition(Predicate)}. One pass in iteration order, {@code f} called once per element, no intermediate
     * collection of {@code Either}s. Values equal on one side are kept once, at the position of the first.
     * <p>
     * Complexity: O(n), one effectively O(1) insertion per element.
     *
     * @param f   Classifies an element
     * @param <L> Component type of the left side
     * @param <R> Component type of the right side
     * @return the left values and the right values, each in the iteration order of the elements they come from
     * @throws NullPointerException if {@code f} is null or returns null
     */
    public <L extends @Nullable Object, R extends @Nullable Object> Tuple2<LinkedHashSet<L>, LinkedHashSet<R>> partitionMap(Function<? super T, ? extends Either<? extends L, ? extends R>> f) {
        Objects.requireNonNull(f, "f is null");
        LinkedHashMap<L, Object> lefts = LinkedHashMap.empty();
        LinkedHashMap<R, Object> rights = LinkedHashMap.empty();
        for (T element : this) {
            switch (Objects.requireNonNull(f.apply(element), "LinkedHashSet.partitionMap: f returned null")) {
                case Either.Left(var left) -> lefts = lefts.putIfAbsent(left, left);
                case Either.Right(var right) -> rights = rights.putIfAbsent(right, right);
            }
        }
        return Tuple.of(lefts.isEmpty() ? empty() : new LinkedHashSet<>(lefts), rights.isEmpty() ? empty() : new LinkedHashSet<>(rights));
    }

    @Override
    public LinkedHashSet<T> tap(Consumer<? super T> action) {
        Objects.requireNonNull(action, "action is null");
        forEach(action);
        return this;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Complexity: effectively O(1) (one hash removal and one marker in the insertion order), amortised over a chain
     * of removals, each on the result of the previous one: when the markers outnumber the elements, the insertion
     * order is rebuilt in O(n). Removing again from the same older set that is about to be rebuilt pays that O(n)
     * each time. Removing the first or the last element also walks past the markers of earlier removals next to it.
     */
    @Override
    public LinkedHashSet<T> remove(T element) {
        final LinkedHashMap<T, Object> newMap = map.remove(element);
        return (newMap == map) ? this : new LinkedHashSet<>(newMap);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Complexity: O(m + n) for m given elements (a hash set of them, then the kept elements copied into a new set).
     */
    @Override
    public LinkedHashSet<T> removeAll(Iterable<? extends T> elements) {
        return Collections.removeAll(this, elements, kept -> filter(kept));
    }

    /**
     * {@inheritDoc}
     * <p>
     * Complexity: effectively O(1) amortised, with the O(n) cases of {@link #remove(Object)}; the new element takes the position of the
     * replaced one.
     */
    @Override
    public LinkedHashSet<T> replace(T currentElement, T newElement) {
        if (!Objects.equals(currentElement, newElement) && contains(currentElement)) {
            // by key: the map may be the one of a LinkedHashMap.keySet(), whose values are not the elements
            Objects.requireNonNull(newElement, "LinkedHashSet: element is null");
            return new LinkedHashSet<>(map.replaceKey(currentElement, Tuple.of(newElement, newElement)));
        } else {
            return this;
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * Complexity: effectively O(1) amortised, that of {@link #replace(Object, Object)}: a set holds an element once.
     */
    @Override
    public LinkedHashSet<T> replaceAll(T currentElement, T newElement) {
        return replace(currentElement, newElement);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Complexity: O(m + n) for m given elements (a hash set of them, then the kept elements copied into a new set).
     */
    @Override
    public LinkedHashSet<T> retainAll(Iterable<? extends T> elements) {
        return Collections.retainAll(this, elements, kept -> filter(kept));
    }

    /**
     * Adds all of the elements of {@code elements} that are not already contained in this set, forming the union.
     * If no new element is added, this instance is returned unchanged (or, if this set is empty and
     * {@code elements} is a {@code LinkedHashSet}, the given {@code elements} instance). An element
     * already in this set keeps its position and its object, as with {@link #add(Object)}; an element repeated
     * in {@code elements} is added at its first occurrence.
     * <p>
     * See also {@link #addAll(Iterable)}.
     * <p>
     * Complexity: O(m) for a set of m elements, each an effectively O(1) {@link #add(Object)}.
     *
     * @param elements The set to form the union with.
     * @return A set that contains all distinct elements of this and {@code elements} set.
     */
    @SuppressWarnings("unchecked")
    @Override
    public LinkedHashSet<T> union(Set<? extends T> elements) {
        Objects.requireNonNull(elements, "elements is null");
        if (isEmpty()) {
            if (elements instanceof LinkedHashSet) {
                return (LinkedHashSet<T>) elements;
            } else {
                return LinkedHashSet.ofAll(elements);
            }
        } else if (elements.isEmpty()) {
            return this;
        } else {
            final LinkedHashMap<T, Object> that = addAll(map, elements);
            if (that.size() == map.size()) {
                return this;
            } else {
                return new LinkedHashSet<>(that);
            }
        }
    }

    // -- Positional operations, in insertion order

    /**
     * The first element in insertion order.
     * <p>
     * Complexity: effectively O(1) (the first element of the insertion order).
     *
     * @return the element inserted first among those present
     * @throws java.util.NoSuchElementException if this set is empty
     */
    public T head() {
        if (isEmpty()) {
            throw new java.util.NoSuchElementException("head of empty LinkedHashSet");
        }
        return map.head()._1();
    }

    /**
     * The first element in insertion order, if any.
     *
     * @return {@code Some} of {@link #head()}, or {@code None} if this set is empty
     */
    public Option<T> headOption() {
        return isEmpty() ? Option.none() : Option.some(head());
    }

    /**
     * The last element in insertion order.
     * <p>
     * Complexity: effectively O(1) (the last element of the insertion order).
     *
     * @return the element inserted last among those present
     * @throws java.util.NoSuchElementException if this set is empty
     */
    public T last() {
        if (isEmpty()) {
            throw new java.util.NoSuchElementException("last of empty LinkedHashSet");
        }
        return map.last()._1();
    }

    /**
     * The last element in insertion order, if any.
     *
     * @return {@code Some} of {@link #last()}, or {@code None} if this set is empty
     */
    public Option<T> lastOption() {
        return isEmpty() ? Option.none() : Option.some(last());
    }

    /**
     * All elements but the last in insertion order.
     * <p>
     * Complexity: effectively O(1) (one element removed from the hash map, the insertion order sliced) on a set with no
     * removals. After removals, up to O(n): the markers of the removed elements next to the last element are walked
     * past, and the insertion order is rebuilt when the result holds more markers than elements.
     *
     * @return this set without its last element
     * @throws UnsupportedOperationException if this set is empty
     */
    public LinkedHashSet<T> init() {
        if (isEmpty()) {
            throw new UnsupportedOperationException("init of empty LinkedHashSet");
        }
        return with(map.slice(0, size() - 1));
    }

    /**
     * All elements but the last in insertion order, if this set is not empty.
     * <p>
     * Complexity: effectively O(1) (one {@code init}).
     *
     * @return {@code Some} of {@link #init()}, or {@code None} if this set is empty
     */
    public Option<LinkedHashSet<T>> initOption() {
        return isEmpty() ? Option.none() : Option.some(init());
    }

    /**
     * All elements but the first in insertion order.
     * <p>
     * Complexity: effectively O(1) (one element removed from the hash map, the insertion order sliced) on a set with no
     * removals. After removals, up to O(n): the markers of the removed elements next to the first element are walked
     * past, and the insertion order is rebuilt when the result holds more markers than elements.
     *
     * @return this set without its first element
     * @throws UnsupportedOperationException if this set is empty
     */
    public LinkedHashSet<T> tail() {
        if (isEmpty()) {
            throw new UnsupportedOperationException("tail of empty LinkedHashSet");
        }
        return with(map.slice(1, size()));
    }

    /**
     * All elements but the first in insertion order, if this set is not empty.
     * <p>
     * Complexity: effectively O(1) (one {@code tail}).
     *
     * @return {@code Some} of {@link #tail()}, or {@code None} if this set is empty
     */
    public Option<LinkedHashSet<T>> tailOption() {
        return isEmpty() ? Option.none() : Option.some(tail());
    }

    /**
     * The first {@code n} elements in insertion order: empty if {@code n <= 0}, this set if {@code n >= size()}.
     * <p>
     * Complexity: effectively O(min(n, size - n)) (the smaller of the kept and the removed elements is inserted into
     * or removed from the hash map; the insertion order is sliced). After removals, up to
     * O(n): finding the cut walks the insertion order from the nearer end past every marker of a removed one in the
     * way, and the insertion order is rebuilt when the result holds more markers than elements.
     *
     * @param n the number of elements to keep
     * @return the {@code n} elements inserted first
     */
    public LinkedHashSet<T> take(int n) {
        return with(map.slice(0, n));
    }

    /**
     * The last {@code n} elements in insertion order: empty if {@code n <= 0}, this set if {@code n >= size()}.
     * <p>
     * Complexity: effectively O(min(n, size - n)), that of {@link #take(int)}, counted from the other end.
     *
     * @param n the number of elements to keep
     * @return the {@code n} elements inserted last
     */
    public LinkedHashSet<T> takeRight(int n) {
        return n <= 0 ? empty() : with(map.slice(size() - n, size()));
    }

    /**
     * The longest prefix, in insertion order, of elements satisfying {@code predicate}.
     * <p>
     * Complexity: O(k) for a prefix of k elements (one walk), then one {@link #take(int)}.
     *
     * @param predicate tested on the elements from the first inserted
     * @return the elements before the first one not satisfying {@code predicate}
     * @throws NullPointerException if {@code predicate} is null
     */
    public LinkedHashSet<T> takeWhile(Predicate<? super T> predicate) {
        Objects.requireNonNull(predicate, "predicate is null");
        return with(map.slice(0, map.countLeadingKeys(predicate, true)));
    }

    /**
     * The longest prefix, in insertion order, of elements not satisfying {@code predicate}.
     * <p>
     * Complexity: O(k) for a prefix of k elements (one walk), then one {@link #take(int)}.
     *
     * @param predicate tested on the elements from the first inserted
     * @return the elements before the first one satisfying {@code predicate}
     * @throws NullPointerException if {@code predicate} is null
     */
    public LinkedHashSet<T> takeUntil(Predicate<? super T> predicate) {
        Objects.requireNonNull(predicate, "predicate is null");
        return with(map.slice(0, map.countLeadingKeys(predicate, false)));
    }

    /**
     * All elements but the first {@code n} in insertion order: this set if {@code n <= 0}, empty if
     * {@code n >= size()}.
     * <p>
     * Complexity: effectively O(min(n, size - n)), that of {@link #take(int)}.
     *
     * @param n the number of elements to drop
     * @return the elements after the {@code n} inserted first
     */
    public LinkedHashSet<T> drop(int n) {
        return with(map.slice(n, size()));
    }

    /**
     * All elements but the last {@code n} in insertion order: this set if {@code n <= 0}, empty if
     * {@code n >= size()}.
     * <p>
     * Complexity: effectively O(min(n, size - n)), that of {@link #take(int)}.
     *
     * @param n the number of elements to drop
     * @return the elements before the {@code n} inserted last
     */
    public LinkedHashSet<T> dropRight(int n) {
        return n <= 0 ? this : with(map.slice(0, size() - n));
    }

    /**
     * The elements from the first one, in insertion order, that does not satisfy {@code predicate}.
     * <p>
     * Complexity: O(k) for k dropped elements (one walk), then one {@link #drop(int)}.
     *
     * @param predicate tested on the elements from the first inserted
     * @return the elements from the first one not satisfying {@code predicate}
     * @throws NullPointerException if {@code predicate} is null
     */
    public LinkedHashSet<T> dropWhile(Predicate<? super T> predicate) {
        Objects.requireNonNull(predicate, "predicate is null");
        return with(map.slice(map.countLeadingKeys(predicate, true), size()));
    }

    /**
     * The elements from the first one, in insertion order, that satisfies {@code predicate}.
     * <p>
     * Complexity: O(k) for k dropped elements (one walk), then one {@link #drop(int)}.
     *
     * @param predicate tested on the elements from the first inserted
     * @return the elements from the first one satisfying {@code predicate}
     * @throws NullPointerException if {@code predicate} is null
     */
    public LinkedHashSet<T> dropUntil(Predicate<? super T> predicate) {
        Objects.requireNonNull(predicate, "predicate is null");
        return with(map.slice(map.countLeadingKeys(predicate, false), size()));
    }

    /**
     * The elements paired with their position in insertion order, from 0.
     * <p>
     * Complexity: O(n).
     *
     * @return the pairs (element, position), in order
     */
    public Vector<Tuple2<T, Integer>> zipWithIndex() {
        return map.zipKeysWithIndex();
    }

    /**
     * The blocks of {@code size} consecutive elements in insertion order; the last block is smaller when
     * {@code size} does not divide {@code size()}. The same as {@code sliding(size, size)}.
     * <p>
     * Complexity: O(n), that of {@link #sliding(int, int)} with a step of {@code size}.
     *
     * @param size the block size, positive
     * @return the blocks, in order; empty if this set is empty
     * @throws IllegalArgumentException if {@code size} is not positive
     */
    public Vector<LinkedHashSet<T>> grouped(int size) {
        return sliding(size, size);
    }

    /**
     * The windows of {@code size} consecutive elements in insertion order, each starting one element after the
     * previous. The same as {@code sliding(size, 1)}.
     * <p>
     * Complexity: O(n * size), that of {@link #sliding(int, int)} with a step of 1.
     *
     * @param size the window size, positive
     * @return the windows, in order; empty if this set is empty
     * @throws IllegalArgumentException if {@code size} is not positive
     */
    public Vector<LinkedHashSet<T>> sliding(int size) {
        return sliding(size, 1);
    }

    /**
     * The windows of {@code size} consecutive elements in insertion order, each starting {@code step} elements after
     * the previous. The window rule is {@link Vector}'s: the last window is shorter than {@code size} when it reaches
     * the end, a window whose elements all belong to the previous one is not produced, a set smaller than
     * {@code size} is one window and an empty set has none.
     * <p>
     * Complexity: O(n + (n / step) * min(size, n - size)): O(n) to drop the removed elements' markers from the
     * insertion order if there are any, then per window that of {@link #take(int)} on a window of {@code size}
     * elements, effectively O(min(size, n - size)).
     *
     * @param size the window size, positive
     * @param step the distance between two window starts, positive
     * @return the windows, in order
     * @throws IllegalArgumentException if {@code size} or {@code step} is not positive
     */
    public Vector<LinkedHashSet<T>> sliding(int size, int step) {
        return map.windows(size, step, LinkedHashSet::wrap);
    }

    /**
     * The maximal runs of consecutive elements, in insertion order, with the same key, computed once per element by
     * {@code classifier}; the runs together are this set.
     * <p>
     * Complexity: O(n) walk (plus O(n) to drop the removed elements' markers from the insertion order if there are
     * any), then per run that of {@link #take(int)} on the run.
     *
     * @param classifier the key of an element; two consecutive elements are in the same run when their keys are equal
     * @return the runs, in order; empty if this set is empty
     * @throws NullPointerException if {@code classifier} is null
     */
    public Vector<LinkedHashSet<T>> slideBy(Function<? super T, ?> classifier) {
        return map.runsByKey(classifier, LinkedHashSet::wrap);
    }

    // this set when `that` is its own map, the empty set when `that` is empty, otherwise a set over `that`
    private LinkedHashSet<T> with(LinkedHashMap<T, Object> that) {
        return that == map ? this : that.isEmpty() ? empty() : new LinkedHashSet<>(that);
    }

    // -- Object

    @Override
    public boolean equals(@Nullable Object o) {
        return Collections.equals(this, o);
    }

    @Override
    public int hashCode() {
        return Collections.hashUnordered(this);
    }

    @Override
    public String toString() {
        return mkString("LinkedHashSet(", ", ", ")");
    }

    private static <T extends @Nullable Object> LinkedHashMap<T, Object> addAll(LinkedHashMap<T, Object> initial,
            Iterable<? extends T> additional) {
        LinkedHashMap<T, Object> that = initial;
        for (T t : additional) {
            Objects.requireNonNull(t, "LinkedHashSet: element is null");
            that = that.putIfAbsent(t, t);
        }
        return that;
    }

}
