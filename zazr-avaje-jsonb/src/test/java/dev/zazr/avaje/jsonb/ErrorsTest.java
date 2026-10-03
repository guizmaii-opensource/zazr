package dev.zazr.avaje.jsonb;

import dev.zazr.Tuple;
import dev.zazr.Tuple1;
import dev.zazr.Tuple2;
import dev.zazr.Tuple3;
import dev.zazr.Tuple8;
import dev.zazr.avaje.jsonb.TestKeys.Mixed;
import dev.zazr.avaje.jsonb.TestKeys.NullKey;
import dev.zazr.avaje.jsonb.TestKeys.Unordered;
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
import dev.zazr.collection.SortedSet;
import dev.zazr.collection.TreeMap;
import dev.zazr.collection.TreeSet;
import dev.zazr.collection.Vector;
import dev.zazr.control.Option;
import io.avaje.json.JsonDataException;
import io.avaje.json.JsonException;
import io.avaje.jsonb.JsonType;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static dev.zazr.avaje.jsonb.JsonTypes.json;
import static dev.zazr.avaje.jsonb.JsonTypes.type;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/// Every JSON that reading rejects, and its message: the type, what is wrong, and where in the input.
class ErrorsTest {

    private static final String NO_NULL =
            " is null: Zazr collections hold no null. Use Option for a value that may be missing.";

    private static final class Records {
        static final JsonType<TestRecords.Order> ORDERS = JsonTypes.JSONB.type(TestRecords.Order.class);
    }

    /// The failure of reading `json` as `type`: a `JsonDataException` whose message starts with `message` and ends
    /// with the position in the input.
    private static void rejects(JsonType<?> type, String json, String message) {
        assertThatThrownBy(() -> type.fromJson(json))
                .isInstanceOf(JsonDataException.class)
                .hasMessageStartingWith(message)
                .hasMessageContaining(" (at position: ");
    }

    @Test
    void aNonEmptyTypeRejectsAnEmptyArrayOrObject() {
        rejects(
                json(NonEmptyVector.class, Integer.class),
                "[]",
                "A NonEmptyVector needs at least one element: the JSON array is empty.");
        rejects(
                json(NonEmptySet.class, Integer.class),
                "[]",
                "A NonEmptySet needs at least one element: the JSON array is empty.");
        rejects(
                json(NonEmptySortedSet.class, Integer.class),
                "[]",
                "A NonEmptySortedSet needs at least one element: the JSON array is empty.");
        rejects(
                json(NonEmptyMap.class, String.class, Integer.class),
                "{}",
                "A NonEmptyMap needs at least one entry: the JSON object is empty.");
        rejects(
                json(NonEmptySortedMap.class, String.class, Integer.class),
                "{}",
                "A NonEmptySortedMap needs at least one entry: the JSON object is empty.");
    }

    @Test
    void anEmptyNonEmptyTypeNestedInAnotherIsRejectedToo() {
        rejects(
                json(Vector.class, type(NonEmptyVector.class, Integer.class)),
                "[[1],[]]",
                "A NonEmptyVector needs at least one element: the JSON array is empty.");
    }

    @Test
    void everySequenceRejectsANullElement() {
        for (var raw : new Class<?>[] {
            Vector.class,
            List.class,
            Queue.class,
            LazyList.class,
            HashSet.class,
            LinkedHashSet.class,
            TreeSet.class,
            NonEmptyVector.class,
            NonEmptySet.class,
            NonEmptySortedSet.class
        }) {
            rejects(json(raw, Integer.class), "[1,2,null]", "Element 2 of the " + raw.getSimpleName() + NO_NULL);
        }
    }

    @Test
    void everyMapRejectsANullValue() {
        for (var raw : new Class<?>[] {
            HashMap.class, LinkedHashMap.class, TreeMap.class, NonEmptyMap.class, NonEmptySortedMap.class
        }) {
            rejects(
                    json(raw, String.class, Integer.class),
                    "{\"a\":1,\"b\":null}",
                    "The value of key \"b\" of the " + raw.getSimpleName() + NO_NULL);
        }
    }

