package com.guizmaii.zazr.collection;

import java.util.function.IntFunction;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import static java.lang.Integer.signum;
import static org.assertj.core.api.Assertions.assertThat;

public class QuickSortTest {
    @Test
    public void shouldQuickSort() {
        final List<Integer> values = List.of(3, 1, 4, 1, 5, 9, 2, 6, 5, 3, 5, 8, 9, 7, 9, 3, 2, 3, 8, 4, 6, 2, 6);
        Assertions.assertThat(sort(values.shuffle())).isEqualTo(values.sorted());
        assertThat(sort2(values.shuffle())).isEqualTo(values.sorted());
    }

    /** Note: this example is only meant to show off, not to be used in reality: it can have quadratic performance and cause stack overflow */
    private static Seq<Integer> sort(Seq<Integer> values) {
        if (values.size() <= 1) return values;
        return values.tail().partition(v -> v <= values.head())
                     .apply((less, more) -> sort(less).append(values.head()).appendAll(sort(more)));
    }
    private static <T extends Comparable<T>> List<T> sort2(List<T> values) {
        if (values.size() <= 1) return values;
        final Map<Integer, List<T>> map = values.groupBy(v -> signum(v.compareTo(values.head())));
        final IntFunction<List<T>> parts =  signum -> map.get(signum).getOrElse(List.empty());
        return sort2(parts.apply(-1)).appendAll(parts.apply(0)).appendAll(sort2(parts.apply(1)));
    }
}
