package com.guizmaii.zazr.collection.internal;

import java.lang.invoke.VarHandle;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/// The state behind `HashSet.Builder`: a transient CHAMP trie (see [ChampNode]), after `HashSetBuilder` in
/// `scala/collection/immutable/HashSet.scala` of the Scala 3 standard library (the Scala 2.13 collection library, which
/// Scala 3 ships unchanged) and Clojure's transients.
///
/// Where Scala's builder updates every node of its trie in place, and copies the whole trie before writing to one it has
/// handed out, the nodes this builder creates carry its owner token and are the only ones updated in place. A node it
/// did not create (one of a set adopted by [#addAll], or of any persistent operation) is copied the first time an
/// addition goes through it, and the copy is owned. Collision nodes are immutable and replaced. The trie produced is
/// the one successive persistent additions of the same elements produce, node for node, the last of equal elements
/// kept.
///
/// [#result()] closes the builder, so no owned node is updated after it: the returned trie never changes. Single-use
/// and not thread-safe; after [#result()], every method throws [IllegalStateException]. Null elements are the caller's
/// to reject.
///
/// @param <T> the element type
public final class HashSetBuilder<T extends @Nullable Object> {

    // compared by identity with the owner of each node
    private final Object owner = new Object();
    // the name used in the messages, e.g. "HashSet.Builder"
    private final String name;
    private BitmapIndexedSetNode<T> root = SetNode.empty();
    private boolean done;

    public HashSetBuilder(String name) {
        this.name = name;
    }

    /// Adds one element, replacing an equal one already present. The caller has called [#checkOpen()] and checked that
    /// it is not null.
    public void add(T element) {
        root = root.addInPlace(owner, element, Objects.hashCode(element), 0);
    }

    /// Adds every element of the trie `that`, in its iteration order. On an empty builder the trie is adopted as it is,
    /// nothing copied; its nodes are copied only when a later addition goes through them. The caller has called
    /// [#checkOpen()].
    public void addAll(BitmapIndexedSetNode<T> that) {
        if (root.size == 0) {
            root = that;
        } else if (that.size != 0) {
            addAllOf(that);
        }
    }

    private void addAllOf(SetNode<T> node) {
        final int payload = node.payloadArity();
        for (int i = 0; i < payload; i++) {
            root = root.addInPlace(owner, node.getPayload(i), node.getHash(i), 0);
        }
        final int children = node.nodeArity();
        for (int i = 0; i < children; i++) {
            addAllOf(node.getNode(i));
        }
    }

    /// The number of distinct elements added so far.
    public int size() {
        checkOpen();
        return root.size;
    }

    /// Returns the trie of the elements added, and closes this builder.
    public BitmapIndexedSetNode<T> result() {
        checkOpen();
        done = true;
        final BitmapIndexedSetNode<T> trie = root;
        root = SetNode.empty();
        // the owned nodes were written through non-final fields: order those writes before the publication of the
        // trie, as the end of a constructor does for final fields (Scala's HashSetBuilder.result does the same)
        VarHandle.releaseFence();
        return trie.size == 0 ? SetNode.empty() : trie;
    }

    public void checkOpen() {
        if (done) {
            throw new IllegalStateException("result() has already been called on this " + name);
        }
    }
}
