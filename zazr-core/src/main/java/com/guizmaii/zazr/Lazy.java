package com.guizmaii.zazr;

import com.guizmaii.zazr.collection.Seq;
import com.guizmaii.zazr.collection.Vector;
import java.util.Objects;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import org.jspecify.annotations.Nullable;

/**
 * Represents a lazily evaluated value. Unlike a standard {@link java.util.function.Supplier},
 * {@code Lazy} is memoizing: once the computation succeeds, its result is cached and the computation is not
 * performed again, ensuring referential transparency. If the computation throws, the exception propagates,
 * nothing is memoized, and the computation is retried on the next access.
 *
 * <p>A {@code Lazy} is a value, not a container: it is never empty, is not iterable, and {@link #get()} is its
 * only conversion. It may hold {@code null}, unlike {@code Option}, {@code Either}, {@code Try} and
 * {@code Validation} (design 3.9), so wrapping its value in one of those is done explicitly, e.g.
 * {@code Option.ofNullable(lazy.get())}.</p>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * final Lazy<Double> l = Lazy.of(Math::random);
 * l.isEvaluated(); // false
 * double value = l.get(); // evaluates and returns a random number, e.g., 0.123
 * l.isEvaluated(); // true
 * double memoizedValue = l.get(); // returns the same value as before, e.g., 0.123
 * }</pre>
 *
 * @param <T> the type of the lazily evaluated value
 * @author Daniel Dietrich
 */
public final class Lazy<T extends @Nullable Object> {

    private final ReentrantLock lock = new ReentrantLock();

    // read http://javarevisited.blogspot.de/2014/05/double-checked-locking-on-singleton-in-java.html
    private volatile @Nullable Supplier<? extends T> supplier;

    private volatile @Nullable T value;

    // should not be called directly
    private Lazy(Supplier<? extends T> supplier) {
        this.supplier = supplier;
    }

    /**
     * Narrows a {@code Lazy<? extends T>} to {@code Lazy<T>} via a
     * type-safe cast. Safe here because the lazy value is immutable and no elements
     * can be added that would violate the type (covariance)
     *
     * @param lazy the lazy value to narrow
     * @param <T>  the target element type
     * @return the same lazy value viewed as {@code Lazy<T>}
     */
    @SuppressWarnings("unchecked")
    public static <T extends @Nullable Object> Lazy<T> narrow(Lazy<? extends T> lazy) {
        return (Lazy<T>) lazy;
    }

    /**
     * Creates a {@code Lazy} instance that obtains its value from the given {@code Supplier}.
     * The supplier is invoked at most once on successful evaluation, and its result is cached for subsequent
     * calls. If the supplier throws, the exception propagates to the caller of {@link #get()}, the value is
     * not memoized, and the supplier is invoked again on the next call to {@link #get()}.
     *
     * @param <T>      the type of the lazy value
     * @param supplier the supplier providing the value
     * @return a new {@code Lazy} instance
     * @throws NullPointerException if {@code supplier} is null
     */
    public static <T extends @Nullable Object> Lazy<T> of(Supplier<? extends T> supplier) {
        Objects.requireNonNull(supplier, "supplier is null");
        return new Lazy<>(supplier);
    }

    /**
     * Turns many {@code Lazy} values into one {@code Lazy} of a {@link Seq} of their values, in iteration order.
     * Nothing is evaluated until the returned {@code Lazy} is, which then evaluates every element.
     *
     * @param <T>    the value type
     * @param values the {@code Lazy} values to collect
     * @return an unevaluated {@code Lazy} of all the values
     * @throws NullPointerException if {@code values} is null
     */
    public static <T extends @Nullable Object> Lazy<Seq<T>> collectAll(Iterable<? extends Lazy<? extends T>> values) {
        Objects.requireNonNull(values, "values is null");
        return Lazy.of(() -> Vector.ofAll(values).map(Lazy::get));
    }

    /**
     * Evaluates this lazy value on the first call and caches the result.
     * Subsequent calls return the cached value without recomputation.
     *
     * @return the evaluated value
     */
    @SuppressWarnings("NullAway") // see computeValue(): a null supplier implies value is computed
    public T get() {
        return (supplier == null) ? value : computeValue();
    }

