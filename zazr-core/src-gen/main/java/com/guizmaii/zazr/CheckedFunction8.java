package com.guizmaii.zazr;

/*-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-*\
   G E N E R A T O R   C R A F T E D
\*-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-*/

import static com.guizmaii.zazr.Throwables.isFatal;
import static com.guizmaii.zazr.Throwables.sneakyThrow;

import com.guizmaii.zazr.control.Option;
import com.guizmaii.zazr.control.Try;
import java.util.Objects;
import java.util.function.Function;
import org.jspecify.annotations.Nullable;

/**
 * Represents a function with 8 arguments.
 *
 * @param <T1> argument 1 of the function
 * @param <T2> argument 2 of the function
 * @param <T3> argument 3 of the function
 * @param <T4> argument 4 of the function
 * @param <T5> argument 5 of the function
 * @param <T6> argument 6 of the function
 * @param <T7> argument 7 of the function
 * @param <T8> argument 8 of the function
 * @param <R> return type of the function
 * @author Daniel Dietrich
 */
@FunctionalInterface
public interface CheckedFunction8<T1 extends @Nullable Object, T2 extends @Nullable Object, T3 extends @Nullable Object, T4 extends @Nullable Object, T5 extends @Nullable Object, T6 extends @Nullable Object, T7 extends @Nullable Object, T8 extends @Nullable Object, R extends @Nullable Object>  {

    /**
     * Returns a function that always returns the constant
     * value that you give in parameter.
     *
     * @param <T1> generic parameter type 1 of the resulting function
     * @param <T2> generic parameter type 2 of the resulting function
     * @param <T3> generic parameter type 3 of the resulting function
     * @param <T4> generic parameter type 4 of the resulting function
     * @param <T5> generic parameter type 5 of the resulting function
     * @param <T6> generic parameter type 6 of the resulting function
     * @param <T7> generic parameter type 7 of the resulting function
     * @param <T8> generic parameter type 8 of the resulting function
     * @param <R> the result type
     * @param value the value to be returned
     * @return a function always returning the given value
     */
    static <T1 extends @Nullable Object, T2 extends @Nullable Object, T3 extends @Nullable Object, T4 extends @Nullable Object, T5 extends @Nullable Object, T6 extends @Nullable Object, T7 extends @Nullable Object, T8 extends @Nullable Object, R extends @Nullable Object> CheckedFunction8<T1, T2, T3, T4, T5, T6, T7, T8, R> constant(R value) {
        return (t1, t2, t3, t4, t5, t6, t7, t8) -> value;
    }

    /**
     * Creates a {@code CheckedFunction8} based on
     * <ul>
     * <li><a href="https://docs.oracle.com/javase/tutorial/java/javaOO/methodreferences.html">method reference</a></li>
     * <li><a href="https://docs.oracle.com/javase/tutorial/java/javaOO/lambdaexpressions.html#syntax">lambda expression</a></li>
     * </ul>
     *
     * Examples (w.l.o.g. referring to CheckedFunction8):
     * <pre>{@code // using a lambda expression
     * CheckedFunction8<T1, T2, T3, T4, T5, T6, T7, T8, R> add1 = CheckedFunction8.of((t1, t2, t3, t4, t5, t6, t7, t8) -> t1 + t2 + t3 + t4 + t5 + t6 + t7 + t8);
     *
     * // using a method reference
     * CheckedFunction8<T1, T2, T3, T4, T5, T6, T7, T8, R> add2 = CheckedFunction8.of(this::method);
     *
     * // using a lambda reference
     * CheckedFunction8<T1, T2, T3, T4, T5, T6, T7, T8, R> add3 = CheckedFunction8.of(add1::apply);
     * }</pre>
     *
     * @param methodReference (typically) a method reference, e.g. {@code Type::method}
     * @param <R> return type
     * @param <T1> 1st argument
     * @param <T2> 2nd argument
     * @param <T3> 3rd argument
     * @param <T4> 4th argument
     * @param <T5> 5th argument
     * @param <T6> 6th argument
     * @param <T7> 7th argument
     * @param <T8> 8th argument
     * @return a {@code CheckedFunction8}
     */
    static <T1 extends @Nullable Object, T2 extends @Nullable Object, T3 extends @Nullable Object, T4 extends @Nullable Object, T5 extends @Nullable Object, T6 extends @Nullable Object, T7 extends @Nullable Object, T8 extends @Nullable Object, R extends @Nullable Object> CheckedFunction8<T1, T2, T3, T4, T5, T6, T7, T8, R> of(CheckedFunction8<T1, T2, T3, T4, T5, T6, T7, T8, R> methodReference) {
        return methodReference;
    }

