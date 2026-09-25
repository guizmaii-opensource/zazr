package com.guizmaii.zazr.control;

import com.guizmaii.zazr.Function3;
import com.guizmaii.zazr.Function4;
import com.guizmaii.zazr.Function5;
import com.guizmaii.zazr.Function6;
import com.guizmaii.zazr.Function7;
import com.guizmaii.zazr.Function8;
import com.guizmaii.zazr.Tuple;
import com.guizmaii.zazr.Tuple2;
import com.guizmaii.zazr.Tuple3;
import com.guizmaii.zazr.Tuple4;
import com.guizmaii.zazr.Tuple5;
import com.guizmaii.zazr.Tuple6;
import com.guizmaii.zazr.Tuple7;
import com.guizmaii.zazr.Tuple8;
import com.guizmaii.zazr.collection.Vector;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import org.jspecify.annotations.Nullable;

/**
 * Represents a value of one of two possible types: {@link Left} or {@link Right}.
 * <p>
 * An {@code Either<L, R>} is typically used to model a computation that may result in either
 * a success (represented by {@code Right}) or a failure (represented by {@code Left}).
 * <p>
 * This implementation is <strong>right-biased</strong>, meaning that most operations such as
 * {@code map}, {@code flatMap}, {@code filter}, etc., are defined for the {@code Right} value,
 * so computations chain fluently in the successful case. The {@code Left} side is reached with
 * {@link #mapLeft(Function)}, {@link #flip()} and {@link #fold(Function, Function)}.
 * <p>
 * {@code Either} is a sealed interface with two record cases, {@link Left} and {@link Right}, so it is
 * eliminated with an exhaustive {@code switch}:
 * <pre>{@code
 * String s = switch (either) {
 *     case Left(var error) -> "failed: " + error;
 *     case Right(var value) -> "got " + value;
 * };
 * }</pre>
 * Neither case holds {@code null}: {@link #left(Object)} and {@link #right(Object)} throw.
 * <p>
 * An {@code Either} is not a collection and not {@link Iterable} (design 3.2): to iterate its right value, convert
 * it explicitly with {@link #toVector()} or {@link #toOption()}.
 *
 * <h2>Example</h2>
 * <p>
 * Suppose we have a {@code compute()} function that returns an {@code Either<String, Integer>},
 * where {@code Right} represents a successful result and {@code Left} holds an error message.
 *
 * <pre>{@code
 * Either<String, Integer> result = compute().map(i -> i * 2);
 * }</pre>
 * <p>
 * If {@code compute()} returns {@code Right(1)}, the result will be {@code Right(2)}.<br>
 * If {@code compute()} returns {@code Left("error")}, the result will remain {@code Left("error")}.
 *
 * @param <L> The type of the Left value.
 * @param <R> The type of the Right value.
 *
 * @author Daniel Dietrich, Grzegorz Piwowarek, Adam Kopeć
 */
public sealed interface Either<L extends @Nullable Object, R extends @Nullable Object> permits Either.Left, Either.Right {

    /**
     * Constructs a new {@link Right} instance containing the given value.
     *
     * @param right the value to store in the {@code Right}, must not be {@code null}
     * @param <L>   the type of the left value
     * @param <R>   the type of the right value
     * @return a new {@code Right} instance
     * @throws NullPointerException if {@code right} is null
     */
    static <L extends @Nullable Object, R extends @Nullable Object> Either<L, R> right(R right) {
        return new Right<>(right);
    }

    /**
     * Constructs a new {@link Left} instance containing the given value.
     *
     * @param left the value to store in the {@code Left}, must not be {@code null}
     * @param <L>  the type of the left value
     * @param <R>  the type of the right value
     * @return a new {@code Left} instance
     * @throws NullPointerException if {@code left} is null
     */
    static <L extends @Nullable Object, R extends @Nullable Object> Either<L, R> left(L left) {
        return new Left<>(left);
    }

    /**
     * Narrows a {@code Either<? extends L, ? extends R>} to {@code Either<L, R>} via a type-safe cast.
     * This is safe because {@code Either} is immutable and its contents are read-only, so it is covariant
     * in both {@code L} and {@code R}.
     *
     * @param either the {@code Either} to narrow
     * @param <L>    the type of the left value
     * @param <R>    the type of the right value
     * @return the same {@code either} instance cast to {@code Either<L, R>}
     */
    @SuppressWarnings("unchecked")
    static <L extends @Nullable Object, R extends @Nullable Object> Either<L, R> narrow(Either<? extends L, ? extends R> either) {
        return (Either<L, R>) either;
    }

    /**
     * Removes one level of nesting on the right: {@code Right(Right(r))} is {@code Right(r)}, {@code Right(Left(l))}
     * is {@code Left(l)}, and an outer {@code Left} is returned as it is. Static, like every {@code flatten} in zazr,
     * because Java cannot demand of an instance method that the right value be an {@code Either} itself.
     *
     * @param nested an {@code Either} whose right value is an {@code Either} with the same left type
     * @param <L>    the type of the left value
     * @param <R>    the type of the inner right value
     * @return the inner {@code Either}, or the outer {@code Left}
     * @throws NullPointerException if {@code nested} is null
     */
    @SuppressWarnings("unchecked")
    static <L extends @Nullable Object, R extends @Nullable Object> Either<L, R> flatten(Either<? extends L, ? extends Either<? extends L, ? extends R>> nested) {
        Objects.requireNonNull(nested, "nested is null");
        return switch (nested) {
            case Right(var inner) -> narrow(inner);
            case Left<?, ?> left -> (Either<L, R>) left;
        };
    }

    /**
     * Tests {@code value} with {@code predicate}: {@code Right(value)} if it holds, {@code Left(ifFalse.get())} if
     * it does not. The supplier is called only when the predicate fails.
     * <pre>{@code
     * Either.fromPredicate(age, a -> a >= 18, () -> "minor"); // = Right(age) or Left("minor")
     * }</pre>
     *
     * @param value     the value to test, must not be {@code null}
     * @param predicate the condition the value has to satisfy
     * @param ifFalse   supplies the left value when the predicate fails; it must not return {@code null}
     * @param <L>       the type of the left value
     * @param <R>       the type of the right value
     * @return {@code Right(value)} if the predicate holds, otherwise {@code Left} of the supplied value
     * @throws NullPointerException if any argument is null, or if {@code ifFalse} supplies null
     */
    static <L extends @Nullable Object, R extends @Nullable Object> Either<L, R> fromPredicate(R value, Predicate<? super R> predicate, Supplier<? extends L> ifFalse) {
        Objects.requireNonNull(value, "value is null");
        Objects.requireNonNull(predicate, "predicate is null");
        Objects.requireNonNull(ifFalse, "ifFalse is null");
        return predicate.test(value) ? right(value) : left(ifFalse.get());
    }

    /**
     * Returns the left value of this {@code Either}.
     *
     * @return the left value
     * @throws NoSuchElementException if this {@code Either} is a {@link Either.Right}
     */
    L getLeft();

    /**
     * Checks whether this {@code Either} is a {@link Either.Left}.
     *
     * @return {@code true} if this is a {@code Left}, {@code false} otherwise
     */
    boolean isLeft();

    /**
     * Checks whether this {@code Either} is a {@link Either.Right}.
     *
     * @return {@code true} if this is a {@code Right}, {@code false} otherwise
     */
    boolean isRight();

