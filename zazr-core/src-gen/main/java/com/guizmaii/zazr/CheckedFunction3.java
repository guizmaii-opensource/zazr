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
 * Represents a function with three arguments.
 *
 * @param <T1> argument 1 of the function
 * @param <T2> argument 2 of the function
 * @param <T3> argument 3 of the function
 * @param <R> return type of the function
 * @author Daniel Dietrich
 */
@FunctionalInterface
public interface CheckedFunction3<T1 extends @Nullable Object, T2 extends @Nullable Object, T3 extends @Nullable Object, R extends @Nullable Object>  {

    /**
     * Returns a function that always returns the constant
     * value that you give in parameter.
     *
     * @param <T1> generic parameter type 1 of the resulting function
     * @param <T2> generic parameter type 2 of the resulting function
     * @param <T3> generic parameter type 3 of the resulting function
     * @param <R> the result type
     * @param value the value to be returned
     * @return a function always returning the given value
     */
    static <T1 extends @Nullable Object, T2 extends @Nullable Object, T3 extends @Nullable Object, R extends @Nullable Object> CheckedFunction3<T1, T2, T3, R> constant(R value) {
        return (t1, t2, t3) -> value;
    }

    /**
     * Creates a {@code CheckedFunction3} based on
     * <ul>
     * <li><a href="https://docs.oracle.com/javase/tutorial/java/javaOO/methodreferences.html">method reference</a></li>
     * <li><a href="https://docs.oracle.com/javase/tutorial/java/javaOO/lambdaexpressions.html#syntax">lambda expression</a></li>
     * </ul>
     *
     * Examples (w.l.o.g. referring to CheckedFunction3):
     * <pre>{@code // using a lambda expression
     * CheckedFunction3<T1, T2, T3, R> add1 = CheckedFunction3.of((t1, t2, t3) -> t1 + t2 + t3);
     *
     * // using a method reference
     * CheckedFunction3<T1, T2, T3, R> add2 = CheckedFunction3.of(this::method);
     *
     * // using a lambda reference
     * CheckedFunction3<T1, T2, T3, R> add3 = CheckedFunction3.of(add1::apply);
     * }</pre>
     *
     * @param methodReference (typically) a method reference, e.g. {@code Type::method}
     * @param <R> return type
     * @param <T1> 1st argument
     * @param <T2> 2nd argument
     * @param <T3> 3rd argument
     * @return a {@code CheckedFunction3}
     */
    static <T1 extends @Nullable Object, T2 extends @Nullable Object, T3 extends @Nullable Object, R extends @Nullable Object> CheckedFunction3<T1, T2, T3, R> of(CheckedFunction3<T1, T2, T3, R> methodReference) {
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
     * @return a function that applies arguments to the given {@code partialFunction} and returns {@code Some(result)}
     *         if the function is defined for the given arguments, and {@code None} if it throws a non-fatal
     *         throwable. Fatal throwables (see {@link Try}) are rethrown
     *         instead of being turned into {@code None}.
     */
    static <T1 extends @Nullable Object, T2 extends @Nullable Object, T3 extends @Nullable Object, R extends @Nullable Object> Function3<T1, T2, T3, Option<R>> lift(CheckedFunction3<? super T1, ? super T2, ? super T3, ? extends R> partialFunction) {
        return (t1, t2, t3) -> {
            try {
                final R result = partialFunction.apply(t1, t2, t3);
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
     * @return a function that applies arguments to the given {@code partialFunction} and returns {@code Success(result)}
     *         if the function is defined for the given arguments, and {@code Failure(throwable)} if it throws a
     *         non-fatal throwable. Fatal throwables (see {@link Try}) are rethrown
     *         instead of being wrapped.
     */
    static <T1 extends @Nullable Object, T2 extends @Nullable Object, T3 extends @Nullable Object, R extends @Nullable Object> Function3<T1, T2, T3, Try<R>> liftTry(CheckedFunction3<? super T1, ? super T2, ? super T3, ? extends R> partialFunction) {
        return (t1, t2, t3) -> Try.of(() -> partialFunction.apply(t1, t2, t3));
    }

    /**
     * Narrows the given {@code CheckedFunction3<? super T1, ? super T2, ? super T3, ? extends R>} to {@code CheckedFunction3<T1, T2, T3, R>}
     *
     * @param f A {@code CheckedFunction3}
     * @param <R> return type
     * @param <T1> 1st argument
     * @param <T2> 2nd argument
     * @param <T3> 3rd argument
     * @return the given {@code f} instance as narrowed type {@code CheckedFunction3<T1, T2, T3, R>}
     */
    @SuppressWarnings("unchecked")
    static <T1 extends @Nullable Object, T2 extends @Nullable Object, T3 extends @Nullable Object, R extends @Nullable Object> CheckedFunction3<T1, T2, T3, R> narrow(CheckedFunction3<? super T1, ? super T2, ? super T3, ? extends R> f) {
        return (CheckedFunction3<T1, T2, T3, R>) f;
    }

    /**
     * Applies this function to three arguments and returns the result.
     *
     * @param t1 argument 1
     * @param t2 argument 2
     * @param t3 argument 3
     * @return the result of function application
     * @throws Exception if something goes wrong applying this function to the given arguments
     */
    R apply(T1 t1, T2 t2, T3 t3) throws Exception;

    /**
     * Applies this function partially to one argument.
     *
     * @param t1 argument 1
     * @return a partial application of this function
     */
    default CheckedFunction2<T2, T3, R> apply(T1 t1) {
        return (T2 t2, T3 t3) -> apply(t1, t2, t3);
    }

    /**
     * Applies this function partially to two arguments.
     *
     * @param t1 argument 1
     * @param t2 argument 2
     * @return a partial application of this function
     */
    default CheckedFunction1<T3, R> apply(T1 t1, T2 t2) {
        return (T3 t3) -> apply(t1, t2, t3);
    }

    /**
     * Returns a curried version of this function.
     *
     * @return a curried function equivalent to this.
     */
    default Function<T1, Function<T2, CheckedFunction1<T3, R>>> curried() {
        return t1 -> t2 -> t3 -> apply(t1, t2, t3);
    }

    /**
     * Returns a tupled version of this function.
     *
     * @return a tupled function equivalent to this.
     */
    default CheckedFunction1<Tuple3<T1, T2, T3>, R> tupled() {
        return t -> apply(t._1(), t._2(), t._3());
    }

    /**
     * Return a composed function that first applies this CheckedFunction3 to the given arguments and in case of a
     * non-fatal throwable tries to get a value from the {@code recover} function with the throwable information.
     * A fatal throwable (see {@link Try}) is never handed to
     * {@code recover}: it propagates unchanged instead.
     *
     * @param recover the function applied in case of a non-fatal throwable
     * @return a function composed of this and recover
     * @throws NullPointerException if recover is null
     */
    default Function3<T1, T2, T3, R> recover(Function<? super Throwable, ? extends Function3<? super T1, ? super T2, ? super T3, ? extends R>> recover) {
        Objects.requireNonNull(recover, "recover is null");
        return (t1, t2, t3) -> {
            try {
                return this.apply(t1, t2, t3);
            } catch (Throwable throwable) {
                if (isFatal(throwable)) {
                    return sneakyThrow(throwable);
                }
                final Function3<? super T1, ? super T2, ? super T3, ? extends R> func = recover.apply(throwable);
                Objects.requireNonNull(func, () -> "recover return null for " + throwable.getClass() + ": " + throwable.getMessage());
                return func.apply(t1, t2, t3);
            }
        };
    }

    /**
     * Returns an unchecked function that will <em>sneaky throw</em> if an exceptions occurs when applying the function.
     *
     * @return a new unchecked function that throws a {@code Throwable}.
     */
    default Function3<T1, T2, T3, R> unchecked() {
        return (t1, t2, t3) -> {
            try {
                return apply(t1, t2, t3);
            } catch(Throwable t) {
                return sneakyThrow(t);
            }
        };
    }

    /**
     * Returns a composed function that first applies this CheckedFunction3 to the given arguments and then applies
     * {@linkplain CheckedFunction1} {@code after} to the result.
     *
     * @param <V> return type of after
     * @param after the function applied after this
     * @return a function composed of this and after
     * @throws NullPointerException if after is null
     */
    default <V extends @Nullable Object> CheckedFunction3<T1, T2, T3, V> andThen(CheckedFunction1<? super R, ? extends V> after) {
        Objects.requireNonNull(after, "after is null");
        return (t1, t2, t3) -> after.apply(apply(t1, t2, t3));
    }

    /**
     * Returns a composed function that first applies the {@linkplain Function} {@code before} to the
     * 1st argument and then applies this CheckedFunction3 to the result and the other arguments.
     *
     * @param <S> argument type of before
     * @param before the function applied before this
     * @return a function composed of before and this
     * @throws NullPointerException if before is null
     */
    default <S extends @Nullable Object> CheckedFunction3<S, T2, T3, R> compose1(Function<? super S, ? extends T1> before) {
        Objects.requireNonNull(before, "before is null");
        return (S s, T2 t2, T3 t3) -> apply(before.apply(s), t2, t3);
    }

    /**
     * Returns a composed function that first applies the {@linkplain Function} {@code before} to the
     * 2nd argument and then applies this CheckedFunction3 to the result and the other arguments.
     *
     * @param <S> argument type of before
     * @param before the function applied before this
     * @return a function composed of before and this
     * @throws NullPointerException if before is null
     */
    default <S extends @Nullable Object> CheckedFunction3<T1, S, T3, R> compose2(Function<? super S, ? extends T2> before) {
        Objects.requireNonNull(before, "before is null");
        return (T1 t1, S s, T3 t3) -> apply(t1, before.apply(s), t3);
    }

    /**
     * Returns a composed function that first applies the {@linkplain Function} {@code before} to the
     * 3rd argument and then applies this CheckedFunction3 to the result and the other arguments.
     *
     * @param <S> argument type of before
     * @param before the function applied before this
     * @return a function composed of before and this
     * @throws NullPointerException if before is null
     */
    default <S extends @Nullable Object> CheckedFunction3<T1, T2, S, R> compose3(Function<? super S, ? extends T3> before) {
        Objects.requireNonNull(before, "before is null");
        return (T1 t1, T2 t2, S s) -> apply(t1, t2, before.apply(s));
    }
}