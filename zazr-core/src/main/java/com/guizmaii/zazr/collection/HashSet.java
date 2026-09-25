package com.guizmaii.zazr.collection;

import com.guizmaii.zazr.*;
import com.guizmaii.zazr.collection.internal.Collections;
import com.guizmaii.zazr.collection.internal.HashArrayMappedTrie;
import com.guizmaii.zazr.collection.internal.Iterator;
import com.guizmaii.zazr.control.Either;
import com.guizmaii.zazr.control.Option;
import java.io.*;
import java.util.ArrayList;
import java.util.Objects;
import java.util.function.*;
import java.util.stream.Collector;
import org.jspecify.annotations.Nullable;

/**
 * An immutable {@code HashSet} implementation.
 *
 * @param <T> Component type
 * @author Ruslan Sennov, Patryk Najda, Daniel Dietrich
 */
public final class HashSet<T extends @Nullable Object> implements Set<T> {

    private static final HashSet<?> EMPTY = new HashSet<>(HashArrayMappedTrie.empty());

    private final HashArrayMappedTrie<T, T> tree;

    private HashSet(HashArrayMappedTrie<T, T> tree) {
        this.tree = tree;
    }

    /**
     * Returns the empty HashSet.
     *
     * @param <T> Component type
     * @return The empty HashSet.
     */
    @SuppressWarnings("unchecked")
    public static <T extends @Nullable Object> HashSet<T> empty() {
        return (HashSet<T>) EMPTY;
    }

    /**
     * Returns a {@link java.util.stream.Collector} which may be used in conjunction with
     * {@link java.util.stream.Stream#collect(java.util.stream.Collector)} to obtain a {@link HashSet}.
     *
     * @param <T> Component type of the HashSet.
     * @return A com.guizmaii.zazr.collection.HashSet Collector.
     */
    public static <T extends @Nullable Object> Collector<T, ArrayList<T>, HashSet<T>> collector() {
        final Supplier<ArrayList<T>> supplier = ArrayList::new;
        final BiConsumer<ArrayList<T>, T> accumulator = ArrayList::add;
        final BinaryOperator<ArrayList<T>> combiner = (left, right) -> {
            left.addAll(right);
            return left;
        };
        final Function<ArrayList<T>, HashSet<T>> finisher = HashSet::ofAll;
        return Collector.of(supplier, accumulator, combiner, finisher);
    }

    /**
     * Narrows a {@code HashSet<? extends T>} to {@code HashSet<T>} via a
     * type-safe cast. Safe here because the set is immutable and no elements
     * can be added that would violate the type (covariance)
     *
     * @param hashSet the set to narrow
     * @param <T>     the target element type
     * @return the same set viewed as {@code HashSet<T>}
     */
    @SuppressWarnings("unchecked")
    public static <T extends @Nullable Object> HashSet<T> narrow(HashSet<? extends T> hashSet) {
        return (HashSet<T>) hashSet;
    }

    /**
     * Returns a singleton {@code HashSet}, i.e. a {@code HashSet} of one element.
     *
     * @param element An element.
     * @param <T>     The component type
     * @return A new HashSet instance containing the given element
     */
    public static <T extends @Nullable Object> HashSet<T> of(T element) {
        return HashSet.<T> empty().add(element);
    }

    /**
     * Creates a HashSet of the given elements.
     *
     * <pre>{@code HashSet.of(1, 2, 3, 4)}</pre>
     *
     * @param <T>      Component type of the HashSet.
     * @param elements Zero or more elements.
     * @return A set containing the given elements.
     * @throws NullPointerException if {@code elements} is null
     */
    @SafeVarargs
    public static <T extends @Nullable Object> HashSet<T> of(T ... elements) {
        Objects.requireNonNull(elements, "elements is null");
        HashArrayMappedTrie<T, T> tree = HashArrayMappedTrie.empty();
        for (T element : elements) {
            Objects.requireNonNull(element, "HashSet.of: element is null");
            tree = tree.put(element, element);
        }
        return tree.isEmpty() ? empty() : new HashSet<>(tree);
    }