    /**
     * Maps both sides at once: {@code leftMapper} is applied to a {@code Left}, {@code rightMapper} to a
     * {@code Right}; only one of them runs. The same as {@code mapLeft(leftMapper).map(rightMapper)}.
     * <p>
     * A mapper that returns {@code null} makes this throw {@link NullPointerException}: neither {@code Left} nor {@code Right} holds {@code null} (design 3.9).
     *
     * @param leftMapper  the function for a left value
     * @param rightMapper the function for a right value
     * @param <X>         the new left type
     * @param <Y>         the new right type
     * @return a {@code Left} or {@code Right} of the mapped value
     * @throws NullPointerException if a mapper is null
     */
    default <X extends @Nullable Object, Y extends @Nullable Object> Either<X, Y> mapBoth(Function<? super L, ? extends X> leftMapper, Function<? super R, ? extends Y> rightMapper) {
        Objects.requireNonNull(leftMapper, "leftMapper is null");
        Objects.requireNonNull(rightMapper, "rightMapper is null");
        if (isRight()) {
            return new Right<>(rightMapper.apply(get()));
        } else {
            return new Left<>(leftMapper.apply(getLeft()));
        }
    }

    /**
     * Reduces this {@code Either} to a single value by applying one of the given functions.
     * <ul>
     *   <li>If this is a {@link Either.Left}, {@code leftMapper} is applied to the left value.</li>
     *   <li>If this is a {@link Either.Right}, {@code rightMapper} is applied to the right value.</li>
     * </ul>
     *
     * @param leftMapper  function to transform the left value if this is a {@code Left}
     * @param rightMapper function to transform the right value if this is a {@code Right}
     * @param <U>         the type of the resulting value
     * @return a value of type {@code U} obtained by applying the appropriate function
     */
    default <U extends @Nullable Object> U fold(Function<? super L, ? extends U> leftMapper, Function<? super R, ? extends U> rightMapper) {
        Objects.requireNonNull(leftMapper, "leftMapper is null");
        Objects.requireNonNull(rightMapper, "rightMapper is null");
        if (isRight()) {
            return rightMapper.apply(get());
        } else {
            return leftMapper.apply(getLeft());
        }
    }

    /**
     * Turns many {@code Either}s into one {@code Either} of all their right values: {@code Right} of a
     * {@link Vector} of the values in iteration order when every element is a {@code Right}, otherwise the first
     * {@code Left} in iteration order. It stops at that first {@code Left}; collecting every left value is what
     * {@link Validation} is for. The empty iterable gives {@code Right} of the empty {@code Vector}.
     * <pre>{@code
     * Either.collectAll(List.of(Either.right(1), Either.right(2)));                     // = Right(Vector(1, 2))
     * Either.collectAll(List.of(Either.right(1), Either.left("x1"), Either.left("x2"))); // = Left("x1")
     * }</pre>
     *
     * @param eithers the {@code Either}s to collect
     * @param <L>     the left type
     * @param <R>     the right type
     * @return {@code Right} of all the right values, or the first {@code Left}
     * @throws NullPointerException if {@code eithers} is null
     */
    static <L extends @Nullable Object, R extends @Nullable Object> Either<L, Vector<R>> collectAll(Iterable<? extends Either<? extends L, ? extends R>> eithers) {
        Objects.requireNonNull(eithers, "eithers is null");
        final Vector.Builder<R> rightValues = Vector.newBuilder();
        for (Either<? extends L, ? extends R> either : eithers) {
            if (either.isRight()) {
                rightValues.add(either.get());
            } else {
                return Either.left(either.getLeft());
            }
        }
        return Either.right(rightValues.result());
    }

    /**
     * Applies {@code mapper} to every element and collects the results as {@link #collectAll(Iterable)} does:
     * {@code Right} of a {@link Vector} of the mapped values when every call returns a {@code Right}, otherwise the
     * first {@code Left}. The mapper is not called for the elements after that one.
     * <pre>{@code
     * Either.forEach(List.of("1", "2"), s -> parse(s)); // = Right(Vector(1, 2)) when both parse
     * }</pre>
     *
     * @param values the elements to map
     * @param mapper a function from an element to an {@code Either}; it must not return {@code null}
     * @param <L>    the left type
     * @param <R>    the right type
     * @param <T>    the element type
     * @return {@code Right} of all the mapped values, or the first {@code Left}
     * @throws NullPointerException if {@code values} or {@code mapper} is null
     */
    static <L extends @Nullable Object, R extends @Nullable Object, T extends @Nullable Object> Either<L, Vector<R>> forEach(Iterable<? extends T> values, Function<? super T, ? extends Either<? extends L, ? extends R>> mapper) {
        Objects.requireNonNull(values, "values is null");
        Objects.requireNonNull(mapper, "mapper is null");
        final Vector.Builder<R> rightValues = Vector.newBuilder();
        for (T value : values) {
            final Either<? extends L, ? extends R> mapped = Objects.requireNonNull(mapper.apply(value), "Either.forEach: mapper returned null");
            if (mapped.isRight()) {
                rightValues.add(mapped.get());
            } else {
                return Either.left(mapped.getLeft());
            }
        }
        return Either.right(rightValues.result());
    }

    /**
     * Returns the right value if this is a {@code Right}; otherwise throws.
     *
     * @return the right value
     * @throws NoSuchElementException if this is a {@code Left}
     */
    R get();

    /**
     * Checks if this {@code Either} is empty, i.e. holds no right value.
     *
     * @return {@code true} if this is a {@link Either.Left}, {@code false} if this is a {@link Either.Right}
     */
    default boolean isEmpty() {
        return isLeft();
    }

    /**
     * Returns the right value of this {@code Either}, or {@code other} if this is a {@link Either.Left}.
     * <p>
     * Note that {@code other} is evaluated eagerly.
     *
     * @param other an alternative value
     * @return the right value if present, otherwise {@code other}
     */
    default R getOrElse(R other) {
        return isRight() ? get() : other;
    }

    /**
     * Returns the right value of this {@code Either}, or the value supplied by {@code supplier} if this is a {@link Either.Left}.
     *
     * @param supplier a supplier of an alternative value, invoked only for a {@code Left}
     * @return the right value if present, otherwise the supplied value
     * @throws NullPointerException if {@code supplier} is null
     */
    default R getOrElse(Supplier<? extends R> supplier) {
        Objects.requireNonNull(supplier, "supplier is null");
        return isRight() ? get() : supplier.get();
    }

    /**
     * Returns the right value, or the value {@code other} computes from the left value if this is a {@code Left}.
     *
     * @param other a function from the left value to a replacement right value, called only for a {@code Left}
     * @return the right value if present, otherwise {@code other.apply(getLeft())}
     * @throws NullPointerException if {@code other} is null
     */
    default R getOrElse(Function<? super L, ? extends R> other) {
        Objects.requireNonNull(other, "other is null");
        if (isRight()) {
            return get();
        } else {
            return other.apply(getLeft());
        }
    }

    /**
     * Returns the right value of this {@code Either}, or throws the exception supplied by {@code exceptionSupplier}
     * if it is a {@link Either.Left}.
     *
     * @param <X>               the type of exception to be thrown
     * @param exceptionSupplier a supplier of the exception, invoked only for a {@code Left}
     * @return the right value if present
     * @throws X if this {@code Either} is a {@link Either.Left}
     * @throws NullPointerException if {@code exceptionSupplier} is null
     */
    default <X extends Throwable> R getOrElseThrow(Supplier<X> exceptionSupplier) throws X {
        Objects.requireNonNull(exceptionSupplier, "exceptionSupplier is null");
        if (isRight()) {
            return get();
        } else {
            throw exceptionSupplier.get();
        }
    }

    /**
     * Returns the right value of this {@code Either}, or throws an exception if it is a {@link Either.Left}.
     *
     * @param <X>               the type of exception to be thrown
     * @param exceptionFunction a function that produces an exception from the left value
     * @return the right value if present
     * @throws X if this {@code Either} is a {@link Either.Left}, using the exception produced by {@code exceptionFunction}
     */
    default <X extends Throwable> R getOrElseThrow(Function<? super L, X> exceptionFunction) throws X {
        Objects.requireNonNull(exceptionFunction, "exceptionFunction is null");
        if (isRight()) {
            return get();
        } else {
            throw exceptionFunction.apply(getLeft());
        }
    }

