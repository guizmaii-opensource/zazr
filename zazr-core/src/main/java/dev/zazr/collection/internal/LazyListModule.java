package dev.zazr.collection.internal;

import dev.zazr.Tuple2;
import dev.zazr.collection.LazyList;
import dev.zazr.control.Option;
import java.util.function.Function;
import java.util.function.ToIntFunction;
import org.jspecify.annotations.Nullable;

public interface LazyListModule {

    // whether elements is known to be empty without reading anything, even through an opaque view: an evaluated
    // empty LazyList (or its asJava() view), an empty strict Zazr collection (or its asJava() view), or an empty
    // java.util collection whose elements it holds itself. Any other Collection may be a view over a LazyList
    // (Collections.unmodifiableList(lazyList.asJava()), asJava().reversed()), whose isEmpty would evaluate it.
    static boolean knownIsEmptyWithoutReading(Iterable<?> elements) {
        Object underlying = JavaConverters.underlying(elements);
        Object source = (underlying == null) ? elements : underlying;
        if (source instanceof LazyList<?> list) {
            return LazyCell.knownIsEmpty(list);
        } else if (source instanceof dev.zazr.collection.Traversable<?> traversable) {
            return !(source instanceof java.util.Iterator) && traversable.isEmpty();
        } else if (holdsItsElements(source)) {
            return ((java.util.Collection<?>) source).isEmpty();
        } else {
            return false;
        }
    }

    // a java.util collection that stores its elements, so that asking whether it is empty reads nothing else
    private static boolean holdsItsElements(Object source) {
        Class<?> type = source.getClass();
        return type == java.util.ArrayList.class
                || type == java.util.LinkedList.class
                || type == java.util.ArrayDeque.class
                || type == java.util.HashSet.class
                || type == java.util.LinkedHashSet.class
                || type == java.util.TreeSet.class
                || type.getName().startsWith("java.util.ImmutableCollections$");
    }

    // the elements f produces from state, each computed when the list reaches it
    static <A extends @Nullable Object, S extends @Nullable Object> LazyList<A> unfolded(
            S state, Function<? super S, ? extends Option<? extends Tuple2<? extends A, ? extends S>>> f) {
        return LazyList.defer(() -> {
            Option<? extends Tuple2<? extends A, ? extends S>> step =
                    java.util.Objects.requireNonNull(f.apply(state), "LazyList.unfold: f returned null");
            if (step.isEmpty()) {
                return LazyList.empty();
            }
            Tuple2<? extends A, ? extends S> next = step.get();
            return LazyCell.cons(next._1(), unfolded(next._2(), f));
        });
    }

    // acc, then the result of operation on acc and each element of list in turn, each computed when it is read
    static <T extends @Nullable Object, U extends @Nullable Object> LazyList<U> scanned(
            LazyList<T> list, U acc, java.util.function.BiFunction<? super U, ? super T, ? extends U> operation) {
        return LazyCell.cons(
                acc,
                LazyList.defer(() -> list.isEmpty()
                        ? LazyList.empty()
                        : scanned(list.tail(), operation.apply(acc, list.head()), operation)));
    }

    // the elements of the non-empty list but its last one, each evaluated when the result reaches it
    static <T extends @Nullable Object> LazyList<T> initOf(LazyList<T> list) {
        return LazyList.defer(() -> {
            LazyList<T> tail = list.tail();
            return tail.isEmpty() ? LazyList.empty() : LazyCell.cons(list.head(), initOf(tail));
        });
    }

    // each element of list preceded by element
    static <T extends @Nullable Object> LazyList<T> separated(LazyList<T> list, T element) {
        return LazyList.defer(() -> list.isEmpty()
                ? LazyList.empty()
                : LazyCell.cons(element, LazyCell.cons(list.head(), separated(list.tail(), element))));
    }

    // list with the element at index replaced by what updater computes from it; original is the index of the call
    static <T extends @Nullable Object> LazyList<T> updated(
            LazyList<T> list, int index, int original, Function<? super T, ? extends T> updater) {
        return LazyList.defer(() -> {
            if (list.isEmpty()) {
                throw new IndexOutOfBoundsException(
                        index == original ? "update(" + original + ", e) on Nil" : "update at " + original);
            } else if (index == 0) {
                return LazyCell.cons(updater.apply(list.head()), list.tail());
            } else {
                return LazyCell.cons(list.head(), updated(list.tail(), index - 1, original, updater));
            }
        });
    }

    /** Slice searches over a LazyList: the candidate start positions are the successive tails. */
    interface Slice {

        static <T extends @Nullable Object> int indexOfSlice(
                LazyList<T> source, Iterable<? extends T> slice, int from) {
            if (source.isEmpty()) {
                return from == 0 && Collections.isEmpty(slice) ? 0 : -1;
            }
            return findFirstSlice(source, toLazyList(slice), Math.max(from, 0));
        }

