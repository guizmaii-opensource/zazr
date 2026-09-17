package com.guizmaii.zazr.control;

import com.guizmaii.zazr.collection.NonEmptyVector;
import com.guizmaii.zazr.collection.Vector;
import com.guizmaii.zazr.control.Try.Failure;
import com.guizmaii.zazr.control.Try.Success;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import org.jspecify.annotations.Nullable;

/**
 * A validation that either succeeds with a value of type {@code A} or fails with <em>one or more</em> errors of type
 * {@code E}. Modelled on zio-prelude's {@code Validation}, minus its log channel (design 3.5).
 * <p>
 * Unlike {@link Either}, which stops at the first error, {@code Validation} keeps <em>all</em> errors: combining two
 * invalid values with {@link #zip(Validation)} concatenates their errors, which is what a form or a configuration
 * wants: every problem reported at once. The errors live in a {@link NonEmptyVector}, so an {@code Invalid} carries
 * at least one and their order is the order in which they were accumulated (the field order, usually).
 * <p>
 * {@code Validation} is a sealed interface with two record cases, {@link Valid} and {@link Invalid}, so it is
 * eliminated with an exhaustive {@code switch}:
 * <pre>{@code
 * String s = switch (validation) {
 *     case Valid(var value) -> "ok: " + value;
 *     case Invalid(var errors) -> "rejected: " + errors.mkString(", ");
 * };
 * }</pre>
 * Neither case holds {@code null}: {@link #valid(Object)}, {@link #invalid(Object)} and
 * {@link #invalidAll(NonEmptyVector)} throw.
 * <p>
 * Two ways to combine validations, with different semantics:
 * <ul>
 * <li>{@link #zip(Validation)}, {@link #zipWith(Validation, BiFunction)}, {@link #collectAll(Iterable)},
 * {@link #forEach(Iterable, Function)} <strong>accumulate</strong>: every operand is evaluated and every error is
 * kept.</li>
 * <li>{@link #flatMap(Function)} and {@link #flatMapEither(Function)} <strong>short-circuit</strong>: the function is
 * not called when this is {@code Invalid}, and its errors are never joined with this one's. Use them for a step that
 * needs the previous value (a cross-field rule after the fields have been validated), never to accumulate.</li>
 * </ul>
 * <pre>{@code
 * Validation<String, Integer> age = Validation.fromPredicate(a, x -> x >= 0, x -> "age " + x + " is negative");
 * Validation<String, String> name = Validation.fromPredicate(n, s -> !s.isBlank(), s -> "name is blank");
 * Validation<String, Person> person = age.zipWith(name, Person::new);   // Invalid("age -1 is negative", "name is blank")
 * }</pre>
 * <p>
 * The sides are not symmetric (one is non-empty), so there is no {@code flip}. Equality is the record equality, order
 * sensitive on the errors: {@code Invalid(a, b)} is not {@code Invalid(b, a)}.
 * <p>
 * A {@code Validation} is not a collection and not {@link Iterable} (design 3.2): to iterate its value, convert it
 * explicitly with {@link #toVector()} or {@link #toOption()}.
 *
 * @param <E> the error type
 * @param <A> the value type
 */
public sealed interface Validation<E extends @Nullable Object, A extends @Nullable Object> permits Validation.Valid, Validation.Invalid {

    // -- constructors

    /**
     * Creates a {@link Valid} holding {@code value}.
     *
     * @param value the value, must not be {@code null}
     * @param <E>   the error type
     * @param <A>   the value type
     * @return {@code Valid(value)}
     * @throws NullPointerException if {@code value} is null
     */
    static <E extends @Nullable Object, A extends @Nullable Object> Validation<E, A> valid(A value) {
        return new Valid<>(value);
    }

    /**
     * Creates an {@link Invalid} holding the single error {@code error}.
     *
     * @param error the error, must not be {@code null}
     * @param <E>   the error type
     * @param <A>   the value type
     * @return {@code Invalid(error)}
     * @throws NullPointerException if {@code error} is null
     */
    static <E extends @Nullable Object, A extends @Nullable Object> Validation<E, A> invalid(E error) {
        Objects.requireNonNull(error, "error is null");
        return new Invalid<>(NonEmptyVector.single(error));
    }

