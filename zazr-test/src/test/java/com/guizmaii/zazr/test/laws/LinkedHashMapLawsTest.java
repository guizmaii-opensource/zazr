package com.guizmaii.zazr.test.laws;

import com.guizmaii.zazr.Tuple2;
import com.guizmaii.zazr.collection.LinkedHashMap;
import com.guizmaii.zazr.control.Option;
import com.guizmaii.zazr.test.Gen;
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
            public Gen<LinkedHashMap<?, ?>> values() {
                return Gen.linkedHashMap(Values.integers(), Values.integers()).map(value -> value);
            }

            @Override
            public LinkedHashMap<?, ?> map(LinkedHashMap<?, ?> fa, Function<Object, Object> f) {
                return fa.mapValues(f);
            }
        };
    }

    @Override
    CollectionSubject<Tuple2<Integer, Integer>, LinkedHashMap<Integer, Integer>> collection() {
        return new CollectionSubject<>(Gen.linkedHashMap(Values.integers(), Values.integers()), LinkedHashMap::ofEntries, LinkedHashMap::size,
                LinkedHashMap::toList, false, Option.some(IterationOrder.keysByLastOccurrence()));
    }

    @Override
    BuilderLaws.CollectorSubject<Tuple2<Integer, Integer>, LinkedHashMap<Integer, Integer>> collector() {
        return new BuilderLaws.CollectorSubject<>(Gen.list(Gen.tuple2(Values.integers(), Values.integers())),
                LinkedHashMap.collector(), LinkedHashMap::ofEntries, Option.some(IterationOrder.keysByLastOccurrence()));
    }

    @Test
    void mapLawsWithCollidingHashCodes() {
        final CollectionSubject<Tuple2<Collider, Integer>, LinkedHashMap<Collider, Integer>> colliders = new CollectionSubject<>(
                Gen.linkedHashMap(Values.integers().map(Collider::new), Values.integers()), LinkedHashMap::ofEntries, LinkedHashMap::size,
                LinkedHashMap::toList, false, Option.some(IterationOrder.keysByLastOccurrence()));
        LawChecks.check(CollectionLaws.<Tuple2<Collider, Integer>, LinkedHashMap<Collider, Integer>>map(), colliders);
        LawChecks.check(EqualityLaws.<LinkedHashMap<Collider, Integer>>all(), new EqualitySubject<>(colliders.values(), m -> LinkedHashMap.ofEntries(m.toList()), c -> new java.util.HashSet<>(CollectionLaws.elements(c))));
    }

    /// `ofEntries` places a repeated key at its last occurrence; `put` keeps an existing key where it is.
    @Test
    void putKeepsTheFirstPositionOfARepeatedKey() {
        final CollectionSubject<Tuple2<Integer, Integer>, LinkedHashMap<Integer, Integer>> putOneByOne = new CollectionSubject<>(
                Gen.linkedHashMap(Values.integers(), Values.integers()), entries -> {
                    LinkedHashMap<Integer, Integer> map = LinkedHashMap.empty();
                    for (Tuple2<Integer, Integer> entry : entries) {
                        map = map.put(entry._1(), entry._2());
                    }
                    return map;
                }, LinkedHashMap::size, LinkedHashMap::toList, false, Option.some(IterationOrder.keysByFirstOccurrence()));
        LawChecks.check(CollectionLaws.<Tuple2<Integer, Integer>, LinkedHashMap<Integer, Integer>>iterationOrder(), putOneByOne);
    }
}
