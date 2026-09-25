package com.guizmaii.zazr.collection.internal;

import static java.lang.Integer.bitCount;

/// A node of a compressed hash-array mapped prefix tree (CHAMP, Steindorfer and Vinju, OOPSLA 2015), the trie behind
/// `HashMap` ([MapNode]) and `HashSet` ([SetNode]). Ported from `Node` in `scala/collection/immutable/ChampCommon.scala`
/// of the Scala 3 standard library, which ships the Scala 2.13 collection library unchanged.
///
/// A node splits 32 slots between two bitmaps: `dataMap` marks the slots holding an entry inline, `nodeMap` the slots
/// holding a child node. The slot of an entry at depth `d` is the fragment `(hash >>> 5d) & 31` of its hash. The entries
/// sit at the front of the node's array, in slot order, and the children at the back, in reverse slot order. Seven
/// levels consume the 32 bits of a hash; below them, keys of one hash share a collision node.
///
/// The shape is canonical: a slot holds a child only when at least two entries share that prefix, so removal compacts
/// the path back to inline entries, and equal collections have the same tree, up to the order inside a collision node.
///
/// The nodes store `Objects.hashCode` of each key, and place the key by [#improve], Scala's bit mixing of it
/// (`scala.collection.Hashing.improve`), so that hash codes which differ only in their high bits still spread over the
/// first levels. The mixing is a bijection, so two keys have equal stored hashes exactly when they have equal mixed ones.
///
/// @param <N> the node type of the trie, map or set
public abstract sealed class ChampNode<N extends ChampNode<N>> permits MapNode, SetNode {

    static final int HASH_CODE_LENGTH = 32;

    static final int BIT_PARTITION_SIZE = 5;

    static final int BIT_PARTITION_MASK = (1 << BIT_PARTITION_SIZE) - 1;

    /// The number of levels a hash splits into: 7, the last of them 2 bits wide.
    static final int MAX_DEPTH = 7;

    static final int BRANCHING_FACTOR = 1 << BIT_PARTITION_SIZE;

    static final Object[] EMPTY_OBJECTS = new Object[0];

    static final int[] EMPTY_INTS = new int[0];

    ChampNode() {
    }

    /// Scala's `Hashing.improve`: the bits of a hash code mixed, so that every bit of it counts in every fragment.
    static int improve(int hashCode) {
        int h = hashCode + ~(hashCode << 9);
        h = h ^ (h >>> 14);
        h = h + (h << 4);
        return h ^ (h >>> 10);
    }

    /// The slot, at `shift`, of a key of hash code `hash`: the fragment of its mixed hash.
    static int maskFrom(int hash, int shift) {
        return (improve(hash) >>> shift) & BIT_PARTITION_MASK;
    }

    static int bitposFrom(int mask) {
        return 1 << mask;
    }

    static int indexFrom(int bitmap, int bitpos) {
        return bitCount(bitmap & (bitpos - 1));
    }

    /// `true` when this node has children.
    abstract boolean hasNodes();

    /// The number of children.
    abstract int nodeArity();

    /// The child at `index`, in slot order.
    abstract N getNode(int index);

    /// `true` when this node holds entries inline.
    abstract boolean hasPayload();

    /// The number of entries held inline.
    abstract int payloadArity();

    /// The hash of the entry at `index`.
    abstract int getHash(int index);

    /// The number of entries in this subtree.
    public abstract int size();

    /// The sum of the hash codes of the keys in this subtree: the hash of a `java.util.Set` of them. (Scala caches the
    /// sum of the mixed hashes; the sum of the hash codes serves the same comparisons and is also a set's hash.)
    abstract int keyHashSum();

    static int[] removeElement(int[] as, int ix) {
        final int[] result = new int[as.length - 1];
        System.arraycopy(as, 0, result, 0, ix);
        System.arraycopy(as, ix + 1, result, ix, as.length - ix - 1);
        return result;
    }

    static int[] insertElement(int[] as, int ix, int elem) {
        final int[] result = new int[as.length + 1];
        System.arraycopy(as, 0, result, 0, ix);
        result[ix] = elem;
        System.arraycopy(as, ix, result, ix + 1, as.length - ix);
        return result;
    }
}
