package dev.zazr.collection.internal;

import dev.zazr.Tuple;
import dev.zazr.Tuple2;
import dev.zazr.collection.HashSet;
import dev.zazr.collection.LazyList;
import dev.zazr.collection.LinkedHashMap;
import dev.zazr.collection.LinkedHashSet;
import dev.zazr.collection.List;
import dev.zazr.collection.Map;
import dev.zazr.collection.NonEmptyMap;
import dev.zazr.collection.NonEmptySet;
import dev.zazr.collection.NonEmptySortedMap;
import dev.zazr.collection.NonEmptySortedSet;
import dev.zazr.collection.NonEmptyVector;
import dev.zazr.collection.Queue;
import dev.zazr.collection.Set;
import dev.zazr.collection.SortedMap;
import dev.zazr.collection.SortedSet;
import dev.zazr.collection.Traversable;
import dev.zazr.collection.Vector;
import dev.zazr.control.Option;
import java.util.*;
import java.util.function.*;
import org.jspecify.annotations.Nullable;

/**
 * Internal class, containing helpers.
 *
 * @author Daniel Dietrich
 */
public final class Collections {

    // checks, if the *elements* of the given iterables are equal
    static boolean areEqual(Iterable<?> iterable1, Iterable<?> iterable2) {
        java.util.Iterator<?> iter1 = iterable1.iterator();
        java.util.Iterator<?> iter2 = iterable2.iterator();
        while (iter1.hasNext() && iter2.hasNext()) {
            if (!Objects.equals(iter1.next(), iter2.next())) {
                return false;
            }
        }
        return iter1.hasNext() == iter2.hasNext();
    }

    @SuppressWarnings("unchecked")
    public static <K extends @Nullable Object, V extends @Nullable Object> boolean equals(
            Map<K, V> source, @Nullable Object object) {
        if (source == object) {
            return true;
        } else if (source != null && object instanceof Map) {
            Map<K, V> map = (Map<K, V>) object;
            if (source.size() != map.size()) {
                return false;
            } else {
                try {
                    return source.forAll(map::contains);
                } catch (ClassCastException e) {
                    return false;
                }
            }
        } else {
            return false;
        }
    }

    public static <V extends @Nullable Object> boolean equals(Vector<V> source, @Nullable Object object) {
        return equalsSequence(source, object);
    }

    public static <V extends @Nullable Object> boolean equals(List<V> source, @Nullable Object object) {
        return equalsSequence(source, object);
    }

    public static <V extends @Nullable Object> boolean equals(Queue<V> source, @Nullable Object object) {
        return equalsSequence(source, object);
    }

    public static <V extends @Nullable Object> boolean equals(LazyList<V> source, @Nullable Object object) {
        return equalsSequence(source, object);
    }

    // the sequence types are equal to each other when their elements are equal in order
    private static boolean equalsSequence(Traversable<?> source, @Nullable Object object) {
        if (object == source) {
            return true;
        } else if (object instanceof Traversable<?> sequence && isSequence(sequence)) {
            // a LazyList's size walks all of it, and never returns on an infinite one: the element by element
            // comparison stops at the first difference or at the end of the shorter side instead
            boolean sizesKnown = !(source instanceof LazyList) && !(sequence instanceof LazyList);
            return (!sizesKnown || sequence.size() == source.size()) && areEqual(source, sequence);
        } else {
            return false;
        }
    }

    // the ordered sequence types, equal to each other element by element in order
    static boolean isSequence(@Nullable Object object) {
        return object instanceof Vector
                || object instanceof List
                || object instanceof Queue
                || object instanceof LazyList;
    }

    @SuppressWarnings("unchecked")
    public static <V extends @Nullable Object> boolean equals(Set<V> source, @Nullable Object object) {
        if (source == object) {
            return true;
        } else if (source != null && object instanceof Set) {
            Set<V> set = (Set<V>) object;
            if (source.size() != set.size()) {
                return false;
            } else {
                try {
                    return source.forAll(set::contains);
                } catch (ClassCastException e) {
                    return false;
                }
            }
        } else {
            return false;
        }
    }