    @Test
    void aNoneElementOrValueIsNotNull() {
        JsonType<Vector<Option<Integer>>> vector = json(Vector.class, type(Option.class, Integer.class));
        assertThat(vector.fromJson("[null]")).isEqualTo(Vector.of(Option.none()));
        JsonType<HashMap<String, Option<Integer>>> map =
                json(HashMap.class, String.class, type(Option.class, Integer.class));
        assertThat(map.fromJson("{\"a\":null}")).isEqualTo(HashMap.of("a", Option.none()));
    }

    @Test
    void aMapRejectsAKeyItsKeyTypeCannotRead() {
        rejects(
                json(HashMap.class, LocalDate.class, Integer.class),
                "{\"2026-13-45\":1}",
                "Cannot read a key of the HashMap from \"2026-13-45\": ");
        rejects(
                json(TreeMap.class, Integer.class, Integer.class),
                "{\"one\":1}",
                "Cannot read a key of the TreeMap from \"one\": For input string: \"one\"");
        rejects(
                json(HashMap.class, UUID.class, Integer.class),
                "{\"x\":1}",
                "Cannot read a key of the HashMap from \"x\": ");
    }

    @Test
    void aMapRejectsAKeyReadAsNull() {
        rejects(
                TestKeys.JSONB.type(type(LinkedHashMap.class, NullKey.class, Integer.class)),
                "{\"a\":1}",
                "The key read from \"a\" of the LinkedHashMap" + NO_NULL);
    }

    @Test
    void aTupleRejectsAnArrayOfTheWrongLength() {
        JsonType<Tuple2<String, Integer>> pair = json(Tuple2.class, String.class, Integer.class);
        rejects(pair, "[]", "A Tuple2 is a JSON array of 2 elements: this one has 0.");
        rejects(pair, "[\"a\"]", "A Tuple2 is a JSON array of 2 elements: this one has 1.");
        rejects(pair, "[\"a\",1,2]", "A Tuple2 is a JSON array of 2 elements: this one has 3.");
        rejects(pair, "[\"a\",1,2,[3,4],{\"b\":5}]", "A Tuple2 is a JSON array of 2 elements: this one has 5.");
        rejects(json(Tuple1.class, String.class), "[]", "A Tuple1 is a JSON array of 1 element: this one has 0.");
        var eight = new Class<?>[8];
        java.util.Arrays.fill(eight, Integer.class);
        rejects(
                json(Tuple8.class, eight),
                "[1,2,3,4,5,6,7,8,9]",
                "A Tuple8 is a JSON array of 8 elements: this one has 9.");
    }

    @Test
    void aTupleReadsANullElementAsANullComponent() {
        JsonType<Tuple3<String, Integer, String>> triple =
                json(Tuple3.class, String.class, Integer.class, String.class);
        assertThat(triple.fromJson("[\"a\",null,\"c\"]")).isEqualTo(Tuple.of("a", null, "c"));
    }

    @Test
    void aSortedTypeWhoseDeclaredElementTypeHasNoNaturalOrderCannotBeRead() {
        rejects(
                json(TreeSet.class, type(Vector.class, Integer.class)),
                "[[1]]",
                "Cannot read a TreeSet of dev.zazr.collection.Vector<java.lang.Integer>: it is read in the natural order "
                        + "of its elements, and dev.zazr.collection.Vector does not implement Comparable");
        rejects(
                TestKeys.JSONB.type(type(TreeMap.class, Unordered.class, Integer.class)),
                "{\"a\":1}",
                "Cannot read a TreeMap of dev.zazr.avaje.jsonb.TestKeys$Unordered: it is read in the natural order of its "
                        + "keys, and dev.zazr.avaje.jsonb.TestKeys$Unordered does not implement Comparable");
        rejects(
                TestKeys.JSONB.type(type(NonEmptySortedMap.class, Unordered.class, Integer.class)),
                "{\"a\":1}",
                "Cannot read a NonEmptySortedMap of dev.zazr.avaje.jsonb.TestKeys$Unordered: it is read in the natural "
                        + "order of its keys, and dev.zazr.avaje.jsonb.TestKeys$Unordered does not implement Comparable");
    }

