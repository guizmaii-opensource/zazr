package com.guizmaii.zazr.test.laws;

import com.guizmaii.zazr.test.Arbitrary;

import java.util.Objects;
import java.util.function.UnaryOperator;

/**
 * A type checked by {@link EqualityLaws}: its arbitrary values, and a way to build an equal value that is not the
 * same instance.
 *
 * @param values arbitrary values of the type under test
 * @param copy   builds a value equal to its argument through another construction path than the one that built
 *               the argument, where the type has one
 * @param <T>    the type under test
 */
public record EqualitySubject<T>(Arbitrary<T> values, UnaryOperator<T> copy) {

    /**
     * Creates a subject.
     *
     * @param values arbitrary values of the type under test
     * @param copy   builds a value equal to its argument
     * @throws NullPointerException if an argument is null
     */
    public EqualitySubject {
        Objects.requireNonNull(values, "values is null");
        Objects.requireNonNull(copy, "copy is null");
    }
}
