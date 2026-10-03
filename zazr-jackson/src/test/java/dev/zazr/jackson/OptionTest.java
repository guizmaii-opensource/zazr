package dev.zazr.jackson;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import dev.zazr.collection.HashMap;
import dev.zazr.collection.NonEmptyVector;
import dev.zazr.collection.Vector;
import dev.zazr.control.Option;
import java.util.Arrays;
import org.junit.jupiter.api.Test;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.exc.MismatchedInputException;
import tools.jackson.databind.json.JsonMapper;

import static dev.zazr.jackson.Json.read;
import static dev.zazr.jackson.Json.write;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/// `Option`: `None` is written as `null`; `null` and an absent creator property read as `None`.
class OptionTest {

    record Note(String title, Option<String> text) {}

    @Test
    void absentNullAndPresent() {
        assertThat(read("{\"title\":\"a\"}", Note.class)).isEqualTo(new Note("a", Option.none()));
        assertThat(read("{\"title\":\"a\",\"text\":null}", Note.class)).isEqualTo(new Note("a", Option.none()));
        assertThat(read("{\"title\":\"a\",\"text\":\"b\"}", Note.class)).isEqualTo(new Note("a", Option.some("b")));
        assertThat(write(new Note("a", Option.none()))).isEqualTo("{\"title\":\"a\",\"text\":null}");
        assertThat(write(new Note("a", Option.some("b")))).isEqualTo("{\"title\":\"a\",\"text\":\"b\"}");
    }

    /// A property set through a field or a setter is not touched when absent: it keeps the value the constructor gave.
    static final class Settable {
        public Option<String> initialised = Option.none();
        public Option<String> uninitialised;
    }

    @Test
    void anAbsentFieldKeepsItsInitialValue() {
        var read = read("{}", Settable.class);
        assertThat(read.initialised).isEqualTo(Option.none());
        assertThat(read.uninitialised).isNull();
        assertThat(read("{\"initialised\":null,\"uninitialised\":\"x\"}", Settable.class).uninitialised)
                .isEqualTo(Option.some("x"));
    }

    @Test
    void jacksonsFeatureForMissingReferenceValuesIsHonoured() {
        // USE_NULL_FOR_MISSING_REFERENCE_VALUES exists from Jackson 3.1 on
        assumeTrue(Arrays.stream(DeserializationFeature.values())
                .anyMatch(f -> f.name().equals("USE_NULL_FOR_MISSING_REFERENCE_VALUES")));
        var mapper = JsonMapper.builder()
                .addModule(new ZazrModule())
                .enable(DeserializationFeature.valueOf("USE_NULL_FOR_MISSING_REFERENCE_VALUES"))
                .build();
        assertThat(mapper.readValue("{\"title\":\"a\"}", Note.class)).isEqualTo(new Note("a", null));
        assertThat(mapper.readValue("{\"title\":\"a\",\"text\":null}", Note.class))
                .isEqualTo(new Note("a", Option.none()));
    }

    @Test
    void optionsInsideCollectionsAndMaps() {
        assertThat(read("[null,2]", new TypeReference<Vector<Option<Integer>>>() {}))
                .isEqualTo(Vector.of(Option.none(), Option.some(2)));
        assertThat(read("{\"a\":null,\"b\":1}", new TypeReference<HashMap<String, Option<Integer>>>() {}))
                .isEqualTo(HashMap.of("a", Option.none(), "b", Option.some(1)));
        assertThat(write(HashMap.of("a", Option.none()))).isEqualTo("{\"a\":null}");
    }

    @Test
    void anOptionOfACollection() {
        var type = new TypeReference<Option<NonEmptyVector<Integer>>>() {};
        assertThat(read("null", type)).isEqualTo(Option.none());
        assertThat(read("[1]", type)).isEqualTo(Option.some(NonEmptyVector.of(1)));
        assertThatThrownBy(() -> read("[]", type))
                .isInstanceOf(MismatchedInputException.class)
                .hasMessageStartingWith("A NonEmptyVector needs at least one element: the JSON array is empty.");
    }

    @Test
    void aValueTheContentReadsAsNullIsNone() {
        // an empty String is read as a null Integer
        assertThat(read("\"\"", new TypeReference<Option<Integer>>() {})).isEqualTo(Option.none());
    }

    @Test
    void nestedOptions() {
        var type = new TypeReference<Option<Option<Integer>>>() {};
        assertThat(read("1", type)).isEqualTo(Option.some(Option.some(1)));
        assertThat(read("null", type)).isEqualTo(Option.none());
    }

    record Name(String first, String last) {}

    static final class Person {
        @JsonUnwrapped
        public Option<Name> name = Option.none();

        public int age;
    }

    @Test
    void anUnwrappedOptionWritesTheFieldsOfItsValue() {
        var person = new Person();
        person.name = Option.some(new Name("Ada", "Lovelace"));
        person.age = 36;
        assertThat(write(person)).isEqualTo("{\"age\":36,\"first\":\"Ada\",\"last\":\"Lovelace\"}");
        person.name = Option.none();
        assertThat(write(person)).isEqualTo("{\"age\":36}");
    }

    @Test
    void readingUpdatesAnOptionInPlace() {
        var settable = new Settable();
        settable.initialised = Option.some("old");
        Json.MAPPER.readerForUpdating(settable).readValue("{\"initialised\":\"new\"}");
        assertThat(settable.initialised).isEqualTo(Option.some("new"));
    }
}
