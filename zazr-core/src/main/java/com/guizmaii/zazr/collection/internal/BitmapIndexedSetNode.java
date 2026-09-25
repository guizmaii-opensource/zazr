package com.guizmaii.zazr.collection.internal;

import java.util.Objects;
import org.jspecify.annotations.Nullable;

import static java.lang.Integer.bitCount;

/// The node of a `HashSet` trie that splits its 32 slots between inline elements and children (see [ChampNode]).
/// Ported from `BitmapIndexedSetNode` in `scala/collection/immutable/HashSet.scala` of the Scala 3 standard library (the
/// Scala 2.13 collection library, which Scala 3 ships unchanged).
///
/// `content` holds the elements, `payloadArity` slots in slot order, then the children in reverse slot order; `hashes`
/// the hash of each element. The node caches the size and the sum of the hashes of its subtree.
///
/// The fields are not final: a node owned by a [HashSetBuilder] (its `owner` is the builder's token) is updated in
/// place until the builder's `result()`, which fences the writes before the trie is published. A node of a persistent
/// operation has no owner and never changes; a trie is only reached through the final field of its `HashSet` or of a
/// view, whose freeze covers the nodes.
///
/// @param <T> the element type
public final class BitmapIndexedSetNode<T extends @Nullable Object> extends SetNode<T> {

    static final BitmapIndexedSetNode<?> EMPTY = new BitmapIndexedSetNode<>(null, 0, 0, EMPTY_OBJECTS, EMPTY_INTS, 0, 0);

    int dataMap;
    int nodeMap;
    Object[] content;
    int[] hashes;
    int size;
    int keyHashSum;
    // the builder token that may update this node in place; null for a node of a persistent operation
    final @Nullable Object owner;

    BitmapIndexedSetNode(@Nullable Object owner, int dataMap, int nodeMap, Object[] content, int[] hashes, int size, int keyHashSum) {
        this.owner = owner;
        this.dataMap = dataMap;
        this.nodeMap = nodeMap;
        this.content = content;
        this.hashes = hashes;
        this.size = size;
        this.keyHashSum = keyHashSum;
    }

    // -- the entry points of HashSet

    /// `true` when `element` is present.
    public boolean contains(T element) {
        return contains(element, Objects.hashCode(element), 0);
    }

    /// The trie with `element`: an equal element present is replaced when `replace` is set, and kept (this trie
    /// returned) otherwise; this trie too when it is the same object.
    public BitmapIndexedSetNode<T> updated(T element, boolean replace) {
        return updated(element, Objects.hashCode(element), 0, replace);
    }

    /// The trie without `element`; this trie when it is absent.
    public BitmapIndexedSetNode<T> removed(T element) {
        return removed(element, Objects.hashCode(element), 0);
    }

    // -- accessors

    @SuppressWarnings("unchecked")
    @Override
    T getPayload(int index) {
        return (T) content[index];
    }

    @Override
    int getHash(int index) {
        return hashes[index];
    }

    @SuppressWarnings("unchecked")
    @Override
    SetNode<T> getNode(int index) {
        return (SetNode<T>) content[content.length - 1 - index];
    }

    @Override
    boolean hasNodes() {
        return nodeMap != 0;
    }

    @Override
    int nodeArity() {
        return bitCount(nodeMap);
    }

    @Override
    boolean hasPayload() {
        return dataMap != 0;
    }