    public static <T extends @Nullable Object> Iterator<T> fill(int n, Supplier<? extends T> supplier) {
        Objects.requireNonNull(supplier, "supplier is null");
        return tabulate(n, ignored -> supplier.get());
    }

    public static <T extends @Nullable Object> Iterator<T> fillObject(int n, T element) {
        if (n <= 0) {
            return Iterator.empty();
        } else {
            return Iterator.continually(element).take(n);
        }
    }

    public static <C extends Traversable<T>, T extends @Nullable Object> C fill(
            int n, Supplier<? extends T> s, C empty, Function<T[], C> of) {
        Objects.requireNonNull(s, "s is null");
        Objects.requireNonNull(empty, "empty is null");
        Objects.requireNonNull(of, "of is null");
        return tabulate(n, anything -> s.get(), empty, of);
    }

    public static <C extends Traversable<T>, T extends @Nullable Object> C fillObject(
            int n, T element, C empty, Function<T[], C> of) {
        Objects.requireNonNull(empty, "empty is null");
        Objects.requireNonNull(of, "of is null");
        if (n <= 0) {
            return empty;
        } else {
            @SuppressWarnings("unchecked")
            T[] elements = (T[]) new Object[n];
            Arrays.fill(elements, element);
            return of.apply(elements);
        }
    }

    public static <T extends @Nullable Object, C extends @Nullable Object, R extends Iterable<T>> Map<C, R> groupBy(
            Traversable<T> source,
            Function<? super T, ? extends C> classifier,
            Function<? super Iterable<T>, R> mapper,
            String nullResult) {
        Objects.requireNonNull(classifier, "classifier is null");
        Objects.requireNonNull(mapper, "mapper is null");
        @SuppressWarnings("Var")
        Map<C, R> results = LinkedHashMap.empty();
        for (java.util.Map.Entry<? extends C, Collection<T>> entry : groupBy(source, classifier, nullResult)) {
            results = results.put(entry.getKey(), mapper.apply(entry.getValue()));
        }
        return results;
    }

    private static <T extends @Nullable Object, C extends @Nullable Object>
            java.util.Set<java.util.Map.Entry<C, Collection<T>>> groupBy(
                    Traversable<T> source, Function<? super T, ? extends C> classifier, String nullResult) {
        java.util.Map<C, Collection<T>> results =
                new java.util.LinkedHashMap<>(isTraversableAgain(source) ? source.size() : 16);
        for (T value : source) {
            C key = Objects.requireNonNull(classifier.apply(value), nullResult);
            results.computeIfAbsent(key, k -> new ArrayList<>()).add(value);
        }
        return results.entrySet();
    }

    /// The elements of `source` grouped by `key`, each element replaced by what `value` returns, in a map ordered
    /// by the first occurrence of each key; each group keeps the iteration order of `source`. `method` names the
    /// public method in the message of a function that returns null (`Vector.groupMap: key returned null`).
    public static <T extends @Nullable Object, K extends @Nullable Object, U extends @Nullable Object>
            java.util.LinkedHashMap<K, ArrayList<U>> groupMap(
                    Iterable<T> source,
                    Function<? super T, ? extends K> key,
                    Function<? super T, ? extends U> value,
                    String method) {
        Objects.requireNonNull(key, "key is null");
        Objects.requireNonNull(value, "value is null");
        java.util.LinkedHashMap<K, ArrayList<U>> groups = new java.util.LinkedHashMap<>(initialCapacity(source));
        for (T element : source) {
            K k = key.apply(element);
            if (k == null) {
                throw new NullPointerException(method + ": key returned null");
            }
            U v = value.apply(element);
            if (v == null) {
                throw new NullPointerException(method + ": value returned null");
            }
            groups.computeIfAbsent(k, ignored -> new ArrayList<>()).add(v);
        }
        return groups;
    }

