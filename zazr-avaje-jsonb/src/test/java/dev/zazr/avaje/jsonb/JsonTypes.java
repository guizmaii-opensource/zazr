package dev.zazr.avaje.jsonb;

import io.avaje.jsonb.JsonType;
import io.avaje.jsonb.Jsonb;
import io.avaje.jsonb.Types;
import java.lang.reflect.Type;

/// The `Jsonb` of the tests, found by avaje-jsonb's service loader as in an application, and shorthands for the
/// generic types the tests read and write.
final class JsonTypes {

    static final Jsonb JSONB = Jsonb.instance();

    private JsonTypes() {}

    /// `raw<arguments>`.
    static Type type(Class<?> raw, Type... arguments) {
        return Types.newParameterizedType(raw, arguments);
    }

    /// The JSON type of `raw<arguments>`.
    static <T> JsonType<T> json(Class<?> raw, Type... arguments) {
        return JSONB.type(type(raw, arguments));
    }
}
