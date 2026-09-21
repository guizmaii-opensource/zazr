package com.guizmaii.zazr.collection;

import com.guizmaii.zazr.Tuple2;
import com.guizmaii.zazr.control.Option;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.StreamSupport;
import org.jspecify.annotations.Nullable;

/**
 * A persistent set: distinct elements, no positions. The three implementations differ in their iteration order and
 * cost model: {@link HashSet} (a hash trie, unspecified order), {@link LinkedHashSet} (insertion order) and
 * {@link TreeSet} (comparator order, a {@link SortedSet}).
 * <p>
 * A set declares the operations whose result does not depend on its iteration order: membership and the set
 * algebra ({@code add}, {@code remove}, {@code union}, {@code intersect}, {@code diff}), the element-wise
 * transformations ({@code map}, {@code flatMap}, {@code filter}, {@code collect}, {@code partition},
 * {@code groupBy}), the reductions that a set can answer ({@code fold}, {@code reduce}, {@code max}, {@code min},
 * {@code sum}) and the conversions. Nothing positional ({@code head}, {@code take}, {@code zip},
 * {@code sliding}, ...) is declared: a set has no first element.
 *
 * @param <T> the element type
 * @author Daniel Dietrich
 */
public interface Set<T extends @Nullable Object> extends Traversable<T> {

    /**
     * Narrows a widened {@code Set<? extends T>} to {@code Set<T>}
     * by performing a type-safe cast. This is eligible because immutable/read-only
     * collections are covariant.
     *
     * @param set A {@code Set}.
     * @param <T> Component type of the {@code Set}.
     * @return the given {@code set} instance as narrowed type {@code Set<T>}.
     */
    @SuppressWarnings("unchecked")
    static <T extends @Nullable Object> Set<T> narrow(Set<? extends T> set) {
        return (Set<T>) set;
    }

    /**
     * Returns a set containing all elements of this set plus the given element,
     * if it was not already present.
     *
     * @param element the element to add
     * @return a set including the element; may be this instance if the element was already present
     */
    Set<T> add(T element);

    /**
     * Returns a set containing all elements of this set plus the given elements,
     * excluding duplicates.
     *
     * @param elements the elements to add
     * @return a set including the additional elements; may be this instance (or {@code elements} itself) if no change was necessary
     */
    Set<T> addAll(Iterable<? extends T> elements);

    /**
     * Whether {@code element} is a member of this set, answered from the set's own structure.
     *
     * @param element the element to look for
     * @return {@code true} if {@code element} is contained, {@code false} otherwise
     */
    @Override
    boolean contains(T element);

    /**
     * Returns a set containing all elements of this set except those in the given set.
     *
     * @param that the set of elements to remove
     * @return a set without the specified elements; may be this instance if none of them was present
     */
    Set<T> diff(Set<? extends T> that);

    /**
     * Returns a set containing only the elements present in both this set and the given set.
     *
     * @param that the set to intersect with
     * @return a set with elements common to both sets; may be this instance if unchanged
     */
    Set<T> intersect(Set<? extends T> that);

    /**
     * Returns a set with the given element removed, if it was present.
     *
     * @param element the element to remove
     * @return a set without the specified element; may be this instance if the element was not present
     */
    Set<T> remove(T element);

    /**
     * Returns a set with all given elements removed, if present.
     *
     * @param elements the elements to remove
     * @return a set without the specified elements; may be this instance if none of them was present
     */
    Set<T> removeAll(Iterable<? extends T> elements);

    /**
     * Converts this zazr set to a {@code java.util.Set}. Ordered implementations ({@code LinkedHashSet},
     * {@code SortedSet}) preserve their insertion or sort order in the returned set; {@code HashSet} makes
     * no ordering guarantee.
     *
     * @return a new {@code java.util.Set} instance
     */
    java.util.Set<T> toJavaSet();

    /**
     * Returns a set containing all distinct elements from this set and the given set.
     *
     * @param that the set to union with
     * @return a set with all elements from both sets; may be this instance (or {@code that} itself) if no change was necessary
     */
    Set<T> union(Set<? extends T> that);

    // -- element-wise transformations

    /**
     * The elements that satisfy {@code predicate}.
     *
     * @param predicate the condition to keep an element
     * @return a set of the matching elements; may be this instance if all of them match
     * @throws NullPointerException if {@code predicate} is null
     */
    Set<T> filter(Predicate<? super T> predicate);

