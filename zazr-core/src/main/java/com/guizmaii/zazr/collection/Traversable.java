package com.guizmaii.zazr.collection;

import com.guizmaii.zazr.Tuple;
import com.guizmaii.zazr.Tuple2;
import com.guizmaii.zazr.control.Option;
import java.util.Comparator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.ObjIntConsumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.ToDoubleFunction;
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
     * An unmodifiable {@link java.util.Collection} view of this collection: no copy, O(1) to create, every
     * mutator throws {@link UnsupportedOperationException}. The sequences return a {@link java.util.List} view.
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
     * {@code SORTED} on the sorted types.
     *
     * @return a new spliterator
     */
    @Override
    default Spliterator<T> spliterator() {
        final int characteristics = Collections.spliteratorCharacteristics(this);
        return (characteristics & Spliterator.SIZED) != 0
          ? Spliterators.spliterator(iterator(), size(), characteristics)
          : Spliterators.spliteratorUnknownSize(iterator(), characteristics);
    }
}

/** The one-pass operations the concrete types declare with their own signatures, implemented once over an Iterable. */
interface TraversableModule {

    // SortedMap<K, V> orders its keys but is a Traversable<Tuple2<K, V>>: its key comparator must not be applied to
    // the entries, so only the comparator of an element-ordered collection is reused.
    @SuppressWarnings("unchecked")
    static <T extends @Nullable Object> Comparator<T> comparatorOf(Traversable<T> traversable) {
        if (traversable instanceof SortedSet<?> sortedSet) {
            return ((SortedSet<T>) sortedSet).comparator();
        } else {
            return (Comparator<T>) Comparator.naturalOrder();
        }
    }

    static <T extends @Nullable Object, R extends Traversable<T>> R toTraversable(
            Traversable<T> traversable, R empty, Function<Iterable<T>, R> ofAll) {
        return traversable.isEmpty() ? empty : ofAll.apply(traversable);
    }

    @SuppressWarnings("unchecked")
    static <T extends @Nullable Object> SortedSet<T> toSortedSet(Traversable<T> traversable) {
        if (traversable instanceof TreeSet<?> treeSet) {
            return (TreeSet<T>) treeSet;
        }
        final Comparator<T> comparator = comparatorOf(traversable);
        return toTraversable(traversable, TreeSet.empty(comparator), values -> TreeSet.ofAll(comparator, values));
    }

    static <T extends @Nullable Object, K extends @Nullable Object, V extends @Nullable Object, E extends Tuple2<? extends K, ? extends V>, R extends Map<K, V>> R toMap(
            Traversable<T> traversable, R empty, Function<Iterable<E>, R> ofAll, Function<? super T, ? extends E> f) {
        Objects.requireNonNull(f, "f is null");
        return traversable.isEmpty() ? empty : ofAll.apply(Iterator.ofAll(traversable).map(f));
    }

    static <T extends @Nullable Object, K extends @Nullable Object, V extends @Nullable Object, M extends java.util.Map<K, V>> M toJavaMap(
            Traversable<T> traversable, Supplier<M> factory, Function<? super T, ? extends Tuple2<? extends K, ? extends V>> f) {
        Objects.requireNonNull(factory, "factory is null");
        Objects.requireNonNull(f, "f is null");
        final M map = factory.get();
        for (T a : traversable) {
            final Tuple2<? extends K, ? extends V> entry = f.apply(a);
            map.put(entry._1(), entry._2());
        }
        return map;
    }

    static <T extends @Nullable Object, K extends @Nullable Object, V extends @Nullable Object> Function<T, Tuple2<K, V>> entryMapper(
            Function<? super T, ? extends K> keyMapper, Function<? super T, ? extends V> valueMapper) {
        Objects.requireNonNull(keyMapper, "keyMapper is null");
        Objects.requireNonNull(valueMapper, "valueMapper is null");
        return t -> Tuple.of(keyMapper.apply(t), valueMapper.apply(t));
    }

    static <T extends @Nullable Object, R extends java.util.Collection<T>> R toJavaCollection(
            Traversable<T> traversable, Function<Integer, R> containerSupplier) {
        return toJavaCollection(traversable, containerSupplier, 16);
    }

    static <T extends @Nullable Object, R extends java.util.Collection<T>> R toJavaCollection(
            Traversable<T> traversable, Function<Integer, R> containerSupplier, int defaultInitialCapacity) {
        Objects.requireNonNull(containerSupplier, "factory is null");
        // a lazy collection has no cheap size: the default capacity avoids a second traversal
        final int size = Collections.hasDefiniteSize(traversable) ? traversable.size() : defaultInitialCapacity;
        final R container = containerSupplier.apply(size);
        traversable.forEach(container::add);
        return container;
    }

    static <K extends @Nullable Object, T extends @Nullable Object> Option<Map<K, T>> arrangeBy(Map<K, ? extends Traversable<T>> groups) {
        for (Tuple2<K, ? extends Traversable<T>> group : groups) {
            if (group._2().size() != 1) {
                return Option.none();
            }
        }
        return Option.some(groups.mapValues(group -> group.iterator().next()));
    }

