package com.guizmaii.zazr.collection.internal;

import org.jspecify.annotations.Nullable;

import static com.guizmaii.zazr.collection.internal.VectorStatics.*;

/**
 * The iterator of a non-empty {@link RadixVector}, ported from {@code NewVectorIterator} of
 * {@code scala/collection/immutable/Vector.scala} (Scala 3 standard library).
 * <p>
 * It walks the slices of the vector in order and, inside a slice of dimension {@code d > 1}, keeps the arrays on the
 * path to the current leaf in {@code a2} .. {@code a6}: moving to the next leaf re-reads only the levels whose index
 * changed ({@code xor} of the old and new positions), so {@code next()} is an array read and an index increment on
 * every element but the first of a leaf.
 *
 * @param <T> the element type
 */
final class VectorIterator<T extends @Nullable Object> extends AbstractIterator<T> {

    private final RadixVector<T> v;
    private final int totalLength;
    private final int sliceCount;

    private Object[] a1;
    private Object[] a2 = EMPTY;
    private Object[] a3 = EMPTY;
    private Object[] a4 = EMPTY;
    private Object[] a5 = EMPTY;
    private Object[] a6 = EMPTY;
    private int a1len;
    /* the index of the next element in a1 */
    private int i1;
    private int oldPos;
    /* the number of elements left, counted from the start of a1 */
    private int len1;

    private int sliceIdx;
    private int sliceDim = 1;
    /* the absolute positions of the start and end of the current slice */
    private int sliceStart;
    private int sliceEnd;

    VectorIterator(RadixVector<T> v) {
        this.v = v;
        this.totalLength = v.length();
        this.sliceCount = v.vectorSliceCount();
        this.a1 = v.prefix1;
        this.a1len = a1.length;
        this.len1 = totalLength;
        this.sliceEnd = a1len;
    }

    @Override
    public boolean hasNext() {
        return len1 > i1;
    }

    @Override
    @SuppressWarnings("unchecked")
    protected T getNext() {
        if (i1 == a1len) {
            advance();
        }
        return (T) a1[i1++];
    }

    private void advanceSlice() {
        sliceIdx += 1;
        Object[] slice = v.vectorSlice(sliceIdx);
        while (slice.length == 0) {
            sliceIdx += 1;
            slice = v.vectorSlice(sliceIdx);
        }
        sliceStart = sliceEnd;
        sliceDim = vectorSliceDim(sliceCount, sliceIdx);
        switch (sliceDim) {
            case 1 -> a1 = slice;
            case 2 -> a2 = slice;
            case 3 -> a3 = slice;
            case 4 -> a4 = slice;
            case 5 -> a5 = slice;
            case 6 -> a6 = slice;
            default -> throw new IllegalStateException("dimension " + sliceDim);
        }
        sliceEnd = sliceStart + slice.length * (1 << (BITS * (sliceDim - 1)));
        if (sliceEnd > totalLength) {
            sliceEnd = totalLength;
        }
        if (sliceDim > 1) {
            oldPos = (1 << (BITS * sliceDim)) - 1;
        }
    }

    private void advance() {
        final int pos = i1 - len1 + totalLength;
        if (pos == sliceEnd) {
            advanceSlice();
        }
        if (sliceDim > 1) {
            final int io = pos - sliceStart;
            final int xor = oldPos ^ io;
            advanceA(io, xor);
            oldPos = io;
        }
        len1 -= i1;
        a1len = Math.min(a1.length, len1);
        i1 = 0;
    }

    private void advanceA(int io, int xor) {
        if (xor < WIDTH2) {
            a1 = (Object[]) a2[(io >>> BITS) & MASK];
        } else if (xor < WIDTH3) {
            a2 = (Object[]) a3[(io >>> BITS2) & MASK];
            a1 = (Object[]) a2[0];
        } else if (xor < WIDTH4) {
            a3 = (Object[]) a4[(io >>> BITS3) & MASK];
            a2 = (Object[]) a3[0];
            a1 = (Object[]) a2[0];
        } else if (xor < WIDTH5) {
            a4 = (Object[]) a5[(io >>> BITS4) & MASK];
            a3 = (Object[]) a4[0];
            a2 = (Object[]) a3[0];
            a1 = (Object[]) a2[0];
        } else {
            a5 = (Object[]) a6[io >>> BITS5];
            a4 = (Object[]) a5[0];
            a3 = (Object[]) a4[0];
            a2 = (Object[]) a3[0];
            a1 = (Object[]) a2[0];
        }
    }
}
