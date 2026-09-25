package com.guizmaii.zazr.test.laws;

import com.guizmaii.zazr.collection.TreeSet;
import com.guizmaii.zazr.control.Option;
import com.guizmaii.zazr.test.Arbitrary;


import java.util.function.Function;


/**
 * The laws of {@code TreeSet}.
 */
class TreeSetLawsTest extends SetLawsSuite<TreeSet<?>, TreeSet<Integer>> {

    @Override
    FlatMapSubject<TreeSet<?>> subject() {
        return new FlatMapSubject<>() {

            @Override
            public Arbitrary<TreeSet<?>> values() {
                return Arbitrary.treeSet(Arbitrary.integer()).map(value -> value);
            }

            @Override
            public TreeSet<?> map(TreeSet<?> fa, Function<Object, Object> f) {
                return fa.map(f);
            }

            @Override
            public TreeSet<?> succeed(Object a) {
                return TreeSet.of((Integer) a);
            }

            @Override
            public TreeSet<?> flatMap(TreeSet<?> fa, Function<Object, TreeSet<?>> f) {
                return fa.flatMap(f);
            }
        };
    }

    @Override
    CollectionSubject<Integer, TreeSet<Integer>> collection() {
        return new CollectionSubject<>(Arbitrary.treeSet(Arbitrary.integer()), TreeSet::ofAll, TreeSet::size, TreeSet::toList, false, Option.some(IterationOrder.sorted(java.util.Comparator.<Integer>naturalOrder())));
    }

    @Override
    BuilderLaws.CollectorSubject<Integer, TreeSet<Integer>> collector() {
        return new BuilderLaws.CollectorSubject<>(Arbitrary.list(Arbitrary.integer()), TreeSet.collector(), TreeSet::ofAll, Option.some(IterationOrder.sorted(java.util.Comparator.<Integer>naturalOrder())));
    }
}