    static <T extends @Nullable Object> boolean existsUnique(Iterable<T> elements, Predicate<? super T> predicate) {
        Objects.requireNonNull(predicate, "predicate is null");
        boolean exists = false;
        for (T t : elements) {
            if (predicate.test(t)) {
                if (exists) {
                    return false;
                } else {
                    exists = true;
                }
            }
        }
        return exists;
    }

    // `found` is set together with `last`, so `last` is a real element whenever it is read
    @SuppressWarnings("NullAway")
    static <T extends @Nullable Object> Option<T> findLast(Iterable<T> elements, Predicate<? super T> predicate) {
        Objects.requireNonNull(predicate, "predicate is null");
        @Nullable T last = null;
        boolean found = false;
        for (T t : elements) {
            if (predicate.test(t)) {
                last = t;
                found = true;
            }
        }
        return found ? Option.some(last) : Option.none();
    }

    static <T extends @Nullable Object> void forEachWithIndex(Iterable<T> elements, ObjIntConsumer<? super T> action) {
        Objects.requireNonNull(action, "action is null");
        int index = 0;
        for (T t : elements) {
            action.accept(t, index++);
        }
    }

    static <T extends @Nullable Object> T reduceLeft(Traversable<T> traversable, BiFunction<? super T, ? super T, ? extends T> op) {
        Objects.requireNonNull(op, "op is null");
        final java.util.Iterator<T> iterator = traversable.iterator();
        if (!iterator.hasNext()) {
            throw new NoSuchElementException("reduceLeft on empty " + traversable.getClass().getSimpleName());
        }
        T xs = iterator.next();
        while (iterator.hasNext()) {
            xs = op.apply(xs, iterator.next());
        }
        return xs;
    }

    static <T extends @Nullable Object> Option<T> reduceLeftOption(Traversable<T> traversable, BiFunction<? super T, ? super T, ? extends T> op) {
        Objects.requireNonNull(op, "op is null");
        return traversable.isEmpty() ? Option.none() : Option.some(reduceLeft(traversable, op));
    }

    static <T extends @Nullable Object> T single(Traversable<T> traversable) {
        return singleOption(traversable).getOrElseThrow(() -> new NoSuchElementException("Does not contain a single value"));
    }

    static <T extends @Nullable Object> Option<T> singleOption(Traversable<T> traversable) {
        final java.util.Iterator<T> it = traversable.iterator();
        if (!it.hasNext()) {
            return Option.none();
        }
        final T first = it.next();
        return it.hasNext() ? Option.none() : Option.some(first);
    }

    static <T extends @Nullable Object> Option<T> max(Traversable<T> traversable) {
        return maxBy(traversable, Comparators.naturalComparator());
    }

    static <T extends @Nullable Object> Option<T> maxBy(Traversable<T> traversable, Comparator<? super T> comparator) {
        Objects.requireNonNull(comparator, "comparator is null");
        final java.util.Iterator<T> iterator = traversable.iterator();
        if (!iterator.hasNext()) {
            return Option.none();
        }
        T max = iterator.next();
        while (iterator.hasNext()) {
            final T t = iterator.next();
            if (comparator.compare(t, max) > 0) {
                max = t;
            }
        }
        return Option.some(max);
    }

    static <T extends @Nullable Object, U extends Comparable<? super U>> Option<T> maxBy(Traversable<T> traversable, Function<? super T, ? extends U> f) {
        Objects.requireNonNull(f, "f is null");
        final java.util.Iterator<T> iterator = traversable.iterator();
        if (!iterator.hasNext()) {
            return Option.none();
        }
        T tm = iterator.next();
        U um = f.apply(tm);
        while (iterator.hasNext()) {
            final T t = iterator.next();
            final U u = f.apply(t);
            if (u.compareTo(um) > 0) {
                um = u;
                tm = t;
            }
        }
        return Option.some(tm);
    }

    // minBy(naturalComparator) would not handle (Double/Float) NaN as min() promises
    @SuppressWarnings("unchecked")
    static <T extends @Nullable Object> Option<T> min(Traversable<T> traversable) {
        final java.util.Iterator<T> iterator = traversable.iterator();
        if (!iterator.hasNext()) {
            return Option.none();
        }
        final T head = iterator.next();
        if (head instanceof Double) {
            double min = (Double) head;
            while (iterator.hasNext()) {
                min = Math.min(min, (Double) iterator.next());
            }
            return Option.some((T) (Double) min);
        } else if (head instanceof Float) {
            float min = (Float) head;
            while (iterator.hasNext()) {
                min = Math.min(min, (Float) iterator.next());
            }
            return Option.some((T) (Float) min);
        } else {
            final Comparator<T> comparator = Comparators.naturalComparator();
            T min = head;
            while (iterator.hasNext()) {
                final T t = iterator.next();
                if (comparator.compare(t, min) < 0) {
                    min = t;
                }
            }
            return Option.some(min);
        }
    }

