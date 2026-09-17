package com.guizmaii.zazr;

/*-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-*\
   G E N E R A T O R   C R A F T E D
\*-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-*/

import com.guizmaii.zazr.control.Option;
import com.guizmaii.zazr.control.Try;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
import org.jspecify.annotations.Nullable;

/**
 * Represents a function with 6 arguments.
 *
 * @param <T1> argument 1 of the function
 * @param <T2> argument 2 of the function
 * @param <T3> argument 3 of the function
 * @param <T4> argument 4 of the function
 * @param <T5> argument 5 of the function
 * @param <T6> argument 6 of the function
 * @param <R> return type of the function
 * @author Daniel Dietrich
 */
@FunctionalInterface
public interface Function6<T1 extends @Nullable Object, T2 extends @Nullable Object, T3 extends @Nullable Object, T4 extends @Nullable Object, T5 extends @Nullable Object, T6 extends @Nullable Object, R extends @Nullable Object>  {

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
     * @param <R> the result type
     * @param value the value to be returned
     * @return a function always returning the given value
     */
    static <T1 extends @Nullable Object, T2 extends @Nullable Object, T3 extends @Nullable Object, T4 extends @Nullable Object, T5 extends @Nullable Object, T6 extends @Nullable Object, R extends @Nullable Object> Function6<T1, T2, T3, T4, T5, T6, R> constant(R value) {
        return (t1, t2, t3, t4, t5, t6) -> value;
    }