    /**
     * The elements that do not satisfy {@code predicate}; the same as {@code filter(predicate.negate())}.
     *
     * @param predicate the condition to drop an element
     * @return a set of the elements that do not match; may be this instance if none of them matches
     * @throws NullPointerException if {@code predicate} is null
     */
    Set<T> reject(Predicate<? super T> predicate);

    /**
     * Maps every element with {@code mapper}; two elements mapped to equal values become one.
     *
     * @param mapper a function from an element to its replacement
     * @param <U>    the new element type
     * @return a set of the mapped elements
     * @throws NullPointerException if {@code mapper} is null
     */
    <U extends @Nullable Object> Set<U> map(Function<? super T, ? extends U> mapper);

    /**
     * Maps every element to an {@code Iterable} and collects all their elements into one set.
     *
     * @param mapper a function from an element to the elements that replace it
     * @param <U>    the new element type
     * @return a set of all the elements the mapper produced
     * @throws NullPointerException if {@code mapper} is null
     */
    <U extends @Nullable Object> Set<U> flatMap(Function<? super T, ? extends Iterable<? extends U>> mapper);

    /**
     * Matches and transforms the elements in one pass: {@code mapper} returns {@code Some} of the new element for
     * an element it accepts and {@code None} for one it drops. The {@code case} ergonomics come from a
     * {@code switch} inside the lambda:
     * <pre>{@code
     * Set<Double> radii = shapes.collect(s -> switch (s) {
     *     case Circle c -> Option.some(c.radius());
     *     default -> Option.none();
     * });
     * }</pre>
     * This is {@code filter} and {@code map} in one step: each element is passed to the mapper exactly once.
     *
     * @param mapper a function from an element to {@code Some} of its replacement or {@code None}; it must not
     *               return {@code null}
     * @param <U>    the type of the collected elements
     * @return a set of the collected elements
     * @throws NullPointerException if {@code mapper} is null, or if it returns {@code null} for an element
     */
    <U extends @Nullable Object> Set<U> collect(Function<? super T, ? extends Option<? extends U>> mapper);

    /**
     * Replaces every element with {@code value}: a set of at most one element. The same as
     * {@code map(ignored -> value)}.
     *
     * @param value the value every element is replaced with
     * @param <U>   the new element type
     * @return the set of {@code value}, or an empty set if this set is empty
     */
    default <U extends @Nullable Object> Set<U> as(U value) {
        return map(ignored -> value);
    }

    /**
     * Splits the elements into those that satisfy {@code predicate} and those that do not.
     *
     * @param predicate the condition
     * @return the matching elements and the others, as two sets
     * @throws NullPointerException if {@code predicate} is null
     */
    Tuple2<? extends Set<T>, ? extends Set<T>> partition(Predicate<? super T> predicate);

    /**
     * Groups the elements by the key {@code classifier} computes for each of them.
     *
     * @param classifier the key of an element
     * @param <C>        the key type
     * @return a map from each key to the set of the elements with that key
     * @throws NullPointerException if {@code classifier} is null
     */
    <C extends @Nullable Object> Map<C, ? extends Set<T>> groupBy(Function<? super T, ? extends C> classifier);

    /**
     * This set if it is non-empty, otherwise a set of the elements of {@code other}.
     *
     * @param other the elements to fall back on
     * @return this set if non-empty, otherwise a set of {@code other}
     * @throws NullPointerException if this set is empty and {@code other} is null
     */
    Set<T> orElse(Iterable<? extends T> other);

    /**
     * This set if it is non-empty, otherwise a set of the elements {@code supplier} provides; the supplier is only
     * called when this set is empty.
     *
     * @param supplier provides the elements to fall back on
     * @return this set if non-empty, otherwise a set of {@code supplier.get()}
     * @throws NullPointerException if this set is empty and {@code supplier} is null
     */
    Set<T> orElse(Supplier<? extends Iterable<? extends T>> supplier);

    /**
     * Runs {@code action} on every element and returns this set, to observe the elements in the middle of a chain
     * of calls. Whatever the action throws propagates to the caller.
     *
     * @param action what to do with each element
     * @return this set
     * @throws NullPointerException if {@code action} is null
     */
    Set<T> tap(Consumer<? super T> action);

