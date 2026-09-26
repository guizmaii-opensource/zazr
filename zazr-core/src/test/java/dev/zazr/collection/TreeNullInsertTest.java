package dev.zazr.collection;

import dev.zazr.Tuple;
import java.util.Comparator;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatNullPointerException;

/**
 * Every insertion path of the tree collections rejects {@code null} before the comparator is consulted, so a
 * comparator that tolerates {@code null} (here one that finds every pair equal, so {@code null} looks present) cannot
 * turn an insertion of {@code null} into a no-op.
 */
public class TreeNullInsertTest {

    private static final Comparator<Integer> ALL_EQUAL = (a, b) -> 0;

    @Test
    public void shouldRejectANullElementOnEveryTreeSetInsertion() {
        for (TreeSet<Integer> set : java.util.List.of(TreeSet.empty(ALL_EQUAL), TreeSet.of(ALL_EQUAL, 1))) {
            assertThatNullPointerException().isThrownBy(() -> set.add(null)).withMessage("TreeSet: element is null");
            assertThatNullPointerException().isThrownBy(() -> set.addAll(java.util.Arrays.asList(1, null))).withMessage("TreeSet: element is null");
            assertThatNullPointerException().isThrownBy(() -> set.addAll(java.util.Arrays.asList((Integer) null))).withMessage("TreeSet: element is null");
            assertThatNullPointerException().isThrownBy(() -> set.replace(1, null)).withMessage("TreeSet: element is null");
            assertThatNullPointerException().isThrownBy(() -> set.replace(2, null)).withMessage("TreeSet: element is null");
        }
    }

    @Test
    public void shouldRejectANullKeyOrValueOnEveryTreeMapInsertion() {
        for (TreeMap<Integer, String> map : java.util.List.of(TreeMap.<Integer, String> empty(ALL_EQUAL), TreeMap.of(ALL_EQUAL, 1, "a"))) {
            assertThatNullPointerException().isThrownBy(() -> map.put(null, "b")).withMessage("TreeMap: key is null");
            assertThatNullPointerException().isThrownBy(() -> map.put(1, null)).withMessage("TreeMap: value is null");
            assertThatNullPointerException().isThrownBy(() -> map.put(Tuple.of(null, "b"))).withMessage("TreeMap: key is null");
            assertThatNullPointerException().isThrownBy(() -> map.put(Tuple.of(1, (String) null))).withMessage("TreeMap: value is null");
        }
    }
}
