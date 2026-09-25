package com.guizmaii.zazr.collection;

import com.guizmaii.zazr.collection.internal.Collections;
import com.guizmaii.zazr.collection.internal.JavaConverters;
import com.guizmaii.zazr.collection.internal.TraversableModule;

import com.guizmaii.zazr.control.Option;
import java.util.Objects;
import java.util.Spliterator;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.IntFunction;
import java.util.function.Predicate;
import java.util.stream.StreamSupport;
import org.jspecify.annotations.Nullable;

/**
 * What every persistent collection can do in one pass over its elements, whatever its shape.
 * <p>
 * A {@code Traversable} is an {@link Iterable} whose iteration order is the type's own: positional on the sequences
 * ({@link Vector}, {@link List}, {@link Queue}, {@link Stream}), insertion order on {@link LinkedHashSet} and
 * {@link LinkedHashMap}, comparator order on {@link TreeSet} and {@link TreeMap}, and unspecified on {@link HashSet}
 * and {@link HashMap}. The operations declared here are the ones whose result does not depend on that order (or,
 * for {@code foldLeft}, {@code mkString}, {@code forEach} and the conversions, that simply follow it) and whose cost
 * is at most one pass, O(n), on every implementation. Everything positional, order-sensitive or
 * complexity-sensitive is declared by the concrete types, each with its own return type and, on the sequences, a
 * {@code Complexity:} line in its javadoc.
 * <p>
 * <strong>Equality.</strong> There are three collection kinds: the sequences, the sets ({@link Set}) and the maps
 * ({@link Map}). Two collections are equal if and only if they are of the same kind and contain the same elements,
 * in the same order for sequences and regardless of order otherwise; so {@code Vector.of(1, 2)} equals
 * {@code List.of(1, 2)} and {@code HashSet.of(1, 2)} equals {@code TreeSet.of(2, 1)}, while no sequence equals a
 * set. Two map entries are equal when both their keys and their values are. {@code hashCode} agrees with
 * {@code equals}: a sequence hashes its elements in order ({@code hash = hash * 31 + Objects.hashCode(t)}), a set
 * or a map sums them ({@code hash += Objects.hashCode(t)}); an empty collection hashes to {@code 1}. Hash codes
 * are not cached: computing one is O(n). {@code toString} is the type name followed by the elements in
 * parentheses, e.g. {@code List(1, 2, 3)}.
 *
 * @param <T> the element type
 * @author Daniel Dietrich, Grzegorz Piwowarek
 */
public interface Traversable<T extends @Nullable Object> extends Iterable<T> {

    /**
     * Narrows a {@code Traversable<? extends T>} to {@code Traversable<T>} with a type-safe cast.
     * <p>
     * This is safe because immutable or read-only collections are covariant in their element type.
     *
     * @param traversable the {@code Traversable} instance to narrow
     * @param <T>         the element type of the resulting {@code Traversable}
     * @return the same {@code traversable} instance with type {@code Traversable<T>}
     */
    @SuppressWarnings("unchecked")
    static <T extends @Nullable Object> Traversable<T> narrow(Traversable<? extends T> traversable) {
        return (Traversable<T>) traversable;
    }

    /**
     * An iterator over the elements, in this collection's own order. The iterator does not support
     * {@link java.util.Iterator#remove()}.
     *
     * @return a new iterator over the elements
     */
    @Override
    java.util.Iterator<T> iterator();

    /**
     * The number of elements.
     *
     * @return the number of elements, {@code >= 0}
     */
    int size();

    /**
     * Whether this collection has no elements.
     *
     * @return {@code true} if there is no element, {@code false} otherwise
     */
    default boolean isEmpty() {
        return size() == 0;
    }

    /**
     * Whether this collection has at least one element; the negation of {@link #isEmpty()}.
     *
     * @return {@code true} if there is at least one element, {@code false} otherwise
     */
    default boolean nonEmpty() {
        return !isEmpty();
    }

    /**
     * Whether {@code element} is one of the elements, compared with {@link Objects#equals(Object, Object)}. The
     * sets and the maps answer it from their own structure, the sequences by walking their elements.
     * <p>
     * Complexity: O(n): the elements are compared one by one until an equal one is found. The sets and the maps
     * override it with a lookup.
     *
     * @param element the element to look for
     * @return {@code true} if an equal element is contained, {@code false} otherwise
     */
    default boolean contains(T element) {
        return exists(e -> Objects.equals(e, element));
    }

