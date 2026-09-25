package com.guizmaii.zazr.collection.internal;

import com.guizmaii.zazr.collection.HashSet;
import com.guizmaii.zazr.collection.LinkedHashSet;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.SequencedSet;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Predicate;
import org.jspecify.annotations.Nullable;

import static com.guizmaii.zazr.collection.internal.JavaConverters.unmodifiable;

/**
 * The unmodifiable {@link java.util.Set} views: {@link java.util.Set} for {@link HashSet}, {@link SequencedSet} for
 * {@link LinkedHashSet}. The {@link java.util.NavigableSet} view of a {@code TreeSet} is in {@link TreeViews}. The
 * rules of {@link JavaConverters} hold: O(1) to create, nothing copied, every mutator throws
 * {@link UnsupportedOperationException}. {@code equals} and {@code hashCode} follow {@link java.util.Set}: a view
 * equals any {@code java.util.Set} with the same elements, and its hash is the sum of the elements' hashes.
 */
public final class SetViews {

    private SetViews() {
    }

    public static <T extends @Nullable Object> java.util.Set<T> asJava(HashSet<T> set) {
        return new HashSetView<>(set);
    }

    /**
     * The {@link SequencedSet} view of a {@link LinkedHashSet}.
     *
     * @param set     the set
     * @param reverse the elements of {@code set} in reverse insertion order, each call a new iterator
     * @param <T>     the element type
     * @return the view
     */
    public static <T extends @Nullable Object> SequencedSet<T> asJava(LinkedHashSet<T> set, Iterable<T> reverse) {
        return new SequencedSetView<>(set, reverse, false);
    }

    /**
     * The base of the read-only {@link java.util.Set} views: every mutator throws.
     *
     * @param <T> the element type
     */
    abstract static class UnmodifiableSet<T extends @Nullable Object> extends AbstractSet<T> implements JavaConverters.View {

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

        /** Throws: the view is unmodifiable. */
        public final void addFirst(T element) {
            throw unmodifiable();
        }

        /** Throws: the view is unmodifiable. */
        public final void addLast(T element) {
            throw unmodifiable();
        }

        /** Throws: the view is unmodifiable. */
        public final T removeFirst() {
            throw unmodifiable();
        }

        /** Throws: the view is unmodifiable. */
        public final T removeLast() {
            throw unmodifiable();
        }
    }

    /** The view of a {@link HashSet}: every read is the set's own. */
    static final class HashSetView<T extends @Nullable Object> extends UnmodifiableSet<T> {

        private final HashSet<T> set;

        HashSetView(HashSet<T> set) {
            this.set = set;
        }

        @Override
        public Object underlying() {
            return set;
        }

        @Override
        public int size() {
            return set.size();
        }

        @Override
        public boolean isEmpty() {
            return set.isEmpty();
        }

        @SuppressWarnings({"unchecked", "NullAway"}) // the unchecked cast of a nullable argument; the delegate accepts null
        @Override
        public boolean contains(@Nullable Object element) {
            return set.contains((T) element);
        }

        @Override
        public java.util.Iterator<T> iterator() {
            return set.iterator();
        }

        @Override
        public Spliterator<T> spliterator() {
            return set.spliterator();
        }

        @Override
        public Object[] toArray() {
            return set.toArray();
        }
    }

    /**
     * The view of a {@link LinkedHashSet} in insertion order, or in reverse insertion order when {@code reversed} is
     * set (the view {@link #reversed()} returns).
     */
    static final class SequencedSetView<T extends @Nullable Object> extends UnmodifiableSet<T> implements SequencedSet<T> {

        private final LinkedHashSet<T> set;
        private final Iterable<T> reverse;
        private final boolean reversed;

        SequencedSetView(LinkedHashSet<T> set, Iterable<T> reverse, boolean reversed) {
            this.set = set;
            this.reverse = reverse;
            this.reversed = reversed;
        }

        @Override
        public @Nullable Object underlying() {
            return reversed ? null : set;
        }

        @Override
        public int size() {
            return set.size();
        }

        @Override
        public boolean isEmpty() {
            return set.isEmpty();
        }

        @SuppressWarnings({"unchecked", "NullAway"}) // the unchecked cast of a nullable argument; the delegate accepts null
        @Override
        public boolean contains(@Nullable Object element) {
            return set.contains((T) element);
        }

        @Override
        public java.util.Iterator<T> iterator() {
            return reversed ? reverse.iterator() : set.iterator();
        }

        @Override
        public Spliterator<T> spliterator() {
            return reversed
                   ? Spliterators.spliterator(this, Spliterator.DISTINCT | Spliterator.ORDERED | Spliterator.IMMUTABLE)
                   : set.spliterator();
        }

        @Override
        public Object[] toArray() {
            return reversed ? super.toArray() : set.toArray();
        }

        @Override
        public T getFirst() {
            return reversed ? set.last() : set.head();
        }

        @Override
        public T getLast() {
            return reversed ? set.head() : set.last();
        }

        @Override
        public SequencedSet<T> reversed() {
            return new SequencedSetView<>(set, reverse, !reversed);
        }
    }
}