    /**
     * Returns a HashSet containing the distinct results of applying {@code f} to the integers
     * {@code 0} through {@code n - 1}. Because a HashSet deduplicates its elements, the resulting
     * set may contain fewer than {@code n} elements if {@code f} produces duplicate values.
     *
     * @param <T> Component type of the HashSet
     * @param n   The number of times {@code f} is invoked
     * @param f   The Function computing element values
     * @return A HashSet consisting of the distinct elements {@code f(0),f(1), ..., f(n - 1)}
     * @throws NullPointerException if {@code f} is null
     */
    public static <T extends @Nullable Object> HashSet<T> tabulate(int n, Function<? super Integer, ? extends T> f) {
        Objects.requireNonNull(f, "f is null");
        return Collections.tabulate(n, f, HashSet.empty(), HashSet::of);
    }

    /**
     * Returns a HashSet containing the distinct values returned by {@code n} calls to a given
     * Supplier {@code s}.
     *
     * @param <T> Component type of the HashSet
     * @param n   The number of times {@code s} is invoked
     * @param s   The Supplier computing element values
     * @return A HashSet of at most {@code n} distinct values supplied by {@code s}.
     * @throws NullPointerException if {@code s} is null
     */
    public static <T extends @Nullable Object> HashSet<T> fill(int n, Supplier<? extends T> s) {
        Objects.requireNonNull(s, "s is null");
        return Collections.fill(n, s, HashSet.empty(), HashSet::of);
    }

    /**
     * Creates a HashSet of the given elements.
     *
     * @param elements Set elements
     * @param <T>      The value type
     * @return A HashSet containing the given elements; if {@code elements} is already a
     *         HashSet, it is returned unchanged.
     */
    @SuppressWarnings("unchecked")
    public static <T extends @Nullable Object> HashSet<T> ofAll(Iterable<? extends T> elements) {
        Objects.requireNonNull(elements, "elements is null");
        if (elements instanceof HashSet) {
            return (HashSet<T>) elements;
        } else {
            final HashArrayMappedTrie<T, T> tree = addAll(HashArrayMappedTrie.empty(), elements);
            return tree.isEmpty() ? empty() : new HashSet<>(tree);
        }
    }

    /**
     * Creates a HashSet that contains the elements of the given {@link java.util.stream.Stream}.
     *
     * @param javaStream A {@link java.util.stream.Stream}
     * @param <T>        Component type of the Stream.
     * @return A HashSet containing the given elements.
     */
    public static <T extends @Nullable Object> HashSet<T> ofAll(java.util.stream.Stream<? extends T> javaStream) {
        Objects.requireNonNull(javaStream, "javaStream is null");
        return HashSet.ofAll(Iterator.ofAll(javaStream.iterator()));
    }

    /**
     * The union of nested iterables. Static, like every {@code flatten} in zazr, because Java cannot demand of an
     * instance method that the receiver's element type be a collection. The outer iterable and each inner one are
     * iterated once, so one-shot iterables are accepted.
     * <p>
     * Complexity: effectively O(n) for n inner elements in total, one insertion each.
     *
     * @param nested Iterables of elements
     * @param <T>    Component type of the inner iterables
     * @return the distinct inner elements
     * @throws NullPointerException if {@code nested}, an inner iterable or an element is null
     */
    public static <T extends @Nullable Object> HashSet<T> flatten(Iterable<? extends Iterable<? extends T>> nested) {
        Objects.requireNonNull(nested, "nested is null");
        HashArrayMappedTrie<T, T> all = HashArrayMappedTrie.empty();
        for (Iterable<? extends T> inner : nested) {
            all = addAll(all, inner);
        }
        return all.isEmpty() ? empty() : new HashSet<>(all);
    }

    /**
     * Creates a HashSet from boolean values.
     *
     * @param elements boolean values
     * @return A new HashSet of Boolean values
     * @throws NullPointerException if elements is null
     */
    public static HashSet<Boolean> ofAll(boolean ... elements) {
        Objects.requireNonNull(elements, "elements is null");
        return HashSet.ofAll(Iterator.ofAll(elements));
    }

