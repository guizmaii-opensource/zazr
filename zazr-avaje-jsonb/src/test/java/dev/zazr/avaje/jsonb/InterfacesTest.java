package dev.zazr.avaje.jsonb;

import dev.zazr.Tuple;
import dev.zazr.avaje.jsonb.TestRecords.Anything;
import dev.zazr.avaje.jsonb.TestRecords.Interfaces;
import dev.zazr.collection.HashMap;
import dev.zazr.collection.HashSet;
import dev.zazr.collection.LinkedHashMap;
import dev.zazr.collection.LinkedHashSet;
import dev.zazr.collection.Map;
import dev.zazr.collection.NonEmptySortedMap;
import dev.zazr.collection.Set;
import dev.zazr.collection.SortedMap;
import dev.zazr.collection.SortedSet;
import dev.zazr.collection.Traversable;
import dev.zazr.collection.TreeMap;
import dev.zazr.collection.TreeSet;
import dev.zazr.collection.Vector;
import dev.zazr.control.Option;
import io.avaje.json.JsonDataException;
import io.avaje.jsonb.JsonType;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

import static dev.zazr.avaje.jsonb.JsonTypes.JSONB;
import static dev.zazr.avaje.jsonb.JsonTypes.json;
import static dev.zazr.avaje.jsonb.JsonTypes.type;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/// Properties declared as `Set`, `SortedSet`, `Map`, `SortedMap` and `Traversable`: written by iterating whatever
/// implementation they hold, and read as `HashSet`, `TreeSet`, `HashMap` and `TreeMap`; `Traversable` is not read.
class InterfacesTest {

    @Test
    void eachInterfaceIsReadAsItsDefaultImplementation() {
        JsonType<Set<Integer>> set = json(Set.class, Integer.class);
        assertThat(set.fromJson("[3,1,3]")).isExactlyInstanceOf(HashSet.class).isEqualTo(HashSet.of(1, 3));
        JsonType<SortedSet<Integer>> sortedSet = json(SortedSet.class, Integer.class);
        assertThat(sortedSet.fromJson("[3,1,3]"))
                .isExactlyInstanceOf(TreeSet.class)
                .isEqualTo(TreeSet.of(1, 3));
        JsonType<Map<String, Integer>> map = json(Map.class, String.class, Integer.class);
        assertThat(map.fromJson("{\"a\":1}")).isExactlyInstanceOf(HashMap.class).isEqualTo(HashMap.of("a", 1));
        JsonType<SortedMap<Integer, String>> sortedMap = json(SortedMap.class, Integer.class, String.class);
        assertThat(sortedMap.fromJson("{\"10\":\"b\",\"2\":\"a\"}"))
                .isExactlyInstanceOf(TreeMap.class)
                .isEqualTo(TreeMap.of(2, "a", 10, "b"));
    }

    @Test
    void eachInterfaceWritesWhateverImplementationItHolds() {
        JsonType<Set<Integer>> set = json(Set.class, Integer.class);
        assertThat(set.toJson(LinkedHashSet.of(3, 1, 2))).isEqualTo("[3,1,2]");
        assertThat(set.toJson(TreeSet.of(3, 1, 2))).isEqualTo("[1,2,3]");
        assertThat(set.toJson(HashSet.empty())).isEqualTo("[]");
        JsonType<SortedSet<Integer>> sortedSet = json(SortedSet.class, Integer.class);
        assertThat(sortedSet.toJson(TreeSet.of(3, 1))).isEqualTo("[1,3]");
        JsonType<Map<LocalDate, Integer>> map = json(Map.class, LocalDate.class, Integer.class);
        assertThat(map.toJson(LinkedHashMap.of(LocalDate.of(2026, 10, 3), 1, LocalDate.of(2025, 1, 1), 2)))
                .isEqualTo("{\"2026-10-03\":1,\"2025-01-01\":2}");
        assertThat(map.toJson(HashMap.empty())).isEqualTo("{}");
        JsonType<SortedMap<String, Integer>> sortedMap = json(SortedMap.class, String.class, Integer.class);
        assertThat(sortedMap.toJson(TreeMap.of("b", 2, "a", 1))).isEqualTo("{\"a\":1,\"b\":2}");
    }

