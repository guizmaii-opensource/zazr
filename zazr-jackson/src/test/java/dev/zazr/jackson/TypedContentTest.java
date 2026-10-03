package dev.zazr.jackson;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import dev.zazr.Tuple;
import dev.zazr.Tuple2;
import dev.zazr.collection.HashMap;
import dev.zazr.collection.LazyList;
import dev.zazr.collection.LinkedHashMap;
import dev.zazr.collection.List;
import dev.zazr.collection.Map;
import dev.zazr.collection.NonEmptyMap;
import dev.zazr.collection.NonEmptySortedMap;
import dev.zazr.collection.SortedMap;
import dev.zazr.collection.TreeMap;
import dev.zazr.collection.Vector;
import dev.zazr.control.Option;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import tools.jackson.databind.DatabindException;
import tools.jackson.databind.DefaultTyping;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import tools.jackson.databind.jsontype.PolymorphicTypeValidator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/// Type ids inside Zazr values: on the elements of a collection property annotated with `@JsonTypeInfo`, on the
/// components of a tuple (with and without default typing), and with `@JsonTypeInfo(use = NAME)`.
class TypedContentTest {

    private static final PolymorphicTypeValidator VALIDATOR = BasicPolymorphicTypeValidator.builder()
            .allowIfSubType("dev.zazr.")
            .allowIfSubType("java.")
            .build();

    private static JsonMapper mapper(DefaultTyping typing) {
        var builder = JsonMapper.builder().addModule(new ZazrModule()).polymorphicTypeValidator(VALIDATOR);
        return typing == null
                ? builder.build()
                : builder.activateDefaultTyping(VALIDATOR, typing).build();
    }

    record Pt(int x) {}

