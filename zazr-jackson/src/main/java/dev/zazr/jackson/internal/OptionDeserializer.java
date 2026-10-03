package dev.zazr.jackson.internal;

import dev.zazr.control.Option;
import org.jspecify.annotations.Nullable;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.deser.std.ReferenceTypeDeserializer;
import tools.jackson.databind.jsontype.TypeDeserializer;

/// Reads an `Option`: `null` as `None`, any other value as `Some` of the value read with the deserializer of the
/// content type. An absent creator property (a record component, a constructor parameter) reads as `None` too, as
/// Jackson does for `java.util.Optional`; the content deserializer returning `null` for a value gives `None`, since
/// `Some` cannot hold `null`.
public final class OptionDeserializer extends ReferenceTypeDeserializer<Option<?>> {

    OptionDeserializer(
            JavaType type,
            @Nullable TypeDeserializer valueTypeDeserializer,
            @Nullable ValueDeserializer<?> valueDeserializer) {
        super(type, null, valueTypeDeserializer, valueDeserializer);
    }

    @Override
    protected ReferenceTypeDeserializer<Option<?>> withResolved(
            @Nullable TypeDeserializer valueTypeDeserializer, @Nullable ValueDeserializer<?> valueDeserializer) {
        return new OptionDeserializer(_fullType, valueTypeDeserializer, valueDeserializer);
    }

    @Override
    public Option<?> getNullValue(DeserializationContext ctxt) {
        return Option.none();
    }

    @Override
    public Option<?> referenceValue(@Nullable Object contents) {
        return Option.ofNullable(contents);
    }

    @Override
    public Option<?> updateReference(Option<?> reference, @Nullable Object contents) {
        return Option.ofNullable(contents);
    }

    @Override
    public @Nullable Object getReferenced(Option<?> reference) {
        return reference.getOrNull();
    }
}