    // `supplier` is nulled only *after* `value` is written, and both are volatile, so observing
    // a null supplier guarantees the computed value is visible. NullAway cannot express that.
    @SuppressWarnings("NullAway")
    private T computeValue() {
        lock.lock();
        try {
            final Supplier<? extends T> s = supplier;
            if (s != null) {
                value = s.get();
                supplier = null;
            }
        } finally {
            lock.unlock();
        }
        return value;
    }

    /**
     * Checks whether this lazy value has been evaluated.
     *
     * <p>Note: The value is evaluated internally (at most once) when {@link #get()} is called.</p>
     *
     * @return {@code true} if the value has been evaluated, {@code false} otherwise
     */
    public boolean isEvaluated() {
        return supplier == null;
    }

    /**
     * Returns a {@code Lazy} that applies {@code mapper} to this value when it is first evaluated.
     * <p>
     * A {@code Lazy} may hold {@code null}, so the mapper may return it.
     *
     * @param mapper a function applied to the value
     * @param <U>    the type of the mapped value
     * @return a new, unevaluated {@code Lazy}
     * @throws NullPointerException if {@code mapper} is null
     */
    public <U extends @Nullable Object> Lazy<U> map(Function<? super T, ? extends U> mapper) {
        Objects.requireNonNull(mapper, "mapper is null");
        return Lazy.of(() -> mapper.apply(get()));
    }

    /**
     * Returns a {@code Lazy} that, when first evaluated, applies {@code mapper} to this value and evaluates
     * the {@code Lazy} it returns. The mapper must return a {@code Lazy}, never {@code null}: a {@code null}
     * result is rejected at evaluation time, i.e. by {@link #get()} on the returned {@code Lazy}, which then
     * stays unevaluated. (The value a {@code Lazy} holds may be {@code null}; see {@link #map(Function)}.)
     *
     * @param mapper a function from the value to another {@code Lazy}
     * @param <U>    the type of the resulting value
     * @return a new, unevaluated {@code Lazy}
     * @throws NullPointerException if {@code mapper} is null
     */
    public <U extends @Nullable Object> Lazy<U> flatMap(Function<? super T, ? extends Lazy<? extends U>> mapper) {
        Objects.requireNonNull(mapper, "mapper is null");
        return Lazy.of(() -> Objects.requireNonNull(mapper.apply(get()), "Lazy.flatMap: mapper returned null").get());
    }

    // -- zip (design 3.4)

    /**
     * Pairs this value with {@code that}'s: an unevaluated {@code Lazy} that, when first evaluated, evaluates this
     * value then {@code that} and caches the pair. The same as {@link #zipWith(Lazy, BiFunction)} with
     * {@code Tuple::of}.
     *
     * @param that the other {@code Lazy}
     * @param <U>  the value type of {@code that}
     * @return a new, unevaluated {@code Lazy} of the pair of values
     * @throws NullPointerException if {@code that} is null
     */
    public <U extends @Nullable Object> Lazy<Tuple2<T, U>> zip(Lazy<? extends U> that) {
        return zipWith(that, Tuple::of);
    }

    /**
     * Combines this value with {@code that}'s through {@code f}: an unevaluated {@code Lazy} that, when first
     * evaluated, evaluates this value then {@code that}, applies {@code f} to the two values and caches the result.
     * Nothing is evaluated before that, and {@code f} runs at most once. A {@code Lazy} may hold {@code null}, so
     * {@code f} may return it.
     *
     * @param that the other {@code Lazy}
     * @param f    combines the two values
     * @param <U>  the value type of {@code that}
     * @param <V>  the result type
     * @return a new, unevaluated {@code Lazy} of the combined value
     * @throws NullPointerException if {@code that} or {@code f} is null
     */
    public <U extends @Nullable Object, V extends @Nullable Object> Lazy<V> zipWith(Lazy<? extends U> that, BiFunction<? super T, ? super U, ? extends V> f) {
        Objects.requireNonNull(that, "that is null");
        Objects.requireNonNull(f, "f is null");
        return Lazy.of(() -> f.apply(get(), that.get()));
    }

    /**
     * {@link #zip(Lazy)} keeping this value: an unevaluated {@code Lazy} that, when first evaluated, evaluates this
     * value then {@code that} and caches this value. {@code that} is evaluated for its effect only.
     *
     * @param that the other {@code Lazy}
     * @param <U>  the value type of {@code that}
     * @return a new, unevaluated {@code Lazy} of this value
     * @throws NullPointerException if {@code that} is null
     */
    public <U extends @Nullable Object> Lazy<T> zipLeft(Lazy<? extends U> that) {
        Objects.requireNonNull(that, "that is null");
        return Lazy.of(() -> {
            final T value = get();
            that.get();
            return value;
        });
    }