    /**
     * Creates a {@code Function6} based on
     * <ul>
     * <li><a href="https://docs.oracle.com/javase/tutorial/java/javaOO/methodreferences.html">method reference</a></li>
     * <li><a href="https://docs.oracle.com/javase/tutorial/java/javaOO/lambdaexpressions.html#syntax">lambda expression</a></li>
     * </ul>
     *
     * Examples (w.l.o.g. referring to Function6):
     * <pre>{@code // using a lambda expression
     * Function6<T1, T2, T3, T4, T5, T6, R> add1 = Function6.of((t1, t2, t3, t4, t5, t6) -> t1 + t2 + t3 + t4 + t5 + t6);
     *
     * // using a method reference
     * Function6<T1, T2, T3, T4, T5, T6, R> add2 = Function6.of(this::method);
     *
     * // using a lambda reference
     * Function6<T1, T2, T3, T4, T5, T6, R> add3 = Function6.of(add1::apply);
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
     * @return a {@code Function6}
     */
    static <T1 extends @Nullable Object, T2 extends @Nullable Object, T3 extends @Nullable Object, T4 extends @Nullable Object, T5 extends @Nullable Object, T6 extends @Nullable Object, R extends @Nullable Object> Function6<T1, T2, T3, T4, T5, T6, R> of(Function6<T1, T2, T3, T4, T5, T6, R> methodReference) {
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
     * @return a function that applies arguments to the given {@code partialFunction} and returns {@code Some(result)}
     *         if the function is defined for the given arguments, and {@code None} if it throws a non-fatal
     *         throwable. Fatal throwables (see {@link Try}) are rethrown
     *         instead of being turned into {@code None}.
     */
    static <T1 extends @Nullable Object, T2 extends @Nullable Object, T3 extends @Nullable Object, T4 extends @Nullable Object, T5 extends @Nullable Object, T6 extends @Nullable Object, R extends @Nullable Object> Function6<T1, T2, T3, T4, T5, T6, Option<R>> lift(Function6<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? super T6, ? extends R> partialFunction) {
        return (t1, t2, t3, t4, t5, t6) -> Try.<R>of(() -> partialFunction.apply(t1, t2, t3, t4, t5, t6)).toOption();
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
     * @return a function that applies arguments to the given {@code partialFunction} and returns {@code Success(result)}
     *         if the function is defined for the given arguments, and {@code Failure(throwable)} if it throws a
     *         non-fatal throwable. Fatal throwables (see {@link Try}) are rethrown
     *         instead of being wrapped.
     */
    static <T1 extends @Nullable Object, T2 extends @Nullable Object, T3 extends @Nullable Object, T4 extends @Nullable Object, T5 extends @Nullable Object, T6 extends @Nullable Object, R extends @Nullable Object> Function6<T1, T2, T3, T4, T5, T6, Try<R>> liftTry(Function6<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? super T6, ? extends R> partialFunction) {
        return (t1, t2, t3, t4, t5, t6) -> Try.of(() -> partialFunction.apply(t1, t2, t3, t4, t5, t6));
    }

    /**
     * Narrows the given {@code Function6<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? super T6, ? extends R>} to {@code Function6<T1, T2, T3, T4, T5, T6, R>}
     *
     * @param f A {@code Function6}
     * @param <R> return type
     * @param <T1> 1st argument
     * @param <T2> 2nd argument
     * @param <T3> 3rd argument
     * @param <T4> 4th argument
     * @param <T5> 5th argument
     * @param <T6> 6th argument
     * @return the given {@code f} instance as narrowed type {@code Function6<T1, T2, T3, T4, T5, T6, R>}
     */
    @SuppressWarnings("unchecked")
    static <T1 extends @Nullable Object, T2 extends @Nullable Object, T3 extends @Nullable Object, T4 extends @Nullable Object, T5 extends @Nullable Object, T6 extends @Nullable Object, R extends @Nullable Object> Function6<T1, T2, T3, T4, T5, T6, R> narrow(Function6<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? super T6, ? extends R> f) {
        return (Function6<T1, T2, T3, T4, T5, T6, R>) f;
    }

    /**
     * Applies this function to 6 arguments and returns the result.
     *
     * @param t1 argument 1
     * @param t2 argument 2
     * @param t3 argument 3
     * @param t4 argument 4
     * @param t5 argument 5
     * @param t6 argument 6
     * @return the result of function application
     * 
     */
    R apply(T1 t1, T2 t2, T3 t3, T4 t4, T5 t5, T6 t6);

    /**
     * Applies this function partially to one argument.
     *
     * @param t1 argument 1
     * @return a partial application of this function
     */
    default Function5<T2, T3, T4, T5, T6, R> apply(T1 t1) {
        return (T2 t2, T3 t3, T4 t4, T5 t5, T6 t6) -> apply(t1, t2, t3, t4, t5, t6);
    }

    /**
     * Applies this function partially to two arguments.
     *
     * @param t1 argument 1
     * @param t2 argument 2
     * @return a partial application of this function
     */
    default Function4<T3, T4, T5, T6, R> apply(T1 t1, T2 t2) {
        return (T3 t3, T4 t4, T5 t5, T6 t6) -> apply(t1, t2, t3, t4, t5, t6);
    }

    /**
     * Applies this function partially to three arguments.
     *
     * @param t1 argument 1
     * @param t2 argument 2
     * @param t3 argument 3
     * @return a partial application of this function
     */
    default Function3<T4, T5, T6, R> apply(T1 t1, T2 t2, T3 t3) {
        return (T4 t4, T5 t5, T6 t6) -> apply(t1, t2, t3, t4, t5, t6);
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
    default BiFunction<T5, T6, R> apply(T1 t1, T2 t2, T3 t3, T4 t4) {
        return (T5 t5, T6 t6) -> apply(t1, t2, t3, t4, t5, t6);
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
    default Function<T6, R> apply(T1 t1, T2 t2, T3 t3, T4 t4, T5 t5) {
        return (T6 t6) -> apply(t1, t2, t3, t4, t5, t6);
    }

    /**
     * Returns a curried version of this function.
     *
     * @return a curried function equivalent to this.
     */
    default Function<T1, Function<T2, Function<T3, Function<T4, Function<T5, Function<T6, R>>>>>> curried() {
        return t1 -> t2 -> t3 -> t4 -> t5 -> t6 -> apply(t1, t2, t3, t4, t5, t6);
    }

    /**
     * Returns a tupled version of this function.
     *
     * @return a tupled function equivalent to this.
     */
    default Function<Tuple6<T1, T2, T3, T4, T5, T6>, R> tupled() {
        return t -> apply(t._1(), t._2(), t._3(), t._4(), t._5(), t._6());
    }

    /**
     * Returns a composed function that first applies this Function6 to the given arguments and then applies
     * {@linkplain Function} {@code after} to the result.
     *
     * @param <V> return type of after
     * @param after the function applied after this
     * @return a function composed of this and after
     * @throws NullPointerException if after is null
     */
    default <V extends @Nullable Object> Function6<T1, T2, T3, T4, T5, T6, V> andThen(Function<? super R, ? extends V> after) {
        Objects.requireNonNull(after, "after is null");
        return (t1, t2, t3, t4, t5, t6) -> after.apply(apply(t1, t2, t3, t4, t5, t6));
    }

    /**
     * Returns a composed function that first applies the {@linkplain Function} {@code before} to the
     * 1st argument and then applies this Function6 to the result and the other arguments.
     *
     * @param <S> argument type of before
     * @param before the function applied before this
     * @return a function composed of before and this
     * @throws NullPointerException if before is null
     */
    default <S extends @Nullable Object> Function6<S, T2, T3, T4, T5, T6, R> compose1(Function<? super S, ? extends T1> before) {
        Objects.requireNonNull(before, "before is null");
        return (S s, T2 t2, T3 t3, T4 t4, T5 t5, T6 t6) -> apply(before.apply(s), t2, t3, t4, t5, t6);
    }

    /**
     * Returns a composed function that first applies the {@linkplain Function} {@code before} to the
     * 2nd argument and then applies this Function6 to the result and the other arguments.
     *
     * @param <S> argument type of before
     * @param before the function applied before this
     * @return a function composed of before and this
     * @throws NullPointerException if before is null
     */
    default <S extends @Nullable Object> Function6<T1, S, T3, T4, T5, T6, R> compose2(Function<? super S, ? extends T2> before) {
        Objects.requireNonNull(before, "before is null");
        return (T1 t1, S s, T3 t3, T4 t4, T5 t5, T6 t6) -> apply(t1, before.apply(s), t3, t4, t5, t6);
    }

    /**
     * Returns a composed function that first applies the {@linkplain Function} {@code before} to the
     * 3rd argument and then applies this Function6 to the result and the other arguments.
     *
     * @param <S> argument type of before
     * @param before the function applied before this
     * @return a function composed of before and this
     * @throws NullPointerException if before is null
     */
    default <S extends @Nullable Object> Function6<T1, T2, S, T4, T5, T6, R> compose3(Function<? super S, ? extends T3> before) {
        Objects.requireNonNull(before, "before is null");
        return (T1 t1, T2 t2, S s, T4 t4, T5 t5, T6 t6) -> apply(t1, t2, before.apply(s), t4, t5, t6);
    }

    /**
     * Returns a composed function that first applies the {@linkplain Function} {@code before} to the
     * 4th argument and then applies this Function6 to the result and the other arguments.
     *
     * @param <S> argument type of before
     * @param before the function applied before this
     * @return a function composed of before and this
     * @throws NullPointerException if before is null
     */
    default <S extends @Nullable Object> Function6<T1, T2, T3, S, T5, T6, R> compose4(Function<? super S, ? extends T4> before) {
        Objects.requireNonNull(before, "before is null");
        return (T1 t1, T2 t2, T3 t3, S s, T5 t5, T6 t6) -> apply(t1, t2, t3, before.apply(s), t5, t6);
    }

    /**
     * Returns a composed function that first applies the {@linkplain Function} {@code before} to the
     * 5th argument and then applies this Function6 to the result and the other arguments.
     *
     * @param <S> argument type of before
     * @param before the function applied before this
     * @return a function composed of before and this
     * @throws NullPointerException if before is null
     */
    default <S extends @Nullable Object> Function6<T1, T2, T3, T4, S, T6, R> compose5(Function<? super S, ? extends T5> before) {
        Objects.requireNonNull(before, "before is null");
        return (T1 t1, T2 t2, T3 t3, T4 t4, S s, T6 t6) -> apply(t1, t2, t3, t4, before.apply(s), t6);
    }

    /**
     * Returns a composed function that first applies the {@linkplain Function} {@code before} to the
     * 6th argument and then applies this Function6 to the result and the other arguments.
     *
     * @param <S> argument type of before
     * @param before the function applied before this
     * @return a function composed of before and this
     * @throws NullPointerException if before is null
     */
    default <S extends @Nullable Object> Function6<T1, T2, T3, T4, T5, S, R> compose6(Function<? super S, ? extends T6> before) {
        Objects.requireNonNull(before, "before is null");
        return (T1 t1, T2 t2, T3 t3, T4 t4, T5 t5, S s) -> apply(t1, t2, t3, t4, t5, before.apply(s));
    }
}