package com.guizmaii.zazr.collection.internal;

import com.guizmaii.zazr.Tuple2;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import org.jspecify.annotations.Nullable;

/// A node of the CHAMP trie behind `HashMap` (see [ChampNode]): a [BitmapIndexedMapNode], or a [HashCollisionMapNode]
/// below the last level of hash bits. Ported from `MapNode` in `scala/collection/immutable/HashMap.scala` of the Scala 3
/// standard library (the Scala 2.13 collection library, which Scala 3 ships unchanged). The root of a map is always a
/// [BitmapIndexedMapNode]; the empty map is the one of [#empty()].
///
/// Keys and values are never null; `hash` is always `Objects.hashCode(key)` and `shift` the depth of the node times 5.
///
/// @param <K> the key type
/// @param <V> the value type
public abstract sealed class MapNode<K extends @Nullable Object, V extends @Nullable Object> extends ChampNode<MapNode<K, V>>
        permits BitmapIndexedMapNode, HashCollisionMapNode {

    MapNode() {
    }

    /// The root of the empty map.
    @SuppressWarnings("unchecked")
    public static <K extends @Nullable Object, V extends @Nullable Object> BitmapIndexedMapNode<K, V> empty() {
        return (BitmapIndexedMapNode<K, V>) BitmapIndexedMapNode.EMPTY;
    }

    abstract V getOrElse(K key, int hash, int shift, V defaultValue);

    abstract boolean containsKey(K key, int hash, int shift);

    /// The key kept for `key` and its value, or null when absent.
    abstract @Nullable Tuple2<K, V> getEntry(K key, int hash, int shift);

    /// The node with `key` mapped to `value`. When an equal key is present: with `replace`, its key and value are both
    /// replaced (and this node is returned if they are the same objects); without, this node is returned unchanged.
    abstract MapNode<K, V> updated(K key, V value, int hash, int shift, boolean replace);

    /// The node without `key`; this node when the key is absent.
    abstract MapNode<K, V> removed(K key, int hash, int shift);

    /// The put of a [HashMapBuilder]: the node with `key` mapped to `value` (key and value replaced when an equal key is
    /// present), where a [BitmapIndexedMapNode] owned by `owner` is updated in place instead of copied, and the one
    /// not owned is copied into a node owned by `owner`, which is updated in place.
    abstract MapNode<K, V> putInPlace(Object owner, K key, V value, int hash, int shift);

    // -- the operations on whole subtrees

    /// The node of the entries of this node and of `that`, which sits at the same place in its trie: of equal keys,
    /// the entry of `that` is kept, key and value. Returns `that` when this node adds nothing to it, and shares the
    /// subtrees of either side that the other side does not touch.
    abstract MapNode<K, V> concat(MapNode<K, V> that, int shift);

    /// The node of the entries for which `predicate` answers `keep`; this node when that is all of them. The
    /// predicate sees the entries in iteration order.
    abstract MapNode<K, V> filter(BiPredicate<? super K, ? super V> predicate, boolean keep);

    /// The node of the same keys, each value replaced by `f(key, value)`, called in iteration order; this node when
    /// every new value is the same object as the old one. A null value is rejected as a put rejects it.
    abstract <W extends @Nullable Object> MapNode<K, W> transform(BiFunction<? super K, ? super V, ? extends W> f);

    /// `true` when the two subtrees, at the same place in their tries, hold equal keys mapped to equal values. Since
    /// the shape is canonical, equal maps have equal bitmaps, hashes and sizes, compared before any key.
    public static boolean sameEntries(MapNode<?, ?> a, MapNode<?, ?> b) {
        if (a == b) {
            return true;
        } else if (a instanceof BitmapIndexedMapNode<?, ?> x && b instanceof BitmapIndexedMapNode<?, ?> y) {
            if (x.keyHashSum != y.keyHashSum || x.dataMap != y.dataMap || x.nodeMap != y.nodeMap || x.size != y.size
                || !java.util.Arrays.equals(x.hashes, y.hashes)) {
                return false;
            }
            final int payload = 2 * Integer.bitCount(x.dataMap);
            for (int i = 0; i < payload; i++) {
                if (!java.util.Objects.equals(x.content[i], y.content[i])) {
                    return false;
                }
            }
            for (int i = payload; i < x.content.length; i++) {
                if (!sameEntries((MapNode<?, ?>) x.content[i], (MapNode<?, ?>) y.content[i])) {
                    return false;
                }
            }
            return true;
        } else if (a instanceof HashCollisionMapNode<?, ?> x && b instanceof HashCollisionMapNode<?, ?> y) {
            if (x.hash != y.hash || x.content.length != y.content.length) {
                return false;
            }
            for (int i = 0; i < x.content.length; i += 2) {
                final int j = y.indexOf(x.content[i]);
                if (j < 0 || !java.util.Objects.equals(x.content[i + 1], y.content[2 * j + 1])) {
                    return false;
                }
            }
            return true;
        } else {
            return false;
        }
    }

    abstract K getKey(int index);

    abstract V getValue(int index);

    /// Calls `action` with the key and the value of every entry of this subtree, in iteration order: the entries of a
    /// node before those of its children.
    public final void forEach(BiConsumer<? super K, ? super V> action) {
        final int payload = payloadArity();
        for (int i = 0; i < payload; i++) {
            action.accept(getKey(i), getValue(i));
        }
        final int children = nodeArity();
        for (int i = 0; i < children; i++) {
            getNode(i).forEach(action);
        }
    }

    /// An iterator of `f(key, value)` over the entries of this subtree.
    public final <T extends @Nullable Object> Iterator<T> iterator(BiFunction<? super K, ? super V, ? extends T> f) {
        return size() == 0 ? Iterator.empty() : new EntryIterator<>(this, f);
    }

    /// An iterator of the keys of this subtree.
    public final Iterator<K> keysIterator() {
        return size() == 0 ? Iterator.empty() : new KeyIterator<>(this);
    }

    /// An iterator of the values of this subtree.
    public final Iterator<V> valuesIterator() {
        return size() == 0 ? Iterator.empty() : new ValueIterator<>(this);
    }

    private static final class EntryIterator<K extends @Nullable Object, V extends @Nullable Object, T extends @Nullable Object>
            extends ChampIterator<T, MapNode<K, V>> {

        private final BiFunction<? super K, ? super V, ? extends T> f;

        EntryIterator(MapNode<K, V> root, BiFunction<? super K, ? super V, ? extends T> f) {
            super(root);
            this.f = f;
        }

        @Override
        protected T getNext() {
            final int cursor = currentValueCursor++;
            return f.apply(currentValueNode.getKey(cursor), currentValueNode.getValue(cursor));
        }
    }

    private static final class KeyIterator<K extends @Nullable Object, V extends @Nullable Object> extends ChampIterator<K, MapNode<K, V>> {

        KeyIterator(MapNode<K, V> root) {
            super(root);
        }

        @Override
        protected K getNext() {
            return currentValueNode.getKey(currentValueCursor++);
        }
    }

    private static final class ValueIterator<K extends @Nullable Object, V extends @Nullable Object> extends ChampIterator<V, MapNode<K, V>> {

        ValueIterator(MapNode<K, V> root) {
            super(root);
        }

        @Override
        protected V getNext() {
            return currentValueNode.getValue(currentValueCursor++);
        }
    }
}
