package com.guizmaii.zazr.test.laws;

import com.guizmaii.zazr.collection.NonEmptyVector;
import com.guizmaii.zazr.test.Check;
import com.guizmaii.zazr.test.Gen;

/**
 * The laws specific to {@code NonEmptyVector}.
 */
public final class NonEmptyVectorLaws {

    private NonEmptyVectorLaws() {
    }

    /**
     * {@code head()} never throws and returns the first element of the iteration and of {@code toVector()}.
     *
     * @return the law, checked against a generator of non-empty vectors
     */
    public static Law<Gen<NonEmptyVector<?>>> nonEmptyVectorHeadIsTotal() {
        return Law.of("nonEmptyVectorHeadIsTotal", (values, config) -> Check.check(config, values,
                nev -> Results.equal(nev.head(), nev.iterator().next()) && Results.equal(nev.head(), nev.toVector().head())
                        && Results.check(nev.size() >= 1, () -> nev + " has size " + nev.size())));
    }

    /**
     * {@code nev.equals(nev.toVector()) == nev.toVector().equals(nev)}.
     *
     * @return the law, checked against a generator of non-empty vectors
     */
    public static Law<Gen<NonEmptyVector<?>>> nonEmptyVectorEqualsVectorSymmetry() {
        return Law.of("nonEmptyVectorEqualsVectorSymmetry", (values, config) -> Check.check(config, values,
                nev -> EqualityLaws.consistent(nev, nev.toVector())));
    }

    /**
     * {@link #nonEmptyVectorHeadIsTotal()} and {@link #nonEmptyVectorEqualsVectorSymmetry()}.
     *
     * @return the law set
     */
    public static Laws<Gen<NonEmptyVector<?>>> all() {
        return Laws.of(nonEmptyVectorHeadIsTotal(), nonEmptyVectorEqualsVectorSymmetry());
    }
}