    /**
     * Lifts the given {@code partialFunction} into a function that returns an {@code Option} result.
     *
     * @param partialFunction a function that is not defined for all values of the domain (e.g. by throwing)
     * @param <R> return type
     * @param <T1> 1st argument
     * @param <T2> 2nd argument
     * @param <T3> 3rd argument
     * @param <T4> 4th argument
     * @param <T5> 5th argument
     * @param <T6> 6th argument
     * @param <T7> 7th argument
     * @param <T8> 8th argument
     * @return a function that applies arguments to the given {@code partialFunction} and returns {@code Some(result)}
     *         if the function is defined for the given arguments, and {@code None} if it throws a non-fatal
     *         throwable. Fatal throwables (see {@link Try}) are rethrown
     *         instead of being turned into {@code None}.
     */
    static <T1 extends @Nullable Object, T2 extends @Nullable Object, T3 extends @Nullable Object, T4 extends @Nullable Object, T5 extends @Nullable Object, T6 extends @Nullable Object, T7 extends @Nullable Object, T8 extends @Nullable Object, R extends @Nullable Object> Function8<T1, T2, T3, T4, T5, T6, T7, T8, Option<R>> lift(CheckedFunction8<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? super T6, ? super T7, ? super T8, ? extends R> partialFunction) {
        return (t1, t2, t3, t4, t5, t6, t7, t8) -> {
            try {
                final R result = partialFunction.apply(t1, t2, t3, t4, t5, t6, t7, t8);
                return result == null ? Option.<R>none() : Option.some(result);
            } catch (Throwable t) {
                if (isFatal(t)) {
                    return sneakyThrow(t);
                }
                return Option.<R>none();
            }
        };
    }

    /**
     * Lifts the given {@code partialFunction} into a function that returns a {@code Try} result.
     *
     * @param partialFunction a function that is not defined for all values of the domain (e.g. by throwing)
     * @param <R> return type
     * @param <T1> 1st argument
     * @param <T2> 2nd argument
     * @param <T3> 3rd argument
     * @param <T4> 4th argument
     * @param <T5> 5th argument
     * @param <T6> 6th argument
     * @param <T7> 7th argument
     * @param <T8> 8th argument
     * @return a function that applies arguments to the given {@code partialFunction} and returns {@code Success(result)}
     *         if the function is defined for the given arguments, and {@code Failure(throwable)} if it throws a
     *         non-fatal throwable. Fatal throwables (see {@link Try}) are rethrown
     *         instead of being wrapped.
     */
    static <T1 extends @Nullable Object, T2 extends @Nullable Object, T3 extends @Nullable Object, T4 extends @Nullable Object, T5 extends @Nullable Object, T6 extends @Nullable Object, T7 extends @Nullable Object, T8 extends @Nullable Object, R extends @Nullable Object> Function8<T1, T2, T3, T4, T5, T6, T7, T8, Try<R>> liftTry(CheckedFunction8<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? super T6, ? super T7, ? super T8, ? extends R> partialFunction) {
        return (t1, t2, t3, t4, t5, t6, t7, t8) -> Try.of(() -> partialFunction.apply(t1, t2, t3, t4, t5, t6, t7, t8));
    }

    /**
     * Narrows the given {@code CheckedFunction8<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? super T6, ? super T7, ? super T8, ? extends R>} to {@code CheckedFunction8<T1, T2, T3, T4, T5, T6, T7, T8, R>}
     *
     * @param f A {@code CheckedFunction8}
     * @param <R> return type
     * @param <T1> 1st argument
     * @param <T2> 2nd argument
     * @param <T3> 3rd argument
     * @param <T4> 4th argument
     * @param <T5> 5th argument
     * @param <T6> 6th argument
     * @param <T7> 7th argument
     * @param <T8> 8th argument
     * @return the given {@code f} instance as narrowed type {@code CheckedFunction8<T1, T2, T3, T4, T5, T6, T7, T8, R>}
     */
    @SuppressWarnings("unchecked")
    static <T1 extends @Nullable Object, T2 extends @Nullable Object, T3 extends @Nullable Object, T4 extends @Nullable Object, T5 extends @Nullable Object, T6 extends @Nullable Object, T7 extends @Nullable Object, T8 extends @Nullable Object, R extends @Nullable Object> CheckedFunction8<T1, T2, T3, T4, T5, T6, T7, T8, R> narrow(CheckedFunction8<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? super T6, ? super T7, ? super T8, ? extends R> f) {
        return (CheckedFunction8<T1, T2, T3, T4, T5, T6, T7, T8, R>) f;
    }

