package com.guizmaii.zazr.collection.internal;

import com.guizmaii.zazr.Lazy;
import com.guizmaii.zazr.collection.Set;
import com.guizmaii.zazr.collection.Vector;
import com.guizmaii.zazr.collection.internal.IteratorModule.ConcatIterator;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.function.Function;
import org.jspecify.annotations.Nullable;

import static com.guizmaii.zazr.collection.internal.IteratorModule.BigDecimalHelper.areEqual;
import static java.lang.Double.NEGATIVE_INFINITY;
import static java.lang.Double.POSITIVE_INFINITY;
import static java.math.RoundingMode.HALF_UP;

public interface IteratorModule {

    // inspired by Scala's ConcatIterator
    final class ConcatIterator<T extends @Nullable Object> extends AbstractIterator<T> {

        private static class Cell<T extends @Nullable Object> {

            final Iterator<T> it;
            @Nullable Cell<T> next;

            Cell(Iterator<T> it) {
                this.it = it;
            }

            static <T extends @Nullable Object> Cell<T> of(Iterator<T> it) {
                return new Cell<>(it);
            }

            Cell<T> append(Iterator<T> it) {
                Cell<T> cell = of(it);
                next = cell;
                return cell;
            }
        }

        private @Nullable Iterator<T> curr;
        private @Nullable Cell<T> tail;
        private @Nullable Cell<T> last;
        private boolean hasNextCalculated;

        // `tail` and `last` are assigned together, so a non-null tail implies a non-null last
        @SuppressWarnings("NullAway")
        void append(java.util.Iterator<? extends T> that) {
            final Iterator<T> it = Iterator.ofAll(that);
            if (tail == null) {
                tail = last = Cell.of(it);
            } else {
                last = last.append(it);
            }
        }

        @Override
        public Iterator<T> concat(java.util.Iterator<? extends T> that) {
            append(that);
            return this;
        }

        @Override
        // a non-empty ConcatIterator always has a non-null `last` cell (see append)
        @SuppressWarnings("NullAway")
        public boolean hasNext() {
            if (hasNextCalculated) {
                return curr != null;
            }
            hasNextCalculated = true;
            while(true) {
                if (curr != null) {
                    if (curr.hasNext()) {
                        return true;
                    } else {
                        curr = null;
                    }
                }
                if (tail == null) {
                    return false;
                }
                curr = tail.it;
                tail = tail.next;
                while (curr instanceof ConcatIterator) {
                    ConcatIterator<T> it = (ConcatIterator<T>) curr;
                    curr = it.curr;
                    it.last.next = tail;
                    tail = it.tail;
                }
            }
        }

        @Override
        // getNext() is only reached after hasNext() returned true, which implies curr != null
        @SuppressWarnings("NullAway")
        public T getNext() {
            hasNextCalculated = false;
            return curr.next();
        }
    }

    final class DistinctIterator<T extends @Nullable Object, U extends @Nullable Object> extends AbstractIterator<T> {

        private final Iterator<? extends T> that;
        private com.guizmaii.zazr.collection.Set<U> known;
        private final Function<? super T, ? extends U> keyExtractor;
        private boolean nextDefined = false;
        private @Nullable T next;

        DistinctIterator(Iterator<? extends T> that, Set<U> set, Function<? super T, ? extends U> keyExtractor) {
            this.that = that;
            this.known = set;
            this.keyExtractor = keyExtractor;
        }

        @Override
        public boolean hasNext() {
            return nextDefined || searchNext();
        }

        private boolean searchNext() {
            while (that.hasNext()) {
                final T elem = that.next();
                final U key = keyExtractor.apply(elem);
                if (!known.contains(key)) {
                    known = known.add(key);
                    nextDefined = true;
                    next = elem;
                    return true;
                }
            }
            return false;
        }

        @Override
        // hasNext() sets `next` whenever it sets `nextDefined`
        @SuppressWarnings("NullAway")
        public T getNext() {
            final T result = next;
            nextDefined = false;
            next = null;
            return result;
        }
    }

    final class EmptyIterator implements Iterator<Object> {

        static final EmptyIterator INSTANCE = new EmptyIterator();

        @Override
        public boolean hasNext() { return false; }

        @Override
        public Object next() { throw new NoSuchElementException("EmptyIterator.next()"); }

        @Override
        public String toString() {
            return "EmptyIterator()";
        }
    }

    /* the groups are Vectors, one leaf array each */
    final class GroupedIterator<T extends @Nullable Object> implements Iterator<Vector<T>> {

        private final Iterator<T> that;
        private final int size;
        private final int step;
        private final int gap;
        private final int preserve;

