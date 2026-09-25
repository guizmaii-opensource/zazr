package com.guizmaii.zazr.collection;

import com.guizmaii.zazr.*;
import com.guizmaii.zazr.collection.internal.Collections;
import com.guizmaii.zazr.collection.internal.Iterator;
import com.guizmaii.zazr.collection.internal.JavaConverters;
import com.guizmaii.zazr.collection.internal.TraversableModule;
import com.guizmaii.zazr.control.Either;
import com.guizmaii.zazr.control.Option;
import java.util.*;
import java.util.function.*;
import java.util.stream.Collector;
import java.util.stream.StreamSupport;
import org.jspecify.annotations.Nullable;

import static com.guizmaii.zazr.collection.internal.JavaConverters.ChangePolicy.IMMUTABLE;
import static com.guizmaii.zazr.collection.internal.JavaConverters.ChangePolicy.MUTABLE;
import static com.guizmaii.zazr.collection.internal.JavaConverters.ListView;

/**
 * An immutable {@code Queue} stores elements allowing a first-in-first-out (FIFO) retrieval.
 * <p>
 * Queue API:
 *
 * <ul>
 * <li>{@link #dequeue()}</li>
 * <li>{@link #dequeueOption()}</li>
 * <li>{@link #enqueue(Object)}</li>
 * <li>{@link #enqueue(Object[])}</li>
 * <li>{@link #enqueueAll(Iterable)}</li>
 * <li>{@link #peek()}</li>
 * <li>{@link #peekOption()}</li>
 * </ul>
 *
 * A Queue internally consists of a front List containing the front elements of the Queue in the correct order and a
 * rear List containing the rear elements of the Queue in reverse order.
 * <p>
 * When the front list is empty, front and rear are swapped and rear is reversed. This implies the following queue
 * invariant: {@code front.isEmpty() => rear.isEmpty()}.
 * <p>
 * See Okasaki, Chris: <em>Purely Functional Data Structures</em> (p. 42 ff.). Cambridge, 2003.
 *
 * @param <T> Component type of the Queue
 * @author Daniel Dietrich
 */
public final class Queue<T extends @Nullable Object> implements Traversable<T> {

    private static final Queue<?> EMPTY = new Queue<>(com.guizmaii.zazr.collection.List.empty(), com.guizmaii.zazr.collection.List.empty());

    private final com.guizmaii.zazr.collection.List<T> front;
    private final com.guizmaii.zazr.collection.List<T> rear;

    /**
     * Creates a Queue consisting of a front List and a rear List.
     * <p>
     * For a {@code Queue(front, rear)} the following invariant holds: {@code Queue is empty <=> front is empty}.
     * In other words: If the Queue is not empty, the front List contains at least one element.
     *
     * @param front A List of front elements, in correct order.
     * @param rear  A List of rear elements, in reverse order.
     */
    private Queue(com.guizmaii.zazr.collection.List<T> front, com.guizmaii.zazr.collection.List<T> rear) {
        final boolean frontIsEmpty = front.isEmpty();
        this.front = frontIsEmpty ? rear.reverse() : front;
        this.rear = frontIsEmpty ? front : rear;
    }

    /**
     * Returns a {@link java.util.stream.Collector} which may be used in conjunction with
     * {@link java.util.stream.Stream#collect(java.util.stream.Collector)} to obtain a {@link Queue}
     * .
     *
     * @param <T> Component type of the Queue.
     * @return A com.guizmaii.zazr.collection.Queue Collector.
     */
    public static <T extends @Nullable Object> Collector<T, ArrayList<T>, Queue<T>> collector() {
        final Supplier<ArrayList<T>> supplier = ArrayList::new;
        final BiConsumer<ArrayList<T>, T> accumulator = ArrayList::add;
        final BinaryOperator<ArrayList<T>> combiner = (left, right) -> {
            left.addAll(right);
            return left;
        };
        final Function<ArrayList<T>, Queue<T>> finisher = Queue::ofAll;
        return Collector.of(supplier, accumulator, combiner, finisher);
    }

    /**
     * Returns the empty Queue.
     *
     * @param <T> Component type
     * @return The empty Queue.
     */
    @SuppressWarnings("unchecked")
    public static <T extends @Nullable Object> Queue<T> empty() {
        return (Queue<T>) EMPTY;
    }

    /**
     * Narrows a {@code Queue<? extends T>} to {@code Queue<T>} via a
     * type-safe cast. Safe here because the queue is immutable and no elements
     * can be added that would violate the type (covariance)
     *
     * @param queue the queue to narrow
     * @param <T>   the target element type
     * @return the same queue viewed as {@code Queue<T>}
     */
    @SuppressWarnings("unchecked")
    public static <T extends @Nullable Object> Queue<T> narrow(Queue<? extends T> queue) {
        return (Queue<T>) queue;
    }

    /**
     * Returns a singleton {@code Queue}, i.e. a {@code Queue} of one element.
     *
     * @param element An element.
     * @param <T>     The component type
     * @return A new Queue instance containing the given element
     */
    public static <T extends @Nullable Object> Queue<T> of(T element) {
        return ofAll(com.guizmaii.zazr.collection.List.of(element));
    }

    /**
     * Creates a Queue of the given elements.
     *
     * @param <T>      Component type of the Queue.
     * @param elements Zero or more elements.
     * @return A queue containing the given elements in the same order.
     * @throws NullPointerException if {@code elements} is null
     */
    @SuppressWarnings("varargs")
    @SafeVarargs
    public static <T extends @Nullable Object> Queue<T> of(T ... elements) {
        Objects.requireNonNull(elements, "elements is null");
        return ofAll(com.guizmaii.zazr.collection.List.of(elements));
    }

    /**
     * Creates a Queue of the given elements.
     *
     * @param <T>      Component type of the Queue.
     * @param elements An Iterable of elements.
     * @return A queue containing the given elements in the same order.
     * @throws NullPointerException if {@code elements} is null
     */
    @SuppressWarnings("unchecked")
    public static <T extends @Nullable Object> Queue<T> ofAll(Iterable<? extends T> elements) {
        Objects.requireNonNull(elements, "elements is null");
        if (elements instanceof Queue) {
            return (Queue<T>) elements;
        } else if (elements instanceof ListView
                && ((ListView<T, ?>) elements).getDelegate() instanceof Queue) {
            return (Queue<T>) ((ListView<T, ?>) elements).getDelegate();
        } else {
            // one read of the argument, which may be a one-shot Iterable: the emptiness is answered by the copy
            final com.guizmaii.zazr.collection.List<T> front = com.guizmaii.zazr.collection.List.ofAll(elements);
            return front.isEmpty() ? empty() : new Queue<>(front, com.guizmaii.zazr.collection.List.empty());
        }
    }

    /**
     * Creates a Queue that contains the elements of the given {@link java.util.stream.Stream}.
     *
     * @param javaStream A {@link java.util.stream.Stream}
     * @param <T>        Component type of the Stream.
     * @return A Queue containing the given elements in the same order.
     */
    public static <T extends @Nullable Object> Queue<T> ofAll(java.util.stream.Stream<? extends T> javaStream) {
        Objects.requireNonNull(javaStream, "javaStream is null");
        return new Queue<>(com.guizmaii.zazr.collection.List.ofAll(javaStream), com.guizmaii.zazr.collection.List.empty());
    }

    /**
     * Concatenates nested iterables into one Queue. Static, like every {@code flatten} in zazr, because Java cannot
     * demand of an instance method that the receiver's element type be a collection. The outer iterable and each inner
     * one are iterated once, so one-shot iterables are accepted.
     * <p>
     * Complexity: O(n) for n inner elements in total: they are collected into the front list of the result.
     *
     * @param nested Iterables of elements
     * @param <T>    Component type of the inner iterables
     * @return the inner elements, in order
     * @throws NullPointerException if {@code nested}, an inner iterable or an element is null
     */
    public static <T extends @Nullable Object> Queue<T> flatten(Iterable<? extends Iterable<? extends T>> nested) {
        return ofAll(com.guizmaii.zazr.collection.List.flatten(nested));
    }

    /**
     * Creates a Queue from boolean values.
     *
     * @param elements boolean values
     * @return A new Queue of Boolean values
     * @throws NullPointerException if elements is null
     */
    public static Queue<Boolean> ofAll(boolean ... elements) {
        Objects.requireNonNull(elements, "elements is null");
        return ofAll(com.guizmaii.zazr.collection.List.ofAll(elements));
    }

    /**
     * Creates a Queue from byte values.
     *
     * @param elements byte values
     * @return A new Queue of Byte values
     * @throws NullPointerException if elements is null
     */
    public static Queue<Byte> ofAll(byte ... elements) {
        Objects.requireNonNull(elements, "elements is null");
        return ofAll(com.guizmaii.zazr.collection.List.ofAll(elements));
    }

    /**
     * Creates a Queue from char values.
     *
     * @param elements char values
     * @return A new Queue of Character values
     * @throws NullPointerException if elements is null
     */
    public static Queue<Character> ofAll(char ... elements) {
        Objects.requireNonNull(elements, "elements is null");
        return ofAll(com.guizmaii.zazr.collection.List.ofAll(elements));
    }

    /**
     * Creates a Queue from double values.
     *
     * @param elements double values
     * @return A new Queue of Double values
     * @throws NullPointerException if elements is null
     */
    public static Queue<Double> ofAll(double ... elements) {
        Objects.requireNonNull(elements, "elements is null");
        return ofAll(com.guizmaii.zazr.collection.List.ofAll(elements));
    }

    /**
     * Creates a Queue from float values.
     *
     * @param elements float values
     * @return A new Queue of Float values
     * @throws NullPointerException if elements is null
     */
    public static Queue<Float> ofAll(float ... elements) {
        Objects.requireNonNull(elements, "elements is null");
        return ofAll(com.guizmaii.zazr.collection.List.ofAll(elements));
    }

    /**
     * Creates a Queue from int values.
     *
     * @param elements int values
     * @return A new Queue of Integer values
     * @throws NullPointerException if elements is null
     */
    public static Queue<Integer> ofAll(int ... elements) {
        Objects.requireNonNull(elements, "elements is null");
        return ofAll(com.guizmaii.zazr.collection.List.ofAll(elements));
    }

    /**
     * Creates a Queue from long values.
     *
     * @param elements  long values
     * @return A new Queue of Long values
     * @throws NullPointerException if elements is null
     */
    public static Queue<Long> ofAll(long ... elements) {
        Objects.requireNonNull(elements, "elements is null");
        return ofAll(com.guizmaii.zazr.collection.List.ofAll(elements));
    }

    /**
     * Creates a Queue from short values.
     *
     * @param elements short values
     * @return A new Queue of Short values
     * @throws NullPointerException if elements is null
     */
    public static Queue<Short> ofAll(short ... elements) {
        Objects.requireNonNull(elements, "elements is null");
        return ofAll(com.guizmaii.zazr.collection.List.ofAll(elements));
    }

    /**
     * Returns a Queue containing {@code n} values of a given Function {@code f}
     * over a range of integer values from 0 to {@code n - 1}.
     *
     * @param <T> Component type of the Queue
     * @param n   The number of elements in the Queue
     * @param f   The Function computing element values
     * @return A Queue consisting of elements {@code f(0),f(1), ..., f(n - 1)}
     * @throws NullPointerException if {@code f} is null
     */
    public static <T extends @Nullable Object> Queue<T> tabulate(int n, Function<? super Integer, ? extends T> f) {
        Objects.requireNonNull(f, "f is null");
        return com.guizmaii.zazr.collection.internal.Collections.tabulate(n, f, empty(), Queue::of);
    }

    /**
     * Returns a Queue containing {@code n} values supplied by a given Supplier {@code s}.
     *
     * @param <T> Component type of the Queue
     * @param n   The number of elements in the Queue
     * @param s   The Supplier computing element values
     * @return An Queue of size {@code n}, where each element contains the result supplied by {@code s}.
     * @throws NullPointerException if {@code s} is null
     */
    public static <T extends @Nullable Object> Queue<T> fill(int n, Supplier<? extends T> s) {
        Objects.requireNonNull(s, "s is null");
        return com.guizmaii.zazr.collection.internal.Collections.fill(n, s, empty(), Queue::of);
    }

    /**
     * Returns a Queue containing {@code n} times the given {@code element}
     *
     * @param <T>     Component type of the Queue
     * @param n       The number of elements in the Queue
     * @param element The element
     * @return An Queue of size {@code n}, where each element is the given {@code element}.
     */
    public static <T extends @Nullable Object> Queue<T> fill(int n, T element) {
        return com.guizmaii.zazr.collection.internal.Collections.fillObject(n, element, empty(), Queue::of);
    }

    /**
     * Creates a Queue of characters starting from {@code from} (inclusive)
     * up to {@code toExclusive} (exclusive).
     *
     * <p>Examples:
     * <pre>
     * Queue.range('a', 'c')  // = Queue('a', 'b')
     * Queue.range('c', 'a')  // = Queue()
     * </pre>
     *
     * @param from        the first character (inclusive)
     * @param toExclusive the end character (exclusive)
     * @return a Queue over the specified character range, or empty if {@code from >= toExclusive}
     */
    public static Queue<Character> range(char from, char toExclusive) {
        return ofAll(Iterator.range(from, toExclusive));
    }