    /**
     * Applies this function to 8 arguments and returns the result.
     *
     * @param t1 argument 1
     * @param t2 argument 2
     * @param t3 argument 3
     * @param t4 argument 4
     * @param t5 argument 5
     * @param t6 argument 6
     * @param t7 argument 7
     * @param t8 argument 8
     * @return the result of function application
     * @throws Exception if something goes wrong applying this function to the given arguments
     */
    R apply(T1 t1, T2 t2, T3 t3, T4 t4, T5 t5, T6 t6, T7 t7, T8 t8) throws Exception;

    /**
     * Applies this function partially to one argument.
     *
     * @param t1 argument 1
     * @return a partial application of this function
     */
    default CheckedFunction7<T2, T3, T4, T5, T6, T7, T8, R> apply(T1 t1) {
        return (T2 t2, T3 t3, T4 t4, T5 t5, T6 t6, T7 t7, T8 t8) -> apply(t1, t2, t3, t4, t5, t6, t7, t8);
    }

    /**
     * Applies this function partially to two arguments.
     *
     * @param t1 argument 1
     * @param t2 argument 2
     * @return a partial application of this function
     */
    default CheckedFunction6<T3, T4, T5, T6, T7, T8, R> apply(T1 t1, T2 t2) {
        return (T3 t3, T4 t4, T5 t5, T6 t6, T7 t7, T8 t8) -> apply(t1, t2, t3, t4, t5, t6, t7, t8);
    }

    /**
     * Applies this function partially to three arguments.
     *
     * @param t1 argument 1
     * @param t2 argument 2
     * @param t3 argument 3
     * @return a partial application of this function
     */
    default CheckedFunction5<T4, T5, T6, T7, T8, R> apply(T1 t1, T2 t2, T3 t3) {
        return (T4 t4, T5 t5, T6 t6, T7 t7, T8 t8) -> apply(t1, t2, t3, t4, t5, t6, t7, t8);
    }

    /**
     * Applies this function partially to 4 arguments.
     *
     * @param t1 argument 1
     * @param t2 argument 2
     * @param t3 argument 3
     * @param t4 argument 4
     * @return a partial application of this function
     */
    default CheckedFunction4<T5, T6, T7, T8, R> apply(T1 t1, T2 t2, T3 t3, T4 t4) {
        return (T5 t5, T6 t6, T7 t7, T8 t8) -> apply(t1, t2, t3, t4, t5, t6, t7, t8);
    }

    /**
     * Applies this function partially to 5 arguments.
     *
     * @param t1 argument 1
     * @param t2 argument 2
     * @param t3 argument 3
     * @param t4 argument 4
     * @param t5 argument 5
     * @return a partial application of this function
     */
    default CheckedFunction3<T6, T7, T8, R> apply(T1 t1, T2 t2, T3 t3, T4 t4, T5 t5) {
        return (T6 t6, T7 t7, T8 t8) -> apply(t1, t2, t3, t4, t5, t6, t7, t8);
    }

    /**
     * Applies this function partially to 6 arguments.
     *
     * @param t1 argument 1
     * @param t2 argument 2
     * @param t3 argument 3
     * @param t4 argument 4
     * @param t5 argument 5
     * @param t6 argument 6
     * @return a partial application of this function
     */
    default CheckedFunction2<T7, T8, R> apply(T1 t1, T2 t2, T3 t3, T4 t4, T5 t5, T6 t6) {
        return (T7 t7, T8 t8) -> apply(t1, t2, t3, t4, t5, t6, t7, t8);
    }

    /**
     * Applies this function partially to 7 arguments.
     *
     * @param t1 argument 1
     * @param t2 argument 2
     * @param t3 argument 3
     * @param t4 argument 4
     * @param t5 argument 5
     * @param t6 argument 6
     * @param t7 argument 7
     * @return a partial application of this function
     */
    default CheckedFunction1<T8, R> apply(T1 t1, T2 t2, T3 t3, T4 t4, T5 t5, T6 t6, T7 t7) {
        return (T8 t8) -> apply(t1, t2, t3, t4, t5, t6, t7, t8);
    }

    /**
     * Returns a curried version of this function.
     *
     * @return a curried function equivalent to this.
     */
    default Function<T1, Function<T2, Function<T3, Function<T4, Function<T5, Function<T6, Function<T7, CheckedFunction1<T8, R>>>>>>>> curried() {
        return t1 -> t2 -> t3 -> t4 -> t5 -> t6 -> t7 -> t8 -> apply(t1, t2, t3, t4, t5, t6, t7, t8);
    }

