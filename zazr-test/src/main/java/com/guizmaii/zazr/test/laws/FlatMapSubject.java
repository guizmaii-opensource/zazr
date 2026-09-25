package com.guizmaii.zazr.test.laws;

import java.util.function.Function;

/**
 * A type checked by the {@link FlatMapLaws}: its {@code map}, its {@code flatMap}, and the value holding a single
 * element.
 *
 * @param <F> the type under test, with a wildcard element type, e.g. {@code Option<?>}
 */
public interface FlatMapSubject<F> extends MapSubject<F> {

    /**
     * The value holding the single element {@code a}: {@code Option.some(a)}, {@code Vector.of(a)}, ...
     *
     * @param a the element
     * @return a value of the type under test
     */
    F succeed(Object a);

    /**
     * Applies {@code f} to every element of {@code fa} and joins the results.
     *
     * @param fa a value of the type under test
     * @param f  the function
     * @return {@code fa.flatMap(f)}
     */
    F flatMap(F fa, Function<Object, F> f);
}