    /// [#groupMap(Iterable, Function, Function, String)] as a [LinkedHashMap] whose groups `group` builds.
    public static <
                    T extends @Nullable Object,
                    K extends @Nullable Object,
                    U extends @Nullable Object,
                    R extends @Nullable Object>
            Map<K, R> groupMap(
                    Iterable<T> source,
                    Function<? super T, ? extends K> key,
                    Function<? super T, ? extends U> value,
                    Function<? super Iterable<U>, ? extends R> group,
                    String method) {
        LinkedHashMap.Builder<K, R> result = LinkedHashMap.newBuilder();
        for (java.util.Map.Entry<K, ArrayList<U>> entry :
                Collections.<T, K, U>groupMap(source, key, value, method).entrySet()) {
            result.put(entry.getKey(), group.apply(entry.getValue()));
        }
        return result.result();
    }

    /// The elements of `source` grouped by `key`, each group's values (what `value` returns for its elements, in
    /// the iteration order of `source`) combined from the left with `reduce`, in a map ordered by the first
    /// occurrence of each key. `method` names the public method in the message of a function that returns null.
    public static <T extends @Nullable Object, K extends @Nullable Object, U extends @Nullable Object>
            java.util.LinkedHashMap<K, U> groupMapReduce(
                    Iterable<T> source,
                    Function<? super T, ? extends K> key,
                    Function<? super T, ? extends U> value,
                    BiFunction<? super U, ? super U, ? extends U> reduce,
                    String method) {
        Objects.requireNonNull(key, "key is null");
        Objects.requireNonNull(value, "value is null");
        Objects.requireNonNull(reduce, "reduce is null");
        java.util.LinkedHashMap<K, U> results = new java.util.LinkedHashMap<>(initialCapacity(source));
        // one function for the whole call; a null result must not reach merge, which would remove the key
        BiFunction<U, U, U> combine = (previous, next) -> {
            U combined = reduce.apply(previous, next);
            if (combined == null) {
                throw new NullPointerException(method + ": reduce returned null");
            }
            return combined;
        };
        for (T element : source) {
            K k = key.apply(element);
            if (k == null) {
                throw new NullPointerException(method + ": key returned null");
            }
            U v = value.apply(element);
            if (v == null) {
                throw new NullPointerException(method + ": value returned null");
            }
            results.merge(k, v, combine);
        }
        return results;
    }

    /// [#groupMapReduce(Iterable, Function, Function, BiFunction, String)] as a [LinkedHashMap].
    public static <T extends @Nullable Object, K extends @Nullable Object, U extends @Nullable Object>
            Map<K, U> groupMapReduceToMap(
                    Iterable<T> source,
                    Function<? super T, ? extends K> key,
                    Function<? super T, ? extends U> value,
                    BiFunction<? super U, ? super U, ? extends U> reduce,
                    String method) {
        LinkedHashMap.Builder<K, U> result = LinkedHashMap.newBuilder();
        for (java.util.Map.Entry<K, U> entry : Collections.<T, K, U>groupMapReduce(source, key, value, reduce, method)
                .entrySet()) {
            result.put(entry.getKey(), entry.getValue());
        }
        return result.result();
    }

    // the capacity of a JDK map that receives at most one entry per element of source
    private static int initialCapacity(Iterable<?> source) {
        int known = knownSize(source);
        return known >= 0 ? known : 16;
    }

    /// The size of `iterable` when it is stored, so that reading it is O(1) and computes nothing: a [Vector], a set,
    /// a map or a non-empty collection. -1 for everything else, whose size takes a walk ([List], [Queue],
    /// [LazyList]), may never be known (a one-shot `Iterable`), or is not trusted to be cheap (a JDK collection,
    /// which may be a view of a [LazyList]).
    public static int knownSize(Iterable<?> iterable) {
        return switch (iterable) {
            case List<?> _, Queue<?> _, LazyList<?> _ -> -1;
            case Traversable<?> traversable -> traversable.size();
            case NonEmptyVector<?> vector -> vector.size();
            case NonEmptySet<?> set -> set.size();
            case NonEmptySortedSet<?> set -> set.size();
            case NonEmptyMap<?, ?> map -> map.size();
            case NonEmptySortedMap<?, ?> map -> map.size();
            default -> -1;
        };
    }

