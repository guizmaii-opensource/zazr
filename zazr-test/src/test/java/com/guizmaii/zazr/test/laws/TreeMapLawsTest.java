package com.guizmaii.zazr.test.laws;

import com.guizmaii.zazr.Tuple2;
import com.guizmaii.zazr.collection.TreeMap;
import com.guizmaii.zazr.control.Option;
import com.guizmaii.zazr.test.Gen;


import java.util.function.Function;


/**
 * The laws of {@code TreeMap}.
 */
class TreeMapLawsTest extends MapLawsSuite<TreeMap<?, ?>, TreeMap<Integer, Integer>> {

    @Override
    MapSubject<TreeMap<?, ?>> mapValues() {
        return new MapSubject<>() {

            @Override
            public Gen<TreeMap<?, ?>> values() {
                return Gen.treeMap(Values.integers(), Values.integers()).map(value -> value);
            }

            @Override
            public TreeMap<?, ?> map(TreeMap<?, ?> fa, Function<Object, Object> f) {
                return fa.mapValues(f);
            }
        };
    }

    @Override
    CollectionSubject<Tuple2<Integer, Integer>, TreeMap<Integer, Integer>> collection() {
        return new CollectionSubject<>(Gen.treeMap(Values.integers(), Values.integers()), TreeMap::ofEntries, TreeMap::size,
                TreeMap::toList, false, Option.some(IterationOrder.keysSorted(java.util.Comparator.<Integer>naturalOrder())));
    }

    @Override
    BuilderLaws.CollectorSubject<Tuple2<Integer, Integer>, TreeMap<Integer, Integer>> collector() {
        return new BuilderLaws.CollectorSubject<>(Gen.list(Gen.tuple2(Values.integers(), Values.integers())),
                TreeMap.collector(), TreeMap::ofEntries, Option.some(IterationOrder.keysSorted(java.util.Comparator.<Integer>naturalOrder())));
    }
}