    @Test
    void aRecordWithInterfaceFieldsRoundTrips() {
        var interfaces = new Interfaces(
                LinkedHashSet.of("b", "a"),
                TreeSet.of(2, 1),
                LinkedHashMap.of("x", 1, "y", 2),
                TreeMap.of(LocalDate.of(2026, 10, 3), 1),
                Vector.of(HashSet.of(1), TreeSet.of(3, 2), LinkedHashSet.empty()),
                HashMap.of("s", TreeSet.of(5, 4)),
                Option.some(TreeMap.of(1, LinkedHashMap.of("k", 7))));
        var json = JSONB.toJson(interfaces);
        assertThat(json)
                .isEqualTo("{\"set\":[\"b\",\"a\"],\"sortedSet\":[1,2],\"map\":{\"x\":1,\"y\":2},"
                        + "\"sortedMap\":{\"2026-10-03\":1},\"sets\":[[1],[2,3],[]],\"sortedSets\":{\"s\":[4,5]},"
                        + "\"nested\":{\"1\":{\"k\":7}}}");
        var back = JSONB.type(Interfaces.class).fromJson(json);
        assertThat(back.set()).isExactlyInstanceOf(HashSet.class).containsExactlyInAnyOrder("a", "b");
        assertThat(back.sortedSet()).isEqualTo(TreeSet.of(1, 2));
        assertThat(back.map()).isEqualTo(HashMap.of("x", 1, "y", 2));
        assertThat(back.sortedMap()).isEqualTo(TreeMap.of(LocalDate.of(2026, 10, 3), 1));
        assertThat(back.sets()).isEqualTo(Vector.of(HashSet.of(1), HashSet.of(2, 3), HashSet.empty()));
        assertThat(back.sortedSets()).isEqualTo(HashMap.of("s", TreeSet.of(4, 5)));
        assertThat(back.nested()).isEqualTo(Option.some(TreeMap.of(1, HashMap.of("k", 7))));
    }

    @Test
    void theInterfacesNestInGenerics() {
        JsonType<Vector<Map<String, SortedSet<Integer>>>> nested =
                json(Vector.class, type(Map.class, String.class, type(SortedSet.class, Integer.class)));
        var value = Vector.<Map<String, SortedSet<Integer>>>of(HashMap.of("a", TreeSet.of(2, 1)), HashMap.empty());
        assertThat(nested.toJson(value)).isEqualTo("[{\"a\":[1,2]},{}]");
        assertThat(nested.fromJson("[{\"a\":[1,2]},{}]")).isEqualTo(value);
        JsonType<Option<Set<Option<Integer>>>> options =
                json(Option.class, type(Set.class, type(Option.class, Integer.class)));
        assertThat(options.fromJson("[1,null]")).isEqualTo(Option.some(HashSet.of(Option.some(1), Option.none())));
    }

    @Test
    void theInterfacesRejectWhatTheirImplementationsReject() {
        assertThatThrownBy(() -> json(Set.class, Integer.class).fromJson("[1,null]"))
                .isInstanceOf(JsonDataException.class)
                .hasMessageStartingWith("Element 1 of the Set is null: Zazr collections hold no null.");
        assertThatThrownBy(() -> json(SortedSet.class, Object.class).fromJson("[{}]"))
                .isInstanceOf(JsonDataException.class)
                .hasMessageStartingWith(
                        "Cannot read a SortedSet of java.lang.Object: it is read in the natural order of its "
                                + "elements, and java.lang.Object does not implement Comparable");
        assertThatThrownBy(
                        () -> json(SortedMap.class, String.class, Integer.class).fromJson("{\"a\":null}"))
                .isInstanceOf(JsonDataException.class)
                .hasMessageStartingWith("The value of key \"a\" of the SortedMap is null");
        assertThatThrownBy(() -> json(Map.class, String.class, Integer.class).fromJson("[]"))
                .isInstanceOf(JsonDataException.class)
                .hasMessageStartingWith("A Map is a JSON object, not an array");
    }

    @Test
    void aTraversableIsWrittenByTheClassOfItsValue() {
        JsonType<Object> traversable = json(Traversable.class, Object.class);
        assertThat(traversable.toJson(Vector.of(1, 2))).isEqualTo("[1,2]");
        assertThat(traversable.toJson(TreeSet.of("b", "a"))).isEqualTo("[\"a\",\"b\"]");
        assertThat(traversable.toJson(TreeMap.of("a", Vector.of(1)))).isEqualTo("{\"a\":[1]}");
        assertThat(JSONB.toJson(new Anything(Vector.of(3, 4)))).isEqualTo("{\"values\":[3,4]}");
        assertThat(JSONB.toJson(new Anything(null))).isEqualTo("{}");
        assertThat(traversable.fromJson("null")).isNull();
    }

    @Test
    void aTraversableCannotBeReadAndSaysWhatToDeclare() {
        assertThatThrownBy(() -> JSONB.type(Anything.class).fromJson("{\"values\":[1,2]}"))
                .isInstanceOf(JsonDataException.class)
                .hasMessageStartingWith("Cannot read a Traversable: the JSON does not say which collection to build. "
                        + "Declare a concrete type such as Vector or HashMap, or Set, SortedSet, Map or SortedMap. (at "
                        + "position: ");
    }

    @Test
    void anInterfaceKeepsTheReadOrderOfItsImplementation() {
        JsonType<SortedMap<Integer, Integer>> sorted = json(SortedMap.class, Integer.class, Integer.class);
        assertThat(sorted.fromJson("{\"3\":1,\"1\":2}").keySet()).containsExactly(1, 3);
        JsonType<Object> nonEmpty = json(NonEmptySortedMap.class, Integer.class, Integer.class);
        assertThat(nonEmpty.fromJson("{\"3\":1,\"1\":2}"))
                .isEqualTo(NonEmptySortedMap.of(Tuple.of(1, 2), Tuple.of(3, 1)));
    }
}