    /**
     * Creates a Queue of characters starting from {@code from} (inclusive)
     * up to {@code toExclusive} (exclusive), advancing by the specified {@code step}.
     *
     * <p>Examples:
     * <pre>
     * Queue.rangeBy('a', 'c', 1)  // = Queue('a', 'b')
     * Queue.rangeBy('a', 'd', 2)  // = Queue('a', 'c')
     * Queue.rangeBy('d', 'a', -2) // = Queue('d', 'b')
     * Queue.rangeBy('d', 'a', 2)  // = Queue()
     * </pre>
     *
     * @param from        the first character (inclusive)
     * @param toExclusive the end character (exclusive)
     * @param step        the increment; must not be zero
     * @return a Queue over the specified character range, or empty if the step
     *         direction does not match the direction from {@code from} to {@code toExclusive}
     * @throws IllegalArgumentException if {@code step} is zero
     */
    public static Queue<Character> rangeBy(char from, char toExclusive, int step) {
        return ofAll(Iterator.rangeBy(from, toExclusive, step));
    }

    /**
     * Creates a Queue of double values starting from {@code from} (inclusive)
     * up to {@code toExclusive} (exclusive), advancing by the specified {@code step}.
     *
     * <p>Examples:
     * <pre>
     * Queue.rangeBy(0.0, 1.0, 0.25)   // = Queue(0.0, 0.25, 0.5, 0.75)
     * Queue.rangeBy(1.0, 0.0, -0.25)  // = Queue(1.0, 0.75, 0.5, 0.25)
     * Queue.rangeBy(0.0, 1.0, -0.25)  // = Queue()
     * </pre>
     *
     * @param from        the first double value (inclusive)
     * @param toExclusive the end value (exclusive)
     * @param step        the increment; must not be zero
     * @return a Queue over the specified double range, or empty if the step
     *         direction does not match the direction from {@code from} to {@code toExclusive}
     * @throws IllegalArgumentException if {@code step} is zero
     */
    public static Queue<Double> rangeBy(double from, double toExclusive, double step) {
        return ofAll(Iterator.rangeBy(from, toExclusive, step));
    }

    /**
     * Creates a Queue of int numbers starting from {@code from}, extending to {@code toExclusive - 1}.
     * <p>
     * Examples:
     * <pre>
     * {@code
     * Queue.range(0, 0)  // = Queue()
     * Queue.range(2, 0)  // = Queue()
     * Queue.range(-2, 2) // = Queue(-2, -1, 0, 1)
     * }
     * </pre>
     *
     * @param from        the first number
     * @param toExclusive the last number + 1
     * @return a range of int values as specified or an empty Queue if {@code from >= toExclusive}
     */
    public static Queue<Integer> range(int from, int toExclusive) {
        return ofAll(Iterator.range(from, toExclusive));
    }

    /**
     * Creates a Queue of int numbers starting from {@code from}, extending to {@code toExclusive - 1},
     * with {@code step}.
     * <p>
     * Examples:
     * <pre>
     * {@code
     * Queue.rangeBy(1, 3, 1)  // = Queue(1, 2)
     * Queue.rangeBy(1, 4, 2)  // = Queue(1, 3)
     * Queue.rangeBy(4, 1, -2) // = Queue(4, 2)
     * Queue.rangeBy(4, 1, 2)  // = Queue()
     * }
     * </pre>
     *
     * @param from        the first number
     * @param toExclusive the last number + 1
     * @param step        the step
     * @return a range of int values as specified or an empty Queue if<br>
     * {@code from >= toExclusive} and {@code step > 0} or<br>
     * {@code from <= toExclusive} and {@code step < 0}
     * @throws IllegalArgumentException if {@code step} is zero
     */
    public static Queue<Integer> rangeBy(int from, int toExclusive, int step) {
        return ofAll(Iterator.rangeBy(from, toExclusive, step));
    }

    /**
     * Creates a Queue of long numbers starting from {@code from}, extending to {@code toExclusive - 1}.
     * <p>
     * Examples:
     * <pre>
     * {@code
     * Queue.range(0L, 0L)  // = Queue()
     * Queue.range(2L, 0L)  // = Queue()
     * Queue.range(-2L, 2L) // = Queue(-2L, -1L, 0L, 1L)
     * }
     * </pre>
     *
     * @param from        the first number
     * @param toExclusive the last number + 1
     * @return a range of long values as specified or an empty Queue if {@code from >= toExclusive}
     */
    public static Queue<Long> range(long from, long toExclusive) {
        return ofAll(Iterator.range(from, toExclusive));
    }

    /**
     * Creates a Queue of long numbers starting from {@code from}, extending to {@code toExclusive - 1},
     * with {@code step}.
     * <p>
     * Examples:
     * <pre>
     * {@code
     * Queue.rangeBy(1L, 3L, 1L)  // = Queue(1L, 2L)
     * Queue.rangeBy(1L, 4L, 2L)  // = Queue(1L, 3L)
     * Queue.rangeBy(4L, 1L, -2L) // = Queue(4L, 2L)
     * Queue.rangeBy(4L, 1L, 2L)  // = Queue()
     * }
     * </pre>
     *
     * @param from        the first number
     * @param toExclusive the last number + 1
     * @param step        the step
     * @return a range of long values as specified or an empty Queue if<br>
     * {@code from >= toExclusive} and {@code step > 0} or<br>
     * {@code from <= toExclusive} and {@code step < 0}
     * @throws IllegalArgumentException if {@code step} is zero
     */
    public static Queue<Long> rangeBy(long from, long toExclusive, long step) {
        return ofAll(Iterator.rangeBy(from, toExclusive, step));
    }

    /**
     * Creates a Queue of characters starting from {@code from} (inclusive)
     * up to {@code toInclusive} (inclusive).
     *
     * <p>Examples:
     * <pre>
     * Queue.rangeClosed('a', 'c')  // = Queue('a', 'b', 'c')
     * Queue.rangeClosed('c', 'a')  // = Queue()
     * </pre>
     *
     * @param from        the first character (inclusive)
     * @param toInclusive the last character (inclusive)
     * @return a Queue over the specified character range, or empty if {@code from > toInclusive}
     */
    public static Queue<Character> rangeClosed(char from, char toInclusive) {
        return ofAll(Iterator.rangeClosed(from, toInclusive));
    }

    /**
     * Creates a Queue of characters starting from {@code from} (inclusive)
     * up to {@code toInclusive} (inclusive), advancing by the specified {@code step}.
     *
     * <p>Examples:
     * <pre>
     * Queue.rangeClosedBy('a', 'c', 1)   // = Queue('a', 'b', 'c')
     * Queue.rangeClosedBy('a', 'd', 2)   // = Queue('a', 'c')
     * Queue.rangeClosedBy('d', 'a', -2)  // = Queue('d', 'b')
     * Queue.rangeClosedBy('d', 'a', 2)   // = Queue()
     * </pre>
     *
     * @param from        the first character (inclusive)
     * @param toInclusive the last character (inclusive)
     * @param step        the increment; must not be zero
     * @return a Queue over the specified character range, or empty if the step
     *         direction does not match the direction from {@code from} to {@code toInclusive}
     * @throws IllegalArgumentException if {@code step} is zero
     */
    public static Queue<Character> rangeClosedBy(char from, char toInclusive, int step) {
        return ofAll(Iterator.rangeClosedBy(from, toInclusive, step));
    }

    /**
     * Creates a Queue of double values starting from {@code from} (inclusive)
     * up to {@code toInclusive} (inclusive), advancing by the specified {@code step}.
     *
     * <p>Examples:
     * <pre>
     * Queue.rangeClosedBy(0.0, 1.0, 0.25)   // = Queue(0.0, 0.25, 0.5, 0.75, 1.0)
     * Queue.rangeClosedBy(1.0, 0.0, -0.25)  // = Queue(1.0, 0.75, 0.5, 0.25, 0.0)
     * Queue.rangeClosedBy(0.0, 1.0, -0.25)  // = Queue()
     * </pre>
     *
     * @param from        the first double value (inclusive)
     * @param toInclusive the last value (inclusive)
     * @param step        the increment; must not be zero
     * @return a Queue over the specified double range, or empty if the step
     *         direction does not match the direction from {@code from} to {@code toInclusive}
     * @throws IllegalArgumentException if {@code step} is zero and {@code from != toInclusive}
     *                                  (if {@code from == toInclusive}, a singleton Queue is returned regardless of {@code step})
     */
    public static Queue<Double> rangeClosedBy(double from, double toInclusive, double step) {
        return ofAll(Iterator.rangeClosedBy(from, toInclusive, step));
    }

    /**
     * Creates a Queue of int numbers starting from {@code from}, extending to {@code toInclusive}.
     * <p>
     * Examples:
     * <pre>
     * {@code
     * Queue.rangeClosed(0, 0)  // = Queue(0)
     * Queue.rangeClosed(2, 0)  // = Queue()
     * Queue.rangeClosed(-2, 2) // = Queue(-2, -1, 0, 1, 2)
     * }
     * </pre>
     *
     * @param from        the first number
     * @param toInclusive the last number
     * @return a range of int values as specified or an empty Queue if {@code from > toInclusive}
     */
    public static Queue<Integer> rangeClosed(int from, int toInclusive) {
        return ofAll(Iterator.rangeClosed(from, toInclusive));
    }

    /**
     * Creates a Queue of int numbers starting from {@code from}, extending to {@code toInclusive},
     * with {@code step}.
     * <p>
     * Examples:
     * <pre>
     * {@code
     * Queue.rangeClosedBy(1, 3, 1)  // = Queue(1, 2, 3)
     * Queue.rangeClosedBy(1, 4, 2)  // = Queue(1, 3)
     * Queue.rangeClosedBy(4, 1, -2) // = Queue(4, 2)
     * Queue.rangeClosedBy(4, 1, 2)  // = Queue()
     * }
     * </pre>
     *
     * @param from        the first number
     * @param toInclusive the last number
     * @param step        the step
     * @return a range of int values as specified or an empty Queue if<br>
     * {@code from > toInclusive} and {@code step > 0} or<br>
     * {@code from < toInclusive} and {@code step < 0}
     * @throws IllegalArgumentException if {@code step} is zero
     */
    public static Queue<Integer> rangeClosedBy(int from, int toInclusive, int step) {
        return ofAll(Iterator.rangeClosedBy(from, toInclusive, step));
    }

    /**
     * Creates a Queue of long numbers starting from {@code from}, extending to {@code toInclusive}.
     * <p>
     * Examples:
     * <pre>
     * {@code
     * Queue.rangeClosed(0L, 0L)  // = Queue(0L)
     * Queue.rangeClosed(2L, 0L)  // = Queue()
     * Queue.rangeClosed(-2L, 2L) // = Queue(-2L, -1L, 0L, 1L, 2L)
     * }
     * </pre>
     *
     * @param from        the first number
     * @param toInclusive the last number
     * @return a range of long values as specified or an empty Queue if {@code from > toInclusive}
     */
    public static Queue<Long> rangeClosed(long from, long toInclusive) {
        return ofAll(Iterator.rangeClosed(from, toInclusive));
    }

    /**
     * Transposes the rows and columns of a {@link Queue} matrix.
     * <p>
     * Complexity: O(rows * columns).
     *
     * @param <T> matrix element type
     * @param matrix to be transposed.
     * @return a transposed {@link Queue} matrix.
     * @throws IllegalArgumentException if the row lengths of {@code matrix} differ.
     *
     * <p>
     * ex: {@code
     * Queue.transpose(Queue(Queue(1,2,3), Queue(4,5,6))) → Queue(Queue(1,4), Queue(2,5), Queue(3,6))
     * }
     */
    public static <T extends @Nullable Object> Queue<Queue<T>> transpose(Queue<Queue<T>> matrix) {
        return com.guizmaii.zazr.collection.internal.Collections.transpose(matrix, Queue::ofAll, Queue::of);
    }

    /**
     * Creates a Queue of long numbers starting from {@code from}, extending to {@code toInclusive},
     * with {@code step}.
     * <p>
     * Examples:
     * <pre>
     * {@code
     * Queue.rangeClosedBy(1L, 3L, 1L)  // = Queue(1L, 2L, 3L)
     * Queue.rangeClosedBy(1L, 4L, 2L)  // = Queue(1L, 3L)
     * Queue.rangeClosedBy(4L, 1L, -2L) // = Queue(4L, 2L)
     * Queue.rangeClosedBy(4L, 1L, 2L)  // = Queue()
     * }
     * </pre>
     *
     * @param from        the first number
     * @param toInclusive the last number
     * @param step        the step
     * @return a range of long values as specified or an empty Queue if<br>
     * {@code from > toInclusive} and {@code step > 0} or<br>
     * {@code from < toInclusive} and {@code step < 0}
     * @throws IllegalArgumentException if {@code step} is zero
     */
    public static Queue<Long> rangeClosedBy(long from, long toInclusive, long step) {
        return ofAll(Iterator.rangeClosedBy(from, toInclusive, step));
    }

