package com.guizmaii.zazr.control;

import com.guizmaii.zazr.*;
import com.guizmaii.zazr.collection.Vector;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import org.jspecify.annotations.Nullable;

import static com.guizmaii.zazr.internal.Throwables.isFatal;
import static com.guizmaii.zazr.internal.Throwables.sneakyThrow;

/**
 * A control structure that allows writing safe code without explicitly managing try-catch blocks for exceptions.
 * <p>
 * {@code Try} is a sealed interface with two record cases, {@link Success} and {@link Failure}, so it is
 * eliminated with an exhaustive {@code switch}:
 * <pre>{@code
 * String s = switch (Try.of(() -> parse(input))) {
 *     case Success(var value) -> "parsed " + value;
 *     case Failure(var cause) -> "failed: " + cause.getMessage();
 * };
 * }</pre>
 * A {@code Success} never holds {@code null}: {@link #success(Object)} throws, and a computation that returns
 * {@code null} under {@link #of(Callable)}, {@link #mapTry(CheckedFunction1)} or
 * {@link #fromCompletableFuture(CompletableFuture)} is captured, like any other non-fatal outcome, as a
 * {@code Failure} of a {@link NullPointerException}. A computation that returns nothing is run with
 * {@link #run(CheckedRunnable)}, whose success value is the empty tuple {@link Tuple0}. Two {@code Failure}s are equal only when they hold the same
 * {@code Throwable} instance, see {@link Failure}.
 * <p>
 * A {@code Try} is not a collection and not {@link Iterable} (design 3.2): to iterate its value, convert it
 * explicitly with {@link #toVector()} or {@link #toOption()}.
 * <p>
 * The following exceptions are considered fatal or non-recoverable:
 * <ul>
 *     <li>{@linkplain InterruptedException}</li>
 *     <li>{@linkplain LinkageError}</li>
 *     <li>{@linkplain ThreadDeath}</li>
 *     <li>{@linkplain VirtualMachineError}, including {@linkplain OutOfMemoryError} and {@linkplain StackOverflowError}</li>
 * </ul>
 * <p>
 * These fatal throwables are never captured in a {@link Failure}: every factory method and combinator below that
 * would otherwise wrap a thrown exception instead rethrows a fatal one sneakily (i.e. without declaring it).
 * <p>
 * <strong>Note:</strong> Methods such as {@code get()} may re-throw exceptions without declaring them. When used
 * within a {@link java.lang.reflect.InvocationHandler} of a dynamic proxy, such exceptions will be wrapped in
 * {@link java.lang.reflect.UndeclaredThrowableException}. See 
 * <a href="https://docs.oracle.com/javase/8/docs/technotes/guides/reflection/proxy.html">
 * Dynamic Proxy Classes</a> for more details.
 *
 * @param <T> the type of the value in case of success
 * @author Daniel
 */
public sealed interface Try<T extends @Nullable Object> permits Try.Success, Try.Failure {

    /**
     * Creates a {@link Try} instance from a {@link Callable}.
     * <p>
     * If the callable executes without throwing an exception, a {@link Success} containing the result is returned.
     * If a non-fatal exception occurs during execution, a {@link Failure} wrapping the thrown exception is returned;
     * fatal throwables (see the class-level documentation) are rethrown instead. A {@code Success} cannot hold
     * {@code null}, so a {@code null} result is captured as a {@link Failure} of a {@link NullPointerException}:
     * every non-fatal outcome of the computation ends up in the returned {@code Try}.
     *
     * @param supplier the callable to execute
     * @param <T>      the type of the value returned by the callable
     * @return a {@link Success} with the callable's result, or a {@link Failure} if an exception is thrown or the
     *         result is {@code null}
     * @throws NullPointerException if {@code supplier} is {@code null}
     */
    static <T extends @Nullable Object> Try<T> of(Callable<? extends T> supplier) {
        Objects.requireNonNull(supplier, "supplier is null");
        try {
            final T value = supplier.call();
            return value == null ? TryModule.nullResult("Try.of") : new Success<>(value);
        } catch (Throwable t) {
            return new Failure<>(t);
        }
    }

    /**
     * Creates a {@link Try} instance from a {@link CheckedRunnable}.
     * <p>
     * If the runnable executes without throwing an exception, a {@link Success} containing the empty tuple
     * {@link Tuple0} is returned: a {@code Success} cannot hold {@code null} and Java has no unit type, so the
     * empty tuple {@code ()} plays that role, as it does in Scala. If a non-fatal exception occurs during execution,
     * a {@link Failure} wrapping the thrown exception is returned; fatal throwables (see the class-level
     * documentation) are rethrown instead.
     *
     * @param runnable the checked runnable to execute
     * @return {@code Success(())} if the runnable completes successfully, or a {@link Failure} if an exception is thrown
     * @throws NullPointerException if {@code runnable} is {@code null}
     */
    static Try<Tuple0> run(CheckedRunnable runnable) {
        Objects.requireNonNull(runnable, "runnable is null");
        try {
            runnable.run();
        } catch (Throwable t) {
            return new Failure<>(t);
        }
        return new Success<>(Tuple.empty());
    }

    /**
     * Turns many {@code Try}s into one {@code Try} of all their values: {@code Success} of a {@link Vector} of the
     * values in iteration order when every element is a {@code Success}, otherwise the first {@code Failure} in
     * iteration order. The empty iterable gives {@code Success} of the empty {@code Vector}.
     * <pre>{@code
     * Try.collectAll(List.of(Try.success(1), Try.success(2))); // = Success(Vector(1, 2))
     * Try.collectAll(List.of(Try.success(1), Try.failure(e))); // = Failure(e)
     * }</pre>
     *
     * @param values the {@code Try}s to collect
     * @param <T>    the value type
     * @return {@code Success} of all the values, or the first {@code Failure}
     * @throws NullPointerException if {@code values} is null
     */
    static <T extends @Nullable Object> Try<Vector<T>> collectAll(Iterable<? extends Try<? extends T>> values) {
        Objects.requireNonNull(values, "values is null");
        final Vector.Builder<T> builder = Vector.newBuilder();
        for (Try<? extends T> value : values) {
            if (value.isFailure()) {
                return Try.failure(value.getCause());
            }
            builder.add(value.get());
        }
        return Try.success(builder.result());
    }

    /**
     * Applies {@code mapper} to every element and collects the results as {@link #collectAll(Iterable)} does:
     * {@code Success} of a {@link Vector} of the mapped values when every call returns a {@code Success}, otherwise
     * the first {@code Failure}. The mapper is not called for the elements after that one; what it throws
     * propagates to the caller, since it is a plain {@link Function}: build the {@code Try} inside it with
     * {@link #of(Callable)}.
     * <pre>{@code
     * Try.forEach(List.of("1", "2"), s -> Try.of(() -> Integer.parseInt(s))); // = Success(Vector(1, 2))
     * }</pre>
     *
     * @param values the elements to map
     * @param mapper a function from an element to a {@code Try}; it must not return {@code null}
     * @param <T>    the element type
     * @param <U>    the mapped value type
     * @return {@code Success} of all the mapped values, or the first {@code Failure}
     * @throws NullPointerException if {@code values} or {@code mapper} is null
     */
    static <T extends @Nullable Object, U extends @Nullable Object> Try<Vector<U>> forEach(Iterable<? extends T> values, Function<? super T, ? extends Try<? extends U>> mapper) {
        Objects.requireNonNull(values, "values is null");
        Objects.requireNonNull(mapper, "mapper is null");
        final Vector.Builder<U> builder = Vector.newBuilder();
        for (T value : values) {
            final Try<? extends U> mapped = Objects.requireNonNull(mapper.apply(value), "Try.forEach: mapper returned null");
            if (mapped.isFailure()) {
                return Try.failure(mapped.getCause());
            }
            builder.add(mapped.get());
        }
        return Try.success(builder.result());
    }

    /**
     * Creates a {@link Success} containing the given {@code value}.
     * <p>
     * This is a convenience method equivalent to {@code new Success<>(value)}.
     *
     * @param value the value to wrap in a {@link Success}, must not be {@code null}
     * @param <T>   the type of the value
     * @return a new {@link Success} containing {@code value}
     * @throws NullPointerException if {@code value} is {@code null}
     */
    static <T extends @Nullable Object> Try<T> success(T value) {
        return new Success<>(value);
    }

    /**
     * Creates a {@link Failure} containing the given {@code exception}.
     * <p>
     * This is a convenience method equivalent to {@code new Failure<>(exception)}. If {@code exception} is fatal
     * (see the class-level documentation), it is rethrown sneakily instead of being wrapped.
     *
     * @param exception the exception to wrap in a {@link Failure}
     * @param <T>       the component type of the {@code Try}
     * @return a new {@link Failure} containing {@code exception}
     * @throws NullPointerException if {@code exception} is {@code null}
     */
    static <T extends @Nullable Object> Try<T> failure(Throwable exception) {
        return new Failure<>(exception);
    }

    /**
     * Narrows a {@code Try<? extends T>} to {@code Try<T>} using a type-safe cast.
     * <p>
     * This is safe because {@code Try} is immutable and its contents are read-only, allowing covariance.
     *
     * @param t   the {@code Try} instance to narrow
     * @param <T> the component type of the {@code Try}
     * @return the given {@code Try} instance as {@code Try<T>}
     */
    @SuppressWarnings("unchecked")
    static <T extends @Nullable Object> Try<T> narrow(Try<? extends T> t) {
        return (Try<T>) t;
    }

