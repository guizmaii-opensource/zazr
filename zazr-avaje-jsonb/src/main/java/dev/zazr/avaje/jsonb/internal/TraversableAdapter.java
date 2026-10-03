package dev.zazr.avaje.jsonb.internal;

import dev.zazr.collection.Traversable;
import io.avaje.json.JsonAdapter;
import io.avaje.json.JsonDataException;
import io.avaje.json.JsonReader;
import io.avaje.json.JsonWriter;
import io.avaje.jsonb.Jsonb;
import java.lang.reflect.Type;
import org.jspecify.annotations.Nullable;

/// The adapter of a property declared as `Traversable`. It writes the value by its class, as `Jsonb.toJson(Object)`
/// does: an array, or an object for a map. It cannot read one, since the JSON does not say which collection to build:
/// reading fails with a message that names the types to declare instead.
final class TraversableAdapter implements JsonAdapter<Traversable<?>> {

    private final Jsonb jsonb;

    TraversableAdapter(Jsonb jsonb) {
        this.jsonb = jsonb;
    }

    @Override
    public void toJson(JsonWriter writer, @Nullable Traversable<?> value) {
        if (value == null) {
            writer.nullValue();
        } else {
            jsonb.<Object>adapter((Type) value.getClass()).toJson(writer, value);
        }
    }

    @Override
    public @Nullable Traversable<?> fromJson(JsonReader reader) {
        if (reader.isNullValue()) {
            return null;
        }
        throw new JsonDataException(
                "Cannot read a Traversable: the JSON does not say which collection to build. Declare"
                        + " a concrete type such as Vector or HashMap, or Set, SortedSet, Map or SortedMap."
                        + Reading.at(reader));
    }

    @Override
    public String toString() {
        return "ZazrAdapter(Traversable)";
    }
}