    /**
     * {@link #zip(Lazy)} keeping {@code that}'s value: an unevaluated {@code Lazy} that, when first evaluated,
     * evaluates this value then {@code that} and caches {@code that}'s value. This value is evaluated for its effect
     * only.
     *
     * @param that the other {@code Lazy}
     * @param <U>  the value type of {@code that}
     * @return a new, unevaluated {@code Lazy} of {@code that}'s value
     * @throws NullPointerException if {@code that} is null
     */
    public <U extends @Nullable Object> Lazy<U> zipRight(Lazy<? extends U> that) {
        Objects.requireNonNull(that, "that is null");
        return Lazy.of(() -> {
            get();
            return that.get();
        });
    }

    /**
     * Pairs the values of two {@code Lazy} values: an unevaluated {@code Lazy} that, when first evaluated, evaluates
     * every argument in argument order and caches the tuple of their values. The same as
     * {@link #zipWith(Lazy, Lazy, BiFunction)} with {@code Tuple::of}.
     *
     * @param l1  the first {@code Lazy}
     * @param l2  the second {@code Lazy}
     * @param <T1> the value type of {@code l1}
     * @param <T2> the value type of {@code l2}
     * @return a new, unevaluated {@code Lazy} of the tuple of the values
     * @throws NullPointerException if any argument is null
     */
    public static <T1 extends @Nullable Object, T2 extends @Nullable Object> Lazy<Tuple2<T1, T2>> zip(Lazy<? extends T1> l1, Lazy<? extends T2> l2) {
        return zipWith(l1, l2, Tuple::of);
    }

    /**
     * Combines the values of two {@code Lazy} values through {@code f}: an unevaluated {@code Lazy} that, when first
     * evaluated, evaluates every argument in argument order, applies {@code f} to their values and caches the result.
     * Nothing is evaluated before that, and {@code f} runs at most once. A {@code Lazy} may hold {@code null}, so
     * {@code f} may return it.
     *
     * @param l1  the first {@code Lazy}
     * @param l2  the second {@code Lazy}
     * @param f  combines the values
     * @param <T1> the value type of {@code l1}
     * @param <T2> the value type of {@code l2}
     * @param <R>  the result type
     * @return a new, unevaluated {@code Lazy} of the combined value
     * @throws NullPointerException if any argument is null
     */
    public static <T1 extends @Nullable Object, T2 extends @Nullable Object, R extends @Nullable Object> Lazy<R> zipWith(Lazy<? extends T1> l1, Lazy<? extends T2> l2, BiFunction<? super T1, ? super T2, ? extends R> f) {
        Objects.requireNonNull(l1, "l1 is null");
        Objects.requireNonNull(l2, "l2 is null");
        Objects.requireNonNull(f, "f is null");
        return Lazy.of(() -> f.apply(l1.get(), l2.get()));
    }

    /**
     * Pairs the values of three {@code Lazy} values: an unevaluated {@code Lazy} that, when first evaluated, evaluates
     * every argument in argument order and caches the tuple of their values. The same as
     * {@link #zipWith(Lazy, Lazy, Lazy, Function3)} with {@code Tuple::of}.
     *
     * @param l1  the first {@code Lazy}
     * @param l2  the second {@code Lazy}
     * @param l3  the third {@code Lazy}
     * @param <T1> the value type of {@code l1}
     * @param <T2> the value type of {@code l2}
     * @param <T3> the value type of {@code l3}
     * @return a new, unevaluated {@code Lazy} of the tuple of the values
     * @throws NullPointerException if any argument is null
     */
    public static <T1 extends @Nullable Object, T2 extends @Nullable Object, T3 extends @Nullable Object> Lazy<Tuple3<T1, T2, T3>> zip(Lazy<? extends T1> l1, Lazy<? extends T2> l2, Lazy<? extends T3> l3) {
        return zipWith(l1, l2, l3, Tuple::of);
    }

