package com.guizmaii.zazr.collection.internal;

import java.util.function.Predicate;
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

    // -- the operations on whole subtrees

    /// The node of the elements of this node and of `that`, which sits at the same place in its trie: of equal
    /// elements, the one of `that` is kept. Returns `that` when this node adds nothing to it, and shares the subtrees
    /// of either side that the other side does not touch.
    abstract SetNode<T> concat(SetNode<T> that, int shift);

    /// The node of the elements for which `predicate` answers `keep`; this node when that is all of them. The
    /// predicate sees the elements in iteration order.
    abstract SetNode<T> filter(Predicate<? super T> predicate, boolean keep);

    /// The node of the elements of this node not in `that`, which sits at the same place in its trie; this node when
    /// none is.
    abstract SetNode<T> diff(SetNode<T> that, int shift);

    /// `true` when every element of this node is in `that`, which sits at the same place in its trie.
    abstract boolean subsetOf(SetNode<T> that, int shift);

    /// `true` when the two subtrees, at the same place in their tries, hold equal elements. Since the shape is
    /// canonical, equal sets have equal bitmaps, hashes and sizes, compared before any element.
    public static boolean sameElements(SetNode<?> a, SetNode<?> b) {
        if (a == b) {
            return true;
        } else if (a instanceof BitmapIndexedSetNode<?> x && b instanceof BitmapIndexedSetNode<?> y) {
            if (x.keyHashSum != y.keyHashSum || x.dataMap != y.dataMap || x.nodeMap != y.nodeMap || x.size != y.size
                || !java.util.Arrays.equals(x.hashes, y.hashes)) {
                return false;
            }
            final int payload = Integer.bitCount(x.dataMap);
            for (int i = 0; i < payload; i++) {
                if (!java.util.Objects.equals(x.content[i], y.content[i])) {
                    return false;
                }
            }
            for (int i = payload; i < x.content.length; i++) {
                if (!sameElements((SetNode<?>) x.content[i], (SetNode<?>) y.content[i])) {
                    return false;
                }
            }
            return true;
        } else if (a instanceof HashCollisionSetNode<?> x && b instanceof HashCollisionSetNode<?> y) {
            if (x.hash != y.hash || x.content.length != y.content.length) {
                return false;
            }
            for (Object element : x.content) {
                if (y.indexOf(element) < 0) {
                    return false;
                }
            }
            return true;
        } else {
            return false;
        }
    }

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
