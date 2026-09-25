package com.guizmaii.zazr.collection.internal;

import com.guizmaii.zazr.collection.List;
import com.guizmaii.zazr.collection.Queue;
import com.guizmaii.zazr.collection.Stream;
import com.guizmaii.zazr.collection.Traversable;
import com.guizmaii.zazr.collection.Vector;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;
import org.jspecify.annotations.Nullable;

/**
 * THIS CLASS IS INTENDED TO BE USED INTERNALLY ONLY!
 * <p>
 * This helper class provides methods that return {@link java.util.List} views on the zazr sequences ({@link Vector},
 * {@link List}, {@link Queue} and {@link Stream}). The view creation and back conversion take O(1).
 *
 * @author Daniel Dietrich
 */
public class JavaConverters {

    private JavaConverters() {
    }

    public static <T extends @Nullable Object> ListView<T, Vector<T>> asJava(Vector<T> vector, ChangePolicy changePolicy) {
        return new VectorListView<>(vector, changePolicy.isMutable());
    }

    public static <T extends @Nullable Object> ListView<T, List<T>> asJava(List<T> list, ChangePolicy changePolicy) {
        return new ListListView<>(list, changePolicy.isMutable());
    }

    public static <T extends @Nullable Object> ListView<T, Queue<T>> asJava(Queue<T> queue, ChangePolicy changePolicy) {
        return new QueueListView<>(queue, changePolicy.isMutable());
    }

    public static <T extends @Nullable Object> ListView<T, Stream<T>> asJava(Stream<T> stream, ChangePolicy changePolicy) {
        return new StreamListView<>(stream, changePolicy.isMutable());
    }

    public enum ChangePolicy {

        IMMUTABLE, MUTABLE;

        boolean isMutable() {
            return this == MUTABLE;
        }
    }

    // -- private view implementations

    /**
     * The read-only {@link java.util.Collection} view every {@link Traversable} gives through {@code asJava()}:
     * the delegate's iterator and size, nothing copied, every mutator throwing {@link UnsupportedOperationException}
     * whether or not it would change anything, as {@link java.util.Collections#unmodifiableCollection} does.
     *
     * @param <T> the element type
     */
    public static final class CollectionView<T extends @Nullable Object> extends AbstractCollection<T> {

        private final Traversable<T> delegate;

        public CollectionView(Traversable<T> delegate) {
            this.delegate = delegate;
        }

        public Traversable<T> getDelegate() {
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

        @Override
        public Object[] toArray() {
            return delegate.toArray();
        }

        @Override
        public java.util.stream.Stream<T> stream() {
            return delegate.stream();
        }

        @Override
        public boolean add(T element) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean addAll(Collection<? extends T> elements) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean remove(@Nullable Object element) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean removeAll(Collection<?> elements) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean removeIf(java.util.function.Predicate<? super T> filter) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean retainAll(Collection<?> elements) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void clear() {
            throw new UnsupportedOperationException();
        }
    }

    /**
     * Encapsulates the access to delegate and performs mutability checks.
     *
     * @param <C> The zazr collection type
     */
    private static abstract class HasDelegate<C extends Traversable<?>> {

        private C delegate;
        private final boolean mutable;

        HasDelegate(C delegate, boolean mutable) {
            this.delegate = delegate;
            this.mutable = mutable;
        }

        protected boolean isMutable() {
            return mutable;
        }

        public C getDelegate() {
            return delegate;
        }

        protected boolean setDelegateAndCheckChanged(Supplier<C> delegate) {
            ensureMutable();
            final C previousDelegate = this.delegate;
            final C newDelegate = delegate.get();
            final boolean changed = newDelegate.size() != previousDelegate.size();
            if (changed) {
                this.delegate = newDelegate;
            }
            return changed;
        }

        protected void setDelegate(Supplier<C> newDelegate) {
            ensureMutable();
            this.delegate = newDelegate.get();
        }

        protected void ensureMutable() {
            if (!mutable) {
                throw new UnsupportedOperationException();
            }
        }
    }

