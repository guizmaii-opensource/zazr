package com.guizmaii.zazr.collection.internal;

import com.guizmaii.zazr.collection.List;
import com.guizmaii.zazr.collection.Queue;
import com.guizmaii.zazr.collection.Stream;
import com.guizmaii.zazr.collection.Traversable;
import com.guizmaii.zazr.collection.Vector;
import java.util.AbstractCollection;
import java.util.Collection;
import java.util.Comparator;
import java.util.NoSuchElementException;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import org.jspecify.annotations.Nullable;

/**
 * The unmodifiable JDK collection views of the zazr collections: {@link java.util.Collection} for every
 * {@link Traversable} ({@link CollectionView}) and {@link java.util.List} for the sequences ({@link ListView});
 * the set views are in {@link SetViews}, the map views in {@link MapViews} and the sorted ones in
 * {@link TreeViews}.
 * <p>
 * Every view is O(1) to create and copies nothing. It reads the persistent value it was made from, which never
 * changes, so a view never goes stale. Every mutator throws {@link UnsupportedOperationException}, whether or not
 * the call would change anything, as the collections of {@link java.util.Collections#unmodifiableList} do.
 */
public final class JavaConverters {

    private JavaConverters() {
    }

    /**
     * Implemented by every view. {@link #underlying()} gives the persistent value whose elements the view shows, in
     * the same order, so that an {@code ofAll} factory handed a view can return that value instead of copying it.
     */
    public interface View {

        /**
         * The persistent value this view shows exactly, or {@code null} when the view shows something else (a
         * reversed or descending view, a key range, the keys or the values of a map).
         *
         * @return the persistent value, or {@code null}
         */
        @Nullable Object underlying();
    }

    /**
     * The persistent value {@code object} shows, if it is a {@link View} of one, otherwise {@code null}.
     *
     * @param object any object
     * @return the persistent value behind the view, or {@code null}
     */
    public static @Nullable Object underlying(@Nullable Object object) {
        return object instanceof View view ? view.underlying() : null;
    }

    static UnsupportedOperationException unmodifiable() {
        return new UnsupportedOperationException("unmodifiable view of a persistent collection");
    }

    public static <T extends @Nullable Object> java.util.List<T> asJava(Vector<T> vector) {
        return new VectorListView<>(vector, false);
    }

    public static <T extends @Nullable Object> java.util.List<T> asJava(List<T> list) {
        return new ListListView<>(list, false);
    }

    public static <T extends @Nullable Object> java.util.List<T> asJava(Queue<T> queue) {
        return new QueueListView<>(queue, false);
    }

    public static <T extends @Nullable Object> java.util.List<T> asJava(Stream<T> stream) {
        return new StreamListView<>(stream, false);
    }

    /**
     * The base of the read-only {@link java.util.Collection} views: every mutator throws.
     *
     * @param <T> the element type
     */
    abstract static class UnmodifiableCollection<T extends @Nullable Object> extends AbstractCollection<T> implements View {

        @Override
        public final boolean add(T element) {
            throw unmodifiable();
        }

        @Override
        public final boolean addAll(Collection<? extends T> elements) {
            throw unmodifiable();
        }

        @Override
        public final boolean remove(@Nullable Object element) {
            throw unmodifiable();
        }

        @Override
        public final boolean removeAll(Collection<?> elements) {
            throw unmodifiable();
        }

        @Override
        public final boolean removeIf(Predicate<? super T> filter) {
            throw unmodifiable();
        }

        @Override
        public final boolean retainAll(Collection<?> elements) {
            throw unmodifiable();
        }

        @Override
        public final void clear() {
            throw unmodifiable();
        }
    }

    /**
     * The read-only {@link java.util.Collection} view every {@link Traversable} gives through {@code asJava()}:
     * the delegate's iterator and size, nothing copied.
     *
     * @param <T> the element type
     */
    public static final class CollectionView<T extends @Nullable Object> extends UnmodifiableCollection<T> {

        private final Traversable<T> delegate;

        public CollectionView(Traversable<T> delegate) {
            this.delegate = delegate;
        }

