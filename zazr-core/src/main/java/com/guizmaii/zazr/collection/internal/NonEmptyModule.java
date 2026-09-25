package com.guizmaii.zazr.collection.internal;

import java.util.Comparator;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
import org.jspecify.annotations.Nullable;

/**
 * The total aggregates of the non-empty collections: loops over an iterable known to hold at least one element, so
 * that nothing is wrapped in an {@code Option} to be unwrapped on the next line. Ties go to the first element in
 * iteration order, as on the plain collections.
 */
public interface NonEmptyModule {

    static <T extends @Nullable Object> T max(Iterable<T> nonEmpty, Comparator<? super T> comparator) {
        Objects.requireNonNull(comparator, "comparator is null");
        final java.util.Iterator<T> iterator = nonEmpty.iterator();
        T max = iterator.next();
        while (iterator.hasNext()) {
            final T element = iterator.next();
            if (comparator.compare(element, max) > 0) {
                max = element;
            }
        }
        return max;
    }

    static <T extends @Nullable Object> T min(Iterable<T> nonEmpty, Comparator<? super T> comparator) {
        Objects.requireNonNull(comparator, "comparator is null");
        final java.util.Iterator<T> iterator = nonEmpty.iterator();
        T min = iterator.next();
        while (iterator.hasNext()) {
            final T element = iterator.next();
            if (comparator.compare(element, min) < 0) {
                min = element;
            }
        }
        return min;
    }

    static <T extends @Nullable Object> T max(Iterable<T> nonEmpty) {
        return max(nonEmpty, Comparators.naturalComparator());
    }

    /* a NaN is the result whenever one is present, as on the plain collections' min() */
    @SuppressWarnings("unchecked")
    static <T extends @Nullable Object> T min(Iterable<T> nonEmpty) {
        final java.util.Iterator<T> iterator = nonEmpty.iterator();
        final T head = iterator.next();
        if (head instanceof Double first) {
            double min = first;
            while (iterator.hasNext()) {
                min = Math.min(min, (Double) iterator.next());
            }
            return (T) (Double) min;
        } else if (head instanceof Float first) {
            float min = first;
            while (iterator.hasNext()) {
                min = Math.min(min, (Float) iterator.next());
            }
            return (T) (Float) min;
        } else {
            final Comparator<T> comparator = Comparators.naturalComparator();
            T min = head;
            while (iterator.hasNext()) {
                final T element = iterator.next();
                if (comparator.compare(element, min) < 0) {
                    min = element;
                }
            }
            return min;
        }
    }

    static <T extends @Nullable Object, U extends Comparable<? super U>> T maxBy(Iterable<T> nonEmpty, Function<? super T, ? extends U> f) {
        Objects.requireNonNull(f, "f is null");
        final java.util.Iterator<T> iterator = nonEmpty.iterator();
        T max = iterator.next();
        U maxKey = f.apply(max);
        while (iterator.hasNext()) {
            final T element = iterator.next();
            final U key = f.apply(element);
            if (key.compareTo(maxKey) > 0) {
                max = element;
                maxKey = key;
            }
        }
        return max;
    }

    static <T extends @Nullable Object, U extends Comparable<? super U>> T minBy(Iterable<T> nonEmpty, Function<? super T, ? extends U> f) {
        Objects.requireNonNull(f, "f is null");
        final java.util.Iterator<T> iterator = nonEmpty.iterator();
        T min = iterator.next();
        U minKey = f.apply(min);
        while (iterator.hasNext()) {
            final T element = iterator.next();
            final U key = f.apply(element);
            if (key.compareTo(minKey) < 0) {
                min = element;
                minKey = key;
            }
        }
        return min;
    }

    static <T extends @Nullable Object> T reduce(Iterable<T> nonEmpty, BiFunction<? super T, ? super T, ? extends T> op) {
        Objects.requireNonNull(op, "op is null");
        final java.util.Iterator<T> iterator = nonEmpty.iterator();
        T result = iterator.next();
        while (iterator.hasNext()) {
            result = op.apply(result, iterator.next());
        }
        return result;
    }

    static <T extends @Nullable Object, B extends @Nullable Object> B reduceMap(Iterable<T> nonEmpty, Function<? super T, ? extends B> mapper,
            BiFunction<? super B, ? super B, ? extends B> op) {
        Objects.requireNonNull(mapper, "mapper is null");
        Objects.requireNonNull(op, "op is null");
        final java.util.Iterator<T> iterator = nonEmpty.iterator();
        B result = mapper.apply(iterator.next());
        while (iterator.hasNext()) {
            result = op.apply(result, mapper.apply(iterator.next()));
        }
        return result;
    }

    /* the value the plain collections' average() holds, from the same compensated sum */
    static double average(Iterable<?> nonEmpty) {
        try {
            final double[] sum = TraversableModule.neumaierSum(nonEmpty, element -> ((Number) element).doubleValue());
            return sum[0] / sum[1];
        } catch (ClassCastException x) {
            throw new UnsupportedOperationException("Elements are not numeric", x);
        }
    }
}
