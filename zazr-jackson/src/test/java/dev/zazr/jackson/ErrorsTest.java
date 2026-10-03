package dev.zazr.jackson;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import dev.zazr.Tuple1;
import dev.zazr.Tuple2;
import dev.zazr.Tuple3;
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
import dev.zazr.collection.TreeMap;
import dev.zazr.collection.TreeSet;
import dev.zazr.collection.Vector;
import dev.zazr.control.Option;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.KeyDeserializer;
import tools.jackson.databind.annotation.JsonDeserialize;
import tools.jackson.databind.exc.MismatchedInputException;

import static dev.zazr.jackson.Json.read;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

/// What fails, and the messages: `null` elements, keys and values, empty non-empty types, tuples of the wrong length,
/// JSON of the wrong shape. The path of the error leads to the element or entry at fault.
class ErrorsTest {

    private static final String NO_NULL =
            " is null: Zazr collections hold no null. Use Option for a value that may be missing.";

    private static MismatchedInputException failure(String json, TypeReference<?> type) {
        return catchThrowableOfType(MismatchedInputException.class, () -> read(json, type));
    }

    private static <T> MismatchedInputException failure(String json, Class<T> type) {
        return catchThrowableOfType(MismatchedInputException.class, () -> read(json, type));
    }

    private static String path(JacksonException e) {
        return e.getPath().stream()
                .map(JacksonException.Reference::getDescription)
                .collect(Collectors.joining("->"));
    }

    static java.util.List<Object[]> collections() {
        return java.util.List.of(
                new Object[] {new TypeReference<Vector<Integer>>() {}, "Vector"},
                new Object[] {new TypeReference<List<Integer>>() {}, "List"},
                new Object[] {new TypeReference<Queue<Integer>>() {}, "Queue"},
                new Object[] {new TypeReference<LazyList<Integer>>() {}, "LazyList"},
                new Object[] {new TypeReference<HashSet<Integer>>() {}, "HashSet"},
                new Object[] {new TypeReference<Set<Integer>>() {}, "HashSet"},
                new Object[] {new TypeReference<LinkedHashSet<Integer>>() {}, "LinkedHashSet"},
                new Object[] {new TypeReference<TreeSet<Integer>>() {}, "TreeSet"},
                new Object[] {new TypeReference<NonEmptyVector<Integer>>() {}, "NonEmptyVector"},
                new Object[] {new TypeReference<NonEmptySet<Integer>>() {}, "NonEmptySet"},
                new Object[] {new TypeReference<NonEmptySortedSet<Integer>>() {}, "NonEmptySortedSet"});
    }

    @ParameterizedTest
    @MethodSource("collections")
    void aNullElementFails(TypeReference<?> type, String name) {
        var e = failure("[1,2,null]", type);
        assertThat(e.getOriginalMessage()).isEqualTo("Element 2 of the " + name + NO_NULL);
        assertThat(e.getPath())
                .last()
                .extracting(JacksonException.Reference::getIndex)
                .isEqualTo(2);
    }

    static java.util.List<Object[]> maps() {
        return java.util.List.of(
                new Object[] {new TypeReference<HashMap<String, Integer>>() {}, "HashMap"},
                new Object[] {new TypeReference<Map<String, Integer>>() {}, "HashMap"},
                new Object[] {new TypeReference<LinkedHashMap<String, Integer>>() {}, "LinkedHashMap"},
                new Object[] {new TypeReference<TreeMap<String, Integer>>() {}, "TreeMap"},
                new Object[] {new TypeReference<NonEmptyMap<String, Integer>>() {}, "NonEmptyMap"},
                new Object[] {new TypeReference<NonEmptySortedMap<String, Integer>>() {}, "NonEmptySortedMap"});
    }

    @ParameterizedTest
    @MethodSource("maps")
    void aNullValueFails(TypeReference<?> type, String name) {
        var e = failure("{\"a\":1,\"b\":null}", type);
        assertThat(e.getOriginalMessage()).isEqualTo("The value of key \"b\" of the " + name + NO_NULL);
        assertThat(e.getPath())
                .last()
                .extracting(JacksonException.Reference::getPropertyName)
                .isEqualTo("b");
    }

