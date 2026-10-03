package dev.zazr.avaje.jsonb;

import dev.zazr.Tuple;
import dev.zazr.avaje.jsonb.TestRecords.Everything;
import dev.zazr.avaje.jsonb.TestRecords.Line;
import dev.zazr.avaje.jsonb.TestRecords.Order;
import dev.zazr.collection.HashMap;
import dev.zazr.collection.LinkedHashMap;
import dev.zazr.collection.List;
import dev.zazr.collection.NonEmptySortedMap;
import dev.zazr.collection.NonEmptyVector;
import dev.zazr.collection.TreeSet;
import dev.zazr.collection.Vector;
import dev.zazr.control.Option;
import io.avaje.jsonb.Jsonb;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/// Records holding Zazr fields, through the adapters avaje-jsonb's annotation processor generates for them.
class RecordsTest {

    private static final Jsonb JSONB = Jsonb.instance();

    private static final Order ORDER =
            new Order(Vector.of(new Line("a-1", 2), new Line("b-7", 1)), Option.some("gift"));

    @Test
    void anOrderRoundTrips() {
        var json = JSONB.toJson(ORDER);
        assertThat(json)
                .isEqualTo("{\"lines\":[{\"sku\":\"a-1\",\"quantity\":2},{\"sku\":\"b-7\",\"quantity\":1}],"
                        + "\"note\":\"gift\"}");
        assertThat(JSONB.type(Order.class).fromJson(json)).isEqualTo(ORDER);
    }

    @Test
    void noneIsWrittenAsNullAndReadBackAsNoneWhateverSerializeNulls() {
        var order = new Order(Vector.empty(), Option.none());
        for (var jsonb : new Jsonb[] {
            JSONB,
            Jsonb.builder().serializeNulls(false).build(),
            Jsonb.builder().serializeNulls(true).build()
        }) {
            var json = jsonb.toJson(order);
            assertThat(json).isEqualTo("{\"lines\":[],\"note\":null}");
            assertThat(jsonb.type(Order.class).fromJson(json)).isEqualTo(order);
        }
    }

    @Test
    void anExplicitNullReadsAsNone() {
        assertThat(JSONB.type(Order.class).fromJson("{\"lines\":[],\"note\":null}"))
                .isEqualTo(new Order(Vector.empty(), Option.none()));
    }

    @Test
    void anAbsentPropertyIsNullNotNone() {
        // avaje-jsonb's generated adapters give a missing property the value null; no adapter is called for it
        var order = JSONB.type(Order.class).fromJson("{\"lines\":[]}");
        assertThat(order.note()).isNull();
        assertThat(order.lines()).isEqualTo(Vector.empty());
    }

    @Test
    void serializeEmptyFalseLeavesOutAnEmptyCollectionPropertyButNotAnEmptyElement() {
        var jsonb = Jsonb.builder().serializeEmpty(false).build();
        assertThat(jsonb.toJson(new Order(Vector.empty(), Option.some("x")))).isEqualTo("{\"note\":\"x\"}");
        var everything = everything();
        assertThat(jsonb.toJson(everything)).contains("\"agenda\":{\"2026-10-03\":[],\"2026-10-04\":[\"standup\"]}");
    }

    @Test
    void aNullFieldIsNull() {
        var jsonb = Jsonb.builder().serializeNulls(true).build();
        var json = jsonb.toJson(new Everything(null, null, null, null, null, null, null, null));
        assertThat(json)
                .isEqualTo("{\"scores\":null,\"agenda\":null,\"linesByCustomer\":null,\"tags\":null,"
                        + "\"mandatory\":null,\"labels\":null,\"deadline\":null,\"parent\":null}");
        var back = jsonb.type(Everything.class).fromJson(json);
        assertThat(back.scores()).isNull();
        assertThat(back.agenda()).isNull();
        assertThat(back.deadline()).isNull();
        assertThat(back.parent()).isEqualTo(Option.none());
        assertThat(JSONB.toJson(new Everything(null, null, null, null, null, null, null, null)))
                .isEqualTo("{\"parent\":null}");
    }

    @Test
    void everyKindOfFieldRoundTrips() {
        var everything = everything();
        var json = JSONB.toJson(everything);
        assertThat(JSONB.type(Everything.class).fromJson(json)).isEqualTo(everything);
    }

    @Test
    void nestedGenericsAreWrittenAsNestedJson() {
        assertThat(JSONB.toJson(everything()))
                .isEqualTo("{\"scores\":[1,null,3],"
                        + "\"agenda\":{\"2026-10-03\":[],\"2026-10-04\":[\"standup\"]},"
                        + "\"linesByCustomer\":{\"c-2\":[{\"sku\":\"a-1\",\"quantity\":2}],\"c-1\":[]},"
                        + "\"tags\":[\"a\",\"b\"],"
                        + "\"mandatory\":[{\"sku\":\"b-7\",\"quantity\":1}],"
                        + "\"labels\":{\"1\":\"one\",\"2\":null},"
                        + "\"deadline\":[\"release\",null],"
                        + "\"parent\":{\"lines\":[],\"note\":null}}");
    }

    private static Everything everything() {
        return new Everything(
                Vector.of(Option.some(1), Option.none(), Option.some(3)),
                HashMap.of(LocalDate.of(2026, 10, 3), Vector.empty(), LocalDate.of(2026, 10, 4), Vector.of("standup")),
                LinkedHashMap.of("c-2", List.of(new Line("a-1", 2)), "c-1", List.empty()),
                TreeSet.of("b", "a"),
                NonEmptyVector.of(new Line("b-7", 1)),
                NonEmptySortedMap.of(Tuple.of(2, Option.<String>none()), Tuple.of(1, Option.some("one"))),
                Tuple.of("release", Option.<LocalDate>none()),
                Option.some(new Order(Vector.empty(), Option.none())));
    }
}