        @Override
        public Object underlying() {
            return delegate;
        }

        @Override
        public java.util.Iterator<T> iterator() {
            return delegate.iterator();
        }

        @Override
        public int size() {
            return delegate.size();
        }

        @Override
        public boolean isEmpty() {
            return delegate.isEmpty();
        }

        @SuppressWarnings({"unchecked", "NullAway"}) // the unchecked cast of a nullable argument; the delegate accepts null
        @Override
        public boolean contains(@Nullable Object element) {
            return delegate.contains((T) element);
        }

        @Override
        public Object[] toArray() {
            return delegate.toArray();
        }

        @Override
        public Spliterator<T> spliterator() {
            return delegate.spliterator();
        }
    }

    /**
     * A {@link java.util.List} view over a persistent sequence, or over the same sequence in reverse order when
     * {@code reversed} is set (the view {@link #reversed()} returns). There is no shared sequence interface to call,
     * so the positional reads go through the abstract hooks below, implemented once per sequence type
     * ({@link VectorListView}, {@link ListListView}, {@link QueueListView}, {@link StreamListView}).
     * <p>
     * Nothing is computed ahead of the operation that needs it, which matters for a {@link Stream}: the iterator,
     * the spliterator, {@code isEmpty}, {@code contains}, {@code indexOf}, {@code equals} and {@code get(i)} force no
     * more cells than they read; {@code size}, {@code lastIndexOf}, {@code hashCode}, {@code getLast} and
     * everything on the reversed view force the whole Stream.
     *
     * @param <T> the element type
     * @param <C> the sequence type
     */
    public abstract static class ListView<T extends @Nullable Object, C extends Traversable<T>> extends UnmodifiableCollection<T> implements java.util.List<T> {

        final C delegate;
        final boolean reversed;

        ListView(C delegate, boolean reversed) {
            this.delegate = delegate;
            this.reversed = reversed;
        }

        // -- the reads a java.util.List needs and Traversable does not declare

        abstract T delegateGet(C delegate, int index);

        abstract T delegateLast(C delegate);

        abstract int delegateIndexOf(C delegate, T element);

        abstract int delegateLastIndexOf(C delegate, T element);

        abstract C delegateSubSequence(C delegate, int beginIndex, int endIndex);

        /** An iterator over the elements from {@code index} on, {@code 0 <= index <= size}. */
        abstract java.util.Iterator<T> delegateIteratorFrom(C delegate, int index);

        abstract java.util.Iterator<T> delegateReverseIterator(C delegate);

        abstract ListView<T, C> view(C delegate, boolean reversed);

        /** Whether the sequence has at least {@code n} elements; forces at most {@code n} cells of a Stream. */
        boolean delegateHasAtLeast(C delegate, int n) {
            return delegate.size() >= n;
        }

        // -- java.util.List

        @Override
        public @Nullable Object underlying() {
            return reversed ? null : delegate;
        }

        @Override
        public int size() {
            return delegate.size();
        }

        @Override
        public boolean isEmpty() {
            return delegate.isEmpty();
        }

        @SuppressWarnings({"unchecked", "NullAway"}) // the unchecked cast of a nullable argument; the delegate accepts null
        @Override
        public boolean contains(@Nullable Object element) {
            return delegate.contains((T) element);
        }

        @Override
        public T get(int index) {
            if (reversed) {
                final int size = delegate.size();
                if (index < 0 || index >= size) {
                    throw new IndexOutOfBoundsException("Index " + index + " out of bounds for length " + size);
                }
                return delegateGet(delegate, size - 1 - index);
            }
            return delegateGet(delegate, index);
        }

        @Override
        public T getFirst() {
            if (delegate.isEmpty()) {
                throw new NoSuchElementException();
            }
            return reversed ? delegateLast(delegate) : delegateGet(delegate, 0);
        }

        @Override
        public T getLast() {
            if (delegate.isEmpty()) {
                throw new NoSuchElementException();
            }
            return reversed ? delegateGet(delegate, 0) : delegateLast(delegate);
        }