    /**
     * Returns a tupled version of this function.
     *
     * @return a tupled function equivalent to this.
     */
    default CheckedFunction1<Tuple8<T1, T2, T3, T4, T5, T6, T7, T8>, R> tupled() {
        return t -> apply(t._1(), t._2(), t._3(), t._4(), t._5(), t._6(), t._7(), t._8());
    }

    /**
     * Return a composed function that first applies this CheckedFunction8 to the given arguments and in case of a
     * non-fatal throwable tries to get a value from the {@code recover} function with the throwable information.
     * A fatal throwable (see {@link Try}) is never handed to
     * {@code recover}: it propagates unchanged instead.
     *
     * @param recover the function applied in case of a non-fatal throwable
     * @return a function composed of this and recover
     * @throws NullPointerException if recover is null
     */
    default Function8<T1, T2, T3, T4, T5, T6, T7, T8, R> recover(Function<? super Throwable, ? extends Function8<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? super T6, ? super T7, ? super T8, ? extends R>> recover) {
        Objects.requireNonNull(recover, "recover is null");
        return (t1, t2, t3, t4, t5, t6, t7, t8) -> {
            try {
                return this.apply(t1, t2, t3, t4, t5, t6, t7, t8);
            } catch (Throwable throwable) {
                if (isFatal(throwable)) {
                    return sneakyThrow(throwable);
                }
                final Function8<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? super T6, ? super T7, ? super T8, ? extends R> func = recover.apply(throwable);
                Objects.requireNonNull(func, () -> "recover return null for " + throwable.getClass() + ": " + throwable.getMessage());
                return func.apply(t1, t2, t3, t4, t5, t6, t7, t8);
            }
        };
    }

    /**
     * Returns an unchecked function that will <em>sneaky throw</em> if an exceptions occurs when applying the function.
     *
     * @return a new unchecked function that throws a {@code Throwable}.
     */
    default Function8<T1, T2, T3, T4, T5, T6, T7, T8, R> unchecked() {
        return (t1, t2, t3, t4, t5, t6, t7, t8) -> {
            try {
                return apply(t1, t2, t3, t4, t5, t6, t7, t8);
            } catch(Throwable t) {
                return sneakyThrow(t);
            }
        };
    }

    /**
     * Returns a composed function that first applies this CheckedFunction8 to the given arguments and then applies
     * {@linkplain CheckedFunction1} {@code after} to the result.
     *
     * @param <V> return type of after
     * @param after the function applied after this
     * @return a function composed of this and after
     * @throws NullPointerException if after is null
     */
    default <V extends @Nullable Object> CheckedFunction8<T1, T2, T3, T4, T5, T6, T7, T8, V> andThen(CheckedFunction1<? super R, ? extends V> after) {
        Objects.requireNonNull(after, "after is null");
        return (t1, t2, t3, t4, t5, t6, t7, t8) -> after.apply(apply(t1, t2, t3, t4, t5, t6, t7, t8));
    }

    /**
     * Returns a composed function that first applies the {@linkplain Function} {@code before} to the
     * 1st argument and then applies this CheckedFunction8 to the result and the other arguments.
     *
     * @param <S> argument type of before
     * @param before the function applied before this
     * @return a function composed of before and this
     * @throws NullPointerException if before is null
     */
    default <S extends @Nullable Object> CheckedFunction8<S, T2, T3, T4, T5, T6, T7, T8, R> compose1(Function<? super S, ? extends T1> before) {
        Objects.requireNonNull(before, "before is null");
        return (S s, T2 t2, T3 t3, T4 t4, T5 t5, T6 t6, T7 t7, T8 t8) -> apply(before.apply(s), t2, t3, t4, t5, t6, t7, t8);
    }

    /**
     * Returns a composed function that first applies the {@linkplain Function} {@code before} to the
     * 2nd argument and then applies this CheckedFunction8 to the result and the other arguments.
     *
     * @param <S> argument type of before
     * @param before the function applied before this
     * @return a function composed of before and this
     * @throws NullPointerException if before is null
     */
    default <S extends @Nullable Object> CheckedFunction8<T1, S, T3, T4, T5, T6, T7, T8, R> compose2(Function<? super S, ? extends T2> before) {
        Objects.requireNonNull(before, "before is null");
        return (T1 t1, S s, T3 t3, T4 t4, T5 t5, T6 t6, T7 t7, T8 t8) -> apply(t1, before.apply(s), t3, t4, t5, t6, t7, t8);
    }

