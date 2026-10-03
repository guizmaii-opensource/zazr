package dev.zazr.jackson.internal;

import dev.zazr.control.Option;
import org.jspecify.annotations.Nullable;
import tools.jackson.databind.BeanDescription;
import tools.jackson.databind.DeserializationConfig;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.KeyDeserializer;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.deser.Deserializers;
import tools.jackson.databind.jsontype.TypeDeserializer;
import tools.jackson.databind.type.CollectionLikeType;
import tools.jackson.databind.type.MapLikeType;
import tools.jackson.databind.type.ReferenceType;

/// The deserializers of the Zazr types, matched on the declared class of the value read: the concrete types, and
/// `Set`, `SortedSet`, `Map` and `SortedMap`, which read as `HashSet`, `TreeSet`, `HashMap` and `TreeMap`.
///
/// Ported from `VavrDeserializers` in `src/main/java/io/vavr/jackson/datatype/deserialize/VavrDeserializers.java` of
/// vavr-jackson (https://github.com/vavr-io/vavr-jackson).
public final class ZazrDeserializers extends Deserializers.Base {

    /// The tuples, and the Zazr types that reach this method without the shape [ZazrTypeModifier] gives them: Jackson
    /// builds the type named by a type id (`@JsonTypeInfo`) or by `@JsonDeserialize(as = ...)` without calling the
    /// type modifiers, so the type is reshaped here.
    @Override
    public @Nullable ValueDeserializer<?> findBeanDeserializer(
            JavaType type, DeserializationConfig config, BeanDescription.Supplier beanDescRef) {
        var arity = ZazrTypes.tupleArity(type.getRawClass());
        if (arity != 0) {
            return new TupleDeserializer(type, arity);
        }
        return switch (ZazrTypeModifier.reshape(type, config.getTypeFactory())) {
            case MapLikeType map -> findMapLikeDeserializer(map, config, beanDescRef, null, null, null);
            case CollectionLikeType collection ->
                findCollectionLikeDeserializer(collection, config, beanDescRef, null, null);
            case ReferenceType reference -> findReferenceDeserializer(reference, config, beanDescRef, null, null);
            default -> null;
        };
    }

    @Override
    public @Nullable ValueDeserializer<?> findReferenceDeserializer(
            ReferenceType type,
            DeserializationConfig config,
            BeanDescription.Supplier beanDescRef,
            @Nullable TypeDeserializer contentTypeDeserializer,
            @Nullable ValueDeserializer<?> contentDeserializer) {
        return type.getRawClass() == Option.class
                ? new OptionDeserializer(type, contentTypeDeserializer, contentDeserializer)
                : null;
    }

    @Override
    public @Nullable ValueDeserializer<?> findCollectionLikeDeserializer(
            CollectionLikeType type,
            DeserializationConfig config,
            BeanDescription.Supplier beanDescRef,
            @Nullable TypeDeserializer elementTypeDeserializer,
            @Nullable ValueDeserializer<?> elementDeserializer) {
        var kind = CollectionKind.of(type.getRawClass());
        return kind == null
                ? null
                : new CollectionDeserializer(type, kind, elementTypeDeserializer, elementDeserializer);
    }

    @Override
    public @Nullable ValueDeserializer<?> findMapLikeDeserializer(
            MapLikeType type,
            DeserializationConfig config,
            BeanDescription.Supplier beanDescRef,
            @Nullable KeyDeserializer keyDeserializer,
            @Nullable TypeDeserializer valueTypeDeserializer,
            @Nullable ValueDeserializer<?> valueDeserializer) {
        var kind = MapKind.of(type.getRawClass());
        return kind == null
                ? null
                : new ZazrMapDeserializer(type, kind, keyDeserializer, valueTypeDeserializer, valueDeserializer);
    }

    @Override
    public boolean hasDeserializerFor(DeserializationConfig config, Class<?> valueType) {
        return valueType == Option.class
                || CollectionKind.of(valueType) != null
                || MapKind.of(valueType) != null
                || ZazrTypes.tupleArity(valueType) != 0;
    }
}
