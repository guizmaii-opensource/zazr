package dev.zazr.collection.internal;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/// `VectorBuilder.LeafWriter` against the vector `VectorBuilder.add` builds from the same elements: the same type of
/// vector, slice for slice the same arrays (same lengths, the same element objects at every position), at the leaf and
/// branch boundaries.
public class VectorLeafWriterTest {

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
            VectorBuilder.LeafWriter<Element> writer = new VectorBuilder.LeafWriter<>(size);
            VectorBuilder<Element> builder = RadixVector.newBuilder();
            for (Element element : elements) {
                writer.add(element);
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
            VectorBuilder.LeafWriter<Element> writer = new VectorBuilder.LeafWriter<>(size);
            for (int i = 0; i < size; i++) {
                writer.add(new Element(i));
            }
            RadixVector<Element> written = writer.result();
            assertThat(written).isInstanceOf(RadixVector.Vector1.class);
            assertThat(written.vectorSlice(0).length).isEqualTo(size);
        }
    }
}