    /// The sign of the number of elements of `iterator` minus `otherSize`, as -1, 0 or 1. It reads at most
    /// `otherSize + 1` elements.
    public static int sizeCompare(java.util.Iterator<?> iterator, int otherSize) {
        if (otherSize < 0) {
            return 1;
        }
        @SuppressWarnings("Var")
        int count = 0;
        while (iterator.hasNext()) {
            if (count == otherSize) {
                return 1;
            }
            iterator.next();
            count++;
        }
        return count == otherSize ? 0 : -1;
    }

    /// The sign of the size of `self` minus the size of `that`, where `thisKnownSize` is the size of `self` if it
    /// is stored, -1 otherwise: a stored size is compared with the other collection, which counts its elements only
    /// up to it; when neither size is stored, the two are walked side by side until the shorter one ends.
    public static int sizeCompare(Iterable<?> self, int thisKnownSize, Iterable<?> that) {
        Objects.requireNonNull(that, "that is null");
        int thatKnownSize = knownSize(that);
        if (thatKnownSize >= 0) {
            return sizeCompareWith(self, thatKnownSize);
        } else if (thisKnownSize >= 0) {
            return -sizeCompareWith(that, thisKnownSize);
        } else {
            java.util.Iterator<?> these = self.iterator();
            java.util.Iterator<?> those = that.iterator();
            while (these.hasNext() && those.hasNext()) {
                these.next();
                those.next();
            }
            return Boolean.compare(these.hasNext(), those.hasNext());
        }
    }

    // the iterable's own sizeCompare where it has one, so that a List walks its cells and a LazyList its computed ones
    private static int sizeCompareWith(Iterable<?> iterable, int otherSize) {
        return switch (iterable) {
            case Traversable<?> traversable -> traversable.sizeCompare(otherSize);
            case NonEmptyVector<?> vector -> vector.sizeCompare(otherSize);
            case NonEmptySet<?> set -> set.sizeCompare(otherSize);
            case NonEmptySortedSet<?> set -> set.sizeCompare(otherSize);
            case NonEmptyMap<?, ?> map -> map.sizeCompare(otherSize);
            case NonEmptySortedMap<?, ?> map -> map.sizeCompare(otherSize);
            default -> sizeCompare(iterable.iterator(), otherSize);
        };
    }

    // hashes the elements respecting their order
    public static int hashOrdered(Iterable<?> iterable) {
        return hash(iterable, (acc, hash) -> acc * 31 + hash);
    }

    // hashes the elements regardless of their order
    public static int hashUnordered(Iterable<?> iterable) {
        return hash(iterable, (acc, hash) -> acc + hash);
    }

    private static int hash(Iterable<?> iterable, IntBinaryOperator accumulator) {
        if (iterable == null) {
            return 0;
        } else {
            @SuppressWarnings("Var")
            int hashCode = 1;
            for (Object o : iterable) {
                hashCode = accumulator.applyAsInt(hashCode, Objects.hashCode(o));
            }
            return hashCode;
        }
    }

    public static Option<Integer> indexOption(int index) {
        return index >= 0 ? Option.some(index) : Option.none();
    }

    // @param iterable may not be null
    public static boolean isEmpty(Iterable<?> iterable) {
        return iterable instanceof Traversable && ((Traversable<?>) iterable).isEmpty()
                || iterable instanceof Collection && ((Collection<?>) iterable).isEmpty()
                || !iterable.iterator().hasNext();
    }

    // Every Traversable and every java.util.Collection can be walked again; a bare Iterable may be one-shot (an
    // Iterator, typically), and so is walked once into a List when a second pass is needed.
    public static boolean isTraversableAgain(Iterable<?> iterable) {
        return (iterable instanceof Collection) || (iterable instanceof Traversable);
    }