    /**
     * Replaces {@code currentElement} with {@code newElement}, if it is a member.
     *
     * @param currentElement the element to replace
     * @param newElement     its replacement
     * @return a set with the replacement made; this set if {@code currentElement} is not a member
     */
    Set<T> replace(T currentElement, T newElement);

    /**
     * The same as {@link #replace(Object, Object)}: a set holds an element at most once.
     *
     * @param currentElement the element to replace
     * @param newElement     its replacement
     * @return a set with the replacement made; this set if {@code currentElement} is not a member
     */
    Set<T> replaceAll(T currentElement, T newElement);

    /**
     * Keeps only the elements that are also in {@code elements}.
     *
     * @param elements the elements to keep
     * @return a set of the elements of this set that are in {@code elements}; may be this instance if all are
     * @throws NullPointerException if {@code elements} is null
     */
    Set<T> retainAll(Iterable<? extends T> elements);

    // -- reductions and conversions

    /**
     * Whether exactly one element satisfies {@code predicate}.
     *
     * @param predicate the condition to test
     * @return {@code true} if one and only one element matches, {@code false} otherwise
     * @throws NullPointerException if {@code predicate} is null
     */
    default boolean existsUnique(Predicate<? super T> predicate) {
        return TraversableModule.existsUnique(this, predicate);
    }

    /**
     * The greatest element in the natural order of the elements, which must be {@link Comparable}; the sort order
     * of a sorted collection is not consulted. {@code NaN} compares as the greatest {@code Double} or {@code Float}.
     *
     * @return {@code Some(maximum)} if there is an element, {@code None} otherwise
     * @throws ClassCastException if two or more elements are not {@code Comparable}
     */
    default Option<T> max() {
        return TraversableModule.max(this);
    }

    /**
     * The greatest element according to {@code comparator}; of equal greatest elements, the first in this
     * Set's order.
     *
     * @param comparator the order
     * @return {@code Some(maximum)} if there is an element, {@code None} otherwise
     * @throws NullPointerException if {@code comparator} is null
     */
    default Option<T> maxBy(Comparator<? super T> comparator) {
        return TraversableModule.maxBy(this, comparator);
    }

    /**
     * The element whose key, computed once by {@code f}, is the greatest; of equal greatest keys, the first
     * element in this Set's order.
     *
     * @param f   the key of an element
     * @param <U> the key type
     * @return {@code Some(element)} if there is an element, {@code None} otherwise
     * @throws NullPointerException if {@code f} is null
     */
    default <U extends Comparable<? super U>> Option<T> maxBy(Function<? super T, ? extends U> f) {
        return TraversableModule.maxBy(this, f);
    }

    /**
     * The least element in the natural order of the elements, which must be {@link Comparable}; the sort order of
     * a sorted collection is not consulted. Among {@code Double}s or {@code Float}s, a {@code NaN} is the result
     * whenever one is present.
     *
     * @return {@code Some(minimum)} if there is an element, {@code None} otherwise
     * @throws ClassCastException if two or more elements are not {@code Comparable}
     */
    default Option<T> min() {
        return TraversableModule.min(this);
    }

    /**
     * The least element according to {@code comparator}; of equal least elements, the first in this Set's
     * order.
     *
     * @param comparator the order
     * @return {@code Some(minimum)} if there is an element, {@code None} otherwise
     * @throws NullPointerException if {@code comparator} is null
     */
    default Option<T> minBy(Comparator<? super T> comparator) {
        return TraversableModule.minBy(this, comparator);
    }

    /**
     * The element whose key, computed once by {@code f}, is the least; of equal least keys, the first element in
     * this Set's order.
     *
     * @param f   the key of an element
     * @param <U> the key type
     * @return {@code Some(element)} if there is an element, {@code None} otherwise
     * @throws NullPointerException if {@code f} is null
     */
    default <U extends Comparable<? super U>> Option<T> minBy(Function<? super T, ? extends U> f) {
        return TraversableModule.minBy(this, f);
    }

    /**
     * Folds the elements with {@code combine}, starting from {@code zero}, which must be its neutral element.
     * The elements are combined in this Set's iteration order, which a {@code HashSet} does not define: {@code combine} should be associative and commutative for the result to be independent of it.
     *
     * @param zero    the neutral element of {@code combine}
     * @param combine combines two elements
     * @return the folded result, {@code zero} on an empty Set
     * @throws NullPointerException if {@code combine} is null
     */
    default T fold(T zero, BiFunction<? super T, ? super T, ? extends T> combine) {
        Objects.requireNonNull(combine, "combine is null");
        return foldLeft(zero, combine);
    }

