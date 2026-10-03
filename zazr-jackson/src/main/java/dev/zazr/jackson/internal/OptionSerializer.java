package dev.zazr.jackson.internal;

import dev.zazr.control.Option;
import org.jspecify.annotations.Nullable;
import tools.jackson.databind.BeanProperty;
import tools.jackson.databind.ValueSerializer;
import tools.jackson.databind.jsontype.TypeSerializer;
import tools.jackson.databind.ser.std.ReferenceTypeSerializer;
import tools.jackson.databind.type.ReferenceType;
import tools.jackson.databind.util.NameTransformer;

/// Writes an `Option` as its value, or `null` for `None`. Jackson's reference-type support does the rest:
/// `@JsonInclude(NON_ABSENT)` and `NON_EMPTY` leave a `None` property out, `@JsonUnwrapped` unwraps the value, and
/// the value is written with its own serializer, polymorphic type information included.
public final class OptionSerializer extends ReferenceTypeSerializer<Option<?>> {

    OptionSerializer(
            ReferenceType type,
            boolean staticTyping,
            @Nullable TypeSerializer valueTypeSerializer,
            @Nullable ValueSerializer<Object> valueSerializer) {
        super(type, staticTyping, valueTypeSerializer, valueSerializer);
    }

    private OptionSerializer(
            OptionSerializer base,
            @Nullable BeanProperty property,
            @Nullable TypeSerializer valueTypeSerializer,
            @Nullable ValueSerializer<?> valueSerializer,
            @Nullable NameTransformer unwrapper,
            @Nullable Object suppressableValue,
            boolean suppressNulls) {
        super(base, property, valueTypeSerializer, valueSerializer, unwrapper, suppressableValue, suppressNulls);
    }

    @Override
    protected ReferenceTypeSerializer<Option<?>> withResolved(
            @Nullable BeanProperty property,
            @Nullable TypeSerializer valueTypeSerializer,
            @Nullable ValueSerializer<?> valueSerializer,
            @Nullable NameTransformer unwrapper) {
        return new OptionSerializer(
                this, property, valueTypeSerializer, valueSerializer, unwrapper, _suppressableValue, _suppressNulls);
    }

    @Override
    public ReferenceTypeSerializer<Option<?>> withContentInclusion(
            @Nullable Object suppressableValue, boolean suppressNulls) {
        return new OptionSerializer(
                this, _property, _valueTypeSerializer, _valueSerializer, _unwrapper, suppressableValue, suppressNulls);
    }

    @Override
    protected boolean _isValuePresent(Option<?> value) {
        return value.isDefined();
    }

    @Override
    protected Object _getReferenced(Option<?> value) {
        return value.get();
    }

    @Override
    protected @Nullable Object _getReferencedIfPresent(Option<?> value) {
        return value.getOrNull();
    }
}