    /**
     * A {@link java.util.List} view over a persistent sequence. There is no shared sequence interface to call (design
     * 3.7), so everything positional goes through the abstract hooks below, implemented once per delegate type:
     * one per sequence type ({@link VectorListView}, {@link ListListView}, {@link QueueListView},
     * {@link StreamListView}).
     *
     * @param <T> the element type
     * @param <C> the delegate type
     */
    public static abstract class ListView<T extends @Nullable Object, C extends Traversable<T>> extends HasDelegate<C> implements java.util.List<T> {

        ListView(C delegate, boolean mutable) {
            super(delegate, mutable);
        }

        // -- the delegate operations a java.util.List needs and Traversable does not declare

        abstract C delegateAppend(C delegate, T element);

        abstract C delegateInsert(C delegate, int index, T element);

        abstract C delegateAppendAll(C delegate, Iterable<? extends T> elements);

        abstract C delegateInsertAll(C delegate, int index, Iterable<? extends T> elements);

        abstract C delegateTake(C delegate, int n);

        abstract T delegateGet(C delegate, int index);

        abstract int delegateIndexOf(C delegate, T element);

        abstract int delegateLastIndexOf(C delegate, T element);

        abstract C delegateRemoveAt(C delegate, int index);

        abstract C delegateRemove(C delegate, T element);

        abstract C delegateRemoveAll(C delegate, Iterable<? extends T> elements);

        abstract C delegateRetainAll(C delegate, Iterable<? extends T> elements);

        abstract C delegateUpdate(C delegate, int index, T element);

        abstract C delegateSorted(C delegate, Comparator<? super T> comparator);

        abstract C delegateSubSequence(C delegate, int beginIndex, int endIndex);

        abstract ListView<T, C> view(C delegate, boolean mutable);

        // -- java.util.List

        @Override
        public boolean add(T element) {
            setDelegate(() -> delegateAppend(getDelegate(), element));
            return true;
        }

        @Override
        public void add(int index, T element) {
            setDelegate(() -> delegateInsert(getDelegate(), index, element));
        }

        @Override
        public boolean addAll(Collection<? extends T> collection) {
            Objects.requireNonNull(collection, "collection is null");
            return setDelegateAndCheckChanged(() -> delegateAppendAll(getDelegate(), collection));
        }

        @Override
        public boolean addAll(int index, Collection<? extends T> collection) {
            Objects.requireNonNull(collection, "collection is null");
            return setDelegateAndCheckChanged(() -> delegateInsertAll(getDelegate(), index, collection));
        }

        @Override
        public void clear() {
            // DEV-NOTE: acts like Java: works for empty immutable collections
            if (isEmpty()) {
                return;
            }
            setDelegate(() -> delegateTake(getDelegate(), 0));
        }

        @Override
        public boolean contains(Object obj) {
            @SuppressWarnings("unchecked") final T that = (T) obj;
            return getDelegate().contains(that);
        }

        @Override
        public boolean containsAll(Collection<?> collection) {
            Objects.requireNonNull(collection, "collection is null");
            @SuppressWarnings("unchecked") final Collection<T> that = (Collection<T>) collection;
            return getDelegate().containsAll(that);
        }

        @Override
        public T get(int index) {
            return delegateGet(getDelegate(), index);
        }

        @Override
        public int indexOf(Object obj) {
            @SuppressWarnings("unchecked") final T that = (T) obj;
            return delegateIndexOf(getDelegate(), that);
        }

        @Override
        public boolean isEmpty() {
            return getDelegate().isEmpty();
        }

        @Override
        public java.util.Iterator<T> iterator() {
            return new Iterator<>(this);
        }

        @Override
        public int lastIndexOf(Object obj) {
            @SuppressWarnings("unchecked") final T that = (T) obj;
            return delegateLastIndexOf(getDelegate(), that);
        }

        @Override
        public java.util.ListIterator<T> listIterator() {
            return new ListIterator<>(this, 0);
        }

        @Override
        public java.util.ListIterator<T> listIterator(int index) {
            return new ListIterator<>(this, index);
        }

        @Override
        public T remove(int index) {
            return setDelegateAndGetPreviousElement(index, () -> delegateRemoveAt(getDelegate(), index));
        }

        @Override
        public boolean remove(Object obj) {
            @SuppressWarnings("unchecked") final T that = (T) obj;
            return setDelegateAndCheckChanged(() -> delegateRemove(getDelegate(), that));
        }

