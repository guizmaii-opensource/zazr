package com.guizmaii.zazr.collection.internal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.Function;

/**
 * The oracle of {@link RadixVectorTest}: the operations it calls, with the contract of {@code Vector}, computed on one
 * flat array that every change copies. Too slow for a library, too simple to share a bug with the finger tree.
 */
final class VectorModel<T> implements Iterable<T> {

    private static final VectorModel<?> EMPTY = new VectorModel<>(new Object[0]);

    /* never written after construction */
    private final Object[] elements;

    private VectorModel(Object[] elements) {
        this.elements = elements;
    }

    @SuppressWarnings("unchecked")
    static <T> VectorModel<T> empty() {
        return (VectorModel<T>) EMPTY;
    }

    @SuppressWarnings("unchecked")
    static <T> VectorModel<T> ofAll(Iterable<? extends T> iterable) {
        if (iterable instanceof VectorModel<?> v) {
            return (VectorModel<T>) v;
        }
        return of(toArray(iterable));
    }

    private static <T> VectorModel<T> of(Object[] elements) {
        return (elements.length == 0) ? empty() : new VectorModel<>(elements);
    }

    private static Object[] toArray(Iterable<?> iterable) {
        if (iterable instanceof VectorModel<?> v) {
            return v.elements;
        }
        final ArrayList<Object> list = new ArrayList<>();
        for (Object element : iterable) {
            list.add(Objects.requireNonNull(element, "Vector: element is null"));
        }
        return list.toArray();
    }

    private static Object[] concat(Object[] first, Object[] second) {
        final Object[] result = Arrays.copyOf(first, first.length + second.length);
        System.arraycopy(second, 0, result, first.length, second.length);
        return result;
    }

    int length() {
        return elements.length;
    }

    boolean isEmpty() {
        return elements.length == 0;
    }

    @SuppressWarnings("unchecked")
    T get(int index) {
        if (index >= 0 && index < elements.length) {
            return (T) elements[index];
        }
        throw new IndexOutOfBoundsException("get(" + index + ")");
    }

    T head() {
        if (isEmpty()) {
            throw new NoSuchElementException("head of empty Vector");
        }
        return get(0);
    }

    T last() {
        if (isEmpty()) {
            throw new NoSuchElementException("last of empty Vector");
        }
        return get(elements.length - 1);
    }

    VectorModel<T> update(int index, T element) {
        if (index < 0 || index >= elements.length) {
            throw new IndexOutOfBoundsException("update(" + index + ")");
        }
        final Object[] result = elements.clone();
        result[index] = Objects.requireNonNull(element, "Vector.update: element is null");
        return new VectorModel<>(result);
    }

    VectorModel<T> append(T element) {
        return of(concat(elements, new Object[] { Objects.requireNonNull(element, "List: element is null") }));
    }

    VectorModel<T> prepend(T element) {
        return of(concat(new Object[] { Objects.requireNonNull(element, "List: element is null") }, elements));
    }

    VectorModel<T> appendAll(Iterable<? extends T> iterable) {
        final Object[] suffix = toArray(iterable);
        return (suffix.length == 0) ? this : of(concat(elements, suffix));
    }

    VectorModel<T> prependAll(Iterable<? extends T> iterable) {
        final Object[] prefix = toArray(iterable);
        return (prefix.length == 0) ? this : of(concat(prefix, elements));
    }

    VectorModel<T> slice(int beginIndex, int endIndex) {
        final int lo = Math.max(beginIndex, 0);
        final int hi = Math.min(endIndex, elements.length);
        if (hi <= lo) {
            return empty();
        } else if (hi - lo == elements.length) {
            return this;
        } else {
            return of(Arrays.copyOfRange(elements, lo, hi));
        }
    }

    VectorModel<T> take(int n) {
        return slice(0, n);
    }

    VectorModel<T> drop(int n) {
        return slice(n, elements.length);
    }

    VectorModel<T> takeRight(int n) {
        return (n <= 0) ? empty() : drop(elements.length - n);
    }

    VectorModel<T> dropRight(int n) {
        return (n <= 0) ? this : take(elements.length - n);
    }

    VectorModel<T> tail() {
        if (isEmpty()) {
            throw new UnsupportedOperationException("tail of empty Vector");
        }
        return drop(1);
    }

    VectorModel<T> init() {
        if (isEmpty()) {
            throw new UnsupportedOperationException("init of empty Vector");
        }
        return dropRight(1);
    }

    @SuppressWarnings("unchecked")
    <U> VectorModel<U> map(Function<? super T, ? extends U> mapper) {
        final Object[] result = new Object[elements.length];
        for (int i = 0; i < result.length; i++) {
            result[i] = Objects.requireNonNull(mapper.apply((T) elements[i]), "Vector: element is null");
        }
        return of(result);
    }

    @Override
    @SuppressWarnings("unchecked")
    public java.util.Iterator<T> iterator() {
        return (java.util.Iterator<T>) Arrays.asList(elements).iterator();
    }
}