    /**
     * Combines the values of three {@code Lazy} values through {@code f}: an unevaluated {@code Lazy} that, when first
     * evaluated, evaluates every argument in argument order, applies {@code f} to their values and caches the result.
     * Nothing is evaluated before that, and {@code f} runs at most once. A {@code Lazy} may hold {@code null}, so
     * {@code f} may return it.
     *
     * @param l1  the first {@code Lazy}
     * @param l2  the second {@code Lazy}
     * @param l3  the third {@code Lazy}
     * @param f  combines the values
     * @param <T1> the value type of {@code l1}
     * @param <T2> the value type of {@code l2}
     * @param <T3> the value type of {@code l3}
     * @param <R>  the result type
     * @return a new, unevaluated {@code Lazy} of the combined value
     * @throws NullPointerException if any argument is null
     */
    public static <T1 extends @Nullable Object, T2 extends @Nullable Object, T3 extends @Nullable Object, R extends @Nullable Object> Lazy<R> zipWith(Lazy<? extends T1> l1, Lazy<? extends T2> l2, Lazy<? extends T3> l3, Function3<? super T1, ? super T2, ? super T3, ? extends R> f) {
        Objects.requireNonNull(l1, "l1 is null");
        Objects.requireNonNull(l2, "l2 is null");
        Objects.requireNonNull(l3, "l3 is null");
        Objects.requireNonNull(f, "f is null");
        return Lazy.of(() -> f.apply(l1.get(), l2.get(), l3.get()));
    }

    /**
     * Pairs the values of four {@code Lazy} values: an unevaluated {@code Lazy} that, when first evaluated, evaluates
     * every argument in argument order and caches the tuple of their values. The same as
     * {@link #zipWith(Lazy, Lazy, Lazy, Lazy, Function4)} with {@code Tuple::of}.
     *
     * @param l1  the first {@code Lazy}
     * @param l2  the second {@code Lazy}
     * @param l3  the third {@code Lazy}
     * @param l4  the fourth {@code Lazy}
     * @param <T1> the value type of {@code l1}
     * @param <T2> the value type of {@code l2}
     * @param <T3> the value type of {@code l3}
     * @param <T4> the value type of {@code l4}
     * @return a new, unevaluated {@code Lazy} of the tuple of the values
     * @throws NullPointerException if any argument is null
     */
    public static <T1 extends @Nullable Object, T2 extends @Nullable Object, T3 extends @Nullable Object, T4 extends @Nullable Object> Lazy<Tuple4<T1, T2, T3, T4>> zip(Lazy<? extends T1> l1, Lazy<? extends T2> l2, Lazy<? extends T3> l3, Lazy<? extends T4> l4) {
        return zipWith(l1, l2, l3, l4, Tuple::of);
    }

    /**
     * Combines the values of four {@code Lazy} values through {@code f}: an unevaluated {@code Lazy} that, when first
     * evaluated, evaluates every argument in argument order, applies {@code f} to their values and caches the result.
     * Nothing is evaluated before that, and {@code f} runs at most once. A {@code Lazy} may hold {@code null}, so
     * {@code f} may return it.
     *
     * @param l1  the first {@code Lazy}
     * @param l2  the second {@code Lazy}
     * @param l3  the third {@code Lazy}
     * @param l4  the fourth {@code Lazy}
     * @param f  combines the values
     * @param <T1> the value type of {@code l1}
     * @param <T2> the value type of {@code l2}
     * @param <T3> the value type of {@code l3}
     * @param <T4> the value type of {@code l4}
     * @param <R>  the result type
     * @return a new, unevaluated {@code Lazy} of the combined value
     * @throws NullPointerException if any argument is null
     */
    public static <T1 extends @Nullable Object, T2 extends @Nullable Object, T3 extends @Nullable Object, T4 extends @Nullable Object, R extends @Nullable Object> Lazy<R> zipWith(Lazy<? extends T1> l1, Lazy<? extends T2> l2, Lazy<? extends T3> l3, Lazy<? extends T4> l4, Function4<? super T1, ? super T2, ? super T3, ? super T4, ? extends R> f) {
        Objects.requireNonNull(l1, "l1 is null");
        Objects.requireNonNull(l2, "l2 is null");
        Objects.requireNonNull(l3, "l3 is null");
        Objects.requireNonNull(l4, "l4 is null");
        Objects.requireNonNull(f, "f is null");
        return Lazy.of(() -> f.apply(l1.get(), l2.get(), l3.get(), l4.get()));
    }