    record ContentTyped(
            @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS) List<Object> list,
            @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS) Vector<Object> vector,
            @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS) LazyList<Object> lazy,
            @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS) java.util.List<Object> jdk) {}

    /// `@JsonTypeInfo` on a collection property puts a type id on each element, as for a `java.util.List`.
    @Test
    void theElementsOfATypedCollectionPropertyCarryTheirTypeId() {
        var mapper = mapper(null);
        var value = new ContentTyped(
                List.of(new Pt(1)), Vector.of(new Pt(2)), LazyList.of(new Pt(3)), java.util.List.of(new Pt(4)));
        var json = mapper.writeValueAsString(value);
        var id = "{\"@class\":\"dev.zazr.jackson.TypedContentTest$Pt\",\"x\":";
        assertThat(json)
                .isEqualTo("{\"list\":[" + id + "1}],\"vector\":[" + id + "2}],\"lazy\":[" + id + "3}],\"jdk\":[" + id
                        + "4}]}");
        assertThat(mapper.readValue(json, ContentTyped.class)).isEqualTo(value);
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "@type")
    @JsonSubTypes({@JsonSubTypes.Type(value = Dog.class, name = "dog")})
    sealed interface Animal permits Dog {}

    record Dog(String n) implements Animal {}

    record VectorOfPt(Tuple2<Integer, Vector<Pt>> t) {}

    record VectorOfVector(Tuple2<Integer, Vector<Vector<Integer>>> t) {}

    record VectorOfList(Tuple2<Integer, Vector<List<Integer>>> t) {}

    record OptionOfVector(Tuple2<Integer, Option<Vector<Integer>>> t) {}

    record ListOfVector(Tuple2<Integer, List<Vector<Integer>>> t) {}

    record Animals(Tuple2<Animal, List<Animal>> t) {}

    record Objects(Tuple2<Object, Object> t) {}

    record MapOfLazy(Tuple2<HashMap<String, LazyList<Integer>>, Option<List<Pt>>> t) {}

    static Stream<Arguments> tuples() {
        var values = java.util.List.<Object>of(
                new VectorOfPt(Tuple.of(1, Vector.of(new Pt(2)))),
                new VectorOfVector(Tuple.of(1, Vector.of(Vector.of(2)))),
                new VectorOfList(Tuple.of(1, Vector.of(List.of(2), List.empty()))),
                new OptionOfVector(Tuple.of(1, Option.some(Vector.of(2)))),
                new ListOfVector(Tuple.of(1, List.of(Vector.of(2)))),
                new Animals(Tuple.of(new Dog("a"), List.of(new Dog("b")))),
                new Objects(Tuple.of(new Dog("c"), 1)),
                new MapOfLazy(Tuple.of(HashMap.of("k", LazyList.of(1)), Option.some(List.of(new Pt(3))))));
        var typings = java.util.Arrays.asList(null, DefaultTyping.JAVA_LANG_OBJECT, DefaultTyping.NON_FINAL);
        // without default typing, a component declared as Object reads as a JSON map
        // (aComponentDeclaredAsObjectReadsAsAJsonMap)
        return typings.stream()
                .flatMap(typing -> values.stream()
                        .filter(value -> typing != null || !(value instanceof Objects))
                        .map(value -> Arguments.of(typing, value)));
    }

    /// A tuple writes each component with the type id its declared type takes, at any depth, as it reads it.
    @ParameterizedTest
    @MethodSource("tuples")
    void tupleComponentsRoundTrip(DefaultTyping typing, Object value) {
        var mapper = mapper(typing);
        var json = mapper.writeValueAsString(value);
        assertThat(json).doesNotContain("$Cons").doesNotContain("$Nil").doesNotContain(".internal.");
        assertThat(mapper.readValue(json, value.getClass())).as(json).isEqualTo(value);
    }

    @Test
    void aClassLevelTypeIdOfATupleComponentIsKept() {
        assertThat(mapper(null).writeValueAsString(new Animals(Tuple.of(new Dog("a"), List.of(new Dog("b"))))))
                .isEqualTo("{\"t\":[{\"@type\":\"dog\",\"n\":\"a\"},[{\"@type\":\"dog\",\"n\":\"b\"}]]}");
    }

    /// Without default typing, a component declared as `Object` is written with the type id its class takes on its
    /// own, as the root value would be, and reads back as a JSON map, as any `Object` does.
    @Test
    void aComponentDeclaredAsObjectReadsAsAJsonMap() {
        var mapper = mapper(null);
        var json = mapper.writeValueAsString(new Objects(Tuple.of(new Dog("c"), 1)));
        assertThat(json).isEqualTo("{\"t\":[{\"@type\":\"dog\",\"n\":\"c\"},1]}");
        assertThat(mapper.readValue(json, Objects.class).t()._1())
                .isEqualTo(java.util.Map.of("@type", "dog", "n", "c"));
    }

    /// Components declared as `Object` (a root tuple) are written by their runtime type.
    @Test
    void componentsOfUnknownTypeAreWrittenByTheirRuntimeType() {
        var tuple = Tuple.of(
                List.of(1), LazyList.of(2), Option.some(3), HashMap.of("a", List.of(4)), Tuple.of(5, List.empty()));
        assertThat(mapper(null).writeValueAsString(tuple)).isEqualTo("[[1],[2],3,{\"a\":[4]},[5,[]]]");
    }

    record Named(
            @JsonTypeInfo(use = JsonTypeInfo.Id.NAME)
            @JsonSubTypes({
                @JsonSubTypes.Type(value = List.class, name = "list"),
                @JsonSubTypes.Type(value = LazyList.class, name = "lazy"),
                @JsonSubTypes.Type(value = Vector.class, name = "vector")
            })
            Object value) {}

    /// With `NAME`, the id is the name registered for the public type.
    @Test
    void nameIdsAreTheNamesOfThePublicTypes() {
        var mapper = mapper(null);
        for (var pair : java.util.List.of(
                Tuple.of(List.of(1, 2), "{\"value\":[\"list\",[1,2]]}"),
                Tuple.of(List.empty(), "{\"value\":[\"list\",[]]}"),
                Tuple.of(LazyList.of(1), "{\"value\":[\"lazy\",[1]]}"),
                Tuple.of(LazyList.from(1).take(2).map(i -> i * 10), "{\"value\":[\"lazy\",[10,20]]}"),
                Tuple.of(Vector.of(1), "{\"value\":[\"vector\",[1]]}"))) {
            var json = mapper.writeValueAsString(new Named(pair._1()));
            assertThat(json).isEqualTo(pair._2());
            assertThat(mapper.readValue(json, Named.class)).isEqualTo(new Named(pair._1()));
        }
    }

    /// Comparable, for the sorted maps.
    record Rank(int r) implements Comparable<Rank> {
        @Override
        public int compareTo(Rank that) {
            return Integer.compare(r, that.r);
        }
    }

    record TypedMaps(
            @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS) HashMap<String, Object> hashMap,
            @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS) LinkedHashMap<String, Object> linkedHashMap,
            @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS) Map<String, Object> map,
            @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS) TreeMap<String, Object> treeMap,
            @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS) SortedMap<String, Object> sortedMap,
            @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS) NonEmptyMap<String, Object> nonEmptyMap,
            @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS) NonEmptySortedMap<String, Object> nonEmptySortedMap,

            @JsonTypeInfo(use = JsonTypeInfo.Id.MINIMAL_CLASS)
            HashMap<String, Object> minimal,

            @JsonTypeInfo(use = JsonTypeInfo.Id.NAME)
            @JsonSubTypes({@JsonSubTypes.Type(value = Rank.class, name = "rank")})
            TreeMap<String, Object> named,

            @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS) java.util.Map<String, Object> jdk) {}

    /// `@JsonTypeInfo` on a map property puts a type id on each value, as for a `java.util.Map`.
    @Test
    void theValuesOfATypedMapPropertyCarryTheirTypeId() {
        var mapper = mapper(null);
        var value = new TypedMaps(
                HashMap.of("a", new Pt(1)),
                LinkedHashMap.of("b", new Pt(2), "a", new Pt(3)),
                HashMap.of("c", new Pt(4)),
                TreeMap.of("d", new Pt(5)),
                TreeMap.of("e", new Pt(6)),
                NonEmptyMap.of(Tuple.of("f", new Pt(7))),
                NonEmptySortedMap.of(Tuple.of("g", new Rank(8))),
                HashMap.of("h", new Pt(9)),
                TreeMap.of("i", new Rank(10)),
                java.util.Map.of("j", new Pt(11)));
        var json = mapper.writeValueAsString(value);
        var pt = "{\"@class\":\"dev.zazr.jackson.TypedContentTest$Pt\",\"x\":";
        assertThat(json)
                .isEqualTo("{\"hashMap\":{\"a\":" + pt + "1}},\"linkedHashMap\":{\"b\":" + pt + "2},\"a\":" + pt
                        + "3}},\"map\":{\"c\":" + pt + "4}},\"treeMap\":{\"d\":" + pt + "5}},\"sortedMap\":{\"e\":" + pt
                        + "6}},\"nonEmptyMap\":{\"f\":" + pt + "7}},\"nonEmptySortedMap\":{\"g\":{\"@class\":"
                        + "\"dev.zazr.jackson.TypedContentTest$Rank\",\"r\":8}},\"minimal\":{\"h\":{\"@c\":"
                        + "\"dev.zazr.jackson.TypedContentTest$Pt\",\"x\":9}},\"named\":{\"i\":{\"@type\":\"rank\",\"r\":10}},"
                        + "\"jdk\":{\"j\":" + pt + "11}}}");
        assertThat(mapper.readValue(json, TypedMaps.class)).isEqualTo(value);
    }

    record Inner(@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS) HashMap<String, Object> values) {}

    record Outer(
            @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS) Vector<Object> items,
            @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS) NonEmptyMap<String, Object> byKey,
            List<Inner> plain) {}

    /// A typed map property inside a value held by a typed collection or a typed map: every level carries its ids.
    @Test
    void typedMapsNestedInTypedContainersReadBack() {
        var mapper = mapper(null);
        var inner = new Inner(HashMap.of("a", new Pt(1), "b", Vector.of(2)));
        var value = new Outer(Vector.of(inner, new Pt(3)), NonEmptyMap.of(Tuple.of("k", inner)), List.of(inner, inner));
        var json = mapper.writeValueAsString(value);
        assertThat(json)
                .contains("{\"@class\":\"dev.zazr.jackson.TypedContentTest$Inner\",\"values\":{")
                .contains("\"a\":{\"@class\":\"dev.zazr.jackson.TypedContentTest$Pt\",\"x\":1}")
                .contains("\"b\":[\"dev.zazr.collection.Vector\",[2]]");
        assertThat(mapper.readValue(json, Outer.class)).isEqualTo(value);
    }

    record NamedOption(
            @JsonTypeInfo(use = JsonTypeInfo.Id.NAME)
            @JsonSubTypes({
                @JsonSubTypes.Type(value = List.class, name = "list"),
                @JsonSubTypes.Type(value = LazyList.class, name = "lazy")
            })
            Option<List<Integer>> list,

            @JsonTypeInfo(use = JsonTypeInfo.Id.NAME)
            @JsonSubTypes({@JsonSubTypes.Type(value = LazyList.class, name = "lazy")})
            Option<LazyList<Integer>> lazy) {}

    /// The workaround the Jackson page gives for `NAME` on an `Option` of a list: name the public type.
    @Test
    void nameOnAnOptionOfAListReadsBackWhenTheListIsNamed() {
        var mapper = mapper(null);
        var value = new NamedOption(Option.some(List.of(1, 2)), Option.some(LazyList.of(3)));
        var json = mapper.writeValueAsString(value);
        assertThat(json).isEqualTo("{\"list\":[\"list\",[1,2]],\"lazy\":[\"lazy\",[3]]}");
        assertThat(mapper.readValue(json, NamedOption.class)).isEqualTo(value);
    }

    record IntAndString(Tuple2<Integer, String> t) {}

    /// Documented as not covered: a component of another type than its declared one fails when written.
    @Test
    @SuppressWarnings("unchecked")
    void aComponentOfTheWrongTypeFailsWhenWritten() {
        var polluted = (Tuple2<Integer, String>) (Tuple2<?, ?>) Tuple.of("x", "s");
        assertThatThrownBy(() -> mapper(null).writeValueAsString(new IntAndString(polluted)))
                .isInstanceOf(DatabindException.class)
                .hasMessageContaining("not subtype of");
    }
}