    /**
     * Whether every element of {@code elements} is contained, as {@link #contains(Object)} tells it.
     *
     * @param elements the elements to look for; an empty {@code Iterable} is always contained
     * @return {@code true} if all of them are contained, {@code false} otherwise
     * @throws NullPointerException if {@code elements} is null
     */
    default boolean containsAll(Iterable<? extends T> elements) {
        Objects.requireNonNull(elements, "elements is null");
        for (T element : elements) {
            if (!contains(element)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Whether at least one element satisfies {@code predicate}. The walk stops at the first match.
     *
     * @param predicate the condition to test
     * @return {@code true} if the predicate holds for one or more elements, {@code false} otherwise
     * @throws NullPointerException if {@code predicate} is null
     */
    default boolean exists(Predicate<? super T> predicate) {
        Objects.requireNonNull(predicate, "predicate is null");
        for (T t : this) {
            if (predicate.test(t)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Whether every element satisfies {@code predicate}; true on an empty collection. The walk stops at the first
     * element that does not.
     *
     * @param predicate the condition to test
     * @return {@code true} if the predicate holds for all elements, {@code false} otherwise
     * @throws NullPointerException if {@code predicate} is null
     */
    default boolean forAll(Predicate<? super T> predicate) {
        Objects.requireNonNull(predicate, "predicate is null");
        return !exists(predicate.negate());
    }

    /**
     * The number of elements that satisfy {@code predicate}.
     *
     * @param predicate the condition to test
     * @return how many elements match, {@code >= 0}
     * @throws NullPointerException if {@code predicate} is null
     */
    default int count(Predicate<? super T> predicate) {
        Objects.requireNonNull(predicate, "predicate is null");
        int count = 0;
        for (T t : this) {
            if (predicate.test(t)) {
                count++;
            }
        }
        return count;
    }

    /**
     * The first element, in this collection's order, that satisfies {@code predicate}.
     *
     * @param predicate the condition to test
     * @return {@code Some(element)} of the first match, or {@code None} if no element matches
     * @throws NullPointerException if {@code predicate} is null
     */
    default Option<T> find(Predicate<? super T> predicate) {
        Objects.requireNonNull(predicate, "predicate is null");
        for (T t : this) {
            if (predicate.test(t)) {
                return Option.some(t);
            }
        }
        return Option.none();
    }

    /**
     * Folds the elements from the left: starts with {@code zero} and combines the accumulator with each element in
     * this collection's order.
     * <pre>{@code
     * // = 42
     * List.of('4', '2').foldLeft(0, (acc, x) -> acc * 10 + (x - '0'));
     * }</pre>
     *
     * @param <U>  the type of the accumulator
     * @param zero the initial accumulator
     * @param f    combines the accumulator so far and the next element
     * @return the final accumulator, {@code zero} on an empty collection
     * @throws NullPointerException if {@code f} is null
     */
    default <U extends @Nullable Object> U foldLeft(U zero, BiFunction<? super U, ? super T, ? extends U> f) {
        Objects.requireNonNull(f, "f is null");
        U xs = zero;
        for (T x : this) {
            xs = f.apply(xs, x);
        }
        return xs;
    }

    /**
     * The string representations of the elements, concatenated; the same as {@code mkString("", "", "")}.
     *
     * @return the concatenation
     */
    default String mkString() {
        return mkString("", "", "");
    }

    /**
     * The string representations of the elements, separated by {@code delimiter}; the same as
     * {@code mkString("", delimiter, "")}.
     *
     * @param delimiter what to put between two elements
     * @return the concatenation
     */
    default String mkString(CharSequence delimiter) {
        return mkString("", delimiter, "");
    }

    /**
     * The string representations of the elements, separated by {@code delimiter}, between {@code prefix} and
     * {@code suffix}: {@code List.of("a", "b", "c").mkString("Chars(", ", ", ")")} is {@code "Chars(a, b, c)"}.
     *
     * @param prefix    what to put first
     * @param delimiter what to put between two elements
     * @param suffix    what to put last
     * @return the concatenation
     */
    default String mkString(CharSequence prefix, CharSequence delimiter, CharSequence suffix) {
        final StringBuilder builder = new StringBuilder(prefix);
        boolean first = true;
        for (T t : this) {
            if (first) {
                first = false;
            } else {
                builder.append(delimiter);
            }
            builder.append(t);
        }
        return builder.append(suffix).toString();
    }

    /**
     * Runs {@code action} on each element, in this collection's order.
     *
     * @param action what to do with each element
     * @throws NullPointerException if {@code action} is null
     */
    @Override
    default void forEach(Consumer<? super T> action) {
        Objects.requireNonNull(action, "action is null");
        for (T t : this) {
            action.accept(t);
        }
    }

    // -- conversions

    /**
     * The elements as a {@link Vector}, in this collection's order; this instance if it already is a {@code Vector}.
     *
     * @return a {@code Vector} of the elements
     */
    default Vector<T> toVector() {
        return TraversableModule.toTraversable(this, Vector.empty(), Vector::ofAll);
    }

    /**
     * The elements as a {@link List}, in this collection's order; this instance if it already is a {@code List}.
     *
     * @return a {@code List} of the elements
     */
    default List<T> toList() {
        return TraversableModule.toTraversable(this, List.empty(), List::ofAll);
    }

    /**
     * The distinct elements as a {@link HashSet}; this instance if it already is a {@code HashSet}.
     *
     * @return a {@code Set} of the elements
     */
    default Set<T> toSet() {
        return TraversableModule.toTraversable(this, HashSet.empty(), HashSet::ofAll);
    }

    /**
     * A sequential {@link java.util.stream.Stream} over the elements, built on {@link #spliterator()}, so it reports
     * this collection's characteristics (its size, whether its elements are distinct, sorted or ordered).
     *
     * @return a new sequential {@code java.util.stream.Stream}
     */
    default java.util.stream.Stream<T> stream() {
        return StreamSupport.stream(spliterator(), false);
    }

    /**
     * The elements copied into a new {@code Object[]}, in this collection's order.
     *
     * @return a new array of the elements
     */
    default Object[] toArray() {
        final Object[] array = new Object[size()];
        int i = 0;
        for (T t : this) {
            array[i++] = t;
        }
        return array;
    }

    /**
     * The elements copied into a new array of the component type {@code arrayFactory} makes, in this collection's
     * order: {@code List.of(1, 2, 3).toArray(Integer[]::new)} is an {@code Integer[]}.
     *
     * @param arrayFactory makes an array of the wanted component type and the given size
     * @return the array {@code arrayFactory} made, filled with the elements
     * @throws NullPointerException if {@code arrayFactory} is null
     */
    default T[] toArray(IntFunction<T[]> arrayFactory) {
        Objects.requireNonNull(arrayFactory, "arrayFactory is null");
        final T[] array = arrayFactory.apply(size());
        int i = 0;
        for (T t : this) {
            array[i++] = t;
        }
        return array;
    }

    /**
     * An unmodifiable {@link java.util.Collection} view of this collection: no copy, O(1) to create, reads go through
     * to this collection, which never changes, and every mutator of the view throws
     * {@link UnsupportedOperationException}, whether or not it would change anything. The sequences narrow it to a
     * {@link java.util.List} view, the sets to a {@link java.util.Set} view; a map's is the {@code Collection} of its
     * entries, whose {@code contains} looks the key of an entry up in the map, and its {@link java.util.Map} view is
     * {@link Map#asJavaMap()}. A mutable copy is
     * {@code new java.util.ArrayList<>(traversable.asJava())}.
     * <p>
     * Complexity: O(1): nothing is copied. On the view, {@code contains} costs what this collection's own
     * {@code contains} costs.
     *
     * @return a read-only view of the elements
     */
    default java.util.Collection<T> asJava() {
        return new JavaConverters.CollectionView<>(this);
    }

    /**
     * A {@link Spliterator} over the elements that reports what this collection guarantees: {@code IMMUTABLE},
     * {@code SIZED} and {@code SUBSIZED} unless the size is not known without a walk ({@link Stream}),
     * {@code DISTINCT} on the sets and the maps, {@code ORDERED} where the iteration order is defined, and
     * {@code SORTED} on a {@link SortedSet}, whose {@link Spliterator#getComparator() comparator} the spliterator
     * reports ({@code null} for the natural order). A {@link SortedMap} orders its keys, not its entries, so it
     * is {@code ORDERED} but not {@code SORTED}.
     *
     * @return a new spliterator
     */
    @Override
    default Spliterator<T> spliterator() {
        return Collections.spliterator(this);
    }
}
