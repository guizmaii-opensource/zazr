package io.vavr.collection;

import java.io.Serializable;
import java.util.Comparator;
import org.jspecify.annotations.Nullable;

/**
 * INTERNAL: Common {@code Comparator} related functions (not intended to be public).
 *
 * @author Daniel Dietrich
 */
final class Comparators {

    private Comparators() {
    }

    /**
     * Returns the natural comparator for type U, i.e. treating it as {@code Comparable<U>}.
     * The returned comparator is also {@code java.io.Serializable}.
     * <p>
     * Please note that this will lead to runtime exceptions, if U is not Comparable.
     *
     * @param <U> The type
     * @return The natural Comparator of type U
     */
    @SuppressWarnings("unchecked")
    static <U extends @Nullable Object> Comparator<U> naturalComparator() {
        return NaturalComparator.instance();
    }
}

final class NaturalComparator<T extends @Nullable Object> implements Comparator<T>, Serializable {

    private static final long serialVersionUID = 1L;

    private static final NaturalComparator<?> INSTANCE = new NaturalComparator<>();

    private NaturalComparator() {
    }

    @SuppressWarnings("unchecked")
    static <T extends @Nullable Object> NaturalComparator<T> instance() {
        return (NaturalComparator<T>) INSTANCE;
    }

    @SuppressWarnings("unchecked")
    @Override
    public int compare(T o1, T o2) {
        return ((Comparable<T>) o1).compareTo(o2);
    }

    /** @see Comparator#equals(Object) */
    @Override
    public boolean equals(@Nullable Object obj) {
        return obj instanceof NaturalComparator;
    }

    @Override
    public int hashCode() {
        return 1;
    }

    /**
     * Instance control for object serialization.
     *
     * @return The singleton instance of NaturalComparator.
     * @see java.io.Serializable
     */
    private Object readResolve() {
        return INSTANCE;
    }

}