    /**
     * Creates a {@link Try} from a {@link CompletableFuture}, joining it on the calling thread.
     * <p>
     * If the future completes normally, a {@link Success} containing its result is returned. If the future
     * completed exceptionally, a {@link Failure} wrapping the cause is returned, with the wrapping
     * {@link CompletionException} unwrapped to expose the original cause. If the future was cancelled, a
     * {@link Failure} wrapping a {@link CancellationException} is returned. A fatal throwable (see the
     * class-level documentation) is rethrown instead of being wrapped.
     *
     * @param future the future to join
     * @param <T>    the type of the future's result
     * @return a {@link Success} with the future's result, or a {@link Failure} describing why it did not complete
     * A future completed with {@code null} (a {@code CompletableFuture<Void>}, typically) yields a {@link Failure}
     * of a {@link NullPointerException}, since a {@code Success} cannot hold {@code null}; map it to a value first.
     *
     * @param future the future to join
     * @param <T>    the type of the future's result
     * @return a {@link Success} with the future's result, or a {@link Failure} describing why it did not complete
     *         or that it completed with {@code null}
     * @throws NullPointerException if {@code future} is {@code null}
     */
    static <T extends @Nullable Object> Try<T> fromCompletableFuture(CompletableFuture<? extends T> future) {
        Objects.requireNonNull(future, "future is null");
        final T value;
        try {
            value = future.join();
        } catch (CancellationException e) {
            return new Failure<>(e);
        } catch (CompletionException e) {
            final Throwable cause = e.getCause();
            return new Failure<>(cause != null ? cause : e);
        }
        return value == null ? TryModule.nullResult("Try.fromCompletableFuture") : new Success<>(value);
    }

    /**
     * Passes the result of this {@code Try} to the given {@link CheckedConsumer} if this is a {@link Success}.
     * <p>
     * This allows chaining of operations that may throw checked exceptions. If this {@code Try} is a {@link Failure},
     * it is returned unchanged. If the consumer throws a non-fatal exception, a {@link Failure} containing that
     * exception is returned; fatal throwables (see the class-level documentation) are rethrown instead.
     * <p>
     * Example usage:
     * <pre>{@code
     * Try.of(() -> 100)
     *    .andThenTry(i -> System.out.println(i));
     * }</pre>
     *
     * @param consumer the checked consumer to execute on the value
     * @return this {@code Try} if it is a {@link Failure} or the consumer succeeds, otherwise a {@link Failure} of the consumer
     * @throws NullPointerException if {@code consumer} is {@code null}
     */
    default Try<T> andThenTry(CheckedConsumer<? super T> consumer) {
        Objects.requireNonNull(consumer, "consumer is null");
        if (isFailure()) {
            return this;
        } else {
            try {
                consumer.accept(get());
                return this;
            } catch (Throwable t) {
                return new Failure<>(t);
            }
        }
    }

    /**
     * Performs the given {@link Runnable} if this {@code Try} is a {@link Success}.
     * <p>
     * This is a shortcut for {@code andThenTry(runnable::run)}. If this {@code Try} is a {@link Failure}, it is returned unchanged.
     * If the runnable throws a non-fatal exception, a {@link Failure} containing that exception is returned;
     * fatal throwables (see the class-level documentation) are rethrown instead.
     *
     * @param runnable the runnable to execute
     * @return this {@code Try} if it is a {@link Failure} or the runnable succeeds, otherwise a {@link Failure} of the runnable
     * @throws NullPointerException if {@code runnable} is {@code null}
     * @see #andThenTry(CheckedRunnable)
     */
    default Try<T> andThen(Runnable runnable) {
        Objects.requireNonNull(runnable, "runnable is null");
        return andThenTry(runnable::run);
    }

    /**
     * Executes the given {@link CheckedRunnable} if this {@code Try} is a {@link Success}; 
     * otherwise, returns this {@code Failure}.
     * <p>
     * This allows chaining of runnables that may throw checked exceptions. If the runnable throws a non-fatal
     * exception, a {@link Failure} containing that exception is returned; fatal throwables (see the class-level
     * documentation) are rethrown instead.
     * <p>
     * Example usage with method references:
     * <pre>{@code
     * Try.run(A::methodRef)
     *    .andThen(B::methodRef)
     *    .andThen(C::methodRef);
     * }</pre>
     *
     * The following two forms are semantically equivalent:
     * <pre>{@code
     * Try.run(this::doStuff)
     *    .andThen(this::doMoreStuff)
     *    .andThen(this::doEvenMoreStuff);
     *
     * Try.run(() -> {
     *     doStuff();
     *     doMoreStuff();
     *     doEvenMoreStuff();
     * });
     * }</pre>
     *
     * @param runnable the checked runnable to execute
     * @return this {@code Try} if it is a {@link Failure} or the runnable succeeds, otherwise a {@link Failure} of the runnable
     * @throws NullPointerException if {@code runnable} is {@code null}
     */
    default Try<T> andThenTry(CheckedRunnable runnable) {
        Objects.requireNonNull(runnable, "runnable is null");
        if (isFailure()) {
            return this;
        } else {
            try {
                runnable.run();
                return this;
            } catch (Throwable t) {
                return new Failure<>(t);
            }
        }
    }

    /**
     * Keeps the value if {@code predicate} holds for it, otherwise fails with the throwable {@code ifFalse} builds
     * from it. A {@code Failure} is returned unchanged and the predicate is not evaluated. The predicate and
     * {@code ifFalse} run under {@code Try}: a non-fatal exception thrown by either becomes the {@code Failure}.
     * <pre>{@code
     * Try.success(3).filter(i -> i > 0, i -> new IllegalArgumentException("not positive: " + i)); // = Success(3)
     * Try.success(-1).filter(i -> i > 0, i -> new IllegalArgumentException("not positive: " + i)); // = Failure(IllegalArgumentException: not positive: -1)
     * }</pre>
     *
     * @param predicate the condition the value has to satisfy
     * @param ifFalse   builds the failure cause from the rejected value; it must not return {@code null} nor a
     *                  fatal throwable
     * @return this {@code Try} if it is a {@code Failure} or the predicate holds, otherwise a {@code Failure}
     * @throws NullPointerException if {@code predicate} or {@code ifFalse} is null
     */
    default Try<T> filter(Predicate<? super T> predicate, Function<? super T, ? extends Throwable> ifFalse) {
        Objects.requireNonNull(predicate, "predicate is null");
        Objects.requireNonNull(ifFalse, "ifFalse is null");
        if (isFailure()) {
            return this;
        }
        try {
            final T value = get();
            return predicate.test(value) ? this : new Failure<>(ifFalse.apply(value));
        } catch (Throwable t) {
            return new Failure<>(t);
        }
    }

    /**
     * Keeps the value if {@code predicate} holds for it, otherwise fails with a {@link NoSuchElementException}.
     * See {@link #filter(Predicate, Function)} to choose the failure cause.
     *
     * @param predicate the condition the value has to satisfy
     * @return this {@code Try} if it is a {@code Failure} or the predicate holds, otherwise a {@code Failure} of a
     *         {@code NoSuchElementException}
     * @throws NullPointerException if {@code predicate} is null
     */
    default Try<T> filter(Predicate<? super T> predicate) {
        Objects.requireNonNull(predicate, "predicate is null");
        return filter(predicate, value -> new NoSuchElementException("Predicate does not hold for " + value));
    }

    /**
     * Transforms the value of this {@code Try} using the given {@link Function} if it is a {@link Success},
     * or returns this {@link Failure}.
     * <p>
     * This is a shortcut for {@link #flatMapTry(CheckedFunction1)}.
     * <p>
     * The mapper must return a {@code Try}, never {@code null}. A {@code Try} built inside the mapper captures a {@code null} computation result as a {@code Failure}, see {@link #of(Callable)}.
     *
     * @param mapper a function mapping the value to another {@code Try}
     * @param <U>    the type of the resulting {@code Try}
     * @return a new {@code Try} resulting from applying the mapper, or this {@code Failure} if this is a failure
     * @throws NullPointerException if {@code mapper} is {@code null}
     */
    default <U extends @Nullable Object> Try<U> flatMap(Function<? super T, ? extends Try<? extends U>> mapper) {
        Objects.requireNonNull(mapper, "mapper is null");
        return flatMapTry((CheckedFunction1<T, Try<? extends U>>) mapper::apply);
    }

    /**
     * Transforms the value of this {@code Try} using the given {@link CheckedFunction1} if it is a {@link Success},
     * or returns this {@link Failure}.
     * <p>
     * If applying the mapper throws a non-fatal exception, a {@link Failure} containing the exception is returned;
     * fatal throwables (see the class-level documentation) are rethrown instead.
     *
     * @param mapper a checked function mapping the value to another {@code Try}
     * @param <U>    the type of the resulting {@code Try}
     * @return a new {@code Try} resulting from applying the mapper, or this {@code Failure} if this is a failure
     * @throws NullPointerException if {@code mapper} is {@code null}
     */
    @SuppressWarnings("unchecked")
    default <U extends @Nullable Object> Try<U> flatMapTry(CheckedFunction1<? super T, ? extends Try<? extends U>> mapper) {
        Objects.requireNonNull(mapper, "mapper is null");
        if (isFailure()) {
            return (Failure<U>) this;
        } else {
            try {
                return (Try<U>) mapper.apply(get());
            } catch (Throwable t) {
                return new Failure<>(t);
            }
        }
    }

    /**
     * Matches and transforms the value in one step: {@code mapper} returns {@code Some} of the new value for a
     * value it accepts and {@code None} for one it rejects, which fails this {@code Try} with a
     * {@link NoSuchElementException}. The {@code case} ergonomics come from a {@code switch} inside the lambda:
     * <pre>{@code
     * Try<Double> radius = shape.collect(s -> switch (s) {
     *     case Circle c -> Option.some(c.radius());
     *     default -> Option.none();
     * });
     * }</pre>
     * The mapper runs under {@code Try}, as {@link #map(Function)} does: a non-fatal exception it throws, or a
     * {@code null} it returns instead of an {@code Option}, becomes the {@code Failure}. A {@code Failure} is
     * returned unchanged and the mapper is not called.
     *
     * @param mapper a function from the value to {@code Some} of its replacement or {@code None}
     * @param <U>    the type of the collected value
     * @return {@code Success} of the collected value, a {@code Failure} of a {@code NoSuchElementException} if the
     *         mapper returned {@code None}, or this {@code Failure}
     * @throws NullPointerException if {@code mapper} is null
     */
    @SuppressWarnings("unchecked")
    default <U extends @Nullable Object> Try<U> collect(Function<? super T, ? extends Option<? extends U>> mapper) {
        Objects.requireNonNull(mapper, "mapper is null");
        if (isFailure()) {
            return (Failure<U>) this;
        }
        try {
            final T value = get();
            final Option<? extends U> collected = Objects.requireNonNull(mapper.apply(value), "Try.collect: mapper returned null");
            return collected.isDefined()
              ? new Success<>(collected.get())
              : new Failure<>(new NoSuchElementException("Predicate does not hold for " + value));
        } catch (Throwable t) {
            return new Failure<>(t);
        }
    }