    // A size that is known without walking the elements: a LazyList may be infinite.
    static boolean hasDefiniteSize(Traversable<?> traversable) {
        return !(traversable instanceof LazyList);
    }

    // sliding/grouped windows: both the size and the step must be positive
    public static void checkWindow(int size, int step) {
        if (size < 1 || step < 1) {
            throw new IllegalArgumentException("size (" + size + ") and step (" + step + ") must both be positive");
        }
    }

    // The characteristics a Traversable reports through Spliterator: what the type guarantees about its elements.
    static int spliteratorCharacteristics(Traversable<?> traversable) {
        int distinct = traversable instanceof Set || traversable instanceof Map ? Spliterator.DISTINCT : 0;
        // a SortedMap orders its keys, not its entries, so it is ORDERED but not SORTED
        int sorted = traversable instanceof SortedSet ? Spliterator.SORTED | Spliterator.ORDERED : 0;
        int ordered = isSequence(traversable)
                        || traversable instanceof SortedMap
                        || traversable instanceof LinkedHashSet
                        || traversable instanceof LinkedHashMap
                ? Spliterator.ORDERED
                : 0;
        int sized = hasDefiniteSize(traversable) ? Spliterator.SIZED | Spliterator.SUBSIZED : 0;
        return Spliterator.IMMUTABLE | distinct | sorted | ordered | sized;
    }

    // The spliterator of a Traversable: sized when the size is known without a walk, and reporting the comparator of a
    // SortedSet (null for the natural order, as Spliterator specifies), so that java.util.stream.Stream.sorted() sorts
    // a set ordered otherwise instead of skipping the sort.
    public static <T extends @Nullable Object> Spliterator<T> spliterator(Traversable<T> traversable) {
        int characteristics = spliteratorCharacteristics(traversable);
        Spliterator<T> spliterator = (characteristics & Spliterator.SIZED) != 0
                ? Spliterators.spliterator(traversable.iterator(), traversable.size(), characteristics)
                : Spliterators.spliteratorUnknownSize(traversable.iterator(), characteristics);
        if (traversable instanceof SortedSet<?> sortedSet && !(sortedSet.comparator() instanceof NaturalComparator)) {
            @SuppressWarnings("unchecked")
            Comparator<? super T> comparator = (Comparator<? super T>) sortedSet.comparator();
            return new SortedSpliterator<>(spliterator, comparator);
        }
        return spliterator;
    }

    static final class SortedSpliterator<T extends @Nullable Object> implements Spliterator<T> {

        private final Spliterator<T> delegate;
        private final Comparator<? super T> comparator;

        SortedSpliterator(Spliterator<T> delegate, Comparator<? super T> comparator) {
            this.delegate = delegate;
            this.comparator = comparator;
        }

        @Override
        public boolean tryAdvance(Consumer<? super T> action) {
            return delegate.tryAdvance(action);
        }

        @Override
        public void forEachRemaining(Consumer<? super T> action) {
            delegate.forEachRemaining(action);
        }

        @Override
        public @Nullable Spliterator<T> trySplit() {
            Spliterator<T> prefix = delegate.trySplit();
            return prefix == null ? null : new SortedSpliterator<>(prefix, comparator);
        }

        @Override
        public long estimateSize() {
            return delegate.estimateSize();
        }

        @Override
        public long getExactSizeIfKnown() {
            return delegate.getExactSizeIfKnown();
        }

        @Override
        public int characteristics() {
            return delegate.characteristics();
        }

        @Override
        public Comparator<? super T> getComparator() {
            return comparator;
        }
    }

    public static <T extends @Nullable Object> T last(Traversable<T> source) {
        if (source.isEmpty()) {
            throw new NoSuchElementException("last of empty " + source);
        } else {
            java.util.Iterator<T> it = source.iterator();
            @SuppressWarnings("Var")
            T result = it.next();
            while (it.hasNext()) {
                result = it.next();
            }
            return result;
        }
    }

