package com.guizmaii.zazr.collection.internal;

import java.util.Arrays;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

import static com.guizmaii.zazr.collection.internal.VectorStatics.*;

/**
 * A single-shot builder of {@link RadixVector}, ported from {@code VectorBuilder} of
 * {@code scala/collection/immutable/Vector.scala} (Scala 3 standard library).
 * <p>
 * The state is one array per level, {@code a1} (the leaf being filled) to {@code a6}, the fill {@code len1} of
 * {@code a1} and {@code lenRest}, the number of elements before {@code a1}. Elements are written once, into their final
 * leaf; adding another {@code RadixVector} copies or shares its arrays whole. {@link #result()} hands the arrays over to
 * the vector without copying them (only the arrays cut at the end are trimmed), so the builder is closed afterwards:
 * every later call throws {@link IllegalStateException}. Scala's builder is reusable instead, which is why it has a
 * {@code clear()} this one does not.
 * <p>
 * {@code offset} counts the empty slots at the front when the prefix is aligned on the other operand of an
 * {@code appendedAll}/{@code prependedAll} ({@link #alignTo(int, RadixVector)}) or when the builder starts from a vector
 * whose prefix is not full ({@code initFrom}); {@code result()} removes them.
 * <p>
 * Not thread-safe.
 *
 * @param <T> the element type
 */
public final class VectorBuilder<T extends @Nullable Object> {

    private Object[] a6 = EMPTY;
    private Object[] a5 = EMPTY;
    private Object[] a4 = EMPTY;
    private Object[] a3 = EMPTY;
    private Object[] a2 = EMPTY;
    private Object[] a1 = new Object[WIDTH];
    private int len1;
    private int lenRest;
    private int offset;
    private boolean prefixIsRightAligned;
    private int depth = 1;
    private boolean done;

    VectorBuilder() {
    }

    private void setLen(int i) {
        len1 = i & MASK;
        lenRest = i - len1;
    }

    /**
     * @return the number of elements added so far
     * @throws IllegalStateException if {@link #result()} has been called
     */
    public int size() {
        checkOpen();
        return len1 + lenRest - offset;
    }

    /**
     * Appends one element.
     *
     * @throws IllegalStateException if {@link #result()} has been called
     * @throws NullPointerException if {@code element} is null
     */
    public VectorBuilder<T> add(T element) {
        // once closed, len1 == WIDTH, so the check of advance() runs before anything is written
        if (len1 == WIDTH) {
            advance();
        }
        a1[len1] = Objects.requireNonNull(element, "Vector.Builder.add: element is null");
        len1 += 1;
        return this;
    }

    /**
     * Appends every element of {@code elements}, in iteration order. A {@code RadixVector} is added by whole arrays.
     *
     * @throws IllegalStateException if {@link #result()} has been called
     * @throws NullPointerException if {@code elements} is null or yields a null element
     */
    @SuppressWarnings("unchecked")
    public VectorBuilder<T> addAll(Iterable<? extends T> elements) {
        checkOpen();
        Objects.requireNonNull(elements, "elements is null");
        if (elements instanceof RadixVector<?> v) {
            if (len1 == 0 && lenRest == 0 && !prefixIsRightAligned) {
                initFrom(v);
            } else {
                addVector(v);
            }
        } else {
            for (T element : elements) {
                add(element);
            }
        }
        return this;
    }

    /**
     * Appends {@code elements[from, to)}, one whole-leaf copy at a time.
     *
     * @throws IllegalStateException if {@link #result()} has been called
     * @throws NullPointerException if one of those elements is null
     */
    public VectorBuilder<T> addArray(@Nullable Object[] elements, int from, int to) {
        checkOpen();
        for (int i = from; i < to; i++) {
            Objects.requireNonNull(elements[i], "Vector: element is null");
        }
        int i = from;
        while (i < to) {
            if (len1 == WIDTH) {
                advance();
            }
            final int count = Math.min(WIDTH - len1, to - i);
            System.arraycopy(elements, i, a1, len1, count);
            len1 += count;
            i += count;
        }
        return this;
    }

