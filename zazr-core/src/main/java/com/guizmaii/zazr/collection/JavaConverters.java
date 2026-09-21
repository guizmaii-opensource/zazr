package com.guizmaii.zazr.collection;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;
import org.jspecify.annotations.Nullable;

/**
 * THIS CLASS IS INTENDED TO BE USED INTERNALLY ONLY!
 * <p>
 * This helper class provides methods that return {@link java.util.List} views on zazr sequences ({@link Vector} and
 * the {@link Seq} types). The view creation and back conversion take O(1).
 *
 * @author Daniel Dietrich
 */
class JavaConverters {

    private JavaConverters() {
    }

    static <T extends @Nullable Object> ListView<T, Vector<T>> asJava(Vector<T> vector, ChangePolicy changePolicy) {
        return new VectorListView<>(vector, changePolicy.isMutable());
    }

    static <T extends @Nullable Object, C extends Seq<T>> ListView<T, C> asJava(C seq, ChangePolicy changePolicy) {
        return new SeqListView<>(seq, changePolicy.isMutable());
    }

    enum ChangePolicy {

        IMMUTABLE, MUTABLE;

        boolean isMutable() {
            return this == MUTABLE;
        }
    }

    // -- private view implementations

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

        C getDelegate() {
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
     * {@link VectorListView} for {@link Vector}, {@link SeqListView} for the {@link Seq} types.
     *
     * @param <T> the element type
     * @param <C> the delegate type
     */
    static abstract class ListView<T extends @Nullable Object, C extends Traversable<T>> extends HasDelegate<C> implements java.util.List<T> {

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
            return getDelegate().toJavaArray();
        }

        // Collection.toArray(T[]) mandates writing null just past the last element, even when
        // the caller's array has non-null components.
        @SuppressWarnings({"unchecked", "NullAway"})
        @Override
        public <U extends @Nullable Object> U [] toArray(U [] array) {
            Objects.requireNonNull(array, "array is null");
            final U[] target;
            final C delegate = getDelegate();
            final int length = delegate.length();
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

    /** The view over a {@link Seq} (List, Queue, Stream). */
    static final class SeqListView<T extends @Nullable Object, C extends Seq<T>> extends ListView<T, C> {

        SeqListView(C delegate, boolean mutable) {
            super(delegate, mutable);
        }

        @SuppressWarnings("unchecked")
        @Override
        C delegateAppend(C delegate, T element) { return (C) delegate.append(element); }

        @SuppressWarnings("unchecked")
        @Override
        C delegateInsert(C delegate, int index, T element) { return (C) delegate.insert(index, element); }

        @SuppressWarnings("unchecked")
        @Override
        C delegateAppendAll(C delegate, Iterable<? extends T> elements) { return (C) delegate.appendAll(elements); }

        @SuppressWarnings("unchecked")
        @Override
        C delegateInsertAll(C delegate, int index, Iterable<? extends T> elements) { return (C) delegate.insertAll(index, elements); }

        @SuppressWarnings("unchecked")
        @Override
        C delegateTake(C delegate, int n) { return (C) delegate.take(n); }

        @Override
        T delegateGet(C delegate, int index) { return delegate.get(index); }

        @Override
        int delegateIndexOf(C delegate, T element) { return delegate.indexOf(element); }

        @Override
        int delegateLastIndexOf(C delegate, T element) { return delegate.lastIndexOf(element); }

        @SuppressWarnings("unchecked")
        @Override
        C delegateRemoveAt(C delegate, int index) { return (C) delegate.removeAt(index); }

        @SuppressWarnings("unchecked")
        @Override
        C delegateRemove(C delegate, T element) { return (C) delegate.remove(element); }

        @SuppressWarnings("unchecked")
        @Override
        C delegateRemoveAll(C delegate, Iterable<? extends T> elements) { return (C) delegate.removeAll(elements); }

        @SuppressWarnings("unchecked")
        @Override
        C delegateRetainAll(C delegate, Iterable<? extends T> elements) { return (C) delegate.retainAll(elements); }

        @SuppressWarnings("unchecked")
        @Override
        C delegateUpdate(C delegate, int index, T element) { return (C) delegate.update(index, element); }

        @SuppressWarnings("unchecked")
        @Override
        C delegateSorted(C delegate, Comparator<? super T> comparator) { return (C) delegate.sorted(comparator); }

        @SuppressWarnings("unchecked")
        @Override
        C delegateSubSequence(C delegate, int beginIndex, int endIndex) { return (C) delegate.subSequence(beginIndex, endIndex); }

        @Override
        ListView<T, C> view(C delegate, boolean mutable) { return new SeqListView<>(delegate, mutable); }
    }
}
