package com.guizmaii.zazr.collection.internal;

import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import org.jspecify.annotations.Nullable;

/**
 * The constants and array helpers of {@link RadixVector}, ported from {@code VectorInline} and {@code VectorStatics} of
 * {@code scala/collection/immutable/Vector.scala} (the Scala 3 standard library, which ships the Scala 2.13.18
 * collections unchanged).
 * <p>
 * Every level of the tree is a plain {@code Object[]}: a leaf holds the elements, an inner array holds the arrays of the
 * level below. Scala types them as {@code Array[Array[...]]}; one runtime class for every level means no reflective
 * array creation and no {@code ArrayStoreException} when arrays of different levels meet in the same slot. The helpers
 * that Scala keeps apart for leaves and inner arrays ({@code copyAppend1}/{@code copyAppend},
 * {@code copyPrepend1}/{@code copyPrepend}) are therefore one method each here.
 * <p>
 * Every helper that copies returns a fresh array and never writes into its argument: the arrays of a vector are shared
 * with every vector derived from it.
 */
final class VectorStatics {

    private VectorStatics() {
    }

    static final int BITS = 5;
    static final int WIDTH = 1 << BITS;
    static final int MASK = WIDTH - 1;
    static final int BITS2 = BITS * 2;
    static final int WIDTH2 = 1 << BITS2;
    static final int BITS3 = BITS * 3;
    static final int WIDTH3 = 1 << BITS3;
    static final int BITS4 = BITS * 4;
    static final int WIDTH4 = 1 << BITS4;
    static final int BITS5 = BITS * 5;
    static final int WIDTH5 = 1 << BITS5;
    /* one extra bit in the top level, to reach Integer.MAX_VALUE elements instead of 2^30 */
    static final int LASTWIDTH = WIDTH << 1;
    /* appendedAll/prependedAll: below 1/32 of the argument's size, this vector's elements are added one by one to it */
    static final int LOG2_CONCAT_FASTER = 5;
    /* appendedAll/prependedAll: aligning the builder on the bigger side pays off once it is 64 elements bigger */
    static final int ALIGN_TO_FASTER = 64;

    /* the empty array of every level (Scala's empty1 .. empty6) */
    static final Object[] EMPTY = new Object[0];

    /* the dimension of the slice at index idx among count slices: 1, 2, .., n, .., 2, 1 */
    static int vectorSliceDim(int count, int idx) {
        final int c = count / 2;
        return c + 1 - Math.abs(idx - c);
    }

    static Object[] copyOrUse(Object[] a, int start, int end) {
        return (start == 0 && end == a.length) ? a : Arrays.copyOfRange(a, start, end);
    }

    static Object[] copyTail(Object[] a) {
        return Arrays.copyOfRange(a, 1, a.length);
    }

    static Object[] copyInit(Object[] a) {
        return Arrays.copyOfRange(a, 0, a.length - 1);
    }

    static Object[] copyIfDifferentSize(Object[] a, int len) {
        return (a.length == len) ? a : Arrays.copyOf(a, len);
    }

    static Object[] wrap1(Object x) {
        return new Object[] { x };
    }

    static Object[] copyUpdate(Object[] a1, int idx1, Object elem) {
        final Object[] a1c = a1.clone();
        a1c[idx1] = elem;
        return a1c;
    }

    static Object[] copyUpdate(Object[] a2, int idx2, int idx1, Object elem) {
        final Object[] a2c = a2.clone();
        a2c[idx2] = copyUpdate((Object[]) a2c[idx2], idx1, elem);
        return a2c;
    }

    static Object[] copyUpdate(Object[] a3, int idx3, int idx2, int idx1, Object elem) {
        final Object[] a3c = a3.clone();
        a3c[idx3] = copyUpdate((Object[]) a3c[idx3], idx2, idx1, elem);
        return a3c;
    }

    static Object[] copyUpdate(Object[] a4, int idx4, int idx3, int idx2, int idx1, Object elem) {
        final Object[] a4c = a4.clone();
        a4c[idx4] = copyUpdate((Object[]) a4c[idx4], idx3, idx2, idx1, elem);
        return a4c;
    }

    static Object[] copyUpdate(Object[] a5, int idx5, int idx4, int idx3, int idx2, int idx1, Object elem) {
        final Object[] a5c = a5.clone();
        a5c[idx5] = copyUpdate((Object[]) a5c[idx5], idx4, idx3, idx2, idx1, elem);
        return a5c;
    }

    static Object[] copyUpdate(Object[] a6, int idx6, int idx5, int idx4, int idx3, int idx2, int idx1, Object elem) {
        final Object[] a6c = a6.clone();
        a6c[idx6] = copyUpdate((Object[]) a6c[idx6], idx5, idx4, idx3, idx2, idx1, elem);
        return a6c;
    }

    static Object[] concatArrays(Object[] a, Object[] b) {
        final Object[] dest = Arrays.copyOf(a, a.length + b.length);
        System.arraycopy(b, 0, dest, a.length, b.length);
        return dest;
    }

    static Object[] copyAppend(Object[] a, Object elem) {
        final int alen = a.length;
        final Object[] ac = new Object[alen + 1];
        System.arraycopy(a, 0, ac, 0, alen);
        ac[alen] = elem;
        return ac;
    }

