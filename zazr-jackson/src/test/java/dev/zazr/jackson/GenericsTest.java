package dev.zazr.jackson;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import dev.zazr.Tuple;
import dev.zazr.Tuple2;
import dev.zazr.collection.HashMap;
import dev.zazr.collection.HashSet;
import dev.zazr.collection.LazyList;
import dev.zazr.collection.LinkedHashMap;
import dev.zazr.collection.List;
import dev.zazr.collection.Map;
import dev.zazr.collection.NonEmptyMap;
import dev.zazr.collection.NonEmptyVector;
import dev.zazr.collection.Queue;
import dev.zazr.collection.Set;
import dev.zazr.collection.SortedMap;
import dev.zazr.collection.SortedSet;
import dev.zazr.collection.Traversable;
import dev.zazr.collection.TreeMap;
import dev.zazr.collection.TreeSet;
import dev.zazr.collection.Vector;
import dev.zazr.control.Option;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.MapperFeature;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.annotation.JsonDeserialize;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.jsontype.BasicPolymorphicTypeValidator;

import static dev.zazr.jackson.Json.read;
import static dev.zazr.jackson.Json.write;
import static org.assertj.core.api.Assertions.assertThat;

/// Generic types are honoured when reading, at any depth; properties declared as the interfaces read into the
/// concrete types; map keys go through Jackson's key (de)serializers.
class GenericsTest {

    record Line(String sku, int quantity) {}

    record Order(Vector<Line> lines, Option<String> note) {}