        private Object[] buffer;

        GroupedIterator(Iterator<T> that, int size, int step) {
            if (size < 1 || step < 1) {
                throw new IllegalArgumentException("size (" + size + ") and step (" + step + ") must both be positive");
            }
            this.that = that;
            this.size = size;
            this.step = step;
            this.gap = Math.max(step - size, 0);
            this.preserve = Math.max(size - step, 0);
            // the first group starts small and grows with the source: a size beyond the source (grouped(Integer.MAX_VALUE)
            // is one group) must not allocate an array of that size. Later groups allocate `size` directly: they are only
            // reached when the previous group was full, so the source is known to hold that many.
            this.buffer = take(that, new Object[Math.min(size, INITIAL_CAPACITY)], 0, size);
        }

        private static final int INITIAL_CAPACITY = 32;

        @Override
        public boolean hasNext() {
            return buffer.length > 0;
        }

        @Override
        public Vector<T> next() {
            if (buffer.length == 0) {
                throw new NoSuchElementException();
            }
            final Object[] result = buffer;
            if (that.hasNext()) {
                buffer = new Object[size];
                if (preserve > 0) {
                    System.arraycopy(result, step, buffer, 0, preserve);
                }
                if (gap > 0) {
                    drop(that, gap);
                    buffer = take(that, buffer, preserve, size);
                } else {
                    buffer = take(that, buffer, preserve, step);
                }
            } else {
                buffer = new Object[0];
            }
            @SuppressWarnings("unchecked")
            final T[] typed = (T[]) result;
            return Vector.of(typed);
        }

        private static void drop(Iterator<?> source, int count) {
            for (int i = 0; i < count && source.hasNext(); i++) {
                source.next();
            }
        }

        /* fills target[offset, offset + count) from the source, doubling the array when it is full, and trims it to what was read */
        private static Object[] take(Iterator<?> source, Object[] target, int offset, int count) {
            final int wanted = offset + count;
            Object[] buffer = target;
            int i = offset;
            while (i < wanted && source.hasNext()) {
                if (i == buffer.length) {
                    buffer = Arrays.copyOf(buffer, (int) Math.min(wanted, 2L * buffer.length));
                }
                buffer[i++] = source.next();
            }
            return (i < buffer.length) ? Arrays.copyOf(buffer, i) : buffer;
        }
    }

    final class CachedIterator<T extends @Nullable Object> extends AbstractIterator<T> {

        private final Iterator<T> that;

        private @Nullable T next;
        private boolean cached = false;

        CachedIterator(Iterator<T> that) {
            this.that = that;
        }

        @Override
        public boolean hasNext() {
            return cached || that.hasNext();
        }

        @Override
        // `next` is populated whenever `cached` is set
        @SuppressWarnings("NullAway")
        public T getNext() {
            if (cached) {
                T result = next;
                next = null;
                cached = false;
                return result;
            } else {
                return that.next();
            }
        }

        T touch() {
            next = next();
            cached = true;
            return next;
        }
    }

    final class BigDecimalHelper {

        private static final Lazy<BigDecimal> INFINITY_DISTANCE = Lazy.of(() -> {
            final BigDecimal two = BigDecimal.valueOf(2);
            final BigDecimal supremum = BigDecimal.valueOf(Math.nextDown(Double.POSITIVE_INFINITY));
            BigDecimal lowerBound = supremum;
            BigDecimal upperBound = two.pow(Double.MAX_EXPONENT + 1);
            while (true) {
                final BigDecimal magicValue = lowerBound.add(upperBound).divide(two, HALF_UP);
                if (Double.isInfinite(magicValue.doubleValue())) {
                    if (areEqual(magicValue, upperBound)) {
                        return magicValue.subtract(supremum);
                    }
                    upperBound = magicValue;
                } else {
                    lowerBound = magicValue;
                }
            }
        });

        /* scale-independent equality */
        static boolean areEqual(BigDecimal from, BigDecimal toExclusive) {
            return from.compareTo(toExclusive) == 0;
        }

        /* parse infinite values also */
        static BigDecimal asDecimal(double number) {
            if (number == NEGATIVE_INFINITY) {
                final BigDecimal result = BigDecimal.valueOf(Math.nextUp(NEGATIVE_INFINITY));
                return result.subtract(INFINITY_DISTANCE.get());
            } else if (number == POSITIVE_INFINITY) {
                final BigDecimal result = BigDecimal.valueOf(Math.nextDown(POSITIVE_INFINITY));
                return result.add(INFINITY_DISTANCE.get());
            } else {
                return BigDecimal.valueOf(number);
            }
        }
    }
}
