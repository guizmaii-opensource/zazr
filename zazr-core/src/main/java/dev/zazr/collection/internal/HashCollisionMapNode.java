package dev.zazr.collection.internal;

import dev.zazr.Tuple;
import dev.zazr.Tuple2;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
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

    // `that` is a collision node too: two nodes at the same place below the last level hold keys of the same hash
    @Override
    MapNode<K, V> concat(MapNode<K, V> that, int shift) {
        final HashCollisionMapNode<K, V> right = (HashCollisionMapNode<K, V>) that;
        if (right == this) {
            return this;
        }
        Object[] result = null;
        int length = right.content.length;
        for (int i = 0; i < content.length; i += 2) {
            if (right.indexOf(content[i]) < 0) {
                if (result == null) {
                    result = java.util.Arrays.copyOf(right.content, right.content.length + content.length);
                }
                result[length] = content[i];
                result[length + 1] = content[i + 1];
                length += 2;
            }
        }
        return (result == null) ? right : new HashCollisionMapNode<>(hash, java.util.Arrays.copyOf(result, length));
    }

    @Override
    MapNode<K, V> filter(BiPredicate<? super K, ? super V> predicate, boolean keep) {
        final Object[] kept = new Object[content.length];
        int length = 0;
        for (int i = 0; i < content.length; i += 2) {
            if (predicate.test(getKey(i >> 1), getValue(i >> 1)) == keep) {
                kept[length] = content[i];
                kept[length + 1] = content[i + 1];
                length += 2;
            }
        }
        if (length == content.length) {
            return this;
        } else if (length == 0) {
            return MapNode.empty();
        } else if (length == 2) {
            // one entry left: a node of the root level, to be inlined by the parent
            return new BitmapIndexedMapNode<>(null, bitposFrom(maskFrom(hash, 0)), 0, new Object[] { kept[0], kept[1] },
                    new int[] { hash }, 1, hash);
        } else {
            return new HashCollisionMapNode<>(hash, java.util.Arrays.copyOf(kept, length));
        }
    }

    @Override
    <W extends @Nullable Object> MapNode<K, W> transform(BiFunction<? super K, ? super V, ? extends W> f) {
        Object[] result = null;
        for (int i = 0; i < content.length; i += 2) {
            final W value = Objects.requireNonNull(f.apply(getKey(i >> 1), getValue(i >> 1)), "HashMap: value is null");
            if (result == null && value != content[i + 1]) {
                result = content.clone();
            }
            if (result != null) {
                result[i + 1] = value;
            }
        }
        @SuppressWarnings("unchecked")
        final MapNode<K, W> unchanged = (MapNode<K, W>) this;
        return (result == null) ? unchanged : new HashCollisionMapNode<>(hash, result);
    }
}