    /**
     * Creates an {@link Invalid} holding {@code errors}, in their order.
     *
     * @param errors the errors, must not be {@code null}
     * @param <E>    the error type
     * @param <A>    the value type
     * @return {@code Invalid(errors)}
     * @throws NullPointerException if {@code errors} is null
     */
    static <E extends @Nullable Object, A extends @Nullable Object> Validation<E, A> invalidAll(NonEmptyVector<E> errors) {
        return new Invalid<>(errors);
    }

    /**
     * Converts an {@link Either}: {@code Valid(value)} for a {@code Right}, {@code Invalid} of the single left value
     * for a {@code Left}.
     *
     * @param either the {@code Either} to convert
     * @param <E>    the error type
     * @param <A>    the value type
     * @return the {@code Validation} equivalent of {@code either}
     * @throws NullPointerException if {@code either} is null
     */
    static <E extends @Nullable Object, A extends @Nullable Object> Validation<E, A> fromEither(Either<? extends E, ? extends A> either) {
        Objects.requireNonNull(either, "either is null");
        return either.isRight() ? valid(either.get()) : invalid(either.getLeft());
    }

    /**
     * Converts an {@link Option}: {@code Valid(value)} for a {@code Some}, {@code Invalid} of the supplied error for
     * {@code None}. The supplier is called only for {@code None}.
     *
     * @param option the {@code Option} to convert
     * @param ifNone supplies the error for {@code None}; it must not return {@code null}
     * @param <E>    the error type
     * @param <A>    the value type
     * @return the {@code Validation} equivalent of {@code option}
     * @throws NullPointerException if {@code option} or {@code ifNone} is null, or if {@code ifNone} supplies null
     */
    static <E extends @Nullable Object, A extends @Nullable Object> Validation<E, A> fromOption(Option<? extends A> option, Supplier<? extends E> ifNone) {
        Objects.requireNonNull(option, "option is null");
        Objects.requireNonNull(ifNone, "ifNone is null");
        return option.isDefined() ? valid(option.get()) : invalid(ifNone.get());
    }

    /**
     * Tests {@code value} with {@code predicate}: {@code Valid(value)} if it holds, {@code Invalid(ifFalse.apply(value))}
     * if it does not. The function is called only when the predicate fails and receives the rejected value, so the
     * error can name it.
     * <pre>{@code
     * Validation.fromPredicate(age, a -> a >= 18, a -> a + " is under 18"); // = Valid(age) or Invalid("17 is under 18")
     * }</pre>
     *
     * @param value     the value to test, must not be {@code null}
     * @param predicate the condition the value has to satisfy
     * @param ifFalse   builds the error from the rejected value; it must not return {@code null}
     * @param <E>       the error type
     * @param <A>       the value type
     * @return {@code Valid(value)} if the predicate holds, otherwise {@code Invalid} of the built error
     * @throws NullPointerException if any argument is null, or if {@code ifFalse} returns null
     */
    static <E extends @Nullable Object, A extends @Nullable Object> Validation<E, A> fromPredicate(A value, Predicate<? super A> predicate, Function<? super A, ? extends E> ifFalse) {
        Objects.requireNonNull(value, "value is null");
        Objects.requireNonNull(predicate, "predicate is null");
        Objects.requireNonNull(ifFalse, "ifFalse is null");
        return predicate.test(value) ? valid(value) : invalid(ifFalse.apply(value));
    }

    /**
     * Converts a {@link Try}: {@code Valid(value)} for a {@code Success}, {@code Invalid} of the single cause for a
     * {@code Failure}.
     *
     * @param t   the {@code Try} to convert
     * @param <A> the value type
     * @return the {@code Validation} equivalent of {@code t}
     * @throws NullPointerException if {@code t} is null
     */
    static <A extends @Nullable Object> Validation<Throwable, A> fromTry(Try<? extends A> t) {
        Objects.requireNonNull(t, "t is null");
        return t.isSuccess() ? valid(t.get()) : invalid(t.getCause());
    }

