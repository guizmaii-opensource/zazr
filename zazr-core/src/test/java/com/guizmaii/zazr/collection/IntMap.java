package com.guizmaii.zazr.collection;

import java.util.Spliterator;
import java.util.function.Consumer;

/**
 * A {@link Traversable} view of the values of a {@code Map<Integer, T>}, so that the map types can run the shared
 * {@link AbstractTraversableTest} cases, whose hooks build a collection of arbitrary elements.
 */
public final class IntMap<T> implements Traversable<T> {

    private final Map<Integer, T> original;

    private static final IntMap<?> EMPTY = new IntMap<>(HashMap.empty());

    @SuppressWarnings("unchecked")
    public static <T> IntMap<T> of(Map<Integer, T> original) {
        return original.isEmpty() ? (IntMap<T>) EMPTY
                                  : new IntMap<>(original);
    }

    private IntMap(Map<Integer, T> original) {
        this.original = original;
    }

    Map<Integer, T> original() {
        return original;
    }

    @Override
    public boolean equals(Object o) {
        final Object that = (o instanceof IntMap) ? ((IntMap<?>) o).original : o;
        return Collections.equals(original, that);
    }

    @Override
    public int hashCode() {
        return original.hashCode();
    }

    @Override
    public String toString() {
        return mkString("IntMap(", ", ", ")");
    }

    @Override
    public boolean isEmpty() {
        return original.isEmpty();
    }

    @Override
    public int size() {
        return original.size();
    }

    @Override
    public java.util.Iterator<T> iterator() {
        return original.values().iterator();
    }

    @Override
    public Spliterator<T> spliterator() {
        return new Spliterator<T>() {

            private final java.util.Iterator<T> iterator = iterator();

            @Override
            public boolean tryAdvance(Consumer<? super T> action) {
                if (iterator.hasNext()) {
                    action.accept(iterator.next());
                    return true;
                } else {
                    return false;
                }
            }

            @Override
            public Spliterator<T> trySplit() {
                return null;
            }

            @Override
            public long estimateSize() {
                return size();
            }

            @Override
            public int characteristics() {
                int characteristics = Spliterator.IMMUTABLE | Spliterator.SIZED | Spliterator.SUBSIZED | Spliterator.DISTINCT;
                if (original instanceof SortedMap || original instanceof LinkedHashMap) {
                    characteristics |= Spliterator.ORDERED; // the values follow the key order, they are not sorted themselves
                }
                return characteristics;
            }
        };
    }
}
