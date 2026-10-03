package dev.zazr.jackson;

import dev.zazr.collection.HashMap;
import dev.zazr.collection.HashSet;
import dev.zazr.collection.LinkedHashMap;
import dev.zazr.collection.LinkedHashSet;
import dev.zazr.collection.NonEmptySortedMap;
import dev.zazr.collection.NonEmptySortedSet;
import dev.zazr.collection.SortedMap;
import dev.zazr.collection.SortedSet;
import dev.zazr.collection.TreeMap;
import dev.zazr.collection.TreeSet;
import dev.zazr.collection.Vector;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.exc.InvalidDefinitionException;

import static dev.zazr.jackson.Json.read;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/// The sorted types use the natural order and need a `Comparable` element or key type; the linked types keep the
/// order of the JSON; duplicates follow the Zazr factories.
class OrderingTest {

    /// No natural order.
    record Point(int x, int y) {}

    @Test
    void sortedTypesUseTheNaturalOrderAndTheComparatorOfTheirEmptyValue() {
        var set = read("[3,1,2]", new TypeReference<TreeSet<Integer>>() {});
        assertThat(set).containsExactly(1, 2, 3);
        assertThat(set.comparator()).isEqualTo(TreeSet.<Integer>empty().comparator());

        var map = read("{\"b\":2,\"a\":1}", new TypeReference<TreeMap<String, Integer>>() {});
        assertThat(map.keySet()).containsExactly("a", "b");
        assertThat(map.comparator()).isEqualTo(TreeMap.<String, Integer>empty().comparator());

        var nonEmptySet = read("[\"b\",\"a\"]", new TypeReference<NonEmptySortedSet<String>>() {});
        assertThat(nonEmptySet).containsExactly("a", "b");
        assertThat(nonEmptySet.comparator()).isEqualTo(TreeSet.<String>empty().comparator());

        var nonEmptyMap = read("{\"2\":\"b\",\"1\":\"a\"}", new TypeReference<NonEmptySortedMap<Integer, String>>() {});
        assertThat(nonEmptyMap.keySet()).containsExactly(1, 2);
        assertThat(nonEmptyMap.comparator())
                .isEqualTo(TreeMap.<Integer, String>empty().comparator());
    }

    @Test
    void aSortedTypeOfANonComparableTypeFailsClearly() {
        assertThatThrownBy(() -> read("[]", new TypeReference<TreeSet<Point>>() {}))
                .isInstanceOf(InvalidDefinitionException.class)
                .hasMessageStartingWith(
                        "Cannot read a TreeSet of dev.zazr.jackson.OrderingTest$Point: it is read in the "
                                + "natural order of its elements, and dev.zazr.jackson.OrderingTest$Point does not implement "
                                + "Comparable");
        assertThatThrownBy(() -> read("[]", new TypeReference<SortedSet<Point>>() {}))
                .isInstanceOf(InvalidDefinitionException.class)
                .hasMessageStartingWith("Cannot read a SortedSet of dev.zazr.jackson.OrderingTest$Point");
        assertThatThrownBy(() -> read("[]", new TypeReference<NonEmptySortedSet<Point>>() {}))
                .isInstanceOf(InvalidDefinitionException.class)
                .hasMessageStartingWith("Cannot read a NonEmptySortedSet of dev.zazr.jackson.OrderingTest$Point");
        assertThatThrownBy(() -> read("{}", new TypeReference<TreeMap<Point, Integer>>() {}))
                .isInstanceOf(InvalidDefinitionException.class)
                .hasMessageStartingWith(
                        "Cannot read a TreeMap of dev.zazr.jackson.OrderingTest$Point: it is read in the "
                                + "natural order of its keys, and dev.zazr.jackson.OrderingTest$Point does not implement "
                                + "Comparable");
        assertThatThrownBy(() -> read("{}", new TypeReference<SortedMap<Point, Integer>>() {}))
                .isInstanceOf(InvalidDefinitionException.class)
                .hasMessageStartingWith("Cannot read a SortedMap of dev.zazr.jackson.OrderingTest$Point");
        assertThatThrownBy(() -> read("{}", new TypeReference<NonEmptySortedMap<Point, Integer>>() {}))
                .isInstanceOf(InvalidDefinitionException.class)
                .hasMessageStartingWith("Cannot read a NonEmptySortedMap of dev.zazr.jackson.OrderingTest$Point");
    }

    @Test
    void anUntypedSortedTypeFailsClearly() {
        assertThatThrownBy(() -> read("[1]", TreeSet.class))
                .isInstanceOf(InvalidDefinitionException.class)
                .hasMessageStartingWith("Cannot read a TreeSet of java.lang.Object: it is read in the natural order of "
                        + "its elements, and java.lang.Object does not implement Comparable");
    }

    @Test
    void linkedTypesKeepTheOrderOfTheJson() {
        var numbers = IntStream.range(0, 100).map(i -> (i * 37) % 100).boxed().toList();
        var array = numbers.stream().map(String::valueOf).collect(Collectors.joining(",", "[", "]"));
        assertThat(read(array, new TypeReference<LinkedHashSet<Integer>>() {})).containsExactlyElementsOf(numbers);
        var object = numbers.stream().map(i -> "\"k" + i + "\":" + i).collect(Collectors.joining(",", "{", "}"));
        var map = read(object, new TypeReference<LinkedHashMap<String, Integer>>() {});
        assertThat(map.values()).containsExactlyElementsOf(numbers);
        assertThat(Json.write(map)).isEqualTo(object);
        assertThat(Json.write(read(array, new TypeReference<LinkedHashSet<Integer>>() {})))
                .isEqualTo(array);
    }

    @Test
    void duplicates() {
        assertThat(read("[1,1,2]", new TypeReference<HashSet<Integer>>() {})).isEqualTo(HashSet.of(1, 2));
        assertThat(read("[2,1,2]", new TypeReference<LinkedHashSet<Integer>>() {}))
                .containsExactly(2, 1);
        assertThat(read("[2,1,2]", new TypeReference<Vector<Integer>>() {})).containsExactly(2, 1, 2);
        assertThat(read("{\"a\":1,\"a\":2}", new TypeReference<HashMap<String, Integer>>() {}))
                .isEqualTo(HashMap.of("a", 2));
        assertThat(read("{\"a\":1,\"b\":2,\"a\":3}", new TypeReference<LinkedHashMap<String, Integer>>() {}))
                .isEqualTo(LinkedHashMap.of("a", 3, "b", 2));
        assertThat(read("{\"a\":1,\"a\":2}", new TypeReference<TreeMap<String, Integer>>() {}))
                .isEqualTo(TreeMap.of("a", 2));
    }
}