    /**
     * Returns the value of this {@code Try} if it is a {@link Success}, or throws the underlying exception if it is a {@link Failure}.
     * <p>
     * <strong>Important:</strong> If this {@code Try} is a {@link Failure}, the exception thrown is exactly the
     * {@link #getCause()} of this {@code Failure}.
     *
     * @return the value contained in this {@code Success}
     * throws Throwable the underlying cause sneakily if this is a {@link Failure}
     */
    T get();

    /**
     * Returns the cause of failure if this {@code Try} is a {@link Failure}.
     *
     * @return the throwable cause of this {@link Failure}
     * @throws UnsupportedOperationException if this {@code Try} is a {@link Success}
     */
    Throwable getCause();

    /**
     * Checks whether this {@code Try} contains no value, i.e., it is a {@link Failure}.
     *
     * @return {@code true} if this is a {@link Failure}, {@code false} if this is a {@link Success}
     */
    boolean isEmpty();

    /**
     * Checks whether this {@code Try} is a {@link Failure}.
     *
     * @return {@code true} if this is a {@link Failure}, {@code false} if this is a {@link Success}
     */
    boolean isFailure();

    /**
     * Checks whether this {@code Try} is a {@link Success}.
     *
     * @return {@code true} if this is a {@link Success}, {@code false} if this is a {@link Failure}
     */
    boolean isSuccess();

    /**
     * Shortcut for {@code mapTry(mapper::apply)}, see {@link #mapTry(CheckedFunction1)}.
     * <p>
     * A mapper that returns {@code null} yields a {@code Failure} of a {@link NullPointerException}, since {@code Success} cannot hold {@code null}; see {@link #mapTry(CheckedFunction1)}.
     *
     * @param <U>    The new component type
     * @param mapper a function to apply to the value
     * @return a {@code Try}
     * @throws NullPointerException if {@code mapper} is null
     */
    default <U extends @Nullable Object> Try<U> map(Function<? super T, ? extends U> mapper) {
        Objects.requireNonNull(mapper, "mapper is null");
        return mapTry(mapper::apply);
    }

    /**
     * Applies the given checked function to the value of this {@link Success}, or returns this {@link Failure} unchanged.
     * <p>
     * If the function throws a non-fatal exception, a new {@link Failure} containing the exception is returned;
     * fatal throwables (see the class-level documentation) are rethrown instead.
     * This allows chaining of computations that may throw checked exceptions.
     * <p>
     * Example:
     * <pre>{@code
     * Try.of(() -> 0)
     *    .mapTry(x -> 1 / x); // division by zero will result in a Failure
     * }</pre>
     *
     * A {@code null} result is captured as a {@link Failure} of a {@link NullPointerException}, since a
     * {@code Success} cannot hold {@code null}.
     *
     * @param <U>    the type of the result
     * @param mapper a checked function to apply to the value
     * @return a new {@code Try} containing the mapped value if this is a {@link Success}, otherwise this {@link Failure}
     * @throws NullPointerException if {@code mapper} is {@code null}
     */
    @SuppressWarnings("unchecked")
    default <U extends @Nullable Object> Try<U> mapTry(CheckedFunction1<? super T, ? extends U> mapper) {
        Objects.requireNonNull(mapper, "mapper is null");
        if (isFailure()) {
            return (Failure<U>) this;
        } else {
            try {
                final U value = mapper.apply(get());
                return value == null ? TryModule.nullResult("Try.mapTry") : new Success<>(value);
            } catch (Throwable t) {
                return new Failure<>(t);
            }
        }
    }

    /**
     * Runs {@code action} on the cause if this is a {@code Failure} and returns this {@code Try} unchanged; does
     * nothing for a {@code Success}. The counterpart of {@link #tap(Consumer)}: what the action throws propagates
     * to the caller.
     * <pre>{@code
     * Try.failure(new IOException("disk")).tapError(e -> log.warn("failed", e)); // logs, stays the same Failure
     * Try.success(1).tapError(e -> log.warn("failed", e));                       // does nothing
     * }</pre>
     *
     * @param action what to do with the cause
     * @return this {@code Try}
     * @throws NullPointerException if {@code action} is null
     */
    default Try<T> tapError(Consumer<? super Throwable> action) {
        Objects.requireNonNull(action, "action is null");
        if (isFailure()) {
            action.accept(getCause());
        }
        return this;
    }

    /**
     * Runs {@code action} on the cause if this is a {@code Failure} whose cause is an instance of
     * {@code exceptionType}, and returns this {@code Try} unchanged; does nothing otherwise.
     * <pre>{@code
     * Try.failure(new IOException("disk"))
     *    .tapError(IOException.class, e -> log.warn("io", e))        // runs
     *    .tapError(IllegalStateException.class, e -> log.warn("?")); // does not run
     * }</pre>
     *
     * @param exceptionType the type the cause has to be an instance of
     * @param action        what to do with the cause
     * @param <X>           the type of the cause
     * @return this {@code Try}
     * @throws NullPointerException if {@code exceptionType} or {@code action} is null
     */
    @SuppressWarnings("unchecked")
    default <X extends Throwable> Try<T> tapError(Class<X> exceptionType, Consumer<? super X> action) {
        Objects.requireNonNull(exceptionType, "exceptionType is null");
        Objects.requireNonNull(action, "action is null");
        if (isFailure() && exceptionType.isInstance(getCause())) {
            action.accept((X) getCause());
        }
        return this;
    }

    /**
     * Returns this {@code Try} if it is a {@link Success}, or the given alternative {@code Try} if this is a {@link Failure}.
     *
     * @param other the alternative {@code Try} to return if this is a {@link Failure}
     * @return this {@code Try} if success, otherwise {@code other}
     * @throws NullPointerException if {@code other} is null
     */
    @SuppressWarnings("unchecked")
    default Try<T> orElse(Try<? extends T> other) {
        Objects.requireNonNull(other, "other is null");
        return isSuccess() ? this : (Try<T>) other;
    }

    /**
     * Returns this {@code Try} if it is a {@link Success}, or a {@code Try} supplied by the given {@link Supplier} if this is a {@link Failure}.
     * <p>
     * The supplier is only invoked if this {@code Try} is a {@link Failure}.
     *
     * @param supplier a supplier of an alternative {@code Try}
     * @return this {@code Try} if success, otherwise the {@code Try} returned by {@code supplier}
     * @throws NullPointerException if {@code supplier} is null
     */
    @SuppressWarnings("unchecked")
    default Try<T> orElse(Supplier<? extends Try<? extends T>> supplier) {
        Objects.requireNonNull(supplier, "supplier is null");
        return isSuccess() ? this : (Try<T>) supplier.get();
    }

    /**
     * Returns the value of this {@code Success}, or {@code other} if this is a {@link Failure}.
     * <p>
     * Note that {@code other} is evaluated eagerly.
     *
     * @param other an alternative value
     * @return the value of this {@link Success}, otherwise {@code other}
     */
    default T getOrElse(T other) {
        return isSuccess() ? get() : other;
    }

    /**
     * Returns the value of this {@code Success}, or the value supplied by {@code supplier} if this is a {@link Failure}.
     *
     * @param supplier a supplier of an alternative value, invoked only for a {@code Failure}
     * @return the value of this {@link Success}, otherwise the supplied value
     * @throws NullPointerException if {@code supplier} is null
     */
    default T getOrElse(Supplier<? extends T> supplier) {
        Objects.requireNonNull(supplier, "supplier is null");
        return isSuccess() ? get() : supplier.get();
    }

    /**
     * Returns the value of this {@code Success}, or {@code null} if this is a {@link Failure}.
     *
     * @return the value of this {@link Success}, otherwise {@code null}
     */
    default @Nullable T getOrNull() {
        return isSuccess() ? get() : null;
    }

    /**
     * Returns the value, or the value {@code other} computes from the cause if this is a {@code Failure}.
     *
     * @param other a function from the cause to a replacement value, called only for a {@code Failure}
     * @return the value of this {@code Success}, otherwise {@code other.apply(getCause())}
     * @throws NullPointerException if {@code other} is null
     */
    default T getOrElse(Function<? super Throwable, ? extends T> other) {
        Objects.requireNonNull(other, "other is null");
        return isFailure() ? other.apply(getCause()) : get();
    }

    /**
     * Returns the value of this {@link Success}, or throws the exception supplied by {@code exceptionSupplier} if this
     * is a {@link Failure}. To throw the cause itself, use {@link #get()}.
     *
     * @param <X>               the type of the exception to throw
     * @param exceptionSupplier a supplier of the exception, invoked only for a {@code Failure}
     * @return the value of this {@link Success}
     * @throws X                    if this is a {@link Failure}
     * @throws NullPointerException if {@code exceptionSupplier} is null
     */
    default <X extends Throwable> T getOrElseThrow(Supplier<X> exceptionSupplier) throws X {
        Objects.requireNonNull(exceptionSupplier, "exceptionSupplier is null");
        if (isFailure()) {
            throw exceptionSupplier.get();
        } else {
            return get();
        }
    }

    /**
     * Returns the value of this {@link Success}, or throws a provided exception if this is a {@link Failure}.
     * <p>
     * The exception to throw is created by applying the given {@code exceptionProvider} function to the cause of the failure.
     *
     * @param <X>               the type of the exception to throw
     * @param exceptionProvider a function mapping the throwable cause to an exception to be thrown
     * @return the value of this {@link Success}
     * @throws X                     the exception provided by {@code exceptionProvider} if this is a {@link Failure}
     * @throws NullPointerException  if {@code exceptionProvider} is null
     */
    default <X extends Throwable> T getOrElseThrow(Function<? super Throwable, X> exceptionProvider) throws X {
        Objects.requireNonNull(exceptionProvider, "exceptionProvider is null");
        if (isFailure()) {
            throw exceptionProvider.apply(getCause());
        } else {
            return get();
        }
    }

