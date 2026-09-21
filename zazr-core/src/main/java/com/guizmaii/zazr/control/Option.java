package com.guizmaii.zazr.control;

import com.guizmaii.zazr.CheckedFunction1;
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
import com.guizmaii.zazr.collection.Iterator;
import com.guizmaii.zazr.collection.List;
import com.guizmaii.zazr.collection.Vector;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiFunction;
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
     * Turns many {@code Option}s into one {@code Option} of all their values: {@code Some} of a {@link Vector} of
     * the values in iteration order when every element is a {@code Some}, {@code None} as soon as one element is
     * {@code None}. The empty iterable gives {@code Some} of the empty {@code Vector}.
     * <pre>{@code
     * Option.collectAll(List.of(Option.some(1), Option.some(2))); // = Some(Vector(1, 2))
     * Option.collectAll(List.of(Option.some(1), Option.none()));  // = None
     * }</pre>
     *
     * @param values the {@code Option}s to collect
     * @param <T>    the value type
     * @return {@code Some} of all the values, or {@code None} if any element is {@code None}
     * @throws NullPointerException if {@code values} is null
     */
    static <T extends @Nullable Object> Option<Vector<T>> collectAll(Iterable<? extends Option<? extends T>> values) {
        Objects.requireNonNull(values, "values is null");
        final Vector.Builder<T> builder = Vector.newBuilder();
        for (Option<? extends T> value : values) {
            if (value.isEmpty()) {
                return Option.none();
            }
            builder.add(value.get());
        }
        return Option.some(builder.result());
    }

    /**
     * Applies {@code mapper} to every element and collects the results as {@link #collectAll(Iterable)} does:
     * {@code Some} of a {@link Vector} of the mapped values when every call returns a {@code Some}, {@code None} as
     * soon as one call returns {@code None}. The mapper is not called for the elements after that one.
     * <pre>{@code
     * Option.forEach(List.of("1", "2"), s -> Option.some(Integer.parseInt(s))); // = Some(Vector(1, 2))
     * }</pre>
     *
     * @param values the elements to map
     * @param mapper a function from an element to an {@code Option}; it must not return {@code null}
     * @param <T>    the element type
     * @param <U>    the mapped value type
     * @return {@code Some} of all the mapped values, or {@code None} if one mapping is {@code None}
     * @throws NullPointerException if {@code values} or {@code mapper} is null
     */
    static <T extends @Nullable Object, U extends @Nullable Object> Option<Vector<U>> forEach(Iterable<? extends T> values, Function<? super T, ? extends Option<? extends U>> mapper) {
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

    // -- zip (design 3.4)

    /**
     * Pairs this value with {@code that}'s, failing fast: {@code Some} of the pair when both are {@code Some},
     * otherwise {@code None}. The same as {@link #zipWith(Option, BiFunction)} with {@code Tuple::of}.
     * <pre>{@code
     * Option.some(1).zip(Option.some("a")); // = Some((1, a))
     * Option.some(1).zip(Option.none());    // = None
     * }</pre>
     *
     * @param that the other option
     * @param <U>  the value type of {@code that}
     * @return {@code Some} of the pair of values, or {@code None} if either side is {@code None}
     * @throws NullPointerException if {@code that} is null
     */
    default <U extends @Nullable Object> Option<Tuple2<T, U>> zip(Option<? extends U> that) {
        return zipWith(that, Tuple::of);
    }

    /**
     * Combines this value with {@code that}'s through {@code f}, failing fast: {@code Some} of the result when both
     * are {@code Some}, otherwise {@code None}. {@code f} is called only when both are {@code Some}; it must not
     * return {@code null}, since {@code Some} cannot hold {@code null} (design 3.9).
     *
     * @param that the other option
     * @param f    combines the two values; it must not return {@code null}
     * @param <U>  the value type of {@code that}
     * @param <V>  the result type
     * @return {@code Some} of the combined value, or {@code None} if either side is {@code None}
     * @throws NullPointerException if {@code that} or {@code f} is null, or if {@code f} returns null
     */
    default <U extends @Nullable Object, V extends @Nullable Object> Option<V> zipWith(Option<? extends U> that, BiFunction<? super T, ? super U, ? extends V> f) {
        Objects.requireNonNull(that, "that is null");
        Objects.requireNonNull(f, "f is null");
        if (isEmpty() || that.isEmpty()) {
            return none();
        }
        return some(Objects.requireNonNull(f.apply(get(), that.get()), "Option.zipWith: f returned null"));
    }

    /**
     * {@link #zip(Option)} keeping this value: this {@code Some} when both are {@code Some}, otherwise {@code None}.
     * Both sides are inspected, so this is not {@link #orElse(Option)}: {@code Some(1).zipLeft(None)} is {@code None}.
     *
     * @param that the other option
     * @param <U>  the value type of {@code that}
     * @return this {@code Some}, or {@code None} if either side is {@code None}
     * @throws NullPointerException if {@code that} is null
     */
    default <U extends @Nullable Object> Option<T> zipLeft(Option<? extends U> that) {
        Objects.requireNonNull(that, "that is null");
        return isEmpty() || that.isEmpty() ? none() : this;
    }

    /**
     * {@link #zip(Option)} keeping {@code that}'s value: {@code that} when both are {@code Some}, otherwise
     * {@code None}. Both sides are inspected: {@code None.zipRight(Some(1))} is {@code None}.
     *
     * @param that the other option
     * @param <U>  the value type of {@code that}
     * @return {@code that}, or {@code None} if either side is {@code None}
     * @throws NullPointerException if {@code that} is null
     */
    default <U extends @Nullable Object> Option<U> zipRight(Option<? extends U> that) {
        Objects.requireNonNull(that, "that is null");
        return isEmpty() || that.isEmpty() ? none() : narrow(that);
    }

    /**
     * Pairs the values of two {@code Option}s, failing fast: {@code Some} of the tuple of the values when every
     * argument is a {@code Some}, otherwise {@code None}. The same as {@link #zipWith(Option, Option, BiFunction)}
     * with {@code Tuple::of}.
     *
     * @param o1  the first {@code Option}
     * @param o2  the second {@code Option}
     * @param <T1> the value type of {@code o1}
     * @param <T2> the value type of {@code o2}
     * @return {@code Some} of the tuple of the values, or {@code None} if any argument is {@code None}
     * @throws NullPointerException if any argument is null
     */
    static <T1 extends @Nullable Object, T2 extends @Nullable Object> Option<Tuple2<T1, T2>> zip(Option<? extends T1> o1, Option<? extends T2> o2) {
        return zipWith(o1, o2, Tuple::of);
    }

    /**
     * Combines the values of two {@code Option}s through {@code f}, failing fast: {@code Some} of the result when
     * every argument is a {@code Some}, otherwise {@code None}. {@code f} is called only when every argument is a
     * {@code Some}, with the values in argument order; it must not return {@code null}, since {@code Some} cannot hold
     * {@code null} (design 3.9).
     *
     * @param o1  the first {@code Option}
     * @param o2  the second {@code Option}
     * @param f  combines the values; it must not return {@code null}
     * @param <T1> the value type of {@code o1}
     * @param <T2> the value type of {@code o2}
     * @param <R>  the result type
     * @return {@code Some} of the combined value, or {@code None} if any argument is {@code None}
     * @throws NullPointerException if any argument is null, or if {@code f} returns null
     */
    static <T1 extends @Nullable Object, T2 extends @Nullable Object, R extends @Nullable Object> Option<R> zipWith(Option<? extends T1> o1, Option<? extends T2> o2, BiFunction<? super T1, ? super T2, ? extends R> f) {
        Objects.requireNonNull(o1, "o1 is null");
        Objects.requireNonNull(o2, "o2 is null");
        Objects.requireNonNull(f, "f is null");
        if (o1.isEmpty() || o2.isEmpty()) {
            return none();
        }
        return some(Objects.requireNonNull(f.apply(o1.get(), o2.get()), "Option.zipWith: f returned null"));
    }

    /**
     * Pairs the values of three {@code Option}s, failing fast: {@code Some} of the tuple of the values when every
     * argument is a {@code Some}, otherwise {@code None}. The same as {@link #zipWith(Option, Option, Option, Function3)}
     * with {@code Tuple::of}.
     *
     * @param o1  the first {@code Option}
     * @param o2  the second {@code Option}
     * @param o3  the third {@code Option}
     * @param <T1> the value type of {@code o1}
     * @param <T2> the value type of {@code o2}
     * @param <T3> the value type of {@code o3}
     * @return {@code Some} of the tuple of the values, or {@code None} if any argument is {@code None}
     * @throws NullPointerException if any argument is null
     */
    static <T1 extends @Nullable Object, T2 extends @Nullable Object, T3 extends @Nullable Object> Option<Tuple3<T1, T2, T3>> zip(Option<? extends T1> o1, Option<? extends T2> o2, Option<? extends T3> o3) {
        return zipWith(o1, o2, o3, Tuple::of);
    }

    /**
     * Combines the values of three {@code Option}s through {@code f}, failing fast: {@code Some} of the result when
     * every argument is a {@code Some}, otherwise {@code None}. {@code f} is called only when every argument is a
     * {@code Some}, with the values in argument order; it must not return {@code null}, since {@code Some} cannot hold
     * {@code null} (design 3.9).
     *
     * @param o1  the first {@code Option}
     * @param o2  the second {@code Option}
     * @param o3  the third {@code Option}
     * @param f  combines the values; it must not return {@code null}
     * @param <T1> the value type of {@code o1}
     * @param <T2> the value type of {@code o2}
     * @param <T3> the value type of {@code o3}
     * @param <R>  the result type
     * @return {@code Some} of the combined value, or {@code None} if any argument is {@code None}
     * @throws NullPointerException if any argument is null, or if {@code f} returns null
     */
    static <T1 extends @Nullable Object, T2 extends @Nullable Object, T3 extends @Nullable Object, R extends @Nullable Object> Option<R> zipWith(Option<? extends T1> o1, Option<? extends T2> o2, Option<? extends T3> o3, Function3<? super T1, ? super T2, ? super T3, ? extends R> f) {
        Objects.requireNonNull(o1, "o1 is null");
        Objects.requireNonNull(o2, "o2 is null");
        Objects.requireNonNull(o3, "o3 is null");
        Objects.requireNonNull(f, "f is null");
        if (o1.isEmpty() || o2.isEmpty() || o3.isEmpty()) {
            return none();
        }
        return some(Objects.requireNonNull(f.apply(o1.get(), o2.get(), o3.get()), "Option.zipWith: f returned null"));
    }

    /**
     * Pairs the values of four {@code Option}s, failing fast: {@code Some} of the tuple of the values when every
     * argument is a {@code Some}, otherwise {@code None}. The same as {@link #zipWith(Option, Option, Option, Option, Function4)}
     * with {@code Tuple::of}.
     *
     * @param o1  the first {@code Option}
     * @param o2  the second {@code Option}
     * @param o3  the third {@code Option}
     * @param o4  the fourth {@code Option}
     * @param <T1> the value type of {@code o1}
     * @param <T2> the value type of {@code o2}
     * @param <T3> the value type of {@code o3}
     * @param <T4> the value type of {@code o4}
     * @return {@code Some} of the tuple of the values, or {@code None} if any argument is {@code None}
     * @throws NullPointerException if any argument is null
     */
    static <T1 extends @Nullable Object, T2 extends @Nullable Object, T3 extends @Nullable Object, T4 extends @Nullable Object> Option<Tuple4<T1, T2, T3, T4>> zip(Option<? extends T1> o1, Option<? extends T2> o2, Option<? extends T3> o3, Option<? extends T4> o4) {
        return zipWith(o1, o2, o3, o4, Tuple::of);
    }

    /**
     * Combines the values of four {@code Option}s through {@code f}, failing fast: {@code Some} of the result when
     * every argument is a {@code Some}, otherwise {@code None}. {@code f} is called only when every argument is a
     * {@code Some}, with the values in argument order; it must not return {@code null}, since {@code Some} cannot hold
     * {@code null} (design 3.9).
     *
     * @param o1  the first {@code Option}
     * @param o2  the second {@code Option}
     * @param o3  the third {@code Option}
     * @param o4  the fourth {@code Option}
     * @param f  combines the values; it must not return {@code null}
     * @param <T1> the value type of {@code o1}
     * @param <T2> the value type of {@code o2}
     * @param <T3> the value type of {@code o3}
     * @param <T4> the value type of {@code o4}
     * @param <R>  the result type
     * @return {@code Some} of the combined value, or {@code None} if any argument is {@code None}
     * @throws NullPointerException if any argument is null, or if {@code f} returns null
     */
    static <T1 extends @Nullable Object, T2 extends @Nullable Object, T3 extends @Nullable Object, T4 extends @Nullable Object, R extends @Nullable Object> Option<R> zipWith(Option<? extends T1> o1, Option<? extends T2> o2, Option<? extends T3> o3, Option<? extends T4> o4, Function4<? super T1, ? super T2, ? super T3, ? super T4, ? extends R> f) {
        Objects.requireNonNull(o1, "o1 is null");
        Objects.requireNonNull(o2, "o2 is null");
        Objects.requireNonNull(o3, "o3 is null");
        Objects.requireNonNull(o4, "o4 is null");
        Objects.requireNonNull(f, "f is null");
        if (o1.isEmpty() || o2.isEmpty() || o3.isEmpty() || o4.isEmpty()) {
            return none();
        }
        return some(Objects.requireNonNull(f.apply(o1.get(), o2.get(), o3.get(), o4.get()), "Option.zipWith: f returned null"));
    }

    /**
     * Pairs the values of five {@code Option}s, failing fast: {@code Some} of the tuple of the values when every
     * argument is a {@code Some}, otherwise {@code None}. The same as {@link #zipWith(Option, Option, Option, Option, Option, Function5)}
     * with {@code Tuple::of}.
     *
     * @param o1  the first {@code Option}
     * @param o2  the second {@code Option}
     * @param o3  the third {@code Option}
     * @param o4  the fourth {@code Option}
     * @param o5  the fifth {@code Option}
     * @param <T1> the value type of {@code o1}
     * @param <T2> the value type of {@code o2}
     * @param <T3> the value type of {@code o3}
     * @param <T4> the value type of {@code o4}
     * @param <T5> the value type of {@code o5}
     * @return {@code Some} of the tuple of the values, or {@code None} if any argument is {@code None}
     * @throws NullPointerException if any argument is null
     */
    static <T1 extends @Nullable Object, T2 extends @Nullable Object, T3 extends @Nullable Object, T4 extends @Nullable Object, T5 extends @Nullable Object> Option<Tuple5<T1, T2, T3, T4, T5>> zip(Option<? extends T1> o1, Option<? extends T2> o2, Option<? extends T3> o3, Option<? extends T4> o4, Option<? extends T5> o5) {
        return zipWith(o1, o2, o3, o4, o5, Tuple::of);
    }

    /**
     * Combines the values of five {@code Option}s through {@code f}, failing fast: {@code Some} of the result when
     * every argument is a {@code Some}, otherwise {@code None}. {@code f} is called only when every argument is a
     * {@code Some}, with the values in argument order; it must not return {@code null}, since {@code Some} cannot hold
     * {@code null} (design 3.9).
     *
     * @param o1  the first {@code Option}
     * @param o2  the second {@code Option}
     * @param o3  the third {@code Option}
     * @param o4  the fourth {@code Option}
     * @param o5  the fifth {@code Option}
     * @param f  combines the values; it must not return {@code null}
     * @param <T1> the value type of {@code o1}
     * @param <T2> the value type of {@code o2}
     * @param <T3> the value type of {@code o3}
     * @param <T4> the value type of {@code o4}
     * @param <T5> the value type of {@code o5}
     * @param <R>  the result type
     * @return {@code Some} of the combined value, or {@code None} if any argument is {@code None}
     * @throws NullPointerException if any argument is null, or if {@code f} returns null
     */
    static <T1 extends @Nullable Object, T2 extends @Nullable Object, T3 extends @Nullable Object, T4 extends @Nullable Object, T5 extends @Nullable Object, R extends @Nullable Object> Option<R> zipWith(Option<? extends T1> o1, Option<? extends T2> o2, Option<? extends T3> o3, Option<? extends T4> o4, Option<? extends T5> o5, Function5<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? extends R> f) {
        Objects.requireNonNull(o1, "o1 is null");
        Objects.requireNonNull(o2, "o2 is null");
        Objects.requireNonNull(o3, "o3 is null");
        Objects.requireNonNull(o4, "o4 is null");
        Objects.requireNonNull(o5, "o5 is null");
        Objects.requireNonNull(f, "f is null");
        if (o1.isEmpty() || o2.isEmpty() || o3.isEmpty() || o4.isEmpty() || o5.isEmpty()) {
            return none();
        }
        return some(Objects.requireNonNull(f.apply(o1.get(), o2.get(), o3.get(), o4.get(), o5.get()), "Option.zipWith: f returned null"));
    }

    /**
     * Pairs the values of six {@code Option}s, failing fast: {@code Some} of the tuple of the values when every
     * argument is a {@code Some}, otherwise {@code None}. The same as {@link #zipWith(Option, Option, Option, Option, Option, Option, Function6)}
     * with {@code Tuple::of}.
     *
     * @param o1  the first {@code Option}
     * @param o2  the second {@code Option}
     * @param o3  the third {@code Option}
     * @param o4  the fourth {@code Option}
     * @param o5  the fifth {@code Option}
     * @param o6  the sixth {@code Option}
     * @param <T1> the value type of {@code o1}
     * @param <T2> the value type of {@code o2}
     * @param <T3> the value type of {@code o3}
     * @param <T4> the value type of {@code o4}
     * @param <T5> the value type of {@code o5}
     * @param <T6> the value type of {@code o6}
     * @return {@code Some} of the tuple of the values, or {@code None} if any argument is {@code None}
     * @throws NullPointerException if any argument is null
     */
    static <T1 extends @Nullable Object, T2 extends @Nullable Object, T3 extends @Nullable Object, T4 extends @Nullable Object, T5 extends @Nullable Object, T6 extends @Nullable Object> Option<Tuple6<T1, T2, T3, T4, T5, T6>> zip(Option<? extends T1> o1, Option<? extends T2> o2, Option<? extends T3> o3, Option<? extends T4> o4, Option<? extends T5> o5, Option<? extends T6> o6) {
        return zipWith(o1, o2, o3, o4, o5, o6, Tuple::of);
    }

    /**
     * Combines the values of six {@code Option}s through {@code f}, failing fast: {@code Some} of the result when
     * every argument is a {@code Some}, otherwise {@code None}. {@code f} is called only when every argument is a
     * {@code Some}, with the values in argument order; it must not return {@code null}, since {@code Some} cannot hold
     * {@code null} (design 3.9).
     *
     * @param o1  the first {@code Option}
     * @param o2  the second {@code Option}
     * @param o3  the third {@code Option}
     * @param o4  the fourth {@code Option}
     * @param o5  the fifth {@code Option}
     * @param o6  the sixth {@code Option}
     * @param f  combines the values; it must not return {@code null}
     * @param <T1> the value type of {@code o1}
     * @param <T2> the value type of {@code o2}
     * @param <T3> the value type of {@code o3}
     * @param <T4> the value type of {@code o4}
     * @param <T5> the value type of {@code o5}
     * @param <T6> the value type of {@code o6}
     * @param <R>  the result type
     * @return {@code Some} of the combined value, or {@code None} if any argument is {@code None}
     * @throws NullPointerException if any argument is null, or if {@code f} returns null
     */
    static <T1 extends @Nullable Object, T2 extends @Nullable Object, T3 extends @Nullable Object, T4 extends @Nullable Object, T5 extends @Nullable Object, T6 extends @Nullable Object, R extends @Nullable Object> Option<R> zipWith(Option<? extends T1> o1, Option<? extends T2> o2, Option<? extends T3> o3, Option<? extends T4> o4, Option<? extends T5> o5, Option<? extends T6> o6, Function6<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? super T6, ? extends R> f) {
        Objects.requireNonNull(o1, "o1 is null");
        Objects.requireNonNull(o2, "o2 is null");
        Objects.requireNonNull(o3, "o3 is null");
        Objects.requireNonNull(o4, "o4 is null");
        Objects.requireNonNull(o5, "o5 is null");
        Objects.requireNonNull(o6, "o6 is null");
        Objects.requireNonNull(f, "f is null");
        if (o1.isEmpty() || o2.isEmpty() || o3.isEmpty() || o4.isEmpty() || o5.isEmpty() || o6.isEmpty()) {
            return none();
        }
        return some(Objects.requireNonNull(f.apply(o1.get(), o2.get(), o3.get(), o4.get(), o5.get(), o6.get()), "Option.zipWith: f returned null"));
    }

    /**
     * Pairs the values of seven {@code Option}s, failing fast: {@code Some} of the tuple of the values when every
     * argument is a {@code Some}, otherwise {@code None}. The same as {@link #zipWith(Option, Option, Option, Option, Option, Option, Option, Function7)}
     * with {@code Tuple::of}.
     *
     * @param o1  the first {@code Option}
     * @param o2  the second {@code Option}
     * @param o3  the third {@code Option}
     * @param o4  the fourth {@code Option}
     * @param o5  the fifth {@code Option}
     * @param o6  the sixth {@code Option}
     * @param o7  the seventh {@code Option}
     * @param <T1> the value type of {@code o1}
     * @param <T2> the value type of {@code o2}
     * @param <T3> the value type of {@code o3}
     * @param <T4> the value type of {@code o4}
     * @param <T5> the value type of {@code o5}
     * @param <T6> the value type of {@code o6}
     * @param <T7> the value type of {@code o7}
     * @return {@code Some} of the tuple of the values, or {@code None} if any argument is {@code None}
     * @throws NullPointerException if any argument is null
     */
    static <T1 extends @Nullable Object, T2 extends @Nullable Object, T3 extends @Nullable Object, T4 extends @Nullable Object, T5 extends @Nullable Object, T6 extends @Nullable Object, T7 extends @Nullable Object> Option<Tuple7<T1, T2, T3, T4, T5, T6, T7>> zip(Option<? extends T1> o1, Option<? extends T2> o2, Option<? extends T3> o3, Option<? extends T4> o4, Option<? extends T5> o5, Option<? extends T6> o6, Option<? extends T7> o7) {
        return zipWith(o1, o2, o3, o4, o5, o6, o7, Tuple::of);
    }

    /**
     * Combines the values of seven {@code Option}s through {@code f}, failing fast: {@code Some} of the result when
     * every argument is a {@code Some}, otherwise {@code None}. {@code f} is called only when every argument is a
     * {@code Some}, with the values in argument order; it must not return {@code null}, since {@code Some} cannot hold
     * {@code null} (design 3.9).
     *
     * @param o1  the first {@code Option}
     * @param o2  the second {@code Option}
     * @param o3  the third {@code Option}
     * @param o4  the fourth {@code Option}
     * @param o5  the fifth {@code Option}
     * @param o6  the sixth {@code Option}
     * @param o7  the seventh {@code Option}
     * @param f  combines the values; it must not return {@code null}
     * @param <T1> the value type of {@code o1}
     * @param <T2> the value type of {@code o2}
     * @param <T3> the value type of {@code o3}
     * @param <T4> the value type of {@code o4}
     * @param <T5> the value type of {@code o5}
     * @param <T6> the value type of {@code o6}
     * @param <T7> the value type of {@code o7}
     * @param <R>  the result type
     * @return {@code Some} of the combined value, or {@code None} if any argument is {@code None}
     * @throws NullPointerException if any argument is null, or if {@code f} returns null
     */
    static <T1 extends @Nullable Object, T2 extends @Nullable Object, T3 extends @Nullable Object, T4 extends @Nullable Object, T5 extends @Nullable Object, T6 extends @Nullable Object, T7 extends @Nullable Object, R extends @Nullable Object> Option<R> zipWith(Option<? extends T1> o1, Option<? extends T2> o2, Option<? extends T3> o3, Option<? extends T4> o4, Option<? extends T5> o5, Option<? extends T6> o6, Option<? extends T7> o7, Function7<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? super T6, ? super T7, ? extends R> f) {
        Objects.requireNonNull(o1, "o1 is null");
        Objects.requireNonNull(o2, "o2 is null");
        Objects.requireNonNull(o3, "o3 is null");
        Objects.requireNonNull(o4, "o4 is null");
        Objects.requireNonNull(o5, "o5 is null");
        Objects.requireNonNull(o6, "o6 is null");
        Objects.requireNonNull(o7, "o7 is null");
        Objects.requireNonNull(f, "f is null");
        if (o1.isEmpty() || o2.isEmpty() || o3.isEmpty() || o4.isEmpty() || o5.isEmpty() || o6.isEmpty() || o7.isEmpty()) {
            return none();
        }
        return some(Objects.requireNonNull(f.apply(o1.get(), o2.get(), o3.get(), o4.get(), o5.get(), o6.get(), o7.get()), "Option.zipWith: f returned null"));
    }

    /**
     * Pairs the values of eight {@code Option}s, failing fast: {@code Some} of the tuple of the values when every
     * argument is a {@code Some}, otherwise {@code None}. The same as {@link #zipWith(Option, Option, Option, Option, Option, Option, Option, Option, Function8)}
     * with {@code Tuple::of}.
     *
     * @param o1  the first {@code Option}
     * @param o2  the second {@code Option}
     * @param o3  the third {@code Option}
     * @param o4  the fourth {@code Option}
     * @param o5  the fifth {@code Option}
     * @param o6  the sixth {@code Option}
     * @param o7  the seventh {@code Option}
     * @param o8  the eighth {@code Option}
     * @param <T1> the value type of {@code o1}
     * @param <T2> the value type of {@code o2}
     * @param <T3> the value type of {@code o3}
     * @param <T4> the value type of {@code o4}
     * @param <T5> the value type of {@code o5}
     * @param <T6> the value type of {@code o6}
     * @param <T7> the value type of {@code o7}
     * @param <T8> the value type of {@code o8}
     * @return {@code Some} of the tuple of the values, or {@code None} if any argument is {@code None}
     * @throws NullPointerException if any argument is null
     */
    static <T1 extends @Nullable Object, T2 extends @Nullable Object, T3 extends @Nullable Object, T4 extends @Nullable Object, T5 extends @Nullable Object, T6 extends @Nullable Object, T7 extends @Nullable Object, T8 extends @Nullable Object> Option<Tuple8<T1, T2, T3, T4, T5, T6, T7, T8>> zip(Option<? extends T1> o1, Option<? extends T2> o2, Option<? extends T3> o3, Option<? extends T4> o4, Option<? extends T5> o5, Option<? extends T6> o6, Option<? extends T7> o7, Option<? extends T8> o8) {
        return zipWith(o1, o2, o3, o4, o5, o6, o7, o8, Tuple::of);
    }

    /**
     * Combines the values of eight {@code Option}s through {@code f}, failing fast: {@code Some} of the result when
     * every argument is a {@code Some}, otherwise {@code None}. {@code f} is called only when every argument is a
     * {@code Some}, with the values in argument order; it must not return {@code null}, since {@code Some} cannot hold
     * {@code null} (design 3.9).
     *
     * @param o1  the first {@code Option}
     * @param o2  the second {@code Option}
     * @param o3  the third {@code Option}
     * @param o4  the fourth {@code Option}
     * @param o5  the fifth {@code Option}
     * @param o6  the sixth {@code Option}
     * @param o7  the seventh {@code Option}
     * @param o8  the eighth {@code Option}
     * @param f  combines the values; it must not return {@code null}
     * @param <T1> the value type of {@code o1}
     * @param <T2> the value type of {@code o2}
     * @param <T3> the value type of {@code o3}
     * @param <T4> the value type of {@code o4}
     * @param <T5> the value type of {@code o5}
     * @param <T6> the value type of {@code o6}
     * @param <T7> the value type of {@code o7}
     * @param <T8> the value type of {@code o8}
     * @param <R>  the result type
     * @return {@code Some} of the combined value, or {@code None} if any argument is {@code None}
     * @throws NullPointerException if any argument is null, or if {@code f} returns null
     */
    static <T1 extends @Nullable Object, T2 extends @Nullable Object, T3 extends @Nullable Object, T4 extends @Nullable Object, T5 extends @Nullable Object, T6 extends @Nullable Object, T7 extends @Nullable Object, T8 extends @Nullable Object, R extends @Nullable Object> Option<R> zipWith(Option<? extends T1> o1, Option<? extends T2> o2, Option<? extends T3> o3, Option<? extends T4> o4, Option<? extends T5> o5, Option<? extends T6> o6, Option<? extends T7> o7, Option<? extends T8> o8, Function8<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? super T6, ? super T7, ? super T8, ? extends R> f) {
        Objects.requireNonNull(o1, "o1 is null");
        Objects.requireNonNull(o2, "o2 is null");
        Objects.requireNonNull(o3, "o3 is null");
        Objects.requireNonNull(o4, "o4 is null");
        Objects.requireNonNull(o5, "o5 is null");
        Objects.requireNonNull(o6, "o6 is null");
        Objects.requireNonNull(o7, "o7 is null");
        Objects.requireNonNull(o8, "o8 is null");
        Objects.requireNonNull(f, "f is null");
        if (o1.isEmpty() || o2.isEmpty() || o3.isEmpty() || o4.isEmpty() || o5.isEmpty() || o6.isEmpty() || o7.isEmpty() || o8.isEmpty()) {
            return none();
        }
        return some(Objects.requireNonNull(f.apply(o1.get(), o2.get(), o3.get(), o4.get(), o5.get(), o6.get(), o7.get(), o8.get()), "Option.zipWith: f returned null"));
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
