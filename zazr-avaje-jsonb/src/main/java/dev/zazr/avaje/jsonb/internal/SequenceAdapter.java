package dev.zazr.avaje.jsonb.internal;

import io.avaje.json.JsonAdapter;
import io.avaje.json.JsonDataException;
import io.avaje.json.JsonReader;
import io.avaje.json.JsonWriter;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Iterator;
import org.jspecify.annotations.Nullable;

/// The adapter of a Zazr type written as a JSON array of its elements (see [SequenceShape]).
///
/// An element is written even when it is `null` or empty and the `Jsonb` leaves out `null` and empty properties: in
/// an array, leaving one out would shift the others.
final class SequenceAdapter<T> implements JsonAdapter<Iterable<T>> {

    private final SequenceShape shape;

    /// The declared type of the elements.
    private final Type elementType;

    private final JsonAdapter<T> elements;

    SequenceAdapter(SequenceShape shape, Type elementType, JsonAdapter<T> elements) {
        this.shape = shape;
        this.elementType = elementType;
        this.elements = elements;
    }

    @Override
    public void toJson(JsonWriter writer, @Nullable Iterable<T> value) {
        if (value == null) {
            writer.nullValue();
            return;
        }
        Iterator<T> iterator = value.iterator();
        if (!iterator.hasNext()) {
            writer.emptyArray();
            return;
        }
        writer.beginArray();
        while (iterator.hasNext()) {
            writeElement(writer, elements, iterator.next());
        }
        writer.endArray();
    }

    /// Writes `element`, `null` included, as an element of an array.
    static <E> void writeElement(JsonWriter writer, JsonAdapter<E> adapter, @Nullable E element) {
        writer.forceSerialize();
        if (element == null) {
            writer.nullValue();
        } else {
            adapter.toJson(writer, element);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public @Nullable Iterable<T> fromJson(JsonReader reader) {
        if (reader.isNullValue()) {
            return null;
        }
        Reading.expect(reader, JsonReader.Token.BEGIN_ARRAY, shape.typeName);
        if (shape.sorted) {
            Reading.requireComparable(shape.typeName, elementType, "elements", reader);
        }
        ArrayList<T> read = new ArrayList<>();
        reader.beginArray();
        while (reader.hasNextElement()) {
            T element = elements.fromJson(reader);
            if (element == null) {
                throw Reading.nullIn("Element " + read.size(), shape.typeName, reader);
            }
            read.add(element);
        }
        reader.endArray();
        if (shape.nonEmpty && read.isEmpty()) {
            throw new JsonDataException("A " + shape.typeName + " needs at least one element: the JSON array is empty."
                    + Reading.at(reader));
        }
        try {
            return (Iterable<T>) shape.build(read);
        } catch (ClassCastException e) {
            throw Reading.notMutuallyComparable(shape.typeName, "elements", e, reader);
        }
    }

    @Override
    public String toString() {
        return "ZazrAdapter(" + shape.typeName + "<" + elements + ">)";
    }
}
