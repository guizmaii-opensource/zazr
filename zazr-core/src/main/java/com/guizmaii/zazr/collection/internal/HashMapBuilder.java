package com.guizmaii.zazr.collection.internal;

import java.lang.invoke.VarHandle;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/// The state behind `HashMap.Builder`: a transient CHAMP trie (see [ChampNode]), after `HashMapBuilder` in
/// `scala/collection/immutable/HashMap.scala` of the Scala 3 standard library (the Scala 2.13 collection library, which
/// Scala 3 ships unchanged) and Clojure's transients.
///
/// Where Scala's builder updates every node of its trie in place, and copies the whole trie before writing to one it has
/// handed out, the nodes this builder creates carry its owner token and are the only ones updated in place. A node it
/// did not create (one of a map adopted by [#putAll], or of any persistent operation) is copied the first time a put
/// goes through it, and the copy is owned. Collision nodes are immutable and replaced. The trie produced is the one
/// successive persistent puts of the same entries produce, node for node.
///
/// [#result()] closes the builder, so no owned node is updated after it: the returned trie never changes. Single-use
/// and not thread-safe; after [#result()], every method throws [IllegalStateException]. Null keys and values are the
/// caller's to reject.
///
/// @param <K> the key type
/// @param <V> the value type
public final class HashMapBuilder<K extends @Nullable Object, V extends @Nullable Object> {

    // compared by identity with the owner of each node
    private final Object owner = new Object();
    // the name used in the messages, e.g. "HashMap.Builder"
    private final String name;
    private BitmapIndexedMapNode<K, V> root = MapNode.empty();
    private boolean done;

    public HashMapBuilder(String name) {
        this.name = name;
    }

    /// Puts one entry; the key and value of an equal key already present are both replaced. The caller has called
    /// [#checkOpen()] and checked that neither is null.
    public void put(K key, V value) {
        root = root.putInPlace(owner, key, value, Objects.hashCode(key), 0);
    }

    /// Puts every entry of the trie `that`, in its iteration order. On an empty builder the trie is adopted as it is,
    /// nothing copied; its nodes are copied only when a later put goes through them. The caller has called
    /// [#checkOpen()].
    public void putAll(BitmapIndexedMapNode<K, V> that) {
        if (root.size == 0) {
            root = that;
        } else if (that.size != 0) {
            putAllOf(that);
        }
    }

    private void putAllOf(MapNode<K, V> node) {
        final int payload = node.payloadArity();
        for (int i = 0; i < payload; i++) {
            root = root.putInPlace(owner, node.getKey(i), node.getValue(i), node.getHash(i), 0);
        }
        final int children = node.nodeArity();
        for (int i = 0; i < children; i++) {
            putAllOf(node.getNode(i));
        }
    }

    /// The number of distinct keys put so far.
    public int size() {
        checkOpen();
        return root.size;
    }

    /// Returns the trie of the entries put, and closes this builder.
    public BitmapIndexedMapNode<K, V> result() {
        checkOpen();
        done = true;
        final BitmapIndexedMapNode<K, V> trie = root;
        root = MapNode.empty();
        // the owned nodes were written through non-final fields: order those writes before the publication of the
        // trie, as the end of a constructor does for final fields (Scala's HashMapBuilder.result does the same)
        VarHandle.releaseFence();
        return trie.size == 0 ? MapNode.empty() : trie;
    }

    public void checkOpen() {
        if (done) {
            throw new IllegalStateException("result() has already been called on this " + name);
        }
    }
}
