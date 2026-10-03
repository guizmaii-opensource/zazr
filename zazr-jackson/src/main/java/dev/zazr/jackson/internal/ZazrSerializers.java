package dev.zazr.jackson.internal;

import com.fasterxml.jackson.annotation.JsonFormat;
import dev.zazr.control.Option;
import org.jspecify.annotations.Nullable;
import tools.jackson.databind.BeanDescription;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.MapperFeature;
import tools.jackson.databind.SerializationConfig;
import tools.jackson.databind.ValueSerializer;
import tools.jackson.databind.jsontype.TypeSerializer;
import tools.jackson.databind.ser.Serializers;
import tools.jackson.databind.ser.jdk.IterableSerializer;
import tools.jackson.databind.type.CollectionLikeType;
import tools.jackson.databind.type.MapLikeType;
import tools.jackson.databind.type.ReferenceType;

/// The serializers of the Zazr types, matched on the runtime class of the value written.
///
/// - The collections other than maps go to Jackson's own `IterableSerializer`, which writes a JSON array by
///   iterating the value: a `LazyList` is forced element by element as it is written.
/// - The maps go to [ZazrMapSerializer], which writes the `asJavaMap()` view with Jackson's map serializer.
/// - `Option` goes to [OptionSerializer], the tuples to [TupleSerializer].
///
/// Ported from `VavrSerializers` in `src/main/java/io/vavr/jackson/datatype/serialize/VavrSerializers.java` of
/// vavr-jackson (https://github.com/vavr-io/vavr-jackson).
public final class ZazrSerializers extends Serializers.Base {

    @Override
    public @Nullable ValueSerializer<?> findSerializer(
            SerializationConfig config,
            JavaType type,
            BeanDescription.Supplier beanDescRef,
            JsonFormat.@Nullable Value formatOverrides) {
        var arity = ZazrTypes.tupleArity(type.getRawClass());
        return arity == 0 ? null : new TupleSerializer(type);
    }

    @Override
    public @Nullable ValueSerializer<?> findReferenceSerializer(
            SerializationConfig config,
            ReferenceType type,
            BeanDescription.Supplier beanDescRef,
            JsonFormat.@Nullable Value formatOverrides,
            @Nullable TypeSerializer contentTypeSerializer,
            @Nullable ValueSerializer<Object> contentValueSerializer) {
        return Option.class.isAssignableFrom(type.getRawClass())
                ? new OptionSerializer(type, staticTyping(config), contentTypeSerializer, contentValueSerializer)
                : null;
    }

    @Override
    public @Nullable ValueSerializer<?> findCollectionLikeSerializer(
            SerializationConfig config,
            CollectionLikeType type,
            BeanDescription.Supplier beanDescRef,
            JsonFormat.@Nullable Value formatOverrides,
            @Nullable TypeSerializer elementTypeSerializer,
            @Nullable ValueSerializer<Object> elementValueSerializer) {
        return ZazrTypes.isCollection(type.getRawClass())
                ? new IterableSerializer(type.getContentType(), staticTyping(config), elementTypeSerializer)
                : null;
    }

    @Override
    public @Nullable ValueSerializer<?> findMapLikeSerializer(
            SerializationConfig config,
            MapLikeType type,
            BeanDescription.Supplier beanDescRef,
            JsonFormat.@Nullable Value formatOverrides,
            @Nullable ValueSerializer<Object> keySerializer,
            @Nullable TypeSerializer elementTypeSerializer,
            @Nullable ValueSerializer<Object> elementValueSerializer) {
        return ZazrTypes.isMap(type.getRawClass()) ? new ZazrMapSerializer(type) : null;
    }

    private static boolean staticTyping(SerializationConfig config) {
        return config.isEnabled(MapperFeature.USE_STATIC_TYPING);
    }
}