    /**
     * Combines the elements with {@code op}, each result with the next element. The elements are combined in this Set's iteration order, which a {@code HashSet} does not define: {@code op} should be associative and commutative for the result to be independent of it.
     *
     * @param op combines two elements
     * @return the combined result
     * @throws NoSuchElementException if this Set is empty
     * @throws NullPointerException   if {@code op} is null
     */
    default T reduce(BiFunction<? super T, ? super T, ? extends T> op) {
        return TraversableModule.reduceLeft(this, op);
    }

    /**
     * {@link #reduce(BiFunction)} as an {@code Option}: {@code None} on an empty Set.
     *
     * @param op combines two elements
     * @return {@code Some(result)}, or {@code None} if this Set is empty
     * @throws NullPointerException if {@code op} is null
     */
    default Option<T> reduceOption(BiFunction<? super T, ? super T, ? extends T> op) {
        return TraversableModule.reduceLeftOption(this, op);
    }

    /**
     * The only element.
     *
     * @return the element
     * @throws NoSuchElementException if this Set is empty or has more than one element
     */
    default T single() {
        return TraversableModule.single(this);
    }

    /**
     * The only element as an {@code Option}.
     *
     * @return {@code Some(element)} if there is exactly one element, {@code None} otherwise
     */
    default Option<T> singleOption() {
        return TraversableModule.singleOption(this);
    }

    /**
     * Arranges the elements by a key that must be unique: {@code Some} of the map from each key to its element,
     * or {@code None} as soon as two elements share a key. The same as {@code groupBy(getKey)} when every group is
     * a singleton.
     *
     * @param getKey the key of an element
     * @param <K>  the key type
     * @return {@code Some(map)} if the keys are unique, {@code None} otherwise
     * @throws NullPointerException if {@code getKey} is null
     */
    default <K extends @Nullable Object> Option<Map<K, T>> arrangeBy(Function<? super T, ? extends K> getKey) {
        Objects.requireNonNull(getKey, "getKey is null");
        return TraversableModule.arrangeBy(groupBy(getKey));
    }

    /**
     * The sum of the elements, which must be {@link Number}s: {@code Byte}, {@code Short}, {@code Integer} and
     * {@code Long} are summed as a {@code long}, {@code BigInteger} and {@code BigDecimal} with their own
     * arithmetic, any other {@code Number} as a {@code double} with Neumaier compensation. The arithmetic is chosen
     * from the first element. {@code 0} on an empty Set.
     *
     * @return the sum
     * @throws UnsupportedOperationException if an element is not a {@code Number}
     */
    default Number sum() {
        return TraversableModule.sum(this);
    }

    /**
     * The product of the elements, which must be {@link Number}s: {@code Byte}, {@code Short}, {@code Integer} and
     * {@code Long} are multiplied as a {@code long}, {@code BigInteger} and {@code BigDecimal} with their own
     * arithmetic, any other {@code Number} as a {@code double}. The arithmetic is chosen from the first element.
     * {@code 1} on an empty Set.
     *
     * @return the product
     * @throws UnsupportedOperationException if an element is not a {@code Number}
     */
    default Number product() {
        return TraversableModule.product(this);
    }

    /**
     * The average of the elements, which must be {@link Number}s, summed as {@code double}s with Neumaier
     * compensation.
     *
     * @return {@code Some(average)} if there is an element, {@code None} otherwise
     * @throws UnsupportedOperationException if an element is not a {@code Number}
     */
    default Option<Double> average() {
        return TraversableModule.average(this);
    }

    /**
     * Collects the elements with {@code collector}, as {@code stream().collect(collector)} does.
     *
     * @param <A>       the collector's accumulation type
     * @param <R>       the result type
     * @param collector the collector
     * @return the collected result
     */
    default <R extends @Nullable Object, A extends @Nullable Object> R collect(Collector<? super T, A, R> collector) {
        return stream().collect(collector);
    }