    /**
     * Creates a HashSet from byte values.
     *
     * @param elements byte values
     * @return A new HashSet of Byte values
     * @throws NullPointerException if elements is null
     */
    public static HashSet<Byte> ofAll(byte ... elements) {
        Objects.requireNonNull(elements, "elements is null");
        return HashSet.ofAll(Iterator.ofAll(elements));
    }

    /**
     * Creates a HashSet from char values.
     *
     * @param elements char values
     * @return A new HashSet of Character values
     * @throws NullPointerException if elements is null
     */
    public static HashSet<Character> ofAll(char ... elements) {
        Objects.requireNonNull(elements, "elements is null");
        return HashSet.ofAll(Iterator.ofAll(elements));
    }

    /**
     * Creates a HashSet from double values.
     *
     * @param elements double values
     * @return A new HashSet of Double values
     * @throws NullPointerException if elements is null
     */
    public static HashSet<Double> ofAll(double ... elements) {
        Objects.requireNonNull(elements, "elements is null");
        return HashSet.ofAll(Iterator.ofAll(elements));
    }

    /**
     * Creates a HashSet from float values.
     *
     * @param elements float values
     * @return A new HashSet of Float values
     * @throws NullPointerException if elements is null
     */
    public static HashSet<Float> ofAll(float ... elements) {
        Objects.requireNonNull(elements, "elements is null");
        return HashSet.ofAll(Iterator.ofAll(elements));
    }

    /**
     * Creates a HashSet from int values.
     *
     * @param elements int values
     * @return A new HashSet of Integer values
     * @throws NullPointerException if elements is null
     */
    public static HashSet<Integer> ofAll(int ... elements) {
        Objects.requireNonNull(elements, "elements is null");
        return HashSet.ofAll(Iterator.ofAll(elements));
    }

    /**
     * Creates a HashSet from long values.
     *
     * @param elements long values
     * @return A new HashSet of Long values
     * @throws NullPointerException if elements is null
     */
    public static HashSet<Long> ofAll(long ... elements) {
        Objects.requireNonNull(elements, "elements is null");
        return HashSet.ofAll(Iterator.ofAll(elements));
    }

    /**
     * Creates a HashSet from short values.
     *
     * @param elements short values
     * @return A new HashSet of Short values
     * @throws NullPointerException if elements is null
     */
    public static HashSet<Short> ofAll(short ... elements) {
        Objects.requireNonNull(elements, "elements is null");
        return HashSet.ofAll(Iterator.ofAll(elements));
    }

    /**
     * Creates a HashSet of int numbers starting from {@code from}, extending to {@code toExclusive - 1}.
     * <p>
     * Examples:
     * <pre>
     * {@code
     * HashSet.range(0, 0)  // = HashSet()
     * HashSet.range(2, 0)  // = HashSet()
     * HashSet.range(-2, 2) // = HashSet(-2, -1, 0, 1)
     * }
     * </pre>
     *
     * @param from        the first number
     * @param toExclusive the last number + 1
     * @return a range of int values as specified or the empty range if {@code from >= toExclusive}
     */
    public static HashSet<Integer> range(int from, int toExclusive) {
        return HashSet.ofAll(Iterator.range(from, toExclusive));
    }

    /**
     * Creates a HashSet of char numbers starting from {@code from}, extending to {@code toExclusive - 1}.
     * <p>
     * Examples:
     * <pre>
     * {@code
     * HashSet.range('a', 'a')  // = HashSet()
     * HashSet.range('c', 'a')  // = HashSet()
     * HashSet.range('a', 'd')  // = HashSet('a', 'b', 'c')
     * }
     * </pre>
     *
     * @param from        the first char
     * @param toExclusive the last char + 1
     * @return a range of char values as specified or the empty range if {@code from >= toExclusive}
     */
    public static HashSet<Character> range(char from, char toExclusive) {
        return HashSet.ofAll(Iterator.range(from, toExclusive));
    }

