package dev.zazr.avaje.jsonb;

import dev.zazr.Tuple;
import dev.zazr.Tuple1;
import dev.zazr.Tuple2;
import dev.zazr.Tuple3;
import dev.zazr.Tuple8;
import dev.zazr.avaje.jsonb.TestRecords.Line;
import dev.zazr.avaje.jsonb.TestRecords.Order;
import dev.zazr.collection.HashMap;
import dev.zazr.collection.HashSet;
import dev.zazr.collection.LazyList;
import dev.zazr.collection.LinkedHashMap;
import dev.zazr.collection.LinkedHashSet;
import dev.zazr.collection.List;
import dev.zazr.collection.Map;
import dev.zazr.collection.NonEmptyMap;
import dev.zazr.collection.NonEmptySet;
import dev.zazr.collection.NonEmptySortedMap;
import dev.zazr.collection.NonEmptySortedSet;
import dev.zazr.collection.NonEmptyVector;
import dev.zazr.collection.Queue;
import dev.zazr.collection.Set;
import dev.zazr.collection.SortedMap;
import dev.zazr.collection.SortedSet;
import dev.zazr.collection.TreeMap;
import dev.zazr.collection.TreeSet;
import dev.zazr.collection.Vector;
import dev.zazr.control.Option;
import io.avaje.jsonb.JsonType;
import java.lang.reflect.Type;
import java.time.LocalDate;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static dev.zazr.avaje.jsonb.JsonTypes.JSONB;
import static dev.zazr.avaje.jsonb.JsonTypes.type;
import static org.assertj.core.api.Assertions.assertThat;

/// The JSON of a fixed sample of each type, written exactly as the format table of the documentation states, the same
/// text `zazr-jackson` writes, and read back from it. A change here changes the JSON format.
class ConformanceTest {

    static Stream<Arguments> samples() {
        var integer = Integer.class;
        var string = String.class;
        return Stream.of(
                Arguments.of(type(Vector.class, integer), Vector.of(1, 2, 3), "[1,2,3]"),
                Arguments.of(type(Vector.class, integer), Vector.empty(), "[]"),
                Arguments.of(type(List.class, string), List.of("a", "b"), "[\"a\",\"b\"]"),
                Arguments.of(type(Queue.class, integer), Queue.of(1, 2), "[1,2]"),
                Arguments.of(type(LazyList.class, integer), LazyList.of(1, 2, 3), "[1,2,3]"),
                Arguments.of(type(HashSet.class, string), HashSet.of("a"), "[\"a\"]"),
                Arguments.of(type(LinkedHashSet.class, integer), LinkedHashSet.of(3, 1, 2), "[3,1,2]"),
                Arguments.of(type(TreeSet.class, integer), TreeSet.of(3, 1, 2), "[1,2,3]"),
                Arguments.of(type(HashMap.class, string, integer), HashMap.of("a", 1), "{\"a\":1}"),
                Arguments.of(type(HashMap.class, string, integer), HashMap.empty(), "{}"),
                Arguments.of(
                        type(LinkedHashMap.class, string, integer),
                        LinkedHashMap.of("b", 2, "a", 1),
                        "{\"b\":2,\"a\":1}"),
                Arguments.of(type(TreeMap.class, string, integer), TreeMap.of("b", 2, "a", 1), "{\"a\":1,\"b\":2}"),
                Arguments.of(
                        type(TreeMap.class, integer, string),
                        TreeMap.of(10, "b", 2, "a"),
                        "{\"2\":\"a\",\"10\":\"b\"}"),
                Arguments.of(
                        type(HashMap.class, LocalDate.class, integer),
                        HashMap.of(LocalDate.of(2026, 10, 3), 1),
                        "{\"2026-10-03\":1}"),
                Arguments.of(type(Set.class, integer), HashSet.of(1), "[1]"),
                Arguments.of(type(SortedSet.class, integer), TreeSet.of(2, 1), "[1,2]"),
                Arguments.of(type(Map.class, string, integer), HashMap.of("a", 1), "{\"a\":1}"),
                Arguments.of(type(SortedMap.class, string, integer), TreeMap.of("b", 2, "a", 1), "{\"a\":1,\"b\":2}"),
                Arguments.of(type(Tuple2.class, integer, integer), Tuple.of(1, null), "[1,null]"),
                Arguments.of(type(Option.class, integer), Option.some(1), "1"),
                Arguments.of(type(Option.class, string), Option.some("a"), "\"a\""),
                Arguments.of(type(Option.class, integer), Option.none(), "null"),
                Arguments.of(type(NonEmptyVector.class, integer), NonEmptyVector.of(1, 2), "[1,2]"),
                Arguments.of(type(NonEmptySet.class, string), NonEmptySet.of("a"), "[\"a\"]"),
                Arguments.of(type(NonEmptySortedSet.class, integer), NonEmptySortedSet.of(2, 1), "[1,2]"),
                Arguments.of(type(NonEmptyMap.class, string, integer), NonEmptyMap.of(Tuple.of("a", 1)), "{\"a\":1}"),
                Arguments.of(
                        type(NonEmptySortedMap.class, string, integer),
                        NonEmptySortedMap.of(Tuple.of("b", 2), Tuple.of("a", 1)),
                        "{\"a\":1,\"b\":2}"),
                Arguments.of(type(Tuple1.class, string), Tuple.of("a"), "[\"a\"]"),
                Arguments.of(type(Tuple2.class, integer, string), Tuple.of(1, "a"), "[1,\"a\"]"),
                Arguments.of(
                        type(Tuple3.class, string, type(Option.class, integer), type(Vector.class, integer)),
                        Tuple.of("a", Option.<Integer>none(), Vector.of(1)),
                        "[\"a\",null,[1]]"),
                Arguments.of(
                        type(Tuple8.class, integer, integer, integer, integer, integer, integer, integer, integer),
                        Tuple.of(1, 2, 3, 4, 5, 6, 7, 8),
                        "[1,2,3,4,5,6,7,8]"),
                Arguments.of(
                        type(Vector.class, type(Option.class, integer)),
                        Vector.of(Option.some(1), Option.none()),
                        "[1,null]"),
                Arguments.of(
                        Order.class,
                        new Order(Vector.of(new Line("a-1", 2)), Option.none()),
                        "{\"lines\":[{\"sku\":\"a-1\",\"quantity\":2}],\"note\":null}"),
                Arguments.of(
                        Order.class,
                        new Order(Vector.empty(), Option.some("gift")),
                        "{\"lines\":[],\"note\":\"gift\"}"));
    }

    @ParameterizedTest
    @MethodSource("samples")
    <T> void theSampleIsWrittenAsTheExpectedJsonAndReadBackFromIt(Type type, T sample, String expected) {
        JsonType<T> json = JSONB.type(type);
        assertThat(json.toJson(sample)).isEqualTo(expected);
        assertThat(json.fromJson(expected)).isEqualTo(sample);
    }
}
