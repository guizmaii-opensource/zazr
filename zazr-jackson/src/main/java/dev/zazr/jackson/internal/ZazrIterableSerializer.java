package dev.zazr.jackson.internal;

import org.jspecify.annotations.Nullable;
import tools.jackson.core.JsonGenerator;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.BeanProperty;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueSerializer;
import tools.jackson.databind.jsontype.TypeSerializer;
import tools.jackson.databind.ser.jdk.IterableSerializer;
import tools.jackson.databind.ser.std.StdContainerSerializer;

/// Writes a Zazr collection other than a map with Jackson's `IterableSerializer`, and names its public type in a type
/// id: `List` for a `List.Cons` or a `List.Nil`, `LazyList` for the implementation classes of `LazyList`, which live
/// in `dev.zazr.collection.internal`. So JSON written with `@JsonTypeInfo` or default typing names only public types,
/// which [ZazrDeserializers] reads. With `Id.NAME`, the id is the name registered for the public type.
///
/// It is a container serializer, as `IterableSerializer` is, so that Jackson gives it the type serializer of the
/// elements when a `@JsonTypeInfo` property holds the collection.
public final class ZazrIterableSerializer extends StdContainerSerializer<Iterable<?>> {

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
        // the id of the public type, not of the value's class, which the NAME resolver would use
        var id = typeSer.getTypeIdResolver().idFromValueAndType(ctxt, null, idType);
        var typeId = typeSer.writeTypePrefix(gen, ctxt, typeSer.typeId(value, JsonToken.START_ARRAY, id));
        gen.assignCurrentValue(value);
        delegate.serializeContents(value, gen, ctxt);
        typeSer.writeTypeSuffix(gen, ctxt, typeId);
    }

    @Override
    public JavaType getContentType() {
        return delegate.getContentType();
    }

    @Override
    public @Nullable ValueSerializer<?> getContentSerializer() {
        return delegate.getContentSerializer();
    }

    @Override
    public boolean hasSingleElement(Iterable<?> value) {
        return delegate.hasSingleElement(value);
    }

    /// `IterableSerializer` gives an `IterableSerializer`.
    @Override
    protected StdContainerSerializer<?> _withValueTypeSerializer(TypeSerializer valueTypeSerializer) {
        return new ZazrIterableSerializer(
                (IterableSerializer) delegate.withValueTypeSerializer(valueTypeSerializer), idType);
    }

    @Override
    public boolean isEmpty(SerializationContext ctxt, Iterable<?> value) {
        return delegate.isEmpty(ctxt, value);
    }
}
