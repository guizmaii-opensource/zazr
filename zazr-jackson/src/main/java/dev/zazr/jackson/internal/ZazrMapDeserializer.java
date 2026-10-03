package dev.zazr.jackson.internal;

import dev.zazr.Tuple;
import dev.zazr.Tuple2;
import java.util.Objects;
import org.jspecify.annotations.Nullable;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.BeanProperty;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.KeyDeserializer;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.deser.ContextualKeyDeserializer;
import tools.jackson.databind.deser.std.StdDeserializer;
import tools.jackson.databind.jsontype.TypeDeserializer;
import tools.jackson.databind.type.MapLikeType;

/// Reads a JSON object into a Zazr map ([MapKind]). Each property name goes through the key deserializer of the key
/// type, and each value through the deserializer of the value type, so non-`String` keys and nested generic types
/// work. A key or a value read as `null` fails, unless the value type reads `null` as a value (`Option` reads it as
/// `None`); an empty object fails for the non-empty types.
///
/// A JSON `null` in place of the whole object reads as Java `null`, as for a `java.util.Map`; with
/// `@JsonSetter(nulls = Nulls.AS_EMPTY)` it reads as the empty map.
///
/// Ported from `MaplikeDeserializer` and `MapDeserializer` in `src/main/java/io/vavr/jackson/datatype/deserialize/` of
/// vavr-jackson (https://github.com/vavr-io/vavr-jackson).
public final class ZazrMapDeserializer extends StdDeserializer<Object> {

    private final MapLikeType type;
    private final MapKind kind;
    private final @Nullable KeyDeserializer keyDeserializer;
    private final @Nullable TypeDeserializer valueTypeDeserializer;
    private final @Nullable ValueDeserializer<Object> valueDeserializer;

    @SuppressWarnings("unchecked")
    ZazrMapDeserializer(
            MapLikeType type,
            MapKind kind,
            @Nullable KeyDeserializer keyDeserializer,
            @Nullable TypeDeserializer valueTypeDeserializer,
            @Nullable ValueDeserializer<?> valueDeserializer) {
        super(type);
        this.type = type;
        this.kind = kind;
        this.keyDeserializer = keyDeserializer;
        this.valueTypeDeserializer = valueTypeDeserializer;
        this.valueDeserializer = (ValueDeserializer<Object>) valueDeserializer;
    }

    @Override
    public ValueDeserializer<?> createContextual(DeserializationContext ctxt, @Nullable BeanProperty property) {
        var keyType = type.getKeyType();
        if (kind.sorted()) {
            Reading.requireComparable(ctxt, type, keyType, "keys");
        }
        var keys = keyDeserializer == null
                ? ctxt.findKeyDeserializer(keyType, property)
                : keyDeserializer instanceof ContextualKeyDeserializer contextual
                        ? contextual.createContextual(ctxt, property)
                        : keyDeserializer;
        var valueType = type.getContentType();
        var values = valueDeserializer == null
                ? ctxt.findContextualValueDeserializer(valueType, property)
                : ctxt.handleSecondaryContextualization(valueDeserializer, property, valueType);
        var valueTypes = valueTypeDeserializer == null ? null : valueTypeDeserializer.forProperty(property);
        return new ZazrMapDeserializer(type, kind, keys, valueTypes, values);
    }

    @Override
    public Object deserialize(JsonParser p, DeserializationContext ctxt) {
        var first = p.currentToken();
        if (first != JsonToken.START_OBJECT && first != JsonToken.PROPERTY_NAME && first != JsonToken.END_OBJECT) {
            return ctxt.handleUnexpectedToken(type, p);
        }
        var keys = Objects.requireNonNull(keyDeserializer, "createContextual sets the key deserializer");
        var values = Objects.requireNonNull(valueDeserializer, "createContextual sets the value deserializer");
        var entries = new java.util.ArrayList<Tuple2<Object, Object>>();
        for (var token = first == JsonToken.START_OBJECT ? p.nextToken() : first;
                token == JsonToken.PROPERTY_NAME;
                token = p.nextToken()) {
            entries.add(entry(p, ctxt, keys, values));
        }
        if (kind.nonEmpty() && entries.isEmpty()) {
            throw Reading.mismatch(ctxt, this, emptyMessage());
        }
        return kind.build(entries);
    }

    /// Reads the entry whose property name is the current token, and leaves the parser on its value.
    private Tuple2<Object, Object> entry(
            JsonParser p, DeserializationContext ctxt, KeyDeserializer keys, ValueDeserializer<Object> values) {
        var name = p.currentName();
        try {
            var key = keys.deserializeKey(name, ctxt);
            if (key == null) {
                throw Reading.mismatch(
                        ctxt, this, Reading.nullMessage("The key read from \"" + name + "\"", kind.typeName()));
            }
            var value = Reading.value(p, ctxt, p.nextToken(), values, valueTypeDeserializer);
            if (value == null) {
                throw Reading.mismatch(
                        ctxt, this, Reading.nullMessage("The value of key \"" + name + "\"", kind.typeName()));
            }
            return Tuple.of(key, value);
        } catch (JacksonException e) {
            e.prependPath(new JacksonException.Reference(type.getRawClass(), name));
            throw e;
        }
    }

    /// The empty map, read for a `null` with `@JsonSetter(nulls = Nulls.AS_EMPTY)`; a non-empty type has none.
    @Override
    public Object getEmptyValue(DeserializationContext ctxt) {
        if (kind.nonEmpty()) {
            throw Reading.mismatch(ctxt, this, emptyMessage());
        }
        return kind.build(java.util.List.of());
    }

    private String emptyMessage() {
        return "A " + kind.typeName() + " needs at least one entry: the JSON object is empty.";
    }
}