    @SuppressWarnings("unchecked")
    public static <
                    K extends @Nullable Object,
                    V extends @Nullable Object,
                    K2 extends @Nullable Object,
                    U extends Map<K2, V>>
            U mapKeys(
                    Map<K, V> source,
                    U zero,
                    Function<? super K, ? extends K2> keyMapper,
                    BiFunction<? super V, ? super V, ? extends V> valueMerge) {
        Objects.requireNonNull(zero, "zero is null");
        Objects.requireNonNull(keyMapper, "keyMapper is null");
        Objects.requireNonNull(valueMerge, "valueMerge is null");
        return source.foldLeft(zero, (acc, entry) -> {
            K2 k2 = Objects.requireNonNull(keyMapper.apply(entry._1()), "mapKeys: key is null");
            V v2 = entry._2();
            V v1 = Maps.getOrAbsent(acc, k2);
            V v = v1 != Maps.ABSENT ? valueMerge.apply(v1, v2) : v2;
            return (U) acc.put(k2, v);
        });
    }

    public static <C extends Traversable<T>, T extends @Nullable Object> Tuple2<C, C> partition(
            C collection, Function<Iterable<T>, C> creator, Predicate<? super T> predicate) {
        Objects.requireNonNull(predicate, "predicate is null");
        java.util.List<T> left = new java.util.ArrayList<>();
        java.util.List<T> right = new java.util.ArrayList<>();
        for (T element : collection) {
            (predicate.test(element) ? left : right).add(element);
        }
        return Tuple.of(creator.apply(left), creator.apply(right));
    }

    /**
     * The first element of every key occurring more than once among {@code elements}, in order of first occurrence
     * (the {@code duplicatesBy} contract): one pass with a {@code LinkedHashMap} of first occurrences and a
     * {@code HashSet} of the keys seen again, the key computed once per element, then one pass over the distinct keys.
     * An element is never null, so {@code putIfAbsent} tells a first occurrence from a repeat even for a null key.
     *
     * @return the duplicated elements; empty (and immutable) when every key is distinct
     */
    public static <T extends @Nullable Object, K extends @Nullable Object> java.util.List<T> duplicatesBy(
            Iterable<? extends T> elements, Function<? super T, ? extends K> keyExtractor) {
        java.util.LinkedHashMap<K, T> first = new java.util.LinkedHashMap<>();
        java.util.HashSet<K> duplicated = new java.util.HashSet<>();
        for (T element : elements) {
            K key = keyExtractor.apply(element);
            if (first.putIfAbsent(key, element) != null) {
                duplicated.add(key);
            }
        }
        if (duplicated.isEmpty()) {
            return java.util.List.of();
        }
        java.util.List<T> result = new java.util.ArrayList<>(duplicated.size());
        for (java.util.Map.Entry<K, T> entry : first.entrySet()) {
            if (duplicated.contains(entry.getKey())) {
                result.add(entry.getValue());
            }
        }
        return result;
    }

    /** A collection's own {@code filter}, handed to the helpers below: {@code Traversable} does not declare one. */
    @FunctionalInterface
    public interface Filter<T extends @Nullable Object, C> {
        C apply(Predicate<? super T> predicate);
    }

    public static <C extends Traversable<T>, T extends @Nullable Object> C removeAll(
            C source, Iterable<? extends T> elements, Filter<T, C> filter) {
        Objects.requireNonNull(elements, "elements is null");
        if (source.isEmpty()) {
            return source;
        } else {
            Set<T> removed = HashSet.ofAll(elements);
            return removed.isEmpty() ? source : filter.apply(e -> !removed.contains(e));
        }
    }

    public static <C extends Traversable<T>, T extends @Nullable Object> C reject(
            C source, Predicate<? super T> predicate, Filter<T, C> filter) {
        Objects.requireNonNull(predicate, "predicate is null");
        if (source.isEmpty()) {
            return source;
        } else {
            return filter.apply(predicate.negate());
        }
    }

