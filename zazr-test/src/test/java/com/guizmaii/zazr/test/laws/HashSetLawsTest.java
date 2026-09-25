package com.guizmaii.zazr.test.laws;

import com.guizmaii.zazr.collection.HashSet;
import com.guizmaii.zazr.test.Arbitrary;
import java.util.Random;
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
            public Arbitrary<HashSet<?>> values() {
                return Arbitrary.hashSet(Arbitrary.integer()).map(value -> value);
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
        return new CollectionSubject<>(Arbitrary.hashSet(Arbitrary.integer()), HashSet::ofAll, HashSet::size, HashSet::toList, false);
    }

    @Override
    BuilderLaws.CollectorSubject<Integer, HashSet<Integer>> collector() {
        return new BuilderLaws.CollectorSubject<>(Arbitrary.list(Arbitrary.integer()), HashSet.collector(), HashSet::ofAll);
    }

    @Test
    void setLawsWithCollidingHashCodes() {
        final CollectionSubject<Collider, HashSet<Collider>> colliders = new CollectionSubject<>(
                Arbitrary.hashSet(Arbitrary.integer().map(Collider::new)), HashSet::ofAll, HashSet::size, HashSet::toList, false);
        CollectionLaws.<Collider, HashSet<Collider>>set().assertSatisfied(colliders, new Random(LawChecks.SEED), LawChecks.SIZE, LawChecks.TRIES);
        EqualityLaws.<HashSet<Collider>>all().assertSatisfied(
                new EqualitySubject<>(colliders.values(), s -> HashSet.ofAll(s.toList())), new Random(LawChecks.SEED), LawChecks.SIZE, LawChecks.TRIES);
    }
}
