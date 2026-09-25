package com.guizmaii.zazr.collection.internal;

import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import org.jspecify.annotations.Nullable;

import static com.guizmaii.zazr.collection.internal.VectorStatics.*;

/**
 * A persistent vector stored as a radix-balanced finger tree of width 32, ported from
 * {@code scala.collection.immutable.Vector} of the Scala 3 standard library (which ships the Scala 2.13.18 collections
 * unchanged; the source is {@code scala/collection/immutable/Vector.scala}).
 * <p>
 * There is one final class per depth: {@link Vector0} (empty), {@link Vector1} (one leaf of up to 32 elements) and
 * {@link Vector2} .. {@link Vector6}. A {@code VectorN} keeps its elements in {@code 2N - 1} slices, from left to right
 * {@code prefix1, prefix2, .., prefix(N-1), dataN, suffix(N-1), .., suffix2, suffix1}, where a slice of dimension
 * {@code d} is an array of arrays nested {@code d} deep (dimension 1 is a leaf of elements). So {@code head},
 * {@code last}, {@code prepended} and {@code appended} touch only the outer leaves, and a slice reuses whole
 * sub-arrays. The running counts {@code len1}, {@code len12}, .. are the number of elements up to the end of each
 * prefix, for indexing without reading the prefix arrays.
 * <p>
 * Balancing rules, as in Scala:
 * <ul>
 *   <li>only the outermost dimension of an array may hold fewer than 32 entries: every array below it is full;</li>
 *   <li>{@code prefix1} and {@code suffix1} hold 1 to 32 elements; the other prefixes and suffixes hold 0 to 31
 *   entries; {@code dataN} holds 0 to 30 entries (0 to 62 for {@code data6}, whose top level has one more bit);</li>
 *   <li>prepending never touches the suffixes and appending never touches the prefixes: the depth grows when the side
 *   being filled and the data are full;</li>
 *   <li>arrays are left-aligned and truncated to their content.</li>
 * </ul>
 * The depth is not always the minimal one for the length: a {@code Vector3} whose elements were dropped from both ends
 * may hold fewer than 1024 elements.
 * <p>
 * Every level is an {@code Object[]} (see {@link VectorStatics}). Elements are never null. Arrays are never written
 * after a vector holding them has been built: every operation copies the arrays it changes.
 *
 * @param <T> the element type
 */
public abstract sealed class RadixVector<T extends @Nullable Object> implements Iterable<T> {

    /* the first leaf: all the elements of a Vector1, the prefix of a bigger one, empty only in Vector0 */
    final Object[] prefix1;

    RadixVector(Object[] prefix1) {
        this.prefix1 = prefix1;
    }

    // ---------------------------------------------------------------------------------------------------------------
    // construction

    /** The empty vector. */
    @SuppressWarnings("unchecked")
    public static <T extends @Nullable Object> RadixVector<T> empty() {
        return (RadixVector<T>) Vector0.INSTANCE;
    }

    /** A vector of one element. */
    public static <T extends @Nullable Object> RadixVector<T> of(T element) {
        return new Vector1<>(wrap1(Objects.requireNonNull(element, "Vector: element is null")));
    }

    /** A vector of the elements of {@code elements}, which is copied. */
    @SafeVarargs
    public static <T extends @Nullable Object> RadixVector<T> of(T... elements) {
        return ofAll(elements);
    }

    /** A vector of the elements of {@code elements}, which is copied (never adopted: the caller may reuse it). */
    public static <T extends @Nullable Object> RadixVector<T> ofAll(@Nullable Object[] elements) {
        final int n = elements.length;
        if (n == 0) {
            return empty();
        } else if (n <= WIDTH) {
            final Object[] a1 = Arrays.copyOf(elements, n, Object[].class);
            for (Object element : a1) {
                Objects.requireNonNull(element, "Vector: element is null");
            }
            return new Vector1<>(a1);
        } else {
            return new VectorBuilder<T>().addArray(elements, 0, n).result();
        }
    }

    /** A vector of the elements of {@code elements}, in iteration order; a {@code RadixVector} is returned as is. */
    @SuppressWarnings("unchecked")
    public static <T extends @Nullable Object> RadixVector<T> ofAll(Iterable<? extends T> elements) {
        Objects.requireNonNull(elements, "elements is null");
        if (elements instanceof RadixVector<?> v) {
            return (RadixVector<T>) v;
        } else if (elements instanceof java.util.Collection<?> c) {
            return ofAll(c.toArray());
        } else {
            return new VectorBuilder<T>().addAll(elements).result();
        }
    }

    /** A new single-shot builder. */
    public static <T extends @Nullable Object> VectorBuilder<T> newBuilder() {
        return new VectorBuilder<>();
    }

    // ---------------------------------------------------------------------------------------------------------------
    // the shape

    /** The number of elements. */
    public final int length() {
        return (this instanceof BigVector<?> big) ? big.length0 : prefix1.length;
    }

    public final boolean isEmpty() {
        return length() == 0;
    }

    /* the number of slices: 2N - 1 for a VectorN, 0 for Vector0 */
    abstract int vectorSliceCount();

    /* the slice at index idx, of dimension vectorSliceDim(vectorSliceCount(), idx) */
    abstract Object[] vectorSlice(int idx);

    /* the number of elements in the slices 0 .. idx */
    abstract int vectorSlicePrefixLength(int idx);

    // ---------------------------------------------------------------------------------------------------------------
    // access and update

    /**
     * The element at {@code index}.
     *
     * @throws IndexOutOfBoundsException if {@code index} is not in {@code [0, length())}
     */
    public abstract T get(int index);

    /**
     * This vector with {@code element} at {@code index}.
     *
     * @throws IndexOutOfBoundsException if {@code index} is not in {@code [0, length())}
     * @throws NullPointerException if {@code element} is null
     */
    public final RadixVector<T> updated(int index, T element) {
        return updated0(index, Objects.requireNonNull(element, "Vector: element is null"));
    }

    abstract RadixVector<T> updated0(int index, Object element);

    /** @throws NullPointerException if {@code element} is null */
    public final RadixVector<T> appended(T element) {
        return appended0(Objects.requireNonNull(element, "Vector: element is null"));
    }

    abstract RadixVector<T> appended0(Object element);

    /** @throws NullPointerException if {@code element} is null */
    public final RadixVector<T> prepended(T element) {
        return prepended0(Objects.requireNonNull(element, "Vector: element is null"));
    }

    abstract RadixVector<T> prepended0(Object element);

    /** @throws NoSuchElementException if this vector is empty */
    @SuppressWarnings("unchecked")
    public final T head() {
        if (prefix1.length == 0) {
            throw new NoSuchElementException("head of empty Vector");
        }
        return (T) prefix1[0];
    }

    /** @throws NoSuchElementException if this vector is empty */
    @SuppressWarnings("unchecked")
    public final T last() {
        final Object[] a = (this instanceof BigVector<?> big) ? big.suffix1 : prefix1;
        if (a.length == 0) {
            throw new NoSuchElementException("last of empty Vector");
        }
        return (T) a[a.length - 1];
    }

    // ---------------------------------------------------------------------------------------------------------------
    // slicing

    /** The elements in {@code [max(from, 0), min(until, length()))}; this vector when that is all of it. */
    public final RadixVector<T> slice(int from, int until) {
        final int lo = Math.max(from, 0);
        final int hi = Math.min(until, length());
        if (hi <= lo) {
            return empty();
        } else if (hi - lo == length()) {
            return this;
        } else {
            return slice0(lo, hi);
        }
    }

    /* slice with 0 <= lo < hi <= length() and hi - lo < length() */
    abstract RadixVector<T> slice0(int lo, int hi);

    public final RadixVector<T> take(int n) {
        return slice(0, n);
    }

    public final RadixVector<T> drop(int n) {
        return slice(n, length());
    }

    public final RadixVector<T> takeRight(int n) {
        return slice(length() - Math.max(n, 0), length());
    }

    public final RadixVector<T> dropRight(int n) {
        return slice(0, length() - Math.max(n, 0));
    }

    /** @throws UnsupportedOperationException if this vector is empty */
    public abstract RadixVector<T> tail();

    /** @throws UnsupportedOperationException if this vector is empty */
    public abstract RadixVector<T> init();

    // ---------------------------------------------------------------------------------------------------------------
    // bulk append and prepend

    /**
     * This vector followed by the elements of {@code suffix}, in iteration order. Another {@code RadixVector} is
     * appended by whole arrays.
     *
     * @throws NullPointerException if {@code suffix} is null or yields a null element
     */
    public final RadixVector<T> appendedAll(Iterable<? extends T> suffix) {
        Objects.requireNonNull(suffix, "suffix is null");
        final int k = knownSize(suffix);
        if (k == 0) {
            return this;
        } else if (k < 0) {
            return new VectorBuilder<T>().addAll(this).addAll(suffix).result();
        } else {
            return appendedAll0(suffix, k);
        }
    }

