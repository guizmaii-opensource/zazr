package dev.zazr.avaje.jsonb.internal;

import dev.zazr.control.Option;
import io.avaje.json.JsonAdapter;
import io.avaje.json.JsonReader;
import io.avaje.json.JsonWriter;
import org.jspecify.annotations.Nullable;

/// The adapter of `Option`: `Some` is its value, `None` is `null`.
///
/// `None` is written as `null` even when the `Jsonb` leaves out `null` properties: avaje-jsonb gives a property
/// missing from the JSON the value `null`, not `None`, so leaving it out would not read back as `None`.
final class OptionAdapter<T> implements JsonAdapter<Option<T>> {

    private final JsonAdapter<T> values;

    OptionAdapter(JsonAdapter<T> values) {
        this.values = values;
    }

    @Override
    public void toJson(JsonWriter writer, @Nullable Option<T> value) {
        if (value == null || value.isEmpty()) {
            writer.forceSerialize();
            writer.nullValue();
        } else {
            values.toJson(writer, value.get());
        }
    }

    @Override
    public Option<T> fromJson(JsonReader reader) {
        if (reader.isNullValue()) {
            return Option.none();
        }
        T value = values.fromJson(reader);
        return value == null ? Option.none() : Option.some(value);
    }

    @Override
    public String toString() {
        return "ZazrAdapter(Option<" + values + ">)";
    }
}
