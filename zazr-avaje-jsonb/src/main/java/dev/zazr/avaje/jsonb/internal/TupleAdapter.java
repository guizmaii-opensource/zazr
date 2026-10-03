package dev.zazr.avaje.jsonb.internal;

import dev.zazr.Tuple;
import dev.zazr.Tuple1;
import dev.zazr.Tuple2;
import dev.zazr.Tuple3;
import dev.zazr.Tuple4;
import dev.zazr.Tuple5;
import dev.zazr.Tuple6;
import dev.zazr.Tuple7;
import dev.zazr.Tuple8;
import io.avaje.json.JsonAdapter;
import io.avaje.json.JsonDataException;
import io.avaje.json.JsonReader;
import io.avaje.json.JsonWriter;
import org.jspecify.annotations.Nullable;

/// The adapter of `Tuple1` to `Tuple8`: a JSON array of exactly as many elements as the arity, each read and written
/// by the adapter of its own type. A tuple holds `null` components, so a `null` element is read as one.
final class TupleAdapter implements JsonAdapter<Tuple> {

    /// The adapters of the elements, in order; their number is the arity.
    private final JsonAdapter<?>[] elements;

    TupleAdapter(JsonAdapter<?>[] elements) {
        this.elements = elements;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void toJson(JsonWriter writer, @Nullable Tuple value) {
        if (value == null) {
            writer.nullValue();
            return;
        }
        @Nullable Object[] values = elementsOf(value);
        writer.beginArray();
        for (int i = 0; i < elements.length; i++) {
            SequenceAdapter.writeElement(writer, (JsonAdapter<Object>) elements[i], values[i]);
        }
        writer.endArray();
    }

    @Override
    // a tuple's components are nullable (its type parameters extend `@Nullable Object`), which the inferred type
    // arguments of the constructor calls below do not say
    @SuppressWarnings("NullAway")
    public @Nullable Tuple fromJson(JsonReader reader) {
        if (reader.isNullValue()) {
            return null;
        }
        int arity = elements.length;
        Reading.expect(reader, JsonReader.Token.BEGIN_ARRAY, "Tuple" + arity);
        @Nullable Object[] read = new Object[arity];
        reader.beginArray();
        for (int i = 0; i < arity; i++) {
            if (!reader.hasNextElement()) {
                throw wrongLength(i, reader);
            }
            read[i] = elements[i].fromJson(reader);
        }
        if (reader.hasNextElement()) {
            reader.skipValue();
            throw wrongLength(countRest(reader, arity + 1), reader);
        }
        reader.endArray();
        return switch (arity) {
            case 1 -> new Tuple1<>(read[0]);
            case 2 -> new Tuple2<>(read[0], read[1]);
            case 3 -> new Tuple3<>(read[0], read[1], read[2]);
            case 4 -> new Tuple4<>(read[0], read[1], read[2], read[3]);
            case 5 -> new Tuple5<>(read[0], read[1], read[2], read[3], read[4]);
            case 6 -> new Tuple6<>(read[0], read[1], read[2], read[3], read[4], read[5]);
            case 7 -> new Tuple7<>(read[0], read[1], read[2], read[3], read[4], read[5], read[6]);
            default -> new Tuple8<>(read[0], read[1], read[2], read[3], read[4], read[5], read[6], read[7]);
        };
    }

    /// The elements of `tuple`, `null` included, in order; `tuple` is a `Tuple1` to `Tuple8`.
    private static @Nullable Object[] elementsOf(Tuple tuple) {
        return switch (tuple) {
            case Tuple1<?>(var a) -> new Object[] {a};
            case Tuple2<?, ?>(var a, var b) -> new Object[] {a, b};
            case Tuple3<?, ?, ?>(var a, var b, var c) -> new Object[] {a, b, c};
            case Tuple4<?, ?, ?, ?>(var a, var b, var c, var d) -> new Object[] {a, b, c, d};
            case Tuple5<?, ?, ?, ?, ?>(var a, var b, var c, var d, var e) -> new Object[] {a, b, c, d, e};
            case Tuple6<?, ?, ?, ?, ?, ?>(var a, var b, var c, var d, var e, var f) -> new Object[] {a, b, c, d, e, f};
            case Tuple7<?, ?, ?, ?, ?, ?, ?>(var a, var b, var c, var d, var e, var f, var g) ->
                new Object[] {a, b, c, d, e, f, g};
            default -> {
                var t = (Tuple8<?, ?, ?, ?, ?, ?, ?, ?>) tuple;
                yield new Object[] {t._1(), t._2(), t._3(), t._4(), t._5(), t._6(), t._7(), t._8()};
            }
        };
    }

    /// `counted` plus the number of elements left in the array, which are skipped.
    private static int countRest(JsonReader reader, int counted) {
        for (int count = counted; ; count++) {
            if (!reader.hasNextElement()) {
                return count;
            }
            reader.skipValue();
        }
    }

    private JsonDataException wrongLength(int length, JsonReader reader) {
        return new JsonDataException("A Tuple" + elements.length + " is a JSON array of " + elements.length
                + (elements.length == 1 ? " element" : " elements") + ": this one has " + length + "."
                + Reading.at(reader));
    }

    @Override
    public String toString() {
        return "ZazrAdapter(Tuple" + elements.length + ")";
    }
}