    private void checkOpen() {
        if (done) {
            throw new IllegalStateException("result() has already been called on this Vector.Builder");
        }
    }

    /* starts from v's arrays: its data and suffixes are copied into the builder's (shared below), its prefixes are
     * joined into the first entry of the top array, with offset counting the missing elements in front */
    private void initFrom(RadixVector<?> v) {
        switch (v) {
            case RadixVector.Vector0<?> v0 -> {
            }
            case RadixVector.Vector1<?> v1 -> {
                depth = 1;
                setLen(v1.prefix1.length);
                a1 = copyOrUse(v1.prefix1, 0, WIDTH);
            }
            case RadixVector.Vector2<?> v2 -> {
                final Object[] d2 = v2.data2;
                a1 = copyOrUse(v2.suffix1, 0, WIDTH);
                depth = 2;
                offset = WIDTH - v2.len1;
                setLen(v2.length0 + offset);
                a2 = new Object[WIDTH];
                a2[0] = v2.prefix1;
                System.arraycopy(d2, 0, a2, 1, d2.length);
                a2[d2.length + 1] = a1;
            }
            case RadixVector.Vector3<?> v3 -> {
                final Object[] d3 = v3.data3;
                final Object[] s2 = v3.suffix2;
                a1 = copyOrUse(v3.suffix1, 0, WIDTH);
                depth = 3;
                offset = WIDTH2 - v3.len12;
                setLen(v3.length0 + offset);
                a3 = new Object[WIDTH];
                a3[0] = copyPrepend(v3.prefix1, v3.prefix2);
                System.arraycopy(d3, 0, a3, 1, d3.length);
                a2 = Arrays.copyOf(s2, WIDTH);
                a3[d3.length + 1] = a2;
                a2[s2.length] = a1;
            }
            case RadixVector.Vector4<?> v4 -> {
                final Object[] d4 = v4.data4;
                final Object[] s3 = v4.suffix3;
                final Object[] s2 = v4.suffix2;
                a1 = copyOrUse(v4.suffix1, 0, WIDTH);
                depth = 4;
                offset = WIDTH3 - v4.len123;
                setLen(v4.length0 + offset);
                a4 = new Object[WIDTH];
                a4[0] = copyPrepend(copyPrepend(v4.prefix1, v4.prefix2), v4.prefix3);
                System.arraycopy(d4, 0, a4, 1, d4.length);
                a3 = Arrays.copyOf(s3, WIDTH);
                a2 = Arrays.copyOf(s2, WIDTH);
                a4[d4.length + 1] = a3;
                a3[s3.length] = a2;
                a2[s2.length] = a1;
            }
            case RadixVector.Vector5<?> v5 -> {
                final Object[] d5 = v5.data5;
                final Object[] s4 = v5.suffix4;
                final Object[] s3 = v5.suffix3;
                final Object[] s2 = v5.suffix2;
                a1 = copyOrUse(v5.suffix1, 0, WIDTH);
                depth = 5;
                offset = WIDTH4 - v5.len1234;
                setLen(v5.length0 + offset);
                a5 = new Object[WIDTH];
                a5[0] = copyPrepend(copyPrepend(copyPrepend(v5.prefix1, v5.prefix2), v5.prefix3), v5.prefix4);
                System.arraycopy(d5, 0, a5, 1, d5.length);
                a4 = Arrays.copyOf(s4, WIDTH);
                a3 = Arrays.copyOf(s3, WIDTH);
                a2 = Arrays.copyOf(s2, WIDTH);
                a5[d5.length + 1] = a4;
                a4[s4.length] = a3;
                a3[s3.length] = a2;
                a2[s2.length] = a1;
            }
            case RadixVector.Vector6<?> v6 -> {
                final Object[] d6 = v6.data6;
                final Object[] s5 = v6.suffix5;
                final Object[] s4 = v6.suffix4;
                final Object[] s3 = v6.suffix3;
                final Object[] s2 = v6.suffix2;
                a1 = copyOrUse(v6.suffix1, 0, WIDTH);
                depth = 6;
                offset = WIDTH5 - v6.len12345;
                setLen(v6.length0 + offset);
                a6 = new Object[LASTWIDTH];
                a6[0] = copyPrepend(copyPrepend(copyPrepend(copyPrepend(v6.prefix1, v6.prefix2), v6.prefix3), v6.prefix4), v6.prefix5);
                System.arraycopy(d6, 0, a6, 1, d6.length);
                a5 = Arrays.copyOf(s5, WIDTH);
                a4 = Arrays.copyOf(s4, WIDTH);
                a3 = Arrays.copyOf(s3, WIDTH);
                a2 = Arrays.copyOf(s2, WIDTH);
                a6[d6.length + 1] = a5;
                a5[s5.length] = a4;
                a4[s4.length] = a3;
                a3[s3.length] = a2;
                a2[s2.length] = a1;
            }
        }
        if (len1 == 0 && lenRest > 0) {
            // a1 is v's full suffix1, shared: force advance() on the next addition, so that it is never written
            len1 = WIDTH;
            lenRest -= WIDTH;
        }
    }

