package dev.zazr.avaje.jsonb;

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
import io.avaje.jsonb.JsonType;
import io.avaje.jsonb.Jsonb;
import java.lang.reflect.Type;
import java.time.LocalDate;
import java.util.function.Function;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static dev.zazr.avaje.jsonb.JsonTypes.JSONB;
import static dev.zazr.avaje.jsonb.JsonTypes.json;
import static dev.zazr.avaje.jsonb.JsonTypes.type;
import static org.assertj.core.api.Assertions.assertThat;

/// Every type read back from what it writes: empty, one element, and sizes past the leaves and the levels of the tries
/// and of a JSON parser's buffer; then nested generics, values written by their class, and the order of each type.
class RoundTripTest {

    /// `value` written as `expected`, and read back from it as an equal value of the same class.
    private static <T> void roundTrips(Type type, T value, String expected) {
        JsonType<T> json = JSONB.type(type);
        assertThat(json.toJson(value)).isEqualTo(expected);
        var back = json.fromJson(expected);
        assertThat(back).isEqualTo(value);
        assertThat(back).hasSameClassAs(value);
    }

    /// The JSON array of `0, 1, ..., size - 1`.
    private static String array(int size) {
        return Vector.range(0, size).mkString("[", ",", "]");
    }

    /// The JSON object of `"0":0, "1":1, ..., "size - 1":size - 1`.
    private static String object(int size) {
        return Vector.range(0, size).map(i -> "\"" + i + "\":" + i).mkString("{", ",", "}");
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2, 31, 32, 33, 1024, 1025, 5000})
    void theSequencesRoundTripAtEverySize(int size) {
        var range = Vector.range(0, size);
        var json = array(size);
        roundTrips(type(Vector.class, Integer.class), range, json);
        roundTrips(type(List.class, Integer.class), List.ofAll(range), json);
        roundTrips(type(Queue.class, Integer.class), Queue.ofAll(range), json);
        roundTrips(type(LazyList.class, Integer.class), LazyList.ofAll(range), json);
        roundTrips(type(LinkedHashSet.class, Integer.class), LinkedHashSet.ofAll(range), json);
        roundTrips(type(TreeSet.class, Integer.class), TreeSet.ofAll(range), json);
        // a hash set writes in its own order: read back what it writes
        var hashSet = HashSet.ofAll(range);
        roundTrips(type(HashSet.class, Integer.class), hashSet, hashSet.mkString("[", ",", "]"));
        if (size > 0) {
            roundTrips(
                    type(NonEmptyVector.class, Integer.class),
                    range.toNonEmptyVector().get(),
                    json);
            roundTrips(
                    type(NonEmptySortedSet.class, Integer.class),
                    TreeSet.ofAll(range).toNonEmptySortedSet().get(),
                    json);
            var nonEmptySet = hashSet.toNonEmptySet().get();
            roundTrips(type(NonEmptySet.class, Integer.class), nonEmptySet, nonEmptySet.mkString("[", ",", "]"));
        }
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2, 31, 32, 33, 1024, 1025, 5000})
    void theMapsRoundTripAtEverySize(int size) {
        var entries = Vector.range(0, size).map(i -> Tuple.of(String.valueOf(i), i));
        var json = object(size);
        roundTrips(type(LinkedHashMap.class, String.class, Integer.class), LinkedHashMap.ofEntries(entries), json);
        var byInteger = TreeMap.ofEntries(Vector.range(0, size).map(i -> Tuple.of(i, i)));
        roundTrips(type(TreeMap.class, Integer.class, Integer.class), byInteger, json);
        var hashMap = HashMap.ofEntries(entries);
        var hashJson =
                hashMap.toVector().map(e -> "\"" + e._1() + "\":" + e._2()).mkString("{", ",", "}");
        roundTrips(type(HashMap.class, String.class, Integer.class), hashMap, hashJson);
        if (size > 0) {
            roundTrips(
                    type(NonEmptySortedMap.class, Integer.class, Integer.class),
                    byInteger.toNonEmptySortedMap().get(),
                    json);
            roundTrips(
                    type(NonEmptyMap.class, String.class, Integer.class),
                    hashMap.toNonEmptyMap().get(),
                    hashJson);
        }
    }

    @Test
    void anOptionIsItsValueOrNull() {
        roundTrips(type(Option.class, Integer.class), Option.some(42), "42");
        roundTrips(type(Option.class, String.class), Option.some("a"), "\"a\"");
        roundTrips(type(Option.class, String.class), Option.none(), "null");
        roundTrips(type(Option.class, type(Vector.class, Integer.class)), Option.some(Vector.of(1, 2)), "[1,2]");
        roundTrips(type(Option.class, type(Vector.class, Integer.class)), Option.some(Vector.empty()), "[]");
    }

    @Test
    void theTuplesRoundTripAtEveryArity() {
        roundTrips(type(Tuple1.class, String.class), Tuple.of("a"), "[\"a\"]");
        roundTrips(type(Tuple2.class, String.class, Integer.class), Tuple.of("a", 1), "[\"a\",1]");
        roundTrips(
                type(Tuple3.class, String.class, Integer.class, Boolean.class),
                Tuple.of("a", 1, true),
                "[\"a\",1,true]");
        roundTrips(
                type(Tuple4.class, Integer.class, Integer.class, Integer.class, Integer.class),
                Tuple.of(1, 2, 3, 4),
                "[1,2,3,4]");
        roundTrips(
                type(Tuple5.class, Integer.class, Integer.class, Integer.class, Integer.class, Integer.class),
                Tuple.of(1, 2, 3, 4, 5),
                "[1,2,3,4,5]");
        roundTrips(
                type(
                        Tuple6.class,
                        Integer.class,
                        Integer.class,
                        Integer.class,
                        Integer.class,
                        Integer.class,
                        Integer.class),
                Tuple.of(1, 2, 3, 4, 5, 6),
                "[1,2,3,4,5,6]");
        roundTrips(
                type(
                        Tuple7.class,
                        Integer.class,
                        Integer.class,
                        Integer.class,
                        Integer.class,
                        Integer.class,
                        Integer.class,
                        Integer.class),
                Tuple.of(1, 2, 3, 4, 5, 6, 7),
                "[1,2,3,4,5,6,7]");
        roundTrips(
                type(
                        Tuple8.class,
                        String.class,
                        Integer.class,
                        Boolean.class,
                        LocalDate.class,
                        type(Option.class, String.class),
                        type(Vector.class, Integer.class),
                        type(TreeMap.class, String.class, Integer.class),
                        type(Tuple2.class, Integer.class, Integer.class)),
                Tuple.of(
                        "a",
                        1,
                        false,
                        LocalDate.of(2026, 10, 3),
                        Option.<String>none(),
                        Vector.of(1, 2),
                        TreeMap.of("b", 2, "a", 1),
                        Tuple.of(3, 4)),
                "[\"a\",1,false,\"2026-10-03\",null,[1,2],{\"a\":1,\"b\":2},[3,4]]");
    }

    @Test
    void nestedGenericsRoundTrip() {
        roundTrips(
                type(Vector.class, type(Option.class, Integer.class)),
                Vector.of(Option.some(1), Option.none(), Option.some(3)),
                "[1,null,3]");
        roundTrips(
                type(HashMap.class, LocalDate.class, type(Vector.class, String.class)),
                HashMap.of(LocalDate.of(2026, 10, 3), Vector.of("standup", "review")),
                "{\"2026-10-03\":[\"standup\",\"review\"]}");
        roundTrips(
                type(List.class, type(List.class, Integer.class)),
                List.of(List.of(1, 2), List.empty(), List.of(3)),
                "[[1,2],[],[3]]");
        roundTrips(
                type(TreeMap.class, String.class, type(NonEmptyVector.class, type(Option.class, String.class))),
                TreeMap.of("a", NonEmptyVector.of(Option.<String>none()), "b", NonEmptyVector.of(Option.some("x"))),
                "{\"a\":[null],\"b\":[\"x\"]}");
        roundTrips(
                type(LinkedHashMap.class, String.class, type(Option.class, Integer.class)),
                LinkedHashMap.of("a", Option.none(), "b", Option.some(2)),
                "{\"a\":null,\"b\":2}");
        roundTrips(
                type(Option.class, type(Tuple2.class, String.class, type(HashSet.class, Integer.class))),
                Option.some(Tuple.of("a", HashSet.of(1))),
                "[\"a\",[1]]");
    }

    @Test
    void theElementsKeepTheirOrderOrAreSorted() {
        JsonType<LinkedHashSet<Integer>> linkedSet = json(LinkedHashSet.class, Integer.class);
        assertThat(linkedSet.fromJson("[3,1,2,1]")).containsExactly(3, 1, 2);
        JsonType<TreeSet<Integer>> treeSet = json(TreeSet.class, Integer.class);
        assertThat(treeSet.fromJson("[3,1,2,1]")).containsExactly(1, 2, 3);
        JsonType<NonEmptySortedSet<String>> sortedSet = json(NonEmptySortedSet.class, String.class);
        assertThat(sortedSet.toJson(NonEmptySortedSet.of("b", "c", "a"))).isEqualTo("[\"a\",\"b\",\"c\"]");
        JsonType<LinkedHashMap<String, Integer>> linkedMap = json(LinkedHashMap.class, String.class, Integer.class);
        assertThat(linkedMap.fromJson("{\"c\":1,\"a\":2,\"b\":3}").keySet()).containsExactly("c", "a", "b");
        JsonType<TreeMap<String, Integer>> treeMap = json(TreeMap.class, String.class, Integer.class);
        assertThat(treeMap.toJson(TreeMap.of("c", 1, "a", 2, "b", 3))).isEqualTo("{\"a\":2,\"b\":3,\"c\":1}");
    }

    @Test
    void ofTwoEqualKeysTheLaterWins() {
        JsonType<LinkedHashMap<String, Integer>> linkedMap = json(LinkedHashMap.class, String.class, Integer.class);
        assertThat(linkedMap.fromJson("{\"a\":1,\"b\":2,\"a\":3}")).isEqualTo(LinkedHashMap.of("a", 3, "b", 2));
        assertThat(linkedMap.fromJson("{\"a\":1,\"b\":2,\"a\":3}").keySet()).containsExactly("a", "b");
        JsonType<HashMap<String, Integer>> hashMap = json(HashMap.class, String.class, Integer.class);
        assertThat(hashMap.fromJson("{\"a\":1,\"a\":3}")).isEqualTo(HashMap.of("a", 3));
        JsonType<TreeMap<Integer, Integer>> treeMap = json(TreeMap.class, Integer.class, Integer.class);
        assertThat(treeMap.fromJson("{\"2\":1,\"1\":2,\"2\":3}")).isEqualTo(TreeMap.of(1, 2, 2, 3));
    }

    @Test
    void aSortedSetOrMapReadComparesEqualToAndCombinesWithOneBuiltInCode() {
        JsonType<TreeSet<Integer>> treeSet = json(TreeSet.class, Integer.class);
        var read = treeSet.fromJson("[3,1]");
        assertThat(read.union(TreeSet.of(2))).isEqualTo(TreeSet.of(1, 2, 3));
        assertThat(read.comparator()).isSameAs(TreeSet.<Integer>empty().comparator());
        JsonType<TreeMap<Integer, String>> treeMap = json(TreeMap.class, Integer.class, String.class);
        assertThat(treeMap.fromJson("{\"3\":\"c\"}").put(1, "a")).isEqualTo(TreeMap.of(1, "a", 3, "c"));
    }

    @Test
    void aValueWrittenByItsClassUsesItsRuntimeType() {
        // Jsonb.toJson(Object) picks the adapter of the value's class, with no type arguments: elements go through
        // the adapter of Object, which writes them by their own class
        assertThat(JSONB.toJson(Vector.of(1, 2))).isEqualTo("[1,2]");
        assertThat(JSONB.toJson(List.of("a"))).isEqualTo("[\"a\"]");
        assertThat(JSONB.toJson(List.empty())).isEqualTo("[]");
        assertThat(JSONB.toJson(LazyList.of(1, 2))).isEqualTo("[1,2]");
        assertThat(JSONB.toJson(Option.some("a"))).isEqualTo("\"a\"");
        assertThat(JSONB.toJson(Option.none())).isEqualTo("null");
        assertThat(JSONB.toJson(Tuple.of(1, "a"))).isEqualTo("[1,\"a\"]");
        assertThat(JSONB.toJson(TreeMap.of("a", Vector.of(1)))).isEqualTo("{\"a\":[1]}");
        assertThat(JSONB.toJson(TreeMap.of(1, "a"))).isEqualTo("{\"1\":\"a\"}");
    }

    @Test
    void aLazyListIsWrittenByIteratingIt() {
        var evaluated = new int[1];
        var lazy = LazyList.iterate(1, i -> {
                    evaluated[0]++;
                    return i + 1;
                })
                .take(4);
        assertThat(evaluated[0]).isZero();
        JsonType<LazyList<Integer>> json = json(LazyList.class, Integer.class);
        assertThat(json.toJson(lazy)).isEqualTo("[1,2,3,4]");
        assertThat(evaluated[0]).isEqualTo(3);
    }

    @Test
    void aNullCollectionOrTupleIsNull() {
        for (var raw : new Class<?>[] {Vector.class, NonEmptyVector.class, TreeSet.class}) {
            JsonType<Object> json = json(raw, Integer.class);
            assertThat(json.fromJson("null")).isNull();
        }
        JsonType<Object> map = json(NonEmptyMap.class, String.class, Integer.class);
        assertThat(map.fromJson("null")).isNull();
        JsonType<Object> tuple = json(Tuple2.class, String.class, Integer.class);
        assertThat(tuple.fromJson("null")).isNull();
    }

    @Test
    void aNullElementOfATupleIsWrittenAsNull() {
        // the collections and Option hold no null; a tuple can, and reading rejects it (see ErrorsTest)
        JsonType<Tuple2<String, Integer>> tuple = json(Tuple2.class, String.class, Integer.class);
        assertThat(tuple.toJson(Tuple.of(null, 1))).isEqualTo("[null,1]");
        assertThat(Jsonb.builder().serializeNulls(false).build().toJson(Tuple.of(null, 1)))
                .isEqualTo("[null,1]");
    }

    @Test
    void theAdaptersNameTheirTypes() {
        Function<Type, String> name = t -> JSONB.adapter(t).toString();
        assertThat(name.apply(type(Vector.class, type(Option.class, String.class))))
                .startsWith("ZazrAdapter(Vector<ZazrAdapter(Option<");
        assertThat(name.apply(type(TreeMap.class, String.class, Integer.class)))
                .startsWith("ZazrAdapter(TreeMap<String, ");
        assertThat(name.apply(type(TreeMap.class, LocalDate.class, Integer.class)))
                .startsWith("ZazrAdapter(TreeMap<");
        assertThat(name.apply(type(Tuple2.class, String.class, Integer.class))).isEqualTo("ZazrAdapter(Tuple2)");
    }
}