    @Test
    void aSortedTypeOfObjectOrRawFailsWhateverTheData() {
        // the elements read could all be comparable: the declared type does not promise it, so reading fails at once
        for (var json : new String[] {"[]", "[1,2]", "[\"a\"]"}) {
            rejects(
                    json(TreeSet.class, Object.class),
                    json,
                    "Cannot read a TreeSet of java.lang.Object: it is read in the natural order of its elements, and "
                            + "java.lang.Object does not implement Comparable");
            rejects(
                    json(NonEmptySortedSet.class, Object.class),
                    json,
                    "Cannot read a NonEmptySortedSet of java.lang.Object: it is read in the natural order of its "
                            + "elements, and java.lang.Object does not implement Comparable");
        }
        rejects(
                JsonTypes.JSONB.type(SortedSet.class),
                "[1]",
                "Cannot read a SortedSet of java.lang.Object: it is read in the natural order of its elements, and "
                        + "java.lang.Object does not implement Comparable");
        rejects(
                JsonTypes.JSONB.type(TreeMap.class),
                "{\"a\":1}",
                "Cannot read a TreeMap of java.lang.Object: it is read in the natural order of its keys, and "
                        + "java.lang.Object does not implement Comparable");
        rejects(
                json(NonEmptySortedMap.class, Object.class, Integer.class),
                "{}",
                "Cannot read a NonEmptySortedMap of java.lang.Object: it is read in the natural order of its keys, and "
                        + "java.lang.Object does not implement Comparable");
    }

    @Test
    void aSortedTypeOfObjectIsStillWritten() {
        JsonType<Object> json = json(TreeSet.class, Object.class);
        assertThat(json.toJson(TreeSet.of(2, 1))).isEqualTo("[1,2]");
    }

    @Test
    void aSortedTypeRejectsElementsNotComparableWithEachOther() {
        rejects(
                TestKeys.JSONB.type(type(TreeSet.class, Mixed.class)),
                "[\"1\",\"a\"]",
                "Cannot read a TreeSet: it is read in the natural order of its elements, and they are not comparable "
                        + "with each other (");
        rejects(
                TestKeys.JSONB.type(type(NonEmptySortedMap.class, Mixed.class, Integer.class)),
                "{\"1\":1,\"a\":2}",
                "Cannot read a NonEmptySortedMap: it is read in the natural order of its keys, and they are not "
                        + "comparable with each other (");
    }

    @Test
    void aValueOfTheWrongKindNamesTheType() {
        rejects(json(NonEmptyVector.class, Integer.class), "{}", "A NonEmptyVector is a JSON array, not an object");
        rejects(json(HashSet.class, Integer.class), "\"a\"", "A HashSet is a JSON array, not a string");
        rejects(
                json(NonEmptyMap.class, String.class, Integer.class),
                "[]",
                "A NonEmptyMap is a JSON object, not an array");
        rejects(json(TreeMap.class, String.class, Integer.class), "true", "A TreeMap is a JSON object, not a boolean");
        rejects(
                json(Tuple3.class, Integer.class, Integer.class, Integer.class),
                "{}",
                "A Tuple3 is a JSON array, not an object");
        // nested: the position is the one of the inner value
        rejects(
                json(Vector.class, type(Vector.class, Integer.class)),
                "[[1],{\"a\":1}]",
                "A Vector is a JSON array, not an object");
        rejects(
                json(HashMap.class, String.class, type(Tuple2.class, Integer.class, Integer.class)),
                "{\"a\":\"x\"}",
                "A Tuple2 is a JSON array, not a string");
        rejects(Records.ORDERS, "{\"lines\":{},\"note\":null}", "A Vector is a JSON array, not an object");
    }

    @ParameterizedTest
    @ValueSource(strings = {"1", "-2.5"})
    void aNumberWhereACollectionIsExpectedIsLeftToAvaje(String json) {
        assertThatThrownBy(() -> json(Vector.class, Integer.class).fromJson(json))
                .isInstanceOf(JsonException.class);
    }

    @Test
    void writingAKeyThatIsNotAStringNumberOrBooleanFails() {
        JsonType<HashMap<Vector<Integer>, Integer>> map =
                json(HashMap.class, type(Vector.class, Integer.class), Integer.class);
        assertThatThrownBy(() -> map.toJson(HashMap.of(Vector.of(1), 1)))
                .hasRootCauseInstanceOf(JsonDataException.class)
                .rootCause()
                .hasMessage("a map key must be written as a JSON string, number or boolean, but the key Vector(1) "
                        + "(dev.zazr.collection.Vector) is written as an array");
    }
}