    /**
     * Aligns this empty builder so that, once {@code before} elements have been added, the arrays of
     * {@code bigVector} added next are copied whole: the prefix is padded with {@code offset} empty slots, removed by
     * {@link #result()}.
     */
    VectorBuilder<T> alignTo(int before, RadixVector<? extends T> bigVector) {
        if (len1 != 0 || lenRest != 0) {
            throw new UnsupportedOperationException("A non-empty VectorBuilder cannot be aligned retrospectively");
        }
        final int prefixLength;
        final int maxPrefixLength;
        switch (bigVector) {
            case RadixVector.Vector0<?> v0 -> {
                prefixLength = 0;
                maxPrefixLength = 1;
            }
            case RadixVector.Vector1<?> v1 -> {
                prefixLength = 0;
                maxPrefixLength = 1;
            }
            case RadixVector.Vector2<?> v2 -> {
                prefixLength = v2.len1;
                maxPrefixLength = WIDTH;
            }
            case RadixVector.Vector3<?> v3 -> {
                prefixLength = v3.len12;
                maxPrefixLength = WIDTH2;
            }
            case RadixVector.Vector4<?> v4 -> {
                prefixLength = v4.len123;
                maxPrefixLength = WIDTH3;
            }
            case RadixVector.Vector5<?> v5 -> {
                prefixLength = v5.len1234;
                maxPrefixLength = WIDTH4;
            }
            case RadixVector.Vector6<?> v6 -> {
                prefixLength = v6.len12345;
                maxPrefixLength = WIDTH5;
            }
        }
        if (maxPrefixLength == 1) {
            // no alignment for a vector of at most 32 elements
            return this;
        }
        final int overallPrefixLength = (before + prefixLength) % maxPrefixLength;
        offset = (maxPrefixLength - overallPrefixLength) % maxPrefixLength;
        // pretend that offset elements were already added
        advanceN(offset & ~MASK);
        len1 = offset & MASK;
        prefixIsRightAligned = true;
        return this;
    }