    /**
     * Checks whether this {@code Try} holds a value equal to {@code element}, as tested by {@link Objects#equals(Object, Object)}.
     *
     * @param element the element to look for, may be {@code null}
     * @return {@code true} if this is {@code Success(element)}, {@code false} otherwise (always for a {@code Failure})
     */
    default boolean contains(@Nullable T element) {
        return isSuccess() && Objects.equals(get(), element);
    }

    /**
     * Checks whether this {@code Try} holds a value satisfying the given predicate. The predicate is not run under
     * {@code Try}: whatever it throws propagates to the caller.
     *
     * @param predicate a predicate to test the value
     * @return {@code true} if this is a {@code Success} and the predicate holds for its value, {@code false} otherwise
     * @throws NullPointerException if {@code predicate} is null
     */
    default boolean exists(Predicate<? super T> predicate) {
        Objects.requireNonNull(predicate, "predicate is null");
        return isSuccess() && predicate.test(get());
    }

    /**
     * Checks whether the given predicate holds for the value of this {@code Try}; it holds vacuously for a
     * {@code Failure}. The predicate is not run under {@code Try}: whatever it throws propagates to the caller.
     *
     * @param predicate a predicate to test the value
     * @return {@code true} if this is a {@code Failure} or the predicate holds for the value, {@code false} otherwise
     * @throws NullPointerException if {@code predicate} is null
     */
    default boolean forAll(Predicate<? super T> predicate) {
        Objects.requireNonNull(predicate, "predicate is null");
        return isFailure() || predicate.test(get());
    }

    /**
     * Performs the given action on the value if this is a {@link Success}; does nothing for a {@code Failure}. Unlike
     * {@link #andThenTry(CheckedConsumer)}, an exception thrown by the action propagates to the caller.
     *
     * @param action a consumer of the value
     * @throws NullPointerException if {@code action} is null
     */
    default void forEach(Consumer<? super T> action) {
        Objects.requireNonNull(action, "action is null");
        if (isSuccess()) {
            action.accept(get());
        }
    }

    /**
     * Folds this {@code Try} into a single value by applying one of two functions:
     * one for the failure case and one for the success case.
     * <p>
     * If this is a {@link Failure}, the {@code ifFail} function is applied to the cause.
     * If this is a {@link Success}, the {@code f} function is applied to the value.
     *
     * @param ifFail maps the throwable cause if this is a {@link Failure}
     * @param f      maps the value if this is a {@link Success}
     * @param <X>    the type of the result
     * @return the result of applying the corresponding function
     */
    default <X extends @Nullable Object> X fold(Function<? super Throwable, ? extends X> ifFail, Function<? super T, ? extends X> f) {
        if (isFailure()) {
            return ifFail.apply(getCause());
        } else {
            return f.apply(get());
        }
    }

    /**
     * Runs {@code action} on the value if this is a {@code Success} and returns this {@code Try} unchanged; does
     * nothing for a {@code Failure}. What the action throws propagates to the caller; to capture it as a
     * {@code Failure} instead, use {@link #andThenTry(CheckedConsumer)}.
     *
     * @param action what to do with the value
     * @return this {@code Try}
     * @throws NullPointerException if {@code action} is null
     */
    default Try<T> tap(Consumer<? super T> action) {
        Objects.requireNonNull(action, "action is null");
        if (isSuccess()) {
            action.accept(get());
        }
        return this;
    }

    /**
     * Recovers from any failure: {@code f} turns the cause into a replacement value. A {@code Success} is returned
     * unchanged. The recovery runs under {@link #of(Callable)}: a non-fatal exception it throws, or a {@code null}
     * it returns, is the new {@code Failure}.
     * <pre>{@code
     * Try.of(() -> 1 / 0).catchAll(e -> Integer.MAX_VALUE); // = Success(2147483647)
     * Try.success(13).catchAll(e -> Integer.MAX_VALUE);     // = Success(13)
     * }</pre>
     *
     * @param f a function from the cause to the replacement value
     * @return this {@code Success}, or the {@code Try} of the recovery
     * @throws NullPointerException if {@code f} is null
     */
    default Try<T> catchAll(Function<? super Throwable, ? extends T> f) {
        Objects.requireNonNull(f, "f is null");
        return isFailure() ? Try.of(() -> f.apply(getCause())) : this;
    }

    /**
     * Recovers from the failures whose cause is an instance of {@code exceptionType}: {@code f} turns the cause
     * into a replacement value. A {@code Success}, or a {@code Failure} of another type, is returned unchanged. The
     * recovery runs under {@link #of(Callable)}: a non-fatal exception it throws, or a {@code null} it returns, is
     * the new {@code Failure}.
     * <pre>{@code
     * Try.of(() -> 1 / 0)
     *    .catchSome(Error.class, e -> -1)                            // does not match, stays the Failure
     *    .catchSome(ArithmeticException.class, e -> Integer.MAX_VALUE); // = Success(2147483647)
     * }</pre>
     *
     * @param exceptionType the type the cause has to be an instance of
     * @param f             a function from the cause to the replacement value
     * @param <X>           the type of the cause
     * @return this {@code Try}, or the {@code Try} of the recovery when the cause matches
     * @throws NullPointerException if {@code exceptionType} or {@code f} is null
     */
    @SuppressWarnings("unchecked")
    default <X extends Throwable> Try<T> catchSome(Class<X> exceptionType, Function<? super X, ? extends T> f) {
        Objects.requireNonNull(exceptionType, "exceptionType is null");
        Objects.requireNonNull(f, "f is null");
        return isFailure() && exceptionType.isInstance(getCause())
          ? Try.of(() -> f.apply((X) getCause()))
          : this;
    }

    /**
     * Recovers from any failure with a {@code Try}: {@code f} turns the cause into the {@code Try} to continue
     * with, which may itself be a {@code Failure}. A {@code Success} is returned unchanged. A non-fatal exception
     * {@code f} throws is the new {@code Failure}; a {@code null} it returns is a {@code Failure} of a
     * {@link NullPointerException}.
     * <pre>{@code
     * Try.of(() -> readCache()).catchAllWith(e -> Try.of(() -> readDisk()));
     * }</pre>
     *
     * @param f a function from the cause to the {@code Try} to continue with
     * @return this {@code Success}, or the {@code Try} the recovery returned
     * @throws NullPointerException if {@code f} is null
     */
    @SuppressWarnings("unchecked")
    default Try<T> catchAllWith(Function<? super Throwable, ? extends Try<? extends T>> f) {
        Objects.requireNonNull(f, "f is null");
        if (isSuccess()) {
            return this;
        }
        try {
            return (Try<T>) Objects.requireNonNull(f.apply(getCause()), "Try.catchAllWith: f returned null");
        } catch (Throwable t) {
            return new Failure<>(t);
        }
    }

    /**
     * Recovers from the failures whose cause is an instance of {@code exceptionType} with a {@code Try}: {@code f}
     * turns the cause into the {@code Try} to continue with, which may itself be a {@code Failure}. A
     * {@code Success}, or a {@code Failure} of another type, is returned unchanged. A non-fatal exception {@code f}
     * throws is the new {@code Failure}; a {@code null} it returns is a {@code Failure} of a
     * {@link NullPointerException}.
     * <pre>{@code
     * Try.of(() -> readCache()).catchSomeWith(IOException.class, e -> Try.of(() -> readDisk()));
     * }</pre>
     *
     * @param exceptionType the type the cause has to be an instance of
     * @param f             a function from the cause to the {@code Try} to continue with
     * @param <X>           the type of the cause
     * @return this {@code Try}, or the {@code Try} the recovery returned when the cause matches
     * @throws NullPointerException if {@code exceptionType} or {@code f} is null
     */
    @SuppressWarnings("unchecked")
    default <X extends Throwable> Try<T> catchSomeWith(Class<X> exceptionType, Function<? super X, ? extends Try<? extends T>> f) {
        Objects.requireNonNull(exceptionType, "exceptionType is null");
        Objects.requireNonNull(f, "f is null");
        if (isSuccess() || !exceptionType.isInstance(getCause())) {
            return this;
        }
        try {
            return (Try<T>) Objects.requireNonNull(f.apply((X) getCause()), "Try.catchSomeWith: f returned null");
        } catch (Throwable t) {
            return new Failure<>(t);
        }
    }

    /**
     * Replaces the cause of a {@code Failure} with the throwable {@code f} builds from it, typically to wrap it in
     * a domain exception. A {@code Success} is returned unchanged. A non-fatal exception {@code f} throws is the
     * new cause; a {@code null} it returns is a {@code Failure} of a {@link NullPointerException}, and a fatal
     * throwable it returns is rethrown (see the class-level documentation).
     * <pre>{@code
     * Try.of(() -> parse(input)).mapError(e -> new ConfigException("bad input", e));
     * }</pre>
     *
     * @param f a function from the cause to its replacement
     * @return this {@code Success}, or a {@code Failure} of the mapped cause
     * @throws NullPointerException if {@code f} is null
     */
    default Try<T> mapError(Function<? super Throwable, ? extends Throwable> f) {
        Objects.requireNonNull(f, "f is null");
        if (isSuccess()) {
            return this;
        }
        try {
            return new Failure<>(f.apply(getCause()));
        } catch (Throwable t) {
            return new Failure<>(t);
        }
    }

    /**
     * Converts this {@code Try} to an {@link Either}.
     * <p>
     * If this is a {@link Try.Success}, the value is wrapped as a {@link Either#right(Object)}.
     * If this is a {@link Try.Failure}, the cause is wrapped as a {@link Either#left(Object)}.
     *
     * @return a new {@code Either} representing this {@code Try}
     */
    default Either<Throwable, T> toEither() {
        if (isFailure()) {
            return Either.left(getCause());
        } else {
            return Either.right(get());
        }
    }

    /**
     * Converts this {@code Try} to a {@link Validation}: {@code Valid(value)} for a {@link Success}, {@code Invalid(cause)}
     * for a {@link Failure}.
     *
     * @return a new {@code Validation} representing this {@code Try}
     */
    default Validation<Throwable, T> toValidation() {
        if (isFailure()) {
            return Validation.invalid(getCause());
        } else {
            return Validation.valid(get());
        }
    }