    /**
     * Creates a HashSet of int numbers starting from {@code from}, extending up to but excluding
     * {@code toExclusive}, in increments of {@code step}.
     * <p>
     * Examples:
     * <pre>
     * {@code
     * HashSet.rangeBy(1, 3, 1)  // = HashSet(1, 2)
     * HashSet.rangeBy(1, 4, 2)  // = HashSet(1, 3)
     * HashSet.rangeBy(4, 1, -2) // = HashSet(4, 2)
     * HashSet.rangeBy(4, 1, 2)  // = HashSet()
     * }
     * </pre>
     *
     * @param from        the first number
     * @param toExclusive the exclusive bound (never part of the result)
     * @param step        the step
     * @return a range of int values as specified or the empty range if<br>
     * {@code from >= toExclusive} and {@code step > 0} or<br>
     * {@code from <= toExclusive} and {@code step < 0}
     * @throws IllegalArgumentException if {@code step} is zero
     */
    public static HashSet<Integer> rangeBy(int from, int toExclusive, int step) {
        return HashSet.ofAll(Iterator.rangeBy(from, toExclusive, step));
    }

    /**
     * Creates a HashSet of char numbers starting from {@code from}, extending up to but excluding
     * {@code toExclusive}, in increments of {@code step}.
     * <p>
     * Examples:
     * <pre>
     * {@code
     * HashSet.rangeBy('a', 'c', 1)  // = HashSet('a', 'b')
     * HashSet.rangeBy('a', 'd', 2)  // = HashSet('a', 'c')
     * HashSet.rangeBy('d', 'a', -2) // = HashSet('d', 'b')
     * HashSet.rangeBy('d', 'a', 2)  // = HashSet()
     * }
     * </pre>
     *
     * @param from        the first char
     * @param toExclusive the exclusive bound (never part of the result)
     * @param step        the step
     * @return a range of char values as specified or the empty range if<br>
     * {@code from >= toExclusive} and {@code step > 0} or<br>
     * {@code from <= toExclusive} and {@code step < 0}
     * @throws IllegalArgumentException if {@code step} is zero
     */
    public static HashSet<Character> rangeBy(char from, char toExclusive, int step) {
        return HashSet.ofAll(Iterator.rangeBy(from, toExclusive, step));
    }

    /**
     * Creates a HashSet of double numbers starting from {@code from}, extending up to but excluding
     * {@code toExclusive}, in increments of {@code step}.
     * <p>
     * Examples:
     * <pre>
     * {@code
     * HashSet.rangeBy(1.0, 3.0, 1.0)  // = HashSet(1.0, 2.0)
     * HashSet.rangeBy(1.0, 4.0, 2.0)  // = HashSet(1.0, 3.0)
     * HashSet.rangeBy(4.0, 1.0, -2.0) // = HashSet(4.0, 2.0)
     * HashSet.rangeBy(4.0, 1.0, 2.0)  // = HashSet()
     * }
     * </pre>
     *
     * @param from        the first double
     * @param toExclusive the exclusive bound (never part of the result)
     * @param step        the step
     * @return a range of double values as specified or the empty range if<br>
     * {@code from >= toExclusive} and {@code step > 0} or<br>
     * {@code from <= toExclusive} and {@code step < 0}
     * @throws IllegalArgumentException if {@code step} is zero
     */
    public static HashSet<Double> rangeBy(double from, double toExclusive, double step) {
        return HashSet.ofAll(Iterator.rangeBy(from, toExclusive, step));
    }

    /**
     * Creates a HashSet of long numbers starting from {@code from}, extending to {@code toExclusive - 1}.
     * <p>
     * Examples:
     * <pre>
     * {@code
     * HashSet.range(0L, 0L)  // = HashSet()
     * HashSet.range(2L, 0L)  // = HashSet()
     * HashSet.range(-2L, 2L) // = HashSet(-2L, -1L, 0L, 1L)
     * }
     * </pre>
     *
     * @param from        the first number
     * @param toExclusive the last number + 1
     * @return a range of long values as specified or the empty range if {@code from >= toExclusive}
     */
    public static HashSet<Long> range(long from, long toExclusive) {
        return HashSet.ofAll(Iterator.range(from, toExclusive));
    }

