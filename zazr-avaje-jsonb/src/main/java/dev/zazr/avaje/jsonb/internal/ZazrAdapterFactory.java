package dev.zazr.avaje.jsonb.internal;

import dev.zazr.Tuple1;
import dev.zazr.Tuple2;
import dev.zazr.Tuple3;
import dev.zazr.Tuple4;
import dev.zazr.Tuple5;
import dev.zazr.Tuple6;
import dev.zazr.Tuple7;
import dev.zazr.Tuple8;
import dev.zazr.control.Option;
import io.avaje.json.JsonAdapter;
import io.avaje.jsonb.AdapterFactory;
import io.avaje.jsonb.Jsonb;
import io.avaje.jsonb.Types;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import org.jspecify.annotations.Nullable;

/// The adapter of a Zazr type, built from the declared type of a property, an element or a value: avaje-jsonb asks
/// every factory in turn and takes the first adapter returned, and this one answers only for the Zazr types.
public final class ZazrAdapterFactory implements AdapterFactory {

    /// The tuple classes by arity minus one.
    private static final Class<?>[] TUPLES = {
        Tuple1.class, Tuple2.class, Tuple3.class, Tuple4.class, Tuple5.class, Tuple6.class, Tuple7.class, Tuple8.class
    };

    /// Creates the factory.
    public ZazrAdapterFactory() {}

    @Override
    public @Nullable JsonAdapter<?> create(Type type, Jsonb jsonb) {
        Class<?> rawType = Types.rawType(type);
        if (Option.class.isAssignableFrom(rawType)) {
            return new OptionAdapter<>(jsonb.adapter(typeArgument(type, 0)));
        }
        SequenceShape sequence = SequenceShape.of(rawType);
        if (sequence != null) {
            return new SequenceAdapter<>(sequence, jsonb.adapter(typeArgument(type, 0)));
        }
        MapShape map = MapShape.of(rawType);
        if (map != null) {
            return new MapAdapter<>(
                    map, KeyCodec.of(typeArgument(type, 0), jsonb), jsonb.adapter(typeArgument(type, 1)));
        }
        for (int i = 0; i < TUPLES.length; i++) {
            if (TUPLES[i] == rawType) {
                JsonAdapter<?>[] elements = new JsonAdapter<?>[i + 1];
                for (int j = 0; j < elements.length; j++) {
                    elements[j] = jsonb.adapter(typeArgument(type, j));
                }
                return new TupleAdapter(elements);
            }
        }
        return null;
    }

    /// The type argument at `index` of `type`, or `Object` when `type` is a raw class (the type of a value written
    /// with `Jsonb.toJson(Object)`, whose adapter is chosen by its class).
    private static Type typeArgument(Type type, int index) {
        return type instanceof ParameterizedType parameterized
                ? parameterized.getActualTypeArguments()[index]
                : Object.class;
    }
}
