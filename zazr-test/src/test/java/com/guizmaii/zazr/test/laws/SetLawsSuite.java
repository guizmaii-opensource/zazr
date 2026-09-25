package com.guizmaii.zazr.test.laws;

import org.junit.jupiter.api.Test;

/**
 * The laws of a set: {@code map}, {@code flatMap}, the collection contracts, its collector, and
 * {@code equals}/{@code hashCode}. A subclass per type supplies the subjects.
 *
 * @param <F> the set type with a wildcard element type, for the operation laws
 * @param <C> the set type of integers, for the collection laws
 */
abstract class SetLawsSuite<F, C extends Iterable<Integer>> {

    abstract FlatMapSubject<F> subject();

    abstract CollectionSubject<Integer, C> collection();

    abstract BuilderLaws.CollectorSubject<Integer, C> collector();

    @Test
    void mapIdentity() {
        LawChecks.check(MapLaws.<F>mapIdentity(), subject());
    }

    @Test
    void mapComposition() {
        LawChecks.check(MapLaws.<F>mapComposition(), subject());
    }

    @Test
    void flatMapAssociativity() {
        LawChecks.check(FlatMapLaws.<F>flatMapAssociativity(), subject());
    }

    @Test
    void flatMapLeftIdentity() {
        LawChecks.check(FlatMapLaws.<F>flatMapLeftIdentity(), subject());
    }

    @Test
    void flatMapRightIdentity() {
        LawChecks.check(FlatMapLaws.<F>flatMapRightIdentity(), subject());
    }

    @Test
    void mapIsFlatMapSucceed() {
        LawChecks.check(FlatMapLaws.<F>mapIsFlatMapSucceed(), subject());
    }

    @Test
    void sizeEqualsIterationCount() {
        LawChecks.check(CollectionLaws.<Integer, C>sizeEqualsIterationCount(), collection());
    }

    @Test
    void toListRoundTrip() {
        LawChecks.check(CollectionLaws.<Integer, C>toListRoundTrip(), collection());
    }

    @Test
    void equalsAgreesWithElements() {
        LawChecks.check(CollectionLaws.<Integer, C>equalsAgreesWithElements(), collection());
    }

    @Test
    void setEqualsAcrossTypes() {
        LawChecks.check(CollectionLaws.<Integer, C>setEqualsAcrossTypes(), collection());
    }

    @Test
    void iterationOrder() {
        LawChecks.check(CollectionLaws.<Integer, C>iterationOrder(), collection());
    }

    @Test
    void equalsHashCodeConsistency() {
        LawChecks.check(EqualityLaws.<C>equalsHashCodeConsistency(), equality());
    }

    @Test
    void equalsAgreesWithModel() {
        LawChecks.check(EqualityLaws.<C>equalsAgreesWithModel(), equality());
    }

    /// Equality modelled by the elements: a JDK list for a sequence, a JDK set otherwise.
    private EqualitySubject<C> equality() {
        final CollectionSubject<Integer, C> collection = collection();
        return new EqualitySubject<>(collection.values(), c -> collection.ofAll().apply(collection.toList().apply(c)),
                c -> collection.ordered() ? CollectionLaws.elements(c) : new java.util.HashSet<>(CollectionLaws.elements(c)));
    }

    @Test
    void collectorResultEqualsOfAll() {
        LawChecks.check(BuilderLaws.<Integer, C>collectorResultEqualsOfAll(), collector());
    }
}
