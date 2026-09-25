package com.guizmaii.zazr.collection;

import com.guizmaii.zazr.Tuple;
import java.util.Comparator;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * An {@code ofAll} factory handed the view of a value of its own type returns that value, not a copy; handed a view
 * that shows something else (a reversed or descending view, a sub-view, a key set) or the view of another type or
 * comparator, it copies what the view shows.
 */
class JavaViewOfAllTest {

    @Test
    void shouldUnwrapTheListViewOfEverySequence() {
        final Vector<Integer> vector = Vector.of(1, 2, 3);
        final List<Integer> list = List.of(1, 2, 3);
        final Queue<Integer> queue = Queue.of(1, 2, 3);
        final Stream<Integer> stream = Stream.of(1, 2, 3);
        assertThat(Vector.ofAll(vector.asJava())).isSameAs(vector);
        assertThat(List.ofAll(list.asJava())).isSameAs(list);
        assertThat(Queue.ofAll(queue.asJava())).isSameAs(queue);
        assertThat(Stream.ofAll(stream.asJava())).isSameAs(stream);
        final NonEmptyVector<Integer> nonEmpty = NonEmptyVector.of(1, 2, 3);
        assertThat(Vector.ofAll(nonEmpty.asJava())).isSameAs(nonEmpty.toVector());
    }

    @Test
    void shouldCopyWhatAReversedListViewShows() {
        final Vector<Integer> vector = Vector.of(1, 2, 3);
        assertThat(Vector.ofAll(vector.asJava().reversed())).isEqualTo(Vector.of(3, 2, 1));
        assertThat(List.ofAll(List.of(1, 2, 3).asJava().reversed())).isEqualTo(List.of(3, 2, 1));
        assertThat(Queue.ofAll(Queue.of(1, 2, 3).asJava().reversed())).isEqualTo(Queue.of(3, 2, 1));
        assertThat(Stream.ofAll(Stream.of(1, 2, 3).asJava().reversed())).isEqualTo(Stream.of(3, 2, 1));
        assertThat(Vector.ofAll(vector.asJava().reversed().reversed())).isSameAs(vector);
    }

    @Test
    void shouldUnwrapASubListToTheSubSequenceItShows() {
        assertThat(Vector.ofAll(Vector.of(1, 2, 3, 4).asJava().subList(1, 3))).isEqualTo(Vector.of(2, 3));
        assertThat(List.ofAll(List.of(1, 2, 3, 4).asJava().subList(1, 3))).isEqualTo(List.of(2, 3));
    }

    @Test
    void shouldCopyTheViewOfAnotherSequenceType() {
        assertThat(Vector.ofAll(List.of(1, 2, 3).asJava())).isEqualTo(Vector.of(1, 2, 3));
        assertThat(List.ofAll(Vector.of(1, 2, 3).asJava())).isEqualTo(List.of(1, 2, 3));
        assertThat(Vector.ofAll(HashSet.of(1).asJava())).isEqualTo(Vector.of(1));
    }

    @Test
    void shouldUnwrapTheSetViews() {
        final HashSet<Integer> hashSet = HashSet.of(1, 2, 3);
        final LinkedHashSet<Integer> linkedHashSet = LinkedHashSet.of(3, 1, 2);
        final TreeSet<Integer> natural = TreeSet.of(1, 2, 3);
        final TreeSet<Integer> reversed = TreeSet.of(Comparator.reverseOrder(), 1, 2, 3);
        assertThat(HashSet.ofAll(hashSet.asJava())).isSameAs(hashSet);
        assertThat(LinkedHashSet.ofAll(linkedHashSet.asJava())).isSameAs(linkedHashSet);
        assertThat(TreeSet.ofAll(natural.asJava())).isSameAs(natural);
        assertThat(TreeSet.ofAll(reversed.comparator(), reversed.asJava())).isSameAs(reversed);
    }

