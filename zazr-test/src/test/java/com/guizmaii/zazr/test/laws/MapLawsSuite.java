package com.guizmaii.zazr.test.laws;

import com.guizmaii.zazr.Tuple2;
import org.junit.jupiter.api.Test;

/**
 * The laws of a map: {@code mapValues} (the map laws over the values), the collection contracts over the entries,
 * its collector, and {@code equals}/{@code hashCode}. A subclass per type supplies the subjects.
 *
 * @param <F> the map type with wildcard key and value types, for the {@code mapValues} laws
 * @param <C> the map type of integers, for the collection laws
 */
abstract class MapLawsSuite<F, C extends Iterable<Tuple2<Integer, Integer>>> {

    abstract MapSubject<F> mapValues();

    abstract CollectionSubject<Tuple2<Integer, Integer>, C> collection();

    abstract BuilderLaws.CollectorSubject<Tuple2<Integer, Integer>, C> collector();

    @Test
    void mapIdentity() {
        LawChecks.check(MapLaws.<F>mapIdentity(), mapValues());
    }

    @Test
    void mapComposition() {
        LawChecks.check(MapLaws.<F>mapComposition(), mapValues());
    }

    @Test
    void sizeEqualsIterationCount() {
        LawChecks.check(CollectionLaws.<Tuple2<Integer, Integer>, C>sizeEqualsIterationCount(), collection());
    }

    @Test
    void toListRoundTrip() {
        LawChecks.check(CollectionLaws.<Tuple2<Integer, Integer>, C>toListRoundTrip(), collection());
    }

    @Test
    void equalsAgreesWithElements() {
        LawChecks.check(CollectionLaws.<Tuple2<Integer, Integer>, C>equalsAgreesWithElements(), collection());
    }

    @Test
    void mapEqualsAcrossTypes() {
        LawChecks.check(CollectionLaws.<Tuple2<Integer, Integer>, C>mapEqualsAcrossTypes(), collection());
    }

    @Test
    void iterationOrder() {
        LawChecks.check(CollectionLaws.<Tuple2<Integer, Integer>, C>iterationOrder(), collection());
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
        final CollectionSubject<Tuple2<Integer, Integer>, C> collection = collection();
        return new EqualitySubject<>(collection.values(), c -> collection.ofAll().apply(collection.toList().apply(c)),
                c -> collection.ordered() ? CollectionLaws.elements(c) : new java.util.HashSet<>(CollectionLaws.elements(c)));
    }

    @Test
    void collectorResultEqualsOfAll() {
        LawChecks.check(BuilderLaws.<Tuple2<Integer, Integer>, C>collectorResultEqualsOfAll(), collector());
    }
}