    /**
     * Pairs the values of five {@code Lazy} values: an unevaluated {@code Lazy} that, when first evaluated, evaluates
     * every argument in argument order and caches the tuple of their values. The same as
     * {@link #zipWith(Lazy, Lazy, Lazy, Lazy, Lazy, Function5)} with {@code Tuple::of}.
     *
     * @param l1  the first {@code Lazy}
     * @param l2  the second {@code Lazy}
     * @param l3  the third {@code Lazy}
     * @param l4  the fourth {@code Lazy}
     * @param l5  the fifth {@code Lazy}
     * @param <T1> the value type of {@code l1}
     * @param <T2> the value type of {@code l2}
     * @param <T3> the value type of {@code l3}
     * @param <T4> the value type of {@code l4}
     * @param <T5> the value type of {@code l5}
     * @return a new, unevaluated {@code Lazy} of the tuple of the values
     * @throws NullPointerException if any argument is null
     */
    public static <T1 extends @Nullable Object, T2 extends @Nullable Object, T3 extends @Nullable Object, T4 extends @Nullable Object, T5 extends @Nullable Object> Lazy<Tuple5<T1, T2, T3, T4, T5>> zip(Lazy<? extends T1> l1, Lazy<? extends T2> l2, Lazy<? extends T3> l3, Lazy<? extends T4> l4, Lazy<? extends T5> l5) {
        return zipWith(l1, l2, l3, l4, l5, Tuple::of);
    }

    /**
     * Combines the values of five {@code Lazy} values through {@code f}: an unevaluated {@code Lazy} that, when first
     * evaluated, evaluates every argument in argument order, applies {@code f} to their values and caches the result.
     * Nothing is evaluated before that, and {@code f} runs at most once. A {@code Lazy} may hold {@code null}, so
     * {@code f} may return it.
     *
     * @param l1  the first {@code Lazy}
     * @param l2  the second {@code Lazy}
     * @param l3  the third {@code Lazy}
     * @param l4  the fourth {@code Lazy}
     * @param l5  the fifth {@code Lazy}
     * @param f  combines the values
     * @param <T1> the value type of {@code l1}
     * @param <T2> the value type of {@code l2}
     * @param <T3> the value type of {@code l3}
     * @param <T4> the value type of {@code l4}
     * @param <T5> the value type of {@code l5}
     * @param <R>  the result type
     * @return a new, unevaluated {@code Lazy} of the combined value
     * @throws NullPointerException if any argument is null
     */
    public static <T1 extends @Nullable Object, T2 extends @Nullable Object, T3 extends @Nullable Object, T4 extends @Nullable Object, T5 extends @Nullable Object, R extends @Nullable Object> Lazy<R> zipWith(Lazy<? extends T1> l1, Lazy<? extends T2> l2, Lazy<? extends T3> l3, Lazy<? extends T4> l4, Lazy<? extends T5> l5, Function5<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? extends R> f) {
        Objects.requireNonNull(l1, "l1 is null");
        Objects.requireNonNull(l2, "l2 is null");
        Objects.requireNonNull(l3, "l3 is null");
        Objects.requireNonNull(l4, "l4 is null");
        Objects.requireNonNull(l5, "l5 is null");
        Objects.requireNonNull(f, "f is null");
        return Lazy.of(() -> f.apply(l1.get(), l2.get(), l3.get(), l4.get(), l5.get()));
    }

    /**
     * Pairs the values of six {@code Lazy} values: an unevaluated {@code Lazy} that, when first evaluated, evaluates
     * every argument in argument order and caches the tuple of their values. The same as
     * {@link #zipWith(Lazy, Lazy, Lazy, Lazy, Lazy, Lazy, Function6)} with {@code Tuple::of}.
     *
     * @param l1  the first {@code Lazy}
     * @param l2  the second {@code Lazy}
     * @param l3  the third {@code Lazy}
     * @param l4  the fourth {@code Lazy}
     * @param l5  the fifth {@code Lazy}
     * @param l6  the sixth {@code Lazy}
     * @param <T1> the value type of {@code l1}
     * @param <T2> the value type of {@code l2}
     * @param <T3> the value type of {@code l3}
     * @param <T4> the value type of {@code l4}
     * @param <T5> the value type of {@code l5}
     * @param <T6> the value type of {@code l6}
     * @return a new, unevaluated {@code Lazy} of the tuple of the values
     * @throws NullPointerException if any argument is null
     */
    public static <T1 extends @Nullable Object, T2 extends @Nullable Object, T3 extends @Nullable Object, T4 extends @Nullable Object, T5 extends @Nullable Object, T6 extends @Nullable Object> Lazy<Tuple6<T1, T2, T3, T4, T5, T6>> zip(Lazy<? extends T1> l1, Lazy<? extends T2> l2, Lazy<? extends T3> l3, Lazy<? extends T4> l4, Lazy<? extends T5> l5, Lazy<? extends T6> l6) {
        return zipWith(l1, l2, l3, l4, l5, l6, Tuple::of);
    }