    static Object[] copyPrepend(Object elem, Object[] a) {
        final Object[] ac = new Object[a.length + 1];
        System.arraycopy(a, 0, ac, 1, a.length);
        ac[0] = elem;
        return ac;
    }

    /* applies f to every element under a, where level 0 means a is a leaf */
    @SuppressWarnings("unchecked")
    static <T extends @Nullable Object> void foreachRec(int level, Object[] a, Consumer<? super T> f) {
        final int len = a.length;
        if (level == 0) {
            for (int i = 0; i < len; i++) {
                f.accept((T) a[i]);
            }
        } else {
            final int l = level - 1;
            for (int i = 0; i < len; i++) {
                foreachRec(l, (Object[]) a[i], f);
            }
        }
    }

    /* maps a leaf; returns a itself when f returns every element unchanged (by identity), so that the leaf stays shared */
    @SuppressWarnings("unchecked")
    static <A extends @Nullable Object, B extends @Nullable Object> Object[] mapElems1(Object[] a, Function<? super A, ? extends B> f) {
        for (int i = 0; i < a.length; i++) {
            final Object v1 = a[i];
            final Object v2 = mapped(f.apply((A) v1));
            if (v1 != v2) {
                return mapElems1Rest(a, f, i, v2);
            }
        }
        return a;
    }

    @SuppressWarnings("unchecked")
    private static <A extends @Nullable Object, B extends @Nullable Object> Object[] mapElems1Rest(Object[] a, Function<? super A, ? extends B> f, int at, Object v2) {
        final Object[] ac = new Object[a.length];
        if (at > 0) {
            System.arraycopy(a, 0, ac, 0, at);
        }
        ac[at] = v2;
        for (int i = at + 1; i < a.length; i++) {
            ac[i] = mapped(f.apply((A) a[i]));
        }
        return ac;
    }

    /* maps an array of dimension n (1 for a leaf); returns a itself when nothing under it changed */
    static <A extends @Nullable Object, B extends @Nullable Object> Object[] mapElems(int n, Object[] a, Function<? super A, ? extends B> f) {
        if (n == 1) {
            return mapElems1(a, f);
        }
        for (int i = 0; i < a.length; i++) {
            final Object[] v1 = (Object[]) a[i];
            final Object[] v2 = mapElems(n - 1, v1, f);
            if (v1 != v2) {
                return mapElemsRest(n, a, f, i, v2);
            }
        }
        return a;
    }

    private static <A extends @Nullable Object, B extends @Nullable Object> Object[] mapElemsRest(int n, Object[] a, Function<? super A, ? extends B> f, int at, Object[] v2) {
        final Object[] ac = new Object[a.length];
        if (at > 0) {
            System.arraycopy(a, 0, ac, 0, at);
        }
        ac[at] = v2;
        for (int i = at + 1; i < a.length; i++) {
            ac[i] = mapElems(n - 1, (Object[]) a[i], f);
        }
        return ac;
    }

    private static Object mapped(@Nullable Object value) {
        return Objects.requireNonNull(value, "Vector.map: element is null");
    }

    /**
     * The number of elements of {@code xs} when it is known without walking them, or -1. Only this package's vector and
     * {@code java.util.Collection} qualify: the size of a zazr {@code Traversable} may cost a walk (a {@code List}) or
     * never end (a {@code Stream}).
     */
    static int knownSize(Iterable<?> xs) {
        if (xs instanceof RadixVector<?> v) {
            return v.length();
        } else if (xs instanceof Collection<?> c) {
            return c.size();
        } else {
            return -1;
        }
    }

    /* copies the first s elements of xs into dest from index start, rejecting null elements */
    static void copyToArray(Iterable<?> xs, int s, Object[] dest, int start) {
        if (xs instanceof RadixVector<?> v) {
            v.copyToArray(dest, start, s);
        } else {
            final java.util.Iterator<?> it = xs.iterator();
            for (int i = 0; i < s; i++) {
                dest[start + i] = Objects.requireNonNull(it.next(), "Vector: element is null");
            }
        }
    }

    /* prefix1 with the k elements of xs prepended, or null when they do not fit in one leaf; k = knownSize(xs) > 0 */
    static Object @Nullable [] prepend1IfSpace(Object[] prefix1, Iterable<?> xs, int k) {
        if (k > 0 && k <= WIDTH - prefix1.length) {
            final Object[] prefix1b = new Object[prefix1.length + k];
            System.arraycopy(prefix1, 0, prefix1b, k, prefix1.length);
            copyToArray(xs, k, prefix1b, 0);
            return prefix1b;
        } else {
            return null;
        }
    }

    /* suffix1 with the k elements of xs appended, or null when they do not fit in one leaf; k = knownSize(xs) > 0 */
    static Object @Nullable [] append1IfSpace(Object[] suffix1, Iterable<?> xs, int k) {
        if (k > 0 && k <= WIDTH - suffix1.length) {
            final Object[] suffix1b = Arrays.copyOf(suffix1, suffix1.length + k);
            copyToArray(xs, k, suffix1b, suffix1.length);
            return suffix1b;
        } else {
            return null;
        }
    }
}
