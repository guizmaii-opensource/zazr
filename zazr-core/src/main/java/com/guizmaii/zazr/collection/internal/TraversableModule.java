package com.guizmaii.zazr.collection.internal;

import com.guizmaii.zazr.Tuple;
import com.guizmaii.zazr.Tuple2;
import com.guizmaii.zazr.collection.Map;
import com.guizmaii.zazr.collection.SortedSet;
import com.guizmaii.zazr.collection.Traversable;
import com.guizmaii.zazr.collection.TreeSet;
import com.guizmaii.zazr.control.Option;
import java.util.Comparator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.ObjIntConsumer;
import java.util.function.Predicate;
import java.util.function.ToDoubleFunction;
import org.jspecify.annotations.Nullable;

/** The one-pass operations the concrete types declare with their own signatures, implemented once over an Iterable. */
public interface TraversableModule {

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
            Traversable<T> traversable, R empty, Function<Iterable<E>, R> ofAll, Function<? super T, ? extends E> f, String nullResult) {
        Objects.requireNonNull(f, "f is null");
        return traversable.isEmpty() ? empty : ofAll.apply(Iterator.ofAll(traversable).map(t -> Objects.requireNonNull(f.apply(t), nullResult)));
    }

    static <T extends @Nullable Object, K extends @Nullable Object, V extends @Nullable Object> Function<T, Tuple2<K, V>> entryMapper(
            Function<? super T, ? extends K> keyMapper, Function<? super T, ? extends V> valueMapper) {
        Objects.requireNonNull(keyMapper, "keyMapper is null");
        Objects.requireNonNull(valueMapper, "valueMapper is null");
        return t -> Tuple.of(keyMapper.apply(t), valueMapper.apply(t));
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