        @Override
        public boolean removeAll(Collection<?> collection) {
            Objects.requireNonNull(collection, "collection is null");
            @SuppressWarnings("unchecked") final Collection<T> that = (Collection<T>) collection;
            return setDelegateAndCheckChanged(() -> delegateRemoveAll(getDelegate(), that));
        }

        @Override
        public boolean removeIf(java.util.function.Predicate<? super T> filter) {
            Objects.requireNonNull(filter, "filter is null");
            ensureMutable(); // an immutable view refuses the call even when no element matches
            return java.util.List.super.removeIf(filter);
        }

        @Override
        public boolean retainAll(Collection<?> collection) {
            Objects.requireNonNull(collection, "collection is null");
            @SuppressWarnings("unchecked") final Collection<T> that = (Collection<T>) collection;
            return setDelegateAndCheckChanged(() -> delegateRetainAll(getDelegate(), that));
        }

        @Override
        public T set(int index, T element) {
            return setDelegateAndGetPreviousElement(index, () -> delegateUpdate(getDelegate(), index, element));
        }

        @Override
        public int size() {
            return getDelegate().size();
        }

        @Override
        public void sort(Comparator<? super T> comparator) {
            Objects.requireNonNull(comparator, "comparator is null");
            if (isEmpty()) {
                return;
            }
            setDelegate(() -> delegateSorted(getDelegate(), comparator));
        }

        /**
         * {@inheritDoc}
         * <p>
         * Unlike the general {@link java.util.List#subList(int, int)} contract, the returned list is
         * <strong>not</strong> backed by this list: it is an independent view over a snapshot of the
         * requested range, so changes made through either list are not reflected in the other.
         * In particular, {@code list.subList(from, to).clear()} does not remove elements from this list.
         */
        @Override
        public java.util.List<T> subList(int fromIndex, int toIndex) {
            return view(delegateSubSequence(getDelegate(), fromIndex, toIndex), isMutable());
        }

        @Override
        public Object [] toArray() {
            return getDelegate().toArray();
        }

        // Collection.toArray(T[]) mandates writing null just past the last element, even when
        // the caller's array has non-null components.
        @SuppressWarnings({"unchecked", "NullAway"})
        @Override
        public <U extends @Nullable Object> U [] toArray(U [] array) {
            Objects.requireNonNull(array, "array is null");
            final U[] target;
            final C delegate = getDelegate();
            final int length = delegate.size();
            if (array.length < length) {
                final Class<? extends Object[]> newType = array.getClass();
                target = (newType == Object[].class)
                         ? (U[]) new Object[length]
                         : (U[]) java.lang.reflect.Array.newInstance(newType.getComponentType(), length);
            } else {
                if (array.length > length) {
                    array[length] = null;
                }
                target = array;
            }
            final java.util.Iterator<T> iter = delegate.iterator();
            for (int i = 0; i < length; i++) {
                target[i] = (U) iter.next();
            }
            return target;
        }

        // -- Object.*

        @Override
        public boolean equals(@Nullable Object o) {
            return o == this || o instanceof java.util.List && Collections.areEqual(getDelegate(), (java.util.List<?>) o);
        }

        @Override
        public int hashCode() {
            // DEV-NOTE: Ensures that hashCode calculation is stable, regardless of delegate.hashCode()
            return Collections.hashOrdered(getDelegate());
        }

        @Override
        public String toString() {
            return getDelegate().mkString("[", ", ", "]");
        }

        // -- private helpers

        private T setDelegateAndGetPreviousElement(int index, Supplier<C> delegate) {
            ensureMutable();
            final T previousElement = get(index);
            setDelegate(delegate);
            return previousElement;
        }

        private static class Iterator<T extends @Nullable Object, C extends Traversable<T>> implements java.util.Iterator<T> {

            ListView<T, C> list;
            int expectedSize;
            int nextIndex = 0;
            int lastIndex = -1;

            Iterator(ListView<T, C> list) {
                this.list = list;
                expectedSize = list.size();
            }

            @Override
            public boolean hasNext() {
                return nextIndex != list.size();
            }

