package com.guizmaii.zazr.test.laws;

import com.guizmaii.zazr.collection.LinkedHashSet;
import com.guizmaii.zazr.control.Option;
import com.guizmaii.zazr.test.Gen;
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
            public Gen<LinkedHashSet<?>> values() {
                return Gen.linkedHashSet(Values.integers()).map(value -> value);
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
        return new CollectionSubject<>(Gen.linkedHashSet(Values.integers()), LinkedHashSet::ofAll, LinkedHashSet::size, LinkedHashSet::toList, false, Option.some(IterationOrder.firstOccurrence()));
    }

    @Override
    BuilderLaws.CollectorSubject<Integer, LinkedHashSet<Integer>> collector() {
        return new BuilderLaws.CollectorSubject<>(Gen.list(Values.integers()), LinkedHashSet.collector(), LinkedHashSet::ofAll, Option.some(IterationOrder.firstOccurrence()));
    }

    @Test
    void setLawsWithCollidingHashCodes() {
        final CollectionSubject<Collider, LinkedHashSet<Collider>> colliders = new CollectionSubject<>(
                Gen.linkedHashSet(Values.integers().map(Collider::new)), LinkedHashSet::ofAll, LinkedHashSet::size, LinkedHashSet::toList, false, Option.some(IterationOrder.firstOccurrence()));
        LawChecks.check(CollectionLaws.<Collider, LinkedHashSet<Collider>>set(), colliders);
        LawChecks.check(EqualityLaws.<LinkedHashSet<Collider>>all(), new EqualitySubject<>(colliders.values(), s -> LinkedHashSet.ofAll(s.toList()), c -> new java.util.HashSet<>(CollectionLaws.elements(c))));
    }
}