    public static <C extends Traversable<T>, T extends @Nullable Object> C removeAll(
            C source, T element, Filter<T, C> filter) {
        if (source.isEmpty()) {
            return source;
        } else {
            return filter.apply(e -> !Objects.equals(e, element));
        }
    }

    public static <C extends Traversable<T>, T extends @Nullable Object> C retainAll(
            C source, Iterable<? extends T> elements, Filter<T, C> filter) {
        Objects.requireNonNull(elements, "elements is null");
        if (source.isEmpty()) {
            return source;
        } else {
            Set<T> retained = HashSet.ofAll(elements);
            return filter.apply(retained::contains);
        }
    }

    static <T extends @Nullable Object> Iterator<T> reverseIterator(Iterable<T> iterable) {
        if (iterable instanceof java.util.List) {
            return reverseListIterator((java.util.List<T>) iterable);
        } else if (iterable instanceof Vector) {
            Vector<T> vector = (Vector<T>) iterable;
            return new AbstractIterator<T>() {
                private int i = vector.size();

                @Override
                public boolean hasNext() {
                    return i > 0;
                }

                @Override
                public T getNext() {
                    return vector.get(--i);
                }
            };
        } else if (iterable instanceof Queue) {
            return Iterator.ofAll(((Queue<T>) iterable).reverse().iterator());
        } else if (iterable instanceof List) {
            return Iterator.ofAll(((List<T>) iterable).reverse());
        } else if (iterable instanceof LazyList) {
            return Iterator.ofAll(((LazyList<T>) iterable).reverse());
        } else {
            return Iterator.ofAll(List.<T>empty().pushAll(iterable));
        }
    }

    private static <T extends @Nullable Object> Iterator<T> reverseListIterator(java.util.List<T> list) {
        return new Iterator<T>() {
            private final java.util.ListIterator<T> delegate = list.listIterator(list.size());

            @Override
            public boolean hasNext() {
                return delegate.hasPrevious();
            }

            @Override
            public T next() {
                return delegate.previous();
            }
        };
    }

    public static <T extends @Nullable Object, U extends @Nullable Object, R extends Traversable<U>> R scanLeft(
            Iterable<? extends T> source,
            U zero,
            BiFunction<? super U, ? super T, ? extends U> operation,
            Function<Iterator<U>, R> finisher) {
        Objects.requireNonNull(operation, "operation is null");
        Iterator<U> iterator = Iterator.ofAll(source).scanLeft(zero, operation);
        return finisher.apply(iterator);
    }

    public static <T extends @Nullable Object, U extends @Nullable Object, R extends Traversable<U>> R scanRight(
            Traversable<? extends T> source,
            U zero,
            BiFunction<? super T, ? super U, ? extends U> operation,
            Function<Iterator<U>, R> finisher) {
        Objects.requireNonNull(operation, "operation is null");
        Iterator<? extends T> reversedElements = reverseIterator(source);
        return scanLeft(
                reversedElements, zero, (u, t) -> operation.apply(t, u), us -> finisher.apply(reverseIterator(us)));
    }

    public static <T extends @Nullable Object, S extends Traversable<T>> S shuffle(
            S source, Function<? super Iterable<T>, S> ofAll) {
        if (source.size() <= 1) {
            return source;
        }

        java.util.List<T> list = new ArrayList<>(source.asJava());
        java.util.Collections.shuffle(list);
        return ofAll.apply(list);
    }

    public static void subSequenceRangeCheck(int beginIndex, int endIndex, int length) {
        if (beginIndex < 0 || endIndex > length) {
            throw new IndexOutOfBoundsException(
                    "subSequence(" + beginIndex + ", " + endIndex + "), length = " + length);
        } else if (beginIndex > endIndex) {
            throw new IllegalArgumentException("subSequence(" + beginIndex + ", " + endIndex + ")");
        }
    }

