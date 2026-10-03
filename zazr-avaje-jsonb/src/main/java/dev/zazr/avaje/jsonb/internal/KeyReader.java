package dev.zazr.avaje.jsonb.internal;

import io.avaje.json.JsonDataException;
import io.avaje.json.JsonReader;
import io.avaje.json.PropertyNames;
import java.math.BigDecimal;
import java.math.BigInteger;

/// The reader a key adapter reads one map key from: the name of a property, presented as a JSON string, which the
/// adapter can also read as a number or a boolean.
final class KeyReader implements JsonReader {

    private final String name;

    KeyReader(String name) {
        this.name = name;
    }

    private JsonDataException notA(String expected) {
        return new JsonDataException("expected " + expected + ", but a map key is a string");
    }

    @Override
    public String readString() {
        return name;
    }

    @Override
    public String readRaw() {
        return name;
    }

    @Override
    public boolean readBoolean() {
        return switch (name) {
            case "true" -> true;
            case "false" -> false;
            default -> throw new JsonDataException("not a boolean: " + name);
        };
    }

    @Override
    public int readInt() {
        return Integer.parseInt(name);
    }

    @Override
    public long readLong() {
        return Long.parseLong(name);
    }

    @Override
    public double readDouble() {
        return Double.parseDouble(name);
    }

    @Override
    public BigDecimal readDecimal() {
        return new BigDecimal(name);
    }

    @Override
    public BigInteger readBigInteger() {
        return new BigInteger(name);
    }

    @Override
    public byte[] readBinary() {
        throw notA("binary data");
    }

    @Override
    public boolean isNullValue() {
        return false;
    }

    @Override
    public Token currentToken() {
        return Token.STRING;
    }

    @Override
    public void beginArray() {
        throw notA("an array");
    }

    @Override
    public void endArray() {
        throw notA("an array");
    }

    @Override
    public boolean hasNextElement() {
        throw notA("an array");
    }

    @Override
    public void beginObject(PropertyNames names) {
        throw notA("an object");
    }

    @Override
    public void beginObject() {
        throw notA("an object");
    }

    @Override
    public void endObject() {
        throw notA("an object");
    }

    @Override
    public boolean hasNextField() {
        throw notA("an object");
    }

    @Override
    public String nextField() {
        throw notA("an object");
    }

    @Override
    public void unmappedField(String fieldName) {
        throw notA("an object");
    }

    @Override
    public void skipValue() {}

    @Override
    public String location() {
        return "in the key \"" + name + "\"";
    }

    @Override
    public <T> T unwrap(Class<T> type) {
        throw new UnsupportedOperationException("a map key is read from no underlying reader");
    }

    @Override
    public void close() {}
}