    /**
     * Combines the values of six {@code Lazy} values through {@code f}: an unevaluated {@code Lazy} that, when first
     * evaluated, evaluates every argument in argument order, applies {@code f} to their values and caches the result.
     * Nothing is evaluated before that, and {@code f} runs at most once. A {@code Lazy} may hold {@code null}, so
     * {@code f} may return it.
     *
     * @param l1  the first {@code Lazy}
     * @param l2  the second {@code Lazy}
     * @param l3  the third {@code Lazy}
     * @param l4  the fourth {@code Lazy}
     * @param l5  the fifth {@code Lazy}
     * @param l6  the sixth {@code Lazy}
     * @param f  combines the values
     * @param <T1> the value type of {@code l1}
     * @param <T2> the value type of {@code l2}
     * @param <T3> the value type of {@code l3}
     * @param <T4> the value type of {@code l4}
     * @param <T5> the value type of {@code l5}
     * @param <T6> the value type of {@code l6}
     * @param <R>  the result type
     * @return a new, unevaluated {@code Lazy} of the combined value
     * @throws NullPointerException if any argument is null
     */
    public static <T1 extends @Nullable Object, T2 extends @Nullable Object, T3 extends @Nullable Object, T4 extends @Nullable Object, T5 extends @Nullable Object, T6 extends @Nullable Object, R extends @Nullable Object> Lazy<R> zipWith(Lazy<? extends T1> l1, Lazy<? extends T2> l2, Lazy<? extends T3> l3, Lazy<? extends T4> l4, Lazy<? extends T5> l5, Lazy<? extends T6> l6, Function6<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? super T6, ? extends R> f) {
        Objects.requireNonNull(l1, "l1 is null");
        Objects.requireNonNull(l2, "l2 is null");
        Objects.requireNonNull(l3, "l3 is null");
        Objects.requireNonNull(l4, "l4 is null");
        Objects.requireNonNull(l5, "l5 is null");
        Objects.requireNonNull(l6, "l6 is null");
        Objects.requireNonNull(f, "f is null");
        return Lazy.of(() -> f.apply(l1.get(), l2.get(), l3.get(), l4.get(), l5.get(), l6.get()));
    }

    /**
     * Pairs the values of seven {@code Lazy} values: an unevaluated {@code Lazy} that, when first evaluated, evaluates
     * every argument in argument order and caches the tuple of their values. The same as
     * {@link #zipWith(Lazy, Lazy, Lazy, Lazy, Lazy, Lazy, Lazy, Function7)} with {@code Tuple::of}.
     *
     * @param l1  the first {@code Lazy}
     * @param l2  the second {@code Lazy}
     * @param l3  the third {@code Lazy}
     * @param l4  the fourth {@code Lazy}
     * @param l5  the fifth {@code Lazy}
     * @param l6  the sixth {@code Lazy}
     * @param l7  the seventh {@code Lazy}
     * @param <T1> the value type of {@code l1}
     * @param <T2> the value type of {@code l2}
     * @param <T3> the value type of {@code l3}
     * @param <T4> the value type of {@code l4}
     * @param <T5> the value type of {@code l5}
     * @param <T6> the value type of {@code l6}
     * @param <T7> the value type of {@code l7}
     * @return a new, unevaluated {@code Lazy} of the tuple of the values
     * @throws NullPointerException if any argument is null
     */
    public static <T1 extends @Nullable Object, T2 extends @Nullable Object, T3 extends @Nullable Object, T4 extends @Nullable Object, T5 extends @Nullable Object, T6 extends @Nullable Object, T7 extends @Nullable Object> Lazy<Tuple7<T1, T2, T3, T4, T5, T6, T7>> zip(Lazy<? extends T1> l1, Lazy<? extends T2> l2, Lazy<? extends T3> l3, Lazy<? extends T4> l4, Lazy<? extends T5> l5, Lazy<? extends T6> l6, Lazy<? extends T7> l7) {
        return zipWith(l1, l2, l3, l4, l5, l6, l7, Tuple::of);
    }