    /**
     * Converts this {@code Try} to an {@link Option}: {@code Some(value)} for a {@link Success}, {@code None} for a
     * {@link Failure}, whose cause is dropped.
     *
     * @return {@code Option.some(get())} if this is a {@code Success}, otherwise {@code Option.none()}
     */
    default Option<T> toOption() {
        return isSuccess() ? Option.some(get()) : Option.none();
    }

    /**
     * Converts this {@code Try} to a {@link Vector} of zero or one element: its value, if any.
     *
     * @return {@code Vector.of(get())} if this is a {@code Success}, otherwise the empty {@code Vector}
     */
    default Vector<T> toVector() {
        return isSuccess() ? Vector.of(get()) : Vector.empty();
    }

    /**
     * Runs {@code finalizer} after this {@code Try} whatever its outcome, like a {@code finally} block, and returns
     * this {@code Try} unchanged when the finalizer completes normally.
     * <p>
     * If the finalizer throws a non-fatal exception: on a {@code Success} the result is a {@code Failure} of that
     * exception; on a {@code Failure} the original cause is kept and the exception is added to it as
     * {@linkplain Throwable#addSuppressed(Throwable) suppressed}, the way {@code try}-with-resources attaches an
     * exception thrown by {@code close()} to the primary one (JLS 14.20.3). A fatal throwable (see the class-level
     * documentation) is rethrown whatever the state of this {@code Try}.
     * <p>
     * A {@link Runnable} lambda is a {@code CheckedRunnable} lambda; a {@code Runnable} variable is passed as
     * {@code runnable::run}.
     * <pre>{@code
     * Try.of(() -> connection.query(sql)).ensuring(connection::close);
     * }</pre>
     *
     * @param finalizer what to run after this {@code Try}
     * @return this {@code Try} if the finalizer completes normally; a {@code Failure} of what it threw when this was
     *         a {@code Success}; this same {@code Failure} with the thrown exception suppressed otherwise
     * @throws NullPointerException if {@code finalizer} is null
     */
    default Try<T> ensuring(CheckedRunnable finalizer) {
        Objects.requireNonNull(finalizer, "finalizer is null");
        try {
            finalizer.run();
            return this;
        } catch (Throwable t) {
            if (isFailure() && !isFatal(t)) {
                getCause().addSuppressed(t);
                return this;
            }
            return new Failure<>(t);
        }
    }

    /**
     * Converts this to a {@link CompletableFuture}, already completed.
     *
     * @return a new {@link CompletableFuture}, completed with the value if this is a {@link Success}, or
     *         completed exceptionally with the cause if this is a {@link Failure}
     */
    default CompletableFuture<T> toCompletableFuture() {
        if (isSuccess()) {
            return CompletableFuture.completedFuture(get());
        } else {
            final CompletableFuture<T> completableFuture = new CompletableFuture<>();
            completableFuture.completeExceptionally(getCause());
            return completableFuture;
        }
    }

    @Override
    boolean equals(@Nullable Object o);

    @Override
    int hashCode();

    @Override
    String toString();

    /**
     * The successful case of a {@link Try}, holding the resulting value. The value is never {@code null}.
     *
     * <pre>{@code
     * Try<Integer> success = Try.success(42);
     * success.isSuccess(); // true
     * success.get();       // 42
     * }</pre>
     *
     * @param value the value, never {@code null}
     * @param <T>   the type of the contained value
     */
    record Success<T extends @Nullable Object>(T value) implements Try<T> {

        /**
         * Rejects {@code null}.
         *
         * @throws NullPointerException if {@code value} is null
         */
        public Success {
            Objects.requireNonNull(value, "value is null");
        }

        @Override
        public T get() {
            return value;
        }

        @Override
        public Throwable getCause() {
            throw new UnsupportedOperationException("getCause on Success");
        }

        @Override
        public boolean isEmpty() {
            return false;
        }

        @Override
        public boolean isFailure() {
            return false;
        }

        @Override
        public boolean isSuccess() {
            return true;
        }

        @Override
        public String toString() {
            return "Success(" + value + ")";
        }
    }

    /**
     * The failed case of a {@link Try}, holding the {@link Throwable} that was thrown.
     *
     * <pre>{@code
     * Try<Integer> failure = Try.failure(new RuntimeException("error"));
     * failure.isFailure();  // true
     * failure.getCause();   // RuntimeException: error
     * }</pre>
     *
     * <strong>Equality.</strong> Two {@code Failure}s are equal when their causes are the same object: the record
     * default, since {@code Throwable} does not override {@code equals}. Class, message and stack trace are not
     * compared, because two exceptions are not the same because they print alike. A test that means "failed the
     * same way" compares {@code getCause().getClass()} or {@code getMessage()} itself.
     *
     * @param cause the throwable, never {@code null} and never fatal (see the class-level documentation of {@link Try})
     * @param <T>   the type of the value that would have been contained if successful
     */
    record Failure<T extends @Nullable Object>(Throwable cause) implements Try<T> {

        /**
         * Rejects {@code null} and rethrows a fatal cause sneakily instead of wrapping it.
         *
         * @throws NullPointerException if {@code cause} is null
         */
        public Failure {
            Objects.requireNonNull(cause, "cause is null");
            if (isFatal(cause)) {
                sneakyThrow(cause);
            }
        }

        @Override
        public T get() {
            return sneakyThrow(cause);
        }

        @Override
        public Throwable getCause() {
            return cause;
        }

        @Override
        public boolean isEmpty() {
            return true;
        }

        @Override
        public boolean isFailure() {
            return true;
        }

        @Override
        public boolean isSuccess() {
            return false;
        }

        @Override
        public String toString() {
            return "Failure(" + cause + ")";
        }
    }

    // -- zip (design 3.4)

    /**
     * Pairs this value with {@code that}'s, failing fast: {@code Success} of the pair when both are {@code Success},
     * otherwise the first {@code Failure} of the two (this one, then {@code that}), as is. The same as
     * {@link #zipWith(Try, BiFunction)} with {@code Tuple::of}.
     * <pre>{@code
     * Try.success(1).zip(Try.success("a"));   // = Success((1, a))
     * Try.success(1).zip(Try.failure(error)); // = Failure(error)
     * }</pre>
     *
     * @param that the other {@code Try}
     * @param <U>  the value type of {@code that}
     * @return {@code Success} of the pair of values, or the first {@code Failure}
     * @throws NullPointerException if {@code that} is null
     */
    default <U extends @Nullable Object> Try<Tuple2<T, U>> zip(Try<? extends U> that) {
        return zipWith(that, Tuple::of);
    }

    /**
     * Combines this value with {@code that}'s through {@code f}, failing fast: {@code Success} of the result when
     * both are {@code Success}, otherwise the first {@code Failure} of the two (this one, then {@code that}), as is.
     * {@code f} is called only when both are {@code Success} and runs under {@code Try} like a {@link #map(Function)}
     * mapper: a non-fatal exception it throws is captured as a {@code Failure}, a fatal one is rethrown, and a
     * {@code null} result is a {@code Failure} of a {@link NullPointerException}, since {@code Success} cannot hold
     * {@code null} (design 3.9).
     *
     * @param that the other {@code Try}
     * @param f    combines the two values
     * @param <U>  the value type of {@code that}
     * @param <V>  the result type
     * @return {@code Success} of the combined value, or the first {@code Failure}, or a {@code Failure} of what
     *         {@code f} threw or of a {@code NullPointerException} if it returned {@code null}
     * @throws NullPointerException if {@code that} or {@code f} is null
     */
    @SuppressWarnings("unchecked")
    default <U extends @Nullable Object, V extends @Nullable Object> Try<V> zipWith(Try<? extends U> that, BiFunction<? super T, ? super U, ? extends V> f) {
        Objects.requireNonNull(that, "that is null");
        Objects.requireNonNull(f, "f is null");
        if (isFailure()) {
            return (Try<V>) this;
        }
        if (that.isFailure()) {
            return (Try<V>) that;
        }
        try {
            final V value = f.apply(get(), that.get());
            return value == null ? TryModule.nullZipResult() : new Success<>(value);
        } catch (Throwable x) {
            return new Failure<>(x);
        }
    }

    /**
     * {@link #zip(Try)} keeping this value: this {@code Success} when both are {@code Success}, otherwise the first
     * {@code Failure} of the two, as is. Both sides are inspected, so this is not {@link #orElse(Try)}:
     * {@code Success(1).zipLeft(Failure(e))} is {@code Failure(e)}.
     *
     * @param that the other {@code Try}
     * @param <U>  the value type of {@code that}
     * @return this {@code Success}, or the first {@code Failure}
     * @throws NullPointerException if {@code that} is null
     */
    @SuppressWarnings("unchecked")
    default <U extends @Nullable Object> Try<T> zipLeft(Try<? extends U> that) {
        Objects.requireNonNull(that, "that is null");
        return isFailure() || that.isSuccess() ? this : (Try<T>) that;
    }

    /**
     * {@link #zip(Try)} keeping {@code that}'s value: {@code that} when both are {@code Success}, otherwise the
     * first {@code Failure} of the two, as is. Both sides are inspected: {@code Failure(e).zipRight(Success(1))} is
     * {@code Failure(e)}.
     *
     * @param that the other {@code Try}
     * @param <U>  the value type of {@code that}
     * @return {@code that}, or the first {@code Failure}
     * @throws NullPointerException if {@code that} is null
     */
    @SuppressWarnings("unchecked")
    default <U extends @Nullable Object> Try<U> zipRight(Try<? extends U> that) {
        Objects.requireNonNull(that, "that is null");
        return isFailure() ? (Try<U>) this : narrow(that);
    }

    /**
     * Pairs the values of two {@code Try}s, failing fast: {@code Success} of the tuple of the values when every
     * argument is a {@code Success}, otherwise the first {@code Failure} in argument order, as is. The same as
     * {@link #zipWith(Try, Try, BiFunction)} with {@code Tuple::of}.
     *
     * @param t1  the first {@code Try}
     * @param t2  the second {@code Try}
     * @param <T1> the value type of {@code t1}
     * @param <T2> the value type of {@code t2}
     * @return {@code Success} of the tuple of the values, or the first {@code Failure}
     * @throws NullPointerException if any argument is null
     */
    static <T1 extends @Nullable Object, T2 extends @Nullable Object> Try<Tuple2<T1, T2>> zip(Try<? extends T1> t1, Try<? extends T2> t2) {
        return zipWith(t1, t2, Tuple::of);
    }

