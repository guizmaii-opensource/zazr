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
     * Removes one level of nesting: {@code Valid(Valid(a))} is {@code Valid(a)}, {@code Valid(Invalid(errors))} is
     * {@code Invalid(errors)}, and an outer {@code Invalid} is returned as it is. Nothing accumulates: the outer
     * validation is either invalid with its own errors or valid with the inner one as its value, so there is never a
     * second set of errors to add. Static, like every {@code flatten} in Zazr, because Java cannot demand of an
     * instance method that the value be a {@code Validation} itself.
     *
     * @param nested a {@code Validation} whose value is a {@code Validation} with the same error type
     * @param <E>    the error type
     * @param <A>    the type of the inner value
     * @return the inner {@code Validation}, or the outer {@code Invalid}
     * @throws NullPointerException if {@code nested} is null
     */
    @SuppressWarnings("unchecked")
    static <E extends @Nullable Object, A extends @Nullable Object> Validation<E, A> flatten(Validation<? extends E, ? extends Validation<? extends E, ? extends A>> nested) {
        Objects.requireNonNull(nested, "nested is null");
        return switch (nested) {
            case Valid(var inner) -> (Validation<E, A>) inner;
            case Invalid<?, ?> invalid -> (Validation<E, A>) invalid;
        };
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
        return option.isDefined() ? valid(option.get()) : invalid(Objects.requireNonNull(ifNone.get(), "Validation.fromOption: ifNone returned null"));
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
        return predicate.test(value) ? valid(value) : invalid(Objects.requireNonNull(ifFalse.apply(value), "Validation.fromPredicate: ifFalse returned null"));
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
            case Failure(var cause) -> invalid(Objects.requireNonNull(onError.apply(cause), "Validation.of: onError returned null"));
        };
    }

    // -- accumulate

    /**
     * Accumulates {@code validations}: {@code Valid} of a {@link Vector} of all the values, in order, when every one is
     * {@code Valid}; otherwise {@code Invalid} of the errors of <em>every</em> {@code Invalid}, in order. The empty
     * iterable gives {@code Valid} of the empty {@code Vector}.
     * <pre>{@code
     * Validation.collectAll(List.of(valid(1), invalid("a"), invalid("b"))); // = Invalid("a", "b")
     * }</pre>
     *
     * @param validations the validations to accumulate
     * @param <E>         the error type
     * @param <A>         the value type
     * @return {@code Valid} of all the values, or {@code Invalid} of all the errors
     * @throws NullPointerException if {@code validations} or one of its elements is null
     */
    static <E extends @Nullable Object, A extends @Nullable Object> Validation<E, Vector<A>> collectAll(Iterable<? extends Validation<? extends E, ? extends A>> validations) {
        Objects.requireNonNull(validations, "validations is null");
        return forEach(validations, v -> Objects.requireNonNull(v, "Validation.collectAll: element is null"));
    }

    /**
     * Applies {@code f} to every element and accumulates the results as {@link #collectAll(Iterable)} does: {@code Valid}
     * of a {@link Vector} of the mapped values when every call returns a {@code Valid}, otherwise {@code Invalid} of
     * the errors of every {@code Invalid}, in order. {@code f} is called for every element, whatever the earlier
     * results were.
     * <pre>{@code
     * Validation.forEach(List.of("1", "x", "y"), s -> parse(s)); // = Invalid("x is not a number", "y is not a number")
     * }</pre>
     *
     * @param values the elements to validate
     * @param f      a function from an element to a {@code Validation}; it must not return {@code null}
     * @param <E>    the error type
     * @param <A>    the element type
     * @param <B>    the value type of the results
     * @return {@code Valid} of all the mapped values, or {@code Invalid} of all the errors
     * @throws NullPointerException if {@code values} or {@code f} is null, or if {@code f} returns null
     */
    static <E extends @Nullable Object, A extends @Nullable Object, B extends @Nullable Object> Validation<E, Vector<B>> forEach(Iterable<? extends A> values, Function<? super A, ? extends Validation<? extends E, ? extends B>> f) {
        Objects.requireNonNull(values, "values is null");
        Objects.requireNonNull(f, "f is null");
        final Vector.Builder<B> results = Vector.newBuilder();
        Vector.Builder<E> errors = null;
        for (A value : values) {
            final Validation<? extends E, ? extends B> validation = Objects.requireNonNull(f.apply(value), "Validation.forEach: f returned null");
            if (validation instanceof Invalid(var es)) {
                if (errors == null) {
                    errors = Vector.newBuilder();
                }
                errors.addAll(es.toVector());
            } else if (errors == null) {
                results.add(validation.get());
            }
        }
        return errors == null ? valid(results.result()) : new Invalid<>(NonEmptyVector.unsafeFromVector(errors.result()));
    }

    /**
     * {@link #forEach(Iterable, Function)} on a {@link NonEmptyVector}: as many results as inputs, so the {@code Valid}
     * side is a {@code NonEmptyVector} too.
     *
     * @param values the elements to validate
     * @param f      a function from an element to a {@code Validation}; it must not return {@code null}
     * @param <E>    the error type
     * @param <A>    the element type
     * @param <B>    the value type of the results
     * @return {@code Valid} of all the mapped values, or {@code Invalid} of all the errors
     * @throws NullPointerException if {@code values} or {@code f} is null, or if {@code f} returns null
     */
    static <E extends @Nullable Object, A extends @Nullable Object, B extends @Nullable Object> Validation<E, NonEmptyVector<B>> forEach(NonEmptyVector<? extends A> values, Function<? super A, ? extends Validation<? extends E, ? extends B>> f) {
        Objects.requireNonNull(values, "values is null");
        return forEach(values.toVector(), f).map(NonEmptyVector::unsafeFromVector);
    }

    /**
     * Applies {@code f} to every element and splits the results: the errors of every {@code Invalid} on the left, in
     * order, the values of every {@code Valid} on the right, in order. Cannot fail: an all-valid input has an empty
     * left side, an all-invalid input an empty right side.
     * <pre>{@code
     * Validation.partition(List.of("1", "x", "2"), s -> parse(s)); // = (Vector("x is not a number"), Vector(1, 2))
     * }</pre>
     *
     * @param values the elements to validate
     * @param f      a function from an element to a {@code Validation}; it must not return {@code null}
     * @param <E>    the error type
     * @param <A>    the element type
     * @param <B>    the value type of the results
     * @return the errors and the values
     * @throws NullPointerException if {@code values} or {@code f} is null, or if {@code f} returns null
     */
    static <E extends @Nullable Object, A extends @Nullable Object, B extends @Nullable Object> Tuple2<Vector<E>, Vector<B>> partition(Iterable<? extends A> values, Function<? super A, ? extends Validation<? extends E, ? extends B>> f) {
        Objects.requireNonNull(values, "values is null");
        Objects.requireNonNull(f, "f is null");
        final Vector.Builder<E> errors = Vector.newBuilder();
        final Vector.Builder<B> results = Vector.newBuilder();
        for (A value : values) {
            switch (Objects.requireNonNull(f.apply(value), "Validation.partition: f returned null")) {
                case Valid(var v) -> results.add(v);
                case Invalid(var es) -> errors.addAll(es.toVector());
            }
        }
        return Tuple.of(errors.result(), results.result());
    }

    /**
     * Pairs this value with {@code that}'s, keeping <em>all</em> errors: {@code Valid((a, b))} when both are
     * {@code Valid}, otherwise {@code Invalid} of this one's errors followed by {@code that}'s.
     * <pre>{@code
     * Validation.invalid("a").zip(Validation.invalid("b")); // = Invalid("a", "b")
     * }</pre>
     *
     * @param that the other validation
     * @param <B>  the value type of {@code that}
     * @return the pair of values, or the accumulated errors
     * @throws NullPointerException if {@code that} is null
     */
    default <B extends @Nullable Object> Validation<E, Tuple2<A, B>> zip(Validation<? extends E, ? extends B> that) {
        return zipWith(that, Tuple::of);
    }

    /**
     * Combines this value with {@code that}'s through {@code f}, keeping <em>all</em> errors: {@code Valid(f(a, b))}
     * when both are {@code Valid}, otherwise {@code Invalid} of this one's errors followed by {@code that}'s. {@code f}
     * is called only when both are {@code Valid}.
     *
     * @param that the other validation
     * @param f    combines the two values; it must not return {@code null}
     * @param <B>  the value type of {@code that}
     * @param <C>  the result type
     * @return the combined value, or the accumulated errors
     * @throws NullPointerException if {@code that} or {@code f} is null, or if {@code f} returns null
     */
    @SuppressWarnings("unchecked")
    default <B extends @Nullable Object, C extends @Nullable Object> Validation<E, C> zipWith(Validation<? extends E, ? extends B> that, BiFunction<? super A, ? super B, ? extends C> f) {
        Objects.requireNonNull(that, "that is null");
        Objects.requireNonNull(f, "f is null");
        return switch (this) {
            case Valid(var a) -> switch (that) {
                case Valid(var b) -> valid(Objects.requireNonNull(f.apply(a, b), "Validation.zipWith: f returned null"));
                case Invalid<? extends E, ? extends B> invalid -> (Validation<E, C>) invalid;
            };
            case Invalid(var errors) -> switch (that) {
                case Valid<?, ?> _ -> (Validation<E, C>) this;
                case Invalid(var more) -> new Invalid<>(errors.appendAll(more));
            };
        };
    }

    /**
     * {@link #zip(Validation)} keeping this value: {@code Valid(a)} when both are {@code Valid}, otherwise the
     * accumulated errors.
     *
     * @param that the other validation
     * @param <B>  the value type of {@code that}
     * @return this value, or the accumulated errors
     * @throws NullPointerException if {@code that} is null
     */
    default <B extends @Nullable Object> Validation<E, A> zipLeft(Validation<? extends E, ? extends B> that) {
        return zipWith(that, (a, _) -> a);
    }

    /**
     * {@link #zip(Validation)} keeping {@code that}'s value: {@code Valid(b)} when both are {@code Valid}, otherwise
     * the accumulated errors.
     *
     * @param that the other validation
     * @param <B>  the value type of {@code that}
     * @return {@code that}'s value, or the accumulated errors
     * @throws NullPointerException if {@code that} is null
     */
    default <B extends @Nullable Object> Validation<E, B> zipRight(Validation<? extends E, ? extends B> that) {
        return zipWith(that, (_, b) -> b);
    }

    /**
     * {@link #zip(Validation)} with an {@link Either} operand, seen as a validation with one error: {@code Left(e)}
     * contributes {@code e} to the accumulated errors.
     *
     * @param that the other side
     * @param <B>  the right type of {@code that}
     * @return the pair of values, or the accumulated errors
     * @throws NullPointerException if {@code that} is null
     */
    default <B extends @Nullable Object> Validation<E, Tuple2<A, B>> zip(Either<? extends E, ? extends B> that) {
        Objects.requireNonNull(that, "that is null");
        return zip(fromEither(that));
    }

    /**
     * Pairs the values of two validations, keeping <em>all</em> errors: {@code Valid} of the tuple of the values when
     * every argument is {@code Valid}, otherwise {@code Invalid} of the errors of every {@code Invalid} argument,
     * concatenated in argument order. The same as {@link #zipWith(Validation, Validation, BiFunction)} with
     * {@code Tuple::of}.
     *
     * @param v1  the first validation
     * @param v2  the second validation
     * @param <E>  the error type, shared by every argument
     * @param <T1> the value type of {@code v1}
     * @param <T2> the value type of {@code v2}
     * @return {@code Valid} of the tuple of the values, or the accumulated errors
     * @throws NullPointerException if any argument is null
     */
    static <E extends @Nullable Object, T1 extends @Nullable Object, T2 extends @Nullable Object> Validation<E, Tuple2<T1, T2>> zip(Validation<? extends E, ? extends T1> v1, Validation<? extends E, ? extends T2> v2) {
        return zipWith(v1, v2, Tuple::of);
    }

    /**
     * Combines the values of two validations through {@code f}, keeping <em>all</em> errors: {@code Valid} of the
     * result when every argument is {@code Valid}, otherwise {@code Invalid} of the errors of every {@code Invalid}
     * argument, concatenated in argument order. {@code f} is called only when every argument is {@code Valid}, with
     * the values in argument order; it must not return {@code null}.
     *
     * @param v1  the first validation
     * @param v2  the second validation
     * @param f  combines the values; it must not return {@code null}
     * @param <E>  the error type, shared by every argument
     * @param <T1> the value type of {@code v1}
     * @param <T2> the value type of {@code v2}
     * @param <R>  the result type
     * @return {@code Valid} of the combined value, or the accumulated errors
     * @throws NullPointerException if any argument is null, or if {@code f} returns null
     */
    static <E extends @Nullable Object, T1 extends @Nullable Object, T2 extends @Nullable Object, R extends @Nullable Object> Validation<E, R> zipWith(Validation<? extends E, ? extends T1> v1, Validation<? extends E, ? extends T2> v2, BiFunction<? super T1, ? super T2, ? extends R> f) {
        Objects.requireNonNull(v1, "v1 is null");
        Objects.requireNonNull(v2, "v2 is null");
        Objects.requireNonNull(f, "f is null");
        Vector.Builder<E> errors = null;
        errors = accumulate(errors, v1);
        errors = accumulate(errors, v2);
        if (errors == null) {
            return valid(Objects.requireNonNull(f.apply(v1.get(), v2.get()), "Validation.zipWith: f returned null"));
        }
        return new Invalid<>(NonEmptyVector.unsafeFromVector(errors.result()));
    }

    /**
     * Pairs the values of three validations, keeping <em>all</em> errors: {@code Valid} of the tuple of the values when
     * every argument is {@code Valid}, otherwise {@code Invalid} of the errors of every {@code Invalid} argument,
     * concatenated in argument order. The same as {@link #zipWith(Validation, Validation, Validation, Function3)} with
     * {@code Tuple::of}.
     *
     * @param v1  the first validation
     * @param v2  the second validation
     * @param v3  the third validation
     * @param <E>  the error type, shared by every argument
     * @param <T1> the value type of {@code v1}
     * @param <T2> the value type of {@code v2}
     * @param <T3> the value type of {@code v3}
     * @return {@code Valid} of the tuple of the values, or the accumulated errors
     * @throws NullPointerException if any argument is null
     */
    static <E extends @Nullable Object, T1 extends @Nullable Object, T2 extends @Nullable Object, T3 extends @Nullable Object> Validation<E, Tuple3<T1, T2, T3>> zip(Validation<? extends E, ? extends T1> v1, Validation<? extends E, ? extends T2> v2, Validation<? extends E, ? extends T3> v3) {
        return zipWith(v1, v2, v3, Tuple::of);
    }

    /**
     * Combines the values of three validations through {@code f}, keeping <em>all</em> errors: {@code Valid} of the
     * result when every argument is {@code Valid}, otherwise {@code Invalid} of the errors of every {@code Invalid}
     * argument, concatenated in argument order. {@code f} is called only when every argument is {@code Valid}, with
     * the values in argument order; it must not return {@code null}.
     *
     * @param v1  the first validation
     * @param v2  the second validation
     * @param v3  the third validation
     * @param f  combines the values; it must not return {@code null}
     * @param <E>  the error type, shared by every argument
     * @param <T1> the value type of {@code v1}
     * @param <T2> the value type of {@code v2}
     * @param <T3> the value type of {@code v3}
     * @param <R>  the result type
     * @return {@code Valid} of the combined value, or the accumulated errors
     * @throws NullPointerException if any argument is null, or if {@code f} returns null
     */
    static <E extends @Nullable Object, T1 extends @Nullable Object, T2 extends @Nullable Object, T3 extends @Nullable Object, R extends @Nullable Object> Validation<E, R> zipWith(Validation<? extends E, ? extends T1> v1, Validation<? extends E, ? extends T2> v2, Validation<? extends E, ? extends T3> v3, Function3<? super T1, ? super T2, ? super T3, ? extends R> f) {
        Objects.requireNonNull(v1, "v1 is null");
        Objects.requireNonNull(v2, "v2 is null");
        Objects.requireNonNull(v3, "v3 is null");
        Objects.requireNonNull(f, "f is null");
        Vector.Builder<E> errors = null;
        errors = accumulate(errors, v1);
        errors = accumulate(errors, v2);
        errors = accumulate(errors, v3);
        if (errors == null) {
            return valid(Objects.requireNonNull(f.apply(v1.get(), v2.get(), v3.get()), "Validation.zipWith: f returned null"));
        }
        return new Invalid<>(NonEmptyVector.unsafeFromVector(errors.result()));
    }

    /**
     * Pairs the values of four validations, keeping <em>all</em> errors: {@code Valid} of the tuple of the values when
     * every argument is {@code Valid}, otherwise {@code Invalid} of the errors of every {@code Invalid} argument,
     * concatenated in argument order. The same as {@link #zipWith(Validation, Validation, Validation, Validation, Function4)} with
     * {@code Tuple::of}.
     *
     * @param v1  the first validation
     * @param v2  the second validation
     * @param v3  the third validation
     * @param v4  the fourth validation
     * @param <E>  the error type, shared by every argument
     * @param <T1> the value type of {@code v1}
     * @param <T2> the value type of {@code v2}
     * @param <T3> the value type of {@code v3}
     * @param <T4> the value type of {@code v4}
     * @return {@code Valid} of the tuple of the values, or the accumulated errors
     * @throws NullPointerException if any argument is null
     */
    static <E extends @Nullable Object, T1 extends @Nullable Object, T2 extends @Nullable Object, T3 extends @Nullable Object, T4 extends @Nullable Object> Validation<E, Tuple4<T1, T2, T3, T4>> zip(Validation<? extends E, ? extends T1> v1, Validation<? extends E, ? extends T2> v2, Validation<? extends E, ? extends T3> v3, Validation<? extends E, ? extends T4> v4) {
        return zipWith(v1, v2, v3, v4, Tuple::of);
    }

    /**
     * Combines the values of four validations through {@code f}, keeping <em>all</em> errors: {@code Valid} of the
     * result when every argument is {@code Valid}, otherwise {@code Invalid} of the errors of every {@code Invalid}
     * argument, concatenated in argument order. {@code f} is called only when every argument is {@code Valid}, with
     * the values in argument order; it must not return {@code null}.
     *
     * @param v1  the first validation
     * @param v2  the second validation
     * @param v3  the third validation
     * @param v4  the fourth validation
     * @param f  combines the values; it must not return {@code null}
     * @param <E>  the error type, shared by every argument
     * @param <T1> the value type of {@code v1}
     * @param <T2> the value type of {@code v2}
     * @param <T3> the value type of {@code v3}
     * @param <T4> the value type of {@code v4}
     * @param <R>  the result type
     * @return {@code Valid} of the combined value, or the accumulated errors
     * @throws NullPointerException if any argument is null, or if {@code f} returns null
     */
    static <E extends @Nullable Object, T1 extends @Nullable Object, T2 extends @Nullable Object, T3 extends @Nullable Object, T4 extends @Nullable Object, R extends @Nullable Object> Validation<E, R> zipWith(Validation<? extends E, ? extends T1> v1, Validation<? extends E, ? extends T2> v2, Validation<? extends E, ? extends T3> v3, Validation<? extends E, ? extends T4> v4, Function4<? super T1, ? super T2, ? super T3, ? super T4, ? extends R> f) {
        Objects.requireNonNull(v1, "v1 is null");
        Objects.requireNonNull(v2, "v2 is null");
        Objects.requireNonNull(v3, "v3 is null");
        Objects.requireNonNull(v4, "v4 is null");
        Objects.requireNonNull(f, "f is null");
        Vector.Builder<E> errors = null;
        errors = accumulate(errors, v1);
        errors = accumulate(errors, v2);
        errors = accumulate(errors, v3);
        errors = accumulate(errors, v4);
        if (errors == null) {
            return valid(Objects.requireNonNull(f.apply(v1.get(), v2.get(), v3.get(), v4.get()), "Validation.zipWith: f returned null"));
        }
        return new Invalid<>(NonEmptyVector.unsafeFromVector(errors.result()));
    }

    /**
     * Pairs the values of five validations, keeping <em>all</em> errors: {@code Valid} of the tuple of the values when
     * every argument is {@code Valid}, otherwise {@code Invalid} of the errors of every {@code Invalid} argument,
     * concatenated in argument order. The same as {@link #zipWith(Validation, Validation, Validation, Validation, Validation, Function5)} with
     * {@code Tuple::of}.
     *
     * @param v1  the first validation
     * @param v2  the second validation
     * @param v3  the third validation
     * @param v4  the fourth validation
     * @param v5  the fifth validation
     * @param <E>  the error type, shared by every argument
     * @param <T1> the value type of {@code v1}
     * @param <T2> the value type of {@code v2}
     * @param <T3> the value type of {@code v3}
     * @param <T4> the value type of {@code v4}
     * @param <T5> the value type of {@code v5}
     * @return {@code Valid} of the tuple of the values, or the accumulated errors
     * @throws NullPointerException if any argument is null
     */
    static <E extends @Nullable Object, T1 extends @Nullable Object, T2 extends @Nullable Object, T3 extends @Nullable Object, T4 extends @Nullable Object, T5 extends @Nullable Object> Validation<E, Tuple5<T1, T2, T3, T4, T5>> zip(Validation<? extends E, ? extends T1> v1, Validation<? extends E, ? extends T2> v2, Validation<? extends E, ? extends T3> v3, Validation<? extends E, ? extends T4> v4, Validation<? extends E, ? extends T5> v5) {
        return zipWith(v1, v2, v3, v4, v5, Tuple::of);
    }

    /**
     * Combines the values of five validations through {@code f}, keeping <em>all</em> errors: {@code Valid} of the
     * result when every argument is {@code Valid}, otherwise {@code Invalid} of the errors of every {@code Invalid}
     * argument, concatenated in argument order. {@code f} is called only when every argument is {@code Valid}, with
     * the values in argument order; it must not return {@code null}.
     *
     * @param v1  the first validation
     * @param v2  the second validation
     * @param v3  the third validation
     * @param v4  the fourth validation
     * @param v5  the fifth validation
     * @param f  combines the values; it must not return {@code null}
     * @param <E>  the error type, shared by every argument
     * @param <T1> the value type of {@code v1}
     * @param <T2> the value type of {@code v2}
     * @param <T3> the value type of {@code v3}
     * @param <T4> the value type of {@code v4}
     * @param <T5> the value type of {@code v5}
     * @param <R>  the result type
     * @return {@code Valid} of the combined value, or the accumulated errors
     * @throws NullPointerException if any argument is null, or if {@code f} returns null
     */
    static <E extends @Nullable Object, T1 extends @Nullable Object, T2 extends @Nullable Object, T3 extends @Nullable Object, T4 extends @Nullable Object, T5 extends @Nullable Object, R extends @Nullable Object> Validation<E, R> zipWith(Validation<? extends E, ? extends T1> v1, Validation<? extends E, ? extends T2> v2, Validation<? extends E, ? extends T3> v3, Validation<? extends E, ? extends T4> v4, Validation<? extends E, ? extends T5> v5, Function5<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? extends R> f) {
        Objects.requireNonNull(v1, "v1 is null");
        Objects.requireNonNull(v2, "v2 is null");
        Objects.requireNonNull(v3, "v3 is null");
        Objects.requireNonNull(v4, "v4 is null");
        Objects.requireNonNull(v5, "v5 is null");
        Objects.requireNonNull(f, "f is null");
        Vector.Builder<E> errors = null;
        errors = accumulate(errors, v1);
        errors = accumulate(errors, v2);
        errors = accumulate(errors, v3);
        errors = accumulate(errors, v4);
        errors = accumulate(errors, v5);
        if (errors == null) {
            return valid(Objects.requireNonNull(f.apply(v1.get(), v2.get(), v3.get(), v4.get(), v5.get()), "Validation.zipWith: f returned null"));
        }
        return new Invalid<>(NonEmptyVector.unsafeFromVector(errors.result()));
    }

    /**
     * Pairs the values of six validations, keeping <em>all</em> errors: {@code Valid} of the tuple of the values when
     * every argument is {@code Valid}, otherwise {@code Invalid} of the errors of every {@code Invalid} argument,
     * concatenated in argument order. The same as {@link #zipWith(Validation, Validation, Validation, Validation, Validation, Validation, Function6)} with
     * {@code Tuple::of}.
     *
     * @param v1  the first validation
     * @param v2  the second validation
     * @param v3  the third validation
     * @param v4  the fourth validation
     * @param v5  the fifth validation
     * @param v6  the sixth validation
     * @param <E>  the error type, shared by every argument
     * @param <T1> the value type of {@code v1}
     * @param <T2> the value type of {@code v2}
     * @param <T3> the value type of {@code v3}
     * @param <T4> the value type of {@code v4}
     * @param <T5> the value type of {@code v5}
     * @param <T6> the value type of {@code v6}
     * @return {@code Valid} of the tuple of the values, or the accumulated errors
     * @throws NullPointerException if any argument is null
     */
    static <E extends @Nullable Object, T1 extends @Nullable Object, T2 extends @Nullable Object, T3 extends @Nullable Object, T4 extends @Nullable Object, T5 extends @Nullable Object, T6 extends @Nullable Object> Validation<E, Tuple6<T1, T2, T3, T4, T5, T6>> zip(Validation<? extends E, ? extends T1> v1, Validation<? extends E, ? extends T2> v2, Validation<? extends E, ? extends T3> v3, Validation<? extends E, ? extends T4> v4, Validation<? extends E, ? extends T5> v5, Validation<? extends E, ? extends T6> v6) {
        return zipWith(v1, v2, v3, v4, v5, v6, Tuple::of);
    }

    /**
     * Combines the values of six validations through {@code f}, keeping <em>all</em> errors: {@code Valid} of the
     * result when every argument is {@code Valid}, otherwise {@code Invalid} of the errors of every {@code Invalid}
     * argument, concatenated in argument order. {@code f} is called only when every argument is {@code Valid}, with
     * the values in argument order; it must not return {@code null}.
     *
     * @param v1  the first validation
     * @param v2  the second validation
     * @param v3  the third validation
     * @param v4  the fourth validation
     * @param v5  the fifth validation
     * @param v6  the sixth validation
     * @param f  combines the values; it must not return {@code null}
     * @param <E>  the error type, shared by every argument
     * @param <T1> the value type of {@code v1}
     * @param <T2> the value type of {@code v2}
     * @param <T3> the value type of {@code v3}
     * @param <T4> the value type of {@code v4}
     * @param <T5> the value type of {@code v5}
     * @param <T6> the value type of {@code v6}
     * @param <R>  the result type
     * @return {@code Valid} of the combined value, or the accumulated errors
     * @throws NullPointerException if any argument is null, or if {@code f} returns null
     */
    static <E extends @Nullable Object, T1 extends @Nullable Object, T2 extends @Nullable Object, T3 extends @Nullable Object, T4 extends @Nullable Object, T5 extends @Nullable Object, T6 extends @Nullable Object, R extends @Nullable Object> Validation<E, R> zipWith(Validation<? extends E, ? extends T1> v1, Validation<? extends E, ? extends T2> v2, Validation<? extends E, ? extends T3> v3, Validation<? extends E, ? extends T4> v4, Validation<? extends E, ? extends T5> v5, Validation<? extends E, ? extends T6> v6, Function6<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? super T6, ? extends R> f) {
        Objects.requireNonNull(v1, "v1 is null");
        Objects.requireNonNull(v2, "v2 is null");
        Objects.requireNonNull(v3, "v3 is null");
        Objects.requireNonNull(v4, "v4 is null");
        Objects.requireNonNull(v5, "v5 is null");
        Objects.requireNonNull(v6, "v6 is null");
        Objects.requireNonNull(f, "f is null");
        Vector.Builder<E> errors = null;
        errors = accumulate(errors, v1);
        errors = accumulate(errors, v2);
        errors = accumulate(errors, v3);
        errors = accumulate(errors, v4);
        errors = accumulate(errors, v5);
        errors = accumulate(errors, v6);
        if (errors == null) {
            return valid(Objects.requireNonNull(f.apply(v1.get(), v2.get(), v3.get(), v4.get(), v5.get(), v6.get()), "Validation.zipWith: f returned null"));
        }
        return new Invalid<>(NonEmptyVector.unsafeFromVector(errors.result()));
    }

    /**
     * Pairs the values of seven validations, keeping <em>all</em> errors: {@code Valid} of the tuple of the values when
     * every argument is {@code Valid}, otherwise {@code Invalid} of the errors of every {@code Invalid} argument,
     * concatenated in argument order. The same as {@link #zipWith(Validation, Validation, Validation, Validation, Validation, Validation, Validation, Function7)} with
     * {@code Tuple::of}.
     *
     * @param v1  the first validation
     * @param v2  the second validation
     * @param v3  the third validation
     * @param v4  the fourth validation
     * @param v5  the fifth validation
     * @param v6  the sixth validation
     * @param v7  the seventh validation
     * @param <E>  the error type, shared by every argument
     * @param <T1> the value type of {@code v1}
     * @param <T2> the value type of {@code v2}
     * @param <T3> the value type of {@code v3}
     * @param <T4> the value type of {@code v4}
     * @param <T5> the value type of {@code v5}
     * @param <T6> the value type of {@code v6}
     * @param <T7> the value type of {@code v7}
     * @return {@code Valid} of the tuple of the values, or the accumulated errors
     * @throws NullPointerException if any argument is null
     */
    static <E extends @Nullable Object, T1 extends @Nullable Object, T2 extends @Nullable Object, T3 extends @Nullable Object, T4 extends @Nullable Object, T5 extends @Nullable Object, T6 extends @Nullable Object, T7 extends @Nullable Object> Validation<E, Tuple7<T1, T2, T3, T4, T5, T6, T7>> zip(Validation<? extends E, ? extends T1> v1, Validation<? extends E, ? extends T2> v2, Validation<? extends E, ? extends T3> v3, Validation<? extends E, ? extends T4> v4, Validation<? extends E, ? extends T5> v5, Validation<? extends E, ? extends T6> v6, Validation<? extends E, ? extends T7> v7) {
        return zipWith(v1, v2, v3, v4, v5, v6, v7, Tuple::of);
    }

    /**
     * Combines the values of seven validations through {@code f}, keeping <em>all</em> errors: {@code Valid} of the
     * result when every argument is {@code Valid}, otherwise {@code Invalid} of the errors of every {@code Invalid}
     * argument, concatenated in argument order. {@code f} is called only when every argument is {@code Valid}, with
     * the values in argument order; it must not return {@code null}.
     *
     * @param v1  the first validation
     * @param v2  the second validation
     * @param v3  the third validation
     * @param v4  the fourth validation
     * @param v5  the fifth validation
     * @param v6  the sixth validation
     * @param v7  the seventh validation
     * @param f  combines the values; it must not return {@code null}
     * @param <E>  the error type, shared by every argument
     * @param <T1> the value type of {@code v1}
     * @param <T2> the value type of {@code v2}
     * @param <T3> the value type of {@code v3}
     * @param <T4> the value type of {@code v4}
     * @param <T5> the value type of {@code v5}
     * @param <T6> the value type of {@code v6}
     * @param <T7> the value type of {@code v7}
     * @param <R>  the result type
     * @return {@code Valid} of the combined value, or the accumulated errors
     * @throws NullPointerException if any argument is null, or if {@code f} returns null
     */
    static <E extends @Nullable Object, T1 extends @Nullable Object, T2 extends @Nullable Object, T3 extends @Nullable Object, T4 extends @Nullable Object, T5 extends @Nullable Object, T6 extends @Nullable Object, T7 extends @Nullable Object, R extends @Nullable Object> Validation<E, R> zipWith(Validation<? extends E, ? extends T1> v1, Validation<? extends E, ? extends T2> v2, Validation<? extends E, ? extends T3> v3, Validation<? extends E, ? extends T4> v4, Validation<? extends E, ? extends T5> v5, Validation<? extends E, ? extends T6> v6, Validation<? extends E, ? extends T7> v7, Function7<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? super T6, ? super T7, ? extends R> f) {
        Objects.requireNonNull(v1, "v1 is null");
        Objects.requireNonNull(v2, "v2 is null");
        Objects.requireNonNull(v3, "v3 is null");
        Objects.requireNonNull(v4, "v4 is null");
        Objects.requireNonNull(v5, "v5 is null");
        Objects.requireNonNull(v6, "v6 is null");
        Objects.requireNonNull(v7, "v7 is null");
        Objects.requireNonNull(f, "f is null");
        Vector.Builder<E> errors = null;
        errors = accumulate(errors, v1);
        errors = accumulate(errors, v2);
        errors = accumulate(errors, v3);
        errors = accumulate(errors, v4);
        errors = accumulate(errors, v5);
        errors = accumulate(errors, v6);
        errors = accumulate(errors, v7);
        if (errors == null) {
            return valid(Objects.requireNonNull(f.apply(v1.get(), v2.get(), v3.get(), v4.get(), v5.get(), v6.get(), v7.get()), "Validation.zipWith: f returned null"));
        }
        return new Invalid<>(NonEmptyVector.unsafeFromVector(errors.result()));
    }

    /**
     * Pairs the values of eight validations, keeping <em>all</em> errors: {@code Valid} of the tuple of the values when
     * every argument is {@code Valid}, otherwise {@code Invalid} of the errors of every {@code Invalid} argument,
     * concatenated in argument order. The same as {@link #zipWith(Validation, Validation, Validation, Validation, Validation, Validation, Validation, Validation, Function8)} with
     * {@code Tuple::of}.
     *
     * @param v1  the first validation
     * @param v2  the second validation
     * @param v3  the third validation
     * @param v4  the fourth validation
     * @param v5  the fifth validation
     * @param v6  the sixth validation
     * @param v7  the seventh validation
     * @param v8  the eighth validation
     * @param <E>  the error type, shared by every argument
     * @param <T1> the value type of {@code v1}
     * @param <T2> the value type of {@code v2}
     * @param <T3> the value type of {@code v3}
     * @param <T4> the value type of {@code v4}
     * @param <T5> the value type of {@code v5}
     * @param <T6> the value type of {@code v6}
     * @param <T7> the value type of {@code v7}
     * @param <T8> the value type of {@code v8}
     * @return {@code Valid} of the tuple of the values, or the accumulated errors
     * @throws NullPointerException if any argument is null
     */
    static <E extends @Nullable Object, T1 extends @Nullable Object, T2 extends @Nullable Object, T3 extends @Nullable Object, T4 extends @Nullable Object, T5 extends @Nullable Object, T6 extends @Nullable Object, T7 extends @Nullable Object, T8 extends @Nullable Object> Validation<E, Tuple8<T1, T2, T3, T4, T5, T6, T7, T8>> zip(Validation<? extends E, ? extends T1> v1, Validation<? extends E, ? extends T2> v2, Validation<? extends E, ? extends T3> v3, Validation<? extends E, ? extends T4> v4, Validation<? extends E, ? extends T5> v5, Validation<? extends E, ? extends T6> v6, Validation<? extends E, ? extends T7> v7, Validation<? extends E, ? extends T8> v8) {
        return zipWith(v1, v2, v3, v4, v5, v6, v7, v8, Tuple::of);
    }

    /**
     * Combines the values of eight validations through {@code f}, keeping <em>all</em> errors: {@code Valid} of the
     * result when every argument is {@code Valid}, otherwise {@code Invalid} of the errors of every {@code Invalid}
     * argument, concatenated in argument order. {@code f} is called only when every argument is {@code Valid}, with
     * the values in argument order; it must not return {@code null}.
     *
     * @param v1  the first validation
     * @param v2  the second validation
     * @param v3  the third validation
     * @param v4  the fourth validation
     * @param v5  the fifth validation
     * @param v6  the sixth validation
     * @param v7  the seventh validation
     * @param v8  the eighth validation
     * @param f  combines the values; it must not return {@code null}
     * @param <E>  the error type, shared by every argument
     * @param <T1> the value type of {@code v1}
     * @param <T2> the value type of {@code v2}
     * @param <T3> the value type of {@code v3}
     * @param <T4> the value type of {@code v4}
     * @param <T5> the value type of {@code v5}
     * @param <T6> the value type of {@code v6}
     * @param <T7> the value type of {@code v7}
     * @param <T8> the value type of {@code v8}
     * @param <R>  the result type
     * @return {@code Valid} of the combined value, or the accumulated errors
     * @throws NullPointerException if any argument is null, or if {@code f} returns null
     */
    static <E extends @Nullable Object, T1 extends @Nullable Object, T2 extends @Nullable Object, T3 extends @Nullable Object, T4 extends @Nullable Object, T5 extends @Nullable Object, T6 extends @Nullable Object, T7 extends @Nullable Object, T8 extends @Nullable Object, R extends @Nullable Object> Validation<E, R> zipWith(Validation<? extends E, ? extends T1> v1, Validation<? extends E, ? extends T2> v2, Validation<? extends E, ? extends T3> v3, Validation<? extends E, ? extends T4> v4, Validation<? extends E, ? extends T5> v5, Validation<? extends E, ? extends T6> v6, Validation<? extends E, ? extends T7> v7, Validation<? extends E, ? extends T8> v8, Function8<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? super T6, ? super T7, ? super T8, ? extends R> f) {
        Objects.requireNonNull(v1, "v1 is null");
        Objects.requireNonNull(v2, "v2 is null");
        Objects.requireNonNull(v3, "v3 is null");
        Objects.requireNonNull(v4, "v4 is null");
        Objects.requireNonNull(v5, "v5 is null");
        Objects.requireNonNull(v6, "v6 is null");
        Objects.requireNonNull(v7, "v7 is null");
        Objects.requireNonNull(v8, "v8 is null");
        Objects.requireNonNull(f, "f is null");
        Vector.Builder<E> errors = null;
        errors = accumulate(errors, v1);
        errors = accumulate(errors, v2);
        errors = accumulate(errors, v3);
        errors = accumulate(errors, v4);
        errors = accumulate(errors, v5);
        errors = accumulate(errors, v6);
        errors = accumulate(errors, v7);
        errors = accumulate(errors, v8);
        if (errors == null) {
            return valid(Objects.requireNonNull(f.apply(v1.get(), v2.get(), v3.get(), v4.get(), v5.get(), v6.get(), v7.get(), v8.get()), "Validation.zipWith: f returned null"));
        }
        return new Invalid<>(NonEmptyVector.unsafeFromVector(errors.result()));
    }

    /**
     * One step of the error accumulation of the static {@code zip}/{@code zipWith} family: adds the errors of
     * {@code validation}, if it is {@code Invalid}, to {@code errors}, which is {@code null} until the first
     * {@code Invalid} operand creates it (the {@link #forEach(Iterable, Function)} pattern).
     */
    private static <E extends @Nullable Object> Vector.@Nullable Builder<E> accumulate(Vector.@Nullable Builder<E> errors, Validation<? extends E, ?> validation) {
        if (validation instanceof Invalid(var es)) {
            if (errors == null) {
                errors = Vector.newBuilder();
            }
            errors.addAll(es.toVector());
        }
        return errors;
    }

    /**
     * Returns this if it is {@code Valid}, otherwise the supplied alternative. The errors of the discarded side are
     * dropped: when both are {@code Invalid}, the result is the alternative's errors only. The supplier is called
     * only when this is {@code Invalid}.
     *
     * @param that supplies the alternative; it must not return {@code null}
     * @return this if {@code Valid}, otherwise {@code that.get()}
     * @throws NullPointerException if {@code that} is null, or if it supplies null
     */
    @SuppressWarnings("unchecked")
    default Validation<E, A> orElse(Supplier<? extends Validation<? extends E, ? extends A>> that) {
        Objects.requireNonNull(that, "that is null");
        return isValid() ? this : (Validation<E, A>) Objects.requireNonNull(that.get(), "Validation.orElse: that returned null");
    }

    // -- short-circuit

    /**
     * Runs {@code f} on this value and returns its result. <strong>Short-circuits</strong>: {@code f} is not called
     * when this is {@code Invalid}, and the errors of the validation it returns are never accumulated with this one's.
     * <p>
     * When we chain validations like this we only do the second validation if the first one is successful. If all we
     * are doing is chaining then we don't actually need {@code Validation} and could just use {@link Either}. Use
     * {@link #zip(Validation)}, {@link #zipWith(Validation, BiFunction)} or {@link #collectAll(Iterable)} to
     * accumulate; use {@code flatMap} for a step that needs the previous value, e.g. a cross-field rule once the
     * fields have been validated:
     * <pre>{@code
     * start.zip(end).flatMap(range -> range._1().isBefore(range._2()) ? valid(range) : invalid("start after end"));
     * }</pre>
     *
     * @param f   the next validation; it must not return {@code null}
     * @param <B> the value type of the result
     * @return {@code f.apply(value)} if this is {@code Valid}, otherwise this
     * @throws NullPointerException if {@code f} is null, or if it returns null
     */
    @SuppressWarnings("unchecked")
    default <B extends @Nullable Object> Validation<E, B> flatMap(Function<? super A, ? extends Validation<? extends E, ? extends B>> f) {
        Objects.requireNonNull(f, "f is null");
        return switch (this) {
            case Valid(var value) -> (Validation<E, B>) Objects.requireNonNull(f.apply(value), "Validation.flatMap: f returned null");
            case Invalid<E, A> invalid -> (Validation<E, B>) invalid;
        };
    }

    /**
     * {@link #flatMap(Function)} for a step that returns an {@link Either}: {@code Left(e)} becomes {@code Invalid(e)}.
     * Short-circuits like {@code flatMap}; the {@code Either} is a single rule, so there is nothing to accumulate.
     *
     * @param f   the next step; it must not return {@code null}
     * @param <B> the value type of the result
     * @return {@code fromEither(f.apply(value))} if this is {@code Valid}, otherwise this
     * @throws NullPointerException if {@code f} is null, or if it returns null
     */
    @SuppressWarnings("unchecked")
    default <B extends @Nullable Object> Validation<E, B> flatMapEither(Function<? super A, ? extends Either<? extends E, ? extends B>> f) {
        Objects.requireNonNull(f, "f is null");
        return switch (this) {
            case Valid(var value) -> fromEither(Objects.requireNonNull(f.apply(value), "Validation.flatMapEither: f returned null"));
            case Invalid<E, A> invalid -> (Validation<E, B>) invalid;
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
            case Valid(var value) -> valid(Objects.requireNonNull(f.apply(value), "Validation.map: f returned null"));
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
            case Invalid(var errors) -> new Invalid<>(errors.map(e -> Objects.requireNonNull(f.apply(e), "Validation.mapError: f returned null")));
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
            case Invalid(var errors) -> new Invalid<>(Objects.requireNonNull(f.apply(errors), "Validation.mapErrorAll: f returned null"));
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
            case Valid(var value) -> valid(Objects.requireNonNull(valueMapper.apply(value), "Validation.mapBoth: valueMapper returned null"));
            case Invalid(var errors) -> new Invalid<>(errors.map(e -> Objects.requireNonNull(errorMapper.apply(e), "Validation.mapBoth: errorMapper returned null")));
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
     * Returns the value if this is {@code Valid}, otherwise the value supplied by {@code supplier}. The supplier is
     * called only for an {@code Invalid}.
     *
     * @param supplier supplies the alternative
     * @return the value or {@code supplier.get()}
     * @throws NullPointerException if {@code supplier} is null
     */
    default A getOrElse(Supplier<? extends A> supplier) {
        Objects.requireNonNull(supplier, "supplier is null");
        return switch (this) {
            case Valid(var value) -> value;
            case Invalid<E, A> _ -> supplier.get();
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
     * Returns the value if this is {@code Valid}, otherwise throws the supplied throwable. The supplier is called only
     * for an {@code Invalid}.
     *
     * @param exceptionSupplier supplies the throwable
     * @param <X>               the type of the throwable
     * @return the value
     * @throws X                    if this is {@code Invalid}
     * @throws NullPointerException if {@code exceptionSupplier} is null, or if it returns null (a {@code null} is
     *                              never thrown)
     */
    default <X extends Throwable> A getOrElseThrow(Supplier<X> exceptionSupplier) throws X {
        Objects.requireNonNull(exceptionSupplier, "exceptionSupplier is null");
        return switch (this) {
            case Valid(var value) -> value;
            case Invalid<E, A> _ -> throw Objects.requireNonNull(exceptionSupplier.get(), "Validation.getOrElseThrow: exceptionSupplier returned null");
        };
    }

    /**
     * Returns the value if this is {@code Valid}, otherwise throws the throwable built from the errors.
     *
     * @param exceptionFunction builds the throwable from the errors; called only for an {@code Invalid}
     * @param <X>               the type of the throwable
     * @return the value
     * @throws X                    if this is {@code Invalid}
     * @throws NullPointerException if {@code exceptionFunction} is null, or if it returns null (a {@code null} is
     *                              never thrown)
     */
    default <X extends Throwable> A getOrElseThrow(Function<? super NonEmptyVector<E>, X> exceptionFunction) throws X {
        Objects.requireNonNull(exceptionFunction, "exceptionFunction is null");
        return switch (this) {
            case Valid(var value) -> value;
            case Invalid(var errors) -> throw Objects.requireNonNull(exceptionFunction.apply(errors), "Validation.getOrElseThrow: exceptionFunction returned null");
        };
    }

    /**
     * Returns the value if this is {@code Valid}, otherwise {@code null}.
     *
     * @return the value or {@code null}
     */
    default @Nullable A getOrNull() {
        return switch (this) {
            case Valid(var value) -> value;
            case Invalid<E, A> _ -> null;
        };
    }

    /**
     * Checks whether this {@code Validation} holds a value equal to {@code element}, as tested by
     * {@link Objects#equals(Object, Object)}.
     *
     * @param element the element to look for, may be {@code null}
     * @return {@code true} if this is {@code Valid(element)}, {@code false} otherwise (always for an {@code Invalid})
     */
    default boolean contains(@Nullable A element) {
        return this instanceof Valid(var value) && Objects.equals(value, element);
    }

    /**
     * Checks whether this {@code Validation} holds a value satisfying {@code predicate}.
     *
     * @param predicate the condition to test the value with
     * @return {@code true} if this is {@code Valid} and the predicate holds for its value, {@code false} otherwise
     * @throws NullPointerException if {@code predicate} is null
     */
    default boolean exists(Predicate<? super A> predicate) {
        Objects.requireNonNull(predicate, "predicate is null");
        return this instanceof Valid(var value) && predicate.test(value);
    }

    /**
     * Checks whether {@code predicate} holds for the value of this {@code Validation}; it holds vacuously for an
     * {@code Invalid}.
     *
     * @param predicate the condition to test the value with
     * @return {@code true} if this is {@code Invalid} or the predicate holds for the value, {@code false} otherwise
     * @throws NullPointerException if {@code predicate} is null
     */
    default boolean forAll(Predicate<? super A> predicate) {
        Objects.requireNonNull(predicate, "predicate is null");
        return !(this instanceof Valid(var value)) || predicate.test(value);
    }

    /**
     * Runs {@code action} on the value of a {@code Valid}; does nothing for an {@code Invalid}. The same as
     * {@link #tap(Consumer)} without the return value.
     *
     * @param action what to do with the value
     * @throws NullPointerException if {@code action} is null
     */
    default void forEach(Consumer<? super A> action) {
        Objects.requireNonNull(action, "action is null");
        if (this instanceof Valid(var value)) {
            action.accept(value);
        }
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
            case Invalid(var errors) -> Either.left(Objects.requireNonNull(f.apply(errors), "Validation.toEitherWith: f returned null"));
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
            case Invalid(var errors) -> Try.failure(Objects.requireNonNull(f.apply(errors), "Validation.toTry: f returned null"));
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
