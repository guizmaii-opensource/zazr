package dev.zazr.jackson.internal;

import dev.zazr.Tuple;
import dev.zazr.Tuple1;
import dev.zazr.Tuple2;
import dev.zazr.Tuple3;
import dev.zazr.Tuple4;
import dev.zazr.Tuple5;
import dev.zazr.Tuple6;
import dev.zazr.Tuple7;
import dev.zazr.Tuple8;
import org.jspecify.annotations.Nullable;
import tools.jackson.core.JsonGenerator;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.jsontype.TypeSerializer;
import tools.jackson.databind.ser.std.StdSerializer;

/// Writes `Tuple1` to `Tuple8` as a JSON array of their components, in order; each component is written with the
/// serializer of its runtime class, `null` as `null`, and with a type id when its declared type takes one.
///
/// Ported from `TupleSerializer` in `src/main/java/io/vavr/jackson/datatype/serialize/TupleSerializer.java` of
/// vavr-jackson (https://github.com/vavr-io/vavr-jackson).
public final class TupleSerializer extends StdSerializer<Tuple> {

    private final JavaType type;

    TupleSerializer(JavaType type) {
        super(type);
        this.type = type;
    }

    /// The declared type of the component at `index`.
    private JavaType componentType(int index) {
        return type.containedTypeOrUnknown(index);
    }

    @Override
    public void serialize(Tuple value, JsonGenerator gen, SerializationContext ctxt) {
        gen.writeStartArray(value, value.arity());
        writeComponents(value, gen, ctxt);
        gen.writeEndArray();
    }

    @Override
    public void serializeWithType(Tuple value, JsonGenerator gen, SerializationContext ctxt, TypeSerializer typeSer) {
        gen.assignCurrentValue(value);
        var typeId = typeSer.writeTypePrefix(gen, ctxt, typeSer.typeId(value, JsonToken.START_ARRAY));
        writeComponents(value, gen, ctxt);
        typeSer.writeTypeSuffix(gen, ctxt, typeId);
    }

    /// Writes each component as [TupleDeserializer] reads it: with a type id when the declared type of the component
    /// takes one (`@JsonTypeInfo` on it, or default typing), otherwise as `SerializationContext.writeValue` does.
    private void writeComponents(Tuple value, JsonGenerator gen, SerializationContext ctxt) {
        var components = components(value);
        for (var i = 0; i < components.length; i++) {
            var component = components[i];
            if (component == null) {
                ctxt.defaultSerializeNullValue(gen);
            } else {
                // the declared type, narrowed to the runtime class: its type parameters stay, so the nested values
                // get the type ids the reader expects, at any depth
                var specialized = ctxt.constructSpecializedType(componentType(i), component.getClass());
                var typeSerializer = ctxt.findTypeSerializer(componentType(i));
                if (typeSerializer == null) {
                    // keeps a type id the runtime class takes on its own (a class-level @JsonTypeInfo)
                    ctxt.findTypedValueSerializer(specialized, true).serialize(component, gen, ctxt);
                } else {
                    ctxt.findValueSerializer(specialized).serializeWithType(component, gen, ctxt, typeSerializer);
                }
            }
        }
    }

    private static @Nullable Object[] components(Tuple value) {
        return switch (value) {
            case Tuple1<?> t -> new @Nullable Object[] {t._1()};
            case Tuple2<?, ?> t -> new @Nullable Object[] {t._1(), t._2()};
            case Tuple3<?, ?, ?> t -> new @Nullable Object[] {t._1(), t._2(), t._3()};
            case Tuple4<?, ?, ?, ?> t -> new @Nullable Object[] {t._1(), t._2(), t._3(), t._4()};
            case Tuple5<?, ?, ?, ?, ?> t -> new @Nullable Object[] {t._1(), t._2(), t._3(), t._4(), t._5()};
            case Tuple6<?, ?, ?, ?, ?, ?> t -> new @Nullable Object[] {t._1(), t._2(), t._3(), t._4(), t._5(), t._6()};
            case Tuple7<?, ?, ?, ?, ?, ?, ?> t ->
                new @Nullable Object[] {t._1(), t._2(), t._3(), t._4(), t._5(), t._6(), t._7()};
            default -> {
                // this serializer is created for Tuple1 to Tuple8 only
                var t = (Tuple8<?, ?, ?, ?, ?, ?, ?, ?>) value;
                yield new @Nullable Object[] {t._1(), t._2(), t._3(), t._4(), t._5(), t._6(), t._7(), t._8()};
            }
        };
    }
}
