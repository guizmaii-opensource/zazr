package dev.zazr.collection.internal;

import dev.zazr.collection.HashSet;
import dev.zazr.collection.List;
import dev.zazr.collection.Vector;
import java.util.AbstractList;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.concurrent.CopyOnWriteArrayList;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/// `RadixVector.ofOwnedArray`, which keeps an `Object[]` of up to 32 elements as the vector's one leaf, and
/// `Collections.toArrayIsOwned`, which tells which sized sources give `Vector.ofAll` an array that nothing else holds.
public class OwnedArrayTest {

    private record Element(int id) {}

    private static Object[] elements(int size) {
        Object[] elements = new Object[size];
        for (int i = 0; i < size; i++) {
            elements[i] = new Element(i);
        }
        return elements;
    }

    @Test
    public void anArrayOfUpTo32ElementsIsTheLeaf() {
        for (int size : new int[] {1, 2, 31, 32}) {
            Object[] array = elements(size);
            RadixVector<Object> vector = RadixVector.ofOwnedArray(array);
            assertThat(vector).as("size " + size).isInstanceOf(RadixVector.Vector1.class);
            assertThat(vector.vectorSlice(0)).as("size " + size).isSameAs(array);
        }
    }

    @Test
    public void aLongerArrayIsCopiedIntoLeaves() {
        for (int size : new int[] {33, 1024, 1025, 40000}) {
            Object[] array = elements(size);
            Object[] copy = array.clone();
            RadixVector<Object> vector = RadixVector.ofOwnedArray(array);
            RadixVector<Object> expected = RadixVector.ofAll(copy);
            assertThat(vector.getClass()).isEqualTo(expected.getClass());
            assertThat(vector.vectorSliceCount()).isEqualTo(expected.vectorSliceCount());
            for (int s = 0; s < vector.vectorSliceCount(); s++) {
                assertThat(vector.vectorSlice(s)).as("size " + size).isNotSameAs(array);
            }
            Arrays.fill(array, new Element(-1));
            for (int i = 0; i < size; i++) {
                assertThat(vector.get(i)).isSameAs(copy[i]);
            }
        }
    }

    @Test
    public void anArrayOfANarrowerClassIsCopiedIntoAnObjectArray() {
        Element[] array = {new Element(0), new Element(1)};
        RadixVector<Object> vector = RadixVector.ofOwnedArray(array);
        assertThat(vector.vectorSlice(0)).isNotSameAs(array);
        assertThat(vector.vectorSlice(0).getClass()).isEqualTo(Object[].class);
        assertThat(vector.updated(0, "another type").get(0)).isEqualTo("another type");
        assertThat(vector.appended(42).get(2)).isEqualTo(42);
        assertThat(vector.get(0)).isSameAs(array[0]);
    }

    @Test
    public void anEmptyArrayIsTheEmptyVector() {
        assertThat(RadixVector.ofOwnedArray(new Object[0])).isSameAs(RadixVector.empty());
    }

    @Test
    public void aNullElementIsRejected() {
        for (int size : new int[] {1, 32, 33}) {
            for (int at : new int[] {0, size - 1}) {
                Object[] array = elements(size);
                array[at] = null;
                assertThatThrownBy(() -> RadixVector.ofOwnedArray(array))
                        .as("size " + size + ", null at " + at)
                        .isInstanceOf(NullPointerException.class)
                        .hasMessage("Vector: element is null");
            }
        }
    }

    @Test
    public void theJdkCollectionsThatCopyAreKnownByTheirExactClass() {
        java.util.List<Integer> three = java.util.List.of(0, 1, 2);
        java.util.List<java.util.Collection<Integer>> copying = java.util.List.of(
                new ArrayList<>(three),
                new ArrayList<>(three).subList(0, 2),
                new LinkedList<>(three),
                new ArrayDeque<>(three),
                new java.util.HashSet<>(three),
                new java.util.LinkedHashSet<>(three),
                new java.util.TreeSet<>(three),
                Arrays.asList(0, 1, 2),
                java.util.List.of(0),
                java.util.List.of(0, 1),
                three,
                java.util.List.of(),
                three.subList(0, 2),
                java.util.Set.of(0),
                java.util.Set.of(0, 1),
                java.util.Set.of(0, 1, 2),
                java.util.Set.of());
        for (java.util.Collection<Integer> collection : copying) {
            assertThat(Collections.toArrayIsOwned(collection))
                    .as(collection.getClass().getName())
                    .isTrue();
        }
    }

    @Test
    public void anyOtherCollectionIsNot() {
        java.util.List<Integer> three = java.util.List.of(0, 1, 2);
        java.util.List<java.util.Collection<Integer>> others = java.util.List.of(
                new ArrayList<>(three) {},
                new AbstractList<Integer>() {
                    @Override
                    public Integer get(int index) {
                        return index;
                    }

                    @Override
                    public int size() {
                        return 3;
                    }
                },
                java.util.Collections.unmodifiableList(new ArrayList<>(three)),
                java.util.Collections.synchronizedList(new ArrayList<>(three)),
                java.util.Collections.checkedList(new ArrayList<>(three), Integer.class),
                new CopyOnWriteArrayList<>(three),
                new java.util.HashMap<>(java.util.Map.of(0, 1)).keySet(),
                new java.util.HashMap<>(java.util.Map.of(0, 1)).values(),
                List.of(0, 1, 2).asJava(),
                Vector.of(0, 1, 2).asJava(),
                HashSet.of(0, 1, 2).asJava());
        for (java.util.Collection<Integer> collection : others) {
            assertThat(Collections.toArrayIsOwned(collection))
                    .as(collection.getClass().getName())
                    .isFalse();
        }
    }

    @Test
    public void aZazrCollectionIsReadIntoAnArrayOfItsOwn() {
        assertThat(Collections.toArrayIsOwned(List.of(0, 1, 2))).isTrue();
        assertThat(Collections.toArrayIsOwned(HashSet.of(0, 1, 2))).isTrue();
        assertThat(Collections.toArrayIsOwned(Vector.of(0, 1, 2))).isTrue();
    }
}
