package dev.zazr.avaje.jsonb;

import io.avaje.json.JsonAdapter;
import io.avaje.json.JsonReader;
import io.avaje.json.JsonWriter;
import io.avaje.jsonb.Jsonb;
import org.jspecify.annotations.Nullable;

/// Key types with hand-written adapters, for the failures of map keys, and the `Jsonb` that knows them.
final class TestKeys {

    private TestKeys() {}

    /// A key with no natural order, written as its name.
    record Unordered(String name) {}

    /// A key type whose adapter reads every key as `null`.
    record NullKey() {}

    /// A `Comparable` type whose adapter reads the text of a number as an `Integer` and any other text as a `String`,
    /// which cannot be compared with each other.
    record Mixed() implements Comparable<Mixed> {

        @Override
        public int compareTo(Mixed other) {
            return 0;
        }
    }

    static final Jsonb JSONB = Jsonb.builder()
            .add(Unordered.class, new JsonAdapter<Unordered>() {
                @Override
                public void toJson(JsonWriter writer, Unordered value) {
                    writer.value(value.name());
                }

                @Override
                public Unordered fromJson(JsonReader reader) {
                    return new Unordered(reader.readString());
                }
            })
            .add(NullKey.class, new JsonAdapter<@Nullable NullKey>() {
                @Override
                public void toJson(JsonWriter writer, @Nullable NullKey value) {
                    writer.nullValue();
                }

                @Override
                public @Nullable NullKey fromJson(JsonReader reader) {
                    return null;
                }
            })
            .add(Mixed.class, new JsonAdapter<Object>() {
                @Override
                public void toJson(JsonWriter writer, Object value) {
                    writer.value(value.toString());
                }

                @Override
                public Object fromJson(JsonReader reader) {
                    var text = reader.readString();
                    return text.chars().allMatch(Character::isDigit) ? (Object) Integer.valueOf(text) : text;
                }
            })
            .build();
}
