package dev.zazr.avaje.jsonb;

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

    /// The failure of reading `json` as `type`: a `JsonDataException` whose message starts with `message` and ends
    /// with the position in the input.
    private static void rejects(JsonType<?> type, String json, String message) {
        assertThatThrownBy(() -> type.fromJson(json))
                .isInstanceOf(JsonDataException.class)
                .hasMessageStartingWith(message)
                .hasMessageContaining(", at position: ");
    }

    @Test
    void aNonEmptyTypeRejectsAnEmptyArrayOrObject() {
        rejects(
                json(NonEmptyVector.class, Integer.class),
                "[]",
                "NonEmptyVector needs at least one element, but the JSON array is empty");
        rejects(
                json(NonEmptySet.class, Integer.class),
                "[]",
                "NonEmptySet needs at least one element, but the JSON array is empty");
        rejects(
                json(NonEmptySortedSet.class, Integer.class),
                "[]",
                "NonEmptySortedSet needs at least one element, but the JSON array is empty");
        rejects(
                json(NonEmptyMap.class, String.class, Integer.class),
                "{}",
                "NonEmptyMap needs at least one entry, but the JSON object is empty");
        rejects(
                json(NonEmptySortedMap.class, String.class, Integer.class),
                "{}",
                "NonEmptySortedMap needs at least one entry, but the JSON object is empty");
    }

    @Test
    void anEmptyNonEmptyTypeNestedInAnotherIsRejectedToo() {
        rejects(
                json(Vector.class, type(NonEmptyVector.class, Integer.class)),
                "[[1],[]]",
                "NonEmptyVector needs at least one element, but the JSON array is empty");
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
            rejects(
                    json(raw, Integer.class),
                    "[1,2,null]",
                    raw.getSimpleName() + " rejects null elements: the element at index 2 is null");
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
                    raw.getSimpleName() + " rejects null values: the value of the key \"b\" is null");
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
                "HashMap cannot read the key \"2026-13-45\": ");
        rejects(
                json(TreeMap.class, Integer.class, Integer.class),
                "{\"one\":1}",
                "TreeMap cannot read the key \"one\": For input string: \"one\"");
        rejects(json(HashMap.class, UUID.class, Integer.class), "{\"x\":1}", "HashMap cannot read the key \"x\": ");
    }

    @Test
    void aMapRejectsAKeyReadAsNull() {
        rejects(
                TestKeys.JSONB.type(type(LinkedHashMap.class, NullKey.class, Integer.class)),
                "{\"a\":1}",
                "LinkedHashMap rejects null keys: the key \"a\" is read as null");
    }

    @Test
    void aTupleRejectsAnArrayOfTheWrongLength() {
        JsonType<Tuple2<String, Integer>> pair = json(Tuple2.class, String.class, Integer.class);
        rejects(pair, "[]", "Tuple2 needs a JSON array of exactly 2 elements, but the array has 0");
        rejects(pair, "[\"a\"]", "Tuple2 needs a JSON array of exactly 2 elements, but the array has 1");
        rejects(pair, "[\"a\",1,2]", "Tuple2 needs a JSON array of exactly 2 elements, but the array has 3");
        rejects(
                pair,
                "[\"a\",1,2,[3,4],{\"b\":5}]",
                "Tuple2 needs a JSON array of exactly 2 elements, but the array has 5");
        rejects(
                json(Tuple1.class, String.class),
                "[]",
                "Tuple1 needs a JSON array of exactly 1 element, but the array has 0");
        var eight = new Class<?>[8];
        java.util.Arrays.fill(eight, Integer.class);
        rejects(
                json(Tuple8.class, eight),
                "[1,2,3,4,5,6,7,8,9]",
                "Tuple8 needs a JSON array of exactly 8 elements, but the array has 9");
    }

    @Test
    void aTupleRejectsANullElement() {
        JsonType<Tuple3<String, Integer, String>> triple =
                json(Tuple3.class, String.class, Integer.class, String.class);
        rejects(triple, "[\"a\",null,\"c\"]", "Tuple3 rejects null elements: the element at index 1 is null");
    }

    @Test
    void aSortedTypeRejectsElementsWithNoNaturalOrder() {
        rejects(
                json(TreeSet.class, type(Vector.class, Integer.class)),
                "[[1]]",
                "TreeSet sorts its elements in their natural order, but dev.zazr.collection.Vector is not Comparable");
        rejects(
                json(NonEmptySortedSet.class, Object.class),
                "[{\"a\":1}]",
                "NonEmptySortedSet sorts its elements in their natural order, but java.util.LinkedHashMap is not "
                        + "Comparable");
        rejects(
                TestKeys.JSONB.type(type(TreeMap.class, Unordered.class, Integer.class)),
                "{\"a\":1}",
                "TreeMap sorts its keys in their natural order, but dev.zazr.avaje.jsonb.TestKeys$Unordered is not Comparable");
        rejects(
                TestKeys.JSONB.type(type(NonEmptySortedMap.class, Unordered.class, Integer.class)),
                "{\"a\":1}",
                "NonEmptySortedMap sorts its keys in their natural order, but dev.zazr.avaje.jsonb.TestKeys$Unordered is not "
                        + "Comparable");
    }

    @Test
    void aSortedTypeRejectsElementsNotComparableWithEachOther() {
        rejects(
                json(TreeSet.class, Object.class),
                "[1,\"a\"]",
                "TreeSet sorts its elements in their natural order, but they are not comparable with each other (");
        rejects(
                TestKeys.JSONB.type(type(NonEmptySortedMap.class, Mixed.class, Integer.class)),
                "{\"1\":1,\"a\":2}",
                "NonEmptySortedMap sorts its keys in their natural order, but they are not comparable with each other (");
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "1", "\"a\""})
    void aSequenceRejectsWhatIsNotAnArray(String json) {
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
