package com.guizmaii.zazr.collection;

import com.guizmaii.zazr.*;
import com.guizmaii.zazr.collection.internal.Collections;
import com.guizmaii.zazr.collection.internal.Comparators;
import com.guizmaii.zazr.collection.internal.Iterator;
import com.guizmaii.zazr.collection.internal.JavaConverters;
import com.guizmaii.zazr.collection.internal.RedBlackTree;
import com.guizmaii.zazr.collection.internal.RedBlackTreeModule;
import com.guizmaii.zazr.collection.internal.TreeViews;
import com.guizmaii.zazr.control.Option;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Objects;
import java.util.function.*;
import java.util.stream.Collector;
import org.jspecify.annotations.Nullable;

/**
 * SortedSet implementation, backed by a Red/Black Tree.
 *
 * @param <T> Component type
 * @author Daniel Dietrich
 */
// DEV-NOTE: it is not possible to create an EMPTY TreeSet without a Comparator type in scope
public final class TreeSet<T extends @Nullable Object> implements SortedSet<T> {

    private final RedBlackTree<T> tree;

    TreeSet(RedBlackTree<T> tree) {
        this.tree = tree;
    }

    /**
     * Returns a {@link java.util.stream.Collector} which may be used in conjunction with
     * {@link java.util.stream.Stream#collect(java.util.stream.Collector)} to obtain a {@link TreeSet}.
     * <p>
     * The natural comparator is used to compare TreeSet elements.
     *
     * @param <T> Component type of the TreeSet.
     * @return A com.guizmaii.zazr.collection.TreeSet Collector.
     */
    public static <T extends Comparable<? super T>> Collector<T, ArrayList<T>, TreeSet<T>> collector() {
        return collector(Comparators.naturalComparator());
    }

    /**
     * Returns a {@link java.util.stream.Collector} which may be used in conjunction with
     * {@link java.util.stream.Stream#collect(java.util.stream.Collector)} to obtain a {@link TreeSet}.
     *
     * @param <T>        Component type of the TreeSet.
     * @param comparator An element comparator
     * @return A com.guizmaii.zazr.collection.TreeSet Collector.
     */
    public static <T extends @Nullable Object> Collector<T, ArrayList<T>, TreeSet<T>> collector(Comparator<? super T> comparator) {
        Objects.requireNonNull(comparator, "comparator is null");
        final Supplier<ArrayList<T>> supplier = ArrayList::new;
        final BiConsumer<ArrayList<T>, T> accumulator = ArrayList::add;
        final BinaryOperator<ArrayList<T>> combiner = (left, right) -> {
            left.addAll(right);
            return left;
        };
        final Function<ArrayList<T>, TreeSet<T>> finisher = list -> TreeSet.ofAll(comparator, list);
        return Collector.of(supplier, accumulator, combiner, finisher);
    }

    public static <T extends Comparable<? super T>> TreeSet<T> empty() {
        return empty(Comparators.naturalComparator());
    }

    public static <T extends @Nullable Object> TreeSet<T> empty(Comparator<? super T> comparator) {
        Objects.requireNonNull(comparator, "comparator is null");
        return new TreeSet<>(RedBlackTree.empty(comparator));
    }

    /**
     * Narrows a {@code TreeSet<? extends T>} to {@code TreeSet<T>} via a
     * type-safe cast. Safe here because the set is immutable and no elements
     * can be added that would violate the type (covariance)
     * <p>
     * CAUTION: The underlying {@code Comparator} might fail!
     *
     * @param treeSet the set to narrow
     * @param <T>     the target element type
     * @return the same set viewed as {@code TreeSet<T>}
     */
    @SuppressWarnings("unchecked")
    public static <T extends @Nullable Object> TreeSet<T> narrow(TreeSet<? extends T> treeSet) {
        return (TreeSet<T>) treeSet;
    }

    public static <T extends Comparable<? super T>> TreeSet<T> of(T value) {
        return of(Comparators.naturalComparator(), value);
    }

    public static <T extends @Nullable Object> TreeSet<T> of(Comparator<? super T> comparator, T value) {
        Objects.requireNonNull(comparator, "comparator is null");
        return new TreeSet<>(RedBlackTree.of(comparator, value));
    }

    @SuppressWarnings("varargs")
    @SafeVarargs
    public static <T extends Comparable<? super T>> TreeSet<T> of(T ... values) {
        return TreeSet.<T> of(Comparators.naturalComparator(), values);
    }

    @SuppressWarnings("varargs")
    @SafeVarargs
    public static <T extends @Nullable Object> TreeSet<T> of(Comparator<? super T> comparator, T ... values) {
        Objects.requireNonNull(comparator, "comparator is null");
        Objects.requireNonNull(values, "values is null");
        return new TreeSet<>(RedBlackTree.of(comparator, values));
    }

