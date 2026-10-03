package dev.zazr.jackson;

import dev.zazr.Tuple;
import dev.zazr.Tuple1;
import dev.zazr.Tuple2;
import dev.zazr.Tuple3;
import dev.zazr.Tuple4;
import dev.zazr.Tuple5;
import dev.zazr.Tuple6;
import dev.zazr.Tuple7;
import dev.zazr.Tuple8;
import dev.zazr.collection.HashMap;
import dev.zazr.collection.HashSet;
import dev.zazr.collection.LazyList;
import dev.zazr.collection.LinkedHashMap;
import dev.zazr.collection.LinkedHashSet;
import dev.zazr.collection.List;
import dev.zazr.collection.NonEmptyMap;
import dev.zazr.collection.NonEmptySet;
import dev.zazr.collection.NonEmptySortedMap;
import dev.zazr.collection.NonEmptySortedSet;
import dev.zazr.collection.NonEmptyVector;
import dev.zazr.collection.Queue;
import dev.zazr.collection.TreeMap;
import dev.zazr.collection.TreeSet;
import dev.zazr.collection.Vector;
import dev.zazr.control.Option;
import org.junit.jupiter.api.Test;
import tools.jackson.core.type.TypeReference;

import static dev.zazr.jackson.Json.read;
import static dev.zazr.jackson.Json.write;
import static org.assertj.core.api.Assertions.assertThat;

/// Every type the module covers, written and read back: the JSON it writes, and the value it reads from that JSON.
class RoundTripTest {

    @Test
    void vector() {
        var value = Vector.of(1, 2, 3);
        assertThat(write(value)).isEqualTo("[1,2,3]");
        assertThat(read("[1,2,3]", new TypeReference<Vector<Integer>>() {})).isEqualTo(value);
    }

    @Test
    void list() {
        var value = List.of("a", "b");
        assertThat(write(value)).isEqualTo("[\"a\",\"b\"]");
        assertThat(read("[\"a\",\"b\"]", new TypeReference<List<String>>() {})).isEqualTo(value);
    }

    @Test
    void queue() {
        var value = Queue.of(1, 2).enqueue(3);
        assertThat(write(value)).isEqualTo("[1,2,3]");
        assertThat(read("[1,2,3]", new TypeReference<Queue<Integer>>() {})).isEqualTo(value);
    }

    @Test
    void lazyList() {
        var value = LazyList.from(1).take(3);
        assertThat(write(value)).isEqualTo("[1,2,3]");
        assertThat(read("[1,2,3]", new TypeReference<LazyList<Integer>>() {})).isEqualTo(value);
    }

    @Test
    void hashSet() {
        var value = HashSet.of(1, 2, 3);
        var json = write(value);
        assertThat(read(json, new TypeReference<java.util.Set<Integer>>() {})).containsExactlyInAnyOrder(1, 2, 3);
        assertThat(read(json, new TypeReference<HashSet<Integer>>() {})).isEqualTo(value);
    }

    @Test
    void linkedHashSet() {
        var value = LinkedHashSet.of(3, 1, 2);
        assertThat(write(value)).isEqualTo("[3,1,2]");
        assertThat(read("[3,1,2]", new TypeReference<LinkedHashSet<Integer>>() {}))
                .isEqualTo(value);
    }

    @Test
    void treeSet() {
        var value = TreeSet.of(3, 1, 2);
        assertThat(write(value)).isEqualTo("[1,2,3]");
        assertThat(read("[3,1,2]", new TypeReference<TreeSet<Integer>>() {})).isEqualTo(value);
    }

    @Test
    void hashMap() {
        var value = HashMap.of("a", 1, "b", 2);
        var json = write(value);
        assertThat(read(json, new TypeReference<java.util.Map<String, Integer>>() {}))
                .isEqualTo(java.util.Map.of("a", 1, "b", 2));
        assertThat(read(json, new TypeReference<HashMap<String, Integer>>() {})).isEqualTo(value);
    }

    @Test
    void linkedHashMap() {
        var value = LinkedHashMap.of("b", 2, "a", 1);
        assertThat(write(value)).isEqualTo("{\"b\":2,\"a\":1}");
        assertThat(read("{\"b\":2,\"a\":1}", new TypeReference<LinkedHashMap<String, Integer>>() {}))
                .isEqualTo(value);
    }

    @Test
    void treeMap() {
        var value = TreeMap.of("b", 2, "a", 1);
        assertThat(write(value)).isEqualTo("{\"a\":1,\"b\":2}");
        assertThat(read("{\"b\":2,\"a\":1}", new TypeReference<TreeMap<String, Integer>>() {}))
                .isEqualTo(value);
    }

