package com.guizmaii.zazr.test.laws;

import com.guizmaii.zazr.test.Gen;

import java.util.function.Function;

/**
 * A type checked by the {@link MapLaws}: the generator of its values and its {@code map}.
 * <p>
 * The laws cannot name the element type of {@code F}, so they see the elements as {@code Object}. The values of
 * {@link #values()} hold {@link Integer} elements: the functions the laws generate are functions of integers.
 *
 * @param <F> the type under test, with a wildcard element type, e.g. {@code Option<?>}
 */
public interface MapSubject<F> {

    /**
     * The generator of the values of the type under test, whose elements are integers.
     *
     * @return a generator
     */
    Gen<F> values();

    /**
     * Applies {@code f} to every element of {@code fa}.
     *
     * @param fa a value of the type under test
     * @param f  the function
     * @return {@code fa.map(f)}
     */
    F map(F fa, Function<Object, Object> f);
}
