package dev.zazr.jackson;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import dev.zazr.Tuple;
import dev.zazr.Tuple2;
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
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import tools.jackson.databind.DefaultTyping;
import tools.jackson.databind.exc.InvalidDefinitionException;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import tools.jackson.databind.jsontype.PolymorphicTypeValidator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/// Type ids, from `@JsonTypeInfo` or from default typing: the id written is the public Zazr type, never a
/// `List.Cons`, a `List.Nil` or a class of `dev.zazr.collection.internal`, and the value reads back.
class TypeIdTest {

    private static final PolymorphicTypeValidator VALIDATOR = BasicPolymorphicTypeValidator.builder()
            .allowIfSubType("dev.zazr.")
            .allowIfSubType("java.")
            .build();

    private static final JsonMapper MAPPER = JsonMapper.builder()
            .addModule(new ZazrModule())
            .polymorphicTypeValidator(VALIDATOR)
            .build();

    private static final JsonMapper DEFAULT_TYPING = JsonMapper.builder()
            .addModule(new ZazrModule())
            .activateDefaultTyping(VALIDATOR, DefaultTyping.NON_FINAL)
            .build();

    record Typed(@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS) Object value) {}

    /// Each value and the public type its id names. The element and key types are `Object` here, which the sorted
    /// types cannot read (see [#sortedTypesInAnObjectPropertyAreWrittenWithTheirPublicId]).
    static Stream<Object[]> values() {
        return Stream.of(
                new Object[] {Vector.of(1, 2), Vector.class},
                new Object[] {Vector.empty(), Vector.class},
                new Object[] {List.of(1, 2), List.class},
                new Object[] {List.empty(), List.class},
                new Object[] {Queue.of(1, 2), Queue.class},
                new Object[] {LazyList.of(1, 2), LazyList.class},
                new Object[] {LazyList.empty(), LazyList.class},
                new Object[] {LazyList.from(1).take(2).map(i -> i * 10), LazyList.class},
                new Object[] {HashSet.of(1), HashSet.class},
                new Object[] {LinkedHashSet.of(2, 1), LinkedHashSet.class},
                new Object[] {NonEmptyVector.of(1), NonEmptyVector.class},
                new Object[] {NonEmptySet.of(1), NonEmptySet.class},
                new Object[] {HashMap.of("a", 1), HashMap.class},
                new Object[] {LinkedHashMap.of("b", 2, "a", 1), LinkedHashMap.class},
                new Object[] {NonEmptyMap.of(Tuple.of("a", 1)), NonEmptyMap.class},
                new Object[] {Tuple.of(1), dev.zazr.Tuple1.class},
                new Object[] {Tuple.of(1, "a", true, 2, "b", false, 3, "c"), dev.zazr.Tuple8.class});
    }

    @ParameterizedTest
    @MethodSource("values")
    void theTypeIdIsThePublicTypeAndReadsBack(Object value, Class<?> publicType) {
        var json = MAPPER.writeValueAsString(new Typed(value));
        assertThat(json)
                // an array is wrapped with its id, an object carries it in its first property
                .containsAnyOf("[\"" + publicType.getName() + "\",", "{\"@class\":\"" + publicType.getName() + "\"")
                .doesNotContain("$")
                .doesNotContain(".internal.");
        var back = MAPPER.readValue(json, Typed.class).value();
        assertThat(back).isEqualTo(value);
        assertThat(publicType).isAssignableFrom(back.getClass());
    }

    @Test
    void theIdsOfTheClassesOfListAndLazyListReadAsListAndLazyList() {
        assertThat(MAPPER.readValue("{\"value\":[\"dev.zazr.collection.List$Cons\",[1,2]]}", Typed.class))
                .isEqualTo(new Typed(List.of(1, 2)));
        assertThat(MAPPER.readValue("{\"value\":[\"dev.zazr.collection.List$Nil\",[]]}", Typed.class))
                .isEqualTo(new Typed(List.empty()));
        assertThat(MAPPER.readValue(
                        "{\"value\":[\"dev.zazr.collection.internal.LazyCell$Deferred\",[1]]}", Typed.class))
                .isEqualTo(new Typed(LazyList.of(1)));
    }

    @Test
    void sortedTypesInAnObjectPropertyAreWrittenWithTheirPublicId() {
        for (var value : java.util.List.<Object>of(
                TreeSet.of(1), TreeMap.of("a", 1), NonEmptySortedSet.of(1), NonEmptySortedMap.of(Tuple.of("a", 1)))) {
            var json = MAPPER.writeValueAsString(new Typed(value));
            assertThat(json).contains("\"" + value.getClass().getName() + "\"");
            // the element or key type is Object, which has no natural order
            assertThatThrownBy(() -> MAPPER.readValue(json, Typed.class))
                    .isInstanceOf(InvalidDefinitionException.class)
                    .hasMessageContaining("does not implement Comparable");
        }
    }

    /// An `Option` is written as its value, with the type id of the value, as Jackson does for `Optional`.
    @Test
    void anOptionInAnObjectPropertyIsWrittenAsItsValue() {
        assertThat(MAPPER.writeValueAsString(new Typed(Option.some(Vector.of(1)))))
                .isEqualTo("{\"value\":[\"dev.zazr.collection.Vector\",[1]]}");
        assertThat(MAPPER.readValue(MAPPER.writeValueAsString(new Typed(Option.some(Vector.of(1)))), Typed.class))
                .isEqualTo(new Typed(Vector.of(1)));
    }

    record Holder(
            List<Integer> list,
            List<Integer> emptyList,
            LazyList<Integer> lazy,
            Queue<Integer> queue,
            Set<Integer> set,
            SortedSet<Integer> sortedSet,
            Map<String, Integer> map,
            SortedMap<String, Integer> sortedMap,
            NonEmptyVector<Integer> nonEmptyVector,
            NonEmptySortedSet<Integer> nonEmptySortedSet,
            NonEmptySortedMap<String, Integer> nonEmptySortedMap,
            Option<String> some,
            Option<String> none,
            Option<List<Integer>> optionOfList,
            Tuple2<Integer, List<String>> tuple,
            Tuple2<Integer, List<String>> tupleWithNull,
            Object any) {}

    /// A property declared as `Traversable` is left out: it reads only with `@JsonDeserialize(as = ...)`.
    @Test
    void defaultTypingRoundTrips() {
        var holder = new Holder(
                List.of(1, 2),
                List.empty(),
                LazyList.of(3),
                Queue.of(4),
                LinkedHashSet.of(7, 6),
                TreeSet.of(9, 8),
                LinkedHashMap.of("a", 1),
                TreeMap.of("b", 2),
                NonEmptyVector.of(10),
                NonEmptySortedSet.of(12, 11),
                NonEmptySortedMap.of(Tuple.of("c", 3)),
                Option.some("x"),
                Option.none(),
                Option.some(List.of(13)),
                Tuple.of(14, List.of("y")),
                Tuple.of(16, null),
                HashMap.of("k", List.of(15)));
        var json = DEFAULT_TYPING.writeValueAsString(holder);
        assertThat(json).doesNotContain("$").doesNotContain(".internal.");
        assertThat(json).contains("[\"dev.zazr.collection.List\",[1,2]]");
        assertThat(json).contains("[\"dev.zazr.collection.LazyList\",[3]]");
        var back = DEFAULT_TYPING.readValue(json, Holder.class);
        assertThat(back).isEqualTo(holder);
        assertThat(back.set()).isInstanceOf(LinkedHashSet.class).containsExactly(7, 6);
        assertThat(back.sortedSet()).isInstanceOf(TreeSet.class);
    }
}
