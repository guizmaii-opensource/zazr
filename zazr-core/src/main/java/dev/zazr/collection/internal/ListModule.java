package dev.zazr.collection.internal;

import dev.zazr.Tuple;
import dev.zazr.Tuple2;
import dev.zazr.collection.List;
import dev.zazr.collection.List.Nil;
import java.util.Objects;
import java.util.function.Function;
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

    /// The operations that return the List they are given when they change nothing, and otherwise share the cells
    /// after the last change: the cells before it are copied once, through a `List.Builder`, and the rest is the
    /// builder's tail. This is how Scala 3's `List.filter` works (`filterCommon` in
    /// `library/src/scala/collection/immutable/List.scala`, the Scala 2.13 collection library that Scala 3 ships
    /// unchanged), without its in-place writes to the cells: a `Cons` is a record and never changes.
    interface Conserve {

        /// The elements of the cells of a List from `from` up to `until`, excluded, added to `builder` in order.
        /// `until` is `from` or one of its tails, reached by walking from `from`; no element is tested.
        @SuppressWarnings("ObjectEquality")
        static <T extends @Nullable Object> void addCells(List.Builder<T> builder, List<T> from, List<T> until) {
            for (List<T> cell = from; cell != until; cell = cell.tail()) {
                builder.add(cell.head());
            }
        }

        /// The elements of `list` that satisfy `predicate`, which is called once per element, in order: `list`
        /// itself when it keeps them all, and the cells after the last rejected element shared.
        @SuppressWarnings("Var")
        static <T extends @Nullable Object> List<T> filter(List<T> list, Predicate<? super T> predicate) {
            List.@Nullable Builder<T> kept = null;
            // the first cell not yet copied: every element from it up to the current cell is kept
            List<T> pending = list;
            for (List<T> cell = list; !cell.isEmpty(); cell = cell.tail()) {
                if (!predicate.test(cell.head())) {
                    if (kept == null) {
                        kept = List.newBuilder();
                    }
                    addCells(kept, pending, cell);
                    pending = cell.tail();
                }
            }
            return (kept == null) ? list : kept.addAll(pending).result();
        }

        /// The results of `mapper` on the elements of `list`, called once per element, in order: `list` itself when
        /// every result is the element itself (`==`), and the cells after the last changed element shared.
        @SuppressWarnings({"Var", "ObjectEquality", "unchecked"})
        static <T extends @Nullable Object, U extends @Nullable Object> List<U> map(
                List<T> list, Function<? super T, ? extends U> mapper) {
            List.@Nullable Builder<U> mapped = null;
            // the first cell not yet copied: every element from it up to the current cell is its own result
            List<T> pending = list;
            for (List<T> cell = list; !cell.isEmpty(); cell = cell.tail()) {
                T element = cell.head();
                U result = mapper.apply(element);
                if (result != element) {
                    if (mapped == null) {
                        mapped = List.newBuilder();
                    }
                    // the elements of the unchanged cells are their own results: they are of type U
                    addCells((List.Builder<T>) (List.Builder<?>) mapped, pending, cell);
                    mapped.add(result);
                    pending = cell.tail();
                }
            }
            return (mapped == null)
                    ? (List<U>) list
                    : mapped.addAll((List<U>) pending).result();
        }

        /// `list` with every element equal to `current` replaced by `replacement`: `list` itself when no element is
        /// equal to `current`, and the cells after the last replaced element shared.
        @SuppressWarnings("Var")
        static <T extends @Nullable Object> List<T> replaceAll(List<T> list, T current, T replacement) {
            List.@Nullable Builder<T> result = null;
            List<T> pending = list;
            for (List<T> cell = list; !cell.isEmpty(); cell = cell.tail()) {
                if (Objects.equals(cell.head(), current)) {
                    if (result == null) {
                        Objects.requireNonNull(replacement, "List: element is null");
                        result = List.newBuilder();
                    }
                    addCells(result, pending, cell);
                    result.add(replacement);
                    pending = cell.tail();
                }
            }
            return (result == null) ? list : result.addAll(pending).result();
        }

        /// The cells of `list` before `cut`, copied, and `cut` itself, one of the tails of `list`: `(list, Nil)`
        /// when `cut` is empty and `(Nil, list)` when `cut` is `list`, with nothing copied.
        @SuppressWarnings("ObjectEquality")
        static <T extends @Nullable Object> Tuple2<List<T>, List<T>> splitAt(List<T> list, List<T> cut) {
            if (cut.isEmpty()) {
                return Tuple.of(list, List.empty());
            } else if (cut == list) {
                return Tuple.of(List.empty(), list);
            } else {
                List.Builder<T> prefix = List.newBuilder();
                addCells(prefix, list, cut);
                return Tuple.of(prefix.result(), cut);
            }
        }
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
