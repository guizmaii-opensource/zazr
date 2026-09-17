package com.guizmaii.zazr.control;

import com.guizmaii.zazr.collection.Iterator;
import com.guizmaii.zazr.collection.Seq;
import com.guizmaii.zazr.collection.Vector;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import org.jspecify.annotations.NonNull;
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
 * {@link #mapLeft(Function)}, {@link #swap()} and {@link #fold(Function, Function)}.
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
     * Returns an {@code Either<L, R>} based on the given test condition.
     * <ul>
     *   <li>If {@code test} is {@code true}, the result is a {@link Either.Right} created from {@code right}.</li>
     *   <li>If {@code test} is {@code false}, the result is a {@link Either.Left} created from {@code left}.</li>
     * </ul>
     *
     * @param test  the boolean condition to evaluate
     * @param right a {@code Supplier<? extends R>} providing the right value if {@code test} is true
     * @param left  a {@code Supplier<? extends L>} providing the left value if {@code test} is false
     * @param <L>   the type of the left value
     * @param <R>   the type of the right value
     * @return an {@code Either<L, R>} containing the left or right value depending on {@code test}
     * @throws NullPointerException if any argument is null
     */
    static <L extends @Nullable Object, R extends @Nullable Object> Either<L, R> cond(boolean test, Supplier<? extends R> right, Supplier<? extends L> left) {
        Objects.requireNonNull(right, "right is null");
        Objects.requireNonNull(left, "left is null");

        return test ? right(right.get()) : left(left.get());
    }

    /**
     * Returns an {@code Either<L, R>} based on the given test condition.
     * <ul>
     *   <li>If {@code test} is {@code true}, the result is a {@link Either.Right} containing {@code right}.</li>
     *   <li>If {@code test} is {@code false}, the result is a {@link Either.Left} containing {@code left}.</li>
     * </ul>
     *
     * @param test  the boolean condition to evaluate
     * @param right the {@code R} value to return if {@code test} is true
     * @param left  the {@code L} value to return if {@code test} is false
     * @param <L>   the type of the left value
     * @param <R>   the type of the right value
     * @return an {@code Either<L, R>} containing either the left or right value depending on {@code test}
     * @throws NullPointerException if any argument is null
     */
    static <L extends @Nullable Object, R extends @Nullable Object> Either<L, R> cond(boolean test, @NonNull R right, @NonNull L left) {
        Objects.requireNonNull(right, "right is null");
        Objects.requireNonNull(left, "left is null");

        return cond(test, () -> right, () -> left);
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
     * Transforms the value of this {@code Either} by applying one of the given mapping functions.
     * <ul>
     *   <li>If this is a {@link Either.Left}, {@code leftMapper} is applied to the left value.</li>
     *   <li>If this is a {@link Either.Right}, {@code rightMapper} is applied to the right value.</li>
     * </ul>
     * <p>
     * A mapper that returns {@code null} makes this throw {@link NullPointerException}: neither {@code Left} nor {@code Right} holds {@code null} (design 3.9).
     *
     * @param leftMapper  function to transform the left value if this is a {@code Left}
     * @param rightMapper function to transform the right value if this is a {@code Right}
     * @param <X>         the type of the left value in the resulting {@code Either}
     * @param <Y>         the type of the right value in the resulting {@code Either}
     * @return a new {@code Either} instance with the transformed value
     */
    default <X extends @Nullable Object, Y extends @Nullable Object> Either<X, Y> bimap(Function<? super L, ? extends X> leftMapper, Function<? super R, ? extends Y> rightMapper) {
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
     * Transforms an {@link Iterable} of {@code Either<L, R>} into a single {@code Either<Seq<L>, Seq<R>>}.
     * <p>
     * If any of the given {@code Either}s is a {@link Either.Left}, the result is a {@link Either.Left}
     * containing a non-empty {@link Seq} of all left values.
     * <p>
     * If all of the given {@code Either}s are {@link Either.Right}, the result is a {@link Either.Right}
     * containing a (possibly empty) {@link Seq} of all right values.
     *
     * <pre>{@code
     * // = Right(Seq())
     * Either.sequence(List.empty())
     *
     * // = Right(Seq(1, 2))
     * Either.sequence(List.of(Either.right(1), Either.right(2)))
     *
     * // = Left(Seq("x"))
     * Either.sequence(List.of(Either.right(1), Either.left("x")))
     * }</pre>
     *
     * @param eithers an {@link Iterable} of {@code Either} instances
     * @param <L>     the common type of left values
     * @param <R>     the common type of right values
     * @return an {@code Either} containing a {@link Seq} of left values if any {@code Either} was a {@link Either.Left},
     *         otherwise a {@link Seq} of right values
     * @throws NullPointerException if {@code eithers} is null
     */
    @SuppressWarnings("unchecked")
    static <L extends @Nullable Object, R extends @Nullable Object> Either<Seq<L>, Seq<R>> sequence(Iterable<? extends Either<? extends L, ? extends R>> eithers) {
        Objects.requireNonNull(eithers, "eithers is null");
        return Iterator.ofAll((Iterable<Either<L, R>>) eithers)
          .partition(Either::isLeft)
          .apply((leftPartition, rightPartition) -> leftPartition.hasNext()
            ? Either.left(leftPartition.map(Either::getLeft).toVector())
            : Either.right(rightPartition.map(Either::get).toVector())
          );
    }

    /**
     * Transforms an {@link Iterable} of values into a single {@code Either<Seq<L>, Seq<R>>} by applying a mapping function
     * that returns an {@code Either} for each value.
     * <p>
     * If the mapper returns any {@link Either.Left}, the resulting {@code Either} is a {@link Either.Left}
     * containing a {@link Seq} of all left values. Otherwise, the result is a {@link Either.Right} containing
     * a {@link Seq} of all right values.
     *
     * @param values an {@code Iterable} of values to map
     * @param mapper a function mapping each value to an {@code Either<L, R>}
     * @param <L>    the type of left values
     * @param <R>    the type of right values
     * @param <T>    the type of the input values
     * @return a single {@code Either} containing a {@link Seq} of left or right results
     * @throws NullPointerException if {@code values} or {@code mapper} is null
     */
    static <L extends @Nullable Object, R extends @Nullable Object, T extends @Nullable Object> Either<Seq<L>, Seq<R>> traverse(Iterable<? extends T> values, Function<? super T, ? extends Either<? extends L, ? extends R>> mapper) {
        Objects.requireNonNull(values, "values is null");
        Objects.requireNonNull(mapper, "mapper is null");
        return sequence(Iterator.ofAll(values).map(mapper));
    }

    /**
     * Transforms an {@link Iterable} of {@code Either<L, R>} into a single {@code Either<L, Seq<R>>}.
     * <p>
     * If any of the given {@code Either}s is a {@link Either.Left}, the result is a {@link Either.Left}
     * containing the first left value encountered in iteration order.
     * <p>
     * If all of the given {@code Either}s are {@link Either.Right}, the result is a {@link Either.Right}
     * containing a (possibly empty) {@link Seq} of all right values.
     *
     * <pre>{@code
     * // = Right(Seq())
     * Either.sequenceRight(List.empty())
     *
     * // = Right(Seq(1, 2))
     * Either.sequenceRight(List.of(Either.right(1), Either.right(2)))
     *
     * // = Left("x1")
     * Either.sequenceRight(List.of(Either.right(1), Either.left("x1"), Either.left("x2")))
     * }</pre>
     *
     * @param eithers an {@link Iterable} of {@code Either} instances
     * @param <L>     the type of left values
     * @param <R>     the type of right values
     * @return an {@code Either} containing either the first left value if present, or a {@link Seq} of all right values
     * @throws NullPointerException if {@code eithers} is null
     */
    static <L extends @Nullable Object, R extends @Nullable Object> Either<L, Seq<R>> sequenceRight(Iterable<? extends Either<? extends L, ? extends R>> eithers) {
        Objects.requireNonNull(eithers, "eithers is null");
        Vector<R> rightValues = Vector.empty();
        for (Either<? extends L, ? extends R> either : eithers) {
            if (either.isRight()) {
                rightValues = rightValues.append(either.get());
            } else {
                return Either.left(either.getLeft());
            }
        }
        return Either.right(rightValues);
    }

    /**
     * Transforms an {@link Iterable} of values into a single {@code Either<L, Seq<R>>} by applying a mapping
     * function that returns an {@code Either} for each element.
     * <p>
     * If the mapper returns any {@link Either.Left}, the result is a {@link Either.Left} containing the first
     * left value encountered in iteration order.
     * <p>
     * If the mapper returns only {@link Either.Right}s, the result is a {@link Either.Right} containing a
     * (possibly empty) {@link Seq} of all right values.
     *
     * @param values an {@code Iterable} of values to map
     * @param mapper a function mapping each value to an {@code Either<L, R>}
     * @param <L>    the type of left values
     * @param <R>    the type of right values
     * @param <T>    the type of the input values
     * @return an {@code Either} containing either the first left value if present, or a {@link Seq} of all right values
     * @throws NullPointerException if {@code values} or {@code mapper} is null
     */
    static <L extends @Nullable Object, R extends @Nullable Object, T extends @Nullable Object> Either<L, Seq<R>> traverseRight(Iterable<? extends T> values, Function<? super T, ? extends Either<? extends L, ? extends R>> mapper) {
        Objects.requireNonNull(values, "values is null");
        Objects.requireNonNull(mapper, "mapper is null");
        return sequenceRight(Iterator.ofAll(values).map(mapper));
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
     * Returns the right value of this {@code Either}, or an alternative value if this is a {@link Either.Left}.
     *
     * @param other a function that converts a left value to an alternative right value
     * @return the right value if present, otherwise the alternative value produced by applying {@code other} to the left value
     */
    default R getOrElseGet(Function<? super L, ? extends R> other) {
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
     * Executes the given action on the left value if this {@code Either} is a {@link Either.Left}; does nothing
     * if it is a {@link Either.Right}.
     *
     * @param action a consumer that processes the left value
     */
    default void orElseRun(Consumer<? super L> action) {
        Objects.requireNonNull(action, "action is null");
        if (isLeft()) {
            action.accept(getLeft());
        }
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
     * Swaps the sides of this {@code Either}, converting a {@link Either.Left} to a {@link Either.Right}
     * and vice versa.
     *
     * @return a new {@code Either} with the left and right values swapped
     */
    default Either<R, L> swap() {
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
     * Transforms this {@code Either} into a value of type {@code U} using the given function.
     *
     * <pre>{@code
     * // = "R:1"
     * Either.right(1).transform(e -> e.fold(l -> "L:" + l, r -> "R:" + r));
     *
     * // = "L:error"
     * Either.left("error").transform(e -> e.fold(l -> "L:" + l, r -> "R:" + r));
     * }</pre>
     *
     * @param f   a function to transform this {@code Either}
     * @param <U> the type of the result
     * @return the result of applying {@code f} to this {@code Either}
     * @throws NullPointerException if {@code f} is null
     */
    default <U extends @Nullable Object> U transform(Function<? super Either<L, R>, ? extends U> f) {
        Objects.requireNonNull(f, "f is null");
        return f.apply(this);
    }

    /**
     * Filters this right-biased {@code Either} by testing the given predicate against the right value.
     * <p>
     * If this {@code Either} is a {@link Either.Left}, or a {@link Either.Right} whose value satisfies
     * the predicate, {@code Option.some(this)} is returned. {@link Option#none()} is returned only if
     * this is a {@link Either.Right} whose value does not satisfy the predicate.
     * <p>
     * Note that a {@code Left} always passes the filter unchanged and the predicate is not evaluated.
     * Like {@code map} and {@code flatMap}, {@code filter} operates on the right side only and never
     * discards a {@code Left}: the resulting {@link Option#none()} carries no left value, so mapping a
     * {@code Left} to it would silently lose the left value and make a rejected right value
     * indistinguishable from an already-present {@code Left}.
     *
     * <pre>{@code
     * // = Some(Right(42))
     * Either.right(42).filter(i -> i > 0);
     *
     * // = None
     * Either.right(42).filter(i -> i < 0);
     *
     * // = Some(Left("error")), predicate is not evaluated
     * Either.left("error").filter(i -> false);
     * }</pre>
     *
     * To fall back to a {@code Left} instead of {@code None} when the predicate rejects the right value,
     * use {@link #filterOrElse(Predicate, Function)}. To obtain {@code None} for a {@code Left} as well,
     * use {@code either.toOption().filter(predicate)}.
     *
     * @param predicate a predicate to test the right value
     * @return {@code Option.some(this)} if this is a {@code Left} or the right value satisfies the predicate, {@link Option#none()} otherwise
     * @throws NullPointerException if {@code predicate} is null
     * @see #filterOrElse(Predicate, Function)
     */
    default Option<Either<L, R>> filter(Predicate<? super R> predicate) {
        Objects.requireNonNull(predicate, "predicate is null");
        return isLeft() || predicate.test(get()) ? Option.some(this) : Option.none();
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
     * Performs the given action on the right value if this is a {@link Either.Right}; does nothing for a {@code Left}.
     *
     * @param action a consumer of the right value
     * @return this {@code Either}
     * @throws NullPointerException if {@code action} is null
     */
    default Either<L, R> peek(Consumer<? super R> action) {
        Objects.requireNonNull(action, "action is null");
        if (isRight()) {
            action.accept(get());
        }
        return this;
    }

    /**
     * Performs the given action on the left value if this is a {@link Either.Left}.
     * <p>
     * If this is a {@link Either.Right}, no action is performed.
     *
     * @param action a consumer that processes the left value
     * @return this {@code Either}
     */
    default Either<L, R> peekLeft(Consumer<? super L> action) {
        Objects.requireNonNull(action, "action is null");
        if (isLeft()) {
            action.accept(getLeft());
        }
        return this;
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