    /**
     * Returns a composed function that first applies the {@linkplain Function} {@code before} to the
     * 3rd argument and then applies this CheckedFunction8 to the result and the other arguments.
     *
     * @param <S> argument type of before
     * @param before the function applied before this
     * @return a function composed of before and this
     * @throws NullPointerException if before is null
     */
    default <S extends @Nullable Object> CheckedFunction8<T1, T2, S, T4, T5, T6, T7, T8, R> compose3(Function<? super S, ? extends T3> before) {
        Objects.requireNonNull(before, "before is null");
        return (T1 t1, T2 t2, S s, T4 t4, T5 t5, T6 t6, T7 t7, T8 t8) -> apply(t1, t2, before.apply(s), t4, t5, t6, t7, t8);
    }

    /**
     * Returns a composed function that first applies the {@linkplain Function} {@code before} to the
     * 4th argument and then applies this CheckedFunction8 to the result and the other arguments.
     *
     * @param <S> argument type of before
     * @param before the function applied before this
     * @return a function composed of before and this
     * @throws NullPointerException if before is null
     */
    default <S extends @Nullable Object> CheckedFunction8<T1, T2, T3, S, T5, T6, T7, T8, R> compose4(Function<? super S, ? extends T4> before) {
        Objects.requireNonNull(before, "before is null");
        return (T1 t1, T2 t2, T3 t3, S s, T5 t5, T6 t6, T7 t7, T8 t8) -> apply(t1, t2, t3, before.apply(s), t5, t6, t7, t8);
    }

    /**
     * Returns a composed function that first applies the {@linkplain Function} {@code before} to the
     * 5th argument and then applies this CheckedFunction8 to the result and the other arguments.
     *
     * @param <S> argument type of before
     * @param before the function applied before this
     * @return a function composed of before and this
     * @throws NullPointerException if before is null
     */
    default <S extends @Nullable Object> CheckedFunction8<T1, T2, T3, T4, S, T6, T7, T8, R> compose5(Function<? super S, ? extends T5> before) {
        Objects.requireNonNull(before, "before is null");
        return (T1 t1, T2 t2, T3 t3, T4 t4, S s, T6 t6, T7 t7, T8 t8) -> apply(t1, t2, t3, t4, before.apply(s), t6, t7, t8);
    }

    /**
     * Returns a composed function that first applies the {@linkplain Function} {@code before} to the
     * 6th argument and then applies this CheckedFunction8 to the result and the other arguments.
     *
     * @param <S> argument type of before
     * @param before the function applied before this
     * @return a function composed of before and this
     * @throws NullPointerException if before is null
     */
    default <S extends @Nullable Object> CheckedFunction8<T1, T2, T3, T4, T5, S, T7, T8, R> compose6(Function<? super S, ? extends T6> before) {
        Objects.requireNonNull(before, "before is null");
        return (T1 t1, T2 t2, T3 t3, T4 t4, T5 t5, S s, T7 t7, T8 t8) -> apply(t1, t2, t3, t4, t5, before.apply(s), t7, t8);
    }

    /**
     * Returns a composed function that first applies the {@linkplain Function} {@code before} to the
     * 7th argument and then applies this CheckedFunction8 to the result and the other arguments.
     *
     * @param <S> argument type of before
     * @param before the function applied before this
     * @return a function composed of before and this
     * @throws NullPointerException if before is null
     */
    default <S extends @Nullable Object> CheckedFunction8<T1, T2, T3, T4, T5, T6, S, T8, R> compose7(Function<? super S, ? extends T7> before) {
        Objects.requireNonNull(before, "before is null");
        return (T1 t1, T2 t2, T3 t3, T4 t4, T5 t5, T6 t6, S s, T8 t8) -> apply(t1, t2, t3, t4, t5, t6, before.apply(s), t8);
    }

    /**
     * Returns a composed function that first applies the {@linkplain Function} {@code before} to the
     * 8th argument and then applies this CheckedFunction8 to the result and the other arguments.
     *
     * @param <S> argument type of before
     * @param before the function applied before this
     * @return a function composed of before and this
     * @throws NullPointerException if before is null
     */
    default <S extends @Nullable Object> CheckedFunction8<T1, T2, T3, T4, T5, T6, T7, S, R> compose8(Function<? super S, ? extends T8> before) {
        Objects.requireNonNull(before, "before is null");
        return (T1 t1, T2 t2, T3 t3, T4 t4, T5 t5, T6 t6, T7 t7, S s) -> apply(t1, t2, t3, t4, t5, t6, t7, before.apply(s));
    }
}