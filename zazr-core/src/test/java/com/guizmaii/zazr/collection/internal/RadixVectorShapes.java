package com.guizmaii.zazr.collection.internal;

/**
 * Read access to the arrays of a {@link RadixVector}, for the tests of the types built on it in other packages.
 */
public final class RadixVectorShapes {

    private RadixVectorShapes() {
    }

    /** The number of levels: 0 for the empty vector, N for a {@code VectorN}. */
    public static int depth(RadixVector<?> v) {
        final int sliceCount = v.vectorSliceCount();
        return (sliceCount == 0) ? 0 : (sliceCount + 1) / 2;
    }

    /** The leaf array holding the element at {@code index}. */
    public static Object[] leafAt(RadixVector<?> v, int index) {
        Object[] a = sliceAt(v, index);
        int i = index - sliceStart(v, index);
        for (int dim = sliceDim(v, index); dim > 1; dim--) {
            final int width = 1 << (VectorStatics.BITS * (dim - 1));
            a = (Object[]) a[i / width];
            i %= width;
        }
        return a;
    }

    /** The position of the element at {@code index} in its leaf. */
    public static int indexInLeaf(RadixVector<?> v, int index) {
        final int dim = sliceDim(v, index);
        final int local = index - sliceStart(v, index);
        return (dim == 1) ? local : local & VectorStatics.MASK;
    }

    private static int sliceIndex(RadixVector<?> v, int index) {
        if (index < 0 || index >= v.length()) {
            throw new IndexOutOfBoundsException("index " + index + " of a vector of " + v.length());
        }
        int s = 0;
        while (index >= v.vectorSlicePrefixLength(s)) {
            s++;
        }
        return s;
    }

    private static Object[] sliceAt(RadixVector<?> v, int index) {
        return v.vectorSlice(sliceIndex(v, index));
    }

    private static int sliceStart(RadixVector<?> v, int index) {
        final int s = sliceIndex(v, index);
        return (s == 0) ? 0 : v.vectorSlicePrefixLength(s - 1);
    }

    private static int sliceDim(RadixVector<?> v, int index) {
        return VectorStatics.vectorSliceDim(v.vectorSliceCount(), sliceIndex(v, index));
    }
}