    /**
     * Combines the values of two {@code Try}s through {@code f}, failing fast: {@code Success} of the result when
     * every argument is a {@code Success}, otherwise the first {@code Failure} in argument order, as is. {@code f} is
     * called only when every argument is a {@code Success}, with the values in argument order, and runs under
     * {@code Try} like a {@link #map(Function)} mapper: a non-fatal exception it throws is captured as a
     * {@code Failure}, a fatal one is rethrown, and a {@code null} result is a {@code Failure} of a
     * {@link NullPointerException}, since {@code Success} cannot hold {@code null} (design 3.9).
     *
     * @param t1  the first {@code Try}
     * @param t2  the second {@code Try}
     * @param f  combines the values
     * @param <T1> the value type of {@code t1}
     * @param <T2> the value type of {@code t2}
     * @param <R>  the result type
     * @return {@code Success} of the combined value, or the first {@code Failure}, or a {@code Failure} of what
     *         {@code f} threw or of a {@code NullPointerException} if it returned {@code null}
     * @throws NullPointerException if any argument is null
     */
    @SuppressWarnings("unchecked")
    static <T1 extends @Nullable Object, T2 extends @Nullable Object, R extends @Nullable Object> Try<R> zipWith(Try<? extends T1> t1, Try<? extends T2> t2, BiFunction<? super T1, ? super T2, ? extends R> f) {
        Objects.requireNonNull(t1, "t1 is null");
        Objects.requireNonNull(t2, "t2 is null");
        Objects.requireNonNull(f, "f is null");
        if (t1.isFailure()) {
            return (Try<R>) t1;
        }
        if (t2.isFailure()) {
            return (Try<R>) t2;
        }
        try {
            final R value = f.apply(t1.get(), t2.get());
            return value == null ? TryModule.nullZipResult() : new Success<>(value);
        } catch (Throwable x) {
            return new Failure<>(x);
        }
    }

    /**
     * Pairs the values of three {@code Try}s, failing fast: {@code Success} of the tuple of the values when every
     * argument is a {@code Success}, otherwise the first {@code Failure} in argument order, as is. The same as
     * {@link #zipWith(Try, Try, Try, Function3)} with {@code Tuple::of}.
     *
     * @param t1  the first {@code Try}
     * @param t2  the second {@code Try}
     * @param t3  the third {@code Try}
     * @param <T1> the value type of {@code t1}
     * @param <T2> the value type of {@code t2}
     * @param <T3> the value type of {@code t3}
     * @return {@code Success} of the tuple of the values, or the first {@code Failure}
     * @throws NullPointerException if any argument is null
     */
    static <T1 extends @Nullable Object, T2 extends @Nullable Object, T3 extends @Nullable Object> Try<Tuple3<T1, T2, T3>> zip(Try<? extends T1> t1, Try<? extends T2> t2, Try<? extends T3> t3) {
        return zipWith(t1, t2, t3, Tuple::of);
    }

    /**
     * Combines the values of three {@code Try}s through {@code f}, failing fast: {@code Success} of the result when
     * every argument is a {@code Success}, otherwise the first {@code Failure} in argument order, as is. {@code f} is
     * called only when every argument is a {@code Success}, with the values in argument order, and runs under
     * {@code Try} like a {@link #map(Function)} mapper: a non-fatal exception it throws is captured as a
     * {@code Failure}, a fatal one is rethrown, and a {@code null} result is a {@code Failure} of a
     * {@link NullPointerException}, since {@code Success} cannot hold {@code null} (design 3.9).
     *
     * @param t1  the first {@code Try}
     * @param t2  the second {@code Try}
     * @param t3  the third {@code Try}
     * @param f  combines the values
     * @param <T1> the value type of {@code t1}
     * @param <T2> the value type of {@code t2}
     * @param <T3> the value type of {@code t3}
     * @param <R>  the result type
     * @return {@code Success} of the combined value, or the first {@code Failure}, or a {@code Failure} of what
     *         {@code f} threw or of a {@code NullPointerException} if it returned {@code null}
     * @throws NullPointerException if any argument is null
     */
    @SuppressWarnings("unchecked")
    static <T1 extends @Nullable Object, T2 extends @Nullable Object, T3 extends @Nullable Object, R extends @Nullable Object> Try<R> zipWith(Try<? extends T1> t1, Try<? extends T2> t2, Try<? extends T3> t3, Function3<? super T1, ? super T2, ? super T3, ? extends R> f) {
        Objects.requireNonNull(t1, "t1 is null");
        Objects.requireNonNull(t2, "t2 is null");
        Objects.requireNonNull(t3, "t3 is null");
        Objects.requireNonNull(f, "f is null");
        if (t1.isFailure()) {
            return (Try<R>) t1;
        }
        if (t2.isFailure()) {
            return (Try<R>) t2;
        }
        if (t3.isFailure()) {
            return (Try<R>) t3;
        }
        try {
            final R value = f.apply(t1.get(), t2.get(), t3.get());
            return value == null ? TryModule.nullZipResult() : new Success<>(value);
        } catch (Throwable x) {
            return new Failure<>(x);
        }
    }

    /**
     * Pairs the values of four {@code Try}s, failing fast: {@code Success} of the tuple of the values when every
     * argument is a {@code Success}, otherwise the first {@code Failure} in argument order, as is. The same as
     * {@link #zipWith(Try, Try, Try, Try, Function4)} with {@code Tuple::of}.
     *
     * @param t1  the first {@code Try}
     * @param t2  the second {@code Try}
     * @param t3  the third {@code Try}
     * @param t4  the fourth {@code Try}
     * @param <T1> the value type of {@code t1}
     * @param <T2> the value type of {@code t2}
     * @param <T3> the value type of {@code t3}
     * @param <T4> the value type of {@code t4}
     * @return {@code Success} of the tuple of the values, or the first {@code Failure}
     * @throws NullPointerException if any argument is null
     */
    static <T1 extends @Nullable Object, T2 extends @Nullable Object, T3 extends @Nullable Object, T4 extends @Nullable Object> Try<Tuple4<T1, T2, T3, T4>> zip(Try<? extends T1> t1, Try<? extends T2> t2, Try<? extends T3> t3, Try<? extends T4> t4) {
        return zipWith(t1, t2, t3, t4, Tuple::of);
    }

    /**
     * Combines the values of four {@code Try}s through {@code f}, failing fast: {@code Success} of the result when
     * every argument is a {@code Success}, otherwise the first {@code Failure} in argument order, as is. {@code f} is
     * called only when every argument is a {@code Success}, with the values in argument order, and runs under
     * {@code Try} like a {@link #map(Function)} mapper: a non-fatal exception it throws is captured as a
     * {@code Failure}, a fatal one is rethrown, and a {@code null} result is a {@code Failure} of a
     * {@link NullPointerException}, since {@code Success} cannot hold {@code null} (design 3.9).
     *
     * @param t1  the first {@code Try}
     * @param t2  the second {@code Try}
     * @param t3  the third {@code Try}
     * @param t4  the fourth {@code Try}
     * @param f  combines the values
     * @param <T1> the value type of {@code t1}
     * @param <T2> the value type of {@code t2}
     * @param <T3> the value type of {@code t3}
     * @param <T4> the value type of {@code t4}
     * @param <R>  the result type
     * @return {@code Success} of the combined value, or the first {@code Failure}, or a {@code Failure} of what
     *         {@code f} threw or of a {@code NullPointerException} if it returned {@code null}
     * @throws NullPointerException if any argument is null
     */
    @SuppressWarnings("unchecked")
    static <T1 extends @Nullable Object, T2 extends @Nullable Object, T3 extends @Nullable Object, T4 extends @Nullable Object, R extends @Nullable Object> Try<R> zipWith(Try<? extends T1> t1, Try<? extends T2> t2, Try<? extends T3> t3, Try<? extends T4> t4, Function4<? super T1, ? super T2, ? super T3, ? super T4, ? extends R> f) {
        Objects.requireNonNull(t1, "t1 is null");
        Objects.requireNonNull(t2, "t2 is null");
        Objects.requireNonNull(t3, "t3 is null");
        Objects.requireNonNull(t4, "t4 is null");
        Objects.requireNonNull(f, "f is null");
        if (t1.isFailure()) {
            return (Try<R>) t1;
        }
        if (t2.isFailure()) {
            return (Try<R>) t2;
        }
        if (t3.isFailure()) {
            return (Try<R>) t3;
        }
        if (t4.isFailure()) {
            return (Try<R>) t4;
        }
        try {
            final R value = f.apply(t1.get(), t2.get(), t3.get(), t4.get());
            return value == null ? TryModule.nullZipResult() : new Success<>(value);
        } catch (Throwable x) {
            return new Failure<>(x);
        }
    }

    /**
     * Pairs the values of five {@code Try}s, failing fast: {@code Success} of the tuple of the values when every
     * argument is a {@code Success}, otherwise the first {@code Failure} in argument order, as is. The same as
     * {@link #zipWith(Try, Try, Try, Try, Try, Function5)} with {@code Tuple::of}.
     *
     * @param t1  the first {@code Try}
     * @param t2  the second {@code Try}
     * @param t3  the third {@code Try}
     * @param t4  the fourth {@code Try}
     * @param t5  the fifth {@code Try}
     * @param <T1> the value type of {@code t1}
     * @param <T2> the value type of {@code t2}
     * @param <T3> the value type of {@code t3}
     * @param <T4> the value type of {@code t4}
     * @param <T5> the value type of {@code t5}
     * @return {@code Success} of the tuple of the values, or the first {@code Failure}
     * @throws NullPointerException if any argument is null
     */
    static <T1 extends @Nullable Object, T2 extends @Nullable Object, T3 extends @Nullable Object, T4 extends @Nullable Object, T5 extends @Nullable Object> Try<Tuple5<T1, T2, T3, T4, T5>> zip(Try<? extends T1> t1, Try<? extends T2> t2, Try<? extends T3> t3, Try<? extends T4> t4, Try<? extends T5> t5) {
        return zipWith(t1, t2, t3, t4, t5, Tuple::of);
    }

