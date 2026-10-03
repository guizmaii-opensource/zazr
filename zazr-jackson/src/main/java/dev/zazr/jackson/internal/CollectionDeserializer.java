package dev.zazr.jackson.internal;

import java.util.Objects;
import org.jspecify.annotations.Nullable;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.BeanProperty;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.deser.std.StdDeserializer;
import tools.jackson.databind.jsontype.TypeDeserializer;

/// Reads a JSON array into a Zazr collection other than a map ([CollectionKind]). Each element is read with the
/// deserializer of the element type, so nested generic types are honoured. A `null` element fails unless the element
/// type reads `null` as a value (`Option` reads it as `None`); an empty array fails for the non-empty types.
///
/// A JSON `null` in place of the whole array reads as Java `null`, as for a `java.util` collection; with
/// `@JsonSetter(nulls = Nulls.AS_EMPTY)` it reads as the empty collection.
///
/// Ported from `ArrayDeserializer`, `SeqDeserializer` and `SetDeserializer` in
/// `src/main/java/io/vavr/jackson/datatype/deserialize/` of vavr-jackson (https://github.com/vavr-io/vavr-jackson).
public final class CollectionDeserializer extends StdDeserializer<Object> {

    private final JavaType type;
    private final CollectionKind kind;
    private final @Nullable TypeDeserializer elementTypeDeserializer;
    private final @Nullable ValueDeserializer<Object> elementDeserializer;

    @SuppressWarnings("unchecked")
    CollectionDeserializer(
            JavaType type,
            CollectionKind kind,
            @Nullable TypeDeserializer elementTypeDeserializer,
            @Nullable ValueDeserializer<?> elementDeserializer) {
        super(type);
        this.type = type;
        this.kind = kind;
        this.elementTypeDeserializer = elementTypeDeserializer;
        this.elementDeserializer = (ValueDeserializer<Object>) elementDeserializer;
    }

    @Override
    public ValueDeserializer<?> createContextual(DeserializationContext ctxt, @Nullable BeanProperty property) {
        var elementType = type.getContentType();
        if (kind.sorted()) {
            Reading.requireComparable(ctxt, type, elementType, "elements");
        }
        var deserializer = elementDeserializer == null
                ? ctxt.findContextualValueDeserializer(elementType, property)
                : ctxt.handleSecondaryContextualization(elementDeserializer, property, elementType);
        var typeDeserializer = elementTypeDeserializer == null ? null : elementTypeDeserializer.forProperty(property);
        return new CollectionDeserializer(type, kind, typeDeserializer, deserializer);
    }

    @Override
    public Object deserialize(JsonParser p, DeserializationContext ctxt) {
        if (!p.isExpectedStartArrayToken()) {
            return ctxt.handleUnexpectedToken(type, p);
        }
        var deserializer =
                Objects.requireNonNull(elementDeserializer, "createContextual sets the element deserializer");
        var elements = new java.util.ArrayList<Object>();
        for (var token = p.nextToken(); token != JsonToken.END_ARRAY; token = p.nextToken()) {
            elements.add(element(p, ctxt, token, deserializer, elements.size()));
        }
        if (kind.nonEmpty() && elements.isEmpty()) {
            throw Reading.mismatch(ctxt, this, emptyMessage());
        }
        return kind.build(elements);
    }

    private Object element(
            JsonParser p,
            DeserializationContext ctxt,
            JsonToken token,
            ValueDeserializer<Object> deserializer,
            int index) {
        try {
            var element = Reading.value(p, ctxt, token, deserializer, elementTypeDeserializer);
            if (element == null) {
                throw Reading.mismatch(ctxt, this, Reading.nullMessage("Element " + index, kind.typeName()));
            }
            return element;
        } catch (JacksonException e) {
            e.prependPath(new JacksonException.Reference(type.getRawClass(), index));
            throw e;
        }
    }

    /// The empty collection, read for a `null` with `@JsonSetter(nulls = Nulls.AS_EMPTY)`; a non-empty type has none.
    @Override
    public Object getEmptyValue(DeserializationContext ctxt) {
        if (kind.nonEmpty()) {
            throw Reading.mismatch(ctxt, this, emptyMessage());
        }
        return kind.build(java.util.List.of());
    }

    private String emptyMessage() {
        return "A " + kind.typeName() + " needs at least one element: the JSON array is empty.";
    }
}