    /**
     * Returns a TreeSet containing {@code n} values of a given Function {@code f}
     * over a range of integer values from 0 to {@code n - 1}.
     *
     * @param <T>        Component type of the TreeSet
     * @param comparator The comparator used to sort the elements
     * @param n          The number of times {@code f} is invoked (for indices {@code 0} through {@code n - 1});
     *                   the resulting TreeSet may contain fewer than {@code n} elements if {@code f} produces
     *                   values considered equal by the comparator
     * @param f          The Function computing element values
     * @return A TreeSet consisting of the distinct elements {@code f(0),f(1), ..., f(n - 1)}
     * @throws NullPointerException if {@code comparator} or {@code f} are null
     */
    public static <T extends @Nullable Object> TreeSet<T> tabulate(Comparator<? super T> comparator, int n, Function<? super Integer, ? extends T> f) {
        Objects.requireNonNull(comparator, "comparator is null");
        Objects.requireNonNull(f, "f is null");
        return Collections.tabulate(n, f, TreeSet.empty(comparator), values -> of(comparator, values));
    }

    /**
     * Returns a TreeSet containing {@code n} values of a given Function {@code f}
     * over a range of integer values from 0 to {@code n - 1}.
     * The underlying comparator is the natural comparator of T.
     *
     * @param <T> Component type of the TreeSet
     * @param n   The number of times {@code f} is invoked (for indices {@code 0} through {@code n - 1});
     *            the resulting TreeSet may contain fewer than {@code n} elements if {@code f} produces
     *            values considered equal by the comparator
     * @param f   The Function computing element values
     * @return A TreeSet consisting of the distinct elements {@code f(0),f(1), ..., f(n - 1)}
     * @throws NullPointerException if {@code f} is null
     */
    public static <T extends Comparable<? super T>> TreeSet<T> tabulate(int n, Function<? super Integer, ? extends T> f) {
        Objects.requireNonNull(f, "f is null");
        return tabulate(Comparators.naturalComparator(), n, f);
    }

    /**
     * Returns a TreeSet containing the values returned by {@code n} calls to a given Supplier {@code s}.
     *
     * @param <T>        Component type of the TreeSet
     * @param comparator The comparator used to sort the elements
     * @param n          The number of times {@code s} is invoked
     * @param s          The Supplier computing element values
     * @return A TreeSet of at most {@code n} elements, containing the values (deduplicated by the comparator) supplied by {@code s}.
     * @throws NullPointerException if {@code comparator} or {@code s} are null
     */
    public static <T extends @Nullable Object> TreeSet<T> fill(Comparator<? super T> comparator, int n, Supplier<? extends T> s) {
        Objects.requireNonNull(comparator, "comparator is null");
        Objects.requireNonNull(s, "s is null");
        return Collections.fill(n, s, TreeSet.empty(comparator), values -> of(comparator, values));
    }

    /**
     * Returns a TreeSet containing the values returned by {@code n} calls to a given Supplier {@code s}.
     * The underlying comparator is the natural comparator of T.
     *
     * @param <T> Component type of the TreeSet
     * @param n   The number of times {@code s} is invoked
     * @param s   The Supplier computing element values
     * @return A TreeSet of at most {@code n} elements, containing the values (deduplicated by the comparator) supplied by {@code s}.
     * @throws NullPointerException if {@code s} is null
     */
    public static <T extends Comparable<? super T>> TreeSet<T> fill(int n, Supplier<? extends T> s) {
        Objects.requireNonNull(s, "s is null");
        return fill(Comparators.naturalComparator(), n, s);
    }

    public static <T extends Comparable<? super T>> TreeSet<T> ofAll(Iterable<? extends T> values) {
        return ofAll(Comparators.naturalComparator(), values);
    }

    @SuppressWarnings("unchecked")
    public static <T extends @Nullable Object> TreeSet<T> ofAll(Comparator<? super T> comparator, Iterable<? extends T> values) {
        Objects.requireNonNull(comparator, "comparator is null");
        Objects.requireNonNull(values, "values is null");
        if (values instanceof TreeSet && ((TreeSet<?>) values).comparator() == comparator) {
            return (TreeSet<T>) values;
        } else if (JavaConverters.underlying(values) instanceof TreeSet<?> underlying && underlying.comparator() == comparator) {
            return (TreeSet<T>) underlying;
        } else {
            // one read of the argument, which may be a one-shot Iterable: the emptiness is answered by the tree
            final RedBlackTree<T> tree = RedBlackTree.ofAll(comparator, values);
            return tree.isEmpty() ? empty(comparator) : new TreeSet<>(tree);
        }
    }

    public static <T extends Comparable<? super T>> TreeSet<T> ofAll(java.util.stream.Stream<? extends T> javaStream) {
        Objects.requireNonNull(javaStream, "javaStream is null");
        return ofAll(Iterator.ofAll(javaStream.iterator()));
    }

    public static <T extends @Nullable Object> TreeSet<T> ofAll(Comparator<? super T> comparator, java.util.stream.Stream<? extends T> javaStream) {
        Objects.requireNonNull(javaStream, "javaStream is null");
        return ofAll(comparator, Iterator.ofAll(javaStream.iterator()));
    }