    /*
     * Removes the offset leading empty slots of the prefix, after alignTo and the additions that followed, right before
     * the arrays become a vector. Example:
     *     a2 = [null, .., null, [null, .., null, 0, 1, .., x], [x+1, .., x+32], ...]
     * becomes
     *     a2 = [[0, 1, .., x], [x+1, .., x+32], ..., ?, ..., ?]
     * The top array is shifted in place (the builder allocated it); a lower array on the path is replaced by a trimmed
     * copy instead.
     */
    private void leftAlignPrefix() {
        Object @Nullable [] a = null; // the array being modified
        Object @Nullable [] aParent = null; // a's parent, so that aParent[0] == a
        if (depth >= 6) {
            a = a6;
            final int i = offset >>> BITS5;
            if (i > 0) {
                System.arraycopy(a, i, a, 0, LASTWIDTH - i);
            }
            shrinkOffsetIfTooLarge(WIDTH5);
            if ((lenRest >>> BITS5) == 0) {
                depth = 5;
            }
            aParent = a;
            a = (Object[]) a[0];
        }
        if (depth >= 5) {
            if (a == null) {
                a = a5;
            }
            final int i = (offset >>> BITS4) & MASK;
            if (depth == 5) {
                if (i > 0) {
                    System.arraycopy(a, i, a, 0, WIDTH - i);
                }
                a5 = a;
                shrinkOffsetIfTooLarge(WIDTH4);
                if ((lenRest >>> BITS4) == 0) {
                    depth = 4;
                }
            } else {
                if (i > 0) {
                    a = Arrays.copyOfRange(a, i, WIDTH);
                }
                Objects.requireNonNull(aParent)[0] = a;
            }
            aParent = a;
            a = (Object[]) a[0];
        }
        if (depth >= 4) {
            if (a == null) {
                a = a4;
            }
            final int i = (offset >>> BITS3) & MASK;
            if (depth == 4) {
                if (i > 0) {
                    System.arraycopy(a, i, a, 0, WIDTH - i);
                }
                a4 = a;
                shrinkOffsetIfTooLarge(WIDTH3);
                if ((lenRest >>> BITS3) == 0) {
                    depth = 3;
                }
            } else {
                if (i > 0) {
                    a = Arrays.copyOfRange(a, i, WIDTH);
                }
                Objects.requireNonNull(aParent)[0] = a;
            }
            aParent = a;
            a = (Object[]) a[0];
        }
        if (depth >= 3) {
            if (a == null) {
                a = a3;
            }
            final int i = (offset >>> BITS2) & MASK;
            if (depth == 3) {
                if (i > 0) {
                    System.arraycopy(a, i, a, 0, WIDTH - i);
                }
                a3 = a;
                shrinkOffsetIfTooLarge(WIDTH2);
                if ((lenRest >>> BITS2) == 0) {
                    depth = 2;
                }
            } else {
                if (i > 0) {
                    a = Arrays.copyOfRange(a, i, WIDTH);
                }
                Objects.requireNonNull(aParent)[0] = a;
            }
            aParent = a;
            a = (Object[]) a[0];
        }
        if (depth >= 2) {
            if (a == null) {
                a = a2;
            }
            final int i = (offset >>> BITS) & MASK;
            if (depth == 2) {
                if (i > 0) {
                    System.arraycopy(a, i, a, 0, WIDTH - i);
                }
                a2 = a;
                shrinkOffsetIfTooLarge(WIDTH);
                if ((lenRest >>> BITS) == 0) {
                    depth = 1;
                }
            } else {
                if (i > 0) {
                    a = Arrays.copyOfRange(a, i, WIDTH);
                }
                Objects.requireNonNull(aParent)[0] = a;
            }
            aParent = a;
            a = (Object[]) a[0];
        }
        if (depth >= 1) {
            if (a == null) {
                a = a1;
            }
            final int i = offset & MASK;
            if (depth == 1) {
                if (i > 0) {
                    System.arraycopy(a, i, a, 0, WIDTH - i);
                }
                a1 = a;
                len1 -= offset;
                offset = 0;
            } else {
                if (i > 0) {
                    a = Arrays.copyOfRange(a, i, WIDTH);
                }
                Objects.requireNonNull(aParent)[0] = a;
            }
        }
        prefixIsRightAligned = false;
    }

    private void shrinkOffsetIfTooLarge(int width) {
        final int newOffset = offset % width;
        lenRest -= offset - newOffset;
        offset = newOffset;
    }