    /**
     * Combines the values of seven {@code Lazy} values through {@code f}: an unevaluated {@code Lazy} that, when first
     * evaluated, evaluates every argument in argument order, applies {@code f} to their values and caches the result.
     * Nothing is evaluated before that, and {@code f} runs at most once. A {@code Lazy} may hold {@code null}, so
     * {@code f} may return it.
     *
     * @param l1  the first {@code Lazy}
     * @param l2  the second {@code Lazy}
     * @param l3  the third {@code Lazy}
     * @param l4  the fourth {@code Lazy}
     * @param l5  the fifth {@code Lazy}
     * @param l6  the sixth {@code Lazy}
     * @param l7  the seventh {@code Lazy}
     * @param f  combines the values
     * @param <T1> the value type of {@code l1}
     * @param <T2> the value type of {@code l2}
     * @param <T3> the value type of {@code l3}
     * @param <T4> the value type of {@code l4}
     * @param <T5> the value type of {@code l5}
     * @param <T6> the value type of {@code l6}
     * @param <T7> the value type of {@code l7}
     * @param <R>  the result type
     * @return a new, unevaluated {@code Lazy} of the combined value
     * @throws NullPointerException if any argument is null
     */
    public static <T1 extends @Nullable Object, T2 extends @Nullable Object, T3 extends @Nullable Object, T4 extends @Nullable Object, T5 extends @Nullable Object, T6 extends @Nullable Object, T7 extends @Nullable Object, R extends @Nullable Object> Lazy<R> zipWith(Lazy<? extends T1> l1, Lazy<? extends T2> l2, Lazy<? extends T3> l3, Lazy<? extends T4> l4, Lazy<? extends T5> l5, Lazy<? extends T6> l6, Lazy<? extends T7> l7, Function7<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? super T6, ? super T7, ? extends R> f) {
        Objects.requireNonNull(l1, "l1 is null");
        Objects.requireNonNull(l2, "l2 is null");
        Objects.requireNonNull(l3, "l3 is null");
        Objects.requireNonNull(l4, "l4 is null");
        Objects.requireNonNull(l5, "l5 is null");
        Objects.requireNonNull(l6, "l6 is null");
        Objects.requireNonNull(l7, "l7 is null");
        Objects.requireNonNull(f, "f is null");
        return Lazy.of(() -> f.apply(l1.get(), l2.get(), l3.get(), l4.get(), l5.get(), l6.get(), l7.get()));
    }

    /**
     * Pairs the values of eight {@code Lazy} values: an unevaluated {@code Lazy} that, when first evaluated, evaluates
     * every argument in argument order and caches the tuple of their values. The same as
     * {@link #zipWith(Lazy, Lazy, Lazy, Lazy, Lazy, Lazy, Lazy, Lazy, Function8)} with {@code Tuple::of}.
     *
     * @param l1  the first {@code Lazy}
     * @param l2  the second {@code Lazy}
     * @param l3  the third {@code Lazy}
     * @param l4  the fourth {@code Lazy}
     * @param l5  the fifth {@code Lazy}
     * @param l6  the sixth {@code Lazy}
     * @param l7  the seventh {@code Lazy}
     * @param l8  the eighth {@code Lazy}
     * @param <T1> the value type of {@code l1}
     * @param <T2> the value type of {@code l2}
     * @param <T3> the value type of {@code l3}
     * @param <T4> the value type of {@code l4}
     * @param <T5> the value type of {@code l5}
     * @param <T6> the value type of {@code l6}
     * @param <T7> the value type of {@code l7}
     * @param <T8> the value type of {@code l8}
     * @return a new, unevaluated {@code Lazy} of the tuple of the values
     * @throws NullPointerException if any argument is null
     */
    public static <T1 extends @Nullable Object, T2 extends @Nullable Object, T3 extends @Nullable Object, T4 extends @Nullable Object, T5 extends @Nullable Object, T6 extends @Nullable Object, T7 extends @Nullable Object, T8 extends @Nullable Object> Lazy<Tuple8<T1, T2, T3, T4, T5, T6, T7, T8>> zip(Lazy<? extends T1> l1, Lazy<? extends T2> l2, Lazy<? extends T3> l3, Lazy<? extends T4> l4, Lazy<? extends T5> l5, Lazy<? extends T6> l6, Lazy<? extends T7> l7, Lazy<? extends T8> l8) {
        return zipWith(l1, l2, l3, l4, l5, l6, l7, l8, Tuple::of);
    }