    @Override
    int payloadArity() {
        return bitCount(dataMap);
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    int keyHashSum() {
        return keyHashSum;
    }

    int dataIndex(int bitpos) {
        return bitCount(dataMap & (bitpos - 1));
    }

    int nodeIndex(int bitpos) {
        return bitCount(nodeMap & (bitpos - 1));
    }

    // -- lookup

    @Override
    boolean contains(T element, int hash, int shift) {
        final int bitpos = bitposFrom(maskFrom(hash, shift));
        if ((dataMap & bitpos) != 0) {
            final int index = indexFrom(dataMap, bitpos);
            return hashes[index] == hash && Objects.equals(element, getPayload(index));
        } else if ((nodeMap & bitpos) != 0) {
            return getNode(indexFrom(nodeMap, bitpos)).contains(element, hash, shift + BIT_PARTITION_SIZE);
        } else {
            return false;
        }
    }

    // -- persistent updates

    @Override
    BitmapIndexedSetNode<T> updated(T element, int hash, int shift, boolean replace) {
        final int bitpos = bitposFrom(maskFrom(hash, shift));
        if ((dataMap & bitpos) != 0) {
            final int index = indexFrom(dataMap, bitpos);
            final T element0 = getPayload(index);
            final int hash0 = hashes[index];
            if (hash0 == hash && Objects.equals(element0, element)) {
                if (replace && element0 != element) {
                    final Object[] dst = content.clone();
                    dst[index] = element;
                    return new BitmapIndexedSetNode<>(null, dataMap, nodeMap, dst, hashes, size, keyHashSum);
                } else {
                    return this;
                }
            } else {
                final SetNode<T> subNodeNew = mergeTwoKeyValPairs(null, element0, hash0, element, hash, shift + BIT_PARTITION_SIZE);
                return copyAndMigrateFromInlineToNode(bitpos, hash0, subNodeNew);
            }
        } else if ((nodeMap & bitpos) != 0) {
            final SetNode<T> subNode = getNode(indexFrom(nodeMap, bitpos));
            final SetNode<T> subNodeNew = subNode.updated(element, hash, shift + BIT_PARTITION_SIZE, replace);
            return (subNodeNew == subNode) ? this : copyAndSetNode(bitpos, subNode, subNodeNew);
        } else {
            return copyAndInsertValue(bitpos, element, hash);
        }
    }

    @Override
    BitmapIndexedSetNode<T> removed(T element, int hash, int shift) {
        final int bitpos = bitposFrom(maskFrom(hash, shift));
        if ((dataMap & bitpos) != 0) {
            final int index = indexFrom(dataMap, bitpos);
            if (hashes[index] == hash && Objects.equals(getPayload(index), element)) {
                if (payloadArity() == 2 && nodeArity() == 0) {
                    // the node of the remaining element either becomes the root, or is inlined by the parent: its slot
                    // is the one of the root level
                    final int newDataMap = (shift == 0) ? (dataMap ^ bitpos) : bitposFrom(maskFrom(hash, 0));
                    final int other = 1 - index;
                    return new BitmapIndexedSetNode<>(null, newDataMap, 0, new Object[] { getPayload(other) },
                            new int[] { hashes[other] }, 1, hashes[other]);
                } else {
                    return copyAndRemoveValue(bitpos, hash);
                }
            } else {
                return this;
            }
        } else if ((nodeMap & bitpos) != 0) {
            final SetNode<T> subNode = getNode(indexFrom(nodeMap, bitpos));
            final SetNode<T> subNodeNew = subNode.removed(element, hash, shift + BIT_PARTITION_SIZE);
            if (subNodeNew == subNode) {
                return this;
            }
            if (subNodeNew.size() == 1) {
                if (size == subNode.size()) {
                    // the child was all this node held: the single remaining element goes up, to be inlined by the parent
                    return (BitmapIndexedSetNode<T>) subNodeNew;
                } else {
                    return copyAndMigrateFromNodeToInline(bitpos, subNode, subNodeNew);
                }
            } else {
                return copyAndSetNode(bitpos, subNode, subNodeNew);
            }
        } else {
            return this;
        }
    }

    /// The node of two elements whose hashes agree up to `shift`, owned by `owner`: one node holding both when their
    /// fragments at `shift` differ, a chain of single-child nodes down to where they do, or a collision node below the
    /// last level.
    static <T extends @Nullable Object> SetNode<T> mergeTwoKeyValPairs(@Nullable Object owner, T element0, int hash0, T element1,
            int hash1, int shift) {
        if (shift >= HASH_CODE_LENGTH) {
            return new HashCollisionSetNode<>(hash0, new Object[] { element0, element1 });
        }
        final int mask0 = maskFrom(hash0, shift);
        final int mask1 = maskFrom(hash1, shift);
        if (mask0 != mask1) {
            final int dataMap = bitposFrom(mask0) | bitposFrom(mask1);
            final int keyHashSum = hash0 + hash1;
            if (mask0 < mask1) {
                return new BitmapIndexedSetNode<>(owner, dataMap, 0, new Object[] { element0, element1 }, new int[] { hash0, hash1 }, 2, keyHashSum);
            } else {
                return new BitmapIndexedSetNode<>(owner, dataMap, 0, new Object[] { element1, element0 }, new int[] { hash1, hash0 }, 2, keyHashSum);
            }
        } else {
            final SetNode<T> node = mergeTwoKeyValPairs(owner, element0, hash0, element1, hash1, shift + BIT_PARTITION_SIZE);
            return new BitmapIndexedSetNode<>(owner, 0, bitposFrom(mask0), new Object[] { node }, EMPTY_INTS, node.size(), node.keyHashSum());
        }
    }

    private BitmapIndexedSetNode<T> copyAndSetNode(int bitpos, SetNode<T> oldNode, SetNode<T> newNode) {
        final Object[] dst = content.clone();
        dst[dst.length - 1 - nodeIndex(bitpos)] = newNode;
        return new BitmapIndexedSetNode<>(null, dataMap, nodeMap, dst, hashes, size - oldNode.size() + newNode.size(),
                keyHashSum - oldNode.keyHashSum() + newNode.keyHashSum());
    }

    private BitmapIndexedSetNode<T> copyAndInsertValue(int bitpos, T element, int hash) {
        final int dataIx = dataIndex(bitpos);
        final Object[] src = content;
        final Object[] dst = new Object[src.length + 1];
        System.arraycopy(src, 0, dst, 0, dataIx);
        dst[dataIx] = element;
        System.arraycopy(src, dataIx, dst, dataIx + 1, src.length - dataIx);
        return new BitmapIndexedSetNode<>(null, dataMap | bitpos, nodeMap, dst, insertElement(hashes, dataIx, hash), size + 1,
                keyHashSum + hash);
    }

    private BitmapIndexedSetNode<T> copyAndRemoveValue(int bitpos, int hash) {
        final int dataIx = dataIndex(bitpos);
        final Object[] src = content;
        final Object[] dst = new Object[src.length - 1];
        System.arraycopy(src, 0, dst, 0, dataIx);
        System.arraycopy(src, dataIx + 1, dst, dataIx, src.length - dataIx - 1);
        return new BitmapIndexedSetNode<>(null, dataMap ^ bitpos, nodeMap, dst, removeElement(hashes, dataIx), size - 1,
                keyHashSum - hash);
    }

    // the content of this node with the element of `bitpos` moved out, and `node` inserted among the children
    private Object[] migratedFromInlineToNode(int bitpos, SetNode<T> node) {
        final int idxOld = dataIndex(bitpos);
        final int idxNew = content.length - 1 - nodeIndex(bitpos);
        final Object[] src = content;
        final Object[] dst = new Object[src.length];
        System.arraycopy(src, 0, dst, 0, idxOld);
        System.arraycopy(src, idxOld + 1, dst, idxOld, idxNew - idxOld);
        dst[idxNew] = node;
        System.arraycopy(src, idxNew + 1, dst, idxNew + 1, src.length - idxNew - 1);
        return dst;
    }

    private BitmapIndexedSetNode<T> copyAndMigrateFromInlineToNode(int bitpos, int hash, SetNode<T> node) {
        return new BitmapIndexedSetNode<>(null, dataMap ^ bitpos, nodeMap | bitpos, migratedFromInlineToNode(bitpos, node),
                removeElement(hashes, dataIndex(bitpos)), size - 1 + node.size(), keyHashSum - hash + node.keyHashSum());
    }

    private BitmapIndexedSetNode<T> copyAndMigrateFromNodeToInline(int bitpos, SetNode<T> oldNode, SetNode<T> node) {
        final int idxOld = content.length - 1 - nodeIndex(bitpos);
        final int idxNew = dataIndex(bitpos);
        final Object[] src = content;
        final Object[] dst = new Object[src.length];
        System.arraycopy(src, 0, dst, 0, idxNew);
        dst[idxNew] = node.getPayload(0);
        System.arraycopy(src, idxNew, dst, idxNew + 1, idxOld - idxNew);
        System.arraycopy(src, idxOld + 1, dst, idxOld + 1, src.length - idxOld - 1);
        return new BitmapIndexedSetNode<>(null, dataMap | bitpos, nodeMap ^ bitpos, dst, insertElement(hashes, idxNew, node.getHash(0)),
                size - oldNode.size() + 1, keyHashSum - oldNode.keyHashSum() + node.keyHashSum());
    }

    // -- the updates in place of a builder

    @Override
    BitmapIndexedSetNode<T> addInPlace(Object owner, T element, int hash, int shift) {
        final BitmapIndexedSetNode<T> node = (this.owner == owner)
                                             ? this
                                             : new BitmapIndexedSetNode<>(owner, dataMap, nodeMap, content.clone(), hashes, size, keyHashSum);
        node.update(owner, element, hash, shift);
        return node;
    }

    // `this` is owned by `owner`; its hashes array is never written in place, only replaced, so it may be shared
    private void update(Object owner, T element, int hash, int shift) {
        final int bitpos = bitposFrom(maskFrom(hash, shift));
        if ((dataMap & bitpos) != 0) {
            final int index = indexFrom(dataMap, bitpos);
            final T element0 = getPayload(index);
            final int hash0 = hashes[index];
            if (hash0 == hash && Objects.equals(element0, element)) {
                content[index] = element;
            } else {
                final SetNode<T> subNodeNew = mergeTwoKeyValPairs(owner, element0, hash0, element, hash, shift + BIT_PARTITION_SIZE);
                // an element and a child take one slot each, so the owned array is reused: the elements after the
                // migrated one and the children before the new one shift left by one
                final int idxNew = content.length - 1 - nodeIndex(bitpos);
                System.arraycopy(content, index + 1, content, index, idxNew - index);
                content[idxNew] = subNodeNew;
                hashes = removeElement(hashes, index);
                dataMap ^= bitpos;
                nodeMap |= bitpos;
                size = size - 1 + subNodeNew.size();
                keyHashSum = keyHashSum - hash0 + subNodeNew.keyHashSum();
            }
        } else if ((nodeMap & bitpos) != 0) {
            final int slot = content.length - 1 - nodeIndex(bitpos);
            @SuppressWarnings("unchecked")
            final SetNode<T> subNode = (SetNode<T>) content[slot];
            // read before the addition, which may update the child in place
            final int subNodeSize = subNode.size();
            final int subNodeHashSum = subNode.keyHashSum();
            final SetNode<T> subNodeNew = subNode.addInPlace(owner, element, hash, shift + BIT_PARTITION_SIZE);
            content[slot] = subNodeNew;
            size = size - subNodeSize + subNodeNew.size();
            keyHashSum = keyHashSum - subNodeHashSum + subNodeNew.keyHashSum();
        } else {
            final int dataIx = dataIndex(bitpos);
            final Object[] src = content;
            final Object[] dst = new Object[src.length + 1];
            System.arraycopy(src, 0, dst, 0, dataIx);
            dst[dataIx] = element;
            System.arraycopy(src, dataIx, dst, dataIx + 1, src.length - dataIx);
            content = dst;
            hashes = insertElement(hashes, dataIx, hash);
            dataMap |= bitpos;
            size++;
            keyHashSum += hash;
        }
    }
}
