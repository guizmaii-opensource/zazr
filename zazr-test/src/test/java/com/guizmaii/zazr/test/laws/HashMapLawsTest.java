package com.guizmaii.zazr.test.laws;

import com.guizmaii.zazr.Tuple2;
import com.guizmaii.zazr.collection.HashMap;
import com.guizmaii.zazr.test.Gen;
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
            public Gen<HashMap<?, ?>> values() {
                return Gen.hashMap(Values.integers(), Values.integers()).map(value -> value);
            }

            @Override
            public HashMap<?, ?> map(HashMap<?, ?> fa, Function<Object, Object> f) {
                return fa.mapValues(f);
            }
        };
    }

    @Override
    CollectionSubject<Tuple2<Integer, Integer>, HashMap<Integer, Integer>> collection() {
        return new CollectionSubject<>(Gen.hashMap(Values.integers(), Values.integers()), HashMap::ofEntries, HashMap::size,
                HashMap::toList, false);
    }

    @Override
    BuilderLaws.CollectorSubject<Tuple2<Integer, Integer>, HashMap<Integer, Integer>> collector() {
        return new BuilderLaws.CollectorSubject<>(Gen.list(Gen.tuple2(Values.integers(), Values.integers())),
                HashMap.collector(), HashMap::ofEntries);
    }

    @Test
    void mapLawsWithCollidingHashCodes() {
        final CollectionSubject<Tuple2<Collider, Integer>, HashMap<Collider, Integer>> colliders = new CollectionSubject<>(
                Gen.hashMap(Values.integers().map(Collider::new), Values.integers()), HashMap::ofEntries, HashMap::size,
                HashMap::toList, false);
        LawChecks.check(CollectionLaws.<Tuple2<Collider, Integer>, HashMap<Collider, Integer>>map(), colliders);
        LawChecks.check(EqualityLaws.<HashMap<Collider, Integer>>all(), new EqualitySubject<>(colliders.values(), m -> HashMap.ofEntries(m.toList()), c -> new java.util.HashSet<>(CollectionLaws.elements(c))));
    }
}
