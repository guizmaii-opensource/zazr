package com.guizmaii.zazr.test.laws;

import com.guizmaii.zazr.test.Arbitrary;

import java.util.Objects;
import java.util.function.Function;
import java.util.function.UnaryOperator;

/**
 * A type checked by {@link EqualityLaws}: its arbitrary values, a way to build an equal value that is not the same
 * instance, and a model of each value whose {@code equals} is known to be right.
 *
 * @param values arbitrary values of the type under test
 * @param copy   builds a value equal to its argument through another construction path than the one that built
 *               the argument, where the type has one
 * @param model  maps a value to a reference representation that is equal exactly when the values must be: the
 *               elements as a {@link java.util.List}, an {@code Option} as an {@link java.util.Optional}, a tag and
 *               a value as a list, ...
 * @param <T>    the type under test
 */
public record EqualitySubject<T>(Arbitrary<T> values, UnaryOperator<T> copy, Function<? super T, ?> model) {

    /**
     * Creates a subject.
     *
     * @param values arbitrary values of the type under test
     * @param copy   builds a value equal to its argument
     * @param model  maps a value to its reference representation
     * @throws NullPointerException if an argument is null
     */
    public EqualitySubject {
        Objects.requireNonNull(values, "values is null");
        Objects.requireNonNull(copy, "copy is null");
        Objects.requireNonNull(model, "model is null");
    }
}
