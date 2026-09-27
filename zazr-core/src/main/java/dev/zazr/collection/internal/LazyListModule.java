package dev.zazr.collection.internal;

import dev.zazr.Tuple2;
import dev.zazr.collection.LazyList;
import dev.zazr.control.Option;
import java.util.function.Function;
import java.util.function.ToIntFunction;
import org.jspecify.annotations.Nullable;

public interface LazyListModule {

    // whether elements is known to be empty without reading anything: an evaluated empty LazyList, or an empty
    // collection whose emptiness is stored (Scala's knownSize == 0)
    static boolean knownIsEmpty(Iterable<?> elements) {
        if (elements instanceof LazyList<?> list) {
            return LazyCell.knownIsEmpty(list);
        } else if (elements instanceof java.util.Collection<?> collection) {
            return collection.isEmpty();
        } else {
            return elements instanceof dev.zazr.collection.Traversable<?> traversable
                    && !(elements instanceof java.util.Iterator)
                    && traversable.isEmpty();
        }
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

    // the list without its first n > 0 elements, evaluating them now
    @SuppressWarnings("Var")
    static <T extends @Nullable Object> LazyList<T> dropNow(LazyList<T> list, int n) {
        LazyList<T> rest = list;
        for (int i = 0; i < n && !rest.isEmpty(); i++) {
            rest = rest.tail();
        }
        return rest;
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

    /** Slice searches over a lazy cons stream: the candidate start positions are the successive tails. */
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

        static <T extends @Nullable Object> LazyList<LazyList<T>> apply(LazyList<T> elements, int k) {
            if (k == 0) {
                return LazyList.of(LazyList.empty());
            } else {
                return elements.zipWithIndex()
                        .flatMap(t ->
                                apply(elements.drop(t._2() + 1), (k - 1)).map((LazyList<T> c) -> c.prepend(t._1())));
            }
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

        // works with infinite streams by buffering elements
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
