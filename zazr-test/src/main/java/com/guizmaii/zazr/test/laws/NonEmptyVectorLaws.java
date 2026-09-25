package com.guizmaii.zazr.test.laws;

import com.guizmaii.zazr.collection.NonEmptyVector;
import com.guizmaii.zazr.test.legacy.Arbitrary;
import com.guizmaii.zazr.test.legacy.Property;

/**
 * The laws specific to {@code NonEmptyVector}.
 */
public final class NonEmptyVectorLaws {

    private NonEmptyVectorLaws() {
    }

    /**
     * {@code head()} never throws and returns the first element of the iteration and of {@code toVector()}.
     *
     * @return the law, checked against arbitrary non-empty vectors
     */
    public static Law<Arbitrary<NonEmptyVector<?>>> nonEmptyVectorHeadIsTotal() {
        return Law.of("nonEmptyVectorHeadIsTotal", values -> Property.named("nonEmptyVectorHeadIsTotal")
                .forAll(values)
                .suchThatResult(nev -> Results.both(Results.equal(nev.head(), nev.iterator().next()),
                        Results.both(Results.equal(nev.head(), nev.toVector().head()),
                                Results.check(nev.size() >= 1, nev + " has size " + nev.size())))));
    }

    /**
     * {@code nev.equals(nev.toVector()) == nev.toVector().equals(nev)}.
     *
     * @return the law, checked against arbitrary non-empty vectors
     */
    public static Law<Arbitrary<NonEmptyVector<?>>> nonEmptyVectorEqualsVectorSymmetry() {
        return Law.of("nonEmptyVectorEqualsVectorSymmetry", values -> Property.named("nonEmptyVectorEqualsVectorSymmetry")
                .forAll(values)
                .suchThatResult(nev -> EqualityLaws.consistent(nev, nev.toVector())));
    }

    /**
     * {@link #nonEmptyVectorHeadIsTotal()} and {@link #nonEmptyVectorEqualsVectorSymmetry()}.
     *
     * @return the law set
     */
    public static Laws<Arbitrary<NonEmptyVector<?>>> all() {
        return Laws.of(nonEmptyVectorHeadIsTotal(), nonEmptyVectorEqualsVectorSymmetry());
    }
}
