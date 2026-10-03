package dev.zazr.jackson.docs;

import dev.zazr.Tuple;
import dev.zazr.Tuple2;
import dev.zazr.collection.HashMap;
import dev.zazr.collection.NonEmptyMap;
import dev.zazr.collection.NonEmptyVector;
import dev.zazr.collection.Vector;
import dev.zazr.control.Option;
import dev.zazr.control.Try;
import dev.zazr.jackson.ZazrModule;
import java.time.LocalDate;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;

/// The fenced `java` blocks of docs/jackson.md and the Jackson blocks of the blog post docs/blog/posts/json.md, pasted
/// verbatim, compiled and run (zazr-core's `DocsExamplesTest` cannot see zazr-jackson, which depends on zazr-core).
/// `make docs-examples` fails when a block of those pages is missing from a docs example test. Each test also checks
/// the static types and the results the page states in its comments.
class DocsJacksonExamplesTest {

    private static final JsonMapper MAPPER =
            JsonMapper.builder().addModule(new ZazrModule()).build();

    @Test
    void registerTheModule() {
        var mapper = JsonMapper.builder().addModule(new ZazrModule()).build(); // JsonMapper

        JsonMapper typed = mapper;
        assertThat(typed.writeValueAsString(Option.some(1))).isEqualTo("1");
    }

    @Test
    void aRecordThatHoldsZazrTypes() {
        var mapper = MAPPER;

        record Line(String sku, int quantity) {}

        record Order(Vector<Line> lines, Option<String> note) {}

        var order = new Order(Vector.of(new Line("A-1", 2)), Option.none());
        var json = mapper.writeValueAsString(order);
        var back = mapper.readValue(json, Order.class); // Order
        // json is {"lines":[{"sku":"A-1","quantity":2}],"note":null}, back equals order

        Order typed = back;
        assertThat(json).isEqualTo("{\"lines\":[{\"sku\":\"A-1\",\"quantity\":2}],\"note\":null}");
        assertThat(typed).isEqualTo(order);
    }

    @Test
    void readingHonoursTheDeclaredTypes() {
        var mapper = MAPPER;

        var byDate = new TypeReference<HashMap<LocalDate, Vector<String>>>() {};
        var dates = mapper.readValue("{\"2026-10-03\":[\"a\"]}", byDate);
        var counts = mapper.readValue("[1,null,3]", new TypeReference<Vector<Option<Integer>>>() {});
        // dates is HashMap((2026-10-03, Vector(a))), counts is Vector(Some(1), None, Some(3))

        HashMap<LocalDate, Vector<String>> typedDates = dates;
        Vector<Option<Integer>> typedCounts = counts;
        assertThat(typedDates.toString()).isEqualTo("HashMap((2026-10-03, Vector(a)))");
        assertThat(typedCounts.toString()).isEqualTo("Vector(Some(1), None, Some(3))");
    }

    @Test
    void whatFails() {
        var mapper = MAPPER;

        var numbers = new TypeReference<Vector<Integer>>() {};
        var failure = Try.of(() -> mapper.readValue("[1,null]", numbers)).getCause(); // Throwable
        // failure.getMessage() starts with "Element 1 of the Vector is null: Zazr collections hold no null."

        Throwable typed = failure;
        assertThat(typed.getMessage()).startsWith("Element 1 of the Vector is null: Zazr collections hold no null.");
    }

    /// The blog post "Zazr types in JSON, with Jackson and avaje-jsonb" (docs/blog/posts/json.md): its Jackson
    /// blocks, and the Jackson side of what its prose states. Its avaje-jsonb blocks are in
    /// `DocsAvajeJsonbExamplesTest`, which checks that avaje-jsonb writes the same JSON.
    @Nested
    class BlogJson {

        @Test
        void oneDependencyOneLine() {
            record Line(String sku, int quantity) {}

            record Order(Vector<Line> lines, Option<String> note) {}

            var mapper = JsonMapper.builder().addModule(new ZazrModule()).build(); // JsonMapper

            var order = new Order(Vector.of(new Line("A-1", 2)), Option.some("ring twice"));
            var json = mapper.writeValueAsString(order);
            var back = mapper.readValue(json, Order.class); // Order
            // json is {"lines":[{"sku":"A-1","quantity":2}],"note":"ring twice"}, back equals order

            JsonMapper typedMapper = mapper;
            Order typed = back;
            assertThat(typedMapper).isNotNull();
            assertThat(json).isEqualTo("{\"lines\":[{\"sku\":\"A-1\",\"quantity\":2}],\"note\":\"ring twice\"}");
            assertThat(typed).isEqualTo(order);
        }

        @Test
        void findAndAddModulesFindsTheModule() {
            var mapper = JsonMapper.builder().findAndAddModules().build();

            assertThat(mapper.registeredModules()).anyMatch(module -> module instanceof ZazrModule);
            assertThat(mapper.writeValueAsString(Vector.of(1, 2, 3))).isEqualTo("[1,2,3]");
            assertThat(mapper.readValue("[1,2,3]", new TypeReference<Vector<Integer>>() {}))
                    .isEqualTo(Vector.of(1, 2, 3));
        }

        @Test
        void nonEmptyStaysNonEmpty() {
            record Line(String sku, int quantity) {}

            record Shipment(NonEmptyVector<Line> lines) {}

            var failure = Try.of(() -> MAPPER.readValue("{\"lines\":[]}", Shipment.class))
                    .getCause();
            assertThat(failure.getMessage())
                    .startsWith("A NonEmptyVector needs at least one element: the JSON array is empty.\n at [");

            var maps = new TypeReference<NonEmptyMap<String, Integer>>() {};
            assertThat(Try.of(() -> MAPPER.readValue("{}", maps)).getCause().getMessage())
                    .startsWith("A NonEmptyMap needs at least one entry: the JSON object is empty.");
        }

        @Test
        void noNullInsideACollection() {
            var numbers = new TypeReference<Vector<Integer>>() {};
            assertThat(Try.of(() -> MAPPER.readValue("[1,null]", numbers))
                            .getCause()
                            .getMessage())
                    .startsWith(
                            "Element 1 of the Vector is null: Zazr collections hold no null. Use Option for a value "
                                    + "that may be missing.");

            var options = new TypeReference<Vector<Option<Integer>>>() {};
            assertThat(MAPPER.readValue("[1,null,3]", options).toString()).isEqualTo("Vector(Some(1), None, Some(3))");

            var pairs = new TypeReference<Tuple2<Integer, Integer>>() {};
            assertThat(MAPPER.readValue("[1,null]", pairs)).isEqualTo(Tuple.of(1, null));
        }

        @Test
        void mapKeysOfAnyType() {
            var mapper = MAPPER;

            var stock = HashMap.of(LocalDate.of(2026, 10, 4), 3);
            var json = mapper.writeValueAsString(stock); // {"2026-10-04":3}
            var back = mapper.readValue(json, new TypeReference<HashMap<LocalDate, Integer>>() {});
            // back equals stock

            assertThat(json).isEqualTo("{\"2026-10-04\":3}");
            assertThat(back).isEqualTo(stock);
        }

        @Test
        void aMissingOptionReadsAsNone() {
            record Line(String sku, int quantity) {}

            record Order(Vector<Line> lines, Option<String> note) {}

            assertThat(MAPPER.readValue("{\"lines\":[]}", Order.class))
                    .isEqualTo(new Order(Vector.empty(), Option.none()));
        }
    }
}