        @SuppressWarnings({"unchecked", "NullAway"}) // the unchecked cast of a nullable argument; the delegate accepts null
        @Override
        public int indexOf(@Nullable Object element) {
            if (reversed) {
                final int index = delegateLastIndexOf(delegate, (T) element);
                return index < 0 ? -1 : delegate.size() - 1 - index;
            }
            return delegateIndexOf(delegate, (T) element);
        }

        @SuppressWarnings({"unchecked", "NullAway"}) // the unchecked cast of a nullable argument; the delegate accepts null
        @Override
        public int lastIndexOf(@Nullable Object element) {
            if (reversed) {
                final int index = delegateIndexOf(delegate, (T) element);
                return index < 0 ? -1 : delegate.size() - 1 - index;
            }
            return delegateLastIndexOf(delegate, (T) element);
        }

        @Override
        public java.util.Iterator<T> iterator() {
            return reversed ? delegateReverseIterator(delegate) : delegate.iterator();
        }

        @Override
        public java.util.ListIterator<T> listIterator() {
            return new ListIterator<>(this, 0);
        }

        @Override
        public java.util.ListIterator<T> listIterator(int index) {
            if (index < 0 || (index > 0 && !hasAtLeast(index))) {
                throw new IndexOutOfBoundsException("Index: " + index);
            }
            return new ListIterator<>(this, index);
        }

        @Override
        public Spliterator<T> spliterator() {
            if (reversed) {
                return Spliterators.spliterator(this, Spliterator.ORDERED | Spliterator.IMMUTABLE);
            }
            return delegate.spliterator();
        }

        /**
         * {@inheritDoc}
         * <p>
         * The sub-list is a view over the sub-sequence of the same persistent value, which never changes, so it
         * shows the same elements as this list does in that range for as long as it lives.
         */
        @Override
        public java.util.List<T> subList(int fromIndex, int toIndex) {
            if (fromIndex < 0) {
                throw new IndexOutOfBoundsException("fromIndex = " + fromIndex);
            }
            if (!hasAtLeast(toIndex)) {
                throw new IndexOutOfBoundsException("toIndex = " + toIndex);
            }
            if (fromIndex > toIndex) {
                // the JDK's reversed list views check the range with Objects.checkFromToIndex, ArrayList does not
                if (reversed) {
                    throw new IndexOutOfBoundsException("fromIndex(" + fromIndex + ") > toIndex(" + toIndex + ")");
                }
                throw new IllegalArgumentException("fromIndex(" + fromIndex + ") > toIndex(" + toIndex + ")");
            }
            if (reversed) {
                final int size = delegate.size();
                return view(delegateSubSequence(delegate, size - toIndex, size - fromIndex), true);
            }
            return view(delegateSubSequence(delegate, fromIndex, toIndex), false);
        }

        @Override
        public java.util.List<T> reversed() {
            return view(delegate, !reversed);
        }

        @Override
        public Object[] toArray() {
            final Object[] array = delegate.toArray();
            if (reversed) {
                for (int i = 0, j = array.length - 1; i < j; i++, j--) {
                    final Object tmp = array[i];
                    array[i] = array[j];
                    array[j] = tmp;
                }
            }
            return array;
        }

        // -- mutators

        @Override
        public final void add(int index, T element) {
            throw unmodifiable();
        }

        @Override
        public final boolean addAll(int index, Collection<? extends T> elements) {
            throw unmodifiable();
        }

        @Override
        public final T remove(int index) {
            throw unmodifiable();
        }

        @Override
        public final T set(int index, T element) {
            throw unmodifiable();
        }

        @Override
        public final void replaceAll(UnaryOperator<T> operator) {
            throw unmodifiable();
        }

        @Override
        public final void sort(@Nullable Comparator<? super T> comparator) {
            throw unmodifiable();
        }

        @Override
        public final void addFirst(T element) {
            throw unmodifiable();
        }

        @Override
        public final void addLast(T element) {
            throw unmodifiable();
        }

        @Override
        public final T removeFirst() {
            throw unmodifiable();
        }

        @Override
        public final T removeLast() {
            throw unmodifiable();
        }

        // -- Object