    /**
     * The union of nested iterables, ordered by {@code comparator}. Static, like every {@code flatten} in Zazr, because
     * Java cannot demand of an instance method that the receiver's element type be a collection. The outer iterable
     * and each inner one are iterated once, so one-shot iterables are accepted.
     * <p>
     * Complexity: O(n log n) comparisons for n inner elements in total.
     *
     * @param comparator the order of the result
     * @param nested     Iterables of elements
     * @param <T>        Component type of the inner iterables
     * @return the distinct inner elements, in {@code comparator} order
     * @throws NullPointerException if {@code comparator}, {@code nested}, an inner iterable or an element is null
     */
    public static <T extends @Nullable Object> TreeSet<T> flatten(Comparator<? super T> comparator, Iterable<? extends Iterable<? extends T>> nested) {
        Objects.requireNonNull(comparator, "comparator is null");
        Objects.requireNonNull(nested, "nested is null");
        // the insertions of ofAll, inlined so that a null element is reported under this type's name
        RedBlackTree<T> tree = RedBlackTree.empty(comparator);
        for (Iterable<? extends T> inner : nested) {
            for (T element : inner) {
                tree = tree.insert(Objects.requireNonNull(element, "TreeSet.flatten: element is null"));
            }
        }
        return tree.isEmpty() ? empty(comparator) : new TreeSet<>(tree);
    }

    /**
     * The union of nested iterables, in natural order: {@link #flatten(Comparator, Iterable)} with the natural
     * comparator, as {@link #ofAll(Iterable)} is {@link #ofAll(Comparator, Iterable)}.
     * <p>
     * Complexity: O(n log n) comparisons for n inner elements in total.
     *
     * @param nested Iterables of elements
     * @param <T>    Component type of the inner iterables
     * @return the distinct inner elements, in natural order
     * @throws NullPointerException if {@code nested}, an inner iterable or an element is null
     */
    public static <T extends Comparable<? super T>> TreeSet<T> flatten(Iterable<? extends Iterable<? extends T>> nested) {
        return flatten(Comparators.naturalComparator(), nested);
    }

    /**
     * Creates a TreeSet from boolean values.
     *
     * @param elements boolean values
     * @return A new TreeSet of Boolean values
     * @throws NullPointerException if elements is null
     */
    public static TreeSet<Boolean> ofAll(boolean ... elements) {
        Objects.requireNonNull(elements, "elements is null");
        return TreeSet.ofAll(Iterator.ofAll(elements));
    }

    /**
     * Creates a TreeSet from byte values.
     *
     * @param elements byte values
     * @return A new TreeSet of Byte values
     * @throws NullPointerException if elements is null
     */
    public static TreeSet<Byte> ofAll(byte ... elements) {
        Objects.requireNonNull(elements, "elements is null");
        return TreeSet.ofAll(Iterator.ofAll(elements));
    }

    /**
     * Creates a TreeSet from char values.
     *
     * @param elements char values
     * @return A new TreeSet of Character values
     * @throws NullPointerException if elements is null
     */
    public static TreeSet<Character> ofAll(char ... elements) {
        Objects.requireNonNull(elements, "elements is null");
        return TreeSet.ofAll(Iterator.ofAll(elements));
    }

    /**
     * Creates a TreeSet from double values.
     *
     * @param elements double values
     * @return A new TreeSet of Double values
     * @throws NullPointerException if elements is null
     */
    public static TreeSet<Double> ofAll(double ... elements) {
        Objects.requireNonNull(elements, "elements is null");
        return TreeSet.ofAll(Iterator.ofAll(elements));
    }

    /**
     * Creates a TreeSet from float values.
     *
     * @param elements float values
     * @return A new TreeSet of Float values
     * @throws NullPointerException if elements is null
     */
    public static TreeSet<Float> ofAll(float ... elements) {
        Objects.requireNonNull(elements, "elements is null");
        return TreeSet.ofAll(Iterator.ofAll(elements));
    }

    /**
     * Creates a TreeSet from int values.
     *
     * @param elements int values
     * @return A new TreeSet of Integer values
     * @throws NullPointerException if elements is null
     */
    public static TreeSet<Integer> ofAll(int ... elements) {
        Objects.requireNonNull(elements, "elements is null");
        return TreeSet.ofAll(Iterator.ofAll(elements));
    }

    /**
     * Creates a TreeSet from long values.
     *
     * @param elements long values
     * @return A new TreeSet of Long values
     * @throws NullPointerException if elements is null
     */
    public static TreeSet<Long> ofAll(long ... elements) {
        Objects.requireNonNull(elements, "elements is null");
        return TreeSet.ofAll(Iterator.ofAll(elements));
    }

    /**
     * Creates a TreeSet from short values.
     *
     * @param elements short values
     * @return A new TreeSet of Short values
     * @throws NullPointerException if elements is null
     */
    public static TreeSet<Short> ofAll(short ... elements) {
        Objects.requireNonNull(elements, "elements is null");
        return TreeSet.ofAll(Iterator.ofAll(elements));
    }

    /**
     * Creates a TreeSet of int numbers starting from {@code from}, extending to {@code toExclusive - 1}.
     * <p>
     * Examples:
     * <pre>
     * {@code
     * TreeSet.range(0, 0)  // = TreeSet()
     * TreeSet.range(2, 0)  // = TreeSet()
     * TreeSet.range(-2, 2) // = TreeSet(-2, -1, 0, 1)
     * }
     * </pre>
     *
     * @param from        the first number
     * @param toExclusive the last number + 1
     * @return a range of int values as specified or the empty range if {@code from >= toExclusive}
     */
    public static TreeSet<Integer> range(int from, int toExclusive) {
        return TreeSet.ofAll(Iterator.range(from, toExclusive));
    }