    /**
     * Runs {@code f}: {@code Valid} of its result, or {@code Invalid} of the single error built by {@code onError}
     * from what it threw. Exactly the outcomes of {@link Try#of(Callable)}: a fatal throwable is rethrown, a
     * {@code null} result is a {@link NullPointerException} handed to {@code onError}.
     * <pre>{@code
     * Validation.of(() -> Integer.parseInt(s), t -> "not a number: " + s);
     * }</pre>
     *
     * @param f       the computation to run
     * @param onError builds the error from the throwable; it must not return {@code null}
     * @param <E>     the error type
     * @param <A>     the value type
     * @return {@code Valid} of the result or {@code Invalid} of the built error
     * @throws NullPointerException if {@code f} or {@code onError} is null, or if {@code onError} returns null
     */
    static <E extends @Nullable Object, A extends @Nullable Object> Validation<E, A> of(Callable<? extends A> f, Function<? super Throwable, ? extends E> onError) {
        Objects.requireNonNull(f, "f is null");
        Objects.requireNonNull(onError, "onError is null");
        return switch (Try.of(f)) {
            case Success(var value) -> valid(value);
            case Failure(var cause) -> invalid(onError.apply(cause));
        };
    }

    // -- transform / eliminate

    /**
     * Applies {@code f} to the value of a {@code Valid}; an {@code Invalid} is returned unchanged.
     *
     * @param f   the mapper; it must not return {@code null}
     * @param <B> the value type of the result
     * @return {@code Valid(f.apply(value))}, or this {@code Invalid}
     * @throws NullPointerException if {@code f} is null, or if it returns null
     */
    @SuppressWarnings("unchecked")
    default <B extends @Nullable Object> Validation<E, B> map(Function<? super A, ? extends B> f) {
        Objects.requireNonNull(f, "f is null");
        return switch (this) {
            case Valid(var value) -> valid(f.apply(value));
            case Invalid<E, A> invalid -> (Validation<E, B>) invalid;
        };
    }

    /**
     * Applies {@code f} to <em>each</em> error of an {@code Invalid}; a {@code Valid} is returned unchanged.
     *
     * @param f    the mapper; it must not return {@code null}
     * @param <E2> the error type of the result
     * @return {@code Invalid} of the mapped errors, or this {@code Valid}
     * @throws NullPointerException if {@code f} is null, or if it returns null for an error
     */
    @SuppressWarnings("unchecked")
    default <E2 extends @Nullable Object> Validation<E2, A> mapError(Function<? super E, ? extends E2> f) {
        Objects.requireNonNull(f, "f is null");
        return switch (this) {
            case Valid<E, A> valid -> (Validation<E2, A>) valid;
            case Invalid(var errors) -> new Invalid<>(errors.map(f));
        };
    }

    /**
     * Applies {@code f} to the errors of an {@code Invalid} as a whole; a {@code Valid} is returned unchanged. The
     * result stays non-empty by construction: {@code f} may merge, reorder or add errors, but cannot make the
     * validation valid.
     *
     * @param f    the mapper; it must not return {@code null}
     * @param <E2> the error type of the result
     * @return {@code Invalid(f.apply(errors))}, or this {@code Valid}
     * @throws NullPointerException if {@code f} is null, or if it returns null
     */
    @SuppressWarnings("unchecked")
    default <E2 extends @Nullable Object> Validation<E2, A> mapErrorAll(Function<? super NonEmptyVector<E>, ? extends NonEmptyVector<E2>> f) {
        Objects.requireNonNull(f, "f is null");
        return switch (this) {
            case Valid<E, A> valid -> (Validation<E2, A>) valid;
            case Invalid(var errors) -> new Invalid<>(f.apply(errors));
        };
    }

    /**
     * {@link #mapError(Function)} and {@link #map(Function)} at once; only the side that applies is called.
     *
     * @param errorMapper applied to each error of an {@code Invalid}; it must not return {@code null}
     * @param valueMapper applied to the value of a {@code Valid}; it must not return {@code null}
     * @param <E2>        the error type of the result
     * @param <B>         the value type of the result
     * @return the mapped {@code Validation}
     * @throws NullPointerException if a mapper is null, or if the applied one returns null
     */
    default <E2 extends @Nullable Object, B extends @Nullable Object> Validation<E2, B> mapBoth(Function<? super E, ? extends E2> errorMapper, Function<? super A, ? extends B> valueMapper) {
        Objects.requireNonNull(errorMapper, "errorMapper is null");
        Objects.requireNonNull(valueMapper, "valueMapper is null");
        return switch (this) {
            case Valid(var value) -> valid(valueMapper.apply(value));
            case Invalid(var errors) -> new Invalid<>(errors.map(errorMapper));
        };
    }

