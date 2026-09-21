package com.guizmaii.zazr.collection;

import com.guizmaii.zazr.Tuple2;
import com.guizmaii.zazr.control.Option;
import java.util.Comparator;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import org.jspecify.annotations.Nullable;

/**
 * A {@link Set} whose elements are kept in the order of a {@link Comparator}; {@link TreeSet} is the implementation.
 * <p>
 * Specific SortedSet operations:
 *
 * <ul>
 * <li>{@link #comparator()}</li>
 * <li>{@link #flatMap(Comparator, Function)}</li>
 * <li>{@link #map(Comparator, Function)}</li>
 * </ul>
 *
 * @param <T> Component type
 * @author Daniel Dietrich
 */
public interface SortedSet<T extends @Nullable Object> extends Set<T> {

    /**
     * Narrows a widened {@code SortedSet<? extends T>} to {@code SortedSet<T>}
     * by performing a type-safe cast. This is eligible because immutable/read-only
     * collections are covariant.
     * <p>
     * CAUTION: The underlying {@code Comparator} might fail!
     *
     * @param sortedSet A {@code SortedSet}.
     * @param <T>       Component type of the {@code SortedSet}.
     * @return the given {@code sortedSet} instance as narrowed type {@code SortedSet<T>}.
     */
    @SuppressWarnings("unchecked")
    static <T extends @Nullable Object> SortedSet<T> narrow(SortedSet<? extends T> sortedSet) {
        return (SortedSet<T>) sortedSet;
    }

    /**
     * The comparator that orders the elements; the iteration order is consistent with it.
     *
     * @return the comparator defining the order
     */
    Comparator<T> comparator();

    /**
     * Same as {@link #flatMap(Function)} but using a specific comparator for values of the codomain of the given
     * {@code mapper}.
     *
     * @param comparator A comparator for values of type U
     * @param mapper     A function which maps values of type T to Iterables of values of type U
     * @param <U>        Type of flat-mapped values
     * @return A new Set instance containing mapped values
     */
    <U extends @Nullable Object> SortedSet<U> flatMap(Comparator<? super U> comparator, Function<? super T, ? extends Iterable<? extends U>> mapper);

    /**
     * Same as {@link #map(Function)} but using a specific comparator for values of the codomain of the given
     * {@code mapper}.
     *
     * @param comparator A comparator for values of type U
     * @param mapper     A function which maps values of type T to values of type U
     * @param <U>        Type of mapped values
     * @return A new Set instance containing mapped values
     */
    <U extends @Nullable Object> SortedSet<U> map(Comparator<? super U> comparator, Function<? super T, ? extends U> mapper);

    // -- Adjusted return types of Set methods

    @Override
    SortedSet<T> add(T element);

    @Override
    SortedSet<T> addAll(Iterable<? extends T> elements);

    @Override
    SortedSet<T> diff(Set<? extends T> elements);

    @Override
    SortedSet<T> filter(Predicate<? super T> predicate);

    @Override
    SortedSet<T> reject(Predicate<? super T> predicate);

    @Override
    <U extends @Nullable Object> SortedSet<U> flatMap(Function<? super T, ? extends Iterable<? extends U>> mapper);

    @Override
    <C extends @Nullable Object> Map<C, ? extends SortedSet<T>> groupBy(Function<? super T, ? extends C> classifier);

    @Override
    SortedSet<T> intersect(Set<? extends T> elements);

    @Override
    <U extends @Nullable Object> SortedSet<U> map(Function<? super T, ? extends U> mapper);

    @Override
    <U extends @Nullable Object> SortedSet<U> collect(Function<? super T, ? extends Option<? extends U>> mapper);

    @Override
    default <U extends @Nullable Object> SortedSet<U> as(U value) {
        return map((o1, o2) -> 0, ignored -> value);
    }

    @Override
    SortedSet<T> orElse(Iterable<? extends T> other);

    @Override
    SortedSet<T> orElse(Supplier<? extends Iterable<? extends T>> supplier);

    @Override
    Tuple2<? extends SortedSet<T>, ? extends SortedSet<T>> partition(Predicate<? super T> predicate);

    @Override
    SortedSet<T> tap(Consumer<? super T> action);

    @Override
    SortedSet<T> remove(T element);

    @Override
    SortedSet<T> removeAll(Iterable<? extends T> elements);

    @Override
    SortedSet<T> replace(T currentElement, T newElement);

    @Override
    SortedSet<T> replaceAll(T currentElement, T newElement);

    @Override
    SortedSet<T> retainAll(Iterable<? extends T> elements);

    @Override
    java.util.SortedSet<T> toJavaSet();

    @Override
    SortedSet<T> union(Set<? extends T> elements);
}