            @Override
            public T next() {
                checkForComodification();
                if (nextIndex >= list.size()) {
                    throw new NoSuchElementException();
                }
                try {
                    return list.get(lastIndex = nextIndex++);
                } catch (IndexOutOfBoundsException x) {
                    throw new ConcurrentModificationException();
                }
            }

            @Override
            public void remove() {
                list.ensureMutable();
                if (lastIndex < 0) {
                    throw new IllegalStateException();
                }
                checkForComodification();
                try {
                    list.remove(nextIndex = lastIndex);
                    lastIndex = -1;
                    expectedSize = list.size();
                } catch (IndexOutOfBoundsException x) {
                    throw new ConcurrentModificationException();
                }
            }

            @Override
            public void forEachRemaining(Consumer<? super T> consumer) {
                Objects.requireNonNull(consumer, "consumer is  null");
                checkForComodification();
                if (nextIndex >= list.size()) {
                    return;
                }
                int index = nextIndex;
                // DEV-NOTE: intentionally not using hasNext() and next() in order not to modify internal state
                while (expectedSize == list.size() && index < expectedSize) {
                    consumer.accept(list.get(index++));
                }
                nextIndex = index;
                lastIndex = index - 1;
                checkForComodification();
            }

            final void checkForComodification() {
                if (expectedSize != list.size()) {
                    throw new ConcurrentModificationException();
                }
            }
        }

        private static class ListIterator<T extends @Nullable Object, C extends Traversable<T>> extends ListView.Iterator<T, C> implements java.util.ListIterator<T> {

            ListIterator(ListView<T, C> list, int index) {
                super(list);
                if (index < 0 || index > list.size()) {
                    throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + list.size());
                }
                this.nextIndex = index;
            }

            @Override
            public boolean hasPrevious() {
                return nextIndex != 0;
            }

            @Override
            public int nextIndex() {
                return nextIndex;
            }

            @Override
            public int previousIndex() {
                return nextIndex - 1;
            }

            @Override
            public T previous() {
                checkForComodification();
                final int index = nextIndex - 1;
                if (index < 0) {
                    throw new NoSuchElementException();
                }
                if (index >= list.size()) {
                    throw new ConcurrentModificationException();
                }
                try {
                    final T element = list.get(index);
                    // DEV-NOTE: intentionally updating indices _after_ reading the element. This makes a difference in case of a concurrent modification.
                    lastIndex = nextIndex = index;
                    return element;
                } catch (IndexOutOfBoundsException x) {
                    throw new ConcurrentModificationException();
                }
            }

            @Override
            public void set(T element) {
                list.ensureMutable();
                if (lastIndex < 0) {
                    throw new IllegalStateException();
                }
                checkForComodification();
                try {
                    list.set(lastIndex, element);
                } catch (IndexOutOfBoundsException x) {
                    throw new ConcurrentModificationException();
                }
            }

            @Override
            public void add(T element) {
                list.ensureMutable();
                checkForComodification();
                try {
                    final int index = nextIndex;
                    list.add(index, element);
                    // DEV-NOTE: intentionally increasing nextIndex _after_ adding the element. This makes a difference in case of a concurrent modification.
                    nextIndex = index + 1;
                    lastIndex = -1;
                    expectedSize = list.size();
                } catch (IndexOutOfBoundsException ex) {
                    throw new ConcurrentModificationException();
                }
            }
        }
    }

    /** The view over a {@link Vector}: every hook is the Vector method of the same name. */
    static final class VectorListView<T extends @Nullable Object> extends ListView<T, Vector<T>> {

        VectorListView(Vector<T> delegate, boolean mutable) {
            super(delegate, mutable);
        }

        @Override
        Vector<T> delegateAppend(Vector<T> delegate, T element) { return delegate.append(element); }

        @Override
        Vector<T> delegateInsert(Vector<T> delegate, int index, T element) { return delegate.insert(index, element); }

        @Override
        Vector<T> delegateAppendAll(Vector<T> delegate, Iterable<? extends T> elements) { return delegate.appendAll(elements); }

        @Override
        Vector<T> delegateInsertAll(Vector<T> delegate, int index, Iterable<? extends T> elements) { return delegate.insertAll(index, elements); }

        @Override
        Vector<T> delegateTake(Vector<T> delegate, int n) { return delegate.take(n); }

        @Override
        T delegateGet(Vector<T> delegate, int index) { return delegate.get(index); }