    /**
     * Creates a TreeSet of char numbers starting from {@code from}, extending to {@code toExclusive - 1}.
     * <p>
     * Examples:
     * <pre>
     * {@code
     * TreeSet.range('a', 'a')  // = TreeSet()
     * TreeSet.range('b', 'a')  // = TreeSet()
     * TreeSet.range('a', 'c')  // = TreeSet('a', 'b')
     * }
     * </pre>
     *
     * @param from        the first char
     * @param toExclusive the last char + 1
     * @return a range of char values as specified or the empty range if {@code from >= toExclusive}
     */
    public static TreeSet<Character> range(char from, char toExclusive) {
        return TreeSet.ofAll(Iterator.range(from, toExclusive));
    }

    /**
     * Creates a TreeSet of int numbers starting from {@code from}, extending to {@code toExclusive - 1},
     * with {@code step}.
     * <p>
     * Examples:
     * <pre>
     * {@code
     * TreeSet.rangeBy(1, 3, 1)  // = TreeSet(1, 2)
     * TreeSet.rangeBy(1, 4, 2)  // = TreeSet(1, 3)
     * TreeSet.rangeBy(4, 1, -2) // = TreeSet(2, 4)
     * TreeSet.rangeBy(4, 1, 2)  // = TreeSet()
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
    public static TreeSet<Integer> rangeBy(int from, int toExclusive, int step) {
        return TreeSet.ofAll(Iterator.rangeBy(from, toExclusive, step));
    }

    /**
     * Creates a TreeSet of char numbers starting from {@code from}, extending to {@code toExclusive - 1},
     * with {@code step}.
     * <p>
     * Examples:
     * <pre>
     * {@code
     * TreeSet.rangeBy('a', 'c', 1)  // = TreeSet('a', 'b')
     * TreeSet.rangeBy('a', 'd', 2)  // = TreeSet('a', 'c')
     * TreeSet.rangeBy('d', 'a', -2) // = TreeSet('b', 'd')
     * TreeSet.rangeBy('d', 'a', 2)  // = TreeSet()
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
    public static TreeSet<Character> rangeBy(char from, char toExclusive, int step) {
        return TreeSet.ofAll(Iterator.rangeBy(from, toExclusive, step));
    }

    /**
     * Creates a TreeSet of double numbers starting from {@code from}, extending up to (but not including) {@code toExclusive},
     * with {@code step}.
     * <p>
     * Examples:
     * <pre>
     * {@code
     * TreeSet.rangeBy(1.0, 3.0, 1.0)  // = TreeSet(1.0, 2.0)
     * TreeSet.rangeBy(1.0, 4.0, 2.0)  // = TreeSet(1.0, 3.0)
     * TreeSet.rangeBy(4.0, 1.0, -2.0) // = TreeSet(2.0, 4.0)
     * TreeSet.rangeBy(4.0, 1.0, 2.0)  // = TreeSet()
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
    public static TreeSet<Double> rangeBy(double from, double toExclusive, double step) {
        return TreeSet.ofAll(Iterator.rangeBy(from, toExclusive, step));
    }

    /**
     * Creates a TreeSet of long numbers starting from {@code from}, extending to {@code toExclusive - 1}.
     * <p>
     * Examples:
     * <pre>
     * {@code
     * TreeSet.range(0L, 0L)  // = TreeSet()
     * TreeSet.range(2L, 0L)  // = TreeSet()
     * TreeSet.range(-2L, 2L) // = TreeSet(-2L, -1L, 0L, 1L)
     * }
     * </pre>
     *
     * @param from        the first number
     * @param toExclusive the last number + 1
     * @return a range of long values as specified or the empty range if {@code from >= toExclusive}
     */
    public static TreeSet<Long> range(long from, long toExclusive) {
        return TreeSet.ofAll(Iterator.range(from, toExclusive));
    }

    /**
     * Creates a TreeSet of long numbers starting from {@code from}, extending to {@code toExclusive - 1},
     * with {@code step}.
     * <p>
     * Examples:
     * <pre>
     * {@code
     * TreeSet.rangeBy(1L, 3L, 1L)  // = TreeSet(1L, 2L)
     * TreeSet.rangeBy(1L, 4L, 2L)  // = TreeSet(1L, 3L)
     * TreeSet.rangeBy(4L, 1L, -2L) // = TreeSet(2L, 4L)
     * TreeSet.rangeBy(4L, 1L, 2L)  // = TreeSet()
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
    public static TreeSet<Long> rangeBy(long from, long toExclusive, long step) {
        return TreeSet.ofAll(Iterator.rangeBy(from, toExclusive, step));
    }

    /**
     * Creates a TreeSet of int numbers starting from {@code from}, extending to {@code toInclusive}.
     * <p>
     * Examples:
     * <pre>
     * {@code
     * TreeSet.rangeClosed(0, 0)  // = TreeSet(0)
     * TreeSet.rangeClosed(2, 0)  // = TreeSet()
     * TreeSet.rangeClosed(-2, 2) // = TreeSet(-2, -1, 0, 1, 2)
     * }
     * </pre>
     *
     * @param from        the first number
     * @param toInclusive the last number
     * @return a range of int values as specified or the empty range if {@code from > toInclusive}
     */
    public static TreeSet<Integer> rangeClosed(int from, int toInclusive) {
        return TreeSet.ofAll(Iterator.rangeClosed(from, toInclusive));
    }

