package com.guizmaii.zazr;

import com.guizmaii.zazr.collection.List;
import com.guizmaii.zazr.collection.TreeMap;
import com.guizmaii.zazr.collection.TreeSet;
import com.guizmaii.zazr.control.Option;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ValueTest {

    @Test
    public void shouldNarrowValue() {
        final Value<Double> doubles = List.of(1.0d);
        final Value<Number> numbers = Value.narrow(doubles);
        assertThat(numbers.get()).isEqualTo(1.0d);
    }

    @Test
    public void collectWorkAsExpectedMultiValue() {
        final Value<Double> doubles = List.of(1.0d, 2.0d);
        final java.util.List<Double> result = doubles.collect(Collectors.toList());
        assertThat(result).contains(1.0d, 2.0d);
    }

    @Test
    public void verboseCollectWorkAsExpectedMultiValue() {
        final Value<Double> doubles = List.of(1.0d, 2.0d);
        final java.util.List<Double> result = doubles.collect(ArrayList<Double>::new, ArrayList::add, ArrayList::addAll);
        assertThat(result).contains(1.0d, 2.0d);
    }

    @Test
    public void collectWorkAsExpectedSingleValue() {
        final Value<Double> doubles = Option.some(1.0d);
        assertThat(doubles.collect(Collectors.toList()).get(0)).isEqualTo(1.0d);
    }

    @Test
    public void verboseCollectWorkAsExpectedSingleValue() {
        final Value<Double> doubles = Option.some(1.0d);
        assertThat(doubles.collect(ArrayList<Double>::new,
                ArrayList::add, ArrayList::addAll).get(0)).isEqualTo(1.0d);
    }

    // -- toSortedSet() on key-ordered maps (Ordered<K> but Value<Tuple2<K, V>>)

    @Test
    public void shouldConvertSortedMapWithKeyComparatorToSortedSetUsingNaturalOrderOfEntries() {
        final Comparator<Integer> keyComparator = Comparator.comparingInt(Integer::intValue); // not applicable to Tuple2
        final Value<Tuple2<Integer, String>> map = TreeMap.of(keyComparator.reversed(), 1, "a", 2, "b");
        // Set now implements Predicate<T> (docs/design.md 3.1), so assertThat(Iterable) vs assertThat(Predicate)
        // is ambiguous without a type witness.
        assertThat((Iterable<Tuple2<Integer, String>>) map.toSortedSet()).isEqualTo(TreeSet.of(Tuple.of(1, "a"), Tuple.of(2, "b")));
    }
}
