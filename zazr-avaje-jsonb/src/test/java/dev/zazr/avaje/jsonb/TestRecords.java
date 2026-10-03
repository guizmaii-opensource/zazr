package dev.zazr.avaje.jsonb;

import dev.zazr.Tuple2;
import dev.zazr.collection.HashMap;
import dev.zazr.collection.LinkedHashMap;
import dev.zazr.collection.List;
import dev.zazr.collection.Map;
import dev.zazr.collection.NonEmptySortedMap;
import dev.zazr.collection.NonEmptyVector;
import dev.zazr.collection.Set;
import dev.zazr.collection.SortedMap;
import dev.zazr.collection.SortedSet;
import dev.zazr.collection.Traversable;
import dev.zazr.collection.TreeMap;
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

    /// Fields declared as the Zazr interfaces, on their own and nested.
    @Json
    record Interfaces(
            Set<String> set,
            SortedSet<Integer> sortedSet,
            Map<String, Integer> map,
            SortedMap<LocalDate, Integer> sortedMap,
            Vector<Set<Integer>> sets,
            Map<String, SortedSet<Integer>> sortedSets,
            Option<SortedMap<Integer, Map<String, Integer>>> nested) {}

    /// A field declared as `Traversable`, which is written but cannot be read.
    @Json
    record Anything(Traversable<Integer> values) {}

    /// Maps whose keys hold characters that JSON escapes.
    @Json
    record Escapes(LinkedHashMap<String, Integer> names, TreeMap<String, Vector<String>> sorted) {}

    @Json
    record Inner(String v) {}

    /// A map whose values are objects, inside a record with a property after it.
    @Json
    record MapOfRecords(LinkedHashMap<String, Inner> m, String after) {}

    /// A map of maps, inside a record with a property after it.
    @Json
    record MapOfMaps(LinkedHashMap<String, LinkedHashMap<String, Integer>> m, String after) {}
}