    /**
     * Creates a TreeSet of char numbers starting from {@code from}, extending to {@code toInclusive}.
     * <p>
     * Examples:
     * <pre>
     * {@code
     * TreeSet.rangeClosed('a', 'a')  // = TreeSet('a')
     * TreeSet.rangeClosed('b', 'a')  // = TreeSet()
     * TreeSet.rangeClosed('a', 'c')  // = TreeSet('a', 'b', 'c')
     * }
     * </pre>
     *
     * @param from        the first char
     * @param toInclusive the last char
     * @return a range of char values as specified or the empty range if {@code from > toInclusive}
     */
    public static TreeSet<Character> rangeClosed(char from, char toInclusive) {
        return TreeSet.ofAll(Iterator.rangeClosed(from, toInclusive));
    }

    /**
     * Creates a TreeSet of int numbers starting from {@code from}, extending to {@code toInclusive},
     * with {@code step}.
     * <p>
     * Examples:
     * <pre>
     * {@code
     * TreeSet.rangeClosedBy(1, 3, 1)  // = TreeSet(1, 2, 3)
     * TreeSet.rangeClosedBy(1, 4, 2)  // = TreeSet(1, 3)
     * TreeSet.rangeClosedBy(4, 1, -2) // = TreeSet(2, 4)
     * TreeSet.rangeClosedBy(4, 1, 2)  // = TreeSet()
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
    public static TreeSet<Integer> rangeClosedBy(int from, int toInclusive, int step) {
        return TreeSet.ofAll(Iterator.rangeClosedBy(from, toInclusive, step));
    }

    /**
     * Creates a TreeSet of char numbers starting from {@code from}, extending to {@code toInclusive},
     * with {@code step}.
     * <p>
     * Examples:
     * <pre>
     * {@code
     * TreeSet.rangeClosedBy('a', 'c', 1)  // = TreeSet('a', 'b', 'c')
     * TreeSet.rangeClosedBy('a', 'd', 2)  // = TreeSet('a', 'c')
     * TreeSet.rangeClosedBy('d', 'a', -2) // = TreeSet('b', 'd')
     * TreeSet.rangeClosedBy('d', 'a', 2)  // = TreeSet()
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
    public static TreeSet<Character> rangeClosedBy(char from, char toInclusive, int step) {
        return TreeSet.ofAll(Iterator.rangeClosedBy(from, toInclusive, step));
    }

    /**
     * Creates a TreeSet of double numbers starting from {@code from}, extending to {@code toInclusive},
     * with {@code step}.
     * <p>
     * Examples:
     * <pre>
     * {@code
     * TreeSet.rangeClosedBy(1.0, 3.0, 1.0)  // = TreeSet(1.0, 2.0, 3.0)
     * TreeSet.rangeClosedBy(1.0, 4.0, 2.0)  // = TreeSet(1.0, 3.0)
     * TreeSet.rangeClosedBy(4.0, 1.0, -2.0) // = TreeSet(2.0, 4.0)
     * TreeSet.rangeClosedBy(4.0, 1.0, 2.0)  // = TreeSet()
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
    public static TreeSet<Double> rangeClosedBy(double from, double toInclusive, double step) {
        return TreeSet.ofAll(Iterator.rangeClosedBy(from, toInclusive, step));
    }

    /**
     * Creates a TreeSet of long numbers starting from {@code from}, extending to {@code toInclusive}.
     * <p>
     * Examples:
     * <pre>
     * {@code
     * TreeSet.rangeClosed(0L, 0L)  // = TreeSet(0L)
     * TreeSet.rangeClosed(2L, 0L)  // = TreeSet()
     * TreeSet.rangeClosed(-2L, 2L) // = TreeSet(-2L, -1L, 0L, 1L, 2L)
     * }
     * </pre>
     *
     * @param from        the first number
     * @param toInclusive the last number
     * @return a range of long values as specified or the empty range if {@code from > toInclusive}
     */
    public static TreeSet<Long> rangeClosed(long from, long toInclusive) {
        return TreeSet.ofAll(Iterator.rangeClosed(from, toInclusive));
    }

