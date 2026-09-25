package com.guizmaii.zazr.test.laws;

/**
 * A type checked by the {@link ZipLaws}: its {@code map} and its {@code zip}, whose elements are
 * {@link com.guizmaii.zazr.Tuple2}s.
 *
 * @param <F> the type under test, with a wildcard element type, e.g. {@code Option<?>}
 */
public interface ZipSubject<F> extends MapSubject<F> {

    /**
     * Pairs the elements of {@code fa} with those of {@code fb}.
     *
     * @param fa a value of the type under test
     * @param fb another value of the type under test
     * @return {@code fa.zip(fb)}
     */
    F zip(F fa, F fb);
}
