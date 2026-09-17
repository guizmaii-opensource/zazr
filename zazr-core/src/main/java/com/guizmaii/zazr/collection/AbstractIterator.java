package com.guizmaii.zazr.collection;

import java.util.NoSuchElementException;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * Provides a common {@link Object#toString()} implementation.
 * <p>
 * {@code equals(Object)} and {@code hashCode()} are intentionally not overridden in order to prevent this iterator
 * from being evaluated. In other words, (identity-)equals and hashCode are implemented by Object.
 *
 * @param <T> Component type
 * @author Daniel Dietrich
 */
abstract class AbstractIterator<T extends @Nullable Object> implements Iterator<T> {

    @Override
    public String toString() {
        return "Iterator(" + (isEmpty() ? "" : "?") + ")";
    }

    protected abstract T getNext();

    /**
     * Every {@code Iterator} implementation funnels through here, so this is the single place that
     * rejects a null element produced by a user-supplied function or source (design 3.9): a mapper,
     * supplier, seed, fill value, or bulk source. {@link Lazy}'s own iterator is not an
     * {@code AbstractIterator} and stays outside this check, since {@code Lazy} may hold {@code null}.
     */
    @Override
    public final T next() {
        if (!hasNext()) {
            throw new NoSuchElementException("next() on empty iterator");
        }
        return Objects.requireNonNull(getNext(), "Iterator: element is null");
    }
}