    /**
     * The elements of {@code prefix}, in iteration order, followed by this vector. Another {@code RadixVector} is
     * prepended by whole arrays.
     *
     * @throws NullPointerException if {@code prefix} is null or yields a null element
     */
    public final RadixVector<T> prependedAll(Iterable<? extends T> prefix) {
        Objects.requireNonNull(prefix, "prefix is null");
        final int k = knownSize(prefix);
        if (k == 0) {
            return this;
        } else if (k < 0) {
            return new VectorBuilder<T>().addAll(prefix).addAll(this).result();
        } else {
            return prependedAll0(prefix, k);
        }
    }

    /* k = knownSize(prefix) > 0; the shapes first try to fit prefix in prefix1 */
    @SuppressWarnings("unchecked")
    RadixVector<T> prependedAll0(Iterable<? extends T> prefix, int k) {
        final int tinyAppendLimit = 4 + vectorSliceCount();
        if (k < tinyAppendLimit) {
            final Object[] elements = new Object[k];
            VectorStatics.copyToArray(prefix, k, elements, 0);
            RadixVector<T> v = this;
            for (int i = k - 1; i >= 0; i--) {
                v = v.prepended0(elements[i]);
            }
            return v;
        } else if (length() < (k >>> LOG2_CONCAT_FASTER) && prefix instanceof RadixVector<?> pv) {
            RadixVector<T> v = (RadixVector<T>) pv;
            final int len = length();
            for (int i = 0; i < len; i++) {
                v = v.appended0(get(i));
            }
            return v;
        } else if (k < length() - ALIGN_TO_FASTER) {
            return new VectorBuilder<T>().alignTo(k, this).addAll(prefix).addAll(this).result();
        } else {
            return new VectorBuilder<T>().addAll(prefix).addAll(this).result();
        }
    }

    /* k = knownSize(suffix) > 0; the shapes first try to fit suffix in suffix1 */
    @SuppressWarnings("unchecked")
    RadixVector<T> appendedAll0(Iterable<? extends T> suffix, int k) {
        final int tinyAppendLimit = 4 + vectorSliceCount();
        if (k < tinyAppendLimit) {
            RadixVector<T> v = this;
            for (T element : suffix) {
                v = v.appended(element);
            }
            return v;
        } else if (length() < (k >>> LOG2_CONCAT_FASTER) && suffix instanceof RadixVector<?> sv) {
            RadixVector<T> v = (RadixVector<T>) sv;
            for (int i = length() - 1; i >= 0; i--) {
                v = v.prepended0(get(i));
            }
            return v;
        } else if (length() < k - ALIGN_TO_FASTER && suffix instanceof RadixVector<?> sv) {
            final RadixVector<T> v = (RadixVector<T>) sv;
            return new VectorBuilder<T>().alignTo(length(), v).addAll(this).addAll(v).result();
        } else {
            return new VectorBuilder<T>().addAll(this).addAll(suffix).result();
        }
    }

    // ---------------------------------------------------------------------------------------------------------------
    // traversal

    @Override
    public final Iterator<T> iterator() {
        return isEmpty() ? Iterator.empty() : new VectorIterator<>(this);
    }

    /** The elements from last to first. */
    public final Iterator<T> reverseIterator() {
        return isEmpty() ? Iterator.empty() : new ReverseIterator<>(this);
    }

    @Override
    public final void forEach(Consumer<? super T> action) {
        Objects.requireNonNull(action, "action is null");
        final int c = vectorSliceCount();
        for (int i = 0; i < c; i++) {
            foreachRec(vectorSliceDim(c, i) - 1, vectorSlice(i), action);
        }
    }

    /**
     * The vector of {@code f} applied to every element, of the same shape; an array whose elements {@code f} all returns
     * unchanged (by identity) is shared, not copied.
     *
     * @throws NullPointerException if {@code f} returns null
     */
    public abstract <U extends @Nullable Object> RadixVector<U> map(Function<? super T, ? extends U> f);

    /* copies the first n elements into dest from index start, one whole-leaf copy at a time */
    final void copyToArray(Object[] dest, int start, int n) {
        final int end = start + n;
        int pos = start;
        final int c = vectorSliceCount();
        for (int i = 0; i < c && pos < end; i++) {
            pos = copyRec(vectorSliceDim(c, i) - 1, vectorSlice(i), dest, pos, end);
        }
    }

    private static int copyRec(int level, Object[] a, Object[] dest, int pos, int end) {
        if (level == 0) {
            final int k = Math.min(a.length, end - pos);
            System.arraycopy(a, 0, dest, pos, k);
            return pos + k;
        }
        for (int i = 0; i < a.length && pos < end; i++) {
            pos = copyRec(level - 1, (Object[]) a[i], dest, pos, end);
        }
        return pos;
    }

    final IndexOutOfBoundsException ioob(String operation, int index) {
        return new IndexOutOfBoundsException(operation + "(" + index + ")");
    }

    // ---------------------------------------------------------------------------------------------------------------
    // the shapes

    /* a vector with a suffix and a length field: every shape but Vector1 */
    abstract static sealed class BigVector<T extends @Nullable Object> extends RadixVector<T> {

        final Object[] suffix1;
        final int length0;

        BigVector(Object[] prefix1, Object[] suffix1, int length0) {
            super(prefix1);
            this.suffix1 = suffix1;
            this.length0 = length0;
        }

        final RadixVector<T> sliceWith(VectorSliceBuilder b) {
            final int c = vectorSliceCount();
            for (int i = 0; i < c; i++) {
                b.consider(vectorSliceDim(c, i), vectorSlice(i));
            }
            return b.result();
        }
    }

    /** The empty vector. */
    static final class Vector0<T extends @Nullable Object> extends BigVector<T> {

        static final Vector0<?> INSTANCE = new Vector0<>();

        private Vector0() {
            super(EMPTY, EMPTY, 0);
        }

        @Override
        public T get(int index) {
            throw ioob("get", index);
        }

        @Override
        RadixVector<T> updated0(int index, Object element) {
            throw ioob("update", index);
        }

        @Override
        RadixVector<T> appended0(Object element) {
            return new Vector1<>(wrap1(element));
        }

        @Override
        RadixVector<T> prepended0(Object element) {
            return new Vector1<>(wrap1(element));
        }

        @Override
        public <U extends @Nullable Object> RadixVector<U> map(Function<? super T, ? extends U> f) {
            return empty();
        }

        @Override
        public RadixVector<T> tail() {
            throw new UnsupportedOperationException("tail of empty Vector");
        }

        @Override
        public RadixVector<T> init() {
            throw new UnsupportedOperationException("init of empty Vector");
        }

        @Override
        RadixVector<T> slice0(int lo, int hi) {
            return this;
        }

        @Override
        int vectorSliceCount() {
            return 0;
        }

        @Override
        Object[] vectorSlice(int idx) {
            throw new IndexOutOfBoundsException(idx);
        }

        @Override
        int vectorSlicePrefixLength(int idx) {
            return 0;
        }

        @Override
        RadixVector<T> prependedAll0(Iterable<? extends T> prefix, int k) {
            return ofAll(prefix);
        }

        @Override
        RadixVector<T> appendedAll0(Iterable<? extends T> suffix, int k) {
            return ofAll(suffix);
        }
    }

    /** One leaf of 1 to 32 elements. */
    static final class Vector1<T extends @Nullable Object> extends RadixVector<T> {

        Vector1(Object[] data1) {
            super(data1);
        }

        @Override
        @SuppressWarnings("unchecked")
        public T get(int index) {
            if (index >= 0 && index < prefix1.length) {
                return (T) prefix1[index];
            }
            throw ioob("get", index);
        }

        @Override
        RadixVector<T> updated0(int index, Object element) {
            if (index >= 0 && index < prefix1.length) {
                return new Vector1<>(copyUpdate(prefix1, index, element));
            }
            throw ioob("update", index);
        }

        @Override
        RadixVector<T> appended0(Object element) {
            final int len1 = prefix1.length;
            if (len1 < WIDTH) {
                return new Vector1<>(copyAppend(prefix1, element));
            } else {
                return new Vector2<>(prefix1, WIDTH, EMPTY, wrap1(element), WIDTH + 1);
            }
        }

        @Override
        RadixVector<T> prepended0(Object element) {
            final int len1 = prefix1.length;
            if (len1 < WIDTH) {
                return new Vector1<>(copyPrepend(element, prefix1));
            } else {
                return new Vector2<>(wrap1(element), 1, EMPTY, prefix1, len1 + 1);
            }
        }

        @Override
        public <U extends @Nullable Object> RadixVector<U> map(Function<? super T, ? extends U> f) {
            return new Vector1<>(mapElems1(prefix1, f));
        }

        @Override
        RadixVector<T> slice0(int lo, int hi) {
            return new Vector1<>(Arrays.copyOfRange(prefix1, lo, hi));
        }

        @Override
        public RadixVector<T> tail() {
            return (prefix1.length == 1) ? empty() : new Vector1<>(copyTail(prefix1));
        }

        @Override
        public RadixVector<T> init() {
            return (prefix1.length == 1) ? empty() : new Vector1<>(copyInit(prefix1));
        }