    @Test
    void option() {
        assertThat(write(Option.some(1))).isEqualTo("1");
        assertThat(write(Option.none())).isEqualTo("null");
        assertThat(read("1", new TypeReference<Option<Integer>>() {})).isEqualTo(Option.some(1));
        assertThat(read("null", new TypeReference<Option<Integer>>() {})).isEqualTo(Option.none());
    }

    @Test
    void nonEmptyVector() {
        var value = NonEmptyVector.of(1, 2);
        assertThat(write(value)).isEqualTo("[1,2]");
        assertThat(read("[1,2]", new TypeReference<NonEmptyVector<Integer>>() {}))
                .isEqualTo(value);
    }

    @Test
    void nonEmptySet() {
        var value = NonEmptySet.of(1, 2);
        var json = write(value);
        assertThat(read(json, new TypeReference<java.util.Set<Integer>>() {})).containsExactlyInAnyOrder(1, 2);
        assertThat(read(json, new TypeReference<NonEmptySet<Integer>>() {})).isEqualTo(value);
    }

    @Test
    void nonEmptySortedSet() {
        var value = NonEmptySortedSet.of(2, 1);
        assertThat(write(value)).isEqualTo("[1,2]");
        assertThat(read("[2,1]", new TypeReference<NonEmptySortedSet<Integer>>() {}))
                .isEqualTo(value);
    }

    @Test
    void nonEmptyMap() {
        var value = NonEmptyMap.of(Tuple.of("a", 1), Tuple.of("b", 2));
        var json = write(value);
        assertThat(read(json, new TypeReference<java.util.Map<String, Integer>>() {}))
                .isEqualTo(java.util.Map.of("a", 1, "b", 2));
        assertThat(read(json, new TypeReference<NonEmptyMap<String, Integer>>() {}))
                .isEqualTo(value);
    }

    @Test
    void nonEmptySortedMap() {
        var value = NonEmptySortedMap.of(Tuple.of("b", 2), Tuple.of("a", 1));
        assertThat(write(value)).isEqualTo("{\"a\":1,\"b\":2}");
        assertThat(read("{\"b\":2,\"a\":1}", new TypeReference<NonEmptySortedMap<String, Integer>>() {}))
                .isEqualTo(value);
    }

    @Test
    void tuples() {
        assertThat(write(Tuple.of(1))).isEqualTo("[1]");
        assertThat(read(write(Tuple.of(1, null)), new TypeReference<Tuple2<Integer, String>>() {}))
                .isEqualTo(Tuple.of(1, null));
        assertThat(read("[1]", new TypeReference<Tuple1<Integer>>() {})).isEqualTo(Tuple.of(1));

        assertThat(write(Tuple.of(1, "a"))).isEqualTo("[1,\"a\"]");
        assertThat(read("[1,\"a\"]", new TypeReference<Tuple2<Integer, String>>() {}))
                .isEqualTo(Tuple.of(1, "a"));

        var t3 = Tuple.of(1, "a", true);
        assertThat(read(write(t3), new TypeReference<Tuple3<Integer, String, Boolean>>() {}))
                .isEqualTo(t3);

        var t4 = Tuple.of(1, "a", true, 2L);
        assertThat(read(write(t4), new TypeReference<Tuple4<Integer, String, Boolean, Long>>() {}))
                .isEqualTo(t4);

        var t5 = Tuple.of(1, "a", true, 2L, 3.5);
        assertThat(read(write(t5), new TypeReference<Tuple5<Integer, String, Boolean, Long, Double>>() {}))
                .isEqualTo(t5);

        var t6 = Tuple.of(1, "a", true, 2L, 3.5, 'c');
        assertThat(read(write(t6), new TypeReference<Tuple6<Integer, String, Boolean, Long, Double, Character>>() {}))
                .isEqualTo(t6);

        var t7 = Tuple.of(1, "a", true, 2L, 3.5, 'c', Vector.of(1));
        assertThat(read(
                        write(t7),
                        new TypeReference<
                                Tuple7<Integer, String, Boolean, Long, Double, Character, Vector<Integer>>>() {}))
                .isEqualTo(t7);

        var t8 = Tuple.of(1, "a", true, 2L, 3.5, 'c', Vector.of(1), Option.some("x"));
        assertThat(write(t8)).isEqualTo("[1,\"a\",true,2,3.5,\"c\",[1],\"x\"]");
        assertThat(read(
                        write(t8),
                        new TypeReference<
                                Tuple8<
                                        Integer,
                                        String,
                                        Boolean,
                                        Long,
                                        Double,
                                        Character,
                                        Vector<Integer>,
                                        Option<String>>>() {}))
                .isEqualTo(t8);
    }
}