        @SuppressWarnings("Var")
        static <T extends @Nullable Object> int lastIndexOfSlice(
                LazyList<T> source, Iterable<? extends T> slice, int end) {
            if (end < 0) {
                return -1;
            }
            // the slice is read once, whatever its shape, and all of it now: a null element throws as Vector's does
            LazyList<T> _slice = toLazyList(slice);
            _slice.size();
            if (_slice.isEmpty()) {
                // the last position at or before end: the length when this LazyList is shorter; no cell past end - 1 is
                // forced
                int length = 0;
                while (length < end && !source.isEmpty()) {
                    length++;
                    if (length < end) {
                        source = source.tail();
                    }
                }
                return length;
            }
            // every start position up to end, each compared with the slice for as long as it matches: at most the
            // first end + m elements are forced, and the walk stops once the rest is shorter than the slice
            int result = -1;
            for (int index = 0; !source.isEmpty(); index++) {
                int match = matchAt(source, _slice);
                if (match > 0) {
                    result = index;
                } else if (match < 0) {
                    return result;
                }
                if (index == end) {
                    return result;
                }
                source = source.tail();
            }
            return result;
        }

        // 1 if the non-empty source starts with the non-empty slice, 0 if an element differs, -1 if source ends first;
        // the cells of source are forced only as far as the comparison goes
        @SuppressWarnings("Var")
        private static <T extends @Nullable Object> int matchAt(LazyList<T> source, LazyList<T> slice) {
            while (true) {
                if (!java.util.Objects.equals(source.head(), slice.head())) {
                    return 0;
                }
                slice = slice.tail();
                if (slice.isEmpty()) {
                    return 1;
                }
                source = source.tail();
                if (source.isEmpty()) {
                    return -1;
                }
            }
        }

        @SuppressWarnings("Var")
        private static <T extends @Nullable Object> int findFirstSlice(
                LazyList<T> source, LazyList<T> slice, int from) {
            int index = 0;
            // a LazyList may be infinite, so its length is never computed here: only the elements the search reaches
            // are forced
            while (source.nonEmpty()) {
                if (index >= from && source.startsWith(slice)) {
                    return index;
                }
                index++;
                source = source.tail();
            }
            return -1;
        }

        @SuppressWarnings("unchecked")
        private static <T extends @Nullable Object> LazyList<T> toLazyList(Iterable<? extends T> iterable) {
            return (iterable instanceof LazyList) ? (LazyList<T>) iterable : LazyList.ofAll(iterable);
        }
    }

    interface Search {