    /**
     * Creates a TreeSet of long numbers starting from {@code from}, extending to {@code toInclusive},
     * with {@code step}.
     * <p>
     * Examples:
     * <pre>
     * {@code
     * TreeSet.rangeClosedBy(1L, 3L, 1L)  // = TreeSet(1L, 2L, 3L)
     * TreeSet.rangeClosedBy(1L, 4L, 2L)  // = TreeSet(1L, 3L)
     * TreeSet.rangeClosedBy(4L, 1L, -2L) // = TreeSet(2L, 4L)
     * TreeSet.rangeClosedBy(4L, 1L, 2L)  // = TreeSet()
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
    public static TreeSet<Long> rangeClosedBy(long from, long toInclusive, long step) {
        return TreeSet.ofAll(Iterator.rangeClosedBy(from, toInclusive, step));
    }

    @Override
    public TreeSet<T> add(T element) {
        return contains(element) ? this : new TreeSet<>(tree.insert(element));
    }

    @Override
    public TreeSet<T> addAll(Iterable<? extends T> elements) {
        Objects.requireNonNull(elements, "elements is null");
        RedBlackTree<T> that = tree;
        for (T element : elements) {
            if (!that.contains(element)) {
                that = that.insert(element);
            }
        }
        if (tree == that) {
            return this;
        } else {
            return new TreeSet<>(that);
        }
    }

    /**
     * An unmodifiable {@link java.util.NavigableSet} view of this TreeSet, in the comparator's order: nothing is
     * copied, reads go through to this set, which never changes, and every mutator of the view (including those of
     * its iterators and sub-views) throws {@link UnsupportedOperationException}, {@code pollFirst} and
     * {@code pollLast} included. {@code subSet}, {@code headSet}, {@code tailSet} and {@code descendingSet} are views
     * too, with the bounds rules of {@link java.util.TreeSet}; {@code comparator()} is {@code null} when this set uses
     * the natural order. The view equals any {@code java.util.Set} with the same elements. A mutable copy is
     * {@code new java.util.TreeSet<>(set.asJava())}; {@code TreeSet.ofAll} given the view and this set's comparator
     * returns this set without copying.
     * <p>
     * Complexity: O(1); {@code contains}, {@code size}, {@code first}, {@code last}, {@code ceiling}, {@code floor},
     * {@code higher} and {@code lower} on the view and on its sub-views are O(log n), an iterator is O(log n) to create
     * and amortized O(1) per step.
     *
     * @return an unmodifiable {@code java.util.NavigableSet} view
     */
    @Override
    public java.util.NavigableSet<T> asJava() {
        return TreeViews.asJava(this, tree);
    }

    @Override
    public Comparator<T> comparator() {
        return tree.comparator();
    }

    @SuppressWarnings("unchecked")
    @Override
    public TreeSet<T> diff(Set<? extends T> elements) {
        Objects.requireNonNull(elements, "elements is null");
        if (isEmpty()) {
            return this;
        } else if (hasSameComparator(elements)) {
            final TreeSet<T> that = (TreeSet<T>) elements;
            return that.isEmpty() ? this : new TreeSet<>(tree.difference(that.tree));
        } else {
            return removeAll(elements);
        }
    }

    // the RedBlackTree set operations require both trees to be ordered by the same comparator
    private boolean hasSameComparator(Set<?> that) {
        return that instanceof TreeSet && comparator().equals(((TreeSet<?>) that).comparator());
    }

    /**
     * Returns {@code true} if this TreeSet contains an element that compares equal to {@code element}
     * according to this set's {@link #comparator()} (not according to {@code equals}), {@code false} otherwise.
     * <p>
     * Whether {@code null} is accepted depends on the comparator: the natural comparator throws
     * {@code NullPointerException} for {@code null}.
     * <p>
     * Complexity: O(log n) comparisons.
     *
     * @param element the element to check
     * @return true, if element is contained, false otherwise.
     */
    @Override
    public boolean contains(T element) {
        return tree.contains(element);
    }

    @Override
    public TreeSet<T> filter(Predicate<? super T> predicate) {
        Objects.requireNonNull(predicate, "predicate is null");
        final TreeSet<T> treeSet = TreeSet.ofAll(tree.comparator(), Iterator.ofAll(this).filter(predicate));
        return (treeSet.size() == size()) ? this : treeSet;
    }

    @Override
    public TreeSet<T> reject(Predicate<? super T> predicate) {
        Objects.requireNonNull(predicate, "predicate is null");
        return filter(predicate.negate());
    }

    @Override
    public <U extends @Nullable Object> TreeSet<U> flatMap(Comparator<? super U> comparator,
                                  Function<? super T, ? extends Iterable<? extends U>> mapper) {
        Objects.requireNonNull(mapper, "mapper is null");
        return TreeSet.ofAll(comparator, Iterator.ofAll(this).flatMap(mapper));
    }

    /**
     * {@inheritDoc}
     * <p>
     * The resulting TreeSet is ordered by the natural comparator of {@code U}.
     *
     * @throws ClassCastException if the flat-mapped elements are not mutually {@link Comparable};
     *                            use {@link #flatMap(Comparator, Function)} to avoid this
     */
    @Override
    public <U extends @Nullable Object> TreeSet<U> flatMap(Function<? super T, ? extends Iterable<? extends U>> mapper) {
        return flatMap(Comparators.naturalComparator(), mapper);
    }

    @Override
    public <C extends @Nullable Object> Map<C, TreeSet<T>> groupBy(Function<? super T, ? extends C> classifier) {
        return Collections.groupBy(this, classifier, elements -> ofAll(comparator(), elements));
    }

