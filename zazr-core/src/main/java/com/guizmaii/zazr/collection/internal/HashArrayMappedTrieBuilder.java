package com.guizmaii.zazr.collection.internal;

import com.guizmaii.zazr.collection.internal.HashArrayMappedTrieModule.AbstractNode;
import com.guizmaii.zazr.collection.internal.HashArrayMappedTrieModule.EmptyNode;
import com.guizmaii.zazr.collection.internal.HashArrayMappedTrieModule.LeafNode;
import java.lang.invoke.VarHandle;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/// The state behind `HashMap.Builder` and `HashSet.Builder`: a transient hash array mapped trie, the pattern of
/// Clojure's transients and of `HashMapBuilder` in the Scala 3 standard library (the Scala 2.13 collection library
/// that Scala 3 ships unchanged). The internal nodes the builder creates carry its owner token and are updated in
/// place while it is open; a node it did not create (one of a map adopted by [#putAll], or of any persistent
/// operation) is copied the first time a put goes through it, and the copy is owned. Leaves are immutable and replaced.
/// The trie produced is the one successive persistent puts of the same keys, in the same order, produce, node for
/// node, for a fraction of the allocations: a put allocates its new leaf and, only on a node not yet owned, a copy.
///
/// [#result()] closes the builder, so no owned node is updated after it: the returned trie never changes, and a
/// persistent operation on it copies nodes as it always does. Single-use and not thread-safe; after [#result()],
/// every method throws [IllegalStateException]. Null keys and values are the caller's to reject.
///
/// @param <K> the key type
/// @param <V> the value type
public final class HashArrayMappedTrieBuilder<K extends @Nullable Object, V extends @Nullable Object> {

    // compared by identity with the owner of each node
    private final Object owner = new Object();
    // the name used in the messages, e.g. "HashMap.Builder"
    private final String name;
    private AbstractNode<K, V> root = EmptyNode.instance();
    private boolean done;

    public HashArrayMappedTrieBuilder(String name) {
        this.name = name;
    }

    /// Puts one entry; the key and value of an equal key already present are both replaced. The caller has called
    /// [#checkOpen()] and checked that neither is null.
    public void put(K key, V value) {
        root = root.putInPlace(owner, 0, Objects.hashCode(key), key, value);
    }

    /// Puts every entry of `trie`, in its iteration order. On an empty builder the trie is adopted as it is, nothing
    /// copied; its nodes are copied only when a later put goes through them. The caller has called [#checkOpen()].
    public void putAll(HashArrayMappedTrie<K, V> trie) {
        if (root.isEmpty()) {
            root = (AbstractNode<K, V>) trie;
        } else {
            final Iterator<LeafNode<K, V>> leaves = ((AbstractNode<K, V>) trie).nodes();
            while (leaves.hasNext()) {
                final LeafNode<K, V> leaf = leaves.next();
                root = root.putInPlace(owner, 0, leaf.hash(), leaf.key(), leaf.value());
            }
        }
    }

    /// The number of distinct keys put so far.
    public int size() {
        checkOpen();
        return root.size();
    }

    /// Returns the trie of the entries put, and closes this builder.
    public HashArrayMappedTrie<K, V> result() {
        checkOpen();
        done = true;
        final AbstractNode<K, V> trie = root;
        root = EmptyNode.instance();
        // the owned nodes were written through non-final fields: order those writes before the publication of the
        // trie, as the end of a constructor does for final fields (Scala's HashMapBuilder.result does the same)
        VarHandle.releaseFence();
        return trie;
    }

    public void checkOpen() {
        if (done) {
            throw new IllegalStateException("result() has already been called on this " + name);
        }
    }
}
