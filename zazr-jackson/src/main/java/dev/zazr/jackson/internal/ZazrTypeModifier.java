package dev.zazr.jackson.internal;

import dev.zazr.control.Option;
import java.lang.reflect.Type;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.type.CollectionLikeType;
import tools.jackson.databind.type.MapLikeType;
import tools.jackson.databind.type.ReferenceType;
import tools.jackson.databind.type.TypeBindings;
import tools.jackson.databind.type.TypeFactory;
import tools.jackson.databind.type.TypeModifier;

/// Tells Jackson the shape of each Zazr type, so that it asks [ZazrSerializers] and [ZazrDeserializers] for the
/// right kind of (de)serializer and resolves the type parameters: the maps are map-like (key and value types), the
/// other collections collection-like (element type), `Option` a reference type (content type).
///
/// The maps are checked first: a Zazr map is also an `Iterable` of its `Tuple2` entries. Only Zazr types are changed;
/// the `asJava()` and `asJavaMap()` views keep the shape of the `java.util` interface they implement.
///
/// Ported from `VavrTypeModifier` in `src/main/java/io/vavr/jackson/datatype/VavrTypeModifier.java` of vavr-jackson
/// (https://github.com/vavr-io/vavr-jackson).
public final class ZazrTypeModifier extends TypeModifier {

    @Override
    public JavaType modifyType(JavaType type, Type jdkType, TypeBindings bindings, TypeFactory typeFactory) {
        return reshape(type, typeFactory);
    }

    /// The map-like, collection-like or reference type of the Zazr type `type`, or `type` itself when it is not one.
    static JavaType reshape(JavaType type, TypeFactory typeFactory) {
        var raw = type.getRawClass();
        if (ZazrTypes.isMap(raw)) {
            var parameters = typeFactory.findTypeParameters(type, ZazrTypes.mapSupertype(raw));
            return MapLikeType.upgradeFrom(type, parameter(parameters, 0), parameter(parameters, 1));
        } else if (ZazrTypes.isCollection(raw)) {
            var element = typeFactory.findFirstTypeParameter(type, Iterable.class);
            return CollectionLikeType.upgradeFrom(type, element);
        } else if (Option.class.isAssignableFrom(raw)) {
            var content = typeFactory.findFirstTypeParameter(type, Option.class);
            return ReferenceType.upgradeFrom(type, content);
        } else {
            return type;
        }
    }

    private static JavaType parameter(JavaType[] parameters, int index) {
        return parameters.length > index ? parameters[index] : TypeFactory.unknownType();
    }
}
