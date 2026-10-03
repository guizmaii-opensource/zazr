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
/// serializer of its runtime class, `null` as `null`.
///
/// Ported from `TupleSerializer` in `src/main/java/io/vavr/jackson/datatype/serialize/TupleSerializer.java` of
/// vavr-jackson (https://github.com/vavr-io/vavr-jackson).
public final class TupleSerializer extends StdSerializer<Tuple> {

    TupleSerializer(JavaType type) {
        super(type);
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

    private static void writeComponents(Tuple value, JsonGenerator gen, SerializationContext ctxt) {
        for (var component : components(value)) {
            ctxt.writeValue(gen, component);
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