    /**
     * Creates a HashSet of long numbers starting from {@code from}, extending up to but excluding
     * {@code toExclusive}, in increments of {@code step}.
     * <p>
     * Examples:
     * <pre>
     * {@code
     * HashSet.rangeBy(1L, 3L, 1L)  // = HashSet(1L, 2L)
     * HashSet.rangeBy(1L, 4L, 2L)  // = HashSet(1L, 3L)
     * HashSet.rangeBy(4L, 1L, -2L) // = HashSet(4L, 2L)
     * HashSet.rangeBy(4L, 1L, 2L)  // = HashSet()
     * }
     * </pre>
     *
     * @param from        the first number
     * @param toExclusive the exclusive bound (never part of the result)
     * @param step        the step
     * @return a range of long values as specified or the empty range if<br>
     * {@code from >= toExclusive} and {@code step > 0} or<br>
     * {@code from <= toExclusive} and {@code step < 0}
     * @throws IllegalArgumentException if {@code step} is zero
     */
    public static HashSet<Long> rangeBy(long from, long toExclusive, long step) {
        return HashSet.ofAll(Iterator.rangeBy(from, toExclusive, step));
    }

    /**
     * Creates a HashSet of int numbers starting from {@code from}, extending to {@code toInclusive}.
     * <p>
     * Examples:
     * <pre>
     * {@code
     * HashSet.rangeClosed(0, 0)  // = HashSet(0)
     * HashSet.rangeClosed(2, 0)  // = HashSet()
     * HashSet.rangeClosed(-2, 2) // = HashSet(-2, -1, 0, 1, 2)
     * }
     * </pre>
     *
     * @param from        the first number
     * @param toInclusive the last number
     * @return a range of int values as specified or the empty range if {@code from > toInclusive}
     */
    public static HashSet<Integer> rangeClosed(int from, int toInclusive) {
        return HashSet.ofAll(Iterator.rangeClosed(from, toInclusive));
    }

    /**
     * Creates a HashSet of char numbers starting from {@code from}, extending to {@code toInclusive}.
     * <p>
     * Examples:
     * <pre>
     * {@code
     * HashSet.rangeClosed('a', 'a')  // = HashSet('a')
     * HashSet.rangeClosed('c', 'a')  // = HashSet()
     * HashSet.rangeClosed('a', 'c')  // = HashSet('a', 'b', 'c')
     * }
     * </pre>
     *
     * @param from        the first char
     * @param toInclusive the last char
     * @return a range of char values as specified or the empty range if {@code from > toInclusive}
     */
    public static HashSet<Character> rangeClosed(char from, char toInclusive) {
        return HashSet.ofAll(Iterator.rangeClosed(from, toInclusive));
    }