        @Override
        int delegateIndexOf(Vector<T> delegate, T element) { return delegate.indexOf(element); }

        @Override
        int delegateLastIndexOf(Vector<T> delegate, T element) { return delegate.lastIndexOf(element); }

        @Override
        Vector<T> delegateRemoveAt(Vector<T> delegate, int index) { return delegate.removeAt(index); }

        @Override
        Vector<T> delegateRemove(Vector<T> delegate, T element) { return delegate.remove(element); }

        @Override
        Vector<T> delegateRemoveAll(Vector<T> delegate, Iterable<? extends T> elements) { return delegate.removeAll(elements); }

        @Override
        Vector<T> delegateRetainAll(Vector<T> delegate, Iterable<? extends T> elements) { return delegate.retainAll(elements); }

        @Override
        Vector<T> delegateUpdate(Vector<T> delegate, int index, T element) { return delegate.update(index, element); }

        @Override
        Vector<T> delegateSorted(Vector<T> delegate, Comparator<? super T> comparator) { return delegate.sorted(comparator); }

        @Override
        Vector<T> delegateSubSequence(Vector<T> delegate, int beginIndex, int endIndex) { return delegate.subSequence(beginIndex, endIndex); }

        @Override
        ListView<T, Vector<T>> view(Vector<T> delegate, boolean mutable) { return new VectorListView<>(delegate, mutable); }
    }

    /** The view over a {@link List}: every hook is the List method of the same name. */
    static final class ListListView<T extends @Nullable Object> extends ListView<T, List<T>> {

        ListListView(List<T> delegate, boolean mutable) {
            super(delegate, mutable);
        }

        @Override
        List<T> delegateAppend(List<T> delegate, T element) { return delegate.append(element); }

        @Override
        List<T> delegateInsert(List<T> delegate, int index, T element) { return delegate.insert(index, element); }

        @Override
        List<T> delegateAppendAll(List<T> delegate, Iterable<? extends T> elements) { return delegate.appendAll(elements); }

        @Override
        List<T> delegateInsertAll(List<T> delegate, int index, Iterable<? extends T> elements) { return delegate.insertAll(index, elements); }

        @Override
        List<T> delegateTake(List<T> delegate, int n) { return delegate.take(n); }

        @Override
        T delegateGet(List<T> delegate, int index) { return delegate.get(index); }

        @Override
        int delegateIndexOf(List<T> delegate, T element) { return delegate.indexOf(element); }

        @Override
        int delegateLastIndexOf(List<T> delegate, T element) { return delegate.lastIndexOf(element); }

        @Override
        List<T> delegateRemoveAt(List<T> delegate, int index) { return delegate.removeAt(index); }

        @Override
        List<T> delegateRemove(List<T> delegate, T element) { return delegate.remove(element); }

        @Override
        List<T> delegateRemoveAll(List<T> delegate, Iterable<? extends T> elements) { return delegate.removeAll(elements); }

        @Override
        List<T> delegateRetainAll(List<T> delegate, Iterable<? extends T> elements) { return delegate.retainAll(elements); }

        @Override
        List<T> delegateUpdate(List<T> delegate, int index, T element) { return delegate.update(index, element); }

        @Override
        List<T> delegateSorted(List<T> delegate, Comparator<? super T> comparator) { return delegate.sorted(comparator); }

        @Override
        List<T> delegateSubSequence(List<T> delegate, int beginIndex, int endIndex) { return delegate.subSequence(beginIndex, endIndex); }

        @Override
        ListView<T, List<T>> view(List<T> delegate, boolean mutable) { return new ListListView<>(delegate, mutable); }
    }

    /** The view over a {@link Queue}: every hook is the Queue method of the same name. */
    static final class QueueListView<T extends @Nullable Object> extends ListView<T, Queue<T>> {

        QueueListView(Queue<T> delegate, boolean mutable) {
            super(delegate, mutable);
        }

        @Override
        Queue<T> delegateAppend(Queue<T> delegate, T element) { return delegate.append(element); }

        @Override
        Queue<T> delegateInsert(Queue<T> delegate, int index, T element) { return delegate.insert(index, element); }

        @Override
        Queue<T> delegateAppendAll(Queue<T> delegate, Iterable<? extends T> elements) { return delegate.appendAll(elements); }