        @Override
        int vectorSliceCount() {
            return 1;
        }

        @Override
        Object[] vectorSlice(int idx) {
            return prefix1;
        }

        @Override
        int vectorSlicePrefixLength(int idx) {
            return prefix1.length;
        }

        @Override
        RadixVector<T> prependedAll0(Iterable<? extends T> prefix, int k) {
            final Object[] data1b = prepend1IfSpace(prefix1, prefix, k);
            return (data1b == null) ? super.prependedAll0(prefix, k) : new Vector1<>(data1b);
        }

        @Override
        RadixVector<T> appendedAll0(Iterable<? extends T> suffix, int k) {
            final Object[] data1b = append1IfSpace(prefix1, suffix, k);
            return (data1b == null) ? super.appendedAll0(suffix, k) : new Vector1<>(data1b);
        }
    }

    /** A 2-dimensional radix-balanced finger tree. */
    static final class Vector2<T extends @Nullable Object> extends BigVector<T> {

        final int len1;
        final Object[] data2;

        Vector2(Object[] prefix1, int len1, Object[] data2, Object[] suffix1, int length0) {
            super(prefix1, suffix1, length0);
            this.len1 = len1;
            this.data2 = data2;
        }

        @Override
        @SuppressWarnings("unchecked")
        public T get(int index) {
            if (index >= 0 && index < length0) {
                final int io = index - len1;
                if (io >= 0) {
                    final int i2 = io >>> BITS;
                    final int i1 = io & MASK;
                    if (i2 < data2.length) {
                        return (T) ((Object[]) data2[i2])[i1];
                    } else {
                        return (T) suffix1[io & MASK];
                    }
                } else {
                    return (T) prefix1[index];
                }
            }
            throw ioob("get", index);
        }

        @Override
        RadixVector<T> updated0(int index, Object elem) {
            if (index >= 0 && index < length0) {
                if (index >= len1) {
                    final int io = index - len1;
                    final int i2 = io >>> BITS;
                    final int i1 = io & MASK;
                    if (i2 < data2.length) {
                        return new Vector2<>(prefix1, len1, copyUpdate(data2, i2, i1, elem), suffix1, length0);
                    } else {
                        return new Vector2<>(prefix1, len1, data2, copyUpdate(suffix1, i1, elem), length0);
                    }
                } else {
                    return new Vector2<>(copyUpdate(prefix1, index, elem), len1, data2, suffix1, length0);
                }
            }
            throw ioob("update", index);
        }

        @Override
        RadixVector<T> appended0(Object elem) {
            if (suffix1.length < WIDTH) {
                return new Vector2<>(prefix1, len1, data2, copyAppend(suffix1, elem), length0 + 1);
            } else if (data2.length < WIDTH - 2) {
                return new Vector2<>(prefix1, len1, copyAppend(data2, suffix1), wrap1(elem), length0 + 1);
            } else {
                return new Vector3<>(prefix1, len1, data2, WIDTH * (WIDTH - 2) + len1, EMPTY, wrap1(suffix1), wrap1(elem), length0 + 1);
            }
        }

        @Override
        RadixVector<T> prepended0(Object elem) {
            if (len1 < WIDTH) {
                return new Vector2<>(copyPrepend(elem, prefix1), len1 + 1, data2, suffix1, length0 + 1);
            } else if (data2.length < WIDTH - 2) {
                return new Vector2<>(wrap1(elem), 1, copyPrepend(prefix1, data2), suffix1, length0 + 1);
            } else {
                return new Vector3<>(wrap1(elem), 1, wrap1(prefix1), len1 + 1, EMPTY, data2, suffix1, length0 + 1);
            }
        }

        @Override
        public <U extends @Nullable Object> RadixVector<U> map(Function<? super T, ? extends U> f) {
            return new Vector2<>(mapElems1(prefix1, f), len1, mapElems(2, data2, f), mapElems1(suffix1, f), length0);
        }

        @Override
        RadixVector<T> slice0(int lo, int hi) {
            return sliceWith(new VectorSliceBuilder(lo, hi));
        }

        @Override
        public RadixVector<T> tail() {
            return (len1 > 1) ? new Vector2<>(copyTail(prefix1), len1 - 1, data2, suffix1, length0 - 1) : slice0(1, length0);
        }

        @Override
        public RadixVector<T> init() {
            return (suffix1.length > 1) ? new Vector2<>(prefix1, len1, data2, copyInit(suffix1), length0 - 1) : slice0(0, length0 - 1);
        }

        @Override
        int vectorSliceCount() {
            return 3;
        }

        @Override
        Object[] vectorSlice(int idx) {
            return switch (idx) {
                case 0 -> prefix1;
                case 1 -> data2;
                case 2 -> suffix1;
                default -> throw new IndexOutOfBoundsException(idx);
            };
        }

        @Override
        int vectorSlicePrefixLength(int idx) {
            return switch (idx) {
                case 0 -> len1;
                case 1 -> length0 - suffix1.length;
                case 2 -> length0;
                default -> throw new IndexOutOfBoundsException(idx);
            };
        }

        @Override
        RadixVector<T> prependedAll0(Iterable<? extends T> prefix, int k) {
            final Object[] prefix1b = prepend1IfSpace(prefix1, prefix, k);
            if (prefix1b == null) {
                return super.prependedAll0(prefix, k);
            }
            final int diff = prefix1b.length - prefix1.length;
            return new Vector2<>(prefix1b, len1 + diff, data2, suffix1, length0 + diff);
        }

        @Override
        RadixVector<T> appendedAll0(Iterable<? extends T> suffix, int k) {
            final Object[] suffix1b = append1IfSpace(suffix1, suffix, k);
            if (suffix1b == null) {
                return super.appendedAll0(suffix, k);
            }
            return new Vector2<>(prefix1, len1, data2, suffix1b, length0 - suffix1.length + suffix1b.length);
        }
    }

    /** A 3-dimensional radix-balanced finger tree. */
    static final class Vector3<T extends @Nullable Object> extends BigVector<T> {

        final int len1;
        final Object[] prefix2;
        final int len12;
        final Object[] data3;
        final Object[] suffix2;

        Vector3(Object[] prefix1, int len1, Object[] prefix2, int len12, Object[] data3, Object[] suffix2, Object[] suffix1, int length0) {
            super(prefix1, suffix1, length0);
            this.len1 = len1;
            this.prefix2 = prefix2;
            this.len12 = len12;
            this.data3 = data3;
            this.suffix2 = suffix2;
        }

        @Override
        @SuppressWarnings("unchecked")
        public T get(int index) {
            if (index >= 0 && index < length0) {
                final int io = index - len12;
                if (io >= 0) {
                    final int i3 = io >>> BITS2;
                    final int i2 = (io >>> BITS) & MASK;
                    final int i1 = io & MASK;
                    if (i3 < data3.length) {
                        return (T) ((Object[]) ((Object[]) data3[i3])[i2])[i1];
                    } else if (i2 < suffix2.length) {
                        return (T) ((Object[]) suffix2[i2])[i1];
                    } else {
                        return (T) suffix1[i1];
                    }
                } else if (index >= len1) {
                    final int io2 = index - len1;
                    return (T) ((Object[]) prefix2[io2 >>> BITS])[io2 & MASK];
                } else {
                    return (T) prefix1[index];
                }
            }
            throw ioob("get", index);
        }

        @Override
        RadixVector<T> updated0(int index, Object elem) {
            if (index >= 0 && index < length0) {
                if (index >= len12) {
                    final int io = index - len12;
                    final int i3 = io >>> BITS2;
                    final int i2 = (io >>> BITS) & MASK;
                    final int i1 = io & MASK;
                    if (i3 < data3.length) {
                        return new Vector3<>(prefix1, len1, prefix2, len12, copyUpdate(data3, i3, i2, i1, elem), suffix2, suffix1, length0);
                    } else if (i2 < suffix2.length) {
                        return new Vector3<>(prefix1, len1, prefix2, len12, data3, copyUpdate(suffix2, i2, i1, elem), suffix1, length0);
                    } else {
                        return new Vector3<>(prefix1, len1, prefix2, len12, data3, suffix2, copyUpdate(suffix1, i1, elem), length0);
                    }
                } else if (index >= len1) {
                    final int io = index - len1;
                    return new Vector3<>(prefix1, len1, copyUpdate(prefix2, io >>> BITS, io & MASK, elem), len12, data3, suffix2, suffix1, length0);
                } else {
                    return new Vector3<>(copyUpdate(prefix1, index, elem), len1, prefix2, len12, data3, suffix2, suffix1, length0);
                }
            }
            throw ioob("update", index);
        }

