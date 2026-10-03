package dev.zazr.avaje.jsonb.docs;

import dev.zazr.Tuple;
import dev.zazr.Tuple2;
import dev.zazr.avaje.jsonb.docs.DocsAvajeJsonbExamplesTest.Defaults.Settings;
import dev.zazr.collection.HashMap;
import dev.zazr.collection.NonEmptyMap;
import dev.zazr.collection.NonEmptyVector;
import dev.zazr.collection.Vector;
import dev.zazr.control.Option;
import io.avaje.json.JsonDataException;
import io.avaje.jsonb.Json;
import io.avaje.jsonb.Jsonb;
import io.avaje.jsonb.Types;
import java.time.LocalDate;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/// The fenced `java` blocks of docs/avaje-jsonb.md and the avaje-jsonb blocks of the blog post docs/blog/posts/json.md,
/// pasted verbatim, compiled and run, with the results the pages state in their comments and text blocks.
/// `make docs-examples` fails when a block of those pages is missing from a docs example test.
public class DocsAvajeJsonbExamplesTest {

    private final Jsonb jsonb = Jsonb.instance();

    @Json
    public record Line(String sku, int quantity) {}

    @Json
    public record Order(Vector<Line> lines, Option<String> note) {}

    @Json
    public record Shipment(NonEmptyVector<Line> lines) {}

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

    /// The blog post "Zazr types in JSON, with Jackson and avaje-jsonb" (docs/blog/posts/json.md): its avaje-jsonb
    /// blocks, whose records are `Line`, `Order` and `Shipment` above, and the avaje-jsonb side of what its prose
    /// states. The JSON asserted here is the text that `DocsJacksonExamplesTest` asserts Jackson writes.
    @Nested
    class BlogJson {

        @Test
        void theDependencyAlone() {
            var jsonb = Jsonb.instance();
            var order = new Order(Vector.of(new Line("A-1", 2)), Option.some("ring twice"));
            var json = jsonb.toJson(order);
            var back = jsonb.type(Order.class).fromJson(json); // Order
            // json is {"lines":[{"sku":"A-1","quantity":2}],"note":"ring twice"}, back equals order

            Order typed = back;
            assertThat(json).isEqualTo("{\"lines\":[{\"sku\":\"A-1\",\"quantity\":2}],\"note\":\"ring twice\"}");
            assertThat(typed).isEqualTo(order);
        }

        @Test
        void nonEmptyStaysNonEmpty() {
            assertThatThrownBy(() -> {
                        jsonb.type(Shipment.class).fromJson("{\"lines\":[]}"); // throws JsonDataException
                    })
                    .isExactlyInstanceOf(JsonDataException.class)
                    .hasMessage(
                            "A NonEmptyVector needs at least one element: the JSON array is empty. (at position: 11, "
                                    + "following: `{\"lines\":[]`, before: `}`)");

            var maps = Types.newParameterizedType(NonEmptyMap.class, String.class, Integer.class);
            assertThatThrownBy(() -> jsonb.type(maps).fromJson("{}"))
                    .isExactlyInstanceOf(JsonDataException.class)
                    .hasMessageStartingWith("A NonEmptyMap needs at least one entry: the JSON object is empty.");
        }

        @Test
        void noNullInsideACollection() {
            var numbers = Types.newParameterizedType(Vector.class, Integer.class);
            assertThatThrownBy(() -> jsonb.type(numbers).fromJson("[1,null]"))
                    .isExactlyInstanceOf(JsonDataException.class)
                    .hasMessageStartingWith(
                            "Element 1 of the Vector is null: Zazr collections hold no null. Use Option "
                                    + "for a value that may be missing.");

            var options =
                    Types.newParameterizedType(Vector.class, Types.newParameterizedType(Option.class, Integer.class));
            assertThat(jsonb.type(options).fromJson("[1,null,3]").toString())
                    .isEqualTo("Vector(Some(1), None, Some(3))");

            var pairs = Types.newParameterizedType(Tuple2.class, Integer.class, Integer.class);
            assertThat(jsonb.type(pairs).fromJson("[1,null]")).isEqualTo(Tuple.of(1, null));
        }

        @Test
        void mapKeysOfAnyType() {
            var stock = HashMap.of(LocalDate.of(2026, 10, 4), 3);
            var type = Types.newParameterizedType(HashMap.class, LocalDate.class, Integer.class);

            var json = jsonb.toJson(stock);
            assertThat(json).isEqualTo("{\"2026-10-04\":3}");
            assertThat(jsonb.type(type).fromJson(json)).isEqualTo(stock);
        }

        @Test
        void aMissingOptionReadsAsNull() {
            assertThat(jsonb.type(Order.class).fromJson("{\"lines\":[]}")).isEqualTo(new Order(Vector.empty(), null));
        }
    }
}
