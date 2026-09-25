package com.guizmaii.zazr.collection.internal;

import java.util.Arrays;
import org.jspecify.annotations.Nullable;

import static com.guizmaii.zazr.collection.internal.VectorStatics.*;

/**
 * Builds the slice {@code [lo, hi)} of a {@link RadixVector}, ported from {@code VectorSliceBuilder} of
 * {@code scala/collection/immutable/Vector.scala} (Scala 3 standard library).
 * <p>
 * The slices of the source vector are passed in order to {@link #consider(int, Object[])}. Whatever the dimension of
 * the source and wherever the cut falls, the kept parts form the highest-dimensional data in the middle and arrays of
 * decreasing dimension at both ends, which {@link #result()} turns into a vector with very little rebalancing. Whole
 * sub-arrays are reused; only the arrays cut through are copied.
 */
final class VectorSliceBuilder {

    private final int lo;
    private final int hi;
    /* prefixes of dimension 1..5 at 0..4 (and the data of dimension 6 at 5), suffixes of dimension n at 11 - n */
    private final Object[] @Nullable [] slices = new Object[11][];
    private int len;
    private int pos;
    private int maxDim;

    VectorSliceBuilder(int lo, int hi) {
        this.lo = lo;
        this.hi = hi;
    }

    private static int prefixIdx(int n) {
        return n - 1;
    }

    private static int suffixIdx(int n) {
        return 11 - n;
    }

    /* the next slice of the source, of dimension n */
    void consider(int n, Object[] a) {
        final int count = a.length * (1 << (BITS * (n - 1)));
        final int lo0 = Math.max(lo - pos, 0);
        final int hi0 = Math.min(hi - pos, count);
        if (hi0 > lo0) {
            addSlice(n, a, lo0, hi0);
            len += (hi0 - lo0);
        }
        pos += count;
    }

    private void addSlice(int n, Object[] a, int lo, int hi) {
        if (n == 1) {
            add(1, copyOrUse(a, lo, hi));
        } else {
            final int bitsN = BITS * (n - 1);
            final int widthN = 1 << bitsN;
            final int loN = lo >>> bitsN;
            final int hiN = hi >>> bitsN;
            final int loRest = lo & (widthN - 1);
            final int hiRest = hi & (widthN - 1);
            if (loRest == 0) {
                if (hiRest == 0) {
                    add(n, copyOrUse(a, loN, hiN));
                } else {
                    if (hiN > loN) {
                        add(n, copyOrUse(a, loN, hiN));
                    }
                    addSlice(n - 1, (Object[]) a[hiN], 0, hiRest);
                }
            } else {
                if (hiN == loN) {
                    addSlice(n - 1, (Object[]) a[loN], loRest, hiRest);
                } else {
                    addSlice(n - 1, (Object[]) a[loN], loRest, widthN);
                    if (hiRest == 0) {
                        if (hiN > loN + 1) {
                            add(n, copyOrUse(a, loN + 1, hiN));
                        }
                    } else {
                        if (hiN > loN + 1) {
                            add(n, copyOrUse(a, loN + 1, hiN));
                        }
                        addSlice(n - 1, (Object[]) a[hiN], 0, hiRest);
                    }
                }
            }
        }
    }

    private void add(int n, Object[] a) {
        final int idx;
        if (n <= maxDim) {
            idx = suffixIdx(n);
        } else {
            maxDim = n;
            idx = prefixIdx(n);
        }
        slices[idx] = a;
    }

    private Object[] slice(int idx) {
        final Object[] s = slices[idx];
        if (s == null) {
            throw new IllegalStateException("VectorSliceBuilder: missing slice " + idx);
        }
        return s;
    }

