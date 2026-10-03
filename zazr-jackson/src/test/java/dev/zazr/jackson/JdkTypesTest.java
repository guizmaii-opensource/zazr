package dev.zazr.jackson;

import dev.zazr.collection.HashMap;
import dev.zazr.collection.TreeMap;
import dev.zazr.collection.TreeSet;
import dev.zazr.collection.Vector;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;

/// Registering the module changes nothing for the types it does not cover: the JDK types, a user-defined `Iterable`,
/// and the `asJava()` views, which are `java.util` collections, are written and read the same with and without it.
class JdkTypesTest {

    private static final JsonMapper PLAIN = JsonMapper.builder().build();

    record Person(
            String name,
            Optional<String> nickname,
            java.util.List<Integer> scores,
            java.util.Map<String, LocalDate> dates) {}

    /// An `Iterable` that is not a collection.
    record Numbers(int count) implements Iterable<Integer> {
        @Override
        public Iterator<Integer> iterator() {
            return java.util.stream.IntStream.range(0, count).iterator();
        }
    }

    static java.util.List<Object> values() {
        var arrayList = new ArrayList<String>();
        arrayList.add("a");
        var nullable = new java.util.HashMap<String, Object>();
        nullable.put("n", null);
        return java.util.List.of(
                java.util.List.of(1, 2),
                arrayList,
                java.util.Set.of("x"),
                new java.util.TreeMap<>(java.util.Map.of(2, "b", 1, "a")),
                nullable,
                Optional.of(1),
                Optional.empty(),
                new Person(
                        "Ada",
                        Optional.empty(),
                        java.util.List.of(1),
                        java.util.Map.of("born", LocalDate.of(1815, 12, 10))),
                new Numbers(3),
                new int[] {1, 2},
                UUID.fromString("123e4567-e89b-12d3-a456-426614174000"),
                Vector.of(1, 2).asJava(),
                TreeSet.of(3, 1).asJava(),
                HashMap.of("a", 1).asJavaMap(),
                TreeMap.of("b", 2, "a", 1).asJavaMap());
    }

    @ParameterizedTest
    @MethodSource("values")
    void writtenTheSame(Object value) {
        assertThat(Json.write(value)).isEqualTo(PLAIN.writeValueAsString(value));
    }

    /// The views are left out: their classes are not types to read into.
    @ParameterizedTest
    @MethodSource("values")
    void readTheSameAsTheirOwnType(Object value) {
        var json = PLAIN.writeValueAsString(value);
        var type = PLAIN.constructType(value.getClass());
        var view = value.getClass().getName().startsWith("dev.zazr.");
        if (!view && (!(value instanceof Iterable<?>) || value instanceof java.util.Collection<?>)) {
            Object plain = PLAIN.readValue(json, type);
            Object zazr = Json.MAPPER.readValue(json, type);
            assertThat(zazr).usingRecursiveComparison().isEqualTo(plain);
        }
    }

    @ParameterizedTest
    @MethodSource("values")
    void readTheSameAsUntypedJson(Object value) {
        var json = PLAIN.writeValueAsString(value);
        assertThat(Json.MAPPER.readValue(json, Object.class)).isEqualTo(PLAIN.readValue(json, Object.class));
        assertThat(Json.MAPPER.readTree(json)).isEqualTo(PLAIN.readTree(json));
    }

    record Generic(java.util.List<Optional<java.util.Map<String, java.util.Set<Integer>>>> nested) {}

    @ParameterizedTest
    @MethodSource("generics")
    void nestedJdkGenericsReadTheSame(String json) {
        var type = new TypeReference<Generic>() {};
        assertThat(Json.MAPPER.readValue(json, type)).isEqualTo(PLAIN.readValue(json, type));
    }

    static java.util.List<String> generics() {
        return java.util.List.of("{\"nested\":[null,{\"a\":[1,2]},{}]}", "{\"nested\":[]}", "{}", "{\"nested\":null}");
    }
}