    /**
     * Returns the right value of this {@code Either}, or {@code null} if this is a {@link Either.Left}.
     *
     * @return the right value if present, otherwise {@code null}
     */
    default @Nullable R getOrNull() {
        return isRight() ? get() : null;
    }

    /**
     * Returns this {@code Either} if it is a {@link Either.Right}, otherwise returns the given {@code other} Either.
     *
     * @param other an alternative {@code Either}
     * @return this {@code Either} if it is a {@code Right}, otherwise {@code other}
     */
    @SuppressWarnings("unchecked")
    default Either<L, R> orElse(Either<? extends L, ? extends R> other) {
        Objects.requireNonNull(other, "other is null");
        return isRight() ? this : (Either<L, R>) other;
    }

    /**
     * Returns this {@code Either} if it is a {@link Either.Right}, otherwise returns the result of evaluating the given {@code supplier}.
     *
     * @param supplier a supplier of an alternative {@code Either}
     * @return this {@code Either} if it is a {@code Right}, otherwise the result of {@code supplier}
     */
    @SuppressWarnings("unchecked")
    default Either<L, R> orElse(Supplier<? extends Either<? extends L, ? extends R>> supplier) {
        Objects.requireNonNull(supplier, "supplier is null");
        return isRight() ? this : (Either<L, R>) supplier.get();
    }

    /**
     * Exchanges the sides: a {@code Left(l)} becomes {@code Right(l)}, a {@code Right(r)} becomes {@code Left(r)}.
     * Useful to run the right-biased operations on the left value, then {@code flip()} back.
     *
     * @return this {@code Either} with its sides exchanged
     */
    default Either<R, L> flip() {
        if (isRight()) {
            return new Left<>(get());
        } else {
            return new Right<>(getLeft());
        }
    }

    /**
     * Checks whether this {@code Either} holds a right value equal to {@code element}, as tested by {@link Objects#equals(Object, Object)}.
     *
     * @param element the element to look for, may be {@code null}
     * @return {@code true} if this is {@code Right(element)}, {@code false} otherwise (always for a {@code Left})
     */
    default boolean contains(@Nullable R element) {
        return isRight() && Objects.equals(get(), element);
    }

    /**
     * Checks whether this {@code Either} holds a right value satisfying the given predicate.
     *
     * @param predicate a predicate to test the right value
     * @return {@code true} if this is a {@code Right} and the predicate holds for its value, {@code false} otherwise
     * @throws NullPointerException if {@code predicate} is null
     */
    default boolean exists(Predicate<? super R> predicate) {
        Objects.requireNonNull(predicate, "predicate is null");
        return isRight() && predicate.test(get());
    }

    /**
     * Checks whether the given predicate holds for the right value of this {@code Either}; it holds vacuously for a {@code Left}.
     *
     * @param predicate a predicate to test the right value
     * @return {@code true} if this is a {@code Left} or the predicate holds for the right value, {@code false} otherwise
     * @throws NullPointerException if {@code predicate} is null
     */
    default boolean forAll(Predicate<? super R> predicate) {
        Objects.requireNonNull(predicate, "predicate is null");
        return isLeft() || predicate.test(get());
    }

    /**
     * Performs the given action on the right value if this is a {@link Either.Right}; does nothing for a {@code Left}.
     *
     * @param action a consumer of the right value
     * @throws NullPointerException if {@code action} is null
     */
    default void forEach(Consumer<? super R> action) {
        Objects.requireNonNull(action, "action is null");
        if (isRight()) {
            action.accept(get());
        }
    }

    /**
     * Applies a flat-mapping function to the right value of this right-biased {@code Either}.
     * <p>
     * If this {@code Either} is a {@link Either.Left}, it is returned unchanged.
     * Otherwise, the {@code mapper} function is applied to the right value, and its result is returned.
     * <p>
     * The mapper must return an {@code Either}, never {@code null}; the {@code Either} it builds rejects {@code null} on both sides (design 3.9).
     *
     * @param mapper a function that maps the right value to another {@code Either<L, U>}
     * @param <U>    the type of the right value in the resulting {@code Either}
     * @return this {@code Either} unchanged if it is a {@link Either.Left}, or the result of applying {@code mapper} if it is a {@link Either.Right}
     * @throws NullPointerException if {@code mapper} is null
     */
    @SuppressWarnings("unchecked")
    default <U extends @Nullable Object> Either<L, U> flatMap(Function<? super R, ? extends Either<L, ? extends U>> mapper) {
        Objects.requireNonNull(mapper, "mapper is null");
        if (isRight()) {
            return (Either<L, U>) mapper.apply(get());
        } else {
            return (Either<L, U>) this;
        }
    }

    /**
     * Transforms the right value of this {@code Either} using the given mapping function.
     * <p>
     * If this {@code Either} is a {@link Either.Left}, no operation is performed and it is returned unchanged.
     *
     * <pre>{@code
     * // = Right("A")
     * Either<Integer, String> right = Either.right("a");
     * right.map(String::toUpperCase);
     *
     * // = Left(1)
     * Either<Integer, String> left = Either.left(1);
     * left.map(String::toUpperCase);
     * }</pre>
     * <p>
     * A mapper that returns {@code null} makes this throw {@link NullPointerException}: neither {@code Left} nor {@code Right} holds {@code null} (design 3.9).
     *
     * @param mapper a function to transform the right value
     * @param <U>    the type of the right value in the resulting {@code Either}
     * @return a new {@code Either} with the right value transformed, or the original left value
     * @throws NullPointerException if {@code mapper} is null
     */
    @SuppressWarnings("unchecked")
    default <U extends @Nullable Object> Either<L, U> map(Function<? super R, ? extends U> mapper) {
        Objects.requireNonNull(mapper, "mapper is null");
        if (isRight()) {
            return Either.right(mapper.apply(get()));
        } else {
            return (Either<L, U>) this;
        }
    }

    /**
     * Transforms the left value of this {@code Either} using the given mapping function.
     * <p>
     * If this {@code Either} is a {@link Either.Right}, no operation is performed and it is returned unchanged.
     *
     * <pre>{@code
     * // = Left(2)
     * Either<Integer, String> left = Either.left(1);
     * left.mapLeft(i -> i + 1);
     *
     * // = Right("a")
     * Either<Integer, String> right = Either.right("a");
     * right.mapLeft(i -> i + 1);
     * }</pre>
     * <p>
     * A mapper that returns {@code null} makes this throw {@link NullPointerException}: neither {@code Left} nor {@code Right} holds {@code null} (design 3.9).
     *
     * @param leftMapper a function to transform the left value
     * @param <U>        the type of the left value in the resulting {@code Either}
     * @return a new {@code Either} with the left value transformed, or the original right value
     * @throws NullPointerException if {@code leftMapper} is null
     */
    @SuppressWarnings("unchecked")
    default <U extends @Nullable Object> Either<U, R> mapLeft(Function<? super L, ? extends U> leftMapper) {
        Objects.requireNonNull(leftMapper, "leftMapper is null");
        if (isLeft()) {
            return Either.left(leftMapper.apply(getLeft()));
        } else {
            return (Either<U, R>) this;
        }
    }