    /**
     * Eliminates this {@code Validation}: {@code ifInvalid} on the errors, or {@code ifValid} on the value. The
     * argument order is the ZIO one, failure first.
     *
     * @param ifInvalid applied to the errors of an {@code Invalid}
     * @param ifValid   applied to the value of a {@code Valid}
     * @param <B>       the result type
     * @return the result of the function that applies
     * @throws NullPointerException if a function is null
     */
    default <B extends @Nullable Object> B fold(Function<? super NonEmptyVector<E>, ? extends B> ifInvalid, Function<? super A, ? extends B> ifValid) {
        Objects.requireNonNull(ifInvalid, "ifInvalid is null");
        Objects.requireNonNull(ifValid, "ifValid is null");
        return switch (this) {
            case Valid(var value) -> ifValid.apply(value);
            case Invalid(var errors) -> ifInvalid.apply(errors);
        };
    }

    /**
     * Returns the value if this is {@code Valid}; otherwise throws.
     *
     * @return the value
     * @throws NoSuchElementException if this is {@code Invalid}
     */
    A get();

    /**
     * Returns the value if this is {@code Valid}, otherwise {@code other}.
     *
     * @param other the alternative, may be {@code null}
     * @return the value or {@code other}
     */
    default A getOrElse(A other) {
        return switch (this) {
            case Valid(var value) -> value;
            case Invalid<E, A> _ -> other;
        };
    }

    /**
     * Returns the value if this is {@code Valid}, otherwise {@code other} applied to the errors.
     *
     * @param other builds the alternative from the errors; called only for an {@code Invalid}
     * @return the value or {@code other.apply(errors)}
     * @throws NullPointerException if {@code other} is null
     */
    default A getOrElse(Function<? super NonEmptyVector<E>, ? extends A> other) {
        Objects.requireNonNull(other, "other is null");
        return switch (this) {
            case Valid(var value) -> value;
            case Invalid(var errors) -> other.apply(errors);
        };
    }

    /**
     * Returns the value if this is {@code Valid}, otherwise throws the throwable built from the errors.
     *
     * @param exceptionFunction builds the throwable from the errors; called only for an {@code Invalid}
     * @param <X>               the type of the throwable
     * @return the value
     * @throws X                    if this is {@code Invalid}
     * @throws NullPointerException if {@code exceptionFunction} is null
     */
    default <X extends Throwable> A getOrElseThrow(Function<? super NonEmptyVector<E>, X> exceptionFunction) throws X {
        Objects.requireNonNull(exceptionFunction, "exceptionFunction is null");
        return switch (this) {
            case Valid(var value) -> value;
            case Invalid(var errors) -> throw exceptionFunction.apply(errors);
        };
    }

    /**
     * Runs {@code action} on the value of a {@code Valid} and returns this {@code Validation} unchanged; does nothing
     * for an {@code Invalid}.
     *
     * @param action what to do with the value
     * @return this {@code Validation}
     * @throws NullPointerException if {@code action} is null
     */
    default Validation<E, A> tap(Consumer<? super A> action) {
        Objects.requireNonNull(action, "action is null");
        if (this instanceof Valid(var value)) {
            action.accept(value);
        }
        return this;
    }

    /**
     * Runs {@code action} on the errors of an {@code Invalid} and returns this {@code Validation} unchanged; does
     * nothing for a {@code Valid}. The counterpart of {@link #tap(Consumer)}.
     *
     * @param action what to do with the errors
     * @return this {@code Validation}
     * @throws NullPointerException if {@code action} is null
     */
    default Validation<E, A> tapError(Consumer<? super NonEmptyVector<E>> action) {
        Objects.requireNonNull(action, "action is null");
        if (this instanceof Invalid(var errors)) {
            action.accept(errors);
        }
        return this;
    }

    /**
     * @return {@code true} if this is a {@link Valid}, {@code false} otherwise
     */
    boolean isValid();

    /**
     * @return {@code true} if this is an {@link Invalid}, {@code false} otherwise
     */
    boolean isInvalid();

    // -- conversions

    /**
     * Converts this {@code Validation} to an {@link Either}: {@code Right(value)} for a {@code Valid}, {@code Left} of
     * all the errors for an {@code Invalid}.
     *
     * @return the {@code Either} equivalent of this {@code Validation}
     */
    default Either<NonEmptyVector<E>, A> toEither() {
        return switch (this) {
            case Valid(var value) -> Either.right(value);
            case Invalid(var errors) -> Either.left(errors);
        };
    }

