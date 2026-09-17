package com.guizmaii.zazr;

import com.guizmaii.zazr.collection.Seq;
import com.guizmaii.zazr.collection.Vector;
import java.util.Objects;
import java.util.concurrent.locks.ReentrantLock;
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
     * Combines multiple {@code Lazy} instances into a single {@code Lazy} containing a sequence of their evaluated values.
     *
     * <p>Transforms an {@code Iterable<Lazy<? extends T>>} into a {@code Lazy<Seq<T>>}, evaluating each value lazily
     * when the resulting {@code Lazy} is accessed.</p>
     *
     * @param <T>    the type of the lazy values
     * @param values an {@code Iterable} of lazy values
     * @return a {@code Lazy} containing a sequence of the evaluated values
     * @throws NullPointerException if {@code values} is null
     */
    public static <T extends @Nullable Object> Lazy<Seq<T>> sequence(Iterable<? extends Lazy<? extends T>> values) {
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

    /**
     * Evaluates this {@code Lazy} and performs the given {@code action} on its value.
     *
     * @param action the action performed on the value
     * @return this instance
     * @throws NullPointerException if {@code action} is null
     */
    public Lazy<T> peek(Consumer<? super T> action) {
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