    /**
     * Combines the values of five {@code Try}s through {@code f}, failing fast: {@code Success} of the result when
     * every argument is a {@code Success}, otherwise the first {@code Failure} in argument order, as is. {@code f} is
     * called only when every argument is a {@code Success}, with the values in argument order, and runs under
     * {@code Try} like a {@link #map(Function)} mapper: a non-fatal exception it throws is captured as a
     * {@code Failure}, a fatal one is rethrown, and a {@code null} result is a {@code Failure} of a
     * {@link NullPointerException}, since {@code Success} cannot hold {@code null} (design 3.9).
     *
     * @param t1  the first {@code Try}
     * @param t2  the second {@code Try}
     * @param t3  the third {@code Try}
     * @param t4  the fourth {@code Try}
     * @param t5  the fifth {@code Try}
     * @param f  combines the values
     * @param <T1> the value type of {@code t1}
     * @param <T2> the value type of {@code t2}
     * @param <T3> the value type of {@code t3}
     * @param <T4> the value type of {@code t4}
     * @param <T5> the value type of {@code t5}
     * @param <R>  the result type
     * @return {@code Success} of the combined value, or the first {@code Failure}, or a {@code Failure} of what
     *         {@code f} threw or of a {@code NullPointerException} if it returned {@code null}
     * @throws NullPointerException if any argument is null
     */
    @SuppressWarnings("unchecked")
    static <T1 extends @Nullable Object, T2 extends @Nullable Object, T3 extends @Nullable Object, T4 extends @Nullable Object, T5 extends @Nullable Object, R extends @Nullable Object> Try<R> zipWith(Try<? extends T1> t1, Try<? extends T2> t2, Try<? extends T3> t3, Try<? extends T4> t4, Try<? extends T5> t5, Function5<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? extends R> f) {
        Objects.requireNonNull(t1, "t1 is null");
        Objects.requireNonNull(t2, "t2 is null");
        Objects.requireNonNull(t3, "t3 is null");
        Objects.requireNonNull(t4, "t4 is null");
        Objects.requireNonNull(t5, "t5 is null");
        Objects.requireNonNull(f, "f is null");
        if (t1.isFailure()) {
            return (Try<R>) t1;
        }
        if (t2.isFailure()) {
            return (Try<R>) t2;
        }
        if (t3.isFailure()) {
            return (Try<R>) t3;
        }
        if (t4.isFailure()) {
            return (Try<R>) t4;
        }
        if (t5.isFailure()) {
            return (Try<R>) t5;
        }
        try {
            final R value = f.apply(t1.get(), t2.get(), t3.get(), t4.get(), t5.get());
            return value == null ? TryModule.nullZipResult() : new Success<>(value);
        } catch (Throwable x) {
            return new Failure<>(x);
        }
    }

    /**
     * Pairs the values of six {@code Try}s, failing fast: {@code Success} of the tuple of the values when every
     * argument is a {@code Success}, otherwise the first {@code Failure} in argument order, as is. The same as
     * {@link #zipWith(Try, Try, Try, Try, Try, Try, Function6)} with {@code Tuple::of}.
     *
     * @param t1  the first {@code Try}
     * @param t2  the second {@code Try}
     * @param t3  the third {@code Try}
     * @param t4  the fourth {@code Try}
     * @param t5  the fifth {@code Try}
     * @param t6  the sixth {@code Try}
     * @param <T1> the value type of {@code t1}
     * @param <T2> the value type of {@code t2}
     * @param <T3> the value type of {@code t3}
     * @param <T4> the value type of {@code t4}
     * @param <T5> the value type of {@code t5}
     * @param <T6> the value type of {@code t6}
     * @return {@code Success} of the tuple of the values, or the first {@code Failure}
     * @throws NullPointerException if any argument is null
     */
    static <T1 extends @Nullable Object, T2 extends @Nullable Object, T3 extends @Nullable Object, T4 extends @Nullable Object, T5 extends @Nullable Object, T6 extends @Nullable Object> Try<Tuple6<T1, T2, T3, T4, T5, T6>> zip(Try<? extends T1> t1, Try<? extends T2> t2, Try<? extends T3> t3, Try<? extends T4> t4, Try<? extends T5> t5, Try<? extends T6> t6) {
        return zipWith(t1, t2, t3, t4, t5, t6, Tuple::of);
    }

    /**
     * Combines the values of six {@code Try}s through {@code f}, failing fast: {@code Success} of the result when
     * every argument is a {@code Success}, otherwise the first {@code Failure} in argument order, as is. {@code f} is
     * called only when every argument is a {@code Success}, with the values in argument order, and runs under
     * {@code Try} like a {@link #map(Function)} mapper: a non-fatal exception it throws is captured as a
     * {@code Failure}, a fatal one is rethrown, and a {@code null} result is a {@code Failure} of a
     * {@link NullPointerException}, since {@code Success} cannot hold {@code null} (design 3.9).
     *
     * @param t1  the first {@code Try}
     * @param t2  the second {@code Try}
     * @param t3  the third {@code Try}
     * @param t4  the fourth {@code Try}
     * @param t5  the fifth {@code Try}
     * @param t6  the sixth {@code Try}
     * @param f  combines the values
     * @param <T1> the value type of {@code t1}
     * @param <T2> the value type of {@code t2}
     * @param <T3> the value type of {@code t3}
     * @param <T4> the value type of {@code t4}
     * @param <T5> the value type of {@code t5}
     * @param <T6> the value type of {@code t6}
     * @param <R>  the result type
     * @return {@code Success} of the combined value, or the first {@code Failure}, or a {@code Failure} of what
     *         {@code f} threw or of a {@code NullPointerException} if it returned {@code null}
     * @throws NullPointerException if any argument is null
     */
    @SuppressWarnings("unchecked")
    static <T1 extends @Nullable Object, T2 extends @Nullable Object, T3 extends @Nullable Object, T4 extends @Nullable Object, T5 extends @Nullable Object, T6 extends @Nullable Object, R extends @Nullable Object> Try<R> zipWith(Try<? extends T1> t1, Try<? extends T2> t2, Try<? extends T3> t3, Try<? extends T4> t4, Try<? extends T5> t5, Try<? extends T6> t6, Function6<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? super T6, ? extends R> f) {
        Objects.requireNonNull(t1, "t1 is null");
        Objects.requireNonNull(t2, "t2 is null");
        Objects.requireNonNull(t3, "t3 is null");
        Objects.requireNonNull(t4, "t4 is null");
        Objects.requireNonNull(t5, "t5 is null");
        Objects.requireNonNull(t6, "t6 is null");
        Objects.requireNonNull(f, "f is null");
        if (t1.isFailure()) {
            return (Try<R>) t1;
        }
        if (t2.isFailure()) {
            return (Try<R>) t2;
        }
        if (t3.isFailure()) {
            return (Try<R>) t3;
        }
        if (t4.isFailure()) {
            return (Try<R>) t4;
        }
        if (t5.isFailure()) {
            return (Try<R>) t5;
        }
        if (t6.isFailure()) {
            return (Try<R>) t6;
        }
        try {
            final R value = f.apply(t1.get(), t2.get(), t3.get(), t4.get(), t5.get(), t6.get());
            return value == null ? TryModule.nullZipResult() : new Success<>(value);
        } catch (Throwable x) {
            return new Failure<>(x);
        }
    }

    /**
     * Pairs the values of seven {@code Try}s, failing fast: {@code Success} of the tuple of the values when every
     * argument is a {@code Success}, otherwise the first {@code Failure} in argument order, as is. The same as
     * {@link #zipWith(Try, Try, Try, Try, Try, Try, Try, Function7)} with {@code Tuple::of}.
     *
     * @param t1  the first {@code Try}
     * @param t2  the second {@code Try}
     * @param t3  the third {@code Try}
     * @param t4  the fourth {@code Try}
     * @param t5  the fifth {@code Try}
     * @param t6  the sixth {@code Try}
     * @param t7  the seventh {@code Try}
     * @param <T1> the value type of {@code t1}
     * @param <T2> the value type of {@code t2}
     * @param <T3> the value type of {@code t3}
     * @param <T4> the value type of {@code t4}
     * @param <T5> the value type of {@code t5}
     * @param <T6> the value type of {@code t6}
     * @param <T7> the value type of {@code t7}
     * @return {@code Success} of the tuple of the values, or the first {@code Failure}
     * @throws NullPointerException if any argument is null
     */
    static <T1 extends @Nullable Object, T2 extends @Nullable Object, T3 extends @Nullable Object, T4 extends @Nullable Object, T5 extends @Nullable Object, T6 extends @Nullable Object, T7 extends @Nullable Object> Try<Tuple7<T1, T2, T3, T4, T5, T6, T7>> zip(Try<? extends T1> t1, Try<? extends T2> t2, Try<? extends T3> t3, Try<? extends T4> t4, Try<? extends T5> t5, Try<? extends T6> t6, Try<? extends T7> t7) {
        return zipWith(t1, t2, t3, t4, t5, t6, t7, Tuple::of);
    }