    /**
     * Creates a HashSet of int numbers starting from {@code from}, extending to {@code toInclusive},
     * with {@code step}.
     * <p>
     * Examples:
     * <pre>
     * {@code
     * HashSet.rangeClosedBy(1, 3, 1)  // = HashSet(1, 2, 3)
     * HashSet.rangeClosedBy(1, 4, 2)  // = HashSet(1, 3)
     * HashSet.rangeClosedBy(4, 1, -2) // = HashSet(4, 2)
     * HashSet.rangeClosedBy(4, 1, 2)  // = HashSet()
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
    public static HashSet<Integer> rangeClosedBy(int from, int toInclusive, int step) {
        return HashSet.ofAll(Iterator.rangeClosedBy(from, toInclusive, step));
    }

    /**
     * Creates a HashSet of char numbers starting from {@code from}, extending to {@code toInclusive},
     * with {@code step}.
     * <p>
     * Examples:
     * <pre>
     * {@code
     * HashSet.rangeClosedBy('a', 'c', 1)  // = HashSet('a', 'b', 'c')
     * HashSet.rangeClosedBy('a', 'd', 2)  // = HashSet('a', 'c')
     * HashSet.rangeClosedBy('d', 'a', -2) // = HashSet('d', 'b')
     * HashSet.rangeClosedBy('d', 'a', 2)  // = HashSet()
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
    public static HashSet<Character> rangeClosedBy(char from, char toInclusive, int step) {
        return HashSet.ofAll(Iterator.rangeClosedBy(from, toInclusive, step));
    }

    /**
     * Creates a HashSet of double numbers starting from {@code from}, extending to {@code toInclusive},
     * with {@code step}.
     * <p>
     * Examples:
     * <pre>
     * {@code
     * HashSet.rangeClosedBy(1.0, 3.0, 1.0)  // = HashSet(1.0, 2.0, 3.0)
     * HashSet.rangeClosedBy(1.0, 4.0, 2.0)  // = HashSet(1.0, 3.0)
     * HashSet.rangeClosedBy(4.0, 1.0, -2.0) // = HashSet(4.0, 2.0)
     * HashSet.rangeClosedBy(4.0, 1.0, 2.0)  // = HashSet()
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
    public static HashSet<Double> rangeClosedBy(double from, double toInclusive, double step) {
        return HashSet.ofAll(Iterator.rangeClosedBy(from, toInclusive, step));
    }

    /**
     * Creates a HashSet of long numbers starting from {@code from}, extending to {@code toInclusive}.
     * <p>
     * Examples:
     * <pre>
     * {@code
     * HashSet.rangeClosed(0L, 0L)  // = HashSet(0L)
     * HashSet.rangeClosed(2L, 0L)  // = HashSet()
     * HashSet.rangeClosed(-2L, 2L) // = HashSet(-2L, -1L, 0L, 1L, 2L)
     * }
     * </pre>
     *
     * @param from        the first number
     * @param toInclusive the last number
     * @return a range of long values as specified or the empty range if {@code from > toInclusive}
     */
    public static HashSet<Long> rangeClosed(long from, long toInclusive) {
        return HashSet.ofAll(Iterator.rangeClosed(from, toInclusive));
    }

    /**
     * Creates a HashSet of long numbers starting from {@code from}, extending to {@code toInclusive},
     * with {@code step}.
     * <p>
     * Examples:
     * <pre>
     * {@code
     * HashSet.rangeClosedBy(1L, 3L, 1L)  // = HashSet(1L, 2L, 3L)
     * HashSet.rangeClosedBy(1L, 4L, 2L)  // = HashSet(1L, 3L)
     * HashSet.rangeClosedBy(4L, 1L, -2L) // = HashSet(4L, 2L)
     * HashSet.rangeClosedBy(4L, 1L, 2L)  // = HashSet()
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
    public static HashSet<Long> rangeClosedBy(long from, long toInclusive, long step) {
        return HashSet.ofAll(Iterator.rangeClosedBy(from, toInclusive, step));
    }

    /**
     * {@inheritDoc}
     * <p>
     * Complexity: effectively O(1) (one hash lookup, then a path copy of the trie when the element is new).
     */
    @Override
    public HashSet<T> add(T element) {
        Objects.requireNonNull(element, "HashSet.add: element is null");
        return contains(element) ? this : new HashSet<>(tree.put(element, element));
    }