    /**
     * Collects the elements with a supplier, an accumulator and a combiner, as
     * {@code stream().collect(supplier, accumulator, combiner)} does.
     *
     * @param <R>         the result type
     * @param supplier    makes a new result container
     * @param accumulator adds an element to a container
     * @param combiner    merges two containers
     * @return the collected result
     */
    default <R extends @Nullable Object> R collect(Supplier<R> supplier, BiConsumer<R, ? super T> accumulator, BiConsumer<R, R> combiner) {
        return stream().collect(supplier, accumulator, combiner);
    }

    /**
     * The elements copied into a new mutable {@link java.util.Collection} that {@code factory} makes for the given
     * capacity, in this Set's order: {@code toJavaCollection(java.util.LinkedHashSet::new)}.
     *
     * @param factory makes an empty mutable collection with the given initial capacity
     * @param <C>     the collection type
     * @return the new collection, filled
     * @throws NullPointerException if {@code factory} is null
     */
    default <C extends java.util.Collection<T>> C toJavaCollection(Function<Integer, C> factory) {
        return TraversableModule.toJavaCollection(this, factory);
    }

    /**
     * The elements copied into a new {@link java.util.ArrayList}, in this Set's order.
     *
     * @return the new list
     */
    default java.util.List<T> toJavaList() {
        return TraversableModule.toJavaCollection(this, ArrayList::new, 10);
    }

    /**
     * The elements copied into a new mutable {@link java.util.List} that {@code factory} makes for the given
     * capacity, in this Set's order: {@code toJavaList(capacity -> new java.util.LinkedList<>())}.
     *
     * @param factory makes an empty mutable list with the given initial capacity
     * @param <LIST>  the list type
     * @return the new list, filled
     * @throws NullPointerException if {@code factory} is null
     */
    default <LIST extends java.util.List<T>> LIST toJavaList(Function<Integer, LIST> factory) {
        return TraversableModule.toJavaCollection(this, factory);
    }

    /**
     * The elements as the entries of a new {@link java.util.HashMap}, each mapped to a key and a value by
     * {@code f}; of two entries with the same key, the later one in this Set's order wins.
     *
     * @param f   the entry an element becomes
     * @param <K> the key type
     * @param <V> the value type
     * @return the new map
     * @throws NullPointerException if {@code f} is null
     */
    default <K extends @Nullable Object, V extends @Nullable Object> java.util.Map<K, V> toJavaMap(Function<? super T, ? extends Tuple2<? extends K, ? extends V>> f) {
        return TraversableModule.toJavaMap(this, java.util.HashMap::new, f);
    }

    /**
     * The elements as the entries of a new mutable {@link java.util.Map} that {@code factory} makes, each mapped
     * to a key by {@code keyMapper} and to a value by {@code valueMapper}; of two entries with the same key, the
     * later one in this Set's order wins.
     *
     * @param factory     makes an empty mutable map
     * @param keyMapper   the key of an element
     * @param valueMapper the value of an element
     * @param <K>         the key type
     * @param <V>         the value type
     * @param <MAP>       the map type
     * @return the new map, filled
     * @throws NullPointerException if an argument is null
     */
    default <K extends @Nullable Object, V extends @Nullable Object, MAP extends java.util.Map<K, V>> MAP toJavaMap(Supplier<MAP> factory, Function<? super T, ? extends K> keyMapper, Function<? super T, ? extends V> valueMapper) {
        return TraversableModule.toJavaMap(this, factory, TraversableModule.entryMapper(keyMapper, valueMapper));
    }

    /**
     * The elements as the entries of a new mutable {@link java.util.Map} that {@code factory} makes, each mapped
     * to a key and a value by {@code f}; of two entries with the same key, the later one in this Set's order
     * wins.
     *
     * @param factory makes an empty mutable map
     * @param f       the entry an element becomes
     * @param <K>     the key type
     * @param <V>     the value type
     * @param <MAP>   the map type
     * @return the new map, filled
     * @throws NullPointerException if an argument is null
     */
    default <K extends @Nullable Object, V extends @Nullable Object, MAP extends java.util.Map<K, V>> MAP toJavaMap(Supplier<MAP> factory, Function<? super T, ? extends Tuple2<? extends K, ? extends V>> f) {
        return TraversableModule.toJavaMap(this, factory, f);
    }