    /**
     * Combines the values of eight {@code Lazy} values through {@code f}: an unevaluated {@code Lazy} that, when first
     * evaluated, evaluates every argument in argument order, applies {@code f} to their values and caches the result.
     * Nothing is evaluated before that, and {@code f} runs at most once. A {@code Lazy} may hold {@code null}, so
     * {@code f} may return it.
     *
     * @param l1  the first {@code Lazy}
     * @param l2  the second {@code Lazy}
     * @param l3  the third {@code Lazy}
     * @param l4  the fourth {@code Lazy}
     * @param l5  the fifth {@code Lazy}
     * @param l6  the sixth {@code Lazy}
     * @param l7  the seventh {@code Lazy}
     * @param l8  the eighth {@code Lazy}
     * @param f  combines the values
     * @param <T1> the value type of {@code l1}
     * @param <T2> the value type of {@code l2}
     * @param <T3> the value type of {@code l3}
     * @param <T4> the value type of {@code l4}
     * @param <T5> the value type of {@code l5}
     * @param <T6> the value type of {@code l6}
     * @param <T7> the value type of {@code l7}
     * @param <T8> the value type of {@code l8}
     * @param <R>  the result type
     * @return a new, unevaluated {@code Lazy} of the combined value
     * @throws NullPointerException if any argument is null
     */
    public static <T1 extends @Nullable Object, T2 extends @Nullable Object, T3 extends @Nullable Object, T4 extends @Nullable Object, T5 extends @Nullable Object, T6 extends @Nullable Object, T7 extends @Nullable Object, T8 extends @Nullable Object, R extends @Nullable Object> Lazy<R> zipWith(Lazy<? extends T1> l1, Lazy<? extends T2> l2, Lazy<? extends T3> l3, Lazy<? extends T4> l4, Lazy<? extends T5> l5, Lazy<? extends T6> l6, Lazy<? extends T7> l7, Lazy<? extends T8> l8, Function8<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? super T6, ? super T7, ? super T8, ? extends R> f) {
        Objects.requireNonNull(l1, "l1 is null");
        Objects.requireNonNull(l2, "l2 is null");
        Objects.requireNonNull(l3, "l3 is null");
        Objects.requireNonNull(l4, "l4 is null");
        Objects.requireNonNull(l5, "l5 is null");
        Objects.requireNonNull(l6, "l6 is null");
        Objects.requireNonNull(l7, "l7 is null");
        Objects.requireNonNull(l8, "l8 is null");
        Objects.requireNonNull(f, "f is null");
        return Lazy.of(() -> f.apply(l1.get(), l2.get(), l3.get(), l4.get(), l5.get(), l6.get(), l7.get(), l8.get()));
    }

    /**
     * Evaluates this {@code Lazy}, runs {@code action} on its value and returns this instance. Whatever the action
     * throws propagates to the caller.
     *
     * @param action what to do with the value
     * @return this instance
     * @throws NullPointerException if {@code action} is null
     */
    public Lazy<T> tap(Consumer<? super T> action) {
        Objects.requireNonNull(action, "action is null");
        action.accept(get());
        return this;
    }

    /**
     * Views this {@code Lazy} as a {@link Supplier}. The supplier delegates to {@link #get()}, so it shares this
     * instance's memoization: the computation still runs at most once.
     *
     * @return a {@code Supplier} of this value
     */
    public Supplier<T> toSupplier() {
        return this::get;
    }

    @Override
    public boolean equals(@Nullable Object o) {
        return (o == this) || (o instanceof Lazy && Objects.equals(((Lazy<?>) o).get(), get()));
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(get());
    }

    @Override
    public String toString() {
        return "Lazy(" + (!isEvaluated() ? "?" : value) + ")";
    }

}
