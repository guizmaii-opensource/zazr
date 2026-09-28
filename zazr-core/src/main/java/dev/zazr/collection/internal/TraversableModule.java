package dev.zazr.collection.internal;

import dev.zazr.Tuple;
import dev.zazr.Tuple2;
import dev.zazr.collection.Map;
import dev.zazr.collection.SortedSet;
import dev.zazr.collection.Traversable;
import dev.zazr.collection.TreeSet;
import dev.zazr.control.Option;
import java.util.Comparator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.ObjIntConsumer;
import java.util.function.Predicate;
import java.util.function.ToDoubleFunction;
import java.util.function.ToIntFunction;
import java.util.function.ToLongFunction;
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
        Comparator<T> comparator = comparatorOf(traversable);
        return toTraversable(traversable, TreeSet.empty(comparator), values -> TreeSet.ofAll(comparator, values));
    }

    static <
                    T extends @Nullable Object,
                    K extends @Nullable Object,
                    V extends @Nullable Object,
                    E extends Tuple2<? extends K, ? extends V>,
                    R extends Map<K, V>>
            R toMap(
                    Traversable<T> traversable,
                    R empty,
                    Function<Iterable<E>, R> ofAll,
                    Function<? super T, ? extends E> f,
                    String nullResult) {
        Objects.requireNonNull(f, "f is null");
        return traversable.isEmpty()
                ? empty
                : ofAll.apply(Iterator.ofAll(traversable).map(t -> Objects.requireNonNull(f.apply(t), nullResult)));
    }

    static <T extends @Nullable Object, K extends @Nullable Object, V extends @Nullable Object>
            Function<T, Tuple2<K, V>> entryMapper(
                    Function<? super T, ? extends K> keyMapper, Function<? super T, ? extends V> valueMapper) {
        Objects.requireNonNull(keyMapper, "keyMapper is null");
        Objects.requireNonNull(valueMapper, "valueMapper is null");
        return t -> Tuple.of(keyMapper.apply(t), valueMapper.apply(t));
    }

    static <K extends @Nullable Object, T extends @Nullable Object> Option<Map<K, T>> arrangeBy(
            Map<K, ? extends Traversable<T>> groups) {
        for (Tuple2<K, ? extends Traversable<T>> group : groups) {
            if (group._2().size() != 1) {
                return Option.none();
            }
        }
        return Option.some(groups.mapValues(group -> group.iterator().next()));
    }

    static <T extends @Nullable Object> boolean existsUnique(Iterable<T> elements, Predicate<? super T> predicate) {
        Objects.requireNonNull(predicate, "predicate is null");
        @SuppressWarnings("Var")
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
        @SuppressWarnings("Var")
        @Nullable
        T last = null;
        @SuppressWarnings("Var")
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
        @SuppressWarnings("Var")
        int index = 0;
        for (T t : elements) {
            action.accept(t, index++);
        }
    }

    static <T extends @Nullable Object> T reduceLeft(
            Traversable<T> traversable, BiFunction<? super T, ? super T, ? extends T> op) {
        Objects.requireNonNull(op, "op is null");
        java.util.Iterator<T> iterator = traversable.iterator();
        if (!iterator.hasNext()) {
            throw new NoSuchElementException(
                    "reduceLeft on empty " + traversable.getClass().getSimpleName());
        }
        @SuppressWarnings("Var")
        T xs = iterator.next();
        while (iterator.hasNext()) {
            xs = op.apply(xs, iterator.next());
        }
        return xs;
    }

    static <T extends @Nullable Object> Option<T> reduceLeftOption(
            Traversable<T> traversable, BiFunction<? super T, ? super T, ? extends T> op) {
        Objects.requireNonNull(op, "op is null");
        return traversable.isEmpty() ? Option.none() : Option.some(reduceLeft(traversable, op));
    }

    static <T extends @Nullable Object> T single(Traversable<T> traversable) {
        return singleOption(traversable)
                .getOrElseThrow(() -> new NoSuchElementException("Does not contain a single value"));
    }

    static <T extends @Nullable Object> Option<T> singleOption(Traversable<T> traversable) {
        java.util.Iterator<T> it = traversable.iterator();
        if (!it.hasNext()) {
            return Option.none();
        }
        T first = it.next();
        return it.hasNext() ? Option.none() : Option.some(first);
    }

    static <T extends @Nullable Object> Option<T> max(Traversable<T> traversable) {
        return maxBy(traversable, Comparators.naturalComparator());
    }

    static <T extends @Nullable Object> Option<T> maxBy(Traversable<T> traversable, Comparator<? super T> comparator) {
        Objects.requireNonNull(comparator, "comparator is null");
        java.util.Iterator<T> iterator = traversable.iterator();
        if (!iterator.hasNext()) {
            return Option.none();
        }
        @SuppressWarnings("Var")
        T max = iterator.next();
        while (iterator.hasNext()) {
            T t = iterator.next();
            if (comparator.compare(t, max) > 0) {
                max = t;
            }
        }
        return Option.some(max);
    }

    static <T extends @Nullable Object, U extends Comparable<? super U>> Option<T> maxBy(
            Traversable<T> traversable, Function<? super T, ? extends U> f) {
        Objects.requireNonNull(f, "f is null");
        java.util.Iterator<T> iterator = traversable.iterator();
        if (!iterator.hasNext()) {
            return Option.none();
        }
        @SuppressWarnings("Var")
        T tm = iterator.next();
        @SuppressWarnings("Var")
        U um = f.apply(tm);
        while (iterator.hasNext()) {
            T t = iterator.next();
            U u = f.apply(t);
            if (u.compareTo(um) > 0) {
                um = u;
                tm = t;
            }
        }
        return Option.some(tm);
    }

    // minBy(naturalComparator) would not handle (Double/Float) NaN as min() promises
    @SuppressWarnings({"unchecked", "Var"})
    static <T extends @Nullable Object> Option<T> min(Traversable<T> traversable) {
        java.util.Iterator<T> iterator = traversable.iterator();
        if (!iterator.hasNext()) {
            return Option.none();
        }
        T head = iterator.next();
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
            Comparator<T> comparator = Comparators.naturalComparator();
            T min = head;
            while (iterator.hasNext()) {
                T t = iterator.next();
                if (comparator.compare(t, min) < 0) {
                    min = t;
                }
            }
            return Option.some(min);
        }
    }

    static <T extends @Nullable Object> Option<T> minBy(Traversable<T> traversable, Comparator<? super T> comparator) {
        Objects.requireNonNull(comparator, "comparator is null");
        java.util.Iterator<T> iterator = traversable.iterator();
        if (!iterator.hasNext()) {
            return Option.none();
        }
        @SuppressWarnings("Var")
        T min = iterator.next();
        while (iterator.hasNext()) {
            T t = iterator.next();
            if (comparator.compare(t, min) < 0) {
                min = t;
            }
        }
        return Option.some(min);
    }

    static <T extends @Nullable Object, U extends Comparable<? super U>> Option<T> minBy(
            Traversable<T> traversable, Function<? super T, ? extends U> f) {
        Objects.requireNonNull(f, "f is null");
        java.util.Iterator<T> iterator = traversable.iterator();
        if (!iterator.hasNext()) {
            return Option.none();
        }
        @SuppressWarnings("Var")
        T tm = iterator.next();
        @SuppressWarnings("Var")
        U um = f.apply(tm);
        while (iterator.hasNext()) {
            T t = iterator.next();
            U u = f.apply(t);
            if (u.compareTo(um) < 0) {
                um = u;
                tm = t;
            }
        }
        return Option.some(tm);
    }

    // The int and long sums and products throw when the exact result does not fit, and only then: the result of a set
    // does not depend on its iteration order, and an overflow the later elements undo does not throw.

    @SuppressWarnings("Var")
    static <T extends @Nullable Object> int sumInt(Iterable<T> ts, ToIntFunction<? super T> mapper) {
        Objects.requireNonNull(mapper, "mapper is null");
        // a long cannot overflow before 2^32 int elements; addExact still guards a longer LazyList
        long sum = 0L;
        for (T t : ts) {
            sum = Math.addExact(sum, mapper.applyAsInt(t));
        }
        return Math.toIntExact(sum);
    }

    @SuppressWarnings("Var")
    static <T extends @Nullable Object> long sumLong(Iterable<T> ts, ToLongFunction<? super T> mapper) {
        Objects.requireNonNull(mapper, "mapper is null");
        // the exact sum is sum + carries * 2^64: a partial sum may overflow as long as the later elements bring it back
        long sum = 0L;
        long carries = 0L;
        for (T t : ts) {
            long x = mapper.applyAsLong(t);
            long next = sum + x;
            if (((sum ^ next) & (x ^ next)) < 0) {
                carries += (x < 0) ? -1 : 1;
            }
            sum = next;
        }
        if (carries != 0) {
            throw new ArithmeticException("long overflow");
        }
        return sum;
    }

    static <T extends @Nullable Object> double sumDouble(Iterable<T> ts, ToDoubleFunction<? super T> mapper) {
        Objects.requireNonNull(mapper, "mapper is null");
        return neumaierSum(ts, mapper)[0];
    }

    @SuppressWarnings("Var")
    static <T extends @Nullable Object> int productInt(Iterable<T> ts, ToIntFunction<? super T> mapper) {
        Objects.requireNonNull(mapper, "mapper is null");
        // once a factor is not zero, the magnitude never shrinks: past 2^31 only a zero factor makes the result fit
        long product = 1L;
        boolean zero = false;
        boolean overflow = false;
        for (T t : ts) {
            int x = mapper.applyAsInt(t);
            if (x == 0) {
                zero = true;
            } else if (!overflow) {
                // |product| <= 2^31 and |x| <= 2^31, so this long product is exact
                product *= x;
                overflow = Math.abs(product) > (1L << 31);
            }
        }
        if (zero) {
            return 0;
        } else if (overflow) {
            throw new ArithmeticException("integer overflow");
        } else {
            return Math.toIntExact(product);
        }
    }

    @SuppressWarnings("Var")
    static <T extends @Nullable Object> long productLong(Iterable<T> ts, ToLongFunction<? super T> mapper) {
        Objects.requireNonNull(mapper, "mapper is null");
        // the magnitude is an unsigned long (Math.abs(Long.MIN_VALUE) is 2^63 read as unsigned) and the sign is kept
        // apart; once a factor is not zero, the magnitude never shrinks: past 2^64 only a zero factor makes it fit
        long magnitude = 1L;
        boolean negative = false;
        boolean zero = false;
        boolean overflow = false;
        for (T t : ts) {
            long x = mapper.applyAsLong(t);
            if (x == 0L) {
                zero = true;
            } else if (!overflow) {
                long m = Math.abs(x);
                overflow = Math.unsignedMultiplyHigh(magnitude, m) != 0L;
                magnitude *= m;
                negative ^= x < 0L;
            }
        }
        if (zero) {
            return 0L;
        } else if (overflow || (negative ? Long.compareUnsigned(magnitude, Long.MIN_VALUE) > 0 : magnitude < 0L)) {
            throw new ArithmeticException("long overflow");
        } else {
            return negative ? -magnitude : magnitude;
        }
    }

    @SuppressWarnings("Var")
    static <T extends @Nullable Object> double productDouble(Iterable<T> ts, ToDoubleFunction<? super T> mapper) {
        Objects.requireNonNull(mapper, "mapper is null");
        double product = 1.0;
        for (T t : ts) {
            product *= mapper.applyAsDouble(t);
        }
        return product;
    }

    static <T extends @Nullable Object> Option<Double> average(Iterable<T> ts, ToDoubleFunction<? super T> mapper) {
        Objects.requireNonNull(mapper, "mapper is null");
        double[] sum = neumaierSum(ts, mapper);
        return (sum[1] == 0) ? Option.none() : Option.some(sum[0] / sum[1]);
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
    @SuppressWarnings("Var")
    static <T extends @Nullable Object> double[] neumaierSum(Iterable<T> ts, ToDoubleFunction<? super T> toDouble) {
        double simpleSum = 0.0;
        double sum = 0.0;
        double compensation = 0.0;
        int size = 0;
        for (T t : ts) {
            double d = toDouble.applyAsDouble(t);
            double tmp = sum + d;
            compensation += (Math.abs(sum) >= Math.abs(d)) ? (sum - tmp) + d : (d - tmp) + sum;
            sum = tmp;
            simpleSum += d;
            size++;
        }
        sum += compensation;
        if (size > 0 && Double.isNaN(sum) && Double.isInfinite(simpleSum)) {
            sum = simpleSum;
        }
        return new double[] {sum, size};
    }
}
