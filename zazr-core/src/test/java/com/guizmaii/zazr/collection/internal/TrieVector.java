package com.guizmaii.zazr.collection.internal;

import java.util.ArrayList;
import java.util.NoSuchElementException;
import java.util.function.Function;

import static com.guizmaii.zazr.collection.internal.Collections.withSize;

/**
 * The oracle of {@link RadixVectorDifferentialTest}: the operations it calls, with the contract of {@code Vector},
 * computed on {@link BitMappedTrie}, an independent implementation.
 */
final class TrieVector<T> implements Iterable<T> {

    private static final TrieVector<?> EMPTY = new TrieVector<>(BitMappedTrie.empty());

    private final BitMappedTrie<T> trie;

    private TrieVector(BitMappedTrie<T> trie) {
        this.trie = trie;
    }

    @SuppressWarnings("unchecked")
    static <T> TrieVector<T> empty() {
        return (TrieVector<T>) EMPTY;
    }

    @SuppressWarnings("unchecked")
    static <T> TrieVector<T> ofAll(Iterable<? extends T> iterable) {
        if (iterable instanceof TrieVector<?> v) {
            return (TrieVector<T>) v;
        }
        final Object[] elements;
        if (Collections.isTraversableAgain(iterable)) {
            elements = withSize(iterable).toArray();
        } else {
            final ArrayList<T> list = new ArrayList<>();
            for (T element : iterable) {
                list.add(java.util.Objects.requireNonNull(element, "Vector: element is null"));
            }
            elements = list.toArray();
        }
        return of(BitMappedTrie.ofAll(elements));
    }

    private static <T> TrieVector<T> of(BitMappedTrie<T> trie) {
        return (trie.length() == 0) ? empty() : new TrieVector<>(trie);
    }

    @SuppressWarnings("ObjectEquality")
    private TrieVector<T> wrap(BitMappedTrie<T> trie) {
        return (trie == this.trie) ? this : of(trie);
    }

    int length() {
        return trie.length();
    }

    boolean isEmpty() {
        return length() == 0;
    }

    T get(int index) {
        if (index >= 0 && index < length()) {
            return trie.get(index);
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
        return get(length() - 1);
    }

    TrieVector<T> update(int index, T element) {
        if (index >= 0 && index < length()) {
            return wrap(trie.update(index, element));
        }
        throw new IndexOutOfBoundsException("update(" + index + ")");
    }

    TrieVector<T> append(T element) {
        return appendAll(java.util.List.of(element));
    }

    TrieVector<T> prepend(T element) {
        return prependAll(java.util.List.of(element));
    }

    TrieVector<T> appendAll(Iterable<? extends T> iterable) {
        if (isEmpty()) {
            return ofAll(iterable);
        }
        if (!Collections.isTraversableAgain(iterable) && !(iterable instanceof TrieVector)) {
            final TrieVector<T> elements = ofAll(iterable);
            return elements.isEmpty() ? this : appendAll(elements);
        }
        if (Collections.isEmpty(iterable)) {
            return this;
        }
        return new TrieVector<>(trie.appendAll(sized(iterable)));
    }

    TrieVector<T> prependAll(Iterable<? extends T> iterable) {
        if (isEmpty()) {
            return ofAll(iterable);
        }
        if (!Collections.isTraversableAgain(iterable) && !(iterable instanceof TrieVector)) {
            final TrieVector<T> elements = ofAll(iterable);
            return elements.isEmpty() ? this : prependAll(elements);
        }
        if (Collections.isEmpty(iterable)) {
            return this;
        }
        return new TrieVector<>(trie.prependAll(sized(iterable)));
    }

    /* another TrieVector as a java.util.Collection, whose size the trie reads without copying it into a List first */
    private static <T> Iterable<? extends T> sized(Iterable<? extends T> iterable) {
        if (iterable instanceof TrieVector<? extends T> v) {
            final ArrayList<T> list = new ArrayList<>(v.length());
            for (T element : v) {
                list.add(element);
            }
            return list;
        }
        return iterable;
    }

    TrieVector<T> take(int n) {
        return wrap(trie.take(n));
    }

    TrieVector<T> drop(int n) {
        return wrap(trie.drop(n));
    }

    TrieVector<T> takeRight(int n) {
        return n <= 0 ? empty() : drop(length() - n);
    }

    TrieVector<T> dropRight(int n) {
        return n <= 0 ? this : take(length() - n);
    }

    TrieVector<T> slice(int beginIndex, int endIndex) {
        if (beginIndex >= endIndex || beginIndex >= length() || isEmpty()) {
            return empty();
        } else if (beginIndex <= 0 && endIndex >= length()) {
            return this;
        } else {
            return take(endIndex).drop(beginIndex);
        }
    }

    TrieVector<T> tail() {
        if (isEmpty()) {
            throw new UnsupportedOperationException("tail of empty Vector");
        }
        return drop(1);
    }

    TrieVector<T> init() {
        if (isEmpty()) {
            throw new UnsupportedOperationException("init of empty Vector");
        }
        return dropRight(1);
    }

    <U> TrieVector<U> map(Function<? super T, ? extends U> mapper) {
        return of(trie.map(mapper));
    }

    @Override
    public java.util.Iterator<T> iterator() {
        return isEmpty() ? java.util.Collections.emptyIterator() : trie.iterator();
    }
}
