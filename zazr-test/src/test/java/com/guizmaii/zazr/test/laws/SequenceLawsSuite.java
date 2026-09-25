package com.guizmaii.zazr.test.laws;

import org.junit.jupiter.api.Test;

/**
 * The laws of a sequence: {@code map}, {@code flatMap}, the positional {@code zip}, the collection contracts, its
 * collector, and {@code equals}/{@code hashCode}. A subclass per type supplies the subjects.
 *
 * @param <F> the sequence type with a wildcard element type, for the operation laws
 * @param <C> the sequence type of integers, for the collection laws
 * @param <S> the subject of the operation laws
 */
abstract class SequenceLawsSuite<F, C extends Iterable<Integer>, S extends FlatMapSubject<F> & ZipSubject<F>> {

    abstract S subject();

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
    void zipAssociativity() {
        LawChecks.check(ZipLaws.<F>zipAssociativity(), subject());
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
    void sequenceEqualsAcrossTypes() {
        LawChecks.check(CollectionLaws.<Integer, C>sequenceEqualsAcrossTypes(), collection());
    }

    @Test
    void equalsHashCodeConsistency() {
        final CollectionSubject<Integer, C> collection = collection();
        LawChecks.check(EqualityLaws.<C>equalsHashCodeConsistency(),
                new EqualitySubject<>(collection.values(), c -> collection.ofAll().apply(collection.toList().apply(c))));
    }

    @Test
    void collectorResultEqualsOfAll() {
        LawChecks.check(BuilderLaws.<Integer, C>collectorResultEqualsOfAll(), collector());
    }
}
