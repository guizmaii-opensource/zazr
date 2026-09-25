package com.guizmaii.zazr.collection.internal;

import com.guizmaii.zazr.collection.Stream;
import com.guizmaii.zazr.collection.Stream.Cons;
import com.guizmaii.zazr.collection.Stream.Empty;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.ToIntFunction;
import org.jspecify.annotations.Nullable;

public interface StreamModule {

    /** Slice searches over a lazy cons stream: the candidate start positions are the successive tails. */
    interface Slice {

        static <T extends @Nullable Object> int indexOfSlice(Stream<T> source, Iterable<? extends T> slice, int from) {
            // the slice is read once, whatever its shape, and all of it now: a null element throws as Vector's does
            final Stream<T> _slice = toStream(slice);
            _slice.length();
            if (source.isEmpty()) {
                return from == 0 && _slice.isEmpty() ? 0 : -1;
            }
            return findFirstSlice(source, _slice, Math.max(from, 0));
        }

        static <T extends @Nullable Object> int lastIndexOfSlice(Stream<T> source, Iterable<? extends T> slice, int end) {
            if (end < 0) {
                return -1;
            }
            // the slice is read once, whatever its shape, and all of it now: a null element throws as Vector's does
            final Stream<T> _slice = toStream(slice);
            _slice.length();
            if (_slice.isEmpty()) {
                // the last position at or before end: the length when this Stream is shorter; no cell past end - 1 is
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
                final int match = matchAt(source, _slice);
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
        private static <T extends @Nullable Object> int matchAt(Stream<T> source, Stream<T> slice) {
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

        private static <T extends @Nullable Object> int findFirstSlice(Stream<T> source, Stream<T> slice, int from) {
            int index = 0;
            // a Stream may be infinite, so its length is never computed here: only the elements the search reaches
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
        private static <T extends @Nullable Object> Stream<T> toStream(Iterable<? extends T> iterable) {
            return (iterable instanceof Stream) ? (Stream<T>) iterable : Stream.ofAll(iterable);
        }
    }

    interface Search {

        static <T extends @Nullable Object> int linearSearch(Stream<T> stream, ToIntFunction<T> comparison) {
            int idx = 0;
            for (T current : stream) {
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


    final class AppendSelf<T extends @Nullable Object> {

        private final Cons<T> self;

        public AppendSelf(Cons<T> self, Function<? super Stream<T>, ? extends Stream<T>> mapper) {
            this.self = appendAll(self, mapper);
        }

        private Cons<T> appendAll(Cons<T> stream, Function<? super Stream<T>, ? extends Stream<T>> mapper) {
            return (Cons<T>) Stream.cons(stream.head(), () -> {
                final Stream<T> tail = stream.tail();
                return tail.isEmpty() ? java.util.Objects.requireNonNull(mapper.apply(self), "Stream.appendSelf: mapper returned null") : appendAll((Cons<T>) tail, mapper);
            });
        }

        public Cons<T> stream() {
            return self;
        }
    }

    interface Combinations {

        static <T extends @Nullable Object> Stream<Stream<T>> apply(Stream<T> elements, int k) {
            if (k == 0) {
                return Stream.of(Stream.empty());
            } else {
                return elements.zipWithIndex().flatMap(
                        t -> apply(elements.drop(t._2() + 1), (k - 1)).map((Stream<T> c) -> c.prepend(t._1()))
                );
            }
        }
    }

    interface Windows {

        // `source` is non-empty and starts a window; the next window starts `step` elements further on, and is produced
        // only when this window is full and followed by at least one element, so that no window repeats the previous one
        static <T extends @Nullable Object> Stream<Stream<T>> apply(Stream<T> source, int size, int step) {
            return Stream.cons(source.take(size), () -> {
                final Stream<T> next = source.drop(step);
                return next.isEmpty() || source.drop(size).isEmpty() ? Stream.empty() : apply(next, size, step);
            });
        }
    }

    interface DropRight {

        // works with infinite streams by buffering elements
        static <T extends @Nullable Object> Stream<T> apply(com.guizmaii.zazr.collection.List<T> front, com.guizmaii.zazr.collection.List<T> rear, Stream<T> remaining) {
            if (remaining.isEmpty()) {
                return remaining;
            } else if (front.isEmpty()) {
                return apply(rear.reverse(), com.guizmaii.zazr.collection.List.empty(), remaining);
            } else {
                return Stream.cons(front.head(),
                        () -> apply(front.tail(), rear.prepend(remaining.head()), remaining.tail()));
            }
        }
    }

    interface StreamFactory {

        static <T extends @Nullable Object> Stream<T> create(java.util.Iterator<? extends T> iterator) {
            return iterator.hasNext() ? Stream.cons(iterator.next(), () -> create(iterator)) : Empty.instance();
        }
    }

    final class StreamIterator<T extends @Nullable Object> extends AbstractIterator<T> {

        private Supplier<Stream<T>> current;

        public StreamIterator(Cons<T> stream) {
            this.current = () -> stream;
        }

        @Override
        public boolean hasNext() {
            return !current.get().isEmpty();
        }

        @Override
        public T getNext() {
            final Stream<T> stream = current.get();
            // DEV-NOTE: we make the stream even more lazy because the next head must not be evaluated on hasNext()
            current = stream::tail;
            return stream.head();
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

        public FlatMapIterator(Iterator<? extends T> inputs, Function<? super T, ? extends Iterable<? extends U>> mapper, String nullResult) {
            this.inputs = inputs;
            this.nullResult = nullResult;
            this.mapper = mapper;
        }

        @Override
        public boolean hasNext() {
            if (failed) {
                throw new NullPointerException(nullResult);
            }
            boolean currentHasNext;
            while (!(currentHasNext = current.hasNext()) && inputs.hasNext()) {
                final Iterable<? extends U> mapped = mapper.apply(inputs.next());
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
