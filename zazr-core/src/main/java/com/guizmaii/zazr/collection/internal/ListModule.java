package com.guizmaii.zazr.collection.internal;

import com.guizmaii.zazr.Tuple;
import com.guizmaii.zazr.Tuple2;
import com.guizmaii.zazr.collection.List;
import com.guizmaii.zazr.collection.List.Nil;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.function.ToIntFunction;
import org.jspecify.annotations.Nullable;

public interface ListModule {

    interface Combinations {

        static <T extends @Nullable Object> List<List<T>> apply(List<T> elements, int k) {
            return combine(elements, elements.length(), k);
        }

        // Walks the tails of elements (of the given length): each element, followed by every combination of k - 1
        // elements after it. A tail shorter than k gives none, so the walk stops there.
        private static <T extends @Nullable Object> List<List<T>> combine(List<T> elements, int length, int k) {
            if (k == 0) {
                return List.of(List.empty());
            }
            List<List<T>> reversed = List.empty();
            int remaining = length;
            for (List<T> rest = elements; remaining >= k; rest = rest.tail(), remaining--) {
                final T head = rest.head();
                for (List<List<T>> tails = combine(rest.tail(), remaining - 1, k - 1); !tails.isEmpty(); tails = tails.tail()) {
                    reversed = reversed.prepend(tails.head().prepend(head));
                }
            }
            return reversed.reverse();
        }
    }

    interface SplitAt {

        static <T extends @Nullable Object> Tuple2<List<T>, List<T>> splitByPredicateReversed(List<T> source, Predicate<? super T> predicate) {
            Objects.requireNonNull(predicate, "predicate is null");
            List<T> init = Nil.instance();
            List<T> tail = source;
            while (!tail.isEmpty() && !predicate.test(tail.head())) {
                init = init.prepend(tail.head());
                tail = tail.tail();
            }
            return Tuple.of(init, tail);
        }
    }

    /** Slice searches over a cons list: the candidate start positions are the successive tails. */
    interface Slice {

        static <T extends @Nullable Object> int indexOfSlice(List<T> source, Iterable<? extends T> slice, int from) {
            // the slice is read now, even for an empty source: a null element throws as Vector's does
            final List<T> _slice = toList(slice);
            if (source.isEmpty()) {
                return from == 0 && _slice.isEmpty() ? 0 : -1;
            }
            return findFirstSlice(source, _slice, Math.max(from, 0));
        }

        static <T extends @Nullable Object> int lastIndexOfSlice(List<T> source, Iterable<? extends T> slice, int end) {
            if (end < 0) {
                return -1;
            }
            // the slice is read once, whatever its shape; its emptiness is answered by the copy
            final List<T> _slice = toList(slice);
            if (source.isEmpty()) {
                return _slice.isEmpty() ? 0 : -1;
            } else if (_slice.isEmpty()) {
                final int len = source.length();
                return len < end ? len : end;
            }
            int index = 0;
            int result = -1;
            // lengths once, then counted down: List.length() walks the list
            final int sliceLength = _slice.length();
            int remaining = source.length();
            while (remaining >= sliceLength) {
                final int found = findNextSlice(source, _slice, remaining, sliceLength);
                if (found < 0) {
                    return result;
                }
                if (index + found > end) {
                    return result;
                }
                result = index + found;
                index += found + 1;
                remaining -= found + 1;
                source = source.drop(found + 1);
            }
            return result;
        }

        private static <T extends @Nullable Object> int findFirstSlice(List<T> source, List<T> slice, int from) {
            int index = 0;
            final int sliceLength = slice.length();
            // length once, then counted down: List.length() walks the list
            int remaining = source.length();
            while (remaining >= sliceLength) {
                if (index >= from && source.startsWith(slice)) {
                    return index;
                }
                if (source.isEmpty()) {
                    // only reachable for an empty slice with from > length()
                    return -1;
                }
                index++;
                remaining--;
                source = source.tail();
            }
            return -1;
        }

        // the offset of the next occurrence of the slice in source, or -1
        private static <T extends @Nullable Object> int findNextSlice(List<T> source, List<T> slice, int remaining, int sliceLength) {
            int index = 0;
            while (remaining >= sliceLength) {
                if (source.startsWith(slice)) {
                    return index;
                }
                index++;
                remaining--;
                source = source.tail();
            }
            return -1;
        }

        @SuppressWarnings("unchecked")
        private static <T extends @Nullable Object> List<T> toList(Iterable<? extends T> iterable) {
            return (iterable instanceof List) ? (List<T>) iterable : List.ofAll(iterable);
        }
    }

    interface Search {

        static <T extends @Nullable Object> int linearSearch(List<T> list, ToIntFunction<T> comparison) {
            int idx = 0;
            for (T current : list) {
                final int cmp = comparison.applyAsInt(current);
                if (cmp == 0) {
                    return idx;
                } else if (cmp < 0) {
                    return -(idx + 1);
                }
                idx += 1;
            }
            return -(idx + 1);
        }
    }
}