    /**
     * Creates a Queue from a seed value and a function.
     * The function takes the seed at first.
     * The function should return {@code None} when it's
     * done generating the Queue, otherwise {@code Some} {@code Tuple}
     * of the value to add to the resulting Queue and the element for
     * the next call.
     * <p>
     * Example:
     * <pre>
     * {@code
     * Queue.unfoldRight(10, x -> x == 0
     *             ? Option.none()
     *             : Option.some(new Tuple2<>(x, x-1)));
     * // Queue(10, 9, 8, 7, 6, 5, 4, 3, 2, 1))
     * }
     * </pre>
     *
     * @param <T>  type of seeds
     * @param <U>  type of unfolded values
     * @param seed the start value for the iteration
     * @param f    the function to get the next step of the iteration
     * @return a Queue with the values built up by the iteration
     * @throws NullPointerException if {@code f} is null
     */
    public static <T extends @Nullable Object, U extends @Nullable Object> Queue<U> unfoldRight(T seed, Function<? super T, Option<Tuple2<? extends U, ? extends T>>> f) {
        return Iterator.unfoldRight(seed, f).toQueue();
    }

    /**
     * Creates a Queue from a seed value and a function.
     * The function takes the seed at first.
     * The function should return {@code None} when it's
     * done generating the Queue, otherwise {@code Some} {@code Tuple}
     * of the element for the next call and
     * the value to add to the resulting Queue.
     * <p>
     * Example:
     * <pre>
     * {@code
     * Queue.unfoldLeft(10, x -> x == 0
     *             ? Option.none()
     *             : Option.some(new Tuple2<>(x-1, x)));
     * // Queue(1, 2, 3, 4, 5, 6, 7, 8, 9, 10))
     * }
     * </pre>
     *
     * @param <T>  type of seeds
     * @param <U>  type of unfolded values
     * @param seed the start value for the iteration
     * @param f    the function to get the next step of the iteration
     * @return a Queue with the values built up by the iteration
     * @throws NullPointerException if {@code f} is null
     */
    public static <T extends @Nullable Object, U extends @Nullable Object> Queue<U> unfoldLeft(T seed, Function<? super T, Option<Tuple2<? extends T, ? extends U>>> f) {
        return Iterator.unfoldLeft(seed, f).toQueue();
    }

    /**
     * Creates a Queue from a seed value and a function.
     * The function takes the seed at first.
     * The function should return {@code None} when it's
     * done generating the Queue, otherwise {@code Some} {@code Tuple}
     * of the element for the next call and
     * the value to add to the resulting Queue.
     * <p>
     * Example:
     * <pre>
     * {@code
     * Queue.unfold(10, x -> x == 0
     *             ? Option.none()
     *             : Option.some(new Tuple2<>(x-1, x)));
     * // Queue(1, 2, 3, 4, 5, 6, 7, 8, 9, 10))
     * }
     * </pre>
     *
     * @param <T>  type of seeds and unfolded values
     * @param seed the start value for the iteration
     * @param f    the function to get the next step of the iteration
     * @return a Queue with the values built up by the iteration
     * @throws NullPointerException if {@code f} is null
     */
    public static <T extends @Nullable Object> Queue<T> unfold(T seed, Function<? super T, Option<Tuple2<? extends T, ? extends T>>> f) {
        return Iterator.unfold(seed, f).toQueue();
    }

    /**
     * Enqueues a new element.
     * <p>
     * Complexity: O(1); the element is prepended to the rear list.
     *
     * @param element The new element
     * @return a new {@code Queue} instance, containing the new element
     */
    public Queue<T> enqueue(T element) {
        return new Queue<>(front, rear.prepend(element));
    }

    /**
     * Enqueues the given elements. A queue has FIFO order, i.e. the first of the given elements is
     * the first which will be retrieved.
     * <p>
     * Complexity: O(m) for m enqueued elements.
     *
     * @param elements An Iterable of elements, may be empty
     * @return a {@code Queue} containing this queue's elements followed by the given elements
     *         (may be {@code this} if {@code elements} is empty, or {@code elements} itself if
     *         this queue is empty and {@code elements} is a {@code Queue})
     * @throws NullPointerException if elements is null
     */
    @SuppressWarnings("unchecked")
    public Queue<T> enqueueAll(Iterable<? extends T> elements) {
        Objects.requireNonNull(elements, "elements is null");
        if (isEmpty() && elements instanceof Queue) {
            return (Queue<T>) elements;
        } else {
            return com.guizmaii.zazr.collection.List.ofAll(elements).foldLeft(this, Queue::enqueue);
        }
    }

    // -- The sequence API

    /**
     * Whether {@code that} occurs in this Queue as a contiguous slice.
     * <p>
     * Complexity: O(n * m) for a slice of m elements.
     *
     * @param that the slice to look for
     * @return true if {@code that} occurs contiguously in this Queue (an empty slice always does)
     * @throws NullPointerException if {@code that} is null
     */
    public boolean containsSlice(Iterable<? extends T> that) {
        Objects.requireNonNull(that, "that is null");
        return indexOfSlice(that) >= 0;
    }

    /**
     * Whether this Queue ends with {@code that}.
     * <p>
     * Complexity: O(n + m) for m elements of {@code that}.
     *
     * @param that the suffix to test
     * @return true if the last {@code m} elements equal {@code that} (an empty {@code that} is always a suffix)
     * @throws NullPointerException if {@code that} is null
     */
    public boolean endsWith(Iterable<? extends T> that) {
        return toList().endsWith(that);
    }

    /**
     * The index of the first occurrence of {@code element}, or -1.
     * <p>
     * Complexity: O(n).
     *
     * @param element the element to find
     * @return the index of its first occurrence, or -1 if absent
     */
    public int indexOf(T element) {
        return indexOf(element, 0);
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
        return toList().indexOfSlice(that, from);
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
        return toList().indexWhere(predicate, from);
    }

