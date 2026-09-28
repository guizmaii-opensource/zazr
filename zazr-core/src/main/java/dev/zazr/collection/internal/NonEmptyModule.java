package dev.zazr.collection.internal;

import java.util.Comparator;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.ToDoubleFunction;
import org.jspecify.annotations.Nullable;

/**
 * The total aggregates of the non-empty collections: loops over an iterable known to hold at least one element, so
 * that nothing is wrapped in an {@code Option} to be unwrapped on the next line. Ties go to the first element in
 * iteration order, as on the plain collections.
 */
public interface NonEmptyModule {

    static <T extends @Nullable Object> T max(Iterable<T> nonEmpty, Comparator<? super T> comparator) {
        Objects.requireNonNull(comparator, "comparator is null");
        java.util.Iterator<T> iterator = nonEmpty.iterator();
        @SuppressWarnings("Var")
        T max = iterator.next();
        while (iterator.hasNext()) {
            T element = iterator.next();
            if (comparator.compare(element, max) > 0) {
                max = element;
            }
        }
        return max;
    }

    static <T extends @Nullable Object> T min(Iterable<T> nonEmpty, Comparator<? super T> comparator) {
        Objects.requireNonNull(comparator, "comparator is null");
        java.util.Iterator<T> iterator = nonEmpty.iterator();
        @SuppressWarnings("Var")
        T min = iterator.next();
        while (iterator.hasNext()) {
            T element = iterator.next();
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
    @SuppressWarnings({"unchecked", "Var"})
    static <T extends @Nullable Object> T min(Iterable<T> nonEmpty) {
        java.util.Iterator<T> iterator = nonEmpty.iterator();
        T head = iterator.next();
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
            Comparator<T> comparator = Comparators.naturalComparator();
            T min = head;
            while (iterator.hasNext()) {
                T element = iterator.next();
                if (comparator.compare(element, min) < 0) {
                    min = element;
                }
            }
            return min;
        }
    }

    static <T extends @Nullable Object, U extends Comparable<? super U>> T maxBy(
            Iterable<T> nonEmpty, Function<? super T, ? extends U> f) {
        Objects.requireNonNull(f, "f is null");
        java.util.Iterator<T> iterator = nonEmpty.iterator();
        @SuppressWarnings("Var")
        T max = iterator.next();
        @SuppressWarnings("Var")
        U maxKey = f.apply(max);
        while (iterator.hasNext()) {
            T element = iterator.next();
            U key = f.apply(element);
            if (key.compareTo(maxKey) > 0) {
                max = element;
                maxKey = key;
            }
        }
        return max;
    }

    static <T extends @Nullable Object, U extends Comparable<? super U>> T minBy(
            Iterable<T> nonEmpty, Function<? super T, ? extends U> f) {
        Objects.requireNonNull(f, "f is null");
        java.util.Iterator<T> iterator = nonEmpty.iterator();
        @SuppressWarnings("Var")
        T min = iterator.next();
        @SuppressWarnings("Var")
        U minKey = f.apply(min);
        while (iterator.hasNext()) {
            T element = iterator.next();
            U key = f.apply(element);
            if (key.compareTo(minKey) < 0) {
                min = element;
                minKey = key;
            }
        }
        return min;
    }

    static <T extends @Nullable Object> T reduce(
            Iterable<T> nonEmpty, BiFunction<? super T, ? super T, ? extends T> op) {
        Objects.requireNonNull(op, "op is null");
        java.util.Iterator<T> iterator = nonEmpty.iterator();
        @SuppressWarnings("Var")
        T result = iterator.next();
        while (iterator.hasNext()) {
            result = op.apply(result, iterator.next());
        }
        return result;
    }

    static <T extends @Nullable Object, B extends @Nullable Object> B reduceMap(
            Iterable<T> nonEmpty,
            Function<? super T, ? extends B> mapper,
            BiFunction<? super B, ? super B, ? extends B> op) {
        Objects.requireNonNull(mapper, "mapper is null");
        Objects.requireNonNull(op, "op is null");
        java.util.Iterator<T> iterator = nonEmpty.iterator();
        @SuppressWarnings("Var")
        B result = mapper.apply(iterator.next());
        while (iterator.hasNext()) {
            result = op.apply(result, mapper.apply(iterator.next()));
        }
        return result;
    }

    /* the average of a collection known to be non-empty: the value the plain collections' average holds */
    static <T extends @Nullable Object> double average(Iterable<T> nonEmpty, ToDoubleFunction<? super T> mapper) {
        Objects.requireNonNull(mapper, "mapper is null");
        double[] sum = TraversableModule.neumaierSum(nonEmpty, mapper);
        return sum[0] / sum[1];
    }
}
