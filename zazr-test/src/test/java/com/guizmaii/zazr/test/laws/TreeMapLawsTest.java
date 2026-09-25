package com.guizmaii.zazr.test.laws;

import com.guizmaii.zazr.Tuple2;
import com.guizmaii.zazr.collection.TreeMap;
import com.guizmaii.zazr.control.Option;
import com.guizmaii.zazr.test.legacy.Arbitrary;


import java.util.function.Function;


/**
 * The laws of {@code TreeMap}.
 */
class TreeMapLawsTest extends MapLawsSuite<TreeMap<?, ?>, TreeMap<Integer, Integer>> {

    @Override
    MapSubject<TreeMap<?, ?>> mapValues() {
        return new MapSubject<>() {

            @Override
            public Arbitrary<TreeMap<?, ?>> values() {
                return Arbitrary.treeMap(Arbitrary.integer(), Arbitrary.integer()).map(value -> value);
            }

            @Override
            public TreeMap<?, ?> map(TreeMap<?, ?> fa, Function<Object, Object> f) {
                return fa.mapValues(f);
            }
        };
    }

    @Override
    CollectionSubject<Tuple2<Integer, Integer>, TreeMap<Integer, Integer>> collection() {
        return new CollectionSubject<>(Arbitrary.treeMap(Arbitrary.integer(), Arbitrary.integer()), TreeMap::ofEntries, TreeMap::size,
                TreeMap::toList, false, Option.some(IterationOrder.keysSorted(java.util.Comparator.<Integer>naturalOrder())));
    }

    @Override
    BuilderLaws.CollectorSubject<Tuple2<Integer, Integer>, TreeMap<Integer, Integer>> collector() {
        return new BuilderLaws.CollectorSubject<>(Arbitrary.list(Arbitrary.tuple2(Arbitrary.integer(), Arbitrary.integer())),
                TreeMap.collector(), TreeMap::ofEntries, Option.some(IterationOrder.keysSorted(java.util.Comparator.<Integer>naturalOrder())));
    }
}
