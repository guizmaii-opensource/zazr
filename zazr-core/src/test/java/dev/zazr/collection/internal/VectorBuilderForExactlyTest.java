package dev.zazr.collection.internal;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/// `VectorBuilder.forExactly` with `addUnchecked` against the vector `VectorBuilder.add` builds from the same elements:
/// the same type of
/// vector, slice for slice the same arrays (same lengths, the same element objects at every position), at the leaf and
/// branch boundaries.
public class VectorBuilderForExactlyTest {

    private static final int[] SIZES = {
        1, 2, 31, 32, 33, 63, 64, 65, 1023, 1024, 1025, 32767, 32768, 32769, 40000, 1048575, 1048576, 1048577
    };

    private record Element(int id) {}

    // the same nested arrays, compared down to the leaves, whose elements are the same objects
    private static void assertSameShape(Object[] actual, Object[] expected, int dim, String where) {
        assertThat(actual.length).as(where + " length").isEqualTo(expected.length);
        for (int i = 0; i < expected.length; i++) {
            if (dim <= 1) {
                assertThat(actual[i]).as(where + "[" + i + "]").isSameAs(expected[i]);
            } else {
                assertSameShape((Object[]) actual[i], (Object[]) expected[i], dim - 1, where + "[" + i + "]");
            }
        }
    }

    @Test
    public void writtenVectorIsTheBuiltVector() {
        for (int size : SIZES) {
            Element[] elements = new Element[size];
            for (int i = 0; i < size; i++) {
                elements[i] = new Element(i);
            }
            VectorBuilder<Element> writer = VectorBuilder.forExactly(size);
            VectorBuilder<Element> builder = RadixVector.newBuilder();
            for (Element element : elements) {
                writer.addUnchecked(element);
                builder.add(element);
            }
            RadixVector<Element> written = writer.result();
            RadixVector<Element> built = builder.result();
            String where = "size " + size;
            assertThat(written.getClass()).as(where).isEqualTo(built.getClass());
            assertThat(written.length()).as(where).isEqualTo(size);
            int slices = built.vectorSliceCount();
            assertThat(written.vectorSliceCount()).as(where).isEqualTo(slices);
            for (int s = 0; s < slices; s++) {
                assertSameShape(
                        written.vectorSlice(s),
                        built.vectorSlice(s),
                        VectorStatics.vectorSliceDim(slices, s),
                        where + " slice " + s);
            }
            for (int i = 0; i < size; i += Math.max(1, size / 97)) {
                assertThat(written.get(i)).as(where + " get " + i).isSameAs(elements[i]);
            }
            assertThat(written.get(size - 1)).isSameAs(elements[size - 1]);
        }
    }

    @Test
    public void aVectorOfUpTo32ElementsIsItsOneLeafOfExactSize() {
        for (int size : new int[] {1, 31, 32}) {
            VectorBuilder<Element> writer = VectorBuilder.forExactly(size);
            for (int i = 0; i < size; i++) {
                writer.addUnchecked(new Element(i));
            }
            RadixVector<Element> written = writer.result();
            assertThat(written).isInstanceOf(RadixVector.Vector1.class);
            assertThat(written.vectorSlice(0).length).isEqualTo(size);
        }
    }

    @Test
    public void aClosedBuilderRejectsAnotherElement() {
        for (int size : new int[] {1, 32, 33, 1025}) {
            VectorBuilder<Element> builder = VectorBuilder.forExactly(size);
            for (int i = 0; i < size; i++) {
                builder.addUnchecked(new Element(i));
            }
            builder.result();
            assertThatThrownBy(() -> builder.addUnchecked(new Element(-1))).isInstanceOf(IllegalStateException.class);
        }
    }

    @Test
    public void tooFewElementsAreRejectedByResult() {
        for (int size : new int[] {1, 2, 31, 32, 33, 1025, 32769}) {
            VectorBuilder<Element> builder = VectorBuilder.forExactly(size);
            for (int i = 0; i < size - 1; i++) {
                builder.addUnchecked(new Element(i));
            }
            assertThatThrownBy(builder::result)
                    .as("size " + size)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("Vector builder for exactly " + size + " elements: " + (size - 1) + " added");
        }
    }

    @Test
    public void tooManyElementsAreRejected() {
        // up to 31 the one leaf is full, and the extra element is rejected at once
        for (int size : new int[] {1, 2, 31}) {
            VectorBuilder<Element> builder = VectorBuilder.forExactly(size);
            for (int i = 0; i < size; i++) {
                builder.addUnchecked(new Element(i));
            }
            assertThatThrownBy(() -> builder.addUnchecked(new Element(-1)))
                    .as("size " + size)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("Vector builder for exactly " + size + " elements: more elements added");
        }
        // from 32 the extra element opens a new leaf, and result() rejects the count
        for (int size : new int[] {32, 33, 1024, 1025, 32768}) {
            VectorBuilder<Element> builder = VectorBuilder.forExactly(size);
            for (int i = 0; i <= size; i++) {
                builder.addUnchecked(new Element(i));
            }
            assertThatThrownBy(builder::result)
                    .as("size " + size)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("Vector builder for exactly " + size + " elements: " + (size + 1) + " added");
        }
    }
}