        @Override
        public boolean equals(@Nullable Object o) {
            return o == this || o instanceof java.util.List<?> that && Collections.areEqual(this, that);
        }

        @Override
        public int hashCode() {
            return Collections.hashOrdered(this);
        }

        // -- private helpers

        private boolean hasAtLeast(int n) {
            return reversed ? delegate.size() >= n : delegateHasAtLeast(delegate, n);
        }

        /**
         * A read-only list iterator. Moving forward on a view that is not reversed reads the delegate's own
         * iterator, so a forward walk costs what iterating the sequence costs; moving backward, and every move on a
         * reversed view, is a positional {@code get}.
         */
        private static final class ListIterator<T extends @Nullable Object, C extends Traversable<T>> implements java.util.ListIterator<T> {

            private final ListView<T, C> list;
            private int cursor;
            private java.util.@Nullable Iterator<T> forward;
            private int forwardPosition = -1;

            ListIterator(ListView<T, C> list, int index) {
                this.list = list;
                this.cursor = index;
            }

            @Override
            public boolean hasNext() {
                if (list.reversed) {
                    return cursor < list.size();
                }
                return forward().hasNext();
            }

            @Override
            public T next() {
                if (list.reversed) {
                    if (cursor >= list.size()) {
                        throw new NoSuchElementException();
                    }
                    return list.get(cursor++);
                }
                final java.util.Iterator<T> iterator = forward();
                if (!iterator.hasNext()) {
                    throw new NoSuchElementException();
                }
                final T element = iterator.next();
                forwardPosition = ++cursor;
                return element;
            }

            @Override
            public boolean hasPrevious() {
                return cursor > 0;
            }

            @Override
            public T previous() {
                if (cursor <= 0) {
                    throw new NoSuchElementException();
                }
                final T element = list.get(cursor - 1);
                cursor--;
                return element;
            }

            @Override
            public int nextIndex() {
                return cursor;
            }

            @Override
            public int previousIndex() {
                return cursor - 1;
            }

            @Override
            public void remove() {
                throw unmodifiable();
            }

            @Override
            public void set(T element) {
                throw unmodifiable();
            }

            @Override
            public void add(T element) {
                throw unmodifiable();
            }

            private java.util.Iterator<T> forward() {
                java.util.Iterator<T> iterator = forward;
                if (iterator == null || forwardPosition != cursor) {
                    iterator = list.delegateIteratorFrom(list.delegate, cursor);
                    forward = iterator;
                    forwardPosition = cursor;
                }
                return iterator;
            }
        }
    }

    /** The view over a {@link Vector}: every positional read is effectively O(1). */
    static final class VectorListView<T extends @Nullable Object> extends ListView<T, Vector<T>> {

        VectorListView(Vector<T> delegate, boolean reversed) {
            super(delegate, reversed);
        }

        @Override
        T delegateGet(Vector<T> delegate, int index) { return delegate.get(index); }

        @Override
        T delegateLast(Vector<T> delegate) { return delegate.last(); }

        @Override
        int delegateIndexOf(Vector<T> delegate, T element) { return delegate.indexOf(element); }

        @Override
        int delegateLastIndexOf(Vector<T> delegate, T element) { return delegate.lastIndexOf(element); }

        @Override
        Vector<T> delegateSubSequence(Vector<T> delegate, int beginIndex, int endIndex) { return delegate.subSequence(beginIndex, endIndex); }

        @Override
        java.util.Iterator<T> delegateIteratorFrom(Vector<T> delegate, int index) {
            return index == 0 ? delegate.iterator() : delegate.drop(index).iterator();
        }

        @Override
        java.util.Iterator<T> delegateReverseIterator(Vector<T> delegate) {
            return new java.util.Iterator<>() {
                private int index = delegate.size() - 1;

                @Override
                public boolean hasNext() {
                    return index >= 0;
                }

                @Override
                public T next() {
                    if (index < 0) {
                        throw new NoSuchElementException();
                    }
                    return delegate.get(index--);
                }
            };
        }

        @Override
        ListView<T, Vector<T>> view(Vector<T> delegate, boolean reversed) { return new VectorListView<>(delegate, reversed); }
    }