        @Override
        RadixVector<T> appended0(Object elem) {
            if (suffix1.length < WIDTH) {
                return new Vector3<>(prefix1, len1, prefix2, len12, data3, suffix2, copyAppend(suffix1, elem), length0 + 1);
            } else if (suffix2.length < WIDTH - 1) {
                return new Vector3<>(prefix1, len1, prefix2, len12, data3, copyAppend(suffix2, suffix1), wrap1(elem), length0 + 1);
            } else if (data3.length < WIDTH - 2) {
                return new Vector3<>(prefix1, len1, prefix2, len12, copyAppend(data3, copyAppend(suffix2, suffix1)), EMPTY, wrap1(elem), length0 + 1);
            } else {
                return new Vector4<>(prefix1, len1, prefix2, len12, data3, (WIDTH - 2) * WIDTH2 + len12, EMPTY,
                    wrap1(copyAppend(suffix2, suffix1)), EMPTY, wrap1(elem), length0 + 1);
            }
        }

        @Override
        RadixVector<T> prepended0(Object elem) {
            if (len1 < WIDTH) {
                return new Vector3<>(copyPrepend(elem, prefix1), len1 + 1, prefix2, len12 + 1, data3, suffix2, suffix1, length0 + 1);
            } else if (len12 < WIDTH2) {
                return new Vector3<>(wrap1(elem), 1, copyPrepend(prefix1, prefix2), len12 + 1, data3, suffix2, suffix1, length0 + 1);
            } else if (data3.length < WIDTH - 2) {
                return new Vector3<>(wrap1(elem), 1, EMPTY, 1, copyPrepend(copyPrepend(prefix1, prefix2), data3), suffix2, suffix1, length0 + 1);
            } else {
                return new Vector4<>(wrap1(elem), 1, EMPTY, 1, wrap1(copyPrepend(prefix1, prefix2)), len12 + 1, EMPTY, data3,
                    suffix2, suffix1, length0 + 1);
            }
        }

        @Override
        public <U extends @Nullable Object> RadixVector<U> map(Function<? super T, ? extends U> f) {
            return new Vector3<>(mapElems1(prefix1, f), len1, mapElems(2, prefix2, f), len12, mapElems(3, data3, f),
                mapElems(2, suffix2, f), mapElems1(suffix1, f), length0);
        }

        @Override
        RadixVector<T> slice0(int lo, int hi) {
            return sliceWith(new VectorSliceBuilder(lo, hi));
        }

        @Override
        public RadixVector<T> tail() {
            return (len1 > 1)
                   ? new Vector3<>(copyTail(prefix1), len1 - 1, prefix2, len12 - 1, data3, suffix2, suffix1, length0 - 1)
                   : slice0(1, length0);
        }

        @Override
        public RadixVector<T> init() {
            return (suffix1.length > 1)
                   ? new Vector3<>(prefix1, len1, prefix2, len12, data3, suffix2, copyInit(suffix1), length0 - 1)
                   : slice0(0, length0 - 1);
        }

        @Override
        int vectorSliceCount() {
            return 5;
        }

        @Override
        Object[] vectorSlice(int idx) {
            return switch (idx) {
                case 0 -> prefix1;
                case 1 -> prefix2;
                case 2 -> data3;
                case 3 -> suffix2;
                case 4 -> suffix1;
                default -> throw new IndexOutOfBoundsException(idx);
            };
        }

        @Override
        int vectorSlicePrefixLength(int idx) {
            return switch (idx) {
                case 0 -> len1;
                case 1 -> len12;
                case 2 -> len12 + data3.length * WIDTH2;
                case 3 -> length0 - suffix1.length;
                case 4 -> length0;
                default -> throw new IndexOutOfBoundsException(idx);
            };
        }

        @Override
        RadixVector<T> prependedAll0(Iterable<? extends T> prefix, int k) {
            final Object[] prefix1b = prepend1IfSpace(prefix1, prefix, k);
            if (prefix1b == null) {
                return super.prependedAll0(prefix, k);
            }
            final int diff = prefix1b.length - prefix1.length;
            return new Vector3<>(prefix1b, len1 + diff, prefix2, len12 + diff, data3, suffix2, suffix1, length0 + diff);
        }

        @Override
        RadixVector<T> appendedAll0(Iterable<? extends T> suffix, int k) {
            final Object[] suffix1b = append1IfSpace(suffix1, suffix, k);
            if (suffix1b == null) {
                return super.appendedAll0(suffix, k);
            }
            return new Vector3<>(prefix1, len1, prefix2, len12, data3, suffix2, suffix1b, length0 - suffix1.length + suffix1b.length);
        }
    }

    /** A 4-dimensional radix-balanced finger tree. */
    static final class Vector4<T extends @Nullable Object> extends BigVector<T> {

        final int len1;
        final Object[] prefix2;
        final int len12;
        final Object[] prefix3;
        final int len123;
        final Object[] data4;
        final Object[] suffix3;
        final Object[] suffix2;

        Vector4(Object[] prefix1, int len1, Object[] prefix2, int len12, Object[] prefix3, int len123, Object[] data4,
                Object[] suffix3, Object[] suffix2, Object[] suffix1, int length0) {
            super(prefix1, suffix1, length0);
            this.len1 = len1;
            this.prefix2 = prefix2;
            this.len12 = len12;
            this.prefix3 = prefix3;
            this.len123 = len123;
            this.data4 = data4;
            this.suffix3 = suffix3;
            this.suffix2 = suffix2;
        }

        @Override
        @SuppressWarnings("unchecked")
        public T get(int index) {
            if (index >= 0 && index < length0) {
                final int io = index - len123;
                if (io >= 0) {
                    final int i4 = io >>> BITS3;
                    final int i3 = (io >>> BITS2) & MASK;
                    final int i2 = (io >>> BITS) & MASK;
                    final int i1 = io & MASK;
                    if (i4 < data4.length) {
                        return (T) ((Object[]) ((Object[]) ((Object[]) data4[i4])[i3])[i2])[i1];
                    } else if (i3 < suffix3.length) {
                        return (T) ((Object[]) ((Object[]) suffix3[i3])[i2])[i1];
                    } else if (i2 < suffix2.length) {
                        return (T) ((Object[]) suffix2[i2])[i1];
                    } else {
                        return (T) suffix1[i1];
                    }
                } else if (index >= len12) {
                    final int io3 = index - len12;
                    return (T) ((Object[]) ((Object[]) prefix3[io3 >>> BITS2])[(io3 >>> BITS) & MASK])[io3 & MASK];
                } else if (index >= len1) {
                    final int io2 = index - len1;
                    return (T) ((Object[]) prefix2[io2 >>> BITS])[io2 & MASK];
                } else {
                    return (T) prefix1[index];
                }
            }
            throw ioob("get", index);
        }

        @Override
        RadixVector<T> updated0(int index, Object elem) {
            if (index >= 0 && index < length0) {
                if (index >= len123) {
                    final int io = index - len123;
                    final int i4 = io >>> BITS3;
                    final int i3 = (io >>> BITS2) & MASK;
                    final int i2 = (io >>> BITS) & MASK;
                    final int i1 = io & MASK;
                    if (i4 < data4.length) {
                        return new Vector4<>(prefix1, len1, prefix2, len12, prefix3, len123, copyUpdate(data4, i4, i3, i2, i1, elem), suffix3, suffix2, suffix1, length0);
                    } else if (i3 < suffix3.length) {
                        return new Vector4<>(prefix1, len1, prefix2, len12, prefix3, len123, data4, copyUpdate(suffix3, i3, i2, i1, elem), suffix2, suffix1, length0);
                    } else if (i2 < suffix2.length) {
                        return new Vector4<>(prefix1, len1, prefix2, len12, prefix3, len123, data4, suffix3, copyUpdate(suffix2, i2, i1, elem), suffix1, length0);
                    } else {
                        return new Vector4<>(prefix1, len1, prefix2, len12, prefix3, len123, data4, suffix3, suffix2, copyUpdate(suffix1, i1, elem), length0);
                    }
                } else if (index >= len12) {
                    final int io = index - len12;
                    return new Vector4<>(prefix1, len1, prefix2, len12, copyUpdate(prefix3, io >>> BITS2, (io >>> BITS) & MASK, io & MASK, elem), len123, data4, suffix3, suffix2, suffix1, length0);
                } else if (index >= len1) {
                    final int io = index - len1;
                    return new Vector4<>(prefix1, len1, copyUpdate(prefix2, io >>> BITS, io & MASK, elem), len12, prefix3, len123, data4, suffix3, suffix2, suffix1, length0);
                } else {
                    return new Vector4<>(copyUpdate(prefix1, index, elem), len1, prefix2, len12, prefix3, len123, data4, suffix3, suffix2, suffix1, length0);
                }
            }
            throw ioob("update", index);
        }

