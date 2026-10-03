package dev.zazr.avaje.jsonb.internal;

import io.avaje.json.JsonAdapter;
import io.avaje.json.JsonDataException;
import io.avaje.json.JsonReader;
import io.avaje.jsonb.Jsonb;
import java.lang.reflect.Type;

/// Turns the keys of a map into the names of the properties of a JSON object, and back.
///
/// A `String` key is the name itself. A key of another type goes through the adapter of its type, which writes it to a
/// [KeyWriter] and reads it from a [KeyReader]: the name is the text of the string, number or boolean the adapter
/// writes (`2026-10-03` for a `LocalDate`, the name of an enum constant, `42` for an `Integer`).
///
/// The key is never written through a `Jsonb` of its own: avaje-jsonb reuses one generator and one parser per thread,
/// and a nested call would reset the ones of the object being written or read.
abstract sealed class KeyCodec<K> {

    /// The name of the property of `key`, which is not `null`.
    abstract String write(K key);

    /// The key of the property named `name` of a `typeName`.
    ///
    /// @throws JsonDataException when the name is not a key, or is the key `null`
    abstract K read(String name, String typeName, JsonReader reader);

    /// The codec of the keys of type `keyType`.
    @SuppressWarnings("unchecked")
    static <K> KeyCodec<K> of(Type keyType, Jsonb jsonb) {
        if (keyType == String.class) {
            return (KeyCodec<K>) StringKeys.INSTANCE;
        }
        return new AdaptedKeys<>(jsonb.adapter(keyType));
    }

    /// `String` keys: the key is the name.
    private static final class StringKeys extends KeyCodec<String> {

        static final StringKeys INSTANCE = new StringKeys();

        @Override
        String write(String key) {
            return key;
        }

        @Override
        String read(String name, String typeName, JsonReader reader) {
            return name;
        }

        @Override
        public String toString() {
            return "String";
        }
    }

    /// Keys of another type, through the adapter of that type.
    private static final class AdaptedKeys<K> extends KeyCodec<K> {

        private final JsonAdapter<K> keys;

        AdaptedKeys(JsonAdapter<K> keys) {
            this.keys = keys;
        }

        @Override
        String write(K key) {
            KeyWriter writer = new KeyWriter(key);
            keys.toJson(writer, key);
            return writer.name();
        }

        @Override
        K read(String name, String typeName, JsonReader reader) {
            K key;
            try {
                key = keys.fromJson(new KeyReader(name));
            } catch (RuntimeException e) {
                throw new JsonDataException(
                        typeName + " cannot read the key \"" + name + "\": " + e.getMessage() + ", "
                                + reader.location(),
                        e);
            }
            if (key == null) {
                throw new JsonDataException(typeName + " rejects null keys: the key \"" + name + "\" is read as null, "
                        + reader.location());
            }
            return key;
        }

        @Override
        public String toString() {
            return keys.toString();
        }
    }
}
