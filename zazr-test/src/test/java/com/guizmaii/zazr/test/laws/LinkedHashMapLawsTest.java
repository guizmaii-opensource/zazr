package com.guizmaii.zazr.test.laws;

import com.guizmaii.zazr.Tuple2;
import com.guizmaii.zazr.collection.LinkedHashMap;
import com.guizmaii.zazr.test.Arbitrary;
import java.util.Random;
import java.util.function.Function;
import org.junit.jupiter.api.Test;

/**
 * The laws of {@code LinkedHashMap}.
 */
class LinkedHashMapLawsTest extends MapLawsSuite<LinkedHashMap<?, ?>, LinkedHashMap<Integer, Integer>> {

    @Override
    MapSubject<LinkedHashMap<?, ?>> mapValues() {
        return new MapSubject<>() {

            @Override
            public Arbitrary<LinkedHashMap<?, ?>> values() {
                return Arbitrary.linkedHashMap(Arbitrary.integer(), Arbitrary.integer()).map(value -> value);
            }

            @Override
            public LinkedHashMap<?, ?> map(LinkedHashMap<?, ?> fa, Function<Object, Object> f) {
                return fa.mapValues(f);
            }
        };
    }

    @Override
    CollectionSubject<Tuple2<Integer, Integer>, LinkedHashMap<Integer, Integer>> collection() {
        return new CollectionSubject<>(Arbitrary.linkedHashMap(Arbitrary.integer(), Arbitrary.integer()), LinkedHashMap::ofEntries, LinkedHashMap::size,
                LinkedHashMap::toList, false);
    }

    @Override
    BuilderLaws.CollectorSubject<Tuple2<Integer, Integer>, LinkedHashMap<Integer, Integer>> collector() {
        return new BuilderLaws.CollectorSubject<>(Arbitrary.list(Arbitrary.tuple2(Arbitrary.integer(), Arbitrary.integer())),
                LinkedHashMap.collector(), LinkedHashMap::ofEntries);
    }

    @Test
    void mapLawsWithCollidingHashCodes() {
        final CollectionSubject<Tuple2<Collider, Integer>, LinkedHashMap<Collider, Integer>> colliders = new CollectionSubject<>(
                Arbitrary.linkedHashMap(Arbitrary.integer().map(Collider::new), Arbitrary.integer()), LinkedHashMap::ofEntries, LinkedHashMap::size,
                LinkedHashMap::toList, false);
        CollectionLaws.<Tuple2<Collider, Integer>, LinkedHashMap<Collider, Integer>>map()
                .assertSatisfied(colliders, new Random(LawChecks.SEED), LawChecks.SIZE, LawChecks.TRIES);
        EqualityLaws.<LinkedHashMap<Collider, Integer>>all().assertSatisfied(
                new EqualitySubject<>(colliders.values(), m -> LinkedHashMap.ofEntries(m.toList())), new Random(LawChecks.SEED), LawChecks.SIZE,
                LawChecks.TRIES);
    }
}
