package dev.zazr.avaje.jsonb.internal;

import io.avaje.json.JsonAdapter;
import io.avaje.json.JsonDataException;
import io.avaje.json.JsonReader;
import io.avaje.json.JsonWriter;
import java.util.ArrayList;
import java.util.Iterator;
import org.jspecify.annotations.Nullable;

/// The adapter of a Zazr type written as a JSON array of its elements (see [SequenceShape]).
///
/// An element is written even when it is `null` or empty and the `Jsonb` leaves out `null` and empty properties: in
/// an array, leaving one out would shift the others.
final class SequenceAdapter<T> implements JsonAdapter<Iterable<T>> {

    private final SequenceShape shape;
    private final JsonAdapter<T> elements;

    SequenceAdapter(SequenceShape shape, JsonAdapter<T> elements) {
        this.shape = shape;
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
        ArrayList<T> read = new ArrayList<>();
        reader.beginArray();
        while (reader.hasNextElement()) {
            T element = elements.fromJson(reader);
            if (element == null) {
                throw new JsonDataException(shape.typeName + " rejects null elements: the element at index "
                        + read.size() + " is null, " + reader.location());
            }
            if (shape.sorted && !(element instanceof Comparable)) {
                throw new JsonDataException(shape.typeName + " sorts its elements in their natural order, but "
                        + element.getClass().getName() + " is not Comparable, " + reader.location());
            }
            read.add(element);
        }
        reader.endArray();
        if (shape.nonEmpty && read.isEmpty()) {
            throw new JsonDataException(
                    shape.typeName + " needs at least one element, but the JSON array is empty, " + reader.location());
        }
        try {
            return (Iterable<T>) shape.build(read);
        } catch (ClassCastException e) {
            throw new JsonDataException(
                    shape.typeName + " sorts its elements in their natural order, but they are not comparable with "
                            + "each other (" + e.getMessage() + "), " + reader.location(),
                    e);
        }
    }

    @Override
    public String toString() {
        return "ZazrAdapter(" + shape.typeName + "<" + elements + ">)";
    }
}