    /// Reads every key as `null`.
    static final class NullKeys extends KeyDeserializer {
        @Override
        public Object deserializeKey(String key, DeserializationContext ctxt) {
            return null;
        }
    }

    record NullKeyed(
            @JsonDeserialize(keyUsing = NullKeys.class) HashMap<String, Integer> map) {}

    @Test
    void aNullKeyFails() {
        var e = failure("{\"map\":{\"a\":1}}", NullKeyed.class);
        assertThat(e.getOriginalMessage()).isEqualTo("The key read from \"a\" of the HashMap" + NO_NULL);
        assertThat(path(e))
                .isEqualTo("dev.zazr.jackson.ErrorsTest$NullKeyed[\"map\"]->dev.zazr.collection.HashMap[\"a\"]");
    }

    record Order(Vector<Vector<Integer>> lines) {}

    @Test
    void theMessageOfANestedNullLeadsToIt() {
        var e = failure("{\"lines\":[[1],[2,null]]}", Order.class);
        assertThat(e.getOriginalMessage()).isEqualTo("Element 1 of the Vector" + NO_NULL);
        assertThat(path(e))
                .isEqualTo("dev.zazr.jackson.ErrorsTest$Order[\"lines\"]->dev.zazr.collection.Vector[1]"
                        + "->dev.zazr.collection.Vector[1]");
        assertThat(e.getMessage())
                .contains("(through reference chain: dev.zazr.jackson.ErrorsTest$Order[\"lines\"]"
                        + "->dev.zazr.collection.Vector[1]->dev.zazr.collection.Vector[1])");
        assertThat(failure("[null]", new TypeReference<Vector<Vector<Integer>>>() {})
                        .getOriginalMessage())
                .isEqualTo("Element 0 of the Vector" + NO_NULL);
    }

    @Test
    void anElementOfTheWrongTypeFailsWithItsIndex() {
        var e = failure("[1,\"x\"]", new TypeReference<Vector<Integer>>() {});
        assertThat(e.getOriginalMessage())
                .startsWith("Cannot deserialize value of type `java.lang.Integer` from String \"x\"");
        assertThat(path(e)).isEqualTo("dev.zazr.collection.Vector[1]");
        var m = failure("{\"a\":\"x\"}", new TypeReference<HashMap<String, Integer>>() {});
        assertThat(path(m)).isEqualTo("dev.zazr.collection.HashMap[\"a\"]");
        var k = catchThrowableOfType(
                JacksonException.class, () -> read("{\"x\":1}", new TypeReference<HashMap<Integer, Integer>>() {}));
        assertThat(k.getOriginalMessage())
                .startsWith("Cannot deserialize Map key of type `java.lang.Integer` from String \"x\"");
        assertThat(path(k)).isEqualTo("dev.zazr.collection.HashMap[\"x\"]");
    }

    @Test
    void emptyNonEmptyTypesFail() {
        for (var type : java.util.List.of(
                new TypeReference<NonEmptyVector<Integer>>() {},
                new TypeReference<NonEmptySet<Integer>>() {},
                new TypeReference<NonEmptySortedSet<Integer>>() {})) {
            var name = type.getType().getTypeName().replaceAll("^.*\\.(\\w+)<.*$", "$1");
            assertThat(failure("[]", type).getOriginalMessage())
                    .isEqualTo("A " + name + " needs at least one element: the JSON array is empty.");
        }
        assertThat(failure("{}", new TypeReference<NonEmptyMap<String, Integer>>() {})
                        .getOriginalMessage())
                .isEqualTo("A NonEmptyMap needs at least one entry: the JSON object is empty.");
        assertThat(failure("{}", new TypeReference<NonEmptySortedMap<String, Integer>>() {})
                        .getOriginalMessage())
                .isEqualTo("A NonEmptySortedMap needs at least one entry: the JSON object is empty.");
    }