    /**
     * The elements copied into a new mutable {@link java.util.Set} that {@code factory} makes for the given
     * capacity: {@code toJavaSet(capacity -> new java.util.TreeSet<>(Comparator.reverseOrder()))}.
     *
     * @param factory makes an empty mutable set with the given initial capacity
     * @param <SET>   the set type
     * @return the new set, filled
     * @throws NullPointerException if {@code factory} is null
     */
    default <SET extends java.util.Set<T>> SET toJavaSet(Function<Integer, SET> factory) {
        return TraversableModule.toJavaCollection(this, factory);
    }

    /**
     * A parallel {@link java.util.stream.Stream} over the elements, built on {@link #spliterator()}.
     *
     * @return a new parallel {@code java.util.stream.Stream}
     */
    default java.util.stream.Stream<T> toJavaParallelStream() {
        return StreamSupport.stream(spliterator(), true);
    }

    /**
     * The elements as the entries of a new {@link HashMap}, each mapped to a key by {@code keyMapper} and to a
     * value by {@code valueMapper}; of two entries with the same key, the later one in this Set's order wins.
     *
     * @param keyMapper   the key of an element
     * @param valueMapper the value of an element
     * @param <K>         the key type
     * @param <V>         the value type
     * @return the new map
     * @throws NullPointerException if an argument is null
     */
    default <K extends @Nullable Object, V extends @Nullable Object> Map<K, V> toMap(Function<? super T, ? extends K> keyMapper, Function<? super T, ? extends V> valueMapper) {
        return toMap(TraversableModule.entryMapper(keyMapper, valueMapper));
    }

    /**
     * The elements as the entries of a new {@link HashMap}, each mapped to a key and a value by {@code f}; of two
     * entries with the same key, the later one in this Set's order wins.
     *
     * @param f   the entry an element becomes
     * @param <K> the key type
     * @param <V> the value type
     * @return the new map
     * @throws NullPointerException if {@code f} is null
     */
    default <K extends @Nullable Object, V extends @Nullable Object> Map<K, V> toMap(Function<? super T, ? extends Tuple2<? extends K, ? extends V>> f) {
        final Function<Iterable<Tuple2<? extends K, ? extends V>>, Map<K, V>> ofAll = HashMap::ofEntries;
        return TraversableModule.toMap(this, HashMap.empty(), ofAll, f);
    }

    /**
     * The elements as the entries of a new {@link LinkedHashMap}, in this Set's order, each mapped to a key by
     * {@code keyMapper} and to a value by {@code valueMapper}; of two entries with the same key, the later one
     * wins the value and the earlier one the position.
     *
     * @param keyMapper   the key of an element
     * @param valueMapper the value of an element
     * @param <K>         the key type
     * @param <V>         the value type
     * @return the new map
     * @throws NullPointerException if an argument is null
     */
    default <K extends @Nullable Object, V extends @Nullable Object> Map<K, V> toLinkedMap(Function<? super T, ? extends K> keyMapper, Function<? super T, ? extends V> valueMapper) {
        return toLinkedMap(TraversableModule.entryMapper(keyMapper, valueMapper));
    }

    /**
     * The elements as the entries of a new {@link LinkedHashMap}, in this Set's order, each mapped to a key
     * and a value by {@code f}; of two entries with the same key, the later one wins the value and the earlier one
     * the position.
     *
     * @param f   the entry an element becomes
     * @param <K> the key type
     * @param <V> the value type
     * @return the new map
     * @throws NullPointerException if {@code f} is null
     */
    default <K extends @Nullable Object, V extends @Nullable Object> Map<K, V> toLinkedMap(Function<? super T, ? extends Tuple2<? extends K, ? extends V>> f) {
        final Function<Iterable<Tuple2<? extends K, ? extends V>>, Map<K, V>> ofAll = LinkedHashMap::ofEntries;
        return TraversableModule.toMap(this, LinkedHashMap.empty(), ofAll, f);
    }

    /**
     * The elements as the entries of a new {@link TreeMap} in the natural order of the keys, each mapped to a key
     * by {@code keyMapper} and to a value by {@code valueMapper}; of two entries with the same key, the later one
     * in this Set's order wins.
     *
     * @param keyMapper   the key of an element
     * @param valueMapper the value of an element
     * @param <K>         the key type
     * @param <V>         the value type
     * @return the new map
     * @throws NullPointerException if an argument is null
     */
    default <K extends Comparable<? super K>, V extends @Nullable Object> SortedMap<K, V> toSortedMap(Function<? super T, ? extends K> keyMapper, Function<? super T, ? extends V> valueMapper) {
        return toSortedMap(TraversableModule.entryMapper(keyMapper, valueMapper));
    }

