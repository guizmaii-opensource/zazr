package dev.zazr.jackson.internal;

import org.jspecify.annotations.Nullable;
import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.exc.InvalidDefinitionException;
import tools.jackson.databind.exc.MismatchedInputException;
import tools.jackson.databind.jsontype.TypeDeserializer;

/// What the deserializers share: reading one value, and the check that the element and key types of a sorted type
/// have a natural order.
final class Reading {

    private Reading() {}

    /// Reads the value at the current token `token`. A JSON `null` reads as the null value of `deserializer`: `None`
    /// for an `Option`, Java `null` for most other types, which the caller rejects.
    static @Nullable Object value(
            JsonParser p,
            DeserializationContext ctxt,
            JsonToken token,
            ValueDeserializer<Object> deserializer,
            @Nullable TypeDeserializer typeDeserializer) {
        if (token == JsonToken.VALUE_NULL) {
            return deserializer.getNullValue(ctxt);
        } else if (typeDeserializer == null) {
            return deserializer.deserialize(p, ctxt);
        } else {
            return deserializer.deserializeWithType(p, ctxt, typeDeserializer);
        }
    }

    /// Fails, when `orderedType` is not `Comparable`, with a message that says why `type` needs it: the sorted Zazr
    /// types read from JSON use the natural order of their elements or keys.
    static void requireComparable(DeserializationContext ctxt, JavaType type, JavaType orderedType, String what) {
        if (!Comparable.class.isAssignableFrom(orderedType.getRawClass())) {
            throw InvalidDefinitionException.from(
                    ctxt.getParser(),
                    "Cannot read a " + type.getRawClass().getSimpleName() + " of " + orderedType.toCanonical()
                            + ": it is read in the natural order of its " + what + ", and "
                            + orderedType.getRawClass().getName() + " does not implement Comparable",
                    type);
        }
    }

    /// The failure to throw for JSON that `deserializer` cannot read, with `message`: what
    /// `DeserializationContext.reportInputMismatch` throws, returned so that the caller throws it.
    static MismatchedInputException mismatch(
            DeserializationContext ctxt, ValueDeserializer<?> deserializer, String message) {
        return MismatchedInputException.from(ctxt.getParser(), deserializer.handledType(), message);
    }

    /// The message for a `null` that a Zazr type cannot hold.
    static String nullMessage(String what, String typeName) {
        return what + " of the " + typeName + " is null: Zazr collections hold no null. Use Option for a value that "
                + "may be missing.";
    }
}