        @Override
        RadixVector<T> appended0(Object elem) {
            if (suffix1.length < WIDTH) {
                return new Vector4<>(prefix1, len1, prefix2, len12, prefix3, len123, data4, suffix3, suffix2, copyAppend(suffix1, elem), length0 + 1);
            } else if (suffix2.length < WIDTH - 1) {
                return new Vector4<>(prefix1, len1, prefix2, len12, prefix3, len123, data4, suffix3, copyAppend(suffix2, suffix1), wrap1(elem), length0 + 1);
            } else if (suffix3.length < WIDTH - 1) {
                return new Vector4<>(prefix1, len1, prefix2, len12, prefix3, len123, data4, copyAppend(suffix3, copyAppend(suffix2, suffix1)), EMPTY, wrap1(elem), length0 + 1);
            } else if (data4.length < WIDTH - 2) {
                return new Vector4<>(prefix1, len1, prefix2, len12, prefix3, len123, copyAppend(data4, copyAppend(suffix3, copyAppend(suffix2, suffix1))), EMPTY, EMPTY, wrap1(elem), length0 + 1);
            } else {
                return new Vector5<>(prefix1, len1, prefix2, len12, prefix3, len123, data4, (WIDTH - 2) * WIDTH3 + len123, EMPTY,
                    wrap1(copyAppend(suffix3, copyAppend(suffix2, suffix1))), EMPTY, EMPTY, wrap1(elem), length0 + 1);
            }
        }

        @Override
        RadixVector<T> prepended0(Object elem) {
            if (len1 < WIDTH) {
                return new Vector4<>(copyPrepend(elem, prefix1), len1 + 1, prefix2, len12 + 1, prefix3, len123 + 1, data4, suffix3, suffix2, suffix1, length0 + 1);
            } else if (len12 < WIDTH2) {
                return new Vector4<>(wrap1(elem), 1, copyPrepend(prefix1, prefix2), len12 + 1, prefix3, len123 + 1, data4, suffix3, suffix2, suffix1, length0 + 1);
            } else if (len123 < WIDTH3) {
                return new Vector4<>(wrap1(elem), 1, EMPTY, 1, copyPrepend(copyPrepend(prefix1, prefix2), prefix3), len123 + 1, data4, suffix3, suffix2, suffix1, length0 + 1);
            } else if (data4.length < WIDTH - 2) {
                return new Vector4<>(wrap1(elem), 1, EMPTY, 1, EMPTY, 1, copyPrepend(copyPrepend(copyPrepend(prefix1, prefix2), prefix3), data4), suffix3, suffix2, suffix1, length0 + 1);
            } else {
                return new Vector5<>(wrap1(elem), 1, EMPTY, 1, EMPTY, 1, wrap1(copyPrepend(copyPrepend(prefix1, prefix2), prefix3)), len123 + 1, EMPTY, data4,
                    suffix3, suffix2, suffix1, length0 + 1);
            }
        }

        @Override
        public <U extends @Nullable Object> RadixVector<U> map(Function<? super T, ? extends U> f) {
            return new Vector4<>(mapElems1(prefix1, f), len1, mapElems(2, prefix2, f), len12, mapElems(3, prefix3, f), len123,
                mapElems(4, data4, f), mapElems(3, suffix3, f), mapElems(2, suffix2, f), mapElems1(suffix1, f), length0);
        }

        @Override
        RadixVector<T> slice0(int lo, int hi) {
            return sliceWith(new VectorSliceBuilder(lo, hi));
        }

        @Override
        public RadixVector<T> tail() {
            return (len1 > 1)
                   ? new Vector4<>(copyTail(prefix1), len1 - 1, prefix2, len12 - 1, prefix3, len123 - 1, data4, suffix3, suffix2, suffix1, length0 - 1)
                   : slice0(1, length0);
        }

        @Override
        public RadixVector<T> init() {
            return (suffix1.length > 1)
                   ? new Vector4<>(prefix1, len1, prefix2, len12, prefix3, len123, data4, suffix3, suffix2, copyInit(suffix1), length0 - 1)
                   : slice0(0, length0 - 1);
        }

        @Override
        int vectorSliceCount() {
            return 7;
        }

        @Override
        Object[] vectorSlice(int idx) {
            return switch (idx) {
                case 0 -> prefix1;
                case 1 -> prefix2;
                case 2 -> prefix3;
                case 3 -> data4;
                case 4 -> suffix3;
                case 5 -> suffix2;
                case 6 -> suffix1;
                default -> throw new IndexOutOfBoundsException(idx);
            };
        }

        @Override
        int vectorSlicePrefixLength(int idx) {
            return switch (idx) {
                case 0 -> len1;
                case 1 -> len12;
                case 2 -> len123;
                case 3 -> len123 + data4.length * WIDTH3;
                case 4 -> len123 + data4.length * WIDTH3 + suffix3.length * WIDTH2;
                case 5 -> length0 - suffix1.length;
                case 6 -> length0;
                default -> throw new IndexOutOfBoundsException(idx);
            };
        }

        @Override
        RadixVector<T> prependedAll0(Iterable<? extends T> prefix, int k) {
            final Object[] prefix1b = prepend1IfSpace(prefix1, prefix, k);
            if (prefix1b == null) {
                return super.prependedAll0(prefix, k);
            }
            final int diff = prefix1b.length - prefix1.length;
            return new Vector4<>(prefix1b, len1 + diff, prefix2, len12 + diff, prefix3, len123 + diff, data4, suffix3, suffix2, suffix1, length0 + diff);
        }

        @Override
        RadixVector<T> appendedAll0(Iterable<? extends T> suffix, int k) {
            final Object[] suffix1b = append1IfSpace(suffix1, suffix, k);
            if (suffix1b == null) {
                return super.appendedAll0(suffix, k);
            }
            return new Vector4<>(prefix1, len1, prefix2, len12, prefix3, len123, data4, suffix3, suffix2, suffix1b, length0 - suffix1.length + suffix1b.length);
        }
    }

    /** A 5-dimensional radix-balanced finger tree. */
    static final class Vector5<T extends @Nullable Object> extends BigVector<T> {

        final int len1;
        final Object[] prefix2;
        final int len12;
        final Object[] prefix3;
        final int len123;
        final Object[] prefix4;
        final int len1234;
        final Object[] data5;
        final Object[] suffix4;
        final Object[] suffix3;
        final Object[] suffix2;

        Vector5(Object[] prefix1, int len1, Object[] prefix2, int len12, Object[] prefix3, int len123, Object[] prefix4, int len1234,
                Object[] data5, Object[] suffix4, Object[] suffix3, Object[] suffix2, Object[] suffix1, int length0) {
            super(prefix1, suffix1, length0);
            this.len1 = len1;
            this.prefix2 = prefix2;
            this.len12 = len12;
            this.prefix3 = prefix3;
            this.len123 = len123;
            this.prefix4 = prefix4;
            this.len1234 = len1234;
            this.data5 = data5;
            this.suffix4 = suffix4;
            this.suffix3 = suffix3;
            this.suffix2 = suffix2;
        }

        @Override
        @SuppressWarnings("unchecked")
        public T get(int index) {
            if (index >= 0 && index < length0) {
                final int io = index - len1234;
                if (io >= 0) {
                    final int i5 = io >>> BITS4;
                    final int i4 = (io >>> BITS3) & MASK;
                    final int i3 = (io >>> BITS2) & MASK;
                    final int i2 = (io >>> BITS) & MASK;
                    final int i1 = io & MASK;
                    if (i5 < data5.length) {
                        return (T) ((Object[]) ((Object[]) ((Object[]) ((Object[]) data5[i5])[i4])[i3])[i2])[i1];
                    } else if (i4 < suffix4.length) {
                        return (T) ((Object[]) ((Object[]) ((Object[]) suffix4[i4])[i3])[i2])[i1];
                    } else if (i3 < suffix3.length) {
                        return (T) ((Object[]) ((Object[]) suffix3[i3])[i2])[i1];
                    } else if (i2 < suffix2.length) {
                        return (T) ((Object[]) suffix2[i2])[i1];
                    } else {
                        return (T) suffix1[i1];
                    }
                } else if (index >= len123) {
                    final int io4 = index - len123;
                    return (T) ((Object[]) ((Object[]) ((Object[]) prefix4[io4 >>> BITS3])[(io4 >>> BITS2) & MASK])[(io4 >>> BITS) & MASK])[io4 & MASK];
                } else if (index >= len12) {
                    final int io3 = index - len12;
                    return (T) ((Object[]) ((Object[]) prefix3[io3 >>> BITS2])[(io3 >>> BITS) & MASK])[io3 & MASK];
                } else if (index >= len1) {
                    final int io2 = index - len1;
                    return (T) ((Object[]) prefix2[io2 >>> BITS])[io2 & MASK];
                } else {
                    return (T) prefix1[index];
                }
            }
            throw ioob("get", index);
        }

