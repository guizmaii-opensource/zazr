package dev.zazr.avaje.jsonb.internal;

import dev.zazr.control.Option;
import io.avaje.json.JsonAdapter;
import io.avaje.json.JsonReader;
import io.avaje.json.JsonWriter;
import org.jspecify.annotations.Nullable;

/// The adapter of `Option`: `Some` is its value, `None` is `null`.
///
/// An `Option` is always written, even when the `Jsonb` leaves out `null` and empty properties: avaje-jsonb gives a
/// property missing from the JSON the value `null`, so a left-out `None` or `Some` of an empty collection would not
/// read back as itself. A `null` reference to an `Option` follows the `serializeNulls` setting.
final class OptionAdapter<T> implements JsonAdapter<Option<T>> {

    private final JsonAdapter<T> values;

    OptionAdapter(JsonAdapter<T> values) {
        this.values = values;
    }

    @Override
    public void toJson(JsonWriter writer, @Nullable Option<T> value) {
        if (value == null) {
            writer.nullValue();
            return;
        }
        writer.forceSerialize();
        if (value.isEmpty()) {
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