    /* appends a leaf of at most WIDTH elements, by at most two array copies */
    private void addArr1(Object[] data) {
        final int dl = data.length;
        if (dl > 0) {
            if (len1 == WIDTH) {
                advance();
            }
            final int copy1 = Math.min(WIDTH - len1, dl);
            final int copy2 = dl - copy1;
            System.arraycopy(data, 0, a1, len1, copy1);
            len1 += copy1;
            if (copy2 > 0) {
                advance();
                System.arraycopy(data, copy1, a1, 0, copy2);
                len1 += copy2;
            }
        }
    }

    /* appends a slice of dimension dim >= 2 while a1 is empty or full: its sub-arrays are shared when they align on the
     * builder's arrays, otherwise the slice is added one dimension lower */
    private void addArrN(Object[] slice, int dim) {
        if (slice.length == 0) {
            return;
        }
        if (len1 == WIDTH) {
            advance();
        }
        final int sl = slice.length;
        switch (dim) {
            case 2 -> {
                // lenRest is always a multiple of WIDTH
                final int copy1 = Math.min(((WIDTH2 - lenRest) >>> BITS) & MASK, sl);
                final int copy2 = sl - copy1;
                final int destPos = (lenRest >>> BITS) & MASK;
                System.arraycopy(slice, 0, a2, destPos, copy1);
                advanceN(WIDTH * copy1);
                if (copy2 > 0) {
                    System.arraycopy(slice, copy1, a2, 0, copy2);
                    advanceN(WIDTH * copy2);
                }
            }
            case 3 -> {
                if (lenRest % WIDTH2 != 0) {
                    // not aligned on a WIDTH2 boundary: add the slice one dimension lower
                    for (Object e : slice) {
                        addArrN((Object[]) e, 2);
                    }
                    return;
                }
                final int copy1 = Math.min(((WIDTH3 - lenRest) >>> BITS2) & MASK, sl);
                final int copy2 = sl - copy1;
                final int destPos = (lenRest >>> BITS2) & MASK;
                System.arraycopy(slice, 0, a3, destPos, copy1);
                advanceN(WIDTH2 * copy1);
                if (copy2 > 0) {
                    System.arraycopy(slice, copy1, a3, 0, copy2);
                    advanceN(WIDTH2 * copy2);
                }
            }
            case 4 -> {
                if (lenRest % WIDTH3 != 0) {
                    for (Object e : slice) {
                        addArrN((Object[]) e, 3);
                    }
                    return;
                }
                final int copy1 = Math.min(((WIDTH4 - lenRest) >>> BITS3) & MASK, sl);
                final int copy2 = sl - copy1;
                final int destPos = (lenRest >>> BITS3) & MASK;
                System.arraycopy(slice, 0, a4, destPos, copy1);
                advanceN(WIDTH3 * copy1);
                if (copy2 > 0) {
                    System.arraycopy(slice, copy1, a4, 0, copy2);
                    advanceN(WIDTH3 * copy2);
                }
            }
            case 5 -> {
                if (lenRest % WIDTH4 != 0) {
                    for (Object e : slice) {
                        addArrN((Object[]) e, 4);
                    }
                    return;
                }
                final int copy1 = Math.min(((WIDTH5 - lenRest) >>> BITS4) & MASK, sl);
                final int copy2 = sl - copy1;
                final int destPos = (lenRest >>> BITS4) & MASK;
                System.arraycopy(slice, 0, a5, destPos, copy1);
                advanceN(WIDTH4 * copy1);
                if (copy2 > 0) {
                    System.arraycopy(slice, copy1, a5, 0, copy2);
                    advanceN(WIDTH4 * copy2);
                }
            }
            case 6 -> {
                // the top level is LASTWIDTH wide
                if (lenRest % WIDTH5 != 0) {
                    for (Object e : slice) {
                        addArrN((Object[]) e, 5);
                    }
                    return;
                }
                // there is no second copy: there is no array above a6 to move to
                final int destPos = lenRest >>> BITS5;
                if (destPos + sl > LASTWIDTH) {
                    throw new IllegalArgumentException("a Vector cannot hold more than Integer.MAX_VALUE elements");
                }
                System.arraycopy(slice, 0, a6, destPos, sl);
                advanceN(WIDTH5 * sl);
            }
            default -> throw new IllegalArgumentException("dimension " + dim);
        }
    }

