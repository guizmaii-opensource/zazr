package com.guizmaii.zazr.test.laws;

import com.guizmaii.zazr.Tuple2;
import com.guizmaii.zazr.collection.HashMap;
import com.guizmaii.zazr.test.legacy.Arbitrary;
import java.util.Random;
import java.util.function.Function;
import org.junit.jupiter.api.Test;

/**
 * The laws of {@code HashMap}.
 */
class HashMapLawsTest extends MapLawsSuite<HashMap<?, ?>, HashMap<Integer, Integer>> {

    @Override
    MapSubject<HashMap<?, ?>> mapValues() {
        return new MapSubject<>() {

            @Override
            public Arbitrary<HashMap<?, ?>> values() {
                return Arbitrary.hashMap(Arbitrary.integer(), Arbitrary.integer()).map(value -> value);
            }

            @Override
            public HashMap<?, ?> map(HashMap<?, ?> fa, Function<Object, Object> f) {
                return fa.mapValues(f);
            }
        };
    }

    @Override
    CollectionSubject<Tuple2<Integer, Integer>, HashMap<Integer, Integer>> collection() {
        return new CollectionSubject<>(Arbitrary.hashMap(Arbitrary.integer(), Arbitrary.integer()), HashMap::ofEntries, HashMap::size,
                HashMap::toList, false);
    }

    @Override
    BuilderLaws.CollectorSubject<Tuple2<Integer, Integer>, HashMap<Integer, Integer>> collector() {
        return new BuilderLaws.CollectorSubject<>(Arbitrary.list(Arbitrary.tuple2(Arbitrary.integer(), Arbitrary.integer())),
                HashMap.collector(), HashMap::ofEntries);
    }

    @Test
    void mapLawsWithCollidingHashCodes() {
        final CollectionSubject<Tuple2<Collider, Integer>, HashMap<Collider, Integer>> colliders = new CollectionSubject<>(
                Arbitrary.hashMap(Arbitrary.integer().map(Collider::new), Arbitrary.integer()), HashMap::ofEntries, HashMap::size,
                HashMap::toList, false);
        CollectionLaws.<Tuple2<Collider, Integer>, HashMap<Collider, Integer>>map()
                .assertSatisfied(colliders, new Random(LawChecks.SEED), LawChecks.SIZE, LawChecks.TRIES);
        EqualityLaws.<HashMap<Collider, Integer>>all().assertSatisfied(
                new EqualitySubject<>(colliders.values(), m -> HashMap.ofEntries(m.toList()), c -> new java.util.HashSet<>(CollectionLaws.elements(c))), new Random(LawChecks.SEED), LawChecks.SIZE,
                LawChecks.TRIES);
    }
}
