package com.guizmaii.zazr.collection;

import com.guizmaii.zazr.*;
import com.guizmaii.zazr.control.Option;
import java.util.*;
import java.util.function.*;
import java.util.stream.Collector;
import org.jspecify.annotations.Nullable;

import static com.guizmaii.zazr.collection.JavaConverters.ChangePolicy.IMMUTABLE;
import static com.guizmaii.zazr.collection.JavaConverters.ChangePolicy.MUTABLE;
import static com.guizmaii.zazr.collection.JavaConverters.ListView;

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
        } else if (!elements.iterator().hasNext()) {
            return empty();
        } else if (elements instanceof com.guizmaii.zazr.collection.List) {
            return new Queue<>((com.guizmaii.zazr.collection.List<T>) elements, com.guizmaii.zazr.collection.List.empty());
        } else {
            return new Queue<>(com.guizmaii.zazr.collection.List.ofAll(elements), com.guizmaii.zazr.collection.List.empty());
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
        return com.guizmaii.zazr.collection.Collections.tabulate(n, f, empty(), Queue::of);
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
        return com.guizmaii.zazr.collection.Collections.fill(n, s, empty(), Queue::of);
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
        return com.guizmaii.zazr.collection.Collections.fillObject(n, element, empty(), Queue::of);
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
        return com.guizmaii.zazr.collection.Collections.transpose(matrix, Queue::ofAll, Queue::of);
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
     * The Cartesian square of this Queue: every pair {@code (a, b)} of elements, {@code a} varying slowest.
     * <p>
     * Complexity: lazy; O(n^2) pairs when consumed.
     *
     * @return an iterator over the pairs
     */
    public Iterator<Tuple2<T, T>> crossProduct() {
        return crossProduct(this);
    }

    /**
     * The Cartesian product of this Queue and {@code that}: every pair {@code (a, b)} with {@code a} from this Queue
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
    public <U extends @Nullable Object> Iterator<Tuple2<T, U>> crossProduct(Iterable<? extends U> that) {
        Objects.requireNonNull(that, "that is null");
        // a lazy, memoising Stream: the result is lazy, so the argument stays lazy too
        final Stream<U> other = Stream.ofAll(that);
        return Iterator.ofAll(this).flatMap(a -> other.map(b -> Tuple.of(a, b)));
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
     * An iterator over the elements from {@code index} on.
     * <p>
     * Complexity: O(index) to reach the start, then O(1) per step.
     *
     * @param index the first position to iterate from
     * @return an iterator over the suffix
     * @throws IndexOutOfBoundsException if {@code index} is negative or greater than {@code length()}
     */
    public Iterator<T> iterator(int index) {
        return subSequence(index).iterator();
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
     * An iterator over the elements from the last to the first.
     * <p>
     * Complexity: O(n) to create (the front is reversed; the rear is already stored in reverse order), then O(1) per step.
     *
     * @return the reverse iterator
     */
    public Iterator<T> reverseIterator() {
        return rear.iterator().concat(front.reverseIterator());
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

    /**
     * {@inheritDoc}
     * <p>
     * The elements are folded from the end: they are copied into a {@code List} first, then folded from the left, so
     * the recursion depth does not grow with the length.
     */
    @Override
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
     * The Cartesian power of this Queue: every Queue of {@code power} elements drawn from this one, in lexicographic
     * position order. {@code power == 0} gives one empty Queue; a negative power gives no result.
     * <p>
     * Complexity: lazy; O(n^power) Queues of size {@code power} when consumed.
     *
     * @param power the size of each result
     * @return an iterator over the Lists
     */
    public Iterator<Queue<T>> crossProduct(int power) {
        if (power < 0) {
            return Iterator.empty();
        }
        return Iterator.range(0, power).foldLeft(Iterator.of(Queue.<T> empty()), (product, ignored) -> product.flatMap(el -> map(el::append)));
    }

    /**
     * {@inheritDoc}
     * <p>
     * Complexity: O(n).
     */
    @Override
    public Queue<T> distinct() {
        return ofAll(toList().distinct());
    }

    /**
     * {@inheritDoc}
     * <p>
     * Complexity: O(n log n) comparisons.
     */
    @Override
    public Queue<T> distinctBy(Comparator<? super T> comparator) {
        Objects.requireNonNull(comparator, "comparator is null");
        return ofAll(toList().distinctBy(comparator));
    }

    /**
     * {@inheritDoc}
     * <p>
     * Complexity: O(n), one key per element.
     */
    @Override
    public <U extends @Nullable Object> Queue<T> distinctBy(Function<? super T, ? extends U> keyExtractor) {
        Objects.requireNonNull(keyExtractor, "keyExtractor is null");
        return ofAll(toList().distinctBy(keyExtractor));
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
     * {@inheritDoc}
     * <p>
     * Complexity: O(n); the front and the rear are both walked.
     */
    @Override
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
     * {@inheritDoc}
     * <p>
     * Complexity: O(n).
     */
    @Override
    public Queue<T> dropWhile(Predicate<? super T> predicate) {
        Objects.requireNonNull(predicate, "predicate is null");
        final List<T> dropped = toList().dropWhile(predicate);
        return ofAll(dropped.length() == length() ? this : dropped);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Complexity: O(n); the front and the rear are both walked.
     */
    @Override
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
     * {@inheritDoc}
     * <p>
     * Complexity: O(n).
     */
    @Override
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

    @Override
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

    @Override
    public <C extends @Nullable Object> Map<C, Queue<T>> groupBy(Function<? super T, ? extends C> classifier) {
        return com.guizmaii.zazr.collection.Collections.groupBy(this, classifier, Queue::ofAll);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Complexity: lazy; O(size) per group when consumed.
     */
    @Override
    public Iterator<Queue<T>> grouped(int size) {
        return sliding(size, size);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Complexity: O(1); the head of the front list.
     */
    @Override
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
     * {@inheritDoc}
     * <p>
     * Complexity: amortised O(1); the last element is the head of the rear list, unless the rear is empty and the front is walked.
     */
    @Override
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
    public Iterator<T> iterator() {
        return front.iterator().concat(rear.reverseIterator());
    }

    /**
     * {@inheritDoc}
     * <p>
     * Complexity: O(1) when the rear is non-empty, O(n) when it is empty and the front is walked.
     */
    @Override
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
     * {@inheritDoc}
     * <p>
     * Complexity: O(n); the front and the rear are counted.
     */
    @Override
    public int length() {
        return front.length() + rear.length();
    }

    @Override
    public <U extends @Nullable Object> Queue<U> map(Function<? super T, ? extends U> mapper) {
        Objects.requireNonNull(mapper, "mapper is null");
        return new Queue<>(front.map(mapper), rear.map(mapper));
    }

    @Override
    public <U extends @Nullable Object> Queue<U> collect(Function<? super T, ? extends Option<? extends U>> mapper) {
        Objects.requireNonNull(mapper, "mapper is null");
        // the null check runs here so that the message names this type, not the List that does the walking
        return isEmpty() ? empty() : new Queue<>(toList().collect(t -> Objects.requireNonNull(mapper.apply(t), "Queue.collect: mapper returned null")), com.guizmaii.zazr.collection.List.empty());
    }

    @Override
    public <U extends @Nullable Object> Queue<U> as(U value) {
        return map(ignored -> value);
    }

    @Override
    public Queue<T> orElse(Iterable<? extends T> other) {
        return isEmpty() ? ofAll(other) : this;
    }

    @Override
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

    @Override
    public Tuple2<Queue<T>, Queue<T>> partition(Predicate<? super T> predicate) {
        Objects.requireNonNull(predicate, "predicate is null");
        return toList().partition(predicate).map(com.guizmaii.zazr.collection.List::toQueue, com.guizmaii.zazr.collection.List::toQueue);
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
        return com.guizmaii.zazr.collection.Collections.removeAll(this, element);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Complexity: O(n).
     */
    @Override
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
     * {@inheritDoc}
     * <p>
     * Complexity: O(n).
     */
    @Override
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
     * {@inheritDoc}
     * <p>
     * Complexity: O(n).
     */
    @Override
    public Queue<T> scan(T zero, BiFunction<? super T, ? super T, ? extends T> operation) {
        return scanLeft(zero, operation);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Complexity: O(n).
     */
    @Override
    public <U extends @Nullable Object> Queue<U> scanLeft(U zero, BiFunction<? super U, ? super T, ? extends U> operation) {
        return com.guizmaii.zazr.collection.Collections.scanLeft(this, zero, operation, Iterator::toQueue);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Complexity: O(n).
     */
    @Override
    public <U extends @Nullable Object> Queue<U> scanRight(U zero, BiFunction<? super T, ? super U, ? extends U> operation) {
        return com.guizmaii.zazr.collection.Collections.scanRight(this, zero, operation, Iterator::toQueue);
    }

    /**
     * The elements in a random order, drawn from a default source of randomness.
     * <p>
     * Complexity: O(n).
     *
     * @return a new Queue, or this Queue if it has fewer than two elements
     */
    public Queue<T> shuffle() {
        return com.guizmaii.zazr.collection.Collections.shuffle(this, Queue::ofAll);
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

    @Override
    public Iterator<Queue<T>> slideBy(Function<? super T, ?> classifier) {
        return iterator().slideBy(classifier).map(Queue::ofAll);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Complexity: lazy; O(size) per window when consumed.
     */
    @Override
    public Iterator<Queue<T>> sliding(int size) {
        return sliding(size, 1);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Complexity: lazy; O(size) per window when consumed.
     */
    @Override
    public Iterator<Queue<T>> sliding(int size, int step) {
        return iterator().sliding(size, step).map(Queue::ofAll);
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
     * {@inheritDoc}
     * <p>
     * Complexity: O(n).
     */
    @Override
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
     * {@inheritDoc}
     * <p>
     * Complexity: amortised O(1); the front loses its head, and the rear is reversed onto it only when the front runs out.
     */
    @Override
    public Queue<T> tail() {
        if (isEmpty()) {
            throw new UnsupportedOperationException("tail of empty Queue");
        } else {
            return new Queue<>(front.tail(), rear);
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * Complexity: O(n).
     */
    @Override
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
     * {@inheritDoc}
     * <p>
     * Complexity: O(n).
     */
    @Override
    public Queue<T> takeUntil(Predicate<? super T> predicate) {
        Objects.requireNonNull(predicate, "predicate is null");
        final com.guizmaii.zazr.collection.List<T> taken = toList().takeUntil(predicate);
        return taken.length() == length() ? this : ofAll(taken);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Complexity: O(n).
     */
    @Override
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

    @Override
    public <T1 extends @Nullable Object, T2 extends @Nullable Object> Tuple2<Queue<T1>, Queue<T2>> unzip(
      Function<? super T, Tuple2<? extends T1, ? extends T2>> unzipper) {
        Objects.requireNonNull(unzipper, "unzipper is null");
        return toList().unzip(unzipper).map(com.guizmaii.zazr.collection.List::toQueue, com.guizmaii.zazr.collection.List::toQueue);
    }

    @Override
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
     * {@inheritDoc}
     * <p>
     * Complexity: O(min(n, m)) for an argument of m elements.
     */
    @Override
    public <U extends @Nullable Object> Queue<Tuple2<T, U>> zip(Iterable<? extends U> that) {
        return zipWith(that, Tuple::of);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Complexity: O(min(n, m)) for an argument of m elements.
     */
    @SuppressWarnings("unchecked")
    @Override
    public <U extends @Nullable Object, R extends @Nullable Object> Queue<R> zipWith(Iterable<? extends U> that, BiFunction<? super T, ? super U, ? extends R> mapper) {
        Objects.requireNonNull(that, "that is null");
        Objects.requireNonNull(mapper, "mapper is null");
        return ofAll(toList().zipWith(that, mapper));
    }

    /**
     * {@inheritDoc}
     * <p>
     * Complexity: O(max(n, m)) for an argument of m elements.
     */
    @Override
    public <U extends @Nullable Object> Queue<Tuple2<T, U>> zipAll(Iterable<? extends U> that, T thisElem, U thatElem) {
        Objects.requireNonNull(that, "that is null");
        return ofAll(toList().zipAll(that, thisElem, thatElem));
    }

    /**
     * {@inheritDoc}
     * <p>
     * Complexity: O(n).
     */
    @Override
    public Queue<Tuple2<T, Integer>> zipWithIndex() {
        return zipWithIndex(Tuple::of);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Complexity: O(n).
     */
    @Override
    public <U extends @Nullable Object> Queue<U> zipWithIndex(BiFunction<? super T, ? super Integer, ? extends U> mapper) {
        Objects.requireNonNull(mapper, "mapper is null");
        return ofAll(toList().zipWithIndex(mapper));
    }

    @Override
    public boolean equals(@Nullable Object o) {
        return com.guizmaii.zazr.collection.Collections.equals(this, o);
    }

    @Override
    public int hashCode() {
        return com.guizmaii.zazr.collection.Collections.hashOrdered(this);
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
     * {@inheritDoc}
     * <p>
     * Complexity: O(n).
     */
    @Override
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
     * {@inheritDoc}
     * <p>
     * Complexity: amortised O(1); see {@link #tail()}.
     */
    @Override
    public Option<Queue<T>> tailOption() {
        return isEmpty() ? Option.none() : Option.some(tail());
    }

    /**
     * {@inheritDoc}
     * <p>
     * Complexity: O(n + m) for m retained elements (they are hashed once, then one filter pass).
     */
    @Override
    public Queue<T> retainAll(Iterable<? extends T> elements) {
        return Collections.retainAll(this, elements);
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
        return Collections.removeAll(this, elements);
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

    @Override
    public Queue<T> reject(Predicate<? super T> predicate) {
        return Collections.reject(this, predicate);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Complexity: O(n).
     */
    @Override
    public Queue<T> takeWhile(Predicate<? super T> predicate) {
        Objects.requireNonNull(predicate, "predicate is null");
        return takeUntil(predicate.negate());
    }

    @Override
    public Queue<T> tap(Consumer<? super T> action) {
        Objects.requireNonNull(action, "action is null");
        forEach(action);
        return this;
    }

    @Override
    public String toString() {
        return mkString("Queue(", ", ", ")");
    }
}
