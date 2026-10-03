package dev.zazr.avaje.jsonb;

import dev.zazr.Tuple;
import dev.zazr.collection.HashMap;
import dev.zazr.collection.LinkedHashMap;
import dev.zazr.collection.NonEmptyMap;
import dev.zazr.collection.TreeMap;
import io.avaje.jsonb.JsonType;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;

import static dev.zazr.avaje.jsonb.JsonTypes.json;
import static org.assertj.core.api.Assertions.assertThat;

/// The keys of the maps: a `String` key is the property name, any other key the text of its JSON value.
class KeysTest {

    /// `map`, a map of `keyType` to `Integer`, written as `expected` and read back from it.
    private static <M> void roundTrips(Class<?> raw, Type keyType, M map, String expected) {
        JsonType<M> json = json(raw, keyType, Integer.class);
        assertThat(json.toJson(map)).isEqualTo(expected);
        assertThat(json.fromJson(expected)).isEqualTo(map);
    }

    @Test
    void aNumberKeyIsItsDecimalText() {
        roundTrips(TreeMap.class, Integer.class, TreeMap.of(-3, 1, 2, 2, 10, 3), "{\"-3\":1,\"2\":2,\"10\":3}");
        roundTrips(TreeMap.class, Long.class, TreeMap.of(1L << 40, 1), "{\"1099511627776\":1}");
        roundTrips(LinkedHashMap.class, Double.class, LinkedHashMap.of(1.5, 1, -0.25, 2), "{\"1.5\":1,\"-0.25\":2}");
        roundTrips(
                LinkedHashMap.class, BigDecimal.class, LinkedHashMap.of(new BigDecimal("12.50"), 1), "{\"12.50\":1}");
    }

    @Test
    void aBooleanKeyIsTrueOrFalse() {
        roundTrips(TreeMap.class, Boolean.class, TreeMap.of(true, 1, false, 0), "{\"false\":0,\"true\":1}");
    }

    @Test
    void aKeyWrittenAsAStringIsThatString() {
        roundTrips(
                TreeMap.class,
                LocalDate.class,
                TreeMap.of(LocalDate.of(2026, 10, 3), 1, LocalDate.of(2025, 1, 31), 2),
                "{\"2025-01-31\":2,\"2026-10-03\":1}");
        var id = UUID.fromString("3f2b8a54-1c3d-4e5f-8a9b-0c1d2e3f4a5b");
        roundTrips(HashMap.class, UUID.class, HashMap.of(id, 7), "{\"3f2b8a54-1c3d-4e5f-8a9b-0c1d2e3f4a5b\":7}");
        roundTrips(
                LinkedHashMap.class,
                DayOfWeek.class,
                LinkedHashMap.of(DayOfWeek.MONDAY, 1, DayOfWeek.SUNDAY, 7),
                "{\"MONDAY\":1,\"SUNDAY\":7}");
        roundTrips(
                NonEmptyMap.class,
                Instant.class,
                NonEmptyMap.of(Tuple.of(Instant.parse("2026-10-03T10:15:30Z"), 1)),
                "{\"2026-10-03T10:15:30Z\":1}");
    }

    @Test
    void aStringKeyIsEscapedAsAPropertyName() {
        roundTrips(
                LinkedHashMap.class,
                String.class,
                LinkedHashMap.of("a\"b", 1, "c\\d", 2, "é\n", 3, "", 4),
                "{\"a\\\"b\":1,\"c\\\\d\":2,\"é\\n\":3,\"\":4}");
    }

    @Test
    void aKeyThatIsAStringWithTheTextOfANumberStaysAString() {
        roundTrips(TreeMap.class, String.class, TreeMap.of("1", 1, "true", 2), "{\"1\":1,\"true\":2}");
    }

    @Test
    void aKeyOfAnUnknownTypeIsReadAsAString() {
        JsonType<HashMap<Object, Integer>> json = json(HashMap.class, Object.class, Integer.class);
        assertThat(json.fromJson("{\"1\":1}")).isEqualTo(HashMap.of("1", 1));
        assertThat(json.toJson(HashMap.of(1, 1))).isEqualTo("{\"1\":1}");
    }
}
