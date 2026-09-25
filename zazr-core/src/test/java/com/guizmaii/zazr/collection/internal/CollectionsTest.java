package com.guizmaii.zazr.collection.internal;

import com.guizmaii.zazr.Tuple;
import com.guizmaii.zazr.collection.HashMap;
import com.guizmaii.zazr.collection.HashSet;
import com.guizmaii.zazr.collection.LinkedHashMap;
import com.guizmaii.zazr.collection.LinkedHashSet;
import com.guizmaii.zazr.collection.List;
import com.guizmaii.zazr.collection.Queue;
import com.guizmaii.zazr.collection.Stream;
import com.guizmaii.zazr.collection.Traversable;
import com.guizmaii.zazr.collection.TreeMap;
import com.guizmaii.zazr.collection.TreeSet;
import com.guizmaii.zazr.collection.Vector;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class CollectionsTest {

    // -- last(): the shared implementation names the empty source in its message

    @Test
    public void shouldNameTheEmptySourceWhenLastOfEmptyThrows() {
        assertThatThrownBy(() -> List.empty().last()).isInstanceOf(java.util.NoSuchElementException.class).hasMessage("last of empty List()");
        assertThatThrownBy(() -> Stream.empty().last()).isInstanceOf(java.util.NoSuchElementException.class).hasMessage("last of empty Stream()");
    }

    @Test
    public void shouldBeEqualSets() throws Exception {
        forAll(List.of(TreeSet.ofAll(1, 2, 3),
                HashSet.ofAll(1, 2, 3),
                LinkedHashSet.ofAll(1, 2, 3)), true);
    }

    @Test
    public void shouldNotBeEqualSets() throws Exception {
        forAll(List.of(HashSet.ofAll(1, 2, 3),
                HashSet.ofAll('a', 'b', 'c')), false);
    }

    @Test
    public void shouldBeEqualSeqs() throws Exception {
        forAll(List.of(Vector.ofAll(1, 2, 3),
                Stream.ofAll(1, 2, 3),
                List.ofAll(1, 2, 3),
                Queue.ofAll(1, 2, 3)), true);
    }

    @Test
    public void shouldNotBeEqualSeqs() throws Exception {
        forAll(List.of(Vector.ofAll(1, 2, 3),
                Vector.ofAll('a', 'b', 'c')), false);
    }

    @Test
    public void shouldBeEqualMaps() throws Exception {
        forAll(List.of(TreeMap.of(1, 2, 2, 3, 3, 4),
                HashMap.of(1, 2, 2, 3, 3, 4),
                LinkedHashMap.of(1, 2, 2, 3, 3, 4)), true);
    }

    @Test
    public void shouldNotBeEqualMaps() throws Exception {
        forAll(List.of(HashMap.of(1, 2, 2, 3, 3, 4),
                HashMap.of('a', 'b', 'c', 'd', 'e', 'f')), false);
    }

    @Test
    public void shouldNotBeEqualSeqAndSet() throws Exception {
        forAll(List.of(Vector.ofAll(1, 2, 3),
                TreeSet.ofAll(1, 2, 3)), false);
    }

    @Test
    public void shouldNotBeEqualMapAndSet() throws Exception {
        forAll(List.of(HashSet.of(Tuple.of(1, 2), Tuple.of(2, 3)),
                TreeMap.of(1, 2, 2, 3)), false);
    }

    private void forAll(List<Traversable<?>> traversables, boolean value) {
        for (Traversable<?> traversable1 : traversables) {
            for (Traversable<?> traversable2 : traversables) {
                if (traversable1 != traversable2) {
                    assertThat(traversable1.equals(traversable2)).isEqualTo(value);
                    if (value) {
                        assertThat(traversable1.hashCode() == traversable2.hashCode()).isTrue();
                    }
                }
            }
        }
    }

}