        static <T extends @Nullable Object> int linearSearch(LazyList<T> stream, ToIntFunction<T> comparison) {
            @SuppressWarnings("Var")
            int idx = 0;
            for (T current : stream) {
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

    /// The elements of a list, then those of the list a mapper computes from the result itself: the mapper is called
    /// once, when a read reaches the end of the list, and not at all when the list is empty.
    final class AppendSelf<T extends @Nullable Object> {

        private final Function<? super LazyList<T>, ? extends LazyList<T>> mapper;
        // the result, set once built: the mapper is called with it
        private @Nullable LazyList<T> self;

        private AppendSelf(Function<? super LazyList<T>, ? extends LazyList<T>> mapper) {
            this.mapper = mapper;
        }

        public static <T extends @Nullable Object> LazyList<T> apply(
                LazyList<T> list, Function<? super LazyList<T>, ? extends LazyList<T>> mapper) {
            AppendSelf<T> appendSelf = new AppendSelf<>(mapper);
            LazyList<T> result = appendSelf.copy(list, true);
            appendSelf.self = result;
            return result;
        }

        private LazyList<T> copy(LazyList<T> list, boolean start) {
            return LazyCell.defer(
                    () -> {
                        if (!list.isEmpty()) {
                            return LazyCell.cons(list.head(), copy(list.tail(), false));
                        } else if (start) {
                            return LazyCell.empty();
                        } else {
                            return java.util.Objects.requireNonNull(
                                    mapper.apply(java.util.Objects.requireNonNull(self)),
                                    "LazyList.appendSelf: mapper returned null");
                        }
                    },
                    "LazyList.appendSelf: null list");
        }
    }

    interface Combinations {

        /// The combinations of `k >= 0` elements, by position, in lexicographic order of the positions, each
        /// computed when the result reaches it.
        static <T extends @Nullable Object> LazyList<LazyList<T>> apply(LazyList<T> elements, int k) {
            if (k == 0) {
                return LazyList.of(LazyList.empty());
            } else if (LazyCell.knownIsEmpty(elements)) {
                return LazyList.empty();
            } else {
                return LazyCell.ofIterator(new CombinationIterator<>(elements, k));
            }
        }
    }

    /// The combinations of k elements of a LazyList, by position, in lexicographic order of the positions: the
    /// index array of [Arrangements.Combinations], where each index is the cell of the chosen element, so that the
    /// elements are read in order and never copied. A cell is read only when a combination needs it, or to find
    /// that the list ends there: the first combination reads the first k cells, and each next one at most one cell
    /// more. The arrays grow as cells are found, so a k far beyond the length costs only the cells there are. Once
    /// the end of the list is found, its length tells in O(1) which cells can no longer move.
    final class CombinationIterator<T extends @Nullable Object> implements java.util.Iterator<LazyList<T>> {

        private static final int UNKNOWN = 0;
        private static final int READY = 1;
        private static final int DONE = 2;
        private static final int INITIAL_CAPACITY = 16;

        private final int k;
        // the cells of the elements of the current combination, from the first one, and their positions in the list;
        // the first k slots are used once a combination is found
        private LazyList<T>[] cells;
        private int[] positions;
        // the length of the list once a read reached its end, -1 before
        private int length = -1;
        // the list, until the first combination is found
        private @Nullable LazyList<T> source;
        private int state = UNKNOWN;

        @SuppressWarnings("unchecked")
        CombinationIterator(LazyList<T> source, int k) {
            this.source = source;
            this.k = k;
            int capacity = Math.min(k, INITIAL_CAPACITY);
            this.cells = (LazyList<T>[]) new LazyList<?>[capacity];
            this.positions = new int[capacity];
        }

        @Override
        public boolean hasNext() {
            if (state == UNKNOWN) {
                state = advance() ? READY : DONE;
            }
            return state == READY;
        }

        @Override
        public LazyList<T> next() {
            if (!hasNext()) {
                throw new java.util.NoSuchElementException("no combination left");
            }
            @SuppressWarnings("Var")
            LazyList<T> combination = LazyList.empty();
            for (int j = k - 1; j >= 0; j--) {
                combination = LazyCell.cons(cells[j].head(), combination);
            }
            // only once it is built: after a VirtualMachineError, the next read gets the same combination
            state = UNKNOWN;
            return combination;
        }

        private boolean advance() {
            LazyList<T> first = source;
            if (first != null) {
                boolean found = layOut(0, first, 0);
                source = null;
                return found;
            }
            // the last cell that can move: moving cell j one step on, the cells after it follow it. Once the length is
            // known, cell j is at its last place when k - 1 - j cells are left after it
            for (int j = k - 1; j >= 0; j--) {
                if (length >= 0 && positions[j] == length - k + j) {
                    continue;
                }
                if (layOut(j, cells[j].tail(), positions[j] + 1)) {
                    return true;
                }
            }
            return false;
        }

        // puts the cells from `from` on at `start`, of position `position`, and the cells after it; false when the
        // list ends before, its length then known. A VirtualMachineError, which a LazyList does not keep, can only
        // be met on a cell evaluated for the first time, past the last cell read. Moving cell k - 1 writes nothing
        // before reading it, and an earlier cell moves only once cell k - 1 could not, after the end was read. So a
        // move that fails leaves the slots as they were, and the next read makes the same move again
        @SuppressWarnings("Var")
        private boolean layOut(int from, LazyList<T> start, int position) {
            LazyList<T> cell = start;
            int p = position;
            for (int j = from; j < k; j++, p++) {
                if (cell.isEmpty()) {
                    length = p;
                    return false;
                }
                if (j == cells.length) {
                    grow();
                }
                cells[j] = cell;
                positions[j] = p;
                cell = cell.tail();
            }
            return true;
        }

        /// The length of the list once a read reached its end, -1 before.
        int knownLength() {
            return length;
        }

        /// The position in the list of the `j`-th cell of the current combination.
        int position(int j) {
            return positions[j];
        }

        // doubles the arrays, up to k slots; both are copied before either is replaced, so that an OutOfMemoryError
        // leaves them as they were for the next read
        private void grow() {
            int capacity = (int) Math.min(k, 2L * cells.length);
            LazyList<T>[] grownCells = java.util.Arrays.copyOf(cells, capacity);
            int[] grownPositions = java.util.Arrays.copyOf(positions, capacity);
            cells = grownCells;
            positions = grownPositions;
        }
    }

    interface Permutations {

        /// The distinct permutations of `elements`, at least two of them, in the order of
        /// [Arrangements.Permutations], each computed when the result reaches it. The elements are read when
        /// this is called.
        static <T extends @Nullable Object> LazyList<LazyList<T>> apply(LazyList<T> elements) {
            return LazyCell.ofIterator(new PermutationIterator<T>(elements.toArray()));
        }
    }

    /// The permutations of the elements of an array, each built as a LazyList of evaluated cells when it is read.
    final class PermutationIterator<T extends @Nullable Object> implements java.util.Iterator<LazyList<T>> {

        private final Object[] source;
        private final Arrangements.Permutations cursor;
        private int state = CombinationIterator.UNKNOWN;

        PermutationIterator(Object[] source) {
            this.source = source;
            this.cursor = new Arrangements.Permutations(source);
        }

        @Override
        public boolean hasNext() {
            if (state == CombinationIterator.UNKNOWN) {
                state = cursor.advance() ? CombinationIterator.READY : CombinationIterator.DONE;
            }
            return state == CombinationIterator.READY;
        }

        @Override
        @SuppressWarnings("unchecked")
        public LazyList<T> next() {
            if (!hasNext()) {
                throw new java.util.NoSuchElementException("no permutation left");
            }
            @SuppressWarnings("Var")
            LazyList<T> permutation = LazyList.empty();
            for (int level = source.length - 1; level >= 0; level--) {
                permutation = LazyCell.cons((T) source[cursor.position(level)], permutation);
            }
            // only once it is built: after a VirtualMachineError, the next read gets the same permutation
            state = CombinationIterator.UNKNOWN;
            return permutation;
        }
    }

    interface Windows {

        // `source` is non-empty and starts a window; the next window starts `step` elements further on, and is produced
        // only when this window is full and followed by at least one element, so that no window repeats the previous
        // one
        static <T extends @Nullable Object> LazyList<LazyList<T>> apply(LazyList<T> source, int size, int step) {
            return LazyList.cons(source.take(size), () -> {
                LazyList<T> next = source.drop(step);
                return next.isEmpty() || source.drop(size).isEmpty() ? LazyList.empty() : apply(next, size, step);
            });
        }
    }

    interface DropRight {

        // works with infinite LazyLists by buffering elements
        static <T extends @Nullable Object> LazyList<T> apply(
                dev.zazr.collection.List<T> front, dev.zazr.collection.List<T> rear, LazyList<T> remaining) {
            if (remaining.isEmpty()) {
                return remaining;
            } else if (front.isEmpty()) {
                return apply(rear.reverse(), dev.zazr.collection.List.empty(), remaining);
            } else {
                return LazyList.cons(
                        front.head(), () -> apply(front.tail(), rear.prepend(remaining.head()), remaining.tail()));
            }
        }
    }

    interface LazyListFactory {

        static <T extends @Nullable Object> LazyList<T> create(java.util.Iterator<? extends T> iterator) {
            return LazyCell.ofIterator(iterator);
        }
    }

    /// Reads a list one cell at a time: [#hasNext()] evaluates the current cell, [#next()] moves to its tail without
    /// evaluating it.
    final class LazyListIterator<T extends @Nullable Object> extends AbstractIterator<T> {

        private LazyList<T> current;

        public LazyListIterator(LazyList<T> list) {
            this.current = list;
        }

        @Override
        public boolean hasNext() {
            return !current.isEmpty();
        }

        @Override
        public T getNext() {
            T head = current.head();
            current = current.tail();
            return head;
        }
    }

    final class FlatMapIterator<T extends @Nullable Object, U extends @Nullable Object> implements Iterator<U> {

        final Function<? super T, ? extends Iterable<? extends U>> mapper;
        final Iterator<? extends T> inputs;
        final String nullResult;
        // set once mapper returned null: its input is consumed, so every later call fails the same way instead of
        // going on with the next input
        boolean failed;
        java.util.Iterator<? extends U> current = java.util.Collections.emptyIterator();

        // for a mapper that never returns null, such as the identity over inputs that reject null
        public FlatMapIterator(
                Iterator<? extends T> inputs, Function<? super T, ? extends Iterable<? extends U>> mapper) {
            this(inputs, mapper, "FlatMapIterator: mapper returned null");
        }

        public FlatMapIterator(
                Iterator<? extends T> inputs,
                Function<? super T, ? extends Iterable<? extends U>> mapper,
                String nullResult) {
            this.inputs = inputs;
            this.nullResult = nullResult;
            this.mapper = mapper;
        }

        @Override
        public boolean hasNext() {
            if (failed) {
                throw new NullPointerException(nullResult);
            }
            @SuppressWarnings("Var")
            boolean currentHasNext;
            while (!(currentHasNext = current.hasNext()) && inputs.hasNext()) {
                Iterable<? extends U> mapped = mapper.apply(inputs.next());
                if (mapped == null) {
                    failed = true;
                    throw new NullPointerException(nullResult);
                }
                current = mapped.iterator();
            }
            return currentHasNext;
        }

        @Override
        public U next() {
            return current.next();
        }
    }
}