    public static <T extends @Nullable Object> Iterator<T> tabulate(int n, Function<? super Integer, ? extends T> f) {
        Objects.requireNonNull(f, "f is null");
        if (n <= 0) {
            return Iterator.empty();
        } else {
            return new AbstractIterator<T>() {

                int i = 0;

                @Override
                public boolean hasNext() {
                    return i < n;
                }

                @Override
                protected T getNext() {
                    return f.apply(i++);
                }
            };
        }
    }

    public static <C extends Traversable<T>, T extends @Nullable Object> C tabulate(
            int n, Function<? super Integer, ? extends T> f, C empty, Function<T[], C> of) {
        Objects.requireNonNull(f, "f is null");
        Objects.requireNonNull(empty, "empty is null");
        Objects.requireNonNull(of, "of is null");
        if (n <= 0) {
            return empty;
        } else {
            @SuppressWarnings("unchecked")
            T[] elements = (T[]) new Object[n];
            for (int i = 0; i < n; i++) {
                elements[i] = f.apply(i);
            }
            return of.apply(elements);
        }
    }

    public static <T extends @Nullable Object, U extends Traversable<T>, V extends Traversable<U>> V transpose(
            V matrix, Function<Iterable<U>, V> rowFactory, Function<T[], U> columnFactory) {
        Objects.requireNonNull(matrix, "matrix is null");
        if (matrix.isEmpty() || (matrix.size() == 1 && matrix.iterator().next().size() <= 1)) {
            return matrix;
        } else {
            return transposeNonEmptyMatrix(matrix, rowFactory, columnFactory);
        }
    }

    private static <T extends @Nullable Object, U extends Traversable<T>, V extends Traversable<U>>
            V transposeNonEmptyMatrix(V matrix, Function<Iterable<U>, V> rowFactory, Function<T[], U> columnFactory) {
        int newHeight = matrix.iterator().next().size(), newWidth = matrix.size();
        @SuppressWarnings("unchecked")
        T[][] results = (T[][]) new Object[newHeight][newWidth];

        if (matrix.exists(r -> r.size() != newHeight)) {
            throw new IllegalArgumentException("the parameter `matrix` is invalid!");
        }

        @SuppressWarnings("Var")
        int rowIndex = 0;
        for (U row : matrix) {
            @SuppressWarnings("Var")
            int columnIndex = 0;
            for (T element : row) {
                results[columnIndex][rowIndex] = element;
                columnIndex++;
            }
            rowIndex++;
        }

        return rowFactory.apply(Iterator.of(results).map(columnFactory));
    }

    public static <T extends @Nullable Object> IterableWithSize<T> withSize(Iterable<? extends T> iterable) {
        return isTraversableAgain(iterable) ? withSizeTraversable(iterable) : withSizeTraversable(List.ofAll(iterable));
    }

    private static <T extends @Nullable Object> IterableWithSize<T> withSizeTraversable(
            Iterable<? extends T> iterable) {
        if (iterable instanceof Collection) {
            return new IterableWithSize<>(iterable, ((Collection<?>) iterable).size());
        } else {
            return new IterableWithSize<>(iterable, ((Traversable<?>) iterable).size());
        }
    }

    public static class IterableWithSize<T extends @Nullable Object> {
        private final Iterable<? extends T> iterable;
        private final int size;

        IterableWithSize(Iterable<? extends T> iterable, int size) {
            this.iterable = iterable;
            this.size = size;
        }

        java.util.Iterator<? extends T> iterator() {
            return iterable.iterator();
        }

        java.util.Iterator<? extends T> reverseIterator() {
            return Collections.reverseIterator(iterable);
        }

        int size() {
            return size;
        }

        public Object[] toArray() {
            if (iterable instanceof Collection<?>) {
                return ((Collection<? extends T>) iterable).toArray();
            } else {
                Object[] array = new Object[size];
                java.util.Iterator<? extends T> it = iterator();
                for (int i = 0; i < size; i++) {
                    array[i] = it.next();
                }
                return array;
            }
        }
    }
}