    <T extends @Nullable Object> RadixVector<T> result() {
        if (len <= WIDTH) {
            if (len == 0) {
                return RadixVector.empty();
            }
            final Object[] prefix1 = slices[prefixIdx(1)];
            final Object[] suffix1 = slices[suffixIdx(1)];
            final Object[] a;
            if (prefix1 != null) {
                a = (suffix1 != null) ? concatArrays(prefix1, suffix1) : prefix1;
            } else if (suffix1 != null) {
                a = suffix1;
            } else {
                final Object[] prefix2 = slices[prefixIdx(2)];
                a = (Object[]) ((prefix2 != null) ? prefix2[0] : slice(suffixIdx(2))[0]);
            }
            return new RadixVector.Vector1<>(a);
        }
        balancePrefix(1);
        balanceSuffix(1);
        int resultDim = maxDim;
        if (resultDim < 6) {
            final Object[] pre = slices[prefixIdx(maxDim)];
            final Object[] suf = slices[suffixIdx(maxDim)];
            if (pre != null && suf != null) {
                // the highest-dimensional data is two slices: concatenate them if they fit in the data array, otherwise
                // add a dimension
                if (pre.length + suf.length <= WIDTH - 2) {
                    slices[prefixIdx(maxDim)] = concatArrays(pre, suf);
                    slices[suffixIdx(maxDim)] = null;
                } else {
                    resultDim += 1;
                }
            } else {
                // a single highest-dimensional slice may hold WIDTH - 1 entries if it came from a prefix or a suffix, but
                // the data holds at most WIDTH - 2: add a dimension then
                final Object[] one = (pre != null) ? pre : slice(suffixIdx(maxDim));
                if (one.length > WIDTH - 2) {
                    resultDim += 1;
                }
            }
        }
        final Object[] prefix1 = slice(prefixIdx(1));
        final Object[] suffix1 = slice(suffixIdx(1));
        final int len1 = prefix1.length;
        switch (resultDim) {
            case 2: {
                final Object[] data2 = dataOr(2);
                return new RadixVector.Vector2<>(prefix1, len1, data2, suffix1, len);
            }
            case 3: {
                final Object[] prefix2 = prefixOr(2);
                final Object[] data3 = dataOr(3);
                final Object[] suffix2 = suffixOr(2);
                final int len12 = len1 + (prefix2.length * WIDTH);
                return new RadixVector.Vector3<>(prefix1, len1, prefix2, len12, data3, suffix2, suffix1, len);
            }
            case 4: {
                final Object[] prefix2 = prefixOr(2);
                final Object[] prefix3 = prefixOr(3);
                final Object[] data4 = dataOr(4);
                final Object[] suffix3 = suffixOr(3);
                final Object[] suffix2 = suffixOr(2);
                final int len12 = len1 + (prefix2.length * WIDTH);
                final int len123 = len12 + (prefix3.length * WIDTH2);
                return new RadixVector.Vector4<>(prefix1, len1, prefix2, len12, prefix3, len123, data4, suffix3, suffix2, suffix1, len);
            }
            case 5: {
                final Object[] prefix2 = prefixOr(2);
                final Object[] prefix3 = prefixOr(3);
                final Object[] prefix4 = prefixOr(4);
                final Object[] data5 = dataOr(5);
                final Object[] suffix4 = suffixOr(4);
                final Object[] suffix3 = suffixOr(3);
                final Object[] suffix2 = suffixOr(2);
                final int len12 = len1 + (prefix2.length * WIDTH);
                final int len123 = len12 + (prefix3.length * WIDTH2);
                final int len1234 = len123 + (prefix4.length * WIDTH3);
                return new RadixVector.Vector5<>(prefix1, len1, prefix2, len12, prefix3, len123, prefix4, len1234, data5, suffix4, suffix3,
                    suffix2, suffix1, len);
            }
            case 6: {
                final Object[] prefix2 = prefixOr(2);
                final Object[] prefix3 = prefixOr(3);
                final Object[] prefix4 = prefixOr(4);
                final Object[] prefix5 = prefixOr(5);
                final Object[] data6 = dataOr(6);
                final Object[] suffix5 = suffixOr(5);
                final Object[] suffix4 = suffixOr(4);
                final Object[] suffix3 = suffixOr(3);
                final Object[] suffix2 = suffixOr(2);
                final int len12 = len1 + (prefix2.length * WIDTH);
                final int len123 = len12 + (prefix3.length * WIDTH2);
                final int len1234 = len123 + (prefix4.length * WIDTH3);
                final int len12345 = len1234 + (prefix5.length * WIDTH4);
                return new RadixVector.Vector6<>(prefix1, len1, prefix2, len12, prefix3, len123, prefix4, len1234, prefix5, len12345, data6,
                    suffix5, suffix4, suffix3, suffix2, suffix1, len);
            }
            default:
                throw new IllegalStateException("VectorSliceBuilder: dimension " + resultDim);
        }
    }

    private Object[] prefixOr(int n) {
        final Object[] p = slices[prefixIdx(n)];
        return (p != null) ? p : EMPTY;
    }

    private Object[] suffixOr(int n) {
        final Object[] s = slices[suffixIdx(n)];
        return (s != null) ? s : EMPTY;
    }

    private Object[] dataOr(int n) {
        final Object[] p = slices[prefixIdx(n)];
        if (p != null) {
            return p;
        }
        final Object[] s = slices[suffixIdx(n)];
        return (s != null) ? s : EMPTY;
    }

    /* makes the prefix of dimension n non-empty, borrowing the first array of the prefix above */
    private void balancePrefix(int n) {
        if (slices[prefixIdx(n)] == null) {
            if (n == maxDim) {
                slices[prefixIdx(n)] = slices[suffixIdx(n)];
                slices[suffixIdx(n)] = null;
            } else {
                balancePrefix(n + 1);
                final Object[] preN1 = slice(prefixIdx(n + 1));
                slices[prefixIdx(n)] = (Object[]) preN1[0];
                if (preN1.length == 1) {
                    slices[prefixIdx(n + 1)] = null;
                    if (maxDim == n + 1 && slices[suffixIdx(n + 1)] == null) {
                        maxDim = n;
                    }
                } else {
                    slices[prefixIdx(n + 1)] = Arrays.copyOfRange(preN1, 1, preN1.length);
                }
            }
        }
    }

    /* makes the suffix of dimension n non-empty, borrowing the last array of the suffix above */
    private void balanceSuffix(int n) {
        if (slices[suffixIdx(n)] == null) {
            if (n == maxDim) {
                slices[suffixIdx(n)] = slices[prefixIdx(n)];
                slices[prefixIdx(n)] = null;
            } else {
                balanceSuffix(n + 1);
                final Object[] sufN1 = slice(suffixIdx(n + 1));
                slices[suffixIdx(n)] = (Object[]) sufN1[sufN1.length - 1];
                if (sufN1.length == 1) {
                    slices[suffixIdx(n + 1)] = null;
                    if (maxDim == n + 1 && slices[prefixIdx(n + 1)] == null) {
                        maxDim = n;
                    }
                } else {
                    slices[suffixIdx(n + 1)] = Arrays.copyOfRange(sufN1, 0, sufN1.length - 1);
                }
            }
        }
    }
}