        @Override
        Queue<T> delegateInsertAll(Queue<T> delegate, int index, Iterable<? extends T> elements) { return delegate.insertAll(index, elements); }

        @Override
        Queue<T> delegateTake(Queue<T> delegate, int n) { return delegate.take(n); }

        @Override
        T delegateGet(Queue<T> delegate, int index) { return delegate.get(index); }

        @Override
        int delegateIndexOf(Queue<T> delegate, T element) { return delegate.indexOf(element); }

        @Override
        int delegateLastIndexOf(Queue<T> delegate, T element) { return delegate.lastIndexOf(element); }

        @Override
        Queue<T> delegateRemoveAt(Queue<T> delegate, int index) { return delegate.removeAt(index); }

        @Override
        Queue<T> delegateRemove(Queue<T> delegate, T element) { return delegate.remove(element); }

        @Override
        Queue<T> delegateRemoveAll(Queue<T> delegate, Iterable<? extends T> elements) { return delegate.removeAll(elements); }

        @Override
        Queue<T> delegateRetainAll(Queue<T> delegate, Iterable<? extends T> elements) { return delegate.retainAll(elements); }

        @Override
        Queue<T> delegateUpdate(Queue<T> delegate, int index, T element) { return delegate.update(index, element); }

        @Override
        Queue<T> delegateSorted(Queue<T> delegate, Comparator<? super T> comparator) { return delegate.sorted(comparator); }

        @Override
        Queue<T> delegateSubSequence(Queue<T> delegate, int beginIndex, int endIndex) { return delegate.subSequence(beginIndex, endIndex); }

        @Override
        ListView<T, Queue<T>> view(Queue<T> delegate, boolean mutable) { return new QueueListView<>(delegate, mutable); }
    }

    /** The view over a {@link Stream}: every hook is the Stream method of the same name. */
    static final class StreamListView<T extends @Nullable Object> extends ListView<T, Stream<T>> {

        StreamListView(Stream<T> delegate, boolean mutable) {
            super(delegate, mutable);
        }

        @Override
        Stream<T> delegateAppend(Stream<T> delegate, T element) { return delegate.append(element); }

        @Override
        Stream<T> delegateInsert(Stream<T> delegate, int index, T element) { return delegate.insert(index, element); }

        @Override
        Stream<T> delegateAppendAll(Stream<T> delegate, Iterable<? extends T> elements) { return delegate.appendAll(elements); }

        @Override
        Stream<T> delegateInsertAll(Stream<T> delegate, int index, Iterable<? extends T> elements) { return delegate.insertAll(index, elements); }

        @Override
        Stream<T> delegateTake(Stream<T> delegate, int n) { return delegate.take(n); }

        @Override
        T delegateGet(Stream<T> delegate, int index) { return delegate.get(index); }

        @Override
        int delegateIndexOf(Stream<T> delegate, T element) { return delegate.indexOf(element); }

        @Override
        int delegateLastIndexOf(Stream<T> delegate, T element) { return delegate.lastIndexOf(element); }

        @Override
        Stream<T> delegateRemoveAt(Stream<T> delegate, int index) { return delegate.removeAt(index); }

        @Override
        Stream<T> delegateRemove(Stream<T> delegate, T element) { return delegate.remove(element); }

        @Override
        Stream<T> delegateRemoveAll(Stream<T> delegate, Iterable<? extends T> elements) { return delegate.removeAll(elements); }

        @Override
        Stream<T> delegateRetainAll(Stream<T> delegate, Iterable<? extends T> elements) { return delegate.retainAll(elements); }

        @Override
        Stream<T> delegateUpdate(Stream<T> delegate, int index, T element) { return delegate.update(index, element); }

        @Override
        Stream<T> delegateSorted(Stream<T> delegate, Comparator<? super T> comparator) { return delegate.sorted(comparator); }

        @Override
        Stream<T> delegateSubSequence(Stream<T> delegate, int beginIndex, int endIndex) { return delegate.subSequence(beginIndex, endIndex); }

        @Override
        ListView<T, Stream<T>> view(Stream<T> delegate, boolean mutable) { return new StreamListView<>(delegate, mutable); }
    }
}
