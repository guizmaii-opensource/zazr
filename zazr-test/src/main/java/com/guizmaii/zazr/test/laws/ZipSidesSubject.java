package com.guizmaii.zazr.test.laws;

/**
 * A type checked by {@link ZipLaws#zipSides()}: its {@code zip}, {@code zipLeft}, {@code zipRight}, and the value
 * holding a single element.
 *
 * @param <F> the type under test, with a wildcard element type, e.g. {@code Option<?>}
 */
public interface ZipSidesSubject<F> extends ZipSubject<F> {

    /**
     * The value holding the single element {@code a}: {@code Option.some(a)}, {@code Either.right(a)}, ...
     *
     * @param a the element
     * @return a value of the type under test
     */
    F succeed(Object a);

    /**
     * Combines {@code fa} and {@code fb} and keeps the elements of {@code fa}.
     *
     * @param fa a value of the type under test
     * @param fb another value of the type under test
     * @return {@code fa.zipLeft(fb)}
     */
    F zipLeft(F fa, F fb);

    /**
     * Combines {@code fa} and {@code fb} and keeps the elements of {@code fb}.
     *
     * @param fa a value of the type under test
     * @param fb another value of the type under test
     * @return {@code fa.zipRight(fb)}
     */
    F zipRight(F fa, F fb);
}
