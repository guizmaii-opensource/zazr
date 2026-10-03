package dev.zazr.jackson.internal;

import dev.zazr.Tuple;
import org.jspecify.annotations.Nullable;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.BeanProperty;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.deser.std.StdDeserializer;
import tools.jackson.databind.exc.MismatchedInputException;
import tools.jackson.databind.jsontype.TypeDeserializer;

/// Reads `Tuple1` to `Tuple8` from a JSON array of exactly as many elements, each read with the deserializer of its
/// type parameter. An array of another length fails. A `null` component reads as the null value of its type: `None`
/// for an `Option`, Java `null` for most other types, which a tuple holds, as `Tuple.of(1, null)` does.
///
/// Ported from `TupleDeserializer` in `src/main/java/io/vavr/jackson/datatype/deserialize/TupleDeserializer.java` of
/// vavr-jackson (https://github.com/vavr-io/vavr-jackson).
public final class TupleDeserializer extends StdDeserializer<Tuple> {

    private final JavaType type;
    private final int arity;
    private final java.util.List<ValueDeserializer<Object>> deserializers;
    private final java.util.List<@Nullable TypeDeserializer> typeDeserializers;

    TupleDeserializer(JavaType type, int arity) {
        this(type, arity, java.util.List.of(), java.util.List.of());
    }

    private TupleDeserializer(
            JavaType type,
            int arity,
            java.util.List<ValueDeserializer<Object>> deserializers,
            java.util.List<@Nullable TypeDeserializer> typeDeserializers) {
        super(type);
        this.type = type;
        this.arity = arity;
        this.deserializers = deserializers;
        this.typeDeserializers = typeDeserializers;
    }

    @Override
    public ValueDeserializer<?> createContextual(DeserializationContext ctxt, @Nullable BeanProperty property) {
        var deserializers = new java.util.ArrayList<ValueDeserializer<Object>>(arity);
        var typeDeserializers = new java.util.ArrayList<@Nullable TypeDeserializer>(arity);
        for (var i = 0; i < arity; i++) {
            var componentType = type.containedTypeOrUnknown(i);
            deserializers.add(ctxt.findContextualValueDeserializer(componentType, property));
            typeDeserializers.add(ctxt.findTypeDeserializer(componentType));
        }
        return new TupleDeserializer(type, arity, deserializers, typeDeserializers);
    }

    @Override
    public Tuple deserialize(JsonParser p, DeserializationContext ctxt) {
        if (!p.isExpectedStartArrayToken()) {
            return (Tuple) ctxt.handleUnexpectedToken(type, p);
        }
        var components = new java.util.ArrayList<@Nullable Object>(arity);
        for (var token = p.nextToken(); token != JsonToken.END_ARRAY; token = p.nextToken()) {
            if (components.size() == arity) {
                throw wrongLength(ctxt, arity + skipRest(p));
            }
            components.add(component(p, ctxt, token, components.size()));
        }
        if (components.size() != arity) {
            throw wrongLength(ctxt, components.size());
        }
        return create(components);
    }

    /// Skips the elements left in the array, from the current one, and returns how many there were. The parser is left
    /// on the end of the array.
    private static int skipRest(JsonParser p) {
        for (var count = 0; ; count++) {
            if (p.currentToken() == JsonToken.END_ARRAY) {
                return count;
            }
            p.skipChildren();
            p.nextToken();
        }
    }

    private MismatchedInputException wrongLength(DeserializationContext ctxt, int length) {
        return Reading.mismatch(
                ctxt,
                this,
                String.format(
                        "A %s is a JSON array of %d element%s: this one has %d.",
                        type.getRawClass().getSimpleName(), arity, arity == 1 ? "" : "s", length));
    }

    /// The component at `index`: `null` reads as the null value of its type, `None` for an `Option`, `null` for most
    /// others, which a tuple holds.
    private @Nullable Object component(JsonParser p, DeserializationContext ctxt, JsonToken token, int index) {
        try {
            return Reading.value(p, ctxt, token, deserializers.get(index), typeDeserializers.get(index));
        } catch (JacksonException e) {
            e.prependPath(new JacksonException.Reference(type.getRawClass(), index));
            throw e;
        }
    }

    private Tuple create(java.util.List<@Nullable Object> c) {
        return switch (arity) {
            case 1 -> Tuple.of(c.get(0));
            case 2 -> Tuple.of(c.get(0), c.get(1));
            case 3 -> Tuple.of(c.get(0), c.get(1), c.get(2));
            case 4 -> Tuple.of(c.get(0), c.get(1), c.get(2), c.get(3));
            case 5 -> Tuple.of(c.get(0), c.get(1), c.get(2), c.get(3), c.get(4));
            case 6 -> Tuple.of(c.get(0), c.get(1), c.get(2), c.get(3), c.get(4), c.get(5));
            case 7 -> Tuple.of(c.get(0), c.get(1), c.get(2), c.get(3), c.get(4), c.get(5), c.get(6));
            default -> Tuple.of(c.get(0), c.get(1), c.get(2), c.get(3), c.get(4), c.get(5), c.get(6), c.get(7));
        };
    }
}
