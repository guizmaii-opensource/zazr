package dev.zazr.avaje.jsonb.internal;

import io.avaje.json.JsonDataException;
import io.avaje.json.JsonReader;
import io.avaje.json.JsonWriter;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/// The writer and the reader a key adapter sees: one string, number or boolean, and nothing else.
class KeyWriterReaderTest {

    /// The name `write` gives, written to a fresh writer.
    private static String name(Consumer<JsonWriter> write) {
        var writer = new KeyWriter("key");
        write.accept(writer);
        return writer.name();
    }

    /// The failure of `write`, written to a fresh writer: the key is written as `written`.
    private static void notAName(Consumer<JsonWriter> write, String written) {
        assertThatThrownBy(() -> name(write))
                .isInstanceOf(JsonDataException.class)
                .hasMessage("a map key must be written as a JSON string, number or boolean, but the key key "
                        + "(java.lang.String) is written as " + written);
    }

    @Test
    void theNameIsTheTextOfTheScalarWritten() {
        assertThat(name(w -> w.value("a\"b"))).isEqualTo("a\"b");
        assertThat(name(w -> w.value(true))).isEqualTo("true");
        assertThat(name(w -> w.value(-12))).isEqualTo("-12");
        assertThat(name(w -> w.value(1L << 40))).isEqualTo("1099511627776");
        assertThat(name(w -> w.value(1.5))).isEqualTo("1.5");
        assertThat(name(w -> w.value(Boolean.FALSE))).isEqualTo("false");
        assertThat(name(w -> w.value(Integer.valueOf(3)))).isEqualTo("3");
        assertThat(name(w -> w.value(Long.valueOf(4)))).isEqualTo("4");
        assertThat(name(w -> w.value(Double.valueOf(0.25)))).isEqualTo("0.25");
        assertThat(name(w -> w.value(new BigDecimal("12.50")))).isEqualTo("12.50");
        assertThat(name(w -> w.value(new BigInteger("123456789012345678901234567890"))))
                .isEqualTo("123456789012345678901234567890");
        assertThat(name(w -> w.rawValue("raw"))).isEqualTo("raw");
        assertThat(name(w -> w.jsonValue("s"))).isEqualTo("s");
        assertThat(name(w -> w.jsonValue(7))).isEqualTo("7");
        assertThat(name(w -> w.jsonValue(true))).isEqualTo("true");
    }

    @Test
    void theSettingsOfAWriterChangeNothing() {
        assertThat(name(w -> {
                    w.serializeNulls(false);
                    w.serializeEmpty(false);
                    w.forceSerialize();
                    w.pretty(true);
                    w.writeNewLine();
                    w.flush();
                    w.markIncomplete();
                    assertThat(w.serializeNulls()).isTrue();
                    assertThat(w.serializeEmpty()).isTrue();
                    assertThat(w.path()).isEmpty();
                    w.value("a");
                    w.close();
                }))
                .isEqualTo("a");
        assertThatThrownBy(() -> new KeyWriter("key").unwrap(Object.class))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void anythingButOneScalarIsNotAName() {
        notAName(w -> {}, "nothing");
        notAName(
                w -> {
                    w.value(1);
                    w.value(2);
                },
                "more than one value");
        notAName(w -> w.value((String) null), "null");
        notAName(JsonWriter::nullValue, "null");
        notAName(w -> w.value(new byte[] {1}), "binary data");
        notAName(
                w -> w.jsonValue(new Object() {
                    @Override
                    public String toString() {
                        return "object";
                    }
                }),
                "a dev.zazr.avaje.jsonb.internal.KeyWriterReaderTest$1");
        notAName(JsonWriter::beginArray, "an array");
        notAName(JsonWriter::emptyArray, "an array");
        notAName(JsonWriter::endArray, "an array");
        notAName(JsonWriter::beginObject, "an object");
        notAName(w -> w.beginObject(null), "an object");
        notAName(JsonWriter::endObject, "an object");
        notAName(w -> w.allNames(null), "an object");
        notAName(w -> w.name(0), "an object");
        notAName(w -> w.name("a"), "an object");
        notAName(JsonWriter::rawChunkStart, "raw chunks");
        notAName(w -> w.rawChunk('a'), "raw chunks");
        notAName(w -> w.rawChunk("a"), "raw chunks");
        notAName(w -> w.rawChunkEncode("a"), "raw chunks");
        notAName(JsonWriter::rawChunkEnd, "raw chunks");
    }

    @Test
    void theNameReadsAsAStringANumberOrABoolean() {
        assertThat(new KeyReader("a").readString()).isEqualTo("a");
        assertThat(new KeyReader("a").readRaw()).isEqualTo("a");
        assertThat(new KeyReader("true").readBoolean()).isTrue();
        assertThat(new KeyReader("false").readBoolean()).isFalse();
        assertThat(new KeyReader("-12").readInt()).isEqualTo(-12);
        assertThat(new KeyReader("1099511627776").readLong()).isEqualTo(1L << 40);
        assertThat(new KeyReader("1.5").readDouble()).isEqualTo(1.5);
        assertThat(new KeyReader("12.50").readDecimal()).isEqualTo(new BigDecimal("12.50"));
        assertThat(new KeyReader("123456789012345678901234567890").readBigInteger())
                .isEqualTo(new BigInteger("123456789012345678901234567890"));
        var reader = new KeyReader("a");
        assertThat(reader.isNullValue()).isFalse();
        assertThat(reader.currentToken()).isEqualTo(JsonReader.Token.STRING);
        assertThat(reader.location()).isEqualTo("in the key \"a\"");
        reader.skipValue();
        reader.close();
        assertThat(reader.readString()).isEqualTo("a");
    }

    @Test
    void theNameIsNotAnArrayAnObjectOrBinaryData() {
        var reader = new KeyReader("a");
        for (Consumer<JsonReader> read : java.util.List.<Consumer<JsonReader>>of(
                JsonReader::beginArray,
                JsonReader::endArray,
                JsonReader::hasNextElement,
                JsonReader::beginObject,
                r -> r.beginObject(null),
                JsonReader::endObject,
                JsonReader::hasNextField,
                JsonReader::nextField,
                r -> r.unmappedField("b"),
                JsonReader::readBinary)) {
            assertThatThrownBy(() -> read.accept(reader))
                    .isInstanceOf(JsonDataException.class)
                    .hasMessageStartingWith("expected ")
                    .hasMessageEndingWith(", but a map key is a string");
        }
        assertThatThrownBy(reader::readBoolean)
                .isInstanceOf(JsonDataException.class)
                .hasMessage("not a boolean: a");
        assertThatThrownBy(reader::readInt).isInstanceOf(NumberFormatException.class);
        assertThatThrownBy(() -> reader.unwrap(Object.class)).isInstanceOf(UnsupportedOperationException.class);
    }
}
