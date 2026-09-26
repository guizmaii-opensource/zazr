package com.guizmaii.zazr.collection.internal;

import com.guizmaii.zazr.collection.Vector;
import java.util.function.IntUnaryOperator;
import org.jspecify.annotations.Nullable;

public interface VectorModule {
    final class Combinations {
        public static <T extends @Nullable Object> Vector<Vector<T>> apply(Vector<T> elements, int k) {
            return (k == 0)
                   ? Vector.of(Vector.empty())
                   : elements.zipWithIndex().flatMap(
                    t -> apply(elements.drop(t._2() + 1), (k - 1)).map((Vector<T> c) -> c.prepend(t._1())));
        }
    }

    /* contiguous-slice search by index; the slice is materialised once, first thing (O(1) when it already is a
     * Vector), so that a one-shot argument is iterated only once */
    final class Slice {

        public static <T extends @Nullable Object> int indexOfSlice(Vector<T> source, Iterable<? extends T> slice, int from) {
            final Vector<? extends T> _slice = Vector.ofAll(slice);
            if (source.isEmpty()) {
                return from == 0 && _slice.isEmpty() ? 0 : -1;
            }
            final int maxIndex = source.size() - _slice.size();
            return findSlice(source, _slice, Math.max(from, 0), maxIndex);
        }

        public static <T extends @Nullable Object> int lastIndexOfSlice(Vector<T> source, Iterable<? extends T> slice, int end) {
            if (end < 0) {
                return -1;
            }
            final Vector<? extends T> _slice = Vector.ofAll(slice);
            if (source.isEmpty()) {
                return _slice.isEmpty() ? 0 : -1;
            } else if (_slice.isEmpty()) {
                final int len = source.size();
                return len < end ? len : end;
            }
            int index = 0;
            int result = -1;
            final int maxIndex = source.size() - _slice.size();
            while (index <= maxIndex) {
                int indexOfSlice = findSlice(source, _slice, index, maxIndex);
                if (indexOfSlice < 0) {
                    return result;
                }
                if (indexOfSlice <= end) {
                    result = indexOfSlice;
                    index = indexOfSlice + 1;
                } else {
                    return result;
                }
            }
            return result;
        }

        private static <T extends @Nullable Object> int findSlice(Vector<T> source, Vector<? extends T> slice, int index, int maxIndex) {
            while (index <= maxIndex) {
                if (source.startsWith(slice, index)) {
                    return index;
                }
                index++;
            }
            return -1;
        }
    }

    /* binary search over the indices; `comparison` compares the element at an index with the searched element */
    final class Search {

        public static int binarySearch(Vector<?> vector, IntUnaryOperator comparison) {
            int low = 0;
            int high = vector.size() - 1;
            while (low <= high) {
                final int mid = (low + high) >>> 1;
                final int cmp = comparison.applyAsInt(mid);
                if (cmp < 0) {
                    low = mid + 1;
                } else if (cmp > 0) {
                    high = mid - 1;
                } else {
                    return mid;
                }
            }
            return -(low + 1);
        }
    }
}
