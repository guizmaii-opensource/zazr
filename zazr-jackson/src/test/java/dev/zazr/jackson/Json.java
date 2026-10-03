package dev.zazr.jackson;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

/// The mapper the tests use, with the module registered, and shorthands to write and read with it.
final class Json {

    static final JsonMapper MAPPER =
            JsonMapper.builder().addModule(new ZazrModule()).build();

    private Json() {}

    static String write(Object value) {
        return MAPPER.writeValueAsString(value);
    }

    static <T> T read(String json, TypeReference<T> type) {
        return MAPPER.readValue(json, type);
    }

    static <T> T read(String json, Class<T> type) {
        return MAPPER.readValue(json, type);
    }
}
