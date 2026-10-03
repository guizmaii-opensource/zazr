package dev.zazr.avaje.jsonb;

import dev.zazr.Tuple2;
import dev.zazr.collection.HashMap;
import dev.zazr.collection.LinkedHashMap;
import dev.zazr.collection.List;
import dev.zazr.collection.NonEmptySortedMap;
import dev.zazr.collection.NonEmptyVector;
import dev.zazr.collection.TreeSet;
import dev.zazr.collection.Vector;
import dev.zazr.control.Option;
import io.avaje.jsonb.Json;
import java.time.LocalDate;

/// The records of the tests, whose adapters avaje-jsonb's annotation processor generates, as in an application.
final class TestRecords {

    private TestRecords() {}

    @Json
    record Line(String sku, int quantity) {}

    @Json
    record Order(Vector<Line> lines, Option<String> note) {}

    /// Every kind of Zazr field, generic ones nested.
    @Json
    record Everything(
            Vector<Option<Integer>> scores,
            HashMap<LocalDate, Vector<String>> agenda,
            LinkedHashMap<String, List<Line>> linesByCustomer,
            TreeSet<String> tags,
            NonEmptyVector<Line> mandatory,
            NonEmptySortedMap<Integer, Option<String>> labels,
            Tuple2<String, Option<LocalDate>> deadline,
            Option<Order> parent) {}
}
