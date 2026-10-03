package dev.zazr.avaje.jsonb.internal;

import io.avaje.json.JsonDataException;
import io.avaje.json.JsonWriter;
import io.avaje.json.PropertyNames;
import java.math.BigDecimal;
import java.math.BigInteger;
import org.jspecify.annotations.Nullable;

/// The writer a key adapter writes one map key to: it keeps the text of the string, number or boolean written, which
/// becomes the name of the property. Any other JSON value fails, since a property name is a string.
final class KeyWriter implements JsonWriter {

    /// The key written, for the error messages.
    private final Object key;

    private @Nullable String name;

    KeyWriter(Object key) {
        this.key = key;
    }

    /// The name of the property: the text of the value written.
    String name() {
        if (name == null) {
            throw notAName("nothing");
        }
        return name;
    }

    private void set(String text) {
        if (name != null) {
            throw notAName("more than one value");
        }
        name = text;
    }

    /// The text of `value`, or the failure of a `null` key.
    private void setOrNull(@Nullable Object value) {
        if (value == null) {
            nullValue();
        } else {
            set(value.toString());
        }
    }

    private JsonDataException notAName(String written) {
        return new JsonDataException("a map key must be written as a JSON string, number or boolean, but the key " + key
                + " (" + key.getClass().getName() + ") is written as " + written);
    }

    @Override
    public void value(@Nullable String value) {
        if (value == null) {
            nullValue();
        } else {
            set(value);
        }
    }

    @Override
    public void value(boolean value) {
        set(Boolean.toString(value));
    }

    @Override
    public void value(int value) {
        set(Integer.toString(value));
    }

    @Override
    public void value(long value) {
        set(Long.toString(value));
    }

    @Override
    public void value(double value) {
        set(Double.toString(value));
    }

    @Override
    public void value(@Nullable Boolean value) {
        setOrNull(value);
    }

    @Override
    public void value(@Nullable Integer value) {
        setOrNull(value);
    }

    @Override
    public void value(@Nullable Long value) {
        setOrNull(value);
    }

    @Override
    public void value(@Nullable Double value) {
        setOrNull(value);
    }

    @Override
    public void value(@Nullable BigDecimal value) {
        setOrNull(value);
    }

    @Override
    public void value(@Nullable BigInteger value) {
        setOrNull(value);
    }

    @Override
    public void rawValue(String value) {
        set(value);
    }

    @Override
    public void jsonValue(Object value) {
        if (value instanceof String || value instanceof Number || value instanceof Boolean) {
            set(value.toString());
        } else {
            throw notAName("a " + value.getClass().getName());
        }
    }

    @Override
    public void nullValue() {
        throw notAName("null");
    }

    @Override
    public void value(byte @Nullable [] value) {
        throw notAName("binary data");
    }

    @Override
    public void beginArray() {
        throw notAName("an array");
    }

    @Override
    public void emptyArray() {
        throw notAName("an array");
    }

    @Override
    public void endArray() {
        throw notAName("an array");
    }

    @Override
    public void beginObject() {
        throw notAName("an object");
    }

    @Override
    public void beginObject(PropertyNames names) {
        throw notAName("an object");
    }

    @Override
    public void endObject() {
        throw notAName("an object");
    }

    @Override
    public void allNames(PropertyNames names) {
        throw notAName("an object");
    }

    @Override
    public void name(int position) {
        throw notAName("an object");
    }

    @Override
    public void name(String name) {
        throw notAName("an object");
    }

    @Override
    public void rawChunkStart() {
        throw notAName("raw chunks");
    }

    @Override
    public void rawChunk(char value) {
        throw notAName("raw chunks");
    }

    @Override
    public void rawChunk(String value) {
        throw notAName("raw chunks");
    }

    @Override
    public void rawChunkEncode(String value) {
        throw notAName("raw chunks");
    }

    @Override
    public void rawChunkEnd() {
        throw notAName("raw chunks");
    }

    @Override
    public <T> T unwrap(Class<T> type) {
        throw new UnsupportedOperationException("a map key is written to no underlying writer");
    }

    @Override
    public void serializeNulls(boolean serializeNulls) {}

    @Override
    public boolean serializeNulls() {
        return true;
    }

    @Override
    public void serializeEmpty(boolean serializeEmpty) {}

    @Override
    public boolean serializeEmpty() {
        return true;
    }

    @Override
    public void forceSerialize() {}

    @Override
    public void pretty(boolean pretty) {}

    @Override
    public String path() {
        return "";
    }

    @Override
    public void writeNewLine() {}

    @Override
    public void flush() {}

    @Override
    public void close() {}

    @Override
    public void markIncomplete() {}
}
