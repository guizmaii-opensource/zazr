package dev.zazr.avaje.jsonb.internal;

import io.avaje.json.JsonDataException;
import io.avaje.json.JsonReader;
import io.avaje.json.JsonReader.Token;
import io.avaje.jsonb.Types;
import java.lang.reflect.Type;

/// The checks and failure messages shared by the adapters that read a Zazr type. Each message names the type, says
/// what is wrong, and ends with the position in the JSON.
final class Reading {

    private Reading() {}

    /// Fails unless the next value, which is not `null`, starts with `expected`: names the Zazr type when the JSON
    /// holds another kind of value. A number is left to avaje-jsonb, whose own failure follows: the reader reports
    /// anything it does not recognise as a number.
    static void expect(JsonReader reader, Token expected, String typeName) {
        Token actual = reader.currentToken();
        if (actual != expected && actual != Token.NUMBER) {
            throw new JsonDataException(
                    "A " + typeName + " is a JSON " + kind(expected) + ", not " + article(kind(actual)) + at(reader));
        }
    }

    /// The failure for a `null` that a Zazr collection cannot hold; `what` is the element, key or value it is.
    static JsonDataException nullIn(String what, String typeName, JsonReader reader) {
        return new JsonDataException(what + " of the " + typeName
                + " is null: Zazr collections hold no null. Use Option for a value that may be missing." + at(reader));
    }

    /// Fails unless the declared type of the elements or keys (`what`) of a sorted type has a natural order: a sorted
    /// type is read in that order, which an element type such as `Object` or a raw type does not promise.
    static void requireComparable(String typeName, Type ordered, String what, JsonReader reader) {
        Class<?> raw = Types.rawType(ordered);
        if (!Comparable.class.isAssignableFrom(raw)) {
            throw new JsonDataException("Cannot read a " + typeName + " of " + ordered.getTypeName()
                    + ": it is read in the natural order of its " + what + ", and " + raw.getName()
                    + " does not implement Comparable" + at(reader));
        }
    }

    /// The failure for elements or keys (`what`) that are not comparable with each other.
    static JsonDataException notMutuallyComparable(
            String typeName, String what, ClassCastException cause, JsonReader reader) {
        return new JsonDataException(
                "Cannot read a " + typeName + ": it is read in the natural order of its " + what
                        + ", and they are not comparable with each other (" + cause.getMessage() + ")" + at(reader),
                cause);
    }

    /// The position of `reader` in the JSON, as the end of a message.
    static String at(JsonReader reader) {
        return " (" + reader.location() + ")";
    }

    private static String kind(Token token) {
        return switch (token) {
            case BEGIN_ARRAY -> "array";
            case BEGIN_OBJECT -> "object";
            case STRING -> "string";
            case BOOLEAN -> "boolean";
            case NULL -> "null";
            case NUMBER -> "number";
        };
    }

    private static String article(String kind) {
        return kind.equals("array") || kind.equals("object") ? "an " + kind : "a " + kind;
    }
}