    /**
     * Filters this right-biased {@code Either} using the given predicate.
     * <p>
     * If this {@code Either} is a {@link Either.Right} and the predicate evaluates to {@code false},
     * the result is a {@link Either.Left} obtained by applying the {@code zero} function to the right value.
     * If the predicate evaluates to {@code true}, the {@code Either.Right} is returned unchanged.
     * A {@link Either.Left} is returned unchanged and the predicate is not evaluated.
     *
     * <pre>{@code
     * // = Left("bad: a")
     * Either.right("a").filterOrElse(i -> false, val -> "bad: " + val);
     *
     * // = Right("a")
     * Either.right("a").filterOrElse(i -> true, val -> "bad: " + val);
     *
     * // = Left("error"), predicate is not evaluated
     * Either.left("error").filterOrElse(i -> false, val -> "bad: " + val);
     * }</pre>
     *
     * @param predicate a predicate to test the right value
     * @param zero      a function that converts a right value to a left value if the predicate fails
     * @return an {@code Either} containing the right value if the predicate matches, or a left value otherwise
     * @throws NullPointerException if {@code predicate} or {@code zero} is null
     */
    default Either<L, R> filterOrElse(Predicate<? super R> predicate, Function<? super R, ? extends L> zero) {
        Objects.requireNonNull(predicate, "predicate is null");
        Objects.requireNonNull(zero, "zero is null");
        if (isLeft() || predicate.test(get())) {
            return this;
        } else {
            return Either.left(zero.apply(get()));
        }
    }

    /**
     * Runs {@code action} on the right value if this is a {@code Right} and returns this {@code Either} unchanged;
     * does nothing for a {@code Left}. Whatever the action throws propagates to the caller.
     *
     * @param action what to do with the right value
     * @return this {@code Either}
     * @throws NullPointerException if {@code action} is null
     */
    default Either<L, R> tap(Consumer<? super R> action) {
        Objects.requireNonNull(action, "action is null");
        if (isRight()) {
            action.accept(get());
        }
        return this;
    }

    /**
     * Runs {@code action} on the left value if this is a {@code Left} and returns this {@code Either} unchanged;
     * does nothing for a {@code Right}. The counterpart of {@link #tap(Consumer)}.
     *
     * @param action what to do with the left value
     * @return this {@code Either}
     * @throws NullPointerException if {@code action} is null
     */
    default Either<L, R> tapLeft(Consumer<? super L> action) {
        Objects.requireNonNull(action, "action is null");
        if (isLeft()) {
            action.accept(getLeft());
        }
        return this;
    }

    // -- zip (design 3.4)

    /**
     * Pairs this right value with {@code that}'s, failing fast: {@code Right} of the pair when both are
     * {@code Right}, otherwise the first {@code Left} of the two (this one, then {@code that}), as is. The same as
     * {@link #zipWith(Either, BiFunction)} with {@code Tuple::of}.
     * <pre>{@code
     * Either.right(1).zip(Either.right("a")); // = Right((1, a))
     * Either.right(1).zip(Either.left("b"));  // = Left(b)
     * Either.left("a").zip(Either.left("b")); // = Left(a)
     * }</pre>
     *
     * @param that the other side
     * @param <U>  the right type of {@code that}
     * @return {@code Right} of the pair of values, or the first {@code Left}
     * @throws NullPointerException if {@code that} is null
     */
    default <U extends @Nullable Object> Either<L, Tuple2<R, U>> zip(Either<? extends L, ? extends U> that) {
        return zipWith(that, Tuple::of);
    }

    /**
     * Combines this right value with {@code that}'s through {@code f}, failing fast: {@code Right} of the result when
     * both are {@code Right}, otherwise the first {@code Left} of the two (this one, then {@code that}), as is.
     * {@code f} is called only when both are {@code Right}; it must not return {@code null}, since {@code Right}
     * cannot hold {@code null} (design 3.9).
     *
     * @param that the other side
     * @param f    combines the two right values; it must not return {@code null}
     * @param <U>  the right type of {@code that}
     * @param <V>  the result type
     * @return {@code Right} of the combined value, or the first {@code Left}
     * @throws NullPointerException if {@code that} or {@code f} is null, or if {@code f} returns null
     */
    @SuppressWarnings("unchecked")
    default <U extends @Nullable Object, V extends @Nullable Object> Either<L, V> zipWith(Either<? extends L, ? extends U> that, BiFunction<? super R, ? super U, ? extends V> f) {
        Objects.requireNonNull(that, "that is null");
        Objects.requireNonNull(f, "f is null");
        if (isLeft()) {
            return (Either<L, V>) this;
        }
        if (that.isLeft()) {
            return (Either<L, V>) that;
        }
        return right(Objects.requireNonNull(f.apply(get(), that.get()), "Either.zipWith: f returned null"));
    }

    /**
     * {@link #zip(Either)} keeping this right value: this {@code Right} when both are {@code Right}, otherwise the
     * first {@code Left} of the two, as is. Both sides are inspected, so this is not {@link #orElse(Either)}:
     * {@code Right(1).zipLeft(Left("b"))} is {@code Left("b")}.
     *
     * @param that the other side
     * @param <U>  the right type of {@code that}
     * @return this {@code Right}, or the first {@code Left}
     * @throws NullPointerException if {@code that} is null
     */
    @SuppressWarnings("unchecked")
    default <U extends @Nullable Object> Either<L, R> zipLeft(Either<? extends L, ? extends U> that) {
        Objects.requireNonNull(that, "that is null");
        return isLeft() || that.isRight() ? this : (Either<L, R>) that;
    }

    /**
     * {@link #zip(Either)} keeping {@code that}'s right value: {@code that} when both are {@code Right}, otherwise
     * the first {@code Left} of the two, as is. Both sides are inspected: {@code Left("a").zipRight(Right(1))} is
     * {@code Left("a")}.
     *
     * @param that the other side
     * @param <U>  the right type of {@code that}
     * @return {@code that}, or the first {@code Left}
     * @throws NullPointerException if {@code that} is null
     */
    @SuppressWarnings("unchecked")
    default <U extends @Nullable Object> Either<L, U> zipRight(Either<? extends L, ? extends U> that) {
        Objects.requireNonNull(that, "that is null");
        return isLeft() ? (Either<L, U>) this : narrow(that);
    }

    /**
     * Pairs the right values of two {@code Either}s, failing fast: {@code Right} of the tuple of the values when
     * every argument is a {@code Right}, otherwise the first {@code Left} in argument order, as is. The same as
     * {@link #zipWith(Either, Either, BiFunction)} with {@code Tuple::of}.
     *
     * @param e1  the first {@code Either}
     * @param e2  the second {@code Either}
     * @param <L>  the left type, shared by every argument
     * @param <T1> the value type of {@code e1}
     * @param <T2> the value type of {@code e2}
     * @return {@code Right} of the tuple of the values, or the first {@code Left}
     * @throws NullPointerException if any argument is null
     */
    static <L extends @Nullable Object, T1 extends @Nullable Object, T2 extends @Nullable Object> Either<L, Tuple2<T1, T2>> zip(Either<? extends L, ? extends T1> e1, Either<? extends L, ? extends T2> e2) {
        return zipWith(e1, e2, Tuple::of);
    }

    /**
     * Combines the right values of two {@code Either}s through {@code f}, failing fast: {@code Right} of the result
     * when every argument is a {@code Right}, otherwise the first {@code Left} in argument order, as is. {@code f} is
     * called only when every argument is a {@code Right}, with the values in argument order; it must not return
     * {@code null}, since {@code Right} cannot hold {@code null} (design 3.9).
     *
     * @param e1  the first {@code Either}
     * @param e2  the second {@code Either}
     * @param f  combines the values; it must not return {@code null}
     * @param <L>  the left type, shared by every argument
     * @param <T1> the value type of {@code e1}
     * @param <T2> the value type of {@code e2}
     * @param <U>  the result type
     * @return {@code Right} of the combined value, or the first {@code Left}
     * @throws NullPointerException if any argument is null, or if {@code f} returns null
     */
    @SuppressWarnings("unchecked")
    static <L extends @Nullable Object, T1 extends @Nullable Object, T2 extends @Nullable Object, U extends @Nullable Object> Either<L, U> zipWith(Either<? extends L, ? extends T1> e1, Either<? extends L, ? extends T2> e2, BiFunction<? super T1, ? super T2, ? extends U> f) {
        Objects.requireNonNull(e1, "e1 is null");
        Objects.requireNonNull(e2, "e2 is null");
        Objects.requireNonNull(f, "f is null");
        if (e1.isLeft()) {
            return (Either<L, U>) e1;
        }
        if (e2.isLeft()) {
            return (Either<L, U>) e2;
        }
        return right(Objects.requireNonNull(f.apply(e1.get(), e2.get()), "Either.zipWith: f returned null"));
    }