    @Test
    void shouldCopyWhatAnotherSetViewShows() {
        final LinkedHashSet<Integer> linkedHashSet = LinkedHashSet.of(3, 1, 2);
        final LinkedHashSet<Integer> reversedCopy = LinkedHashSet.ofAll(linkedHashSet.asJava().reversed());
        assertThat(reversedCopy).isNotSameAs(linkedHashSet);
        assertThat(reversedCopy.toVector()).isEqualTo(Vector.of(2, 1, 3));
        final TreeSet<Integer> natural = TreeSet.of(1, 2, 3, 4);
        assertThat(TreeSet.ofAll(natural.asJava().headSet(3))).isEqualTo(TreeSet.of(1, 2));
        assertThat(TreeSet.ofAll(natural.asJava().descendingSet())).isNotSameAs(natural).isEqualTo(natural);
        final TreeSet<Integer> reordered = TreeSet.ofAll(Comparator.reverseOrder(), natural.asJava());
        assertThat(reordered).isNotSameAs(natural);
        assertThat(reordered.toVector()).isEqualTo(Vector.of(4, 3, 2, 1));
        assertThat(TreeSet.ofAll(TreeSet.of(Comparator.<Integer> reverseOrder(), 1, 2).asJava())).isEqualTo(TreeSet.of(1, 2));
        assertThat(HashSet.ofAll(natural.asJava())).isEqualTo(HashSet.of(1, 2, 3, 4));
        assertThat(LinkedHashSet.ofAll(HashSet.of(1).asJava())).isEqualTo(LinkedHashSet.of(1));
    }

    @Test
    void shouldUnwrapTheMapViews() {
        final HashMap<Integer, String> hashMap = HashMap.of(1, "a", 2, "b");
        final LinkedHashMap<Integer, String> linkedHashMap = LinkedHashMap.of(2, "b", 1, "a");
        final TreeMap<Integer, String> natural = TreeMap.of(1, "a", 2, "b");
        final TreeMap<Integer, String> reversed = TreeMap.of(Comparator.reverseOrder(), 1, "a", 2, "b");
        assertThat(HashMap.ofAll(hashMap.asJavaMap())).isSameAs(hashMap);
        assertThat(LinkedHashMap.ofAll(linkedHashMap.asJavaMap())).isSameAs(linkedHashMap);
        assertThat(TreeMap.ofAll(natural.asJavaMap())).isSameAs(natural);
        assertThat(TreeMap.ofAll(reversed.comparator(), reversed.asJavaMap())).isSameAs(reversed);
        assertThat(HashMap.ofEntries(hashMap.asJava())).isSameAs(hashMap);
        assertThat(LinkedHashMap.ofEntries(linkedHashMap.asJava())).isSameAs(linkedHashMap);
    }

    @Test
    void shouldCopyWhatAnotherMapViewShows() {
        final LinkedHashMap<Integer, String> linkedHashMap = LinkedHashMap.of(2, "b", 1, "a");
        final LinkedHashMap<Integer, String> reversedCopy = LinkedHashMap.ofAll(linkedHashMap.asJavaMap().reversed());
        assertThat(reversedCopy).isNotSameAs(linkedHashMap);
        assertThat(reversedCopy.keySet().toVector()).isEqualTo(Vector.of(1, 2));
        final TreeMap<Integer, String> natural = TreeMap.of(1, "a", 2, "b", 3, "c");
        assertThat(TreeMap.ofAll(natural.asJavaMap().headMap(3))).isEqualTo(TreeMap.of(1, "a", 2, "b"));
        assertThat(TreeMap.ofAll(natural.asJavaMap().descendingMap())).isNotSameAs(natural).isEqualTo(natural);
        final TreeMap<Integer, String> reordered = TreeMap.ofAll(Comparator.reverseOrder(), natural.asJavaMap());
        assertThat(reordered).isNotSameAs(natural);
        assertThat(reordered.keySet().toVector()).isEqualTo(Vector.of(3, 2, 1));
        assertThat(HashMap.ofAll(natural.asJavaMap())).isEqualTo(HashMap.of(1, "a", 2, "b", 3, "c"));
        assertThat(LinkedHashMap.ofAll(HashMap.of(1, "a").asJavaMap())).isEqualTo(LinkedHashMap.of(1, "a"));
        assertThat(HashMap.ofEntries(TreeMap.of(1, "a").asJava())).isEqualTo(HashMap.of(1, "a"));
        assertThat(HashMap.ofEntries(java.util.List.of(Tuple.of(1, "a")))).isEqualTo(HashMap.of(1, "a"));
    }
}
