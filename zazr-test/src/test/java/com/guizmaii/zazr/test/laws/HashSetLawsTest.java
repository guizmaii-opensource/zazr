package com.guizmaii.zazr.test.laws;

import com.guizmaii.zazr.collection.HashSet;
import com.guizmaii.zazr.test.Gen;
import java.util.function.Function;
import org.junit.jupiter.api.Test;

/**
 * The laws of {@code HashSet}.
 */
class HashSetLawsTest extends SetLawsSuite<HashSet<?>, HashSet<Integer>> {

    @Override
    FlatMapSubject<HashSet<?>> subject() {
        return new FlatMapSubject<>() {

            @Override
            public Gen<HashSet<?>> values() {
                return Gen.hashSet(Values.integers()).map(value -> value);
            }

            @Override
            public HashSet<?> map(HashSet<?> fa, Function<Object, Object> f) {
                return fa.map(f);
            }

            @Override
            public HashSet<?> succeed(Object a) {
                return HashSet.of(a);
            }

            @Override
            public HashSet<?> flatMap(HashSet<?> fa, Function<Object, HashSet<?>> f) {
                return fa.flatMap(f);
            }
        };
    }

    @Override
    CollectionSubject<Integer, HashSet<Integer>> collection() {
        return new CollectionSubject<>(Gen.hashSet(Values.integers()), HashSet::ofAll, HashSet::size, HashSet::toList, false);
    }

    @Override
    BuilderLaws.CollectorSubject<Integer, HashSet<Integer>> collector() {
        return new BuilderLaws.CollectorSubject<>(Gen.list(Values.integers()), HashSet.collector(), HashSet::ofAll);
    }

    @Test
    void setLawsWithCollidingHashCodes() {
        final CollectionSubject<Collider, HashSet<Collider>> colliders = new CollectionSubject<>(
                Gen.hashSet(Values.integers().map(Collider::new)), HashSet::ofAll, HashSet::size, HashSet::toList, false);
        LawChecks.check(CollectionLaws.<Collider, HashSet<Collider>>set(), colliders);
        LawChecks.check(EqualityLaws.<HashSet<Collider>>all(), new EqualitySubject<>(colliders.values(), s -> HashSet.ofAll(s.toList()), c -> new java.util.HashSet<>(CollectionLaws.elements(c))));
    }
}