    /** The view over a {@link List}: a positional read walks the cons cells up to the index. */
    static final class ListListView<T extends @Nullable Object> extends ListView<T, List<T>> {

        ListListView(List<T> delegate, boolean reversed) {
            super(delegate, reversed);
        }

        @Override
        T delegateGet(List<T> delegate, int index) { return delegate.get(index); }

        @Override
        T delegateLast(List<T> delegate) { return delegate.last(); }

        @Override
        int delegateIndexOf(List<T> delegate, T element) { return delegate.indexOf(element); }

        @Override
        int delegateLastIndexOf(List<T> delegate, T element) { return delegate.lastIndexOf(element); }

        @Override
        List<T> delegateSubSequence(List<T> delegate, int beginIndex, int endIndex) { return delegate.subSequence(beginIndex, endIndex); }

        @Override
        java.util.Iterator<T> delegateIteratorFrom(List<T> delegate, int index) { return delegate.drop(index).iterator(); }

        @Override
        java.util.Iterator<T> delegateReverseIterator(List<T> delegate) { return delegate.reverse().iterator(); }

        @Override
        ListView<T, List<T>> view(List<T> delegate, boolean reversed) { return new ListListView<>(delegate, reversed); }
    }

    /** The view over a {@link Queue}. */
    static final class QueueListView<T extends @Nullable Object> extends ListView<T, Queue<T>> {

        QueueListView(Queue<T> delegate, boolean reversed) {
            super(delegate, reversed);
        }

        @Override
        T delegateGet(Queue<T> delegate, int index) { return delegate.get(index); }

        @Override
        T delegateLast(Queue<T> delegate) { return delegate.last(); }

        @Override
        int delegateIndexOf(Queue<T> delegate, T element) { return delegate.indexOf(element); }

        @Override
        int delegateLastIndexOf(Queue<T> delegate, T element) { return delegate.lastIndexOf(element); }

        @Override
        Queue<T> delegateSubSequence(Queue<T> delegate, int beginIndex, int endIndex) { return delegate.subSequence(beginIndex, endIndex); }

        @Override
        java.util.Iterator<T> delegateIteratorFrom(Queue<T> delegate, int index) { return delegate.drop(index).iterator(); }

        @Override
        java.util.Iterator<T> delegateReverseIterator(Queue<T> delegate) { return delegate.reverse().iterator(); }

        @Override
        ListView<T, Queue<T>> view(Queue<T> delegate, boolean reversed) { return new QueueListView<>(delegate, reversed); }
    }

    /**
     * The view over a {@link Stream}. The bound checks of {@code listIterator(int)} and {@code subList} force as many
     * cells as the index they check, never the whole Stream.
     */
    static final class StreamListView<T extends @Nullable Object> extends ListView<T, Stream<T>> {

        StreamListView(Stream<T> delegate, boolean reversed) {
            super(delegate, reversed);
        }

        @Override
        boolean delegateHasAtLeast(Stream<T> delegate, int n) {
            return n <= 0 || !delegate.drop(n - 1).isEmpty();
        }

        @Override
        T delegateGet(Stream<T> delegate, int index) { return delegate.get(index); }

        @Override
        T delegateLast(Stream<T> delegate) { return delegate.last(); }

        @Override
        int delegateIndexOf(Stream<T> delegate, T element) { return delegate.indexOf(element); }

        @Override
        int delegateLastIndexOf(Stream<T> delegate, T element) { return delegate.lastIndexOf(element); }

        @Override
        Stream<T> delegateSubSequence(Stream<T> delegate, int beginIndex, int endIndex) { return delegate.subSequence(beginIndex, endIndex); }

        @Override
        java.util.Iterator<T> delegateIteratorFrom(Stream<T> delegate, int index) { return delegate.drop(index).iterator(); }

        @Override
        java.util.Iterator<T> delegateReverseIterator(Stream<T> delegate) { return delegate.reverse().iterator(); }

        @Override
        ListView<T, Stream<T>> view(Stream<T> delegate, boolean reversed) { return new StreamListView<>(delegate, reversed); }
    }
}
