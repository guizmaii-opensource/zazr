package dev.zazr.collection.internal;

import dev.zazr.Tuple;
import dev.zazr.Tuple2;
import dev.zazr.collection.List;
import dev.zazr.collection.List.Nil;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.function.ToIntFunction;
import org.jspecify.annotations.Nullable;

public interface ListModule {

    interface Combinations {

        /// The combinations of `k >= 0` elements, by position, in lexicographic order of the positions.
        static <T extends @Nullable Object> List<List<T>> apply(List<T> elements, int k) {
            if (k == 0) {
                return List.of(List.empty());
            }
            Object[] source = elements.toArray();
            int n = source.length;
            if (k > n) {
                return List.empty();
            }
            Arrangements.Combinations cursor = new Arrangements.Combinations(n, k);
            List.Builder<List<T>> result = List.newBuilder();
            while (cursor.advance()) {
                @SuppressWarnings("Var")
                List<T> combination = List.empty();
                for (int j = k - 1; j >= 0; j--) {
                    combination = combination.prepend(elementAt(source, cursor.index(j)));
                }
                result.add(combination);
            }
            return result.result();
        }
    }

    interface Permutations {

        /// The distinct permutations of at least two elements, in the order of [Arrangements.Permutations].
        static <T extends @Nullable Object> List<List<T>> apply(List<T> elements) {
            Object[] source = elements.toArray();
            int n = source.length;
            Arrangements.Permutations cursor = new Arrangements.Permutations(source);
            List.Builder<List<T>> result = List.newBuilder();
            while (cursor.advance()) {
                @SuppressWarnings("Var")
                List<T> permutation = List.empty();
                for (int level = n - 1; level >= 0; level--) {
                    permutation = permutation.prepend(elementAt(source, cursor.position(level)));
                }
                result.add(permutation);
            }
            return result.result();
        }
    }

    interface Rotate {

        /// `elements`, of `length` elements, rotated `0 < k < length` positions to the right: its last k
        /// elements, then the others. Every cell is new, since the last one of each part links to the other
        /// part: the elements are read into an array once, and the cells built from the end.
        static <T extends @Nullable Object> List<T> right(List<T> elements, int length, int k) {
            Object[] source = new Object[length];
            @SuppressWarnings("Var")
            List<T> cursor = elements;
            for (int i = 0; i < length; i++) {
                source[i] = cursor.head();
                cursor = cursor.tail();
            }
            return fromArrayRotated(source, k);
        }

        /// The elements of `source` rotated `0 < k < source.length` positions to the right, as a List.
        @SuppressWarnings("Var")
        static <T extends @Nullable Object> List<T> fromArrayRotated(Object[] source, int k) {
            int split = source.length - k;
            List<T> result = List.empty();
            for (int i = split - 1; i >= 0; i--) {
                result = result.prepend(elementAt(source, i));
            }
            for (int i = source.length - 1; i >= split; i--) {
                result = result.prepend(elementAt(source, i));
            }
            return result;
        }
    }

    // the element at `index` of an array of elements of type T
    @SuppressWarnings("unchecked")
    private static <T extends @Nullable Object> T elementAt(Object[] source, int index) {
        return (T) source[index];
    }

    interface SplitAt {

        static <T extends @Nullable Object> Tuple2<List<T>, List<T>> splitByPredicateReversed(
                List<T> source, Predicate<? super T> predicate) {
            Objects.requireNonNull(predicate, "predicate is null");
            @SuppressWarnings("Var")
            List<T> init = Nil.instance();
            @SuppressWarnings("Var")
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
            if (source.isEmpty()) {
                return from == 0 && Collections.isEmpty(slice) ? 0 : -1;
            }
            return findFirstSlice(source, toList(slice), Math.max(from, 0));
        }

        @SuppressWarnings("Var")
        static <T extends @Nullable Object> int lastIndexOfSlice(List<T> source, Iterable<? extends T> slice, int end) {
            if (end < 0) {
                return -1;
            }
            // the slice is read once, whatever its shape; its emptiness is answered by the copy
            List<T> _slice = toList(slice);
            if (source.isEmpty()) {
                return _slice.isEmpty() ? 0 : -1;
            } else if (_slice.isEmpty()) {
                int len = source.size();
                return len < end ? len : end;
            }
            int index = 0;
            int result = -1;
            // lengths once, then counted down: List.size() walks the list
            int sliceLength = _slice.size();
            int remaining = source.size();
            while (remaining >= sliceLength) {
                int found = findNextSlice(source, _slice, remaining, sliceLength);
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

        @SuppressWarnings("Var")
        private static <T extends @Nullable Object> int findFirstSlice(List<T> source, List<T> slice, int from) {
            int index = 0;
            int sliceLength = slice.size();
            // length once, then counted down: List.size() walks the list
            int remaining = source.size();
            while (remaining >= sliceLength) {
                if (index >= from && source.startsWith(slice)) {
                    return index;
                }
                if (source.isEmpty()) {
                    // only reachable for an empty slice with from > size()
                    return -1;
                }
                index++;
                remaining--;
                source = source.tail();
            }
            return -1;
        }

        // the offset of the next occurrence of the slice in source, or -1
        @SuppressWarnings("Var")
        private static <T extends @Nullable Object> int findNextSlice(
                List<T> source, List<T> slice, int remaining, int sliceLength) {
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
            @SuppressWarnings("Var")
            int idx = 0;
            for (T current : list) {
                int cmp = comparison.applyAsInt(current);
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