    /**
     * Pairs the right values of three {@code Either}s, failing fast: {@code Right} of the tuple of the values when
     * every argument is a {@code Right}, otherwise the first {@code Left} in argument order, as is. The same as
     * {@link #zipWith(Either, Either, Either, Function3)} with {@code Tuple::of}.
     *
     * @param e1  the first {@code Either}
     * @param e2  the second {@code Either}
     * @param e3  the third {@code Either}
     * @param <L>  the left type, shared by every argument
     * @param <T1> the value type of {@code e1}
     * @param <T2> the value type of {@code e2}
     * @param <T3> the value type of {@code e3}
     * @return {@code Right} of the tuple of the values, or the first {@code Left}
     * @throws NullPointerException if any argument is null
     */
    static <L extends @Nullable Object, T1 extends @Nullable Object, T2 extends @Nullable Object, T3 extends @Nullable Object> Either<L, Tuple3<T1, T2, T3>> zip(Either<? extends L, ? extends T1> e1, Either<? extends L, ? extends T2> e2, Either<? extends L, ? extends T3> e3) {
        return zipWith(e1, e2, e3, Tuple::of);
    }

    /**
     * Combines the right values of three {@code Either}s through {@code f}, failing fast: {@code Right} of the result
     * when every argument is a {@code Right}, otherwise the first {@code Left} in argument order, as is. {@code f} is
     * called only when every argument is a {@code Right}, with the values in argument order; it must not return
     * {@code null}, since {@code Right} cannot hold {@code null} (design 3.9).
     *
     * @param e1  the first {@code Either}
     * @param e2  the second {@code Either}
     * @param e3  the third {@code Either}
     * @param f  combines the values; it must not return {@code null}
     * @param <L>  the left type, shared by every argument
     * @param <T1> the value type of {@code e1}
     * @param <T2> the value type of {@code e2}
     * @param <T3> the value type of {@code e3}
     * @param <U>  the result type
     * @return {@code Right} of the combined value, or the first {@code Left}
     * @throws NullPointerException if any argument is null, or if {@code f} returns null
     */
    @SuppressWarnings("unchecked")
    static <L extends @Nullable Object, T1 extends @Nullable Object, T2 extends @Nullable Object, T3 extends @Nullable Object, U extends @Nullable Object> Either<L, U> zipWith(Either<? extends L, ? extends T1> e1, Either<? extends L, ? extends T2> e2, Either<? extends L, ? extends T3> e3, Function3<? super T1, ? super T2, ? super T3, ? extends U> f) {
        Objects.requireNonNull(e1, "e1 is null");
        Objects.requireNonNull(e2, "e2 is null");
        Objects.requireNonNull(e3, "e3 is null");
        Objects.requireNonNull(f, "f is null");
        if (e1.isLeft()) {
            return (Either<L, U>) e1;
        }
        if (e2.isLeft()) {
            return (Either<L, U>) e2;
        }
        if (e3.isLeft()) {
            return (Either<L, U>) e3;
        }
        return right(Objects.requireNonNull(f.apply(e1.get(), e2.get(), e3.get()), "Either.zipWith: f returned null"));
    }

    /**
     * Pairs the right values of four {@code Either}s, failing fast: {@code Right} of the tuple of the values when
     * every argument is a {@code Right}, otherwise the first {@code Left} in argument order, as is. The same as
     * {@link #zipWith(Either, Either, Either, Either, Function4)} with {@code Tuple::of}.
     *
     * @param e1  the first {@code Either}
     * @param e2  the second {@code Either}
     * @param e3  the third {@code Either}
     * @param e4  the fourth {@code Either}
     * @param <L>  the left type, shared by every argument
     * @param <T1> the value type of {@code e1}
     * @param <T2> the value type of {@code e2}
     * @param <T3> the value type of {@code e3}
     * @param <T4> the value type of {@code e4}
     * @return {@code Right} of the tuple of the values, or the first {@code Left}
     * @throws NullPointerException if any argument is null
     */
    static <L extends @Nullable Object, T1 extends @Nullable Object, T2 extends @Nullable Object, T3 extends @Nullable Object, T4 extends @Nullable Object> Either<L, Tuple4<T1, T2, T3, T4>> zip(Either<? extends L, ? extends T1> e1, Either<? extends L, ? extends T2> e2, Either<? extends L, ? extends T3> e3, Either<? extends L, ? extends T4> e4) {
        return zipWith(e1, e2, e3, e4, Tuple::of);
    }

    /**
     * Combines the right values of four {@code Either}s through {@code f}, failing fast: {@code Right} of the result
     * when every argument is a {@code Right}, otherwise the first {@code Left} in argument order, as is. {@code f} is
     * called only when every argument is a {@code Right}, with the values in argument order; it must not return
     * {@code null}, since {@code Right} cannot hold {@code null} (design 3.9).
     *
     * @param e1  the first {@code Either}
     * @param e2  the second {@code Either}
     * @param e3  the third {@code Either}
     * @param e4  the fourth {@code Either}
     * @param f  combines the values; it must not return {@code null}
     * @param <L>  the left type, shared by every argument
     * @param <T1> the value type of {@code e1}
     * @param <T2> the value type of {@code e2}
     * @param <T3> the value type of {@code e3}
     * @param <T4> the value type of {@code e4}
     * @param <U>  the result type
     * @return {@code Right} of the combined value, or the first {@code Left}
     * @throws NullPointerException if any argument is null, or if {@code f} returns null
     */
    @SuppressWarnings("unchecked")
    static <L extends @Nullable Object, T1 extends @Nullable Object, T2 extends @Nullable Object, T3 extends @Nullable Object, T4 extends @Nullable Object, U extends @Nullable Object> Either<L, U> zipWith(Either<? extends L, ? extends T1> e1, Either<? extends L, ? extends T2> e2, Either<? extends L, ? extends T3> e3, Either<? extends L, ? extends T4> e4, Function4<? super T1, ? super T2, ? super T3, ? super T4, ? extends U> f) {
        Objects.requireNonNull(e1, "e1 is null");
        Objects.requireNonNull(e2, "e2 is null");
        Objects.requireNonNull(e3, "e3 is null");
        Objects.requireNonNull(e4, "e4 is null");
        Objects.requireNonNull(f, "f is null");
        if (e1.isLeft()) {
            return (Either<L, U>) e1;
        }
        if (e2.isLeft()) {
            return (Either<L, U>) e2;
        }
        if (e3.isLeft()) {
            return (Either<L, U>) e3;
        }
        if (e4.isLeft()) {
            return (Either<L, U>) e4;
        }
        return right(Objects.requireNonNull(f.apply(e1.get(), e2.get(), e3.get(), e4.get()), "Either.zipWith: f returned null"));
    }

