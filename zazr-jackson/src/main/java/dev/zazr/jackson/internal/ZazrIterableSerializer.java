package dev.zazr.jackson.internal;

import org.jspecify.annotations.Nullable;
import tools.jackson.core.JsonGenerator;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.BeanProperty;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueSerializer;
import tools.jackson.databind.jsontype.TypeSerializer;
import tools.jackson.databind.ser.jdk.IterableSerializer;
import tools.jackson.databind.ser.std.StdSerializer;

/// Writes a Zazr collection other than a map with Jackson's `IterableSerializer`, and names its public type in a type
/// id: `List` for a `List.Cons` or a `List.Nil`, `LazyList` for the implementation classes of `LazyList`, which live
/// in `dev.zazr.collection.internal`. So JSON written with `@JsonTypeInfo` or default typing names only public types,
/// which [ZazrDeserializers] reads.
public final class ZazrIterableSerializer extends StdSerializer<Iterable<?>> {

    private final IterableSerializer delegate;
    private final Class<?> idType;

    ZazrIterableSerializer(IterableSerializer delegate, Class<?> idType) {
        super(Iterable.class);
        this.delegate = delegate;
        this.idType = idType;
    }

    /// Jackson's `IterableSerializer` contextualizes into an `IterableSerializer`.
    @Override
    public ValueSerializer<?> createContextual(SerializationContext ctxt, @Nullable BeanProperty property) {
        var contextual = delegate.createContextual(ctxt, property);
        return contextual == delegate ? this : new ZazrIterableSerializer((IterableSerializer) contextual, idType);
    }

    @Override
    public void serialize(Iterable<?> value, JsonGenerator gen, SerializationContext ctxt) {
        delegate.serialize(value, gen, ctxt);
    }

    /// What `IterableSerializer` does, with the public type as the type id.
    @Override
    public void serializeWithType(
            Iterable<?> value, JsonGenerator gen, SerializationContext ctxt, TypeSerializer typeSer) {
        var typeId = typeSer.writeTypePrefix(gen, ctxt, typeSer.typeId(value, idType, JsonToken.START_ARRAY));
        gen.assignCurrentValue(value);
        delegate.serializeContents(value, gen, ctxt);
        typeSer.writeTypeSuffix(gen, ctxt, typeId);
    }

    @Override
    public boolean isEmpty(SerializationContext ctxt, Iterable<?> value) {
        return delegate.isEmpty(ctxt, value);
    }
}