    /**
     * {@inheritDoc}
     * <p>
     * Complexity: O(m) for m elements, each an effectively O(1) insertion; O(1) when this set is empty and {@code
     * elements} is a HashSet, which is returned as is.
     */
    @Override
    public HashSet<T> addAll(Iterable<? extends T> elements) {
        Objects.requireNonNull(elements, "elements is null");
        if (isEmpty() && elements instanceof HashSet) {
            @SuppressWarnings("unchecked")
            final HashSet<T> set = (HashSet<T>) elements;
            return set;
        }
        final HashArrayMappedTrie<T, T> that = addAll(tree, elements);
        if (that.size() == tree.size()) {
            return this;
        } else {
            return new HashSet<>(that);
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * Complexity: effectively O(1) (one hash lookup).
     */
    @Override
    public boolean contains(T element) {
        return tree.containsKey(element);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Complexity: O(n + m) for a set of m elements (a hash set of them, then one filter pass).
     */
    @Override
    public HashSet<T> diff(Set<? extends T> elements) {
        Objects.requireNonNull(elements, "elements is null");
        if (isEmpty() || elements.isEmpty()) {
            return this;
        } else {
            return removeAll(elements);
        }
    }

    @Override
    public HashSet<T> filter(Predicate<? super T> predicate) {
        Objects.requireNonNull(predicate, "predicate is null");
        final HashSet<T> filtered = HashSet.ofAll(Iterator.ofAll(this).filter(predicate));

        if (filtered.isEmpty()) {
            return empty();
        } else if (filtered.size() == size()) {
            return this;
        } else {
            return filtered;
        }
    }

    @Override
    public HashSet<T> reject(Predicate<? super T> predicate) {
        Objects.requireNonNull(predicate, "predicate is null");
        return filter(predicate.negate());
    }

    @Override
    public <U extends @Nullable Object> HashSet<U> flatMap(Function<? super T, ? extends Iterable<? extends U>> mapper) {
        Objects.requireNonNull(mapper, "mapper is null");
        if (isEmpty()) {
            return empty();
        } else {
            final HashArrayMappedTrie<U, U> that = foldLeft(HashArrayMappedTrie.empty(),
                    (tree, t) -> addAll(tree, mapper.apply(t)));
            return new HashSet<>(that);
        }
    }

    @Override
    public <C extends @Nullable Object> Map<C, HashSet<T>> groupBy(Function<? super T, ? extends C> classifier) {
        return Collections.groupBy(this, classifier, HashSet::ofAll);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Complexity: O(n + m) for a set of m elements (the smaller set is filtered against a hash set of the larger one).
     */
    @Override
    public HashSet<T> intersect(Set<? extends T> elements) {
        Objects.requireNonNull(elements, "elements is null");
        if (isEmpty() || elements.isEmpty()) {
            return empty();
        } else {
            final int size = size();
            if (size <= elements.size()) {
                return retainAll(elements);
            } else {
                final HashSet<T> results = HashSet.<T> ofAll(elements).retainAll(this);
                return (size == results.size()) ? this : results;
            }
        }
    }

    @Override
    public boolean isEmpty() {
        return tree.isEmpty();
    }

    @Override
    public int size() {
        return tree.size();
    }

    /**
     * {@inheritDoc}
     * <p>
     * Complexity: O(1) to create; a whole walk is O(n).
     */
    @Override
    public java.util.Iterator<T> iterator() {
        return tree.keysIterator();
    }

    @Override
    public <U extends @Nullable Object> HashSet<U> map(Function<? super T, ? extends U> mapper) {
        Objects.requireNonNull(mapper, "mapper is null");
        if (isEmpty()) {
            return empty();
        } else {
            final HashArrayMappedTrie<U, U> that = foldLeft(HashArrayMappedTrie.empty(), (tree, t) -> {
                final U u = mapper.apply(t);
                return tree.put(u, u);
            });
            return new HashSet<>(that);
        }
    }

    @Override
    public <U extends @Nullable Object> HashSet<U> collect(Function<? super T, ? extends Option<? extends U>> mapper) {
        Objects.requireNonNull(mapper, "mapper is null");
        if (isEmpty()) {
            return empty();
        }
        HashArrayMappedTrie<U, U> that = HashArrayMappedTrie.empty();
        for (T t : this) {
            final Option<? extends U> collected = Objects.requireNonNull(mapper.apply(t), "HashSet.collect: mapper returned null");
            if (collected.isDefined()) {
                final U u = collected.get();
                that = that.put(u, u);
            }
        }
        return new HashSet<>(that);
    }

    @Override
    public <U extends @Nullable Object> HashSet<U> as(U value) {
        return map(ignored -> value);
    }

    @Override
    public HashSet<T> orElse(Iterable<? extends T> other) {
        return isEmpty() ? ofAll(other) : this;
    }

    @Override
    public HashSet<T> orElse(Supplier<? extends Iterable<? extends T>> supplier) {
        return isEmpty() ? ofAll(supplier.get()) : this;
    }

    @Override
    public Tuple2<HashSet<T>, HashSet<T>> partition(Predicate<? super T> predicate) {
        return Collections.partition(this, HashSet::ofAll, predicate);
    }

    /**
     * Splits the elements into a left and a right side according to the {@link Either} {@code f} returns for each: the
     * generalisation of {@link #partition(Predicate)}. One pass, {@code f} called once per element, no intermediate
     * collection of {@code Either}s. Values equal on one side are kept once.
     * <p>
     * Complexity: effectively O(n), one insertion per element.
     *
     * @param f   Classifies an element
     * @param <L> Component type of the left side
     * @param <R> Component type of the right side
     * @return the left values and the right values
     * @throws NullPointerException if {@code f} is null or returns null
     */
    public <L extends @Nullable Object, R extends @Nullable Object> Tuple2<HashSet<L>, HashSet<R>> partitionMap(Function<? super T, ? extends Either<? extends L, ? extends R>> f) {
        Objects.requireNonNull(f, "f is null");
        HashArrayMappedTrie<L, L> lefts = HashArrayMappedTrie.empty();
        HashArrayMappedTrie<R, R> rights = HashArrayMappedTrie.empty();
        for (T element : this) {
            switch (Objects.requireNonNull(f.apply(element), "HashSet.partitionMap: f returned null")) {
                case Either.Left(var left) -> lefts = lefts.put(left, left);
                case Either.Right(var right) -> rights = rights.put(right, right);
            }
        }
        return Tuple.of(lefts.isEmpty() ? empty() : new HashSet<>(lefts), rights.isEmpty() ? empty() : new HashSet<>(rights));
    }

    @Override
    public HashSet<T> tap(Consumer<? super T> action) {
        Objects.requireNonNull(action, "action is null");
        forEach(action);
        return this;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Complexity: effectively O(1) (one hash lookup and a path copy of the trie).
     */
    @Override
    public HashSet<T> remove(T element) {
        final HashArrayMappedTrie<T, T> newTree = tree.remove(element);
        return (newTree == tree) ? this : new HashSet<>(newTree);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Complexity: O(n + m) for m given elements (a hash set of them, then one filter pass).
     */
    @Override
    public HashSet<T> removeAll(Iterable<? extends T> elements) {
        return Collections.removeAll(this, elements, kept -> filter(kept));
    }

    /**
     * {@inheritDoc}
     * <p>
     * Complexity: effectively O(1) (one lookup, one removal and one insertion).
     */
    @Override
    public HashSet<T> replace(T currentElement, T newElement) {
        if (tree.containsKey(currentElement)) {
            return remove(currentElement).add(newElement);
        } else {
            return this;
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * Complexity: effectively O(1), that of {@link #replace(Object, Object)}: a set holds an element once.
     */
    @Override
    public HashSet<T> replaceAll(T currentElement, T newElement) {
        return replace(currentElement, newElement);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Complexity: O(n + m) for m given elements (a hash set of them, then one filter pass).
     */
    @Override
    public HashSet<T> retainAll(Iterable<? extends T> elements) {
        return Collections.retainAll(this, elements, kept -> filter(kept));
    }

    @Override
    public java.util.HashSet<T> toJavaSet() {
        return toJavaSet(java.util.HashSet::new);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Complexity: O(m) for a set of m elements, each an effectively O(1) insertion; this set or a HashSet argument is
     * returned as is when the other side is empty.
     */
    @SuppressWarnings("unchecked")
    @Override
    public HashSet<T> union(Set<? extends T> elements) {
        Objects.requireNonNull(elements, "elements is null");
        if (isEmpty()) {
            if (elements instanceof HashSet) {
                return (HashSet<T>) elements;
            } else {
                return HashSet.ofAll(elements);
            }
        } else if (elements.isEmpty()) {
            return this;
        } else {
            final HashArrayMappedTrie<T, T> that = addAll(tree, elements);
            if (that.size() == tree.size()) {
                return this;
            } else {
                return new HashSet<>(that);
            }
        }
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
        return mkString("HashSet(", ", ", ")");
    }

    private static <T extends @Nullable Object> HashArrayMappedTrie<T, T> addAll(HashArrayMappedTrie<T, T> initial,
            Iterable<? extends T> additional) {
        HashArrayMappedTrie<T, T> that = initial;
        for (T t : additional) {
            Objects.requireNonNull(t, "HashSet: element is null");
            that = that.put(t, t);
        }
        return that;
    }

}