    /**
     * Combines the values of seven {@code Try}s through {@code f}, failing fast: {@code Success} of the result when
     * every argument is a {@code Success}, otherwise the first {@code Failure} in argument order, as is. {@code f} is
     * called only when every argument is a {@code Success}, with the values in argument order, and runs under
     * {@code Try} like a {@link #map(Function)} mapper: a non-fatal exception it throws is captured as a
     * {@code Failure}, a fatal one is rethrown, and a {@code null} result is a {@code Failure} of a
     * {@link NullPointerException}, since {@code Success} cannot hold {@code null} (design 3.9).
     *
     * @param t1  the first {@code Try}
     * @param t2  the second {@code Try}
     * @param t3  the third {@code Try}
     * @param t4  the fourth {@code Try}
     * @param t5  the fifth {@code Try}
     * @param t6  the sixth {@code Try}
     * @param t7  the seventh {@code Try}
     * @param f  combines the values
     * @param <T1> the value type of {@code t1}
     * @param <T2> the value type of {@code t2}
     * @param <T3> the value type of {@code t3}
     * @param <T4> the value type of {@code t4}
     * @param <T5> the value type of {@code t5}
     * @param <T6> the value type of {@code t6}
     * @param <T7> the value type of {@code t7}
     * @param <R>  the result type
     * @return {@code Success} of the combined value, or the first {@code Failure}, or a {@code Failure} of what
     *         {@code f} threw or of a {@code NullPointerException} if it returned {@code null}
     * @throws NullPointerException if any argument is null
     */
    @SuppressWarnings("unchecked")
    static <T1 extends @Nullable Object, T2 extends @Nullable Object, T3 extends @Nullable Object, T4 extends @Nullable Object, T5 extends @Nullable Object, T6 extends @Nullable Object, T7 extends @Nullable Object, R extends @Nullable Object> Try<R> zipWith(Try<? extends T1> t1, Try<? extends T2> t2, Try<? extends T3> t3, Try<? extends T4> t4, Try<? extends T5> t5, Try<? extends T6> t6, Try<? extends T7> t7, Function7<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? super T6, ? super T7, ? extends R> f) {
        Objects.requireNonNull(t1, "t1 is null");
        Objects.requireNonNull(t2, "t2 is null");
        Objects.requireNonNull(t3, "t3 is null");
        Objects.requireNonNull(t4, "t4 is null");
        Objects.requireNonNull(t5, "t5 is null");
        Objects.requireNonNull(t6, "t6 is null");
        Objects.requireNonNull(t7, "t7 is null");
        Objects.requireNonNull(f, "f is null");
        if (t1.isFailure()) {
            return (Try<R>) t1;
        }
        if (t2.isFailure()) {
            return (Try<R>) t2;
        }
        if (t3.isFailure()) {
            return (Try<R>) t3;
        }
        if (t4.isFailure()) {
            return (Try<R>) t4;
        }
        if (t5.isFailure()) {
            return (Try<R>) t5;
        }
        if (t6.isFailure()) {
            return (Try<R>) t6;
        }
        if (t7.isFailure()) {
            return (Try<R>) t7;
        }
        try {
            final R value = f.apply(t1.get(), t2.get(), t3.get(), t4.get(), t5.get(), t6.get(), t7.get());
            return value == null ? TryModule.nullZipResult() : new Success<>(value);
        } catch (Throwable x) {
            return new Failure<>(x);
        }
    }

    /**
     * Pairs the values of eight {@code Try}s, failing fast: {@code Success} of the tuple of the values when every
     * argument is a {@code Success}, otherwise the first {@code Failure} in argument order, as is. The same as
     * {@link #zipWith(Try, Try, Try, Try, Try, Try, Try, Try, Function8)} with {@code Tuple::of}.
     *
     * @param t1  the first {@code Try}
     * @param t2  the second {@code Try}
     * @param t3  the third {@code Try}
     * @param t4  the fourth {@code Try}
     * @param t5  the fifth {@code Try}
     * @param t6  the sixth {@code Try}
     * @param t7  the seventh {@code Try}
     * @param t8  the eighth {@code Try}
     * @param <T1> the value type of {@code t1}
     * @param <T2> the value type of {@code t2}
     * @param <T3> the value type of {@code t3}
     * @param <T4> the value type of {@code t4}
     * @param <T5> the value type of {@code t5}
     * @param <T6> the value type of {@code t6}
     * @param <T7> the value type of {@code t7}
     * @param <T8> the value type of {@code t8}
     * @return {@code Success} of the tuple of the values, or the first {@code Failure}
     * @throws NullPointerException if any argument is null
     */
    static <T1 extends @Nullable Object, T2 extends @Nullable Object, T3 extends @Nullable Object, T4 extends @Nullable Object, T5 extends @Nullable Object, T6 extends @Nullable Object, T7 extends @Nullable Object, T8 extends @Nullable Object> Try<Tuple8<T1, T2, T3, T4, T5, T6, T7, T8>> zip(Try<? extends T1> t1, Try<? extends T2> t2, Try<? extends T3> t3, Try<? extends T4> t4, Try<? extends T5> t5, Try<? extends T6> t6, Try<? extends T7> t7, Try<? extends T8> t8) {
        return zipWith(t1, t2, t3, t4, t5, t6, t7, t8, Tuple::of);
    }

    /**
     * Combines the values of eight {@code Try}s through {@code f}, failing fast: {@code Success} of the result when
     * every argument is a {@code Success}, otherwise the first {@code Failure} in argument order, as is. {@code f} is
     * called only when every argument is a {@code Success}, with the values in argument order, and runs under
     * {@code Try} like a {@link #map(Function)} mapper: a non-fatal exception it throws is captured as a
     * {@code Failure}, a fatal one is rethrown, and a {@code null} result is a {@code Failure} of a
     * {@link NullPointerException}, since {@code Success} cannot hold {@code null} (design 3.9).
     *
     * @param t1  the first {@code Try}
     * @param t2  the second {@code Try}
     * @param t3  the third {@code Try}
     * @param t4  the fourth {@code Try}
     * @param t5  the fifth {@code Try}
     * @param t6  the sixth {@code Try}
     * @param t7  the seventh {@code Try}
     * @param t8  the eighth {@code Try}
     * @param f  combines the values
     * @param <T1> the value type of {@code t1}
     * @param <T2> the value type of {@code t2}
     * @param <T3> the value type of {@code t3}
     * @param <T4> the value type of {@code t4}
     * @param <T5> the value type of {@code t5}
     * @param <T6> the value type of {@code t6}
     * @param <T7> the value type of {@code t7}
     * @param <T8> the value type of {@code t8}
     * @param <R>  the result type
     * @return {@code Success} of the combined value, or the first {@code Failure}, or a {@code Failure} of what
     *         {@code f} threw or of a {@code NullPointerException} if it returned {@code null}
     * @throws NullPointerException if any argument is null
     */
    @SuppressWarnings("unchecked")
    static <T1 extends @Nullable Object, T2 extends @Nullable Object, T3 extends @Nullable Object, T4 extends @Nullable Object, T5 extends @Nullable Object, T6 extends @Nullable Object, T7 extends @Nullable Object, T8 extends @Nullable Object, R extends @Nullable Object> Try<R> zipWith(Try<? extends T1> t1, Try<? extends T2> t2, Try<? extends T3> t3, Try<? extends T4> t4, Try<? extends T5> t5, Try<? extends T6> t6, Try<? extends T7> t7, Try<? extends T8> t8, Function8<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? super T6, ? super T7, ? super T8, ? extends R> f) {
        Objects.requireNonNull(t1, "t1 is null");
        Objects.requireNonNull(t2, "t2 is null");
        Objects.requireNonNull(t3, "t3 is null");
        Objects.requireNonNull(t4, "t4 is null");
        Objects.requireNonNull(t5, "t5 is null");
        Objects.requireNonNull(t6, "t6 is null");
        Objects.requireNonNull(t7, "t7 is null");
        Objects.requireNonNull(t8, "t8 is null");
        Objects.requireNonNull(f, "f is null");
        if (t1.isFailure()) {
            return (Try<R>) t1;
        }
        if (t2.isFailure()) {
            return (Try<R>) t2;
        }
        if (t3.isFailure()) {
            return (Try<R>) t3;
        }
        if (t4.isFailure()) {
            return (Try<R>) t4;
        }
        if (t5.isFailure()) {
            return (Try<R>) t5;
        }
        if (t6.isFailure()) {
            return (Try<R>) t6;
        }
        if (t7.isFailure()) {
            return (Try<R>) t7;
        }
        if (t8.isFailure()) {
            return (Try<R>) t8;
        }
        try {
            final R value = f.apply(t1.get(), t2.get(), t3.get(), t4.get(), t5.get(), t6.get(), t7.get(), t8.get());
            return value == null ? TryModule.nullZipResult() : new Success<>(value);
        } catch (Throwable x) {
            return new Failure<>(x);
        }
    }

    // -- try with resources

    /**
     * Runs {@code f} with a resource and closes the resource afterwards, like {@code try}-with-resources under
     * {@code Try}: the resource is obtained from {@code resource}, passed to {@code f}, then closed whatever
     * happened. The result is {@code Success} of what {@code f} returned, or a {@code Failure} of the first
     * non-fatal exception thrown by the acquisition, by {@code f} or by {@code close()}; an exception thrown by
     * {@code close()} after {@code f} threw is added to the cause as
     * {@linkplain Throwable#addSuppressed(Throwable) suppressed}. A {@code null} result is a {@code Failure} of a
     * {@link NullPointerException}, see {@link #of(Callable)}. Several resources nest:
     * <pre>{@code
     * Try<String> firstLine = Try.withResources(() -> new FileReader(path), reader ->
     *     Try.withResources(() -> new BufferedReader(reader), BufferedReader::readLine).get());
     * }</pre>
     *
     * @param resource obtains the resource; called once
     * @param f        the computation over the resource
     * @param <R>      the resource type
     * @param <T>      the result type
     * @return {@code Success} of the result of {@code f}, or a {@code Failure}
     * @throws NullPointerException if {@code resource} or {@code f} is null
     */
    @SuppressWarnings("try") /* https://bugs.openjdk.java.net/browse/JDK-8155591 */
    static <R extends AutoCloseable, T extends @Nullable Object> Try<T> withResources(Callable<? extends R> resource, CheckedFunction1<? super R, ? extends T> f) {
        Objects.requireNonNull(resource, "resource is null");
        Objects.requireNonNull(f, "f is null");
        return Try.of(() -> {
            try (R r = resource.call()) {
                return f.apply(r);
            }
        });
    }
}

interface TryModule {

    /** The Failure a capturing constructor returns when the computation yields null, which Success cannot hold. */
    static <T extends @Nullable Object> Try<T> nullResult(String constructor) {
        return new Try.Failure<>(new NullPointerException(constructor + ": the computation returned null"));
    }

    /** The Failure {@code zipWith} returns when {@code f} yields null, which Success cannot hold. */
    static <T extends @Nullable Object> Try<T> nullZipResult() {
        return new Try.Failure<>(new NullPointerException("Try.zipWith: f returned null"));
    }
}
