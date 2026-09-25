package com.guizmaii.zazr.test.laws;

import com.guizmaii.zazr.collection.LinkedHashSet;
import com.guizmaii.zazr.test.Arbitrary;
import java.util.Random;
import java.util.function.Function;
import org.junit.jupiter.api.Test;

/**
 * The laws of {@code LinkedHashSet}.
 */
class LinkedHashSetLawsTest extends SetLawsSuite<LinkedHashSet<?>, LinkedHashSet<Integer>> {

    @Override
    FlatMapSubject<LinkedHashSet<?>> subject() {
        return new FlatMapSubject<>() {

            @Override
            public Arbitrary<LinkedHashSet<?>> values() {
                return Arbitrary.linkedHashSet(Arbitrary.integer()).map(value -> value);
            }

            @Override
            public LinkedHashSet<?> map(LinkedHashSet<?> fa, Function<Object, Object> f) {
                return fa.map(f);
            }

            @Override
            public LinkedHashSet<?> succeed(Object a) {
                return LinkedHashSet.of(a);
            }

            @Override
            public LinkedHashSet<?> flatMap(LinkedHashSet<?> fa, Function<Object, LinkedHashSet<?>> f) {
                return fa.flatMap(f);
            }
        };
    }

    @Override
    CollectionSubject<Integer, LinkedHashSet<Integer>> collection() {
        return new CollectionSubject<>(Arbitrary.linkedHashSet(Arbitrary.integer()), LinkedHashSet::ofAll, LinkedHashSet::size, LinkedHashSet::toList, false);
    }

    @Override
    BuilderLaws.CollectorSubject<Integer, LinkedHashSet<Integer>> collector() {
        return new BuilderLaws.CollectorSubject<>(Arbitrary.list(Arbitrary.integer()), LinkedHashSet.collector(), LinkedHashSet::ofAll);
    }

    @Test
    void setLawsWithCollidingHashCodes() {
        final CollectionSubject<Collider, LinkedHashSet<Collider>> colliders = new CollectionSubject<>(
                Arbitrary.linkedHashSet(Arbitrary.integer().map(Collider::new)), LinkedHashSet::ofAll, LinkedHashSet::size, LinkedHashSet::toList, false);
        CollectionLaws.<Collider, LinkedHashSet<Collider>>set().assertSatisfied(colliders, new Random(LawChecks.SEED), LawChecks.SIZE, LawChecks.TRIES);
        EqualityLaws.<LinkedHashSet<Collider>>all().assertSatisfied(
                new EqualitySubject<>(colliders.values(), s -> LinkedHashSet.ofAll(s.toList())), new Random(LawChecks.SEED), LawChecks.SIZE, LawChecks.TRIES);
    }
}