    private void addVector(RadixVector<?> xs) {
        final int sliceCount = xs.vectorSliceCount();
        for (int sliceIdx = 0; sliceIdx < sliceCount; sliceIdx++) {
            final Object[] slice = xs.vectorSlice(sliceIdx);
            final int n = vectorSliceDim(sliceCount, sliceIdx);
            if (n == 1) {
                addArr1(slice);
            } else if (len1 == WIDTH || len1 == 0) {
                addArrN(slice, n);
            } else {
                addLeaves(n - 2, slice);
            }
        }
    }

    /* adds every leaf under a, where level 0 means that a holds leaves */
    private void addLeaves(int level, Object[] a) {
        if (level == 0) {
            for (Object leaf : a) {
                addArr1((Object[]) leaf);
            }
        } else {
            for (Object child : a) {
                addLeaves(level - 1, (Object[]) child);
            }
        }
    }

    private void advance() {
        checkOpen();
        final int idx = lenRest + WIDTH;
        final int xor = idx ^ lenRest;
        lenRest = idx;
        len1 = 0;
        advance1(idx, xor);
    }

    private void advanceN(int n) {
        if (n > 0) {
            final int idx = lenRest + n;
            final int xor = idx ^ lenRest;
            lenRest = idx;
            len1 = 0;
            advance1(idx, xor);
        }
    }

    /* opens the arrays for the leaf at idx; xor tells how many levels changed */
    private void advance1(int idx, int xor) {
        if (xor <= 0) {
            // beyond level 6
            throw new IllegalArgumentException("a Vector cannot hold more than Integer.MAX_VALUE elements");
        } else if (xor < WIDTH2) { // level 1
            if (depth <= 1) {
                a2 = new Object[WIDTH];
                a2[0] = a1;
                depth = 2;
            }
            a1 = new Object[WIDTH];
            a2[(idx >>> BITS) & MASK] = a1;
        } else if (xor < WIDTH3) { // level 2
            if (depth <= 2) {
                a3 = new Object[WIDTH];
                a3[0] = a2;
                depth = 3;
            }
            a1 = new Object[WIDTH];
            a2 = new Object[WIDTH];
            a2[(idx >>> BITS) & MASK] = a1;
            a3[(idx >>> BITS2) & MASK] = a2;
        } else if (xor < WIDTH4) { // level 3
            if (depth <= 3) {
                a4 = new Object[WIDTH];
                a4[0] = a3;
                depth = 4;
            }
            a1 = new Object[WIDTH];
            a2 = new Object[WIDTH];
            a3 = new Object[WIDTH];
            a2[(idx >>> BITS) & MASK] = a1;
            a3[(idx >>> BITS2) & MASK] = a2;
            a4[(idx >>> BITS3) & MASK] = a3;
        } else if (xor < WIDTH5) { // level 4
            if (depth <= 4) {
                a5 = new Object[WIDTH];
                a5[0] = a4;
                depth = 5;
            }
            a1 = new Object[WIDTH];
            a2 = new Object[WIDTH];
            a3 = new Object[WIDTH];
            a4 = new Object[WIDTH];
            a2[(idx >>> BITS) & MASK] = a1;
            a3[(idx >>> BITS2) & MASK] = a2;
            a4[(idx >>> BITS3) & MASK] = a3;
            a5[(idx >>> BITS4) & MASK] = a4;
        } else { // level 5
            if (depth <= 5) {
                a6 = new Object[LASTWIDTH];
                a6[0] = a5;
                depth = 6;
            }
            a1 = new Object[WIDTH];
            a2 = new Object[WIDTH];
            a3 = new Object[WIDTH];
            a4 = new Object[WIDTH];
            a5 = new Object[WIDTH];
            a2[(idx >>> BITS) & MASK] = a1;
            a3[(idx >>> BITS2) & MASK] = a2;
            a4[(idx >>> BITS3) & MASK] = a3;
            a5[(idx >>> BITS4) & MASK] = a4;
            a6[idx >>> BITS5] = a5;
        }
    }