    /**
     * Pairs the right values of five {@code Either}s, failing fast: {@code Right} of the tuple of the values when
     * every argument is a {@code Right}, otherwise the first {@code Left} in argument order, as is. The same as
     * {@link #zipWith(Either, Either, Either, Either, Either, Function5)} with {@code Tuple::of}.
     *
     * @param e1  the first {@code Either}
     * @param e2  the second {@code Either}
     * @param e3  the third {@code Either}
     * @param e4  the fourth {@code Either}
     * @param e5  the fifth {@code Either}
     * @param <L>  the left type, shared by every argument
     * @param <T1> the value type of {@code e1}
     * @param <T2> the value type of {@code e2}
     * @param <T3> the value type of {@code e3}
     * @param <T4> the value type of {@code e4}
     * @param <T5> the value type of {@code e5}
     * @return {@code Right} of the tuple of the values, or the first {@code Left}
     * @throws NullPointerException if any argument is null
     */
    static <L extends @Nullable Object, T1 extends @Nullable Object, T2 extends @Nullable Object, T3 extends @Nullable Object, T4 extends @Nullable Object, T5 extends @Nullable Object> Either<L, Tuple5<T1, T2, T3, T4, T5>> zip(Either<? extends L, ? extends T1> e1, Either<? extends L, ? extends T2> e2, Either<? extends L, ? extends T3> e3, Either<? extends L, ? extends T4> e4, Either<? extends L, ? extends T5> e5) {
        return zipWith(e1, e2, e3, e4, e5, Tuple::of);
    }

    /**
     * Combines the right values of five {@code Either}s through {@code f}, failing fast: {@code Right} of the result
     * when every argument is a {@code Right}, otherwise the first {@code Left} in argument order, as is. {@code f} is
     * called only when every argument is a {@code Right}, with the values in argument order; it must not return
     * {@code null}, since {@code Right} cannot hold {@code null} (design 3.9).
     *
     * @param e1  the first {@code Either}
     * @param e2  the second {@code Either}
     * @param e3  the third {@code Either}
     * @param e4  the fourth {@code Either}
     * @param e5  the fifth {@code Either}
     * @param f  combines the values; it must not return {@code null}
     * @param <L>  the left type, shared by every argument
     * @param <T1> the value type of {@code e1}
     * @param <T2> the value type of {@code e2}
     * @param <T3> the value type of {@code e3}
     * @param <T4> the value type of {@code e4}
     * @param <T5> the value type of {@code e5}
     * @param <U>  the result type
     * @return {@code Right} of the combined value, or the first {@code Left}
     * @throws NullPointerException if any argument is null, or if {@code f} returns null
     */
    @SuppressWarnings("unchecked")
    static <L extends @Nullable Object, T1 extends @Nullable Object, T2 extends @Nullable Object, T3 extends @Nullable Object, T4 extends @Nullable Object, T5 extends @Nullable Object, U extends @Nullable Object> Either<L, U> zipWith(Either<? extends L, ? extends T1> e1, Either<? extends L, ? extends T2> e2, Either<? extends L, ? extends T3> e3, Either<? extends L, ? extends T4> e4, Either<? extends L, ? extends T5> e5, Function5<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? extends U> f) {
        Objects.requireNonNull(e1, "e1 is null");
        Objects.requireNonNull(e2, "e2 is null");
        Objects.requireNonNull(e3, "e3 is null");
        Objects.requireNonNull(e4, "e4 is null");
        Objects.requireNonNull(e5, "e5 is null");
        Objects.requireNonNull(f, "f is null");
        if (e1.isLeft()) {
            return (Either<L, U>) e1;
        }
        if (e2.isLeft()) {
            return (Either<L, U>) e2;
        }
        if (e3.isLeft()) {
            return (Either<L, U>) e3;
        }
        if (e4.isLeft()) {
            return (Either<L, U>) e4;
        }
        if (e5.isLeft()) {
            return (Either<L, U>) e5;
        }
        return right(Objects.requireNonNull(f.apply(e1.get(), e2.get(), e3.get(), e4.get(), e5.get()), "Either.zipWith: f returned null"));
    }

    /**
     * Pairs the right values of six {@code Either}s, failing fast: {@code Right} of the tuple of the values when
     * every argument is a {@code Right}, otherwise the first {@code Left} in argument order, as is. The same as
     * {@link #zipWith(Either, Either, Either, Either, Either, Either, Function6)} with {@code Tuple::of}.
     *
     * @param e1  the first {@code Either}
     * @param e2  the second {@code Either}
     * @param e3  the third {@code Either}
     * @param e4  the fourth {@code Either}
     * @param e5  the fifth {@code Either}
     * @param e6  the sixth {@code Either}
     * @param <L>  the left type, shared by every argument
     * @param <T1> the value type of {@code e1}
     * @param <T2> the value type of {@code e2}
     * @param <T3> the value type of {@code e3}
     * @param <T4> the value type of {@code e4}
     * @param <T5> the value type of {@code e5}
     * @param <T6> the value type of {@code e6}
     * @return {@code Right} of the tuple of the values, or the first {@code Left}
     * @throws NullPointerException if any argument is null
     */
    static <L extends @Nullable Object, T1 extends @Nullable Object, T2 extends @Nullable Object, T3 extends @Nullable Object, T4 extends @Nullable Object, T5 extends @Nullable Object, T6 extends @Nullable Object> Either<L, Tuple6<T1, T2, T3, T4, T5, T6>> zip(Either<? extends L, ? extends T1> e1, Either<? extends L, ? extends T2> e2, Either<? extends L, ? extends T3> e3, Either<? extends L, ? extends T4> e4, Either<? extends L, ? extends T5> e5, Either<? extends L, ? extends T6> e6) {
        return zipWith(e1, e2, e3, e4, e5, e6, Tuple::of);
    }

    /**
     * Combines the right values of six {@code Either}s through {@code f}, failing fast: {@code Right} of the result
     * when every argument is a {@code Right}, otherwise the first {@code Left} in argument order, as is. {@code f} is
     * called only when every argument is a {@code Right}, with the values in argument order; it must not return
     * {@code null}, since {@code Right} cannot hold {@code null} (design 3.9).
     *
     * @param e1  the first {@code Either}
     * @param e2  the second {@code Either}
     * @param e3  the third {@code Either}
     * @param e4  the fourth {@code Either}
     * @param e5  the fifth {@code Either}
     * @param e6  the sixth {@code Either}
     * @param f  combines the values; it must not return {@code null}
     * @param <L>  the left type, shared by every argument
     * @param <T1> the value type of {@code e1}
     * @param <T2> the value type of {@code e2}
     * @param <T3> the value type of {@code e3}
     * @param <T4> the value type of {@code e4}
     * @param <T5> the value type of {@code e5}
     * @param <T6> the value type of {@code e6}
     * @param <U>  the result type
     * @return {@code Right} of the combined value, or the first {@code Left}
     * @throws NullPointerException if any argument is null, or if {@code f} returns null
     */
    @SuppressWarnings("unchecked")
    static <L extends @Nullable Object, T1 extends @Nullable Object, T2 extends @Nullable Object, T3 extends @Nullable Object, T4 extends @Nullable Object, T5 extends @Nullable Object, T6 extends @Nullable Object, U extends @Nullable Object> Either<L, U> zipWith(Either<? extends L, ? extends T1> e1, Either<? extends L, ? extends T2> e2, Either<? extends L, ? extends T3> e3, Either<? extends L, ? extends T4> e4, Either<? extends L, ? extends T5> e5, Either<? extends L, ? extends T6> e6, Function6<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? super T6, ? extends U> f) {
        Objects.requireNonNull(e1, "e1 is null");
        Objects.requireNonNull(e2, "e2 is null");
        Objects.requireNonNull(e3, "e3 is null");
        Objects.requireNonNull(e4, "e4 is null");
        Objects.requireNonNull(e5, "e5 is null");
        Objects.requireNonNull(e6, "e6 is null");
        Objects.requireNonNull(f, "f is null");
        if (e1.isLeft()) {
            return (Either<L, U>) e1;
        }
        if (e2.isLeft()) {
            return (Either<L, U>) e2;
        }
        if (e3.isLeft()) {
            return (Either<L, U>) e3;
        }
        if (e4.isLeft()) {
            return (Either<L, U>) e4;
        }
        if (e5.isLeft()) {
            return (Either<L, U>) e5;
        }
        if (e6.isLeft()) {
            return (Either<L, U>) e6;
        }
        return right(Objects.requireNonNull(f.apply(e1.get(), e2.get(), e3.get(), e4.get(), e5.get(), e6.get()), "Either.zipWith: f returned null"));
    }