        @Override
        RadixVector<T> updated0(int index, Object elem) {
            if (index >= 0 && index < length0) {
                if (index >= len1234) {
                    final int io = index - len1234;
                    final int i5 = io >>> BITS4;
                    final int i4 = (io >>> BITS3) & MASK;
                    final int i3 = (io >>> BITS2) & MASK;
                    final int i2 = (io >>> BITS) & MASK;
                    final int i1 = io & MASK;
                    if (i5 < data5.length) {
                        return new Vector5<>(prefix1, len1, prefix2, len12, prefix3, len123, prefix4, len1234, copyUpdate(data5, i5, i4, i3, i2, i1, elem), suffix4, suffix3, suffix2, suffix1, length0);
                    } else if (i4 < suffix4.length) {
                        return new Vector5<>(prefix1, len1, prefix2, len12, prefix3, len123, prefix4, len1234, data5, copyUpdate(suffix4, i4, i3, i2, i1, elem), suffix3, suffix2, suffix1, length0);
                    } else if (i3 < suffix3.length) {
                        return new Vector5<>(prefix1, len1, prefix2, len12, prefix3, len123, prefix4, len1234, data5, suffix4, copyUpdate(suffix3, i3, i2, i1, elem), suffix2, suffix1, length0);
                    } else if (i2 < suffix2.length) {
                        return new Vector5<>(prefix1, len1, prefix2, len12, prefix3, len123, prefix4, len1234, data5, suffix4, suffix3, copyUpdate(suffix2, i2, i1, elem), suffix1, length0);
                    } else {
                        return new Vector5<>(prefix1, len1, prefix2, len12, prefix3, len123, prefix4, len1234, data5, suffix4, suffix3, suffix2, copyUpdate(suffix1, i1, elem), length0);
                    }
                } else if (index >= len123) {
                    final int io = index - len123;
                    return new Vector5<>(prefix1, len1, prefix2, len12, prefix3, len123, copyUpdate(prefix4, io >>> BITS3, (io >>> BITS2) & MASK, (io >>> BITS) & MASK, io & MASK, elem), len1234, data5, suffix4, suffix3, suffix2, suffix1, length0);
                } else if (index >= len12) {
                    final int io = index - len12;
                    return new Vector5<>(prefix1, len1, prefix2, len12, copyUpdate(prefix3, io >>> BITS2, (io >>> BITS) & MASK, io & MASK, elem), len123, prefix4, len1234, data5, suffix4, suffix3, suffix2, suffix1, length0);
                } else if (index >= len1) {
                    final int io = index - len1;
                    return new Vector5<>(prefix1, len1, copyUpdate(prefix2, io >>> BITS, io & MASK, elem), len12, prefix3, len123, prefix4, len1234, data5, suffix4, suffix3, suffix2, suffix1, length0);
                } else {
                    return new Vector5<>(copyUpdate(prefix1, index, elem), len1, prefix2, len12, prefix3, len123, prefix4, len1234, data5, suffix4, suffix3, suffix2, suffix1, length0);
                }
            }
            throw ioob("update", index);
        }

        @Override
        RadixVector<T> appended0(Object elem) {
            if (suffix1.length < WIDTH) {
                return new Vector5<>(prefix1, len1, prefix2, len12, prefix3, len123, prefix4, len1234, data5, suffix4, suffix3, suffix2, copyAppend(suffix1, elem), length0 + 1);
            } else if (suffix2.length < WIDTH - 1) {
                return new Vector5<>(prefix1, len1, prefix2, len12, prefix3, len123, prefix4, len1234, data5, suffix4, suffix3, copyAppend(suffix2, suffix1), wrap1(elem), length0 + 1);
            } else if (suffix3.length < WIDTH - 1) {
                return new Vector5<>(prefix1, len1, prefix2, len12, prefix3, len123, prefix4, len1234, data5, suffix4, copyAppend(suffix3, copyAppend(suffix2, suffix1)), EMPTY, wrap1(elem), length0 + 1);
            } else if (suffix4.length < WIDTH - 1) {
                return new Vector5<>(prefix1, len1, prefix2, len12, prefix3, len123, prefix4, len1234, data5, copyAppend(suffix4, copyAppend(suffix3, copyAppend(suffix2, suffix1))), EMPTY, EMPTY, wrap1(elem), length0 + 1);
            } else if (data5.length < WIDTH - 2) {
                return new Vector5<>(prefix1, len1, prefix2, len12, prefix3, len123, prefix4, len1234, copyAppend(data5, copyAppend(suffix4, copyAppend(suffix3, copyAppend(suffix2, suffix1)))), EMPTY, EMPTY, EMPTY, wrap1(elem), length0 + 1);
            } else {
                return new Vector6<>(prefix1, len1, prefix2, len12, prefix3, len123, prefix4, len1234, data5, (WIDTH - 2) * WIDTH4 + len1234, EMPTY,
                    wrap1(copyAppend(suffix4, copyAppend(suffix3, copyAppend(suffix2, suffix1)))), EMPTY, EMPTY, EMPTY, wrap1(elem), length0 + 1);
            }
        }

        @Override
        RadixVector<T> prepended0(Object elem) {
            if (len1 < WIDTH) {
                return new Vector5<>(copyPrepend(elem, prefix1), len1 + 1, prefix2, len12 + 1, prefix3, len123 + 1, prefix4, len1234 + 1, data5, suffix4, suffix3, suffix2, suffix1, length0 + 1);
            } else if (len12 < WIDTH2) {
                return new Vector5<>(wrap1(elem), 1, copyPrepend(prefix1, prefix2), len12 + 1, prefix3, len123 + 1, prefix4, len1234 + 1, data5, suffix4, suffix3, suffix2, suffix1, length0 + 1);
            } else if (len123 < WIDTH3) {
                return new Vector5<>(wrap1(elem), 1, EMPTY, 1, copyPrepend(copyPrepend(prefix1, prefix2), prefix3), len123 + 1, prefix4, len1234 + 1, data5, suffix4, suffix3, suffix2, suffix1, length0 + 1);
            } else if (len1234 < WIDTH4) {
                return new Vector5<>(wrap1(elem), 1, EMPTY, 1, EMPTY, 1, copyPrepend(copyPrepend(copyPrepend(prefix1, prefix2), prefix3), prefix4), len1234 + 1, data5, suffix4, suffix3, suffix2, suffix1, length0 + 1);
            } else if (data5.length < WIDTH - 2) {
                return new Vector5<>(wrap1(elem), 1, EMPTY, 1, EMPTY, 1, EMPTY, 1, copyPrepend(copyPrepend(copyPrepend(copyPrepend(prefix1, prefix2), prefix3), prefix4), data5), suffix4, suffix3, suffix2, suffix1, length0 + 1);
            } else {
                return new Vector6<>(wrap1(elem), 1, EMPTY, 1, EMPTY, 1, EMPTY, 1, wrap1(copyPrepend(copyPrepend(copyPrepend(prefix1, prefix2), prefix3), prefix4)), len1234 + 1, EMPTY, data5,
                    suffix4, suffix3, suffix2, suffix1, length0 + 1);
            }
        }

        @Override
        public <U extends @Nullable Object> RadixVector<U> map(Function<? super T, ? extends U> f) {
            return new Vector5<>(mapElems1(prefix1, f), len1, mapElems(2, prefix2, f), len12, mapElems(3, prefix3, f), len123,
                mapElems(4, prefix4, f), len1234, mapElems(5, data5, f),
                mapElems(4, suffix4, f), mapElems(3, suffix3, f), mapElems(2, suffix2, f), mapElems1(suffix1, f), length0);
        }

        @Override
        RadixVector<T> slice0(int lo, int hi) {
            return sliceWith(new VectorSliceBuilder(lo, hi));
        }

        @Override
        public RadixVector<T> tail() {
            return (len1 > 1)
                   ? new Vector5<>(copyTail(prefix1), len1 - 1, prefix2, len12 - 1, prefix3, len123 - 1, prefix4, len1234 - 1, data5, suffix4, suffix3, suffix2, suffix1, length0 - 1)
                   : slice0(1, length0);
        }

        @Override
        public RadixVector<T> init() {
            return (suffix1.length > 1)
                   ? new Vector5<>(prefix1, len1, prefix2, len12, prefix3, len123, prefix4, len1234, data5, suffix4, suffix3, suffix2, copyInit(suffix1), length0 - 1)
                   : slice0(0, length0 - 1);
        }

        @Override
        int vectorSliceCount() {
            return 9;
        }

        @Override
        Object[] vectorSlice(int idx) {
            return switch (idx) {
                case 0 -> prefix1;
                case 1 -> prefix2;
                case 2 -> prefix3;
                case 3 -> prefix4;
                case 4 -> data5;
                case 5 -> suffix4;
                case 6 -> suffix3;
                case 7 -> suffix2;
                case 8 -> suffix1;
                default -> throw new IndexOutOfBoundsException(idx);
            };
        }

        @Override
        int vectorSlicePrefixLength(int idx) {
            return switch (idx) {
                case 0 -> len1;
                case 1 -> len12;
                case 2 -> len123;
                case 3 -> len1234;
                case 4 -> len1234 + data5.length * WIDTH4;
                case 5 -> len1234 + data5.length * WIDTH4 + suffix4.length * WIDTH3;
                case 6 -> len1234 + data5.length * WIDTH4 + suffix4.length * WIDTH3 + suffix3.length * WIDTH2;
                case 7 -> length0 - suffix1.length;
                case 8 -> length0;
                default -> throw new IndexOutOfBoundsException(idx);
            };
        }

        @Override
        RadixVector<T> prependedAll0(Iterable<? extends T> prefix, int k) {
            final Object[] prefix1b = prepend1IfSpace(prefix1, prefix, k);
            if (prefix1b == null) {
                return super.prependedAll0(prefix, k);
            }
            final int diff = prefix1b.length - prefix1.length;
            return new Vector5<>(prefix1b, len1 + diff, prefix2, len12 + diff, prefix3, len123 + diff, prefix4, len1234 + diff, data5,
                suffix4, suffix3, suffix2, suffix1, length0 + diff);
        }