    @Test
    void tuplesOfTheWrongLengthFail() {
        assertThat(failure("[]", new TypeReference<Tuple2<Integer, Integer>>() {})
                        .getOriginalMessage())
                .isEqualTo("A Tuple2 is a JSON array of 2 elements: this one has 0.");
        assertThat(failure("[1]", new TypeReference<Tuple2<Integer, Integer>>() {})
                        .getOriginalMessage())
                .isEqualTo("A Tuple2 is a JSON array of 2 elements: this one has 1.");
        assertThat(failure("[1,2,[3,[4]],{\"a\":[5]}]", new TypeReference<Tuple2<Integer, Integer>>() {})
                        .getOriginalMessage())
                .isEqualTo("A Tuple2 is a JSON array of 2 elements: this one has 4.");
        assertThat(failure("[1,2]", new TypeReference<Tuple1<Integer>>() {}).getOriginalMessage())
                .isEqualTo("A Tuple1 is a JSON array of 1 element: this one has 2.");
    }

    /// A tuple holds `null` components, as `Tuple.of(1, null)` does.
    @Test
    void aNullTupleComponentReadsAsTheNullValueOfItsType() {
        assertThat(read("[1,null,3]", new TypeReference<Tuple3<Integer, Integer, Integer>>() {}))
                .isEqualTo(dev.zazr.Tuple.of(1, null, 3));
        var e = failure("[1,\"x\",3]", new TypeReference<Tuple3<Integer, Integer, Integer>>() {});
        assertThat(path(e)).isEqualTo("dev.zazr.Tuple3[1]");
        assertThat(read("[1,null]", new TypeReference<Tuple2<Integer, Option<Integer>>>() {}))
                .isEqualTo(dev.zazr.Tuple.of(1, Option.none()));
    }

    @Test
    void jsonOfTheWrongShapeFails() {
        assertThat(failure("{}", new TypeReference<Vector<Integer>>() {}).getOriginalMessage())
                .startsWith("Cannot deserialize value of type `dev.zazr.collection.Vector<java.lang.Integer>` from "
                        + "Object value");
        assertThat(failure("\"x\"", new TypeReference<HashSet<Integer>>() {}).getOriginalMessage())
                .startsWith("Cannot deserialize value of type `dev.zazr.collection.HashSet<java.lang.Integer>` from "
                        + "String value");
        assertThat(failure("[]", new TypeReference<HashMap<String, Integer>>() {})
                        .getOriginalMessage())
                .startsWith("Cannot deserialize value of type "
                        + "`dev.zazr.collection.HashMap<java.lang.String,java.lang.Integer>` from Array value");
        assertThat(failure("{}", new TypeReference<Tuple1<Integer>>() {}).getOriginalMessage())
                .startsWith("Cannot deserialize value of type `dev.zazr.Tuple1<java.lang.Integer>` from Object value");
    }

    record Lines(Vector<Integer> lines) {}

    record EmptyWhenNull(
            @JsonSetter(nulls = Nulls.AS_EMPTY) Vector<Integer> vector,
            @JsonSetter(nulls = Nulls.AS_EMPTY) HashMap<String, Integer> map) {}

    record NonEmptyWhenNull(
            @JsonSetter(nulls = Nulls.AS_EMPTY) NonEmptyVector<Integer> vector) {}

    record NonEmptyMapWhenNull(
            @JsonSetter(nulls = Nulls.AS_EMPTY) NonEmptyMap<String, Integer> map) {}

    @Test
    void aNullCollectionIsNullUnlessAskedToBeEmpty() {
        assertThat(read("{\"lines\":null}", Lines.class)).isEqualTo(new Lines(null));
        assertThat(read("{}", Lines.class)).isEqualTo(new Lines(null));
        assertThat(read("{\"vector\":null,\"map\":null}", EmptyWhenNull.class))
                .isEqualTo(new EmptyWhenNull(Vector.empty(), HashMap.empty()));
        assertThat(failure("{\"vector\":null}", NonEmptyWhenNull.class).getOriginalMessage())
                .isEqualTo("A NonEmptyVector needs at least one element: the JSON array is empty.");
        assertThat(failure("{\"map\":null}", NonEmptyMapWhenNull.class).getOriginalMessage())
                .isEqualTo("A NonEmptyMap needs at least one entry: the JSON object is empty.");
    }
}
