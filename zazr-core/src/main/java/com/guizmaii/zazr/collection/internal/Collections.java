package com.guizmaii.zazr.collection.internal;

import com.guizmaii.zazr.Tuple;
import com.guizmaii.zazr.Tuple2;
import com.guizmaii.zazr.collection.HashSet;
import com.guizmaii.zazr.collection.LinkedHashMap;
import com.guizmaii.zazr.collection.LinkedHashSet;
import com.guizmaii.zazr.collection.List;
import com.guizmaii.zazr.collection.Map;
import com.guizmaii.zazr.collection.Queue;
import com.guizmaii.zazr.collection.Set;
import com.guizmaii.zazr.collection.SortedMap;
import com.guizmaii.zazr.collection.SortedSet;
import com.guizmaii.zazr.collection.Stream;
import com.guizmaii.zazr.collection.Traversable;
import com.guizmaii.zazr.collection.Vector;
import com.guizmaii.zazr.control.Option;
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
        final java.util.Iterator<?> iter1 = iterable1.iterator();
        final java.util.Iterator<?> iter2 = iterable2.iterator();
        while (iter1.hasNext() && iter2.hasNext()) {
            if (!Objects.equals(iter1.next(), iter2.next())) {
                return false;
            }
        }
        return iter1.hasNext() == iter2.hasNext();
    }

    @SuppressWarnings("unchecked")
    public static <K extends @Nullable Object, V extends @Nullable Object> boolean equals(Map<K, V> source, @Nullable Object object) {
        if (source == object) {
            return true;
        } else if (source != null && object instanceof Map) {
            final Map<K, V> map = (Map<K, V>) object;
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

    public static <V extends @Nullable Object> boolean equals(Stream<V> source, @Nullable Object object) {
        return equalsSequence(source, object);
    }

    // the sequence types are equal to each other when their elements are equal in order
    private static boolean equalsSequence(Traversable<?> source, @Nullable Object object) {
        if (object == source) {
            return true;
        } else if (object instanceof Traversable<?> sequence && isSequence(sequence)) {
            return sequence.size() == source.size() && areEqual(source, sequence);
        } else {
            return false;
        }
    }

    // the ordered sequence types, equal to each other element by element in order
    static boolean isSequence(@Nullable Object object) {
        return object instanceof Vector || object instanceof List || object instanceof Queue || object instanceof Stream;
    }

    @SuppressWarnings("unchecked")
    public static <V extends @Nullable Object> boolean equals(Set<V> source, @Nullable Object object) {
        if (source == object) {
            return true;
        } else if (source != null && object instanceof Set) {
            final Set<V> set = (Set<V>) object;
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

    public static <C extends Traversable<T>, T extends @Nullable Object> C fill(int n, Supplier<? extends T> s, C empty, Function<T[], C> of) {
        Objects.requireNonNull(s, "s is null");
        Objects.requireNonNull(empty, "empty is null");
        Objects.requireNonNull(of, "of is null");
        return tabulate(n, anything -> s.get(), empty, of);
    }

    public static <C extends Traversable<T>, T extends @Nullable Object> C fillObject(int n, T element, C empty, Function<T[], C> of) {
        Objects.requireNonNull(empty, "empty is null");
        Objects.requireNonNull(of, "of is null");
        if (n <= 0) {
            return empty;
        } else {
            @SuppressWarnings("unchecked")
            final T[] elements = (T[]) new Object[n];
            Arrays.fill(elements, element);
            return of.apply(elements);
        }
    }

    public static <T extends @Nullable Object, C extends @Nullable Object, R extends Iterable<T>> Map<C, R> groupBy(Traversable<T> source, Function<? super T, ? extends C> classifier, Function<? super Iterable<T>, R> mapper) {
        Objects.requireNonNull(classifier, "classifier is null");
        Objects.requireNonNull(mapper, "mapper is null");
        Map<C, R> results = LinkedHashMap.empty();
        for (java.util.Map.Entry<? extends C, Collection<T>> entry : groupBy(source, classifier)) {
            results = results.put(entry.getKey(), mapper.apply(entry.getValue()));
        }
        return results;

    }

    private static <T extends @Nullable Object, C extends @Nullable Object> java.util.Set<java.util.Map.Entry<C, Collection<T>>> groupBy(Traversable<T> source, Function<? super T, ? extends C> classifier) {
        final java.util.Map<C, Collection<T>> results = new java.util.LinkedHashMap<>(isTraversableAgain(source) ? source.size() : 16);
        for (T value : source) {
            final C key = Objects.requireNonNull(classifier.apply(value), "groupBy: key is null");
            results.computeIfAbsent(key, k -> new ArrayList<>()).add(value);
        }
        return results.entrySet();
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

    // A size that is known without walking the elements: a Stream may be infinite.
    static boolean hasDefiniteSize(Traversable<?> traversable) {
        return !(traversable instanceof Stream);
    }

    // sliding/grouped windows: both the size and the step must be positive
    public static void checkWindow(int size, int step) {
        if (size < 1 || step < 1) {
            throw new IllegalArgumentException("size (" + size + ") and step (" + step + ") must both be positive");
        }
    }

    // The characteristics a Traversable reports through Spliterator: what the type guarantees about its elements.
    static int spliteratorCharacteristics(Traversable<?> traversable) {
        int characteristics = Spliterator.IMMUTABLE;
        if (traversable instanceof Set || traversable instanceof Map) {
            characteristics |= Spliterator.DISTINCT;
        }
        // a SortedMap orders its keys, not its entries, so it is ORDERED but not SORTED
        if (traversable instanceof SortedSet) {
            characteristics |= (Spliterator.SORTED | Spliterator.ORDERED);
        }
        if (isSequence(traversable) || traversable instanceof SortedMap
                || traversable instanceof LinkedHashSet || traversable instanceof LinkedHashMap) {
            characteristics |= Spliterator.ORDERED;
        }
        if (hasDefiniteSize(traversable)) {
            characteristics |= (Spliterator.SIZED | Spliterator.SUBSIZED);
        }
        return characteristics;
    }

    // The spliterator of a Traversable: sized when the size is known without a walk, and reporting the comparator of a
    // SortedSet (null for the natural order, as Spliterator specifies), so that java.util.stream.Stream.sorted() sorts
    // a set ordered otherwise instead of skipping the sort.
    public static <T extends @Nullable Object> Spliterator<T> spliterator(Traversable<T> traversable) {
        final int characteristics = spliteratorCharacteristics(traversable);
        final Spliterator<T> spliterator = (characteristics & Spliterator.SIZED) != 0
          ? Spliterators.spliterator(traversable.iterator(), traversable.size(), characteristics)
          : Spliterators.spliteratorUnknownSize(traversable.iterator(), characteristics);
        if (traversable instanceof SortedSet<?> sortedSet && !(sortedSet.comparator() instanceof NaturalComparator)) {
            @SuppressWarnings("unchecked")
            final Comparator<? super T> comparator = (Comparator<? super T>) sortedSet.comparator();
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
            final Spliterator<T> prefix = delegate.trySplit();
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

    public static <T extends @Nullable Object> T last(Traversable<T> source){
        if (source.isEmpty()) {
            throw new NoSuchElementException("last of empty " + source);
        } else {
            final java.util.Iterator<T> it = source.iterator();
            T result = it.next();
            while (it.hasNext()) {
                result = it.next();
            }
            return result;
        }
    }

    @SuppressWarnings("unchecked")
    public static <K extends @Nullable Object, V extends @Nullable Object, K2 extends @Nullable Object, U extends Map<K2, V>> U mapKeys(Map<K, V> source, U zero, Function<? super K, ? extends K2> keyMapper, BiFunction<? super V, ? super V, ? extends V> valueMerge) {
        Objects.requireNonNull(zero, "zero is null");
        Objects.requireNonNull(keyMapper, "keyMapper is null");
        Objects.requireNonNull(valueMerge, "valueMerge is null");
        return source.foldLeft(zero, (acc, entry) -> {
            final K2 k2 = Objects.requireNonNull(keyMapper.apply(entry._1()), "mapKeys: key is null");
            final V v2 = entry._2();
            final V v1 = Maps.getOrAbsent(acc, k2);
            final V v = v1 != Maps.ABSENT ? valueMerge.apply(v1, v2) : v2;
            return (U) acc.put(k2, v);
        });
    }

    public static <C extends Traversable<T>, T extends @Nullable Object> Tuple2<C, C> partition(C collection, Function<Iterable<T>, C> creator,
                                                                Predicate<? super T> predicate) {
        Objects.requireNonNull(predicate, "predicate is null");
        final java.util.List<T> left = new java.util.ArrayList<>();
        final java.util.List<T> right = new java.util.ArrayList<>();
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
    public static <T extends @Nullable Object, K extends @Nullable Object> java.util.List<T> duplicatesBy(Iterable<? extends T> elements, Function<? super T, ? extends K> keyExtractor) {
        final java.util.LinkedHashMap<K, T> first = new java.util.LinkedHashMap<>();
        final java.util.HashSet<K> duplicated = new java.util.HashSet<>();
        for (T element : elements) {
            final K key = keyExtractor.apply(element);
            if (first.putIfAbsent(key, element) != null) {
                duplicated.add(key);
            }
        }
        if (duplicated.isEmpty()) {
            return java.util.List.of();
        }
        final java.util.List<T> result = new java.util.ArrayList<>(duplicated.size());
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

    public static <C extends Traversable<T>, T extends @Nullable Object> C removeAll(C source, Iterable<? extends T> elements, Filter<T, C> filter) {
        Objects.requireNonNull(elements, "elements is null");
        if (source.isEmpty()) {
            return source;
        } else {
            final Set<T> removed = HashSet.ofAll(elements);
            return removed.isEmpty() ? source : filter.apply(e -> !removed.contains(e));
        }
    }

    public static <C extends Traversable<T>, T extends @Nullable Object> C reject(C source, Predicate<? super T> predicate, Filter<T, C> filter) {
        Objects.requireNonNull(predicate, "predicate is null");
        if (source.isEmpty()) {
            return source;
        } else {
            return filter.apply(predicate.negate());
        }
    }

    public static <C extends Traversable<T>, T extends @Nullable Object> C removeAll(C source, T element, Filter<T, C> filter) {
        if (source.isEmpty()) {
            return source;
        } else {
            return filter.apply(e -> !Objects.equals(e, element));
        }
    }

    public static <C extends Traversable<T>, T extends @Nullable Object> C retainAll(C source, Iterable<? extends T> elements, Filter<T, C> filter) {
        Objects.requireNonNull(elements, "elements is null");
        if (source.isEmpty()) {
            return source;
        } else {
            final Set<T> retained = HashSet.ofAll(elements);
            return filter.apply(retained::contains);
        }
    }

    static <T extends @Nullable Object> Iterator<T> reverseIterator(Iterable<T> iterable) {
        if (iterable instanceof java.util.List) {
            return reverseListIterator((java.util.List<T>) iterable);
        } else if (iterable instanceof Vector) {
            final Vector<T> vector = (Vector<T>) iterable;
            return new AbstractIterator<T>() {
                private int i = vector.length();

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
        } else if (iterable instanceof Stream) {
            return Iterator.ofAll(((Stream<T>) iterable).reverse());
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

    public static <T extends @Nullable Object, U extends @Nullable Object, R extends Traversable<U>> R scanLeft(Iterable<? extends T> source,
                                                       U zero, BiFunction<? super U, ? super T, ? extends U> operation, Function<Iterator<U>, R> finisher) {
        Objects.requireNonNull(operation, "operation is null");
        final Iterator<U> iterator = Iterator.ofAll(source).scanLeft(zero, operation);
        return finisher.apply(iterator);
    }

    public static <T extends @Nullable Object, U extends @Nullable Object, R extends Traversable<U>> R scanRight(Traversable<? extends T> source,
                                                        U zero, BiFunction<? super T, ? super U, ? extends U> operation, Function<Iterator<U>, R> finisher) {
        Objects.requireNonNull(operation, "operation is null");
        final Iterator<? extends T> reversedElements = reverseIterator(source);
        return scanLeft(reversedElements, zero, (u, t) -> operation.apply(t, u), us -> finisher.apply(reverseIterator(us)));
    }

    public static <T extends @Nullable Object, S extends Traversable<T>> S shuffle(S source, Function<? super Iterable<T>, S> ofAll) {
        if (source.size() <= 1) {
            return source;
        }

        final java.util.List<T> list = TraversableModule.toJavaCollection(source, ArrayList::new, 10);
        java.util.Collections.shuffle(list);
        return ofAll.apply(list);
    }

    public static void subSequenceRangeCheck(int beginIndex, int endIndex, int length) {
        if (beginIndex < 0 || endIndex > length) {
            throw new IndexOutOfBoundsException("subSequence(" + beginIndex + ", " + endIndex + "), length = " + length);
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

    public static <C extends Traversable<T>, T extends @Nullable Object> C tabulate(int n, Function<? super Integer, ? extends T> f, C empty, Function<T[], C> of) {
        Objects.requireNonNull(f, "f is null");
        Objects.requireNonNull(empty, "empty is null");
        Objects.requireNonNull(of, "of is null");
        if (n <= 0) {
            return empty;
        } else {
            @SuppressWarnings("unchecked")
            final T[] elements = (T[]) new Object[n];
            for (int i = 0; i < n; i++) {
                elements[i] = f.apply(i);
            }
            return of.apply(elements);
        }
    }

    public static <T extends @Nullable Object, U extends Traversable<T>, V extends Traversable<U>> V transpose(V matrix, Function<Iterable<U>, V> rowFactory, Function<T[], U> columnFactory) {
        Objects.requireNonNull(matrix, "matrix is null");
        if (matrix.isEmpty() || (matrix.size() == 1 && matrix.iterator().next().size() <= 1)) {
            return matrix;
        } else {
            return transposeNonEmptyMatrix(matrix, rowFactory, columnFactory);
        }
    }

    private static <T extends @Nullable Object, U extends Traversable<T>, V extends Traversable<U>> V transposeNonEmptyMatrix(V matrix, Function<Iterable<U>, V> rowFactory, Function<T[], U> columnFactory) {
        final int newHeight = matrix.iterator().next().size(), newWidth = matrix.size();
        @SuppressWarnings("unchecked") final T[][] results = (T[][]) new Object[newHeight][newWidth];

        if (matrix.exists(r -> r.size() != newHeight)) {
            throw new IllegalArgumentException("the parameter `matrix` is invalid!");
        }

        int rowIndex = 0;
        for (U row : matrix) {
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

    private static <T extends @Nullable Object> IterableWithSize<T> withSizeTraversable(Iterable<? extends T> iterable) {
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
                return ArrayType.asArray(iterator(), size());
            }
        }
    }

}