    /**
     * Converts this {@code Validation} to an {@link Either} with the errors merged by {@code f}: {@code Right(value)}
     * for a {@code Valid}, {@code Left(f.apply(errors))} for an {@code Invalid}.
     *
     * @param f    merges the errors into one left value; it must not return {@code null}
     * @param <E2> the left type
     * @return the {@code Either} equivalent of this {@code Validation}
     * @throws NullPointerException if {@code f} is null, or if it returns null for an {@code Invalid}
     */
    default <E2 extends @Nullable Object> Either<E2, A> toEitherWith(Function<? super NonEmptyVector<E>, ? extends E2> f) {
        Objects.requireNonNull(f, "f is null");
        return switch (this) {
            case Valid(var value) -> Either.right(value);
            case Invalid(var errors) -> Either.left(f.apply(errors));
        };
    }

    /**
     * Converts this {@code Validation} to an {@link Option}: {@code Some(value)} for a {@code Valid}, {@code None} for
     * an {@code Invalid}; the errors are dropped.
     *
     * @return the {@code Option} of the value
     */
    default Option<A> toOption() {
        return switch (this) {
            case Valid(var value) -> Option.some(value);
            case Invalid<E, A> _ -> Option.none();
        };
    }

    /**
     * Converts this {@code Validation} to a {@link Try}: {@code Success(value)} for a {@code Valid}, {@code Failure}
     * of the throwable built from the errors for an {@code Invalid}.
     *
     * @param f builds the failure cause from the errors; it must not return {@code null} nor a fatal throwable (see
     *          {@link Try})
     * @return the {@code Try} equivalent of this {@code Validation}
     * @throws NullPointerException if {@code f} is null, or if it returns null for an {@code Invalid}
     */
    default Try<A> toTry(Function<? super NonEmptyVector<E>, ? extends Throwable> f) {
        Objects.requireNonNull(f, "f is null");
        return switch (this) {
            case Valid(var value) -> Try.success(value);
            case Invalid(var errors) -> Try.failure(f.apply(errors));
        };
    }

    /**
     * Converts this {@code Validation} to a {@link Vector} of zero or one element: its value, if any.
     *
     * @return {@code Vector.of(value)} for a {@code Valid}, otherwise the empty {@code Vector}
     */
    default Vector<A> toVector() {
        return switch (this) {
            case Valid(var value) -> Vector.of(value);
            case Invalid<E, A> _ -> Vector.empty();
        };
    }

    // -- cases

    /**
     * The {@code Valid} case of a {@code Validation}. The value is never {@code null}.
     *
     * @param value the value, never {@code null}
     * @param <E>   the error type
     * @param <A>   the value type
     */
    record Valid<E extends @Nullable Object, A extends @Nullable Object>(A value) implements Validation<E, A> {

        /**
         * Rejects {@code null}.
         *
         * @throws NullPointerException if {@code value} is null
         */
        public Valid {
            Objects.requireNonNull(value, "value is null");
        }

        @Override
        public A get() {
            return value;
        }

        @Override
        public boolean isValid() {
            return true;
        }

        @Override
        public boolean isInvalid() {
            return false;
        }

        @Override
        public String toString() {
            return "Valid(" + value + ")";
        }
    }

    /**
     * The {@code Invalid} case of a {@code Validation}: at least one error, in accumulation order. Equality is the
     * record equality, so it is order sensitive.
     *
     * @param errors the errors, never {@code null}
     * @param <E>    the error type
     * @param <A>    the value type
     */
    record Invalid<E extends @Nullable Object, A extends @Nullable Object>(NonEmptyVector<E> errors) implements Validation<E, A> {

        /**
         * Rejects {@code null}.
         *
         * @throws NullPointerException if {@code errors} is null
         */
        public Invalid {
            Objects.requireNonNull(errors, "errors is null");
        }

        @Override
        public A get() {
            throw new NoSuchElementException("get() on Invalid");
        }

        @Override
        public boolean isValid() {
            return false;
        }

        @Override
        public boolean isInvalid() {
            return true;
        }

        @Override
        public String toString() {
            return errors.mkString("Invalid(", ", ", ")");
        }
    }
}