    /**
     * The elements as the entries of a new {@link TreeMap} in the natural order of the keys, each mapped to a key
     * and a value by {@code f}; of two entries with the same key, the later one in this Set's order wins.
     *
     * @param f   the entry an element becomes
     * @param <K> the key type
     * @param <V> the value type
     * @return the new map
     * @throws NullPointerException if {@code f} is null
     */
    default <K extends Comparable<? super K>, V extends @Nullable Object> SortedMap<K, V> toSortedMap(Function<? super T, ? extends Tuple2<? extends K, ? extends V>> f) {
        Objects.requireNonNull(f, "f is null");
        return toSortedMap(Comparator.naturalOrder(), f);
    }

    /**
     * The elements as the entries of a new {@link TreeMap} ordered by {@code comparator}, each mapped to a key by
     * {@code keyMapper} and to a value by {@code valueMapper}; of two entries with the same key, the later one in
     * this Set's order wins.
     *
     * @param comparator  the order of the keys
     * @param keyMapper   the key of an element
     * @param valueMapper the value of an element
     * @param <K>         the key type
     * @param <V>         the value type
     * @return the new map
     * @throws NullPointerException if an argument is null
     */
    default <K extends @Nullable Object, V extends @Nullable Object> SortedMap<K, V> toSortedMap(Comparator<? super K> comparator, Function<? super T, ? extends K> keyMapper, Function<? super T, ? extends V> valueMapper) {
        return toSortedMap(comparator, TraversableModule.entryMapper(keyMapper, valueMapper));
    }

    /**
     * The elements as the entries of a new {@link TreeMap} ordered by {@code comparator}, each mapped to a key and
     * a value by {@code f}; of two entries with the same key, the later one in this Set's order wins.
     *
     * @param comparator the order of the keys
     * @param f          the entry an element becomes
     * @param <K>        the key type
     * @param <V>        the value type
     * @return the new map
     * @throws NullPointerException if an argument is null
     */
    default <K extends @Nullable Object, V extends @Nullable Object> SortedMap<K, V> toSortedMap(Comparator<? super K> comparator, Function<? super T, ? extends Tuple2<? extends K, ? extends V>> f) {
        Objects.requireNonNull(comparator, "comparator is null");
        final Function<Iterable<Tuple2<? extends K, ? extends V>>, SortedMap<K, V>> ofAll = t -> TreeMap.ofEntries(comparator, t);
        return TraversableModule.toMap(this, TreeMap.empty(comparator), ofAll, f);
    }

    /**
     * The elements as a {@link Queue}, in this Set's order.
     *
     * @return a {@code Queue} of the elements
     */
    default Queue<T> toQueue() {
        return TraversableModule.toTraversable(this, Queue.empty(), Queue::ofAll);
    }

    /**
     * The distinct elements as a {@link LinkedHashSet}, in this Set's order.
     *
     * @return a {@code LinkedHashSet} of the elements
     */
    default Set<T> toLinkedSet() {
        return TraversableModule.toTraversable(this, LinkedHashSet.empty(), LinkedHashSet::ofAll);
    }

    /**
     * The distinct elements as a {@link TreeSet} in their natural order; a {@code TreeSet} returns itself.
     *
     * @return a {@code TreeSet} of the elements
     * @throws ClassCastException if the elements are not {@link Comparable}
     */
    default SortedSet<T> toSortedSet() {
        return TraversableModule.toSortedSet(this);
    }

    /**
     * The distinct elements as a {@link TreeSet} ordered by {@code comparator}.
     *
     * @param comparator the order
     * @return a {@code TreeSet} of the elements
     * @throws NullPointerException if {@code comparator} is null
     */
    default SortedSet<T> toSortedSet(Comparator<? super T> comparator) {
        Objects.requireNonNull(comparator, "comparator is null");
        return TraversableModule.toTraversable(this, TreeSet.empty(comparator), values -> TreeSet.ofAll(comparator, values));
    }

    /**
     * The elements as a {@link Stream}, in this Set's order.
     *
     * @return a {@code Stream} of the elements
     */
    default Stream<T> toStream() {
        return TraversableModule.toTraversable(this, Stream.empty(), Stream::ofAll);
    }
}