    /**
     * The index of the last occurrence of {@code element}, or -1.
     * <p>
     * Complexity: O(n).
     *
     * @param element the element to find
     * @return the index of its last occurrence, or -1 if absent
     */
    public int lastIndexOf(T element) {
        return lastIndexOf(element, Integer.MAX_VALUE);
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
        return toList().lastIndexOfSlice(that, end);
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
     * The index of the last element at or before {@code end} satisfying {@code predicate}, or -1.
     * <p>
     * Complexity: O(n).
     *
     * @param predicate the condition
     * @param end       the last position to look at
     * @return the last index {@code <= end} of a satisfying element, or -1
     * @throws NullPointerException if {@code predicate} is null
     */
    public int lastIndexWhere(Predicate<? super T> predicate, int end) {
        return toList().lastIndexWhere(predicate, end);
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
    public int prefixLength(Predicate<? super T> predicate) {
        return segmentLength(predicate, 0);
    }

    /**
     * The position of {@code element} in this Queue, which must already be sorted in ascending natural order; the
     * result is undefined otherwise. The search is linear, as a Queue has no indexed access.
     * <p>
     * Complexity: O(n).
     *
     * @param element the element to find
     * @return the index of the element if it is present; otherwise {@code (-(insertion point) - 1)}, the insertion
     *         point being the index at which the element would be inserted
     * @throws ClassCastException if {@code T} is not {@code Comparable}
     */
    public int search(T element) {
        return toList().search(element);
    }

    /**
     * The position of {@code element} in this Queue, which must already be sorted in ascending order according to
     * {@code comparator}; the result is undefined otherwise. The search is linear, as a Queue has no indexed access.
     * <p>
     * Complexity: O(n).
     *
     * @param element    the element to find
     * @param comparator the order this Queue is sorted by
     * @return the index of the element if it is present; otherwise {@code (-(insertion point) - 1)}, the insertion
     *         point being the index at which the element would be inserted
     * @throws NullPointerException if {@code comparator} is null
     */
    public int search(T element, Comparator<? super T> comparator) {
        return toList().search(element, comparator);
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
    public int segmentLength(Predicate<? super T> predicate, int from) {
        return toList().segmentLength(predicate, from);
    }

    /**
     * Whether this Queue starts with {@code that}: {@code startsWith(that, 0)}.
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
     * {@link #indexOf(Object)} as an {@link Option}: {@code None} for -1.
     *
     * @param element the element to find
     * @return {@code Some(index)} of its first occurrence, or {@code None}
     */
    public Option<Integer> indexOfOption(T element) {
        return com.guizmaii.zazr.collection.internal.Collections.indexOption(indexOf(element));
    }

    /**
     * {@link #indexOf(Object, int)} as an {@link Option}: {@code None} for -1.
     *
     * @param element the element to find
     * @param from    the first position to look at
     * @return {@code Some(index)} of its first occurrence at or after {@code from}, or {@code None}
     */
    public Option<Integer> indexOfOption(T element, int from) {
        return com.guizmaii.zazr.collection.internal.Collections.indexOption(indexOf(element, from));
    }

    /**
     * {@link #indexOfSlice(Iterable)} as an {@link Option}: {@code None} for -1.
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
     * {@link #indexWhere(Predicate)} as an {@link Option}: {@code None} for -1.
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
     * {@link #lastIndexOf(Object)} as an {@link Option}: {@code None} for -1.
     *
     * @param element the element to find
     * @return {@code Some(index)} of its last occurrence, or {@code None}
     */
    public Option<Integer> lastIndexOfOption(T element) {
        return com.guizmaii.zazr.collection.internal.Collections.indexOption(lastIndexOf(element));
    }

    /**
     * {@link #lastIndexOf(Object, int)} as an {@link Option}: {@code None} for -1.
     *
     * @param element the element to find
     * @param end     the last position to look at
     * @return {@code Some(index)} of its last occurrence at or before {@code end}, or {@code None}
     */
    public Option<Integer> lastIndexOfOption(T element, int end) {
        return com.guizmaii.zazr.collection.internal.Collections.indexOption(lastIndexOf(element, end));
    }

    /**
     * {@link #lastIndexOfSlice(Iterable)} as an {@link Option}: {@code None} for -1.
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
     * {@link #lastIndexWhere(Predicate)} as an {@link Option}: {@code None} for -1.
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
     * Folds the elements from the right: starts with {@code zero} and combines each element, from the last to the
     * first, with the accumulator.
     * <pre>{@code
     * // = 24
     * List.of('4', '2').foldRight(0, (x, acc) -> acc * 10 + x - '0');
     * }</pre>
     * <p>
     * The elements are folded from the end: they are copied into a {@code List} first, then folded from the left, so
     * the recursion depth does not grow with the length.
     *
     * @param <U>  the type of the accumulator
     * @param zero the initial accumulator
     * @param f    combines the next element (from the right) and the accumulator so far
     * @return the final accumulator, {@code zero} on an empty sequence
     * @throws NullPointerException if {@code f} is null
     */
    public <U extends @Nullable Object> U foldRight(U zero, BiFunction<? super T, ? super U, ? extends U> f) {
        Objects.requireNonNull(f, "f is null");
        return toList().foldRight(zero, f);
    }

    /**
     * Returns a new Queue with the given element appended at the end.
     * <p>
     * Complexity: amortised O(1); the element is prepended to the rear list.
     *
     * @param element the element to append
     * @return a new Queue ending with the given element
     */
    public Queue<T> append(T element) {
        return enqueue(element);
    }

    /**
     * Returns a new Queue with the given elements appended at the end, in iteration order.
     * <p>
     * Complexity: O(m) for m appended elements.
     *
     * @param elements the elements to append
     * @return a new Queue ending with the given elements, or this Queue if there are none
     * @throws NullPointerException if {@code elements} is null
     */
    public Queue<T> appendAll(Iterable<? extends T> elements) {
        return enqueueAll(elements);
    }

    /**
     * Returns an immutable {@link java.util.List} view of this Queue: reads go through to this Queue, mutators throw
     * {@link UnsupportedOperationException}.
     * <p>
     * Complexity: O(1); {@code get(i)} on the view is O(i).
     *
     * @return an immutable {@code java.util.List} view
     */
    public java.util.List<T> asJava() {
        return JavaConverters.asJava(this, IMMUTABLE);
    }

    /**
     * Passes an immutable {@link java.util.List} view of this Queue to {@code action} and returns this Queue.
     * <p>
     * Complexity: O(1) to create the view.
     *
     * @param action receives the view
     * @return this Queue
     * @throws NullPointerException if {@code action} is null
     * @see #asJava()
     */
    public Queue<T> asJava(Consumer<? super java.util.List<T>> action) {
        Objects.requireNonNull(action, "action is null");
        action.accept(asJava());
        return this;
    }

    /**
     * Returns a mutable {@link java.util.List} view of this Queue: every mutator replaces the view's underlying Queue
     * by a new one; this Queue is never modified.
     * <p>
     * Complexity: O(1); each mutator costs what the corresponding Queue operation costs.
     *
     * @return a mutable {@code java.util.List} view
     */
    public java.util.List<T> asJavaMutable() {
        return JavaConverters.asJava(this, MUTABLE);
    }

    /**
     * Passes a mutable {@link java.util.List} view of this Queue to {@code action} and returns the Queue the view holds
     * afterwards: this Queue if the action only read, a new one reflecting the writes otherwise.
     * <p>
     * Complexity: O(1) to create the view.
     *
     * @param action receives the view
     * @return this Queue, or a new Queue reflecting the modifications made through the view
     * @throws NullPointerException if {@code action} is null
     * @see #asJavaMutable()
     */
    public Queue<T> asJavaMutable(Consumer<? super java.util.List<T>> action) {
        Objects.requireNonNull(action, "action is null");
        final ListView<T, Queue<T>> view = JavaConverters.asJava(this, MUTABLE);
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
    public Queue<Queue<T>> combinations() {
        return ofAll(toList().combinations().map(Queue::ofAll));
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
    public Queue<Queue<T>> combinations(int k) {
        return ofAll(toList().combinations(k).map(Queue::ofAll));
    }

    /**
     * Returns a new {@code Queue} containing the elements of this instance
     * with all duplicates removed. Element equality is determined using {@code equals}.
     * <p>
     * Complexity: O(n).
     *
     * @return a new {@code Queue} without duplicate elements
     */
    public Queue<T> distinct() {
        return ofAll(toList().distinct());
    }

    /**
     * Returns a new {@code Queue} containing the elements of this instance
     * without duplicates, as determined by the given {@code comparator}; the first of two equal elements is kept.
     * <p>
     * Complexity: O(n log n) comparisons.
     *
     * @param comparator a comparator used to determine equality of elements
     * @return a new {@code Queue} with duplicates removed
     * @throws NullPointerException if {@code comparator} is null
     */
    public Queue<T> distinctBy(Comparator<? super T> comparator) {
        Objects.requireNonNull(comparator, "comparator is null");
        return ofAll(toList().distinctBy(comparator));
    }

    /**
     * Returns a new {@code Queue} containing the elements of this instance
     * without duplicates, based on keys extracted from elements using {@code keyExtractor}.
     * <p>
     * The first occurrence of each key is retained in the resulting sequence.
     * <p>
     * Complexity: O(n), one key per element.
     *
     * @param keyExtractor a function to extract keys for determining uniqueness
     * @param <U>          the type of key
     * @return a new {@code Queue} with duplicates removed based on keys
     * @throws NullPointerException if {@code keyExtractor} is null
     */
    public <U extends @Nullable Object> Queue<T> distinctBy(Function<? super T, ? extends U> keyExtractor) {
        Objects.requireNonNull(keyExtractor, "keyExtractor is null");
        return ofAll(toList().distinctBy(keyExtractor));
    }

    /**
     * The complement of {@link #distinct()}: the elements occurring more than once, each once, in order of first
     * occurrence. {@code Queue.of(3, 1, 3, 2, 1, 3).duplicates()} is {@code Queue.of(3, 1)}. {@code isEmpty()} on the
     * result is the "all distinct" test.
     * <p>
     * Complexity: O(n), one hash lookup per element.
     *
     * @return a new Queue of the repeated elements
     */
    public Queue<T> duplicates() {
        return duplicatesBy(Function.identity());
    }

    /**
     * {@link #duplicates()} under a key: the first element of each key occurring more than once, in order of first
     * occurrence. One pass, the key computed once per element.
     * <p>
     * Complexity: O(n), one key and one hash lookup per element.
     *
     * @param keyExtractor computes the key an element is compared by
     * @param <U>          the key type
     * @return a new Queue of the first element of each repeated key
     * @throws NullPointerException if {@code keyExtractor} is null
     */
    public <U extends @Nullable Object> Queue<T> duplicatesBy(Function<? super T, ? extends U> keyExtractor) {
        Objects.requireNonNull(keyExtractor, "keyExtractor is null");
        final java.util.List<T> duplicated = Collections.duplicatesBy(this, keyExtractor);
        return duplicated.isEmpty() ? empty() : ofAll(duplicated);
    }

    /**
     * The elements without duplicates, keeping the last occurrence of each group of elements the comparator calls
     * equal, in the order of those last occurrences.
     * <p>
     * Complexity: O(n log n) comparisons.
     *
     * @param comparator decides which elements are duplicates
     * @return a new Queue
     * @throws NullPointerException if {@code comparator} is null
     */
    public Queue<T> distinctByKeepLast(Comparator<? super T> comparator) {
        Objects.requireNonNull(comparator, "comparator is null");
        return ofAll(toList().distinctByKeepLast(comparator));
    }

    /**
     * The elements without duplicates, keeping the last occurrence of each key, in the order of those last
     * occurrences.
     * <p>
     * Complexity: O(n), one key per element.
     *
     * @param keyExtractor computes the key an element is deduplicated by
     * @param <U>          the key type
     * @return a new Queue
     * @throws NullPointerException if {@code keyExtractor} is null
     */
    public <U extends @Nullable Object> Queue<T> distinctByKeepLast(Function<? super T, ? extends U> keyExtractor) {
        Objects.requireNonNull(keyExtractor, "keyExtractor is null");
        return ofAll(toList().distinctByKeepLast(keyExtractor));
    }

    /**
     * Returns a new {@code Queue} without the first {@code n} elements,
     * or an empty instance if this contains fewer than {@code n} elements.
     * <p>
     * Complexity: O(n); the front and the rear are both walked.
     *
     * @param n the number of elements to drop
     * @return a new instance excluding the first {@code n} elements
     */
    public Queue<T> drop(int n) {
        if (n <= 0) {
            return this;
        }
        if (n >= length()) {
            return empty();
        }
        return new Queue<>(front.drop(n), rear.dropRight(n - front.length()));
    }

    /**
     * Returns a new {@code Queue} starting from the first element
     * that does not satisfy the given {@code predicate}, dropping all preceding elements.
     * <p>
     * This is equivalent to {@code dropUntil(predicate.negate())}, which is useful
     * for method references that cannot be negated directly.
     * <p>
     * Complexity: O(n).
     *
     * @param predicate a condition tested on each element
     * @return a new instance starting from the first element not matching the predicate
     * @throws NullPointerException if {@code predicate} is null
     */
    public Queue<T> dropWhile(Predicate<? super T> predicate) {
        Objects.requireNonNull(predicate, "predicate is null");
        final List<T> dropped = toList().dropWhile(predicate);
        return ofAll(dropped.length() == length() ? this : dropped);
    }

    /**
     * Returns a new {@code Queue} without the last {@code n} elements,
     * or an empty instance if this contains fewer than {@code n} elements.
     * <p>
     * Complexity: O(n); the front and the rear are both walked.
     *
     * @param n the number of elements to drop from the end
     * @return a new instance excluding the last {@code n} elements
     */
    public Queue<T> dropRight(int n) {
        if (n <= 0) {
            return this;
        }
        if (n >= length()) {
            return empty();
        }
        return new Queue<>(front.dropRight(n - rear.length()), rear.drop(n));
    }

    /**
     * The elements up to and including the last one satisfying {@code predicate}: the elements after it are dropped.
     * <p>
     * Complexity: O(n).
     *
     * @param predicate the condition, tested from the end
     * @return a new Queue
     * @throws NullPointerException if {@code predicate} is null
     */
    public Queue<T> dropRightUntil(Predicate<? super T> predicate) {
        Objects.requireNonNull(predicate, "predicate is null");
        return reverse().dropUntil(predicate).reverse();
    }

    /**
     * The elements up to and including the last one not satisfying {@code predicate}, that is
     * {@code dropRightUntil(predicate.negate())}.
     * <p>
     * Complexity: O(n).
     *
     * @param predicate the condition, tested from the end
     * @return a new Queue
     * @throws NullPointerException if {@code predicate} is null
     */
    public Queue<T> dropRightWhile(Predicate<? super T> predicate) {
        Objects.requireNonNull(predicate, "predicate is null");
        return dropRightUntil(predicate.negate());
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
    public Queue<T> filter(Predicate<? super T> predicate) {
        Objects.requireNonNull(predicate, "predicate is null");
        final com.guizmaii.zazr.collection.List<T> filtered = toList().filter(predicate);

        if (filtered.isEmpty()) {
            return empty();
        } else if (filtered.length() == length()) {
            return this;
        } else {
            return ofAll(filtered);
        }
    }

    public <U extends @Nullable Object> Queue<U> flatMap(Function<? super T, ? extends Iterable<? extends U>> mapper) {
        Objects.requireNonNull(mapper, "mapper is null");
        if (isEmpty()) {
            return empty();
        } else {
            return new Queue<>(toList().flatMap(mapper), com.guizmaii.zazr.collection.List.empty());
        }
    }

    /**
     * The element at {@code index}.
     * <p>
     * Complexity: O(index) while the index is in the front; O(n) once it falls in the rear, which is measured and indexed from its end.
     *
     * @param index the position
     * @return the element at that position
     * @throws IndexOutOfBoundsException if {@code index} is negative or not less than {@code length()}
     */
    public T get(int index) {
        if (isEmpty()) {
            throw new IndexOutOfBoundsException("get(" + index + ") on empty Queue");
        }
        if (index < 0) {
            throw new IndexOutOfBoundsException("get(" + index + ")");
        }
        // walk the front instead of measuring it: List.length() is O(n)
        int remaining = index;
        List<T> list = front;
        while (remaining > 0 && !list.isEmpty()) {
            list = list.tail();
            remaining--;
        }
        if (!list.isEmpty()) {
            return list.head();
        }
        final int rearIndex = remaining;
        final int rearLength = rear.length();
        if (rearIndex < rearLength) {
            return rear.get(rearLength - rearIndex - 1);
        } else {
            throw new IndexOutOfBoundsException("get(" + index + ") on Queue of length " + (index - remaining + rearLength));
        }
    }

    public <C extends @Nullable Object> Map<C, Queue<T>> groupBy(Function<? super T, ? extends C> classifier) {
        return com.guizmaii.zazr.collection.internal.Collections.groupBy(this, classifier, Queue::ofAll);
    }

    /**
     * Returns the first element of this non-empty {@code Queue}.
     * <p>
     * Complexity: O(1); the head of the front list.
     *
     * @return the first element
     * @throws NoSuchElementException if this {@code Queue} is empty
     */
    public T head() {
        if (isEmpty()) {
            throw new NoSuchElementException("head of empty Queue");
        } else {
            return front.head();
        }
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
    public int indexOf(T element, int from) {
        final int frontIndex = front.indexOf(element, from);
        if (frontIndex != -1) {
            return frontIndex;
        } else {
            // we need to reverse because we search the first occurrence
            final int frontLength = front.length();
            final int rearIndex = rear.reverse().indexOf(element, from - frontLength);
            return (rearIndex == -1) ? -1 : rearIndex + frontLength;
        }
    }

    /**
     * Returns all elements of this Queue except the last one.
     * <p>
     * This is the dual of {@link #tail()}.
     * <p>
     * Complexity: amortised O(1); the last element is the head of the rear list, unless the rear is empty and the front is walked.
     *
     * @return a new instance containing all elements except the last
     * @throws UnsupportedOperationException if this Queue is empty
     */
    public Queue<T> init() {
        if (isEmpty()) {
            throw new UnsupportedOperationException("init of empty Queue");
        } else if (rear.isEmpty()) {
            return new Queue<>(front.init(), rear);
        } else {
            return new Queue<>(front, rear.tail());
        }
    }

    /**
     * A new Queue with {@code element} inserted at {@code index}, the elements from {@code index} on shifted right.
     * <p>
     * Complexity: O(n); the front, and the rear when the index falls in it, are walked.
     *
     * @param index   the position of the inserted element
     * @param element the element to insert
     * @return a new Queue
     * @throws IndexOutOfBoundsException if {@code index} is negative or greater than {@code length()}
     */
    public Queue<T> insert(int index, T element) {
        if (index < 0) {
            throw new IndexOutOfBoundsException("insert(" + index + ", element)");
        }
        final int length = front.length();
        if (index <= length) {
            return new Queue<>(front.insert(index, element), rear);
        } else {
            final int rearIndex = index - length;
            final int rearLength = rear.length();
            if (rearIndex <= rearLength) {
                final int reverseRearIndex = rearLength - rearIndex;
                return new Queue<>(front, rear.insert(reverseRearIndex, element));
            } else {
                throw new IndexOutOfBoundsException("insert(" + index + ", element) on Queue of length " + length());
            }
        }
    }

    /**
     * A new Queue with {@code elements} inserted at {@code index}, in iteration order, the elements from {@code index}
     * on shifted right.
     * <p>
     * Complexity: O(n + m) for m inserted elements.
     * shared.
     *
     * @param index    the position of the first inserted element
     * @param elements the elements to insert
     * @return a new Queue
     * @throws IndexOutOfBoundsException if {@code index} is negative or greater than {@code length()}
     * @throws NullPointerException      if {@code elements} is null
     */
    @SuppressWarnings("unchecked")
    public Queue<T> insertAll(int index, Iterable<? extends T> elements) {
        Objects.requireNonNull(elements, "elements is null");
        if (index < 0) {
            throw new IndexOutOfBoundsException("insertAll(" + index + ", elements)");
        }
        final int length = front.length();
        if (index <= length) {
            if (isEmpty() && elements instanceof Queue) {
                return (Queue<T>) elements;
            } else {
                final com.guizmaii.zazr.collection.List<T> newFront = front.insertAll(index, elements);
                return (newFront == front) ? this : new Queue<>(newFront, rear);
            }
        } else {
            final int rearIndex = index - length;
            final int rearLength = rear.length();
            if (rearIndex <= rearLength) {
                final int reverseRearIndex = rearLength - rearIndex;
                final com.guizmaii.zazr.collection.List<T> newRear = rear.insertAll(reverseRearIndex, com.guizmaii.zazr.collection.List.ofAll(elements).reverse());
                return (newRear == rear) ? this : new Queue<>(front, newRear);
            } else {
                throw new IndexOutOfBoundsException("insertAll(" + index + ", elements) on Queue of length " + length());
            }
        }
    }

    /**
     * The elements with {@code element} inserted between every two of them.
     * <p>
     * Complexity: O(n).
     *
     * @param element the separator
     * @return a new Queue, or this Queue if it is empty
     */
    public Queue<T> intersperse(T element) {
        if (isEmpty()) {
            return this;
        } else if (rear.isEmpty()) {
            return new Queue<>(front.intersperse(element), rear);
        } else {
            return new Queue<>(front.intersperse(element), rear.intersperse(element).append(element));
        }
    }

    @Override
    public boolean isEmpty() {
        return front.isEmpty();
    }

    /**
     * {@inheritDoc}
     * <p>
     * Complexity: O(m) to create, for the m elements of the rear list, which is reversed; then O(1) per step.
     */
    @Override
    public java.util.Iterator<T> iterator() {
        return Iterator.ofAll(front).concat(rear.reverse().iterator());
    }

    /**
     * Returns the last element of this Queue.
     * <p>
     * Complexity: O(1) when the rear is non-empty, O(n) when it is empty and the front is walked.
     *
     * @return the last element
     * @throws NoSuchElementException if this Queue is empty
     */
    public T last() {
        return rear.isEmpty() ? front.last() : rear.head();
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
    public int lastIndexOf(T element, int end) {
        return toList().lastIndexOf(element, end);
    }

    /**
     * Returns the number of elements in this Queue.
     * <p>
     * Equivalent to {@link #size()}.
     * <p>
     * Complexity: O(n); the front and the rear are counted.
     *
     * @return the number of elements
     */
    public int length() {
        return front.length() + rear.length();
    }

    public <U extends @Nullable Object> Queue<U> map(Function<? super T, ? extends U> mapper) {
        Objects.requireNonNull(mapper, "mapper is null");
        return new Queue<>(front.map(mapper), rear.map(mapper));
    }

    public <U extends @Nullable Object> Queue<U> collect(Function<? super T, ? extends Option<? extends U>> mapper) {
        Objects.requireNonNull(mapper, "mapper is null");
        // the null check runs here so that the message names this type, not the List that does the walking
        return isEmpty() ? empty() : new Queue<>(toList().collect(t -> Objects.requireNonNull(mapper.apply(t), "Queue.collect: mapper returned null")), com.guizmaii.zazr.collection.List.empty());
    }

    public <U extends @Nullable Object> Queue<U> as(U value) {
        return map(ignored -> value);
    }

    public Queue<T> orElse(Iterable<? extends T> other) {
        return isEmpty() ? ofAll(other) : this;
    }

    public Queue<T> orElse(Supplier<? extends Iterable<? extends T>> supplier) {
        return isEmpty() ? ofAll(supplier.get()) : this;
    }

    /**
     * This Queue padded on the right with {@code element} until it is {@code length} long.
     * <p>
     * Complexity: O(n + k) for k added elements.
     *
     * @param length  the target length
     * @param element the padding element
     * @return a new Queue, or this Queue if it is already at least {@code length} long
     */
    public Queue<T> padTo(int length, T element) {
        final int actualLength = length();
        if (length <= actualLength) {
            return this;
        } else {
            return ofAll(toList().padTo(length, element));
        }
    }

    /**
     * This Queue padded on the left with {@code element} until it is {@code length} long.
     * <p>
     * Complexity: O(n + k) for k added elements.
     *
     * @param length  the target length
     * @param element the padding element
     * @return a new Queue, or this Queue if it is already at least {@code length} long
     */
    public Queue<T> leftPadTo(int length, T element) {
        final int actualLength = length();
        if (length <= actualLength) {
            return this;
        } else {
            return ofAll(toList().leftPadTo(length, element));
        }
    }

    /**
     * This Queue with {@code replaced} elements from {@code from} on replaced by {@code that}. A negative
     * {@code from} or {@code replaced} counts as 0.
     * <p>
     * Complexity: O(n + m) for m replacement elements.
     *
     * @param from     the first replaced position
     * @param that     the replacement elements
     * @param replaced how many elements are replaced
     * @return a new Queue
     * @throws NullPointerException if {@code that} is null
     */
    public Queue<T> patch(int from, Iterable<? extends T> that, int replaced) {
        from = Math.max(from, 0);
        replaced = Math.max(replaced, 0);
        Queue<T> result = take(from).appendAll(that);
        from += replaced;
        result = result.appendAll(drop(from));
        return result;
    }

    public Tuple2<Queue<T>, Queue<T>> partition(Predicate<? super T> predicate) {
        Objects.requireNonNull(predicate, "predicate is null");
        return toList().partition(predicate).map(com.guizmaii.zazr.collection.List::toQueue, com.guizmaii.zazr.collection.List::toQueue);
    }

    /**
     * Splits the elements into a left and a right side according to the {@link Either} {@code f} returns for each: the
     * generalisation of {@link #partition(Predicate)}. One pass in queue order, {@code f} called once per element, no
     * intermediate list of {@code Either}s.
     * <p>
     * Complexity: O(n); each side is built reversed and becomes the front list of its Queue.
     *
     * @param f   Classifies an element
     * @param <L> Component type of the left side
     * @param <R> Component type of the right side
     * @return the left values and the right values, each in the order of the elements they come from
     * @throws NullPointerException if {@code f} is null or returns null
     */
    public <L extends @Nullable Object, R extends @Nullable Object> Tuple2<Queue<L>, Queue<R>> partitionMap(Function<? super T, ? extends Either<? extends L, ? extends R>> f) {
        Objects.requireNonNull(f, "f is null");
        com.guizmaii.zazr.collection.List<L> lefts = com.guizmaii.zazr.collection.List.empty();
        com.guizmaii.zazr.collection.List<R> rights = com.guizmaii.zazr.collection.List.empty();
        for (T element : this) {
            switch (Objects.requireNonNull(f.apply(element), "Queue.partitionMap: f returned null")) {
                case Either.Left(var left) -> lefts = lefts.prepend(left);
                case Either.Right(var right) -> rights = rights.prepend(right);
            }
        }
        return Tuple.of(ofAll(lefts.reverse()), ofAll(rights.reverse()));
    }

    /**
     * All distinct permutations of the elements.
     * <p>
     * Complexity: O(n!) permutations.
     *
     * @return the permutations
     */
    public Queue<Queue<T>> permutations() {
        return ofAll(toList().permutations().map(com.guizmaii.zazr.collection.List::toQueue));
    }

    /**
     * A new Queue with {@code element} in front of this one.
     * <p>
     * Complexity: O(1); the element is prepended to the front list.
     *
     * @param element the new head
     * @return a new Queue starting with the given element
     */
    public Queue<T> prepend(T element) {
        return new Queue<>(front.prepend(element), rear);
    }

    /**
     * A new Queue with {@code elements} in front of this one, in iteration order.
     * <p>
     * Complexity: O(m) for m prepended elements.
     *
     * @param elements the elements to prepend
     * @return a new Queue starting with the given elements, or this Queue if there are none
     * @throws NullPointerException if {@code elements} is null
     */
    @SuppressWarnings("unchecked")
    public Queue<T> prependAll(Iterable<? extends T> elements) {
        Objects.requireNonNull(elements, "elements is null");
        if (isEmpty() && elements instanceof Queue) {
            return (Queue<T>) elements;
        } else {
            final com.guizmaii.zazr.collection.List<T> newFront = front.prependAll(elements);
            return (newFront == front) ? this : new Queue<>(newFront, rear);
        }
    }

    /**
     * This Queue without the first occurrence of {@code element}.
     * <p>
     * Complexity: O(n).
     *
     * @param element the element to remove
     * @return a new Queue, or this Queue if the element is absent
     */
    public Queue<T> remove(T element) {
        final com.guizmaii.zazr.collection.List<T> removed = toList().remove(element);
        return ofAll(removed.length() == length() ? this : removed);
    }

    /**
     * This Queue without the first element satisfying {@code predicate}.
     * <p>
     * Complexity: O(n).
     *
     * @param predicate the condition
     * @return a new Queue, or this Queue if no element satisfies the predicate
     * @throws NullPointerException if {@code predicate} is null
     */
    public Queue<T> removeFirst(Predicate<T> predicate) {
        final com.guizmaii.zazr.collection.List<T> removed = toList().removeFirst(predicate);
        return ofAll(removed.length() == length() ? this : removed);
    }

    /**
     * This Queue without the last element satisfying {@code predicate}.
     * <p>
     * Complexity: O(n).
     *
     * @param predicate the condition
     * @return a new Queue, or this Queue if no element satisfies the predicate
     * @throws NullPointerException if {@code predicate} is null
     */
    public Queue<T> removeLast(Predicate<T> predicate) {
        final com.guizmaii.zazr.collection.List<T> removed = toList().removeLast(predicate);
        return ofAll(removed.length() == length() ? this : removed);
    }

    /**
     * This Queue without the element at {@code index}, the elements after it shifted left.
     * <p>
     * Complexity: O(n).
     *
     * @param index the position of the removed element
     * @return a new Queue
     * @throws IndexOutOfBoundsException if {@code index} is negative or not less than {@code length()}
     */
    public Queue<T> removeAt(int index) {
        return ofAll(toList().removeAt(index));
    }

    /**
     * This Queue without any occurrence of {@code element}.
     * <p>
     * Complexity: O(n).
     *
     * @param element the element to remove
     * @return a new Queue, or this Queue if the element is absent
     */
    public Queue<T> removeAll(T element) {
        return com.guizmaii.zazr.collection.internal.Collections.removeAll(this, element, kept -> filter(kept));
    }

    /**
     * Replaces the first occurrence of {@code currentElement} with {@code newElement}, if it exists.
     * <p>
     * Complexity: O(n).
     *
     * @param currentElement the element to be replaced
     * @param newElement     the replacement element
     * @return a new Queue with the first occurrence of {@code currentElement} replaced by {@code newElement}
     */
    public Queue<T> replace(T currentElement, T newElement) {
        final com.guizmaii.zazr.collection.List<T> newFront = front.replace(currentElement, newElement);
        if (newFront != front) {
            return new Queue<>(newFront, rear);
        }
        final com.guizmaii.zazr.collection.List<T> rearInOrder = rear.reverse();
        final com.guizmaii.zazr.collection.List<T> newRearInOrder = rearInOrder.replace(currentElement, newElement);
        if (newRearInOrder == rearInOrder) {
            return this;
        }
        return new Queue<>(front, newRearInOrder.reverse());
    }

    /**
     * Replaces all occurrences of {@code currentElement} with {@code newElement}.
     * <p>
     * Complexity: O(n).
     *
     * @param currentElement the element to be replaced
     * @param newElement     the replacement element
     * @return a new Queue with all occurrences of {@code currentElement} replaced by {@code newElement}
     */
    public Queue<T> replaceAll(T currentElement, T newElement) {
        final com.guizmaii.zazr.collection.List<T> newFront = front.replaceAll(currentElement, newElement);
        final com.guizmaii.zazr.collection.List<T> newRear = rear.replaceAll(currentElement, newElement);
        return newFront.size() + newRear.size() == 0 ? empty()
                                                     : newFront == front && newRear == rear ? this
                                                                                            : new Queue<>(newFront, newRear);
    }

    /**
     * The elements in reverse order.
     * <p>
     * Complexity: O(n).
     *
     * @return a new Queue, or this Queue if it is empty
     */
    public Queue<T> reverse() {
        return isEmpty() ? this : ofAll(toList().reverse());
    }

    /**
     * Rotates the elements {@code n} positions to the left: {@code Queue(1, 2, 3, 4, 5).rotateLeft(2)} is
     * {@code Queue(3, 4, 5, 1, 2)}. A negative {@code n} rotates right; {@code n} is taken modulo the length.
     * <p>
     * Complexity: O(n); O(1) for {@code n == 0}, which is answered without walking the elements.
     *
     * @param n the distance
     * @return the rotated Queue, or this Queue if the rotation is a multiple of the length
     */
    public Queue<T> rotateLeft(int n) {
        // n == 0 before length(): a no-op rotation must not walk the elements, let alone force a lazy sequence
        if (n == 0 || isEmpty()) {
            return this;
        }
        final int k = Math.floorMod(n, length());
        return (k == 0) ? this : drop(k).appendAll(take(k));
    }

    /**
     * Rotates the elements {@code n} positions to the right: {@code Queue(1, 2, 3, 4, 5).rotateRight(2)} is
     * {@code Queue(4, 5, 1, 2, 3)}. A negative {@code n} rotates left; {@code n} is taken modulo the length.
     * <p>
     * Complexity: O(n); O(1) for {@code n == 0}, which is answered without walking the elements.
     *
     * @param n the distance
     * @return the rotated Queue, or this Queue if the rotation is a multiple of the length
     */
    public Queue<T> rotateRight(int n) {
        // n == 0 before length(): a no-op rotation must not walk the elements, let alone force a lazy sequence
        if (n == 0 || isEmpty()) {
            return this;
        }
        final int k = Math.floorMod(n, length());
        return (k == 0) ? this : takeRight(k).appendAll(dropRight(k));
    }

    /**
     * Computes a prefix scan of the elements of this Queue.
     * <p>
     * The neutral element {@code zero} may be applied more than once.
     * <p>
     * Complexity: O(n).
     *
     * @param zero      the neutral element for the operator
     * @param operation an associative binary operator
     * @return a new Queue containing the prefix scan of the elements
     * @throws NullPointerException if {@code operation} is null
     */
    public Queue<T> scan(T zero, BiFunction<? super T, ? super T, ? extends T> operation) {
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
     * @return a new Queue containing the cumulative results
     * @throws NullPointerException if {@code operation} is null
     */
    public <U extends @Nullable Object> Queue<U> scanLeft(U zero, BiFunction<? super U, ? super T, ? extends U> operation) {
        return com.guizmaii.zazr.collection.internal.Collections.scanLeft(this, zero, operation, Iterator::toQueue);
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
     * @return a new Queue containing the cumulative results
     * @throws NullPointerException if {@code operation} is null
     */
    public <U extends @Nullable Object> Queue<U> scanRight(U zero, BiFunction<? super T, ? super U, ? extends U> operation) {
        return com.guizmaii.zazr.collection.internal.Collections.scanRight(this, zero, operation, Iterator::toQueue);
    }

    /**
     * The elements in a random order, drawn from a default source of randomness.
     * <p>
     * Complexity: O(n).
     *
     * @return a new Queue, or this Queue if it has fewer than two elements
     */
    public Queue<T> shuffle() {
        return com.guizmaii.zazr.collection.internal.Collections.shuffle(this, Queue::ofAll);
    }

    /**
     * The elements from {@code beginIndex} inclusive to {@code endIndex} exclusive, both clamped to the bounds of
     * this Queue.
     * <p>
     * Complexity: O(n).
     *
     * @param beginIndex the first position
     * @param endIndex   the position after the last one
     * @return a new Queue, empty if the range is empty
     */
    public Queue<T> slice(int beginIndex, int endIndex) {
        return ofAll(toList().slice(beginIndex, endIndex));
    }

    /**
     * The elements in ascending natural order (a stable sort).
     * <p>
     * Complexity: O(n log n) comparisons.
     *
     * @return a new sorted Queue, or this Queue if it is empty
     * @throws ClassCastException if {@code T} is not {@code Comparable}
     */
    public Queue<T> sorted() {
        return ofAll(toList().sorted());
    }

    /**
     * The elements in the order of {@code comparator} (a stable sort).
     * <p>
     * Complexity: O(n log n) comparisons.
     *
     * @param comparator the order
     * @return a new sorted Queue, or this Queue if it is empty
     * @throws NullPointerException if {@code comparator} is null
     */
    public Queue<T> sorted(Comparator<? super T> comparator) {
        Objects.requireNonNull(comparator, "comparator is null");
        return ofAll(toList().sorted(comparator));
    }

    /**
     * The elements sorted by the natural order of the key {@code mapper} computes (a stable sort).
     * <p>
     * Complexity: O(n log n) comparisons; the key is recomputed at every comparison.
     *
     * @param mapper computes the sort key
     * @param <U>    the key type
     * @return a new sorted Queue, or this Queue if it is empty
     * @throws NullPointerException if {@code mapper} is null
     */
    public <U extends Comparable<? super U>> Queue<T> sortBy(Function<? super T, ? extends U> mapper) {
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
     * @return a new sorted Queue, or this Queue if it is empty
     * @throws NullPointerException if {@code comparator} or {@code mapper} is null
     */
    public <U extends @Nullable Object> Queue<T> sortBy(Comparator<? super U> comparator, Function<? super T, ? extends U> mapper) {
        Objects.requireNonNull(comparator, "comparator is null");
        Objects.requireNonNull(mapper, "mapper is null");
        return sorted((e1, e2) -> comparator.compare(mapper.apply(e1), mapper.apply(e2)));
    }

    /**
     * Splits this {@code Queue} into a prefix and remainder according to the given {@code predicate}.
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
    public Tuple2<Queue<T>, Queue<T>> span(Predicate<? super T> predicate) {
        Objects.requireNonNull(predicate, "predicate is null");
        return toList().span(predicate).map(com.guizmaii.zazr.collection.List::toQueue, com.guizmaii.zazr.collection.List::toQueue);
    }

    /**
     * This Queue split in two at position {@code n}: the first {@code n} elements and the rest.
     * <p>
     * Complexity: O(n).
     *
     * @param n the position of the split
     * @return the prefix and the suffix
     */
    public Tuple2<Queue<T>, Queue<T>> splitAt(int n) {
        return toList().splitAt(n).map(com.guizmaii.zazr.collection.List::toQueue, com.guizmaii.zazr.collection.List::toQueue);
    }

    /**
     * This Queue split in two before the first element satisfying {@code predicate}. If no element satisfies it, the
     * whole Queue is the first part.
     * <p>
     * Complexity: O(n).
     *
     * @param predicate the condition
     * @return the prefix and the suffix
     */
    public Tuple2<Queue<T>, Queue<T>> splitAt(Predicate<? super T> predicate) {
        return toList().splitAt(predicate).map(com.guizmaii.zazr.collection.List::toQueue, com.guizmaii.zazr.collection.List::toQueue);
    }

    /**
     * This Queue split in two after the first element satisfying {@code predicate}. If no element satisfies it, the
     * whole Queue is the first part.
     * <p>
     * Complexity: O(n).
     *
     * @param predicate the condition
     * @return the prefix including the matching element, and the suffix
     */
    public Tuple2<Queue<T>, Queue<T>> splitAtInclusive(Predicate<? super T> predicate) {
        return toList().splitAtInclusive(predicate).map(com.guizmaii.zazr.collection.List::toQueue, com.guizmaii.zazr.collection.List::toQueue);
    }

    /**
     * Whether the elements from {@code offset} on start with {@code that}. {@code that} is walked once, so a
     * one-shot iterator is accepted.
     * <p>
     * Complexity: O(offset + m) for m elements of {@code that}.
     *
     * @param that   the prefix to test
     * @param offset the position in this Queue at which the prefix should start
     * @return false if {@code offset} is negative; otherwise true if {@code that} equals the {@code m} elements from
     *         {@code offset} on (an empty {@code that} is always a prefix, even beyond the end)
     * @throws NullPointerException if {@code that} is null
     */
    public boolean startsWith(Iterable<? extends T> that, int offset) {
        return toList().startsWith(that, offset);
    }

    /**
     * The elements from {@code beginIndex} on.
     * <p>
     * Complexity: O(n).
     *
     * @param beginIndex the first position
     * @return a new Queue
     * @throws IndexOutOfBoundsException if {@code beginIndex} is negative or greater than {@code length()}
     */
    public Queue<T> subSequence(int beginIndex) {
        if (beginIndex < 0 || beginIndex > length()) {
            throw new IndexOutOfBoundsException("subSequence(" + beginIndex + ")");
        } else {
            return drop(beginIndex);
        }
    }

    /**
     * The elements from {@code beginIndex} inclusive to {@code endIndex} exclusive.
     * <p>
     * Complexity: O(n).
     *
     * @param beginIndex the first position
     * @param endIndex   the position after the last one
     * @return a new Queue
     * @throws IndexOutOfBoundsException if the range is not within {@code [0, length()]}
     * @throws IllegalArgumentException  if {@code beginIndex} is greater than {@code endIndex}
     */
    public Queue<T> subSequence(int beginIndex, int endIndex) {
        Collections.subSequenceRangeCheck(beginIndex, endIndex, length());
        if (beginIndex == endIndex) {
            return empty();
        } else if (beginIndex == 0 && endIndex == length()) {
            return this;
        } else {
            return ofAll(toList().subSequence(beginIndex, endIndex));
        }
    }

    /**
     * Returns a new {@code Queue} without its first element.
     * <p>
     * Complexity: amortised O(1); the front loses its head, and the rear is reversed onto it only when the front runs out.
     *
     * @return a new {@code Queue} containing all elements except the first
     * @throws UnsupportedOperationException if this {@code Queue} is empty
     */
    public Queue<T> tail() {
        if (isEmpty()) {
            throw new UnsupportedOperationException("tail of empty Queue");
        } else {
            return new Queue<>(front.tail(), rear);
        }
    }

    /**
     * Returns the first {@code n} elements of this {@code Queue}, or all elements if {@code n} exceeds the length.
     * <p>
     * If {@code n < 0}, an empty instance is returned. If {@code n > length()}, the full instance is returned.
     * <p>
     * Complexity: O(n).
     *
     * @param n the number of elements to take
     * @return a new {@code Queue} containing the first {@code n} elements
     */
    public Queue<T> take(int n) {
        if (n <= 0) {
            return empty();
        }
        if (n >= length()) {
            return this;
        }
        final int frontLength = front.length();
        if (n < frontLength) {
            return new Queue<>(front.take(n), com.guizmaii.zazr.collection.List.empty());
        } else if (n == frontLength) {
            return new Queue<>(front, com.guizmaii.zazr.collection.List.empty());
        } else {
            return new Queue<>(front, rear.takeRight(n - frontLength));
        }
    }

    /**
     * Takes elements from this {@code Queue} until the given predicate holds for an element.
     * <p>
     * Equivalent to {@code takeWhile(predicate.negate())}, but useful when using method references
     * that cannot be negated directly.
     * <p>
     * Complexity: O(n).
     *
     * @param predicate a condition tested sequentially on the elements
     * @return a new {@code Queue} containing all elements before the first one that satisfies the predicate
     * @throws NullPointerException if {@code predicate} is null
     */
    public Queue<T> takeUntil(Predicate<? super T> predicate) {
        Objects.requireNonNull(predicate, "predicate is null");
        final com.guizmaii.zazr.collection.List<T> taken = toList().takeUntil(predicate);
        return taken.length() == length() ? this : ofAll(taken);
    }

    /**
     * Returns the last {@code n} elements of this {@code Queue}, or all elements if {@code n} exceeds the length.
     * <p>
     * If {@code n < 0}, an empty instance is returned. If {@code n > length()}, the full instance is returned.
     * <p>
     * Complexity: O(n).
     *
     * @param n the number of elements to take from the end
     * @return a new {@code Queue} containing the last {@code n} elements
     */
    public Queue<T> takeRight(int n) {
        if (n <= 0) {
            return empty();
        }
        if (n >= length()) {
            return this;
        }
        final int rearLength = rear.length();
        if (n < rearLength) {
            return new Queue<>(rear.take(n).reverse(), com.guizmaii.zazr.collection.List.empty());
        } else if (n == rearLength) {
            return new Queue<>(rear.reverse(), com.guizmaii.zazr.collection.List.empty());
        } else {
            return new Queue<>(front.takeRight(n - rearLength), rear);
        }
    }

    /**
     * The longest suffix whose elements, from the end, do not satisfy {@code predicate}.
     * <p>
     * Complexity: O(n).
     *
     * @param predicate the condition, tested from the end
     * @return a new Queue
     * @throws NullPointerException if {@code predicate} is null
     */
    public Queue<T> takeRightUntil(Predicate<? super T> predicate) {
        Objects.requireNonNull(predicate, "predicate is null");
        final com.guizmaii.zazr.collection.List<T> taken = toList().takeRightUntil(predicate);
        return taken.length() == length() ? this : ofAll(taken);
    }

    /**
     * The longest suffix whose elements, from the end, all satisfy {@code predicate}.
     * <p>
     * Complexity: O(n).
     *
     * @param predicate the condition, tested from the end
     * @return a new Queue
     * @throws NullPointerException if {@code predicate} is null
     */
    public Queue<T> takeRightWhile(Predicate<? super T> predicate) {
        Objects.requireNonNull(predicate, "predicate is null");
        return takeRightUntil(predicate.negate());
    }

    public <T1 extends @Nullable Object, T2 extends @Nullable Object> Tuple2<Queue<T1>, Queue<T2>> unzip(
      Function<? super T, Tuple2<? extends T1, ? extends T2>> unzipper) {
        Objects.requireNonNull(unzipper, "unzipper is null");
        return toList().unzip(unzipper).map(com.guizmaii.zazr.collection.List::toQueue, com.guizmaii.zazr.collection.List::toQueue);
    }

    public <T1 extends @Nullable Object, T2 extends @Nullable Object, T3 extends @Nullable Object> Tuple3<Queue<T1>, Queue<T2>, Queue<T3>> unzip3(Function<? super T, Tuple3<? extends T1, ? extends T2, ? extends T3>> unzipper) {
        Objects.requireNonNull(unzipper, "unzipper is null");
        return toList().unzip3(unzipper).map(com.guizmaii.zazr.collection.List::toQueue, com.guizmaii.zazr.collection.List::toQueue, com.guizmaii.zazr.collection.List::toQueue);
    }

    /**
     * This Queue with the element at {@code index} replaced by {@code element}.
     * <p>
     * Complexity: O(n).
     *
     * @param index   the position to update
     * @param element the new element
     * @return a new Queue
     * @throws IndexOutOfBoundsException if {@code index} is negative or not less than {@code length()}
     */
    public Queue<T> update(int index, T element) {
        return ofAll(toList().update(index, element));
    }

    /**
     * This Queue with the element at {@code index} replaced by what {@code updater} computes from it.
     * <p>
     * Complexity: O(n).
     *
     * @param index   the position to update
     * @param updater computes the new element from the current one
     * @return a new Queue
     * @throws IndexOutOfBoundsException if {@code index} is negative or not less than {@code length()}
     * @throws NullPointerException      if {@code updater} is null
     */
    public Queue<T> update(int index, Function<? super T, ? extends T> updater) {
        Objects.requireNonNull(updater, "updater is null");
        return update(index, updater.apply(get(index)));
    }

    /**
     * Returns a {@code Queue} formed by pairing elements of this {@code Queue} with elements of another
     * {@code Iterable}. Pairing stops when either collection runs out of elements; any remaining elements in the longer
     * collection are ignored.
     * <p>
     * The length of the resulting {@code Queue} is the minimum of the lengths of this {@code Queue} and
     * {@code that}.
     * <p>
     * Complexity: O(min(n, m)) for an argument of m elements.
     *
     * @param <U>  the type of elements in the second half of each pair
     * @param that an {@code Iterable} providing the second element of each pair
     * @return a new {@code Queue} containing pairs of corresponding elements
     * @throws NullPointerException if {@code that} is null
     */
    public <U extends @Nullable Object> Queue<Tuple2<T, U>> zip(Iterable<? extends U> that) {
        return zipWith(that, Tuple::of);
    }

    /**
     * Returns a {@code Queue} by combining elements of this {@code Queue} with elements of another
     * {@code Iterable} using a mapping function. Pairing stops when either collection runs out of elements.
     * <p>
     * The length of the resulting {@code Queue} is the minimum of the lengths of this {@code Queue} and
     * {@code that}.
     * <p>
     * Complexity: O(min(n, m)) for an argument of m elements.
     *
     * @param <U>    the type of elements in the second parameter of the mapper
     * @param <R>    the type of elements in the resulting {@code Queue}
     * @param that   an {@code Iterable} providing the second parameter of the mapper
     * @param mapper a function that combines elements from this and {@code that} into a new element
     * @return a new {@code Queue} containing mapped elements
     * @throws NullPointerException if {@code that} or {@code mapper} is null
     */
    @SuppressWarnings("unchecked")
    public <U extends @Nullable Object, R extends @Nullable Object> Queue<R> zipWith(Iterable<? extends U> that, BiFunction<? super T, ? super U, ? extends R> mapper) {
        Objects.requireNonNull(that, "that is null");
        Objects.requireNonNull(mapper, "mapper is null");
        return ofAll(toList().zipWith(that, mapper));
    }

    /**
     * Returns a {@code Queue} formed by pairing elements of this {@code Queue} with elements of another
     * {@code Iterable}, filling in placeholder elements when one collection is shorter than the other.
     * <p>
     * The length of the resulting {@code Queue} is the maximum of the lengths of this {@code Queue} and
     * {@code that}.
     * <p>
     * If this {@code Queue} is shorter than {@code that}, {@code thisElem} is used as a filler. Conversely, if
     * {@code that} is shorter, {@code thatElem} is used.
     * <p>
     * Complexity: O(max(n, m)) for an argument of m elements.
     *
     * @param <U>      the type of elements in the second half of each pair
     * @param that     an {@code Iterable} providing the second element of each pair
     * @param thisElem the element used to fill missing values if this {@code Queue} is shorter than {@code that}
     * @param thatElem the element used to fill missing values if {@code that} is shorter than this {@code Queue}
     * @return a new {@code Queue} containing pairs of elements, including fillers as needed
     * @throws NullPointerException if {@code that} is null
     */
    public <U extends @Nullable Object> Queue<Tuple2<T, U>> zipAll(Iterable<? extends U> that, T thisElem, U thatElem) {
        Objects.requireNonNull(that, "that is null");
        return ofAll(toList().zipAll(that, thisElem, thatElem));
    }

    /**
     * Zips this {@code Queue} with its indices, starting at 0.
     * <p>
     * Complexity: O(n).
     *
     * @return a new {@code Queue} containing each element paired with its index
     */
    public Queue<Tuple2<T, Integer>> zipWithIndex() {
        return zipWithIndex(Tuple::of);
    }

    /**
     * Zips this {@code Queue} with its indices and maps the resulting pairs using the provided mapper.
     * <p>
     * Complexity: O(n).
     *
     * @param <U>    the type of elements in the resulting {@code Queue}
     * @param mapper a function mapping an element and its index to a new element
     * @return a new {@code Queue} containing the mapped elements
     * @throws NullPointerException if {@code mapper} is null
     */
    public <U extends @Nullable Object> Queue<U> zipWithIndex(BiFunction<? super T, ? super Integer, ? extends U> mapper) {
        Objects.requireNonNull(mapper, "mapper is null");
        return ofAll(toList().zipWithIndex(mapper));
    }

    @Override
    public boolean equals(@Nullable Object o) {
        return com.guizmaii.zazr.collection.internal.Collections.equals(this, o);
    }

    @Override
    public int hashCode() {
        return com.guizmaii.zazr.collection.internal.Collections.hashOrdered(this);
    }

    /**
     * Removes an element from this Queue.
     * <p>
     * Complexity: amortised O(1); see {@link #tail()}.
     *
     * @return a tuple containing the first element and the remaining elements of this Queue
     * @throws NoSuchElementException if this Queue is empty
     */
    public Tuple2<T, Queue<T>> dequeue() {
        if (isEmpty()) {
            throw new NoSuchElementException("dequeue of empty " + getClass().getSimpleName());
        } else {
            return Tuple.of(head(), tail());
        }
    }

    /**
     * Removes an element from this Queue.
     * <p>
     * Complexity: amortised O(1); see {@link #dequeue()}.
     *
     * @return {@code None} if this Queue is empty, otherwise {@code Some} {@code Tuple} containing the first element and the remaining elements of this Queue
     */
    public Option<Tuple2<T, Queue<T>>> dequeueOption() {
        return isEmpty() ? Option.none() : Option.some(dequeue());
    }

    /**
     * Enqueues the given elements. A queue has FIFO order, i.e. the first of the given elements is
     * the first which will be retrieved.
     * <p>
     * Complexity: O(m) for m enqueued elements.
     *
     * @param elements Elements, may be empty
     * @return a new {@code Queue} instance, containing the new elements
     * @throws NullPointerException if elements is null
     */
    @SuppressWarnings("unchecked")
    public Queue<T> enqueue(T ... elements) {
        Objects.requireNonNull(elements, "elements is null");
        return enqueueAll(com.guizmaii.zazr.collection.List.of(elements));
    }

    /**
     * Returns the first element without modifying it.
     * <p>
     * Complexity: O(1); the head of the front list.
     *
     * @return the first element
     * @throws NoSuchElementException if this Queue is empty
     */
    public T peek() {
        if (isEmpty()) {
            throw new NoSuchElementException("peek of empty " + getClass().getSimpleName());
        } else {
            return head();
        }
    }

    /**
     * Returns the first element without modifying the Queue.
     * <p>
     * A {@code null} head throws {@link NullPointerException}, see {@link #headOption()}.
     * <p>
     * Complexity: O(1); the head of the front list.
     *
     * @return {@code None} if this Queue is empty, otherwise a {@code Some} containing the first element
     */
    public Option<T> peekOption() {
        return isEmpty() ? Option.none() : Option.some(peek());
    }

    /**
     * Returns a new {@code Queue} starting from the first element
     * that satisfies the given {@code predicate}, dropping all preceding elements.
     * <p>
     * Complexity: O(n).
     *
     * @param predicate a condition tested on each element
     * @return a new instance starting from the first element matching the predicate
     * @throws NullPointerException if {@code predicate} is null
     */
    public Queue<T> dropUntil(Predicate<? super T> predicate) {
        Objects.requireNonNull(predicate, "predicate is null");
        return dropWhile(predicate.negate());
    }

    /**
     * Dual of {@linkplain #tailOption()}, returning all elements except the last as {@code Option}.
     * <p>
     * Complexity: amortised O(1); see {@link #init()}.
     *
     * @return {@code Some(Queue)} or {@code None} if this is empty.
     */
    public Option<Queue<T>> initOption() {
        return isEmpty() ? Option.none() : Option.some(init());
    }

    /**
     * Returns a new {@code Queue} without its first element as an {@code Option}.
     * <p>
     * Complexity: amortised O(1); see {@link #tail()}.
     *
     * @return {@code Some(traversable)} if non-empty, otherwise {@code None}
     */
    public Option<Queue<T>> tailOption() {
        return isEmpty() ? Option.none() : Option.some(tail());
    }

    /**
     * Retains only the elements from this Queue that are contained in the given {@code elements}.
     * <p>
     * Complexity: O(n + m) for m retained elements (they are hashed once, then one filter pass).
     *
     * @param elements the elements to keep
     * @return a new Queue containing only the elements present in {@code elements}, in their original order
     * @throws NullPointerException if {@code elements} is null
     */
    public Queue<T> retainAll(Iterable<? extends T> elements) {
        return Collections.retainAll(this, elements, kept -> filter(kept));
    }

    /**
     * Removes all occurrences of the specified elements from this Queue.
     * <p>
     * Complexity: O(n + m) for m removed elements (they are hashed once, then one filter pass).
     *
     * @param elements the elements to be removed
     * @return a new Queue with all occurrences of the specified elements removed
     * @throws NullPointerException if {@code elements} is null
     */
    public Queue<T> removeAll(Iterable<? extends T> elements) {
        return Collections.removeAll(this, elements, kept -> filter(kept));
    }

    /**
     * Removes all elements from this Queue that satisfy the given predicate.
     * <p>
     * Complexity: O(n).
     *
     * @param predicate the predicate used to test elements
     * @return a new Queue with all elements that satisfy the predicate removed
     * @throws NullPointerException if {@code predicate} is null
     * @deprecated Use {@link #reject(Predicate)} instead
     */
    @Deprecated
    public Queue<T> removeAll(Predicate<? super T> predicate) {
        Objects.requireNonNull(predicate, "predicate is null");
        return reject(predicate);
    }

    public Queue<T> reject(Predicate<? super T> predicate) {
        return Collections.reject(this, predicate, kept -> filter(kept));
    }

    /**
     * Takes elements from this {@code Queue} while the given predicate holds.
     * <p>
     * Complexity: O(n).
     *
     * @param predicate a condition tested sequentially on the elements
     * @return a new {@code Queue} containing all elements up to (but not including) the first one
     *         that does not satisfy the predicate
     * @throws NullPointerException if {@code predicate} is null
     */
    public Queue<T> takeWhile(Predicate<? super T> predicate) {
        Objects.requireNonNull(predicate, "predicate is null");
        return takeUntil(predicate.negate());
    }

    public Queue<T> tap(Consumer<? super T> action) {
        Objects.requireNonNull(action, "action is null");
        forEach(action);
        return this;
    }

    @Override
    public String toString() {
        return mkString("Queue(", ", ", ")");
    }

    // -- windows and products

    /**
     * The elements in consecutive blocks of {@code size}: {@code Queue.of(1, 2, 3, 4, 5).grouped(2)} is
     * {@code Queue(Queue(1, 2), Queue(3, 4), Queue(5))}; the last block is smaller when {@code size} does not
     * divide the length. The same as {@code sliding(size, size)}.
     * <p>
     * Complexity: O(n); each block is copied into its own Queue.
     *
     * @param size the block size, positive
     * @return the blocks, in order; empty if this Queue is empty
     * @throws IllegalArgumentException if {@code size} is not positive
     */
    public Queue<Queue<T>> grouped(int size) {
        return sliding(size, size);
    }

    /**
     * The windows of {@code size} consecutive elements, each starting one element after the previous:
     * {@code Queue.of(1, 2, 3, 4).sliding(3)} is {@code Queue(Queue(1, 2, 3), Queue(2, 3, 4))}. A Queue shorter
     * than {@code size} is one window. The same as {@code sliding(size, 1)}.
     * <p>
     * Complexity: O(n * size); each window is copied into its own Queue.
     *
     * @param size the window size, positive
     * @return the windows, in order; empty if this Queue is empty
     * @throws IllegalArgumentException if {@code size} is not positive
     */
    public Queue<Queue<T>> sliding(int size) {
        return sliding(size, 1);
    }

    /**
     * The windows of {@code size} consecutive elements, each starting {@code step} elements after the previous:
     * {@code Queue.of(1, 2, 3, 4, 5).sliding(2, 3)} is {@code Queue(Queue(1, 2), Queue(4, 5))} and
     * {@code sliding(2, 4)} is {@code Queue(Queue(1, 2), Queue(5))}. The last window is shorter than {@code size}
     * when it reaches the end; a window whose elements all belong to the previous one is not produced, so
     * {@code Queue.of(1, 2, 3, 4).sliding(3)} has two windows. A Queue shorter than {@code size} is one window; an
     * empty Queue has none.
     * <p>
     * Complexity: O(n * size / step); each window is copied into its own Queue.
     *
     * @param size the window size, positive
     * @param step the distance between two window starts, positive
     * @return the windows, in order
     * @throws IllegalArgumentException if {@code size} or {@code step} is not positive
     */
    public Queue<Queue<T>> sliding(int size, int step) {
        return ofAll(Iterator.ofAll(this).sliding(size, step).map(Queue::ofAll));
    }

    /**
     * The elements in maximal runs of consecutive elements with the same key, computed once per element by
     * {@code classifier}: {@code Queue.of(1, 2, 3, 10, 12, 5, 7, 20, 29).slideBy(x -> x / 10)} is
     * {@code Queue(Queue(1, 2, 3), Queue(10, 12), Queue(5, 7), Queue(20, 29))}. The runs concatenate back to this
     * Queue.
     * <p>
     * Complexity: O(n); each run is copied into its own Queue.
     *
     * @param classifier the key of an element; two consecutive elements are in the same run when their keys are
     *                   equal
     * @return the runs, in order; empty if this Queue is empty
     * @throws NullPointerException if {@code classifier} is null
     */
    public Queue<Queue<T>> slideBy(Function<? super T, ?> classifier) {
        Objects.requireNonNull(classifier, "classifier is null");
        return ofAll(Iterator.ofAll(this).slideBy(classifier).map(Queue::ofAll));
    }

    /**
     * The Cartesian square of this Queue: every pair {@code (a, b)} of elements, {@code a} varying slowest.
     * <p>
     * Complexity: O(n^2); the pairs are built now.
     *
     * @return the pairs
     */
    public Queue<Tuple2<T, T>> crossProduct() {
        return crossProduct(this);
    }

    /**
     * The Cartesian power of this Queue: every Queue of {@code power} elements drawn from this one, in
     * lexicographic position order. {@code power == 0} gives one empty Queue; a negative power gives no result.
     * <p>
     * Complexity: O(n^power) Queues of size {@code power}, built now.
     *
     * @param power the size of each result
     * @return the Queues
     */
    public Queue<Queue<T>> crossProduct(int power) {
        if (power < 0) {
            return empty();
        }
        Queue<Queue<T>> product = Queue.of(Queue.<T> empty());
        for (int i = 0; i < power; i++) {
            product = product.flatMap(el -> map(el::append));
        }
        return product;
    }

    /**
     * The Cartesian product of this Queue and {@code that}: every pair {@code (a, b)} with {@code a} from this
     * Queue and {@code b} from {@code that}, {@code a} varying slowest. {@code that} is walked once.
     * <p>
     * Complexity: O(n * m) for m elements of {@code that}; the pairs are built now.
     *
     * @param that the right-hand elements
     * @param <U>  their type
     * @return the pairs
     * @throws NullPointerException if {@code that} is null
     */
    public <U extends @Nullable Object> Queue<Tuple2<T, U>> crossProduct(Iterable<? extends U> that) {
        Objects.requireNonNull(that, "that is null");
        final Queue<U> other = Queue.ofAll(that);
        return flatMap(a -> other.map(b -> Tuple.of(a, b)));
    }

    /**
     * Combines the elements from the right: the last with the one before it, the result with the one before that,
     * and so on.
     * <p>
     * Complexity: O(n); the elements are walked once as a List, in reverse.
     *
     * @param op combines the next element and the result so far
     * @return the combined result
     * @throws NoSuchElementException if this Queue is empty
     * @throws NullPointerException   if {@code op} is null
     */
    public T reduceRight(BiFunction<? super T, ? super T, ? extends T> op) {
        Objects.requireNonNull(op, "op is null");
        if (isEmpty()) {
            throw new NoSuchElementException("reduceRight on empty Queue");
        }
        return toList().reduceRight(op);
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
     *
     * @return {@code Some(maximum)} if there is an element, {@code None} otherwise
     * @throws ClassCastException if two or more elements are not {@code Comparable}
     */
    public Option<T> max() {
        return TraversableModule.max(this);
    }

    /**
     * The greatest element according to {@code comparator}; of equal greatest elements, the first in this
     * Queue's order.
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
     * element in this Queue's order.
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
     *
     * @return {@code Some(minimum)} if there is an element, {@code None} otherwise
     * @throws ClassCastException if two or more elements are not {@code Comparable}
     */
    public Option<T> min() {
        return TraversableModule.min(this);
    }

    /**
     * The least element according to {@code comparator}; of equal least elements, the first in this Queue's
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
     * this Queue's order.
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
     * @return the folded result, {@code zero} on an empty Queue
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
     * @throws NoSuchElementException if this Queue is empty
     * @throws NullPointerException   if {@code op} is null
     */
    public T reduce(BiFunction<? super T, ? super T, ? extends T> op) {
        return TraversableModule.reduceLeft(this, op);
    }

    /**
     * {@link #reduce(BiFunction)} as an {@code Option}: {@code None} on an empty Queue.
     *
     * @param op combines two elements
     * @return {@code Some(result)}, or {@code None} if this Queue is empty
     * @throws NullPointerException if {@code op} is null
     */
    public Option<T> reduceOption(BiFunction<? super T, ? super T, ? extends T> op) {
        return TraversableModule.reduceLeftOption(this, op);
    }

    /**
     * The only element.
     *
     * @return the element
     * @throws NoSuchElementException if this Queue is empty or has more than one element
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
     * from the first element. {@code 0} on an empty Queue.
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
     * {@code 1} on an empty Queue.
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
     * Complexity: that of {@link #head()}.
     *
     * @return {@code Some(head)}, or {@code None} if this Queue is empty
     */
    public Option<T> headOption() {
        return isEmpty() ? Option.none() : Option.some(head());
    }

    /**
     * The last element as an {@code Option}.
     * <p>
     * Complexity: that of {@link #last()}.
     *
     * @return {@code Some(last)}, or {@code None} if this Queue is empty
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
     * @throws NoSuchElementException if this Queue is empty
     * @throws NullPointerException   if {@code op} is null
     */
    public T reduceLeft(BiFunction<? super T, ? super T, ? extends T> op) {
        return TraversableModule.reduceLeft(this, op);
    }

    /**
     * {@link #reduceLeft(BiFunction)} as an {@code Option}: {@code None} on an empty Queue.
     *
     * @param op combines the result so far and the next element
     * @return {@code Some(result)}, or {@code None} if this Queue is empty
     * @throws NullPointerException if {@code op} is null
     */
    public Option<T> reduceLeftOption(BiFunction<? super T, ? super T, ? extends T> op) {
        return TraversableModule.reduceLeftOption(this, op);
    }

    /**
     * {@link #reduceRight(BiFunction)} as an {@code Option}: {@code None} on an empty Queue.
     *
     * @param op combines the next element and the result so far
     * @return {@code Some(result)}, or {@code None} if this Queue is empty
     * @throws NullPointerException if {@code op} is null
     */
    public Option<T> reduceRightOption(BiFunction<? super T, ? super T, ? extends T> op) {
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
     * The elements copied into a new mutable {@link java.util.Collection} that {@code factory} makes for the given
     * capacity, in this Queue's order: {@code toJavaCollection(java.util.LinkedHashSet::new)}.
     *
     * @param factory makes an empty mutable collection with the given initial capacity
     * @param <C>     the collection type
     * @return the new collection, filled
     * @throws NullPointerException if {@code factory} is null
     */
    public <C extends java.util.Collection<T>> C toJavaCollection(Function<Integer, C> factory) {
        return TraversableModule.toJavaCollection(this, factory);
    }

    /**
     * The elements copied into a new {@link java.util.ArrayList}, in this Queue's order.
     *
     * @return the new list
     */
    public java.util.List<T> toJavaList() {
        return TraversableModule.toJavaCollection(this, ArrayList::new, 10);
    }

    /**
     * The elements copied into a new mutable {@link java.util.List} that {@code factory} makes for the given
     * capacity, in this Queue's order: {@code toJavaList(capacity -> new java.util.LinkedList<>())}.
     *
     * @param factory makes an empty mutable list with the given initial capacity
     * @param <LIST>  the list type
     * @return the new list, filled
     * @throws NullPointerException if {@code factory} is null
     */
    public <LIST extends java.util.List<T>> LIST toJavaList(Function<Integer, LIST> factory) {
        return TraversableModule.toJavaCollection(this, factory);
    }

    /**
     * The elements as the entries of a new {@link java.util.HashMap}, each mapped to a key and a value by
     * {@code f}; of two entries with the same key, the later one in this Queue's order wins.
     *
     * @param f   the entry an element becomes
     * @param <K> the key type
     * @param <V> the value type
     * @return the new map
     * @throws NullPointerException if {@code f} is null
     */
    public <K extends @Nullable Object, V extends @Nullable Object> java.util.Map<K, V> toJavaMap(Function<? super T, ? extends Tuple2<? extends K, ? extends V>> f) {
        return TraversableModule.toJavaMap(this, java.util.HashMap::new, f);
    }

    /**
     * The elements as the entries of a new mutable {@link java.util.Map} that {@code factory} makes, each mapped
     * to a key by {@code keyMapper} and to a value by {@code valueMapper}; of two entries with the same key, the
     * later one in this Queue's order wins.
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
    public <K extends @Nullable Object, V extends @Nullable Object, MAP extends java.util.Map<K, V>> MAP toJavaMap(Supplier<MAP> factory, Function<? super T, ? extends K> keyMapper, Function<? super T, ? extends V> valueMapper) {
        return TraversableModule.toJavaMap(this, factory, TraversableModule.entryMapper(keyMapper, valueMapper));
    }

    /**
     * The elements as the entries of a new mutable {@link java.util.Map} that {@code factory} makes, each mapped
     * to a key and a value by {@code f}; of two entries with the same key, the later one in this Queue's order
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
    public <K extends @Nullable Object, V extends @Nullable Object, MAP extends java.util.Map<K, V>> MAP toJavaMap(Supplier<MAP> factory, Function<? super T, ? extends Tuple2<? extends K, ? extends V>> f) {
        return TraversableModule.toJavaMap(this, factory, f);
    }

    /**
     * The distinct elements copied into a new {@link java.util.HashSet}.
     *
     * @return the new set
     */
    public java.util.Set<T> toJavaSet() {
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
    public <SET extends java.util.Set<T>> SET toJavaSet(Function<Integer, SET> factory) {
        return TraversableModule.toJavaCollection(this, factory);
    }

    /**
     * A parallel {@link java.util.stream.Stream} over the elements, built on {@link #spliterator()}.
     *
     * @return a new parallel {@code java.util.stream.Stream}
     */
    public java.util.stream.Stream<T> toJavaParallelStream() {
        return StreamSupport.stream(spliterator(), true);
    }

    /**
     * The elements as the entries of a new {@link HashMap}, each mapped to a key by {@code keyMapper} and to a
     * value by {@code valueMapper}; of two entries with the same key, the later one in this Queue's order wins.
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
     * entries with the same key, the later one in this Queue's order wins.
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
     * The elements as the entries of a new {@link LinkedHashMap}, in this Queue's order, each mapped to a key by
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
     * The elements as the entries of a new {@link LinkedHashMap}, in this Queue's order, each mapped to a key
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
     * in this Queue's order wins.
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
     * and a value by {@code f}; of two entries with the same key, the later one in this Queue's order wins.
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
     * this Queue's order wins.
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
     * a value by {@code f}; of two entries with the same key, the later one in this Queue's order wins.
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
     * The elements as a {@link Queue}, in this Queue's order.
     *
     * @return a {@code Queue} of the elements
     */
    public Queue<T> toQueue() {
        return TraversableModule.toTraversable(this, Queue.empty(), Queue::ofAll);
    }

    /**
     * The distinct elements as a {@link LinkedHashSet}, in this Queue's order.
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
     * The elements as a {@link Stream}, in this Queue's order.
     *
     * @return a {@code Stream} of the elements
     */
    public Stream<T> toStream() {
        return TraversableModule.toTraversable(this, Stream.empty(), Stream::ofAll);
    }

}
