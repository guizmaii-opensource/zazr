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
            if (source.isEmpty()) {
                return from == 0 && Collections.isEmpty(slice) ? 0 : -1;
            }
            return findFirstSlice(source, toStream(slice), Math.max(from, 0));
        }

        static <T extends @Nullable Object> int lastIndexOfSlice(Stream<T> source, Iterable<? extends T> slice, int end) {
            if (end < 0) {
                return -1;
            }
            // the slice is read once, whatever its shape; its emptiness is answered by the copy
            final Stream<T> _slice = toStream(slice);
            if (source.isEmpty()) {
                return _slice.isEmpty() ? 0 : -1;
            } else if (_slice.isEmpty()) {
                final int len = source.length();
                return len < end ? len : end;
            }
            int index = 0;
            int result = -1;
            // lengths once, then counted down: Stream.length() walks and forces the whole Stream
            final int sliceLength = _slice.length();
            int remaining = source.length();
            while (remaining >= sliceLength) {
                final int found = findNextSlice(source, _slice, remaining, sliceLength);
                if (found < 0 || index + found > end) {
                    return result;
                }
                result = index + found;
                index += found + 1;
                remaining -= found + 1;
                source = source.drop(found + 1);
            }
            return result;
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

        // the offset of the next occurrence of the slice in source, or -1
        private static <T extends @Nullable Object> int findNextSlice(Stream<T> source, Stream<T> slice, int remaining, int sliceLength) {
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
                return tail.isEmpty() ? mapper.apply(self) : appendAll((Cons<T>) tail, mapper);
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
        java.util.Iterator<? extends U> current = java.util.Collections.emptyIterator();

        public FlatMapIterator(Iterator<? extends T> inputs, Function<? super T, ? extends Iterable<? extends U>> mapper) {
            this.inputs = inputs;
            this.mapper = mapper;
        }

        @Override
        public boolean hasNext() {
            boolean currentHasNext;
            while (!(currentHasNext = current.hasNext()) && inputs.hasNext()) {
                current = mapper.apply(inputs.next()).iterator();
            }
            return currentHasNext;
        }

        @Override
        public U next() {
            return current.next();
        }
    }
}