    /**
     * Pairs the right values of seven {@code Either}s, failing fast: {@code Right} of the tuple of the values when
     * every argument is a {@code Right}, otherwise the first {@code Left} in argument order, as is. The same as
     * {@link #zipWith(Either, Either, Either, Either, Either, Either, Either, Function7)} with {@code Tuple::of}.
     *
     * @param e1  the first {@code Either}
     * @param e2  the second {@code Either}
     * @param e3  the third {@code Either}
     * @param e4  the fourth {@code Either}
     * @param e5  the fifth {@code Either}
     * @param e6  the sixth {@code Either}
     * @param e7  the seventh {@code Either}
     * @param <L>  the left type, shared by every argument
     * @param <T1> the value type of {@code e1}
     * @param <T2> the value type of {@code e2}
     * @param <T3> the value type of {@code e3}
     * @param <T4> the value type of {@code e4}
     * @param <T5> the value type of {@code e5}
     * @param <T6> the value type of {@code e6}
     * @param <T7> the value type of {@code e7}
     * @return {@code Right} of the tuple of the values, or the first {@code Left}
     * @throws NullPointerException if any argument is null
     */
    static <L extends @Nullable Object, T1 extends @Nullable Object, T2 extends @Nullable Object, T3 extends @Nullable Object, T4 extends @Nullable Object, T5 extends @Nullable Object, T6 extends @Nullable Object, T7 extends @Nullable Object> Either<L, Tuple7<T1, T2, T3, T4, T5, T6, T7>> zip(Either<? extends L, ? extends T1> e1, Either<? extends L, ? extends T2> e2, Either<? extends L, ? extends T3> e3, Either<? extends L, ? extends T4> e4, Either<? extends L, ? extends T5> e5, Either<? extends L, ? extends T6> e6, Either<? extends L, ? extends T7> e7) {
        return zipWith(e1, e2, e3, e4, e5, e6, e7, Tuple::of);
    }

    /**
     * Combines the right values of seven {@code Either}s through {@code f}, failing fast: {@code Right} of the result
     * when every argument is a {@code Right}, otherwise the first {@code Left} in argument order, as is. {@code f} is
     * called only when every argument is a {@code Right}, with the values in argument order; it must not return
     * {@code null}, since {@code Right} cannot hold {@code null} (design 3.9).
     *
     * @param e1  the first {@code Either}
     * @param e2  the second {@code Either}
     * @param e3  the third {@code Either}
     * @param e4  the fourth {@code Either}
     * @param e5  the fifth {@code Either}
     * @param e6  the sixth {@code Either}
     * @param e7  the seventh {@code Either}
     * @param f  combines the values; it must not return {@code null}
     * @param <L>  the left type, shared by every argument
     * @param <T1> the value type of {@code e1}
     * @param <T2> the value type of {@code e2}
     * @param <T3> the value type of {@code e3}
     * @param <T4> the value type of {@code e4}
     * @param <T5> the value type of {@code e5}
     * @param <T6> the value type of {@code e6}
     * @param <T7> the value type of {@code e7}
     * @param <U>  the result type
     * @return {@code Right} of the combined value, or the first {@code Left}
     * @throws NullPointerException if any argument is null, or if {@code f} returns null
     */
    @SuppressWarnings("unchecked")
    static <L extends @Nullable Object, T1 extends @Nullable Object, T2 extends @Nullable Object, T3 extends @Nullable Object, T4 extends @Nullable Object, T5 extends @Nullable Object, T6 extends @Nullable Object, T7 extends @Nullable Object, U extends @Nullable Object> Either<L, U> zipWith(Either<? extends L, ? extends T1> e1, Either<? extends L, ? extends T2> e2, Either<? extends L, ? extends T3> e3, Either<? extends L, ? extends T4> e4, Either<? extends L, ? extends T5> e5, Either<? extends L, ? extends T6> e6, Either<? extends L, ? extends T7> e7, Function7<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? super T6, ? super T7, ? extends U> f) {
        Objects.requireNonNull(e1, "e1 is null");
        Objects.requireNonNull(e2, "e2 is null");
        Objects.requireNonNull(e3, "e3 is null");
        Objects.requireNonNull(e4, "e4 is null");
        Objects.requireNonNull(e5, "e5 is null");
        Objects.requireNonNull(e6, "e6 is null");
        Objects.requireNonNull(e7, "e7 is null");
        Objects.requireNonNull(f, "f is null");
        if (e1.isLeft()) {
            return (Either<L, U>) e1;
        }
        if (e2.isLeft()) {
            return (Either<L, U>) e2;
        }
        if (e3.isLeft()) {
            return (Either<L, U>) e3;
        }
        if (e4.isLeft()) {
            return (Either<L, U>) e4;
        }
        if (e5.isLeft()) {
            return (Either<L, U>) e5;
        }
        if (e6.isLeft()) {
            return (Either<L, U>) e6;
        }
        if (e7.isLeft()) {
            return (Either<L, U>) e7;
        }
        return right(Objects.requireNonNull(f.apply(e1.get(), e2.get(), e3.get(), e4.get(), e5.get(), e6.get(), e7.get()), "Either.zipWith: f returned null"));
    }

    /**
     * Pairs the right values of eight {@code Either}s, failing fast: {@code Right} of the tuple of the values when
     * every argument is a {@code Right}, otherwise the first {@code Left} in argument order, as is. The same as
     * {@link #zipWith(Either, Either, Either, Either, Either, Either, Either, Either, Function8)} with {@code Tuple::of}.
     *
     * @param e1  the first {@code Either}
     * @param e2  the second {@code Either}
     * @param e3  the third {@code Either}
     * @param e4  the fourth {@code Either}
     * @param e5  the fifth {@code Either}
     * @param e6  the sixth {@code Either}
     * @param e7  the seventh {@code Either}
     * @param e8  the eighth {@code Either}
     * @param <L>  the left type, shared by every argument
     * @param <T1> the value type of {@code e1}
     * @param <T2> the value type of {@code e2}
     * @param <T3> the value type of {@code e3}
     * @param <T4> the value type of {@code e4}
     * @param <T5> the value type of {@code e5}
     * @param <T6> the value type of {@code e6}
     * @param <T7> the value type of {@code e7}
     * @param <T8> the value type of {@code e8}
     * @return {@code Right} of the tuple of the values, or the first {@code Left}
     * @throws NullPointerException if any argument is null
     */
    static <L extends @Nullable Object, T1 extends @Nullable Object, T2 extends @Nullable Object, T3 extends @Nullable Object, T4 extends @Nullable Object, T5 extends @Nullable Object, T6 extends @Nullable Object, T7 extends @Nullable Object, T8 extends @Nullable Object> Either<L, Tuple8<T1, T2, T3, T4, T5, T6, T7, T8>> zip(Either<? extends L, ? extends T1> e1, Either<? extends L, ? extends T2> e2, Either<? extends L, ? extends T3> e3, Either<? extends L, ? extends T4> e4, Either<? extends L, ? extends T5> e5, Either<? extends L, ? extends T6> e6, Either<? extends L, ? extends T7> e7, Either<? extends L, ? extends T8> e8) {
        return zipWith(e1, e2, e3, e4, e5, e6, e7, e8, Tuple::of);
    }

