package dev.zazr.avaje.jsonb.internal;

import dev.zazr.Tuple2;
import io.avaje.json.JsonAdapter;
import io.avaje.json.JsonDataException;
import io.avaje.json.JsonReader;
import io.avaje.json.JsonWriter;
import java.lang.reflect.Type;
import java.util.ArrayList;
import org.jspecify.annotations.Nullable;

/// The adapter of a Zazr map, written as a JSON object (see [MapShape]); [KeyCodec] turns its keys into property
/// names and back.
///
/// A value is written even when it is `null` or empty and the `Jsonb` leaves out `null` and empty properties: leaving
/// it out would drop its key.
final class MapAdapter<K, V> implements JsonAdapter<Iterable<Tuple2<K, V>>> {

    private final MapShape shape;

    /// The declared type of the keys.
    private final Type keyType;

    private final KeyCodec<K> keys;
    private final JsonAdapter<V> values;

    MapAdapter(MapShape shape, Type keyType, KeyCodec<K> keys, JsonAdapter<V> values) {
        this.shape = shape;
        this.keyType = keyType;
        this.keys = keys;
        this.values = values;
    }

    @Override
    public void toJson(JsonWriter writer, @Nullable Iterable<Tuple2<K, V>> value) {
        if (value == null) {
            writer.nullValue();
            return;
        }
        writer.beginObject();
        for (Tuple2<K, V> entry : value) {
            writer.name(keys.write(entry._1()));
            SequenceAdapter.writeElement(writer, values, entry._2());
        }
        writer.endObject();
    }

    @Override
    @SuppressWarnings("unchecked")
    public @Nullable Iterable<Tuple2<K, V>> fromJson(JsonReader reader) {
        if (reader.isNullValue()) {
            return null;
        }
        Reading.expect(reader, JsonReader.Token.BEGIN_OBJECT, shape.typeName);
        if (shape.sorted) {
            Reading.requireComparable(shape.typeName, keyType, "keys", reader);
        }
        ArrayList<Tuple2<Object, Object>> read = new ArrayList<>();
        reader.beginObject();
        while (reader.hasNextField()) {
            String name = reader.nextField();
            K key = keys.read(name, shape.typeName, reader);
            V value = values.fromJson(reader);
            if (value == null) {
                throw Reading.nullIn("The value of key \"" + name + "\"", shape.typeName, reader);
            }
            read.add(new Tuple2<>(key, value));
        }
        reader.endObject();
        if (shape.nonEmpty && read.isEmpty()) {
            throw new JsonDataException("A " + shape.typeName + " needs at least one entry: the JSON object is empty."
                    + Reading.at(reader));
        }
        try {
            return (Iterable<Tuple2<K, V>>) shape.build(read);
        } catch (ClassCastException e) {
            throw Reading.notMutuallyComparable(shape.typeName, "keys", e, reader);
        }
    }

    @Override
    public String toString() {
        return "ZazrAdapter(" + shape.typeName + "<" + keys + ", " + values + ">)";
    }
}