    private static Object[] at(Object[] a, int i) {
        return (Object[]) a[i];
    }

    /**
     * Builds the vector. The builder cannot be used afterwards.
     *
     * @throws IllegalStateException if {@link #result()} has already been called
     */
    public RadixVector<T> result() {
        checkOpen();
        if (prefixIsRightAligned) {
            leftAlignPrefix();
        }
        final int len = len1 + lenRest;
        final int realLen = len - offset;
        final RadixVector<T> result;
        if (realLen == 0) {
            result = RadixVector.empty();
        } else if (len < 0) {
            throw new IndexOutOfBoundsException("Vector cannot have negative size " + len);
        } else if (len <= WIDTH) {
            result = new RadixVector.Vector1<>(copyIfDifferentSize(a1, realLen));
        } else if (len <= WIDTH2) {
            final int i1 = (len - 1) & MASK;
            final int i2 = (len - 1) >>> BITS;
            final Object[] data = Arrays.copyOfRange(a2, 1, i2);
            final Object[] prefix1 = at(a2, 0);
            final Object[] suffix1 = copyIfDifferentSize(at(a2, i2), i1 + 1);
            result = new RadixVector.Vector2<>(prefix1, WIDTH - offset, data, suffix1, realLen);
        } else if (len <= WIDTH3) {
            final int i1 = (len - 1) & MASK;
            final int i2 = ((len - 1) >>> BITS) & MASK;
            final int i3 = (len - 1) >>> BITS2;
            final Object[] data = Arrays.copyOfRange(a3, 1, i3);
            final Object[] prefix2 = copyTail(at(a3, 0));
            final Object[] prefix1 = at(at(a3, 0), 0);
            final Object[] suffix2 = Arrays.copyOf(at(a3, i3), i2);
            final Object[] suffix1 = copyIfDifferentSize(at(at(a3, i3), i2), i1 + 1);
            final int len1 = prefix1.length;
            final int len12 = len1 + prefix2.length * WIDTH;
            result = new RadixVector.Vector3<>(prefix1, len1, prefix2, len12, data, suffix2, suffix1, realLen);
        } else if (len <= WIDTH4) {
            final int i1 = (len - 1) & MASK;
            final int i2 = ((len - 1) >>> BITS) & MASK;
            final int i3 = ((len - 1) >>> BITS2) & MASK;
            final int i4 = (len - 1) >>> BITS3;
            final Object[] data = Arrays.copyOfRange(a4, 1, i4);
            final Object[] prefix3 = copyTail(at(a4, 0));
            final Object[] prefix2 = copyTail(at(at(a4, 0), 0));
            final Object[] prefix1 = at(at(at(a4, 0), 0), 0);
            final Object[] suffix3 = Arrays.copyOf(at(a4, i4), i3);
            final Object[] suffix2 = Arrays.copyOf(at(at(a4, i4), i3), i2);
            final Object[] suffix1 = copyIfDifferentSize(at(at(at(a4, i4), i3), i2), i1 + 1);
            final int len1 = prefix1.length;
            final int len12 = len1 + prefix2.length * WIDTH;
            final int len123 = len12 + prefix3.length * WIDTH2;
            result = new RadixVector.Vector4<>(prefix1, len1, prefix2, len12, prefix3, len123, data, suffix3, suffix2, suffix1, realLen);
        } else if (len <= WIDTH5) {
            final int i1 = (len - 1) & MASK;
            final int i2 = ((len - 1) >>> BITS) & MASK;
            final int i3 = ((len - 1) >>> BITS2) & MASK;
            final int i4 = ((len - 1) >>> BITS3) & MASK;
            final int i5 = (len - 1) >>> BITS4;
            final Object[] data = Arrays.copyOfRange(a5, 1, i5);
            final Object[] prefix4 = copyTail(at(a5, 0));
            final Object[] prefix3 = copyTail(at(at(a5, 0), 0));
            final Object[] prefix2 = copyTail(at(at(at(a5, 0), 0), 0));
            final Object[] prefix1 = at(at(at(at(a5, 0), 0), 0), 0);
            final Object[] suffix4 = Arrays.copyOf(at(a5, i5), i4);
            final Object[] suffix3 = Arrays.copyOf(at(at(a5, i5), i4), i3);
            final Object[] suffix2 = Arrays.copyOf(at(at(at(a5, i5), i4), i3), i2);
            final Object[] suffix1 = copyIfDifferentSize(at(at(at(at(a5, i5), i4), i3), i2), i1 + 1);
            final int len1 = prefix1.length;
            final int len12 = len1 + prefix2.length * WIDTH;
            final int len123 = len12 + prefix3.length * WIDTH2;
            final int len1234 = len123 + prefix4.length * WIDTH3;
            result = new RadixVector.Vector5<>(prefix1, len1, prefix2, len12, prefix3, len123, prefix4, len1234, data, suffix4, suffix3,
                suffix2, suffix1, realLen);
        } else {
            final int i1 = (len - 1) & MASK;
            final int i2 = ((len - 1) >>> BITS) & MASK;
            final int i3 = ((len - 1) >>> BITS2) & MASK;
            final int i4 = ((len - 1) >>> BITS3) & MASK;
            final int i5 = ((len - 1) >>> BITS4) & MASK;
            final int i6 = (len - 1) >>> BITS5;
            final Object[] data = Arrays.copyOfRange(a6, 1, i6);
            final Object[] prefix5 = copyTail(at(a6, 0));
            final Object[] prefix4 = copyTail(at(at(a6, 0), 0));
            final Object[] prefix3 = copyTail(at(at(at(a6, 0), 0), 0));
            final Object[] prefix2 = copyTail(at(at(at(at(a6, 0), 0), 0), 0));
            final Object[] prefix1 = at(at(at(at(at(a6, 0), 0), 0), 0), 0);
            final Object[] suffix5 = Arrays.copyOf(at(a6, i6), i5);
            final Object[] suffix4 = Arrays.copyOf(at(at(a6, i6), i5), i4);
            final Object[] suffix3 = Arrays.copyOf(at(at(at(a6, i6), i5), i4), i3);
            final Object[] suffix2 = Arrays.copyOf(at(at(at(at(a6, i6), i5), i4), i3), i2);
            final Object[] suffix1 = copyIfDifferentSize(at(at(at(at(at(a6, i6), i5), i4), i3), i2), i1 + 1);
            final int len1 = prefix1.length;
            final int len12 = len1 + prefix2.length * WIDTH;
            final int len123 = len12 + prefix3.length * WIDTH2;
            final int len1234 = len123 + prefix4.length * WIDTH3;
            final int len12345 = len1234 + prefix5.length * WIDTH4;
            result = new RadixVector.Vector6<>(prefix1, len1, prefix2, len12, prefix3, len123, prefix4, len1234, prefix5, len12345, data,
                suffix5, suffix4, suffix3, suffix2, suffix1, realLen);
        }
        close();
        return result;
    }

    /* the arrays now belong to the vector: drop them, and leave len1 == WIDTH so that add() reaches checkOpen() */
    private void close() {
        done = true;
        a1 = EMPTY;
        a2 = EMPTY;
        a3 = EMPTY;
        a4 = EMPTY;
        a5 = EMPTY;
        a6 = EMPTY;
        len1 = WIDTH;
        lenRest = 0;
        offset = 0;
    }
}
