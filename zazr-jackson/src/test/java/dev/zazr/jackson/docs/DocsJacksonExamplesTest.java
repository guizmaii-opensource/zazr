package dev.zazr.jackson.docs;

import dev.zazr.collection.HashMap;
import dev.zazr.collection.Vector;
import dev.zazr.control.Option;
import dev.zazr.control.Try;
import dev.zazr.jackson.ZazrModule;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;

/// The fenced `java` blocks of docs/jackson.md, pasted verbatim, compiled and run (zazr-core's `DocsExamplesTest`
/// cannot see zazr-jackson, which depends on zazr-core). `make docs-examples` fails when a block of that page is
/// missing from this file. Each test also checks the static types and the results the page states in its comments.
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
}
