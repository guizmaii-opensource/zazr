package com.guizmaii.zazr.collection.internal;

import org.jspecify.annotations.Nullable;

/// A node of the CHAMP trie behind `HashSet` (see [ChampNode]): a [BitmapIndexedSetNode], or a [HashCollisionSetNode]
/// below the last level of hash bits. Ported from `SetNode` in `scala/collection/immutable/HashSet.scala` of the Scala 3
/// standard library (the Scala 2.13 collection library, which Scala 3 ships unchanged). The root of a set is always a
/// [BitmapIndexedSetNode]; the empty set is the one of [#empty()].
///
/// Elements are never null; `hash` is always `Objects.hashCode(element)` and `shift` the depth of the node times 5.
///
/// @param <T> the element type
public abstract sealed class SetNode<T extends @Nullable Object> extends ChampNode<SetNode<T>>
        permits BitmapIndexedSetNode, HashCollisionSetNode {

    SetNode() {
    }

    /// The root of the empty set.
    @SuppressWarnings("unchecked")
    public static <T extends @Nullable Object> BitmapIndexedSetNode<T> empty() {
        return (BitmapIndexedSetNode<T>) BitmapIndexedSetNode.EMPTY;
    }

    abstract boolean contains(T element, int hash, int shift);

    /// The node with `element`. When an equal element is present: with `replace`, it is replaced (and this node is
    /// returned if it is the same object); without, this node is returned unchanged.
    abstract SetNode<T> updated(T element, int hash, int shift, boolean replace);

    /// The node without `element`; this node when it is absent.
    abstract SetNode<T> removed(T element, int hash, int shift);

    /// The addition of a [HashSetBuilder]: the node with `element` (an equal element replaced), where a
    /// [BitmapIndexedSetNode] owned by `owner` is updated in place instead of copied, and the one not owned is copied
    /// into a node owned by `owner`, which is updated in place.
    abstract SetNode<T> addInPlace(Object owner, T element, int hash, int shift);

    abstract T getPayload(int index);

    /// An iterator of the elements of this subtree.
    public final Iterator<T> iterator() {
        return size() == 0 ? Iterator.empty() : new ElementIterator<>(this);
    }

    private static final class ElementIterator<T extends @Nullable Object> extends ChampIterator<T, SetNode<T>> {

        ElementIterator(SetNode<T> root) {
            super(root);
        }

        @Override
        protected T getNext() {
            return currentValueNode.getPayload(currentValueCursor++);
        }
    }
}
