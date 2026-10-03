package dev.zazr.jackson;

import dev.zazr.Tuple;
import dev.zazr.collection.HashMap;
import dev.zazr.collection.HashSet;
import dev.zazr.collection.LazyList;
import dev.zazr.collection.LinkedHashMap;
import dev.zazr.collection.LinkedHashSet;
import dev.zazr.collection.List;
import dev.zazr.collection.NonEmptyVector;
import dev.zazr.collection.Queue;
import dev.zazr.collection.TreeMap;
import dev.zazr.collection.TreeSet;
import dev.zazr.collection.Vector;
import java.util.stream.Collectors;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import tools.jackson.core.type.TypeReference;

import static dev.zazr.jackson.Json.read;
import static dev.zazr.jackson.Json.write;
import static org.assertj.core.api.Assertions.assertThat;

/// Empty, one element, the boundaries of the tries (32, 1024) and a large collection: written as the JDK writes the
/// same elements, and read back equal.
class SizesTest {

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2, 31, 32, 33, 1023, 1024, 1025, 100_000})
    void sequencesAndSets(int size) {
        var vector = Vector.range(0, size);
        var json = vector.stream().map(String::valueOf).collect(Collectors.joining(",", "[", "]"));
        assertThat(write(vector)).isEqualTo(json);
        assertThat(read(json, new TypeReference<Vector<Integer>>() {})).isEqualTo(vector);
        assertThat(read(json, new TypeReference<List<Integer>>() {})).isEqualTo(List.ofAll(vector));
        assertThat(read(json, new TypeReference<Queue<Integer>>() {})).isEqualTo(Queue.ofAll(vector));
        assertThat(read(json, new TypeReference<LazyList<Integer>>() {})).isEqualTo(LazyList.ofAll(vector));
        assertThat(write(LazyList.ofAll(vector))).isEqualTo(json);
        assertThat(read(json, new TypeReference<HashSet<Integer>>() {})).isEqualTo(HashSet.ofAll(vector));
        assertThat(read(json, new TypeReference<LinkedHashSet<Integer>>() {})).isEqualTo(LinkedHashSet.ofAll(vector));
        assertThat(write(LinkedHashSet.ofAll(vector))).isEqualTo(json);
        assertThat(read(json, new TypeReference<TreeSet<Integer>>() {})).isEqualTo(TreeSet.ofAll(vector));
        assertThat(write(TreeSet.ofAll(vector))).isEqualTo(json);
        if (size > 0) {
            assertThat(read(json, new TypeReference<NonEmptyVector<Integer>>() {}))
                    .isEqualTo(NonEmptyVector.fromIterable(vector).get());
        }
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2, 31, 32, 33, 1023, 1024, 1025, 100_000})
    void maps(int size) {
        var entries = Vector.range(0, size).map(i -> Tuple.of(i, "v" + i));
        var json = entries.stream()
                .map(e -> "\"" + e._1() + "\":\"" + e._2() + "\"")
                .collect(Collectors.joining(",", "{", "}"));
        var linked = LinkedHashMap.ofEntries(entries);
        assertThat(write(linked)).isEqualTo(json);
        assertThat(read(json, new TypeReference<LinkedHashMap<Integer, String>>() {}))
                .isEqualTo(linked);
        assertThat(read(json, new TypeReference<HashMap<Integer, String>>() {})).isEqualTo(HashMap.ofEntries(entries));
        assertThat(read(json, new TypeReference<TreeMap<Integer, String>>() {})).isEqualTo(TreeMap.ofEntries(entries));
        assertThat(write(TreeMap.ofEntries(entries))).isEqualTo(json);
        assertThat(read(write(HashMap.ofEntries(entries)), new TypeReference<HashMap<Integer, String>>() {}))
                .isEqualTo(HashMap.ofEntries(entries));
    }
}
