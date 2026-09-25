package com.guizmaii.zazr.test.laws;

import com.guizmaii.zazr.Tuple2;
import com.guizmaii.zazr.collection.LinkedHashMap;
import com.guizmaii.zazr.control.Option;
import com.guizmaii.zazr.test.legacy.Arbitrary;
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
                LinkedHashMap::toList, false, Option.some(IterationOrder.keysByLastOccurrence()));
    }

    @Override
    BuilderLaws.CollectorSubject<Tuple2<Integer, Integer>, LinkedHashMap<Integer, Integer>> collector() {
        return new BuilderLaws.CollectorSubject<>(Arbitrary.list(Arbitrary.tuple2(Arbitrary.integer(), Arbitrary.integer())),
                LinkedHashMap.collector(), LinkedHashMap::ofEntries, Option.some(IterationOrder.keysByLastOccurrence()));
    }

    @Test
    void mapLawsWithCollidingHashCodes() {
        final CollectionSubject<Tuple2<Collider, Integer>, LinkedHashMap<Collider, Integer>> colliders = new CollectionSubject<>(
                Arbitrary.linkedHashMap(Arbitrary.integer().map(Collider::new), Arbitrary.integer()), LinkedHashMap::ofEntries, LinkedHashMap::size,
                LinkedHashMap::toList, false, Option.some(IterationOrder.keysByLastOccurrence()));
        CollectionLaws.<Tuple2<Collider, Integer>, LinkedHashMap<Collider, Integer>>map()
                .assertSatisfied(colliders, new Random(LawChecks.SEED), LawChecks.SIZE, LawChecks.TRIES);
        EqualityLaws.<LinkedHashMap<Collider, Integer>>all().assertSatisfied(
                new EqualitySubject<>(colliders.values(), m -> LinkedHashMap.ofEntries(m.toList()), c -> new java.util.HashSet<>(CollectionLaws.elements(c))), new Random(LawChecks.SEED), LawChecks.SIZE,
                LawChecks.TRIES);
    }

    /// `ofEntries` places a repeated key at its last occurrence; `put` keeps an existing key where it is.
    @Test
    void putKeepsTheFirstPositionOfARepeatedKey() {
        final CollectionSubject<Tuple2<Integer, Integer>, LinkedHashMap<Integer, Integer>> putOneByOne = new CollectionSubject<>(
                Arbitrary.linkedHashMap(Arbitrary.integer(), Arbitrary.integer()), entries -> {
                    LinkedHashMap<Integer, Integer> map = LinkedHashMap.empty();
                    for (Tuple2<Integer, Integer> entry : entries) {
                        map = map.put(entry._1(), entry._2());
                    }
                    return map;
                }, LinkedHashMap::size, LinkedHashMap::toList, false, Option.some(IterationOrder.keysByFirstOccurrence()));
        LawChecks.check(CollectionLaws.<Tuple2<Integer, Integer>, LinkedHashMap<Integer, Integer>>iterationOrder(), putOneByOne);
    }
}
