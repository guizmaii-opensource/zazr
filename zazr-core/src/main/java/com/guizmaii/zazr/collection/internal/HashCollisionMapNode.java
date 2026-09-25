package com.guizmaii.zazr.collection.internal;

import com.guizmaii.zazr.Tuple;
import com.guizmaii.zazr.Tuple2;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/// The entries of a `HashMap` trie whose keys have one same hash: a leaf below the last level of hash bits, holding at
/// least two entries. Ported from `HashCollisionMapNode` in `scala/collection/immutable/HashMap.scala` of the Scala 3
/// standard library (the Scala 2.13 collection library, which Scala 3 ships unchanged), with the entries kept in a flat
/// array of keys and values instead of a vector of pairs. Immutable: every update copies the array, in a builder too.
///
/// @param <K> the key type
/// @param <V> the value type
final class HashCollisionMapNode<K extends @Nullable Object, V extends @Nullable Object> extends MapNode<K, V> {

    final int hash;
    // key0, value0, key1, value1, ..., in insertion order
    final Object[] content;

    HashCollisionMapNode(int hash, Object[] content) {
        this.hash = hash;
        this.content = content;
    }

    // the index of the entry of `key`, or -1
    int indexOf(@Nullable Object key) {
        for (int i = 0; i < content.length; i += 2) {
            if (Objects.equals(content[i], key)) {
                return i >> 1;
            }
        }
        return -1;
    }

    @SuppressWarnings("unchecked")
    @Override
    K getKey(int index) {
        return (K) content[2 * index];
    }

    @SuppressWarnings("unchecked")
    @Override
    V getValue(int index) {
        return (V) content[2 * index + 1];
    }

    @Override
    int getHash(int index) {
        return hash;
    }

    @Override
    MapNode<K, V> getNode(int index) {
        throw new IndexOutOfBoundsException("No sub-nodes present in hash-collision leaf node.");
    }

    @Override
    boolean hasNodes() {
        return false;
    }

    @Override
    int nodeArity() {
        return 0;
    }

    @Override
    boolean hasPayload() {
        return true;
    }

    @Override
    int payloadArity() {
        return content.length >> 1;
    }

    @Override
    public int size() {
        return content.length >> 1;
    }

    @Override
    int keyHashSum() {
        return size() * hash;
    }

    @Override
    V getOrElse(K key, int hash, int shift, V defaultValue) {
        if (this.hash == hash) {
            final int index = indexOf(key);
            return index >= 0 ? getValue(index) : defaultValue;
        } else {
            return defaultValue;
        }
    }

    @Override
    boolean containsKey(K key, int hash, int shift) {
        return this.hash == hash && indexOf(key) >= 0;
    }

    @Override
    @Nullable Tuple2<K, V> getEntry(K key, int hash, int shift) {
        if (this.hash == hash) {
            final int index = indexOf(key);
            return index >= 0 ? Tuple.of(getKey(index), getValue(index)) : null;
        } else {
            return null;
        }
    }

    @Override
    MapNode<K, V> updated(K key, V value, int hash, int shift, boolean replace) {
        final int index = indexOf(key);
        if (index >= 0) {
            if (replace && (getKey(index) != key || getValue(index) != value)) {
                final Object[] dst = content.clone();
                dst[2 * index] = key;
                dst[2 * index + 1] = value;
                return new HashCollisionMapNode<>(this.hash, dst);
            } else {
                return this;
            }
        } else {
            final Object[] dst = java.util.Arrays.copyOf(content, content.length + 2);
            dst[content.length] = key;
            dst[content.length + 1] = value;
            return new HashCollisionMapNode<>(this.hash, dst);
        }
    }

    @Override
    MapNode<K, V> removed(K key, int hash, int shift) {
        final int index = (this.hash == hash) ? indexOf(key) : -1;
        if (index < 0) {
            return this;
        } else if (content.length == 4) {
            // one entry left: a node of the root level, to be inlined by the parent
            final int other = 1 - index;
            return new BitmapIndexedMapNode<>(null, bitposFrom(maskFrom(this.hash, 0)), 0,
                    new Object[] { getKey(other), getValue(other) }, new int[] { this.hash }, 1, this.hash);
        } else {
            final Object[] dst = new Object[content.length - 2];
            System.arraycopy(content, 0, dst, 0, 2 * index);
            System.arraycopy(content, 2 * index + 2, dst, 2 * index, content.length - 2 * index - 2);
            return new HashCollisionMapNode<>(this.hash, dst);
        }
    }

    @Override
    MapNode<K, V> putInPlace(Object owner, K key, V value, int hash, int shift) {
        return updated(key, value, hash, shift, true);
    }
}
