package dev.zazr.collection.internal;

import dev.zazr.collection.HashSet;
import dev.zazr.collection.List;
import dev.zazr.collection.Vector;
import java.util.AbstractList;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.PriorityQueue;
import java.util.concurrent.ConcurrentHashMap;
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
                java.util.Set.of(),
                new PriorityQueue<>(three),
                new CopyOnWriteArrayList<>(three),
                new CopyOnWriteArrayList<>(three).subList(0, 2),
                hashMap().keySet(),
                hashMap().values(),
                linkedHashMap().keySet(),
                linkedHashMap().values(),
                linkedHashMap().reversed().keySet(),
                linkedHashMap().reversed().values(),
                treeMap().keySet(),
                treeMap().descendingKeySet(),
                treeMap().headMap(2).keySet(),
                treeMap().values(),
                concurrentHashMap().keySet(),
                concurrentHashMap().keySet(7),
                ConcurrentHashMap.<Integer>newKeySet(),
                concurrentHashMap().values());
        for (java.util.Collection<Integer> collection : copying) {
            assertThat(Collections.toArrayIsOwned(collection))
                    .as(collection.getClass().getName())
                    .isTrue();
        }
        assertThat(Collections.toArrayIsOwned(concurrentHashMap().entrySet())).isTrue();
    }

    private static java.util.HashMap<Integer, Integer> hashMap() {
        return new java.util.HashMap<>(java.util.Map.of(0, 10, 1, 11, 2, 12));
    }

    private static java.util.LinkedHashMap<Integer, Integer> linkedHashMap() {
        return new java.util.LinkedHashMap<>(hashMap());
    }

    private static java.util.TreeMap<Integer, Integer> treeMap() {
        return new java.util.TreeMap<>(hashMap());
    }

    private static ConcurrentHashMap<Integer, Integer> concurrentHashMap() {
        return new ConcurrentHashMap<>(hashMap());
    }

    // the implementation classes whose toArray() was read in the JDK 25 sources
    private static void assertReadClass(Class<?> type, String name) {
        assertThat(type.getName())
                .as(
                        "this JDK gives %s where the JDK 25 gave %s: read the toArray() of %s, and trust it in"
                                + " Collections.copiesOnToArray only if it returns an Object[] allocated for the call",
                        type.getName(), name, type.getName())
                .isEqualTo(name);
    }

    @Test
    public void theTrustedImplementationClassesAreTheOnesWhoseSourcesWereRead() {
        assertReadClass(Collections.ARRAY_LIST_SUB_LIST, "java.util.ArrayList$SubList");
        assertReadClass(Collections.ARRAYS_AS_LIST, "java.util.Arrays$ArrayList");
        assertReadClass(Collections.LIST_12, "java.util.ImmutableCollections$List12");
        assertReadClass(Collections.LIST_N, "java.util.ImmutableCollections$ListN");
        assertReadClass(Collections.IMMUTABLE_SUB_LIST, "java.util.ImmutableCollections$SubList");
        assertReadClass(Collections.SET_12, "java.util.ImmutableCollections$Set12");
        assertReadClass(Collections.SET_N, "java.util.ImmutableCollections$SetN");
        assertReadClass(Collections.COW_SUB_LIST, "java.util.concurrent.CopyOnWriteArrayList$COWSubList");
        assertReadClass(Collections.HASH_MAP_KEY_SET, "java.util.HashMap$KeySet");
        assertReadClass(Collections.HASH_MAP_VALUES, "java.util.HashMap$Values");
        assertReadClass(Collections.LINKED_HASH_MAP_KEY_SET, "java.util.LinkedHashMap$LinkedKeySet");
        assertReadClass(Collections.LINKED_HASH_MAP_VALUES, "java.util.LinkedHashMap$LinkedValues");
        assertReadClass(Collections.TREE_MAP_KEY_SET, "java.util.TreeMap$KeySet");
        assertReadClass(Collections.TREE_MAP_VALUES, "java.util.TreeMap$Values");
        assertReadClass(Collections.CONCURRENT_HASH_MAP_VALUES, "java.util.concurrent.ConcurrentHashMap$ValuesView");
        assertReadClass(
                Collections.CONCURRENT_HASH_MAP_ENTRY_SET, "java.util.concurrent.ConcurrentHashMap$EntrySetView");
        // the factories that callers use give those classes, at every size the matching relies on
        java.util.List<Class<?>> trusted = java.util.List.of(
                Collections.ARRAY_LIST_SUB_LIST,
                Collections.ARRAYS_AS_LIST,
                Collections.LIST_12,
                Collections.LIST_N,
                Collections.IMMUTABLE_SUB_LIST,
                Collections.SET_12,
                Collections.SET_N,
                Collections.COW_SUB_LIST,
                Collections.HASH_MAP_KEY_SET,
                Collections.HASH_MAP_VALUES,
                Collections.LINKED_HASH_MAP_KEY_SET,
                Collections.LINKED_HASH_MAP_VALUES,
                Collections.TREE_MAP_KEY_SET,
                Collections.TREE_MAP_VALUES,
                ConcurrentHashMap.KeySetView.class,
                Collections.CONCURRENT_HASH_MAP_VALUES,
                Collections.CONCURRENT_HASH_MAP_ENTRY_SET);
        for (java.util.Collection<?> collection : java.util.List.<java.util.Collection<?>>of(
                java.util.List.of(),
                java.util.List.of(0, 1),
                java.util.List.of(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11),
                java.util.List.copyOf(new ArrayList<>(java.util.List.of(0, 1, 2))),
                java.util.List.of(0, 1, 2, 3).subList(1, 3),
                java.util.Set.of(),
                java.util.Set.of(0, 1),
                java.util.Set.of(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11),
                new CopyOnWriteArrayList<>(java.util.List.of(0, 1, 2)).subList(1, 2),
                new CopyOnWriteArrayList<>(java.util.List.of(0, 1, 2))
                        .subList(0, 2)
                        .subList(0, 1),
                hashMap().keySet(),
                hashMap().values(),
                new java.util.HashMap<Integer, Integer>() {}.keySet(),
                linkedHashMap().keySet(),
                linkedHashMap().values(),
                linkedHashMap().sequencedKeySet(),
                linkedHashMap().sequencedValues(),
                linkedHashMap().sequencedKeySet().reversed(),
                linkedHashMap().sequencedValues().reversed(),
                linkedHashMap().reversed().keySet(),
                linkedHashMap().reversed().values(),
                treeMap().keySet(),
                treeMap().navigableKeySet(),
                treeMap().descendingKeySet(),
                treeMap().descendingMap().keySet(),
                treeMap().headMap(2).keySet(),
                treeMap().tailMap(1).keySet(),
                treeMap().subMap(0, 2).keySet(),
                treeMap().values(),
                concurrentHashMap().keySet(),
                concurrentHashMap().keySet(7),
                ConcurrentHashMap.newKeySet(),
                concurrentHashMap().values(),
                concurrentHashMap().entrySet())) {
            assertThat(trusted).as(collection.getClass().getName()).contains(collection.getClass());
        }
    }

    @Test
    public void anyOtherCollectionIsNot() {
        java.util.List<Integer> three = java.util.List.of(0, 1, 2);
        java.util.List<java.util.Collection<?>> others = java.util.List.of(
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
                java.util.Collections.unmodifiableCollection(new ArrayList<>(three)),
                java.util.Collections.synchronizedSet(new java.util.HashSet<>(three)),
                new CopyOnWriteArrayList<>(three) {},
                new CopyOnWriteArrayList<>(three).reversed(),
                new PriorityQueue<>(three) {},
                hashMap().entrySet(),
                linkedHashMap().entrySet(),
                treeMap().entrySet(),
                treeMap().descendingMap().values(),
                treeMap().headMap(2).values(),
                treeMap().sequencedKeySet(),
                treeMap().sequencedValues(),
                List.of(0, 1, 2).asJava(),
                Vector.of(0, 1, 2).asJava(),
                HashSet.of(0, 1, 2).asJava());
        for (java.util.Collection<?> collection : others) {
            assertThat(Collections.toArrayIsOwned(collection))
                    .as(collection.getClass().getName())
                    .isFalse();
        }
    }

    @Test
    public void aSizedSourceIsReadIntoANewArrayOfItsElements() {
        List<Integer> list = List.of(0, 1, 2);
        Object[] first = Collections.toArrayOfSized(list);
        Object[] second = Collections.toArrayOfSized(list);
        assertThat(first).isNotSameAs(second).containsExactly(0, 1, 2);
        assertThat(first.getClass()).isEqualTo(Object[].class);
        assertThat(Collections.toArrayOfSized(HashSet.of(5))).containsExactly(5);
        assertThat(Collections.toArrayOfSized(List.empty())).isEmpty();
        assertThat(Collections.toArrayOfSized(new ArrayList<>(java.util.List.of(3, 4))))
                .containsExactly(3, 4);
    }

    @Test
    public void aZazrCollectionIsReadIntoAnArrayOfItsOwn() {
        assertThat(Collections.toArrayIsOwned(List.of(0, 1, 2))).isTrue();
        assertThat(Collections.toArrayIsOwned(HashSet.of(0, 1, 2))).isTrue();
        assertThat(Collections.toArrayIsOwned(Vector.of(0, 1, 2))).isTrue();
    }
}