    record Declared(
            Set<Integer> set,
            SortedSet<String> sortedSet,
            Map<String, Integer> map,
            SortedMap<Integer, String> sortedMap) {}

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "kind")
    @JsonSubTypes({
        @JsonSubTypes.Type(value = Circle.class, name = "circle"),
        @JsonSubTypes.Type(value = Square.class, name = "square")
    })
    sealed interface Shape permits Circle, Square {}

    record Circle(double radius) implements Shape {}

    record Square(double side) implements Shape {}

    record Shapes(Vector<Shape> all, Tuple2<Shape, Shape> pair, HashMap<String, Shape> named, Option<Shape> maybe) {}

    record Holder(Object value) {}

    record Declarations(Traversable<Integer> traversable, Iterable<String> iterable) {}

    @Test
    void theOrderOfTheIssueReadsAndWrites() {
        var json = "{\"lines\":[{\"sku\":\"a\",\"quantity\":2},{\"sku\":\"b\",\"quantity\":1}],\"note\":\"fragile\"}";
        var order = read(json, Order.class);
        assertThat(order).isEqualTo(new Order(Vector.of(new Line("a", 2), new Line("b", 1)), Option.some("fragile")));
        assertThat(read(write(order), Order.class)).isEqualTo(order);
    }

    @Test
    void anOptionInsideACollectionReadsNullAsNone() {
        var read = read("[1,null,3]", new TypeReference<Vector<Option<Integer>>>() {});
        assertThat(read).isEqualTo(Vector.of(Option.some(1), Option.none(), Option.some(3)));
        assertThat(write(read)).isEqualTo("[1,null,3]");
    }

    @Test
    void mapKeysAndValuesHonourTheirTypes() {
        var type = new TypeReference<HashMap<LocalDate, Vector<String>>>() {};
        var read = read("{\"2026-10-03\":[\"a\",\"b\"],\"2026-10-04\":[]}", type);
        assertThat(read)
                .isEqualTo(HashMap.of(
                        LocalDate.of(2026, 10, 3), Vector.of("a", "b"), LocalDate.of(2026, 10, 4), Vector.empty()));
        assertThat(read(write(read), type)).isEqualTo(read);
    }

    @Test
    void nonStringKeysUseTheKeyDeserializers() {
        var id = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
        assertThat(read("{\"" + id + "\":1}", new TypeReference<HashMap<UUID, Integer>>() {}))
                .isEqualTo(HashMap.of(id, 1));
        assertThat(read("{\"MONDAY\":1}", new TypeReference<LinkedHashMap<DayOfWeek, Integer>>() {}))
                .isEqualTo(LinkedHashMap.of(DayOfWeek.MONDAY, 1));
        var numbers = read("{\"10\":\"ten\",\"9\":\"nine\"}", new TypeReference<TreeMap<Integer, String>>() {});
        assertThat(numbers.keySet()).containsExactly(9, 10);
        assertThat(write(numbers)).isEqualTo("{\"9\":\"nine\",\"10\":\"ten\"}");
        assertThat(write(HashMap.of(DayOfWeek.MONDAY, 1))).isEqualTo("{\"MONDAY\":1}");
        assertThat(write(HashMap.of(LocalDate.of(2026, 10, 3), 1))).isEqualTo("{\"2026-10-03\":1}");
    }

    @Test
    void propertiesDeclaredAsTheInterfacesReadIntoTheConcreteTypes() {
        var declared = read(
                "{\"set\":[1,2],\"sortedSet\":[\"b\",\"a\"],\"map\":{\"a\":1},\"sortedMap\":{\"2\":\"b\",\"1\":\"a\"}}",
                Declared.class);
        assertThat(declared.set()).isInstanceOf(HashSet.class).isEqualTo(HashSet.of(1, 2));
        assertThat(declared.sortedSet()).isInstanceOf(TreeSet.class).isEqualTo(TreeSet.of("a", "b"));
        assertThat(declared.map()).isInstanceOf(HashMap.class).isEqualTo(HashMap.of("a", 1));
        assertThat(declared.sortedMap()).isInstanceOf(TreeMap.class).isEqualTo(TreeMap.of(1, "a", 2, "b"));
        assertThat(read(write(declared), Declared.class)).isEqualTo(declared);
    }

    @Test
    void nestedGenericTypes() {
        var type = new TypeReference<List<HashMap<String, Option<Tuple2<Integer, Vector<LocalDate>>>>>>() {};
        var value = List.of(
                HashMap.of("a", Option.some(Tuple.of(1, Vector.of(LocalDate.of(2026, 1, 1))))),
                HashMap.of("b", Option.<Tuple2<Integer, Vector<LocalDate>>>none()));
        var json = write(value);
        assertThat(json).isEqualTo("[{\"a\":[1,[\"2026-01-01\"]]},{\"b\":null}]");
        assertThat(read(json, type)).isEqualTo(value);

        var vectors = read("[[1],[],[2,3]]", new TypeReference<Vector<Vector<Integer>>>() {});
        assertThat(vectors).isEqualTo(Vector.of(Vector.of(1), Vector.empty(), Vector.of(2, 3)));
        assertThat(read("[[1,2]]", new TypeReference<Queue<NonEmptyVector<Integer>>>() {}))
                .isEqualTo(Queue.of(NonEmptyVector.of(1, 2)));
        assertThat(read("[1,2]", new TypeReference<Option<Vector<Integer>>>() {}))
                .isEqualTo(Option.some(Vector.of(1, 2)));
    }

    @Test
    void polymorphicElementsKeepTheirTypeInformation() {
        var shapes = new Shapes(
                Vector.of(new Circle(1), new Square(2)),
                Tuple.of(new Square(3), new Circle(4)),
                HashMap.of("c", new Circle(5)),
                Option.some(new Square(6)));
        var json = write(shapes);
        assertThat(json)
                .isEqualTo("{\"all\":[{\"kind\":\"circle\",\"radius\":1.0},{\"kind\":\"square\",\"side\":2.0}],"
                        + "\"pair\":[{\"kind\":\"square\",\"side\":3.0},{\"kind\":\"circle\",\"radius\":4.0}],"
                        + "\"named\":{\"c\":{\"kind\":\"circle\",\"radius\":5.0}},"
                        + "\"maybe\":{\"kind\":\"square\",\"side\":6.0}}");
        assertThat(read(json, Shapes.class)).isEqualTo(shapes);
    }

    @Test
    void valuesDeclaredAsObjectAreWrittenByTheirRuntimeType() {
        assertThat(write(new Holder(Vector.of(1, 2)))).isEqualTo("{\"value\":[1,2]}");
        assertThat(write(new Holder(List.of(1, 2)))).isEqualTo("{\"value\":[1,2]}");
        assertThat(write(new Holder(List.empty()))).isEqualTo("{\"value\":[]}");
        assertThat(write(new Holder(LazyList.of(1, 2)))).isEqualTo("{\"value\":[1,2]}");
        assertThat(write(new Holder(Option.some(1)))).isEqualTo("{\"value\":1}");
        assertThat(write(new Holder(Option.none()))).isEqualTo("{\"value\":null}");
        assertThat(write(new Holder(TreeMap.of("a", 1)))).isEqualTo("{\"value\":{\"a\":1}}");
        assertThat(write(new Holder(Tuple.of(1, 2)))).isEqualTo("{\"value\":[1,2]}");
        assertThat(write(new Declarations(Vector.of(1), List.of("a"))))
                .isEqualTo("{\"traversable\":[1],\"iterable\":[\"a\"]}");
    }

    @Test
    void theRootValueIsWrittenByItsType() {
        assertThat(write(List.of(1, 2))).isEqualTo("[1,2]");
        assertThat(write(Option.some(Vector.of(1)))).isEqualTo("[1]");
        assertThat(write(Vector.of(Option.none(), Option.some(2)))).isEqualTo("[null,2]");
        assertThat(write(Tuple.of(1, null))).isEqualTo("[1,null]");
        assertThat(write(LazyList.from(0).take(3))).isEqualTo("[0,1,2]");
    }

    record Included(
            @JsonInclude(JsonInclude.Include.NON_ABSENT) Option<String> note,
            @JsonInclude(JsonInclude.Include.NON_EMPTY) Vector<Integer> numbers,
            @JsonInclude(JsonInclude.Include.NON_EMPTY) HashMap<String, Integer> counts,

            @JsonInclude(value = JsonInclude.Include.NON_ABSENT, content = JsonInclude.Include.NON_ABSENT)
            HashMap<String, Option<Integer>> present) {}

    @Test
    void inclusionAnnotationsApply() {
        assertThat(write(new Included(Option.none(), Vector.empty(), HashMap.empty(), HashMap.of("a", Option.none()))))
                .isEqualTo("{\"present\":{}}");
        assertThat(write(new Included(
                        Option.some("x"), Vector.of(1), HashMap.of("a", 1), HashMap.of("b", Option.some(2)))))
                .isEqualTo("{\"note\":\"x\",\"numbers\":[1],\"counts\":{\"a\":1},\"present\":{\"b\":2}}");
    }

    @Test
    void mapEntriesAreOrderedByKeyWhenJacksonIsAskedTo() {
        var mapper = JsonMapper.builder()
                .addModule(new ZazrModule())
                .enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS)
                .build();
        var map = HashMap.ofEntries(Vector.range(0, 40).map(i -> Tuple.of(String.format("k%02d", i), i)));
        var expected = new StringBuilder("{");
        for (var i = 0; i < 40; i++) {
            expected.append(i == 0 ? "" : ",").append(String.format("\"k%02d\":%d", i, i));
        }
        assertThat(mapper.writeValueAsString(map))
                .isEqualTo(expected.append("}").toString());
    }

    @Test
    void staticTypingWritesTheSameJson() {
        var mapper = JsonMapper.builder()
                .addModule(new ZazrModule())
                .enable(MapperFeature.USE_STATIC_TYPING)
                .build();
        var order = new Order(Vector.of(new Line("a", 1)), Option.some("x"));
        assertThat(mapper.writeValueAsString(order)).isEqualTo(write(order));
        assertThat(mapper.writeValueAsString(Vector.of(Option.some(1), Option.none())))
                .isEqualTo("[1,null]");
    }

    record Typed(@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS) Object value) {}

    @Test
    void theTypeIdOfAZazrValueIsItsOwnClass() {
        var mapper = JsonMapper.builder()
                .addModule(new ZazrModule())
                .polymorphicTypeValidator(BasicPolymorphicTypeValidator.builder()
                        .allowIfSubType("dev.zazr.")
                        .build())
                .build();
        for (var value : java.util.List.<Object>of(
                HashMap.of("a", 1),
                LinkedHashMap.of("b", 2, "a", 1),
                NonEmptyMap.of(Tuple.of("a", 1)),
                Vector.of(1, 2),
                Tuple.of(1, "a"),
                HashSet.of(1))) {
            var json = mapper.writeValueAsString(new Typed(value));
            assertThat(json).contains("\"" + value.getClass().getName() + "\"");
            assertThat(mapper.readValue(json, Typed.class)).isEqualTo(new Typed(value));
        }
    }

    record AsVector(@JsonDeserialize(as = Vector.class) Traversable<LocalDate> dates) {}

    @Test
    void aPropertyDeclaredAsTraversableReadsIntoTheTypeItsAnnotationNames() {
        var read = read("{\"dates\":[\"2026-10-03\"]}", AsVector.class);
        assertThat(read.dates()).isInstanceOf(Vector.class).isEqualTo(Vector.of(LocalDate.of(2026, 10, 3)));
    }

    record AllAbsent(
            @JsonInclude(value = JsonInclude.Include.NON_EMPTY, content = JsonInclude.Include.NON_ABSENT)
            HashMap<String, Option<Integer>> zazr,

            @JsonInclude(value = JsonInclude.Include.NON_EMPTY, content = JsonInclude.Include.NON_ABSENT)
            java.util.Map<String, java.util.Optional<Integer>> jdk) {}

    /// A map whose every entry the content inclusion leaves out counts as empty, as Jackson does for a java.util.Map.
    @Test
    void aMapOfOnlyExcludedEntriesIsEmpty() {
        assertThat(write(new AllAbsent(
                        HashMap.of("a", Option.none()), java.util.Map.of("a", java.util.Optional.empty()))))
                .isEqualTo("{}");
        assertThat(write(new AllAbsent(
                        HashMap.of("a", Option.some(1)), java.util.Map.of("a", java.util.Optional.of(1)))))
                .isEqualTo("{\"zazr\":{\"a\":1},\"jdk\":{\"a\":1}}");
    }
}