        @Override
        RadixVector<T> appendedAll0(Iterable<? extends T> suffix, int k) {
            final Object[] suffix1b = append1IfSpace(suffix1, suffix, k);
            if (suffix1b == null) {
                return super.appendedAll0(suffix, k);
            }
            return new Vector5<>(prefix1, len1, prefix2, len12, prefix3, len123, prefix4, len1234, data5, suffix4, suffix3, suffix2, suffix1b,
                length0 - suffix1.length + suffix1b.length);
        }
    }

    /** A 6-dimensional radix-balanced finger tree; its data holds up to 62 entries, to reach Integer.MAX_VALUE elements. */
    static final class Vector6<T extends @Nullable Object> extends BigVector<T> {

        final int len1;
        final Object[] prefix2;
        final int len12;
        final Object[] prefix3;
        final int len123;
        final Object[] prefix4;
        final int len1234;
        final Object[] prefix5;
        final int len12345;
        final Object[] data6;
        final Object[] suffix5;
        final Object[] suffix4;
        final Object[] suffix3;
        final Object[] suffix2;

        Vector6(Object[] prefix1, int len1, Object[] prefix2, int len12, Object[] prefix3, int len123, Object[] prefix4, int len1234,
                Object[] prefix5, int len12345, Object[] data6, Object[] suffix5, Object[] suffix4, Object[] suffix3, Object[] suffix2,
                Object[] suffix1, int length0) {
            super(prefix1, suffix1, length0);
            this.len1 = len1;
            this.prefix2 = prefix2;
            this.len12 = len12;
            this.prefix3 = prefix3;
            this.len123 = len123;
            this.prefix4 = prefix4;
            this.len1234 = len1234;
            this.prefix5 = prefix5;
            this.len12345 = len12345;
            this.data6 = data6;
            this.suffix5 = suffix5;
            this.suffix4 = suffix4;
            this.suffix3 = suffix3;
            this.suffix2 = suffix2;
        }

        @Override
        @SuppressWarnings("unchecked")
        public T get(int index) {
            if (index >= 0 && index < length0) {
                final int io = index - len12345;
                if (io >= 0) {
                    final int i6 = io >>> BITS5;
                    final int i5 = (io >>> BITS4) & MASK;
                    final int i4 = (io >>> BITS3) & MASK;
                    final int i3 = (io >>> BITS2) & MASK;
                    final int i2 = (io >>> BITS) & MASK;
                    final int i1 = io & MASK;
                    if (i6 < data6.length) {
                        return (T) ((Object[]) ((Object[]) ((Object[]) ((Object[]) ((Object[]) data6[i6])[i5])[i4])[i3])[i2])[i1];
                    } else if (i5 < suffix5.length) {
                        return (T) ((Object[]) ((Object[]) ((Object[]) ((Object[]) suffix5[i5])[i4])[i3])[i2])[i1];
                    } else if (i4 < suffix4.length) {
                        return (T) ((Object[]) ((Object[]) ((Object[]) suffix4[i4])[i3])[i2])[i1];
                    } else if (i3 < suffix3.length) {
                        return (T) ((Object[]) ((Object[]) suffix3[i3])[i2])[i1];
                    } else if (i2 < suffix2.length) {
                        return (T) ((Object[]) suffix2[i2])[i1];
                    } else {
                        return (T) suffix1[i1];
                    }
                } else if (index >= len1234) {
                    final int io5 = index - len1234;
                    return (T) ((Object[]) ((Object[]) ((Object[]) ((Object[]) prefix5[io5 >>> BITS4])[(io5 >>> BITS3) & MASK])[(io5 >>> BITS2) & MASK])[(io5 >>> BITS) & MASK])[io5 & MASK];
                } else if (index >= len123) {
                    final int io4 = index - len123;
                    return (T) ((Object[]) ((Object[]) ((Object[]) prefix4[io4 >>> BITS3])[(io4 >>> BITS2) & MASK])[(io4 >>> BITS) & MASK])[io4 & MASK];
                } else if (index >= len12) {
                    final int io3 = index - len12;
                    return (T) ((Object[]) ((Object[]) prefix3[io3 >>> BITS2])[(io3 >>> BITS) & MASK])[io3 & MASK];
                } else if (index >= len1) {
                    final int io2 = index - len1;
                    return (T) ((Object[]) prefix2[io2 >>> BITS])[io2 & MASK];
                } else {
                    return (T) prefix1[index];
                }
            }
            throw ioob("get", index);
        }

        @Override
        RadixVector<T> updated0(int index, Object elem) {
            if (index >= 0 && index < length0) {
                if (index >= len12345) {
                    final int io = index - len12345;
                    final int i6 = io >>> BITS5;
                    final int i5 = (io >>> BITS4) & MASK;
                    final int i4 = (io >>> BITS3) & MASK;
                    final int i3 = (io >>> BITS2) & MASK;
                    final int i2 = (io >>> BITS) & MASK;
                    final int i1 = io & MASK;
                    if (i6 < data6.length) {
                        return new Vector6<>(prefix1, len1, prefix2, len12, prefix3, len123, prefix4, len1234, prefix5, len12345, copyUpdate(data6, i6, i5, i4, i3, i2, i1, elem), suffix5, suffix4, suffix3, suffix2, suffix1, length0);
                    } else if (i5 < suffix5.length) {
                        return new Vector6<>(prefix1, len1, prefix2, len12, prefix3, len123, prefix4, len1234, prefix5, len12345, data6, copyUpdate(suffix5, i5, i4, i3, i2, i1, elem), suffix4, suffix3, suffix2, suffix1, length0);
                    } else if (i4 < suffix4.length) {
                        return new Vector6<>(prefix1, len1, prefix2, len12, prefix3, len123, prefix4, len1234, prefix5, len12345, data6, suffix5, copyUpdate(suffix4, i4, i3, i2, i1, elem), suffix3, suffix2, suffix1, length0);
                    } else if (i3 < suffix3.length) {
                        return new Vector6<>(prefix1, len1, prefix2, len12, prefix3, len123, prefix4, len1234, prefix5, len12345, data6, suffix5, suffix4, copyUpdate(suffix3, i3, i2, i1, elem), suffix2, suffix1, length0);
                    } else if (i2 < suffix2.length) {
                        return new Vector6<>(prefix1, len1, prefix2, len12, prefix3, len123, prefix4, len1234, prefix5, len12345, data6, suffix5, suffix4, suffix3, copyUpdate(suffix2, i2, i1, elem), suffix1, length0);
                    } else {
                        return new Vector6<>(prefix1, len1, prefix2, len12, prefix3, len123, prefix4, len1234, prefix5, len12345, data6, suffix5, suffix4, suffix3, suffix2, copyUpdate(suffix1, i1, elem), length0);
                    }
                } else if (index >= len1234) {
                    final int io = index - len1234;
                    return new Vector6<>(prefix1, len1, prefix2, len12, prefix3, len123, prefix4, len1234, copyUpdate(prefix5, io >>> BITS4, (io >>> BITS3) & MASK, (io >>> BITS2) & MASK, (io >>> BITS) & MASK, io & MASK, elem), len12345, data6, suffix5, suffix4, suffix3, suffix2, suffix1, length0);
                } else if (index >= len123) {
                    final int io = index - len123;
                    return new Vector6<>(prefix1, len1, prefix2, len12, prefix3, len123, copyUpdate(prefix4, io >>> BITS3, (io >>> BITS2) & MASK, (io >>> BITS) & MASK, io & MASK, elem), len1234, prefix5, len12345, data6, suffix5, suffix4, suffix3, suffix2, suffix1, length0);
                } else if (index >= len12) {
                    final int io = index - len12;
                    return new Vector6<>(prefix1, len1, prefix2, len12, copyUpdate(prefix3, io >>> BITS2, (io >>> BITS) & MASK, io & MASK, elem), len123, prefix4, len1234, prefix5, len12345, data6, suffix5, suffix4, suffix3, suffix2, suffix1, length0);
                } else if (index >= len1) {
                    final int io = index - len1;
                    return new Vector6<>(prefix1, len1, copyUpdate(prefix2, io >>> BITS, io & MASK, elem), len12, prefix3, len123, prefix4, len1234, prefix5, len12345, data6, suffix5, suffix4, suffix3, suffix2, suffix1, length0);
                } else {
                    return new Vector6<>(copyUpdate(prefix1, index, elem), len1, prefix2, len12, prefix3, len123, prefix4, len1234, prefix5, len12345, data6, suffix5, suffix4, suffix3, suffix2, suffix1, length0);
                }
            }
            throw ioob("update", index);
        }

