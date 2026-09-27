package dev.zazr.collection.internal;

/**
 * Read access to the arrays of a {@link RadixVector}, for the tests of the types built on it in other packages.
 */
public final class RadixVectorShapes {

    private RadixVectorShapes() {}

    /** The number of levels: 0 for the empty vector, N for a {@code VectorN}. */
    public static int depth(RadixVector<?> v) {
        int sliceCount = v.vectorSliceCount();
        return (sliceCount == 0) ? 0 : (sliceCount + 1) / 2;
    }

    /** The leaf array holding the element at {@code index}. */
    public static Object[] leafAt(RadixVector<?> v, int index) {
        return descend(sliceAt(v, index), index - sliceStart(v, index), sliceDim(v, index));
    }

    // the leaf holding position `i` of the `dim`-dimensional array `a`
    private static Object[] descend(Object[] a, int i, int dim) {
        if (dim <= 1) {
            return a;
        }
        int width = 1 << (VectorStatics.BITS * (dim - 1));
        return descend((Object[]) a[i / width], i % width, dim - 1);
    }

    /** The position of the element at {@code index} in its leaf. */
    public static int indexInLeaf(RadixVector<?> v, int index) {
        int dim = sliceDim(v, index);
        int local = index - sliceStart(v, index);
        return (dim == 1) ? local : local & VectorStatics.MASK;
    }

    private static int sliceIndex(RadixVector<?> v, int index) {
        if (index < 0 || index >= v.length()) {
            throw new IndexOutOfBoundsException("index " + index + " of a vector of " + v.length());
        }
        for (int s = 0; ; s++) {
            if (index < v.vectorSlicePrefixLength(s)) {
                return s;
            }
        }
    }

    private static Object[] sliceAt(RadixVector<?> v, int index) {
        return v.vectorSlice(sliceIndex(v, index));
    }

    private static int sliceStart(RadixVector<?> v, int index) {
        int s = sliceIndex(v, index);
        return (s == 0) ? 0 : v.vectorSlicePrefixLength(s - 1);
    }

    private static int sliceDim(RadixVector<?> v, int index) {
        return VectorStatics.vectorSliceDim(v.vectorSliceCount(), sliceIndex(v, index));
    }
}