    @SuppressWarnings("unchecked")
    @Override
    public TreeSet<T> intersect(Set<? extends T> elements) {
        Objects.requireNonNull(elements, "elements is null");
        if (isEmpty()) {
            return this;
        } else if (hasSameComparator(elements)) {
            final TreeSet<T> that = (TreeSet<T>) elements;
            return new TreeSet<>(tree.intersection(that.tree));
        } else {
            return retainAll(elements);
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
     * Complexity: O(log n) to create (the path to the least element); a whole walk is O(n).
     */
    @Override
    public java.util.Iterator<T> iterator() {
        return tree.iterator();
    }

    @Override
    public <U extends @Nullable Object> TreeSet<U> map(Comparator<? super U> comparator, Function<? super T, ? extends U> mapper) {
        Objects.requireNonNull(mapper, "mapper is null");
        return TreeSet.ofAll(comparator, Iterator.ofAll(this).map(mapper));
    }

    /**
     * {@inheritDoc}
     * <p>
     * The resulting TreeSet is ordered by the natural comparator of {@code U}.
     *
     * @throws ClassCastException if the mapped elements are not mutually {@link Comparable};
     *                            use {@link #map(Comparator, Function)} to avoid this
     */
    @Override
    public <U extends @Nullable Object> TreeSet<U> map(Function<? super T, ? extends U> mapper) {
        return map(Comparators.naturalComparator(), mapper);
    }

    /**
     * Matches and transforms the elements in one pass into a {@code TreeSet} ordered by {@code comparator}; see
     * {@link #collect(Function)}.
     *
     * @param comparator the order of the collected elements
     * @param mapper     a function from an element to {@code Some} of its replacement or {@code None}; it must
     *                   not return {@code null}
     * @param <U>        the type of the collected elements
     * @return a {@code TreeSet} of the collected elements
     * @throws NullPointerException if an argument is null, or if {@code mapper} returns {@code null} for an element
     */
    public <U extends @Nullable Object> TreeSet<U> collect(Comparator<? super U> comparator, Function<? super T, ? extends Option<? extends U>> mapper) {
        Objects.requireNonNull(comparator, "comparator is null");
        Objects.requireNonNull(mapper, "mapper is null");
        // the null check runs here so that the message names this type, not the Iterator that does the walking
        return TreeSet.ofAll(comparator, Iterator.ofAll(this).collect(t -> Objects.requireNonNull(mapper.apply(t), "TreeSet.collect: mapper returned null")));
    }

    /**
     * {@inheritDoc}
     * <p>
     * The result is ordered by the natural order of {@code U}; use {@link #collect(Comparator, Function)} to
     * choose the order.
     *
     * @throws ClassCastException if the collected elements are not mutually {@link Comparable}
     */
    @Override
    public <U extends @Nullable Object> TreeSet<U> collect(Function<? super T, ? extends Option<? extends U>> mapper) {
        return collect(Comparators.naturalComparator(), mapper);
    }

    /**
     * {@inheritDoc}
     * <p>
     * The resulting TreeSet is ordered by the natural comparator of {@code U}.
     *
     * @throws ClassCastException if this set has more than one element and {@code value} is not {@link Comparable}
     */
    @Override
    public <U extends @Nullable Object> TreeSet<U> as(U value) {
        return map(ignored -> value);
    }

    /**
     * Returns this {@code TreeSet} if it is nonempty,
     * otherwise {@code TreeSet} created from iterable, using existing comparator.
     *
     * @param other An alternative {@code Traversable}
     * @return this {@code TreeSet} if it is nonempty,
     * otherwise {@code TreeSet} created from iterable, using existing comparator.
     */
    @Override
    public TreeSet<T> orElse(Iterable<? extends T> other) {
        return isEmpty() ? ofAll(tree.comparator(), other) : this;
    }

    /**
     * Returns this {@code TreeSet} if it is nonempty,
     * otherwise {@code TreeSet} created from result of evaluating supplier, using existing comparator.
     *
     * @param supplier A supplier of an alternative {@code Iterable}, evaluated only if this TreeSet is empty
     * @return this {@code TreeSet} if it is nonempty,
     * otherwise {@code TreeSet} created from result of evaluating supplier, using existing comparator.
     */
    @Override
    public TreeSet<T> orElse(Supplier<? extends Iterable<? extends T>> supplier) {
        return isEmpty() ? ofAll(tree.comparator(), supplier.get()) : this;
    }

    @Override
    public Tuple2<TreeSet<T>, TreeSet<T>> partition(Predicate<? super T> predicate) {
        return Collections.partition(this, values -> TreeSet.ofAll(tree.comparator(), values), predicate);
    }

    @Override
    public TreeSet<T> tap(Consumer<? super T> action) {
        Objects.requireNonNull(action, "action is null");
        forEach(action);
        return this;
    }

    @Override
    public TreeSet<T> remove(T element) {
        return new TreeSet<>(tree.delete(element));
    }

    @Override
    public TreeSet<T> removeAll(Iterable<? extends T> elements) {
        return Collections.removeAll(this, elements, kept -> filter(kept));
    }

    @Override
    public TreeSet<T> replace(T currentElement, T newElement) {
        if (tree.contains(currentElement)) {
            return new TreeSet<>(tree.delete(currentElement).insert(newElement));
        } else {
            return this;
        }
    }

    @Override
    public TreeSet<T> replaceAll(T currentElement, T newElement) {
        // a set has only one occurrence
        return replace(currentElement, newElement);
    }

    @Override
    public TreeSet<T> retainAll(Iterable<? extends T> elements) {
        return Collections.retainAll(this, elements, kept -> filter(kept));
    }

    @SuppressWarnings("unchecked")
    @Override
    public TreeSet<T> union(Set<? extends T> elements) {
        Objects.requireNonNull(elements, "elements is null");
        if (hasSameComparator(elements)) {
            final TreeSet<T> that = (TreeSet<T>) elements;
            return that.isEmpty() ? this : new TreeSet<>(tree.union(that.tree));
        } else {
            return addAll(elements);
        }
    }

    // -- Positional operations, in the comparator's order

    @Override
    public T head() {
        if (tree.isEmpty()) {
            throw new java.util.NoSuchElementException("head of empty TreeSet");
        }
        return RedBlackTreeModule.Node.minimum((RedBlackTreeModule.Node<T>) tree);
    }

    @Override
    public Option<T> headOption() {
        return tree.min();
    }

    @Override
    public T last() {
        if (tree.isEmpty()) {
            throw new java.util.NoSuchElementException("last of empty TreeSet");
        }
        return RedBlackTreeModule.Node.maximum((RedBlackTreeModule.Node<T>) tree);
    }

    @Override
    public Option<T> lastOption() {
        return tree.max();
    }

    @Override
    public TreeSet<T> init() {
        if (tree.isEmpty()) {
            throw new UnsupportedOperationException("init of empty TreeSet");
        }
        return new TreeSet<>(RedBlackTreeModule.Node.take(tree, tree.size() - 1));
    }

    @Override
    public Option<TreeSet<T>> initOption() {
        return tree.isEmpty() ? Option.none() : Option.some(init());
    }

    @Override
    public TreeSet<T> tail() {
        if (tree.isEmpty()) {
            throw new UnsupportedOperationException("tail of empty TreeSet");
        }
        return new TreeSet<>(RedBlackTreeModule.Node.drop(tree, 1));
    }

    @Override
    public Option<TreeSet<T>> tailOption() {
        return tree.isEmpty() ? Option.none() : Option.some(tail());
    }

    @Override
    public TreeSet<T> take(int n) {
        return slice(0, n);
    }

    @Override
    public TreeSet<T> takeRight(int n) {
        // n <= 0 keeps nothing; the subtraction cannot overflow because size() >= 0
        return n <= 0 ? slice(0, 0) : slice(tree.size() - n, tree.size());
    }

    @Override
    public TreeSet<T> takeWhile(Predicate<? super T> predicate) {
        Objects.requireNonNull(predicate, "predicate is null");
        return slice(0, RedBlackTreeModule.Node.prefixLength(tree, predicate, true));
    }

    @Override
    public TreeSet<T> takeUntil(Predicate<? super T> predicate) {
        Objects.requireNonNull(predicate, "predicate is null");
        return slice(0, RedBlackTreeModule.Node.prefixLength(tree, predicate, false));
    }

    @Override
    public TreeSet<T> drop(int n) {
        return slice(n, tree.size());
    }

    @Override
    public TreeSet<T> dropRight(int n) {
        return n <= 0 ? this : slice(0, tree.size() - n);
    }

    @Override
    public TreeSet<T> dropWhile(Predicate<? super T> predicate) {
        Objects.requireNonNull(predicate, "predicate is null");
        return slice(RedBlackTreeModule.Node.prefixLength(tree, predicate, true), tree.size());
    }

    @Override
    public TreeSet<T> dropUntil(Predicate<? super T> predicate) {
        Objects.requireNonNull(predicate, "predicate is null");
        return slice(RedBlackTreeModule.Node.prefixLength(tree, predicate, false), tree.size());
    }

    @Override
    public Vector<Tuple2<T, Integer>> zipWithIndex() {
        return RedBlackTreeModule.Node.zipWithIndex(tree);
    }

    @Override
    public Vector<TreeSet<T>> grouped(int size) {
        return sliding(size, size);
    }

    @Override
    public Vector<TreeSet<T>> sliding(int size) {
        return sliding(size, 1);
    }

    @Override
    public Vector<TreeSet<T>> sliding(int size, int step) {
        return RedBlackTreeModule.Node.sliding(tree, size, step, TreeSet::new);
    }

    @Override
    public Vector<TreeSet<T>> slideBy(Function<? super T, ?> classifier) {
        return RedBlackTreeModule.Node.slideBy(tree, classifier, TreeSet::new);
    }

    // the elements of rank from (inclusive) to until (exclusive), clamped; this set when nothing is cut off
    private TreeSet<T> slice(int from, int until) {
        final RedBlackTree<T> sliced = RedBlackTreeModule.Node.slice(tree, from, until);
        return sliced == tree ? this : new TreeSet<>(sliced);
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
        return mkString("TreeSet(", ", ", ")");
    }
}