        @Override
        RadixVector<T> appended0(Object elem) {
            if (suffix1.length < WIDTH) {
                return new Vector6<>(prefix1, len1, prefix2, len12, prefix3, len123, prefix4, len1234, prefix5, len12345, data6, suffix5, suffix4, suffix3, suffix2, copyAppend(suffix1, elem), length0 + 1);
            } else if (suffix2.length < WIDTH - 1) {
                return new Vector6<>(prefix1, len1, prefix2, len12, prefix3, len123, prefix4, len1234, prefix5, len12345, data6, suffix5, suffix4, suffix3, copyAppend(suffix2, suffix1), wrap1(elem), length0 + 1);
            } else if (suffix3.length < WIDTH - 1) {
                return new Vector6<>(prefix1, len1, prefix2, len12, prefix3, len123, prefix4, len1234, prefix5, len12345, data6, suffix5, suffix4, copyAppend(suffix3, copyAppend(suffix2, suffix1)), EMPTY, wrap1(elem), length0 + 1);
            } else if (suffix4.length < WIDTH - 1) {
                return new Vector6<>(prefix1, len1, prefix2, len12, prefix3, len123, prefix4, len1234, prefix5, len12345, data6, suffix5, copyAppend(suffix4, copyAppend(suffix3, copyAppend(suffix2, suffix1))), EMPTY, EMPTY, wrap1(elem), length0 + 1);
            } else if (suffix5.length < WIDTH - 1) {
                return new Vector6<>(prefix1, len1, prefix2, len12, prefix3, len123, prefix4, len1234, prefix5, len12345, data6, copyAppend(suffix5, copyAppend(suffix4, copyAppend(suffix3, copyAppend(suffix2, suffix1)))), EMPTY, EMPTY, EMPTY, wrap1(elem), length0 + 1);
            } else if (data6.length < LASTWIDTH - 2) {
                return new Vector6<>(prefix1, len1, prefix2, len12, prefix3, len123, prefix4, len1234, prefix5, len12345, copyAppend(data6, copyAppend(suffix5, copyAppend(suffix4, copyAppend(suffix3, copyAppend(suffix2, suffix1))))), EMPTY, EMPTY, EMPTY, EMPTY, wrap1(elem), length0 + 1);
            } else {
                throw new IllegalArgumentException("a Vector cannot hold more than Integer.MAX_VALUE elements");
            }
        }

        @Override
        RadixVector<T> prepended0(Object elem) {
            if (len1 < WIDTH) {
                return new Vector6<>(copyPrepend(elem, prefix1), len1 + 1, prefix2, len12 + 1, prefix3, len123 + 1, prefix4, len1234 + 1, prefix5, len12345 + 1, data6, suffix5, suffix4, suffix3, suffix2, suffix1, length0 + 1);
            } else if (len12 < WIDTH2) {
                return new Vector6<>(wrap1(elem), 1, copyPrepend(prefix1, prefix2), len12 + 1, prefix3, len123 + 1, prefix4, len1234 + 1, prefix5, len12345 + 1, data6, suffix5, suffix4, suffix3, suffix2, suffix1, length0 + 1);
            } else if (len123 < WIDTH3) {
                return new Vector6<>(wrap1(elem), 1, EMPTY, 1, copyPrepend(copyPrepend(prefix1, prefix2), prefix3), len123 + 1, prefix4, len1234 + 1, prefix5, len12345 + 1, data6, suffix5, suffix4, suffix3, suffix2, suffix1, length0 + 1);
            } else if (len1234 < WIDTH4) {
                return new Vector6<>(wrap1(elem), 1, EMPTY, 1, EMPTY, 1, copyPrepend(copyPrepend(copyPrepend(prefix1, prefix2), prefix3), prefix4), len1234 + 1, prefix5, len12345 + 1, data6, suffix5, suffix4, suffix3, suffix2, suffix1, length0 + 1);
            } else if (len12345 < WIDTH5) {
                return new Vector6<>(wrap1(elem), 1, EMPTY, 1, EMPTY, 1, EMPTY, 1, copyPrepend(copyPrepend(copyPrepend(copyPrepend(prefix1, prefix2), prefix3), prefix4), prefix5), len12345 + 1, data6, suffix5, suffix4, suffix3, suffix2, suffix1, length0 + 1);
            } else if (data6.length < LASTWIDTH - 2) {
                return new Vector6<>(wrap1(elem), 1, EMPTY, 1, EMPTY, 1, EMPTY, 1, EMPTY, 1, copyPrepend(copyPrepend(copyPrepend(copyPrepend(copyPrepend(prefix1, prefix2), prefix3), prefix4), prefix5), data6), suffix5, suffix4, suffix3, suffix2, suffix1, length0 + 1);
            } else {
                throw new IllegalArgumentException("a Vector cannot hold more than Integer.MAX_VALUE elements");
            }
        }

        @Override
        public <U extends @Nullable Object> RadixVector<U> map(Function<? super T, ? extends U> f) {
            return new Vector6<>(mapElems1(prefix1, f), len1, mapElems(2, prefix2, f), len12, mapElems(3, prefix3, f), len123,
                mapElems(4, prefix4, f), len1234, mapElems(5, prefix5, f), len12345, mapElems(6, data6, f),
                mapElems(5, suffix5, f), mapElems(4, suffix4, f), mapElems(3, suffix3, f), mapElems(2, suffix2, f), mapElems1(suffix1, f), length0);
        }

        @Override
        RadixVector<T> slice0(int lo, int hi) {
            return sliceWith(new VectorSliceBuilder(lo, hi));
        }

        @Override
        public RadixVector<T> tail() {
            return (len1 > 1)
                   ? new Vector6<>(copyTail(prefix1), len1 - 1, prefix2, len12 - 1, prefix3, len123 - 1, prefix4, len1234 - 1, prefix5, len12345 - 1, data6, suffix5, suffix4, suffix3, suffix2, suffix1, length0 - 1)
                   : slice0(1, length0);
        }

        @Override
        public RadixVector<T> init() {
            return (suffix1.length > 1)
                   ? new Vector6<>(prefix1, len1, prefix2, len12, prefix3, len123, prefix4, len1234, prefix5, len12345, data6, suffix5, suffix4, suffix3, suffix2, copyInit(suffix1), length0 - 1)
                   : slice0(0, length0 - 1);
        }

        @Override
        int vectorSliceCount() {
            return 11;
        }

        @Override
        Object[] vectorSlice(int idx) {
            return switch (idx) {
                case 0 -> prefix1;
                case 1 -> prefix2;
                case 2 -> prefix3;
                case 3 -> prefix4;
                case 4 -> prefix5;
                case 5 -> data6;
                case 6 -> suffix5;
                case 7 -> suffix4;
                case 8 -> suffix3;
                case 9 -> suffix2;
                case 10 -> suffix1;
                default -> throw new IndexOutOfBoundsException(idx);
            };
        }

        @Override
        int vectorSlicePrefixLength(int idx) {
            return switch (idx) {
                case 0 -> len1;
                case 1 -> len12;
                case 2 -> len123;
                case 3 -> len1234;
                case 4 -> len12345;
                case 5 -> len12345 + data6.length * WIDTH5;
                case 6 -> len12345 + data6.length * WIDTH5 + suffix5.length * WIDTH4;
                case 7 -> len12345 + data6.length * WIDTH5 + suffix5.length * WIDTH4 + suffix4.length * WIDTH3;
                case 8 -> len12345 + data6.length * WIDTH5 + suffix5.length * WIDTH4 + suffix4.length * WIDTH3 + suffix3.length * WIDTH2;
                case 9 -> length0 - suffix1.length;
                case 10 -> length0;
                default -> throw new IndexOutOfBoundsException(idx);
            };
        }

        @Override
        RadixVector<T> prependedAll0(Iterable<? extends T> prefix, int k) {
            final Object[] prefix1b = prepend1IfSpace(prefix1, prefix, k);
            if (prefix1b == null) {
                return super.prependedAll0(prefix, k);
            }
            final int diff = prefix1b.length - prefix1.length;
            return new Vector6<>(prefix1b, len1 + diff, prefix2, len12 + diff, prefix3, len123 + diff, prefix4, len1234 + diff, prefix5,
                len12345 + diff, data6, suffix5, suffix4, suffix3, suffix2, suffix1, length0 + diff);
        }

        @Override
        RadixVector<T> appendedAll0(Iterable<? extends T> suffix, int k) {
            final Object[] suffix1b = append1IfSpace(suffix1, suffix, k);
            if (suffix1b == null) {
                return super.appendedAll0(suffix, k);
            }
            return new Vector6<>(prefix1, len1, prefix2, len12, prefix3, len123, prefix4, len1234, prefix5, len12345, data6, suffix5,
                suffix4, suffix3, suffix2, suffix1b, length0 - suffix1.length + suffix1b.length);
        }
    }

    /* walks the indices from last to first; one indexed access per element */
    private static final class ReverseIterator<T extends @Nullable Object> extends AbstractIterator<T> {

        private final RadixVector<T> vector;
        private int index;

        ReverseIterator(RadixVector<T> vector) {
            this.vector = vector;
            this.index = vector.length();
        }

        @Override
        public boolean hasNext() {
            return index > 0;
        }

        @Override
        protected T getNext() {
            return vector.get(--index);
        }
    }
}
