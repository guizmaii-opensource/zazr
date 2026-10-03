package dev.zazr.jackson;

import com.fasterxml.jackson.annotation.JsonMerge;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import dev.zazr.collection.HashMap;
import dev.zazr.collection.Vector;
import dev.zazr.control.Option;
import org.junit.jupiter.api.Test;
import tools.jackson.core.JsonGenerator;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.BeanProperty;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.KeyDeserializer;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.annotation.JsonDeserialize;
import tools.jackson.databind.deser.ContextualKeyDeserializer;
import tools.jackson.databind.deser.std.StdDeserializer;
import tools.jackson.databind.exc.InvalidDefinitionException;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import tools.jackson.databind.module.SimpleModule;
import tools.jackson.databind.ser.std.StdSerializer;

import static dev.zazr.jackson.Json.read;
import static dev.zazr.jackson.Json.write;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/// Jackson's customisations reach the elements, keys and values: `contentUsing`, `keyUsing` (contextual too),
/// `@JsonMerge`, a serializer registered for `java.util.Map`.
class CustomisationTest {

    /// Reads a String in upper case.
    static final class Upper extends StdDeserializer<String> {
        Upper() {
            super(String.class);
        }

        @Override
        public String deserialize(JsonParser p, DeserializationContext ctxt) {
            return p.getString().toUpperCase(java.util.Locale.ROOT);
        }
    }

    /// Reads a key with the name of the property it belongs to as a prefix: a contextual key deserializer.
    static final class Prefixed extends KeyDeserializer implements ContextualKeyDeserializer {
        private final String prefix;

        Prefixed() {
            this("");
        }

        Prefixed(String prefix) {
            this.prefix = prefix;
        }

        @Override
        public KeyDeserializer createContextual(DeserializationContext ctxt, BeanProperty property) {
            return new Prefixed(property.getName() + ":");
        }

        @Override
        public Object deserializeKey(String key, DeserializationContext ctxt) {
            return prefix + key;
        }
    }

    record Annotated(
            @JsonDeserialize(contentUsing = Upper.class) Vector<String> names,

            @JsonDeserialize(contentUsing = Upper.class, keyUsing = Prefixed.class)
            HashMap<String, String> labels,

            @JsonDeserialize(contentUsing = Upper.class) Option<String> title) {}

    @Test
    void contentAndKeyDeserializersApply() {
        var read = read("{\"names\":[\"ada\"],\"labels\":{\"a\":\"b\"},\"title\":\"x\"}", Annotated.class);
        assertThat(read).isEqualTo(new Annotated(Vector.of("ADA"), HashMap.of("labels:a", "B"), Option.some("X")));
    }

    /// A mutable value, which `@JsonMerge` updates in place.
    static final class Counter {
        public int a;
        public int b;
    }

    static final class Merged {
        @JsonMerge
        public Option<Counter> counter = Option.none();
    }

    @Test
    void jsonMergeUpdatesTheValueOfAnOption() {
        var merged = new Merged();
        var counter = new Counter();
        counter.a = 1;
        counter.b = 2;
        merged.counter = Option.some(counter);
        Json.MAPPER.readerForUpdating(merged).readValue("{\"counter\":{\"b\":5}}");
        assertThat(merged.counter.get()).isSameAs(counter);
        assertThat(counter.a).isEqualTo(1);
        assertThat(counter.b).isEqualTo(5);

        var empty = new Merged();
        Json.MAPPER.readerForUpdating(empty).readValue("{\"counter\":{\"b\":5}}");
        assertThat(empty.counter.get().b).isEqualTo(5);
    }

    record Typed(@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS) Object value) {}

    @Test
    void anEmptyMapWithATypeId() {
        var mapper = JsonMapper.builder()
                .addModule(new ZazrModule())
                .polymorphicTypeValidator(BasicPolymorphicTypeValidator.builder()
                        .allowIfSubType("dev.zazr.")
                        .build())
                .build();
        var json = mapper.writeValueAsString(new Typed(HashMap.empty()));
        assertThat(json).isEqualTo("{\"value\":{\"@class\":\"dev.zazr.collection.HashMap\"}}");
        assertThat(mapper.readValue(json, Typed.class)).isEqualTo(new Typed(HashMap.empty()));
    }

    /// Writes any `java.util.Map` as its size.
    @SuppressWarnings("rawtypes")
    static final class SizeOnly extends StdSerializer<java.util.Map> {
        SizeOnly() {
            super(java.util.Map.class);
        }

        @Override
        public void serialize(java.util.Map value, JsonGenerator gen, SerializationContext ctxt) {
            gen.writeNumber(value.size());
        }
    }

    @Test
    void aSerializerRegisteredForJavaUtilMapWritesTheZazrMaps() {
        var mapper = JsonMapper.builder()
                .addModule(new ZazrModule())
                .addModule(new SimpleModule().addSerializer(java.util.Map.class, new SizeOnly()))
                .polymorphicTypeValidator(BasicPolymorphicTypeValidator.builder()
                        .allowIfSubType("dev.zazr.")
                        .build())
                .build();
        assertThat(mapper.writeValueAsString(HashMap.of("a", 1, "b", 2))).isEqualTo("2");
        // with a type id, the registered serializer is asked to write it, as for a java.util.Map
        assertThatThrownBy(() -> mapper.writeValueAsString(new Typed(HashMap.of("a", 1))))
                .isInstanceOf(InvalidDefinitionException.class)
                .hasMessageContaining("CustomisationTest$SizeOnly");
        assertThat(write(HashMap.of("a", 1))).isEqualTo("{\"a\":1}");
    }
}
