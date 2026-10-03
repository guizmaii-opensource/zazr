package dev.zazr.avaje.jsonb.docs;

import dev.zazr.avaje.jsonb.docs.DocsAvajeJsonbExamplesTest.Defaults.Settings;
import dev.zazr.collection.HashMap;
import dev.zazr.collection.NonEmptyVector;
import dev.zazr.collection.Vector;
import dev.zazr.control.Option;
import io.avaje.json.JsonDataException;
import io.avaje.jsonb.Json;
import io.avaje.jsonb.Jsonb;
import io.avaje.jsonb.Types;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/// The fenced `java` blocks of docs/avaje-jsonb.md, pasted verbatim, compiled and run, with the results the page
/// states in its comments. `make docs-examples` fails when a block of the page is missing from this file.
public class DocsAvajeJsonbExamplesTest {

    private final Jsonb jsonb = Jsonb.instance();

    @Json
    public record Line(String sku, int quantity) {}

    @Json
    public record Order(Vector<Line> lines, Option<String> note) {}

    /// The records of the page that reassign a parameter of their compact constructor, as the page shows.
    @SuppressWarnings("Var")
    public static final class Defaults {

        private Defaults() {}

        @Json
        public record Settings(String theme, Option<String> locale) {

            public Settings {
                locale = locale == null ? Option.none() : locale;
            }
        }
    }

    @Test
    void readingAndWriting() {
        var jsonb = Jsonb.instance();
        var order = new Order(Vector.of(new Line("a-1", 2)), Option.none());

        var json = jsonb.toJson(order); // {"lines":[{"sku":"a-1","quantity":2}],"note":null}
        var back = jsonb.type(Order.class).fromJson(json); // Order, equal to order

        assertThat(json).isEqualTo("{\"lines\":[{\"sku\":\"a-1\",\"quantity\":2}],\"note\":null}");
        assertThat(back).isEqualTo(order);
    }

    @Test
    void theJsonOfEachType() {
        var agenda = HashMap.of(LocalDate.of(2026, 10, 3), Vector.of("standup"));
        var type = Types.newParameterizedType(
                HashMap.class, LocalDate.class, Types.newParameterizedType(Vector.class, String.class));

        var json = jsonb.toJson(agenda); // {"2026-10-03":["standup"]}
        var back = jsonb.<HashMap<LocalDate, Vector<String>>>type(type).fromJson(json);

        assertThat(json).isEqualTo("{\"2026-10-03\":[\"standup\"]}");
        assertThat(back).isEqualTo(agenda);
    }

    @Test
    void optionAndMissingProperties() {
        var settings = jsonb.type(Settings.class).fromJson("{\"theme\":\"dark\"}");

        var locale = settings.locale(); // Option<String>: None

        Option<String> typed = locale;
        assertThat(typed).isEqualTo(Option.none());
        assertThat(settings.theme()).isEqualTo("dark");
    }

    @Test
    void whatReadingRejects() {
        assertThatThrownBy(() -> {
                    var vectors = Types.newParameterizedType(NonEmptyVector.class, Integer.class);

                    jsonb.type(vectors).fromJson("[]"); // throws JsonDataException:
                    // A NonEmptyVector needs at least one element: the JSON array is empty. (at position: 2,
                    // following: `[]`)
                })
                .isExactlyInstanceOf(JsonDataException.class)
                .hasMessage(
                        "A NonEmptyVector needs at least one element: the JSON array is empty. (at position: 2, following: `[]`)");
    }
}