    /**
     * Combines the right values of eight {@code Either}s through {@code f}, failing fast: {@code Right} of the result
     * when every argument is a {@code Right}, otherwise the first {@code Left} in argument order, as is. {@code f} is
     * called only when every argument is a {@code Right}, with the values in argument order; it must not return
     * {@code null}, since {@code Right} cannot hold {@code null} (design 3.9).
     *
     * @param e1  the first {@code Either}
     * @param e2  the second {@code Either}
     * @param e3  the third {@code Either}
     * @param e4  the fourth {@code Either}
     * @param e5  the fifth {@code Either}
     * @param e6  the sixth {@code Either}
     * @param e7  the seventh {@code Either}
     * @param e8  the eighth {@code Either}
     * @param f  combines the values; it must not return {@code null}
     * @param <L>  the left type, shared by every argument
     * @param <T1> the value type of {@code e1}
     * @param <T2> the value type of {@code e2}
     * @param <T3> the value type of {@code e3}
     * @param <T4> the value type of {@code e4}
     * @param <T5> the value type of {@code e5}
     * @param <T6> the value type of {@code e6}
     * @param <T7> the value type of {@code e7}
     * @param <T8> the value type of {@code e8}
     * @param <U>  the result type
     * @return {@code Right} of the combined value, or the first {@code Left}
     * @throws NullPointerException if any argument is null, or if {@code f} returns null
     */
    @SuppressWarnings("unchecked")
    static <L extends @Nullable Object, T1 extends @Nullable Object, T2 extends @Nullable Object, T3 extends @Nullable Object, T4 extends @Nullable Object, T5 extends @Nullable Object, T6 extends @Nullable Object, T7 extends @Nullable Object, T8 extends @Nullable Object, U extends @Nullable Object> Either<L, U> zipWith(Either<? extends L, ? extends T1> e1, Either<? extends L, ? extends T2> e2, Either<? extends L, ? extends T3> e3, Either<? extends L, ? extends T4> e4, Either<? extends L, ? extends T5> e5, Either<? extends L, ? extends T6> e6, Either<? extends L, ? extends T7> e7, Either<? extends L, ? extends T8> e8, Function8<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? super T6, ? super T7, ? super T8, ? extends U> f) {
        Objects.requireNonNull(e1, "e1 is null");
        Objects.requireNonNull(e2, "e2 is null");
        Objects.requireNonNull(e3, "e3 is null");
        Objects.requireNonNull(e4, "e4 is null");
        Objects.requireNonNull(e5, "e5 is null");
        Objects.requireNonNull(e6, "e6 is null");
        Objects.requireNonNull(e7, "e7 is null");
        Objects.requireNonNull(e8, "e8 is null");
        Objects.requireNonNull(f, "f is null");
        if (e1.isLeft()) {
            return (Either<L, U>) e1;
        }
        if (e2.isLeft()) {
            return (Either<L, U>) e2;
        }
        if (e3.isLeft()) {
            return (Either<L, U>) e3;
        }
        if (e4.isLeft()) {
            return (Either<L, U>) e4;
        }
        if (e5.isLeft()) {
            return (Either<L, U>) e5;
        }
        if (e6.isLeft()) {
            return (Either<L, U>) e6;
        }
        if (e7.isLeft()) {
            return (Either<L, U>) e7;
        }
        if (e8.isLeft()) {
            return (Either<L, U>) e8;
        }
        return right(Objects.requireNonNull(f.apply(e1.get(), e2.get(), e3.get(), e4.get(), e5.get(), e6.get(), e7.get(), e8.get()), "Either.zipWith: f returned null"));
    }

    // -- conversions (design 3.2)

    /**
     * Converts this {@code Either} to an {@link Option} of its right value: {@code Some(value)} for a {@code Right},
     * {@code None} for a {@code Left}, whose left value is dropped.
     *
     * @return {@code Option.some(get())} if this is a {@code Right}, otherwise {@code Option.none()}
     */
    default Option<R> toOption() {
        return isRight() ? Option.some(get()) : Option.none();
    }

    /**
     * Converts this {@code Either} to a {@link Try}: {@code Success(value)} for a {@code Right}, a {@code Failure}
     * of the throwable {@code f} builds from the left value for a {@code Left}.
     * <p>
     * Java cannot restrict this method to an {@code Either} whose left type extends {@code Throwable}, so the
     * mapping is always explicit; for such an {@code Either} it is the identity, {@code either.toTry(t -> t)}.
     * The function is applied only to a {@code Left}; it must not return {@code null} nor a fatal throwable (see {@link Try}).
     *
     * @param f a function from the left value to the failure cause
     * @return a {@code Success} of the right value, or a {@code Failure} of {@code f.apply(getLeft())}
     * @throws NullPointerException if {@code f} is null, or if it returns {@code null} for a {@code Left}
     */
    default Try<R> toTry(Function<? super L, ? extends Throwable> f) {
        Objects.requireNonNull(f, "f is null");
        return isRight() ? Try.success(get()) : Try.failure(f.apply(getLeft()));
    }

    /**
     * Converts this {@code Either} to a {@link Validation}: {@code Valid(value)} for a {@code Right}, {@code Invalid(left)} for a {@code Left}.
     *
     * @return {@code Validation.valid(get())} if this is a {@code Right}, otherwise {@code Validation.invalid(getLeft())}
     */
    default Validation<L, R> toValidation() {
        return isRight() ? Validation.valid(get()) : Validation.invalid(getLeft());
    }

    /**
     * Converts this {@code Either} to a {@link Vector} of zero or one element: its right value, if any.
     *
     * @return {@code Vector.of(get())} if this is a {@code Right}, otherwise the empty {@code Vector}
     */
    default Vector<R> toVector() {
        return isRight() ? Vector.of(get()) : Vector.empty();
    }

    // -- Object.*

    @Override
    boolean equals(@Nullable Object o);

    @Override
    int hashCode();

    @Override
    String toString();

    /**
     * The {@code Left} case of an {@code Either}. The value is never {@code null}.
     *
     * @param value the left value, never {@code null}
     * @param <L>   left component type
     * @param <R>   right component type
     */
    record Left<L extends @Nullable Object, R extends @Nullable Object>(L value) implements Either<L, R> {

        /**
         * Rejects {@code null}.
         *
         * @throws NullPointerException if {@code value} is null
         */
        public Left {
            Objects.requireNonNull(value, "value is null");
        }

        @Override
        public R get() {
            throw new NoSuchElementException("get() on Left");
        }

        @Override
        public L getLeft() {
            return value;
        }

        @Override
        public boolean isLeft() {
            return true;
        }

        @Override
        public boolean isRight() {
            return false;
        }

        @Override
        public String toString() {
            return "Left(" + value + ")";
        }
    }

    /**
     * The {@code Right} case of an {@code Either}. The value is never {@code null}.
     *
     * @param value the right value, never {@code null}
     * @param <L>   left component type
     * @param <R>   right component type
     */
    record Right<L extends @Nullable Object, R extends @Nullable Object>(R value) implements Either<L, R> {

        /**
         * Rejects {@code null}.
         *
         * @throws NullPointerException if {@code value} is null
         */
        public Right {
            Objects.requireNonNull(value, "value is null");
        }

        @Override
        public R get() {
            return value;
        }

        @Override
        public L getLeft() {
            throw new NoSuchElementException("getLeft() on Right");
        }

        @Override
        public boolean isLeft() {
            return false;
        }

        @Override
        public boolean isRight() {
            return true;
        }

        @Override
        public String toString() {
            return "Right(" + value + ")";
        }
    }
}
