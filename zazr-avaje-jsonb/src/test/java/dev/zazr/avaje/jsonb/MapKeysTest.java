package dev.zazr.avaje.jsonb;

import dev.zazr.avaje.jsonb.TestRecords.Escapes;
import dev.zazr.avaje.jsonb.TestRecords.Inner;
import dev.zazr.avaje.jsonb.TestRecords.MapOfMaps;
import dev.zazr.avaje.jsonb.TestRecords.MapOfRecords;
import dev.zazr.avaje.jsonb.TestRecords.Order;
import dev.zazr.collection.LinkedHashMap;
import dev.zazr.collection.Vector;
import io.avaje.json.JsonDataException;
import io.avaje.jsonb.JsonType;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.Test;

import static dev.zazr.avaje.jsonb.JsonTypes.JSONB;
import static dev.zazr.avaje.jsonb.JsonTypes.json;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/// The property names of a map read from JSON: no key is ever lost or replaced by another, wherever the map is.
///
/// avaje-jsonb 3.16 decodes the escapes of a property name (`\"`, `\\`, `\n`, `é`) only in an object read at the
/// top level of a fresh parser. Inside an object read with property names (a record), and on a thread whose earlier
/// read failed inside a record, it returns each name as the JSON writes it, escapes included; its own
/// `java.util.Map` does the same. These tests pin both, so that a change of avaje-jsonb shows here.
///
/// avaje-jsonb keeps one parser per thread, with the state a failed read leaves: each test runs on a thread of its own.
class MapKeysTest {

    /// The names of the tests, as the JSON writes them (escapes included): plain names, every kind of JSON escape, and
    /// `kqbcb4c`, whose 32-bit FNV-1a hash is the one of `after`, a property of the enclosing records.
    private static final Vector<String> WRITTEN = Vector.of(
            "a",
            "b\\\"c",
            "d\\\\e",
            "\\n",
            "\\t\\r\\b\\f",
            "\\u0001",
            "\\u00e9",
            "\\uD83D\\uDE00",
            "\\/x",
            "kqbcb4c",
            "after",
            "lines");

    /// The same names, decoded.
    private static final Vector<String> DECODED = Vector.of(
            "a",
            "b\"c",
            "d\\e",
            "\n",
            "\t\r\b\f",
            "\u0001",
            "\u00e9",
            "\uD83D\uDE00",
            "/x",
            "kqbcb4c",
            "after",
            "lines");

    /// A JSON object whose names are `WRITTEN`, and whose `i`-th value is `value` of `i`.
    private static String object(java.util.function.IntFunction<String> value) {
        return Vector.range(0, WRITTEN.size())
                .map(i -> "\"" + WRITTEN.get(i) + "\":" + value.apply(i))
                .mkString("{", ",", "}");
    }

    /// `task`, run on a thread that has read no JSON yet.
    private static <T> T onFreshThread(Callable<T> task) throws Exception {
        try (var executor = Executors.newSingleThreadExecutor()) {
            return executor.submit(task).get();
        }
    }

    /// Reads a record whose read fails inside the record, leaving the parser of this thread with its names.
    private static void failInsideARecord() {
        assertThatThrownBy(() -> JSONB.type(Order.class).fromJson("{\"lines\":[null]}"))
                .isInstanceOf(JsonDataException.class);
    }

    @Test
    void aTopLevelMapOnAFreshThreadDecodesEveryName() throws Exception {
        JsonType<LinkedHashMap<String, Inner>> records = json(LinkedHashMap.class, String.class, Inner.class);
        var keys = onFreshThread(() ->
                records.fromJson(object(i -> "{\"v\":\"" + i + "\"}")).keySet().toVector());
        assertThat(keys).isEqualTo(DECODED);
    }

    @Test
    void aMapOfRecordsInsideARecordKeepsEveryNameAsWritten() throws Exception {
        var json = "{\"m\":" + object(i -> "{\"v\":\"" + i + "\"}") + ",\"after\":\"x\"}";
        var read = onFreshThread(() -> JSONB.type(MapOfRecords.class).fromJson(json));
        assertThat(read.m().keySet().toVector()).isEqualTo(WRITTEN);
        assertThat(read.m().values().toVector())
                .isEqualTo(Vector.range(0, WRITTEN.size()).map(i -> new Inner("" + i)));
        assertThat(read.after()).isEqualTo("x");
    }

    @Test
    void aMapOfMapsInsideARecordKeepsEveryNameAsWritten() throws Exception {
        var json = "{\"m\":" + object(i -> object(j -> "" + (i * 100 + j))) + ",\"after\":\"x\"}";
        var read = onFreshThread(() -> JSONB.type(MapOfMaps.class).fromJson(json));
        assertThat(read.m().keySet().toVector()).isEqualTo(WRITTEN);
        for (var outer : read.m()) {
            assertThat(outer._2().keySet().toVector()).isEqualTo(WRITTEN);
            assertThat(outer._2().values().toVector())
                    .isEqualTo(Vector.range(0, WRITTEN.size()).map(j -> WRITTEN.indexOf(outer._1()) * 100 + j));
        }
        assertThat(read.after()).isEqualTo("x");
    }

    @Test
    void aFlatMapInsideARecordKeepsEveryNameAsWritten() throws Exception {
        var json = "{\"names\":" + object(i -> "" + i) + ",\"sorted\":{}}";
        var read = onFreshThread(() -> JSONB.type(Escapes.class).fromJson(json));
        assertThat(read.names().keySet().toVector()).isEqualTo(WRITTEN);
        assertThat(read.names().values().toVector()).isEqualTo(Vector.range(0, WRITTEN.size()));
    }

    @Test
    void afterAFailedReadATopLevelMapKeepsEveryNameAsWritten() throws Exception {
        JsonType<LinkedHashMap<String, Inner>> records = json(LinkedHashMap.class, String.class, Inner.class);
        var keys = onFreshThread(() -> {
            failInsideARecord();
            return records.fromJson(object(i -> "{\"v\":\"" + i + "\"}"))
                    .keySet()
                    .toVector();
        });
        assertThat(keys).isEqualTo(WRITTEN);
    }

    @Test
    void afterAFailedReadARecordHoldingAMapOfRecordsKeepsEveryNameAsWritten() throws Exception {
        var json = "{\"m\":" + object(i -> "{\"v\":\"" + i + "\"}") + ",\"after\":\"x\"}";
        var read = onFreshThread(() -> {
            failInsideARecord();
            return JSONB.type(MapOfRecords.class).fromJson(json);
        });
        assertThat(read.m().keySet().toVector()).isEqualTo(WRITTEN);
        assertThat(read.after()).isEqualTo("x");
    }

    @Test
    void namesWithoutEscapesRoundTripInsideARecord() throws Exception {
        var map = LinkedHashMap.<String, Inner>empty();
        var many = Vector.range(0, 3000).foldLeft(map, (m, i) -> m.put("k" + i, new Inner("" + i)));
        var value = new MapOfRecords(many.put("kqbcb4c", new Inner("c")).put("after", new Inner("d")), "x");
        var back = onFreshThread(() -> JSONB.type(MapOfRecords.class).fromJson(JSONB.toJson(value)));
        assertThat(back).isEqualTo(value);
    }
}