    static <T extends @Nullable Object> Option<T> minBy(Traversable<T> traversable, Comparator<? super T> comparator) {
        Objects.requireNonNull(comparator, "comparator is null");
        final java.util.Iterator<T> iterator = traversable.iterator();
        if (!iterator.hasNext()) {
            return Option.none();
        }
        T min = iterator.next();
        while (iterator.hasNext()) {
            final T t = iterator.next();
            if (comparator.compare(t, min) < 0) {
                min = t;
            }
        }
        return Option.some(min);
    }

    static <T extends @Nullable Object, U extends Comparable<? super U>> Option<T> minBy(Traversable<T> traversable, Function<? super T, ? extends U> f) {
        Objects.requireNonNull(f, "f is null");
        final java.util.Iterator<T> iterator = traversable.iterator();
        if (!iterator.hasNext()) {
            return Option.none();
        }
        T tm = iterator.next();
        U um = f.apply(tm);
        while (iterator.hasNext()) {
            final T t = iterator.next();
            final U u = f.apply(t);
            if (u.compareTo(um) < 0) {
                um = u;
                tm = t;
            }
        }
        return Option.some(tm);
    }

    static Option<Double> average(Traversable<?> traversable) {
        try {
            final double[] sum = neumaierSum(traversable, t -> ((Number) t).doubleValue());
            final double count = sum[1];
            return (count == 0) ? Option.none() : Option.some(sum[0] / count);
        } catch (ClassCastException x) {
            throw new UnsupportedOperationException("Elements are not numeric", x);
        }
    }

    static Number product(Traversable<?> traversable) {
        final java.util.Iterator<?> iterator = traversable.iterator();
        if (!iterator.hasNext()) {
            return 1;
        }
        try {
            final Object o = iterator.next();
            if (o instanceof Integer || o instanceof Long || o instanceof Byte || o instanceof Short) {
                long product = ((Number) o).longValue();
                while (iterator.hasNext()) {
                    product *= ((Number) iterator.next()).longValue();
                }
                return product;
            } else if (o instanceof java.math.BigInteger) {
                java.math.BigInteger product = (java.math.BigInteger) o;
                while (iterator.hasNext()) {
                    product = product.multiply((java.math.BigInteger) iterator.next());
                }
                return product;
            } else if (o instanceof java.math.BigDecimal) {
                java.math.BigDecimal product = (java.math.BigDecimal) o;
                while (iterator.hasNext()) {
                    product = product.multiply((java.math.BigDecimal) iterator.next());
                }
                return product;
            } else {
                double product = ((Number) o).doubleValue();
                while (iterator.hasNext()) {
                    product *= ((Number) iterator.next()).doubleValue();
                }
                return product;
            }
        } catch (ClassCastException x) {
            throw new UnsupportedOperationException("not numeric", x);
        }
    }

    static Number sum(Traversable<?> traversable) {
        final java.util.Iterator<?> iterator = traversable.iterator();
        if (!iterator.hasNext()) {
            return 0;
        }
        try {
            final Object o = iterator.next();
            if (o instanceof Integer || o instanceof Long || o instanceof Byte || o instanceof Short) {
                long sum = ((Number) o).longValue();
                while (iterator.hasNext()) {
                    sum += ((Number) iterator.next()).longValue();
                }
                return sum;
            } else if (o instanceof java.math.BigInteger) {
                java.math.BigInteger sum = (java.math.BigInteger) o;
                while (iterator.hasNext()) {
                    sum = sum.add((java.math.BigInteger) iterator.next());
                }
                return sum;
            } else if (o instanceof java.math.BigDecimal) {
                java.math.BigDecimal sum = (java.math.BigDecimal) o;
                while (iterator.hasNext()) {
                    sum = sum.add((java.math.BigDecimal) iterator.next());
                }
                return sum;
            } else {
                // any other Number, Double and Float included: Neumaier summation over the whole collection
                return neumaierSum(traversable, t -> ((Number) t).doubleValue())[0];
            }
        } catch (ClassCastException x) {
            throw new UnsupportedOperationException("Elements are not numeric", x);
        }
    }

    /**
     * Uses Neumaier's variant of the Kahan summation algorithm in order to sum double values.
     * <p>
     * See <a href="https://en.wikipedia.org/wiki/Kahan_summation_algorithm">Kahan summation algorithm</a>.
     *
     * @param <T> element type
     * @param ts the elements
     * @param toDouble function which maps elements to {@code double} values
     * @return A pair {@code [sum, size]}, where {@code sum} is the compensated sum and {@code size} is the number of elements which were summed.
     */
    static <T extends @Nullable Object> double[] neumaierSum(Iterable<T> ts, ToDoubleFunction<T> toDouble) {
        double simpleSum = 0.0;
        double sum = 0.0;
        double compensation = 0.0;
        int size = 0;
        for (T t : ts) {
            final double d = toDouble.applyAsDouble(t);
            final double tmp = sum + d;
            compensation += (Math.abs(sum) >= Math.abs(d)) ? (sum - tmp) + d : (d - tmp) + sum;
            sum = tmp;
            simpleSum += d;
            size++;
        }
        sum += compensation;
        if (size > 0 && Double.isNaN(sum) && Double.isInfinite(simpleSum)) {
            sum = simpleSum;
        }
        return new double[] { sum, size };
    }
}
