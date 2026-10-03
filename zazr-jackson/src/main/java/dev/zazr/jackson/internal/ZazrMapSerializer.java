package dev.zazr.jackson.internal;

import dev.zazr.collection.Map;
import dev.zazr.collection.NonEmptyMap;
import dev.zazr.collection.NonEmptySortedMap;
import org.jspecify.annotations.Nullable;
import tools.jackson.core.JsonGenerator;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.BeanProperty;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueSerializer;
import tools.jackson.databind.jsontype.TypeSerializer;
import tools.jackson.databind.ser.jdk.MapSerializer;
import tools.jackson.databind.ser.std.StdSerializer;
import tools.jackson.databind.type.MapLikeType;

/// Writes a Zazr map as a JSON object: its `asJavaMap()` view, which copies nothing, goes to the serializer Jackson
/// has for a `java.util.Map` of the same key and value types. So the keys go through Jackson's key serializers, and
/// the property's annotations (`@JsonInclude` on the content, `@JsonFormat`) and `ORDER_MAP_ENTRIES_BY_KEYS` apply as
/// they do to a `java.util.Map`.
///
/// Ported from `MapSerializer` in `src/main/java/io/vavr/jackson/datatype/serialize/MapSerializer.java` of
/// vavr-jackson (https://github.com/vavr-io/vavr-jackson), which copies the map into a `java.util.LinkedHashMap` and
/// serializes the copy the same way.
public final class ZazrMapSerializer extends StdSerializer<Object> {

    private final MapLikeType type;
    private final @Nullable ValueSerializer<Object> delegate;

    ZazrMapSerializer(MapLikeType type) {
        this(type, null);
    }

    private ZazrMapSerializer(MapLikeType type, @Nullable ValueSerializer<Object> delegate) {
        super(type);
        this.type = type;
        this.delegate = delegate;
    }

    @Override
    public ValueSerializer<?> createContextual(SerializationContext ctxt, @Nullable BeanProperty property) {
        return new ZazrMapSerializer(type, javaMapSerializer(ctxt, property));
    }

    @Override
    public void serialize(Object value, JsonGenerator gen, SerializationContext ctxt) {
        delegate(ctxt).serialize(view(value), gen, ctxt);
    }

    /// The type id is the one of the Zazr map, not of its view: the prefix is written here, and Jackson's map
    /// serializer writes the entries.
    @Override
    public void serializeWithType(Object value, JsonGenerator gen, SerializationContext ctxt, TypeSerializer typeSer) {
        ValueSerializer<?> delegate = delegate(ctxt);
        if (delegate instanceof MapSerializer mapSerializer) {
            gen.assignCurrentValue(value);
            var typeId = typeSer.writeTypePrefix(gen, ctxt, typeSer.typeId(value, JsonToken.START_OBJECT));
            mapSerializer.serializeWithoutTypeInfo(view(value), gen, ctxt);
            typeSer.writeTypeSuffix(gen, ctxt, typeId);
        } else {
            delegate(ctxt).serializeWithType(view(value), gen, ctxt, typeSer);
        }
    }

    /// Empty as Jackson's map serializer sees the view: no entry, or, with a content `@JsonInclude`, no entry left.
    @Override
    public boolean isEmpty(SerializationContext ctxt, Object value) {
        return delegate(ctxt).isEmpty(ctxt, view(value));
    }

    private ValueSerializer<Object> delegate(SerializationContext ctxt) {
        return delegate != null ? delegate : javaMapSerializer(ctxt, null);
    }

    /// Jackson's serializer of a `java.util.Map` with the key and value types of this map, for `property`.
    private ValueSerializer<Object> javaMapSerializer(SerializationContext ctxt, @Nullable BeanProperty property) {
        var javaMapType =
                ctxt.getTypeFactory().constructMapType(java.util.Map.class, type.getKeyType(), type.getContentType());
        return ctxt.findPrimaryPropertySerializer(javaMapType, property);
    }

    /// The `asJavaMap()` view of `value`, a Zazr map: this serializer is created for those only.
    private static java.util.Map<?, ?> view(Object value) {
        if (value instanceof Map<?, ?> map) {
            return map.asJavaMap();
        } else if (value instanceof NonEmptyMap<?, ?> map) {
            return map.asJavaMap();
        } else {
            return ((NonEmptySortedMap<?, ?>) value).asJavaMap();
        }
    }
}
