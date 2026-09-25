package com.guizmaii.zazr.test.laws;

import com.guizmaii.zazr.Tuple;
import com.guizmaii.zazr.control.Validation;
import com.guizmaii.zazr.test.Arbitrary;
import com.guizmaii.zazr.test.Property;

/**
 * The laws specific to {@code Validation}.
 */
public final class ValidationLaws {

    private ValidationLaws() {
    }

    /**
     * {@code a.zip(b)} is {@code Valid} of both values when both are valid, and otherwise {@code Invalid} of the
     * errors of {@code a} followed by those of {@code b}.
     *
     * @return the law, checked against arbitrary validations
     */
    public static Law<Arbitrary<Validation<?, ?>>> validationZipAccumulatesBothSides() {
        return Law.of("validationZipAccumulatesBothSides", values -> Property.named("validationZipAccumulatesBothSides")
                .forAll(values, values)
                .suchThatResult((a, b) -> Results.equal(zip(a, b), expected(a, b))));
    }

    /**
     * {@link #validationZipAccumulatesBothSides()}.
     *
     * @return the law set
     */
    public static Laws<Arbitrary<Validation<?, ?>>> all() {
        return Laws.of(validationZipAccumulatesBothSides());
    }

    @SuppressWarnings("unchecked")
    private static Validation<Object, ?> zip(Validation<?, ?> a, Validation<?, ?> b) {
        return ((Validation<Object, Object>) a).zip((Validation<Object, Object>) b);
    }

    @SuppressWarnings("unchecked")
    private static Validation<Object, ?> expected(Validation<?, ?> a, Validation<?, ?> b) {
        return switch (a) {
            case Validation.Valid<?, ?>(var x) -> switch (b) {
                case Validation.Valid<?, ?>(var y) -> Validation.valid(Tuple.of(x, y));
                case Validation.Invalid<?, ?>(var errors) -> Validation.invalidAll((com.guizmaii.zazr.collection.NonEmptyVector<Object>) errors);
            };
            case Validation.Invalid<?, ?>(var errorsA) -> switch (b) {
                case Validation.Valid<?, ?> ignored -> Validation.invalidAll((com.guizmaii.zazr.collection.NonEmptyVector<Object>) errorsA);
                case Validation.Invalid<?, ?>(var errorsB) -> Validation.invalidAll(
                        ((com.guizmaii.zazr.collection.NonEmptyVector<Object>) errorsA).appendAll(errorsB));
            };
        };
    }
}
