package com.guizmaii.zazr.control;

import com.guizmaii.zazr.CheckedFunction1;
import com.guizmaii.zazr.collection.Iterator;
import com.guizmaii.zazr.collection.List;
import com.guizmaii.zazr.collection.Seq;
import com.guizmaii.zazr.collection.Vector;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * A replacement for {@link java.util.Optional}: a value that is either present, {@link Some}, or absent, {@link None}.
 * <p>
 * {@code Option} is a sealed interface with two record cases, so it is eliminated with an exhaustive {@code switch}:
 * <pre>{@code
 * String s = switch (option) {
 *     case Some(var value) -> "got " + value;
 *     case None() -> "nothing";
 * };
 * }</pre>
 * <p>
 * {@code Some} never holds {@code null}: {@link #some(Object)} throws and {@link #ofNullable(Object)} is the escape
 * hatch, as {@link java.util.Optional#of(Object)} and {@link java.util.Optional#ofNullable(Object)} are.
 * <p>
 * An {@code Option} is not a collection and not {@link Iterable} (design 3.2): to iterate or collect its value, convert
 * it explicitly with {@link #toVector()}, {@link #toList()} or {@link #stream()}.
 * <p>
 * The design is similar to {@link java.util.Optional} and to
 * <a href="http://www.scala-lang.org/api/current/#scala.Option">Scala's {@code Option}</a>.
 *
 * @param <T> the type of the optional value
 */
public sealed interface Option<T extends @Nullable Object> permits Option.Some, Option.None {

    /**
     * Creates an {@code Option} from a nullable value: {@code None} for {@code null}, {@code Some(value)} otherwise.
     * <p>
     * This is the escape hatch of the null policy, like {@link Optional#ofNullable(Object)}: {@link #some(Object)}
     * rejects {@code null}.
     *
     * @param value the value to wrap, possibly {@code null}
     * @param <T>   the (non-null) value type
     * @return {@code Some(value)} if the value is non-null, otherwise {@code None}
     */
    static <T extends @NonNull Object> Option<T> ofNullable(@Nullable T value) {
        return (value == null) ? none() : some(value);
    }

    /**
     * Turns many {@code Option}s into one {@code Option} of all their values: {@code Some} of a {@link Seq} of the
     * values in iteration order when every element is a {@code Some}, {@code None} as soon as one element is
     * {@code None}. The empty iterable gives {@code Some} of the empty {@code Seq}.
     * <pre>{@code
     * Option.collectAll(List.of(Option.some(1), Option.some(2))); // = Some(Seq(1, 2))
     * Option.collectAll(List.of(Option.some(1), Option.none()));  // = None
     * }</pre>
     *
     * @param values the {@code Option}s to collect
     * @param <T>    the value type
     * @return {@code Some} of all the values, or {@code None} if any element is {@code None}
     * @throws NullPointerException if {@code values} is null
     */
    static <T extends @Nullable Object> Option<Seq<T>> collectAll(Iterable<? extends Option<? extends T>> values) {
        Objects.requireNonNull(values, "values is null");
        Vector<T> vector = Vector.empty();
        for (Option<? extends T> value : values) {
            if (value.isEmpty()) {
                return Option.none();
            }
            vector = vector.append(value.get());
        }
        return Option.some(vector);
    }

    /**
     * Applies {@code mapper} to every element and collects the results as {@link #collectAll(Iterable)} does:
     * {@code Some} of a {@link Seq} of the mapped values when every call returns a {@code Some}, {@code None} as
     * soon as one call returns {@code None}. The mapper is not called for the elements after that one.
     * <pre>{@code
     * Option.forEach(List.of("1", "2"), s -> Option.some(Integer.parseInt(s))); // = Some(Seq(1, 2))
     * }</pre>
     *
     * @param values the elements to map
     * @param mapper a function from an element to an {@code Option}; it must not return {@code null}
     * @param <T>    the element type
     * @param <U>    the mapped value type
     * @return {@code Some} of all the mapped values, or {@code None} if one mapping is {@code None}
     * @throws NullPointerException if {@code values} or {@code mapper} is null
     */
    static <T extends @Nullable Object, U extends @Nullable Object> Option<Seq<U>> forEach(Iterable<? extends T> values, Function<? super T, ? extends Option<? extends U>> mapper) {
        Objects.requireNonNull(values, "values is null");
        Objects.requireNonNull(mapper, "mapper is null");
        return collectAll(Iterator.ofAll(values).map(mapper));
    }

    /**
     * Creates a {@code Some} containing the given value.
     * <p>
     * {@code null} is rejected, as {@link Optional#of(Object)} does; use {@link #ofNullable(Object)} to turn a
     * nullable value into {@code None}:
     * <pre>
     * Option.ofNullable(null); // yields None
     * Option.some(null);       // throws NullPointerException
     * </pre>
     *
     * @param value the value to wrap, must not be {@code null}
     * @param <T>   the value type
     * @return a {@code Some} containing {@code value}
     * @throws NullPointerException if {@code value} is null
     */
    static <T extends @Nullable Object> Option<T> some(T value) {
        return new Some<>(value);
    }

    /**
     * Returns the singleton {@code None} instance.
     *
     * @param <T> the option's component type
     * @return the singleton {@code None}
     */
    static <T extends @Nullable Object> Option<T> none() {
        @SuppressWarnings("unchecked")
        final None<T> none = (None<T>) None.INSTANCE;
        return none;
    }

    /**
     * Narrows a widened {@code Option<? extends T>} to {@code Option<T>} via a type-safe cast.
     * <p>
     * This is safe because immutable/read-only types are covariant.
     *
     * @param option the {@code Option} to narrow
     * @param <T>    the component type of the {@code Option}
     * @return the same {@code Option} instance, cast to {@code Option<T>}
     */
    @SuppressWarnings("unchecked")
    static <T extends @Nullable Object> Option<T> narrow(Option<? extends T> option) {
        return (Option<T>) option;
    }

    /**
     * Returns {@code Some} of the value supplied by {@code supplier} if {@code condition} is true,
     * or {@code None} if {@code condition} is false.
     *
     * @param <T>       the type of the optional value
     * @param condition the condition to test
     * @param supplier  a supplier of the value, must not return {@code null}
     * @return {@code Some} of the supplied value if {@code condition} is true, otherwise {@code None}
     * @throws NullPointerException if {@code supplier} is null, or supplies {@code null} when {@code condition} is true
     */
    static <T extends @Nullable Object> Option<T> when(boolean condition, Supplier<? extends T> supplier) {
        Objects.requireNonNull(supplier, "supplier is null");
        return condition ? some(supplier.get()) : none();
    }

    /**
     * Wraps a {@link java.util.Optional} in a new {@code Option}.
     *
     * @param optional the Java {@code Optional} to wrap
     * @param <T>      the type of the contained value
     * @return {@code Some(optional.get())} if the {@code Optional} is present, otherwise {@code None}
     */
    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    static <T extends @NonNull Object> Option<T> ofOptional(Optional<? extends T> optional) {
        Objects.requireNonNull(optional, "optional is null");
        return optional.<Option<T>>map(Option::some).orElseGet(Option::none);
    }

    /**
     * Checks whether this {@code Option} is empty.
     *
     * @return {@code true} if this is {@code None}, {@code false} if this is {@code Some}
     */
    boolean isEmpty();

    /**
     * Checks whether this {@code Option} contains a value.
     *
     * @return {@code true} if this is {@code Some}, {@code false} if this is {@code None}
     */
    default boolean isDefined() {
        return !isEmpty();
    }

    /**
     * Runs {@code action} if this is {@code None} and returns this {@code Option} unchanged; does nothing for a
     * {@code Some}. The counterpart of {@link #tap(Consumer)}.
     *
     * @param action what to run when there is no value
     * @return this {@code Option}
     * @throws NullPointerException if {@code action} is null
     */
    default Option<T> tapNone(Runnable action) {
        Objects.requireNonNull(action, "action is null");
        if (isEmpty()) {
            action.run();
        }
        return this;
    }

    /**
     * Returns the value contained in this {@code Some}, or throws if this is {@code None}.
     *
     * @return the contained value
     * @throws NoSuchElementException if this is {@code None}
     */
    T get();

    /**
     * Returns the value contained in this {@code Some}, or the provided {@code other} value if this is {@code None}.
     * <p>
     * Note that {@code other} is evaluated eagerly.
     *
     * @param other an alternative value to return if this is {@code None}
     * @return the contained value if defined, otherwise {@code other}
     */
    default T getOrElse(T other) {
        return isEmpty() ? other : get();
    }

    /**
     * Returns the value contained in this {@code Some}, or the value supplied by {@code supplier} if this is {@code None}.
     * <p>
     * The alternative value is evaluated lazily.
     *
     * @param supplier a supplier of an alternative value if this is {@code None}
     * @return the contained value if defined, otherwise the value returned by {@code supplier}
     * @throws NullPointerException if {@code supplier} is null
     */
    default T getOrElse(Supplier<? extends T> supplier) {
        Objects.requireNonNull(supplier, "supplier is null");
        return isEmpty() ? supplier.get() : get();
    }

    /**
     * Returns the value contained in this {@code Some}, or throws an exception provided by {@code exceptionSupplier} if this is {@code None}.
     *
     * @param exceptionSupplier a supplier of the exception to throw if this is {@code None}
     * @param <X>               the type of the exception
     * @return the contained value if defined
     * @throws X if this {@code Option} is {@code None}
     * @throws NullPointerException if {@code exceptionSupplier} is null
     */
    default <X extends Throwable> T getOrElseThrow(Supplier<X> exceptionSupplier) throws X {
        Objects.requireNonNull(exceptionSupplier, "exceptionSupplier is null");
        if (isEmpty()) {
            throw exceptionSupplier.get();
        } else {
            return get();
        }
    }

    /**
     * Returns the value contained in this {@code Some}, or {@code null} if this is {@code None}.
     *
     * @return the contained value if defined, otherwise {@code null}
     */
    default @Nullable T getOrNull() {
        return isEmpty() ? null : get();
    }

    /**
     * Returns this {@code Option} if it is non-empty, otherwise returns the provided alternative {@code Option}.
     *
     * @param other an alternative {@code Option} to return if this is {@code None}
     * @return this {@code Option} if defined, otherwise {@code other}
     */
    @SuppressWarnings("unchecked")
    default Option<T> orElse(Option<? extends T> other) {
        Objects.requireNonNull(other, "other is null");
        return isEmpty() ? (Option<T>) other : this;
    }

    /**
     * Returns this {@code Option} if it is non-empty; otherwise, returns the {@code Option} provided by the supplier.
     *
     * @param supplier a supplier of an alternative {@code Option} if this is {@code None}
     * @return this {@code Option} if defined, otherwise the result of {@code supplier.get()}
     * @throws NullPointerException if {@code supplier} is null
     */
    @SuppressWarnings("unchecked")
    default Option<T> orElse(Supplier<? extends Option<? extends T>> supplier) {
        Objects.requireNonNull(supplier, "supplier is null");
        return isEmpty() ? (Option<T>) supplier.get() : this;
    }

    /**
     * Checks whether this {@code Option} holds a value equal to {@code element}, as tested by {@link Objects#equals(Object, Object)}.
     *
     * @param element the element to look for, may be {@code null}
     * @return {@code true} if this is {@code Some(element)}, {@code false} otherwise (always for {@code None})
     */
    default boolean contains(@Nullable T element) {
        return isDefined() && Objects.equals(get(), element);
    }

    /**
     * Checks whether this {@code Option} holds a value satisfying the given predicate.
     *
     * @param predicate a predicate to test the contained value
     * @return {@code true} if this is {@code Some} and the predicate holds for its value, {@code false} otherwise
     * @throws NullPointerException if {@code predicate} is null
     */
    default boolean exists(Predicate<? super T> predicate) {
        Objects.requireNonNull(predicate, "predicate is null");
        return isDefined() && predicate.test(get());
    }

    /**
     * Checks whether the given predicate holds for the value of this {@code Option}; it holds vacuously for {@code None}.
     *
     * @param predicate a predicate to test the contained value
     * @return {@code true} if this is {@code None} or the predicate holds for the value of this {@code Some}, {@code false} otherwise
     * @throws NullPointerException if {@code predicate} is null
     */
    default boolean forAll(Predicate<? super T> predicate) {
        Objects.requireNonNull(predicate, "predicate is null");
        return isEmpty() || predicate.test(get());
    }

    /**
     * Performs the given action on the value of this {@code Some}; does nothing for {@code None}.
     *
     * @param action a consumer of the contained value
     * @throws NullPointerException if {@code action} is null
     */
    default void forEach(Consumer<? super T> action) {
        Objects.requireNonNull(action, "action is null");
        if (isDefined()) {
            action.accept(get());
        }
    }

    /**
     * Returns {@code Some(value)} if this {@code Option} is a {@code Some} and the contained value satisfies the given predicate.
     * Otherwise, returns {@code None}.
     *
     * @param predicate a predicate to test the contained value
     * @return {@code Some(value)} if the value satisfies the predicate, otherwise {@code None}
     * @throws NullPointerException if {@code predicate} is null
     */
    default Option<T> filter(Predicate<? super T> predicate) {
        Objects.requireNonNull(predicate, "predicate is null");
        return isEmpty() || predicate.test(get()) ? this : none();
    }

    /**
     * Transforms the value of this {@code Option} using the given mapper if it is a {@code Some}.
     * Returns {@code None} if this is {@code None}.
     * <p>
     * This is how a value maps to absence: the mapper returns {@link #none()} or {@link #ofNullable(Object)}. The mapper must return an {@code Option}, never {@code null}.
     *
     * @param mapper a function to transform the contained value
     * @param <U>    the type of the resulting {@code Option}'s value
     * @return a new {@code Option} containing the mapped value, or {@code None}
     * @throws NullPointerException if {@code mapper} is null
     */
    @SuppressWarnings("unchecked")
    default <U extends @Nullable Object> Option<U> flatMap(Function<? super T, ? extends Option<? extends U>> mapper) {
        Objects.requireNonNull(mapper, "mapper is null");
        return isEmpty() ? none() : (Option<U>) mapper.apply(get());
    }

    /**
     * Matches and transforms the value in one step: {@code mapper} returns {@code Some} of the new value for a
     * value it accepts and {@code None} for one it rejects. The {@code case} ergonomics come from a {@code switch}
     * inside the lambda:
     * <pre>{@code
     * Option<Double> radius = shape.collect(s -> switch (s) {
     *     case Circle c -> Option.some(c.radius());
     *     default -> Option.none();
     * });
     * }</pre>
     * On an {@code Option} this is the same operation as {@link #flatMap(Function)}, spelled the way it is spelled on
     * the collections.
     *
     * @param mapper a function from the value to {@code Some} of its replacement or {@code None}; it must not
     *               return {@code null}
     * @param <U>    the type of the collected value
     * @return the {@code Option} the mapper returned for a {@code Some}, {@code None} for a {@code None}
     * @throws NullPointerException if {@code mapper} is null, or if it returns {@code null}
     */
    @SuppressWarnings("unchecked")
    default <U extends @Nullable Object> Option<U> collect(Function<? super T, ? extends Option<? extends U>> mapper) {
        Objects.requireNonNull(mapper, "mapper is null");
        return isEmpty() ? none() : (Option<U>) Objects.requireNonNull(mapper.apply(get()), "Option.collect: mapper returned null");
    }

    /**
     * Transforms the value of this {@code Some} using the given mapper and wraps it in a new {@code Some}.
     * Returns {@code None} if this is {@code None}.
     * <p>
     * A mapper that returns {@code null} makes this throw {@link NullPointerException}: {@code Some} cannot hold {@code null} (design 3.9). Map to absence with {@link #flatMap(Function)} and {@link #ofNullable(Object)} instead.
     *
     * @param mapper a function to transform the contained value
     * @param <U>    the type of the resulting {@code Some}'s value
     * @return a new {@code Some} with the mapped value if this is defined, otherwise {@code None}
     * @throws NullPointerException if {@code mapper} is null
     */
    default <U extends @Nullable Object> Option<U> map(Function<? super T, ? extends U> mapper) {
        Objects.requireNonNull(mapper, "mapper is null");
        return isEmpty() ? none() : some(mapper.apply(get()));
    }

    /**
     * Applies the given checked function to the value of this {@code Some} under {@link Try}: a {@code Success} of
     * the result, a {@code Failure} of whatever the function throws, or a {@code Failure} of a
     * {@link NoSuchElementException} if this is {@code None}.
     *
     * @param <U>    the type of the resulting {@code Try}'s value
     * @param mapper a checked function to transform the contained value
     * @return a {@link Try.Success} containing the mapped value if this {@code Option} is defined and {@code mapper}
     * completes normally, a {@link Try.Failure} wrapping the exception thrown by {@code mapper} if it throws, or a
     * {@link Try.Failure} holding a {@link java.util.NoSuchElementException} if this is {@code None}
     * @throws NullPointerException if {@code mapper} is null
     */
    default <U extends @Nullable Object> Try<U> mapTry(CheckedFunction1<? super T, ? extends U> mapper) {
        Objects.requireNonNull(mapper, "mapper is null");
        return isEmpty() ? Try.failure(new NoSuchElementException("No value present")) : Try.success(get()).mapTry(mapper);
    }

    /**
     * Folds this {@code Option} into a single value by applying one of two functions:
     * <ul>
     *     <li>{@code ifNone} is applied if this is {@code None}</li>
     *     <li>{@code f} is applied to the contained value if this is {@code Some}</li>
     * </ul>
     *
     * @param ifNone a function to produce a value if this is {@code None}
     * @param f      a function to transform the contained value if this is {@code Some}
     * @param <U>    the type of the folded result
     * @return the result of applying {@code f} or {@code ifNone} depending on whether this is {@code Some} or {@code None}
     * @throws NullPointerException if {@code ifNone} or {@code f} is null
     */
    default <U extends @Nullable Object> U fold(Supplier<? extends U> ifNone, Function<? super T, ? extends U> f) {
        Objects.requireNonNull(ifNone, "ifNone is null");
        Objects.requireNonNull(f, "f is null");
        return isEmpty() ? ifNone.get() : f.apply(get());
    }

    /**
     * Runs {@code action} on the value if this is a {@code Some} and returns this {@code Option} unchanged; does
     * nothing for {@code None}. Whatever the action throws propagates to the caller.
     *
     * @param action what to do with the value
     * @return this {@code Option}
     * @throws NullPointerException if {@code action} is null
     */
    default Option<T> tap(Consumer<? super T> action) {
        Objects.requireNonNull(action, "action is null");
        if (isDefined()) {
            action.accept(get());
        }
        return this;
    }

    // -- conversions (design 3.2)

    /**
     * Converts this {@code Option} to an {@link Either}: {@code Right(value)} for {@code Some}, {@code Left(leftSupplier.get())} for {@code None}.
     * <p>
     * The supplier is invoked only for {@code None}; it must not supply {@code null}, since {@code Left} cannot hold {@code null} (design 3.9).
     *
     * @param leftSupplier a supplier of the left value, invoked if this is {@code None}
     * @param <L>          the left type of the {@link Either}
     * @return a {@code Right} of the contained value, or a {@code Left} of the supplied value
     * @throws NullPointerException if {@code leftSupplier} is null, or if it supplies {@code null}
     */
    default <L extends @Nullable Object> Either<L, T> toEither(Supplier<? extends L> leftSupplier) {
        Objects.requireNonNull(leftSupplier, "leftSupplier is null");
        return isEmpty() ? Either.left(leftSupplier.get()) : Either.right(get());
    }

    /**
     * Converts this {@code Option} to a {@link Try}: {@code Success(value)} for {@code Some}, {@code Failure(ifEmpty.get())} for {@code None}.
     * <p>
     * The supplier is invoked only for {@code None}; it must not supply {@code null} nor a fatal throwable (see {@link Try}).
     *
     * @param ifEmpty a supplier of the failure cause, invoked if this is {@code None}
     * @return a {@code Success} of the contained value, or a {@code Failure} of the supplied throwable
     * @throws NullPointerException if {@code ifEmpty} is null, or if it supplies {@code null}
     */
    default Try<T> toTry(Supplier<? extends Throwable> ifEmpty) {
        Objects.requireNonNull(ifEmpty, "ifEmpty is null");
        return isEmpty() ? Try.failure(ifEmpty.get()) : Try.success(get());
    }

    /**
     * Converts this {@code Option} to a {@link Validation}: {@code Valid(value)} for {@code Some}, {@code Invalid(invalidSupplier.get())} for {@code None}.
     * <p>
     * The supplier is invoked only for {@code None}; it must not supply {@code null}, since {@code Invalid} cannot hold {@code null} (design 3.9).
     *
     * @param invalidSupplier a supplier of the error, invoked if this is {@code None}
     * @param <E>             the error type of the {@link Validation}
     * @return a {@code Valid} of the contained value, or an {@code Invalid} of the supplied error
     * @throws NullPointerException if {@code invalidSupplier} is null, or if it supplies {@code null}
     */
    default <E extends @Nullable Object> Validation<E, T> toValidation(Supplier<? extends E> invalidSupplier) {
        Objects.requireNonNull(invalidSupplier, "invalidSupplier is null");
        return isEmpty() ? Validation.invalid(invalidSupplier.get()) : Validation.valid(get());
    }

    /**
     * Converts this {@code Option} to a {@link Vector} of zero or one element.
     *
     * @return {@code Vector.of(value)} for {@code Some}, the empty {@code Vector} for {@code None}
     */
    default Vector<T> toVector() {
        return isEmpty() ? Vector.empty() : Vector.of(get());
    }

    /**
     * Converts this {@code Option} to a {@link List} of zero or one element.
     *
     * @return {@code List.of(value)} for {@code Some}, the empty {@code List} for {@code None}
     */
    default List<T> toList() {
        return isEmpty() ? List.empty() : List.of(get());
    }

    /**
     * Converts this {@code Option} to a {@link java.util.Optional}.
     *
     * @return {@code Optional.of(value)} for {@code Some}, {@code Optional.empty()} for {@code None}
     */
    default Optional<T> toOptional() {
        return isEmpty() ? Optional.empty() : Optional.of(get());
    }

    /**
     * Converts this {@code Option} to a sequential {@link java.util.stream.Stream} of zero or one element.
     *
     * @return {@code Stream.of(value)} for {@code Some}, an empty {@code Stream} for {@code None}
     */
    default java.util.stream.Stream<T> stream() {
        return isEmpty() ? java.util.stream.Stream.empty() : java.util.stream.Stream.of(get());
    }

    @Override
    boolean equals(@Nullable Object o);

    @Override
    int hashCode();

    @Override
    String toString();

    /**
     * A defined {@link Option}. The value is never {@code null}: the constructor and {@link Option#some(Object)}
     * throw, {@link Option#ofNullable(Object)} turns a nullable value into {@code None}.
     *
     * @param value the value, never {@code null}
     * @param <T>   The type of the optional value.
     */
    record Some<T extends @Nullable Object>(T value) implements Option<T> {

        /**
         * Rejects {@code null}.
         *
         * @throws NullPointerException if {@code value} is null
         */
        public Some {
            Objects.requireNonNull(value, "value is null");
        }

        @Override
        public T get() {
            return value;
        }

        @Override
        public boolean isEmpty() {
            return false;
        }

        @Override
        public String toString() {
            return "Some(" + value + ")";
        }
    }

    /**
     * The undefined {@link Option}. {@link Option#none()} returns a shared instance; {@code new None<>()} is legal,
     * a record constructor is public, and equal to it, since a record without components equals every other.
     *
     * @param <T> The type of the optional value.
     */
    record None<T extends @Nullable Object>() implements Option<T> {

        private static final None<?> INSTANCE = new None<>();

        @Override
        public T get() {
            throw new NoSuchElementException("No value present");
        }

        @Override
        public boolean isEmpty() {
            return true;
        }

        @Override
        public String toString() {
            return "None";
        }
    }
}
